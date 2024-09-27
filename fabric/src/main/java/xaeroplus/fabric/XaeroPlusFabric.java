package xaeroplus.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.loader.api.Version;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.PackType;
import xaeroplus.XaeroPlus;
import xaeroplus.fabric.util.FabricWaystonesHelperInit;
import xaeroplus.fabric.util.XPShaderResourceReloadListener;
import xaeroplus.fabric.util.compat.IncompatibleMinimapWarningScreen;
import xaeroplus.fabric.util.compat.XaeroPlusMinimapCompatibilityChecker;
import xaeroplus.settings.Settings;
import xaeroplus.util.DataFolderResolveUtil;
import xaeroplus.util.XaeroPlusGameTest;

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
			XaeroPlus.initializeSettings();
			Settings.REGISTRY.getKeybindings().forEach(KeyBindingHelper::registerKeyBinding);
            FabricWaystonesHelperInit.doInit();
			if (System.getenv("XP_CI_TEST") != null)
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
		});
		ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new XPShaderResourceReloadListener());
	}
}
