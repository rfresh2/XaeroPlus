plugins {
    java
    id("maven-publish")
    id("architectury-plugin")
}

configure<BasePluginExtension> {
    archivesName = "XaeroPlus"
}

version = project.properties["mod_version"].toString()
group = "xaeroplus"

repositories {
    maven("https://maven.neoforged.net/releases/")
    maven("https://api.modrinth.com/maven")
    maven("https://maven.2b2t.vc/releases")
    maven("https://maven.2b2t.vc/xaero")
    maven("https://maven.parchmentmc.org")
    maven("https://maven.shedaniel.me/")
    maven("https://maven.2b2t.vc/remote")
    maven("https://cursemaven.com")
}

tasks {
    withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.release = 17
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
