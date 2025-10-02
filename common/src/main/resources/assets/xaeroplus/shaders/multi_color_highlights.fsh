#version 330

#moj_import <xaeroplus:multi_color_highlights_include.glsl>

in vec4 vertexColor;

out vec4 fragColor;

void main() {
    if (vertexColor.a == 0.0) {
        discard;
    }
    fragColor = vertexColor;
}
