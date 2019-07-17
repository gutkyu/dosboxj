package org.gutkyu.dosboxj.hardware.video;

import org.gutkyu.dosboxj.hardware.io.IO;
import org.gutkyu.dosboxj.hardware.video.VGA.RGBEntry;
import org.gutkyu.dosboxj.util.*;
import org.gutkyu.dosboxj.DOSBox;
import org.gutkyu.dosboxj.gui.*;

public final class VGADac<RGB> {
    public enum DACState {
        DAC_READ, DAC_WRITE
    }

    public byte Bits; /* DAC bits, usually 6 or 8 */
    public short PelMask;
    public byte PelIndex;
    public DACState State;
    public byte WriteIndex;
    public byte ReadIndex;
    public int FirstChanged;
    // public byte combine[16];
    public byte[] Combine;
    // public RGBEntry rgb[0x100];
    public RGBEntry[] RGB;
    // public short xlat16[256];
    public short[] Xlat16;

    private VGA _vga = null;

    public VGADac(VGA vga) {
        Combine = new byte[16];
        RGB = new RGBEntry[0x100];
        for (int i = 0; i < RGB.length; i++) {
            RGB[i] = new RGBEntry();
        }
        Xlat16 = new short[256];

        _vga = vga;
    }

    private void sendColor(int index, int src) {
        RGBEntry[] dacRGB = RGB;
        int red = 0xff & dacRGB[src].Red;
        int green = 0xff & dacRGB[src].Green;
        int blue = 0xff & dacRGB[src].Blue;
        // Set entry in 16bit output lookup table
        Xlat16[index] = (short) (((blue >>> 1) & 0x1f) | ((green & 0x3f) << 5)
                | (((red >>> 1) & 0x1f) << 11));

        Render.instance().setPal(index, (red << 2) | (red >>> 4), (green << 2) | (green >>> 4),
                (blue << 2) | (blue >>> 4));
    }

    private void updateColor(int index) {
        int maskIndex = index & PelMask;
        sendColor(index, maskIndex);
    }

    private void writeP3C6(int port, int val, int iolen) {
        if (PelMask != val) {
            Log.logging(Log.LogTypes.VGAMISC, Log.LogServerities.Normal,
                    "VGA:DCA:Pel Mask set to %X", val);
            PelMask = (byte) val;
            for (int i = 0; i < 256; i++)
                updateColor(i);
        }
    }


    private int readP3C6(int port, int iolen) {
        return PelMask;
    }


    private void writeP3C7(int port, int val, int iolen) {
        ReadIndex = (byte) val;
        PelIndex = 0;
        State = DACState.DAC_READ;
        WriteIndex = (byte) (val + 1);
    }

    private int readP3C7(int port, int iolen) {
        if (State == DACState.DAC_READ)
            return 0x3;
        else
            return 0x0;
    }

    private void writeP3C8(int port, int val, int iolen) {
        WriteIndex = (byte) val;
        PelIndex = 0;
        State = DACState.DAC_WRITE;
    }

    private int readP3C8(int port, int iolen) {
        return WriteIndex;
    }

    private void writeP3C9(int port, int val, int iolen) {
        val &= 0x3f;
        switch (PelIndex) {
            case 0:
                RGB[WriteIndex].Red = (byte) val;
                PelIndex = 1;
                break;
            case 1:
                RGB[WriteIndex].Green = (byte) val;
                PelIndex = 2;
                break;
            case 2:
                RGB[WriteIndex].Blue = (byte) val;
                switch (_vga.Mode) {
                    case VGA:
                    case LIN8:
                        updateColor(WriteIndex);
                        if (PelMask != 0xff) {
                            int index = WriteIndex;
                            if ((index & PelMask) == index) {
                                for (int i = index + 1; i < 256; i++)
                                    if ((i & PelMask) == index)
                                        updateColor(i);
                            }
                        }
                        break;
                    default:
                        /* Check for attributes and DAC entry link */
                        for (int i = 0; i < 16; i++) {
                            if (Combine[i] == WriteIndex) {
                                sendColor(i, WriteIndex);
                            }
                        }
                        break;
                }
                WriteIndex++;
                // vga.dac.read_index = vga.dac.write_index - 1;//disabled as it breaks Wari
                PelIndex = 0;
                break;
            default:
                // If this can actually happen that will be the day
                Log.logging(Log.LogTypes.VGAGFX, Log.LogServerities.Normal,
                        "VGA:DAC:Illegal Pel Index");
                break;
        }
    }

