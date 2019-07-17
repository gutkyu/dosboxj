package org.gutkyu.dosboxj.interrupt.int10;

import org.gutkyu.dosboxj.hardware.io.*;
import org.gutkyu.dosboxj.hardware.memory.*;
import org.gutkyu.dosboxj.hardware.video.*;
import org.gutkyu.dosboxj.util.*;

/*--------------------------- begin INT10Misc -----------------------------*/
// EGA Register Interface Library
final class EGARIL {
    public static void getFuncStateInformation(int save) {
        /* set static state pointer */
        Memory.writeD(save, INT10.int10.RomStaticState);
        /* Copy BIOS Segment areas */
        short i;

        /* First area in Bios Seg */
        for (i = 0; i < 0x1e; i++) {
            Memory.writeB(save + 0x4 + i,
                    Memory.realReadB(INT10.BIOSMEM_SEG, (INT10.BIOSMEM_CURRENT_MODE + i)));
        }
        /* Second area */
        Memory.writeB(save + 0x22,
                Memory.realReadB(INT10.BIOSMEM_SEG, INT10.BIOSMEM_NB_ROWS) + 1);
        for (i = 1; i < 3; i++) {
            Memory.writeB(save + 0x22 + i,
                    Memory.realReadB(INT10.BIOSMEM_SEG, (byte) (INT10.BIOSMEM_NB_ROWS + i)));
        }
        /* Zero out rest of block */
        for (i = 0x25; i < 0x40; i++)
            Memory.writeB(save + i, 0);
        /* DCC */
        // MemModule.mem_writeb(save+0x25,MemModule.real_readb(INT10.BIOSMEM_SEG,INT10.BIOSMEM_DCC_INDEX));
        byte dccode = 0x00;
        int vsavept = (int) Memory.realReadD(INT10.BIOSMEM_SEG, INT10.BIOSMEM_VS_POINTER);
        int svstable =
                (int) Memory.realReadD(Memory.realSeg(vsavept), Memory.realOff(vsavept) + 0x10);
        if (svstable != 0) {
            int dcctable = (int) Memory.realReadD(Memory.realSeg(svstable),
                    Memory.realOff(svstable) + 0x02);
            byte entries = (byte) Memory.realReadB(Memory.realSeg(dcctable),
                    Memory.realOff(dcctable) + 0x00);
            int idx = Memory.realReadB(INT10.BIOSMEM_SEG, INT10.BIOSMEM_DCC_INDEX);
            // check if index within range
            if (idx < entries) {
                int dccentry = Memory.realReadW(Memory.realSeg(dcctable),
                        Memory.realOff(dcctable) + 0x04 + idx * 2);
                if ((dccentry & 0xff) == 0)
                    dccode = (byte) ((dccentry >>> 8) & 0xff);
                else
                    dccode = (byte) (dccentry & 0xff);
            }
        }
        Memory.writeB(save + 0x25, dccode);

        short col_count = 0;
        switch (INT10Mode.CurMode.Type) {
            case TEXT:
                if (INT10Mode.CurMode.Mode == 0x7)
                    col_count = 1;
                else
                    col_count = 16;
                break;
            case CGA2:
                col_count = 2;
                break;
            case CGA4:
                col_count = 4;
                break;
            case EGA:
                if (INT10Mode.CurMode.Mode == 0x11 || INT10Mode.CurMode.Mode == 0x0f)
                    col_count = 2;
                else
                    col_count = 16;
                break;
            case VGA:
                col_count = 256;
                break;
            default:
                Log.logging(Log.LogTypes.INT10, Log.LogServerities.Error,
                        "Get Func State illegal mode type %d", INT10Mode.CurMode.Type.toString());
                break;
        }
        /* Colour count */
        Memory.writeW(save + 0x27, col_count);
        /* Page count */
        Memory.writeB(save + 0x29, INT10Mode.CurMode.PTotal);
        /* scan lines */
        switch (INT10Mode.CurMode.SHeight) {
            case 200:
                Memory.writeB(save + 0x2a, 0);
                break;
            case 350:
                Memory.writeB(save + 0x2a, 1);
                break;
            case 400:
                Memory.writeB(save + 0x2a, 2);
                break;
            case 480:
                Memory.writeB(save + 0x2a, 3);
                break;
        }
        /* misc flags */
        if (INT10Mode.CurMode.Type == VGAModes.TEXT)
            Memory.writeB(save + 0x2d, 0x21);
        else
            Memory.writeB(save + 0x2d, 0x01);
        /* Video Memory available */
        Memory.writeB(save + 0x31, 3);
    }

