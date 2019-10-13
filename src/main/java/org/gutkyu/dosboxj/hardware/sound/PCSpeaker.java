package org.gutkyu.dosboxj.hardware.sound;

import org.gutkyu.dosboxj.misc.setup.*;
import org.gutkyu.dosboxj.util.ByteConv;
import org.gutkyu.dosboxj.util.Log;
import org.gutkyu.dosboxj.hardware.*;

public final class PCSpeaker extends ModuleBase {
    private static final int SPKR_ENTRIES = 1024;
    private static final int SPKR_VOLUME = 5000;
    private static float SPKR_SPEED = (float) ((SPKR_VOLUME * 2) / 0.070f);

    private enum SPKR_MODES {
        SPKR_OFF, SPKR_ON, SPKR_PIT_OFF, SPKR_PIT_ON
    };

    private class DelayEntry {
        float index;
        float vol;
    };

    // struct spkr
    private MixerChannel chan;
    private SPKR_MODES mode;
    private int pit_mode;// Bitu
    private int rate;// Bitu
    private float pit_last;
    private float pit_new_max, pit_new_half;
    private float pit_max, pit_half;
    private float pit_index;
    private float volwant, volcur;
    private int last_ticks;// Bitu
    private float last_index;
    private int min_tr;// Bitu
    private DelayEntry entries[] = new DelayEntry[SPKR_ENTRIES];
    private int used;// Bitu


    private MixerObject MixerChan;

    public PCSpeaker(Section configuration) {
        super(configuration);
        this.chan = null;
        SectionProperty section = (SectionProperty) configuration;
        if (!section.getBool("pcspeaker"))
            return;
        this.mode = SPKR_MODES.SPKR_OFF;
        this.last_ticks = 0;
        this.last_index = 0;
        this.rate = section.getInt("pcrate");
        this.pit_max = (1000.0f / Timer.PIT_TICK_RATE) * 65535;
        this.pit_half = this.pit_max / 2;
        this.pit_new_max = this.pit_max;
        this.pit_new_half = this.pit_half;
        this.pit_index = 0;
        this.min_tr = (Timer.PIT_TICK_RATE + this.rate / 2 - 1) / (this.rate / 2);
        this.used = 0;
        /* Register the sound channel */
        this.chan = MixerChan.install(this::callback, this.rate, "SPKR");
    }

    private void addDelayEntry(float index, float vol) {
        if (this.used == SPKR_ENTRIES) {
            return;
        }
        this.entries[this.used].index = index;
        this.entries[this.used].vol = vol;
        this.used++;
    }


