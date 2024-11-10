package xaeroplus.feature.render;

import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.CompiledShaderProgram;
import net.minecraft.client.renderer.ShaderDefines;
import net.minecraft.client.renderer.ShaderProgram;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

public class XaeroPlusShaders {
    public static final ShaderProgram HIGHLIGHT_SHADER_PROGRAM = new ShaderProgram(
        ResourceLocation.fromNamespaceAndPath("xaeroplus", "highlights"),
        DefaultVertexFormat.POSITION,
        ShaderDefines.EMPTY
    );

    private static CompiledShaderProgram CACHED_HIGHLIGHT_SHADER_PROGRAM;
    private static Uniform HIGHLIGHT_COLOR_UNIFORM;
    private static Uniform HIGHLIGHT_MAP_VIEW_MATRIX_UNIFORM;

    public static void setHighlightColor(float r, float g, float b, float a) {
        CompiledShaderProgram currentProgram = Minecraft.getInstance().getShaderManager().getProgram(HIGHLIGHT_SHADER_PROGRAM);
        if (currentProgram != CACHED_HIGHLIGHT_SHADER_PROGRAM) {
            CACHED_HIGHLIGHT_SHADER_PROGRAM = currentProgram;
            HIGHLIGHT_MAP_VIEW_MATRIX_UNIFORM = currentProgram.getUniform("MapViewMatrix");
            HIGHLIGHT_COLOR_UNIFORM = currentProgram.getUniform("HighlightColor");
        }

        HIGHLIGHT_COLOR_UNIFORM.set(r, g, b, a);
    }

    public static void setMapViewMatrix(Matrix4f matrix) {
        CompiledShaderProgram currentProgram = Minecraft.getInstance().getShaderManager().getProgram(HIGHLIGHT_SHADER_PROGRAM);
        if (currentProgram != CACHED_HIGHLIGHT_SHADER_PROGRAM) {
            CACHED_HIGHLIGHT_SHADER_PROGRAM = currentProgram;
            HIGHLIGHT_MAP_VIEW_MATRIX_UNIFORM = currentProgram.getUniform("MapViewMatrix");
            HIGHLIGHT_COLOR_UNIFORM = currentProgram.getUniform("HighlightColor");
        }

        HIGHLIGHT_MAP_VIEW_MATRIX_UNIFORM.set(matrix);
    }
}
