package xaeroplus.settings;

import com.google.common.base.Suppliers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import sun.reflect.ConstructorAccessor;
import xaero.map.WorldMapSession;
import xaero.map.settings.ModOptions;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.reflect.Modifier.FINAL;
import static xaeroplus.settings.XaeroPlusSetting.SETTING_PREFIX;

// yucky reflection and utils so our settings can be in xaero's gui's
public class XaeroPlusSettingsReflectionHax {
    public static final List<XaeroPlusSetting> XAERO_PLUS_WORLDMAP_SETTINGS = new ArrayList<>();

    public static final List<XaeroPlusSetting> XAERO_PLUS_MINIMAP_OVERLAY_SETTINGS = new ArrayList<>();
    public static final List<XaeroPlusSetting> XAERO_PLUS_MINIMAP_ENTITY_RADAR_SETTINGS = new ArrayList<>();
    public static final List<XaeroPlusSetting> XAERO_PLUS_MINIMAP_SETTINGS = new ArrayList<>();
    public static final List<XaeroPlusSetting> XAERO_PLUS_WAYPOINTS_SETTINGS = new ArrayList<>();
    public static final Supplier<List<XaeroPlusSetting>> ALL_MINIMAP_SETTINGS = () ->
            Stream.of(XAERO_PLUS_MINIMAP_OVERLAY_SETTINGS.stream(),
                            XAERO_PLUS_MINIMAP_SETTINGS.stream(),
                            XAERO_PLUS_MINIMAP_ENTITY_RADAR_SETTINGS.stream(),
                            XAERO_PLUS_WAYPOINTS_SETTINGS.stream())
            .flatMap(x -> x)
            .collect(Collectors.toList());
    public static final List<XaeroPlusSetting> XAERO_PLUS_KEYBIND_SETTINGS = new ArrayList<>();
    public static final Supplier<List<XaeroPlusSetting>> ALL_SETTINGS = () ->
            Stream.of(XAERO_PLUS_WORLDMAP_SETTINGS.stream(),
                            XAERO_PLUS_MINIMAP_OVERLAY_SETTINGS.stream(),
                            XAERO_PLUS_MINIMAP_ENTITY_RADAR_SETTINGS.stream(),
                            XAERO_PLUS_MINIMAP_SETTINGS.stream(),
                            XAERO_PLUS_KEYBIND_SETTINGS.stream(),
                            XAERO_PLUS_WAYPOINTS_SETTINGS.stream())
            .flatMap(x -> x)
            .collect(Collectors.toList());

    public enum SettingLocation {
        WORLD_MAP_MAIN(XAERO_PLUS_WORLDMAP_SETTINGS),
        MINIMAP_OVERLAYS(XAERO_PLUS_MINIMAP_OVERLAY_SETTINGS),
        MINIMAP(XAERO_PLUS_MINIMAP_SETTINGS),
        KEYBINDS(XAERO_PLUS_KEYBIND_SETTINGS),
        MINIMAP_ENTITY_RADAR(XAERO_PLUS_MINIMAP_ENTITY_RADAR_SETTINGS),
        WAYPOINTS(XAERO_PLUS_WAYPOINTS_SETTINGS);

        private final List<XaeroPlusSetting> settingsList;

        SettingLocation(final List<XaeroPlusSetting> settingsList) {
            this.settingsList = settingsList;
        }

        public List<XaeroPlusSetting> getSettingsList() {
            return settingsList;
        }
    }

