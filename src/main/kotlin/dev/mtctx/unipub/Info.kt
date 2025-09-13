/*
 *     UniPub: Info.kt
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

data class ProjectInfo(
    private val _name: String,
    private val _id: String,
    private val _description: String,
    private val _version: String,
    private val _inceptionYear: String,
    private val _groupId: String,
    private val _url: String,
    val licenses: List<License>,
    val scm: SCM,
) {
    val name get() = _name.resolveEnv()
    val id get() = _id.resolveEnv()
    val description get() = _description.resolveEnv()
    val version get() = _version.resolveEnv()
    val inceptionYear get() = _inceptionYear.resolveEnv()
    val groupId get() = _groupId.resolveEnv()
    val url get() = _url.resolveEnv()

    data class SCM(
        private val _url: String,
        private val _connection: String,
        private val _developerConnection: String,
    ) {
        val url get() = _url.resolveEnv()
        val connection get() = _connection.resolveEnv()
        val developerConnection get() = _developerConnection.resolveEnv()
    }
}

data class DeveloperInfo(
    private val _name: String,
    private val _email: String,
    private val _organization: String,
    private val _organizationUrl: String,
) {
    val name get() = _name.resolveEnv()
    val email get() = _email.resolveEnv()
    val organization get() = _organization.resolveEnv()
    val organizationUrl get() = _organizationUrl.resolveEnv()
}