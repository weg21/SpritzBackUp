package com.andrewgiang.textspritzer.app;

import android.graphics.Color;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Toast;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import com.andrewgiang.textspritzer.lib.Spritzer;
import com.andrewgiang.textspritzer.lib.SpritzerTextView;
import edu.pitt.cs.mips.hrv_exp.*;
import android.content.Context;
import android.util.Log;
import android.content.res.Configuration;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera.Size;
import android.hardware.SensorManager;
import android.view.Display;
import android.view.OrientationEventListener;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import java.util.EnumSet;

import com.qualcomm.snapdragon.sdk.face.FaceData;
import com.qualcomm.snapdragon.sdk.face.FacialProcessing;
import com.qualcomm.snapdragon.sdk.face.FacialProcessing.FP_MODES;
import com.qualcomm.snapdragon.sdk.sample.CameraSurfacePreview;
import com.qualcomm.snapdragon.sdk.face.FacialProcessing.PREVIEW_ROTATION_ANGLE;
import com.qualcomm.snapdragon.sdk.sample.saveFaceData;
import org.ejml.simple.SimpleMatrix;

public class MainActivity extends ActionBarActivity implements Camera.PreviewCallback {

    public static final String TAG = MainActivity.class.getName();
    private static Context context;
    private SpritzerTextView mSpritzerTextView;
    private SeekBar mSeekBarTextSize;
    private SeekBar mSeekBarWpm;
    private ProgressBar mProgressBar;
    SurfaceView mPreview;
    FrameLayout preview;
    DataStorage storage;
    saveFaceData saving;
    OrientationEventListener orientationEventListener;
    int deviceOrientation, presentOrientation;
    FacialProcessing faceProc;
    boolean fpFeatureSupported = false;
    Camera cameraObj;
    boolean faceDetected;
    TextView numFaceText,faceYawText,gazePointText,heartDataText, heartIndicator;
    Button start;
    int surfaceWidth = 0, surfaceHeight = 0;
    int smileValue = 0, leftEyeBlink = 0, rightEyeBlink = 0, faceRollValue = 0, pitch = 0, yaw = 0, horizontalGaze = 0, verticalGaze = 0, displayAngle;
    PointF gazePointValue = null;
    Point leftEyeCoord=null, rightEyeCoord=null;
    Rect faceRect = null;
    View myView, faceShape, temperatureShape;
    Display display;
    FaceData[] faceArray = null;
    boolean landScapeMode = false;
    ImageView statusImage;
    ShapeDrawable faceBoundingBox = null;
    ShapeDrawable tempBoundingBox = null;
    FrameLayout.LayoutParams paramsShape;
    FrameLayout.LayoutParams paramsShape2;

    //HeartBeat variables
    HeartBeatAlgorithm hb_hralgirthm;
    long hb_lastTime, hb_prev_timestamp, hb_interpolated_timestamp, hb_start_timestamp, hb_init_timestamp;
    boolean hb_running, hb_isDetecting, hb_started, hb_firstTime_flag, hb_covered, focusing, startingFaceControl;
    private int hb_skipSamples;
    int hb_positiveFrameCounter, hb_negativeFrameCounter, hb_lambda, hb_counter;
    SimpleMatrix hb_I, hb_D2, hb_Temp1, hb_Temp2;
    double[] hb_interpolated_value, hb_tempBuffer;
    double hb_prev_value, hb_init_value;
    PointF leftCorner, rightCorner;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = getApplicationContext();
        Log.i("HeartBeatAlgorithm", "AppCreated");
        myView = new View(MainActivity.this);
        preview = (FrameLayout) findViewById(R.id.preview);
        faceBoundingBox = new ShapeDrawable(new RectShape());
        faceBoundingBox.getPaint().setColor(Color.GREEN);//0xFFFFFFFF);
        faceBoundingBox.getPaint().setStyle(Paint.Style.STROKE);
        faceBoundingBox.getPaint().setStrokeWidth(10);
        tempBoundingBox = new ShapeDrawable(new RectShape());
        tempBoundingBox.getPaint().setColor(Color.YELLOW);//0xFFFFFFFF);
        tempBoundingBox.getPaint().setStyle(Paint.Style.STROKE);
        tempBoundingBox.getPaint().setStrokeWidth(10);
        faceShape = new View(context);
        faceShape.setBackground(faceBoundingBox);
        temperatureShape = new View(context);
        temperatureShape.setBackground(tempBoundingBox);
        paramsShape = new FrameLayout.LayoutParams(100, 100);
        paramsShape.setMargins(0, 0, 0, 0);
        paramsShape2 = new FrameLayout.LayoutParams(50, 25);
        paramsShape2.setMargins(0, 0, 0, 0);