    private static final Supplier<List<KeyBinding>> memoizingKeybindsList = Suppliers.memoize(() ->
            Stream.of(XAERO_PLUS_WORLDMAP_SETTINGS.stream(),
                            XAERO_PLUS_MINIMAP_OVERLAY_SETTINGS.stream(),
                            XAERO_PLUS_MINIMAP_ENTITY_RADAR_SETTINGS.stream(),
                            XAERO_PLUS_MINIMAP_SETTINGS.stream(),
                            XAERO_PLUS_KEYBIND_SETTINGS.stream(),
                            XAERO_PLUS_WAYPOINTS_SETTINGS.stream())
                    .flatMap(x -> x)
                    .filter(setting -> setting instanceof XaeroPlusBooleanSetting)
                    .map(XaeroPlusSetting::getKeyBinding)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList())
    );
    private static List<ModOptions> WORLDMAP_MOD_OPTIONS_LIST = null;
    private static List<xaero.common.settings.ModOptions> MINIMAP_OVERLAY_MOD_OPTIONS_LIST = null;
    private static List<xaero.common.settings.ModOptions> MINIMAP_MOD_OPTIONS_LIST = null;
    private static List<xaero.common.settings.ModOptions> MINIMAP_ENTITY_RADAR_MOD_OPTIONS_LIST = null;
    private static List<xaero.common.settings.ModOptions> WAYPOINTS_MOD_OPTIONS_LIST = null;

    private static int enumOrdinal = 69; // needs to not overlap with existing enum indeces

    private static <T extends Enum<T>> List<T> constructXaeroPlusSettings(Class<T> clazz, ModType type, List<XaeroPlusSetting> settings) {
        try {
            final ConstructorAccessor modOptionsConstructorAccessor = getModOptionsConstructorAccessor(clazz, type);
            final List<T> xaeroModOptions = settings.stream()
                    .map(setting -> XaeroPlusSettingsReflectionHax.<T>buildModOptions(type, modOptionsConstructorAccessor, setting, enumOrdinal++))
                    .collect(Collectors.toList());
            setEnumInternalFields(clazz, xaeroModOptions);
            return xaeroModOptions;
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static List<xaero.map.gui.ConfigSettingEntry> getWorldMapConfigSettingEntries() {
        if (WORLDMAP_MOD_OPTIONS_LIST == null) {
            WORLDMAP_MOD_OPTIONS_LIST = constructXaeroPlusSettings(ModOptions.class, ModType.WORLDMAP, XAERO_PLUS_WORLDMAP_SETTINGS);
        }
        return WORLDMAP_MOD_OPTIONS_LIST.stream()
                .map(xaero.map.gui.ConfigSettingEntry::new)
                .collect(Collectors.toList());
    }

    public static List<xaero.common.gui.ConfigSettingEntry> getMiniMapOverlayConfigSettingEntries() {
        if (MINIMAP_OVERLAY_MOD_OPTIONS_LIST == null) {
            MINIMAP_OVERLAY_MOD_OPTIONS_LIST = constructXaeroPlusSettings(xaero.common.settings.ModOptions.class, ModType.MINIMAP, XAERO_PLUS_MINIMAP_OVERLAY_SETTINGS);
        }
        return MINIMAP_OVERLAY_MOD_OPTIONS_LIST.stream()
                .map(xaero.common.gui.ConfigSettingEntry::new)
                .collect(Collectors.toList());
    }

    public static List<xaero.common.gui.ConfigSettingEntry> getMiniMapEntityRadarSettingEntries() {
        if (MINIMAP_ENTITY_RADAR_MOD_OPTIONS_LIST == null) {
            MINIMAP_ENTITY_RADAR_MOD_OPTIONS_LIST = constructXaeroPlusSettings(xaero.common.settings.ModOptions.class, ModType.MINIMAP, XAERO_PLUS_MINIMAP_ENTITY_RADAR_SETTINGS);
        }
        return MINIMAP_ENTITY_RADAR_MOD_OPTIONS_LIST.stream()
                .map(xaero.common.gui.ConfigSettingEntry::new)
                .collect(Collectors.toList());
    }

    public static List<xaero.common.gui.ConfigSettingEntry> getMiniMapConfigSettingEntries() {
        if (MINIMAP_MOD_OPTIONS_LIST == null) {
            MINIMAP_MOD_OPTIONS_LIST = constructXaeroPlusSettings(xaero.common.settings.ModOptions.class, ModType.MINIMAP, XAERO_PLUS_MINIMAP_SETTINGS);
        }
        return MINIMAP_MOD_OPTIONS_LIST.stream()
                .map(xaero.common.gui.ConfigSettingEntry::new)
                .collect(Collectors.toList());
    }

    public static List<xaero.common.gui.ConfigSettingEntry> getWaypointSettingEntries() {
        if (WAYPOINTS_MOD_OPTIONS_LIST == null) {
            WAYPOINTS_MOD_OPTIONS_LIST = constructXaeroPlusSettings(xaero.common.settings.ModOptions.class, ModType.MINIMAP, XAERO_PLUS_WAYPOINTS_SETTINGS);
        }
        return WAYPOINTS_MOD_OPTIONS_LIST.stream()
                .map(xaero.common.gui.ConfigSettingEntry::new)
                .collect(Collectors.toList());
    }

    private static <T extends Enum<T>> ConstructorAccessor getModOptionsConstructorAccessor(Class<T> clazz, ModType type) throws Exception {
        final int numArgs;
        if (type == ModType.WORLDMAP) {
            numArgs = 11;
        } else {
            numArgs = 10; // no "requiresMinimap" argument for minimap
        }
        Constructor<T>[] declaredConstructors = (Constructor<T>[]) clazz.getDeclaredConstructors();
        Constructor<T> constructor = null;
        for (Constructor<T> c : declaredConstructors) {
            if (c.getParameterCount() == numArgs) {
                constructor = c;
                break;
            }
        }
        if (constructor == null) {
            throw new IllegalStateException("No constructor found in " + clazz.getName() + " with " + numArgs + " args");
        }

        constructor.setAccessible(true);

        Field constructorAccessorField = Constructor.class.getDeclaredField("constructorAccessor");
        constructorAccessorField.setAccessible(true);
        ConstructorAccessor ca = (ConstructorAccessor) constructorAccessorField.get(constructor);
        if (ca == null) {
            Method acquireConstructorAccessorMethod = Constructor.class.getDeclaredMethod("acquireConstructorAccessor");
            acquireConstructorAccessorMethod.setAccessible(true);
            ca = (ConstructorAccessor) acquireConstructorAccessorMethod.invoke(constructor);
        }
        return ca;
    }

    private static <T extends Enum<T>> T buildModOptions(final ModType type, final ConstructorAccessor ca, final XaeroPlusSetting xaeroPlusSetting, final int ordinal) {
        try {
            final Object cursorBox;
            if (xaeroPlusSetting.getTooltip() != null) {
                if (type == ModType.WORLDMAP) {
                    cursorBox = new xaero.map.gui.CursorBox(xaeroPlusSetting.getTooltip());
                } else {
                    cursorBox = new xaero.common.graphics.CursorBox(xaeroPlusSetting.getTooltip());
                }
            } else {
                cursorBox = null;
            }

            Object[] args = new Object[] {
                    xaeroPlusSetting.getSettingName().replace(SETTING_PREFIX, ""),
                    ordinal,
                    xaeroPlusSetting.getSettingName(),
                    xaeroPlusSetting instanceof XaeroPlusFloatSetting || xaeroPlusSetting instanceof XaeroPlusEnumSetting,
                    xaeroPlusSetting instanceof XaeroPlusBooleanSetting,
                    (xaeroPlusSetting instanceof XaeroPlusFloatSetting)
                            ? ((XaeroPlusFloatSetting) xaeroPlusSetting).getValueMin()
                            : 0f,
                    (xaeroPlusSetting instanceof XaeroPlusFloatSetting)
                            ? ((XaeroPlusFloatSetting) xaeroPlusSetting).getValueMax()
                            : (xaeroPlusSetting instanceof XaeroPlusEnumSetting)
                                ? ((XaeroPlusEnumSetting) xaeroPlusSetting).getIndexMax()
                                : 0f,
                    (xaeroPlusSetting instanceof XaeroPlusFloatSetting) ? ((XaeroPlusFloatSetting) xaeroPlusSetting).getValueStep() : 1f,
                    cursorBox,
                    xaeroPlusSetting.isIngameOnly(),
                    xaeroPlusSetting.isRequiresMinimap()
            };
            if (type == ModType.MINIMAP) {
                args = Arrays.copyOf(args, args.length - 1);
            }
            return (T) ca.newInstance(args);
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static <T extends Enum<T>> void setEnumInternalFields(final Class<T> clazz, final List<T> xaeroPlusModOptions) {
        try {
            Field valuesField = clazz.getDeclaredField("$VALUES");
            makeAccessible(valuesField);
            T[] oldValues = (T[]) valuesField.get(null);
            T[] newValues = (T[]) Array.newInstance(clazz, oldValues.length + xaeroPlusModOptions.size());
            System.arraycopy(oldValues, 0, newValues, 0, oldValues.length);
            for (int i = oldValues.length; i < newValues.length; i++) {
                newValues[i] = xaeroPlusModOptions.get(i - oldValues.length);
            }
            valuesField.set(clazz, newValues);
            Field enumConstantsField = Class.class.getDeclaredField("enumConstants");
            makeAccessible(enumConstantsField);
            enumConstantsField.set(clazz, null);

            Field enumConstantDirectoryField = Class.class.getDeclaredField("enumConstantDirectory");
            makeAccessible(enumConstantDirectoryField);
            enumConstantDirectoryField.set(clazz, null);
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void makeAccessible(Field field) throws Exception {
        field.setAccessible(true);
        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(field, field.getModifiers() & ~ FINAL);
    }

    static void markChunksDirtyInWriteDistance() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world != null && mc.player != null) {
            WorldMapSession session = WorldMapSession.getCurrentSession();
            if (session != null) {
                session.getMapProcessor().getMapWriter().setDirtyInWriteDistance(mc.player, mc.world);
                session.getMapProcessor().getMapWriter().requestCachedColoursClear();
            }
        }
    }

    public static List<KeyBinding> getKeybinds() {
        return memoizingKeybindsList.get();
    }

    private enum ModType {
        WORLDMAP,
        MINIMAP
    }
}
