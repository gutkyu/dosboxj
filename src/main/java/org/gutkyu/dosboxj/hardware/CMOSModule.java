package org.gutkyu.dosboxj.hardware;

import org.gutkyu.dosboxj.misc.setup.*;
import org.gutkyu.dosboxj.interrupt.bios.*;
import org.gutkyu.dosboxj.util.*;
import java.time.LocalDateTime;
import org.gutkyu.dosboxj.hardware.io.*;
import org.gutkyu.dosboxj.hardware.io.iohandler.*;
import org.gutkyu.dosboxj.hardware.memory.*;

public final class CMOSModule extends ModuleBase {

    private IOReadHandleObject[] ReadHandler = new IOReadHandleObject[2];
    private IOWriteHandleObject[] WriteHandler = new IOWriteHandleObject[2];

    public CMOSModule(Section configuration) {
        super(configuration);
        for (int i = 0; i < ReadHandler.length; i++) {
            ReadHandler[i] = new IOReadHandleObject();
        }
        for (int i = 0; i < WriteHandler.length; i++) {
            WriteHandler[i] = new IOWriteHandleObject();
        }
        WriteHandler[0].install(0x70, CMOSModule::selReg, IO.IO_MB);
        WriteHandler[1].install(0x71, CMOSModule::writeReg, IO.IO_MB);
        ReadHandler[0].install(0x71, this::readReg, IO.IO_MB);
        CMOSInfo.timer.enabled = false;
        CMOSInfo.timer.acknowledged = true;
        CMOSInfo.reg = 0xa;
        writeReg(0x71, 0x26, 1);
        CMOSInfo.reg = 0xb;
        writeReg(0x71, 0x2, 1); // Struct tm *loctime is of 24 hour format,
        CMOSInfo.reg = 0xd;
        writeReg(0x71, 0x80, 1); /* RTC power on */
        // Equipment is updated from bios.cpp and bios_disk.cpp
        /* Fill in base memory size, it is 640K always */
        CMOSInfo.regs[0x15] = (byte) 0x80;
        CMOSInfo.regs[0x16] = (byte) 0x02;
        /* Fill in extended memory size */
        int exsize = (Memory.totalPages() * 4) - 1024;
        CMOSInfo.regs[0x17] = (byte) exsize;
        CMOSInfo.regs[0x18] = (byte) (exsize >>> 8);
        CMOSInfo.regs[0x30] = (byte) exsize;
        CMOSInfo.regs[0x31] = (byte) (exsize >>> 8);
    }

    private static class CMOSInfo {
        public static byte[] regs = new byte[0x40];
        public static boolean nmi;
        public static boolean bcd;
        public static int reg;// byte

        public static class timer {
            public static boolean enabled;
            public static byte div;
            public static float delay;
            public static boolean acknowledged;
        }
        public static class last {
            public static double timer;
            public static double ended;
            public static double alarm;
        }

        public static boolean update_ended;
    }

    private static EventHandler timerEventWrap = CMOSModule::timerEvent;

    private static void timerEvent(int val) {
        if (CMOSInfo.timer.acknowledged) {
            CMOSInfo.timer.acknowledged = false;
            PIC.activateIRQ(8);
        }
        if (CMOSInfo.timer.enabled) {
            PIC.addEvent(CMOSModule.timerEventWrap, CMOSInfo.timer.delay);
            CMOSInfo.regs[0xc] = (byte) 0xC0;// Contraption Zack (music)
        }
    }

    private static void checkTimer() {
        PIC.removeEvents(CMOSModule.timerEventWrap);
        if (CMOSInfo.timer.div <= 2)
            CMOSInfo.timer.div += 7;
        CMOSInfo.timer.delay = (1000.0f / (32768.0f / (1 << (CMOSInfo.timer.div - 1))));
        if (CMOSInfo.timer.div == 0 || !CMOSInfo.timer.enabled)
            return;
        Log.logging(Log.LogTypes.PIT, Log.LogServerities.Normal, "RTC Timer at %.2f hz",
                1000.0 / CMOSInfo.timer.delay);
        // PIC_AddEvent(cmos_timerevent,cmos.timer.delay);
        /* A rtc is always running */
        double remd = PIC.getFullIndex() % (double) CMOSInfo.timer.delay;
        PIC.addEvent(CMOSModule.timerEventWrap, (float) ((double) CMOSInfo.timer.delay - remd));
    }

    private static void selReg(int port, int val, int iolen) {
        CMOSInfo.reg = val & 0x3f;
        CMOSInfo.nmi = (val & 0x80) > 0;
    }

