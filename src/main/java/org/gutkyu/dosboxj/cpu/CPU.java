package org.gutkyu.dosboxj.cpu;

import org.gutkyu.dosboxj.hardware.memory.*;
import org.gutkyu.dosboxj.hardware.memory.paging.*;
import org.gutkyu.dosboxj.misc.*;
import org.gutkyu.dosboxj.*;
import org.gutkyu.dosboxj.gui.*;
import org.gutkyu.dosboxj.util.*;
import org.gutkyu.dosboxj.misc.setup.*;
import org.gutkyu.dosboxj.misc.setup.Value.WrongType;

public final class CPU {
    // 디버그와 관련된 함수
    // 더 자세한 내용은 dosbox 소스를 살펴볼 것
    public static void checkCPUCond(boolean cond, String msg, int exc, int sel) {
        if (cond)
            do {
            } while (false);
    }

    private static final CPUDecoder HLTDecoder = new CPUDecoder(CPU::hltDecode);

    // regsModule으로 이동
    // public static regsModule.CPU_Regs cpu_regs;
    public static CPUBlock Block = new CPUBlock();
    // regsModule으로 이동
    // public static regsModule.Segments Segs;

    public static final int AutoDetermineNone = 0x00;
    public static final int AutoDetermineCore = 0x01;
    public static final int AutoDetermineCycles = 0x02;

    public static final int AutoDetermineShift = 0x02;
    public static final int AutoDetermineMask = 0x03;

    public static final int CyclesLowerLimit = 100;

    public static final int ArchTypeMixed = 0xff;
    public static final int ArchType386Slow = 0x30;
    public static final int ArchType386Fast = 0x35;
    public static final int ArchType486OldSlow = 0x40;
    public static final int ArchType486NewSlow = 0x45;
    public static final int ArchTypePentiumSlow = 0x50;

    /* CPU Cycle Timing */
    public static int Cycles = 0;
    public static int CycleLeft = 3000;
    public static int CycleMax = 3000;
    public static int OldCycleMax = 3000;
    public static int CyclePercUsed = 100;
    public static int CycleLimit = -1;
    public static int CycleUp = 0;
    public static int CycleDown = 0;
    public static long IODelayRemoved = 0;
    public static CPUDecoder CpuDecoder;
    public static boolean CycleAutoAdjust = false;
    public static boolean SkipCycleAutoAdjust = false;
    public static int AutoDetermineMode = 0;

    public static int ArchitectureType = ArchTypeMixed;

    public static int FlagIdToggle = 0;

    public static int PrefetchQueueSize = 0;

    public static final int ExceptionUD = 6;
    public static final int ExceptionTS = 10;
    public static final int ExceptionNP = 11;
    public static final int ExceptionSS = 12;
    public static final int ExceptionGP = 13;
    public static final int ExceptionPF = 14;

    public static int CR0_PROTECTION = 0x00000001;
    public static int CR0_MONITORPROCESSOR = 0x00000002;
    public static int CR0_FPUEMULATION = 0x00000004;
    public static int CR0_TASKSWITCH = 0x00000008;
    public static int CR0_FPUPRESENT = 0x00000010;
    public static int CR0_PAGING = 0x80000000;

    // *********************************************************************
    // Descriptor
    // *********************************************************************

    public static int DESC_INVALID = 0x00;
    public static int DESC_286_TSS_A = 0x01;
    public static int DESC_LDT = 0x02;
    public static int DESC_286_TSS_B = 0x03;
    public static int DESC_286_CALL_GATE = 0x04;
    public static int DESC_TASK_GATE = 0x05;
    public static int DESC_286_INT_GATE = 0x06;
    public static int DESC_286_TRAP_GATE = 0x07;

    public static int DESC_386_TSS_A = 0x09;
    public static int DESC_386_TSS_B = 0x0b;
    public static int DESC_386_CALL_GATE = 0x0c;
    public static int DESC_386_INT_GATE = 0x0e;
    public static int DESC_386_TRAP_GATE = 0x0f;

    /* EU/ED Expand UP/DOWN RO/RW Read Only/Read Write NA/A Accessed */
    public static int DESC_DATA_EU_RO_NA = 0x10;
    public static int DESC_DATA_EU_RO_A = 0x11;
    public static int DESC_DATA_EU_RW_NA = 0x12;
    public static int DESC_DATA_EU_RW_A = 0x13;
    public static int DESC_DATA_ED_RO_NA = 0x14;
    public static int DESC_DATA_ED_RO_A = 0x15;
    public static int DESC_DATA_ED_RW_NA = 0x16;
    public static int DESC_DATA_ED_RW_A = 0x17;

    /* N/R Readable NC/C Confirming A/NA Accessed */
    public static int DESC_CODE_N_NC_A = 0x18;
    public static int DESC_CODE_N_NC_NA = 0x19;
    public static int DESC_CODE_R_NC_A = 0x1a;
    public static int DESC_CODE_R_NC_NA = 0x1b;
    public static int DESC_CODE_N_C_A = 0x1c;
    public static int DESC_CODE_N_C_NA = 0x1d;
    public static int DESC_CODE_R_C_A = 0x1e;
    public static int DESC_CODE_R_C_NA = 0x1f;

    public static void push16(int value) {
        int newESP = (Register.getRegESP() & Block.Stack.NotMask)
                | ((Register.getRegESP() - 2) & Block.Stack.Mask);
        Memory.writeW(Register.segPhys(Register.SEG_NAME_SS) + (newESP & Block.Stack.Mask), value);
        Register.setRegESP(newESP);
    }

    public static void push32(int value) {
        int newESP = (Register.getRegESP() & Block.Stack.NotMask)
                | ((Register.getRegESP() - 4) & Block.Stack.Mask);
        Memory.writeD(Register.segPhys(Register.SEG_NAME_SS) + (newESP & Block.Stack.Mask), value);
        Register.setRegESP(newESP);
    }

    public static int pop16() {
        int val = Memory.readW(
                Register.segPhys(Register.SEG_NAME_SS) + (Register.getRegESP() & Block.Stack.Mask));
        Register.setRegESP((Register.getRegESP() & Block.Stack.NotMask)
                | ((Register.getRegESP() + 2) & Block.Stack.Mask));
        return val;
    }

    public static int pop32() {
        int val = Memory.readD(
                Register.segPhys(Register.SEG_NAME_SS) + (Register.getRegESP() & Block.Stack.Mask));
        Register.setRegESP((Register.getRegESP() & Block.Stack.NotMask)
                | ((Register.getRegESP() + 4) & Block.Stack.Mask));
        return val;
    }

    int selBase(int sel) {
        if ((Block.CR0 & CR0_PROTECTION) != 0) {
            Descriptor desc = new Descriptor();
            Block.GDT.getDescriptor(sel, desc);
            return desc.getBase();
        } else {
            return sel << 4;
        }
    }

    public static void setFlags(int word, int mask) {
        mask |= FlagIdToggle; // ID-flag can be toggled on cpuid-supporting CPUs
        Register.Flags = (Register.Flags & ~mask) | (word & mask) | 2;
        Block.Direction = 1 - ((Register.Flags & Register.FlagDF) >>> 9);
    }

    public static void setFlagsD(int word) {
        int mask = Block.CPL != 0 ? Register.FMaskNormal : Register.FMaskAll;
        setFlags(word, mask);
    }

    public static void setFlagsW(int word) {
        int mask = (Block.CPL != 0 ? Register.FMaskNormal : Register.FMaskAll) & 0xffff;
        setFlags(word, mask);
    }

    public static boolean prepareException(int which, int error) {
        Block.Exception.Which = which;
        Block.Exception.Error = error;
        return true;
    }

    public static boolean cli() {
        if (Block.PMode && ((Register.getFlag(Register.FlagVM) == 0
                && (Register.getFlagIOPL() < Block.CPL))
                || (Register.getFlag(Register.FlagVM) != 0 && (Register.getFlagIOPL() < 3)))) {
            return prepareException(ExceptionGP, 0);
        } else {
            Register.setFlagBit(Register.FlagIF, false);
            return false;
        }
    }

    public static boolean sti() {
        if (Block.PMode && ((Register.getFlag(Register.FlagVM) == 0
                && (Register.getFlagIOPL() < Block.CPL))
                || (Register.getFlag(Register.FlagVM) != 0 && (Register.getFlagIOPL() < 3)))) {
            return prepareException(ExceptionGP, 0);
        } else {
            Register.setFlagBit(Register.FlagIF, true);
            return false;
        }
    }

    public static boolean popf(int use32) {
        if (Block.PMode && Register.getFlag(Register.FlagVM) != 0
                && (Register.getFlag(Register.FlagIOPL) != Register.FlagIOPL)) {
            /* Not enough privileges to execute POPF */
            return prepareException(ExceptionGP, 0);
        }
        int mask = Register.FMaskAll;
        /* IOPL field can only be modified when CPL=0 or in real mode: */
        if (Block.PMode && (Block.CPL > 0))
            mask &= (~Register.FlagIOPL);
        if (Block.PMode && Register.getFlag(Register.FlagVM) == 0
                && (Register.getFlagIOPL() < Block.CPL))
            mask &= (~Register.FlagIF);
        if (use32 != 0)
            setFlags(pop32(), mask);
        else
            setFlags(pop16(), mask & 0xffff);
        Flags.destroyConditionFlags();
        return false;
    }

    public static boolean popf(boolean use32) {
        if (Block.PMode && Register.getFlag(Register.FlagVM) != 0
                && (Register.getFlag(Register.FlagIOPL) != Register.FlagIOPL)) {
            /* Not enough privileges to execute POPF */
            return prepareException(ExceptionGP, 0);
        }
        int mask = Register.FMaskAll;
        /* IOPL field can only be modified when CPL=0 or in real mode: */
        if (Block.PMode && (Block.CPL > 0))
            mask &= (~Register.FlagIOPL);
        if (Block.PMode && Register.getFlag(Register.FlagVM) == 0
                && (Register.getFlagIOPL() < Block.CPL))
            mask &= (~Register.FlagIF);
        if (use32)
            setFlags(pop32(), mask);
        else
            setFlags(pop16(), mask & 0xffff);
        Flags.destroyConditionFlags();
        return false;
    }

    public static boolean pushf(int use32) {
        if (Block.PMode && Register.getFlag(Register.FlagVM) != 0
                && (Register.getFlag(Register.FlagIOPL) != Register.FlagIOPL)) {
            /* Not enough privileges to execute PUSHF */
            return prepareException(ExceptionGP, 0);
        }
        Flags.fillFlags();
        if (use32 != 0)
            push32(Register.Flags & 0xfcffff);
        else
            push16(Register.Flags);
        return false;
    }

    public static boolean pushf(boolean use32) {
        if (Block.PMode && Register.getFlag(Register.FlagVM) != 0
                && (Register.getFlag(Register.FlagIOPL) != Register.FlagIOPL)) {
            /* Not enough privileges to execute PUSHF */
            return prepareException(ExceptionGP, 0);
        }
        Flags.fillFlags();
        if (use32)
            push32(Register.Flags & 0xfcffff);
        else
            push16(Register.Flags);
        return false;
    }

    public static void checkSegments() {
        boolean needsInvalidation = false;
        Descriptor desc = new Descriptor();
        if (!Block.GDT.getDescriptor(Register.segValue(Register.SEG_NAME_ES), desc))
            needsInvalidation = true;
        else
            switch (desc.type()) {
                case Descriptor.DESC_DATA_EU_RO_NA:
                case Descriptor.DESC_DATA_EU_RO_A:
                case Descriptor.DESC_DATA_EU_RW_NA:
                case Descriptor.DESC_DATA_EU_RW_A:
                case Descriptor.DESC_DATA_ED_RO_NA:
                case Descriptor.DESC_DATA_ED_RO_A:
                case Descriptor.DESC_DATA_ED_RW_NA:
                case Descriptor.DESC_DATA_ED_RW_A:
                case Descriptor.DESC_CODE_N_NC_A:
                case Descriptor.DESC_CODE_N_NC_NA:
                case Descriptor.DESC_CODE_R_NC_A:
                case Descriptor.DESC_CODE_R_NC_NA:
                    if (Block.CPL > desc.dpl())
                        needsInvalidation = true;
                    break;
                default:
                    break;
            }
        if (needsInvalidation)
            setSegGeneral(Register.SEG_NAME_ES, 0);

        needsInvalidation = false;
        if (!Block.GDT.getDescriptor(Register.segValue(Register.SEG_NAME_DS), desc))
            needsInvalidation = true;
        else
            switch (desc.type()) {
                case Descriptor.DESC_DATA_EU_RO_NA:
                case Descriptor.DESC_DATA_EU_RO_A:
                case Descriptor.DESC_DATA_EU_RW_NA:
                case Descriptor.DESC_DATA_EU_RW_A:
                case Descriptor.DESC_DATA_ED_RO_NA:
                case Descriptor.DESC_DATA_ED_RO_A:
                case Descriptor.DESC_DATA_ED_RW_NA:
                case Descriptor.DESC_DATA_ED_RW_A:
                case Descriptor.DESC_CODE_N_NC_A:
                case Descriptor.DESC_CODE_N_NC_NA:
                case Descriptor.DESC_CODE_R_NC_A:
                case Descriptor.DESC_CODE_R_NC_NA:
                    if (Block.CPL > desc.dpl())
                        needsInvalidation = true;
                    break;
                default:
                    break;
            }
        if (needsInvalidation)
            setSegGeneral(Register.SEG_NAME_DS, 0);

        needsInvalidation = false;
        if (!Block.GDT.getDescriptor(Register.segValue(Register.SEG_NAME_FS), desc))
            needsInvalidation = true;
        else
            switch (desc.type()) {
                case Descriptor.DESC_DATA_EU_RO_NA:
                case Descriptor.DESC_DATA_EU_RO_A:
                case Descriptor.DESC_DATA_EU_RW_NA:
                case Descriptor.DESC_DATA_EU_RW_A:
                case Descriptor.DESC_DATA_ED_RO_NA:
                case Descriptor.DESC_DATA_ED_RO_A:
                case Descriptor.DESC_DATA_ED_RW_NA:
                case Descriptor.DESC_DATA_ED_RW_A:
                case Descriptor.DESC_CODE_N_NC_A:
                case Descriptor.DESC_CODE_N_NC_NA:
                case Descriptor.DESC_CODE_R_NC_A:
                case Descriptor.DESC_CODE_R_NC_NA:
                    if (Block.CPL > desc.dpl())
                        needsInvalidation = true;
                    break;
                default:
                    break;
            }
        if (needsInvalidation)
            setSegGeneral(Register.SEG_NAME_FS, 0);

        needsInvalidation = false;
        if (!Block.GDT.getDescriptor(Register.segValue(Register.SEG_NAME_GS), desc))
            needsInvalidation = true;
        else
            switch (desc.type()) {
                case Descriptor.DESC_DATA_EU_RO_NA:
                case Descriptor.DESC_DATA_EU_RO_A:
                case Descriptor.DESC_DATA_EU_RW_NA:
                case Descriptor.DESC_DATA_EU_RW_A:
                case Descriptor.DESC_DATA_ED_RO_NA:
                case Descriptor.DESC_DATA_ED_RO_A:
                case Descriptor.DESC_DATA_ED_RW_NA:
                case Descriptor.DESC_DATA_ED_RW_A:
                case Descriptor.DESC_CODE_N_NC_A:
                case Descriptor.DESC_CODE_N_NC_NA:
                case Descriptor.DESC_CODE_R_NC_A:
                case Descriptor.DESC_CODE_R_NC_NA:
                    if (Block.CPL > desc.dpl())
                        needsInvalidation = true;
                    break;
                default:
                    break;
            }
        if (needsInvalidation)
            setSegGeneral(Register.SEG_NAME_GS, 0);
    }

