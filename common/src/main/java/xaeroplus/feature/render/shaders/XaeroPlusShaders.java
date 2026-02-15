package xaeroplus.feature.render.shaders;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DestFactor;
import com.mojang.blaze3d.platform.PolygonMode;
import com.mojang.blaze3d.platform.SourceFactor;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.ResourceLocation;
import xaero.lib.client.graphics.shader.BuiltInCustomUniformValueTypes;
import xaero.lib.client.graphics.shader.CustomUniform;

public class XaeroPlusShaders {
    public static final RenderPipeline HIGHLIGHT_PIPELINE = RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
        .withLocation(ResourceLocation.fromNamespaceAndPath("xaeroplus", "pipeline/highlights"))
        .withVertexShader(ResourceLocation.fromNamespaceAndPath("xaeroplus", "highlights"))
        .withFragmentShader(ResourceLocation.fromNamespaceAndPath("xaeroplus", "highlights"))
        .withVertexFormat(DefaultVertexFormat.POSITION, VertexFormat.Mode.QUADS)
        .withPolygonMode(PolygonMode.FILL)
        .withUniform("HighlightTransforms", UniformType.UNIFORM_BUFFER)
        .withBlend(new BlendFunction(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ONE_MINUS_SRC_ALPHA))
        .build();

    public static final RenderPipeline MULTI_COLOR_HIGHLIGHT_PIPELINE = RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
        .withLocation(ResourceLocation.fromNamespaceAndPath("xaeroplus", "pipeline/multi_color_highlights"))
        .withVertexShader(ResourceLocation.fromNamespaceAndPath("xaeroplus", "multi_color_highlights"))
        .withFragmentShader(ResourceLocation.fromNamespaceAndPath("xaeroplus", "multi_color_highlights"))
        .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS)
        .withPolygonMode(PolygonMode.FILL)
        .withUniform("MultiColorHighlightTransforms", UniformType.UNIFORM_BUFFER)
        .withBlend(new BlendFunction(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ONE_MINUS_SRC_ALPHA))
        .build();

    public static final RenderPipeline LINES_PIPELINE = RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
        .withLocation(ResourceLocation.fromNamespaceAndPath("xaeroplus", "pipeline/lines"))
        .withVertexShader(ResourceLocation.fromNamespaceAndPath("xaeroplus", "lines"))
        .withFragmentShader(ResourceLocation.fromNamespaceAndPath("xaeroplus", "lines"))
        .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS)
        .withPolygonMode(PolygonMode.FILL)
        .withUniform("LinesTransforms", UniformType.UNIFORM_BUFFER)
        .withBlend(new BlendFunction(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ONE_MINUS_SRC_ALPHA))
        .withCull(false)
        .build();

    public static Integer cachedTransparentBackground = null;
    public static final CustomUniform<Integer> TRANSPARENT_WM_BACKGROUND_UNIFORM = new CustomUniform<>(
        new RenderPipeline.UniformDescription("TransparentBackgroundBlock", UniformType.UNIFORM_BUFFER),
        BuiltInCustomUniformValueTypes.INT, 32
    );

    public static void setTransparentWMBackground(boolean value) {
        int intValue = value ? 1 : 0;
        if (cachedTransparentBackground == null || cachedTransparentBackground != intValue) {
            cachedTransparentBackground = intValue;
            TRANSPARENT_WM_BACKGROUND_UNIFORM.setValue(intValue);
        }
    }

    public static final float[] LINES_FRAME_SIZE = new float[2];

    public static void setLinesFrameSize(float width, float height) {
        LINES_FRAME_SIZE[0] = width;
        LINES_FRAME_SIZE[1] = height;
    }
}
