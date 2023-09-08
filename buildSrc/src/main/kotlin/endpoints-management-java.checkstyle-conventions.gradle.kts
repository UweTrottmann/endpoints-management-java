// Configures common Checkstyle settings

plugins {
    checkstyle
}

checkstyle {
    configFile = rootProject.file("config/checkstyle/checkstyle.xml")
    toolVersion = "6.19"
}

tasks.withType(Checkstyle::class.java).configureEach {
    doLast {
        reports.all {
            val outputFile = destination
            if (outputFile.exists() && outputFile.readText().contains("<error ")) {
                outputs.upToDateWhen { false }
                throw GradleException("There were checkstyle warnings! For more info check $outputFile")
            }
        }
    }
}
