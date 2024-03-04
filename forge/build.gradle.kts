import dev.architectury.plugin.TransformingTask
import dev.architectury.plugin.transformers.AddRefmapName
import dev.architectury.transformer.transformers.FixForgeMixin
import dev.architectury.transformer.transformers.TransformForgeAnnotations
import dev.architectury.transformer.transformers.TransformForgeEnvironment

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
        mixin {
            defaultRefmapName.set("xaeroplus-refmap.json")
        }
    }
    runs {
        getByName("client") {
            ideConfigGenerated(true)
            client()
        }
    }
}

val worldmap_version: String by rootProject
val minimap_version: String by rootProject
val minecraft_version: String by rootProject
val parchment_version: String by rootProject
val loader_version: String by rootProject
val forge_version: String by rootProject
val destArchiveVersion = "${project.version}+${loom.platform.get().id()}-${minecraft_version}"
val destArchiveClassifier = "WM${worldmap_version}-MM${minimap_version}"

sourceSets.main.get().java.srcDir(common.layout.buildDirectory.get().asFile.path + "/remappedSources/forge/java")
sourceSets.main.get().resources.srcDir(common.layout.buildDirectory.get().asFile.path + "/remappedSources/forge/resources")

dependencies {
    forge("net.minecraftforge:forge:${forge_version}")
    implementation(annotationProcessor("io.github.llamalad7:mixinextras-common:0.3.5")!!)
    implementation(include("io.github.llamalad7:mixinextras-forge:0.3.5")!!)
    modImplementation("maven.modrinth:xaeros-world-map:${worldmap_version}_Forge_1.20.4")
    modImplementation("maven.modrinth:xaeros-minimap:${minimap_version}_Forge_1.20.4")
    modImplementation(files("libs/baritone-unoptimized-forge-1.10.2.jar"))
    modCompileOnly(libs.waystones.forge)
    modCompileOnly(libs.balm.forge)
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
}
