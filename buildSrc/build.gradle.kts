plugins {
    `kotlin-dsl`
}

repositories {
    maven("https://maven.fabricmc.net/")
    maven("https://maven.neoforged.net/releases/")
    maven("https://files.minecraftforge.net/maven/")
    maven("https://maven.2b2t.vc/remote")
    mavenCentral()
    mavenLocal()
    gradlePluginPortal()
}

dependencies {
    implementation("dev.architectury:architectury-loom:1.17-SNAPSHOT")
    implementation("architectury-plugin:architectury-plugin.gradle.plugin:3.5.167")
    implementation("com.gradleup.shadow:shadow-gradle-plugin:9.4.2")
}

