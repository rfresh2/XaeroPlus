package xaeroplus;

import sun.reflect.ConstructorAccessor;
import xaero.map.gui.ConfigSettingEntry;
import xaero.map.settings.ModOptions;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.lang.reflect.Modifier.FINAL;
import static java.util.Arrays.asList;
import static xaeroplus.XaeroPlusSetting.SETTING_PREFIX;

/**
 * Registry for XaeroPlus-specific settings
 */
public final class XaeroPlusSettingRegistry {

    public static final XaeroPlusSetting fastMapSetting = XaeroPlusSetting.createBooleanSetting("Fast Mapping", true);
    public static final XaeroPlusSetting mapWriterDelaySetting = XaeroPlusSetting.createFloatSetting("Fast Mapping Delay ms", 10, 2000, 10, 50);

    public static final List<XaeroPlusSetting> XAERO_PLUS_SETTING_LIST = asList(
            // add settings here
            fastMapSetting,
            mapWriterDelaySetting
    );

    public static final Map<ModOptions, ConfigSettingEntry> SETTING_ENTRY_MAP = constructXaeroPlusSettings();

    private static int enumOrdinal = 29;

    private static Map<ModOptions, ConfigSettingEntry> constructXaeroPlusSettings() {
        try {
            final ConstructorAccessor modOptionsConstructorAccessor = getModOptionsConstructorAccessor();
            final List<ModOptions> xaeroModOptions = XaeroPlusSettingRegistry.XAERO_PLUS_SETTING_LIST.stream()
                    .map(setting -> buildModOptions(modOptionsConstructorAccessor, setting, enumOrdinal++))
                    .collect(Collectors.toList());
            setEnumInternalFields(xaeroModOptions);
            return xaeroModOptions.stream()
                    .collect(Collectors.toMap(k -> k, XaeroPlusSettingRegistry::buildConfigSettingEntry, (v1, v2) -> v1));
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static ConstructorAccessor getModOptionsConstructorAccessor() throws Exception {
        Constructor<?>[] declaredConstructors = ModOptions.class.getDeclaredConstructors();
        Constructor<?> constructor = declaredConstructors[0];
        for (Constructor<?> c : declaredConstructors) {
            // this one doesn't have a CursorBox parameter
            // if we want to use that, this can always be updated to that constructor instead
            if (c.getParameterCount() == 10) {
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
