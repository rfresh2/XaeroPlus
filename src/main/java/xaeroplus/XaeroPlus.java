package xaeroplus;

import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.EventBus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xaero.map.MapProcessor;
import xaero.map.WorldMap;
import xaero.map.WorldMapSession;
import xaero.map.mods.SupportMods;
import xaero.map.world.MapDimension;
import xaeroplus.module.ModuleManager;
import xaeroplus.settings.XaeroPlusSettingRegistry;
import xaeroplus.settings.XaeroPlusSettingRegistry.DataFolderResolutionMode;
import xaeroplus.util.CustomDimensionMapSaveLoad;

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
    public static String waypointsSearchFilter = "";
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

    public static void switchToDimension(final int newDimId) {
        MapProcessor mapProcessor = WorldMapSession.getCurrentSession().getMapProcessor();
        mapProcessor.getMapSaveLoad().setRegionDetectionComplete(false);
        MapDimension dimension = mapProcessor.getMapWorld().getDimension(newDimId);
        if (dimension == null) {
            dimension = mapProcessor.getMapWorld().createDimensionUnsynced(mapProcessor.mainWorld, newDimId);
        }
        if (!dimension.hasDoneRegionDetection()) {
            ((CustomDimensionMapSaveLoad) mapProcessor.getMapSaveLoad()).detectRegionsInDimension(10, newDimId);
        }
        mapProcessor.getMapSaveLoad().setRegionDetectionComplete(true);
        // kind of shit but its ok. need to reset setting when GuiMap closes
        int worldDim = Minecraft.getMinecraft().world.provider.getDimension();
        if (worldDim != newDimId) {
            WorldMap.settings.minimapRadar = false;
        } else {
            WorldMap.settings.minimapRadar = true;
        }
        customDimensionId = newDimId;
        SupportMods.xaeroMinimap.requestWaypointsRefresh();
    }
}
