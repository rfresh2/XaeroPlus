package xaeroplus;

import net.lenni0451.lambdaevents.LambdaManager;
import net.lenni0451.lambdaevents.generator.LambdaMetaFactoryGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xaero.map.platform.Services;
import xaeroplus.settings.Settings;
import xaeroplus.settings.XaeroPlusSetting;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

import static xaeroplus.settings.SettingHooks.loadXPSettings;

public class XaeroPlus {
	public static final Logger LOGGER = LoggerFactory.getLogger("XaeroPlus");
	public static final LambdaManager EVENT_BUS = LambdaManager.basic(new LambdaMetaFactoryGenerator());
	public static final AtomicBoolean initialized = new AtomicBoolean(false);
	public static final File configFile = Services.PLATFORM.getConfigDir().resolve("xaeroplus.txt").toFile();

	public static void initializeSettings() {
		loadXPSettings();
		Settings.REGISTRY.getAllSettings().forEach(XaeroPlusSetting::init);
		Globals.initStickySettings();
	}
}
