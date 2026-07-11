package xaeroplus.feature.render.shaders;

import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.server.packs.resources.ResourceProvider;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import xaeroplus.XaeroPlus;

import java.io.IOException;

public class HighlightShader extends ShaderInstance {
    public final Uniform highlightColor = getUniform("HighlightColor");
    public final Uniform mapViewMatrix = getUniform("MapViewMatrix");
    public final Uniform cameraChunk = getUniform("CameraChunk");
    public final Uniform cameraInChunk = getUniform("CameraInChunk");

    public HighlightShader(final ResourceProvider resourceProvider) throws IOException {
        super(resourceProvider, "xaeroplus/highlights", DefaultVertexFormat.POSITION);
    }

    public void setHighlightColor(float r, float g, float b, float a) {
        if (highlightColor == null) {
            XaeroPlus.LOGGER.error("highlightColor uniform is null");
            return;
        }
        highlightColor.set(new Vector4f(r, g, b, a));
    }

    public void setMapViewMatrix(Matrix4f transform) {
        if (mapViewMatrix == null) {
            XaeroPlus.LOGGER.error("mapViewMatrix is null");
            return;
        }
        mapViewMatrix.set(transform);
    }

    public void setCameraPosition(final int cameraBlockX, final int cameraBlockZ) {
        if (cameraChunk == null || cameraInChunk == null) {
            XaeroPlus.LOGGER.error("Highlight camera position uniform is null");
            return;
        }
        cameraChunk.set(
            (float) Math.floorDiv(cameraBlockX, 16),
            (float) Math.floorDiv(cameraBlockZ, 16)
        );
        cameraInChunk.set(
            (float) Math.floorMod(cameraBlockX, 16),
            (float) Math.floorMod(cameraBlockZ, 16)
        );
    }
}
