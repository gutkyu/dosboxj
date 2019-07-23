package org.gutkyu.dosboxj.interrupt.int10;

import org.gutkyu.dosboxj.cpu.*;
import org.gutkyu.dosboxj.hardware.io.*;
import org.gutkyu.dosboxj.hardware.memory.*;
import org.gutkyu.dosboxj.hardware.video.*;
import org.gutkyu.dosboxj.util.*;
import java.util.Arrays;
import org.gutkyu.dosboxj.*;

/*--------------------------- begin INT10Vesa -----------------------------*/
final class VESA {

    private static String string_oem = "S3 Incorporated. Trio64";
    private static String string_vendorname = "DOSBox Development Team";
    private static String string_productname = "DOSBox - The DOS Emulator";
    private static String string_productrev = "DOSBox " + DOSBox.VERSION;

    private static class callback {
        public int setwindow;
        public int pmStart;
        public int pmWindow;
        public int pmPalette;
    }

    private static callback _callback = new callback();

    private static final int Size_MODE_INFO_ModeAttributes = 2;
    private static final int Size_MODE_INFO_WinAAttributes = 1;
    private static final int Size_MODE_INFO_WinBAttributes = 1;
    private static final int Size_MODE_INFO_WinGranularity = 2;
    private static final int Size_MODE_INFO_WinSize = 2;
    private static final int Size_MODE_INFO_WinASegment = 2;
    private static final int Size_MODE_INFO_WinBSegment = 2;
    private static final int Size_MODE_INFO_WinFuncPtr = 4;
    private static final int Size_MODE_INFO_BytesPerScanLine = 2;
    private static final int Size_MODE_INFO_XResolution = 2;
    private static final int Size_MODE_INFO_YResolution = 2;
    private static final int Size_MODE_INFO_XCharSize = 1;
    private static final int Size_MODE_INFO_YCharSize = 1;
    private static final int Size_MODE_INFO_NumberOfPlanes = 1;
    private static final int Size_MODE_INFO_BitsPerPixel = 1;
    private static final int Size_MODE_INFO_NumberOfBanks = 1;
    private static final int Size_MODE_INFO_MemoryModel = 1;
    private static final int Size_MODE_INFO_BankSize = 1;
    private static final int Size_MODE_INFO_NumberOfImagePages = 1;
    private static final int Size_MODE_INFO_Reserved_page = 1;
    private static final int Size_MODE_INFO_RedMaskSize = 1;
    private static final int Size_MODE_INFO_RedMaskPos = 1;
    private static final int Size_MODE_INFO_GreenMaskSize = 1;
    private static final int Size_MODE_INFO_GreenMaskPos = 1;
    private static final int Size_MODE_INFO_BlueMaskSize = 1;
    private static final int Size_MODE_INFO_BlueMaskPos = 1;
    private static final int Size_MODE_INFO_ReservedMaskSize = 1;
    private static final int Size_MODE_INFO_ReservedMaskPos = 1;
    private static final int Size_MODE_INFO_DirectColorModeInfo = 1;
    private static final int Size_MODE_INFO_PhysBasePtr = 4;
    private static final int Size_MODE_INFO_OffScreenMemOffset = 4;
    private static final int Size_MODE_INFO_OffScreenMemSize = 2;
    private static final int Size_MODE_INFO_Reserved = 206;

    private static final int Size_MODE_INFO_Total = 256;


