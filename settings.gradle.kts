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
	set("minecraft_version", "1.21.1")
	set("parchment_version", "2024.11.17")
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
			library("fabric-loader", "net.fabricmc:fabric-loader:0.15.11")
			library("forge", "net.minecraftforge:forge:${gradle.extra.get("minecraft_version")}-52.0.38")
			library("fabric-api", "net.fabricmc.fabric-api:fabric-api:0.114.0+1.21.1")
			library("neoforge", "net.neoforged:neoforge:21.1.92")
			library("worldmap-fabric", "maven.modrinth:xaeros-world-map:${gradle.extra.get("worldmap_version_fabric")}_Fabric_1.21")
			library("worldmap-forge", "maven.modrinth:xaeros-world-map:${gradle.extra.get("worldmap_version_forge")}_Forge_1.21")
			library("worldmap-neo", "maven.modrinth:xaeros-world-map:${gradle.extra.get("worldmap_version_neo")}_NeoForge_1.21")
			library("minimap-fabric", "maven.modrinth:xaeros-minimap:${gradle.extra.get("minimap_version_fabric")}_Fabric_1.21")
			library("minimap-forge", "maven.modrinth:xaeros-minimap:${gradle.extra.get("minimap_version_forge")}_Forge_1.21")
			library("minimap-neo", "maven.modrinth:xaeros-minimap:${gradle.extra.get("minimap_version_neo")}_NeoForge_1.21")
            library("mixinextras-common", "io.github.llamalad7:mixinextras-common:0.4.1")
            library("mixinextras-forge", "io.github.llamalad7:mixinextras-forge:0.4.1")
			library("caffeine", "com.github.ben-manes.caffeine:caffeine:3.2.0")
			library("lambdaEvents", "net.lenni0451:LambdaEvents:2.4.2")
			library("waystones-fabric", "maven.modrinth:waystones:21.1.4+fabric-1.21.1")
			library("waystones-forge", "maven.modrinth:waystones:21.1.4+forge-1.21.1")
			library("waystones-neoforge", "maven.modrinth:waystones:21.1.4+neoforge-1.21.1")
			library("balm-fabric", "maven.modrinth:balm:21.0.20+fabric-1.21.1")
			library("balm-forge", "maven.modrinth:balm:21.0.20+forge-1.21.1")
			library("balm-neoforge", "maven.modrinth:balm:21.0.20+neoforge-1.21.1")
			library("fabric-waystones", "maven.modrinth:fwaystones:3.3.4+mc1.21.1")
			library("worldtools", "maven.modrinth:worldtools:1.2.6+1.21.1")
            library("sqlite", "org.rfresh.xerial:sqlite-jdbc:3.47.1.0") // relocated xerial sqlite to avoid conflicts with other mods
			library("immediatelyfast", "maven.modrinth:immediatelyfast:1.2.21+1.21.1-fabric")
			library("immediatelyfast-neo", "maven.modrinth:immediatelyfast:1.2.21+1.21.1-neoforge")
			library("modmenu", "maven.modrinth:modmenu:11.0.3")
			library("sodium-fabric", "maven.modrinth:sodium:mc1.21.1-0.6.0-fabric")
			library("sodium-neoforge", "maven.modrinth:sodium:mc1.21.1-0.6.0-neoforge")
//			library("fpsdisplay", "maven.modrinth:fpsdisplay:3.1.0+1.20.x")
			library("cloth-config-fabric", "me.shedaniel.cloth:cloth-config-fabric:15.0.127")
            library("oldbiomes", "com.github.rfresh2:OldBiomes:1.0.0")
            library("baritone-fabric", "com.github.rfresh2:baritone-fabric:1.21")
            library("baritone-forge", "com.github.rfresh2:baritone-forge:1.21")
            library("baritone-neoforge", "com.github.rfresh2:baritone-neoforge:1.21")
        }
	}
}



include("common")
include("fabric")
include("forge")
include("neo")

rootProject.name = "XaeroPlus"
