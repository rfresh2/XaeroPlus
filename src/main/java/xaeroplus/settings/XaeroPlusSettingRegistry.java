package xaeroplus.settings;

import xaeroplus.module.ModuleManager;
import xaeroplus.module.impl.BaritoneGoalSync;
import xaeroplus.module.impl.NewChunks;
import xaeroplus.module.impl.PortalSkipDetection;
import xaeroplus.util.BaritoneHelper;
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

    public static final XaeroPlusBooleanSetting fastMapSetting = XaeroPlusBooleanSetting.create(
            "Fast Mapping",
            "setting.world_map.fast_mapping",
            "Increases mapping speed, might hurt FPS. Adjust rate limit and delay to regain FPS.",
            true,
            SettingLocation.WORLD_MAP_MAIN);
    public static final XaeroPlusFloatSetting fastMapWriterDelaySetting = XaeroPlusFloatSetting.create(
            "Fast Mapping Delay",
            "setting.world_map.fast_mapping_delay",
            10, 1000, 10,
            "Fast Mapping must be enabled. \\n " +
                    "This is roughly the delay in milliseconds between minimap update operations, both render and actual file writes.",
            250,
            SettingLocation.WORLD_MAP_MAIN);
    public static final XaeroPlusFloatSetting fastMapMaxTilesPerCycle = XaeroPlusFloatSetting.create(
            "Fast Mapping Rate Limit",
            "setting.world_map.fast_mapping_rate_limit",
            10, 120, 10,
            "Fast Mapping must be enabled. \\n " +
                    "Limits how many chunks can be written in a single cycle. Lower values improve FPS at high render distances.",
            50,
            SettingLocation.WORLD_MAP_MAIN);
    public static final XaeroPlusBooleanSetting baritoneWaypointSyncSetting = XaeroPlusBooleanSetting.create(
            "Baritone Goal Waypoint",
            "setting.world_map.baritone_waypoint",
            "Syncs Baritone goals as temporary waypoints.",
            BaritoneHelper::isBaritonePresent,
            (b) -> {
                if (BaritoneHelper.isBaritonePresent()) ModuleManager.getModule(BaritoneGoalSync.class).setEnabled(b);
            },
            true,
            SettingLocation.WORLD_MAP_MAIN);
    public static final XaeroPlusBooleanSetting persistMapDimensionSwitchSetting = XaeroPlusBooleanSetting.create(
            "Persist WM Dim Switch",
            "setting.world_map.persist_dimension_switch",
            "If enabled, the dimension will not be switched back to current when the WorldMap GUI is closed.",
            false,
            SettingLocation.WORLD_MAP_MAIN);
    public static final XaeroPlusBooleanSetting transparentObsidianRoofSetting = XaeroPlusBooleanSetting.create(
            "Transparent Obsidian Roof",
            "setting.world_map.transparent_obsidian_roof",
            "Makes obsidian placed at build height transparent. Does not update tiles already mapped - you need to remap them.",
            (v) -> markChunksDirtyInWriteDistance(),
            true,
            SettingLocation.WORLD_MAP_MAIN);
    public static final XaeroPlusFloatSetting transparentObsidianRoofDarkeningSetting = XaeroPlusFloatSetting.create(
            "Roof Obsidian Opacity",
            "setting.world_map.transparent_obsidian_roof_darkening",
            0, 255, 5,
            "Sets the opacity of the transparent obsidian roof tiles.",
            (v) -> markChunksDirtyInWriteDistance(),
            150,
            SettingLocation.WORLD_MAP_MAIN);
    public static final XaeroPlusFloatSetting worldMapMinZoomSetting = XaeroPlusFloatSetting.create(
            "Min WorldMap Zoom",
            "setting.world_map.min_zoom",
            0, 0.625f, 0.01f,
            "Minimum WorldMap Zoom Setting. This is 10x what you see on the WorldMap.",
            0.1f,
            SettingLocation.WORLD_MAP_MAIN);
    public static final XaeroPlusBooleanSetting skipWorldRenderSetting = XaeroPlusBooleanSetting.create(
            "Skip Background Render",
            "setting.world_map.skip_world_render",
            "Skip MC world render while in a Xaero GUI. Having this on can cause issues with travel mods while you're in a Xaero GUI like the WorldMap.",
            false,
            SettingLocation.WORLD_MAP_MAIN);
    public static final XaeroPlusBooleanSetting newChunksEnabledSetting = XaeroPlusBooleanSetting.create(
            "NewChunks Highlighting",
            "setting.world_map.new_chunks_highlighting",
            "Highlights NewChunks on the Minimap and WorldMap.",
            (b) -> ModuleManager.getModule(NewChunks.class).setEnabled(b),
            false,
            SettingLocation.WORLD_MAP_MAIN);
    public static final XaeroPlusBooleanSetting newChunksSaveLoadToDisk = XaeroPlusBooleanSetting.create(
            "Save/Load NewChunks to Disk",
            "setting.world_map.new_chunks_save_load_to_disk",
            "Saves and loads NewChunk data to disk for each world and dimension. Requires NewChunk Highlighting to be enabled.",
            (b) -> ModuleManager.getModule(NewChunks.class).setNewChunksCache(b),
            true,
            SettingLocation.WORLD_MAP_MAIN);
    public static final XaeroPlusFloatSetting newChunksAlphaSetting = XaeroPlusFloatSetting.create(
            "New Chunks Opacity",
            "setting.world_map.new_chunks_opacity",
            10f, 255f, 10f,
            "Changes the color opacity of NewChunks.",
            (b) -> ModuleManager.getModule(NewChunks.class).setAlpha(b),
            100,
            SettingLocation.WORLD_MAP_MAIN);
    public static final XaeroPlusEnumSetting<ColorHelper.HighlightColor> newChunksColorSetting = XaeroPlusEnumSetting.create(
            "New Chunks Color",
            "setting.world_map.new_chunks_color",
            "Changes the color of NewChunks.",
            (b) -> ModuleManager.getModule(NewChunks.class).setRgbColor(b.getColor()),
            ColorHelper.HighlightColor.values(),
            ColorHelper.HighlightColor.RED,
            SettingLocation.WORLD_MAP_MAIN);
    public static final XaeroPlusBooleanSetting portalSkipDetectionEnabledSetting = XaeroPlusBooleanSetting.create(
            "PortalSkip Detection",
            "setting.world_map.portal_skip_detection",
            "Highlights chunks where portals could have been loaded into. \\n "
                    + "This is useful for basehunting to detect where players could switch dimensions along a trail to avoid hunters. \\n "
                    + "One thing to note: 2b2t's view distance is not large enough to detect portal skip areas. You need to load surrounding chunks - specifically a 15x15 chunk area",
            (b) -> ModuleManager.getModule(PortalSkipDetection.class).setEnabled(b),
            false,
            SettingLocation.WORLD_MAP_MAIN);
    public static final XaeroPlusFloatSetting portalSkipDetectionAlphaSetting = XaeroPlusFloatSetting.create(
            "PortalSkip Opacity",
            "setting.world_map.portal_skip_opacity",
            10f, 255f, 10f,
            "Changes the color opacity of Portal Skip Detection.",
            (b) -> ModuleManager.getModule(PortalSkipDetection.class).setAlpha(b),
            100,
            SettingLocation.WORLD_MAP_MAIN);
    public static final XaeroPlusEnumSetting<ColorHelper.HighlightColor> portalSkipDetectionColorSetting = XaeroPlusEnumSetting.create(
            "PortalSkip Color",
            "setting.world_map.portal_skip_color",
            "Changes the color of Portal Skip Detection.",
            (b) -> ModuleManager.getModule(PortalSkipDetection.class).setRgbColor(b.getColor()),
            ColorHelper.HighlightColor.values(),
            ColorHelper.HighlightColor.WHITE,
            SettingLocation.WORLD_MAP_MAIN);
    public static final XaeroPlusFloatSetting portalSkipDetectionSearchDelayTicksSetting = XaeroPlusFloatSetting.create(
            "PortalSkip Search Delay",
            "setting.world_map.portal_skip_search_delay",
            0, 100, 1,
            "Portal Skip Detection must be enabled. \\n " +
                    "This is the delay in ticks between Portal Skip Detection search operations.",
            (b) -> ModuleManager.getModule(PortalSkipDetection.class).setSearchDelayTicks(b),
            10,
            SettingLocation.WORLD_MAP_MAIN);
    public static final XaeroPlusBooleanSetting wdlEnabledSetting = XaeroPlusBooleanSetting.create(
            "WDL Highlight",
            "setting.world_map.wdl_highlight",
            "Highlights chunks WDL mod has downloaded on the Minimap and WorldMap.",
            WDLHelper::isWdlPresent,
            false,
            SettingLocation.WORLD_MAP_MAIN);
    public static final XaeroPlusFloatSetting wdlAlphaSetting = XaeroPlusFloatSetting.create(
            "WDL Opacity",
            "setting.world_map.wdl_opacity",
            10f, 255f, 10f,
            "Changes the color opacity of WDL chunks.",
            WDLHelper::isWdlPresent,
            WDLHelper::setAlpha,
            100,
            SettingLocation.WORLD_MAP_MAIN);
    public static final XaeroPlusEnumSetting<ColorHelper.HighlightColor> wdlColorSetting = XaeroPlusEnumSetting.create(
            "WDL Color",
            "setting.world_map.wdl_color",
            "Changes the color of WDL chunks.",
            WDLHelper::isWdlPresent,
            (b) -> WDLHelper.setRgbColor(b.getColor()),
            ColorHelper.HighlightColor.values(),
            ColorHelper.HighlightColor.GREEN,
            SettingLocation.WORLD_MAP_MAIN);
    public static final XaeroPlusBooleanSetting owAutoWaypointDimension = XaeroPlusBooleanSetting.create(
            "Prefer Overworld Waypoints",
            "setting.world_map.ow_auto_waypoint_dimension",
            "Prefer creating and viewing Overworld waypoints when in the nether.",
            true,
            SettingLocation.WORLD_MAP_MAIN);
    public static final XaeroPlusBooleanSetting showWaypointDistances = XaeroPlusBooleanSetting.create(
            "Show Waypoint Distances",
            "setting.world_map.show_waypoint_distances",
            "Display the distance to every waypoint in the full waypoint menu.",
            true,
            SettingLocation.WORLD_MAP_MAIN);
    public static final XaeroPlusBooleanSetting showRenderDistanceSetting = XaeroPlusBooleanSetting.create(
            "Show Render Distance",
            "setting.world_map.show_render_distance",
            "Show server side render distance (actually just another setting)",
            false,
            SettingLocation.MINIMAP_OVERLAYS);
    public static final XaeroPlusBooleanSetting showRenderDistanceWorldMapSetting = XaeroPlusBooleanSetting.create(
            "Show Render Distance WorldMap",
            "setting.world_map.show_render_distance_world_map",
            "Show server side render distance on the WorldMap",
            false,
            SettingLocation.MINIMAP_OVERLAYS);
    public static final XaeroPlusFloatSetting assumedServerRenderDistanceSetting = XaeroPlusFloatSetting.create(
            "Server Render Distance",
            "setting.world_map.assumed_server_render_distance",
            1f, 32f, 1f,
            "view-distance of the server",
            4f,
            SettingLocation.MINIMAP_OVERLAYS); // 2b2t
    public static final XaeroPlusBooleanSetting nullOverworldDimensionFolder = XaeroPlusBooleanSetting.create(
            "null OW Dim Dir",
            "setting.world_map.null_overworld_dimension_folder",
            "Sets whether the overworld WorldMap directory is in DIM0 or null (default)"
                    + " \\n MC must be restarted for changes to take effect.",
            true,
            SettingLocation.WORLD_MAP_MAIN);
    public static final XaeroPlusEnumSetting<DataFolderResolutionMode> dataFolderResolutionMode = XaeroPlusEnumSetting.create(
            "Data Dir Mode",
            "setting.world_map.data_folder_resolution_mode",
            "Sets how the WorldMap and Waypoints data folders are resolved."
                    + " \\n MC must be restarted for changes to take effect."
                    + " \\n IP = Server IP (Xaero Default). Example: .minecraft/XaeroWorldMap/Multiplayer_connect.2b2t.org"
                    + " \\n SERVER_NAME = MC Server Entry Name. Example: .minecraft/XaeroWorldMap/Multiplayer_2b2t"
                    + " \\n BASE_DOMAIN = Base Server Domain Name. Example: .minecraft/XaeroWorldMap/Multiplayer_2b2t.org",
            DataFolderResolutionMode.values(),
            DataFolderResolutionMode.IP,
            SettingLocation.WORLD_MAP_MAIN);
    public enum DataFolderResolutionMode implements TranslatableSettingEnum {
        IP("setting.world_map.data_folder_resolution_mode.ip"),
        SERVER_NAME("setting.world_map.data_folder_resolution_mode.server_name"),
        BASE_DOMAIN("setting.world_map.data_folder_resolution_mode.base_domain");

        private final String translationKey;
        DataFolderResolutionMode(final String translationKey) {
            this.translationKey = translationKey;
        }

        @Override
        public String getTranslationKey() {
            return translationKey;
        }
    }
    public static final XaeroPlusBooleanSetting transparentMinimapBackground = XaeroPlusBooleanSetting.create(
            "Transparent Background",
            "setting.minimap.transparent_background",
            "Makes the minimap background transparent instead of black.",
            false,
            SettingLocation.MINIMAP);
    public static final XaeroPlusFloatSetting minimapScaling = XaeroPlusFloatSetting.create(
            "Minimap Scaling Factor",
            "setting.minimap.minimap_scaling",
            // todo: increase max. need design improvements
            1f, 2f, 1f,
            "Increases the base minimap scale beyond the default size.",
            (b) -> Shared.shouldResetFBO = true,
            2f,
            SettingLocation.MINIMAP);
    public static final XaeroPlusBooleanSetting switchToNetherSetting = XaeroPlusBooleanSetting.create(
            "Switch to Nether",
            "setting.keybinds.switch_to_nether",
            "Switches to the nether map.",
            (b) -> Shared.switchToDimension(-1),
            false,
            SettingLocation.KEYBINDS);
    public static final XaeroPlusBooleanSetting switchToOverworldSetting = XaeroPlusBooleanSetting.create(
            "Switch to Overworld",
            "setting.keybinds.switch_to_overworld",
            "Switches to the overworld map.",
            (b) -> Shared.switchToDimension(0),
            false,
            SettingLocation.KEYBINDS);
    public static final XaeroPlusBooleanSetting switchToEndSetting = XaeroPlusBooleanSetting.create(
            "Switch to End",
            "setting.keybinds.switch_to_end",
            "Switches to the end map.",
            (b) -> Shared.switchToDimension(1),
            false,
            SettingLocation.KEYBINDS);
    public static final XaeroPlusBooleanSetting netherCaveFix = XaeroPlusBooleanSetting.create(
            "Nether Cave Fix",
            "setting.world_map.nether_cave_fix",
            "Forces full cave maps to be written and rendered when cave mode is \"off\" in the nether."
                    + " \\n Without this, you have to manually move region files pre WorldMap 1.30.0 to the correct cave folder",
            true,
            SettingLocation.WORLD_MAP_MAIN);
    public static final XaeroPlusBooleanSetting alwaysRenderPlayerWithNameOnRadar = XaeroPlusBooleanSetting.create(
            "Always Render Player Name",
            "setting.minimap.always_render_player_name",
            "Always render player name on the radar.",
            true,
            SettingLocation.MINIMAP_ENTITY_RADAR);
    public static final XaeroPlusBooleanSetting alwaysRenderPlayerIconOnRadar = XaeroPlusBooleanSetting.create(
            "Always Render Player Icon",
            "setting.minimap.always_render_player_icon",
            "Always render player icon on the radar.",
            true,
            SettingLocation.MINIMAP_ENTITY_RADAR);
    public static final XaeroPlusBooleanSetting fixMainEntityDot = XaeroPlusBooleanSetting.create(
            "Fix Main Entity Dot",
            "setting.minimap.fix_main_entity_dot",
            "Fixes the main entity dot rendering on the radar when arrow is rendered.",
            true,
            SettingLocation.MINIMAP_ENTITY_RADAR);
    public static final XaeroPlusBooleanSetting waypointBeacons = XaeroPlusBooleanSetting.create(
            "Waypoint Beacons",
            "setting.waypoints.waypoint_beacons",
            "Render waypoint beacons in game.",
            true,
            SettingLocation.WAYPOINTS);
    public static final XaeroPlusFloatSetting waypointBeaconScaleMin = XaeroPlusFloatSetting.create(
            "Waypoint Beacon Scale Min",
            "setting.waypoints.waypoint_beacon_scale_min",
            0f, 30f, 1f,
            "Sets the minimum scale of the waypoint beacon when it is far away."
                    + " \\n This value is the number of chunks away from the player the beacon is rendered at."
                    + " \\n 0 = auto-match the player's render distance",
            0f,
            SettingLocation.WAYPOINTS);
    public static final XaeroPlusFloatSetting waypointBeaconDistanceMin = XaeroPlusFloatSetting.create(
            "Waypoint Beacon Distance Min",
            "setting.waypoints.waypoint_beacon_distance_min",
            0f, 512f, 8f,
            "Sets the minimum xz distance from the player the waypoint must be to render a beacon.",
            0f,
            SettingLocation.WAYPOINTS);
}
