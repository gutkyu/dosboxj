package org.gutkyu.dosboxj.util;

public final class RefU8Ret {
    public byte U8;

    public RefU8Ret(byte u8) {
        this.U8 = u8;
    }

    public RefU8Ret(int u8) {
        this((byte) u8);
    }

    public void set(byte u8) {
        this.U8 = u8;
    }
}
