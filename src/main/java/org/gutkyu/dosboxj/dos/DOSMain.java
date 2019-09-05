package org.gutkyu.dosboxj.dos;

import org.gutkyu.dosboxj.misc.setup.*;
import org.gutkyu.dosboxj.cpu.*;
import org.gutkyu.dosboxj.dos.mem_block.*;
import org.gutkyu.dosboxj.hardware.memory.*;
import org.gutkyu.dosboxj.misc.*;
import org.gutkyu.dosboxj.util.*;
import org.gutkyu.dosboxj.interrupt.bios.*;
import org.gutkyu.dosboxj.dos.system.device.*;
import org.gutkyu.dosboxj.dos.system.drive.*;
import org.gutkyu.dosboxj.gui.*;
import org.gutkyu.dosboxj.dos.system.file.*;
import java.util.Random;
import org.gutkyu.dosboxj.*;
import org.gutkyu.dosboxj.dos.keyboardlayout.*;
import org.gutkyu.dosboxj.dos.software.*;



public final class DOSMain {
    public static DOSBlock DOS = new DOSBlock();
    public static DOSInfoBlock DOSInfoBlock = new DOSInfoBlock();

    public static final byte RETURN_TYPE_EXIT = 0;
    public static final byte RETURN_TYPE_CTRLC = 1;
    public static final byte RETURN_TYPE_ABORT = 2;
    public static final byte RETURN_TYPE_TSR = 3;

    public static final byte DOS_FILES = 127;
    public static final byte DOS_DRIVES = 26;
    public static final byte DOS_DEVICES = 10;


    // dos swappable area is 0x320 bytes beyond the sysvars table
    // device driver chain is inside sysvars
    public static final short DOS_INFOBLOCK_SEG = 0x80; // sysvars (list of lists)
    public static final short DOS_CONDRV_SEG = 0xa0;
    public static final short DOS_CONSTRING_SEG = 0xa8;
    public static final short DOS_SDA_SEG = 0xb2; // dos swappable area
    public static final short DOS_SDA_OFS = 0;
    public static final short DOS_CDS_SEG = 0x108;
    public static final short DOS_FIRST_SHELL = 0x118;
    public static final short DOS_MEM_START = 0x16f; // First Segment that DOS can use

    public static final int DOS_PRIVATE_SEGMENT = 0xc800;
    public static final int DOS_PRIVATE_SEGMENT_END = 0xd000;


    /* File Handling Routines */

    public static final byte STDIN = 0, STDOUT = 1, STDERR = 2, STDAUX = 3, STDPRN = 4;

    public static final short FileHAND_NONE = 0;
    public static final short FileHAND_FILE = 1;
    public static final short FileHAND_DEVICE = 2;

    private static final int DOS_COPYBUFSIZE = 0x10000;
    private static byte[] dosCopyBuf = new byte[DOS_COPYBUFSIZE];


    public static String dbgCurLoadedProgram = "";

    // uint16(int)
    private static int doLong2Para(int size) {
        if (size > 0xFFFF0)
            return 0xffff;
        if ((size & 0xf) != 0)
            return 0xffff & ((size >>> 4) + 1);
        else
            return 0xffff & (size >>> 4);
    }


    // uint16(uint16, uint16, uint16)
    public static int packTime(int hour, int min, int sec) {
        return 0xffff & ((hour & 0x1f) << 11 | (min & 0x3f) << 5 | ((sec / 2) & 0x1f));
    }

    // uint16(uint16, uint16, uint16)
    public static int packDate(int year, int mon, int day) {
        return 0xffff & (((year - 1980) & 0x7f) << 9 | (mon & 0x3f) << 5 | (day & 0x1f));
    }

    /* Dos Error Codes */
    public static final short DOSERR_NONE = 0;
    public static final short DOSERR_FUNCTION_NUMBER_INVALID = 1;
    public static final short DOSERR_FILE_NOT_FOUND = 2;
    public static final short DOSERR_PATH_NOT_FOUND = 3;
    public static final short DOSERR_TOO_MANY_OPEN_FILES = 4;
    public static final short DOSERR_ACCESS_DENIED = 5;
    public static final short DOSERR_INVALID_HANDLE = 6;
    public static final short DOSERR_MCB_DESTROYED = 7;
    public static final short DOSERR_INSUFFICIENT_MEMORY = 8;
    public static final short DOSERR_MB_ADDRESS_INVALID = 9;
    public static final short DOSERR_ENVIRONMENT_INVALID = 10;
    public static final short DOSERR_FORMAT_INVALID = 11;
    public static final short DOSERR_ACCESS_CODE_INVALID = 12;
    public static final short DOSERR_DATA_INVALID = 13;
    public static final short DOSERR_RESERVED = 14;
    public static final short DOSERR_FIXUP_OVERFLOW = 14;
    public static final short DOSERR_INVALID_DRIVE = 15;
    public static final short DOSERR_REMOVE_CURRENT_DIRECTORY = 16;
    public static final short DOSERR_NOT_SAME_DEVICE = 17;
    public static final short DOSERR_NO_MORE_FILES = 18;
    public static final short DOSERR_FILE_ALREADY_EXISTS = 80;



    public static void setError(short code) {
        DOS.ErrorCode = code;
    }

    static void modifyCycles(int value) {
        if ((4 * value + 5) < CPU.Cycles) {
            CPU.Cycles -= 4 * value;
            CPU.IODelayRemoved += 4 * value;
        } else {
            CPU.IODelayRemoved += CPU.Cycles/*-5*/; // don't want to mess with negative
            CPU.Cycles = 5;
        }
    }

    static void overhead() {
        Register.setRegIP(Register.getRegIP() + 2);
    }

    public static int realHandle(int handle) {
        DOSPSP psp = new DOSPSP(DOS.getPSP());
        return psp.getFileHandle(handle);
    }

    private static final int DOSNAMEBUF = 256;


