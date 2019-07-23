package org.gutkyu.dosboxj.interrupt.bios;

import org.gutkyu.dosboxj.cpu.*;
import org.gutkyu.dosboxj.dos.*;
import org.gutkyu.dosboxj.util.*;
import org.gutkyu.dosboxj.interrupt.*;
import org.gutkyu.dosboxj.hardware.memory.*;
import org.gutkyu.dosboxj.dos.mem_block.*;
import org.gutkyu.dosboxj.gui.*;
import org.gutkyu.dosboxj.dos.system.drive.*;
import org.gutkyu.dosboxj.*;

public final class BIOSDisk {
    /* The Section handling Bios Disk Access */
    private static final int BIOS_MAX_DISK = 10;

    public static final int MAX_SWAPPABLE_DISKS = 20;

    private static final int MAX_DISK_IMAGES = 4;

    private static final int MAX_HDD_IMAGES = 2;

    public static class DiskGeo {
        public int KSize; /* Size in kilobytes */
        public int SectorsTrack; /* Sectors per track, uint16 */
        public int HeadsCylinder; /* Heads per cylinder, uint16 */
        public int CylinderCount; /* Cylinders per side, uint16 */
        public int BiosValue; /* Type to return from BIOS, uint16 */

        public DiskGeo(int KSize, int SectorsTrack, int HeadsCylinder, int CylinderCount,
                int BiosValue) {
            this.KSize = KSize;
            this.SectorsTrack = 0xffff & SectorsTrack;
            this.HeadsCylinder = 0xffff & HeadsCylinder;
            this.CylinderCount = 0xffff & CylinderCount;
            this.BiosValue = 0xffff & BiosValue;
        }
    }

    public static DiskGeo[] DiskGeometryList = {new DiskGeo(160, 8, 1, 40, 0),
            new DiskGeo(180, 9, 1, 40, 0), new DiskGeo(200, 10, 1, 40, 0),
            new DiskGeo(320, 8, 2, 40, 1), new DiskGeo(360, 9, 2, 40, 1),
            new DiskGeo(400, 10, 2, 40, 1), new DiskGeo(720, 9, 2, 80, 3),
            new DiskGeo(1200, 15, 2, 80, 2), new DiskGeo(1440, 18, 2, 80, 4),
            new DiskGeo(2880, 36, 2, 80, 6), new DiskGeo(0, 0, 0, 0, 0)};

    private static int _callInt13;
    private static int _diskParm0, _diskParm1;
    private static byte _lastStatus;
    private static int _lastDrive;
    public static int ImgDTASeg;
    public static int ImgDTAPtr;
    public static DOSDTA ImgDTA;
    private static boolean _killRead;
    private static boolean _swappingRequested;

    /* 2 floppys and 2 harddrives, max */
    public static ImageDisk[] ImageDiskList = new ImageDisk[BIOSDisk.MAX_DISK_IMAGES];
    public static ImageDisk[] DiskSwap = new ImageDisk[BIOSDisk.MAX_SWAPPABLE_DISKS];
    public static int SwapPosition;


    private static int getDosDriveNumber(int biosNum) {
        switch (biosNum) {
            case 0x0:
                return 0x0;
            case 0x1:
                return 0x1;
            case 0x80:
                return 0x2;
            case 0x81:
                return 0x3;
            case 0x82:
                return 0x4;
            case 0x83:
                return 0x5;
            default:
                return 0x7f;
        }
    }

    private static boolean driveInActive(int driveNum) {
        if (driveNum >= (2 + MAX_HDD_IMAGES)) {
            Log.logging(Log.LogTypes.BIOS, Log.LogServerities.Error, "Disk %d non-existant",
                    driveNum);
            _lastStatus = 0x01;
            Callback.scf(true);
            return true;
        }
        if (ImageDiskList[driveNum] == null) {
            Log.logging(Log.LogTypes.BIOS, Log.LogServerities.Error, "Disk %d not active",
                    driveNum);
            _lastStatus = 0x01;
            Callback.scf(true);
            return true;
        }
        if (!ImageDiskList[driveNum].active) {
            Log.logging(Log.LogTypes.BIOS, Log.LogServerities.Error, "Disk %d not active",
                    driveNum);
            _lastStatus = 0x01;
            Callback.scf(true);
            return true;
        }
        return false;
    }

