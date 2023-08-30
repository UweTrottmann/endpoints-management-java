// Configures common publishing settings

plugins {
    id("maven-publish")
    id("signing")
}

// Make javadoc task errors not break the build.
if (JavaVersion.current().isJava8Compatible) {
    tasks.withType<Javadoc> {
        isFailOnError = false
    }
}

publishing {
    // Note: Sonatype repo created by publish-plugin, see root build.gradle.

    publications {
        create<MavenPublication>("mavenJava") {
            // Note: Projects set additional specific properties.
            pom {
                packaging = "jar"
                url.set("https://github.com/UweTrottmann/endpoints-management-java/")

                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                        distribution.set("repo")
                    }
                }

                developers {
                    developer {
                        name.set("Uwe Trottmann")
                        email.set("uwe@uwetrottmann.com")
                        organization.set("Uwe Trottmann")
                        organizationUrl.set("https://www.uwetrottmann.com")
                    }
                }

                scm {
                    connection.set("scm:git@github.com:UweTrottmann/endpoints-management-java.git")
                    developerConnection.set("scm:git@github.com:UweTrottmann/endpoints-management-java.git")
                    url.set("https://github.com/UweTrottmann/endpoints-management-java/")
                }
            }
        }
    }
}

signing {
    // https://docs.gradle.org/current/userguide/signing_plugin.html#sec:using_gpg_agent
    useGpgCmd()
    sign(publishing.publications["mavenJava"])
}
