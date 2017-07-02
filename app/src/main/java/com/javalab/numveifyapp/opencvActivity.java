package com.javalab.numveifyapp;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Parcelable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.features2d.FeatureDetector;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class opencvActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private TessHandler mTess;
    private JavaCameraView mOpenCvCameraView;
    private boolean isProcess = false;
    private Mat mRgba;
    private Mat mGray;
    private Mat mByte;
    private Scalar CONTOUR_COLOR;
    private GeoNumber gn;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    mOpenCvCameraView.enableView();
                    Log.d("OPENCV", "Loaded successfully");
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("OPENCV", "oncreate");
        setContentView(R.layout.activity_opencv);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        Log.d("OPENCV", "beforeset visible");
        mOpenCvCameraView = (JavaCameraView) findViewById(R.id.opencvView);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        Log.d("OPENCV", "Visible");
        mOpenCvCameraView.setCvCameraViewListener(this);
        mTess = new TessHandler(opencvActivity.this);

    }

    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mGray = inputFrame.gray();
        mRgba = inputFrame.rgba();
        //detectText();
        return mRgba;
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume() {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_2_0, this, mLoaderCallback);

    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {

        mGray = new Mat(height, width, CvType.CV_8UC4);
        mRgba = new Mat(height, width, CvType.CV_8UC4);
    }


    public void onCameraViewStopped() {
        mGray.release();
        mRgba.release();
    }

    public void onScanButtonPressed(View view){
        detectText();
    }

    private void detectText() {
        CONTOUR_COLOR = new Scalar(255);
        MatOfKeyPoint keypoint = new MatOfKeyPoint();
        List<KeyPoint> listpoint = new ArrayList<KeyPoint>();
        KeyPoint kpoint = new KeyPoint();
        Mat mask = Mat.zeros(mGray.size(), CvType.CV_8UC1);
        int rectanx1;
        int rectany1;
        int rectanx2;
        int rectany2;

        //
        Scalar zeos = new Scalar(0, 0, 0);

        List<MatOfPoint> contour2 = new ArrayList<MatOfPoint>();
        Mat kernel = new Mat(1, 50, CvType.CV_8UC1, Scalar.all(255));
        Mat morbyte = new Mat();
        Mat hierarchy = new Mat();

        Rect rectan3 = new Rect();//
        int imgsize = mRgba.height() * mRgba.width();

        FeatureDetector detector = FeatureDetector
                .create(FeatureDetector.MSER);
        detector.detect(mGray, keypoint);
        listpoint = keypoint.toList();

        for (int ind = 0; ind < listpoint.size(); ind++) {
            kpoint = listpoint.get(ind);
            rectanx1 = (int) (kpoint.pt.x - 0.5 * kpoint.size);
            rectany1 = (int) (kpoint.pt.y - 0.5 * kpoint.size);

            rectanx2 = (int) (kpoint.size);
            rectany2 = (int) (kpoint.size);
            if (rectanx1 <= 0)
                rectanx1 = 1;
            if (rectany1 <= 0)
                rectany1 = 1;
            if ((rectanx1 + rectanx2) > mGray.width())
                rectanx2 = mGray.width() - rectanx1;
            if ((rectany1 + rectany2) > mGray.height())
                rectany2 = mGray.height() - rectany1;
            Rect rectant = new Rect(rectanx1, rectany1, rectanx2, rectany2);
            Mat roi = new Mat(mask, rectant);
            roi.setTo(CONTOUR_COLOR);

        }

        Imgproc.morphologyEx(mask, morbyte, Imgproc.MORPH_DILATE, kernel);
        Imgproc.findContours(morbyte, contour2, hierarchy,
                Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_NONE);

        String mes = "";

        for (int ind = 0; ind < contour2.size(); ind++) {
            rectan3 = Imgproc.boundingRect(contour2.get(ind));
            if (rectan3.area() > 0.5 * imgsize || rectan3.area() < 100 || rectan3.width / rectan3.height < 2) {
                Mat roi = new Mat(morbyte, rectan3);
                roi.setTo(zeos);

            } else {
                Imgproc.rectangle(mRgba, rectan3.br(), rectan3.tl(), CONTOUR_COLOR);
                Mat croppedPart;
                croppedPart = mRgba.submat(rectan3);
                Bitmap bmp = Bitmap.createBitmap(croppedPart.width(), croppedPart.height(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(croppedPart, bmp);

                if(bmp != null){
                    mes += mTess.processImage(bmp);
                    mes += " ";
                }
            }
           String correctedMes = mes.replaceAll("[^0-9+]+", "");
            //String[] bits = mes.split("[+]+");
            if(correctedMes.length() > 11 && correctedMes.contains("+")) {
                int begin = correctedMes.indexOf("+");
                String ret = correctedMes.substring(begin, begin + 11);
                Toast.makeText(getApplicationContext(), ret, Toast.LENGTH_SHORT);
                Log.d("OCR+", ret);
                new GetNumberAsync().execute(ret);
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

        }


    }

    private class GetNumberAsync extends AsyncTask<String, String, String>{
        @Override
        protected String doInBackground(String... params) {
            gn = NumberGeoLocator.getLocalTimeOf(params[0]);
            if(gn!= null){
                return "success";
            } else {
                return "fail";
            }
        }
    }
}
