package xaeroplus.feature.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import org.joml.Matrix4f;

public record DrawContext(
    PoseStack matrixStack,
    MultiBufferSource.BufferSource renderTypeBuffers,
    double fboScale,
    boolean worldmap,
    Matrix4f untranslatedMapViewMatrix,
    int cameraBlockX,
    int cameraBlockZ
) { }
