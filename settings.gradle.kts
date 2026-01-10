pluginManagement {
	repositories {
		maven("https://maven.fabricmc.net/")
		maven("https://files.minecraftforge.net/maven/")
		maven("https://maven.neoforged.net/releases")
		maven("https://maven.2b2t.vc/remote")
		maven("https://maven.architectury.dev/")
		mavenCentral()
		gradlePluginPortal()
	}
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention").version("1.0.0")
}

val minecraft_version: String by ext.properties
val worldmap_version_fabric: String by ext.properties
val minimap_version_fabric: String by ext.properties
val worldmap_version_forge: String by ext.properties
val minimap_version_forge: String by ext.properties
val worldmap_version_neo: String by ext.properties
val minimap_version_neo: String by ext.properties
val xaerolib_version: String by ext.properties

dependencyResolutionManagement {
	versionCatalogs {
		create("libs") {
			library("fabric-loader", "net.fabricmc:fabric-loader:0.16.10")
			library("forge", "net.minecraftforge:forge:${minecraft_version}-53.1.2")
			library("fabric-api", "net.fabricmc.fabric-api:fabric-api:0.114.1+1.21.3")
			library("neoforge", "net.neoforged:neoforge:21.3.87")
            library("worldmap-fabric", "xaero.map:xaeroworldmap-fabric-${minecraft_version}:${worldmap_version_fabric}")
            library("worldmap-forge", "xaero.map:xaeroworldmap-forge-${minecraft_version}:${worldmap_version_forge}")
            library("worldmap-neo", "xaero.map:xaeroworldmap-neoforge-${minecraft_version}:${worldmap_version_neo}")
            library("minimap-fabric", "xaero.minimap:xaerominimap-fabric-${minecraft_version}:${minimap_version_fabric}")
            library("minimap-forge", "xaero.minimap:xaerominimap-forge-${minecraft_version}:${minimap_version_forge}")
            library("minimap-neo", "xaero.minimap:xaerominimap-neoforge-${minecraft_version}:${minimap_version_neo}")
            library("xaerolib-fabric", "xaero.lib:xaerolib-fabric-${minecraft_version}:${xaerolib_version}")
            library("xaerolib-forge", "xaero.lib:xaerolib-forge-${minecraft_version}:${xaerolib_version}")
            library("xaerolib-neo", "xaero.lib:xaerolib-neoforge-${minecraft_version}:${xaerolib_version}")
            library("mixinextras-common", "io.github.llamalad7:mixinextras-common:0.5.2")
            library("mixinextras-forge", "io.github.llamalad7:mixinextras-forge:0.5.2")
			library("caffeine", "com.github.ben-manes.caffeine:caffeine:3.2.0")
			library("lambdaEvents", "net.lenni0451:LambdaEvents:2.4.2")
			library("waystones-fabric", "maven.modrinth:waystones:21.3.3+fabric-1.21.3")
			library("waystones-forge", "maven.modrinth:waystones:21.1.4+forge-1.21.1")
			library("waystones-neoforge", "maven.modrinth:waystones:21.3.3+neoforge-1.21.3")
			library("balm-fabric", "maven.modrinth:balm:21.3.5+fabric-1.21.3")
			library("balm-forge", "maven.modrinth:balm:21.0.20+forge-1.21.1")
			library("balm-neoforge", "maven.modrinth:balm:21.3.5+neoforge-1.21.3")
			library("fabric-waystones", "maven.modrinth:fwaystones:3.3.4+mc1.21.1")
			library("worldtools", "maven.modrinth:worldtools:1.2.6+1.21.1")
            library("sqlite", "org.rfresh.xerial:sqlite-jdbc:3.50.3.1") // relocated xerial sqlite to avoid conflicts with other mods
			library("immediatelyfast", "maven.modrinth:immediatelyfast:1.7.1+1.21.3-fabric")
			library("immediatelyfast-neo", "maven.modrinth:immediatelyfast:1.7.1+1.21.3-neoforge")
			library("modmenu", "maven.modrinth:modmenu:12.0.0")
			library("sodium-fabric", "maven.modrinth:sodium:mc1.21.3-0.6.13-fabric")
			library("sodium-neoforge", "maven.modrinth:sodium:mc1.21.3-0.6.13-neoforge")
//			library("fpsdisplay", "maven.modrinth:fpsdisplay:3.1.0+1.20.x")
			library("cloth-config-fabric", "me.shedaniel.cloth:cloth-config-fabric:16.0.141")
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
