#include <minecraft:projection.glsl>
#include <minecraft:dynamictransforms.glsl>

layout(std140) uniform MultiColorHighlightTransforms {
    mat4 MapViewMatrix;
	vec2 CameraChunk;
	vec2 CameraInChunk;
};
