package xaeroplus.neo;

import com.github.benmanes.caffeine.cache.RemovalCause;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import xaeroplus.XaeroPlus;
import xaeroplus.commands.XPClientCommandSource;
import xaeroplus.settings.Settings;
import xaeroplus.util.XaeroPlusGameTest;

import java.util.concurrent.ForkJoinPool;

@Mod(value = "xaeroplus", dist = Dist.CLIENT)
public class XaeroPlusNeo {
    public static final IEventBus FORGE_EVENT_BUS = NeoForge.EVENT_BUS;
    public XaeroPlusNeo(IEventBus modEventBus) {
        if (FMLEnvironment.dist.isClient()) {
            modEventBus.addListener(this::onRegisterKeyMappingsEvent);
            FORGE_EVENT_BUS.addListener(this::onRegisterClientCommandsEvent);
            RemovalCause explicit = RemovalCause.EXPLICIT; // force class load to stop forge shitting itself at runtime??
            ModLoadingContext.get().registerExtensionPoint(
                IConfigScreenFactory.class,
                XaeroPlusConfigScreenFactory::new
            );
        }
    }

    public void onRegisterKeyMappingsEvent(final RegisterKeyMappingsEvent event) {
        if (XaeroPlus.initialized.compareAndSet(false, true)) {
            XaeroPlus.XP_VERSION = FMLLoader.getLoadingModList().getModFileById("xaeroplus").versionString();
            XaeroPlus.initializeSettings();
            Settings.REGISTRY.getKeybindings().forEach(event::register);
            if (System.getenv("XP_CI_TEST") != null)
                ForkJoinPool.commonPool().execute(() -> {
                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    Minecraft.getInstance().execute(() -> {
                        XaeroPlusGameTest.applyMixinsTest();
                        System.exit(0);
                    });
                });
        }
    }

    public void onRegisterClientCommandsEvent(final RegisterClientCommandsEvent event) {
        XaeroPlus.registerCommands((CommandDispatcher<XPClientCommandSource>) (CommandDispatcher<?>) event.getDispatcher(), event.getBuildContext());
    }
}
