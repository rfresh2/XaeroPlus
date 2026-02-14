package xaeroplus.feature.render.shaders;

import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.server.packs.resources.ResourceProvider;

import java.io.IOException;

public class LinesShader extends ShaderInstance {
    private Uniform frameSize = this.getUniform("FrameSize");

    public LinesShader(ResourceProvider factory) throws IOException {
        super(factory, "xaeroplus/lines", DefaultVertexFormat.POSITION_TEX_COLOR);
    }

    public void setFrameSize(float width, float height) {
        if (this.frameSize.getFloatBuffer().get(0) != width || this.frameSize.getFloatBuffer().get(1) != height) {
            this.frameSize.set(width, height);
        }

    }
}
