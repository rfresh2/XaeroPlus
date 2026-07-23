package xaeroplus.feature.render.shaders;

import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.CompiledShaderProgram;
import net.minecraft.client.renderer.ShaderDefines;
import net.minecraft.client.renderer.ShaderProgram;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import xaero.lib.client.graphics.shader.LibShaders;

public class XaeroPlusShaders {
    public static final ShaderProgram HIGHLIGHT_SHADER_PROGRAM = new ShaderProgram(
        ResourceLocation.fromNamespaceAndPath("xaeroplus", "highlights"),
        DefaultVertexFormat.POSITION,
        ShaderDefines.EMPTY
    );
    public static final ShaderProgram MULTI_COLOR_HIGHLIGHT_SHADER_PROGRAM = new ShaderProgram(
        ResourceLocation.fromNamespaceAndPath("xaeroplus", "multi_color_highlights"),
        DefaultVertexFormat.POSITION_COLOR,
        ShaderDefines.EMPTY
    );
    public static final ShaderProgram LINES_SHADER_PROGRAM = new ShaderProgram(
        ResourceLocation.fromNamespaceAndPath("xaeroplus", "lines"),
        DefaultVertexFormat.POSITION_TEX_COLOR,
        ShaderDefines.EMPTY
    );
    public static final ShaderProgram ELLIPSES_SHADER_PROGRAM = new ShaderProgram(
        ResourceLocation.fromNamespaceAndPath("xaeroplus", "ellipses"),
        DefaultVertexFormat.POSITION_TEX_COLOR,
        ShaderDefines.EMPTY
    );

    private static CompiledShaderProgram CACHED_HIGHLIGHT_SHADER_PROGRAM;
    private static CompiledShaderProgram CACHED_MULTI_COLOR_HIGHLIGHT_SHADER_PROGRAM;
    private static CompiledShaderProgram CACHED_CUSTOM_MAP_SHADER_PROGRAM;
    private static CompiledShaderProgram CACHED_LINES_SHADER_PROGRAM;
    private static CompiledShaderProgram CACHED_ELLIPSES_SHADER_PROGRAM;
    private static Uniform HIGHLIGHT_COLOR_UNIFORM;
    private static Uniform HIGHLIGHT_MAP_VIEW_MATRIX_UNIFORM;
    private static Uniform HIGHLIGHT_CAMERA_CHUNK_UNIFORM;
    private static Uniform HIGHLIGHT_CAMERA_IN_CHUNK_UNIFORM;
    private static Uniform MULTI_COLOR_HIGHLIGHT_MAP_VIEW_MATRIX_UNIFORM;
    private static Uniform MULTI_COLOR_HIGHLIGHT_CAMERA_CHUNK_UNIFORM;
    private static Uniform MULTI_COLOR_HIGHLIGHT_CAMERA_IN_CHUNK_UNIFORM;
    private static Uniform TRANSPARENT_BACKGROUND_UNIFORM;
    private static Uniform LINES_FRAME_SIZE_UNIFORM;
    private static Uniform LINES_MAP_VIEW_MATRIX_UNIFORM;
    private static Uniform LINES_CAMERA_RELATIVE_ORIGIN_UNIFORM;
    private static Uniform ELLIPSES_FRAME_SIZE_UNIFORM;
    private static Uniform ELLIPSES_MAP_VIEW_MATRIX_UNIFORM;
    private static Uniform ELLIPSES_CAMERA_RELATIVE_ORIGIN_UNIFORM;
    private static Uniform ELLIPSES_THICKNESS_UNIFORM;

