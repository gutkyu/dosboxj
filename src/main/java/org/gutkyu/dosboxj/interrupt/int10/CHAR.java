package org.gutkyu.dosboxj.interrupt.int10;

import org.gutkyu.dosboxj.hardware.io.*;
import org.gutkyu.dosboxj.hardware.memory.*;
import org.gutkyu.dosboxj.hardware.video.*;
import org.gutkyu.dosboxj.util.*;
import org.gutkyu.dosboxj.*;

/*--------------------------- begin INT10Char -----------------------------*/
public final class CHAR {
    // (Bit8u cleft,Bit8u cright,Bit8u rold,Bit8u rnew,PhysPt base)
    private static void cga2CopyRow(int cleft, int cright, int rold, int rnew, int _base) {
        int cheight = Memory.realReadB(INT10.BIOSMEM_SEG, INT10.BIOSMEM_CHAR_HEIGHT);
        int dest = _base + ((INT10Mode.CurMode.TWidth * rnew) * (cheight / 2) + cleft);
        int src = _base + ((INT10Mode.CurMode.TWidth * rold) * (cheight / 2) + cleft);
        int copy = cright - cleft;
        int nextline = INT10Mode.CurMode.TWidth;
        for (int i = 0; i < cheight / 2; i++) {
            Memory.blockCopy(dest, src, copy);
            Memory.blockCopy(dest + 8 * 1024, src + 8 * 1024, copy);
            dest += nextline;
            src += nextline;
        }
    }

    // (Bit8u cleft,Bit8u cright,Bit8u rold,Bit8u rnew,PhysPt base)
    private static void cga4CopyRow(int cleft, int cright, int rold, int rnew, int _base) {
        int cheight = Memory.realReadB(INT10.BIOSMEM_SEG, INT10.BIOSMEM_CHAR_HEIGHT);
        int dest = _base + ((INT10Mode.CurMode.TWidth * rnew) * (cheight / 2) + cleft) * 2;
        int src = _base + ((INT10Mode.CurMode.TWidth * rold) * (cheight / 2) + cleft) * 2;
        int copy = (cright - cleft) * 2;
        int nextline = INT10Mode.CurMode.TWidth * 2;
        for (int i = 0; i < cheight / 2; i++) {
            Memory.blockCopy(dest, src, copy);
            Memory.blockCopy(dest + 8 * 1024, src + 8 * 1024, copy);
            dest += nextline;
            src += nextline;
        }
    }

    // (Bit8u cleft,Bit8u cright,Bit8u rold,Bit8u rnew,PhysPt base)
    private static void tandy16CopyRow(int cleft, int cright, int rold, int rnew, int _base) {
        int cheight = Memory.realReadB(INT10.BIOSMEM_SEG, INT10.BIOSMEM_CHAR_HEIGHT);
        int dest = _base + ((INT10Mode.CurMode.TWidth * rnew) * (cheight / 4) + cleft) * 4;
        int src = _base + ((INT10Mode.CurMode.TWidth * rold) * (cheight / 4) + cleft) * 4;
        int copy = (cright - cleft) * 4;
        int nextline = INT10Mode.CurMode.TWidth * 4;
        for (int i = 0; i < cheight / 4; i++) {
            Memory.blockCopy(dest, src, copy);
            Memory.blockCopy(dest + 8 * 1024, src + 8 * 1024, copy);
            Memory.blockCopy(dest + 16 * 1024, src + 16 * 1024, copy);
            Memory.blockCopy(dest + 24 * 1024, src + 24 * 1024, copy);
            dest += nextline;
            src += nextline;
        }
    }

    // (Bit8u cleft,Bit8u cright,Bit8u rold,Bit8u rnew,PhysPt base)
    private static void ega16CopyRow(int cleft, int cright, int rold, int rnew, int _base) {
        int src, dest;
        int copy;
        int cheight = Memory.realReadB(INT10.BIOSMEM_SEG, INT10.BIOSMEM_CHAR_HEIGHT);
        dest = _base + (INT10Mode.CurMode.TWidth * rnew) * cheight + cleft;
        src = _base + (INT10Mode.CurMode.TWidth * rold) * cheight + cleft;
        int nextline = INT10Mode.CurMode.TWidth;
        /* Setup registers correctly */
        IO.write(0x3ce, 5);
        IO.write(0x3cf, 1); /* Memory transfer mode */
        IO.write(0x3c4, 2);
        IO.write(0x3c5, 0xf); /* Enable all Write planes */
        /* Do some copying */
        int rowsize = cright - cleft;
        copy = cheight;
        for (; copy > 0; copy--) {
            for (int x = 0; x < rowsize; x++)
                Memory.writeB(dest + x, Memory.readB(src + x));
            dest += nextline;
            src += nextline;
        }
        /* Restore registers */
        IO.write(0x3ce, 5);
        IO.write(0x3cf, 0); /* Normal transfer mode */
    }

