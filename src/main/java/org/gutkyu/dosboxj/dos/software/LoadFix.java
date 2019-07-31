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
        if (Cmd.findCommand(commandNr)) {
            TempLine = Cmd.returnedCmd;
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
        if (DOSMain.tryAllocateMemory(blocks)) {
            segment = DOSMain.returnedAllocateMemorySeg;
            blocks = DOSMain.returnedAllocateMemoryBlock;

            DOSMCB mcb = new DOSMCB(segment - 1);
            mcb.setPSPSeg(0x40); // use fake segment
            writeOut(Message.get("PROGRAM_LOADFIX_ALLOC"), kb);
            // Prepare commandline...
            if (Cmd.findCommand(commandNr++)) {
                TempLine = Cmd.returnedCmd;
                // get Filename
                CStringPt filename = CStringPt.create(128);
                CStringPt.safeCopy(TempLine, filename, 128);
                // Setup commandline
                boolean ok;
                CStringPt args = CStringPt.create(256);
                args.set(0, (char) 0);
                do {
                    ok = Cmd.findCommand(commandNr++);
                    TempLine = ok ? Cmd.returnedCmd : TempLine;
                    if (args.length() - args.length() - 1 < TempLine.length() + 1)
                        break;
                    args.concat(TempLine);
                    args.concat(" ");
                } while (ok);
                // Use shell to start program
                DOSShell shell = new DOSShell();
                shell.execute(filename.toString(), args.toString());
                DOSMain.freeMemory(segment);
                writeOut(Message.get("PROGRAM_LOADFIX_DEALLOC"), kb);
            }
        } else {
            writeOut(Message.get("PROGRAM_LOADFIX_ERROR"), kb);
        }
    }
}
