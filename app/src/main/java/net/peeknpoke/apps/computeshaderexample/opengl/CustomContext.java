package net.peeknpoke.apps.computeshaderexample.opengl;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.hardware.camera2.CameraCharacteristics;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLSurface;
import android.opengl.GLES31;

import android.util.Size;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class CustomContext {
    //private static final String TAG = CustomContext.class.getSimpleName();

    private Renderer mRenderer;
    private ComputeShader mComputeShader;

    private final EGLContext mCtx;
    private final EGLDisplay mDpy;
    private final EGLSurface mSurf;

    private final TextureHandler mTextureHandler;

    // Capturing elements
    private final ByteBuffer mBB;
    private final Bitmap mBitmap;

    /*
     *  imageWidth, imageHeight: the dimensions of the captured image, already adjusted
     *                           based on the camera rotation. The image is not rotated yet
     *  surfaceWidth, surfaceHeight: the dimensions of the surface where the image will be drawn
     *                               They can be the same as the captured image dimensions or arbitrary.
     *                               The second option is primarily for the case where the surface dimensions
     *                               will match the preview screen surface dimensions
     */
    public CustomContext(Context context, Size imageSize)
    {
        mDpy = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
        int[] version = new int[2];
        EGL14.eglInitialize(mDpy, version, 0, version, 1);

        int[] configAttr = {
                EGL14.EGL_COLOR_BUFFER_TYPE, EGL14.EGL_RGB_BUFFER,
                EGL14.EGL_LEVEL, 0,
                EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                EGL14.EGL_SURFACE_TYPE, EGL14.EGL_PBUFFER_BIT,
                EGL14.EGL_NONE
        };
        EGLConfig[] configs = new EGLConfig[1];
        int[] numConfig = new int[1];
        EGL14.eglChooseConfig(mDpy, configAttr, 0,
                configs, 0, 1, numConfig, 0);

        EGLConfig config = configs[0];

        int[] surfAttr = {
                EGL14.EGL_WIDTH, imageSize.getWidth(),
                EGL14.EGL_HEIGHT, imageSize.getHeight(),
                EGL14.EGL_NONE
        };
        mSurf = EGL14.eglCreatePbufferSurface(mDpy, config, surfAttr, 0);

        int[] ctxAttrib = {
                EGL14.EGL_CONTEXT_CLIENT_VERSION, 3,
                EGL14.EGL_NONE
        };
        mCtx = EGL14.eglCreateContext(mDpy, config, EGL14.EGL_NO_CONTEXT, ctxAttrib, 0);

        EGL14.eglMakeCurrent(mDpy, mSurf, mSurf, mCtx);

        mTextureHandler = new TextureHandler();
        GLES31.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);

        int screenshotSize = imageSize.getWidth()*imageSize.getHeight();
        mBB = ByteBuffer.allocateDirect(screenshotSize * 4);
        mBB.order(ByteOrder.nativeOrder());
        mBitmap = Bitmap.createBitmap(imageSize.getWidth(), imageSize.getHeight(), Bitmap.Config.ARGB_8888);
        mRenderer = new Renderer(context);
        mComputeShader = new ComputeShader(context, mBitmap.getWidth(), mBitmap.getHeight());
    }

    public void onDrawFrame()
    {
        GLES31.glClear(GLES31.GL_COLOR_BUFFER_BIT | GLES31.GL_DEPTH_BUFFER_BIT);
        if (mRenderer !=null && mComputeShader!=null)
        {
            mComputeShader.execute(mTextureHandler.getTexture());
            //mRenderer.onDrawFrame(mTextureHandler.getTexture(), mBitmap.getWidth(),
              //      mBitmap.getHeight());

            mComputeShader.logHistogram();
            mComputeShader.logInfo();
        }
    }

    public Bitmap getPixels()
    {
        GLES31.glReadPixels(0, 0, mBitmap.getWidth(), mBitmap.getHeight(), GLES31.GL_RGBA, GLES31.GL_UNSIGNED_BYTE, mBB);
        mBitmap.copyPixelsFromBuffer(mBB);
        mBB.rewind();

        return Bitmap.createBitmap(mBitmap);
    }

    public void release()
    {
        cleanup();
        mTextureHandler.cleanup();
        EGL14.eglMakeCurrent(mDpy, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE,
                EGL14.EGL_NO_CONTEXT);
        EGL14.eglDestroySurface(mDpy, mSurf);
        EGL14.eglDestroyContext(mDpy, mCtx);
        EGL14.eglTerminate(mDpy);
    }

    public void loadTexture(Bitmap image)
    {
        mTextureHandler.loadTexture(image);
    }

    public static Matrix getImageRotationMatrix(int cameraRotation)
    {
        Matrix rotationMatrix = new Matrix();
        rotationMatrix.postRotate(cameraRotation);
        return rotationMatrix;
    }

    public static int getImageRotation(int cameraFacing, int cameraRotation, int deviceOrientation)
    {
        int imageRotation = (cameraFacing == CameraCharacteristics.LENS_FACING_FRONT)?(cameraRotation-deviceOrientation)%360:
                (cameraRotation+deviceOrientation)%360;

        if (imageRotation<0)
            imageRotation+=360;

        return imageRotation;
    }

    public void copyPixelsIntoBitmap(Bitmap bitmap)
    {
        GLES31.glReadPixels(0, 0, bitmap.getWidth(), bitmap.getHeight(), GLES31.GL_RGBA, GLES31.GL_UNSIGNED_BYTE, mBB);

        bitmap.copyPixelsFromBuffer(mBB);
        mBB.rewind();
    }


    private void cleanup()
    {
        if (mRenderer !=null)
            mRenderer.cleanup();

        mRenderer = null;
    }
}
