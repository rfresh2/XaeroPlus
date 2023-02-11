package xaeroplus.settings;

import com.google.common.base.Suppliers;
import com.google.common.collect.Streams;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import sun.reflect.ConstructorAccessor;
import xaero.map.WorldMapSession;
import xaeroplus.module.ModuleManager;
import xaeroplus.module.impl.NewChunks;
import xaeroplus.util.WDLHelper;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.reflect.Modifier.FINAL;
import static java.lang.reflect.Modifier.PRIVATE;
import static java.util.Arrays.asList;
import static xaeroplus.settings.XaeroPlusSetting.SETTING_PREFIX;

/**
 * Registry for XaeroPlus-specific settings
 */
public final class XaeroPlusSettingRegistry {

    public static final XaeroPlusSetting fastMapSetting = XaeroPlusSetting.createBooleanSetting("Fast Mapping",
                    "Increases mapping speed, might hurt FPS. Adjust rate limit and delay to regain FPS.",
            true);
    public static final XaeroPlusSetting mapWriterDelaySetting = XaeroPlusSetting.createFloatSetting("Fast Mapping Delay",
            10, 1000, 10,
                    "Fast Mapping must be enabled. \\n " +
                    "This is roughly the delay in milliseconds between minimap update operations, both render and actual file writes.",
            50);
    public static final XaeroPlusSetting fastMapMaxTilesPerCycle = XaeroPlusSetting.createFloatSetting("Fast Mapping Rate Limit",
            10, 120, 10,
                    "Fast Mapping must be enabled. \\n " +
                    "Limits how many chunks can be written in a single cycle. Lower values improve FPS at high render distances.",
            50);
    public static final XaeroPlusSetting transparentObsidianRoofSetting = XaeroPlusSetting.createBooleanSetting("Transparent Obsidian Roof",
                    "Makes obsidian placed at build height transparent. Does not update tiles already mapped - you need to remap them.",
            (v) -> XaeroPlusSettingRegistry.markChunksDirtyInWriteDistance(),
            true);
    public static final XaeroPlusSetting transparentObsidianRoofDarkeningSetting = XaeroPlusSetting.createFloatSetting("Roof Obsidian Opacity",
            -1, 15, 1,
                    "Sets the opacity of the transparent obsidian roof tiles. Does not update tiles already mapped - you need to remap them. \\n " +
                    "Change this to -1 to completely hide roof obsidian.",
            (v) -> XaeroPlusSettingRegistry.markChunksDirtyInWriteDistance(),
            5);
    public static final XaeroPlusSetting worldMapMinZoomSetting = XaeroPlusSetting.createFloatSetting("Min WorldMap Zoom",
            0, 0.625f, 0.01f,
                    "Minimum WorldMap Zoom Setting. This is 10x what you see on the WorldMap.",
            0.1f);
    public static final XaeroPlusSetting skipWorldRenderSetting = XaeroPlusSetting.createBooleanSetting("Skip Background Render",
                    "Skip MC world render while in a Xaero GUI. Having this on can cause issues with travel mods while you're in a Xaero GUI like the WorldMap.",
            false);
    public static final XaeroPlusSetting newChunksEnabledSetting = XaeroPlusSetting.createBooleanSetting("NewChunks Highlighting",
                    "Highlights NewChunks on the Minimap and WorldMap.",
            (b) -> ModuleManager.getModule(NewChunks.class).setEnabled(b),
            true);
    public static final XaeroPlusSetting newChunksSaveLoadToDisk = XaeroPlusSetting.createBooleanSetting("Save/Load NewChunks to Disk",
            "Saves and loads NewChunk data to disk for each world and dimension. Requires NewChunk Highlighting to be enabled.",
            (b) -> ModuleManager.getModule(NewChunks.class).setSaveLoad(b),
            false);
    public static final XaeroPlusSetting newChunksSeenResetTime = XaeroPlusSetting.createFloatSetting("Reset Seen Chunks Time",
            0, 1000f, 10f,
            "If we load a chunk in our NewChunk list again, reset it to an old chunk after this time period in seconds."
                    + " \\n 0 = never reset chunks",
            0f);
    public static final XaeroPlusSetting newChunksDimensionReload = XaeroPlusSetting.createBooleanSetting("Reload NewChunks",
            "If Save/Load disabled, reload all NewChunks on world or dimension change.",
            false);
    public static final XaeroPlusSetting newChunksAlphaSetting = XaeroPlusSetting.createFloatSetting("New Chunks Opacity",
            10f, 255f, 10f,
                    "Changes the color opacity of NewChunks.",
            (b) -> ModuleManager.getModule(NewChunks.class).setAlpha(b),
            100);
    public static final XaeroPlusSetting wdlEnabledSetting = XaeroPlusSetting.createBooleanSetting("WDL Highlight",
                    "Highlights chunks WDL mod has downloaded on the Minimap and WorldMap.",
            true);
    public static final XaeroPlusSetting wdlAlphaSetting = XaeroPlusSetting.createFloatSetting("WDL Opacity",
            10f, 255f, 10f,
                    "Changes the color opacity of WDL chunks.",
            WDLHelper::setAlpha,
            100);
    public static final XaeroPlusSetting owAutoWaypointDimension = XaeroPlusSetting.createBooleanSetting("Prefer Overworld Waypoints",
                    "Prefer creating and viewing Overworld waypoints when in the nether.",
            true);
    public static final XaeroPlusSetting showWaypointDistances = XaeroPlusSetting.createBooleanSetting("Show Waypoint Distances",
            "Display the distance to every waypoint in the full waypoint menu.",
            true);
    public static final XaeroPlusSetting showRenderDistanceSetting = XaeroPlusSetting.createBooleanSetting("Show Render Distance",
            "Show server side render distance (actually just another setting)",
            false);
    public static final XaeroPlusSetting assumedServerRenderDistanceSetting = XaeroPlusSetting.createFloatSetting("Server Render Distance",
            1f, 32f, 1f,
            "width of the square for displaying the render distance",
            9f); // 2b2t currently returns a 9 wide square to the client

