package org.gutkyu.dosboxj.hardware.video.svga;

import org.gutkyu.dosboxj.util.*;
import org.gutkyu.dosboxj.hardware.io.IO;
import org.gutkyu.dosboxj.hardware.memory.*;
import org.gutkyu.dosboxj.hardware.video.VGA;
import org.gutkyu.dosboxj.hardware.video.VGAModes;
import org.gutkyu.dosboxj.interrupt.int10.INT10Mode;

public final class TsengET4KSVGADriverProvider {
    public byte extensionsEnabled;

    // Stored exact values of some registers. Documentation only specifies some bits but hardware
    // checks may
    // expect other bits to be preserved.
    public int _store_3d4_31 = 0;
    public int _store_3d4_32 = 0;
    public int _store_3d4_33 = 0;
    public int _store_3d4_34 = 0;
    public int _store_3d4_35 = 0;
    public int _store_3d4_36 = 0;
    public int _store_3d4_37 = 0;
    public int _store_3d4_3f = 0;

    public int _store_3c0_16 = 0;
    public int _store_3c0_17 = 0;

    public int _store_3c4_06 = 0;
    public int _store_3c4_07 = 0;

    public int[] _clockFreq = new int[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    public int _biosMode = 0;

    private VGA vga = null;

    public TsengET4KSVGADriverProvider(VGA vga) {
        this.vga = vga;

        vga.SVGADrv.WriteP3D5 = this::writeP3D5;
        vga.SVGADrv.ReadP3D5 = this::readP3D5;
        vga.SVGADrv.WriteP3C5 = this::writeP3C5;
        vga.SVGADrv.ReadP3C5 = this::readP3C5;
        vga.SVGADrv.WriteP3C0 = this::writeP3C0;
        vga.SVGADrv.ReadP3C1 = this::readP3C1;

        vga.SVGADrv.SetVideoMode = this::finishSetMode;
        vga.SVGADrv.DetermineMode = this::determineMode;
        vga.SVGADrv.SetClock = this::setClock;
        vga.SVGADrv.GetClock = this::getClock;
        vga.SVGADrv.AcceptsMode = this::acceptsMode;

        // From the depths of X86Config, probably inexact
        vga.setClock(0, VGA.CLK_25);
        vga.setClock(1, VGA.CLK_28);
        vga.setClock(2, 32400);
        vga.setClock(3, 35900);
        vga.setClock(4, 39900);
        vga.setClock(5, 44700);
        vga.setClock(6, 31400);
        vga.setClock(7, 37500);
        vga.setClock(8, 50000);
        vga.setClock(9, 56500);
        vga.setClock(10, 64900);
        vga.setClock(11, 71900);
        vga.setClock(12, 79900);
        vga.setClock(13, 89600);
        vga.setClock(14, 62800);
        vga.setClock(15, 74800);

        IO.registerReadHandler(0x3cd, this::readP3CD, IO.IO_MB);
        IO.registerWriteHandler(0x3cd, this::writeP3CD, IO.IO_MB);

        // Default to 1M of VRAM
        if (vga.VMemSize == 0)
            vga.VMemSize = 1024 * 1024;

        if (vga.VMemSize < 512 * 1024)
            vga.VMemSize = 256 * 1024;
        else if (vga.VMemSize < 1024 * 1024)
            vga.VMemSize = 512 * 1024;
        else
            vga.VMemSize = 1024 * 1024;

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

    // Tseng ET4K implementation
    private void writeP3D5(int reg, int val, int iolen) {
        if (extensionsEnabled == 0 && reg != 0x33)
            return;

        switch (reg) {
            /*
             * 3d4h index 31h (R/W): General Purpose bit 0-3 Scratch pad 6-7 Clock Select bits 3-4.
             * int 0-1 are in 3C2h/3CCh bits 2-3.
             */
            // STORE_ET4K(3d4, 31);
            case 0x31:
                _store_3d4_31 = val;
                break;
            // 3d4h index 32h - RAS/CAS Configuration (R/W)
            // No effect on emulation. Should not be written by software.

            // STORE_ET4K(3d4, 32);
            case 0x32:
                _store_3d4_32 = val;
                break;
            case 0x33:
                // 3d4 index 33h (R/W): Extended start Address
                // 0-1 Display Start Address bits 16-17
                // 2-3 Cursor start address bits 16-17
                // Used by standard Tseng ID scheme
                _store_3d4_33 = val;
                vga.Config.DisplayStart = (vga.Config.DisplayStart & 0xffff) | ((val & 0x03) << 16);
                vga.Config.CursorStart = (vga.Config.CursorStart & 0xffff) | ((val & 0x0c) << 14);
                break;

            /*
             * 3d4h index 34h (R/W): 6845 Compatibility Control Register bit 0 Enable CS0 (alternate
             * clock timing) 1 Clock Select bit 2. int 0-1 in 3C2h bits 2-3, bits 3-4 are in 3d4h
             * index 31h bits 6-7 2 Tristate ET4000 bus and color outputs if set 3 Video Subsystem
             * Enable Register at 46E8h if set, at 3C3h if clear. 4 Enable Translation ROM for
             * reading CRTC and MISCOUT if set 5 Enable Translation ROM for writing CRTC and MISCOUT
             * if set 6 Enable double scan in AT&T compatibility mode if set 7 Enable 6845
             * compatibility if set
             */
            // TODO: Bit 6 may have effect on emulation
            // STORE_ET4K(3d4, 34);
            case 0x34:
                _store_3d4_34 = val;
                break;
            case 0x35:
                /*
                 * 3d4h index 35h (R/W): Overflow High bit 0 Vertical Blank Start Bit 10 (3d4h index
                 * 15h). 1 Vertical Total Bit 10 (3d4h index 6). 2 Vertical Display End Bit 10 (3d4h
                 * index 12h). 3 Vertical Sync Start Bit 10 (3d4h index 10h). 4 Line Compare Bit 10
                 * (3d4h index 18h). 5 Gen-Lock Enabled if set (External sync) 6 (4000)
                 * Read/Modify/Write Enabled if set. Currently not implemented. 7 Vertical interlace
                 * if set. The Vertical timing registers are programmed as if the mode was
                 * non-interlaced!!
                 */
                _store_3d4_35 = val;
                vga.Config.LineCompare = (vga.Config.LineCompare & 0x3ff) | ((val & 0x10) << 6);
            // Abusing s3 ex_ver_overflow field. This is to be cleaned up later.
            {
                byte s3val = (byte) (((val & 0x01) << 2) | // vbstart
                        ((val & 0x02) >>> 1) | // vtotal
                        ((val & 0x04) >>> 1) | // vdispend
                        ((val & 0x08) << 1) | // vsyncstart (?)
                        ((val & 0x10) << 2)); // linecomp
                if (((s3val ^ vga.S3.ExVerOverflow) & 0x3) != 0) {
                    vga.S3.ExVerOverflow = s3val;
                    vga.startResize();
                } else
                    vga.S3.ExVerOverflow = s3val;
            }
                break;

            // 3d4h index 36h - Video System Configuration 1 (R/W)
            // VGADOC provides a lot of info on this register, Ferraro has significantly less
            // detail.
            // This is unlikely to be used by any games. Bit 4 switches chipset into linear mode -
            // that may be useful in some cases if there is any software actually using it.
            // TODO (not near future): support linear addressing
            // STORE_ET4K(3d4, 36);
            case 0x36:
                _store_3d4_36 = val;
                break;
            // 3d4h index 37 - Video System Configuration 2 (R/W)
            // int 0,1, and 3 provides information about memory size:
            // 0-1 Bus width (1: 8 bit, 2: 16 bit, 3: 32 bit)
            // 3 Size of RAM chips (0: 64Kx, 1: 256Kx)
            // Other bits have no effect on emulation.
            case 0x37:
                if (val != _store_3d4_37) {
                    _store_3d4_37 = val;
                    vga.VMemWrap = ((64 * 1024) << ((val & 8) >>> 2)) << ((val & 3) - 1);
                    vga.setupHandlers();
                }
                break;

            case 0x3f:
                /*
                 * 3d4h index 3Fh (R/W): bit 0 Bit 8 of the Horizontal Total (3d4h index 0) 2 Bit 8
                 * of the Horizontal Blank Start (3d4h index 3) 4 Bit 8 of the Horizontal Retrace
                 * Start (3d4h index 4) 7 Bit 8 of the CRTC offset register (3d4h index 13h).
                 */
                // The only unimplemented one is bit 7
                _store_3d4_3f = val;
                // Abusing s3 ex_hor_overflow field which very similar. This is
                // to be cleaned up later
                if (((val ^ vga.S3.ExHorOverflow) & 3) != 0) {
                    vga.S3.ExHorOverflow = (byte) (val & 0x15);
                    vga.startResize();
                } else
                    vga.S3.ExHorOverflow = (byte) (val & 0x15);
                break;
            default:
                Log.logging(Log.LogTypes.VGAMISC, Log.LogServerities.Normal,
                        "VGA:CRTC:ET4K:Write to illegal index %2X", reg);
                break;
        }
    }

    private int readP3D5(int reg, int iolen) {
        if (extensionsEnabled == 0 && reg != 0x33)
            return 0x0;
        switch (reg) {
            // RESTORE_ET4K(3d4, 31);
            case 0x31:
                return _store_3d4_31;
            // RESTORE_ET4K(3d4, 32);
            case 0x32:
                return _store_3d4_32;
            // RESTORE_ET4K(3d4, 33);
            case 0x33:
                return _store_3d4_33;
            // RESTORE_ET4K(3d4, 34);
            case 0x34:
                return _store_3d4_34;
            // RESTORE_ET4K(3d4, 35);
            case 0x35:
                return _store_3d4_35;
            // RESTORE_ET4K(3d4, 36);
            case 0x36:
                return _store_3d4_36;
            // RESTORE_ET4K(3d4, 37);
            case 0x37:
                return _store_3d4_37;
            // RESTORE_ET4K(3d4, 3f);
            case 0x3f:
                return _store_3d4_3f;
            default:
                Log.logging(Log.LogTypes.VGAMISC, Log.LogServerities.Normal,
                        "VGA:CRTC:ET4K:Read from illegal index %2X", reg);
                break;
        }
        return 0x0;
    }

    private void writeP3C5(int reg, int val, int iolen) {
        switch (reg) {
            /*
             * 3C4h index 6 (R/W): TS State Control bit 1-2 Font Width Select in dots/character If
             * 3C4h index 4 bit 0 clear: 0: 9 dots, 1: 10 dots, 2: 12 dots, 3: 6 dots If 3C4h index
             * 5 bit 0 set: 0: 8 dots, 1: 11 dots, 2: 7 dots, 3: 16 dots Only valid if 3d4h index
             * 34h bit 3 set.
             */
            // TODO: Figure out if this has any practical use
            // STORE_ET4K(3c4, 06);
            case 0x06:
                _store_3c4_06 = val;
                break;
            // 3C4h index 7 (R/W): TS Auxiliary Mode
            // Unlikely to be used by games (things like ROM enable/disable and emulation of VGA vs
            // EGA)
            // STORE_ET4K(3c4, 07);
            case 0x07:
                _store_3c4_07 = val;
                break;
            default:
                Log.logging(Log.LogTypes.VGAMISC, Log.LogServerities.Normal,
                        "VGA:SEQ:ET4K:Write to illegal index %2X", reg);
                break;
        }
    }

    private int readP3C5(int reg, int iolen) {
        switch (reg) {
            // RESTORE_ET4K(3c4, 06);
            case 0x06:
                return _store_3c4_06;
            // RESTORE_ET4K(3c4, 07);
            case 0x07:
                return _store_3c4_07;
            default:
                Log.logging(Log.LogTypes.VGAMISC, Log.LogServerities.Normal,
                        "VGA:SEQ:ET4K:Read from illegal index %2X", reg);
                break;
        }
        return 0x0;
    }

    /*
     * 3CDh (R/W): Segment Select bit 0-3 64k Write bank number (0..15) 4-7 64k Read bank number
     * (0..15)
     */
    private void writeP3CD(int port, int val, int iolen) {
        vga.SVGA.BankWrite = val & 0x0f;
        vga.SVGA.BankRead = (val >>> 4) & 0x0f;
        vga.setupHandlers();
    }

    private int readP3CD(int port, int iolen) {
        return (vga.SVGA.BankRead << 4) | vga.SVGA.BankWrite;
    }

    private void writeP3C0(int reg, int val, int iolen) {
        switch (reg) {
            // 3c0 index 16h: ATC Miscellaneous
            // VGADOC provides a lot of information, Ferarro documents only two bits
            // and even those incompletely. The register is used as part of identification
            // scheme.
            // Unlikely to be used by any games but double timing may be useful.
            // TODO: Figure out if this has any practical use
            // STORE_ET4K(3c0, 16);
            case 0x16:
                _store_3c0_16 = val;
                break;
            /*
             * 3C0h index 17h (R/W): Miscellaneous 1 bit 7 If set protects the internal palette ram
             * and redefines the attribute bits as follows: Monochrome: bit 0-2 Select font 0-7 3 If
             * set selects blinking 4 If set selects underline 5 If set prevents the character from
             * being displayed 6 If set displays the character at half intensity 7 If set selects
             * reverse video Color: bit 0-1 Selects font 0-3 2 Foreground Blue 3 Foreground Green 4
             * Foreground Red 5 Background Blue 6 Background Green 7 Background Red
             */
            // TODO: Figure out if this has any practical use
            // STORE_ET4K(3c0, 17);
            case 0x17:
                _store_3c0_17 = val;
                break;
            default:
                Log.logging(Log.LogTypes.VGAMISC, Log.LogServerities.Normal,
                        "VGA:ATTR:ET4K:Write to illegal index %2X", reg);
                break;
        }
    }

    private int readP3C1(int reg, int iolen) {
        switch (reg) {
            // RESTORE_ET4K(3c0, 16);
            case 0x16:
                return _store_3c0_16;
            // RESTORE_ET4K(3c0, 17);
            case 0x17:
                return _store_3c0_17;
            default:
                Log.logging(Log.LogTypes.VGAMISC, Log.LogServerities.Normal,
                        "VGA:ATTR:ET4K:Read from illegal index %2X", reg);
                break;
        }
        return 0x0;
    }

    /*
     * These ports are used but have little if any effect on emulation: 3BFh (R/W): Hercules
     * Compatibility Mode 3CBh (R/W): PEL Address/Data Wd 3CEh index 0Dh (R/W): Microsequencer Mode
     * 3CEh index 0Eh (R/W): Microsequencer Reset 3d8h (R/W): Display Mode Control 3DEh (W); AT&T
     * Mode Control Register
     */

    private int getClockIndex() {
        // Ignoring bit 4, using "only" 16 frequencies. Looks like most implementations had only
        // that
        return ((vga.MiscOutput >>> 2) & 3) | ((_store_3d4_34 << 1) & 4)
                | ((_store_3d4_31 >>> 3) & 8);
    }

    private void setClockIndex(int index) {
        // Shortwiring register reads/writes for simplicity
        IO.write(0x3c2, 0xff & (0xff & (vga.MiscOutput & ~0x0c) | ((index & 3) << 2)));
        _store_3d4_34 = (_store_3d4_34 & ~0x02) | ((index & 4) >>> 1);
        // (index&0x18) if 32 clock frequencies are to be supported
        _store_3d4_31 = (_store_3d4_31 & ~0xc0) | ((index & 8) << 3);
    }

    private void finishSetMode(int crtc_base, ModeExtraData modeData) {
        _biosMode = modeData.ModeNo;

        IO.write(0x3cd, 0x00); // both banks to 0

        // Reinterpret hor_overflow. Curiously, three bits out of four are
        // in the same places. Input has hdispend (not supported), output
        // has CRTC offset (also not supported)
        int et4kHorOverflow = 0xff & ((modeData.HOverflow & 0x01) | (modeData.HOverflow & 0x04)
                | (modeData.HOverflow & 0x10));
        IO.write(crtc_base, 0x3f);
        IO.write(crtc_base + 1, et4kHorOverflow);

        // Reinterpret ver_overflow
        int et4kVerOverflow = 0xff & (((modeData.VOverflow & 0x01) << 1) | // vtotal10
                ((modeData.VOverflow & 0x02) << 1) | // vdispend10
                ((modeData.VOverflow & 0x04) >>> 2) | // vbstart10
                ((modeData.VOverflow & 0x10) >>> 1) | // vretrace10 (tseng has vsync start?)
                ((modeData.VOverflow & 0x40) >>> 2)); // line_compare
        IO.write(crtc_base, 0x35);
        IO.write(crtc_base + 1, et4kVerOverflow);

        // Clear remaining ext CRTC registers
        IO.write(crtc_base, 0x31);
        IO.write(crtc_base + 1, 0);
        IO.write(crtc_base, 0x32);
        IO.write(crtc_base + 1, 0);
        IO.write(crtc_base, 0x33);
        IO.write(crtc_base + 1, 0);
        IO.write(crtc_base, 0x34);
        IO.write(crtc_base + 1, 0);
        IO.write(crtc_base, 0x36);
        IO.write(crtc_base + 1, 0);
        IO.write(crtc_base, 0x37);
        IO.write(crtc_base + 1,
                0x0c | (vga.VMemSize == 1024 * 1024 ? 3 : vga.VMemSize == 512 * 1024 ? 2 : 1));
        // Clear ext SEQ
        IO.write(0x3c4, 0x06);
        IO.write(0x3c5, 0);
        IO.write(0x3c4, 0x07);
        IO.write(0x3c5, 0);
        // Clear ext ATTR
        IO.write(0x3c0, 0x16);
        IO.write(0x3c0, 0);
        IO.write(0x3c0, 0x17);
        IO.write(0x3c0, 0);

        // Select SVGA clock to get close to 60Hz (not particularly clean implementation)
        if (modeData.ModeNo > 0x13) {
            int target = modeData.VTotal * 8 * modeData.HTotal * 60;
            int best = 1;
            int dist = 100000000;
            for (int i = 0; i < 16; i++) {
                int cdiff = Math.abs(target - _clockFreq[i]);
                if (cdiff < dist) {
                    best = i;
                    dist = cdiff;
                }
            }
            setClockIndex(best);
        }

        if (vga.SVGADrv.DetermineMode != null)
            vga.SVGADrv.DetermineMode.exec();

        // Verified (on real hardware and in a few games): Tseng ET4000 used chain4 implementation
        // different from standard VGA. It was also not limited to 64K in regular mode 13h.
        vga.Config.CompatibleChain4 = false;
        vga.VMemWrap = vga.VMemSize;

        vga.setupHandlers();
    }

    private void determineMode() {
        // Close replica from the base implementation. It will stay here
        // until I figure a way to either distinguish VGAModes.M_VGA and VGAModes.M_LIN8 or
        // merge them.
        if ((vga.Attr.ModeControl & 1) != 0) {
            if ((vga.GFX.Mode & 0x40) != 0)
                vga.setMode((_biosMode <= 0x13) ? VGAModes.VGA : VGAModes.LIN8); // Ugly...
            else if ((vga.GFX.Mode & 0x20) != 0)
                vga.setMode(VGAModes.CGA4);
            else if ((vga.GFX.Miscellaneous & 0x0c) == 0x0c)
                vga.setMode(VGAModes.CGA2);
            else
                vga.setMode((_biosMode <= 0x13) ? VGAModes.EGA : VGAModes.LIN4);
        } else {
            vga.setMode(VGAModes.TEXT);
        }
    }

    private void setClock(int which, int target) {
        _clockFreq[which] = 1000 * target;
        vga.startResize();
    }

    private int getClock() {
        return _clockFreq[getClockIndex()];
    }

    private boolean acceptsMode(int mode) {
        return INT10Mode.videoModeMemSize(mode) < vga.VMemSize;
        // return mode != 0x3d;
    }
}
