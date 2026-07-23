#version 150

#moj_import <xaeroplus:ellipses_include.glsl>

in vec4 vertexColor;
in vec2 ellipseLocalPx;
in vec2 ellipseRadiiPx;

out vec4 fragColor;

float ellipseSignedDistance(vec2 point, vec2 radii) {
    // This approximation is exact for circles and at the ellipse boundary. Keeping the signed
    // distance separate from stroke coverage makes a future filled mode a coverage-only change.
    radii = max(radii, vec2(0.0001));
    if (length(point) < 0.0001) {
        return -min(radii.x, radii.y);
    }
    float normalizedDistance = length(point / radii);
    float gradientLength = length(point / (radii * radii));
    return normalizedDistance * (normalizedDistance - 1.0) / gradientLength;
}

void main() {
    const float aaRadiusPx = 1.0;

    float signedDistance = ellipseSignedDistance(ellipseLocalPx, ellipseRadiiPx);
    float strokeDistance = abs(signedDistance) - 0.5 * LineWidth;
    float alpha = 1.0 - smoothstep(0.0, aaRadiusPx, strokeDistance);
    if (alpha <= 0.0) {
        discard;
    }

    vec4 color = vertexColor;
    color.a *= alpha;
    fragColor = color;
}