    // (Bit8u cleft,Bit8u cright,Bit8u rold,Bit8u rnew,PhysPt base)
    private static void vgaCopyRow(int cleft, int cright, int rold, int rnew, int _base) {
        int src, dest;
        int copy;
        int cheight = Memory.realReadB(INT10.BIOSMEM_SEG, INT10.BIOSMEM_CHAR_HEIGHT);
        dest = _base + 8 * ((INT10Mode.CurMode.TWidth * rnew) * cheight + cleft);
        src = _base + 8 * ((INT10Mode.CurMode.TWidth * rold) * cheight + cleft);
        int nextline = 8 * INT10Mode.CurMode.TWidth;
        int rowsize = 8 * (cright - cleft);
        copy = cheight;
        for (; copy > 0; copy--) {
            for (int x = 0; x < rowsize; x++)
                Memory.writeB(dest + x, Memory.readB(src + x));
            dest += nextline;
            src += nextline;
        }
    }

    // (Bit8u cleft,Bit8u cright,Bit8u rold,Bit8u rnew,PhysPt base)
    private static void textCopyRow(int cleft, int cright, int rold, int rnew, int _base) {
        int src, dest;
        src = _base + (rold * INT10Mode.CurMode.TWidth + cleft) * 2;
        dest = _base + (rnew * INT10Mode.CurMode.TWidth + cleft) * 2;
        Memory.blockCopy(dest, src, (cright - cleft) * 2);
    }

    // (Bit8u cleft,Bit8u cright,Bit8u row,PhysPt base,Bit8u attr)
    private static void cga2FillRow(int cleft, int cright, int row, int _base, int attr) {
        int cheight = Memory.realReadB(INT10.BIOSMEM_SEG, INT10.BIOSMEM_CHAR_HEIGHT);
        int dest = _base + ((INT10Mode.CurMode.TWidth * row) * (cheight / 2) + cleft);
        int copy = cright - cleft;
        int nextline = INT10Mode.CurMode.TWidth;
        attr = 0xff
                & ((attr & 0x3) | ((attr & 0x3) << 2) | ((attr & 0x3) << 4) | ((attr & 0x3) << 6));
        for (int i = 0; i < cheight / 2; i++) {
            for (int x = 0; x < copy; x++) {
                Memory.writeB(dest + x, attr);
                Memory.writeB(dest + 8 * 1024 + x, attr);
            }
            dest += nextline;
        }
    }

    // (Bit8u cleft,Bit8u cright,Bit8u row,PhysPt base,Bit8u attr)
    private static void cga4FillRow(int cleft, int cright, int row, int _base, int attr) {
        int cheight = Memory.realReadB(INT10.BIOSMEM_SEG, INT10.BIOSMEM_CHAR_HEIGHT);
        int dest = _base + ((INT10Mode.CurMode.TWidth * row) * (cheight / 2) + cleft) * 2;
        int copy = (cright - cleft) * 2;
        int nextline = INT10Mode.CurMode.TWidth * 2;
        attr = 0xff
                & ((attr & 0x3) | ((attr & 0x3) << 2) | ((attr & 0x3) << 4) | ((attr & 0x3) << 6));
        for (int i = 0; i < cheight / 2; i++) {
            for (int x = 0; x < copy; x++) {
                Memory.writeB(dest + x, attr);
                Memory.writeB(dest + 8 * 1024 + x, attr);
            }
            dest += nextline;
        }
    }

