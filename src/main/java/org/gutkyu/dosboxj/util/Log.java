package org.gutkyu.dosboxj.util;

public final class Log {
    public enum LogTypes {
        All, VGA, VGAGFX, VGAMISC, INT10, SB, DMACONTROL, FPU, CPU, PAGING, FCB, FILES, IOCTL, EXEC, DOSMISC, PIT, KEYBOARD, PIC, Mouse, BIOS, GUI, MISC, IO, Max
    }

    public enum LogServerities {
        Normal, Warn, Error
    }

    public static void logging(LogTypes logType, LogServerities logSeverity, String logMsg,
    Object... args) {
        System.out.println(String.format(logMsg, args));
    }

    public static void logMsg(String msg, Object... args) {

    }


}
