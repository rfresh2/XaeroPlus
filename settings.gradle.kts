pluginManagement {
	repositories {
		maven("https://maven.fabricmc.net/")
		maven("https://files.minecraftforge.net/maven/")
		maven("https://maven.neoforged.net/releases")
		maven("https://maven.2b2t.vc/remote")
		mavenCentral()
		mavenLocal()
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
			library("fabric-loader", "net.fabricmc:fabric-loader:0.19.3")
			library("forge", "net.minecraftforge:forge:$minecraft_version-64.0.8")
			library("fabric-api", "net.fabricmc.fabric-api:fabric-api:0.154.0+$minecraft_version")
			library("neoforge", "net.neoforged:neoforge:$minecraft_version.71")
            library("worldmap-fabric", "maven.modrinth:xaeros-world-map:fabric-$minecraft_version-${worldmap_version_fabric}")
            library("worldmap-forge", "maven.modrinth:xaeros-world-map:forge-$minecraft_version-${worldmap_version_forge}")
			library("worldmap-neo", "maven.modrinth:xaeros-world-map:neoforge-$minecraft_version-${worldmap_version_neo}")
            library("minimap-fabric", "maven.modrinth:xaeros-minimap:fabric-$minecraft_version-${minimap_version_fabric}")
            library("minimap-forge", "maven.modrinth:xaeros-minimap:forge-$minecraft_version-${minimap_version_forge}")
            library("minimap-neo", "maven.modrinth:xaeros-minimap:neoforge-$minecraft_version-${minimap_version_neo}")
            library("xaerolib-fabric", "xaero.lib:xaerolib-fabric-$minecraft_version:${xaerolib_version}")
			library("xaerolib-forge", "xaero.lib:xaerolib-forge-1.21.11:${xaerolib_version}")
			library("xaerolib-neo", "xaero.lib:xaerolib-neoforge-$minecraft_version:${xaerolib_version}")
            library("mixinextras-common", "io.github.llamalad7:mixinextras-common:0.5.4")
            library("mixinextras-forge", "io.github.llamalad7:mixinextras-forge:0.5.4")
            library("caffeine", "com.github.ben-manes.caffeine:caffeine:3.2.4")
			library("lambdaEvents", "net.lenni0451:LambdaEvents:2.4.2")
			library("waystones-fabric", "net.blay09.mods:waystones-fabric:$minecraft_version.5")
			library("waystones-forge", "maven.modrinth:waystones:21.11.9+forge-1.21.11")
			library("waystones-neoforge", "net.blay09.mods:waystones-neoforge:$minecraft_version.5")
			library("balm-fabric", "net.blay09.mods:balm-fabric:$minecraft_version.6")
			library("balm-forge", "maven.modrinth:balm:21.11.8+forge-1.21.11")
			library("balm-neoforge", "net.blay09.mods:balm-neoforge:$minecraft_version.6")
			library("fabric-waystones", "maven.modrinth:fwaystones:3.3.5+mc1.21.4")
			library("worldtools", "maven.modrinth:worldtools:1.2.8+1.21.4")
            library("sqlite", "org.rfresh.xerial:sqlite-jdbc:3.53.2.0") // relocated xerial sqlite to avoid conflicts with other mods
			library("immediatelyfast", "maven.modrinth:immediatelyfast:1.15.3+26.1-fabric")
			library("immediatelyfast-neo", "maven.modrinth:immediatelyfast:1.15.3+26.1-neoforge")
			library("modmenu", "maven.modrinth:modmenu:18.0.0-beta.1")
			library("sodium-fabric", "net.caffeinemc:sodium-fabric:0.8.12+mc$minecraft_version")
            library("sodium-neoforge", "net.caffeinemc:sodium-neoforge-mod:0.8.12+mc$minecraft_version")
            library("spark-fabric", "maven.modrinth:spark:1.10.172-fabric")
            library("fabric-permissions-api", "maven.modrinth:fabric-permissions-api:0.7.0")
            library("oldbiomes", "com.github.rfresh2:OldBiomes:1.0.0")
            library("baritone-fabric", "com.github.rfresh2:baritone-fabric:$minecraft_version-SNAPSHOT")
            library("baritone-forge", "com.github.rfresh2:baritone-forge:$minecraft_version-SNAPSHOT")
            library("baritone-neoforge", "com.github.rfresh2:baritone-neoforge:$minecraft_version-SNAPSHOT")
		}
	}
}

include("common")
include("fabric")
//include("forge")
include("neo")

rootProject.name = "XaeroPlus"
