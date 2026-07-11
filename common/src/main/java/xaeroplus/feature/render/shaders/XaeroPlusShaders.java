package xaeroplus.feature.render.shaders;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DestFactor;
import com.mojang.blaze3d.platform.PolygonMode;
import com.mojang.blaze3d.platform.SourceFactor;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.resources.ResourceLocation;

public class XaeroPlusShaders {
    public static final RenderPipeline HIGHLIGHT_PIPELINE = RenderPipeline.builder()
        .withLocation(ResourceLocation.fromNamespaceAndPath("xaeroplus", "pipeline/highlights"))
        .withVertexShader(ResourceLocation.fromNamespaceAndPath("xaeroplus", "highlights"))
        .withFragmentShader(ResourceLocation.fromNamespaceAndPath("xaeroplus", "highlights"))
        .withVertexFormat(DefaultVertexFormat.POSITION, VertexFormat.Mode.QUADS)
        .withPolygonMode(PolygonMode.FILL)
        .withUniform("ModelViewMat", UniformType.MATRIX4X4)
        .withUniform("ProjMat", UniformType.MATRIX4X4)
        .withUniform("MapViewMatrix", UniformType.MATRIX4X4)
        .withUniform("HighlightColor", UniformType.VEC4)
        .withUniform("CameraChunk", UniformType.VEC2)
        .withUniform("CameraInChunk", UniformType.VEC2)
        .withBlend(new BlendFunction(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ONE_MINUS_SRC_ALPHA))
         .build();

    public static final RenderPipeline MULTI_COLOR_HIGHLIGHT_PIPELINE = RenderPipeline.builder()
        .withLocation(ResourceLocation.fromNamespaceAndPath("xaeroplus", "pipeline/multi_color_highlights"))
        .withVertexShader(ResourceLocation.fromNamespaceAndPath("xaeroplus", "multi_color_highlights"))
        .withFragmentShader(ResourceLocation.fromNamespaceAndPath("xaeroplus", "multi_color_highlights"))
        .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS)
        .withPolygonMode(PolygonMode.FILL)
        .withUniform("ModelViewMat", UniformType.MATRIX4X4)
        .withUniform("ProjMat", UniformType.MATRIX4X4)
        .withUniform("MapViewMatrix", UniformType.MATRIX4X4)
        .withUniform("CameraChunk", UniformType.VEC2)
        .withUniform("CameraInChunk", UniformType.VEC2)
        .withBlend(new BlendFunction(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ONE_MINUS_SRC_ALPHA))
        .build();

    public static final RenderPipeline LINES_PIPELINE = RenderPipeline.builder()
        .withLocation(ResourceLocation.fromNamespaceAndPath("xaeroplus", "pipeline/lines"))
        .withVertexShader(ResourceLocation.fromNamespaceAndPath("xaeroplus", "lines"))
        .withFragmentShader(ResourceLocation.fromNamespaceAndPath("xaeroplus", "lines"))
        .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS)
        .withPolygonMode(PolygonMode.FILL)
        .withUniform("ModelViewMat", UniformType.MATRIX4X4)
        .withUniform("ProjMat", UniformType.MATRIX4X4)
        .withUniform("ColorModulator", UniformType.VEC4)
        .withUniform("LineWidth", UniformType.FLOAT)
        .withUniform("FrameSize", UniformType.VEC2)
        .withUniform("MapViewMatrix", UniformType.MATRIX4X4)
        .withUniform("CameraRelativeOrigin", UniformType.VEC2)
        .withBlend(new BlendFunction(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ONE_MINUS_SRC_ALPHA))
        .withCull(false)
        .build();

    public static final int[] TRANSPARENT_WM_BACKGROUND_UNIFORM_VALUE = new int[1];
    public static final RenderPipeline.UniformDescription TRANSPARENT_WM_BACKGROUND_UNIFORM = new RenderPipeline.UniformDescription("TransparentBackground", UniformType.INT);

    public static void setTransparentWMBackground(boolean value) {
        int intValue = value ? 1 : 0;
        TRANSPARENT_WM_BACKGROUND_UNIFORM_VALUE[0] = intValue;
    }

    public static final float[] LINES_FRAME_SIZE = new float[2];

    public static void setLinesFrameSize(float width, float height) {
        LINES_FRAME_SIZE[0] = width;
        LINES_FRAME_SIZE[1] = height;
    }
}