    public static int getVersionPt() {
        /*
         * points to a graphics ROM location at the moment as checks test for bx!=0 only
         */
        return Memory.realMake(0xc000, 0x30);
    }

    // private static int getEGA_RIL_Regs(short dx, ref int port, ref int regs)
    private static int getEGA_RIL_port(int dx) {
        int port = 0;
        switch (dx) {
            case 0x00: /* CRT Controller (25 reg) 3B4h mono modes, 3D4h color modes */
                port = Memory.realReadW(INT10.BIOSMEM_SEG, INT10.BIOSMEM_CRTC_ADDRESS);
                break;
            case 0x08: /* Sequencer (5 registers) 3C4h */
                port = 0x3C4;
                break;
            case 0x10: /* Graphics Controller (9 registers) 3CEh */
                port = 0x3CE;
                break;
            case 0x18: /* Attribute Controller (20 registers) 3C0h */
                port = 0x3c0;
                break;
            case 0x20: /* Miscellaneous Output register 3C2h */
                port = 0x3C2;
                break;
            case 0x28: /* Feature Control register (3BAh mono modes, 3DAh color modes) */
                port = Memory.realReadW(INT10.BIOSMEM_SEG, INT10.BIOSMEM_CRTC_ADDRESS) + 6;
                break;
            case 0x30: /* Graphics 1 Position register 3CCh */
                port = 0x3CC;
                break;
            case 0x38: /* Graphics 2 Position register 3CAh */
                port = 0x3CA;
                break;
            default:
                Log.logging(Log.LogTypes.INT10, Log.LogServerities.Error,
                        "unknown RIL port selection %X", dx);
                break;
        }
        return port;
    }

    private static int getEGA_RIL_Regs(int dx) {
        int regs = 0; // if nul is returned it's a single register port
        switch (dx) {
            case 0x00: /* CRT Controller (25 reg) 3B4h mono modes, 3D4h color modes */
                regs = 25;
                break;
            case 0x08: /* Sequencer (5 registers) 3C4h */
                regs = 5;
                break;
            case 0x10: /* Graphics Controller (9 registers) 3CEh */
                regs = 9;
                break;
            case 0x18: /* Attribute Controller (20 registers) 3C0h */
                regs = 20;
                break;
            case 0x20: /* Miscellaneous Output register 3C2h */
                break;
            case 0x28: /* Feature Control register (3BAh mono modes, 3DAh color modes) */
                break;
            case 0x30: /* Graphics 1 Position register 3CCh */
                break;
            case 0x38: /* Graphics 2 Position register 3CAh */
                break;
            default:
                Log.logging(Log.LogTypes.INT10, Log.LogServerities.Error,
                        "unknown RIL port selection %X", dx);
                break;
        }
        return regs;
    }

    // byte ReadRegister(byte, uint16)
    public static int readRegister(int bl, int dx) {
        int port = getEGA_RIL_port(dx);
        int regs = getEGA_RIL_Regs(dx);

        if (regs == 0) {
            if (port != 0)
                bl = IO.read(port);
        } else {
            if (port == 0x3c0)
                IO.read(Memory.realReadW(INT10.BIOSMEM_SEG, INT10.BIOSMEM_CRTC_ADDRESS) + 6);
            IO.write(port, bl);
            bl = IO.read(port + 1);
            if (port == 0x3c0)
                IO.read(Memory.realReadW(INT10.BIOSMEM_SEG, INT10.BIOSMEM_CRTC_ADDRESS) + 6);
            Log.logging(Log.LogTypes.INT10, Log.LogServerities.Normal,
                    "EGA RIL read used with multi-reg");
        }
        return bl;
    }

