/*
 *     UniPub: DSL.kt
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

package dev.mtctx.unipub.dsl

import dev.mtctx.unipub.ArtifactInfo
import dev.mtctx.unipub.DeveloperInfo
import dev.mtctx.unipub.License
import dev.mtctx.unipub.ProjectInfo
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.TaskProvider
import java.io.File

class ArtifactsBuilder {
    private val artifacts = mutableListOf<ArtifactInfo>()

    fun component(name: String) {
        artifacts.add(ArtifactInfo.Component(name))
    }

    fun task(task: TaskProvider<*>) {
        artifacts.add(ArtifactInfo.Task(task))
    }

    fun file(file: File, classifier: String? = null) {
        artifacts.add(ArtifactInfo.File(file, classifier))
    }

    fun custom(classifier: String? = null, extension: String? = null, configure: (MavenPublication) -> Unit) {
        artifacts.add(ArtifactInfo.Custom(classifier, extension, configure))
    }

    fun build(): List<ArtifactInfo> = artifacts
}

class ProjectBuilder {
    var name: String = ""
    var id: String = ""
    var description: String = ""
    var version: String = ""
    var inceptionYear: String = ""
    var groupId: String = ""
    var url: String = ""
    var licenses: List<License> = emptyList()
    private var scmBuilder: ScmBuilder? = null

    fun scm(block: ScmBuilder.() -> Unit) {
        val builder = ScmBuilder()
        builder.block()
        scmBuilder = builder
    }

    fun build(): ProjectInfo {
        require(name.isNotBlank()) { "Project name cannot be blank." }
        require(id.isNotBlank()) { "Project ID cannot be blank." }
        require(description.isNotBlank()) { "Project description cannot be blank." }
        require(inceptionYear.isNotBlank()) { "Project inception year cannot be blank." }
        require(url.isNotBlank()) { "Project URL cannot be blank." }
        require(licenses.isNotEmpty()) { "Project Licenses cannot be empty." }

        val scm = scmBuilder?.build() ?: throw IllegalStateException("SCM must be configured for project")
        return ProjectInfo(
            _name = name,
            _id = id,
            _description = description,
            _version = version,
            _inceptionYear = inceptionYear,
            _groupId = groupId,
            _url = url,
            licenses = licenses,
            scm = scm
        )
    }
}

class ScmBuilder {
    var url: String = ""
    var connection: String = ""
    var developerConnection: String = ""

    fun build(): ProjectInfo.SCM {
        require(url.isNotBlank()) { "SCM URL cannot be blank." }
        if (connection.isBlank()) connection = buildUrl(url, "git")
        if (developerConnection.isBlank()) connection = buildUrl(url, "ssh")

        return ProjectInfo.SCM(
            _url = url,
            _connection = connection,
            _developerConnection = developerConnection
        )
    }

    private fun buildUrl(url: String, newProtocol: String): String =
        "scm:git:${url.replaceFirst("https://", "$newProtocol://").replaceFirst("http://", "$newProtocol://")}.git"
}

class DevelopersBuilder {
    private val developers = mutableListOf<DeveloperInfo>()

    fun developer(block: DeveloperBuilder.() -> Unit) {
        val builder = DeveloperBuilder()
        builder.block()
        developers.add(builder.build())
    }

    fun build(): List<DeveloperInfo> = developers
}

class DeveloperBuilder {
    var name: String = ""
    var email: String = ""
    var organization: String = "None"
    var organizationUrl: String = ""

    fun build(): DeveloperInfo {
        require(name.isNotBlank()) { "Developer name cannot be blank." }
        require(email.isNotBlank()) { "Developer email cannot be blank." }

        return DeveloperInfo(
            _name = name,
            _email = email,
            _organization = organization,
            _organizationUrl = organizationUrl
        )
    }
}
