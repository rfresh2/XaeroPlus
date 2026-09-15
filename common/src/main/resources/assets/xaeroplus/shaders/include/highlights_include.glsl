#include <minecraft:projection.glsl>
#include <minecraft:dynamictransforms.glsl>

layout(std140) uniform HighlightTransforms {
    mat4 MapViewMatrix;
    vec4 HighlightColor;
    vec2 CameraChunk;
    vec2 CameraInChunk;
};
