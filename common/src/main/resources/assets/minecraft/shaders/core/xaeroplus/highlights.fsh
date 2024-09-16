#version 150

uniform vec4 HighlightColor;

out vec4 fragColor;

void main() {
    if (HighlightColor.a == 0.0) {
        discard;
    }
    fragColor = HighlightColor;
}
