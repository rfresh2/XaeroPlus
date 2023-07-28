package xaeroplus.settings;

import xaeroplus.module.ModuleManager;
import xaeroplus.module.impl.*;
import xaeroplus.util.*;

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
            "setting.world_map.fast_mapping.tooltip",
            true,
            SettingLocation.WORLD_MAP_MAIN);
    public static final XaeroPlusFloatSetting fastMapMaxTilesPerCycle = XaeroPlusFloatSetting.create(
            "Fast Mapping Chunk Limit",
            "setting.world_map.fast_mapping_rate_limit",
            10, 120, 10,
            "setting.world_map.fast_mapping_rate_limit.tooltip",
            80,
            SettingLocation.WORLD_MAP_MAIN);
    public static final XaeroPlusBooleanSetting baritoneWaypointSyncSetting = XaeroPlusBooleanSetting.create(
            "Baritone Goal Waypoint",
            "setting.world_map.baritone_waypoint",
            "setting.world_map.baritone_waypoint.tooltip",
            BaritoneHelper::isBaritonePresent,
            (b) -> {
                if (BaritoneHelper.isBaritonePresent()) ModuleManager.getModule(BaritoneGoalSync.class).setEnabled(b);
            },
            true,
            SettingLocation.WORLD_MAP_MAIN);
    public static final XaeroPlusBooleanSetting waystonesWaypointSyncSetting = XaeroPlusBooleanSetting.create(
            "Waystones Waypoint Sync",
            "setting.world_map.waystones_sync",
            "setting.world_map.waystones_sync.tooltip",
            WaystonesHelper::isWaystonesPresent,
            (b) -> {
                if (WaystonesHelper.isWaystonesPresent()) ModuleManager.getModule(WaystoneSync.class).setEnabled(b);
            },
            true,
            SettingLocation.WORLD_MAP_MAIN);
    public static final XaeroPlusBooleanSetting waystonesCrossDimSyncSetting = XaeroPlusBooleanSetting.create(
            "Waystones Dimension Sync",
            "setting.world_map.cross_dim_waystones_sync",
            "setting.world_map.cross_dim_waystones_sync.tooltip",
            WaystonesHelper::isWaystonesPresent,
            false,
            SettingLocation.WORLD_MAP_MAIN);
    public static final XaeroPlusBooleanSetting persistMapDimensionSwitchSetting = XaeroPlusBooleanSetting.create(
            "Persist WM Dim Switch",
            "setting.world_map.persist_dimension_switch",
            "setting.world_map.persist_dimension_switch.tooltip",
            false,
            SettingLocation.WORLD_MAP_MAIN);
    public static final XaeroPlusBooleanSetting transparentObsidianRoofSetting = XaeroPlusBooleanSetting.create(
            "Transparent Obsidian Roof",
            "setting.world_map.transparent_obsidian_roof",
            "setting.world_map.transparent_obsidian_roof.tooltip",
            (v) -> markChunksDirtyInWriteDistance(),
            true,
            SettingLocation.WORLD_MAP_MAIN);
    public static final XaeroPlusFloatSetting transparentObsidianRoofDarkeningSetting = XaeroPlusFloatSetting.create(
            "Roof Obsidian Opacity",
            "setting.world_map.transparent_obsidian_roof_darkening",
            0, 255, 5,
            "setting.world_map.transparent_obsidian_roof_darkening.tooltip",
            (v) -> markChunksDirtyInWriteDistance(),
            150,
            SettingLocation.WORLD_MAP_MAIN);
    public static final XaeroPlusFloatSetting worldMapMinZoomSetting = XaeroPlusFloatSetting.create(
            "Min WorldMap Zoom",
            "setting.world_map.min_zoom",
            0, 0.625f, 0.01f,
            "setting.world_map.min_zoom.tooltip",
            0.1f,
            SettingLocation.WORLD_MAP_MAIN);
    public static final XaeroPlusBooleanSetting skipWorldRenderSetting = XaeroPlusBooleanSetting.create(
            "Skip Background Render",
            "setting.world_map.skip_world_render",
            "setting.world_map.skip_world_render.tooltip",
            false,
            SettingLocation.WORLD_MAP_MAIN);
    public static final XaeroPlusBooleanSetting newChunksEnabledSetting = XaeroPlusBooleanSetting.create(
            "NewChunks Highlighting",
            "setting.world_map.new_chunks_highlighting",
            "setting.world_map.new_chunks_highlighting.tooltip",
            (b) -> ModuleManager.getModule(NewChunks.class).setEnabled(b),
            false,
            SettingLocation.WORLD_MAP_MAIN);
    public static final XaeroPlusBooleanSetting newChunksSaveLoadToDisk = XaeroPlusBooleanSetting.create(
            "Save/Load NewChunks to Disk",
            "setting.world_map.new_chunks_save_load_to_disk",
            "setting.world_map.new_chunks_save_load_to_disk.tooltip",
            (b) -> ModuleManager.getModule(NewChunks.class).setNewChunksCache(b),
            true,
            SettingLocation.WORLD_MAP_MAIN);
    public static final XaeroPlusFloatSetting newChunksAlphaSetting = XaeroPlusFloatSetting.create(
            "New Chunks Opacity",
            "setting.world_map.new_chunks_opacity",
            10f, 255f, 10f,
            "setting.world_map.new_chunks_opacity.tooltip",
            (b) -> ModuleManager.getModule(NewChunks.class).setAlpha(b),
            100,
            SettingLocation.WORLD_MAP_MAIN);
    public static final XaeroPlusEnumSetting<ColorHelper.HighlightColor> newChunksColorSetting = XaeroPlusEnumSetting.create(
            "New Chunks Color",
            "setting.world_map.new_chunks_color",
            "setting.world_map.new_chunks_color.tooltip",
            (b) -> ModuleManager.getModule(NewChunks.class).setRgbColor(b.getColor()),
            ColorHelper.HighlightColor.values(),
            ColorHelper.HighlightColor.RED,
            SettingLocation.WORLD_MAP_MAIN);
    public static final XaeroPlusBooleanSetting portalsEnabledSetting = XaeroPlusBooleanSetting.create(
            "Portal Highlights",
            "setting.world_map.portals",
            "setting.world_map.portals.tooltip",
            (b) -> ModuleManager.getModule(Portals.class).setEnabled(b),
            true,
            SettingLocation.WORLD_MAP_MAIN);
    public static final XaeroPlusFloatSetting portalsAlphaSetting = XaeroPlusFloatSetting.create(
            "Portal Highlights Opacity",
            "setting.world_map.portals_opacity",
            10f, 255f, 10f,
            "setting.world_map.portals_opacity.tooltip",
            (b) -> ModuleManager.getModule(Portals.class).setAlpha(b),
            100,
            SettingLocation.WORLD_MAP_MAIN);
    public static final XaeroPlusEnumSetting<ColorHelper.HighlightColor> portalsColorSetting = XaeroPlusEnumSetting.create(
            "Portal Highlights Color",
            "setting.world_map.portals_color",
            "setting.world_map.portals_color.tooltip",
            (b) -> ModuleManager.getModule(Portals.class).setRgbColor(b.getColor()),
            ColorHelper.HighlightColor.values(),
            ColorHelper.HighlightColor.MAGENTA,
            SettingLocation.WORLD_MAP_MAIN);
    public static final XaeroPlusBooleanSetting portalSkipDetectionEnabledSetting = XaeroPlusBooleanSetting.create(
            "PortalSkip Detection",
            "setting.world_map.portal_skip_detection",
            "setting.world_map.portal_skip_detection.tooltip",
            (b) -> ModuleManager.getModule(PortalSkipDetection.class).setEnabled(b),
            false,
            SettingLocation.WORLD_MAP_MAIN);
    public static final XaeroPlusFloatSetting portalSkipDetectionAlphaSetting = XaeroPlusFloatSetting.create(
            "PortalSkip Opacity",
            "setting.world_map.portal_skip_opacity",
            10f, 255f, 10f,
            "setting.world_map.portal_skip_opacity.tooltip",
            (b) -> ModuleManager.getModule(PortalSkipDetection.class).setAlpha(b),
            100,
            SettingLocation.WORLD_MAP_MAIN);
    public static final XaeroPlusEnumSetting<ColorHelper.HighlightColor> portalSkipDetectionColorSetting = XaeroPlusEnumSetting.create(
            "PortalSkip Color",
            "setting.world_map.portal_skip_color",
            "setting.world_map.portal_skip_color.tooltip",
            (b) -> ModuleManager.getModule(PortalSkipDetection.class).setRgbColor(b.getColor()),
            ColorHelper.HighlightColor.values(),
            ColorHelper.HighlightColor.WHITE,
            SettingLocation.WORLD_MAP_MAIN);
    public static final XaeroPlusFloatSetting portalSkipDetectionSearchDelayTicksSetting = XaeroPlusFloatSetting.create(
            "PortalSkip Search Delay",
            "setting.world_map.portal_skip_search_delay",
            0, 100, 1,
            "setting.world_map.portal_skip_search_delay.tooltip",
            (b) -> ModuleManager.getModule(PortalSkipDetection.class).setSearchDelayTicks(b),
            10,
            SettingLocation.WORLD_MAP_MAIN);
    public static final XaeroPlusBooleanSetting wdlEnabledSetting = XaeroPlusBooleanSetting.create(
            "WDL Highlight",
            "setting.world_map.wdl_highlight",
            "setting.world_map.wdl_highlight.tooltip",
            WDLHelper::isWdlPresent,
            false,
            SettingLocation.WORLD_MAP_MAIN);
    public static final XaeroPlusFloatSetting wdlAlphaSetting = XaeroPlusFloatSetting.create(
            "WDL Opacity",
            "setting.world_map.wdl_opacity",
            10f, 255f, 10f,
            "setting.world_map.wdl_opacity.tooltip",
            WDLHelper::isWdlPresent,
            WDLHelper::setAlpha,
            100,
            SettingLocation.WORLD_MAP_MAIN);
    public static final XaeroPlusEnumSetting<ColorHelper.HighlightColor> wdlColorSetting = XaeroPlusEnumSetting.create(
            "WDL Color",
            "setting.world_map.wdl_color",
            "setting.world_map.wdl_color.tooltip",
            WDLHelper::isWdlPresent,
            (b) -> WDLHelper.setRgbColor(b.getColor()),
            ColorHelper.HighlightColor.values(),
            ColorHelper.HighlightColor.GREEN,
            SettingLocation.WORLD_MAP_MAIN);
    public static final XaeroPlusBooleanSetting owAutoWaypointDimension = XaeroPlusBooleanSetting.create(
            "Prefer Overworld Waypoints",
            "setting.world_map.ow_auto_waypoint_dimension",
            "setting.world_map.ow_auto_waypoint_dimension.tooltip",
            true,
            SettingLocation.WORLD_MAP_MAIN);
    public static final XaeroPlusBooleanSetting showWaypointDistances = XaeroPlusBooleanSetting.create(
            "Show Waypoint Distances",
            "setting.world_map.show_waypoint_distances",
            "setting.world_map.show_waypoint_distances.tooltip",
            true,
            SettingLocation.WORLD_MAP_MAIN);
    public static final XaeroPlusBooleanSetting showRenderDistanceSetting = XaeroPlusBooleanSetting.create(
            "Show Render Distance",
            "setting.world_map.show_render_distance",
            "setting.world_map.show_render_distance.tooltip",
            false,
            SettingLocation.MINIMAP_OVERLAYS);
    public static final XaeroPlusBooleanSetting showRenderDistanceWorldMapSetting = XaeroPlusBooleanSetting.create(
            "Show Render Distance WorldMap",
            "setting.world_map.show_render_distance_world_map",
            "setting.world_map.show_render_distance_world_map.tooltip",
            false,
            SettingLocation.MINIMAP_OVERLAYS);
    public static final XaeroPlusFloatSetting assumedServerRenderDistanceSetting = XaeroPlusFloatSetting.create(
            "Server Render Distance",
            "setting.world_map.assumed_server_render_distance",
            1f, 32f, 1f,
            "setting.world_map.assumed_server_render_distance.tooltip",
            4f,
            SettingLocation.MINIMAP_OVERLAYS); // 2b2t
    public static final XaeroPlusBooleanSetting nullOverworldDimensionFolder = XaeroPlusBooleanSetting.create(
            "null OW Dim Dir",
            "setting.world_map.null_overworld_dimension_folder",
            "setting.world_map.null_overworld_dimension_folder.tooltip",
            true,
            SettingLocation.WORLD_MAP_MAIN);
    public static final XaeroPlusEnumSetting<DataFolderResolutionMode> dataFolderResolutionMode = XaeroPlusEnumSetting.create(
            "Data Dir Mode",
            "setting.world_map.data_folder_resolution_mode",
            "setting.world_map.data_folder_resolution_mode.tooltip",
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
            "setting.minimap.transparent_background.tooltip",
            false,
            SettingLocation.MINIMAP);
    public static final XaeroPlusFloatSetting minimapScaling = XaeroPlusFloatSetting.create(
            "Minimap Scaling Factor",
            "setting.minimap.minimap_scaling",
            // todo: increase max. need design improvements
            1f, 2f, 1f,
            "setting.minimap.minimap_scaling.tooltip",
            (b) -> Shared.shouldResetFBO = true,
            2f,
            SettingLocation.MINIMAP);
    public static final XaeroPlusBooleanSetting switchToNetherSetting = XaeroPlusBooleanSetting.create(
            "Switch to Nether",
            "setting.keybinds.switch_to_nether",
            "setting.keybinds.switch_to_nether.tooltip",
            (b) -> Shared.switchToDimension(-1),
            false,
            SettingLocation.KEYBINDS);
    public static final XaeroPlusBooleanSetting switchToOverworldSetting = XaeroPlusBooleanSetting.create(
            "Switch to Overworld",
            "setting.keybinds.switch_to_overworld",
            "setting.keybinds.switch_to_overworld.tooltip",
            (b) -> Shared.switchToDimension(0),
            false,
            SettingLocation.KEYBINDS);
    public static final XaeroPlusBooleanSetting switchToEndSetting = XaeroPlusBooleanSetting.create(
            "Switch to End",
            "setting.keybinds.switch_to_end",
            "setting.keybinds.switch_to_end.tooltip",
            (b) -> Shared.switchToDimension(1),
            false,
            SettingLocation.KEYBINDS);
    public static final XaeroPlusBooleanSetting netherCaveFix = XaeroPlusBooleanSetting.create(
            "Nether Cave Fix",
            "setting.world_map.nether_cave_fix",
            "setting.world_map.nether_cave_fix.tooltip",
            true,
            SettingLocation.WORLD_MAP_MAIN);
    public static final XaeroPlusBooleanSetting alwaysRenderPlayerWithNameOnRadar = XaeroPlusBooleanSetting.create(
            "Always Render Player Name",
            "setting.minimap.always_render_player_name",
            "setting.minimap.always_render_player_name.tooltip",
            true,
            SettingLocation.MINIMAP_ENTITY_RADAR);
    public static final XaeroPlusBooleanSetting alwaysRenderPlayerIconOnRadar = XaeroPlusBooleanSetting.create(
            "Always Render Player Icon",
            "setting.minimap.always_render_player_icon",
            "setting.minimap.always_render_player_icon.tooltip",
            true,
            SettingLocation.MINIMAP_ENTITY_RADAR);
    public static final XaeroPlusBooleanSetting fixMainEntityDot = XaeroPlusBooleanSetting.create(
            "Fix Main Entity Dot",
            "setting.minimap.fix_main_entity_dot",
            "setting.minimap.fix_main_entity_dot.tooltip",
            true,
            SettingLocation.MINIMAP_ENTITY_RADAR);
    public static final XaeroPlusBooleanSetting waypointBeacons = XaeroPlusBooleanSetting.create(
            "Waypoint Beacons",
            "setting.waypoints.waypoint_beacons",
            "setting.waypoints.waypoint_beacons.tooltip",
            true,
            SettingLocation.WAYPOINTS);
    public static final XaeroPlusFloatSetting waypointBeaconScaleMin = XaeroPlusFloatSetting.create(
            "Waypoint Beacon Scale Min",
            "setting.waypoints.waypoint_beacon_scale_min",
            0f, 30f, 1f,
            "setting.waypoints.waypoint_beacon_scale_min.tooltip",
            0f,
            SettingLocation.WAYPOINTS);
    public static final XaeroPlusFloatSetting waypointBeaconDistanceMin = XaeroPlusFloatSetting.create(
            "Waypoint Beacon Distance Min",
            "setting.waypoints.waypoint_beacon_distance_min",
            0f, 512f, 8f,
            "setting.waypoints.waypoint_beacon_distance_min.tooltip",
            0f,
            SettingLocation.WAYPOINTS);
    public static final XaeroPlusBooleanSetting crossDimensionTeleportCommand = XaeroPlusBooleanSetting.create(
            "Cross Dimension Teleport",
            "setting.world_map.cross_dimension_teleport",
            "setting.world_map.cross_dimension_teleport.tooltip",
            true,
            SettingLocation.WORLD_MAP_MAIN);
}