    private static int INT13DiskHandler() {
        int segAt, bufPtr;
        byte[] sectBuf = new byte[512];
        int driveNum;
        int i, t;
        _lastDrive = Register.getRegDL();
        driveNum = getDosDriveNumber(Register.getRegDL());
        boolean anyImages = false;
        for (i = 0; i < MAX_DISK_IMAGES; i++) {
            if (ImageDiskList[i] != null)
                anyImages = true;
        }

        // unconditionally enable the interrupt flag
        Callback.sif(true);

        // drivenum = 0;
        // Log.LOG_MSG("INT13: Function %x called on drive %x (dos drive %d)", reg_ah, reg_dl,
        // drivenum);
        switch (Register.getRegAH()) {
            case 0x0: /* Reset disk */
            {
                /*
                 * if there aren't any diskimages (so only localdrives and virtual drives) always
                 * succeed on reset disk. If there are diskimages then and only then do real checks
                 */
                if (anyImages && driveInActive(driveNum)) {
                    /* driveInactive sets carry flag if the specified drive is not available */
                    if ((DOSBox.Machine == DOSBox.MachineType.CGA)
                            || (DOSBox.Machine == DOSBox.MachineType.PCJR)) {
                        /* those bioses call floppy drive reset for invalid drive values */
                        if (((ImageDiskList[0] != null) && (ImageDiskList[0].active))
                                || ((ImageDiskList[1] != null) && (ImageDiskList[1].active))) {
                            _lastStatus = 0x00;
                            Callback.scf(false);
                        }
                    }
                    return Callback.ReturnTypeNone;
                }
                _lastStatus = 0x00;
                Callback.scf(false);
            }
                break;
            case 0x1: /* Get status of last operation */

                if (_lastStatus != 0x00) {
                    Register.setRegAH(_lastStatus);
                    Callback.scf(true);
                } else {
                    Register.setRegAH(0x00);
                    Callback.scf(false);
                }
                break;
            case 0x2: /* Read sectors */
                if (Register.getRegAL() == 0) {
                    Register.setRegAH(0x01);
                    Callback.scf(true);
                    return Callback.ReturnTypeNone;
                }
                if (!anyImages) {
                    // Inherit the Earth cdrom (uses it as disk test)
                    if (((Register.getRegDL() & 0x80) == 0x80) && (Register.getRegDH() == 0)
                            && ((Register.getRegCL() & 0x3f) == 1)) {
                        Register.setRegAH(0);
                        Callback.scf(false);
                        return Callback.ReturnTypeNone;
                    }
                }
                if (driveInActive(driveNum)) {
                    Register.setRegAH(0xff);
                    Callback.scf(true);
                    return Callback.ReturnTypeNone;
                }

                segAt = Register.segValue(Register.SEG_NAME_ES);
                bufPtr = Register.getRegBX();
                for (i = 0; i < Register.getRegAL(); i++) {
                    _lastStatus = ImageDiskList[driveNum].readSector(Register.getRegDH(),
                            Register.getRegCH() | ((Register.getRegCL() & 0xc0) << 2),
                            (Register.getRegCL() & 63) + i, sectBuf);
                    if ((_lastStatus != 0x00) || (_killRead)) {
                        Log.logMsg("Error in disk read");
                        _killRead = false;
                        Register.setRegAH(0x04);
                        Callback.scf(true);
                        return Callback.ReturnTypeNone;
                    }
                    for (t = 0; t < 512; t++) {
                        Memory.realWriteB(segAt, bufPtr, sectBuf[t]);
                        bufPtr++;
                    }
                }
                Register.setRegAH(0x00);
                Callback.scf(false);
                break;
            case 0x3: /* Write sectors */

                if (driveInActive(driveNum)) {
                    Register.setRegAH(0xff);
                    Callback.scf(true);
                    return Callback.ReturnTypeNone;
                }


                bufPtr = Register.getRegBX();
                for (i = 0; i < Register.getRegAL(); i++) {
                    for (t = 0; t < ImageDiskList[driveNum].getGeometrySectSize(); t++) {
                        sectBuf[t] = (byte) Memory
                                .realReadB(Register.segValue(Register.SEG_NAME_ES), bufPtr);
                        bufPtr++;
                    }

                    _lastStatus = ImageDiskList[driveNum].writeSector(Register.getRegDH(),
                            Register.getRegCH() | ((Register.getRegCL() & 0xc0) << 2),
                            (Register.getRegCL() & 63) + i, sectBuf);
                    if (_lastStatus != 0x00) {
                        Callback.scf(true);
                        return Callback.ReturnTypeNone;
                    }
                }
                Register.setRegAH(0x00);
                Callback.scf(false);
                break;
            case 0x04: /* Verify sectors */
                if (Register.getRegAL() == 0) {
                    Register.setRegAH(0x01);
                    Callback.scf(true);
                    return Callback.ReturnTypeNone;
                }
                if (driveInActive(driveNum))
                    return Callback.ReturnTypeNone;

                /* TODO: Finish coding this section */
                /*
                 * segat = SegValue(es); bufptr = reg_bx; for(i=0;i<reg_al;i++) { last_status =
                 * imageDiskList[drivenum].Read_Sector((Bit32u)reg_dh, (Bit32u)(reg_ch |
                 * ((regsModule.reg_cl & 0xc0)<< 2)), (Bit32u)((reg_cl & 63)+i), sectbuf);
                 * if(last_status != 0x00) { Log.LOG_MSG("Error in disk read"); CALLBACK_SCF(true);
                 * return CBRET_NONE; } for(t=0;t<512;t++) { real_writeb(segat,bufptr,sectbuf[t]);
                 * bufptr++; } }
                 */
                Register.setRegAH(0x00);
                // Qbix: The following codes don't match my specs. al should be number of sector
                // verified
                // reg_al = 0x10; /* CRC verify failed */
                // reg_al = 0x00; /* CRC verify succeeded */
                Callback.scf(false);

                break;
            case 0x08: /* Get drive parameters */
                if (driveInActive(driveNum)) {
                    _lastStatus = 0x07;
                    Register.setRegAH(_lastStatus);
                    Callback.scf(true);
                    return Callback.ReturnTypeNone;
                }
                Register.setRegAX(0x00);
                ImageDisk imgDsk = ImageDiskList[driveNum];
                Register.setRegBL(imgDsk.getBiosType());
                int tmpheads = imgDsk.getGeometryCylinders();
                int tmpcyl = imgDsk.getGeometryCylinders();
                int tmpsect = imgDsk.getGeometrySectors();
                int tmpsize = imgDsk.getGeometrySectSize();
                if (tmpcyl == 0)
                    Log.logging(Log.LogTypes.BIOS, Log.LogServerities.Error,
                            "INT13 DrivParm: cylinder count zero!");
                else
                    tmpcyl--; // cylinder count . max cylinder
                if (tmpheads == 0)
                    Log.logging(Log.LogTypes.BIOS, Log.LogServerities.Error,
                            "INT13 DrivParm: head count zero!");
                else
                    tmpheads--; // head count . max head
                Register.setRegCH(tmpcyl & 0xff);
                Register.setRegCL(((tmpcyl >>> 2) & 0xc0) | (tmpsect & 0x3f));
                Register.setRegDH(tmpheads);
                _lastStatus = 0x00;
                if ((Register.getRegDL() & 0x80) != 0) { // harddisks
                    Register.setRegDL(0);
                    if (ImageDiskList[2] != null)
                        Register.setRegDL(Register.getRegDL() + 1);
                    if (ImageDiskList[3] != null)
                        Register.setRegDL(Register.getRegDL() + 1);
                } else { // floppy disks
                    Register.setRegDL(0);
                    if (ImageDiskList[0] != null)
                        Register.setRegDL(Register.getRegDL() + 1);
                    if (ImageDiskList[1] != null)
                        Register.setRegDL(Register.getRegDL() + 1);
                }
                Callback.scf(false);
                break;
            case 0x11: /* Recalibrate drive */
                Register.setRegAH(0x00);
                Callback.scf(false);
                break;
            case 0x17: /* Set disk type for format */
                /* Pirates! needs this to load */
                _killRead = true;
                Register.setRegAH(0x00);
                Callback.scf(false);
                break;
            default:
                Log.logging(Log.LogTypes.BIOS, Log.LogServerities.Error,
                        "INT13: Function %x called on drive %x (dos drive %d)", Register.getRegAH(),
                        Register.getRegDL(), driveNum);
                Register.setRegAH(0xff);
                Callback.scf(true);
                break;
        }
        return Callback.ReturnTypeNone;
    }