    private static TaskStateSegment _cpuTss;

    enum TSwitchType {
        JMP(0), CALL_INT(1), IRET(2);
        private final int value;

        private TSwitchType(int value) {
            this.value = value;
        }

        public int toValue() {
            return value;
        }

    }

    private static boolean switchTask(int newTSSSelector, TSwitchType tsType, int oldEIP) {
        Flags.fillFlags();
        TaskStateSegment newTSS = new TaskStateSegment();
        if (!newTSS.setSelector(newTSSSelector))
            Support.exceptionExit("Illegal TSS for switch, selector=%x, switchtype=%x",
                    newTSSSelector, tsType.toValue());
        if (tsType == TSwitchType.IRET) {
            if (!newTSS.Desc.isBusy())
                Support.exceptionExit("TSS not busy for IRET");
        } else {
            if (newTSS.Desc.isBusy())
                Support.exceptionExit("TSS busy for JMP/CALL/INT");
        }
        int newCR3 = 0;
        int newEAX, newEBX, newECX, newEDX, newESP, newEBP, newESI, newEDI;
        int newES, newCS, newSS, newDS, newFS, newGS;
        int newLDT, newEIP, newEFlags;
        /* Read new context from new TSS */
        if (newTSS.Is386) {
            newCR3 = Memory
                    .readD(newTSS.BaseAddr + TaskStateSegment.offsetOf(TaskStateSegment.TSS32.cr3));
            newEIP = Memory
                    .readD(newTSS.BaseAddr + TaskStateSegment.offsetOf(TaskStateSegment.TSS32.eip));
            newEFlags = Memory.readD(
                    newTSS.BaseAddr + TaskStateSegment.offsetOf(TaskStateSegment.TSS32.eflags));
            newEAX = Memory
                    .readD(newTSS.BaseAddr + TaskStateSegment.offsetOf(TaskStateSegment.TSS32.eax));
            newECX = Memory
                    .readD(newTSS.BaseAddr + TaskStateSegment.offsetOf(TaskStateSegment.TSS32.ecx));
            newEDX = Memory
                    .readD(newTSS.BaseAddr + TaskStateSegment.offsetOf(TaskStateSegment.TSS32.edx));
            newEBX = Memory
                    .readD(newTSS.BaseAddr + TaskStateSegment.offsetOf(TaskStateSegment.TSS32.ebx));
            newESP = Memory
                    .readD(newTSS.BaseAddr + TaskStateSegment.offsetOf(TaskStateSegment.TSS32.esp));
            newEBP = Memory
                    .readD(newTSS.BaseAddr + TaskStateSegment.offsetOf(TaskStateSegment.TSS32.ebp));
            newEDI = Memory
                    .readD(newTSS.BaseAddr + TaskStateSegment.offsetOf(TaskStateSegment.TSS32.edi));
            newESI = Memory
                    .readD(newTSS.BaseAddr + TaskStateSegment.offsetOf(TaskStateSegment.TSS32.esi));

            newES = Memory
                    .readW(newTSS.BaseAddr + TaskStateSegment.offsetOf(TaskStateSegment.TSS32.es));
            newCS = Memory
                    .readW(newTSS.BaseAddr + TaskStateSegment.offsetOf(TaskStateSegment.TSS32.cs));
            newSS = Memory
                    .readW(newTSS.BaseAddr + TaskStateSegment.offsetOf(TaskStateSegment.TSS32.ss));
            newDS = Memory
                    .readW(newTSS.BaseAddr + TaskStateSegment.offsetOf(TaskStateSegment.TSS32.ds));
            newFS = Memory
                    .readW(newTSS.BaseAddr + TaskStateSegment.offsetOf(TaskStateSegment.TSS32.fs));
            newGS = Memory
                    .readW(newTSS.BaseAddr + TaskStateSegment.offsetOf(TaskStateSegment.TSS32.gs));
            newLDT = Memory
                    .readW(newTSS.BaseAddr + TaskStateSegment.offsetOf(TaskStateSegment.TSS32.ldt));
        } else {
            Support.exceptionExit("286 task switch");
            newCR3 = 0;
            newEIP = 0;
            newEFlags = 0;
            newEAX = 0;
            newECX = 0;
            newEDX = 0;
            newEBX = 0;
            newESP = 0;
            newEBP = 0;
            newEDI = 0;
            newESI = 0;

            newES = 0;
            newCS = 0;
            newSS = 0;
            newDS = 0;
            newFS = 0;
            newGS = 0;
            newLDT = 0;
        }

        /* Check if we need to clear busy bit of old TASK */
        if (tsType == TSwitchType.JMP || tsType == TSwitchType.IRET) {
            _cpuTss.Desc.setBusy(false);
            _cpuTss.saveSelector();
        }
        int oldFlags = Register.Flags;
        if (tsType == TSwitchType.IRET)
            oldFlags &= (~Register.FlagNT);

        /* Save current context in current TSS */
        if (_cpuTss.Is386) {
            Memory.writeD(
                    _cpuTss.BaseAddr + TaskStateSegment.offsetOf(TaskStateSegment.TSS32.eflags),
                    oldFlags);
            Memory.writeD(_cpuTss.BaseAddr + TaskStateSegment.offsetOf(TaskStateSegment.TSS32.eip),
                    oldEIP);

            Memory.writeD(_cpuTss.BaseAddr + TaskStateSegment.offsetOf(TaskStateSegment.TSS32.eax),
                    Register.getRegEAX());
            Memory.writeD(_cpuTss.BaseAddr + TaskStateSegment.offsetOf(TaskStateSegment.TSS32.ecx),
                    Register.getRegECX());
            Memory.writeD(_cpuTss.BaseAddr + TaskStateSegment.offsetOf(TaskStateSegment.TSS32.edx),
                    Register.getRegEDX());
            Memory.writeD(_cpuTss.BaseAddr + TaskStateSegment.offsetOf(TaskStateSegment.TSS32.ebx),
                    Register.getRegEBX());
            Memory.writeD(_cpuTss.BaseAddr + TaskStateSegment.offsetOf(TaskStateSegment.TSS32.esp),
                    Register.getRegESP());
            Memory.writeD(_cpuTss.BaseAddr + TaskStateSegment.offsetOf(TaskStateSegment.TSS32.ebp),
                    Register.getRegEBP());
            Memory.writeD(_cpuTss.BaseAddr + TaskStateSegment.offsetOf(TaskStateSegment.TSS32.esi),
                    Register.getRegESI());
            Memory.writeD(_cpuTss.BaseAddr + TaskStateSegment.offsetOf(TaskStateSegment.TSS32.edi),
                    Register.getRegEDI());

            Memory.writeD(_cpuTss.BaseAddr + TaskStateSegment.offsetOf(TaskStateSegment.TSS32.es),
                    Register.segValue(Register.SEG_NAME_ES));
            Memory.writeD(_cpuTss.BaseAddr + TaskStateSegment.offsetOf(TaskStateSegment.TSS32.cs),
                    Register.segValue(Register.SEG_NAME_CS));
            Memory.writeD(_cpuTss.BaseAddr + TaskStateSegment.offsetOf(TaskStateSegment.TSS32.ss),
                    Register.segValue(Register.SEG_NAME_SS));
            Memory.writeD(_cpuTss.BaseAddr + TaskStateSegment.offsetOf(TaskStateSegment.TSS32.ds),
                    Register.segValue(Register.SEG_NAME_DS));
            Memory.writeD(_cpuTss.BaseAddr + TaskStateSegment.offsetOf(TaskStateSegment.TSS32.fs),
                    Register.segValue(Register.SEG_NAME_FS));
            Memory.writeD(_cpuTss.BaseAddr + TaskStateSegment.offsetOf(TaskStateSegment.TSS32.gs),
                    Register.segValue(Register.SEG_NAME_GS));
        } else {
            Support.exceptionExit("286 task switch");
        }

        /* Setup a back link to the old TSS in new TSS */
        if (tsType == TSwitchType.CALL_INT) {
            if (newTSS.Is386) {
                Memory.writeD(
                        newTSS.BaseAddr + TaskStateSegment.offsetOf(TaskStateSegment.TSS32.back),
                        _cpuTss.Selector);
            } else {
                Memory.writeW(
                        newTSS.BaseAddr + TaskStateSegment.offsetOf(TaskStateSegment.TSS16.back),
                        _cpuTss.Selector);
            }
            /* And make the new task's eflag have the nested task bit */
            newEFlags |= Register.FlagNT;
        }
        /* Set the busy bit in the new task */
        if (tsType == TSwitchType.JMP || tsType == TSwitchType.CALL_INT) {
            newTSS.Desc.setBusy(true);
            newTSS.saveSelector();
        }

        // cpu.cr0|=CR0_TASKSWITCHED;
        if (newTSSSelector == _cpuTss.Selector) {
            Register.setRegEIP(oldEIP);
            newCS = Register.segValue(Register.SEG_NAME_CS);
            newSS = Register.segValue(Register.SEG_NAME_SS);
            newDS = Register.segValue(Register.SEG_NAME_DS);
            newES = Register.segValue(Register.SEG_NAME_ES);
            newFS = Register.segValue(Register.SEG_NAME_FS);
            newGS = Register.segValue(Register.SEG_NAME_GS);
        } else {

            /* Setup the new cr3 */
            Paging.setDirBase(newCR3);

            /* Load new context */
            if (newTSS.Is386) {
                Register.setRegEIP(newEIP);
                setFlags(newEFlags, Register.FMaskAll | Register.FlagVM);
                Register.setRegEAX(newEAX);
                Register.setRegECX(newECX);
                Register.setRegEDX(newEDX);
                Register.setRegEBX(newEBX);
                Register.setRegESP(newESP);
                Register.setRegEBP(newEBP);
                Register.setRegEDI(newEDI);
                Register.setRegESI(newESI);

                // new_cs=mem_readw(new_tss.base+offsetof(TSS32,cs));
            } else {
                Support.exceptionExit("286 task switch");
            }
        }
        /* Load the new selectors */
        if ((Register.Flags & Register.FlagVM) != 0) {
            Register.segSet16(Register.SEG_NAME_CS, newCS);
            Block.Code.Big = false;
            Block.CPL = 3; // We don't have segment caches so this will do
        } else {
            /* Protected mode task */
            if (newLDT != 0)
                lldt(newLDT);
            /* Load the new CS */
            Descriptor csDesc = new Descriptor();
            Block.CPL = newCS & 3;
            if (!Block.GDT.getDescriptor(newCS, csDesc))
                Support.exceptionExit("Task switch with CS beyond limits");
            if (csDesc.Saved.Seg.P == 0)
                Support.exceptionExit("Task switch with non present code-segment");
            switch (csDesc.type()) {
                case Descriptor.DESC_CODE_N_NC_A:
                case Descriptor.DESC_CODE_N_NC_NA:
                case Descriptor.DESC_CODE_R_NC_A:
                case Descriptor.DESC_CODE_R_NC_NA:
                    if (Block.CPL != csDesc.dpl())
                        Support.exceptionExit("Task CS RPL != DPL");
                    // goto doconforming;
                    doConforming(csDesc, newCS);
                    break;
                case Descriptor.DESC_CODE_N_C_A:
                case Descriptor.DESC_CODE_N_C_NA:
                case Descriptor.DESC_CODE_R_C_A:
                case Descriptor.DESC_CODE_R_C_NA:
                    if (Block.CPL < csDesc.dpl())
                        Support.exceptionExit("Task CS RPL < DPL");
                    doConforming(csDesc, newCS);
                    // doconforming:
                    // Register.SegmentPhys[(int)Register.SEG_NAME_CS] = cs_desc.GetBase();
                    // Block.Code.Big = cs_desc.Big > 0;
                    // Register.SegmentVals[(int)Register.SEG_NAME_CS] = new_cs;
                    break;
                default:
                    Support.exceptionExit("Task switch CS Type %d", csDesc.type());
                    break;
            }
        }
        setSegGeneral(Register.SEG_NAME_ES, newES);
        setSegGeneral(Register.SEG_NAME_SS, newSS);
        setSegGeneral(Register.SEG_NAME_DS, newDS);
        setSegGeneral(Register.SEG_NAME_FS, newFS);
        setSegGeneral(Register.SEG_NAME_GS, newGS);
        if (!_cpuTss.setSelector(newTSSSelector)) {
            Log.logging(Log.LogTypes.CPU, Log.LogServerities.Normal,
                    "TaskSwitch: set tss selector %X failed", newTSSSelector);
        }
        // cpu_tss.desc.SetBusy(true);
        // cpu_tss.SaveSelector();
        // Log.LOG_MSG("Task CPL %X CS:%X IP:%X SS:%X SP:%X eflags
        // %x",cpu.cpl,SegValue(cs),reg_eip,SegValue(ss),reg_esp,reg_flags);
        return true;
    }

    // switchTask()의 goto label doconforming:
    private static void doConforming(Descriptor descCS, int newCS) {
        Register.SegmentPhys[Register.SEG_NAME_CS] = descCS.getBase();
        Block.Code.Big = descCS.big() > 0;
        Register.SegmentVals[Register.SEG_NAME_CS] = newCS;
    }

