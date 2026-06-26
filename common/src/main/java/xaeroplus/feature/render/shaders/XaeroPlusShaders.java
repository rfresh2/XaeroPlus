package xaeroplus.feature.render.shaders;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.*;
import com.mojang.blaze3d.platform.BlendFactor;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.platform.PolygonMode;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.OutputTarget;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import xaero.lib.client.graphics.XaeroRenderType;
import xaero.lib.client.graphics.shader.BuiltInCustomUniformValueTypes;
import xaero.lib.client.graphics.shader.BuiltInCustomUniforms;
import xaero.lib.client.graphics.shader.CustomUniform;
import xaero.lib.client.graphics.shader.LibShaders;
import xaero.map.WorldMap;

import java.util.OptionalDouble;

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

    public static final RenderPipeline TEXT_NO_CULL_RP = RenderPipeline.builder(RenderPipelines.WORLD_TEXT_SNIPPET)
        .withLocation(Identifier.fromNamespaceAndPath("xaeroplus", "pipeline/text_no_cull"))
        .withVertexShader("core/text")
        .withFragmentShader("core/text")
        .withCull(false)
        .build();

    public static final RenderType TEXT_NO_CULL = XaeroRenderType.createRenderType(
        "xaeroplus_text_no_cull",
        RenderSetup.builder(TEXT_NO_CULL_RP)
            // todo: does this need to change when language/font is switched?
            .withTexture("Sampler0", Identifier.withDefaultNamespace("default/0"))
            .useLightmap()
            .setOutputTarget(OutputTarget.MAIN_TARGET));

    public static final RenderPipeline CUSTOM_MAP_RP = RenderPipeline.builder()
        .withBindGroupLayout(
            BindGroupLayout.builder().withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER).withUniform("Projection", UniformType.UNIFORM_BUFFER).build()
        )
        .withLocation(Identifier.fromNamespaceAndPath("xaeroplus", "pipeline/custom_map"))
        .withVertexShader(Identifier.fromNamespaceAndPath("xaeroplus", "custom_map"))
        .withFragmentShader(Identifier.fromNamespaceAndPath("xaeroplus", "custom_map"))
        .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX)
        .withPrimitiveTopology(PrimitiveTopology.QUADS)
        .withBindGroupLayout(
            BindGroupLayout.builder()
                .withSampler("Sampler0")
                .withUniform(BuiltInCustomUniforms.BRIGHTNESS.name(), BuiltInCustomUniforms.BRIGHTNESS.type())
                .withUniform(BuiltInCustomUniforms.WITH_LIGHT.name(), BuiltInCustomUniforms.WITH_LIGHT.type())
                .withUniform(XaeroPlusShaders.TRANSPARENT_WM_BACKGROUND_UNIFORM.name(), XaeroPlusShaders.TRANSPARENT_WM_BACKGROUND_UNIFORM.type())
                .build()
        )
        .withColorTargetState(new ColorTargetState(new BlendFunction(BlendFactor.ONE, BlendFactor.ZERO, BlendFactor.ONE, BlendFactor.ZERO)))
        .withCull(false)
        .withDepthStencilState(DepthStencilState.DEFAULT)
        .build();

    public static final RenderType CUSTOM_MAP = XaeroRenderType.createRenderType(
        "xaeroplus_custom_map",
        RenderSetup.builder(CUSTOM_MAP_RP)
            .withTexture("Sampler0", WorldMap.guiTextures, () -> RenderSystem.getDevice().createSampler(AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE, FilterMode.LINEAR, FilterMode.NEAREST, 1, OptionalDouble.of(1.0)))
            .setOutputTarget(OutputTarget.MAIN_TARGET)
    );

    public static final RenderPipeline CUSTOM_MAP_FRAME_RP = RenderPipeline.builder()
        .withBindGroupLayout(
            BindGroupLayout.builder().withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER).withUniform("Projection", UniformType.UNIFORM_BUFFER).build()
        )
        .withVertexShader(LibShaders.POSITION_COLOR_TEX)
        .withFragmentShader(LibShaders.POSITION_COLOR_TEX)
        .withVertexBinding(0, XaeroRenderType.POSITION_COLOR_TEX)
        .withPrimitiveTopology(PrimitiveTopology.QUADS)
        .withBindGroupLayout(BindGroupLayout.builder().withSampler("Sampler0").build())
        .withLocation(Identifier.fromNamespaceAndPath("xaeroplus", "pipeline/custom_map_frame"))
        .withColorTargetState(new ColorTargetState(new BlendFunction(BlendFactor.ONE, BlendFactor.ZERO, BlendFactor.ONE, BlendFactor.ZERO)))
        .withCull(false)
        .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
        .build();

    public static final RenderType CUSTOM_MAP_FRAME = XaeroRenderType.createRenderType(
        "xaeroplus_custom_map_frame",
        RenderSetup.builder(CUSTOM_MAP_FRAME_RP)
            .withTexture("Sampler0", WorldMap.guiTextures, () -> RenderSystem.getDevice()
                .createSampler(AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE, FilterMode.LINEAR, FilterMode.LINEAR, 1, OptionalDouble.of(1.0)))
            .setOutputTarget(OutputTarget.MAIN_TARGET)
    );
}