    private static final int Off_MODE_INFO_ModeAttributes = 0;
    private static final int Off_MODE_INFO_WinAAttributes = 2;
    private static final int Off_MODE_INFO_WinBAttributes = 3;
    private static final int Off_MODE_INFO_WinGranularity = 4;
    private static final int Off_MODE_INFO_WinSize = 6;
    private static final int Off_MODE_INFO_WinASegment = 8;
    private static final int Off_MODE_INFO_WinBSegment = 10;
    private static final int Off_MODE_INFO_WinFuncPtr = 12;
    private static final int Off_MODE_INFO_BytesPerScanLine = 16;
    private static final int Off_MODE_INFO_XResolution = 18;
    private static final int Off_MODE_INFO_YResolution = 20;
    private static final int Off_MODE_INFO_XCharSize = 22;
    private static final int Off_MODE_INFO_YCharSize = 23;
    private static final int Off_MODE_INFO_NumberOfPlanes = 24;
    private static final int Off_MODE_INFO_BitsPerPixel = 25;
    private static final int Off_MODE_INFO_NumberOfBanks = 26;
    private static final int Off_MODE_INFO_MemoryModel = 27;
    private static final int Off_MODE_INFO_BankSize = 28;
    private static final int Off_MODE_INFO_NumberOfImagePages = 29;
    private static final int Off_MODE_INFO_Reserved_page = 30;
    private static final int Off_MODE_INFO_RedMaskSize = 31;
    private static final int Off_MODE_INFO_RedMaskPos = 32;
    private static final int Off_MODE_INFO_GreenMaskSize = 33;
    private static final int Off_MODE_INFO_GreenMaskPos = 34;
    private static final int Off_MODE_INFO_BlueMaskSize = 35;
    private static final int Off_MODE_INFO_BlueMaskPos = 36;
    private static final int Off_MODE_INFO_ReservedMaskSize = 37;
    private static final int Off_MODE_INFO_ReservedMaskPos = 38;
    private static final int Off_MODE_INFO_DirectColorModeInfo = 39;
    private static final int Off_MODE_INFO_PhysBasePtr = 40;
    private static final int Off_MODE_INFO_OffScreenMemOffset = 44;
    private static final int Off_MODE_INFO_OffScreenMemSize = 48;
    private static final int Off_MODE_INFO_Reserved = 50;

    public static int getSVGAInformation(int seg, int off) {
        /* Fill 256 byte buffer with VESA information */
        int buffer = Memory.physMake(seg, off);
        int i;
        boolean vbe2 = false;
        int vbe2_pos = 0xffff & (256 + off);
        int id = Memory.readD(buffer);
        if (((id == 0x56424532) || (id == 0x32454256)) && (!INT10.int10.VesaOldVbe))
            vbe2 = true;
        if (vbe2) {
            for (i = 0; i < 0x200; i++)
                Memory.writeB(buffer + i, 0);
        } else {
            for (i = 0; i < 0x100; i++)
                Memory.writeB(buffer + i, 0);
        }
        /* Fill common data */
        Memory.blockWrite(buffer, CStringPt.create("VESA"), 4); // Identification
        if (!INT10.int10.VesaOldVbe)
            Memory.writeW(buffer + 0x04, 0x200); // Vesa version 2.0
        else
            Memory.writeW(buffer + 0x04, 0x102); // Vesa version 1.2
        if (vbe2) {
            Memory.writeD(buffer + 0x06, Memory.realMake(seg, vbe2_pos));
            for (i = 0; i < string_oem.length(); i++)
                Memory.realWriteB(seg, vbe2_pos++, string_oem.charAt(i));
            Memory.writeW(buffer + 0x14, 0x200); // VBE 2 software revision
            Memory.writeD(buffer + 0x16, Memory.realMake(seg, vbe2_pos));
            for (i = 0; i < string_vendorname.length(); i++)
                Memory.realWriteB(seg, vbe2_pos++, string_vendorname.charAt(i));
            Memory.writeD(buffer + 0x1a, Memory.realMake(seg, vbe2_pos));
            for (i = 0; i < string_productname.length(); i++)
                Memory.realWriteB(seg, vbe2_pos++, string_productname.charAt(i));
            Memory.writeD(buffer + 0x1e, Memory.realMake(seg, vbe2_pos));
            for (i = 0; i < string_productrev.length(); i++)
                Memory.realWriteB(seg, vbe2_pos++, string_productrev.charAt(i));
        } else {
            Memory.writeD(buffer + 0x06, INT10.int10.RomOEMString); // Oemstring
        }
        Memory.writeD(buffer + 0x0a, 0x0); // Capabilities and flags
        Memory.writeD(buffer + 0x0e, INT10.int10.RomVesaModes); // VESA Mode list
        // memory size in 64kb blocks
        Memory.writeW(buffer + 0x12, 0xffff & (VGA.instance().VMemSize / (64 * 1024)));
        return 0x00;
    }

