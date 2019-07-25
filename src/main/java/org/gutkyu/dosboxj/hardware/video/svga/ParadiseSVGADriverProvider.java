package org.gutkyu.dosboxj.hardware.video.svga;

import org.gutkyu.dosboxj.util.*;
import org.gutkyu.dosboxj.hardware.io.*;
import org.gutkyu.dosboxj.hardware.memory.*;
import org.gutkyu.dosboxj.hardware.video.VGA;
import org.gutkyu.dosboxj.hardware.video.VGAModes;
import org.gutkyu.dosboxj.interrupt.int10.INT10Mode;

// PVGA1a
public final class ParadiseSVGADriverProvider {
    private int _pr0A = 0;
    private int _pr0B = 0;
    private int _pr1 = 0;
    private int _pr2 = 0;
    private int _pr3 = 0;
    private int _pr4 = 0;
    private int _pr5 = 0;

    private boolean locked() {
        return (_pr5 & 7) != 5;
    }

    private int[] _clockFreq = new int[] {0, 0, 0, 0};
    private int _biosMode = 0;

    private VGA vga = null;

    public ParadiseSVGADriverProvider(VGA vga) {
        this.vga = vga;
        vga.SVGADrv.WriteP3CF = this::writeP3CF;
        vga.SVGADrv.ReadP3CF = this::readP3CF;

        vga.SVGADrv.SetVideoMode = this::finishSetMode;
        vga.SVGADrv.DetermineMode = this::determineMode;
        vga.SVGADrv.SetClock = this::setClock;
        vga.SVGADrv.GetClock = this::getClock;
        vga.SVGADrv.AcceptsMode = this::acceptsMode;

        vga.setClock(0, VGA.CLK_25);
        vga.setClock(1, VGA.CLK_28);
        vga.setClock(2, 32400); // could not find documentation
        vga.setClock(3, 35900);

        // Adjust memory, default to 512K
        if (vga.VMemSize == 0)
            vga.VMemSize = 512 * 1024;

        if (vga.VMemSize < 512 * 1024) {
            vga.VMemSize = 256 * 1024;
            _pr1 = 1 << 6;
        } else if (vga.VMemSize > 512 * 1024) {
            vga.VMemSize = 1024 * 1024;
            _pr1 = 3 << 6;
        } else {
            _pr1 = 2 << 6;
        }

        // Paradise ROM signature
        int romBase = Memory.physMake(0xc000, 0);
        Memory.physWriteB(romBase + 0x007d, 'V');
        Memory.physWriteB(romBase + 0x007e, 'G');
        Memory.physWriteB(romBase + 0x007f, 'A');
        Memory.physWriteB(romBase + 0x0080, '=');

        IO.write(0x3cf, 0x05); // Enable!
    }

    private void setupBank() {
        // Note: There is some inconsistency in available documentation. Most sources tell that
        // PVGA1A used
        // only 7 bits of bank index (VGADOC and Ferraro agree on that) but also point that there
        // are
        // implementations with 1M of RAM which is simply not possible with 7-bit banks. This
        // implementation
        // assumes that the eighth bit was actually wired and could be used. This does not conflict
        // with
        // anything and actually works in WHATVGA just fine.
        if ((_pr1 & 0x08) != 0) {
            // TODO: Dual bank function is not supported yet
            // TODO: Requirements are not compatible with vga_memory implementation.
        } else {
            // Single bank config is straightforward
            vga.SVGA.BankRead = vga.SVGA.BankWrite = (byte) _pr0A;
            vga.SVGA.BankSize = 4 * 1024;
            vga.setupHandlers();
        }
    }

