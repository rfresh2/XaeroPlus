package xaeroplus.settings;

import xaeroplus.module.ModuleManager;
import xaeroplus.module.impl.*;
import xaeroplus.util.*;

import static xaeroplus.util.GuiMapHelper.markChunksDirtyInWriteDistance;

/**
 * Registry for XaeroPlus-specific settings
 */
public final class Settings extends SettingRegistry {
    public static final Settings REGISTRY = new Settings();

    /**
     * The order settings are defined here determines the order in the settings GUI's.
     */

    public final BooleanSetting fastMapSetting = register(
        BooleanSetting.create(
            "Fast Mapping",
            "setting.world_map.fast_mapping",
            "setting.world_map.fast_mapping.tooltip",
            true
        ), SettingLocation.WORLD_MAP_MAIN);
    public final DoubleSetting fastMapMaxTilesPerCycle = register(
        DoubleSetting.create(
            "Fast Mapping Chunk Limit",
            "setting.world_map.fast_mapping_rate_limit",
            10, 120, 10,
            "setting.world_map.fast_mapping_rate_limit.tooltip",
            80),
        SettingLocation.WORLD_MAP_MAIN);
    public final BooleanSetting baritoneWaypointSyncSetting = register(
        BooleanSetting.create(
            "Baritone Goal Waypoint",
            "setting.world_map.baritone_waypoint",
            "setting.world_map.baritone_waypoint.tooltip",
            BaritoneHelper::isBaritonePresent,
            (b) -> {
                if (BaritoneHelper.isBaritonePresent()) ModuleManager.getModule(BaritoneGoalSync.class).setEnabled(b);
            },
            true),
        SettingLocation.WORLD_MAP_MAIN
    );
    public final BooleanSetting waystonesWaypointSyncSetting = register(BooleanSetting.create(
            "Waystones Waypoint Sync",
            "setting.world_map.waystones_sync",
            "setting.world_map.waystones_sync.tooltip",
            WaystonesHelper::isWaystonesPresent,
            (b) -> {
                if (WaystonesHelper.isWaystonesPresent()) ModuleManager.getModule(WaystoneSync.class).setEnabled(b);
            },
            true),
        SettingLocation.WORLD_MAP_MAIN
    );
    public final BooleanSetting waystonesCrossDimSyncSetting = register(
        BooleanSetting.create(
            "Waystones Dimension Sync",
            "setting.world_map.cross_dim_waystones_sync",
            "setting.world_map.cross_dim_waystones_sync.tooltip",
            WaystonesHelper::isWaystonesPresent,
            false),
        SettingLocation.WORLD_MAP_MAIN
    );
    public final BooleanSetting persistMapDimensionSwitchSetting = register(
        BooleanSetting.create(
            "Persist Dim Switch",
            "setting.world_map.persist_dimension_switch",
            "setting.world_map.persist_dimension_switch.tooltip",
            false),
        SettingLocation.WORLD_MAP_MAIN
    );
    public final BooleanSetting transparentObsidianRoofSetting = register(
        BooleanSetting.create(
            "Transparent Obsidian Roof",
            "setting.world_map.transparent_obsidian_roof",
            "setting.world_map.transparent_obsidian_roof.tooltip",
            (v) -> markChunksDirtyInWriteDistance(),
            true),
        SettingLocation.WORLD_MAP_MAIN);
    public final DoubleSetting transparentObsidianRoofDarkeningSetting = register(
        DoubleSetting.create(
            "Roof Obsidian Opacity",
            "setting.world_map.transparent_obsidian_roof_darkening",
            0, 255, 5,
            "setting.world_map.transparent_obsidian_roof_darkening.tooltip",
            (v) -> markChunksDirtyInWriteDistance(),
            150),
        SettingLocation.WORLD_MAP_MAIN);
    public final DoubleSetting worldMapMinZoomSetting = register(
        DoubleSetting.create(
            "Min WorldMap Zoom",
            "setting.world_map.min_zoom",
            0, 0.625f, 0.01f,
            "setting.world_map.min_zoom.tooltip",
            0.1f),
        SettingLocation.WORLD_MAP_MAIN);
    public final BooleanSetting skipWorldRenderSetting = register(
        BooleanSetting.create(
            "Skip Background Render",
            "setting.world_map.skip_world_render",
            "setting.world_map.skip_world_render.tooltip",
            false),
        SettingLocation.WORLD_MAP_MAIN
    );
    public final BooleanSetting newChunksEnabledSetting = register(
        BooleanSetting.create(
            "NewChunks Highlighting",
            "setting.world_map.new_chunks_highlighting",
            "setting.world_map.new_chunks_highlighting.tooltip",
            (b) -> ModuleManager.getModule(NewChunks.class).setEnabled(b),
            false),
        SettingLocation.WORLD_MAP_MAIN);
    public final BooleanSetting newChunksSaveLoadToDisk = register(
        BooleanSetting.create(
            "Save/Load NewChunks to Disk",
            "setting.world_map.new_chunks_save_load_to_disk",
            "setting.world_map.new_chunks_save_load_to_disk.tooltip",
            (b) -> ModuleManager.getModule(NewChunks.class).setNewChunksCache(b),
            true),
        SettingLocation.WORLD_MAP_MAIN);
    public final DoubleSetting newChunksAlphaSetting = register(
        DoubleSetting.create(
            "New Chunks Opacity",
            "setting.world_map.new_chunks_opacity",
            10f, 255f, 10f,
            "setting.world_map.new_chunks_opacity.tooltip",
            (b) -> ModuleManager.getModule(NewChunks.class).setAlpha(b),
            100),
        SettingLocation.WORLD_MAP_MAIN);
    public final EnumSetting<ColorHelper.HighlightColor> newChunksColorSetting = register(
        EnumSetting.create(
            "New Chunks Color",
            "setting.world_map.new_chunks_color",
            "setting.world_map.new_chunks_color.tooltip",
            (b) -> ModuleManager.getModule(NewChunks.class).setRgbColor(b.getColor()),
            ColorHelper.HighlightColor.values(),
            ColorHelper.HighlightColor.RED),
        SettingLocation.WORLD_MAP_MAIN);
    public final BooleanSetting portalsEnabledSetting = register(
        BooleanSetting.create(
            "Portal Highlights",
            "setting.world_map.portals",
            "setting.world_map.portals.tooltip",
            (b) -> ModuleManager.getModule(Portals.class).setEnabled(b),
            false),
        SettingLocation.WORLD_MAP_MAIN);
    public final DoubleSetting portalsAlphaSetting = register(
        DoubleSetting.create(
            "Portal Highlights Opacity",
            "setting.world_map.portals_opacity",
            10f, 255f, 10f,
            "setting.world_map.portals_opacity.tooltip",
            (b) -> ModuleManager.getModule(Portals.class).setAlpha(b),
            100),
        SettingLocation.WORLD_MAP_MAIN);
    public final EnumSetting<ColorHelper.HighlightColor> portalsColorSetting = register(
        EnumSetting.create(
            "Portal Highlights Color",
            "setting.world_map.portals_color",
            "setting.world_map.portals_color.tooltip",
            (b) -> ModuleManager.getModule(Portals.class).setRgbColor(b.getColor()),
            ColorHelper.HighlightColor.values(),
            ColorHelper.HighlightColor.MAGENTA),
        SettingLocation.WORLD_MAP_MAIN);
    public final BooleanSetting portalSkipDetectionEnabledSetting = register(
        BooleanSetting.create(
            "PortalSkip Detection",
            "setting.world_map.portal_skip_detection",
            "setting.world_map.portal_skip_detection.tooltip",
            (b) -> ModuleManager.getModule(PortalSkipDetection.class).setEnabled(b),
            false),
        SettingLocation.WORLD_MAP_MAIN);
    public final DoubleSetting portalSkipDetectionAlphaSetting = register(
        DoubleSetting.create(
            "PortalSkip Opacity",
            "setting.world_map.portal_skip_opacity",
            10f, 255f, 10f,
            "setting.world_map.portal_skip_opacity.tooltip",
            (b) -> ModuleManager.getModule(PortalSkipDetection.class).setAlpha(b),
            100),
        SettingLocation.WORLD_MAP_MAIN);
    public final EnumSetting<ColorHelper.HighlightColor> portalSkipDetectionColorSetting = register(
        EnumSetting.create(
            "PortalSkip Color",
            "setting.world_map.portal_skip_color",
            "setting.world_map.portal_skip_color.tooltip",
            (b) -> ModuleManager.getModule(PortalSkipDetection.class).setRgbColor(b.getColor()),
            ColorHelper.HighlightColor.values(),
            ColorHelper.HighlightColor.WHITE),
        SettingLocation.WORLD_MAP_MAIN);
    public final DoubleSetting portalSkipDetectionSearchDelayTicksSetting = register(
        DoubleSetting.create(
            "PortalSkip Search Delay",
            "setting.world_map.portal_skip_search_delay",
            0, 100, 1,
            "setting.world_map.portal_skip_search_delay.tooltip",
            (b) -> ModuleManager.getModule(PortalSkipDetection.class).setSearchDelayTicks(b),
            10),
        SettingLocation.WORLD_MAP_MAIN);
    public final BooleanSetting wdlEnabledSetting = register(
        BooleanSetting.create(
            "WDL Highlight",
            "setting.world_map.wdl_highlight",
            "setting.world_map.wdl_highlight.tooltip",
            WDLHelper::isWdlPresent,
            false),
        SettingLocation.WORLD_MAP_MAIN
    );
    public final DoubleSetting wdlAlphaSetting = register(
        DoubleSetting.create(
            "WDL Opacity",
            "setting.world_map.wdl_opacity",
            10f, 255f, 10f,
            "setting.world_map.wdl_opacity.tooltip",
            WDLHelper::isWdlPresent,
            WDLHelper::setAlpha,
            100),
        SettingLocation.WORLD_MAP_MAIN);
    public final EnumSetting<ColorHelper.HighlightColor> wdlColorSetting = register(
        EnumSetting.create(
            "WDL Color",
            "setting.world_map.wdl_color",
            "setting.world_map.wdl_color.tooltip",
            WDLHelper::isWdlPresent,
            (b) -> WDLHelper.setRgbColor(b.getColor()),
            ColorHelper.HighlightColor.values(),
            ColorHelper.HighlightColor.GREEN),
        SettingLocation.WORLD_MAP_MAIN);
    public final BooleanSetting showWaypointDistances = register(
        BooleanSetting.create(
            "Show Waypoint Distances",
            "setting.world_map.show_waypoint_distances",
            "setting.world_map.show_waypoint_distances.tooltip",
            true),
        SettingLocation.WAYPOINTS);
    public final BooleanSetting showRenderDistanceSetting = register(
        BooleanSetting.create(
            "Show Render Distance",
            "setting.world_map.show_render_distance",
            "setting.world_map.show_render_distance.tooltip",
            false),
        SettingLocation.MINIMAP_OVERLAYS);
    public final BooleanSetting showRenderDistanceWorldMapSetting = register(
        BooleanSetting.create(
            "Show Render Distance WorldMap",
            "setting.world_map.show_render_distance_world_map",
            "setting.world_map.show_render_distance_world_map.tooltip",
            false),
        SettingLocation.MINIMAP_OVERLAYS);
    public final DoubleSetting assumedServerRenderDistanceSetting = register(
        DoubleSetting.create(
            "Server Render Distance",
            "setting.world_map.assumed_server_render_distance",
            1f, 32f, 1f,
            "setting.world_map.assumed_server_render_distance.tooltip",
            4f),
        SettingLocation.MINIMAP_OVERLAYS); // 2b2t
    public final BooleanSetting nullOverworldDimensionFolder = register(
        BooleanSetting.create(
            "null OW Dim Dir",
            "setting.world_map.null_overworld_dimension_folder",
            "setting.world_map.null_overworld_dimension_folder.tooltip",
            () -> false,
            true),
        SettingLocation.WORLD_MAP_MAIN
    );
    public final EnumSetting<DataFolderResolutionMode> dataFolderResolutionMode = register(
        EnumSetting.create(
            "Data Dir Mode",
            "setting.world_map.data_folder_resolution_mode",
            "setting.world_map.data_folder_resolution_mode.tooltip",
            DataFolderResolutionMode.values(),
            DataFolderResolutionMode.IP),
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
    public final BooleanSetting transparentMinimapBackground = register(
        BooleanSetting.create(
            "Transparent Background",
            "setting.minimap.transparent_background",
            "setting.minimap.transparent_background.tooltip",
            false),
        SettingLocation.MINIMAP);
    public final DoubleSetting minimapScaling = register(
        DoubleSetting.create(
            "Minimap Scaling Factor",
            "setting.minimap.minimap_scaling",
            // todo: increase max. need design improvements
            1f, 2f, 1f,
            "setting.minimap.minimap_scaling.tooltip",
            (b) -> Globals.shouldResetFBO = true,
            2f),
        SettingLocation.MINIMAP);
    public final BooleanSetting switchToNetherSetting = register(
        BooleanSetting.create(
            "Switch to Nether",
            "setting.keybinds.switch_to_nether",
            "setting.keybinds.switch_to_nether.tooltip",
            (b) -> Globals.switchToDimension(-1),
            false),
        SettingLocation.KEYBINDS);
    public final BooleanSetting switchToOverworldSetting = register(
        BooleanSetting.create(
            "Switch to Overworld",
            "setting.keybinds.switch_to_overworld",
            "setting.keybinds.switch_to_overworld.tooltip",
            (b) -> Globals.switchToDimension(0),
            false),
        SettingLocation.KEYBINDS);
    public final BooleanSetting switchToEndSetting = register(
        BooleanSetting.create(
            "Switch to End",
            "setting.keybinds.switch_to_end",
            "setting.keybinds.switch_to_end.tooltip",
            (b) -> Globals.switchToDimension(1),
            false),
        SettingLocation.KEYBINDS);
    public final BooleanSetting netherCaveFix = register(
        BooleanSetting.create(
            "Nether Cave Fix",
            "setting.world_map.nether_cave_fix",
            "setting.world_map.nether_cave_fix.tooltip",
            true),
        SettingLocation.WORLD_MAP_MAIN);
    public final BooleanSetting alwaysRenderPlayerWithNameOnRadar = register(
        BooleanSetting.create(
            "Always Render Player Name",
            "setting.minimap.always_render_player_name",
            "setting.minimap.always_render_player_name.tooltip",
            true),
        SettingLocation.MINIMAP_ENTITY_RADAR);
    public final BooleanSetting alwaysRenderPlayerIconOnRadar = register(
        BooleanSetting.create(
            "Always Render Player Icon",
            "setting.minimap.always_render_player_icon",
            "setting.minimap.always_render_player_icon.tooltip",
            true),
        SettingLocation.MINIMAP_ENTITY_RADAR);
    public final BooleanSetting fixMainEntityDot = register(
        BooleanSetting.create(
            "Fix Main Entity Dot",
            "setting.minimap.fix_main_entity_dot",
            "setting.minimap.fix_main_entity_dot.tooltip",
            true),
        SettingLocation.MINIMAP_ENTITY_RADAR
    );
    public final BooleanSetting waypointBeacons = register(
        BooleanSetting.create(
            "Waypoint Beacons",
            "setting.waypoints.waypoint_beacons",
            "setting.waypoints.waypoint_beacons.tooltip",
            true),
        SettingLocation.WAYPOINTS);
    public final DoubleSetting waypointBeaconScaleMin = register(
        DoubleSetting.create(
            "Waypoint Beacon Scale Min",
            "setting.waypoints.waypoint_beacon_scale_min",
            0, 30, 1,
            "setting.waypoints.waypoint_beacon_scale_min.tooltip",
            0),
        SettingLocation.WAYPOINTS);
    public final DoubleSetting waypointBeaconDistanceMin = register(
        DoubleSetting.create(
            "Waypoint Beacon Distance Min",
            "setting.waypoints.waypoint_beacon_distance_min",
            0, 512, 8,
            "setting.waypoints.waypoint_beacon_distance_min.tooltip",
            0),
        SettingLocation.WAYPOINTS);
}