    // (Bit8u cleft,Bit8u cright,Bit8u row,PhysPt base,Bit8u attr)
    private static void tandy16FillRow(int cleft, int cright, int row, int _base, int attr) {
        int cheight = Memory.realReadB(INT10.BIOSMEM_SEG, INT10.BIOSMEM_CHAR_HEIGHT);
        int dest = _base + ((INT10Mode.CurMode.TWidth * row) * (cheight / 4) + cleft) * 4;
        int copy = (cright - cleft) * 4;
        int nextline = INT10Mode.CurMode.TWidth * 4;
        attr = 0xff & ((attr & 0xf) | (attr & 0xf) << 4);
        for (int i = 0; i < cheight / 4; i++) {
            for (int x = 0; x < copy; x++) {
                Memory.writeB(dest + x, attr);
                Memory.writeB(dest + 8 * 1024 + x, attr);
                Memory.writeB(dest + 16 * 1024 + x, attr);
                Memory.writeB(dest + 24 * 1024 + x, attr);
            }
            dest += nextline;
        }
    }

    // (Bit8u cleft,Bit8u cright,Bit8u row,PhysPt base,Bit8u attr)
    private static void ega16FillRow(int cleft, int cright, int row, int _base, int attr) {
        /* Set Bitmask / Color / Full Set Reset */
        IO.write(0x3ce, 0x8);
        IO.write(0x3cf, 0xff);
        IO.write(0x3ce, 0x0);
        IO.write(0x3cf, attr);
        IO.write(0x3ce, 0x1);
        IO.write(0x3cf, 0xf);
        /* Write some bytes */
        int cheight = Memory.realReadB(INT10.BIOSMEM_SEG, INT10.BIOSMEM_CHAR_HEIGHT);
        int dest = _base + (INT10Mode.CurMode.TWidth * row) * cheight + cleft;
        int nextline = INT10Mode.CurMode.TWidth;
        int copy = cheight;
        int rowsize = (cright - cleft);
        for (; copy > 0; copy--) {
            for (int x = 0; x < rowsize; x++)
                Memory.writeB(dest + x, 0xff);
            dest += nextline;
        }
        IO.write(0x3cf, 0);
    }

    // (Bit8u cleft,Bit8u cright,Bit8u row,PhysPt base,Bit8u attr)
    private static void vgaFillRow(int cleft, int cright, int row, int _base, int attr) {
        /* Write some bytes */
        int cheight = Memory.realReadB(INT10.BIOSMEM_SEG, INT10.BIOSMEM_CHAR_HEIGHT);
        int dest = _base + 8 * ((INT10Mode.CurMode.TWidth * row) * cheight + cleft);
        int nextline = 8 * INT10Mode.CurMode.TWidth;
        int copy = cheight;
        int rowsize = 8 * (cright - cleft);
        for (; copy > 0; copy--) {
            for (int x = 0; x < rowsize; x++)
                Memory.writeB(dest + x, attr);
            dest += nextline;
        }
    }

    private static void textFillRow(int cleft, int cright, int row, int _base, int attr) {
        /* Do some filing */
        int dest;
        dest = _base + (row * INT10Mode.CurMode.TWidth + cleft) * 2;
        int fill = (attr << 8) + (0xff & (byte) ' ');
        for (int x = 0; x < (cright - cleft); x++) {
            Memory.writeW(dest, fill);
            dest += 2;
        }
    }

