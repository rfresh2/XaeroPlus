#version 150

in vec3 Position;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform mat4 MapViewMatrix;

void main() {
    gl_Position = ProjMat * ModelViewMat * MapViewMatrix * vec4(Position, 1.0);
}