    public static void setHighlightColor(float r, float g, float b, float a) {
        CompiledShaderProgram currentProgram = Minecraft.getInstance().getShaderManager().getProgram(HIGHLIGHT_SHADER_PROGRAM);
        if (currentProgram != CACHED_HIGHLIGHT_SHADER_PROGRAM) {
            CACHED_HIGHLIGHT_SHADER_PROGRAM = currentProgram;
            HIGHLIGHT_MAP_VIEW_MATRIX_UNIFORM = currentProgram.getUniform("MapViewMatrix");
            HIGHLIGHT_CAMERA_CHUNK_UNIFORM = currentProgram.getUniform("CameraChunk");
            HIGHLIGHT_CAMERA_IN_CHUNK_UNIFORM = currentProgram.getUniform("CameraInChunk");
            HIGHLIGHT_COLOR_UNIFORM = currentProgram.getUniform("HighlightColor");
        }

        HIGHLIGHT_COLOR_UNIFORM.set(r, g, b, a);
    }

    public static void setHighlightMapViewMatrix(Matrix4f matrix) {
        CompiledShaderProgram currentProgram = Minecraft.getInstance().getShaderManager().getProgram(HIGHLIGHT_SHADER_PROGRAM);
        if (currentProgram != CACHED_HIGHLIGHT_SHADER_PROGRAM) {
            CACHED_HIGHLIGHT_SHADER_PROGRAM = currentProgram;
            HIGHLIGHT_MAP_VIEW_MATRIX_UNIFORM = currentProgram.getUniform("MapViewMatrix");
            HIGHLIGHT_CAMERA_CHUNK_UNIFORM = currentProgram.getUniform("CameraChunk");
            HIGHLIGHT_CAMERA_IN_CHUNK_UNIFORM = currentProgram.getUniform("CameraInChunk");
            HIGHLIGHT_COLOR_UNIFORM = currentProgram.getUniform("HighlightColor");
        }

        HIGHLIGHT_MAP_VIEW_MATRIX_UNIFORM.set(matrix);
    }

    public static void setHighlightCameraPosition(final int cameraBlockX, final int cameraBlockZ) {
        CompiledShaderProgram currentProgram = Minecraft.getInstance().getShaderManager().getProgram(HIGHLIGHT_SHADER_PROGRAM);
        if (currentProgram != CACHED_HIGHLIGHT_SHADER_PROGRAM) {
            CACHED_HIGHLIGHT_SHADER_PROGRAM = currentProgram;
            HIGHLIGHT_MAP_VIEW_MATRIX_UNIFORM = currentProgram.getUniform("MapViewMatrix");
            HIGHLIGHT_CAMERA_CHUNK_UNIFORM = currentProgram.getUniform("CameraChunk");
            HIGHLIGHT_CAMERA_IN_CHUNK_UNIFORM = currentProgram.getUniform("CameraInChunk");
            HIGHLIGHT_COLOR_UNIFORM = currentProgram.getUniform("HighlightColor");
        }
        HIGHLIGHT_CAMERA_CHUNK_UNIFORM.set(
            (float) Math.floorDiv(cameraBlockX, 16),
            (float) Math.floorDiv(cameraBlockZ, 16)
        );
        HIGHLIGHT_CAMERA_IN_CHUNK_UNIFORM.set(
            (float) Math.floorMod(cameraBlockX, 16),
            (float) Math.floorMod(cameraBlockZ, 16)
        );
    }

    public static void setMultiColorMapViewMatrix(Matrix4f matrix) {
        CompiledShaderProgram currentProgram = Minecraft.getInstance().getShaderManager().getProgram(MULTI_COLOR_HIGHLIGHT_SHADER_PROGRAM);
        if (currentProgram != CACHED_MULTI_COLOR_HIGHLIGHT_SHADER_PROGRAM) {
            CACHED_MULTI_COLOR_HIGHLIGHT_SHADER_PROGRAM = currentProgram;
            MULTI_COLOR_HIGHLIGHT_MAP_VIEW_MATRIX_UNIFORM = currentProgram.getUniform("MapViewMatrix");
            MULTI_COLOR_HIGHLIGHT_CAMERA_CHUNK_UNIFORM = currentProgram.getUniform("CameraChunk");
            MULTI_COLOR_HIGHLIGHT_CAMERA_IN_CHUNK_UNIFORM = currentProgram.getUniform("CameraInChunk");
        }

        MULTI_COLOR_HIGHLIGHT_MAP_VIEW_MATRIX_UNIFORM.set(matrix);
    }

