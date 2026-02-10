pluginManagement {
    repositories {
        maven("https://nexus.gtnewhorizons.com/repository/public/") {
            content {
                includeGroup("com.gtnewhorizons")
                includeGroup("com.gtnewhorizons.retrofuturagradle")
            }
        }
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "XaeroPlus"
