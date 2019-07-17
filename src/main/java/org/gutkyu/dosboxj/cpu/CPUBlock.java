package org.gutkyu.dosboxj.cpu;

public final class CPUBlock {
    public static class STACK {
        public int Mask, NotMask;
        public boolean Big;
    }
    public static class CODE {
        public boolean Big;
    }
    public static class HLT {
        public int CS, EIP;
        public CPUDecoder OldDecoder;
    }
    public static class EXCEPTION {
        public int Which, Error;
    }

    public int CPL; /* Current Privilege */
    public int MPL;
    public int CR0;
    public boolean PMode; /* Is Protected mode enabled */
    public GDTDescriptorTable GDT = new GDTDescriptorTable();
    public DescriptorTable IDT = new DescriptorTable();
    public STACK Stack = new STACK();
    public CODE Code = new CODE();
    public HLT HLTObj = new HLT();
    public EXCEPTION Exception = new EXCEPTION();

    public int Direction;
    public boolean TrapSkip;
    public int[] DRX = new int[8];
    public int[] TRX = new int[8];
}
