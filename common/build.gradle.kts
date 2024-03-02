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
