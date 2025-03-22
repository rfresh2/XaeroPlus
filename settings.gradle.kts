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
			library("fabric-loader", "net.fabricmc:fabric-loader:0.16.10")
			library("forge", "net.minecraftforge:forge:${minecraft_version}-54.1.3")
			library("fabric-api", "net.fabricmc.fabric-api:fabric-api:0.119.2+${minecraft_version}")
			library("neoforge", "net.neoforged:neoforge:21.4.123")
            library("worldmap-fabric", "maven.modrinth:xaeros-world-map:${worldmap_version_fabric}_Fabric_${minecraft_version}")
            library("worldmap-forge", "maven.modrinth:xaeros-world-map:${worldmap_version_forge}_Forge_${minecraft_version}")
            library("worldmap-neo", "maven.modrinth:xaeros-world-map:${worldmap_version_neo}_NeoForge_${minecraft_version}")
            library("minimap-fabric", "maven.modrinth:xaeros-minimap:${minimap_version_fabric}_Fabric_${minecraft_version}")
            library("minimap-forge", "maven.modrinth:xaeros-minimap:${minimap_version_forge}_Forge_${minecraft_version}")
            library("minimap-neo", "maven.modrinth:xaeros-minimap:${minimap_version_neo}_NeoForge_${minecraft_version}")
            library("mixinextras-common", "io.github.llamalad7:mixinextras-common:0.4.1")
            library("mixinextras-forge", "io.github.llamalad7:mixinextras-forge:0.4.1")
			library("caffeine", "com.github.ben-manes.caffeine:caffeine:3.2.0")
			library("lambdaEvents", "net.lenni0451:LambdaEvents:2.4.2")
			library("waystones-fabric", "maven.modrinth:waystones:21.4.10+fabric-1.21.4")
			library("waystones-forge", "maven.modrinth:waystones:21.4.10+forge-1.21.4")
			library("waystones-neoforge", "maven.modrinth:waystones:21.4.10+neoforge-1.21.4")
			library("balm-fabric", "maven.modrinth:balm:21.4.19+fabric-1.21.4")
			library("balm-forge", "maven.modrinth:balm:21.4.19+forge-1.21.4")
			library("balm-neoforge", "maven.modrinth:balm:21.4.19+neoforge-1.21.4")
			library("fabric-waystones", "maven.modrinth:fwaystones:3.3.5+mc1.21.4")
			library("worldtools", "maven.modrinth:worldtools:1.2.8+1.21.4")
            library("sqlite", "org.rfresh.xerial:sqlite-jdbc:3.49.1.0") // relocated xerial sqlite to avoid conflicts with other mods
			library("immediatelyfast", "maven.modrinth:immediatelyfast:1.8.0+1.21.4-fabric")
			library("immediatelyfast-neo", "maven.modrinth:immediatelyfast:1.8.0+1.21.4-neoforge")
			library("modmenu", "maven.modrinth:modmenu:13.0.3")
			library("sodium-fabric", "maven.modrinth:sodium:mc1.21.4-0.6.10-fabric")
			library("sodium-neoforge", "maven.modrinth:sodium:mc1.21.4-0.6.10-neoforge")
//			library("fpsdisplay", "maven.modrinth:fpsdisplay:3.1.0+1.20.x")
			library("cloth-config-fabric", "me.shedaniel.cloth:cloth-config-fabric:17.0.144")
            library("oldbiomes", "com.github.rfresh2:OldBiomes:1.0.0")
            library("baritone-fabric", "com.github.rfresh2:baritone-fabric:${minecraft_version}")
            library("baritone-forge", "com.github.rfresh2:baritone-forge:${minecraft_version}")
            library("baritone-neoforge", "com.github.rfresh2:baritone-neoforge:${minecraft_version}")
        }
	}
}



include("common")
include("fabric")
include("forge")
include("neo")

rootProject.name = "XaeroPlus"
