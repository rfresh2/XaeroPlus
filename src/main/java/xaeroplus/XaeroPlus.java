package xaeroplus;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.lenni0451.lambdaevents.LambdaManager;
import net.lenni0451.lambdaevents.generator.LambdaMetaFactoryGenerator;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xaeroplus.module.ModuleManager;
import xaeroplus.settings.XaeroPlusSettingRegistry;
import xaeroplus.settings.XaeroPlusSettingsReflectionHax;
import xaeroplus.util.compat.IncompatibleMinimapWarningScreen;
import xaeroplus.util.compat.MinimapBaseVersionCheck;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class XaeroPlus implements ClientModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger("XaeroPlus");
	public static final LambdaManager EVENT_BUS = LambdaManager.basic(new LambdaMetaFactoryGenerator());
	public static AtomicBoolean initialized = new AtomicBoolean(false);
	public static void initialize() {
		if (initialized.compareAndSet(false, true)) {
			LOGGER.info("Initializing XaeroPlus");
			ModuleManager.init();
			boolean a = Globals.FOLLOW; // force static instances to init
			XaeroPlusSettingRegistry.fastMapSetting.getValue(); // force static instances to init
			List<KeyMapping> keybinds = XaeroPlusSettingsReflectionHax.keybindsSupplier.get();
			keybinds.forEach(KeyBindingHelper::registerKeyBinding);
		}
	}

	@Override
	public void onInitializeClient() {
		initialize();
		ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
			// needed as we can either accept Xaero's Minimap or BetterPVP but can't describe this in the fabric.mod.json
			var versionCheckResult = MinimapBaseVersionCheck.versionCheck();
			if (versionCheckResult.minimapCompatible()) return;
			var anyPresentVersion = versionCheckResult.minimapVersion().or(versionCheckResult::betterPvpVersion);
			Minecraft.getInstance().setScreen(
				new IncompatibleMinimapWarningScreen(anyPresentVersion, versionCheckResult.expectedVersion()));
		});
	}
}
