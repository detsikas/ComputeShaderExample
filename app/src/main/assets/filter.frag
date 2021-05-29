#version 300 es

precision mediump float;
uniform sampler2D sTexture;
uniform float histogram[256];

in vec2 TexCoord; // the camera bg texture coordinates
out vec4 FragColor;

void main() {
    vec4 color = texture(sTexture, TexCoord);
    int gray = int(255.0*(0.299*color.r+0.587*color.g+0.114*color.b));
    if (TexCoord.x>0.5)
    {
        ivec2 rt = textureSize(sTexture, 0);
        float newColor = histogram[gray];
        FragColor = vec4(newColor, newColor, newColor, 1.0);
    }
    else
    {
        FragColor = vec4(float(gray)/255.0, float(gray)/255.0, float(gray)/255.0, 1.0);
    }
}
