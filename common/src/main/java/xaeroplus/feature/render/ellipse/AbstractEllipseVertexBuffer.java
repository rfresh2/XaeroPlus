package xaeroplus.feature.render.ellipse;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.CompiledShaderProgram;
import xaeroplus.feature.render.CachedVertexBuffer;
import xaeroplus.feature.render.DrawContext;
import xaeroplus.feature.render.shaders.XaeroPlusShaders;

public abstract class AbstractEllipseVertexBuffer<T> extends CachedVertexBuffer {
    protected int bufferOriginBlockX;
    protected int bufferOriginBlockZ;

    public void preRender(final DrawContext ctx, final T ellipses) {
        if (needsRefresh(ctx)) {
            refresh(ctx, ellipses);
        }
        XaeroPlusShaders.setEllipsesCameraRelative(
            bufferOriginBlockX,
            bufferOriginBlockZ,
            ctx.cameraBlockX(),
            ctx.cameraBlockZ()
        );
    }

    protected void setBufferOrigin(final DrawContext ctx) {
        bufferOriginBlockX = ctx.cameraBlockX();
        bufferOriginBlockZ = ctx.cameraBlockZ();
    }

    protected abstract void refresh(DrawContext ctx, T ellipses);

    @Override
    protected CompiledShaderProgram shaderInstance() {
        return Minecraft.getInstance()
            .getShaderManager()
            .getProgram(XaeroPlusShaders.ELLIPSES_SHADER_PROGRAM);
    }
}
