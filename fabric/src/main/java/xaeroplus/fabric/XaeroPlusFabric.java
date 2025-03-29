package xaeroplus.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import xaero.hud.minimap.BuiltInHudModules;
import xaero.hud.minimap.waypoint.set.WaypointSet;
import xaeroplus.XaeroPlus;
import xaeroplus.fabric.util.FabricWaystonesHelperInit;
import xaeroplus.fabric.util.XPShaderResourceReloadListener;
import xaeroplus.fabric.util.compat.IncompatibleMinimapWarningScreen;
import xaeroplus.fabric.util.compat.XaeroPlusMinimapCompatibilityChecker;
import xaeroplus.feature.waypoint.WaypointAPI;
import xaeroplus.module.ModuleManager;
import xaeroplus.module.impl.TickTaskExecutor;
import xaeroplus.settings.Settings;
import xaeroplus.util.AtlasWaypointImport;
import xaeroplus.util.DataFolderResolveUtil;
import xaeroplus.util.XaeroPlusGameTest;

import java.util.Optional;

import static xaeroplus.fabric.util.compat.XaeroPlusMinimapCompatibilityChecker.versionCheckResult;

public class XaeroPlusFabric implements ClientModInitializer {
	public static void initialize() {
		if (XaeroPlus.initialized.compareAndSet(false, true)) {
			XaeroPlus.LOGGER.info("Initializing XaeroPlus");
            if (!versionCheckResult.minimapCompatible()) {
				XaeroPlus.LOGGER.error("Incompatible Xaero Minimap version detected! Expected: {} Actual: {}",
									   versionCheckResult.expectedVersion().getFriendlyString(),
									   versionCheckResult.anyPresentMinimapVersion().map(Version::getFriendlyString).orElse("None!"));
				return;
			}
			XaeroPlus.XP_VERSION = FabricLoader.getInstance().getModContainer("xaeroplus")
				.map(ModContainer::getMetadata)
				.map(ModMetadata::getVersion)
				.map(Version::getFriendlyString)
				.orElse("2.x");
			FabricWaystonesHelperInit.doInit();
			XaeroPlus.initializeSettings();
			Settings.REGISTRY.getKeybindings().forEach(KeyBindingHelper::registerKeyBinding);
			if (System.getenv("XP_CI_TEST") != null || System.getProperty("XP_CI_TEST") != null)
				Minecraft.getInstance().execute(XaeroPlusGameTest::applyMixinsTest);
        }
	}

	@Override
	public void onInitializeClient() {
		initialize();
		ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
			// needed as we can either accept Xaero's Minimap or BetterPVP but can't describe this in the fabric.mod.json
			var versionCheckResult = XaeroPlusMinimapCompatibilityChecker.versionCheckResult;
			if (versionCheckResult.minimapCompatible()) return;
			var anyPresentVersion = versionCheckResult.minimapVersion().or(versionCheckResult::betterPvpVersion);
			Minecraft.getInstance().setScreen(
				new IncompatibleMinimapWarningScreen(anyPresentVersion, versionCheckResult.expectedVersion()));
		});
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			dispatcher.register(ClientCommandManager.literal("xaeroDataDir").executes(c -> {
				c.getSource().sendFeedback(DataFolderResolveUtil.getCurrentDataDirPath());
				return 1;
			}));
			dispatcher.register(ClientCommandManager.literal("xaeroWaypointDir").executes(c -> {
				c.getSource().sendFeedback(DataFolderResolveUtil.getCurrentWaypointDataDirPath());
				return 1;
			}));
			dispatcher.register(ClientCommandManager.literal("xaero2b2tAtlasImport").executes(c -> {
				c.getSource().sendFeedback(Component.literal("Atlas import started..."));
				AtlasWaypointImport.importAtlasWaypoints()
					.whenCompleteAsync((addedCount, e) -> {
						if (e != null) {
							XaeroPlus.LOGGER.error("Atlas import failed", e);
							c.getSource().sendFeedback(Component.literal("Atlas import failed! Check log for details."));
						} else {
							c.getSource().sendFeedback(Component.literal(addedCount + " waypoints imported to the \"atlas\" waypoint set!"));
							var session = BuiltInHudModules.MINIMAP.getCurrentSession();
							boolean allSetsEnabled = session.getModMain().getSettings().renderAllSets;
							boolean isAtlasSetActive = Optional.ofNullable(WaypointAPI.getCurrentWaypointSet())
								.map(WaypointSet::getName)
								.filter(n -> n.equals("atlas"))
								.isPresent();
							if (!allSetsEnabled && !isAtlasSetActive) {
								c.getSource().sendFeedback(Component.literal("To see the waypoints, enable rendering all waypoint sets or switch to the \"atlas\" set."));
							}
						}
						c.getSource().sendFeedback(Component.literal("Atlas Import Complete!"));
					}, ModuleManager.getModule(TickTaskExecutor.class));
				return 1;
			}));
		});
		ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new XPShaderResourceReloadListener());
	}
}