    public static int INT21Handler() {
        if (((Register.getRegAH() != 0x50) && (Register.getRegAH() != 0x51)
                && (Register.getRegAH() != 0x62) && (Register.getRegAH() != 0x64))
                && (Register.getRegAH() < 0x6c)) {
            DOSPSP psp = new DOSPSP(DOS.getPSP());
            psp.setStack(Memory.realMake(Register.segValue(Register.SEG_NAME_SS),
                    Register.getRegSP() - 18));
        }

        String name1 = null;
        String name2 = null;
        CStringPt name1pt = CStringPt.create(DOSNAMEBUF + 2 + DOSSystem.DOS_NAMELENGTH_ASCII);
        CStringPt name2pt = CStringPt.create(DOSNAMEBUF + 2 + DOSSystem.DOS_NAMELENGTH_ASCII);

        switch (Register.getRegAH()) {
            case 0x00: /* Terminate Program */
                terminate(
                        Memory.readW(
                                Register.segPhys(Register.SEG_NAME_SS) + Register.getRegSP() + 2),
                        false, 0);
                break;
            case 0x01: /* Read character from STDIN, with echo */
            {
                DOS.Echo = true;
                readFile(STDIN);
                Register.setRegAL(ReadByte);
                DOS.Echo = false;
            }
                break;
            case 0x02: /* Write character to STDOUT */
            {
                byte c = (byte) Register.getRegDL();
                writeFile(STDOUT, c);
                // Not in the official specs, but happens nonetheless. (last written character)
                Register.setRegAL(c);// regsModule.reg_al=(c==9)?0x20:c); //Officially: tab to
                                     // spaces
            }
                break;
            case 0x03: /* Read character from STDAUX */
            {
                // short port = MemModule.real_readw(0x40,0);
                // if(port!=0 && serialports[0]) {
                // byte status;
                // // RTS/DTR on
                // iohandler.IO_WriteB(port+4,0x3);
                // serialports[0].Getchar(&regsModule.reg_al, &status, true, 0xFFFFFFFF);
                // }
            }
                break;
            case 0x04: /* Write Character to STDAUX */
            {
                // short port = MemModule.real_readw(0x40,0);
                // if(port!=0 && serialports[0]) {
                // // RTS/DTR on
                // iohandler.IO_WriteB(port+4,0x3);
                // serialports[0].Putchar(regsModule.reg_dl,true,true, 0xFFFFFFFF);
                // // RTS off
                // iohandler.IO_WriteB(port+4,0x1);
                // }
            }
                break;
            case 0x05: /* Write Character to PRINTER */
                Support.exceptionExit("DOS:Unhandled call %02X", Register.getRegAH());
                break;
            case 0x06: /* Direct Console Output / Input */
                switch (Register.getRegDL()) {
                    case 0xFF: /* Input */
                    {
                        // Simulate DOS overhead for timing sensitive games
                        // MM1
                        overhead();
                        // TODO Make this better according to standards
                        if (!getSTDINStatus()) {
                            Register.setRegAL(0);
                            Callback.szf(true);
                            break;
                        }
                        byte c = 0;
                        short n = 1;
                        readFile(STDIN);
                        c = ReadByte;
                        Register.setRegAL(c);
                        Callback.szf(false);
                        break;
                    }
                    default: {
                        byte c = (byte) Register.getRegDL();
                        // short n = 1;
                        writeFile(STDOUT, c);

                        Register.setRegAL(Register.getRegDL());
                    }
                        break;
                }
                break;
            case 0x07: /* Character Input, without echo */
            {
                readFile(STDIN);
                Register.setRegAL(ReadByte);
                break;
            }
            case 0x08: /* Direct Character Input, without echo (checks for breaks officially :) */
            {
                readFile(STDIN);
                Register.setRegAL(ReadByte);
                break;
            }
            case 0x09: /* Write string to STDOUT */
            {
                byte c = 0;
                // short n = 1;
                int buf = Register.segPhys(Register.SEG_NAME_DS) + Register.getRegDX();
                while ((c = (byte) Memory.readB(buf++)) != '$') {
                    writeFile(STDOUT, c);
                }
            }
                break;
            case 0x0a: /* Buffered Input */
            {
                // TODO ADD Break checkin in STDIN but can't care that much for it
                int data = Register.segPhys(Register.SEG_NAME_DS) + Register.getRegDX();
                int free = Memory.readB(data);
                byte read = 0;
                byte c = 0;
                // short n = 1;
                if (free == 0)
                    break;
                for (;;) {
                    readFile(STDIN);
                    c = ReadByte;
                    if (ReadByte == 8) { // Backspace
                        if (read != 0) {
                            // Something to backspace.
                            // STDOUT treats backspace as non-destructive.
                            writeFile(STDOUT, c);
                            c = (byte) ' ';
                            writeFile(STDOUT, c);
                            c = 8;
                            writeFile(STDOUT, c);
                            --read;
                        }
                        continue;
                    }
                    if (read >= free) { // Keyboard buffer full
                        c = 7;
                        writeFile(STDOUT, c);
                        continue;
                    }
                    writeFile(STDOUT, c);
                    Memory.writeB(data + read + 2, c);
                    if (c == 13)
                        break;
                    read++;
                }
                Memory.writeB(data + 1, read);
                break;
            }
            case 0x0b: /* Get STDIN Status */
                if (!getSTDINStatus()) {
                    Register.setRegAL(0x00);
                } else {
                    Register.setRegAL(0xFF);
                }
                // Simulate some overhead for timing issues
                // Tankwar menu (needs maybe even more)
                overhead();
                break;
            case 0x0c: /* Flush Buffer and read STDIN call */
            {
                /* flush STDIN-buffer */
                byte c = 0;
                int n;
                while (getSTDINStatus()) {
                    n = 1;
                    readFile(STDIN);
                    c = ReadByte;
                }
                switch (Register.getRegAL()) {
                    case 0x1:
                    case 0x6:
                    case 0x7:
                    case 0x8:
                    case 0xa: {
                        int oldah = Register.getRegAH();
                        Register.setRegAH(Register.getRegAL());
                        INT21Handler();
                        Register.setRegAH(oldah);
                    }
                        break;
                    default:
                        // LOG_ERROR("DOS:0C:Illegal Flush STDIN Buffer call %d",regsModule.reg_al);
                        Register.setRegAL(0);
                        break;
                }
            }
                break;
            // TODO Find out the values for when regsModule.reg_al!=0
            // TODO Hope this doesn't do anything special
            case 0x0d: /* Disk Reset */
                // Sure let's reset a virtual disk
                break;
            case 0x0e: /* Select Default Drive */
                setDefaultDrive(Register.getRegDL());
                Register.setRegAL(DOS_DRIVES);
                break;
            case 0x0f: /* Open File using FCB */
                if (doFCBOpen(Register.segValue(Register.SEG_NAME_DS), Register.getRegDX())) {
                    Register.setRegAL(0);
                } else {
                    Register.setRegAL(0xff);
                }
                Log.logging(Log.LogTypes.FCB, Log.LogServerities.Normal,
                        "DOS:0x0f FCB-fileopen used, result:al=%d", Register.getRegAL());
                break;
            case 0x10: /* Close File using FCB */
                if (doFCBClose(Register.segValue(Register.SEG_NAME_DS), Register.getRegDX())) {
                    Register.setRegAL(0);
                } else {
                    Register.setRegAL(0xff);
                }
                Log.logging(Log.LogTypes.FCB, Log.LogServerities.Normal,
                        "DOS:0x10 FCB-fileclose used, result:al=%d", Register.getRegAL());
                break;
            case 0x11: /* Find First Matching File using FCB */
                if (doFCBFindFirst(Register.segValue(Register.SEG_NAME_DS), Register.getRegDX()))
                    Register.setRegAL(0x00);
                else
                    Register.setRegAL(0xFF);
                Log.logging(Log.LogTypes.FCB, Log.LogServerities.Normal,
                        "DOS:0x11 FCB-FindFirst used, result:al=%d", Register.getRegAL());
                break;
            case 0x12: /* Find Next Matching File using FCB */
                if (doFCBFindNext(Register.segValue(Register.SEG_NAME_DS), Register.getRegDX()))
                    Register.setRegAL(0x00);
                else
                    Register.setRegAL(0xFF);
                Log.logging(Log.LogTypes.FCB, Log.LogServerities.Normal,
                        "DOS:0x12 FCB-FindNext used, result:al=%d", Register.getRegAL());
                break;
            case 0x13: /* Delete File using FCB */
                if (doFCBDeleteFile(Register.segValue(Register.SEG_NAME_DS), Register.getRegDX()))
                    Register.setRegAL(0x00);
                else
                    Register.setRegAL(0xFF);
                Log.logging(Log.LogTypes.FCB, Log.LogServerities.Normal,
                        "DOS:0x16 FCB-Delete used, result:al=%d", Register.getRegAL());
                break;
            case 0x14: /* Sequential read from FCB */
                Register.setRegAL(
                        doFCBRead(Register.segValue(Register.SEG_NAME_DS), Register.getRegDX(), 0));
                Log.logging(Log.LogTypes.FCB, Log.LogServerities.Normal,
                        "DOS:0x14 FCB-Read used, result:al=%d", Register.getRegAL());
                break;
            case 0x15: /* Sequential write to FCB */
                Register.setRegAL(doFCBWrite(Register.segValue(Register.SEG_NAME_DS),
                        Register.getRegDX(), 0));
                Log.logging(Log.LogTypes.FCB, Log.LogServerities.Normal,
                        "DOS:0x15 FCB-Write used, result:al=%d", Register.getRegAL());
                break;
            case 0x16: /* Create or truncate file using FCB */
                if (doFCBCreate(Register.segValue(Register.SEG_NAME_DS), Register.getRegDX()))
                    Register.setRegAL(0x00);
                else
                    Register.setRegAL(0xFF);
                Log.logging(Log.LogTypes.FCB, Log.LogServerities.Normal,
                        "DOS:0x16 FCB-Create used, result:al=%d", Register.getRegAL());
                break;
            case 0x17: /* Rename file using FCB */
                if (doFCBRenameFile(Register.segValue(Register.SEG_NAME_DS), Register.getRegDX()))
                    Register.setRegAL(0x00);
                else
                    Register.setRegAL(0xFF);
                break;
            case 0x1b: /* Get allocation info for default drive */
            {
                DriveAllocationInfo alloc = new DriveAllocationInfo(
                        Register.Regs[Register.CX].getWord(), Register.Regs[Register.AX].getByteL(),
                        Register.Regs[Register.DX].getWord());
                if (getAllocationInfo(0, alloc)) {
                    Register.Regs[Register.CX].setWord(alloc.bytesSector);
                    Register.Regs[Register.AX].setByteL(alloc.sectorsCluster);
                    Register.Regs[Register.DX].setWord(alloc.totalClusters);
                } else {
                    Register.setRegAL(0xff);

                }
                break;
            }
            case 0x1c: /* Get allocation info for specific drive */
            {
                DriveAllocationInfo alloc = new DriveAllocationInfo(
                        Register.Regs[Register.CX].getWord(), Register.Regs[Register.AX].getByteL(),
                        Register.Regs[Register.DX].getWord());
                if (getAllocationInfo(Register.getRegDL(), alloc)) {
                    Register.Regs[Register.CX].setWord(alloc.bytesSector);
                    Register.Regs[Register.AX].setByteL(alloc.sectorsCluster);
                    Register.Regs[Register.DX].setWord(alloc.totalClusters);
                } else {
                    Register.setRegAL(0xff);
                }
                break;
            }
            case 0x21: /* Read random record from FCB */
                Register.setRegAL(doFCBRandomRead(Register.segValue(Register.SEG_NAME_DS),
                        Register.getRegDX(), 1, true));
                Log.logging(Log.LogTypes.FCB, Log.LogServerities.Normal,
                        "DOS:0x21 FCB-Random read used, result:al=%d", Register.getRegAL());
                break;
            case 0x22: /* Write random record to FCB */
                Register.setRegAL(doFCBRandomWrite(Register.segValue(Register.SEG_NAME_DS),
                        Register.getRegDX(), 1, true));
                Log.logging(Log.LogTypes.FCB, Log.LogServerities.Normal,
                        "DOS:0x22 FCB-Random write used, result:al=%d", Register.getRegAL());
                break;
            case 0x23: /* Get file size for FCB */
                if (doFCBGetFileSize(Register.segValue(Register.SEG_NAME_DS), Register.getRegDX()))
                    Register.setRegAL(0x00);
                else
                    Register.setRegAL(0xFF);
                break;
            case 0x24: /* Set Random Record number for FCB */
                doFCBSetRandomRecord(Register.segValue(Register.SEG_NAME_DS), Register.getRegDX());
                break;
            case 0x27: /* Random block read from FCB */
                Register.setRegAL(doFCBRandomRead(Register.segValue(Register.SEG_NAME_DS),
                        Register.getRegDX(), Register.getRegCX(), false));
                Log.logging(Log.LogTypes.FCB, Log.LogServerities.Normal,
                        "DOS:0x27 FCB-Random(block) read used, result:al=%d", Register.getRegAL());
                break;
            case 0x28: /* Random Block write to FCB */
                Register.setRegAL(doFCBRandomWrite(Register.segValue(Register.SEG_NAME_DS),
                        Register.getRegDX(), Register.getRegCX(), false));
                Log.logging(Log.LogTypes.FCB, Log.LogServerities.Normal,
                        "DOS:0x28 FCB-Random(block) write used, result:al=%d", Register.getRegAL());
                break;
            case 0x29: /* Parse filename into FCB */
            {
                CStringPt str = CStringPt.create(1024);
                Memory.strCopy(Register.segPhys(Register.SEG_NAME_DS) + Register.getRegSI(), str,
                        1023); // 1024 toasts the stack
                Register.setRegAL(FCBParseName(Register.segValue(Register.SEG_NAME_ES),
                        Register.getRegDI(), Register.getRegAL(), str));
                Register.setRegSI(Register.getRegSI() + returnedFCBParseNameChange);
            }
                Log.logging(Log.LogTypes.FCB, Log.LogServerities.Normal,
                        "DOS:29:FCB Parse Filename, result:al=%d", Register.getRegAL());
                break;
            case 0x19: /* Get current default drive */
                Register.setRegAL(getDefaultDrive());
                break;
            case 0x1a: /* Set Disk Transfer Area Address */
                DOS.setDTA(Register.realMakeSeg(Register.SEG_NAME_DS, Register.getRegDX()));
                break;
            case 0x25: /* Set Interrupt Vector */
                Memory.realSetVec(Register.getRegAL(),
                        Register.realMakeSeg(Register.SEG_NAME_DS, Register.getRegDX()));
                break;
            case 0x26: /* Create new PSP */
                newPSP(Register.getRegDX(), new DOSPSP(DOS.getPSP()).getSize());
                break;
            case 0x2a: /* Get System Date */
            {
                int a = (14 - DOS.Date.Month) / 12;
                int y = DOS.Date.Year - a;
                int m = DOS.Date.Month + 12 * a - 2;
                Register.setRegAL(
                        ((DOS.Date.Day + y + (y / 4) - (y / 100) + (y / 400) + (31 * m) / 12) % 7));
                Register.setRegCX(DOS.Date.Year);
                Register.setRegDH(DOS.Date.Month);
                Register.setRegDL(DOS.Date.Day);
            }
                break;
            case 0x2b: /* Set System Date */
                if (Register.getRegCX() < 1980) {
                    Register.setRegAL(0xff);
                    break;
                }
                if ((Register.getRegDH() > 12) || (Register.getRegDH() == 0)) {
                    Register.setRegAL(0xff);
                    break;
                }
                if ((Register.getRegDL() > 31) || (Register.getRegDL() == 0)) {
                    Register.setRegAL(0xff);
                    break;
                }
                DOS.Date.Year = Register.getRegCX();
                DOS.Date.Month = Register.getRegDH();
                DOS.Date.Day = Register.getRegDL();
                Register.setRegAL(0);
                break;
            case 0x2c: /* Get System Time */
            // TODO Get time through bios calls date is fixed
            {
                /* Calculate how many miliseconds have passed */
                int ticks = 5 * Memory.readD(BIOS.BIOS_TIMER);
                ticks = ((ticks / 59659) << 16) + ((ticks % 59659) << 16) / 59659;
                int seconds = (ticks / 100);
                Register.setRegCH(0xff & (seconds / 3600));
                Register.setRegCL(0xff & ((seconds % 3600) / 60));
                Register.setRegDH(0xff & (seconds % 60));
                Register.setRegDL(0xff & (ticks % 100));
            }
                // Simulate DOS overhead for timing-sensitive games
                // Robomaze 2
                overhead();
                break;
            case 0x2d: /* Set System Time */
                Log.logging(Log.LogTypes.DOSMISC, Log.LogServerities.Error,
                        "DOS:Set System Time not supported");
                // Check input parameters nonetheless
                if (Register.getRegCH() > 23 || Register.getRegCL() > 59 || Register.getRegDH() > 59
                        || Register.getRegDL() > 99)
                    Register.setRegAL(0xff);
                else
                    Register.setRegAL(0);
                break;
            case 0x2e: /* Set Verify flag */
                DOS.Verify = (Register.getRegAL() == 1);
                break;
            case 0x2f: /* Get Disk Transfer Area */
                Register.segSet16(Register.SEG_NAME_ES, Memory.realSeg(DOS.getDTA()));
                Register.setRegBX(Memory.realOff(DOS.getDTA()));
                break;
            case 0x30: /* Get DOS Version */
                if (Register.getRegAL() == 0)
                    Register.setRegBH(0xFF); /* Fake Microsoft DOS */
                if (Register.getRegAL() == 1)
                    Register.setRegBH(0x10); /* DOS is in HMA */
                Register.setRegAL(DOS.Version.major);
                Register.setRegAH(DOS.Version.minor);
                /* Serialnumber */
                Register.setRegBL(0x00);
                Register.setRegCX(0x0000);
                break;
            case 0x31: /* Terminate and stay resident */
                // Important: This service does not set the carry flag!
                tryResizeMemory(DOS.getPSP(), Register.getRegDX());
                Register.setRegDX(returnedResizedMemoryBlocks);
                terminate(DOS.getPSP(), true, Register.getRegAL());
                break;
            case 0x1f: /* Get drive parameter block for default drive */
            case 0x32: /* Get drive parameter block for specific drive */
            { /*
               * Officially a dpb should be returned as well. The disk detection part is implemented
               */
                int drive = Register.getRegDL();
                if (drive == 0 || Register.getRegAH() == 0x1f)
                    drive = getDefaultDrive();
                else
                    drive--;
                if (Drives[drive] != null) {
                    Register.setRegAL(0x00);
                    Register.segSet16(Register.SEG_NAME_DS, DOS.tables.DPB);
                    Register.setRegBX(drive);// Faking only the first entry (that is the
                                             // driveletter)
                    Log.logging(Log.LogTypes.DOSMISC, Log.LogServerities.Error,
                            "Get drive parameter block.");
                } else {
                    Register.setRegAL(0xff);
                }
            }
                break;
            case 0x33: /* Extended Break Checking */
                switch (Register.getRegAL()) {
                    case 0:
                        Register.setRegDL(Convert.toByte(DOS.BreakCheck));
                        break; /* Get the breakcheck flag */
                    case 1:
                        DOS.BreakCheck = (Register.getRegDL() > 0);
                        break; /* Set the breakcheck flag */
                    case 2: {
                        boolean old = DOS.BreakCheck;
                        DOS.BreakCheck = (Register.getRegDL() > 0);
                        Register.setRegDL(old ? 1 : 0);
                    }
                        break;
                    case 3: /* Get cpsw */
                        /* Fallthrough */
                    case 4: /* Set cpsw */
                        Log.logging(Log.LogTypes.DOSMISC, Log.LogServerities.Error,
                                "Someone playing with cpsw %x", Register.getRegAX());
                        break;
                    case 5:
                        Register.setRegDL(3);
                        break;// TODO should be z /* Always boot from c: :) */
                    case 6: /* Get true version number */
                        Register.setRegBL(DOS.Version.major);
                        Register.setRegBH(DOS.Version.minor);
                        Register.setRegDL(DOS.Version.revision);
                        Register.setRegDH(0x10); /* Dos in HMA */
                        break;
                    default:
                        Support.exceptionExit("DOS:Illegal 0x33 Call %2X", Register.getRegAL());
                        break;
                }
                break;
            case 0x34: /* Get INDos Flag */
                Register.segSet16(Register.SEG_NAME_ES, DOS_SDA_SEG);
                Register.setRegBX(DOS_SDA_OFS + 0x01);
                break;
            case 0x35: /* Get interrupt vector */
                Register.setRegBX(Memory.realReadW(0, Register.getRegAL() * 4));
                Register.segSet16(Register.SEG_NAME_ES,
                        Memory.realReadW(0, Register.getRegAL() * 4 + 2));
                break;
            case 0x36: /* Get Free Disk Space */
            {
                DriveAllocationInfo alloc = new DriveAllocationInfo(0, 0, 0, 0);
                if (getFreeDiskSpace(Register.getRegDL(), alloc)) {
                    Register.setRegAX(alloc.sectorsCluster);
                    Register.setRegBX(alloc.freeClusters);
                    Register.setRegCX(alloc.bytesSector);
                    Register.setRegDX(alloc.totalClusters);
                } else {
                    int drive = Register.getRegDL();
                    if (drive == 0)
                        drive = getDefaultDrive();
                    else
                        drive--;
                    if (drive < 2) {
                        // floppy drive, non-present drivesdisks issue floppy check through int24
                        // (critical error handler); needed for Mixed up Mother Goose (hook)
                        // CALLBACK_RunRealInt(0x24);
                    }
                    Register.setRegAX(0xffff); // invalid drive specified
                }
            }
                break;
            case 0x37: /* Get/Set Switch char Get/Set Availdev thing */
                // TODO Give errors for these functions to see if anyone actually uses this shit-
                switch (Register.getRegAL()) {
                    case 0:
                        Register.setRegAL(0);
                        Register.setRegDL(0x2f);
                        break; /* always return '/' like dos 5.0+ */
                    case 1:
                        Register.setRegAL(0);
                        break;
                    case 2:
                        Register.setRegAL(0);
                        Register.setRegDL(0x2f);
                        break;
                    case 3:
                        Register.setRegAL(0);
                        break;
                }
                Log.logging(Log.LogTypes.MISC, Log.LogServerities.Error,
                        "DOS:0x37:Call for not supported switchchar");
                break;
            case 0x38: /* Set Country Code */
                if (Register.getRegAL() == 0) { /* Get country specidic information */
                    int dest = Register.segPhys(Register.SEG_NAME_DS) + Register.getRegDX();
                    Memory.blockWrite(dest, DOS.tables.Country, 0, 0x18);
                    Register.setRegAX(0x01);
                    Register.setRegBX(0x01);
                    Callback.scf(false);
                    break;
                } else { /* Set country code */
                    Log.logging(Log.LogTypes.MISC, Log.LogServerities.Error,
                            "DOS:Setting country code not supported");
                }
                Callback.scf(true);
                break;
            case 0x39: /* MKDIR Create directory */
                name1 = Memory.strCopy(Register.segPhys(Register.SEG_NAME_DS) + Register.getRegDX(),
                        DOSNAMEBUF);
                if (makeDir(name1)) {
                    Callback.scf(false);
                } else {
                    Register.setRegAX(DOS.ErrorCode);
                    Callback.scf(true);
                }
                break;
            case 0x3a: /* RMDIR Remove directory */
                name1 = Memory.strCopy(Register.segPhys(Register.SEG_NAME_DS) + Register.getRegDX(),
                        DOSNAMEBUF);
                if (removeDir(name1)) {
                    Callback.scf(false);
                } else {
                    Register.setRegAX(DOS.ErrorCode);
                    Callback.scf(true);
                    Log.logging(Log.LogTypes.MISC, Log.LogServerities.Normal,
                            "Remove dir failed on %s with error %X", name1, DOS.ErrorCode);
                }
                break;
            case 0x3b: /* CHDIR Set current directory */
                name1 = Memory.strCopy(Register.segPhys(Register.SEG_NAME_DS) + Register.getRegDX(),
                        DOSNAMEBUF);
                if (changeDir(name1)) {
                    Callback.scf(false);
                } else {
                    Register.setRegAX(DOS.ErrorCode);
                    Callback.scf(true);
                }
                break;
            case 0x3c: /* CREATE Create of truncate file */
            {
                name1 = Memory.strCopy(Register.segPhys(Register.SEG_NAME_DS) + Register.getRegDX(),
                        DOSNAMEBUF);
                if (createFile(name1, Register.getRegCX())) {
                    Register.Regs[Register.AX].setWord(returnFileHandle);
                    Callback.scf(false);
                } else {
                    Register.setRegAX(DOS.ErrorCode);
                    Callback.scf(true);
                }
                break;
            }
            case 0x3d: /* OPEN Open existing file */
            {
                name1 = Memory.strCopy(Register.segPhys(Register.SEG_NAME_DS) + Register.getRegDX(),
                        DOSNAMEBUF);
                if (openFile(name1, Register.getRegAL())) {
                    Register.Regs[Register.AX].setWord(returnFileHandle);
                    Callback.scf(false);
                } else {
                    Register.setRegAX(DOS.ErrorCode);
                    Callback.scf(true);
                }
                break;
            }
            case 0x3e: /* CLOSE Close file */
                if (closeFile(Register.getRegBX())) {
                    Callback.scf(false);
                } else {
                    Register.setRegAX(DOS.ErrorCode);
                    Callback.scf(true);
                }
                break;
            case 0x3f: /* READ Read from file or device */
            {
                int toread = Register.getRegCX();
                DOS.Echo = true;
                if (readFile(Register.getRegBX(), dosCopyBuf, 0, toread)) {
                    toread = readFileSize;
                    Memory.blockWrite(Register.segPhys(Register.SEG_NAME_DS) + Register.getRegDX(),
                            dosCopyBuf, 0, toread);
                    // toread = (int) refBuf.Len;
                    Register.setRegAX(toread);
                    Callback.scf(false);
                } else {
                    Register.setRegAX(DOS.ErrorCode);
                    Callback.scf(true);
                }
                modifyCycles(Register.getRegAX());
                DOS.Echo = false;
                break;
            }
            case 0x40: /* WRITE Write to file or device */
            {
                int towrite = Register.getRegCX();
                Memory.blockRead(Register.segPhys(Register.SEG_NAME_DS) + Register.getRegDX(),
                        dosCopyBuf, 0, towrite);
                if (writeFile(Register.getRegBX(), dosCopyBuf, 0, towrite)) {
                    towrite = DOSMain.WrittenSize;
                    Register.setRegAX(towrite);
                    Callback.scf(false);
                } else {
                    Register.setRegAX(DOS.ErrorCode);
                    Callback.scf(true);
                }
                modifyCycles(Register.getRegAX());
                break;
            }
            case 0x41: /* UNLINK Delete file */
                name1 = Memory.strCopy(Register.segPhys(Register.SEG_NAME_DS) + Register.getRegDX(),
                        DOSNAMEBUF);
                if (unlinkFile(name1)) {
                    Callback.scf(false);
                } else {
                    Register.setRegAX(DOS.ErrorCode);
                    Callback.scf(true);
                }
                break;
            case 0x42: /* LSEEK Set current file position */
            {
                long pos = (Register.getRegCX() << 16) + Register.getRegDX();
                if ((pos = seekFile(Register.Regs[Register.BX].getWord(), pos,
                        Register.getRegAL())) >= 0) {
                    Register.setRegDX((int) (pos >>> 16));
                    Register.setRegAX((int) (pos & 0xFFFF));
                    Callback.scf(false);
                } else {
                    Register.setRegAX(DOS.ErrorCode);
                    Callback.scf(true);
                }
                break;
            }
            case 0x43: /* Get/Set file attributes */
                name1 = Memory.strCopy(Register.segPhys(Register.SEG_NAME_DS) + Register.getRegDX(),
                        DOSNAMEBUF);
                switch (Register.getRegAL()) {
                    case 0x00: /* Get */
                    {
                        // TODO : DOSFile Attr 불러오는 함수에 인자로 대입할 필요없다. 굳이 attr 인자의 초기값으로 설정할 필요있나? 확인
                        // 필요
                        int attrVal = Register.getRegCX();
                        if (tryFileAttr(name1)) {
                            attrVal = returnFileAttr();
                            Register.setRegCX(attrVal);
                            Register.setRegAX(attrVal); /* Undocumented */
                            Callback.scf(false);
                        } else {
                            Callback.scf(true);
                            Register.setRegAX(DOS.ErrorCode);
                        }
                        break;
                    }
                    case 0x01: /* Set */
                        Log.logging(Log.LogTypes.MISC, Log.LogServerities.Error,
                                "DOS:Set File Attributes for %s not supported", name1);
                        if (setFileAttr(name1, Register.getRegCX())) {
                            Register.setRegAX(0x202); /* ax destroyed */
                            Callback.scf(false);
                        } else {
                            Callback.scf(true);
                            Register.setRegAX(DOS.ErrorCode);
                        }
                        break;
                    default:
                        Log.logging(Log.LogTypes.MISC, Log.LogServerities.Error,
                                "DOS:0x43:Illegal subfunction %2X", Register.getRegAL());
                        Register.setRegAX(1);
                        Callback.scf(true);
                        break;
                }
                break;
            case 0x44: /* IOCTL Functions */
                if (doIOCTL()) {
                    Callback.scf(false);
                } else {
                    Register.setRegAX(DOS.ErrorCode);
                    Callback.scf(true);
                }
                break;
            case 0x45: /* DUP Duplicate file handle */
                if (duplicateEntry(Register.getRegBX())) {
                    Register.Regs[Register.AX].setWord(duplicatedNewEntry);
                    Callback.scf(false);
                } else {
                    Register.setRegAX(DOS.ErrorCode);
                    Callback.scf(true);
                }
                break;
            case 0x46: /* DUP2,FORCEDUP Force duplicate file handle */
                if (forceDuplicateEntry(Register.getRegBX(), Register.getRegCX())) {
                    Register.setRegAX(Register.getRegCX()); // Not all sources agree on it.
                    Callback.scf(false);
                } else {
                    Register.setRegAX(DOS.ErrorCode);
                    Callback.scf(true);
                }
                break;
            case 0x47: /* CWD Get current directory */
                if (getCurrentDir(Register.getRegDL(), name1pt)) {
                    Memory.blockWrite(Register.segPhys(Register.SEG_NAME_DS) + Register.getRegSI(),
                            name1pt);
                    Register.setRegAX(0x0100);
                    Callback.scf(false);
                } else {
                    Register.setRegAX(DOS.ErrorCode);
                    Callback.scf(true);
                }
                break;
            case 0x48: /* Allocate memory */
            {
                if (tryAllocateMemory(Register.getRegBX())) {
                    Register.setRegAX(returnedAllocateMemorySeg);
                    Callback.scf(false);
                } else {
                    Register.setRegAX(DOS.ErrorCode);
                    Register.setRegBX(returnedAllocateMemoryBlock);
                    Callback.scf(true);
                }
                break;
            }
            case 0x49: /* Free memory */
                if (freeMemory(Register.segValue(Register.SEG_NAME_ES))) {
                    Callback.scf(false);
                } else {
                    Register.setRegAX(DOS.ErrorCode);
                    Callback.scf(true);
                }
                break;
            case 0x4a: /* Resize memory block */
            {
                if (tryResizeMemory(Register.segValue(Register.SEG_NAME_ES), Register.getRegBX())) {
                    Register.setRegAX(Register.segValue(Register.SEG_NAME_ES));
                    Callback.scf(false);
                } else {
                    Register.setRegAX(DOS.ErrorCode);
                    Register.setRegBX(returnedResizedMemoryBlocks);
                    Callback.scf(true);
                }
                break;
            }
            case 0x4b: /* EXEC Load and/or execute program */
            {
                name1 = Memory.strCopy(Register.segPhys(Register.SEG_NAME_DS) + Register.getRegDX(),
                        DOSNAMEBUF);
                DOSMain.dbgCurLoadedProgram = name1;
                Log.logging(Log.LogTypes.EXEC, Log.LogServerities.Error, "Execute %s %d", name1,
                        Register.getRegAL());
                if (!execute(name1, Register.segPhys(Register.SEG_NAME_ES) + Register.getRegBX(),
                        Register.getRegAL())) {
                    Register.setRegAX(DOS.ErrorCode);
                    Callback.scf(true);
                }
            }
                break;
            // TODO Check for use of execution state AL=5
            case 0x4c: /* EXIT Terminate with return code */
                terminate(DOS.getPSP(), false, Register.getRegAL());
                break;
            case 0x4d: /* Get Return code */
                Register.setRegAL(DOS.ReturnCode);/* Officially read from SDA and clear when read */
                Register.setRegAH(DOS.ReturnMode);
                break;
            case 0x4e: /* FINDFIRST Find first matching file */
                name1 = Memory.strCopy(Register.segPhys(Register.SEG_NAME_DS) + Register.getRegDX(),
                        DOSNAMEBUF);
                if (findFirst(name1, Register.getRegCX())) {
                    Callback.scf(false);
                    Register.setRegAX(0); /* Undocumented */
                } else {
                    Register.setRegAX(DOS.ErrorCode);
                    Callback.scf(true);
                }
                break;
            case 0x4f: /* FINDNEXT Find next matching file */
                if (findNext()) {
                    Callback.scf(false);
                    /* regsModule.reg_ax=0xffff; */
                    /* Undocumented */
                    Register.setRegAX(0); /* Undocumented:Qbix Willy beamish */
                } else {
                    Register.setRegAX(DOS.ErrorCode);
                    Callback.scf(true);
                }
                break;
            case 0x50: /* Set current PSP */
                DOS.setPSP(Register.getRegBX());
                break;
            case 0x51: /* Get current PSP */
                Register.setRegBX(DOS.getPSP());
                break;
            case 0x52: { /* Get list of lists */
                int addr = DOSInfoBlock.getPointer();
                Register.segSet16(Register.SEG_NAME_ES, Memory.realSeg(addr));
                Register.setRegBX(Memory.realOff(addr));
                Log.logging(Log.LogTypes.DOSMISC, Log.LogServerities.Normal,
                        "Call is made for list of lists - let's hope for the best");
                break;
            }
            // TODO Think hard how shit this is gonna be
            // And will any game ever use this :)
            case 0x53: /* Translate BIOS parameter block to drive parameter block */
                Support.exceptionExit("Unhandled Dos 21 call %02X", Register.getRegAH());
                break;
            case 0x54: /* Get verify flag */
                Register.setRegAL(DOS.Verify ? 1 : 0);
                break;
            case 0x55: /* Create Child PSP */
                childPSP(Register.getRegDX(), Register.getRegSI());
                DOS.setPSP(Register.getRegDX());
                break;
            case 0x56: /* RENAME Rename file */
                name1 = Memory.strCopy(Register.segPhys(Register.SEG_NAME_DS) + Register.getRegDX(),
                        DOSNAMEBUF);
                name2 = Memory.strCopy(Register.segPhys(Register.SEG_NAME_ES) + Register.getRegDI(),
                        DOSNAMEBUF);
                if (rename(name1, name2)) {
                    Callback.scf(false);
                } else {
                    Register.setRegAX(DOS.ErrorCode);
                    Callback.scf(true);
                }
                break;
            case 0x57: /* Get/Set File's Date and Time */
                if (Register.getRegAL() == 0x00) {
                    if (tryGetFileDate(Register.getRegBX())) {
                        Register.setRegCX(returnedGetFileOTime);
                        Register.setRegDX(returnedGetFileODate);

                        Callback.scf(false);
                    } else {
                        Callback.scf(true);
                    }
                } else if (Register.getRegAL() == 0x01) {
                    Log.logging(Log.LogTypes.DOSMISC, Log.LogServerities.Error,
                            "DOS:57:Set File Date Time Faked");
                    Callback.scf(false);
                } else {
                    Log.logging(Log.LogTypes.DOSMISC, Log.LogServerities.Error,
                            "DOS:57:Unsupported subtion %X", Register.getRegAL());
                }
                break;
            case 0x58: /* Get/Set Memory allocation strategy */
                switch (Register.getRegAL()) {
                    case 0: /* Get Strategy */
                        Register.setRegAX(getMemAllocStrategy());
                        break;
                    case 1: /* Set Strategy */
                        if (setMemAllocStrategy(Register.getRegBX()))
                            Callback.scf(false);
                        else {
                            Register.setRegAX(1);
                            Callback.scf(true);
                        }
                        break;
                    case 2: /* Get UMB Link Status */
                        Register.setRegAL(DOSInfoBlock.getUMBChainState() & 1);
                        Callback.scf(false);
                        break;
                    case 3: /* Set UMB Link Status */
                        if (linkUMBsToMemChain(Register.getRegBX()))
                            Callback.scf(false);
                        else {
                            Register.setRegAX(1);
                            Callback.scf(true);
                        }
                        break;
                    default:
                        Log.logging(Log.LogTypes.DOSMISC, Log.LogServerities.Error,
                                "DOS:58:Not Supported Set//Get memory allocation call %X",
                                Register.getRegAL());
                        Register.setRegAX(1);
                        Callback.scf(true);
                        break;
                }
                break;
            case 0x59: /* Get Extended error information */
                Register.setRegAX(DOS.ErrorCode);
                if (DOS.ErrorCode == DOSERR_FILE_NOT_FOUND
                        || DOS.ErrorCode == DOSERR_PATH_NOT_FOUND) {
                    Register.setRegBH(8); // Not Found error class (Road Hog)
                } else {
                    Register.setRegBH(0); // Unspecified error class
                }
                Register.setRegBL(1); // Retry retry retry
                Register.setRegCH(0); // Unkown error locus
                break;
            case 0x5a: /* Create temporary file */
            {
                int handle = 0;
                Memory.strCopy(Register.segPhys(Register.SEG_NAME_DS) + Register.getRegDX(),
                        name1pt, DOSNAMEBUF);
                if (createTempFile(name1pt)) {
                    handle = returnFileHandle;
                    Register.setRegAX(handle);
                    Memory.blockWrite(Register.segPhys(Register.SEG_NAME_DS) + Register.getRegDX(),
                            name1pt);
                    Callback.scf(false);
                } else {
                    Register.setRegAX(DOS.ErrorCode);
                    Callback.scf(true);
                }
            }
                break;
            case 0x5b: /* Create new file */
            {
                name1 = Memory.strCopy(Register.segPhys(Register.SEG_NAME_DS) + Register.getRegDX(),
                        DOSNAMEBUF);
                int handle = 0;
                if (openFile(name1, 0)) {
                    handle = returnFileHandle;
                    closeFile(handle);
                    setError(DOSERR_FILE_ALREADY_EXISTS);
                    Register.setRegAX(DOS.ErrorCode);
                    Callback.scf(true);
                    break;
                }
                if (createFile(name1, Register.getRegCX())) {
                    Register.setRegAX(returnFileHandle);
                    Callback.scf(false);
                } else {
                    Register.setRegAX(DOS.ErrorCode);
                    Callback.scf(true);
                }
                break;
            }
            case 0x5c: /* FLOCK File region locking */
                setError(DOSERR_FUNCTION_NUMBER_INVALID);
                Register.setRegAX(DOS.ErrorCode);
                Callback.scf(true);
                break;
            case 0x5d: /* Network Functions */
                if (Register.getRegAL() == 0x06) {
                    Register.segSet16(Register.SEG_NAME_DS, DOS_SDA_SEG);
                    Register.setRegSI(DOS_SDA_OFS);
                    Register.setRegCX(0x80); // swap if in dos
                    Register.setRegDX(0x1a); // swap always
                    Log.logging(Log.LogTypes.DOSMISC, Log.LogServerities.Error,
                            "Get SDA, Let's hope for the best!");
                }
                break;
            case 0x5f: /* Network redirection */
                Register.setRegAX(0x0001); // Failing it
                Callback.scf(true);
                break;
            case 0x60: /* Canonicalize filename or path */
            {
                name1 = Memory.strCopy(Register.segPhys(Register.SEG_NAME_DS) + Register.getRegSI(),
                        DOSNAMEBUF);
                CStringPt name2cstr =
                        CStringPt.create(DOSMain.DOSNAMEBUF + 2 + DOSSystem.DOS_NAMELENGTH_ASCII);
                if (canonicalize(name1, name2cstr)) {
                    Memory.blockWrite(Register.segPhys(Register.SEG_NAME_ES) + Register.getRegDI(),
                            name2cstr);
                    Callback.scf(false);
                } else {
                    Register.setRegAX(DOS.ErrorCode);
                    Callback.scf(true);
                }
            }
                break;
            case 0x62: /* Get Current PSP Address */
                Register.setRegBX(DOS.getPSP());
                break;
            case 0x63: /* DOUBLE BYTE CHARACTER SET */
                if (Register.getRegAL() == 0) {
                    Register.segSet16(Register.SEG_NAME_DS, Memory.realSeg(DOS.tables.DBCS));
                    Register.setRegSI(Memory.realOff(DOS.tables.DBCS));
                    Register.setRegAL(0);
                    Callback.scf(false); // undocumented
                } else
                    Register.setRegAL(0xff); // Doesn't officially touch carry flag
                break;
            case 0x64: /* Set device driver lookahead flag */
                Log.logging(Log.LogTypes.DOSMISC, Log.LogServerities.Normal,
                        "set driver look ahead flag");
                break;
            case 0x65: /* Get extented country information and a lot of other useless shit */
            { /* Todo maybe fully support this for now we set it standard for USA */
                Log.logging(Log.LogTypes.DOSMISC, Log.LogServerities.Error,
                        "DOS:65:Extended country information call %X", Register.getRegAX());
                if ((Register.getRegAL() <= 0x07) && (Register.getRegCX() < 0x05)) {
                    setError(DOSERR_FUNCTION_NUMBER_INVALID);
                    Callback.scf(true);
                    break;
                }
                int len = 0; /* For 0x21 and 0x22 */
                int data = Register.segPhys(Register.SEG_NAME_ES) + Register.getRegDI();
                switch (Register.getRegAL()) {
                    case 0x01:
                        Memory.writeB(data + 0x00, Register.getRegAL());
                        Memory.writeW(data + 0x01, 0x26);
                        Memory.writeW(data + 0x03, 1);
                        if (Register.getRegCX() > 0x06)
                            Memory.writeW(data + 0x05, DOS.LoadedCodepage);
                        if (Register.getRegCX() > 0x08) {
                            int amount =
                                    (Register.getRegCX() >= 0x29) ? 0x22 : Register.getRegCX() - 7;
                            Memory.blockWrite(data + 0x07, DOS.tables.Country, 0, amount);
                            Register.setRegCX(
                                    (Register.getRegCX() >= 0x29) ? 0x29 : Register.getRegCX());
                        }
                        Callback.scf(false);
                        break;
                    case 0x05: // Get pointer to filename terminator table
                        Memory.writeB(data + 0x00, Register.getRegAL());
                        Memory.writeD(data + 0x01, DOS.tables.FileNameChar);
                        Register.setRegCX(5);
                        Callback.scf(false);
                        break;
                    case 0x02: // Get pointer to uppercase table
                        Memory.writeB(data + 0x00, Register.getRegAL());
                        Memory.writeD(data + 0x01, DOS.tables.Upcase);
                        Register.setRegCX(5);
                        Callback.scf(false);
                        break;
                    case 0x06: // Get pointer to collating sequence table
                        Memory.writeB(data + 0x00, Register.getRegAL());
                        Memory.writeD(data + 0x01, DOS.tables.CollatingSeq);
                        Register.setRegCX(5);
                        Callback.scf(false);
                        break;
                    case 0x03: // Get pointer to lowercase table
                    case 0x04: // Get pointer to filename uppercase table
                    case 0x07: // Get pointer to double byte char set table
                        Memory.writeB(data + 0x00, Register.getRegAL());
                        Memory.writeD(data + 0x01, DOS.tables.DBCS); // used to be 0
                        Register.setRegCX(5);
                        Callback.scf(false);
                        break;
                    case 0x20: /* Capitalize Character */
                    {
                        int outval = CStringHelper.toupper((byte) Register.getRegDL());
                        Register.setRegDL(outval);
                    }
                        Callback.scf(false);
                        break;
                    case 0x21: /* Capitalize String (cx=length) */
                    case 0x22: /* Capatilize ASCIZ string */
                        data = Register.segPhys(Register.SEG_NAME_DS) + Register.getRegDX();
                        if (Register.getRegAL() == 0x21)
                            len = Register.getRegCX();
                        else
                            len = Memory.memStrLen(data); /* Is limited to 1024 */

                        if (len > DOS_COPYBUFSIZE - 1)
                            Support.exceptionExit("DOS:0x65 Buffer overflow");
                        if (len != 0) {
                            Memory.blockRead(data, dosCopyBuf, 0, len);
                            dosCopyBuf[len] = 0;
                            // No upcase as String(0x21) might be multiple asciz strings
                            for (int count = 0; count < len; count++)
                                dosCopyBuf[count] = (byte) CStringHelper.toupper(dosCopyBuf[count]);
                            Memory.blockWrite(data, dosCopyBuf, 0, len);
                        }
                        Callback.scf(false);
                        break;
                    default:
                        Support.exceptionExit("DOS:0x65:Unhandled country information call %2X",
                                Register.getRegAL());
                        break;
                }
                break;
            }
            case 0x66: /* Get/Set global code page table */
                if (Register.getRegAL() == 1) {
                    Log.logging(Log.LogTypes.DOSMISC, Log.LogServerities.Error,
                            "Getting global code page table");
                    Register.setRegDX(DOS.LoadedCodepage);
                    Register.setRegBX(DOS.LoadedCodepage);
                    Callback.scf(false);
                    break;
                }
                Log.logging(Log.LogTypes.DOSMISC, Log.LogServerities.Normal,
                        "DOS:Setting code page table is not supported");
                break;
            case 0x67: /* Set handle count */
            /* Weird call to increase amount of file handles needs to allocate memory if >20 */
            {
                DOSPSP psp = new DOSPSP(DOS.getPSP());
                psp.setNumFiles(Register.getRegBX());
                Callback.scf(false);
                break;
            }
            case 0x68: /* FFLUSH Commit file */
                if (flushFile(Register.getRegBL())) {
                    Callback.scf(false);
                } else {
                    Register.setRegAX(DOS.ErrorCode);
                    Callback.scf(true);
                }
                break;
            case 0x69: /* Get/Set disk serial number */
            {
                switch (Register.getRegAL()) {
                    case 0x00: /* Get */
                        Log.logging(Log.LogTypes.DOSMISC, Log.LogServerities.Error,
                                "DOS:Get Disk serial number");
                        Callback.scf(true);
                        break;
                    case 0x01:
                        Log.logging(Log.LogTypes.DOSMISC, Log.LogServerities.Error,
                                "DOS:Set Disk serial number");
                        // goto Goto0x69default;
                        Support.exceptionExit("DOS:Illegal Get Serial Number call %2X",
                                Register.getRegAL());
                        break;
                    default:
                        // Goto0x69default:
                        Support.exceptionExit("DOS:Illegal Get Serial Number call %2X",
                                Register.getRegAL());
                        break;
                }
                break;
            }
            case 0x6c: /* Extended Open/Create */
            {
                name1 = Memory.strCopy(Register.segPhys(Register.SEG_NAME_DS) + Register.getRegSI(),
                        DOSNAMEBUF);
                if (openFileExtended(name1, Register.getRegBX(), Register.getRegCX(),
                        Register.getRegDX())) {
                    Register.Regs[Register.AX].setWord(returnedFileExtendedHandle);
                    Register.Regs[Register.CX].setWord(returnedFileExtendedStatus);
                    Callback.scf(false);
                } else {
                    Register.setRegAX(DOS.ErrorCode);
                    Callback.scf(true);
                }
                break;
            }
            case 0x71: /* Unknown probably 4dos detection */
                Register.setRegAX(0x7100);
                Callback.scf(true);
                Log.logging(Log.LogTypes.DOSMISC, Log.LogServerities.Normal,
                        "DOS:Windows long file name support call %2X", Register.getRegAL());
                break;

            case 0xE0:
            case 0x18: /* NULL Function for CP/M compatibility or Extended rename FCB */
            case 0x1d: /* NULL Function for CP/M compatibility or Extended rename FCB */
            case 0x1e: /* NULL Function for CP/M compatibility or Extended rename FCB */
            case 0x20: /* NULL Function for CP/M compatibility or Extended rename FCB */
            case 0x6b: /* NULL Function */
            case 0x61: /* UNUSED */
            case 0xEF: /* Used in Ancient Art Of War CGA */
            case 0x5e: /* More Network Functions */
            default:
                Log.logging(Log.LogTypes.DOSMISC, Log.LogServerities.Error,
                        "DOS:Unhandled call %02X al=%02X. Set al to default of 0",
                        Register.getRegAH(), Register.getRegAL());
                Register.setRegAL(0x00); /* default value */
                break;
        }
        return Callback.ReturnTypeNone;
    }


