#version 330
#extension GL_ARB_separate_shader_objects : require

#include <xaeroplus:multi_color_highlights_include.glsl>

layout(location = 0) in vec4 vertexColor;

layout(location = 0) out vec4 fragColor;

void main() {
    if (vertexColor.a == 0.0) {
        discard;
    }
    fragColor = vertexColor;
}
