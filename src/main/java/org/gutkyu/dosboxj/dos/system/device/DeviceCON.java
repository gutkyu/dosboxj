package org.gutkyu.dosboxj.dos.system.device;

import org.gutkyu.dosboxj.DOSBox;
import org.gutkyu.dosboxj.dos.*;
import org.gutkyu.dosboxj.hardware.memory.*;
import org.gutkyu.dosboxj.cpu.*;
import org.gutkyu.dosboxj.util.*;
import org.gutkyu.dosboxj.interrupt.bios.*;
import org.gutkyu.dosboxj.interrupt.int10.*;

public final class DeviceCON extends DOSDevice {
    private static final int NUMBER_ANSI_DATA = 10;

    public DeviceCON() {
        setName(CStringPt.create("CON"));
        readcache = 0;
        lastwrite = 0;
        ansi.enabled = false;
        ansi.attr = 0x7;
        // should be updated once set/reset modeis implemented
        ansi.ncols = Memory.realReadW(INT10.BIOSMEM_SEG, INT10.BIOSMEM_NB_COLS);
        ansi.nrows = (Memory.realReadB(INT10.BIOSMEM_SEG, INT10.BIOSMEM_NB_ROWS) + 1);
        ansi.saverow = 0;
        ansi.savecol = 0;
        ansi.warned = false;
        ClearAnsi();
    }

    private final byte[] tmpRd = new byte[1];

    @Override
    public boolean read() {
        return this.read(tmpRd, 0, 1);
    }

    @Override
    public boolean read(byte[] buf, int offset, int size) {
        byte[] data = buf;
        int oldax = Register.getRegAX();
        short count = 0;
        if (readcache != 0 && size > 0) {
            data[offset + count++] = readcache;
            if (DOSMain.DOS.Echo)
                CHAR.teletypeOutput((byte) readcache, (byte) 7);
            readcache = 0;
        }
        while (size > count) {
            Register.setRegAH(DOSBox.isEGAVGAArch() ? 0x10 : 0x0);
            Callback.runRealInt(0x16);
            switch (Register.getRegAL()) {
                case 13:
                    data[offset + count++] = 0x0D;
                    if (size > count)
                        data[offset + count++] = 0x0A; // it's only expanded if there is room for
                                                       // it. (NO cache)
                    size = count;
                    Register.setRegAX(oldax);
                    if (DOSMain.DOS.Echo) {
                        // maybe don't do this ( no need for it actually ) (but it's compatible)
                        CHAR.teletypeOutput((byte) 13, (byte) 7);
                        CHAR.teletypeOutput((byte) 10, (byte) 7);
                    }
                    return true;
                // break;
                case 8:
                    if (size == 1)
                        data[offset + count++] = (byte) Register.getRegAL(); // one char at the time
                                                                             // so
                    // give back that BS
                    else if (count != 0) { // Remove data if it exists (extended keys don't go
                                           // right)
                        data[offset + count--] = 0;
                        CHAR.teletypeOutput((byte) 8, (byte) 7);
                        CHAR.teletypeOutput((byte) ' ', (byte) 7);
                    } else {
                        continue; // no data read yet so restart whileloop.
                    }
                    break;
                case 0xe0: /* Extended keys in the int 16 0x10 case */
                    if (Register.getRegAH() == 0) { /* extended key if reg_ah isn't 0 */
                        data[offset + count++] = (byte) Register.getRegAL();
                    } else {
                        data[offset + count++] = 0;
                        if (size > count)
                            data[offset + count++] = (byte) Register.getRegAH();
                        else
                            readcache = (byte) Register.getRegAH();
                    }
                    break;
                case 0: /* Extended keys in the int 16 0x0 case */
                    data[offset + count++] = (byte) Register.getRegAL();
                    if (size > count)
                        data[offset + count++] = (byte) Register.getRegAH();
                    else
                        readcache = (byte) Register.getRegAH();
                    break;
                default:
                    data[offset + count++] = (byte) Register.getRegAL();
                    break;
            }
            if (DOSMain.DOS.Echo) { // what to do if *size==1 and character is BS ?????
                CHAR.teletypeOutput((byte) Register.getRegAL(), (byte) 7);
            }
        }
        rdSz = count;
        Register.setRegAX(oldax);
        return true;
    }

    @Override
    public byte getReadByte() {
        return tmpRd[0];
    }

    private int rdSz = 0;

    @Override
    public int readSize() {
        return rdSz;
    }