    public static int INT20Handler() {
        Register.setRegAH(0x00);
        INT21Handler();
        return Callback.ReturnTypeNone;
    }

    public static int INT27Handler() {
        int blocks = (Register.getRegDX() / 16) + Convert.toShort((Register.getRegDX() % 16) > 0);
        // Terminate & stay resident
        int psp = DOS.getPSP(); // MemModule.mem_readw(SegPhys(ss)+regsModule.reg_sp+2);
        if (tryResizeMemory(psp, blocks))
            terminate(psp, true, 0);
        return Callback.ReturnTypeNone;
    }

    public static int INT25Handler() {
        if (Drives[Register.getRegAL()] == null) {
            Register.setRegAX(0x8002);
            Register.setFlagBit(Register.FlagCF, true);
        } else {
            Register.setFlagBit(Register.FlagCF, false);
            if ((Register.getRegCX() != 1) || (Register.getRegDX() != 1))
                Log.logging(Log.LogTypes.DOSMISC, Log.LogServerities.Normal,
                        "int 25 called but not as diskdetection drive %X", Register.getRegAL());

            Register.setRegAX(0);
        }
        return Callback.ReturnTypeNone;
    }

    public static int INT26Handler() {
        Log.logging(Log.LogTypes.DOSMISC, Log.LogServerities.Normal,
                "int 26 called: hope for the best!");
        if (Drives[Register.getRegAL()] == null) {
            Register.setRegAX(0x8002);
            Register.setFlagBit(Register.FlagCF, true);
        } else {
            Register.setFlagBit(Register.FlagCF, false);
            Register.setRegAX(0);
        }
        return Callback.ReturnTypeNone;
    }

    private static void shutdown(Section sec) {
        _dos.dispose();
        _dos = null;
    }

    private static DOSModule _dos;

    public static void init(Section sec) {
        _dos = new DOSModule(sec);
        /* shutdown function */
        sec.addDestroyFunction(DOSMain::shutdown, false);
    }

    /*--------------------------- begin DOSTables -----------------------------*/
    // -- #region dos_tables
    // RealPt DOS_TableUpCase;
    // RealPt DOS_TableLowCase;

    private static int _callCaseMap;

    private static int _dosMemSeg = DOS_PRIVATE_SEGMENT;

    // uint16 (uint 16)
    public static int getMemory(int pages) {
        if (pages + _dosMemSeg >= DOS_PRIVATE_SEGMENT_END) {
            Support.exceptionExit("DOS:Not enough memory for internal tables");
        }
        int page = _dosMemSeg;
        _dosMemSeg += pages;
        return page;
    }

    private static int caseMapFunc() {
        // LOG(LOG_DOSMISC,LOG_ERROR)("Case map routine called : %c",reg_al);
        return Callback.ReturnTypeNone;
    }

    private static byte[] countryInfo = {/* Date format */ 0x00, 0x00, /* Currencystring */ 0x24,
            0x00, 0x00, 0x00, 0x00, /* Thousands sep */ 0x2c, 0x00, /* Decimal sep */ 0x2e, 0x00,
            /* Date sep */ 0x2d, 0x00, /* time sep */ 0x3a, 0x00, /* currency form */ 0x00,
            /* digits after dec */ 0x02, /* Time format */ 0x00, /* Casemap */ 0x00, 0x00, 0x00,
            0x00, /* Data sep */ 0x2c, 0x00, /* Reservered 5 */ 0x00, 0x00, 0x00, 0x00, 0x00,
            /* Reservered 5 */ 0x00, 0x00, 0x00, 0x00, 0x00};

    public static void setupTables() {
        int seg;
        int i;
        DOS.tables.MediaId = Memory.realMake(getMemory(4), 0);
        DOS.tables.TempDTA = Memory.realMake(getMemory(4), 0);
        DOS.tables.TempDTA_FCBDelete = Memory.realMake(getMemory(4), 0);
        for (i = 0; i < DOS_DRIVES; i++)
            Memory.writeW(Memory.real2Phys(DOS.tables.MediaId) + i * 2, 0);
        /* Create the DOS Info Block */
        DOSInfoBlock.setLocation(DOS_INFOBLOCK_SEG); // c2woody

        /* create SDA */
        (new DOSSDA(DOS_SDA_SEG, 0)).init();

        /* Some weird files >20 detection routine */
        /* Possibly obselete when SFT is properly handled */
        Memory.realWriteD(DOS_CONSTRING_SEG, 0x0a, 0x204e4f43);
        Memory.realWriteD(DOS_CONSTRING_SEG, 0x1a, 0x204e4f43);
        Memory.realWriteD(DOS_CONSTRING_SEG, 0x2a, 0x204e4f43);

        /* create a CON device driver */
        seg = DOS_CONDRV_SEG;
        Memory.realWriteD(seg, 0x00, 0xffffffff); // next ptr
        Memory.realWriteW(seg, 0x04, 0x8013); // attributes
        Memory.realWriteD(seg, 0x06, 0xffffffff); // strategy routine
        Memory.realWriteD(seg, 0x0a, 0x204e4f43); // driver name
        Memory.realWriteD(seg, 0x0e, 0x20202020); // driver name
        DOSInfoBlock.setDeviceChainStart(Memory.realMake(seg, 0));

        /* Create a fake Current Directory Structure */
        seg = DOS_CDS_SEG;
        Memory.realWriteD(seg, 0x00, 0x005c3a43);
        DOSInfoBlock.setCurDirStruct(Memory.realMake(seg, 0));



        /* Allocate DCBS DOUBLE BYTE CHARACTER SET LEAD-BYTE TABLE */
        DOS.tables.DBCS = Memory.realMake(getMemory(12), 0);
        Memory.writeD(Memory.real2Phys(DOS.tables.DBCS), 0); // empty table
        /* FILENAME CHARACTER TABLE */
        DOS.tables.FileNameChar = Memory.realMake(getMemory(2), 0);
        Memory.writeW(Memory.real2Phys(DOS.tables.FileNameChar) + 0x00, 0x16);
        Memory.writeW(Memory.real2Phys(DOS.tables.FileNameChar) + 0x02, 0x01);
        Memory.writeW(Memory.real2Phys(DOS.tables.FileNameChar) + 0x03, 0x00); // allowed
                                                                               // chars from
        Memory.writeW(Memory.real2Phys(DOS.tables.FileNameChar) + 0x04, 0xff); // ...to
        Memory.writeW(Memory.real2Phys(DOS.tables.FileNameChar) + 0x05, 0x00);
        Memory.writeW(Memory.real2Phys(DOS.tables.FileNameChar) + 0x06, 0x00); // excluded
                                                                               // chars from
        Memory.writeW(Memory.real2Phys(DOS.tables.FileNameChar) + 0x07, 0x20); // ...to
        Memory.writeW(Memory.real2Phys(DOS.tables.FileNameChar) + 0x08, 0x02);
        Memory.writeW(Memory.real2Phys(DOS.tables.FileNameChar) + 0x09, 0x0e); // number of
                                                                               // illegal
                                                                               // separators
        Memory.writeW(Memory.real2Phys(DOS.tables.FileNameChar) + 0x0a, 0x2e);
        Memory.writeW(Memory.real2Phys(DOS.tables.FileNameChar) + 0x0b, 0x22);
        Memory.writeW(Memory.real2Phys(DOS.tables.FileNameChar) + 0x0c, 0x2f);
        Memory.writeW(Memory.real2Phys(DOS.tables.FileNameChar) + 0x0d, 0x5c);
        Memory.writeW(Memory.real2Phys(DOS.tables.FileNameChar) + 0x0e, 0x5b);
        Memory.writeW(Memory.real2Phys(DOS.tables.FileNameChar) + 0x0f, 0x5d);
        Memory.writeW(Memory.real2Phys(DOS.tables.FileNameChar) + 0x10, 0x3a);
        Memory.writeW(Memory.real2Phys(DOS.tables.FileNameChar) + 0x11, 0x7c);
        Memory.writeW(Memory.real2Phys(DOS.tables.FileNameChar) + 0x12, 0x3c);
        Memory.writeW(Memory.real2Phys(DOS.tables.FileNameChar) + 0x13, 0x3e);
        Memory.writeW(Memory.real2Phys(DOS.tables.FileNameChar) + 0x14, 0x2b);
        Memory.writeW(Memory.real2Phys(DOS.tables.FileNameChar) + 0x15, 0x3d);
        Memory.writeW(Memory.real2Phys(DOS.tables.FileNameChar) + 0x16, 0x3b);
        Memory.writeW(Memory.real2Phys(DOS.tables.FileNameChar) + 0x17, 0x2c);
        /* COLLATING SEQUENCE TABLE + UPCASE TABLE */
        // 256 bytes for col table, 128 for upcase, 4 for number of entries
        DOS.tables.CollatingSeq = Memory.realMake(getMemory(25), 0);
        Memory.writeW(Memory.real2Phys(DOS.tables.CollatingSeq), 0x100);
        for (i = 0; i < 256; i++)
            Memory.writeW(Memory.real2Phys(DOS.tables.CollatingSeq) + i + 2, i);
        DOS.tables.Upcase = DOS.tables.CollatingSeq + 258;
        Memory.writeW(Memory.real2Phys(DOS.tables.Upcase), 0x80);
        for (i = 0; i < 128; i++)
            Memory.writeW(Memory.real2Phys(DOS.tables.Upcase) + i + 2, 0x80 + i);


        /* Create a fake FCB SFT */
        seg = getMemory(4);
        Memory.realWriteD(seg, 0, 0xffffffff); // Last File Table
        Memory.realWriteW(seg, 4, 100); // File Table supports 100 files
        DOSInfoBlock.setFCBTable(Memory.realMake(seg, 0));

        /* Create a fake DPB */
        DOS.tables.DPB = getMemory(2);
        for (short d = 0; d < 26; d++)
            Memory.realWriteB(DOS.tables.DPB, d, d);

        /* Create a fake disk buffer head */
        seg = getMemory(6);
        for (short ct = 0; ct < 0x20; ct++)
            Memory.realWriteB(seg, ct, 0);
        Memory.realWriteW(seg, 0x00, 0xffff); // forward ptr
        Memory.realWriteW(seg, 0x02, 0xffff); // backward ptr
        Memory.realWriteB(seg, 0x04, 0xff); // not in use
        Memory.realWriteB(seg, 0x0a, 0x01); // number of FATs
        Memory.realWriteD(seg, 0x0d, 0xffffffff); // pointer to DPB
        DOSInfoBlock.setDiskBufferHeadPt(Memory.realMake(seg, 0));

        /* Set buffers to a nice value */
        DOSInfoBlock.setBuffers(50, 50);

        /* case map routine INT 0x21 0x38 */
        _callCaseMap = Callback.allocate();
        Callback.setup(_callCaseMap, DOSMain::caseMapFunc, Callback.Symbol.RETF, "DOS CaseMap\0");
        /* Add it to country structure */
        Memory.hostWriteD(countryInfo, 0x12, Callback.realPointer(_callCaseMap));
        DOS.tables.Country = countryInfo;
    }

    // -- #endregion
    /*--------------------------- end DOSTables -----------------------------*/

    /*--------------------------- begin DOSPrograms -----------------------------*/

    public static void setupPrograms() {
        /* Add Messages */

        Message.addMsg("PROGRAM_MOUNT_CDROMS_FOUND", "CDROMs found: {0:D}\n");
        Message.addMsg("PROGRAM_MOUNT_STATUS_2", "Drive {0} is mounted as {1}\n");
        Message.addMsg("PROGRAM_MOUNT_STATUS_1", "Current mounted drives are:\n");
        Message.addMsg("PROGRAM_MOUNT_ERROR_1", "Directory {0} doesn't exist.\n");
        Message.addMsg("PROGRAM_MOUNT_ERROR_2", "{0} isn't a directory\n");
        Message.addMsg("PROGRAM_MOUNT_ILL_TYPE", "Illegal type {0}\n");
        Message.addMsg("PROGRAM_MOUNT_ALREADY_MOUNTED", "Drive {0} already mounted with {1}\n");
        Message.addMsg("PROGRAM_MOUNT_USAGE",
                "Usage \u001B[34;1mMOUNT Drive-Letter Local-Directory\u001B[0m\n"
                        + "For example: MOUNT c {0}\n"
                        + "This makes the directory {1} act as the C: drive inside DOSBox.\n"
                        + "The directory has to exist.\n");
        Message.addMsg("PROGRAM_MOUNT_UMOUNT_NOT_MOUNTED", "Drive {0} isn't mounted.\n");
        Message.addMsg("PROGRAM_MOUNT_UMOUNT_SUCCESS",
                "Drive {0} has successfully been removed.\n");
        Message.addMsg("PROGRAM_MOUNT_UMOUNT_NO_VIRTUAL", "Virtual Drives can not be unMOUNTed.\n");
        Message.addMsg("PROGRAM_MOUNT_WARNING_WIN",
                "\u001B[31;1mMounting c:\\ is NOT recommended. Please mount a (sub)directory next time.\u001B[0m\n");
        Message.addMsg("PROGRAM_MOUNT_WARNING_OTHER",
                "\u001B[31;1mMounting / is NOT recommended. Please mount a (sub)directory next time.\u001B[0m\n");

        Message.addMsg("PROGRAM_MEM_CONVEN", "%10d Kb free conventional memory\n");
        Message.addMsg("PROGRAM_MEM_EXTEND", "%10d Kb free extended memory\n");
        Message.addMsg("PROGRAM_MEM_EXPAND", "%10d Kb free expanded memory\n");
        Message.addMsg("PROGRAM_MEM_UPPER",
                "%10d Kb free upper memory in %d blocks (largest UMB %d Kb)\n");

        Message.addMsg("PROGRAM_LOADFIX_ALLOC", "{0} kb allocated.\n");
        Message.addMsg("PROGRAM_LOADFIX_DEALLOC", "{0} kb freed.\n");
        Message.addMsg("PROGRAM_LOADFIX_DEALLOCALL", "Used memory freed.\n");
        Message.addMsg("PROGRAM_LOADFIX_ERROR", "Memory allocation error.\n");

        Message.addMsg("MSCDEX_SUCCESS", "MSCDEX installed.\n");
        Message.addMsg("MSCDEX_ERROR_MULTIPLE_CDROMS",
                "MSCDEX: Failure: Drive-letters of multiple CDRom-drives have to be continuous.\n");
        Message.addMsg("MSCDEX_ERROR_NOT_SUPPORTED", "MSCDEX: Failure: Not yet supported.\n");
        Message.addMsg("MSCDEX_ERROR_OPEN", "MSCDEX: Failure: Invalid file or unable to open.\n");
        Message.addMsg("MSCDEX_TOO_MANY_DRIVES",
                "MSCDEX: Failure: Too many CDRom-drives (max: 5). MSCDEX Installation failed.\n");
        Message.addMsg("MSCDEX_LIMITED_SUPPORT",
                "MSCDEX: Mounted subdirectory: limited support.\n");
        Message.addMsg("MSCDEX_INVALID_FILEFORMAT",
                "MSCDEX: Failure: File is either no iso/cue image or contains errors.\n");
        Message.addMsg("MSCDEX_UNKNOWN_ERROR", "MSCDEX: Failure: Unknown error.\n");

        Message.addMsg("PROGRAM_RESCAN_SUCCESS", "Drive cache cleared.\n");

        Message.addMsg("PROGRAM_INTRO",
                "\u001B[2J\u001B[32;1mWelcome to DOSBox\u001B[0m, an x86 emulator with sound and graphics.\n"
                        + "DOSBox creates a shell for you which looks like old plain DOS.\n" + "\n"
                        + "For information about basic mount type \u001B[34;1mintro mount\u001B[0m\n"
                        + "For information about CD-ROM support type \u001B[34;1mintro cdrom\u001B[0m\n"
                        + "For information about special keys type \u001B[34;1mintro special\u001B[0m\n"
                        + "For more information about DOSBox, go to \u001B[34;1mhttp://www.dosbox.com/wiki\u001B[0m\n"
                        + "\n"
                        + "\u001B[31;1mDOSBox will stop/exit without a warning if an error occured!\u001B[0m\n"
                        + "\n" + "\n");
        Message.addMsg("PROGRAM_INTRO_OTHER",
                "\u001B[2J\u001B[32;1mWelcome to DOSBox.j\u001B[0m, an x86 emulator with graphics.\n"
                        + "For more information about DOSBox, go to \u001B[34;1mhttp://www.dosbox.com/wiki\u001B[0m\n"
                        + "\n"
                        + "\u001B[31;1mDOSBox will stop/exit without a warning if an error occured!\u001B[0m\n"
                        + "\n" + "\n");
        Message.addMsg("PROGRAM_INTRO_MOUNT_START",
                "\u001B[32;1mHere are some commands to get you started:\u001B[0m\n"
                        + "Before you can use the files located on your own filesystem,\n"
                        + "You have to mount the directory containing the files.\n" + "\n");
        Message.addMsg("PROGRAM_INTRO_MOUNT_WINDOWS",
                "\u001B[44;1m\u00C9\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD"
                        + "\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD"
                        + "\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00BB\n"
                        + "\u00BA \u001B[32mmount c c:\\dosprogs\\\u001B[37m will create a C drive with c:\\dosprogs as contents.\u00BA\n"
                        + "\u00BA                                                                         \u00BA\n"
                        + "\u00BA \u001B[32mc:\\dosprogs\\\u001B[37m is an example. Replace it with your own games directory.  \u001B[37m \u00BA\n"
                        + "\u00C8\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD"
                        + "\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD"
                        + "\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00BC\u001B[0m\n");
        Message.addMsg("PROGRAM_INTRO_MOUNT_OTHER",
                "\u001B[44;1m\u00C9\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD"
                        + "\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD"
                        + "\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00BB\n"
                        + "\u00BA \u001B[32mmount c ~/dosprogs\u001B[37m will create a C drive with ~/dosprogs as contents.\u00BA\n"
                        + "\u00BA                                                                      \u00BA\n"
                        + "\u00BA \u001B[32m~/dosprogs\u001B[37m is an example. Replace it with your own games directory.\u001B[37m  \u00BA\n"
                        + "\u00C8\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD"
                        + "\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD"
                        + "\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00BC\u001B[0m\n");
        Message.addMsg("PROGRAM_INTRO_MOUNT_END",
                "When the mount has successfully completed you can type \u001B[34;1mc:\u001B[0m to go to your freshly\n"
                        + "mounted C-drive. Typing \u001B[34;1mdir\u001B[0m there will show its contents."
                        + " \u001B[34;1mcd\u001B[0m will allow you to\n"
                        + "enter a directory (recognised by the \u001B[33;1m[]\u001B[0m in a directory listing).\n"
                        + "You can run programs/files which end with \u001B[31m.exe .bat\u001B[0m and \u001B[31m.com\u001B[0m.\n");
        Message.addMsg("PROGRAM_INTRO_CDROM",
                "\u001B[2J\u001B[32;1mHow to mount a Real/Virtual CD-ROM Drive in DOSBox:\u001B[0m\n"
                        + "DOSBox provides CD-ROM emulation on several levels.\n" + "\n"
                        + "The \u001B[33mbasic\u001B[0m level works on all CD-ROM drives and normal directories.\n"
                        + "It installs MSCDEX and marks the files read-only.\n"
                        + "Usually this is enough for most games:\n"
                        + "\u001B[34;1mmount d \u001B[0;31mD:\\\u001B[34;1m -t cdrom\u001B[0m   or   \u001B[34;1mmount d C:\\example -t cdrom\u001B[0m\n"
                        + "If it doesn't work you might have to tell DOSBox the label of the CD-ROM:\n"
                        + "\u001B[34;1mmount d C:\\example -t cdrom -label CDLABEL\u001B[0m\n"
                        + "\n" + "The \u001B[33mnext\u001B[0m level adds some low-level support.\n"
                        + "Therefore only works on CD-ROM drives:\n"
                        + "\u001B[34;1mmount d \u001B[0;31mD:\\\u001B[34;1m -t cdrom -usecd \u001B[33m0\u001B[0m\n"
                        + "\n"
                        + "The \u001B[33mlast\u001B[0m level of support depends on your Operating System:\n"
                        + "For \u001B[1mWindows 2000\u001B[0m, \u001B[1mWindows XP\u001B[0m and \u001B[1mLinux\u001B[0m:\n"
                        + "\u001B[34;1mmount d \u001B[0;31mD:\\\u001B[34;1m -t cdrom -usecd \u001B[33m0 \u001B[34m-ioctl\u001B[0m\n"
                        + "For \u001B[1mWindows 9x\u001B[0m with a ASPI layer installed:\n"
                        + "\u001B[34;1mmount d \u001B[0;31mD:\\\u001B[34;1m -t cdrom -usecd \u001B[33m0 \u001B[34m-aspi\u001B[0m\n"
                        + "\n"
                        + "Replace \u001B[0;31mD:\\\u001B[0m with the location of your CD-ROM.\n"
                        + "Replace the \u001B[33;1m0\u001B[0m in \u001B[34;1m-usecd \u001B[33m0\u001B[0m with the number reported for your CD-ROM if you type:\n"
                        + "\u001B[34;1mmount -cd\u001B[0m\n");
        Message.addMsg("PROGRAM_INTRO_SPECIAL", "\u001B[2J\u001B[32;1mSpecial keys:\u001B[0m\n"
                + "These are the default keybindings.\n"
                + "They can be changed in the \u001B[33mkeymapper\u001B[0m.\n" + "\n"
                + "\u001B[33;1mALT-ENTER\u001B[0m   : Go full screen and back.\n"
                + "\u001B[33;1mALT-PAUSE\u001B[0m   : Pause DOSBox.\n"
                + "\u001B[33;1mCTRL-F1\u001B[0m     : Start the \u001B[33mkeymapper\u001B[0m.\n"
                + "\u001B[33;1mCTRL-F4\u001B[0m     : Update directory cache for all drives! Swap mounted disk-image.\n"
                + "\u001B[33;1mCTRL-ALT-F5\u001B[0m : Start/Stop creating a movie of the screen.\n"
                + "\u001B[33;1mCTRL-F5\u001B[0m     : Save a screenshot.\n"
                + "\u001B[33;1mCTRL-F6\u001B[0m     : Start/Stop recording sound output to a wave file.\n"
                + "\u001B[33;1mCTRL-ALT-F7\u001B[0m : Start/Stop recording of OPL commands.\n"
                + "\u001B[33;1mCTRL-ALT-F8\u001B[0m : Start/Stop the recording of raw MIDI commands.\n"
                + "\u001B[33;1mCTRL-F7\u001B[0m     : Decrease frameskip.\n"
                + "\u001B[33;1mCTRL-F8\u001B[0m     : Increase frameskip.\n"
                + "\u001B[33;1mCTRL-F9\u001B[0m     : Kill DOSBox.\n"
                + "\u001B[33;1mCTRL-F10\u001B[0m    : Capture/Release the mouse.\n"
                + "\u001B[33;1mCTRL-F11\u001B[0m    : Slow down emulation (Decrease DOSBox Cycles).\n"
                + "\u001B[33;1mCTRL-F12\u001B[0m    : Speed up emulation (Increase DOSBox Cycles).\n"
                + "\u001B[33;1mALT-F12\u001B[0m     : Unlock speed (turbo button/fast forward).\n");
        Message.addMsg("PROGRAM_BOOT_NOT_EXIST", "Bootdisk file does not exist.  Failing.\n");
        Message.addMsg("PROGRAM_BOOT_NOT_OPEN", "Cannot open bootdisk file.  Failing.\n");
        Message.addMsg("PROGRAM_BOOT_WRITE_PROTECTED",
                "Image file is read-only! Might create problems.\n");
        Message.addMsg("PROGRAM_BOOT_PRINT_ERROR",
                "This command boots DOSBox from either a floppy or hard disk image.\n\n"
                        + "For this command, one can specify a succession of floppy disks swappable\n"
                        + "by pressing Ctrl-F4, and -l specifies the mounted drive to boot from.  If\n"
                        + "no drive letter is specified, this defaults to booting from the A drive.\n"
                        + "The only bootable drive letters are A, C, and D.  For booting from a hard\n"
                        + "drive (C or D), the image should have already been mounted using the\n"
                        + "\u001B[34;1mIMGMOUNT\u001B[0m command.\n\n"
                        + "The syntax of this command is:\n\n"
                        + "\u001B[34;1mBOOT [diskimg1.img diskimg2.img] [-l driveletter]\u001B[0m\n");
        Message.addMsg("PROGRAM_BOOT_UNABLE", "Unable to boot off of drive {0}");
        Message.addMsg("PROGRAM_BOOT_IMAGE_OPEN", "Opening image file: {0}\n");
        Message.addMsg("PROGRAM_BOOT_IMAGE_NOT_OPEN", "Cannot open {0}");
        Message.addMsg("PROGRAM_BOOT_BOOT", "Booting from drive {0}...\n");
        Message.addMsg("PROGRAM_BOOT_CART_WO_PCJR",
                "PCjr cartridge found, but machine is not PCjr");
        Message.addMsg("PROGRAM_BOOT_CART_LIST_CMDS", "Available PCjr cartridge commandos:{0}");
        Message.addMsg("PROGRAM_BOOT_CART_NO_CMDS", "No PCjr cartridge commandos found");

        Message.addMsg("PROGRAM_IMGMOUNT_SPECIFY_DRIVE",
                "Must specify drive letter to mount image at.\n");
        Message.addMsg("PROGRAM_IMGMOUNT_SPECIFY2",
                "Must specify drive number (0 or 3) to mount image at (0,1=fda,fdb;2,3=hda,hdb).\n");
        Message.addMsg("PROGRAM_IMGMOUNT_SPECIFY_GEOMETRY",
                "For \u001B[33mCD-ROM\u001B[0m images:   \u001B[34;1mIMGMOUNT drive-letter location-of-image -t iso\u001B[0m\n"
                        + "\n"
                        + "For \u001B[33mhardrive\u001B[0m images: Must specify drive geometry for hard drives:\n"
                        + "bytes_per_sector, sectors_per_cylinder, heads_per_cylinder, cylinder_count.\n"
                        + "\u001B[34;1mIMGMOUNT drive-letter location-of-image -size bps,spc,hpc,cyl\u001B[0m\n");
        Message.addMsg("PROGRAM_IMGMOUNT_INVALID_IMAGE", "Could not load image file.\n"
                + "Check that the path is correct and the image is accessible.\n");
        Message.addMsg("PROGRAM_IMGMOUNT_INVALID_GEOMETRY",
                "Could not extract drive geometry from image.\n"
                        + "Use parameter -size bps,spc,hpc,cyl to specify the geometry.\n");
        Message.addMsg("PROGRAM_IMGMOUNT_TYPE_UNSUPPORTED",
                "Type \"{0}\" is unsupported. Specify \"hdd\" or \"floppy\" or\"iso\".\n");
        Message.addMsg("PROGRAM_IMGMOUNT_FORMAT_UNSUPPORTED",
                "Format \"{0}\" is unsupported. Specify \"fat\" or \"iso\" or \"none\".\n");
        Message.addMsg("PROGRAM_IMGMOUNT_SPECIFY_FILE", "Must specify file-image to mount.\n");
        Message.addMsg("PROGRAM_IMGMOUNT_FILE_NOT_FOUND", "Image file not found.\n");
        Message.addMsg("PROGRAM_IMGMOUNT_MOUNT",
                "To mount directories, use the \u001B[34;1mMOUNT\u001B[0m command, not the \u001B[34;1mIMGMOUNT\u001B[0m command.\n");
        Message.addMsg("PROGRAM_IMGMOUNT_ALREADY_MOUNTED",
                "Drive already mounted at that letter.\n");
        Message.addMsg("PROGRAM_IMGMOUNT_CANT_CREATE", "Can't create drive from file.\n");
        Message.addMsg("PROGRAM_IMGMOUNT_MOUNT_NUMBER", "Drive number {0:D} mounted as {1}\n");
        Message.addMsg("PROGRAM_IMGMOUNT_NON_LOCAL_DRIVE",
                "The image must be on a host or local drive.\n");
        Message.addMsg("PROGRAM_IMGMOUNT_MULTIPLE_NON_CUEISO_FILES",
                "Using multiple files is only supported for cue/iso images.\n");

        Message.addMsg("PROGRAM_KEYB_INFO", "Codepage {0} has been loaded\n");
        Message.addMsg("PROGRAM_KEYB_INFO_LAYOUT", "Codepage {0} has been loaded for layout {1}\n");
        Message.addMsg("PROGRAM_KEYB_SHOWHELP",
                "\u001B[32;1mKEYB\u001B[0m [keyboard layout ID[ codepage number[ codepage file]]]\n\n"
                        + "Some examples:\n"
                        + "  \u001B[32;1mKEYB\u001B[0m: Display currently loaded codepage.\n"
                        + "  \u001B[32;1mKEYB\u001B[0m sp: Load the spanish (SP) layout, use an appropriate codepage.\n"
                        + "  \u001B[32;1mKEYB\u001B[0m sp 850: Load the spanish (SP) layout, use codepage 850.\n"
                        + "  \u001B[32;1mKEYB\u001B[0m sp 850 mycp.cpi: Same as above, but use file mycp.cpi.\n");
        Message.addMsg("PROGRAM_KEYB_NOERROR", "Keyboard layout {0} loaded for codepage {1}\n");
        Message.addMsg("PROGRAM_KEYB_FILENOTFOUND", "Keyboard file {0} not found\n\n");
        Message.addMsg("PROGRAM_KEYB_INVALIDFILE", "Keyboard file {0} invalid\n");
        Message.addMsg("PROGRAM_KEYB_LAYOUTNOTFOUND", "No layout in {0} for codepage {1}\n");
        Message.addMsg("PROGRAM_KEYB_INVCPFILE",
                "None or invalid codepage file for layout {0}\n\n");

        /* regular setup */
        Programs.makeFile("MOUNT.COM", Mount::makeProgram);
        Programs.makeFile("MEM.COM", MEM::makeProgram);
        Programs.makeFile("LOADFIX.COM", LoadFix::makeProgram);
        Programs.makeFile("RESCAN.COM", Rescan::makeProgram);
        Programs.makeFile("INTRO.COM", Intro::makeProgram);
        Programs.makeFile("BOOT.COM", Boot::makeProgram);

        Programs.makeFile("IMGMOUNT.COM", ImgMount::makeProgram);
        Programs.makeFile("KEYB.COM", KEYB::makeProgram);
    }
    /*--------------------------- end DOSPrograms -----------------------------*/