    public static void setMultiColorHighlightCameraPosition(final int cameraBlockX, final int cameraBlockZ) {
        CompiledShaderProgram currentProgram = Minecraft.getInstance().getShaderManager().getProgram(MULTI_COLOR_HIGHLIGHT_SHADER_PROGRAM);
        if (currentProgram != CACHED_HIGHLIGHT_SHADER_PROGRAM) {
            CACHED_HIGHLIGHT_SHADER_PROGRAM = currentProgram;
            MULTI_COLOR_HIGHLIGHT_MAP_VIEW_MATRIX_UNIFORM = currentProgram.getUniform("MapViewMatrix");
            MULTI_COLOR_HIGHLIGHT_CAMERA_CHUNK_UNIFORM = currentProgram.getUniform("CameraChunk");
            MULTI_COLOR_HIGHLIGHT_CAMERA_IN_CHUNK_UNIFORM = currentProgram.getUniform("CameraInChunk");
        }
        MULTI_COLOR_HIGHLIGHT_CAMERA_CHUNK_UNIFORM.set(
            (float) Math.floorDiv(cameraBlockX, 16),
            (float) Math.floorDiv(cameraBlockZ, 16)
        );
        MULTI_COLOR_HIGHLIGHT_CAMERA_IN_CHUNK_UNIFORM.set(
            (float) Math.floorMod(cameraBlockX, 16),
            (float) Math.floorMod(cameraBlockZ, 16)
        );
    }

    public static void ensureTransparentBackgroundUniforms() {
        CompiledShaderProgram currentProgram = Minecraft.getInstance().getShaderManager().getProgram(LibShaders.WORLD_MAP);
        if (currentProgram != CACHED_CUSTOM_MAP_SHADER_PROGRAM) {
            CACHED_CUSTOM_MAP_SHADER_PROGRAM = currentProgram;
            TRANSPARENT_BACKGROUND_UNIFORM = currentProgram.getUniform("TransparentBackground");
        }
    }

    public static void setTransparentBackground(boolean value) {
        ensureTransparentBackgroundUniforms();
        final int intValue = value ? 1 : 0;
        TRANSPARENT_BACKGROUND_UNIFORM.set(intValue);
    }

    public static void setLinesFrameSize(float width, float height) {
        CompiledShaderProgram currentProgram = Minecraft.getInstance().getShaderManager().getProgram(LINES_SHADER_PROGRAM);
        if (currentProgram != CACHED_LINES_SHADER_PROGRAM) {
            CACHED_LINES_SHADER_PROGRAM = currentProgram;
            LINES_FRAME_SIZE_UNIFORM = currentProgram.getUniform("FrameSize");
            LINES_MAP_VIEW_MATRIX_UNIFORM = currentProgram.getUniform("MapViewMatrix");
            LINES_CAMERA_RELATIVE_ORIGIN_UNIFORM = currentProgram.getUniform("CameraRelativeOrigin");
        }
        LINES_FRAME_SIZE_UNIFORM.set(width, height);
    }

    public static void setLinesWidth(float linesWidth) {
        CompiledShaderProgram currentProgram = Minecraft.getInstance().getShaderManager().getProgram(LINES_SHADER_PROGRAM);
        if (currentProgram != CACHED_LINES_SHADER_PROGRAM) {
            CACHED_LINES_SHADER_PROGRAM = currentProgram;
            LINES_MAP_VIEW_MATRIX_UNIFORM = currentProgram.getUniform("MapViewMatrix");
            LINES_CAMERA_RELATIVE_ORIGIN_UNIFORM = currentProgram.getUniform("CameraRelativeOrigin");
        }
        CACHED_LINES_SHADER_PROGRAM.LINE_WIDTH.set(linesWidth);
    }

