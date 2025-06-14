package xaeroplus.feature.render;

import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.math.Matrix4f;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.server.packs.resources.ResourceProvider;
import xaeroplus.XaeroPlus;

import java.io.IOException;

public class MultiColorHighlightShader extends ShaderInstance {
    public final Uniform mapViewMatrix = getUniform("MapViewMatrix");

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
}
