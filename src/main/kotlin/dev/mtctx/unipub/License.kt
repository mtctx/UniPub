/*
 *     UniPub: License.kt
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

sealed interface License {
    val name: String
    val url: String
    val distribution: Distribution

    companion object {
        @JvmStatic
        val AGPL_V3 = LicenseInfo(
            _name = "GNU Affero General Public License v3.0",
            _url = "https://www.gnu.org/licenses/agpl-3.0.en.html",
            _distribution = Distribution.REPO,
        )

        @JvmStatic
        val GPL_V3 = LicenseInfo(
            _name = "GNU General Public License v3.0",
            _url = "https://www.gnu.org/licenses/gpl-3.0.en.html",
            _distribution = Distribution.REPO
        )

        @JvmStatic
        val LGPL_V3 = LicenseInfo(
            _name = "GNU Lesser General Public License v3.0",
            _url = "https://www.gnu.org/licenses/lgpl-3.0.en.html",
            _distribution = Distribution.REPO
        )

        @JvmStatic
        val MPL_2_0 = LicenseInfo(
            _name = "Mozilla Public License 2.0",
            _url = "https://www.mozilla.org/en-US/MPL/2.0/",
            _distribution = Distribution.REPO
        )

        @JvmStatic
        val APACHE_2_0 = LicenseInfo(
            _name = "Apache License 2.0",
            _url = "https://www.apache.org/licenses/LICENSE-2.0",
            _distribution = Distribution.REPO
        )

        @JvmStatic
        val MIT = LicenseInfo(
            _name = "MIT License",
            _url = "https://opensource.org/licenses/MIT",
            _distribution = Distribution.REPO
        )

        @JvmStatic
        val BOOST_1_0 = LicenseInfo(
            _name = "Boost Software License 1.0",
            _url = "https://www.boost.org/LICENSE_1_0.txt",
            _distribution = Distribution.SRC
        )

        @JvmStatic
        val UNLICENSE = LicenseInfo(
            _name = "The Unlicense",
            _url = "https://unlicense.org/",
            _distribution = Distribution.REPO
        )

        @JvmStatic
        fun CUSTOM(
            name: String,
            url: String,
            distribution: Distribution,
        ) = LicenseInfo(name, url, distribution)
    }

    enum class Distribution(val value: String) {
        REPO("repo"),
        SRC("src"),
        BINARY("binary"),
        MANUAL("manual"),
        MANUAL_DOWNLOAD("manual-download"),
    }
}

data class LicenseInfo(
    private val _name: String,
    private val _url: String,
    private val _distribution: License.Distribution,
) : License {
    override val name = _name.resolveEnv()
    override val url = _url.resolveEnv()
    override val distribution get() = _distribution
}