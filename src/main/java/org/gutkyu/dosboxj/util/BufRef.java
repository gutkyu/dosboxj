package org.gutkyu.dosboxj.util;

public final class BufRef {
    public BufRef() {
        this(null, 0, 0);
    }

    public BufRef(byte[] buf, int startIndex, int len) {
        Buf = buf;
        StartIndex = startIndex;
        Len = len;
    }

    public byte[] Buf;
    public int StartIndex;
    public int Len; // uint16

    public void set(byte[] buf, int startIndex, int len) {
        Buf = buf;
        StartIndex = startIndex;
        Len = len;
    }
}
