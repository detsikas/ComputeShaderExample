#version 300 es

precision mediump float;
uniform sampler2D sTexture;
in vec2 TexCoord; // the camera bg texture coordinates
out vec4 FragColor;

void main() {
    vec4 color = texture(sTexture, TexCoord);
	FragColor = vec4(color.b, color.g, color.r, 1.0);
}
