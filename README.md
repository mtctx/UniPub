# UniPub » Secure Secrets for Maven Publishing

UniPub (Unified Publishing) is a **Gradle plugin** designed to make publishing to Maven repositories safer and cleaner.
Instead of wiring credentials directly into your `build.gradle.kts`, UniPub keeps **sensitive data separate** and
injects it automatically at publish time.

Think of it as a **secrets bridge** on top of existing publishing plugins like Gradle’s built-in `maven-publish`
or [vanniktech’s maven publish plugin](https://github.com/vanniktech/gradle-maven-publish-plugin).

---

## Features

* **Secure Credential Handling**: Reads repository credentials and signing keys from a non-version-controlled YAML file.
* **Environment Variable Support**: Supports `ENV(VARIABLE_NAME)` syntax to load secrets directly from your environment.
* **Profile-based Configuration**: Use multiple profiles (e.g., `main`, `staging`, `ci`) without touching build logic.
* **Automatic Injection**: Intercepts `publish` tasks (`maven-publish`, vanniktech, etc.) and injects the correct
  credentials.
* **GPG Signing Support**: Optionally loads GPG keys in-memory for CI/CD pipelines (`useInMemoryPgpKey()`).
* **Tasks for Setup**: Generate ready-to-edit `.unipub.yml` templates in your project or home directory, with
  `.gitignore` safety built in.

---

## How to Set Up

### 1. Apply the Plugin

```kotlin
plugins {
    id("dev.mtctx.unipub") version "LATEST_VERSION"
}
```

👉 Check the [Gradle Plugin Portal](https://plugins.gradle.org/plugin/dev.mtctx.unipub) for the latest version.

---

### 2. Create the Settings File

Generate a starter template:

```bash
./gradlew generateUniPubFileInProjectDir   # creates ./unipub.yml (gitignored)
./gradlew generateUniPubFileInHomeDir      # creates ~/.unipub.yml
```

Example `unipub.yml`:

```yaml
profiles:
  - name: "main"
    username: "ENV(OSSRH_USERNAME)"
    password: "ENV(OSSRH_PASSWORD)"
```

---

### 3. Configure Your Project

Use your publishing plugin of choice (`maven-publish`, vanniktech, etc.).
UniPub will inject the credentials automatically when you run `publish`.

Minimal example:

```kotlin
plugins {
    `maven-publish`
    id("dev.mtctx.unipub") version "LATEST_VERSION"
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }
    repositories {
        maven {
            name = "mavenCentral"
            url = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
        }
    }
}

unipub {
    profileName.set("main") // optional, defaults to "primary"
}
```

---

## How It Works

1. UniPub loads your secrets from `unipub.yml` (project-local or global).
2. On `PublishToMavenRepository` tasks, UniPub checks the target repository.
3. If credentials are missing, UniPub injects `username`/`password` from your profile.

Your build scripts stay clean — no more hardcoded secrets.

---

## Documentation

* **API Reference:** [API Docs](https://unipub.apidoc.mtctx.dev)
* **Latest Version:** [Gradle Plugin Portal](https://plugins.gradle.org/plugin/dev.mtctx.unipub)

---

## License

This project is licensed under [**GNU GPL v3.0**](https://www.gnu.org/licenses/gpl-3.0.html).