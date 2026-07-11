package xaeroplus.feature.render.shaders;

import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.math.Matrix4f;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.server.packs.resources.ResourceProvider;
import xaeroplus.XaeroPlus;

import java.io.IOException;

public class MultiColorHighlightShader extends ShaderInstance {
    public final Uniform mapViewMatrix = getUniform("MapViewMatrix");
    public final Uniform cameraChunk = getUniform("CameraChunk");
    public final Uniform cameraInChunk = getUniform("CameraInChunk");

    public MultiColorHighlightShader(final ResourceProvider resourceProvider) throws IOException {
        super(resourceProvider, "xaeroplus/multi_color_highlights", DefaultVertexFormat.POSITION_COLOR);
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
            XaeroPlus.LOGGER.error("Multi-color highlight camera position uniform is null");
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
