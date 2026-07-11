#moj_import <minecraft:projection.glsl>
#moj_import <minecraft:dynamictransforms.glsl>

layout(std140) uniform LinesTransforms {
    mat4 MapViewMatrix;
    vec2 FrameSize;
    float LineWidth;
    vec2 CameraRelativeOrigin;
};
