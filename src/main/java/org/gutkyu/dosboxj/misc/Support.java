package org.gutkyu.dosboxj.misc;



import org.gutkyu.dosboxj.util.*;

public final class Support {
    public static void exceptionExit(String arg) {
        // va_list msg;
        // va_start(msg, format);
        // vsprintf(buf, format, msg);
        // va_end(msg);
        // strcat(buf, "\n");

        // throw (buf);
        throw new DOSException(arg + "\n");
    }

    public static void exceptionExit(String format, int... args) {
        // va_list msg;
        // va_start(msg, format);
        // vsprintf(buf, format, msg);
        // va_end(msg);
        // strcat(buf, "\n");

        // throw (buf);
        throw new DOSException(format + "\n");
    }

    public static void exceptionExit(String format, String... args) {
        // va_list msg;
        // va_start(msg, format);
        // vsprintf(buf, format, msg);
        // va_end(msg);
        // strcat(buf, "\n");

        // throw (buf);
        throw new DOSException(format + "\n");

    }

    public static int lTrimIndex(byte[] src, int startIndex) {
        while (startIndex < src.length && Character.isWhitespace((char) src[startIndex]))
            startIndex++;
        return startIndex;
    }

    // ltrim의 byte 배열 버전
    public static byte[] lTrim(byte[] str, int strIndex) {
        while (strIndex < str.length && Character.isWhitespace((char) str[strIndex]))
            strIndex++;
        byte[] tmp = new byte[str.length - strIndex];
        for (int i = 0; i < tmp.length; i++) {
            tmp[i] = str[strIndex + i];
        }
        return tmp;
    }

    public static byte[] lTrim(byte[] str) {
        return lTrim(str);
    }

    public static byte[] rTrim(byte[] str, int strIndex) {
        int p_idx = CStringHelper.strchr(str, strIndex, (byte) '\0');
        while (--p_idx >= strIndex && Character.isWhitespace((char) str[p_idx])) {
        }
        str[p_idx + 1] = (byte) '\0';
        return str;
    }

    public static byte[] rTrim(byte[] str) {
        return rTrim(str, 0);
    }

    public static byte[] trim(byte[] str, int strIndex) {
        return lTrim(rTrim(str, strIndex));
    }

    public static byte[] trim(byte[] str) {
        return trim(str);
    }

    public static boolean isSpace(byte c) {
        switch (c) {
            case (byte) ' ':
            case (byte) '\n':
            case (byte) '\f':
            case (byte) '\t':
            case (byte) '\u000B':// \v
            case (byte) '\r':
                return true;
            default:
                return false;
        }
    }

    public static boolean scanCmdBool(CStringPt cmd, String check) {
        CStringPt scan = CStringPt.clone(cmd);
        int cLen = check.length();
        while (!(scan = scan.positionOf('/')).isEmpty()) {
            /* found a / now see behind it */
            scan.movePtToR1();
            if (scan.equalsIgnoreCase(check) && (scan.get(cLen) == ' ' || scan.get(cLen) == '\t'
                    || scan.get(cLen) == '/' || scan.get(cLen) == 0)) {
                /* Found a math now remove it from the string */
                // memmove(scan - 1, scan + c_len, strlen(scan + c_len) + 1);
                CStringPt pt1 = CStringPt.clone(scan, cLen);
                CStringPt pt2 = CStringPt.clone(scan, -1);
                CStringPt.rawMove(pt1, pt2, pt1.length() + 1);
                pt2.trim();
                return true;
            }
        }
        return false;
    }


    /* This scans the command line for a remaining switch and reports it else returns 0 */
    public static CStringPt scanCmdRemain(CStringPt cmd) {
        CStringPt scan, found;
        if (!(scan = found = cmd.positionOf('/')).isEmpty()) {
            while (scan.get() != 0 && !Character.isWhitespace(scan.get()))
                scan.movePtToR1();
            scan.set((char) 0);
            return found;
        } else
            return CStringPt.getZero();
    }

    public static CStringPt stripWord(CStringPt line) {
        // CStringPt scan = line;
        CStringPt scan = CStringPt.clone(line);
        scan = scan.lTrim();
        if (scan.get() == '"') {
            CStringPt end_quote = CStringPt.clone(scan, 1).positionOf('"');
            if (!end_quote.isEmpty()) {
                end_quote.set((char) 0);
                line = end_quote.movePtToR1().lTrim();
                return CStringPt.clone(scan, 1);
            }
        }
        CStringPt begin = CStringPt.clone(scan);
        for (char c = scan.get(); (c = scan.get()) != 0; scan.movePtToR1()) {
            if (Character.isWhitespace(c)) {
                scan.set((char) 0);
                scan.movePtToL1();
                break;
            }
        }
        CStringPt.copyPt(scan, line);
        return begin;
    }

    public static void stripSpaces(CStringPt args, char also) {
        while (!args.isEmpty() && args.get() != 0
                && (Character.isWhitespace(args.get()) || args.get() == also)) {
            args.movePtToR1();
        }
    }
}
