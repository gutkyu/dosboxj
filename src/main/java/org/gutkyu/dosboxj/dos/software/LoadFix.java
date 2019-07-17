package org.gutkyu.dosboxj.dos.software;

import org.gutkyu.dosboxj.misc.*;
import org.gutkyu.dosboxj.dos.DOSMain;
import org.gutkyu.dosboxj.dos.mem_block.*;
import org.gutkyu.dosboxj.util.*;
import org.gutkyu.dosboxj.shell.*;

public final class LoadFix extends Program {
    public static Program makeProgram() {
        return new LoadFix();
    }

    @Override
    public void run() {
        short commandNr = 1;
        short kb = 64;
        if ((TempLine = Cmd.findCommand(commandNr)) != null) {
            if (TempLine.charAt(0) == '-') {
                char ch = TempLine.charAt(1);
                if ((Character.toUpperCase(ch) == 'D') || (Character.toUpperCase(ch) == 'F')) {
                    // Deallocate all
                    DOSMain.freeProcessMemory(0x40);
                    writeOut(Message.get("PROGRAM_LOADFIX_DEALLOCALL"), kb);
                    return;
                } else {
                    // Set mem amount to allocate
                    kb = Short.parseShort(TempLine.substring(1));
                    if (kb == 0)
                        kb = 64;
                    commandNr++;
                }
            }
        }
        // Allocate Memory
        int segment = 0;
        int blocks = (kb * 1024 / 16);
        RefU32Ret refSize = new RefU32Ret(blocks);
        RefU32Ret refSeg = new RefU32Ret(segment);
        if (DOSMain.allocateMemory(refSeg, refSize)) {
            segment = refSeg.U32;
            blocks = refSize.U32;

            DOSMCB mcb = new DOSMCB(segment - 1);
            mcb.setPSPSeg(0x40); // use fake segment
            writeOut(Message.get("PROGRAM_LOADFIX_ALLOC"), kb);
            // Prepare commandline...
            if ((TempLine = Cmd.findCommand(commandNr++)) != null) {
                // get Filename
                CStringPt filename = CStringPt.create(128);
                CStringPt.safeCopy(TempLine, filename, 128);
                // Setup commandline
                boolean ok;
                CStringPt args = CStringPt.create(256);
                args.set(0, (char) 0);
                do {
                    ok = (TempLine = Cmd.findCommand(commandNr++)) != null;
                    if (args.length() - args.length() - 1 < TempLine.length() + 1)
                        break;
                    args.concat(TempLine);
                    args.concat(" ");
                } while (ok);
                // Use shell to start program
                DOSShell shell = new DOSShell();
                shell.execute(filename, args);
                DOSMain.freeMemory(segment);
                writeOut(Message.get("PROGRAM_LOADFIX_DEALLOC"), kb);
            }
        } else {
            writeOut(Message.get("PROGRAM_LOADFIX_ERROR"), kb);
        }
    }
}
