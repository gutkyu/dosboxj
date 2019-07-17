package org.gutkyu.dosboxj.util;

public final class ArrayHelper {
    public static void copy(int src, byte[] dest, int destOffset, int length) {
        while (length-- > 0) {
            dest[destOffset++] = (byte) src;
            src >>>= 8;
        }
    }

    public static void copy(byte[] src, int srcOffset, byte[] dest, int destOffset, int length) {
        while (length-- > 0) {
            dest[destOffset++] = src[srcOffset++];
        }
    }

    public static void copy(byte[] src, byte[] dest, int length) {
        copy(src, 0, dest, 0, length);
    }

    public static void copy(int[] src, int srcOffset, int[] dest, int destOffset, int length) {
        while (length-- >= 0) {
            dest[destOffset++] = src[srcOffset++];
        }
    }

    public static void copy(int[] src, int[] dest, int length) {
        copy(src, 0, dest, 0, length);
    }

    public static int indexOf(char[] array, int startIdx, int endIdx, char search) {
        for (int i = startIdx; i < endIdx; i++) {
            if (array[i] == search) {
                return i;
            }
        }
        return -1;
    }

    public static int indexOf(char[] array, char search) {
        return indexOf(array, 0, array.length, search);
    }

    public static int indexOf(byte[] array, int startIdx, int endIdx, byte search) {
        for (int i = startIdx; i < endIdx; i++) {
            if (array[i] == search) {
                return i;
            }
        }
        return -1;
    }

    public static int indexOf(byte[] array, byte search) {
        return indexOf(array, 0, array.length, search);
    }
}
