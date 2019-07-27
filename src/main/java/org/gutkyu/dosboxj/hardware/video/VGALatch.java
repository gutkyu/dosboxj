package org.gutkyu.dosboxj.hardware.video;

public final class VGALatch {
    // FieldOffset(0)
    public byte b0;
    // FieldOffset(1)
    public byte b1;
    // FieldOffset(2)
    public byte b2;
    // FieldOffset(3)
    public byte b3;

    // FieldOffset(0)
    public int d() {
        return (0xff & b0) | (0xff & b1) << 8 | (0xff & b2) << 16 | (0xff & b3) << 24;
    }

    public void d(int value) {
        b0 = (byte) value;
        b1 = (byte) (value >>> 8);
        b2 = (byte) (value >>> 16);
        b3 = (byte) (value >>> 24);
    }
}
