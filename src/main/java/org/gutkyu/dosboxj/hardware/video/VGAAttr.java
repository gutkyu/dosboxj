package org.gutkyu.dosboxj.hardware.video;

import org.gutkyu.dosboxj.DOSBox;
import org.gutkyu.dosboxj.hardware.io.IO;
import org.gutkyu.dosboxj.util.*;

public final class VGAAttr {
    // byte palette[16];
    public byte[] Palette;
    public byte ModeControl;
    public int HorizontalPelPanning;
    public byte OverscanColor;
    public byte ColorPlaneEnable;
    public byte ColorSelect;
    public int Index;//byte
    public byte Disabled; // Used for disabling the screen.
    // Bit0: screen disabled by attribute controller index
    // Bit1: screen disabled by sequencer index 1 bit 5
    // These are put together in one variable for performance reasons:
    // the line drawing function is called maybe 60*480=28800 times/s,
    // and we only need to check one variable for zero this way.

    VGA _vga = null;

    public VGAAttr(VGA vga) {
        Palette = new byte[16];

        _vga = vga;
    }

    //public void setPalette(int index, byte val) {
    public void setPalette(int index, int val) {
        Palette[index] = (byte)val;
        if ((ModeControl & 0x80) != 0)
            val = 0xff & ((val & 0xf) | (ColorSelect << 4));
        val &= 63;
        val |= 0xff & ((ColorSelect & 0xc) << 4);
        if (DOSBox.Machine == DOSBox.MachineType.EGA) {
            if ((_vga.Crtc.VerticalTotal | ((_vga.Crtc.Overflow & 1) << 8)) == 260) {
                // check for intensity bit
                if ((val & 0x10) != 0)
                    val |= 0x38;
                else {
                    val &= 0x7;
                    // check for special brown
                    if (val == 6)
                        val = 0x14;
                }
            }
        }
        _vga.Dac.combineColor(index, val);
    }

    private int readP3C0(int port, int iolen) {
        // Wcharts, Win 3.11 & 95 SVGA
        int retVal = Index & 0x1f;
        if ((Disabled & 0x1) == 0)
            retVal |= 0x20;
        return retVal;
    }

