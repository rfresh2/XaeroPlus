plugins {
    id("dev.architectury.loom")
    id("com.gradleup.shadow")
}

loom {
    silentMojangMappingsLicense()
    runs {
        getByName("client") {
            programArgs("--username", "rfresh2")
        }
    }
}

val minecraft_version: String by gradle.extra
val parchment_version: String by gradle.extra
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
        exclude("com/google/**")
        exclude("org/objectweb/**")
        exclude("org/checkerframework/**")
        exclude("org/rfresh/sqlite/native/FreeBSD/**")
        exclude("org/rfresh/sqlite/native/Linux-Android/**")
        exclude("org/rfresh/sqlite/native/Linux-Musl/**")
        exclude("org/rfresh/sqlite/native/Linux/arm/**")
        exclude("org/rfresh/sqlite/native/Linux/aarch64/**")
        exclude("org/rfresh/sqlite/native/Linux/armv6/**")
        exclude("org/rfresh/sqlite/native/Linux/x86/**")
        exclude("org/rfresh/sqlite/native/Linux/armv7/**")
        exclude("org/rfresh/sqlite/native/Linux/ppc64/**")
        exclude("org/rfresh/sqlite/native/Linux/riscv64/**")
        exclude("org/rfresh/sqlite/native/Windows/armv7/**")
        exclude("org/rfresh/sqlite/native/Windows/aarch64/**")
        exclude("org/rfresh/sqlite/native/Windows/armv7/**")
        exclude("org/slf4j/**")
    }
}
