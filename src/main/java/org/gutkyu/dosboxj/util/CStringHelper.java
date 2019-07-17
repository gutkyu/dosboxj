package org.gutkyu.dosboxj.util;

import java.nio.charset.StandardCharsets;

public final class CStringHelper {

    // 포팅 작업의 효율을 높이기 위해 c표준 라이브러리 함수 구현

    // ascii코드 기준
    public static byte[] strcpy(byte[] dest, byte[] src) {
        return strcpy(dest, 0, src, 0);
    }

    public static byte[] strcpy(byte[] dest, byte[] src, int srcIdx) {
        return strcpy(dest, 0, src, srcIdx);
    }

    public static byte[] strcpy(byte[] dest, int destIdx, byte[] src, int srcIdx) {
        int size = ArrayHelper.indexOf(src, srcIdx, src.length, (byte) 0) + 1; // null 문자 포함
        for (int i = 0; i < size; i++) {
            dest[i + destIdx] = src[i + srcIdx];
        }
        return dest;
    }

    // wikipedia
    // -this length does *NOT* include the array entry for the trailing null byte required for the
    // ending character of C strings
    // null문자열 포함하지 않음
    public static int strlen(byte[] str) {
        return ArrayHelper.indexOf(str, (byte) 0);
    }

    public static int strlen(byte[] str, int startIdx) {
        return ArrayHelper.indexOf(str, startIdx, str.length, (byte) 0);
    }

    public static int strcmp(byte[] src1, byte[] src2) {
        int i = 0;
        for (; src1[i] == src2[i]; i++) {
            if (src1[i] == 0)
                return 0;
        }
        return src1[i] < src2[i] ? -1 : 1;
    }

    public static int strcmp(byte[] src1, int startIdx, byte[] src2) {
        int i = 0;
        for (; src1[i + startIdx] == src2[i]; i++) {
            if (src1[i] == 0)
                return 0;
        }
        return src1[i] < src2[i] ? -1 : 1;
    }

    public static byte[] strcat(byte[] dest, byte[] src) {
        int i, j;

        i = ArrayHelper.indexOf(dest, (byte) 0x00);
        for (j = 0; src[j] != 0x00; j++)
            dest[i + j] = src[j];
        dest[i + j] = 0x00;
        return dest;
    }

    public static byte[] strcat(byte[] dest, byte src) {
        int i;
        i = ArrayHelper.indexOf(dest, (byte) 0x00);
        dest[i] = src;
        dest[i + 1] = 0x00;
        return dest;
    }

    public static byte[] strncat(byte[] dest, int destIdx, byte[] src, int srcIdx, int size) {
        int i, j;
        i = ArrayHelper.indexOf(dest, destIdx, dest.length, (byte) 0x00);
        for (j = srcIdx; j < size && src[j] != 0x00; j++)
            dest[i + j] = src[j];
        dest[i + j] = 0x00;
        return dest;
    }


    // ascii 문자값 chr을 처음 만나는 위치를 반환
    // 없다면 -1을 반환
    // c의 strchr은 결과를 찾지 못하면 null 포인터(0x00)를 반환하지만 c#에서는 -1을 반환한다.
    public static int strchr(byte[] src, int startIdx, byte chr) {
        for (int i = startIdx; src[i] != 0x00; i++) {
            if (src[i] == chr)
                return i;
        }
        return -1;
    }

    // ascii 문자값 chr을 마지막 위치를 반환
    // 없다면 -1을 반환
    // c의 strchr은 결과를 찾지 못하면 null 포인터(0x00)를 반환하지만 c#에서는 -1을 반환한다.
    public static int strrchr(byte[] src, byte chr) {
        for (int i = src.length - 1; src[i] != 0x00; i--) {
            if (src[i] == chr)
                return i;
        }
        return -1;
    }

    public static int strrchr(byte[] src, int startIdx, byte chr) {
        for (int i = startIdx; i >= startIdx && src[i] != 0x00; i--) {
            if (src[i] == chr)
                return i;
        }
        return -1;
    }


    // 주어진 길이안에서 비교
    public static int strncmp(byte[] src1, byte[] src2, int num) {

        for (int i = 0; i < num; ++i) {
            if (src1[i] < src2[i])
                return -1;
            else if (src1[i] > src2[i])
                return 1;
            else if (src1[i] == 0) /* null byte -- end of string */
                return 0;
        }

        return 0;
    }

    public static int strncmp(byte[] src1, int src1Idx, byte[] src2, int src2Idx, int num) {

        for (int i = 0; i < num; ++i) {
            if (src1[i + src1Idx] < src2[i + src2Idx])
                return -1;
            else if (src1[i + src1Idx] > src2[i + src2Idx])
                return 1;
            else if (src1[i + src1Idx] == 0) /* null byte -- end of string */
                return 0;
        }

        return 0;
    }