    public static void setupDisks() {
        /* TODO Start the time correctly */
        _callInt13 = Callback.allocate();
        Callback.setup(_callInt13, BIOSDisk::INT13DiskHandler, Callback.Symbol.IRET,
                "Int 13 Bios disk");
        Memory.realSetVec(0x13, Callback.realPointer(_callInt13));
        int i;
        for (i = 0; i < 4; i++) {
            ImageDiskList[i] = null;
        }

        for (i = 0; i < MAX_SWAPPABLE_DISKS; i++) {
            DiskSwap[i] = null;
        }

        _diskParm0 = Callback.allocate();
        _diskParm1 = Callback.allocate();
        SwapPosition = 0;

        Memory.realSetVec(0x41, Callback.realPointer(_diskParm0));
        Memory.realSetVec(0x46, Callback.realPointer(_diskParm1));

        int dp0physaddr = Callback.physPointer(_diskParm0);
        int dp1physaddr = Callback.physPointer(_diskParm1);
        for (i = 0; i < 16; i++) {
            Memory.physWriteB(dp0physaddr + i, 0);
            Memory.physWriteB(dp1physaddr + i, 0);
        }

        ImgDTASeg = 0;

        /* Setup the Bios Area */
        Memory.writeB(BIOS.BIOS_HARDDISK_COUNT, 2);

        GUIPlatform.mapper.addKeyHandler(BIOSDisk::swapInNextDisk, MapKeys.F4, Mapper.MMOD1,
                "swapimg", "Swap Image");
        _killRead = false;
        _swappingRequested = false;
    }

