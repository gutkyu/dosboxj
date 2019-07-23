package org.gutkyu.dosboxj.hardware.video;

import org.gutkyu.dosboxj.*;
import org.gutkyu.dosboxj.hardware.*;
import org.gutkyu.dosboxj.hardware.io.iohandler.ReadHandler;
import org.gutkyu.dosboxj.hardware.io.iohandler.WriteHandler;
import org.gutkyu.dosboxj.util.*;

final class VGACrtc {
    public int HorizontalTotal;// byte
    public int HorizontalDisplayEnd;// byte
    public int StartHorizontalBlanking;// byte
    public int EndHorizontalBlanking;// byte
    public int StartHorizontalRetrace;// byte
    public int EndHorizontalRetrace;// byte
    public int VerticalTotal;// byte
    public int Overflow;// byte
    public int PresetRowScan;// byte
    public int MaximumScanLine;// byte
    public int CursorStart;// byte
    public int CursorEnd;// byte
    public int StartAddressHigh;// byte
    public int StartAddressLow;// byte
    public int CursorLocationHigh;// byte
    public int CursorLocationLow;// byte
    public int VerticalRetraceStart;// byte
    public int VerticalRetraceEnd;// byte
    public int VerticalDisplayEnd;// byte
    public int Offset;// byte
    public int UnderlineLocation;// byte
    public int StartVerticalBlanking;// byte
    public int EndVerticalBlanking;// byte
    public int ModeControl;// byte
    public int LineCompare;// byte

    public int Index;// byte
    public boolean ReadOnly;

    private VGA _vga;

    protected VGACrtc(VGA vga) {
        this._vga = vga;
    }

    /*--------------------------- begin VGACrtc -----------------------------*/
    protected WriteHandler writeP3D4Wrap = this::writeP3D4;

    protected void writeP3D4(int port, int val, int iolen) {
        this.Index = val;
    }

    protected ReadHandler readP3D4Wrap = this::readP3D4;

    protected int readP3D4(int port, int iolen) {
        return this.Index;
    }

    protected WriteHandler writeP3D5Wrap = this::writeP3D5;

