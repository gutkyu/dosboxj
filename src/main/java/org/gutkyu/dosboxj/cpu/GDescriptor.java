package org.gutkyu.dosboxj.cpu;



public class GDescriptor {

    public int Offset0to15;// :16;
    public int Selector;// :16;
    public int ParamCount;// :5;
    public int Reserved;// :3;
    public int Type;// :5;
    public int DPL;// :2;
    public int P;// :1;
    public int Offset16to31;// :16;

    public void fill(int input0, int input1) {
        Offset0to15 = input0 & 0xFFFF;
        Selector = (input0 >>> 16) & 0xFFFF;
        ParamCount = input1 & 0x1F;
        Reserved = (input1 >>> 5) & 0x07;
        Type = (input1 >>> 8) & 0x1F;
        DPL = (input1 >>> 13) & 0x3;
        P = (input1 >>> 15) & 0x1;
        Offset16to31 = (input1 >>> 16) & 0xFFFF;

    }

    public int takeOutInput0() {
        return Offset0to15 | Selector << 16;

    }

    public int takeOutInput1() {
        return ParamCount | (Reserved << 5) | (Type << 8) | (DPL << 13) | (P << 15)
                | (Offset16to31 << 16);

    }
}

