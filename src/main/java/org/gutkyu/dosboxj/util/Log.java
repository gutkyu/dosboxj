package org.gutkyu.dosboxj.util;

public final class Log {
    public enum LogTypes {
        All, VGA, VGAGFX, VGAMISC, INT10, SB, DMACONTROL, FPU, CPU, PAGING, FCB, FILES, IOCTL, EXEC, DOSMISC, PIT, KEYBOARD, PIC, Mouse, BIOS, GUI, MISC, IO, Max
    }

    public enum LogServerities {
        Normal, Warn, Error
    }

    public static void logging(LogTypes logType, LogServerities logSeverity, String logMsg,
            double... values) {
    }

    public static void logging(LogTypes logType, LogServerities logSeverity, String logMsg,
            String val0) {
    }

    public static void logging(LogTypes logType, LogServerities logSeverity, String logMsg,
            String val0, double val1) {
    }

    public static void logging(LogTypes logType, LogServerities logSeverity, String logMsg,
            String val0, double val1, double val2) {
    }

    public static void logging(LogTypes logType, LogServerities logSeverity, String logMsg,
            double val0, String val1) {
    }

    public static void logging(LogTypes logType, LogServerities logSeverity, String logMsg,
            double val0, double val1, String val2) {
    }

    public static void logging(LogTypes logType, LogServerities logSeverity, String logMsg,
            String val0, String val1) {
    }



    public static void logMsg(String msg, Object... args) {

    }


}
