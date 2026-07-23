package xaeroplus.feature.render.shaders;

import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.server.packs.resources.ResourceProvider;
import org.joml.Matrix4f;
import xaeroplus.XaeroPlus;

import java.io.IOException;

public class EllipsesShader extends ShaderInstance {
    private final Uniform frameSize = getUniform("FrameSize");
    private final Uniform mapViewMatrix = getUniform("MapViewMatrix");
    private final Uniform cameraRelativeOrigin = getUniform("CameraRelativeOrigin");
    private final Uniform thickness = getUniform("Thickness");

    public EllipsesShader(final ResourceProvider resourceProvider) throws IOException {
        super(resourceProvider, "xaeroplus/ellipses", DefaultVertexFormat.POSITION_TEX_COLOR);
    }

    public void setFrameSize(final float width, final float height) {
        if (frameSize == null) {
            XaeroPlus.LOGGER.error("Ellipse shader FrameSize uniform is null");
            return;
        }
        if (frameSize.getFloatBuffer().get(0) != width || frameSize.getFloatBuffer().get(1) != height) {
            frameSize.set(width, height);
        }
    }

    public void setMapViewMatrix(final Matrix4f transform) {
        if (mapViewMatrix == null) {
            XaeroPlus.LOGGER.error("Ellipse shader MapViewMatrix uniform is null");
            return;
        }
        mapViewMatrix.set(transform);
    }

    public void setCameraRelativeOrigin(
        final int bufferOriginBlockX,
        final int bufferOriginBlockZ,
        final int cameraBlockX,
        final int cameraBlockZ
    ) {
        if (cameraRelativeOrigin == null) {
            XaeroPlus.LOGGER.error("Ellipse shader CameraRelativeOrigin uniform is null");
            return;
        }
        cameraRelativeOrigin.set(
            (float) ((long) bufferOriginBlockX - cameraBlockX),
            (float) ((long) bufferOriginBlockZ - cameraBlockZ)
        );
    }

    public void setThickness(final float value) {
        if (thickness == null) {
            XaeroPlus.LOGGER.error("Ellipse shader Thickness uniform is null");
            return;
        }
        thickness.set(value);
    }
}
