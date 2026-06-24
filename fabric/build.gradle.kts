import net.fabricmc.loom.task.prod.ClientProductionRunTask

plugins {
	id("xaeroplus-all.conventions")
	id("xaeroplus-platform.conventions")
}

architectury {
	platformSetupLoomIde()
	fabric()
}

loom {
	accessWidenerPath = project(":common").loom.accessWidenerPath
	runs {
		getByName("client") {
			generateRunConfig = true
			preferGradleTask = true
			client()
		}
	}
}

val common = configurations.create("common")
configurations.compileClasspath.get().extendsFrom(common)
configurations.runtimeClasspath.get().extendsFrom(common)
configurations.getByName("developmentFabric").extendsFrom(common)

afterEvaluate {
	loom.runs.configureEach {
		// https://fabricmc.net/wiki/tutorial:mixin_hotswaps
		jvmArguments.addAll(
			"-javaagent:${ configurations.compileClasspath.get().find { it.name.contains("sponge-mixin") } }",
//			"-Dmixin.debug.export=true"
		)
	}
}

val worldmap_version_fabric = providers.gradleProperty("worldmap_version_fabric").get()
val minimap_version_fabric= providers.gradleProperty("minimap_version_fabric").get()
val minecraft_version = providers.gradleProperty("minecraft_version").get()
val destArchiveVersion = "${project.version}+${loom.platform.get().id()}-${minecraft_version}"
val destArchiveClassifier = "WM${worldmap_version_fabric}-MM${minimap_version_fabric}"

dependencies {
	modImplementation(libs.fabric.loader)
	modApi(libs.fabric.api)
	shadow(libs.sqlite)
	implementation(libs.sqlite)
	modImplementation(libs.worldmap.fabric) { isTransitive = false }
	modImplementation(libs.minimap.fabric) { isTransitive = false }
	modImplementation(libs.xaerolib.fabric)
	modImplementation(libs.baritone.fabric)
	modImplementation(libs.waystones.fabric)
	modImplementation(libs.balm.fabric)
	modCompileOnly(libs.fabric.waystones)
//	modRuntimeOnly(libs.immediatelyfast)
	modImplementation(libs.modmenu)
    modImplementation(libs.sodium.fabric)
    modRuntimeOnly(libs.spark.fabric)
	modRuntimeOnly(libs.fabric.permissions.api)
    implementation(shadow(libs.caffeine.get())!!)
	implementation(shadow(libs.lambdaEvents.get())!!)
	implementation(shadow(libs.oldbiomes.get())!!)
	productionRuntimeMods(libs.minimap.fabric)
	productionRuntimeMods(libs.worldmap.fabric)
	productionRuntimeMods(libs.fabric.api)

	common(project(path = ":common", configuration = "namedElements")) { isTransitive = false }
    shadow(project(path = ":common", configuration = "transformProductionFabric")) { isTransitive = false }
}

tasks {
	processResources {
		filesMatching("fabric.mod.json") {
			expand(mapOf(
				"version" to project.version,
				"worldmap_version" to worldmap_version_fabric,
				"minimap_version" to minimap_version_fabric
			))
		}
	}

	shadowJar {
		configurations = listOf(project.configurations.shadow.get())
		destinationDirectory = project.layout.buildDirectory.dir("shadowJar")
	}

	remapJar {
		injectAccessWidener = true
		dependsOn(shadowJar)
		inputFile.set(shadowJar.get().archiveFile)
		archiveVersion = destArchiveVersion
		archiveClassifier = destArchiveClassifier
	}

	register<ClientProductionRunTask>("runProdTest") {
		this.jvmArgs.add("-DXP_CI_TEST")
	}

	register<ClientProductionRunTask>("runProd") {

	}
}
