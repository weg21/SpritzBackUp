package com.qualcomm.snapdragon.sdk.sample;


import android.content.Context;
import android.graphics.PointF;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Iterator;
/**
 * Created by Wei on 10/12/2015.
 */
class FaceData {
    public long timestamp;
    public int numFaces;
    public int smileValue;
    public int leftEyeBlink;
    public int rightEyeBlink;
    public int faceRollValue;
    public int faceYawValue;
    public int facePitchValue;
    public PointF gazePointValue;
    public int horizontalGazeAngle;
    public int verticalGazeAngle;



    public FaceData(int numFaces0, int smileValue0, int leftEyeBlink0, int rightEyeBlink0, int faceRollValue0,
                        int faceYawValue0, int facePitchValue0, PointF gazePointValue0, int horizontalGazeAngle0, int verticalGazeAngle0, long timestamp0) {
        timestamp = timestamp0;
        numFaces = numFaces0;
        smileValue = smileValue0;
        leftEyeBlink = leftEyeBlink0;
        rightEyeBlink = rightEyeBlink0;
        faceRollValue = faceRollValue0;
        faceYawValue = faceYawValue0;
        facePitchValue = facePitchValue0;
        gazePointValue = gazePointValue0;
        horizontalGazeAngle = horizontalGazeAngle0;
        verticalGazeAngle = verticalGazeAngle0;
    }

    public static String toCSV(ArrayList<FaceData> arraylist) {
        StringBuilder stringbuilder = new StringBuilder();

        for (Iterator<FaceData> iterator = arraylist.iterator(); iterator.hasNext(); ) {
            FaceData sample = iterator.next();

            stringbuilder.append("" + sample.timestamp + "," + sample.numFaces + ","
                    + sample.smileValue + "," + sample.leftEyeBlink + "," + sample.rightEyeBlink + "," + sample.faceRollValue + "," + sample.faceYawValue
                    + "," + sample.facePitchValue + "," + sample.gazePointValue + "," + sample.horizontalGazeAngle + "," + sample.verticalGazeAngle + "\r\n");
        }

        return stringbuilder.toString();
    }

    public long getTimeStamp() {
        return timestamp;
    }

    public String getValue() {
        StringBuilder stringbuilder = new StringBuilder();
        stringbuilder.append("" + timestamp + "," + numFaces + ","
                + smileValue + "," + leftEyeBlink + "," + rightEyeBlink + "," + faceRollValue + "," + faceYawValue
                + "," + facePitchValue + "," + gazePointValue + "," + horizontalGazeAngle + "," + verticalGazeAngle + "\r\n");

        return stringbuilder.toString();
    }

    public boolean equals(Object o) {
        FaceData newData = (FaceData) o;
        StringBuilder stringbuilder = new StringBuilder();
        stringbuilder.append("" + this.timestamp + "," + this.numFaces + ","
                + this.smileValue + "," + this.leftEyeBlink + "," + this.rightEyeBlink + "," + this.faceRollValue + "," + this.faceYawValue
                + "," + this.facePitchValue + "," + this.gazePointValue + "," + this.horizontalGazeAngle + "," + this.verticalGazeAngle + "\r\n");
        if (this.timestamp == newData.getTimeStamp() && stringbuilder.toString() == newData.getValue()) {
            return true;
        } else {
            return false;
        }
    }
}


public class saveFaceData {

    private saveFaceData(Context c) {
        context = c;
        samples = new ArrayList<FaceData>(100000);
    }

    public static boolean AddFaceDataSample(int numFaces0, int smileValue0, int leftEyeBlink0, int rightEyeBlink0, int faceRollValue0,
                                    int faceYawValue0, int facePitchValue0, PointF gazePointValue0, int horizontalGazeAngle0, int verticalGazeAngle0, long timestamp0) {
        if (instance != null) {
            instance.addFaceDataSamples(numFaces0, smileValue0, leftEyeBlink0, rightEyeBlink0, faceRollValue0, faceYawValue0, facePitchValue0, gazePointValue0, horizontalGazeAngle0, verticalGazeAngle0, timestamp0);
            return true;
        }

        return false;
    }




    public static saveFaceData getInstance(Context c) {
        if (instance == null) {
            instance = new saveFaceData(c);
        }

        return instance;
    }

    public boolean setUser (String s) {
        if ( instance != null) {
            userId = s;

            return true;
        }

        return false;
    }

    public void addFaceDataSamples(int numFaces0, int smileValue0, int leftEyeBlink0, int rightEyeBlink0, int faceRollValue0,
                    int faceYawValue0, int facePitchValue0, PointF gazePointValue0, int horizontalGazeAngle0, int verticalGazeAngle0, long timestamp0) {
        if (samples != null) {
            FaceData sample = new FaceData(numFaces0, smileValue0, leftEyeBlink0, rightEyeBlink0, faceRollValue0, faceYawValue0, facePitchValue0, gazePointValue0, horizontalGazeAngle0, verticalGazeAngle0, timestamp0);
            samples.add(sample);
        }
    }

    public void clearData() {
        if (samples != null) {
            samples.clear();
        }
    }
    //public String save( String course, String lecture, String topic, String time ) {
    public String save( String title, long time ) {

        if (samples == null  || samples.size() == 0) {
            return "samples null";
        }

        Log.i("saveFaceData", "end");

        String directory = context.getExternalFilesDir(null) + "/pitthrv/AttentiveLearnerData/" + userId + "/" + title + "/" + time + "/";
        //System.out.println("directory is: ~~~~~~~~~~~~"+directory);

        File dir = null;

        String filename = "faceData.csv";

        try {
            OutputStreamWriter outputstreamwriter;

            if ( samples != null && samples.size() != 0) {
                dir = new File(directory + "quiz/");

                if (!dir.exists())
                    dir.mkdirs();

                File file = new File(dir, filename);
                outputstreamwriter = new OutputStreamWriter(new FileOutputStream(file, true));

                outputstreamwriter.write(FaceData.toCSV(samples));
            }

        } catch (IOException e) {
            e.printStackTrace();
            Log.i("DataStorage", e.toString());
        }
        return "saved";
    }

    private static Context context;
    private static saveFaceData instance;

    private String userId;
    private ArrayList<FaceData> samples;

}
