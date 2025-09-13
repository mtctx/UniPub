/*
 *     UniPub: UniPub.kt
 *     Copyright (C) 2025  mtctx
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

package dev.mtctx.library

import com.charleskorn.kaml.Yaml
import dev.mtctx.library.task.UniPubGenerateTemplateSettingsInHomeDirTask
import dev.mtctx.library.task.UniPubGenerateTemplateSettingsInProjectDirTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.gradle.plugins.signing.SigningExtension
import java.io.File
import kotlin.io.path.Path
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
                Path(
                    System.getProperty("user.home"),
                    ".unipub"
                ).absolutePathString()
            }."
        }

        target.afterEvaluate {
            val (projectInfo, developerInfos) = validateExtensionConfiguration(extension)
            val settings = loadAndValidateSettingsFile(extension)

            configurePublishing(settings, projectInfo, developerInfos)
            configureSigning(settings)
        }
    }

    private fun validateExtensionConfiguration(extension: UniPubExtension): Pair<ProjectInfo, List<DeveloperInfo>> {
        require(extension.project.isPresent) {
            "UniPub 'project' info is not configured. Please configure the 'unipub' extension in your build script."
        }
        require(extension.developer.isPresent) {
            "UniPub 'developer' info is not configured. Please configure the 'unipub' extension in your build script."
        }
        val projectInfo = extension.project.get()
        val developerInfos = extension.developer.get()

        require(developerInfos.isNotEmpty()) {
            "UniPub 'developer' list cannot be empty."
        }
        return projectInfo to developerInfos
    }

    private fun loadAndValidateSettingsFile(extension: UniPubExtension): UniPubSettings {
        val settingsFilePath = extension.uniPubSettingsFile.get().resolveEnv()
        require(settingsFilePath.isNotBlank()) { "UniPub settings file path cannot be blank." }

        val settingsFile = File(settingsFilePath)
        require(settingsFile.exists()) {
            "UniPub settings file not found at path: ${settingsFile.absolutePath}"
        }

        return Yaml.default.decodeFromString(
            UniPubSettings.serializer(),
            settingsFile.readText()
        )
    }

    private fun Project.configurePublishing(
        settings: UniPubSettings,
        projectInfo: ProjectInfo,
        developerInfos: List<DeveloperInfo>
    ) {
        extensions.getByType<PublishingExtension>().apply {
            repositories {
                settings.repositories.forEach { repository ->
                    maven {
                        name = repository.name
                        url = uri(repository.url)
                        credentials {
                            username = repository.username
                            password = repository.password
                        }
                    }
                }
            }

            publications.withType<MavenPublication> {
                pom {
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
        }
    }

    private fun Project.configureSigning(settings: UniPubSettings) {
        extensions.getByType<SigningExtension>().apply {
            useInMemoryPgpKeys(
                settings.gpgKey.keyId,
                settings.gpgKey.privateKey,
                settings.gpgKey.passphrase
            )
            sign(extensions.getByType<PublishingExtension>().publications)
        }
    }
}

fun String.resolveEnv(): String =
    if (startsWith("ENV(") && endsWith(")")) {
        val envKey = removePrefix("ENV(").removeSuffix(")")
        System.getenv(envKey) ?: throw IllegalStateException("Required environment variable '$envKey' is not set.")
    } else this