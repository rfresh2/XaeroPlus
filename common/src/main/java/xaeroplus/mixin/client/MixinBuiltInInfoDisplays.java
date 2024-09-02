package xaeroplus.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import xaero.common.minimap.info.BuiltInInfoDisplays;
import xaero.common.minimap.info.render.compile.InfoDisplayCompiler;
import xaero.hud.minimap.world.MinimapWorld;
import xaeroplus.settings.XaeroPlusSettingRegistry;
import xaeroplus.util.ChunkUtils;

@Mixin(value = BuiltInInfoDisplays.class, remap = false)
public class MixinBuiltInInfoDisplays {

    @WrapOperation(method = "lambda$static$13", at = @At(
        value = "INVOKE",
        target = "Lxaero/common/minimap/info/render/compile/InfoDisplayCompiler;addWords(ILjava/lang/String;)V"))
    private static void hideAutoSubworldInfoWhenOwAutoWaypointsEnabled(final InfoDisplayCompiler instance, final int lineWidth, final String text, final Operation<Void> original,
                                                                       @Local(name = "currentWorld") MinimapWorld currentWorld) {
        if (XaeroPlusSettingRegistry.owAutoWaypointDimension.getValue()) {
            ResourceKey<Level> actualDimension = ChunkUtils.getActualDimension();
            if (actualDimension == Level.NETHER && currentWorld.getDimId() == Level.OVERWORLD) return;
        }
        original.call(instance, lineWidth, text);
    }
}
