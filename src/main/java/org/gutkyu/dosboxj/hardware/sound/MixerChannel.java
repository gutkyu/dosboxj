package org.gutkyu.dosboxj.hardware.sound;

import org.gutkyu.dosboxj.hardware.PIC;
import org.gutkyu.dosboxj.hardware.memory.*;
import org.gutkyu.dosboxj.util.ByteConv;
import org.gutkyu.dosboxj.util.Log;

final class MixerChannel {
    public MixerHandler handler;
    public float[] volmain = new float[2];
    public float scale;
    public int[] volmul = new int[2];// int32
    public int freq_add, freq_index;// Bitu
    public long done, needed;// Bitu
    public int[] last = new int[2];// int32
    public String name;
    public boolean enabled;
    public MixerChannel next;
    final private MixerCore mixer = MixerCore.instance();
private IAudioSystem audioSys = JavaAudio.instance();

    public void setVolume(float left, float right) {
        volmain[0] = left;
        volmain[1] = right;
        UpdateVolume();
    }

    public void SetScale(float f) {
        scale = f;
        UpdateVolume();
    }

    public void UpdateVolume() {
        volmul[0] =
                (int) ((1 << MixerCore.MIXER_VOLSHIFT) * scale * volmain[0] * mixer.mastervol[0]);// Bits
        volmul[1] =
                (int) ((1 << MixerCore.MIXER_VOLSHIFT) * scale * volmain[1] * mixer.mastervol[1]);// Bits
    }

    // (Bitu)
    public void setFreq(int freq) {
        freq_add = ((freq << MixerCore.MIXER_SHIFT) / mixer.freq);// Bitu
    }

    // (Bitu)
    public void mix(int needed) {
        this.needed = 0xffffffffL & needed;
        while (this.enabled && this.needed > done) {
            long todo = 0xffffffffL & (this.needed - done);// Bitu
            todo = todo * freq_add;
            todo = (todo >>> MixerCore.MIXER_SHIFT)
                    + ((todo & MixerCore.MIXER_REMAIN) != 0 ? 1 : 0);
            handler.run((int)todo);
        }
    }

    // Fill up until needed
    public void addSilence() {
        if (done < needed) {
            done = needed;
            last[0] = last[1] = 0;
            freq_index = MixerCore.MIXER_REMAIN;
        }
    }

