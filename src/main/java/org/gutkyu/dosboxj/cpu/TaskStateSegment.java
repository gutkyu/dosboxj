package org.gutkyu.dosboxj.cpu;


import org.gutkyu.dosboxj.hardware.memory.*;



public final class TaskStateSegment {
    public TSSDescriptor Desc = new TSSDescriptor();
    public int Selector;
    public int BaseAddr;
    public int Limit;
    public boolean Is386;
    private boolean _valid;

    // struct TSS_16는 각 요소의 offset을 구하는데만 사용, map역할만 하고 직접적인 데이타저장에는 사용되지 않음
    // enum형과 OffsetOf함수 조합으로 사용하도록 변경
    public enum TSS16 {
        back(0), /* Back link to other task */
        sp0(1), /* The CK stack pointer */
        ss0(2), /* The CK stack selector */
        sp1(3), /* The parent KL stack pointer */
        ss1(4), /* The parent KL stack selector */
        sp2(5), /* Unused */
        ss2(6), /* Unused */
        ip(7), /* The instruction pointer */
        flags(8), /* The flags */
        ax(9), cx(10), dx(11), bx(12), /* The general purpose registers */
        sp(13), bp(14), si(15), di(16), /* The special purpose registers */
        es(17), /* The extra selector */
        cs(18), /* The code selector */
        ss(19), /* The application stack selector */
        ds(20), /* The data selector */
        ldt(21); /* The local descriptor table */
        private final byte value;

        private TSS16(int value) {
            this.value = (byte) value;
        }

        public byte toValue() {
            return this.value;
        }
    }

    // struct TSS_32는 각 요소의 offset을 구하는데만 사용, map역할만 하고 직접적인 데이타저장에는 사용되지 않음
    // enum형과 OffsetOf함수 조합으로 사용하도록 변경
    public enum TSS32 {
        back(0), /* Back link to other task */
        esp0(1), /* The CK stack pointer */
        ss0(2), /* The CK stack selector */
        esp1(3), /* The parent KL stack pointer */
        ss1(4), /* The parent KL stack selector */
        esp2(5), /* Unused */
        ss2(6), /* Unused */
        cr3(7), /* The page directory pointer */
        eip(8), /* The instruction pointer */
        eflags(9), /* The flags */
        eax(10), ecx(11), edx(12), ebx(13), /* The general purpose registers */
        esp(14), ebp(15), esi(16), edi(17), /* The special purpose registers */
        es(18), /* The extra selector */
        cs(19), /* The code selector */
        ss(20), /* The application stack selector */
        ds(21), /* The data selector */
        fs(22), /* And another extra selector */
        gs(23), /* ... and another one */
        ldt(24); /* The local descriptor table */
        private final byte value;

        private TSS32(int value) {
            this.value = (byte) value;
        }

        public byte toValue() {
            return this.value;
        }
    }

    public TaskStateSegment() {
        _valid = false;
    }

    public boolean isValid() {
        return _valid;
    }

    public int getBack() {
        CPU.Block.MPL = 0;
        int backlink = Memory.readW(BaseAddr);
        CPU.Block.MPL = 3;
        return backlink;
    }

    public void saveSelector() {
        CPU.Block.GDT.setDescriptor(Selector, Desc);
    }

    // public void SSxESPx(int level, out int outSS, out int outESP)
    public int whereSSxESPx(int level) {
        if (Is386) {
            return BaseAddr + offsetOf(TSS32.esp0) + level * 8;
        } else {
            return BaseAddr + offsetOf(TSS16.sp0) + level * 4;
        }
    }

    public int getSSx(int where) {
        int outSS = 0;
        CPU.Block.MPL = 0;
        if (Is386) {
            outSS = Memory.readW(where + 4);
        } else {
            outSS = Memory.readW(where + 2);
        }
        CPU.Block.MPL = 3;
        return outSS;
    }

    public int getESPx(int where) {
        int outESP = 0;
        CPU.Block.MPL = 0;
        if (Is386) {
            outESP = Memory.readD(where);
        } else {
            outESP = Memory.readW(where);
        }
        CPU.Block.MPL = 3;
        return outESP;
    }

    public boolean setSelector(int newSel) {
        _valid = false;
        if ((newSel & 0xfffc) == 0) {
            Selector = 0;
            BaseAddr = 0;
            Limit = 0;
            Is386 = true;
            return true;
        }
        if ((newSel & 4) != 0)
            return false;
        if (!CPU.Block.GDT.getDescriptor(newSel, Desc))
            return false;
        switch (Desc.type()) {
            case Descriptor.DESC_286_TSS_A:
            case Descriptor.DESC_286_TSS_B:
            case Descriptor.DESC_386_TSS_A:
            case Descriptor.DESC_386_TSS_B:
                break;
            default:
                return false;
        }
        if (Desc.Saved.Seg.P == 0)
            return false;
        Selector = newSel;
        _valid = true;
        BaseAddr = Desc.getBase();
        Limit = Desc.getLimit();
        Is386 = Desc.is386();
        return true;
    }

    public static int offsetOf(TaskStateSegment.TSS16 tts16) {
        return tts16.toValue() * 2;
    }

    public static int offsetOf(TaskStateSegment.TSS32 tts32) {
        return tts32.toValue() * 4;
    }
}