    public static final List<XaeroPlusSetting> XAERO_PLUS_WORLDMAP_SETTINGS = asList(
            // add settings here
            fastMapSetting,
            mapWriterDelaySetting,
            fastMapMaxTilesPerCycle,
            transparentObsidianRoofSetting,
            transparentObsidianRoofDarkeningSetting,
            worldMapMinZoomSetting,
            skipWorldRenderSetting,
            newChunksEnabledSetting,
            newChunksSaveLoadToDisk,
            newChunksSeenResetTime,
            newChunksAlphaSetting,
            newChunksDimensionReload,
            wdlEnabledSetting,
            wdlAlphaSetting,
            owAutoWaypointDimension,
            showWaypointDistances
    );

    public static final List<XaeroPlusSetting> XAERO_PLUS_MINIMAP_SETTINGS = asList(
            // add settings here
            showRenderDistanceSetting,
            assumedServerRenderDistanceSetting
    );

    private static List<xaero.map.settings.ModOptions> WORLDMAP_MOD_OPTIONS_LIST = null;
    private static List<xaero.common.settings.ModOptions> MINIMAP_MOD_OPTIONS_LIST = null;

    private static int enumOrdinal = 69; // needs to not overlap with existing enum indeces

    private enum ModType {
        WORLDMAP,
        MINIMAP
    }

    private static <T extends Enum<T>> List<T> constructXaeroPlusSettings(Class<T> clazz, ModType type, List<XaeroPlusSetting> settings) {
        try {
            final ConstructorAccessor modOptionsConstructorAccessor = getModOptionsConstructorAccessor(clazz, type);
            final List<T> xaeroModOptions = settings.stream()
                    .map(setting -> XaeroPlusSettingRegistry.<T>buildModOptions(type, modOptionsConstructorAccessor, setting, enumOrdinal++))
                    .collect(Collectors.toList());
            setEnumInternalFields(clazz, xaeroModOptions);
            return xaeroModOptions;
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static List<KeyBinding> getKeybinds() {
        return memoizingKeybindsList.get();
    }

    private static final Supplier<List<KeyBinding>> memoizingKeybindsList = Suppliers.memoize(() ->
            Stream.concat(XAERO_PLUS_WORLDMAP_SETTINGS.stream(), XAERO_PLUS_MINIMAP_SETTINGS.stream())
                .filter(XaeroPlusSetting::isBooleanSetting)
                .map(XaeroPlusSetting::getKeyBinding)
                .filter(Objects::nonNull)
                .collect(Collectors.toList())
    );

    public static List<xaero.map.gui.ConfigSettingEntry> getWorldMapConfigSettingEntries() {
        if (WORLDMAP_MOD_OPTIONS_LIST == null) {
            WORLDMAP_MOD_OPTIONS_LIST = constructXaeroPlusSettings(xaero.map.settings.ModOptions.class, ModType.WORLDMAP, XAERO_PLUS_WORLDMAP_SETTINGS);
        }
        return WORLDMAP_MOD_OPTIONS_LIST.stream()
                .map(xaero.map.gui.ConfigSettingEntry::new)
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
                    xaeroPlusSetting.isFloatSetting(),
                    xaeroPlusSetting.isBooleanSetting(),
                    xaeroPlusSetting.getValueMin(),
                    xaeroPlusSetting.getValueMax(),
                    xaeroPlusSetting.getValueStep(),
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

    private static void markChunksDirtyInWriteDistance() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world != null && mc.player != null) {
            WorldMapSession session = WorldMapSession.getCurrentSession();
            if (session != null) {
                session.getMapProcessor().getMapWriter().setDirtyInWriteDistance(mc.player, mc.world);
            }
        }
    }
}
