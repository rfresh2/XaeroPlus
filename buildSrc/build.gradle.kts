plugins {
    `kotlin-dsl`
}

repositories {
    maven("https://maven.fabricmc.net/")
    maven("https://maven.architectury.dev/")
    maven("https://maven.neoforged.net/releases/")
    maven("https://files.minecraftforge.net/maven/")
    maven("https://maven.2b2t.vc/remote")
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation("architectury-plugin:architectury-plugin.gradle.plugin:3.4-SNAPSHOT")
    implementation("dev.architectury:architectury-loom:1.13-SNAPSHOT")
    implementation("com.gradleup.shadow:shadow-gradle-plugin:9.3.0")
}

