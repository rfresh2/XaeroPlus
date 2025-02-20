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
gradle.extra.apply {
	set("mod_version", "2.26.3")
	set("minecraft_version", "1.21.4")
    set("parchment_version", "2024.12.29")
    set("worldmap_version_fabric", "1.39.4")
	set("minimap_version_fabric", "25.1.0")
	set("worldmap_version_forge", "1.39.4")
	set("minimap_version_forge", "25.1.0")
	set("worldmap_version_neo", "1.39.4")
	set("minimap_version_neo", "25.1.0")
}

dependencyResolutionManagement {
	versionCatalogs {
		create("libs") {
			library("fabric-loader", "net.fabricmc:fabric-loader:0.16.10")
			library("forge", "net.minecraftforge:forge:${gradle.extra.get("minecraft_version")}-54.1.1")
			library("fabric-api", "net.fabricmc.fabric-api:fabric-api:0.115.1+1.21.4")
			library("neoforge", "net.neoforged:neoforge:21.4.91-beta")
			library("worldmap-fabric", "maven.modrinth:xaeros-world-map:${gradle.extra.get("worldmap_version_fabric")}_Fabric_${gradle.extra.get("minecraft_version")}")
			library("worldmap-forge", "maven.modrinth:xaeros-world-map:${gradle.extra.get("worldmap_version_forge")}_Forge_${gradle.extra.get("minecraft_version")}")
			library("worldmap-neo", "maven.modrinth:xaeros-world-map:${gradle.extra.get("worldmap_version_neo")}_NeoForge_${gradle.extra.get("minecraft_version")}")
			library("minimap-fabric", "maven.modrinth:xaeros-minimap:${gradle.extra.get("minimap_version_fabric")}_Fabric_${gradle.extra.get("minecraft_version")}")
			library("minimap-forge", "maven.modrinth:xaeros-minimap:${gradle.extra.get("minimap_version_forge")}_Forge_${gradle.extra.get("minecraft_version")}")
			library("minimap-neo", "maven.modrinth:xaeros-minimap:${gradle.extra.get("minimap_version_neo")}_NeoForge_${gradle.extra.get("minecraft_version")}")
            library("mixinextras-common", "io.github.llamalad7:mixinextras-common:0.4.1")
            library("mixinextras-forge", "io.github.llamalad7:mixinextras-forge:0.4.1")
			library("caffeine", "com.github.ben-manes.caffeine:caffeine:3.2.0")
			library("lambdaEvents", "net.lenni0451:LambdaEvents:2.4.2")
			library("waystones-fabric", "maven.modrinth:waystones:21.4.6+fabric-1.21.4")
			library("waystones-forge", "maven.modrinth:waystones:21.1.4+forge-1.21.1")
			library("waystones-neoforge", "maven.modrinth:waystones:21.4.6+neoforge-1.21.4")
			library("balm-fabric", "maven.modrinth:balm:21.4.13+fabric-1.21.4")
			library("balm-forge", "maven.modrinth:balm:21.0.20+forge-1.21.1")
			library("balm-neoforge", "maven.modrinth:balm:21.4.13+neoforge-1.21.4")
			library("fabric-waystones", "maven.modrinth:fwaystones:3.3.4+mc1.21.1")
			library("worldtools", "maven.modrinth:worldtools:1.2.8+1.21.4")
            library("sqlite", "org.rfresh.xerial:sqlite-jdbc:3.47.1.0") // relocated xerial sqlite to avoid conflicts with other mods
			library("immediatelyfast", "maven.modrinth:immediatelyfast:1.3.4+1.21.4-fabric")
			library("immediatelyfast-neo", "maven.modrinth:immediatelyfast:1.3.4+1.21.4-neoforge")
			library("modmenu", "maven.modrinth:modmenu:13.0.2")
			library("sodium-fabric", "maven.modrinth:sodium:mc1.21.4-0.6.7-fabric")
			library("sodium-neoforge", "maven.modrinth:sodium:mc1.21.4-0.6.7-neoforge")
//			library("fpsdisplay", "maven.modrinth:fpsdisplay:3.1.0+1.20.x")
			library("cloth-config-fabric", "me.shedaniel.cloth:cloth-config-fabric:17.0.144")
            library("oldbiomes", "com.github.rfresh2:OldBiomes:1.0.0")
			library("baritone-fabric", "com.github.rfresh2:baritone-fabric:${gradle.extra.get("minecraft_version")}")
			library("baritone-forge", "com.github.rfresh2:baritone-forge:${gradle.extra.get("minecraft_version")}")
			library("baritone-neoforge", "com.github.rfresh2:baritone-neoforge:${gradle.extra.get("minecraft_version")}")
        }
	}
}



include("common")
include("fabric")
include("forge")
include("neo")

rootProject.name = "XaeroPlus"
