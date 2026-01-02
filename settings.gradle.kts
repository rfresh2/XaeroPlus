pluginManagement {
	repositories {
		maven("https://maven.fabricmc.net/") { name = "Fabric" }
		maven("https://maven.architectury.dev/")
		maven("https://files.minecraftforge.net/maven/")
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
val xaerolib_version: String by ext.properties

dependencyResolutionManagement {
	versionCatalogs {
		create("libs") {
			library("fabric-loader", "net.fabricmc:fabric-loader:0.15.11")
			library("forge", "net.minecraftforge:forge:${minecraft_version}-47.4.4")
            library("fabric-api", "net.fabricmc.fabric-api:fabric-api:0.92.3+${minecraft_version}")
			library("worldmap-fabric", "maven.modrinth:xaeros-world-map:${worldmap_version_fabric}_Fabric_1.20")
			library("worldmap-forge", "maven.modrinth:xaeros-world-map:${worldmap_version_forge}_Forge_1.20")
			library("minimap-fabric", "maven.modrinth:xaeros-minimap:${minimap_version_fabric}_Fabric_1.20")
			library("minimap-forge", "maven.modrinth:xaeros-minimap:${minimap_version_forge}_Forge_1.20")
			library("xaerolib-fabric", "xaero.lib:xaerolib-fabric-1.20:${xaerolib_version}")
			library("xaerolib-forge", "xaero.lib:xaerolib-forge-1.20:${xaerolib_version}")
			library("mixinextras-common", "io.github.llamalad7:mixinextras-common:0.4.1")
			library("mixinextras-forge", "io.github.llamalad7:mixinextras-forge:0.4.1")
			library("caffeine", "com.github.ben-manes.caffeine:caffeine:3.2.0")
			library("lambdaEvents", "net.lenni0451:LambdaEvents:2.4.2")
			library("waystones-fabric", "maven.modrinth:waystones:14.1.17+fabric-1.20.1")
			library("waystones-forge", "maven.modrinth:waystones:14.1.17+forge-1.20.1")
			library("balm-fabric", "maven.modrinth:balm:7.3.35+fabric-1.20.1")
			library("balm-forge", "maven.modrinth:balm:7.3.35+forge-1.20.1")
			library("fabric-waystones", "maven.modrinth:fwaystones:3.3.3+mc1.20.1")
			library("worldtools", "maven.modrinth:worldtools:1.2.4+1.20.1")
			library("sqlite", "org.rfresh.xerial:sqlite-jdbc:3.50.3.1") // relocated xerial sqlite to avoid conflicts with other mods
			library("immediatelyfast", "maven.modrinth:immediatelyfast:1.5.2+1.20.4-fabric")
			library("modmenu", "maven.modrinth:modmenu:7.2.2")
			library("sodium", "maven.modrinth:sodium:mc1.20.1-0.5.13-fabric")
			library("embeddium", "maven.modrinth:embeddium:0.3.31+mc1.20.1")
			library("fpsdisplay", "maven.modrinth:fpsdisplay:3.1.0+1.20.1")
			library("cloth-config-fabric", "me.shedaniel.cloth:cloth-config-fabric:11.1.136")
			library("opac-fabric", "maven.modrinth:open-parties-and-claims:fabric-1.20.1-0.24.0")
			library("forge-config-api-port", "maven.modrinth:forge-config-api-port:v8.0.1-1.20.1-Fabric")
			library("oldbiomes", "com.github.rfresh2:OldBiomes:1.0.0")
			library("baritone-fabric", "com.github.rfresh2:baritone-fabric:${minecraft_version}-SNAPSHOT")
			library("baritone-forge", "com.github.rfresh2:baritone-forge:${minecraft_version}-SNAPSHOT")
		}
	}
}

include("common")
include("fabric")
include("forge")

rootProject.name = "XaeroPlus"