    public static void iret(boolean use32, int oldeip) {
        if (!Block.PMode) { /* RealMode IRET */
            if (use32) {
                Register.setRegEIP(pop32());
                Register.segSet16(Register.SEG_NAME_CS, pop32());
                setFlags(pop32(), Register.FMaskAll);
            } else {
                Register.setRegEIP(pop16());
                Register.segSet16(Register.SEG_NAME_CS, pop16());
                setFlags(pop16(), Register.FMaskAll & 0xffff);
            }
            Block.Code.Big = false;
            Flags.destroyConditionFlags();
            return;
        } else { /* Protected mode IRET */
            if ((Register.Flags & Register.FlagVM) != 0) {
                if ((Register.Flags & Register.FlagIOPL) != Register.FlagIOPL) {
                    // win3.x e
                    exception(ExceptionGP, 0);
                    return;
                } else {
                    if (use32) {
                        int newEIP = Memory.readD(Register.segPhys(Register.SEG_NAME_SS)
                                + (Register.getRegESP() & Block.Stack.Mask));
                        int tempESP = (Register.getRegESP() & Block.Stack.NotMask)
                                | ((Register.getRegESP() + 4) & Block.Stack.Mask);
                        int newCS = Memory.readD(Register.segPhys(Register.SEG_NAME_SS)
                                + (tempESP & Block.Stack.Mask));
                        tempESP = (tempESP & Block.Stack.NotMask)
                                | ((tempESP + 4) & Block.Stack.Mask);
                        int newFlags = Memory.readD(Register.segPhys(Register.SEG_NAME_SS)
                                + (tempESP & Block.Stack.Mask));
                        Register.setRegESP((tempESP & Block.Stack.NotMask)
                                | ((tempESP + 4) & Block.Stack.Mask));

                        Register.setRegEIP(newEIP);
                        Register.segSet16(Register.SEG_NAME_CS, newCS & 0xffff);
                        /* IOPL can not be modified in v86 mode by IRET */
                        setFlags(newFlags, Register.FMaskNormal | Register.FlagNT);
                    } else {
                        int newEIP = Memory.readW(Register.segPhys(Register.SEG_NAME_SS)
                                + (Register.getRegESP() & Block.Stack.Mask));
                        int tempesp = (Register.getRegESP() & Block.Stack.NotMask)
                                | ((Register.getRegESP() + 2) & Block.Stack.Mask);
                        int new_cs = Memory.readW(Register.segPhys(Register.SEG_NAME_SS)
                                + (tempesp & Block.Stack.Mask));
                        tempesp = (tempesp & Block.Stack.NotMask)
                                | ((tempesp + 2) & Block.Stack.Mask);
                        int new_flags = Memory.readW(Register.segPhys(Register.SEG_NAME_SS)
                                + (tempesp & Block.Stack.Mask));
                        Register.setRegESP((tempesp & Block.Stack.NotMask)
                                | ((tempesp + 2) & Block.Stack.Mask));

                        Register.setRegEIP((int) newEIP);
                        Register.segSet16(Register.SEG_NAME_CS, new_cs);
                        /* IOPL can not be modified in v86 mode by IRET */
                        setFlags(new_flags, Register.FMaskNormal | Register.FlagNT);
                    }
                    Block.Code.Big = false;
                    Flags.destroyConditionFlags();
                    return;
                }
            }
            /* Check if this is task IRET */
            if (Register.getFlag(Register.FlagNT) != 0) {
                if (Register.getFlag(Register.FlagVM) != 0)
                    Support.exceptionExit("Pmode IRET with VM bit set");
                checkCPUCond(!_cpuTss.isValid(), "TASK Iret without valid TSS", ExceptionTS,
                        _cpuTss.Selector & 0xfffc);
                if (!_cpuTss.Desc.isBusy()) {
                    Log.logging(Log.LogTypes.CPU, Log.LogServerities.Error,
                            "TASK Iret:TSS not busy");
                }
                int back_link = _cpuTss.getBack();
                switchTask(back_link, TSwitchType.IRET, oldeip);
                return;
            }
            int n_cs_sel, n_eip, n_flags;
            int tempesp_;
            if (use32) {
                n_eip = Memory.readD(Register.segPhys(Register.SEG_NAME_SS)
                        + (Register.getRegESP() & Block.Stack.Mask));
                tempesp_ = (Register.getRegESP() & Block.Stack.NotMask)
                        | ((Register.getRegESP() + 4) & Block.Stack.Mask);
                n_cs_sel = Memory.readD(
                        Register.segPhys(Register.SEG_NAME_SS) + (tempesp_ & Block.Stack.Mask))
                        & 0xffff;
                tempesp_ = (tempesp_ & Block.Stack.NotMask) | ((tempesp_ + 4) & Block.Stack.Mask);
                n_flags = Memory.readD(
                        Register.segPhys(Register.SEG_NAME_SS) + (tempesp_ & Block.Stack.Mask));
                tempesp_ = (tempesp_ & Block.Stack.NotMask) | ((tempesp_ + 4) & Block.Stack.Mask);

                if ((n_flags & Register.FlagVM) != 0 && (Block.CPL == 0)) {
                    // commit point
                    Register.setRegESP(tempesp_);
                    Register.setRegEIP(n_eip & 0xffff);
                    int n_ss, n_esp, n_es, n_ds, n_fs, n_gs;
                    n_esp = pop32();
                    n_ss = pop32() & 0xffff;
                    n_es = pop32() & 0xffff;
                    n_ds = pop32() & 0xffff;
                    n_fs = pop32() & 0xffff;
                    n_gs = pop32() & 0xffff;

                    setFlags(n_flags, Register.FMaskAll | Register.FlagVM);
                    Flags.destroyConditionFlags();
                    Block.CPL = 3;

                    setSegGeneral(Register.SEG_NAME_SS, n_ss);
                    setSegGeneral(Register.SEG_NAME_ES, n_es);
                    setSegGeneral(Register.SEG_NAME_DS, n_ds);
                    setSegGeneral(Register.SEG_NAME_FS, n_fs);
                    setSegGeneral(Register.SEG_NAME_GS, n_gs);
                    Register.setRegESP(n_esp);
                    Block.Code.Big = false;
                    Register.segSet16(Register.SEG_NAME_CS, n_cs_sel);
                    Log.logging(Log.LogTypes.CPU, Log.LogServerities.Normal,
                            "IRET:Back to V86: CS:%X IP %X SS:%X SP %X FLAGS:%X",
                            Register.segValue(Register.SEG_NAME_CS), Register.getRegEIP(),
                            Register.segValue(Register.SEG_NAME_SS), Register.getRegESP(),
                            Register.Flags);
                    return;
                }
                if ((n_flags & Register.FlagVM) != 0)
                    Support.exceptionExit("IRET from pmode to v86 with CPL!=0");
            } else {
                n_eip = Memory.readW(Register.segPhys(Register.SEG_NAME_SS)
                        + (Register.getRegESP() & Block.Stack.Mask));
                tempesp_ = (Register.getRegESP() & Block.Stack.NotMask)
                        | ((Register.getRegESP() + 2) & Block.Stack.Mask);
                n_cs_sel = Memory.readW(
                        Register.segPhys(Register.SEG_NAME_SS) + (tempesp_ & Block.Stack.Mask));
                tempesp_ = (tempesp_ & Block.Stack.NotMask) | ((tempesp_ + 2) & Block.Stack.Mask);
                n_flags = Memory.readW(
                        Register.segPhys(Register.SEG_NAME_SS) + (tempesp_ & Block.Stack.Mask));
                n_flags |= (Register.Flags & 0xffff0000);
                tempesp_ = (tempesp_ & Block.Stack.NotMask) | ((tempesp_ + 2) & Block.Stack.Mask);

                if ((n_flags & Register.FlagVM) != 0)
                    Support.exceptionExit("VM Flag in 16-bit iret");
            }
            checkCPUCond((n_cs_sel & 0xfffc) == 0, "IRET:CS selector zero", ExceptionGP, 0);
            int n_cs_rpl = n_cs_sel & 3;
            Descriptor n_cs_desc = new Descriptor();
            checkCPUCond(!Block.GDT.getDescriptor(n_cs_sel, n_cs_desc),
                    "IRET:CS selector beyond limits", ExceptionGP, n_cs_sel & 0xfffc);
            checkCPUCond(n_cs_rpl < Block.CPL, "IRET to lower privilege", ExceptionGP,
                    n_cs_sel & 0xfffc);

            switch (n_cs_desc.type()) {
                case Descriptor.DESC_CODE_N_NC_A:
                case Descriptor.DESC_CODE_N_NC_NA:
                case Descriptor.DESC_CODE_R_NC_A:
                case Descriptor.DESC_CODE_R_NC_NA:
                    checkCPUCond(n_cs_rpl != n_cs_desc.dpl(), "IRET:NC:DPL!=RPL", ExceptionGP,
                            n_cs_sel & 0xfffc);
                    break;
                case Descriptor.DESC_CODE_N_C_A:
                case Descriptor.DESC_CODE_N_C_NA:
                case Descriptor.DESC_CODE_R_C_A:
                case Descriptor.DESC_CODE_R_C_NA:
                    checkCPUCond(n_cs_desc.dpl() > n_cs_rpl, "IRET:C:DPL>RPL", ExceptionGP,
                            n_cs_sel & 0xfffc);
                    break;
                default:
                    Support.exceptionExit("IRET:Illegal descriptor type %X", n_cs_desc.type());
                    break;
            }
            checkCPUCond(n_cs_desc.Saved.Seg.P == 0, "IRET with nonpresent code segment",
                    ExceptionNP, n_cs_sel & 0xfffc);

            if (n_cs_rpl == Block.CPL) {
                /* Return to same level */

                // commit point
                Register.setRegESP(tempesp_);
                Register.SegmentPhys[Register.SEG_NAME_CS] = n_cs_desc.getBase();
                Block.Code.Big = n_cs_desc.big() > 0;
                Register.SegmentVals[Register.SEG_NAME_CS] = n_cs_sel;
                Register.setRegEIP(n_eip);

                int mask = Block.CPL != 0 ? (Register.FMaskNormal | Register.FlagNT)
                        : Register.FMaskAll;
                if (Register.getFlagIOPL() < Block.CPL)
                    mask &= (~Register.FlagIF);
                setFlags(n_flags, mask);
                Flags.destroyConditionFlags();
                Log.logging(Log.LogTypes.CPU, Log.LogServerities.Normal,
                        "IRET:Same level:%X:%X big %d", (double) n_cs_sel, (double) n_eip,
                        Convert.toByte(Block.Code.Big));
            } else {
                /* Return to outer level */
                int n_ss, n_esp;
                if (use32) {
                    n_esp = Memory.readD(
                            Register.segPhys(Register.SEG_NAME_SS) + (tempesp_ & Block.Stack.Mask));
                    tempesp_ =
                            (tempesp_ & Block.Stack.NotMask) | ((tempesp_ + 4) & Block.Stack.Mask);
                    n_ss = Memory.readD(
                            Register.segPhys(Register.SEG_NAME_SS) + (tempesp_ & Block.Stack.Mask))
                            & 0xffff;
                } else {
                    n_esp = Memory.readW(
                            Register.segPhys(Register.SEG_NAME_SS) + (tempesp_ & Block.Stack.Mask));
                    tempesp_ =
                            (tempesp_ & Block.Stack.NotMask) | ((tempesp_ + 2) & Block.Stack.Mask);
                    n_ss = Memory.readW(
                            Register.segPhys(Register.SEG_NAME_SS) + (tempesp_ & Block.Stack.Mask));
                }
                checkCPUCond((n_ss & 0xfffc) == 0, "IRET:Outer level:SS selector zero", ExceptionGP,
                        0);
                checkCPUCond((n_ss & 3) != n_cs_rpl, "IRET:Outer level:SS rpl!=CS rpl", ExceptionGP,
                        n_ss & 0xfffc);
                Descriptor n_ss_desc = new Descriptor();
                checkCPUCond(!Block.GDT.getDescriptor(n_ss, n_ss_desc),
                        "IRET:Outer level:SS beyond limit", ExceptionGP, n_ss & 0xfffc);
                checkCPUCond(n_ss_desc.dpl() != n_cs_rpl, "IRET:Outer level:SS dpl!=CS rpl",
                        ExceptionGP, n_ss & 0xfffc);

                // check if stack segment is a writable data segment
                switch (n_ss_desc.type()) {
                    case Descriptor.DESC_DATA_EU_RW_NA:
                    case Descriptor.DESC_DATA_EU_RW_A:
                    case Descriptor.DESC_DATA_ED_RW_NA:
                    case Descriptor.DESC_DATA_ED_RW_A:
                        break;
                    default:
                        // or #GP(ss_sel)
                        Support.exceptionExit("IRET:Outer level:Stack segment not writable");

                        break;
                }
                checkCPUCond(n_ss_desc.Saved.Seg.P == 0,
                        "IRET:Outer level:Stack segment not present", ExceptionNP, n_ss & 0xfffc);

                // commit point

                Register.SegmentPhys[Register.SEG_NAME_CS] = n_cs_desc.getBase();
                Block.Code.Big = n_cs_desc.big() > 0;
                Register.SegmentVals[Register.SEG_NAME_CS] = n_cs_sel;

                int mask = Block.CPL != 0 ? (Register.FMaskNormal | Register.FlagNT)
                        : Register.FMaskAll;
                if (Register.getFlagIOPL() < Block.CPL)
                    mask &= (~Register.FlagIF);
                setFlags(n_flags, mask);
                Flags.destroyConditionFlags();

                Block.CPL = n_cs_rpl;
                Register.setRegEIP(n_eip);

                Register.SegmentVals[Register.SEG_NAME_SS] = n_ss;
                Register.SegmentPhys[Register.SEG_NAME_SS] = n_ss_desc.getBase();
                if (n_ss_desc.big() != 0) {
                    Block.Stack.Big = true;
                    Block.Stack.Mask = 0xffffffff;
                    Block.Stack.NotMask = 0;
                    Register.setRegESP(n_esp);
                } else {
                    Block.Stack.Big = false;
                    Block.Stack.Mask = 0xffff;
                    Block.Stack.NotMask = 0xffff0000;
                    Register.setRegSP(n_esp & 0xffff);
                }

                // borland extender, zrdx
                checkSegments();

                Log.logging(Log.LogTypes.CPU, Log.LogServerities.Normal,
                        "IRET:Outer level:%X:%X big %d", (double) n_cs_sel, (double) n_eip,
                        Convert.toByte(Block.Code.Big));
            }
            return;
        }
    }

