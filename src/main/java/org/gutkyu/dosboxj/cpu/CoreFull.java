package org.gutkyu.dosboxj.cpu;

import org.gutkyu.dosboxj.hardware.memory.paging.*;
import org.gutkyu.dosboxj.util.Convert;

public final class CoreFull extends CPUCore {
    /* opcode table normal,double, 66 normal, 66 double */
    private OpCode[] OpCodeTable = new OpCode[1024];
    private OpCode[][] Groups = new OpCode[16][8];

    private CoreFull() {
        super();
        for (int i = 0; i < 1024; i++) {
            OpCodeTable[i] = new OpCode(_tmpOpCodeTable[i][0], _tmpOpCodeTable[i][1],
                    _tmpOpCodeTable[i][2], _tmpOpCodeTable[i][3]);
        }
        _tmpOpCodeTable = null;

        for (int i = 0; i < 16; i++) {
            for (int j = 0; j < 8; j++) {
                Groups[i][j] = new OpCode(_tmpGroups[i][j][0], _tmpGroups[i][j][1],
                        _tmpGroups[i][j][2], _tmpGroups[i][j][3]);
            }
        }
        _tmpGroups = null;
    }

    @Override
    public String getDecorderName() {
        return "CoreFull";
    }

    private static class OP {
        // Offset(0)
        public byte b;// unsigned
        // Offset(0)
        public byte bs; // signed
        // Offset(0)
        public short w;// unsigned
        // Offset(0)
        public short ws;// signed
        // Offset(0)
        public int d;// unsigned
        // Offset(0)
        public int ds;// signed

        private int value = 0;

        public void set(byte value) {
            this.value &= 0xff & value;
        }

        public void set(short value) {
            this.value &= 0xffff & value;
        }

        public void set(int value) {
            this.value = value;
        }

        public void set(long value) {
            this.value = (int) value;
        }

        public byte b() {
            return (byte) value;
        }

        public byte bs() {
            return (byte) value;
        }

        public short w() {
            return (short) value;
        }

        public short ws() {
            return (short) value;
        }

        public int d() {
            return value;
        }

        public int ds() {
            return value;
        }
    }
    private static class OpCode {
        public byte load, op, save, extra;

        public OpCode(byte load, byte op, byte save, byte extra) {
            this.load = load;
            this.op = op;
            this.save = save;
            this.extra = extra;
        }
    }
    private static class FullData {
        public int entry;
        public int rm;
        public int rmEAA;
        public int rmOff;
        public int rmEAI;
        public int rmIndex;
        public int rmMod;
        public OpCode code;
        public int cseip;

        public OP op1, op2, imm;

        public int newFlags;
        public int segBase;
        public int cond;
        public boolean repz;
        public int prefix;
    }

    // 원래는 CPU_Core_Run()안에 내부변수
    // fetchB처럼 매크로를 함수로 변환한 부분때문에 class의 private field로 꺼내놓음
    // CPU_Core_Run()을 실행할때마다 초기화 팔요
    private FullData inst;

    @Override
    protected int fetchB() {
        int temp = Paging.memReadBInline(inst.cseip);
        inst.cseip += 1;
        return temp;
    }

    // protected ushort Fetchw()
    @Override
    protected int fetchW() {
        int temp = Paging.memReadWInline(inst.cseip);
        inst.cseip += 2;
        return temp;
    }

    // protected uint Fetchd()
    @Override
    protected int fetchD() {
        int temp = Paging.memReadDInline(inst.cseip);
        inst.cseip += 4;
        return temp;
    }

    @Override
    protected void loadIP() {
        inst.cseip = Register.segPhys(Register.SEG_NAME_CS) + Register.getRegEIP();
    }

    @Override
    protected void saveIP() {
        Register.setRegEIP((int) (inst.cseip - Register.segPhys(Register.SEG_NAME_CS)));
    }

    // protected uint GETIP()
    @Override
    protected int getIP() {
        return inst.cseip - Register.segPhys(Register.SEG_NAME_CS);
    }

    @Override
    public void initCPUCore() {

    }

    private static CPUCore _cpu = new CoreFull();

    public static CPUCore instance() {
        return _cpu;
    }

    /*--------------------------- begin CoreFullInstructions -----------------------------*/
    private void doDIMULW(short op1, short op2, short op3) {
        int res = op2 * op3;
        op1 = (short) (res & 0xffff);
        Flags.fillFlagsNoCFOF();
        if ((res > -32768) && (res < 32767)) {
            Register.setFlagBit(Register.FlagCF, false);
            Register.setFlagBit(Register.FlagOF, false);
        } else {
            Register.setFlagBit(Register.FlagCF, true);
            Register.setFlagBit(Register.FlagOF, true);
        }
    }

    private void doDIMULD(int op1, int op2, int op3) {
        long res = (long) op2 * (long) op3;
        op1 = (int) res;
        Flags.fillFlagsNoCFOF();
        if ((res > -((long) (2147483647) + 1)) && (res < (long) 2147483647)) {
            Register.setFlagBit(Register.FlagCF, false);
            Register.setFlagBit(Register.FlagOF, false);
        } else {
            Register.setFlagBit(Register.FlagCF, true);
            Register.setFlagBit(Register.FlagOF, true);
        }
    }

    private void doMULB(byte op1) {
        Register.setRegAX(Register.getRegAL() * op1);
        Flags.fillFlagsNoCFOF();
        Register.setFlagBit(Register.FlagZF, Register.getRegAL() == 0);
        if ((Register.getRegAX() & 0xff00) != 0) {
            Register.setFlagBit(Register.FlagCF, true);
            Register.setFlagBit(Register.FlagOF, true);
        } else {
            Register.setFlagBit(Register.FlagCF, false);
            Register.setFlagBit(Register.FlagOF, false);
        }

    }

    // private void doMULW(ushort op1)
    private void doMULW(short op1) {
        int tempu = Register.getRegAX() * op1;
        Register.setRegAX(tempu);
        Register.setRegDX(tempu >>> 16);
        Flags.fillFlagsNoCFOF();
        Register.setFlagBit(Register.FlagZF, Register.getRegAX() == 0);
        if (Register.getRegDX() != 0) {
            Register.setFlagBit(Register.FlagCF, true);
            Register.setFlagBit(Register.FlagOF, true);
        } else {
            Register.setFlagBit(Register.FlagCF, false);
            Register.setFlagBit(Register.FlagOF, false);
        }
    }

    private void doMULDFull(int op1) {
        long tempu = (long) Register.getRegEAX() * op1;
        Register.setRegEAX((int) (tempu));
        Register.setRegEDX((int) (tempu >>> 32));
        Flags.fillFlagsNoCFOF();
        Register.setFlagBit(Register.FlagZF, Register.getRegEAX() == 0);
        if (Register.getRegEDX() != 0) {
            Register.setFlagBit(Register.FlagCF, true);
            Register.setFlagBit(Register.FlagOF, true);
        } else {
            Register.setFlagBit(Register.FlagCF, false);
            Register.setFlagBit(Register.FlagOF, false);
        }
    }

    private void doIMULB(byte op1) {
        // Register.setRegAX((short)((sbyte)Register.getRegAL() * (sbyte)op1));
        Register.setRegAX(Register.getRegAL() * op1);
        Flags.fillFlagsNoCFOF();
        if ((Register.getRegAX() & 0xff80) == 0xff80 || (Register.getRegAX() & 0xff80) == 0x0000) {
            Register.setFlagBit(Register.FlagCF, false);
            Register.setFlagBit(Register.FlagOF, false);
        } else {
            Register.setFlagBit(Register.FlagCF, true);
            Register.setFlagBit(Register.FlagOF, true);
        }
    }

    // private void doIMULW(ushort op1)
    private void doIMULW(Short op1) {
        int temps = Register.getRegAX() * op1;
        Register.setRegAX(temps);
        Register.setRegDX(temps >>> 16);
        Flags.fillFlagsNoCFOF();
        if (((temps & 0xffff8000) == 0xffff8000 || (temps & 0xffff8000) == 0x0000)) {
            Register.setFlagBit(Register.FlagCF, false);
            Register.setFlagBit(Register.FlagOF, false);
        } else {
            Register.setFlagBit(Register.FlagCF, true);
            Register.setFlagBit(Register.FlagOF, true);
        }
    }

    private void doIMULDFull(int op1) {
        long temps = ((long) ((int) Register.getRegEAX()) * ((long) ((int) op1)));
        Register.setRegEAX((int) (temps));
        Register.setRegEDX((int) (temps >>> 32));
        Flags.fillFlagsNoCFOF();
        if ((Register.getRegEDX() == 0xffffffff) && (Register.getRegEAX() & 0x80000000) != 0) {
            Register.setFlagBit(Register.FlagCF, false);
            Register.setFlagBit(Register.FlagOF, false);
        } else if ((Register.getRegEDX() == 0x00000000) && (Register.getRegEAX() < 0x80000000)) {
            Register.setFlagBit(Register.FlagCF, false);
            Register.setFlagBit(Register.FlagOF, false);
        } else {
            Register.setFlagBit(Register.FlagCF, true);
            Register.setFlagBit(Register.FlagOF, true);
        }
    }
    // public SwitchReturn DIVB(byte op1)
    // {
    // int val = op1;
    // if (val == 0)
    // {
    // byte new_num = 0;
    // cpuModule.CPU_Exception(new_num, 0);
    // return SwitchReturn.SR_Continue;
    // }

    // int quo = regsModule.reg_ax / val;
    // byte rem = (byte)(regsModule.reg_ax % val);
    // byte quo8 = (byte)(quo & 0xff);
    // if (quo > 0xff)
    // {
    // byte new_num = 0;
    // cpuModule.CPU_Exception(new_num, 0);
    // return SwitchReturn.SR_Continue;
    // }
    // regsModule.reg_ah = rem;
    // regsModule.reg_al = quo8;

    // return SwitchReturn.SR_None;
    // }
    // public SwitchReturn DIVW(ushort op1)
    // {
    // int val = op1;
    // if (val == 0)
    // {
    // byte new_num = 0;
    // cpuModule.CPU_Exception(new_num, 0);
    // return SwitchReturn.SR_Continue;
    // }
    // int num = ((int)regsModule.reg_dx << 16) | regsModule.reg_ax;
    // int quo = num / val;
    // short rem = (short)(num % val);
    // short quo16 = (short)(quo & 0xffff);
    // if (quo != (int)quo16)
    // {
    // byte new_num = 0;
    // cpuModule.CPU_Exception(new_num, 0);
    // return SwitchReturn.SR_Continue;
    // }
    // regsModule.reg_dx = rem;
    // regsModule.reg_ax = quo16;
    // return SwitchReturn.SR_None;
    // }
    // public SwitchReturn DIVD_full(int op1)
    // {
    // int val = op1;
    // if (val == 0)
    // {
    // byte new_num = 0;
    // cpuModule.CPU_Exception(new_num, 0);
    // return SwitchReturn.SR_Continue;
    // }
    // UInt64 num = (((UInt64)regsModule.reg_edx) << 32) | regsModule.reg_eax;
    // UInt64 quo = num / val;
    // int rem = (int)(num % val);
    // int quo32 = (int)(quo & 0xffffffff);
    // if (quo != (UInt64)quo32)
    // {
    // byte new_num = 0;
    // cpuModule.CPU_Exception(new_num, 0);
    // return SwitchReturn.SR_Continue;
    // }
    // regsModule.reg_edx = rem;
    // regsModule.reg_eax = quo32;

    // return SwitchReturn.SR_None;
    // }
    // public SwitchReturn IDIVB(byte op1)
    // {
    // int val = (sbyte)(op1);
    // if (val == 0)
    // {
    // byte new_num = 0;
    // cpuModule.CPU_Exception(new_num, 0);
    // return SwitchReturn.SR_Continue;
    // }
    // int quo = ((Int16)regsModule.reg_ax) / val;
    // sbyte rem = (sbyte)((Int16)regsModule.reg_ax % val);
    // sbyte quo8s = (sbyte)(quo & 0xff);
    // if (quo != (Int16)quo8s)
    // {
    // byte new_num = 0;
    // cpuModule.CPU_Exception(new_num, 0);
    // return SwitchReturn.SR_Continue;
    // }
    // regsModule.reg_ah = (byte)rem;
    // regsModule.reg_al = (byte)quo8s;
    // return SwitchReturn.SR_None;
    // }
    // public SwitchReturn IDIVW(ushort op1)
    // {
    // int val = (Int16)(op1);
    // if (val == 0)
    // {
    // byte new_num = 0;
    // cpuModule.CPU_Exception(new_num, 0);
    // return SwitchReturn.SR_Continue;
    // }
    // int num = (int)((regsModule.reg_dx << 16) | regsModule.reg_ax);
    // int quo = num / val;
    // Int16 rem = (Int16)(num % val);
    // Int16 quo16s = (Int16)quo;
    // if (quo != (int)quo16s)
    // {
    // byte new_num = 0;
    // cpuModule.CPU_Exception(new_num, 0);
    // return SwitchReturn.SR_Continue;
    // }
    // regsModule.reg_dx = (short)rem;
    // regsModule.reg_ax = (short)quo16s;

    // return SwitchReturn.SR_None;
    // }
    // public SwitchReturn IDIVD_full(int op1)
    // {
    // int val = (int)(op1);
    // if (val == 0)
    // {
    // byte new_num = 0;
    // cpuModule.CPU_Exception(new_num, 0);
    // return SwitchReturn.SR_Continue;
    // }
    // long num = (long)((((UInt64)regsModule.reg_edx) << 32) | regsModule.reg_eax);
    // long quo = num / val;
    // int rem = (int)(num % val);
    // int quo32s = (int)(quo & 0xffffffff);
    // if (quo != (long)quo32s)
    // {
    // byte new_num = 0;
    // cpuModule.CPU_Exception(new_num, 0);
    // return SwitchReturn.SR_Continue;
    // }
    // regsModule.reg_edx = (int)rem;
    // regsModule.reg_eax = (int)quo32s;

    // return SwitchReturn.SR_None;
    // }
    /*--------------------------- end CoreFullInstructions -----------------------------*/
    /*--------------------------- begin CoreFullOpCodeTable -----------------------------*/
    enum OP1 {
        //@formatter:off
        L_N(0),
        L_SKIP(1),
        /* Grouped ones using MOD/RM */
        L_MODRM(2), L_MODRM_NVM(3), L_POPwRM(4), L_POPdRM(5),
        
        L_Ib(6), L_Iw(7), L_Id(8),
        L_Ibx(9), L_Iwx(10), L_Idx(11), // Sign extend
        L_Ifw(12), L_Ifd(13),
        L_OP(14),
        
        L_REGb(15), L_REGw(16), L_REGd(17),
        L_REGbIb(18), L_REGwIw(19), L_REGdId(20),
        L_POPw(21), L_POPd(22),
        L_POPfw(23), L_POPfd(24),
        L_SEG(25),

        L_INTO(26),

        L_VAL(27),
        L_PRESEG(28),
        L_DOUBLE(29),
        L_PREOP(30), L_PREADD(31), L_PREREP(32), L_PREREPNE(33),
        L_STRING(34),

        /* Direct ones */
        D_IRETw(35), D_IRETd(36),
        D_PUSHAw(37), D_PUSHAd(38),
        D_POPAw(39), D_POPAd(40),
        D_POPSEGw(41), D_POPSEGd(42),
        D_DAA(43), D_DAS(44),
        D_AAA(45), D_AAS(46),
        D_CBW(47), D_CWDE(48),
        D_CWD(49), D_CDQ(50),
        D_SETALC(51),
        D_XLAT(52),
        D_CLI(53), D_STI(54), D_STC(55), D_CLC(56), D_CMC(57), D_CLD(58), D_STD(59),
        D_NOP(60), D_WAIT(61),
        D_ENTERw(62), D_ENTERd(63),
        D_LEAVEw(64), D_LEAVEd(65),

        D_RETFw(66), D_RETFd(67),
        D_RETFwIw(68), D_RETFdIw(69),
        D_POPF(70), D_PUSHF(71),
        D_SAHF(72), D_LAHF(73),
        D_CPUID(74),
        D_HLT(75), D_CLTS(76),
        D_LOCK(77), D_ICEBP(78),
        L_ERROR(79);
        //@formatter:on

        private final byte value;

        private OP1(int value) {
            this.value = (byte) value;
        }

        public byte toValue() {
            return value;
        }
    }


