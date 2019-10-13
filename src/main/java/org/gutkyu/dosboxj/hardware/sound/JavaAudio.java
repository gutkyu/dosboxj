package org.gutkyu.dosboxj.hardware.sound;

final class JavaAudio implements IAudioSystem {

    private JavaAudio(){

    }

    @Override
    public void open() {
        // TODO Auto-generated method stub

    }

    @Override
    public void lock() {
        // TODO Auto-generated method stub

    }

    @Override
    public void unlock() {
        // TODO Auto-generated method stub

    }

    @Override
    public void stop() {
        // TODO Auto-generated method stub

    }

    @Override
    public void pause() {
        // TODO Auto-generated method stub

    }

    @Override
    public void write(byte[] buffer, int length) {
        // TODO Auto-generated method stub

    }
    
    private static JavaAudio audio = new JavaAudio();
    public static IAudioSystem instance(){
        return audio;
    }
}