    public static int getSVGAModeInformation(int mode, int seg, int off) {
        byte[] minfo = new byte[Size_MODE_INFO_Total];
        // Array.Clear(minfo, 0, minfo.Length);
        Arrays.fill(minfo, 0, minfo.length, (byte) 0);
        int buf = Memory.physMake(seg, off);
        int pageSize;
        byte modeAttributes;
        int i = 0;

        mode &= 0x3fff; // vbe2 compatible, ignore lfb and keep screen content bits
        if (mode < 0x100)
            return 0x01;
        if (VGA.instance().SVGADrv.AcceptsMode != null) {
            if (!VGA.instance().SVGADrv.AcceptsMode.exec(mode))
                return 0x01;
        }
        boolean foundIt = false;
        while (INT10Mode.ModeList_VGA[i].Mode != 0xffff) {
            if (mode == INT10Mode.ModeList_VGA[i].Mode) {
                // goto foundit;
                foundIt = true;
                break;
            } else
                i++;
        }
        if (!foundIt)
            return 0x01;
        // foundit:
        if ((INT10.int10.VesaOldVbe) && (INT10Mode.ModeList_VGA[i].Mode >= 0x120))
            return 0x01;
        INT10Mode.VideoModeBlock mblock = INT10Mode.ModeList_VGA[i];
        switch (mblock.Type) {
            case LIN4:
                pageSize = mblock.SHeight * mblock.SWidth / 2;
                pageSize = (pageSize | 15) & ~15;
                Memory.hostWriteW(minfo, Off_MODE_INFO_BytesPerScanLine, mblock.SWidth / 8);
                Memory.hostWriteB(minfo, Off_MODE_INFO_NumberOfPlanes, 0x4);
                Memory.hostWriteB(minfo, Off_MODE_INFO_BitsPerPixel, 4);
                Memory.hostWriteB(minfo, Off_MODE_INFO_MemoryModel, 3); // ega planar mode
                modeAttributes = 0x1b; // Color, graphics, no linear buffer
                break;
            case LIN8:
                pageSize = mblock.SHeight * mblock.SWidth;
                pageSize = (pageSize | 15) & ~15;
                Memory.hostWriteW(minfo, Off_MODE_INFO_BytesPerScanLine, mblock.SWidth);
                Memory.hostWriteB(minfo, Off_MODE_INFO_NumberOfPlanes, 0x1);
                Memory.hostWriteB(minfo, Off_MODE_INFO_BitsPerPixel, 8);
                Memory.hostWriteB(minfo, Off_MODE_INFO_MemoryModel, 4); // packed pixel
                modeAttributes = 0x1b; // Color, graphics
                if (!INT10.int10.VesaNoLFB)
                    modeAttributes |= 0x80; // linear framebuffer
                break;
            case LIN15:
                pageSize = mblock.SHeight * mblock.SWidth * 2;
                pageSize = (pageSize | 15) & ~15;
                Memory.hostWriteW(minfo, Off_MODE_INFO_BytesPerScanLine, mblock.SWidth * 2);
                Memory.hostWriteB(minfo, Off_MODE_INFO_NumberOfPlanes, 0x1);
                Memory.hostWriteB(minfo, Off_MODE_INFO_BitsPerPixel, 15);
                Memory.hostWriteB(minfo, Off_MODE_INFO_MemoryModel, 6); // HiColour
                Memory.hostWriteB(minfo, Off_MODE_INFO_RedMaskSize, 5);
                Memory.hostWriteB(minfo, Off_MODE_INFO_RedMaskPos, 10);
                Memory.hostWriteB(minfo, Off_MODE_INFO_GreenMaskSize, 5);
                Memory.hostWriteB(minfo, Off_MODE_INFO_GreenMaskPos, 5);
                Memory.hostWriteB(minfo, Off_MODE_INFO_BlueMaskSize, 5);
                Memory.hostWriteB(minfo, Off_MODE_INFO_BlueMaskPos, 0);
                Memory.hostWriteB(minfo, Off_MODE_INFO_ReservedMaskSize, 0x01);
                Memory.hostWriteB(minfo, Off_MODE_INFO_ReservedMaskPos, 0x0f);
                modeAttributes = 0x1b; // Color, graphics
                if (!INT10.int10.VesaNoLFB)
                    modeAttributes |= 0x80; // linear framebuffer
                break;
            case LIN16:
                pageSize = mblock.SHeight * mblock.SWidth * 2;
                pageSize = (pageSize | 15) & ~15;
                Memory.hostWriteW(minfo, Off_MODE_INFO_BytesPerScanLine, mblock.SWidth * 2);
                Memory.hostWriteB(minfo, Off_MODE_INFO_NumberOfPlanes, 0x1);
                Memory.hostWriteB(minfo, Off_MODE_INFO_BitsPerPixel, 16);
                Memory.hostWriteB(minfo, Off_MODE_INFO_MemoryModel, 6); // HiColour
                Memory.hostWriteB(minfo, Off_MODE_INFO_RedMaskSize, 5);
                Memory.hostWriteB(minfo, Off_MODE_INFO_RedMaskPos, 11);
                Memory.hostWriteB(minfo, Off_MODE_INFO_GreenMaskSize, 6);
                Memory.hostWriteB(minfo, Off_MODE_INFO_GreenMaskPos, 5);
                Memory.hostWriteB(minfo, Off_MODE_INFO_BlueMaskSize, 5);
                Memory.hostWriteB(minfo, Off_MODE_INFO_BlueMaskPos, 0);
                modeAttributes = 0x1b; // Color, graphics
                if (!INT10.int10.VesaNoLFB)
                    modeAttributes |= 0x80; // linear framebuffer
                break;
            case LIN32:
                pageSize = mblock.SHeight * mblock.SWidth * 4;
                pageSize = (pageSize | 15) & ~15;
                Memory.hostWriteW(minfo, Off_MODE_INFO_BytesPerScanLine, mblock.SWidth * 4);
                Memory.hostWriteB(minfo, Off_MODE_INFO_NumberOfPlanes, 0x1);
                Memory.hostWriteB(minfo, Off_MODE_INFO_BitsPerPixel, 32);
                Memory.hostWriteB(minfo, Off_MODE_INFO_MemoryModel, 6); // HiColour
                Memory.hostWriteB(minfo, Off_MODE_INFO_RedMaskSize, 8);
                Memory.hostWriteB(minfo, Off_MODE_INFO_RedMaskPos, 0x10);
                Memory.hostWriteB(minfo, Off_MODE_INFO_GreenMaskSize, 0x8);
                Memory.hostWriteB(minfo, Off_MODE_INFO_GreenMaskPos, 0x8);
                Memory.hostWriteB(minfo, Off_MODE_INFO_BlueMaskSize, 0x8);
                Memory.hostWriteB(minfo, Off_MODE_INFO_BlueMaskPos, 0x0);
                Memory.hostWriteB(minfo, Off_MODE_INFO_ReservedMaskSize, 0x8);
                Memory.hostWriteB(minfo, Off_MODE_INFO_ReservedMaskPos, 0x18);
                modeAttributes = 0x1b; // Color, graphics
                if (!INT10.int10.VesaNoLFB)
                    modeAttributes |= 0x80; // linear framebuffer
                break;
            /*
             * case M_TEXT: pageSize = mblock.sheight/8 * mblock.swidth*2/8; pageSize = (pageSize |
             * 15) & ~ 15; var_write(&minfo.BytesPerScanLine,mblock.swidth*2/8);
             * var_write(&minfo.NumberOfPlanes,0x4); var_write(&minfo.BitsPerPixel,4);
             * var_write(&minfo.MemoryModel,0); //Text modeAttributes = 0x0f; //Color, text, bios
             * output break;
             */
            default:
                return 0x1;
        }
        // var_write(&minfo.WinAAttributes, 0x7);
        // minfo.WinAAttributes = 0x7; // Exists/readable/writable
        minfo[Off_MODE_INFO_WinBAttributes] = 0x7;// Exists/readable/writable
        if (pageSize > VGA.instance().VMemSize) {
            // Mode not supported by current hardware configuration
            Memory.hostWriteW(minfo, Off_MODE_INFO_ModeAttributes,
                    0xffff & (modeAttributes & ~0x1));
            Memory.hostWriteW(minfo, Off_MODE_INFO_NumberOfImagePages, 0);
        } else {
            Memory.hostWriteW(minfo, Off_MODE_INFO_ModeAttributes, modeAttributes);
            int pages = (VGA.instance().VMemSize / pageSize) - 1;
            Memory.hostWriteB(minfo, Off_MODE_INFO_NumberOfImagePages, (byte) pages);
        }

        if (mblock.Type == VGAModes.TEXT) {
            Memory.hostWriteW(minfo, Off_MODE_INFO_WinGranularity, 32);
            Memory.hostWriteW(minfo, Off_MODE_INFO_WinSize, 32);
            Memory.hostWriteW(minfo, Off_MODE_INFO_WinASegment, 0xb800);
            Memory.hostWriteW(minfo, Off_MODE_INFO_XResolution, mblock.SWidth / 8);
            Memory.hostWriteW(minfo, Off_MODE_INFO_YResolution, mblock.SHeight / 8);
        } else {
            Memory.hostWriteW(minfo, Off_MODE_INFO_WinGranularity, 64);
            Memory.hostWriteW(minfo, Off_MODE_INFO_WinSize, 64);
            Memory.hostWriteW(minfo, Off_MODE_INFO_WinASegment, 0xa000);
            Memory.hostWriteW(minfo, Off_MODE_INFO_XResolution, mblock.SWidth);
            Memory.hostWriteW(minfo, Off_MODE_INFO_YResolution, mblock.SHeight);
        }
        Memory.hostWriteD(minfo, Off_MODE_INFO_WinFuncPtr,
                Callback.realPointer(_callback.setwindow));
        Memory.hostWriteB(minfo, Off_MODE_INFO_NumberOfBanks, 0x1);
        Memory.hostWriteB(minfo, Off_MODE_INFO_Reserved_page, 0x1);
        Memory.hostWriteB(minfo, Off_MODE_INFO_XCharSize, mblock.CWidth);
        Memory.hostWriteB(minfo, Off_MODE_INFO_YCharSize, mblock.CHeight);
        if (!INT10.int10.VesaNoLFB)
            Memory.hostWriteD(minfo, Off_MODE_INFO_PhysBasePtr, INT10.S3_LFB_BASE);

        Memory.blockWrite(buf, minfo, 0, minfo.length);
        return 0x00;
    }


