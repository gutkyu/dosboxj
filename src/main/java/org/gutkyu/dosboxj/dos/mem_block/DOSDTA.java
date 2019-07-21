package org.gutkyu.dosboxj.dos.mem_block;



import org.gutkyu.dosboxj.dos.*;
import org.gutkyu.dosboxj.hardware.memory.*;
import org.gutkyu.dosboxj.util.*;

public final class DOSDTA extends MemStruct {
    public DOSDTA(int addr) {
        setRealPt(addr);
    }

    // (uint8, uint8, string)
    public void setupSearch(int sDrive, int sAttr, String pattern) {
        saveIt(Size_sDTA_sdrive, Off_sDTA_sdrive, sDrive);
        saveIt(Size_sDTA_sattr, Off_sDTA_sattr, sAttr);
        /* Fill with spaces */
        int i;
        for (i = 0; i < 11; i++)
            Memory.writeB(pt + Off_sDTA_sname + i, 0x20);// 아스키 문자 = ' '
        CStringPt csPattern = CStringPt.create(pattern);
        CStringPt findExt = csPattern.positionOf('.');
        if (!findExt.isEmpty()) {
            int size = CStringPt.diff(findExt, csPattern);
            if (size > 8)
                size = 8;
            Memory.blockWrite(pt + Off_sDTA_sname, csPattern);
            findExt.movePtToR1();// find_ext++;
            Memory.blockWrite(pt + Off_sDTA_sext, findExt,
                    findExt.length() > 3 ? 3 : findExt.length());
        } else {
            Memory.blockWrite(pt + Off_sDTA_sname, csPattern,
                    csPattern.length() > 8 ? 8 : csPattern.length());
        }
    }

    public void setupSearch(int sDrive, int sAttr, CStringPt pattern) {
        saveIt(Size_sDTA_sdrive, Off_sDTA_sdrive, sDrive);
        saveIt(Size_sDTA_sattr, Off_sDTA_sattr, sAttr);
        /* Fill with spaces */
        int i;
        for (i = 0; i < 11; i++)
            Memory.writeB(pt + Off_sDTA_sname + i, 0x20);// 아스키 문자 = ' '
        CStringPt findExt = pattern.positionOf('.');
        if (!findExt.isEmpty()) {
            int size = CStringPt.diff(findExt, pattern);
            if (size > 8)
                size = 8;
            Memory.blockWrite(pt + Off_sDTA_sname, pattern, size);
            findExt.movePtToR1(); // find_ext++;
            Memory.blockWrite(pt + Off_sDTA_sext, findExt,
                    findExt.length() > 3 ? 3 : findExt.length());
        } else {
            Memory.blockWrite(pt + Off_sDTA_sname, pattern,
                    pattern.length() > 8 ? 8 : pattern.length());
        }
    }

    // 인자 _name은 크기 고정
    // dos_system.DOS_NAMELENGTH_ASCII
    public void setResult(CStringPt name, int size, int date, int time, int attr) {
        attr &= 0xff;
        Memory.blockWrite(pt + Off_sDTA_name, name, DOSSystem.DOS_NAMELENGTH_ASCII);
        saveIt(Size_sDTA_size, Off_sDTA_size, size);
        saveIt(Size_sDTA_date, Off_sDTA_date, date);
        saveIt(Size_sDTA_time, Off_sDTA_time, time);
        saveIt(Size_sDTA_attr, Off_sDTA_attr, attr);
    }


    public byte getSearchDrive() {
        return (byte) getIt(Size_sDTA_sdrive, Off_sDTA_sdrive);
    }

    // return attr
    // uint8(string)
    public int getSearchParams(CStringPt pattern) {
        int attr = 0xff & getIt(Size_sDTA_sattr, Off_sDTA_sattr);
        byte[] temp = new byte[11];
        Memory.blockRead(pt + Off_sDTA_sname, temp, 0, 11);
        pattern.setBytes(temp, 0, 8);
        pattern.set(8, '.');
        CStringPt.clone(pattern, 9).setBytes(temp, 8, 3);
        pattern.set(12, (char) 0);
        return attr;

    }

    // 4개함수로 분할
    // getResult(CStringPt name, RefU32Ret refSize, RefU16Ret refDate, RefU16Ret refTime, RefU8Ret
    // refAttr)

    // a part of getResult
    public void getResultName(CStringPt name) {
        Memory.blockRead(pt + Off_sDTA_name, name, DOSSystem.DOS_NAMELENGTH_ASCII);
    }

    // a part of getResult
    public int getResultSize() {
        return getIt(Size_sDTA_size, Off_sDTA_size);
    }

    // a part of getResult
    public int getResultDate() {
        return 0xffff & getIt(Size_sDTA_date, Off_sDTA_date);
    }

    // a part of getResult
    public int getResultTime() {
        return 0xffff & getIt(Size_sDTA_time, Off_sDTA_time);
    }

    // a part of getResult
    public int getResultAttr() {
        return 0xff & getIt(Size_sDTA_attr, Off_sDTA_attr);
    }

    // (uint16)
    public void setDirID(int entry) {
        saveIt(Size_sDTA_dirID, Off_sDTA_dirID, entry);
    }

    public void setDirIDCluster(int entry) {
        saveIt(Size_sDTA_dirCluster, Off_sDTA_dirCluster, entry);
    }

    public int getDirID() {
        return 0xffff & getIt(Size_sDTA_dirID, Off_sDTA_dirID);
    }

    public int getDirIDCluster() {
        return 0xffff & getIt(Size_sDTA_dirCluster, Off_sDTA_dirCluster);
    }

    // ------------------------------------ struct sDTA start ----------------------------------//

    private static final int Size_sDTA_sdrive = 1;/* The Drive the search is taking place */
    private static final int Size_sDTA_sname = 8;/* The Search pattern for the filename */
    private static final int Size_sDTA_sext = 3;/* The Search pattern for the extenstion */
    private static final int Size_sDTA_sattr = 1;/* The Attributes that need to be found */
    /* custom: dir-search ID for multiple searches at the same time */
    private static final int Size_sDTA_dirID = 2;
    /* custom (drive_fat only): cluster number for multiple searches at the */
    private static final int Size_sDTA_dirCluster = 2;
    private static final int Size_sDTA_fill = 4;
    private static final int Size_sDTA_attr = 1;
    private static final int Size_sDTA_time = 2;
    private static final int Size_sDTA_date = 2;
    private static final int Size_sDTA_size = 4;
    private static final int Size_sDTA_name = DOSSystem.DOS_NAMELENGTH_ASCII;

    private static final int Off_sDTA_sdrive = 0;
    private static final int Off_sDTA_sname = 1;
    private static final int Off_sDTA_sext = 9;
    private static final int Off_sDTA_sattr = 12;
    private static final int Off_sDTA_dirID = 13;
    private static final int Off_sDTA_dirCluster = 15;
    private static final int Off_sDTA_fill = 17;
    private static final int Off_sDTA_attr = 21;
    private static final int Off_sDTA_time = 22;
    private static final int Off_sDTA_date = 24;
    private static final int Off_sDTA_size = 26;
    private static final int Off_sDTA_name = 30;

    private static final int sDTA_Size = Off_sDTA_name + DOSSystem.DOS_NAMELENGTH_ASCII;

    // ------------------------------------ struct sDTA end ----------------------------------//

}