    private static void writeReg(int port, int val, int iolen) {
        switch (CMOSInfo.reg) {
            case 0x00: /* Seconds */
            case 0x02: /* Minutes */
            case 0x04: /* Hours */
            case 0x06: /* Day of week */
            case 0x07: /* Date of month */
            case 0x08: /* Month */
            case 0x09: /* Year */
            case 0x32: /* Century */
                /* Ignore writes to change alarm */
                break;
            case 0x01: /* Seconds Alarm */
            case 0x03: /* Minutes Alarm */
            case 0x05: /* Hours Alarm */
                Log.logging(Log.LogTypes.BIOS, Log.LogServerities.Normal,
                        "CMOS:Trying to set alarm");
                CMOSInfo.regs[CMOSInfo.reg] = (byte) val;
                break;
            case 0x0a: /* Status reg A */
                CMOSInfo.regs[CMOSInfo.reg] = (byte) (val & 0x7f);
                if ((val & 0x70) != 0x20)
                    Log.logging(Log.LogTypes.BIOS, Log.LogServerities.Error,
                            "CMOS Illegal 22 stage divider value");
                CMOSInfo.timer.div = (byte) (val & 0xf);
                checkTimer();
                break;
            case 0x0b: /* Status reg B */
                CMOSInfo.bcd = (val & 0x4) == 0;
                CMOSInfo.regs[CMOSInfo.reg] = (byte) (val & 0x7f);
                CMOSInfo.timer.enabled = (val & 0x40) > 0;
                if ((val & 0x10) != 0)
                    Log.logging(Log.LogTypes.BIOS, Log.LogServerities.Error,
                            "CMOS:Updated ended interrupt not supported yet");
                checkTimer();
                break;
            case 0x0d:/* Status reg D */
                CMOSInfo.regs[CMOSInfo.reg] = (byte) (val & 0x80); /* Bit 7=1:RTC Pown on */
                break;
            case 0x0f: /* Shutdown status byte */
                CMOSInfo.regs[CMOSInfo.reg] = (byte) (val & 0x7f);
                break;
            default:
                CMOSInfo.regs[CMOSInfo.reg] = (byte) (val & 0x7f);
                Log.logging(Log.LogTypes.BIOS, Log.LogServerities.Error,
                        "CMOS:WRite to unhandled register %x", CMOSInfo.reg);
                break;
        }
    }

    private int makeReturn(int val) {
        return CMOSInfo.bcd ? (((val / 10) << 4) | (val % 10)) : val;
    }

