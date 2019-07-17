package org.gutkyu.dosboxj.dos.system.device;

import org.gutkyu.dosboxj.dos.DOSMain;
import org.gutkyu.dosboxj.hardware.memory.*;
import org.gutkyu.dosboxj.util.*;
import org.gutkyu.dosboxj.interrupt.*;

public final class DeviceEMM extends DOSDevice {

    private static int GEMMIS_seg;

    public DeviceEMM() {
        setName(CStringPt.create("EMMXXXX0"));
        GEMMIS_seg = 0;
    }

    public static void clearGEMMIS() {
        GEMMIS_seg = 0;
    }

    @Override
    public boolean read(byte[] buf, int offset, int size) {
        return false;
    }

    @Override
    public boolean read() {
        return false;
    }

    @Override
    public byte getReadByte() {
        return 0;
    }

    @Override
    public int readSize() {
        return 0;
    }

    @Override
    public boolean write(byte[] buf, int offset, int size) {
        Log.logging(Log.LogTypes.IOCTL, Log.LogServerities.Normal, "EMS:Write to device");
        return false;
    }

    @Override
    public boolean write(byte value, int size) {
        Log.logging(Log.LogTypes.IOCTL, Log.LogServerities.Normal, "EMS:Write to device");
        return false;
    }

    @Override
    public boolean write(byte value) {
        Log.logging(Log.LogTypes.IOCTL, Log.LogServerities.Normal, "EMS:Write to device");
        return false;
    }

    @Override
    public int writtenSize() {
        Log.logging(Log.LogTypes.IOCTL, Log.LogServerities.Normal, "EMS:get writtenSize");
        return 0;
    }

    @Override
    public long seek(long pos, int type) {
        return -1;
    }

    @Override
    public boolean close() {
        return false;
    }

    @Override
    public int getInformation() {
        return 0xc080;
    }

