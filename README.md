# UniPub » Simplifying Maven Publishing

UniPub (Unified Publishing) is a **Gradle plugin** designed to streamline the process of publishing artifacts to Maven
repositories. It simplifies the configuration by separating **project metadata** from **sensitive credentials**, using a
clean Kotlin DSL in your build script and a dedicated settings file for secrets.

-----

## Features

* **Secure Credential Handling**: Stores repository credentials and GPG keys in a separate, non-version-controlled YAML
  file (`.unipub`).
* **Environment Variable Support**: Natively supports referencing secrets via the validated **`ENV(VARIABLE_NAME)`**
  syntax.
* **Automatic POM Generation**: Dynamically configures the Maven POM with project details, licenses, developers, and SCM
  information defined in the build script.
* **GPG Signing**: Automates the signing process using your GPG key, with an option to load the private key **in-memory
  ** from the settings file for better security on CI systems using `useInMemoryPgpKey()`.
* **Flexible Artifact Configuration**: Easily include components (like `java` or `kotlin`), output from specific Gradle
  tasks, custom files, or define entirely custom artifacts.

-----

## How to Set Up

Setting up UniPub involves three main steps: applying the plugin, creating a settings file for your credentials, and
configuring your project's metadata in the build script.

### 1\. Apply the Plugin

First, apply the UniPub plugin in your `build.gradle.kts` file.

```kotlin
// build.gradle.kts
plugins {
    id("dev.mtctx.unipub") version "LATEST_VERSION" // Check for the latest version
}
```

**Note:** Find the **latest version** on the [Gradle Plugin Portal](https://plugins.gradle.org/plugin/dev.mtctx.unipub).

### 2\. Create the Settings File

UniPub uses a YAML file (default is `main.unipub`) to store sensitive data like repository credentials and GPG keys,
keeping them out of your version-controlled build scripts.

You can generate a template for this file by running one of the provided Gradle tasks:

* **For a project-specific file:** `./gradlew generateSettingsFileInProjectDir`
    * This creates **`main.unipub`** in your project's root directory and automatically adds it to your **`.gitignore`
      **.
* **For a global file:** `./gradlew generateSettingsFileInHomeDir`
    * This creates **`main.unipub`** in the **`~/.unipub/`** directory in your user home folder.

**Note:** The default profile name is `main`. You can change it by adding the **`-PprofileName=<name>`** property to the
command (e.g., `./gradlew generateSettingsFileInProjectDir -PprofileName=staging`). Use the **`-Poverwrite=true`**
property to overwrite an existing settings file.

After generating the file, you must edit it to add your actual credentials. For security, it's highly recommended to
store secrets in environment variables and reference them using the **`ENV()`** syntax.

**Example `main.unipub` file:**

```yaml
# You can use ENV(VARIABLE_NAME) to refer to environment variables.
repositories:
  - name: "maven-central"
    url: "https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/"
    username: "ENV(OSSRH_USERNAME)"
    password: "ENV(OSSRH_PASSWORD)"
gpgKey: # Required only if you use the useInMemoryPgpKey() feature
  keyId: "ENV(GPG_KEY_ID)"
  passphrase: "ENV(GPG_PASSPHRASE)"
  privateKey: "ENV(GPG_PRIVATE_KEY)"
```

### 3\. Configure Your Build Script

Finally, configure the `unipub` extension in your `build.gradle.kts`. Here you will define all the public metadata and
artifact details for your project.

\<details\>
\<summary\>Minimal `build.gradle.kts` setup\</summary\>

```kotlin
// build.gradle.kts

import dev.mtctx.unipub.License

plugins {
    // other plugins
    id("dev.mtctx.unipub") version "LATEST_VERSION"
}

unipub {
    // Optional: Use the private key from the .unipub file to sign publications in memory
    // useInMemoryPgpKey()

    // Optional: Manually set the path to your settings file (defaults to ~/.unipub/main.unipub)
    // uniPubSettingsFile.set("/path/to/your/custom.unipub")

    project {
        name = "My Awesome Library"
        id = "awesome-lib"
        description = "A library that does awesome things."
        inceptionYear = "2025"
        url = "https://github.com/example/awesome-lib"

        licenses(License.APACHE_2_0)

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

\</details\>

\<details\>
\<summary\>Full-featured `build.gradle.kts` setup (almost)\</summary\>

```kotlin
// build.gradle.kts
import dev.mtctx.unipub.License

plugins {
    // other plugins
    id("dev.mtctx.unipub") version "LATEST_VERSION"
}

unipub {
    // Use in-memory signing (requires 'gpgKey' in .unipub file)
    useInMemoryPgpKey()

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
        licenses(License.APACHE_2_0, License.MIT)

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

\</details\>

With this configuration in place, you can now publish your project by running the standard Gradle `publish` task.

-----

## How It Works

UniPub automates the configuration of Gradle's built-in `maven-publish` and `signing` plugins.

1. It reads the project metadata and artifact configuration you defined in the **`unipub`** extension block.
2. It parses the specified **`.unipub`** settings file to load repository credentials and GPG signing keys, securely
   resolving any **`ENV()`** variables.
3. It dynamically configures the `MavenPublication` with all the necessary POM details (name, description, licenses,
   developers, SCM).
4. It sets up the repositories for publishing.
5. It configures the **`signing`** plugin to use either your local GPG agent (`useGpgCmd()`) or your GPG key loaded from
   memory (`useInMemoryPgpKeys()`) to sign the publications.

This approach keeps your build script clean and your secrets secure while handling the boilerplate of publishing
configuration for you.

-----

## Documentation

* **API Documentation:** Explore the full Kotlin DSL reference on the [API Docs](https://unipub.apidoc.mtctx.dev).
* **Latest Plugin Version:** Check for the most recent release on
  the [Gradle Plugin Portal](https://plugins.gradle.org/plugin/dev.mtctx.unipub).

-----

## License

This project is licensed under the terms of the **GNU General Public License v3.0**. For more details, see
the [GNU General Public License v3.0](https://www.google.com/search?q=LICENSE).