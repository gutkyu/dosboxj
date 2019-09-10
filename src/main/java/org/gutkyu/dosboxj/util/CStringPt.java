package org.gutkyu.dosboxj.util;

import java.util.Arrays;

public final class CStringPt {

    private char[] arrays;
    private int posNullChar;
    private boolean changed;// 내용이 바뀌었는지 체크, 문자열 생성할때 사용, 생성하고 나서 false로 변경


    private int index;
    private String str;// tostring을 위한 캐시


    private CStringPt() {
        this.arrays = null;
        this.posNullChar = -1;
        this.changed = false;
        this.index = -1;
        this.str = null;
        this.changed = false;
    }

    public CStringPt(int length) {
        this.arrays = new char[length];
        this.posNullChar = length - 1;
        this.changed = false;
        this.index = 0;
        this.str = null;
        this.changed = false;
    }

    public char get(int index) {
        return this.arrays[this.index + index];
    }

    public void set(int index, char value) {
        this.arrays[this.index + index] = value;
        this.changed = true;
    }

    public char get() {
        return get(0);
    }

    public void set(char value) {
        set(0, value);
    }

    // 사용 안함
    // Empty관련 함수,속성으로 대체
    private static CStringPt zero;

    // Zero의 특징은 항상 IsEmpty == true임
    public static CStringPt getZero() {
        if (zero == null)
            zero = CStringPt.create();
        return zero;
    }

    // ++
    public CStringPt movePtToR1() {
        this.index++;
        this.changed = true;
        return this;
    }

    // --
    public CStringPt movePtToL1() {
        this.index--;
        this.changed = true;
        return this;
    }

    // +=
    public CStringPt moveR(int offset) {
        this.index += offset;
        this.changed = true;
        return this;
    }

    // +, -
    //
    public static CStringPt clone(CStringPt source, int offset) {
        CStringPt pt = new CStringPt();
        pt.arrays = source.arrays;
        pt.posNullChar = source.posNullChar;
        pt.changed = source.changed;
        pt.str = source.str;
        pt.index = source.index + offset;
        return pt;
    }

    public static CStringPt clone(CStringPt source) {
        return clone(source, 0);
    }


    // <
    // 같은 Cstring에서 파생된 것만 비교 연산자 사용토록 소스 수정할 것
    public static boolean less(CStringPt value1, CStringPt value2) {
        return value1.index < value2.index;
    }

    // <
    public static boolean lessOrEqual(CStringPt value1, CStringPt value2) {
        return value1.index <= value2.index;
    }

    // >
    public static boolean great(CStringPt value1, CStringPt value2) {
        return value1.index > value2.index;
    }

    // >=
    public static boolean greatOrEqual(CStringPt value1, CStringPt value2) {
        return value1.index >= value2.index;
    }

    // ==
    public static boolean equal(CStringPt value1, CStringPt value2) {
        return value1.index == value2.index;
    }

    // !=
    public static boolean notEqual(CStringPt value1, CStringPt value2) {
        return value1.index != value2.index;
    }

    // copyPt, 포인터 정보 복사, 주로 함수 포인터 매개변수의 값을 바꾸기 위해 사용
    public static void copyPt(CStringPt src, CStringPt dest) {
        dest.arrays = src.arrays;
        dest.posNullChar = src.posNullChar;
        dest.index = src.index;
        dest.changed = src.changed;

        dest.str = src.str;
        dest.zero = src.zero;
    }

    // dispose하지 않고 할당된 메모리 해제만 수행
    public void empty() {
        this.arrays = null;
    }

    public boolean isEmpty() {
        return this.arrays == null;
    }

    public boolean equals(CStringPt value) {
        for (int i = 0; this.arrays[i + this.index] == value.arrays[i + value.index]; i++) {
            if (this.arrays[i + this.index] == 0)
                return true;
        }
        return false;
    }

    public boolean equals(String value) {
        int i = 0;
        for (; i < value.length() && this.arrays[i + this.index] == value.charAt(i); i++) {
        }
        if (i == value.length() && this.arrays[i + this.index] == 0)
            return true;
        return false;
    }

    public boolean equalsIgnoreCase(CStringPt value) {
        if (this.arrays == null)
            return false;

        for (int i = 0; Character.toUpperCase(this.arrays[i + this.index]) == Character
                .toUpperCase(value.arrays[i + value.index]); i++) {
            if (this.arrays[i + this.index] == 0)
                return true;
        }
        return false;
    }

    public boolean equalsIgnoreCase(String value) {
        int i = 0;
        for (; i < value.length() && Character.toUpperCase(this.arrays[i + this.index]) == Character
                .toUpperCase(value.charAt(i)); i++) {
        }
        if (i == value.length() && this.arrays[i + this.index] == 0)
            return true;
        return false;
    }

