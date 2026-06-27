import dev.architectury.plugin.TransformingTask
import dev.architectury.transformer.transformers.TransformNeoForgeAnnotations
import dev.architectury.transformer.transformers.TransformNeoForgeEnvironment
import dev.architectury.transformer.transformers.TransformPlatformOnly

plugins {
    id("xaeroplus-all.conventions")
    id("xaeroplus-platform.conventions")
}

architectury {
    platformSetupLoomIde()
    neoForge()
}

val common = project(":common")

loom {
    neoForge {
        accessWidenerPath.set(common.loom.accessWidenerPath)
    }
}

val worldmap_version_neo = providers.gradleProperty("worldmap_version_neo").get()
val minimap_version_neo = providers.gradleProperty("minimap_version_neo").get()
val minecraft_version = providers.gradleProperty("minecraft_version").get()
val destArchiveVersion = "${project.version}+${loom.platform.get().id()}-${minecraft_version}"
val destArchiveClassifier = "WM${worldmap_version_neo}-MM${minimap_version_neo}"

sourceSets.main.get().java.srcDir(common.layout.buildDirectory.get().asFile.path + "/remappedSources/forge/java")
sourceSets.main.get().resources.srcDir(common.layout.buildDirectory.get().asFile.path + "/remappedSources/forge/resources")

dependencies {
    neoForge(libs.neoforge)
    modImplementation(libs.worldmap.neo) { isTransitive = false }
    modImplementation(libs.minimap.neo) { isTransitive = false }
    modImplementation(libs.xaerolib.neo)
    modCompileOnly(libs.baritone.neoforge)
    modCompileOnly(libs.waystones.neoforge)
    modCompileOnly(libs.balm.neoforge)
    modCompileOnly(libs.worldtools)
    modCompileOnly(libs.fabric.waystones)
    modCompileOnly(libs.sodium.neoforge)
    modRuntimeOnly(libs.immediatelyfast.neo)
    shadow(libs.sqlite)
    forgeRuntimeLibrary(implementation(shadow(libs.caffeine.get())!!)!!)
    forgeRuntimeLibrary(implementation(shadow(libs.lambdaEvents.get())!!)!!)
    forgeRuntimeLibrary(implementation(shadow(libs.oldbiomes.get())!!)!!)
    compileOnly(project(":common"))
}

tasks {
    processResources {
        filesMatching("META-INF/neoforge.mods.toml") {
            expand(mapOf(
                "version" to project.version,
                "worldmap_version" to worldmap_version_neo,
                "minimap_version" to minimap_version_neo
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
        val shadePkg = "xaeroplus.shadow"
        relocate("kaptainwutax", "$shadePkg.kaptainwutax")
        relocate("net.lenni0451.lambdaevents", "$shadePkg.lambdaevents")
        relocate("com.github.benmanes.caffeine", "$shadePkg.caffeine")
    }

    remapJar {
        dependsOn(shadowJar, transformNeo)
        inputFile.set(shadowJar.get().archiveFile.get())
        archiveVersion = destArchiveVersion
        archiveClassifier = destArchiveClassifier
        atAccessWideners.add(loom.accessWidenerPath.get().asFile.name)
    }

    compileJava {
        dependsOn(common.tasks.getByName("remapForge"))
    }
}
