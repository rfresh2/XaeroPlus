package xaeroplus.forge;

import com.github.benmanes.caffeine.cache.RemovalCause;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.loading.FMLLoader;
import xaero.lib.client.gui.config.context.BuiltInEditConfigScreenContexts;
import xaero.map.gui.GuiWorldMapSettings;
import xaeroplus.XaeroPlus;
import xaeroplus.commands.XPClientCommandSource;
import xaeroplus.feature.extensions.GuiXaeroPlusWorldMapSettings;
import xaeroplus.settings.Settings;
import xaeroplus.util.XaeroPlusGameTest;

public class XaeroPlusForgeClient {
    public void init(final IEventBus modEventBus, final IEventBus forgeEventBus) {
        modEventBus.addListener(this::onRegisterKeyMappingsEvent);
        modEventBus.addListener(this::onRegisterClientResourceReloadListeners);
        forgeEventBus.addListener(this::onRegisterClientCommandsEvent);
        forgeEventBus.register(modEventBus);
        if (EmbeddiumHelper.isEmbeddiumPresent())
            forgeEventBus.addListener(XaeroPlusEmbeddiumOptionsInit::onEmbeddiumOptionGUIConstructionEvent);
        RemovalCause explicit = RemovalCause.EXPLICIT; // force class load to stop forge shitting itself at runtime??
        ModLoadingContext.get().registerExtensionPoint(
            ConfigScreenHandler.ConfigScreenFactory.class,
            () -> new ConfigScreenHandler.ConfigScreenFactory((mc, screen) -> new GuiXaeroPlusWorldMapSettings(new GuiWorldMapSettings(BuiltInEditConfigScreenContexts.CLIENT), screen))
        );
    }

    public void onRegisterKeyMappingsEvent(final RegisterKeyMappingsEvent event) {
        if (XaeroPlus.initialized.compareAndSet(false, true)) {
            XaeroPlus.XP_VERSION = FMLLoader.getLoadingModList().getModFileById("xaeroplus").versionString();
            XaeroPlus.initializeSettings();
            Settings.REGISTRY.getKeybindings().forEach(event::register);
            if (System.getenv("XP_CI_TEST") != null)
                Minecraft.getInstance().execute(XaeroPlusGameTest::applyMixinsTest);
        }
    }

    public void onRegisterClientCommandsEvent(final RegisterClientCommandsEvent event) {
        XaeroPlus.registerCommands((CommandDispatcher<XPClientCommandSource>) (CommandDispatcher<?>) event.getDispatcher(), event.getBuildContext());
    }

    public void onRegisterClientResourceReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new XaeroPlusForgeResourceReloadListener());
    }
}
