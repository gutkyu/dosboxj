package org.gutkyu.dosboxj.dos.mem_block;


import org.gutkyu.dosboxj.hardware.memory.*;
import org.gutkyu.dosboxj.dos.*;
import org.gutkyu.dosboxj.util.*;


public final class DOSFCB extends MemStruct {
    // (int seg, uint16 off)
    public DOSFCB(int seg, int off) {
        this(seg, off, true);
    }

    public DOSFCB(int seg, int off, boolean allowExtended) {
        setSegPt(seg, off);
        real_pt = pt;
        extended = false;
        if (allowExtended) {
            if (getIt(Size_sFCB_drive, Off_sFCB_drive) == 0xff) {
                pt += 7;
                extended = true;
            }
        }
    }

    public void create(boolean extended) {
        int fill;
        if (extended)
            fill = 36 + 7;
        else
            fill = 36;
        int i;
        for (i = 0; i < fill; i++)
            Memory.writeB(real_pt + i, 0);
        pt = real_pt;
        if (extended) {
            Memory.writeB(real_pt, 0xff);
            pt += 7;
            extended = true;
        } else
            extended = false;
    }

    // (uint8, string, string)
    public void setName(int drive, CStringPt fName, CStringPt ext) {
        saveIt(Size_sFCB_drive, Off_sFCB_drive, drive);
        Memory.blockWrite(pt + Off_sFCB_filename, fName, 8);
        Memory.blockWrite(pt + Off_sFCB_ext, ext, 3);
    }

    // (uint32, uint16, uint16)
    public void setSizeDateTime(int size, int date, int time) {
        saveIt(Size_sFCB_filesize, Off_sFCB_filesize, size);
        saveIt(Size_sFCB_date, Off_sFCB_date, date);
        saveIt(Size_sFCB_time, Off_sFCB_time, time);
    }

    // GetSizeDateTime을 3개의 함수로 분할
    public int getSize() {
        return getIt(Size_sFCB_filesize, Off_sFCB_filesize);
    }

    // uint16()
    public int getDate() {
        return 0xffff & getIt(Size_sFCB_date, Off_sFCB_date);
    }

    // uint16()
    public int getTime() {
        return 0xffff & getIt(Size_sFCB_time, Off_sFCB_time);
    }

    public void getName(CStringPt fileName) {
        fileName.set(0, (char) (getDrive() + 'A'));
        fileName.set(1, ':');
        Memory.blockRead(pt + Off_sFCB_filename, CStringPt.clone(fileName, 2), 8);
        fileName.set(10, '.');
        Memory.blockRead(pt + Off_sFCB_ext, CStringPt.clone(fileName, 11), 3);
        fileName.set(14, (char) 0);
    }

    public void getName(byte[] fillName) {
        fillName[0] = (byte) (getDrive() + 0x41);// 'A'
        fillName[1] = 0x3a;// ':'
        Memory.blockRead(pt + Off_sFCB_filename, fillName, 2, 8);
        fillName[10] = 0x2e;// '.'
        Memory.blockRead(pt + Off_sFCB_ext, fillName, 11, 3);
        fillName[14] = 0;
    }

    public void getName(byte[] fillName, int fillNameIdx) {
        fillName[fillNameIdx + 0] = (byte) (getDrive() + 0x41);// 'A'
        fillName[fillNameIdx + 1] = 0x3a;// ':'
        Memory.blockRead(pt + Off_sFCB_filename, fillName, (int) (fillNameIdx + 2), 8);
        fillName[fillNameIdx + 10] = 0x2e;// '.'
        Memory.blockRead(pt + Off_sFCB_ext, fillName, (int) (fillNameIdx + 11), 3);
        fillName[fillNameIdx + 14] = 0;
    }

    public void openFile(byte fHandle) {
        saveIt(Size_sFCB_drive, Off_sFCB_drive, getDrive() + 1);
        saveIt(Size_sFCB_file_handle, Off_sFCB_file_handle, fHandle);
        saveIt(Size_sFCB_cur_block, Off_sFCB_cur_block, 0);
        saveIt(Size_sFCB_rec_size, Off_sFCB_rec_size, 128);
        // SaveIt(sFCB,rndm,0); // breaks Jewels of darkness.
        int temp = DOSMain.realHandle(fHandle);
        long size = 0;
        size = DOSMain.Files[temp].seek(size, DOSSystem.DOS_SEEK_END);
        saveIt(Size_sFCB_filesize, Off_sFCB_filesize, (int) size);
        size = 0;
        size = DOSMain.Files[temp].seek(size, DOSSystem.DOS_SEEK_SET);
        saveIt(Size_sFCB_time, Off_sFCB_time, DOSMain.Files[temp].Time);
        saveIt(Size_sFCB_date, Off_sFCB_date, DOSMain.Files[temp].Date);
    }