    /*--------------------------- begin DOSMemory -----------------------------*/
    // -- #region dos_memory

    private static final int UMB_START_SEG = 0x9fff;

    private static int memAllocStrategy = 0x00;

    public static void compressMemory() {
        int mcbSegment = DOS.FirstMCB;
        DOSMCB mcb = new DOSMCB(mcbSegment);
        DOSMCB mcbNext = new DOSMCB(0);

        while (mcb.getType() != 0x5a) {
            mcbNext.setSegPt(mcbSegment + mcb.getSize() + 1);
            if ((mcb.getPSPSeg() == 0) && (mcbNext.getPSPSeg() == 0)) {
                mcb.setSize(mcb.getSize() + mcbNext.getSize() + 1);
                mcb.setType(mcbNext.getType());
            } else {
                mcbSegment += mcb.getSize() + 1;
                mcb.setSegPt(mcbSegment);
            }
        }
    }

    // (uint16)
    public static void freeProcessMemory(int pspseg) {
        int mcbSegment = DOS.FirstMCB;
        DOSMCB mcb = new DOSMCB(mcbSegment);
        for (;;) {
            if (mcb.getPSPSeg() == pspseg) {
                mcb.setPSPSeg(DOSMCB.MCB_FREE);
            }
            if (mcb.getType() == 0x5a) {
                /* check if currently last block reaches up to the PCJr graphics memory */
                if ((DOSBox.Machine == DOSBox.MachineType.PCJR)
                        && (mcbSegment + mcb.getSize() == 0x17fe)
                        && (Memory.realReadB(0x17ff, 0) == 0x4d)
                        && (Memory.realReadW(0x17ff, 1) == 8)) {
                    /* re-enable the memory past segment 0x2000 */
                    mcb.setType(0x4d);
                } else
                    break;
            }
            mcbSegment += mcb.getSize() + 1;
            mcb.setSegPt(mcbSegment);
        }

        int umbStart = DOSInfoBlock.getStartOfUMBChain();
        if (umbStart == UMB_START_SEG) {
            DOSMCB umbMCB = new DOSMCB(umbStart);
            for (;;) {
                if (umbMCB.getPSPSeg() == pspseg) {
                    umbMCB.setPSPSeg(DOSMCB.MCB_FREE);
                }
                if (umbMCB.getType() != 0x4d)
                    break;
                umbStart += umbMCB.getSize() + 1;
                umbMCB.setSegPt(umbStart);
            }
        } else if (umbStart != 0xffff)
            Log.logging(Log.LogTypes.DOSMISC, Log.LogServerities.Error, "Corrupt UMB chain: %x",
                    umbStart);

        compressMemory();
    }

    public static int getMemAllocStrategy() {
        return memAllocStrategy;
    }

    // public static boolean SetMemAllocStrategy(short strat) {
    public static boolean setMemAllocStrategy(int strat) {
        if ((strat & 0x3f) < 3) {
            memAllocStrategy = strat;
            return true;
        }
        /* otherwise an invalid allocation strategy was specified */
        return false;
    }

    public static int returnedAllocateMemorySeg, returnedAllocateMemoryBlock;

    public static boolean tryAllocateMemory(int blocks) {
        compressMemory();
        int bigSize = 0;
        int memStrat = memAllocStrategy;
        int mcbSegment = DOS.FirstMCB;

        int umbStart = DOSInfoBlock.getStartOfUMBChain();
        if (umbStart == UMB_START_SEG) {
            /* start with UMBs if requested (bits 7 or 6 set) */
            if ((memStrat & 0xc0) != 0)
                mcbSegment = umbStart;
        } else if (umbStart != 0xffff)
            Log.logging(Log.LogTypes.DOSMISC, Log.LogServerities.Error, "Corrupt UMB chain: %x",
                    umbStart);

        DOSMCB mcb = new DOSMCB(0);
        DOSMCB mcbNext = new DOSMCB(0);
        DOSMCB pspMCB = new DOSMCB(DOS.getPSP() - 1);
        CStringPt pspName = CStringPt.create(9);
        pspMCB.getFileName(pspName);
        int foundSeg = 0, foundSegSize = 0;
        for (;;) {
            mcb.setSegPt(mcbSegment);
            if (mcb.getPSPSeg() == 0) {
                /* Check for enough free memory in current block */
                int blockSize = mcb.getSize();
                if (blockSize < blocks) {
                    if (bigSize < blockSize) {
                        /*
                         * current block is largest block that was found, but still not as big as
                         * requested
                         */
                        bigSize = blockSize;
                    }
                } else if ((blockSize == blocks) && ((memStrat & 0x3f) < 2)) {
                    /* MCB fits precisely, use it if search strategy is firstfit or bestfit */
                    mcb.setPSPSeg(DOS.getPSP());
                    returnedAllocateMemorySeg = mcbSegment + 1;
                    return true;
                } else {
                    switch (memStrat & 0x3f) {
                        case 0: /* firstfit */
                            mcbNext.setSegPt(mcbSegment + blocks + 1);
                            mcbNext.setPSPSeg(DOSMCB.MCB_FREE);
                            mcbNext.setType(mcb.getType());
                            mcbNext.setSize(blockSize - blocks - 1);
                            mcb.setSize(blocks);
                            mcb.setType(0x4d);
                            mcb.setPSPSeg(DOS.getPSP());
                            mcb.setFileName(pspName);
                            // TODO Filename
                            returnedAllocateMemorySeg = mcbSegment + 1;
                            return true;
                        case 1: /* bestfit */
                            if ((foundSegSize == 0) || (blockSize < foundSegSize)) {
                                /* first fitting MCB, or smaller than the last that was found */
                                foundSeg = mcbSegment;
                                foundSegSize = blockSize;
                            }
                            break;
                        default: /* everything else is handled as lastfit by dos */
                            /* MCB is large enough, note it down */
                            foundSeg = mcbSegment;
                            foundSegSize = blockSize;
                            break;
                    }
                }
            }
            /* Onward to the next MCB if there is one */
            if (mcb.getType() == 0x5a) {
                if ((memStrat & 0x80) != 0 && (umbStart == UMB_START_SEG)) {
                    /* bit 7 set: try high memory first, then low */
                    mcbSegment = DOS.FirstMCB;
                    memStrat &= 0xffff & ~0xc0;
                } else {
                    /* finished searching all requested MCB chains */
                    if (foundSeg != 0) {
                        /* a matching MCB was found (cannot occur for firstfit) */
                        if ((memStrat & 0x3f) == 0x01) {
                            /* bestfit, allocate block at the beginning of the MCB */
                            mcb.setSegPt(foundSeg);

                            mcbNext.setSegPt(foundSeg + blocks + 1);
                            mcbNext.setPSPSeg(DOSMCB.MCB_FREE);
                            mcbNext.setType(mcb.getType());
                            mcbNext.setSize(foundSegSize - blocks - 1);

                            mcb.setSize(blocks);
                            mcb.setType(0x4d);
                            mcb.setPSPSeg(DOS.getPSP());
                            mcb.setFileName(pspName);
                            // TODO Filename
                            returnedAllocateMemorySeg = foundSeg + 1;
                        } else {
                            /* lastfit, allocate block at the end of the MCB */
                            mcb.setSegPt(foundSeg);
                            if (foundSegSize == blocks) {
                                /* use the whole block */
                                mcb.setPSPSeg(DOS.getPSP());
                                // Not consistent with line 124. But how many application will use
                                // this information ?
                                mcb.setFileName(pspName);
                                returnedAllocateMemorySeg = foundSeg + 1;
                                return true;
                            }
                            returnedAllocateMemorySeg = foundSeg + 1 + foundSegSize - blocks;
                            mcbNext.setSegPt(returnedAllocateMemorySeg - 1);
                            mcbNext.setSize(blocks);
                            mcbNext.setType(mcb.getType());
                            mcbNext.setPSPSeg(DOS.getPSP());
                            mcbNext.setFileName(pspName);
                            // Old Block
                            mcb.setSize(foundSegSize - blocks - 1);
                            mcb.setPSPSeg(DOSMCB.MCB_FREE);
                            mcb.setType(0x4D);
                        }
                        return true;
                    }
                    /* no fitting MCB found, return size of largest block */
                    returnedAllocateMemoryBlock = blocks = bigSize;
                    setError(DOSERR_INSUFFICIENT_MEMORY);
                    return false;
                }
            } else
                mcbSegment += mcb.getSize() + 1;
        }
        // return false;
    }


    public static int returnedResizedMemoryBlocks;

    public static boolean tryResizeMemory(int segment, int blocks) {
        if (segment < DOS_MEM_START + 1) {
            Log.logging(Log.LogTypes.DOSMISC, Log.LogServerities.Error,
                    "Program resizes %X, take care", segment);
        }

        DOSMCB mcb = new DOSMCB(segment - 1);
        if ((mcb.getType() != 0x4d) && (mcb.getType() != 0x5a)) {
            setError(DOSERR_MCB_DESTROYED);
            return false;
        }

        compressMemory();
        int total = mcb.getSize();
        DOSMCB mcbNext = new DOSMCB(segment + total);
        if (blocks <= total) {
            if (blocks == total) {
                /* Nothing to do */
                return true;
            }
            /* Shrinking MCB */
            DOSMCB mcbNewNext = new DOSMCB(segment + blocks);
            mcb.setSize(blocks);
            mcbNewNext.setType(mcb.getType());
            if (mcb.getType() == 0x5a) {
                /* Further blocks follow */
                mcb.setType(0x4d);
            }

            mcbNewNext.setSize(total - blocks - 1);
            mcbNewNext.setPSPSeg(DOSMCB.MCB_FREE);
            mcb.setPSPSeg(DOS.getPSP());
            return true;
        }
        /* MCB will grow, try to join with following MCB */
        if (mcb.getType() != 0x5a) {
            if (mcbNext.getPSPSeg() == DOSMCB.MCB_FREE) {
                total += mcbNext.getSize() + 1;
            }
        }
        if (blocks < total) {
            if (mcb.getType() != 0x5a) {
                /* save type of following MCB */
                mcb.setType(mcbNext.getType());
            }
            mcb.setSize(blocks);
            mcbNext.setSegPt(segment + blocks);
            mcbNext.setSize(total - blocks - 1);
            mcbNext.setType(mcb.getType());
            mcbNext.setPSPSeg(DOSMCB.MCB_FREE);
            mcb.setType(0x4d);
            mcb.setPSPSeg(DOS.getPSP());
            return true;
        }

        /*
         * at this point: *blocks==total (fits) or *blocks>total, in the second case resize block to
         * maximum
         */

        if ((mcbNext.getPSPSeg() == DOSMCB.MCB_FREE) && (mcb.getType() != 0x5a)) {
            /* adjust type of joined MCB */
            mcb.setType(mcbNext.getType());
        }
        mcb.setSize(total);
        mcb.setPSPSeg(DOS.getPSP());
        if (blocks == total)
            return true; /* block fit exactly */

        returnedResizedMemoryBlocks = total; /* return maximum */
        setError(DOSERR_INSUFFICIENT_MEMORY);
        return false;
    }


    // uint16
    public static boolean freeMemory(int segment) {
        // TODO Check if allowed to free this segment
        if (segment < DOS_MEM_START + 1) {
            Log.logging(Log.LogTypes.DOSMISC, Log.LogServerities.Error,
                    "Program tried to free %X ---ERROR", segment);
            setError(DOSERR_MB_ADDRESS_INVALID);
            return false;
        }

        DOSMCB mcb = new DOSMCB(segment - 1);
        if ((mcb.getType() != 0x4d) && (mcb.getType() != 0x5a)) {
            setError(DOSERR_MB_ADDRESS_INVALID);
            return false;
        }
        mcb.setPSPSeg(DOSMCB.MCB_FREE);
        // DOS_CompressMemory();
        return true;
    }


    public static void buildUMBChain(boolean umbActive, boolean emsActive) {
        if (umbActive && (DOSBox.Machine != DOSBox.MachineType.TANDY)) {
            int first_umb_seg = 0xd000;
            int first_umb_size = 0x2000;
            if (emsActive || (DOSBox.Machine == DOSBox.MachineType.PCJR))
                first_umb_size = 0x1000;

            DOSInfoBlock.setStartOfUMBChain(UMB_START_SEG);
            DOSInfoBlock.setUMBChainState(0); // UMBs not linked yet

            DOSMCB umbMCB = new DOSMCB(first_umb_seg);
            umbMCB.setPSPSeg(0); // currently free
            umbMCB.setSize(first_umb_size - 1);
            umbMCB.setType(0x5a);

            /* Scan MCB-chain for last block */
            int mcbSegment = DOS.FirstMCB;
            DOSMCB mcb = new DOSMCB(mcbSegment);
            while (mcb.getType() != 0x5a) {
                mcbSegment += mcb.getSize() + 1;
                mcb.setSegPt(mcbSegment);
            }

            /*
             * A system MCB has to cover the space between the regular MCB-chain and the UMBs
             */
            int cover_mcb = mcbSegment + mcb.getSize() + 1;
            mcb.setSegPt(cover_mcb);
            mcb.setType(0x4d);
            mcb.setPSPSeg(0x0008);
            mcb.setSize(first_umb_seg - cover_mcb - 1);
            mcb.setFileName(CStringPt.create("SC      "));

        } else {
            DOSInfoBlock.setStartOfUMBChain(0xffff);
            DOSInfoBlock.setUMBChainState(0);
        }
    }

    // boolean(short linkState)
    public static boolean linkUMBsToMemChain(int linkState) {
        /* Get start of UMB-chain */
        int umbStart = DOSInfoBlock.getStartOfUMBChain();
        if (umbStart != UMB_START_SEG) {
            if (umbStart != 0xffff)
                Log.logging(Log.LogTypes.DOSMISC, Log.LogServerities.Error, "Corrupt UMB chain: %x",
                        umbStart);
            return false;
        }

        if ((linkState & 1) == (DOSInfoBlock.getUMBChainState() & 1))
            return true;

        /* Scan MCB-chain for last block before UMB-chain */
        int mcbSegment = DOS.FirstMCB;
        int prevMCBSegment = DOS.FirstMCB;
        DOSMCB mcb = new DOSMCB(mcbSegment);
        while ((mcbSegment != umbStart) && (mcb.getType() != 0x5a)) {
            prevMCBSegment = mcbSegment;
            mcbSegment += mcb.getSize() + 1;
            mcb.setSegPt(mcbSegment);
        }
        DOSMCB prevMCB = new DOSMCB(prevMCBSegment);

        switch (linkState) {
            case 0x0000: // unlink
                if ((prevMCB.getType() == 0x4d) && (mcbSegment == umbStart)) {
                    prevMCB.setType(0x5a);
                }
                DOSInfoBlock.setUMBChainState(0);
                break;
            case 0x0001: // link
                if (mcb.getType() == 0x5a) {
                    mcb.setType(0x4d);
                    DOSInfoBlock.setUMBChainState(1);
                }
                break;
            default:
                Log.logMsg("Invalid link state %x when reconfiguring MCB chain", linkState);
                return false;
        }

        return true;
    }


    private static int defaultHandler() {
        Log.logging(Log.LogTypes.CPU, Log.LogServerities.Error, "DOS rerouted Interrupt Called %X",
                CPU.LastInt);
        return Callback.ReturnTypeNone;
    }

    private static CallbackHandlerObject callbackHandler = new CallbackHandlerObject();

    public static void setupMemory() {
        /*
         * Let dos claim a few bios interrupts. Makes DOSBox more compatible with buggy games, which
         * compare against the interrupt table. (probably a broken linked list implementation)
         */
        callbackHandler.allocate(DOSMain::defaultHandler, "DOS default int");
        short ihseg = 0x70;
        short ihofs = 0x08;
        Memory.realWriteB(ihseg, ihofs + 0x00, 0xFE); // GRP 4
        Memory.realWriteB(ihseg, ihofs + 0x01, 0x38); // Extra Callback instruction
        Memory.realWriteW(ihseg, ihofs + 0x02, callbackHandler.getCallback()); // The immediate word
        Memory.realWriteB(ihseg, ihofs + 0x04, 0xCF); // An IRET Instruction
        Memory.realSetVec(0x01, Memory.realMake(ihseg, ihofs)); // BioMenace (offset!=4)
        Memory.realSetVec(0x02, Memory.realMake(ihseg, ihofs)); // BioMenace (segment<0x8000)
        Memory.realSetVec(0x03, Memory.realMake(ihseg, ihofs)); // Alien Incident (offset!=0)
        Memory.realSetVec(0x04, Memory.realMake(ihseg, ihofs)); // Shadow President (lower
                                                                // byte of segment!=0)
        // MemModule.RealSetVec(0x0f,MemModule.RealMake(ihseg,ihofs)); //Always a tricky one
        // (soundblaster irq)

        // Create a dummy device MCB with PSPSeg=0x0008
        DOSMCB mcb_devicedummy = new DOSMCB(DOS_MEM_START);
        mcb_devicedummy.setPSPSeg(DOSMCB.MCB_DOS); // Devices
        mcb_devicedummy.setSize(1);
        mcb_devicedummy.setType(0x4d); // More blocks will follow
        // mcb_devicedummy.SetFileName("SD ");

        short mcb_sizes = 2;
        // Create a small empty MCB (result from a growing environment block)
        DOSMCB tempmcb = new DOSMCB(DOS_MEM_START + mcb_sizes);
        tempmcb.setPSPSeg(DOSMCB.MCB_FREE);
        tempmcb.setSize(4);
        mcb_sizes += 5;
        tempmcb.setType(0x4d);

        // Lock the previous empty MCB
        DOSMCB tempmcb2 = new DOSMCB(DOS_MEM_START + mcb_sizes);
        tempmcb2.setPSPSeg(0x40); // can be removed by loadfix
        tempmcb2.setSize(16);
        mcb_sizes += 17;
        tempmcb2.setType(0x4d);

        DOSMCB mcb = new DOSMCB(DOS_MEM_START + mcb_sizes);
        mcb.setPSPSeg(DOSMCB.MCB_FREE); // Free
        mcb.setType(0x5a); // Last Block
        if (DOSBox.Machine == DOSBox.MachineType.TANDY) {
            /*
             * memory up to 608k available, the rest (to 640k) is used by the tandy graphics
             * system's variable mapping of 0xb800
             */
            mcb.setSize(0x97FF - DOS_MEM_START - mcb_sizes);
        } else if (DOSBox.Machine == DOSBox.MachineType.PCJR) {
            /* memory from 128k to 640k is available */
            mcb_devicedummy.setSegPt(0x2000);
            mcb_devicedummy.setPSPSeg(DOSMCB.MCB_FREE);
            mcb_devicedummy.setSize(0x9FFF - 0x2000);
            mcb_devicedummy.setType(0x5a);

            /* exclude PCJr graphics region */
            mcb_devicedummy.setSegPt(0x17ff);
            mcb_devicedummy.setPSPSeg(DOSMCB.MCB_DOS);
            mcb_devicedummy.setSize(0x800);
            mcb_devicedummy.setType(0x4d);

            /* memory below 96k */
            mcb.setSize(0x1800 - DOS_MEM_START - (2 + mcb_sizes));
            mcb.setType(0x4d);
        } else {
            /* complete memory up to 640k available */
            /* last paragraph used to add UMB chain to low-memory MCB chain */
            mcb.setSize(0x9FFE - DOS_MEM_START - mcb_sizes);
        }

        DOS.FirstMCB = DOS_MEM_START;
        DOSInfoBlock.setFirstMCB(DOS_MEM_START);
    }

