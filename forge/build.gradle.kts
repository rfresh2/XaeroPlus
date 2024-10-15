import dev.architectury.plugin.TransformingTask
import dev.architectury.plugin.transformers.AddRefmapName
import dev.architectury.transformer.transformers.FixForgeMixin
import dev.architectury.transformer.transformers.TransformForgeAnnotations
import dev.architectury.transformer.transformers.TransformForgeEnvironment

plugins {
    id("xaeroplus-all.conventions")
    id("xaeroplus-platform.conventions")
}

architectury {
    platformSetupLoomIde()
    forge()
}

val common = project(":common")

loom {
    accessWidenerPath = common.loom.accessWidenerPath
    forge {
        mixinConfigs.set(listOf("xaeroplus.mixins.json", "xaeroplus-forge.mixins.json"))
        convertAccessWideners = true
        extraAccessWideners.add(loom.accessWidenerPath.get().asFile.name)
        useCustomMixin = false
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
val destArchiveVersion = "${project.version}+${loom.platform.get().id()}-${minecraft_version}"
val destArchiveClassifier = "WM${worldmap_version_forge}-MM${minimap_version_forge}"

sourceSets.main.get().java.srcDir(common.layout.buildDirectory.get().asFile.path + "/remappedSources/forge/java")
sourceSets.main.get().resources.srcDir(common.layout.buildDirectory.get().asFile.path + "/remappedSources/forge/resources")

dependencies {
    forge(libs.forge)
    compileOnly(annotationProcessor(libs.mixinextras.common.get())!!)
    implementation(include(libs.mixinextras.forge.get())!!)
    modImplementation(libs.worldmap.forge)
    modImplementation(libs.minimap.forge)
    modCompileOnly(files("libs/baritone-unoptimized-forge-1.10.2.jar"))
    modCompileOnly(libs.waystones.forge)
    modCompileOnly(libs.balm.forge)
    modCompileOnly(libs.worldtools)
    modCompileOnly(libs.fabric.waystones)
    modCompileOnly(libs.embeddium)
    shadow(libs.sqlite)
    implementation(include(libs.oldbiomes.get())!!)
    forgeRuntimeLibrary(implementation(include(libs.caffeine.get())!!)!!)
    forgeRuntimeLibrary(implementation(include(libs.lambdaEvents.get())!!)!!)
    compileOnly(project(":common"))
}

configurations.all {
    resolutionStrategy.force("net.sf.jopt-simple:jopt-simple:5.0.4")
}

tasks {
    processResources {
        dependsOn(common.tasks.getByName("remapForge"))
        filesMatching("META-INF/mods.toml") {
            expand(mapOf(
                "version" to project.version,
                "worldmap_version" to worldmap_version_forge,
                "minimap_version" to minimap_version_forge
            ))
        }
    }

    val transformForge = register("transformForge", TransformingTask::class.java) {
        group = "build"
        input.set(shadowJar.get().archiveFile)
        platform = loom.platform.get().name
        transformers.add(AddRefmapName())
        transformers.add(TransformForgeAnnotations())
        transformers.add(TransformForgeEnvironment())
        transformers.add(FixForgeMixin())
        loom.setGenerateSrgTiny(true)
        archiveVersion = destArchiveVersion
        archiveClassifier = destArchiveClassifier
    }

    shadowJar {
        configurations = listOf(project.configurations.shadow.get())
    }

    remapJar {
        dependsOn(shadowJar, transformForge)
        inputFile.set(shadowJar.get().archiveFile.get())
        archiveVersion = destArchiveVersion
        archiveClassifier = destArchiveClassifier
    }

    compileJava {
        dependsOn(common.tasks.getByName("remapForge"))
    }
}
