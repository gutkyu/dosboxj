package org.gutkyu.dosboxj.hardware.video.svga;

import org.gutkyu.dosboxj.util.*;
import org.gutkyu.dosboxj.hardware.io.IO;
import org.gutkyu.dosboxj.hardware.memory.*;
import org.gutkyu.dosboxj.hardware.video.VGA;
import org.gutkyu.dosboxj.hardware.video.VGAModes;
import org.gutkyu.dosboxj.interrupt.int10.INT10Mode;

public final class TsengET3KSVGADriverProvider {
    // Stored exact values of some registers. Documentation only specifies some bits but hardware
    // checks may
    // expect other bits to be preserved.
    public int _store_3d4_1b = 0;
    public int _store_3d4_1c = 0;
    public int _store_3d4_1d = 0;
    public int _store_3d4_1e = 0;
    public int _store_3d4_1f = 0;
    public int _store_3d4_20 = 0;
    public int _store_3d4_21 = 0;
    public int _store_3d4_23 = 0; // note that 22 is missing
    public int _store_3d4_24 = 0;
    public int _store_3d4_25 = 0;

    public int _store_3c0_16 = 0;
    public int _store_3c0_17 = 0;

    public int _store_3c4_06 = 0;
    public int _store_3c4_07 = 0;

    public int[] _clockFreq = new int[] {0, 0, 0, 0, 0, 0, 0, 0};
    public int _biosMode = 0;

    VGA _vga = null;

    public TsengET3KSVGADriverProvider(VGA vga) {
        _vga = vga;

        _vga.SVGADrv.WriteP3D5 = this::writeP3D5;
        _vga.SVGADrv.ReadP3D5 = this::readP3D5;
        _vga.SVGADrv.WriteP3C5 = this::writeP3C5;
        _vga.SVGADrv.ReadP3C5 = this::readP3C5;
        _vga.SVGADrv.WriteP3C0 = this::writeP3C0;
        _vga.SVGADrv.ReadP3C1 = this::readP3C1;

        _vga.SVGADrv.SetVideoMode = this::finishSetMode;
        _vga.SVGADrv.DetermineMode = this::determineMode;
        _vga.SVGADrv.SetClock = this::setClock;
        _vga.SVGADrv.GetClock = this::getClock;
        _vga.SVGADrv.AcceptsMode = this::acceptsMode;

        _vga.setClock(0, VGA.CLK_25);
        _vga.setClock(1, VGA.CLK_28);
        _vga.setClock(2, 32400);
        _vga.setClock(3, 35900);
        _vga.setClock(4, 39900);
        _vga.setClock(5, 44700);
        _vga.setClock(6, 31400);
        _vga.setClock(7, 37500);

        IO.registerReadHandler(0x3cd, this::readP3CD, IO.IO_MB);
        IO.registerWriteHandler(0x3cd, this::writeP3CD, IO.IO_MB);

        _vga.VMemSize = 512 * 1024; // Cannot figure how this was supposed to work on the real card

        // Tseng ROM signature
        int romBase = Memory.physMake(0xc000, 0);
        Memory.physWriteB(romBase + 0x0075, ' ');
        Memory.physWriteB(romBase + 0x0076, 'T');
        Memory.physWriteB(romBase + 0x0077, 's');
        Memory.physWriteB(romBase + 0x0078, 'e');
        Memory.physWriteB(romBase + 0x0079, 'n');
        Memory.physWriteB(romBase + 0x007a, 'g');
        Memory.physWriteB(romBase + 0x007b, ' ');
    }

