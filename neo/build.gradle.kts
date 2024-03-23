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

val worldmap_version_forge: String by gradle.extra
val minimap_version_forge: String by gradle.extra
val minecraft_version: String by gradle.extra
val parchment_version: String by gradle.extra
val loader_version: String by gradle.extra
val neoforge_version: String by gradle.extra
val destArchiveVersion = "${project.version}+${loom.platform.get().id()}-${minecraft_version}"
val destArchiveClassifier = "WM${worldmap_version_forge}-MM${minimap_version_forge}"

sourceSets.main.get().java.srcDir(common.layout.buildDirectory.get().asFile.path + "/remappedSources/forge/java")
sourceSets.main.get().resources.srcDir(common.layout.buildDirectory.get().asFile.path + "/remappedSources/forge/resources")

dependencies {
    neoForge(libs.neoforge)
    modImplementation(libs.worldmap.neo)
    modImplementation(libs.minimap.neo)
    modImplementation(files("../forge/libs/baritone-unoptimized-forge-1.10.2.jar"))
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
                "worldmap_version" to worldmap_version_forge,
                "minimap_version" to minimap_version_forge
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
        archiveVersion = destArchiveVersion
        archiveClassifier = destArchiveClassifier
    }

    shadowJar {
        configurations = listOf(project.configurations.shadow.get())
    }

    remapJar {
        dependsOn(shadowJar, transformNeo)
        inputFile.set(shadowJar.get().archiveFile.get())
        archiveVersion = destArchiveVersion
        archiveClassifier = destArchiveClassifier
        atAccessWideners.add(loom.accessWidenerPath.get().asFile.name)
    }
}
