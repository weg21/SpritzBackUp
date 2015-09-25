package edu.pitt.cs.mips.hrv_exp;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.media.MediaPlayer;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStreamWriter;

//import edu.pitt.cs.mips.app.mooc.util.DataStorage;


public class HeartBeat
        implements Camera.PreviewCallback, PulseObserver {
	
    public HeartBeat(SurfaceView surfaceview, Context context1) {
        observer = null;
        hralgirthm = new HeartBeatAlgorithm();

        lightIntensity = 0;
        redIndex = 0;
        skipSamples = 25;    // pick a pixel every skipSamples pixels
        running = false;
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
        hralgirthm.setPulseObserver((PulseObserver) this);
        
        isDetecting = false;
        positiveFrameCounter = 0;
        negativeFrameCounter = 0;
        
        player = null;
    }
    
    public HeartBeat(SurfaceView surfaceview, MediaPlayer player, Context context1)  {
        observer = null;
        hralgirthm = new HeartBeatAlgorithm();

        lightIntensity = 0;
        redIndex = 0;
        skipSamples = 25;    // pick a pixel every skipSamples pixels
        running = false;
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
        hralgirthm.setPulseObserver((PulseObserver) this);
        
        isDetecting = false;
        positiveFrameCounter = 0;
        negativeFrameCounter = 0;
        
        this.player = player;
    }

    private Camera getDefault() {
        return Camera.open();
    }

    private void preinit() {

    }

    private void resetSampleAquisitor() {
        lastFrameRateBoundTimestamp = 0;
        useFrameRateTiming = true;
        cntSamplesThisSession = 0;
    }

    public void setFlash(boolean flag) {
        try {
            Parameters parameters = c.getParameters();
            if (flag){
                parameters.setFlashMode(Parameters.FLASH_MODE_TORCH);
                flash = true;
            }

            else{
                parameters.setFlashMode(Parameters.FLASH_MODE_OFF);
                flash = false;
            }

            c.setParameters(parameters);
//          flashLight.setFlashlightEnabled(flag);
        } catch (Exception exception) {
            c.release();
            c = null;
        }

    }

    private void setPreviewCallback() {
        c.setPreviewCallback(this);
    }

    public void addSample(byte buffer[], long timestamp) throws IOException {
    	
        boolean covered = detectFullCovering(buffer, c.getParameters().getPreviewSize().width, c.getParameters().getPreviewSize().height);
        
        if (covered) {
        	positiveFrameCounter++;
        	negativeFrameCounter = 0;
        	if (!isDetecting) {
        		if (positiveFrameCounter >= 5) {
        			isDetecting = true;
        			observer.onCovered();
        			
        			int light = -analyze(buffer, skipSamples);

                    if (observer != null) {
                        observer.onSample(timestamp, light);
                    }

//                    if ( video_timestamp < 0) {
//                    	hralgirthm.addSample(timestamp, light);
//                    } else {
//                    	hralgirthm.addSample(timestamp, light, video_timestamp);
//                    }
                    hralgirthm.addSample(timestamp, light);
                   // DataStorage.AddCoverMoment(timestamp, light);
        		}
        	} else {
        		int light = -analyze(buffer, skipSamples);

                if (observer != null) {
                    observer.onSample(timestamp, light);
                }

//                if ( video_timestamp < 0) {
//                	hralgirthm.addSample(timestamp, light);
//                } else {
//                	hralgirthm.addSample(timestamp, light, video_timestamp);
//                }
                hralgirthm.addSample(timestamp, light);
        	}
        } else {
        	negativeFrameCounter++;
        	positiveFrameCounter = 0;
        	if (isDetecting) {
        		if (negativeFrameCounter >= 5) {
        			isDetecting = false;
        			observer.onUncovered();
        			//DataStorage.AddUncoverMoment(timestamp, 0);
        		} else {
        			int light = -analyze(buffer, skipSamples);

                    if (observer != null) {
                        observer.onSample(timestamp, light);
                    }
                    
//                    if ( video_timestamp < 0) {
//                    	hralgirthm.addSample(timestamp, light);
//                    } else {
//                    	hralgirthm.addSample(timestamp, light, video_timestamp);
//                    }

                    hralgirthm.addSample(timestamp, light);
        		}
        	}
        }
    }
    
    public boolean detectFullCovering (byte[] data, int width, int height) throws IOException {
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

        // linear decision boundaries for detecting full-covering gestures
//        Log.i("Mean:" + mean, "Standard Deviation:"+ standard_dev);
/*      outputstreamwriter.append("" + mean);
        outputstreamwriter.append(",");
        outputstreamwriter.append("" + standard_dev);
        outputstreamwriter.append("\r\n");*/
        
/*      int mean_cap;
        int std_cap;
        
        if (flash){
        	mean_cap = 100;
        	std_cap = 30;
        }
        else{
           mean_cap = 100;
           std_cap = 30;
        }
        
        if( mean < mean_cap && standard_dev < std_cap){
        	
            if(mean <2 && standard_dev<1.1  && !flash){

                observer.onPoorQualityData();
            }else{
                this.observer.onQualityData(mean, standard_dev);
            }
            return true;
        }*/
        
        int mean_cap_upper = 100;
        int std_cap_upper = 30;
        
        int mean_cap_lower = 10;
        int std_cap_lower = 5;
        
/*        if( mean < mean_cap_upper && standard_dev < std_cap_upper){
        	
        	if ( mean < mean_cap_lower && standard_dev < std_cap_lower  ) {
        		return false;
        	} else {
        		return true;
        	}
        }*/
        
        if( mean <= 100 && mean >= 50 && standard_dev <= 20){
        	return true;
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
    
    void allocatePreviewBuffers() {
        Camera.Size size = c.getParameters().getPreviewSize();

        int frame_size = size.height * size.width * 2;

        int buffer_size = 4000000; // approximately 4MB
        int max_frame = 8;
        int frames = 0;

        if (frame_size != 0) {
            frames = Math.max(Math.min(buffer_size / frame_size, max_frame), 1);
        }

        for (int i = 0; i < frames; i++) {
            try {
                c.addCallbackBuffer(new byte[frame_size]);
            } catch (Exception exception) {
            }
        }
    }

    public int analyze(byte frame[], int sample_span) {
        int ychannel_size = (frame.length * 2) / 3;

        int sum = 0;

        for (int pos = 0; pos < ychannel_size; pos += sample_span) {
            sum += frame[pos] & 0xff;
        }

        return sum;
    }

    public int getFramerate() {
        return frameRate;
    }

    SurfaceHolder.Callback getPreviewSurfaceCallback() {
        return new SurfaceHolder.Callback() {

            public void surfaceChanged(SurfaceHolder holder, int format,
                                       int width, int height) {
                // TODO Auto-generated method stub

            }

            public void surfaceCreated(SurfaceHolder holder) {
                // TODO Auto-generated method stub
                previewSurfaceValid = true;

                if (startRequest) {
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

    public boolean isRunning() {
        if (c != null && running) {
            return true;
        }

        return false;
    }

    public void loadSettings(SharedPreferences sharedpreferences) {
    }

    public void onHRUpdate(int heartrate, int duration) {
        if (observer != null) {
            observer.onBeat(heartrate, duration / 1000);
        }
    }

    public void onPreviewFrame(byte frame[], Camera camera) {
        lastTime = System.nanoTime();

        if (running) {
            try {
//            	if (player != null) {
//            		addSample(frame, lastTime / 1000000, player.getCurrentPosition());
//            	} else {
//            		addSample(frame, lastTime / 1000000, -1);
//            	}
                addSample(frame, lastTime / 1000000);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
        returnBuffer(frame);
    }

    public void onPreviewFrameOptimized(byte frame[], Camera camera) throws IOException {
        cntSamplesThisSession = cntSamplesThisSession + 1;
        lastTime = System.nanoTime();
        double last = (double) lastTime / 1000000;

        if (useFrameRateTiming) {
            if (cntSamplesThisSession > frameRate * 8) {
                lastFrameRateBoundTimestamp = lastFrameRateBoundTimestamp + 1000 / frameRate;

                if (Math.abs(lastFrameRateBoundTimestamp - last) > 6000D / frameRate) {
                    useFrameRateTiming = false;
                } else {
                    last = lastFrameRateBoundTimestamp;
                }
            }
        }
        if (running) {
//        	if (player != null) {
//        		addSample(frame, (long) last, player.getCurrentPosition());
//        	} else {
//        		addSample(frame, (long) last, -1 );
//        	}
            addSample(frame, (long) last);
        }
        
        boolean covered = detectFullCovering(frame, previewSurface.getWidth(), previewSurface.getHeight());
        if(covered){
            observer.onCovered();
        }else{
            observer.onUncovered();
        }
        returnBuffer(frame);
    }

    public void onValidRR(long timestamp, int value) {
        if (observer != null) {
            observer.onValidRR(timestamp, value);
        }
    }

    public void onValidatedRR(long timestamp, int value) {
        if (observer != null) {
            observer.onValidatedRR(timestamp, value);
        }
    }

    void returnBuffer(byte buffer[]) {
        try {
            c.addCallbackBuffer(buffer);
        } catch (Exception exception) {
        }
    }

    public void saveSettings(SharedPreferences sharedpreferences) {
    }

    public void setBPMObserver(BeatObserver beatobserver) {
        observer = beatobserver;
    }

    public boolean start() {
        try {
            resetSampleAquisitor();
            hralgirthm.reset();
            startRequest = true;

            if (startCamera()) {
                c.startPreview();
                Parameters parameters = c.getParameters();
                parameters.setExposureCompensation(-3);
                c.setParameters(parameters);
                running = true;

                if (observer != null) {
                    observer.onHBStart();
                }
            } else {
                if (observer != null) {
                    Log.e("PreviewSurface invalid!", "");
                }
            }


        } catch (Exception exception) {
            exception.printStackTrace();

            if (observer != null) {
                BeatObserver beatlistener = observer;
                Parameters parameters;
                if (c != null) {
                    parameters = c.getParameters();
                } else {
                    parameters = null;
                }
                beatlistener.onCameraError(exception, parameters);
            }
        }

        return true;
    }

    boolean startCamera() {
        boolean flag;
        if (!previewSurfaceValid) {
            Log.e("ERROR", "Preview surface invalid");
            flag = false;
        } else {
            Log.e("SUCCESS", "Preview surface valid");
            flag = true;
        }

        c = getDefault();
        Parameters parameters = c.getParameters();

        try {
            c.setPreviewDisplay(previewSurfaceHolder);

            int pw = 176;// 260;
            int ph = 144; //220;

            Log.i("INFO", "Setting preview size: " + pw + " | " + ph);

            skipSamples = (pw * ph) / 1000;
            parameters.setPreviewSize(pw, ph);
            c.setParameters(parameters);

            if (useCameraLed) {
//              parameters.set("flash-mode", "torch");
//                parameters.set("flash-mode", "off");
                c.setParameters(parameters);
            }
            parameters.set("focus-mode", "infinity");
            c.setParameters(parameters);
            c.setParameters(parameters);
            frameRate = parameters.getPreviewFrameRate();
            
            c.setParameters(parameters);
            setPreviewCallback();
            allocatePreviewBuffers();
            c.startPreview();

        } catch (IOException e) {
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

    public void stop() {
        startRequest = false;


        running = false;
        stopCamera();

        if (observer != null) {
            observer.onHBStop();
        }
        
/*      try {
			outputstreamwriter.flush();
	        outputstreamwriter.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
    }

    public void stopCamera() {
        running = false;
        if (c != null) {
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

    int frameRate;
    HeartBeatAlgorithm hralgirthm;
    double lastFrameRateBoundTimestamp;
    long lastTime;
    int lightIntensity;
    BeatObserver observer;
    SurfaceView previewSurface;
    private SurfaceHolder previewSurfaceHolder;
    boolean previewSurfaceValid;
    int redIndex;
    boolean running;
    private int skipSamples;
    boolean startRequest;
    boolean useCameraLed;
    boolean useFrameRateTiming;
    boolean flash;
    OutputStreamWriter outputstreamwriter;
    boolean isDetecting;
    int positiveFrameCounter;
    int negativeFrameCounter;
    
    MediaPlayer player;
}
