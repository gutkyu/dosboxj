package org.gutkyu.dosboxj.interrupt.int10;

import org.gutkyu.dosboxj.cpu.*;
import org.gutkyu.dosboxj.hardware.io.*;
import org.gutkyu.dosboxj.hardware.memory.*;
import org.gutkyu.dosboxj.hardware.video.VGAModes;
import org.gutkyu.dosboxj.misc.setup.*;
import org.gutkyu.dosboxj.util.*;
import org.gutkyu.dosboxj.*;

public final class INT10 {

    static final int S3_LFB_BASE = 0xC0000000;

    public static final int BIOSMEM_SEG = 0x40;

    static final int BIOSMEM_INITIAL_MODE = 0x10;
    static final int BIOSMEM_CURRENT_MODE = 0x49;
    public static final int BIOSMEM_NB_COLS = 0x4A;
    static final int BIOSMEM_PAGE_SIZE = 0x4C;
    static final int BIOSMEM_CURRENT_START = 0x4E;
    static final int BIOSMEM_CURSOR_POS = 0x50;
    static final int BIOSMEM_CURSOR_TYPE = 0x60;
    public static final int BIOSMEM_CURRENT_PAGE = 0x62;
    static final int BIOSMEM_CRTC_ADDRESS = 0x63;
    static final int BIOSMEM_CURRENT_MSR = 0x65;
    static final int BIOSMEM_CURRENT_PAL = 0x66;
    public static final int BIOSMEM_NB_ROWS = 0x84;
    static final int BIOSMEM_CHAR_HEIGHT = 0x85;
    static final int BIOSMEM_VIDEO_CTL = 0x87;
    static final int BIOSMEM_SWITCHES = 0x88;
    static final int BIOSMEM_MODESET_CTL = 0x89;
    static final int BIOSMEM_DCC_INDEX = 0x8A;
    static final int BIOSMEM_CRTCPU_PAGE = 0x8A;
    static final int BIOSMEM_VS_POINTER = 0xA8;

    public static byte[] int10Font08, int10Font14, int10Font16;
    static {
        int10Font08 = Resources.get("INT10Font08");
        int10Font14 = Resources.get("INT10Font14");
        int10Font16 = Resources.get("INT10Font16");
    }

    // uint8(uint8)
    public static int getCursorPosCol(int page) {
        return Memory.realReadB(BIOSMEM_SEG, BIOSMEM_CURSOR_POS + page * 2);
    }

    // uint8(uint8)
    public static int getCursorPosRow(int page) {
        return Memory.realReadB(BIOSMEM_SEG, BIOSMEM_CURSOR_POS + page * 2 + 1);
    }

    public static class Int10Data {
        public int RomFont8First;
        public int RomFont8Second;
        public int RomFont14;
        public int RomFont16;
        public int RomFont14Alternate;
        public int RomFont16Alternate;
        public int RomStaticState;
        public int RomVideoSavePointers;
        public int RomVideoParameterTable;
        public int RomVideoSavePointerTable;
        public int RomVideoDccTable;
        public int RomOEMString;
        public int RomVesaModes;
        public int RomPModeInterface;
        public int RomPModeInterfaceSize;// uint16
        public int RomPModeInterfaceStart;// uint16
        public int RomPModeInterfaceWindow;// uint16
        public int RomPModeInterfacePalette;// uint16
        public short RomUsed;

        public int VesaSetMode;
        public boolean VesaNoLFB;
        public boolean VesaOldVbe;
    }

    public static final Int10Data int10 = new Int10Data();
    private static int call10;
    private static boolean warnedFF = false;