    public static void jmp(boolean use32, int selector, int offset, int oldEIP) {
        if (!Block.PMode || (Register.Flags & Register.FlagVM) != 0) {
            if (!use32) {
                Register.setRegEIP(offset & 0xffff);
            } else {
                Register.setRegEIP(offset);
            }
            Register.segSet16(Register.SEG_NAME_CS, selector);
            Block.Code.Big = false;
            return;
        } else {
            checkCPUCond((selector & 0xfffc) == 0, "JMP:CS selector zero", ExceptionGP, 0);
            int rpl = selector & 3;
            Descriptor desc = new Descriptor();
            checkCPUCond(!Block.GDT.getDescriptor(selector, desc), "JMP:CS beyond limits",
                    ExceptionGP, selector & 0xfffc);
            switch (desc.type()) {
                case Descriptor.DESC_CODE_N_NC_A:
                case Descriptor.DESC_CODE_N_NC_NA:
                case Descriptor.DESC_CODE_R_NC_A:
                case Descriptor.DESC_CODE_R_NC_NA:
                    checkCPUCond(rpl > Block.CPL, "JMP:NC:RPL>CPL", ExceptionGP, selector & 0xfffc);
                    checkCPUCond(Block.CPL != desc.dpl(), "JMP:NC:RPL != DPL", ExceptionGP,
                            selector & 0xfffc);
                    Log.logging(Log.LogTypes.CPU, Log.LogServerities.Normal,
                            "JMP:Code:NC to %X:%X big %d", selector, offset, desc.big());
                    // goto CODE_jmp;
                    jumpCODE(desc, selector, offset);
                    return;
                case Descriptor.DESC_CODE_N_C_A:
                case Descriptor.DESC_CODE_N_C_NA:
                case Descriptor.DESC_CODE_R_C_A:
                case Descriptor.DESC_CODE_R_C_NA:
                    Log.logging(Log.LogTypes.CPU, Log.LogServerities.Normal,
                            "JMP:Code:C to %X:%X big %d", selector, offset, desc.big());
                    checkCPUCond(Block.CPL < desc.dpl(), "JMP:C:CPL < DPL", ExceptionGP,
                            selector & 0xfffc);
                    // CODE_jmp:
                    // if (desc.Saved.Seg.P == 0)
                    // {
                    // // win
                    // Exception(ExceptionNP, selector & 0xfffc);
                    // return;
                    // }

                    // /* Normal jump to another selector:offset */
                    // Register.SegmentPhys[(int)Register.SEG_NAME_CS] = desc.GetBase();
                    // Block.Code.Big = desc.Big() > 0;
                    // Register.SegmentVals[(int)Register.SEG_NAME_CS] = (selector & 0xfffc) |
                    // Block.CPL;
                    // Register.setRegEIP(offset);
                    // return;
                    jumpCODE(desc, selector, offset);
                    return;
                case Descriptor.DESC_386_TSS_A:
                    checkCPUCond(desc.dpl() < Block.CPL, "JMP:TSS:dpl<cpl", ExceptionGP,
                            selector & 0xfffc);
                    checkCPUCond(desc.dpl() < rpl, "JMP:TSS:dpl<rpl", ExceptionGP,
                            selector & 0xfffc);
                    Log.logging(Log.LogTypes.CPU, Log.LogServerities.Normal, "JMP:TSS to %X",
                            selector);
                    switchTask(selector, TSwitchType.JMP, oldEIP);
                    break;
                default:
                    Support.exceptionExit("JMP Illegal descriptor type %X", desc.type());
                    break;
            }
        }
        // assert(1);
    }

    // CODE_jmp:
    private static void jumpCODE(Descriptor desc, int selector, int offset) {
        if (desc.Saved.Seg.P == 0) {
            // win
            exception(ExceptionNP, selector & 0xfffc);
            return;
        }

        /* Normal jump to another selector:offset */
        Register.SegmentPhys[Register.SEG_NAME_CS] = desc.getBase();
        Block.Code.Big = desc.big() > 0;
        Register.SegmentVals[Register.SEG_NAME_CS] = (selector & 0xfffc) | Block.CPL;
        Register.setRegEIP(offset);
        return;
    }

    public static void call(boolean use32, int selector, int offset, int oldEIP) {
        if (!Block.PMode || (Register.Flags & Register.FlagVM) != 0) {
            if (!use32) {
                push16(Register.segValue(Register.SEG_NAME_CS));
                push16(oldEIP);
                Register.setRegEIP(offset & 0xffff);
            } else {
                push32(Register.segValue(Register.SEG_NAME_CS));
                push32(oldEIP);
                Register.setRegEIP(offset);
            }
            Block.Code.Big = false;
            Register.segSet16(Register.SEG_NAME_CS, selector);
            return;
        } else {
            checkCPUCond((selector & 0xfffc) == 0, "CALL:CS selector zero", ExceptionGP, 0);
            int rpl = selector & 3;
            Descriptor call = new Descriptor();
            checkCPUCond(!Block.GDT.getDescriptor(selector, call), "CALL:CS beyond limits",
                    ExceptionGP, selector & 0xfffc);
            /* Check for type of far call */
            switch (call.type()) {
                case Descriptor.DESC_CODE_N_NC_A:
                case Descriptor.DESC_CODE_N_NC_NA:
                case Descriptor.DESC_CODE_R_NC_A:
                case Descriptor.DESC_CODE_R_NC_NA:
                    checkCPUCond(rpl > Block.CPL, "CALL:CODE:NC:RPL>CPL", ExceptionGP,
                            selector & 0xfffc);
                    checkCPUCond(call.dpl() != Block.CPL, "CALL:CODE:NC:DPL!=CPL", ExceptionGP,
                            selector & 0xfffc);
                    Log.logging(Log.LogTypes.CPU, Log.LogServerities.Normal,
                            "CALL:CODE:NC to %X:%X", selector, offset);
                    // goto call_code;
                    callCode(use32, call, selector, offset, oldEIP);
                    return;
                case Descriptor.DESC_CODE_N_C_A:
                case Descriptor.DESC_CODE_N_C_NA:
                case Descriptor.DESC_CODE_R_C_A:
                case Descriptor.DESC_CODE_R_C_NA:
                    checkCPUCond(call.dpl() > Block.CPL, "CALL:CODE:C:DPL>CPL", ExceptionGP,
                            selector & 0xfffc);
                    Log.logging(Log.LogTypes.CPU, Log.LogServerities.Normal, "CALL:CODE:C to %X:%X",
                            selector, offset);
                    callCode(use32, call, selector, offset, oldEIP);
                    return;
                // call_code:
                // if (call.Saved.Seg.P == 0)
                // {
                // // borland extender (RTM)
                // Exception(ExceptionNP, selector & 0xfffc);
                // return;
                // }
                // // commit point
                // if (!use32)
                // {
                // Push16(Register.SegValue(Register.SEG_NAME_CS));
                // Push16(oldEIP);
                // Register.setRegEIP(offset & 0xffff);
                // }
                // else
                // {
                // Push32(Register.SegValue(Register.SEG_NAME_CS));
                // Push32(oldEIP);
                // Register.setRegEIP(offset);
                // }
                // Register.SegmentPhys[(int)Register.SEG_NAME_CS] = call.GetBase();
                // Block.Code.Big = call.Big() > 0;
                // Register.SegmentVals[(int)Register.SEG_NAME_CS] = (selector & 0xfffc) |
                // Block.CPL;
                // return;
                case Descriptor.DESC_386_CALL_GATE:
                case Descriptor.DESC_286_CALL_GATE: {
                    checkCPUCond(call.dpl() < Block.CPL, "CALL:Gate:Gate DPL<CPL", ExceptionGP,
                            selector & 0xfffc);
                    checkCPUCond(call.dpl() < rpl, "CALL:Gate:Gate DPL<RPL", ExceptionGP,
                            selector & 0xfffc);
                    checkCPUCond(call.Saved.Seg.P == 0, "CALL:Gate:Segment not present",
                            ExceptionNP, selector & 0xfffc);
                    Descriptor n_cs_desc = new Descriptor();
                    int n_cs_sel = call.getSelector();

                    checkCPUCond((n_cs_sel & 0xfffc) == 0, "CALL:Gate:CS selector zero",
                            ExceptionGP, 0);
                    checkCPUCond(!Block.GDT.getDescriptor(n_cs_sel, n_cs_desc),
                            "CALL:Gate:CS beyond limits", ExceptionGP, n_cs_sel & 0xfffc);
                    int n_cs_dpl = n_cs_desc.dpl();
                    checkCPUCond(n_cs_dpl > Block.CPL, "CALL:Gate:CS DPL>CPL", ExceptionGP,
                            n_cs_sel & 0xfffc);

                    checkCPUCond(n_cs_desc.Saved.Seg.P == 0, "CALL:Gate:CS not present",
                            ExceptionNP, n_cs_sel & 0xfffc);

                    int n_eip = call.getOffset();
                    switch (n_cs_desc.type()) {
                        case Descriptor.DESC_CODE_N_NC_A:
                        case Descriptor.DESC_CODE_N_NC_NA:
                        case Descriptor.DESC_CODE_R_NC_A:
                        case Descriptor.DESC_CODE_R_NC_NA:
                            /* Check if we goto inner priviledge */
                            if (n_cs_dpl < Block.CPL) {
                                /* Get new SS:ESP out of TSS */
                                int n_ss_sel, n_esp;
                                Descriptor n_ss_desc = new Descriptor();
                                int where = _cpuTss.whereSSxESPx(n_cs_dpl);
                                n_ss_sel = _cpuTss.getSSx(where);
                                n_esp = _cpuTss.getESPx(where);
                                checkCPUCond((n_ss_sel & 0xfffc) == 0,
                                        "CALL:Gate:NC:SS selector zero", ExceptionTS, 0);
                                checkCPUCond(!Block.GDT.getDescriptor(n_ss_sel, n_ss_desc),
                                        "CALL:Gate:Invalid SS selector", ExceptionTS,
                                        n_ss_sel & 0xfffc);
                                checkCPUCond(
                                        ((n_ss_sel & 3) != n_cs_desc.dpl())
                                                || (n_ss_desc.dpl() != n_cs_desc.dpl()),
                                        "CALL:Gate:Invalid SS selector privileges", ExceptionTS,
                                        n_ss_sel & 0xfffc);

                                switch (n_ss_desc.type()) {
                                    case Descriptor.DESC_DATA_EU_RW_NA:
                                    case Descriptor.DESC_DATA_EU_RW_A:
                                    case Descriptor.DESC_DATA_ED_RW_NA:
                                    case Descriptor.DESC_DATA_ED_RW_A:
                                        // writable data segment
                                        break;
                                    default:
                                        // or #TS(ss_sel)
                                        Support.exceptionExit(
                                                "Call:Gate:SS no writable data segment");
                                        break;
                                }
                                checkCPUCond(n_ss_desc.Saved.Seg.P == 0,
                                        "CALL:Gate:Stack segment not present", ExceptionSS,
                                        n_ss_sel & 0xfffc);

                                /* Load the new SS:ESP and save data on it */
                                int o_esp = Register.getRegESP();
                                int o_ss = Register.segValue(Register.SEG_NAME_SS);
                                int o_stack = Register.segPhys(Register.SEG_NAME_SS)
                                        + (Register.getRegESP() & Block.Stack.Mask);

                                // catch pagefaults
                                if ((call.Saved.Gate.ParamCount & 31) != 0) {
                                    if (call.type() == Descriptor.DESC_386_CALL_GATE) {
                                        for (int i =
                                                (call.Saved.Gate.ParamCount & 31) - 1; i >= 0; i--)
                                            Memory.readD(o_stack + i * 4);
                                    } else {
                                        for (int i =
                                                (call.Saved.Gate.ParamCount & 31) - 1; i >= 0; i--)
                                            Memory.readW(o_stack + i * 2);
                                    }
                                }

                                // commit point
                                Register.SegmentVals[Register.SEG_NAME_SS] = n_ss_sel;
                                Register.SegmentPhys[Register.SEG_NAME_SS] = n_ss_desc.getBase();
                                if (n_ss_desc.big() != 0) {
                                    Block.Stack.Big = true;
                                    Block.Stack.Mask = 0xffffffff;
                                    Block.Stack.NotMask = 0;
                                    Register.setRegESP(n_esp);
                                } else {
                                    Block.Stack.Big = false;
                                    Block.Stack.Mask = 0xffff;
                                    Block.Stack.NotMask = 0xffff0000;
                                    Register.setRegSP(n_esp & 0xffff);
                                }

                                Block.CPL = n_cs_desc.dpl();
                                int oldcs = Register.segValue(Register.SEG_NAME_CS);
                                /* Switch to new CS:EIP */
                                Register.SegmentPhys[Register.SEG_NAME_CS] = n_cs_desc.getBase();
                                Register.SegmentVals[Register.SEG_NAME_CS] =
                                        (n_cs_sel & 0xfffc) | Block.CPL;
                                Block.Code.Big = n_cs_desc.big() > 0;
                                Register.setRegEIP(n_eip);
                                if (!use32)
                                    Register.setRegEIP(Register.getRegEIP() & 0xffff);

                                if (call.type() == Descriptor.DESC_386_CALL_GATE) {
                                    push32(o_ss); // save old stack
                                    push32(o_esp);
                                    if ((call.Saved.Gate.ParamCount & 31) != 0)
                                        for (int i =
                                                (call.Saved.Gate.ParamCount & 31) - 1; i >= 0; i--)
                                            push32(Memory.readD(o_stack + i * 4));
                                    push32(oldcs);
                                    push32(oldEIP);
                                } else {
                                    push16(o_ss); // save old stack
                                    push16(o_esp);
                                    if ((call.Saved.Gate.ParamCount & 31) != 0)
                                        for (int i =
                                                (call.Saved.Gate.ParamCount & 31) - 1; i >= 0; i--)
                                            push16(Memory.readW(o_stack + i * 2));
                                    push16(oldcs);
                                    push16(oldEIP);
                                }

                                break;
                            } else if (n_cs_dpl > Block.CPL)
                                Support.exceptionExit("CALL:GATE:CS DPL>CPL"); // or #GP(sel)
                            // goto GotoDESC_CODE_R_C_NA;
                            gotoDESC_CODE_R_C_NA(use32, call, oldEIP, n_cs_desc, n_cs_sel, n_eip);
                            break;
                        case Descriptor.DESC_CODE_N_C_A:
                        case Descriptor.DESC_CODE_N_C_NA:
                        case Descriptor.DESC_CODE_R_C_A:
                        case Descriptor.DESC_CODE_R_C_NA:
                            gotoDESC_CODE_R_C_NA(use32, call, oldEIP, n_cs_desc, n_cs_sel, n_eip);
                            break;
                        // GotoDESC_CODE_R_C_NA:
                        // // zrdx extender

                        // if (call.Type == Descriptor.DESC_386_CALL_GATE)
                        // {
                        // Push32(Register.SegValue(Register.SEG_NAME_CS));
                        // Push32(oldEIP);
                        // }
                        // else
                        // {
                        // Push16(Register.SegValue(Register.SEG_NAME_CS));
                        // Push16(oldEIP);
                        // }

                        // /* Switch to new CS:EIP */
                        // Register.SegmentPhys[(int)Register.SEG_NAME_CS] = n_cs_desc.GetBase();
                        // Register.SegmentVals[(int)Register.SEG_NAME_CS] = (n_cs_sel & 0xfffc) |
                        // Block.CPL;
                        // Block.Code.Big = n_cs_desc.Big() > 0;
                        // Register.setRegEIP(n_eip);
                        // if (!use32) Register.setRegEIP(Register.getRegEIP()&0xffff);
                        // break;
                        default:
                            Support.exceptionExit("CALL:GATE:CS no executable segment");
                            break;
                    }
                } /* Call Gates */
                    break;
                case Descriptor.DESC_386_TSS_A:
                    checkCPUCond(call.dpl() < Block.CPL, "CALL:TSS:dpl<cpl", ExceptionGP,
                            selector & 0xfffc);
                    checkCPUCond(call.dpl() < rpl, "CALL:TSS:dpl<rpl", ExceptionGP,
                            selector & 0xfffc);

                    checkCPUCond(call.Saved.Seg.P == 0, "CALL:TSS:Segment not present", ExceptionNP,
                            selector & 0xfffc);

                    Log.logging(Log.LogTypes.CPU, Log.LogServerities.Normal, "CALL:TSS to %X",
                            selector);
                    switchTask(selector, TSwitchType.CALL_INT, oldEIP);
                    break;
                case Descriptor.DESC_DATA_EU_RW_NA: // vbdos
                case Descriptor.DESC_INVALID: // used by some installers
                    exception(ExceptionGP, selector & 0xfffc);
                    return;
                default:
                    Support.exceptionExit("CALL:Descriptor type %x unsupported", call.type());
                    break;
            }
        }
        // assert(1);
    }

