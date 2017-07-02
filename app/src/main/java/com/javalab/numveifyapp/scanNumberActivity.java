package com.javalab.numveifyapp;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Parcelable;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutionException;


public class scanNumberActivity extends AppCompatActivity  {

    private TextureView mTextureView;
    private TessHandler tess;
    private String mCameraID;
    private Size mPreviewSize;
    private LinearLayout greyBox;
    private CaptureRequest.Builder mCaptureRequestBuilder;
    private int rectCaptureX, rectCaptureY, rectCaptureWidth, rectCaptureHeight;
    private GeoNumber gn;

    private static SparseIntArray ORIENTATIONS = new SparseIntArray();
    static{
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    private TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            setupCamera(width, height);
            connectCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    };

    private CameraDevice mCameraDevice;
    private CameraDevice.StateCallback mCameraDeviceStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            mCameraDevice = camera;
            startPreview();
            //Toast.makeText(getApplicationContext(), "Camera is on", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            camera.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            camera.close();
            mCameraDevice = null;
        }


    };

    private HandlerThread mBackgroundHandlerThread;
    private Handler mBackgroundHandler;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_number);

        mTextureView = (TextureView) findViewById(R.id.textureView);
        greyBox = (LinearLayout) findViewById(R.id.opencv_greyBG);


        tess = new TessHandler(scanNumberActivity.this);

    }

    @Override
    protected void onResume(){
        super.onResume();

        startBackgroundThread();

        if(mTextureView.isAvailable()){
            setupCamera(mTextureView.getWidth(), mTextureView.getHeight());
            connectCamera();
        } else {
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }
    }



    @Override
    protected void onPause(){
        closeCamera();
        stopBackgroundThread();
        super.onPause();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus){
        super.onWindowFocusChanged(hasFocus);
        View decorView = getWindow().getDecorView();
        if(hasFocus){
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_FULLSCREEN);
        }
    }

    private void connectCamera(){
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            cameraManager.openCamera(mCameraID, mCameraDeviceStateCallback, mBackgroundHandler);
        } catch (CameraAccessException e) {
            Log.e("Camera", e.getMessage());
        } catch (SecurityException e){
            Log.e("Security", e.getMessage());
        }
    }

    private void setupCamera(int width, int height){
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            for (String cameraId : cameraManager.getCameraIdList()) {
                CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId);
                if(cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT){
                    continue;
                }
                StreamConfigurationMap map = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                int deviceOrientation = getWindowManager().getDefaultDisplay().getRotation();
                int totalRotation = sensorToDeviceRotation(cameraCharacteristics, deviceOrientation);
                boolean swapRotation = (totalRotation == 90 ||totalRotation == 270);
                int rotatedWidth = width;
                int rotatedHeight = height;
                if(swapRotation){
                    rotatedWidth = height;
                    rotatedHeight = width;
                }
                rectCaptureHeight = greyBox.getHeight();
                rectCaptureWidth = greyBox.getWidth();

                //rectCaptureX = rotatedWidth/2 - rectCaptureWidth/2;
                //rectCaptureY = rotatedHeight/2 + rectCaptureHeight/2;

                rectCaptureX = greyBox.getLeft();
                rectCaptureY = greyBox.getTop() + rectCaptureHeight;


                //greyBox.setLeft(rectCaptureX);
                //greyBox.setTop(rectCaptureY);

                greyBox.bringToFront();
                mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class), width, height);
                mCameraID = cameraId;
                return;
            }
        } catch(CameraAccessException e){
            Log.e("Camera", e.getMessage());
        }
    }

    private void startPreview(){
        SurfaceTexture surfaceTexture = mTextureView.getSurfaceTexture();
        surfaceTexture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
        Surface previewSurface = new Surface(surfaceTexture);

        try {
            mCaptureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mCaptureRequestBuilder.addTarget(previewSurface);

            mCameraDevice.createCaptureSession(Arrays.asList(previewSurface),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession session) {
                            try {
                                session.setRepeatingRequest(mCaptureRequestBuilder.build(), null, mBackgroundHandler);
                            } catch (CameraAccessException e) {
                                Log.e("Camera", e.getMessage());
                            }
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                            Toast.makeText(getApplicationContext(), "Unable to set up camera preview", Toast.LENGTH_SHORT).show();
                        }
                    }, null);
        } catch (CameraAccessException e) {
            Log.e("Camera", e.getMessage());
        }
    }

    private void closeCamera(){
        if(mCameraDevice != null){
            mCameraDevice.close();
            mCameraDevice = null;
        }
    }

    private void startBackgroundThread(){
        mBackgroundHandlerThread = new HandlerThread("CameraScanner");
        mBackgroundHandlerThread.start();
        mBackgroundHandler = new Handler(mBackgroundHandlerThread.getLooper());
    }

    private void stopBackgroundThread(){
        mBackgroundHandlerThread.quitSafely();
        try {
            mBackgroundHandlerThread.join();
            mBackgroundHandlerThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            Log.e("Background Thread", e.getMessage());
        }
    }

    public void onCaptureButtonClicked(View view){
        Bitmap raw = mTextureView.getBitmap();
        Bitmap bmp = Bitmap.createBitmap(raw, rectCaptureX, rectCaptureY, rectCaptureWidth, rectCaptureHeight);
        String mes = tess.processImage(bmp);
        String correctedMes = mes.replaceAll("[^0-9+]+", "");
        //String[] bits = mes.split("[+]+");
        if(correctedMes.length() > 11 && correctedMes.startsWith("+")) {

            Log.d("OCR+", correctedMes);

            try {
                gn = new GetNumberAsync().execute(correctedMes).get();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
            Toast.makeText(getApplicationContext(), correctedMes, Toast.LENGTH_SHORT);
            if(gn != null) {
                Intent intent = new Intent();
                intent.putExtra("newEntry", (Parcelable) gn);
                setResult(RESULT_OK, intent);
                finish();
            }

        } else {
            Toast.makeText(getApplicationContext(), "NO>> " + mes, Toast.LENGTH_SHORT);
            Log.d("OCR-", mes);
        }
        Toast.makeText(getApplicationContext(), mes, Toast.LENGTH_SHORT).show();
    }

    private static Size chooseOptimalSize(Size[] choices, int width, int height){
        List<Size> bigEnough = new ArrayList<>();
        for(Size option : choices){
            if(option.getHeight() == option.getWidth()*height/width && option.getWidth() >= width && option.getHeight() >= height){
                bigEnough.add(option);
            }
        }
        if(bigEnough.size() > 0){
            return Collections.min(bigEnough, new CompareSizeByArea());
        } else {
            return choices[0];
        }
    }

    private static int sensorToDeviceRotation(CameraCharacteristics cameraCharacteristics, int deviceOrientation){
        int sensorOrientation = cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        deviceOrientation = ORIENTATIONS.get(deviceOrientation);
        return(sensorOrientation + deviceOrientation+360) % 360;
    }

    private static class CompareSizeByArea implements Comparator<Size>{
        @Override
        public int compare(Size lhs, Size rhs){
            return Long.signum((long) lhs.getWidth()*lhs.getHeight()/(long)rhs.getWidth()*rhs.getHeight());
        }
    }

    private class GetNumberAsync extends AsyncTask<String, String, GeoNumber> {
        @Override
        protected GeoNumber doInBackground(String... params) {
            GeoNumber geoNumber = NumberGeoLocator.getLocalTimeOf(params[0]);
            return geoNumber;
        }
    }

}
