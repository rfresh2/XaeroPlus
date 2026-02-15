#version 150

in vec3 Position;
in vec4 Color;
in vec2 UV0;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform mat4 MapViewMatrix;
uniform float LineWidth;
uniform vec2 FrameSize;

out vec4 vertexColor;
out vec2 segmentLocalPx;
out float segmentLengthPx;

void main() {
	// can be tuned: higher radius = softer edges
    const float aaRadiusPx = 1.0;

    // Vertex packing contract:
    // - Position.xy: segment start in map-space
    // - UV0.xy: segment end in map-space
    // - gl_VertexID % 4: logical quad corner selector
    vec4 startPos = ProjMat * ModelViewMat * MapViewMatrix * vec4(Position, 1.0);
    vec4 endPos = ProjMat * ModelViewMat * MapViewMatrix * vec4(UV0, Position.z, 1.0);

    vec3 startNdc = startPos.xyz / startPos.w;
    vec3 endNdc = endPos.xyz / endPos.w;

    // NDC delta converted to doubled-pixel space (NDC range is [-1, 1]).
    vec2 lineVecDoubledPx = FrameSize * (endNdc.xy - startNdc.xy);
    float lineLengthDoubledPx = length(lineVecDoubledPx);

    vec2 dirDoubledPx = lineLengthDoubledPx > 0.0001 ? (lineVecDoubledPx / lineLengthDoubledPx) : vec2(1.0, 0.0);
    vec2 perpDoubledPx = vec2(-dirDoubledPx.y, dirDoubledPx.x);

    float halfWidthPx = 0.5 * LineWidth;
    float expandPx = halfWidthPx + aaRadiusPx;
    float lineLength = 0.5 * lineLengthDoubledPx;

    // In this indexed QUADS path, gl_VertexID is the element index value.
    // Indices are emitted as 0,1,2,2,3,0 so `% 4` recovers the logical quad corner.
    int corner = gl_VertexID % 4;
    bool isStart = corner == 0 || corner == 3;
    bool isTop = corner == 0 || corner == 1;
    float endpointT = isStart ? 0.0 : 1.0;

    float localX = isStart ? -expandPx : (lineLength + expandPx);
    float localY = isTop ? expandPx : -expandPx;

    vec2 expandedDoubledPx = FrameSize * startNdc.xy
        + dirDoubledPx * (2.0 * localX)
        + perpDoubledPx * (2.0 * localY);
    vec2 expandedNdc = expandedDoubledPx / FrameSize;

    gl_Position = vec4(expandedNdc, mix(startNdc.z, endNdc.z, endpointT), 1.0);
    vertexColor = Color;
    segmentLocalPx = vec2(localX, localY);
    segmentLengthPx = lineLength;
}