    public static void setLinesMapViewMatrix(Matrix4f matrix) {
        CompiledShaderProgram currentProgram = Minecraft.getInstance().getShaderManager().getProgram(LINES_SHADER_PROGRAM);
        if (currentProgram != CACHED_LINES_SHADER_PROGRAM) {
            CACHED_LINES_SHADER_PROGRAM = currentProgram;
            LINES_MAP_VIEW_MATRIX_UNIFORM = currentProgram.getUniform("MapViewMatrix");
            LINES_CAMERA_RELATIVE_ORIGIN_UNIFORM = currentProgram.getUniform("CameraRelativeOrigin");
        }
        LINES_MAP_VIEW_MATRIX_UNIFORM.set(matrix);
    }

    public static void setLinesCameraRelative(
        final int bufferOriginBlockX,
        final int bufferOriginBlockZ,
        final int cameraBlockX,
        final int cameraBlockZ
    ) {
        CompiledShaderProgram currentProgram = Minecraft.getInstance().getShaderManager().getProgram(LINES_SHADER_PROGRAM);
        if (currentProgram != CACHED_LINES_SHADER_PROGRAM) {
            CACHED_LINES_SHADER_PROGRAM = currentProgram;
            LINES_MAP_VIEW_MATRIX_UNIFORM = currentProgram.getUniform("MapViewMatrix");
            LINES_CAMERA_RELATIVE_ORIGIN_UNIFORM = currentProgram.getUniform("CameraRelativeOrigin");
        }
        LINES_CAMERA_RELATIVE_ORIGIN_UNIFORM.set(
            (float) ((long) bufferOriginBlockX - cameraBlockX),
            (float) ((long) bufferOriginBlockZ - cameraBlockZ)
        );
    }

    private static void ensureEllipsesUniforms() {
        var currentProgram = Minecraft.getInstance().getShaderManager().getProgram(ELLIPSES_SHADER_PROGRAM);
        if (currentProgram != CACHED_ELLIPSES_SHADER_PROGRAM) {
            CACHED_ELLIPSES_SHADER_PROGRAM = currentProgram;
            ELLIPSES_FRAME_SIZE_UNIFORM = currentProgram.getUniform("FrameSize");
            ELLIPSES_MAP_VIEW_MATRIX_UNIFORM = currentProgram.getUniform("MapViewMatrix");
            ELLIPSES_CAMERA_RELATIVE_ORIGIN_UNIFORM = currentProgram.getUniform("CameraRelativeOrigin");
            ELLIPSES_THICKNESS_UNIFORM = currentProgram.getUniform("Thickness");
        }
    }

    public static void setEllipsesFrameSize(final float width, final float height) {
        ensureEllipsesUniforms();
        ELLIPSES_FRAME_SIZE_UNIFORM.set(width, height);
    }

    public static void setEllipsesThickness(final float thickness) {
        ensureEllipsesUniforms();
        ELLIPSES_THICKNESS_UNIFORM.set(thickness);
    }

    public static void setEllipsesMapViewMatrix(final Matrix4f matrix) {
        ensureEllipsesUniforms();
        ELLIPSES_MAP_VIEW_MATRIX_UNIFORM.set(matrix);
    }

    public static void setEllipsesCameraRelative(
        final int bufferOriginBlockX,
        final int bufferOriginBlockZ,
        final int cameraBlockX,
        final int cameraBlockZ
    ) {
        ensureEllipsesUniforms();
        ELLIPSES_CAMERA_RELATIVE_ORIGIN_UNIFORM.set(
            (float) ((long) bufferOriginBlockX - cameraBlockX),
            (float) ((long) bufferOriginBlockZ - cameraBlockZ)
        );
    }

    public static void setFrameSize(final float width, final float height) {
        setLinesFrameSize(width, height);
        setEllipsesFrameSize(width, height);
    }
}