    private void writeP3D5(int reg, int val, int iolen) {
        switch (reg) {
            // 3d4 index 1bh-21h: Hardware zoom control registers
            // I am not sure if there was a piece of software that used these.
            // Not implemented and will probably stay this way.
            // STORE_ET3K(3d4, 1b);
            case 0x1b:
                _store_3d4_1b = val;
                break;
            // STORE_ET3K(3d4, 1c);
            case 0x1c:
                _store_3d4_1c = val;
                break;
            // STORE_ET3K(3d4, 1d);
            case 0x1d:
                _store_3d4_1d = val;
                break;
            // STORE_ET3K(3d4, 1e);
            case 0x1e:
                _store_3d4_1e = val;
                break;
            // STORE_ET3K(3d4, 1f);
            case 0x1f:
                _store_3d4_1f = val;
                break;
            // STORE_ET3K(3d4, 20);
            case 0x20:
                _store_3d4_20 = val;
                break;
            // STORE_ET3K(3d4, 21);
            case 0x21:
                _store_3d4_21 = val;
                break;
            case 0x23:
                /*
                 * 3d4h index 23h (R/W): Extended start ET3000 bit 0 Cursor start address bit 16 1
                 * Display start address bit 16 2 Zoom start address bit 16 7 If set memory address
                 * 8 is output on the MBSL pin (allowing access to 1MB), if clear the blanking
                 * signal is output.
                 */
                // Only bits 1 and 2 are supported. Bit 2 is related to hardware zoom, bit 7 is too
                // obscure to be useful
                _store_3d4_23 = val;
                _vga.Config.DisplayStart =
                        (_vga.Config.DisplayStart & 0xffff) | ((val & 0x02) << 15);
                _vga.Config.CursorStart = (_vga.Config.CursorStart & 0xffff) | ((val & 0x01) << 16);
                break;


            /*
             * 3d4h index 24h (R/W): Compatibility Control bit 0 Enable Clock Translate if set 1
             * Clock Select bit 2. int 0-1 are in 3C2h/3CCh. 2 Enable tri-state for all output pins
             * if set 3 Enable input A8 of 1MB DRAMs from the INTL output if set 4 Reserved 5 Enable
             * external ROM CRTC translation if set 6 Enable Double Scan and Underline Attribute if
             * set 7 Enable 6845 compatibility if set.
             */
            // TODO: Some of these may be worth implementing.
            // STORE_ET3K(3d4, 24);
            case 0x24:
                _store_3d4_24 = val;
                break;
            case 0x25:
                /*
                 * 3d4h index 25h (R/W): Overflow High bit 0 Vertical Blank Start bit 10 1 Vertical
                 * Total Start bit 10 2 Vertical Display End bit 10 3 Vertical Sync Start bit 10 4
                 * Line Compare bit 10 5-6 Reserved 7 Vertical Interlace if set
                 */
                _store_3d4_25 = val;
                _vga.Config.LineCompare = (_vga.Config.LineCompare & 0x3ff) | ((val & 0x10) << 6);
            // Abusing s3 ex_ver_overflow field. This is to be cleaned up later.
            {
                byte s3val = (byte) (((val & 0x01) << 2) | // vbstart
                        ((val & 0x02) >>> 1) | // vtotal
                        ((val & 0x04) >>> 1) | // vdispend
                        ((val & 0x08) << 1) | // vsyncstart (?)
                        ((val & 0x10) << 2)); // linecomp
                if (((s3val ^ _vga.S3.ExVerOverflow) & 0x3) != 0) {
                    _vga.S3.ExVerOverflow = s3val;
                    _vga.startResize();
                } else
                    _vga.S3.ExVerOverflow = s3val;
            }
                break;

            default:
                Log.logging(Log.LogTypes.VGAMISC, Log.LogServerities.Normal,
                        "VGA:CRTC:ET3K:Write to illegal index %2X", reg);
                break;
        }
    }

    private int readP3D5(int reg, int iolen) {
        switch (reg) {
            // RESTORE_ET3K(3d4, 1b);
            case 0x1b:
                return _store_3d4_1b;

            // RESTORE_ET3K(3d4, 1c);
            case 0x1c:
                return _store_3d4_1c;
            // RESTORE_ET3K(3d4, 1d);
            case 0x1d:
                return _store_3d4_1d;
            // RESTORE_ET3K(3d4, 1e);
            case 0x1e:
                return _store_3d4_1e;
            // RESTORE_ET3K(3d4, 1f);
            case 0x1f:
                return _store_3d4_1f;
            // RESTORE_ET3K(3d4, 20);
            case 0x20:
                return _store_3d4_20;
            // RESTORE_ET3K(3d4, 21);
            case 0x21:
                return _store_3d4_21;
            // RESTORE_ET3K(3d4, 23);
            case 0x23:
                return _store_3d4_23;
            // RESTORE_ET3K(3d4, 24);
            case 0x24:
                return _store_3d4_24;
            // RESTORE_ET3K(3d4, 25);
            case 0x25:
                return _store_3d4_25;
            default:
                Log.logging(Log.LogTypes.VGAMISC, Log.LogServerities.Normal,
                        "VGA:CRTC:ET3K:Read from illegal index %2X", reg);
                break;
        }
        return 0x0;
    }

