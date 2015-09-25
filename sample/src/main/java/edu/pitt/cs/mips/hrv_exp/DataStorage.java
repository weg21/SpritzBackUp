package edu.pitt.cs.mips.hrv_exp;

import android.content.Context;

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;

import android.util.Log;

class DataSample
{
    public long timestamp;
    public int value;

    public DataSample(long l, int i)
    {
        timestamp = l;
        value = i;
    }

    public static String toCSV(ArrayList<DataSample> arraylist)
    {
        StringBuilder stringbuilder = new StringBuilder();

        for (Iterator<DataSample> iterator = arraylist.iterator();iterator.hasNext();  )
        {
            DataSample sample = iterator.next();
            
            stringbuilder.append( "" + sample.timestamp + "," + sample.value + "\r\n");
        }
        
        return stringbuilder.toString();
    }
    
    public long getTimeStamp(){
    	return timestamp;
    }
    
    public int getValue(){
    	return value;
    }
    
    public boolean equals(Object o){
    	DataSample newData = (DataSample)o;
    	
    	if(this.timestamp == newData.getTimeStamp() && this.value == newData.getValue()){
    		return true;
    	} else {
    		return false;
    	}
    }
}

public class DataStorage
{

    private DataStorage(Context c)
    {
        context = c;
        samples =  new ArrayList<DataSample>(10000);
        Bpms = new ArrayList<DataSample>(10000);
        Us = new ArrayList<DataSample>(10000);
        Vs = new ArrayList<DataSample>(10000);
        Rs = new ArrayList<DataSample>(10000);
        Gs = new ArrayList<DataSample>(10000);
        Bs = new ArrayList<DataSample>(10000);
    }

    public static boolean AddSample(long l, int i)
    {
        if(instance != null)
        {
            instance.add(l, i);
            
            return true;
        } 
        
        return false;
    }
    
    public static boolean AddBpm(long l, int i){
    	if(instance != null)
        {
            instance.add2(l, i);
            
            return true;
        } 
        
        return false;
    }
    
    public static boolean AddUSample(long l, int i){
    	if(instance != null)
        {
            instance.add3(l, i);
            
            return true;
        } 
        
        return false;
    }
    
    public static boolean AddVSample(long l, int i){
    	if(instance != null)
        {
            instance.add4(l, i);
            
            return true;
        } 
        
        return false;
    }
    
    public static boolean AddRSample(long l, int i){
    	if(instance != null)
        {
            instance.add5(l, i);
            
            return true;
        } 
        
        return false;
    }
    
    public static boolean AddGSample(long l, int i){
    	if(instance != null)
        {
            instance.add6(l, i);
            
            return true;
        } 
        
        return false;
    }
    
    public static boolean AddBSample(long l, int i){
    	if(instance != null)
        {
            instance.add7(l, i);
            
            return true;
        } 
        
        return false;
    }

    public static DataStorage getInstance(Context c)
    {
        if(instance == null)
        {
            instance = new DataStorage(c);
        }
        
        return instance;
    }

    public void add(long l, int i)
    {
        if(samples != null)
        {
            DataSample sample = new DataSample(l, i);
            samples.add(sample);
        }
    }
    
    public void add2(long l, int i)
    {
    	DataSample sample = new DataSample(l, i);
    	
        if(Bpms != null)
        {
            Bpms.add(sample);
        }
        
    }
    
    public void add3(long l, int i)
    {
    	DataSample sample = new DataSample(l, i);
    	
        if(Us != null)
        {
            Us.add(sample);
        }
    }
    
    public void add4(long l, int i)
    {
        if(Vs != null)
        {
            DataSample sample = new DataSample(l, i);
            Vs.add(sample);
        }
    }
    
    public void add5(long l, int i)
    {
        if(Rs != null)
        {
            DataSample sample = new DataSample(l, i);
            Rs.add(sample);
        }
    }
    
    public void add6(long l, int i)
    {
        if(Gs != null)
        {
            DataSample sample = new DataSample(l, i);
            Gs.add(sample);
        }
    }
    
    public void add7(long l, int i)
    {
        if(Bs != null)
        {
            DataSample sample = new DataSample(l, i);
            Bs.add(sample);
        }
    }
    
    public void clearData()
    {
    	if(samples != null){
    		samples.clear();
    	}
    	
    	if(Bpms != null){
    		Bpms.clear();
    	}
    	
    	if(Us != null){
    		Us.clear();
    	}
    	
    	if(Vs != null){
    		Vs.clear();
    	}
    	
    	if(Rs != null){
    		Rs.clear();
    	}
    	
    	if(Gs != null){
    		Gs.clear();
    	}
    	
    	if(Bs != null){
    		Bs.clear();
    	}
    }

    public String save()
    {
        return save( null);
    }


