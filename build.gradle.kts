plugins {
    id("xaeroplus-all.conventions")
    idea
}

val minecraft_version = providers.gradleProperty("minecraft_version").get()

architectury {
    minecraft = minecraft_version
}

tasks {
    register("printWorldMapVersionFabric") {
        doLast {
            println(providers.gradleProperty("worldmap_version_fabric").get())
        }
        outputs.upToDateWhen { false }
    }
    register("printMinimapVersionFabric") {
        doLast {
            println(providers.gradleProperty("minimap_version_fabric").get())
        }
        outputs.upToDateWhen { false }
    }
    register("printWorldMapVersionForge") {
        doLast {
            println(providers.gradleProperty("worldmap_version_forge").get())
        }
        outputs.upToDateWhen { false }
    }
    register("printMinimapVersionForge") {
        doLast {
            println(providers.gradleProperty("minimap_version_forge").get())
        }
        outputs.upToDateWhen { false }
    }
    register("printXaeroPlusVersion") {
        doLast {
            println(version)
        }
        outputs.upToDateWhen { false }
    }
}

idea {
    module {
        excludeDirs.add(project.layout.buildDirectory.asFile.get())
        excludeDirs.addAll(subprojects.map { p -> p.layout.buildDirectory.asFile.get() }.toList())
        excludeDirs.addAll(subprojects.map { p -> p.layout.projectDirectory }.map { d -> d.asFile.resolve("run") }.toList())
        val forgeRemapDir = subprojects.first { p -> p.name == "common" }.layout.buildDirectory.dir("remappedSources/forge").get().asFile
        excludeDirs.addAll(listOf(forgeRemapDir.resolve("java"), forgeRemapDir.resolve("resources")))
    }
}
