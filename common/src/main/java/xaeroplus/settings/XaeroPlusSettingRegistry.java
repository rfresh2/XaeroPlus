package xaeroplus.settings;

import xaeroplus.Globals;
import xaeroplus.module.ModuleManager;
import xaeroplus.module.impl.*;
import xaeroplus.settings.XaeroPlusSettingsReflectionHax.SettingLocation;
import xaeroplus.util.BaritoneHelper;
import xaeroplus.util.ColorHelper;
import xaeroplus.util.ColorHelper.WaystoneColor;
import xaeroplus.util.WaystonesHelper;
import xaeroplus.util.WorldToolsHelper;

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
    public static final XaeroPlusBooleanSetting minimapFpsLimiter = XaeroPlusBooleanSetting.create(
        "Minimap FPS Limiter",
        "setting.minimap.fps_limiter",
        "setting.minimap.fps_limiter.tooltip",
        false,
        (b) -> ModuleManager.getModule(FpsLimiter.class).setEnabled(b),
        SettingLocation.MINIMAP_MAIN);
    public static final XaeroPlusFloatSetting minimapFpsLimit = XaeroPlusFloatSetting.create(
        "Minimap FPS Limit",
        "setting.minimap.fps_limiter_limit",
        "setting.minimap.fps_limiter_limit.tooltip",
        5f, 120f, 5f,
        60f,
        SettingLocation.MINIMAP_MAIN);
    public static final XaeroPlusBooleanSetting fastMapSetting = XaeroPlusBooleanSetting.create(
        "Fast Mapping",
        "setting.world_map.fast_mapping",
        "setting.world_map.fast_mapping.tooltip",
        false,
        SettingLocation.WORLD_MAP_MAIN);
    public static final XaeroPlusFloatSetting fastMapWriterDelaySetting = XaeroPlusFloatSetting.create(
        "Fast Mapping Delay",
        "setting.world_map.fast_mapping_delay",
        "setting.world_map.fast_mapping_delay.tooltip",
        10, 1000, 10,
        250,
        SettingLocation.WORLD_MAP_MAIN);
    public static final XaeroPlusFloatSetting fastMapMaxTilesPerCycle = XaeroPlusFloatSetting.create(
        "Fast Mapping Rate Limit",
        "setting.world_map.fast_mapping_rate_limit",
        "setting.world_map.fast_mapping_rate_limit.tooltip",
        10, 120, 10,
        25,
        SettingLocation.WORLD_MAP_MAIN);
    public static final XaeroPlusBooleanSetting fastZipWrite = XaeroPlusBooleanSetting.create(
        "Fast Zip Writes",
        "setting.world_map.fast_zip_writes",
        "setting.world_map.fast_zip_writes.tooltip",
        true,
        (b) -> {
            if (!b) Globals.zipFastByteBuffer = new ByteArrayOutputStream(); // release any existing sized buffer to gc
        },
        SettingLocation.WORLD_MAP_MAIN);
    public static final XaeroPlusBooleanSetting writesWhileDimSwitched = XaeroPlusBooleanSetting.create(
        "Region Writes While Dim Switched",
        "setting.world_map.region_write_while_dimension_switched",
        "setting.world_map.region_write_while_dimension_switched.tooltip",
        false,
        SettingLocation.WORLD_MAP_MAIN);
    public static final XaeroPlusBooleanSetting baritoneWaypointSyncSetting = XaeroPlusBooleanSetting.create(
        "Baritone Goal Waypoint",
        "setting.world_map.baritone_waypoint",
        "setting.world_map.baritone_waypoint.tooltip",
        true,
        (b) -> {
            if (BaritoneHelper.isBaritonePresent()) ModuleManager.getModule(BaritoneGoalSync.class).setEnabled(b);
        },
        BaritoneHelper::isBaritonePresent,
        SettingLocation.WORLD_MAP_MAIN);
    public static final XaeroPlusBooleanSetting waystonesWaypointSyncSetting = XaeroPlusBooleanSetting.create(
        "Waystones Sync",
        "setting.world_map.waystones_sync",
        "setting.world_map.waystones_sync.tooltip",
        true,
        (b) -> {
            if (WaystonesHelper.isAnyWaystonesPresent()) ModuleManager.getModule(WaystoneSync.class).setEnabled(b);
        },
        WaystonesHelper::isAnyWaystonesPresent,
        SettingLocation.WORLD_MAP_MAIN);
    public static final XaeroPlusEnumSetting<WaystoneColor> waystoneColorSetting = XaeroPlusEnumSetting.create(
        "Waystone Color",
        "setting.world_map.waystone_color",
        "setting.world_map.waystone_color.tooltip",
        WaystoneColor.values(),
        WaystoneColor.RANDOM,
        (b) -> {
            if (WaystonesHelper.isAnyWaystonesPresent()) ModuleManager.getModule(WaystoneSync.class).setColor(b);
        },
        WaystonesHelper::isAnyWaystonesPresent,
        SettingLocation.WORLD_MAP_MAIN);
    public static final XaeroPlusBooleanSetting waystoneWaypointSetSetting = XaeroPlusBooleanSetting.create(
        "Waystone Waypoint Set",
        "setting.world_map.waystone_waypoint_set",
        "setting.world_map.waystone_waypoint_set.tooltip",
        false,
        (b) -> {
            if (WaystonesHelper.isAnyWaystonesPresent()) ModuleManager.getModule(WaystoneSync.class).setWaypointSet(b);
        },
        WaystonesHelper::isAnyWaystonesPresent,
        SettingLocation.WORLD_MAP_MAIN);
    public static final XaeroPlusEnumSetting<WaystoneWpVisibilityType> waystoneWaypointVisibilityModeSetting = XaeroPlusEnumSetting.create(
        "Waystone WP Visibility Type",
        "setting.world_map.waystone_visibility_type",
        "setting.world_map.waystone_visibility_type.tooltip",
        WaystoneWpVisibilityType.values(),
        WaystoneWpVisibilityType.LOCAL,
        (mode) -> {
            if (WaystonesHelper.isAnyWaystonesPresent()) ModuleManager.getModule(WaystoneSync.class).setVisibilityType(mode.ordinal());
        },
        WaystonesHelper::isAnyWaystonesPresent,
        SettingLocation.WORLD_MAP_MAIN);
    public enum WaystoneWpVisibilityType implements TranslatableSettingEnum {
        // order here must mirror xaero's visibility enum
        LOCAL("gui.xaeroplus.waystone_visibility_type.local"),
        GLOBAL("gui.xaeroplus.waystone_visibility_type.global"),
        WORLD_MAP_LOCAL("gui.xaeroplus.waystone_visibility_type.world_map_local"),
        WORLD_MAP_GLOBAL("gui.xaeroplus.waystone_visibility_type.world_map_global");
        private final String translationKey;
        WaystoneWpVisibilityType(final String translationKey) {
            this.translationKey = translationKey;
        }
        @Override
        public String getTranslationKey() {
            return translationKey;
        }
    }
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
        false,
        (v) -> markChunksDirtyInWriteDistance(),
        SettingLocation.WORLD_MAP_MAIN);
    public static final XaeroPlusFloatSetting transparentObsidianRoofYSetting = XaeroPlusFloatSetting.create(
        "Roof Y Level",
        "setting.world_map.transparent_obsidian_roof_y",
        "Sets the starting Y level of the roof",
        0, 320, 1,
        250,
        (v) -> markChunksDirtyInWriteDistance(),
        SettingLocation.WORLD_MAP_MAIN);
    public static final XaeroPlusFloatSetting transparentObsidianRoofDarkeningSetting = XaeroPlusFloatSetting.create(
        "Roof Obsidian Opacity",
        "setting.world_map.transparent_obsidian_roof_darkening",
        "setting.world_map.transparent_obsidian_roof_darkening.tooltip",
        0, 255, 5,
        150,
        (v) -> markChunksDirtyInWriteDistance(),
        SettingLocation.WORLD_MAP_MAIN);
    public static final XaeroPlusFloatSetting transparentObsidianRoofSnowOpacitySetting = XaeroPlusFloatSetting.create(
        "Roof Snow Opacity",
        "setting.world_map.transparent_obsidian_roof_snow_opacity",
        "setting.world_map.transparent_obsidian_roof_snow_opacity.tooltip",
        0, 255, 5,
        10,
        (v) -> markChunksDirtyInWriteDistance(),
        SettingLocation.WORLD_MAP_MAIN);
    public static final XaeroPlusBooleanSetting overlayOpacityFix = XaeroPlusBooleanSetting.create(
        "Overlay Opacity Fix",
        "setting.world_map.overlay_opacity_fix",
        "setting.world_map.overlay_opacity_fix.tooltip",
        true,
        SettingLocation.WORLD_MAP_MAIN);
    public static final XaeroPlusFloatSetting worldMapMinZoomSetting = XaeroPlusFloatSetting.create(
        "Min WorldMap Zoom",
        "setting.world_map.min_zoom",
        "setting.world_map.min_zoom.tooltip",
        0, 0.625f, 0.01f,
        0.1f,
        SettingLocation.WORLD_MAP_MAIN);
    public static final XaeroPlusBooleanSetting crossDimensionCursorCoordinates = XaeroPlusBooleanSetting.create(
        "Cross Dim Cursor Coords",
        "setting.world_map.cross_dimension_cursor_coordinates",
        "setting.world_map.cross_dimension_cursor_coordinates.tooltip",
        false,
        SettingLocation.WORLD_MAP_MAIN);
    public static final XaeroPlusBooleanSetting paletteNewChunksEnabledSetting = XaeroPlusBooleanSetting.create(
        "Palette NewChunks",
        "setting.world_map.palette_new_chunks_highlighting",
        "setting.world_map.palette_new_chunks_highlighting.tooltip",
        false,
        (b) -> ModuleManager.getModule(PaletteNewChunks.class).setEnabled(b),
        SettingLocation.CHUNK_HIGHLIGHTS);
    public static final XaeroPlusBooleanSetting paletteNewChunksSaveLoadToDisk = XaeroPlusBooleanSetting.create(
        "Save/Load Palette NewChunks to Disk",
        "setting.world_map.palette_new_chunks_save_load_to_disk",
        "setting.world_map.palette_new_chunks_save_load_to_disk.tooltip",
        true,
        (b) -> ModuleManager.getModule(PaletteNewChunks.class).setDiskCache(b),
        SettingLocation.CHUNK_HIGHLIGHTS);
    public static final XaeroPlusFloatSetting paletteNewChunksAlphaSetting = XaeroPlusFloatSetting.create(
        "Palette NewChunks Opacity",
        "setting.world_map.palette_new_chunks_opacity",
        "setting.world_map.palette_new_chunks_opacity.tooltip",
        0f, 255f, 10f,
        100,
        (b) -> ModuleManager.getModule(PaletteNewChunks.class).setAlpha(b),
        SettingLocation.CHUNK_HIGHLIGHTS);
    public static final XaeroPlusEnumSetting<ColorHelper.HighlightColor> paletteNewChunksColorSetting = XaeroPlusEnumSetting.create(
        "Palette NewChunks Color",
        "setting.world_map.palette_new_chunks_color",
        "setting.world_map.palette_new_chunks_color.tooltip",
        ColorHelper.HighlightColor.values(),
        ColorHelper.HighlightColor.RED,
        (b) -> ModuleManager.getModule(PaletteNewChunks.class).setRgbColor(b.getColor()),
        SettingLocation.CHUNK_HIGHLIGHTS);
    public static final XaeroPlusBooleanSetting paletteNewChunksRenderInverse = XaeroPlusBooleanSetting.create(
        "Palette NewChunks Inverse",
        "setting.world_map.palette_new_chunks_inverse",
        "setting.world_map.palette_new_chunks_inverse.tooltip",
        false,
        (b) -> ModuleManager.getModule(PaletteNewChunks.class).setInverse(b),
        SettingLocation.CHUNK_HIGHLIGHTS);
    public static final XaeroPlusBooleanSetting oldChunksEnabledSetting = XaeroPlusBooleanSetting.create(
        "OldChunks Highlighting",
        "setting.world_map.old_chunks_highlighting",
        "setting.world_map.old_chunks_highlighting.tooltip",
        false,
        (b) -> ModuleManager.getModule(OldChunks.class).setEnabled(b),
        SettingLocation.CHUNK_HIGHLIGHTS);
    public static final XaeroPlusBooleanSetting oldChunksInverse = XaeroPlusBooleanSetting.create(
        "OldChunks Inverse",
        "setting.world_map.old_chunks_inverse",
        "setting.world_map.old_chunks_inverse.tooltip",
        false,
        (b) -> ModuleManager.getModule(OldChunks.class).setInverse(b),
        SettingLocation.CHUNK_HIGHLIGHTS);
    public static final XaeroPlusBooleanSetting oldChunksSaveLoadToDisk = XaeroPlusBooleanSetting.create(
        "Save/Load OldChunks to Disk",
        "setting.world_map.old_chunks_save_load_to_disk",
        "setting.world_map.old_chunks_save_load_to_disk.tooltip",
        true,
        (b) -> ModuleManager.getModule(OldChunks.class).setDiskCache(b),
        SettingLocation.CHUNK_HIGHLIGHTS);
    public static final XaeroPlusFloatSetting oldChunksAlphaSetting = XaeroPlusFloatSetting.create(
        "Old Chunks Opacity",
        "setting.world_map.old_chunks_opacity",
        "setting.world_map.old_chunks_opacity.tooltip",
        0f, 255f, 10f,
        100,
        (b) -> ModuleManager.getModule(OldChunks.class).setAlpha(b),
        SettingLocation.CHUNK_HIGHLIGHTS);
    public static final XaeroPlusEnumSetting<ColorHelper.HighlightColor> oldChunksColorSetting = XaeroPlusEnumSetting.create(
        "Old Chunks Color",
        "setting.world_map.old_chunks_color",
        "setting.world_map.old_chunks_color.tooltip",
        ColorHelper.HighlightColor.values(),
        ColorHelper.HighlightColor.YELLOW,
        (b) -> ModuleManager.getModule(OldChunks.class).setRgbColor(b.getColor()),
        SettingLocation.CHUNK_HIGHLIGHTS);
    public static final XaeroPlusBooleanSetting portalsEnabledSetting = XaeroPlusBooleanSetting.create(
        "Portal Highlights",
        "setting.world_map.portals",
        "setting.world_map.portals.tooltip",
        false,
        (b) -> ModuleManager.getModule(Portals.class).setEnabled(b),
        SettingLocation.CHUNK_HIGHLIGHTS);
    public static final XaeroPlusBooleanSetting portalsSaveLoadToDisk = XaeroPlusBooleanSetting.create(
        "Save/Load Portals to Disk",
        "setting.world_map.portals_save_load_to_disk",
        "setting.world_map.portals_save_load_to_disk.tooltip",
        true,
        (b) -> ModuleManager.getModule(Portals.class).setDiskCache(b),
        SettingLocation.CHUNK_HIGHLIGHTS);
    public static final XaeroPlusFloatSetting portalsAlphaSetting = XaeroPlusFloatSetting.create(
        "Portal Highlights Opacity",
        "setting.world_map.portals_opacity",
        "setting.world_map.portals_opacity.tooltip",
        0f, 255f, 10f,
        100,
        (b) -> ModuleManager.getModule(Portals.class).setAlpha(b),
        SettingLocation.CHUNK_HIGHLIGHTS);
    public static final XaeroPlusEnumSetting<ColorHelper.HighlightColor> portalsColorSetting = XaeroPlusEnumSetting.create(
        "Portal Highlights Color",
        "setting.world_map.portals_color",
        "setting.world_map.portals_color.tooltip",
        ColorHelper.HighlightColor.values(),
        ColorHelper.HighlightColor.MAGENTA,
        (b) -> ModuleManager.getModule(Portals.class).setRgbColor(b.getColor()),
        SettingLocation.CHUNK_HIGHLIGHTS);
    public static final XaeroPlusBooleanSetting oldBiomesSetting = XaeroPlusBooleanSetting.create(
        "Old Biomes",
        "setting.world_map.old_biomes_enabled",
        "setting.world_map.old_biomes_enabled.tooltip",
        false,
        (b) -> ModuleManager.getModule(OldBiomes.class).setEnabled(b),
        SettingLocation.CHUNK_HIGHLIGHTS);
    public static final XaeroPlusBooleanSetting oldBiomesSaveToDiskSetting = XaeroPlusBooleanSetting.create(
        "Save/Load OldBiomes To Disk",
        "setting.world_map.old_biomes_save_load_to_disk",
        "setting.world_map.old_biomes_save_load_to_disk.tooltip",
        true,
        (b) -> ModuleManager.getModule(OldBiomes.class).setDiskCache(b),
        SettingLocation.CHUNK_HIGHLIGHTS);
    public static final XaeroPlusFloatSetting oldBiomesAlphaSetting = XaeroPlusFloatSetting.create(
        "OldBiomes Opacity",
        "setting.world_map.old_biomes_opacity",
        "setting.world_map.old_biomes_opacity.tooltip",
        0f, 255f, 10f,
        100,
        (b) -> ModuleManager.getModule(OldBiomes.class).setAlpha(b),
        SettingLocation.CHUNK_HIGHLIGHTS);
    public static final XaeroPlusEnumSetting<ColorHelper.HighlightColor> oldBiomesColorSetting = XaeroPlusEnumSetting.create(
        "OldBiomes Color",
        "setting.world_map.old_biomes_color",
        "setting.world_map.old_biomes_color.tooltip",
        ColorHelper.HighlightColor.values(),
        ColorHelper.HighlightColor.GREEN,
        (b) -> ModuleManager.getModule(OldBiomes.class).setRgbColor(b.getColor()),
        SettingLocation.CHUNK_HIGHLIGHTS);
    public static final XaeroPlusBooleanSetting liquidNewChunksEnabledSetting = XaeroPlusBooleanSetting.create(
        "NewChunks Highlighting",
        "setting.world_map.new_chunks_highlighting",
        "setting.world_map.new_chunks_highlighting.tooltip",
        false,
        (b) -> ModuleManager.getModule(LiquidNewChunks.class).setEnabled(b),
        SettingLocation.CHUNK_HIGHLIGHTS);
    public static final XaeroPlusBooleanSetting liquidNewChunksSaveLoadToDisk = XaeroPlusBooleanSetting.create(
        "Save/Load NewChunks to Disk",
        "setting.world_map.new_chunks_save_load_to_disk",
        "setting.world_map.new_chunks_save_load_to_disk.tooltip",
        true,
        (b) -> ModuleManager.getModule(LiquidNewChunks.class).setDiskCache(b),
        SettingLocation.CHUNK_HIGHLIGHTS);
    public static final XaeroPlusFloatSetting liquidNewChunksAlphaSetting = XaeroPlusFloatSetting.create(
        "New Chunks Opacity",
        "setting.world_map.new_chunks_opacity",
        "setting.world_map.new_chunks_opacity.tooltip",
        0f, 255f, 10f,
        100,
        (b) -> ModuleManager.getModule(LiquidNewChunks.class).setAlpha(b),
        SettingLocation.CHUNK_HIGHLIGHTS);
    public static final XaeroPlusEnumSetting<ColorHelper.HighlightColor> liquidNewChunksColorSetting = XaeroPlusEnumSetting.create(
        "New Chunks Color",
        "setting.world_map.new_chunks_color",
        "setting.world_map.new_chunks_color.tooltip",
        ColorHelper.HighlightColor.values(),
        ColorHelper.HighlightColor.RED,
        (b) -> ModuleManager.getModule(LiquidNewChunks.class).setRgbColor(b.getColor()),
        SettingLocation.CHUNK_HIGHLIGHTS);
    public static final XaeroPlusBooleanSetting liquidNewChunksInverseHighlightsSetting = XaeroPlusBooleanSetting.create(
        "New Chunks Render Inverse",
        "setting.world_map.new_chunks_inverse_enabled",
        "setting.world_map.new_chunks_inverse_enabled.tooltip",
        false,
        (b) -> ModuleManager.getModule(LiquidNewChunks.class).setInverseRenderEnabled(b),
        SettingLocation.CHUNK_HIGHLIGHTS);
    public static final XaeroPlusEnumSetting<ColorHelper.HighlightColor> liquidNewChunksInverseColorSetting = XaeroPlusEnumSetting.create(
        "New Chunks Inverse Color",
        "setting.world_map.new_chunks_inverse_color",
        "setting.world_map.new_chunks_inverse_color.tooltip",
        ColorHelper.HighlightColor.values(),
        ColorHelper.HighlightColor.GREEN,
        (b) -> ModuleManager.getModule(LiquidNewChunks.class).setInverseRgbColor(b.getColor()),
        SettingLocation.CHUNK_HIGHLIGHTS);
    public static final XaeroPlusBooleanSetting liquidNewChunksOnlyAboveY0Setting = XaeroPlusBooleanSetting.create(
        "Liquid NewChunks Only Y > 0",
        "setting.world_map.new_chunks_only_above_y0",
        "setting.world_map.new_chunks_only_above_y0.tooltip",
        false,
        SettingLocation.CHUNK_HIGHLIGHTS);
    public static final XaeroPlusBooleanSetting worldToolsEnabledSetting = XaeroPlusBooleanSetting.create(
        "WorldTools Highlights",
        "setting.world_map.world_tools",
        "setting.world_map.world_tools.tooltip",
        true,
        (b) -> ModuleManager.getModule(WorldTools.class).setEnabled(b),
        WorldToolsHelper::isWorldToolsPresent,
        SettingLocation.CHUNK_HIGHLIGHTS);
    public static XaeroPlusFloatSetting worldToolsAlphaSetting = XaeroPlusFloatSetting.create(
        "WorldTools Highlights Opacity",
        "setting.world_map.world_tools_opacity",
        "setting.world_map.world_tools_opacity.tooltip",
        0f, 255f, 10f,
        100,
        (b) -> ModuleManager.getModule(WorldTools.class).setAlpha(b),
        WorldToolsHelper::isWorldToolsPresent,
        SettingLocation.CHUNK_HIGHLIGHTS);
    public static final XaeroPlusEnumSetting<ColorHelper.HighlightColor> worldToolsColorSetting = XaeroPlusEnumSetting.create(
        "WorldTools Highlights Color",
        "setting.world_map.world_tools_color",
        "setting.world_map.world_tools_color.tooltip",
        ColorHelper.HighlightColor.values(),
        ColorHelper.HighlightColor.GREEN,
        (b) -> ModuleManager.getModule(WorldTools.class).setRgbColor(b.getColor()),
        WorldToolsHelper::isWorldToolsPresent,
        SettingLocation.CHUNK_HIGHLIGHTS);
    public static final XaeroPlusBooleanSetting portalSkipDetectionEnabledSetting = XaeroPlusBooleanSetting.create(
        "PortalSkip Detection",
        "setting.world_map.portal_skip_detection",
        "setting.world_map.portal_skip_detection.tooltip",
        false,
        (b) -> ModuleManager.getModule(PortalSkipDetection.class).setEnabled(b),
        SettingLocation.CHUNK_HIGHLIGHTS);
    public static final XaeroPlusFloatSetting portalSkipDetectionAlphaSetting = XaeroPlusFloatSetting.create(
        "PortalSkip Opacity",
        "setting.world_map.portal_skip_opacity",
        "setting.world_map.portal_skip_opacity.tooltip",
        0f, 255f, 10f,
        100,
        (b) -> ModuleManager.getModule(PortalSkipDetection.class).setAlpha(b),
        SettingLocation.CHUNK_HIGHLIGHTS);
    public static final XaeroPlusEnumSetting<ColorHelper.HighlightColor> portalSkipDetectionColorSetting = XaeroPlusEnumSetting.create(
        "PortalSkip Color",
        "setting.world_map.portal_skip_color",
        "setting.world_map.portal_skip_color.tooltip",
        ColorHelper.HighlightColor.values(),
        ColorHelper.HighlightColor.WHITE,
        (b) -> ModuleManager.getModule(PortalSkipDetection.class).setRgbColor(b.getColor()),
        SettingLocation.CHUNK_HIGHLIGHTS);
    public static final XaeroPlusFloatSetting portalSkipPortalRadius = XaeroPlusFloatSetting.create(
        "PortalSkip Portal Radius",
        "setting.world_map.portal_skip_portal_radius",
        "setting.world_map.portal_skip_portal_radius.tooltip",
        0, 32, 1,
        15,
        (b) -> ModuleManager.getModule(PortalSkipDetection.class).setPortalRadius(b),
        SettingLocation.CHUNK_HIGHLIGHTS);
    public static final XaeroPlusFloatSetting portalSkipDetectionSearchDelayTicksSetting = XaeroPlusFloatSetting.create(
        "PortalSkip Search Delay",
        "setting.world_map.portal_skip_search_delay",
        "setting.world_map.portal_skip_search_delay.tooltip",
        0, 100, 1,
        10,
        (b) -> ModuleManager.getModule(PortalSkipDetection.class).setSearchDelayTicks(b),
        SettingLocation.CHUNK_HIGHLIGHTS);
    public static final XaeroPlusBooleanSetting portalSkipNewChunksSetting = XaeroPlusBooleanSetting.create(
        "PortalSkip NewChunks",
        "setting.world_map.portal_skip_new_chunks",
        "setting.world_map.portal_skip_new_chunks.tooltip",
        true,
        (b) -> ModuleManager.getModule(PortalSkipDetection.class).setNewChunks(b),
        SettingLocation.CHUNK_HIGHLIGHTS);
    public static final XaeroPlusBooleanSetting portalSkipOldChunkInverseSetting = XaeroPlusBooleanSetting.create(
        "PortalSkip OldChunks Inverse",
        "setting.world_map.portal_skip_old_chunks_inverse",
        "setting.world_map.portal_skip_old_chunks_inverse.tooltip",
        true,
        (b) -> ModuleManager.getModule(PortalSkipDetection.class).setOldChunksInverse(b),
        SettingLocation.CHUNK_HIGHLIGHTS);
    public static final XaeroPlusBooleanSetting highwayHighlightsSetting = XaeroPlusBooleanSetting.create(
        "2b2t Highways",
        "setting.world_map.2b2t_highways_enabled",
        "setting.world_map.2b2t_highways_enabled.tooltip",
        false,
        (b) -> ModuleManager.getModule(Highways.class).setEnabled(b),
        SettingLocation.CHUNK_HIGHLIGHTS);
    public static final XaeroPlusEnumSetting<ColorHelper.HighlightColor> highwaysColorSetting = XaeroPlusEnumSetting.create(
        "2b2t Highways Color",
        "setting.world_map.2b2t_highways_color",
        "setting.world_map.2b2t_highways_color.tooltip",
        ColorHelper.HighlightColor.values(),
        ColorHelper.HighlightColor.BLUE,
        (b) -> ModuleManager.getModule(Highways.class).setRgbColor(b.getColor()),
        SettingLocation.CHUNK_HIGHLIGHTS);
    public static final XaeroPlusFloatSetting highwaysColorAlphaSetting = XaeroPlusFloatSetting.create(
        "2b2t Highways Opacity",
        "setting.world_map.2b2t_highways_opacity",
        "setting.world_map.2b2t_highways_opacity.tooltip",
        0f, 255f, 10f,
        100,
        (b) -> ModuleManager.getModule(Highways.class).setAlpha(b),
        SettingLocation.CHUNK_HIGHLIGHTS);
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
        Globals::setNullOverworldDimFolderIfAble,
        SettingLocation.WORLD_MAP_MAIN);
    public static final XaeroPlusEnumSetting<DataFolderResolutionMode> dataFolderResolutionMode = XaeroPlusEnumSetting.create(
        "Data Dir Mode",
        "setting.world_map.data_folder_resolution_mode",
        "setting.world_map.data_folder_resolution_mode.tooltip",
        DataFolderResolutionMode.values(),
        DataFolderResolutionMode.IP,
        Globals::setDataFolderResolutionModeIfAble,
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
        SettingLocation.MINIMAP_VIEW);
    public static final XaeroPlusFloatSetting minimapScaleMultiplierSetting = XaeroPlusFloatSetting.create(
        "Minimap Scaling Factor",
        "setting.minimap.minimap_scaling",
        "setting.minimap.minimap_scaling.tooltip",
        1f, 5f, 1f,
        1f,
        (b) -> Globals.shouldResetFBO = true,
        SettingLocation.MINIMAP_VIEW);
    public static final XaeroPlusFloatSetting minimapSizeMultiplierSetting = XaeroPlusFloatSetting.create(
        "Minimap Size Multiplier",
        "setting.minimap_size_multiplier",
        "setting.minimap_size_multiplier.tooltip",
        1f, 4f, 1f,
        1f,
        (b) -> Globals.shouldResetFBO = true,
        SettingLocation.MINIMAP_VIEW);
    public static final XaeroPlusBooleanSetting switchToNetherSetting = XaeroPlusBooleanSetting.create(
        "Switch to Nether",
        "setting.keybinds.switch_to_nether",
        "setting.keybinds.switch_to_nether.tooltip",
        false,
        (b) -> Globals.switchToDimension(NETHER),
        SettingLocation.KEYBINDS);
    public static final XaeroPlusBooleanSetting switchToOverworldSetting = XaeroPlusBooleanSetting.create(
        "Switch to Overworld",
        "setting.keybinds.switch_to_overworld",
        "setting.keybinds.switch_to_overworld.tooltip",
        false,
        (b) -> Globals.switchToDimension(OVERWORLD),
        SettingLocation.KEYBINDS);
    public static final XaeroPlusBooleanSetting switchToEndSetting = XaeroPlusBooleanSetting.create(
        "Switch to End",
        "setting.keybinds.switch_to_end",
        "setting.keybinds.switch_to_end.tooltip",
        false,
        (b) -> Globals.switchToDimension(END),
        SettingLocation.KEYBINDS);
    public static final XaeroPlusBooleanSetting worldMapBaritoneGoalHereKeybindSetting = XaeroPlusBooleanSetting.create(
        "WorldMap Baritone Goal Here",
        "setting.keybinds.world_map_baritone_goal_here",
        "setting.keybinds.world_map_baritone_goal_here.tooltip",
        false,
        SettingLocation.KEYBINDS);
    public static final XaeroPlusBooleanSetting worldMapBaritonePathHereKeybindSetting = XaeroPlusBooleanSetting.create(
        "WorldMap Baritone Path Here",
        "setting.keybinds.world_map_baritone_path_here",
        "setting.keybinds.world_map_baritone_path_here.tooltip",
        false,
        SettingLocation.KEYBINDS);
    public static final XaeroPlusBooleanSetting worldMapBaritoneElytraHereKeybindSetting = XaeroPlusBooleanSetting.create(
        "WorldMap Baritone Elytra Here",
        "setting.keybinds.world_map_baritone_elytra_here",
        "setting.keybinds.world_map_baritone_elytra_here.tooltip",
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
        false,
        SettingLocation.MINIMAP_WAYPOINTS);
    public static final XaeroPlusFloatSetting waypointBeaconScaleMin = XaeroPlusFloatSetting.create(
        "Waypoint Beacon Scale Min",
        "setting.waypoints.waypoint_beacon_scale_min",
        "setting.waypoints.waypoint_beacon_scale_min.tooltip",
        0f, 30f, 1f,
        0f,
        SettingLocation.MINIMAP_WAYPOINTS);
    public static final XaeroPlusFloatSetting waypointBeaconDistanceMin = XaeroPlusFloatSetting.create(
        "Waypoint Beacon Distance Min",
        "setting.waypoints.waypoint_beacon_distance_min",
        "setting.waypoints.waypoint_beacon_distance_min.tooltip",
        0f, 512f, 8f,
        0f,
        SettingLocation.MINIMAP_WAYPOINTS);
    public static final XaeroPlusBooleanSetting waypointEta = XaeroPlusBooleanSetting.create(
        "Waypoint ETA",
        "setting.waypoints.waypoint_eta",
        "setting.waypoints.waypoint_eta.tooltip",
        false,
        SettingLocation.MINIMAP_WAYPOINTS);
    public static final XaeroPlusBooleanSetting disableWaypointSharing = XaeroPlusBooleanSetting.create(
        "Disable Waypoint Sharing",
        "setting.world_map.disable_waypoint_sharing",
        "setting.world_map.disable_waypoint_sharing.tooltip",
        false,
        SettingLocation.MINIMAP_WAYPOINTS);
    public static final XaeroPlusBooleanSetting plainWaypointSharing = XaeroPlusBooleanSetting.create(
        "Plain Waypoint Sharing",
        "setting.world_map.plain_waypoint_sharing",
        "setting.world_map.plain_waypoint_sharing.tooltip",
        false,
        SettingLocation.MINIMAP_WAYPOINTS);
    public static final XaeroPlusBooleanSetting disableReceivingWaypoints = XaeroPlusBooleanSetting.create(
        "Disable Receiving Waypoints",
        "setting.world_map.disable_receiving_waypoints",
        "setting.world_map.disable_receiving_waypoints.tooltip",
        false,
        SettingLocation.MINIMAP_WAYPOINTS);
    public static final XaeroPlusBooleanSetting disableXaeroInternetAccess = XaeroPlusBooleanSetting.create(
        "Disable Xaero Internet Access",
        "setting.world_map.disable_internet",
        "setting.world_map.disable_internet.tooltip",
        false,
        SettingLocation.WORLD_MAP_MAIN);
    public static final XaeroPlusBooleanSetting expandSettingEntries = XaeroPlusBooleanSetting.create(
        "Expanded Setting Entries",
        "setting.world_map.expanded_settings",
        "setting.world_map.expanded_settings.tooltip",
        false,
        SettingLocation.WORLD_MAP_MAIN);
    public static final XaeroPlusBooleanSetting sodiumSettingIntegration = XaeroPlusBooleanSetting.create(
        "Sodium/Embeddium Setting Integration",
        "setting.xaeroplus.sodium_embeddium_integration",
        "setting.xaeroplus.sodium_embeddium_integration.tooltip",
        true,
        SettingLocation.WORLD_MAP_MAIN);
    public static final XaeroPlusBooleanSetting highlightShader = XaeroPlusBooleanSetting.create(
        "Highlight Shader",
        "setting.world_map.highlight_shader",
        "setting.world_map.highlight_shader.tooltip",
        true,
        SettingLocation.CHUNK_HIGHLIGHTS);
}