    @Override
    public boolean write(byte[] buf, int offset, int size) {
        int count = 0;
        int i;
        byte col, row;
        byte tempdata;
        while (size > count) {
            if (!ansi.esc) {
                if (buf[count + offset] == 0x1b) // c++ 8진수표현 '\033' -> c# 16진수 유니코드'\u001B'
                {
                    /* clear the datastructure */
                    ClearAnsi();
                    /* start the sequence */
                    ansi.esc = true;
                    count++;
                    continue;
                } else {
                    /*
                     * Some sort of "hack" now that \n doesn't set col to 0 (int10_char.cpp old
                     * chessgame)
                     */
                    if ((buf[count + offset] == '\n') && (lastwrite != '\r'))
                        CHAR.teletypeOutputAttr((byte) '\r', ansi.attr, ansi.enabled);
                    /* pass attribute only if ansi is enabled */
                    CHAR.teletypeOutputAttr(buf[count + offset], ansi.attr, ansi.enabled);
                    lastwrite = buf[count++];
                    continue;
                }
            }

            if (!ansi.sci) {

                switch (buf[count + offset]) {
                    case (byte) '[':
                        ansi.sci = true;
                        break;
                    case (byte) '7': /* save cursor pos + attr */
                    case (byte) '8': /* restore this (Wonder if this is actually used) */
                    case (byte) 'D':/* scrolling DOWN */
                    case (byte) 'M':/* scrolling UP */
                    default:
                        Log.logging(Log.LogTypes.IOCTL, Log.LogServerities.Normal,
                                "ANSI: unknown char %c after a esc",
                                buf[count + offset]); /* prob () */
                        ClearAnsi();
                        break;
                }
                count++;
                continue;
            }
            /* ansi.esc and ansi.sci are true */
            byte page = (byte) Memory.realReadB(INT10.BIOSMEM_SEG, INT10.BIOSMEM_CURRENT_PAGE);
            switch (buf[count + offset]) {
                case (byte) '0':
                case (byte) '1':
                case (byte) '2':
                case (byte) '3':
                case (byte) '4':
                case (byte) '5':
                case (byte) '6':
                case (byte) '7':
                case (byte) '8':
                case (byte) '9':
                    ansi.data[ansi.numberofarg] =
                            (byte) (10 * ansi.data[ansi.numberofarg] + (buf[count + offset] - '0'));
                    break;
                case (byte) ';': /* till a max of NUMBER_ANSI_DATA */
                    ansi.numberofarg++;
                    break;
                case (byte) 'm': /* SGR */
                    for (i = 0; i <= ansi.numberofarg; i++) {
                        ansi.enabled = true;
                        switch (ansi.data[i]) {
                            case 0: /* normal */
                                ansi.attr = 0x07;// Real ansi does this as well. (should do current
                                                 // defaults)
                                ansi.enabled = false;
                                break;
                            case 1: /* bold mode on */
                                ansi.attr |= 0x08;
                                break;
                            case 4: /* underline */
                                Log.logging(Log.LogTypes.IOCTL, Log.LogServerities.Normal,
                                        "ANSI:no support for underline yet");
                                break;
                            case 5: /* blinking */
                                ansi.attr |= 0x80;
                                break;
                            case 7: /* reverse */
                                ansi.attr = 0x70;// Just like real ansi. (should do use current
                                                 // colors reversed)
                                break;
                            case 30: /* fg color black */
                                ansi.attr &= 0xf8;
                                ansi.attr |= 0x0;
                                break;
                            case 31: /* fg color red */
                                ansi.attr &= 0xf8;
                                ansi.attr |= 0x4;
                                break;
                            case 32: /* fg color green */
                                ansi.attr &= 0xf8;
                                ansi.attr |= 0x2;
                                break;
                            case 33: /* fg color yellow */
                                ansi.attr &= 0xf8;
                                ansi.attr |= 0x6;
                                break;
                            case 34: /* fg color blue */
                                ansi.attr &= 0xf8;
                                ansi.attr |= 0x1;
                                break;
                            case 35: /* fg color magenta */
                                ansi.attr &= 0xf8;
                                ansi.attr |= 0x5;
                                break;
                            case 36: /* fg color cyan */
                                ansi.attr &= 0xf8;
                                ansi.attr |= 0x3;
                                break;
                            case 37: /* fg color white */
                                ansi.attr &= 0xf8;
                                ansi.attr |= 0x7;
                                break;
                            case 40:
                                ansi.attr &= 0x8f;
                                ansi.attr |= 0x0;
                                break;
                            case 41:
                                ansi.attr &= 0x8f;
                                ansi.attr |= 0x40;
                                break;
                            case 42:
                                ansi.attr &= 0x8f;
                                ansi.attr |= 0x20;
                                break;
                            case 43:
                                ansi.attr &= 0x8f;
                                ansi.attr |= 0x60;
                                break;
                            case 44:
                                ansi.attr &= 0x8f;
                                ansi.attr |= 0x10;
                                break;
                            case 45:
                                ansi.attr &= 0x8f;
                                ansi.attr |= 0x50;
                                break;
                            case 46:
                                ansi.attr &= 0x8f;
                                ansi.attr |= 0x30;
                                break;
                            case 47:
                                ansi.attr &= 0x8f;
                                ansi.attr |= 0x70;
                                break;
                            default:
                                break;
                        }
                    }
                    ClearAnsi();
                    break;
                case (byte) 'f':
                case (byte) 'H':/* Cursor Pos */
                    if (!ansi.warned) { // Inform the debugger that ansi is used.
                        ansi.warned = true;
                        Log.logging(Log.LogTypes.IOCTL, Log.LogServerities.Warn,
                                "ANSI SEQUENCES USED");
                    }
                    /* Turn them into positions that are on the screen */
                    if (ansi.data[0] == 0)
                        ansi.data[0] = 1;
                    if (ansi.data[1] == 0)
                        ansi.data[1] = 1;
                    if (ansi.data[0] > ansi.nrows)
                        ansi.data[0] = (byte) ansi.nrows;
                    if (ansi.data[1] > ansi.ncols)
                        ansi.data[1] = (byte) ansi.ncols;
                    CHAR.setCursorPos(--(ansi.data[0]), --(ansi.data[1]),
                            page); /* ansi=1 based, int10 is 0 based */
                    ClearAnsi();
                    break;
                /*
                 * cursor up down and forward and backward only change the row or the col not both
                 */
                case (byte) 'A': /* cursor up */
                    col = INT10.getCursorPosCol(page);
                    row = INT10.getCursorPosRow(page);
                    tempdata = (ansi.data[0] != 0 ? ansi.data[0] : (byte) 1);
                    if (tempdata > row) {
                        row = 0;
                    } else {
                        row -= tempdata;
                    }
                    CHAR.setCursorPos(row, col, page);
                    ClearAnsi();
                    break;
                case (byte) 'B': /* cursor Down */
                    col = INT10.getCursorPosCol(page);
                    row = INT10.getCursorPosRow(page);
                    tempdata = (ansi.data[0] != 0 ? ansi.data[0] : (byte) 1);
                    if (tempdata + (int) row >= ansi.nrows) {
                        row = (byte) (ansi.nrows - 1);
                    } else {
                        row += tempdata;
                    }
                    CHAR.setCursorPos(row, col, page);
                    ClearAnsi();
                    break;
                case (byte) 'C': /* cursor forward */
                    col = INT10.getCursorPosCol(page);
                    row = INT10.getCursorPosRow(page);
                    tempdata = (ansi.data[0] != 0 ? ansi.data[0] : (byte) 1);
                    if (tempdata + (int) col >= ansi.ncols) {
                        col = (byte) (ansi.ncols - 1);
                    } else {
                        col += tempdata;
                    }
                    CHAR.setCursorPos(row, col, page);
                    ClearAnsi();
                    break;
                case (byte) 'D': /* Cursor Backward */
                    col = INT10.getCursorPosCol(page);
                    row = INT10.getCursorPosRow(page);
                    tempdata = (ansi.data[0] != 0 ? ansi.data[0] : (byte) 1);
                    if (tempdata > col) {
                        col = 0;
                    } else {
                        col -= tempdata;
                    }
                    CHAR.setCursorPos(row, col, page);
                    ClearAnsi();
                    break;
                case (byte) 'J': /* erase screen and move cursor home */
                    if (ansi.data[0] == 0)
                        ansi.data[0] = 2;
                    if (ansi.data[0] != 2) {/* every version behaves like type 2 */
                        Log.logging(Log.LogTypes.IOCTL, Log.LogServerities.Normal,
                                "ANSI: esc[%dJ called : not supported handling as 2", ansi.data[0]);
                    }
                    CHAR.scrollWindow((byte) 0, (byte) 0, (byte) 255, (byte) 255, (byte) 0,
                            ansi.attr, page);
                    ClearAnsi();
                    CHAR.setCursorPos((byte) 0, (byte) 0, page);
                    break;
                case (byte) 'h': /* SET MODE (if code =7 enable linewrap) */
                case (byte) 'I': /* RESET MODE */
                    Log.logging(Log.LogTypes.IOCTL, Log.LogServerities.Normal,
                            "ANSI: set/reset mode called(not supported)");
                    ClearAnsi();
                    break;
                case (byte) 'u': /* Restore Cursor Pos */
                    CHAR.setCursorPos((byte) ansi.saverow, (byte) ansi.savecol, page);
                    ClearAnsi();
                    break;
                case (byte) 's': /* SAVE CURSOR POS */
                    ansi.savecol = (byte) INT10.getCursorPosCol(page);
                    ansi.saverow = (byte) INT10.getCursorPosRow(page);
                    ClearAnsi();
                    break;
                case (byte) 'K': /* erase till end of line (don't touch cursor) */
                    col = INT10.getCursorPosCol(page);
                    row = INT10.getCursorPosRow(page);
                    // Use this one to prevent scrolling when end of screen is reached
                    CHAR.writeChar2((byte) ' ', ansi.attr, page, (short) (ansi.ncols - col), true);
                    // for(i = col;i<(int) ansi.ncols; i++) INT10_TeletypeOutputAttr('
                    // ',ansi.attr,true);
                    CHAR.setCursorPos(row, col, page);
                    ClearAnsi();
                    break;
                case (byte) 'M': /* delete line (NANSI) */
                    col = INT10.getCursorPosCol(page);
                    row = INT10.getCursorPosRow(page);
                    CHAR.scrollWindow(row, (byte) 0, (byte) (ansi.nrows - 1),
                            (byte) (ansi.ncols - 1),
                            ansi.data[0] != 0 ? (byte) -ansi.data[0] : (byte) -1, ansi.attr,
                            (byte) 0xFF);
                    ClearAnsi();
                    break;
                case (byte) 'l':/* (if code =7) disable linewrap */
                case (byte) 'p':/* reassign keys (needs strings) */
                case (byte) 'i':/* printer stuff */
                default:
                    Log.logging(Log.LogTypes.IOCTL, Log.LogServerities.Normal,
                            "ANSI: unhandled char %c in esc[", buf[count + offset]);
                    ClearAnsi();
                    break;
            }
            count++;
        }
        wrtSz = count;
        return true;
    }