    // public static byte SetSVGAMode(short mode) {
    public static int setSVGAMode(int mode) {
        if (INT10Mode.setVideoMode(mode)) {
            INT10.int10.VesaSetMode = mode & 0x7fff;
            return 0x00;
        }
        return 0x01;
    }

    // uint16
    public static int getSVGAMode() {
        int mode = 0;
        if (INT10.int10.VesaSetMode != 0xffff)
            mode = INT10.int10.VesaSetMode;
        else
            mode = INT10Mode.CurMode.Mode;
        return mode;
        // return 0x00;
    }

    public static byte setCPUWindow(byte window, byte address) {
        if (window > 0)
            return 0x1;
        if ((0xff & address) * 64 * 1024 < VGA.instance().VMemSize) {
            IO.write(0x3d4, 0x6a);
            IO.write(0x3d5, 0xff & address);
            return 0x0;
        } else
            return 0x1;
    }

    public static byte setCPUWindow(int window, int address) {
        return setCPUWindow((byte) window, (byte) address);
    }

    // public static byte GetCPUWindow(byte window, ref UInt16 address)
    // uint8(uint8)
    public static int tryCPUWindow(int window) {
        return window > 0 ? 0x1 : 0x00;
    }

    // uint16(uint8)
    public static int getCPUWindow() {
        IO.write(0x3d4, 0x6a);
        return 0xffff & IO.read(0x3d5);// address
    }


