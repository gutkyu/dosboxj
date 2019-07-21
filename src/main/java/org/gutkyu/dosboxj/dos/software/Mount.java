package org.gutkyu.dosboxj.dos.software;

import org.gutkyu.dosboxj.dos.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import org.gutkyu.dosboxj.*;
import org.gutkyu.dosboxj.misc.*;
import org.gutkyu.dosboxj.hardware.memory.*;
import org.gutkyu.dosboxj.dos.system.drive.*;

public final class Mount extends Program {
    public static Program makeProgram() {
        return new Mount();
    }

    @Override
    public void run() {
        DOSDrive newdrive;
        char drive;
        String label = null;
        String umount = null;

        // Hack To allow long commandlines
        changeToLongCmd();
        /* Parse the command line */
        /* if the command line is empty show current mounts */
        if (Cmd.getCount() == 0) {
            writeOut(Message.get("PROGRAM_MOUNT_STATUS_1"));
            for (int d = 0; d < DOSMain.DOS_DRIVES; d++) {
                if (DOSMain.Drives[d] != null) {
                    writeOut(Message.get("PROGRAM_MOUNT_STATUS_2"), (char) (d + 'A'),
                            DOSMain.Drives[d].getInfo());
                }
            }
            return;
        }

        /*
         * In secure mode don't allow people to change mount points. Neither mount nor unmount
         */
        if (DOSBox.Control.secureMode()) {
            writeOut(Message.get("PROGRAM_CONFIG_SECURE_DISALLOW"));
            return;
        }

        /* Check for unmounting */
        if ((umount = Cmd.findString("-u", false)) != null) {
            umount = Character.toUpperCase(umount.charAt(0)) + umount.substring(1);
            int iDrive = umount.charAt(0) - 'A';
            if (iDrive < DOSMain.DOS_DRIVES && iDrive >= 0 && DOSMain.Drives[iDrive] != null) {
                switch (DriveManager.unmountDrive(iDrive)) {
                    case 0:
                        DOSMain.Drives[iDrive] = null;
                        if (iDrive == DOSMain.getDefaultDrive())
                            DOSMain.setDrive((byte) ('Z' - 'A'));
                        writeOut(Message.get("PROGRAM_MOUNT_UMOUNT_SUCCESS"), umount.charAt(0));
                        break;
                    case 1:
                        writeOut(Message.get("PROGRAM_MOUNT_UMOUNT_NO_VIRTUAL"));
                        break;
                    case 2:
                        writeOut(Message.get("MSCDEX_ERROR_MULTIPLE_CDROMS"));
                        break;
                }
            } else {
                writeOut(Message.get("PROGRAM_MOUNT_UMOUNT_NOT_MOUNTED"), umount.charAt(0));
            }
            return;
        }

        // Show list of cdroms
        if (Cmd.findExist("-cd", false)) {
            /*
             * #if CDROM int num = SDL_CDNumDrives();
             * WriteOut(messages.MSG_Get("PROGRAM_MOUNT_CDROMS_FOUND"), num); for (int i = 0; i <
             * num; i++) { WriteOut("%2d. %s\n", i, SDL_CDName(i)); }; #else
             */
            writeOut(Message.get("PROGRAM_MOUNT_CDROMS_FOUND"), 0);
            /*
             * #endif
             */
            return;
        }

        String type = "dir";
        type = Cmd.findString("-t", true);
        boolean iscdrom = (type == "cdrom"); // Used for mscdex bug cdrom label name emulation
        if (type == "floppy" || type == "dir" || type == "cdrom") {
            int[] sizes = new int[4];
            int mediaId;
            String strSize;
            if (type == "floppy") {
                strSize = "512,1,2880,2880";/* All space free */
                mediaId = 0xF0; /* Floppy 1.44 media */
            } else if (type == "dir") {
                // 512*127*16383==~1GB total size
                // 512*127*4031==~250MB total free size
                strSize = "512,127,16383,4031";
                mediaId = 0xF8; /* Hard Disk */
            } else if (type == "cdrom") {
                strSize = "2048,1,65535,0";
                mediaId = 0xF8; /* Hard Disk */
            } else {
                writeOut(Message.get("PROGAM_MOUNT_ILL_TYPE"), type);
                return;
            }
            /* Parse the free space in mb's (kb's for floppies) */
            String mbSize = null;
            if ((mbSize = Cmd.findString("-freesize", true)) != null) {
                int sizemb = Integer.parseInt(mbSize);
                if (type == "floppy") {
                    strSize = String.format("512,1,2880,%d", sizemb * 1024 / (512 * 1));
                } else {
                    strSize = String.format("512,127,16513,%d", sizemb * 1024 * 1024 / (512 * 127));
                }
            }

            strSize = Cmd.findString("-size", true);
            char[] number = new char[20];
            int scanIdx = 0;
            int index = 0;
            int count = 0;
            /* Parse the str_size string */
            while (scanIdx < strSize.length()) {
                if (strSize.charAt(scanIdx) == ',') {
                    number[index] = (char) 0;
                    sizes[count++] = Integer.parseInt(new String(number));
                    index = 0;
                } else
                    number[index++] = strSize.charAt(scanIdx);
                scanIdx++;
            }
            number[index] = (char) 0;
            sizes[count++] = Integer.parseInt(new String(number));

            // get the drive letter
            TempLine = Cmd.findCommand(1);
            if ((TempLine.length() > 2)
                    || ((TempLine.length() > 1) && (TempLine.charAt(1) != ':'))) {
                // goto showusage
                showUsage();
                return;
            }
            drive = Character.toUpperCase((char) TempLine.charAt(0));
            if (!Character.isLetter(drive)) {
                // goto showusage
                showUsage();
                return;
            }

            if ((TempLine = Cmd.findCommand(2)) == null) {
                // goto showusage
                showUsage();
                return;
            }
            if (TempLine.length() == 0) {
                // goto showusage
                showUsage();
                return;
            }

            boolean failed = false;
            /* Removing trailing backslash if not root dir so stat will succeed */
            if (TempLine.length() > 3 && TempLine.charAt(TempLine.length() - 1) == '\\')
                TempLine = TempLine.substring(0, TempLine.length() - 1);;
            Path path = Paths.get(TempLine);
            BasicFileAttributes attr = null;
            try {
                attr = Files.readAttributes(path, BasicFileAttributes.class);
            } catch (Exception e) {
                writeOut(Message.get("PROGRAM_MOUNT_ERROR_1"), TempLine);
                return;
            }
            if (!attr.isDirectory()) {
                writeOut(Message.get("PROGRAM_MOUNT_ERROR_2"), TempLine);
                return;
            }


            if (TempLine.charAt(TempLine.length() - 1) != Cross.FILESPLIT)
                TempLine += Cross.FILESPLIT;
            int bit8Size = 0xff & sizes[1];

            // TODO have to implement CDROM
            /*
             * if (type=="cdrom") { ... } else {
             */

            /* Give a warning when mount c:\ or the / */
            if ((TempLine == "c:\\") || (TempLine == "C:\\") || (TempLine == "c:/")
                    || (TempLine == "C:/"))
                writeOut(Message.get("PROGRAM_MOUNT_WARNING_WIN"));
            newdrive = new LocalDrive(TempLine, 0xffff & sizes[0], bit8Size, 0xffff & sizes[2],
                    0xffff & sizes[3], mediaId);

        } else {
            writeOut(Message.get("PROGRAM_MOUNT_ILL_TYPE"), type);
            return;
        }
        if (DOSMain.Drives[drive - 'A'] != null) {
            writeOut(Message.get("PROGRAM_MOUNT_ALREADY_MOUNTED"), drive,
                    DOSMain.Drives[drive - 'A'].getInfo());
            if (newdrive != null)
                newdrive = null;
            return;
        }
        if (newdrive == null)
            Support.exceptionExit("DOS:Can't create drive");
        DOSMain.Drives[drive - 'A'] = newdrive;
        /* Set the correct media byte in the table */
        Memory.writeB(Memory.real2Phys(DOSMain.DOS.tables.MediaId) + (drive - 'A') * 2,
                newdrive.getMediaByte());
        writeOut(Message.get("PROGRAM_MOUNT_STATUS_2"), drive, newdrive.getInfo());
        /* check if volume label is given and don't allow it to updated in the future */
        if ((label = Cmd.findString("-label", true)) != null)
            newdrive.dirCache.setLabel(label, iscdrom, false);
        /*
         * For hard drives set the label to DRIVELETTER_Drive. For floppy drives set the label to
         * DRIVELETTER_Floppy. This way every drive except cdroms should get a label.
         */
        else if (type == "dir") {
            label = drive + "_DRIVE";
            newdrive.dirCache.setLabel(label, iscdrom, true);
        } else if (type == "floppy") {
            label = drive + "_FLOPPY";
            newdrive.dirCache.setLabel(label, iscdrom, true);
        }
        return;
    }

    private void showUsage() {
        writeOut(Message.get("PROGRAM_MOUNT_USAGE"), "d:\\dosprogs", "d:\\dosprogs");
    }
}
