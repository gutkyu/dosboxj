package org.gutkyu.dosboxj.util;

public final class Convert {

    public static byte toByte(boolean value) {
        return (byte) (value ? 1 : 0);
    }

    public static byte toByte(String value) {
        return Byte.parseByte(value);
    }

    public static byte toByte(short value) {
        return (byte) value;
    }

    public static byte toByte(int value) {
        return (byte) value;
    }

    public static short toShort(boolean value) {
        return (byte) (value ? 1 : 0);
    }

    public static short toShort(String value) {
        return Short.parseShort(value);
    }

    public static short toShort(int value) {
        return (short) value;
    }


}
