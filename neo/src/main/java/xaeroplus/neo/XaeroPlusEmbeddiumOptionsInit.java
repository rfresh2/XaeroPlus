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
import xaeroplus.settings.XaeroPlusSettingRegistry;

public class XaeroPlusEmbeddiumOptionsInit {
    public static void onEmbeddiumOptionGUIConstructionEvent(OptionGUIConstructionEvent event) {
        event.addPage(new OptionPage(OptionIdentifier.create("xaeroplus", "options"), Component.literal("XaeroPlus"), ImmutableList.<OptionGroup>of(
            OptionGroup.createBuilder()
                .setId(ResourceLocation.tryBuild("xaeroplus", "option-group"))
                .add(OptionImpl.createBuilder(Boolean.TYPE, XaeroPlusEmbeddiumOptionStorage.INSTANCE)
                         .setId(ResourceLocation.tryBuild("xaeroplus", "minimap-fps-limiter-enabled"))
                         .setName(Component.translatable("setting.minimap.fps_limiter"))
                         .setTooltip(Component.translatable("setting.minimap.fps_limiter.tooltip"))
                         .setControl(TickBoxControl::new)
                         .setBinding(new GenericBinding<>(
                             (config, value) -> XaeroPlusSettingRegistry.minimapFpsLimiter.setValue(value),
                             config -> XaeroPlusSettingRegistry.minimapFpsLimiter.getValue()
                         ))
                         .build())
                .add(OptionImpl.createBuilder(int.class, XaeroPlusEmbeddiumOptionStorage.INSTANCE)
                         .setId(ResourceLocation.tryBuild("xaeroplus", "minimap-fps-limit"))
                         .setName(Component.translatable("setting.minimap.fps_limiter_limit"))
                         .setTooltip(Component.translatable("setting.minimap.fps_limiter_limit.tooltip"))
                         .setControl(option -> new SliderControl(
                             option,
                             (int) XaeroPlusSettingRegistry.minimapFpsLimit.getValueMin(),
                             (int) XaeroPlusSettingRegistry.minimapFpsLimit.getValueMax(),
                             (int) XaeroPlusSettingRegistry.minimapFpsLimit.getValueStep(),
                             ControlValueFormatter.number()))
                         .setBinding(new GenericBinding<>(
                             (config, value) -> XaeroPlusSettingRegistry.minimapFpsLimit.setValue((float) value),
                             config -> (int) XaeroPlusSettingRegistry.minimapFpsLimit.getValue()
                         ))
                         .build())
                .build()
        )));
    }
}