    protected void writeP3D5(int port, int val, int iolen) {
        // if (vga.crtc.index>0x18) LOG_MSG("VGA CRCT write %X to reg %X",val,vga.crtc.index);
        switch (this.Index) {
            case 0x00: /* Horizontal Total Register */
                if (this.ReadOnly)
                    break;
                this.HorizontalTotal = val;
                /* 0-7 Horizontal Total Character Clocks-5 */
                break;
            case 0x01: /* Horizontal Display End Register */
                if (this.ReadOnly)
                    break;
                if (val != this.HorizontalDisplayEnd) {
                    this.HorizontalDisplayEnd = val;
                    _vga.startResize();
                }
                /* 0-7 Number of Character Clocks Displayed -1 */
                break;
            case 0x02: /* Start Horizontal Blanking Register */
                if (this.ReadOnly)
                    break;
                this.StartHorizontalBlanking = val;
                /* 0-7 The count at which Horizontal Blanking starts */
                break;
            case 0x03: /* End Horizontal Blanking Register */
                if (this.ReadOnly)
                    break;
                this.EndHorizontalBlanking = val;
                /*
                 * 0-4 Horizontal Blanking ends when the last 6 bits of the character counter equals
                 * this field. Bit 5 is at 3d4h index 5 bit 7. 5-6 Number of character clocks to
                 * delay start of display after Horizontal Total has been reached. 7 Access to
                 * Vertical Retrace registers if set. If clear reads to 3d4h index 10h and 11h
                 * access the Lightpen read back registers ??
                 */
                break;
            case 0x04: /* Start Horizontal Retrace Register */
                if (this.ReadOnly)
                    break;
                this.StartHorizontalRetrace = val;
                /* 0-7 Horizontal Retrace starts when the Character Counter reaches this value. */
                break;
            case 0x05: /* End Horizontal Retrace Register */
                if (this.ReadOnly)
                    break;
                this.EndHorizontalRetrace = val;
                /*
                 * 0-4 Horizontal Retrace ends when the last 5 bits of the character counter equals
                 * this value. 5-6 Number of character clocks to delay start of display after
                 * Horizontal Retrace. 7 bit 5 of the End Horizontal Blanking count (See 3d4h index
                 * 3 bit 0-4)
                 */
                break;
            case 0x06: /* Vertical Total Register */
                if (this.ReadOnly)
                    break;
                if (val != this.VerticalTotal) {
                    this.VerticalTotal = val;
                    _vga.startResize();
                }
                /*
                 * 0-7 Lower 8 bits of the Vertical Total. Bit 8 is found in 3d4h index 7 bit 0. Bit
                 * 9 is found in 3d4h index 7 bit 5. Note: For the VGA this value is the number of
                 * scan lines in the display -2.
                 */
                break;
            case 0x07: /* Overflow Register */
                // Line compare bit ignores read only */
                _vga.Config.LineCompare = (_vga.Config.LineCompare & 0x6ff) | (val & 0x10) << 4;
                if (this.ReadOnly)
                    break;
                if (((this.Overflow ^ val) & 0xd6) != 0) {
                    this.Overflow = val;
                    _vga.startResize();
                } else
                    this.Overflow = val;
                /*
                 * 0 Bit 8 of Vertical Total (3d4h index 6) 1 Bit 8 of Vertical Display End (3d4h
                 * index 12h) 2 Bit 8 of Vertical Retrace Start (3d4h index 10h) 3 Bit 8 of Start
                 * Vertical Blanking (3d4h index 15h) 4 Bit 8 of Line Compare Register (3d4h index
                 * 18h) 5 Bit 9 of Vertical Total (3d4h index 6) 6 Bit 9 of Vertical Display End
                 * (3d4h index 12h) 7 Bit 9 of Vertical Retrace Start (3d4h index 10h)
                 */
                break;
            case 0x08: /* Preset Row Scan Register */
                this.PresetRowScan = val;
                _vga.Config.HLinesSkip = val & 31;
                if (DOSBox.isVGAArch())
                    _vga.Config.BytesSkip = (val >>> 5) & 3;
                else
                    _vga.Config.BytesSkip = 0;
                // LOG_DEBUG("Skip lines %d bytes %d",vga.config.hlines_skip,vga.config.bytes_skip);
                /*
                 * 0-4 Number of lines we have scrolled down in the first character row. Provides
                 * Smooth Vertical Scrolling.b 5-6 Number of bytes to skip at the start of scanline.
                 * Provides Smooth Horizontal Scrolling together with the Horizontal Panning
                 * Register (3C0h index 13h).
                 */
                break;
            case 0x09: /* Maximum Scan Line Register */
                if (DOSBox.isVGAArch())
                    _vga.Config.LineCompare = (_vga.Config.LineCompare & 0x5ff) | (val & 0x40) << 3;

                if (DOSBox.isVGAArch() && (DOSBox.SVGACard == DOSBox.SVGACards.None)
                        && (_vga.Mode == VGAModes.EGA || _vga.Mode == VGAModes.VGA)) {
                    // in vgaonly mode we take special care of line repeats (excluding CGA modes)
                    if (((this.MaximumScanLine ^ val) & 0x20) != 0) {
                        this.MaximumScanLine = val;
                        _vga.startResize();
                    } else {
                        this.MaximumScanLine = val;
                    }
                    _vga.Draw.AddressLineTotal = (val & 0x1F) + 1;
                    if ((val & 0x80) != 0)
                        _vga.Draw.AddressLineTotal *= 2;
                } else {
                    if (((this.MaximumScanLine ^ val) & 0xbf) != 0) {
                        this.MaximumScanLine = val;
                        _vga.startResize();
                    } else {
                        this.MaximumScanLine = val;
                    }
                }
                /*
                 * 0-4 Number of scan lines in a character row -1. In graphics modes this is the
                 * number of times (-1) the line is displayed before passing on to the next line (0:
                 * normal, 1: double, 2: triple...). This is independent of bit 7, except in CGA
                 * modes which seems to require this field to be 1 and bit 7 to be set to work. 5
                 * Bit 9 of Start Vertical Blanking 6 Bit 9 of Line Compare Register 7 Doubles each
                 * scan line if set. I.e. displays 200 lines on a 400 display.
                 */
                break;
            case 0x0A: /* Cursor Start Register */
                this.CursorStart = val;
                _vga.Draw.Cursor.SLine = val & 0x1f;
                if (DOSBox.isVGAArch())
                    _vga.Draw.Cursor.Enabled = (val & 0x20) == 0 ? 1 : 0;
                else
                    _vga.Draw.Cursor.Enabled = 1;
                /*
                 * 0-4 First scanline of cursor within character. 5 Turns Cursor off if set
                 */
                break;
            case 0x0B: /* Cursor End Register */
                this.CursorEnd = val;
                _vga.Draw.Cursor.ELine = val & 0x1f;
                _vga.Draw.Cursor.Delay = (val >>> 5) & 0x3;

                /*
                 * 0-4 Last scanline of cursor within character 5-6 Delay of cursor data in
                 * character clocks.
                 */
                break;
            case 0x0C: /* Start Address High Register */
                this.StartAddressHigh = val;
                _vga.Config.DisplayStart = (_vga.Config.DisplayStart & 0xFF00FF) | (val << 8);
                /* 0-7 Upper 8 bits of the start address of the display buffer */
                break;
            case 0x0D: /* Start Address Low Register */
                this.StartAddressLow = val;
                _vga.Config.DisplayStart = (_vga.Config.DisplayStart & 0xFFFF00) | val;
                /* 0-7 Lower 8 bits of the start address of the display buffer */
                break;
            case 0x0E: /* Cursor Location High Register */
                this.CursorLocationHigh = val;
                _vga.Config.CursorStart &= 0xff00ff;
                _vga.Config.CursorStart |= val << 8;
                /* 0-7 Upper 8 bits of the address of the cursor */
                break;
            case 0x0F: /* Cursor Location Low Register */
                // TODO update cursor on screen
                this.CursorLocationLow = val;
                _vga.Config.CursorStart &= 0xffff00;
                _vga.Config.CursorStart |= val;
                /* 0-7 Lower 8 bits of the address of the cursor */
                break;
            case 0x10: /* Vertical Retrace Start Register */
                this.VerticalRetraceStart = val;
                /*
                 * 0-7 Lower 8 bits of Vertical Retrace Start. Vertical Retrace starts when the line
                 * counter reaches this value. Bit 8 is found in 3d4h index 7 bit 2. Bit 9 is found
                 * in 3d4h index 7 bit 7.
                 */
                break;
            case 0x11: /* Vertical Retrace End Register */
                this.VerticalRetraceEnd = val;

                if (DOSBox.isEGAVGAArch() && (val & 0x10) == 0) {
                    _vga.Draw.VRetTriggered = false;
                    if (DOSBox.Machine == DOSBox.MachineType.EGA)
                        PIC.deactivateIRQ(9);
                }
                if (DOSBox.isVGAArch())
                    this.ReadOnly = (val & 128) > 0;
                else
                    this.ReadOnly = false;
                /*
                 * 0-3 Vertical Retrace ends when the last 4 bits of the line counter equals this
                 * value. 4 if clear Clears pending Vertical Interrupts. 5 Vertical Interrupts (IRQ
                 * 2) disabled if set. Can usually be left disabled, but some systems (including
                 * PS/2) require it to be enabled. 6 If set selects 5 refresh cycles per scanline
                 * rather than 3. 7 Disables writing to registers 0-7 if set 3d4h index 7 bit 4 is
                 * not affected by this bit.
                 */
                break;
            case 0x12: /* Vertical Display End Register */
                if (val != this.VerticalDisplayEnd) {
                    if (Math.abs(val - this.VerticalDisplayEnd) < 3) {
                        // delay small vde changes a bit to avoid screen resizing
                        // if they are reverted in a short timeframe
                        PIC.removeEvents(_vga.setupDrawingWrap);
                        _vga.Draw.Resizing = false;
                        this.VerticalDisplayEnd = val;
                        _vga.startResize(150);
                    } else {
                        this.VerticalDisplayEnd = val;
                        _vga.startResize();
                    }
                }
                /*
                 * 0-7 Lower 8 bits of Vertical Display End. The display ends when the line counter
                 * reaches this value. Bit 8 is found in 3d4h index 7 bit 1. Bit 9 is found in 3d4h
                 * index 7 bit 6.
                 */
                break;
            case 0x13: /* Offset register */
                this.Offset = val;
                _vga.Config.ScanLen &= 0x300;
                _vga.Config.ScanLen |= val;
                _vga.checkScanLength();
                /*
                 * 0-7 Number of bytes in a scanline / K. Where K is 2 for byte mode, 4 for word
                 * mode and 8 for Double Word mode.
                 */
                break;
            case 0x14: /* Underline Location Register */
                this.UnderlineLocation = val;
                if (DOSBox.isVGAArch()) {
                    // Byte,word,dword mode
                    if ((this.UnderlineLocation & 0x20) != 0)
                        _vga.Config.AddrShift = 2;
                    else if ((this.ModeControl & 0x40) != 0)
                        _vga.Config.AddrShift = 0;
                    else
                        _vga.Config.AddrShift = 1;
                } else {
                    _vga.Config.AddrShift = 1;
                }
                /*
                 * 0-4 Position of underline within Character cell. 5 If set memory address is only
                 * changed every fourth character clock. 6 Double Word mode addressing if set
                 */
                break;
            case 0x15: /* Start Vertical Blank Register */
                if (val != this.StartVerticalBlanking) {
                    this.StartVerticalBlanking = val;
                    _vga.startResize();
                }
                /*
                 * 0-7 Lower 8 bits of Vertical Blank Start. Vertical blanking starts when the line
                 * counter reaches this value. Bit 8 is found in 3d4h index 7 bit 3.
                 */
                break;
            case 0x16: /* End Vertical Blank Register */
                this.EndVerticalBlanking = val;
                /*
                 * 0-6 Vertical blanking stops when the lower 7 bits of the line counter equals this
                 * field. Some SVGA chips uses all 8 bits!
                 */
                break;
            case 0x17: /* Mode Control Register */
                this.ModeControl = val;
                _vga.Tandy.LineMask = (byte) ((~val) & 3);
                // Byte,word,dword mode
                if ((this.UnderlineLocation & 0x20) != 0)
                    _vga.Config.AddrShift = 2;
                else if ((this.ModeControl & 0x40) != 0)
                    _vga.Config.AddrShift = 0;
                else
                    _vga.Config.AddrShift = 1;

                if (_vga.Tandy.LineMask != 0) {
                    _vga.Tandy.LineShift = 13;
                    _vga.Tandy.AddrMask = (1 << 13) - 1;
                } else {
                    _vga.Tandy.AddrMask = ~0;
                    _vga.Tandy.LineShift = 0;
                }
                // Should we really need to do a determinemode here?
                // VGA_DetermineMode();
                /*
                 * 0 If clear use CGA compatible memory addressing system by substituting character
                 * row scan counter bit 0 for address bit 13, thus creating 2 banks for even and odd
                 * scan lines. 1 If clear use Hercules compatible memory addressing system by
                 * substituting character row scan counter bit 1 for address bit 14, thus creating 4
                 * banks. 2 If set increase scan line counter only every second line. 3 If set
                 * increase memory address counter only every other character clock. 5 When in Word
                 * Mode bit 15 is rotated to bit 0 if this bit is set else bit 13 is rotated into
                 * bit 0. 6 If clear system is in word mode. Addresses are rotated 1 position up
                 * bringing either bit 13 or 15 into bit 0. 7 Clearing this bit will reset the
                 * display system until the bit is set again.
                 */
                break;
            case 0x18: /* Line Compare Register */
                this.LineCompare = val;
                _vga.Config.LineCompare = (_vga.Config.LineCompare & 0x700) | val;
                /*
                 * 0-7 Lower 8 bits of the Line Compare. When the Line counter reaches this value,
                 * the display address wraps to 0. Provides Split Screen facilities. Bit 8 is found
                 * in 3d4h index 7 bit 4. Bit 9 is found in 3d4h index 9 bit 6.
                 */
                break;
            default:
                if (_vga.SVGADrv.WriteP3D5 != null) {
                    _vga.SVGADrv.WriteP3D5.exec(this.Index, val, iolen);
                } else {
                    Log.logging(Log.LogTypes.VGAMISC, Log.LogServerities.Normal,
                            "VGA:CRTC:Write to unknown index %X", this.Index);
                }
                break;
        }
    }

