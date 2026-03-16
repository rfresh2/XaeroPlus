plugins {
    `kotlin-dsl`
}

repositories {
    maven("https://maven.fabricmc.net/")
    maven("https://maven.neoforged.net/releases/")
    maven("https://files.minecraftforge.net/maven/")
    maven("https://maven.2b2t.vc/remote")
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation("gg.essential.loom:gg.essential.loom.gradle.plugin:1.15.45")
    implementation("architectury-plugin:architectury-plugin.gradle.plugin:3.4-SNAPSHOT")
    implementation("com.gradleup.shadow:shadow-gradle-plugin:9.4.0")
}

