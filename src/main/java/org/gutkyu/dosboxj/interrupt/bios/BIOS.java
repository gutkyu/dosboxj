package org.gutkyu.dosboxj.interrupt.bios;

import org.gutkyu.dosboxj.misc.setup.*;
import org.gutkyu.dosboxj.cpu.*;
import org.gutkyu.dosboxj.dos.*;
import org.gutkyu.dosboxj.misc.*;
import org.gutkyu.dosboxj.util.*;
import org.gutkyu.dosboxj.interrupt.*;
import org.gutkyu.dosboxj.hardware.*;
import org.gutkyu.dosboxj.hardware.io.IO;
import org.gutkyu.dosboxj.hardware.memory.*;
import org.gutkyu.dosboxj.*;

public final class BIOS {
    protected BIOS() {

    }

    /*
     * if mem_systems 0 then size_extended is reported as the real size else zero is reported. ems
     * and xms can increase or decrease the other_memsystems counter using the BIOS_ZeroExtendedSize
     * call
     */
    private static int _sizeExtended;// uint16
    private static int _otherMemSystems = 0;

    private static CallbackHandlerObject[] tandyDACCallback = new CallbackHandlerObject[2];

    private static class TandySB {
        public short port;
        public byte irq;
        public byte dma;
    }
    private static class TandyDAC {
        public short port;
        public byte irq;
        public byte dma;
    }

    private static TandySB tandySb = new TandySB();
    private static TandyDAC tandyDAC = new TandyDAC();

    public static final short BIOS_BASE_ADDRESS_COM1 = 0x400;
    public static final short BIOS_BASE_ADDRESS_COM2 = 0x402;
    public static final short BIOS_BASE_ADDRESS_COM3 = 0x404;
    public static final short BIOS_BASE_ADDRESS_COM4 = 0x406;
    public static final short BIOS_ADDRESS_LPT1 = 0x408;
    public static final short BIOS_ADDRESS_LPT2 = 0x40a;
    public static final short BIOS_ADDRESS_LPT3 = 0x40c;

    public static final short BIOS_CONFIGURATION = 0x410;

    public static final short BIOS_MEMORY_SIZE = 0x413;
    public static final short BIOS_TRUE_MEMORY_SIZE = 0x415;

    public static final short BIOS_KEYBOARD_STATE = 0x417;
    public static final short BIOS_KEYBOARD_FLAGS1 = BIOS_KEYBOARD_STATE;
    public static final short BIOS_KEYBOARD_FLAGS2 = 0x418;
    public static final short BIOS_KEYBOARD_TOKEN = 0x419;

    public static final short BIOS_KEYBOARD_BUFFER_HEAD = 0x41a;
    public static final short BIOS_KEYBOARD_BUFFER_TAIL = 0x41c;
    public static final short BIOS_KEYBOARD_BUFFER = 0x41e;

    public static final short BIOS_DRIVE_ACTIVE = 0x43e;
    public static final short BIOS_DRIVE_RUNNING = 0x43f;
    public static final short BIOS_DISK_MOTOR_TIMEOUT = 0x440;
    public static final short BIOS_DISK_STATUS = 0x441;

    public static final short BIOS_VIDEO_MODE = 0x449;
    public static final short BIOS_SCREEN_COLUMNS = 0x44a;
    public static final short BIOS_VIDEO_MEMORY_USED = 0x44c;
    public static final short BIOS_VIDEO_MEMORY_ADDRESS = 0x44e;
    public static final short BIOS_VIDEO_CURSOR_POS = 0x450;

    public static final short BIOS_CURSOR_SHAPE = 0x460;
    public static final short BIOS_CURSOR_LAST_LINE = 0x460;
    public static final short BIOS_CURSOR_FIRST_LINE = 0x461;
    public static final short BIOS_CURRENT_SCREEN_PAGE = 0x462;
    public static final short BIOS_VIDEO_PORT = 0x463;
    public static final short BIOS_VDU_CONTROL = 0x465;
    public static final short BIOS_VDU_COLOR_REGISTER = 0x466;

    public static final short BIOS_TIMER = 0x46c;
    public static final short BIOS_24_HOURS_FLAG = 0x470;
    public static final short BIOS_KEYBOARD_FLAGS = 0x471;
    public static final short BIOS_CTRL_ALT_DEL_FLAG = 0x472;
    public static final short BIOS_HARDDISK_COUNT = 0x475;

    public static final short BIOS_LPT1_TIMEOUT = 0x478;
    public static final short BIOS_LPT2_TIMEOUT = 0x479;
    public static final short BIOS_LPT3_TIMEOUT = 0x47a;

    public static final int BIOS_COM1_TIMEOUT = 0x47c;
    public static final short BIOS_COM2_TIMEOUT = 0x47d;
    public static final short BIOS_COM3_TIMEOUT = 0x47e;
    public static final short BIOS_COM4_TIMEOUT = 0x47f;


    public static final short BIOS_KEYBOARD_BUFFER_START = 0x480;
    public static final short BIOS_KEYBOARD_BUFFER_END = 0x482;

    public static final short BIOS_ROWS_ON_SCREEN_MINUS_1 = 0x484;
    public static final short BIOS_FONT_HEIGHT = 0x485;

    public static final short BIOS_VIDEO_INFO_0 = 0x487;
    public static final short BIOS_VIDEO_INFO_1 = 0x488;
    public static final short BIOS_VIDEO_INFO_2 = 0x489;
    public static final short BIOS_VIDEO_COMBO = 0x48a;

    public static final short BIOS_KEYBOARD_FLAGS3 = 0x496;
    public static final short BIOS_KEYBOARD_LEDS = 0x497;

    public static final short BIOS_WAIT_FLAG_POINTER = 0x498;
    public static final short BIOS_WAIT_FLAG_COUNT = 0x49c;
    public static final short BIOS_WAIT_FLAG_ACTIVE = 0x4a0;
    public static final short BIOS_WAIT_FLAG_TEMP = 0x4a1;

    public static final short BIOS_PRINT_SCREEN_FLAG = 0x500;

    public static final short BIOS_VIDEO_SAVEPTR = 0x4a8;

    public static final int BIOS_DEFAULT_HANDLER_LOCATION = Memory.realMake(0xf000, 0xff53);
    public static final int BIOS_DEFAULT_IRQ0_LOCATION = Memory.realMake(0xf000, 0xfea5);
    public static final int BIOS_DEFAULT_IRQ1_LOCATION = Memory.realMake(0xf000, 0xe987);
    public static final int BIOS_DEFAULT_IRQ2_LOCATION = Memory.realMake(0xf000, 0xff55);

    /* maximum of scancodes handled by keyboard bios routines */
    public static final byte MAX_SCAN_CODE = 0x58;

    /* The Section handling Bios Disk Access */
    // #define BIOS_MAX_DISK 10

    // #define MAX_SWAPPABLE_DISKS 20



    public static void ZeroExtendedSize(boolean input) {
        if (input)
            _otherMemSystems++;
        else
            _otherMemSystems--;
        if (_otherMemSystems < 0)
            _otherMemSystems = 0;
    }