    private static void swapInNextDisk(boolean pressed) {
        if (!pressed)
            return;
        DriveManager.cycleAllDisks();
        /* Hack/feature: rescan all disks as well */
        Log.logMsg("Diskcaching reset for normal mounted drives.");
        for (int i = 0; i < DOSMain.DOS_DRIVES; i++) {
            if (DOSMain.Drives[i] != null)
                DOSMain.Drives[i].emptyCache();
        }
        SwapPosition++;
        if (DiskSwap[SwapPosition] == null)
            SwapPosition = 0;
        swapInDisks();
        _swappingRequested = true;
    }

    public static void updateDPT() {
        int tmpHeads = 0, tmpCylinder = 0, tmpSect = 0, tmpSize = 0;
        if (ImageDiskList[2] != null) {
            int dp0physaddr = Callback.physPointer(_diskParm0);

            ImageDisk imgDsk = ImageDiskList[2];
            tmpHeads = imgDsk.getGeometryCylinders();
            tmpCylinder = imgDsk.getGeometryCylinders();
            tmpSect = imgDsk.getGeometrySectors();
            tmpSize = imgDsk.getGeometrySectSize();

            Memory.physWriteW(dp0physaddr, tmpCylinder);
            Memory.physWriteB(dp0physaddr + 0x2, tmpHeads);
            Memory.physWriteW(dp0physaddr + 0x3, 0);
            Memory.physWriteW(dp0physaddr + 0x5, (-1));
            Memory.physWriteB(dp0physaddr + 0x7, 0);
            Memory.physWriteB(dp0physaddr + 0x8,
                    (0xc0 | ((((ImageDiskList[2].heads) > 8) ? 1 : 0) << 3)));
            Memory.physWriteB(dp0physaddr + 0x9, 0);
            Memory.physWriteB(dp0physaddr + 0xa, 0);
            Memory.physWriteB(dp0physaddr + 0xb, 0);
            Memory.physWriteW(dp0physaddr + 0xc, tmpCylinder);
            Memory.physWriteB(dp0physaddr + 0xe, tmpSect);
        }
        if (ImageDiskList[3] != null) {
            int dp1physaddr = Callback.physPointer(_diskParm1);
            ImageDisk imgDsk = ImageDiskList[3];

            tmpHeads = imgDsk.getGeometryCylinders();
            tmpCylinder = imgDsk.getGeometryCylinders();
            tmpSect = imgDsk.getGeometrySectors();
            tmpSize = imgDsk.getGeometrySectSize();

            Memory.physWriteW(dp1physaddr, tmpCylinder);
            Memory.physWriteB(dp1physaddr + 0x2, tmpHeads);
            Memory.physWriteB(dp1physaddr + 0xe, tmpSect);
        }
    }


    public static void swapInDisks() {
        boolean allNull = true;
        int diskCount = 0;
        int swapPos = SwapPosition;
        int i;

        /* Check to make sure there's atleast one setup image */
        for (i = 0; i < MAX_SWAPPABLE_DISKS; i++) {
            if (DiskSwap[i] != null) {
                allNull = false;
                break;
            }
        }

        /* No disks setup... fail */
        if (allNull)
            return;

        /* If only one disk is loaded, this loop will load the same disk in dive A and drive B */
        while (diskCount < 2) {
            if (DiskSwap[swapPos] != null) {
                Log.logMsg("Loaded disk %d from swaplist position %d - \"%s\"", diskCount, swapPos,
                        DiskSwap[swapPos].diskname);
                ImageDiskList[diskCount] = DiskSwap[swapPos];
                diskCount++;
            }
            swapPos++;
            if (swapPos >= MAX_SWAPPABLE_DISKS)
                swapPos = 0;
        }
    }

}
