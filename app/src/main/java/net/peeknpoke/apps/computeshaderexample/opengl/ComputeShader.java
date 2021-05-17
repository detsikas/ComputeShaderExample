package net.peeknpoke.apps.computeshaderexample.opengl;

import android.content.Context;
import android.opengl.GLES31;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

public class ComputeShader {
    private static final String TAG = ComputeShader.class.getSimpleName();

    private final String COMPUTE_SHADER_NAME = "histogram.comp";
    private final int WORK_GROUPS_X;
    private final int WORK_GROUPS_Y;
    private final int WORK_GROUPS_Z = 1;
    private int mSSBO;
    private IntBuffer mHistogram;
    String mComputeShaderCode;
    int mProgram;

    public ComputeShader(Context context, int width, int height)
    {
        WORK_GROUPS_X = (int)Math.ceil(width/16.0);
        WORK_GROUPS_Y = (int)Math.ceil(height/8.0);
        createSSBO();
        parseShaders(context);
        createProgram();
    }

    private void createSSBO()
    {
        final int[] ssbo = new int[1];
        GLES31.glGenBuffers(1, ssbo, 0);
        GLES31.glBindBuffer(GLES31.GL_SHADER_STORAGE_BUFFER, ssbo[0]);
        GLES31.glBufferData(GLES31.GL_SHADER_STORAGE_BUFFER, 4*256, null, GLES31.GL_STATIC_DRAW);
        mSSBO = ssbo[0];

        int bufMask = GLES31.GL_MAP_WRITE_BIT | GLES31.GL_MAP_INVALIDATE_BUFFER_BIT;
        mHistogram = ((ByteBuffer) GLES31.glMapBufferRange(GLES31.GL_SHADER_STORAGE_BUFFER, 0, 4*256, bufMask)).asIntBuffer();
        resetHistogram();
        // Maybe set to zero
        GLES31.glUnmapBuffer(GLES31.GL_SHADER_STORAGE_BUFFER);
    }

    private void createProgram()
    {
        int computeShader =
                ShaderUtil.loadGLShader(TAG, mComputeShaderCode, GLES31.GL_COMPUTE_SHADER);

        mProgram = GLES31.glCreateProgram();
        GLES31.glAttachShader(mProgram, computeShader);
        GLES31.glLinkProgram(mProgram);
    }

    private void parseShaders(Context context)
    {
        String computeShaderFile = COMPUTE_SHADER_NAME;

        try
        {
            mComputeShaderCode = ShaderUtil.readRawTextFileFromAssets(context, computeShaderFile);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    void execute(int texture)
    {
        GLES31.glBindBufferBase(GLES31.GL_SHADER_STORAGE_BUFFER, 0, mSSBO);

        GLES31.glBindImageTexture(1, texture, 0, false, 0, GLES31.GL_READ_ONLY, GLES31.GL_R32F);
        GLES31.glUseProgram(mProgram); // Compute shader program.
        GLES31.glDispatchCompute(WORK_GROUPS_X, WORK_GROUPS_Y, WORK_GROUPS_Z);
        GLES31.glMemoryBarrier(GLES31.GL_ALL_BARRIER_BITS);
        GLES31.glUseProgram(0);
    }

    IntBuffer getHistogram()
    {
        return mHistogram;
    }

    void logHistogram()
    {
        ByteOrder order = mHistogram.order();
        mHistogram.rewind();
        while(mHistogram.hasRemaining())
        {
            int pos = mHistogram.position();
            int value = mHistogram.get();
            if (order==ByteOrder.BIG_ENDIAN)
            {
                value = Integer.reverseBytes(value);
            }
            Log.d(TAG, ""+pos+": "+value);
        }
    }

    void logInfo()
    {
        int[] threadsPerGroup = new int[1];
        int[] maxWorkGroupsDispatched = new int[3];
        int[] maxWorkGroupsCompiled = new int[3];
        GLES31.glGetIntegerv(GLES31.GL_MAX_COMPUTE_WORK_GROUP_INVOCATIONS, threadsPerGroup, 0);
        GLES31.glGetIntegeri_v(GLES31.GL_MAX_COMPUTE_WORK_GROUP_COUNT, 0, maxWorkGroupsDispatched, 0);
        GLES31.glGetIntegeri_v(GLES31.GL_MAX_COMPUTE_WORK_GROUP_COUNT, 1, maxWorkGroupsDispatched, 1);
        GLES31.glGetIntegeri_v(GLES31.GL_MAX_COMPUTE_WORK_GROUP_COUNT, 2, maxWorkGroupsDispatched, 2);
        GLES31.glGetIntegeri_v(GLES31.GL_MAX_COMPUTE_WORK_GROUP_SIZE, 0, maxWorkGroupsCompiled, 0);
        GLES31.glGetIntegeri_v(GLES31.GL_MAX_COMPUTE_WORK_GROUP_SIZE, 1, maxWorkGroupsCompiled, 1);
        GLES31.glGetIntegeri_v(GLES31.GL_MAX_COMPUTE_WORK_GROUP_SIZE, 2, maxWorkGroupsCompiled, 2);
        Log.i(TAG, "Max threads per group (product of all dimensions): "+threadsPerGroup[0]);
        Log.i(TAG, "Max workgroups allowed: "+maxWorkGroupsDispatched[0]+"x"+maxWorkGroupsDispatched[1]+"x"+maxWorkGroupsDispatched[2]);
        Log.i(TAG, "Max workgroup size per dimension: "+maxWorkGroupsCompiled[0]+"x"+maxWorkGroupsCompiled[1]+"x"+maxWorkGroupsCompiled[2]);
    }

    void resetHistogram()
    {
        mHistogram.rewind();
        while(mHistogram.hasRemaining())
            mHistogram.put(0);
    }
}
