package xaeroplus;

import com.collarmc.pounce.EventBus;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xaeroplus.module.ModuleManager;
import xaeroplus.settings.XaeroPlusSettingRegistry;
import xaeroplus.settings.XaeroPlusSettingsReflectionHax;
import xaeroplus.util.Shared;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class XaeroPlus implements ClientModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger("XaeroPlus");
	public static final EventBus EVENT_BUS = new EventBus(Runnable::run);
	public static AtomicBoolean initialized = new AtomicBoolean(false);

	public static void initialize() {
		if (initialized.compareAndSet(false, true)) {
			LOGGER.info("Initializing XaeroPlus");
			ModuleManager.init();
			boolean a = Shared.FOLLOW; // force static instances to init
			XaeroPlusSettingRegistry.fastMapSetting.getValue(); // force static instances to init
			List<KeyBinding> keybinds = XaeroPlusSettingsReflectionHax.getKeybinds();
			keybinds.forEach(KeyBindingHelper::registerKeyBinding);
		}
	}

	@Override
	public void onInitializeClient() {
		initialize();
	}
}