    // 호출부의 코드들은 0에서 시작하는 고정 크기의 array만 인자로 전달
    // 항상 0으로 시작하기 때문에 byte[] data의 시작위치를 표시할 별도 인자를 받을 필요없음
    private void addSamples(int typeSize, boolean stereo, boolean signeddata,
            boolean nativeorder, int lenBasedType, byte[] data) {

        int[] diff = new int[2];// Bits
        int mixpos = (int) (mixer.pos + done);// uint32
        freq_index &= MixerCore.MIXER_REMAIN;
        int pos = 0;// uint32
        int new_pos = 0;// uint32

        // goto thestart;
        boolean theStart = true;
        while (true) {
            if (!theStart)
                new_pos = freq_index >>> MixerCore.MIXER_SHIFT;
            if (theStart || pos < new_pos) {
                if (!theStart) {
                    last[0] += diff[0];
                    if (stereo)
                        last[1] += diff[1];
                    pos = new_pos;
                }
                // thestart:
                if (theStart)
                    theStart = false;
                if (pos >= lenBasedType)
                    return;
                if (typeSize == 1) {
                    if (!signeddata) {
                        if (stereo) {
                            diff[0] = (((byte) (data[pos * 2 + 0] ^ 0x80)) << 8) - last[0];
                            diff[1] = (((byte) (data[pos * 2 + 1] ^ 0x80)) << 8) - last[1];
                        } else {
                            diff[0] = (((byte) (data[pos] ^ 0x80)) << 8) - last[0];
                        }
                    } else {
                        if (stereo) {
                            diff[0] = (data[pos * 2 + 0] << 8) - last[0];
                            diff[1] = (data[pos * 2 + 1] << 8) - last[1];
                        } else {
                            diff[0] = (data[pos] << 8) - last[0];
                        }
                    }
                    // 16bit and 32bit both contain 16bit data internally
                } else {
                    if (signeddata) {
                        if (stereo) {
                            if (nativeorder) {
                                if (typeSize == 2) {
                                    diff[0] = (short) ByteConv.getShort(data, pos * 2 * 2 + 2 * 0)
                                            - last[0];
                                    diff[1] = (short) ByteConv.getShort(data, pos * 2 * 2 + 2 * 1)
                                            - last[1];
                                } else {
                                    diff[0] = ByteConv.getInt(data, pos * 4 * 2 + 4 * 0) - last[0];
                                    diff[1] = ByteConv.getInt(data, pos * 4 * 2 + 4 * 1) - last[1];
                                }
                            } else {
                                if (typeSize == 2) {
                                    diff[0] = (short) Memory.hostReadW(data, pos * 2 + 0) - last[0];
                                    diff[1] = (short) Memory.hostReadW(data, pos * 2 + 1) - last[1];
                                } else {
                                    diff[0] = (int) Memory.hostReadD(data, pos * 2 + 0) - last[0];
                                    diff[1] = (int) Memory.hostReadD(data, pos * 2 + 1) - last[1];
                                }
                            }
                        } else {
                            if (nativeorder) {
                                if (typeSize == 2) {
                                    diff[0] = (short) ByteConv.getShort(data, pos * 2) - last[0];
                                } else {
                                    diff[0] = ByteConv.getInt(data, pos * 4) - last[0];
                                }
                            } else {
                                if (typeSize == 2) {
                                    diff[0] = (short) Memory.hostReadW(data, pos) - last[0];
                                } else {
                                    diff[0] = (int) Memory.hostReadD(data, pos) - last[0];
                                }
                            }
                        }
                    } else {
                        if (stereo) {
                            if (nativeorder) {
                                if (typeSize == 2) {
                                    diff[0] = ByteConv.getShort(data, pos * 2 * 2 + 2 * 0) - 32768
                                            - last[0];// Bits
                                    diff[1] = ByteConv.getShort(data, pos * 2 * 2 + 2 * 1) - 32768
                                            - last[1];// Bits
                                } else {
                                    diff[0] = ByteConv.getInt(data, pos * 4 * 2 + 4 * 0) - 32768
                                            - last[0];// Bits
                                    diff[1] = ByteConv.getInt(data, pos * 4 * 2 + 4 * 1) - 32768
                                            - last[1];// Bits
                                }
                            } else {
                                if (typeSize == 2) {
                                    diff[0] = (int) Memory.hostReadW(data, pos * 2 + 0) - 32768
                                            - last[0];// Bits
                                    diff[1] = (int) Memory.hostReadW(data, pos * 2 + 1) - 32768
                                            - last[1];// Bits
                                } else {
                                    diff[0] = (int) Memory.hostReadD(data, pos * 2 + 0) - 32768
                                            - last[0];// Bits
                                    diff[1] = (int) Memory.hostReadD(data, pos * 2 + 1) - 32768
                                            - last[1];// Bits
                                }
                            }
                        } else {
                            if (nativeorder) {
                                if (typeSize == 2) {
                                    diff[0] = ByteConv.getShort(data, pos * 2) - 32768 - last[0];// Bits
                                } else {
                                    diff[0] = ByteConv.getInt(data, pos * 4) - 32768 - last[0];// Bits
                                }
                            } else {
                                if (typeSize == 2) {
                                    diff[0] = (int) Memory.hostReadW(data, pos) - 32768 - last[0];// Bits
                                } else {
                                    diff[0] = (int) Memory.hostReadD(data, pos) - 32768 - last[0];// Bits
                                }
                            }
                        }
                    }
                }
            }
            int diff_mul = freq_index & MixerCore.MIXER_REMAIN;// Bits
            freq_index = freq_index + freq_add;
            mixpos &= MixerCore.MIXER_BUFMASK;
            int sample = last[0] + ((diff[0] * diff_mul) >> MixerCore.MIXER_SHIFT);// Bits
            mixer.work[mixpos][0] += sample * volmul[0];
            if (stereo)
                sample = last[1] + ((diff[1] * diff_mul) >> MixerCore.MIXER_SHIFT);
            mixer.work[mixpos][1] += sample * volmul[1];
            mixpos++;
            done++;
        }
    }