    enum OP2 {
        //@formatter:off
        O_N(Flags.TypeFlag.LASTFLAG.toValue()),
        O_COND(1 + Flags.TypeFlag.LASTFLAG.toValue()),
        O_XCHG_AX(2 + Flags.TypeFlag.LASTFLAG.toValue()),
        O_XCHG_EAX(3 + Flags.TypeFlag.LASTFLAG.toValue()),
        O_IMULRw(4 + Flags.TypeFlag.LASTFLAG.toValue()),
        O_IMULRd(5 + Flags.TypeFlag.LASTFLAG.toValue()),
        O_BOUNDw(6 + Flags.TypeFlag.LASTFLAG.toValue()),
        O_BOUNDd(7 + Flags.TypeFlag.LASTFLAG.toValue()),
        O_CALLNw(8 + Flags.TypeFlag.LASTFLAG.toValue()),
        O_CALLNd(9 + Flags.TypeFlag.LASTFLAG.toValue()),
        O_CALLFw(10 + Flags.TypeFlag.LASTFLAG.toValue()),
        O_CALLFd(11 + Flags.TypeFlag.LASTFLAG.toValue()),
        O_JMPFw(12 + Flags.TypeFlag.LASTFLAG.toValue()),
        O_JMPFd(13 + Flags.TypeFlag.LASTFLAG.toValue()),
        O_OPAL(14 + Flags.TypeFlag.LASTFLAG.toValue()),
        O_ALOP(15 + Flags.TypeFlag.LASTFLAG.toValue()),
        O_OPAX(16 + Flags.TypeFlag.LASTFLAG.toValue()),
        O_AXOP(17 + Flags.TypeFlag.LASTFLAG.toValue()),
        O_OPEAX(18 + Flags.TypeFlag.LASTFLAG.toValue()),
        O_EAXOP(19 + Flags.TypeFlag.LASTFLAG.toValue()),
        O_INT(20 + Flags.TypeFlag.LASTFLAG.toValue()),
        O_SEGDS(21 + Flags.TypeFlag.LASTFLAG.toValue()),
        O_SEGES(22 + Flags.TypeFlag.LASTFLAG.toValue()),
        O_SEGFS(23 + Flags.TypeFlag.LASTFLAG.toValue()),
        O_SEGGS(24 + Flags.TypeFlag.LASTFLAG.toValue()),
        O_SEGSS(25 + Flags.TypeFlag.LASTFLAG.toValue()),
        O_LOOP(26 + Flags.TypeFlag.LASTFLAG.toValue()),
        O_LOOPZ(27 + Flags.TypeFlag.LASTFLAG.toValue()),
        O_LOOPNZ(28 + Flags.TypeFlag.LASTFLAG.toValue()),
        O_JCXZ(29 + Flags.TypeFlag.LASTFLAG.toValue()),
        O_INb(30 + Flags.TypeFlag.LASTFLAG.toValue()),
        O_INw(31 + Flags.TypeFlag.LASTFLAG.toValue()),
        O_INd(32 + Flags.TypeFlag.LASTFLAG.toValue()),
        O_OUTb(33 + Flags.TypeFlag.LASTFLAG.toValue()),
        O_OUTw(34 + Flags.TypeFlag.LASTFLAG.toValue()),
        O_OUTd(35 + Flags.TypeFlag.LASTFLAG.toValue()),
        O_NOT(36 + Flags.TypeFlag.LASTFLAG.toValue()),
        O_AAM(37 + Flags.TypeFlag.LASTFLAG.toValue()),
        O_AAD(38 + Flags.TypeFlag.LASTFLAG.toValue()),
        O_MULb(39 + Flags.TypeFlag.LASTFLAG.toValue()),
        O_MULw(40 + Flags.TypeFlag.LASTFLAG.toValue()),
        O_MULd(41 + Flags.TypeFlag.LASTFLAG.toValue()),
        O_IMULb(42 + Flags.TypeFlag.LASTFLAG.toValue()),
        O_IMULw(43 + Flags.TypeFlag.LASTFLAG.toValue()),
        O_IMULd(44 + Flags.TypeFlag.LASTFLAG.toValue()),
        O_DIVb(45 + Flags.TypeFlag.LASTFLAG.toValue()),
        O_DIVw(46 + Flags.TypeFlag.LASTFLAG.toValue()),
        O_DIVd(47 + Flags.TypeFlag.LASTFLAG.toValue()),
        O_IDIVb(48 + Flags.TypeFlag.LASTFLAG.toValue()),
        O_IDIVw(49 + Flags.TypeFlag.LASTFLAG.toValue()),
        O_IDIVd(50 + Flags.TypeFlag.LASTFLAG.toValue()),
        O_CBACK(51 + Flags.TypeFlag.LASTFLAG.toValue()),
        O_DSHLw(52 + Flags.TypeFlag.LASTFLAG.toValue()),
        O_DSHLd(53 + Flags.TypeFlag.LASTFLAG.toValue()),
        O_DSHRw(54 + Flags.TypeFlag.LASTFLAG.toValue()),
        O_DSHRd(55 + Flags.TypeFlag.LASTFLAG.toValue()),
        O_C_O(56 + Flags.TypeFlag.LASTFLAG.toValue()),
        O_C_NO(57 + Flags.TypeFlag.LASTFLAG.toValue()),
        O_C_B(58 + Flags.TypeFlag.LASTFLAG.toValue()),
        O_C_NB(59 + Flags.TypeFlag.LASTFLAG.toValue()),
        O_C_Z(60 + Flags.TypeFlag.LASTFLAG.toValue()),
        O_C_NZ(61 + Flags.TypeFlag.LASTFLAG.toValue()),
        O_C_BE(62 + Flags.TypeFlag.LASTFLAG.toValue()),
        O_C_NBE(63 + Flags.TypeFlag.LASTFLAG.toValue()),
        O_C_S(64 + Flags.TypeFlag.LASTFLAG.toValue()),
        O_C_NS(65 + Flags.TypeFlag.LASTFLAG.toValue()),
        O_C_P(66 + Flags.TypeFlag.LASTFLAG.toValue()),
        O_C_NP(67 + Flags.TypeFlag.LASTFLAG.toValue()),
        O_C_L(68 + Flags.TypeFlag.LASTFLAG.toValue()),
        O_C_NL(69 + Flags.TypeFlag.LASTFLAG.toValue()),
        O_C_LE(70 + Flags.TypeFlag.LASTFLAG.toValue()),
        O_C_NLE(71 + Flags.TypeFlag.LASTFLAG.toValue()),
        O_GRP6w(72 + Flags.TypeFlag.LASTFLAG.toValue()),
        O_GRP6d(73 + Flags.TypeFlag.LASTFLAG.toValue()),
        O_GRP7w(74 + Flags.TypeFlag.LASTFLAG.toValue()),
        O_GRP7d(75 + Flags.TypeFlag.LASTFLAG.toValue()),
        O_M_CRx_Rd(76 + Flags.TypeFlag.LASTFLAG.toValue()),
        O_M_Rd_CRx(77 + Flags.TypeFlag.LASTFLAG.toValue()),
        O_M_DRx_Rd(78 + Flags.TypeFlag.LASTFLAG.toValue()),
        O_M_Rd_DRx(79 + Flags.TypeFlag.LASTFLAG.toValue()),
        O_M_TRx_Rd(80 + Flags.TypeFlag.LASTFLAG.toValue()),
        O_M_Rd_TRx(81 + Flags.TypeFlag.LASTFLAG.toValue()),
        O_LAR(82 + Flags.TypeFlag.LASTFLAG.toValue()),
        O_LSL(83 + Flags.TypeFlag.LASTFLAG.toValue()),
        O_ARPL(84 + Flags.TypeFlag.LASTFLAG.toValue()),
        O_BTw(85 + Flags.TypeFlag.LASTFLAG.toValue()),
        O_BTSw(86 + Flags.TypeFlag.LASTFLAG.toValue()),
        O_BTRw(87 + Flags.TypeFlag.LASTFLAG.toValue()),
        O_BTCw(88 + Flags.TypeFlag.LASTFLAG.toValue()),
        O_BTd(89 + Flags.TypeFlag.LASTFLAG.toValue()),
        O_BTSd(90 + Flags.TypeFlag.LASTFLAG.toValue()),
        O_BTRd(91 + Flags.TypeFlag.LASTFLAG.toValue()),
        O_BTCd(92 + Flags.TypeFlag.LASTFLAG.toValue()),
        O_BSFw(93 + Flags.TypeFlag.LASTFLAG.toValue()),
        O_BSRw(94 + Flags.TypeFlag.LASTFLAG.toValue()),
        O_BSFd(95 + Flags.TypeFlag.LASTFLAG.toValue()),
        O_BSRd(96 + Flags.TypeFlag.LASTFLAG.toValue()),
        O_BSWAPw(97 + Flags.TypeFlag.LASTFLAG.toValue()),
        O_BSWAPd(98 + Flags.TypeFlag.LASTFLAG.toValue()),
        O_CMPXCHG(99 + Flags.TypeFlag.LASTFLAG.toValue()),
        O_FPU(100 + Flags.TypeFlag.LASTFLAG.toValue());
        //@formatter:on

        private final int value;

        private OP2(int value) {
            this.value = value;
        }

        public int toValue() {
            return value;
        }
    }

    enum OP3 {
        //@formatter:off
        S_N(0),
        S_C_Eb(1),
        S_Eb(2), S_Gb(3), S_EbGb(4),
        S_Ew(5), S_Gw(6), S_EwGw(7),
        S_Ed(8), S_Gd(9), S_EdGd(10),
        S_EdMw(11),


        S_REGb(12), S_REGw(13), S_REGd(14),
        S_PUSHw(15), S_PUSHd(16),
        S_SEGm(17),
        S_SEGGw(18), S_SEGGd(19),


        S_AIPw(20), S_C_AIPw(21),
        S_AIPd(22), S_C_AIPd(23),

        S_IP(24), S_IPIw(25);
        //@formatter:on

        private final byte value;

        private OP3(int value) {
            this.value = (byte) value;
        }

        public byte toValue() {
            return value;
        }
    }

    enum OP4 {
        //@formatter:off
        R_OUTSB(0), R_OUTSW(1), R_OUTSD(2),
        R_INSB(3), R_INSW(4), R_INSD(5),
        R_MOVSB(6), R_MOVSW(7), R_MOVSD(8),
        R_LODSB(9), R_LODSW(10), R_LODSD(11),
        R_STOSB(12), R_STOSW(13), R_STOSD(14),
        R_SCASB(15), R_SCASW(16), R_SCASD(17),
        R_CMPSB(18), R_CMPSW(19), R_CMPSD(20);
        //@formatter:on

        private final byte value;

        private OP4(int value) {
            this.value = (byte) value;
        }

        public byte toValue() {
            return value;
        }
    }

    enum op5 {
        //@formatter:off
        M_None(0),
        M_Ebx(1), M_Eb(2), M_Gb(3), M_EbGb(4), M_GbEb(5),
        M_Ewx(6), M_Ew(7), M_Gw(8), M_EwGw(9), M_GwEw(10), M_EwxGwx(11), M_EwGwt(12),
        M_Edx(13), M_Ed(14), M_Gd(15), M_EdGd(16), M_GdEd(17), M_EdxGdx(18), M_EdGdt(19),

        M_EbIb(20), M_EwIb(21), M_EdIb(22),
        M_EwIw(23), M_EwIbx(24), M_EwxIbx(25), M_EwxIwx(26), M_EwGwIb(27), M_EwGwCL(28),
        M_EdId(29), M_EdIbx(30), M_EdGdIb(31), M_EdGdCL(32),

        M_Efw(33), M_Efd(34),

        M_Ib(35), M_Iw(36), M_Id(37),


        M_SEG(38), M_EA(39),
        M_GRP(40),
        M_GRP_Ib(41), M_GRP_CL(42), M_GRP_1(43),

        M_POPw(44), M_POPd(50);
        //@formatter:on

        private final byte value;

        private op5(int value) {
            this.value = (byte) value;
        }

        public byte toValue() {
            return value;
        }
    }