    // -- #endregion

    /*--------------------------- end DOSMemory -----------------------------*/

    /*--------------------------- begin DOSKeyboardLayout -----------------------------*/
    public static final byte KEYB_NOERROR = 0;
    public static final byte KEYB_FILENOTFOUND = 1;
    public static final byte KEYB_INVALIDFILE = 2;
    public static final byte KEYB_LAYOUTNOTFOUND = 3;
    public static final byte KEYB_INVALIDCPFILE = 4;

    public static KeyboardLayout LoadedLayout = null;

    // public static boolean layoutKey(int key, byte flags1, byte flags2, byte flags3) {
    public static boolean layoutKey(int key, int flags1, int flags2, int flags3) {
        if (LoadedLayout != null)
            return LoadedLayout.LayoutKey(key, flags1, flags2, flags3);
        else
            return false;
    }

    public static int loadKeyboardLayout(String layoutName, int codepage, String codepageFile) {
        KeyboardLayout temp_layout = new KeyboardLayout();
        // try to read the layout for the specified codepage
        int kerrcode = temp_layout.ReadKeyboardFile(layoutName, codepage);
        if (kerrcode != 0) {
            temp_layout.dispose();
            temp_layout = null;
            return kerrcode;
        }
        // ...else keyboard layout loaded successfully, change codepage accordingly
        kerrcode = temp_layout.readCodePageFile(codepageFile, codepage);
        if (kerrcode != 0) {
            temp_layout.dispose();
            temp_layout = null;
            return kerrcode;
        }
        // Everything went fine, switch to new layout
        LoadedLayout = temp_layout;
        return KEYB_NOERROR;
    }


    public static int returnedSwitchKBLTryiedCP;

    public static int trySwitchKeyboardLayout(String newLayout) {
        if (LoadedLayout != null) {
            KeyboardLayout changedLayout = null;
            int retCode = LoadedLayout.trySwitchKeyboardLayout(newLayout);
            changedLayout = LoadedLayout.returnedSwitchKBLCreatedLayout;
            returnedSwitchKBLTryiedCP = LoadedLayout.returnedSwitchKBLTriedCP;
            if (changedLayout != null) {
                // Remove old layout, activate new layout
                LoadedLayout.dispose();
                LoadedLayout = null;
                LoadedLayout = changedLayout;
            }
            return retCode;
        } else
            return 0xff;
    }

    // get currently loaded layout name (NULL if no layout is loaded)
    public static String getLoadedLayout() {
        if (LoadedLayout != null) {
            return LoadedLayout.getLayoutName();
        }
        return null;
    }

    /*--------------------------- end DOSKeyboardLayout -----------------------------*/

    /*--------------------------- begin DOSIOCtl -----------------------------*/

    // -- #region dos_ioctl
    private static boolean doIOCTL() {
        int handle = 0;
        int drive = 0;
        DOSFile file = null;
        /* calls 0-4,6,7,10,12,16 use a file handle */
        if ((Register.getRegAL() < 4) || (Register.getRegAL() == 0x06)
                || (Register.getRegAL() == 0x07) || (Register.getRegAL() == 0x0a)
                || (Register.getRegAL() == 0x0c) || (Register.getRegAL() == 0x10)) {
            handle = realHandle(Register.getRegBX());
            if (handle >= DOS_FILES) {
                setError(DOSERR_INVALID_HANDLE);
                return false;
            }
            file = Files[handle];
            if (file == null) {
                setError(DOSERR_INVALID_HANDLE);
                return false;
            }
        } else if (Register.getRegAL() < 0x12) { /* those use a diskdrive except 0x0b */
            if (Register.getRegAL() != 0x0b) {
                drive = Register.getRegBL();
                if (drive == 0)
                    drive = getDefaultDrive();
                else
                    drive--;
                if (!((drive < DOS_DRIVES) && Drives[drive] != null)) {
                    setError(DOSERR_INVALID_DRIVE);
                    return false;
                }
            }
        } else {
            Log.logging(Log.LogTypes.DOSMISC, Log.LogServerities.Error,
                    "DOS:IOCTL Call %2X unhandled", Register.getRegAL());
            setError(DOSERR_FUNCTION_NUMBER_INVALID);
            return false;
        }
        switch (Register.getRegAL()) {
            case 0x00: /* Get Device Information */
                if ((file.getInformation() & 0x8000) != 0) { // Check for device
                    Register.setRegDX(file.getInformation());
                } else {
                    int hdrive = file.getDrive();
                    if (hdrive == 0xff) {
                        Log.logging(Log.LogTypes.IOCTL, Log.LogServerities.Normal,
                                "00:No drive set");
                        hdrive = 2; // defaulting to C:
                    }
                    /* return drive number in lower 5 bits for block devices */
                    Register.setRegDX((file.getInformation() & 0xffe0) | hdrive);
                }
                Register.setRegAX(Register.getRegDX()); // Destroyed officially
                return true;
            case 0x01: /* Set Device Information */
                if (Register.getRegDH() != 0) {
                    setError(DOSERR_DATA_INVALID);
                    return false;
                } else {
                    if ((file.getInformation() & 0x8000) != 0) { // Check for device
                        Register.setRegAL(file.getInformation() & 0xff);
                    } else {
                        setError(DOSERR_FUNCTION_NUMBER_INVALID);
                        return false;
                    }
                }
                return true;
            case 0x02: /* Read from Device Control Channel */
                if ((file.getInformation() & 0xc000) != 0) {
                    /* is character device with IOCTL support */
                    int bufptr = Memory.physMake(Register.segValue(Register.SEG_NAME_DS),
                            Register.getRegDX());
                    int retcode =
                            ((DOSDevice) file).readFromControlChannel(bufptr, Register.getRegCX());
                    if (retcode >= 0) {
                        Register.setRegAX(retcode);
                        return true;
                    }
                }
                setError(DOSERR_FUNCTION_NUMBER_INVALID);
                return false;
            case 0x03: /* Write to Device Control Channel */
                if ((file.getInformation() & 0xc000) != 0) {
                    /* is character device with IOCTL support */
                    int bufptr = Memory.physMake(Register.segValue(Register.SEG_NAME_DS),
                            Register.getRegDX());
                    int retcode =
                            ((DOSDevice) file).writeToControlChannel(bufptr, Register.getRegCX());
                    if (retcode >= 0) {
                        Register.setRegAX(retcode);
                        return true;
                    }
                }
                setError(DOSERR_FUNCTION_NUMBER_INVALID);
                return false;
            case 0x06: /* Get Input Status */
                if ((file.getInformation() & 0x8000) != 0) { // Check for device
                    Register.setRegAL((file.getInformation() & 0x40) != 0 ? 0x0 : 0xff);
                } else { // FILE
                    long oldlocation = 0;
                    oldlocation = file.seek(oldlocation, DOSSystem.DOS_SEEK_CUR);
                    long endlocation = 0;
                    endlocation = file.seek(endlocation, DOSSystem.DOS_SEEK_END);
                    if (oldlocation < endlocation) {// Still data available
                        Register.setRegAL(0xff);
                    } else {
                        Register.setRegAL(0x0); // EOF or beyond
                    }
                    oldlocation = file.seek(oldlocation, DOSSystem.DOS_SEEK_SET); // restore
                                                                                  // filelocation
                    Log.logging(Log.LogTypes.IOCTL, Log.LogServerities.Normal,
                            "06:Used Get Input Status on regular file with handle %d", handle);
                }
                return true;
            case 0x07: /* Get Output Status */
                Log.logging(Log.LogTypes.IOCTL, Log.LogServerities.Normal,
                        "07:Fakes output status is ready for handle %d", handle);
                Register.setRegAL(0xff);
                return true;
            case 0x08: /* Check if block device removable */
                /* cdrom drives and drive a&b are removable */
                if (drive < 2)
                    Register.setRegAX(0);
                else if (!Drives[drive].isRemovable())
                    Register.setRegAX(1);
                else {
                    setError(DOSERR_FUNCTION_NUMBER_INVALID);
                    return false;
                }
                return true;
            case 0x09: /* Check if block device remote */
                if (Drives[drive].isRemote()) {
                    Register.setRegDX(0x1000); // device is remote
                    // undocumented bits always clear
                } else {
                    Register.setRegDX(0x0802); // Open/Close supported; 32bit access
                                               // supported (any use? fixes Fable installer)
                    // undocumented bits from device attribute word
                    // TODO Set bit 9 on drives that don't support direct I/O
                }
                Register.setRegAX(0x300);
                return true;
            case 0x0B: /* Set sharing retry count */
                if (Register.getRegDX() == 0) {
                    setError(DOSERR_FUNCTION_NUMBER_INVALID);
                    return false;
                }
                return true;
            case 0x0D: /* Generic block device request */
            {
                if (Drives[drive].isRemovable()) {
                    setError(DOSERR_FUNCTION_NUMBER_INVALID);
                    return false;
                }
                int ptr = Register.segPhys(Register.SEG_NAME_DS) + Register.getRegDX();
                switch (Register.getRegCL()) {
                    case 0x60: /* Get Device parameters */
                        Memory.writeB(ptr, 0x03); // special function
                        Memory.writeB(ptr + 1, (drive >= 2) ? 0x05 : 0x14); // fixed
                                                                            // disc(5),
                                                                            // 1.44
                                                                            // floppy(14)
                        Memory.writeW(ptr + 2, Convert.toShort(drive >= 2)); // nonremovable ?
                        Memory.writeW(ptr + 4, 0x0000); // num of cylinders
                        Memory.writeB(ptr + 6, 0x00); // media type (00=other type)
                        // drive parameter block following
                        Memory.writeB(ptr + 7, drive); // drive
                        Memory.writeB(ptr + 8, 0x00); // unit number
                        Memory.writeD(ptr + 0x1f, 0xffffffff); // next parameter block
                        break;
                    case 0x46:
                    case 0x66: /* Volume label */
                    {
                        CStringPt bufin = DOSMain.Drives[drive].getLabel();
                        CStringPt buffer = CStringPt.create(11);
                        buffer.set(0, ' ');

                        CStringPt find_ext = bufin.positionOf('.');
                        if (!find_ext.isEmpty()) {
                            int size = CStringPt.diff(find_ext, bufin);
                            if (size > 8)
                                size = 8;
                            CStringPt.rawMove(bufin, buffer, size);
                            find_ext.movePtToR1();
                            CStringPt.rawMove(find_ext, CStringPt.clone(buffer, size),
                                    (find_ext.length() > 3) ? 3 : find_ext.length());
                        } else {
                            CStringPt.rawMove(bufin, buffer,
                                    (bufin.length() > 8) ? 8 : bufin.length());
                        }
                        byte[] buf2 = {(byte) 'F', (byte) 'A', (byte) 'T', (byte) '1', (byte) '6',
                                (byte) ' ', (byte) ' ', (byte) ' '};
                        if (drive < 2)
                            buf2[4] = (byte) '2'; // FAT12 for floppies

                        Memory.writeW(ptr + 0, 0); // 0
                        Memory.writeD(ptr + 2, 0x1234); // Serial number
                        Memory.blockWrite(ptr + 6, buffer);// volumename
                        if (Register.getRegCL() == 0x66)
                            Memory.blockWrite(ptr + 0x11, buf2, 0, 8);// filesystem

                    }
                        break;
                    default:
                        Log.logging(Log.LogTypes.IOCTL, Log.LogServerities.Error,
                                "DOS:IOCTL Call 0D:%2X Drive %2X unhandled", Register.getRegCL(),
                                drive);
                        setError(DOSERR_FUNCTION_NUMBER_INVALID);
                        return false;
                }
                return true;
            }
            case 0x0E: /* Get Logical Drive Map */
                if (Drives[drive].isRemovable()) {
                    setError(DOSERR_FUNCTION_NUMBER_INVALID);
                    return false;
                }
                Register.setRegAL(0); /* Only 1 logical drive assigned */
                return true;
            default:
                Log.logging(Log.LogTypes.DOSMISC, Log.LogServerities.Error,
                        "DOS:IOCTL Call %2X unhandled", Register.getRegAL());
                setError(DOSERR_FUNCTION_NUMBER_INVALID);
                break;
        }
        return false;
    }


    private static boolean getSTDINStatus() {
        int handle = realHandle(STDIN);
        if (handle == 0xFF)
            return false;
        DOSFile file = Files[handle];
        if (file != null && (file.getInformation() & 64) != 0)
            return false;
        return true;
    }

    // -- #endregion

    /*--------------------------- end DOSIOCtl -----------------------------*/

    /*--------------------------- begin DOSFiles -----------------------------*/
    // -- #region dos_files

    private static final int DOS_FILESTART = 4;

    private static final int FCB_SUCCESS = 0;
    private static final int FCB_READ_NODATA = 1;
    private static final int FCB_READ_PARTIAL = 3;
    private static final int FCB_ERR_NODATA = 1;
    private static final int FCB_ERR_EOF = 3;
    private static final int FCB_ERR_WRITE = 1;


    public static DOSFile[] Files = new DOSFile[DOS_FILES];
    public static DOSDrive[] Drives = new DOSDrive[DOS_DRIVES];

    public static int getDefaultDrive() {
        // return DOS_SDA(DOS_SDA_SEG,DOS_SDA_OFS).GetDrive();
        int d = (new DOSSDA(DOS_SDA_SEG, DOS_SDA_OFS)).getDrive();
        if (d != DOS.CurrentDrive)
            Log.logging(Log.LogTypes.DOSMISC, Log.LogServerities.Error,
                    "SDA drive %d not the same as dos.current_drive %d", d, DOS.CurrentDrive);
        return DOS.CurrentDrive;
    }

    // public static void SetDefaultDrive(byte drive) {
    public static void setDefaultDrive(int drive) {
        // if (drive<=DOS_DRIVES && ((drive<2) || Drives[drive]))
        // DOS_SDA(DOS_SDA_SEG,DOS_SDA_OFS).SetDrive(drive);
        if (drive <= DOS_DRIVES && ((drive < 2) || Drives[drive] != null)) {
            DOS.CurrentDrive = drive;
            new DOSSDA(DOS_SDA_SEG, DOS_SDA_OFS).setDrive(drive);
        }
    }

    public static String returnedFullName = "";
    public static int returnedFullNameDrive = 0;

    // TODO makeName 메소드 대체
    // fullname -> resultMakeFullName, drive -> resultMakeFullNameDrive
    public static boolean makeFullName(String name, int maxNameSize) {
        if (name == null || name == "" || name.charAt(0) == 0 || name.charAt(0) == ' ') {
            /*
             * Both \0 and space are seperators and empty filenames report file not found
             */
            setError(DOSERR_FILE_NOT_FOUND);
            return false;
        }
        int nameIdx = 0;
        StringBuffer tempdir = new StringBuffer(DOSSystem.DOS_PATHLENGTH);
        StringBuffer upname = new StringBuffer(DOSSystem.DOS_PATHLENGTH);
        StringBuffer fullname = new StringBuffer(maxNameSize);
        int r;
        int drive = getDefaultDrive();
        /* First get the drive */
        if (name.length() > 1 && name.charAt(nameIdx + 1) == ':') {
            drive = 0xff & ((name.charAt(nameIdx + 0) | 0x20) - 0x61);// 'a' 0x61
            nameIdx += 2;
        }
        returnedFullNameDrive = drive;
        if (drive >= DOS_DRIVES || Drives[drive] == null) {
            setError(DOSERR_PATH_NOT_FOUND);
            return false;
        }
        r = 0;
        while (nameIdx + r < name.length() && (r < DOSSystem.DOS_PATHLENGTH)) {
            char c = name.charAt(nameIdx + r++);
            if ((c >= 'a') && (c <= 'z')) {
                // upname.setCharAt(w++, (char) (c - 32));
                upname.append((char) (c - 32));
                continue;
            }
            if ((c >= 'A') && (c <= 'Z')) {
                // upname.setCharAt(w++, c);
                upname.append(c);
                continue;
            }
            if ((c >= '0') && (c <= '9')) {
                // upname.setCharAt(w++, c);
                upname.append(c);
                continue;
            }
            switch (c) {
                case '/':
                    // upname.setCharAt(w++, '\\');
                    upname.append('\\');
                    break;
                case ' ': /* should be seperator */
                    break;
                case '\\':
                case '$':
                case '#':
                case '@':
                case '(':
                case ')':
                case '!':
                case '%':
                case '{':
                case '}':
                case '`':
                case '~':
                case '_':
                case '-':
                case '.':
                case '*':
                case '?':
                case '&':
                case '\'':
                case '+':
                case '^':
                case (char) 246:
                case (char) 255:
                case (char) 0xa0:
                case (char) 0xe5:
                case (char) 0xbd:
                    // upname.setCharAt(w++, c);
                    upname.append(c);
                    break;
                default:
                    Log.logging(Log.LogTypes.FILES, Log.LogServerities.Normal,
                            "Makename encountered an illegal char %c hex:%X in %s!", c, c, name);
                    setError(DOSERR_PATH_NOT_FOUND);
                    return false;
                // break;
            }
        }
        if (r >= DOSSystem.DOS_PATHLENGTH) {
            setError(DOSERR_PATH_NOT_FOUND);
            return false;
        }
        // upname.setCharAt(w, (char) 0);

        /* Now parse the new file name to make the final filename */
        if (upname.charAt(0) != '\\') {
            fullname.append(Drives[drive].curDir);
        } else {
            fullname.setLength(0);
        }
        int lastdir = 0;
        int t = 0;
        while (fullname.length() > t) {
            if (fullname.charAt(t) == '\\' && fullname.length() > t + 1)
                lastdir = t;
            t++;
        }
        r = 0;
        boolean stop = false;
        while (!stop) {
            if (upname.length() == r)
                stop = true;
            if (upname.length() == r || upname.charAt(r) == '\\') {
                // tempdir.setCharAt(w, (char) 0);
                // tempdir.setLength(w);
                if (tempdir.length() == 0) {
                    r++;
                    continue;
                }
                if (tempdir.charAt(0) == '.') {
                    // tempdir.setCharAt(0, (char) 0);
                    tempdir.setLength(0);
                    r++;
                    continue;
                }

                int iDown;
                boolean dots = true;
                int templen = tempdir.length();
                for (iDown = 0; (iDown < templen) && dots; iDown++)
                    if (tempdir.charAt(iDown) != '.')
                        dots = false;

                // only dots?
                if (dots && (templen > 1)) {
                    int cDots = templen - 1;
                    for (iDown = fullname.length() - 1; iDown >= 0; iDown--) {
                        if (fullname.charAt(iDown) == '\\' || iDown == 0) {
                            lastdir = iDown;
                            cDots--;
                            if (cDots == 0)
                                break;
                        }
                    }
                    // fullname.setCharAt( lastdir, (char) 0);
                    fullname.setLength(lastdir);
                    t = 0;
                    lastdir = 0;
                    while (fullname.length() > t) {
                        if (fullname.charAt(t) == '\\' && fullname.length() > t + 1)
                            lastdir = t;
                        t++;
                    }
                    // tempdir.setCharAt(0, (char) 0);
                    tempdir.setLength(0);
                    r++;
                    continue;
                }


                lastdir = fullname.length();

                if (lastdir > 0)
                    fullname.append('\\');
                // CStringPt ext = tempdir.PositionOf('.');
                int extIdx = tempdir.indexOf(".");
                if (extIdx >= 0) {
                    if (tempdir.indexOf(".", extIdx + 1) >= 0) {
                        // another dot in the extension =>file not found
                        // Or path not found depending on wether
                        // we are still in dir check stage or file stage
                        if (stop)
                            setError(DOSERR_FILE_NOT_FOUND);
                        else
                            setError(DOSERR_PATH_NOT_FOUND);
                        return false;
                    }

                    // ext.set(4, (char) 0);
                    int extLen = 4;
                    tempdir.setLength(extIdx + extLen);
                    // if ((strlen(tempdir) - strlen(ext)) > 8) memmove(tempdir + 8, ext, 5);
                    if ((tempdir.length() - extLen) > 8) {
                        for (int i = 0; i < 4; i++) {// '/0'제외
                            tempdir.setCharAt(i + 8, tempdir.charAt(i + extIdx));
                        }
                    }
                } else {
                    if (tempdir.length() > 8)
                        tempdir.setLength(8);// tempdir.set(8, (char) 0);
                }
                if (fullname.length() + tempdir.length() >= DOSSystem.DOS_PATHLENGTH) {
                    setError(DOSERR_PATH_NOT_FOUND);
                    return false;
                }

                fullname.append(tempdir);
                tempdir.setLength(0);// tempdir.set(0, (char) 0);
                r++;
                continue;
            }
            // tempdir.setCharAt(w++, upname.charAt(r++));
            tempdir.append(upname.charAt(r++));
        }
        returnedFullName = fullname.toString();
        return true;
    }

    public static String getCurrentDir(int drive) {
        if (drive == 0)
            drive = getDefaultDrive();
        else
            drive--;
        if ((drive >= DOS_DRIVES) || (Drives[drive] == null)) {
            setError(DOSERR_INVALID_DRIVE);
            return null;
        }
        return Drives[drive].curDir;
    }

    public static boolean getCurrentDir(int drive, CStringPt buffer) {
        if (drive == 0)
            drive = getDefaultDrive();
        else
            drive--;
        if ((drive >= DOS_DRIVES) || (Drives[drive] == null)) {
            setError(DOSERR_INVALID_DRIVE);
            return false;
        }
        CStringPt.copy(Drives[drive].curDir, buffer);
        return true;
    }

    public static boolean changeDir(String dir) {
        if (!makeFullName(dir, DOSSystem.DOS_PATHLENGTH))
            return false;
        String fullDir = returnedFullName;
        int drive = returnedFullNameDrive;
        if (Drives[drive].testDir(fullDir)) {
            Drives[drive].curDir = fullDir;
            return true;
        } else {
            setError(DOSERR_PATH_NOT_FOUND);
        }
        return false;
    }

    public static boolean makeDir(String dir) {
        int len = dir.length();
        if (len == 0 || dir.charAt(len - 1) == 0x5c) {// '\\' 0x5c
            setError(DOSERR_PATH_NOT_FOUND);
            return false;
        }
        if (!makeFullName(dir, DOSSystem.DOS_PATHLENGTH))
            return false;
        String fullDir = returnedFullName;
        int drive = returnedFullNameDrive;
        if (Drives[drive].makeDir(fullDir))
            return true;

        /* Determine reason for failing */
        if (Drives[drive].testDir(fullDir))
            setError(DOSERR_ACCESS_DENIED);
        else
            setError(DOSERR_PATH_NOT_FOUND);
        return false;
    }

    public static boolean removeDir(String dir) {
        /*
         * We need to do the test before the removal as can not rely on the host to forbid removal
         * of the current directory. We never change directory. Everything happens in the drives.
         */
        if (!makeFullName(dir, DOSSystem.DOS_PATHLENGTH))
            return false;
        String fullDir = returnedFullName;
        int drive = returnedFullNameDrive;

        /* Check if exists */
        if (!Drives[drive].testDir(fullDir)) {
            setError(DOSERR_PATH_NOT_FOUND);
            return false;
        }
        /* See if it's current directory */
        CStringPt currdir = CStringPt.create(DOSSystem.DOS_PATHLENGTH);
        currdir.set(0, (char) 0);
        getCurrentDir(drive + 1, currdir);
        if (currdir.equalsIgnoreCase(fullDir)) {
            setError(DOSERR_REMOVE_CURRENT_DIRECTORY);
            return false;
        }

        if (Drives[drive].removeDir(fullDir))
            return true;

        /* Failed. We know it exists and it's not the current dir */
        /* Assume non empty */
        setError(DOSERR_ACCESS_DENIED);
        return false;
    }

    public static boolean rename(String oldName, String newName) {
        if (!makeFullName(oldName, DOSSystem.DOS_PATHLENGTH))
            return false;
        String fullOld = returnedFullName;
        int driveOld = returnedFullNameDrive;
        if (!makeFullName(newName, DOSSystem.DOS_PATHLENGTH))
            return false;
        String fullNew = returnedFullName;
        int driveNew = returnedFullNameDrive;
        /* No tricks with devices */
        if ((findDevice(oldName) != DOS_DEVICES) || (findDevice(newName) != DOS_DEVICES)) {
            setError(DOSERR_FILE_NOT_FOUND);
            return false;
        }
        /* Must be on the same drive */
        if (driveOld != driveNew) {
            setError(DOSERR_NOT_SAME_DEVICE);
            return false;
        }
        /* Test if target exists => no access */
        if (Drives[driveNew].tryFileAttr(fullNew.toString())) {
            setError(DOSERR_ACCESS_DENIED);
            return false;
        }
        /* Source must exist, check for path ? */
        if (!Drives[driveOld].tryFileAttr(fullOld.toString())) {
            setError(DOSERR_FILE_NOT_FOUND);
            return false;
        }

        if (Drives[driveNew].rename(fullOld.toString(), fullNew.toString()))
            return true;
        /* If it still fails. which error should we give ? PATH NOT FOUND or EACCESS */
        Log.logging(Log.LogTypes.FILES, Log.LogServerities.Normal,
                "Rename fails for %s to %s, no proper errorcode returned.", oldName, newName);
        setError(DOSERR_FILE_NOT_FOUND);
        return false;
    }

