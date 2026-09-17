package xaeroplus.loom.prod;/*
 * This file is part of fabric-loom, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2025 FabricMC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

import net.fabricmc.loom.LoomGradlePlugin;
import net.fabricmc.loom.api.mappings.layered.MappingsNamespace;
import net.fabricmc.loom.task.prod.TracyCapture;
import net.fabricmc.loom.util.Constants;
import net.fabricmc.loom.util.Platform;
import net.fabricmc.loom.util.XVFBExistsValueSource;
import org.gradle.api.Action;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;
import org.gradle.process.ExecSpec;
import org.gradle.work.DisableCachingByDefault;
import org.jetbrains.annotations.ApiStatus;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.stream.Stream;

/**
 * A task that runs the Minecraft client in a similar way to a production launcher. You must manually register a task of this type to use it.
 *
 * <p>Forge and NeoForge clients are installed into the project's Loom cache using their installer.
 * Mods are copied into the run directory's {@code mods} folder, including {@code productionRuntimeMods}.
 * Only unchanged jars previously copied by this task are removed when the mod collection changes.
 */
@ApiStatus.Experimental
@DisableCachingByDefault
public abstract non-sealed class XPClientProductionRunTask extends XPAbstractProductionRunTask {
	/**
	 * Whether to use XVFB to run the game, using a virtual framebuffer. This is useful for CI environments that don't have a display server.
	 *
	 * <p>Defaults to true only on Linux and when the "CI" environment variable is set.
	 *
	 * <p>XVFB must be installed, on Debian-based systems you can install it with: <code>apt install -y xvfb</code>
	 */
	@Input
	public abstract Property<Boolean> getUseXVFB();

	@Nested
	@Optional
	public abstract Property<TracyCapture> getTracyCapture();

	/**
	 * Configures the tracy profiler to run alongside the game. See @{@link TracyCapture} for more information.
	 *
	 * @param action The configuration action.
	 */
	public void tracy(Action<? super TracyCapture> action) {
		getTracyCapture().set(getProject().getObjects().newInstance(TracyCapture.class));
		getTracyCapture().finalizeValue();
		action.execute(getTracyCapture().get());
	}

	// Internal options
	@Input
	protected abstract Property<String> getAssetsIndex();

	@InputFiles
	@PathSensitive(PathSensitivity.ABSOLUTE)
	protected abstract DirectoryProperty getAssetsDir();

	@InputFile
	@Optional
	@PathSensitive(PathSensitivity.NONE)
	protected abstract RegularFileProperty getForgeInstaller();

	@Internal
	protected abstract DirectoryProperty getForgeInstallation();

	@Input
	protected abstract Property<String> getMinecraftMetadata();

	@Input
	protected abstract Property<Boolean> getOffline();

	@Internal
	protected abstract DirectoryProperty getNativesDirectory();

	@Input
	protected abstract MapProperty<String, String> getRuntimeLibraryCoordinates();

	private XPForgeProductionClient forgeClient;

	@Inject
	public XPClientProductionRunTask() {
		getUseXVFB().convention(getProject().getProviders().environmentVariable("CI")
				.map(value -> Platform.CURRENT.getOperatingSystem().isLinux())
				.orElse(false)
		);

		getAssetsIndex().set(getExtension().getMinecraftVersion()
				.map(minecraftVersion -> getExtension()
						.getMinecraftProvider()
						.getVersionInfo()
						.assetIndex()
						.fabricId(minecraftVersion)
				)
		);
		getAssetsDir().set(new File(getExtension().getFiles().getUserCache(), "assets"));
		getMinecraftMetadata().set(LoomGradlePlugin.GSON.toJson(getExtension().getMinecraftProvider().getVersionInfo()));
		getOffline().set(getProject().getGradle().getStartParameter().isOffline());
		getNativesDirectory().set(getExtension().getFiles().getNativesDirectory(getProject()));

		if (getExtension().getMinecraftProvider().getVersionInfo().hasNativesToExtract()) {
			dependsOn("extractNatives");
		}

		if (getExtension().isForgeLike()) {
			getForgeInstaller().set(getProject().getLayout().file(getProject().provider(() -> getProject().getConfigurations()
					.getByName(Constants.Configurations.FORGE_INSTALLER).getSingleFile())));
			getForgeInstallation().set(new File(getExtension().getFiles().getProjectPersistentCache(), "production/" + getName()));
			getClasspath().from(getProject().getConfigurations().named(Constants.Configurations.MINECRAFT_CLIENT_RUNTIME_LIBRARIES));
			getClasspath().from(getProject().getConfigurations().named(Constants.Configurations.MINECRAFT_NATIVES));
			// The installer supplies the production entrypoint, patched jars and loader libraries.
			getMainClass().convention(getForgeInstaller().map(file -> XPForgeProductionClient.readProfile(file.getAsFile().toPath()).get("mainClass").getAsString()));

			for (var configuration : new String[] { Constants.Configurations.MINECRAFT_CLIENT_RUNTIME_LIBRARIES, Constants.Configurations.MINECRAFT_NATIVES }) {
				getRuntimeLibraryCoordinates().putAll(getProject().getConfigurations().named(configuration).map(libraries -> {
					var coordinates = new HashMap<String, String>();

					for (var artifact : libraries.getResolvedConfiguration().getResolvedArtifacts()) {
						var id = artifact.getModuleVersion().getId();
						coordinates.put(artifact.getFile().getAbsolutePath(), id.getGroup() + ":" + id.getName() + ":" + (artifact.getClassifier() == null ? "" : artifact.getClassifier()));
					}

					return coordinates;
				}));
			}
		} else {
			getMainClass().convention("net.fabricmc.loader.impl.launch.knot.KnotClient");
			getClasspath().from(getProject().getConfigurations().named(Constants.Configurations.MINECRAFT_TEST_CLIENT_RUNTIME_LIBRARIES));

			getClasspath().from(getExtension().getMinecraftProvider().getMinecraftClientJar());
			getClasspath().from(detachedConfigurationProvider("net.fabricmc:fabric-loader:%s", getProjectLoaderVersion()));

			if (getExtension().getProductionNamespaceEnum().get() == MappingsNamespace.INTERMEDIARY) {
				getClasspath().from(detachedConfigurationProvider("net.fabricmc:intermediary:%s", getExtension().getMinecraftVersion()));
			}
		}

		dependsOn("downloadAssets");
	}

	@Override
	public void run() throws IOException {
		if (getForgeInstaller().isPresent()) {
			forgeClient = new XPForgeProductionClient(getForgeInstaller().get().getAsFile().toPath(),
					getForgeInstallation().get().getAsFile().toPath(), getMinecraftMetadata().get(), getRuntimeLibraryCoordinates().get());
			forgeClient.install(getExecOperations(), getJavaLauncher().get(), getOffline().get());
			forgeClient.copyMods(getMods().getFiles(), getRunDir().get().getAsFile().toPath());
		}

		if (getTracyCapture().isPresent()) {
			getTracyCapture().get().runWithTracy(super::run);
			return;
		}

		super.run();
	}

	@Override
	protected void configureCommand(ExecSpec exec) {
		if (getUseXVFB().get()) {
			if (!Platform.CURRENT.getOperatingSystem().isLinux()) {
				throw new UnsupportedOperationException("XVFB is only supported on Linux");
			}

			// GLFW 3.4 can otherwise connect to an inherited Wayland session instead of XVFB.
			exec.getEnvironment().remove("WAYLAND_DISPLAY");
			exec.environment("XDG_SESSION_TYPE", "x11");
			exec.commandLine(XVFBExistsValueSource.XVFB);
			exec.args("-a", getJavaLauncher().get().getExecutablePath());

			return;
		}

		super.configureCommand(exec);
	}

	@Override
	protected void configureJvmArgs(ExecSpec exec) {
		if (forgeClient != null) {
			exec.args(forgeClient.jvmArgs());
			exec.args(getJvmArgs().get());
		} else {
			super.configureJvmArgs(exec);
		}

		exec.args("-XX:StackShadowPages=32");
		exec.args("-Djava.library.path=" + getNativesDirectory().get().getAsFile().getAbsolutePath());

		if (getJavaLauncher().get().getMetadata().getLanguageVersion().asInt() >= 25) {
			exec.args("--sun-misc-unsafe-memory-access=allow", "--enable-native-access=ALL-UNNAMED");
		}

		if (Platform.CURRENT.getOperatingSystem().isMacOS()) {
			exec.args("-XstartOnFirstThread");
		}
	}

	@Override
	protected Stream<File> streamClasspath() {
		return forgeClient == null ? super.streamClasspath() : forgeClient.classpath(super.streamClasspath());
	}

	@Override
	protected void configureProgramArgs(ExecSpec exec) {
		if (forgeClient != null) {
			exec.args(forgeClient.gameArgs());
			exec.args("--version", forgeClient.version(), "--accessToken", "0", "--userType", "legacy");
		}

		super.configureProgramArgs(exec);

		exec.args(
				"--assetIndex", getAssetsIndex().get(),
				"--assetsDir", getAssetsDir().get().getAsFile().getAbsolutePath(),
				"--gameDir", getRunDir().get().getAsFile().getAbsolutePath()
		);

		if (getTracyCapture().isPresent()) {
			exec.args("--tracy");
		}
	}
}
