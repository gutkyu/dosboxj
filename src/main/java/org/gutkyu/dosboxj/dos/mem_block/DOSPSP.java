package org.gutkyu.dosboxj.dos.mem_block;



import org.gutkyu.dosboxj.hardware.memory.*;
import org.gutkyu.dosboxj.dos.*;


public final class DOSPSP extends MemStruct {
    // uint16
    public DOSPSP(int segment) {
        setSegPt(segment);
        seg = segment;
    }

    // (uint16)
    public void makeNew(int memSize) {
        /* get previous */
        // DOS_PSP prevpsp(dos.psp());
        /* Clear it first */
        int i;
        for (i = 0; i < sPSP_Size; i++)
            Memory.writeB(pt + i, 0);
        // Set size
        // sSave(sPSP,next_seg,seg+memSize);
        saveIt(Size_next_seg, Off_next_seg, seg + memSize);

        /* far call opcode */
        // sSave(sPSP,far_call,0xea);
        saveIt(Size_far_call, Off_far_call, 0xea);

        // far call to interrupt 0x21 - faked for bill & ted
        // lets hope nobody really uses this address
        // sSave(sPSP,cpm_entry,RealMake(0xDEAD,0xFFFF));
        saveIt(Size_cpm_entry, Off_cpm_entry, Memory.realMake(0xDEAD, 0xFFFF));
        /* Standard blocks,int 20 and int21 retf */
        // sSave(sPSP,exit[0],0xcd);
        saveIt(1, Off_Exit, 0xcd);
        // sSave(sPSP,exit[1],0x20);
        saveIt(1, Off_Exit + 1, 0x20);
        // sSave(sPSP,service[0],0xcd);
        saveIt(1, Off_service, 0xcd);
        // sSave(sPSP,service[1],0x21);
        saveIt(1, Off_service + 1, 0x21);
        // sSave(sPSP,service[2],0xcb);
        saveIt(1, Off_service + 2, 0xcb);
        /* psp and psp-parent */
        // sSave(sPSP,psp_parent,dos.psp());
        saveIt(Size_psp_parent, Off_psp_parent, DOSMain.DOS.getPSP());
        // sSave(sPSP,prev_psp,0xffffffff);
        saveIt(Size_prev_psp, Off_prev_psp, 0xffffffff);
        // sSave(sPSP,dos_version,0x0005);
        saveIt(Size_dos_version, Off_dos_version, 0x0005);
        /* terminate 22,break 23,crititcal error 24 address stored */
        saveVectors();

        /* FCBs are filled with 0 */
        // ....
        /* Init file pointer and max_files */
        // sSave(sPSP,file_table,MemModule.RealMake(seg,offsetof(sPSP,files)));
        saveIt(Size_file_table, Off_file_table, Memory.realMake(seg, Off_files));
        // sSave(sPSP,max_files,20);
        saveIt(Size_max_files, Off_max_files, 20);
        for (short ct = 0; ct < 20; ct++)
            setFileHandle(ct, 0xff);

        /* User Stack pointer */
        // if (prevpsp.GetSegment()!=0) sSave(sPSP,stack,prevpsp.GetStack());

        if (rootpsp == 0)
            rootpsp = seg;
    }

    public void copyFileTable(DOSPSP srcPSP, boolean createChildPSP) {
        /* Copy file table from calling process */
        for (short i = 0; i < 20; i++) {
            int handle = srcPSP.getFileHandle(i);
            if (createChildPSP) { // copy obeying not inherit flag.(but dont duplicate them)
                // (handle==0) || ((handle>0) && (FindEntryByHandle(handle)==0xff));
                boolean allowCopy = true;
                if ((handle < DOSMain.DOS_FILES) && DOSMain.Files[handle] != null
                        && (DOSMain.Files[handle].Flags & DOSSystem.DOS_NOT_INHERIT) == 0
                        && allowCopy) {
                    DOSMain.Files[handle].addRef();
                    setFileHandle(i, handle);
                } else {
                    setFileHandle(i, 0xff);
                }
            } else { // normal copy so don't mind the inheritance
                setFileHandle(i, handle);
            }
        }
    }

