package edu.pitt.cs.mips.hrv_exp;

import android.hardware.Camera;
import android.hardware.Camera.Parameters;

public class Flashlight
{

    public Flashlight()
    {
    }


    public static void enable(boolean flag)
    {
    	Camera camera = Camera.open();
    	
    	if (flag)
    	{
    		Parameters p = camera.getParameters();
    		p.setFlashMode(Parameters.FLASH_MODE_TORCH);
    		camera.setParameters(p);
    		camera.startPreview();    	
    	} else {
    		Parameters p = camera.getParameters();
    		p.setFlashMode(Parameters.FLASH_MODE_OFF);
    		camera.setParameters(p);
    		camera.stopPreview();
        }
    }

}
