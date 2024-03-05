architectury {
    common(rootProject.properties["enabled_platforms"].toString().split(","))
}

loom {
    accessWidenerPath = file("src/main/resources/xaeroplus.accesswidener")
}

val worldmap_version: String by rootProject
val minimap_version: String by rootProject
val loader_version: String by rootProject

dependencies {
    modImplementation("net.fabricmc:fabric-loader:${loader_version}")
    modCompileOnly("maven.modrinth:xaeros-world-map:${worldmap_version}_Fabric_1.20.2")
    modCompileOnly("maven.modrinth:xaeros-minimap:${minimap_version}_Fabric_1.20.2")
    implementation(libs.caffeine)
    implementation(libs.lambdaEvents)
    modCompileOnly(files("../fabric/libs/baritone-unoptimized-fabric-1.10.2.jar"))
    modCompileOnly(libs.waystones.fabric)
    modCompileOnly(libs.balm.fabric)
    modCompileOnly(libs.fabric.waystones)
    modCompileOnly(libs.worldtools)
}

tasks {
    val remapForgeTask = register("remapForge") {
        group = "build"
        description = "Remap the source files, replacing fabric-specific strings with forge-specific strings."
        doLast {
            val remapDir = file("build/remappedSources/forge")
            // clear directory if it exists
            if (remapDir.exists()) {
                remapDir.delete()
            }
            remapDir.mkdirs()
            // create sourceset directory structure
            val javaDir = file(remapDir.path + "/java")
            javaDir.mkdirs()
            val resourcesDir = file(remapDir.path + "/resources")
            resourcesDir.mkdirs()

            // copy java sources to java dir
            copy {
                from(sourceSets.main.get().java.srcDirs)
                into(javaDir)
            }

            // copy resources to resources dir
            copy {
                from(sourceSets.main.get().resources.srcDirs)
                into(resourcesDir)
            }

            val remapFile = file("remap/remap.txt")
            // read remap file to a map
            // format is: 'fabricClassName::forgeClassName'
            val remap = hashMapOf<String, String>()
            remapFile.forEachLine { line ->
                if (line.startsWith("#") || !line.contains("::")) return@forEachLine
                val parts = line.split("::")
                remap[parts[0]] = parts[1]
                println("Loaded Forge Remap: '${parts[0]}' to '${parts[1]}'")
            }

            // exec remap on every source file
            remapDir.walk().forEach { file ->
                if (file.isFile && (file.extension == "java" || file.extension == "json")) {
                    var text = file.readText()
                    remap.forEach { (fabric, forge) ->
                        text = text.replace(fabric, forge)
                    }
                    file.writeText(text)
                }
            }
        }
    }
    compileJava.get().dependsOn(remapForgeTask)
}

