pluginManagement {
	repositories {
		maven("https://maven.fabricmc.net/") { name = "Fabric" }
		maven("https://maven.architectury.dev/")
		maven("https://files.minecraftforge.net/maven/")
		maven("https://maven.neoforged.net/releases")
		mavenCentral()
		gradlePluginPortal()
	}
}

val minecraft_version: String by ext.properties
val worldmap_version_fabric: String by ext.properties
val minimap_version_fabric: String by ext.properties
val worldmap_version_forge: String by ext.properties
val minimap_version_forge: String by ext.properties
val worldmap_version_neo: String by ext.properties
val minimap_version_neo: String by ext.properties

dependencyResolutionManagement {
	versionCatalogs {
		create("libs") {
			library("fabric-loader", "net.fabricmc:fabric-loader:0.16.14")
			library("forge", "net.minecraftforge:forge:1.21.7-57.0.2")
			library("fabric-api", "net.fabricmc.fabric-api:fabric-api:0.128.2+${minecraft_version}")
			library("neoforge", "net.neoforged:neoforge:21.7.4-beta")
			library("worldmap-fabric", "maven.modrinth:xaeros-world-map:${worldmap_version_fabric}_Fabric_1.21.6")
			library("worldmap-forge", "maven.modrinth:xaeros-world-map:${worldmap_version_forge}_Forge_${minecraft_version}")
			library("worldmap-neo", "maven.modrinth:xaeros-world-map:${worldmap_version_neo}_NeoForge_${minecraft_version}")
			library("minimap-fabric", "maven.modrinth:xaeros-minimap:${minimap_version_fabric}_Fabric_${minecraft_version}")
			library("minimap-forge", "maven.modrinth:xaeros-minimap:${minimap_version_forge}_Forge_${minecraft_version}")
			library("minimap-neo", "maven.modrinth:xaeros-minimap:${minimap_version_neo}_NeoForge_${minecraft_version}")
            library("mixinextras-common", "io.github.llamalad7:mixinextras-common:0.4.1")
            library("mixinextras-forge", "io.github.llamalad7:mixinextras-forge:0.4.1")
			library("caffeine", "com.github.ben-manes.caffeine:caffeine:3.2.0")
			library("lambdaEvents", "net.lenni0451:LambdaEvents:2.4.2")
			library("waystones-fabric", "maven.modrinth:waystones:21.7.1+fabric-1.21.7")
			library("waystones-forge", "maven.modrinth:waystones:21.7.1+forge-1.21.7")
			library("waystones-neoforge", "maven.modrinth:waystones:21.7.1+neoforge-1.21.7")
			library("balm-fabric", "maven.modrinth:balm:21.7.2+fabric-1.21.7")
			library("balm-forge", "maven.modrinth:balm:21.7.2+forge-1.21.7")
			library("balm-neoforge", "maven.modrinth:balm:21.7.2+neoforge-1.21.7")
			library("fabric-waystones", "maven.modrinth:fwaystones:3.3.5+mc1.21.4")
			library("worldtools", "maven.modrinth:worldtools:1.2.8+1.21.4")
            library("sqlite", "org.rfresh.xerial:sqlite-jdbc:3.50.1.0") // relocated xerial sqlite to avoid conflicts with other mods
			library("immediatelyfast", "maven.modrinth:immediatelyfast:1.11.0+1.21.7-fabric")
			library("immediatelyfast-neo", "maven.modrinth:immediatelyfast:1.11.0+1.21.7-neoforge")
			library("modmenu", "maven.modrinth:modmenu:15.0.0-beta.3")
			library("sodium-fabric", "maven.modrinth:sodium:mc1.21.6-0.6.13-fabric")
			library("sodium-neoforge", "maven.modrinth:sodium:mc1.21.6-0.6.13-neoforge")
//			library("fpsdisplay", "maven.modrinth:fpsdisplay:3.1.0+1.20.x")
			library("cloth-config-fabric", "me.shedaniel.cloth:cloth-config-fabric:17.0.144")
            library("oldbiomes", "com.github.rfresh2:OldBiomes:1.0.0")
            library("baritone-fabric", "com.github.rfresh2:baritone-fabric:${minecraft_version}-SNAPSHOT")
            library("baritone-forge", "com.github.rfresh2:baritone-forge:${minecraft_version}-SNAPSHOT")
            library("baritone-neoforge", "com.github.rfresh2:baritone-neoforge:${minecraft_version}-SNAPSHOT")
        }
	}
}



include("common")
include("fabric")
include("forge")
include("neo")

rootProject.name = "XaeroPlus"
