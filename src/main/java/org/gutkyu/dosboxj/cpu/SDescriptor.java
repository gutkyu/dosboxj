package org.gutkyu.dosboxj.cpu;



public final class SDescriptor {

    public int Limit0to15;// :16;
    public int Base0to15;// :16;
    public int Base16to23;// :8;
    public int Type;// :5;
    public int DPL;// :2;
    public int P;// :1;
    public int Limit16to19;// :4;
    public int AVL;// :1;
    public int R;// :1;
    public int Big;// :1;
    public int G;// :1;
    public int Base24to31;// :8;

    public void fill(int input0, int input1) {
        Limit0to15 = (int) input0 & 0xFFFF;
        Base0to15 = (int) (input0 >>>16) & 0xFFFF;
        Base16to23 = (int) input1 & 0xFF;
        Type = (int) (input1 >>>8) & 0x1F;
        DPL = (int) (input1 >>>13) & 0x3;
        P = (int) (input1 >>>15) & 0x1;
        Limit16to19 = (int) (input1 >>>16) & 0xF;
        AVL = (int) (input1 >>>20) & 0x1;
        R = (int) (input1 >>>21) & 0x1;
        Big = (int) (input1 >>>22) & 0x1;
        G = (int) (input1 >>>23) & 0x1;
        Base24to31 = (int) (input1 >>>24) & 0xFF;
    }

    public int takeOutInput0() {
        return Limit0to15 | (Base0to15 << 16);
    }

    public int takeOutInput1(int input) {
        return Base16to23 | (Type << 8) | (input << 13) | (P << 15) | (Limit16to19 << 16)
                | (AVL << 20) | (R << 21) | (Big << 22) | (G << 23) | (Base24to31 << 24);
    }
}
