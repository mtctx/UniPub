/*
 *     UniPub: build.gradle.kts
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

plugins {
    `kotlin-dsl`
    kotlin("jvm") version "2.2.0"
    kotlin("plugin.serialization") version "2.2.0"
    id("com.gradle.plugin-publish") version "1.2.1"
    id("org.jetbrains.dokka") version "2.0.0"
}

group = "dev.mtctx.unipub"
version = "2.0.6"

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation("com.squareup.okio:okio:3.16.0")
    implementation("com.charleskorn.kaml:kaml:0.95.0")
}

gradlePlugin {
    website = "https://github.com/mtctx/UniPub"
    vcsUrl = "https://github.com/mtctx/UniPub.git"

    plugins {
        register("unipub") {
            id = "dev.mtctx.unipub"
            displayName = "UniPub"
            description = "A Gradle plugin for publishing to Maven Repositories."
            implementationClass = "dev.mtctx.unipub.UniPub"
            tags = listOf(
                "maven",
                "central",
                "publishing",
                "library",
                "publish",
                "publish to maven repositories",
                "maven repository",
                "maven repositories"
            )
        }
    }
}

dokka {
    dokkaPublications.html {
        outputDirectory.set(layout.buildDirectory.dir("dokka"))
    }

    dokkaSourceSets.configureEach {
        jdkVersion.set(21)
        sourceLink {
            localDirectory.set(file("src/main/kotlin"))
            remoteUrl.set(uri("https://github.com/mtctx/UniPub/"))
            remoteLineSuffix.set("#L")
        }
    }
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}