    private static int INT10Handler() {
        switch (Register.getRegAH()) {
            case 0x00: /* Set VideoMode */
                INT10Mode.setVideoMode(Register.getRegAL());
                break;
            case 0x01: /* Set TextMode Cursor Shape */
                CHAR.setCursorShape(0xff & Register.getRegCH(), 0xff & Register.getRegCL());
                break;
            case 0x02: /* Set Cursor Pos */
                CHAR.setCursorPos(Register.getRegDH(), Register.getRegDL(), Register.getRegBH());
                break;
            case 0x03: /* get Cursor Pos and Cursor Shape */
                // regsModule.reg_ah=0;
                Register.setRegDL(getCursorPosCol(Register.getRegBH()));
                Register.setRegDH(getCursorPosRow(Register.getRegBH()));
                Register.setRegCX(Memory.realReadW(BIOSMEM_SEG, BIOSMEM_CURSOR_TYPE));
                break;
            case 0x04: /* read light pen pos YEAH RIGHT */
                /* Light pen is not supported */
                Register.setRegAX(0);
                break;
            case 0x05: /* Set Active Page */
                if ((Register.getRegAL() & 0x80) != 0 && DOSBox.isTANDYArch()) {
                    int crtCPU = Memory.realReadB(BIOSMEM_SEG, BIOSMEM_CRTCPU_PAGE);
                    switch (Register.getRegAL()) {
                        case 0x80:
                            Register.setRegBH(crtCPU & 7);
                            Register.setRegBL((crtCPU >>> 3) & 0x7);
                            break;
                        case 0x81:
                            crtCPU = 0xff & ((crtCPU & 0xc7) | ((Register.getRegBL() & 7) << 3));
                            break;
                        case 0x82:
                            crtCPU = 0xff & ((crtCPU & 0xf8) | (Register.getRegBH() & 7));
                            break;
                        case 0x83:
                            crtCPU = 0xff & ((crtCPU & 0xc0) | (Register.getRegBH() & 7)
                                    | ((Register.getRegBL() & 7) << 3));
                            break;
                    }
                    if (DOSBox.Machine == DOSBox.MachineType.PCJR) {
                        /* always return graphics mapping, even for invalid values of AL */
                        Register.setRegBH(crtCPU & 7);
                        Register.setRegBL((crtCPU >>> 3) & 0x7);
                    }
                    IO.writeB(0x3df, crtCPU);
                    Memory.realWriteB(BIOSMEM_SEG, BIOSMEM_CRTCPU_PAGE, crtCPU);
                } else
                    CHAR.setActivePage(Register.getRegAL());
                break;
            case 0x06: /* Scroll Up */
                // CHAR.ScrollWindow(Register.getRegCH(), Register.getRegCL(), Register.getRegDH(),
                // Register.getRegDL(), (sbyte)-Register.getRegAL(), Register.getRegBH(), 0xFF);
                CHAR.scrollWindow(Register.getRegCH(), Register.getRegCL(), Register.getRegDH(),
                        Register.getRegDL(), -Register.getRegAL(), Register.getRegBH(), 0xFF);
                break;
            case 0x07: /* Scroll Down */
                // CHAR.ScrollWindow(Register.getRegCH(), Register.getRegCL(), Register.getRegDH(),
                // Register.getRegDL(), (sbyte)Register.getRegAL(), Register.getRegBH(), 0xFF);
                CHAR.scrollWindow(Register.getRegCH(), Register.getRegCL(), Register.getRegDH(),
                        Register.getRegDL(), Register.getRegAL(), Register.getRegBH(), 0xFF);
                break;
            case 0x08: /* Read character & attribute at cursor */
            {
                int refVal = CHAR.readCharAttr(Register.getRegBH());
                Register.setRegAX(refVal);
                break;
            }
            case 0x09: /* Write Character & Attribute at cursor CX times */
                CHAR.writeChar2(Register.getRegAL(), Register.getRegBL(), Register.getRegBH(),
                        Register.getRegCX(), true);
                break;
            case 0x0A: /* Write Character at cursor CX times */
                CHAR.writeChar2(Register.getRegAL(), Register.getRegBL(), Register.getRegBH(),
                        Register.getRegCX(), false);
                break;
            case 0x0B: /* Set Background/Border Colour & Set Palette */
                switch (Register.getRegBH()) {
                    case 0x00: // Background/Border color
                        PAL.setBackgroundBorder(Register.getRegBL());
                        break;
                    case 0x01: // Set color Select
                    default:
                        PAL.setColorSelect(Register.getRegBL());
                        break;
                }
                break;
            case 0x0C: /* Write Graphics Pixel */
                putPixel(Register.getRegCX(), Register.getRegDX(), Register.getRegBH(),
                        (byte) Register.getRegAL());
                break;
            case 0x0D: /* Read Graphics Pixel */
            {
                if (canGetPixel())
                    Register.setRegAL(getPixel(Register.getRegCX(), Register.getRegDX(),
                            Register.getRegBH()));
                break;
            }
            case 0x0E: /* Teletype OutPut */
                CHAR.teletypeOutput((byte) Register.getRegAL(), Register.getRegBL());
                break;
            case 0x0F: /* Get videomode */
                Register.setRegBH(Memory.realReadB(BIOSMEM_SEG, BIOSMEM_CURRENT_PAGE));
                Register.setRegAL(Memory.realReadB(BIOSMEM_SEG, BIOSMEM_CURRENT_MODE)
                        | (Memory.realReadB(BIOSMEM_SEG, BIOSMEM_VIDEO_CTL) & 0x80));
                Register.setRegAH(0xff & Memory.realReadW(BIOSMEM_SEG, BIOSMEM_NB_COLS));
                break;
            case 0x10: /* Palette functions */
                if ((DOSBox.Machine == DOSBox.MachineType.PCJR)
                        || ((!DOSBox.isVGAArch()) && (Register.getRegAL() > 0x02)))
                    break;
                // TODO: subfunction 0x03 for ega
                switch (Register.getRegAL()) {
                    case 0x00: /* SET SINGLE PALETTE REGISTER */
                        PAL.setSinglePaletteRegister(Register.getRegBL(), Register.getRegBH());
                        break;
                    case 0x01: /* SET BORDER (OVERSCAN) COLOR */
                        PAL.setOverscanBorderColor(Register.getRegBH());
                        break;
                    case 0x02: /* SET ALL PALETTE REGISTERS */
                        PAL.setAllPaletteRegisters(
                                Register.segPhys(Register.SEG_NAME_ES) + Register.getRegDX());
                        break;
                    case 0x03: /* TOGGLE INTENSITY/BLINKING BIT */
                        PAL.toggleBlinkingBit(Register.getRegBL());
                        break;
                    case 0x07: /* GET SINGLE PALETTE REGISTER */
                    {
                        int regVal = PAL.getSinglePaletteRegister(Register.getRegBL());
                        Register.setRegBH(regVal);
                        break;
                    }
                    case 0x08: /* READ OVERSCAN (BORDER COLOR) REGISTER */
                    {
                        int regVal = PAL.getOverscanBorderColor();
                        Register.setRegBH(regVal);
                        break;
                    }
                    case 0x09: /* READ ALL PALETTE REGISTERS AND OVERSCAN REGISTER */
                        PAL.getAllPaletteRegisters(
                                Register.segPhys(Register.SEG_NAME_ES) + Register.getRegDX());
                        break;
                    case 0x10: /* SET INDIVIDUAL DAC REGISTER */
                        PAL.setSingleDacRegister(Register.getRegBL(), Register.getRegDH(),
                                Register.getRegCH(), Register.getRegCL());
                        break;
                    case 0x12: /* SET BLOCK OF DAC REGISTERS */
                        PAL.setDACBlock(Register.getRegBX(), Register.getRegCX(),
                                Register.segPhys(Register.SEG_NAME_ES) + Register.getRegDX());
                        break;
                    case 0x13: /* SELECT VIDEO DAC COLOR PAGE */
                        PAL.selectDACPage(Register.getRegBL(), Register.getRegBH());
                        break;
                    case 0x15: /* GET INDIVIDUAL DAC REGISTER */
                    {
                        PAL.getSingleDacRegisterReady(Register.getRegBL());
                        Register.setRegDH(PAL.getSingleDacRegisterRed());
                        Register.setRegCH(PAL.getSingleDacRegisterGreen());
                        Register.setRegCL(PAL.getSingleDacRegisterBlue());
                        break;
                    }
                    case 0x17: /* GET BLOCK OF DAC REGISTER */
                        PAL.getDACBlock(Register.getRegBX(), Register.getRegCX(),
                                Register.segPhys(Register.SEG_NAME_ES) + Register.getRegDX());
                        break;
                    case 0x18: /* undocumented - SET PEL MASK */
                        PAL.setPelMask(Register.getRegBL());
                        break;
                    case 0x19: /* undocumented - GET PEL MASK */
                    {
                        int mask = PAL.getPelMask();
                        Register.setRegBH(0); // bx for get mask
                        Register.setRegBL(mask);
                        break;
                    }
                    case 0x1A: /* GET VIDEO DAC COLOR PAGE */
                    {
                        int mode = PAL.getDACPageMode();
                        int page = PAL.getDACPage(mode);
                        Register.setRegBL(mode);
                        Register.setRegBH(page);
                        break;
                    }
                    case 0x1B: /* PERFORM GRAY-SCALE SUMMING */
                        PAL.performGrayScaleSumming(Register.getRegBX(), Register.getRegCX());
                        break;
                    case 0xF0: /* ET4000: SET HiColor GRAPHICS MODE */
                    case 0xF1: /* ET4000: GET DAC TYPE */
                    case 0xF2: /* ET4000: CHECK/SET HiColor MODE */
                    default:
                        Log.logging(Log.LogTypes.INT10, Log.LogServerities.Error,
                                "Function 10:Unhandled EGA/VGA Palette Function %2X",
                                Register.getRegAL());
                        break;
                }
                break;
            case 0x11: /* Character generator functions */
                if (!DOSBox.isEGAVGAArch())
                    break;
                switch (Register.getRegAL()) {
                    /* Textmode calls */
                    case 0x00: /* Load user font */
                    case 0x10:
                        loadFont(Register.segPhys(Register.SEG_NAME_ES) + Register.getRegBP(),
                                Register.getRegAL() == 0x10, Register.getRegCX(),
                                Register.getRegDX(), Register.getRegBL(), Register.getRegBH());
                        break;
                    case 0x01: /* Load 8x14 font */
                    case 0x11:
                        loadFont(Memory.real2Phys(int10.RomFont14), Register.getRegAL() == 0x11,
                                256, 0, 0, 14);
                        break;
                    case 0x02: /* Load 8x8 font */
                    case 0x12:
                        loadFont(Memory.real2Phys(int10.RomFont8First), Register.getRegAL() == 0x12,
                                256, 0, 0, 8);
                        break;
                    case 0x03: /* Set Block Specifier */
                        IO.write(0x3c4, 0x3);
                        IO.write(0x3c5, Register.getRegBL());
                        break;
                    case 0x04: /* Load 8x16 font */
                    case 0x14:
                        if (!DOSBox.isVGAArch())
                            break;
                        loadFont(Memory.real2Phys(int10.RomFont16), Register.getRegAL() == 0x14,
                                256, 0, 0, 16);
                        break;
                    /* Graphics mode calls */
                    case 0x20: /* Set User 8x8 Graphics characters */
                        Memory.realSetVec(0x1f, Memory.realMake(
                                Register.segValue(Register.SEG_NAME_ES), Register.getRegBP()));
                        break;
                    case 0x21: /* Set user graphics characters */
                        Memory.realSetVec(0x43, Memory.realMake(
                                Register.segValue(Register.SEG_NAME_ES), Register.getRegBP()));
                        Memory.realWriteW(BIOSMEM_SEG, BIOSMEM_CHAR_HEIGHT, Register.getRegCX());
                        // goto graphics_chars;
                        graphicsChars();
                        break;
                    case 0x22: /* Rom 8x14 set */
                        Memory.realSetVec(0x43, int10.RomFont14);
                        Memory.realWriteW(BIOSMEM_SEG, BIOSMEM_CHAR_HEIGHT, 14);
                        // goto graphics_chars;
                        graphicsChars();
                        break;
                    case 0x23: /* Rom 8x8 double dot set */
                        Memory.realSetVec(0x43, int10.RomFont8First);
                        Memory.realWriteW(BIOSMEM_SEG, BIOSMEM_CHAR_HEIGHT, 8);
                        // goto graphics_chars;
                        graphicsChars();
                        break;
                    case 0x24: /* Rom 8x16 set */
                        if (!DOSBox.isVGAArch())
                            break;
                        Memory.realSetVec(0x43, int10.RomFont16);
                        Memory.realWriteW(BIOSMEM_SEG, BIOSMEM_CHAR_HEIGHT, 16);
                        graphicsChars();
                        break;
                    // goto graphics_chars;
                    // graphics_chars:
                    // break;
                    /* General */
                    case 0x30:/* Get Font Information */
                        switch (Register.getRegBH()) {
                            case 0x00: /* interupt 0x1f vector */
                            {
                                int int_1f = Memory.realGetVec(0x1f);
                                Register.segSet16(Register.SEG_NAME_ES, Memory.realSeg(int_1f));
                                Register.setRegBP(Memory.realOff(int_1f));
                            }
                                break;
                            case 0x01: /* interupt 0x43 vector */
                            {
                                int int_43 = Memory.realGetVec(0x43);
                                Register.segSet16(Register.SEG_NAME_ES, Memory.realSeg(int_43));
                                Register.setRegBP(Memory.realOff(int_43));
                            }
                                break;
                            case 0x02: /* font 8x14 */
                                Register.segSet16(Register.SEG_NAME_ES,
                                        Memory.realSeg(int10.RomFont14));
                                Register.setRegBP(Memory.realOff(int10.RomFont14));
                                break;
                            case 0x03: /* font 8x8 first 128 */
                                Register.segSet16(Register.SEG_NAME_ES,
                                        Memory.realSeg(int10.RomFont8First));
                                Register.setRegBP(Memory.realOff(int10.RomFont8First));
                                break;
                            case 0x04: /* font 8x8 second 128 */
                                Register.segSet16(Register.SEG_NAME_ES,
                                        Memory.realSeg(int10.RomFont8Second));
                                Register.setRegBP(Memory.realOff(int10.RomFont8Second));
                                break;
                            case 0x05: /* alpha alternate 9x14 */
                                if (!DOSBox.isVGAArch())
                                    break;
                                Register.segSet16(Register.SEG_NAME_ES,
                                        Memory.realSeg(int10.RomFont14Alternate));
                                Register.setRegBP(Memory.realOff(int10.RomFont14Alternate));
                                break;
                            case 0x06: /* font 8x16 */
                                if (!DOSBox.isVGAArch())
                                    break;
                                Register.segSet16(Register.SEG_NAME_ES,
                                        Memory.realSeg(int10.RomFont16));
                                Register.setRegBP(Memory.realOff(int10.RomFont16));
                                break;
                            case 0x07: /* alpha alternate 9x16 */
                                if (!DOSBox.isVGAArch())
                                    break;
                                Register.segSet16(Register.SEG_NAME_ES,
                                        Memory.realSeg(int10.RomFont16Alternate));
                                Register.setRegBP(Memory.realOff(int10.RomFont16Alternate));
                                break;
                            default:
                                Log.logging(Log.LogTypes.INT10, Log.LogServerities.Error,
                                        "Function 11:30 Request for font %2X", Register.getRegBH());
                                break;
                        }
                        if ((Register.getRegBH() <= 7)
                                || (DOSBox.SVGACard == DOSBox.SVGACards.TsengET4K)) {
                            if (DOSBox.Machine == DOSBox.MachineType.EGA) {
                                Register.setRegCX(0x0e);
                                Register.setRegDL(0x18);
                            } else {
                                Register.setRegCX(
                                        Memory.realReadW(BIOSMEM_SEG, BIOSMEM_CHAR_HEIGHT));
                                Register.setRegDL(Memory.realReadB(BIOSMEM_SEG, BIOSMEM_NB_ROWS));
                            }
                        }
                        break;
                    default:
                        Log.logging(Log.LogTypes.INT10, Log.LogServerities.Error,
                                "Function 11:Unsupported character generator call %2X",
                                Register.getRegAL());
                        break;
                }
                break;
            case 0x12: /* alternate function select */
                if (!DOSBox.isEGAVGAArch())
                    break;
                switch (Register.getRegBL()) {
                    case 0x10: /* Get EGA Information */
                        Register.setRegBH(
                                (Memory.realReadW(BIOSMEM_SEG, BIOSMEM_CRTC_ADDRESS) == 0x3B4 ? 1
                                        : 0));
                        Register.setRegBL(3); // 256 kb
                        Register.setRegCL(Memory.realReadB(BIOSMEM_SEG, BIOSMEM_SWITCHES) & 0x0F);
                        Register.setRegCH(Memory.realReadB(BIOSMEM_SEG, BIOSMEM_SWITCHES) >>> 4);
                        break;
                    case 0x20: /* Set alternate printscreen */
                        break;
                    case 0x30: /* Select vertical resolution */
                    {
                        if (!DOSBox.isVGAArch())
                            break;
                        Log.logging(Log.LogTypes.INT10, Log.LogServerities.Warn,
                                "Function 12:Call %2X (select vertical resolution)",
                                Register.getRegBL());
                        if (DOSBox.SVGACard != DOSBox.SVGACards.None) {
                            if (Register.getRegAL() > 2) {
                                Register.setRegAL(0); // invalid subfunction
                                break;
                            }
                        }
                        int modesetCTL = Memory.realReadB(BIOSMEM_SEG, BIOSMEM_MODESET_CTL);
                        int videoSwitches = Memory.realReadB(BIOSMEM_SEG, BIOSMEM_SWITCHES) & 0xf0;
                        switch (Register.getRegAL()) {
                            case 0: // 200
                                modesetCTL &= 0xef;
                                modesetCTL |= 0x80;
                                videoSwitches |= 8; // ega normal/cga emulation
                                break;
                            case 1: // 350
                                modesetCTL &= 0x6f;
                                videoSwitches |= 9; // ega enhanced
                                break;
                            case 2: // 400
                                modesetCTL &= 0x6f;
                                modesetCTL |= 0x10; // use 400-line mode at next mode set
                                videoSwitches |= 9; // ega enhanced
                                break;
                            default:
                                modesetCTL &= 0xef;
                                videoSwitches |= 8; // ega normal/cga emulation
                                break;
                        }
                        Memory.realWriteB(BIOSMEM_SEG, BIOSMEM_MODESET_CTL, modesetCTL);
                        Memory.realWriteB(BIOSMEM_SEG, BIOSMEM_SWITCHES, videoSwitches);
                        Register.setRegAL(0x12); // success
                        break;
                    }
                    case 0x31: /* Palette loading on modeset */
                    {
                        if (!DOSBox.isVGAArch())
                            break;
                        if (DOSBox.SVGACard == DOSBox.SVGACards.TsengET4K)
                            Register.setRegAL(Register.getRegAL() & 1);
                        if (Register.getRegAL() > 1) {
                            Register.setRegAL(0); // invalid subfunction
                            break;
                        }
                        int temp = Memory.realReadB(BIOSMEM_SEG, BIOSMEM_MODESET_CTL) & 0xf7;
                        if ((Register.getRegAL() & 1) != 0)
                            temp |= 8; // enable if al=0
                        Memory.realWriteB(BIOSMEM_SEG, BIOSMEM_MODESET_CTL, temp);
                        Register.setRegAL(0x12);
                        break;
                    }
                    case 0x32: /* Video adressing */
                        if (!DOSBox.isVGAArch())
                            break;
                        Log.logging(Log.LogTypes.INT10, Log.LogServerities.Error,
                                "Function 12:Call %2X not handled", Register.getRegBL());
                        if (DOSBox.SVGACard == DOSBox.SVGACards.TsengET4K)
                            Register.setRegAL(Register.getRegAL() & 1);
                        if (Register.getRegAL() > 1)
                            Register.setRegAL(0); // invalid subfunction
                        else
                            Register.setRegAL(0x12); // fake a success call
                        break;
                    case 0x33: /* SWITCH GRAY-SCALE SUMMING */
                    {
                        if (!DOSBox.isVGAArch())
                            break;
                        if (DOSBox.SVGACard == DOSBox.SVGACards.TsengET4K)
                            Register.setRegAL(Register.getRegAL() & 1);
                        if (Register.getRegAL() > 1) {
                            Register.setRegAL(0);
                            break;
                        }
                        int temp = Memory.realReadB(BIOSMEM_SEG, BIOSMEM_MODESET_CTL) & 0xfd;
                        if ((Register.getRegAL() & 1) == 0)
                            temp |= 2; // enable if al=0
                        Memory.realWriteB(BIOSMEM_SEG, BIOSMEM_MODESET_CTL, temp);
                        Register.setRegAL(0x12);
                        break;
                    }
                    case 0x34: /* ALTERNATE FUNCTION SELECT (VGA) - CURSOR EMULATION */
                    {
                        // bit 0: 0=enable, 1=disable
                        if (!DOSBox.isVGAArch())
                            break;
                        if (DOSBox.SVGACard == DOSBox.SVGACards.TsengET4K)
                            Register.setRegAL(Register.getRegAL() & 1);
                        if (Register.getRegAL() > 1) {
                            Register.setRegAL(0);
                            break;
                        }
                        int temp = Memory.realReadB(BIOSMEM_SEG, BIOSMEM_VIDEO_CTL) & 0xfe;
                        Memory.realWriteB(BIOSMEM_SEG, BIOSMEM_VIDEO_CTL,
                                0xff & (temp | Register.getRegAL()));
                        Register.setRegAL(0x12);
                        break;
                    }
                    case 0x35:
                        if (!DOSBox.isVGAArch())
                            break;
                        Log.logging(Log.LogTypes.INT10, Log.LogServerities.Error,
                                "Function 12:Call %2X not handled", Register.getRegBL());
                        Register.setRegAL(0x12);
                        break;
                    case 0x36: { /* VGA Refresh control */
                        if (!DOSBox.isVGAArch())
                            break;
                        if ((DOSBox.SVGACard == DOSBox.SVGACards.S3Trio)
                                && (Register.getRegAL() > 1)) {
                            Register.setRegAL(0);
                            break;
                        }
                        IO.write(0x3c4, 0x1);
                        int clocking = IO.read(0x3c5);

                        if (Register.getRegAL() == 0)
                            clocking &= 0xff & ~0x20;
                        else
                            clocking |= 0x20;

                        IO.write(0x3c4, 0x1);
                        IO.write(0x3c5, clocking);

                        Register.setRegAL(0x12); // success
                        break;
                    }
                    default:
                        Log.logging(Log.LogTypes.INT10, Log.LogServerities.Error,
                                "Function 12:Call %2X not handled", Register.getRegBL());
                        if (DOSBox.Machine != DOSBox.MachineType.EGA)
                            Register.setRegAL(0);
                        break;
                }
                break;
            case 0x13: /* Write String */
                CHAR.writeString(Register.getRegDH(), Register.getRegDL(), Register.getRegAL(),
                        Register.getRegBL(),
                        Register.segPhys(Register.SEG_NAME_ES) + Register.getRegBP(),
                        Register.getRegCX(), Register.getRegBH());
                break;
            case 0x1A: /* Display Combination */
                if (!DOSBox.isVGAArch())
                    break;
                if (Register.getRegAL() == 0) { // get dcc
                                                // walk the tables...
                    int vsavept = (int) Memory.realReadD(BIOSMEM_SEG, BIOSMEM_VS_POINTER);
                    int svstable = (int) Memory.realReadD(Memory.realSeg(vsavept),
                            Memory.realOff(vsavept) + 0x10);
                    if (svstable != 0) {
                        int dcctable = (int) Memory.realReadD(Memory.realSeg(svstable),
                                Memory.realOff(svstable) + 0x02);
                        int entries = Memory.realReadB(Memory.realSeg(dcctable),
                                Memory.realOff(dcctable) + 0x00);
                        int idx = Memory.realReadB(BIOSMEM_SEG, BIOSMEM_DCC_INDEX);
                        // check if index within range
                        if (idx < entries) {
                            int dccEntry = Memory.realReadW(Memory.realSeg(dcctable),
                                    Memory.realOff(dcctable) + 0x04 + idx * 2);
                            if ((dccEntry & 0xff) == 0)
                                Register.setRegBX(dccEntry >>> 8);
                            else
                                Register.setRegBX(dccEntry);
                        } else
                            Register.setRegBX(0xffff);
                    } else
                        Register.setRegBX(0xffff);
                    Register.setRegAX(0x1A); // high part destroyed or zeroed depending on BIOS
                } else if (Register.getRegAL() == 1) { // set dcc
                    int newIdx = 0xff;
                    // walk the tables...
                    int vsavept = (int) Memory.realReadD(BIOSMEM_SEG, BIOSMEM_VS_POINTER);
                    int svstable = (int) Memory.realReadD(Memory.realSeg(vsavept),
                            Memory.realOff(vsavept) + 0x10);
                    if (svstable != 0) {
                        int dcctable = (int) Memory.realReadD(Memory.realSeg(svstable),
                                Memory.realOff(svstable) + 0x02);
                        int entries = Memory.realReadB(Memory.realSeg(dcctable),
                                Memory.realOff(dcctable) + 0x00);
                        if (entries != 0) {
                            int ct;
                            int swpIdx = Register.getRegBH() | (Register.getRegBL() << 8);
                            // search the ddc index in the dcc table
                            for (ct = 0; ct < entries; ct++) {
                                int dccEntry = Memory.realReadW(Memory.realSeg(dcctable),
                                        Memory.realOff(dcctable) + 0x04 + ct * 2);
                                if ((dccEntry == Register.getRegBX()) || (dccEntry == swpIdx)) {
                                    newIdx = 0xff & ct;
                                    break;
                                }
                            }
                        }
                    }

                    Memory.realWriteB(BIOSMEM_SEG, BIOSMEM_DCC_INDEX, newIdx);
                    Register.setRegAX(0x1A); // high part destroyed or zeroed depending on BIOS
                }
                break;
            case 0x1B: /* functionality State Information */
                if (!DOSBox.isVGAArch())
                    break;
                switch (Register.getRegBX()) {
                    case 0x0000:
                        EGARIL.getFuncStateInformation(
                                Register.segPhys(Register.SEG_NAME_ES) + Register.getRegDI());
                        Register.setRegAL(0x1B);
                        break;
                    default:
                        Log.logging(Log.LogTypes.INT10, Log.LogServerities.Error,
                                "1B:Unhandled call BX %2X", Register.getRegBX());
                        Register.setRegAL(0);
                        break;
                }
                break;
            case 0x1C: /* Video Save Area */
                if (!DOSBox.isVGAArch())
                    break;
                switch (Register.getRegAL()) {
                    case 0: {
                        int ret = VideoState.getSize(Register.getRegCX());
                        if (ret != 0) {
                            Register.setRegAL(0x1c);
                            Register.setRegBX(ret);
                        } else
                            Register.setRegAL(0);
                    }
                        break;
                    case 1:
                        if (VideoState.save(Register.getRegCX(), Memory.realMake(
                                Register.segValue(Register.SEG_NAME_ES), Register.getRegBX())))
                            Register.setRegAL(0x1c);
                        else
                            Register.setRegAL(0);
                        break;
                    case 2:
                        if (VideoState.restore(Register.getRegCX(), Memory.realMake(
                                Register.segValue(Register.SEG_NAME_ES), Register.getRegBX())))
                            Register.setRegAL(0x1c);
                        else
                            Register.setRegAL(0);
                        break;
                    default:
                        if (DOSBox.SVGACard == DOSBox.SVGACards.TsengET4K)
                            Register.setRegAX(0);
                        else
                            Register.setRegAL(0);
                        break;
                }
                break;
            case 0x4f: /* VESA Calls */
                if ((!DOSBox.isVGAArch()) || (DOSBox.SVGACard != DOSBox.SVGACards.S3Trio))
                    break;
                switch (Register.getRegAL()) {
                    case 0x00: /* Get SVGA Information */
                        Register.setRegAL(0x4f);
                        Register.setRegAH(VESA.getSVGAInformation(
                                Register.segValue(Register.SEG_NAME_ES), Register.getRegDI()));
                        break;
                    case 0x01: /* Get SVGA Mode Information */
                        Register.setRegAL(0x4f);
                        Register.setRegAH(VESA.getSVGAModeInformation(Register.getRegCX(),
                                Register.segValue(Register.SEG_NAME_ES), Register.getRegDI()));
                        break;
                    case 0x02: /* Set videomode */
                        Register.setRegAL(0x4f);
                        Register.setRegAH(VESA.setSVGAMode(Register.getRegBX()));
                        break;
                    case 0x03: /* Get videomode */
                    {
                        Register.setRegAL(0x4f);
                        Register.setRegAH(0x00);
                        Register.setRegBX(VESA.getSVGAMode());
                        break;
                    }
                    case 0x04: /* Save/restore state */
                        Register.setRegAL(0x4f);
                        switch (Register.getRegDL()) {
                            case 0: {
                                int ret = VideoState.getSize(Register.getRegCX());
                                if (ret != 0) {
                                    Register.setRegAH(0);
                                    Register.setRegBX(ret);
                                } else
                                    Register.setRegAH(1);
                            }
                                break;
                            case 1:
                                if (VideoState.save(Register.getRegCX(),
                                        Memory.realMake(Register.segValue(Register.SEG_NAME_ES),
                                                Register.getRegBX())))
                                    Register.setRegAH(0);
                                else
                                    Register.setRegAH(1);
                                break;
                            case 2:
                                if (VideoState.restore(Register.getRegCX(),
                                        Memory.realMake(Register.segValue(Register.SEG_NAME_ES),
                                                Register.getRegBX())))
                                    Register.setRegAH(0);
                                else
                                    Register.setRegAH(1);
                                break;
                            default:
                                Register.setRegAH(1);
                                break;
                        }
                        break;
                    case 0x05:
                        if (Register.getRegBH() == 0) { /* Set CPU Window */
                            Register.setRegAH(
                                    VESA.setCPUWindow(Register.getRegBL(), Register.getRegDL()));
                            Register.setRegAL(0x4f);
                        } else if (Register.getRegBH() == 1) { /* Get CPU Window */
                            int regAH = VESA.tryCPUWindow(Register.getRegBL());
                            Register.setRegAH(regAH);
                            if (regAH == 0) {
                                Register.setRegDX(VESA.getCPUWindow());
                            }
                            Register.setRegAL(0x4f);
                        } else {
                            Log.logging(Log.LogTypes.INT10, Log.LogServerities.Error,
                                    "Unhandled VESA Function %X Subfunction %X",
                                    Register.getRegAL(), Register.getRegBH());
                            Register.setRegAH(0x01);
                        }
                        break;
                    case 0x06: {
                        Register.setRegAL(0x4f);
                        int result = VESA.scanLineLength(Register.getRegBL(), Register.getRegCX());
                        Register.setRegAH(result);
                        if (result == 0x00) {
                            Register.setRegBX(VESA.LineLengthInfoBytes);
                            Register.setRegCX(VESA.LineLengthInfoPixels);
                            Register.setRegDX(VESA.LineLengthInfoLines);
                        }
                        break;
                    }
                    case 0x07:
                        switch (Register.getRegBL()) {
                            case 0x80: /* Set Display Start during retrace ?? */
                            case 0x00: /* Set display Start */
                                Register.setRegAL(0x4f);
                                Register.setRegAH(VESA.setDisplayStart(Register.getRegCX(),
                                        Register.getRegDX()));
                                break;
                            case 0x01: {
                                Register.setRegAL(0x4f);
                                Register.setRegBH(0x00); // reserved
                                byte result = VESA.getDisplayStart();
                                Register.setRegAH(result);
                                if (result == 0x00) {
                                    Register.setRegCX(VESA.ResponseDisplayStartX);
                                    Register.setRegDX(VESA.ResponseDisplayStartY);
                                }
                                break;
                            }
                            default:
                                Log.logging(Log.LogTypes.INT10, Log.LogServerities.Error,
                                        "Unhandled VESA Function %X Subfunction %X",
                                        Register.getRegAL(), Register.getRegBL());
                                Register.setRegAH(0x1);
                                break;
                        }
                        break;
                    case 0x09:
                        switch (Register.getRegBL()) {
                            case 0x80: /* Set Palette during retrace */
                                // TODO
                            case 0x00: /* Set Palette */
                                Register.setRegAH(VESA.setPalette(
                                        Register.segPhys(Register.SEG_NAME_ES)
                                                + Register.getRegDI(),
                                        Register.getRegDX(), Register.getRegCX()));
                                Register.setRegAL(0x4f);
                                break;
                            case 0x01: /* Get Palette */
                                Register.setRegAH(VESA.getPalette(
                                        Register.segPhys(Register.SEG_NAME_ES)
                                                + Register.getRegDI(),
                                        Register.getRegDX(), Register.getRegCX()));
                                Register.setRegAL(0x4f);
                                break;
                            default:
                                Log.logging(Log.LogTypes.INT10, Log.LogServerities.Error,
                                        "Unhandled VESA Function %X Subfunction %X",
                                        Register.getRegAL(), Register.getRegBL());
                                Register.setRegAH(0x01);
                                break;
                        }
                        break;
                    case 0x0a: /* Get Pmode Interface */
                        if (int10.VesaOldVbe) {
                            Register.setRegAX(0x014f);
                            break;
                        }
                        switch (Register.getRegBL()) {
                            case 0x00:
                                Register.setRegEDI(Memory.realOff(int10.RomPModeInterface));
                                Register.segSet16(Register.SEG_NAME_ES,
                                        Memory.realSeg(int10.RomPModeInterface));
                                Register.setRegCX(int10.RomPModeInterfaceSize);
                                Register.setRegAX(0x004f);
                                break;
                            case 0x01: /* Get code for "set window" */
                                Register.setRegEDI(Memory.realOff(int10.RomPModeInterface)
                                        + int10.RomPModeInterfaceWindow);
                                Register.segSet16(Register.SEG_NAME_ES,
                                        Memory.realSeg(int10.RomPModeInterface));
                                Register.setRegCX(0x10); // 0x10 should be enough for the callbacks
                                Register.setRegAX(0x004f);
                                break;
                            case 0x02: /* Get code for "set display start" */
                                Register.setRegEDI(Memory.realOff(int10.RomPModeInterface)
                                        + int10.RomPModeInterfaceStart);
                                Register.segSet16(Register.SEG_NAME_ES,
                                        Memory.realSeg(int10.RomPModeInterface));
                                Register.setRegCX(0x10); // 0x10 should be enough for the callbacks
                                Register.setRegAX(0x004f);
                                break;
                            case 0x03: /* Get code for "set palette" */
                                Register.setRegEDI(Memory.realOff(int10.RomPModeInterface)
                                        + int10.RomPModeInterfacePalette);
                                Register.segSet16(Register.SEG_NAME_ES,
                                        Memory.realSeg(int10.RomPModeInterface));
                                Register.setRegCX(0x10); // 0x10 should be enough for the callbacks
                                Register.setRegAX(0x004f);
                                break;
                            default:
                                Register.setRegAX(0x014f);
                                break;
                        }
                        break;

                    default:
                        Log.logging(Log.LogTypes.INT10, Log.LogServerities.Error,
                                "Unhandled VESA Function %X", Register.getRegAL());
                        Register.setRegAL(0x0);
                        break;
                }
                break;
            case 0xf0: {
                int bl = 0;
                bl = EGARIL.readRegister(bl, Register.getRegDX());
                Register.setRegBL(bl);
                break;
            }
            case 0xf1: {
                int bl = 0;
                bl = EGARIL.writeRegister(bl, Register.getRegBH(), Register.getRegDX());
                Register.setRegBL(bl);
                break;
            }
            case 0xf2:
                EGARIL.readRegisterRange(Register.getRegCH(), Register.getRegCL(),
                        Register.getRegDX(),
                        Register.segPhys(Register.SEG_NAME_ES) + Register.getRegBX());
                break;
            case 0xf3:
                EGARIL.writeRegisterRange(Register.getRegCH(), Register.getRegCL(),
                        Register.getRegDX(),
                        Register.segPhys(Register.SEG_NAME_ES) + Register.getRegBX());
                break;
            case 0xf4:
                EGARIL.readRegisterSet(Register.getRegCX(),
                        Register.segPhys(Register.SEG_NAME_ES) + Register.getRegBX());
                break;
            case 0xf5:
                EGARIL.writeRegisterSet(Register.getRegCX(),
                        Register.segPhys(Register.SEG_NAME_ES) + Register.getRegBX());
                break;
            case 0xfa: {
                int pt = EGARIL.getVersionPt();
                Register.segSet16(Register.SEG_NAME_ES, Memory.realSeg(pt));
                Register.setRegBX(Memory.realOff(pt));
            }
                break;
            case 0xff:
                if (!warnedFF)
                    Log.logging(Log.LogTypes.INT10, Log.LogServerities.Normal,
                            "INT10:FF:Weird NC call");
                warnedFF = true;
                break;
            default:
                Log.logging(Log.LogTypes.INT10, Log.LogServerities.Error,
                        "Function %4X not supported", Register.getRegAX());
                // regsModule.reg_al=0x00; //Successfull, breaks marriage
                break;
        }
        return Callback.ReturnTypeNone;
    }

