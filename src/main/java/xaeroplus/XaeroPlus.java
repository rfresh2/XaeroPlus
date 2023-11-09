package xaeroplus;

import com.collarmc.pounce.EventBus;
import com.github.benmanes.caffeine.cache.RemovalCause;
import net.minecraft.client.option.KeyBinding;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xaeroplus.module.ModuleManager;
import xaeroplus.settings.XaeroPlusSettingRegistry;
import xaeroplus.settings.XaeroPlusSettingsReflectionHax;

import java.util.List;

import static net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext.get;

@Mod(value = "xaeroplus")
public class XaeroPlus {
	public static final Logger LOGGER = LoggerFactory.getLogger("XaeroPlus");
	public static final EventBus EVENT_BUS = new EventBus(Runnable::run);
	public static final IEventBus FORGE_EVENT_BUS = MinecraftForge.EVENT_BUS;

	public XaeroPlus() {
		IEventBus modEventBus = get().getModEventBus();
		DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> {
			return () -> {
				modEventBus.addListener(this::onInitialize);
				modEventBus.addListener(this::onRegisterKeyMappingsEvent);
				FORGE_EVENT_BUS.register(modEventBus);
				RemovalCause explicit = RemovalCause.EXPLICIT; // force class load to stop forge shitting itself at runtime??
			};
		});
	}

	public void onInitialize(FMLClientSetupEvent event) {
		// this is called after RegisterKeyMappingsEvent for some reason
	}

	public void onRegisterKeyMappingsEvent(final RegisterKeyMappingsEvent event) {
		ModuleManager.init();
		boolean a = Globals.FOLLOW; // force static instances to init
		XaeroPlusSettingRegistry.fastMapSetting.getValue(); // force static instances to init
		List<KeyBinding> keybinds = XaeroPlusSettingsReflectionHax.getKeybinds();
		keybinds.forEach(event::register);
	}
}
