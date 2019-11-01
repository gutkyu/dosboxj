package org.gutkyu.dosboxj.hardware.sound;

import org.gutkyu.dosboxj.DOSBox;
import org.gutkyu.dosboxj.util.ByteConv;

final class MixerCore {
    protected final static int MIXER_BUFSIZE = 16 * 1024;
    protected final static int MIXER_BUFMASK = MIXER_BUFSIZE - 1;

    protected final static int MIXER_SSIZE = 4;
    protected final static int MIXER_SHIFT = 14;
    protected final static int MIXER_REMAIN = ((1 << MIXER_SHIFT) - 1);
    protected final static int MIXER_VOLSHIFT = 13;

    protected final static int MAX_AUDIO = (1 << (16 - 1)) - 1;
    protected final static int MIN_AUDIO = -(1 << (16 - 1));

    // static struct mixer
    protected int work[][] = new int[MIXER_BUFSIZE][2];// int32
    protected int pos;// uint32
    protected int done;// uint32
    protected int needed, minNeeded, maxNeeded;// uint32
    protected long tickAdd, tickRemain;// uint32
    protected float mastervol[] = new float[2];
    protected MixerChannel channels;
    protected boolean nosound;
    protected int freq;// uint32
    protected int blockSize;// uint32
    final private byte[] MixTemp = new byte[MIXER_BUFSIZE];// uint8

    final private IAudioSystem audioSys = JavaAudio.instance();

    protected MixerChannel addChannel(MixerHandler handler, int freq, String name) {
        MixerChannel chan = new MixerChannel();
        chan.scale = 1.0f;
        chan.handler = handler;
        chan.name = name;
        chan.setFreq(freq);
        chan.next = this.channels;
        chan.setVolume(1, 1);
        chan.enabled = false;
        this.channels = chan;
        return chan;
    }

    protected MixerChannel findChannel(String name) {
        MixerChannel chan = this.channels;
        while (chan != null) {
            if (chan.name.equalsIgnoreCase(name))
                break;
            chan = chan.next;
        }
        return chan;
    }

    protected void delChannel(MixerChannel delchan) {
        MixerChannel chan = this.channels;
        MixerChannel pre = null;
        while (chan != null) {
            if (chan == delchan) {
                if (pre == null) {
                    this.channels = chan.next;
                } else {
                    pre.next = chan.next;
                }
                // delchan.dispose();
                return;
            }
            pre = chan;
            chan = chan.next;
        }
    }

    //
    protected byte[] getBuffer() {
        return MixTemp;// 매번 메모리 할당하지 않고 고정크기의 작업용 버퍼 생성 후 여러군데에서 재사용
    }

    private boolean irqImportant() {
        /*
         * In some states correct timing of the irqs is more important then non
         * stuttering audo
         */
        // TODO:CAPTURE_WAVE or CAPTURE_VIDEO
        // return (ticksLocked || (CaptureState & (CAPTURE_WAVE|CAPTURE_VIDEO)));
        return DOSBox._ticksLocked;
    }

    // todo: int needed -> long needed로 바꿀것
    /* Mix a certain amount of new samples */
    private void mixData(int needed) {
        MixerChannel chan = this.channels;
        while (chan != null) {
            chan.mix(needed);
            chan = chan.next;
        }
        // TODO:CAPTURE_WAVE or CAPTURE_VIDEO
        /*
         * if (CaptureState & (CAPTURE_WAVE|CAPTURE_VIDEO)) { }
         */
        // Reset the the tick_add for constant speed
        if (irqImportant())
            this.tickAdd = ((this.freq) << MIXER_SHIFT) / 1000;
        this.done = needed;
    }

    protected void mix() {
        audioSys.lock();
        mixData(this.needed);
        this.tickRemain += this.tickAdd;
        this.needed += (this.tickRemain >> MIXER_SHIFT);
        this.tickRemain &= MIXER_REMAIN;
        audioSys.unlock();
    }

    protected void mixNoSound() {
        mixData(this.needed);
        /* Clear piece we've just generated */
        for (int i = 0; i < this.needed; i++) {
            this.work[this.pos][0] = 0;
            this.work[this.pos][1] = 0;
            this.pos = (this.pos + 1) & MIXER_BUFMASK;
        }
        /* Reduce count in channels */
        for (MixerChannel chan = this.channels; chan != null; chan = chan.next) {
            if (chan.done > this.needed)
                chan.done -= this.needed;
            else
                chan.done = 0;
        }
        /* Set values for next tick */
        this.tickRemain += this.tickAdd;
        this.needed = (int) (this.tickRemain >> MIXER_SHIFT);
        this.tickRemain &= MIXER_REMAIN;
        this.done = 0;
    }

    // int16 (int32)
    private short clip(int samp) {
        if (samp < MAX_AUDIO) {
            if (samp > MIN_AUDIO)
                return (short) samp;
            else
                return MIN_AUDIO;
        } else
            return MAX_AUDIO;
    }

