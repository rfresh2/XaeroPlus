package xaeroplus;

import com.collarmc.pounce.EventBus;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xaeroplus.module.ModuleManager;
import xaeroplus.settings.XaeroPlusSettingRegistry;
import xaeroplus.util.Shared;

import static net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext.get;

@Mod("xaeroplus")
public class XaeroPlus {
	public static final Logger LOGGER = LoggerFactory.getLogger("XaeroPlus");
	public static final EventBus EVENT_BUS = new EventBus(Runnable::run);
	public static final IEventBus FORGE_EVENT_BUS = MinecraftForge.EVENT_BUS;

	public XaeroPlus() {
		IEventBus modEventBus = get().getModEventBus();
		modEventBus.addListener(this::onInitialize);
		FORGE_EVENT_BUS.register(modEventBus);
	}

	public void onInitialize(FMLClientSetupEvent event) {
		ModuleManager.init();
		boolean a = Shared.FOLLOW; // force static instances to init
		XaeroPlusSettingRegistry.fastMapSetting.getValue(); // force static instances to init
	}
}
