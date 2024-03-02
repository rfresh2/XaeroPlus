plugins {
	id("dev.architectury.loom") version "1.5-SNAPSHOT"
	id("maven-publish")
	id("com.github.johnrengelman.shadow") version "8.1.1"
}

architectury {
	platformSetupLoomIde()
	fabric()
}

loom {
	accessWidenerPath = project(":common").loom.accessWidenerPath
}

val jarLibs: Configuration by configurations.creating
configurations.implementation.get().extendsFrom(jarLibs)

val common: Configuration by configurations.creating
val shadowCommon: Configuration by configurations.creating
configurations.compileClasspath.get().extendsFrom(common)
configurations.runtimeClasspath.get().extendsFrom(common)
configurations.getByName("developmentFabric").extendsFrom(common)

afterEvaluate {
	loom.runs.configureEach {
		// https://fabricmc.net/wiki/tutorial:mixin_hotswaps
		vmArg("-javaagent:${ configurations.compileClasspath.get().find { it.name.contains("sponge-mixin") } }")
//		vmArg("-Dmixin.debug.export=true")
	}
}

val worldmap_version: String by rootProject
val minimap_version: String by rootProject
val minecraft_version: String by rootProject
val parchment_version: String by rootProject
val loader_version: String by rootProject
val fabric_version: String by rootProject

dependencies {
	modImplementation("net.fabricmc:fabric-loader:${loader_version}")
	modApi("net.fabricmc.fabric-api:fabric-api:${fabric_version}")

//	compileOnly(project(":common"))

	implementation(include("com.github.ben-manes.caffeine:caffeine:3.1.8")!!)
	implementation(include("net.lenni0451:LambdaEvents:2.4.1")!!)
	jarLibs("org.xerial:sqlite-jdbc:3.45.1.0")

	modImplementation("maven.modrinth:xaeros-world-map:${worldmap_version}_Fabric_1.20")
	modImplementation("maven.modrinth:xaeros-minimap:${minimap_version}_Fabric_1.20")

	modCompileOnly(files("libs/baritone-api-fabric-1.20.1-elytra-beta-v1.jar"))
	modCompileOnly("maven.modrinth:waystones:14.0.2+fabric-1.20")
	modCompileOnly("maven.modrinth:balm:7.1.4+fabric-1.20.1")
	modCompileOnly("maven.modrinth:fwaystones:3.1.3+mc1.20")
	modCompileOnly("maven.modrinth:worldtools:1.2.0+1.20.1")
//	modRuntimeOnly "maven.modrinth:owo-lib:0.11.2+1.20"
//	runtimeOnly "blue.endless:jankson:1.2.3"
//	modRuntimeOnly "maven.modrinth:auth-me:7.0.2+1.20"
	modRuntimeOnly("maven.modrinth:immediatelyfast:1.2.10+1.20.4-fabric")
	modRuntimeOnly("maven.modrinth:modmenu:7.2.2")
	modRuntimeOnly("maven.modrinth:sodium:mc1.20.1-0.5.3")
	modRuntimeOnly("maven.modrinth:fpsdisplay:3.1.0+1.20.x")
	modRuntimeOnly("me.shedaniel.cloth:cloth-config-fabric:11.1.118") {
		exclude(group = "net.fabricmc.fabric-api")
	}

	common(project(path = ":common", configuration = "namedElements")) { isTransitive = false }
    shadowCommon(project(path = ":common", configuration = "transformProductionFabric")) { isTransitive = false }
}

tasks {
	processResources {
		filesMatching("fabric.mod.json") {
			expand(mapOf(
				"version" to project.version,
				"worldmap_version" to worldmap_version,
				"minimap_version" to minimap_version
			))
		}
	}

	withType(JavaCompile::class.java).configureEach {
		options.release.set(17)
	}

	shadowJar {
		configurations = listOf(jarLibs, shadowCommon)
		archiveClassifier.set("shadow")

		exclude("com/google/**")
		exclude("org/objectweb/**")
		exclude("org/checkerframework/**")
		exclude("org/sqlite/native/FreeBSD/**")
		exclude("org/sqlite/native/Linux-Android/**")
		exclude("org/sqlite/native/Linux-Musl/**")
		exclude("org/sqlite/native/Linux/arm/**")
		exclude("org/sqlite/native/Linux/aarch64/**")
		exclude("org/sqlite/native/Linux/armv6/**")
		exclude("org/sqlite/native/Linux/x86/**")
		exclude("org/sqlite/native/Linux/armv7/**")
		exclude("org/sqlite/native/Linux/ppc64/**")
		exclude("org/sqlite/native/Windows/armv7/**")
		exclude("org/sqlite/native/Windows/aarch64/**")
		exclude("org/sqlite/native/Windows/armv7/**")
		exclude("org/slf4j/**")
	}

	remapJar {
		injectAccessWidener = true
		dependsOn(shadowJar)
		inputFile.set(shadowJar.get().archiveFile)
	}
}

java {
	sourceCompatibility = JavaVersion.VERSION_17
	targetCompatibility = JavaVersion.VERSION_17
}