    // null char 까지 비교
    public boolean startWith(CStringPt value) {
        char c0, c1;
        for (int i = 0; i < value.arrays.length && value.arrays[value.index + i] != 0; i++) {
            c0 = this.arrays[this.index + i];
            c1 = value.arrays[value.index + i];
            if (c0 != c1)
                return false;
            else if (c0 == 0) /* null byte -- end of string */
                return true;
        }

        return false;
    }

    public boolean startWithIgnoreCase(CStringPt value) {
        char c0, c1;
        for (int i = 0; i < value.arrays.length && value.arrays[value.index + i] != 0; i++) {
            c0 = this.arrays[this.index + i];
            c1 = value.arrays[value.index + i];
            if (c0 != c1 && (Character.toUpperCase(c0) != Character.toUpperCase(c1)))
                return false;
            else if (c0 == 0) /* null byte -- end of string */
                return true;
        }

        return false;
    }

    public boolean startWith(String value) {
        char c;
        for (int i = 0; i < value.length(); i++) {
            c = this.arrays[this.index + i];
            if (c != value.charAt(i))
                return false;
            else if (c == 0) /* null byte -- end of string */
                return true;
        }

        return false;
    }

    public boolean startWithIgnoreCase(String value) {
        char c;
        for (int i = 0; i < value.length(); i++) {
            c = this.arrays[this.index + i];
            if (c != value.charAt(i)
                    && Character.toUpperCase(c) != Character.toUpperCase(value.charAt(i)))
                return false;
            else if (c == 0) /* null byte -- end of string */
                return true;
        }
        return false;
    }

    private int indexOf(char[] array, char search, int offset, int count) {
        for (int i = offset; i < count; i++) {
            if (array[i] == search) {
                return i;
            }
        }
        return -1;
    }

    // 현재 위치 _index가 0으로 대응됨
    public int indexOf(char value) {
        calcNullCharPos(this);
        return indexOf(this.arrays, value, this.index, this.posNullChar - this.index) - this.index;
    }

    private int lastIndexOf(char[] array, char search, int offset, int count) {
        while (offset-- > 0 && count-- > 0) {
            if (array[offset] == search) {
                return offset - this.index;
            }
        }
        return -1;
    }

    // strchr
    public CStringPt positionOf(char value) {
        CStringPt pt = CStringPt.clone(this);
        calcNullCharPos(this);
        int idx = indexOf(this.arrays, value, this.index, this.posNullChar - this.index);
        if (idx < 0) {
            pt = new CStringPt();
            pt.arrays = null;
            pt.posNullChar = -1;
        } else {
            pt.index = idx;
            pt.changed = true;
        }
        return pt;
    }

    // strstr
    public CStringPt positionOf(String value) {
        CStringPt pt = CStringPt.clone(this);
        calcNullCharPos(this);
        int start = this.index;
        int i = 0;
        int idx = indexOf(this.arrays, value.charAt(i++), start, this.posNullChar - this.index);
        if (idx < 0) {
            pt = new CStringPt();
            pt.arrays = null;
            pt.posNullChar = -1;
            return pt;
        }
        pt.index = idx;

        start = idx + 1;
        while (start < pt.posNullChar && i < value.length()) {
            if (pt.arrays[start] != value.charAt(i)) {
                pt = new CStringPt();
                pt.arrays = null;
                pt.posNullChar = -1;
                return pt;
            }
            start++;
            i++;
        }

        pt.changed = true;
        return pt;
    }

    // 현재 위치 _index이 0으로 대응됨
    public int lastIndexOf(char value) {
        calcNullCharPos(this);
        return lastIndexOf(this.arrays, value, this.posNullChar - 1, this.posNullChar - this.index)
                - this.index;
    }

    public CStringPt lastPositionOf(char value) {
        CStringPt pt = CStringPt.clone(this);
        calcNullCharPos(this);
        if (this.index == this.posNullChar) {
            pt = new CStringPt();
            pt.arrays = null;
            pt.posNullChar = -1;
            return pt;
        }
        int idx = lastIndexOf(this.arrays, value, this.posNullChar - 1,
                this.posNullChar - this.index);
        if (idx < 0) {
            pt = new CStringPt();
            pt.arrays = null;
            pt.posNullChar = -1;
        } else {
            pt.index = idx;
            pt.changed = true;
        }
        return pt;
    }

    private static void strcat(char[] dest, char src) {
        int i = ArrayHelper.indexOf(dest, 0, dest.length, (char) 0x00);
        dest[i] = src;
        dest[i + 1] = (char) 0x00;
    }

    private static void strncat(char[] dest, int dest_idx, char[] src, int src_idx, int size) {
        int j;
        int i = ArrayHelper.indexOf(dest, dest_idx, dest.length, (char) 0x00);
        for (j = src_idx; j < size && src[j] != 0x00; j++)
            dest[i + j] = src[j];
        dest[i + j] = (char) 0x00;
    }


