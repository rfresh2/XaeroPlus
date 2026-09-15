#version 330
#extension GL_ARB_separate_shader_objects : require

#include <xaeroplus:highlights_include.glsl>

layout(location = 0) out vec4 fragColor;

void main() {
    if (HighlightColor.a == 0.0) {
        discard;
    }
    fragColor = HighlightColor;
}
