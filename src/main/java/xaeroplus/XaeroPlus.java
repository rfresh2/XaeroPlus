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
import xaeroplus.settings.XaeroPlusSettingRegistry;
import xaeroplus.settings.XaeroPlusSettingRegistry.DataFolderResolutionMode;

import java.util.UUID;

@Mod(modid = XaeroPlus.MODID, name = XaeroPlus.NAME, version = XaeroPlus.VERSION, dependencies = "required-after:xaerominimap;required-after:xaeroworldmap;")
public class XaeroPlus {
    public static final String MODID = "xaeroplus";
    public static final String NAME = "XaeroPlus";
    public static final String VERSION = "1.12.2";
    // Map gui follow mode
    public static boolean FOLLOW = false;
    // cache and only update this on new world loads
    public static boolean nullOverworldDimensionFolder = XaeroPlusSettingRegistry.nullOverworldDimensionFolder.getValue();
    public static DataFolderResolutionMode dataFolderResolutionMode = XaeroPlusSettingRegistry.dataFolderResolutionMode.getValue();
    public static int minimapScalingFactor = (int) XaeroPlusSettingRegistry.minimapScaling.getValue();
    public static boolean settingsLoadedInit = false;
    public static boolean shouldResetFBO = false;
    public static String LOCK_ID = UUID.randomUUID().toString();
    public static int customDimensionId = 0;
    public static EventBus EVENT_BUS = MinecraftForge.EVENT_BUS;
    public static Logger LOGGER = LogManager.getLogger("XaeroPlus");

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
        settingsLoadedInit = true;
    }

    public static void onSettingLoad() {
        if (!settingsLoadedInit) { // handle settings where we want them to take effect only on first load
            nullOverworldDimensionFolder = XaeroPlusSettingRegistry.nullOverworldDimensionFolder.getValue();
            dataFolderResolutionMode = XaeroPlusSettingRegistry.dataFolderResolutionMode.getValue();
            minimapScalingFactor = (int) XaeroPlusSettingRegistry.minimapScaling.getValue();
        }
    }
}
