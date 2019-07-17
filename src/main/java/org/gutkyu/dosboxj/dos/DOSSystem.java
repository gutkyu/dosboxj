package org.gutkyu.dosboxj.dos;

import java.util.LinkedList;
import org.gutkyu.dosboxj.cpu.*;
import org.gutkyu.dosboxj.hardware.memory.*;
import org.gutkyu.dosboxj.util.*;

public final class DOSSystem {
    public static final int DOS_NAMELENGTH = 12;
    public static final int DOS_NAMELENGTH_ASCII = (DOS_NAMELENGTH + 1);
    public static final byte DOS_FCBNAME = 15;
    public static final int DOS_DIRDEPTH = 8;
    public static final int DOS_PATHLENGTH = 80;
    public static final int DOS_TEMPSIZE = 1024;

    public static final byte DOS_ATTR_READ_ONLY = 0x01;
    public static final byte DOS_ATTR_HIDDEN = 0x02;
    public static final byte DOS_ATTR_SYSTEM = 0x04;
    public static final byte DOS_ATTR_VOLUME = 0x08;
    public static final byte DOS_ATTR_DIRECTORY = 0x10;
    public static final byte DOS_ATTR_ARCHIVE = 0x20;
    public static final byte DOS_ATTR_DEVICE = 0x40;

    public static final short OPEN_READ = 0, OPEN_WRITE = 1, OPEN_READWRITE = 2,
            DOS_NOT_INHERIT = 128;
    public static final int DOS_SEEK_SET = 0, DOS_SEEK_CUR = 1, DOS_SEEK_END = 2;

    /*
     * The following variable can be lowered to free up some memory. The negative side effect: The
     * stored searches will be turned over faster. Should not have impact on systems with few
     * directory entries.
     */
    public static final int MAX_OPENDIRS = 2048;
    // Can be high as it's only storage (16 bit variable)

    private static int call_int2f, call_int2a;

    private static LinkedList<MultiplexHandler> Multiplex = new LinkedList<MultiplexHandler>();

    public static void addMultiplexHandler(MultiplexHandler handler) {
        Multiplex.addFirst(handler);
    }

    public static void delMultiplexHandler(MultiplexHandler handler) {
        Multiplex.remove(handler);
    }

    private static int INT2FHandler() {
        for (MultiplexHandler handler : Multiplex) {
            if (handler.run())
                return Callback.ReturnTypeNone;
        }

        Log.logging(Log.LogTypes.DOSMISC, Log.LogServerities.Error,
                "DOS:Multiplex Unhandled call %4X", Register.getRegAX());
        return Callback.ReturnTypeNone;
    }


    private static int INT2AHandler() {
        return Callback.ReturnTypeNone;
    }