    // graphics_chars
    private static void graphicsChars() {
        switch (Register.getRegBL()) {
            case 0x00:
                Memory.realWriteB(BIOSMEM_SEG, BIOSMEM_NB_ROWS, Register.getRegDL() - 1);
                break;
            case 0x01:
                Memory.realWriteB(BIOSMEM_SEG, BIOSMEM_NB_ROWS, 13);
                break;
            case 0x03:
                Memory.realWriteB(BIOSMEM_SEG, BIOSMEM_NB_ROWS, 42);
                break;
            case 0x02:
            default:
                Memory.realWriteB(BIOSMEM_SEG, BIOSMEM_NB_ROWS, 24);
                break;
        }
    }

    private static void initSeg40() {
        // the default char height
        Memory.realWriteB(BIOSMEM_SEG, BIOSMEM_CHAR_HEIGHT, 16);
        // Clear the screen
        Memory.realWriteB(BIOSMEM_SEG, BIOSMEM_VIDEO_CTL, 0x60);
        // Set the basic screen we have
        Memory.realWriteB(BIOSMEM_SEG, BIOSMEM_SWITCHES, 0xF9);
        // Set the basic modeset options
        Memory.realWriteB(BIOSMEM_SEG, BIOSMEM_MODESET_CTL, 0x51);
        // Set the default MSR
        Memory.realWriteB(BIOSMEM_SEG, BIOSMEM_CURRENT_MSR, 0x09);
    }


