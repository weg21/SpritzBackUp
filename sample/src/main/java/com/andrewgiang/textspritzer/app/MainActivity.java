package com.andrewgiang.textspritzer.app;

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

public class MainActivity extends ActionBarActivity  implements BeatObserver {

    public static final String TAG = MainActivity.class.getName();
    private SpritzerTextView mSpritzerTextView;
    private SeekBar mSeekBarTextSize;
    private SeekBar mSeekBarWpm;
    private ProgressBar mProgressBar;
    private SurfaceView mPreview;
    HeartBeat heartrate;
    Button start;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        start = (Button) findViewById(R.id.camBtn);
        mPreview = (SurfaceView) findViewById(R.id.preview);

        mPreview.setMinimumWidth(176);
        mPreview.setMinimumHeight(144);
        heartrate = new HeartBeat(mPreview, this);
        heartrate.setBPMObserver(this);

       // heartrate.stop();
       // heartrate.start();

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
         //       heartrate.stop();

            }

            @Override
            public void onPlay() {
                Toast.makeText(MainActivity.this, "Spritzer is playing", Toast.LENGTH_SHORT).show();
           //     heartrate.start();
            }
        });

        mSpritzerTextView.setOnCompletionListener(new Spritzer.OnCompletionListener() {
            @Override
            public void onComplete() {
                Toast.makeText(MainActivity.this, "Spritzer is finished", Toast.LENGTH_SHORT).show();

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

    public void OnClickStart(View view) {//??????????????????????
        Button btn = (Button)view;
        heartrate.stop();
        heartrate.start();
    }
    public void onBeat(int heartrate, int duration) {
        // TODO Auto-generated method stub
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
    public void onSample(long timestamp, float value){

    }
    public void onValidRR(long timestamp, int value){

    }
    public void onValidatedRR(long timestamp, int value){

    }
    public void onCovered(){
        mSpritzerTextView.play();
        Toast.makeText(MainActivity.this, "Spritzer is playing", Toast.LENGTH_SHORT).show();
    }
    public void onUncovered(){

        mSpritzerTextView.pause();
        Toast.makeText(MainActivity.this, "Spritzer has been paused", Toast.LENGTH_SHORT).show();
    }


}