    // set com port data in bios data area
    // parameter: array of 4 com port base addresses, 0 = none
    void setComPorts(short[] baseaddr) {
        short portcount = 0;
        int equipmentWord;
        for (int i = 0; i < 4; i++) {
            if (baseaddr[i] != 0)
                portcount++;
            if (i == 0)
                Memory.writeW(BIOS_BASE_ADDRESS_COM1, baseaddr[i]);
            else if (i == 1)
                Memory.writeW(BIOS_BASE_ADDRESS_COM2, baseaddr[i]);
            else if (i == 2)
                Memory.writeW(BIOS_BASE_ADDRESS_COM3, baseaddr[i]);
            else
                Memory.writeW(BIOS_BASE_ADDRESS_COM4, baseaddr[i]);
        }
        // set equipment word
        equipmentWord = Memory.readW(BIOS_CONFIGURATION);
        equipmentWord &= 0xffff & ~0x0E00;
        equipmentWord |= 0xffff & (portcount << 9);
        Memory.writeW(BIOS_CONFIGURATION, equipmentWord);
        CMOSModule.setRegister(0x14, (byte) equipmentWord); // Should be updated on changes
    }

    private class BIOSModule extends ModuleBase {

        private CallbackHandlerObject[] callback = new CallbackHandlerObject[11];

