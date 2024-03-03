package xaeroplus.forge;

import com.github.benmanes.caffeine.cache.RemovalCause;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import xaeroplus.Globals;
import xaeroplus.XaeroPlus;
import xaeroplus.module.ModuleManager;
import xaeroplus.settings.XaeroPlusSettingRegistry;
import xaeroplus.settings.XaeroPlusSettingsReflectionHax;

import java.util.List;

import static net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext.get;

@Mod(value = "xaeroplus")
public class XaeroPlusForge {
    public static final IEventBus FORGE_EVENT_BUS = MinecraftForge.EVENT_BUS;

    public XaeroPlusForge() {
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
        if (XaeroPlus.initialized.compareAndSet(false, true)) {
            ModuleManager.init();
            boolean a = Globals.FOLLOW; // force static instances to init
            XaeroPlusSettingRegistry.fastMapSetting.getValue(); // force static instances to init
            List<KeyMapping> keybinds = XaeroPlusSettingsReflectionHax.keybindsSupplier.get();
            keybinds.forEach(event::register);
        }
    }
}