    public static boolean findFirst(String search, int attr) {
        return findFirst(search, attr, false);
    }

    public static boolean findFirst(String search, int attr, boolean fcb_findfirst) {
        DOSDTA dta = new DOSDTA(DOS.getDTA());
        CStringPt dir = CStringPt.create(DOSSystem.DOS_PATHLENGTH);
        CStringPt pattern = CStringPt.create(DOSSystem.DOS_PATHLENGTH);
        int len = search.length();
        // '\\' 0x5c
        if (len >= 0 && search.charAt(len - 1) == 0x5c && !((len > 2)
                && (search.charAt(len - 2) == ':') && (attr == DOSSystem.DOS_ATTR_VOLUME))) {
            // Dark Forces installer, but c:\ is allright for volume labels(exclusively set)
            setError(DOSERR_NO_MORE_FILES);
            return false;
        }
        if (!makeFullName(search, DOSSystem.DOS_PATHLENGTH))
            return false;
        String fullSearch = returnedFullName;
        int drive = returnedFullNameDrive;

        // Check for devices. FindDevice checks for leading subdir as well
        boolean device = (findDevice(search) != DOS_DEVICES);

        /* Split the search in dir and pattern */
        // CStringPt find_last = fullsearch.lastPositionOf('\\');
        int lastIdx = fullSearch.lastIndexOf('\\');
        if (lastIdx < 0) { /* No dir */
            CStringPt.copy(fullSearch, pattern);
            dir.set(0, (char) 0);
        } else {
            CStringPt.copy(fullSearch.substring(lastIdx + 1), pattern);
            fullSearch = fullSearch.substring(0, lastIdx);
            CStringPt.copy(fullSearch, dir);
        }

        dta.setupSearch(drive, 0xff & attr, pattern);

        if (device) {
            CStringPt findLast = pattern.lastPositionOf('.');
            if (!findLast.isEmpty())
                findLast.set((char) 0);
            // TODO use current date and time
            dta.setResult(pattern, 0, 0, 0, DOSSystem.DOS_ATTR_DEVICE);
            Log.logging(Log.LogTypes.DOSMISC, Log.LogServerities.Warn, "finding device %s",
                    pattern.toString());
            return true;
        }

        if (Drives[drive].findFirst(dir, dta, fcb_findfirst))
            return true;

        return false;
    }

    public static boolean findNext() {
        DOSDTA dta = new DOSDTA(DOS.getDTA());
        int i = dta.getSearchDrive();
        if (i >= DOS_DRIVES || Drives[i] == null) {
            /* Corrupt search. */
            Log.logging(Log.LogTypes.FILES, Log.LogServerities.Error, "Corrupt search!!!!");
            setError(DOSERR_NO_MORE_FILES);
            return false;
        }
        if (Drives[i].findNext(dta))
            return true;
        return false;
    }


    public static byte ReadByte;
    private static int readFileSize;

    // bool(uint16, ref bytes[], offset, len )
    public static boolean readFile(int entry, byte[] buf, int offset, int size) {
        int handle = realHandle(entry);
        if (handle >= DOS_FILES) {
            setError(DOSERR_INVALID_HANDLE);
            return false;
        }
        DOSFile file = Files[handle];
        if (file == null || !file.isOpen()) {
            setError(DOSERR_INVALID_HANDLE);
            return false;
        }

        // short toread = amount;
        boolean ret = file.read(buf, offset, size);
        readFileSize = file.readSize();
        return ret;
    }

    // bool(uint16, ref byte, uint16)
    public static boolean readFile(int entry) {
        int handle = realHandle(entry);
        if (handle >= DOS_FILES) {
            setError(DOSERR_INVALID_HANDLE);
            return false;
        }
        DOSFile file = Files[handle];
        if (file == null || !file.isOpen()) {
            setError(DOSERR_INVALID_HANDLE);
            return false;
        }

        boolean ret = file.read();
        readFileSize = file.readSize();
        ReadByte = file.getReadByte();
        return ret;

    }

    public static int readSize() {
        return readFileSize;
    }

    public static int WrittenSize;

    // public static boolean WriteFile(short entry, BufRef buf)
    public static boolean writeFile(int entry, byte[] buf, int offset, int size) {
        int handle = realHandle(entry);
        if (handle >= DOS_FILES) {
            setError(DOSERR_INVALID_HANDLE);
            return false;
        }
        DOSFile file = Files[handle];
        if (file == null || !file.isOpen()) {
            setError(DOSERR_INVALID_HANDLE);
            return false;
        }
        boolean ret = file.write(buf, offset, size);
        WrittenSize = file.writtenSize();
        return ret;
    }

    // (uint16, uint8)
    public static boolean writeFile(int entry, byte buf, int size) {
        int handle = realHandle(entry);
        if (handle >= DOS_FILES) {
            setError(DOSERR_INVALID_HANDLE);
            return false;
        }
        DOSFile file = Files[handle];
        if (file == null || !file.isOpen()) {
            setError(DOSERR_INVALID_HANDLE);
            return false;
        }
        boolean ret = file.write(buf, size);
        WrittenSize = file.writtenSize();
        return ret;
    }

    // (uint16, uint8)
    public static boolean writeFile(int entry, byte buf) {
        int handle = realHandle(entry);
        if (handle >= DOS_FILES) {
            setError(DOSERR_INVALID_HANDLE);
            return false;
        }
        DOSFile file = Files[handle];
        if (file == null || !file.isOpen()) {
            setError(DOSERR_INVALID_HANDLE);
            return false;
        }
        boolean ret = file.write(buf);
        WrittenSize = file.writtenSize();
        return ret;
    }

    // return pos
    // 실패하면 -1 반환
    public static long seekFile(int entry, long pos, int type) {
        int handle = realHandle(entry);
        if (handle >= DOS_FILES) {
            setError(DOSERR_INVALID_HANDLE);
            return -1;
        }
        DOSFile file = Files[handle];
        if (file == null || !file.isOpen()) {
            setError(DOSERR_INVALID_HANDLE);
            return -1;
        }
        return file.seek(pos, type);
    }

    // boolean (uint16)
    public static boolean closeFile(int entry) {
        int handle = realHandle(entry);
        if (handle >= DOS_FILES) {
            setError(DOSERR_INVALID_HANDLE);
            return false;
        }
        DOSFile file = Files[handle];
        if (file == null) {
            setError(DOSERR_INVALID_HANDLE);
            return false;
        }
        if (file.isOpen()) {
            file.close();
        }
        DOSPSP psp = new DOSPSP(DOS.getPSP());
        psp.setFileHandle(entry, 0xff);
        if (file.removeRef() <= 0) {
            file.dispose();
            Files[handle] = null;
        }
        return true;
    }

    public static boolean flushFile(int entry) {
        int handle = realHandle(entry);
        if (handle >= DOS_FILES) {
            setError(DOSERR_INVALID_HANDLE);
            return false;
        }
        DOSFile file = Files[handle];
        if (file == null || !file.isOpen()) {
            setError(DOSERR_INVALID_HANDLE);
            return false;
        }
        Log.logging(Log.LogTypes.DOSMISC, Log.LogServerities.Normal, "FFlush used.");
        return true;
    }

    private static boolean pathExists(String name) {
        int leading_idx = name.lastIndexOf('\\');
        if (leading_idx < 0 || leading_idx == 0)
            return true;
        String temp = name;
        if (!makeFullName(temp, DOSSystem.DOS_PATHLENGTH))
            return false;
        String fullDir = returnedFullName;
        int drive = returnedFullNameDrive;
        if (!Drives[drive].testDir(fullDir))
            return false;
        return true;
    }

    public static int returnFileHandle;

    // 생성한 file handle은 FileEntry에 저장
    // bool(string, uint16 , ref uint16)
    // created file handle -> returnFileHandle
    public static boolean createFile(String name, int attributes) {
        attributes &= 0xffff;
        // Creation of a device is the same as opening it
        // Tc201 installer
        if (findDevice(name) != DOS_DEVICES)
            return openFile(name, DOSSystem.OPEN_READ);

        Log.logging(Log.LogTypes.FILES, Log.LogServerities.Normal,
                "file create attributes %X file %s", attributes, name);
        DOSPSP psp = new DOSPSP(DOS.getPSP());
        if (!makeFullName(name, DOSSystem.DOS_PATHLENGTH))
            return false;
        String fullName = returnedFullName;
        int drive = returnedFullNameDrive;
        /* Check for a free file handle */
        int handle = DOS_FILES;
        int i;
        for (i = 0; i < DOS_FILES; i++) {
            if (Files[i] == null) {
                handle = i;
                break;
            }
        }
        if (handle == DOS_FILES) {
            setError(DOSERR_TOO_MANY_OPEN_FILES);
            return false;
        }
        /* We have a position in the main table now find one in the psp table */
        returnFileHandle = 0xffff & psp.findFreeFileEntry();
        if (returnFileHandle == 0xff) {
            setError(DOSERR_TOO_MANY_OPEN_FILES);
            return false;
        }
        /* Don't allow directories to be created */
        if ((attributes & DOSSystem.DOS_ATTR_DIRECTORY) != 0) {
            setError(DOSERR_ACCESS_DENIED);
            return false;
        }
        boolean foundit =
                (Files[handle] = Drives[drive].fileCreate(fullName.toString(), attributes)) != null;
        if (foundit) {
            Files[handle].setDrive(drive);
            Files[handle].addRef();
            psp.setFileHandle(returnFileHandle, handle);
            return true;
        } else {
            if (!pathExists(name))
                setError(DOSERR_PATH_NOT_FOUND);
            else
                setError(DOSERR_FILE_NOT_FOUND);
            return false;
        }
    }

    // 오픈한 file handle은 FileEntry에 저장
    // bool(string, uint8 , ref uint16 )
    public static boolean openFile1(String name, int flags) {
        flags &= 0xff;
        /* First check for devices */
        if (flags > 2)
            Log.logging(Log.LogTypes.FILES, Log.LogServerities.Error,
                    "Special file open command %X file %s", flags, name);
        else
            Log.logging(Log.LogTypes.FILES, Log.LogServerities.Normal,
                    "file open command %X file %s", flags, name);

        DOSPSP psp = new DOSPSP(DOS.getPSP());
        int attr = 0;
        byte devnum = findDevice(name);
        boolean device = (devnum != DOS_DEVICES);
        if (!device && tryFileAttr(name)) {
            attr = returnFileAttr();
            // DON'T ALLOW directories to be openened.(skip test if file is device).
            if ((attr & DOSSystem.DOS_ATTR_DIRECTORY) != 0
                    || (attr & DOSSystem.DOS_ATTR_VOLUME) != 0) {
                setError(DOSERR_ACCESS_DENIED);
                return false;
            }
        }

        /* First check if the name is correct */
        if (!makeFullName(name, DOSSystem.DOS_PATHLENGTH))
            return false;
        String fullName = returnedFullName;
        int drive = returnedFullNameDrive;

        int handle = 255;
        /* Check for a free file handle */
        int i;
        for (i = 0; i < DOS_FILES; i++) {
            if (Files[i] == null) {
                handle = i;
                break;
            }
        }
        if (handle == 255) {
            setError(DOSERR_TOO_MANY_OPEN_FILES);
            return false;
        }
        /* We have a position in the main table now find one in the psp table */
        returnFileHandle = 0xffff & psp.findFreeFileEntry();

        if (returnFileHandle == 0xff) {
            setError(DOSERR_TOO_MANY_OPEN_FILES);
            return false;
        }
        boolean exists = false;
        if (device) {
            Files[handle] = new DOSDevice(Devices[devnum]);
        } else {
            exists = (Files[handle] = Drives[drive].fileOpen(fullName.toString(), flags)) != null;
            if (exists)
                Files[handle].setDrive(drive);
        }
        if (exists || device) {
            Files[handle].addRef();
            psp.setFileHandle(returnFileHandle, handle);
            return true;
        } else {
            // Test if file exists, but opened in read-write mode (and writeprotected)
            if (((flags & 3) != DOSSystem.OPEN_READ)
                    && Drives[drive].fileExists(fullName.toString()))
                setError(DOSERR_ACCESS_DENIED);
            else {
                if (!pathExists(name))
                    setError(DOSERR_PATH_NOT_FOUND);
                else
                    setError(DOSERR_FILE_NOT_FOUND);
            }
            return false;
        }
    }

    // opened file handle -> returnfileHandle
    public static boolean openFile(String name, int flags) {
        flags &= 0xff;
        /* First check for devices */
        if (flags > 2)
            Log.logging(Log.LogTypes.FILES, Log.LogServerities.Error,
                    "Special file open command %X file %s", flags, name);
        else
            Log.logging(Log.LogTypes.FILES, Log.LogServerities.Normal,
                    "file open command %X file %s", flags, name);

        DOSPSP psp = new DOSPSP(DOS.getPSP());
        int attr = 0;
        byte devnum = findDevice(name);
        boolean device = (devnum != DOS_DEVICES);
        if (!device && tryFileAttr(name)) {
            attr = returnFileAttr();
            // DON'T ALLOW directories to be openened.(skip test if file is device).
            if ((attr & DOSSystem.DOS_ATTR_DIRECTORY) != 0
                    || (attr & DOSSystem.DOS_ATTR_VOLUME) != 0) {
                setError(DOSERR_ACCESS_DENIED);
                return false;
            }
        }

        if (!makeFullName(name, DOSSystem.DOS_PATHLENGTH)) {
            return false;
        }

        String fullName = returnedFullName;
        int drive = returnedFullNameDrive;

        int i = 0;
        int handle = 255;
        /* Check for a free file handle */
        for (i = 0; i < DOS_FILES; i++) {
            if (Files[i] == null) {
                handle = i;
                break;
            }
        }
        if (handle == 255) {
            setError(DOSERR_TOO_MANY_OPEN_FILES);
            return false;
        }
        /* We have a position in the main table now find one in the psp table */
        returnFileHandle = 0xffff & psp.findFreeFileEntry();

        if (returnFileHandle == 0xff) {
            setError(DOSERR_TOO_MANY_OPEN_FILES);
            return false;
        }
        boolean exists = false;
        if (device) {
            Files[handle] = new DOSDevice(Devices[devnum]);
        } else {
            exists = (Files[handle] = Drives[drive].fileOpen(fullName, flags)) != null;
            if (exists)
                Files[handle].setDrive(drive);
        }
        if (exists || device) {
            Files[handle].addRef();
            psp.setFileHandle(returnFileHandle, handle);
            return true;
        } else {
            // Test if file exists, but opened in read-write mode (and writeprotected)
            if (((flags & 3) != DOSSystem.OPEN_READ) && Drives[drive].fileExists(fullName))
                setError(DOSERR_ACCESS_DENIED);
            else {
                if (!pathExists(name))
                    setError(DOSERR_PATH_NOT_FOUND);
                else
                    setError(DOSERR_FILE_NOT_FOUND);
            }
            return false;
        }
    }

    public static int returnedFileExtendedStatus;
    public static int returnedFileExtendedHandle;

    // opened file handle -> returnedFileExtendedHandle
    // file open status -> returnedFileExtendedStatus
    public static boolean openFileExtended(String name, int flags, int createAttr, int action) {
        // FIXME: Not yet supported : Bit 13 of flags (int 0x24 on critical error)
        short result = 0;
        if (action == 0) {
            // always fail setting
            setError(DOSERR_FUNCTION_NUMBER_INVALID);
            return false;
        } else {
            if (((action & 0x0f) > 2) || ((action & 0xf0) > 0x10)) {
                // invalid action parameter
                setError(DOSERR_FUNCTION_NUMBER_INVALID);
                return false;
            }
        }
        if (openFile(name, flags & 0xff)) {
            returnedFileExtendedHandle = returnFileHandle;
            // File already exists
            switch (action & 0x0f) {
                case 0x00: // failed
                    setError(DOSERR_FILE_ALREADY_EXISTS);
                    return false;
                case 0x01: // file open (already done)
                    result = 1;
                    break;
                case 0x02: // replace
                    closeFile(returnFileHandle);
                    if (!createFile(name, createAttr))
                        return false;
                    result = 3;
                    returnedFileExtendedHandle = returnFileHandle;
                    break;
                default:
                    setError(DOSERR_FUNCTION_NUMBER_INVALID);
                    Support.exceptionExit("DOS: OpenFileExtended: Unknown action.");
                    break;
            }
        } else {
            // File doesn't exist
            if ((action & 0xf0) == 0) {
                // uses error code from failed open
                return false;
            }
            // Create File
            if (!createFile(name, createAttr)) {
                // uses error code from failed create
                return false;
            }
            returnedFileExtendedHandle = returnFileHandle;
            result = 2;
        }
        // success
        returnedFileExtendedStatus = result;
        return true;
    }


    public static boolean unlinkFile(String name) {
        if (!makeFullName(name, DOSSystem.DOS_PATHLENGTH))
            return false;
        String fullName = returnedFullName;
        int drive = returnedFullNameDrive;

        if (Drives[drive].fileUnlink(fullName)) {
            return true;
        } else {
            setError(DOSERR_FILE_NOT_FOUND);
            return false;
        }
    }

    public static int returnedFileAttr;

    public static boolean tryFileAttr(String name) {
        if (!makeFullName(name, DOSSystem.DOS_PATHLENGTH))
            return false;
        String fullName = returnedFullName;
        int drive = returnedFullNameDrive;

        if (Drives[drive].tryFileAttr(fullName)) {
            returnedFileAttr = Drives[drive].returnFileAttr();
            return true;
        } else {
            setError(DOSERR_FILE_NOT_FOUND);
            return false;
        }
    }

    public static int returnFileAttr() {
        return returnedFileAttr;
    }

    // this function does not change the file attributs
    // it just does some tests if file is available
    // returns false when using on cdrom (stonekeep)
    // public static boolean SetFileAttr(String name, short attr)
    public static boolean setFileAttr(String name, int attr) {
        if (!makeFullName(name, DOSSystem.DOS_PATHLENGTH))
            return false;
        String fullName = returnedFullName;
        int drive = returnedFullNameDrive;

        if (Drives[drive].getInfo().startsWith("CDRom ")
                || Drives[drive].getInfo().startsWith("isoDrive ")) {
            setError(DOSERR_ACCESS_DENIED);
            return false;
        }
        return Drives[drive].tryFileAttr(fullName.toString());
    }

    public static boolean canonicalize(String name, CStringPt big) {
        // TODO Add Better support for devices and shit but will it be needed i doubt it :)
        if (!makeFullName(name, DOSSystem.DOS_PATHLENGTH))
            return false;
        String fullName = returnedFullName;
        int drive = returnedFullNameDrive;
        big.set(0, (char) (drive + 'A'));
        big.set(1, ':');
        big.set(2, '\\');
        CStringPt.copy(fullName, CStringPt.clone(big, 3));

        return true;
    }

    public static boolean getFreeDiskSpace(int drive, DriveAllocationInfo buf) {
        if (drive == 0)
            drive = getDefaultDrive();
        else
            drive--;
        if ((drive >= DOS_DRIVES) || (Drives[drive] == null)) {
            setError(DOSERR_INVALID_DRIVE);
            return false;
        }

        return Drives[drive].allocationInfo(buf);
    }

    private static boolean duplicateEntry(int entry) {
        // Dont duplicate console handles
        /*
         * if (entry<=STDPRN) { newentry = entry; return true; };
         */
        int handle = realHandle(entry);
        if (handle >= DOS_FILES) {
            setError(DOSERR_INVALID_HANDLE);
            return false;
        }
        DOSFile file = Files[handle];
        if (file == null || !file.isOpen()) {
            setError(DOSERR_INVALID_HANDLE);
            return false;
        }
        DOSPSP psp = new DOSPSP(DOS.getPSP());
        int newEntry = psp.findFreeFileEntry();
        duplicatedNewEntry = newEntry;
        if (newEntry == 0xff) {
            setError(DOSERR_TOO_MANY_OPEN_FILES);
            return false;
        }
        file.addRef();
        psp.setFileHandle(newEntry, handle);
        return true;
    }

    public static int duplicatedNewEntry = 0;

    public static boolean forceDuplicateEntry(int entry, int newentry) {
        if (entry == newentry) {
            setError(DOSERR_INVALID_HANDLE);
            return false;
        }
        int orig = realHandle(entry);
        if (orig >= DOS_FILES) {
            setError(DOSERR_INVALID_HANDLE);
            return false;
        }
        if (Files[orig] == null || !Files[orig].isOpen()) {
            setError(DOSERR_INVALID_HANDLE);
            return false;
        }
        int newone = realHandle(newentry);
        if (newone < DOS_FILES && Files[newone] != null) {
            closeFile(newentry);
        }
        DOSPSP psp = new DOSPSP(DOS.getPSP());
        Files[orig].addRef();
        psp.setFileHandle(newentry, orig);
        return true;
    }

    // 이 메소드의 쓰임새를 봤을때 name을 굳이 string으로 변경할 필요없음
    public static boolean createTempFile(CStringPt name) {
        int namelen = name.length();
        CStringPt tempname = CStringPt.clone(name, namelen);
        if (namelen == 0) {
            // temp file created in root directory
            tempname.set(0, '\\');
            tempname.movePtToR1();
        } else {
            if ((name.get(namelen - 1) != '\\') && (name.get(namelen - 1) != '/')) {
                tempname.set(0, '\\');
                tempname.movePtToR1();
            }
        }
        DOS.ErrorCode = 0;
        /* add random crap to the end of the name and try to open */
        Random rnd = new Random();
        do {
            int i;
            for (i = 0; i < 8; i++) {
                tempname.set(i, (char) ((rnd.nextInt() % 26) + 'A'));
            }
            tempname.set(8, (char) 0);
        } while ((!createFile(name.toString(), 0))
                && (DOS.ErrorCode == DOSERR_FILE_ALREADY_EXISTS));
        if (DOS.ErrorCode != 0)
            return false;
        return true;

    }

    private static final char[] FCB_SEP = ":.;,=+".toCharArray();
    private static final char[] ILLEGAL = ":.;,=+ \t/\"[]<>|".toCharArray();

    private static boolean isValid(char inchr) {
        char[] ill = ILLEGAL;
        return (inchr > 0x1F) && (ArrayHelper.indexOf(ill, inchr) < 0);
    }

    private static final int PARSE_SEP_STOP = 0x01;
    private static final int PARSE_DFLT_DRIVE = 0x02;
    private static final int PARSE_BLNK_FNAME = 0x04;
    private static final int PARSE_BLNK_FEXT = 0x08;

    private static final int PARSE_RET_NOWILD = 0;
    private static final int PARSE_RET_WILD = 1;
    private static final int PARSE_RET_BADDRIVE = 0xff;

    private static final int Off_FCB_drive = 0;
    private static final int Off_FCB_name = 2;
    private static final int Off_FCB_ext = 11;
    private static final int Off_FCB_part = 11;
    private static final int Off_FCB_full = 0;
    private static final int Size_FCB_drive = 2;
    private static final int Size_FCB_name = 9;
    private static final int Size_FCB_ext = 4;
    private static final int Size_FCB_part = 15;
    private static final int Size_FCB_full = 15;
    private static final int Size_FCB_total = 15;

    // public struct CommandTail
    // {
    // public byte count; /* number of bytes returned */
    // public byte[] buffer = new byte[127]; /* the buffer itself */
    // }


    public static final byte CommandTail_Size_Count = 1;
    public static final byte CommandTail_Size_Buffer = 127;
    public static final short CommandTailSize = 128;
    public static final byte CommandTailOffCount = 0;
    public static final byte CommandTailOffBuffer = 1;

    public static int returnedFCBParseNameChange;