    private void concat(char[] dest, int dest_idx, char[] src, int src_idx, int size) {
        strncat(dest, dest_idx, src, src_idx, size);
        // this._cstr._posNullChar = this._cstr._posNullChar + size;
        this.changed = true;
        // _cstr._arrays[this._cstr._posNullChar] = (char)0x00;
    }

    // 현재 배열에서 null 문자를 시작 위치로 삼아 특정 배열의 현재 위치에서 null 문자까지의 문자열을 복사
    public void concat(CStringPt value) {
        calcNullCharPos(this);
        calcNullCharPos(value);
        int len = value.posNullChar - value.index;
        concat(this.arrays, this.posNullChar, value.arrays, value.index, len);
        this.changed = true;
    }


    public void concat(CStringPt value, int count) {
        calcNullCharPos(this);
        calcNullCharPos(value);
        int len = count;
        concat(this.arrays, this.posNullChar, value.arrays, value.index, len);
        this.changed = true;
    }


    // 현재 배열에서 null 문자를 시작 위치로 삼아 특정 배열의 현재 위치에서 null 문자까지의 문자열을 복사
    public void concat(char[] value) {
        calcNullCharPos(this);
        int len = indexOf(value, (char) 0x00, 0, value.length);
        concat(this.arrays, this.posNullChar, value, 0, len);
        // this._cstr._posNullChar = this._cstr._posNullChar + len;
        this.changed = true;
        this.arrays[this.posNullChar] = (char) 0x00;
    }

    public void concat(String value) {

        calcNullCharPos(this);
        int len = value.length();
        int i, j;
        i = indexOf(this.arrays, (char) 0x00, this.index, this.posNullChar - this.index + 1);
        for (j = 0; j < len && value.charAt(j) != 0x00; j++)
            this.arrays[i + j] = value.charAt(j);
        this.arrays[i + j] = (char) 0x00;
        // this._cstr._posNullChar = this._cstr._posNullChar + len;
        this.changed = true;
        // _cstr._arrays[this._cstr._posNullChar] = (char)0x00;
    }

    // 현재 배열에서 null 문자를 시작 위치로 삼아 문자복사
    public void concat(char value) {
        calcNullCharPos(this);
        this.arrays[this.posNullChar] = value;
        this.arrays[this.posNullChar + 1] = (char) 0x00;
        // this._cstr._posNullChar = this._cstr._posNullChar + 1;
        this.changed = true;

    }

    // 원래 함수는 새로운 char *값 생성하는 듯
    public CStringPt lTrim() {
        calcNullCharPos(this);
        while (index < this.arrays.length && index < this.posNullChar
                && Character.isWhitespace(this.arrays[index]))
            index++;
        this.changed = true;
        return this;
    }

    // 원래 함수는 새로운 char *값 생성하는 듯
    public CStringPt rTrim() {
        calcNullCharPos(this);
        int p_idx = this.posNullChar;
        while (--p_idx >= index && Character.isWhitespace(this.arrays[p_idx])) {
        }
        this.arrays[p_idx + 1] = '\0';
        this.posNullChar = p_idx + 1;
        this.changed = true;
        return this;
    }

    public CStringPt trim() {
        rTrim();
        lTrim();
        return this;
    }

    public static void clear(CStringPt source, int start, int count) {
        source.str = null;
        if (source.isEmpty()) {
            if (source.arrays == null) {
                source.arrays = null;
                source.changed = false;
                source.posNullChar = -1;
            }
            source.arrays = new char[start + count];
            source.changed = true;
        }
        Arrays.fill(source.arrays, start, start + count - 1, (char) 0);
        source.changed = true;
    }

    public static void clear(CStringPt source) {
        Arrays.fill(source.arrays, (char) 0);
    }

    // c문자열 기준 길이
    // strlen
    public int length() {
        calcNullCharPos(this);
        return this.posNullChar - index;
    }

    public int lengthWithNull() {
        calcNullCharPos(this);
        return this.posNullChar - index + 1;
    }

    @Override
    public String toString() {
        if (str == null || changed || this.posNullChar - index != str.length()) {
            calcNullCharPos(this);
            str = new String(this.arrays, index, this.posNullChar - index);
        }
        this.changed = false;
        return str;
    }

    public String getString() {
        return toString();
    }

    // null 문자를 마지막에 연결
    public byte[] getAsciiBytes(boolean terminateWithNull) {
        byte[] ret = new byte[this.length() + 1];
        for (int i = index; i < ret.length - 1; i++) {
            ret[i] = (byte) this.arrays[i];
        }
        ret[ret.length - 1] = 0x00;
        return ret;
    }

