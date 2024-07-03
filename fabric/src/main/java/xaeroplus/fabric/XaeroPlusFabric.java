package xaeroplus.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.loader.api.Version;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import xaeroplus.Globals;
import xaeroplus.XaeroPlus;
import xaeroplus.fabric.util.FabricWaystonesHelperInit;
import xaeroplus.fabric.util.compat.IncompatibleMinimapWarningScreen;
import xaeroplus.fabric.util.compat.MinimapBaseVersionCheck;
import xaeroplus.module.ModuleManager;
import xaeroplus.settings.XaeroPlusSettingRegistry;
import xaeroplus.settings.XaeroPlusSettingsReflectionHax;
import xaeroplus.util.DataFolderResolveUtil;

import java.util.List;

public class XaeroPlusFabric implements ClientModInitializer {
	public static void initialize() {
		if (XaeroPlus.initialized.compareAndSet(false, true)) {
			XaeroPlus.LOGGER.info("Initializing XaeroPlus");
			ModuleManager.init();
			boolean a = Globals.FOLLOW; // force static instances to init
			XaeroPlusSettingRegistry.fastMapSetting.getValue(); // force static instances to init
			List<KeyMapping> keybinds = XaeroPlusSettingsReflectionHax.keybindsSupplier.get();
			keybinds.forEach(KeyBindingHelper::registerKeyBinding);
			FabricWaystonesHelperInit.doInit();
		}
	}

	@Override
	public void onInitializeClient() {
		initialize();
		ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
			// needed as we can either accept Xaero's Minimap or BetterPVP but can't describe this in the fabric.mod.json
			var versionCheckResult = MinimapBaseVersionCheck.versionCheck();
			if (versionCheckResult.minimapCompatible()) return;
			XaeroPlus.LOGGER.error("Incompatible Xaero Minimap version detected! Expected: {} Actual: {}",
								   versionCheckResult.expectedVersion().getFriendlyString(),
								   versionCheckResult.anyPresentMinimapVersion().map(Version::getFriendlyString).orElse("None!"));
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
	}
}