    // public static void ScrollWindow(byte rul, byte cul, byte rlr, byte clr, sbyte nlines, byte
    // attr, byte page)
    public static void scrollWindow(int rul, int cul, int rlr, int clr, int nlines, int attr,
            int page) {
        /* Do some range checking */
        if (INT10Mode.CurMode.Type != VGAModes.TEXT)
            page = 0xff;
        int ncols = Memory.realReadW(INT10.BIOSMEM_SEG, INT10.BIOSMEM_NB_COLS);
        int nrows = Memory.realReadB(INT10.BIOSMEM_SEG, INT10.BIOSMEM_NB_ROWS) + 1;
        if (rul > rlr)
            return;
        if (cul > clr)
            return;
        if (rlr >= nrows)
            rlr = 0xff & (nrows - 1);
        if (clr >= ncols)
            clr = 0xff & (ncols - 1);
        clr++;

        /* Get the correct page */
        if (page == 0xFF)
            page = Memory.realReadB(INT10.BIOSMEM_SEG, INT10.BIOSMEM_CURRENT_PAGE);
        int _base = INT10Mode.CurMode.PStart
                + page * Memory.realReadW(INT10.BIOSMEM_SEG, INT10.BIOSMEM_PAGE_SIZE);

        /* See how much lines need to be copied */
        int start = 0, end = 0;
        byte next = 0;// sbyte
        boolean gotoFilling = false;
        /* Copy some lines */
        if (nlines > 0) {
            start = (rlr - nlines + 1);
            end = rul;
            next = -1;
        } else if (nlines < 0) {
            start = (rul - nlines - 1);
            end = rlr;
            next = 1;
        } else {
            // nlines = (sbyte)(rlr - rul + 1);
            nlines = (byte) (rlr - rul + 1);
            // goto filling;
            gotoFilling = true;
        }
        if (!gotoFilling)
            while (start != end) {
                start += (byte) next;
                switch (INT10Mode.CurMode.Type) {
                    case TEXT:
                        textCopyRow(0xff & cul, 0xff & clr, 0xff & start, 0xff & (start + nlines),
                                _base);
                        break;
                    case CGA2:
                        cga2CopyRow(0xff & cul, 0xff & clr, 0xff & start, 0xff & (start + nlines),
                                _base);
                        break;
                    case CGA4:
                        cga4CopyRow(0xff & cul, 0xff & clr, 0xff & start, 0xff & (start + nlines),
                                _base);
                        break;
                    case TANDY16:
                        tandy16CopyRow(0xff & cul, 0xff & clr, 0xff & start,
                                0xff & (start + nlines), _base);
                        break;
                    case EGA:
                        ega16CopyRow(0xff & cul, 0xff & clr, 0xff & start, 0xff & (start + nlines),
                                _base);
                        break;
                    case VGA:
                        vgaCopyRow(0xff & cul, 0xff & clr, 0xff & start, 0xff & (start + nlines),
                                _base);
                        break;
                    case LIN4:
                        if ((DOSBox.Machine == DOSBox.MachineType.VGA)
                                && (DOSBox.SVGACard == DOSBox.SVGACards.TsengET4K)
                                && (INT10Mode.CurMode.SWidth <= 800)) {
                            // the ET4000 BIOS supports text output in 800x600 SVGA
                            ega16CopyRow(0xff & cul, 0xff & clr, 0xff & start,
                                    0xff & (start + nlines), _base);
                            break;
                        }
                        // goto Gotodefault;
                        // fall-through
                    default:
                        // Gotodefault:
                        Log.logging(Log.LogTypes.INT10, Log.LogServerities.Error,
                                "Unhandled mode %d for scroll", INT10Mode.CurMode.Type.toString());
                        break;
                }
            }
        /* Fill some lines */
        // filling:
        if (nlines > 0) {
            start = rul;
        } else {
            // nlines = (sbyte)-nlines;
            nlines = (byte) -nlines;
            start = (byte) (rlr - nlines + 1);
        }
        for (; nlines > 0; nlines--) {
            switch (INT10Mode.CurMode.Type) {
                case TEXT:
                    textFillRow(0xff & cul, 0xff & clr, 0xff & start, _base, 0xff & attr);
                    break;
                case CGA2:
                    cga2FillRow(0xff & cul, 0xff & clr, 0xff & start, _base, 0xff & attr);
                    break;
                case CGA4:
                    cga4FillRow(0xff & cul, 0xff & clr, 0xff & start, _base, 0xff & attr);
                    break;
                case TANDY16:
                    tandy16FillRow(0xff & cul, 0xff & clr, 0xff & start, _base, 0xff & attr);
                    break;
                case EGA:
                    ega16FillRow(0xff & cul, 0xff & clr, 0xff & start, _base, 0xff & attr);
                    break;
                case VGA:
                    vgaFillRow(0xff & cul, 0xff & clr, 0xff & start, _base, 0xff & attr);
                    break;
                case LIN4:
                    if ((DOSBox.Machine == DOSBox.MachineType.VGA)
                            && (DOSBox.SVGACard == DOSBox.SVGACards.TsengET4K)
                            && (INT10Mode.CurMode.SWidth <= 800)) {
                        ega16FillRow(0xff & cul, 0xff & clr, 0xff & start, _base, 0xff & attr);
                        break;
                    }
                    // goto Gotodefault;
                    // fall-through
                default:
                    // Gotodefault:
                    Log.logging(Log.LogTypes.INT10, Log.LogServerities.Error,
                            "Unhandled mode %d for scroll", INT10Mode.CurMode.Type.toString());
                    break;
            }
            start++;
        }
    }

