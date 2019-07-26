package org.gutkyu.dosboxj.dos;

import org.gutkyu.dosboxj.misc.setup.*;
import org.gutkyu.dosboxj.cpu.*;
import org.gutkyu.dosboxj.dos.mem_block.*;
import org.gutkyu.dosboxj.hardware.*;
import org.gutkyu.dosboxj.hardware.memory.*;
import org.gutkyu.dosboxj.interrupt.bios.*;
import java.time.LocalDateTime;

public final class DOSModule extends ModuleBase {
    private CallbackHandlerObject[] callback = new CallbackHandlerObject[7];

    public DOSModule(Section configuration) {
        super(configuration);
        for (int i = 0; i < callback.length; i++) {
            callback[i] = new CallbackHandlerObject();
        }
        callback[0].install(DOSMain::INT20Handler, Callback.Symbol.IRET, "DOS Int 20");
        callback[0].setRealVec(0x20);

        callback[1].install(DOSMain::INT21Handler, Callback.Symbol.INT21, "DOS Int 21");
        callback[1].setRealVec(0x21);
        // Pseudo code for int 21
        // sti
        // callback
        // iret
        // retf <- int 21 4c jumps here to mimic a retf Cyber

        callback[2].install(DOSMain::INT25Handler, Callback.Symbol.RETF, "DOS Int 25");
        callback[2].setRealVec(0x25);

        callback[3].install(DOSMain::INT26Handler, Callback.Symbol.RETF, "DOS Int 26");
        callback[3].setRealVec(0x26);

        callback[4].install(DOSMain::INT27Handler, Callback.Symbol.IRET, "DOS Int 27");
        callback[4].setRealVec(0x27);

        callback[5].install(null, Callback.Symbol.IRET, "DOS Int 28");
        callback[5].setRealVec(0x28);

        callback[6].install(null, Callback.Symbol.INT29, "CON Output Int 29");
        callback[6].setRealVec(0x29);
        // pseudocode for CB_INT29:
        // push ax
        // mov ah, 0x0e
        // int 0x10
        // pop ax
        // iret

        DOSMain.setupFiles(); /* Setup system File tables */
        DOSMain.setupDevices(); /* Setup dos devices */
        DOSMain.setupTables();
        DOSMain.setupMemory(); /* Setup first MCB */
        DOSMain.setupPrograms();
        DOSSystem.setupMisc(); /* Some additional dos interrupts */
        (new DOSSDA(DOSMain.DOS_SDA_SEG, DOSMain.DOS_SDA_OFS))
                .setDrive(25); /* Else the next call gives a warning. */
        DOSMain.setDefaultDrive(25);

        DOSMain.DOS.Version.major = 5;
        DOSMain.DOS.Version.minor = 0;

        /* Setup time and date */
        LocalDateTime localtime = LocalDateTime.now();

        DOSMain.DOS.Date.Day = 0xff & localtime.getDayOfMonth();
        DOSMain.DOS.Date.Month = 0xff & localtime.getMonthValue();
        DOSMain.DOS.Date.Year = 0xffff & localtime.getYear();
        int ticks = (int) ((localtime.getHour() * 3600 + localtime.getMinute() * 60
                + localtime.getSecond()) * (float) Timer.PIT_TICK_RATE / 65536.0);
        Memory.writeD(BIOS.BIOS_TIMER, ticks);
    }

    // TODO dosbox 소스는 callback를 해제하지 않을까?
    @Override
    protected void dispose(boolean disposing) {
        if (disposing) {
        }
        for (short i = 0; i < DOSMain.DOS_DRIVES; i++)
            if (DOSMain.Drives[i] != null) {
                DOSMain.Drives[i] = null;
            }
    }

}
