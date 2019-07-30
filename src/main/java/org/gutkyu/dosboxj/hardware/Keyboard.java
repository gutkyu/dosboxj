package org.gutkyu.dosboxj.hardware;

import org.gutkyu.dosboxj.misc.setup.*;
import org.gutkyu.dosboxj.hardware.io.*;
import org.gutkyu.dosboxj.hardware.memory.*;
import org.gutkyu.dosboxj.hardware.sound.PCSpeaker;
import org.gutkyu.dosboxj.DOSBox;
import org.gutkyu.dosboxj.misc.*;

public final class Keyboard {
    private Keyboard() {

    }

    public enum KBDKeys {
        KBD_NONE, KBD_1, KBD_2, KBD_3, KBD_4, KBD_5, KBD_6, KBD_7, KBD_8, KBD_9, KBD_0, KBD_q, KBD_w, KBD_e, KBD_r, KBD_t, KBD_y, KBD_u, KBD_i, KBD_o, KBD_p, KBD_a, KBD_s, KBD_d, KBD_f, KBD_g, KBD_h, KBD_j, KBD_k, KBD_l, KBD_z, KBD_x, KBD_c, KBD_v, KBD_b, KBD_n, KBD_m, KBD_f1, KBD_f2, KBD_f3, KBD_f4, KBD_f5, KBD_f6, KBD_f7, KBD_f8, KBD_f9, KBD_f10, KBD_f11, KBD_f12,

        /* Now the weirder keys */

        KBD_esc, KBD_tab, KBD_backspace, KBD_enter, KBD_space, KBD_leftalt, KBD_rightalt, KBD_leftctrl, KBD_rightctrl, KBD_leftshift, KBD_rightshift, KBD_capslock, KBD_scrolllock, KBD_numlock,

        KBD_grave, KBD_minus, KBD_equals, KBD_backslash, KBD_leftbracket, KBD_rightbracket, KBD_semicolon, KBD_quote, KBD_period, KBD_comma, KBD_slash, KBD_extra_lt_gt,

        KBD_printscreen, KBD_pause, KBD_insert, KBD_home, KBD_pageup, KBD_delete, KBD_end, KBD_pagedown, KBD_left, KBD_up, KBD_down, KBD_right,

        KBD_kp1, KBD_kp2, KBD_kp3, KBD_kp4, KBD_kp5, KBD_kp6, KBD_kp7, KBD_kp8, KBD_kp9, KBD_kp0, KBD_kpdivide, KBD_kpmultiply, KBD_kpminus, KBD_kpplus, KBD_kpenter, KBD_kpperiod,


        KBD_LAST
    }

    private static final int KEYBUFSIZE = 32;
    private static final float KEYDELAY = 0.300f; // Considering 20-30 khz serial clock and 11
                                                  // bits/char

    enum KeyCommands {
        CMD_NONE, CMD_SETLEDS, CMD_SETTYPERATE, CMD_SETOUTPORT
    }

    // -- #region keyb
    private byte[] buffer = new byte[KEYBUFSIZE];
    private int used;
    private int pos;

    private KBDKeys repeatKey;
    private int repeatWait;
    private int repeatPause, repeatRate;
    private KeyCommands command;
    private byte p60data;
    private boolean p60changed;
    private boolean active;
    private boolean scanning;
    private boolean scheduled;
    // -- #endregion

    private void setPort60(byte val) {
        p60changed = true;
        p60data = val;
        if (DOSBox.Machine == DOSBox.MachineType.PCJR)
            PIC.activateIRQ(6);
        else
            PIC.activateIRQ(1);
    }

    private EventHandler transferBufferWrap = this::transferBuffer;

    private void transferBuffer(int val) {
        scheduled = false;
        if (used == 0) {
            // LOG(LOG_KEYBOARD,LOG_NORMAL)("Transfer started with empty buffer");
            return;
        }
        setPort60(buffer[pos]);
        if (++pos >= KEYBUFSIZE)
            pos -= KEYBUFSIZE;
        used--;
    }

    // KEYBOARD_ClrBuffer
    public void clrBuffer() {
        used = 0;
        pos = 0;
        PIC.removeEvents(this::transferBuffer);
        scheduled = false;
    }

