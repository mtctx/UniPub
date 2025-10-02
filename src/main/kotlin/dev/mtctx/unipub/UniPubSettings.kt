/*
 *     UniPub: UniPubSettings.kt
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

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UniPubSettings(
    val repositories: List<Repository>,
    val gpgKey: GpgKey? = null
) {
    init {
        require(repositories.isNotEmpty()) { "Repository list in settings file cannot be empty" }
    }

    @Serializable
    data class Repository(
        @SerialName("name")
        private val _name: String,
        @SerialName("url")
        private val _url: String,
        @SerialName("username")
        private val _username: String,
        @SerialName("password")
        private val _password: String
    ) {
        val name get() = _name.resolveEnv()
        val url get() = _url.resolveEnv()
        val username get() = _username.resolveEnv()
        val password get() = _password.resolveEnv()

        init {
            require(name.isNotBlank()) { "A repository 'name' cannot be blank in the settings file." }
            require(url.isNotBlank()) { "The 'url' for repository '$name' cannot be blank." }
            require(username.isNotBlank()) { "The 'username' for repository '$name' cannot be blank." }
            require(password.isNotBlank()) { "The 'password' for repository '$name' cannot be blank." }
        }

        enum class URL(val url: String) {
            MAVEN_CENTRAL("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/"),
            MAVEN_CENTRAL_SNAPSHOTS("https://s01.oss.sonatype.org/content/repositories/snapshots/"),
        }
    }

    @Serializable
    data class GpgKey(
        @SerialName("keyId")
        private val _keyId: String? = null,
        @SerialName("passphrase")
        private val _passphrase: String? = null,
        @SerialName("privateKey")
        private val _privateKey: String? = null,
    ) {
        val keyId get() = _keyId?.resolveEnv().orEmpty()
        val passphrase get() = _passphrase?.resolveEnv().orEmpty()
        val privateKey get() = _privateKey?.resolveEnv().orEmpty()
    }
}
