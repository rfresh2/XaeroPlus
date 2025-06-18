#version 150

#moj_import <xaeroplus:highlights_include.glsl>

out vec4 fragColor;

void main() {
    if (HighlightColor.a == 0.0) {
        discard;
    }
    fragColor = HighlightColor;
}
