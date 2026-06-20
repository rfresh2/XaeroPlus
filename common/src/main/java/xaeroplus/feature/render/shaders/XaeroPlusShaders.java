package xaeroplus.feature.render.shaders;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.BindGroupLayout;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.BlendFactor;
import com.mojang.blaze3d.platform.PolygonMode;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.resources.Identifier;
import xaero.lib.client.graphics.shader.BuiltInCustomUniformValueTypes;
import xaero.lib.client.graphics.shader.CustomUniform;

public class XaeroPlusShaders {
    public static final RenderPipeline HIGHLIGHT_PIPELINE = RenderPipeline.builder()
        .withBindGroupLayout(BindGroupLayouts.GLOBALS)
        .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
        .withBindGroupLayout(BindGroupLayout.builder().withUniform("HighlightTransforms", UniformType.UNIFORM_BUFFER).build())
        .withLocation(Identifier.fromNamespaceAndPath("xaeroplus", "pipeline/highlights"))
        .withVertexShader(Identifier.fromNamespaceAndPath("xaeroplus", "highlights"))
        .withFragmentShader(Identifier.fromNamespaceAndPath("xaeroplus", "highlights"))
        .withPrimitiveTopology(PrimitiveTopology.QUADS)
        .withVertexBinding(0, DefaultVertexFormat.POSITION)
        .withPolygonMode(PolygonMode.FILL)
        .withColorTargetState(new ColorTargetState(new BlendFunction(BlendFactor.SRC_ALPHA, BlendFactor.ONE_MINUS_SRC_ALPHA, BlendFactor.ONE, BlendFactor.ONE_MINUS_SRC_ALPHA)))
        .build();

    public static final RenderPipeline MULTI_COLOR_HIGHLIGHT_PIPELINE = RenderPipeline.builder()
        .withBindGroupLayout(BindGroupLayouts.GLOBALS)
        .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
        .withBindGroupLayout(BindGroupLayout.builder().withUniform("MultiColorHighlightTransforms", UniformType.UNIFORM_BUFFER).build())
        .withLocation(Identifier.fromNamespaceAndPath("xaeroplus", "pipeline/multi_color_highlights"))
        .withVertexShader(Identifier.fromNamespaceAndPath("xaeroplus", "multi_color_highlights"))
        .withFragmentShader(Identifier.fromNamespaceAndPath("xaeroplus", "multi_color_highlights"))
        .withPrimitiveTopology(PrimitiveTopology.QUADS)
        .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
        .withPolygonMode(PolygonMode.FILL)
        .withColorTargetState(new ColorTargetState(new BlendFunction(BlendFactor.SRC_ALPHA, BlendFactor.ONE_MINUS_SRC_ALPHA, BlendFactor.ONE, BlendFactor.ONE_MINUS_SRC_ALPHA)))
        .build();

    public static final RenderPipeline LINES_PIPELINE = RenderPipeline.builder()
        .withBindGroupLayout(BindGroupLayouts.GLOBALS)
        .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
        .withBindGroupLayout(BindGroupLayout.builder().withUniform("LinesTransforms", UniformType.UNIFORM_BUFFER).build())
        .withLocation(Identifier.fromNamespaceAndPath("xaeroplus", "pipeline/lines"))
        .withVertexShader(Identifier.fromNamespaceAndPath("xaeroplus", "lines"))
        .withFragmentShader(Identifier.fromNamespaceAndPath("xaeroplus", "lines"))
        .withPrimitiveTopology(PrimitiveTopology.QUADS)
        .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR)
        .withPolygonMode(PolygonMode.FILL)
        .withColorTargetState(new ColorTargetState(new BlendFunction(BlendFactor.SRC_ALPHA, BlendFactor.ONE_MINUS_SRC_ALPHA, BlendFactor.ONE, BlendFactor.ONE_MINUS_SRC_ALPHA)))
        .withCull(false)
        .build();

    public static Integer cachedTransparentBackground = null;
    public static final CustomUniform<Integer> TRANSPARENT_WM_BACKGROUND_UNIFORM = new CustomUniform<>(
        new BindGroupLayout.UniformDescription("TransparentBackgroundBlock", UniformType.UNIFORM_BUFFER),
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