        public BIOSModule(Section configuration) {
            super(configuration);
            for (int i = 0; i < callback.length; i++) {
                callback[i] = new CallbackHandlerObject();
            }

            /* tandy DAC can be requested in tandy_sound.cpp by initializing this field */
            boolean useTandyDAC = (Memory.realReadB(0x40, 0xd4) == 0xff);

            /* Clear the Bios Data Area (0x400-0x5ff, 0x600- is accounted to DOS) */
            for (short i = 0; i < 0x200; i++)
                Memory.realWriteB(0x40, i, 0);

            /* Setup all the interrupt handlers the bios controls */

            /* INT 8 Clock IRQ Handler */
            int callIRQ0 = Callback.allocate();
            Callback.setup(callIRQ0, BIOS::INT8Handler, Callback.Symbol.IRQ0,
                    Memory.real2Phys(BIOS_DEFAULT_IRQ0_LOCATION), "IRQ 0 Clock");
            Memory.realSetVec(0x08, BIOS_DEFAULT_IRQ0_LOCATION);
            // pseudocode for CB_IRQ0:
            // callback INT8_Handler
            // push ax,dx,ds
            // int 0x1c
            // cli
            // pop ds,dx
            // mov al, 0x20
            // out 0x20, al
            // pop ax
            // iret

            Memory.writeD(BIOS_TIMER, 0); // Calculate the correct time

            /* INT 11 Get equipment list */
            callback[1].install(BIOS::INT11Handler, Callback.Symbol.IRET, "Int 11 Equipment");
            callback[1].setRealVec(0x11);

            /* INT 12 Memory Size default at 640 kb */
            callback[2].install(BIOS::INT12Handler, Callback.Symbol.IRET, "Int 12 Memory");
            callback[2].setRealVec(0x12);
            if (DOSBox.isTANDYArch()) {
                /*
                 * reduce reported memory size for the Tandy (32k graphics memory at the end of the
                 * conventional 640k)
                 */
                if (DOSBox.Machine == DOSBox.MachineType.TANDY)
                    Memory.writeW(BIOS_MEMORY_SIZE, 608);
                else
                    Memory.writeW(BIOS_MEMORY_SIZE, 640);
                Memory.writeW(BIOS_TRUE_MEMORY_SIZE, 640);
            } else
                Memory.writeW(BIOS_MEMORY_SIZE, 640);

            /* INT 13 Bios Disk Support */
            BIOSDisk.setupDisks();

            /* INT 14 Serial Ports */
            callback[3].install(BIOS::INT14Handler, Callback.Symbol.IRET_STI, "Int 14 COM-port");
            callback[3].setRealVec(0x14);

            /* INT 15 Misc Calls */
            callback[4].install(BIOS::INT15Handler, Callback.Symbol.IRET, "Int 15 Bios");
            callback[4].setRealVec(0x15);

            /* INT 16 Keyboard handled in another file */
            BIOSKeyboard.setupKeyboard();

            /* INT 17 Printer Routines */
            callback[5].install(BIOS::INT17Handler, Callback.Symbol.IRET_STI, "Int 17 Printer");
            callback[5].setRealVec(0x17);

            /* INT 1A TIME and some other functions */
            callback[6].install(BIOS::INT1AHandler, Callback.Symbol.IRET_STI, "Int 1a Time");
            callback[6].setRealVec(0x1A);

            /* INT 1C System Timer tick called from INT 8 */
            callback[7].install(BIOS::INT1CHandler, Callback.Symbol.IRET, "Int 1c Timer");
            callback[7].setRealVec(0x1C);

            /* IRQ 8 RTC Handler */
            callback[8].install(BIOS::INT70Handler, Callback.Symbol.IRET, "Int 70 RTC");
            callback[8].setRealVec(0x70);

            /* Irq 9 rerouted to irq 2 */
            callback[9].install(null, Callback.Symbol.IRQ9, "irq 9 bios");
            callback[9].setRealVec(0x71);

            /* Reboot */
            callback[10].install(BIOS::rebootHandler, Callback.Symbol.IRET, "reboot");
            callback[10].setRealVec(0x18);
            int rptr = callback[10].getRealPointer();
            Memory.realSetVec(0x19, rptr);
            // set system BIOS entry point too
            Memory.physWriteB(0xFFFF0, 0xEA); // FARJMP
            Memory.physWriteW(0xFFFF1, Memory.realOff(rptr)); // offset
            Memory.physWriteW(0xFFFF3, Memory.realSeg(rptr)); // segment

            /* Irq 2 */
            int callIRQ2 = Callback.allocate();
            Callback.setup(callIRQ2, null, Callback.Symbol.IRET_EOI_PIC1,
                    Memory.real2Phys(BIOS_DEFAULT_IRQ2_LOCATION), "irq 2 bios");
            Memory.realSetVec(0x0a, BIOS_DEFAULT_IRQ2_LOCATION);

            /* Some hardcoded vectors */
            Memory.physWriteB(Memory.real2Phys(BIOS_DEFAULT_HANDLER_LOCATION),
                    0xcf); /* bios default interrupt vector location -> IRET */
            // Hack for Jurresic
            Memory.physWriteW(Memory.real2Phys(Memory.realGetVec(0x12)) + 0x12, 0x20);
            if (DOSBox.Machine == DOSBox.MachineType.TANDY)
                Memory.physWriteB(0xffffe, 0xff); /* Tandy model */
            else if (DOSBox.Machine == DOSBox.MachineType.PCJR)
                Memory.physWriteB(0xffffe, 0xfd); /* PCJr model */
            else
                Memory.physWriteB(0xffffe, 0xfc); /* PC */

            // System BIOS identification
            String bType = "IBM COMPATIBLE 486 BIOS COPYRIGHT The DOSBox Team.";
            for (int i = 0; i < bType.length(); i++)
                Memory.physWriteB(0xfe00e + i, Convert.toByte(bType.charAt(i)));

            // System BIOS version
            String bVers = "DOSBox FakeBIOS v1.0";
            for (int i = 0; i < bVers.length(); i++)
                Memory.physWriteB(0xfe061 + i, Convert.toByte(bVers.charAt(i)));

            // write system BIOS date
            String b_date = "01/01/92";
            for (int i = 0; i < b_date.length(); i++)
                Memory.physWriteB(0xffff5 + i, Convert.toByte(b_date.charAt(i)));
            Memory.physWriteB(0xfffff, 0x55); // signature

            tandySb.port = 0;
            tandyDAC.port = 0;
            if (useTandyDAC) {
                /* tandy DAC sound requested, see if soundblaster device is available */
                int tandyDACType = 0;
                if (TandyInitializeSB()) {
                    tandyDACType = 1;
                } else if (TandyInitializeTS()) {
                    tandyDACType = 2;
                }
                if (tandyDACType != 0) {
                    Memory.realWriteW(0x40, 0xd0, 0x0000);
                    Memory.realWriteW(0x40, 0xd2, 0x0000);
                    Memory.realWriteB(0x40, 0xd4, 0xff); /* tandy DAC init value */
                    Memory.realWriteD(0x40, 0xd6, 0x00000000);
                    /* install the DAC callback handler */
                    tandyDACCallback[0] = new CallbackHandlerObject();
                    tandyDACCallback[1] = new CallbackHandlerObject();
                    tandyDACCallback[0].install(BIOS::IRQTandyDAC, Callback.Symbol.IRET,
                            "Tandy DAC IRQ");
                    tandyDACCallback[1].install(null, Callback.Symbol.TDE_IRET,
                            "Tandy DAC end transfer");
                    // pseudocode for CB_TDE_IRET:
                    // push ax
                    // mov ax, 0x91fb
                    // int 15
                    // cli
                    // mov al, 0x20
                    // out 0x20, al
                    // pop ax
                    // iret

                    int tandyIRQ = 7;
                    if (tandyDACType == 1)
                        tandyIRQ = tandySb.irq;
                    else if (tandyDACType == 2)
                        tandyIRQ = tandyDAC.irq;
                    int tandyIRQVector = 0xff & tandyIRQ;
                    if (tandyIRQVector < 8)
                        tandyIRQVector += 8;
                    else
                        tandyIRQVector += (0x70 - 8);

                    int currentIRQ = Memory.realGetVec(tandyIRQVector);
                    Memory.realWriteD(0x40, 0xd6, currentIRQ);
                    for (short i = 0; i < 0x10; i++)
                        Memory.physWriteB(Memory.physMake(0xf000, 0xa084 + i), 0x80);
                } else
                    Memory.realWriteB(0x40, 0xd4, 0x00);
            }

            /* Setup some stuff in 0x40 bios segment */

            // port timeouts
            // always 1 second even if the port does not exist
            Memory.writeB(BIOS_LPT1_TIMEOUT, 1);
            Memory.writeB(BIOS_LPT2_TIMEOUT, 1);
            Memory.writeB(BIOS_LPT3_TIMEOUT, 1);
            Memory.writeB(BIOS_COM1_TIMEOUT, 1);
            Memory.writeB(BIOS_COM2_TIMEOUT, 1);
            Memory.writeB(BIOS_COM3_TIMEOUT, 1);
            Memory.writeB(BIOS_COM4_TIMEOUT, 1);

            /* detect parallel ports */
            int ppIndex = 0; // number of lpt ports
            if ((IO.read(0x378) != 0xff) | (IO.read(0x379) != 0xff)) {
                // this is our LPT1
                Memory.writeW(BIOS_ADDRESS_LPT1, 0x378);
                ppIndex++;
                if ((IO.read(0x278) != 0xff) | (IO.read(0x279) != 0xff)) {
                    // this is our LPT2
                    Memory.writeW(BIOS_ADDRESS_LPT2, 0x278);
                    ppIndex++;
                    if ((IO.read(0x3bc) != 0xff) | (IO.read(0x3be) != 0xff)) {
                        // this is our LPT3
                        Memory.writeW(BIOS_ADDRESS_LPT3, 0x3bc);
                        ppIndex++;
                    }
                } else if ((IO.read(0x3bc) != 0xff) | (IO.read(0x3be) != 0xff)) {
                    // this is our LPT2
                    Memory.writeW(BIOS_ADDRESS_LPT2, 0x3bc);
                    ppIndex++;
                }
            } else if ((IO.read(0x3bc) != 0xff) | (IO.read(0x3be) != 0xff)) {
                // this is our LPT1
                Memory.writeW(BIOS_ADDRESS_LPT1, 0x3bc);
                ppIndex++;
                if ((IO.read(0x278) != 0xff) | (IO.read(0x279) != 0xff)) {
                    // this is our LPT2
                    Memory.writeW(BIOS_ADDRESS_LPT2, 0x278);
                    ppIndex++;
                }
            } else if ((IO.read(0x278) != 0xff) | (IO.read(0x279) != 0xff)) {
                // this is our LPT1
                Memory.writeW(BIOS_ADDRESS_LPT1, 0x278);
                ppIndex++;
            }

            /* Setup equipment list */
            // look http://www.bioscentral.com/misc/bda.htm

            // short config=0x4400; //1 Floppy, 2 serial and 1 parallel
            short config = 0x0;

            // set number of parallel ports
            // if(ppindex == 0) config |= 0x8000; // looks like 0 ports are not specified
            // else if(ppindex == 1) config |= 0x0000;
            if (ppIndex == 2)
                config |= 0x4000;
            else
                config |= 0xc000; // 3 ports

            switch (DOSBox.Machine) {
                case HERC:
                    // Startup monochrome
                    config |= 0x30;
                    break;
                case EGA:
                case VGA:
                case CGA:
                case TANDY:
                case PCJR:
                    // Startup 80x25 color
                    config |= 0x20;
                    break;
                default:
                    // EGA VGA
                    config |= 0;
                    break;
            }
            // PS2 mouse
            config |= 0x04;
            // Gameport
            config |= 0x1000;
            Memory.writeW(BIOS_CONFIGURATION, config);
            // Should be updated on changes
            CMOSModule.setRegister(0x14, (byte) config);
            /* Setup extended memory size */
            IO.write(0x70, 0x30);
            _sizeExtended = IO.read(0x71);
            IO.write(0x70, 0x31);
            _sizeExtended |= 0xffff & (IO.read(0x71) << 8);
        }

        @Override
        protected void dispose(boolean disposing) {
            if (disposing) {
                eventOnFinalization();

                for (CallbackHandlerObject cb : callback) {
                    cb.dispose();
                }
                callback = null;
            }

            super.dispose(disposing);
        }

        // 객체 소멸시 실행
        private void eventOnFinalization() {
            /* abort DAC playing */
            if (tandySb.port != 0) {
                IO.write(tandySb.port + 0xc, 0xd3);
                IO.write(tandySb.port + 0xc, 0xd0);
            }
            Memory.realWriteB(0x40, 0xd4, 0x00);
            if (tandyDACCallback[0] != null) {
                int origVector = (int) Memory.realReadD(0x40, 0xd6);
                if (origVector == tandyDACCallback[0].getRealPointer()) {
                    /* set IRQ vector to old value */
                    int tandyIRQ = 7;
                    if (tandySb.port != 0)
                        tandyIRQ = tandySb.irq;
                    else if (tandyDAC.port != 0)
                        tandyIRQ = tandyDAC.irq;
                    int tandyIRQVector = 0xff & tandyIRQ;
                    if (tandyIRQVector < 8)
                        tandyIRQVector += 8;
                    else
                        tandyIRQVector += (0x70 - 8);

                    Memory.realSetVec(tandyIRQVector, (int) Memory.realReadD(0x40, 0xd6));
                    Memory.realWriteD(0x40, 0xd6, 0x00000000);
                }
                tandyDACCallback[0].dispose();
                tandyDACCallback[1].dispose();
                tandyDACCallback[0] = null;
                tandyDACCallback[1] = null;
            }
        }


    }