    private void writeP3CF(int reg, int val, int iolen) {
        if (locked() && reg >= 0x09 && reg <= 0x0e)
            return;

        switch (reg) {
            case 0x09:
                // Bank A, 4K granularity, not using bit 7
                // Maps to A800h-AFFFh if PR1 bit 3 set and 64k config B000h-BFFFh if 128k config.
                // A000h-AFFFh otherwise.
                _pr0A = val;
                setupBank();
                break;
            case 0x0a:
                // Bank B, 4K granularity, not using bit 7
                // Maps to A000h-A7FFh if PR1 bit 3 set and 64k config, A000h-AFFFh if 128k
                _pr0B = val;
                setupBank();
                break;
            case 0x0b:
                // Memory size. We only allow to mess with bit 3 here (enable bank B) - this may
                // break some detection schemes
                _pr1 = (_pr1 & ~0x08) | (val & 0x08);
                setupBank();
                break;
            case 0x0c:
                // Video configuration
                // TODO: Figure out if there is anything worth implementing here.
                _pr2 = val;
                break;
            case 0x0d:
                // CRT control. int 3-4 contain bits 16-17 of CRT start.
                // TODO: Implement bit 2 (CRT address doubling - this mechanism is present in other
                // chipsets as well,
                // but not implemented in DosBox core)
                _pr3 = val;
                vga.Config.DisplayStart = (vga.Config.DisplayStart & 0xffff) | ((val & 0x18) << 13);
                vga.Config.CursorStart = (vga.Config.CursorStart & 0xffff) | ((val & 0x18) << 13);
                break;
            case 0x0e:
                // Video control
                // TODO: Figure out if there is anything worth implementing here.
                _pr4 = val;
                break;
            case 0x0f:
                // Enable extended registers
                _pr5 = val;
                break;
            default:
                Log.logging(Log.LogTypes.VGAMISC, Log.LogServerities.Normal,
                        "VGA:GFX:PVGA1A:Write to illegal index %2X", reg);
                break;
        }
    }

    private int readP3CF(int reg, int iolen) {
        if (locked() && reg >= 0x09 && reg <= 0x0e)
            return 0x0;

        switch (reg) {
            case 0x09:
                return _pr0A;
            case 0x0a:
                return _pr0B;
            case 0x0b:
                return _pr1;
            case 0x0c:
                return _pr2;
            case 0x0d:
                return _pr3;
            case 0x0e:
                return _pr4;
            case 0x0f:
                return _pr5;
            default:
                Log.logging(Log.LogTypes.VGAMISC, Log.LogServerities.Normal,
                        "VGA:GFX:PVGA1A:Read from illegal index %2X", reg);
                break;
        }

        return 0x0;
    }

    private void finishSetMode(int crtcBase, ModeExtraData modeData) {
        _biosMode = modeData.ModeNo;

        // Reset to single bank and set it to 0. May need to unlock first (DPaint locks on exit)
        IO.write(0x3ce, 0x0f);
        int oldlock = IO.read(0x3cf);
        IO.write(0x3cf, 0x05);
        IO.write(0x3ce, 0x09);
        IO.write(0x3cf, 0x00);
        IO.write(0x3ce, 0x0a);
        IO.write(0x3cf, 0x00);
        IO.write(0x3ce, 0x0b);
        int val = IO.read(0x3cf);
        IO.write(0x3cf, val & ~0x08);
        IO.write(0x3ce, 0x0c);
        IO.write(0x3cf, 0x00);
        IO.write(0x3ce, 0x0d);
        IO.write(0x3cf, 0x00);
        IO.write(0x3ce, 0x0e);
        IO.write(0x3cf, 0x00);
        IO.write(0x3ce, 0x0f);
        IO.write(0x3cf, oldlock);

        if (vga.SVGADrv.DetermineMode != null)
            vga.SVGADrv.DetermineMode.exec();

        if (vga.Mode != VGAModes.VGA) {
            vga.Config.CompatibleChain4 = false;
            vga.VMemWrap = vga.VMemSize;
        } else {
            vga.Config.CompatibleChain4 = true;
            vga.VMemWrap = 256 * 1024;
        }

        vga.setupHandlers();
    }

    private void determineMode() {
        // Close replica from the base implementation. It will stay here
        // until I figure a way to either distinguish VGAModes.M_VGA and VGAModes.M_LIN8 or
        // merge them.
        if ((vga.Attr.ModeControl & 1) != 0) {
            if ((vga.GFX.Mode & 0x40) != 0)
                vga.setMode((_biosMode <= 0x13) ? VGAModes.VGA : VGAModes.LIN8);
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
        if (which < 4) {
            _clockFreq[which] = 1000 * target;
            vga.startResize();
        }
    }

    private int getClock() {
        return _clockFreq[(vga.MiscOutput >>> 2) & 3];
    }

    private boolean acceptsMode(int mode) {
        return INT10Mode.videoModeMemSize(mode) < vga.VMemSize;
    }

}