    // (void * userdata, Uint8 *stream, int len)
    public int callback(byte[] stream, int len) {
        int need = len / MIXER_SSIZE;// Bitu
        // Bit16s * output=(Bit16s *)stream;
        int outputIdx = 0;
        int reduce;// Bitu
        int pos, index, indexAdd;// Bitu
        int sample;// Bits
        /* Enough room in the buffer ? */
        if (this.done < need) {
            // LOG_MSG("Full underrun need %d, have %d, min %d", need, mixer.done,
            // mixer.min_needed);
            if ((need - this.done) > (need >> 7)) // Max 1 procent stretch.
                return outputIdx;
            reduce = this.done;
            indexAdd = (reduce << MIXER_SHIFT) / need;
            this.tickAdd = ((this.freq + this.minNeeded) << MIXER_SHIFT) / 1000;
        } else if (this.done < this.maxNeeded) {
            int left = this.done - need;// Bitu
            if (left < this.minNeeded) {
                if (!irqImportant()) {
                    int needed = this.needed - need;// Bitu
                    int diff = (this.minNeeded > needed ? this.minNeeded : needed) - left;// Bitu
                    this.tickAdd = ((this.freq + (diff * 3)) << MIXER_SHIFT) / 1000;
                    left = 0; // No stretching as we compensate with the tick_add value
                } else {
                    left = (this.minNeeded - left);
                    left = 1 + (2 * left) / this.minNeeded; // left=1,2,3
                }
                // LOG_MSG("needed underrun need %d, have %d, min %d, left %d", need,
                // mixer.done, mixer.min_needed, left);
                reduce = need - left;
                indexAdd = (reduce << MIXER_SHIFT) / need;
            } else {
                reduce = need;
                indexAdd = (1 << MIXER_SHIFT);
                // LOG_MSG("regular run need %d, have %d, min %d, left %d", need, mixer.done,
                // mixer.min_needed, left);

                /*
                 * Mixer tick value being updated: 3 cases: 1) A lot too high. >division by 5.
                 * but maxed by 2* min to prevent too fast drops. 2) A little too high >
                 * division by 8 3) A little to nothing above the min_needed buffer > go to
                 * default value
                 */
                int diff = left - this.minNeeded;// Bitu
                if (diff > (this.minNeeded << 1))
                    diff = this.minNeeded << 1;
                if (diff > (this.minNeeded >> 1))
                    this.tickAdd = ((this.freq - (diff / 5)) << MIXER_SHIFT) / 1000;
                else if (diff > (this.minNeeded >> 2))
                    this.tickAdd = ((this.freq - (diff >> 3)) << MIXER_SHIFT) / 1000;
                else
                    this.tickAdd = (this.freq << MIXER_SHIFT) / 1000;
            }
        } else {
            /* There is way too much data in the buffer */
            // LOG_MSG("overflow run need %d, have %d, min %d", need, mixer.done,
            // mixer.min_needed);
            if (this.done > MIXER_BUFSIZE)
                indexAdd = MIXER_BUFSIZE - 2 * this.minNeeded;
            else
                indexAdd = this.done - 2 * this.minNeeded;
            indexAdd = (indexAdd << MIXER_SHIFT) / need;
            reduce = this.done - 2 * this.minNeeded;
            this.tickAdd = ((this.freq - (this.minNeeded / 5)) << MIXER_SHIFT) / 1000;
        }
        /* Reduce done count in all channels */
        for (MixerChannel chan = this.channels; chan != null; chan = chan.next) {
            if (chan.done > reduce)
                chan.done -= reduce;
            else
                chan.done = 0;
        }

        // Reset mixer.tick_add when irqs are important
        if (irqImportant())
            this.tickAdd = (this.freq << MIXER_SHIFT) / 1000;

        this.done -= reduce;
        this.needed -= reduce;
        pos = this.pos;
        this.pos = (this.pos + reduce) & MIXER_BUFMASK;
        index = 0;
        if (need != reduce) {
            while (need-- >= 0) {
                int i = (pos + (index >> MIXER_SHIFT)) & MIXER_BUFMASK;
                index += indexAdd;
                sample = this.work[i][0] >> MIXER_VOLSHIFT;
                // *output++=clip(sample);
                ByteConv.setShort(stream, outputIdx, clip(sample));
                outputIdx += 2;
                sample = this.work[i][1] >> MIXER_VOLSHIFT;
                // *output++=clip(sample);
                ByteConv.setShort(stream, outputIdx, clip(sample));
                outputIdx += 2;
            }
            /* Clean the used buffer */
            while (reduce-- >= 0) {
                pos &= MIXER_BUFMASK;
                this.work[pos][0] = 0;
                this.work[pos][1] = 0;
                pos++;
            }
        } else {
            while (reduce-- >= 0) {
                pos &= MIXER_BUFMASK;
                sample = this.work[pos][0] >> MIXER_VOLSHIFT;
                // *output++=clip(sample);
                ByteConv.setShort(stream, outputIdx, clip(sample));
                outputIdx += 2;
                sample = this.work[pos][1] >> MIXER_VOLSHIFT;
                // *output++=clip(sample);
                ByteConv.setShort(stream, outputIdx, clip(sample));
                outputIdx += 2;
                this.work[pos][0] = 0;
                this.work[pos][1] = 0;
                pos++;
            }
        }
        return outputIdx;
    }

    private static final MixerCore mixer = new MixerCore();

    // sigleton
    public static MixerCore instance() {
        return mixer;
    }
}
