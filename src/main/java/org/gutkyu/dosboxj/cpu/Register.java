package org.gutkyu.dosboxj.cpu;

import org.gutkyu.dosboxj.hardware.memory.*;

public final class Register {
    // int
    public static final int FlagCF = 0x00000001;
    public static final int FlagPF = 0x00000004;
    public static final int FlagAF = 0x00000010;
    public static final int FlagZF = 0x00000040;
    public static final int FlagSF = 0x00000080;
    public static final int FlagOF = 0x00000800;

    public static final int FlagTF = 0x00000100;
    public static final int FlagIF = 0x00000200;
    public static final int FlagDF = 0x00000400;

    public static final int FlagIOPL = 0x00003000;
    public static final int FlagNT = 0x00004000;
    public static final int FlagVM = 0x00020000;
    public static final int FlagAC = 0x00040000;
    public static final int FlagID = 0x00200000;

    public static final int FMaskTest = (FlagCF | FlagPF | FlagAF | FlagZF | FlagSF | FlagOF);
    public static final int FMaskNormal = (FMaskTest | FlagDF | FlagTF | FlagIF | FlagAC);
    public static final int FMaskAll = (FMaskNormal | FlagIOPL | FlagNT);

    // -- #region Segments
    public static class Segment {
        short Val;
        int Phys; /* The phyiscal address start in emulated machine */
    }

    public static final byte SEG_NAME_ES = 0;
    public static final byte SEG_NAME_CS = 1;
    public static final byte SEG_NAME_SS = 2;
    public static final byte SEG_NAME_DS = 3;
    public static final byte SEG_NAME_FS = 4;
    public static final byte SEG_NAME_GS = 5;

    public static int[] SegmentVals = new int[8];
    public static int[] SegmentPhys = new int[8];
    // -- #endregion


    public static void init() {
        for (int i = 0; i < Regs.length; i++) {
            Regs[i] = new GeneralReg32();
        }
        IP = new GeneralReg32();

        Register.setRegEAX(0);
        Register.setRegEBX(0);
        Register.setRegECX(0);
        Register.setRegEDX(0);
        Register.setRegEDI(0);
        Register.setRegESI(0);
        Register.setRegEBP(0);
        Register.setRegESP(0);

        Register.segSet16(Register.SEG_NAME_CS, 0);
        Register.segSet16(Register.SEG_NAME_DS, 0);
        Register.segSet16(Register.SEG_NAME_ES, 0);
        Register.segSet16(Register.SEG_NAME_FS, 0);
        Register.segSet16(Register.SEG_NAME_GS, 0);
        Register.segSet16(Register.SEG_NAME_SS, 0);
    }

    public static class GeneralReg32 {

        // private byte byteH;
        // private byte byteL;
        // private short word;
        private int u32;

        // (byte)
        public void setByteL(int value) {
            u32 = (0xffffff00 & u32) | (0xff & value);
        }

        public int getByteL() {
            return 0xff & u32;
        }

        // (byte)
        public void setByteH(int value) {
            u32 = (0xffff00ff & u32) | ((0xff & value) << 8);
        }

        public int getByteH() {
            return 0xff & (u32 >>> 8);
        }

        // (uint16)
        public void setWord(int value) {
            u32 = (0xffff0000 & u32) | (0xffff & value);
        }

        // uint16
        public int getWord() {
            return 0xffff & u32;
        }

        public void setDWord(int value) {
            u32 = value;
        }

        public int getDWord() {
            return u32;
        }


    }

    public static GeneralReg32[] Regs = new GeneralReg32[8];
    public static GeneralReg32 IP;
    public static int Flags;

    public static void setFlagBit(int TYPE, boolean TEST) {
        if (TEST)
            Flags |= TYPE;
        else
            Flags &= ~TYPE;
    }

    public static void setFlagBit(int TYPE, int TEST) {
        if (TEST != 0)
            Flags |= TYPE;
        else
            Flags &= ~TYPE;
    }

    public static int getFlag(int TYPE) {
        return (Flags & TYPE);
    }

    public static boolean getFlagBool(int TYPE) {
        return (Flags & TYPE) != 0;
    }

    public static int getFlagIOPL() {
        return ((Flags & FlagIOPL) >>> 12);
    }


    // int SegPhys(byte index)
    public static int segPhys(int index) // SegNames
    {
        return SegmentPhys[index];
    }

    // uint16 SegValue(byte index)
    public static int segValue(int index) // SegNames
    {
        return 0xffff & SegmentVals[index];
    }

    // int RealMakeSeg(byte index, uint16 off)
    public static int realMakeSeg(int index, int off) // SegNames
    {
        return Memory.realMake(segValue(index), off);
    }

    // (byte index, uint16 val)
    public static void segSet16(int index, int val) // SegNames
    {
        SegmentVals[index] = 0xffff & val;
        SegmentPhys[index] = (0xffff & val) << 4;
    }

    // REGI16BIT
    public static final int AX = 0, CX = 1, DX = 2, BX = 3, SP = 4, BP = 5, SI = 6, DI = 7;
    // REGI8BIT
    public static final int AL = 0, CL = 1, DL = 2, BL = 3, AH = 4, CH = 5, DH = 6, BH = 7;

    // macros to convert a 3-bit register index to the correct register
    public static int getReg8L(int reg) {
        return Regs[reg].getByteL();
    }

