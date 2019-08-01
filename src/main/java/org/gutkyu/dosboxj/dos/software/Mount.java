package org.gutkyu.dosboxj.dos.software;

import org.gutkyu.dosboxj.dos.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
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
        if (cmd.getCount() == 0) {
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
        if (DOSBox.Control.getSecureMode()) {
            writeOut(Message.get("PROGRAM_CONFIG_SECURE_DISALLOW"));
            return;
        }

        /* Check for unmounting */
        if (cmd.findString("-u", false)) {
            umount = cmd.returnedString;
            umount = Character.toUpperCase(umount.charAt(0)) + umount.substring(1);
            int iDrive = umount.charAt(0) - 'A';
            if (iDrive < DOSMain.DOS_DRIVES && iDrive >= 0 && DOSMain.Drives[iDrive] != null) {
                switch (DriveManager.unmountDrive(iDrive)) {
                    case 0:
                        DOSMain.Drives[iDrive] = null;
                        if (iDrive == DOSMain.getDefaultDrive())
                            DOSMain.setDrive('Z' - 'A');
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
        if (cmd.findExist("-cd", false)) {
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
        type = cmd.findString("-t", true) ? cmd.returnedString : type;
        boolean iscdrom = type.equals("cdrom"); // Used for mscdex bug cdrom label name emulation
        if (type.equals("floppy") || type.equals("dir") || type.equals("cdrom")) {
            int[] sizes = new int[4];
            int mediaId;
            String strSize;
            if (type.equals("floppy")) {
                strSize = "512,1,2880,2880";/* All space free */
                mediaId = 0xF0; /* Floppy 1.44 media */
            } else if (type.equals("dir")) {
                // 512*127*16383==~1GB total size
                // 512*127*4031==~250MB total free size
                strSize = "512,127,16383,4031";
                mediaId = 0xF8; /* Hard Disk */
            } else if (type.equals("cdrom")) {
                strSize = "2048,1,65535,0";
                mediaId = 0xF8; /* Hard Disk */
            } else {
                writeOut(Message.get("PROGAM_MOUNT_ILL_TYPE"), type);
                return;
            }
            /* Parse the free space in mb's (kb's for floppies) */
            String mbSize = null;
            if (cmd.findString("-freesize", true)) {
                mbSize = cmd.returnedString;
                int sizemb = Integer.parseInt(mbSize);
                if (type.equals("floppy")) {
                    strSize = String.format("512,1,2880,%d", sizemb * 1024 / (512 * 1));
                } else {
                    strSize = String.format("512,127,16513,%d", sizemb * 1024 * 1024 / (512 * 127));
                }
            }

            strSize = cmd.findString("-size", true) ? cmd.returnedString : strSize;
            /* Parse the str_size string */
            sizes = Arrays.stream(strSize.split(",")).mapToInt(Integer::parseInt).toArray();

            // get the drive letter
            tempLine = cmd.findCommand(1) ? cmd.returnedCmd : tempLine;
            if ((tempLine.length() > 2)
                    || ((tempLine.length() > 1) && (tempLine.charAt(1) != ':'))) {
                // goto showusage
                showUsage();
                return;
            }
            drive = Character.toUpperCase((char) tempLine.charAt(0));
            if (!Character.isLetter(drive)) {
                // goto showusage
                showUsage();
                return;
            }

            if (!cmd.findCommand(2)) {
                // goto showusage
                showUsage();
                return;
            }
            tempLine = cmd.returnedCmd;
            if (tempLine.length() == 0) {
                // goto showusage
                showUsage();
                return;
            }

            if (Cross.IS_WINDOWS) {
                /* Removing trailing backslash if not root dir so stat will succeed */
                if (tempLine.length() > 3 && tempLine.charAt(tempLine.length() - 1) == '\\')
                    tempLine = tempLine.substring(0, tempLine.length() - 1);
            }
            Path path = Paths.get(tempLine);
            BasicFileAttributes attr = null;
            try {
                attr = Files.readAttributes(path, BasicFileAttributes.class);
            } catch (Exception e) {
                writeOut(Message.get("PROGRAM_MOUNT_ERROR_1"), tempLine);
                return;
            }
            if (!attr.isDirectory()) {
                writeOut(Message.get("PROGRAM_MOUNT_ERROR_2"), tempLine);
                return;
            }


            if (tempLine.charAt(tempLine.length() - 1) != Cross.FILESPLIT)
                tempLine += Cross.FILESPLIT;
            int bit8Size = 0xff & sizes[1];

            // TODO have to implement CDROM
            /*
             * if (type=="cdrom") { ... } else {
             */

            /* Give a warning when mount c:\ or the / */
            if (Cross.IS_WINDOWS) {
                if ((tempLine.equals("c:\\")) || (tempLine.equals("C:\\"))
                        || (tempLine.equals("c:/")) || (tempLine.equals("C:/")))
                    writeOut(Message.get("PROGRAM_MOUNT_WARNING_WIN"));
            } else {
                if (tempLine == "/")
                    writeOut(Message.get("PROGRAM_MOUNT_WARNING_OTHER"));
            }
            newdrive = new LocalDrive(tempLine, 0xffff & sizes[0], bit8Size, 0xffff & sizes[2],
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
        if (cmd.findString("-label", true)) {
            label = cmd.returnedString;
            newdrive.dirCache.setLabel(label, iscdrom, false);
        }
        /*
         * For hard drives set the label to DRIVELETTER_Drive. For floppy drives set the label to
         * DRIVELETTER_Floppy. This way every drive except cdroms should get a label.
         */
        else if (type.equals("dir")) {
            label = drive + "_DRIVE";
            newdrive.dirCache.setLabel(label, iscdrom, true);
        } else if (type.equals("floppy")) {
            label = drive + "_FLOPPY";
            newdrive.dirCache.setLabel(label, iscdrom, true);
        }
        return;
    }

    private void showUsage() {
        writeOut(Message.get("PROGRAM_MOUNT_USAGE"), "d:\\dosprogs", "d:\\dosprogs");
    }
}