    // (parsed idx - str idx) -> returnedFCBParseNameChange
    public static int FCBParseName(int seg, int offset, int parser, CStringPt str) {
        int saveFCB = 1, checkext = 2;

        CStringPt string_begin = str;
        int ret = 0;
        if ((parser & PARSE_DFLT_DRIVE) == 0) {
            // default drive forced, this intentionally invalidates an extended FCB
            Memory.writeB(Memory.physMake(seg, offset), 0);
        }
        DOSFCB fcb = new DOSFCB(seg, offset, false); // always a non-extended FCB
        boolean hasdrive, hasname, hasext, finished;
        hasdrive = hasname = hasext = finished = false;
        int index = 0;
        char fill = ' ';
        /* First get the old data from the fcb */
        // char[] fcb_name = new char[Size_FCB_total];
        CStringPt fcbNamePt = CStringPt.create(Size_FCB_total);
        /* Get the old information from the previous fcb */
        fcb.getName(fcbNamePt);
        fcbNamePt.set(Off_FCB_drive + 0,
                (char) (fcbNamePt.get(Off_FCB_drive + 0) - (char) ('A' - 1)));
        fcbNamePt.set(Off_FCB_drive + 1, (char) 0);
        fcbNamePt.set(Off_FCB_name + 8, (char) 0);
        fcbNamePt.set(Off_FCB_ext + 3, (char) 0);
        /* Strip of the leading sepetaror */
        if ((parser & PARSE_SEP_STOP) != 0 && str.get() != 0) { // ignore leading seperator
            if (ArrayHelper.indexOf(FCB_SEP, str.get()) >= 0)
                str.movePtToR1();
        }

        /* strip leading spaces */
        while ((str.get() == ' ') || (str.get() == '\t'))
            str.movePtToR1();
        /* Check for a drive */
        if (str.get(1) == ':') {
            fcbNamePt.set(Off_FCB_drive + 0, (char) 0);
            hasdrive = true;
            if (Character.isLetter(str.get(0))
                    && Drives[Character.toUpperCase(str.get(0)) - 'A'] != null) {
                fcbNamePt.set(Off_FCB_drive + 0,
                        (char) (Character.toUpperCase(str.get(0)) - 'A' + 1));
            } else
                ret = 0xff;
            str.moveR(2);
        }

        int nextStep = saveFCB;// default
        while (true) {
            /* Special checks for . and .. */
            if (str.get(0) == '.') {
                str.movePtToR1();
                if (str.get(0) == 0) {
                    hasname = true;
                    ret = PARSE_RET_NOWILD;
                    CStringPt.copy(".       ", CStringPt.clone(fcbNamePt, Off_FCB_name));
                    // goto savefcb;
                    nextStep = saveFCB;
                    break;
                }
                if (str.get(1) == '.' && str.get(1) == 0) {
                    str.movePtToR1();
                    hasname = true;
                    ret = PARSE_RET_NOWILD;
                    CStringPt.copy("..      ", CStringPt.clone(fcbNamePt, Off_FCB_name));
                    // goto savefcb;
                    nextStep = saveFCB;
                    break;
                }
                // goto checkext;
                nextStep = checkext;
                break;
            }
            /* Copy the name */
            hasname = true;
            finished = false;
            fill = ' ';
            index = 0;
            while (index < 8) {
                if (!finished) {
                    if (str.get(0) == '*') {
                        fill = '?';
                        fcbNamePt.set(Off_FCB_name + index, '?');
                        if (ret == 0)
                            ret = 1;
                        finished = true;
                    } else if (str.get(0) == '?') {
                        fcbNamePt.set(Off_FCB_name + index, '?');
                        if (ret == 0)
                            ret = 1;
                    } else if (isValid(str.get(0))) {
                        fcbNamePt.set(Off_FCB_name + index,
                                (char) (Character.toUpperCase(str.get(0))));
                    } else {
                        finished = true;
                        continue;
                    }
                    str.movePtToR1();
                } else {
                    fcbNamePt.set(Off_FCB_name + index, fill);
                }
                index++;
            }
            if (!(str.get(0) == '.')) {
                // goto savefcb;
                nextStep = saveFCB;
                break;
            }
            str.movePtToR1();
            nextStep = checkext;
            break;
        }
        // checkext:
        if (nextStep == checkext) {
            /* Copy the extension */
            hasext = true;
            finished = false;
            fill = ' ';
            index = 0;
            while (index < 3) {
                if (!finished) {
                    if (str.get(0) == '*') {
                        fill = '?';
                        fcbNamePt.set(Off_FCB_ext + index, '?');
                        finished = true;
                    } else if (str.get(0) == '?') {
                        fcbNamePt.set(Off_FCB_ext + index, '?');
                        if (ret == 0)
                            ret = 1;
                    } else if (isValid(str.get(0))) {
                        fcbNamePt.set(Off_FCB_ext + index,
                                (char) (Character.toUpperCase(str.get(0))));
                    } else {
                        finished = true;
                        continue;
                    }
                    str.movePtToR1();
                } else {
                    fcbNamePt.set(Off_FCB_ext + index, fill);
                }
                index++;
            }
            nextStep++;
        }
        // savefcb:
        if (nextStep == saveFCB) {
            if (!hasdrive & (parser & PARSE_DFLT_DRIVE) == 0)
                fcbNamePt.set(Off_FCB_drive + 0, (char) 0);
            if (!hasname & (parser & PARSE_BLNK_FNAME) == 0)
                CStringPt.copy("        ", CStringPt.clone(fcbNamePt, Off_FCB_name));
            if (!hasext & (parser & PARSE_BLNK_FEXT) == 0)
                CStringPt.copy("   ", CStringPt.clone(fcbNamePt, Off_FCB_ext));
            fcb.setName(0xff & fcbNamePt.get(Off_FCB_drive + 0),
                    CStringPt.clone(fcbNamePt, Off_FCB_name),
                    CStringPt.clone(fcbNamePt, Off_FCB_ext));
            returnedFCBParseNameChange = CStringPt.diff(str, string_begin);
        }
        return ret;
    }

    private static void doDTAExtendName(CStringPt name, CStringPt filename, CStringPt ext) {
        CStringPt find = name.positionOf('.');
        if (!find.isEmpty()) {
            CStringPt.copy(CStringPt.clone(find, 1), ext);
            find.set((char) 0);
        } else
            ext.set(0, (char) 0);
        CStringPt.copy(name, filename);
        for (int i = name.length(); i < 8; i++)
            filename.set(i, ' ');
        filename.set(8, (char) 0);
        for (int i = ext.length(); i < 3; i++)
            ext.set(i, ' ');
        ext.set(3, (char) 0);
    }

    private static void saveFindResult(DOSFCB findFCB) {

        DOSDTA findDTA = new DOSDTA(DOS.tables.TempDTA);
        CStringPt name = CStringPt.create(DOSSystem.DOS_NAMELENGTH_ASCII);
        CStringPt file_name = CStringPt.create(9);
        CStringPt ext = CStringPt.create(4);
        findDTA.getResultName(file_name);
        int size = findDTA.getResultSize();
        int date = findDTA.getResultDate();
        int time = findDTA.getResultTime();
        int attr = findDTA.getResultAttr();
        int drive = findFCB.getDrive() + 1;
        /* Create a correct file and extention */
        doDTAExtendName(name, file_name, ext);
        DOSFCB fcb = new DOSFCB(Memory.realSeg(DOS.getDTA()), Memory.realOff(DOS.getDTA()));// TODO
        fcb.create(findFCB.extended());
        fcb.setName(0xff & drive, file_name, ext);
        fcb.setAttr(attr); /* Only adds attribute if fcb is extended */
        fcb.setSizeDateTime(size, date, time);

    }

    // private static boolean doFCBCreate(short seg, short offset)
    private static boolean doFCBCreate(int seg, int offset) {
        DOSFCB fcb = new DOSFCB(seg, offset);
        CStringPt shortname = CStringPt.create(DOSSystem.DOS_FCBNAME);
        fcb.getName(shortname);
        if (!createFile(shortname.toString(), DOSSystem.DOS_ATTR_ARCHIVE))
            return false;
        fcb.openFile(returnFileHandle);
        return true;
    }

    // private static boolean doFCBOpen(short seg, short offset)
    private static boolean doFCBOpen(int seg, int offset) {
        DOSFCB fcb = new DOSFCB(seg, offset);
        CStringPt shortname = CStringPt.create(DOSSystem.DOS_FCBNAME);
        int handle = 0;
        fcb.getName(shortname);

        /* First check if the name is correct */
        if (!makeFullName(shortname.toString(), DOSSystem.DOS_PATHLENGTH))
            return false;
        String fullName = returnedFullName;
        int drive = returnedFullNameDrive;

        /* Check, if file is already opened */
        for (int i = 0; i < DOS_FILES; i++) {
            DOSPSP psp = new DOSPSP(DOS.getPSP());
            if (Files[i] != null && Files[i].isOpen() && Files[i].isName(fullName)) {
                handle = psp.findEntryByHandle(i);
                if (handle == 0xFF) {
                    // This shouldnt happen
                    Log.logging(Log.LogTypes.FILES, Log.LogServerities.Error,
                            "DOS: File %s is opened but has no psp entry.", shortname.toString());
                    return false;
                }
                fcb.openFile(handle);
                return true;
            }
        }
        if (!openFile(shortname.toString(), DOSSystem.OPEN_READWRITE))
            return false;
        handle = returnFileHandle;
        fcb.openFile(handle);
        return true;
    }

    // private static boolean doFCBClose(short seg, short offset)
    private static boolean doFCBClose(int seg, int offset) {
        DOSFCB fcb = new DOSFCB(seg, offset);
        if (!fcb.valid())
            return false;
        int fhandle = fcb.closeFile();
        closeFile(fhandle);
        return true;
    }

    // private static boolean doFCBFindFirst(short seg, short offset)
    private static boolean doFCBFindFirst(int seg, int offset) {
        DOSFCB fcb = new DOSFCB(seg, offset);
        int old_dta = DOS.getDTA();
        DOS.setDTA(DOS.tables.TempDTA);
        CStringPt name = CStringPt.create(DOSSystem.DOS_FCBNAME);
        fcb.getName(name);
        int attr = DOSSystem.DOS_ATTR_ARCHIVE;
        // todo : ref attr -> attr을 인자로 전달해도 영향을 주지 않음, 생략처리
        attr = fcb.getAttr(); /* Gets search attributes if extended */
        boolean ret = findFirst(name.toString(), attr, true);
        DOS.setDTA(old_dta);
        if (ret)
            saveFindResult(fcb);
        return ret;
    }

    // private static boolean doFCBFindNext(short seg, short offset)
    private static boolean doFCBFindNext(int seg, int offset) {
        DOSFCB fcb = new DOSFCB(seg, offset);
        int old_dta = DOS.getDTA();
        DOS.setDTA(DOS.tables.TempDTA);
        boolean ret = findNext();
        DOS.setDTA(old_dta);
        if (ret)
            saveFindResult(fcb);
        return ret;
    }

    // private static byte doFCBRead(short seg, short offset, short recNo)
    private static int doFCBRead(int seg, int offset, int recNo) {
        DOSFCB fcb = new DOSFCB(seg, offset);
        int fHandle = 0;// uint8
        int curRec = 0;// uint8
        int curBlock = 0, recSize = 0;
        fHandle = fcb.getSeqDataFileHandle();
        recSize = fcb.getSeqDataFileSize();
        curBlock = fcb.getBlock();
        curRec = fcb.getRecord();
        long pos = ((curBlock * 128L) + curRec) * recSize;
        if ((pos = seekFile(fHandle, pos, DOSSystem.DOS_SEEK_SET)) < 0)
            return FCB_READ_NODATA;
        int toread = recSize;
        if (!readFile(fHandle, dosCopyBuf, 0, toread))
            return FCB_READ_NODATA;
        toread = readFileSize;
        if (toread == 0)
            return FCB_READ_NODATA;
        if (toread < recSize) { // Zero pad copybuffer to rec_size
            int i = toread;
            while (i < recSize)
                dosCopyBuf[i++] = 0;
        }
        Memory.blockWrite(Memory.real2Phys(DOS.getDTA()) + recNo * recSize, dosCopyBuf, 0, recSize);
        if (++curRec > 127) {
            curBlock++;
            curRec = 0;
        }
        fcb.setRecord(curBlock, curRec);
        if (toread == recSize)
            return FCB_SUCCESS;
        if (toread == 0)
            return FCB_READ_NODATA;
        return FCB_READ_PARTIAL;
    }

    // private static byte doFCBWrite(short seg, short offset, short recNo)
    private static int doFCBWrite(int seg, int offset, int recNo) {
        DOSFCB fcb = new DOSFCB(seg, offset);
        int fHandle = 0;;// uint8
        int curRec = 0;;// uint8
        int curBlock = 0, recSize = 0;
        fHandle = fcb.getSeqDataFileHandle();
        recSize = fcb.getSeqDataFileSize();
        curBlock = fcb.getBlock();
        curRec = fcb.getRecord();
        long pos = ((curBlock * 128L) + curRec) * recSize;
        if ((pos = seekFile(fHandle, pos, DOSSystem.DOS_SEEK_SET)) < 0)
            return FCB_ERR_WRITE;
        Memory.blockRead(Memory.real2Phys(DOS.getDTA()) + recNo * recSize, dosCopyBuf, 0, recSize);
        int towrite = recSize;
        if (!writeFile(fHandle, dosCopyBuf, 0, towrite))
            return FCB_ERR_WRITE;
        towrite = DOSMain.WrittenSize;
        long size = 0;
        int date = 0;
        int time = 0;
        size = fcb.getSize();
        date = fcb.getDate();
        time = fcb.getTime();
        if (pos + towrite > size)
            size = pos + towrite;
        // time doesn't keep track of endofday
        date = packDate(DOS.Date.Year, DOS.Date.Month, DOS.Date.Day);
        int ticks = Memory.readD(BIOS.BIOS_TIMER);
        int seconds = (ticks * 10) / 182;
        int hour = 0xffff & (seconds / 3600);
        int min = 0xffff & ((seconds % 3600) / 60);
        int sec = 0xffff & (seconds % 60);
        time = packTime(hour, min, sec);
        int temp = realHandle(fHandle);
        Files[temp].Time = time;
        Files[temp].Date = date;
        fcb.setSizeDateTime((int) size, date, time);
        if (++curRec > 127) {
            curBlock++;
            curRec = 0;
        }
        fcb.setRecord(curBlock, curRec);
        return FCB_SUCCESS;
    }

    private static int doFCBIncreaseSize(int seg, int offset) {
        DOSFCB fcb = new DOSFCB(seg, offset);
        int fHandle = 0;// uint8
        int curRec = 0;// uint8
        int curBlock = 0, recSize = 0;
        fHandle = fcb.getSeqDataFileHandle();
        recSize = fcb.getSeqDataFileSize();
        curBlock = fcb.getBlock();
        curRec = fcb.getRecord();
        long pos = ((curBlock * 128L) + curRec) * recSize;
        if ((pos = seekFile(fHandle, pos, DOSSystem.DOS_SEEK_SET)) < 0)
            return FCB_ERR_WRITE;
        int towrite = 0;
        if (!writeFile(fHandle, dosCopyBuf, 0, towrite))
            return FCB_ERR_WRITE;
        towrite = WrittenSize;
        long size = 0;
        int date = 0;
        int time = 0;
        size = fcb.getSize();
        date = fcb.getDate();
        time = fcb.getTime();
        if (pos + towrite > size)
            size = pos + towrite;
        // time doesn't keep track of endofday
        date = packDate(DOS.Date.Year, DOS.Date.Month, DOS.Date.Day);
        int ticks = Memory.readD(BIOS.BIOS_TIMER);
        int seconds = (ticks * 10) / 182;
        int hour = 0xffff & (seconds / 3600);
        int min = 0xffff & ((seconds % 3600) / 60);
        int sec = 0xffff & (seconds % 60);
        time = packTime(hour, min, sec);
        int temp = realHandle(fHandle);
        Files[temp].Time = time;
        Files[temp].Date = date;
        fcb.setSizeDateTime((int) size, date, time);
        fcb.setRecord(curBlock, curRec);
        return FCB_SUCCESS;
    }

    // private static byte doFCBRandomRead(short seg, short offset, short numRec, boolean restore)
    private static int doFCBRandomRead(int seg, int offset, int numRec, boolean restore) {
        /*
         * if restore is true :random read else random blok read. random read updates old block and
         * old record to reflect the random data before the read!!!!!!!!! and the random data is not
         * updated! (user must do this) Random block read updates these fields to reflect the state
         * after the read!
         */

        /*
         * BUG: numRec should return the amount of records read! Not implemented yet as I'm unsure
         * how to count on error states (partial/failed)
         */

        DOSFCB fcb = new DOSFCB(seg, offset);
        int random = 0;
        int oldBlock = 0;// uint16
        int oldRec = 0;// uint8
        int error = 0;

        /* Set the correct record from the random data */
        random = fcb.getRandom();
        fcb.setRecord(0xffff & (random / 128), random & 127);
        if (restore) {
            // store this for after the read.
            oldBlock = fcb.getBlock();
            oldRec = fcb.getRecord();
        }
        // Read records
        for (int i = 0; i < numRec; i++) {
            error = doFCBRead(seg, offset, i);
            if (error != 0x00)
                break;
        }
        int newBlock = 0;// uint16
        int newRec = 0;// uint8
        newBlock = fcb.getBlock();
        newRec = fcb.getRecord();
        if (restore)
            fcb.setRecord(oldBlock, oldRec);
        /* Update the random record pointer with new position only when restore is false */
        if (!restore)
            fcb.setRandom(newBlock * 128 + newRec);
        return error;
    }

    private static int doFCBRandomWrite(int seg, int offset, int numRec, boolean restore) {
        /* see FCB_RandomRead */
        DOSFCB fcb = new DOSFCB(seg, offset);
        int random = 0;
        int oldBlock = 0;// uint16
        int oldRec = 0;// uint8
        int error = 0;

        /* Set the correct record from the random data */
        random = fcb.getRandom();
        fcb.setRecord(0xffff & (random / 128), random & 127);
        if (restore) {
            oldBlock = fcb.getBlock();
            oldRec = fcb.getRecord();
        }
        if (numRec > 0) {
            /* Write records */
            for (int i = 0; i < numRec; i++) {
                // dos_fcbwrite return 0 false when true...
                error = doFCBWrite(seg, offset, i);

                if (error != 0x00)
                    break;
            }
        } else {
            doFCBIncreaseSize(seg, offset);
        }
        int newBlock = 0;// uint16
        int newRec = 0;// uint8
        newBlock = fcb.getBlock();
        newRec = fcb.getRecord();
        if (restore)
            fcb.setRecord(oldBlock, oldRec);
        /* Update the random record pointer with new position only when restore is false */
        if (!restore)
            fcb.setRandom(newBlock * 128 + newRec);
        return error;
    }

    private static boolean doFCBGetFileSize(int seg, int offset) {
        CStringPt shortname = CStringPt.create(DOSSystem.DOS_PATHLENGTH);
        int entry = 0;
        int handle;
        int recSize = 0;
        DOSFCB fcb = new DOSFCB(seg, offset);
        fcb.getName(shortname);
        if (!openFile(shortname.toString(), DOSSystem.OPEN_READ))
            return false;
        entry = 0xffff & returnFileHandle;
        handle = realHandle(entry);
        long size = 0;
        size = Files[handle].seek(size, DOSSystem.DOS_SEEK_END);
        closeFile(entry);
        handle = fcb.getSeqDataFileHandle();
        recSize = fcb.getSeqDataFileSize();
        int random = (int) (size / recSize);
        if ((size % recSize) != 0)
            random++;
        fcb.setRandom(random);
        return true;
    }

    // private static boolean doFCBDeleteFile(short seg, short offset) {
    private static boolean doFCBDeleteFile(int seg, int offset) {
        /*
         * FCB DELETE honours wildcards. it will return true if one or more files get deleted. To
         * get this: the dta is set to temporary dta in which found files are stored. This can not
         * be the tempdta as that one is used by fcbfindfirst
         */
        int old_dta = DOS.getDTA();
        DOS.setDTA(DOS.tables.TempDTA_FCBDelete);
        DOSFCB fcb = new DOSFCB(Memory.realSeg(DOS.getDTA()), Memory.realOff(DOS.getDTA()));
        boolean nextfile = false;
        boolean return_value = false;
        nextfile = doFCBFindFirst(seg, offset);
        while (nextfile) {
            CStringPt shortname = CStringPt.create(DOSSystem.DOS_FCBNAME);// = { 0 };
            shortname.set(0, (char) 0);
            fcb.getName(shortname);
            boolean res = unlinkFile(shortname.toString());
            if (!return_value && res)
                return_value = true; // at least one file deleted
            nextfile = doFCBFindNext(seg, offset);
        }
        DOS.setDTA(old_dta); /* Restore dta */
        return return_value;
    }

    // private static boolean doFCBRenameFile(short seg, short offset) {
    private static boolean doFCBRenameFile(int seg, int offset) {
        DOSFCB fcbold = new DOSFCB(seg, offset);
        DOSFCB fcbnew = new DOSFCB(seg, offset + 16);
        CStringPt oldname = CStringPt.create(DOSSystem.DOS_FCBNAME);
        CStringPt newname = CStringPt.create(DOSSystem.DOS_FCBNAME);
        fcbold.getName(oldname);
        fcbnew.getName(newname);
        return rename(oldname.toString(), newname.toString());
    }

    private static void doFCBSetRandomRecord(int seg, int offset) {
        DOSFCB fcb = new DOSFCB(seg, offset);
        int block = 0;// uint16
        int rec = 0;// uint8
        block = fcb.getBlock();
        rec = fcb.getRecord();
        fcb.setRandom(block * 128 + rec);
    }


    public static boolean fileExists(String name) {
        if (!makeFullName(name, DOSSystem.DOS_PATHLENGTH))
            return false;
        String fullName = returnedFullName;
        int drive = returnedFullNameDrive;
        return Drives[drive].fileExists(fullName);
    }

    private static boolean getAllocationInfo(int drive, DriveAllocationInfo alloc) {
        if (drive == 0)
            drive = getDefaultDrive();
        else
            drive--;
        if (drive >= DOS_DRIVES || Drives[drive] == null)
            return false;
        alloc.freeClusters = 0;
        Drives[drive].allocationInfo(alloc);
        Register.segSet16(Register.SEG_NAME_DS, Memory.realSeg(DOS.tables.MediaId));
        Register.setRegBX(Memory.realOff(DOS.tables.MediaId + drive * 2));
        return true;
    }

    public static boolean setDrive(int drive) {
        if (Drives[drive] != null) {
            setDefaultDrive(drive);
            return true;
        } else {
            return false;
        }
    }

    private static int returnedGetFileOTime, returnedGetFileODate;

    // (short, ref uint16, ref uint16)
    private static boolean tryGetFileDate(int entry) {
        int handle = realHandle(entry);
        if (handle >= DOS_FILES) {
            setError(DOSERR_INVALID_HANDLE);
            return false;
        }
        DOSFile file = Files[handle];
        if (file == null || !file.isOpen()) {
            setError(DOSERR_INVALID_HANDLE);
            return false;
        }
        if (!file.updateDateTimeFromHost()) {
            setError(DOSERR_INVALID_HANDLE);
            return false;
        }
        returnedGetFileOTime = file.Time;
        returnedGetFileODate = file.Date;
        return true;
    }

    public static void setupFiles() {
        /* Setup the File Handles */
        int i;
        for (i = 0; i < DOS_FILES; i++) {
            Files[i] = null;
        }
        /* Setup the Virtual Disk System */
        for (i = 0; i < DOS_DRIVES; i++) {
            Drives[i] = null;
        }
        Drives[25] = new VirtualDrive();
    }

    // -- #endregion

    /*-------------------------------------- end DOSFiles ---------------------------------------*/

    /*------------------------------------ begin DOSExecute -------------------------------------*/
    public static CStringPt RunningProgram = CStringPt.create("DOSBOX");

    // --------------------------------- struct EXE_Header start ---------------------------------//
    private static final int Size_EXE_Header_signature = 2;
    private static final int Size_EXE_Header_extrabytes = 2;
    private static final int Size_EXE_Header_pages = 2;
    private static final int Size_EXE_Header_relocations = 2;
    private static final int Size_EXE_Header_headersize = 2;
    private static final int Size_EXE_Header_minmemory = 2;
    private static final int Size_EXE_Header_maxmemory = 2;
    private static final int Size_EXE_Header_initSS = 2;
    private static final int Size_EXE_Header_initSP = 2;
    private static final int Size_EXE_Header_checksum = 2;
    private static final int Size_EXE_Header_initIP = 2;
    private static final int Size_EXE_Header_initCS = 2;
    private static final int Size_EXE_Header_reloctable = 2;
    private static final int Size_EXE_Header_overlay = 2;

    private static final int Off_EXE_Header_signature = 0;
    private static final int Off_EXE_Header_extrabytes = 2;
    private static final int Off_EXE_Header_pages = 4;
    private static final int Off_EXE_Header_relocations = 6;
    private static final int Off_EXE_Header_headersize = 8;
    private static final int Off_EXE_Header_minmemory = 10;
    private static final int Off_EXE_Header_maxmemory = 12;
    private static final int Off_EXE_Header_initSS = 14;
    private static final int Off_EXE_Header_initSP = 16;
    private static final int Off_EXE_Header_checksum = 18;
    private static final int Off_EXE_Header_initIP = 20;
    private static final int Off_EXE_Header_initCS = 22;
    private static final int Off_EXE_Header_reloctable = 24;
    private static final int Off_EXE_Header_overlay = 26;

    private static final int Size_EXE_Header = 28;

    // --------------------------------- struct EXE_Header end ---------------------------------//

    private static final int MAGIC1 = 0x5a4d;
    private static final int MAGIC2 = 0x4d5a;
    private static final int MAXENV = 32768;
    private static final int ENV_KEEPFREE = 83; /* keep unallocated by environment variables */
    private static final int LOADNGO = 0;
    private static final int LOAD = 1;
    private static final int OVERLAY = 3;



    private static void saveRegisters() {
        Register.setRegSP(Register.getRegSP() - 18);
        Memory.writeW(Register.segPhys(Register.SEG_NAME_SS) + Register.getRegSP() + 0,
                Register.getRegAX());
        Memory.writeW(Register.segPhys(Register.SEG_NAME_SS) + Register.getRegSP() + 2,
                Register.getRegCX());
        Memory.writeW(Register.segPhys(Register.SEG_NAME_SS) + Register.getRegSP() + 4,
                Register.getRegDX());
        Memory.writeW(Register.segPhys(Register.SEG_NAME_SS) + Register.getRegSP() + 6,
                Register.getRegBX());
        Memory.writeW(Register.segPhys(Register.SEG_NAME_SS) + Register.getRegSP() + 8,
                Register.getRegSI());
        Memory.writeW(Register.segPhys(Register.SEG_NAME_SS) + Register.getRegSP() + 10,
                Register.getRegDI());
        Memory.writeW(Register.segPhys(Register.SEG_NAME_SS) + Register.getRegSP() + 12,
                Register.getRegBP());
        Memory.writeW(Register.segPhys(Register.SEG_NAME_SS) + Register.getRegSP() + 14,
                Register.segValue(Register.SEG_NAME_DS));
        Memory.writeW(Register.segPhys(Register.SEG_NAME_SS) + Register.getRegSP() + 16,
                Register.segValue(Register.SEG_NAME_ES));
    }

    private static void restoreRegisters() {
        Register.setRegAX(
                Memory.readW(Register.segPhys(Register.SEG_NAME_SS) + Register.getRegSP() + 0));
        Register.setRegCX(
                Memory.readW(Register.segPhys(Register.SEG_NAME_SS) + Register.getRegSP() + 2));
        Register.setRegDX(
                Memory.readW(Register.segPhys(Register.SEG_NAME_SS) + Register.getRegSP() + 4));
        Register.setRegBX(
                Memory.readW(Register.segPhys(Register.SEG_NAME_SS) + Register.getRegSP() + 6));
        Register.setRegSI(
                Memory.readW(Register.segPhys(Register.SEG_NAME_SS) + Register.getRegSP() + 8));
        Register.setRegDI(
                Memory.readW(Register.segPhys(Register.SEG_NAME_SS) + Register.getRegSP() + 10));
        Register.setRegBP(
                Memory.readW(Register.segPhys(Register.SEG_NAME_SS) + Register.getRegSP() + 12));
        Register.segSet16(Register.SEG_NAME_DS,
                Memory.readW(Register.segPhys(Register.SEG_NAME_SS) + Register.getRegSP() + 14));
        Register.segSet16(Register.SEG_NAME_ES,
                Memory.readW(Register.segPhys(Register.SEG_NAME_SS) + Register.getRegSP() + 16));
        Register.setRegSP(Register.getRegSP() + 18);
    }

