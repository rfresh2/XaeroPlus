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
}
