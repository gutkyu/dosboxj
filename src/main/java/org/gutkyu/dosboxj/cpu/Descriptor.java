package org.gutkyu.dosboxj.cpu;



import org.gutkyu.dosboxj.hardware.memory.*;

// struct
public class Descriptor {
    // *********************************************************************
    // Descriptor
    // *********************************************************************

    public static final int DESC_INVALID = 0x00;
    public static final int DESC_286_TSS_A = 0x01;
    public static final int DESC_LDT = 0x02;
    public static final int DESC_286_TSS_B = 0x03;
    public static final int DESC_286_CALL_GATE = 0x04;
    public static final int DESC_TASK_GATE = 0x05;
    public static final int DESC_286_INT_GATE = 0x06;
    public static final int DESC_286_TRAP_GATE = 0x07;

    public static final int DESC_386_TSS_A = 0x09;
    public static final int DESC_386_TSS_B = 0x0b;
    public static final int DESC_386_CALL_GATE = 0x0c;
    public static final int DESC_386_INT_GATE = 0x0e;
    public static final int DESC_386_TRAP_GATE = 0x0f;

    /* EU/ED Expand UP/DOWN RO/RW Read Only/Read Write NA/A Accessed */
    public static final int DESC_DATA_EU_RO_NA = 0x10;
    public static final int DESC_DATA_EU_RO_A = 0x11;
    public static final int DESC_DATA_EU_RW_NA = 0x12;
    public static final int DESC_DATA_EU_RW_A = 0x13;
    public static final int DESC_DATA_ED_RO_NA = 0x14;
    public static final int DESC_DATA_ED_RO_A = 0x15;
    public static final int DESC_DATA_ED_RW_NA = 0x16;
    public static final int DESC_DATA_ED_RW_A = 0x17;

    /* N/R Readable NC/C Confirming A/NA Accessed */
    public static final int DESC_CODE_N_NC_A = 0x18;
    public static final int DESC_CODE_N_NC_NA = 0x19;
    public static final int DESC_CODE_R_NC_A = 0x1a;
    public static final int DESC_CODE_R_NC_NA = 0x1b;
    public static final int DESC_CODE_N_C_A = 0x1c;
    public static final int DESC_CODE_N_C_NA = 0x1d;
    public static final int DESC_CODE_R_C_A = 0x1e;
    public static final int DESC_CODE_R_C_NA = 0x1f;

    public final class SAVED {
        public SDescriptor Seg = new SDescriptor();
        public GDescriptor Gate = new GDescriptor();
        public int[] Fill = new int[2];

        // SAVED는 본래 union 구조
        // 소속 속성을 변경하면 나머지 다른 속성들도 업데이트 필요
        public void updateSDescriptor() {
            int f0 = Seg.takeOutInput0();
            int f1 = Seg.takeOutInput1(0);
            Fill[0] = f0;
            Fill[1] = f1;
            Gate.fill(f0, f1);
        }

        public void updateGDescriptor() {
            int f0 = Gate.takeOutInput0();
            int f1 = Gate.takeOutInput1();
            Fill[0] = f0;
            Fill[1] = f1;
            Seg.fill(f0, f1);
        }
    }

    // TODO SAVED는 원래 union 구조이기 때문에 내부 속성을 채우는 함수를 별도로 구현 필요
    // saved를 사용한 코드를 모두 검토해서 union처럼 작동하도록 반영해야함
    public SAVED Saved = new SAVED();

    public Descriptor() {
        Saved.Fill[0] = Saved.Fill[1] = 0;
    }

    // TODO SAVED는 원래 union 구조
    public void load(int address) {
        CPU.Block.MPL = 0;
        Saved.Fill[0] = Memory.readD(address);
        Saved.Fill[1] = Memory.readD(address + 4);
        Saved.Seg.fill(Saved.Fill[0], Saved.Fill[1]);
        Saved.Gate.fill(Saved.Fill[0], Saved.Fill[1]);
        CPU.Block.MPL = 3;
    }

    public void save(int address) {
        CPU.Block.MPL = 0;
        Memory.writeD(address, Saved.Fill[0]);
        Memory.writeD(address + 4, Saved.Fill[1]);
        CPU.Block.MPL = 3;
    }

    public int getBase() {
        return (Saved.Seg.Base24to31 << 24) | (Saved.Seg.Base16to23 << 16) | Saved.Seg.Base0to15;
    }

    public int getLimit() {
        int limit = (Saved.Seg.Limit16to19 << 16) | Saved.Seg.Limit0to15;
        if (Saved.Seg.G != 0)
            return (limit << 12) | 0xFFF;
        return limit;
    }

    public int getOffset() {
        return (Saved.Gate.Offset16to31 << 16) | Saved.Gate.Offset0to15;
    }

    public int getSelector() {
        return Saved.Gate.Selector;
    }

    public int type() {
        return Saved.Seg.Type;
    }

    public int conforming() {
        return Saved.Seg.Type & 8;
    }

    public int dpl() {
        return Saved.Seg.DPL;
    }

    public int big() {
        return Saved.Seg.Big;
    }

}
