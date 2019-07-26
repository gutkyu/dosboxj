package org.gutkyu.dosboxj.dos.mem_block;

import org.gutkyu.dosboxj.dos.DOSMain;
import org.gutkyu.dosboxj.hardware.memory.*;

public final class DOSInfoBlock extends MemStruct {

    public DOSInfoBlock() {
    }

    public void setLocation(short segment) {
        seg = segment;
        pt = Memory.physMake(seg, 0);
        /* Clear the initial Block */
        for (int i = 0; i < sDIM_Size; i++)
            Memory.writeB(pt + i, 0xff);
        for (int i = 0; i < 14; i++)
            Memory.writeB(pt + i, 0);

        saveIt(Size_sDIB_regCXfrom5e, Off_sDIB_regCXfrom5e, 0);
        saveIt(Size_sDIB_countLRUcache, Off_sDIB_countLRUcache, 0);
        saveIt(Size_sDIB_countLRUopens, Off_sDIB_countLRUopens, 0);

        saveIt(Size_sDIB_protFCBs, Off_sDIB_protFCBs, 0);
        saveIt(Size_sDIB_specialCodeSeg, Off_sDIB_specialCodeSeg, 0);
        saveIt(Size_sDIB_joindedDrives, Off_sDIB_joindedDrives, 0);
        // increase this if you add drives to cds-chain
        saveIt(Size_sDIB_lastdrive, Off_sDIB_lastdrive, 0x01);
        saveIt(Size_sDIB_diskInfoBuffer, Off_sDIB_diskInfoBuffer,
                Memory.realMake(segment, Off_sDIB_diskBufferHeadPt));
        saveIt(Size_sDIB_setverPtr, Off_sDIB_setverPtr, 0);

        saveIt(Size_sDIB_a20FixOfs, Off_sDIB_a20FixOfs, 0);
        saveIt(Size_sDIB_pspLastIfHMA, Off_sDIB_pspLastIfHMA, 0);
        saveIt(Size_sDIB_blockDevices, Off_sDIB_blockDevices, 0);

        saveIt(Size_sDIB_bootDrive, Off_sDIB_bootDrive, 0);
        saveIt(Size_sDIB_useDwordMov, Off_sDIB_useDwordMov, 1);
        saveIt(Size_sDIB_extendedSize, Off_sDIB_extendedSize, Memory.totalPages() * 4 - 1024);
        saveIt(Size_sDIB_magicWord, Off_sDIB_magicWord, 0x0001); // dos5+

        saveIt(Size_sDIB_sharingCount, Off_sDIB_sharingCount, 0);
        saveIt(Size_sDIB_sharingDelay, Off_sDIB_sharingDelay, 0);
        saveIt(Size_sDIB_ptrCONinput, Off_sDIB_ptrCONinput, 0); // no unread input available
        saveIt(Size_sDIB_maxSectorLength, Off_sDIB_maxSectorLength, 0x200);

        saveIt(Size_sDIB_dirtyDiskBuffers, Off_sDIB_dirtyDiskBuffers, 0);
        saveIt(Size_sDIB_lookaheadBufPt, Off_sDIB_lookaheadBufPt, 0);
        saveIt(Size_sDIB_lookaheadBufNumber, Off_sDIB_lookaheadBufNumber, 0);
        // buffer in base memory, no workspace
        saveIt(Size_sDIB_bufferLocation, Off_sDIB_bufferLocation, 0);
        saveIt(Size_sDIB_workspaceBuffer, Off_sDIB_workspaceBuffer, 0);

        saveIt(Size_sDIB_minMemForExec, Off_sDIB_minMemForExec, 0);
        saveIt(Size_sDIB_memAllocScanStart, Off_sDIB_memAllocScanStart, DOSMain.DOS_MEM_START);
        saveIt(Size_sDIB_startOfUMBChain, Off_sDIB_startOfUMBChain, 0xffff);
        saveIt(Size_sDIB_chainingUMB, Off_sDIB_chainingUMB, 0);

        saveIt(Size_sDIB_nulNextDriver, Off_sDIB_nulNextDriver, 0xffffffff);
        saveIt(Size_sDIB_nulAttributes, Off_sDIB_nulAttributes, 0x8004);
        saveIt(Size_sDIB_nulStrategy, Off_sDIB_nulStrategy, 0x00000000);
        saveIt(1, Off_sDIB_nulString + 0, 0x4e);
        saveIt(1, Off_sDIB_nulString + 1, 0x55);
        saveIt(1, Off_sDIB_nulString + 2, 0x4c);
        saveIt(1, Off_sDIB_nulString + 3, 0x20);
        saveIt(1, Off_sDIB_nulString + 4, 0x20);
        saveIt(1, Off_sDIB_nulString + 5, 0x20);
        saveIt(1, Off_sDIB_nulString + 6, 0x20);
        saveIt(1, Off_sDIB_nulString + 7, 0x20);

        /* Create a fake SFT, so programs think there are 100 file handles */
        short sftOffset = Off_sDIB_firstFileTable + 0xa2;
        saveIt(Size_sDIB_firstFileTable, Off_sDIB_firstFileTable,
                Memory.realMake(segment, sftOffset));
        // Next File Table
        Memory.realWriteD(segment, sftOffset + 0x00, Memory.realMake(segment + 0x26, 0));
        // File Table supports 100 files
        Memory.realWriteW(segment, sftOffset + 0x04, 100);
        Memory.realWriteD(segment + 0x26, 0x00, 0xffffffff); // Last File Table
        // File Table supports 100 files}
        Memory.realWriteW(segment + 0x26, 0x04, 100);
    }

