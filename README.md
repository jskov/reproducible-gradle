# buildinfo-gradle
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=jskov_buildinfo-gradle&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=jskov_buildinfo-gradle)
[![Reproducible Builds](https://img.shields.io/badge/Reproducible_Builds-ok-success?labelColor=1e5b96)](https://github.com/jvm-repo-rebuild/reproducible-central#dk.mada.buildinfo:buildinfo-gradle)

A small Gradle plugin that enables [reproducible builds](https://reproducible-builds.org/) by [configuring archive tasks](https://docs.gradle.org/current/userguide/working_with_files.html#sec:reproducible_archives) to be reproducible.

The only thing this Plugin provides is a slight simplification of the build files.

## Using the Plugin

Add plugin activation to the build file header (substitute the relevant version):

    plugins {
        ...
        id 'dk.mada.buildinfo' version '1.n.n'
    }

And make sure the plugin can be fetched from MavenCentral:

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
