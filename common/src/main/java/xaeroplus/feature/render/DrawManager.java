package xaeroplus.feature.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.lenni0451.lambdaevents.EventHandler;
import net.minecraft.client.renderer.MultiBufferSource;
import xaero.common.HudMod;
import xaeroplus.XaeroPlus;
import xaeroplus.event.XaeroWorldChangeEvent;
import xaeroplus.module.impl.TickTaskExecutor;

public class DrawManager {
    private final DrawFeatureRegistry registry = new DrawFeatureRegistry();

    public final DrawFeatureRegistry registry() {
        return registry;
    }

    public DrawManager() {
        XaeroPlus.EVENT_BUS.register(this);
    }

    @EventHandler
    public void onXaeroWorldChange(XaeroWorldChangeEvent event) {
        TickTaskExecutor.INSTANCE.execute(() -> {
            registry.forEach(DrawFeature::invalidateCache);
        });
    }

    public void drawMinimapFeatures(
        int chunkX,
        int chunkZ,
        int tileX,
        int tileZ,
        int insideX,
        int insideZ,
        final PoseStack matrixStack,
        final MultiBufferSource.BufferSource renderTypeBuffers
    ) {
        if (HudMod.INSTANCE.isFairPlay()) return;
        var ctx = new DrawContext(matrixStack, renderTypeBuffers, 1.0, false);
        matrixStack.pushPose();
        matrixStack.translate(
            -(chunkX * 64) - (tileX * 16) - insideX,
            -(chunkZ * 64) - (tileZ * 16) - insideZ,
            0);
        registry.forEach(feature -> {
            feature.render(ctx);
        });
        matrixStack.popPose();
    }

    public void drawWorldMapFeatures(
        final int flooredCameraX,
        final int flooredCameraZ,
        final PoseStack matrixStack,
        final double fboScale,
        final MultiBufferSource.BufferSource renderTypeBuffers
    ) {
        if (HudMod.INSTANCE.isFairPlay()) return;
        var ctx = new DrawContext(matrixStack, renderTypeBuffers, fboScale, true);
        matrixStack.pushPose();
        matrixStack.translate(-flooredCameraX, -flooredCameraZ, 1.0f);
        registry.forEach(feature -> {
            feature.render(ctx);
        });
        matrixStack.popPose();
    }
}