    public void setFirstMCB(short firstmcb) {
        saveIt(Size_sDIB_firstMCB, Off_sDIB_firstMCB, firstmcb); // c2woody
    }

    // (short, short)
    public void setBuffers(int x, int y) {
        saveIt(Size_sDIB_buffers_x, Off_sDIB_buffers_x, x);
        saveIt(Size_sDIB_buffers_y, Off_sDIB_buffers_y, y);
    }

    public void setCurDirStruct(int curDirStruct) {
        saveIt(Size_sDIB_curDirStructure, Off_sDIB_curDirStructure, curDirStruct);
    }

    public void setFCBTable(int fcbTable) {
        saveIt(Size_sDIB_fcbTable, Off_sDIB_fcbTable, fcbTable);
    }

    public void setDeviceChainStart(int devChain) {
        saveIt(Size_sDIB_nulNextDriver, Off_sDIB_nulNextDriver, devChain);
    }

    public void setDiskBufferHeadPt(int dbHeadPt) {
        saveIt(Size_sDIB_diskBufferHeadPt, Off_sDIB_diskBufferHeadPt, dbHeadPt);
    }

    public void setStartOfUMBChain(int umbStartSeg) {
        saveIt(Size_sDIB_startOfUMBChain, Off_sDIB_startOfUMBChain, umbStartSeg);
    }

    public void setUMBChainState(int umbChaining) {
        saveIt(Size_sDIB_chainingUMB, Off_sDIB_chainingUMB, umbChaining);
    }

    // uint16()
    public int getStartOfUMBChain() {
        return getIt(Size_sDIB_startOfUMBChain, Off_sDIB_startOfUMBChain);
    }

    // uint8()
    public int getUMBChainState() {
        return 0xff & getIt(Size_sDIB_chainingUMB, Off_sDIB_chainingUMB);
    }

    public int getPointer() {
        return Memory.realMake(seg, Off_sDIB_firstDPB);
    }

    public int getDeviceChain() {
        return getIt(Size_sDIB_nulNextDriver, Off_sDIB_nulNextDriver);
    }

