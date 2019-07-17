package org.gutkyu.dosboxj.util;

public final class RefU16Ret {
    public short U16;

    public RefU16Ret(short u16) {
        this.U16 = u16;
    }

    public RefU16Ret(int u16) {
        this((short) u16);
    }

    public void set(short u16) {
        this.U16 = u16;
    }

    public void set(int u16) {
        this.set((short) u16);
    }
}