    private static void initVGA() {
        /* switch to color mode and enable CPU access 480 lines */
        IO.write(0x3c2, 0xc3);
        /* More than 64k */
        IO.write(0x3c4, 0x04);
        IO.write(0x3c5, 0x02);
    }

    private static byte[] TandyConfig = {0x21, 0x42, 0x49, 0x4f, 0x53, 0x20, 0x52, 0x4f, 0x4d, 0x20,
            0x76, 0x65, 0x72, 0x73, 0x69, 0x6f, 0x6e, 0x20, 0x30, 0x32, 0x2e, 0x30, 0x30, 0x2e,
            0x30, 0x30, 0x0d, 0x0a, 0x43, 0x6f, 0x6d, 0x70, 0x61, 0x74, 0x69, 0x62, 0x69, 0x6c,
            0x69, 0x74, 0x79, 0x20, 0x53, 0x6f, 0x66, 0x74, 0x77, 0x61, 0x72, 0x65, 0x0d, 0x0a,
            0x43, 0x6f, 0x70, 0x79, 0x72, 0x69, 0x67, 0x68, 0x74, 0x20, 0x28, 0x43, 0x29, 0x20,
            0x31, 0x39, 0x38, 0x34, 0x2c, 0x31, 0x39, 0x38, 0x35, 0x2c, 0x31, 0x39, 0x38, 0x36,
            0x2c, 0x31, 0x39, 0x38, 0x37, 0x0d, 0x0a, 0x50, 0x68, 0x6f, 0x65, 0x6e, 0x69, 0x78,
            0x20, 0x53, 0x6f, 0x66, 0x74, 0x77, 0x61, 0x72, 0x65, 0x20, 0x41, 0x73, 0x73, 0x6f,
            0x63, 0x69, 0x61, 0x74, 0x65, 0x73, 0x20, 0x4c, 0x74, 0x64, 0x2e, 0x0d, 0x0a, 0x61,
            0x6e, 0x64, 0x20, 0x54, 0x61, 0x6e, 0x64, 0x79};