    // (uint8)
    private void addBuffer(int data) {
        if (used >= KEYBUFSIZE) {
            // LOG(LOG_KEYBOARD,LOG_NORMAL)("Buffer full, dropping code");
            return;
        }
        int start = pos + used;
        if (start >= KEYBUFSIZE)
            start -= KEYBUFSIZE;
        buffer[start] = (byte) data;
        used++;
        /* Start up an event to start the first IRQ */
        if (!scheduled && !p60changed) {
            scheduled = true;
            PIC.addEvent(transferBufferWrap, KEYDELAY);
        }
    }

    // uint32(uint32, uint32)
    private int readP60(int port, int iolen) {
        p60changed = false;
        if (!scheduled && used != 0) {
            scheduled = true;
            PIC.addEvent(transferBufferWrap, KEYDELAY);
        }
        return 0xff & p60data;
    }

    private static final int[] delay = new int[] {250, 500, 750, 1000};
    private static final int[] repeat =
            new int[] {33, 37, 42, 46, 50, 54, 58, 63, 67, 75, 83, 92, 100, 109, 118, 125, 133, 149,
                    167, 182, 200, 217, 233, 250, 270, 303, 333, 370, 400, 435, 476, 500};

    private void writeP60(int port, int val, int iolen) {
        switch (command) {
            case CMD_NONE: /* None */
                /* No active command this would normally get sent to the keyboard then */
                clrBuffer();
                switch (val) {
                    case 0xed: /* Set Leds */
                        command = KeyCommands.CMD_SETLEDS;
                        addBuffer(0xfa); /* Acknowledge */
                        break;
                    case 0xee: /* Echo */
                        addBuffer(0xfa); /* Acknowledge */
                        break;
                    case 0xf2: /* Identify keyboard */
                        /* AT's just send acknowledge */
                        addBuffer(0xfa); /* Acknowledge */
                        break;
                    case 0xf3: /* Typematic rate programming */
                        command = KeyCommands.CMD_SETTYPERATE;
                        addBuffer(0xfa); /* Acknowledge */
                        break;
                    case 0xf4: /* Enable keyboard,clear buffer, start scanning */
                        // LOG(LOG_KEYBOARD,LOG_NORMAL)("Clear buffer,enable Scaning");
                        addBuffer(0xfa); /* Acknowledge */
                        scanning = true;
                        break;
                    case 0xf5: /* Reset keyboard and disable scanning */
                        // LOG(LOG_KEYBOARD,LOG_NORMAL)("Reset, disable scanning");
                        scanning = false;
                        addBuffer(0xfa); /* Acknowledge */
                        break;
                    case 0xf6: /* Reset keyboard and enable scanning */
                        // LOG(LOG_KEYBOARD,LOG_NORMAL)("Reset, enable scanning");
                        addBuffer(0xfa); /* Acknowledge */
                        scanning = false;
                        break;
                    default:
                        /* Just always acknowledge strange commands */
                        // LOG(LOG_KEYBOARD,LOG_ERROR)("60:Unhandled command %X",val);
                        addBuffer(0xfa); /* Acknowledge */
                        break;
                }
                return;
            case CMD_SETOUTPORT:
                Memory.A20Enable((val & 2) > 0);
                command = KeyCommands.CMD_NONE;
                break;
            case CMD_SETTYPERATE: {

                repeatPause = delay[(val >>> 5) & 3];
                repeatRate = repeat[val & 0x1f];
                command = KeyCommands.CMD_NONE;
                // goto KeyCmdSETLEDS;
            }
            /* Fallthrough! as setleds does what we want */
            case CMD_SETLEDS:
                // KeyCmdSETLEDS:
                command = KeyCommands.CMD_NONE;
                clrBuffer();
                addBuffer(0xfa); /* Acknowledge */
                break;
        }
    }

    private byte port61Data = 0;

    private int readP61(int port, int iolen) {
        port61Data ^= 0x20;
        port61Data ^= 0x10;
        return 0xff & port61Data;
    }

    private void writeP61(int port, int val, int iolen) {
        if (((port61Data ^ val) & 3) != 0) {
            if (((port61Data ^ val) & 1) != 0)
                Timer.setGate2((val & 0x1) != 0);
            PCSpeaker.setType(val & 3);
        }
        port61Data = (byte) val;
    }