    // call_code
    private static void callCode(boolean use32, Descriptor call, int selector, int offset,
            int oldEIP) {
        if (call.Saved.Seg.P == 0) {
            // borland extender (RTM)
            exception(ExceptionNP, selector & 0xfffc);
            return;
        }
        // commit point
        if (!use32) {
            push16(Register.segValue(Register.SEG_NAME_CS));
            push16(oldEIP);
            Register.setRegEIP(offset & 0xffff);
        } else {
            push32(Register.segValue(Register.SEG_NAME_CS));
            push32(oldEIP);
            Register.setRegEIP(offset);
        }
        Register.SegmentPhys[Register.SEG_NAME_CS] = call.getBase();
        Block.Code.Big = call.big() > 0;
        Register.SegmentVals[Register.SEG_NAME_CS] = (selector & 0xfffc) | Block.CPL;
        return;
    }

    // GotoDESC_CODE_R_C_NA
    private static void gotoDESC_CODE_R_C_NA(boolean use32, Descriptor call, int oldEIP,
            Descriptor n_cs_desc, int n_cs_sel, int n_eip) {
        if (call.type() == Descriptor.DESC_386_CALL_GATE) {
            push32(Register.segValue(Register.SEG_NAME_CS));
            push32(oldEIP);
        } else {
            push16(Register.segValue(Register.SEG_NAME_CS));
            push16(oldEIP);
        }

        /* Switch to new CS:EIP */
        Register.SegmentPhys[Register.SEG_NAME_CS] = n_cs_desc.getBase();
        Register.SegmentVals[Register.SEG_NAME_CS] = (n_cs_sel & 0xfffc) | Block.CPL;
        Block.Code.Big = n_cs_desc.big() > 0;
        Register.setRegEIP(n_eip);
        if (!use32)
            Register.setRegEIP(Register.getRegEIP() & 0xffff);
    }

    public static void ret(boolean use32, int bytes, int oldEIP) {
        if (!Block.PMode || (Register.Flags & Register.FlagVM) != 0) {
            int new_ip, new_cs;
            if (!use32) {
                new_ip = pop16();
                new_cs = pop16();
            } else {
                new_ip = pop32();
                new_cs = pop32() & 0xffff;
            }
            Register.setRegESP(Register.getRegESP() + bytes);
            Register.segSet16(Register.SEG_NAME_CS, new_cs);
            Register.setRegEIP(new_ip);
            Block.Code.Big = false;
            return;
        } else {
            int offset = 0, selector = 0;
            if (!use32)
                selector = Memory.readW(Register.segPhys(Register.SEG_NAME_SS)
                        + (Register.getRegESP() & Block.Stack.Mask) + 2);
            else
                selector = Memory.readD(Register.segPhys(Register.SEG_NAME_SS)
                        + (Register.getRegESP() & Block.Stack.Mask) + 4) & 0xffff;

            Descriptor desc = new Descriptor();
            int rpl = selector & 3;
            if (rpl < Block.CPL) {
                // win setup
                exception(ExceptionGP, selector & 0xfffc);
                return;
            }

            checkCPUCond((selector & 0xfffc) == 0, "RET:CS selector zero", ExceptionGP, 0);
            checkCPUCond(!Block.GDT.getDescriptor(selector, desc), "RET:CS beyond limits",
                    ExceptionGP, selector & 0xfffc);

            if (Block.CPL == rpl) {
                /* Return to same level */
                switch (desc.type()) {
                    case Descriptor.DESC_CODE_N_NC_A:
                    case Descriptor.DESC_CODE_N_NC_NA:
                    case Descriptor.DESC_CODE_R_NC_A:
                    case Descriptor.DESC_CODE_R_NC_NA:
                        checkCPUCond(Block.CPL != desc.dpl(),
                                "RET to NC segment of other privilege", ExceptionGP,
                                selector & 0xfffc);
                        // goto RET_same_level;
                        returnSameLevel(use32, desc, selector, offset, bytes, rpl);
                        return;
                    case Descriptor.DESC_CODE_N_C_A:
                    case Descriptor.DESC_CODE_N_C_NA:
                    case Descriptor.DESC_CODE_R_C_A:
                    case Descriptor.DESC_CODE_R_C_NA:
                        checkCPUCond(desc.dpl() > Block.CPL, "RET to C segment of higher privilege",
                                ExceptionGP, selector & 0xfffc);
                        break;
                    default:
                        Support.exceptionExit("RET from illegal descriptor type %X", desc.type());
                        break;
                }
                returnSameLevel(use32, desc, selector, offset, bytes, rpl);
                return;
                // RET_same_level:
                // if (desc.Saved.Seg.P == 0)
                // {
                // // borland extender (RTM)
                // Exception(ExceptionNP, selector & 0xfffc);
                // return;
                // }

                // // commit point
                // if (!use32)
                // {
                // offset = Pop16();
                // selector = Pop16();
                // }
                // else
                // {
                // offset = Pop32();
                // selector = Pop32() & 0xffff;
                // }

                // Register.SegmentPhys[(int)Register.SEG_NAME_CS] = desc.GetBase();
                // Block.Code.Big = desc.Big() > 0;
                // Register.SegmentVals[(int)Register.SEG_NAME_CS] = selector;
                // Register.setRegEIP(offset);
                // if (Block.Stack.Big)
                // {
                // Register.setRegESP(Register.getRegESP()+bytes);
                // }
                // else
                // {
                // Register.setRegSP(Register.getRegSP()+(short)bytes);
                // }
                // Log.Logging(Log.LogTypes.CPU, Log.LogServerities.Normal,"RET - Same level to
                // %X:%X RPL %X DPL %X", selector, offset, rpl, desc.DPL());
                // return;
            } else {
                /* Return to outer level */
                switch (desc.type()) {
                    case Descriptor.DESC_CODE_N_NC_A:
                    case Descriptor.DESC_CODE_N_NC_NA:
                    case Descriptor.DESC_CODE_R_NC_A:
                    case Descriptor.DESC_CODE_R_NC_NA:
                        checkCPUCond(desc.dpl() != rpl, "RET to outer NC segment with DPL!=RPL",
                                ExceptionGP, selector & 0xfffc);
                        break;
                    case Descriptor.DESC_CODE_N_C_A:
                    case Descriptor.DESC_CODE_N_C_NA:
                    case Descriptor.DESC_CODE_R_C_A:
                    case Descriptor.DESC_CODE_R_C_NA:
                        checkCPUCond(desc.dpl() > rpl, "RET to outer C segment with DPL>RPL",
                                ExceptionGP, selector & 0xfffc);
                        break;
                    default:
                        Support.exceptionExit("RET from illegal descriptor type %X", desc.type()); // or
                                                                                                   // #GP(selector)
                        break;
                }

                checkCPUCond(desc.Saved.Seg.P == 0, "RET:Outer level:CS not present", ExceptionNP,
                        selector & 0xfffc);

                // commit point
                int n_esp, n_ss;
                if (use32) {
                    offset = pop32();
                    selector = pop32() & 0xffff;
                    Register.setRegESP(Register.getRegESP() + bytes);
                    n_esp = pop32();
                    n_ss = pop32() & 0xffff;
                } else {
                    offset = pop16();
                    selector = pop16();
                    Register.setRegESP(Register.getRegESP() + bytes);
                    n_esp = pop16();
                    n_ss = pop16();
                }

                checkCPUCond((n_ss & 0xfffc) == 0, "RET to outer level with SS selector zero",
                        ExceptionGP, 0);

                Descriptor n_ss_desc = new Descriptor();
                checkCPUCond(!Block.GDT.getDescriptor(n_ss, n_ss_desc), "RET:SS beyond limits",
                        ExceptionGP, n_ss & 0xfffc);

                checkCPUCond(((n_ss & 3) != rpl) || (n_ss_desc.dpl() != rpl),
                        "RET to outer segment with invalid SS privileges", ExceptionGP,
                        n_ss & 0xfffc);
                switch (n_ss_desc.type()) {
                    case Descriptor.DESC_DATA_EU_RW_NA:
                    case Descriptor.DESC_DATA_EU_RW_A:
                    case Descriptor.DESC_DATA_ED_RW_NA:
                    case Descriptor.DESC_DATA_ED_RW_A:
                        break;
                    default:
                        Support.exceptionExit("RET:SS selector type no writable data segment"); // or
                                                                                                // #GP(selector)
                        break;
                }
                checkCPUCond(n_ss_desc.Saved.Seg.P == 0, "RET:Stack segment not present",
                        ExceptionSS, n_ss & 0xfffc);

                Block.CPL = rpl;
                Register.SegmentPhys[Register.SEG_NAME_CS] = desc.getBase();
                Block.Code.Big = desc.big() > 0;
                Register.SegmentVals[Register.SEG_NAME_CS] = (selector & 0xfffc) | Block.CPL;
                Register.setRegEIP(offset);

                Register.SegmentVals[Register.SEG_NAME_SS] = n_ss;
                Register.SegmentPhys[Register.SEG_NAME_SS] = n_ss_desc.getBase();
                if (n_ss_desc.big() != 0) {
                    Block.Stack.Big = true;
                    Block.Stack.Mask = 0xffffffff;
                    Block.Stack.NotMask = 0;
                    Register.setRegESP(n_esp + bytes);
                } else {
                    Block.Stack.Big = false;
                    Block.Stack.Mask = 0xffff;
                    Block.Stack.NotMask = 0xffff0000;
                    Register.setRegSP((n_esp & 0xffff) + bytes);
                }

                checkSegments();

                // Log.Logging(Log.LOG_TYPES.LOG_MISC Log.LOG_SEVERITIES.LOG_ERROR,"RET - Higher
                // level to %X:%X RPL %X DPL %X",selector,offset,rpl,desc.DPL);
                return;
            }
            /*
             * Log.Logging(Log.LogTypes.CPU, Log.LogServerities.Normal,"Prot ret %X:%X", selector,
             * offset); return;
             */
        }
        // assert(1);
    }

    // RET_same_level
    private static void returnSameLevel(boolean use32, Descriptor desc, int selector, int offset,
            int bytes, int rpl) {
        if (desc.Saved.Seg.P == 0) {
            // borland extender (RTM)
            exception(ExceptionNP, selector & 0xfffc);
            return;
        }

        // commit point
        if (!use32) {
            offset = pop16();
            selector = pop16();
        } else {
            offset = pop32();
            selector = pop32() & 0xffff;
        }

        Register.SegmentPhys[Register.SEG_NAME_CS] = desc.getBase();
        Block.Code.Big = desc.big() > 0;
        Register.SegmentVals[Register.SEG_NAME_CS] = selector;
        Register.setRegEIP(offset);
        if (Block.Stack.Big) {
            Register.setRegESP(Register.getRegESP() + bytes);
        } else {
            Register.setRegSP(Register.getRegSP() + (0xffff & bytes));
        }
        Log.logging(Log.LogTypes.CPU, Log.LogServerities.Normal,
                "RET - Same level to %X:%X RPL %X DPL %X", selector, offset, rpl, desc.dpl());
        return;
    }

    public static int sldt() {
        return Block.GDT.sldt();
    }

    public static boolean lldt(int selector) {
        if (!Block.GDT.lldt(selector)) {
            Log.logging(Log.LogTypes.CPU, Log.LogServerities.Error, "LLDT failed, selector=%X",
                    selector);
            return true;
        }
        Log.logging(Log.LogTypes.CPU, Log.LogServerities.Normal, "LDT Set to %X", selector);
        return false;
    }

    public static int str() {
        return _cpuTss.Selector;
    }

    public static boolean ltr(int selector) {
        if ((selector & 0xfffc) == 0) {
            _cpuTss.setSelector(selector);
            return false;
        }
        Descriptor desc = new Descriptor();
        if ((selector & 4) != 0 || (!Block.GDT.getDescriptor(selector, desc))) {
            Log.logging(Log.LogTypes.CPU, Log.LogServerities.Error, "LTR failed, selector=%X",
                    selector);
            return prepareException(ExceptionGP, selector);
        }

        if ((desc.type() == Descriptor.DESC_286_TSS_A)
                || (desc.type() == Descriptor.DESC_386_TSS_A)) {
            if (desc.Saved.Seg.P == 0) {
                Log.logging(Log.LogTypes.CPU, Log.LogServerities.Error,
                        "LTR failed, selector=%X (not present)", selector);
                return prepareException(ExceptionNP, selector);
            }
            if (!_cpuTss.setSelector(selector))
                Support.exceptionExit("LTR failed, selector=%X", selector);
            _cpuTss.Desc.setBusy(true);
            _cpuTss.saveSelector();
        } else {
            /* Descriptor was no available TSS descriptor */
            Log.logging(Log.LogTypes.CPU, Log.LogServerities.Normal,
                    "LTR failed, selector=%X (type=%X)", selector, desc.type());
            return prepareException(ExceptionGP, selector);
        }
        return false;
    }

