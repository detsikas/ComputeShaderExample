package net.peeknpoke.apps.computeshaderexample.opengl;

import android.graphics.Bitmap;
import android.opengl.GLES31;
import android.opengl.GLUtils;

class TextureHandler {
    private final int mTexture;

    TextureHandler() {
        mTexture = createTexture();
    }

    static int createTexture()
    {
        final int[] textureHandle = new int[1];
        GLES31.glGenTextures(1, textureHandle, 0);

        if (textureHandle[0] == 0)
        {
            throw new RuntimeException("Error creating texture.");
        }

        return textureHandle[0];
    }

    void loadTexture(Bitmap image) {
        // Bind to the texture in OpenGL
        GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, mTexture);

        // Set filtering
        GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_WRAP_S, GLES31.GL_CLAMP_TO_EDGE);
        GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_WRAP_T, GLES31.GL_CLAMP_TO_EDGE);
        GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_MIN_FILTER, GLES31.GL_LINEAR);
        GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_MAG_FILTER, GLES31.GL_LINEAR);

        // Load the bitmap into the bound texture.
        GLUtils.texImage2D(GLES31.GL_TEXTURE_2D, 0, image, 0);
        GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, 0);
    }

    int getTexture() {
        return mTexture;
    }

    void cleanup()
    {
        int[] toIDs = new int[1];
        toIDs[0] = mTexture;
        GLES31.glDeleteTextures(1, toIDs, 0);
    }
}
