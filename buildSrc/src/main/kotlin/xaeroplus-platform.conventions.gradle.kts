plugins {
    id("gg.essential.loom")
    id("com.gradleup.shadow")
}

loom {
    silentMojangMappingsLicense()
    runs {
        getByName("client") {
            vmArg("-Dsodium.checks.issue2561=false")
            programArgs("--username", "rfresh2")
        }
    }
//    mixin {
//        useLegacyMixinAp = true
//    }
}

val minecraft_version: String by project.properties
val parchment_version: String by project.properties
val mc = "com.mojang:minecraft:${minecraft_version}"
val parchment = "org.parchmentmc.data:parchment-${minecraft_version}:${parchment_version}"

dependencies {
    minecraft(mc)
    mappings(loom.layered {
        officialMojangMappings()
        parchment(parchment)
    })
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