    private void writeP3C0(int port, int val, int iolen) {
        if (!_vga.Internal.AttrIndex) {
            Index = val & 0x1F;
            _vga.Internal.AttrIndex = true;
            if ((val & 0x20) != 0)
                Disabled &= 0xff & ~1;
            else
                Disabled |= 1;
            /*
             * 0-4 Address of data register to write to port 3C0h or read from port 3C1h 5 If set
             * screen output is enabled and the palette can not be modified, if clear screen output
             * is disabled and the palette can be modified.
             */
            return;
        } else {
            _vga.Internal.AttrIndex = false;
            switch (Index) {
                /* Palette */
                case 0x00:
                case 0x01:
                case 0x02:
                case 0x03:
                case 0x04:
                case 0x05:
                case 0x06:
                case 0x07:
                case 0x08:
                case 0x09:
                case 0x0a:
                case 0x0b:
                case 0x0c:
                case 0x0d:
                case 0x0e:
                case 0x0f:
                    if ((Disabled & 0x1) != 0)
                        setPalette(Index, (byte) val);
                    /*
                     * 0-5 Index into the 256 color DAC table. May be modified by 3C0h index 10h and
                     * 14h.
                     */
                    break;
                case 0x10: /* Mode Control Register */
                    if (!DOSBox.isVGAArch())
                        val &= 0x1f; // not really correct, but should do it
                    if (((ModeControl ^ val) & 0x80) != 0) {
                        ModeControl ^= 0x80;
                        for (byte i = 0; i < 0x10; i++) {
                            setPalette(i, Palette[i]);
                        }
                    }
                    if (((ModeControl ^ val) & 0x08) != 0) {
                        _vga.setBlinking(val & 0x8);
                    }
                    if (((ModeControl ^ val) & 0x04) != 0) {
                        ModeControl = (byte) val;
                        _vga.determineMode();
                        if ((DOSBox.isVGAArch()) && (DOSBox.SVGACard == DOSBox.SVGACards.None))
                            _vga.startResize();
                    } else {
                        ModeControl = (byte) val;
                        _vga.determineMode();
                    }

                    /*
                     * 0 Graphics mode if set, Alphanumeric mode else. 1 Monochrome mode if set,
                     * color mode else. 2 9-bit wide characters if set. The 9th bit of characters
                     * C0h-DFh will be the same as the 8th bit. Otherwise it will be the background
                     * color. 3 If set Attribute bit 7 is blinking, else high intensity. 5 If set
                     * the PEL panning register (3C0h index 13h) is temporarily set to 0 from when
                     * the line compare causes a wrap around until the next vertical retrace when
                     * the register is automatically reloaded with the old value, else the PEL
                     * panning register ignores line compares. 6 If set pixels are 8 bits wide. Used
                     * in 256 color modes. 7 If set bit 4-5 of the index into the DAC table are
                     * taken from port 3C0h index 14h bit 0-1, else the bits in the palette register
                     * are used.
                     */
                    break;
                case 0x11: /* Overscan Color Register */
                    OverscanColor = (byte) val;
                    /* 0-5 Color of screen border. Color is defined as in the palette registers. */
                    break;
                case 0x12: /* Color Plane Enable Register */
                    /* Why disable colour planes? */
                    ColorPlaneEnable = (byte) val;
                    /*
                     * 0 Bit plane 0 is enabled if set. 1 Bit plane 1 is enabled if set. 2 Bit plane
                     * 2 is enabled if set. 3 Bit plane 3 is enabled if set. 4-5 Video Status MUX.
                     * Diagnostics use only. Two attribute bits appear on bits 4 and 5 of the Input
                     * Status Register 1 (3dAh). 0: Bit 2/0, 1: Bit 5/4, 2: bit 3/1, 3: bit 7/6
                     */
                    break;
                case 0x13: /* Horizontal PEL Panning Register */
                    HorizontalPelPanning = val & 0xF;
                    switch (_vga.Mode) {
                        case TEXT:
                            if ((val == 0x7) && (DOSBox.SVGACard == DOSBox.SVGACards.None))
                                _vga.Config.PelPanning = 7;
                            if (val > 0x7)
                                _vga.Config.PelPanning = 0;
                            else
                                _vga.Config.PelPanning = val + 1;
                            break;
                        case VGA:
                        case LIN8:
                            _vga.Config.PelPanning = (val & 0x7) / 2;
                            break;
                        case LIN16:
                        default:
                            _vga.Config.PelPanning = val & 0x7;
                            break;
                    }
                    /*
                     * 0-3 Indicates number of pixels to shift the display left Value 9bit textmode
                     * 256color mode Other modes 0 1 0 0 1 2 n/a 1 2 3 1 2 3 4 n/a 3 4 5 2 4 5 6 n/a
                     * 5 6 7 3 6 7 8 n/a 7 8 0 n/a n/a
                     */
                    break;
                case 0x14: /* Color Select Register */
                    if (!DOSBox.isVGAArch()) {
                        ColorSelect = 0;
                        break;
                    }
                    if ((ColorSelect ^ val) != 0) {
                        ColorSelect = (byte) val;
                        for (byte i = 0; i < 0x10; i++) {
                            setPalette(i, Palette[i]);
                        }
                    }
                    /*
                     * 0-1 If 3C0h index 10h bit 7 is set these 2 bits are used as bits 4-5 of the
                     * index into the DAC table. 2-3 These 2 bits are used as bit 6-7 of the index
                     * into the DAC table except in 256 color mode. Note: this register does not
                     * affect 256 color modes.
                     */
                    break;
                default:
                    if (_vga.SVGADrv.WriteP3C0 != null) {
                        _vga.SVGADrv.WriteP3C0.exec(Index, val, iolen);
                        break;
                    }
                    Log.logging(Log.LogTypes.VGAMISC, Log.LogServerities.Normal,
                            "VGA:ATTR:Write to unkown Index %2X", Index);
                    break;
            }
        }
    }

    private int readP3C1(int port, int iolen) {
        // vga._internal.attrindex=false;
        switch (Index) {
            /* Palette */
            case 0x00:
            case 0x01:
            case 0x02:
            case 0x03:
            case 0x04:
            case 0x05:
            case 0x06:
            case 0x07:
            case 0x08:
            case 0x09:
            case 0x0a:
            case 0x0b:
            case 0x0c:
            case 0x0d:
            case 0x0e:
            case 0x0f:
                return Palette[Index];
            case 0x10: /* Mode Control Register */
                return ModeControl;
            case 0x11: /* Overscan Color Register */
                return OverscanColor;
            case 0x12: /* Color Plane Enable Register */
                return ColorPlaneEnable;
            case 0x13: /* Horizontal PEL Panning Register */
                return HorizontalPelPanning;
            case 0x14: /* Color Select Register */
                return ColorSelect;
            default:
                if (_vga.SVGADrv.ReadP3C1 != null)
                    return _vga.SVGADrv.ReadP3C1.exec(Index, iolen);
                Log.logging(Log.LogTypes.VGAMISC, Log.LogServerities.Normal,
                        "VGA:ATTR:Read from unkown Index %2X", Index);
                break;
        }
        return 0;
    }


    public void setup() {
        if (DOSBox.isEGAVGAArch()) {
            IO.registerWriteHandler(0x3c0, this::writeP3C0, IO.IO_MB);
            if (DOSBox.isVGAArch()) {
                IO.registerReadHandler(0x3c0, this::readP3C0, IO.IO_MB);
                IO.registerReadHandler(0x3c1, this::readP3C1, IO.IO_MB);
            }
        }
    }

}