    public static void setActivePage(int page) {
        int mem_address;
        if (page > 7)
            Log.logging(Log.LogTypes.INT10, Log.LogServerities.Error, "INT10_SetActivePage page %d",
                    page);

        if (DOSBox.isEGAVGAArch() && (DOSBox.SVGACard == DOSBox.SVGACards.S3Trio))
            page &= 7;

        mem_address =
                0xffff & (page * Memory.realReadW(INT10.BIOSMEM_SEG, INT10.BIOSMEM_PAGE_SIZE));
        /* Write the new page start */
        Memory.realWriteW(INT10.BIOSMEM_SEG, INT10.BIOSMEM_CURRENT_START, mem_address);
        if (DOSBox.isEGAVGAArch()) {
            if (INT10Mode.CurMode.Mode < 8)
                mem_address >>>= 1;
            // rare alternative: if (CurMode.type==M_TEXT) mem_address>>>=1;
        } else {
            mem_address >>>= 1;
        }
        /* Write the new start address in vgahardware */
        int _base = Memory.realReadW(INT10.BIOSMEM_SEG, INT10.BIOSMEM_CRTC_ADDRESS);
        IO.write(_base, 0x0c);
        IO.write(_base + 1, mem_address >>> 8);
        IO.write(_base, 0x0d);
        IO.write(_base + 1, mem_address);

        // And change the BIOS page
        Memory.realWriteB(INT10.BIOSMEM_SEG, INT10.BIOSMEM_CURRENT_PAGE, page);
        int curRow = INT10.getCursorPosRow(page);// uint8
        int curCol = INT10.getCursorPosCol(page);// uint8
        // Display the cursor, now the page is active
        setCursorPos(curRow, curCol, page);
    }

    // (byte, byte)
    public static void setCursorShape(int first, int last) {
        Memory.realWriteW(INT10.BIOSMEM_SEG, INT10.BIOSMEM_CURSOR_TYPE, last | (first << 8));
        if (DOSBox.Machine == DOSBox.MachineType.CGA) {
            doWrite(first, last); // goto dowrite;
            return;
        }
        if (DOSBox.isTANDYArch()) {
            doWrite(first, last); // goto dowrite;
            return;
        }
        /* Skip CGA cursor emulation if EGA/VGA system is active */
        if ((Memory.realReadB(INT10.BIOSMEM_SEG, INT10.BIOSMEM_VIDEO_CTL) & 0x8) == 0) {
            /* Check for CGA type 01, invisible */
            if ((first & 0x60) == 0x20) {
                first = 0x1e;
                last = 0x00;
                doWrite(first, last); // goto dowrite;
                return;
            }
            /* Check if we need to convert CGA Bios cursor values */

            // set by int10 fun12 sub34 if (CurMode.mode>0x3) goto dowrite;
            // Only mode 0-3 are text modes on cga
            if ((Memory.realReadB(INT10.BIOSMEM_SEG, INT10.BIOSMEM_VIDEO_CTL) & 0x1) == 0) {
                if ((first & 0xe0) != 0 || (last & 0xe0) != 0) {
                    doWrite(first, last); // goto dowrite;
                    return;
                }
                int cheight =
                        0xff & (Memory.realReadB(INT10.BIOSMEM_SEG, INT10.BIOSMEM_CHAR_HEIGHT) - 1);
                /* Creative routine i based of the original ibmvga bios */

                if (last < first) {
                    if (last == 0) {
                        doWrite(first, last); // goto dowrite;
                        return;
                    }
                    first = last;
                    last = cheight;
                    /* Test if this might be a cga style cursor set, if not don't do anything */
                } else if (((first | last) >= cheight) || !(last == (cheight - 1))
                        || !(first == cheight)) {
                    if (last <= 3) {
                        doWrite(first, last); // goto dowrite;
                        return;
                    }
                    if (first + 2 < last) {
                        if (first > 2) {
                            first = (byte) ((cheight + 1) / 2);
                            last = cheight;
                        } else {
                            last = cheight;
                        }
                    } else {
                        first = (byte) ((first - last) + cheight);
                        last = cheight;

                        if (cheight > 0xc) { // vgatest sets 15 15 2x where only one should be
                                             // decremented to 14 14
                            first--; // implementing int10 fun12 sub34 fixed this.
                            last--;
                        }
                    }
                }

            }
        }
        // dowrite:
        doWrite(first, last);
        // short _base = Memory.RealReadW(INT10.BIOSMEM_SEG, INT10.BIOSMEM_CRTC_ADDRESS);
        // IO.Write(_base, 0xa); IO.Write(_base + 1, first);
        // IO.Write(_base, 0xb); IO.Write(_base + 1, last);
    }