    // -- #region sDIB
    // ------------------------------------ struct sDIB start ----------------------------------//
    private static final int Size_sDIB_unknown1 = 4;
    private static final int Size_sDIB_magicWord = 2;
    private static final int Size_sDIB_unknown2 = 8;
    private static final int Size_sDIB_regCXfrom5e = 2;
    private static final int Size_sDIB_countLRUcache = 2;
    private static final int Size_sDIB_countLRUopens = 2;
    private static final int Size_sDIB_stuff = 6;
    private static final int Size_sDIB_sharingCount = 2;
    private static final int Size_sDIB_sharingDelay = 2;
    private static final int Size_sDIB_diskBufPtr = 4;
    private static final int Size_sDIB_ptrCONinput = 2;
    private static final int Size_sDIB_firstMCB = 2;
    private static final int Size_sDIB_firstDPB = 4;
    private static final int Size_sDIB_firstFileTable = 4;
    private static final int Size_sDIB_activeClock = 4;
    private static final int Size_sDIB_activeCon = 4;
    private static final int Size_sDIB_maxSectorLength = 2;
    private static final int Size_sDIB_diskInfoBuffer = 4;
    private static final int Size_sDIB_curDirStructure = 4;
    private static final int Size_sDIB_fcbTable = 4;
    private static final int Size_sDIB_protFCBs = 2;
    private static final int Size_sDIB_blockDevices = 1;
    private static final int Size_sDIB_lastdrive = 1;
    private static final int Size_sDIB_nulNextDriver = 4;
    private static final int Size_sDIB_nulAttributes = 2;
    private static final int Size_sDIB_nulStrategy = 4;
    private static final int Size_sDIB_nulString = 8;
    private static final int Size_sDIB_joindedDrives = 1;
    private static final int Size_sDIB_specialCodeSeg = 2;
    private static final int Size_sDIB_setverPtr = 4;
    private static final int Size_sDIB_a20FixOfs = 2;
    private static final int Size_sDIB_pspLastIfHMA = 2;
    private static final int Size_sDIB_buffers_x = 2;
    private static final int Size_sDIB_buffers_y = 2;
    private static final int Size_sDIB_bootDrive = 1;
    private static final int Size_sDIB_useDwordMov = 1;
    private static final int Size_sDIB_extendedSize = 2;
    private static final int Size_sDIB_diskBufferHeadPt = 4;
    private static final int Size_sDIB_dirtyDiskBuffers = 2;
    private static final int Size_sDIB_lookaheadBufPt = 4;
    private static final int Size_sDIB_lookaheadBufNumber = 2;
    private static final int Size_sDIB_bufferLocation = 1;
    private static final int Size_sDIB_workspaceBuffer = 4;
    private static final int Size_sDIB_unknown3 = 11;
    private static final int Size_sDIB_chainingUMB = 1;
    private static final int Size_sDIB_minMemForExec = 2;
    private static final int Size_sDIB_startOfUMBChain = 2;
    private static final int Size_sDIB_memAllocScanStart = 2;

