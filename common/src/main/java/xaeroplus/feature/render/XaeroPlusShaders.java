package xaeroplus.feature.render;

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
        .withVertexFormat(DefaultVertexFormat.POSITION, VertexFormat.Mode.TRIANGLES)
        .withPolygonMode(PolygonMode.FILL)
        .withUniform("ModelViewMat", UniformType.MATRIX4X4)
        .withUniform("ProjMat", UniformType.MATRIX4X4)
        .withUniform("MapViewMatrix", UniformType.MATRIX4X4)
        .withUniform("HighlightColor", UniformType.VEC4)
        .withBlend(new BlendFunction(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ONE_MINUS_SRC_ALPHA))
        .build();
}