    public byte[] getAsciiBytes() {
        return getAsciiBytes(true);
    }


    // 바이트를 그대로 복사, 바이트 주입
    public void setBytes(byte[] data) {
        int i = index;
        for (byte val : data) {
            this.arrays[i++] = (char) val;
        }
    }

    public void setBytes(byte[] data, int data_start, int count) {

        for (int i = 0; i < count; i++) {
            this.arrays[index + i] = (char) data[data_start + i];
        }
    }


    public void upper() {
        calcNullCharPos(this);
        for (int i = index; i < this.posNullChar; i++) {
            this.arrays[i] = Character.toUpperCase(this.arrays[i]);
        }
    }

    // cstring 값 사이의 간격
    // cstring1 - cstring2
    public static int diff(CStringPt cstring1, CStringPt cstring2) {
        return cstring1.index - cstring2.index;
    }

    // _posNullChar을 사용하기 전에 호출
    private static void calcNullCharPos(CStringPt source) {
        // if (source.changed)
        source.posNullChar =
                ArrayHelper.indexOf(source.arrays, source.index, source.arrays.length, (char) 0);
        source.changed = false;
    }

    // source의 문자열을 null문자까지 복사
    // strcpy
    public static CStringPt copy(CStringPt source, CStringPt destination) {
        int length = source.length() + 1;// null문자까지 복사
        char[] src = source.arrays;
        int srcIdx = source.index;
        char[] dest = destination.arrays;
        int destIdx = destination.index;
        for (int i = 0; i < length; i++) {
            dest[i + destIdx] = src[i + srcIdx];
        }
        destination.changed = true;
        // CalcNullCharPos( destination);
        return destination;
    }

    public static CStringPt copy(String source, CStringPt destination) {
        int length = source.length();// null문자까지 복사
        char[] dest = destination.arrays;
        int destIdx = destination.index;
        for (int i = 0; i < length; i++) {
            dest[i + destIdx] = source.charAt(i);
        }
        destination.set(source.length(), (char) 0);
        destination.changed = true;
        // CalcNullCharPos( destination);
        return destination;
    }

    // 부분 복사, 복사한 값의 마지막에 null 문자가 온다.
    // 나머지 부분은 클리어
    // strncpy
    public static CStringPt safeCopy(CStringPt source, CStringPt destination, int count) {
        int srcLen = source.arrays.length - source.index;
        int cpyLen = count > srcLen ? srcLen : count - 1;
        for (int i = 0; i < cpyLen; i++) {
            destination.arrays[i + destination.index] = source.arrays[i + source.index];
        }
        int remain = (destination.arrays.length - destination.index) - cpyLen;
        // 0으로 클리어
        if (remain > 0)
            Arrays.fill(destination.arrays, destination.arrays.length - remain,
                    destination.arrays.length - 1, (char) 0);
        destination.changed = true;
        // CalcNullCharPos( destination);
        return destination;
    }


    public static CStringPt safeCopy(String source, CStringPt destination, int count) {
        for (int i = 0; i < count - 1; i++) {
            destination.set(i, source.charAt(i));
        }
        int remain = destination.arrays.length - (count - 1);
        // 0으로 클리어
        if (remain > 0) {
            int startIdx = destination.index + count - 1;
            int endIdx = startIdx + remain - 1;
            Arrays.fill(destination.arrays, startIdx, endIdx, (char) 0);
        }
        destination.changed = true;
        // CalcNullCharPos( destination);
        return destination;
    }

    // memmove
    // null 문자와 상관없이 내부 값들의 이동
    // memcpy대용으로도 가능
    public static void rawMove(CStringPt source, CStringPt destination, int count) {
        for (int i = 0; i < count; i++) {
            destination.arrays[destination.index + i] = source.arrays[i + source.index];
        }
        // 내부 정보 정리
        destination.changed = true;
        // CalcNullCharPos( destination);
    }

    public static CStringPt create() {
        return new CStringPt();
    }

    public static CStringPt create(int length) {
        if (length == 0)
            return create();
        return new CStringPt(length);
    }

    public static CStringPt create(char[] value) {
        if (value == null)
            return create();
        if (value.length == 0)
            return create();
        int len = value.length + 1;
        CStringPt pt = new CStringPt(len);
        for (int i = 0; i < value.length; i++) {
            pt.arrays[i] = value[i];
        }
        return pt;
    }

    public static CStringPt create(String value) {
        if (value == null)
            return create();
        // if (value.length() == 0) return Create();
        // value.length() == 0 이면 길이가 1인 문자배열 생성; \0만 들어가 있는 문자배열
        int len = value.length() + 1;
        CStringPt pt = new CStringPt(len);
        int cpLen = value.length();
        for (int i = 0; i < cpLen; i++) {
            pt.arrays[i] = value.charAt(i);
        }
        return pt;
    }
}
