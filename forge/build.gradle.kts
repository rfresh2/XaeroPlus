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
    injectInjectables = false
}

val common = project(":common")
val shadowCommon = configurations.create("shadowCommon")

loom {
    accessWidenerPath = common.loom.accessWidenerPath
    forge {
        mixinConfigs.set(listOf("xaeroplus.mixins.json", "xaeroplus-forge.mixins.json"))
        convertAccessWideners = true
        extraAccessWideners.add(loom.accessWidenerPath.get().asFile.name)
        useCustomMixin = false
    }
}

val worldmap_version_forge = providers.gradleProperty("worldmap_version_forge").get()
val minimap_version_forge = providers.gradleProperty("minimap_version_forge").get()
val minecraft_version = providers.gradleProperty("minecraft_version").get()
val destArchiveVersion = "${project.version}+${loom.platform.get().id()}-${minecraft_version}"
val destArchiveClassifier = "WM${worldmap_version_forge}-MM${minimap_version_forge}"

sourceSets.main.get().java.srcDir(common.layout.buildDirectory.get().asFile.path + "/remappedSources/forge/java")
sourceSets.main.get().resources.srcDir(common.layout.buildDirectory.get().asFile.path + "/remappedSources/forge/resources")

dependencies {
    forge(libs.forge)
    compileOnly(annotationProcessor(libs.mixinextras.common.get())!!)
    implementation(include(libs.mixinextras.forge.get())!!)
    modImplementation(libs.worldmap.forge) { isTransitive = false }
    modImplementation(libs.minimap.forge) { isTransitive = false }
    modImplementation(libs.xaerolib.forge)
    modImplementation(libs.baritone.forge)
    modCompileOnly(libs.waystones.forge)
    modCompileOnly(libs.balm.forge)
    modCompileOnly(libs.worldtools)
    modCompileOnly(libs.wraith.waystones)
    shadow(libs.sqlite)
    forgeRuntimeLibrary(implementation(shadow(libs.oldbiomes.get())!!)!!)
    forgeRuntimeLibrary(implementation(shadow(libs.caffeine.get())!!)!!)
    forgeRuntimeLibrary(implementation(shadow(libs.lambdaEvents.get())!!)!!)
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
        destinationDirectory = project.layout.buildDirectory.dir("shadowJar")
        val shadePkg = "xaeroplus.shadow"
        relocate("kaptainwutax", "$shadePkg.kaptainwutax")
        relocate("net.lenni0451.lambdaevents", "$shadePkg.lambdaevents")
        relocate("com.github.benmanes.caffeine", "$shadePkg.caffeine")
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
