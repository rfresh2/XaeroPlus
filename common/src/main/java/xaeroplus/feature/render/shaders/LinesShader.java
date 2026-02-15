package xaeroplus.feature.render.shaders;

import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.server.packs.resources.ResourceProvider;
import org.joml.Matrix4f;
import xaeroplus.XaeroPlus;

import java.io.IOException;

public class LinesShader extends ShaderInstance {
    private Uniform frameSize = this.getUniform("FrameSize");
    private Uniform mapViewMatrix = this.getUniform("MapViewMatrix");

    public LinesShader(ResourceProvider factory) throws IOException {
        super(factory, "xaeroplus/lines", DefaultVertexFormat.POSITION_COLOR_TEX);
    }

    public void setFrameSize(float width, float height) {
        if (this.frameSize.getFloatBuffer().get(0) != width || this.frameSize.getFloatBuffer().get(1) != height) {
            this.frameSize.set(width, height);
        }

    }

    public void setMapViewMatrix(final Matrix4f transform) {
        if (mapViewMatrix == null) {
            XaeroPlus.LOGGER.error("mapViewMatrix is null");
            return;
        }
        mapViewMatrix.set(transform);
    }
}
