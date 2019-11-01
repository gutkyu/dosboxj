package org.gutkyu.dosboxj.hardware.sound;

public interface IAudioSystem {
    public void open(IAudioCallback callback, AudioSpecs actualSpecs) throws Exception;

    public void lock();

    public void unlock();

    public void start();

    public void stop();

    public void pause();
}