    public static int getReg8H(int reg) {
        return Regs[reg].getByteH();
    }

    public static int getReg8(int reg) {
        return (reg & 4) != 0 ? getReg8H(reg & 3) : getReg8L(reg & 3);
    }

    public static void setReg8(int reg, byte val) {
        if ((reg & 4) != 0)
            Regs[reg & 3].setByteH(val);
        else
            Regs[reg & 3].setByteL(val);
    }

    public static int getReg16(int reg) {
        return Regs[reg].getWord();
    }

    // public static void SetReg16(int reg, short val) {
    public static void setReg16(int reg, int val) {
        Regs[reg].setWord(val);
    }

    public static int getReg32(int reg) {
        return (Regs[reg].getDWord());
    }

    public static void setReg32(int reg, int val) {
        Regs[reg].setDWord(val);
    }

    public static int getRegAL() {
        return Regs[AX].getByteL();
    }

    // public static void setRegAL(byte value)
    public static void setRegAL(int value) {
        Regs[AX].setByteL(value);
    }

    public static int getRegAH() {
        return Regs[AX].getByteH();
    }

    // public static void setRegAH(byte value)
    public static void setRegAH(int value) {
        Regs[AX].setByteH(value);
    }

    public static int getRegAX() {
        return Regs[AX].getWord();
    }

    // public static void setRegAX(short value)
    public static void setRegAX(int value) {
        Regs[AX].setWord(value);
    }

    public static int getRegEAX() {
        return Regs[AX].getDWord();
    }

    public static void setRegEAX(int value) {
        Regs[AX].setDWord(value);
    }


    public static int getRegBL() {
        return Regs[BX].getByteL();
    }

    // public static void setRegBL(byte value
    public static void setRegBL(int value) {
        Regs[BX].setByteL(value);
    }

    public static int getRegBH() {
        return Regs[BX].getByteH();
    }

    // public static void setRegBH(byte value)
    public static void setRegBH(int value) {
        Regs[BX].setByteH(value);
    }

    public static int getRegBX() {
        return Regs[BX].getWord();
    }

    // public static void setRegBX(short value)
    public static void setRegBX(int value) {
        Regs[BX].setWord(value);
    }

    public static int getRegEBX() {
        return Regs[BX].getDWord();
    }

    public static void setRegEBX(int value) {
        Regs[BX].setDWord(value);
    }


    public static int getRegCL() {
        return Regs[CX].getByteL();
    }

    // public static void setRegCL(byte value)
    public static void setRegCL(int value) {
        Regs[CX].setByteL(value);
    }

    public static int getRegCH() {
        return Regs[CX].getByteH();
    }

    // public static void setRegCH(byte value)
    public static void setRegCH(int value) {
        Regs[CX].setByteH(value);
    }

    public static int getRegCX() {
        return Regs[CX].getWord();
    }

    // public static void setRegCX(short value)
    public static void setRegCX(int value) {
        Regs[CX].setWord(value);
    }

    public static int getRegECX() {
        return Regs[CX].getDWord();
    }

    public static void setRegECX(int value) {
        Regs[CX].setDWord(value);
    }

    public static int getRegDL() {
        return Regs[DX].getByteL();
    }

    // (byte)
    public static void setRegDL(int value) {
        Regs[DX].setByteL(value);
    }

    public static int getRegDH() {
        return Regs[DX].getByteH();
    }

    // (byte)
    public static void setRegDH(int value) {
        Regs[DX].setByteH(value);
    }

    public static int getRegDX() {
        return Regs[DX].getWord();
    }

    public static void setRegDX(int value) {
        Regs[DX].setWord(value);
    }

    public static int getRegEDX() {
        return Regs[DX].getDWord();
    }

    public static void setRegEDX(int value) {
        Regs[DX].setDWord(value);
    }


    public static int getRegSI() {
        return Regs[SI].getWord();
    }

    public static void setRegSI(int value) {
        Regs[SI].setWord(value);
    }

    public static int getRegESI() {
        return Regs[SI].getDWord();
    }

    public static void setRegESI(int value) {
        Regs[SI].setDWord(value);
    }


    public static int getRegDI() {
        return Regs[DI].getWord();
    }

    // (uint16)
    public static void setRegDI(int value) {
        Regs[DI].setWord(value);
    }

    public static int getRegEDI() {
        return Regs[DI].getDWord();
    }

    public static void setRegEDI(int value) {
        Regs[DI].setDWord(value);
    }


    public static int getRegSP() {
        return Regs[SP].getWord();
    }

    public static void setRegSP(int value) {
        Regs[SP].setWord(value);
    }

    public static int getRegESP() {
        return Regs[SP].getDWord();
    }

    public static void setRegESP(int value) {
        Regs[SP].setDWord(value);
    }

    public static int getRegBP() {
        return Regs[BP].getWord();
    }

    // uint16
    public static void setRegBP(int value) {
        Regs[BP].setWord(value);
    }

    public static int getRegEBP() {
        return Regs[BP].getDWord();
    }

    public static void setRegEBP(int value) {
        Regs[BP].setDWord(value);
    }


    public static int getRegIP() {
        return IP.getWord();
    }

    // (uint16)
    public static void setRegIP(int value) {
        IP.setWord(value);
    }

    public static int getRegEIP() {
        return IP.getDWord();
    }

    public static void setRegEIP(int value) {
        IP.setDWord(value);
    }


}
