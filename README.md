# UniPub » Simplifying Maven Publishing

UniPub (Unified Publishing) is a Gradle plugin designed to streamline the process of publishing artifacts to Maven
repositories. It
simplifies the configuration by separating project metadata from sensitive credentials, using a clean DSL in your build
script and a dedicated settings file for secrets.

-----

## How to Set Up

Setting up UniPub involves three main steps: applying the plugin, creating a settings file for your credentials, and
configuring your project's metadata in the build script.

### 1\. Apply the Plugin

First, apply the UniPub plugin in your `build.gradle.kts` file. You'll also need to add the repository where the plugin
is hosted.

```kotlin
// build.gradle.kts
plugins {
    id("dev.mtctx.unipub") version "LATEST_VERSION"
}
``` 

### 2\. Create the Settings File

UniPub uses a YAML file (`.unipub`) to store sensitive data like repository credentials and GPG keys, keeping them out
of your version-controlled build scripts.

You can generate a template for this file by running one of the provided Gradle tasks:

* **For a project-specific file:** `./gradlew generateSettingsFileInProjectDir`
    * This creates a `.unipub` file in your project's root directory and adds it to your `.gitignore`.
* **For a global file:** `./gradlew generateSettingsFileInHomeDir`
    * This creates a `.unipub` file in a `.unipub` directory in your user home folder.

**Note:** The default profile name is `main`. You can change it by adding the `--profileName=<name>` option to the
command.

After generating the file, you must edit it to add your actual credentials. For security, it's highly recommended to
store secrets in environment variables and reference them using the `ENV()` syntax.

**Example `main.unipub` file:**

```yaml
# You can use ENV(VARIABLE_NAME) to refer to environment variables.
repositories:
  - name: "maven-central"
    url: "https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/"
    username: "ENV(OSSRH_USERNAME)"
    password: "ENV(OSSRH_PASSWORD)"
gpgKey:
  keyId: "ENV(GPG_KEY_ID)"
  passphrase: "ENV(GPG_PASSPHRASE)"
  privateKey: "ENV(GPG_PRIVATE_KEY)"
```

### 3\. Configure Your Build Script

Finally, configure the `unipub` extension in your `build.gradle.kts`. Here you will define all the public metadata for
your project, such as its name, description, developers, and license.

<details>
<summary>Minimal `build.gradle.kts` setup</summary>

```kotlin
// build.gradle.kts

import dev.mtctx.unipub.License

plugins {
    // other plugins
    id("dev.mtctx.unipub") version "LATEST_VERSION"
}

unipub {
    // If you used a custom name or location for the settings file
    // uniPubSettingsFile.set("/path/to/your/custom.unipub")
    // it defaults to main.unipub in your user home directory.

    project {
        name = "My Awesome Library"
        id = "awesome-lib"
        description = "A library that does awesome things."
        inceptionYear = "2025"
        url = "https://github.com/example/awesome-lib"

        licenses = listOf(License.APACHE_2_0)

        scm {
            url = "https://github.com/example/awesome-lib"
        }
    }

    developers {
        developer {
            name = "Jane Doe"
            email = "jane.doe@example.com"
        }
    }
}
```

</details>

<details>
<summary>Full-featured `build.gradle.kts` setup (almost)</summary>

```kotlin
// build.gradle.kts
import dev.mtctx.unipub.License

plugins {
    // other plugins
    id("dev.mtctx.unipub") version "LATEST_VERSION"
}

unipub {
    // Custom settings file location (optional)
    uniPubSettingsFile.set("${project.projectDir}/config/production.unipub")

    project {
        name = "ENV(PROJECT_NAME)" // Using environment variable
        id = "awesome-library"
        description = "A comprehensive library providing advanced distributed systems functionality"
        version = "2.0.0" // Override project version
        inceptionYear = "2023"
        groupId = "com.corporate.awesome" // Override project group
        url = "https://github.com/corporate/awesome-library"

        // Multiple licenses
        licenses = listOf(
            License.APACHE_2_0,
            License.MIT
        )

        scm {
            url = "https://github.com/corporate/awesome-library"
            connection = "scm:git:git@github.com:corporate/awesome-library.git"
            developerConnection = "scm:git:ssh://git@github.com:corporate/awesome-library.git"
        }
    }

    developers {
        developer {
            name = "Dr. Jane Smith"
            email = "jane.smith@corporate.com"
            organization = "Awesome Corp R&D"
            organizationUrl = "https://rd.awesome-corp.com"
        }

        developer {
            name = "John Developer"
            email = "john.developer@corporate.com"
            organization = "Awesome Corp Engineering"
            organizationUrl = "https://engineering.awesome-corp.com"
        }
    }

    artifacts {
        // Multiple components
        component("java")
        component("kotlin")

        // Standard tasks
        task(tasks.named("sourcesJar"))
        task(tasks.named("javadocJar"))

        // Custom file artifact
        file(file("docs/API_GUIDE.md"), "documentation")

        // Custom artifact configuration
        custom(classifier = "metrics", extension = "json") { publication ->
            publication.artifact(tasks.named("generateMetrics")) {
                classifier = "performance-metrics"
                extension = "json"
            }
        }
    }
}
```

</details>

With this configuration in place, you can now publish your project by running the standard Gradle `publish` task.

-----

## How It Works

UniPub automates the configuration of Gradle's built-in `maven-publish` and `signing` plugins.

1. It reads the project metadata you defined in the `unipub` extension block.
2. It parses the specified `.unipub` settings file to load repository credentials and GPG signing keys.
3. It dynamically configures the `MavenPublication` with all the necessary POM details (name, description, licenses,
   developers, SCM).
4. It sets up the repositories for publishing.
5. It configures the `signing` plugin to use your GPG key from memory to sign the publications.

This approach keeps your build script clean and your secrets secure while handling the boilerplate of publishing
configuration for you.

-----

## License

This project is licensed under the terms of the **GNU General Public License v3.0**. For more details, see
the [GNU General Public License v3.0](LICENSE).