    private final byte[] tmpWrt = new byte[1];

    @Override
    public boolean write(byte value, int size) {
        tmpWrt[0] = value;
        return this.write(tmpWrt, 0, size);
    }

    @Override
    public boolean write(byte value) {
        tmpWrt[0] = value;
        return write(tmpWrt, 0, 1);
    }

    private int wrtSz = 0;

    @Override
    public int writtenSize() {
        return wrtSz;
    }

    @Override
    public long seek(long pos, int type) {
        // seek is valid
        return pos = 0;
    }

    @Override
    public boolean close() {
        return true;
    }

    public void ClearAnsi() {
        for (byte i = 0; i < NUMBER_ANSI_DATA; i++)
            ansi.data[i] = 0;
        ansi.esc = false;
        ansi.sci = false;
        ansi.numberofarg = 0;
    }

    // uint16()
    @Override
    public int getInformation() {
        int head = Memory.readW(BIOS.BIOS_KEYBOARD_BUFFER_HEAD);
        int tail = Memory.readW(BIOS.BIOS_KEYBOARD_BUFFER_TAIL);

        if ((head == tail) && readcache == 0)
            return 0x80D3; /* No Key Available */
        if (readcache != 0 || Memory.realReadW(0x40, head) != 0)
            return 0x8093; /* Key Available */

        /* remove the zero from keyboard buffer */
        int start = Memory.readW(BIOS.BIOS_KEYBOARD_BUFFER_START);
        int end = Memory.readW(BIOS.BIOS_KEYBOARD_BUFFER_END);
        head += 2;
        if (head >= end)
            head = start;
        Memory.writeW(BIOS.BIOS_KEYBOARD_BUFFER_HEAD, head);
        return 0x80D3; /* No Key Available */
    }

    // 실패하면 code -1 리턴
    @Override
    public int readFromControlChannel(int bufPtr, int size) {
        return -1;
    }

    // 실패하면 code -1 리턴
    @Override
    public int writeToControlChannel(int bufPtr, int size) {
        return -1;
    }

    private byte readcache;
    private byte lastwrite;

    class ANSI { /* should create a constructor which fills them with the appriorate values */
        public boolean esc;
        public boolean sci;
        public boolean enabled;
        public byte attr;
        public byte[] data = new byte[NUMBER_ANSI_DATA];
        public byte numberofarg;
        public int nrows;
        public int ncols;
        // sbyte
        public byte savecol;
        // sbyte
        public byte saverow;
        public boolean warned;
    }

    private ANSI ansi = new ANSI();
}
