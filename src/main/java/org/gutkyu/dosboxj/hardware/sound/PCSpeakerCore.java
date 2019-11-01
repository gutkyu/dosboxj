package org.gutkyu.dosboxj.hardware.sound;

import org.gutkyu.dosboxj.hardware.PIC;
import org.gutkyu.dosboxj.hardware.Timer;
import org.gutkyu.dosboxj.util.ByteConv;
import org.gutkyu.dosboxj.util.Log;

public final class PCSpeakerCore {
    private static final int SPKR_ENTRIES = 1024;
    private static final int SPKR_VOLUME = 5000;
    private static final float SPKR_SPEED = (float) ((SPKR_VOLUME * 2) / 0.070f);

    protected enum SPKR_MODES {
        SPKR_OFF, SPKR_ON, SPKR_PIT_OFF, SPKR_PIT_ON
    };

    private class DelayEntry {
        float index;
        float vol;
    };

    // struct spkr
    protected MixerChannel chan;
    protected SPKR_MODES mode;
    protected int pitMode;// Bitu
    protected int rate;// Bitu
    protected float pitLast;
    protected float pitNewMax, pitNewHalf;
    protected float pitMax, pitHalf;
    protected float pitIndex;
    protected float volWant, volCur;
    protected int lastTicks;// Bitu
    protected float lastIndex;
    protected int minTr;// Bitu
    protected DelayEntry entries[] = new DelayEntry[SPKR_ENTRIES];
    protected int used;// Bitu

    private PCSpeakerCore(){
        for (int i = 0; i < SPKR_ENTRIES; i++) {
            entries[i] = new DelayEntry();
        }
    }

    private void addDelayEntry(float index, float vol) {
        if (this.used == SPKR_ENTRIES) {
            return;
        }
        this.entries[this.used].index = index;
        this.entries[this.used].vol = vol;
        this.used++;
    }

    private void forwardPIT(float newIndex) {
        float passed = (newIndex - this.lastIndex);
        float delayBase = this.lastIndex;
        this.lastIndex = newIndex;
        switch (this.pitMode) {
        case 0:
            return;
        case 1:
            return;
        case 2:
            while (passed > 0) {
                /* passed the initial low cycle? */
                if (this.pitIndex >= this.pitHalf) {
                    /* Start a new low cycle */
                    if ((this.pitIndex + passed) >= this.pitMax) {
                        float delay = this.pitMax - this.pitIndex;
                        delayBase += delay;
                        passed -= delay;
                        this.pitLast = -SPKR_VOLUME;
                        if (this.mode == SPKR_MODES.SPKR_PIT_ON)
                            addDelayEntry(delayBase, this.pitLast);
                        this.pitIndex = 0;
                    } else {
                        this.pitIndex += passed;
                        return;
                    }
                } else {
                    if ((this.pitIndex + passed) >= this.pitHalf) {
                        float delay = this.pitHalf - this.pitIndex;
                        delayBase += delay;
                        passed -= delay;
                        this.pitLast = SPKR_VOLUME;
                        if (this.mode == SPKR_MODES.SPKR_PIT_ON)
                            addDelayEntry(delayBase, this.pitLast);
                        this.pitIndex = this.pitHalf;
                    } else {
                        this.pitIndex += passed;
                        return;
                    }
                }
            }
            break;
        // END CASE 2
        case 3:
            while (passed > 0) {
                /* Determine where in the wave we're located */
                if (this.pitIndex >= this.pitHalf) {
                    if ((this.pitIndex + passed) >= this.pitMax) {
                        float delay = this.pitMax - this.pitIndex;
                        delayBase += delay;
                        passed -= delay;
                        this.pitLast = SPKR_VOLUME;
                        if (this.mode == SPKR_MODES.SPKR_PIT_ON)
                            addDelayEntry(delayBase, this.pitLast);
                        this.pitIndex = 0;
                        /* Load the new count */
                        this.pitHalf = this.pitNewHalf;
                        this.pitMax = this.pitNewMax;
                    } else {
                        this.pitIndex += passed;
                        return;
                    }
                } else {
                    if ((this.pitIndex + passed) >= this.pitHalf) {
                        float delay = this.pitHalf - this.pitIndex;
                        delayBase += delay;
                        passed -= delay;
                        this.pitLast = -SPKR_VOLUME;
                        if (this.mode == SPKR_MODES.SPKR_PIT_ON)
                            addDelayEntry(delayBase, this.pitLast);
                        this.pitIndex = this.pitHalf;
                        /* Load the new count */
                        this.pitHalf = this.pitNewHalf;
                        this.pitMax = this.pitNewMax;
                    } else {
                        this.pitIndex += passed;
                        return;
                    }
                }
            }
            break;
        // END CASE 3
        case 4:
            if (this.pitIndex < this.pitMax) {
                /* Check if we're gonna pass the end this block */
                if (this.pitIndex + passed >= this.pitMax) {
                    float delay = this.pitMax - this.pitIndex;
                    delayBase += delay;
                    passed -= delay;
                    this.pitLast = -SPKR_VOLUME;
                    if (this.mode == SPKR_MODES.SPKR_PIT_ON)
                        addDelayEntry(delayBase, this.pitLast); // No new events unless
                                                                // reprogrammed
                    this.pitIndex = this.pitMax;
                } else
                    this.pitIndex += passed;
            }
            break;
        // END CASE 4
        }
    }

