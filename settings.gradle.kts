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
	set("mod_version", "2.16")
	set("maven_group", "xaeroplus")
	set("archives_base_name", "XaeroPlus")
	set("minecraft_version", "1.21")
	set("parchment_version", "2024.06.23")
	set("fabric_loader_version", "0.15.11")
	set("fabric_api_version", "0.100.2+1.21")
	set("forge_loader_version", "51.0.23")
    set("neoforge_version", "21.0.77-beta")
    set("worldmap_version_fabric", "1.38.8")
    set("minimap_version_fabric", "24.2.1")
    set("worldmap_version_forge", "1.38.8")
    set("minimap_version_forge", "24.2.1")
    set("worldmap_version_neo", "1.38.8")
    set("minimap_version_neo", "24.2.1")
}

dependencyResolutionManagement {
	versionCatalogs {
		create("libs") {
			library("minecraft", "com.mojang:minecraft:${gradle.extra.get("minecraft_version")}")
			library("fabric-loader", "net.fabricmc:fabric-loader:${gradle.extra.get("fabric_loader_version")}")
			library("forge", "net.minecraftforge:forge:${gradle.extra.get("minecraft_version")}-${gradle.extra.get("forge_loader_version")}")
			library("fabric-api", "net.fabricmc.fabric-api:fabric-api:${gradle.extra.get("fabric_api_version")}")
			library("parchment", "org.parchmentmc.data:parchment-1.21:${gradle.extra.get("parchment_version")}")
			library("neoforge", "net.neoforged:neoforge:${gradle.extra.get("neoforge_version")}")
			library("worldmap-fabric", "maven.modrinth:xaeros-world-map:${gradle.extra.get("worldmap_version_fabric")}_Fabric_${gradle.extra.get("minecraft_version")}")
			library("worldmap-forge", "maven.modrinth:xaeros-world-map:${gradle.extra.get("worldmap_version_forge")}_Forge_${gradle.extra.get("minecraft_version")}")
			library("worldmap-neo", "maven.modrinth:xaeros-world-map:${gradle.extra.get("worldmap_version_neo")}_NeoForge_${gradle.extra.get("minecraft_version")}")
			library("minimap-fabric", "maven.modrinth:xaeros-minimap:${gradle.extra.get("minimap_version_fabric")}_Fabric_${gradle.extra.get("minecraft_version")}")
			library("minimap-forge", "maven.modrinth:xaeros-minimap:${gradle.extra.get("minimap_version_forge")}_Forge_${gradle.extra.get("minecraft_version")}")
			library("minimap-neo", "maven.modrinth:xaeros-minimap:${gradle.extra.get("minimap_version_neo")}_NeoForge_${gradle.extra.get("minecraft_version")}")
			library("mixinextras-common", "io.github.llamalad7:mixinextras-common:0.3.6")
			library("mixinextras-forge", "io.github.llamalad7:mixinextras-forge:0.3.6")
			library("caffeine", "com.github.ben-manes.caffeine:caffeine:3.1.8")
			library("lambdaEvents", "net.lenni0451:LambdaEvents:2.4.2")
			library("waystones-fabric", "maven.modrinth:waystones:21.0.5+fabric-1.21")
			library("waystones-forge", "maven.modrinth:waystones:21.0.5+forge-1.21")
			library("waystones-neoforge", "maven.modrinth:waystones:21.0.5+neoforge-1.21")
			library("balm-fabric", "maven.modrinth:balm:21.0.6+fabric-1.21")
			library("balm-forge", "maven.modrinth:balm:21.0.11+forge-1.21")
			library("balm-neoforge", "maven.modrinth:balm:21.0.11+neoforge-1.21")
			library("fabric-waystones", "maven.modrinth:fwaystones:3.3.2+mc1.20.4")
			library("worldtools", "maven.modrinth:worldtools:1.2.0+1.20.4")
            library("sqlite", "com.github.rfresh2:sqlite-jdbc:2ba0c66439") // relocated xerial sqlite to avoid conflicts with other mods
			library("immediatelyfast", "maven.modrinth:immediatelyfast:1.2.18+1.21-fabric")
			library("immediatelyfast-neo", "maven.modrinth:immediatelyfast:1.2.18+1.21-neoforge")
			library("modmenu", "maven.modrinth:modmenu:11.0.1")
			library("sodium", "maven.modrinth:sodium:mc1.21-0.5.11")
//			library("fpsdisplay", "maven.modrinth:fpsdisplay:3.1.0+1.20.x")
			library("cloth-config-fabric", "me.shedaniel.cloth:cloth-config-fabric:15.0.127")
            library("embeddium", "maven.modrinth:embeddium:1.0.3+mc1.21")
        }
	}
}



include("common")
include("fabric")
include("forge")
include("neo")

rootProject.name = "XaeroPlus"
