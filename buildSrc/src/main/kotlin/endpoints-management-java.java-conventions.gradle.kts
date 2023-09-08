// Configures common Java settings

plugins {
    `java-library`
    jacoco
}

repositories {
    mavenLocal()
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
    withJavadocJar()
    withSourcesJar()
}

tasks.jacocoTestReport {
    reports {
        xml.isEnabled = true
    }
}
