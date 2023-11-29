package xaeroplus;

import com.collarmc.pounce.EventBus;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.SemanticVersion;
import net.fabricmc.loader.api.VersionParsingException;
import net.minecraft.client.option.KeyBinding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xaeroplus.module.ModuleManager;
import xaeroplus.settings.XaeroPlusSettingRegistry;
import xaeroplus.settings.XaeroPlusSettingsReflectionHax;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class XaeroPlus implements ClientModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger("XaeroPlus");
	public static final EventBus EVENT_BUS = new EventBus(Runnable::run);
	public static AtomicBoolean initialized = new AtomicBoolean(false);
	public static void initialize() {
		if (initialized.compareAndSet(false, true)) {
			LOGGER.info("Initializing XaeroPlus");
			// needed as we can either accept Xaero's Minimap or BetterPVP but can't describe this in the fabric.mod.json
			minimapCompatibleVersionCheck();
			ModuleManager.init();
			boolean a = Globals.FOLLOW; // force static instances to init
			XaeroPlusSettingRegistry.fastMapSetting.getValue(); // force static instances to init
			List<KeyBinding> keybinds = XaeroPlusSettingsReflectionHax.keybindsSupplier.get();
			keybinds.forEach(KeyBindingHelper::registerKeyBinding);
		}
	}

	@Override
	public void onInitializeClient() {
		initialize();
	}

	private static void minimapCompatibleVersionCheck() {
		try {
			SemanticVersion compatibleMinimapVersion = SemanticVersion.parse(getCompatibleMinimapVersion());
			if (!checkVersion("xaerominimap", compatibleMinimapVersion) && !checkVersion("xaerobetterpvp", compatibleMinimapVersion)) {
				throw new RuntimeException("XaeroPlus requires version: '" + compatibleMinimapVersion + "' of Xaero's Minimap or BetterPVP installed");
			}
		} catch (VersionParsingException e) {
			throw new RuntimeException(e);
		}
	}

	private static boolean checkVersion(final String modId, final SemanticVersion version) {
		try {
			return FabricLoader.getInstance().getAllMods().stream()
				.filter(modContainer -> modContainer.getMetadata().getId().equals(modId))
				.map(modContainer -> modContainer.getMetadata().getVersion())
				.anyMatch(ver -> ver.compareTo(version) == 0);
		} catch (final Exception e) {
			LOGGER.error("Failed to check version for {}", modId, e);
			return false;
		}
	}

	private static String getCompatibleMinimapVersion() {
		return FabricLoader.getInstance().getModContainer("xaeroplus").get().getMetadata().getCustomValue("minimap_version").getAsString();
	}
}
