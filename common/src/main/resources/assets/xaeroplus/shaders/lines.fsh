#version 330
#extension GL_ARB_separate_shader_objects : require

#include <xaeroplus:lines_include.glsl>

layout(location = 0) in vec4 vertexColor;
layout(location = 1) in vec2 segmentLocalPx;
layout(location = 2) in float segmentLengthPx;

layout(location = 0) out vec4 fragColor;

void main() {
    // can be tuned: higher radius = softer edges
    const float aaRadiusPx = 1.0;

    float halfWidthPx = 0.5 * LineWidth;

    // Signed distance to a capsule from (0,0) to (segmentLengthPx,0) with radius halfWidthPx.
    float clampedX = clamp(segmentLocalPx.x, 0.0, segmentLengthPx);
    vec2 delta = vec2(segmentLocalPx.x - clampedX, segmentLocalPx.y);
    float dist = length(delta) - halfWidthPx;
    // wider smoothstep = more feathering
    float alpha = 1.0 - smoothstep(0.0, aaRadiusPx, dist);
    if (alpha <= 0.0) {
        discard;
    }

    vec4 color = vertexColor;
    color.a *= alpha;
    fragColor = color;
}
