package org.gutkyu.dosboxj.dos.software;



import org.gutkyu.dosboxj.dos.*;
import org.gutkyu.dosboxj.misc.*;

public final class Rescan extends Program {
    public static Program makeProgram() {
        return new Rescan();
    }

    @Override
    public void run() { // Get current drive
        int drive = DOSMain.getDefaultDrive();
        if (DOSMain.Drives[drive] != null) {
            DOSMain.Drives[drive].emptyCache();
            writeOut(Message.get("PROGRAM_RESCAN_SUCCESS"));
        }
    }

}