    public static void lgdt(int limit, int baseAddress) {
        Log.logging(Log.LogTypes.CPU, Log.LogServerities.Normal, "GDT Set to base:%X limit:%X",
                baseAddress, limit);
        Block.GDT.setLimit(limit);
        Block.GDT.setBase(baseAddress);
    }

    public static void lidt(int limit, int baseAddress) {
        Log.logging(Log.LogTypes.CPU, Log.LogServerities.Normal, "IDT Set to base:%X limit:%X",
                baseAddress, limit);
        Block.IDT.setLimit(limit);
        Block.IDT.setBase(baseAddress);
    }

    public static int sgdtBase() {
        return Block.GDT.getBase();
    }

    public static int sgdtLimit() {
        return Block.GDT.getLimit();
    }

    public static int sidtBase() {
        return Block.IDT.getBase();
    }

    public static int sidtLimit() {
        return Block.IDT.getLimit();
    }

    private static boolean printed_cycles_auto_info = false;

    public static void setCRX(int cr, int value) {
        switch (cr) {
            case 0: {
                int changed = Block.CR0 ^ value;
                if (changed == 0)
                    return;
                Block.CR0 = value;
                if ((value & CR0_PROTECTION) != 0) {
                    Block.PMode = true;
                    Log.logging(Log.LogTypes.CPU, Log.LogServerities.Normal, "Protected mode");
                    Paging.enable((value & CR0_PAGING) > 0);

                    if ((AutoDetermineMode & AutoDetermineMask) == 0)
                        break;

                    if ((AutoDetermineMode & AutoDetermineCycles) != 0) {
                        CycleAutoAdjust = true;
                        CycleLeft = 0;
                        Cycles = 0;
                        OldCycleMax = CycleMax;
                        GUIPlatform.gfx.setTitle(CyclePercUsed, -1, false);
                        if (!printed_cycles_auto_info) {
                            printed_cycles_auto_info = true;
                            Log.logMsg(
                                    "DOSBox switched to max cycles, because of the setting: cycles=auto. If the game runs too fast try a fixed cycles amount in DOSBox's options.");
                        }
                    } else {
                        GUIPlatform.gfx.setTitle(-1, -1, false);
                    }

                    AutoDetermineMode <<= AutoDetermineShift;
                } else {
                    Block.PMode = false;
                    if ((value & CR0_PAGING) != 0)
                        Log.logMsg("Paging requested without PE=1");
                    Paging.enable(false);
                    Log.logging(Log.LogTypes.CPU, Log.LogServerities.Normal, "Real mode");
                }
                break;
            }
            case 2:
                Paging.paging.CR2 = value;
                break;
            case 3:
                Paging.setDirBase(value);
                break;
            default:
                Log.logging(Log.LogTypes.CPU, Log.LogServerities.Error, "Unhandled MOV CR%d,%X", cr,
                        value);
                break;
        }
    }

    public static boolean writeCRX(int cr, int value) {
        /* Check if privileged to access control registers */
        if (Block.PMode && (Block.CPL > 0))
            return prepareException(ExceptionGP, 0);
        if ((cr == 1) || (cr > 4))
            return prepareException(ExceptionUD, 0);
        if (ArchitectureType < ArchType486OldSlow) {
            if (cr == 4)
                return prepareException(ExceptionUD, 0);
        }
        setCRX(cr, value);
        return false;
    }

    public static int getCRX(int cr) {
        switch (cr) {
            case 0:
                if (ArchitectureType >= ArchTypePentiumSlow)
                    return Block.CR0;
                else if (ArchitectureType >= ArchType486OldSlow)
                    return (Block.CR0 & 0xe005003f);
                else
                    return (Block.CR0 | 0x7ffffff0);
            case 2:
                return (int) Paging.paging.CR2;
            case 3:
                return Paging.getDirBase() & 0xfffff000;
            default:
                Log.logging(Log.LogTypes.CPU, Log.LogServerities.Error, "Unhandled MOV XXX, CR%d",
                        cr);
                break;
        }
        return 0;
    }

    // if success, return -1
    // if false, return long type value
    // int로 변환해서 사용
    public static long readCRX(int cr) {
        /* Check if privileged to access control registers */
        if (Block.PMode && (Block.CPL > 0)) {
            prepareException(ExceptionGP, 0);
            return -1;
        }
        if ((cr == 1) || (cr > 4)) {
            prepareException(ExceptionUD, 0);
            return -1;
        }
        return getCRX(cr);
        // return false;
    }

    public static boolean writeDRX(int dr, int value) {
        /* Check if privileged to access control registers */
        if (Block.PMode && (Block.CPL > 0))
            return prepareException(ExceptionGP, 0);
        switch (dr) {
            case 0:
            case 1:
            case 2:
            case 3:
                Block.DRX[dr] = value;
                break;
            case 4:
            case 6:
                Block.DRX[6] = (value | 0xffff0ff0) & 0xffffefff;
                break;
            case 5:
            case 7:
                if (ArchitectureType < ArchTypePentiumSlow) {
                    Block.DRX[7] = (value | 0x400) & 0xffff2fff;
                } else {
                    Block.DRX[7] = (value | 0x400);
                }
                break;
            default:
                Log.logging(Log.LogTypes.CPU, Log.LogServerities.Error, "Unhandled MOV DR%d,%X", dr,
                        value);
                break;
        }
        return false;
    }

    // if success, return -1
    // if false, return long type value
    // int로 변환해서 사용
    public static long readDRX(int dr) {
        long retVal = -1;
        /* Check if privileged to access control registers */
        if (Block.PMode && (Block.CPL > 0)) {
            prepareException(ExceptionGP, 0);
            return -1;
        }
        switch (dr) {
            case 0:
            case 1:
            case 2:
            case 3:
            case 6:
            case 7:
                retVal = Block.DRX[dr];
                break;
            case 4:
                retVal = Block.DRX[6];
                break;
            case 5:
                retVal = Block.DRX[7];
                break;
            default:
                Log.logging(Log.LogTypes.CPU, Log.LogServerities.Error, "Unhandled MOV XXX, DR%d",
                        dr);
                retVal = 0;
                break;
        }
        return retVal;
        // return false;
    }

    public static boolean writeTRX(int tr, int value) {
        /* Check if privileged to access control registers */
        if (Block.PMode && (Block.CPL > 0))
            return prepareException(ExceptionGP, 0);
        switch (tr) {
            // case 3:
            case 6:
            case 7:
                Block.TRX[tr] = value;
                return false;
            default:
                Log.logging(Log.LogTypes.CPU, Log.LogServerities.Error, "Unhandled MOV TR%d,%X", tr,
                        value);
                break;
        }
        return prepareException(ExceptionUD, 0);
    }

    // if success, return -1
    // if false, return long type value
    // int로 변환해서 사용
    public static long readTRX(int tr) {
        /* Check if privileged to access control registers */
        if (Block.PMode && (Block.CPL > 0)) {
            prepareException(ExceptionGP, 0);
            return -1;
        }
        switch (tr) {
            // case 3:
            case 6:
            case 7:
                return Block.TRX[tr];
            // return false;
            default:
                Log.logging(Log.LogTypes.CPU, Log.LogServerities.Error, "Unhandled MOV XXX, TR%d",
                        tr);
                break;
        }
        prepareException(ExceptionUD, 0);
        return -1;
    }

    public static int smsw() {
        return Block.CR0;
    }

    public static boolean lmsw(int word) {
        if (Block.PMode && (Block.CPL > 0))
            return prepareException(ExceptionGP, 0);
        word &= 0xf;
        if ((Block.CR0 & 1) != 0)
            word |= 1;
        word |= (Block.CR0 & 0xfffffff0);
        setCRX(0, word);
        return false;
    }

    public static int arpl(int dest_sel, int src_sel) {
        Flags.fillFlags();
        if ((dest_sel & 3) < (src_sel & 3)) {
            dest_sel = (dest_sel & 0xfffc) + (src_sel & 3);
            // dest_sel|=0xff3f0000;
            Register.setFlagBit(Register.FlagZF, true);
        } else {
            Register.setFlagBit(Register.FlagZF, false);
        }
        return dest_sel;
    }

    // if success, return ar
    // if false, return -1
    // int로 변환해서 사용
    public static int lar(int selector, int ar) {
        Flags.fillFlags();
        if (selector == 0) {
            Register.setFlagBit(Register.FlagZF, false);
            return -1;
        }
        Descriptor desc = new Descriptor();
        int rpl = selector & 3;
        if (!Block.GDT.getDescriptor(selector, desc)) {
            Register.setFlagBit(Register.FlagZF, false);
            return -1;
        }
        switch (desc.type()) {
            case Descriptor.DESC_CODE_N_C_A:
            case Descriptor.DESC_CODE_N_C_NA:
            case Descriptor.DESC_CODE_R_C_A:
            case Descriptor.DESC_CODE_R_C_NA:
                break;
            // TODO: dosbox 소스 분석 필요, case 문 사용이 이상함
            case Descriptor.DESC_286_INT_GATE:
            case Descriptor.DESC_286_TRAP_GATE:
            case Descriptor.DESC_386_INT_GATE:
            case Descriptor.DESC_386_TRAP_GATE:
                Register.setFlagBit(Register.FlagZF, false);
                return -1;

            case Descriptor.DESC_LDT:
            case Descriptor.DESC_TASK_GATE:

            case Descriptor.DESC_286_TSS_A:
            case Descriptor.DESC_286_TSS_B:
            case Descriptor.DESC_286_CALL_GATE:

            case Descriptor.DESC_386_TSS_A:
            case Descriptor.DESC_386_TSS_B:
            case Descriptor.DESC_386_CALL_GATE:

            case Descriptor.DESC_DATA_EU_RO_NA:
            case Descriptor.DESC_DATA_EU_RO_A:
            case Descriptor.DESC_DATA_EU_RW_NA:
            case Descriptor.DESC_DATA_EU_RW_A:
            case Descriptor.DESC_DATA_ED_RO_NA:
            case Descriptor.DESC_DATA_ED_RO_A:
            case Descriptor.DESC_DATA_ED_RW_NA:
            case Descriptor.DESC_DATA_ED_RW_A:
            case Descriptor.DESC_CODE_N_NC_A:
            case Descriptor.DESC_CODE_N_NC_NA:
            case Descriptor.DESC_CODE_R_NC_A:
            case Descriptor.DESC_CODE_R_NC_NA:
                if (desc.dpl() < Block.CPL || desc.dpl() < rpl) {
                    Register.setFlagBit(Register.FlagZF, false);
                    return -1;
                }
                break;
            default:
                Register.setFlagBit(Register.FlagZF, false);
                return -1;
        }
        /* Valid descriptor */
        ar = desc.Saved.Fill[1] & 0x00ffff00;
        Register.setFlagBit(Register.FlagZF, true);
        return ar;
    }

    // if false, return -1
    public static int lsl(int selector) {
        int limit;
        Flags.fillFlags();
        if (selector == 0) {
            Register.setFlagBit(Register.FlagZF, false);
            return -1;
        }
        Descriptor desc = new Descriptor();
        int rpl = selector & 3;
        if (!Block.GDT.getDescriptor(selector, desc)) {
            Register.setFlagBit(Register.FlagZF, false);
            return -1;
        }
        switch (desc.type()) {
            case Descriptor.DESC_CODE_N_C_A:
            case Descriptor.DESC_CODE_N_C_NA:
            case Descriptor.DESC_CODE_R_C_A:
            case Descriptor.DESC_CODE_R_C_NA:
                break;

            case Descriptor.DESC_LDT:
            case Descriptor.DESC_286_TSS_A:
            case Descriptor.DESC_286_TSS_B:

            case Descriptor.DESC_386_TSS_A:
            case Descriptor.DESC_386_TSS_B:

            case Descriptor.DESC_DATA_EU_RO_NA:
            case Descriptor.DESC_DATA_EU_RO_A:
            case Descriptor.DESC_DATA_EU_RW_NA:
            case Descriptor.DESC_DATA_EU_RW_A:
            case Descriptor.DESC_DATA_ED_RO_NA:
            case Descriptor.DESC_DATA_ED_RO_A:
            case Descriptor.DESC_DATA_ED_RW_NA:
            case Descriptor.DESC_DATA_ED_RW_A:

            case Descriptor.DESC_CODE_N_NC_A:
            case Descriptor.DESC_CODE_N_NC_NA:
            case Descriptor.DESC_CODE_R_NC_A:
            case Descriptor.DESC_CODE_R_NC_NA:
                if (desc.dpl() < Block.CPL || desc.dpl() < rpl) {
                    Register.setFlagBit(Register.FlagZF, false);
                    return -1;
                }
                break;
            default:
                Register.setFlagBit(Register.FlagZF, false);
                return -1;
        }
        limit = desc.getLimit();
        Register.setFlagBit(Register.FlagZF, true);
        return limit;
    }

    public static void verr(int selector) {
        Flags.fillFlags();
        if (selector == 0) {
            Register.setFlagBit(Register.FlagZF, false);
            return;
        }
        Descriptor desc = new Descriptor();
        int rpl = selector & 3;
        if (!Block.GDT.getDescriptor(selector, desc)) {
            Register.setFlagBit(Register.FlagZF, false);
            return;
        }
        switch (desc.type()) {
            case Descriptor.DESC_CODE_R_C_A:
            case Descriptor.DESC_CODE_R_C_NA:
                // Conforming readable code segments can be always read
                break;
            case Descriptor.DESC_DATA_EU_RO_NA:
            case Descriptor.DESC_DATA_EU_RO_A:
            case Descriptor.DESC_DATA_EU_RW_NA:
            case Descriptor.DESC_DATA_EU_RW_A:
            case Descriptor.DESC_DATA_ED_RO_NA:
            case Descriptor.DESC_DATA_ED_RO_A:
            case Descriptor.DESC_DATA_ED_RW_NA:
            case Descriptor.DESC_DATA_ED_RW_A:

            case Descriptor.DESC_CODE_R_NC_A:
            case Descriptor.DESC_CODE_R_NC_NA:
                if (desc.dpl() < Block.CPL || desc.dpl() < rpl) {
                    Register.setFlagBit(Register.FlagZF, false);
                    return;
                }
                break;
            default:
                Register.setFlagBit(Register.FlagZF, false);
                return;
        }
        Register.setFlagBit(Register.FlagZF, true);
    }

