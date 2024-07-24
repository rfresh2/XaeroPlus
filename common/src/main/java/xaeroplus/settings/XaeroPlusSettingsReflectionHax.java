package xaeroplus.settings;

import com.google.common.base.Suppliers;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import xaero.map.WorldMapSession;
import xaero.map.gui.CursorBox;
import xaero.map.settings.ModOptions;
import xaeroplus.mixin.client.MixinMinimapModOptionsAccessor;
import xaeroplus.mixin.client.MixinWorldMapModOptionsAccessor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// yucky reflection and utils so our settings can be in xaero's gui's
public class XaeroPlusSettingsReflectionHax {
    public static final List<XaeroPlusSetting> XAERO_PLUS_WORLDMAP_SETTINGS = new ArrayList<>();
    public static final List<XaeroPlusSetting> XAERO_PLUS_CHUNK_HIGHLIGHT_SETTINGS = new ArrayList<>();
    public static final List<XaeroPlusSetting> XAERO_PLUS_MINIMAP_OVERLAY_SETTINGS = new ArrayList<>();
    public static final List<XaeroPlusSetting> XAERO_PLUS_MINIMAP_ENTITY_RADAR_SETTINGS = new ArrayList<>();
    public static final List<XaeroPlusSetting> XAERO_PLUS_MINIMAP_SETTINGS = new ArrayList<>();
    public static final List<XaeroPlusSetting> XAERO_PLUS_WAYPOINT_SETTINGS = new ArrayList<>();
    public static final Supplier<List<XaeroPlusSetting>> ALL_MINIMAP_SETTINGS = Suppliers.memoize(() ->
            Stream.of(XAERO_PLUS_MINIMAP_OVERLAY_SETTINGS.stream(),
                            XAERO_PLUS_MINIMAP_SETTINGS.stream(),
                            XAERO_PLUS_MINIMAP_ENTITY_RADAR_SETTINGS.stream(),
                            XAERO_PLUS_WAYPOINT_SETTINGS.stream())
            .flatMap(x -> x)
            .collect(Collectors.toList()));
    public static final Supplier<List<XaeroPlusSetting>> ALL_WORLD_MAP_SETTINGS = Suppliers.memoize(() ->
        Stream.of(XAERO_PLUS_WORLDMAP_SETTINGS.stream(),
                  XAERO_PLUS_CHUNK_HIGHLIGHT_SETTINGS.stream())
            .flatMap(x -> x)
            .collect(Collectors.toList()));
    public static final List<XaeroPlusSetting> XAERO_PLUS_KEYBIND_SETTINGS = new ArrayList<>();
    public static final Supplier<List<XaeroPlusSetting>> ALL_SETTINGS = Suppliers.memoize(() ->
            Stream.of(XAERO_PLUS_WORLDMAP_SETTINGS.stream(),
                            XAERO_PLUS_CHUNK_HIGHLIGHT_SETTINGS.stream(),
                            XAERO_PLUS_MINIMAP_OVERLAY_SETTINGS.stream(),
                            XAERO_PLUS_MINIMAP_ENTITY_RADAR_SETTINGS.stream(),
                            XAERO_PLUS_MINIMAP_SETTINGS.stream(),
                            XAERO_PLUS_KEYBIND_SETTINGS.stream(),
                            XAERO_PLUS_WAYPOINT_SETTINGS.stream())
            .flatMap(x -> x)
            .collect(Collectors.toList()));

    public enum SettingLocation {
        WORLD_MAP_MAIN(XAERO_PLUS_WORLDMAP_SETTINGS),
        CHUNK_HIGHLIGHTS(XAERO_PLUS_CHUNK_HIGHLIGHT_SETTINGS),
        MINIMAP_OVERLAYS(XAERO_PLUS_MINIMAP_OVERLAY_SETTINGS),
        MINIMAP(XAERO_PLUS_MINIMAP_SETTINGS),
        KEYBINDS(XAERO_PLUS_KEYBIND_SETTINGS),
        MINIMAP_ENTITY_RADAR(XAERO_PLUS_MINIMAP_ENTITY_RADAR_SETTINGS),
        WAYPOINTS(XAERO_PLUS_WAYPOINT_SETTINGS);

        private final List<XaeroPlusSetting> settingsList;

        SettingLocation(final List<XaeroPlusSetting> settingsList) {
            this.settingsList = settingsList;
        }

        public List<XaeroPlusSetting> getSettingsList() {
            return settingsList;
        }
    }

