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
	set("mod_version", "2.24.6")
	set("minecraft_version", "1.21.4")
	set("parchment_version", "2024.07.28")
    set("worldmap_version_fabric", "1.39.1")
	set("minimap_version_fabric", "24.6.2")
	set("worldmap_version_forge", "1.39.1")
	set("minimap_version_forge", "24.6.2")
	set("worldmap_version_neo", "1.39.1")
	set("minimap_version_neo", "24.6.2")
}

dependencyResolutionManagement {
	versionCatalogs {
		create("libs") {
			library("fabric-loader", "net.fabricmc:fabric-loader:0.16.9")
			library("forge", "net.minecraftforge:forge:1.21.3-53.0.25")
			library("fabric-api", "net.fabricmc.fabric-api:fabric-api:0.110.5+1.21.4")
			library("neoforge", "net.neoforged:neoforge:21.4.7-beta")
			library("worldmap-fabric", "maven.modrinth:xaeros-world-map:${gradle.extra.get("worldmap_version_fabric")}_Fabric_${gradle.extra.get("minecraft_version")}")
			library("worldmap-forge", "maven.modrinth:xaeros-world-map:${gradle.extra.get("worldmap_version_forge")}_Forge_1.21.3")
			library("worldmap-neo", "maven.modrinth:xaeros-world-map:${gradle.extra.get("worldmap_version_neo")}_NeoForge_${gradle.extra.get("minecraft_version")}")
			library("minimap-fabric", "maven.modrinth:xaeros-minimap:${gradle.extra.get("minimap_version_fabric")}_Fabric_${gradle.extra.get("minecraft_version")}")
			library("minimap-forge", "maven.modrinth:xaeros-minimap:${gradle.extra.get("minimap_version_forge")}_Forge_1.21.3")
			library("minimap-neo", "maven.modrinth:xaeros-minimap:${gradle.extra.get("minimap_version_neo")}_NeoForge_${gradle.extra.get("minecraft_version")}")
            library("mixinextras-common", "io.github.llamalad7:mixinextras-common:0.4.1")
            library("mixinextras-forge", "io.github.llamalad7:mixinextras-forge:0.4.1")
			library("caffeine", "com.github.ben-manes.caffeine:caffeine:3.1.8")
			library("lambdaEvents", "net.lenni0451:LambdaEvents:2.4.2")
			library("waystones-fabric", "maven.modrinth:waystones:21.3.3+fabric-1.21.3")
			library("waystones-forge", "maven.modrinth:waystones:21.1.4+forge-1.21.1")
			library("waystones-neoforge", "maven.modrinth:waystones:21.3.3+neoforge-1.21.3")
			library("balm-fabric", "maven.modrinth:balm:21.3.3+fabric-1.21.3")
			library("balm-forge", "maven.modrinth:balm:21.0.20+forge-1.21.1")
			library("balm-neoforge", "maven.modrinth:balm:21.3.3+neoforge-1.21.3")
			library("fabric-waystones", "maven.modrinth:fwaystones:3.3.4+mc1.21.1")
			library("worldtools", "maven.modrinth:worldtools:1.2.6+1.21.1")
            library("sqlite", "org.rfresh.xerial:sqlite-jdbc:3.47.1.0") // relocated xerial sqlite to avoid conflicts with other mods
			library("immediatelyfast", "maven.modrinth:immediatelyfast:1.3.3+1.21.4-fabric")
			library("immediatelyfast-neo", "maven.modrinth:immediatelyfast:1.3.3+1.21.4-neoforge")
			library("modmenu", "maven.modrinth:modmenu:13.0.0-beta.1")
			library("sodium-fabric", "maven.modrinth:sodium:mc1.21.4-0.6.2-fabric")
			library("sodium-neoforge", "maven.modrinth:sodium:mc1.21.4-0.6.2-neoforge")
//			library("fpsdisplay", "maven.modrinth:fpsdisplay:3.1.0+1.20.x")
			library("cloth-config-fabric", "me.shedaniel.cloth:cloth-config-fabric:16.0.141")
            library("oldbiomes", "com.github.rfresh2:OldBiomes:1.0.0")
            library("baritone-fabric", "com.github.rfresh2:baritone-fabric:1.21.3")
            library("baritone-forge", "com.github.rfresh2:baritone-forge:1.21.3")
            library("baritone-neoforge", "com.github.rfresh2:baritone-neoforge:1.21.3")
        }
	}
}



include("common")
include("fabric")
//include("forge")
include("neo")

rootProject.name = "XaeroPlus"
