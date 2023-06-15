package xaeroplus;

import com.collarmc.pounce.EventBus;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xaeroplus.module.ModuleManager;
import xaeroplus.util.Shared;

public class XaeroPlus implements ModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger("XaeroPlus");
	public static final EventBus EVENT_BUS = new EventBus(Runnable::run);
	private static boolean a = Shared.settingsLoadedInit; // needed to load static shared classes on init
	@Override
	public void onInitialize() {
		ModuleManager.init();
	}
}