    private static final Supplier<List<XaeroPlusBooleanSetting>> memoizingKeybindsList = Suppliers.memoize(() ->
            Stream.of(XAERO_PLUS_WORLDMAP_SETTINGS.stream(),
                            XAERO_PLUS_CHUNK_HIGHLIGHT_SETTINGS.stream(),
                            XAERO_PLUS_MINIMAP_OVERLAY_SETTINGS.stream(),
                            XAERO_PLUS_MINIMAP_ENTITY_RADAR_SETTINGS.stream(),
                            XAERO_PLUS_MINIMAP_SETTINGS.stream(),
                            XAERO_PLUS_KEYBIND_SETTINGS.stream(),
                            XAERO_PLUS_WAYPOINT_SETTINGS.stream())
                .flatMap(x -> x)
                .filter(setting -> setting instanceof XaeroPlusBooleanSetting)
                .map(setting -> (XaeroPlusBooleanSetting) setting)
                .filter(setting -> setting.getKeyBinding() != null)
                .collect(Collectors.toList()));
    private static List<ModOptions> WORLDMAP_MOD_OPTIONS_LIST = null;
    private static List<ModOptions> CHUNK_HIGHLIGHTS_MOD_OPTIONS_LIST = null;
    private static List<xaero.common.settings.ModOptions> MINIMAP_OVERLAY_MOD_OPTIONS_LIST = null;
    private static List<xaero.common.settings.ModOptions> MINIMAP_MOD_OPTIONS_LIST = null;
    private static List<xaero.common.settings.ModOptions> MINIMAP_ENTITY_RADAR_MOD_OPTIONS_LIST = null;
    private static List<xaero.common.settings.ModOptions> WAYPOINTS_MOD_OPTIONS_LIST = null;

    private static List<xaero.common.settings.ModOptions> constructXaeroPlusMinimapModOptions(final List<XaeroPlusSetting> settings) {
        return settings.stream().map(XaeroPlusSettingsReflectionHax::buildMinimapModOptions).collect(Collectors.toList());
    }

    private static List<xaero.map.settings.ModOptions> constructXaeroPlusWorldMapModOptions(final List<XaeroPlusSetting> settings) {
        return settings.stream().map(XaeroPlusSettingsReflectionHax::buildWorldMapModOptions).collect(Collectors.toList());
    }

    public static List<xaero.map.gui.ConfigSettingEntry> getWorldMapConfigSettingEntries() {
        if (WORLDMAP_MOD_OPTIONS_LIST == null) {
            WORLDMAP_MOD_OPTIONS_LIST = constructXaeroPlusWorldMapModOptions(XAERO_PLUS_WORLDMAP_SETTINGS);
        }
        return WORLDMAP_MOD_OPTIONS_LIST.stream()
                .map(xaero.map.gui.ConfigSettingEntry::new)
                .collect(Collectors.toList());
    }

    public static List<xaero.map.gui.ConfigSettingEntry> getChunkHighlightConfigSettingEntries() {
        if (CHUNK_HIGHLIGHTS_MOD_OPTIONS_LIST == null) {
            CHUNK_HIGHLIGHTS_MOD_OPTIONS_LIST = constructXaeroPlusWorldMapModOptions(XAERO_PLUS_CHUNK_HIGHLIGHT_SETTINGS);
        }
        return CHUNK_HIGHLIGHTS_MOD_OPTIONS_LIST.stream()
            .map(xaero.map.gui.ConfigSettingEntry::new)
            .collect(Collectors.toList());
    }

    public static List<xaero.common.gui.ConfigSettingEntry> getMiniMapOverlayConfigSettingEntries() {
        if (MINIMAP_OVERLAY_MOD_OPTIONS_LIST == null) {
            MINIMAP_OVERLAY_MOD_OPTIONS_LIST = constructXaeroPlusMinimapModOptions(XAERO_PLUS_MINIMAP_OVERLAY_SETTINGS);
        }
        return MINIMAP_OVERLAY_MOD_OPTIONS_LIST.stream()
                .map(xaero.common.gui.ConfigSettingEntry::new)
                .collect(Collectors.toList());
    }

    public static List<xaero.common.gui.ConfigSettingEntry> getMiniMapEntityRadarSettingEntries() {
        if (MINIMAP_ENTITY_RADAR_MOD_OPTIONS_LIST == null) {
            MINIMAP_ENTITY_RADAR_MOD_OPTIONS_LIST = constructXaeroPlusMinimapModOptions(XAERO_PLUS_MINIMAP_ENTITY_RADAR_SETTINGS);
        }
        return MINIMAP_ENTITY_RADAR_MOD_OPTIONS_LIST.stream()
                .map(xaero.common.gui.ConfigSettingEntry::new)
                .collect(Collectors.toList());
    }

    public static List<xaero.common.gui.ConfigSettingEntry> getMiniMapConfigSettingEntries() {
        if (MINIMAP_MOD_OPTIONS_LIST == null) {
            MINIMAP_MOD_OPTIONS_LIST = constructXaeroPlusMinimapModOptions(XAERO_PLUS_MINIMAP_SETTINGS);
        }
        return MINIMAP_MOD_OPTIONS_LIST.stream()
                .map(xaero.common.gui.ConfigSettingEntry::new)
                .collect(Collectors.toList());
    }

