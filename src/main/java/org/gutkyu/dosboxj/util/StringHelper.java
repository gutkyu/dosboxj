package org.gutkyu.dosboxj.util;

public final class StringHelper {

    // String.PadRight
    public static String padRight(String src, int width) {
        if (src.length() >= width)
            return src;

        int size = width - src.length();
        return String.format(src + "%1$" + size + "s", ' ');
    }

    // String.PadLeft
    public static String padLeft(String src, int width) {
        if (src.length() >= width)
            return src;

        int size = width - src.length();
        return String.format("%1$" + size + "s" + src, ' ');
    }
}

