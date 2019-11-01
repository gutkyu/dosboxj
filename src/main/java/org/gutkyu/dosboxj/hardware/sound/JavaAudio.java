package org.gutkyu.dosboxj.hardware.sound;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;

import org.gutkyu.dosboxj.util.DOSException;

final class JavaAudio implements IAudioSystem {

    private final float sampleRate = 22050;// 22khz
    private final int sampleSizeInBits = 16;// 16bits
    private final int channels = 2;
    private final boolean signed = true;
    private final boolean bigEndian = false;
    private AudioFormat format = null;
    private int frequency = 0;
    private int sampleFrames = 0;
    private byte[] stream = null;
    private IAudioCallback callback = null;
    private ScheduledExecutorService exec = null;
    private AtomicBoolean preventCallback = new AtomicBoolean(false);
    private AtomicBoolean isRunning = new AtomicBoolean(false);
    private AtomicInteger skipCount = new AtomicInteger(0);
    private SourceDataLine src = null;

    private JavaAudio() {
        format = new AudioFormat(sampleRate, sampleSizeInBits, channels, signed, bigEndian);
        exec = Executors.newSingleThreadScheduledExecutor();
    }

    @Override
    public void open(IAudioCallback callback, AudioSpecs actualSpecs) throws Exception {
        if (src != null)
            throw new DOSException("a device don't been opened. current status is opened ");

        src = AudioSystem.getSourceDataLine(format);
        src.open(format);

        AudioFormat currentAudioFormat = src.getFormat();
        frequency = actualSpecs.frequency = (int) currentAudioFormat.getSampleRate();
        sampleFrames = actualSpecs.sampleFrames = currentAudioFormat.getFrameSize();
        stream = new byte[Math.max(src.getBufferSize(), MixerCore.MIXER_BUFMASK)];
        this.callback = callback;
        long period = (long) (1000000 / currentAudioFormat.getFrameRate());
        exec.scheduleAtFixedRate(this::run, 0, period, TimeUnit.MICROSECONDS);

    }

    private void run() {
        if (preventCallback.get() || isRunning.get()) {
            skipCount.incrementAndGet();
            return;
        }
        isRunning.set(true);
        int times = skipCount.get() + 1;
        int writen = callback.call(stream, times * sampleFrames);
        if (writen > 0) {
            src.write(stream, 0, writen);
        }
        skipCount.addAndGet(-1 * (Math.max(times - 1, 0)));
        isRunning.set(false);
    }

    @Override
    public void lock() {
        preventCallback.set(true);
    }

    @Override
    public void unlock() {
        preventCallback.set(false);
    }

    @Override
    public void start() {
        if (src == null)
            return;
        src.start();
    }

    @Override
    public void stop() {
        if (src == null)
            return;
        src.stop();
        src.flush();
        src.close();
    }

    @Override
    public void pause() {
        if (src == null)
            return;
        src.stop();
    }

    private static final JavaAudio audio = new JavaAudio();

    public static IAudioSystem instance() {
        return audio;
    }

}