    private int readP3C9(int port, int iolen) {
        byte ret;
        switch (PelIndex) {
            case 0:
                ret = RGB[ReadIndex].Red;
                PelIndex = 1;
                break;
            case 1:
                ret = RGB[ReadIndex].Green;
                PelIndex = 2;
                break;
            case 2:
                ret = RGB[ReadIndex].Blue;
                ReadIndex++;
                PelIndex = 0;
                // vga.dac.write_index=vga.dac.read_index+1;//disabled as it breaks wari
                break;
            default:
                // If this can actually happen that will be the day
                Log.logging(Log.LogTypes.VGAMISC, Log.LogServerities.Normal,
                        "VGA:DAC:Illegal Pel Index");
                ret = 0;
                break;
        }
        return 0xff & ret;
    }

    // public void combineColor(byte attr, byte pal) {
    public void combineColor(int attr, int pal) {
        /* Check if this is a new color */
        Combine[attr] = (byte) pal;
        switch (_vga.Mode) {
            case LIN8:
                break;
            case VGA:
                // used by copper demo; almost no video card seems to suport it
                if (!DOSBox.isVGAArch() || (DOSBox.SVGACard != DOSBox.SVGACards.None))
                    break;
                // goto GotoDefault;
            default:
                // GotoDefault:
                sendColor(attr, pal);
                break;
        }
    }

    public void setEntry(int entry, byte red, byte green, byte blue) {
        // Should only be called in dosbox.machine != vga
        RGB[entry].Red = red;
        RGB[entry].Green = green;
        RGB[entry].Blue = blue;
        for (int i = 0; i < 16; i++)
            if (Combine[i] == entry)
                sendColor(i, i);
    }

    public void setEntry(int entry, int red, int green, int blue) {
        setEntry(entry, (byte) red, (byte) green, (byte) blue);
    }

    public void setup() {
        FirstChanged = 256;
        Bits = 6;
        PelMask = 0xff;
        PelIndex = 0;
        State = DACState.DAC_READ;
        ReadIndex = 0;
        WriteIndex = 0;
        if (DOSBox.isVGAArch()) {
            /* Setup the DAC IO port Handlers */
            IO.registerWriteHandler(0x3c6, this::writeP3C6, IO.IO_MB);
            IO.registerReadHandler(0x3c6, this::readP3C6, IO.IO_MB);
            IO.registerWriteHandler(0x3c7, this::writeP3C7, IO.IO_MB);
            IO.registerReadHandler(0x3c7, this::readP3C7, IO.IO_MB);
            IO.registerWriteHandler(0x3c8, this::writeP3C8, IO.IO_MB);
            IO.registerReadHandler(0x3c8, this::readP3C8, IO.IO_MB);
            IO.registerWriteHandler(0x3c9, this::writeP3C9, IO.IO_MB);
            IO.registerReadHandler(0x3c9, this::readP3C9, IO.IO_MB);
        } else if (DOSBox.Machine == DOSBox.MachineType.EGA) {
            for (int i = 0; i < 64; i++) {
                if ((i & 4) > 0)
                    RGB[i].Red = 0x2a;
                else
                    RGB[i].Red = 0;
                if ((i & 32) > 0)
                    RGB[i].Red += 0x15;

                if ((i & 2) > 0)
                    RGB[i].Green = 0x2a;
                else
                    RGB[i].Green = 0;
                if ((i & 16) > 0)
                    RGB[i].Green += 0x15;

                if ((i & 1) > 0)
                    RGB[i].Blue = 0x2a;
                else
                    RGB[i].Blue = 0;
                if ((i & 8) > 0)
                    RGB[i].Blue += 0x15;
            }
        }
    }

}
