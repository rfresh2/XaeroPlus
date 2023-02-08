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

import java.util.UUID;

@Mod(modid = XaeroPlus.MODID, name = XaeroPlus.NAME, version = XaeroPlus.VERSION)
public class XaeroPlus {
    public static final String MODID = "xaeroplus";
    public static final String NAME = "XaeroPlus";
    public static final String VERSION = "1.12.2";
    // Map gui follow mode
    public static boolean FOLLOW = false;
    public static String LOCK_ID = UUID.randomUUID().toString();
    public static EventBus EVENT_BUS = MinecraftForge.EVENT_BUS;
    public static Logger LOGGER = LogManager.getLogger("XaeroPlus");

    @Mod.Instance
    public static XaeroPlus INSTANCE;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) { }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        ModuleManager.init();
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) { }

    public static int getColor(final int r, final int g, final int b, final int a) {
        return ((a & 255) << 24) | ((r & 255) << 16) | ((g & 255) << 8) | (b & 255);
    }
}
