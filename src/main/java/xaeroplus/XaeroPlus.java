package xaeroplus;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xaeroplus.util.Shared;

public class XaeroPlus implements ModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger("XaeroPlus");
	private static boolean a = Shared.settingsLoadedInit; // needed to load static shared classes on init


	@Override
	public void onInitialize() {

	}
}
