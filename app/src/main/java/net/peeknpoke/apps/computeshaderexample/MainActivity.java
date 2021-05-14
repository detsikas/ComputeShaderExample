package net.peeknpoke.apps.computeshaderexample;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import androidx.exifinterface.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import net.peeknpoke.apps.computeshaderexample.permissions.StoragePermissionHandler;

import java.io.IOException;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int PICK_FROM_GALLERY = 2;
    private ImageView mSelectedImage = null;
    final StoragePermissionHandler mStoragePermissionHandler = new StoragePermissionHandler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSelectedImage = findViewById(R.id.imageView);
    }

    public void onApplyShader(View view)
    {

    }

    public void onImagePick(View view)
    {
        if (mStoragePermissionHandler.checkAndRequestPermission(MainActivity.this, StoragePermissionHandler.CODE_GALLERY))
        {
            Intent galleryIntent = new Intent(Intent.ACTION_PICK,
                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            // Start the Intent
            startActivityForResult(galleryIntent, PICK_FROM_GALLERY);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_FROM_GALLERY && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();

            try{
                ExifInterface exif = null;
                try {
                    exif = new ExifInterface(getContentResolver().openInputStream(uri));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                int orientation = Objects.requireNonNull(exif).getAttributeInt(ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_UNDEFINED);

                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                Bitmap rotatedBitmap = rotateBitmap(bitmap, orientation);
                //Bitmap scaledBitmap = Bitmap.createScaledBitmap(rotatedBitmap, rotatedBitmap.getWidth(), rotatedBitmap.getHeight(),
                //      true);
                mSelectedImage.setImageBitmap(rotatedBitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static Bitmap rotateBitmap(Bitmap bitmap, int orientation)
    {

        Matrix matrix = new Matrix();
        switch (orientation) {
            case ExifInterface.ORIENTATION_NORMAL:
                return bitmap;
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                matrix.setScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.setRotate(180);
                break;
            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                matrix.setRotate(180);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_TRANSPOSE:
                matrix.setRotate(90);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.setRotate(90);
                break;
            case ExifInterface.ORIENTATION_TRANSVERSE:
                matrix.setRotate(-90);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.setRotate(-90);
                break;
            default:
                return bitmap;
        }
        try {
            Bitmap bmRotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            bitmap.recycle();
            return bmRotated;
        }
        catch (OutOfMemoryError e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mStoragePermissionHandler.mPendingGalleryAccess) {
            mStoragePermissionHandler.mPendingGalleryAccess = false;
            onImagePick(null);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode != StoragePermissionHandler.CODE_GALLERY) {
            Log.d(TAG, "Got unexpected permission result: " + requestCode);
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            return;
        }

        if (grantResults.length != 0) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                // Permission was denied
                mStoragePermissionHandler.checkAndRequestPermission(this, requestCode);

                Log.e(TAG, "Permission not granted: results len = " + grantResults.length +
                        " Result code = " + grantResults[0]);
            } else {
                // Permission was granted
                mStoragePermissionHandler.mPendingGalleryAccess = true;
            }
        }
    }
}