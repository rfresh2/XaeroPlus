package xaeroplus.feature.keybind;

import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import xaeroplus.settings.XaeroPlusBooleanSetting;
import xaeroplus.settings.XaeroPlusSettingsReflectionHax;

public class KeybindListener {
    // prevents repeat events if keybind is held down
    private final Object2BooleanMap<KeyBinding> prevKeybindState = new Object2BooleanOpenHashMap<>();

    @SubscribeEvent
    public void onTick(InputEvent.KeyInputEvent event) {
        if (Minecraft.getMinecraft().currentScreen != null) return;
        if (Minecraft.getMinecraft().player == null) return;
        for (KeyBinding keybind : XaeroPlusSettingsReflectionHax.keybindsSupplier.get()) {
            if (keybind.isPressed()) {
                boolean wasPrevDown = prevKeybindState.getOrDefault(keybind, false);
                prevKeybindState.put(keybind, true);
                if (!wasPrevDown) {
                    XaeroPlusBooleanSetting setting = XaeroPlusSettingsReflectionHax.keybindingMapSupplier.get().get(keybind);
                    if (setting != null) {
                        setting.setValue(!setting.getValue());
                    }
                }
            } else {
                prevKeybindState.put(keybind, false);
            }
        }
    }
}
