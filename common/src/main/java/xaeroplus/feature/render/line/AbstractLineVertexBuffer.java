package xaeroplus.feature.render.line;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.CompiledShaderProgram;
import xaeroplus.feature.render.CachedVertexBuffer;
import xaeroplus.feature.render.DrawContext;
import xaeroplus.feature.render.shaders.XaeroPlusShaders;

public abstract class AbstractLineVertexBuffer<T> extends CachedVertexBuffer {

    public void preRender(final DrawContext ctx, final T lines) {
        if (needsRefresh(ctx)) {
            refresh(ctx, lines);
        }
    }

    protected abstract void refresh(DrawContext ctx, T lines);

    protected CompiledShaderProgram shaderInstance() {
        return Minecraft.getInstance()
            .getShaderManager()
            .getProgram(XaeroPlusShaders.LINES_SHADER_PROGRAM);
    }
}
