package xaeroplus.loom.prod;/*
 * This file is part of fabric-loom, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2026 FabricMC
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

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loom.LoomGradlePlugin;
import net.fabricmc.loom.configuration.providers.minecraft.MinecraftVersionMeta;
import net.fabricmc.loom.util.Checksum;
import net.fabricmc.loom.util.Platform;
import net.fabricmc.loom.util.ZipUtils;
import org.gradle.jvm.toolchain.JavaLauncher;
import org.gradle.process.ExecOperations;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Stream;

/**
 * Installs and launches the same client distribution used by a production launcher.
 * The installer owns the version-specific processors and patched library layout; userdev
 * jars and development launch targets must never be used here.
 */
final class XPForgeProductionClient {
	private final Path installer;
	private final Path installation;
	private final JsonObject profile;
	private final MinecraftVersionMeta minecraft;
	private final Map<String, String> runtimeLibraryCoordinates;

	XPForgeProductionClient(Path installer, Path installation, String minecraftMetadata, Map<String, String> runtimeLibraryCoordinates) {
		this.installer = installer;
		this.installation = installation;
		this.profile = readProfile(installer);
		this.runtimeLibraryCoordinates = runtimeLibraryCoordinates;
		this.minecraft = LoomGradlePlugin.GSON.fromJson(minecraftMetadata, MinecraftVersionMeta.class);
	}

	static JsonObject readProfile(Path installer) {
		try {
			return JsonParser.parseString(new String(ZipUtils.unpack(installer, "version.json"), StandardCharsets.UTF_8)).getAsJsonObject();
		} catch (IOException e) {
			throw new UncheckedIOException("Failed to read installer launch profile: " + installer, e);
		}
	}

	void install(ExecOperations execOperations, JavaLauncher java, boolean offline) throws IOException {
		Files.createDirectories(installation);
		var marker = installation.resolve("loom-install.sha256");
		var hash = Checksum.of(installer).sha256().hex();

		if (Files.exists(marker) && Files.readString(marker).equals(hash) && Files.isRegularFile(clientJar())) {
			return;
		}

		Files.deleteIfExists(marker);
		Files.writeString(installation.resolve("launcher_profiles.json"), "{\"profiles\":{}}");
		var log = installation.resolve("installer.log");

		try (var output = Files.newOutputStream(log)) {
			var result = execOperations.exec(exec -> {
				exec.commandLine(java.getExecutablePath(), "-jar", installer.toAbsolutePath(), "--installClient", installation.toAbsolutePath());

				if (offline) {
					exec.args("--offline");
				}

				exec.setWorkingDir(installation);
				exec.setStandardOutput(output);
				exec.setErrorOutput(output);
				exec.setIgnoreExitValue(true);
			});

			if (result.getExitValue() != 0) {
				throw new IOException("Client installer failed with exit code " + result.getExitValue() + "; see " + log);
			}
		}

		// Launchers name the inherited game jar after the launched profile. Older Forge
		// relies on this name in its ignoreList to keep vanilla classes out of the module layer.
		var vanillaClient = installation.resolve("versions").resolve(minecraft.id()).resolve(minecraft.id() + ".jar");
		Files.copy(vanillaClient, clientJar(), StandardCopyOption.REPLACE_EXISTING);
		Files.writeString(marker, hash);
	}

	private Path clientJar() {
		return installation.resolve("versions").resolve(version()).resolve(version() + ".jar");
	}

	String mainClass() {
		return profile.get("mainClass").getAsString();
	}

	String version() {
		return profile.get("id").getAsString();
	}

	List<String> jvmArgs() {
		return arguments("jvm");
	}

	List<String> gameArgs() {
		return arguments("game");
	}

	private List<String> arguments(String type) {
		var result = new ArrayList<String>();

		for (var argument : profile.getAsJsonObject("arguments").getAsJsonArray(type)) {
			if (!argument.isJsonPrimitive()) {
				throw new IllegalArgumentException("Unsupported installer launch argument: " + argument);
			}

			var value = argument.getAsString()
					.replace("${library_directory}", installation.resolve("libraries").toAbsolutePath().toString())
					.replace("${classpath_separator}", File.pathSeparator)
					.replace("${version_name}", version());

			if (value.contains("${")) {
				throw new IllegalArgumentException("Unresolved installer launch argument: " + value);
			}

			result.add(value);
		}

		return result;
	}

	Stream<File> classpath(Stream<File> minecraftLibraries) {
		var libraries = new LinkedHashMap<String, File>();

		// Match Maven coordinates so launcher overrides replace old versions,
		// while retaining Loom's platform-specific native library selection.
		minecraftLibraries.forEach(file -> libraries.put(runtimeLibraryCoordinates.getOrDefault(file.getAbsolutePath(), file.getAbsolutePath()), file));

		for (var element : profile.getAsJsonArray("libraries")) {
			var library = LoomGradlePlugin.GSON.fromJson(element, MinecraftVersionMeta.Library.class);

			if (library.isValidForOS(Platform.CURRENT) && library.artifact() != null) {
				var file = installation.resolve("libraries").resolve(library.artifact().path()).toFile();

				if (!file.isFile()) {
					throw new IllegalStateException("Missing installed production library: " + file);
				}

				var coordinates = library.name().split(":");
				var key = coordinates[0] + ":" + coordinates[1] + ":" + (coordinates.length > 3 ? coordinates[3] : "");
				libraries.put(key, file);
			}
		}

		var client = clientJar().toFile();
		libraries.put("minecraft-client", client);
		return libraries.values().stream();
	}

	void copyMods(Set<File> mods, Path runDirectory) throws IOException {
		var directory = runDirectory.resolve("mods");
		Files.createDirectories(directory);
		var manifest = installation.resolve("loom-mods.json");
		var previous = Files.exists(manifest) ? JsonParser.parseString(Files.readString(manifest)).getAsJsonObject() : new JsonObject();
		var next = new JsonObject();

		for (var mod : mods) {
			var name = mod.getName();
			var target = directory.resolve(name);
			var hash = Checksum.of(mod.toPath()).sha256().hex();

			if (next.has(name) && !next.get(name).getAsString().equals(hash)) {
				throw new IOException("Production mods have the same filename: " + name);
			}

			if (Files.exists(target)) {
				var currentHash = Checksum.of(target).sha256().hex();

				if (!currentHash.equals(hash) && (!previous.has(name) || !previous.get(name).getAsString().equals(currentHash))) {
					throw new IOException("Refusing to replace an unmanaged production mod: " + target);
				}
			}

			Files.copy(mod.toPath(), target, StandardCopyOption.REPLACE_EXISTING);
			next.addProperty(name, hash);
		}

		for (var entry : previous.entrySet()) {
			var target = directory.resolve(entry.getKey());

			if (!next.has(entry.getKey()) && Files.isRegularFile(target) && Checksum.of(target).sha256().hex().equals(entry.getValue().getAsString())) {
				Files.delete(target);
			}
		}

		Files.writeString(manifest, LoomGradlePlugin.GSON.toJson(next));
	}
}
