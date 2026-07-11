plugins {
    java
    id("maven-publish")
    id("architectury-plugin")
}

configure<BasePluginExtension> {
    archivesName = "XaeroPlus"
}

version = providers.gradleProperty("mod_version").get()
group = "xaeroplus"

repositories {
    maven("https://maven.fabricmc.net/")
    maven("https://maven.neoforged.net/releases/")
    maven("https://api.modrinth.com/maven") {
        content {
            includeGroup("maven.modrinth")
        }
    }
    maven("https://maven.2b2t.vc/releases")
    maven("https://maven.2b2t.vc/xaero")
    maven("https://maven.parchmentmc.org") {
        content {
            includeGroup("org.parchmentmc.data")
        }
    }
    maven("https://maven.2b2t.vc/remote")
    maven("https://cursemaven.com") {
        content {
            includeGroup("curse.maven")
        }
    }
}

tasks {
    withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.release = 17
        options.compilerArgs.add("-parameters")
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