    // dowrite:
    // (int, int)
    private static void doWrite(int first, int last) {
        int _base = Memory.realReadW(INT10.BIOSMEM_SEG, INT10.BIOSMEM_CRTC_ADDRESS);
        IO.write(_base, 0xa);
        IO.write(_base + 1, first);
        IO.write(_base, 0xb);
        IO.write(_base + 1, last);
    }

    // (uint8,uint8,uint8)
    public static void setCursorPos(int row, int col, int page) {
        int address;

        if (page > 7)
            Log.logging(Log.LogTypes.INT10, Log.LogServerities.Error, "INT10_SetCursorPos page %d",
                    page);
        // Bios cursor pos
        Memory.realWriteB(INT10.BIOSMEM_SEG, (INT10.BIOSMEM_CURSOR_POS + page * 2), col);
        Memory.realWriteB(INT10.BIOSMEM_SEG, (INT10.BIOSMEM_CURSOR_POS + page * 2 + 1), row);
        // Set the hardware cursor
        int current = Memory.realReadB(INT10.BIOSMEM_SEG, INT10.BIOSMEM_CURRENT_PAGE);
        if (page == current) {
            // Get the dimensions
            int ncols = Memory.realReadW(INT10.BIOSMEM_SEG, INT10.BIOSMEM_NB_COLS);;
            // Calculate the address knowing nbcols nbrows and page num
            // NOTE: INT10.BIOSMEM_CURRENT_START counts in colour/flag pairs
            address = 0xffff & ((ncols * row) + col
                    + Memory.realReadW(INT10.BIOSMEM_SEG, INT10.BIOSMEM_CURRENT_START) / 2);
            // CRTC regs 0x0e and 0x0f
            int _base = Memory.realReadW(INT10.BIOSMEM_SEG, INT10.BIOSMEM_CRTC_ADDRESS);
            IO.write(_base, 0x0e);
            IO.write(_base + 1, 0xff & (address >>> 8));
            IO.write(_base, 0x0f);
            IO.write(_base + 1, 0xff & address);
        }
    }

    // uint16 ReadCharAttr
    // public static int ReadCharAttr(short col, short row, byte page) {
    public static int readCharAttr(int col, int row, int page) {
        int result;
        /* Externally used by the mouse routine */
        int fontdata;
        int x, y;
        int cheight = Memory.realReadB(INT10.BIOSMEM_SEG, INT10.BIOSMEM_CHAR_HEIGHT);
        boolean split_chr = false;
        switch (INT10Mode.CurMode.Type) {
            case TEXT: {
                // Compute the address
                int address = page * Memory.realReadW(INT10.BIOSMEM_SEG, INT10.BIOSMEM_PAGE_SIZE);
                address +=
                        row * Memory.realReadW(INT10.BIOSMEM_SEG, INT10.BIOSMEM_NB_COLS + col) * 2;
                // read the char
                int where = INT10Mode.CurMode.PStart + address;
                result = Memory.readW(where);
            }
                return result;
            case CGA4:
            case CGA2:
            case TANDY16:
                split_chr = true;
                // goto Gotodefault;
                /* Fallthrough */
            default: /* EGA/VGA don't have a split font-table */
                // Gotodefault:
                for (short chr = 0; chr <= 255; chr++) {
                    if (!split_chr || (chr < 128))
                        fontdata = Memory.real2Phys(Memory.realGetVec(0x43)) + chr * cheight;
                    else
                        fontdata =
                                Memory.real2Phys(Memory.realGetVec(0x1F)) + (chr - 128) * cheight;

                    x = 8 * col;
                    y = cheight * row;
                    boolean error = false;
                    for (byte h = 0; h < cheight; h++) {
                        int bitsel = 128;
                        int bitline = Memory.readB(fontdata++);
                        byte res = 0;
                        byte vidline = 0;
                        int tx = x;
                        while (bitsel != 0) {
                            // Construct bitline in memory
                            res = INT10.getPixel(tx, y, page);
                            if (res != 0)
                                vidline |= bitsel;
                            tx++;
                            bitsel >>>= 1;
                        }
                        y++;
                        if (bitline != vidline) {
                            /* It's not character 'chr', move on to the next */
                            error = true;
                            break;
                        }
                    }
                    if (!error) {
                        /* We found it */
                        return result = chr;
                    }
                }
                Log.logging(Log.LogTypes.INT10, Log.LogServerities.Error,
                        "ReadChar didn't find character");
                return result = 0;
        }
    }

