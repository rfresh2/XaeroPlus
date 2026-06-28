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
			library("forge", "net.minecraftforge:forge:$minecraft_version-43.5.1")
			library("fabric-api", "net.fabricmc.fabric-api:fabric-api:0.77.0+$minecraft_version")
            library("worldmap-fabric", "maven.modrinth:xaeros-world-map:fabric-$minecraft_version-${worldmap_version_fabric}")
            library("worldmap-forge", "maven.modrinth:xaeros-world-map:forge-$minecraft_version-${worldmap_version_forge}")
            library("minimap-fabric", "maven.modrinth:xaeros-minimap:fabric-$minecraft_version-${minimap_version_fabric}")
            library("minimap-forge", "maven.modrinth:xaeros-minimap:forge-$minecraft_version-${minimap_version_forge}")
            library("xaerolib-fabric", "xaero.lib:xaerolib-fabric-$minecraft_version:${xaerolib_version}")
            library("xaerolib-forge", "xaero.lib:xaerolib-forge-$minecraft_version:${xaerolib_version}")
			library("mixinextras-common", "io.github.llamalad7:mixinextras-common:0.5.4")
			library("mixinextras-forge", "io.github.llamalad7:mixinextras-forge:0.5.4")
			library("caffeine", "com.github.ben-manes.caffeine:caffeine:3.2.4")
			library("lambdaEvents", "net.lenni0451:LambdaEvents:2.4.2")
			library("waystones-fabric", "maven.modrinth:waystones:11.4.2+fabric-$minecraft_version")
			library("waystones-forge", "maven.modrinth:waystones:11.4.2+forge-$minecraft_version")
			library("balm-fabric", "maven.modrinth:balm:4.6.0+fabric-$minecraft_version")
			library("balm-forge", "maven.modrinth:balm:4.6.0+forge-$minecraft_version")
			library("fabric-waystones", "maven.modrinth:fwaystones:3.0.8+MC$minecraft_version")
			library("worldtools", "maven.modrinth:worldtools:1.2.0+1.20.1")
            library("sqlite", "org.rfresh.xerial:sqlite-jdbc:3.53.2.0") // relocated xerial sqlite to avoid conflicts with other mods
			library("sodium", "maven.modrinth:sodium:mc$minecraft_version-0.4.4")
			library("embeddium", "maven.modrinth:embeddium:0.3.18+mc$minecraft_version")
            library("modmenu", "maven.modrinth:modmenu:4.2.0-beta.2")
            library("oldbiomes", "com.github.rfresh2:OldBiomes:1.0.0")
            library("baritone-fabric", "com.github.rfresh2:baritone-fabric:1.19.4-SNAPSHOT")
            library("baritone-forge", "com.github.rfresh2:baritone-forge:1.19.4-SNAPSHOT")
        }
	}
}

include("common")
include("fabric")
include("forge")

rootProject.name = "XaeroPlus"