    private static int rebootHandler() {
        // switch to text mode, notify user (let's hope INT10 still works)
        String text = "\n\n   Reboot requested, quitting now.";
        Register.setRegAX(0);
        Callback.runRealInt(0x10);
        Register.setRegAH(0xe);
        Register.setRegBX(0);
        for (int i = 0; i < text.length(); i++) {
            Register.setRegAL(Convert.toByte(text.charAt(i)));
            Callback.runRealInt(0x10);
        }
        Log.logMsg(text);
        double start = PIC.getFullIndex();
        while ((PIC.getFullIndex() - start) < 3000)
            Callback.idle();
        throw new DOSException();
        // return Callback.ReturnTypeNone;
    }

    private static int IRQTandyDAC() {
        if (tandyDAC.port != 0) {
            IO.read(tandyDAC.port);
        }
        if (Memory.realReadW(0x40, 0xd0) != 0) { /* play/record next buffer */
            /* acknowledge IRQ */
            IO.write(0x20, 0x20);
            if (tandySb.port != 0) {
                IO.read((0xffff & tandySb.port) + 0xe);
            }

            /* buffer starts at the next page */
            int npage = 0xff & (Memory.realReadB(0x40, 0xd4) + 1);
            Memory.realWriteB(0x40, 0xd4, npage);

            int rb = Memory.realReadB(0x40, 0xd3);
            if ((rb & 0x10) != 0) {
                /* start recording */
                Memory.realWriteB(0x40, 0xd3, rb & 0xef);
                TandySetupTransfer(npage << 16, false);
            } else {
                /* start playback */
                TandySetupTransfer(npage << 16, true);
            }
        } else { /* playing/recording is finished */
            int tandyIRQ = 7;
            if (tandySb.port != 0)
                tandyIRQ = tandySb.irq;
            else if (tandyDAC.port != 0)
                tandyIRQ = tandyDAC.irq;
            int tandyIRQVector = 0xff & tandyIRQ;
            if (tandyIRQVector < 8)
                tandyIRQVector += 8;
            else
                tandyIRQVector += (0x70 - 8);

            Memory.realSetVec(tandyIRQVector, (int) Memory.realReadD(0x40, 0xd6));

            /* turn off speaker and acknowledge soundblaster IRQ */
            if (tandySb.port != 0) {
                IO.write((0xffff & tandySb.port) + 0xc, 0xd3);
                IO.read((0xffff & tandySb.port) + 0xe);
            }

            /* issue BIOS tandy sound device busy callout */
            Register.segSet16(Register.SEG_NAME_CS,
                    Memory.realSeg(tandyDACCallback[1].getRealPointer()));
            Register.setRegIP(Memory.realOff(tandyDACCallback[1].getRealPointer()));
        }
        return Callback.ReturnTypeNone;
    }

    // TODO
    // 일단 sblaster는 구현하지 않음
    private static boolean TandyInitializeSB() {
        /* see if soundblaster module available and at what port/IRQ/DMA */
        // int sbport, sbirq, sbdma;
        // if (sblasterModule.SB_Get_Address(out sbport,out sbirq,out sbdma))
        // {
        // tandy_sb.port = (short)(sbport & 0xffff);
        // tandy_sb.irq = (byte)(sbirq & 0xff);
        // tandy_sb.dma = (byte)(sbdma & 0xff);
        // return true;
        // }
        // else
        // {
        /* no soundblaster accessible, disable Tandy DAC */
        tandySb.port = 0;
        return false;
        // }
    }

    // TODO
    // 일단 tandy는 구현하지 않음
    private static boolean TandyInitializeTS() {
        /// * see if Tandy DAC module available and at what port/IRQ/DMA */
        // int tsport, tsirq, tsdma;
        // if (tandy_soundModule.TS_Get_Address(out tsport,out tsirq,out tsdma))
        // {
        // tandy_dac.port = (short)(tsport & 0xffff);
        // tandy_dac.irq = (byte)(tsirq & 0xff);
        // tandy_dac.dma = (byte)(tsdma & 0xff);
        // return true;
        // }
        // else
        // {
        /* no Tandy DAC accessible */
        tandyDAC.port = 0;
        return false;
        // }
    }

    /* check if Tandy DAC is still playing */
    private static boolean TandyTransferInProgress() {
        if (Memory.realReadW(0x40, 0xd0) != 0)
            return true; /* not yet done */
        if (Memory.realReadB(0x40, 0xd4) == 0xff)
            return false; /* still in init-state */

        int tandyDMA = 1;
        if (tandySb.port != 0)
            tandyDMA = tandySb.dma;
        else if (tandyDAC.port != 0)
            tandyDMA = tandyDAC.dma;

        IO.write(0x0c, 0x00);
        int dataLen = IO.readB((0xff & tandyDMA) * 2 + 1);
        dataLen |= 0xffff & (IO.readB((0xff & tandyDMA) * 2 + 1) << 8);
        if (dataLen == 0xffff)
            return false; /* no DMA transfer */
        else if ((dataLen < 0x10) && (Memory.realReadB(0x40, 0xd4) == 0x0f)
                && (Memory.realReadW(0x40, 0xd2) == 0x1c)) {
            /* stop already requested */
            return false;
        }
        return true;
    }


