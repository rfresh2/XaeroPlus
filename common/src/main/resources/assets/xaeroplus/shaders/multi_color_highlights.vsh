#version 330
#extension GL_ARB_separate_shader_objects : require

#include <xaeroplus:multi_color_highlights_include.glsl>

layout(location = 0) in vec3 Position;
layout(location = 1) in vec4 Color;

layout(location = 0) out vec4 vertexColor;

void main() {
    // Subtract in chunk space before converting to blocks so large common coordinates cancel exactly.
    vec2 relativePosition = (Position.xy - CameraChunk) * 16.0 - CameraInChunk;
    gl_Position = ProjMat * ModelViewMat * MapViewMatrix * vec4(relativePosition, Position.z, 1.0);
    vertexColor = Color;
}
