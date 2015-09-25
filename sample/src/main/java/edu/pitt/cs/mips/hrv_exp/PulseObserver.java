package edu.pitt.cs.mips.hrv_exp;

public abstract interface PulseObserver
{
  public abstract void onHRUpdate(int heartrate, int duration);

  public abstract void onValidRR(long timestamp, int value);

  public abstract void onValidatedRR(long timestamp, int value);
}