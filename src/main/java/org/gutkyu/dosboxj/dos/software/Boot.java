package org.gutkyu.dosboxj.dos.software;

import org.gutkyu.dosboxj.dos.*;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import org.gutkyu.dosboxj.*;
import org.gutkyu.dosboxj.cpu.*;
import org.gutkyu.dosboxj.interrupt.*;
import org.gutkyu.dosboxj.interrupt.bios.*;
import org.gutkyu.dosboxj.misc.*;
import org.gutkyu.dosboxj.util.*;
import org.gutkyu.dosboxj.hardware.memory.*;
import org.gutkyu.dosboxj.dos.system.drive.*;
import org.gutkyu.dosboxj.misc.setup.*;
import org.gutkyu.dosboxj.misc.setup.Value.WrongType;

public final class Boot extends Program {

    private static final int bootSector_Off_jump = 0;
    private static final int bootSector_Off_oem_name = 3;
    private static final int bootSector_Off_bytesect = 11;
    private static final int bootSector_Off_sectclust = 13;
    private static final int bootSector_Off_reserve_sect = 14;
    private static final int bootSector_Off_misc = 16;

    private static final int bootSector_Size_jump = 3;
    private static final int bootSector_Size_oem_name = 8;
    private static final int bootSector_Size_bytesect = 2;
    private static final int bootSector_Size_sectclust = 1;
    private static final int bootSector_Size_reserve_sect = 2;
    private static final int bootSector_Size_misc = 496;
    private static final int bootSector_Size_Total = 512;

    public static Program makeProgram() {
        return new Boot();
    }

    private int returnedFSFileMountedKSize, returnedFSFileMountedBSize, returnedFSFileMountedErr;

    private SeekableByteChannel tryGetFSFileMounted(String fileName, int error) {
        // if return null then put in error the errormessage code if an error was
        // requested
        boolean tryload = error > 0 ? true : false;
        returnedFSFileMountedErr = 0;
        int drive = 0;
        SeekableByteChannel tmpCh;

        LocalDrive ldp = null;
        if (!DOSMain.makeFullName(fileName, DOSSystem.DOS_PATHLENGTH))
            return null;
        String fullName = DOSMain.returnedFullName;
        drive = DOSMain.returnedFullNameDrive;

        try {
            DOSDrive drv = DOSMain.Drives[drive];
            if (!(drv instanceof LocalDrive))
                return null;
            ldp = (LocalDrive) drv;

            tmpCh = ldp.getSystemFileChannel(fullName, StandardOpenOption.READ);
            if (tmpCh == null) {
                if (!tryload)
                    returnedFSFileMountedErr = 1;
                return null;
            }

            // get file size
            // tmpfile.Seek(0L, SeekOrigin.End);
            // ksize = (int)(tmpfile.Position / 1024);
            // bsize = (int)tmpfile.Position;
            // tmpfile.Close();
            returnedFSFileMountedKSize = (int) (tmpCh.size() / 1024);
            returnedFSFileMountedBSize = (int) tmpCh.size();
            tmpCh.close();

            // "rb+"
            tmpCh = ldp.getSystemFileChannel(fullName, StandardOpenOption.READ,
                    StandardOpenOption.WRITE);
            if (tmpCh == null) {
                // if (!tryload) *error=2;
                // return null;
                writeOut(Message.get("PROGRAM_BOOT_WRITE_PROTECTED"));
                tmpCh = ldp.getSystemFileChannel(fullName, StandardOpenOption.READ);
                if (tmpCh == null) {
                    if (!tryload)
                        returnedFSFileMountedErr = 1;
                    return null;
                }
            }

            return tmpCh;
        } catch (Exception e) {
            return null;
        }
    }

    private int returnedFSFileKSize, returnedFSFileBSize;

