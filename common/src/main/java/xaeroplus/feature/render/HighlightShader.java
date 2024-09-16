package xaeroplus.feature.render;

import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector4f;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.server.packs.resources.ResourceProvider;
import xaeroplus.XaeroPlus;

import java.io.IOException;

public class HighlightShader extends ShaderInstance {
    public final Uniform highlightColor = getUniform("HighlightColor");
    public final Uniform worldMapViewMatrix = getUniform("WorldMapViewMatrix");

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

    public void setWorldMapViewMatrix(Matrix4f transform) {
        if (worldMapViewMatrix == null) {
            XaeroPlus.LOGGER.error("worldmapTransform is null");
            return;
        }
        worldMapViewMatrix.set(transform);
    }
}
