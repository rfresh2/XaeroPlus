package xaeroplus.feature.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Matrix4f;
import net.minecraft.client.renderer.MultiBufferSource;

public record DrawContext(
    PoseStack matrixStack,
    MultiBufferSource.BufferSource renderTypeBuffers,
    double fboScale,
    boolean worldmap,
    Matrix4f untranslatedMapViewMatrix,
    int cameraBlockX,
    int cameraBlockZ
) { }