    // uint16 ReadCharAttr
    public static int readCharAttr(int page) {
        if (page == 0xFF)
            page = Memory.realReadB(INT10.BIOSMEM_SEG, INT10.BIOSMEM_CURRENT_PAGE);
        int curRow = INT10.getCursorPosRow(page);
        int curCol = INT10.getCursorPosCol(page);
        return readCharAttr(curCol, curRow, page);
    }

    private static boolean warned_use = false;

    public static void writeChar1(int col, int row, int page, byte chr, int attr, boolean useattr) {
        /* Externally used by the mouse routine */
        int fontdata;
        int x, y;
        int cheight = Memory.realReadB(INT10.BIOSMEM_SEG, INT10.BIOSMEM_CHAR_HEIGHT);
        switch (INT10Mode.CurMode.Type) {
            case TEXT: {
                // Compute the address
                int address = page * Memory.realReadW(INT10.BIOSMEM_SEG, INT10.BIOSMEM_PAGE_SIZE);
                address += (row * Memory.realReadW(INT10.BIOSMEM_SEG, INT10.BIOSMEM_NB_COLS) + col)
                        * 2;
                // Write the char
                int where = INT10Mode.CurMode.PStart + address;
                Memory.writeB(where, chr);
                if (useattr) {
                    Memory.writeB(where + 1, attr);
                }
            }
                return;
            case CGA4:
            case CGA2:
            case TANDY16:
                if (chr < 128)
                    fontdata = Memory.realGetVec(0x43);
                else {
                    chr -= 128;
                    fontdata = Memory.realGetVec(0x1f);
                }
                fontdata = Memory.realMake(Memory.realSeg(fontdata),
                        Memory.realOff(fontdata) + chr * cheight);
                break;
            default:
                fontdata = Memory.realGetVec(0x43);
                fontdata = Memory.realMake(Memory.realSeg(fontdata),
                        Memory.realOff(fontdata) + chr * cheight);
                break;
        }

        if (!useattr) { // Set attribute(color) to a sensible value

            if (!warned_use) {
                Log.logging(Log.LogTypes.INT10, Log.LogServerities.Error,
                        "writechar used without attribute in non-textmode %c %X", chr, chr);
                warned_use = true;
            }
            switch (INT10Mode.CurMode.Type) {
                case CGA4:
                    attr = 0x3;
                    break;
                case CGA2:
                    attr = 0x1;
                    break;
                case TANDY16:
                case EGA:
                default:
                    attr = 0xf;
                    break;
            }
        }

        // Some weird behavior of mode 6 (and 11)
        if ((INT10Mode.CurMode.Mode == 0x6)/* || (CurMode.mode==0x11) */)
            attr = (attr & 0x80) | 1;
        // (same fix for 11 fixes vgatest2, but it's not entirely correct according to wd)

        x = 8 * col;
        y = cheight * row;
        int xorMask = (INT10Mode.CurMode.Type == VGAModes.VGA) ? 0x0 : 0x80;
        // TODO Check for out of bounds
        if (INT10Mode.CurMode.Type == VGAModes.EGA) {
            /* enable all planes for EGA modes (Ultima 1 colour bug) */
            /*
             * might be put into INT10_PutPixel but different vga bios implementations have
             * different opinions about this
             */
            IO.write(0x3c4, 0x2);
            IO.write(0x3c5, 0xf);
        }
        int bitsel = 0;
        int bitline = 0;
        for (byte h = 0; h < cheight; h++) {
            bitsel = 128;
            bitline = Memory.readB(Memory.real2Phys(fontdata));
            fontdata = Memory.realMake(Memory.realSeg(fontdata), Memory.realOff(fontdata) + 1);
            int tx = x;
            while (bitsel != 0) {
                if ((bitline & bitsel) != 0)
                    INT10.putPixel(tx, y, page, (byte) attr);
                else
                    INT10.putPixel(tx, y, page, (byte) (attr & xorMask));
                tx++;
                bitsel >>>= 1;
            }
            y++;
        }
    }

