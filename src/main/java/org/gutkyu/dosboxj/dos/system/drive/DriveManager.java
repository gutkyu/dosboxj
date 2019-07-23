package org.gutkyu.dosboxj.dos.system.drive;

import java.util.List;
import org.gutkyu.dosboxj.dos.*;
import org.gutkyu.dosboxj.misc.setup.*;
import org.gutkyu.dosboxj.util.*;

public final class DriveManager {

    private static class DriveInfo {
        public List<DOSDrive> Disks;
        public int CurrentDisk;
    }

    private static DriveInfo[] driveInfos = new DriveInfo[DOSMain.DOS_DRIVES];
    private static int currentDrive;

    static {

        for (int i = 0; i < driveInfos.length; i++) {
            driveInfos[i] = new DriveInfo();
        }
    }

    public static void appendDisk(int drive, DOSDrive disk) {
        driveInfos[drive].Disks.add(disk);
    }

    public static void initializeDrive(int drive) {
        currentDrive = drive;
        DriveInfo drvInf = driveInfos[currentDrive];
        if (drvInf.Disks.size() > 0) {
            drvInf.CurrentDisk = 0;
            DOSDrive disk = drvInf.Disks.get(drvInf.CurrentDisk);
            DOSMain.Drives[currentDrive] = disk;
            disk.activate();
        }
    }

    public static int unmountDrive(int drive) {
        int result = 0;
        // unmanaged drive
        if (driveInfos[drive].Disks.size() == 0) {
            result = DOSMain.Drives[drive].unMount();
        } else {
            // managed drive
            int currentDisk = driveInfos[drive].CurrentDisk;
            result = driveInfos[drive].Disks.get(currentDisk).unMount();
            // only delete on success, current disk set to NULL because of UnMount
            if (result == 0) {
                driveInfos[drive].Disks.set(currentDisk, null);
                int dskSize = driveInfos[drive].Disks.size();
                for (int i = 0; i < dskSize; i++) {
                    driveInfos[drive].Disks.set(i, null);
                }
                driveInfos[drive].Disks.clear();
            }
        }

        return result;
    }

    // static void CycleDrive(boolean pressed);
    // static void CycleDisk(boolean pressed);
    public static void cycleAllDisks() {
        for (int idrive = 0; idrive < DOSMain.DOS_DRIVES; idrive++) {
            int numDisks = driveInfos[idrive].Disks.size();
            if (numDisks > 1) {
                // cycle disk
                int currentDisk = driveInfos[idrive].CurrentDisk;
                DOSDrive oldDisk = driveInfos[idrive].Disks.get(currentDisk);
                currentDisk = (currentDisk + 1) % numDisks;
                DOSDrive newDisk = driveInfos[idrive].Disks.get(currentDisk);
                driveInfos[idrive].CurrentDisk = currentDisk;

                // copy working directory, acquire system resources and finally switch to next drive
                newDisk.curdir = oldDisk.curdir;
                newDisk.activate();
                DOSMain.Drives[idrive] = newDisk;
                Log.logMsg("Drive %c: disk %d of %d now active", 'A' + idrive, currentDisk + 1,
                        numDisks);
            }
        }
    }

    public static void init(Section sec) {

        // setup driveInfos structure
        currentDrive = 0;
        for (int i = 0; i < DOSMain.DOS_DRIVES; i++) {
            driveInfos[i].CurrentDisk = 0;
        }

        // MAPPER_AddHandler(&CycleDisk, MK_f3, MMOD1, "cycledisk", "Cycle Disk");
        // MAPPER_AddHandler(&CycleDrive, MK_f3, MMOD2, "cycledrive", "Cycle Drv");
    }



}
