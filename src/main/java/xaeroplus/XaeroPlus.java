package xaeroplus;

import com.github.benmanes.caffeine.cache.RemovalCause;
import net.lenni0451.lambdaevents.LambdaManager;
import net.lenni0451.lambdaevents.generator.LambdaMetaFactoryGenerator;
import net.minecraft.client.KeyMapping;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xaeroplus.module.ModuleManager;
import xaeroplus.settings.XaeroPlusSettingRegistry;
import xaeroplus.settings.XaeroPlusSettingsReflectionHax;

import java.util.List;


@Mod(value = "xaeroplus")
public class XaeroPlus {
	public static final Logger LOGGER = LoggerFactory.getLogger("XaeroPlus");
    public static final LambdaManager EVENT_BUS = LambdaManager.basic(new LambdaMetaFactoryGenerator());
	public static final IEventBus FORGE_EVENT_BUS = NeoForge.EVENT_BUS;

	public XaeroPlus(IEventBus modEventBus) {
		if (FMLEnvironment.dist.isClient()) {
			modEventBus.addListener(this::onInitialize);
			modEventBus.addListener(this::onRegisterKeyMappingsEvent);
//			FORGE_EVENT_BUS.register(modEventBus);
			RemovalCause explicit = RemovalCause.EXPLICIT; // force class load to stop forge shitting itself at runtime??
		}
	}

	public void onInitialize(FMLClientSetupEvent event) {
		// this is called after RegisterKeyMappingsEvent for some reason
	}

	public void onRegisterKeyMappingsEvent(final RegisterKeyMappingsEvent event) {
		ModuleManager.init();
		boolean a = Globals.FOLLOW; // force static instances to init
		XaeroPlusSettingRegistry.fastMapSetting.getValue(); // force static instances to init
		List<KeyMapping> keybinds = XaeroPlusSettingsReflectionHax.keybindsSupplier.get();
		keybinds.forEach(event::register);
	}
}
