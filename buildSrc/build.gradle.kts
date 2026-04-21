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
    implementation("dev.architectury:at:1.0.1")
    implementation("gg.essential.loom-no-remap:gg.essential.loom-no-remap.gradle.plugin:1.15.50")
    implementation("architectury-plugin:architectury-plugin.gradle.plugin:3.5-SNAPSHOT")
    implementation("com.gradleup.shadow:shadow-gradle-plugin:9.4.1")
}
