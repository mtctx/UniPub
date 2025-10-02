/*
 *     UniPub: UniPubExtension.kt
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

import okio.Path
import okio.Path.Companion.toPath

open class UniPubExtension {
    internal var profileName: String = "primary"
    internal var unipubPath: Path = globalPath

    fun profile(name: String) {
        profileName = name.resolveEnv()
    }

    fun unipubPath(path: String) = unipubPath(fs.canonicalize(path.resolveEnv().toPath()))

    fun unipubPath(path: Path) {
        unipubPath = path
    }
}
