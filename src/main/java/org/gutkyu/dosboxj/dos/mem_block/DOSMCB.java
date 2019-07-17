package org.gutkyu.dosboxj.dos.mem_block;


import org.gutkyu.dosboxj.hardware.memory.*;
import org.gutkyu.dosboxj.util.*;

public final class DOSMCB extends MemStruct {
    public static final short MCB_FREE = 0x0000;
    public static final short MCB_DOS = 0x0008;

    // uint16
    public DOSMCB(int seg) {
        setSegPt(seg);
    }

    public void setFileName(CStringPt name) {
        Memory.blockWrite(pt + Off_sMCB_filename, name, 8);
    }

    public void getFileName(CStringPt name) {
        Memory.blockRead(pt + Off_sMCB_filename, name, 8);
        name.set(8, (char) 0);
    }

    public void setType(byte type) {
        saveIt(Size_sMCB_type, Off_sMCB_type, type);
    }

    // uint16
    public void setSize(int size) {
        saveIt(Size_sMCB_size, Off_sMCB_size, size);
    }

    public void setPSPSeg(int pspSeg) {
        saveIt(Size_sMCB_psp_segment, Off_sMCB_psp_segment, pspSeg);
    }

    public byte getType() {
        return (byte) getIt(Size_sMCB_type, Off_sMCB_type);
    }

    // uint16
    public int getSize() {
        return getIt(Size_sMCB_size, Off_sMCB_size);
    }

    public int getPSPSeg() {
        return getIt(Size_sMCB_psp_segment, Off_sMCB_psp_segment);
    }

    // ------------------------------------ struct sMCB start ----------------------------------//

    private static final int Size_sMCB_type = 1;
    private static final int Size_sMCB_psp_segment = 2;
    private static final int Size_sMCB_size = 2;
    private static final int Size_sMCB_unused = 3;
    private static final int Size_sMCB_filename = 8;

    private static final int Off_sMCB_type = 0;
    private static final int Off_sMCB_psp_segment = 1;
    private static final int Off_sMCB_size = 3;
    private static final int Off_sMCB_unused = 5;
    private static final int Off_sMCB_filename = 8;

    private static final int sMCB_Size = 16;

    // ------------------------------------ struct sMCB end ----------------------------------//

    // #endif
}
