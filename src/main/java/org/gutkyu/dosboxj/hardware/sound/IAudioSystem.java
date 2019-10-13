package org.gutkyu.dosboxj.hardware.sound;

public interface IAudioSystem {
    public void open();
    public void lock();
    public void unlock();
    public void stop();
    public void pause();
    public void write(byte[] buffer, int length);
}