    private static void TandySetupTransfer(int bufpt, boolean isplayback) {
        int length = Memory.realReadW(0x40, 0xd0);
        if (length == 0)
            return; /* nothing to do... */

        if ((tandySb.port == 0) && (tandyDAC.port == 0))
            return;

        int tandyIRQ = 7;
        if (tandySb.port != 0)
            tandyIRQ = tandySb.irq;
        else if (tandyDAC.port != 0)
            tandyIRQ = tandyDAC.irq;
        int tandyIRQVector = 0xff & tandyIRQ;
        if (tandyIRQVector < 8)
            tandyIRQVector += 8;
        else
            tandyIRQVector += (0x70 - 8);

        /* revector IRQ-handler if necessary */
        int currentIRQ = Memory.realGetVec(tandyIRQVector);
        if (currentIRQ != tandyDACCallback[0].getRealPointer()) {
            Memory.realWriteD(0x40, 0xd6, currentIRQ);
            Memory.realSetVec(tandyIRQVector, tandyDACCallback[0].getRealPointer());
        }

        int tandyDMA = 1;// uint8
        if (tandySb.port != 0)
            tandyDMA = tandySb.dma;
        else if (tandyDAC.port != 0)
            tandyDMA = tandyDAC.dma;

        if (tandySb.port != 0) {
            IO.write((0xffff & tandySb.port) + 0xc, 0xd0); /* stop DMA transfer */
            IO.write(0x21, 0xff & (IO.read(0x21) & (~(1 << tandyIRQ)))); /* unmask IRQ */
            IO.write((0xffff & tandySb.port) + 0xc, 0xd1); /* turn speaker on */
        } else {
            IO.write((0xffff & tandyDAC.port),
                    0xff & (IO.read(tandyDAC.port) & 0x60)); /* disable DAC */
            IO.write(0x21, 0xff & (IO.read(0x21) & (~(1 << tandyIRQ)))); /* unmask IRQ */
        }

        IO.write(0x0a, 0xff & (0x04 | tandyDMA)); /* mask DMA channel */
        IO.write(0x0c, 0x00); /* clear DMA flipflop */
        if (isplayback)
            IO.write(0x0b, 0xff & (0x48 | tandyDMA));
        else
            IO.write(0x0b, 0xff & (0x44 | tandyDMA));
        /* set physical address of buffer */
        int bufpage = (bufpt >>> 16) & 0xff;
        IO.write((0xff & tandyDMA) * 2, bufpt & 0xff);
        IO.write((0xff & tandyDMA) * 2, (bufpt >>> 8) & 0xff);
        switch (tandyDMA) {
            case 0:
                IO.write(0x87, bufpage);
                break;
            case 1:
                IO.write(0x83, bufpage);
                break;
            case 2:
                IO.write(0x81, bufpage);
                break;
            case 3:
                IO.write(0x82, bufpage);
                break;
        }
        Memory.realWriteB(0x40, 0xd4, bufpage);

        /* calculate transfer size (respects segment boundaries) */
        int tlength = length;
        if (tlength + (bufpt & 0xffff) > 0x10000)
            tlength = 0x10000 - (bufpt & 0xffff);
        Memory.realWriteW(0x40, 0xd0, length - tlength); /* remaining buffer length */
        tlength--;

        /* set transfer size */
        IO.write((0xff & tandyDMA) * 2 + 1, tlength & 0xff);
        IO.write((0xff & tandyDMA) * 2 + 1, (tlength >>> 8) & 0xff);

        int delay = Memory.realReadW(0x40, 0xd2) & 0xfff;
        int amplitude = (Memory.realReadW(0x40, 0xd2) >>> 13) & 0x7;
        if (tandySb.port != 0) {
            IO.write(0x0a, tandyDMA); /* enable DMA channel */
            /* set frequency */
            IO.write((0xffff & tandySb.port) + 0xc, 0x40);
            IO.write((0xffff & tandySb.port) + 0xc, 0xff & ((int) (256 - delay * 100 / 358)));
            /* set playback type to 8bit */
            if (isplayback)
                IO.write((0xffff & tandySb.port) + 0xc, 0x14);
            else
                IO.write((0xffff & tandySb.port) + 0xc, 0x24);
            /* set transfer size */
            IO.write((0xffff & tandySb.port) + 0xc, tlength & 0xff);
            IO.write((0xffff & tandySb.port) + 0xc, (tlength >>> 8) & 0xff);
        } else {
            if (isplayback)
                IO.write(0xffff & tandyDAC.port, 0xff & ((IO.read(tandyDAC.port) & 0x7c) | 0x03));
            else
                IO.write(0xffff & tandyDAC.port, 0xff & ((IO.read(tandyDAC.port) & 0x7c) | 0x02));
            IO.write(0xffff & tandyDAC.port + 2, delay & 0xff);
            IO.write(0xffff & tandyDAC.port + 3, 0xff & (((delay >>> 8) & 0xf) | (amplitude << 5)));
            if (isplayback)
                IO.write(0xffff & tandyDAC.port, 0xff & ((IO.read(tandyDAC.port) & 0x7c) | 0x1f));
            else
                IO.write(0xffff & tandyDAC.port, 0xff & ((IO.read(tandyDAC.port) & 0x7c) | 0x1e));
            IO.write(0x0a, tandyDMA); /* enable DMA channel */
        }

        if (!isplayback) {
            /* mark transfer as recording operation */
            Memory.realWriteW(0x40, 0xd2, 0xffff & (delay | 0x1000));
        }
    }

    // private static void TandyDACHandler(byte tfunction) {
    private static void TandyDACHandler(int tfunction) {
        if ((tandySb.port == 0) && (tandyDAC.port == 0))
            return;
        switch (tfunction) {
            case 0x81: /* Tandy sound system check */
                if (tandyDAC.port == 0) {
                    Register.setRegAX(tandyDAC.port);
                } else {
                    Register.setRegAX(0xc4);
                }
                Callback.scf(TandyTransferInProgress());
                break;
            case 0x82: /* Tandy sound system start recording */
            case 0x83: /* Tandy sound system start playback */
                if (TandyTransferInProgress()) {
                    /* cannot play yet as the last transfer isn't finished yet */
                    Register.setRegAH(0x00);
                    Callback.scf(true);
                    break;
                }
                /* store buffer length */
                Memory.realWriteW(0x40, 0xd0, Register.getRegCX());
                /* store delay and volume */
                Memory.realWriteW(0x40, 0xd2,
                        (Register.getRegDX() & 0xfff) | ((Register.getRegAL() & 7) << 13));
                TandySetupTransfer(Memory.physMake(Register.segValue(Register.SEG_NAME_ES),
                        Register.getRegBX()), Register.getRegAH() == 0x83);
                Register.setRegAH(0x00);
                Callback.scf(false);
                break;
            case 0x84: /* Tandy sound system stop playing */
                Register.setRegAH(0x00);

                /* setup for a small buffer with silence */
                Memory.realWriteW(0x40, 0xd0, 0x0a);
                Memory.realWriteW(0x40, 0xd2, 0x1c);
                TandySetupTransfer(Memory.physMake(0xf000, 0xa084), true);
                Callback.scf(false);
                break;
            case 0x85: /* Tandy sound system reset */
                if (tandyDAC.port != 0) {
                    IO.write(0xffff & tandyDAC.port, IO.read(0xffff & tandyDAC.port) & 0xe0);
                }
                Register.setRegAH(0x00);
                Callback.scf(false);
                break;
        }
    }


    private static int INT8Handler() {
        /* Increase the bios tick counter */
        int value = Memory.readD(BIOS_TIMER) + 1;
        Memory.writeD(BIOS_TIMER, value);

        /* decrease floppy motor timer */
        int val = Memory.readB(BIOS_DISK_MOTOR_TIMEOUT);
        if (val != 0)
            Memory.writeB(BIOS_DISK_MOTOR_TIMEOUT, val - 1);
        /* and running drive */
        Memory.writeB(BIOS_DRIVE_RUNNING, Memory.readB(BIOS_DRIVE_RUNNING) & 0xF0);
        return Callback.ReturnTypeNone;
    }