    public static int writeRegister(int bl, int bh, int dx) {
        int port = getEGA_RIL_port(dx);
        int regs = getEGA_RIL_Regs(dx);
        if (regs == 0) {
            if (port != 0)
                IO.write(port, bl);
        } else {
            if (port == 0x3c0) {
                IO.read(Memory.realReadW(INT10.BIOSMEM_SEG, INT10.BIOSMEM_CRTC_ADDRESS) + 6);
                IO.write(port, bl);
                IO.write(port, bh);
            } else {
                IO.write(port, bl);
                IO.write(port + 1, bh);
            }
            bl = bh;// Not sure
            Log.logging(Log.LogTypes.INT10, Log.LogServerities.Normal,
                    "EGA RIL write used with multi-reg");
        }
        return bl;
    }

    public static void readRegisterRange(int ch, int cl, int dx, int dst) {
        int port = getEGA_RIL_port(dx);
        int regs = getEGA_RIL_Regs(dx);
        if (regs == 0) {
            Log.logging(Log.LogTypes.INT10, Log.LogServerities.Error,
                    "EGA RIL range read with port %x called", port);
        } else {
            if (ch < regs) {
                if (ch + cl > regs)
                    cl = 0xff & (regs - ch);
                for (int i = 0; i < cl; i++) {
                    if (port == 0x3c0)
                        IO.read(Memory.realReadW(INT10.BIOSMEM_SEG, INT10.BIOSMEM_CRTC_ADDRESS)
                                + 6);
                    IO.write(port, 0xff & (ch + i));
                    Memory.writeB(dst++, IO.read(port + 1));
                }
                if (port == 0x3c0)
                    IO.read(Memory.realReadW(INT10.BIOSMEM_SEG, INT10.BIOSMEM_CRTC_ADDRESS) + 6);
            }
            Log.logging(Log.LogTypes.INT10, Log.LogServerities.Error,
                    "EGA RIL range read from %x for invalid register %x", port, ch);
        }
    }

    public static void writeRegisterRange(int ch, int cl, int dx, int src) {
        int port = getEGA_RIL_port(dx);
        int regs = getEGA_RIL_Regs(dx);
        if (regs == 0) {
            Log.logging(Log.LogTypes.INT10, Log.LogServerities.Error,
                    "EGA RIL range write called with port %x", port);
        } else {
            if (ch < regs) {
                if (ch + cl > regs)
                    cl = 0xff & (regs - ch);
                if (port == 0x3c0) {
                    IO.read(Memory.realReadW(INT10.BIOSMEM_SEG, INT10.BIOSMEM_CRTC_ADDRESS) + 6);
                    for (int i = 0; i < cl; i++) {
                        IO.write(port, 0xff & (ch + i));
                        IO.write(port, Memory.readB(src++));
                    }
                } else {
                    for (int i = 0; i < cl; i++) {
                        IO.write(port, 0xff & (ch + i));
                        IO.write(port + 1, Memory.readB(src++));
                    }
                }
            }
            Log.logging(Log.LogTypes.INT10, Log.LogServerities.Error,
                    "EGA RIL range write to %x with invalid register %x", port, ch);
        }
    }

    /*
     * register sets are of the form offset 0 (word): group index offset 2 (byte): register number
     * (0 for single registers, ignored) offset 3 (byte): register value (return value when reading)
     */
    public static void readRegisterSet(int cx, int tbl) {
        int vl;
        /* read cx register sets */
        for (int i = 0; i < cx; i++) {
            vl = Memory.readB(tbl + 2);
            vl = readRegister(vl, Memory.readW(tbl));
            Memory.writeB(tbl + 3, vl);
            tbl += 4;
        }
    }

    public static void writeRegisterSet(int cx, int tbl) {
        /* write cx register sets */
        int dx = 0;// uint16
        int port = 0;
        int regs = 0;
        for (int i = 0; i < cx; i++) {
            dx = Memory.readW(tbl);
            port = getEGA_RIL_port(dx);
            regs = getEGA_RIL_Regs(dx);
            int vl = Memory.readB(tbl + 3);
            if (regs == 0) {
                if (port != 0)
                    IO.write(port, vl);
            } else {
                int idx = Memory.readB(tbl + 2);
                if (port == 0x3c0) {
                    IO.read(Memory.realReadW(INT10.BIOSMEM_SEG, INT10.BIOSMEM_CRTC_ADDRESS) + 6);
                    IO.write(port, idx);
                    IO.write(port, vl);
                } else {
                    IO.write(port, idx);
                    IO.write(port + 1, vl);
                }
            }
            tbl += 4;
        }
    }

}
/*--------------------------- end INT10Misc -----------------------------*/