    private static CStringPt name;// DOS_UpdatePSPName 전용

    private static void updatePSPName() {
        DOSMCB mcb = new DOSMCB(DOS.getPSP() - 1);
        name = CStringPt.create(9);
        mcb.getFileName(name);
        if (name.length() == 0)
            name = CStringPt.create("DOSBOX");
        RunningProgram = name;
        GUIPlatform.gfx.setTitle(-1, -1, false);
    }

    // uint16, bool, byte
    // public static void Terminate(int pspSeg, boolean tsr, byte exitCode) {
    public static void terminate(int pspSeg, boolean tsr, int exitCode) {

        DOS.ReturnCode = exitCode;
        DOS.ReturnMode = (tsr) ? DOSMain.RETURN_TYPE_TSR : DOSMain.RETURN_TYPE_EXIT;

        DOSPSP curPSP = new DOSPSP(pspSeg);
        if (pspSeg == curPSP.getParent())
            return;
        /* Free Files owned by process */
        if (!tsr)
            curPSP.closeFiles();

        /* Get the termination address */
        int old22 = curPSP.getINT22();
        /* Restore vector 22,23,24 */
        curPSP.restoreVectors();
        /* Set the parent PSP */
        DOS.setPSP(curPSP.getParent());
        DOSPSP parentPSP = new DOSPSP(curPSP.getParent());

        /* Restore the SS:SP to the previous one */
        Register.segSet16(Register.SEG_NAME_SS, Memory.realSeg(parentPSP.getStack()));
        Register.setRegSP(Memory.realOff(parentPSP.getStack()));
        /* Restore the old CS:IP from int 22h */
        restoreRegisters();
        /* Set the CS:IP stored in int 0x22 back on the stack */
        Memory.writeW(Register.segPhys(Register.SEG_NAME_SS) + Register.getRegSP() + 0,
                Memory.realOff(old22));
        Memory.writeW(Register.segPhys(Register.SEG_NAME_SS) + Register.getRegSP() + 2,
                Memory.realSeg(old22));
        /*
         * set IOPL=3 (Strike Commander), nested task set, interrupts enabled, test flags cleared
         */
        Memory.writeW(Register.segPhys(Register.SEG_NAME_SS) + Register.getRegSP() + 4, 0x7202);
        // Free memory owned by process
        if (!tsr)
            freeProcessMemory(pspSeg);
        updatePSPName();

        if (((CPU.AutoDetermineMode >>> (0xff & CPU.AutoDetermineShift)) == 0) || (CPU.Block.PMode))
            return;

        CPU.AutoDetermineMode >>>= (0xff & CPU.AutoDetermineShift);
        if ((CPU.AutoDetermineMode & CPU.AutoDetermineCycles) != 0) {
            CPU.CycleAutoAdjust = false;
            CPU.CycleLeft = 0;
            CPU.Cycles = 0;
            CPU.CycleMax = CPU.OldCycleMax;
            GUIPlatform.gfx.setTitle(CPU.OldCycleMax, -1, false);
        } else {
            GUIPlatform.gfx.setTitle(-1, -1, false);
        }


        return;
    }

    private static int returnedMakeEnvSeg;

    private static boolean makeEnv(String name, int segment) {
        /* If segment to copy environment is 0 copy the caller's environment */
        DOSPSP psp = new DOSPSP(DOS.getPSP());
        int envRead, envWrite;
        int envSize = 1;
        boolean parentEnv = true;

        if (segment == 0) {
            if (psp.getEnvironment() == 0)
                parentEnv = false; // environment seg=0
            envRead = Memory.physMake(psp.getEnvironment(), 0);
        } else {
            if (segment == 0)
                parentEnv = false; // environment seg=0
            envRead = Memory.physMake(segment, 0);
        }

        if (parentEnv) {
            for (envSize = 0;; envSize++) {
                if (envSize >= MAXENV - ENV_KEEPFREE) {
                    setError(DOSERR_ENVIRONMENT_INVALID);
                    return false;
                }
                if (Memory.readW(envRead + envSize) == 0)
                    break;
            }
            envSize += 2; /* account for trailing \0\0 */
        }
        int size = doLong2Para(envSize + ENV_KEEPFREE);
        if (!tryAllocateMemory(size))
            return false;
        returnedMakeEnvSeg = segment = returnedAllocateMemorySeg;
        size = returnedAllocateMemoryBlock;
        envWrite = Memory.physMake(segment, 0);
        if (parentEnv) {
            Memory.blockCopy(envWrite, envRead, envSize);
            // mem_memcpy(envwrite,envread,envsize);
            envWrite += envSize;
        } else {
            Memory.writeB(envWrite++, 0);
        }
        Memory.writeW(envWrite, 1);
        envWrite += 2;
        CStringPt namebuf = CStringPt.create(DOSSystem.DOS_PATHLENGTH);
        if (canonicalize(name, namebuf)) {
            Memory.blockWrite(envWrite, namebuf);
            return true;
        } else
            return false;
    }

    public static boolean newPSP(int segment, int size) {
        DOSPSP psp = new DOSPSP(segment);
        psp.makeNew(size);
        int parentPSPSeg = psp.getParent();// uint16
        DOSPSP pspParent = new DOSPSP(parentPSPSeg);
        psp.copyFileTable(pspParent, false);
        // copy command line as well (Kings Quest AGI -cga switch)
        psp.setCommandTail(Memory.realMake(parentPSPSeg, 0x80));
        return true;
    }

    // public static boolean ChildPSP(short segment, short size) {
    public static boolean childPSP(int segment, int size) {
        DOSPSP psp = new DOSPSP(segment);
        psp.makeNew(size);
        int parentPSPSeg = psp.getParent();// uint16
        DOSPSP pspParent = new DOSPSP(parentPSPSeg);
        psp.copyFileTable(pspParent, true);
        psp.setCommandTail(Memory.realMake(parentPSPSeg, 0x80));
        psp.setFCB1(Memory.realMake(parentPSPSeg, 0x5c));
        psp.setFCB2(Memory.realMake(parentPSPSeg, 0x6c));
        psp.setEnvironment(pspParent.getEnvironment());
        psp.setSize(size);
        return true;
    }

    // (uint16, uint16, uint16)
    private static void setupPSP(int pspSeg, int memSize, int envSeg) {
        /* Fix the PSP for psp and environment MCB's */
        DOSMCB mcb = new DOSMCB(pspSeg - 1);
        mcb.setPSPSeg(pspSeg);
        mcb.setSegPt(envSeg - 1);
        mcb.setPSPSeg(pspSeg);

        DOSPSP psp = new DOSPSP(pspSeg);
        psp.makeNew(memSize);
        psp.setEnvironment(envSeg);

        /* Copy file handles */
        DOSPSP oldpsp = new DOSPSP(DOS.getPSP());
        psp.copyFileTable(oldpsp, true);

    }

    // (uint16, DOSParamBlock)
    private static void setupCMDLine(int pspSeg, DOSParamBlock block) {
        DOSPSP psp = new DOSPSP(pspSeg);
        // if cmdtail==0 it will inited as empty in SetCommandTail
        psp.setCommandTail(block.Exec.CmdTail);
    }

    // public static boolean Execute(String name, int blockPt, byte flags) {
    public static boolean execute(String name, int blockPt, int flags) {
        byte[] head = new byte[Size_EXE_Header];
        int i;
        int fHandle = 0;
        int len;
        long pos;
        int pspSeg = 0, envSeg = 0, loadSeg, memSize = 0, readSize;
        int loadAddress;
        byte[] relocpt = new byte[4];
        int headerSize = 0, imagesize = 0;
        DOSParamBlock block = new DOSParamBlock(blockPt);

        block.loadData();
        // Remove the loadhigh flag for the moment!
        if ((flags & 0x80) != 0)
            Log.logging(Log.LogTypes.EXEC, Log.LogServerities.Error,
                    "using loadhigh flag!!!!!. dropping it");
        flags &= 0x7f;
        if (flags != LOADNGO && flags != OVERLAY && flags != LOAD) {
            setError(DOSERR_FORMAT_INVALID);
            return false;
            // E_Exit("DOS:Not supported execute mode %d for file %s",flags,name);
        }
        /* Check for EXE or COM File */
        boolean isCom = false;
        if (!openFile(name, DOSSystem.OPEN_READ)) {
            setError(DOSERR_FILE_NOT_FOUND);
            return false;
        }
        fHandle = returnFileHandle;
        len = Size_EXE_Header;
        if (!readFile(fHandle, head, 0, len)) {
            closeFile(fHandle);
            return false;
        }
        len = readFileSize;
        if (len < Size_EXE_Header) {
            if (len == 0) {
                /* Prevent executing zero byte files */
                setError(DOSERR_ACCESS_DENIED);
                closeFile(fHandle);
                return false;
            }
            /* Otherwise must be a .com file */
            isCom = true;
        } else {
            // 어디에서 endian이 뒤바뀌었는지 모르겠는데, little endian으로 읽었기 때문에 일단 사용안함
            /* Convert the header to correct endian, i hope this works */
            // HostPt endian=(HostPt)&head;
            // for (i=0;i<Size_EXE_Header/2;i++) {
            // *((short *)endian)=host_readw(endian);
            // endian+=2;
            // }
            int tmpSignature = ByteConv.getShort(head, Off_EXE_Header_signature);
            int tmpPages = ByteConv.getShort(head, Off_EXE_Header_pages);
            if ((tmpSignature != MAGIC1) && (tmpSignature != MAGIC2))
                isCom = true;
            else {
                if ((tmpPages & ~0x07ff) != 0) {
                    /* 1 MB dos maximum address limit. Fixes TC3 IDE (kippesoep) */
                    Log.logging(Log.LogTypes.EXEC, Log.LogServerities.Normal,
                            "Weird header: head.pages > 1 MB");
                }
                head[Off_EXE_Header_pages] &= 0xff;
                head[Off_EXE_Header_pages + 1] &= 0x07;
                headerSize = ByteConv.getShort(head, Off_EXE_Header_headersize) * 16;
                imagesize = ByteConv.getShort(head, Off_EXE_Header_pages) * 512 - headerSize;
                if (imagesize + headerSize < 512)
                    imagesize = 512 - headerSize;
            }
        }
        byte[] loadBuf = new byte[0x10000];
        int headMinMemory = 0, headMaxMemory = 0;
        if (flags != OVERLAY) {
            /* Create an environment block */
            envSeg = block.Exec.EnvSeg;
            if (!makeEnv(name, envSeg)) {
                closeFile(fHandle);
                return false;
            }
            envSeg = returnedMakeEnvSeg;
            /* Get Memory */
            int minSize, maxSize;
            int maxFree = 0xffff;
            tryAllocateMemory(maxFree);
            maxFree = returnedAllocateMemoryBlock;
            pspSeg = returnedAllocateMemorySeg;
            if (isCom) {
                minSize = 0x1000;
                maxSize = 0xffff;
                if (DOSBox.Machine == DOSBox.MachineType.PCJR) {
                    /* try to load file into memory below 96k */
                    pos = 0;
                    pos = seekFile(fHandle, pos, DOSSystem.DOS_SEEK_SET);
                    int dataRead = 0x1800;
                    readFile(fHandle, loadBuf, 0, dataRead);
                    dataRead = readFileSize;
                    if (dataRead < 0x1800)
                        maxSize = dataRead;
                    if (minSize > maxSize)
                        minSize = maxSize;
                }
            } else { /* Exe size calculated from header */
                headMinMemory = ByteConv.getShort(head, Off_EXE_Header_minmemory);
                minSize = doLong2Para(imagesize + (headMinMemory << 4) + 256);
                headMaxMemory = ByteConv.getShort(head, Off_EXE_Header_maxmemory);
                if (headMaxMemory != 0)
                    maxSize = doLong2Para(imagesize + (headMaxMemory << 4) + 256);
                else
                    maxSize = 0xffff;
            }
            if (maxFree < minSize) {
                if (isCom) {
                    /* Reduce minimum of needed memory size to filesize */
                    pos = 0;
                    pos = seekFile(fHandle, pos, DOSSystem.DOS_SEEK_SET);
                    int dataread = 0xf800;
                    readFile(fHandle, loadBuf, 0, dataread);
                    dataread = readFileSize;
                    if (dataread < 0xf800)
                        minSize = 0xffff & (((dataread + 0x10) >>> 4) + 0x20);
                }
                if (maxFree < minSize) {
                    closeFile(fHandle);
                    setError(DOSERR_INSUFFICIENT_MEMORY);
                    freeMemory(envSeg);
                    return false;
                }
            }
            if (maxFree < maxSize)
                memSize = maxFree;
            else
                memSize = maxSize;
            if (!tryAllocateMemory(memSize))
                Support.exceptionExit("DOS:Exec error in memory");
            pspSeg = returnedAllocateMemorySeg;
            memSize = returnedAllocateMemoryBlock;

            if (isCom && (DOSBox.Machine == DOSBox.MachineType.PCJR) && (pspSeg < 0x2000)) {
                maxSize = 0xffff;
                /* resize to full extent of memory block */
                tryResizeMemory(pspSeg, maxSize);
                maxSize = returnedResizedMemoryBlocks;
                /* now try to lock out memory above segment 0x2000 */
                if ((Memory.realReadB(0x2000, 0) == 0x5a) && (Memory.realReadW(0x2000, 1) == 0)
                        && (Memory.realReadW(0x2000, 3) == 0x7ffe)) {
                    /* MCB after PCJr graphics memory region is still free */
                    if (pspSeg + maxSize == 0x17ff) {
                        DOSMCB cmcb = new DOSMCB(pspSeg - 1);
                        cmcb.setType(0x5a); // last block
                    }
                }
            }
            loadSeg = pspSeg + 16;
            if (!isCom) {
                /* Check if requested to load program into upper part of allocated memory */
                if ((headMinMemory == 0) && (headMaxMemory == 0))
                    loadSeg = 0xffff & (((pspSeg + memSize) * 0x10 - imagesize) / 0x10);
            }
        } else
            loadSeg = block.Overlay.LoadSeg;
        /* Load the executable */
        loadAddress = Memory.physMake(loadSeg, 0);

        if (isCom) { /* COM Load 64k - 256 bytes max */
            pos = 0;
            pos = seekFile(fHandle, pos, DOSSystem.DOS_SEEK_SET);
            readSize = 0xffff - 256;
            readFile(fHandle, loadBuf, 0, readSize);
            readSize = readFileSize;
            Memory.blockWrite(loadAddress, loadBuf, 0, readSize);
        } else { /* EXE Load in 32kb blocks and then relocate */
            pos = headerSize;
            pos = seekFile(fHandle, pos, DOSSystem.DOS_SEEK_SET);
            while (imagesize > 0x7FFF) {
                readSize = 0x8000;
                readFile(fHandle, loadBuf, 0, readSize);
                readSize = readFileSize;
                Memory.blockWrite(loadAddress, loadBuf, 0, readSize);
                // if (readsize!=0x8000) LOG(LOG_EXEC,LOG_NORMAL)("Illegal header");
                loadAddress += 0x8000;
                imagesize -= 0x8000;
            }
            if (imagesize > 0) {
                readSize = imagesize;
                readFile(fHandle, loadBuf, 0, readSize);
                readSize = readFileSize;
                Memory.blockWrite(loadAddress, loadBuf, 0, readSize);
                // if (readsize!=imagesize) LOG(LOG_EXEC,LOG_NORMAL)("Illegal header");
            }
            /* Relocate the exe image */
            int relocate;
            if (flags == OVERLAY)
                relocate = block.Overlay.Relocation;
            else
                relocate = loadSeg;
            pos = ByteConv.getShort(head, Off_EXE_Header_reloctable);
            pos = seekFile(fHandle, pos, 0);
            int headRelocations = ByteConv.getShort(head, Off_EXE_Header_relocations);
            for (i = 0; i < headRelocations; i++) {
                readSize = 4;
                readFile(fHandle, relocpt, 0, readSize);
                readSize = readFileSize;
                // relocpt=host_readd((HostPt)&relocpt); //Endianize
                int uintrelocpt = ByteConv.getInt(relocpt, 0);
                int address = Memory.physMake(Memory.realSeg(uintrelocpt) + loadSeg,
                        Memory.realOff(uintrelocpt));
                Memory.writeW(address, Memory.readW(address) + relocate);
            }
        }
        loadBuf = null;
        closeFile(fHandle);

        /* Setup a psp */
        if (flags != OVERLAY) {
            // Create psp after closing exe, to avoid dead file handle of exe in copied psp
            setupPSP(pspSeg, memSize, envSeg);
            setupCMDLine(pspSeg, block);
        }
        Callback.scf(false); /* Carry flag cleared for caller if successfull */
        if (flags == OVERLAY)
            return true; /* Everything done for overlays */
        int csip, sssp;
        if (isCom) {
            csip = Memory.realMake(pspSeg, 0x100);
            sssp = Memory.realMake(pspSeg, 0xfffe);
            Memory.writeW(Memory.physMake(pspSeg, 0xfffe), 0);
        } else {
            csip = Memory.realMake(loadSeg + ByteConv.getShort(head, Off_EXE_Header_initCS),
                    ByteConv.getShort(head, Off_EXE_Header_initIP));
            int headinitSP = ByteConv.getShort(head, Off_EXE_Header_initSP);
            sssp = Memory.realMake(loadSeg + ByteConv.getShort(head, Off_EXE_Header_initSS),
                    headinitSP);
            if (headinitSP < 4)
                Log.logging(Log.LogTypes.EXEC, Log.LogServerities.Error,
                        "stack underflow/wrap at EXEC");
        }

        if (flags == LOAD) {
            saveRegisters();
            DOSPSP callpsp = new DOSPSP(DOS.getPSP());
            /* Save the SS:SP on the PSP of calling program */
            callpsp.setStack(Register.realMakeSeg(Register.SEG_NAME_SS, Register.getRegSP()));
            Register.setRegSP(0xffff & (Register.getRegSP() + 18));
            /* Switch the psp's */
            DOS.setPSP(pspSeg);
            DOSPSP newpsp = new DOSPSP(DOS.getPSP());
            DOS.setDTA(Memory.realMake(newpsp.getSegment(), 0x80));
            /* First word on the stack is the value ax should contain on startup */
            Memory.realWriteW(Memory.realSeg(sssp - 2), Memory.realOff(sssp - 2), 0xffff);
            block.Exec.InitSSSP = sssp - 2;
            block.Exec.InitCSIP = csip;
            block.saveData();
            return true;
        }

        if (flags == LOADNGO) {
            if ((Register.getRegSP() > 0xfffe) || (Register.getRegSP() < 18))
                Log.logging(Log.LogTypes.EXEC, Log.LogServerities.Error,
                        "stack underflow/wrap at EXEC");
            /* Get Caller's program CS:IP of the stack and set termination address to that */
            Memory.realSetVec(0x22, Memory.realMake(
                    Memory.readW(Register.segPhys(Register.SEG_NAME_SS) + Register.getRegSP() + 2),
                    Memory.readW(Register.segPhys(Register.SEG_NAME_SS) + Register.getRegSP())));
            saveRegisters();
            DOSPSP callpsp = new DOSPSP(DOS.getPSP());
            /* Save the SS:SP on the PSP of calling program */
            callpsp.setStack(Register.realMakeSeg(Register.SEG_NAME_SS, Register.getRegSP()));
            /* Switch the psp's and set new DTA */
            DOS.setPSP(pspSeg);
            DOSPSP newpsp = new DOSPSP(DOS.getPSP());
            DOS.setDTA(Memory.realMake(newpsp.getSegment(), 0x80));
            /* save vectors */
            newpsp.saveVectors();
            /* copy fcbs */
            newpsp.setFCB1(block.Exec.FCB1);
            newpsp.setFCB2(block.Exec.FCB2);
            /* Set the stack for new program */
            Register.segSet16(Register.SEG_NAME_SS, Memory.realSeg(sssp));
            Register.setRegSP(Memory.realOff(sssp));
            /* Add some flags and CS:IP on the stack for the IRET */
            CPU.push16(Memory.realSeg(csip));
            CPU.push16(Memory.realOff(csip));
            /*
             * DOS starts programs with a RETF, so critical flags should not be modified (IOPL in
             * v86 mode); interrupt flag is set explicitly, test flags cleared
             */
            Register.Flags = (Register.Flags & (~Register.FMaskTest)) | Register.FlagIF;
            // Jump to retf so that we only need to store cs:ip on the stack
            Register.setRegIP(Register.getRegIP() + 1);
            /* Setup the rest of the registers */
            Register.setRegAX(0);
            Register.setRegBX(0);
            Register.setRegCX(0xff);
            Register.setRegDX(pspSeg);
            Register.setRegSI(Memory.realOff(csip));
            Register.setRegDI(Memory.realOff(sssp));
            Register.setRegBP(0x91c); /* DOS internal stack begin relict */
            Register.segSet16(Register.SEG_NAME_DS, pspSeg);
            Register.segSet16(Register.SEG_NAME_ES, pspSeg);

            /* Add the filename to PSP and environment MCB's */
            CStringPt stripname = CStringPt.create(8);
            stripname.set(0, (char) 0);
            int index = 0;

            int nmSz = name.length();
            for (int nameIdx = 0; nameIdx < nmSz; nameIdx++) {
                char chr = name.charAt(nameIdx);
                switch (chr) {
                    case ':':
                    case '\\':
                    case '/':
                        index = 0;
                        break;
                    default:
                        if (index < 8)
                            stripname.set(index++, Character.toUpperCase(chr));
                        break;
                }
            }
            index = 0;
            while (index < 8) {
                if (stripname.get(index) == '.')
                    break;
                if (stripname.get(index) == 0)
                    break;
                index++;
            }
            CStringPt.clear(stripname, index, (8 - index));
            DOSMCB pspmcb = new DOSMCB(DOS.getPSP() - 1);
            pspmcb.setFileName(stripname);
            updatePSPName();
            return true;
        }
        return false;
    }

    /*--------------------------- end DOSExecute -----------------------------*/

    /*--------------------------- begin DOSDevices -----------------------------*/
    // -- #region dos_devices
    public static DOSDevice[] Devices = new DOSDevice[DOS_DEVICES];

    private static final String com = "COM1";
    private static final String lpt = "LPT1";

    private static byte findDevice(String name) {
        /* should only check for the names before the dot and spacepadded */
        // CStringPt fullname = CStringPt.Create( DOSSystem.DOS_PATHLENGTH);
        // byte drive = 0;
        // RefU8Ret refDrive = new RefU8Ret(drive);
        //// if(!name || !(*name)) return DOS_DEVICES; //important, but makename does it
        // if (!MakeName(name, fullname, refDrive))
        // return DOS_DEVICES;
        // drive = refDrive.U8;

        if (!makeFullName(name, DOSSystem.DOS_PATHLENGTH))
            return DOS_DEVICES;
        int drive = 0xff & returnedFullNameDrive;
        String fullname = returnedFullName;


        // CStringPt namePart = fullname.lastPositionOf('\\');
        int dirSepIdx = fullname.lastIndexOf("\\");
        String namePart = "";
        if (dirSepIdx >= 0) {
            namePart = fullname.substring(dirSepIdx + 1);
            // Check validity of leading directory.
            if (!Drives[drive].testDir(fullname.substring(0,dirSepIdx)))
                return DOS_DEVICES;
        } else
            namePart = fullname;

        int dotIdx = namePart.lastIndexOf(".");
        if (dotIdx >= 0)
            namePart = namePart.substring(0, dotIdx); // no ext checking


        // AUX is alias for COM1 and PRN for LPT1
        // A bit of a hack. (but less then before).
        // no need for casecmp as makename returns uppercase
        if (namePart.equalsIgnoreCase("AUX"))
            namePart = com;
        if (namePart.equalsIgnoreCase("PRN"))
            namePart = lpt;

        /* loop through devices */
        for (byte index = 0; index < DOS_DEVICES; index++) {
            if (Devices[index] != null) {
                if (org.gutkyu.dosboxj.dos.system.drive.Drives.compareWildFile(namePart,
                        Devices[index].Name.toString()))
                    return index;
            }
        }
        return DOS_DEVICES;
    }

    public static void addDevice(DOSDevice addDev) {
        // Caller creates the device. We store a pointer to it
        // TODO Give the Device a real handler in low memory that responds to calls
        for (int i = 0; i < DOS_DEVICES; i++) {
            if (Devices[i] == null) {
                Devices[i] = addDev;
                Devices[i].setDeviceNumber(i);
                return;
            }
        }
        Support.exceptionExit("DOS:Too many devices added");
    }

    public static void delDevice(DOSDevice dev) {
        // We will destroy the device if we find it in our list.
        // TODO:The file table is not checked to see the device is opened somewhere!
        for (int i = 0; i < DOS_DEVICES; i++) {
            if (Devices[i] != null && !Devices[i].Name.equalsIgnoreCase(dev.Name)) {
                Devices[i].dispose();
                Devices[i] = null;
                return;
            }
        }
    }

    public static void setupDevices() {
        DOSDevice newdev = new DeviceCON();
        addDevice(newdev);
        DOSDevice newdev2 = new DeviceNUL();
        addDevice(newdev2);
        // DOS_Device newdev3 = new device_LPT1();
        // DOS_AddDevice(newdev3);
    }

    // -- #endregion

    /*--------------------------- end DOSDevices -----------------------------*/

}