        //heartbeat variables ini
        hb_hralgirthm = new HeartBeatAlgorithm();
        hb_skipSamples = 25;	// pick a pixel every skipSamples pixels
        hb_lastTime = 0;
        hb_isDetecting = false;
        hb_positiveFrameCounter = 0;
        hb_negativeFrameCounter = 0;
        hb_lambda = 10;
        hb_counter = 0;
        double[][] temp = new double[98][100];
        for (int i = 0; i < 98; i++ ){
            temp[i][i] = 1;
            temp[i][i+1] = -2;
            temp[i][i+2] = 1;
        }
        hb_I = SimpleMatrix.identity(100);
        hb_D2 = new SimpleMatrix(temp);
        hb_Temp1 = ((hb_D2.transpose()).mult(hb_D2)).scale(hb_lambda * hb_lambda);
        hb_Temp2 = hb_I.minus((hb_I.plus(hb_Temp1)).invert());
        hb_interpolated_value = new double[50000];
        hb_prev_timestamp = 0;
        hb_interpolated_timestamp = 0;
        hb_prev_value = 0;
        hb_tempBuffer = new double[100];
        hb_started = false;
        hb_init_timestamp = 0;
        hb_init_value = 0;
        hb_firstTime_flag = false;
        leftCorner=new PointF(0,0);
        rightCorner=new PointF(0,0);;
        focusing=false;
        storage = DataStorage.getInstance(context);
        saving = saveFaceData.getInstance(context);
        start = (Button) findViewById(R.id.faceConrol);
        startingFaceControl=false;

        statusImage = (ImageView) findViewById(R.id.statusImage);
        statusImage.setVisibility(View.VISIBLE);
        fpFeatureSupported = FacialProcessing.isFeatureSupported(FacialProcessing.FEATURE_LIST.FEATURE_FACIAL_PROCESSING);

