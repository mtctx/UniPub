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

package dev.mtctx.unipub

import com.charleskorn.kaml.Yaml
import okio.Path
import okio.Path.Companion.toPath
import okio.buffer
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

open class UniPubGenerateTemplateUniPubInProjectDirTask : UniPubGenerateTemplateUniPubTask() {
    override fun file(): Path = projectPath(project.projectDir.absolutePath)

    @TaskAction
    override fun run() {
        super.run()
        val gitignore = fs
            .canonicalize(project.projectDir.absolutePath.toPath())
            .resolve(".gitignore")

        val entry = file().name

        if (!fs.exists(gitignore)) {
            fs.write(gitignore) {
                writeUtf8(entry)
                writeUtf8("\n")
            }
            return
        }

        val content = fs.read(gitignore) { readUtf8() }
        if (!content.contains(entry)) {
            fs.appendingSink(gitignore).buffer().use {
                it.writeUtf8("\n")
                it.writeUtf8(entry)
            }
        }
    }

}

open class UniPubGenerateTemplateUniPubInHomeDirTask : UniPubGenerateTemplateUniPubTask() {
    override fun file(): Path = globalPath
}

abstract class UniPubGenerateTemplateUniPubTask : DefaultTask() {
    @Input
    var profileName: String = "main"

    @Input
    var overwrite: Boolean = false

    abstract fun file(): Path

    @TaskAction
    open fun run() {
        var settingsYaml = Yaml.default.encodeToString(
            UniPubSettings.serializer(), UniPubSettings(
                profiles = listOf(
                    UniPubSettings.Profile(
                        "primary # Can be anything you want",
                        "YOUR_USERNAME",
                        "YOUR_PASSWORD"
                    ),
                    UniPubSettings.Profile(
                        "secondary",
                        "YOUR_USERNAME",
                        "YOUR_PASSWORD"
                    )
                )
            )
        )

        settingsYaml = """
            # You can customize the settings file for UniPub here.
            # You can use ENV(VARIABLE_NAME) to refer to environment variables.
            
            $settingsYaml
        """.trimIndent()

        val file = file()
        if (fs.exists(file) && !overwrite) {
            logger.lifecycle("Settings file already exists at $file. Use '-Poverwrite=true' to overwrite it.")
            return
        }

        fs.write(file) {
            writeUtf8(settingsYaml)
        }
        logger.lifecycle("Settings file generated at $file")
    }
}