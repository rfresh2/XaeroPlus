#version 150
#moj_import <xaeroplus:highlights_include.glsl>

in vec3 Position;

void main() {
    gl_Position = ProjMat * ModelViewMat * MapViewMatrix * vec4(Position, 1.0);
}
