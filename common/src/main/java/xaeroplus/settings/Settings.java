package xaeroplus.settings;

import net.minecraft.client.Minecraft;
import xaero.map.WorldMapSession;
import xaeroplus.Globals;
import xaeroplus.module.ModuleManager;
import xaeroplus.module.impl.*;
import xaeroplus.util.BaritoneHelper;
import xaeroplus.util.ColorHelper;
import xaeroplus.util.WaystonesHelper;
import xaeroplus.util.WorldToolsHelper;

import java.io.ByteArrayOutputStream;

import static net.minecraft.world.level.Level.*;

public final class Settings extends SettingRegistry {
    public static final Settings REGISTRY = new Settings();

    private Settings() {}

    /**
     * The order settings are defined here determines the order in the settings GUI's.
     */
    public final BooleanSetting minimapFpsLimiter = register(
        BooleanSetting.create(
            "Minimap FPS Limiter",
            "xaeroplus.setting.fps_limiter",
            false,
            (b) -> ModuleManager.getModule(FpsLimiter.class).setEnabled(b)),
        SettingLocation.MINIMAP_MAIN);
    public final DoubleSetting minimapFpsLimit = register(
        DoubleSetting.create(
            "Minimap FPS Limit",
            "xaeroplus.setting.fps_limiter_limit",
            5, 120, 5,
            60),
        SettingLocation.MINIMAP_MAIN);
    public final BooleanSetting fastMapSetting = register(
        BooleanSetting.create(
            "Fast Mapping",
            "xaeroplus.setting.fast_mapping",
            false),
        SettingLocation.WORLD_MAP_MAIN);
    public final DoubleSetting fastMapWriterDelaySetting = register(
        DoubleSetting.create(
            "Fast Mapping Delay",
            "xaeroplus.setting.fast_mapping_delay",
            10, 1000, 10,
            250),
        SettingLocation.WORLD_MAP_MAIN);
    public final DoubleSetting fastMapMaxTilesPerCycle = register(
        DoubleSetting.create(
            "Fast Mapping Rate Limit",
            "xaeroplus.setting.fast_mapping_rate_limit",
            10, 120, 10,
            25),
        SettingLocation.WORLD_MAP_MAIN);
    public final BooleanSetting fastZipWrite = register(
        BooleanSetting.create(
            "Fast Zip Writes",
            "xaeroplus.setting.fast_zip_writes",
            true,
            (b) -> {
                if (!b) Globals.zipFastByteBuffer = new ByteArrayOutputStream(); // release any existing sized buffer to gc
            }),
        SettingLocation.WORLD_MAP_MAIN);
    public final BooleanSetting writesWhileDimSwitched = register(
        BooleanSetting.create(
            "Region Writes While Dim Switched",
            "xaeroplus.setting.region_write_while_dimension_switched",
            false),
        SettingLocation.WORLD_MAP_MAIN);
    public final BooleanSetting baritoneWaypointSyncSetting = register(
        BooleanSetting.create(
            "Baritone Goal Waypoint",
            "xaeroplus.setting.baritone_waypoint",
            true,
            (b) -> {
                if (BaritoneHelper.isBaritonePresent()) ModuleManager.getModule(BaritoneGoalSync.class).setEnabled(b);
            },
            BaritoneHelper::isBaritonePresent),
        SettingLocation.WORLD_MAP_MAIN);
    public final BooleanSetting waystonesWaypointSyncSetting = register(
        BooleanSetting.create(
            "Waystones Sync",
            "xaeroplus.setting.waystones_sync",
            true,
            (b) -> {
                if (WaystonesHelper.isAnyWaystonesPresent()) ModuleManager.getModule(WaystoneSync.class).setEnabled(b);
            },
            WaystonesHelper::isAnyWaystonesPresent),
        SettingLocation.WORLD_MAP_MAIN);
    public final EnumSetting<ColorHelper.WaystoneColor> waystoneColorSetting = register(
        EnumSetting.create(
            "Waystone Color",
            "xaeroplus.setting.waystone_color",
            ColorHelper.WaystoneColor.values(),
            ColorHelper.WaystoneColor.RANDOM,
            (b) -> {
                if (WaystonesHelper.isAnyWaystonesPresent()) ModuleManager.getModule(WaystoneSync.class).setColor(b);
            },
            WaystonesHelper::isAnyWaystonesPresent),
        SettingLocation.WORLD_MAP_MAIN);
    public final BooleanSetting waystoneWaypointSetSetting = register(
        BooleanSetting.create(
            "Waystone Waypoint Set",
            "xaeroplus.setting.waystone_waypoint_set",
            false,
            (b) -> {
                if (WaystonesHelper.isAnyWaystonesPresent()) ModuleManager.getModule(WaystoneSync.class).setWaypointSet(b);
            },
            WaystonesHelper::isAnyWaystonesPresent),
        SettingLocation.WORLD_MAP_MAIN);
    public final EnumSetting<WaystoneWpVisibilityType> waystoneWaypointVisibilityModeSetting = register(
        EnumSetting.create(
            "Waystone WP Visibility Type",
            "xaeroplus.setting.waystone_visibility_type",
            WaystoneWpVisibilityType.values(),
            WaystoneWpVisibilityType.LOCAL,
            (mode) -> {
                if (WaystonesHelper.isAnyWaystonesPresent()) ModuleManager.getModule(WaystoneSync.class).setVisibilityType(mode.ordinal());
            },
            WaystonesHelper::isAnyWaystonesPresent),
        SettingLocation.WORLD_MAP_MAIN);
    public enum WaystoneWpVisibilityType implements TranslatableSettingEnum {
        // order here must mirror xaero's visibility enum
        LOCAL("xaeroplus.gui.waystone_visibility_type.local"),
        GLOBAL("xaeroplus.gui.waystone_visibility_type.global"),
        WORLD_MAP_LOCAL("xaeroplus.gui.waystone_visibility_type.world_map_local"),
        WORLD_MAP_GLOBAL("xaeroplus.gui.waystone_visibility_type.world_map_global");
        private final String translationKey;
        WaystoneWpVisibilityType(final String translationKey) {
            this.translationKey = translationKey;
        }
        @Override
        public String getTranslationKey() {
            return translationKey;
        }
    }
    public final BooleanSetting persistMapDimensionSwitchSetting = register(
        BooleanSetting.create(
            "Persist Dim Switch",
            "xaeroplus.setting.persist_dimension_switch",
            true),
        SettingLocation.WORLD_MAP_MAIN);
    public final BooleanSetting radarWhileDimensionSwitchedSetting = register(
        BooleanSetting.create(
            "Radar While Dim Switched",
            "xaeroplus.setting.radar_while_dimension_switched",
            true),
        SettingLocation.WORLD_MAP_MAIN);
    static void markChunksDirtyInWriteDistance() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.player != null) {
            WorldMapSession session = WorldMapSession.getCurrentSession();
            if (session != null) {
                session.getMapProcessor().getMapWriter().setDirtyInWriteDistance(mc.player, mc.level);
                session.getMapProcessor().getMapWriter().requestCachedColoursClear();
            }
        }
    }
    public final BooleanSetting transparentObsidianRoofSetting = register(
        BooleanSetting.create(
            "Transparent Obsidian Roof",
            "xaeroplus.setting.transparent_obsidian_roof",
            false,
            (v) -> markChunksDirtyInWriteDistance()),
        SettingLocation.WORLD_MAP_MAIN);
    public final DoubleSetting transparentObsidianRoofYSetting = register(
        DoubleSetting.create(
            "Roof Y Level",
            "xaeroplus.setting.transparent_obsidian_roof_y",
            0, 320, 1,
            250,
            (v) -> markChunksDirtyInWriteDistance()),
        SettingLocation.WORLD_MAP_MAIN);
    public final DoubleSetting transparentObsidianRoofDarkeningSetting = register(
        DoubleSetting.create(
            "Roof Obsidian Opacity",
            "xaeroplus.setting.transparent_obsidian_roof_darkening",
            0, 255, 5,
            150,
            (v) -> markChunksDirtyInWriteDistance()),
        SettingLocation.WORLD_MAP_MAIN);
    public final DoubleSetting transparentObsidianRoofSnowOpacitySetting = register(
        DoubleSetting.create(
            "Roof Snow Opacity",
            "xaeroplus.setting.transparent_obsidian_roof_snow_opacity",
            0, 255, 5,
            10,
            (v) -> markChunksDirtyInWriteDistance()),
        SettingLocation.WORLD_MAP_MAIN);
    public final BooleanSetting overlayOpacityFix = register(
        BooleanSetting.create(
            "Overlay Opacity Fix",
            "xaeroplus.setting.overlay_opacity_fix",
            true),
        SettingLocation.WORLD_MAP_MAIN);
    public final DoubleSetting worldMapMinZoomSetting = register(
        DoubleSetting.create(
            "Min WorldMap Zoom",
            "xaeroplus.setting.min_zoom",
            0, 0.625, 0.01,
            0.1),
        SettingLocation.WORLD_MAP_MAIN);
    public  final BooleanSetting crossDimensionCursorCoordinates = register(
        BooleanSetting.create(
            "Cross Dim Cursor Coords",
            "xaeroplus.setting.cross_dimension_cursor_coordinates",
            false),
        SettingLocation.WORLD_MAP_MAIN);
    public final BooleanSetting paletteNewChunksEnabledSetting = register(
        BooleanSetting.create(
            "Palette NewChunks",
            "xaeroplus.setting.palette_new_chunks_highlighting",
            false,
            (b) -> ModuleManager.getModule(PaletteNewChunks.class).setEnabled(b)),
        SettingLocation.CHUNK_HIGHLIGHTS);
    public final BooleanSetting paletteNewChunksSaveLoadToDisk = register(
        BooleanSetting.create(
            "Save/Load Palette NewChunks to Disk",
            "xaeroplus.setting.palette_new_chunks_save_load_to_disk",
            true,
            (b) -> ModuleManager.getModule(PaletteNewChunks.class).setDiskCache(b)),
        SettingLocation.CHUNK_HIGHLIGHTS);
    public final DoubleSetting paletteNewChunksAlphaSetting = register(
        DoubleSetting.create(
            "Palette NewChunks Opacity",
            "xaeroplus.setting.palette_new_chunks_opacity",
            0, 255, 10,
            100,
            (b) -> ModuleManager.getModule(PaletteNewChunks.class).setAlpha(b)),
        SettingLocation.CHUNK_HIGHLIGHTS);
    public final EnumSetting<ColorHelper.HighlightColor> paletteNewChunksColorSetting = register(
        EnumSetting.create(
            "Palette NewChunks Color",
            "xaeroplus.setting.palette_new_chunks_color",
            ColorHelper.HighlightColor.values(),
            ColorHelper.HighlightColor.RED,
            (b) -> ModuleManager.getModule(PaletteNewChunks.class).setRgbColor(b.getColor())),
        SettingLocation.CHUNK_HIGHLIGHTS);
    public final BooleanSetting paletteNewChunksRenderInverse = register(
        BooleanSetting.create(
            "Palette NewChunks Inverse",
            "xaeroplus.setting.palette_new_chunks_inverse",
            false,
            (b) -> ModuleManager.getModule(PaletteNewChunks.class).setInverse(b)),
        SettingLocation.CHUNK_HIGHLIGHTS);
    public final BooleanSetting oldChunksEnabledSetting = register(
        BooleanSetting.create(
            "OldChunks Highlighting",
            "xaeroplus.setting.old_chunks_highlighting",
            false,
            (b) -> ModuleManager.getModule(OldChunks.class).setEnabled(b)),
        SettingLocation.CHUNK_HIGHLIGHTS);
    public final BooleanSetting oldChunksInverse = register(
        BooleanSetting.create(
            "OldChunks Inverse",
            "xaeroplus.setting.old_chunks_inverse",
            false,
            (b) -> ModuleManager.getModule(OldChunks.class).setInverse(b)),
        SettingLocation.CHUNK_HIGHLIGHTS);
    public final BooleanSetting oldChunksSaveLoadToDisk = register(
        BooleanSetting.create(
            "Save/Load OldChunks to Disk",
            "xaeroplus.setting.old_chunks_save_load_to_disk",
            true,
            (b) -> ModuleManager.getModule(OldChunks.class).setDiskCache(b)),
        SettingLocation.CHUNK_HIGHLIGHTS);
    public final DoubleSetting oldChunksAlphaSetting = register(
        DoubleSetting.create(
            "Old Chunks Opacity",
            "xaeroplus.setting.old_chunks_opacity",
            0, 255, 10,
            100,
            (b) -> ModuleManager.getModule(OldChunks.class).setAlpha(b)),
        SettingLocation.CHUNK_HIGHLIGHTS);
    public final EnumSetting<ColorHelper.HighlightColor> oldChunksColorSetting = register(
        EnumSetting.create(
            "Old Chunks Color",
            "xaeroplus.setting.old_chunks_color",
            ColorHelper.HighlightColor.values(),
            ColorHelper.HighlightColor.YELLOW,
            (b) -> ModuleManager.getModule(OldChunks.class).setRgbColor(b.getColor())),
        SettingLocation.CHUNK_HIGHLIGHTS);
    public final BooleanSetting portalsEnabledSetting = register(
        BooleanSetting.create(
            "Portal Highlights",
            "xaeroplus.setting.portals",
            false,
            (b) -> ModuleManager.getModule(Portals.class).setEnabled(b)),
        SettingLocation.CHUNK_HIGHLIGHTS);
    public final BooleanSetting portalsSaveLoadToDisk = register(
        BooleanSetting.create(
            "Save/Load Portals to Disk",
            "xaeroplus.setting.portals_save_load_to_disk",
            true,
            (b) -> ModuleManager.getModule(Portals.class).setDiskCache(b)),
        SettingLocation.CHUNK_HIGHLIGHTS);
    public final DoubleSetting portalsAlphaSetting = register(
        DoubleSetting.create(
            "Portal Highlights Opacity",
            "xaeroplus.setting.portals_opacity",
            0, 255, 10,
            100,
            (b) -> ModuleManager.getModule(Portals.class).setAlpha(b)),
        SettingLocation.CHUNK_HIGHLIGHTS);
    public final EnumSetting<ColorHelper.HighlightColor> portalsColorSetting = register(
        EnumSetting.create(
            "Portal Highlights Color",
            "xaeroplus.setting.portals_color",
            ColorHelper.HighlightColor.values(),
            ColorHelper.HighlightColor.MAGENTA,
            (b) -> ModuleManager.getModule(Portals.class).setRgbColor(b.getColor())),
        SettingLocation.CHUNK_HIGHLIGHTS);
    public final BooleanSetting oldBiomesSetting = register(
        BooleanSetting.create(
            "Old Biomes",
            "xaeroplus.setting.old_biomes_enabled",
            false,
            (b) -> ModuleManager.getModule(OldBiomes.class).setEnabled(b)),
        SettingLocation.CHUNK_HIGHLIGHTS);
    public final BooleanSetting oldBiomesSaveToDiskSetting = register(
        BooleanSetting.create(
            "Save/Load OldBiomes To Disk",
            "xaeroplus.setting.old_biomes_save_load_to_disk",
            true,
            (b) -> ModuleManager.getModule(OldBiomes.class).setDiskCache(b)),
        SettingLocation.CHUNK_HIGHLIGHTS);
    public final DoubleSetting oldBiomesAlphaSetting = register(
        DoubleSetting.create(
            "OldBiomes Opacity",
            "xaeroplus.setting.old_biomes_opacity",
            0, 255, 10,
            100,
            (b) -> ModuleManager.getModule(OldBiomes.class).setAlpha(b)),
        SettingLocation.CHUNK_HIGHLIGHTS);
    public final EnumSetting<ColorHelper.HighlightColor> oldBiomesColorSetting = register(
        EnumSetting.create(
            "OldBiomes Color",
            "xaeroplus.setting.old_biomes_color",
            ColorHelper.HighlightColor.values(),
            ColorHelper.HighlightColor.GREEN,
            (b) -> ModuleManager.getModule(OldBiomes.class).setRgbColor(b.getColor())),
        SettingLocation.CHUNK_HIGHLIGHTS);
    public final BooleanSetting liquidNewChunksEnabledSetting = register(
        BooleanSetting.create(
            "NewChunks Highlighting",
            "xaeroplus.setting.new_chunks_highlighting",
            false,
            (b) -> ModuleManager.getModule(LiquidNewChunks.class).setEnabled(b)),
        SettingLocation.CHUNK_HIGHLIGHTS);
    public final BooleanSetting liquidNewChunksSaveLoadToDisk = register(
        BooleanSetting.create(
            "Save/Load NewChunks to Disk",
            "xaeroplus.setting.new_chunks_save_load_to_disk",
            true,
            (b) -> ModuleManager.getModule(LiquidNewChunks.class).setDiskCache(b)),
        SettingLocation.CHUNK_HIGHLIGHTS);
    public final DoubleSetting liquidNewChunksAlphaSetting = register(
        DoubleSetting.create(
            "New Chunks Opacity",
            "xaeroplus.setting.new_chunks_opacity",
            0, 255, 10,
            100,
            (b) -> ModuleManager.getModule(LiquidNewChunks.class).setAlpha(b)),
        SettingLocation.CHUNK_HIGHLIGHTS);
    public final EnumSetting<ColorHelper.HighlightColor> liquidNewChunksColorSetting = register(
        EnumSetting.create(
            "New Chunks Color",
            "xaeroplus.setting.new_chunks_color",
            ColorHelper.HighlightColor.values(),
            ColorHelper.HighlightColor.RED,
            (b) -> ModuleManager.getModule(LiquidNewChunks.class).setRgbColor(b.getColor())),
        SettingLocation.CHUNK_HIGHLIGHTS);
    public final BooleanSetting liquidNewChunksInverseHighlightsSetting = register(
        BooleanSetting.create(
            "New Chunks Render Inverse",
            "xaeroplus.setting.new_chunks_inverse_enabled",
            false,
            (b) -> ModuleManager.getModule(LiquidNewChunks.class).setInverseRenderEnabled(b)),
        SettingLocation.CHUNK_HIGHLIGHTS);
    public final EnumSetting<ColorHelper.HighlightColor> liquidNewChunksInverseColorSetting = register(
        EnumSetting.create(
            "New Chunks Inverse Color",
            "xaeroplus.setting.new_chunks_inverse_color",
            ColorHelper.HighlightColor.values(),
            ColorHelper.HighlightColor.GREEN,
            (b) -> ModuleManager.getModule(LiquidNewChunks.class).setInverseRgbColor(b.getColor())),
        SettingLocation.CHUNK_HIGHLIGHTS);
    public final BooleanSetting liquidNewChunksOnlyAboveY0Setting = register(
        BooleanSetting.create(
            "Liquid NewChunks Only Y > 0",
            "xaeroplus.setting.new_chunks_only_above_y0",
            false),
        SettingLocation.CHUNK_HIGHLIGHTS);
    public final BooleanSetting worldToolsEnabledSetting = register(
        BooleanSetting.create(
            "WorldTools Highlights",
            "xaeroplus.setting.world_tools",
            true,
            (b) -> ModuleManager.getModule(WorldTools.class).setEnabled(b),
            WorldToolsHelper::isWorldToolsPresent),
        SettingLocation.CHUNK_HIGHLIGHTS);
    public DoubleSetting worldToolsAlphaSetting = register(
        DoubleSetting.create(
            "WorldTools Highlights Opacity",
            "xaeroplus.setting.world_tools_opacity",
            0, 255, 10,
            100,
            (b) -> ModuleManager.getModule(WorldTools.class).setAlpha(b),
            WorldToolsHelper::isWorldToolsPresent),
        SettingLocation.CHUNK_HIGHLIGHTS);
    public final EnumSetting<ColorHelper.HighlightColor> worldToolsColorSetting = register(
        EnumSetting.create(
            "WorldTools Highlights Color",
            "xaeroplus.setting.world_tools_color",
            ColorHelper.HighlightColor.values(),
            ColorHelper.HighlightColor.GREEN,
            (b) -> ModuleManager.getModule(WorldTools.class).setRgbColor(b.getColor()),
            WorldToolsHelper::isWorldToolsPresent),
        SettingLocation.CHUNK_HIGHLIGHTS);
    public final BooleanSetting portalSkipDetectionEnabledSetting = register(
        BooleanSetting.create(
            "PortalSkip Detection",
            "xaeroplus.setting.portal_skip_detection",
            false,
            (b) -> ModuleManager.getModule(PortalSkipDetection.class).setEnabled(b)),
        SettingLocation.CHUNK_HIGHLIGHTS);
    public final DoubleSetting portalSkipDetectionAlphaSetting = register(
        DoubleSetting.create(
            "PortalSkip Opacity",
            "xaeroplus.setting.portal_skip_opacity",
            0, 255, 10,
            100,
            (b) -> ModuleManager.getModule(PortalSkipDetection.class).setAlpha(b)),
        SettingLocation.CHUNK_HIGHLIGHTS);
    public final EnumSetting<ColorHelper.HighlightColor> portalSkipDetectionColorSetting = register(
        EnumSetting.create(
            "PortalSkip Color",
            "xaeroplus.setting.portal_skip_color",
            ColorHelper.HighlightColor.values(),
            ColorHelper.HighlightColor.WHITE,
            (b) -> ModuleManager.getModule(PortalSkipDetection.class).setRgbColor(b.getColor())),
        SettingLocation.CHUNK_HIGHLIGHTS);
    public final DoubleSetting portalSkipPortalRadius = register(
        DoubleSetting.create(
            "PortalSkip Portal Radius",
            "xaeroplus.setting.portal_skip_portal_radius",
            0, 32, 1,
            15,
            (b) -> ModuleManager.getModule(PortalSkipDetection.class).setPortalRadius(b)),
        SettingLocation.CHUNK_HIGHLIGHTS);
    public final DoubleSetting portalSkipDetectionSearchDelayTicksSetting = register(
        DoubleSetting.create(
            "PortalSkip Search Delay",
            "xaeroplus.setting.portal_skip_search_delay",
            0, 100, 1,
            10,
            (b) -> ModuleManager.getModule(PortalSkipDetection.class).setSearchDelayTicks(b)),
        SettingLocation.CHUNK_HIGHLIGHTS);
    public final BooleanSetting portalSkipNewChunksSetting = register(
        BooleanSetting.create(
            "PortalSkip NewChunks",
            "xaeroplus.setting.portal_skip_new_chunks",
            true,
            (b) -> ModuleManager.getModule(PortalSkipDetection.class).setNewChunks(b)),
        SettingLocation.CHUNK_HIGHLIGHTS);
    public final BooleanSetting portalSkipOldChunkInverseSetting = register(
        BooleanSetting.create(
            "PortalSkip OldChunks Inverse",
            "xaeroplus.setting.portal_skip_old_chunks_inverse",
            true,
            (b) -> ModuleManager.getModule(PortalSkipDetection.class).setOldChunksInverse(b)),
        SettingLocation.CHUNK_HIGHLIGHTS);
    public final BooleanSetting highwayHighlightsSetting = register(
        BooleanSetting.create(
            "2b2t Highways",
            "xaeroplus.setting.2b2t_highways_enabled",
            false,
            (b) -> ModuleManager.getModule(Highways.class).setEnabled(b)),
        SettingLocation.CHUNK_HIGHLIGHTS);
    public final EnumSetting<ColorHelper.HighlightColor> highwaysColorSetting = register(
        EnumSetting.create(
            "2b2t Highways Color",
            "xaeroplus.setting.2b2t_highways_color",
            ColorHelper.HighlightColor.values(),
            ColorHelper.HighlightColor.BLUE,
            (b) -> ModuleManager.getModule(Highways.class).setRgbColor(b.getColor())),
        SettingLocation.CHUNK_HIGHLIGHTS);
    public final DoubleSetting highwaysColorAlphaSetting = register(
        DoubleSetting.create(
            "2b2t Highways Opacity",
            "xaeroplus.setting.2b2t_highways_opacity",
            0, 255, 10,
            100,
            (b) -> ModuleManager.getModule(Highways.class).setAlpha(b)),
        SettingLocation.CHUNK_HIGHLIGHTS);
    public final BooleanSetting owAutoWaypointDimension = register(
        BooleanSetting.create(
            "Prefer Overworld Waypoints",
            "xaeroplus.setting.ow_auto_waypoint_dimension",
            false),
        SettingLocation.WORLD_MAP_MAIN);
    public final BooleanSetting showWaypointDistances = register(
        BooleanSetting.create(
            "Show Waypoint Distances",
            "xaeroplus.setting.show_waypoint_distances",
            true),
        SettingLocation.WORLD_MAP_MAIN);
    public final BooleanSetting showRenderDistanceSetting = register(
        BooleanSetting.create(
            "Show Render Distance",
            "xaeroplus.setting.show_render_distance",
            false),
        SettingLocation.MINIMAP_OVERLAYS);
    public final BooleanSetting showRenderDistanceWorldMapSetting = register(
        BooleanSetting.create(
            "Show Render Distance WorldMap",
            "xaeroplus.setting.show_render_distance_world_map",
            false),
        SettingLocation.MINIMAP_OVERLAYS);
    public final BooleanSetting showWorldBorderSetting = register(
        BooleanSetting.create(
            "Show World Border",
            "xaeroplus.setting.show_world_border",
            false),
        SettingLocation.MINIMAP_OVERLAYS);
    public final BooleanSetting showWorldBorderWorldMapSetting = register(
        BooleanSetting.create(
            "Show World Border WorldMap",
            "xaeroplus.setting.show_world_border_world_map",
            false),
        SettingLocation.MINIMAP_OVERLAYS);
    public final BooleanSetting nullOverworldDimensionFolder = register(
        BooleanSetting.create(
            "null OW Dim Dir",
            "xaeroplus.setting.null_overworld_dimension_folder",
            true,
            Globals::setNullOverworldDimFolderIfAble),
        SettingLocation.WORLD_MAP_MAIN);
    public final EnumSetting<DataFolderResolutionMode> dataFolderResolutionMode = register(
        EnumSetting.create(
            "Data Dir Mode",
            "xaeroplus.setting.data_folder_resolution_mode",
            DataFolderResolutionMode.values(),
            DataFolderResolutionMode.IP,
            Globals::setDataFolderResolutionModeIfAble),
        SettingLocation.WORLD_MAP_MAIN);

    public enum DataFolderResolutionMode implements TranslatableSettingEnum {
        IP("xaeroplus.setting.data_folder_resolution_mode.ip"),
        SERVER_NAME("xaeroplus.setting.data_folder_resolution_mode.server_name"),
        BASE_DOMAIN("xaeroplus.setting.data_folder_resolution_mode.base_domain");

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
            "xaeroplus.setting.transparent_background",
            false),
        SettingLocation.MINIMAP_VIEW);
    public final DoubleSetting minimapScaleMultiplierSetting = register(
        DoubleSetting.create(
            "Minimap Scaling Factor",
            "xaeroplus.setting.minimap_scaling",
            1, 5, 1,
            1,
            (b) -> Globals.shouldResetFBO = true),
        SettingLocation.MINIMAP_VIEW);
    public final DoubleSetting minimapSizeMultiplierSetting = register(
        DoubleSetting.create(
            "Minimap Size Multiplier",
            "xaeroplus.setting.minimap_size_multiplier",
            1, 4, 1,
            1,
            (b) -> Globals.shouldResetFBO = true),
        SettingLocation.MINIMAP_VIEW);
    public final BooleanSetting switchToNetherSetting = register(
        BooleanSetting.create(
            "Switch to Nether",
            "xaeroplus.keybind.switch_to_nether",
            false,
            (b) -> Globals.switchToDimension(NETHER)),
        SettingLocation.KEYBINDS);
    public final BooleanSetting switchToOverworldSetting = register(
        BooleanSetting.create(
            "Switch to Overworld",
            "xaeroplus.keybind.switch_to_overworld",
            false,
            (b) -> Globals.switchToDimension(OVERWORLD)),
        SettingLocation.KEYBINDS);
    public final BooleanSetting switchToEndSetting = register(
        BooleanSetting.create(
            "Switch to End",
            "xaeroplus.keybind.switch_to_end",
            false,
            (b) -> Globals.switchToDimension(END)),
        SettingLocation.KEYBINDS);
    public final BooleanSetting worldMapBaritoneGoalHereKeybindSetting = register(
        BooleanSetting.create(
            "WorldMap Baritone Goal Here",
            "xaeroplus.keybind.world_map_baritone_goal_here",
            false),
        SettingLocation.KEYBINDS);
    public final BooleanSetting worldMapBaritonePathHereKeybindSetting = register(
        BooleanSetting.create(
            "WorldMap Baritone Path Here",
            "xaeroplus.keybind.world_map_baritone_path_here",
            false),
        SettingLocation.KEYBINDS);
    public final BooleanSetting worldMapBaritoneElytraHereKeybindSetting = register(
        BooleanSetting.create(
            "WorldMap Baritone Elytra Here",
            "xaeroplus.keybind.world_map_baritone_elytra_here",
            false),
        SettingLocation.KEYBINDS);
    public final BooleanSetting netherCaveFix = register(
        BooleanSetting.create(
            "Nether Cave Fix",
            "xaeroplus.setting.nether_cave_fix",
            true),
        SettingLocation.WORLD_MAP_MAIN);
    public final BooleanSetting alwaysRenderPlayerWithNameOnRadar = register(
        BooleanSetting.create(
            "Always Render Player Name",
            "xaeroplus.setting.always_render_player_name",
            true),
        SettingLocation.MINIMAP_ENTITY_RADAR);
    public final BooleanSetting alwaysRenderPlayerIconOnRadar = register(
        BooleanSetting.create(
            "Always Render Player Icon",
            "xaeroplus.setting.always_render_player_icon",
            true),
        SettingLocation.MINIMAP_ENTITY_RADAR);
    public final BooleanSetting fixMainEntityDot = register(
        BooleanSetting.create(
            "Fix Main Entity Dot",
            "xaeroplus.setting.fix_main_entity_dot",
            true),
        SettingLocation.MINIMAP_ENTITY_RADAR);
    public final BooleanSetting waypointBeacons = register(
        BooleanSetting.create(
            "Waypoint Beacons",
            "xaeroplus.setting.waypoint_beacons",
            false),
        SettingLocation.MINIMAP_WAYPOINTS);
    public final DoubleSetting waypointBeaconScaleMin = register(
        DoubleSetting.create(
            "Waypoint Beacon Scale Min",
            "xaeroplus.setting.waypoint_beacon_scale_min",
            0, 30, 1,
            0),
        SettingLocation.MINIMAP_WAYPOINTS);
    public final DoubleSetting waypointBeaconDistanceMin = register(
        DoubleSetting.create(
            "Waypoint Beacon Distance Min",
            "xaeroplus.setting.waypoint_beacon_distance_min",
            0, 512, 8,
            0),
        SettingLocation.MINIMAP_WAYPOINTS);
    public final BooleanSetting waypointEta = register(
        BooleanSetting.create(
            "Waypoint ETA",
            "xaeroplus.setting.waypoint_eta",
            false),
        SettingLocation.MINIMAP_WAYPOINTS);
    public final BooleanSetting disableWaypointSharing = register(
        BooleanSetting.create(
            "Disable Waypoint Sharing",
            "xaeroplus.setting.disable_waypoint_sharing",
            false),
        SettingLocation.MINIMAP_WAYPOINTS);
    public final BooleanSetting plainWaypointSharing = register(
        BooleanSetting.create(
            "Plain Waypoint Sharing",
            "xaeroplus.setting.plain_waypoint_sharing",
            false),
        SettingLocation.MINIMAP_WAYPOINTS);
    public final BooleanSetting disableReceivingWaypoints = register(
        BooleanSetting.create(
            "Disable Receiving Waypoints",
            "xaeroplus.setting.disable_receiving_waypoints",
            false),
        SettingLocation.MINIMAP_WAYPOINTS);
    public final BooleanSetting disableXaeroInternetAccess = register(
        BooleanSetting.create(
            "Disable Xaero Internet Access",
            "xaeroplus.setting.disable_internet",
            false),
        SettingLocation.WORLD_MAP_MAIN);
    public final BooleanSetting expandSettingEntries = register(
        BooleanSetting.create(
            "Expanded Setting Entries",
            "xaeroplus.setting.expanded_settings",
            false),
        SettingLocation.WORLD_MAP_MAIN);
    public final BooleanSetting sodiumSettingIntegration = register(
        BooleanSetting.create(
            "Sodium/Embeddium Setting Integration",
            "xaeroplus.setting.sodium_embeddium_integration",
            true),
        SettingLocation.WORLD_MAP_MAIN);
    public final BooleanSetting highlightShader = register(
        BooleanSetting.create(
            "Highlight Shader",
            "xaeroplus.setting.highlight_shader",
            true),
        SettingLocation.CHUNK_HIGHLIGHTS);
}
