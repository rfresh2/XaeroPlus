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
val worldmap_version_neo: String by ext.properties
val minimap_version_neo: String by ext.properties
val xaerolib_version: String by ext.properties

dependencyResolutionManagement {
	versionCatalogs {
		create("libs") {
			library("fabric-loader", "net.fabricmc:fabric-loader:0.16.10")
			library("forge", "net.minecraftforge:forge:${minecraft_version}-54.1.5")
			library("fabric-api", "net.fabricmc.fabric-api:fabric-api:0.119.3+${minecraft_version}")
			library("neoforge", "net.neoforged:neoforge:21.4.148")
            library("worldmap-fabric", "maven.modrinth:xaeros-world-map:fabric-${minecraft_version}-${worldmap_version_fabric}")
            library("worldmap-forge", "maven.modrinth:xaeros-world-map:forge-${minecraft_version}-${worldmap_version_forge}")
            library("worldmap-neo", "maven.modrinth:xaeros-world-map:neoforge-${minecraft_version}-${worldmap_version_neo}")
            library("minimap-fabric", "maven.modrinth:xaeros-minimap:fabric-${minecraft_version}-${minimap_version_fabric}")
            library("minimap-forge", "maven.modrinth:xaeros-minimap:forge-${minecraft_version}-${minimap_version_forge}")
            library("minimap-neo", "maven.modrinth:xaeros-minimap:neoforge-${minecraft_version}-${minimap_version_neo}")
            library("xaerolib-fabric", "xaero.lib:xaerolib-fabric-${minecraft_version}:${xaerolib_version}")
            library("xaerolib-forge", "xaero.lib:xaerolib-forge-${minecraft_version}:${xaerolib_version}")
            library("xaerolib-neo", "xaero.lib:xaerolib-neoforge-${minecraft_version}:${xaerolib_version}")
            library("mixinextras-common", "io.github.llamalad7:mixinextras-common:0.5.4")
            library("mixinextras-forge", "io.github.llamalad7:mixinextras-forge:0.5.4")
			library("caffeine", "com.github.ben-manes.caffeine:caffeine:3.2.0")
			library("lambdaEvents", "net.lenni0451:LambdaEvents:2.4.2")
			library("waystones-fabric", "maven.modrinth:waystones:21.4.16+fabric-1.21.4")
			library("waystones-forge", "maven.modrinth:waystones:21.4.16+forge-1.21.4")
			library("waystones-neoforge", "maven.modrinth:waystones:21.4.16+neoforge-1.21.4")
			library("balm-fabric", "maven.modrinth:balm:21.4.36+fabric-1.21.4")
			library("balm-forge", "maven.modrinth:balm:21.4.36+forge-1.21.4")
			library("balm-neoforge", "maven.modrinth:balm:21.4.36+neoforge-1.21.4")
			library("fabric-waystones", "maven.modrinth:fwaystones:3.3.5+mc1.21.4")
			library("worldtools", "maven.modrinth:worldtools:1.2.8+1.21.4")
            library("sqlite", "org.rfresh.xerial:sqlite-jdbc:3.53.1.0") // relocated xerial sqlite to avoid conflicts with other mods
			library("immediatelyfast", "maven.modrinth:immediatelyfast:1.8.1+1.21.4-fabric")
			library("immediatelyfast-neo", "maven.modrinth:immediatelyfast:1.8.1+1.21.4-neoforge")
			library("modmenu", "maven.modrinth:modmenu:13.0.3")
			library("sodium-fabric", "maven.modrinth:sodium:mc1.21.4-0.6.13-fabric")
			library("sodium-neoforge", "maven.modrinth:sodium:mc1.21.4-0.6.13-neoforge")
            library("spark-fabric", "maven.modrinth:spark:1.10.121-fabric")
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
