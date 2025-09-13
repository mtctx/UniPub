/*
 *     UniPub: GenerateSettingsTemplateTasks.kt
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

package dev.mtctx.unipub.task

import com.charleskorn.kaml.Yaml
import dev.mtctx.unipub.UniPubSettings
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import java.io.File
import kotlin.io.path.Path

class UniPubGenerateTemplateSettingsInProjectDirTask : UniPubGenerateTemplateSettingsTask() {
    override fun file(profileName: String): File = File(project.projectDir, "$profileName.unipub")

    @TaskAction
    override fun run() {
        super.run()
        val gitignore = File(project.projectDir, ".gitignore")

        if (!gitignore.exists()) {
            gitignore.writeText("/${file(profileName).name}")
            return
        }

        if (!gitignore.readText().contains("${file(profileName).name}")) {
            gitignore.appendText("\n${file(profileName).name}")
        }
    }
}

class UniPubGenerateTemplateSettingsInHomeDirTask : UniPubGenerateTemplateSettingsTask() {
    override fun file(profileName: String): File =
        Path(System.getProperty("user.home"), ".unipub", "$profileName.unipub").toFile().apply { parentFile.mkdirs() }
}

abstract class UniPubGenerateTemplateSettingsTask : DefaultTask() {
    @Input
    var profileName: String = "main"

    @Input
    var overwrite: Boolean = false

    abstract fun file(profileName: String): File

    @TaskAction
    open fun run() {
        var settingsYaml = Yaml.default.encodeToString(
            UniPubSettings.serializer(), UniPubSettings(
                repositories = listOf(
                    UniPubSettings.Repository(
                        "maven-central",
                        UniPubSettings.Repository.URL.MAVEN_CENTRAL.url,
                        "YOUR_USERNAME",
                        "YOUR_PASSWORD"
                    )
                ),
                gpgKey = UniPubSettings.GpgKey("YOUR_KEY_ID", "YOUR_PASSPHRASE", "YOUR_PRIVATE_KEY")
            )
        )

        settingsYaml = """
            # You can customize the settings file for UniPub here.
            # You can use ENV(VARIABLE_NAME) to refer to environment variables.
            
            $settingsYaml
        """.trimIndent()

        val file = file(profileName)
        if (file.exists() && !overwrite) {
            logger.lifecycle("Settings file already exists at ${file.absolutePath}. Use '-Poverwrite=true' to overwrite it.")
            return
        }

        file.writeText(settingsYaml)
        logger.lifecycle("Settings file generated at ${file.absolutePath}")
    }
}