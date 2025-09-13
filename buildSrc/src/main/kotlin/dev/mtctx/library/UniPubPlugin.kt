/*
 *     UniPub: UniPubPlugin.kt
 *     Copyright (C) 2025 mtctx
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package dev.mtctx.library

import com.charleskorn.kaml.Yaml
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.*
import org.gradle.plugins.signing.SigningExtension
import java.io.File
import javax.inject.Inject
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString

private fun String.resolveEnv(): String =
    if (startsWith("ENV(") && endsWith(")")) {
        val envKey = removePrefix("ENV(").removeSuffix(")")
        System.getenv(envKey) ?: throw IllegalStateException("Required environment variable '$envKey' is not set.")
    } else {
        this
    }

class UniPubPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        val extension = target.extensions.create<UniPubExtension>("unipub")

        target.pluginManager.apply("maven-publish")
        target.pluginManager.apply("signing")

        target.tasks.register<UniPubGenerateTemplateSettingsInProjectDirTask>("generateSettingsFileInProjectDir") {
            group = "UniPub"
            description = "Generate a default settings file for UniPub in the project directory."
        }
        target.tasks.register<UniPubGenerateTemplateSettingsInHomeDirTask>("generateSettingsFileInHomeDir") {
            group = "UniPub"
            description = "Generate a default settings file for UniPub in ${
                Path(
                    System.getProperty("user.home"),
                    ".unipub"
                ).absolutePathString()
            }."
        }

        target.afterEvaluate {
            val (projectInfo, developerInfos) = validateExtensionConfiguration(extension)
            val settings = loadAndValidateSettingsFile(extension)

            configurePublishing(settings, projectInfo, developerInfos)
            configureSigning(settings)
        }
    }

    private fun validateExtensionConfiguration(extension: UniPubExtension): Pair<ProjectInfo, List<DeveloperInfo>> {
        require(extension.project.isPresent) {
            "UniPub 'project' info is not configured. Please configure the 'unipub' extension in your build script."
        }
        require(extension.developer.isPresent) {
            "UniPub 'developer' info is not configured. Please configure the 'unipub' extension in your build script."
        }
        val projectInfo = extension.project.get()
        val developerInfos = extension.developer.get()

        require(developerInfos.isNotEmpty()) {
            "UniPub 'developer' list cannot be empty."
        }
        return projectInfo to developerInfos
    }

    private fun loadAndValidateSettingsFile(extension: UniPubExtension): UniPubSettings {
        val settingsFilePath = extension.uniPubSettingsFile.get().resolveEnv()
        require(settingsFilePath.isNotBlank()) { "UniPub settings file path cannot be blank." }

        val settingsFile = File(settingsFilePath)
        require(settingsFile.exists()) {
            "UniPub settings file not found at path: ${settingsFile.absolutePath}"
        }

        return Yaml.default.decodeFromString(
            UniPubSettings.serializer(),
            settingsFile.readText()
        )
    }

    private fun Project.configurePublishing(
        settings: UniPubSettings,
        projectInfo: ProjectInfo,
        developerInfos: List<DeveloperInfo>
    ) {
        extensions.getByType<PublishingExtension>().apply {
            repositories {
                settings.repositories.forEach { repository ->
                    maven {
                        name = repository.name
                        url = uri(repository.url)
                        credentials {
                            username = repository.username
                            password = repository.password
                        }
                    }
                }
            }

            publications.withType<MavenPublication> {
                pom {
                    name.set(projectInfo.name)
                    description.set(projectInfo.description)
                    inceptionYear.set(projectInfo.inceptionYear)
                    url.set(projectInfo.url)
                    licenses {
                        projectInfo.licenses.forEach { licenseInfo ->
                            license {
                                name.set(licenseInfo.name)
                                url.set(licenseInfo.url)
                                distribution.set(licenseInfo.distribution.value)
                            }
                        }
                    }
                    developers {
                        developerInfos.forEach { developerInfo ->
                            developer {
                                name.set(developerInfo.name)
                                email.set(developerInfo.email)
                                organization.set(developerInfo.organization)
                                organizationUrl.set(developerInfo.organizationUrl)
                            }
                        }
                    }
                    scm {
                        url.set(projectInfo.scm.url)
                        connection.set(projectInfo.scm.connection)
                        developerConnection.set(projectInfo.scm.developerConnection)
                    }
                }
            }
        }
    }

    private fun Project.configureSigning(settings: UniPubSettings) {
        extensions.getByType<SigningExtension>().apply {
            useInMemoryPgpKeys(
                settings.gpgKey.keyId,
                settings.gpgKey.privateKey,
                settings.gpgKey.passphrase
            )
            sign(extensions.getByType<PublishingExtension>().publications)
        }
    }
}

abstract class UniPubExtension @Inject constructor(objects: ObjectFactory) {
    val project: Property<ProjectInfo> = objects.property()
    val developer: ListProperty<DeveloperInfo> = objects.listProperty()
    val uniPubSettingsFile: Property<String> = objects.property<String>().convention(
        Path(System.getProperty("user.home"), "main.unipub").absolutePathString()
    )
}

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

@Serializable
data class UniPubSettings(
    val repositories: List<Repository>,
    val gpgKey: GpgKey
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
        private val _keyId: String,
        @SerialName("passphrase")
        private val _passphrase: String,
        @SerialName("privateKey")
        private val _privateKey: String,
    ) {
        val keyId get() = _keyId.resolveEnv()
        val passphrase get() = _passphrase.resolveEnv()
        val privateKey get() = _privateKey.resolveEnv()

        init {
            require(keyId.isNotBlank()) { "GPG 'keyId' cannot be blank in the settings file." }
            require(passphrase.isNotBlank()) { "GPG 'passphrase' cannot be blank in the settings file." }
            require(privateKey.isNotBlank()) { "GPG 'privateKey' cannot be blank in the settings file." }
        }
    }
}