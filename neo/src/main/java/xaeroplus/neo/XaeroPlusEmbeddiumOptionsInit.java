package xaeroplus.neo;

import com.google.common.collect.ImmutableList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.embeddedt.embeddium.api.OptionGUIConstructionEvent;
import org.embeddedt.embeddium.api.options.OptionIdentifier;
import org.embeddedt.embeddium.api.options.binding.GenericBinding;
import org.embeddedt.embeddium.api.options.control.ControlValueFormatter;
import org.embeddedt.embeddium.api.options.control.SliderControl;
import org.embeddedt.embeddium.api.options.control.TickBoxControl;
import org.embeddedt.embeddium.api.options.structure.OptionGroup;
import org.embeddedt.embeddium.api.options.structure.OptionImpl;
import org.embeddedt.embeddium.api.options.structure.OptionPage;
import xaeroplus.settings.Settings;

public class XaeroPlusEmbeddiumOptionsInit {
    public static void onEmbeddiumOptionGUIConstructionEvent(OptionGUIConstructionEvent event) {
        if (!Settings.REGISTRY.sodiumSettingIntegration.get()) return;
        event.addPage(new OptionPage(OptionIdentifier.create("xaeroplus", "options"), Component.literal("XaeroPlus"), ImmutableList.<OptionGroup>of(
            OptionGroup.createBuilder()
                .setId(ResourceLocation.tryBuild("xaeroplus", "option-group"))
                .add(OptionImpl.createBuilder(Boolean.TYPE, XaeroPlusEmbeddiumOptionStorage.INSTANCE)
                         .setId(ResourceLocation.tryBuild("xaeroplus", "minimap-fps-limiter-enabled"))
                         .setName(Component.translatable("xaeroplus.setting.fps_limiter"))
                         .setTooltip(Component.translatable("xaeroplus.setting.fps_limiter.tooltip"))
                         .setControl(TickBoxControl::new)
                         .setBinding(new GenericBinding<>(
                             (config, value) -> Settings.REGISTRY.minimapFpsLimiter.setValue(value),
                             config -> Settings.REGISTRY.minimapFpsLimiter.get()
                         ))
                         .build())
                .add(OptionImpl.createBuilder(int.class, XaeroPlusEmbeddiumOptionStorage.INSTANCE)
                         .setId(ResourceLocation.tryBuild("xaeroplus", "minimap-fps-limit"))
                         .setName(Component.translatable("xaeroplus.setting.fps_limiter_limit"))
                         .setTooltip(Component.translatable("xaeroplus.setting.fps_limiter_limit.tooltip"))
                         .setControl(option -> new SliderControl(
                             option,
                             (int) Settings.REGISTRY.minimapFpsLimit.getValueMin(),
                             (int) Settings.REGISTRY.minimapFpsLimit.getValueMax(),
                             (int) Settings.REGISTRY.minimapFpsLimit.getValueStep(),
                             ControlValueFormatter.number()))
                         .setBinding(new GenericBinding<>(
                             (config, value) -> Settings.REGISTRY.minimapFpsLimit.setValue(value),
                             config -> Settings.REGISTRY.minimapFpsLimit.getAsInt()
                         ))
                         .build())
                .add(OptionImpl.createBuilder(int.class, XaeroPlusEmbeddiumOptionStorage.INSTANCE)
                         .setId(ResourceLocation.tryBuild("xaeroplus", "minimap-scaling-factor"))
                         .setName(Component.translatable("xaeroplus.setting.fps_limiter_limit"))
                         .setTooltip(Component.translatable("xaeroplus.setting.fps_limiter_limit.tooltip"))
                         .setControl(option -> new SliderControl(
                             option,
                             (int) Settings.REGISTRY.minimapScaleMultiplierSetting.getValueMin(),
                             (int) Settings.REGISTRY.minimapScaleMultiplierSetting.getValueMax(),
                             (int) Settings.REGISTRY.minimapScaleMultiplierSetting.getValueStep(),
                             ControlValueFormatter.number()))
                         .setBinding(new GenericBinding<>(
                             (config, value) -> Settings.REGISTRY.minimapScaleMultiplierSetting.setValue(value),
                             config -> Settings.REGISTRY.minimapScaleMultiplierSetting.getAsInt()
                         ))
                         .build())
                .build()
        )));
    }
}
