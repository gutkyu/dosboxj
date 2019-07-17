package org.gutkyu.dosboxj.util;


public final class ByteConv {
    public static byte[] getBytes(short value) {
        byte[] result = new byte[2];
        byte offset = 0;
        result[offset++] = (byte) value;
        result[offset] = (byte) (value >>> 8);
        // result[offset++] = (byte)(value>>>6);
        // result[offset++] = (byte)(value>>>4);
        return result;
    }

    public static byte[] getBytes(int value) {
        byte[] result = new byte[4];
        byte offset = 0;
        result[offset++] = (byte) value;
        result[offset++] = (byte) (value >>> 8);
        result[offset++] = (byte) (value >>> 16);
        result[offset] = (byte) (value >>> 24);
        return result;
    }

    public static void setShort(byte[] buf, int startIndex, int value){
        buf[startIndex++] = (byte) value;
        buf[startIndex] = (byte) (value >>> 8);
    }

    public static void setInt(byte[] buf, int startIndex, int value){
        buf[startIndex++] = (byte) value;
        buf[startIndex++] = (byte) (value >>> 8);
        buf[startIndex++] = (byte) (value >>> 16);
        buf[startIndex] = (byte) (value >>> 24);
    }

    public static int getShort(byte[] value, int startIndex) {
        return (0xff & value[startIndex++]) | (0xff & value[startIndex]) << 8;
    }

    public static int getInt(byte[] value, int startIndex) {
        return (0xff & value[startIndex++]) | (0xff & value[startIndex++]) << 8
                | (0xff & value[startIndex++]) << 16 | (0xff & value[startIndex]) << 24;
    }
}