    private SeekableByteChannel tryGetFSFile(String fileName, boolean tryLoad) {
        int error = tryLoad ? 1 : 0;
        SeekableByteChannel tmpCh = tryGetFSFileMounted(fileName, error);
        returnedFSFileKSize = returnedFSFileMountedKSize;
        returnedFSFileBSize = returnedFSFileMountedBSize;
        error = returnedFSFileMountedErr;
        if (tmpCh != null)
            return tmpCh;
        // File not found on mounted filesystem. Try regular filesystem
        String filenameS = fileName;
        Path path = Paths.get(Cross.resolveHomedir(filenameS));
        try {
            // "rb+"
            tmpCh = Files.newByteChannel(path, StandardOpenOption.READ, StandardOpenOption.WRITE);
            // fseek(tmpfile,0L, SEEK_END);
            // *ksize = (ftell(tmpfile) / 1024);
            // *bsize = ftell(tmpfile);
            returnedFSFileKSize = (int) (tmpCh.size() / 1024);
            returnedFSFileBSize = (int) tmpCh.size();
        } catch (Exception e1) {
            try {
                // "rb"
                tmpCh = Files.newByteChannel(path, StandardOpenOption.READ);
                returnedFSFileKSize = (int) (tmpCh.size() / 1024);
                returnedFSFileBSize = (int) tmpCh.size();
            } catch (Exception e2) {
                // Give the delayed errormessages from the mounted variant (or from above)
                if (error == 1)
                    writeOut(Message.get("PROGRAM_BOOT_NOT_EXIST"));
                if (error == 2)
                    writeOut(Message.get("PROGRAM_BOOT_NOT_OPEN"));
                return null;
            }
            // File exists; So can't be opened in correct mode => error 2
            // fclose(tmpfile);
            // if(tryload) error = 2;
            writeOut(Message.get("PROGRAM_BOOT_WRITE_PROTECTED"));
            // fseek(tmpfile,0L, SEEK_END);
            // *ksize = (ftell(tmpfile) / 1024);
            // *bsize = ftell(tmpfile);
            return tmpCh;
        }

        return tmpCh;
    }

    private SeekableByteChannel tryGetFSFile(String fileName) {
        return tryGetFSFile(fileName, false);
    }

    private void printError() {
        writeOut(Message.get("PROGRAM_BOOT_PRINT_ERROR"));
    }

    private void disableUMB_EMS_XMS() throws WrongType {
        Section dos_sec = DOSBox.Control.getSection("dos");
        dos_sec.executeDestroy(false);
        byte[] test = new byte[20];
        // CString.strcpy(test,"umb=false");
        dos_sec.handleInputline("umb=false");
        // CString.strcpy(test,"xms=false");
        dos_sec.handleInputline("xms=false");
        // CString.strcpy(test,"ems=false");
        dos_sec.handleInputline("ems=false");
        dos_sec.executeInit(false);
    }

