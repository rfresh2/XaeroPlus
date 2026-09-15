#include <minecraft:projection.glsl>
#include <minecraft:dynamictransforms.glsl>

layout(std140) uniform LinesTransforms {
    mat4 MapViewMatrix;
    vec2 FrameSize;
    float LineWidth;
    vec2 CameraRelativeOrigin;
};
