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
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.register

val fs = FileSystem.SYSTEM
val globalPath = fs.canonicalize(System.getProperty("user.home").toPath()).resolve(".unipub.yml")
fun projectPath(projectDir: String) = fs.canonicalize(projectDir.toPath()).resolve("unipub.yml")

class UniPub : Plugin<Project> {
    override fun apply(target: Project) {
        val extension = target.extensions.create<UniPubExtension>("unipub")

        target.tasks.register<UniPubGenerateTemplateUniPubInProjectDirTask>("generateUniPubFileInProjectDir") {
            group = "UniPub"
            description = "Generate a template file for UniPub in the project directory."
        }
        target.tasks.register<UniPubGenerateTemplateUniPubInHomeDirTask>("generateUniPubFileInHomeDir") {
            group = "UniPub"
            description = "Generate a template file for UniPub at $globalPath."
        }


        val settings =
            loadAndValidateSettingsFile(extension, projectPath(target.project.projectDir.absolutePath))
        val profile = settings.profiles.find { it.name == extension.profileName }
            ?: throw GradleException(
                """
                        Profile '${extension.profileName}' not found in settings file. Please check your settings file and try again.'
                        Available profiles: ${settings.profiles.joinToString(", ") { it.name }}
                        """.trimIndent()
            )

        val usesVanniktechMavenPublish = target.plugins.hasPlugin("com.vanniktech.maven.publish")
        if (usesVanniktechMavenPublish) {
            target.gradle.startParameter.projectProperties["mavenCentralUsername"] = profile.username
            target.gradle.startParameter.projectProperties["mavenCentralPassword"] = profile.password
            target.logger.lifecycle("UniPub: Injected Gradle properties for Vanniktech (profile '${profile.name}')")
        }

        target.tasks.withType(PublishToMavenRepository::class.java).configureEach {
            doFirst {
                if (repository.url.scheme == "file") return@doFirst

                repository.credentials {
                    username = username?.takeIf { it.isNotBlank() && it.isNotEmpty() } ?: profile.username
                    password = password?.takeIf { it.isNotBlank() && it.isNotEmpty() } ?: profile.password
                }

                logger.lifecycle("UniPub: Injected credentials for profile '${profile.name}'")
            }
        }
    }

    private fun loadAndValidateSettingsFile(extension: UniPubExtension, projectDir: Path): UniPubSettings {
        val path = when {
            fs.exists(extension.unipubPath) -> extension.unipubPath
            fs.exists(projectDir) -> projectDir
            fs.exists(globalPath) -> globalPath
            else -> throw GradleException(
                """
                    UniPub settings file not found. Checked:
                    - ${extension.unipubPath}
                    - $globalPath
                    """.trimIndent()
            )
        }

        return Yaml.default.decodeFromString(
            UniPubSettings.serializer(),
            fs.read(path) { readUtf8() }
        )
    }
}

@Throws(IllegalStateException::class)
fun String.resolveEnv(): String =
    if (Regex("^ENV\\([^\" ]+\\)$").matches(this)) {
        val envKey = removePrefix("ENV(").removeSuffix(")")
        System.getenv(envKey) ?: throw IllegalStateException("Required environment variable '$envKey' is not set.")
    } else this