    private static int INT1AHandler() {
        switch (Register.getRegAH()) {
            case 0x00: /* Get System time */
            {
                int ticks = Memory.readD(BIOS_TIMER);
                Register.setRegAL(0); /* Midnight never passes :) */
                Register.setRegCX(ticks >>> 16);
                Register.setRegDX(ticks & 0xffff);
                break;
            }
            case 0x01: /* Set System time */
                Memory.writeD(BIOS_TIMER, (Register.getRegCX() << 16) | Register.getRegDX());
                break;
            case 0x02: /* GET REAL-TIME CLOCK TIME (AT,XT286,PS) */
                IO.write(0x70, 0x04); // Hours
                Register.setRegCH(IO.read(0x71));
                IO.write(0x70, 0x02); // Minutes
                Register.setRegCL(IO.read(0x71));
                IO.write(0x70, 0x00); // Seconds
                Register.setRegDH(IO.read(0x71));
                Register.setRegDL(0); // Daylight saving disabled
                Callback.scf(false);
                break;
            case 0x04: /* GET REAL-TIME ClOCK DATE (AT,XT286,PS) */
                IO.write(0x70, 0x32); // Centuries
                Register.setRegCH(IO.read(0x71));
                IO.write(0x70, 0x09); // Years
                Register.setRegCL(IO.read(0x71));
                IO.write(0x70, 0x08); // Months
                Register.setRegDH(IO.read(0x71));
                IO.write(0x70, 0x07); // Days
                Register.setRegDL(IO.read(0x71));
                Callback.scf(false);
                break;
            case 0x80: /* Pcjr Setup Sound Multiplexer */
                Log.logging(Log.LogTypes.BIOS, Log.LogServerities.Error,
                        "INT1A:80:Setup tandy sound multiplexer to %d", Register.getRegAL());
                break;
            case 0x81: /* Tandy sound system check */
            case 0x82: /* Tandy sound system start recording */
            case 0x83: /* Tandy sound system start playback */
            case 0x84: /* Tandy sound system stop playing */
            case 0x85: /* Tandy sound system reset */
                TandyDACHandler(Register.getRegAH());
                break;
            case 0xb1: /* PCI Bios Calls */
                Log.logging(Log.LogTypes.BIOS, Log.LogServerities.Error, "INT1A:PCI bios call %2X",
                        Register.getRegAL());
                Callback.scf(true);
                break;
            default:
                Log.logging(Log.LogTypes.BIOS, Log.LogServerities.Error, "INT1A:Undefined call %2X",
                        Register.getRegAH());
                break;
        }
        return Callback.ReturnTypeNone;
    }

    private static int INT11Handler() {
        Register.setRegAX(Memory.readW(BIOS_CONFIGURATION));
        return Callback.ReturnTypeNone;
    }

    private static int INT1CHandler() {
        return Callback.ReturnTypeNone;
    }

    private static int INT12Handler() {
        Register.setRegAX(Memory.readW(BIOS_MEMORY_SIZE));
        return Callback.ReturnTypeNone;
    }

    private static int INT17Handler() {
        Log.logging(Log.LogTypes.BIOS, Log.LogServerities.Normal, "INT17:Function %X",
                Register.getRegAH());
        switch (Register.getRegAH()) {
            case 0x00: /* PRINTER: Write Character */
                Register.setRegAH(1); /* Report a timeout */
                break;
            case 0x01: /* PRINTER: Initialize port */
                break;
            case 0x02: /* PRINTER: Get Status */
                Register.setRegAH(0);
                break;
            case 0x20: /* Some sort of printerdriver install check */
                break;
            default:
                Support.exceptionExit("Unhandled INT 17 call %2X", Register.getRegAH());
                break;
        }
        return Callback.ReturnTypeNone;
    }

    // uint8(uint16, uint8, uint8)
    private static int INT14Wait(int port, int mask, int timeout) {
        double starttime = PIC.getFullIndex();
        double timeout_f = timeout * 1000.0;
        int retval;
        while (((retval = IO.readB(port)) & mask) != mask) {
            if (starttime < (PIC.getFullIndex() - timeout_f)) {
                retval |= 0x80;
                break;
            }
            Callback.idle();
        }
        return retval;
    }

    private static int INT14Handler() {
        // 0-3 serial port functions and no more than 4 serial ports
        if (Register.getRegAH() > 0x3 || Register.getRegDX() > 0x3) {
            Log.logMsg("BIOS INT14: Unhandled call AH=%2X DX=%4x", Register.getRegAH(),
                    Register.getRegDX());
            return Callback.ReturnTypeNone;
        }
        // DX is always port number
        int port = Memory.realReadW(0x40, (Register.getRegDX() * 2));
        int timeout = Memory.readB(BIOS_COM1_TIMEOUT + Register.getRegDX());
        if (port == 0) {
            Log.logging(Log.LogTypes.BIOS, Log.LogServerities.Normal,
                    "BIOS INT14: port %d does not exist.", Register.getRegDX());
            return Callback.ReturnTypeNone;
        }
        switch (Register.getRegAH()) {
            case 0x00: {
                // Initialize port
                // Parameters: Return:
                // AL: port parameters AL: modem status
                // AH: line status

                // set baud rate
                int baudrate = 9600;
                int baudresult;
                int rawbaud = Register.getRegAL() >>> 5;

                if (rawbaud == 0) {
                    baudrate = 110;
                } else if (rawbaud == 1) {
                    baudrate = 150;
                } else if (rawbaud == 2) {
                    baudrate = 300;
                } else if (rawbaud == 3) {
                    baudrate = 600;
                } else if (rawbaud == 4) {
                    baudrate = 1200;
                } else if (rawbaud == 5) {
                    baudrate = 2400;
                } else if (rawbaud == 6) {
                    baudrate = 4800;
                } else if (rawbaud == 7) {
                    baudrate = 9600;
                }

                baudresult = 115200 / baudrate;

                IO.writeB(port + 3, 0x80); // enable divider access
                IO.writeB(port, baudresult & 0xff);
                IO.writeB(port + 1, baudresult >>> 8);

                // set line parameters, disable divider access
                IO.writeB(port + 3, Register.getRegAL() & 0x1F); // LCR

                // disable interrupts
                IO.writeB(port + 1, 0); // IER

                // get result
                Register.setRegAH(IO.readB(port + 5) & 0xff);
                Register.setRegAL(IO.readB(port + 6) & 0xff);
                Callback.scf(false);
                break;
            }
            case 0x01: { // Transmit character
                         // Parameters: Return:
                         // AL: character AL: unchanged
                         // AH: 0x01 AH: line status from just before the char was sent
                         // (0x80 | unpredicted) in case of timeout
                         // [undoc] (0x80 | line status) in case of tx timeout
                         // [undoc] (0x80 | modem status) in case of dsr/cts timeout

                // set DTR & RTS on
                IO.writeB(port + 4, 0x3);

                // wait for DSR & CTS
                Register.setRegAH(INT14Wait(port + 6, 0x30, timeout));
                if ((Register.getRegAH() & 0x80) == 0) {
                    // wait for TX buffer empty
                    Register.setRegAH(INT14Wait(port + 5, 0x20, timeout));
                    if ((Register.getRegAH() & 0x80) == 0) {
                        // fianlly send the character
                        IO.writeB(port, Register.getRegAL());
                    }
                } // else timed out
                Callback.scf(false);
                break;
            }
            case 0x02: // Read character
                // Parameters: Return:
                // AH: 0x02 AL: received character
                // [undoc] will be trashed in case of timeout
                // AH: (line status & 0x1E) in case of success
                // (0x80 | unpredicted) in case of timeout
                // [undoc] (0x80 | line status) in case of rx timeout
                // [undoc] (0x80 | modem status) in case of dsr timeout

                // set DTR on
                IO.writeB(port + 4, 0x1);

                // wait for DSR
                Register.setRegAH(INT14Wait(port + 6, 0x20, timeout));
                if ((Register.getRegAH() & 0x80) == 0) {
                    // wait for character to arrive
                    Register.setRegAH(INT14Wait(port + 5, 0x01, timeout));
                    if ((Register.getRegAH() & 0x80) == 0) {
                        Register.setRegAH(Register.getRegAH() & 0x1E);
                        Register.setRegAL(IO.readB(port));
                    }
                }
                Callback.scf(false);
                break;
            case 0x03: // get status
                Register.setRegAH(IO.readB(port + 5) & 0xff);
                Register.setRegAL(IO.readB(port + 6) & 0xff);
                Callback.scf(false);
                break;

        }
        return Callback.ReturnTypeNone;
    }

