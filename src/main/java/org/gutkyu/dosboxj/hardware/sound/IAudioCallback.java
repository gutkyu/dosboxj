package org.gutkyu.dosboxj.hardware.sound;

@FunctionalInterface
interface IAudioCallback {
    // return : actually writen stream length
    int call(byte[] stream, int wantedLength);
}