package xaeroplus.settings;

import xaeroplus.module.ModuleManager;
import xaeroplus.module.impl.NewChunks;
import xaeroplus.util.ColorHelper;
import xaeroplus.util.Shared;
import xaeroplus.util.WDLHelper;

import static xaeroplus.settings.XaeroPlusSettingsReflectionHax.SettingLocation;
import static xaeroplus.settings.XaeroPlusSettingsReflectionHax.markChunksDirtyInWriteDistance;

/**
 * Registry for XaeroPlus-specific settings
 */
public final class XaeroPlusSettingRegistry {

    /**
     * The order settings are defined here determines the order in the settings GUI's.
     */

    public static final XaeroPlusBooleanSetting fastMapSetting = XaeroPlusBooleanSetting.create("Fast Mapping",
                    "Increases mapping speed, might hurt FPS. Adjust rate limit and delay to regain FPS.",
            true, SettingLocation.WORLD_MAP_MAIN);
    public static final XaeroPlusFloatSetting mapWriterDelaySetting = XaeroPlusFloatSetting.create("Fast Mapping Delay",
            10, 1000, 10,
                    "Fast Mapping must be enabled. \\n " +
                    "This is roughly the delay in milliseconds between minimap update operations, both render and actual file writes.",
            50, SettingLocation.WORLD_MAP_MAIN);
    public static final XaeroPlusFloatSetting fastMapMaxTilesPerCycle = XaeroPlusFloatSetting.create("Fast Mapping Rate Limit",
            10, 120, 10,
                    "Fast Mapping must be enabled. \\n " +
                    "Limits how many chunks can be written in a single cycle. Lower values improve FPS at high render distances.",
            50, SettingLocation.WORLD_MAP_MAIN);
    public static final XaeroPlusBooleanSetting transparentObsidianRoofSetting = XaeroPlusBooleanSetting.create("Transparent Obsidian Roof",
                    "Makes obsidian placed at build height transparent. Does not update tiles already mapped - you need to remap them.",
            (v) -> markChunksDirtyInWriteDistance(),
            true, SettingLocation.WORLD_MAP_MAIN);
    public static final XaeroPlusFloatSetting transparentObsidianRoofDarkeningSetting = XaeroPlusFloatSetting.create("Roof Obsidian Opacity",
            0, 255, 5,
                    "Sets the opacity of the transparent obsidian roof tiles.",
            (v) -> markChunksDirtyInWriteDistance(),
            150, SettingLocation.WORLD_MAP_MAIN);
    public static final XaeroPlusFloatSetting worldMapMinZoomSetting = XaeroPlusFloatSetting.create("Min WorldMap Zoom",
            0, 0.625f, 0.01f,
                    "Minimum WorldMap Zoom Setting. This is 10x what you see on the WorldMap.",
            0.1f, SettingLocation.WORLD_MAP_MAIN);
    public static final XaeroPlusBooleanSetting skipWorldRenderSetting = XaeroPlusBooleanSetting.create("Skip Background Render",
                    "Skip MC world render while in a Xaero GUI. Having this on can cause issues with travel mods while you're in a Xaero GUI like the WorldMap.",
            false, SettingLocation.WORLD_MAP_MAIN);
    public static final XaeroPlusBooleanSetting newChunksEnabledSetting = XaeroPlusBooleanSetting.create("NewChunks Highlighting",
                    "Highlights NewChunks on the Minimap and WorldMap.",
            (b) -> ModuleManager.getModule(NewChunks.class).setEnabled(b),
            true, SettingLocation.WORLD_MAP_MAIN);
    public static final XaeroPlusBooleanSetting newChunksSaveLoadToDisk = XaeroPlusBooleanSetting.create("Save/Load NewChunks to Disk",
            "Saves and loads NewChunk data to disk for each world and dimension. Requires NewChunk Highlighting to be enabled.",
            (b) -> ModuleManager.getModule(NewChunks.class).setNewChunksCache(b),
            true, SettingLocation.WORLD_MAP_MAIN);
    public static final XaeroPlusFloatSetting newChunksAlphaSetting = XaeroPlusFloatSetting.create("New Chunks Opacity",
            10f, 255f, 10f,
                    "Changes the color opacity of NewChunks.",
            (b) -> ModuleManager.getModule(NewChunks.class).setAlpha(b),
            100, SettingLocation.WORLD_MAP_MAIN);
    public static final XaeroPlusEnumSetting<ColorHelper.HighlightColor> newChunksColorSetting = XaeroPlusEnumSetting.create("New Chunks Color",
            "Changes the color of NewChunks.",
            (b) -> ModuleManager.getModule(NewChunks.class).setRgbColor(b.getColor()),
            ColorHelper.HighlightColor.values(),
            ColorHelper.HighlightColor.RED, SettingLocation.WORLD_MAP_MAIN);
    public static final XaeroPlusBooleanSetting wdlEnabledSetting = XaeroPlusBooleanSetting.create("WDL Highlight",
                    "Highlights chunks WDL mod has downloaded on the Minimap and WorldMap.",
            true, SettingLocation.WORLD_MAP_MAIN);
    public static final XaeroPlusFloatSetting wdlAlphaSetting = XaeroPlusFloatSetting.create("WDL Opacity",
            10f, 255f, 10f,
                    "Changes the color opacity of WDL chunks.",
            WDLHelper::setAlpha,
            100, SettingLocation.WORLD_MAP_MAIN);
    public static final XaeroPlusEnumSetting<ColorHelper.HighlightColor> wdlColorSetting = XaeroPlusEnumSetting.create("WDL Color",
            "Changes the color of WDL chunks.",
            (b) -> WDLHelper.setRgbColor(b.getColor()),
            ColorHelper.HighlightColor.values(),
            ColorHelper.HighlightColor.GREEN, SettingLocation.WORLD_MAP_MAIN);
    public static final XaeroPlusBooleanSetting owAutoWaypointDimension = XaeroPlusBooleanSetting.create("Prefer Overworld Waypoints",
                    "Prefer creating and viewing Overworld waypoints when in the nether.",
            true, SettingLocation.WORLD_MAP_MAIN);
    public static final XaeroPlusBooleanSetting showWaypointDistances = XaeroPlusBooleanSetting.create("Show Waypoint Distances",
            "Display the distance to every waypoint in the full waypoint menu.",
            true, SettingLocation.WORLD_MAP_MAIN);
    public static final XaeroPlusBooleanSetting showRenderDistanceSetting = XaeroPlusBooleanSetting.create("Show Render Distance",
            "Show server side render distance (actually just another setting)",
            false, SettingLocation.MINIMAP_OVERLAYS);
    public static final XaeroPlusBooleanSetting showRenderDistanceWorldMapSetting = XaeroPlusBooleanSetting.create("Show Render Distance WorldMap",
            "Show server side render distance on the WorldMap",
            false, SettingLocation.MINIMAP_OVERLAYS);
    public static final XaeroPlusFloatSetting assumedServerRenderDistanceSetting = XaeroPlusFloatSetting.create("Server Render Distance",
            1f, 32f, 1f,
            "view-distance of the server",
            4f, SettingLocation.MINIMAP_OVERLAYS); // 2b2t
    public static final XaeroPlusBooleanSetting nullOverworldDimensionFolder = XaeroPlusBooleanSetting.create("null OW Dim Dir",
            "Sets whether the overworld WorldMap directory is in DIM0 or null (default)"
                    + " \\n MC must be restarted for changes to take effect.",
            true, SettingLocation.WORLD_MAP_MAIN);
    public static final XaeroPlusEnumSetting<DataFolderResolutionMode> dataFolderResolutionMode = XaeroPlusEnumSetting.create("Data Dir Mode",
            "Sets how the WorldMap and Waypoints data folders are resolved."
                    + " \\n MC must be restarted for changes to take effect."
                    + " \\n IP = Server IP (Xaero Default). Example: .minecraft/XaeroWorldMap/Multiplayer_connect.2b2t.org"
                    + " \\n SERVER_NAME = MC Server Entry Name. Example: .minecraft/XaeroWorldMap/Multiplayer_2b2t"
                    + " \\n BASE_DOMAIN = Base Server Domain Name. Example: .minecraft/XaeroWorldMap/Multiplayer_2b2t.org",
            DataFolderResolutionMode.values(), DataFolderResolutionMode.IP, SettingLocation.WORLD_MAP_MAIN);
    public enum DataFolderResolutionMode {
        IP, SERVER_NAME, BASE_DOMAIN;
    }
    public static final XaeroPlusBooleanSetting transparentMinimapBackground = XaeroPlusBooleanSetting.create("Transparent Background",
            "Makes the minimap background transparent instead of black.",
            false, SettingLocation.MINIMAP);
    public static final XaeroPlusFloatSetting minimapScaling = XaeroPlusFloatSetting.create("Minimap Scaling Factor",
            // todo: increase max but we need to start generating mipmaps and change the framebuffer filter for anti aliasing to work better
            1f, 2f, 1f,
            "Increases the base minimap scale beyond the default size.",
            (b) -> Shared.shouldResetFBO = true,
            2f, SettingLocation.MINIMAP);
    public static final XaeroPlusBooleanSetting switchToNetherSetting = XaeroPlusBooleanSetting.create("Switch to Nether",
            "Switches to the nether map.",
            (b) -> Shared.switchToDimension(-1),
            false, SettingLocation.KEYBINDS);
    public static final XaeroPlusBooleanSetting switchToOverworldSetting = XaeroPlusBooleanSetting.create("Switch to Overworld",
            "Switches to the overworld map.",
            (b) -> Shared.switchToDimension(0),
            false, SettingLocation.KEYBINDS);
    public static final XaeroPlusBooleanSetting switchToEndSetting = XaeroPlusBooleanSetting.create("Switch to End",
            "Switches to the end map.",
            (b) -> Shared.switchToDimension(1),
            false, SettingLocation.KEYBINDS);
    public static final XaeroPlusBooleanSetting netherCaveFix = XaeroPlusBooleanSetting.create("Nether Cave Fix",
            "Forces full cave maps to be written and rendered when cave mode is \"off\" in the nether."
                    + " \\n Without this, you have to manually move region files pre WorldMap 1.30.0 to the correct cave folder",
            true, SettingLocation.WORLD_MAP_MAIN);
    public static final XaeroPlusBooleanSetting alwaysRenderPlayerWithNameOnRadar = XaeroPlusBooleanSetting.create("Always Render Player Name",
            "Always render player name on the radar.",
            true, SettingLocation.MINIMAP_ENTITY_RADAR);
    public static final XaeroPlusBooleanSetting alwaysRenderPlayerIconOnRadar = XaeroPlusBooleanSetting.create("Always Render Player Icon",
            "Always render player icon on the radar.",
            true, SettingLocation.MINIMAP_ENTITY_RADAR);
    public static final XaeroPlusBooleanSetting fixMainEntityDot = XaeroPlusBooleanSetting.create("Fix Main Entity Dot",
            "Fixes the main entity dot rendering on the radar when arrow is rendered.",
            true, SettingLocation.MINIMAP_ENTITY_RADAR);
}