    public static void verw(int selector) {
        Flags.fillFlags();
        if (selector == 0) {
            Register.setFlagBit(Register.FlagZF, false);
            return;
        }
        Descriptor desc = new Descriptor();
        int rpl = selector & 3;
        if (!Block.GDT.getDescriptor(selector, desc)) {
            Register.setFlagBit(Register.FlagZF, false);
            return;
        }
        switch (desc.type()) {
            case Descriptor.DESC_DATA_EU_RW_NA:
            case Descriptor.DESC_DATA_EU_RW_A:
            case Descriptor.DESC_DATA_ED_RW_NA:
            case Descriptor.DESC_DATA_ED_RW_A:
                if (desc.dpl() < Block.CPL || desc.dpl() < rpl) {
                    Register.setFlagBit(Register.FlagZF, false);
                    return;
                }
                break;
            default:
                Register.setFlagBit(Register.FlagZF, false);
                return;
        }
        Register.setFlagBit(Register.FlagZF, true);
    }

    // public static boolean setSegGeneral(byte seg, int value)
    public static boolean setSegGeneral(int seg, int value) {
        value &= 0xffff;
        if (!Block.PMode || (Register.Flags & Register.FlagVM) != 0) {
            Register.SegmentVals[seg] = value;
            Register.SegmentPhys[seg] = value << 4;
            if (seg == Register.SEG_NAME_SS) {
                Block.Stack.Big = false;
                Block.Stack.Mask = 0xffff;
                Block.Stack.NotMask = 0xffff0000;
            }
            return false;
        } else {
            if (seg == Register.SEG_NAME_SS) {
                // Stack needs to be non-zero
                if ((value & 0xfffc) == 0) {
                    Support.exceptionExit("CPU_SetSegGeneral: Stack segment zero");
                    // return CPU_PrepareException(EXCEPTION_GP,0);
                }
                Descriptor desc = new Descriptor();
                if (!Block.GDT.getDescriptor(value, desc)) {
                    Support.exceptionExit("CPU_SetSegGeneral: Stack segment beyond limits");
                    // return CPU_PrepareException(EXCEPTION_GP,value & 0xfffc);
                }
                if (((value & 3) != Block.CPL) || (desc.dpl() != Block.CPL)) {
                    Support.exceptionExit(
                            "CPU_SetSegGeneral: Stack segment with invalid privileges");
                    // return CPU_PrepareException(EXCEPTION_GP,value & 0xfffc);
                }

                switch (desc.type()) {
                    case Descriptor.DESC_DATA_EU_RW_NA:
                    case Descriptor.DESC_DATA_EU_RW_A:
                    case Descriptor.DESC_DATA_ED_RW_NA:
                    case Descriptor.DESC_DATA_ED_RW_A:
                        break;
                    default:
                        // Earth Siege 1
                        return prepareException(ExceptionGP, value & 0xfffc);
                }

                if (desc.Saved.Seg.P == 0) {
                    // E_Exit("CPU_SetSegGeneral: Stack segment not present"); // or #SS(sel)
                    return prepareException(ExceptionSS, value & 0xfffc);
                }

                Register.SegmentVals[seg] = value;
                Register.SegmentPhys[seg] = desc.getBase();
                if (desc.big() != 0) {
                    Block.Stack.Big = true;
                    Block.Stack.Mask = 0xffffffff;
                    Block.Stack.NotMask = 0;
                } else {
                    Block.Stack.Big = false;
                    Block.Stack.Mask = 0xffff;
                    Block.Stack.NotMask = 0xffff0000;
                }
            } else {
                if ((value & 0xfffc) == 0) {
                    Register.SegmentVals[seg] = value;
                    Register.SegmentPhys[seg] = 0; // ??
                    return false;
                }
                Descriptor desc = new Descriptor();
                if (!Block.GDT.getDescriptor(value, desc)) {
                    return prepareException(ExceptionGP, value & 0xfffc);
                }
                switch (desc.type()) {
                    case Descriptor.DESC_DATA_EU_RO_NA:
                    case Descriptor.DESC_DATA_EU_RO_A:
                    case Descriptor.DESC_DATA_EU_RW_NA:
                    case Descriptor.DESC_DATA_EU_RW_A:
                    case Descriptor.DESC_DATA_ED_RO_NA:
                    case Descriptor.DESC_DATA_ED_RO_A:
                    case Descriptor.DESC_DATA_ED_RW_NA:
                    case Descriptor.DESC_DATA_ED_RW_A:
                    case Descriptor.DESC_CODE_R_NC_A:
                    case Descriptor.DESC_CODE_R_NC_NA:
                        if (((value & 3) > desc.dpl()) || (Block.CPL > desc.dpl())) {
                            // extreme pinball
                            return prepareException(ExceptionGP, value & 0xfffc);
                        }
                        break;
                    case Descriptor.DESC_CODE_R_C_A:
                    case Descriptor.DESC_CODE_R_C_NA:
                        break;
                    default:
                        // gabriel knight
                        return prepareException(ExceptionGP, value & 0xfffc);

                }
                if (desc.Saved.Seg.P == 0) {
                    // win
                    return prepareException(ExceptionNP, value & 0xfffc);
                }

                Register.SegmentVals[seg] = value;
                Register.SegmentPhys[seg] = desc.getBase();
            }

            return false;
        }
    }

    // public static boolean popSeg(byte seg, boolean use32)
    public static boolean popSeg(int seg, boolean use32) {
        int val = Memory.readW(
                Register.segPhys(Register.SEG_NAME_SS) + (Register.getRegESP() & Block.Stack.Mask));
        if (setSegGeneral(seg, val))
            return true;
        int addsp = use32 ? 0x04 : 0x02;
        Register.setRegESP((Register.getRegESP() & Block.Stack.NotMask)
                | ((Register.getRegESP() + addsp) & Block.Stack.Mask));
        return false;
    }

    public static boolean cpuId() {
        if (ArchitectureType < ArchType486NewSlow)
            return false;
        switch (Register.getRegEAX()) {
            case 0: /* Vendor ID String and maximum level? */
                Register.setRegEAX(1); /* Maximum level */
                Register.setRegEBX('G' | ('e' << 8) | ('n' << 16) | ('u' << 24));
                Register.setRegEDX('i' | ('n' << 8) | ('e' << 16) | ('I' << 24));
                Register.setRegECX('n' | ('t' << 8) | ('e' << 16) | ('l' << 24));
                break;
            case 1: /* get processor type/family/model/stepping and feature flags */
                if ((ArchitectureType == ArchType486NewSlow)
                        || (ArchitectureType == ArchTypeMixed)) {
                    Register.setRegEAX(0x402); /* intel 486dx */
                    Register.setRegEBX(0); /* Not Supported */
                    Register.setRegECX(0); /* No features */
                    Register.setRegEDX(0x00000001); /* FPU */
                } else if (ArchitectureType == ArchTypePentiumSlow) {
                    Register.setRegEAX(0x513); /* intel pentium */
                    Register.setRegEBX(0); /* Not Supported */
                    Register.setRegECX(0); /* No features */
                    Register.setRegEDX(0x00000011); /* FPU+TimeStamp/RDTSC */
                } else {
                    return false;
                }
                break;
            default:
                Log.logging(Log.LogTypes.CPU, Log.LogServerities.Error,
                        "Unhandled CPUID Function %x", Register.getRegEAX());
                Register.setRegEAX(0);
                Register.setRegEBX(0);
                Register.setRegECX(0);
                Register.setRegEDX(0);
                break;
        }
        return true;
    }

    private static int hltDecode() {
        /* Once an interrupt occurs, it should change cpu core */
        if (Register.getRegEIP() != Block.HLTObj.EIP
                || Register.segValue(Register.SEG_NAME_CS) != Block.HLTObj.CS) {
            CpuDecoder = Block.HLTObj.OldDecoder;
        } else {
            Cycles = 0;
        }
        return 0;
    }

    public static void hlt(int oldEIP) {
        Register.setRegEIP(oldEIP);
        Cycles = 0;
        Block.HLTObj.CS = Register.segValue(Register.SEG_NAME_CS);
        Block.HLTObj.EIP = Register.getRegEIP();
        Block.HLTObj.OldDecoder = CpuDecoder;
        CpuDecoder = CPU.HLTDecoder;
    }

    public static void enter(boolean use32, int bytes, int level) {
        level &= 0x1f;
        int sp_index = Register.getRegESP() & Block.Stack.Mask;
        int bp_index = Register.getRegEBP() & Block.Stack.Mask;
        if (!use32) {
            sp_index -= 2;
            Memory.writeW(Register.segPhys(Register.SEG_NAME_SS) + sp_index, Register.getRegBP());
            Register.setRegBP(Register.getRegESP() - 2);
            if (level != 0) {
                for (int i = 1; i < level; i++) {
                    sp_index -= 2;
                    bp_index -= 2;
                    Memory.writeW(Register.segPhys(Register.SEG_NAME_SS) + sp_index,
                            Memory.readW(Register.segPhys(Register.SEG_NAME_SS) + bp_index));
                }
                sp_index -= 2;
                Memory.writeW(Register.segPhys(Register.SEG_NAME_SS) + sp_index,
                        Register.getRegBP());
            }
        } else {
            sp_index -= 4;
            Memory.writeD(Register.segPhys(Register.SEG_NAME_SS) + sp_index, Register.getRegEBP());
            Register.setRegEBP((Register.getRegESP() - 4));
            if (level != 0) {
                for (int i = 1; i < level; i++) {
                    sp_index -= 4;
                    bp_index -= 4;
                    Memory.writeD(Register.segPhys(Register.SEG_NAME_SS) + sp_index,
                            Memory.readD(Register.segPhys(Register.SEG_NAME_SS) + bp_index));
                }
                sp_index -= 4;
                Memory.writeD(Register.segPhys(Register.SEG_NAME_SS) + sp_index,
                        Register.getRegEBP());
            }
        }
        sp_index -= bytes;
        Register.setRegESP(
                (Register.getRegESP() & Block.Stack.NotMask) | ((sp_index) & Block.Stack.Mask));
    }

    public static void cycleIncrease(boolean pressed) {
        if (!pressed)
            return;
        if (CycleAutoAdjust) {
            CyclePercUsed += 5;
            if (CyclePercUsed > 105)
                CyclePercUsed = 105;
            Log.logMsg("CPU speed: max %d percent.", CyclePercUsed);
            GUIPlatform.gfx.setTitle(CyclePercUsed, -1, false);
        } else {
            int old_cycles = CycleMax;
            if (CycleUp < 100) {
                CycleMax = (int) (CycleMax * (1 + (float) CycleUp / 100.0));
            } else {
                CycleMax = (int) (CycleMax + CycleUp);
            }

            CycleLeft = 0;
            Cycles = 0;
            if (CycleMax == old_cycles)
                CycleMax++;
            if (CycleMax > 15000)
                Log.logMsg(
                        "CPU speed: fixed %d cycles. If you need more than 20000, try core=dynamic in DOSBox's options.",
                        CycleMax);
            else
                Log.logMsg("CPU speed: fixed %d cycles.", CycleMax);
            GUIPlatform.gfx.setTitle(CycleMax, -1, false);
        }
    }

    public static void cycleDecrease(boolean pressed) {
        if (!pressed)
            return;
        if (CycleAutoAdjust) {
            CyclePercUsed -= 5;
            if (CyclePercUsed <= 0)
                CyclePercUsed = 1;
            if (CyclePercUsed <= 70)
                Log.logMsg(
                        "CPU speed: max %d percent. If the game runs too fast, try a fixed cycles amount in DOSBox's options.",
                        CyclePercUsed);
            else
                Log.logMsg("CPU speed: max %d percent.", CyclePercUsed);
            GUIPlatform.gfx.setTitle(CyclePercUsed, -1, false);
        } else {
            if (CycleDown < 100) {
                CycleMax = (int) (CycleMax / (1 + (float) CycleDown / 100.0));
            } else {
                CycleMax = (int) (CycleMax - CycleDown);
            }
            CycleLeft = 0;
            Cycles = 0;
            if (CycleMax <= 0)
                CycleMax = 1;
            Log.logMsg("CPU speed: fixed %d cycles.", CycleMax);
            GUIPlatform.gfx.setTitle(CycleMax, -1, false);
        }
    }

    public static void enableSkipAutoAdjust() {
        if (CycleAutoAdjust) {
            CycleMax /= 2;
            if (CycleMax < CyclesLowerLimit)
                CycleMax = CyclesLowerLimit;
        }
        SkipCycleAutoAdjust = true;
    }

    public static void disableSkipAutoAdjust() {
        SkipCycleAutoAdjust = false;
    }

    public static void resetAutoAdjust() {
        IODelayRemoved = 0;
        DOSBox._ticksDone = 0;
        DOSBox._ticksScheduled = 0;
    }

    public static boolean ioException(int port, int size) {
        if (Block.PMode && ((Register.getFlagIOPL() < Block.CPL)
                || Register.getFlag(Register.FlagVM) != 0)) {
            Block.MPL = 0;
            if (!_cpuTss.Is386)

                return doException(port);// goto doexception;
            int bwhere = _cpuTss.BaseAddr + 0x66;
            int ofs = Memory.readW(bwhere);
            if (ofs > _cpuTss.Limit)
                return doException(port);// goto doexception;

            bwhere = _cpuTss.BaseAddr + ofs + (port / 8);
            int map = Memory.readW(bwhere);
            int mask = (0xffff >>> (16 - size)) << (port & 7);
            if ((map & mask) != 0)
                return doException(port);// goto doexception;
            Block.MPL = 3;
        }
        return false;

        // doexception:
        // Block.MPL = 3;
        // Log.Logging(Log.LogTypes.CPU, Log.LogServerities.Normal,"IO Exception port
        // %X", port);
        // return PrepareException(ExceptionGP, 0);
    }

    // doexception
    private static boolean doException(int port) {
        Block.MPL = 3;
        Log.logging(Log.LogTypes.CPU, Log.LogServerities.Normal, "IO Exception port %X", port);
        return prepareException(ExceptionGP, 0);
    }

    public static void exception(int which, int error) {
        // Log.LOG_MSG("Exception %d error %x",which,error);
        Block.Exception.Error = error;
        interrupt(which, CPU_INT_EXCEPTION | ((which >= 8) ? CPU_INT_HAS_ERROR : 0),
                Register.getRegEIP());
    }

    public static void exception(int which) {
        exception(which, 0);
    }

    // -- #region Interrupt
    public static int LastInt;

