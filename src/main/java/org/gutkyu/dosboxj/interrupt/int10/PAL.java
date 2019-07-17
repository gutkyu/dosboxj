package org.gutkyu.dosboxj.interrupt.int10;

import org.gutkyu.dosboxj.hardware.io.*;
import org.gutkyu.dosboxj.hardware.memory.*;
import org.gutkyu.dosboxj.*;

/*--------------------------- begin INT10Pal -----------------------------*/
final class PAL {

    /*
     *
     * VGA registers
     *
     */
    private static final int VGAREG_ACTL_ADDRESS = 0x3c0;
    private static final int VGAREG_ACTL_WRITE_DATA = 0x3c0;
    private static final int VGAREG_ACTL_READ_DATA = 0x3c1;

    private static final int VGAREG_INPUT_STATUS = 0x3c2;
    private static final int VGAREG_WRITE_MISC_OUTPUT = 0x3c2;
    private static final int VGAREG_VIDEO_ENABLE = 0x3c3;
    private static final int VGAREG_SEQU_ADDRESS = 0x3c4;
    private static final int VGAREG_SEQU_DATA = 0x3c5;

    private static final int VGAREG_PEL_MASK = 0x3c6;
    private static final int VGAREG_DAC_STATE = 0x3c7;
    private static final int VGAREG_DAC_READ_ADDRESS = 0x3c7;
    private static final int VGAREG_DAC_WRITE_ADDRESS = 0x3c8;
    private static final int VGAREG_DAC_DATA = 0x3c9;

    private static final int VGAREG_READ_FEATURE_CTL = 0x3ca;
    private static final int VGAREG_READ_MISC_OUTPUT = 0x3cc;

    private static final int VGAREG_GRDC_ADDRESS = 0x3ce;
    private static final int VGAREG_GRDC_DATA = 0x3cf;

    private static final int VGAREG_MDA_CRTC_ADDRESS = 0x3b4;
    private static final int VGAREG_MDA_CRTC_DATA = 0x3b5;
    private static final int VGAREG_VGA_CRTC_ADDRESS = 0x3d4;
    private static final int VGAREG_VGA_CRTC_DATA = 0x3d5;

    private static final int VGAREG_MDA_WRITE_FEATURE_CTL = 0x3ba;
    private static final int VGAREG_VGA_WRITE_FEATURE_CTL = 0x3da;
    private static final int VGAREG_ACTL_RESET = 0x3da;
    private static final int VGAREG_TDY_RESET = 0x3da;
    private static final int VGAREG_TDY_ADDRESS = 0x3da;
    private static final int VGAREG_TDY_DATA = 0x3de;
    private static final int VGAREG_PCJR_DATA = 0x3da;

    private static final int VGAREG_MDA_MODECTL = 0x3b8;
    private static final int VGAREG_CGA_MODECTL = 0x3d8;
    private static final int VGAREG_CGA_PALETTE = 0x3d9;

    /* Video memory */
    private static final int VGAMEM_GRAPH = 0xA000;
    private static final int VGAMEM_CTEXT = 0xB800;
    private static final int VGAMEM_MTEXT = 0xB000;


    private static final int ACTL_MAX_REG = 0x14;

    private static void resetACTL() {
        IO.read(Memory.realReadW(INT10.BIOSMEM_SEG, INT10.BIOSMEM_CRTC_ADDRESS) + 6);
    }

    private static void writeTandyACTL(byte creg, byte val) {
        IO.write(VGAREG_TDY_ADDRESS, creg);
        if (DOSBox.Machine == DOSBox.MachineType.TANDY)
            IO.write(VGAREG_TDY_DATA, val);
        else
            IO.write(VGAREG_PCJR_DATA, val);
    }

    private static void writeTandyACTL(int creg, int val) {
        writeTandyACTL((byte) creg, (byte) val);
    }