    private byte[][] _tmpOpCodeTable = {
            /* 0x00 - 0x07 */
            {(byte) OP1.L_MODRM.toValue(), (byte) Flags.TypeFlag.ADDb.toValue(),
                    (byte) OP3.S_Eb.toValue(), (byte) op5.M_EbGb.toValue()},
            {(byte) OP1.L_MODRM.toValue(), (byte) Flags.TypeFlag.ADDw.toValue(),
                    (byte) OP3.S_Ew.toValue(), (byte) op5.M_EwGw.toValue()},
            {(byte) OP1.L_MODRM.toValue(), (byte) Flags.TypeFlag.ADDb.toValue(),
                    (byte) OP3.S_Gb.toValue(), (byte) op5.M_GbEb.toValue()},
            {(byte) OP1.L_MODRM.toValue(), (byte) Flags.TypeFlag.ADDw.toValue(),
                    (byte) OP3.S_Gw.toValue(), (byte) op5.M_GwEw.toValue()},
            {(byte) OP1.L_REGbIb.toValue(), (byte) Flags.TypeFlag.ADDb.toValue(),
                    (byte) OP3.S_REGb.toValue(), (byte) Register.AL},
            {(byte) OP1.L_REGwIw.toValue(), (byte) Flags.TypeFlag.ADDw.toValue(),
                    (byte) OP3.S_REGw.toValue(), (byte) Register.AX},
            {(byte) OP1.L_SEG.toValue(), 0, (byte) OP3.S_PUSHw.toValue(),
                    (byte) Register.SEG_NAME_ES},
            {(byte) OP1.D_POPSEGw.toValue(), 0, 0, (byte) Register.SEG_NAME_ES},
            /* 0x08 - 0x0f */
            {(byte) OP1.L_MODRM.toValue(), (byte) Flags.TypeFlag.ORb.toValue(),
                    (byte) OP3.S_Eb.toValue(), (byte) op5.M_EbGb.toValue()},
            {(byte) OP1.L_MODRM.toValue(), (byte) Flags.TypeFlag.ORw.toValue(),
                    (byte) OP3.S_Ew.toValue(), (byte) op5.M_EwGw.toValue()},
            {(byte) OP1.L_MODRM.toValue(), (byte) Flags.TypeFlag.ORb.toValue(),
                    (byte) OP3.S_Gb.toValue(), (byte) op5.M_GbEb.toValue()},
            {(byte) OP1.L_MODRM.toValue(), (byte) Flags.TypeFlag.ORw.toValue(),
                    (byte) OP3.S_Gw.toValue(), (byte) op5.M_GwEw.toValue()},
            {(byte) OP1.L_REGbIb.toValue(), (byte) Flags.TypeFlag.ORb.toValue(),
                    (byte) OP3.S_REGb.toValue(), (byte) Register.AL},
            {(byte) OP1.L_REGwIw.toValue(), (byte) Flags.TypeFlag.ORw.toValue(),
                    (byte) OP3.S_REGw.toValue(), (byte) Register.AX},
            {(byte) OP1.L_SEG.toValue(), 0, (byte) OP3.S_PUSHw.toValue(),
                    (byte) Register.SEG_NAME_CS},
            {(byte) OP1.L_DOUBLE.toValue(), 0, 0, 0},

            /* 0x10 - 0x17 */
            {(byte) OP1.L_MODRM.toValue(), (byte) Flags.TypeFlag.ADCb.toValue(),
                    (byte) OP3.S_Eb.toValue(), (byte) op5.M_EbGb.toValue()},
            {(byte) OP1.L_MODRM.toValue(), (byte) Flags.TypeFlag.ADCw.toValue(),
                    (byte) OP3.S_Ew.toValue(), (byte) op5.M_EwGw.toValue()},
            {(byte) OP1.L_MODRM.toValue(), (byte) Flags.TypeFlag.ADCb.toValue(),
                    (byte) OP3.S_Gb.toValue(), (byte) op5.M_GbEb.toValue()},
            {(byte) OP1.L_MODRM.toValue(), (byte) Flags.TypeFlag.ADCw.toValue(),
                    (byte) OP3.S_Gw.toValue(), (byte) op5.M_GwEw.toValue()},
            {(byte) OP1.L_REGbIb.toValue(), (byte) Flags.TypeFlag.ADCb.toValue(),
                    (byte) OP3.S_REGb.toValue(), (byte) Register.AL},
            {(byte) OP1.L_REGwIw.toValue(), (byte) Flags.TypeFlag.ADCw.toValue(),
                    (byte) OP3.S_REGw.toValue(), (byte) Register.AX},
            {(byte) OP1.L_SEG.toValue(), 0, (byte) OP3.S_PUSHw.toValue(),
                    (byte) Register.SEG_NAME_SS},
            {(byte) OP1.D_POPSEGw.toValue(), 0, 0, (byte) Register.SEG_NAME_SS},
            /* 0x18 - 0x1f */
            {(byte) OP1.L_MODRM.toValue(), (byte) Flags.TypeFlag.SBBb.toValue(),
                    (byte) OP3.S_Eb.toValue(), (byte) op5.M_EbGb.toValue()},
            {(byte) OP1.L_MODRM.toValue(), (byte) Flags.TypeFlag.SBBw.toValue(),
                    (byte) OP3.S_Ew.toValue(), (byte) op5.M_EwGw.toValue()},
            {(byte) OP1.L_MODRM.toValue(), (byte) Flags.TypeFlag.SBBb.toValue(),
                    (byte) OP3.S_Gb.toValue(), (byte) op5.M_GbEb.toValue()},
            {(byte) OP1.L_MODRM.toValue(), (byte) Flags.TypeFlag.SBBw.toValue(),
                    (byte) OP3.S_Gw.toValue(), (byte) op5.M_GwEw.toValue()},
            {(byte) OP1.L_REGbIb.toValue(), (byte) Flags.TypeFlag.SBBb.toValue(),
                    (byte) OP3.S_REGb.toValue(), (byte) Register.AL},
            {(byte) OP1.L_REGwIw.toValue(), (byte) Flags.TypeFlag.SBBw.toValue(),
                    (byte) OP3.S_REGw.toValue(), (byte) Register.AX},
            {(byte) OP1.L_SEG.toValue(), 0, (byte) OP3.S_PUSHw.toValue(),
                    (byte) Register.SEG_NAME_DS},
            {(byte) OP1.D_POPSEGw.toValue(), 0, 0, (byte) Register.SEG_NAME_DS},

            /* 0x20 - 0x27 */
            {(byte) OP1.L_MODRM.toValue(), (byte) Flags.TypeFlag.ANDb.toValue(),
                    (byte) OP3.S_Eb.toValue(), (byte) op5.M_EbGb.toValue()},
            {(byte) OP1.L_MODRM.toValue(), (byte) Flags.TypeFlag.ANDw.toValue(),
                    (byte) OP3.S_Ew.toValue(), (byte) op5.M_EwGw.toValue()},
            {(byte) OP1.L_MODRM.toValue(), (byte) Flags.TypeFlag.ANDb.toValue(),
                    (byte) OP3.S_Gb.toValue(), (byte) op5.M_GbEb.toValue()},
            {(byte) OP1.L_MODRM.toValue(), (byte) Flags.TypeFlag.ANDw.toValue(),
                    (byte) OP3.S_Gw.toValue(), (byte) op5.M_GwEw.toValue()},
            {(byte) OP1.L_REGbIb.toValue(), (byte) Flags.TypeFlag.ANDb.toValue(),
                    (byte) OP3.S_REGb.toValue(), (byte) Register.AL},
            {(byte) OP1.L_REGwIw.toValue(), (byte) Flags.TypeFlag.ANDw.toValue(),
                    (byte) OP3.S_REGw.toValue(), (byte) Register.AX},
            {(byte) OP1.L_PRESEG.toValue(), 0, 0, (byte) Register.SEG_NAME_ES},
            {(byte) OP1.D_DAA.toValue(), 0, 0, 0},
            /* 0x28 - 0x2f */
            {(byte) OP1.L_MODRM.toValue(), (byte) Flags.TypeFlag.SUBb.toValue(),
                    (byte) OP3.S_Eb.toValue(), (byte) op5.M_EbGb.toValue()},
            {(byte) OP1.L_MODRM.toValue(), (byte) Flags.TypeFlag.SUBw.toValue(),
                    (byte) OP3.S_Ew.toValue(), (byte) op5.M_EwGw.toValue()},
            {(byte) OP1.L_MODRM.toValue(), (byte) Flags.TypeFlag.SUBb.toValue(),
                    (byte) OP3.S_Gb.toValue(), (byte) op5.M_GbEb.toValue()},
            {(byte) OP1.L_MODRM.toValue(), (byte) Flags.TypeFlag.SUBw.toValue(),
                    (byte) OP3.S_Gw.toValue(), (byte) op5.M_GwEw.toValue()},
            {(byte) OP1.L_REGbIb.toValue(), (byte) Flags.TypeFlag.SUBb.toValue(),
                    (byte) OP3.S_REGb.toValue(), (byte) Register.AL},
            {(byte) OP1.L_REGwIw.toValue(), (byte) Flags.TypeFlag.SUBw.toValue(),
                    (byte) OP3.S_REGw.toValue(), (byte) Register.AX},
            {(byte) OP1.L_PRESEG.toValue(), 0, 0, (byte) Register.SEG_NAME_CS},
            {(byte) OP1.D_DAS.toValue(), 0, 0, 0},

            /* 0x30 - 0x37 */
            {(byte) OP1.L_MODRM.toValue(), (byte) Flags.TypeFlag.XORb.toValue(),
                    (byte) OP3.S_Eb.toValue(), (byte) op5.M_EbGb.toValue()},
            {(byte) OP1.L_MODRM.toValue(), (byte) Flags.TypeFlag.XORw.toValue(),
                    (byte) OP3.S_Ew.toValue(), (byte) op5.M_EwGw.toValue()},
            {(byte) OP1.L_MODRM.toValue(), (byte) Flags.TypeFlag.XORb.toValue(),
                    (byte) OP3.S_Gb.toValue(), (byte) op5.M_GbEb.toValue()},
            {(byte) OP1.L_MODRM.toValue(), (byte) Flags.TypeFlag.XORw.toValue(),
                    (byte) OP3.S_Gw.toValue(), (byte) op5.M_GwEw.toValue()},
            {(byte) OP1.L_REGbIb.toValue(), (byte) Flags.TypeFlag.XORb.toValue(),
                    (byte) OP3.S_REGb.toValue(), (byte) Register.AL},
            {(byte) OP1.L_REGwIw.toValue(), (byte) Flags.TypeFlag.XORw.toValue(),
                    (byte) OP3.S_REGw.toValue(), (byte) Register.AX},
            {(byte) OP1.L_PRESEG.toValue(), 0, 0, (byte) Register.SEG_NAME_SS},
            {(byte) OP1.D_AAA.toValue(), 0, 0, 0},
            /* 0x38 - 0x3f */
            {(byte) OP1.L_MODRM.toValue(), (byte) Flags.TypeFlag.CMPb.toValue(), 0,
                    (byte) op5.M_EbGb.toValue()},
            {(byte) OP1.L_MODRM.toValue(), (byte) Flags.TypeFlag.CMPw.toValue(), 0,
                    (byte) op5.M_EwGw.toValue()},
            {(byte) OP1.L_MODRM.toValue(), (byte) Flags.TypeFlag.CMPb.toValue(), 0,
                    (byte) op5.M_GbEb.toValue()},
            {(byte) OP1.L_MODRM.toValue(), (byte) Flags.TypeFlag.CMPw.toValue(), 0,
                    (byte) op5.M_GwEw.toValue()},
            {(byte) OP1.L_REGbIb.toValue(), (byte) Flags.TypeFlag.CMPb.toValue(), 0,
                    (byte) Register.AL},
            {(byte) OP1.L_REGwIw.toValue(), (byte) Flags.TypeFlag.CMPw.toValue(), 0,
                    (byte) Register.AX},
            {(byte) OP1.L_PRESEG.toValue(), 0, 0, (byte) Register.SEG_NAME_DS},
            {(byte) OP1.D_AAS.toValue(), 0, 0, 0},

            /* 0x40 - 0x47 */
            {(byte) OP1.L_REGw.toValue(), (byte) Flags.TypeFlag.INCw.toValue(),
                    (byte) OP3.S_REGw.toValue(), (byte) Register.AX},
            {(byte) OP1.L_REGw.toValue(), (byte) Flags.TypeFlag.INCw.toValue(),
                    (byte) OP3.S_REGw.toValue(), (byte) Register.CX},
            {(byte) OP1.L_REGw.toValue(), (byte) Flags.TypeFlag.INCw.toValue(),
                    (byte) OP3.S_REGw.toValue(), (byte) Register.DX},
            {(byte) OP1.L_REGw.toValue(), (byte) Flags.TypeFlag.INCw.toValue(),
                    (byte) OP3.S_REGw.toValue(), (byte) Register.BX},
            {(byte) OP1.L_REGw.toValue(), (byte) Flags.TypeFlag.INCw.toValue(),
                    (byte) OP3.S_REGw.toValue(), (byte) Register.SP},
            {(byte) OP1.L_REGw.toValue(), (byte) Flags.TypeFlag.INCw.toValue(),
                    (byte) OP3.S_REGw.toValue(), (byte) Register.BP},
            {(byte) OP1.L_REGw.toValue(), (byte) Flags.TypeFlag.INCw.toValue(),
                    (byte) OP3.S_REGw.toValue(), (byte) Register.SI},
            {(byte) OP1.L_REGw.toValue(), (byte) Flags.TypeFlag.INCw.toValue(),
                    (byte) OP3.S_REGw.toValue(), (byte) Register.DI},
            /* 0x48 - 0x4f */
            {(byte) OP1.L_REGw.toValue(), (byte) Flags.TypeFlag.DECw.toValue(),
                    (byte) OP3.S_REGw.toValue(), (byte) Register.AX},
            {(byte) OP1.L_REGw.toValue(), (byte) Flags.TypeFlag.DECw.toValue(),
                    (byte) OP3.S_REGw.toValue(), (byte) Register.CX},
            {(byte) OP1.L_REGw.toValue(), (byte) Flags.TypeFlag.DECw.toValue(),
                    (byte) OP3.S_REGw.toValue(), (byte) Register.DX},
            {(byte) OP1.L_REGw.toValue(), (byte) Flags.TypeFlag.DECw.toValue(),
                    (byte) OP3.S_REGw.toValue(), (byte) Register.BX},
            {(byte) OP1.L_REGw.toValue(), (byte) Flags.TypeFlag.DECw.toValue(),
                    (byte) OP3.S_REGw.toValue(), (byte) Register.SP},
            {(byte) OP1.L_REGw.toValue(), (byte) Flags.TypeFlag.DECw.toValue(),
                    (byte) OP3.S_REGw.toValue(), (byte) Register.BP},
            {(byte) OP1.L_REGw.toValue(), (byte) Flags.TypeFlag.DECw.toValue(),
                    (byte) OP3.S_REGw.toValue(), (byte) Register.SI},
            {(byte) OP1.L_REGw.toValue(), (byte) Flags.TypeFlag.DECw.toValue(),
                    (byte) OP3.S_REGw.toValue(), (byte) Register.DI},

            /* 0x50 - 0x57 */
            {(byte) OP1.L_REGw.toValue(), 0, (byte) OP3.S_PUSHw.toValue(), (byte) Register.AX},
            {(byte) OP1.L_REGw.toValue(), 0, (byte) OP3.S_PUSHw.toValue(), (byte) Register.CX},
            {(byte) OP1.L_REGw.toValue(), 0, (byte) OP3.S_PUSHw.toValue(), (byte) Register.DX},
            {(byte) OP1.L_REGw.toValue(), 0, (byte) OP3.S_PUSHw.toValue(), (byte) Register.BX},
            {(byte) OP1.L_REGw.toValue(), 0, (byte) OP3.S_PUSHw.toValue(), (byte) Register.SP},
            {(byte) OP1.L_REGw.toValue(), 0, (byte) OP3.S_PUSHw.toValue(), (byte) Register.BP},
            {(byte) OP1.L_REGw.toValue(), 0, (byte) OP3.S_PUSHw.toValue(), (byte) Register.SI},
            {(byte) OP1.L_REGw.toValue(), 0, (byte) OP3.S_PUSHw.toValue(), (byte) Register.DI},
            /* 0x58 - 0x5f */
            {(byte) OP1.L_POPw.toValue(), 0, (byte) OP3.S_REGw.toValue(), (byte) Register.AX},
            {(byte) OP1.L_POPw.toValue(), 0, (byte) OP3.S_REGw.toValue(), (byte) Register.CX},
            {(byte) OP1.L_POPw.toValue(), 0, (byte) OP3.S_REGw.toValue(), (byte) Register.DX},
            {(byte) OP1.L_POPw.toValue(), 0, (byte) OP3.S_REGw.toValue(), (byte) Register.BX},
            {(byte) OP1.L_POPw.toValue(), 0, (byte) OP3.S_REGw.toValue(), (byte) Register.SP},
            {(byte) OP1.L_POPw.toValue(), 0, (byte) OP3.S_REGw.toValue(), (byte) Register.BP},
            {(byte) OP1.L_POPw.toValue(), 0, (byte) OP3.S_REGw.toValue(), (byte) Register.SI},
            {(byte) OP1.L_POPw.toValue(), 0, (byte) OP3.S_REGw.toValue(), (byte) Register.DI},


            /* 0x60 - 0x67 */
            {(byte) OP1.D_PUSHAw.toValue(), 0, 0, 0}, {(byte) OP1.D_POPAw.toValue(), 0, 0, 0},
            {(byte) OP1.L_MODRM.toValue(), (byte) OP2.O_BOUNDw.toValue(), 0,
                    (byte) op5.M_Gw.toValue()},
            {(byte) OP1.L_MODRM_NVM.toValue(), (byte) OP2.O_ARPL.toValue(),
                    (byte) OP3.S_Ew.toValue(), (byte) op5.M_EwGw.toValue()},
            {(byte) OP1.L_PRESEG.toValue(), 0, 0, (byte) Register.SEG_NAME_FS},
            {(byte) OP1.L_PRESEG.toValue(), 0, 0, (byte) Register.SEG_NAME_GS},
            {(byte) OP1.L_PREOP.toValue(), 0, 0, 0}, {(byte) OP1.L_PREADD.toValue(), 0, 0, 0},
            /* 0x68 - 0x6f */
            {(byte) OP1.L_Iw.toValue(), 0, (byte) OP3.S_PUSHw.toValue(), 0},
            {(byte) OP1.L_MODRM.toValue(), (byte) OP2.O_IMULRw.toValue(), (byte) OP3.S_Gw.toValue(),
                    (byte) op5.M_EwxIwx.toValue()},
            {(byte) OP1.L_Ibx.toValue(), 0, (byte) OP3.S_PUSHw.toValue(), 0},
            {(byte) OP1.L_MODRM.toValue(), (byte) OP2.O_IMULRw.toValue(), (byte) OP3.S_Gw.toValue(),
                    (byte) op5.M_EwxIbx.toValue()},
            {(byte) OP1.L_STRING.toValue(), (byte) OP4.R_INSB.toValue(), 0, 0},
            {(byte) OP1.L_STRING.toValue(), (byte) OP4.R_INSW.toValue(), 0, 0},
            {(byte) OP1.L_STRING.toValue(), (byte) OP4.R_OUTSB.toValue(), 0, 0},
            {(byte) OP1.L_STRING.toValue(), (byte) OP4.R_OUTSW.toValue(), 0, 0},


            /* 0x70 - 0x77 */
            {(byte) OP1.L_Ibx.toValue(), (byte) OP2.O_C_O.toValue(), (byte) OP3.S_C_AIPw.toValue(),
                    0},
            {(byte) OP1.L_Ibx.toValue(), (byte) OP2.O_C_NO.toValue(), (byte) OP3.S_C_AIPw.toValue(),
                    0},
            {(byte) OP1.L_Ibx.toValue(), (byte) OP2.O_C_B.toValue(), (byte) OP3.S_C_AIPw.toValue(),
                    0},
            {(byte) OP1.L_Ibx.toValue(), (byte) OP2.O_C_NB.toValue(), (byte) OP3.S_C_AIPw.toValue(),
                    0},
            {(byte) OP1.L_Ibx.toValue(), (byte) OP2.O_C_Z.toValue(), (byte) OP3.S_C_AIPw.toValue(),
                    0},
            {(byte) OP1.L_Ibx.toValue(), (byte) OP2.O_C_NZ.toValue(), (byte) OP3.S_C_AIPw.toValue(),
                    0},
            {(byte) OP1.L_Ibx.toValue(), (byte) OP2.O_C_BE.toValue(), (byte) OP3.S_C_AIPw.toValue(),
                    0},
            {(byte) OP1.L_Ibx.toValue(), (byte) OP2.O_C_NBE.toValue(),
                    (byte) OP3.S_C_AIPw.toValue(), 0},
            /* 0x78 - 0x7f */
            {(byte) OP1.L_Ibx.toValue(), (byte) OP2.O_C_S.toValue(), (byte) OP3.S_C_AIPw.toValue(),
                    0},
            {(byte) OP1.L_Ibx.toValue(), (byte) OP2.O_C_NS.toValue(), (byte) OP3.S_C_AIPw.toValue(),
                    0},
            {(byte) OP1.L_Ibx.toValue(), (byte) OP2.O_C_P.toValue(), (byte) OP3.S_C_AIPw.toValue(),
                    0},
            {(byte) OP1.L_Ibx.toValue(), (byte) OP2.O_C_NP.toValue(), (byte) OP3.S_C_AIPw.toValue(),
                    0},
            {(byte) OP1.L_Ibx.toValue(), (byte) OP2.O_C_L.toValue(), (byte) OP3.S_C_AIPw.toValue(),
                    0},
            {(byte) OP1.L_Ibx.toValue(), (byte) OP2.O_C_NL.toValue(), (byte) OP3.S_C_AIPw.toValue(),
                    0},
            {(byte) OP1.L_Ibx.toValue(), (byte) OP2.O_C_LE.toValue(), (byte) OP3.S_C_AIPw.toValue(),
                    0},
            {(byte) OP1.L_Ibx.toValue(), (byte) OP2.O_C_NLE.toValue(),
                    (byte) OP3.S_C_AIPw.toValue(), 0},


            /* 0x80 - 0x87 */
            {(byte) OP1.L_MODRM.toValue(), 0, 0, (byte) op5.M_GRP.toValue()},
            {(byte) OP1.L_MODRM.toValue(), 1, 0, (byte) op5.M_GRP.toValue()},
            {(byte) OP1.L_MODRM.toValue(), 0, 0, (byte) op5.M_GRP.toValue()},
            {(byte) OP1.L_MODRM.toValue(), 3, 0, (byte) op5.M_GRP.toValue()},
            {(byte) OP1.L_MODRM.toValue(), (byte) Flags.TypeFlag.TESTb.toValue(), 0,
                    (byte) op5.M_EbGb.toValue()},
            {(byte) OP1.L_MODRM.toValue(), (byte) Flags.TypeFlag.TESTw.toValue(), 0,
                    (byte) op5.M_EwGw.toValue()},
            {(byte) OP1.L_MODRM.toValue(), 0, (byte) OP3.S_EbGb.toValue(),
                    (byte) op5.M_GbEb.toValue()},
            {(byte) OP1.L_MODRM.toValue(), 0, (byte) OP3.S_EwGw.toValue(),
                    (byte) op5.M_GwEw.toValue()},
            /* 0x88 - 0x8f */
            {(byte) OP1.L_MODRM.toValue(), 0, (byte) OP3.S_Eb.toValue(), (byte) op5.M_Gb.toValue()},
            {(byte) OP1.L_MODRM.toValue(), 0, (byte) OP3.S_Ew.toValue(), (byte) op5.M_Gw.toValue()},
            {(byte) OP1.L_MODRM.toValue(), 0, (byte) OP3.S_Gb.toValue(), (byte) op5.M_Eb.toValue()},
            {(byte) OP1.L_MODRM.toValue(), 0, (byte) OP3.S_Gw.toValue(), (byte) op5.M_Ew.toValue()},
            {(byte) OP1.L_MODRM.toValue(), 0, (byte) OP3.S_Ew.toValue(),
                    (byte) op5.M_SEG.toValue()},
            {(byte) OP1.L_MODRM.toValue(), 0, (byte) OP3.S_Gw.toValue(), (byte) op5.M_EA.toValue()},
            {(byte) OP1.L_MODRM.toValue(), 0, (byte) OP3.S_SEGm.toValue(),
                    (byte) op5.M_Ew.toValue()},
            {(byte) OP1.L_POPwRM.toValue(), 0, (byte) OP3.S_Ew.toValue(),
                    (byte) op5.M_None.toValue()},

            /* 0x90 - 0x97 */
            {(byte) OP1.D_NOP.toValue(), 0, 0, 0},
            {(byte) OP1.L_REGw.toValue(), (byte) OP2.O_XCHG_AX.toValue(),
                    (byte) OP3.S_REGw.toValue(), (byte) Register.CX},
            {(byte) OP1.L_REGw.toValue(), (byte) OP2.O_XCHG_AX.toValue(),
                    (byte) OP3.S_REGw.toValue(), (byte) Register.DX},
            {(byte) OP1.L_REGw.toValue(), (byte) OP2.O_XCHG_AX.toValue(),
                    (byte) OP3.S_REGw.toValue(), (byte) Register.BX},
            {(byte) OP1.L_REGw.toValue(), (byte) OP2.O_XCHG_AX.toValue(),
                    (byte) OP3.S_REGw.toValue(), (byte) Register.SP},
            {(byte) OP1.L_REGw.toValue(), (byte) OP2.O_XCHG_AX.toValue(),
                    (byte) OP3.S_REGw.toValue(), (byte) Register.BP},
            {(byte) OP1.L_REGw.toValue(), (byte) OP2.O_XCHG_AX.toValue(),
                    (byte) OP3.S_REGw.toValue(), (byte) Register.SI},
            {(byte) OP1.L_REGw.toValue(), (byte) OP2.O_XCHG_AX.toValue(),
                    (byte) OP3.S_REGw.toValue(), (byte) Register.DI},
            /* 0x98 - 0x9f */
            {(byte) OP1.D_CBW.toValue(), 0, 0, 0}, {(byte) OP1.D_CWD.toValue(), 0, 0, 0},
            {(byte) OP1.L_Ifw.toValue(), (byte) OP2.O_CALLFw.toValue(), 0, 0},
            {(byte) OP1.D_WAIT.toValue(), 0, 0, 0}, {(byte) OP1.D_PUSHF.toValue(), 0, 0, 0},
            {(byte) OP1.D_POPF.toValue(), 0, 0, 0}, {(byte) OP1.D_SAHF.toValue(), 0, 0, 0},
            {(byte) OP1.D_LAHF.toValue(), 0, 0, 0},


            /* 0xa0 - 0xa7 */
            {(byte) OP1.L_OP.toValue(), (byte) OP2.O_ALOP.toValue(), 0, 0},
            {(byte) OP1.L_OP.toValue(), (byte) OP2.O_AXOP.toValue(), 0, 0},
            {(byte) OP1.L_OP.toValue(), (byte) OP2.O_OPAL.toValue(), 0, 0},
            {(byte) OP1.L_OP.toValue(), (byte) OP2.O_OPAX.toValue(), 0, 0},
            {(byte) OP1.L_STRING.toValue(), (byte) OP4.R_MOVSB.toValue(), 0, 0},
            {(byte) OP1.L_STRING.toValue(), (byte) OP4.R_MOVSW.toValue(), 0, 0},
            {(byte) OP1.L_STRING.toValue(), (byte) OP4.R_CMPSB.toValue(), 0, 0},
            {(byte) OP1.L_STRING.toValue(), (byte) OP4.R_CMPSW.toValue(), 0, 0},
            /* 0xa8 - 0xaf */
            {(byte) OP1.L_REGbIb.toValue(), (byte) Flags.TypeFlag.TESTb.toValue(), 0,
                    (byte) Register.AL},
            {(byte) OP1.L_REGwIw.toValue(), (byte) Flags.TypeFlag.TESTw.toValue(), 0,
                    (byte) Register.AX},
            {(byte) OP1.L_STRING.toValue(), (byte) OP4.R_STOSB.toValue(), 0, 0},
            {(byte) OP1.L_STRING.toValue(), (byte) OP4.R_STOSW.toValue(), 0, 0},
            {(byte) OP1.L_STRING.toValue(), (byte) OP4.R_LODSB.toValue(), 0, 0},
            {(byte) OP1.L_STRING.toValue(), (byte) OP4.R_LODSW.toValue(), 0, 0},
            {(byte) OP1.L_STRING.toValue(), (byte) OP4.R_SCASB.toValue(), 0, 0},
            {(byte) OP1.L_STRING.toValue(), (byte) OP4.R_SCASW.toValue(), 0, 0},

            /* 0xb0 - 0xb7 */
            {(byte) OP1.L_Ib.toValue(), 0, (byte) OP3.S_REGb.toValue(), (byte) Register.AL},
            {(byte) OP1.L_Ib.toValue(), 0, (byte) OP3.S_REGb.toValue(), (byte) Register.CL},
            {(byte) OP1.L_Ib.toValue(), 0, (byte) OP3.S_REGb.toValue(), (byte) Register.DL},
            {(byte) OP1.L_Ib.toValue(), 0, (byte) OP3.S_REGb.toValue(), (byte) Register.BL},
            {(byte) OP1.L_Ib.toValue(), 0, (byte) OP3.S_REGb.toValue(), (byte) Register.AH},
            {(byte) OP1.L_Ib.toValue(), 0, (byte) OP3.S_REGb.toValue(), (byte) Register.CH},
            {(byte) OP1.L_Ib.toValue(), 0, (byte) OP3.S_REGb.toValue(), (byte) Register.DH},
            {(byte) OP1.L_Ib.toValue(), 0, (byte) OP3.S_REGb.toValue(), (byte) Register.BH},
            /* 0xb8 - 0xbf */
            {(byte) OP1.L_Iw.toValue(), 0, (byte) OP3.S_REGw.toValue(), (byte) Register.AX},
            {(byte) OP1.L_Iw.toValue(), 0, (byte) OP3.S_REGw.toValue(), (byte) Register.CX},
            {(byte) OP1.L_Iw.toValue(), 0, (byte) OP3.S_REGw.toValue(), (byte) Register.DX},
            {(byte) OP1.L_Iw.toValue(), 0, (byte) OP3.S_REGw.toValue(), (byte) Register.BX},
            {(byte) OP1.L_Iw.toValue(), 0, (byte) OP3.S_REGw.toValue(), (byte) Register.SP},
            {(byte) OP1.L_Iw.toValue(), 0, (byte) OP3.S_REGw.toValue(), (byte) Register.BP},
            {(byte) OP1.L_Iw.toValue(), 0, (byte) OP3.S_REGw.toValue(), (byte) Register.SI},
            {(byte) OP1.L_Iw.toValue(), 0, (byte) OP3.S_REGw.toValue(), (byte) Register.DI},

            /* 0xc0 - 0xc7 */
            {(byte) OP1.L_MODRM.toValue(), 5, 0, (byte) op5.M_GRP_Ib.toValue()},
            {(byte) OP1.L_MODRM.toValue(), 6, 0, (byte) op5.M_GRP_Ib.toValue()},
            {(byte) OP1.L_POPw.toValue(), 0, (byte) OP3.S_IPIw.toValue(), 0},
            {(byte) OP1.L_POPw.toValue(), 0, (byte) OP3.S_IP.toValue(), 0},
            {(byte) OP1.L_MODRM.toValue(), (byte) OP2.O_SEGES.toValue(),
                    (byte) OP3.S_SEGGw.toValue(), (byte) op5.M_Efw.toValue()},
            {(byte) OP1.L_MODRM.toValue(), (byte) OP2.O_SEGDS.toValue(),
                    (byte) OP3.S_SEGGw.toValue(), (byte) op5.M_Efw.toValue()},
            {(byte) OP1.L_MODRM.toValue(), 0, (byte) OP3.S_Eb.toValue(), (byte) op5.M_Ib.toValue()},
            {(byte) OP1.L_MODRM.toValue(), 0, (byte) OP3.S_Ew.toValue(), (byte) op5.M_Iw.toValue()},
            /* 0xc8 - 0xcf */
            {(byte) OP1.D_ENTERw.toValue(), 0, 0, 0}, {(byte) OP1.D_LEAVEw.toValue(), 0, 0, 0},
            {(byte) OP1.D_RETFwIw.toValue(), 0, 0, 0}, {(byte) OP1.D_RETFw.toValue(), 0, 0, 0},
            {(byte) OP1.L_VAL.toValue(), (byte) OP2.O_INT.toValue(), 0, 3},
            {(byte) OP1.L_Ib.toValue(), (byte) OP2.O_INT.toValue(), 0, 0},
            {(byte) OP1.L_INTO.toValue(), (byte) OP2.O_INT.toValue(), 0, 0},
            {(byte) OP1.D_IRETw.toValue(), 0, 0, 0},

            /* 0xd0 - 0xd7 */
            {(byte) OP1.L_MODRM.toValue(), 5, 0, (byte) op5.M_GRP_1.toValue()},
            {(byte) OP1.L_MODRM.toValue(), 6, 0, (byte) op5.M_GRP_1.toValue()},
            {(byte) OP1.L_MODRM.toValue(), 5, 0, (byte) op5.M_GRP_CL.toValue()},
            {(byte) OP1.L_MODRM.toValue(), 6, 0, (byte) op5.M_GRP_CL.toValue()},
            {(byte) OP1.L_Ib.toValue(), (byte) OP2.O_AAM.toValue(), 0, 0},
            {(byte) OP1.L_Ib.toValue(), (byte) OP2.O_AAD.toValue(), 0, 0},
            {(byte) OP1.D_SETALC.toValue(), 0, 0, 0}, {(byte) OP1.D_XLAT.toValue(), 0, 0, 0},
            // TODO FPU
            /* 0xd8 - 0xdf */
            {(byte) OP1.L_MODRM.toValue(), (byte) OP2.O_FPU.toValue(), 0, 0},
            {(byte) OP1.L_MODRM.toValue(), (byte) OP2.O_FPU.toValue(), 1, 0},
            {(byte) OP1.L_MODRM.toValue(), (byte) OP2.O_FPU.toValue(), 2, 0},
            {(byte) OP1.L_MODRM.toValue(), (byte) OP2.O_FPU.toValue(), 3, 0},
            {(byte) OP1.L_MODRM.toValue(), (byte) OP2.O_FPU.toValue(), 4, 0},
            {(byte) OP1.L_MODRM.toValue(), (byte) OP2.O_FPU.toValue(), 5, 0},
            {(byte) OP1.L_MODRM.toValue(), (byte) OP2.O_FPU.toValue(), 6, 0},
            {(byte) OP1.L_MODRM.toValue(), (byte) OP2.O_FPU.toValue(), 7, 0},

            /* 0xe0 - 0xe7 */
            {(byte) OP1.L_Ibx.toValue(), (byte) OP2.O_LOOPNZ.toValue(), (byte) OP3.S_AIPw.toValue(),
                    0},
            {(byte) OP1.L_Ibx.toValue(), (byte) OP2.O_LOOPZ.toValue(), (byte) OP3.S_AIPw.toValue(),
                    0},
            {(byte) OP1.L_Ibx.toValue(), (byte) OP2.O_LOOP.toValue(), (byte) OP3.S_AIPw.toValue(),
                    0},
            {(byte) OP1.L_Ibx.toValue(), (byte) OP2.O_JCXZ.toValue(), (byte) OP3.S_AIPw.toValue(),
                    0},
            {(byte) OP1.L_Ib.toValue(), (byte) OP2.O_INb.toValue(), 0, 0},
            {(byte) OP1.L_Ib.toValue(), (byte) OP2.O_INw.toValue(), 0, 0},
            {(byte) OP1.L_Ib.toValue(), (byte) OP2.O_OUTb.toValue(), 0, 0},
            {(byte) OP1.L_Ib.toValue(), (byte) OP2.O_OUTw.toValue(), 0, 0},
            /* 0xe8 - 0xef */
            {(byte) OP1.L_Iw.toValue(), (byte) OP2.O_CALLNw.toValue(), (byte) OP3.S_AIPw.toValue(),
                    0},
            {(byte) OP1.L_Iwx.toValue(), 0, (byte) OP3.S_AIPw.toValue(), 0},
            {(byte) OP1.L_Ifw.toValue(), (byte) OP2.O_JMPFw.toValue(), 0, 0},
            {(byte) OP1.L_Ibx.toValue(), 0, (byte) OP3.S_AIPw.toValue(), 0},
            {(byte) OP1.L_REGw.toValue(), (byte) OP2.O_INb.toValue(), 0, (byte) Register.DX},
            {(byte) OP1.L_REGw.toValue(), (byte) OP2.O_INw.toValue(), 0, (byte) Register.DX},
            {(byte) OP1.L_REGw.toValue(), (byte) OP2.O_OUTb.toValue(), 0, (byte) Register.DX},
            {(byte) OP1.L_REGw.toValue(), (byte) OP2.O_OUTw.toValue(), 0, (byte) Register.DX},

            /* 0xf0 - 0xf7 */
            {(byte) OP1.D_LOCK.toValue(), 0, 0, 0}, {(byte) OP1.D_ICEBP.toValue(), 0, 0, 0},
            {(byte) OP1.L_PREREPNE.toValue(), 0, 0, 0}, {(byte) OP1.L_PREREP.toValue(), 0, 0, 0},
            {(byte) OP1.D_HLT.toValue(), 0, 0, 0}, {(byte) OP1.D_CMC.toValue(), 0, 0, 0},
            {(byte) OP1.L_MODRM.toValue(), 8, 0, (byte) op5.M_GRP.toValue()},
            {(byte) OP1.L_MODRM.toValue(), 9, 0, (byte) op5.M_GRP.toValue()},
            /* 0xf8 - 0xff */
            {(byte) OP1.D_CLC.toValue(), 0, 0, 0}, {(byte) OP1.D_STC.toValue(), 0, 0, 0},
            {(byte) OP1.D_CLI.toValue(), 0, 0, 0}, {(byte) OP1.D_STI.toValue(), 0, 0, 0},
            {(byte) OP1.D_CLD.toValue(), 0, 0, 0}, {(byte) OP1.D_STD.toValue(), 0, 0, 0},
            {(byte) OP1.L_MODRM.toValue(), 0xb, 0, (byte) op5.M_GRP.toValue()},
            {(byte) OP1.L_MODRM.toValue(), 0xc, 0, (byte) op5.M_GRP.toValue()},

            /* 0x100 - 0x107 */
            {(byte) OP1.L_MODRM.toValue(), (byte) OP2.O_GRP6w.toValue(), (byte) OP3.S_Ew.toValue(),
                    (byte) op5.M_Ew.toValue()},
            {(byte) OP1.L_MODRM.toValue(), (byte) OP2.O_GRP7w.toValue(), (byte) OP3.S_Ew.toValue(),
                    (byte) op5.M_Ew.toValue()},
            {(byte) OP1.L_MODRM_NVM.toValue(), (byte) OP2.O_LAR.toValue(),
                    (byte) OP3.S_Gw.toValue(), (byte) op5.M_EwGw.toValue()},
            {(byte) OP1.L_MODRM_NVM.toValue(), (byte) OP2.O_LSL.toValue(),
                    (byte) OP3.S_Gw.toValue(), (byte) op5.M_EwGw.toValue()},
            {0, 0, 0, 0}, {0, 0, 0, 0}, {(byte) OP1.D_CLTS.toValue(), 0, 0, 0}, {0, 0, 0, 0},
            /* 0x108 - 0x10f */
            {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0},
            {0, 0, 0, 0}, {0, 0, 0, 0},

            /* 0x110 - 0x117 */
            {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0},
            {0, 0, 0, 0}, {0, 0, 0, 0},
            /* 0x118 - 0x11f */
            {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0},
            {0, 0, 0, 0}, {0, 0, 0, 0},

            /* 0x120 - 0x127 */
            {(byte) OP1.L_MODRM.toValue(), (byte) OP2.O_M_Rd_CRx.toValue(),
                    (byte) OP3.S_Ed.toValue(), 0},
            {(byte) OP1.L_MODRM.toValue(), (byte) OP2.O_M_Rd_DRx.toValue(),
                    (byte) OP3.S_Ed.toValue(), 0},
            {(byte) OP1.L_MODRM.toValue(), (byte) OP2.O_M_CRx_Rd.toValue(), 0,
                    (byte) op5.M_Ed.toValue()},
            {(byte) OP1.L_MODRM.toValue(), (byte) OP2.O_M_DRx_Rd.toValue(), 0,
                    (byte) op5.M_Ed.toValue()},
            {(byte) OP1.L_MODRM.toValue(), (byte) OP2.O_M_Rd_TRx.toValue(),
                    (byte) OP3.S_Ed.toValue(), 0},
            {0, 0, 0, 0},
            {(byte) OP1.L_MODRM.toValue(), (byte) OP2.O_M_TRx_Rd.toValue(), 0,
                    (byte) op5.M_Ed.toValue()},
            {0, 0, 0, 0},

            /* 0x128 - 0x12f */
            {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0},
            {0, 0, 0, 0}, {0, 0, 0, 0},

            /* 0x130 - 0x137 */
            {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0},
            {0, 0, 0, 0}, {0, 0, 0, 0},
            /* 0x138 - 0x13f */
            {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0},
            {0, 0, 0, 0}, {0, 0, 0, 0},

            /* 0x140 - 0x147 */
            {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0},
            {0, 0, 0, 0}, {0, 0, 0, 0},
            /* 0x148 - 0x14f */
            {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0},
            {0, 0, 0, 0}, {0, 0, 0, 0},

            /* 0x150 - 0x157 */
            {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0},
            {0, 0, 0, 0}, {0, 0, 0, 0},
            /* 0x158 - 0x15f */
            {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0},
            {0, 0, 0, 0}, {0, 0, 0, 0},

            /* 0x160 - 0x167 */
            {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0},
            {0, 0, 0, 0}, {0, 0, 0, 0},
            /* 0x168 - 0x16f */
            {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0},
            {0, 0, 0, 0}, {0, 0, 0, 0},


            /* 0x170 - 0x177 */
            {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0},
            {0, 0, 0, 0}, {0, 0, 0, 0},
            /* 0x178 - 0x17f */
            {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0},
            {0, 0, 0, 0}, {0, 0, 0, 0},

            /* 0x180 - 0x187 */
            {(byte) OP1.L_Iwx.toValue(), (byte) OP2.O_C_O.toValue(), (byte) OP3.S_C_AIPw.toValue(),
                    0},
            {(byte) OP1.L_Iwx.toValue(), (byte) OP2.O_C_NO.toValue(), (byte) OP3.S_C_AIPw.toValue(),
                    0},
            {(byte) OP1.L_Iwx.toValue(), (byte) OP2.O_C_B.toValue(), (byte) OP3.S_C_AIPw.toValue(),
                    0},
            {(byte) OP1.L_Iwx.toValue(), (byte) OP2.O_C_NB.toValue(), (byte) OP3.S_C_AIPw.toValue(),
                    0},
            {(byte) OP1.L_Iwx.toValue(), (byte) OP2.O_C_Z.toValue(), (byte) OP3.S_C_AIPw.toValue(),
                    0},
            {(byte) OP1.L_Iwx.toValue(), (byte) OP2.O_C_NZ.toValue(), (byte) OP3.S_C_AIPw.toValue(),
                    0},
            {(byte) OP1.L_Iwx.toValue(), (byte) OP2.O_C_BE.toValue(), (byte) OP3.S_C_AIPw.toValue(),
                    0},
            {(byte) OP1.L_Iwx.toValue(), (byte) OP2.O_C_NBE.toValue(),
                    (byte) OP3.S_C_AIPw.toValue(), 0},
            /* 0x188 - 0x18f */
            {(byte) OP1.L_Iwx.toValue(), (byte) OP2.O_C_S.toValue(), (byte) OP3.S_C_AIPw.toValue(),
                    0},
            {(byte) OP1.L_Iwx.toValue(), (byte) OP2.O_C_NS.toValue(), (byte) OP3.S_C_AIPw.toValue(),
                    0},
            {(byte) OP1.L_Iwx.toValue(), (byte) OP2.O_C_P.toValue(), (byte) OP3.S_C_AIPw.toValue(),
                    0},
            {(byte) OP1.L_Iwx.toValue(), (byte) OP2.O_C_NP.toValue(), (byte) OP3.S_C_AIPw.toValue(),
                    0},
            {(byte) OP1.L_Iwx.toValue(), (byte) OP2.O_C_L.toValue(), (byte) OP3.S_C_AIPw.toValue(),
                    0},
            {(byte) OP1.L_Iwx.toValue(), (byte) OP2.O_C_NL.toValue(), (byte) OP3.S_C_AIPw.toValue(),
                    0},
            {(byte) OP1.L_Iwx.toValue(), (byte) OP2.O_C_LE.toValue(), (byte) OP3.S_C_AIPw.toValue(),
                    0},
            {(byte) OP1.L_Iwx.toValue(), (byte) OP2.O_C_NLE.toValue(),
                    (byte) OP3.S_C_AIPw.toValue(), 0},

            /* 0x190 - 0x197 */
            {(byte) OP1.L_MODRM.toValue(), (byte) OP2.O_C_O.toValue(), (byte) OP3.S_C_Eb.toValue(),
                    0},
            {(byte) OP1.L_MODRM.toValue(), (byte) OP2.O_C_NO.toValue(), (byte) OP3.S_C_Eb.toValue(),
                    0},
            {(byte) OP1.L_MODRM.toValue(), (byte) OP2.O_C_B.toValue(), (byte) OP3.S_C_Eb.toValue(),
                    0},
            {(byte) OP1.L_MODRM.toValue(), (byte) OP2.O_C_NB.toValue(), (byte) OP3.S_C_Eb.toValue(),
                    0},
            {(byte) OP1.L_MODRM.toValue(), (byte) OP2.O_C_Z.toValue(), (byte) OP3.S_C_Eb.toValue(),
                    0},
            {(byte) OP1.L_MODRM.toValue(), (byte) OP2.O_C_NZ.toValue(), (byte) OP3.S_C_Eb.toValue(),
                    0},
            {(byte) OP1.L_MODRM.toValue(), (byte) OP2.O_C_BE.toValue(), (byte) OP3.S_C_Eb.toValue(),
                    0},
            {(byte) OP1.L_MODRM.toValue(), (byte) OP2.O_C_NBE.toValue(),
                    (byte) OP3.S_C_Eb.toValue(), 0},
            /* 0x198 - 0x19f */
            {(byte) OP1.L_MODRM.toValue(), (byte) OP2.O_C_S.toValue(), (byte) OP3.S_C_Eb.toValue(),
                    0},
            {(byte) OP1.L_MODRM.toValue(), (byte) OP2.O_C_NS.toValue(), (byte) OP3.S_C_Eb.toValue(),
                    0},
            {(byte) OP1.L_MODRM.toValue(), (byte) OP2.O_C_P.toValue(), (byte) OP3.S_C_Eb.toValue(),
                    0},
            {(byte) OP1.L_MODRM.toValue(), (byte) OP2.O_C_NP.toValue(), (byte) OP3.S_C_Eb.toValue(),
                    0},
            {(byte) OP1.L_MODRM.toValue(), (byte) OP2.O_C_L.toValue(), (byte) OP3.S_C_Eb.toValue(),
                    0},
            {(byte) OP1.L_MODRM.toValue(), (byte) OP2.O_C_NL.toValue(), (byte) OP3.S_C_Eb.toValue(),
                    0},
            {(byte) OP1.L_MODRM.toValue(), (byte) OP2.O_C_LE.toValue(), (byte) OP3.S_C_Eb.toValue(),
                    0},
            {(byte) OP1.L_MODRM.toValue(), (byte) OP2.O_C_NLE.toValue(),
                    (byte) OP3.S_C_Eb.toValue(), 0},

            /* 0x1a0 - 0x1a7 */
            {(byte) OP1.L_SEG.toValue(), 0, (byte) OP3.S_PUSHw.toValue(),
                    (byte) Register.SEG_NAME_FS},
            {(byte) OP1.D_POPSEGw.toValue(), 0, 0, (byte) Register.SEG_NAME_FS},
            {(byte) OP1.D_CPUID.toValue(), 0, 0, 0},
            {(byte) OP1.L_MODRM.toValue(), (byte) OP2.O_BTw.toValue(), (byte) OP3.S_Ew.toValue(),
                    (byte) op5.M_EwGwt.toValue()},
            {(byte) OP1.L_MODRM.toValue(), (byte) OP2.O_DSHLw.toValue(), (byte) OP3.S_Ew.toValue(),
                    (byte) op5.M_EwGwIb.toValue()},
            {(byte) OP1.L_MODRM.toValue(), (byte) OP2.O_DSHLw.toValue(), (byte) OP3.S_Ew.toValue(),
                    (byte) op5.M_EwGwCL.toValue()},
            {0, 0, 0, 0}, {0, 0, 0, 0},
            /* 0x1a8 - 0x1af */
            {(byte) OP1.L_SEG.toValue(), 0, (byte) OP3.S_PUSHw.toValue(),
                    (byte) Register.SEG_NAME_GS},
            {(byte) OP1.D_POPSEGw.toValue(), 0, 0, (byte) Register.SEG_NAME_GS}, {0, 0, 0, 0},
            {(byte) OP1.L_MODRM.toValue(), (byte) OP2.O_BTSw.toValue(), (byte) OP3.S_Ew.toValue(),
                    (byte) op5.M_EwGwt.toValue()},
            {(byte) OP1.L_MODRM.toValue(), (byte) OP2.O_DSHRw.toValue(), (byte) OP3.S_Ew.toValue(),
                    (byte) op5.M_EwGwIb.toValue()},
            {(byte) OP1.L_MODRM.toValue(), (byte) OP2.O_DSHRw.toValue(), (byte) OP3.S_Ew.toValue(),
                    (byte) op5.M_EwGwCL.toValue()},
            {0, 0, 0, 0},
            {(byte) OP1.L_MODRM.toValue(), (byte) OP2.O_IMULRw.toValue(), (byte) OP3.S_Gw.toValue(),
                    (byte) op5.M_EwxGwx.toValue()},

            /* 0x1b0 - 0x1b7 */
            {0, 0, 0, 0}, {0, 0, 0, 0},
            {(byte) OP1.L_MODRM.toValue(), (byte) OP2.O_SEGSS.toValue(),
                    (byte) OP3.S_SEGGw.toValue(), (byte) op5.M_Efw.toValue()},
            {(byte) OP1.L_MODRM.toValue(), (byte) OP2.O_BTRw.toValue(), (byte) OP3.S_Ew.toValue(),
                    (byte) op5.M_EwGwt.toValue()},
            {(byte) OP1.L_MODRM.toValue(), (byte) OP2.O_SEGFS.toValue(),
                    (byte) OP3.S_SEGGw.toValue(), (byte) op5.M_Efw.toValue()},
            {(byte) OP1.L_MODRM.toValue(), (byte) OP2.O_SEGGS.toValue(),
                    (byte) OP3.S_SEGGw.toValue(), (byte) op5.M_Efw.toValue()},
            {(byte) OP1.L_MODRM.toValue(), 0, (byte) OP3.S_Gw.toValue(), (byte) op5.M_Eb.toValue()},
            {(byte) OP1.L_MODRM.toValue(), 0, (byte) OP3.S_Gw.toValue(), (byte) op5.M_Ew.toValue()},
            /* 0x1b8 - 0x1bf */
            {0, 0, 0, 0}, {0, 0, 0, 0},
            {(byte) OP1.L_MODRM.toValue(), 0xe, 0, (byte) op5.M_GRP.toValue()},
            {(byte) OP1.L_MODRM.toValue(), (byte) OP2.O_BTCw.toValue(), (byte) OP3.S_Ew.toValue(),
                    (byte) op5.M_EwGwt.toValue()},
            {(byte) OP1.L_MODRM.toValue(), (byte) OP2.O_BSFw.toValue(), (byte) OP3.S_Gw.toValue(),
                    (byte) op5.M_Ew.toValue()},
            {(byte) OP1.L_MODRM.toValue(), (byte) OP2.O_BSRw.toValue(), (byte) OP3.S_Gw.toValue(),
                    (byte) op5.M_Ew.toValue()},
            {(byte) OP1.L_MODRM.toValue(), 0, (byte) OP3.S_Gw.toValue(),
                    (byte) op5.M_Ebx.toValue()},
            {(byte) OP1.L_MODRM.toValue(), 0, (byte) OP3.S_Gw.toValue(),
                    (byte) op5.M_Ewx.toValue()},

            /* 0x1c0 - 0x1cc */
            {(byte) OP1.L_MODRM.toValue(), (byte) Flags.TypeFlag.ADDb.toValue(),
                    (byte) OP3.S_EbGb.toValue(), (byte) op5.M_GbEb.toValue()},
            {(byte) OP1.L_MODRM.toValue(), (byte) Flags.TypeFlag.ADDw.toValue(),
                    (byte) OP3.S_EwGw.toValue(), (byte) op5.M_GwEw.toValue()},
            {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0},
            /* 0x1c8 - 0x1cf */
            {(byte) OP1.L_REGw.toValue(), (byte) OP2.O_BSWAPw.toValue(),
                    (byte) OP3.S_REGw.toValue(), (byte) Register.AX},
            {(byte) OP1.L_REGw.toValue(), (byte) OP2.O_BSWAPw.toValue(),
                    (byte) OP3.S_REGw.toValue(), (byte) Register.CX},
            {(byte) OP1.L_REGw.toValue(), (byte) OP2.O_BSWAPw.toValue(),
                    (byte) OP3.S_REGw.toValue(), (byte) Register.DX},
            {(byte) OP1.L_REGw.toValue(), (byte) OP2.O_BSWAPw.toValue(),
                    (byte) OP3.S_REGw.toValue(), (byte) Register.BX},
            {(byte) OP1.L_REGw.toValue(), (byte) OP2.O_BSWAPw.toValue(),
                    (byte) OP3.S_REGw.toValue(), (byte) Register.SP},
            {(byte) OP1.L_REGw.toValue(), (byte) OP2.O_BSWAPw.toValue(),
                    (byte) OP3.S_REGw.toValue(), (byte) Register.BP},
            {(byte) OP1.L_REGw.toValue(), (byte) OP2.O_BSWAPw.toValue(),
                    (byte) OP3.S_REGw.toValue(), (byte) Register.SI},
            {(byte) OP1.L_REGw.toValue(), (byte) OP2.O_BSWAPw.toValue(),
                    (byte) OP3.S_REGw.toValue(), (byte) Register.DI},

            /* 0x1d0 - 0x1d7 */
            {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0},
            {0, 0, 0, 0}, {0, 0, 0, 0},
            /* 0x1d8 - 0x1df */
            {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0},
            {0, 0, 0, 0}, {0, 0, 0, 0},

            /* 0x1e0 - 0x1ee */
            {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0},
            {0, 0, 0, 0}, {0, 0, 0, 0},
            /* 0x1e8 - 0x1ef */
            {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0},
            {0, 0, 0, 0}, {0, 0, 0, 0},

            /* 0x1f0 - 0x1fc */
            {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0},
            {0, 0, 0, 0}, {0, 0, 0, 0},
            /* 0x1f8 - 0x1ff */
            {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0},
            {0, 0, 0, 0}, {0, 0, 0, 0},


            /* 0x200 - 0x207 */
            {(byte) OP1.L_MODRM.toValue(), (byte) Flags.TypeFlag.ADDb.toValue(),
                    (byte) OP3.S_Eb.toValue(), (byte) op5.M_EbGb.toValue()},
            {(byte) OP1.L_MODRM.toValue(), (byte) Flags.TypeFlag.ADDd.toValue(),
                    (byte) OP3.S_Ed.toValue(), (byte) op5.M_EdGd.toValue()},
            {(byte) OP1.L_MODRM.toValue(), (byte) Flags.TypeFlag.ADDb.toValue(),
                    (byte) OP3.S_Gb.toValue(), (byte) op5.M_GbEb.toValue()},
            {(byte) OP1.L_MODRM.toValue(), (byte) Flags.TypeFlag.ADDd.toValue(),
                    (byte) OP3.S_Gd.toValue(), (byte) op5.M_GdEd.toValue()},
            {(byte) OP1.L_REGbIb.toValue(), (byte) Flags.TypeFlag.ADDb.toValue(),
                    (byte) OP3.S_REGb.toValue(), (byte) Register.AL},
            {(byte) OP1.L_REGdId.toValue(), (byte) Flags.TypeFlag.ADDd.toValue(),
                    (byte) OP3.S_REGd.toValue(), (byte) Register.AX},
            {(byte) OP1.L_SEG.toValue(), 0, (byte) OP3.S_PUSHd.toValue(),
                    (byte) Register.SEG_NAME_ES},
            {(byte) OP1.D_POPSEGd.toValue(), 0, 0, (byte) Register.SEG_NAME_ES},
            /* 0x208 - 0x20f */
            {(byte) OP1.L_MODRM.toValue(), (byte) Flags.TypeFlag.ORb.toValue(),
                    (byte) OP3.S_Eb.toValue(), (byte) op5.M_EbGb.toValue()},
            {(byte) OP1.L_MODRM.toValue(), (byte) Flags.TypeFlag.ORd.toValue(),
                    (byte) OP3.S_Ed.toValue(), (byte) op5.M_EdGd.toValue()},
            {(byte) OP1.L_MODRM.toValue(), (byte) Flags.TypeFlag.ORb.toValue(),
                    (byte) OP3.S_Gb.toValue(), (byte) op5.M_GbEb.toValue()},
            {(byte) OP1.L_MODRM.toValue(), (byte) Flags.TypeFlag.ORd.toValue(),
                    (byte) OP3.S_Gd.toValue(), (byte) op5.M_GdEd.toValue()},
            {(byte) OP1.L_REGbIb.toValue(), (byte) Flags.TypeFlag.ORb.toValue(),
                    (byte) OP3.S_REGb.toValue(), (byte) Register.AL},
            {(byte) OP1.L_REGdId.toValue(), (byte) Flags.TypeFlag.ORd.toValue(),
                    (byte) OP3.S_REGd.toValue(), (byte) Register.AX},
            {(byte) OP1.L_SEG.toValue(), 0, (byte) OP3.S_PUSHd.toValue(),
                    (byte) Register.SEG_NAME_CS},
            {(byte) OP1.L_DOUBLE.toValue(), 0, 0, 0},

            /* 0x210 - 0x217 */
            {(byte) OP1.L_MODRM.toValue(), (byte) Flags.TypeFlag.ADCb.toValue(),
                    (byte) OP3.S_Eb.toValue(), (byte) op5.M_EbGb.toValue()},
            {(byte) OP1.L_MODRM.toValue(), (byte) Flags.TypeFlag.ADCd.toValue(),
                    (byte) OP3.S_Ed.toValue(), (byte) op5.M_EdGd.toValue()},
            {(byte) OP1.L_MODRM.toValue(), (byte) Flags.TypeFlag.ADCb.toValue(),
                    (byte) OP3.S_Gb.toValue(), (byte) op5.M_GbEb.toValue()},
            {(byte) OP1.L_MODRM.toValue(), (byte) Flags.TypeFlag.ADCd.toValue(),
                    (byte) OP3.S_Gd.toValue(), (byte) op5.M_GdEd.toValue()},
            {(byte) OP1.L_REGbIb.toValue(), (byte) Flags.TypeFlag.ADCb.toValue(),
                    (byte) OP3.S_REGb.toValue(), (byte) Register.AL},
            {(byte) OP1.L_REGdId.toValue(), (byte) Flags.TypeFlag.ADCd.toValue(),
                    (byte) OP3.S_REGd.toValue(), (byte) Register.AX},
            {(byte) OP1.L_SEG.toValue(), 0, (byte) OP3.S_PUSHd.toValue(),
                    (byte) Register.SEG_NAME_SS},
            {(byte) OP1.D_POPSEGd.toValue(), 0, 0, (byte) Register.SEG_NAME_SS},
            /* 0x218 - 0x21f */
            {(byte) OP1.L_MODRM.toValue(), (byte) Flags.TypeFlag.SBBb.toValue(),
                    (byte) OP3.S_Eb.toValue(), (byte) op5.M_EbGb.toValue()},
            {(byte) OP1.L_MODRM.toValue(), (byte) Flags.TypeFlag.SBBd.toValue(),
                    (byte) OP3.S_Ed.toValue(), (byte) op5.M_EdGd.toValue()},
            {(byte) OP1.L_MODRM.toValue(), (byte) Flags.TypeFlag.SBBb.toValue(),
                    (byte) OP3.S_Gb.toValue(), (byte) op5.M_GbEb.toValue()},
            {(byte) OP1.L_MODRM.toValue(), (byte) Flags.TypeFlag.SBBd.toValue(),
                    (byte) OP3.S_Gd.toValue(), (byte) op5.M_GdEd.toValue()},
            {(byte) OP1.L_REGbIb.toValue(), (byte) Flags.TypeFlag.SBBb.toValue(),
                    (byte) OP3.S_REGb.toValue(), (byte) Register.AL},
            {(byte) OP1.L_REGdId.toValue(), (byte) Flags.TypeFlag.SBBd.toValue(),
                    (byte) OP3.S_REGd.toValue(), (byte) Register.AX},
            {(byte) OP1.L_SEG.toValue(), 0, (byte) OP3.S_PUSHd.toValue(),
                    (byte) Register.SEG_NAME_DS},
            {(byte) OP1.D_POPSEGd.toValue(), 0, 0, (byte) Register.SEG_NAME_DS},

            /* 0x220 - 0x227 */
            {(byte) OP1.L_MODRM.toValue(), (byte) Flags.TypeFlag.ANDb.toValue(),
                    (byte) OP3.S_Eb.toValue(), (byte) op5.M_EbGb.toValue()},
            {(byte) OP1.L_MODRM.toValue(), (byte) Flags.TypeFlag.ANDd.toValue(),
                    (byte) OP3.S_Ed.toValue(), (byte) op5.M_EdGd.toValue()},
            {(byte) OP1.L_MODRM.toValue(), (byte) Flags.TypeFlag.ANDb.toValue(),
                    (byte) OP3.S_Gb.toValue(), (byte) op5.M_GbEb.toValue()},
            {(byte) OP1.L_MODRM.toValue(), (byte) Flags.TypeFlag.ANDd.toValue(),
                    (byte) OP3.S_Gd.toValue(), (byte) op5.M_GdEd.toValue()},
            {(byte) OP1.L_REGbIb.toValue(), (byte) Flags.TypeFlag.ANDb.toValue(),
                    (byte) OP3.S_REGb.toValue(), (byte) Register.AL},
            {(byte) OP1.L_REGdId.toValue(), (byte) Flags.TypeFlag.ANDd.toValue(),
                    (byte) OP3.S_REGd.toValue(), (byte) Register.AX},
            {(byte) OP1.L_PRESEG.toValue(), 0, 0, (byte) Register.SEG_NAME_ES},
            {(byte) OP1.D_DAA.toValue(), 0, 0, 0},
            /* 0x228 - 0x22f */
            {(byte) OP1.L_MODRM.toValue(), (byte) Flags.TypeFlag.SUBb.toValue(),
                    (byte) OP3.S_Eb.toValue(), (byte) op5.M_EbGb.toValue()},
            {(byte) OP1.L_MODRM.toValue(), (byte) Flags.TypeFlag.SUBd.toValue(),
                    (byte) OP3.S_Ed.toValue(), (byte) op5.M_EdGd.toValue()},
            {(byte) OP1.L_MODRM.toValue(), (byte) Flags.TypeFlag.SUBb.toValue(),
                    (byte) OP3.S_Gb.toValue(), (byte) op5.M_GbEb.toValue()},
            {(byte) OP1.L_MODRM.toValue(), (byte) Flags.TypeFlag.SUBd.toValue(),
                    (byte) OP3.S_Gd.toValue(), (byte) op5.M_GdEd.toValue()},
            {(byte) OP1.L_REGbIb.toValue(), (byte) Flags.TypeFlag.SUBb.toValue(),
                    (byte) OP3.S_REGb.toValue(), (byte) Register.AL},
            {(byte) OP1.L_REGdId.toValue(), (byte) Flags.TypeFlag.SUBd.toValue(),
                    (byte) OP3.S_REGd.toValue(), (byte) Register.AX},
            {(byte) OP1.L_PRESEG.toValue(), 0, 0, (byte) Register.SEG_NAME_CS},
            {(byte) OP1.D_DAS.toValue(), 0, 0, 0},

            /* 0x230 - 0x237 */
            {(byte) OP1.L_MODRM.toValue(), (byte) Flags.TypeFlag.XORb.toValue(),
                    (byte) OP3.S_Eb.toValue(), (byte) op5.M_EbGb.toValue()},
            {(byte) OP1.L_MODRM.toValue(), (byte) Flags.TypeFlag.XORd.toValue(),
                    (byte) OP3.S_Ed.toValue(), (byte) op5.M_EdGd.toValue()},
            {(byte) OP1.L_MODRM.toValue(), (byte) Flags.TypeFlag.XORb.toValue(),
                    (byte) OP3.S_Gb.toValue(), (byte) op5.M_GbEb.toValue()},
            {(byte) OP1.L_MODRM.toValue(), (byte) Flags.TypeFlag.XORd.toValue(),
                    (byte) OP3.S_Gd.toValue(), (byte) op5.M_GdEd.toValue()},
            {(byte) OP1.L_REGbIb.toValue(), (byte) Flags.TypeFlag.XORb.toValue(),
                    (byte) OP3.S_REGb.toValue(), (byte) Register.AL},
            {(byte) OP1.L_REGdId.toValue(), (byte) Flags.TypeFlag.XORd.toValue(),
                    (byte) OP3.S_REGd.toValue(), (byte) Register.AX},
            {(byte) OP1.L_PRESEG.toValue(), 0, 0, (byte) Register.SEG_NAME_SS},
            {(byte) OP1.D_AAA.toValue(), 0, 0, 0},
            /* 0x238 - 0x23f */
            {(byte) OP1.L_MODRM.toValue(), (byte) Flags.TypeFlag.CMPb.toValue(), 0,
                    (byte) op5.M_EbGb.toValue()},
            {(byte) OP1.L_MODRM.toValue(), (byte) Flags.TypeFlag.CMPd.toValue(), 0,
                    (byte) op5.M_EdGd.toValue()},
            {(byte) OP1.L_MODRM.toValue(), (byte) Flags.TypeFlag.CMPb.toValue(), 0,
                    (byte) op5.M_GbEb.toValue()},
            {(byte) OP1.L_MODRM.toValue(), (byte) Flags.TypeFlag.CMPd.toValue(), 0,
                    (byte) op5.M_GdEd.toValue()},
            {(byte) OP1.L_REGbIb.toValue(), (byte) Flags.TypeFlag.CMPb.toValue(), 0,
                    (byte) Register.AL},
            {(byte) OP1.L_REGdId.toValue(), (byte) Flags.TypeFlag.CMPd.toValue(), 0,
                    (byte) Register.AX},
            {(byte) OP1.L_PRESEG.toValue(), 0, 0, (byte) Register.SEG_NAME_DS},
            {(byte) OP1.D_AAS.toValue(), 0, 0, 0},

            /* 0x240 - 0x247 */
            {(byte) OP1.L_REGd.toValue(), (byte) Flags.TypeFlag.INCd.toValue(),
                    (byte) OP3.S_REGd.toValue(), (byte) Register.AX},
            {(byte) OP1.L_REGd.toValue(), (byte) Flags.TypeFlag.INCd.toValue(),
                    (byte) OP3.S_REGd.toValue(), (byte) Register.CX},
            {(byte) OP1.L_REGd.toValue(), (byte) Flags.TypeFlag.INCd.toValue(),
                    (byte) OP3.S_REGd.toValue(), (byte) Register.DX},
            {(byte) OP1.L_REGd.toValue(), (byte) Flags.TypeFlag.INCd.toValue(),
                    (byte) OP3.S_REGd.toValue(), (byte) Register.BX},
            {(byte) OP1.L_REGd.toValue(), (byte) Flags.TypeFlag.INCd.toValue(),
                    (byte) OP3.S_REGd.toValue(), (byte) Register.SP},
            {(byte) OP1.L_REGd.toValue(), (byte) Flags.TypeFlag.INCd.toValue(),
                    (byte) OP3.S_REGd.toValue(), (byte) Register.BP},
            {(byte) OP1.L_REGd.toValue(), (byte) Flags.TypeFlag.INCd.toValue(),
                    (byte) OP3.S_REGd.toValue(), (byte) Register.SI},
            {(byte) OP1.L_REGd.toValue(), (byte) Flags.TypeFlag.INCd.toValue(),
                    (byte) OP3.S_REGd.toValue(), (byte) Register.DI},
            /* 0x248 - 0x24f */
            {(byte) OP1.L_REGd.toValue(), (byte) Flags.TypeFlag.DECd.toValue(),
                    (byte) OP3.S_REGd.toValue(), (byte) Register.AX},
            {(byte) OP1.L_REGd.toValue(), (byte) Flags.TypeFlag.DECd.toValue(),
                    (byte) OP3.S_REGd.toValue(), (byte) Register.CX},
            {(byte) OP1.L_REGd.toValue(), (byte) Flags.TypeFlag.DECd.toValue(),
                    (byte) OP3.S_REGd.toValue(), (byte) Register.DX},
            {(byte) OP1.L_REGd.toValue(), (byte) Flags.TypeFlag.DECd.toValue(),
                    (byte) OP3.S_REGd.toValue(), (byte) Register.BX},
            {(byte) OP1.L_REGd.toValue(), (byte) Flags.TypeFlag.DECd.toValue(),
                    (byte) OP3.S_REGd.toValue(), (byte) Register.SP},
            {(byte) OP1.L_REGd.toValue(), (byte) Flags.TypeFlag.DECd.toValue(),
                    (byte) OP3.S_REGd.toValue(), (byte) Register.BP},
            {(byte) OP1.L_REGd.toValue(), (byte) Flags.TypeFlag.DECd.toValue(),
                    (byte) OP3.S_REGd.toValue(), (byte) Register.SI},
            {(byte) OP1.L_REGd.toValue(), (byte) Flags.TypeFlag.DECd.toValue(),
                    (byte) OP3.S_REGd.toValue(), (byte) Register.DI},

            /* 0x250 - 0x257 */
            {(byte) OP1.L_REGd.toValue(), 0, (byte) OP3.S_PUSHd.toValue(), (byte) Register.AX},
            {(byte) OP1.L_REGd.toValue(), 0, (byte) OP3.S_PUSHd.toValue(), (byte) Register.CX},
            {(byte) OP1.L_REGd.toValue(), 0, (byte) OP3.S_PUSHd.toValue(), (byte) Register.DX},
            {(byte) OP1.L_REGd.toValue(), 0, (byte) OP3.S_PUSHd.toValue(), (byte) Register.BX},
            {(byte) OP1.L_REGd.toValue(), 0, (byte) OP3.S_PUSHd.toValue(), (byte) Register.SP},
            {(byte) OP1.L_REGd.toValue(), 0, (byte) OP3.S_PUSHd.toValue(), (byte) Register.BP},
            {(byte) OP1.L_REGd.toValue(), 0, (byte) OP3.S_PUSHd.toValue(), (byte) Register.SI},
            {(byte) OP1.L_REGd.toValue(), 0, (byte) OP3.S_PUSHd.toValue(), (byte) Register.DI},
            /* 0x258 - 0x25f */
            {(byte) OP1.L_POPd.toValue(), 0, (byte) OP3.S_REGd.toValue(), (byte) Register.AX},
            {(byte) OP1.L_POPd.toValue(), 0, (byte) OP3.S_REGd.toValue(), (byte) Register.CX},
            {(byte) OP1.L_POPd.toValue(), 0, (byte) OP3.S_REGd.toValue(), (byte) Register.DX},
            {(byte) OP1.L_POPd.toValue(), 0, (byte) OP3.S_REGd.toValue(), (byte) Register.BX},
            {(byte) OP1.L_POPd.toValue(), 0, (byte) OP3.S_REGd.toValue(), (byte) Register.SP},
            {(byte) OP1.L_POPd.toValue(), 0, (byte) OP3.S_REGd.toValue(), (byte) Register.BP},
            {(byte) OP1.L_POPd.toValue(), 0, (byte) OP3.S_REGd.toValue(), (byte) Register.SI},
            {(byte) OP1.L_POPd.toValue(), 0, (byte) OP3.S_REGd.toValue(), (byte) Register.DI},

            /* 0x260 - 0x267 */
            {(byte) OP1.D_PUSHAd.toValue(), 0, 0, 0}, {(byte) OP1.D_POPAd.toValue(), 0, 0, 0},
            {(byte) OP1.L_MODRM.toValue(), (byte) OP2.O_BOUNDd.toValue(), 0, 0}, {0, 0, 0, 0},
            {(byte) OP1.L_PRESEG.toValue(), 0, 0, (byte) Register.SEG_NAME_FS},
            {(byte) OP1.L_PRESEG.toValue(), 0, 0, (byte) Register.SEG_NAME_GS},
            {(byte) OP1.L_PREOP.toValue(), 0, 0, 0}, {(byte) OP1.L_PREADD.toValue(), 0, 0, 0},
            /* 0x268 - 0x26f */
            {(byte) OP1.L_Id.toValue(), 0, (byte) OP3.S_PUSHd.toValue(), 0},
            {(byte) OP1.L_MODRM.toValue(), (byte) OP2.O_IMULRd.toValue(), (byte) OP3.S_Gd.toValue(),
                    (byte) op5.M_EdId.toValue()},
            {(byte) OP1.L_Ibx.toValue(), 0, (byte) OP3.S_PUSHd.toValue(), 0},
            {(byte) OP1.L_MODRM.toValue(), (byte) OP2.O_IMULRd.toValue(), (byte) OP3.S_Gd.toValue(),
                    (byte) op5.M_EdIbx.toValue()},
            {(byte) OP1.L_STRING.toValue(), (byte) OP4.R_INSB.toValue(), 0, 0},
            {(byte) OP1.L_STRING.toValue(), (byte) OP4.R_INSD.toValue(), 0, 0},
            {(byte) OP1.L_STRING.toValue(), (byte) OP4.R_OUTSB.toValue(), 0, 0},
            {(byte) OP1.L_STRING.toValue(), (byte) OP4.R_OUTSD.toValue(), 0, 0},

            /* 0x270 - 0x277 */
            {(byte) OP1.L_Ibx.toValue(), (byte) OP2.O_C_O.toValue(), (byte) OP3.S_C_AIPd.toValue(),
                    0},
            {(byte) OP1.L_Ibx.toValue(), (byte) OP2.O_C_NO.toValue(), (byte) OP3.S_C_AIPd.toValue(),
                    0},
            {(byte) OP1.L_Ibx.toValue(), (byte) OP2.O_C_B.toValue(), (byte) OP3.S_C_AIPd.toValue(),
                    0},
            {(byte) OP1.L_Ibx.toValue(), (byte) OP2.O_C_NB.toValue(), (byte) OP3.S_C_AIPd.toValue(),
                    0},
            {(byte) OP1.L_Ibx.toValue(), (byte) OP2.O_C_Z.toValue(), (byte) OP3.S_C_AIPd.toValue(),
                    0},
            {(byte) OP1.L_Ibx.toValue(), (byte) OP2.O_C_NZ.toValue(), (byte) OP3.S_C_AIPd.toValue(),
                    0},
            {(byte) OP1.L_Ibx.toValue(), (byte) OP2.O_C_BE.toValue(), (byte) OP3.S_C_AIPd.toValue(),
                    0},
            {(byte) OP1.L_Ibx.toValue(), (byte) OP2.O_C_NBE.toValue(),
                    (byte) OP3.S_C_AIPd.toValue(), 0},
            /* 0x278 - 0x27f */
            {(byte) OP1.L_Ibx.toValue(), (byte) OP2.O_C_S.toValue(), (byte) OP3.S_C_AIPd.toValue(),
                    0},
            {(byte) OP1.L_Ibx.toValue(), (byte) OP2.O_C_NS.toValue(), (byte) OP3.S_C_AIPd.toValue(),
                    0},
            {(byte) OP1.L_Ibx.toValue(), (byte) OP2.O_C_P.toValue(), (byte) OP3.S_C_AIPd.toValue(),
                    0},
            {(byte) OP1.L_Ibx.toValue(), (byte) OP2.O_C_NP.toValue(), (byte) OP3.S_C_AIPd.toValue(),
                    0},
            {(byte) OP1.L_Ibx.toValue(), (byte) OP2.O_C_L.toValue(), (byte) OP3.S_C_AIPd.toValue(),
                    0},
            {(byte) OP1.L_Ibx.toValue(), (byte) OP2.O_C_NL.toValue(), (byte) OP3.S_C_AIPd.toValue(),
                    0},
            {(byte) OP1.L_Ibx.toValue(), (byte) OP2.O_C_LE.toValue(), (byte) OP3.S_C_AIPd.toValue(),
                    0},
            {(byte) OP1.L_Ibx.toValue(), (byte) OP2.O_C_NLE.toValue(),
                    (byte) OP3.S_C_AIPd.toValue(), 0},

            /* 0x280 - 0x287 */
            {(byte) OP1.L_MODRM.toValue(), 0, 0, (byte) op5.M_GRP.toValue()},
            {(byte) OP1.L_MODRM.toValue(), 2, 0, (byte) op5.M_GRP.toValue()},
            {(byte) OP1.L_MODRM.toValue(), 0, 0, (byte) op5.M_GRP.toValue()},
            {(byte) OP1.L_MODRM.toValue(), 4, 0, (byte) op5.M_GRP.toValue()},
            {(byte) OP1.L_MODRM.toValue(), (byte) Flags.TypeFlag.TESTb.toValue(), 0,
                    (byte) op5.M_EbGb.toValue()},
            {(byte) OP1.L_MODRM.toValue(), (byte) Flags.TypeFlag.TESTd.toValue(), 0,
                    (byte) op5.M_EdGd.toValue()},
            {(byte) OP1.L_MODRM.toValue(), 0, (byte) OP3.S_EbGb.toValue(),
                    (byte) op5.M_GbEb.toValue()},
            {(byte) OP1.L_MODRM.toValue(), 0, (byte) OP3.S_EdGd.toValue(),
                    (byte) op5.M_GdEd.toValue()},
            /* 0x288 - 0x28f */
            {(byte) OP1.L_MODRM.toValue(), 0, (byte) OP3.S_Eb.toValue(), (byte) op5.M_Gb.toValue()},
            {(byte) OP1.L_MODRM.toValue(), 0, (byte) OP3.S_Ed.toValue(), (byte) op5.M_Gd.toValue()},
            {(byte) OP1.L_MODRM.toValue(), 0, (byte) OP3.S_Gb.toValue(), (byte) op5.M_Eb.toValue()},
            {(byte) OP1.L_MODRM.toValue(), 0, (byte) OP3.S_Gd.toValue(), (byte) op5.M_Ed.toValue()},
            {(byte) OP1.L_MODRM.toValue(), 0, (byte) OP3.S_EdMw.toValue(),
                    (byte) op5.M_SEG.toValue()},
            {(byte) OP1.L_MODRM.toValue(), 0, (byte) OP3.S_Gd.toValue(), (byte) op5.M_EA.toValue()},
            {(byte) OP1.L_MODRM.toValue(), 0, (byte) OP3.S_SEGm.toValue(),
                    (byte) op5.M_Ew.toValue()},
            {(byte) OP1.L_POPdRM.toValue(), 0, (byte) OP3.S_Ed.toValue(),
                    (byte) op5.M_None.toValue()},

            /* 0x290 - 0x297 */
            {(byte) OP1.D_NOP.toValue(), 0, 0, 0},
            {(byte) OP1.L_REGd.toValue(), (byte) OP2.O_XCHG_EAX.toValue(),
                    (byte) OP3.S_REGd.toValue(), (byte) Register.CX},
            {(byte) OP1.L_REGd.toValue(), (byte) OP2.O_XCHG_EAX.toValue(),
                    (byte) OP3.S_REGd.toValue(), (byte) Register.DX},
            {(byte) OP1.L_REGd.toValue(), (byte) OP2.O_XCHG_EAX.toValue(),
                    (byte) OP3.S_REGd.toValue(), (byte) Register.BX},
            {(byte) OP1.L_REGd.toValue(), (byte) OP2.O_XCHG_EAX.toValue(),
                    (byte) OP3.S_REGd.toValue(), (byte) Register.SP},
            {(byte) OP1.L_REGd.toValue(), (byte) OP2.O_XCHG_EAX.toValue(),
                    (byte) OP3.S_REGd.toValue(), (byte) Register.BP},
            {(byte) OP1.L_REGd.toValue(), (byte) OP2.O_XCHG_EAX.toValue(),
                    (byte) OP3.S_REGd.toValue(), (byte) Register.SI},
            {(byte) OP1.L_REGd.toValue(), (byte) OP2.O_XCHG_EAX.toValue(),
                    (byte) OP3.S_REGd.toValue(), (byte) Register.DI},
            /* 0x298 - 0x29f */
            {(byte) OP1.D_CWDE.toValue(), 0, 0, 0}, {(byte) OP1.D_CDQ.toValue(), 0, 0, 0},
            {(byte) OP1.L_Ifd.toValue(), (byte) OP2.O_CALLFd.toValue(), 0, 0},
            {(byte) OP1.D_WAIT.toValue(), 0, 0, 0},
            {(byte) OP1.D_PUSHF.toValue(), 0, 0, Convert.toByte(true)},
            {(byte) OP1.D_POPF.toValue(), 0, 0, Convert.toByte(true)},
            {(byte) OP1.D_SAHF.toValue(), 0, 0, 0}, {(byte) OP1.D_LAHF.toValue(), 0, 0, 0},

            /* 0x2a0 - 0x2a7 */
            {(byte) OP1.L_OP.toValue(), (byte) OP2.O_ALOP.toValue(), 0, 0},
            {(byte) OP1.L_OP.toValue(), (byte) OP2.O_EAXOP.toValue(), 0, 0},
            {(byte) OP1.L_OP.toValue(), (byte) OP2.O_OPAL.toValue(), 0, 0},
            {(byte) OP1.L_OP.toValue(), (byte) OP2.O_OPEAX.toValue(), 0, 0},
            {(byte) OP1.L_STRING.toValue(), (byte) OP4.R_MOVSB.toValue(), 0, 0},
            {(byte) OP1.L_STRING.toValue(), (byte) OP4.R_MOVSD.toValue(), 0, 0},
            {(byte) OP1.L_STRING.toValue(), (byte) OP4.R_CMPSB.toValue(), 0, 0},
            {(byte) OP1.L_STRING.toValue(), (byte) OP4.R_CMPSD.toValue(), 0, 0},
            /* 0x2a8 - 0x2af */
            {(byte) OP1.L_REGbIb.toValue(), (byte) Flags.TypeFlag.TESTb.toValue(), 0,
                    (byte) Register.AL},
            {(byte) OP1.L_REGdId.toValue(), (byte) Flags.TypeFlag.TESTd.toValue(), 0,
                    (byte) Register.AX},
            {(byte) OP1.L_STRING.toValue(), (byte) OP4.R_STOSB.toValue(), 0, 0},
            {(byte) OP1.L_STRING.toValue(), (byte) OP4.R_STOSD.toValue(), 0, 0},
            {(byte) OP1.L_STRING.toValue(), (byte) OP4.R_LODSB.toValue(), 0, 0},
            {(byte) OP1.L_STRING.toValue(), (byte) OP4.R_LODSD.toValue(), 0, 0},
            {(byte) OP1.L_STRING.toValue(), (byte) OP4.R_SCASB.toValue(), 0, 0},
            {(byte) OP1.L_STRING.toValue(), (byte) OP4.R_SCASD.toValue(), 0, 0},

            /* 0x2b0 - 0x2b7 */
            {(byte) OP1.L_Ib.toValue(), 0, (byte) OP3.S_REGb.toValue(), (byte) Register.AL},
            {(byte) OP1.L_Ib.toValue(), 0, (byte) OP3.S_REGb.toValue(), (byte) Register.CL},
            {(byte) OP1.L_Ib.toValue(), 0, (byte) OP3.S_REGb.toValue(), (byte) Register.DL},
            {(byte) OP1.L_Ib.toValue(), 0, (byte) OP3.S_REGb.toValue(), (byte) Register.BL},
            {(byte) OP1.L_Ib.toValue(), 0, (byte) OP3.S_REGb.toValue(), (byte) Register.AH},
            {(byte) OP1.L_Ib.toValue(), 0, (byte) OP3.S_REGb.toValue(), (byte) Register.CH},
            {(byte) OP1.L_Ib.toValue(), 0, (byte) OP3.S_REGb.toValue(), (byte) Register.DH},
            {(byte) OP1.L_Ib.toValue(), 0, (byte) OP3.S_REGb.toValue(), (byte) Register.BH},
            /* 0x2b8 - 0x2bf */
            {(byte) OP1.L_Id.toValue(), 0, (byte) OP3.S_REGd.toValue(), (byte) Register.AX},
            {(byte) OP1.L_Id.toValue(), 0, (byte) OP3.S_REGd.toValue(), (byte) Register.CX},
            {(byte) OP1.L_Id.toValue(), 0, (byte) OP3.S_REGd.toValue(), (byte) Register.DX},
            {(byte) OP1.L_Id.toValue(), 0, (byte) OP3.S_REGd.toValue(), (byte) Register.BX},
            {(byte) OP1.L_Id.toValue(), 0, (byte) OP3.S_REGd.toValue(), (byte) Register.SP},
            {(byte) OP1.L_Id.toValue(), 0, (byte) OP3.S_REGd.toValue(), (byte) Register.BP},
            {(byte) OP1.L_Id.toValue(), 0, (byte) OP3.S_REGd.toValue(), (byte) Register.SI},
            {(byte) OP1.L_Id.toValue(), 0, (byte) OP3.S_REGd.toValue(), (byte) Register.DI},

            /* 0x2c0 - 0x2c7 */
            {(byte) OP1.L_MODRM.toValue(), 5, 0, (byte) op5.M_GRP_Ib.toValue()},
            {(byte) OP1.L_MODRM.toValue(), 7, 0, (byte) op5.M_GRP_Ib.toValue()},
            {(byte) OP1.L_POPd.toValue(), 0, (byte) OP3.S_IPIw.toValue(), 0},
            {(byte) OP1.L_POPd.toValue(), 0, (byte) OP3.S_IP.toValue(), 0},
            {(byte) OP1.L_MODRM.toValue(), (byte) OP2.O_SEGES.toValue(),
                    (byte) OP3.S_SEGGd.toValue(), (byte) op5.M_Efd.toValue()},
            {(byte) OP1.L_MODRM.toValue(), (byte) OP2.O_SEGDS.toValue(),
                    (byte) OP3.S_SEGGd.toValue(), (byte) op5.M_Efd.toValue()},
            {(byte) OP1.L_MODRM.toValue(), 0, (byte) OP3.S_Eb.toValue(), (byte) op5.M_Ib.toValue()},
            {(byte) OP1.L_MODRM.toValue(), 0, (byte) OP3.S_Ed.toValue(), (byte) op5.M_Id.toValue()},
            /* 0x2c8 - 0x2cf */
            {(byte) OP1.D_ENTERd.toValue(), 0, 0, 0}, {(byte) OP1.D_LEAVEd.toValue(), 0, 0, 0},
            {(byte) OP1.D_RETFdIw.toValue(), 0, 0, 0}, {(byte) OP1.D_RETFd.toValue(), 0, 0, 0},
            {(byte) OP1.L_VAL.toValue(), (byte) OP2.O_INT.toValue(), 0, 3},
            {(byte) OP1.L_Ib.toValue(), (byte) OP2.O_INT.toValue(), 0, 0},
            {(byte) OP1.L_INTO.toValue(), (byte) OP2.O_INT.toValue(), 0, 0},
            {(byte) OP1.D_IRETd.toValue(), 0, 0, 0},

            /* 0x2d0 - 0x2d7 */
            {(byte) OP1.L_MODRM.toValue(), 5, 0, (byte) op5.M_GRP_1.toValue()},
            {(byte) OP1.L_MODRM.toValue(), 7, 0, (byte) op5.M_GRP_1.toValue()},
            {(byte) OP1.L_MODRM.toValue(), 5, 0, (byte) op5.M_GRP_CL.toValue()},
            {(byte) OP1.L_MODRM.toValue(), 7, 0, (byte) op5.M_GRP_CL.toValue()},
            {(byte) OP1.L_Ib.toValue(), (byte) OP2.O_AAM.toValue(), 0, 0},
            {(byte) OP1.L_Ib.toValue(), (byte) OP2.O_AAD.toValue(), 0, 0},
            {(byte) OP1.D_SETALC.toValue(), 0, 0, 0}, {(byte) OP1.D_XLAT.toValue(), 0, 0, 0},
            /* 0x2d8 - 0x2df */
            {(byte) OP1.L_MODRM.toValue(), (byte) OP2.O_FPU.toValue(), 0, 0},
            {(byte) OP1.L_MODRM.toValue(), (byte) OP2.O_FPU.toValue(), 1, 0},
            {(byte) OP1.L_MODRM.toValue(), (byte) OP2.O_FPU.toValue(), 2, 0},
            {(byte) OP1.L_MODRM.toValue(), (byte) OP2.O_FPU.toValue(), 3, 0},
            {(byte) OP1.L_MODRM.toValue(), (byte) OP2.O_FPU.toValue(), 4, 0},
            {(byte) OP1.L_MODRM.toValue(), (byte) OP2.O_FPU.toValue(), 5, 0},
            {(byte) OP1.L_MODRM.toValue(), (byte) OP2.O_FPU.toValue(), 6, 0},
            {(byte) OP1.L_MODRM.toValue(), (byte) OP2.O_FPU.toValue(), 7, 0},

            /* 0x2e0 - 0x2e7 */
            {(byte) OP1.L_Ibx.toValue(), (byte) OP2.O_LOOPNZ.toValue(), (byte) OP3.S_AIPd.toValue(),
                    0},
            {(byte) OP1.L_Ibx.toValue(), (byte) OP2.O_LOOPZ.toValue(), (byte) OP3.S_AIPd.toValue(),
                    0},
            {(byte) OP1.L_Ibx.toValue(), (byte) OP2.O_LOOP.toValue(), (byte) OP3.S_AIPd.toValue(),
                    0},
            {(byte) OP1.L_Ibx.toValue(), (byte) OP2.O_JCXZ.toValue(), (byte) OP3.S_AIPd.toValue(),
                    0},
            {(byte) OP1.L_Ib.toValue(), (byte) OP2.O_INb.toValue(), 0, 0},
            {(byte) OP1.L_Ib.toValue(), (byte) OP2.O_INd.toValue(), 0, 0},
            {(byte) OP1.L_Ib.toValue(), (byte) OP2.O_OUTb.toValue(), 0, 0},
            {(byte) OP1.L_Ib.toValue(), (byte) OP2.O_OUTd.toValue(), 0, 0},
            /* 0x2e8 - 0x2ef */
            {(byte) OP1.L_Id.toValue(), (byte) OP2.O_CALLNd.toValue(), (byte) OP3.S_AIPd.toValue(),
                    0},
            {(byte) OP1.L_Idx.toValue(), 0, (byte) OP3.S_AIPd.toValue(), 0},
            {(byte) OP1.L_Ifd.toValue(), (byte) OP2.O_JMPFd.toValue(), 0, 0},
            {(byte) OP1.L_Ibx.toValue(), 0, (byte) OP3.S_AIPd.toValue(), 0},
            {(byte) OP1.L_REGw.toValue(), (byte) OP2.O_INb.toValue(), 0, (byte) Register.DX},
            {(byte) OP1.L_REGw.toValue(), (byte) OP2.O_INd.toValue(), 0, (byte) Register.DX},
            {(byte) OP1.L_REGw.toValue(), (byte) OP2.O_OUTb.toValue(), 0, (byte) Register.DX},
            {(byte) OP1.L_REGw.toValue(), (byte) OP2.O_OUTd.toValue(), 0, (byte) Register.DX},

            /* 0x2f0 - 0x2f7 */
            {(byte) OP1.D_LOCK.toValue(), 0, 0, 0}, {(byte) OP1.D_ICEBP.toValue(), 0, 0, 0},
            {(byte) OP1.L_PREREPNE.toValue(), 0, 0, 0}, {(byte) OP1.L_PREREP.toValue(), 0, 0, 0},
            {(byte) OP1.D_HLT.toValue(), 0, 0, 0}, {(byte) OP1.D_CMC.toValue(), 0, 0, 0},
            {(byte) OP1.L_MODRM.toValue(), 8, 0, (byte) op5.M_GRP.toValue()},
            {(byte) OP1.L_MODRM.toValue(), 0xa, 0, (byte) op5.M_GRP.toValue()},
            /* 0x2f8 - 0x2ff */
            {(byte) OP1.D_CLC.toValue(), 0, 0, 0}, {(byte) OP1.D_STC.toValue(), 0, 0, 0},
            {(byte) OP1.D_CLI.toValue(), 0, 0, 0}, {(byte) OP1.D_STI.toValue(), 0, 0, 0},
            {(byte) OP1.D_CLD.toValue(), 0, 0, 0}, {(byte) OP1.D_STD.toValue(), 0, 0, 0},
            {(byte) OP1.L_MODRM.toValue(), 0xb, 0, (byte) op5.M_GRP.toValue()},
            {(byte) OP1.L_MODRM.toValue(), 0xd, 0, (byte) op5.M_GRP.toValue()},


            /* 0x300 - 0x307 */
            {(byte) OP1.L_MODRM.toValue(), (byte) OP2.O_GRP6d.toValue(), (byte) OP3.S_Ew.toValue(),
                    (byte) op5.M_Ew.toValue()},
            {(byte) OP1.L_MODRM.toValue(), (byte) OP2.O_GRP7d.toValue(), (byte) OP3.S_Ew.toValue(),
                    (byte) op5.M_Ew.toValue()},
            {(byte) OP1.L_MODRM_NVM.toValue(), (byte) OP2.O_LAR.toValue(),
                    (byte) OP3.S_Gd.toValue(), (byte) op5.M_EdGd.toValue()},
            {(byte) OP1.L_MODRM_NVM.toValue(), (byte) OP2.O_LSL.toValue(),
                    (byte) OP3.S_Gd.toValue(), (byte) op5.M_EdGd.toValue()},
            {0, 0, 0, 0}, {0, 0, 0, 0}, {(byte) OP1.D_CLTS.toValue(), 0, 0, 0}, {0, 0, 0, 0},
            /* 0x308 - 0x30f */
            {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0},
            {0, 0, 0, 0}, {0, 0, 0, 0},

            /* 0x310 - 0x317 */
            {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0},
            {0, 0, 0, 0}, {0, 0, 0, 0},
            /* 0x318 - 0x31f */
            {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0},
            {0, 0, 0, 0}, {0, 0, 0, 0},

            /* 0x320 - 0x327 */
            {(byte) OP1.L_MODRM.toValue(), (byte) OP2.O_M_Rd_CRx.toValue(),
                    (byte) OP3.S_Ed.toValue(), 0},
            {(byte) OP1.L_MODRM.toValue(), (byte) OP2.O_M_Rd_DRx.toValue(),
                    (byte) OP3.S_Ed.toValue(), 0},
            {(byte) OP1.L_MODRM.toValue(), (byte) OP2.O_M_CRx_Rd.toValue(), 0,
                    (byte) op5.M_Ed.toValue()},
            {(byte) OP1.L_MODRM.toValue(), (byte) OP2.O_M_DRx_Rd.toValue(), 0,
                    (byte) op5.M_Ed.toValue()},
            {(byte) OP1.L_MODRM.toValue(), (byte) OP2.O_M_Rd_TRx.toValue(),
                    (byte) OP3.S_Ed.toValue(), 0},
            {0, 0, 0, 0},
            {(byte) OP1.L_MODRM.toValue(), (byte) OP2.O_M_TRx_Rd.toValue(), 0,
                    (byte) op5.M_Ed.toValue()},
            {0, 0, 0, 0},

            /* 0x328 - 0x32f */
            {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0},
            {0, 0, 0, 0}, {0, 0, 0, 0},

            /* 0x330 - 0x337 */
            {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0},
            {0, 0, 0, 0}, {0, 0, 0, 0},
            /* 0x338 - 0x33f */
            {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0},
            {0, 0, 0, 0}, {0, 0, 0, 0},

            /* 0x340 - 0x347 */
            {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0},
            {0, 0, 0, 0}, {0, 0, 0, 0},
            /* 0x348 - 0x34f */
            {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0},
            {0, 0, 0, 0}, {0, 0, 0, 0},

            /* 0x350 - 0x357 */
            {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0},
            {0, 0, 0, 0}, {0, 0, 0, 0},
            /* 0x358 - 0x35f */
            {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0},
            {0, 0, 0, 0}, {0, 0, 0, 0},

            /* 0x360 - 0x367 */
            {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0},
            {0, 0, 0, 0}, {0, 0, 0, 0},
            /* 0x368 - 0x36f */
            {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0},
            {0, 0, 0, 0}, {0, 0, 0, 0},


            /* 0x370 - 0x377 */
            {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0},
            {0, 0, 0, 0}, {0, 0, 0, 0},
            /* 0x378 - 0x37f */
            {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0},
            {0, 0, 0, 0}, {0, 0, 0, 0},

            /* 0x380 - 0x387 */
            {(byte) OP1.L_Idx.toValue(), (byte) OP2.O_C_O.toValue(), (byte) OP3.S_C_AIPd.toValue(),
                    0},
            {(byte) OP1.L_Idx.toValue(), (byte) OP2.O_C_NO.toValue(), (byte) OP3.S_C_AIPd.toValue(),
                    0},
            {(byte) OP1.L_Idx.toValue(), (byte) OP2.O_C_B.toValue(), (byte) OP3.S_C_AIPd.toValue(),
                    0},
            {(byte) OP1.L_Idx.toValue(), (byte) OP2.O_C_NB.toValue(), (byte) OP3.S_C_AIPd.toValue(),
                    0},
            {(byte) OP1.L_Idx.toValue(), (byte) OP2.O_C_Z.toValue(), (byte) OP3.S_C_AIPd.toValue(),
                    0},
            {(byte) OP1.L_Idx.toValue(), (byte) OP2.O_C_NZ.toValue(), (byte) OP3.S_C_AIPd.toValue(),
                    0},
            {(byte) OP1.L_Idx.toValue(), (byte) OP2.O_C_BE.toValue(), (byte) OP3.S_C_AIPd.toValue(),
                    0},
            {(byte) OP1.L_Idx.toValue(), (byte) OP2.O_C_NBE.toValue(),
                    (byte) OP3.S_C_AIPd.toValue(), 0},
            /* 0x388 - 0x38f */
            {(byte) OP1.L_Idx.toValue(), (byte) OP2.O_C_S.toValue(), (byte) OP3.S_C_AIPd.toValue(),
                    0},
            {(byte) OP1.L_Idx.toValue(), (byte) OP2.O_C_NS.toValue(), (byte) OP3.S_C_AIPd.toValue(),
                    0},
            {(byte) OP1.L_Idx.toValue(), (byte) OP2.O_C_P.toValue(), (byte) OP3.S_C_AIPd.toValue(),
                    0},
            {(byte) OP1.L_Idx.toValue(), (byte) OP2.O_C_NP.toValue(), (byte) OP3.S_C_AIPd.toValue(),
                    0},
            {(byte) OP1.L_Idx.toValue(), (byte) OP2.O_C_L.toValue(), (byte) OP3.S_C_AIPd.toValue(),
                    0},
            {(byte) OP1.L_Idx.toValue(), (byte) OP2.O_C_NL.toValue(), (byte) OP3.S_C_AIPd.toValue(),
                    0},
            {(byte) OP1.L_Idx.toValue(), (byte) OP2.O_C_LE.toValue(), (byte) OP3.S_C_AIPd.toValue(),
                    0},
            {(byte) OP1.L_Idx.toValue(), (byte) OP2.O_C_NLE.toValue(),
                    (byte) OP3.S_C_AIPd.toValue(), 0},

            /* 0x390 - 0x397 */
            {(byte) OP1.L_MODRM.toValue(), (byte) OP2.O_C_O.toValue(), (byte) OP3.S_C_Eb.toValue(),
                    0},
            {(byte) OP1.L_MODRM.toValue(), (byte) OP2.O_C_NO.toValue(), (byte) OP3.S_C_Eb.toValue(),
                    0},
            {(byte) OP1.L_MODRM.toValue(), (byte) OP2.O_C_B.toValue(), (byte) OP3.S_C_Eb.toValue(),
                    0},
            {(byte) OP1.L_MODRM.toValue(), (byte) OP2.O_C_NB.toValue(), (byte) OP3.S_C_Eb.toValue(),
                    0},
            {(byte) OP1.L_MODRM.toValue(), (byte) OP2.O_C_Z.toValue(), (byte) OP3.S_C_Eb.toValue(),
                    0},
            {(byte) OP1.L_MODRM.toValue(), (byte) OP2.O_C_NZ.toValue(), (byte) OP3.S_C_Eb.toValue(),
                    0},
            {(byte) OP1.L_MODRM.toValue(), (byte) OP2.O_C_BE.toValue(), (byte) OP3.S_C_Eb.toValue(),
                    0},
            {(byte) OP1.L_MODRM.toValue(), (byte) OP2.O_C_NBE.toValue(),
                    (byte) OP3.S_C_Eb.toValue(), 0},
            /* 0x398 - 0x39f */
            {(byte) OP1.L_MODRM.toValue(), (byte) OP2.O_C_S.toValue(), (byte) OP3.S_C_Eb.toValue(),
                    0},
            {(byte) OP1.L_MODRM.toValue(), (byte) OP2.O_C_NS.toValue(), (byte) OP3.S_C_Eb.toValue(),
                    0},
            {(byte) OP1.L_MODRM.toValue(), (byte) OP2.O_C_P.toValue(), (byte) OP3.S_C_Eb.toValue(),
                    0},
            {(byte) OP1.L_MODRM.toValue(), (byte) OP2.O_C_NP.toValue(), (byte) OP3.S_C_Eb.toValue(),
                    0},
            {(byte) OP1.L_MODRM.toValue(), (byte) OP2.O_C_L.toValue(), (byte) OP3.S_C_Eb.toValue(),
                    0},
            {(byte) OP1.L_MODRM.toValue(), (byte) OP2.O_C_NL.toValue(), (byte) OP3.S_C_Eb.toValue(),
                    0},
            {(byte) OP1.L_MODRM.toValue(), (byte) OP2.O_C_LE.toValue(), (byte) OP3.S_C_Eb.toValue(),
                    0},
            {(byte) OP1.L_MODRM.toValue(), (byte) OP2.O_C_NLE.toValue(),
                    (byte) OP3.S_C_Eb.toValue(), 0},

            /* 0x3a0 - 0x3a7 */
            {(byte) OP1.L_SEG.toValue(), 0, (byte) OP3.S_PUSHd.toValue(),
                    (byte) Register.SEG_NAME_FS},
            {(byte) OP1.D_POPSEGd.toValue(), 0, 0, (byte) Register.SEG_NAME_FS},
            {(byte) OP1.D_CPUID.toValue(), 0, 0, 0},
            {(byte) OP1.L_MODRM.toValue(), (byte) OP2.O_BTd.toValue(), (byte) OP3.S_Ed.toValue(),
                    (byte) op5.M_EdGdt.toValue()},
            {(byte) OP1.L_MODRM.toValue(), (byte) OP2.O_DSHLd.toValue(), (byte) OP3.S_Ed.toValue(),
                    (byte) op5.M_EdGdIb.toValue()},
            {(byte) OP1.L_MODRM.toValue(), (byte) OP2.O_DSHLd.toValue(), (byte) OP3.S_Ed.toValue(),
                    (byte) op5.M_EdGdCL.toValue()},
            {0, 0, 0, 0}, {0, 0, 0, 0},
            /* 0x3a8 - 0x3af */
            {(byte) OP1.L_SEG.toValue(), 0, (byte) OP3.S_PUSHd.toValue(),
                    (byte) Register.SEG_NAME_GS},
            {(byte) OP1.D_POPSEGd.toValue(), 0, 0, (byte) Register.SEG_NAME_GS}, {0, 0, 0, 0},
            {(byte) OP1.L_MODRM.toValue(), (byte) OP2.O_BTSd.toValue(), (byte) OP3.S_Ed.toValue(),
                    (byte) op5.M_EdGdt.toValue()},
            {(byte) OP1.L_MODRM.toValue(), (byte) OP2.O_DSHRd.toValue(), (byte) OP3.S_Ed.toValue(),
                    (byte) op5.M_EdGdIb.toValue()},
            {(byte) OP1.L_MODRM.toValue(), (byte) OP2.O_DSHRd.toValue(), (byte) OP3.S_Ed.toValue(),
                    (byte) op5.M_EdGdCL.toValue()},
            {0, 0, 0, 0},
            {(byte) OP1.L_MODRM.toValue(), (byte) OP2.O_IMULRd.toValue(), (byte) OP3.S_Gd.toValue(),
                    (byte) op5.M_EdxGdx.toValue()},

            /* 0x3b0 - 0x3b7 */
            {0, 0, 0, 0},
            {(byte) OP1.L_MODRM.toValue(), (byte) OP2.O_CMPXCHG.toValue(),
                    (byte) OP3.S_Ed.toValue(), (byte) op5.M_Ed.toValue()},
            {(byte) OP1.L_MODRM.toValue(), (byte) OP2.O_SEGSS.toValue(),
                    (byte) OP3.S_SEGGd.toValue(), (byte) op5.M_Efd.toValue()},
            {(byte) OP1.L_MODRM.toValue(), (byte) OP2.O_BTRd.toValue(), (byte) OP3.S_Ed.toValue(),
                    (byte) op5.M_EdGdt.toValue()},
            {(byte) OP1.L_MODRM.toValue(), (byte) OP2.O_SEGFS.toValue(),
                    (byte) OP3.S_SEGGd.toValue(), (byte) op5.M_Efd.toValue()},
            {(byte) OP1.L_MODRM.toValue(), (byte) OP2.O_SEGGS.toValue(),
                    (byte) OP3.S_SEGGd.toValue(), (byte) op5.M_Efd.toValue()},
            {(byte) OP1.L_MODRM.toValue(), 0, (byte) OP3.S_Gd.toValue(), (byte) op5.M_Eb.toValue()},
            {(byte) OP1.L_MODRM.toValue(), 0, (byte) OP3.S_Gd.toValue(), (byte) op5.M_Ew.toValue()},
            /* 0x3b8 - 0x3bf */
            {0, 0, 0, 0}, {0, 0, 0, 0},
            {(byte) OP1.L_MODRM.toValue(), 0xf, 0, (byte) op5.M_GRP.toValue()},
            {(byte) OP1.L_MODRM.toValue(), (byte) OP2.O_BTCd.toValue(), (byte) OP3.S_Ed.toValue(),
                    (byte) op5.M_EdGdt.toValue()},
            {(byte) OP1.L_MODRM.toValue(), (byte) OP2.O_BSFd.toValue(), (byte) OP3.S_Gd.toValue(),
                    (byte) op5.M_Ed.toValue()},
            {(byte) OP1.L_MODRM.toValue(), (byte) OP2.O_BSRd.toValue(), (byte) OP3.S_Gd.toValue(),
                    (byte) op5.M_Ed.toValue()},
            {(byte) OP1.L_MODRM.toValue(), 0, (byte) OP3.S_Gd.toValue(),
                    (byte) op5.M_Ebx.toValue()},
            {(byte) OP1.L_MODRM.toValue(), 0, (byte) OP3.S_Gd.toValue(),
                    (byte) op5.M_Ewx.toValue()},

            /* 0x3c0 - 0x3cc */
            {(byte) OP1.L_MODRM.toValue(), (byte) Flags.TypeFlag.ADDb.toValue(),
                    (byte) OP3.S_EbGb.toValue(), (byte) op5.M_GbEb.toValue()},
            {(byte) OP1.L_MODRM.toValue(), (byte) Flags.TypeFlag.ADDd.toValue(),
                    (byte) OP3.S_EdGd.toValue(), (byte) op5.M_GdEd.toValue()},
            {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0},
            /* 0x3c8 - 0x3cf */
            {(byte) OP1.L_REGd.toValue(), (byte) OP2.O_BSWAPd.toValue(),
                    (byte) OP3.S_REGd.toValue(), (byte) Register.AX},
            {(byte) OP1.L_REGd.toValue(), (byte) OP2.O_BSWAPd.toValue(),
                    (byte) OP3.S_REGd.toValue(), (byte) Register.CX},
            {(byte) OP1.L_REGd.toValue(), (byte) OP2.O_BSWAPd.toValue(),
                    (byte) OP3.S_REGd.toValue(), (byte) Register.DX},
            {(byte) OP1.L_REGd.toValue(), (byte) OP2.O_BSWAPd.toValue(),
                    (byte) OP3.S_REGd.toValue(), (byte) Register.BX},
            {(byte) OP1.L_REGd.toValue(), (byte) OP2.O_BSWAPd.toValue(),
                    (byte) OP3.S_REGd.toValue(), (byte) Register.SP},
            {(byte) OP1.L_REGd.toValue(), (byte) OP2.O_BSWAPd.toValue(),
                    (byte) OP3.S_REGd.toValue(), (byte) Register.BP},
            {(byte) OP1.L_REGd.toValue(), (byte) OP2.O_BSWAPd.toValue(),
                    (byte) OP3.S_REGd.toValue(), (byte) Register.SI},
            {(byte) OP1.L_REGd.toValue(), (byte) OP2.O_BSWAPd.toValue(),
                    (byte) OP3.S_REGd.toValue(), (byte) Register.DI},

            /* 0x3d0 - 0x3d7 */
            {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0},
            {0, 0, 0, 0}, {0, 0, 0, 0},
            /* 0x3d8 - 0x3df */
            {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0},
            {0, 0, 0, 0}, {0, 0, 0, 0},

            /* 0x3e0 - 0x3ee */
            {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0},
            {0, 0, 0, 0}, {0, 0, 0, 0},
            /* 0x3e8 - 0x3ef */
            {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0},
            {0, 0, 0, 0}, {0, 0, 0, 0},

            /* 0x3f0 - 0x3fc */
            {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0},
            {0, 0, 0, 0}, {0, 0, 0, 0},
            /* 0x3f8 - 0x3ff */
            {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0},
            {0, 0, 0, 0}, {0, 0, 0, 0},

    };
    private byte[][][] _tmpGroups = {{ /* 0x00 Group 1 Eb,Ib */
            {0, (byte) Flags.TypeFlag.ADDb.toValue(), (byte) OP3.S_Eb.toValue(),
                    (byte) op5.M_EbIb.toValue()},
            {0, (byte) Flags.TypeFlag.ORb.toValue(), (byte) OP3.S_Eb.toValue(),
                    (byte) op5.M_EbIb.toValue()},
            {0, (byte) Flags.TypeFlag.ADCb.toValue(), (byte) OP3.S_Eb.toValue(),
                    (byte) op5.M_EbIb.toValue()},
            {0, (byte) Flags.TypeFlag.SBBb.toValue(), (byte) OP3.S_Eb.toValue(),
                    (byte) op5.M_EbIb.toValue()},
            {0, (byte) Flags.TypeFlag.ANDb.toValue(), (byte) OP3.S_Eb.toValue(),
                    (byte) op5.M_EbIb.toValue()},
            {0, (byte) Flags.TypeFlag.SUBb.toValue(), (byte) OP3.S_Eb.toValue(),
                    (byte) op5.M_EbIb.toValue()},
            {0, (byte) Flags.TypeFlag.XORb.toValue(), (byte) OP3.S_Eb.toValue(),
                    (byte) op5.M_EbIb.toValue()},
            {0, (byte) Flags.TypeFlag.CMPb.toValue(), 0, (byte) op5.M_EbIb.toValue()},},
            { /* 0x01 Group 1 Ew,Iw */
                    {0, (byte) Flags.TypeFlag.ADDw.toValue(), (byte) OP3.S_Ew.toValue(),
                            (byte) op5.M_EwIw.toValue()},
                    {0, (byte) Flags.TypeFlag.ORw.toValue(), (byte) OP3.S_Ew.toValue(),
                            (byte) op5.M_EwIw.toValue()},
                    {0, (byte) Flags.TypeFlag.ADCw.toValue(), (byte) OP3.S_Ew.toValue(),
                            (byte) op5.M_EwIw.toValue()},
                    {0, (byte) Flags.TypeFlag.SBBw.toValue(), (byte) OP3.S_Ew.toValue(),
                            (byte) op5.M_EwIw.toValue()},
                    {0, (byte) Flags.TypeFlag.ANDw.toValue(), (byte) OP3.S_Ew.toValue(),
                            (byte) op5.M_EwIw.toValue()},
                    {0, (byte) Flags.TypeFlag.SUBw.toValue(), (byte) OP3.S_Ew.toValue(),
                            (byte) op5.M_EwIw.toValue()},
                    {0, (byte) Flags.TypeFlag.XORw.toValue(), (byte) OP3.S_Ew.toValue(),
                            (byte) op5.M_EwIw.toValue()},
                    {0, (byte) Flags.TypeFlag.CMPw.toValue(), 0, (byte) op5.M_EwIw.toValue()},},
            { /* 0x02 Group 1 Ed,Id */
                    {0, (byte) Flags.TypeFlag.ADDd.toValue(), (byte) OP3.S_Ed.toValue(),
                            (byte) op5.M_EdId.toValue()},
                    {0, (byte) Flags.TypeFlag.ORd.toValue(), (byte) OP3.S_Ed.toValue(),
                            (byte) op5.M_EdId.toValue()},
                    {0, (byte) Flags.TypeFlag.ADCd.toValue(), (byte) OP3.S_Ed.toValue(),
                            (byte) op5.M_EdId.toValue()},
                    {0, (byte) Flags.TypeFlag.SBBd.toValue(), (byte) OP3.S_Ed.toValue(),
                            (byte) op5.M_EdId.toValue()},
                    {0, (byte) Flags.TypeFlag.ANDd.toValue(), (byte) OP3.S_Ed.toValue(),
                            (byte) op5.M_EdId.toValue()},
                    {0, (byte) Flags.TypeFlag.SUBd.toValue(), (byte) OP3.S_Ed.toValue(),
                            (byte) op5.M_EdId.toValue()},
                    {0, (byte) Flags.TypeFlag.XORd.toValue(), (byte) OP3.S_Ed.toValue(),
                            (byte) op5.M_EdId.toValue()},
                    {0, (byte) Flags.TypeFlag.CMPd.toValue(), 0, (byte) op5.M_EdId.toValue()},},
            { /* 0x03 Group 1 Ew,Ibx */
                    {0, (byte) Flags.TypeFlag.ADDw.toValue(), (byte) OP3.S_Ew.toValue(),
                            (byte) op5.M_EwIbx.toValue()},
                    {0, (byte) Flags.TypeFlag.ORw.toValue(), (byte) OP3.S_Ew.toValue(),
                            (byte) op5.M_EwIbx.toValue()},
                    {0, (byte) Flags.TypeFlag.ADCw.toValue(), (byte) OP3.S_Ew.toValue(),
                            (byte) op5.M_EwIbx.toValue()},
                    {0, (byte) Flags.TypeFlag.SBBw.toValue(), (byte) OP3.S_Ew.toValue(),
                            (byte) op5.M_EwIbx.toValue()},
                    {0, (byte) Flags.TypeFlag.ANDw.toValue(), (byte) OP3.S_Ew.toValue(),
                            (byte) op5.M_EwIbx.toValue()},
                    {0, (byte) Flags.TypeFlag.SUBw.toValue(), (byte) OP3.S_Ew.toValue(),
                            (byte) op5.M_EwIbx.toValue()},
                    {0, (byte) Flags.TypeFlag.XORw.toValue(), (byte) OP3.S_Ew.toValue(),
                            (byte) op5.M_EwIbx.toValue()},
                    {0, (byte) Flags.TypeFlag.CMPw.toValue(), 0, (byte) op5.M_EwIbx.toValue()},},
            { /* 0x04 Group 1 Ed,Ibx */
                    {0, (byte) Flags.TypeFlag.ADDd.toValue(), (byte) OP3.S_Ed.toValue(),
                            (byte) op5.M_EdIbx.toValue()},
                    {0, (byte) Flags.TypeFlag.ORd.toValue(), (byte) OP3.S_Ed.toValue(),
                            (byte) op5.M_EdIbx.toValue()},
                    {0, (byte) Flags.TypeFlag.ADCd.toValue(), (byte) OP3.S_Ed.toValue(),
                            (byte) op5.M_EdIbx.toValue()},
                    {0, (byte) Flags.TypeFlag.SBBd.toValue(), (byte) OP3.S_Ed.toValue(),
                            (byte) op5.M_EdIbx.toValue()},
                    {0, (byte) Flags.TypeFlag.ANDd.toValue(), (byte) OP3.S_Ed.toValue(),
                            (byte) op5.M_EdIbx.toValue()},
                    {0, (byte) Flags.TypeFlag.SUBd.toValue(), (byte) OP3.S_Ed.toValue(),
                            (byte) op5.M_EdIbx.toValue()},
                    {0, (byte) Flags.TypeFlag.XORd.toValue(), (byte) OP3.S_Ed.toValue(),
                            (byte) op5.M_EdIbx.toValue()},
                    {0, (byte) Flags.TypeFlag.CMPd.toValue(), 0, (byte) op5.M_EdIbx.toValue()},

            }, { /* 0x05 Group 2 Eb,XXX */
                    {0, (byte) Flags.TypeFlag.ROLb.toValue(), (byte) OP3.S_Eb.toValue(),
                            (byte) op5.M_Eb.toValue()},
                    {0, (byte) Flags.TypeFlag.RORb.toValue(), (byte) OP3.S_Eb.toValue(),
                            (byte) op5.M_Eb.toValue()},
                    {0, (byte) Flags.TypeFlag.RCLb.toValue(), (byte) OP3.S_Eb.toValue(),
                            (byte) op5.M_Eb.toValue()},
                    {0, (byte) Flags.TypeFlag.RCRb.toValue(), (byte) OP3.S_Eb.toValue(),
                            (byte) op5.M_Eb.toValue()},
                    {0, (byte) Flags.TypeFlag.SHLb.toValue(), (byte) OP3.S_Eb.toValue(),
                            (byte) op5.M_Eb.toValue()},
                    {0, (byte) Flags.TypeFlag.SHRb.toValue(), (byte) OP3.S_Eb.toValue(),
                            (byte) op5.M_Eb.toValue()},
                    {0, (byte) Flags.TypeFlag.SHLb.toValue(), (byte) OP3.S_Eb.toValue(),
                            (byte) op5.M_Eb.toValue()},
                    {0, (byte) Flags.TypeFlag.SARb.toValue(), (byte) OP3.S_Eb.toValue(),
                            (byte) op5.M_Eb.toValue()},},
            { /* 0x06 Group 2 Ew,XXX */
                    {0, (byte) Flags.TypeFlag.ROLw.toValue(), (byte) OP3.S_Ew.toValue(),
                            (byte) op5.M_Ew.toValue()},
                    {0, (byte) Flags.TypeFlag.RORw.toValue(), (byte) OP3.S_Ew.toValue(),
                            (byte) op5.M_Ew.toValue()},
                    {0, (byte) Flags.TypeFlag.RCLw.toValue(), (byte) OP3.S_Ew.toValue(),
                            (byte) op5.M_Ew.toValue()},
                    {0, (byte) Flags.TypeFlag.RCRw.toValue(), (byte) OP3.S_Ew.toValue(),
                            (byte) op5.M_Ew.toValue()},
                    {0, (byte) Flags.TypeFlag.SHLw.toValue(), (byte) OP3.S_Ew.toValue(),
                            (byte) op5.M_Ew.toValue()},
                    {0, (byte) Flags.TypeFlag.SHRw.toValue(), (byte) OP3.S_Ew.toValue(),
                            (byte) op5.M_Ew.toValue()},
                    {0, (byte) Flags.TypeFlag.SHLw.toValue(), (byte) OP3.S_Ew.toValue(),
                            (byte) op5.M_Ew.toValue()},
                    {0, (byte) Flags.TypeFlag.SARw.toValue(), (byte) OP3.S_Ew.toValue(),
                            (byte) op5.M_Ew.toValue()},},
            { /* 0x07 Group 2 Ed,XXX */
                    {0, (byte) Flags.TypeFlag.ROLd.toValue(), (byte) OP3.S_Ed.toValue(),
                            (byte) op5.M_Ed.toValue()},
                    {0, (byte) Flags.TypeFlag.RORd.toValue(), (byte) OP3.S_Ed.toValue(),
                            (byte) op5.M_Ed.toValue()},
                    {0, (byte) Flags.TypeFlag.RCLd.toValue(), (byte) OP3.S_Ed.toValue(),
                            (byte) op5.M_Ed.toValue()},
                    {0, (byte) Flags.TypeFlag.RCRd.toValue(), (byte) OP3.S_Ed.toValue(),
                            (byte) op5.M_Ed.toValue()},
                    {0, (byte) Flags.TypeFlag.SHLd.toValue(), (byte) OP3.S_Ed.toValue(),
                            (byte) op5.M_Ed.toValue()},
                    {0, (byte) Flags.TypeFlag.SHRd.toValue(), (byte) OP3.S_Ed.toValue(),
                            (byte) op5.M_Ed.toValue()},
                    {0, (byte) Flags.TypeFlag.SHLd.toValue(), (byte) OP3.S_Ed.toValue(),
                            (byte) op5.M_Ed.toValue()},
                    {0, (byte) Flags.TypeFlag.SARd.toValue(), (byte) OP3.S_Ed.toValue(),
                            (byte) op5.M_Ed.toValue()},


            }, { /* 0x08 Group 3 Eb */
                    {0, (byte) Flags.TypeFlag.TESTb.toValue(), 0, (byte) op5.M_EbIb.toValue()},
                    {0, (byte) Flags.TypeFlag.TESTb.toValue(), 0, (byte) op5.M_EbIb.toValue()},
                    {0, (byte) OP2.O_NOT.toValue(), (byte) OP3.S_Eb.toValue(),
                            (byte) op5.M_Eb.toValue()},
                    {0, (byte) Flags.TypeFlag.NEGb.toValue(), (byte) OP3.S_Eb.toValue(),
                            (byte) op5.M_Eb.toValue()},
                    {0, (byte) OP2.O_MULb.toValue(), 0, (byte) op5.M_Eb.toValue()},
                    {0, (byte) OP2.O_IMULb.toValue(), 0, (byte) op5.M_Eb.toValue()},
                    {0, (byte) OP2.O_DIVb.toValue(), 0, (byte) op5.M_Eb.toValue()},
                    {0, (byte) OP2.O_IDIVb.toValue(), 0, (byte) op5.M_Eb.toValue()},},
            { /* 0x09 Group 3 Ew */
                    {0, (byte) Flags.TypeFlag.TESTw.toValue(), 0, (byte) op5.M_EwIw.toValue()},
                    {0, (byte) Flags.TypeFlag.TESTw.toValue(), 0, (byte) op5.M_EwIw.toValue()},
                    {0, (byte) OP2.O_NOT.toValue(), (byte) OP3.S_Ew.toValue(),
                            (byte) op5.M_Ew.toValue()},
                    {0, (byte) Flags.TypeFlag.NEGw.toValue(), (byte) OP3.S_Ew.toValue(),
                            (byte) op5.M_Ew.toValue()},
                    {0, (byte) OP2.O_MULw.toValue(), 0, (byte) op5.M_Ew.toValue()},
                    {0, (byte) OP2.O_IMULw.toValue(), 0, (byte) op5.M_Ew.toValue()},
                    {0, (byte) OP2.O_DIVw.toValue(), 0, (byte) op5.M_Ew.toValue()},
                    {0, (byte) OP2.O_IDIVw.toValue(), 0, (byte) op5.M_Ew.toValue()},},
            { /* 0x0a Group 3 Ed */
                    {0, (byte) Flags.TypeFlag.TESTd.toValue(), 0, (byte) op5.M_EdId.toValue()},
                    {0, (byte) Flags.TypeFlag.TESTd.toValue(), 0, (byte) op5.M_EdId.toValue()},
                    {0, (byte) OP2.O_NOT.toValue(), (byte) OP3.S_Ed.toValue(),
                            (byte) op5.M_Ed.toValue()},
                    {0, (byte) Flags.TypeFlag.NEGd.toValue(), (byte) OP3.S_Ed.toValue(),
                            (byte) op5.M_Ed.toValue()},
                    {0, (byte) OP2.O_MULd.toValue(), 0, (byte) op5.M_Ed.toValue()},
                    {0, (byte) OP2.O_IMULd.toValue(), 0, (byte) op5.M_Ed.toValue()},
                    {0, (byte) OP2.O_DIVd.toValue(), 0, (byte) op5.M_Ed.toValue()},
                    {0, (byte) OP2.O_IDIVd.toValue(), 0, (byte) op5.M_Ed.toValue()},

            }, { /* 0x0b Group 4 Eb */
                    {0, (byte) Flags.TypeFlag.INCb.toValue(), (byte) OP3.S_Eb.toValue(),
                            (byte) op5.M_Eb.toValue()},
                    {0, (byte) Flags.TypeFlag.DECb.toValue(), (byte) OP3.S_Eb.toValue(),
                            (byte) op5.M_Eb.toValue()},
                    {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0},
                    {0, (byte) OP2.O_CBACK.toValue(), 0, (byte) op5.M_Iw.toValue()},},
            { /* 0x0c Group 5 Ew */
                    {0, (byte) Flags.TypeFlag.INCw.toValue(), (byte) OP3.S_Ew.toValue(),
                            (byte) op5.M_Ew.toValue()},
                    {0, (byte) Flags.TypeFlag.DECw.toValue(), (byte) OP3.S_Ew.toValue(),
                            (byte) op5.M_Ew.toValue()},
                    {0, (byte) OP2.O_CALLNw.toValue(), (byte) OP3.S_IP.toValue(),
                            (byte) op5.M_Ew.toValue()},
                    {0, (byte) OP2.O_CALLFw.toValue(), 0, (byte) op5.M_Efw.toValue()},
                    {0, 0, (byte) OP3.S_IP.toValue(), (byte) op5.M_Ew.toValue()},
                    {0, (byte) OP2.O_JMPFw.toValue(), 0, (byte) op5.M_Efw.toValue()},
                    {0, 0, (byte) OP3.S_PUSHw.toValue(), (byte) op5.M_Ew.toValue()}, {0, 0, 0, 0},},
            { /* 0x0d Group 5 Ed */
                    {0, (byte) Flags.TypeFlag.INCd.toValue(), (byte) OP3.S_Ed.toValue(),
                            (byte) op5.M_Ed.toValue()},
                    {0, (byte) Flags.TypeFlag.DECd.toValue(), (byte) OP3.S_Ed.toValue(),
                            (byte) op5.M_Ed.toValue()},
                    {0, (byte) OP2.O_CALLNd.toValue(), (byte) OP3.S_IP.toValue(),
                            (byte) op5.M_Ed.toValue()},
                    {0, (byte) OP2.O_CALLFd.toValue(), 0, (byte) op5.M_Efd.toValue()},
                    {0, 0, (byte) OP3.S_IP.toValue(), (byte) op5.M_Ed.toValue()},
                    {0, (byte) OP2.O_JMPFd.toValue(), 0, (byte) op5.M_Efd.toValue()},
                    {0, 0, (byte) OP3.S_PUSHd.toValue(), (byte) op5.M_Ed.toValue()}, {0, 0, 0, 0},


            }, { /* 0x0e Group 8 Ew */
                    {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0},
                    {0, (byte) OP2.O_BTw.toValue(), (byte) OP3.S_Ew.toValue(),
                            (byte) op5.M_EwIb.toValue()},
                    {0, (byte) OP2.O_BTSw.toValue(), (byte) OP3.S_Ew.toValue(),
                            (byte) op5.M_EwIb.toValue()},
                    {0, (byte) OP2.O_BTRw.toValue(), (byte) OP3.S_Ew.toValue(),
                            (byte) op5.M_EwIb.toValue()},
                    {0, (byte) OP2.O_BTCw.toValue(), (byte) OP3.S_Ew.toValue(),
                            (byte) op5.M_EwIb.toValue()},},
            { /* 0x0f Group 8 Ed */
                    {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0},
                    {0, (byte) OP2.O_BTd.toValue(), (byte) OP3.S_Ed.toValue(),
                            (byte) op5.M_EdIb.toValue()},
                    {0, (byte) OP2.O_BTSd.toValue(), (byte) OP3.S_Ed.toValue(),
                            (byte) op5.M_EdIb.toValue()},
                    {0, (byte) OP2.O_BTRd.toValue(), (byte) OP3.S_Ed.toValue(),
                            (byte) op5.M_EdIb.toValue()},
                    {0, (byte) OP2.O_BTCd.toValue(), (byte) OP3.S_Ed.toValue(),
                            (byte) op5.M_EdIb.toValue()},



            }};
    /*--------------------------- end CoreFullOpCodeTable -----------------------------*/


}
