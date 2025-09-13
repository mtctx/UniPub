/*
 *     UniPub: UniPubExtension.kt
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

package dev.mtctx.unipub

import dev.mtctx.unipub.dsl.ArtifactsBuilder
import dev.mtctx.unipub.dsl.DevelopersBuilder
import dev.mtctx.unipub.dsl.ProjectBuilder
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.property
import javax.inject.Inject
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString

abstract class UniPubExtension @Inject constructor(objects: ObjectFactory) {
    private val projectInfo = objects.property<ProjectInfo>()
    private val developerInfos = mutableListOf<DeveloperInfo>()
    private val artifactInfos = mutableListOf<ArtifactInfo>()
    val uniPubSettingsFile: Property<String> = objects.property<String>().convention(
        Path(System.getProperty("user.home"), "main.unipub").absolutePathString()
    )

    fun artifacts(block: ArtifactsBuilder.() -> Unit) {
        val builder = ArtifactsBuilder()
        builder.block()
        artifactInfos.addAll(builder.build())
    }

    fun project(block: ProjectBuilder.() -> Unit) {
        val builder = ProjectBuilder()
        builder.block()
        projectInfo.set(builder.build())
    }

    fun developers(block: DevelopersBuilder.() -> Unit) {
        val builder = DevelopersBuilder()
        builder.block()
        developerInfos.addAll(builder.build())
    }

    internal fun projectInfo(): ProjectInfo = projectInfo.get()
    internal fun developerInfos(): List<DeveloperInfo> = developerInfos
    internal fun projectAndDeveloperInfos(): Pair<ProjectInfo, List<DeveloperInfo>> =
        projectInfo.get() to developerInfos

    internal fun artifactInfos(): List<ArtifactInfo> = artifactInfos
}