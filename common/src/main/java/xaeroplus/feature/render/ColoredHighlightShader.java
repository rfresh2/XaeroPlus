package xaeroplus.feature.render;

import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.server.packs.resources.ResourceProvider;
import org.joml.Matrix4f;
import xaeroplus.XaeroPlus;

import java.io.IOException;

public class ColoredHighlightShader extends ShaderInstance {
    public final Uniform mapViewMatrix = getUniform("MapViewMatrix");

    public ColoredHighlightShader(final ResourceProvider resourceProvider) throws IOException {
        super(resourceProvider, "xaeroplus/colored_highlights", DefaultVertexFormat.POSITION_COLOR);
    }

    public void setMapViewMatrix(Matrix4f transform) {
        if (mapViewMatrix == null) {
            XaeroPlus.LOGGER.error("mapViewMatrix is null");
            return;
        }
        mapViewMatrix.set(transform);
    }
}
