package org.gutkyu.dosboxj.util;

public final class U8Ref {
    public U8Ref(byte u8, int len) {
        U8 = u8;
        Len = len;
    }

    public U8Ref(int u8, int len) {
        this((byte) u8, len);
    }

    public byte U8;
    public int Len; // uint16

    public void set(byte u8, int len) {
        U8 = u8;
        Len = len;
    }

    public void set(int u8, int len) {
        this.set((byte) u8, len);
    }
}