    public static void interrupt(int num, int type, int oldeip) {
        LastInt = num;
        Flags.fillFlags();

        if (!Block.PMode) {
            /* Save everything on a 16-bit stack */
            push16(Register.Flags & 0xffff);
            push16(Register.segValue(Register.SEG_NAME_CS));
            push16(oldeip);
            Register.setFlagBit(Register.FlagIF, false);
            Register.setFlagBit(Register.FlagTF, false);
            /* Get the new CS:IP from vector table */
            int baseAddr = Block.IDT.getBase();
            Register.setRegEIP(Memory.readW(baseAddr + (num << 2)));
            Register.SegmentVals[Register.SEG_NAME_CS] = Memory.readW(baseAddr + (num << 2) + 2);
            Register.SegmentPhys[Register.SEG_NAME_CS] =
                    Register.SegmentVals[Register.SEG_NAME_CS] << 4;
            Block.Code.Big = false;
            return;
        } else {
            /* Protected Mode Interrupt */
            if ((Register.Flags & Register.FlagVM) != 0 && (type & CPU_INT_SOFTWARE) != 0
                    && (type & CPU_INT_NOIOPLCHECK) == 0) {
                // Log.LOG_MSG("Software int in v86, AH %X IOPL %x",reg_ah,(reg_flags &
                // FLAG_IOPL) >>12);
                if ((Register.Flags & Register.FlagIOPL) != Register.FlagIOPL) {
                    exception(ExceptionGP, 0);
                    return;
                }
            }

            Descriptor gate = new Descriptor();
            if (!Block.IDT.getDescriptor(num << 3, gate)) {
                // zone66
                exception(ExceptionGP, (num * 8 + 2 + (type & CPU_INT_SOFTWARE)) != 0 ? 0 : 1);
                return;
            }

            if ((type & CPU_INT_SOFTWARE) != 0 && (gate.dpl() < Block.CPL)) {
                // zone66, win3.x e
                exception(ExceptionGP, num * 8 + 2);
                return;
            }

            switch (gate.type()) {
                case Descriptor.DESC_286_INT_GATE:
                case Descriptor.DESC_386_INT_GATE:
                case Descriptor.DESC_286_TRAP_GATE:
                case Descriptor.DESC_386_TRAP_GATE: {
                    checkCPUCond(gate.Saved.Seg.P == 0, "INT:Gate segment not present", ExceptionNP,
                            (num * 8 + 2 + (type & CPU_INT_SOFTWARE)) != 0 ? 0 : 1);

                    Descriptor csDesc = new Descriptor();
                    int gateSel = gate.getSelector();
                    int gateOff = gate.getOffset();
                    checkCPUCond((gateSel & 0xfffc) == 0, "INT:Gate with CS zero selector",
                            ExceptionGP, (type & CPU_INT_SOFTWARE) != 0 ? 0 : 1);
                    checkCPUCond(!Block.GDT.getDescriptor(gateSel, csDesc),
                            "INT:Gate with CS beyond limit", ExceptionGP,
                            ((gateSel & 0xfffc) + (type & CPU_INT_SOFTWARE)) != 0 ? 0 : 1);

                    int csDPL = csDesc.dpl();
                    checkCPUCond(csDPL > Block.CPL, "Interrupt to higher privilege", ExceptionGP,
                            ((gateSel & 0xfffc) + (type & CPU_INT_SOFTWARE)) != 0 ? 0 : 1);
                    switch (csDesc.type()) {
                        case Descriptor.DESC_CODE_N_NC_A:
                        case Descriptor.DESC_CODE_N_NC_NA:
                        case Descriptor.DESC_CODE_R_NC_A:
                        case Descriptor.DESC_CODE_R_NC_NA:
                            if (csDPL < Block.CPL) {
                                /* Prepare for gate to inner level */
                                checkCPUCond(csDesc.Saved.Seg.P == 0,
                                        "INT:Inner level:CS segment not present", ExceptionNP,
                                        ((gateSel & 0xfffc) + (type & CPU_INT_SOFTWARE)) != 0 ? 0
                                                : 1);
                                checkCPUCond(
                                        (Register.Flags & Register.FlagVM) != 0 && (csDPL != 0),
                                        "V86 interrupt calling codesegment with DPL>0", ExceptionGP,
                                        gateSel & 0xfffc);

                                int nSS, nESP;
                                int oSS, oESP;
                                oSS = Register.segValue(Register.SEG_NAME_SS);
                                oESP = Register.getRegESP();
                                int where = _cpuTss.whereSSxESPx(csDPL);
                                nSS = _cpuTss.getSSx(where);
                                nESP = _cpuTss.getESPx(where);
                                checkCPUCond((nSS & 0xfffc) == 0, "INT:Gate with SS zero selector",
                                        ExceptionTS, (type & CPU_INT_SOFTWARE) != 0 ? 0 : 1);
                                Descriptor nSSdesc = new Descriptor();
                                checkCPUCond(!Block.GDT.getDescriptor(nSS, nSSdesc),
                                        "INT:Gate with SS beyond limit", ExceptionTS,
                                        ((nSS & 0xfffc) + (type & CPU_INT_SOFTWARE)) != 0 ? 0 : 1);
                                checkCPUCond(((nSS & 3) != csDPL) || (nSSdesc.dpl() != csDPL),
                                        "INT:Inner level with CS_DPL!=SS_DPL and SS_RPL",
                                        ExceptionTS,
                                        ((nSS & 0xfffc) + (type & CPU_INT_SOFTWARE)) != 0 ? 0 : 1);

                                // check if stack segment is a writable data segment
                                switch (nSSdesc.type()) {
                                    case Descriptor.DESC_DATA_EU_RW_NA:
                                    case Descriptor.DESC_DATA_EU_RW_A:
                                    case Descriptor.DESC_DATA_ED_RW_NA:
                                    case Descriptor.DESC_DATA_ED_RW_A:
                                        break;
                                    default:
                                        // or #TS(ss_sel+EXT)
                                        Support.exceptionExit(
                                                "INT:Inner level:Stack segment not writable.");
                                        break;
                                }
                                checkCPUCond(nSSdesc.Saved.Seg.P == 0,
                                        "INT:Inner level with nonpresent SS", ExceptionSS,
                                        ((nSS & 0xfffc) + (type & CPU_INT_SOFTWARE)) != 0 ? 0 : 1);

                                // commit point
                                Register.SegmentPhys[Register.SEG_NAME_SS] = nSSdesc.getBase();
                                Register.SegmentVals[Register.SEG_NAME_SS] = nSS;
                                if (nSSdesc.big() != 0) {
                                    Block.Stack.Big = true;
                                    Block.Stack.Mask = 0xffffffff;
                                    Block.Stack.NotMask = 0;
                                    Register.setRegESP(nESP);
                                } else {
                                    Block.Stack.Big = false;
                                    Block.Stack.Mask = 0xffff;
                                    Block.Stack.NotMask = 0xffff0000;
                                    Register.setRegSP(nESP & 0xffff);
                                }

                                Block.CPL = csDPL;
                                if ((gate.type() & 0x8) != 0) { /* 32-bit Gate */
                                    if ((Register.Flags & Register.FlagVM) != 0) {
                                        push32(Register.segValue(Register.SEG_NAME_GS));
                                        Register.segSet16(Register.SEG_NAME_GS, 0x0);
                                        push32(Register.segValue(Register.SEG_NAME_FS));
                                        Register.segSet16(Register.SEG_NAME_FS, 0x0);
                                        push32(Register.segValue(Register.SEG_NAME_DS));
                                        Register.segSet16(Register.SEG_NAME_DS, 0x0);
                                        push32(Register.segValue(Register.SEG_NAME_ES));
                                        Register.segSet16(Register.SEG_NAME_ES, 0x0);
                                    }
                                    push32(oSS);
                                    push32(oESP);
                                } else { /* 16-bit Gate */
                                    if ((Register.Flags & Register.FlagVM) != 0)
                                        Support.exceptionExit("V86 to 16-bit gate");
                                    push16(oSS);
                                    push16(oESP);
                                }
                                // Log.LOG_MSG("INT:Gate to inner level SS:%X SP:%X",n_ss,n_esp);
                                // goto do_interrupt;
                                doInterrupt(gate, oldeip, type);
                                break;
                            }
                            if (csDPL != Block.CPL)
                                Support.exceptionExit(
                                        "Non-conforming intra privilege INT with DPL!=CPL");
                            // goto GotoDESC_CODE_R_C_NA;
                            gotoDESC_CODE_R_C_NA(csDesc, gate, gateSel, oldeip, csDPL, type);
                            break;
                        case Descriptor.DESC_CODE_N_C_A:
                        case Descriptor.DESC_CODE_N_C_NA:
                        case Descriptor.DESC_CODE_R_C_A:
                        case Descriptor.DESC_CODE_R_C_NA:
                            gotoDESC_CODE_R_C_NA(csDesc, gate, gateSel, oldeip, csDPL, type);
                            break;
                        // GotoDESC_CODE_R_C_NA:
                        // /* Prepare stack for gate to same priviledge */
                        // CPUCheckCond(csDesc.Saved.Seg.P == 0,
                        // "INT:Same level:CS segment not present",
                        // ExceptionNP, ((gateSel & 0xfffc) + (type & CPU_INT_SOFTWARE)) != 0 ?
                        // (int)0 :
                        // 1);
                        // if ((Register.Flags & Register.FlagVM) != 0 && (csDPL < Block.CPL))
                        // Support.ExceptionExit("V86 interrupt doesn't change to pl0"); // or
                        // #GP(cs_sel)

                        // commit point
                        // do_interrupt:
                        // if ((gate.Type & 0x8) != 0)
                        // { /* 32-bit Gate */
                        // Push32(Register.Flags);
                        // Push32(Register.SegValue(Register.SEG_NAME_CS));
                        // Push32(oldeip);
                        // if ((type & CPU_INT_HAS_ERROR) != 0) Push32(Block.Exception.Error);
                        // }
                        // else
                        // { /* 16-bit gate */
                        // Push16(Register.Flags & 0xffff);
                        // Push16(Register.SegValue(Register.SEG_NAME_CS));
                        // Push16(oldeip);
                        // if ((type & CPU_INT_HAS_ERROR) != 0) Push16(Block.Exception.Error);
                        // }
                        // break;
                        default:
                            Support.exceptionExit(
                                    "INT:Gate Selector points to illegal descriptor with type %x",
                                    csDesc.type());
                            break;
                    }

                    Register.SegmentVals[Register.SEG_NAME_CS] = (gateSel & 0xfffc) | Block.CPL;
                    Register.SegmentPhys[Register.SEG_NAME_CS] = csDesc.getBase();
                    Block.Code.Big = csDesc.big() > 0;
                    Register.setRegEIP(gateOff);

                    if ((gate.type() & 1) == 0) {
                        Register.setFlagBit(Register.FlagIF, false);
                    }
                    Register.setFlagBit(Register.FlagTF, false);
                    Register.setFlagBit(Register.FlagNT, false);
                    Register.setFlagBit(Register.FlagVM, false);
                    Log.logging(Log.LogTypes.CPU, Log.LogServerities.Normal,
                            "INT:Gate to %X:%X big %d %s", gateSel, gateOff, csDesc.big(),
                            (gate.type() & 0x8) != 0 ? 386 : 286);
                    return;
                }
                case Descriptor.DESC_TASK_GATE:
                    checkCPUCond(gate.Saved.Seg.P == 0, "INT:Gate segment not present", ExceptionNP,
                            (num * 8 + 2 + (type & CPU_INT_SOFTWARE)) != 0 ? 0 : 1);

                    switchTask(gate.getSelector(), TSwitchType.CALL_INT, oldeip);
                    if ((type & CPU_INT_HAS_ERROR) != 0) {
                        // TODO Be sure about this, seems somewhat unclear
                        if (_cpuTss.Is386)
                            push32(Block.Exception.Error);
                        else
                            push16(Block.Exception.Error);
                    }
                    return;
                default:
                    Support.exceptionExit("Illegal descriptor type %X for int %X", gate.type(),
                            num);
                    break;
            }
        }
        // assert(1);
        return; // make compiler happy
    }

    // do_interrupt
    private static void doInterrupt(Descriptor gate, int oldeip, int type) {
        if ((gate.type() & 0x8) != 0) { /* 32-bit Gate */
            push32(Register.Flags);
            push32(Register.segValue(Register.SEG_NAME_CS));
            push32(oldeip);
            if ((type & CPU_INT_HAS_ERROR) != 0)
                push32(Block.Exception.Error);
        } else { /* 16-bit gate */
            push16(Register.Flags & 0xffff);
            push16(Register.segValue(Register.SEG_NAME_CS));
            push16(oldeip);
            if ((type & CPU_INT_HAS_ERROR) != 0)
                push16(Block.Exception.Error);
        }
    }

    // GotoDESC_CODE_R_C_NA
    private static void gotoDESC_CODE_R_C_NA(Descriptor csDesc, Descriptor gate, int gateSel,
            int oldeip, int csDPL, int type) {
        /* Prepare stack for gate to same priviledge */
        checkCPUCond(csDesc.Saved.Seg.P == 0, "INT:Same level:CS segment not present", ExceptionNP,
                ((gateSel & 0xfffc) + (type & CPU_INT_SOFTWARE)) != 0 ? 0 : 1);
        if ((Register.Flags & Register.FlagVM) != 0 && (csDPL < Block.CPL))
            Support.exceptionExit("V86 interrupt doesn't change to pl0"); // or #GP(cs_sel)

        // commit point
        doInterrupt(gate, oldeip, type);

    }

    public static void hwHInterrupt(int num) {
        interrupt(num, 0, Register.getRegEIP());
    }

    public static void swInterrupt(int num, int oldeip) {
        interrupt(num, CPU_INT_SOFTWARE, oldeip);
    }

    public static void swInterruptNoIOPLCheck(int num, int oldeip) {
        interrupt(num, CPU_INT_SOFTWARE | CPU_INT_NOIOPLCHECK, oldeip);
    }
    // -- #endregion

    public static final int CPU_INT_SOFTWARE = 0x1;
    public static final int CPU_INT_EXCEPTION = 0x2;
    public static final int CPU_INT_HAS_ERROR = 0x4;
    public static final int CPU_INT_NOIOPLCHECK = 0x8;

    private static CPUModule _cpu;

    private static void shutdown(Section sec) {
        _cpu.dispose();
        _cpu = null;
    }

    public static void init(Section sec) throws WrongType {
        _cpu = new CPUModule(sec);
        sec.addDestroyFunction(CPU::shutdown, true);
    }
    //// initialize static members
    // boolean CPU::inited=false;


}

