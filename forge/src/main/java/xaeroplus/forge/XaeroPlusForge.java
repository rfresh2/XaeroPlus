package xaeroplus.forge;

import com.github.benmanes.caffeine.cache.RemovalCause;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.client.KeyMapping;
import net.minecraft.commands.CommandSourceStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import xaero.map.gui.GuiWorldMapSettings;
import xaeroplus.Globals;
import xaeroplus.XaeroPlus;
import xaeroplus.feature.extensions.GuiXaeroPlusWorldMapSettings;
import xaeroplus.module.ModuleManager;
import xaeroplus.settings.XaeroPlusSettingRegistry;
import xaeroplus.settings.XaeroPlusSettingsReflectionHax;
import xaeroplus.util.DataFolderResolveUtil;

import java.util.List;

import static net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext.get;

@Mod(value = "xaeroplus")
public class XaeroPlusForge {
    public static final IEventBus FORGE_EVENT_BUS = MinecraftForge.EVENT_BUS;

    public XaeroPlusForge() {
        IEventBus modEventBus = get().getModEventBus();
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> {
            return () -> {
                modEventBus.addListener(this::onInitialize);
                modEventBus.addListener(this::onRegisterKeyMappingsEvent);
                FORGE_EVENT_BUS.addListener(this::onRegisterClientCommandsEvent);
                FORGE_EVENT_BUS.register(modEventBus);
                RemovalCause explicit = RemovalCause.EXPLICIT; // force class load to stop forge shitting itself at runtime??
                ModLoadingContext.get().registerExtensionPoint(
                    ConfigScreenHandler.ConfigScreenFactory.class,
                    () -> new ConfigScreenHandler.ConfigScreenFactory((mc, screen) -> new GuiXaeroPlusWorldMapSettings(new GuiWorldMapSettings(screen), screen))
                );
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

    public void onRegisterClientCommandsEvent(final RegisterClientCommandsEvent event) {
        event.getDispatcher().register(LiteralArgumentBuilder.<CommandSourceStack>literal("xaeroDataDir").executes(c -> {
            c.getSource().sendSuccess(DataFolderResolveUtil::getCurrentDataDirPath, false);
            return 1;
        }));
    }
}
