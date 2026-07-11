package xaeroplus.feature.render;

import com.mojang.blaze3d.vertex.PoseStack;
import xaero.lib.client.graphics.XaeroBufferProvider;
import org.joml.Matrix4f;

public record DrawContext(
    PoseStack matrixStack,
    XaeroBufferProvider renderTypeBuffers,
    double fboScale,
    boolean worldmap,
    Matrix4f untranslatedMapViewMatrix,
    int cameraBlockX,
    int cameraBlockZ
) { }
