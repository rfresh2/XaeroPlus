package xaeroplus;

import sun.reflect.ConstructorAccessor;
import xaero.map.gui.ConfigSettingEntry;
import xaero.map.settings.ModOptions;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.reflect.Modifier.FINAL;
import static java.util.Arrays.asList;
import static xaeroplus.XaeroPlusSetting.SETTING_PREFIX;

/**
 * Registry for XaeroPlus-specific settings
 */
public final class XaeroPlusSettingRegistry {

    public static final XaeroPlusSetting fastMapSetting = XaeroPlusSetting.createBooleanSetting("Fast Mapping",
            "Fast Mapping will increase mapping speed in exchange for increased CPU load",
            true);
    public static final XaeroPlusSetting mapWriterDelaySetting = XaeroPlusSetting.createFloatSetting("Fast Mapping Delay ms",
            10, 2000, 10,
            "Fast Mapping must be enabled. This is roughly the delay between minimap update operations, both render and actual file writes",
            50);
    public static final XaeroPlusSetting transparentObsidianRoofSetting = XaeroPlusSetting.createBooleanSetting("Transparent Obsidian Roof",
            "Makes obsidian placed at build height transparent. Does not update tiles already mapped - you need to remap them.",
            true);
    public static final XaeroPlusSetting transparentObsidianRoofDarkeningSetting = XaeroPlusSetting.createFloatSetting("Roof Obsidian Opacity",
            // todo: I think a -1 setting here to make the obsidian completely invisible would be cool
            0, 15, 1,
            "Sets the opacity of the transparent obsidian roof tiles. Does not update tiles already mapped - you need to remap them.",
            10);
    public static final XaeroPlusSetting worldMapMinZoomSetting = XaeroPlusSetting.createFloatSetting("Min WorldMap Zoom",
            0, 0.625f, 0.01f,
            "Minimum WorldMap Zoom Setting. This is 10x what you see on the WorldMap.",
            0.1f);
    public static final XaeroPlusSetting skipWorldRenderSetting = XaeroPlusSetting.createBooleanSetting("Background World Render",
            "Skip MC world render while in a Xaero GUI. Having this on can cause issues with travel mods while you're in a Xaero GUI like the WorldMap.",
            false);

    public static final List<XaeroPlusSetting> XAERO_PLUS_SETTING_LIST = asList(
            // add settings here
            fastMapSetting,
            mapWriterDelaySetting,
            transparentObsidianRoofSetting,
            transparentObsidianRoofDarkeningSetting,
            worldMapMinZoomSetting
    );

    public static final List<ModOptions> MOD_OPTIONS_LIST = constructXaeroPlusSettings();

    private static int enumOrdinal = 69; // needs to not overlap with existing enum indeces

    private static List<ModOptions> constructXaeroPlusSettings() {
        try {
            final ConstructorAccessor modOptionsConstructorAccessor = getModOptionsConstructorAccessor();
            final List<ModOptions> xaeroModOptions = XaeroPlusSettingRegistry.XAERO_PLUS_SETTING_LIST.stream()
                    .map(setting -> buildModOptions(modOptionsConstructorAccessor, setting, enumOrdinal++))
                    .collect(Collectors.toList());
            setEnumInternalFields(xaeroModOptions);
            return xaeroModOptions;
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static List<ConfigSettingEntry> getConfigSettingEntries() {
        return MOD_OPTIONS_LIST.stream()
                .map(XaeroPlusSettingRegistry::buildConfigSettingEntry)
                .collect(Collectors.toList());
    }

    private static ConstructorAccessor getModOptionsConstructorAccessor() throws Exception {
        Constructor<?>[] declaredConstructors = ModOptions.class.getDeclaredConstructors();
        Constructor<?> constructor = declaredConstructors[0];
        for (Constructor<?> c : declaredConstructors) {
            if (c.getParameterCount() == 11) {
                constructor = c;
                break;
            }
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

    private static ModOptions buildModOptions(final ConstructorAccessor ca, final XaeroPlusSetting xaeroPlusSetting, final int ordinal) {
        try {
            return (ModOptions) ca.newInstance(new Object[]{
                    xaeroPlusSetting.getSettingName().replace(SETTING_PREFIX, ""),
                    ordinal,
                    xaeroPlusSetting.getSettingName(),
                    xaeroPlusSetting.isFloatSetting(),
                    xaeroPlusSetting.isBooleanSetting(),
                    xaeroPlusSetting.getValueMin(),
                    xaeroPlusSetting.getValueMax(),
                    xaeroPlusSetting.getValueStep(),
                    xaeroPlusSetting.getTooltip(),
                    xaeroPlusSetting.isIngameOnly(),
                    xaeroPlusSetting.isRequiresMinimap()});
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static ConfigSettingEntry buildConfigSettingEntry(final ModOptions modOptions) {
        return new ConfigSettingEntry(modOptions);
    }

    private static void setEnumInternalFields(final List<ModOptions> xaeroPlusModOptions) {
        try {
            Field valuesField = ModOptions.class.getDeclaredField("$VALUES");
            makeAccessible(valuesField);
            ModOptions[] oldValues = (ModOptions[]) valuesField.get(null);
            ModOptions[] newValues = new ModOptions[oldValues.length + xaeroPlusModOptions.size()];
            System.arraycopy(oldValues, 0, newValues, 0, oldValues.length);
            for (int i = oldValues.length; i < newValues.length; i++) {
                newValues[i] = xaeroPlusModOptions.get(i - oldValues.length);
            }
            valuesField.set(ModOptions.class, newValues);
            Field enumConstantsField = Class.class.getDeclaredField("enumConstants");
            makeAccessible(enumConstantsField);
            enumConstantsField.set(ModOptions.class, null);

            Field enumConstantDirectoryField = Class.class.getDeclaredField("enumConstantDirectory");
            makeAccessible(enumConstantDirectoryField);
            enumConstantDirectoryField.set(ModOptions.class, null);
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
}