    private static void setupTandyBios() {

        if (DOSBox.Machine == DOSBox.MachineType.TANDY) {
            int i;
            for (i = 0; i < 130; i++) {
                Memory.physWriteB(0xf0000 + i + 0xc000, TandyConfig[i]);
            }
        }
    }

    public static void init(Section sec) {
        initVGA();
        if (DOSBox.isTANDYArch())
            setupTandyBios();
        /* Setup the INT 10 vector */
        call10 = Callback.allocate();
        Callback.setup(call10, INT10::INT10Handler, Callback.Symbol.IRET, "Int 10 video");
        Memory.realSetVec(0x10, Callback.realPointer(call10));
        // Init the 0x40 segment and init the datastructures in the the video rom area
        setupRomMemory();
        initSeg40();
        VESA.setupVESA();
        setupRomMemoryChecksum();// SetupVesa modifies the rom as well.
        INT10Mode.setVideoMode(0x3);

    }

    /*--------------------------- begin INT10Memory -----------------------------*/
    static byte[] staticFunctionality = {

            /* 0 */ (byte) 0xff, // All modes supported #1
            /* 1 */ (byte) 0xff, // All modes supported #2
            /* 2 */ 0x0f, // All modes supported #3
            /* 3 */ 0x00, 0x00, 0x00, 0x00, // reserved
            /* 7 */ 0x07, // 200, 350, 400 scan lines
            /* 8 */ 0x04, // total number of character blocks available in text modes
            /* 9 */ 0x02, // maximum number of active character blocks in text modes
            /* a */ (byte) 0xff, // Misc Flags Everthing supported
            /* b */ 0x0e, // Support for Display combination, intensity/blinking and video state
                          // saving/restoring
            /* c */ 0x00, // reserved
            /* d */ 0x00, // reserved
            /* e */ 0x00, // Change to add new functions
            /* f */ 0x00 // reserved

    };

    static short[] mapOffset = {0x0000, 0x4000, (short) 0x8000, (short) 0xc000, 0x2000, 0x6000,
            (short) 0xa000, (short) 0xe000};

    private static void loadFont(int font, boolean reload, int count, int offset, int map,
            int height) {
        int ftwhere = Memory.physMake(0xa000, mapOffset[map & 0x7] + (offset * 32));
        IO.write(0x3c4, 0x2);
        IO.write(0x3c5, 0x4); // Enable plane 2
        IO.write(0x3ce, 0x6);
        int old_6 = IO.read(0x3cf);
        IO.write(0x3cf, 0x0); // Disable odd/even and a0000 adressing
        for (int i = 0; i < count; i++) {
            Memory.blockCopy(ftwhere, font, height);
            ftwhere += 32;
            font += height;
        }
        IO.write(0x3c4, 0x2);
        IO.write(0x3c5, 0x3); // Enable textmode planes (0,1)
        IO.write(0x3ce, 0x6);
        if (DOSBox.isVGAArch())
            IO.write(0x3cf, old_6); // odd/even and b8000 adressing
        else
            IO.write(0x3cf, 0x0e);
        /* Reload tables and registers with new values based on this height */
        if (reload) {
            // Max scanline
            int _base = Memory.realReadW(BIOSMEM_SEG, BIOSMEM_CRTC_ADDRESS);
            IO.write(_base, 0x9);
            IO.write(_base + 1, 0xff & ((IO.read(_base + 1) & 0xe0) | (height - 1)));
            // Vertical display end bios says, but should stay the same?
            // Rows setting in bios segment
            Memory.realWriteB(BIOSMEM_SEG, BIOSMEM_NB_ROWS,
                    (INT10Mode.CurMode.SHeight / height) - 1);
            Memory.realWriteB(BIOSMEM_SEG, BIOSMEM_CHAR_HEIGHT, height);
            // TODO Reprogram cursor size?
        }
    }

