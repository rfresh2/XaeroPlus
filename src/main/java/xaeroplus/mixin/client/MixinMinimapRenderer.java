package xaeroplus.mixin.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import xaero.common.AXaeroMinimap;
import xaero.common.minimap.MinimapProcessor;
import xaero.common.minimap.radar.MinimapRadar;
import xaero.common.minimap.render.MinimapFBORenderer;
import xaero.common.minimap.render.MinimapRenderer;
import xaero.common.settings.ModSettings;
import xaeroplus.settings.XaeroPlusSettingRegistry;

@Mixin(value = MinimapRenderer.class, remap = false)
public class MixinMinimapRenderer {
    @Shadow
    protected AXaeroMinimap modMain;

    /**
     * Inspiration for the below mods came from: https://github.com/Abbie5/xaeroarrowfix
     */

    @Redirect(method = "renderMinimap", at = @At(value = "INVOKE", target = "Lxaero/common/minimap/render/MinimapFBORenderer;renderMainEntityDot(Lnet/minecraft/client/gui/DrawContext;Lxaero/common/minimap/MinimapProcessor;Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/entity/Entity;DDDDFLxaero/common/minimap/radar/MinimapRadar;ZIZZZDLxaero/common/settings/ModSettings;Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;F)V"))
    public void redirectRenderMainEntityDot(final MinimapFBORenderer instance,
                                            final DrawContext guiGraphics,
                                            final MinimapProcessor minimap,
                                            final PlayerEntity p,
                                            final Entity renderEntity,
                                            final double ps,
                                            final double pc,
                                            final double playerX,
                                            final double playerZ,
                                            final float partial,
                                            final MinimapRadar minimapRadar,
                                            final boolean lockedNorth,
                                            final int style,
                                            final boolean smooth,
                                            final boolean debug,
                                            final boolean cave,
                                            final double dotNameScale,
                                            final ModSettings settings,
                                            final VertexConsumerProvider.Immediate renderTypeBuffers,
                                            final float minimapScale) {
        if (XaeroPlusSettingRegistry.fixMainEntityDot.getValue()) {
            if (!(modMain.getSettings().mainEntityAs != 2 && !lockedNorth)) {
                return;
            }
        }
        instance.renderMainEntityDot(
                guiGraphics,
                minimap,
                p,
                renderEntity,
                ps,
                pc,
                playerX,
                playerZ,
                partial,
                minimapRadar,
                lockedNorth,
                style,
                smooth,
                debug,
                cave,
                dotNameScale,
                settings,
                renderTypeBuffers,
                minimapScale
        );
    }

    @ModifyVariable(method = "drawArrow", name = "offsetY", ordinal = 0, at = @At(value = "STORE"))
    public int modifyArrowOffsetY(final int offsetY) {
        return XaeroPlusSettingRegistry.fixMainEntityDot.getValue() ? -10 : offsetY;
    }
}
