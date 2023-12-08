package xaeroplus.mixin.client;

import net.minecraft.text.MutableText;
import net.minecraft.text.PlainTextContent;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.map.settings.ModOptions;
import xaero.map.settings.Option;
import xaeroplus.settings.XaeroPlusSettingsReflectionHax;

import static xaeroplus.settings.XaeroPlusSetting.SETTING_PREFIX;

@Mixin(value = Option.class, remap = false)
public class MixinWorldMapOption {
    @Mutable
    @Final
    @Shadow
    private Text caption;
    @Inject(method = "<init>", at = @At("RETURN"))
    public void constructorInject(final ModOptions option, final CallbackInfo ci) {
        if (option.getEnumString().startsWith(SETTING_PREFIX)) {
            XaeroPlusSettingsReflectionHax.XAERO_PLUS_WORLDMAP_SETTINGS.stream()
                    .filter(s -> s.getSettingName().equals(option.getEnumString()))
                    .findFirst()
                    .ifPresent(s -> caption = MutableText.of(new PlainTextContent.Literal(SETTING_PREFIX)).append(Text.translatable(s.getSettingNameTranslationKey())));
        }
    }
}
