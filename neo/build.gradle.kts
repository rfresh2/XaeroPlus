import dev.architectury.plugin.TransformingTask
import dev.architectury.transformer.transformers.TransformNeoForgeAnnotations
import dev.architectury.transformer.transformers.TransformNeoForgeEnvironment
import dev.architectury.transformer.transformers.TransformPlatformOnly

architectury {
    platformSetupLoomIde()
    neoForge()
}

val common = project(":common")

loom {
    neoForge {
        accessWidenerPath.set(common.loom.accessWidenerPath)
    }
    runs {
        getByName("client") {
            ideConfigGenerated(true)
            client()
        }
    }
}

// todo: reset when other loaders mods are updated
val worldmap_version = "1.38.0"
val minimap_version = "24.0.0"
//val worldmap_version: String by rootProject
//val minimap_version: String by rootProject
val minecraft_version: String by rootProject
val parchment_version: String by rootProject
val loader_version: String by rootProject
val neoforge_version: String by rootProject

sourceSets.main.get().java.srcDir(common.layout.buildDirectory.get().asFile.path + "/remappedSources/forge/java")
sourceSets.main.get().resources.srcDir(common.layout.buildDirectory.get().asFile.path + "/remappedSources/forge/resources")

dependencies {
    neoForge("net.neoforged:neoforge:${neoforge_version}")
    modImplementation("maven.modrinth:xaeros-world-map:${worldmap_version}_NeoForge_1.20.4")
    modImplementation("maven.modrinth:xaeros-minimap:${minimap_version}_NeoForge_1.20.4")
    modImplementation(files("libs/baritone-unoptimized-neoforge-1.10.2.jar"))
    modCompileOnly(libs.waystones.neoforge)
    modCompileOnly(libs.balm.neoforge)
    modCompileOnly(libs.worldtools)
    modCompileOnly(libs.fabric.waystones)
    shadow(libs.sqlite)
    forgeRuntimeLibrary(implementation(include(libs.caffeine.get())!!)!!)
    forgeRuntimeLibrary(implementation(include(libs.lambdaEvents.get())!!)!!)
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

    val transformNeo = register("transformNeo", TransformingTask::class.java) {
        group = "build"
        input.set(shadowJar.get().archiveFile)
        platform = loom.platform.get().name
        transformers.add(TransformPlatformOnly())
        transformers.add(TransformNeoForgeAnnotations())
        transformers.add(TransformNeoForgeEnvironment())
    }

    shadowJar {
        configurations = listOf(project.configurations.shadow.get())
    }

    remapJar {
        dependsOn(shadowJar, transformNeo)
        inputFile.set(shadowJar.get().archiveFile.get())
    }
}
