pluginManagement {
	repositories {
		maven("https://maven.fabricmc.net/") { name = "Fabric" }
		maven("https://maven.architectury.dev/")
		maven("https://files.minecraftforge.net/maven/")
		mavenCentral()
		gradlePluginPortal()
	}
}

val minecraft_version: String by ext.properties
val worldmap_version_fabric: String by ext.properties
val minimap_version_fabric: String by ext.properties
val worldmap_version_forge: String by ext.properties
val minimap_version_forge: String by ext.properties

dependencyResolutionManagement {
	versionCatalogs {
		create("libs") {
			library("fabric-loader", "net.fabricmc:fabric-loader:0.15.11")
			library("forge", "net.minecraftforge:forge:${minecraft_version}-45.2.8")
			library("fabric-api", "net.fabricmc.fabric-api:fabric-api:0.87.2+${minecraft_version}")
            library("worldmap-fabric", "maven.modrinth:xaeros-world-map:${worldmap_version_fabric}_Fabric_${minecraft_version}")
            library("worldmap-forge", "maven.modrinth:xaeros-world-map:${worldmap_version_forge}_Forge_${minecraft_version}")
            library("minimap-fabric", "curse.maven:xaeros-minimap-263420:6184427")
            library("minimap-forge", "curse.maven:xaeros-minimap-263420:6184426")
			library("mixinextras-common", "io.github.llamalad7:mixinextras-common:0.4.1")
			library("mixinextras-forge", "io.github.llamalad7:mixinextras-forge:0.4.1")
			library("caffeine", "com.github.ben-manes.caffeine:caffeine:3.2.0")
			library("lambdaEvents", "net.lenni0451:LambdaEvents:2.4.2")
			library("waystones-fabric", "maven.modrinth:waystones:13.1.0+fabric-1.19.4")
			library("waystones-forge", "maven.modrinth:waystones:13.1.0+forge-1.19.4")
			library("balm-fabric", "maven.modrinth:balm:6.0.2+fabric-1.19.4")
			library("balm-forge", "maven.modrinth:balm:6.0.2+forge-1.19.4")
			library("fabric-waystones", "maven.modrinth:fwaystones:3.1.1+mc1.19.4")
			library("worldtools", "maven.modrinth:worldtools:1.2.0+1.20.1")
            library("sqlite", "org.rfresh.xerial:sqlite-jdbc:3.47.1.0") // relocated xerial sqlite to avoid conflicts with other mods
			library("sodium", "maven.modrinth:sodium:mc1.19.4-0.4.10")
            library("modmenu", "maven.modrinth:modmenu:6.3.1")
            library("oldbiomes", "com.github.rfresh2:OldBiomes:1.0.0")
			library("baritone-fabric", "com.github.rfresh2:baritone-fabric:${minecraft_version}")
			library("baritone-forge", "com.github.rfresh2:baritone-forge:${minecraft_version}")
        }
	}
}



include("common")
include("fabric")
include("forge")

rootProject.name = "XaeroPlus"
