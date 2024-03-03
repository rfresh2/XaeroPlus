import groovy.lang.Closure
import org.codehaus.groovy.runtime.ResourceGroovyMethods

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
    modCompileOnly("maven.modrinth:xaeros-world-map:${worldmap_version}_Fabric_1.20")
    modCompileOnly("maven.modrinth:xaeros-minimap:${minimap_version}_Fabric_1.20")
    compileOnly("com.github.ben-manes.caffeine:caffeine:3.1.8")
    compileOnly("net.lenni0451:LambdaEvents:2.4.1")
    modCompileOnly(files("libs/baritone-api-fabric-1.20.1-elytra-beta-v1.jar"))
    modCompileOnly("maven.modrinth:waystones:14.0.2+fabric-1.20")
    modCompileOnly("maven.modrinth:balm:7.1.4+fabric-1.20.1")
    modCompileOnly("maven.modrinth:fwaystones:3.1.3+mc1.20")
    modCompileOnly("maven.modrinth:worldtools:1.2.0+1.20.1")
}

tasks {
    val remapForgeTask = register("remapForge") {
        group = "build"
        description = "Remap the source files, replacing xaero's fabric class names with xaero's forge class names."
        doLast {
            val remapDir = file("build/remappedSources/forge")
            // clear directory if it exists
            if (remapDir.exists()) {
                remapDir.delete()
            }
            remapDir.mkdirs()
            // create sourceset directory structure
            // i.e. java and resources directories
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
            // read remap file to a string2string map
            // format is: 'fabricClassName:forgeClassName'
            val remap = hashMapOf<String, String>()
            remapFile.forEachLine { line ->
                val parts = line.split(":")
                remap[parts[0]] = parts[1]
                println("Loaded Forge Remap: '${parts[0]}' to '${parts[1]}'")
            }

            // for each source file, replace fabric class names with forge class names
            // this is just a simple string replacement, so it's not perfect, but it should work for this case
            ResourceGroovyMethods.eachFileRecurse(
                remapDir,
                object : Closure<Any?>(
                    this,
                    this
                ) {
                    fun doCall(file: File? = null) {
                        val fileName = file!!.name
                        if (fileName.endsWith(".java")) {
                            var text = file.readText()
                            remap.forEach { (fabric, forge) ->
                                text = text.replace(fabric, forge)
                            }
                            file.writeText(text)
                        }
                    }
                })
        }
    }
    compileJava.get().dependsOn(remapForgeTask)
}

