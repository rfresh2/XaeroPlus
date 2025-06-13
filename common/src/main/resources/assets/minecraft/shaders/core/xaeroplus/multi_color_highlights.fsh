#version 150

in vec4 vertexColor;

out vec4 fragColor;

void main() {
    if (vertexColor.a == 0.0) {
        discard;
    }
    fragColor = vertexColor;
}
