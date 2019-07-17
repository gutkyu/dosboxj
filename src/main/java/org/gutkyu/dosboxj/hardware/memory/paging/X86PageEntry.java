package org.gutkyu.dosboxj.hardware.memory.paging;

final class X86PageEntry {
    private int mask1;
    private int mask3;
    private int _value;

    // 동일 package만 접근
    protected X86PageEntry() {
        mask1 = 0x1;
        mask3 = 0x7;
    }

    public int getLoad() {
        return Base << 12 | avl << 9 | g << 8 | pat << 7 | d << 6 | a << 5 | pcd << 4 | pwt << 3
                | us << 2 | wr << 1 | p;
    }

    public void setLoad(int value) {
        p = value & mask1;
        wr = p >>> 1 & mask1;
        us = wr >>> 1 & mask1;
        pwt = us >>> 1 & mask1;
        pcd = pwt >>> 1 & mask1;
        a = pcd >>> 1 & mask1;
        d = a >>> 1 & mask1;
        pat = d >>> 1 & mask1;
        g = pat >>> 1 & mask1;
        avl = g >>> 1 & mask3;
        Base = avl >>> 3;
    }



    public int p;

    public int wr;


    public int us;

    public int pwt;

    public int pcd;

    public int a;

    public int d;

    public int pat;
    public int g;

    public int avl;
    public int Base;

}