    @Override
    public void run() throws WrongType {
        // Hack To allow long commandlines
        changeToLongCmd();
        /*
         * In secure mode don't allow people to boot stuff. They might try to corrupt the data on it
         */
        if (DOSBox.Control.getSecureMode()) {
            writeOut(Message.get("PROGRAM_CONFIG_SECURE_DISALLOW"));
            return;
        }

        SeekableByteChannel useFileCh1 = null;
        SeekableByteChannel useFileCh2 = null;
        int i = 0;
        int floppySize = 0;
        int romByteSize1 = 0;
        int romByteSize2 = 0;
        char drive = 'A';
        String cartCmd = "";

        if (Cmd.getCount() == 0) {
            printError();
            return;
        }
        while (i < Cmd.getCount()) {
            if ((TempLine = Cmd.findCommand(i + 1)) != null) {
                if ((TempLine == "-l") || (TempLine == "-L")) {
                    /* Specifying drive... next argument then is the drive */
                    i++;
                    if ((TempLine = Cmd.findCommand(i + 1)) != null) {
                        drive = Character.toUpperCase(TempLine.charAt(0));
                        if ((drive != 'A') && (drive != 'C') && (drive != 'D')) {
                            printError();
                            return;
                        }

                    } else {
                        printError();
                        return;
                    }
                    i++;
                    continue;
                }

                if ((TempLine == "-e") || (TempLine == "-E")) {
                    /* Command mode for PCJr cartridges */
                    i++;
                    if ((TempLine = Cmd.findCommand(i + 1)) != null) {
                        TempLine = TempLine.toUpperCase();
                        cartCmd = TempLine;
                    } else {
                        printError();
                        return;
                    }
                    i++;
                    continue;
                }

                writeOut(Message.get("PROGRAM_BOOT_IMAGE_OPEN"), TempLine);
                int romByteSize = 0;
                SeekableByteChannel usefile = tryGetFSFile(TempLine);
                floppySize = returnedFSFileKSize;
                romByteSize = returnedFSFileBSize;
                if (usefile != null) {
                    if (BIOSDisk.DiskSwap[i] != null)
                        BIOSDisk.DiskSwap[i] = null;
                    BIOSDisk.DiskSwap[i] = new ImageDisk(usefile, TempLine, floppySize, false);
                    if (useFileCh1 == null) {
                        useFileCh1 = usefile;
                        romByteSize1 = romByteSize;
                    } else {
                        useFileCh2 = usefile;
                        romByteSize2 = romByteSize;
                    }
                } else {
                    writeOut(Message.get("PROGRAM_BOOT_IMAGE_NOT_OPEN"), TempLine);
                    return;
                }

            }
            i++;
        }

        BIOSDisk.SwapPosition = 0;

        BIOSDisk.swapInDisks();

        if (BIOSDisk.ImageDiskList[drive - 65] == null) {
            writeOut(Message.get("PROGRAM_BOOT_UNABLE"), drive);
            return;
        }

        byte[] bootarea = new byte[bootSector_Size_Total];
        BIOSDisk.ImageDiskList[drive - 65].readSector(0, 0, 1, bootarea);
        if ((bootarea[0] == 0x50) && (bootarea[1] == 0x43) && (bootarea[2] == 0x6a)
                && (bootarea[3] == 0x72)) {
            if (DOSBox.Machine != DOSBox.MachineType.PCJR)
                writeOut(Message.get("PROGRAM_BOOT_CART_WO_PCJR"));
            else {
                byte[] tmSpB = new byte[] {(byte) ' ', (byte) '\0'};
                byte[] romBuf = new byte[65536];
                int cFoundAt = -1;
                if (cartCmd != "") {
                    /* read cartridge data into buffer */
                    ByteBuffer rdBuf = ByteBuffer.wrap(romBuf, 0, (romByteSize1 - 0x200) * 1);
                    try {
                        useFileCh1.position(0x200L).read(rdBuf);
                    } catch (Exception e) {

                    }

                    byte[] cmdlist = new byte[1024];
                    cmdlist[0] = 0;
                    int ct = 6;
                    int clen = romBuf[ct];
                    byte[] buf = new byte[257];
                    if (cartCmd == "?") {
                        while (clen != 0) {
                            CStringHelper.strncpy(buf, 0, romBuf, ct + 1, clen);
                            buf[clen] = 0;
                            CStringHelper.upcase(buf);
                            CStringHelper.strcat(cmdlist, tmSpB);
                            CStringHelper.strcat(cmdlist, buf);
                            ct += 1 + clen + 3;
                            if (ct > cmdlist.length)
                                break;
                            clen = romBuf[ct];
                        }
                        if (ct > 6) {
                            writeOut(Message.get("PROGRAM_BOOT_CART_LIST_CMDS"),
                                    new String(cmdlist, StandardCharsets.US_ASCII));
                        } else {
                            writeOut(Message.get("PROGRAM_BOOT_CART_NO_CMDS"));
                        }
                        for (int dct = 0; dct < BIOSDisk.MAX_SWAPPABLE_DISKS; dct++) {
                            if (BIOSDisk.DiskSwap[dct] != null) {
                                BIOSDisk.DiskSwap[dct].dispose();
                                BIOSDisk.DiskSwap[dct] = null;
                            }
                        }
                        // fclose(useFileCh1); //delete diskSwap closes the file
                        return;
                    } else {
                        while (clen != 0) {
                            CStringHelper.strncpy(buf, 0, romBuf, ct + 1, clen);
                            buf[clen] = 0;
                            CStringHelper.upcase(buf);
                            CStringHelper.strcat(cmdlist, tmSpB);
                            CStringHelper.strcat(cmdlist, buf);
                            ct += 1 + clen;
                            if (cartCmd == new String(buf, 0, CStringHelper.strlen(buf),
                                    StandardCharsets.US_ASCII)) {
                                cFoundAt = ct;
                                break;
                            }

                            ct += 3;
                            if (ct > cmdlist.length)
                                break;
                            clen = romBuf[ct];
                        }
                        if (cFoundAt <= 0) {
                            if (ct > 6) {
                                writeOut(Message.get("PROGRAM_BOOT_CART_LIST_CMDS"),
                                        new String(cmdlist, StandardCharsets.US_ASCII));
                            } else {
                                writeOut(Message.get("PROGRAM_BOOT_CART_NO_CMDS"));
                            }
                            for (int dct = 0; dct < BIOSDisk.MAX_SWAPPABLE_DISKS; dct++) {
                                if (BIOSDisk.DiskSwap[dct] != null) {
                                    BIOSDisk.DiskSwap[dct].dispose();
                                    BIOSDisk.DiskSwap[dct] = null;
                                }
                            }
                            // fclose(usefile_1); //Delete diskSwap closes the file
                            return;
                        }
                    }
                }

                disableUMB_EMS_XMS();
                Memory.preparePCJRCartRom();

                if (useFileCh1 == null)
                    return;

                SeekableByteChannel tfile = tryGetFSFile("system.rom", true);
                if (tfile != null) {
                    ByteBuffer rbBuf = ByteBuffer.wrap(romBuf, 0, 1 * 0xb000);
                    try {
                        int drd = tfile.position(0x3000L).read(rbBuf);
                        if (drd == 0xb000) {
                            for (i = 0; i < 0xb000; i++)
                                Memory.physWriteB(0xf3000 + i, romBuf[i]);
                        }
                        tfile.close();
                    } catch (Exception e) {
                    }
                }

                if (useFileCh2 != null) {
                    ByteBuffer rbBuf = ByteBuffer.wrap(romBuf, 0, 1 * 0x200);
                    try {
                        useFileCh2.position(0x0L).read(rbBuf);
                        int romsegPt = Memory.hostReadW(romBuf, 0x1ce) << 4;

                        /* read cartridge data into buffer */
                        rbBuf = ByteBuffer.wrap(romBuf, 0, 1 * (romByteSize2 - 0x200));
                        useFileCh2.position(0x200L).read(rbBuf);
                        // fclose(usefile_2); //usefile_2 is in diskSwap structure which should be
                        // deleted to close the file

                        /* write cartridge data into ROM */
                        for (i = 0; i < romByteSize2 - 0x200; i++)
                            Memory.physWriteB(romsegPt + i, romBuf[i]);
                    } catch (Exception e) {
                    }
                }
                ByteBuffer rbBuf = ByteBuffer.wrap(romBuf, 0, 1 * 0x200);
                try {
                    useFileCh1.position(0x0L).read(rbBuf);
                    int romseg = Memory.hostReadW(romBuf, 0x1ce);

                    /* read cartridge data into buffer */
                    rbBuf = ByteBuffer.wrap(romBuf, 0, 1 * (romByteSize1 - 0x200));
                    useFileCh1.position(0x200L).read(rbBuf);
                    // fclose(usefile_1); //usefile_1 is in diskSwap structure which should be
                    // deleted to close the file

                    /* write cartridge data into ROM */
                    for (i = 0; i < romByteSize1 - 0x200; i++)
                        Memory.physWriteB((romseg << 4) + i, romBuf[i]);

                    // Close cardridges
                    for (int dct = 0; dct < BIOSDisk.MAX_SWAPPABLE_DISKS; dct++) {
                        if (BIOSDisk.DiskSwap[dct] != null) {
                            BIOSDisk.DiskSwap[dct].dispose();
                            BIOSDisk.DiskSwap[dct] = null;
                        }
                    }


                    if (cartCmd == "") {
                        int old_int18 = Memory.readD(0x60);
                        /* run cartridge setup */
                        Register.segSet16(Register.SEG_NAME_DS, romseg);
                        Register.segSet16(Register.SEG_NAME_ES, romseg);
                        Register.segSet16(Register.SEG_NAME_SS, 0x8000);
                        Register.setRegESP(0xfffe);
                        Callback.runRealFar(romseg, 0x0003);

                        int new_int18 = Memory.readD(0x60);
                        if (old_int18 != new_int18) {
                            /* boot cartridge (int18) */
                            Register.segSet16(Register.SEG_NAME_CS, Memory.realSeg(new_int18));
                            Register.setRegIP(Memory.realOff(new_int18));
                        }
                    } else {
                        if (cFoundAt > 0) {
                            /* run cartridge setup */
                            Register.segSet16(Register.SEG_NAME_DS, DOSMain.DOS.getPSP());
                            Register.segSet16(Register.SEG_NAME_ES, DOSMain.DOS.getPSP());
                            Callback.runRealFar(romseg, cFoundAt);
                        }
                    }
                } catch (Exception e) {
                }
            }
        } else {
            disableUMB_EMS_XMS();
            Memory.removeEMSPageFrame();
            writeOut(Message.get("PROGRAM_BOOT_BOOT"), (char) drive);
            for (i = 0; i < 512; i++)
                Memory.realWriteB(0, 0x7c00 + i, bootarea[i]);

            /* revector some dos-allocated interrupts */
            Memory.realWriteD(0, 0x01 * 4, 0xf000ff53);
            Memory.realWriteD(0, 0x03 * 4, 0xf000ff53);

            Register.segSet16(Register.SEG_NAME_CS, 0);
            Register.setRegIP(0x7c00);
            Register.segSet16(Register.SEG_NAME_DS, 0);
            Register.segSet16(Register.SEG_NAME_ES, 0);
            /* set up stack at a safe place */
            Register.segSet16(Register.SEG_NAME_SS, 0x7000);
            Register.setRegESP(0x100);
            Register.setRegESI(0);
            Register.setRegECX(1);
            Register.setRegEBP(0);
            Register.setRegEAX(0);
            Register.setRegEDX(0); // Head 0 drive 0
            Register.setRegEBX(0x7c00); // Real code probably uses bx to load the image
        }
    }
}
