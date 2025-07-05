package xaeroplus.util;

import xaeroplus.settings.Settings;

import java.util.ArrayList;
import java.util.List;

public class DrawOrderHelper {

    public static List<String> load(String serializedValue) {
        ArrayList<String> result = new ArrayList<>();
        if (serializedValue.isBlank()) return result;
        String[] split = serializedValue.split("\\|");
        for (var s : split) {
            if (s.isBlank()) continue;
            result.add(s.trim());
        }
        return result;
    }

    public static List<String> load() {
        return load(Settings.REGISTRY.drawOrderSetting.getSerializedValue());
    }

    public static String serialize(List<String> ids) {
        if (ids.isEmpty()) return "";
        return String.join("|", ids);
    }
}
