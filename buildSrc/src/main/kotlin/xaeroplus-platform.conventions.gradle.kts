plugins {
    id("dev.architectury.loom-no-remap")
    id("com.gradleup.shadow")
}

loom {
    silentMojangMappingsLicense()
    runs {
        getByName("client") {
            jvmArguments.addAll("-Dsodium.checks.issue2561=false", "-Xmx4G")
            programArguments.addAll("--username", "rfresh2")
        }
    }
//    mixin {
//        useLegacyMixinAp = true
//    }
}

val minecraft_version = providers.gradleProperty("minecraft_version").get()
val mc = "com.mojang:minecraft:${minecraft_version}"

// Unobfuscated versions of Loom no longer need to remap mod dependencies, and so no longer provide the `mod*`
// configurations. We'll re-create them so they can be used across all versions.
configurations.api.get().extendsFrom(configurations.create("modApi"))
configurations.implementation.get().extendsFrom(configurations.create("modImplementation"))
configurations.compileOnly.get().extendsFrom(configurations.create("modCompileOnly"))
configurations.runtimeOnly.get().extendsFrom(configurations.create("modRuntimeOnly"))
configurations.localRuntime.get().extendsFrom(configurations.create("modLocalRuntime"))


dependencies {
    minecraft(mc)
}

tasks {
    shadowJar {
        archiveClassifier.set("shadow")
        dependencies {
            exclude(dependency("com.google.errorprone:.*:.*"))
            exclude(dependency("org.jspecify:.*:.*"))
            exclude(dependency("org.ow2.asm:.*:.*"))
            exclude(dependency("org.slf4j:.*:.*"))
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
    }
}
