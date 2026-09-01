import net.fabricmc.loom.task.RemapJarTask
import net.fabricmc.loom.task.prod.ClientProductionRunTask
import org.gradle.jvm.tasks.Jar

plugins {
	id("xaeroplus-all.conventions")
	id("xaeroplus-platform.conventions")
}

architectury {
	platformSetupLoomIde()
	fabric()
	injectInjectables = false
}

fabricApi {
	configureTests {
		createSourceSet = true
		modId = "xaeroplus-gametest"
		enableGameTests = false
		eula = true
	}
}

loom {
	accessWidenerPath = project(":common").loom.accessWidenerPath
	runs {
		named("clientGameTest") {
			jvmArguments = listOf("-Dsodium.checks.issue2561=false", "-DXP_CI_TEST", "-Dfabric.client.gametest")
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
//	tasks.named("runClientGameTest", RunGameTask::class) {
//		useXvfb = true
//	}
}

val worldmap_version_fabric = providers.gradleProperty("worldmap_version_fabric").get()
val minimap_version_fabric= providers.gradleProperty("minimap_version_fabric").get()
val minecraft_version = providers.gradleProperty("minecraft_version").get()
val destArchiveVersion = "${project.version}+${loom.platform.get().id()}-${minecraft_version}"
val destArchiveClassifier = "WM${worldmap_version_fabric}-MM${minimap_version_fabric}"

val productionRuntimeMods = configurations.getByName("productionRuntimeMods")
productionRuntimeMods.extendsFrom(configurations.getByName("modRuntimeOnly"))
productionRuntimeMods.extendsFrom(configurations.getByName("modImplementation"))
productionRuntimeMods.extendsFrom(configurations.getByName("modApi"))

dependencies {
	modImplementation(libs.fabric.loader)
	modApi(libs.fabric.api)
	shadow(libs.sqlite)
	implementation(libs.sqlite)
	modImplementation(libs.worldmap.fabric) { isTransitive = false }
	modImplementation(libs.minimap.fabric) { isTransitive = false }
	modImplementation(libs.xaerolib.fabric)
	modImplementation(libs.baritone.fabric)
	modRuntimeOnly(libs.immediatelyfast)
	runtimeOnly("net.lenni0451:Reflect:1.6.4") // immediatelyfast jij
	modImplementation(libs.modmenu)
	modImplementation(libs.sodium)
	modRuntimeOnly(libs.opac.fabric)
	modRuntimeOnly(libs.forge.config.api.port)
	modRuntimeOnly(libs.spark.fabric)
	implementation(shadow(libs.caffeine.get())!!)
	implementation(shadow(libs.lambdaEvents.get())!!)
	implementation(shadow(libs.oldbiomes.get())!!)

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

	val gametestJar = register<Jar>("gametestJar") {
		from(sourceSets["gametest"].output)
		archiveClassifier = "gametest-dev"
		destinationDirectory = project.layout.buildDirectory.dir("gametestJar")
	}

	val remapGametestJar = register<RemapJarTask>("remapGametestJar") {
		dependsOn(gametestJar)
		inputFile = gametestJar.flatMap { it.archiveFile }
		archiveClassifier = "gametest"
		destinationDirectory = project.layout.buildDirectory.dir("gametestJar")
	}

	register<ClientProductionRunTask>("runProdTest") {
		jvmArgs = listOf("-DXP_CI_TEST", "-Dfabric.client.gametest", "-Dsodium.checks.issue2561=false")
		mods.from(remapGametestJar)
		runDir = file("build/runProdTest")
		useXVFB = true
		doFirst {
			runDir.get().asFile.deleteRecursively()
		}
		outputs.upToDateWhen { false }
	}

	register<ClientProductionRunTask>("runProd") {
		jvmArgs = listOf("-Dsodium.checks.issue2561=false")
		runDir = file("build/runProd")
		outputs.upToDateWhen { false }
	}
}