    public byte closeFile() {
        byte fHandle = (byte) getIt(Size_sFCB_file_handle, Off_sFCB_file_handle);
        saveIt(Size_sFCB_file_handle, Off_sFCB_file_handle, 0xff);
        return fHandle;
    }

    // GetRecord 함수 분리, curBlock는 GetRecord함수로 반환처리
    // uint8()
    public int getRecord() {
        return 0xff & getIt(Size_sFCB_cur_rec, Off_sFCB_cur_rec);

    }

    // GetRecord 함수 분리, curBlock만 반환
    // uint16()
    public int getBlock() {
        return 0xffff & getIt(Size_sFCB_cur_block, Off_sFCB_cur_block);
    }

    // (uint16, uint8)
    public void setRecord(int curBlock, int curRec) {
        saveIt(Size_sFCB_cur_block, Off_sFCB_cur_block, 0xffff & curBlock);
        saveIt(Size_sFCB_cur_rec, Off_sFCB_cur_rec, 0xff & curRec);
    }

    // 두개의 함수로 분할
    // uint8()
    public int getSeqDataFileHandle() {
        return 0xff & getIt(Size_sFCB_file_handle, Off_sFCB_file_handle);
    }

    // uint16()
    public int getSeqDataFileSize() {
        return 0xffff & getIt(Size_sFCB_rec_size, Off_sFCB_rec_size);
    }

    public int getRandom() {
        return getIt(Size_sFCB_rndm, Off_sFCB_rndm);
    }

    public void setRandom(int random) {
        saveIt(Size_sFCB_rndm, Off_sFCB_rndm, random);
    }

    public byte getDrive() {
        byte drive = (byte) getIt(Size_sFCB_drive, Off_sFCB_drive);
        if (drive == 0)
            return DOSMain.getDefaultDrive();
        else
            return (byte) (drive - 1);
    }

    public boolean extended() {
        return extended;
    }

    // uint8
    public int getAttr() {
        if (extended)
            return 0xff & Memory.readB(pt - 1);
        return 0;
    }

    // (uint8)
    public void setAttr(int attr) {
        if (extended)
            Memory.writeB(pt - 1, attr);
    }

    public boolean valid() {
        // Very simple check for Oubliette
        if (getIt(1, Off_sFCB_filename) == 0
                && getIt(Size_sFCB_file_handle, Off_sFCB_file_handle) == 0)
            return false;
        return true;
    }

    private boolean extended;
    private int real_pt;

    // ------------------------------------ struct sFCB start ----------------------------------//

    private static final int Size_sFCB_drive = 1; /* Drive number 0=default, 1=A, etc */
    private static final int Size_sFCB_filename = 8;/* Space padded name */
    private static final int Size_sFCB_ext = 3;/* Space padded extension */
    private static final int Size_sFCB_cur_block = 2;/* Current Block */
    private static final int Size_sFCB_rec_size = 2;/* Logical record size */
    private static final int Size_sFCB_filesize = 4;/* File Size */
    private static final int Size_sFCB_date = 2;
    private static final int Size_sFCB_time = 2;
    /* Reserved Block should be 8 bytes */
    /* start */
    private static final int Size_sFCB_sft_entries = 1;
    private static final int Size_sFCB_share_attributes = 1;
    private static final int Size_sFCB_extra_info = 1;
    private static final int Size_sFCB_file_handle = 1;
    private static final int Size_sFCB_reserved = 4;
    /* end */
    private static final int Size_sFCB_cur_rec = 1;/* Current record in current block */
    private static final int Size_sFCB_rndm = 4;

    private static final int Off_sFCB_drive = 0;
    private static final int Off_sFCB_filename = 1;
    private static final int Off_sFCB_ext = 9;
    private static final int Off_sFCB_cur_block = 12;
    private static final int Off_sFCB_rec_size = 14;
    private static final int Off_sFCB_filesize = 16;
    private static final int Off_sFCB_date = 20;
    private static final int Off_sFCB_time = 22;
    private static final int Off_sFCB_sft_entries = 24;
    private static final int Off_sFCB_share_attributes = 25;
    private static final int Off_sFCB_extra_info = 26;
    private static final int Off_sFCB_file_handle = 27;
    private static final int Off_sFCB_reserved = 28;
    private static final int Off_sFCB_cur_rec = 32;
    private static final int Off_sFCB_rndm = 33;

    private static final int sFCB_Size = 37;

    // ------------------------------------ struct sFCB end ----------------------------------//

}
