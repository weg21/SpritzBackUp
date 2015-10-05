package edu.pitt.cs.mips.hrv_exp;

import android.util.Log;

import java.util.Arrays;

public class HeartBeatAlgorithm
{
	private static final String TAG = "HeartBeatAlgorithm";

	static final int MAX_PEAKS = 12500;
	static final int MAX_SAMPLES = 50000;
	static final int TEMP_SIZE = 100;

	static final int DIR_DOWN = -1;
	static final int DIR_UNDEF = 0;
	static final int DIR_UP = 1;

	int badBeatRR;
	int bpm_count;
	long[] bpmT;
	int[] bpmV;
	int calculatedHr;
	int calculatedHrQuality;
	public int currentFps;
	int ecg_direction;
	int errorBeatAllowedAfterNoBeats;
	int errorCounter;
	int firstGoodRRIdx;
	int fpsCounter;
	long fpsLastTimeStamp;
	int goodRRCnt;
	int goodRRTime;
	int goodUninteruptedRRCnt;
	public int heartRateLimit;
	PulseObserver pulseobserver;
	final int hrMax;
	final int hrMin;
	int lastBadBeatIdx;
	int lastProcessedCrossSample;
	int maxAveragingTime;
	int maxBadBeats;
	public int maxHeartRate = 190;
	long maxTime;
	int maxTimeVariability;
	public int minHeartRate = 30;
	int peak_count;
	long[] peaksT;
	double[] peaksV;
	int sample_count;
	long[] samplesT;
	double[] samplesV;
	int shanonFactor;
	final int[] tempBuffer;
	int thD;
	int totalMeasuringTime;
	int valid_peak_count;
	long[] validPeaksT;
	double[] validPeaksV;
	int valueDifferenceTriggerFactor;
	int zc_count;
	int zcDiscriminatorTime;
	int zcMaxShootDifference;
	int[] zcRR;
	double[] zcShot;
	long[] zcT;
	double[] zcV;

	boolean newSection;
	public long begin_timestamp;

	public HeartBeatAlgorithm()
	{
		heartRateLimit = maxHeartRate;
		currentFps = 30;
		thD = 4;
		maxTime = 1500;
		zcDiscriminatorTime = 1000;
		zcMaxShootDifference = 50;

		samplesT = new long[50000];
		samplesV = new double[50000];
		sample_count = 0;

		peaksT = new long[12500];
		peaksV = new double[12500];
		peak_count = 0;

		validPeaksT = new long[12500];
		validPeaksV = new double[12500];
		valid_peak_count = 0;

		zcT = new long[12500];
		zcV = new double[12500];
		zcShot =  new double[12500];
		zcRR = new int[12500];
		zc_count = 0;

		bpmT = new long[12500];
		bpmV = new int[12500];
		bpm_count = 0;
		lastProcessedCrossSample = 0;

		ecg_direction = DIR_UNDEF;
		fpsLastTimeStamp = 0L;
		fpsCounter = 0;
		shanonFactor = 6;
		maxTimeVariability = 200;
		maxAveragingTime = 15000;
		errorBeatAllowedAfterNoBeats = 5;
		maxBadBeats = 1;
		valueDifferenceTriggerFactor = 5;
		firstGoodRRIdx = -1;
		goodRRCnt = 0;
		errorCounter = 0;
		goodRRTime = 0;
		goodUninteruptedRRCnt = 0;
		badBeatRR = 0;
		totalMeasuringTime = 0;
		lastBadBeatIdx = 0;

		tempBuffer = new int[100];
		hrMin = 4;
		hrMax = 30;
		calculatedHr = 0;
		calculatedHrQuality = 0;

		newSection = false;
		begin_timestamp = 0;
	}

	private void addBpm(long timestamp, int heart_rate)
	{
		Log.i(TAG, "addBpm," + timestamp + "," + heart_rate);
//		DataStorage.AddBpm(timestamp, heart_rate);

		bpmT[bpm_count] = timestamp;
		bpmV[bpm_count] = heart_rate;

		bpm_count++;
	}

