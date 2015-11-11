package com.mage.sopxy.internal;


public class Timer
{
    private final long ms;

    private final long start;


    public Timer(long ms)
    {
        this.ms = ms;
        this.start = System.currentTimeMillis();
    }


    public boolean timeLeft()
    {
        return System.currentTimeMillis() - start < ms;
    }
}