    // Get span until character in string
    // 소스문자열에서 주어진 리스트에 포함된 문자가 처음 발견된 위치 앞까지 문자의 갯수를 반환
    // null 문자에 이르면 멈추고 그 위치를 반환하기때문에 발견하지 못하면 반환값은 소스 문자열의 길이(strlen)와같다
    public static int strcspn(byte[] src, byte[] charList) {
        return strcspn(src, 0, charList);
    }

    public static int strcspn(byte[] src, int srcIdx, byte[] charList) {

        int i = srcIdx, len = srcIdx;
        if ((i = ArrayHelper.indexOf(src, charList[0])) < 0 || charList.length != src.length - i)
            return ArrayHelper.indexOf(src, i, src.length, (byte) 0x00);
        len = i;
        i = 0;
        while (i < charList.length && src[len + i] != (byte) 0x00 && src[len + i] == charList[i]) {
            i++;
        }
        if (i == charList.length)
            return len;
        return ArrayHelper.indexOf(src, i, src.length, (byte) 0x00);

    }

    // local 고려하지 않았음.
    public static boolean isalpha(byte chr) {
        return ((0x41 <= chr) && (chr <= 0x5a)) || ((0x61 <= chr) && (chr <= 0x7a));
    }

    // local 고려하지 않았음
    public static byte toupper(byte chr) {
        return (0x61 <= chr) && (chr <= 0x7a) ? (byte) (chr - 0x20) : chr;
    }

    // local은 고려하지 않았음
    // 대소문자 구별없이 비교
    public static int strcasecmp(byte[] src1, byte[] src2) {
        int i = 0;
        for (; (src1[i] == src2[i]) || (toupper(src1[i]) == toupper(src2[i])); i++) {
            if (src1[i] == 0)
                return 0;
        }
        return src1[i] < src2[i] ? -1 : 1;
    }

    public static int strcasecmp(byte[] src1, int src1Start, byte[] src2, int src2Start) {
        int i = 0;
        for (; (src1[i + src1Start] == src2[i + src2Start])
                || (toupper(src1[i + src1Start]) == toupper(src2[i + src2Start])); i++) {
            if (src1[i + src1Start] == 0)
                return 0;
        }
        return src1[i + src1Start] < src2[i + src2Start] ? -1 : 1;
    }

    public static int strncasecmp(byte[] src1, byte[] src2, int size) {
        int i = 0;
        for (; (i < size)
                && ((src1[i] == src2[i]) || (toupper(src1[i]) == toupper(src2[i]))); i++) {
            if (src1[i] == 0)
                return 0;
        }
        return src1[i] < src2[i] ? -1 : 1;
    }

    public static int strncasecmp(byte[] src1, int src1Start, byte[] src2, int src2Start,
            int size) {
        int i = 0;
        for (; (i < size) && ((src1[i + src1Start] == src2[i + src2Start])
                || (toupper(src1[i + src1Start]) == toupper(src2[i + src2Start]))); i++) {
            if (src1[i + src1Start] == 0)
                return 0;
        }
        return src1[i + src1Start] < src2[i + src2Start] ? -1 : 1;
    }

    // Copy characters from string
    // Copies the first num characters of source to destination. If the end of the source C string
    // (which is signaled by a null-character) is found before num characters have been copied,
    // destination is padded with zeros until a total of num characters have been written to it.
    // No null-character is implicitly appended to the end of destination, so destination will only
    // be null-terminated if the length of the C string in source is less than num.
    // 문자열을 주어진 길이 만큼 복사하지만 복사된 문자열의 끝에 암시적으로 null문자를 붙이진 않는다.
    // null문자 종결을 보장하지 않는다.
    public static byte[] strncpy(byte[] dest, byte[] src, int num) {
        for (int i = 0; i < num; i++) {
            dest[i] = src[i];
        }
        for (int i = 0; i < num - src.length; i++) {
            dest[i] = 0;
        }
        return dest;
    }

    public static byte[] strncpy(byte[] dest, int destIdx, byte[] src, int srcIdx, int num) {
        while (num-- > 0 && srcIdx < src.length) {
            dest[destIdx++] = (byte) src[srcIdx++];
        }
        while (num-- > 0) {
            dest[destIdx++] = 0;
        }
        return dest;
    }

    public static byte[] safeStrncpy(byte[] dest, int destIdx, byte[] src, int srcIdx, int num) {
        safeStrncpy(dest, destIdx, src, srcIdx, num - 1);
        dest[destIdx + num - 1] = 0x00;
        return dest;
    }

    public static byte[] safeStrncpy(byte[] dest, byte[] src, int num) {
        strncpy(dest, src, num - 1);
        dest[num - 1] = 0x00;
        return dest;
    }

    public static byte[] upcase(byte[] src) {
        for (int i = 0; i > 0; i++) {
            src[i] = toupper(src[i]);
        }
        return src;
    }

    public static byte[] cstr(String src) {
        return (src + '\u0000').getBytes(StandardCharsets.US_ASCII);
    }


}
