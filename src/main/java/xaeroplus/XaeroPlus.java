package xaeroplus;

import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.EventBus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xaero.lib.platform.Services;
import xaeroplus.event.ForgeEventHandler;
import xaeroplus.feature.keybind.KeybindListener;
import xaeroplus.module.ModuleManager;
import xaeroplus.settings.SettingHooks;
import xaeroplus.settings.Settings;
import xaeroplus.util.Globals;
import xaeroplus.util.XaeroPlusGameTest;

import java.io.File;

@Mod(
        modid = XaeroPlus.MODID,
        name = XaeroPlus.NAME,
        version = XaeroPlus.VERSION,
        clientSideOnly = true,
        dependencies = "required:mixinbooter@[9.4,);after:xaerominimap@[25.3.5];required-after:xaeroworldmap@[1.40.6];required-after:xaerolib@[1.0.44];"
)
public class XaeroPlus {
    public static final String MODID = "xaeroplus";
    public static final String NAME = "XaeroPlus";
    public static final String VERSION = "1.12.2";
    public static EventBus EVENT_BUS = MinecraftForge.EVENT_BUS;
    public static Logger LOGGER = LogManager.getLogger("XaeroPlus");
    private static final ForgeEventHandler forgeEventHandler = new ForgeEventHandler();
    private static final KeybindListener keybindListener = new KeybindListener();
    public static final File configFile = Services.PLATFORM.getConfigDir().resolve("xaeroplus.txt").toFile();

    @Mod.Instance
    public static XaeroPlus INSTANCE;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        ModuleManager.init();
        boolean follow = Globals.FOLLOW;// force static instances to init
        SettingHooks.loadXPSettings();
        Globals.onAllSettingsLoaded();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        EVENT_BUS.register(forgeEventHandler);
        LOGGER.info("XaeroPlus initialized");
        if (System.getenv("XP_CI_TEST") != null) {
            XaeroPlusGameTest.applyMixinsTest();
        }
        for (KeyBinding kb : Settings.REGISTRY.getKeybindings()) {
            ClientRegistry.registerKeyBinding(kb);
        }
        EVENT_BUS.register(keybindListener);
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {}

}