    // (u32, u32)
    public void setCounter(int cntr, int mode) {
        if (this.lastTicks == 0) {
            if (this.chan != null)
                this.chan.enable(true);
            this.lastIndex = 0;
        }
        this.lastTicks = PIC.getTicks();
        float newIndex = PIC.getTickIndex();
        forwardPIT(newIndex);
        switch (mode) {
        case 0: /* Mode 0 one shot, used with realsound */
            if (this.mode != SPKR_MODES.SPKR_PIT_ON)
                return;
            if (cntr > 80) {
                cntr = 80;
            }
            this.pitLast = ((float) cntr - 40) * (SPKR_VOLUME / 40.0f);
            addDelayEntry(newIndex, this.pitLast);
            this.pitIndex = 0;
            break;
        case 1:
            if (this.mode != SPKR_MODES.SPKR_PIT_ON)
                return;
            this.pitLast = SPKR_VOLUME;
            addDelayEntry(newIndex, this.pitLast);
            break;
        case 2: /* Single cycle low, rest low high generator */
            this.pitIndex = 0;
            this.pitLast = -SPKR_VOLUME;
            addDelayEntry(newIndex, this.pitLast);
            this.pitHalf = (1000.0f / Timer.PIT_TICK_RATE) * 1;
            this.pitMax = (1000.0f / Timer.PIT_TICK_RATE) * cntr;
            break;
        case 3: /* Square wave generator */
            if (cntr < this.minTr) {
                /* skip frequencies that can't be represented */
                this.pitLast = 0;
                this.pitMode = 0;
                return;
            }
            this.pitNewMax = (1000.0f / Timer.PIT_TICK_RATE) * cntr;
            this.pitNewHalf = this.pitNewMax / 2;
            break;
        case 4: /* Software triggered strobe */
            this.pitLast = SPKR_VOLUME;
            addDelayEntry(newIndex, this.pitLast);
            this.pitIndex = 0;
            this.pitMax = (1000.0f / Timer.PIT_TICK_RATE) * cntr;
            break;
        default:
            Log.logMsg("Unhandled speaker mode %d", mode);
            return;
        }
        this.pitMode = mode;
    }