	public void addPeak(long timestamp, double pulse_value)
	{
		if (timestamp >= begin_timestamp) {
			Log.i(TAG, "addPeak," + timestamp + "," + pulse_value);
		}

		peaksT[peak_count] = timestamp;
		peaksV[peak_count] = pulse_value;

		validPeaksT[valid_peak_count] = timestamp;
		validPeaksV[valid_peak_count] = pulse_value;

		peak_count += 1;
		valid_peak_count += 1;

		if (valid_peak_count < 4)
		{

			return; // we need at least 4 peaks for the follow up calculations
		}

		double amp1 = validPeaksV[(valid_peak_count - 4)] - validPeaksV[(valid_peak_count - 3)];
		double amp2 = validPeaksV[(valid_peak_count - 3)] - validPeaksV[(valid_peak_count - 2)];
		double amp3 = validPeaksV[(valid_peak_count - 2)] - validPeaksV[(valid_peak_count - 1)];

		long t1 = validPeaksT[(valid_peak_count - 3)] - validPeaksT[(valid_peak_count - 4)];
		long t2 = validPeaksT[(valid_peak_count - 2)] - validPeaksT[(valid_peak_count - 3)];
		long t3 = validPeaksT[(valid_peak_count - 1)] - validPeaksT[(valid_peak_count - 2)];

		long last_span = t1 + t2;

		if (Math.abs(amp2) < Math.abs(amp1 / thD)) // amp2 is very very small, most likely it's a noise
		{
			if (last_span < maxTime)
			{
				if (Math.abs(amp3) > Math.abs(amp2))
				{
					if (validPeaksT[(valid_peak_count - 3)] >= begin_timestamp) {
						Log.i(TAG, "deletePeak," + validPeaksT[(valid_peak_count - 3)] + "," + validPeaksV[(valid_peak_count - 3)]);
					}

					validPeaksV[(valid_peak_count - 3)] = validPeaksV[(valid_peak_count - 1)];
					validPeaksT[(valid_peak_count - 3)] = validPeaksT[(valid_peak_count - 1)];

					if (validPeaksT[(valid_peak_count - 2)] >= begin_timestamp) {
						Log.i(TAG, "deletePeak," + validPeaksT[(valid_peak_count - 2)] + "," + validPeaksV[(valid_peak_count - 2)]);
					}
				} else {
					if (validPeaksT[(valid_peak_count - 1)] >= begin_timestamp) {
						Log.i(TAG, "deletePeak," + validPeaksT[(valid_peak_count - 1)] + "," + validPeaksV[(valid_peak_count - 1)]);
					}
					if (validPeaksT[(valid_peak_count - 2)] >= begin_timestamp) {
						Log.i(TAG, "deletePeak," + validPeaksT[(valid_peak_count - 2)] + "," + validPeaksV[(valid_peak_count - 2)]);
					}
				}

				valid_peak_count -= 2;

				return;
			}
		}
		if (amp1 >= 0)
		{
			return;
		}

		if (samplesT[lastProcessedCrossSample] >= validPeaksT[(valid_peak_count - 4)])
		{
			return;
		}

		double zc_amp = (validPeaksV[(valid_peak_count - 4)] + validPeaksV[(valid_peak_count - 3)]) / 2;
		long zc_timestamp = (validPeaksT[(valid_peak_count - 4)] + validPeaksT[(valid_peak_count - 3)]) / 2L;

		while ((lastProcessedCrossSample < sample_count) && (samplesT[lastProcessedCrossSample] < validPeaksT[(valid_peak_count - 4)]))
		{
			lastProcessedCrossSample += 1;
		}

		while ((lastProcessedCrossSample < sample_count) && (samplesV[lastProcessedCrossSample] < zc_amp))
		{
			lastProcessedCrossSample += 1;
		}

		double change = samplesV[lastProcessedCrossSample] - samplesV[(lastProcessedCrossSample - 1)];

		if (change == 0)
		{
			change = 1;
		}

		double ratio = (zc_amp - samplesV[(lastProcessedCrossSample - 1)]) * 1000 / change;

		long whole_step = samplesT[lastProcessedCrossSample] - samplesT[(lastProcessedCrossSample - 1)];

		// getting the timestamp information of the zero crossing point by interprotaion

		long time = samplesT[(lastProcessedCrossSample - 1)] + (long)ratio * whole_step / 1000L;

		addZeroCross(time, zc_amp, validPeaksV[(valid_peak_count - 3)] - validPeaksV[(valid_peak_count - 4)]);

		Log.i("addPeak", "Step 5");
	}

