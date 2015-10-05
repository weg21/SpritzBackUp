package edu.pitt.cs.mips.hrv_exp;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.util.Log;

import java.io.IOException;
import java.util.Arrays;

import org.ejml.simple.SimpleMatrix;

public class HeartBeat
        implements android.hardware.Camera.PreviewCallback, PulseObserver
{

    public HeartBeat(SurfaceView surfaceview, Context context1)
    {
        observer = null;
        hralgirthm = new HeartBeatAlgorithm();;
        Flashlight flashlight = new Flashlight();
        flashLight = flashlight;
        lightIntensity = 0;
        skipSamples = 25;	// pick a pixel every skipSamples pixels
        startRequest = false;
        previewSurfaceValid = false;
        c = null;
        useCameraLed = true;
        cameraOpen = false;
        lastTime = 0;
        frameRate = 30;
        lastFrameRateBoundTimestamp = 0;
        useFrameRateTiming = true;
        cntSamplesThisSession = 0;
        preinit();
        context = context1;
        previewSurface = surfaceview;
        previewSurfaceHolder = surfaceview.getHolder();
        previewSurfaceHolder.addCallback(getPreviewSurfaceCallback());
        hralgirthm.setPulseObserver(this);

        isDetecting = false;
        positiveFrameCounter = 0;
        negativeFrameCounter = 0;

        lambda = 10;
        counter = 0;
        double[][] temp = new double[98][100];
        for (int i = 0; i < 98; i++ ){
            temp[i][i] = 1;
            temp[i][i+1] = -2;
            temp[i][i+2] = 1;
        }

        I = SimpleMatrix.identity(100);
        D2 = new SimpleMatrix(temp);
        Temp1 = ((D2.transpose()).mult(D2)).scale(lambda*lambda);
        Temp2 = I.minus((I.plus(Temp1)).invert());
        interpolated_value = new double[50000];
        prev_timestamp = 0;
        interpolated_timestamp = 0;
        prev_value = 0;
        tempBuffer = new double[100];

        started = false;

        init_timestamp = 0;
        init_value = 0;
        firstTime_flag = false;
    }

    private Camera getDefault()
    {
        return Camera.open();
    }

    private void preinit()
    {

    }

    private void resetSampleAquisitor()
    {
        lastFrameRateBoundTimestamp = 0;
        useFrameRateTiming = true;
        cntSamplesThisSession = 0;
    }

    private void setFlash(boolean flag)
    {
        try
        {
            android.hardware.Camera.Parameters parameters = c.getParameters();
            if(flag)
                parameters.setFlashMode(Parameters.FLASH_MODE_TORCH);
            else
                parameters.setFlashMode(Parameters.FLASH_MODE_OFF);
            c.setParameters(parameters);
//            flashLight.setFlashlightEnabled(flag);
        } catch(Exception exception) {
            c.release();
            c = null;
        }
    }

    private void setPreviewCallback()
    {
        c.setPreviewCallback(this);

    }

    public void addSample(byte buffer[], long timestamp)
    {
        if (!started) {
            started = true;
            start_timestamp = timestamp;
        }

        boolean covered = detectFullCovering(buffer, c.getParameters().getPreviewSize().width, c.getParameters().getPreviewSize().height);

        if (covered) {
            positiveFrameCounter++;
            negativeFrameCounter = 0;
            if (!isDetecting) {
                if (positiveFrameCounter >= 5) {
                    isDetecting = true;
                    observer.onCovered();

                    double light = -analyze(buffer, skipSamples);
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
                double light = -analyze(buffer, skipSamples);

                while ( (interpolated_timestamp <= timestamp) && (interpolated_timestamp > prev_timestamp)) {
                    long deltaT1 = interpolated_timestamp - prev_timestamp;
                    long deltaT2 = timestamp - prev_timestamp;

                    double deltaH2 = light - prev_value;
                    double deltaH1 = (deltaH2 * deltaT1)/deltaT2;
                    double new_value = prev_value + deltaH1;

                    interpolated_value[counter] = new_value;
                    counter++;
                    Log.i("Test", ""+new_value);

/*    				if (counter == 20) {
    					double temp = 0;
    					System.arraycopy(interpolated_value, 0, tempBuffer, 0, 20 );
    					Arrays.sort(tempBuffer, 0, 20);

    					for (int i = 5; i < 15; i++) {
    						temp = temp + interpolated_value[i];
    					}
    					init_value = temp/10;
    				}*/

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

/*    				if ( counter <= 100 ) {
    					Log.i("HeartBeatAlgorithm", "addSample," + ((counter-1)*50 + init_timestamp - start_timestamp) + "," + (new_value - init_value));
    				}*/

                    // detrending
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
/*        			double light = -analyze(buffer, skipSamples);

        			while ( (interpolated_timestamp <= timestamp) && (interpolated_timestamp > prev_timestamp)) {
        				long deltaT1 = interpolated_timestamp - prev_timestamp;
        				long deltaT2 = timestamp - prev_timestamp;

        				double deltaH2 = light - prev_value;
        				double deltaH1 = (deltaH2 * deltaT1)/deltaT2;
        				double new_value = prev_value + deltaH1;

        				interpolated_value[counter] = new_value;
        				counter++;
        				Log.i("Test", ""+new_value);

        				if (counter == 20) {
        					double temp = 0;
        					System.arraycopy(interpolated_value, 0, tempBuffer, 0, 20 );
        					Arrays.sort(tempBuffer, 0, 20);

        					for (int i = 5; i < 15; i++) {
        						temp = temp + interpolated_value[i];
        					}
        					init_value = temp/10;
        				}

        				if ( counter <= 100 ) {
        					Log.i("HeartBeatAlgorithm", "addSample," + ((counter-1)*50 + init_timestamp - start_timestamp) + "," + (new_value - init_value));
        				}

        				// detrending
        				if ( counter == 100 ) {
        					hralgirthm.begin_timestamp = 4950 + init_timestamp - start_timestamp;
        					System.arraycopy(interpolated_value, 0, tempBuffer, 0, 100 );

        					SimpleMatrix tempMatrix = new SimpleMatrix(100, 1, true, tempBuffer);
        					SimpleMatrix newMatrix = Temp2.mult(tempMatrix);

        					for (int i = 0; i < newMatrix.numRows(); i++ ) {
        			        	hralgirthm.addSample(i*50 + init_timestamp - start_timestamp, newMatrix.get(i, 0));
//       			        	DataStorage.AddSample(i*50 + init_timestamp - start_timestamp, newMatrix.get(i, 0));

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
//        		        	DataStorage.AddSample((counter-1)*50 + init_timestamp - start_timestamp, newMatrix.get(99, 0));

        		            if(observer != null)
        		            {
        		                observer.onSample((counter-1)*50 + init_timestamp - start_timestamp, newMatrix.get(99, 0));
        		            }
        				}

        				interpolated_timestamp = interpolated_timestamp + 50;
        			}

        			prev_timestamp = timestamp;
        			prev_value = light;*/

                    Log.i("HeartBeatAlgorithm", "addSample," + (timestamp - start_timestamp) + "," + 0);
                }
            } else {
                Log.i("HeartBeatAlgorithm", "addSample," + (timestamp - start_timestamp) + "," + 0);
            }
        }
    }

    public boolean detectFullCovering (byte[] data, int width, int height) {
        int[] pixels = new int[width * height];

        // get the luma component from the image array in the form of YUV420sp
        decodeYUV420SPtoLuma(data, pixels, width, height);

        int mean = 0;
        double standard_dev = 0;

        for(int i = 0; i < pixels.length ; i++)
        {
            mean = mean + pixels[i];
        }

        // calculate the global mean of all the pixels in the image frame
        mean = mean/pixels.length;

        // calculate standard deviation
        int diff = 0;
        int diff_sum = 0;

        for(int i = 0; i < pixels.length; i++)
        {
            diff = pixels[i] - mean;
            diff_sum = diff_sum + diff*diff;
        }
        standard_dev = Math.sqrt(diff_sum/pixels.length);

        int mean_cap_upper = 100;
        int std_cap_upper = 30;

        int mean_cap_lower = 10;
        int std_cap_lower = 5;

        if( mean < mean_cap_upper && standard_dev < std_cap_upper){

            if ( mean < mean_cap_lower && standard_dev < std_cap_lower  ) {
                return false;
            } else {
                return true;
            }
        }

        return false;
    }

    public static void decodeYUV420SPtoLuma(byte[] yuv420sp, int[] luma, int width, int height) {
        if (yuv420sp == null) throw new NullPointerException();

        for (int j = 0, yp = 0; j < height; j++) {
            for (int i = 0; i < width; i++, yp++) {
                int y = (0xff & ((int) yuv420sp[yp])) - 16;
                if (y < 0) y = 0;
                luma[yp] = y;
            }
        }
        return;
    }

    void allocatePreviewBuffers()
    {
        android.hardware.Camera.Size size = c.getParameters().getPreviewSize();

        int frame_size = size.height * size.width * 2;
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
                c.addCallbackBuffer(new byte[frame_size]);
            }
            catch(Exception exception) { }
        }
    }

    public double analyze(byte frame[], int sample_span)
    {
        int ychannel_size = (frame.length * 2) / 3;

        double sum = 0;

        for ( int pos = 0; pos < ychannel_size; pos +=  sample_span)
        {
            sum += frame[pos] & 0xff;
        }

        return sum;
    }

    public int getFramerate()
    {
        return frameRate;
    }

    android.view.SurfaceHolder.Callback getPreviewSurfaceCallback()
    {
        return new android.view.SurfaceHolder.Callback()
        {

            public void surfaceChanged(SurfaceHolder holder, int format,
                                       int width, int height) {
                // TODO Auto-generated method stub

            }

            public void surfaceCreated(SurfaceHolder holder) {
                // TODO Auto-generated method stub
                previewSurfaceValid = true;

                if(startRequest)
                {
                    start();
                }

                startRequest = false;
            }

            public void surfaceDestroyed(SurfaceHolder holder) {
                // TODO Auto-generated method stub
                previewSurfaceValid = false;
                stopCamera();
            }
        };
    }

    public boolean isRunning()
    {
        if(c != null && running)
        {
            return true;
        }

        return false;
    }

    public void loadSettings(SharedPreferences sharedpreferences)
    {
    }

    public void onHRUpdate(int heartrate, int duration )
    {
        if(observer != null)
        {
            observer.onBeat(heartrate, duration / 1000);
        }
    }

    public void onPreviewFrame(byte frame[], Camera camera)
    {
        lastTime = System.nanoTime();

        if(running)
        {
            addSample(frame, lastTime / 1000000);
        }

        returnBuffer(frame);
    }

    public void onPreviewFrameOptimized(byte frame[], Camera camera)
    {
        cntSamplesThisSession = cntSamplesThisSession + 1;
        lastTime = System.nanoTime();
        double last = (double)lastTime / 1000000;

        if(useFrameRateTiming)
        {
            if(cntSamplesThisSession > frameRate * 8)
            {
                lastFrameRateBoundTimestamp = lastFrameRateBoundTimestamp + 1000 / frameRate;

                if( Math.abs(lastFrameRateBoundTimestamp - last) >6000D / frameRate)
                {
                    useFrameRateTiming = false;
                } else
                {
                    last = lastFrameRateBoundTimestamp;
                }
            }
        }
        if(running)
        {
            addSample(frame, (long)last);
        }
        returnBuffer(frame);
    }

    public void onValidRR(long timestamp, int value)
    {
        if(observer != null)
        {
            observer.onValidRR(timestamp, value);
        }
    }

    public void onValidatedRR(long timestamp, int value)
    {
        if(observer != null)
        {
            observer.onValidatedRR(timestamp, value);
        }
    }

    void returnBuffer(byte buffer[])
    {
        try
        {
            c.addCallbackBuffer(buffer);
        }
        catch(Exception exception) { }
    }

    public void saveSettings(SharedPreferences sharedpreferences)
    {
    }

    public void setBPMObserver(BeatObserver beatobserver)
    {
        observer = beatobserver;
    }

    public boolean start()
    {
        try
        {
            resetSampleAquisitor();
            hralgirthm.reset();
            startRequest = true;

            if ( startCamera() )
            {
                setFlash(true);
                c.startPreview();
                running = true;

                if(observer != null)
                {
                    observer.onHBStart();
                }
            } else
            {
                if(observer != null)
                {
                    Log.e("PreviewSurface invalid!", null);
                }
            }


        }
        catch(Exception exception)
        {
            exception.printStackTrace();

            if(observer != null)
            {
                BeatObserver beatlistener = observer;
                android.hardware.Camera.Parameters parameters;
                if(c != null)
                {
                    parameters = c.getParameters();
                } else
                {
                    parameters = null;
                }
                beatlistener.onCameraError(exception, parameters);
            }
        }

        return true;
    }

    boolean startCamera()
    {
        boolean flag;
        if(!previewSurfaceValid)
        {
            Log.e("ERROR", "Preview surface invalid");
            flag = false;
        } else
        {
            Log.e("SUCCESS", "Preview surface valid");
            flag = true;
        }

        c = getDefault();
        Camera.Parameters parameters = c.getParameters();

        try
        {
            c.setPreviewDisplay(previewSurfaceHolder);

            pw =  176;// 260;
            ph = 144; //220;

            Log.i("INFO", "Setting preview size: " + pw  + " | " + ph);

            skipSamples = (pw * ph) / 1000;
            parameters.setPreviewSize(pw, ph);
            c.setParameters(parameters);

            if(useCameraLed)
            {
                parameters.set("flash-mode", "torch");
                c.setParameters(parameters);
            }

            parameters.set("focus-mode", "infinity");
            c.setParameters(parameters);

            parameters.setExposureCompensation(0);
            parameters.setWhiteBalance(Parameters.WHITE_BALANCE_FLUORESCENT);

            c.setParameters(parameters);

            frameRate = parameters.getPreviewFrameRate();

            c.setParameters(parameters);
            setPreviewCallback();
            allocatePreviewBuffers();
            c.startPreview();

        } catch (IOException e)
        {
            c.release();
            c = null;
            e.printStackTrace();

            return false;
        }
        cameraOpen = true;
        running = true;
        flag = true;

        return flag;
    }

    public void reset() {
        counter = 0;
        interpolated_value = new double[50000];
        prev_timestamp = 0;
        interpolated_timestamp = 0;
        prev_value = 0;
        init_timestamp = 0;
    }

    public void stop()
    {
        startRequest = false;
        setFlash(false);
        running = false;
        stopCamera();

        if(observer != null)
        {
            observer.onHBStop();
        }
    }

    public void stopCamera()
    {
        running = false;
        if(c != null)
        {
            c.setOneShotPreviewCallback(null);
            c.stopPreview();
            c.release();
            c = null;
        }
        cameraOpen = false;
        c = null;
    }

    private static final String TAG = "HeartBeat";

    Camera c;
    boolean cameraOpen;
    int cntSamplesThisSession;
    Context context;
    Flashlight flashLight;
    int frameRate;
    HeartBeatAlgorithm hralgirthm;
    double lastFrameRateBoundTimestamp;
    long lastTime;
    int lightIntensity;
    BeatObserver observer;
    SurfaceView previewSurface;
    private SurfaceHolder previewSurfaceHolder;
    boolean previewSurfaceValid;
    boolean running;
    private int skipSamples;
    boolean startRequest;
    boolean useCameraLed;
    boolean useFrameRateTiming;
    int pw;
    int ph;

    boolean isDetecting;
    int positiveFrameCounter;
    int negativeFrameCounter;

    int lambda;
    int counter;
    SimpleMatrix I;
    SimpleMatrix D2;
    SimpleMatrix Temp1;
    SimpleMatrix Temp2;
    double[] interpolated_value;
    long prev_timestamp;
    long interpolated_timestamp;
    double prev_value;
    double[] tempBuffer;

    boolean started;
    long start_timestamp;
    long init_timestamp;
    double init_value;
    boolean firstTime_flag;
}