    public static byte setPalette(int data, int index, int count) {
        // Structure is (vesa 3.0 doc): blue,green,red,alignment
        int r, g, b;
        if (index > 255)
            return 0x1;
        if (index + count > 256)
            return 0x1;
        IO.write(0x3c8, index);
        while (count > 0) {
            b = Memory.readB(data++);
            g = Memory.readB(data++);
            r = Memory.readB(data++);
            data++;
            IO.write(0x3c9, r);
            IO.write(0x3c9, g);
            IO.write(0x3c9, b);
            count--;
        }
        return 0x00;
    }


    public static byte getPalette(int data, int index, int count) {
        int r, g, b;// uint8
        if (index > 255)
            return 0x1;
        if (index + count > 256)
            return 0x1;
        IO.write(0x3c7, index);
        while (count > 0) {
            r = IO.read(0x3c9);
            g = IO.read(0x3c9);
            b = IO.read(0x3c9);
            Memory.writeB(data++, b);
            Memory.writeB(data++, g);
            Memory.writeB(data++, r);
            data++;
            count--;
        }
        return 0x00;
    }

    public static int LineLengthInfoBytes, LineLengthInfoPixels, LineLengthInfoLines;

    public static byte scanLineLength(int subcall, int val) {
        int bytes, pixels, lines;
        byte bpp;
        switch (INT10Mode.CurMode.Type) {
            case LIN4:
                bpp = 1;
                break;
            case LIN8:
                bpp = 1;
                break;
            case LIN15:
            case LIN16:
                bpp = 2;
                break;
            case LIN32:
                bpp = 4;
                break;
            default:
                return 0x1;
        }
        switch (subcall) {
            case 0x00: /* Set in pixels */
                if (INT10Mode.CurMode.Type == VGAModes.LIN4)
                    VGA.instance().Config.ScanLen = val / 2;
                else
                    VGA.instance().Config.ScanLen = val * bpp;
                break;
            case 0x02: /* Set in bytes */
                if (INT10Mode.CurMode.Type == VGAModes.LIN4)
                    VGA.instance().Config.ScanLen = val * 4;
                else
                    VGA.instance().Config.ScanLen = val;
                break;
            case 0x03: /* Get maximum */
                bytes = 0x400 * 4;
                pixels = 0xffff & (bytes / bpp);
                lines = 0xffff & (VGA.instance().VMemSize / bytes);
                return 0x00;
            case 0x01: /* Get lengths */
                break;
            default:
                return 0x1; // Illegal call
        }
        if (subcall != 0x01) {
            /* Write the scan line to video card the simple way */
            if ((VGA.instance().Config.ScanLen & 7) != 0)
                VGA.instance().Config.ScanLen += 8;
            VGA.instance().Config.ScanLen /= 8;
        }
        if (INT10Mode.CurMode.Type == VGAModes.LIN4) {
            pixels = 0xffff & ((VGA.instance().Config.ScanLen * 16) / bpp);
            bytes = 0xffff & (VGA.instance().Config.ScanLen * 2);
            lines = 0xffff & (VGA.instance().VMemSize / (bytes * 4));
        } else {
            pixels = 0xffff & ((VGA.instance().Config.ScanLen * 8) / bpp);
            bytes = 0xffff & (VGA.instance().Config.ScanLen * 8);
            lines = 0xffff & (VGA.instance().VMemSize / bytes);
        }
        LineLengthInfoBytes = bytes;
        LineLengthInfoPixels = pixels;
        LineLengthInfoLines = lines;
        VGA.instance().startResize();
        return 0x0;
    }

