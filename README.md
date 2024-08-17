# reproducible-gradle
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=jskov_reproducible-gradle&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=jskov_reproducible-gradle)
[![Reproducible Builds](https://img.shields.io/endpoint?url=https://raw.githubusercontent.com/jvm-repo-rebuild/reproducible-central/master/content/dk/mada/reproducible/reproducible-gradle/badge.json)](https://github.com/jvm-repo-rebuild/reproducible-central/blob/master/content/dk/mada/reproducible/reproducible-gradle/README.md)

A small Gradle plugin that enables [reproducible builds](https://reproducible-builds.org/) by [configuring archive tasks](https://docs.gradle.org/current/userguide/working_with_files.html#sec:reproducible_archives) to be reproducible.

The only thing this Plugin provides is a slight simplification of the build files.

## Using the Plugin

Add plugin activation to the build file header (substitute the relevant version):

    plugins {
        ...
        id 'dk.mada.reproducible' version '1.n.n'
    }

And make sure the plugin can be fetched from MavenCentral by adding this to `settings.gradle`:

    pluginManagement {
        repositories {
            gradlePluginPortal()
            mavenCentral()
        }
    }

## Development

For testing snapshot builds in other projects:

```console
$ ./gradlew -t publishToMavenLocal -Pversion=0.0.1
```