    public static void setSinglePaletteRegister(byte reg, byte val) {
        switch (DOSBox.Machine) {
            case TANDY:
            case PCJR:
                IO.read(VGAREG_TDY_RESET);
                writeTandyACTL((byte) (reg + 0x10), val);
                break;
            case EGA:
            case VGA:
                if (!DOSBox.isVGAArch())
                    reg &= 0x1f;
                if (reg <= ACTL_MAX_REG) {
                    resetACTL();
                    IO.write(VGAREG_ACTL_ADDRESS, reg);
                    IO.write(VGAREG_ACTL_WRITE_DATA, val);
                }
                IO.write(VGAREG_ACTL_ADDRESS, 32); // Enable output and protect palette
                break;
        }
    }

    public static void setSinglePaletteRegister(int reg, int val) {
        setSinglePaletteRegister((byte) reg, (byte) val);
    }

    public static void setOverscanBorderColor(int val) {
        switch (DOSBox.Machine) {
            case TANDY:
            case PCJR:
                IO.read(VGAREG_TDY_RESET);
                writeTandyACTL(0x02, val);
                break;
            case EGA:
            case VGA:
                resetACTL();
                IO.write(VGAREG_ACTL_ADDRESS, 0x11);
                IO.write(VGAREG_ACTL_WRITE_DATA, val);
                IO.write(VGAREG_ACTL_ADDRESS, 32); // Enable output and protect palette
                break;
        }
    }

    public static void setAllPaletteRegisters(int data) {
        switch (DOSBox.Machine) {
            case TANDY:
            case PCJR:
                IO.read(VGAREG_TDY_RESET);
                // First the colors
                for (byte i = 0; i < 0x10; i++) {
                    writeTandyACTL((byte) (i + 0x10), Memory.readB(data));
                    data++;
                }
                // Then the border
                writeTandyACTL(0x02, Memory.readB(data));
                break;
            case EGA:
            case VGA:
                resetACTL();
                // First the colors
                for (byte i = 0; i < 0x10; i++) {
                    IO.write(VGAREG_ACTL_ADDRESS, i);
                    IO.write(VGAREG_ACTL_WRITE_DATA, Memory.readB(data));
                    data++;
                }
                // Then the border
                IO.write(VGAREG_ACTL_ADDRESS, 0x11);
                IO.write(VGAREG_ACTL_WRITE_DATA, Memory.readB(data));
                IO.write(VGAREG_ACTL_ADDRESS, 32); // Enable output and protect palette
                break;
        }
    }

    // public static void ToggleBlinkingBit(byte state) {
    public static void toggleBlinkingBit(int state) {
        byte value;
        // state&=0x01;
        if ((state > 1) && (DOSBox.SVGACard == DOSBox.SVGACards.S3Trio))
            return;
        resetACTL();

        IO.write(VGAREG_ACTL_ADDRESS, 0x10);
        value = IO.read(VGAREG_ACTL_READ_DATA);
        if (state <= 1) {
            value &= 0xf7;
            value |= (byte) (state << 3);
        }

        resetACTL();
        IO.write(VGAREG_ACTL_ADDRESS, 0x10);
        IO.write(VGAREG_ACTL_WRITE_DATA, value);
        IO.write(VGAREG_ACTL_ADDRESS, 32); // Enable output and protect palette

        if (state <= 1) {
            byte msrval =
                    (byte) (Memory.realReadB(INT10.BIOSMEM_SEG, INT10.BIOSMEM_CURRENT_MSR) & 0xdf);
            if (state != 0)
                msrval |= 0x20;
            Memory.realWriteB(INT10.BIOSMEM_SEG, INT10.BIOSMEM_CURRENT_MSR, msrval);
        }
    }

    // public static byte GetSinglePaletteRegister(byte reg) {
    public static int getSinglePaletteRegister(int reg) {
        int ret = 0;
        if (reg <= ACTL_MAX_REG) {
            resetACTL();
            IO.write(VGAREG_ACTL_ADDRESS, reg + 32);
            ret = IO.read(VGAREG_ACTL_READ_DATA);
            IO.write(VGAREG_ACTL_WRITE_DATA, ret);
        }
        return ret;
    }

