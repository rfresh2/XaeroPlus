plugins {
    java
    id("maven-publish")
    id("architectury-plugin")
}

configure<BasePluginExtension> {
    archivesName = "XaeroPlus"
}

version = gradle.extra.get("mod_version").toString()
group = "xaeroplus"

repositories {
    maven("https://maven.neoforged.net/releases/") {
        name = "NeoForge"
    }
    maven("https://api.modrinth.com/maven") {
        name = "Modrinth"
    }
    maven("https://maven.2b2t.vc/releases") {
        name = "maven.2b2t.vc"
    }
    maven("https://maven.parchmentmc.org") {
        name = "ParchmentMC"
    }
    maven("https://maven.lenni0451.net/releases") {
        name = "Lenni0451"
    }
    maven("https://maven.shedaniel.me/")
    maven("https://maven.terraformersmc.com/releases/")
    mavenLocal()
}

tasks {
    withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.release = 21
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}
