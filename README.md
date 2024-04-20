# buildinfo-gradle
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=jskov_buildinfo-gradle&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=jskov_buildinfo-gradle)

A Gradle plugin that enables [reproducible builds](https://reproducible-builds.org/) by providing [buildinfo](https://reproducible-builds.org/docs/jvm) when publishing Maven artifacts.

The plugin also configurs [reproducible archive tasks](https://docs.gradle.org/current/userguide/working_with_files.html#sec:reproducible_archives).

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

## Open Source vs Expectancy of Work

This project is Open Source and I am happy for you to use the plugin, report bugs and suggest changes.

But while the source code is free, it does not come bundled with promises or guarantees of free work.

I will try to fix reported bugs (if I agree with them), but will commit to no time tables.

If you are itching to make some changes (to this repository), please open an issue first, so we can discuss.  
I do not want you to waste your time!

If this is not agreeable, you are more than welcome to fork the project and do your own thing.

Open Source, Yay!

## Development

For testing snapshot builds in other projects:

```console
$ ./gradlew -t publishToMavenLocal -Pversion=0.0.1
```