    private void writeP3C5(int reg, int val, int iolen) {
        switch (reg) {
            // Both registers deal mostly with hardware zoom which is not implemented. Other bits
            // seem to be useless for emulation with the exception of index 7 bit 4 (font select)
            // STORE_ET3K(3c4, 06);
            case 0x06:
                _store_3c4_06 = val;
                break;
            // STORE_ET3K(3c4, 07);
            case 0x07:
                _store_3c4_07 = val;
                break;
            default:
                Log.logging(Log.LogTypes.VGAMISC, Log.LogServerities.Normal,
                        "VGA:SEQ:ET3K:Write to illegal index %2X", reg);
                break;
        }
    }

    private int readP3C5(int reg, int iolen) {
        switch (reg) {
            // RESTORE_ET3K(3c4, 06);
            case 0x06:
                return _store_3c4_06;
            // RESTORE_ET3K(3c4, 07);
            case 0x07:
                return _store_3c4_07;
            default:
                Log.logging(Log.LogTypes.VGAMISC, Log.LogServerities.Normal,
                        "VGA:SEQ:ET3K:Read from illegal index %2X", reg);
                break;
        }
        return 0x0;
    }

    /*
     * 3CDh (R/W): Segment Select bit 0-2 64k Write bank number 3-5 64k Read bank number 6-7 Segment
     * Configuration. 0 128K segments 1 64K segments 2 1M linear memory NOTES: 1M linear memory is
     * not supported
     */
    private void writeP3CD(int port, int val, int iolen) {
        _vga.SVGA.BankWrite = (byte) (val & 0x07);
        _vga.SVGA.BankRead = (byte) ((val >>> 3) & 0x07);
        _vga.SVGA.BankSize = (val & 0x40) != 0 ? 64 * 1024 : 128 * 1024;
        _vga.setupHandlers();
    }

    private int readP3CD(int port, int iolen) {
        return (int) (_vga.SVGA.BankRead << 3) | _vga.SVGA.BankWrite
                | ((_vga.SVGA.BankSize == 128 * 1024) ? 0 : 0x40);
    }

    private void writeP3C0(int reg, int val, int iolen) {
        // See ET4K notes.
        switch (reg) {
            // STORE_ET3K(3c0, 16);
            case 0x16:
                _store_3c0_16 = val;
                break;
            // STORE_ET3K(3c0, 17);
            case 0x17:
                _store_3c0_17 = val;
                break;
            default:
                Log.logging(Log.LogTypes.VGAMISC, Log.LogServerities.Normal,
                        "VGA:ATTR:ET3K:Write to illegal index %2X", reg);
                break;
        }
    }

    private int readP3C1(int reg, int iolen) {
        switch (reg) {
            // RESTORE_ET3K(3c0, 16);
            case 0x16:
                return _store_3c0_16;
            // RESTORE_ET3K(3c0, 17);
            case 0x17:
                return _store_3c0_17;
            default:
                Log.logging(Log.LogTypes.VGAMISC, Log.LogServerities.Normal,
                        "VGA:ATTR:ET3K:Read from illegal index %2X", reg);
                break;
        }
        return 0x0;
    }

    /*
     * These ports are used but have little if any effect on emulation: 3B8h (W): Display Mode
     * Control Register 3BFh (R/W): Hercules Compatibility Mode 3CBh (R/W): PEL Address/Data Wd 3CEh
     * index 0Dh (R/W): Microsequencer Mode 3CEh index 0Eh (R/W): Microsequencer Reset 3d8h (R/W):
     * Display Mode Control 3D9h (W): Color Select Register 3dAh (W): Feature Control Register 3DEh
     * (W); AT&T Mode Control Register
     */

    private int getClockIndex() {
        return ((_vga.MiscOutput >>> 2) & 3) | ((_store_3d4_24 << 1) & 4);
    }

