#version 150

#moj_import <xaeroplus:multi_color_highlights_include.glsl>

in vec3 Position;
in vec4 Color;

out vec4 vertexColor;

void main() {
    gl_Position = Projection * ModelViewMat * MapViewMatrix * vec4(Position, 1.0);
    vertexColor = Color;
}
