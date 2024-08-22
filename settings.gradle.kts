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
	set("mod_version", "2.22.2")
	set("maven_group", "xaeroplus")
	set("archives_base_name", "XaeroPlus")
	set("minecraft_version", "1.19.2")
	set("parchment_version", "2022.11.27")
	set("fabric_loader_version", "0.15.2")
	set("fabric_api_version", "0.77.0+1.19.2")
	set("forge_loader_version", "43.3.7")
    set("worldmap_version_fabric", "1.38.8")
	set("minimap_version_fabric", "24.3.0")
    set("worldmap_version_forge", "1.38.8")
	set("minimap_version_forge", "24.3.0")
}

dependencyResolutionManagement {
	versionCatalogs {
		create("libs") {
			library("minecraft", "com.mojang:minecraft:${gradle.extra.get("minecraft_version")}")
			library("fabric-loader", "net.fabricmc:fabric-loader:${gradle.extra.get("fabric_loader_version")}")
			library("forge", "net.minecraftforge:forge:${gradle.extra.get("minecraft_version")}-${gradle.extra.get("forge_loader_version")}")
			library("fabric-api", "net.fabricmc.fabric-api:fabric-api:${gradle.extra.get("fabric_api_version")}")
			library("parchment", "org.parchmentmc.data:parchment-${gradle.extra.get("minecraft_version")}:${gradle.extra.get("parchment_version")}")
			library("worldmap-fabric", "maven.modrinth:xaeros-world-map:${gradle.extra.get("worldmap_version_fabric")}_Fabric_1.19.1")
			library("worldmap-forge", "maven.modrinth:xaeros-world-map:${gradle.extra.get("worldmap_version_forge")}_Forge_1.19.1")
			library("minimap-fabric", "maven.modrinth:xaeros-minimap:${gradle.extra.get("minimap_version_fabric")}_Fabric_1.19.1")
			library("minimap-forge", "maven.modrinth:xaeros-minimap:${gradle.extra.get("minimap_version_forge")}_Forge_1.19.1")
            library("mixinextras-common", "io.github.llamalad7:mixinextras-common:0.4.0")
            library("mixinextras-forge", "io.github.llamalad7:mixinextras-forge:0.4.0")
			library("caffeine", "com.github.ben-manes.caffeine:caffeine:3.1.8")
			library("lambdaEvents", "net.lenni0451:LambdaEvents:2.4.2")
			library("waystones-fabric", "maven.modrinth:waystones:11.4.2+fabric-1.19.2")
			library("waystones-forge", "maven.modrinth:waystones:11.4.2+forge-1.19.2")
			library("balm-fabric", "maven.modrinth:balm:4.6.0+fabric-1.19.2")
			library("balm-forge", "maven.modrinth:balm:4.6.0+forge-1.19.2")
			library("fabric-waystones", "maven.modrinth:fwaystones:3.0.8+MC1.19.2")
			library("worldtools", "maven.modrinth:worldtools:1.2.0+1.20.1")
            library("sqlite", "org.rfresh.xerial:sqlite-jdbc:3.46.1.0") // relocated xerial sqlite to avoid conflicts with other mods
			library("sodium", "maven.modrinth:sodium:mc1.19.2-0.4.4")
			library("embeddium", "maven.modrinth:embeddium:0.3.18+mc1.19.2")
            library("modmenu", "maven.modrinth:modmenu:4.2.0-beta.2")
        }
	}
}



include("common")
include("fabric")
include("forge")

rootProject.name = "XaeroPlus"