    // (uint16,uint16)
    public static byte setDisplayStart(int x, int y) {
        // TODO Maybe do things differently with lowres double line modes?
        int start;
        switch (INT10Mode.CurMode.Type) {
            case LIN4:
                start = VGA.instance().Config.ScanLen * 16 * y + x;
                VGA.instance().Config.DisplayStart = start / 8;
                IO.read(0x3da);
                IO.write(0x3c0, 0x13 + 32);
                IO.write(0x3c0, 0xff & (start % 8));
                break;
            case LIN8:
                start = VGA.instance().Config.ScanLen * 8 * y + x;
                VGA.instance().Config.DisplayStart = start / 4;
                IO.read(0x3da);
                IO.write(0x3c0, 0x13 + 32);
                IO.write(0x3c0, 0xff & ((start % 4) * 2));
                break;
            case LIN16:
            case LIN15:
                start = VGA.instance().Config.ScanLen * 8 * y + x * 2;
                VGA.instance().Config.DisplayStart = start / 4;
                break;
            case LIN32:
                start = VGA.instance().Config.ScanLen * 8 * y + x * 4;
                VGA.instance().Config.DisplayStart = start / 4;
                break;
            default:
                return 0x1;
        }
        return 0x00;
    }

    public static int ResponseDisplayStartX, ResponseDisplayStartY;