    // Strech block up into needed data
    // (Bitu len,Bit16s * data)
    public void addStretched(int len, short[] data) {

        if (done >= needed) {
            Log.logMsg("Can't add, buffer full");
            return;
        }
        int outlen = (int) (needed - done);// Bitu
        int diff;
        freq_index = 0;
        int temp_add = (len << MixerCore.MIXER_SHIFT) / outlen;// Bitu
        int mixpos = (int) (mixer.pos + done);// Bitu
        done = needed;
        int pos = 0;// Bitu
        diff = data[0] - last[0];
        while (outlen-- >= 0) {
            int new_pos = freq_index >>> MixerCore.MIXER_SHIFT;// Bitu
            if (pos < new_pos) {
                pos = new_pos;
                last[0] += diff;
                diff = data[pos] - last[0];
            }
            int diff_mul = freq_index & MixerCore.MIXER_REMAIN;// Bits
            freq_index += temp_add;
            mixpos &= MixerCore.MIXER_BUFMASK;
            int sample = last[0] + ((diff * diff_mul) >>> MixerCore.MIXER_SHIFT);
            mixer.work[mixpos][0] += sample * volmul[0];
            mixer.work[mixpos][1] += sample * volmul[1];
            mixpos++;
        }
    }

    //addSamples_m8
    public void addSamplesMono8(int len, byte[] data) {
        // Type : Bit8u
        addSamples(1, false, false, true, len, data);
    }

    // addSamples_s8(Bitu len,const Bit8u * data)
    public void addSamplesStereo8(int len, byte[] data) {
        // Type : Bit8u
        addSamples(1, true, false, true, len, data);
    }

    // addSamples_m8s(Bitu len,const Bit8s * data)
    public void addSamplesMono8S(int len, byte[] data) {
        // Type : Bit8s
        addSamples(1, false, true, true, len, data);
    }

    // addSamples_s8s(Bitu len,const Bit8s * data)
    public void addSamplesStereo8S(int len, byte[] data) {
        // Type : Bit8s
        addSamples(1, true, true, true, len, data);
    }

    // addSamples_m16(Bitu len,const Bit16s * data)
    public void addSamplesMono16(int len2B, byte[] data) {
        // Type : Bit16s
        addSamples(2, false, true, true, len2B, data);
    }

    // addSamples_s16(Bitu len,const Bit16s * data)
    public void addSamplesStereo16(int len2B, byte[] data) {
        // Type : Bit16s
        addSamples(2, true, true, true, len2B, data);
    }

    // addSamples_m16u(Bitu len,const Bit16u * data)
    public void addSamplesMono16U(int len2B, byte[] data) {
        // Type : Bit16u
        addSamples(2, false, false, true, len2B, data);
    }

    // addSamples_s16u(Bitu len,const Bit16u * data)
    public void addSamplesStereo16U(int len2B, byte[] data) {
        // Type : Bit16u
        addSamples(2, true, false, true, len2B, data);
    }

    // addSamples_m32(Bitu len,const Bit32s * data)
    public void addSamplesMono32(int len4B, byte[] data) {
        // Type : Bit32s
        addSamples(4, false, true, true, len4B, data);
    }

    // addSamples_s32(Bitu len,const Bit32s * data)
    public void addSamplesStero32(int len4B, byte[] data) {
        // Type : Bit32s
        addSamples(4, true, true, true, len4B, data);
    }

    // TODO : if system is big endian, need to follow methods
    // AddSamples_m16_nonnative
    // AddSamples_s16_nonnative
    // AddSamples_m16u_nonnative
    // AddSamples_s16u_nonnative
    // AddSamples_m32_nonnative
    // AddSamples_s32_nonnative

    public void FillUp() {

        audioSys.lock();
        if (!this.enabled || done < mixer.done) {
            audioSys.unlock();
            return;
        }
        float index = PIC.getTickIndex();
        mix((int) (index * mixer.needed));
        audioSys.unlock();
    }

    public void enable(boolean enabled) {
        if (this.enabled == enabled)
            return;
        this.enabled = enabled;
        if (this.enabled) {
            freq_index = MixerCore.MIXER_REMAIN;
        audioSys.lock();
            if (done < mixer.done)
                done = mixer.done;
            audioSys.unlock();
        }
    }

}
