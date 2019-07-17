package org.gutkyu.dosboxj.dos.software;

import org.gutkyu.dosboxj.misc.*;
import org.gutkyu.dosboxj.dos.DOSMain;
import org.gutkyu.dosboxj.dos.mem_block.*;
import org.gutkyu.dosboxj.util.*;

public final class Intro extends Program {
    public static Program makeProgram() {
        return new Intro();
    }

    public void displayMount() {
        /*
         * Basic mounting has a version for each operating system. This is done this way so both
         * messages appear in the language file
         */
        writeOut(Message.get("PROGRAM_INTRO_MOUNT_START"));
        writeOut(Message.get("PROGRAM_INTRO_MOUNT_WINDOWS"));

        writeOut(Message.get("PROGRAM_INTRO_MOUNT_END"));
    }

    @Override
    public void run() {
        /* Only run if called from the first shell (Xcom TFTD runs any intro file in the path) */
        if ((new DOSPSP(DOSMain.DOS.getPSP()))
                .getParent() != (new DOSPSP(new DOSPSP(DOSMain.DOS.getPSP()).getParent())).getParent())
            return;
        if (Cmd.findExist("cdrom", false)) {
            writeOut(Message.get("PROGRAM_INTRO_CDROM"));
            return;
        }
        if (Cmd.findExist("mount", false)) {
            writeOut("\u001B[2J");// Clear screen before printing
            displayMount();
            return;
        }
        if (Cmd.findExist("special", false)) {
            writeOut(Message.get("PROGRAM_INTRO_SPECIAL"));
            return;
        }
        /* Default action is to show all pages */
        writeOut(Message.get("PROGRAM_INTRO"));
        byte c = 0;
        short n = 1;
        U8Ref refChar = new U8Ref(c, n);
        DOSMain.readFile(DOSMain.STDIN, refChar);
        displayMount();
        DOSMain.readFile(DOSMain.STDIN, refChar);
        writeOut(Message.get("PROGRAM_INTRO_CDROM"));
        DOSMain.readFile(DOSMain.STDIN, refChar);
        writeOut(Message.get("PROGRAM_INTRO_SPECIAL"));
    }
}