	public void addSample(long timestamp, double pulse_value)
	{
//		Log.i(TAG, "addSample," + timestamp + "," + pulse_value);
		samplesT[sample_count] = timestamp;
		samplesV[sample_count] = pulse_value;

		if (timestamp - fpsLastTimeStamp > 1000)
		{
			currentFps = fpsCounter;
			fpsCounter = 0;
			fpsLastTimeStamp = timestamp;

			heartRateLimit = Math.min(maxHeartRate, currentFps * 60 / shanonFactor);
		}

		fpsCounter += 1;
		sample_count += 1;

		if (sample_count < 2)
		{
			return;
		}

		switch ( ecg_direction )
		{
			case DIR_UP:
				if (samplesV[sample_count - 1] >= samplesV[sample_count - 2])
					return;

				addPeak(samplesT[sample_count - 2], samplesV[sample_count - 2]);

				ecg_direction = DIR_DOWN;
				break;
			case DIR_DOWN:
				if (samplesV[sample_count - 1] <= samplesV[sample_count - 2])
					return;

				addPeak(samplesT[sample_count - 2], samplesV[sample_count - 2]);

				ecg_direction = DIR_UP;
				break;
			default:
				if (samplesV[sample_count - 1] < samplesV[sample_count - 2])
				{
					ecg_direction = DIR_DOWN;
					break;
				}
				ecg_direction = DIR_UP;
		}
	}

	public void addZeroCross(long timestamp, double pulse_value, double shot_value)
	{
		int k = 1;
		if ( timestamp >= begin_timestamp ) {
			Log.i(TAG, "addZeroCross," + timestamp + "," + pulse_value + "," + shot_value);
		}

		zcT[zc_count] = timestamp;
		zcV[zc_count] = pulse_value;

		zcShot[zc_count] = shot_value;
		zc_count += 1;

		if (zc_count == k || newSection)
		{
			newSection = false;
			return;
		}

		if (zcShot[(zc_count - 2)] * zcMaxShootDifference / 100 > shot_value)
		{
			if (timestamp - zcT[(zc_count - 2)] < zcDiscriminatorTime)
			{
				if ( zcT[zc_count-1] >= begin_timestamp ) {
					Log.i(TAG, "deleteZeroCross," + zcT[zc_count-1] + "," + zcV[zc_count-1] + "," + zcShot[zc_count-1]);
				}

				zc_count -= 1;

				return;
			}
		}

		zcRR[(zc_count - 1)] = (int)(timestamp - zcT[(zc_count - 2)]);
		int rawBpm = 60000/zcRR[(zc_count - 1)];
		Log.i(TAG, "addRawBpm," + timestamp + "," + rawBpm);

		if (pulseobserver != null)
		{
			pulseobserver.onValidRR(timestamp, zcRR[(zc_count - 1)]);
		}

		if (Math.abs(zcV[(zc_count - 2)] - zcV[(zc_count - 1)]) <= Math.abs(shot_value) * valueDifferenceTriggerFactor)
		{
			k = 0;
		}

		if (k != 0)
		{
			if (pulseobserver != null)
			{
				pulseobserver.onHRUpdate(0, totalMeasuringTime);
			}
			totalMeasuringTime = 0;
			return;
		}

		calcHR3();

		if ((calculatedHr >= minHeartRate) && (calculatedHr <= heartRateLimit))
		{
			if (totalMeasuringTime == 0)
			{
				totalMeasuringTime = calculatedHrQuality;
			} else
			{
				totalMeasuringTime += zcRR[(zc_count - 1)];
			}

			if (pulseobserver != null)
			{
				pulseobserver.onHRUpdate(calculatedHr, totalMeasuringTime);
				pulseobserver.onValidatedRR(timestamp, zcRR[(zc_count - 1)]);
			}

//			Log.i(TAG, "Calculated " + calculatedHr + " " + calculatedHrQuality);
//			addBpm(timestamp, calculatedHr);
		}

		addBpm(timestamp, calculatedHr);

	}

	int calcHR3()
	{

		int span = 0;

		calculatedHr = 0;
		calculatedHrQuality = 0;

		int temp = 0;
		int rr_count = 0;

		for (rr_count = zc_count - 1; rr_count >= 1; rr_count--)
		{
			temp = temp + zcRR[rr_count];

			if ( temp > 5000 && zc_count - rr_count >= 3 && temp < 15000) {
				System.arraycopy(zcRR, rr_count, tempBuffer, 0, zc_count - rr_count );
				Arrays.sort(tempBuffer, 0, zc_count - rr_count);

				int medium = 0;
				if ( (zc_count - rr_count) % 2 == 1) {
					medium = tempBuffer[(zc_count - rr_count)/2];
				} else {
					medium = (tempBuffer[(zc_count - rr_count)/2 - 1] + tempBuffer[(zc_count - rr_count)/2])/2;
				}

				int quality = 0;

				int violation = 0;

				for (int i = 0; i < zc_count - rr_count; i++)
				{
					if (Math.abs(tempBuffer[i] - medium) <= maxTimeVariability)
					{
						quality += tempBuffer[i];
					} else {
						violation++;
					}
				}

				if (violation <= 0.2 * (zc_count - rr_count))
				{
					span = quality/(zc_count - rr_count - violation);
					calculatedHr = (60000 / span);

					return calculatedHr;
				}
			}
		}
		// break;

		return calculatedHr;
	}

