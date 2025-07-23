package xaeroplus.forge;

import com.github.benmanes.caffeine.cache.RemovalCause;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLLoader;
import xaero.map.gui.GuiWorldMapSettings;
import xaeroplus.XaeroPlus;
import xaeroplus.commands.XPClientCommandSource;
import xaeroplus.feature.extensions.GuiXaeroPlusWorldMapSettings;
import xaeroplus.settings.Settings;
import xaeroplus.util.XaeroPlusGameTest;

public class XaeroPlusForgeClient {
    public void init(final FMLJavaModLoadingContext context, BusGroup modBusGroup) {
        RegisterKeyMappingsEvent.getBus(modBusGroup).addListener(this::onRegisterKeyMappingsEvent);
        RegisterClientCommandsEvent.BUS.addListener(this::onRegisterClientCommandsEvent);
        RemovalCause explicit = RemovalCause.EXPLICIT; // force class load to stop forge shitting itself at runtime??
        context.registerExtensionPoint(
            ConfigScreenHandler.ConfigScreenFactory.class,
            () -> new ConfigScreenHandler.ConfigScreenFactory((mc, screen) -> new GuiXaeroPlusWorldMapSettings(new GuiWorldMapSettings(
                screen), screen))
        );
    }

    public void onRegisterKeyMappingsEvent(final RegisterKeyMappingsEvent event) {
        if (XaeroPlus.initialized.compareAndSet(false, true)) {
            XaeroPlus.XP_VERSION = FMLLoader.getLoadingModList().getModFileById("xaeroplus").versionString();
            XaeroPlus.initializeSettings();
            Settings.REGISTRY.getKeybindings().forEach(event::register);
            if (System.getenv("XP_CI_TEST") != null) {
                Minecraft.getInstance().execute(XaeroPlusGameTest::applyMixinsTest);
            }
        }
    }

    public void onRegisterClientCommandsEvent(final RegisterClientCommandsEvent event) {
        XaeroPlus.registerCommands((CommandDispatcher<XPClientCommandSource>) (CommandDispatcher<?>) event.getDispatcher(), event.getBuildContext());
    }
}
