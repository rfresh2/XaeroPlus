pluginManagement {
	repositories {
		maven("https://maven.fabricmc.net/")
		maven("https://files.minecraftforge.net/maven/")
		maven("https://maven.neoforged.net/releases")
		maven("https://maven.2b2t.vc/remote")
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
			library("worldmap-fabric", "maven.modrinth:xaeros-world-map:fabric-${minecraft_version}-${worldmap_version_fabric}")
			library("worldmap-forge", "maven.modrinth:xaeros-world-map:forge-${minecraft_version}-${worldmap_version_forge}")
			library("minimap-fabric", "maven.modrinth:xaeros-minimap:fabric-${minecraft_version}-${minimap_version_fabric}")
			library("minimap-forge", "maven.modrinth:xaeros-minimap:forge-${minecraft_version}-${minimap_version_forge}")
			library("xaerolib-fabric", "xaero.lib:xaerolib-fabric-${minecraft_version}:${xaerolib_version}")
			library("xaerolib-forge", "xaero.lib:xaerolib-forge-${minecraft_version}:${xaerolib_version}")
			library("mixinextras-common", "io.github.llamalad7:mixinextras-common:0.5.3")
			library("mixinextras-forge", "io.github.llamalad7:mixinextras-forge:0.5.3")
			library("caffeine", "com.github.ben-manes.caffeine:caffeine:3.2.0")
			library("lambdaEvents", "net.lenni0451:LambdaEvents:2.4.2")
			library("waystones-fabric", "maven.modrinth:waystones:14.1.20+fabric-1.20.1")
			library("waystones-forge", "maven.modrinth:waystones:14.1.20+forge-1.20.1")
			library("balm-fabric", "maven.modrinth:balm:7.3.38+fabric-1.20.1")
			library("balm-forge", "maven.modrinth:balm:7.3.38+forge-1.20.1")
			library("fabric-waystones", "maven.modrinth:fwaystones:3.3.3+mc1.20.1")
			library("worldtools", "maven.modrinth:worldtools:1.2.4+1.20.1")
			library("sqlite", "org.rfresh.xerial:sqlite-jdbc:3.51.2.1") // relocated xerial sqlite to avoid conflicts with other mods
			library("immediatelyfast", "maven.modrinth:immediatelyfast:1.5.4+1.20.4-fabric")
			library("modmenu", "maven.modrinth:modmenu:7.2.2")
			library("sodium", "maven.modrinth:sodium:mc1.20.1-0.5.13-fabric")
			library("embeddium", "maven.modrinth:embeddium:0.3.31+mc1.20.1")
			library("opac-fabric", "maven.modrinth:open-parties-and-claims:fabric-1.20.1-0.25.10")
			library("forge-config-api-port", "maven.modrinth:forge-config-api-port:v8.0.3-1.20.1-Fabric")
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