    private void setClockIndex(int index) {
        // Shortwiring register reads/writes for simplicity
        IO.write(0x3c2, 0xff & (0xff & (_vga.MiscOutput & ~0x0c) | ((index & 3) << 2)));
        _store_3d4_24 = (_store_3d4_24 & ~0x02) | ((index & 4) >>> 1);
    }

    private void finishSetMode(int crtcBase, ModeExtraData modeData) {
        _biosMode = modeData.ModeNo;

        IO.write(0x3cd, 0x40); // both banks to 0, 64K bank size

        // Tseng ET3K does not have horizontal overflow bits
        // Reinterpret ver_overflow
        byte et4k_ver_overflow = (byte) (((modeData.VOverflow & 0x01) << 1) | // vtotal10
                ((modeData.VOverflow & 0x02) << 1) | // vdispend10
                ((modeData.VOverflow & 0x04) >>> 2) | // vbstart10
                ((modeData.VOverflow & 0x10) >>> 1) | // vretrace10 (tseng has vsync start?)
                ((modeData.VOverflow & 0x40) >>> 2)); // line_compare
        IO.write(crtcBase, 0x25);
        IO.write(crtcBase + 1, et4k_ver_overflow);

        // Clear remaining ext CRTC registers
        for (int i = 0x16; i <= 0x21; i++)
            IO.write(crtcBase, (byte) i);
        IO.write(crtcBase + 1, 0);
        IO.write(crtcBase, 0x23);
        IO.write(crtcBase + 1, 0);
        IO.write(crtcBase, 0x24);
        IO.write(crtcBase + 1, 0);
        // Clear ext SEQ
        IO.write(0x3c4, 0x06);
        IO.write(0x3c5, 0);
        IO.write(0x3c4, 0x07);
        IO.write(0x3c5, 0x40); // 0 in this register breaks WHATVGA
        // Clear ext ATTR
        IO.write(0x3c0, 0x16);
        IO.write(0x3c0, 0);
        IO.write(0x3c0, 0x17);
        IO.write(0x3c0, 0);

        // Select _vga.SVGA clock to get close to 60Hz (not particularly clean implementation)
        if (modeData.ModeNo > 0x13) {
            int target = modeData.VTotal * 8 * modeData.HTotal * 60;
            int best = 1;
            int dist = 100000000;
            for (int i = 0; i < 8; i++) {
                int cdiff = Math.abs((int) (target - _clockFreq[i]));
                if (cdiff < dist) {
                    best = i;
                    dist = cdiff;
                }
            }
            setClockIndex(best);
        }

        if (_vga.SVGADrv.DetermineMode != null)
            _vga.SVGADrv.DetermineMode.exec();

        // Verified on functioning (at last!) hardware: Tseng ET3000 is the same as ET4000 when
        // it comes to chain4 architecture
        _vga.Config.CompatibleChain4 = false;
        _vga.VMemWrap = _vga.VMemSize;

        _vga.setupHandlers();
    }

    private void determineMode() {
        // Close replica from the base implementation. It will stay here
        // until I figure a way to either distinguish VGAModes.M_VGA and VGAModes.M_LIN8 or
        // merge them.
        if ((_vga.Attr.ModeControl & 1) != 0) {
            if ((_vga.GFX.Mode & 0x40) != 0)
                _vga.setMode((_biosMode <= 0x13) ? VGAModes.VGA : VGAModes.LIN8); // Ugly...
            else if ((_vga.GFX.Mode & 0x20) != 0)
                _vga.setMode(VGAModes.CGA4);
            else if ((_vga.GFX.Miscellaneous & 0x0c) == 0x0c)
                _vga.setMode(VGAModes.CGA2);
            else
                _vga.setMode((_biosMode <= 0x13) ? VGAModes.EGA : VGAModes.LIN4);
        } else {
            _vga.setMode(VGAModes.TEXT);
        }
    }

    private void setClock(int which, int target) {
        _clockFreq[which] = 1000 * target;
        _vga.startResize();
    }

    private int getClock() {
        return _clockFreq[getClockIndex()];
    }

    private boolean acceptsMode(int mode) {
        return mode <= 0x37 && mode != 0x2f && INT10Mode.videoModeMemSize(mode) < _vga.VMemSize;
    }
}