    // 실패하면 code -1 리턴
    @Override
    public int readFromControlChannel(int bufPtr, int size) {
        short retCode = -1;
        int subfct = Memory.readB(bufPtr);
        switch (subfct) {
            case 0x00:
                if (size != 6)
                    return retCode;
                Memory.writeW(bufPtr + 0x00, 0x0023); // ID
                Memory.writeD(bufPtr + 0x02, 0); // private API entry point
                return retCode = 6;
            case 0x01: {
                if (size != 6)
                    return retCode;
                if (GEMMIS_seg == 0)
                    GEMMIS_seg = DOSMain.getMemory(0x20);
                int GEMMIS_addr = Memory.physMake(GEMMIS_seg, 0);

                Memory.writeW(GEMMIS_addr + 0x00, 0x0004); // flags
                Memory.writeW(GEMMIS_addr + 0x02, 0x019d); // size of this structure
                Memory.writeW(GEMMIS_addr + 0x04, EMS.GEMMIS_VERSION); // version 1.0 (provide ems
                                                                       // information only)
                Memory.writeD(GEMMIS_addr + 0x06, 0); // reserved

                /* build non-EMS frames (0-0xe000) */
                for (int frct = 0; frct < EMS.EMM_PAGEFRAME4K / 4; frct++) {
                    Memory.writeB(GEMMIS_addr + 0x0a + frct * 6, 0x00); // frame type: NONE
                    Memory.writeB(GEMMIS_addr + 0x0b + frct * 6, 0xff); // owner: NONE
                    Memory.writeW(GEMMIS_addr + 0x0c + frct * 6, 0xffff); // non-EMS frame
                    Memory.writeB(GEMMIS_addr + 0x0e + frct * 6, 0xff); // EMS page number
                                                                        // (NONE)
                    Memory.writeB(GEMMIS_addr + 0x0f + frct * 6, 0xaa); // flags: direct
                                                                        // mapping
                }
                /* build EMS page frame (0xe000-0xf000) */
                for (int frct = 0; frct < 0x10 / 4; frct++) {
                    int frnr = (int) (frct + EMS.EMM_PAGEFRAME4K / 4) * 6;
                    Memory.writeB(GEMMIS_addr + 0x0a + frnr, 0x03); // frame type: EMS frame
                                                                    // in 64k page
                    Memory.writeB(GEMMIS_addr + 0x0b + frnr, 0xff); // owner: NONE
                    Memory.writeW(GEMMIS_addr + 0x0c + frnr, 0x7fff); // no logical page
                                                                      // number
                    Memory.writeB(GEMMIS_addr + 0x0e + frnr, frct & 0xff); // physical EMS
                                                                           // page number
                    Memory.writeB(GEMMIS_addr + 0x0f + frnr, 0x00); // EMS frame
                }
                /* build non-EMS ROM frames (0xf000-0x10000) */
                for (int frct = (int) (EMS.EMM_PAGEFRAME4K + 0x10) / 4; frct < 0xf0 / 4; frct++) {
                    Memory.writeB(GEMMIS_addr + 0x0a + frct * 6, 0x00); // frame type: NONE
                    Memory.writeB(GEMMIS_addr + 0x0b + frct * 6, 0xff); // owner: NONE
                    Memory.writeW(GEMMIS_addr + 0x0c + frct * 6, 0xffff); // non-EMS frame
                    Memory.writeB(GEMMIS_addr + 0x0e + frct * 6, 0xff); // EMS page number
                                                                        // (NONE)
                    Memory.writeB(GEMMIS_addr + 0x0f + frct * 6, 0xaa); // flags: direct
                                                                        // mapping
                }

                Memory.writeB(GEMMIS_addr + 0x18a, 0x74); // ???
                Memory.writeB(GEMMIS_addr + 0x18b, 0x00); // no UMB descriptors following
                Memory.writeB(GEMMIS_addr + 0x18c, 0x01); // 1 EMS handle info recort
                Memory.writeW(GEMMIS_addr + 0x18d, 0x0000); // system handle
                Memory.writeD(GEMMIS_addr + 0x18f, 0); // handle name
                Memory.writeD(GEMMIS_addr + 0x193, 0); // handle name
                if (EMS.EMMHandles[EMS.EMM_SYSTEM_HANDLE].Pages != EMS.NULL_HANDLE) {
                    Memory.writeW(GEMMIS_addr + 0x197,
                            0xffff & (int) ((EMS.EMMHandles[EMS.EMM_SYSTEM_HANDLE].Pages + 3) / 4));
                    // physical address
                    Memory.writeD(GEMMIS_addr + 0x199,
                            0xffff & (EMS.EMMHandles[EMS.EMM_SYSTEM_HANDLE].Mem << 12));
                } else {
                    Memory.writeW(GEMMIS_addr + 0x197, 0x0001); // system handle
                    Memory.writeD(GEMMIS_addr + 0x199, 0x00110000); // physical address
                }

                /* fill buffer with import structure */
                Memory.writeD(bufPtr + 0x00, GEMMIS_seg << 4);
                Memory.writeW(bufPtr + 0x04, EMS.GEMMIS_VERSION);
                return retCode = 6;
            }
            case 0x02:
                if (size != 2)
                    return retCode;
                Memory.writeB(bufPtr + 0x00, EMS.EMM_VERSION >>> 4); // version 4
                Memory.writeB(bufPtr + 0x01, EMS.EMM_MINOR_VERSION);
                return retCode = 2;
            case 0x03:
                if (EMS.EMM_MINOR_VERSION < 0x2d)
                    return retCode;
                if (size != 4)
                    return retCode;
                Memory.writeW(bufPtr + 0x00, Memory.totalPages() * 4); // max size (kb)
                Memory.writeW(bufPtr + 0x02, 0x80); // min size (kb)
                return retCode = 2;
        }
        return retCode;
    }

    // 실패하면 code -1 리턴
    @Override
    public int writeToControlChannel(int bufPtr, int size) {
        return -1;
    }

    private byte cache;

}
