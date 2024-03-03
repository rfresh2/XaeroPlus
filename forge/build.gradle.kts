import dev.architectury.plugin.TransformingTask
import dev.architectury.plugin.transformers.AddRefmapName
import dev.architectury.transformer.transformers.BuiltinProperties
import dev.architectury.transformer.transformers.FixForgeMixin
import dev.architectury.transformer.transformers.TransformForgeAnnotations
import dev.architectury.transformer.transformers.TransformForgeEnvironment

plugins {
    id("dev.architectury.loom") version "1.5-SNAPSHOT"
    id("maven-publish")
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

architectury {
    platformSetupLoomIde()
    forge()
}

loom {
    accessWidenerPath = project(":common").loom.accessWidenerPath
    forge {
        mixinConfigs.set(listOf("xaeroplus.mixins.json", "xaeroplus-forge.mixins.json"))
        convertAccessWideners = true
        extraAccessWideners.add(loom.accessWidenerPath.get().asFile.name)
        mixin {
            defaultRefmapName.set("xaeroplus-refmap.json")
        }
    }
}

val worldmap_version: String by rootProject
val minimap_version: String by rootProject
val minecraft_version: String by rootProject
val parchment_version: String by rootProject
val loader_version: String by rootProject
val forge_version: String by rootProject

sourceSets.main.get().java.srcDir(project(":common").layout.buildDirectory.get().asFile.path + "/remappedSources/forge/java")
sourceSets.main.get().resources.srcDir(project(":common").layout.buildDirectory.get().asFile.path + "/remappedSources/forge/resources")

dependencies {
    forge("net.minecraftforge:forge:${forge_version}")
    implementation(include("com.github.ben-manes.caffeine:caffeine:3.1.8")!!)
    implementation(include("net.lenni0451:LambdaEvents:2.4.1")!!)
    implementation(annotationProcessor("io.github.llamalad7:mixinextras-common:0.3.5")!!)
    implementation(include("io.github.llamalad7:mixinextras-forge:0.3.5")!!)
    modImplementation("maven.modrinth:xaeros-world-map:${worldmap_version}_Forge_1.20")
    modImplementation("maven.modrinth:xaeros-minimap:${minimap_version}_Forge_1.20")
    modImplementation(files("libs/baritone-unoptimized-forge-1.10.1.jar"))
    modCompileOnly("maven.modrinth:fwaystones:3.1.3+mc1.20")
    modCompileOnly("maven.modrinth:waystones:14.0.2+forge-1.20")
    modCompileOnly("maven.modrinth:balm:7.1.4+forge-1.20.1")
    modCompileOnly("maven.modrinth:worldtools:1.2.0+1.20.1")
//    modRuntimeOnly("maven.modrinth:immediatelyfast:1.2.10+1.20.4-forge")
    // add remapped sources in build/remappedSources to the compile classpath
    compileOnly(project(":common"))
}

tasks {
    processResources {
        filesMatching("META-INF/mods.toml") {
            expand(mapOf(
                "version" to project.version,
                "worldmap_version" to worldmap_version,
                "minimap_version" to minimap_version
            ))
        }
    }

    val transformForge = register("transformForge", TransformingTask::class.java) {
        group = "build"
        input.set(shadowJar.get().archiveFile)
        platform = loom.platform.get().name
//    it.add(new RemapInjectables(), new Function2<Map<String, Object>, java.nio.file.Path, Unit>() {
//        @Override
//        Unit invoke(final Map<String, Object> stringObjectMap, final java.nio.file.Path path) {
//            this[BuiltinProperties.UNIQUE_IDENTIFIER] = "example_mod"
//            return null
//        }
//    })
        System.setProperty(BuiltinProperties.REFMAP_NAME, "xaeroplus-refmap.json")
        transformers.add(AddRefmapName())
        transformers.add(TransformForgeAnnotations())
        transformers.add(TransformForgeEnvironment())
        transformers.add(FixForgeMixin())
        loom.setGenerateSrgTiny(true)
    }

    shadowJar {
        configurations = listOf(project.configurations.shadow.get())
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

    remapJar {
        dependsOn(shadowJar, transformForge)
        inputFile.set(shadowJar.get().archiveFile.get())
    }
}
