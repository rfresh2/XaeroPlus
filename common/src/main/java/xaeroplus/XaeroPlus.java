package xaeroplus;

import com.mojang.brigadier.CommandDispatcher;
import net.lenni0451.lambdaevents.LambdaManager;
import net.lenni0451.lambdaevents.generator.LambdaMetaFactoryGenerator;
import net.minecraft.DetectedVersion;
import net.minecraft.commands.CommandBuildContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xaero.map.platform.Services;
import xaeroplus.commands.XPClientCommandSource;
import xaeroplus.commands.XPCommandManager;
import xaeroplus.event.MinimapInitCompletedEvent;
import xaeroplus.feature.keybind.KeybindListener;
import xaeroplus.module.ModuleManager;
import xaeroplus.module.impl.Drawing;
import xaeroplus.settings.Settings;
import xaeroplus.settings.XaeroPlusSetting;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

import static xaeroplus.settings.SettingHooks.loadXPSettings;

public class XaeroPlus {
	public static final Logger LOGGER = LoggerFactory.getLogger("XaeroPlus");
	public static final LambdaManager EVENT_BUS = LambdaManager.threadSafe(new LambdaMetaFactoryGenerator());
	public static final AtomicBoolean initialized = new AtomicBoolean(false);
	public static final File configFile = Services.PLATFORM.getConfigDir().resolve("xaeroplus.txt").toFile();
	public static String XP_VERSION = "2";
	public static final String MC_VERSION = DetectedVersion.BUILT_IN.getName();
	public static final KeybindListener KEYBIND_LISTENER = new KeybindListener();

	public static void initializeSettings() {
		loadXPSettings();
		Settings.REGISTRY.getAllSettings().forEach(XaeroPlusSetting::init);
		Globals.initStickySettings();
		ModuleManager.getModule(Drawing.class).enable();
		XaeroPlus.EVENT_BUS.registerConsumer((e) -> {
			if (Globals.minimapSettingsInitialized) return;
			Globals.minimapSettingsInitialized = true;
		}, MinimapInitCompletedEvent.class);
		XaeroPlus.EVENT_BUS.register(KEYBIND_LISTENER);
	}

	public static void registerCommands(CommandDispatcher<XPClientCommandSource> dispatcher, CommandBuildContext context) {
		XPCommandManager.registerCommands(dispatcher, context);
	}
}
