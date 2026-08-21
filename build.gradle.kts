import org.jetbrains.gradle.ext.Gradle
import org.jetbrains.gradle.ext.RunConfigurationContainer
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

plugins {
    java
    id("java-library")
    id("org.jetbrains.gradle.plugin.idea-ext") version "1.3"
    id("com.gtnewhorizons.retrofuturagradle") version "2.0.2"
    id("com.gradleup.shadow") version "9.4.2"
}

group = "xaeroplus"
version = providers.environmentVariable("RELEASE_VERSION").orElse("1.12.2").get()
val mixin_booter_version = property("mixin_booter_version") as String
val worldmap_version = property("worldmap_version") as String
val minimap_version = property("minimap_version") as String
val xaerolib_version = property("xaerolib_version") as String

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
}

minecraft {
    mcVersion = "1.12.2"
    username = "rfresh2"

    injectedTags.put("MOD_VERSION", project.version.toString())
    injectedTags.put("MIXIN_BOOTER_VERSION", mixin_booter_version)
    injectedTags.put("WORLDMAP_VERSION", worldmap_version)
    injectedTags.put("MINIMAP_VERSION", minimap_version)
    injectedTags.put("XAEROLIB_VERSION", xaerolib_version)

    extraRunJvmArguments.add("-ea:${project.group}")

    extraTweakClasses.add("org.spongepowered.asm.launch.MixinTweaker")
}

// Add an access tranformer
// tasks.deobfuscateMergedJarToSrg.configure {accessTransformerFiles.from("src/main/resources/META-INF/mymod_at.cfg")}

repositories {
    maven {
        name = "GTNH Maven"
        url = uri("https://nexus.gtnewhorizons.com/repository/public/")
    }
    maven("https://maven.2b2t.vc/releases")
    maven("https://maven.2b2t.vc/remote")
    maven("https://api.modrinth.com/maven") {
        content {
            includeGroup("maven.modrinth")
        }
    }
    maven("https://cursemaven.com") {
        content {
            includeGroup("curse.maven")
        }
    }
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

val jarLibs = configurations.create("jarLibs")
configurations.implementation.get().extendsFrom(jarLibs)

dependencies {
    val mixin: String = modUtils.enableMixins("zone.rong:mixinbooter:$mixin_booter_version", "mixins.xaeroplus.refmap.json") as String
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
    jarLibs("org.rfresh.xerial:sqlite-jdbc:3.53.2.0")
    implementation(modUtils.deobfuscate("maven.modrinth:xaeros-world-map:forge-1.12.2-$worldmap_version"))
    implementation(modUtils.deobfuscate("maven.modrinth:xaeros-minimap:forge-1.12.2-$minimap_version"))
    implementation(modUtils.deobfuscate("maven.modrinth:xaerolib:forge-1.12.2-$xaerolib_version"))
    compileOnly(modUtils.deobfuscate("cabaletta:baritone-deobf-unoptimized-mcp-dev:1.2"))
}

tasks {
    jar {
        destinationDirectory = project.layout.buildDirectory.dir("devlib")
    }

    reobfJar {
        inputJar.set(shadowJar.get().archiveFile)
        destinationDirectory = project.layout.buildDirectory.dir("libs")
    }

    injectTags.configure {
        outputClassName.set("${project.group}.BuildConstants")
    }

    processResources.configure {
        val projVersion = project.version.toString()
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
            println(worldmap_version)
        }
        outputs.upToDateWhen { false }
    }
    register("printMinimapVersion") {
        doLast {
            println(minimap_version)
        }
        outputs.upToDateWhen { false }
    }
    register("printXaeroLibVersion") {
        doLast {
            println(xaerolib_version)
        }
        outputs.upToDateWhen { false }
    }
    register("printMixinBooterVersion") {
        doLast {
            println(mixin_booter_version)
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
                }
                "compiler" {
                    val self = this.delegate as org.jetbrains.gradle.ext.IdeaCompilerConfiguration
                    afterEvaluate {
                        self.javac.moduleJavacAdditionalOptions = mapOf(
                            (project.name + ".main") to
                                    tasks.compileJava.get().options.compilerArgs.joinToString(" ") { '"' + it + '"' }
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