    // (Bit8u chr,Bit8u attr,Bit8u page,Bit16u count,bool showattr)
    public static void writeChar2(byte chr, int attr, int page, int count, boolean showattr) {
        if (INT10Mode.CurMode.Type != VGAModes.TEXT) {
            showattr = true; // Use attr in graphics mode always
            switch (DOSBox.Machine) {
                case EGA:
                case VGA:
                    page = 0xff & (page % INT10Mode.CurMode.PTotal);
                    break;
                case CGA:
                case PCJR:
                    page = 0;
                    break;
            }
        }

        int cur_row = INT10.getCursorPosRow(page);
        int cur_col = INT10.getCursorPosCol(page);
        int ncols = Memory.realReadW(INT10.BIOSMEM_SEG, INT10.BIOSMEM_NB_COLS);;
        while (count > 0) {
            writeChar1(cur_col, cur_row, page, chr, attr, showattr);
            count--;
            cur_col++;
            if (cur_col == ncols) {
                cur_col = 0;
                cur_row++;
            }
        }
    }

    public static void teletypeOutputAttr(byte chr, int attr, boolean useAttr, int page) {
        int ncols = Memory.realReadW(INT10.BIOSMEM_SEG, INT10.BIOSMEM_NB_COLS);
        int nrows = Memory.realReadB(INT10.BIOSMEM_SEG, INT10.BIOSMEM_NB_ROWS) + 1;
        int curRow = INT10.getCursorPosRow(page);// uint8
        int curCol = INT10.getCursorPosCol(page);// uint8
        switch (chr) {
            case 7:
                // TODO BEEP
                break;
            case 8:
                if (curCol > 0)
                    curCol--;
                break;
            case (byte) '\r':
                curCol = 0;
                break;
            case (byte) '\n':
                // cur_col=0; //Seems to break an old chess game
                curRow++;
                break;
            case (byte) '\t':
                do {
                    teletypeOutputAttr((byte) ' ', attr, useAttr, page);
                    curRow = INT10.getCursorPosRow(page);
                    curCol = INT10.getCursorPosCol(page);
                } while ((curCol % 8) != 0);
                break;
            default:
                /* Draw the actual Character */
                writeChar1(curCol, curRow, page, chr, attr, useAttr);
                curCol++;
                break;
        }
        if (curCol == ncols) {
            curCol = 0;
            curRow++;
        }
        // Do we need to scroll ?
        if (curRow == nrows) {
            // Fill with black on non-text modes and with 0x7 on textmode
            int fill = (INT10Mode.CurMode.Type == VGAModes.TEXT) ? 0x7 : 0;
            scrollWindow(0, 0, 0xff & (nrows - 1), 0xff & (ncols - 1), -1, fill, page);
            curRow--;
        }
        // Set the cursor for the page
        setCursorPos(curRow, curCol, page);
    }

    public static void teletypeOutputAttr(byte chr, int attr, boolean useAttr) {
        teletypeOutputAttr(chr, attr, useAttr,
                Memory.realReadB(INT10.BIOSMEM_SEG, INT10.BIOSMEM_CURRENT_PAGE));
    }

    public static void teletypeOutput(byte chr, int attr) {
        teletypeOutputAttr(chr, attr, INT10Mode.CurMode.Type != VGAModes.TEXT);
    }

    public static void writeString(int row, int col, int flag, int attr, int str, int count,
            int page) {
        int curRow = INT10.getCursorPosRow(page);
        int curCol = INT10.getCursorPosCol(page);

        // if row=0xff special case : use current cursor position
        if (row == 0xff) {
            row = curRow;
            col = curCol;
        }
        setCursorPos(row, col, page);
        byte chr = 0;
        while (count > 0) {
            chr = (byte) Memory.readB(str);
            str++;
            if ((flag & 2) != 0) {
                attr = Memory.readB(str);
                str++;
            }
            teletypeOutputAttr(chr, attr, true, page);
            count--;
        }
        if ((flag & 1) == 0) {
            setCursorPos(curRow, curCol, page);
        }
    }
}
/*--------------------------- end INT10Char -----------------------------*/