    private void forwardPIT(float newindex) {
        float passed = (newindex - this.last_index);
        float delay_base = this.last_index;
        this.last_index = newindex;
        switch (this.pit_mode) {
            case 0:
                return;
            case 1:
                return;
            case 2:
                while (passed > 0) {
                    /* passed the initial low cycle? */
                    if (this.pit_index >= this.pit_half) {
                        /* Start a new low cycle */
                        if ((this.pit_index + passed) >= this.pit_max) {
                            float delay = this.pit_max - this.pit_index;
                            delay_base += delay;
                            passed -= delay;
                            this.pit_last = -SPKR_VOLUME;
                            if (this.mode == SPKR_MODES.SPKR_PIT_ON)
                                addDelayEntry(delay_base, this.pit_last);
                            this.pit_index = 0;
                        } else {
                            this.pit_index += passed;
                            return;
                        }
                    } else {
                        if ((this.pit_index + passed) >= this.pit_half) {
                            float delay = this.pit_half - this.pit_index;
                            delay_base += delay;
                            passed -= delay;
                            this.pit_last = SPKR_VOLUME;
                            if (this.mode == SPKR_MODES.SPKR_PIT_ON)
                                addDelayEntry(delay_base, this.pit_last);
                            this.pit_index = this.pit_half;
                        } else {
                            this.pit_index += passed;
                            return;
                        }
                    }
                }
                break;
            // END CASE 2
            case 3:
                while (passed > 0) {
                    /* Determine where in the wave we're located */
                    if (this.pit_index >= this.pit_half) {
                        if ((this.pit_index + passed) >= this.pit_max) {
                            float delay = this.pit_max - this.pit_index;
                            delay_base += delay;
                            passed -= delay;
                            this.pit_last = SPKR_VOLUME;
                            if (this.mode == SPKR_MODES.SPKR_PIT_ON)
                                addDelayEntry(delay_base, this.pit_last);
                            this.pit_index = 0;
                            /* Load the new count */
                            this.pit_half = this.pit_new_half;
                            this.pit_max = this.pit_new_max;
                        } else {
                            this.pit_index += passed;
                            return;
                        }
                    } else {
                        if ((this.pit_index + passed) >= this.pit_half) {
                            float delay = this.pit_half - this.pit_index;
                            delay_base += delay;
                            passed -= delay;
                            this.pit_last = -SPKR_VOLUME;
                            if (this.mode == SPKR_MODES.SPKR_PIT_ON)
                                addDelayEntry(delay_base, this.pit_last);
                            this.pit_index = this.pit_half;
                            /* Load the new count */
                            this.pit_half = this.pit_new_half;
                            this.pit_max = this.pit_new_max;
                        } else {
                            this.pit_index += passed;
                            return;
                        }
                    }
                }
                break;
            // END CASE 3
            case 4:
                if (this.pit_index < this.pit_max) {
                    /* Check if we're gonna pass the end this block */
                    if (this.pit_index + passed >= this.pit_max) {
                        float delay = this.pit_max - this.pit_index;
                        delay_base += delay;
                        passed -= delay;
                        this.pit_last = -SPKR_VOLUME;
                        if (this.mode == SPKR_MODES.SPKR_PIT_ON)
                            addDelayEntry(delay_base, this.pit_last); // No new events unless
                                                                      // reprogrammed
                        this.pit_index = this.pit_max;
                    } else
                        this.pit_index += passed;
                }
                break;
            // END CASE 4
        }
    }

    // (u32, u32)
    private void SetCounter(int cntr, int mode) {
        if (this.last_ticks == 0) {
            if (this.chan != null)
                this.chan.enable(true);
            this.last_index = 0;
        }
        this.last_ticks = PIC.getTicks();
        float newindex = PIC.getTickIndex();
        forwardPIT(newindex);
        switch (mode) {
            case 0: /* Mode 0 one shot, used with realsound */
                if (this.mode != SPKR_MODES.SPKR_PIT_ON)
                    return;
                if (cntr > 80) {
                    cntr = 80;
                }
                this.pit_last = ((float) cntr - 40) * (SPKR_VOLUME / 40.0f);
                addDelayEntry(newindex, this.pit_last);
                this.pit_index = 0;
                break;
            case 1:
                if (this.mode != SPKR_MODES.SPKR_PIT_ON)
                    return;
                this.pit_last = SPKR_VOLUME;
                addDelayEntry(newindex, this.pit_last);
                break;
            case 2: /* Single cycle low, rest low high generator */
                this.pit_index = 0;
                this.pit_last = -SPKR_VOLUME;
                addDelayEntry(newindex, this.pit_last);
                this.pit_half = (1000.0f / Timer.PIT_TICK_RATE) * 1;
                this.pit_max = (1000.0f / Timer.PIT_TICK_RATE) * cntr;
                break;
            case 3: /* Square wave generator */
                if (cntr < this.min_tr) {
                    /* skip frequencies that can't be represented */
                    this.pit_last = 0;
                    this.pit_mode = 0;
                    return;
                }
                this.pit_new_max = (1000.0f / Timer.PIT_TICK_RATE) * cntr;
                this.pit_new_half = this.pit_new_max / 2;
                break;
            case 4: /* Software triggered strobe */
                this.pit_last = SPKR_VOLUME;
                addDelayEntry(newindex, this.pit_last);
                this.pit_index = 0;
                this.pit_max = (1000.0f / Timer.PIT_TICK_RATE) * cntr;
                break;
            default:
                Log.logMsg("Unhandled speaker mode %d", mode);
                return;
        }
        this.pit_mode = mode;
    }