    public static byte getDisplayStart() {
        int times = (VGA.instance().Config.DisplayStart * 4) / (VGA.instance().Config.ScanLen * 8);
        int rem = (VGA.instance().Config.DisplayStart * 4) % (VGA.instance().Config.ScanLen * 8);
        int pan = VGA.instance().Config.PelPanning;
        switch (INT10Mode.CurMode.Type) {
            case LIN8:
                ResponseDisplayStartY = 0xffff & times;
                ResponseDisplayStartX = 0xffff & (rem + pan);
                break;
            default:
                return 0x1;
        }
        return 0x00;
    }

    private static int setWindow() {
        if (Register.getRegBH() != 0) {
            int regAH = VESA.tryCPUWindow(Register.getRegBL());
            Register.setRegAH(regAH);
            if (regAH == 0) {
                Register.setRegDX(getCPUWindow());
            }
        } else
            Register.setRegAH(setCPUWindow(Register.getRegBL(), 0xff & Register.getRegDX()));
        Register.setRegAL(0x4f);
        return 0;
    }

    private static int pmSetWindow() {
        setCPUWindow(0, 0xff & Register.getRegDX());
        return 0;
    }

    private static int pmSetPalette() {
        setPalette(Register.segPhys(Register.SEG_NAME_ES) + Register.getRegEDI(),
                Register.getRegDX(), Register.getRegCX());
        return 0;
    }

    private static int pmSetStart() {
        int start = (Register.getRegDX() << 16) | Register.getRegCX();
        VGA.instance().Config.DisplayStart = start;
        return 0;
    }

