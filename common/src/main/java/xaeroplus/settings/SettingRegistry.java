package xaeroplus.settings;

import net.minecraft.client.KeyMapping;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public abstract class SettingRegistry {
    private final Map<SettingLocation, ArrayList<XaeroPlusSetting>> settingLocationMap = new EnumMap<>(SettingLocation.class);
    private final Map<String, XaeroPlusSetting> settingNameMap = new ConcurrentHashMap<>();
    private final Map<KeyMapping, BooleanSetting> keybindingMap = new ConcurrentHashMap<>();

    public BooleanSetting register(BooleanSetting setting, SettingLocation settingLocation) {
        register0(settingLocation, setting);
        return setting;
    }

    public DoubleSetting register(DoubleSetting setting, SettingLocation settingLocation) {
        register0(settingLocation, setting);
        return setting;
    }

    public <E extends Enum<E>> EnumSetting<E> register(EnumSetting<E> setting, SettingLocation settingLocation) {
        register0(settingLocation, setting);
        return setting;
    }

    public StringSetting register(StringSetting setting, SettingLocation settingLocation) {
        register0(settingLocation, setting);
        return setting;
    }

    private synchronized void register0(SettingLocation settingLocation, XaeroPlusSetting setting) {
        if (settingNameMap.containsKey(setting.getSettingName())) {
            throw new RuntimeException("Setting with name '" + setting.getSettingName() + "' already exists");
        }
        var settingList = this.settingLocationMap.getOrDefault(settingLocation, new ArrayList<>());
        settingList.add(setting);
        this.settingLocationMap.put(settingLocation, settingList);
        this.settingNameMap.put(setting.getSettingName(), setting);
        if (setting instanceof BooleanSetting booleanSetting) {
            var kb = booleanSetting.getKeyBinding();
            if (kb != null) keybindingMap.put(kb, booleanSetting);
        }
    }

    public XaeroPlusSetting getSettingByName(String name) {
        return settingNameMap.get(name);
    }

    public Set<KeyMapping> getKeybindings() {
        return keybindingMap.keySet();
    }

    public BooleanSetting getKeybindingSetting(KeyMapping keyMapping) {
        return keybindingMap.get(keyMapping);
    }

    public List<XaeroPlusSetting> getAllSettings() {
        return new ArrayList<>(settingNameMap.values());
    }

    public synchronized xaero.common.gui.ConfigSettingEntry[] getMinimapConfigSettingEntries(SettingLocation settingLocation) {
        var settingList = this.settingLocationMap.get(settingLocation);
        if (settingList != null) {
            List<xaero.common.gui.ConfigSettingEntry> entries = new ArrayList<>(settingList.size());
            for (int i = 0; i < settingList.size(); i++) {
                final XaeroPlusSetting xaeroPlusSetting = settingList.get(i);
                var entry = xaeroPlusSetting.toMinimapConfigSettingEntry();
                if (entry != null) entries.add(entry);
            }
            return entries.toArray(new xaero.common.gui.ConfigSettingEntry[0]);
        }
        return new xaero.common.gui.ConfigSettingEntry[0];
    }

    public synchronized xaero.map.gui.ConfigSettingEntry[] getWorldmapConfigSettingEntries(SettingLocation settingLocation) {
        var settingList = this.settingLocationMap.get(settingLocation);
        if (settingList != null) {
            List<xaero.map.gui.ConfigSettingEntry> entries = new ArrayList<>(settingList.size());
            for (int i = 0; i < settingList.size(); i++) {
                final XaeroPlusSetting xaeroPlusSetting = settingList.get(i);
                var entry = xaeroPlusSetting.toWorldmapConfigSettingEntry();
                if (entry != null) entries.add(entry);
            }
            return entries.toArray(new xaero.map.gui.ConfigSettingEntry[0]);
        }
        return new xaero.map.gui.ConfigSettingEntry[0];
    }
}
