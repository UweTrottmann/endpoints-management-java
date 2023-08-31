# Overview

Most of the release process is handled by the maven-publish and nexus plugin tasks. The tasks can be triggered by
running "./gradlew publishToSonatype closeAndReleaseSonatypeStagingRepository" from the project's directory.
A new artifact will be uploaded to the staging repository in Sonatype when "-SNAPSHOT" is not included in the version.
When "-SNAPSHOT" is included, the task only updates the artifact in the snapshot repository.

# Release Instructions

Sign up for Maven Central
------------------------------
See [the Central website](https://central.sonatype.org/) for specifics.

Add deploy credential settings
------------------------
* Create a settings file at ```$HOME/.gradle/gradle.properties``` with your sonatype username/password

```
SONATYPE_NEXUS_USERNAME=<YOUR-NEXUS-USERNAME>
SONATYPE_NEXUS_PASSWORD=<YOUR-NEXUS-PASSWORD>
```

* A [working gpg-agent setup](https://docs.gradle.org/current/userguide/signing_plugin.html#sec:using_gpg_agent) is expected.

Publish to Central
------------------

* Update `version` in all ```build.gradle``` files in the project to the release version you want.
* ```./gradlew publishToSonatype closeAndReleaseSonatypeStagingRepository``` to release all artifacts of all subprojects.
  * If only some subprojects are updated, execute the tasks for those subprojects only.
* Update `version` in all ```build.gradle``` files to the new snapshot version.
