package net.peeknpoke.apps.computeshaderexample.opengl;

import android.content.Context;
import android.opengl.GLES31;
import android.opengl.Matrix;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class Renderer {
    private static final String TAG = Renderer.class.getSimpleName();
    final int COORDS_PER_VERTEX = 2;
    FloatBuffer mTextureVertexBuffer;
    FloatBuffer mVertexBuffer;
    final String VERTEX_SHADER_NAME = "quad.vert";
    final String FRAGMENT_SHADER_NAME = "filter.frag";

    static final float[] IDENTITY_MATRIX = new float[16];

    // OpenGL handles
    int mProgram;

    int quadPositionParam;
    int quadTexCoordParam;

    String mVertexShaderCode;
    String mFragmentShaderCode;

    int muTexMatrixLoc;

    private static final float[] QUAD_COORDS = {
            -1.0f, -1.0f,   // 0 bottom left
            1.0f, -1.0f,   // 1 bottom right
            -1.0f,  1.0f,   // 2 top left
            1.0f,  1.0f,   // 3 top right
    };

    private static final float[] QUAD_TEXCOORDS_IDENTITY = {
            0.0f, 0.0f,     // 0 bottom left
            1.0f, 0.0f,     // 1 bottom right
            0.0f, 1.0f,     // 2 top left
            1.0f, 1.0f      // 3 top right
    };

    private static final float[] QUAD_TEXCOORDS_FLIPPED = {
            0.0f, 1.0f,     // 2 top left
            1.0f, 1.0f,      // 3 top right
            0.0f, 0.0f,     // 0 bottom left
            1.0f, 0.0f     // 1 bottom right
    };

    Renderer(Context context)
    {
        Matrix.setIdentityM(IDENTITY_MATRIX, 0);
        createTextureVertexBuffer(false);
        createVertexBuffer();

        parseShaders(context);
        createProgram();
    }

    private void parseShaders(Context context)
    {
        String vertexShaderFile, fragmentShaderFile;
        vertexShaderFile = VERTEX_SHADER_NAME;
        fragmentShaderFile = FRAGMENT_SHADER_NAME;

        try
        {
            mVertexShaderCode = ShaderUtil.readRawTextFileFromAssets(context, vertexShaderFile);
            mFragmentShaderCode = ShaderUtil.readRawTextFileFromAssets(context, fragmentShaderFile);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    void createTextureVertexBuffer(boolean flipped)
    {
        // initialize vertex byte buffer for shape coordinates
        ByteBuffer bb = ByteBuffer.allocateDirect(
                // (# of coordinate values * 4 bytes per float)
                4 * COORDS_PER_VERTEX * ShaderUtil.SIZEOF_FLOAT);
        bb.order(ByteOrder.nativeOrder());
        mTextureVertexBuffer = bb.asFloatBuffer();
        mTextureVertexBuffer.put(flipped?QUAD_TEXCOORDS_FLIPPED:QUAD_TEXCOORDS_IDENTITY);
        mTextureVertexBuffer.position(0);
    }

    void createVertexBuffer()
    {
        ByteBuffer bbVertices = ByteBuffer.allocateDirect(QUAD_COORDS.length * ShaderUtil.SIZEOF_FLOAT);
        bbVertices.order(ByteOrder.nativeOrder());
        mVertexBuffer = bbVertices.asFloatBuffer();
        mVertexBuffer.put(QUAD_COORDS);
        mVertexBuffer.position(0);
    }

    void cleanup()
    {
        GLES31.glDeleteProgram(mProgram);
    }

    void onDrawFrame(int texture, int viewPortWidth, int viewPortHeight)
    {
        GLES31.glViewport(0, 0, viewPortWidth, viewPortHeight);
        // No need to test or write depth, the screen quad has arbitrary depth, and is expected
        // to be drawn first.
        //GLES31.glDisable(GLES31.GL_DEPTH_TEST);
        //GLES31.glDepthMask(false);

        GLES31.glUseProgram(mProgram);

        GLES31.glActiveTexture(GLES31.GL_TEXTURE0);
        GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, texture);

        // Copy the texture transformation matrix over.
        GLES31.glUniformMatrix4fv(muTexMatrixLoc, 1, false, IDENTITY_MATRIX, 0);
        ShaderUtil.checkGLError(TAG, "glUniformMatrix4fv");

        // Set the vertex positions.
        GLES31.glVertexAttribPointer(
                quadPositionParam,
                COORDS_PER_VERTEX,
                GLES31.GL_FLOAT,
                false,
                COORDS_PER_VERTEX*ShaderUtil.SIZEOF_FLOAT,
                mVertexBuffer);

        // Set the texture coordinates.
        GLES31.glVertexAttribPointer(
                quadTexCoordParam,
                COORDS_PER_VERTEX,
                GLES31.GL_FLOAT,
                false,
                COORDS_PER_VERTEX*ShaderUtil.SIZEOF_FLOAT,
                mTextureVertexBuffer);

        // Enable vertex arrays
        GLES31.glEnableVertexAttribArray(quadPositionParam);
        GLES31.glEnableVertexAttribArray(quadTexCoordParam);

        GLES31.glDrawArrays(GLES31.GL_TRIANGLE_STRIP, 0, 4);

        // Disable vertex arrays
        GLES31.glDisableVertexAttribArray(quadPositionParam);
        GLES31.glDisableVertexAttribArray(quadTexCoordParam);

        GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, 0);

        // Restore the depth state for further drawing.
        //GLES31.glDepthMask(true);
        //GLES31.glEnable(GLES31.GL_DEPTH_TEST);

        ShaderUtil.checkGLError(TAG, "Draw");
        GLES31.glUseProgram(0);
    }

    private void createProgram()
    {
        int vertexShader =
                ShaderUtil.loadGLShader(TAG, mVertexShaderCode, GLES31.GL_VERTEX_SHADER);
        int fragmentShader =
                ShaderUtil.loadGLShader(TAG, mFragmentShaderCode, GLES31.GL_FRAGMENT_SHADER);

        mProgram = GLES31.glCreateProgram();
        GLES31.glAttachShader(mProgram, vertexShader);
        GLES31.glAttachShader(mProgram, fragmentShader);
        GLES31.glLinkProgram(mProgram);
        GLES31.glUseProgram(mProgram);

        ShaderUtil.checkGLError(TAG, "Program creation");

        quadPositionParam = GLES31.glGetAttribLocation(mProgram, "a_Position");
        quadTexCoordParam = GLES31.glGetAttribLocation(mProgram, "a_TexCoord");

        muTexMatrixLoc = GLES31.glGetUniformLocation(mProgram, "uTexMatrix");

        ShaderUtil.checkGLError(TAG, "Program parameters");
    }
}
