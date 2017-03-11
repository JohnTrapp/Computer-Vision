package com.johnvontrapp.assignment3v2;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity implements CvCameraViewListener2 {
    private static final String TAG = "OpenCVActivity";
    private CameraBridgeViewBase mOpenCvCameraView;
    private boolean mIsJavaCamera = true;
    private MenuItem mItemSwitchCamera = null;
    private File mCascadeFile;
    private CascadeClassifier mJavaDetector, mLeftEye, mRightEye;
    private int mDetectorType = 0;
    private int absoluteFaceSize;

    private Mat mRgba, gray;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        //Load openCV first...
        if (!OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_2, this, mLoaderCallback)) {
            Log.e("OpenCVActivity", "Cannot connect to OpenCV Manager");
        }

        //Required camera/app creation stuff
        mOpenCvCameraView = new JavaCameraView(this, -1);
        setContentView(mOpenCvCameraView);
        mOpenCvCameraView.setCvCameraViewListener(this);
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
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        //A guess on the face size
        absoluteFaceSize = (int) (height * 0.2);

        //Get the data
        mRgba = new Mat(height, width, CvType.CV_8UC4);

    }

    @Override
    public void onCameraViewStopped() {
        mRgba.release();  //Get rid of the traces
    }

    @Override
    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba(); //Create those mats!
        gray = inputFrame.gray();
        Imgproc.cvtColor(mRgba, gray, Imgproc.COLOR_RGBA2RGB); //Not sure if necessary

        MatOfRect faces = new MatOfRect();  //Mat of rectanges for detection
        MatOfRect leftEyes = new MatOfRect();
        MatOfRect rightEyes = new MatOfRect();

        if (mJavaDetector != null) { //If shit not fucked up
            mJavaDetector.detectMultiScale(gray, faces, 1.3, 2, 2, new Size(absoluteFaceSize, absoluteFaceSize), new Size());
        }

        Rect[] facesArray = faces.toArray();

        if (facesArray.length > 0) {
            Log.i(TAG, "Face detected!"); //Uncomment for debugging.
        }

        for (int i = 0; i < facesArray.length; i++) { //What to do with found faces. (A great name for a book...)
            Mat ROI = gray.submat(facesArray[i]);  //Create a submat
            mLeftEye.detectMultiScale(ROI, leftEyes, 1.1, 2, 2, new Size(absoluteFaceSize, absoluteFaceSize), new Size()); //Find those left eyes
            Rect[] leftEyesArray = leftEyes.toArray();
            for (int k = 0; k < leftEyesArray.length; k++) {
                Imgproc.rectangle(mRgba,
                        new Point(leftEyesArray[k].x + facesArray[i].x, leftEyesArray[k].y + facesArray[i].y),
                        new Point((leftEyesArray[k].x + facesArray[i].x) + leftEyesArray[k].width, (leftEyesArray[k].y + facesArray[i].y) + leftEyesArray[k].height),
                        new Scalar(255, 179, 0, 255), 5);  //Why the above submat was a bad idea (relative positioning...)
            }

            mRightEye.detectMultiScale(ROI, rightEyes, 1.1, 2, 2, new Size(absoluteFaceSize, absoluteFaceSize), new Size());
            Rect[] rightEyesArray = leftEyes.toArray();  //Same thing but with the right eye
            for (int k = 0; k < leftEyesArray.length; k++) {
                Imgproc.rectangle(mRgba,
                        new Point(rightEyesArray[k].x + facesArray[i].x, rightEyesArray[k].y + facesArray[i].y),
                        new Point((rightEyesArray[k].x + facesArray[i].x) + rightEyesArray[k].width, (rightEyesArray[k].y + facesArray[i].y) + rightEyesArray[k].height),
                        new Scalar(176, 138, 66, 255), 5);
            }

            //Draw that rectangle (face)
            Imgproc.rectangle(mRgba, facesArray[i].tl(), new Point(facesArray[i].x + facesArray[i].width, facesArray[i].y + facesArray[i].height), new Scalar(0, 24, 155, 255), 10);
        }
        return mRgba;
    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            //Loads openCV before starting the app
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");
                    initializeOpenCV();
                    mOpenCvCameraView.enableView();
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    private void initializeOpenCV() {
        //Apparently, this HAS TO be here. It only took me 3 hours to figure that out...
        try {
            InputStream is = getResources().openRawResource(R.raw.lbpcascade_frontalface);
            File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
            File mCascadeFile = new File(cascadeDir, "lbpcascade_frontalface.xml");
            FileOutputStream os = new FileOutputStream(mCascadeFile);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            is.close();
            os.close();

            InputStream isRight = getResources().openRawResource(R.raw.right_eye); //Change cascade files here
            File cascadeDirRight = getDir("cascade", Context.MODE_PRIVATE);
            File cascadeFileRight = new File(cascadeDir, "right_eye.xmll");
            FileOutputStream osRight = new FileOutputStream(cascadeFileRight);

            byte[] bufferRight = new byte[4096];
            int bytesReadRight;
            while ((bytesReadRight = isRight.read(bufferRight)) != -1) {
                osRight.write(bufferRight, 0, bytesReadRight);
            }
            isRight.close();
            osRight.close();

            InputStream isLeft = getResources().openRawResource(R.raw.left_eye);
            File cascadeDirLeft = getDir("cascade", Context.MODE_PRIVATE);
            File cascadeFileLeft = new File(cascadeDir, "left_eye.xmll");
            FileOutputStream osLeft = new FileOutputStream(cascadeFileLeft);

            byte[] bufferLeft = new byte[4096];
            int bytesReadLeft;
            while ((bytesReadLeft = isLeft.read(bufferLeft)) != -1) {
                osLeft.write(bufferLeft, 0, bytesReadLeft);
            }
            isLeft.close();
            osLeft.close();


            mJavaDetector = new CascadeClassifier(mCascadeFile.getAbsolutePath());
            mJavaDetector.load(mCascadeFile.getAbsolutePath());

            mLeftEye = new CascadeClassifier(cascadeFileLeft.getAbsolutePath());
            mLeftEye.load(cascadeFileLeft.getAbsolutePath());

            mRightEye = new CascadeClassifier(cascadeFileRight.getAbsolutePath());
            mRightEye.load(cascadeFileRight.getAbsolutePath());

            if (mJavaDetector.empty()) {
                //Shit got fucked up.
                Log.e(TAG, "Failed to load cascade classifier");
            }

            cascadeDir.delete();  //Free those resources!
            cascadeFileLeft.delete();
            cascadeDirLeft.delete();
            cascadeFileRight.delete();
            cascadeDirRight.delete();

        } catch (Exception e) {
            Log.e(TAG, "Error loading cascade", e);
        }

        mOpenCvCameraView.enableView();
    }
}
