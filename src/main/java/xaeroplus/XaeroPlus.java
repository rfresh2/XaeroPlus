package xaeroplus;

import com.collarmc.pounce.EventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    public static EventBus EVENT_BUS = new EventBus(Runnable::run);
    public static Logger LOGGER = LoggerFactory.getLogger("XaeroPlus");

    @Mod.Instance
    public static XaeroPlus INSTANCE;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) { }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        ModuleManager.init();
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
//        ModuleManager.init();
    }

    public static int getColor(final int r, final int g, final int b, final int a) {
        return ((a & 255) << 24) | ((r & 255) << 16) | ((g & 255) << 8) | (b & 255);
    }
}
