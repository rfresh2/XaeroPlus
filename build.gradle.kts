import org.jetbrains.gradle.ext.Gradle
import org.jetbrains.gradle.ext.RunConfigurationContainer
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

plugins {
    java
    id("java-library")
    id("org.jetbrains.gradle.plugin.idea-ext") version "1.3"
    id("com.gtnewhorizons.retrofuturagradle") version "2.0.2"
    id("com.gradleup.shadow") version "9.4.1"
}

// Project properties
group = "xaeroplus"
version = providers.environmentVariable("RELEASE_VERSION").orElse("1.12.2").get()


// Set the toolchain version to decouple the Java we run Gradle with from the Java used to compile and run the mod
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
}

// Most RFG configuration lives here, see the JavaDoc for com.gtnewhorizons.retrofuturagradle.MinecraftExtension
minecraft {
    mcVersion.set("1.12.2")

    // Username for client run configurations
    username.set("Developer")

    // Generate a field named VERSION with the mod version in the injected Tags class
    injectedTags.put("WORLDMAP_VERSION", project.properties["worldmap_version"] as String)
    injectedTags.put("MINIMAP_VERSION", project.properties["minimap_version"] as String)
    injectedTags.put("XAEROLIB_VERSION", project.properties["xaerolib_version"] as String)

    // If you need the old replaceIn mechanism, prefer the injectTags task because it doesn't inject a javac plugin.
    // tagReplacementFiles.add("RfgExampleMod.java")

    // Enable assertions in the mod's package when running the client or server
    extraRunJvmArguments.add("-ea:${project.group}")

    // If needed, add extra tweaker classes like for mixins.
     extraTweakClasses.add("org.spongepowered.asm.launch.MixinTweaker")

    // Exclude some Maven dependency groups from being automatically included in the reobfuscated runs
}

// Create a new dependency type for runtime-only dependencies that don't get included in the maven publication
//val runtimeOnlyNonPublishable: Configuration by configurations.creating {
//    description = "Runtime only dependencies that are not published alongside the jar"
//    isCanBeConsumed = false
//    isCanBeResolved = false
//}
//listOf(configurations.runtimeClasspath, configurations.testRuntimeClasspath).forEach {
//    it.configure {
//        extendsFrom(
//            runtimeOnlyNonPublishable
//        )
//    }
//}

// Add an access tranformer
// tasks.deobfuscateMergedJarToSrg.configure {accessTransformerFiles.from("src/main/resources/META-INF/mymod_at.cfg")}

// Dependencies
repositories {
    maven {
        name = "OvermindDL1 Maven"
        url = uri("https://gregtech.overminddl1.com/")
    }
    maven {
        name = "GTNH Maven"
        url = uri("https://nexus.gtnewhorizons.com/repository/public/")
    }
    maven("https://maven.2b2t.vc/releases")
    maven("https://maven.2b2t.vc/remote")
    maven("https://api.modrinth.com/maven")
    maven("https://cursemaven.com")
    maven("https://jitpack.io")
    maven("https://impactdevelopment.github.io/maven/")
    maven("https://maven.cleanroommc.com")
}

sourceSets {
    create("wdl") {
        compileClasspath += sourceSets.main.get().compileClasspath
    }
    getByName("main") {
        compileClasspath += getByName("wdl").output
        output.setResourcesDir(output.classesDirs.singleFile)
    }
}

val jarLibs by configurations.creating
configurations.implementation.get().extendsFrom(jarLibs)

dependencies {
    val mixin: String = modUtils.enableMixins("zone.rong:mixinbooter:10.7", "mixins.xaeroplus.refmap.json") as String
    api(mixin) {
        isTransitive = false
    }
    annotationProcessor("org.ow2.asm:asm-debug-all:5.2")
    annotationProcessor("com.google.guava:guava:32.1.2-jre")
    annotationProcessor("com.google.code.gson:gson:2.8.9")
    annotationProcessor(mixin) {
        isTransitive = false
    }

    jarLibs("com.github.ben-manes.caffeine:caffeine:2.9.3")
    jarLibs("org.rfresh.xerial:sqlite-jdbc:3.53.0.1")
    implementation(modUtils.deobfuscate("maven.modrinth:xaeros-world-map:forge-1.12.2-${project.properties["worldmap_version"]}"))
    implementation(modUtils.deobfuscate("maven.modrinth:xaeros-minimap:forge-1.12.2-${project.properties["minimap_version"]}"))
    implementation(modUtils.deobfuscate("maven.modrinth:xaerolib:forge-1.12.2-${project.properties["xaerolib_version"]}"))
    implementation(modUtils.deobfuscate("cabaletta:baritone-deobf-unoptimized-mcp-dev:1.2"))
    compileOnly(modUtils.deobfuscate("curse.maven:waystones-245755:2859589"))
}

