#version 150

in vec3 Position;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform mat4 WorldMapViewMatrix;

void main() {
    gl_Position = ProjMat * ModelViewMat * WorldMapViewMatrix * vec4(Position, 1.0);
}
