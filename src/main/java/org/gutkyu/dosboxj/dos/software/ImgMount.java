package org.gutkyu.dosboxj.dos.software;

import org.gutkyu.dosboxj.hardware.memory.*;
import org.gutkyu.dosboxj.misc.*;
import org.gutkyu.dosboxj.util.*;
import org.gutkyu.dosboxj.dos.*;
import org.gutkyu.dosboxj.interrupt.*;
import org.gutkyu.dosboxj.interrupt.bios.*;
import org.gutkyu.dosboxj.dos.system.drive.*;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import org.gutkyu.dosboxj.*;

public final class ImgMount extends Program {
    public static Program makeProgram() {
        return new ImgMount();
    }

    @Override
    public void run() {
        // Hack To allow long commandlines
        this.changeToLongCmd();
        /*
         * In secure mode don't allow people to change imgmount points. Neither mount nor unmount
         */
        if (DOSBox.Control.getSecureMode()) {
            writeOut(Message.get("PROGRAM_CONFIG_SECURE_DISALLOW"));
            return;
        }
        DOSDrive newdrive = null;
        ImageDisk newImage = null;
        int imagesize = 0;
        char drive;
        String label;
        List<String> paths = new ArrayList<String>();
        String umount = "";
        /* Check for unmounting */
        if (this.cmd.findString("-u", false)) {
            umount = cmd.returnedString;
            umount = String.valueOf(Character.toUpperCase(umount.charAt(0)));
            if (umount.length() > 1) {
                umount += umount.substring(1);
            }
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


        String type = "hdd";
        String fstype = "fat";
        type = cmd.findString("-t", true) ? cmd.returnedString : type;
        fstype = cmd.findString("-fs", true) ? cmd.returnedString : fstype;
        if (type.equals("cdrom"))
            type = "iso"; // Tiny hack for people who like to type -t cdrom
        int mediaId;
        if (type.equals("floppy") || type.equals("hdd") || type.equals("iso")) {
            int[] sizes = new int[4];
            boolean imgsizedetect = false;

            String strSize = "";
            mediaId = 0xF8;

            if (type.equals("floppy")) {
                mediaId = 0xF0;
            } else if (type.equals("iso")) {
                strSize = "650,127,16513,1700";
                mediaId = 0xF8;
                fstype = "iso";
            }
            strSize = cmd.findString("-size", true) ? cmd.returnedString : strSize;
            if ((type.equals("hdd")) && (strSize.length() == 0)) {
                imgsizedetect = true;
            } else {
                int i = 0;
                for (String sz : strSize.split(",")) {
                    sizes[i++] = Integer.parseInt(sz);
                }
            }

            if (fstype.equals("fat") || fstype.equals("iso")) {
                boolean foundCmd = cmd.findCommand(1);
                tempLine = foundCmd ? cmd.returnedCmd : tempLine;
                // get the drive letter
                if (!foundCmd || (tempLine.length() > 2)
                        || ((tempLine.length() > 1) && (tempLine.charAt(1) != ':'))) {
                    writeOutNoParsing(Message.get("PROGRAM_IMGMOUNT_SPECIFY_DRIVE"));
                    return;
                }
                drive = Character.toUpperCase(tempLine.charAt(0));
                if (!Character.isLetter(drive)) {
                    writeOutNoParsing(Message.get("PROGRAM_IMGMOUNT_SPECIFY_DRIVE"));
                    return;
                }
            } else if (fstype.equals("none")) {
                tempLine = cmd.findCommand(1) ? cmd.returnedCmd : tempLine;
                if ((tempLine.length() > 1) || (!Character.isDigit(tempLine.charAt(0)))) {
                    writeOutNoParsing(Message.get("PROGRAM_IMGMOUNT_SPECIFY2"));
                    return;
                }
                drive = tempLine.charAt(0);
                if ((drive < '0') || (drive > 3 + '0')) {
                    writeOutNoParsing(Message.get("PROGRAM_IMGMOUNT_SPECIFY2"));
                    return;
                }
            } else {
                writeOut(Message.get("PROGRAM_IMGMOUNT_FORMAT_UNSUPPORTED"), fstype);
                return;
            }

            // find all file parameters, assuming that all option parameters have been removed
            while (cmd.findCommand(paths.size() + 2) && (tempLine = cmd.returnedCmd).length() > 0) {
                Path path = Paths.get(tempLine);
                BasicFileAttributes attr = null;
                try {
                    attr = Files.readAttributes(path, BasicFileAttributes.class);
                } catch (Exception e) {


                    // See if it works if the ~ are written out
                    String homedir = tempLine;
                    homedir = Cross.resolveHomedir(homedir);
                    path = Paths.get(homedir);
                    try {
                        attr = Files.readAttributes(path, BasicFileAttributes.class);
                        tempLine = homedir;
                    } catch (Exception e1) {
                        // convert dosbox filename to system filename
                        CStringPt tmp = CStringPt.create(Cross.LEN);
                        CStringPt.safeCopy(tempLine, tmp, Cross.LEN);
                        if (!DOSMain.makeFullName(tmp.toString(), Cross.LEN)
                                || DOSMain.Drives[DOSMain.returnedFullNameDrive]
                                        .getInfo() != "local directory") {
                            writeOut(Message.get("PROGRAM_IMGMOUNT_NON_LOCAL_DRIVE"));
                            return;
                        }
                        String fullName = DOSMain.returnedFullName;
                        int dummy = DOSMain.returnedFullNameDrive;

                        DOSDrive drv = DOSMain.Drives[dummy];
                        if (!(drv instanceof LocalDrive)) {
                            writeOut(Message.get("PROGRAM_IMGMOUNT_FILE_NOT_FOUND"));
                            return;
                        }
                        LocalDrive ldp = (LocalDrive) drv;
                        ldp.getSystemFilename(tmp, fullName);
                        tempLine = tmp.toString();

                        path = Paths.get(tempLine);
                        try {
                            attr = Files.readAttributes(path, BasicFileAttributes.class);
                        } catch (Exception e3) {
                            writeOut(Message.get("PROGRAM_IMGMOUNT_FILE_NOT_FOUND"));
                            return;
                        }
                    }

                }
                if (attr.isDirectory()) {
                    writeOut(Message.get("PROGRAM_IMGMOUNT_MOUNT"));
                    return;
                }
                paths.add(tempLine);
            }
            if (paths.size() == 0) {
                writeOut(Message.get("PROGRAM_IMGMOUNT_SPECIFY_FILE"));
                return;
            }
            if (paths.size() == 1)
                tempLine = paths.get(0);
            if (paths.size() > 1 && !fstype.equals("iso")) {
                writeOut(Message.get("PROGRAM_IMGMOUNT_MULTIPLE_NON_CUEISO_FILES"));
                return;
            }
            int fcsize = 0;
            int sectors = 0;
            if (fstype.equals("fat")) {
                if (imgsizedetect) {
                    Path path = Paths.get(tempLine);
                    SeekableByteChannel diskfile = null;
                    try {
                        diskfile = Files.newByteChannel(path, StandardOpenOption.READ,
                                StandardOpenOption.WRITE);// "rb+"
                        if (diskfile == null) {
                            writeOut(Message.get("PROGRAM_IMGMOUNT_INVALID_IMAGE"));
                            return;
                        }
                        fcsize = (int) (diskfile.size() / 512L);

                        byte[] buf = new byte[512];
                        ByteBuffer rb = ByteBuffer.wrap(buf, 0, 512 * 1);
                        diskfile.position(0);
                        if (diskfile.read(rb) < 512) {
                            diskfile.close();
                            writeOut(Message.get("PROGRAM_IMGMOUNT_INVALID_IMAGE"));
                            return;
                        }
                        diskfile.close();
                        if ((buf[510] != 0x55) || (buf[511] != (byte) 0xaa)) {
                            writeOut(Message.get("PROGRAM_IMGMOUNT_INVALID_GEOMETRY"));
                            return;
                        }
                        sectors = (int) (fcsize / (16 * 63));
                        if (sectors * 16 * 63 != fcsize) {
                            writeOut(Message.get("PROGRAM_IMGMOUNT_INVALID_GEOMETRY"));
                            return;
                        }

                    } catch (Exception e) {

                    }

                    sizes[0] = 512;
                    sizes[1] = 63;
                    sizes[2] = 16;
                    sizes[3] = sectors;
                    Log.logMsg("autosized image file: %d:%d:%d:%d", sizes[0], sizes[1], sizes[2],
                            sizes[3]);
                }

                newdrive = new FATDrive(tempLine, sizes[0], sizes[1], sizes[2], sizes[3], 0);
                if (!((FATDrive) newdrive).createdSuccessfully) {
                    ((FATDrive) newdrive).dispose();
                    newdrive = null;
                }
            } else if (fstype.equals("iso")) {
            } else {
                // fopen(temp_line.c_str(), "rb+");
                Path path = Paths.get(tempLine);
                SeekableByteChannel newDisk = null;
                try {
                    newDisk = Files.newByteChannel(path, StandardOpenOption.READ,
                            StandardOpenOption.WRITE);
                    // fseek(newDisk,0L, SEEK_END);
                    // imagesize = (ftell(newDisk) / 1024);
                    imagesize = (int) (newDisk.size() / 1024);
                } catch (Exception e) {

                }
                newImage = new ImageDisk(newDisk, tempLine, imagesize, (imagesize > 2880));
                if (imagesize > 2880)
                    newImage.setGeometry(sizes[2], sizes[3], sizes[1], sizes[0]);
            }
        } else {
            writeOut(Message.get("PROGRAM_IMGMOUNT_TYPE_UNSUPPORTED"), type);
            return;
        }

        if (fstype.equals("fat")) {
            if (DOSMain.Drives[drive - 'A'] != null) {
                writeOut(Message.get("PROGRAM_IMGMOUNT_ALREADY_MOUNTED"));
                if (newdrive != null)
                    ((FATDrive) newdrive).dispose();
                return;
            }
            if (newdrive == null) {
                writeOut(Message.get("PROGRAM_IMGMOUNT_CANT_CREATE"));
                return;
            }
            DOSMain.Drives[drive - 'A'] = newdrive;
            // Set the correct media byte in the table
            Memory.writeB(Memory.real2Phys(DOSMain.DOS.tables.MediaId) + (drive - 'A') * 2,
                    mediaId);
            writeOut(Message.get("PROGRAM_MOUNT_STATUS_2"), drive, tempLine);
            if (((FATDrive) newdrive).LoadedDisk.hardDrive) {
                if (BIOSDisk.ImageDiskList[2] == null) {
                    BIOSDisk.ImageDiskList[2] = ((FATDrive) newdrive).LoadedDisk;
                    BIOSDisk.updateDPT();
                    return;
                }
                if (BIOSDisk.ImageDiskList[3] == null) {
                    BIOSDisk.ImageDiskList[3] = ((FATDrive) newdrive).LoadedDisk;
                    BIOSDisk.updateDPT();
                    return;
                }
            }
            if (!((FATDrive) newdrive).LoadedDisk.hardDrive) {
                BIOSDisk.ImageDiskList[0] = ((FATDrive) newdrive).LoadedDisk;
            }
        } else if (fstype.equals("iso")) {
            // TODO have to implement

        } else if (fstype.equals("none")) {
            if (BIOSDisk.ImageDiskList[drive - '0'] != null)
                BIOSDisk.ImageDiskList[drive - '0'].dispose();
            BIOSDisk.ImageDiskList[drive - '0'] = newImage;
            BIOSDisk.updateDPT();
            writeOut(Message.get("PROGRAM_IMGMOUNT_MOUNT_NUMBER"), drive - '0', tempLine);
        }

        // check if volume label is given. becareful for cdrom
        // if (cmd.FindString("-label",label,true)) newdrive.dirCache.SetLabel(label.c_str());
        return;
    }
}