    // INT15_Handler안에서만 사용
    private static int _biosConfigSeg = 0;

    private static int INT15Handler() {

        switch (Register.getRegAH()) {
            case 0x06:
                Log.logging(Log.LogTypes.BIOS, Log.LogServerities.Normal,
                        "INT15 Unkown Function 6");
                break;
            case 0xC0: /* Get Configuration */
            {
                if (_biosConfigSeg == 0)
                    _biosConfigSeg = DOSMain.getMemory(1); // We have 16 bytes
                int data = Memory.physMake(_biosConfigSeg, 0);
                Memory.writeW(data, 8); // 8 Bytes following
                if (DOSBox.isTANDYArch()) {
                    if (DOSBox.Machine == DOSBox.MachineType.TANDY) {
                        // Model ID (Tandy)
                        Memory.writeB(data + 2, 0xFF);
                    } else {
                        // Model ID (PCJR)
                        Memory.writeB(data + 2, 0xFD);
                    }
                    Memory.writeB(data + 3, 0x0A); // Submodel ID
                    Memory.writeB(data + 4, 0x10); // Bios Revision
                    /* Tandy doesn't have a 2nd PIC, left as is for now */
                    Memory.writeB(data + 5, (1 << 6) | (1 << 5) | (1 << 4)); // Feature Byte 1
                } else {
                    Memory.writeB(data + 2, 0xFC); // Model ID (PC)
                    Memory.writeB(data + 3, 0x00); // Submodel ID
                    Memory.writeB(data + 4, 0x01); // Bios Revision
                    Memory.writeB(data + 5, (1 << 6) | (1 << 5) | (1 << 4)); // Feature Byte 1
                }
                Memory.writeB(data + 6, 1 << 6); // Feature Byte 2
                Memory.writeB(data + 7, 0); // Feature Byte 3
                Memory.writeB(data + 8, 0); // Feature Byte 4
                Memory.writeB(data + 9, 0); // Feature Byte 5
                CPU.setSegGeneral(Register.SEG_NAME_ES, _biosConfigSeg);
                Register.setRegBX(0);
                Register.setRegAH(0);
                Callback.scf(false);
            }
                break;
            case 0x4f: /* BIOS - Keyboard intercept */
                /* Carry should be set but let's just set it just in case */
                Callback.scf(true);
                break;
            case 0x83: /* BIOS - SET EVENT WAIT INTERVAL */
            {
                if (Register.getRegAL() == 0x01) { /* Cancel it */
                    Memory.writeB(BIOS_WAIT_FLAG_ACTIVE, 0);
                    IO.write(0x70, 0xb);
                    IO.write(0x71, 0xff & (IO.read(0x71) & ~0x40));
                    Callback.scf(false);
                    break;
                }
                if (Memory.readB(BIOS_WAIT_FLAG_ACTIVE) != 0) {
                    Register.setRegAH(0x80);
                    Callback.scf(true);
                    break;
                }
                int count = (Register.getRegCX() << 16) | Register.getRegDX();
                Memory.writeD(BIOS_WAIT_FLAG_POINTER, Memory
                        .realMake(Register.segValue(Register.SEG_NAME_ES), Register.getRegBX()));
                Memory.writeD(BIOS_WAIT_FLAG_COUNT, count);
                Memory.writeB(BIOS_WAIT_FLAG_ACTIVE, 1);
                /* Reprogram RTC to start */
                IO.write(0x70, 0xb);
                IO.write(0x71, 0xff & (IO.read(0x71) | 0x40));
                Callback.scf(false);
            }
                break;
            case 0x84: /* BIOS - JOYSTICK SUPPORT (XT after 11/8/82,AT,XT286,PS) */
                if (Register.getRegDX() == 0x0000) {
                    // Get Joystick button status
                    if (JoysticModule.isEnabled(0) || JoysticModule.isEnabled(1)) {
                        Register.setRegAL(IO.readB(0x201) & 0xf0);
                        Callback.scf(false);
                    } else {
                        // dos values
                        Register.setRegAX(0x00f0);
                        Register.setRegDX(0x0201);
                        Callback.scf(true);
                    }
                } else if (Register.getRegDX() == 0x0001) {
                    if (JoysticModule.isEnabled(0)) {
                        Register.setRegAX((int) (JoysticModule.getMoveX(0) * 127 + 128));
                        Register.setRegBX((int) (JoysticModule.getMoveY(0) * 127 + 128));
                        if (JoysticModule.isEnabled(1)) {
                            Register.setRegCX((int) (JoysticModule.getMoveX(1) * 127 + 128));
                            Register.setRegDX((int) (JoysticModule.getMoveY(1) * 127 + 128));
                        } else {
                            Register.setRegCX(0);
                            Register.setRegDX(0);
                        }
                        Callback.scf(false);
                    } else if (JoysticModule.isEnabled(1)) {
                        Register.setRegAX(0);
                        Register.setRegBX(0);
                        Register.setRegCX((int) (JoysticModule.getMoveX(1) * 127 + 128));
                        Register.setRegDX((int) (JoysticModule.getMoveY(1) * 127 + 128));
                        Callback.scf(false);
                    } else {
                        Register.setRegAX(0);
                        Register.setRegBX(0);
                        Register.setRegCX(0);
                        Register.setRegDX(0);
                        Callback.scf(true);
                    }
                } else {
                    Log.logging(Log.LogTypes.BIOS, Log.LogServerities.Error,
                            "INT15:84:Unknown Bios Joystick functionality.");
                }
                break;
            case 0x86: /* BIOS - WAIT (AT,PS) */
            {

                if (Memory.readB(BIOS_WAIT_FLAG_ACTIVE) != 0) {
                    Register.setRegAH(0x83);
                    Callback.scf(true);
                    break;
                }
                int count = (Register.getRegCX() << 16) | Register.getRegDX();
                Memory.writeD(BIOS_WAIT_FLAG_POINTER, Memory.realMake(0, BIOS_WAIT_FLAG_TEMP));
                Memory.writeD(BIOS_WAIT_FLAG_COUNT, count);
                Memory.writeB(BIOS_WAIT_FLAG_ACTIVE, 1);
                /* Reprogram RTC to start */
                IO.write(0x70, 0xb);
                IO.write(0x71, 0xff & (IO.read(0x71) | 0x40));
                while (Memory.readD(BIOS_WAIT_FLAG_COUNT) != 0) {
                    Callback.idle();
                }
                Callback.scf(false);
            }
            // goto GotoCase_0x87;
            case 0x87: /* Copy extended memory */
            // GotoCase_0x87:
            {
                boolean enabled = Memory.A20Enabled();
                Memory.A20Enable(true);
                int bytes = Register.getRegCX() * 2;
                int data = Register.segPhys(Register.SEG_NAME_ES) + Register.getRegSI();
                int source = (Memory.readD(data + 0x12) & 0x00FFFFFF)
                        + (Memory.readB(data + 0x16) << 24);
                int dest = (Memory.readD(data + 0x1A) & 0x00FFFFFF)
                        + (Memory.readB(data + 0x1E) << 24);
                Memory.blockCopy(dest, source, bytes);
                Register.setRegAX(0x00);
                Memory.A20Enable(enabled);
                Callback.scf(false);
                break;
            }
            case 0x88: /* SYSTEM - GET EXTENDED MEMORY SIZE (286+) */
                Register.setRegAX(_otherMemSystems != 0 ? 0 : _sizeExtended);
                Log.logging(Log.LogTypes.BIOS, Log.LogServerities.Normal,
                        "INT15:Function 0x88 Remaining %04X kb", Register.getRegAX());
                Callback.scf(false);
                break;
            case 0x89: /* SYSTEM - SWITCH TO PROTECTED MODE */
            {
                IO.write(0x20, 0x10);
                IO.write(0x21, Register.getRegBH());
                IO.write(0x21, 0);
                IO.write(0xA0, 0x10);
                IO.write(0xA1, Register.getRegBL());
                IO.write(0xA1, 0);
                Memory.A20Enable(true);
                int table = Register.segPhys(Register.SEG_NAME_ES) + Register.getRegSI();
                CPU.lgdt(Memory.readW(table + 0x8), Memory.readD(table + 0x8 + 0x2) & 0xFFFFFF);
                CPU.lidt(Memory.readW(table + 0x10), Memory.readD(table + 0x10 + 0x2) & 0xFFFFFF);
                CPU.setCRX(0, CPU.getCRX(0) | 1);
                CPU.setSegGeneral(Register.SEG_NAME_DS, 0x18);
                CPU.setSegGeneral(Register.SEG_NAME_ES, 0x20);
                CPU.setSegGeneral(Register.SEG_NAME_SS, 0x28);
                Register.setRegSP(Register.getRegSP() + 6); // Clear stack of interrupt frame
                CPU.setFlags(0, Register.FMaskAll);
                Register.setRegAX(0);
                CPU.jmp(false, 0x30, Register.getRegCX(), 0);
            }
                break;
            case 0x90: /* OS HOOK - DEVICE BUSY */
                Callback.scf(false);
                Register.setRegAH(0);
                break;
            case 0x91: /* OS HOOK - DEVICE POST */
                Callback.scf(false);
                Register.setRegAH(0);
                break;
            case 0xc2: /* BIOS PS2 Pointing Device Support */
                switch (Register.getRegAL()) {
                    case 0x00: // enable/disable
                        if (Register.getRegBH() == 0) { // disable
                            Mouse.instance().setPS2State(false);
                            Register.setRegAH(0);
                            Callback.scf(false);
                        } else if (Register.getRegBH() == 0x01) { // enable
                            if (!Mouse.instance().setPS2State(true)) {
                                Register.setRegAH(5);
                                Callback.scf(true);
                                break;
                            }
                            Register.setRegAH(0);
                            Callback.scf(false);
                        } else {
                            Callback.scf(true);
                            Register.setRegAH(1);
                        }
                        break;
                    case 0x01: // reset
                        // mouse fall through goto GotoCase_0xc2_0x05;
                        Register.setRegBX(0x00aa);
                    case 0x05: // initialize
                        // GotoCase_0xc2_0x05:
                        Mouse.instance().setPS2State(false);
                        Callback.scf(false);
                        Register.setRegAH(0);
                        break;
                    case 0x02: // set sampling rate
                    case 0x03: // set resolution
                        Callback.scf(false);
                        Register.setRegAH(0);
                        break;
                    case 0x04: // get type
                        Register.setRegBH(0); // ID
                        Callback.scf(false);
                        Register.setRegAH(0);
                        break;
                    case 0x06: // extended commands
                        if ((Register.getRegBH() == 0x01) || (Register.getRegBH() == 0x02)) {
                            Callback.scf(false);
                            Register.setRegAH(0);
                        } else {
                            Callback.scf(true);
                            Register.setRegAH(1);
                        }
                        break;
                    case 0x07: // set callback
                        Mouse.instance().changePS2Callback(Register.segValue(Register.SEG_NAME_ES),
                                Register.getRegBX());
                        Callback.scf(false);
                        Register.setRegAH(0);
                        break;
                    default:
                        Callback.scf(true);
                        Register.setRegAH(1);
                        break;
                }
                break;
            case 0xc3: /* set carry flag so BorlandRTM doesn't assume a VECTRA/PS2 */
                Register.setRegAH(0x86);
                Callback.scf(true);
                break;
            case 0xc4: /* BIOS POS Programm option Select */
                Log.logging(Log.LogTypes.BIOS, Log.LogServerities.Normal,
                        "INT15:Function %X called, bios mouse not supported", Register.getRegAH());
                Callback.scf(true);
                break;
            default:
                Log.logging(Log.LogTypes.BIOS, Log.LogServerities.Error, "INT15:Unknown call %4X",
                        Register.getRegAX());
                Register.setRegAH(0x86);
                Callback.scf(true);
                if ((DOSBox.isEGAVGAArch()) || (DOSBox.Machine == DOSBox.MachineType.CGA)) {
                    /* relict from comparisons, as int15 exits with a retf2 instead of an iret */
                    Callback.szf(false);
                }
                break;
        }
        return Callback.ReturnTypeNone;
    }

