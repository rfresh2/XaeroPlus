plugins {
    `kotlin-dsl`
}

repositories {
    maven("https://maven.fabricmc.net/") { name = "Fabric" }
    maven("https://maven.architectury.dev/")
    maven("https://files.minecraftforge.net/maven/")
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation("architectury-plugin:architectury-plugin.gradle.plugin:3.4-SNAPSHOT")
    implementation("dev.architectury:architectury-loom:1.7-SNAPSHOT")
    implementation("com.gradleup.shadow:shadow-gradle-plugin:8.3.5")
}

