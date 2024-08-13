package xaeroplus.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import xaero.map.gui.CursorBox;

@Mixin(value = xaero.map.settings.ModOptions.class, remap = false)
public interface AccessorWorldMapModOptions {

    @Invoker(value = "<init>")
    static xaero.map.settings.ModOptions createBooleanSetting(String settingName,
                                                              CursorBox tooltip,
                                                              boolean inGameOnly,
                                                              boolean requiresMinimap,
                                                              boolean requiresPac) {
        throw new AssertionError();
    }

    @Invoker(value = "<init>")
    static xaero.map.settings.ModOptions createEnumSetting(String settingName,
                                                           int optionCount,
                                                           CursorBox tooltip,
                                                           boolean inGameOnly,
                                                           boolean requiresMinimap,
                                                           boolean requiresPac) {
        throw new AssertionError();
    }

    @Invoker(value = "<init>")
    static xaero.map.settings.ModOptions createDoubleSetting(String settingName,
                                                             double valueMin,
                                                             double valueMax,
                                                             double valueStep,
                                                             CursorBox tooltip,
                                                             boolean inGameOnly,
                                                             boolean requiresMinimap,
                                                             boolean requiresPac) {
        throw new AssertionError();
    }
}