    private void writeP64(int port, int val, int iolen) {
        switch (val) {
            case 0xae: /* Activate keyboard */
                active = true;
                if (used != 0 && !scheduled && !p60changed) {
                    scheduled = true;
                    PIC.addEvent(transferBufferWrap, KEYDELAY);
                }
                // LOG(LOG_KEYBOARD,LOG_NORMAL)("Activated");
                break;
            case 0xad: /* Deactivate keyboard */
                active = false;
                // LOG(LOG_KEYBOARD,LOG_NORMAL)("De-Activated");
                break;
            case 0xd0: /* Outport on buffer */
                setPort60((byte) (Memory.A20Enabled() ? 0x02 : 0));
                break;
            case 0xd1: /* Write to outport */
                command = KeyCommands.CMD_SETOUTPORT;
                break;
            default:
                // LOG(LOG_KEYBOARD,LOG_ERROR)("Port 64 write with val %d",val);
                break;
        }
    }

    private int readP64(int port, int iolen) {
        return 0xff & (0x1c | (p60changed ? 0x1 : 0x0));
    }

    public void addKey(KBDKeys keytype, boolean pressed) {
        int ret = 0;// uint8
        boolean extend = false;
        switch (keytype) {
            case KBD_esc:
                ret = 1;
                break;
            case KBD_1:
                ret = 2;
                break;
            case KBD_2:
                ret = 3;
                break;
            case KBD_3:
                ret = 4;
                break;
            case KBD_4:
                ret = 5;
                break;
            case KBD_5:
                ret = 6;
                break;
            case KBD_6:
                ret = 7;
                break;
            case KBD_7:
                ret = 8;
                break;
            case KBD_8:
                ret = 9;
                break;
            case KBD_9:
                ret = 10;
                break;
            case KBD_0:
                ret = 11;
                break;

            case KBD_minus:
                ret = 12;
                break;
            case KBD_equals:
                ret = 13;
                break;
            case KBD_backspace:
                ret = 14;
                break;
            case KBD_tab:
                ret = 15;
                break;

            case KBD_q:
                ret = 16;
                break;
            case KBD_w:
                ret = 17;
                break;
            case KBD_e:
                ret = 18;
                break;
            case KBD_r:
                ret = 19;
                break;
            case KBD_t:
                ret = 20;
                break;
            case KBD_y:
                ret = 21;
                break;
            case KBD_u:
                ret = 22;
                break;
            case KBD_i:
                ret = 23;
                break;
            case KBD_o:
                ret = 24;
                break;
            case KBD_p:
                ret = 25;
                break;

            case KBD_leftbracket:
                ret = 26;
                break;
            case KBD_rightbracket:
                ret = 27;
                break;
            case KBD_enter:
                ret = 28;
                break;
            case KBD_leftctrl:
                ret = 29;
                break;

            case KBD_a:
                ret = 30;
                break;
            case KBD_s:
                ret = 31;
                break;
            case KBD_d:
                ret = 32;
                break;
            case KBD_f:
                ret = 33;
                break;
            case KBD_g:
                ret = 34;
                break;
            case KBD_h:
                ret = 35;
                break;
            case KBD_j:
                ret = 36;
                break;
            case KBD_k:
                ret = 37;
                break;
            case KBD_l:
                ret = 38;
                break;
            case KBD_semicolon:
                ret = 39;
                break;
            case KBD_quote:
                ret = 40;
                break;
            case KBD_grave:
                ret = 41;
                break;
            case KBD_leftshift:
                ret = 42;
                break;
            case KBD_backslash:
                ret = 43;
                break;
            case KBD_z:
                ret = 44;
                break;
            case KBD_x:
                ret = 45;
                break;
            case KBD_c:
                ret = 46;
                break;
            case KBD_v:
                ret = 47;
                break;
            case KBD_b:
                ret = 48;
                break;
            case KBD_n:
                ret = 49;
                break;
            case KBD_m:
                ret = 50;
                break;
            case KBD_comma:
                ret = 51;
                break;
            case KBD_period:
                ret = 52;
                break;
            case KBD_slash:
                ret = 53;
                break;
            case KBD_rightshift:
                ret = 54;
                break;
            case KBD_kpmultiply:
                ret = 55;
                break;
            case KBD_leftalt:
                ret = 56;
                break;
            case KBD_space:
                ret = 57;
                break;
            case KBD_capslock:
                ret = 58;
                break;
            case KBD_f1:
                ret = 59;
                break;
            case KBD_f2:
                ret = 60;
                break;
            case KBD_f3:
                ret = 61;
                break;
            case KBD_f4:
                ret = 62;
                break;
            case KBD_f5:
                ret = 63;
                break;
            case KBD_f6:
                ret = 64;
                break;
            case KBD_f7:
                ret = 65;
                break;
            case KBD_f8:
                ret = 66;
                break;
            case KBD_f9:
                ret = 67;
                break;
            case KBD_f10:
                ret = 68;
                break;
            case KBD_numlock:
                ret = 69;
                break;
            case KBD_scrolllock:
                ret = 70;
                break;
            case KBD_kp7:
                ret = 71;
                break;
            case KBD_kp8:
                ret = 72;
                break;
            case KBD_kp9:
                ret = 73;
                break;
            case KBD_kpminus:
                ret = 74;
                break;
            case KBD_kp4:
                ret = 75;
                break;
            case KBD_kp5:
                ret = 76;
                break;
            case KBD_kp6:
                ret = 77;
                break;
            case KBD_kpplus:
                ret = 78;
                break;
            case KBD_kp1:
                ret = 79;
                break;
            case KBD_kp2:
                ret = 80;
                break;
            case KBD_kp3:
                ret = 81;
                break;
            case KBD_kp0:
                ret = 82;
                break;
            case KBD_kpperiod:
                ret = 83;
                break;

            case KBD_extra_lt_gt:
                ret = 86;
                break;
            case KBD_f11:
                ret = 87;
                break;
            case KBD_f12:
                ret = 88;
                break;

            // The Extended keys

            case KBD_kpenter:
                extend = true;
                ret = 28;
                break;
            case KBD_rightctrl:
                extend = true;
                ret = 29;
                break;
            case KBD_kpdivide:
                extend = true;
                ret = 53;
                break;
            case KBD_rightalt:
                extend = true;
                ret = 56;
                break;
            case KBD_home:
                extend = true;
                ret = 71;
                break;
            case KBD_up:
                extend = true;
                ret = 72;
                break;
            case KBD_pageup:
                extend = true;
                ret = 73;
                break;
            case KBD_left:
                extend = true;
                ret = 75;
                break;
            case KBD_right:
                extend = true;
                ret = 77;
                break;
            case KBD_end:
                extend = true;
                ret = 79;
                break;
            case KBD_down:
                extend = true;
                ret = 80;
                break;
            case KBD_pagedown:
                extend = true;
                ret = 81;
                break;
            case KBD_insert:
                extend = true;
                ret = 82;
                break;
            case KBD_delete:
                extend = true;
                ret = 83;
                break;
            case KBD_pause:
                addBuffer(0xe1);
                addBuffer(0xff & (29 | (pressed ? 0 : 0x80)));
                addBuffer(0xff & (69 | (pressed ? 0 : 0x80)));
                return;
            case KBD_printscreen:
                /* Not handled yet. But usuable in mapper for special events */
                return;
            default:
                Support.exceptionExit("Unsupported key press");
                break;
        }
        /* Add the actual key in the keyboard queue */
        if (pressed) {
            if (repeatKey == keytype)
                repeatWait = repeatRate;
            else
                repeatWait = repeatPause;
            repeatKey = keytype;
        } else {
            repeatKey = KBDKeys.KBD_NONE;
            repeatWait = 0;
            ret += 128;
        }
        if (extend)
            addBuffer(0xe0);
        addBuffer(ret);
    }

