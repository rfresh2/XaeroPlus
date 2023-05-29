package xaeroplus;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.EventBus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xaeroplus.module.ModuleManager;
import xaeroplus.util.Shared;

@Mod(
        modid = XaeroPlus.MODID,
        name = XaeroPlus.NAME,
        version = XaeroPlus.VERSION,
        dependencies = "required-after:xaerominimap@[23.4.4];required-after:xaeroworldmap@[1.30.3];"
)
public class XaeroPlus {
    public static final String MODID = "xaeroplus";
    public static final String NAME = "XaeroPlus";
    public static final String VERSION = "1.12.2";
    public static EventBus EVENT_BUS = MinecraftForge.EVENT_BUS;
    public static Logger LOGGER = LogManager.getLogger("XaeroPlus");
    private static boolean a = Shared.settingsLoadedInit; // needed to load static shared classes on init

    @Mod.Instance
    public static XaeroPlus INSTANCE;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        ModuleManager.init();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) { }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        Shared.settingsLoadedInit = true;
    }

}