    public static void reloadFont() {
        switch (INT10Mode.CurMode.CHeight) {
            case 8:
                loadFont(Memory.real2Phys(int10.RomFont8First), true, 256, 0, 0, 8);
                break;
            case 14:
                loadFont(Memory.real2Phys(int10.RomFont14), true, 256, 0, 0, 14);
                break;
            case 16:
            default:
                loadFont(Memory.real2Phys(int10.RomFont16), true, 256, 0, 0, 16);
                break;
        }
    }

    private static void setupRomMemory() {
        /* This should fill up certain structures inside the Video Bios Rom Area */
        int rom_base = Memory.physMake(0xc000, 0);
        int i;
        int10.RomUsed = 3;
        if (DOSBox.isEGAVGAArch()) {
            // set up the start of the ROM
            Memory.physWriteW(rom_base + 0, 0xaa55);
            Memory.physWriteB(rom_base + 2, 0x40); // Size of ROM: 64 512-blocks = 32KB
            if (DOSBox.isVGAArch()) {
                Memory.physWriteB(rom_base + 0x1e, 0x49); // IBM String
                Memory.physWriteB(rom_base + 0x1f, 0x42);
                Memory.physWriteB(rom_base + 0x20, 0x4d);
                Memory.physWriteB(rom_base + 0x21, 0x00);
            }
            int10.RomUsed = 0x100;
        }
        int10.RomFont8First = Memory.realMake(0xC000, int10.RomUsed);

        for (i = 0; i < 128 * 8; i++) {
            Memory.physWriteB(rom_base + int10.RomUsed++, int10Font08[i]);
        }
        int10.RomFont8Second = Memory.realMake(0xC000, int10.RomUsed);
        for (i = 0; i < 128 * 8; i++) {
            Memory.physWriteB(rom_base + int10.RomUsed++, int10Font08[i + 128 * 8]);
        }
        int10.RomFont14 = Memory.realMake(0xC000, int10.RomUsed);
        for (i = 0; i < 256 * 14; i++) {
            Memory.physWriteB(rom_base + int10.RomUsed++, int10Font14[i]);
        }
        int10.RomFont16 = Memory.realMake(0xC000, int10.RomUsed);
        for (i = 0; i < 256 * 16; i++) {
            Memory.physWriteB(rom_base + int10.RomUsed++, int10Font16[i]);
        }
        int10.RomStaticState = Memory.realMake(0xC000, int10.RomUsed);
        for (i = 0; i < 0x10; i++) {
            Memory.physWriteB(rom_base + int10.RomUsed++, staticFunctionality[i]);
        }
        for (i = 0; i < 128 * 8; i++) {
            Memory.physWriteB(Memory.physMake(0xf000, 0xfa6e) + i, int10Font08[i]);
        }
        Memory.realSetVec(0x1F, int10.RomFont8Second);
        int10.RomFont14Alternate = Memory.realMake(0xC000, int10.RomUsed);
        int10.RomFont16Alternate = Memory.realMake(0xC000, int10.RomUsed);
        Memory.physWriteB(rom_base + int10.RomUsed++, 0x00); // end of table (empty)

        if (DOSBox.isEGAVGAArch()) {
            int10.RomVideoParameterTable = Memory.realMake(0xC000, int10.RomUsed);
            int10.RomUsed += setupVideoParameterTable(rom_base + int10.RomUsed);

            if (DOSBox.isVGAArch()) {
                int10.RomVideoDccTable = Memory.realMake(0xC000, int10.RomUsed);
                Memory.physWriteB(rom_base + int10.RomUsed++, 0x10); // number of entries
                Memory.physWriteB(rom_base + int10.RomUsed++, 1); // version number
                Memory.physWriteB(rom_base + int10.RomUsed++, 8); // maximal display code
                Memory.physWriteB(rom_base + int10.RomUsed++, 0); // reserved
                // display combination codes
                Memory.physWriteW(rom_base + int10.RomUsed, 0x0000);
                int10.RomUsed += 2;
                Memory.physWriteW(rom_base + int10.RomUsed, 0x0100);
                int10.RomUsed += 2;
                Memory.physWriteW(rom_base + int10.RomUsed, 0x0200);
                int10.RomUsed += 2;
                Memory.physWriteW(rom_base + int10.RomUsed, 0x0102);
                int10.RomUsed += 2;
                Memory.physWriteW(rom_base + int10.RomUsed, 0x0400);
                int10.RomUsed += 2;
                Memory.physWriteW(rom_base + int10.RomUsed, 0x0104);
                int10.RomUsed += 2;
                Memory.physWriteW(rom_base + int10.RomUsed, 0x0500);
                int10.RomUsed += 2;
                Memory.physWriteW(rom_base + int10.RomUsed, 0x0502);
                int10.RomUsed += 2;
                Memory.physWriteW(rom_base + int10.RomUsed, 0x0600);
                int10.RomUsed += 2;
                Memory.physWriteW(rom_base + int10.RomUsed, 0x0601);
                int10.RomUsed += 2;
                Memory.physWriteW(rom_base + int10.RomUsed, 0x0605);
                int10.RomUsed += 2;
                Memory.physWriteW(rom_base + int10.RomUsed, 0x0800);
                int10.RomUsed += 2;
                Memory.physWriteW(rom_base + int10.RomUsed, 0x0801);
                int10.RomUsed += 2;
                Memory.physWriteW(rom_base + int10.RomUsed, 0x0700);
                int10.RomUsed += 2;
                Memory.physWriteW(rom_base + int10.RomUsed, 0x0702);
                int10.RomUsed += 2;
                Memory.physWriteW(rom_base + int10.RomUsed, 0x0706);
                int10.RomUsed += 2;

                int10.RomVideoSavePointerTable = Memory.realMake(0xC000, int10.RomUsed);
                Memory.physWriteW(rom_base + int10.RomUsed, 0x1a); // length of table
                int10.RomUsed += 2;
                Memory.physWriteD(rom_base + int10.RomUsed, int10.RomVideoDccTable);
                int10.RomUsed += 4;
                Memory.physWriteD(rom_base + int10.RomUsed, 0); // alphanumeric charset override
                int10.RomUsed += 4;
                Memory.physWriteD(rom_base + int10.RomUsed, 0); // user palette table
                int10.RomUsed += 4;
                Memory.physWriteD(rom_base + int10.RomUsed, 0);
                int10.RomUsed += 4;
                Memory.physWriteD(rom_base + int10.RomUsed, 0);
                int10.RomUsed += 4;
                Memory.physWriteD(rom_base + int10.RomUsed, 0);
                int10.RomUsed += 4;
            }

            int10.RomVideoSavePointers = Memory.realMake(0xC000, int10.RomUsed);
            Memory.physWriteD(rom_base + int10.RomUsed, int10.RomVideoParameterTable);
            int10.RomUsed += 4;
            Memory.physWriteD(rom_base + int10.RomUsed, 0); // dynamic save area pointer
            int10.RomUsed += 4;
            Memory.physWriteD(rom_base + int10.RomUsed, 0); // alphanumeric character set override
            int10.RomUsed += 4;
            Memory.physWriteD(rom_base + int10.RomUsed, 0); // graphics character set override
            int10.RomUsed += 4;
            if (DOSBox.isVGAArch()) {
                Memory.physWriteD(rom_base + int10.RomUsed, int10.RomVideoSavePointerTable);
            } else {
                Memory.physWriteD(rom_base + int10.RomUsed, 0); // secondary save pointer table
            }
            int10.RomUsed += 4;
            Memory.physWriteD(rom_base + int10.RomUsed, 0);
            int10.RomUsed += 4;
            Memory.physWriteD(rom_base + int10.RomUsed, 0);
            int10.RomUsed += 4;
        }

        setupBasicVideoParameterTable();

        if (DOSBox.isTANDYArch()) {
            Memory.realSetVec(0x44, int10.RomFont8First);
        }
    }

    public static void reloadRomFonts() {
        // 16x8 font
        int font16pt = Memory.real2Phys(int10.RomFont16);
        for (int i = 0; i < 256 * 16; i++) {
            Memory.physWriteB(font16pt + i, int10Font16[i]);
        }
        // 14x8 font
        int font14pt = Memory.real2Phys(int10.RomFont14);
        for (int i = 0; i < 256 * 14; i++) {
            Memory.physWriteB(font14pt + i, int10Font14[i]);
        }
        // 8x8 fonts
        int font8pt = Memory.real2Phys(int10.RomFont8First);
        for (int i = 0; i < 128 * 8; i++) {
            Memory.physWriteB(font8pt + i, int10Font08[i]);
        }
        font8pt = Memory.real2Phys(int10.RomFont8Second);
        for (int i = 0; i < 128 * 8; i++) {
            Memory.physWriteB(font8pt + i, int10Font08[i + 128 * 8]);
        }
    }

    public static void setupRomMemoryChecksum() {
        if (DOSBox.isEGAVGAArch()) { // EGA/VGA. Just to be safe
            /* Sum of all bytes in rom module 256 should be 0 */
            int sum = 0;// uint8
            int rom_base = Memory.physMake(0xc000, 0);
            int last_rombyte = 32 * 1024 - 1; // 32 KB romsize
            for (int i = 0; i < last_rombyte; i++)
                sum += Memory.physReadB(rom_base + i); // OVERFLOW IS OKAY
            sum = (256 - (0xff & sum)) & 0xff;
            Memory.physWriteB(rom_base + last_rombyte, sum);
        }
    }

