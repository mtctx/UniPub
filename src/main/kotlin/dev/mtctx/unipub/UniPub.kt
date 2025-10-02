/*
 *     UniPub: UniPub.kt
 *     Copyright (C) 2025 mtctx
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package dev.mtctx.unipub

import com.charleskorn.kaml.Yaml
import dev.mtctx.unipub.task.UniPubGenerateTemplateSettingsInHomeDirTask
import dev.mtctx.unipub.task.UniPubGenerateTemplateSettingsInProjectDirTask
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.UnknownTaskException
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register
import org.gradle.plugins.signing.SigningExtension
import java.io.File
import java.nio.file.Paths
import kotlin.io.path.absolutePathString

class UniPub : Plugin<Project> {
    override fun apply(target: Project) {
        val extension = target.extensions.create<UniPubExtension>("unipub")

        target.pluginManager.apply("maven-publish")
        target.pluginManager.apply("signing")

        target.tasks.register<UniPubGenerateTemplateSettingsInProjectDirTask>("generateSettingsFileInProjectDir") {
            group = "UniPub"
            description = "Generate a default settings file for UniPub in the project directory."
        }
        target.tasks.register<UniPubGenerateTemplateSettingsInHomeDirTask>("generateSettingsFileInHomeDir") {
            group = "UniPub"
            description = "Generate a default settings file for UniPub in ${
                Paths.get(
                    System.getProperty("user.home"),
                    ".unipub"
                ).absolutePathString()
            }."
        }

        target.afterEvaluate {
            if (extension.shouldAutoApplyJavaPlugin()) project.pluginManager.apply("java-library")

            val (projectInfo, developerInfos) = extension.projectAndDeveloperInfos()
            var artifactInfos = extension.artifactInfos().toMutableList()

            if (artifactInfos.isEmpty()) {
                if (!components.names.contains("java")) {
                    throw GradleException(
                        """
                            UniPub: No 'java' component found.
                            Please apply either the 'java' or 'java-library' plugin in your build.gradle.kts,
                            or enable autoApplyJavaPlugin() in the unipub block.""".trimIndent()
                    )
                }

                artifactInfos = mutableListOf(ArtifactInfo.Component("java"))
            }

            val settings = loadAndValidateSettingsFile(extension)

            artifactInfos.filterIsInstance<ArtifactInfo.Component>().forEach { artifact ->
                if (!components.names.contains(artifact.componentName)) {
                    logger.warn("Component '${artifact.componentName}' not found. Skipping.")
                    artifactInfos.remove(artifact)
                }
            }

            artifactInfos.filterIsInstance<ArtifactInfo.Task>().forEach { artifact ->
                if (tasks.findByName(artifact.task.name) == null) {
                    logger.warn("Task '${artifact.task.name}' not found. Skipping.")
                    artifactInfos.remove(artifact)
                }
            }

            configurePublishing(settings, projectInfo, developerInfos, artifactInfos)
            configureSigning(settings, extension.isUsingMemoryPgpKey())
        }
    }

    private fun loadAndValidateSettingsFile(extension: UniPubExtension): UniPubSettings {
        val settingsFilePath = extension.uniPubSettingsFile.get().resolveEnv()
        require(settingsFilePath.isNotBlank()) { "UniPub settings file path cannot be blank." }

        val settingsFile = File(settingsFilePath)
        require(settingsFile.exists()) {
            "UniPub settings file not found at path: ${settingsFile.absolutePath}"
        }

        val settings = Yaml.default.decodeFromString(
            UniPubSettings.serializer(),
            settingsFile.readText()
        )

        if (extension.isUsingMemoryPgpKey()) {
            val gpg = settings.gpgKey
                ?: throw GradleException("UniPub: GPG config must be provided when using in-memory PGP keys.")
            require(gpg.keyId.isNotBlank()) { "GPG 'keyId' cannot be blank when using in-memory PGP." }
            require(gpg.passphrase.isNotBlank()) { "GPG 'passphrase' cannot be blank when using in-memory PGP." }
            require(gpg.privateKey.isNotBlank()) { "GPG 'privateKey' cannot be blank when using in-memory PGP." }
        }

        return settings
    }

    private fun Project.configurePublishing(
        settings: UniPubSettings,
        projectInfo: ProjectInfo,
        developerInfos: List<DeveloperInfo>,
        artifactInfos: List<ArtifactInfo>,
    ) {
        extensions.getByType<PublishingExtension>().apply {
            repositories {
                settings.repositories.forEach { repository ->
                    maven {
                        name = repository.name
                        url = repository.uri
                        credentials {
                            username = repository.username
                            password = repository.password
                        }
                    }
                }
            }

            val publication = publications.maybeCreate("unipub", MavenPublication::class.java)

            publication.groupId = projectInfo.groupId
            publication.artifactId = projectInfo.id
            publication.version = projectInfo.version

            artifactInfos.forEach { artifactInfo ->
                when (artifactInfo) {
                    is ArtifactInfo.Component -> {
                        val component = components.findByName(artifactInfo.componentName)
                        if (component != null) {
                            publication.from(component)
                        } else {
                            logger.warn("Component '${artifactInfo.componentName}' not found during publication")
                        }
                    }

                    is ArtifactInfo.Task ->
                        try {
                            publication.artifact(artifactInfo.task)
                        } catch (e: UnknownTaskException) {
                            logger.error("Task not found: ${artifactInfo.task.name}")
                            logger.trace("Stacktrace: ", e)
                        }

                    is ArtifactInfo.File ->
                        publication.artifact(artifactInfo.file) {
                            artifactInfo.classifier?.let { classifier = it }
                        }

                    is ArtifactInfo.Custom ->
                        artifactInfo.configure(publication)
                }
            }

            publication.pom {
                name.set(projectInfo.name)
                description.set(projectInfo.description)
                inceptionYear.set(projectInfo.inceptionYear)
                url.set(projectInfo.url)
                licenses {
                    projectInfo.licenses.forEach { licenseInfo ->
                        license {
                            name.set(licenseInfo.name)
                            url.set(licenseInfo.url)
                            distribution.set(licenseInfo.distribution.value)
                        }
                    }
                }
                developers {
                    developerInfos.forEach { developerInfo ->
                        developer {
                            name.set(developerInfo.name)
                            email.set(developerInfo.email)
                            organization.set(developerInfo.organization)
                            organizationUrl.set(developerInfo.organizationUrl)
                        }
                    }
                }
                scm {
                    url.set(projectInfo.scm.url)
                    connection.set(projectInfo.scm.connection)
                    developerConnection.set(projectInfo.scm.developerConnection)
                }
            }
        }

        project.tasks.withType(PublishToMavenRepository::class.java) {
            doFirst {
                require(developerInfos.isNotEmpty()) {
                    "UniPub: At least one developer must be defined before publishing."
                }
                require(projectInfo.licenses.isNotEmpty()) {
                    "UniPub: At least one license must be defined before publishing."
                }
                require(projectInfo.scm.url.isNotBlank()) {
                    "UniPub: SCM URL must be provided before publishing."
                }

            }
        }
    }

    @Throws(GradleException::class)
    private fun Project.configureSigning(settings: UniPubSettings, useInMemoryPgpKey: Boolean = false) {
        val publishing = extensions.getByType<PublishingExtension>()
        extensions.getByType<SigningExtension>().apply {
            if (!useInMemoryPgpKey) {
                useGpgCmd()
                sign(publishing.publications)
            } else if (settings.gpgKey?.keyId?.isNotBlank() == true && settings.gpgKey.privateKey.isNotBlank()) {
                useInMemoryPgpKeys(
                    settings.gpgKey.keyId,
                    settings.gpgKey.privateKey,
                    settings.gpgKey.passphrase
                )
                sign(publishing.publications)
            } else throw GradleException("GPG not configured properly. Exiting.")
        }
    }
}

@Throws(IllegalStateException::class)
fun String.resolveEnv(): String =
    if (startsWith("ENV(") && endsWith(")")) {
        val envKey = removePrefix("ENV(").removeSuffix(")")
        System.getenv(envKey) ?: throw IllegalStateException("Required environment variable '$envKey' is not set.")
    } else this