    public static byte getOverscanBorderColor() {
        byte val = 0;
        resetACTL();
        IO.write(VGAREG_ACTL_ADDRESS, 0x11 + 32);
        val = IO.read(VGAREG_ACTL_READ_DATA);
        IO.write(VGAREG_ACTL_WRITE_DATA, val);
        return val;
    }

    public static void getAllPaletteRegisters(int data) {
        resetACTL();
        // First the colors
        for (byte i = 0; i < 0x10; i++) {
            IO.write(VGAREG_ACTL_ADDRESS, i);
            Memory.writeB(data, IO.read(VGAREG_ACTL_READ_DATA));
            resetACTL();
            data++;
        }
        // Then the border
        IO.write(VGAREG_ACTL_ADDRESS, 0x11 + 32);
        Memory.writeB(data, IO.read(VGAREG_ACTL_READ_DATA));
        resetACTL();
    }

    public static void setSingleDacRegister(int index, byte red, byte green, byte blue) {
        IO.write(VGAREG_DAC_WRITE_ADDRESS, index);
        IO.write(VGAREG_DAC_DATA, red);
        IO.write(VGAREG_DAC_DATA, green);
        IO.write(VGAREG_DAC_DATA, blue);
    }

    // public static void GetSingleDacRegister(byte index, ref byte red, ref byte green, ref byte
    // blue)
    // 4개의 함수로 분할
    // Dac 읽기 준비
    // public static void getSingleDacRegisterReady(byte index) {
    public static void getSingleDacRegisterReady(int index) {
        IO.write(VGAREG_DAC_READ_ADDRESS, index);
    }

    // red 반환
    public static byte getSingleDacRegisterRed() {
        return IO.read(VGAREG_DAC_DATA);
    }

    // green 반환
    public static byte getSingleDacRegisterGreen() {
        return IO.read(VGAREG_DAC_DATA);
    }

    // blue 반환
    public static byte getSingleDacRegisterBlue() {
        return IO.read(VGAREG_DAC_DATA);
    }

    public static void setDACBlock(int index, int count, int data) {
        IO.write(VGAREG_DAC_WRITE_ADDRESS, index);
        for (; count > 0; count--) {
            IO.write(VGAREG_DAC_DATA, Memory.readB(data++));
            IO.write(VGAREG_DAC_DATA, Memory.readB(data++));
            IO.write(VGAREG_DAC_DATA, Memory.readB(data++));
        }
    }

    public static void getDACBlock(int index, int count, int data) {
        IO.write(VGAREG_DAC_READ_ADDRESS, index);
        for (; count > 0; count--) {
            Memory.writeB(data++, IO.read(VGAREG_DAC_DATA));
            Memory.writeB(data++, IO.read(VGAREG_DAC_DATA));
            Memory.writeB(data++, IO.read(VGAREG_DAC_DATA));
        }
    }

    // public static void SelectDACPage(byte function, byte mode) {
    public static void selectDACPage(int function, int mode) {
        resetACTL();
        IO.write(VGAREG_ACTL_ADDRESS, 0x10);
        byte old10 = IO.read(VGAREG_ACTL_READ_DATA);
        if (function == 0) { // Select paging mode
            if (mode != 0)
                old10 |= 0x80;
            else
                old10 &= 0x7f;
            // inoutModule.IO_Write(VGAREG_ACTL_ADDRESS,0x10);
            IO.write(VGAREG_ACTL_WRITE_DATA, old10);
        } else { // Select page
            IO.write(VGAREG_ACTL_WRITE_DATA, old10);
            if ((old10 & 0x80) == 0)
                mode <<= 2;
            mode &= 0xf;
            IO.write(VGAREG_ACTL_ADDRESS, 0x14);
            IO.write(VGAREG_ACTL_WRITE_DATA, mode);
        }
        IO.write(VGAREG_ACTL_ADDRESS, 32); // Enable output and protect palette
    }