    public int findFreeFileEntry() {
        int files = Memory.real2Phys(getIt(Size_file_table, Off_file_table));
        for (int i = 0; i < getIt(Size_max_files, Off_max_files); i++) {
            if (Memory.readB(files + i) == 0xff)
                return i;
        }
        return 0xff;
    }

    public void closeFiles() {
        for (short i = 0; i < getIt(Size_max_files, Off_max_files); i++) {
            DOSMain.closeFile(i);
        }
    }

    public void saveVectors() {
        /* Save interrupt 22,23,24 */
        // sSave(sPSP,int_22,RealGetVec(0x22));
        // sSave(sPSP,int_23,RealGetVec(0x23));
        // sSave(sPSP,int_24,RealGetVec(0x24));
        saveIt(Size_int_22, Off_int_22, Memory.realGetVec(0x22));
        saveIt(Size_int_23, Off_int_23, Memory.realGetVec(0x23));
        saveIt(Size_int_24, Off_int_24, Memory.realGetVec(0x24));
    }

    public void restoreVectors() {
        /* Restore interrupt 22,23,24 */
        // RealSetVec(0x22,sGet(sPSP,int_22));
        // RealSetVec(0x23,sGet(sPSP,int_23));
        // RealSetVec(0x24,sGet(sPSP,int_24));
        Memory.realSetVec(0x22, getIt(Size_int_22, Off_int_22));
        Memory.realSetVec(0x23, getIt(Size_int_23, Off_int_23));
        Memory.realSetVec(0x24, getIt(Size_int_24, Off_int_24));
    }

    // public void SetSize(short size) {
    public void setSize(int size) {
        saveIt(Size_next_seg, Off_next_seg, size);
    }

    public int getSize() {
        return getIt(Size_next_seg, Off_next_seg);
    }

    public void setEnvironment(int envSeg) {
        saveIt(Size_environment, Off_environment, envSeg);
    }

    public int getEnvironment() {
        return getIt(Size_environment, Off_environment);
    }

    // uint16
    public int getSegment() {
        return seg;
    }

    // (uint16, byte )
    public void setFileHandle(int index, int handle) {
        if (index < getIt(Size_max_files, Off_max_files)) {
            int files = Memory.real2Phys(getIt(Size_file_table, Off_file_table));
            Memory.writeB(files + index, handle);
        }
    }

    // byte (uint16)
    public int getFileHandle(int index) {
        if (index >= getIt(Size_max_files, Off_max_files))
            return 0xff;
        int files = Memory.real2Phys(getIt(Size_file_table, Off_file_table));
        return Memory.readB(files + index);
    }

    public void setParent(int parent) {
        saveIt(Size_psp_parent, Off_psp_parent, parent);
    }

    // uint16
    public int getParent() {
        return 0xffff & getIt(Size_psp_parent, Off_psp_parent);
    }

    public void setStack(int stackPt) {
        saveIt(Size_stack, Off_stack, stackPt);
    }

    public int getStack() {
        return getIt(Size_stack, Off_stack);
    }

    public void setINT22(int int22Pt) {
        saveIt(Size_int_22, Off_int_22, int22Pt);
    }

    public int getINT22() {
        return getIt(Size_int_22, Off_int_22);
    }

    public void setFCB1(int src) {
        if (src != 0)
            Memory.blockCopy(Memory.physMake(seg, Off_fcb1), Memory.real2Phys(src), 16);
    }

    public void setFCB2(int src) {
        if (src != 0)
            Memory.blockCopy(Memory.physMake(seg, Off_fcb2), Memory.real2Phys(src), 16);
    }

    public void setCommandTail(int src) {
        if (src != 0) { // valid source
            Memory.blockCopy(pt + Off_CommandTail, Memory.real2Phys(src), 128);
        } else { // empty
            saveIt(Size_CommandTail_count, Off_CommandTail_count, 0x00);
            Memory.writeB(pt + Off_CommandTail_buffer, 0x0d);
        }
    }