    public static void setupVESA() {
        /* Put the mode list somewhere in memory */
        int i;
        i = 0;
        INT10.int10.RomVesaModes = Memory.realMake(0xc000, INT10.int10.RomUsed);
        // TODO Maybe add normal vga modes too, but only seems to complicate things
        while (INT10Mode.ModeList_VGA[i].Mode != 0xffff) {
            boolean canuse_mode = false;
            if (VGA.instance().SVGADrv.AcceptsMode == null)
                canuse_mode = true;
            else {
                if (VGA.instance().SVGADrv.AcceptsMode.exec(INT10Mode.ModeList_VGA[i].Mode))
                    canuse_mode = true;
            }
            if (INT10Mode.ModeList_VGA[i].Mode >= 0x100 && canuse_mode) {
                if ((!INT10.int10.VesaOldVbe) || (INT10Mode.ModeList_VGA[i].Mode < 0x120)) {
                    Memory.physWriteW(Memory.physMake(0xc000, INT10.int10.RomUsed),
                            INT10Mode.ModeList_VGA[i].Mode);
                    INT10.int10.RomUsed += 2;
                }
            }
            i++;
        }
        Memory.physWriteW(Memory.physMake(0xc000, INT10.int10.RomUsed), 0xffff);
        INT10.int10.RomUsed += 2;
        INT10.int10.RomOEMString = Memory.realMake(0xc000, INT10.int10.RomUsed);
        int len = string_oem.length();
        for (i = 0; i < len; i++) {
            Memory.physWriteB(0xc0000 + INT10.int10.RomUsed++, string_oem.charAt(i));
        }
        Memory.physWriteB(0xc0000 + INT10.int10.RomUsed++, 0);// null 문자 입력

        switch (DOSBox.SVGACard) {
            case S3Trio:
                break;
        }
        _callback.setwindow = Callback.allocate();
        _callback.pmPalette = Callback.allocate();
        _callback.pmStart = Callback.allocate();
        Callback.setup(_callback.setwindow, VESA::setWindow, Callback.Symbol.RETF,
                "VESA Real Set Window");
        /* Prepare the pmode interface */
        INT10.int10.RomPModeInterface = Memory.realMake(0xc000, INT10.int10.RomUsed);
        INT10.int10.RomUsed += 8; // Skip the byte later used for offsets
        /* PM Set Window call */
        INT10.int10.RomPModeInterfaceWindow =
                0xffff & (INT10.int10.RomUsed - Memory.realOff(INT10.int10.RomPModeInterface));
        Memory.physWriteW(Memory.real2Phys(INT10.int10.RomPModeInterface) + 0,
                INT10.int10.RomPModeInterfaceWindow);
        _callback.pmWindow = Callback.allocate();
        INT10.int10.RomUsed +=
                Callback.setup(_callback.pmWindow, VESA::pmSetWindow, Callback.Symbol.RETN,
                        Memory.physMake(0xc000, INT10.int10.RomUsed), "VESA PM Set Window");
        /* PM Set start call */
        INT10.int10.RomPModeInterfaceStart =
                0xffff & (INT10.int10.RomUsed - Memory.realOff(INT10.int10.RomPModeInterface));
        Memory.physWriteW(Memory.real2Phys(INT10.int10.RomPModeInterface) + 2,
                INT10.int10.RomPModeInterfaceStart);
        _callback.pmStart = Callback.allocate();
        INT10.int10.RomUsed +=
                Callback.setup(_callback.pmStart, VESA::pmSetStart, Callback.Symbol.RETN,
                        Memory.physMake(0xc000, INT10.int10.RomUsed), "VESA PM Set Start");
        /* PM Set Palette call */
        INT10.int10.RomPModeInterfacePalette =
                0xffff & (INT10.int10.RomUsed - Memory.realOff(INT10.int10.RomPModeInterface));
        Memory.physWriteW(Memory.real2Phys(INT10.int10.RomPModeInterface) + 4,
                INT10.int10.RomPModeInterfacePalette);
        _callback.pmPalette = Callback.allocate();
        INT10.int10.RomUsed +=
                Callback.setup(_callback.pmPalette, VESA::pmSetPalette, Callback.Symbol.RETN,
                        Memory.physMake(0xc000, INT10.int10.RomUsed), "VESA PM Set Palette");
        /* Finalize the size and clear the required ports pointer */
        Memory.physWriteW(Memory.real2Phys(INT10.int10.RomPModeInterface) + 6, 0);
        INT10.int10.RomPModeInterfaceSize =
                0xffff & (INT10.int10.RomUsed - Memory.realOff(INT10.int10.RomPModeInterface));
    }

}
/*--------------------------- end INT10Vesa -----------------------------*/
