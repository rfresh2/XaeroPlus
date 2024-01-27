package xaeroplus.settings;

import xaeroplus.Globals;
import xaeroplus.feature.render.ColorHelper;
import xaeroplus.feature.render.ColorHelper.WaystoneColor;
import xaeroplus.module.ModuleManager;
import xaeroplus.module.impl.*;
import xaeroplus.settings.XaeroPlusSettingsReflectionHax.SettingLocation;
import xaeroplus.util.BaritoneHelper;
import xaeroplus.util.WaystonesHelper;

import java.io.ByteArrayOutputStream;

import static net.minecraft.world.level.Level.*;
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
            false,
            SettingLocation.WORLD_MAP_MAIN);
    public static final XaeroPlusFloatSetting fastMapWriterDelaySetting = XaeroPlusFloatSetting.create(
            "Fast Mapping Delay",
            "setting.world_map.fast_mapping_delay",
            10, 1000, 10,
            "setting.world_map.fast_mapping_delay.tooltip",
            250,
            SettingLocation.WORLD_MAP_MAIN);
    public static final XaeroPlusFloatSetting fastMapMaxTilesPerCycle = XaeroPlusFloatSetting.create(
            "Fast Mapping Rate Limit",
            "setting.world_map.fast_mapping_rate_limit",
            10, 120, 10,
            "setting.world_map.fast_mapping_rate_limit.tooltip",
            25,
            SettingLocation.WORLD_MAP_MAIN);
    public static final XaeroPlusBooleanSetting fastZipWrite = XaeroPlusBooleanSetting.create(
        "Fast Zip Writes",
        "setting.world_map.fast_zip_writes",
        "setting.world_map.fast_zip_writes.tooltip",
        (b) -> {
            if (!b) Globals.zipFastByteBuffer = new ByteArrayOutputStream(); // release any existing sized buffer to gc
        },
        true,
        SettingLocation.WORLD_MAP_MAIN
    );
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
    public static final XaeroPlusBooleanSetting disableWaypointSharing = XaeroPlusBooleanSetting.create(
        "Disable Waypoint Sharing",
        "setting.world_map.disable_waypoint_sharing",
        "setting.world_map.disable_waypoint_sharing.tooltip",
        false,
        SettingLocation.WORLD_MAP_MAIN);
    public static final XaeroPlusBooleanSetting disableReceivingWaypoints = XaeroPlusBooleanSetting.create(
        "Disable Receiving Waypoints",
        "setting.world_map.disable_receiving_waypoints",
        "setting.world_map.disable_receiving_waypoints.tooltip",
        false,
        SettingLocation.WORLD_MAP_MAIN);
    public static final XaeroPlusBooleanSetting waystonesWaypointSyncSetting = XaeroPlusBooleanSetting.create(
            "Waystones Sync",
            "setting.world_map.waystones_sync",
            "setting.world_map.waystones_sync.tooltip",
            WaystonesHelper::isAnyWaystonesPresent,
            (b) -> {
                if (WaystonesHelper.isAnyWaystonesPresent()) ModuleManager.getModule(WaystoneSync.class).setEnabled(b);
            },
            true,
            SettingLocation.WORLD_MAP_MAIN);
    public static final XaeroPlusEnumSetting<WaystoneColor> waystoneColorSetting = XaeroPlusEnumSetting.create(
            "Waystone Color",
            "setting.world_map.waystone_color",
            "setting.world_map.waystone_color.tooltip",
            WaystonesHelper::isAnyWaystonesPresent,
            (b) -> {
                if (WaystonesHelper.isAnyWaystonesPresent()) ModuleManager.getModule(WaystoneSync.class).setColor(b);
            },
            ColorHelper.WaystoneColor.values(),
            WaystoneColor.RANDOM,
            SettingLocation.WORLD_MAP_MAIN);
    public static final XaeroPlusBooleanSetting waystoneWaypointSetSetting = XaeroPlusBooleanSetting.create(
            "Waystone Waypoint Set",
            "setting.world_map.waystone_waypoint_set",
            "setting.world_map.waystone_waypoint_set.tooltip",
            WaystonesHelper::isAnyWaystonesPresent,
            (b) -> {
                if (WaystonesHelper.isAnyWaystonesPresent()) ModuleManager.getModule(WaystoneSync.class).setWaypointSet(b);
            },
            false,
            SettingLocation.WORLD_MAP_MAIN);
    public static final XaeroPlusBooleanSetting persistMapDimensionSwitchSetting = XaeroPlusBooleanSetting.create(
            "Persist Dim Switch",
            "setting.world_map.persist_dimension_switch",
            "setting.world_map.persist_dimension_switch.tooltip",
            true,
            SettingLocation.WORLD_MAP_MAIN);
    public static final XaeroPlusBooleanSetting radarWhileDimensionSwitchedSetting = XaeroPlusBooleanSetting.create(
            "Radar While Dim Switched",
            "setting.world_map.radar_while_dimension_switched",
            "setting.world_map.radar_while_dimension_switched.tooltip",
            true,
            SettingLocation.WORLD_MAP_MAIN);
    public static final XaeroPlusBooleanSetting transparentObsidianRoofSetting = XaeroPlusBooleanSetting.create(
            "Transparent Obsidian Roof",
            "setting.world_map.transparent_obsidian_roof",
            "setting.world_map.transparent_obsidian_roof.tooltip",
            (v) -> markChunksDirtyInWriteDistance(),
            false,
            SettingLocation.WORLD_MAP_MAIN);
    public static final XaeroPlusFloatSetting transparentObsidianRoofYSetting = XaeroPlusFloatSetting.create(
            "Roof Y Level",
            "setting.world_map.transparent_obsidian_roof_y",
            0,
            320,
            1,
            "Sets the starting Y level of the roof",
            (v) -> markChunksDirtyInWriteDistance(),
            250,
            SettingLocation.WORLD_MAP_MAIN);
    public static final XaeroPlusFloatSetting transparentObsidianRoofDarkeningSetting = XaeroPlusFloatSetting.create(
            "Roof Obsidian Opacity",
            "setting.world_map.transparent_obsidian_roof_darkening",
            0, 255, 5,
            "setting.world_map.transparent_obsidian_roof_darkening.tooltip",
            (v) -> markChunksDirtyInWriteDistance(),
            150,
            SettingLocation.WORLD_MAP_MAIN);
    public static final XaeroPlusFloatSetting transparentObsidianRoofSnowOpacitySetting = XaeroPlusFloatSetting.create(
        "Roof Snow Opacity",
        "setting.world_map.transparent_obsidian_roof_snow_opacity", // todo: translations
        0, 255, 5,
        "setting.world_map.transparent_obsidian_roof_snow_opacity.tooltip",
        (v) -> markChunksDirtyInWriteDistance(),
        10,
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
            0f, 255f, 10f,
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
    public static final XaeroPlusBooleanSetting oldChunksEnabledSetting = XaeroPlusBooleanSetting.create(
            "OldChunks Highlighting",
            "setting.world_map.old_chunks_highlighting",
            "setting.world_map.old_chunks_highlighting.tooltip",
            (b) -> ModuleManager.getModule(OldChunks.class).setEnabled(b),
            false,
            SettingLocation.WORLD_MAP_MAIN);
    public static final XaeroPlusBooleanSetting oldChunksInverse = XaeroPlusBooleanSetting.create(
            "OldChunks Inverse",
            "setting.world_map.old_chunks_inverse",
            "setting.world_map.old_chunks_inverse.tooltip",
            (b) -> ModuleManager.getModule(OldChunks.class).setInverse(b),
            false,
            SettingLocation.WORLD_MAP_MAIN);
    public static final XaeroPlusBooleanSetting oldChunksSaveLoadToDisk = XaeroPlusBooleanSetting.create(
            "Save/Load OldChunks to Disk",
            "setting.world_map.old_chunks_save_load_to_disk",
            "setting.world_map.old_chunks_save_load_to_disk.tooltip",
            (b) -> ModuleManager.getModule(OldChunks.class).setOldChunksCache(b),
            true,
            SettingLocation.WORLD_MAP_MAIN);
    public static final XaeroPlusFloatSetting oldChunksAlphaSetting = XaeroPlusFloatSetting.create(
            "Old Chunks Opacity",
            "setting.world_map.old_chunks_opacity",
            0f, 255f, 10f,
            "setting.world_map.old_chunks_opacity.tooltip",
            (b) -> ModuleManager.getModule(OldChunks.class).setAlpha(b),
            100,
            SettingLocation.WORLD_MAP_MAIN);
    public static final XaeroPlusEnumSetting<ColorHelper.HighlightColor> oldChunksColorSetting = XaeroPlusEnumSetting.create(
            "Old Chunks Color",
            "setting.world_map.old_chunks_color",
            "setting.world_map.old_chunks_color.tooltip",
            (b) -> ModuleManager.getModule(OldChunks.class).setRgbColor(b.getColor()),
            ColorHelper.HighlightColor.values(),
            ColorHelper.HighlightColor.YELLOW,
            SettingLocation.WORLD_MAP_MAIN);
    public static final XaeroPlusBooleanSetting portalsEnabledSetting = XaeroPlusBooleanSetting.create(
            "Portal Highlights",
            "setting.world_map.portals",
            "setting.world_map.portals.tooltip",
            (b) -> ModuleManager.getModule(Portals.class).setEnabled(b),
            false,
            SettingLocation.WORLD_MAP_MAIN);
    public static final XaeroPlusBooleanSetting portalsSaveLoadToDisk = XaeroPlusBooleanSetting.create(
            "Save/Load Portals to Disk",
            "setting.world_map.portals_save_load_to_disk",
            "setting.world_map.portals_save_load_to_disk.tooltip",
            (b) -> ModuleManager.getModule(Portals.class).setPortalsCache(b),
            true,
            SettingLocation.WORLD_MAP_MAIN);
    public static final XaeroPlusFloatSetting portalsAlphaSetting = XaeroPlusFloatSetting.create(
            "Portal Highlights Opacity",
            "setting.world_map.portals_opacity",
            0f, 255f, 10f,
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
            0f, 255f, 10f,
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
    public static final XaeroPlusFloatSetting portalSkipPortalRadius = XaeroPlusFloatSetting.create(
            "PortalSkip Portal Radius",
            "setting.world_map.portal_skip_portal_radius",
            0, 32, 1,
            "setting.world_map.portal_skip_portal_radius.tooltip",
            (b) -> ModuleManager.getModule(PortalSkipDetection.class).setPortalRadius(b),
            15,
            SettingLocation.WORLD_MAP_MAIN);
    public static final XaeroPlusFloatSetting portalSkipDetectionSearchDelayTicksSetting = XaeroPlusFloatSetting.create(
            "PortalSkip Search Delay",
            "setting.world_map.portal_skip_search_delay",
            0, 100, 1,
            "setting.world_map.portal_skip_search_delay.tooltip",
            (b) -> ModuleManager.getModule(PortalSkipDetection.class).setSearchDelayTicks(b),
            10,
            SettingLocation.WORLD_MAP_MAIN);
    public static final XaeroPlusBooleanSetting portalSkipNewChunksSetting = XaeroPlusBooleanSetting.create(
            "PortalSkip NewChunks",
            "setting.world_map.portal_skip_new_chunks",
            "setting.world_map.portal_skip_new_chunks.tooltip",
            (b) -> ModuleManager.getModule(PortalSkipDetection.class).setNewChunks(b),
            true,
            SettingLocation.WORLD_MAP_MAIN);
    public static final XaeroPlusBooleanSetting portalSkipOldChunkInverseSetting = XaeroPlusBooleanSetting.create(
            "PortalSkip OldChunks Inverse",
            "setting.world_map.portal_skip_old_chunks_inverse",
            "setting.world_map.portal_skip_old_chunks_inverse.tooltip",
            (b) -> ModuleManager.getModule(PortalSkipDetection.class).setOldChunksInverse(b),
            true,
            SettingLocation.WORLD_MAP_MAIN);
    public static final XaeroPlusBooleanSetting owAutoWaypointDimension = XaeroPlusBooleanSetting.create(
            "Prefer Overworld Waypoints",
            "setting.world_map.ow_auto_waypoint_dimension",
            "setting.world_map.ow_auto_waypoint_dimension.tooltip",
            false,
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
            // todo: increase max but we need to improve rendering or start using different texture levels
            "setting.minimap.minimap_scaling",
            1f, 2f, 1f,
            "setting.minimap.minimap_scaling.tooltip",
            (b) -> Globals.shouldResetFBO = true,
            2f,
            SettingLocation.MINIMAP);
    public static final XaeroPlusBooleanSetting switchToNetherSetting = XaeroPlusBooleanSetting.create(
            "Switch to Nether",
            "setting.keybinds.switch_to_nether",
            "setting.keybinds.switch_to_nether.tooltip",
            (b) -> Globals.switchToDimension(NETHER),
            false,
            SettingLocation.KEYBINDS);
    public static final XaeroPlusBooleanSetting switchToOverworldSetting = XaeroPlusBooleanSetting.create(
            "Switch to Overworld",
            "setting.keybinds.switch_to_overworld",
            "setting.keybinds.switch_to_overworld.tooltip",
            (b) -> Globals.switchToDimension(OVERWORLD),
            false,
            SettingLocation.KEYBINDS);
    public static final XaeroPlusBooleanSetting switchToEndSetting = XaeroPlusBooleanSetting.create(
            "Switch to End",
            "setting.keybinds.switch_to_end",
            "setting.keybinds.switch_to_end.tooltip",
            (b) -> Globals.switchToDimension(END),
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
}
