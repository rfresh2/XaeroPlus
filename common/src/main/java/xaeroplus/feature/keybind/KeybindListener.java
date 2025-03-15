package xaeroplus.feature.keybind;

import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import net.lenni0451.lambdaevents.EventHandler;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import xaeroplus.event.ClientTickEvent;
import xaeroplus.settings.Settings;

public class KeybindListener {
    // prevents repeat events if keybind is held down
    private final Object2BooleanMap<KeyMapping> prevKeybindState = new Object2BooleanOpenHashMap<>();

    @EventHandler
    public void onTick(ClientTickEvent.Post event) {
        if (Minecraft.getInstance().screen != null) return;
        if (Minecraft.getInstance().player == null) return;
        for (KeyMapping keybind : Settings.REGISTRY.getKeybindings()) {
            if (keybind.isDown()) {
                boolean wasPrevDown = prevKeybindState.getOrDefault(keybind, false);
                prevKeybindState.put(keybind, true);
                if (!wasPrevDown) {
                    var setting = Settings.REGISTRY.getKeybindingSetting(keybind);
                    if (setting != null) {
                        setting.setValue(!setting.get());
                    }
                }
            } else {
                prevKeybindState.put(keybind, false);
            }
        }
    }
}