tasks {
    jar {
        enabled = false
    }

    reobfJar {
        inputJar.set(shadowJar.get().archiveFile)
    }

    // Generates a class named rfg.examplemod.Tags with the mod version in it, you can find it at
    injectTags.configure {
        outputClassName.set("${project.group}.BuildConstants")
    }

    // Put the version from gradle into mcmod.info
    processResources.configure {
        val projVersion = project.version.toString() // Needed for configuration cache to work
        inputs.property("version", projVersion)

        filesMatching("mcmod.info") {
            expand(mapOf("version" to projVersion))
        }
    }

    shadowJar {
        configurations.set(listOf(jarLibs))
        archiveClassifier = ""
        manifest {
            attributes.putAll(mapOf(
                "FMLCorePluginContainsFMLMod" to "true",
                "FMLCorePlugin" to "xaeroplus.mixin.MixinLoaderForge",
                "ForceLoadAsMod" to "true",
                "Specification-Title" to "XaeroPlus",
                "Specification-Vendor" to "rfresh2",
                "Specification-Version" to "1",
                "Implementation-Title" to project.name,
                "Implementation-Version" to "${version}",
                "Implementation-Vendor" to "rfresh2",
                "Implementation-Timestamp" to DateTimeFormatter.ISO_DATE_TIME.format(ZonedDateTime.now())
            ))
        }
        mergeServiceFiles()
        dependencies {
            exclude(dependency(":checker-qual:.*"))
            exclude(dependency(":error_prone_annotations:.*"))
        }
        exclude("org/rfresh/sqlite/native/FreeBSD/**")
        exclude("org/rfresh/sqlite/native/Linux-Android/x86/**")
        exclude("org/rfresh/sqlite/native/Linux-Android/x86_64/**")
        exclude("org/rfresh/sqlite/native/Linux-Musl/**")
        exclude("org/rfresh/sqlite/native/Linux/arm/**")
        exclude("org/rfresh/sqlite/native/Linux/armv6/**")
        exclude("org/rfresh/sqlite/native/Linux/x86/**")
        exclude("org/rfresh/sqlite/native/Linux/armv7/**")
        exclude("org/rfresh/sqlite/native/Linux/ppc64/**")
        exclude("org/rfresh/sqlite/native/Linux/riscv64/**")
        exclude("org/rfresh/sqlite/native/Windows/armv7/**")
        exclude("META-INF/versions/9/**")
        exclude("org/rfresh/sqlite/nativeimage/**")
    }

    register("printWorldMapVersion") {
        doLast {
            println(project.properties["worldmap_version"])
        }
        outputs.upToDateWhen { false }
    }
    register("printMinimapVersion") {
        doLast {
            println(project.properties["minimap_version"])
        }
        outputs.upToDateWhen { false }
    }
    register("printXaeroLibVersion") {
        doLast {
            println(project.properties["xaerolib_version"])
        }
        outputs.upToDateWhen { false }
    }
}

idea {
    module {
        isDownloadJavadoc = true
        isDownloadSources = true
        inheritOutputDirs = true // Fix resources in IJ-Native runs
    }
    project {
        this.withGroovyBuilder {
            "settings" {
                "runConfigurations" {
                    val self = this.delegate as RunConfigurationContainer
                    self.add(Gradle("1. Run Client").apply {
                        setProperty("taskNames", listOf("runClient"))
                    })
                    self.add(Gradle("2. Run Server").apply {
                        setProperty("taskNames", listOf("runServer"))
                    })
                    self.add(Gradle("3. Run Obfuscated Client").apply {
                        setProperty("taskNames", listOf("runObfClient"))
                    })
                    self.add(Gradle("4. Run Obfuscated Server").apply {
                        setProperty("taskNames", listOf("runObfServer"))
                    })
                    /*
                    These require extra configuration in IntelliJ, so are not enabled by default
                    self.add(Application("Run Client (IJ Native, Deprecated)", project).apply {
                      mainClass = "GradleStart"
                      moduleName = project.name + ".ideVirtualMain"
                      afterEvaluate {
                        val runClient = tasks.runClient.get()
                        workingDirectory = runClient.workingDir.absolutePath
                        programParameters = runClient.calculateArgs(project).map { '"' + it + '"' }.joinToString(" ")
                        jvmArgs = runClient.calculateJvmArgs(project).map { '"' + it + '"' }.joinToString(" ") +
                          ' ' + runClient.systemProperties.map { "\"-D" + it.key + '=' + it.value.toString() + '"' }
                          .joinToString(" ")
                      }
                    })
                    self.add(Application("Run Server (IJ Native, Deprecated)", project).apply {
                      mainClass = "GradleStartServer"
                      moduleName = project.name + ".ideVirtualMain"
                      afterEvaluate {
                        val runServer = tasks.runServer.get()
                        workingDirectory = runServer.workingDir.absolutePath
                        programParameters = runServer.calculateArgs(project).map { '"' + it + '"' }.joinToString(" ")
                        jvmArgs = runServer.calculateJvmArgs(project).map { '"' + it + '"' }.joinToString(" ") +
                          ' ' + runServer.systemProperties.map { "\"-D" + it.key + '=' + it.value.toString() + '"' }
                          .joinToString(" ")
                      }
                    })
                    */
                }
                "compiler" {
                    val self = this.delegate as org.jetbrains.gradle.ext.IdeaCompilerConfiguration
                    afterEvaluate {
                        self.javac.moduleJavacAdditionalOptions = mapOf(
                            (project.name + ".main") to
                                    tasks.compileJava.get().options.compilerArgs.map { '"' + it + '"' }.joinToString(" ")
                        )
                    }
                }
            }
        }
    }
}

tasks.processIdeaSettings.configure {
    dependsOn(tasks.injectTags)
}
