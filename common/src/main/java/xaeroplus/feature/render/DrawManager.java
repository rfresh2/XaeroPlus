package xaeroplus.feature.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;
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
        final double zoom,
        final PoseStack matrixStack,
        final MultiBufferSource.BufferSource renderTypeBuffers
    ) {
        if (HudMod.INSTANCE.isFairPlay()) return;
        var cameraBlockX = chunkX * 64 + tileX * 16 + insideX;
        var cameraBlockZ = chunkZ * 64 + tileZ * 16 + insideZ;
        var ctx = new DrawContext(
            matrixStack,
            renderTypeBuffers,
            zoom,
            false,
            new Matrix4f(matrixStack.last().pose()),
            cameraBlockX,
            cameraBlockZ
        );
        matrixStack.pushPose();
        matrixStack.translate(-cameraBlockX, -cameraBlockZ, 0);
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
        var untranslatedMapViewMatrix = new Matrix4f(matrixStack.last().pose());
        untranslatedMapViewMatrix.translate(new Vector3f(0.0f, 0.0f, 1.0f));
        var ctx = new DrawContext(
            matrixStack,
            renderTypeBuffers,
            fboScale,
            true,
            untranslatedMapViewMatrix,
            flooredCameraX,
            flooredCameraZ
        );
        matrixStack.pushPose();
        matrixStack.translate(-flooredCameraX, -flooredCameraZ, 1.0f);
        registry.forEach(feature -> {
            feature.render(ctx);
        });
        matrixStack.popPose();
    }
}