    // public static void GetDACPage(ref byte mode, ref byte page)
    public static byte getDACPageMode() {
        resetACTL();
        IO.write(VGAREG_ACTL_ADDRESS, 0x10);
        byte reg10 = IO.read(VGAREG_ACTL_READ_DATA);
        IO.write(VGAREG_ACTL_WRITE_DATA, reg10);
        return (reg10 & 0x80) != 0 ? (byte) 0x01 : (byte) 0x00;
    }

    // GetDACPageMode와 같이 사용
    public static byte getDACPage(byte mode) {

        IO.write(VGAREG_ACTL_ADDRESS, 0x14);
        int page = 0xff & IO.read(VGAREG_ACTL_READ_DATA);
        IO.write(VGAREG_ACTL_WRITE_DATA, page);
        if (mode != 0) {
            page &= 0xf;
        } else {
            page &= 0xc;
            page >>>= 2;
        }
        return (byte) page;
    }

    public static void setPelMask(int mask) {
        IO.write(VGAREG_PEL_MASK, mask);
    }

    public static int getPelMask() {
        return IO.read(VGAREG_PEL_MASK);
    }

    public static void setBackgroundBorder(int val) {
        int temp = Memory.realReadB(INT10.BIOSMEM_SEG, INT10.BIOSMEM_CURRENT_PAL);
        temp = (temp & 0xe0) | (val & 0x1f);
        Memory.realWriteB(INT10.BIOSMEM_SEG, INT10.BIOSMEM_CURRENT_PAL, temp);
        if (DOSBox.Machine == DOSBox.MachineType.CGA || DOSBox.isTANDYArch())
            IO.write(0x3d9, temp);
        else if (DOSBox.isEGAVGAArch()) {
            val = ((val << 1) & 0x10) | (val & 0x7);
            /* Aways set the overscan color */
            setSinglePaletteRegister(0x11, val);
            /* Don't set any extra colors when in text mode */
            if (INT10Mode.CurMode.Mode <= 3)
                return;
            setSinglePaletteRegister(0, val);
            val = (temp & 0x10) | 2 | ((temp & 0x20) >>> 5);
            setSinglePaletteRegister(1, val);
            val += 2;
            setSinglePaletteRegister(2, val);
            val += 2;
            setSinglePaletteRegister(3, val);
        }
    }

    public static void setColorSelect(int val) {
        int temp = Memory.realReadB(INT10.BIOSMEM_SEG, INT10.BIOSMEM_CURRENT_PAL);
        temp = (temp & 0xdf) | ((val & 1) != 0 ? 0x20 : 0x0);
        Memory.realWriteB(INT10.BIOSMEM_SEG, INT10.BIOSMEM_CURRENT_PAL, temp);
        if (DOSBox.Machine == DOSBox.MachineType.CGA || DOSBox.isTANDYArch())
            IO.write(0x3d9, temp);
        else if (DOSBox.isEGAVGAArch()) {
            if (INT10Mode.CurMode.Mode <= 3) // Maybe even skip the total function!
                return;
            val = (byte) ((temp & 0x10) | 2 | val);
            setSinglePaletteRegister(1, val);
            val += 2;
            setSinglePaletteRegister(2, val);
            val += 2;
            setSinglePaletteRegister(3, val);
        }
    }

    public static void performGrayScaleSumming(int start_reg, int count) {
        if (count > 0x100)
            count = 0x100;
        for (int ct = 0; ct < count; ct++) {
            IO.write(VGAREG_DAC_READ_ADDRESS, (byte) (start_reg + ct));
            byte red = IO.read(VGAREG_DAC_DATA);
            byte green = IO.read(VGAREG_DAC_DATA);
            byte blue = IO.read(VGAREG_DAC_DATA);

            /* calculate clamped intensity, taken from VGABIOS */
            int i = (int) (((77 * red + 151 * green + 28 * blue) + 0x80) >>> 8);
            byte ic = (i > 0x3f) ? (byte) 0x3f : ((byte) (i & 0xff));
            setSingleDacRegister((byte) (start_reg + ct), ic, ic, ic);
        }
    }
}
/*--------------------------- end INT10Pal -----------------------------*/
