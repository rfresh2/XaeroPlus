pluginManagement {
	repositories {
		maven("https://maven.fabricmc.net/") { name = "Fabric" }
		maven("https://maven.architectury.dev/")
		maven("https://files.minecraftforge.net/maven/")
		mavenCentral()
		gradlePluginPortal()
	}
}
gradle.extra.apply {
	set("mod_version", "2.4")
	set("maven_group", "xaeroplus")
	set("archives_base_name", "XaeroPlus")
	set("minecraft_version", "1.19.4")
	set("parchment_version", "2023.06.26")
	set("fabric_loader_version", "0.15.2")
	set("fabric_api_version", "0.87.2+1.19.4")
	set("forge_loader_version", "45.2.8")
    set("worldmap_version_fabric", "1.38.1")
    set("minimap_version_fabric", "24.0.2")
    set("worldmap_version_forge", "1.38.1")
    set("minimap_version_forge", "24.0.1")
}

dependencyResolutionManagement {
	versionCatalogs {
		create("libs") {
			library("minecraft", "com.mojang:minecraft:${gradle.extra.get("minecraft_version")}")
			library("fabric-loader", "net.fabricmc:fabric-loader:${gradle.extra.get("fabric_loader_version")}")
			library("forge", "net.minecraftforge:forge:${gradle.extra.get("minecraft_version")}-${gradle.extra.get("forge_loader_version")}")
			library("fabric-api", "net.fabricmc.fabric-api:fabric-api:${gradle.extra.get("fabric_api_version")}")
			library("parchment", "org.parchmentmc.data:parchment-${gradle.extra.get("minecraft_version")}:${gradle.extra.get("parchment_version")}")
			library("worldmap-fabric", "maven.modrinth:xaeros-world-map:${gradle.extra.get("worldmap_version_fabric")}_Fabric_${gradle.extra.get("minecraft_version")}")
			library("worldmap-forge", "maven.modrinth:xaeros-world-map:${gradle.extra.get("worldmap_version_forge")}_Forge_${gradle.extra.get("minecraft_version")}")
            library("minimap-fabric", "maven.modrinth:xaeros-minimap:${gradle.extra.get("minimap_version_fabric")}_Fabric_${gradle.extra.get("minecraft_version")}")
            library("minimap-forge", "maven.modrinth:xaeros-minimap:${gradle.extra.get("minimap_version_forge")}_Forge_${gradle.extra.get("minecraft_version")}")
			library("mixinextras-common", "io.github.llamalad7:mixinextras-common:0.3.5")
			library("mixinextras-forge", "io.github.llamalad7:mixinextras-forge:0.3.5")
			library("caffeine", "com.github.ben-manes.caffeine:caffeine:3.1.8")
			library("lambdaEvents", "net.lenni0451:LambdaEvents:2.4.1")
			library("waystones-fabric", "maven.modrinth:waystones:13.1.0+fabric-1.19.4")
			library("waystones-forge", "maven.modrinth:waystones:13.1.0+forge-1.19.4")
			library("balm-fabric", "maven.modrinth:balm:6.0.2+fabric-1.19.4")
			library("balm-forge", "maven.modrinth:balm:6.0.2+forge-1.19.4")
			library("fabric-waystones", "maven.modrinth:fwaystones:3.1.1+mc1.19.4")
			library("worldtools", "maven.modrinth:worldtools:1.2.0+1.20.1")
            library("sqlite", "com.github.rfresh2:sqlite-jdbc:2ba0c66439") // relocated xerial sqlite to avoid conflicts with other mods
		}
	}
}



include("common")
include("fabric")
include("forge")

rootProject.name = "XaeroPlus"