    private void tickHandler() {
        if (repeatWait != 0) {
            repeatWait--;
            if (repeatWait == 0)
                addKey(repeatKey, true);
        }
    }

    private static Keyboard kbd = null;

    public static void init(Section sec) {
        if (kbd == null)
            kbd = new Keyboard();
        IO.registerWriteHandler(0x60, kbd::writeP60, IO.IO_MB);
        IO.registerReadHandler(0x60, kbd::readP60, IO.IO_MB);
        IO.registerWriteHandler(0x61, kbd::writeP61, IO.IO_MB);
        IO.registerReadHandler(0x61, kbd::readP61, IO.IO_MB);
        IO.registerWriteHandler(0x64, kbd::writeP64, IO.IO_MB);
        IO.registerReadHandler(0x64, kbd::readP64, IO.IO_MB);
        Timer.addTickHandler(kbd::tickHandler);
        kbd.writeP61(0, 0, 0);
        /* Init the keyb struct */
        kbd.active = true;
        kbd.scanning = true;
        kbd.command = KeyCommands.CMD_NONE;
        kbd.p60changed = false;
        kbd.repeatKey = KBDKeys.KBD_NONE;
        kbd.repeatPause = 500;
        kbd.repeatRate = 33;
        kbd.repeatWait = 0;
        kbd.clrBuffer();
    }

    public static Keyboard instance() {
        return kbd;
    }
}