	public void restart(){
		newSection = true;

		sample_count = 0;
		peak_count = 0;
		valid_peak_count = 0;
//		zc_count = 0;
//		bpm_count = 0;
		ecg_direction = 0;
		lastProcessedCrossSample = 0;
//		goodRRCnt = 0;
//		errorCounter = 0;
//		goodRRTime = 0;
//		goodUninteruptedRRCnt = 0;
//		badBeatRR = 0;
//		totalMeasuringTime = 0;

		Log.i(TAG, "Restart");
	}

	int calcHR()
	{

		int span = 0;

		calculatedHr = 0;
		calculatedHrQuality = 0;

		for (int rr_count = 4; rr_count <= Math.min(30, zc_count); rr_count++)
		{
			System.arraycopy(zcRR, zc_count - rr_count, tempBuffer, 0, rr_count);
			Arrays.sort(tempBuffer, 0, rr_count);

			int quality = 0;

			for (int i = 1; i < rr_count - 1; i++)
			{
				quality += tempBuffer[i];
			}

			span = quality / (rr_count - 2);

			int violation = 0;

			if (Math.abs(tempBuffer[0] - span) <= maxTimeVariability)
			{
				quality += tempBuffer[0];
			} else {
				violation++;
			}

			if (Math.abs(tempBuffer[(rr_count - 1)] - span) < maxTimeVariability)
			{
				quality += tempBuffer[(rr_count - 1)];
			} else {
				violation++;
			}

			if (violation <= 1)
			{
				calculatedHr = (60000 / span);
				calculatedHrQuality = quality;
				goodRRCnt = rr_count;

				if (calculatedHrQuality <= maxAveragingTime)
				{

					break;
				}

				return calculatedHr;
			}

		}

		return calculatedHr;
	}

	public Sample[] getBpm()
	{
		Sample[] arrayOfSample = new Sample[bpm_count];

		for (int i = 0; i < bpm_count; i++)
		{
			Sample sample = new Sample();
			sample.t = bpmT[i];
			sample.v = bpmV[i];

			arrayOfSample[i] = sample;
		}

		return arrayOfSample;
	}

	public PulseObserver getPulseObserver()
	{
		return pulseobserver;
	}

	public Sample[] getPeaks()
	{
		Sample[] samples = new Sample[peak_count];

		for (int i = 0; i < peak_count; i++)
		{
			Sample sample = new Sample();
			sample.t = peaksT[i];
			sample.v = peaksV[i];

			samples[i] = sample;
		}

		return samples;
	}

	public Sample[] getPeaksGood()
	{
		Sample[] samples = new Sample[valid_peak_count];

		for (int i = 0; i < valid_peak_count; i++)
		{
			Sample sample = new Sample();
			sample.t = validPeaksT[i];
			sample.v = validPeaksV[i];

			samples[i] = sample;
		}

		return samples;
	}

	public Sample[] getRR()
	{
		Sample[] samples = new Sample[zc_count];

		for (int i = 0; i < zc_count; i++)
		{
			Sample sample = new Sample();
			sample.t = zcT[i];
			sample.v = zcV[i];

			samples[i] = sample;
		}

		return samples;
	}

	public Sample[] getSamples()
	{
		Sample[] samples = new Sample[sample_count];

		for (int i = 0; i < sample_count; i++)
		{
			Sample sample = new Sample();
			sample.t = samplesT[i];
			sample.v = samplesV[i];

			samples[i] = sample;
		}

		return samples;
	}

	public Sample[] getZeroCross()
	{
		Sample[] samples = new Sample[zc_count];

		for (int i = 0; i < zc_count; i++)
		{
			Sample sample = new Sample();
			sample.t = zcT[i];
			sample.v = zcV[i];

			samples[i] = sample;
		}

		return samples;
	}

	public void loadSamples(Sample[] samples)
	{
		for (int i = 0; i < samples.length; i++)
		{
			addSample(samples[i].t, (int)samples[i].v);
		}
	}

	public void reset()
	{
		sample_count = 0;
		peak_count = 0;
		valid_peak_count = 0;
		zc_count = 0;
		bpm_count = 0;
		ecg_direction = 0;
		lastProcessedCrossSample = 0;
		goodRRCnt = 0;
		errorCounter = 0;
		goodRRTime = 0;
		goodUninteruptedRRCnt = 0;
		badBeatRR = 0;
		totalMeasuringTime = 0;

		Log.i(TAG, "Reset");
	}

	public void setPulseObserver(PulseObserver paramPulseObserver)
	{
		pulseobserver = paramPulseObserver;
	}
}
