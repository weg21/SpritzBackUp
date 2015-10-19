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
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera.Size;
import android.hardware.SensorManager;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.OrientationEventListener;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import java.util.EnumSet;
import android.view.LayoutInflater;

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
    private SurfaceView mPreview;
    SurfaceView mPreview2;
    FrameLayout preview2;
    DataStorage storage;
    saveFaceData saving;
    HeartBeat heartrate;
    Button start;
    Canvas cv = new Canvas();
    OrientationEventListener orientationEventListener;
    int deviceOrientation;
    int presentOrientation;
    FacialProcessing faceProc;
    boolean fpFeatureSupported = false;
    int cameraIndex;// Integer to keep track of which camera is open.
    Camera cameraObj;
    boolean faceDetected;
    boolean lookingAtScreen;
    TextView numFaceText,faceYawText,gazePointText,smileValueText, leftBlinkText, rightBlinkText,faceRollText,
            facePitchText, horizontalGazeText, verticalGazeText;
    int surfaceWidth = 0;
    int surfaceHeight = 0;
    int smileValue = 0;
    int leftEyeBlink = 0;
    int rightEyeBlink = 0;
    int faceRollValue = 0;
    int pitch = 0;
    int yaw = 0;
    int horizontalGaze = 0;
    int verticalGaze = 0;
    PointF gazePointValue = null;
    Point leftEyeCoord=null;
    Point rightEyeCoord=null;
    Rect faceRect = null;
    View myView;
    View shapeView;
    View shapeView2;
    float rounded;
    Display display;
    int displayAngle;
    FaceData[] faceArray = null;// Array in which all the face data values will be returned for each face detected.
    boolean landScapeMode = false;
    ImageView statusImage;
    LayoutInflater controlInflater = null;
    ShapeDrawable sd = null;
    ShapeDrawable sd2 = null;
    FrameLayout.LayoutParams paramsShape;
    FrameLayout.LayoutParams paramsShape2;


    //---------
    PulseData pulData = null;

    /**************************************************
    long start_timestamp;
    int positiveFrameCounter;
    int negativeFrameCounter;
    boolean isDetecting;
    int skipSamples;
    long init_timestamp;
    double init_value;
    double [] interpolated_value;
    int counter;
    long prev_timestamp;
    long interpolated_timestamp;
    double prev_value;
    boolean firstTime_flag;
    double[] tempBuffer;
    int lambda;
    BeatObserver observer;
    SimpleMatrix Temp1;
    SimpleMatrix Temp2;
    SimpleMatrix I;
    HeartBeatAlgorithm hralgirthm;
    //**************************************************/

    public static Context getAppContext() {
        return MainActivity.context;
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = getApplicationContext();
        myView = new View(MainActivity.this);
        preview2 = (FrameLayout) findViewById(R.id.preview2);
       // framel = (FrameLayout) findViewById(R.id.test2);
        sd = new ShapeDrawable(new RectShape());
        sd.getPaint().setColor(Color.GREEN);//0xFFFFFFFF);
        sd.getPaint().setStyle(Paint.Style.STROKE);
        sd.getPaint().setStrokeWidth(10);

        sd2 = new ShapeDrawable(new RectShape());
        sd2.getPaint().setColor(Color.YELLOW);//0xFFFFFFFF);
        sd2.getPaint().setStyle(Paint.Style.STROKE);
        sd2.getPaint().setStrokeWidth(10);
        shapeView = new View(context);
        shapeView.setBackground(sd);
        shapeView2 = new View(context);
        shapeView2.setBackground(sd2);

        paramsShape = new FrameLayout.LayoutParams(100, 100);
        paramsShape.setMargins(0, 0, 0, 0);

        paramsShape2 = new FrameLayout.LayoutParams(50, 25);
        paramsShape2.setMargins(0, 0, 0, 0);
        //--------------
        //pulData = new PulseData(mPreview2);
        /******************
        isDetecting = false;
        skipSamples = 3;
        init_timestamp=0;
        init_value=0;
        interpolated_value = new double[50000];
        counter=0;
        prev_timestamp = 0;
        interpolated_timestamp = 0;
        prev_value = 0;
        firstTime_flag = false;
        tempBuffer = new double[100];
        lambda = 10;
        observer = null;
        I = SimpleMatrix.identity(100);
        Temp2 = I.minus((I.plus(Temp1)).invert());
        hralgirthm = new HeartBeatAlgorithm();
        ******************/

        //preview2.addView(shapeView, params);




/*
        start = (Button) findViewById(R.id.camBtn);
        context = getApplicationContext();
        Log.i("HeartBeatAlgorithm", "AppCreated");
        storage = DataStorage.getInstance(context);
        saving = saveFaceData.getInstance(context);
        mPreview = (SurfaceView) findViewById(R.id.preview);
        mPreview.setMinimumWidth(176);
        mPreview.setMinimumHeight(144);

        heartrate = new HeartBeat(mPreview, this);
        heartrate.setBPMObserver(this);
       // heartrate.stop();
       // heartrate.start();
*/
        /*
        controlInflater = LayoutInflater.from(getBaseContext());
       // LinearLayout temp = (LinearLayout) findViewById(R.id.ll);

        View viewControl = controlInflater.inflate(R.layout.control, null);
        LayoutParams layoutParamsControl
                = new LayoutParams(LayoutParams.FILL_PARENT,
                LayoutParams.FILL_PARENT);
        this.addContentView(viewControl, layoutParamsControl);




 /***********************************************************************/
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

//        cameraIndex = Camera.getNumberOfCameras() - 1;// Start with front Camera
        try {
            cameraObj = Camera.open(1); // attempt to get a Camera instance
            Toast.makeText(MainActivity.this, "Front Camera is open", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            //Log.d("TAG", "Camera Does Not exist");// Camera is not available (in use or does not exist)
        }

        //cameraObj.setPreviewCallback(MainActivity.this);
        mPreview2 = new CameraSurfacePreview(MainActivity.this, cameraObj, faceProc);
//        mPreview2.setMinimumWidth(176);
//        mPreview2.setMinimumHeight(144);
        preview2.removeAllViews();
        preview2 = (FrameLayout) findViewById(R.id.preview2);
        preview2.addView(mPreview2, 0);
        preview2.addView(shapeView, 1, paramsShape);
        preview2.addView(shapeView2, 2, paramsShape2);



        //cameraObj.setPreviewCallback(MainActivity.this);
        numFaceText = (TextView) findViewById(R.id.faceNum);
        faceYawText = (TextView) findViewById(R.id.yawVal);
        gazePointText = (TextView) findViewById(R.id.gazePoint);
        faceDetected=false;
        lookingAtScreen=false;
        //Toast.makeText(MainActivity.this, "Faces number: "+faceProc.getNumFaces(), Toast.LENGTH_SHORT).show();
        orientationListener();
        display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();


/***********************************************************************/
        //Review the view and set text to be spritzed
        mSpritzerTextView = (SpritzerTextView) findViewById(R.id.spritzTV);
        mSpritzerTextView.setSpritzText("Galileo teacher and politician who lived in Florence from 1370 to 1450; at that time in the late 14th century, the family's surname shifted from Bonaiuti (or Buonaiuti) to Galilei. Galileo Bonaiuti was buried in the same church, the Basilica of Santa Croce in Florence, where about 200 years later his more famous descendant Galileo Galilei was also buried. When Galileo Galilei was eight, his family moved to Florence, but he was left with Jacopo Borghini for two years.[15] He then was educated in the Camaldolese Monastery at Vallombrosa, 35 km southeast of Florence.[15]");
        //This attaches a progress bar that show exactly how far you are into your spritz
        mProgressBar = (ProgressBar) findViewById(R.id.spritz_progress);
        mSpritzerTextView.attachProgressBar(mProgressBar);
        //Set how fast the spritzer should go
        mSpritzerTextView.setWpm(500);
        //Set Click Control listeners, these will be called when the user uses the click controls
        mSpritzerTextView.setOnClickControlListener(new SpritzerTextView.OnClickControlListener() {

            @Override
            public void onPause() {
                Toast.makeText(MainActivity.this, "Spritzer has been paused", Toast.LENGTH_SHORT).show();
                Log.i("HeartBeatAlgorithm", "AppPause");
               // Toast.makeText(MainActivity.this, "Here we are: "+DataStorage.AddWordIndex(System.currentTimeMillis(), mSpritzerTextView.getCurrentWordIndex()), Toast.LENGTH_SHORT).show();
              //  Toast.makeText(MainActivity.this, storage.toString(), Toast.LENGTH_SHORT).show();

            //       heartrate.stop();
        //mSpritzerTextView.getCurrentWordIndex()
                //
            }

            @Override
            public void onPlay() {
                Toast.makeText(MainActivity.this, "Spritzer is playing", Toast.LENGTH_SHORT).show();
           //     heartrate.start();
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

        //RectShape rs = new RectShape();

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
/*
    public void OnClickStart(View view) {//??????????????????????
        Button btn = (Button)view;
//        heartrate.stop();
        heartrate.start();
        onUncovered();
    }
    public void onBeat(int heartrate, int duration) {
        // TODO Auto-generated method stub
        String hr = "" + heartrate;

        Log.i("HeartBeatAlgorithm", "onBeat");
    }

    public void onCameraError(Exception exception, Parameters parameters) {
        // TODO Auto-generated method stub

    }

    public void onHBError() {
        // TODO Auto-generated method stub

    }

    public void onHBStart() {

    }

    public void onHBStop(){

    }
    public void onSample(long timestamp, double value){

    }
    public void onValidRR(long timestamp, int value){

    }
    public void onValidatedRR(long timestamp, int value){

    }
    public void onCovered(){
        mSpritzerTextView.play();
        Toast.makeText(MainActivity.this, "Spritzer is playing by camera", Toast.LENGTH_SHORT).show();

        Log.i("HeartBeatAlgorithm", "onCovered");
    }
    public void onUncovered(){

        mSpritzerTextView.pause();

        Toast.makeText(MainActivity.this, "Spritzer has been paused by camera.", Toast.LENGTH_SHORT).show();
       // storage.AddWordIndex(System.currentTimeMillis(), mSpritzerTextView.getCurrentWordIndex());

        Log.i("HeartBeatAlgorithm", "onUncovered");
       // System.out.println("~~~``````````~~~~~~~~~`" + String.valueOf(DataStorage.AddWordIndex(System.currentTimeMillis(), mSpritzerTextView.getCurrentWordIndex())));
       // Toast.makeText(MainActivity.this,storage.save("Galileo",System.currentTimeMillis()) , Toast.LENGTH_SHORT).show();
    }

*/
  /*

    FaceDetectionListener faceDetectionListener = new FaceDetectionListener() {

        @Override
        public void onFaceDetection(Face[] faces, Camera camera) {
            Log.e(TAG, "Faces Detected through FaceDetectionListener = " + faces.length);
        }
    };

*/

    private void orientationListener() {
        //System.out.println("%%%%%%%%%%%%%%&&&&&&&&&&&&&&&&&&&&&&&& orientationListener");
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

    private void writeFaceData(int numFaces, int smileValue, int leftEyeBlink, int rightEyeBlink, int faceRollValue,
                               int faceYawValue, int facePitchValue, PointF gazePointValue, int horizontalGazeAngle, int verticalGazeAngle, long timestamp)
    {


    }

    /*
     * Function for the screen touch action listener. On touching the screen, the face data info will be displayed.
     */



    /*
     * Function for pause button action listener to pause and resume the preview.
     */

    /*
     * This function will update the TextViews with the new values that come in.
     */

    public void setUI(int numFaces, int smileValue, int leftEyeBlink, int rightEyeBlink, int faceRollValue,
                      int faceYawValue, int facePitchValue, PointF gazePointValue, int horizontalGazeAngle, int verticalGazeAngle, Rect faceRect, Point leftEyeCoord, Point rightEyeCoord) {
        System.out.println("%%%%%%%%%%%%%%&&&&&&&&&&&&&&&&&&&&&&&& setUI");
        //Toast.makeText(MainActivity.this, "SETUI is running", Toast.LENGTH_SHORT).show();

        numFaceText.setText("Faces: " + numFaces);

//        smileValueText.setText("Smile Value: " + smileValue);
//        leftBlinkText.setText("Left Eye Blink Value: " + leftEyeBlink);
//        rightBlinkText.setText("Right Eye Blink Value " + rightEyeBlink);
//        faceRollText.setText("Face Roll Value: " + faceRollValue);
        faceYawText.setText("Yaw: " + faceYawValue);
//        facePitchText.setText("Face Pitch Value: " + facePitchValue);
//        horizontalGazeText.setText("Horizontal Gaze: " + horizontalGazeAngle);
//        verticalGazeText.setText("VerticalGaze: " + verticalGazeAngle);

        if ((numFaces > 0) && (faceYawValue*faceYawValue < 500)) {
            statusImage.setImageResource(R.drawable.green);
        }
        else{
            statusImage.setImageResource(R.drawable.red);
        }

        if (gazePointValue != null) {
            double x = Math.round(gazePointValue.x * 100.0) / 100.0;// Rounding the gaze point value.
            double y = Math.round(gazePointValue.y * 100.0) / 100.0;
            float d=(float)Math.sqrt((leftEyeCoord.x-rightEyeCoord.x)*(leftEyeCoord.x-rightEyeCoord.x)+(leftEyeCoord.y-rightEyeCoord.y)*(leftEyeCoord.y-rightEyeCoord.y))/4;
            //rectangle used for detecting the pulse
            PointF leftCorner = new PointF(leftEyeCoord.x+d/2,leftEyeCoord.y-5*d/2);
            PointF rightCorner = new PointF(rightEyeCoord.x-d/2,rightEyeCoord.y-d);
            gazePointText.setText("Gaze: (" + x + "," + y + ")" + "l,t,r,b:" + faceRect.left + "," + faceRect.top + "," + faceRect.right + "," + faceRect.bottom + "\n lc: " + leftCorner + "\n rc:" + rightCorner);
            preview2.bringChildToFront(shapeView);
            preview2.bringChildToFront(shapeView2);
            paramsShape.width = faceRect.right-faceRect.left;
            paramsShape.height = faceRect.bottom-faceRect.top;
            paramsShape.setMargins(faceRect.left, faceRect.top, faceRect.right, faceRect.bottom);
            paramsShape2.width=(int)(3*d);
            paramsShape2.height=(int)(3*d/2);
            paramsShape2.setMargins((int)leftCorner.x,(int)leftCorner.y,(int)rightCorner.x,(int)rightCorner.y);
        } else {
            gazePointText.setText("Gaze: ( , )");
            preview2.bringChildToFront(mPreview2);
        }

        saving.AddFaceDataSample(numFaces, smileValue, leftEyeBlink, rightEyeBlink, faceRollValue,
                faceYawValue, facePitchValue, gazePointValue, horizontalGazeAngle, verticalGazeAngle, System.currentTimeMillis());
        storage.AddWordIndex(System.currentTimeMillis(), mSpritzerTextView.getCurrentWordIndex());
    }

    @Override
    protected void onPause() {
        System.out.println("%%%%%%%%%%%%%%&&&&&&&&&&&&&&&&&&&&&&&& onPause");
        super.onPause();
        stopCamera();
    }

    @Override
    protected void onDestroy() {
        System.out.println("%%%%%%%%%%%%%%&&&&&&&&&&&&&&&&&&&&&&&& onDestroy");
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        System.out.println("%%%%%%%%%%%%%%&&&&&&&&&&&&&&&&&&&&&&&& onResume");
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
        System.out.println("%%%%%%%%%%%%%%&&&&&&&&&&&&&&&&&&&&&&&& stopCamera");
        if (cameraObj != null) {
            cameraObj.stopPreview();
            cameraObj.setPreviewCallback(null);
            preview2.removeAllViews();
            //preview2.removeView(mPreview2);
           // preview2.removeView(shapeView);
            preview2.addView(mPreview2, 0);
            preview2.addView(shapeView, 1, paramsShape);
            preview2.addView(shapeView2, 2, paramsShape2);
            preview2.bringChildToFront(shapeView);
            preview2.bringChildToFront(shapeView2);
            cameraObj.release();
            faceProc.release();
            faceProc = null;
        }

        cameraObj = null;
    }

    /*
     * This is a function to start the camera preview. Call the appropriate constructors and objects.
     * @param-cameraIndex: Will specify which camera (front/back) to start.
     */
    public void startCamera(int cameraIndex) {
        System.out.println("%%%%%%%%%%%%%%&&&&&&&&&&&&&&&&&&&&&&&& startCamera");
        if (fpFeatureSupported && faceProc == null) {

            //Log.e("TAG", "Feature is supported");
            faceProc = FacialProcessing.getInstance();// Calling the Facial Processing Constructor.
        }

        try {
            cameraObj = Camera.open(cameraIndex);// attempt to get a Camera instance
        } catch (Exception e) {
            //Log.d("TAG", "Camera Does Not exist");// Camera is not available (in use or does not exist)
        }

        mPreview2 = new CameraSurfacePreview(MainActivity.this, cameraObj, faceProc);
        preview2.removeAllViews();
       // preview2.removeView(mPreview2);
        //preview2.removeView(shapeView);
        preview2 = (FrameLayout) findViewById(R.id.preview2);
        preview2.addView(mPreview2,0);
        preview2.addView(shapeView,1, paramsShape);
        preview2.addView(shapeView2, 2, paramsShape2);
        preview2.bringChildToFront(shapeView);
        preview2.bringChildToFront(shapeView2);
        cameraObj.setPreviewCallback(MainActivity.this);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        System.out.println("%%%%%%%%%%%%%%&&&&&&&&&&&&&&&&&&&&&&&& onCreateOptionsMenu");
        // Inflate the menu; this adds items to the action bar if it is present.
       // getMenuInflater().inflate(R.menu.camera_preview, menu);
        return true;
    }

    /*
     * Detecting the face according to the new Snapdragon SDK. Face detection will now take place in this function.
     * 1) Set the Frame
     * 2) Detect the Number of faces.
     * 3) If(numFaces > 0) then do the necessary processing.
     */
    @Override
    public void onPreviewFrame(byte[] data, Camera arg1) {

        System.out.println("%%%%%%%%%%%%%%&&&&&&&&&&&&&&&&&&&&&&&& onPreviewFrames");

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
        surfaceWidth = mPreview2.getWidth();
        surfaceHeight = mPreview2.getHeight();

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
     /*       if (drawView != null) {
                preview.removeView(drawView);

                drawView = new DrawView(this, null, false, 0, 0, null, landScapeMode);
                preview.addView(drawView);
            }*/
            setUI(0, 0, 0, 0, 0, 0, 0, null, 0, 0, null, null, null);
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
            } else {
                if (faceArray[0].leftEyeObj == null) {
                    Log.e(TAG, "Eye Object NULL");
                } else {
                    Log.e(TAG, "Eye Object not NULL");
                }

                faceProc.normalizeCoordinates(surfaceWidth, surfaceHeight);
                //           preview.removeView(drawView);// Remove the previously created view to avoid unnecessary stacking of Views.
                //           drawView = new DrawView(this, faceArray, true, surfaceWidth, surfaceHeight, cameraObj, landScapeMode);
                //           preview.addView(drawView);
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
            }
        }
    }
/**********
    public void addSample(byte buffer[], long timestamp, FrameLayout.LayoutParams pm, int numFaces, PointF leftCorner, PointF rightCorner)
    {
        start_timestamp = timestamp;
        //boolean covered = detectFullCovering(buffer, c.getParameters().getPreviewSize().width, c.getParameters().getPreviewSize().height);
        boolean detected=false;
        if(numFaces>0) {
            detected=true;
        }
        if (detected) {
            positiveFrameCounter++;
            negativeFrameCounter = 0;
            if (!isDetecting) {
                if (positiveFrameCounter >= 5) {
                    isDetecting = true;
                    observer.onCovered();

                    double light = -analyze(buffer, skipSamples, pm, numFaces, leftCorner, rightCorner);
                    init_timestamp = timestamp;
                    init_value = light;
                    Log.i("HeartBeatAlgorithm", "addSample," + (timestamp - start_timestamp) + "," + (light - init_value));

                    interpolated_value[counter] = light;
                    counter++;
                    prev_timestamp = timestamp;
                    interpolated_timestamp = timestamp + 50;
                    prev_value = light;
                } else {
                    Log.i("HeartBeatAlgorithm", "addSample," + (timestamp - start_timestamp) + "," + 0);
                }
            } else {
                double light = -analyze(buffer, skipSamples, pm, numFaces, leftCorner, rightCorner);

                while ( (interpolated_timestamp <= timestamp) && (interpolated_timestamp > prev_timestamp)) {
                    long deltaT1 = interpolated_timestamp - prev_timestamp;
                    long deltaT2 = timestamp - prev_timestamp;

                    double deltaH2 = light - prev_value;
                    double deltaH1 = (deltaH2 * deltaT1)/deltaT2;
                    double new_value = prev_value + deltaH1;

                    interpolated_value[counter] = new_value;
                    counter++;
                    Log.i("Test", ""+new_value);
                    if ( counter <= 5) {
                        Log.i("HeartBeatAlgorithm", "addSample," + ((counter-1)*50 + init_timestamp - start_timestamp) + "," + 0);
                    } else {
                        if ( counter <= 100 ) {

                            if (!firstTime_flag) {
                                System.arraycopy(interpolated_value, 3, tempBuffer, 0, counter - 3 );
                            }

                            double[][] V = new double[counter - 5][counter - 3];
                            for (int i = 0; i < counter - 5; i++ ){
                                V[i][i] = 1;
                                V[i][i+1] = -2;
                                V[i][i+2] = 1;
                            }

                            SimpleMatrix V2 = SimpleMatrix.identity(counter - 3);
                            SimpleMatrix V3 = new SimpleMatrix(V);
                            SimpleMatrix V4 = ((V3.transpose()).mult(V3)).scale(lambda*lambda);
                            SimpleMatrix V5 = V2.minus((V2.plus(V4)).invert());

                            SimpleMatrix tempMatrix = new SimpleMatrix(counter - 3, 1, true, tempBuffer);
                            SimpleMatrix newMatrix = V5.mult(tempMatrix);
                            Log.i("HeartBeatAlgorithm", "addSample," + ((counter-1)*50 + init_timestamp - start_timestamp) + "," + newMatrix.get(counter-4, 0));

                        }
                    }
                    if ( counter == 100) {
                        if (!firstTime_flag) {
                            firstTime_flag = true;
                        }

                        hralgirthm.begin_timestamp = 4950 + init_timestamp - start_timestamp;
                        System.arraycopy(interpolated_value, 0, tempBuffer, 0, 100 );

                        SimpleMatrix tempMatrix = new SimpleMatrix(100, 1, true, tempBuffer);
                        SimpleMatrix newMatrix = Temp2.mult(tempMatrix);

                        for (int i = 0; i < newMatrix.numRows(); i++ ) {
                            hralgirthm.addSample(i*50 + init_timestamp - start_timestamp, newMatrix.get(i, 0));
//    			        	DataStorage.AddSample(i*50 + init_timestamp - start_timestamp, newMatrix.get(i, 0));

                            if(observer != null)
                            {
                                observer.onSample(i*50 + init_timestamp - start_timestamp, newMatrix.get(i, 0));
                            }
                        }
                    }

                    if ( counter > 100 ) {
                        System.arraycopy(interpolated_value, counter - 100, tempBuffer, 0, 100 );

                        SimpleMatrix tempMatrix = new SimpleMatrix(100, 1, true, tempBuffer);
                        SimpleMatrix newMatrix = Temp2.mult(tempMatrix);

                        hralgirthm.addSample((counter-1)*50 + init_timestamp - start_timestamp, newMatrix.get(99, 0));
                        Log.i("HeartBeatAlgorithm", "addSample," + ((counter-1)*50 + init_timestamp - start_timestamp) + "," + newMatrix.get(99, 0));
//    		        	DataStorage.AddSample((counter-1)*50 + init_timestamp - start_timestamp, newMatrix.get(99, 0));

                        if(observer != null)
                        {
                            observer.onSample((counter-1)*50 + init_timestamp - start_timestamp, newMatrix.get(99, 0));
                        }
                    }

                    interpolated_timestamp = interpolated_timestamp + 50;
                }

                prev_timestamp = timestamp;
                prev_value = light;
            }
        } else {
            negativeFrameCounter++;
            positiveFrameCounter = 0;
            if (isDetecting) {
                if (negativeFrameCounter >= 5) {
                    isDetecting = false;
                    observer.onUncovered();
                    hralgirthm.restart();
                    reset();
                    Log.i("HeartBeatAlgorithm", "addSample," + (timestamp - start_timestamp) + "," + 0);
                } else {
                    Log.i("HeartBeatAlgorithm", "addSample," + (timestamp - start_timestamp) + "," + 0);
                }
            } else {
                Log.i("HeartBeatAlgorithm", "addSample," + (timestamp - start_timestamp) + "," + 0);
            }
        }
    }

    public double analyze(byte frame[], int sample_span, FrameLayout.LayoutParams pm, int numFaces, PointF leftCorner, PointF rightCorner)
    {
        int ychannel_size = (frame.length * 2) / 3;

        double sum = 0;

        for ( int pos = 0; pos < ychannel_size; pos +=  sample_span)
        {
            sum += frame[pos] & 0xff;
        }

        return sum;
    }
    public void reset() {
        counter = 0;
        interpolated_value = new double[50000];
        prev_timestamp = 0;
        interpolated_timestamp = 0;
        prev_value = 0;
        init_timestamp = 0;
    }

    void allocatePreviewBuffers()
    {
        android.hardware.Camera.Size size = cameraObj.getParameters().getPreviewSize();

        int frame_size = paramsShape2.width * paramsShape2.height;
        //size.height * size.width * 2;
        int buffer_size = 4000000; // approximately 4MB
        int max_frame = 8;
        int frames = 0;

        if(frame_size != 0)
        {
            frames = Math.max(Math.min(buffer_size / frame_size, max_frame), 1);
        }

        for (int i =0; i < frames; i++)
        {
            try
            {
                cameraObj.addCallbackBuffer(new byte[frame_size]);
            }
            catch(Exception exception) { }
        }
    }

*****************/

}