    private int readReg(int port, int iolen) {
        if (CMOSInfo.reg > 0x3f) {
            Log.logging(Log.LogTypes.BIOS, Log.LogServerities.Error,
                    "CMOS:Read from illegal register %x", CMOSInfo.reg);
            return 0xff;
        }
        int drive_a, drive_b;
        byte hdparm;
        // DateTime loctime = DateTime.Now;
        LocalDateTime loctime = LocalDateTime.now();

        switch (CMOSInfo.reg) {
            case 0x00: /* Seconds */
                return makeReturn(loctime.getSecond());
            case 0x02: /* Minutes */
                return makeReturn(loctime.getMinute());
            case 0x04: /* Hours */
                return makeReturn(loctime.getHour());
            case 0x06: /* Day of week */
                int dw = loctime.getDayOfWeek().getValue();
                return makeReturn((dw == 7 ? 0 : dw) + 1);
            case 0x07: /* Date of month */
                return makeReturn(loctime.getDayOfMonth());
            case 0x08: /* Month */
                return makeReturn(loctime.getMonthValue());
            case 0x09: /* Year */
                return makeReturn(loctime.getYear() % 100);
            case 0x32: /* Century */
                return makeReturn(loctime.getYear() / 100);
            case 0x01: /* Seconds Alarm */
            case 0x03: /* Minutes Alarm */
            case 0x05: /* Hours Alarm */
                return CMOSInfo.regs[CMOSInfo.reg];
            case 0x0a: /* Status register A */
                if (PIC.getTickIndex() < 0.002) {
                    return (CMOSInfo.regs[0x0a] & 0x7f) | 0x80;
                } else {
                    return CMOSInfo.regs[0x0a] & 0x7f;
                }
            case 0x0c: /* Status register C */
                CMOSInfo.timer.acknowledged = true;
                if (CMOSInfo.timer.enabled) {
                    /* In periodic interrupt mode only care for those flags */
                    int val = 0xff & CMOSInfo.regs[0xc];
                    CMOSInfo.regs[0xc] = 0;
                    return val;
                } else {
                    /* Give correct values at certain times */
                    int val = 0;
                    double index = PIC.getFullIndex();
                    if (index >= (CMOSInfo.last.timer + CMOSInfo.timer.delay)) {
                        CMOSInfo.last.timer = index;
                        val |= 0x40;
                    }
                    if (index >= (CMOSInfo.last.ended + 1000)) {
                        CMOSInfo.last.ended = index;
                        val |= 0x10;
                    }
                    return val;
                }
            case 0x10: /* Floppy size */
                drive_a = 0;
                drive_b = 0;
                if (BIOSDisk.ImageDiskList[0] != null)
                    drive_a = BIOSDisk.ImageDiskList[0].getBiosType();
                if (BIOSDisk.ImageDiskList[1] != null)
                    drive_b = BIOSDisk.ImageDiskList[1].getBiosType();
                return (drive_a << 4) | (drive_b);
            /* First harddrive info */
            case 0x12:
                hdparm = 0;
                if (BIOSDisk.ImageDiskList[2] != null)
                    hdparm |= 0xf;
                if (BIOSDisk.ImageDiskList[3] != null)
                    hdparm |= 0xf0;
                return hdparm;
            case 0x19:
                if (BIOSDisk.ImageDiskList[2] != null)
                    return 47; /* User defined type */
                return 0;
            case 0x1b:
                if (BIOSDisk.ImageDiskList[2] != null)
                    return (BIOSDisk.ImageDiskList[2].cylinders & 0xff);
                return 0;
            case 0x1c:
                if (BIOSDisk.ImageDiskList[2] != null)
                    return ((BIOSDisk.ImageDiskList[2].cylinders & 0xff00) >>> 8);
                return 0;
            case 0x1d:
                if (BIOSDisk.ImageDiskList[2] != null)
                    return (BIOSDisk.ImageDiskList[2].heads);
                return 0;
            case 0x1e:
                if (BIOSDisk.ImageDiskList[2] != null)
                    return 0xff;
                return 0;
            case 0x1f:
                if (BIOSDisk.ImageDiskList[2] != null)
                    return 0xff;
                return 0;
            case 0x20:
                if (BIOSDisk.ImageDiskList[2] != null)
                    return 0xc0 | (Convert.toByte((BIOSDisk.ImageDiskList[2].heads) > 8) << 3);
                return 0;
            case 0x21:
                if (BIOSDisk.ImageDiskList[2] != null)
                    return (BIOSDisk.ImageDiskList[2].cylinders & 0xff);
                return 0;
            case 0x22:
                if (BIOSDisk.ImageDiskList[2] != null)
                    return ((BIOSDisk.ImageDiskList[2].cylinders & 0xff00) >>> 8);
                return 0;
            case 0x23:
                if (BIOSDisk.ImageDiskList[2] != null)
                    return (BIOSDisk.ImageDiskList[2].sectors);
                return 0;
            /* Second harddrive info */
            case 0x1a:
                if (BIOSDisk.ImageDiskList[3] != null)
                    return 47; /* User defined type */
                return 0;
            case 0x24:
                if (BIOSDisk.ImageDiskList[3] != null)
                    return (BIOSDisk.ImageDiskList[3].cylinders & 0xff);
                return 0;
            case 0x25:
                if (BIOSDisk.ImageDiskList[3] != null)
                    return ((BIOSDisk.ImageDiskList[3].cylinders & 0xff00) >>> 8);
                return 0;
            case 0x26:
                if (BIOSDisk.ImageDiskList[3] != null)
                    return (BIOSDisk.ImageDiskList[3].heads);
                return 0;
            case 0x27:
                if (BIOSDisk.ImageDiskList[3] != null)
                    return 0xff;
                return 0;
            case 0x28:
                if (BIOSDisk.ImageDiskList[3] != null)
                    return 0xff;
                return 0;
            case 0x29:
                if (BIOSDisk.ImageDiskList[3] != null)
                    return 0xc0 | (Convert.toByte((BIOSDisk.ImageDiskList[3].heads) > 8) << 3);
                return 0;
            case 0x2a:
                if (BIOSDisk.ImageDiskList[3] != null)
                    return (BIOSDisk.ImageDiskList[3].cylinders & 0xff);
                return 0;
            case 0x2b:
                if (BIOSDisk.ImageDiskList[3] != null)
                    return ((BIOSDisk.ImageDiskList[3].cylinders & 0xff00) >>> 8);
                return 0;
            case 0x2c:
                if (BIOSDisk.ImageDiskList[3] != null)
                    return (BIOSDisk.ImageDiskList[3].sectors);
                return 0;
            case 0x39:
                return 0;
            case 0x3a:
                return 0;
            case 0x0b: /* Status register B */
            case 0x0d: /* Status register D */
            case 0x0f: /* Shutdown status byte */
            case 0x14: /* Equipment */
            case 0x15: /* Base Memory KB Low Byte */
            case 0x16: /* Base Memory KB High Byte */
            case 0x17: /* Extended memory in KB Low Byte */
            case 0x18: /* Extended memory in KB High Byte */
            case 0x30: /* Extended memory in KB Low Byte */
            case 0x31: /* Extended memory in KB High Byte */
                // Log.Logging(Log.LOG_TYPES.LOG_BIOS,Log.LOG_SEVERITIES.LOG_NORMAL,"CMOS:Read from
                // reg %X : %04X",cmos.reg,cmos.regs[cmos.reg]);
                return CMOSInfo.regs[CMOSInfo.reg];
            default:
                Log.logging(Log.LogTypes.BIOS, Log.LogServerities.Normal, "CMOS:Read from reg %X",
                        CMOSInfo.reg);
                return CMOSInfo.regs[CMOSInfo.reg];
        }
    }

    // -- #region CMOS_prefix
    public static void setRegister(int regNr, byte val) {
        CMOSInfo.regs[regNr] = val;
    }


    private static CMOSModule _cmos;

    private static void destroy(Section sec) {
        _cmos.dispose();
        _cmos = null;
    }

    public static void init(Section sec) {
        _cmos = new CMOSModule(sec);
        sec.addDestroyFunction(CMOSModule::destroy, true);
    }
    // -- #endregion
}
