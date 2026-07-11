#version 330
#moj_import <xaeroplus:highlights_include.glsl>

in vec3 Position;

void main() {
    // Subtract in chunk space before converting to blocks so large common coordinates cancel exactly.
    vec2 relativePosition = (Position.xy - CameraChunk) * 16.0 - CameraInChunk;
    gl_Position = ProjMat * ModelViewMat * MapViewMatrix * vec4(relativePosition, Position.z, 1.0);
}