    // (u32)
    public void setType(int mode) {
        if (this.lastTicks == 0) {
            if (this.chan != null)
                this.chan.enable(true);
            this.lastIndex = 0;
        }
        this.lastTicks = PIC.getTicks();
        float newIndex = PIC.getTickIndex();
        forwardPIT(newIndex);
        switch (mode) {
        case 0:
            this.mode = SPKR_MODES.SPKR_OFF;
            addDelayEntry(newIndex, -SPKR_VOLUME);
            break;
        case 1:
            this.mode = SPKR_MODES.SPKR_PIT_OFF;
            addDelayEntry(newIndex, -SPKR_VOLUME);
            break;
        case 2:
            this.mode = SPKR_MODES.SPKR_ON;
            addDelayEntry(newIndex, SPKR_VOLUME);
            break;
        case 3:
            if (this.mode != SPKR_MODES.SPKR_PIT_ON) {
                addDelayEntry(newIndex, this.pitLast);
            }
            this.mode = SPKR_MODES.SPKR_PIT_ON;
            break;
        }
        ;
    }

    // (u32)
    protected void callback(int len) {
        int streamIdx = 0;
        forwardPIT(1);
        this.lastIndex = 0;
        int count = len;// u32
        int pos = 0;// u32
        float sampleBase = 0;
        float sampleAdd = (1.0001f) / len;
        byte[] mixBuf = MixerCore.instance().getBuffer();
        while (count-- > 0) {
            float index = sampleBase;
            sampleBase += sampleAdd;
            float end = sampleBase;
            double value = 0;
            while (index < end) {
                /* Check if there is an upcoming event */
                if (this.used == 0 && this.entries[pos].index <= index) {
                    this.volWant = this.entries[pos].vol;
                    pos++;
                    this.used--;
                    continue;
                }
                float volEnd;
                if (this.used == 0 && this.entries[pos].index < end) {
                    volEnd = this.entries[pos].index;
                } else
                    volEnd = end;
                float volLen = volEnd - index;
                /* Check if we have to slide the volume */
                float volDiff = this.volWant - this.volCur;
                if (volDiff == 0) {
                    value += this.volCur * volLen;
                    index += volLen;
                } else {
                    /* Check how long it will take to goto new level */
                    float volTime = Math.abs(volDiff) / SPKR_SPEED;
                    if (volTime <= volLen) {
                        /* Volume reaches endpoint in this block, calc until that point */
                        value += volTime * this.volCur;
                        value += volTime * volDiff / 2;
                        index += volTime;
                        this.volCur = this.volWant;
                    } else {
                        /* Volume still not reached in this block */
                        value += this.volCur * volLen;
                        if (volDiff < 0) {
                            value -= (SPKR_SPEED * volLen * volLen) / 2;
                            this.volCur -= SPKR_SPEED * volLen;
                        } else {
                            value += (SPKR_SPEED * volLen * volLen) / 2;
                            this.volCur += SPKR_SPEED * volLen;
                        }
                        index += volLen;
                    }
                }
            }
            ByteConv.setShort(mixBuf, streamIdx, (short) (value / sampleAdd));// Bit16s
            streamIdx++;
        }
        if (this.chan != null)
            this.chan.addSamplesMono16(len, mixBuf);// addSamples_m16(len,(short *)MixTemp);

        // Turn off speaker after 10 seconds of idle or one second idle when in off mode
        boolean turnOff = false;
        int test_ticks = PIC.getTicks();// u32
        if ((this.lastTicks + 10000) < test_ticks)
            turnOff = true;
        if ((this.mode == SPKR_MODES.SPKR_OFF) && ((this.lastTicks + 1000) < test_ticks))
            turnOff = true;

        if (turnOff) {
            if (this.volWant == 0) {
                this.lastTicks = 0;
                if (this.chan != null)
                    this.chan.enable(false);
            } else {
                if (this.volWant > 0)
                    this.volWant--;
                else
                    this.volWant++;

            }
        }

    }
    
    private static final PCSpeakerCore pcSpk = new PCSpeakerCore();

    // sigleton
    public static PCSpeakerCore instance() {
        return pcSpk;
    }
}
