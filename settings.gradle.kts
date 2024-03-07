pluginManagement {
	repositories {
		maven("https://maven.fabricmc.net/") { name = "Fabric" }
		maven("https://maven.architectury.dev/")
		maven("https://files.minecraftforge.net/maven/")
		mavenCentral()
		gradlePluginPortal()
	}
}

dependencyResolutionManagement {
	versionCatalogs {
		create("libs") {
			library("caffeine", "com.github.ben-manes.caffeine:caffeine:3.1.8")
			library("lambdaEvents", "net.lenni0451:LambdaEvents:2.4.1")
			library("waystones-fabric", "maven.modrinth:waystones:13.1.0+fabric-1.19.4")
			library("waystones-forge", "maven.modrinth:waystones:13.1.0+forge-1.19.4")
			library("balm-fabric", "maven.modrinth:balm:6.0.2+fabric-1.19.4")
			library("balm-forge", "maven.modrinth:balm:6.0.2+forge-1.19.4")
			library("fabric-waystones", "maven.modrinth:fwaystones:3.1.1+mc1.19.4")
			library("worldtools", "maven.modrinth:worldtools:1.2.0+1.20.1")
			library("sqlite", "org.xerial:sqlite-jdbc:3.45.1.0")
		}
	}
}



include("common")
include("fabric")
include("forge")

rootProject.name = "XaeroPlus"
