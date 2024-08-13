package xaeroplus.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import xaero.common.graphics.CursorBox;

@Mixin(value = xaero.common.settings.ModOptions.class, remap = false)
public interface AccessorMinimapModOptions {
    @Invoker(value = "<init>")
    static xaero.common.settings.ModOptions createBooleanSetting(String settingName,
                                                                 CursorBox tooltip,
                                                                 boolean inGameOnly) {
        throw new AssertionError();
    }

    @Invoker(value = "<init>")
    static xaero.common.settings.ModOptions createEnumSetting(String settingName,
                                                              int firstOption,
                                                              int lastOption,
                                                              CursorBox tooltip,
                                                              boolean inGameOnly) {
        throw new AssertionError();
    }

    @Invoker(value = "<init>")
    static xaero.common.settings.ModOptions createDoubleSetting(String settingName,
                                                                double valueMin,
                                                                double valueMax,
                                                                float valueStep,
                                                                CursorBox tooltip,
                                                                boolean inGameOnly) {
        throw new AssertionError();
    }
}
