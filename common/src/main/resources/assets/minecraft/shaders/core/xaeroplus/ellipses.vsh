#version 150

in vec3 Position;
in vec4 Color;
in vec2 UV0;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform mat4 MapViewMatrix;
uniform vec2 CameraRelativeOrigin;
uniform float Thickness;
uniform vec2 FrameSize;

out vec4 vertexColor;
out vec2 ellipseLocalPx;
out vec2 ellipseRadiiPx;

void main() {
    const float aaRadiusPx = 1.0;

    // Vertex packing contract:
    // - Position.xy: center relative to the buffer's block origin
    // - UV0.xy: X/Z radii in blocks
    // - gl_VertexID % 4: logical quad corner selector
    vec2 centerRelative = Position.xy + CameraRelativeOrigin;
    vec4 centerPos = ProjMat * ModelViewMat * MapViewMatrix * vec4(centerRelative, Position.z, 1.0);
    vec4 xRadiusPos = ProjMat * ModelViewMat * MapViewMatrix
        * vec4(centerRelative + vec2(UV0.x, 0.0), Position.z, 1.0);
    vec4 zRadiusPos = ProjMat * ModelViewMat * MapViewMatrix
        * vec4(centerRelative + vec2(0.0, UV0.y), Position.z, 1.0);

    vec3 centerNdc = centerPos.xyz / centerPos.w;
    vec3 xRadiusNdc = xRadiusPos.xyz / xRadiusPos.w;
    vec3 zRadiusNdc = zRadiusPos.xyz / zRadiusPos.w;

    // NDC deltas converted to doubled-pixel space (NDC range is [-1, 1]).
    vec2 xAxisDoubledPx = FrameSize * (xRadiusNdc.xy - centerNdc.xy);
    vec2 zAxisDoubledPx = FrameSize * (zRadiusNdc.xy - centerNdc.xy);
    float radiusXPx = 0.5 * length(xAxisDoubledPx);
    float radiusZPx = 0.5 * length(zAxisDoubledPx);
    vec2 xDirection = length(xAxisDoubledPx) > 0.0001 ? normalize(xAxisDoubledPx) : vec2(1.0, 0.0);
    vec2 zDirection = length(zAxisDoubledPx) > 0.0001 ? normalize(zAxisDoubledPx) : vec2(0.0, 1.0);

    int corner = gl_VertexID % 4;
    float xSign = corner == 0 || corner == 3 ? -1.0 : 1.0;
    float zSign = corner == 0 || corner == 1 ? 1.0 : -1.0;
    float expansionPx = 0.5 * Thickness + aaRadiusPx;
    vec2 localPx = vec2(
        xSign * (radiusXPx + expansionPx),
        zSign * (radiusZPx + expansionPx)
    );

    vec2 expandedDoubledPx = FrameSize * centerNdc.xy
        + 2.0 * xDirection * localPx.x
        + 2.0 * zDirection * localPx.y;
    vec2 expandedNdc = expandedDoubledPx / FrameSize;

    gl_Position = vec4(expandedNdc, centerNdc.z, 1.0);
    vertexColor = Color;
    ellipseLocalPx = localPx;
    ellipseRadiiPx = vec2(radiusXPx, radiusZPx);
}
