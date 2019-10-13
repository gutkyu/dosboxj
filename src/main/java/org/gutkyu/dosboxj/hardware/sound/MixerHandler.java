package org.gutkyu.dosboxj.hardware.sound;

@FunctionalInterface
public interface MixerHandler {
    //(Bitu len)
    void run(int len);
}
