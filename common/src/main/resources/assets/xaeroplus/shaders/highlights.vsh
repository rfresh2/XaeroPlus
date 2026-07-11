#version 150

in vec3 Position;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform mat4 MapViewMatrix;
uniform vec2 CameraChunk;
uniform vec2 CameraInChunk;

void main() {
    // Subtract in chunk space before converting to blocks so large common coordinates cancel exactly.
    vec2 relativePosition = (Position.xy - CameraChunk) * 16.0 - CameraInChunk;
    gl_Position = ProjMat * ModelViewMat * MapViewMatrix * vec4(relativePosition, Position.z, 1.0);
}
