package xaeroplus.settings;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import xaero.map.WorldMapSession;
import xaeroplus.Globals;
import xaeroplus.feature.extensions.DrawOrderScreen;
import xaeroplus.feature.extensions.GuiMinimapWaypointTeleportCommandSettings;
import xaeroplus.feature.waypoint.WaypointAPI;
import xaeroplus.feature.waypoint.eta.WaypointEtaManager;
import xaeroplus.module.ModuleManager;
import xaeroplus.module.impl.*;
import xaeroplus.util.BaritoneHelper;
import xaeroplus.util.ColorHelper;

import java.io.ByteArrayOutputStream;
import java.time.Duration;

import static net.minecraft.world.level.Level.*;

public final class Settings extends SettingRegistry {
    public static final Settings REGISTRY = new Settings();

    private Settings() {}

    /**
     * The order settings are defined here determines the order in the settings GUI's.
     */

    /**
     * WorldMap Main
     */
    public final StringSetting drawOrderSetting = register(
        StringSetting.builder()
            .name("Draw Order")
            .translationKey("xaeroplus.setting.draw_order")
            .defaultValue("")
            .onChange((s) -> Globals.drawManager.registry().loadOrder(s))
            .screen((parent, escape, setting) -> new DrawOrderScreen(parent, escape))
            .build(),
        SettingLocation.WORLD_MAP_MAIN);
    public final BooleanSetting transparentWorldmapBackgroundSetting = register(
        BooleanSetting.builder()
            .name("Transparent WorldMap Background")
            .translationKey("xaeroplus.setting.transparent_worldmap_background")
            .defaultValue(false)
            .build(),
        SettingLocation.WORLD_MAP_MAIN);
    public final BooleanSetting fastZipWrite = register(
        BooleanSetting.builder()
            .name("Fast Zip Writes")
            .translationKey("xaeroplus.setting.fast_zip_writes")
            .defaultValue(true)
            .onChange((b) -> {
                if (!b) Globals.zipFastByteBuffer = new ByteArrayOutputStream(); // release any existing sized buffer to gc
            })
            .build(),
        SettingLocation.WORLD_MAP_MAIN);
    public final BooleanSetting writesWhileDimSwitched = register(
        BooleanSetting.builder()
            .name("Region Writes While Dim Switched")
            .translationKey("xaeroplus.setting.region_write_while_dimension_switched")
            .defaultValue(false)
            .build(),
        SettingLocation.WORLD_MAP_MAIN);
    public final BooleanSetting baritoneWaypointSyncSetting = register(
        BooleanSetting.builder()
            .name("Baritone Goal Waypoint")
            .translationKey("xaeroplus.setting.baritone_waypoint")
            .defaultValue(true)
            .onChange((b) -> {
                if (BaritoneHelper.isBaritonePresent()) ModuleManager.getModule(BaritoneGoalSync.class).setEnabled(b);
            })
            .visibleWhen(BaritoneHelper::isBaritonePresent)
            .build(),
        SettingLocation.WORLD_MAP_MAIN);
    public final BooleanSetting spawnPointSetting = register(
        BooleanSetting.builder()
            .name("Spawn Point Waypoint")
            .translationKey("xaeroplus.setting.spawn_point_waypoint")
            .defaultValue(false)
            .onChange((b) -> ModuleManager.getModule(SpawnPoint.class).setEnabled(b))
            .build(),
        SettingLocation.WORLD_MAP_MAIN);
    public final BooleanSetting pearlWaypointsSetting = register(
        BooleanSetting.builder()
            .name("Pearl Waypoints")
            .translationKey("xaeroplus.setting.pearl_waypoints")
            .defaultValue(false)
            .onChange((b) -> ModuleManager.getModule(Pearls.class).setEnabled(b))
            .build(),
        SettingLocation.WORLD_MAP_MAIN);
    public final BooleanSetting persistMapDimensionSwitchSetting = register(
        BooleanSetting.builder()
            .name("Persist Dim Switch")
            .translationKey("xaeroplus.setting.persist_dimension_switch")
            .defaultValue(true)
            .build(),
        SettingLocation.WORLD_MAP_MAIN);
    public final BooleanSetting radarWhileDimensionSwitchedSetting = register(
        BooleanSetting.builder()
            .name("Radar While Dim Switched")
            .translationKey("xaeroplus.setting.radar_while_dimension_switched")
            .defaultValue(true)
            .build(),
        SettingLocation.WORLD_MAP_MAIN);
    static void markChunksDirtyInWriteDistance() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.getCameraEntity() != null && mc.getCameraEntity() instanceof Player player) {
            WorldMapSession session = WorldMapSession.getCurrentSession();
            if (session != null) {
                session.getMapProcessor().getMapWriter().setDirtyInWriteDistance(player, mc.level);
                session.getMapProcessor().getMapWriter().requestCachedColoursClear();
            }
        }
    }
    public final BooleanSetting transparentObsidianRoofSetting = register(
        BooleanSetting.builder()
            .name("Transparent Obsidian Roof")
            .translationKey("xaeroplus.setting.transparent_obsidian_roof")
            .defaultValue(false)
            .onChange((v) -> markChunksDirtyInWriteDistance())
            .build(),
        SettingLocation.WORLD_MAP_MAIN);
    public final DoubleSetting transparentObsidianRoofYSetting = register(
        DoubleSetting.builder()
            .name("Roof Y Level")
            .translationKey("xaeroplus.setting.transparent_obsidian_roof_y")
            .range(0, 320, 1)
            .defaultValue(250)
            .onChange((v) -> markChunksDirtyInWriteDistance())
            .visibleWhen(transparentObsidianRoofSetting::get)
            .build(),
        SettingLocation.WORLD_MAP_MAIN);
    public final DoubleSetting transparentObsidianRoofDarkeningSetting = register(
        DoubleSetting.builder()
            .name("Roof Obsidian Opacity")
            .translationKey("xaeroplus.setting.transparent_obsidian_roof_darkening")
            .range(0, 255, 5)
            .defaultValue(150)
            .onChange((v) -> markChunksDirtyInWriteDistance())
            .visibleWhen(transparentObsidianRoofSetting::get)
            .build(),
        SettingLocation.WORLD_MAP_MAIN);
    public final DoubleSetting transparentObsidianRoofSnowOpacitySetting = register(
        DoubleSetting.builder()
            .name("Roof Snow Opacity")
            .translationKey("xaeroplus.setting.transparent_obsidian_roof_snow_opacity")
            .range(0, 255, 5)
            .defaultValue(10)
            .onChange((v) -> markChunksDirtyInWriteDistance())
            .visibleWhen(transparentObsidianRoofSetting::get)
            .build(),
        SettingLocation.WORLD_MAP_MAIN);
    public  final BooleanSetting crossDimensionCursorCoordinates = register(
        BooleanSetting.builder()
            .name("Cross Dim Cursor Coords")
            .translationKey("xaeroplus.setting.cross_dimension_cursor_coordinates")
            .defaultValue(false)
            .build(),
        SettingLocation.WORLD_MAP_MAIN);
    public final BooleanSetting owAutoWaypointDimension = register(
        BooleanSetting.builder()
            .name("Prefer Overworld Waypoints")
            .translationKey("xaeroplus.setting.ow_auto_waypoint_dimension")
            .defaultValue(false)
            .build(),
        SettingLocation.WORLD_MAP_MAIN);
    public final BooleanSetting trulyUnlimitedWorldMapZoom = register(
        BooleanSetting.builder()
            .name("Truly Unlimited WorldMap Zoom")
            .translationKey("xaeroplus.setting.truly_unlimited_worldmap_zoom")
            .defaultValue(false)
            .build(),
        SettingLocation.WORLD_MAP_MAIN);
    public final BooleanSetting nullOverworldDimensionFolder = register(
        BooleanSetting.builder()
            .name("null OW Dim Dir")
            .translationKey("xaeroplus.setting.null_overworld_dimension_folder")
            .defaultValue(true)
            .onChange(Globals::setNullOverworldDimFolderIfAble)
            .visibleWhen(() -> false)
            .build(),
        SettingLocation.WORLD_MAP_MAIN);
    public final EnumSetting<DataFolderResolutionMode> dataFolderResolutionMode = register(
        EnumSetting.<DataFolderResolutionMode>builder()
            .name("Data Dir Mode")
            .translationKey("xaeroplus.setting.data_folder_resolution_mode")
            .values(DataFolderResolutionMode.values())
            .defaultValue(DataFolderResolutionMode.IP)
            .onChange(Globals::setDataFolderResolutionModeIfAble)
            .build(),
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
    public final BooleanSetting netherCaveFix = register(
        BooleanSetting.builder()
            .name("Nether Cave Fix")
            .translationKey("xaeroplus.setting.nether_cave_fix")
            .defaultValue(true)
            .build(),
        SettingLocation.WORLD_MAP_MAIN);
    public final BooleanSetting disableXaeroInternetAccess = register(
        BooleanSetting.builder()
            .name("Disable Xaero Internet Access")
            .translationKey("xaeroplus.setting.disable_internet")
            .defaultValue(false)
            .build(),
        SettingLocation.WORLD_MAP_MAIN);
    public final BooleanSetting expandSettingEntries = register(
        BooleanSetting.builder()
            .name("Expanded Setting Entries")
            .translationKey("xaeroplus.setting.expanded_settings")
            .defaultValue(false)
            .build(),
        SettingLocation.WORLD_MAP_MAIN);
    public final BooleanSetting teleportFailNotifier = register(
        BooleanSetting.builder()
            .name("Teleport Fail Notifier")
            .translationKey("xaeroplus.setting.teleport_fail_notifier")
            .defaultValue(true)
            .onChange((b) -> ModuleManager.getModule(TeleportFailNotifier.class).setEnabled(b))
            .build(), SettingLocation.WORLD_MAP_MAIN);
    public final DoubleSetting teleportFailNotifierDelay = register(
        DoubleSetting.builder()
            .name("Teleport Fail Delay")
            .translationKey("xaeroplus.setting.teleport_fail_notifier_delay")
            .range(1, 120, 1)
            .defaultValue(30) // 1.5 seconds
            .visibleWhen(() -> ModuleManager.getModule(TeleportFailNotifier.class).isEnabled())
            .build(), SettingLocation.WORLD_MAP_MAIN);
    public final BooleanSetting disableTeleportation = register(
        BooleanSetting.builder()
            .name("Disable Teleportation")
            .translationKey("xaeroplus.setting.disable_teleportation")
            .defaultValue(false)
            .build(),
        SettingLocation.WORLD_MAP_MAIN);
    public final BooleanSetting waypointsListDistanceColumn = register(
        BooleanSetting.builder()
            .name("Waypoints List Distance Column")
            .translationKey("xaeroplus.setting.waypoints_gui_distance_column")
            .defaultValue(false)
            .build(),
        SettingLocation.WORLD_MAP_MAIN);
    public final BooleanSetting sodiumSettingIntegration = register(
        BooleanSetting.builder()
            .name("Sodium/Embeddium Setting Integration")
            .translationKey("xaeroplus.setting.sodium_embeddium_integration")
            .defaultValue(true)
            .build(),
        SettingLocation.WORLD_MAP_MAIN);
    public final BooleanSetting worldMapUIAdditions = register(
        BooleanSetting.builder()
            .name("WorldMap UI Additions")
            .translationKey("xaeroplus.setting.world_map_ui_additions")
            .defaultValue(true)
            .build(),
        SettingLocation.WORLD_MAP_MAIN);
    public final BooleanSetting waypointsListUIAdditions = register(
        BooleanSetting.builder()
            .name("Waypoints List UI Additions")
            .translationKey("xaeroplus.setting.waypoints_list_ui_additions")
            .defaultValue(true)
            .build(),
        SettingLocation.WORLD_MAP_MAIN);
    public final BooleanSetting atomicMoveAndReplace = register(
        BooleanSetting.builder()
            .name("Atomic File Move And Replace")
            .translationKey("Atomic File Move And Replace")
            .defaultValue(true)
            .build(),
        SettingLocation.WORLD_MAP_MAIN);
    public final BooleanSetting optimizeRegionDetectionLookups = register(
        BooleanSetting.builder()
            .name("Optimize Region Detection Lookups")
            .translationKey("Optimize Region Detection Lookups")
            .defaultValue(true)
            .build(),
        SettingLocation.WORLD_MAP_MAIN);

    /**
     * Chunk Highlights
     */

    public final BooleanSetting paletteNewChunksEnabledSetting = register(
        BooleanSetting.builder()
            .name("Palette NewChunks")
            .translationKey("xaeroplus.setting.palette_new_chunks_highlighting")
            .defaultValue(false)
            .keybind()
            .onChange((b) -> ModuleManager.getModule(PaletteNewChunks.class).setEnabled(b))
            .build(),
        SettingLocation.CHUNK_HIGHLIGHTS);
    public final BooleanSetting paletteNewChunksVersionUpgradedChunks = register(
        BooleanSetting.builder()
            .name("Palette NewChunks Version Upgraded")
            .translationKey("xaeroplus.setting.palette_new_chunks_version_upgraded")
            .defaultValue(true)
            .visibleWhen(() -> ModuleManager.getModule(PaletteNewChunks.class).isEnabled())
            .build(),
        SettingLocation.CHUNK_HIGHLIGHTS);
    public final BooleanSetting paletteNewChunksSaveLoadToDisk = register(
        BooleanSetting.builder()
            .name("Save/Load Palette NewChunks to Disk")
            .translationKey("xaeroplus.setting.palette_new_chunks_save_load_to_disk")
            .defaultValue(true)
            .onChange((b) -> ModuleManager.getModule(PaletteNewChunks.class).setDiskCache(b))
            .visibleWhen(() -> ModuleManager.getModule(PaletteNewChunks.class).isEnabled())
            .build(),
        SettingLocation.CHUNK_HIGHLIGHTS);
    public final DoubleSetting paletteNewChunksAlphaSetting = register(
        DoubleSetting.builder()
            .name("Palette NewChunks Opacity")
            .translationKey("xaeroplus.setting.palette_new_chunks_opacity")
            .range(0, 255, 10)
            .defaultValue(100)
            .onChange((b) -> ModuleManager.getModule(PaletteNewChunks.class).setAlpha(b))
            .visibleWhen(() -> ModuleManager.getModule(PaletteNewChunks.class).isEnabled())
            .build(),
        SettingLocation.CHUNK_HIGHLIGHTS);
    public final EnumSetting<ColorHelper.HighlightColor> paletteNewChunksColorSetting = register(
        EnumSetting.<ColorHelper.HighlightColor>builder()
            .name("Palette NewChunks Color")
            .translationKey("xaeroplus.setting.palette_new_chunks_color")
            .values(ColorHelper.HighlightColor.values())
            .defaultValue(ColorHelper.HighlightColor.RED)
            .onChange((b) -> ModuleManager.getModule(PaletteNewChunks.class).setRgbColor(b.getColor()))
            .visibleWhen(() -> ModuleManager.getModule(PaletteNewChunks.class).isEnabled())
            .build(),
        SettingLocation.CHUNK_HIGHLIGHTS);
    public final BooleanSetting paletteNewChunksRenderInverse = register(
        BooleanSetting.builder()
            .name("Palette NewChunks Inverse")
            .translationKey("xaeroplus.setting.palette_new_chunks_inverse")
            .defaultValue(false)
            .onChange((b) -> ModuleManager.getModule(PaletteNewChunks.class).setInverse(b))
            .visibleWhen(() -> ModuleManager.getModule(PaletteNewChunks.class).isEnabled())
            .build(),
        SettingLocation.CHUNK_HIGHLIGHTS);
    public final BooleanSetting paletteNewChunksRescan = register(
        BooleanSetting.builder()
            .name("Palette NewChunks Rescan")
            .translationKey("xaeroplus.setting.palette_new_chunks_rescan")
            .defaultValue(false)
            .onChange((b) -> ModuleManager.getModule(PaletteNewChunks.class).setRescan(b))
            .visibleWhen(() -> ModuleManager.getModule(PaletteNewChunks.class).isEnabled())
            .build(),
        SettingLocation.CHUNK_HIGHLIGHTS);
    public enum PaletteNewChunksRescanAge implements TranslatableSettingEnum {
        ZERO(Duration.ZERO, "xaeroplus.setting.palette_new_chunks_rescan_age.zero"),
        ONE_HOUR(Duration.ofHours(1), "xaeroplus.setting.palette_new_chunks_rescan_age.one_hour"),
        ONE_DAY(Duration.ofDays(1), "xaeroplus.setting.palette_new_chunks_rescan_age.one_day"),
        ONE_WEEK(Duration.ofDays(7), "xaeroplus.setting.palette_new_chunks_rescan_age.one_week");

        private final Duration duration;
        private final String translationKey;
        PaletteNewChunksRescanAge(Duration duration, String translationKey) {
            this.duration = duration;
            this.translationKey = translationKey;
        }
        public Duration getDuration() {
            return duration;
        }
        @Override
        public String getTranslationKey() {
            return translationKey;
        }
    }
    public final EnumSetting<PaletteNewChunksRescanAge> paletteNewChunksMinRescanAge = register(
        EnumSetting.<PaletteNewChunksRescanAge>builder()
            .name("Palette NewChunks Min Rescan Age")
            .translationKey("xaeroplus.setting.palette_new_chunks_min_rescan_age")
            .values(PaletteNewChunksRescanAge.values())
            .defaultValue(PaletteNewChunksRescanAge.ONE_WEEK)
            .onChange((v) -> ModuleManager.getModule(PaletteNewChunks.class).setMinRescanAge(v.getDuration()))
            .visibleWhen(() -> ModuleManager.getModule(PaletteNewChunks.class).isEnabled() && paletteNewChunksRescan.get())
            .build(),
        SettingLocation.CHUNK_HIGHLIGHTS);
    public final BooleanSetting oldChunksEnabledSetting = register(
        BooleanSetting.builder()
            .name("OldChunks Highlighting")
            .translationKey("xaeroplus.setting.old_chunks_highlighting")
            .defaultValue(false)
            .keybind()
            .onChange((b) -> ModuleManager.getModule(OldChunks.class).setEnabled(b))
            .build(),
        SettingLocation.CHUNK_HIGHLIGHTS);
    public final BooleanSetting oldChunksInverse = register(
        BooleanSetting.builder()
            .name("OldChunks Inverse")
            .translationKey("xaeroplus.setting.old_chunks_inverse")
            .defaultValue(false)
            .onChange((b) -> ModuleManager.getModule(OldChunks.class).setInverse(b))
            .visibleWhen(() -> ModuleManager.getModule(OldChunks.class).isEnabled())
            .build(),
        SettingLocation.CHUNK_HIGHLIGHTS);
    public final BooleanSetting oldChunksSaveLoadToDisk = register(
        BooleanSetting.builder()
            .name("Save/Load OldChunks to Disk")
            .translationKey("xaeroplus.setting.old_chunks_save_load_to_disk")
            .defaultValue(true)
            .onChange((b) -> ModuleManager.getModule(OldChunks.class).setDiskCache(b))
            .visibleWhen(() -> ModuleManager.getModule(OldChunks.class).isEnabled())
            .build(),
        SettingLocation.CHUNK_HIGHLIGHTS);
    public final DoubleSetting oldChunksAlphaSetting = register(
        DoubleSetting.builder()
            .name("Old Chunks Opacity")
            .translationKey("xaeroplus.setting.old_chunks_opacity")
            .range(0, 255, 10)
            .defaultValue(100)
            .onChange((b) -> ModuleManager.getModule(OldChunks.class).setAlpha(b))
            .visibleWhen(() -> ModuleManager.getModule(OldChunks.class).isEnabled())
            .build(),
        SettingLocation.CHUNK_HIGHLIGHTS);
    public final EnumSetting<ColorHelper.HighlightColor> oldChunksColorSetting = register(
        EnumSetting.<ColorHelper.HighlightColor>builder()
            .name("Old Chunks Color")
            .translationKey("xaeroplus.setting.old_chunks_color")
            .values(ColorHelper.HighlightColor.values())
            .defaultValue(ColorHelper.HighlightColor.YELLOW)
            .onChange((b) -> ModuleManager.getModule(OldChunks.class).setRgbColor(b.getColor()))
            .visibleWhen(() -> ModuleManager.getModule(OldChunks.class).isEnabled())
            .build(),
        SettingLocation.CHUNK_HIGHLIGHTS);
    public final BooleanSetting portalsEnabledSetting = register(
        BooleanSetting.builder()
            .name("Portal Highlights")
            .translationKey("xaeroplus.setting.portals")
            .defaultValue(false)
            .keybind()
            .onChange((b) -> ModuleManager.getModule(Portals.class).setEnabled(b))
            .build(),
        SettingLocation.CHUNK_HIGHLIGHTS);
    public final BooleanSetting portalsSaveLoadToDisk = register(
        BooleanSetting.builder()
            .name("Save/Load Portals to Disk")
            .translationKey("xaeroplus.setting.portals_save_load_to_disk")
            .defaultValue(true)
            .onChange((b) -> ModuleManager.getModule(Portals.class).setDiskCache(b))
            .visibleWhen(() -> ModuleManager.getModule(Portals.class).isEnabled())
            .build(),
        SettingLocation.CHUNK_HIGHLIGHTS);
    public final DoubleSetting portalsAlphaSetting = register(
        DoubleSetting.builder()
            .name("Portal Highlights Opacity")
            .translationKey("xaeroplus.setting.portals_opacity")
            .range(0, 255, 10)
            .defaultValue(100)
            .onChange((b) -> ModuleManager.getModule(Portals.class).setAlpha(b))
            .visibleWhen(() -> ModuleManager.getModule(Portals.class).isEnabled())
            .build(),
        SettingLocation.CHUNK_HIGHLIGHTS);
    public final EnumSetting<ColorHelper.HighlightColor> portalsColorSetting = register(
        EnumSetting.<ColorHelper.HighlightColor>builder()
            .name("Portal Highlights Color")
            .translationKey("xaeroplus.setting.portals_color")
            .values(ColorHelper.HighlightColor.values())
            .defaultValue(ColorHelper.HighlightColor.MAGENTA)
            .onChange((b) -> ModuleManager.getModule(Portals.class).setRgbColor(b.getColor()))
            .visibleWhen(() -> ModuleManager.getModule(Portals.class).isEnabled())
            .build(),
        SettingLocation.CHUNK_HIGHLIGHTS);
    public final BooleanSetting lavaColumnsEnabledSetting = register(
        BooleanSetting.builder()
            .name("Lava Columns")
            .translationKey("xaeroplus.setting.lava_columns")
            .defaultValue(false)
            .keybind()
            .onChange((b) -> ModuleManager.getModule(LavaColumns.class).setEnabled(b))
            .build(),
        SettingLocation.CHUNK_HIGHLIGHTS);
    public final DoubleSetting lavaColumnsMinHeight = register(
        DoubleSetting.builder()
            .name("Min Lava Column Height")
            .translationKey("xaeroplus.setting.lava_columns_min_height")
            .range(0, 20, 1)
            .defaultValue(5)
            .onChange((b) -> ModuleManager.getModule(LavaColumns.class).setMinColumnHeight((int) b))
            .visibleWhen(() -> ModuleManager.getModule(LavaColumns.class).isEnabled())
            .build(),
        SettingLocation.CHUNK_HIGHLIGHTS);
    public final DoubleSetting lavaColumnsAlphaShift = register(
        DoubleSetting.builder()
            .name("Lava Columns Base Alpha Shift")
            .translationKey("xaeroplus.setting.lava_columns_alpha_shift")
            .range(-200, 200, 1)
            .defaultValue(0)
            .onChange((b) -> ModuleManager.getModule(LavaColumns.class).setAlphaShift((int) b))
            .visibleWhen(() -> ModuleManager.getModule(LavaColumns.class).isEnabled())
            .build(),
        SettingLocation.CHUNK_HIGHLIGHTS);
    public final DoubleSetting lavaColumnsAlphaStep = register(
        DoubleSetting.builder()
            .name("Lava Columns Alpha Step")
            .translationKey("xaeroplus.setting.lava_columns_alpha_step")
            .range(1, 30, 1)
            .defaultValue(8)
            .onChange((b) -> ModuleManager.getModule(LavaColumns.class).setAlphaStep((int) b))
            .visibleWhen(() -> ModuleManager.getModule(LavaColumns.class).isEnabled())
            .build(),
        SettingLocation.CHUNK_HIGHLIGHTS);
    public final EnumSetting<ColorHelper.HighlightColor> lavaColumnsColor = register(
        EnumSetting.<ColorHelper.HighlightColor>builder()
            .name("Lava Columns Color")
            .translationKey("xaeroplus.setting.lava_columns_color")
            .values(ColorHelper.HighlightColor.values())
            .defaultValue(ColorHelper.HighlightColor.GREEN)
            .onChange((b) -> ModuleManager.getModule(LavaColumns.class).setRgbColor(b.getColor()))
            .visibleWhen(() -> ModuleManager.getModule(LavaColumns.class).isEnabled())
            .build(),
        SettingLocation.CHUNK_HIGHLIGHTS);
    public final BooleanSetting lavaColumnsSaveLoadToDisk = register(
        BooleanSetting.builder()
            .name("Save/Load Lava Columns to Disk")
            .translationKey("xaeroplus.setting.lava_columns_save_load_to_disk")
            .defaultValue(true)
            .onChange((b) -> ModuleManager.getModule(LavaColumns.class).setDiskCache(b))
            .visibleWhen(() -> ModuleManager.getModule(LavaColumns.class).isEnabled())
            .build(),
        SettingLocation.CHUNK_HIGHLIGHTS);
    public final BooleanSetting oldBiomesSetting = register(
        BooleanSetting.builder()
            .name("Old Biomes")
            .translationKey("xaeroplus.setting.old_biomes_enabled")
            .defaultValue(false)
            .keybind()
            .onChange((b) -> ModuleManager.getModule(OldBiomes.class).setEnabled(b))
            .build(),
        SettingLocation.CHUNK_HIGHLIGHTS);
    public final BooleanSetting oldBiomesSaveToDiskSetting = register(
        BooleanSetting.builder()
            .name("Save/Load OldBiomes To Disk")
            .translationKey("xaeroplus.setting.old_biomes_save_load_to_disk")
            .defaultValue(true)
            .onChange((b) -> ModuleManager.getModule(OldBiomes.class).setDiskCache(b))
            .visibleWhen(() -> ModuleManager.getModule(OldBiomes.class).isEnabled())
            .build(),
        SettingLocation.CHUNK_HIGHLIGHTS);
    public final DoubleSetting oldBiomesAlphaSetting = register(
        DoubleSetting.builder()
            .name("OldBiomes Opacity")
            .translationKey("xaeroplus.setting.old_biomes_opacity")
            .range(0, 255, 10)
            .defaultValue(100)
            .onChange((b) -> ModuleManager.getModule(OldBiomes.class).setAlpha(b))
            .visibleWhen(() -> ModuleManager.getModule(OldBiomes.class).isEnabled())
            .build(),
        SettingLocation.CHUNK_HIGHLIGHTS);
    public final EnumSetting<ColorHelper.HighlightColor> oldBiomesColorSetting = register(
        EnumSetting.<ColorHelper.HighlightColor>builder()
            .name("OldBiomes Color")
            .translationKey("xaeroplus.setting.old_biomes_color")
            .values(ColorHelper.HighlightColor.values())
            .defaultValue(ColorHelper.HighlightColor.GREEN)
            .onChange((b) -> ModuleManager.getModule(OldBiomes.class).setRgbColor(b.getColor()))
            .visibleWhen(() -> ModuleManager.getModule(OldBiomes.class).isEnabled())
            .build(),
        SettingLocation.CHUNK_HIGHLIGHTS);
    public final BooleanSetting liquidNewChunksEnabledSetting = register(
        BooleanSetting.builder()
            .name("NewChunks Highlighting")
            .translationKey("xaeroplus.setting.new_chunks_highlighting")
            .defaultValue(false)
            .keybind()
            .onChange((b) -> ModuleManager.getModule(LiquidNewChunks.class).setEnabled(b))
            .build(),
        SettingLocation.CHUNK_HIGHLIGHTS);
    public final BooleanSetting liquidNewChunksSaveLoadToDisk = register(
        BooleanSetting.builder()
            .name("Save/Load NewChunks to Disk")
            .translationKey("xaeroplus.setting.new_chunks_save_load_to_disk")
            .defaultValue(true)
            .onChange((b) -> ModuleManager.getModule(LiquidNewChunks.class).setDiskCache(b))
            .visibleWhen(() -> ModuleManager.getModule(LiquidNewChunks.class).isEnabled())
            .build(),
        SettingLocation.CHUNK_HIGHLIGHTS);
    public final DoubleSetting liquidNewChunksAlphaSetting = register(
        DoubleSetting.builder()
            .name("New Chunks Opacity")
            .translationKey("xaeroplus.setting.new_chunks_opacity")
            .range(0, 255, 10)
            .defaultValue(100)
            .onChange((b) -> ModuleManager.getModule(LiquidNewChunks.class).setAlpha(b))
            .visibleWhen(() -> ModuleManager.getModule(LiquidNewChunks.class).isEnabled())
            .build(),
        SettingLocation.CHUNK_HIGHLIGHTS);
    public final EnumSetting<ColorHelper.HighlightColor> liquidNewChunksColorSetting = register(
        EnumSetting.<ColorHelper.HighlightColor>builder()
            .name("New Chunks Color")
            .translationKey("xaeroplus.setting.new_chunks_color")
            .values(ColorHelper.HighlightColor.values())
            .defaultValue(ColorHelper.HighlightColor.RED)
            .onChange((b) -> ModuleManager.getModule(LiquidNewChunks.class).setRgbColor(b.getColor()))
            .visibleWhen(() -> ModuleManager.getModule(LiquidNewChunks.class).isEnabled())
            .build(),
        SettingLocation.CHUNK_HIGHLIGHTS);
    public final BooleanSetting liquidNewChunksInverseHighlightsSetting = register(
        BooleanSetting.builder()
            .name("New Chunks Render Inverse")
            .translationKey("xaeroplus.setting.new_chunks_inverse_enabled")
            .defaultValue(false)
            .onChange((b) -> ModuleManager.getModule(LiquidNewChunks.class).setInverseRenderEnabled(b))
            .visibleWhen(() -> ModuleManager.getModule(LiquidNewChunks.class).isEnabled())
            .build(),
        SettingLocation.CHUNK_HIGHLIGHTS);
    public final EnumSetting<ColorHelper.HighlightColor> liquidNewChunksInverseColorSetting = register(
        EnumSetting.<ColorHelper.HighlightColor>builder()
            .name("New Chunks Inverse Color")
            .translationKey("xaeroplus.setting.new_chunks_inverse_color")
            .values(ColorHelper.HighlightColor.values())
            .defaultValue(ColorHelper.HighlightColor.GREEN)
            .onChange((b) -> ModuleManager.getModule(LiquidNewChunks.class).setInverseRgbColor(b.getColor()))
            .visibleWhen(() -> ModuleManager.getModule(LiquidNewChunks.class).isEnabled())
            .build(),
        SettingLocation.CHUNK_HIGHLIGHTS);
    public final BooleanSetting liquidNewChunksOnlyAboveY0Setting = register(
        BooleanSetting.builder()
            .name("Liquid NewChunks Only Y > 0")
            .translationKey("xaeroplus.setting.new_chunks_only_above_y0")
            .defaultValue(false)
            .visibleWhen(() -> ModuleManager.getModule(LiquidNewChunks.class).isEnabled())
            .build(),
        SettingLocation.CHUNK_HIGHLIGHTS);
    public final BooleanSetting portalSkipDetectionEnabledSetting = register(
        BooleanSetting.builder()
            .name("PortalSkip Detection")
            .translationKey("xaeroplus.setting.portal_skip_detection")
            .defaultValue(false)
            .keybind()
            .onChange((b) -> ModuleManager.getModule(PortalSkipDetection.class).setEnabled(b))
            .build(),
        SettingLocation.CHUNK_HIGHLIGHTS);
    public final DoubleSetting portalSkipDetectionAlphaSetting = register(
        DoubleSetting.builder()
            .name("PortalSkip Opacity")
            .translationKey("xaeroplus.setting.portal_skip_opacity")
            .range(0, 255, 10)
            .defaultValue(100)
            .onChange((b) -> ModuleManager.getModule(PortalSkipDetection.class).setAlpha(b))
            .visibleWhen(() -> ModuleManager.getModule(PortalSkipDetection.class).isEnabled())
            .build(),
        SettingLocation.CHUNK_HIGHLIGHTS);
    public final EnumSetting<ColorHelper.HighlightColor> portalSkipDetectionColorSetting = register(
        EnumSetting.<ColorHelper.HighlightColor>builder()
            .name("PortalSkip Color")
            .translationKey("xaeroplus.setting.portal_skip_color")
            .values(ColorHelper.HighlightColor.values())
            .defaultValue(ColorHelper.HighlightColor.WHITE)
            .onChange((b) -> ModuleManager.getModule(PortalSkipDetection.class).setRgbColor(b.getColor()))
            .visibleWhen(() -> ModuleManager.getModule(PortalSkipDetection.class).isEnabled())
            .build(),
        SettingLocation.CHUNK_HIGHLIGHTS);
    public final DoubleSetting portalSkipPortalRadius = register(
        DoubleSetting.builder()
            .name("PortalSkip Portal Radius")
            .translationKey("xaeroplus.setting.portal_skip_portal_radius")
            .range(0, 32, 1)
            .defaultValue(15)
            .onChange((b) -> ModuleManager.getModule(PortalSkipDetection.class).setPortalRadius(b))
            .visibleWhen(() -> ModuleManager.getModule(PortalSkipDetection.class).isEnabled())
            .build(),
        SettingLocation.CHUNK_HIGHLIGHTS);
    public final DoubleSetting portalSkipDetectionSearchDelayTicksSetting = register(
        DoubleSetting.builder()
            .name("PortalSkip Search Delay")
            .translationKey("xaeroplus.setting.portal_skip_search_delay")
            .range(0, 100, 1)
            .defaultValue(10)
            .onChange((b) -> ModuleManager.getModule(PortalSkipDetection.class).setSearchDelayTicks(b))
            .visibleWhen(() -> ModuleManager.getModule(PortalSkipDetection.class).isEnabled())
            .build(),
        SettingLocation.CHUNK_HIGHLIGHTS);
    public final BooleanSetting portalSkipNewChunksSetting = register(
        BooleanSetting.builder()
            .name("PortalSkip NewChunks")
            .translationKey("xaeroplus.setting.portal_skip_new_chunks")
            .defaultValue(true)
            .onChange((b) -> ModuleManager.getModule(PortalSkipDetection.class).setNewChunks(b))
            .visibleWhen(() -> ModuleManager.getModule(PortalSkipDetection.class).isEnabled())
            .build(),
        SettingLocation.CHUNK_HIGHLIGHTS);
    public final BooleanSetting portalSkipOldChunkInverseSetting = register(
        BooleanSetting.builder()
            .name("PortalSkip OldChunks Inverse")
            .translationKey("xaeroplus.setting.portal_skip_old_chunks_inverse")
            .defaultValue(true)
            .onChange((b) -> ModuleManager.getModule(PortalSkipDetection.class).setOldChunksInverse(b))
            .visibleWhen(() -> ModuleManager.getModule(PortalSkipDetection.class).isEnabled())
            .build(),
        SettingLocation.CHUNK_HIGHLIGHTS);
    public final BooleanSetting breadcrumbsEnabledSetting = register(
        BooleanSetting.builder()
            .name("Breadcrumbs")
            .translationKey("xaeroplus.setting.breadcrumbs")
            .defaultValue(false)
            .onChange((b) -> ModuleManager.getModule(Breadcrumbs.class).setEnabled(b))
            .build(),
        SettingLocation.CHUNK_HIGHLIGHTS);
    public final BooleanSetting breadcrumbsSaveLoadToDiskSetting = register(
        BooleanSetting.builder()
            .name("Save/Load Breadcrumbs to Disk")
            .translationKey("xaeroplus.setting.breadcrumbs_save_load_to_disk")
            .defaultValue(true)
            .onChange((b) -> ModuleManager.getModule(Breadcrumbs.class).setDiskCache(b))
            .visibleWhen(() -> ModuleManager.getModule(Breadcrumbs.class).isEnabled())
            .build(),
        SettingLocation.CHUNK_HIGHLIGHTS);
    public enum BreadcrumbsMode implements TranslatableSettingEnum {
        CHUNK_RADIUS("xaeroplus.setting.breadcrumbs_mode.chunk_radius"),
        SEEN_CHUNKS("xaeroplus.setting.breadcrumbs_mode.seen_chunks");
        private final String translationKey;
        BreadcrumbsMode(final String translationKey) {
            this.translationKey = translationKey;
        }
        @Override
        public String getTranslationKey() {
            return translationKey;
        }
    }
    public final EnumSetting<BreadcrumbsMode> breadcrumbsModeSetting = register(
        EnumSetting.<BreadcrumbsMode>builder()
            .name("Breadcrumbs Mode")
            .translationKey("xaeroplus.setting.breadcrumbs_mode")
            .values(BreadcrumbsMode.values())
            .defaultValue(BreadcrumbsMode.CHUNK_RADIUS)
            .build(), SettingLocation.CHUNK_HIGHLIGHTS);
    public final DoubleSetting breadcrumbsChunkRadiusSetting = register(
        DoubleSetting.builder()
            .name("Breadcrumbs Chunk Radius")
            .translationKey("xaeroplus.setting.breadcrumbs_chunk_radius")
            .range(0, 16, 1)
            .defaultValue(0)
            .onChange((d) -> ModuleManager.getModule(Breadcrumbs.class).setChunkRadius(d))
            .visibleWhen(() -> ModuleManager.getModule(Breadcrumbs.class).isEnabled() && breadcrumbsModeSetting.get() == BreadcrumbsMode.CHUNK_RADIUS)
            .build(),
        SettingLocation.CHUNK_HIGHLIGHTS);
    public final EnumSetting<ColorHelper.HighlightColor> breadcrumbsColorSetting = register(
        EnumSetting.<ColorHelper.HighlightColor>builder()
            .name("Breadcrumbs Color")
            .translationKey("xaeroplus.setting.breadcrumbs_color")
            .values(ColorHelper.HighlightColor.values())
            .defaultValue(ColorHelper.HighlightColor.CYAN)
            .onChange((b) -> ModuleManager.getModule(Breadcrumbs.class).setRgbColor(b.getColor()))
            .visibleWhen(() -> ModuleManager.getModule(Breadcrumbs.class).isEnabled())
            .build(),
        SettingLocation.CHUNK_HIGHLIGHTS);
    public final DoubleSetting breadcrumbsOpacitySetting = register(
        DoubleSetting.builder()
            .name("Breadcrumbs Opacity")
            .translationKey("xaeroplus.setting.breadcrumbs_opacity")
            .range(0, 255, 10)
            .defaultValue(100)
            .onChange((b) -> ModuleManager.getModule(Breadcrumbs.class).setAlpha(b))
            .visibleWhen(() -> ModuleManager.getModule(Breadcrumbs.class).isEnabled())
            .build(),
        SettingLocation.CHUNK_HIGHLIGHTS);

    /**
     * Overlays
     */

    public final BooleanSetting baritonePathSyncSetting = register(
        BooleanSetting.builder()
            .name("Baritone Path")
            .translationKey("xaeroplus.setting.baritone_path")
            .defaultValue(true)
            .onChange((b) -> {
                if (BaritoneHelper.isBaritonePresent()) ModuleManager.getModule(BaritonePathSync.class).setEnabled(b);
            })
            .visibleWhen(BaritoneHelper::isBaritonePresent)
            .build(),
        SettingLocation.OVERLAYS);
    public final EnumSetting<ColorHelper.HighlightColor> baritonePathSyncColorSetting = register(
        EnumSetting.<ColorHelper.HighlightColor>builder()
            .name("Baritone Path Color")
            .translationKey("xaeroplus.setting.baritone_path_color")
            .values(ColorHelper.HighlightColor.values())
            .defaultValue(ColorHelper.HighlightColor.RED)
            .onChange((b) -> ModuleManager.getModule(BaritonePathSync.class).setColor(b.getColor()))
            .visibleWhen(() -> ModuleManager.getModule(BaritonePathSync.class).isEnabled())
            .build(),
        SettingLocation.OVERLAYS);
    public final DoubleSetting baritonePathSyncOpacity = register(
        DoubleSetting.builder()
            .name("Baritone Path Opacity")
            .translationKey("xaeroplus.setting.baritone_path_opacity")
            .range(0, 255, 5)
            .defaultValue(150)
            .onChange((v) -> ModuleManager.getModule(BaritonePathSync.class).setOpacity((int) v))
            .visibleWhen(() -> ModuleManager.getModule(BaritonePathSync.class).isEnabled())
            .build(),
        SettingLocation.OVERLAYS);
    public final BooleanSetting highwayHighlightsSetting = register(
        BooleanSetting.builder()
            .name("2b2t Highways")
            .translationKey("xaeroplus.setting.2b2t_highways_enabled")
            .defaultValue(false)
            .keybind()
            .onChange((b) -> ModuleManager.getModule(Highways.class).setEnabled(b))
            .build(),
        SettingLocation.OVERLAYS);
    public enum HighwayWidth implements TranslatableSettingEnum {
        // Must be odd numbers for the center to be aligned correctly
        ONE(1), THREE(3), FIVE(5);
        private final int width;
        HighwayWidth(final int width) {
            this.width = width;
        }

        @Override
        public String getTranslationKey() {
            return String.valueOf(width);
        }

        public int getWidth() {
            return width;
        }
    }
    public final EnumSetting<HighwayWidth> highwayWidthSetting = register(
        EnumSetting.<HighwayWidth>builder()
            .name("2b2t Highways Width")
            .translationKey("xaeroplus.setting.2b2t_highways_width")
            .values(HighwayWidth.values())
            .defaultValue(HighwayWidth.ONE)
            .onChange((v) -> ModuleManager.getModule(Highways.class).setWidth(v))
            .visibleWhen(() -> ModuleManager.getModule(Highways.class).isEnabled())
            .build(),
        SettingLocation.OVERLAYS);
    public final EnumSetting<ColorHelper.HighlightColor> highwaysColorSetting = register(
        EnumSetting.<ColorHelper.HighlightColor>builder()
            .name("2b2t Highways Color")
            .translationKey("xaeroplus.setting.2b2t_highways_color")
            .values(ColorHelper.HighlightColor.values())
            .defaultValue(ColorHelper.HighlightColor.BLUE)
            .onChange((b) -> ModuleManager.getModule(Highways.class).setRgbColor(b.getColor()))
            .visibleWhen(() -> ModuleManager.getModule(Highways.class).isEnabled())
            .build(),
        SettingLocation.OVERLAYS);
    public final DoubleSetting highwaysColorAlphaSetting = register(
        DoubleSetting.builder()
            .name("2b2t Highways Opacity")
            .translationKey("xaeroplus.setting.2b2t_highways_opacity")
            .range(0, 255, 10)
            .defaultValue(100)
            .onChange((b) -> ModuleManager.getModule(Highways.class).setAlpha(b))
            .visibleWhen(() -> ModuleManager.getModule(Highways.class).isEnabled())
            .build(),
        SettingLocation.OVERLAYS);
    public final BooleanSetting showRenderDistanceSetting = register(
        BooleanSetting.builder()
            .name("Show Render Distance")
            .translationKey("xaeroplus.setting.show_render_distance")
            .defaultValue(false)
            .keybind()
            .onChange((b) -> ModuleManager.getModule(RenderDistance.class).setEnabled(b))
            .build(),
        SettingLocation.OVERLAYS);
    public final BooleanSetting showWorldBorderSetting = register(
        BooleanSetting.builder()
            .name("Show World Border")
            .translationKey("xaeroplus.setting.show_world_border")
            .defaultValue(false)
            .onChange((b) -> ModuleManager.getModule(WorldBorder.class).setEnabled(b))
            .build(),
        SettingLocation.OVERLAYS);
    public final BooleanSetting spawnChunksEnabledSetting = register(
        BooleanSetting.builder()
            .name("Spawn Chunks")
            .translationKey("xaeroplus.setting.spawn_chunks")
            .defaultValue(false)
            .keybind()
            .onChange((b) -> ModuleManager.getModule(SpawnChunks.class).setEnabled(b))
            .build(),
        SettingLocation.OVERLAYS);
    public final BooleanSetting playerSpawnChunksEnabledSetting = register(
        BooleanSetting.builder()
            .name("Player Spawn Chunks")
            .translationKey("xaeroplus.setting.player_spawn_chunks")
            .defaultValue(false)
            .keybind()
            .onChange((b) -> ModuleManager.getModule(SpawnChunksPlayer.class).setEnabled(b))
            .build(),
        SettingLocation.OVERLAYS);
    public final BooleanSetting spawnChunksRedstoneProcessingEnabled = register(
        BooleanSetting.builder()
            .name("Spawn Chunks Redstone Processing")
            .translationKey("xaeroplus.setting.spawn_chunks_redstone_processing")
            .defaultValue(false)
            .visibleWhen(() -> ModuleManager.getModule(SpawnChunks.class).isEnabled() || ModuleManager.getModule(SpawnChunksPlayer.class).isEnabled())
            .build(),
        SettingLocation.OVERLAYS);
    public final BooleanSetting spawnChunksOuterChunksEnabled = register(
        BooleanSetting.builder()
            .name("Spawn Chunks Outer Chunks")
            .translationKey("xaeroplus.setting.spawn_chunks_outer_chunks")
            .defaultValue(false)
            .visibleWhen(() -> ModuleManager.getModule(SpawnChunks.class).isEnabled() || ModuleManager.getModule(SpawnChunksPlayer.class).isEnabled())
            .build(),
        SettingLocation.OVERLAYS);
    public final EnumSetting<ColorHelper.HighlightColor> spawnChunksEntityProcessingColor = register(
        EnumSetting.<ColorHelper.HighlightColor>builder()
            .name("Spawn Chunks Entity Processing Color")
            .translationKey("xaeroplus.setting.spawn_chunks_entity_processing_color")
            .values(ColorHelper.HighlightColor.values())
            .defaultValue(ColorHelper.HighlightColor.GREEN)
            .onChange((b) -> {
                ModuleManager.getModule(SpawnChunks.class).setEntityProcessingColor(b.getColor());
                ModuleManager.getModule(SpawnChunksPlayer.class).setEntityProcessingColor(b.getColor());
            })
            .visibleWhen(() -> ModuleManager.getModule(SpawnChunks.class).isEnabled() || ModuleManager.getModule(SpawnChunksPlayer.class).isEnabled())
            .build(),
        SettingLocation.OVERLAYS);
    public final EnumSetting<ColorHelper.HighlightColor> spawnChunksRedstoneProcessingColor = register(
        EnumSetting.<ColorHelper.HighlightColor>builder()
            .name("Spawn Chunks Redstone Processing Color")
            .translationKey("xaeroplus.setting.spawn_chunks_redstone_processing_color")
            .values(ColorHelper.HighlightColor.values())
            .defaultValue(ColorHelper.HighlightColor.RED)
            .onChange((b) -> {
                ModuleManager.getModule(SpawnChunks.class).setRedstoneProcessingColor(b.getColor());
                ModuleManager.getModule(SpawnChunksPlayer.class).setRedstoneProcessingColor(b.getColor());
            })
            .visibleWhen(() -> ModuleManager.getModule(SpawnChunks.class).isEnabled() || ModuleManager.getModule(SpawnChunksPlayer.class).isEnabled())
            .build(),
        SettingLocation.OVERLAYS);
    public final EnumSetting<ColorHelper.HighlightColor> spawnChunksLazyChunksColor = register(
        EnumSetting.<ColorHelper.HighlightColor>builder()
            .name("Spawn Chunks Lazy Chunks Color")
            .translationKey("xaeroplus.setting.spawn_chunks_lazy_chunks_color")
            .values(ColorHelper.HighlightColor.values())
            .defaultValue(ColorHelper.HighlightColor.BLUE)
            .onChange((b) -> {
                ModuleManager.getModule(SpawnChunks.class).setLazyChunksColor(b.getColor());
                ModuleManager.getModule(SpawnChunksPlayer.class).setLazyChunksColor(b.getColor());
            })
            .visibleWhen(() -> ModuleManager.getModule(SpawnChunks.class).isEnabled() || ModuleManager.getModule(SpawnChunksPlayer.class).isEnabled())
            .build(),
        SettingLocation.OVERLAYS);
    public final EnumSetting<ColorHelper.HighlightColor> spawnChunksOuterChunksColor = register(
        EnumSetting.<ColorHelper.HighlightColor>builder()
            .name("Spawn Chunks Outer Chunks Color")
            .translationKey("xaeroplus.setting.spawn_chunks_outer_chunks_color")
            .values(ColorHelper.HighlightColor.values())
            .defaultValue(ColorHelper.HighlightColor.YELLOW)
            .onChange((b) -> {
                ModuleManager.getModule(SpawnChunks.class).setOuterChunksColor(b.getColor());
                ModuleManager.getModule(SpawnChunksPlayer.class).setOuterChunksColor(b.getColor());
            })
            .visibleWhen(() -> ModuleManager.getModule(SpawnChunks.class).isEnabled() || ModuleManager.getModule(SpawnChunksPlayer.class).isEnabled())
            .build(),
        SettingLocation.OVERLAYS);
    public final BooleanSetting mapArtGridEnabledSetting = register(
        BooleanSetting.builder()
            .name("Map Art Grid")
            .translationKey("xaeroplus.setting.map_art_grid")
            .defaultValue(false)
            .keybind()
            .onChange((b) -> ModuleManager.getModule(MapArtGrid.class).setEnabled(b))
            .build(),
        SettingLocation.OVERLAYS);
    public final EnumSetting<ColorHelper.HighlightColor> mapArtGridColorSetting = register(
        EnumSetting.<ColorHelper.HighlightColor>builder()
            .name("Map Art Grid Color")
            .translationKey("xaeroplus.setting.map_art_grid_color")
            .values(ColorHelper.HighlightColor.values())
            .defaultValue(ColorHelper.HighlightColor.RED)
            .onChange((b) -> ModuleManager.getModule(MapArtGrid.class).setRgbColor(b.getColor()))
            .visibleWhen(() -> ModuleManager.getModule(MapArtGrid.class).isEnabled())
            .build(),
        SettingLocation.OVERLAYS);
    public final DoubleSetting mapArtGridZoomSetting = register(
        DoubleSetting.builder()
            .name("Map Art Grid Zoom")
            .translationKey("xaeroplus.setting.map_art_grid_zoom")
            .range(0.0, 4.0, 1.0)
            .defaultValue(0.0)
            .onChange((v) -> ModuleManager.getModule(MapArtGrid.class).setZoom((int) v))
            .visibleWhen(() -> ModuleManager.getModule(MapArtGrid.class).isEnabled())
            .build(), SettingLocation.OVERLAYS);
    public final BooleanSetting regionGridEnabledSetting = register(
        BooleanSetting.builder()
            .name("Region Grid")
            .translationKey("xaeroplus.setting.region_grid")
            .defaultValue(false)
            .keybind()
            .onChange((b) -> ModuleManager.getModule(RegionGrid.class).setEnabled(b))
            .build(),
        SettingLocation.OVERLAYS);
    public final EnumSetting<ColorHelper.HighlightColor> regionGridColorSetting = register(
        EnumSetting.<ColorHelper.HighlightColor>builder()
            .name("Region Grid Color")
            .translationKey("xaeroplus.setting.region_grid_color")
            .values(ColorHelper.HighlightColor.values())
            .defaultValue(ColorHelper.HighlightColor.RED)
            .onChange((b) -> ModuleManager.getModule(RegionGrid.class).setRgbColor(b.getColor()))
            .visibleWhen(() -> ModuleManager.getModule(RegionGrid.class).isEnabled())
            .build(),
        SettingLocation.OVERLAYS);
    public final BooleanSetting regionGridTextSetting = register(
        BooleanSetting.builder()
            .name("Region Grid Text")
            .translationKey("xaeroplus.setting.region_grid_text")
            .defaultValue(false)
            .onChange((b) -> ModuleManager.getModule(RegionGrid.class).setTextEnabled(b))
            .visibleWhen(() -> ModuleManager.getModule(RegionGrid.class).isEnabled())
            .build(),
        SettingLocation.OVERLAYS);
    public final BooleanSetting beaconsOverlaySetting = register(
        BooleanSetting.builder()
            .name("Beacons Overlay")
            .translationKey("xaeroplus.setting.beacons_overlay")
            .defaultValue(false)
            .keybind()
            .onChange((b) -> ModuleManager.getModule(Beacons.class).setEnabled(b))
            .build(), SettingLocation.OVERLAYS);

    /**
     * Minimap Main
     */

    public final BooleanSetting minimapFpsLimiter = register(
        BooleanSetting.builder()
            .name("Minimap FPS Limiter")
            .translationKey("xaeroplus.setting.fps_limiter")
            .defaultValue(false)
            .onChange((b) -> ModuleManager.getModule(FpsLimiter.class).setEnabled(b))
            .build(),
        SettingLocation.MINIMAP_MAIN);
    public final DoubleSetting minimapFpsLimit = register(
        DoubleSetting.builder()
            .name("Minimap FPS Limit")
            .translationKey("xaeroplus.setting.fps_limiter_limit")
            .range(5, 120, 5)
            .defaultValue(60)
            .build(),
        SettingLocation.MINIMAP_MAIN);

    /**
     * Minimap View
     */

    public final DoubleSetting minimapScaleMultiplierSetting = register(
        DoubleSetting.builder()
            .name("Minimap Scaling Factor")
            .translationKey("xaeroplus.setting.minimap_scaling")
            .range(1, 5, 1)
            .defaultValue(1)
            .onChange((b) -> Globals.shouldResetFBO = true)
            .build(),
        SettingLocation.MINIMAP_VIEW);
    public final DoubleSetting minimapSizeMultiplierSetting = register(
        DoubleSetting.builder()
            .name("Minimap Size Multiplier")
            .translationKey("xaeroplus.setting.minimap_size_multiplier")
            .range(1, 4, 1)
            .defaultValue(1)
            .onChange((b) -> Globals.shouldResetFBO = true)
            .build(),
        SettingLocation.MINIMAP_VIEW);
    public final DoubleSetting minimapRenderZOffsetSetting = register(
        DoubleSetting.builder()
            .name("Minimap Render Z")
            .translationKey("xaeroplus.setting.minimap_render_z_offset")
            .range(-1000, 1000, 50)
            .defaultValue(0)
            .visibleWhen(() -> false)
            .build(),
        SettingLocation.MINIMAP_VIEW);

    /**
     * Minimap Entity Radar
     */

    public final BooleanSetting alwaysRenderPlayerWithNameOnRadar = register(
        BooleanSetting.builder()
            .name("Always Render Player Name")
            .translationKey("xaeroplus.setting.always_render_player_name")
            .defaultValue(true)
            .build(),
        SettingLocation.MINIMAP_ENTITY_RADAR);
    public final BooleanSetting alwaysRenderPlayerIconOnRadar = register(
        BooleanSetting.builder()
            .name("Always Render Player Icon")
            .translationKey("xaeroplus.setting.always_render_player_icon")
            .defaultValue(true)
            .build(),
        SettingLocation.MINIMAP_ENTITY_RADAR);

    /**
     * Minimap Waypoints
     */

    public final BooleanSetting waypointBeacons = register(
        BooleanSetting.builder()
            .name("Waypoint Beacons")
            .translationKey("xaeroplus.setting.waypoint_beacons")
            .defaultValue(false)
            .build(),
        SettingLocation.MINIMAP_WAYPOINTS);
    public final DoubleSetting waypointBeaconScaleMin = register(
        DoubleSetting.builder()
            .name("Waypoint Beacon Scale Min")
            .translationKey("xaeroplus.setting.waypoint_beacon_scale_min")
            .range(0, 30, 1)
            .defaultValue(0)
            .visibleWhen(waypointBeacons::get)
            .build(),
        SettingLocation.MINIMAP_WAYPOINTS);
    public final DoubleSetting waypointBeaconDistanceMin = register(
        DoubleSetting.builder()
            .name("Waypoint Beacon Distance Min")
            .translationKey("xaeroplus.setting.waypoint_beacon_distance_min")
            .range(0, 512, 8)
            .defaultValue(0)
            .visibleWhen(waypointBeacons::get)
            .build(),
        SettingLocation.MINIMAP_WAYPOINTS);
    public final BooleanSetting waypointEta = register(
        BooleanSetting.builder()
            .name("Waypoint ETA")
            .translationKey("xaeroplus.setting.waypoint_eta")
            .defaultValue(false)
            .build(),
        SettingLocation.MINIMAP_WAYPOINTS);
    public final DoubleSetting waypointEtaMeasurementInterval = register(
        DoubleSetting.builder()
            .name("Waypoint ETA Measurement Interval")
            .translationKey("xaeroplus.setting.waypoint_eta_measurement_interval")
            .range(0, 120, 2)
            .defaultValue(10)
            .onChange((v) -> WaypointEtaManager.INSTANCE.updateMeasurementInterval((int) v))
            .visibleWhen(waypointEta::get)
            .build(), SettingLocation.MINIMAP_WAYPOINTS);
    public final BooleanSetting longWaypointInitials = register(
        BooleanSetting.builder()
            .name("Long Waypoint Initials")
            .translationKey("xaeroplus.setting.allow_longer_waypoint_initials")
            .defaultValue(false)
            .build(),
        SettingLocation.MINIMAP_WAYPOINTS);
    public final BooleanSetting disableWaypointSharing = register(
        BooleanSetting.builder()
            .name("Disable Waypoint Sharing")
            .translationKey("xaeroplus.setting.disable_waypoint_sharing")
            .defaultValue(false)
            .build(),
        SettingLocation.MINIMAP_WAYPOINTS);
    public final BooleanSetting plainWaypointSharing = register(
        BooleanSetting.builder()
            .name("Plain Waypoint Sharing")
            .translationKey("xaeroplus.setting.plain_waypoint_sharing")
            .defaultValue(false)
            .build(),
        SettingLocation.MINIMAP_WAYPOINTS);
    public final BooleanSetting disableReceivingWaypoints = register(
        BooleanSetting.builder()
            .name("Disable Receiving Waypoints")
            .translationKey("xaeroplus.setting.disable_receiving_waypoints")
            .defaultValue(false)
            .build(),
        SettingLocation.MINIMAP_WAYPOINTS);
    public final BooleanSetting limitDeathpointsRenderDistance = register(
        BooleanSetting.builder()
            .name("Deathpoints Render Distance")
            .translationKey("xaeroplus.setting.deathpoints_render_distance")
            .defaultValue(false)
            .build(),
        SettingLocation.MINIMAP_WAYPOINTS);
    public final BooleanSetting disableWaypointSetChangeTooltip = register(
        BooleanSetting.builder()
            .name("Disable Waypoint Set Change Tooltip")
            .translationKey("xaeroplus.setting.waypoint_set_change_tooltip")
            .defaultValue(false)
            .build(),
        SettingLocation.MINIMAP_WAYPOINTS);
    public final BooleanSetting useCustomCrossDimensionWaypointTeleportFormat = register(
        BooleanSetting.builder()
            .name("Use Custom Cross-Dim Waypoint Teleport Format")
            .translationKey("xaeroplus.setting.use_custom_cross_dimension_waypoint_teleport_format")
            .defaultValue(false)
            .build(),
        SettingLocation.MINIMAP_WAYPOINTS);
    public final StringSetting crossDimensionWaypointTeleportFormat = register(
        StringSetting.builder()
            .name("Cross-Dim Waypoint Teleport Format")
            .translationKey("xaeroplus.setting.cross_dimension_waypoint_teleport_format")
            .defaultValue("/execute as @s in {d} run tp {x} {y} {z}")
            .screen(GuiMinimapWaypointTeleportCommandSettings::new)
            .visibleWhen(useCustomCrossDimensionWaypointTeleportFormat::get)
            .build(),
        SettingLocation.MINIMAP_WAYPOINTS);
    public final StringSetting crossDimensionWaypointTeleportRotationFormat = register(
        StringSetting.builder()
            .name("Cross-Dim Waypoint Teleport Rotation Format")
            .translationKey("xaeroplus.setting.cross_dimension_waypoint_teleport_rotation_format")
            .defaultValue("/execute as @s in {d} run tp {x} {y} {z} {yaw} ~")
            .screen(GuiMinimapWaypointTeleportCommandSettings::new)
            .visibleWhen(useCustomCrossDimensionWaypointTeleportFormat::get)
            .build(),
        SettingLocation.MINIMAP_WAYPOINTS);

    /**
     * Keybinds (hidden toggles)
     */

    public final BooleanSetting switchToNetherSetting = register(
        BooleanSetting.builder()
            .name("Switch to Nether")
            .translationKey("xaeroplus.keybind.switch_to_nether")
            .defaultValue(false)
            .keybind()
            .onChange((b) -> Globals.switchToDimension(NETHER))
            .build(),
        SettingLocation.KEYBINDS);
    public final BooleanSetting switchToOverworldSetting = register(
        BooleanSetting.builder()
            .name("Switch to Overworld")
            .translationKey("xaeroplus.keybind.switch_to_overworld")
            .defaultValue(false)
            .keybind()
            .onChange((b) -> Globals.switchToDimension(OVERWORLD))
            .build(),
        SettingLocation.KEYBINDS);
    public final BooleanSetting switchToEndSetting = register(
        BooleanSetting.builder()
            .name("Switch to End")
            .translationKey("xaeroplus.keybind.switch_to_end")
            .defaultValue(false)
            .keybind()
            .onChange((b) -> Globals.switchToDimension(END))
            .build(),
        SettingLocation.KEYBINDS);
    public final BooleanSetting switchWaypointsToNetherSetting = register(
        BooleanSetting.builder()
            .name("Switch Waypoints to Nether")
            .translationKey("xaeroplus.keybind.switch_waypoints_to_nether")
            .defaultValue(false)
            .keybind()
            .onChange((b) -> WaypointAPI.switchWaypointDimension(NETHER))
            .build(),
        SettingLocation.KEYBINDS);
    public final BooleanSetting switchWaypointsToOverworldSetting = register(
        BooleanSetting.builder()
            .name("Switch Waypoints to Overworld")
            .translationKey("xaeroplus.keybind.switch_waypoints_to_overworld")
            .defaultValue(false)
            .keybind()
            .onChange((b) -> WaypointAPI.switchWaypointDimension(OVERWORLD))
            .build(),
        SettingLocation.KEYBINDS);
    public final BooleanSetting switchWaypointsToEndSetting = register(
        BooleanSetting.builder()
            .name("Switch Waypoints to End")
            .translationKey("xaeroplus.keybind.switch_waypoints_to_end")
            .defaultValue(false)
            .keybind()
            .onChange((b) -> WaypointAPI.switchWaypointDimension(END))
            .build(),
        SettingLocation.KEYBINDS);
    public final BooleanSetting worldMapBaritoneGoalHereKeybindSetting = register(
        BooleanSetting.builder()
            .name("WorldMap Baritone Goal Here")
            .translationKey("xaeroplus.keybind.world_map_baritone_goal_here")
            .defaultValue(false)
            .keybind()
            .build(),
        SettingLocation.KEYBINDS);
    public final BooleanSetting worldMapBaritonePathHereKeybindSetting = register(
        BooleanSetting.builder()
            .name("WorldMap Baritone Path Here")
            .translationKey("xaeroplus.keybind.world_map_baritone_path_here")
            .defaultValue(false)
            .keybind()
            .build(),
        SettingLocation.KEYBINDS);
    public final BooleanSetting worldMapBaritoneElytraHereKeybindSetting = register(
        BooleanSetting.builder()
            .name("WorldMap Baritone Elytra Here")
            .translationKey("xaeroplus.keybind.world_map_baritone_elytra_here")
            .defaultValue(false)
            .keybind()
            .build(),
        SettingLocation.KEYBINDS);
    public final BooleanSetting worldMapToggleDrawingKeybindSetting = register(
        BooleanSetting.builder()
            .name("WorldMap Toggle Drawing")
            .translationKey("xaeroplus.gui.world_map.start_drawing")
            .defaultValue(false)
            .keybind()
            .build(),
        SettingLocation.KEYBINDS);
    public final BooleanSetting worldMapRotateHereKeybindSetting = register(
        BooleanSetting.builder()
            .name("WorldMap Rotate Here")
            .translationKey("xaeroplus.keybind.world_map_rotate_here")
            .defaultValue(false)
            .keybind()
            .build(),
        SettingLocation.KEYBINDS);
}