    protected ReadHandler readP3D5Wrap = this::readP3D5;

    protected int readP3D5(int port, int iolen) {
        // LOG_MSG("VGA CRCT read from reg %X",vga.crtc.index);
        switch (this.Index) {
            case 0x00: /* Horizontal Total Register */
                return this.HorizontalTotal;
            case 0x01: /* Horizontal Display End Register */
                return this.HorizontalDisplayEnd;
            case 0x02: /* Start Horizontal Blanking Register */
                return this.StartHorizontalBlanking;
            case 0x03: /* End Horizontal Blanking Register */
                return this.EndHorizontalBlanking;
            case 0x04: /* Start Horizontal Retrace Register */
                return this.StartHorizontalRetrace;
            case 0x05: /* End Horizontal Retrace Register */
                return this.EndHorizontalRetrace;
            case 0x06: /* Vertical Total Register */
                return this.VerticalTotal;
            case 0x07: /* Overflow Register */
                return this.Overflow;
            case 0x08: /* Preset Row Scan Register */
                return this.PresetRowScan;
            case 0x09: /* Maximum Scan Line Register */
                return this.MaximumScanLine;
            case 0x0A: /* Cursor Start Register */
                return this.CursorStart;
            case 0x0B: /* Cursor End Register */
                return this.CursorEnd;
            case 0x0C: /* Start Address High Register */
                return this.StartAddressHigh;
            case 0x0D: /* Start Address Low Register */
                return this.StartAddressLow;
            case 0x0E: /* Cursor Location High Register */
                return this.CursorLocationHigh;
            case 0x0F: /* Cursor Location Low Register */
                return this.CursorLocationLow;
            case 0x10: /* Vertical Retrace Start Register */
                return this.VerticalRetraceStart;
            case 0x11: /* Vertical Retrace End Register */
                return this.VerticalRetraceEnd;
            case 0x12: /* Vertical Display End Register */
                return this.VerticalDisplayEnd;
            case 0x13: /* Offset register */
                return this.Offset;
            case 0x14: /* Underline Location Register */
                return this.UnderlineLocation;
            case 0x15: /* Start Vertical Blank Register */
                return this.StartVerticalBlanking;
            case 0x16: /* End Vertical Blank Register */
                return this.EndVerticalBlanking;
            case 0x17: /* Mode Control Register */
                return this.ModeControl;
            case 0x18: /* Line Compare Register */
                return this.LineCompare;
            default:
                if (_vga.SVGADrv.ReadP3D5 != null) {
                    return _vga.SVGADrv.ReadP3D5.exec(this.Index, iolen);
                } else {
                    Log.logging(Log.LogTypes.VGAMISC, Log.LogServerities.Normal,
                            "VGA:CRTC:Read from unknown index %X", this.Index);
                    return 0x0;
                }
        }
    }

    /*--------------------------- end VGACrtc -----------------------------*/
}
