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
        .withBlend(new BlendFunction(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ONE_MINUS_SRC_ALPHA))
        .build();

    public static final int[] TRANSPARENT_WM_BACKGROUND_UNIFORM_VALUE = new int[1];
    public static final RenderPipeline.UniformDescription TRANSPARENT_WM_BACKGROUND_UNIFORM = new RenderPipeline.UniformDescription("TransparentBackground", UniformType.INT);
    public static void setTransparentWMBackground(boolean value) {
        int intValue = value ? 1 : 0;
        TRANSPARENT_WM_BACKGROUND_UNIFORM_VALUE[0] = intValue;
    }
}
