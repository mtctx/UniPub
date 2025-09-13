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

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.listProperty
import org.gradle.kotlin.dsl.property
import javax.inject.Inject
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString

abstract class UniPubExtension @Inject constructor(objects: ObjectFactory) {
    val project: Property<ProjectInfo> = objects.property()
    val developer: ListProperty<DeveloperInfo> = objects.listProperty()
    val uniPubSettingsFile: Property<String> = objects.property<String>().convention(
        Path(System.getProperty("user.home"), "main.unipub").absolutePathString()
    )
}