    public static List<xaero.common.gui.ConfigSettingEntry> getWaypointConfigSettingEntries() {
        if (WAYPOINTS_MOD_OPTIONS_LIST == null) {
            WAYPOINTS_MOD_OPTIONS_LIST = constructXaeroPlusMinimapModOptions(XAERO_PLUS_WAYPOINT_SETTINGS);
        }
        return WAYPOINTS_MOD_OPTIONS_LIST.stream()
                .map(xaero.common.gui.ConfigSettingEntry::new)
                .collect(Collectors.toList());
    }

    private static xaero.map.settings.ModOptions buildWorldMapModOptions(final XaeroPlusSetting xaeroPlusSetting) {
        try {
            if (xaeroPlusSetting instanceof XaeroPlusBooleanSetting) {
                return MixinWorldMapModOptionsAccessor.createBooleanSetting(
                        xaeroPlusSetting.getSettingName(),
                        new CursorBox(xaeroPlusSetting.getTooltipTranslationKey()),
                        xaeroPlusSetting.isIngameOnly(),
                        xaeroPlusSetting.isRequiresMinimap(),
                        false);
            } else if (xaeroPlusSetting instanceof XaeroPlusFloatSetting) {
                return MixinWorldMapModOptionsAccessor.createDoubleSetting(
                        xaeroPlusSetting.getSettingName(),
                        ((XaeroPlusFloatSetting) xaeroPlusSetting).getValueMin(),
                        ((XaeroPlusFloatSetting) xaeroPlusSetting).getValueMax(),
                        ((XaeroPlusFloatSetting) xaeroPlusSetting).getValueStep(),
                        new CursorBox(xaeroPlusSetting.getTooltipTranslationKey()),
                        xaeroPlusSetting.isIngameOnly(),
                        xaeroPlusSetting.isRequiresMinimap(),
                        false
                );
            } else if (xaeroPlusSetting instanceof XaeroPlusEnumSetting) {
                return MixinWorldMapModOptionsAccessor.createEnumSetting(
                        xaeroPlusSetting.getSettingName(),
                        ((XaeroPlusEnumSetting<?>) xaeroPlusSetting).getIndexMax() + 1,
                        new CursorBox(xaeroPlusSetting.getTooltipTranslationKey()),
                        xaeroPlusSetting.isIngameOnly(),
                        xaeroPlusSetting.isRequiresMinimap(),
                        false
                );
            }
            throw new RuntimeException("Unknown XaeroPlusSetting type: " + xaeroPlusSetting.getClass().getName());
        } catch (final Throwable e) {
            throw new RuntimeException(e);
        }
    }

    private static xaero.common.settings.ModOptions buildMinimapModOptions(final XaeroPlusSetting xaeroPlusSetting) {
        try {
            if (xaeroPlusSetting instanceof XaeroPlusBooleanSetting) {
                return MixinMinimapModOptionsAccessor.createBooleanSetting(
                        xaeroPlusSetting.getSettingName(),
                        new xaero.common.graphics.CursorBox(xaeroPlusSetting.getTooltipTranslationKey()),
                        xaeroPlusSetting.isIngameOnly());
            } else if (xaeroPlusSetting instanceof XaeroPlusFloatSetting) {
                return MixinMinimapModOptionsAccessor.createDoubleSetting(
                        xaeroPlusSetting.getSettingName(),
                        ((XaeroPlusFloatSetting) xaeroPlusSetting).getValueMin(),
                        ((XaeroPlusFloatSetting) xaeroPlusSetting).getValueMax(),
                        ((XaeroPlusFloatSetting) xaeroPlusSetting).getValueStep(),
                        new xaero.common.graphics.CursorBox(xaeroPlusSetting.getTooltipTranslationKey()),
                        xaeroPlusSetting.isIngameOnly()
                );
            } else if (xaeroPlusSetting instanceof XaeroPlusEnumSetting) {
                return MixinMinimapModOptionsAccessor.createEnumSetting(
                        xaeroPlusSetting.getSettingName(),
                        0,
                        ((XaeroPlusEnumSetting<?>) xaeroPlusSetting).getIndexMax(),
                        new xaero.common.graphics.CursorBox(xaeroPlusSetting.getTooltipTranslationKey()),
                        xaeroPlusSetting.isIngameOnly()
                );
            }
            throw new RuntimeException("Unknown XaeroPlusSetting type: " + xaeroPlusSetting.getClass().getName());
        } catch (final Throwable e) {
            throw new RuntimeException(e);
        }
    }

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

    public static Supplier<Map<KeyMapping, XaeroPlusBooleanSetting>> keybindingMapSupplier = Suppliers.memoize(() ->
            memoizingKeybindsList.get().stream().collect(Collectors.toMap(XaeroPlusSetting::getKeyBinding, s -> s)));

    public static Supplier<List<KeyMapping>> keybindsSupplier = Suppliers.memoize(() -> new ArrayList<>(
        keybindingMapSupplier.get().keySet()));
}