    /*--------------------------- end INT10Memory -----------------------------*/

    /*--------------------------- begin INT10PutPixel -----------------------------*/
    private static byte[] cgaMasks = {0x3f, (byte) 0xcf, (byte) 0xf3, (byte) 0xfc};
    private static byte[] cgaMasks2 = {0x7f, (byte) 0xbf, (byte) 0xdf, (byte) 0xef, (byte) 0xf7,
            (byte) 0xfb, (byte) 0xfd, (byte) 0xfe};

    private static boolean putPixelWarned = false;

    // public static void PutPixel(short x, short y, byte page, byte color) {
    public static void putPixel(int x, int y, int page, byte color) {
        switch (INT10Mode.CurMode.Type) {
            case CGA4: {
                if (Memory.realReadB(BIOSMEM_SEG, BIOSMEM_CURRENT_MODE) <= 5) {
                    int off = 0xffff & ((y >>> 1) * 80 + (x >>> 2));
                    if ((y & 1) != 0)
                        off += 8 * 1024;

                    int old = Memory.realReadB(0xb800, off);
                    if ((color & 0x80) != 0) {
                        color &= 3;
                        old ^= 0xff & (color << (2 * (3 - (x & 3))));
                    } else {
                        old = 0xff & ((old & (0xff & cgaMasks[x & 3]))
                                | ((color & 3) << (2 * (3 - (x & 3)))));
                    }
                    Memory.realWriteB(0xb800, off, old);
                } else {
                    int off = 0xffff & ((y >>> 2) * 160 + ((x >>> 2) & (~1)));
                    off += 0xffff & ((8 * 1024) * (y & 3));

                    int old = Memory.realReadW(0xb800, off);
                    if ((color & 0x80) != 0) {
                        old ^= 0xffff & ((color & 1) << (7 - (x & 7)));
                        old ^= 0xffff & (((color & 2) >>> 1) << ((7 - (x & 7)) + 8));
                    } else {
                        old = 0xffff & ((old & (~(0x101 << (7 - (x & 7)))))
                                | ((color & 1) << (7 - (x & 7)))
                                | (((color & 2) >>> 1) << ((7 - (x & 7)) + 8)));
                    }
                    Memory.realWriteW(0xb800, off, old);
                }
            }
                break;
            case CGA2: {
                int off = 0xffff & ((y >>> 1) * 80 + (x >>> 3));
                if ((y & 1) != 0)
                    off += 8 * 1024;
                int old = Memory.realReadB(0xb800, off);
                if ((color & 0x80) != 0) {
                    color &= 1;
                    old ^= 0xff & (color << ((7 - (x & 7))));
                } else {
                    old = 0xff & ((old & (0xff & cgaMasks2[x & 7]))
                            | ((color & 1) << ((7 - (x & 7)))));
                }
                Memory.realWriteB(0xb800, off, old);
            }
                break;
            case TANDY16: {
                IO.write(0x3d4, 0x09);
                int scanlinesM1 = IO.read(0x3d5);
                int off = 0xffff
                        & ((y >>> ((scanlinesM1 == 1) ? 1 : 2)) * (INT10Mode.CurMode.SWidth >>> 1)
                                + (x >>> 1));
                off += 0xffff & ((8 * 1024) * (y & scanlinesM1));
                int old = Memory.realReadB(0xb800, off);
                byte[] p = new byte[2];
                p[1] = (byte) ((old >>> 4) & 0xf);
                p[0] = (byte) (old & 0xf);
                int ind = 1 - (x & 0x1);

                if ((color & 0x80) != 0) {
                    p[ind] ^= 0xff & (color & 0x7f);
                } else {
                    p[ind] = color;
                }
                old = 0xff & (((p[1] & 0xff) << 4) | (p[0] & 0xff));
                Memory.realWriteB(0xb800, off, old);
            }
                break;
            case LIN4:
                if ((DOSBox.Machine != DOSBox.MachineType.VGA)
                        || (DOSBox.SVGACard != DOSBox.SVGACards.TsengET4K)
                        || (INT10Mode.CurMode.SWidth > 800)) {
                    // the ET4000 BIOS supports text output in 800x600 SVGA (Gateway 2)
                    // putpixel warining?
                    break;
                }
                // goto GotoM_EGA;
            case EGA:
            // GotoM_EGA:
            {
                /* Set the correct bitmask for the pixel position */
                IO.write(0x3ce, 0x8);
                int mask = 128 >>> (x & 7);
                IO.write(0x3cf, mask);
                /* Set the color to set/reset register */
                IO.write(0x3ce, 0x0);
                IO.write(0x3cf, color);
                /* Enable all the set/resets */
                IO.write(0x3ce, 0x1);
                IO.write(0x3cf, 0xf);
                /* test for xorring */
                if ((color & 0x80) != 0) {
                    IO.write(0x3ce, 0x3);
                    IO.write(0x3cf, 0x18);
                }
                // Perhaps also set mode 1
                /* Calculate where the pixel is in video memory */
                if (INT10Mode.CurMode.Plength != Memory.realReadW(BIOSMEM_SEG, BIOSMEM_PAGE_SIZE))
                    Log.logging(Log.LogTypes.INT10, Log.LogServerities.Error,
                            "PutPixel_EGA_p: %x!=%x", INT10Mode.CurMode.Plength,
                            Memory.realReadW(BIOSMEM_SEG, BIOSMEM_PAGE_SIZE));
                if (INT10Mode.CurMode.SWidth != Memory.realReadW(BIOSMEM_SEG, BIOSMEM_NB_COLS) * 8)
                    Log.logging(Log.LogTypes.INT10, Log.LogServerities.Error,
                            "PutPixel_EGA_w: %x!=%x", INT10Mode.CurMode.SWidth,
                            Memory.realReadW(BIOSMEM_SEG, BIOSMEM_NB_COLS) * 8);
                int off = 0xa0000 + Memory.realReadW(BIOSMEM_SEG, BIOSMEM_PAGE_SIZE) * page
                        + ((y * Memory.realReadW(BIOSMEM_SEG, BIOSMEM_NB_COLS) * 8 + x) >>> 3);
                /* Bitmask and set/reset should do the rest */
                Memory.readB(off);
                Memory.writeB(off, 0xff);
                /* Restore bitmask */
                IO.write(0x3ce, 0x8);
                IO.write(0x3cf, 0xff);
                IO.write(0x3ce, 0x1);
                IO.write(0x3cf, 0);
                /* Restore write operating if changed */
                if ((color & 0x80) != 0) {
                    IO.write(0x3ce, 0x3);
                    IO.write(0x3cf, 0x0);
                }
                break;
            }

            case VGA:
                Memory.writeB(Memory.physMake(0xa000, y * 320 + x), color);
                break;
            case LIN8: {
                if (INT10Mode.CurMode.SWidth != Memory.realReadW(BIOSMEM_SEG, BIOSMEM_NB_COLS) * 8)
                    Log.logging(Log.LogTypes.INT10, Log.LogServerities.Error,
                            "PutPixel_VGA_w: %x!=%x", INT10Mode.CurMode.SWidth,
                            Memory.realReadW(BIOSMEM_SEG, BIOSMEM_NB_COLS) * 8);
                int off = S3_LFB_BASE + y * Memory.realReadW(BIOSMEM_SEG, BIOSMEM_NB_COLS) * 8 + x;
                Memory.writeB(off, color);
                break;
            }
            default:
                if (!putPixelWarned) {
                    putPixelWarned = true;
                    Log.logging(Log.LogTypes.INT10, Log.LogServerities.Error,
                            "PutPixel unhandled mode type %d", INT10Mode.CurMode.Type.toString());
                }
                break;
        }
    }

    public static boolean canGetPixel() {
        return INT10Mode.CurMode.Type == VGAModes.CGA4 || INT10Mode.CurMode.Type == VGAModes.CGA2
                || INT10Mode.CurMode.Type == VGAModes.EGA || INT10Mode.CurMode.Type == VGAModes.VGA
                || INT10Mode.CurMode.Type == VGAModes.LIN8;
    }