    private static int INT70Handler() {
        /* Acknowledge irq with cmos */
        IO.write(0x70, 0xc);
        IO.read(0x71);
        if (Memory.readB(BIOS_WAIT_FLAG_ACTIVE) != 0) {
            int count = Memory.readD(BIOS_WAIT_FLAG_COUNT);
            if (count > 997) {
                Memory.writeD(BIOS_WAIT_FLAG_COUNT, count - 997);
            } else {
                Memory.writeD(BIOS_WAIT_FLAG_COUNT, 0);
                int where = Memory.real2Phys(Memory.readD(BIOS_WAIT_FLAG_POINTER));
                Memory.writeB(where, Memory.readB(where) | 0x80);
                Memory.writeB(BIOS_WAIT_FLAG_ACTIVE, 0);
                Memory.writeD(BIOS_WAIT_FLAG_POINTER, Memory.realMake(0, BIOS_WAIT_FLAG_TEMP));
                IO.write(0x70, 0xb);
                IO.write(0x71, 0xff & (IO.read(0x71) & ~0x40));
            }
        }
        /* Signal EOI to both pics */
        IO.write(0xa0, 0x20);
        IO.write(0x20, 0x20);
        return 0;
    }

    private static BIOSModule bios = null;

    private static void destroy(Section sec) {
        bios.dispose();
        bios = null;
    }

    public static void init(Section sec) {
        bios = (new BIOS()).new BIOSModule(sec);
        sec.addDestroyFunction(BIOS::destroy, false);
    }


}