    // public boolean SetNumFiles(short fileNum) {
    public boolean setNumFiles(int fileNum) {
        if (fileNum > 20) {
            // Allocate needed paragraphs
            fileNum += 2; // Add a few more files for safety
            int para = 0xffff & ((fileNum / 16) + ((fileNum % 16) > 0 ? 1 : 0));
            int data = Memory.realMake(DOSMain.getMemory(para), 0);
            saveIt(Size_file_table, Off_file_table, data);
            saveIt(Size_max_files, Off_max_files, fileNum);
            short i;
            for (i = 0; i < 20; i++)
                setFileHandle(i, getIt(1, Off_files + i));
            for (i = 20; i < fileNum; i++)
                setFileHandle(i, 0xFF);
        } else {
            saveIt(Size_max_files, Off_max_files, fileNum);
        }
        return true;
    }

    public short findEntryByHandle(byte handle) {
        int files = Memory.real2Phys(getIt(Size_file_table, Off_file_table));
        for (short i = 0; i < getIt(Size_max_files, Off_max_files); i++) {
            if (Memory.readB(files + i) == handle)
                return i;
        }
        return 0xFF;
    }

    // ------------------------------------ struct sPSP start ----------------------------------//

    private static final int Size_Exit = 2;/* CP/M-like exit poimt */
    private static final int Size_next_seg = 2; // Segment of first byte beyond memory allocated or
                                                // program
    private static final int Size_fill_1 = 1; /* single char fill */
    private static final int Size_far_call = 1; /* far call opcode */
    private static final int Size_cpm_entry = 4; /* CPM Service Request address */
    private static final int Size_int_22 = 4; /* Terminate Address */
    private static final int Size_int_23 = 4; /* Break Address */
    private static final int Size_int_24 = 4; /* Critical Error Address */
    private static final int Size_psp_parent = 2; /* Parent PSP Segment */
    private static final int Size_files = 20; /* File Table - 0xff is unused */
    private static final int Size_environment = 2; /* Segment of evironment table */
    private static final int Size_stack = 4; /* SS:SP Save point for int 0x21 calls */
    private static final int Size_max_files = 2; /* Maximum open files */
    private static final int Size_file_table = 4; /* Pointer to File Table PSP:0x18 */
    private static final int Size_prev_psp = 4; /* Pointer to previous PSP */
    private static final int Size_interim_flag = 1;
    private static final int Size_truename_flag = 1;
    private static final int Size_nn_flags = 2;
    private static final int Size_dos_version = 2;
    private static final int Size_fill_2 = 14; /* Lot's of unused stuff i can't care aboue */
    private static final int Size_service = 3; /* INT 0x21 Service call int 0x21;retf; */
    private static final int Size_fill_3 = 9; /* This has some blocks with FCB info */
    private static final int Size_fcb1 = 16; /* first FCB */
    private static final int Size_fcb2 = 16; /* second FCB */
    private static final int Size_fill_4 = 4; /* unused */
    private static final int Size_CommandTail_count = 1;
    private static final int Size_CommandTail_buffer = 127;

    private static final int Off_Exit = 0;
    private static final int Off_next_seg = 2;
    private static final int Off_fill_1 = 4;
    private static final int Off_far_call = 5;
    private static final int Off_cpm_entry = 6;
    private static final int Off_int_22 = 10;
    private static final int Off_int_23 = 14;
    private static final int Off_int_24 = 18;
    private static final int Off_psp_parent = 22;
    private static final short Off_files = 24;
    private static final int Off_environment = 44;
    private static final int Off_stack = 46;
    private static final int Off_max_files = 50;
    private static final int Off_file_table = 52;
    private static final int Off_prev_psp = 56;
    private static final int Off_interim_flag = 60;
    private static final int Off_truename_flag = 61;
    private static final int Off_nn_flags = 62;
    private static final int Off_dos_version = 64;
    private static final int Off_fill_2 = 66;
    private static final int Off_service = 80;
    private static final int Off_fill_3 = 83;
    private static final short Off_fcb1 = 92;
    private static final short Off_fcb2 = 108;
    private static final int Off_fill_4 = 124;
    private static final int Off_CommandTail = 128;
    private static final int Off_CommandTail_count = 128;
    private static final int Off_CommandTail_buffer = 129;

    private static final int sPSP_Size = 256;

    // ------------------------------------ struct sPSP end ----------------------------------//

    private int seg;// uint16

    /* program Segment prefix */

    public static int rootpsp = 0;// uint16
}