    // public static void GetPixel(uint16 x, uint16 y, uint8 page, uint8 color)
    public static byte getPixel(int x, int y, int page) {
        switch (INT10Mode.CurMode.Type) {
            case CGA4: {
                int off = 0xffff & ((y >>> 1) * 80 + (x >>> 2));
                if ((y & 1) != 0)
                    off += 8 * 1024;
                int val = Memory.realReadB(0xb800, off);
                return (byte) ((val >>> (((3 - (x & 3))) * 2)) & 3);
            }
            case CGA2: {
                int off = 0xffff & ((y >>> 1) * 80 + (x >>> 3));
                if ((y & 1) != 0)
                    off += 8 * 1024;
                int val = Memory.realReadB(0xb800, off);
                return (byte) ((val >>> (((7 - (x & 7))))) & 1);
            }
            case EGA: {
                /* Calculate where the pixel is in video memory */
                if (INT10Mode.CurMode.Plength != Memory.realReadW(BIOSMEM_SEG, BIOSMEM_PAGE_SIZE))
                    Log.logging(Log.LogTypes.INT10, Log.LogServerities.Error,
                            "GetPixel_EGA_p: %x!=%x", INT10Mode.CurMode.Plength,
                            Memory.realReadW(BIOSMEM_SEG, BIOSMEM_PAGE_SIZE));
                if (INT10Mode.CurMode.SWidth != Memory.realReadW(BIOSMEM_SEG, BIOSMEM_NB_COLS) * 8)
                    Log.logging(Log.LogTypes.INT10, Log.LogServerities.Error,
                            "GetPixel_EGA_w: %x!=%x", INT10Mode.CurMode.SWidth,
                            Memory.realReadW(BIOSMEM_SEG, BIOSMEM_NB_COLS) * 8);
                int off = 0xa0000 + Memory.realReadW(BIOSMEM_SEG, BIOSMEM_PAGE_SIZE) * page
                        + ((y * Memory.realReadW(BIOSMEM_SEG, BIOSMEM_NB_COLS) * 8 + x) >>> 3);
                int shift = 7 - (x & 7);
                /* Set the read map */
                byte color = 0;
                IO.write(0x3ce, 0x4);
                IO.write(0x3cf, 0);
                color |= (byte) (((Memory.readB(off) >>> shift) & 1) << 0);
                IO.write(0x3ce, 0x4);
                IO.write(0x3cf, 1);
                color |= (byte) (((Memory.readB(off) >>> shift) & 1) << 1);
                IO.write(0x3ce, 0x4);
                IO.write(0x3cf, 2);
                color |= (byte) (((Memory.readB(off) >>> shift) & 1) << 2);
                IO.write(0x3ce, 0x4);
                IO.write(0x3cf, 3);
                color |= (byte) (((Memory.readB(off) >>> shift) & 1) << 3);
                return color;
            }
            case VGA:
                return (byte) Memory.readB(Memory.physMake(0xa000, 320 * y + x));
            case LIN8: {
                if (INT10Mode.CurMode.SWidth != Memory.realReadW(BIOSMEM_SEG, BIOSMEM_NB_COLS) * 8)
                    Log.logging(Log.LogTypes.INT10, Log.LogServerities.Error,
                            "GetPixel_VGA_w: %x!=%x", INT10Mode.CurMode.SWidth,
                            Memory.realReadW(BIOSMEM_SEG, BIOSMEM_NB_COLS) * 8);
                int off = S3_LFB_BASE + y * Memory.realReadW(BIOSMEM_SEG, BIOSMEM_NB_COLS) * 8 + x;
                return (byte) Memory.readB(off);
            }
            default:
                Log.logging(Log.LogTypes.INT10, Log.LogServerities.Error,
                        "GetPixel unhandled mode type %d", INT10Mode.CurMode.Type.toString());
                return 0;
        }
    }
    /*--------------------------- end INT10PutPixel -----------------------------*/


    /*--------------------------- begin INT10VPTable -----------------------------*/
    private static final byte[] vparams = {
            // 40x25 mode 0 and 1 crtc registers
            0x38, 0x28, 0x2d, 0x0a, 0x1f, 0x06, 0x19, 0x1c, 0x02, 0x07, 0x06, 0x07, 0, 0, 0, 0,
            // 80x25 mode 2 and 3 crtc registers
            0x71, 0x50, 0x5a, 0x0a, 0x1f, 0x06, 0x19, 0x1c, 0x02, 0x07, 0x06, 0x07, 0, 0, 0, 0,
            // graphics modes 4, 5 and 6
            0x38, 0x28, 0x2d, 0x0a, 0x7f, 0x06, 0x64, 0x70, 0x02, 0x01, 0x06, 0x07, 0, 0, 0, 0,
            // mode 7 MDA text
            0x61, 0x50, 0x52, 0x0f, 0x19, 0x06, 0x19, 0x19, 0x02, 0x0d, 0x0b, 0x0c, 0, 0, 0, 0,
            // buffer length words 2048, 4096, 16384, 16384
            0x00, 0x08, 0x00, 0x10, 0x00, 0x40, 0x00, 0x40,
            // columns
            40, 40, 80, 80, 40, 40, 80, 80,
            // CGA mode register
            0x2c, 0x28, 0x2d, 0x29, 0x2a, 0x2e, 0x1e, 0x29};

    private static final byte[] vparamsPCjr = {
            // 40x25 mode 0 and 1 crtc registers
            0x38, 0x28, 0x2c, 0x06, 0x1f, 0x06, 0x19, 0x1c, 0x02, 0x07, 0x06, 0x07, 0, 0, 0, 0,
            // 80x25 mode 2 and 3 crtc registers
            0x71, 0x50, 0x5a, 0x0c, 0x1f, 0x06, 0x19, 0x1c, 0x02, 0x07, 0x06, 0x07, 0, 0, 0, 0,
            // graphics modes 4, 5, 6, 8
            0x38, 0x28, 0x2b, 0x06, 0x7f, 0x06, 0x64, 0x70, 0x02, 0x01, 0x26, 0x07, 0, 0, 0, 0,
            // other graphics modes
            0x71, 0x50, 0x56, 0x0c, 0x3f, 0x06, 0x32, 0x38, 0x02, 0x03, 0x26, 0x07, 0, 0, 0, 0,
            // buffer length words 2048, 4096, 16384, 16384
            0x00, 0x08, 0x00, 0x10, 0x00, 0x40, 0x00, 0x40,
            // columns
            40, 40, 80, 80, 40, 40, 80, 80,
            // CGA mode register
            0x2c, 0x28, 0x2d, 0x29, 0x2a, 0x2e, 0x1e, 0x29};

    private static final byte[] vparamsTandy = {
            // 40x25 mode 0 and 1 crtc registers
            0x38, 0x28, 0x2c, 0x08, 0x1f, 0x06, 0x19, 0x1c, 0x02, 0x07, 0x06, 0x07, 0, 0, 0, 0,
            // 80x25 mode 2 and 3 crtc registers
            0x71, 0x50, 0x58, 0x10, 0x1f, 0x06, 0x19, 0x1c, 0x02, 0x07, 0x06, 0x07, 0, 0, 0, 0,
            // graphics modes 4, 5 and 6
            0x38, 0x28, 0x2c, 0x08, 0x7f, 0x06, 0x64, 0x70, 0x02, 0x01, 0x06, 0x07, 0, 0, 0, 0,
            // graphics mode 7
            0x71, 0x50, 0x58, 0x10, 0x3f, 0x06, 0x32, 0x38, 0x02, 0x03, 0x06, 0x07, 0, 0, 0, 0,
            // buffer length words 2048, 4096, 16384, 16384
            0x00, 0x08, 0x00, 0x10, 0x00, 0x40, 0x00, 0x40,
            // columns
            40, 40, 80, 80, 40, 40, 80, 80,
            // CGA mode register
            0x2c, 0x28, 0x2d, 0x29, 0x2a, 0x2e, 0x1e, 0x29};

    private static final byte[] vparamsTandyTD = {

            // 40x25 mode 0 and 1 crtc registers
            0x38, 0x28, 0x2d, 0x0a, 0x1f, 0x06, 0x19, 0x1c, 0x02, 0x07, 0x06, 0x07, 0, 0, 0, 0,
            // 80x25 mode 2 and 3 crtc registers
            0x71, 0x50, 0x5a, 0x0a, 0x1f, 0x06, 0x19, 0x1c, 0x02, 0x07, 0x06, 0x07, 0, 0, 0, 0,
            // graphics modes 4, 5 and 6
            0x38, 0x28, 0x2d, 0x0a, 0x7f, 0x06, 0x64, 0x70, 0x02, 0x01, 0x06, 0x07, 0, 0, 0, 0,
            // mode 7 MDA text
            0x61, 0x50, 0x52, 0x0f, 0x19, 0x06, 0x19, 0x19, 0x02, 0x0d, 0x0b, 0x0c, 0, 0, 0, 0,
            // ?? mode 2 and 3 crtc registers
            0x71, 0x50, 0x5a, 0x0a, 0x3f, 0x06, 0x32, 0x38, 0x02, 0x03, 0x06, 0x07, 0, 0, 0, 0,
            // buffer length words 2048, 4096, 16384, 16384
            0x00, 0x08, 0x00, 0x10, 0x00, 0x40, 0x00, 0x40,
            // columns
            40, 40, 80, 80, 40, 40, 80, 80,
            // CGA mode register
            0x2c, 0x28, 0x2d, 0x29, 0x2a, 0x2e, 0x1e, 0x29};

    // uint16(uint32)
    private static int setupVideoParameterTable(int basePos) {
        if (DOSBox.isVGAArch()) {
            byte[] videoParameterTableVGA = Resources.get("VideoParameterTableVGA");
            for (int i = 0; i < 0x40 * 0x1d; i++) {
                Memory.physWriteB(basePos + i, videoParameterTableVGA[i]);
            }
            return 0x40 * 0x1d;
        } else {
            byte[] videoParameterTableEGA = Resources.get("VideoParameterTableEGA");
            for (int i = 0; i < 0x40 * 0x17; i++) {
                Memory.physWriteB(basePos + i, videoParameterTableEGA[i]);
            }
            return 0x40 * 0x17;
        }
    }

    private static void setupBasicVideoParameterTable() {
        /* video parameter table at F000:F0A4 */
        Memory.realSetVec(0x1d, Memory.realMake(0xF000, 0xF0A4));
        switch (DOSBox.Machine) {
            case TANDY:
                for (short i = 0; i < vparamsTandy.length; i++) {
                    Memory.physWriteB(0xFF0A4 + i, vparamsTandy[i]);
                }
                break;
            case PCJR:
                for (short i = 0; i < vparamsPCjr.length; i++) {
                    Memory.physWriteB(0xFF0A4 + i, vparamsPCjr[i]);
                }
                break;
            default:
                for (short i = 0; i < vparams.length; i++) {
                    Memory.physWriteB(0xFF0A4 + i, vparams[i]);
                }
                break;
        }
    }

    /*--------------------------- end INT10VPTable -----------------------------*/

}