    private static final int Off_sDIB_unknown1 = 0;
    private static final int Off_sDIB_magicWord = 4;// -0x22 needs to be 1
    private static final int Off_sDIB_unknown2 = 6;
    private static final int Off_sDIB_regCXfrom5e = 14; // -0x18 CX from last int21/ah=5e
    private static final int Off_sDIB_countLRUcache = 16; // -0x16 LRU counter for FCB caching
    private static final int Off_sDIB_countLRUopens = 18; // -0x14 LRU counter for FCB openings
    private static final int Off_sDIB_stuff = 20; // -0x12 some stuff, hopefully never used....
    private static final int Off_sDIB_sharingCount = 26; // -0x0c sharing retry count
    private static final int Off_sDIB_sharingDelay = 28; // -0x0a sharing retry delay
    private static final int Off_sDIB_diskBufPtr = 30; // -0x08 pointer to disk buffer
    private static final int Off_sDIB_ptrCONinput = 34; // -0x04 pointer to con input
    private static final int Off_sDIB_firstMCB = 36; // -0x02 first memory control block
    private static final int Off_sDIB_firstDPB = 38; // 0x00 first drive parameter block
    private static final int Off_sDIB_firstFileTable = 42; // 0x04 first system file table
    private static final int Off_sDIB_activeClock = 46; // 0x08 active clock device header
    private static final int Off_sDIB_activeCon = 50; // 0x0c active console device header
    private static final int Off_sDIB_maxSectorLength = 54; // 0x10 maximum bytes per sector of any
                                                            // block device;
    private static final int Off_sDIB_diskInfoBuffer = 56; // 0x12 pointer to disk info buffer
    private static final int Off_sDIB_curDirStructure = 60; // 0x16 pointer to current array of
                                                            // directory structure
    private static final int Off_sDIB_fcbTable = 64; // 0x1a pointer to system FCB table
    private static final int Off_sDIB_protFCBs = 68; // 0x1e protected fcbs
    private static final int Off_sDIB_blockDevices = 70; // 0x20 installed block devices
    private static final int Off_sDIB_lastdrive = 71; // 0x21 lastdrive
    private static final int Off_sDIB_nulNextDriver = 72; // 0x22 NUL driver next pointer
    private static final int Off_sDIB_nulAttributes = 76; // 0x26 NUL driver aattributes
    private static final int Off_sDIB_nulStrategy = 78; // 0x28 NUL driver strategy routine
    private static final int Off_sDIB_nulString = 82; // 0x2c NUL driver name string
    private static final int Off_sDIB_joindedDrives = 90; // 0x34 joined drives
    private static final int Off_sDIB_specialCodeSeg = 91; // 0x35 special code segment
    private static final int Off_sDIB_setverPtr = 93; // 0x37 pointer to setver
    private static final int Off_sDIB_a20FixOfs = 97; // 0x3b a20 fix routine offset
    private static final int Off_sDIB_pspLastIfHMA = 99; // 0x3d psp of last program (if dos in hma)
    private static final int Off_sDIB_buffers_x = 101; // 0x3f x in BUFFERS x,y
    private static final int Off_sDIB_buffers_y = 103; // 0x41 y in BUFFERS x,y
    private static final int Off_sDIB_bootDrive = 105; // 0x43 boot drive
    private static final int Off_sDIB_useDwordMov = 106; // 0x44 use dword moves
    private static final int Off_sDIB_extendedSize = 107; // 0x45 size of extended memory
    private static final int Off_sDIB_diskBufferHeadPt = 109; // 0x47 pointer to least-recently used
                                                              // buffer header
    private static final int Off_sDIB_dirtyDiskBuffers = 113; // 0x4b number of dirty disk buffers
    private static final int Off_sDIB_lookaheadBufPt = 115; // 0x4d pointer to lookahead buffer
    private static final int Off_sDIB_lookaheadBufNumber = 119; // 0x51 number of lookahead buffers
    private static final int Off_sDIB_bufferLocation = 121; // 0x53 workspace buffer location
    private static final int Off_sDIB_workspaceBuffer = 122; // 0x54 pointer to workspace buffer
    private static final int Off_sDIB_unknown3 = 126; // 0x58
    private static final int Off_sDIB_chainingUMB = 137; // 0x63 bit0: UMB chain linked to MCB chain
    private static final int Off_sDIB_minMemForExec = 138; // 0x64 minimum paragraphs needed for
                                                           // current program
    private static final int Off_sDIB_startOfUMBChain = 140; // 0x66 segment of first UMB-MCB
    private static final int Off_sDIB_memAllocScanStart = 142; // 0x68 start paragraph for memory
                                                               // allocation

    private static final int sDIM_Size = 144;
    // ------------------------------------ struct sDIB end ----------------------------------//

    // -- #endregion

    public short seg;

}