    private static boolean multiplexFunctions() {
        switch (Register.getRegAX()) {
            case 0x1216: /* GET ADDRESS OF SYSTEM FILE TABLE ENTRY */
                // regsModule.reg_bx is a system file table entry, should coincide with
                // the file handle so just use that
                Log.logging(Log.LogTypes.DOSMISC, Log.LogServerities.Error,
                        "Some BAD filetable call used bx=%X", Register.getRegBX());
                if (Register.getRegBX() <= DOSMain.DOS_FILES)
                    Callback.scf(false);
                else
                    Callback.scf(true);
                if (Register.getRegBX() < 16) {
                    int sftrealpt =
                            Memory.readD(Memory.real2Phys(DOSMain.DOSInfoBlock.getPointer()) + 4);
                    int sftptr = Memory.real2Phys(sftrealpt);
                    int sftofs = 0x06 + Register.getRegBX() * 0x3b;

                    if (DOSMain.Files[Register.getRegBX()] != null)
                        Memory.writeB(sftptr + sftofs, DOSMain.Files[Register.getRegBX()].RefCtr);
                    else
                        Memory.writeB(sftptr + sftofs, 0);

                    if (DOSMain.Files[Register.getRegBX()] == null)
                        return true;

                    int handle = DOSMain.realHandle(Register.getRegBX());
                    if (handle >= DOSMain.DOS_FILES) {
                        Memory.writeW(sftptr + sftofs + 0x02, 0x02); // file open mode
                        Memory.writeB(sftptr + sftofs + 0x04, 0x00); // file attribute
                        Memory.writeW(sftptr + sftofs + 0x05,
                                DOSMain.Files[Register.getRegBX()].getInformation()); // device info
                        Memory.writeD(sftptr + sftofs + 0x07, 0); // device driver header
                        Memory.writeW(sftptr + sftofs + 0x0d, 0); // packed time
                        Memory.writeW(sftptr + sftofs + 0x0f, 0); // packed date
                        Memory.writeW(sftptr + sftofs + 0x11, 0); // size
                        Memory.writeW(sftptr + sftofs + 0x15, 0); // current position
                    } else {
                        byte drive = DOSMain.Files[Register.getRegBX()].getDrive();

                        Memory.writeW(sftptr + sftofs + 0x02,
                                DOSMain.Files[Register.getRegBX()].Flags & 3); // file open mode
                        Memory.writeB(sftptr + sftofs + 0x04,
                                DOSMain.Files[Register.getRegBX()].Attr); // file attribute
                        Memory.writeW(sftptr + sftofs + 0x05, 0x40 | drive); // device info word
                        Memory.writeD(sftptr + sftofs + 0x07,
                                Memory.realMake(DOSMain.DOS.tables.DPB, drive)); // dpb of the drive
                        Memory.writeW(sftptr + sftofs + 0x0d,
                                DOSMain.Files[Register.getRegBX()].Time); // packed file time
                        Memory.writeW(sftptr + sftofs + 0x0f,
                                DOSMain.Files[Register.getRegBX()].Date); // packed file date
                        long curPos = 0;
                        curPos = DOSMain.Files[Register.getRegBX()].seek(curPos,
                                DOSSystem.DOS_SEEK_CUR);
                        long endPos = 0;
                        endPos = DOSMain.Files[Register.getRegBX()].seek(endPos,
                                DOSSystem.DOS_SEEK_END);
                        Memory.writeD(sftptr + sftofs + 0x11, (int) endPos); // size
                        Memory.writeD(sftptr + sftofs + 0x15, (int) curPos); // current position
                        curPos = DOSMain.Files[Register.getRegBX()].seek(curPos,
                                DOSSystem.DOS_SEEK_SET);
                    }

                    // fill in filename in fcb style
                    // (space-padded name (8 chars)+space-padded extension (3 chars))
                    CStringPt filename = DOSMain.Files[Register.getRegBX()].getName();
                    if (!filename.lastPositionOf('\\').isEmpty())
                        filename = CStringPt.clone(filename.lastPositionOf('\\'), 1);
                    if (!filename.lastPositionOf('/').isEmpty())
                        filename = CStringPt.clone(filename.lastPositionOf('/'), 1);
                    if (filename.isEmpty())
                        return true;
                    CStringPt dotpos = filename.lastPositionOf('.');
                    if (!dotpos.isEmpty()) {
                        dotpos.movePtToR1();
                        int nlen = filename.length();
                        int extlen = dotpos.length();
                        int nmelen = (int) nlen - (int) extlen;
                        if (nmelen < 1)
                            return true;
                        nlen -= (extlen + 1);

                        if (nlen > 8)
                            nlen = 8;
                        int i;

                        for (i = 0; i < nlen; i++)
                            Memory.writeB(sftptr + sftofs + 0x20 + i, filename.get(i));
                        for (i = nlen; i < 8; i++)
                            Memory.writeB(sftptr + sftofs + 0x20 + i, (byte) ' ');

                        if (extlen > 3)
                            extlen = 3;
                        for (i = 0; i < extlen; i++)
                            Memory.writeB(sftptr + sftofs + 0x28 + i, (byte) dotpos.get(i));
                        for (i = extlen; i < 3; i++)
                            Memory.writeB(sftptr + sftofs + 0x28 + i, (byte) ' ');
                    } else {
                        int i;
                        int nlen = filename.length();
                        if (nlen > 8)
                            nlen = 8;
                        for (i = 0; i < nlen; i++)
                            Memory.writeB(sftptr + sftofs + 0x20 + i, (byte) filename.get(i));
                        for (i = nlen; i < 11; i++)
                            Memory.writeB(sftptr + sftofs + 0x20 + i, (byte) ' ');
                    }

                    Register.segSet16(Register.SEG_NAME_ES, Memory.realSeg(sftrealpt));
                    Register.setRegDI(Memory.realOff(sftrealpt + sftofs));
                    Register.setRegAX(0xc000);

                }
                return true;
            case 0x1607:
                if (Register.getRegBX() == 0x15) {
                    switch (Register.getRegCX()) {
                        case 0x0000: // query instance
                            Register.setRegCX(0x0001);
                            Register.setRegDX(0x50); // dos driver segment
                            Register.segSet16(Register.SEG_NAME_ES, 0x50); // patch table seg
                            Register.setRegBX(0x60); // patch table ofs
                            return true;
                        case 0x0001: // set patches
                            Register.setRegAX(0xb97c);
                            Register.setRegBX(Register.getRegDX() & 0x16);
                            Register.setRegDX(0xa2ab);
                            return true;
                        case 0x0003: // get size of data struc
                            if (Register.getRegDX() == 0x0001) {
                                // CDS size requested
                                Register.setRegAX(0xb97c);
                                Register.setRegDX(0xa2ab);
                                Register.setRegCX(0x000e); // size
                            }
                            return true;
                        case 0x0004: // instanced data
                            Register.setRegDX(0); // none
                            return true;
                        case 0x0005: // get device driver size
                            Register.setRegAX(0);
                            Register.setRegDX(0);
                            return true;
                        default:
                            return false;
                    }
                } else if (Register.getRegBX() == 0x18)
                    return true; // idle callout
                else
                    return false;
            case 0x1680: /* RELEASE CURRENT VIRTUAL MACHINE TIME-SLICE */
                // TODO Maybe do some idling but could screw up other systems :)
                return true; // So no warning in the debugger anymore
            case 0x1689: /* Kernel IDLE CALL */
            case 0x168f: /* Close awareness crap */
                /* Removing warning */
                return true;
            case 0x4a01: /* Query free hma space */
            case 0x4a02: /* ALLOCATE HMA SPACE */
                Log.logging(Log.LogTypes.DOSMISC, Log.LogServerities.Warn,
                        "INT 2f:4a HMA. DOSBox reports none available.");
                // number of bytes available in HMA or amount successfully allocated
                Register.setRegBX(0);
                // ESDI=ffff:ffff Location of HMA/Allocated memory
                Register.segSet16(Register.SEG_NAME_ES, 0xffff);
                Register.setRegDI(0xffff);
                return true;
        }

        return false;
    }

    public static void setupMisc() {
        /* Setup the dos multiplex interrupt */
        call_int2f = Callback.allocate();
        Callback.setup(call_int2f, DOSSystem::INT2FHandler, Callback.Symbol.IRET, "DOS Int 2f");
        Memory.realSetVec(0x2f, Callback.realPointer(call_int2f));
        addMultiplexHandler(DOSSystem::multiplexFunctions);
        /* Setup the dos network interrupt */
        call_int2a = Callback.allocate();
        Callback.setup(call_int2a, DOSSystem::INT2AHandler, Callback.Symbol.IRET, "DOS Int 2a");
        Memory.realSetVec(0x2A, Callback.realPointer(call_int2a));
    }
}