        if (fpFeatureSupported && faceProc == null) {
            Toast.makeText(MainActivity.this, "Feature is supported", Toast.LENGTH_SHORT).show();
            faceProc = FacialProcessing.getInstance();  // Calling the Facial Processing Constructor.
            faceProc.setProcessingMode(FP_MODES.FP_MODE_VIDEO);
        } else {
            Toast.makeText(MainActivity.this, "Feature is NOT supported", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            cameraObj = Camera.open(1); // attempt to get a Camera instance
            Toast.makeText(MainActivity.this, "Front Camera is open", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Log.d("TAG", "Camera Does Not exist");// Camera is not available (in use or does not exist)
        }

        mPreview = new CameraSurfacePreview(MainActivity.this, cameraObj, faceProc);
        preview.removeAllViews();
        preview = (FrameLayout) findViewById(R.id.preview);
        preview.addView(mPreview, 0);
        preview.addView(faceShape, 1, paramsShape);
        preview.addView(temperatureShape, 2, paramsShape2);
        numFaceText = (TextView) findViewById(R.id.faceNum);
        faceYawText = (TextView) findViewById(R.id.yawVal);
        gazePointText = (TextView) findViewById(R.id.gazePoint);
        heartDataText = (TextView) findViewById(R.id.heartData);
        heartIndicator = (TextView) findViewById(R.id.heartIndicater);
        faceDetected=false;
        orientationListener();
        display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
/***********************************************************************/
        //Review the view and set text to be spritzed
        mSpritzerTextView = (SpritzerTextView) findViewById(R.id.spritzTV);
        mSpritzerTextView.setSpritzText("Galileo teacher and politician who lived in Florence from 1370 to 1450; at that time in the late 14th century, the family's surname shifted from Bonaiuti (or Buonaiuti) to Galilei. Galileo Bonaiuti was buried in the same church, the Basilica of Santa Croce in Florence, where about 200 years later his more famous descendant Galileo Galilei was also buried. When Galileo Galilei was eight, his family moved to Florence, but he was left with Jacopo Borghini for two years.[15] He then was educated in the Camaldolese Monastery at Vallombrosa, 35 km southeast of Florence.[15]");
        mProgressBar = (ProgressBar) findViewById(R.id.spritz_progress);
        mSpritzerTextView.attachProgressBar(mProgressBar);
        mSpritzerTextView.setWpm(500);
        mSpritzerTextView.setOnClickControlListener(new SpritzerTextView.OnClickControlListener() {

            @Override
            public void onPause() {
                Toast.makeText(MainActivity.this, "Spritzer has been paused", Toast.LENGTH_SHORT).show();
                Log.i("HeartBeatAlgorithm", "AppPause");
                startingFaceControl=false;
            }

            @Override
            public void onPlay() {
                Toast.makeText(MainActivity.this, "Spritzer is playing", Toast.LENGTH_SHORT).show();
                startingFaceControl=true;
                Log.i("HeartBeatAlgorithm", "AppPlay");

            }
        });

        mSpritzerTextView.setOnCompletionListener(new Spritzer.OnCompletionListener() {
            @Override
            public void onComplete() {
                Toast.makeText(MainActivity.this, "Spritzer is finished", Toast.LENGTH_SHORT).show();
                storage.save("Galileo", System.currentTimeMillis());
                saving.save("FaceDataSaving",System.currentTimeMillis());
                Log.i("HeartBeatAlgorithm", "AppFinish");
                startingFaceControl=false;


            }
        });

//        mSpritzerTextView.setDelayStrategy(new DelayStrategy() {
//            @Override
//            public int delayMultiplier(String word) {
//                if(word.contains("-")){
//                  return 5;
//                }
//                return 1;
//            }
//        });
        setupSeekBars();
    }

    /**
     * This is just shows two seek bars to change wpm and text size
     */
    private void setupSeekBars() {
        mSeekBarTextSize = (SeekBar) findViewById(R.id.seekBarTextSize);
        mSeekBarWpm = (SeekBar) findViewById(R.id.seekBarWpm);
        if (mSeekBarWpm != null && mSeekBarTextSize != null) {
            mSeekBarWpm.setMax(mSpritzerTextView.getWpm() * 2);

            mSeekBarTextSize.setMax((int) mSpritzerTextView.getTextSize() * 2);
            mSeekBarWpm.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (progress > 0) {
                        mSpritzerTextView.setWpm(progress);
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                }
            });
            mSeekBarTextSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    mSpritzerTextView.setTextSize(progress);

                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });

            mSeekBarWpm.setProgress(mSpritzerTextView.getWpm());
            mSeekBarTextSize.setProgress((int) mSpritzerTextView.getTextSize());
        }

    }

    public void OnClickStart(View view) {
        startingFaceControl=true;
    }



    private void orientationListener() {
        orientationEventListener = new OrientationEventListener(this, SensorManager.SENSOR_DELAY_NORMAL) {
            @Override
            public void onOrientationChanged(int orientation) {
                deviceOrientation = orientation;
            }
        };

        if (orientationEventListener.canDetectOrientation()) {
            orientationEventListener.enable();
        }

        presentOrientation = 90 * (deviceOrientation / 360) % 360;
    }

    public void setUI(int numFaces, int smileValue, int leftEyeBlink, int rightEyeBlink, int faceRollValue, int faceYawValue, int facePitchValue, PointF gazePointValue, int horizontalGazeAngle, int verticalGazeAngle, Rect faceRect, Point leftEyeCoord, Point rightEyeCoord) {
        numFaceText.setText("Faces: " + numFaces);
        faceYawText.setText("Yaw: " + faceYawValue);
        if ((numFaces > 0) && (faceYawValue<20) && (faceYawValue>=-30)) {
            statusImage.setImageResource(R.drawable.green);
            focusing=true;

            if(startingFaceControl){
                mSpritzerTextView.play();
                storage.AddWordIndex(System.currentTimeMillis(), mSpritzerTextView.getCurrentWordIndex());
            }

        }
        else{
            statusImage.setImageResource(R.drawable.red);
            focusing = false;
            if(startingFaceControl){
                mSpritzerTextView.pause();
                storage.AddWordIndex(System.currentTimeMillis(), mSpritzerTextView.getCurrentWordIndex());
            }
        }

        if (gazePointValue != null) {
            double x = Math.round(gazePointValue.x * 100.0) / 100.0;// Rounding the gaze point value.
            double y = Math.round(gazePointValue.y * 100.0) / 100.0;
            float d=(float)Math.sqrt((leftEyeCoord.x-rightEyeCoord.x)*(leftEyeCoord.x-rightEyeCoord.x)+(leftEyeCoord.y-rightEyeCoord.y)*(leftEyeCoord.y-rightEyeCoord.y))/4;
            //rectangle used for detecting the pulse
            leftCorner = new PointF(leftEyeCoord.x+d/2,leftEyeCoord.y-5*d/2);
            rightCorner = new PointF(rightEyeCoord.x-d/2,rightEyeCoord.y-d);
            //gazePointText.setText("Gaze: (" + x + "," + y + ")" + "l,t,r,b:" + faceRect.left + "," + faceRect.top + "," + faceRect.right + "," + faceRect.bottom + "\n lc: " + leftCorner + "\n rc:" + rightCorner);
            gazePointText.setText("Gaze: (" + x + "," + y + ")" + "\n lc_s: " + leftCorner + "\n rc_s:" + rightCorner+ "\n lc_m: ");
            preview.bringChildToFront(faceShape);
            preview.bringChildToFront(temperatureShape);
            paramsShape.width = faceRect.right-faceRect.left;
            paramsShape.height = faceRect.bottom-faceRect.top;
            paramsShape.setMargins(faceRect.left, faceRect.top, faceRect.right, faceRect.bottom);
            paramsShape2.width=(int)(3*d);
            paramsShape2.height=(int)(3*d/2);
            paramsShape2.setMargins((int)leftCorner.x,(int)leftCorner.y,(int)rightCorner.x,(int)rightCorner.y);
        } else {
            gazePointText.setText("Gaze: ( , )");
            preview.bringChildToFront(mPreview);
        }

        saving.AddFaceDataSample(numFaces, smileValue, leftEyeBlink, rightEyeBlink, faceRollValue,
                faceYawValue, facePitchValue, gazePointValue, horizontalGazeAngle, verticalGazeAngle, System.currentTimeMillis());
        storage.AddWordIndex(System.currentTimeMillis(), mSpritzerTextView.getCurrentWordIndex());
    }

    @Override
    protected void onPause() {
        Log.i("HeartBeatAlgorithm", "AppPaused");
        System.out.println("%%%%%%%%%%%%%%&&&&&&&&&&&&&&&&&&&&&&&& onPause");
        super.onPause();
        stopCamera();
    }

    @Override
    protected void onDestroy() {
        Log.i("HeartBeatAlgorithm", "AppClosed");
        //System.out.print("~~~~~~~~~$$$$$$$$$$$$$$$$$$$: " + dataStorage.get(2).length);
        System.out.println("%%%%%%%%%%%%%%&&&&&&&&&&&&&&&&&&&&&&&& onDestroy");
        super.onDestroy();

    }

    @Override
    protected void onResume() {
        System.out.println("%%%%%%%%%%%%%%&&&&&&&&&&&&&&&&&&&&&&&& onResume");
        Log.i("HeartBeatAlgorithm", "AppResumed");
        super.onResume();
        if (cameraObj != null) {
            stopCamera();
        }
        startCamera(1);
    }

    /*
     * This is a function to stop the camera preview. Release the appropriate objects for later use.
     */
    public void stopCamera() {
        if (cameraObj != null) {
            cameraObj.stopPreview();
            cameraObj.setPreviewCallback(null);
            preview.removeAllViews();
            preview.addView(mPreview, 0);
            preview.addView(faceShape, 1, paramsShape);
            preview.addView(temperatureShape, 2, paramsShape2);
            preview.bringChildToFront(faceShape);
            preview.bringChildToFront(temperatureShape);
            cameraObj.release();
            faceProc.release();
            faceProc = null;
        }

        cameraObj = null;
        hb_running=false;
    }

    /*
     * This is a function to start the camera preview. Call the appropriate constructors and objects.
     * @param-cameraIndex: Will specify which camera (front/back) to start.
     */
    public void startCamera(int cameraIndex) {
        Log.e("SUCCESS", "Preview surface valid");
        Log.i("INFO", "Setting preview size: 176, 144");
        if (fpFeatureSupported && faceProc == null) {
            Log.e("TAG", "Feature is supported");
            faceProc = FacialProcessing.getInstance();// Calling the Facial Processing Constructor.
        }

        try {
            cameraObj = Camera.open(cameraIndex);// attempt to get a Camera instance
        } catch (Exception e) {
            Log.d("TAG", "Camera Does Not exist");// Camera is not available (in use or does not exist)
        }

        mPreview = new CameraSurfacePreview(MainActivity.this, cameraObj, faceProc);
        preview.removeAllViews();
        preview = (FrameLayout) findViewById(R.id.preview);
        preview.addView(mPreview,0);
        preview.addView(faceShape, 1, paramsShape);
        preview.addView(temperatureShape, 2, paramsShape2);
        preview.bringChildToFront(faceShape);
        preview.bringChildToFront(temperatureShape);
        cameraObj.setPreviewCallback(MainActivity.this);
        hb_running = true;
    }

    /*
     * Detecting the face according to the new Snapdragon SDK. Face detection will now take place in this function.
     * 1) Set the Frame
     * 2) Detect the Number of faces.
     * 3) If(numFaces > 0) then do the necessary processing.
     */
    @Override
    public void onPreviewFrame(byte[] data, Camera arg1) {
        presentOrientation = (90 * Math.round(deviceOrientation / 90)) % 360;
        int dRotation = display.getRotation();
        PREVIEW_ROTATION_ANGLE angleEnum = PREVIEW_ROTATION_ANGLE.ROT_0;

        switch (dRotation) {
            case 0:
                displayAngle = 90;
                angleEnum = PREVIEW_ROTATION_ANGLE.ROT_90;
                break;

            case 1:
                displayAngle = 0;
                angleEnum = PREVIEW_ROTATION_ANGLE.ROT_0;
                break;

            case 2:
                // This case is never reached.
                break;

            case 3:
                displayAngle = 180;
                angleEnum = PREVIEW_ROTATION_ANGLE.ROT_180;
                break;
        }

        if (faceProc == null) {
            faceProc = FacialProcessing.getInstance();
        }

        Parameters params = cameraObj.getParameters();
        Size previewSize = params.getPreviewSize();
        surfaceWidth = mPreview.getWidth();
        surfaceHeight = mPreview.getHeight();

        // Landscape mode - front camera
        if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            faceProc.setFrame(data, previewSize.width, previewSize.height, true, angleEnum);
            cameraObj.setDisplayOrientation(displayAngle);
            landScapeMode = true;
        }
        // Portrait mode - front camera
        else{
            faceProc.setFrame(data, previewSize.width, previewSize.height, true, angleEnum);
            cameraObj.setDisplayOrientation(displayAngle);
            landScapeMode = false;
        }

        int numFaces = faceProc.getNumFaces();

        if (numFaces == 0) {
            Log.d("TAG", "No Face Detected");
            setUI(0, 0, 0, 0, 0, 0, 0, null, 0, 0, null, null, null);
            hb_covered=false;
        } else {
            Log.d("TAG", "Face Detected");
            faceArray = faceProc.getFaceData(EnumSet.of(FacialProcessing.FP_DATA.FACE_RECT,
                    FacialProcessing.FP_DATA.FACE_COORDINATES, FacialProcessing.FP_DATA.FACE_CONTOUR,
                    FacialProcessing.FP_DATA.FACE_SMILE, FacialProcessing.FP_DATA.FACE_ORIENTATION,
                    FacialProcessing.FP_DATA.FACE_BLINK, FacialProcessing.FP_DATA.FACE_GAZE, FacialProcessing.FP_DATA.FACE_CONTOUR));
            // faceArray = faceProc.getFaceData(); // Calling getFaceData() alone will give you all facial data except the
            // face
            // contour. Face Contour might be a heavy operation, it is recommended that you use it only when you need it.
            if (faceArray == null) {
                Log.e("TAG", "Face array is null");
                hb_covered=false;
            } else {
                if (faceArray[0].leftEyeObj == null) {
                    Log.e(TAG, "Eye Object NULL");
                } else {
                    Log.e(TAG, "Eye Object not NULL");
                }

                faceProc.normalizeCoordinates(surfaceWidth, surfaceHeight);
                for (int j = 0; j < numFaces; j++) {
                    smileValue = faceArray[j].getSmileValue();
                    leftEyeBlink = faceArray[j].getLeftEyeBlink();
                    rightEyeBlink = faceArray[j].getRightEyeBlink();
                    faceRollValue = faceArray[j].getRoll();
                    gazePointValue = faceArray[j].getEyeGazePoint();
                    pitch = faceArray[j].getPitch();
                    yaw = faceArray[j].getYaw();
                    horizontalGaze = faceArray[j].getEyeHorizontalGazeAngle();
                    verticalGaze = faceArray[j].getEyeVerticalGazeAngle();
                    faceRect = faceArray[j].rect;
                    leftEyeCoord = faceArray[j].leftEye;
                    rightEyeCoord = faceArray[j].rightEye;

                }
                setUI(numFaces, smileValue, leftEyeBlink, rightEyeBlink, faceRollValue, yaw, pitch, gazePointValue,
                        horizontalGaze, verticalGaze, faceRect, leftEyeCoord, rightEyeCoord);
                hb_covered=true;
                float d=(float)Math.sqrt((leftEyeCoord.x-rightEyeCoord.x)*(leftEyeCoord.x-rightEyeCoord.x)+(leftEyeCoord.y-rightEyeCoord.y)*(leftEyeCoord.y-rightEyeCoord.y))/4;
                leftCorner.set(leftEyeCoord.x + d / 2, leftEyeCoord.y - 5 * d / 2);
                rightCorner.set(rightEyeCoord.x-d/2,rightEyeCoord.y-d);
            }
        }
        hb_lastTime = System.nanoTime();

        if(hb_running)
        {
            addSample(data, hb_lastTime / 1000000, leftCorner, rightCorner);
        }

        returnBuffer(data);
    }

    void returnBuffer(byte buffer[])
    {
        try
        {
            cameraObj.addCallbackBuffer(buffer);
        }
        catch(Exception exception) { }
    }