    public String save(String surfix)
    {
        if(samples == null || samples.size() == 0) 
        {	
        	return "";
        }

        Log.i("DataStorage", "end");
        if(surfix == null) 
        {
        	surfix = "";
        }
        
        if(!surfix.startsWith("_"))
        {
        	surfix = "_" + surfix;
        }
        
        File dir = new File(context.getExternalFilesDir(null) + "/pitthrv/" );
        
        String time1 = String.valueOf(System.nanoTime()/1000000);
        String time2 = String.valueOf(System.currentTimeMillis());
        String filename = time1 + "_" + time2 + surfix +  "_samples.csv";
        String filename2 = time1 + "_" + time2 + surfix + "_Bpms.csv";
        String filename3 = time1 + "_" + time2 + surfix + "_Us.csv";
        String filename4 = time1 + "_" + time2 + surfix + "_Vs.csv";
        String filename5 = time1 + "_" + time2 + surfix + "_Rs.csv";
        String filename6 = time1 + "_" + time2 + surfix + "_Gs.csv";
        String filename7 = time1 + "_" + time2 + surfix + "_Bs.csv";
        
        File file = new File(dir, filename);
        
        if(!dir.exists())
            dir.mkdir();

        try {
	        OutputStreamWriter outputstreamwriter = new OutputStreamWriter( new FileOutputStream(file, true));
	
	        outputstreamwriter.write( DataSample.toCSV(samples) );
	        outputstreamwriter.close();
        
        } catch (IOException e)
        {
            e.printStackTrace();
            Log.i("DataStorage", e.toString());
        }
        
        if(Bpms != null && Bpms.size() != 0)
        {
            File file2 = new File(dir, filename2);
            
        	try {
        		OutputStreamWriter outputstreamwriter = new OutputStreamWriter( new FileOutputStream(file2, true));
        		
        		outputstreamwriter.write( DataSample.toCSV(Bpms) );
        		outputstreamwriter.close();
        	} 
        	catch (IOException e)
        	{
        		e.printStackTrace();
        		Log.i("DataStorage", e.toString());
        	}
        }
        
        if(Us != null && Us.size() != 0)
        {
        	File file3 = new File(dir, filename3);
        
        	try {
        		OutputStreamWriter outputstreamwriter = new OutputStreamWriter( new FileOutputStream(file3, true));
	
        		outputstreamwriter.write( DataSample.toCSV(Us) );
        		outputstreamwriter.close(); 
        	} 
        	catch (IOException e)
        	{
        		e.printStackTrace();
        		Log.i("DataStorage", e.toString());
        	}
        }
        
        if(Vs != null && Vs.size() != 0)
        {
        	File file4 = new File(dir, filename4);
        
        	try {
        		OutputStreamWriter outputstreamwriter = new OutputStreamWriter( new FileOutputStream(file4, true));
        		
        		outputstreamwriter.write( DataSample.toCSV(Vs) );
        		outputstreamwriter.close();
        	} 
        	catch (IOException e)
        	{
        		e.printStackTrace();
        		Log.i("DataStorage", e.toString());
        	}
        }
        
        if(Rs != null && Rs.size() != 0)
        {
        	File file5 = new File(dir, filename5);
        
        	try {
        		OutputStreamWriter outputstreamwriter = new OutputStreamWriter( new FileOutputStream(file5, true));
        		
        		outputstreamwriter.write( DataSample.toCSV(Rs) );
        		outputstreamwriter.close();
        	} 
        	catch (IOException e)
        	{
        		e.printStackTrace();
        		Log.i("DataStorage", e.toString());
        	}
        }
        
        if(Gs != null && Gs.size() != 0)
        {
        	File file6 = new File(dir, filename6);
        
        	try {
        		OutputStreamWriter outputstreamwriter = new OutputStreamWriter( new FileOutputStream(file6, true));
        		
        		outputstreamwriter.write( DataSample.toCSV(Gs) );
        		outputstreamwriter.close();
        	} 
        	catch (IOException e)
        	{
        		e.printStackTrace();
        		Log.i("DataStorage", e.toString());
        	}
        }
        
        if(Bs != null && Bs.size() != 0)
        {
        	File file7 = new File(dir, filename7);
        
        	try {
        		OutputStreamWriter outputstreamwriter = new OutputStreamWriter( new FileOutputStream(file7, true));
        		
        		outputstreamwriter.write( DataSample.toCSV(Bs) );
        		outputstreamwriter.close();
        	} 
        	catch (IOException e)
        	{
        		e.printStackTrace();
        		Log.i("DataStorage", e.toString());
        	}
        }
        
        return surfix;
    }

    private static Context context;
    private static DataStorage instance;
    private ArrayList<DataSample> samples;
    private ArrayList<DataSample> Bpms;
    private ArrayList<DataSample> Us;
    private ArrayList<DataSample> Vs;
    private ArrayList<DataSample> Rs;
    private ArrayList<DataSample> Gs;
    private ArrayList<DataSample> Bs;
}