    // (u32)
    public void setType(int mode) {
        if (this.last_ticks == 0) {
            if (this.chan != null)
                this.chan.enable(true);
            this.last_index = 0;
        }
        this.last_ticks = PIC.getTicks();
        float newindex = PIC.getTickIndex();
        forwardPIT(newindex);
        switch (mode) {
            case 0:
                this.mode = SPKR_MODES.SPKR_OFF;
                addDelayEntry(newindex, -SPKR_VOLUME);
                break;
            case 1:
                this.mode = SPKR_MODES.SPKR_PIT_OFF;
                addDelayEntry(newindex, -SPKR_VOLUME);
                break;
            case 2:
                this.mode = SPKR_MODES.SPKR_ON;
                addDelayEntry(newindex, SPKR_VOLUME);
                break;
            case 3:
                if (this.mode != SPKR_MODES.SPKR_PIT_ON) {
                    addDelayEntry(newindex, this.pit_last);
                }
                this.mode = SPKR_MODES.SPKR_PIT_ON;
                break;
        };
    }


    // (u32)
    private void callback(int len) {
        int streamIdx = 0;
        forwardPIT(1);
        this.last_index = 0;
        int count = len;// u32
        int pos = 0;// u32
        float sample_base = 0;
        float sample_add = (1.0001f) / len;
        byte[] mixBuf = MixerCore.instance().getBuffer();
        while (count-- <= 0) {
            float index = sample_base;
            sample_base += sample_add;
            float end = sample_base;
            double value = 0;
            while (index < end) {
                /* Check if there is an upcoming event */
                if (this.used == 0 && this.entries[pos].index <= index) {
                    this.volwant = this.entries[pos].vol;
                    pos++;
                    this.used--;
                    continue;
                }
                float vol_end;
                if (this.used == 0 && this.entries[pos].index < end) {
                    vol_end = this.entries[pos].index;
                } else
                    vol_end = end;
                float vol_len = vol_end - index;
                /* Check if we have to slide the volume */
                float vol_diff = this.volwant - this.volcur;
                if (vol_diff == 0) {
                    value += this.volcur * vol_len;
                    index += vol_len;
                } else {
                    /* Check how long it will take to goto new level */
                    float vol_time = Math.abs(vol_diff) / SPKR_SPEED;
                    if (vol_time <= vol_len) {
                        /* Volume reaches endpoint in this block, calc until that point */
                        value += vol_time * this.volcur;
                        value += vol_time * vol_diff / 2;
                        index += vol_time;
                        this.volcur = this.volwant;
                    } else {
                        /* Volume still not reached in this block */
                        value += this.volcur * vol_len;
                        if (vol_diff < 0) {
                            value -= (SPKR_SPEED * vol_len * vol_len) / 2;
                            this.volcur -= SPKR_SPEED * vol_len;
                        } else {
                            value += (SPKR_SPEED * vol_len * vol_len) / 2;
                            this.volcur += SPKR_SPEED * vol_len;
                        }
                        index += vol_len;
                    }
                }
            }
            ByteConv.setShort(mixBuf, streamIdx, (short) (value / sample_add));// Bit16s
            streamIdx++;
        }
        if (this.chan != null)
            this.chan.addSamplesMono16(len, mixBuf);// addSamples_m16(len,(short *)MixTemp);

        // Turn off speaker after 10 seconds of idle or one second idle when in off mode
        boolean turnOff = false;
        int test_ticks = PIC.getTicks();// u32
        if ((this.last_ticks + 10000) < test_ticks)
            turnOff = true;
        if ((this.mode == SPKR_MODES.SPKR_OFF) && ((this.last_ticks + 1000) < test_ticks))
            turnOff = true;

        if (turnOff) {
            if (this.volwant == 0) {
                this.last_ticks = 0;
                if (this.chan != null)
                    this.chan.enable(false);
            } else {
                if (this.volwant > 0)
                    this.volwant--;
                else
                    this.volwant++;

            }
        }

    }

    private static PCSpeaker obj;

    public static void shutdown(Section sec) {
        obj = null;
    }

    public static void init(Section sec) {
        obj = new PCSpeaker(sec);
        sec.addDestroyFunction(PCSpeaker::shutdown, true);
    }
}
