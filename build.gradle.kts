import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import net.fabricmc.loom.api.LoomGradleExtensionAPI

plugins {
    id("architectury-plugin") version "3.4-SNAPSHOT"
    id("dev.architectury.loom") version "1.5-SNAPSHOT" apply false
    id("com.github.johnrengelman.shadow") version "8.1.1" apply false
}

val minecraft_version: String by rootProject
val parchment_version: String by rootProject
val worldmap_version: String by rootProject
val minimap_version: String by rootProject

architectury {
    minecraft = minecraft_version
}

subprojects {
    apply(plugin = "dev.architectury.loom")
    apply(plugin = "com.github.johnrengelman.shadow")

    configure<LoomGradleExtensionAPI> {
        silentMojangMappingsLicense()
        runs {
            getByName("client") {
                programArgs("--username", "rfresh2")
            }
        }
    }

    dependencies {
        "minecraft"("com.mojang:minecraft:${minecraft_version}")
        "mappings"((project.extensions.getByType(LoomGradleExtensionAPI::class)).layered {
            officialMojangMappings()
            parchment("org.parchmentmc.data:parchment-${minecraft_version}:${parchment_version}@zip")
        })
    }

    tasks {
        getByName<ShadowJar>("shadowJar") {
            archiveClassifier.set("shadow")
            exclude("com/google/**")
            exclude("org/objectweb/**")
            exclude("org/checkerframework/**")
            exclude("org/sqlite/native/FreeBSD/**")
            exclude("org/sqlite/native/Linux-Android/**")
            exclude("org/sqlite/native/Linux-Musl/**")
            exclude("org/sqlite/native/Linux/arm/**")
            exclude("org/sqlite/native/Linux/aarch64/**")
            exclude("org/sqlite/native/Linux/armv6/**")
            exclude("org/sqlite/native/Linux/x86/**")
            exclude("org/sqlite/native/Linux/armv7/**")
            exclude("org/sqlite/native/Linux/ppc64/**")
            exclude("org/sqlite/native/Windows/armv7/**")
            exclude("org/sqlite/native/Windows/aarch64/**")
            exclude("org/sqlite/native/Windows/armv7/**")
            exclude("org/slf4j/**")
        }
    }
}

allprojects {
    apply(plugin = "java")
    apply(plugin = "architectury-plugin")
    apply(plugin = "maven-publish")

    configure<BasePluginExtension> {
        archivesName = rootProject.properties["archives_base_name"].toString()
    }

    version = rootProject.properties["mod_version"].toString()
    group = rootProject.properties["maven_group"].toString()

    repositories {
        maven("https://api.modrinth.com/maven") {
            name = "Modrinth"
        }
        maven("https://jitpack.io") {
            name = "jitpack.io"
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
            options.release = 17
        }
    }

    configure<JavaPluginExtension> {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

tasks {
    register("printWorldMapVersion") {
        println(worldmap_version)
    }
    register("printMinimapVersion") {
        println(minimap_version)
    }
    register("printXaeroPlusVersion") {
        println(version)
    }
}