//From here are the functions from HeartBeat.java
public void addSample(byte buffer[], long timestamp, PointF leftCorner, PointF rightCorner)
{
    if (!hb_started) {
        hb_started = true;
        hb_start_timestamp = timestamp;
    }
    if (hb_covered) {
        hb_positiveFrameCounter++;
        hb_negativeFrameCounter = 0;
        if (!hb_isDetecting) {
            if (hb_positiveFrameCounter >= 5) {
                hb_isDetecting = true;
          //      hb_observer.onCovered();
                Log.i("HeartBeatAlgorithm", "onCovered");

                double light = -analyze(buffer, hb_skipSamples, leftCorner, rightCorner);
                hb_init_timestamp = timestamp;
                hb_init_value = light;
                Log.i("HeartBeatAlgorithm", "addSample," + (timestamp - hb_start_timestamp) + "," + (light - hb_init_value));

                hb_interpolated_value[hb_counter] = light;
                hb_counter++;
                hb_prev_timestamp = timestamp;
                hb_interpolated_timestamp = timestamp + 50;
                hb_prev_value = light;
                DataStorage.AddCoverMoment(timestamp, (int) light);
            } else {
                Log.i("HeartBeatAlgorithm", "addSample," + (timestamp - hb_start_timestamp) + "," + 0);
            }
        } else {
            double light = -analyze(buffer, hb_skipSamples, leftCorner, rightCorner);

            while ( (hb_interpolated_timestamp <= timestamp) && (hb_interpolated_timestamp > hb_prev_timestamp)) {
                long deltaT1 = hb_interpolated_timestamp - hb_prev_timestamp;
                long deltaT2 = timestamp - hb_prev_timestamp;

                double deltaH2 = light - hb_prev_value;
                double deltaH1 = (deltaH2 * deltaT1)/deltaT2;
                double new_value = hb_prev_value + deltaH1;

                hb_interpolated_value[hb_counter] = new_value;
                hb_counter++;
                Log.i("Test", ""+new_value);
                if ( hb_counter <= 5) {
                    Log.i("HeartBeatAlgorithm", "addSample," + ((hb_counter-1)*50 + hb_init_timestamp - hb_start_timestamp) + "," + 0);
                } else {
                    if ( hb_counter <= 100 ) {

                        if (!hb_firstTime_flag) {
                            System.arraycopy(hb_interpolated_value, 3, hb_tempBuffer, 0, hb_counter - 3 );
                        }

                        double[][] V = new double[hb_counter - 5][hb_counter - 3];
                        for (int i = 0; i < hb_counter - 5; i++ ){
                            V[i][i] = 1;
                            V[i][i+1] = -2;
                            V[i][i+2] = 1;
                        }
                        SimpleMatrix V2 = SimpleMatrix.identity(hb_counter - 3);
                        SimpleMatrix V3 = new SimpleMatrix(V);
                        SimpleMatrix V4 = ((V3.transpose()).mult(V3)).scale(hb_lambda*hb_lambda);
                        SimpleMatrix V5 = V2.minus((V2.plus(V4)).invert());

                        SimpleMatrix tempMatrix = new SimpleMatrix(hb_counter - 3, 1, true, hb_tempBuffer);
                        SimpleMatrix newMatrix = V5.mult(tempMatrix);
                        Log.i("HeartBeatAlgorithm", "addSample," + ((hb_counter-1)*50 + hb_init_timestamp - hb_start_timestamp) + "," + newMatrix.get(hb_counter-4, 0));

                    }
                }
                // detrending
                if ( hb_counter == 100) {
                    if (!hb_firstTime_flag) {
                        hb_firstTime_flag = true;
                    }

                    hb_hralgirthm.begin_timestamp = 4950 + hb_init_timestamp - hb_start_timestamp;
                    System.arraycopy(hb_interpolated_value, 0, hb_tempBuffer, 0, 100 );

                    SimpleMatrix tempMatrix = new SimpleMatrix(100, 1, true, hb_tempBuffer);
                    SimpleMatrix newMatrix = hb_Temp2.mult(tempMatrix);

                    for (int i = 0; i < newMatrix.numRows(); i++ ) {
                        hb_hralgirthm.addSample(i*50 + hb_init_timestamp - hb_start_timestamp, newMatrix.get(i, 0));
                        DataStorage.AddSample(i * 50 + hb_init_timestamp - hb_start_timestamp, (int)newMatrix.get(i, 0));
                    }
                }

                if ( hb_counter > 100 ) {
                    System.arraycopy(hb_interpolated_value, hb_counter - 100, hb_tempBuffer, 0, 100 );

                    SimpleMatrix tempMatrix = new SimpleMatrix(100, 1, true, hb_tempBuffer);
                    SimpleMatrix newMatrix = hb_Temp2.mult(tempMatrix);

                    hb_hralgirthm.addSample((hb_counter - 1) * 50 + hb_init_timestamp - hb_start_timestamp, newMatrix.get(99, 0));
                    Log.i("HeartBeatAlgorithm", "addSample," + ((hb_counter - 1) * 50 + hb_init_timestamp - hb_start_timestamp) + "," + newMatrix.get(99, 0));
                    DataStorage.AddSample((hb_counter - 1) * 50 + hb_init_timestamp - hb_start_timestamp, (int)newMatrix.get(99, 0));
                }

                hb_interpolated_timestamp = hb_interpolated_timestamp + 50;
            }

            hb_prev_timestamp = timestamp;
            hb_prev_value = light;
        }
    } else {
        hb_negativeFrameCounter++;
        hb_positiveFrameCounter = 0;
        if (hb_isDetecting) {
            if (hb_negativeFrameCounter >= 5) {
                hb_isDetecting = false;
       //         hb_observer.onUncovered();
                Log.i("HeartBeatAlgorithm", "onUncovered");
                hb_hralgirthm.restart();
                reset();
                Log.i("HeartBeatAlgorithm", "addSample," + (timestamp - hb_start_timestamp) + "," + 0);
                DataStorage.AddUncoverMoment(timestamp, 0);
            } else {
                Log.i("HeartBeatAlgorithm", "addSample," + (timestamp - hb_start_timestamp) + "," + 0);
            }
        } else {
            Log.i("HeartBeatAlgorithm", "addSample," + (timestamp - hb_start_timestamp) + "," + 0);
        }
    }
}

    public double analyze(byte frame[], int sample_span, PointF leftCorner, PointF rightCorner)
    {
        //Using Averaging Method
        float x1=leftCorner.x;
        float y1=leftCorner.y;
        float x2=rightCorner.x;
        float y2=rightCorner.y;
        float rX1=x1/150*1280;
        float rX2=x2/150*1280;
        float rY1=y1/200*720;
        float rY2=y2/200*720;
        int shushu=0;
        double sum = 0;
        for( int i=0;i<1280; i++)
        {
            for(int j=0;j<720;j++)
            {
                if((i>=rX1)&&(i<=rX2)&&(j>=rY1)&&(j<=rY2)){
                    int pos=1280*j+i;
                    sum +=frame[pos];
                    shushu++;
                }
            }
        }
        double avg=sum/shushu;
        heartDataText.setText("Heart Data: "+avg);
        //return avg*36864;
        return avg*4500;
        //Using skipstamp Method
    }

    public void reset() {
        hb_counter = 0;
        hb_interpolated_value = new double[50000];
        hb_prev_timestamp = 0;
        hb_interpolated_timestamp = 0;
        hb_prev_value = 0;
        hb_init_timestamp = 0;
    }



}
