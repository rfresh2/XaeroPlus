package xaeroplus.fabric.mixin.client;

import net.caffeinemc.mods.sodium.client.gui.SodiumOptionsGUI;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

@Pseudo
@Mixin(value = SodiumOptionsGUI.class, remap = false)
public class MixinSodium6OptionsGUI {

    // todo: re-enable after porting fps limiter
//    @Final
//    @Shadow
//    private List<OptionPage> pages;
//
//    @Inject(method = "<init>", at = @At(
//        value = "RETURN"
//    ))
//    public void injectXPSettings(final Screen prevScreen, final CallbackInfo ci) {
//        if (!Settings.REGISTRY.sodiumSettingIntegration.get()) return;
//        pages.add(new OptionPage(Component.literal("XaeroPlus"), ImmutableList.<OptionGroup>of(
//            OptionGroup.createBuilder()
//                .add(OptionImpl.createBuilder(Boolean.TYPE, XaeroPlusSodium6OptionStorage.INSTANCE)
//                         .setName(Component.translatable("xaeroplus.setting.fps_limiter"))
//                         .setTooltip(Component.translatable("xaeroplus.setting.fps_limiter.tooltip"))
//                         .setControl(TickBoxControl::new)
//                         .setBinding(new GenericBinding<>(
//                             (config, value) -> Settings.REGISTRY.minimapFpsLimiter.setValue(value),
//                             config -> Settings.REGISTRY.minimapFpsLimiter.get()
//                         ))
//                         .build())
//                .add(OptionImpl.createBuilder(int.class, XaeroPlusSodium6OptionStorage.INSTANCE)
//                         .setName(Component.translatable("xaeroplus.setting.fps_limiter_limit"))
//                         .setTooltip(Component.translatable("xaeroplus.setting.fps_limiter_limit.tooltip"))
//                         .setControl(option -> new SliderControl(
//                             option,
//                             (int) Settings.REGISTRY.minimapFpsLimit.getValueMin(),
//                             (int) Settings.REGISTRY.minimapFpsLimit.getValueMax(),
//                             (int) Settings.REGISTRY.minimapFpsLimit.getValueStep(),
//                             ControlValueFormatter.number()))
//                         .setBinding(new GenericBinding<>(
//                             (config, value) -> Settings.REGISTRY.minimapFpsLimit.setValue(value),
//                             config -> Settings.REGISTRY.minimapFpsLimit.getAsInt()
//                         ))
//                         .build())
//                .add(OptionImpl.createBuilder(int.class, XaeroPlusSodium6OptionStorage.INSTANCE)
//                         .setName(Component.translatable("xaeroplus.setting.minimap_scaling"))
//                         .setTooltip(Component.translatable("xaeroplus.setting.minimap_scaling.tooltip"))
//                         .setControl(option -> new SliderControl(
//                             option,
//                             (int) Settings.REGISTRY.minimapScaleMultiplierSetting.getValueMin(),
//                             (int) Settings.REGISTRY.minimapScaleMultiplierSetting.getValueMax(),
//                             (int) Settings.REGISTRY.minimapScaleMultiplierSetting.getValueStep(),
//                             ControlValueFormatter.number()))
//                         .setBinding(new GenericBinding<>(
//                             (config, value) -> Settings.REGISTRY.minimapScaleMultiplierSetting.setValue(value),
//                             config -> Settings.REGISTRY.minimapScaleMultiplierSetting.getAsInt()
//                         ))
//                         .build())
//                .build()
//        )));
//    }
}
