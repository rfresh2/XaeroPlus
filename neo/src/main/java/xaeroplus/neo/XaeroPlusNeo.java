package xaeroplus.neo;

import com.github.benmanes.caffeine.cache.RemovalCause;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import xaero.map.gui.GuiWorldMapSettings;
import xaeroplus.XaeroPlus;
import xaeroplus.feature.extensions.GuiXaeroPlusWorldMapSettings;
import xaeroplus.settings.Settings;
import xaeroplus.util.DataFolderResolveUtil;
import xaeroplus.util.XaeroPlusGameTest;

import java.util.List;

@Mod(value = "xaeroplus", dist = Dist.CLIENT)
public class XaeroPlusNeo {
    public static final IEventBus FORGE_EVENT_BUS = NeoForge.EVENT_BUS;
    public XaeroPlusNeo(IEventBus modEventBus) {
        if (FMLEnvironment.dist.isClient()) {
            modEventBus.addListener(this::onInitialize);
            modEventBus.addListener(this::onRegisterKeyMappingsEvent);
            modEventBus.addListener(this::onRegisterClientResourceReloadListeners);
            FORGE_EVENT_BUS.addListener(this::onRegisterClientCommandsEvent);
            if (EmbeddiumHelper.isEmbeddiumPresent())
                FORGE_EVENT_BUS.addListener(XaeroPlusEmbeddiumOptionsInit::onEmbeddiumOptionGUIConstructionEvent);
            RemovalCause explicit = RemovalCause.EXPLICIT; // force class load to stop forge shitting itself at runtime??
            ModLoadingContext.get().registerExtensionPoint(
                IConfigScreenFactory.class,
                XaeroPlusConfigScreenFactory::new
            );
        }
    }

    public void onInitialize(FMLClientSetupEvent event) {
        // this is called after RegisterKeyMappingsEvent for some reason
    }

    public void onRegisterKeyMappingsEvent(final RegisterKeyMappingsEvent event) {
        if (XaeroPlus.initialized.compareAndSet(false, true)) {
            XaeroPlus.initializeSettings();
            Settings.REGISTRY.getKeybindings().forEach(event::register);
            if (System.getenv("XP_CI_TEST") != null)
                Minecraft.getInstance().execute(XaeroPlusGameTest::applyMixinsTest);
        }
    }

    public void onRegisterClientCommandsEvent(final RegisterClientCommandsEvent event) {
        event.getDispatcher().register(LiteralArgumentBuilder.<CommandSourceStack>literal("xaeroDataDir").executes(c -> {
            c.getSource().sendSuccess(DataFolderResolveUtil::getCurrentDataDirPath, false);
            return 1;
        }));
    }

    public void onRegisterClientResourceReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new XaeroPlusNeoResourceReloadListener());
    }
}
