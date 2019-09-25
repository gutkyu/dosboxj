package org.gutkyu.dosboxj.interrupt;

import org.gutkyu.dosboxj.misc.setup.*;
import org.gutkyu.dosboxj.cpu.*;
import org.gutkyu.dosboxj.util.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.gutkyu.dosboxj.DOSBox;
import org.gutkyu.dosboxj.dos.*;
import org.gutkyu.dosboxj.hardware.*;
import org.gutkyu.dosboxj.hardware.io.*;
import org.gutkyu.dosboxj.hardware.memory.*;
import org.gutkyu.dosboxj.hardware.video.*;
import org.gutkyu.dosboxj.interrupt.bios.*;
import org.gutkyu.dosboxj.interrupt.int10.*;
import org.gutkyu.dosboxj.gui.*;


public final class Mouse {
    private int callINT33, callINT74, int74RetCallback, callMouseBD;
    private int ps2CbSeg, ps2CbOfs;
    private boolean usePS2Callback, ps2CallbackInit;
    private int callPS2;
    private int ps2Callback;
    private short oldMouseX, oldMouseY;

    private static final int QUEUE_SIZE = 32;
    private static final int MOUSE_BUTTONS = 3;
    private static final int MOUSE_IRQ = 12;

    private short posX() {
        return (short) (((short) (mouse.X)) & mouse.GranMask);
    }

    private short posY() {
        return (short) (mouse.Y);
    }

    private static final int CURSOR_X = 16;
    private static final int CURSOR_Y = 16;
    private static final int HIGHESTBIT = (1 << (CURSOR_X - 1));

    private Mouse() {
        mouse = new MouseStruct();
    }

    private static Mouse mouseObj = null;

    public static Mouse instance() {
        return mouseObj;
    }

    DOSActionBool AutoLock;

    public static class AutoLockBinder {
        public AutoLockBinder(MouseAutoLockable autoLockProvider) {
            if (Mouse.mouseObj == null)
                mouseObj = new Mouse();
            mouseObj.AutoLock = autoLockProvider::MouseAutoLock;
        }
    }

    public boolean setPS2State(boolean use) {
        if (use && (!ps2CallbackInit)) {
            usePS2Callback = false;
            PIC.setIRQMask(MOUSE_IRQ, true);
            return false;
        }
        usePS2Callback = use;
        AutoLock.run(usePS2Callback);
        PIC.setIRQMask(MOUSE_IRQ, !usePS2Callback);
        return true;
    }

    // public void ChangePS2Callback(short pseg, short pofs) {
    public void changePS2Callback(int pseg, int pofs) {
        if ((pseg == 0) && (pofs == 0)) {
            ps2CallbackInit = false;
            AutoLock.run(false);
        } else {
            ps2CallbackInit = true;
            ps2CbSeg = 0xffff & pseg;
            ps2CbOfs = 0xffff & pofs;
        }
        AutoLock.run(ps2CallbackInit);
    }

    // (UInt16 data, Int16 mouseX, Int16 mouseY)
    private void doPS2Callback(int data, short mouseX, short mouseY) {
        if (usePS2Callback) {
            int mDat = (data & 0x03) | 0x08;
            short xDiff = (short) (mouseX - oldMouseX);
            short yDiff = (short) (oldMouseY - mouseY);
            oldMouseX = mouseX;
            oldMouseY = mouseY;
            if ((xDiff > 0xff) || (xDiff < -0xff))
                mDat |= 0x40; // x overflow
            if ((yDiff > 0xff) || (yDiff < -0xff))
                mDat |= 0x80; // y overflow
            xDiff %= 256;
            yDiff %= 256;
            if (xDiff < 0) {
                xDiff = (short) (0x100 + xDiff);
                mDat |= 0x10;
            }
            if (yDiff < 0) {
                yDiff = (short) (0x100 + yDiff);
                mDat |= 0x20;
            }
            CPU.push16(mDat);
            CPU.push16(xDiff % 256);
            CPU.push16(yDiff % 256);
            CPU.push16(0);
            CPU.push16(Memory.realSeg(ps2Callback));
            CPU.push16(Memory.realOff(ps2Callback));
            Register.segSet16(Register.SEG_NAME_CS, ps2CbSeg);
            Register.setRegIP(ps2CbOfs);
        }
    }

    private int ps2Handler() {
        CPU.pop16();
        CPU.pop16();
        CPU.pop16();
        CPU.pop16();// remove the 4 words
        return Callback.ReturnTypeNone;
    }

    private static final int X_MICKEY = 8;// SDL아래에서 유효한 마우스 이동 기본 단위같음. WPF에 적용할 필요 있을까?
    private static final int Y_MICKEY = 8;

    private static final int MOUSE_HAS_MOVED = 1;
    private static final int MOUSE_LEFT_PRESSED = 2;
    private static final int MOUSE_LEFT_RELEASED = 4;
    private static final int MOUSE_RIGHT_PRESSED = 8;
    private static final int MOUSE_RIGHT_RELEASED = 16;
    private static final int MOUSE_MIDDLE_PRESSED = 32;
    private static final int MOUSE_MIDDLE_RELEASED = 64;
    private static final float MOUSE_DELAY = 5.0f;



    // (byte type)
    private void addEvent(int type) {
        if (mouse.Events < QUEUE_SIZE) {
            if (mouse.Events > 0) {
                /* Skip duplicate events */
                if (type == MOUSE_HAS_MOVED)
                    return;
                /*
                 * Always put the newest element in the front as that the events are handled
                 * backwards (prevents doubleclicks while moving)
                 */
                for (int i = mouse.Events; i != 0; i--)
                    mouse.EventQueue[i] = mouse.EventQueue[i - 1];
            }
            mouse.EventQueue[0].Type = type;
            mouse.EventQueue[0].Buttons = 0xff & mouse.Buttons;
            mouse.Events++;
        }
        if (!mouse.TimerInProgress) {
            mouse.TimerInProgress = true;
            PIC.addEvent(limitEventsWrap, MOUSE_DELAY);
            PIC.activateIRQ(MOUSE_IRQ);
        }
    }

    private short defaultTextAndMask = 0x77FF;
    private short defaultTextXorMask = 0x7700;

    private short[] defaultScreenMask =
            {0x3FFF, 0x1FFF, 0x0FFF, 0x07FF, 0x03FF, 0x01FF, 0x00FF, 0x007F, 0x003F, 0x001F, 0x01FF,
                    0x00FF, 0x30FF, (short) 0xF87F, (short) 0xF87F, (short) 0xFCFF};

    private short[] defaultCursorMask = {0x0000, 0x4000, 0x6000, 0x7000, 0x7800, 0x7C00, 0x7E00,
            0x7F00, 0x7F80, 0x7C00, 0x6C00, 0x4600, 0x0600, 0x0300, 0x0300, 0x0000};


    // -- #region mouse
    private static class ButtonEvent {
        public int Type;// uint8
        public int Buttons;// uint8
    }

    private short[] userDefScreenMask = new short[CURSOR_Y];
    private short[] userDefCursorMask = new short[CURSOR_Y];

    private class MouseStruct {

        public byte Buttons; // 1
        public short[] TimesPressed; // 6
        public short[] TimesReleased; // 6
        public short[] LastReleasedX; // 6
        public short[] LastReleasedY; // 6
        public short[] LastPressedX; // 6
        public short[] LastPressedY; // 6
        public short Hidden; // 2
        public float addX, addY; // 4,4
        public short MinX, MaxX, MinY, MaxY; // 2,2,2,2
        public float MickeyX, MickeyY; // 4,4
        public float X, Y; // 4,4
        public ButtonEvent[] EventQueue; // 64
        public int Events;// Increase if QUEUE_SIZE >255 (currently 32) // 1
        public short SubSeg, SubOfs; // 2,2
        public short SubMask; // 2
        //
        public boolean Background; // 1
        public short BackPosX, BackPosY; // 2,2
        public byte[] BackData; // 256
        // screenMask, cursorMask가 가리키는 값은 Mouse Instance 내부에 이미 존재하기 때문에 어떤 값(default or user
        // defined)을 사용할건지만 정의하면됨
        public short[] ScreenMask;// 원 소스는 포인터 변수임 4 or 8
        public short[] CursorMask; // 원 소스는 포인터 변수임 4 or 8
        public boolean MaskIsUserDefined; //사용자 정의 마스크를 사용하고 있는지 여부
        public short ClipX, ClipY; // 2,2
        public short HotX, HotY; // 2,2
        public short TextAndMask, TextXorMask; // 2,2
        //
        public float MickeysPerPixelX; // 4
        public float MickeysPerPixelY; // 4
        public float PixelPerMickeyX; // 4
        public float PixelPerMickeyY; // 4
        public int SenvXVal; // 2, uint16
        public int SenvYVal; // 2, uint16
        public int DSpeedVal; // 2, uint16
        public float SenvX; // 4
        public float SenvY; // 4
        public short[] UpdateRegionX; // 4
        public short[] UpdateRegionY; // 4
        public short DoubleSpeedThreshold; // 2
        public short Language; // 2
        public short CursorType; // 2
        public short OldHidden; // 2
        public int Page; // 1, uint8
        public boolean Enabled; // 1
        public boolean InhibitDraw; // 1
        public boolean TimerInProgress; // 1
        public boolean InUIR; // 1
        public int Mode; // 1
        public short GranMask; // 2

        private byte[] rawData = null;
        
        public void unpack(){
            ByteBuffer unpacker = ByteBuffer.wrap(rawData).order(ByteOrder.LITTLE_ENDIAN);

            Buttons = unpacker.get();
            for (int i = 0; i < TimesPressed.length; i++) {
                TimesPressed[i] = unpacker.getShort();
            }
            for (int i = 0; i < TimesReleased.length; i++) {
                TimesReleased[i] = unpacker.getShort();
            }
            for (int i = 0; i < LastReleasedX.length; i++) {
                LastReleasedX[i] = unpacker.getShort();
            }
            for (int i = 0; i < LastReleasedY.length; i++) {
                LastReleasedY[i] = unpacker.getShort();
            }
            for (int i = 0; i < LastPressedX.length; i++) {
                LastPressedX[i] = unpacker.getShort();
            }
            for (int i = 0; i < LastPressedY.length; i++) {
                LastPressedY[i] = unpacker.getShort();
            }
            Hidden = unpacker.getShort();
            addX = unpacker.getFloat();
            addY = unpacker.getFloat();
            MinX = unpacker.getShort();
            MaxX = unpacker.getShort();
            MinY = unpacker.getShort();
            MaxY = unpacker.getShort();
            MickeyX = unpacker.getFloat();
            MickeyY = unpacker.getFloat();
            X = unpacker.getFloat();
            Y = unpacker.getFloat();

            for (ButtonEvent ev : EventQueue) {
                ev.Type = unpacker.getInt();
                ev.Buttons = unpacker.getInt();
            }
            Events = unpacker.getInt();
            SubSeg = unpacker.getShort();
            SubOfs = unpacker.getShort();
            SubMask = unpacker.getShort();
            Background = unpacker.get() == 1;
            BackPosX = unpacker.getShort();
            BackPosY = unpacker.getShort();
            for (int i = 0; i < BackData.length; i++) {
                BackData[i] = unpacker.get();
            }
            MaskIsUserDefined = unpacker.get() == 1; // 사용자 정의 마스크를 사용하고 있는지 여부
            
            ClipX = unpacker.getShort();
            ClipY = unpacker.getShort();
            HotX = unpacker.getShort();
            HotY = unpacker.getShort();
            TextAndMask = unpacker.getShort();
            TextXorMask = unpacker.getShort();
            MickeysPerPixelX = unpacker.getFloat();
            MickeysPerPixelY = unpacker.getFloat();
            PixelPerMickeyX = unpacker.getFloat();
            PixelPerMickeyY = unpacker.getFloat();
            SenvXVal = unpacker.getInt();
            SenvYVal = unpacker.getInt();
            DSpeedVal = unpacker.getInt();
            SenvX = unpacker.getFloat();
            SenvY = unpacker.getFloat();
            for (int i = 0; i < UpdateRegionX.length; i++) {
                UpdateRegionX[i] = unpacker.getShort();
            }
            for (int i = 0; i < UpdateRegionY.length; i++) {
                UpdateRegionY[i] = unpacker.getShort();
            }
            DoubleSpeedThreshold = unpacker.getShort();
            Language = unpacker.getShort();
            CursorType = unpacker.getShort();
            OldHidden = unpacker.getShort();
            Page = unpacker.getInt();
            Enabled = unpacker.get() == 1;;
            InhibitDraw = unpacker.get() == 1;
            TimerInProgress = unpacker.get() == 1;
            InUIR = unpacker.get() == 1;
            Mode = unpacker.getInt();
            GranMask = unpacker.getShort();

        }

        public void pack() {
            if(rawData == null)
                rawData = new byte[MOUSE_DATA_SIZE];
            ByteBuffer packer = ByteBuffer.wrap(rawData).order(ByteOrder.LITTLE_ENDIAN);
            
            packer.put(Buttons);
            for (short val : TimesPressed) {
                packer.putShort(val);
            }
            for (short val : TimesReleased) {
                packer.putShort(val);
            }
            for (short val : LastReleasedX) {
                packer.putShort(val);
            }
            for (short val : LastReleasedY) {
                packer.putShort(val);
            }
            for (short val : LastPressedX) {
                packer.putShort(val);
            }
            for (short val : LastPressedY) {
                packer.putShort(val);
            }
            packer.putShort(Hidden).putFloat(addX).putFloat(addY).putShort(MinX).putShort(MaxX)
                    .putShort(MinY).putShort(MaxY).putFloat(MickeyX).putFloat(MickeyY).putFloat(X)
                    .putFloat(Y);

            for (ButtonEvent ev : EventQueue) {
                packer.putInt(ev.Type).putInt(ev.Buttons);
            }
            packer.putInt(Events).putShort(SubSeg).putShort(SubOfs).putShort(SubMask)
                    .put(Background ? (byte) 1 : 0).putShort(BackPosX).putShort(BackPosY);
            for (byte val : BackData) {
                packer.put(val);
            }
            packer.put(MaskIsUserDefined ? (byte) 1 : 0) // 사용자 정의 마스크를 사용하고 있는지 여부
                    .putShort(ClipX).putShort(ClipY).putShort(HotX).putShort(HotY)
                    .putShort(TextAndMask).putShort(TextXorMask)
                    //
                    .putFloat(MickeysPerPixelX).putFloat(MickeysPerPixelY).putFloat(PixelPerMickeyX)
                    .putFloat(PixelPerMickeyY).putInt(SenvXVal).putInt(SenvYVal).putInt(DSpeedVal)
                    .putFloat(SenvX).putFloat(SenvY);
            for (short val : UpdateRegionX) {
                packer.putShort(val);
            }
            for (short val : UpdateRegionY) {
                packer.putShort(val);
            }
            packer.putShort(DoubleSpeedThreshold).putShort(Language).putShort(CursorType)
                    .putShort(OldHidden).putInt(Page).put(Enabled ? (byte) 1 : 0)
                    .put(InhibitDraw ? (byte) 1 : 0).put(TimerInProgress ? (byte) 1 : 0)
                    .put(InUIR ? (byte) 1 : 0).putInt(Mode).putShort(GranMask);

        }

        // screenMask,cursorMask가 포인터 변수이므로 4바이트로 정의
        private static final int MOUSE_DATA_SIZE = 496;

        public int size() {
            return MOUSE_DATA_SIZE;
        };

        public MouseStruct() {
            TimesPressed = new short[MOUSE_BUTTONS];
            TimesReleased = new short[MOUSE_BUTTONS];
            LastReleasedX = new short[MOUSE_BUTTONS];
            LastReleasedY = new short[MOUSE_BUTTONS];
            LastPressedX = new short[MOUSE_BUTTONS];
            LastPressedY = new short[MOUSE_BUTTONS];
            EventQueue = new ButtonEvent[QUEUE_SIZE];
            for (int i = 0; i < QUEUE_SIZE; i++) {
                EventQueue[i] = new ButtonEvent();
            }
            BackData = new byte[CURSOR_X * CURSOR_Y];
            UpdateRegionX = new short[2];
            UpdateRegionY = new short[2];

        }

    }
    // -- #endregion

    private MouseStruct mouse;

    private int int33Handler() {
        // LOG(LOG_MOUSE,LOG_NORMAL)("MOUSE: %04X %X %X %d %d",reg_ax,reg_bx,reg_cx,POS_X,POS_Y);
        switch (Register.getRegAX()) {
            case 0x00: /* Reset Driver and Read Status */
                resetHardware(); /* fallthrough */
                // goto INT33_Handler_GOTO_0x21;
            case 0x21: /* Software Reset */
                // INT33_Handler_GOTO_0x21:
                Register.setRegAX(0xffff);
                Register.setRegBX(MOUSE_BUTTONS);
                reset();
                AutoLock.run(true);
                break;
            case 0x01: /* Show Mouse */
                if (mouse.Hidden != 0)
                    mouse.Hidden--;
                AutoLock.run(true);
                drawCursor();
                break;
            case 0x02: /* Hide Mouse */
            {
                if (INT10Mode.CurMode.Type != VGAModes.TEXT)
                    restoreCursorBackground();
                else
                    restoreCursorBackgroundText();
                mouse.Hidden++;
            }
                break;
            case 0x03: /* Return position and Button Status */
                Register.setRegBX(mouse.Buttons);
                Register.setRegCX((short) posX());
                Register.setRegDX((short) posY());
                break;
            case 0x04: /* Position Mouse */
                /*
                 * If position isn't different from current position don't change it then. (as
                 * position is rounded so numbers get lost when the rounded number is set)
                 * (arena/simulation Wolf)
                 */
                if ((short) Register.getRegCX() >= mouse.MaxX)
                    mouse.X = (float) (mouse.MaxX);
                else if (mouse.MinX >= (short) Register.getRegCX())
                    mouse.X = (float) (mouse.MinX);
                else if ((short) Register.getRegCX() != posX())
                    mouse.X = (float) (Register.getRegCX());

                if ((short) Register.getRegDX() >= mouse.MaxY)
                    mouse.Y = (float) (mouse.MaxY);
                else if (mouse.MinY >= (short) Register.getRegDX())
                    mouse.Y = (float) (mouse.MinY);
                else if ((short) Register.getRegDX() != posY())
                    mouse.Y = (float) (Register.getRegDX());
                drawCursor();
                break;
            case 0x05: /* Return Button Press Data */
            {
                int but = Register.getRegBX();
                Register.setRegAX(mouse.Buttons);
                if (but >= MOUSE_BUTTONS)
                    but = MOUSE_BUTTONS - 1;
                Register.setRegCX(mouse.LastPressedX[but]);
                Register.setRegDX(mouse.LastPressedY[but]);
                Register.setRegBX(mouse.TimesPressed[but]);
                mouse.TimesPressed[but] = 0;
                break;
            }
            case 0x06: /* Return Button Release Data */
            {
                int but = Register.getRegBX();
                Register.setRegAX(mouse.Buttons);
                if (but >= MOUSE_BUTTONS)
                    but = MOUSE_BUTTONS - 1;
                Register.setRegCX(mouse.LastReleasedX[but]);
                Register.setRegDX(mouse.LastReleasedY[but]);
                Register.setRegBX(mouse.TimesReleased[but]);
                mouse.TimesReleased[but] = 0;
                break;
            }
            case 0x07: /* Define horizontal cursor range */
            { // lemmings set 1-640 and wants that. iron seeds set 0-640 but doesn't like 640
              // Iron seed works if newvideo mode with mode 13 sets 0-639
              // Larry 6 actually wants newvideo mode with mode 13 to set it to 0-319
                short max, min;// Bit16s
                if ((short) Register.getRegCX() < (short) Register.getRegDX()) {
                    min = (short) Register.getRegCX();
                    max = (short) Register.getRegDX();
                } else {
                    min = (short) Register.getRegDX();
                    max = (short) Register.getRegCX();
                }
                mouse.MinX = min;
                mouse.MaxX = max;
                /* Battlechess wants this */
                if (mouse.X > mouse.MaxX)
                    mouse.X = mouse.MaxX;
                if (mouse.X < mouse.MinX)
                    mouse.X = mouse.MinX;
                /*
                 * Or alternatively this: mouse.x = (mouse.max_x - mouse.min_x + 1)/2;
                 */
                Log.logging(Log. LogTypes.Mouse, Log. LogServerities.Normal,"Define Hortizontal range min:%d max:%d", min, max);
            }
                break;
            case 0x08: /* Define vertical cursor range */
            { // not sure what to take instead of the CurMode (see case 0x07 as well)
              // especially the cases where sheight= 400 and we set it with the mouse_reset to 200
              // disabled it at the moment. Seems to break syndicate who want 400 in mode 13
                short max, min;
                if ((short) Register.getRegCX() < (short) Register.getRegDX()) {
                    min = (short) Register.getRegCX();
                    max = (short) Register.getRegDX();
                } else {
                    min = (short) Register.getRegDX();
                    max = (short) Register.getRegCX();
                }
                mouse.MinY = min;
                mouse.MaxY = max;
                /* Battlechess wants this */
                if (mouse.Y > mouse.MaxY)
                    mouse.Y = mouse.MaxY;
                if (mouse.Y < mouse.MinY)
                    mouse.Y = mouse.MinY;
                /*
                 * Or alternatively this: mouse.y = (mouse.max_y - mouse.min_y + 1)/2;
                 */
                Log.logging(Log. LogTypes.Mouse, Log. LogServerities.Normal,"Define Vertical range min:%d max:%d", min, max);
            }
                break;
            case 0x09: /* Define GFX Cursor */
            {
                int src = Register.segPhys(Register.SEG_NAME_ES) + Register.getRegDX();
                byte[] mask = new byte[CURSOR_Y * 2];
                Memory.blockRead(src, mask, 0, CURSOR_Y * 2);
                userDefScreenMask = ByteBuffer.wrap(mask).order(ByteOrder.LITTLE_ENDIAN)
                        .asShortBuffer().array();
                Memory.blockRead(src + CURSOR_Y * 2, mask, 0, CURSOR_Y * 2);
                userDefCursorMask = ByteBuffer.wrap(mask).order(ByteOrder.LITTLE_ENDIAN)
                        .asShortBuffer().array();
                mouse.MaskIsUserDefined = true;
                mouse.ScreenMask = userDefScreenMask;
                mouse.CursorMask = userDefCursorMask;
                mouse.HotX = (short) Register.getRegBX();
                mouse.HotY = (short) Register.getRegCX();
                mouse.CursorType = 2;
                drawCursor();
            }
            // break;
            case 0x0a: /* Define Text Cursor */
                mouse.CursorType = (short) Register.getRegBX();
                mouse.TextAndMask = (short) Register.getRegCX();
                mouse.TextXorMask = (short) Register.getRegDX();
                break;
            case 0x0b: /* Read Motion Data */
                Register.setRegCX((short) (mouse.MickeyX * mouse.MickeysPerPixelX));
                Register.setRegDX((short) (mouse.MickeyY * mouse.MickeysPerPixelY));
                mouse.MickeyX = 0;
                mouse.MickeyY = 0;
                break;
            case 0x0c: /* Define interrupt subroutine parameters */
                mouse.SubMask = (short) Register.getRegCX();
                mouse.SubSeg = (short) Register.segValue(Register.SEG_NAME_ES);
                mouse.SubOfs = (short) Register.getRegDX();
                AutoLock.run(true); // Some games don't seem to reset the mouse before using
                break;
            case 0x0f: /* Define mickey/pixel rate */
                setMickeyPixelRate((short) Register.getRegCX(), (short) Register.getRegDX());
                break;
            case 0x10: /* Define screen region for updating */
                mouse.UpdateRegionX[0] = (short) Register.getRegCX();
                mouse.UpdateRegionY[0] = (short) Register.getRegDX();
                mouse.UpdateRegionX[1] = (short) Register.getRegSI();
                mouse.UpdateRegionY[1] = (short) Register.getRegDI();
                break;
            case 0x11: /* Get number of buttons */
                Register.setRegAX(0xffff);
                Register.setRegBX(MOUSE_BUTTONS);
                break;
            case 0x13: /* Set double-speed threshold */
                mouse.DoubleSpeedThreshold =
                        (Register.getRegBX() != 0 ? (short) Register.getRegBX() : (short) 64);
                break;
            case 0x14: /* Exchange event-handler */
            {
                short oldSeg = mouse.SubSeg;
                short oldOfs = mouse.SubOfs;
                short oldMask = mouse.SubMask;
                // Set new values
                mouse.SubMask = (short) Register.getRegCX();
                mouse.SubSeg = (short) Register.segValue(Register.SEG_NAME_ES);
                mouse.SubOfs = (short) Register.getRegDX();
                // Return old values
                Register.setRegCX(oldMask);
                Register.setRegDX(oldOfs);
                Register.segSet16(Register.SEG_NAME_ES, oldSeg);
            }
                break;
            case 0x15: /* Get Driver storage space requirements */
                Register.setRegBX(mouse.size());
                break;
            case 0x16: /* Save driver state */
            {
                Log.logging(Log.LogTypes.Mouse, Log.LogServerities.Warn, "Saving driver state...");
                int dest = Register.segPhys(Register.SEG_NAME_ES) + Register.getRegDX();
                mouse.pack();
                Memory.blockWrite(dest, mouse.rawData, 0, mouse.size());
            }
                break;
            case 0x17: /* load driver state */
            {
                Log.logging(Log.LogTypes.Mouse, Log.LogServerities.Warn, "Loading driver state...");
                int src = Register.segPhys(Register.SEG_NAME_ES) + Register.getRegDX();
                Memory.blockRead(src, mouse.rawData, 0, mouse.size());
                mouse.unpack();
                mouse.ScreenMask = mouse.MaskIsUserDefined ? defaultScreenMask : userDefScreenMask;
                mouse.CursorMask = mouse.MaskIsUserDefined ? defaultCursorMask : userDefCursorMask;
            }
            break;
            case 0x1a: /* Set mouse sensitivity */
                // ToDo : double mouse speed value
                setSensitivity(Register.getRegBX(), Register.getRegCX(), Register.getRegDX());

                // LOG(LOG_MOUSE, LOG_WARN)("Set sensitivity used with %d %d (%d)",
                // regsModule.reg_bx, regsModule.reg_cx, regsModule.reg_dx);
                break;
            case 0x1b: /* Get mouse sensitivity */
                Register.setRegBX(mouse.SenvXVal);
                Register.setRegCX(mouse.SenvYVal);
                Register.setRegDX(mouse.DSpeedVal);

                // LOG(LOG_MOUSE, LOG_WARN)("Get sensitivity %d %d", regsModule.reg_bx,
                // regsModule.reg_cx);
                break;
            case 0x1c: /* Set interrupt rate */
                /* Can't really set a rate this is host determined */
                break;
            case 0x1d: /* Set display page number */
                mouse.Page = Register.getRegBL();
                break;
            case 0x1e: /* Get display page number */
                Register.setRegBX(mouse.Page);
                break;
            case 0x1f: /* Disable Mousedriver */
                /* ES:BX old mouse driver Zero at the moment TODO */
                Register.setRegBX(0);
                Register.segSet16(Register.SEG_NAME_ES, 0);
                mouse.Enabled = false; /* Just for reporting not doing a thing with it */
                mouse.OldHidden = mouse.Hidden;
                mouse.Hidden = 1;
                break;
            case 0x20: /* Enable Mousedriver */
                mouse.Enabled = true;
                mouse.Hidden = mouse.OldHidden;
                break;
            case 0x22: /* Set language for messages */
                /*
                 * Values for mouse driver language:
                 * 
                 * 00h English 01h French 02h Dutch 03h German 04h Swedish 05h Finnish 06h Spanish
                 * 07h Portugese 08h Italian
                 * 
                 */
                mouse.Language = (short) Register.getRegBX();
                break;
            case 0x23: /* Get language for messages */
                Register.setRegBX(mouse.Language);
                break;
            case 0x24: /* Get Software version and mouse type */
                Register.setRegBX(0x805); // Version 8.05 woohoo
                Register.setRegCH(0x04); /* PS/2 type */
                Register.setRegCL(0); /* PS/2 (unused) */
                break;
            case 0x26: /* Get Maximum virtual coordinates */
                Register.setRegBX((mouse.Enabled ? 0x0000 : 0xffff));
                Register.setRegCX((short) mouse.MaxX);
                Register.setRegDX((short) mouse.MaxY);
                break;
            case 0x31: /* Get Current Minimum/Maximum virtual coordinates */
                Register.setRegAX((short) mouse.MinX);
                Register.setRegBX((short) mouse.MinY);
                Register.setRegCX((short) mouse.MaxX);
                Register.setRegDX((short) mouse.MaxY);
                break;
            default:
                // LOG(LOG_MOUSE, LOG_ERROR)("Mouse Function %04X not implemented!",
                // regsModule.reg_ax);
                break;
        }
        return Callback.ReturnTypeNone;
    }

    private int bdHandler() {
        // the stack contains offsets to register values
        int raxpt = Memory.realReadW(Register.segValue(Register.SEG_NAME_SS),
                Register.getRegSP() + 0x0a);
        int rbxpt = Memory.realReadW(Register.segValue(Register.SEG_NAME_SS),
                Register.getRegSP() + 0x08);
        int rcxpt = Memory.realReadW(Register.segValue(Register.SEG_NAME_SS),
                Register.getRegSP() + 0x06);
        int rdxpt = Memory.realReadW(Register.segValue(Register.SEG_NAME_SS),
                Register.getRegSP() + 0x04);

        // read out the actual values, registers ARE overwritten
        int rax = Memory.realReadW(Register.segValue(Register.SEG_NAME_DS), raxpt);
        Register.setRegAX(rax);
        Register.setRegBX(Memory.realReadW(Register.segValue(Register.SEG_NAME_DS), rbxpt));
        Register.setRegCX(Memory.realReadW(Register.segValue(Register.SEG_NAME_DS), rcxpt));
        Register.setRegDX(Memory.realReadW(Register.segValue(Register.SEG_NAME_DS), rdxpt));
        // LOG_MSG("MOUSE BD: %04X %X %X %X %d %d",reg_ax,reg_bx,reg_cx,reg_dx,POS_X,POS_Y);

        // some functions are treated in a special way (additional registers)
        switch (rax) {
            case 0x09: /* Define GFX Cursor */
            case 0x16: /* Save driver state */
            case 0x17: /* load driver state */
                Register.segSet16(Register.SEG_NAME_ES, Register.segValue(Register.SEG_NAME_DS));
                break;
            case 0x0c: /* Define interrupt subroutine parameters */
            case 0x14: /* Exchange event-handler */
                if (Register.getRegBX() != 0)
                    Register.segSet16(Register.SEG_NAME_ES, Register.getRegBX());
                else
                    Register.segSet16(Register.SEG_NAME_ES,
                            Register.segValue(Register.SEG_NAME_DS));
                break;
            case 0x10: /* Define screen region for updating */
                Register.setRegCX(Memory.realReadW(Register.segValue(Register.SEG_NAME_DS), rdxpt));
                Register.setRegDX(
                        Memory.realReadW(Register.segValue(Register.SEG_NAME_DS), rdxpt + 2));
                Register.setRegSI(
                        Memory.realReadW(Register.segValue(Register.SEG_NAME_DS), rdxpt + 4));
                Register.setRegDI(
                        Memory.realReadW(Register.segValue(Register.SEG_NAME_DS), rdxpt + 6));
                break;
            default:
                break;
        }

        int33Handler();

        // save back the registers, too
        Memory.realWriteW(Register.segValue(Register.SEG_NAME_DS), raxpt, Register.getRegAX());
        Memory.realWriteW(Register.segValue(Register.SEG_NAME_DS), raxpt, Register.getRegBX());
        Memory.realWriteW(Register.segValue(Register.SEG_NAME_DS), raxpt, Register.getRegCX());
        Memory.realWriteW(Register.segValue(Register.SEG_NAME_DS), raxpt, Register.getRegDX());
        switch (rax) {
            case 0x1f: /* Disable Mousedriver */
                Memory.realWriteW(Register.segValue(Register.SEG_NAME_DS), rbxpt,
                        Register.segValue(Register.SEG_NAME_ES));
                break;
            case 0x14: /* Exchange event-handler */
                Memory.realWriteW(Register.segValue(Register.SEG_NAME_DS), rcxpt,
                        Register.segValue(Register.SEG_NAME_ES));
                break;
            default:
                break;
        }

        Register.setRegAX(rax);
        return Callback.ReturnTypeNone;
    }

    private int int74Handler() {
        if (mouse.Events > 0) {
            mouse.Events--;
            /* Check for an active Interrupt Handler that will get called */
            if ((mouse.SubMask & mouse.EventQueue[mouse.Events].Type) != 0) {
                Register.setRegAX(mouse.EventQueue[mouse.Events].Type);
                Register.setRegBX(mouse.EventQueue[mouse.Events].Buttons);
                Register.setRegCX((short) posX());
                Register.setRegDX((short) posY());
                Register.setRegSI((short) ((short) (mouse.MickeyX * mouse.MickeysPerPixelX)));
                Register.setRegDI((short) ((short) (mouse.MickeyY * mouse.MickeysPerPixelY)));
                CPU.push16(Memory.realSeg(Callback.realPointer(int74RetCallback)));
                CPU.push16(Memory.realOff(Callback.realPointer(int74RetCallback)));
                Register.segSet16(Register.SEG_NAME_CS, mouse.SubSeg);
                Register.setRegIP(mouse.SubOfs);
                // if (mouse.in_UIR) LOG(LOG_MOUSE, LOG_ERROR)("Already in UIR!");
                mouse.InUIR = true;
            } else if (usePS2Callback) {
                CPU.push16(Memory.realSeg(Callback.realPointer(int74RetCallback)));
                CPU.push16(Memory.realOff(Callback.realPointer(int74RetCallback)));
                doPS2Callback(mouse.EventQueue[mouse.Events].Buttons, posX(), posY());
            } else {
                Register.segSet16(Register.SEG_NAME_CS,
                        Memory.realSeg(Callback.realPointer(int74RetCallback)));
                Register.setRegIP(Memory.realOff(Callback.realPointer(int74RetCallback)));
            }
        } else {
            Register.segSet16(Register.SEG_NAME_CS,
                    Memory.realSeg(Callback.realPointer(int74RetCallback)));
            Register.setRegIP(Memory.realOff(Callback.realPointer(int74RetCallback)));
        }
        return Callback.ReturnTypeNone;
    }

    private int UserIntCBHandler() {
        mouse.InUIR = false;
        if (mouse.Events != 0) {
            if (!mouse.TimerInProgress) {
                mouse.TimerInProgress = true;
                PIC.addEvent(limitEventsWrap, MOUSE_DELAY);
            }
        }
        return Callback.ReturnTypeNone;
    }

    private EventHandler limitEventsWrap = this::limitEvents;

    private void limitEvents(int val) {
        mouse.TimerInProgress = false;
        if (mouse.Events != 0) {
            mouse.TimerInProgress = true;
            PIC.addEvent(limitEventsWrap, MOUSE_DELAY);
            PIC.activateIRQ(MOUSE_IRQ);
        }
    }

    private void resetHardware() {
        PIC.setIRQMask(MOUSE_IRQ, false);
    }

    private void setMickeyPixelRate(short px, short py) {
        if ((px != 0) && (py != 0)) {
            mouse.MickeysPerPixelX = (float) px / X_MICKEY;
            mouse.MickeysPerPixelY = (float) py / Y_MICKEY;
            mouse.PixelPerMickeyX = X_MICKEY / (float) px;
            mouse.PixelPerMickeyY = Y_MICKEY / (float) py;
        }
    }

    // private void SetSensitivity(short px, short py, short dspeed)
    private void setSensitivity(int px, int py, int dspeed) {
        if (px > 100)
            px = 100;
        if (py > 100)
            py = 100;
        if (dspeed > 100)
            dspeed = 100;
        // save values
        mouse.SenvXVal = px;
        mouse.SenvYVal = py;
        mouse.DSpeedVal = dspeed;
        if ((px != 0) && (py != 0)) {
            px--; // Inspired by cutemouse
            py--; // Although their cursor update routine is far more complex then ours
            mouse.SenvX = ((float) px * px) / 3600.0f + 1.0f / 3.0f;
            mouse.SenvY = ((float) py * py) / 3600.0f + 1.0f / 3.0f;
        }
    }


    // Does way to much. Many things should be moved to mouse reset one day
    public void newVideoMode() {
        mouse.InhibitDraw = false;
        /* Get the correct resolution from the current video mode */
        int mode = Memory.readB(BIOS.BIOS_VIDEO_MODE);
        if (mode == mouse.Mode) {
            // LOG(LOG_MOUSE,LOG_NORMAL)("New video is the same as the old"); /*return;*/
        }
        switch (mode) {
            case 0x00:
            case 0x01:
            case 0x02:
            case 0x03: {
                int rows = Memory.realReadB(INT10.BIOSMEM_SEG, INT10.BIOSMEM_NB_ROWS);
                if ((rows == 0) || (rows > 250))
                    rows = 25 - 1;
                mouse.MaxY = (short) (8 * (rows + 1) - 1);
                break;
            }
            case 0x04:
            case 0x05:
            case 0x06:
            case 0x07:
            case 0x08:
            case 0x09:
            case 0x0a:
            case 0x0d:
            case 0x0e:
            case 0x13:
                mouse.MaxY = 199;
                break;
            case 0x0f:
            case 0x10:
                mouse.MaxY = 349;
                break;
            case 0x11:
            case 0x12:
                mouse.MaxY = 479;
                break;
            default:
                // LOG(LOG_MOUSE,LOG_ERROR)("Unhandled videomode %X on reset",mode);
                mouse.InhibitDraw = true;
                return;
        }
        mouse.Mode = mode;
        mouse.Hidden = 1;
        mouse.MaxX = 639;
        mouse.MinX = 0;
        mouse.MinY = 0;
        mouse.GranMask = (mode == 0x0d || mode == 0x13) ? (short) 0xfffe : (short) 0xffff;

        mouse.Events = 0;
        mouse.TimerInProgress = false;
        PIC.removeEvents(limitEventsWrap);

        mouse.HotX = 0;
        mouse.HotY = 0;
        mouse.Background = false;
        mouse.ScreenMask = defaultScreenMask;
        mouse.CursorMask = defaultCursorMask;
        mouse.TextAndMask = defaultTextAndMask;
        mouse.TextXorMask = defaultTextXorMask;
        mouse.Language = 0;
        mouse.Page = 0;
        mouse.DoubleSpeedThreshold = 64;
        mouse.UpdateRegionX[0] = 1;
        mouse.UpdateRegionY[0] = 1;
        mouse.UpdateRegionX[1] = 1;
        mouse.UpdateRegionY[1] = 1;
        mouse.CursorType = 0;
        mouse.Enabled = true;
        mouse.OldHidden = 1;

        oldMouseX = (short) mouse.X;
        oldMouseY = (short) mouse.Y;


    }

    // ***************************************************************************
    // Mouse cursor - text mode
    // ***************************************************************************
    /* Write and read directly to the screen. Do no use int_setcursorpos (LOTUS123) */
    private void restoreCursorBackgroundText() {
        if (mouse.Hidden != 0 || mouse.InhibitDraw)
            return;

        if (mouse.Background) {
            // CHAR.WriteChar((ushort)mouse.BackPosX, (ushort)mouse.BackPosY,
            // Memory.RealReadB(INT10.BIOSMEM_SEG, INT10.BIOSMEM_CURRENT_PAGE), mouse.BackData[0],
            // mouse.BackData[1], true);
            CHAR.writeChar1(0xffff & mouse.BackPosX, 0xffff & mouse.BackPosY,
                    Memory.realReadB(INT10.BIOSMEM_SEG, INT10.BIOSMEM_CURRENT_PAGE),
                    0xff & mouse.BackData[0], 0xff & mouse.BackData[1], true);
            mouse.Background = false;
        }
    }

    private void drawCursorText() {
        // Restore Background
        restoreCursorBackgroundText();


        // Save Background
        mouse.BackPosX = (short) (posX() >>> 3);
        mouse.BackPosY = (short) (posY() >>> 3);

        // use current page (CV program)
        int page = Memory.realReadB(INT10.BIOSMEM_SEG, INT10.BIOSMEM_CURRENT_PAGE);
        int result = CHAR.readCharAttr(0xffff & mouse.BackPosX, 0xffff & mouse.BackPosY, page);
        mouse.BackData[0] = (byte) (result & 0xFF);
        mouse.BackData[1] = (byte) (result >>> 8);
        mouse.Background = true;
        // Write Cursor
        result = 0xffff & ((result & mouse.TextAndMask) ^ mouse.TextXorMask);
        CHAR.writeChar1(0xffff & mouse.BackPosX, 0xffff & mouse.BackPosY, page, result & 0xFF,
                0xff & (result >>> 8), true);
    }


    // ***************************************************************************
    // Mouse cursor - graphic mode
    // ***************************************************************************

    private byte[] gfxReg3CE = new byte[9];
    int index3C4, gfxReg3C5;// uint8

    private void saveVgaRegisters() {
        if (DOSBox.isVGAArch()) {
            for (byte i = 0; i < 9; i++) {
                IO.write(0x3CE, i);
                gfxReg3CE[i] = (byte) IO.read(0x3CF);
            }
            /* Setup some default values in GFX regs that should work */
            IO.write(0x3CE, 3);
            IO.write(0x3Cf, 0); // disable rotate and operation
            IO.write(0x3CE, 5);
            IO.write(0x3Cf, gfxReg3CE[5] & 0xf0); // Force read/write mode 0

            // Set Map to all planes. Celtic Tales
            index3C4 = IO.read(0x3c4);
            IO.write(0x3C4, 2);
            gfxReg3C5 = IO.read(0x3C5);
            IO.write(0x3C5, 0xF);
        } else if (DOSBox.Machine == DOSBox.MachineType.EGA) {
            // Set Map to all planes.
            IO.write(0x3C4, 2);
            IO.write(0x3C5, 0xF);
        }
    }

    private void restoreCursorBackground() {
        if (mouse.Hidden != 0 || mouse.InhibitDraw)
            return;

        saveVgaRegisters();
        if (mouse.Background) {
            // Restore background
            short x, y;
            int addX1 = 0, addX2 = 0, addY = 0;// uint16
            int dataPos = 0;// uint16
            short x1 = mouse.BackPosX;
            short y1 = mouse.BackPosY;
            short x2 = (short) (x1 + CURSOR_X - 1);
            short y2 = (short) (y1 + CURSOR_Y - 1);

            clipCursorArea(x1, x2, y1, y2, addX1, addX2, addY);
            x1 = returnedClipCursorAreaX1;
            x2 = returnedClipCursorAreaX2;
            y1 = returnedClipCursorAreaY1;
            y2 = returnedClipCursorAreaY2;
            addX1 = returnedClipCursorAreaAddX1;
            addX2 = returnedClipCursorAreaAddX2;
            addY = returnedClipCursorAreaAddY;

            dataPos = 0xffff & (addY * CURSOR_X);
            for (y = y1; y <= y2; y++) {
                dataPos += addX1;
                for (x = x1; x <= x2; x++) {
                    INT10.putPixel(0xffff & x, 0xffff & y, mouse.Page, mouse.BackData[dataPos++]);
                }
                dataPos += addX2;
            }
            mouse.Background = false;
        }
        restoreVGARegisters();
    }


    private void restoreVGARegisters() {
        if (DOSBox.isVGAArch()) {
            for (byte i = 0; i < 9; i++) {
                IO.write(0x3CE, i);
                IO.write(0x3CF, gfxReg3CE[i]);
            }

            IO.write(0x3C4, 2);
            IO.write(0x3C5, gfxReg3C5);
            IO.write(0x3C4, index3C4);
        }
    }

    private short returnedClipCursorAreaX1, returnedClipCursorAreaX2, returnedClipCursorAreaY1,
            returnedClipCursorAreaY2;// int16

    private int returnedClipCursorAreaAddX1, returnedClipCursorAreaAddX2,
            returnedClipCursorAreaAddY;// uint16
    // (ref Int16 x1, ref Int16 x2, ref Int16 y1, ref Int16 y2, ref UInt16 addx1, ref UInt16 addx2,
    // ref UInt16 addy)

    private void clipCursorArea(short x1, short x2, short y1, short y2, int addx1, int addx2,
            int addy) {
        addx1 = addx2 = addy = 0;
        // Clip up
        if (y1 < 0) {
            addy += 0xffff & -y1;
            y1 = 0;
        }
        // Clip down
        if (y2 > mouse.ClipY) {
            y2 = mouse.ClipY;
        }
        // Clip left
        if (x1 < 0) {
            addx1 += 0xffff & -x1;
            x1 = 0;
        }
        // Clip right
        if (x2 > mouse.ClipX) {
            addx2 = 0xffff & (x2 - mouse.ClipX);
            x2 = mouse.ClipX;
        }
        returnedClipCursorAreaX1 = x1;
        returnedClipCursorAreaX2 = x2;
        returnedClipCursorAreaY1 = y1;
        returnedClipCursorAreaY2 = y2;
        returnedClipCursorAreaAddX1 = addx1;
        returnedClipCursorAreaAddX2 = addx2;
        returnedClipCursorAreaAddY = addy;
    }

    private void drawCursor() {
        if (mouse.Hidden != 0 || mouse.InhibitDraw)
            return;
        // In Textmode ?
        if (INT10Mode.CurMode.Type == VGAModes.TEXT) {
            drawCursorText();
            return;
        }

        // Check video page. Seems to be ignored for text mode.
        // hence the text mode handled above this
        if (Memory.realReadB(INT10.BIOSMEM_SEG, INT10.BIOSMEM_CURRENT_PAGE) != mouse.Page)
            return;
        // Check if cursor in update region
        /*
         * if ((POS_X >= mouse.updateRegion_x[0]) && (POS_X <= mouse.updateRegion_x[1]) && (POS_Y >=
         * mouse.updateRegion_y[0]) && (POS_Y <= mouse.updateRegion_y[1])) { if
         * (CurMode->type==M_TEXT16) RestoreCursorBackgroundText(); else RestoreCursorBackground();
         * mouse.shown--; return; }
         */
        /* Not sure yet what to do update region should be set to ??? */

        // Get Clipping ranges


        mouse.ClipX = (short) ((int) INT10Mode.CurMode.SWidth - 1); /* Get from bios ? */
        mouse.ClipY = (short) ((int) INT10Mode.CurMode.SHeight - 1);

        /* might be vidmode == 0x13?2:1 */
        short xratio = 640;
        if (INT10Mode.CurMode.SWidth > 0)
            xratio = (short) (xratio / INT10Mode.CurMode.SWidth);
        if (xratio == 0)
            xratio = 1;

        restoreCursorBackground();

        saveVgaRegisters();

        // Save Background
        short x, y;
        int addX1 = 0, addX2 = 0, addY = 0;// uint16
        int dataPos = 0;// uint16
        short x1 = (short) (posX() / xratio - mouse.HotX);
        short y1 = (short) (posY() - mouse.HotY);
        short x2 = (short) (x1 + CURSOR_X - 1);
        short y2 = (short) (y1 + CURSOR_Y - 1);

        clipCursorArea(x1, x2, y1, y2, addX1, addX2, addY);
        x1 = returnedClipCursorAreaX1;
        x2 = returnedClipCursorAreaX2;
        y1 = returnedClipCursorAreaY1;
        y2 = returnedClipCursorAreaY2;
        addX1 = returnedClipCursorAreaAddX1;
        addX2 = returnedClipCursorAreaAddX2;
        addY = returnedClipCursorAreaAddY;

        dataPos = 0xffff & (addY * CURSOR_X);
        if (INT10.canGetPixel()) {
            for (y = y1; y <= y2; y++) {
                dataPos += addX1;
                for (x = x1; x <= x2; x++) {
                    mouse.BackData[dataPos++] = INT10.getPixel(x, y, mouse.Page);
                }
                dataPos += addX2;
            }
        }
        mouse.Background = true;
        mouse.BackPosX = (short) (posX() / xratio - mouse.HotX);
        mouse.BackPosY = (short) (posY() - mouse.HotY);

        // Draw Mousecursor
        dataPos = 0xffff & (addY * CURSOR_X);
        for (y = y1; y <= y2; y++) {
            int scMask = 0xffff & mouse.ScreenMask[addY + y - y1];// uint16
            int cuMask = 0xffff & mouse.CursorMask[addY + y - y1];// uint16
            if (addX1 > 0) {
                scMask <<= addX1;
                cuMask <<= addX1;
                dataPos += addX1;
            }
            for (x = x1; x <= x2; x++) {
                byte pixel = 0;
                // ScreenMask
                if ((scMask & HIGHESTBIT) != 0)
                    pixel = mouse.BackData[dataPos];
                scMask <<= 1;
                // CursorMask
                if ((cuMask & HIGHESTBIT) != 0)
                    pixel = (byte) (pixel ^ 0x0F);
                cuMask <<= 1;
                // Set Pixel
                INT10.putPixel(0xffff & x, 0xffff & y, mouse.Page, pixel);
                dataPos++;
            }
            dataPos += addX2;
        }
        restoreVGARegisters();
    }

    public void cursorMoved(float xRel, float yRel, float xRatio, float yRatio, float xAbs,
            float yAbs, boolean emulate) {
        float dx = xRel * mouse.PixelPerMickeyX;
        float dy = yRel * mouse.PixelPerMickeyY;

        if ((Math.abs(xRel) > 1.0) || (mouse.SenvX < 1.0))
            dx *= mouse.SenvX;
        if ((Math.abs(yRel) > 1.0) || (mouse.SenvY < 1.0))
            dy *= mouse.SenvY;
        if (usePS2Callback)
            dy *= 2;

        mouse.MickeyX += dx;
        mouse.MickeyY += dy;
        if (emulate) {
            mouse.X += dx;
            mouse.Y += dy;
        } else {
            if (INT10Mode.CurMode.Type == VGAModes.TEXT) {
                mouse.X = xRatio * INT10Mode.CurMode.SWidth;
                mouse.Y = yRatio * INT10Mode.CurMode.SHeight * 8 / INT10Mode.CurMode.CHeight;
            } else if ((mouse.MaxX < 2048) || (mouse.MaxY < 2048) || (mouse.MaxX != mouse.MaxY)) {
                if ((mouse.MaxX > 0) && (mouse.MaxY > 0)) {
                    // mouse.x = xRatio * mouse.max_x;
                    // mouse.y = yRatio * mouse.max_y;
                    mouse.X = xAbs;
                    mouse.Y = yAbs;
                } else {
                    mouse.X += xRel;
                    mouse.Y += yRel;
                }
            } else { // Games faking relative movement through absolute coordinates. Quite
                     // surprising that this actually works..
                mouse.X += xRel;
                mouse.Y += yRel;
            }
        }

        /* ignore constraints if using PS2 mouse callback in the bios */

        if (!usePS2Callback) {
            if (mouse.X > mouse.MaxX)
                mouse.X = mouse.MaxX;
            if (mouse.X < mouse.MinX)
                mouse.X = mouse.MinX;
            if (mouse.Y > mouse.MaxY)
                mouse.Y = mouse.MaxY;
            if (mouse.Y < mouse.MinY)
                mouse.Y = mouse.MinY;
        }
        addEvent(MOUSE_HAS_MOVED);
        drawCursor();
    }


    private void setCursor(float x, float y) {
        mouse.X = x;
        mouse.Y = y;
        drawCursor();
    }

    public void buttonPressed(int button) {
        switch (button) {
            case 0:
                mouse.Buttons |= 1;
                addEvent(MOUSE_LEFT_PRESSED);
                break;
            case 1:
                mouse.Buttons |= 2;
                addEvent(MOUSE_RIGHT_PRESSED);
                break;
            case 2:
                mouse.Buttons |= 4;
                addEvent(MOUSE_MIDDLE_PRESSED);
                break;
            default:
                return;
        }
        mouse.TimesPressed[button]++;
        mouse.LastPressedX[button] = (short) posX();
        mouse.LastPressedY[button] = (short) posY();
    }

    public void buttonReleased(int button) {
        switch (button) {
            case 0:
                mouse.Buttons &= (byte) ~1;
                addEvent(MOUSE_LEFT_RELEASED);
                break;
            case 1:
                mouse.Buttons &= (byte) ~2;
                addEvent(MOUSE_RIGHT_RELEASED);
                break;
            case 2:
                mouse.Buttons &= (byte) ~4;
                addEvent(MOUSE_MIDDLE_RELEASED);
                break;
            default:
                return;
        }
        mouse.TimesReleased[button]++;
        mouse.LastReleasedX[button] = (short) posX();
        mouse.LastReleasedY[button] = (short) posY();
    }


    // Much too empty, Mouse_NewVideoMode contains stuff that should be in here
    private void reset() {
        /* Remove drawn mouse Legends of Valor */
        if (INT10Mode.CurMode.Type != VGAModes.TEXT)
            restoreCursorBackground();
        else
            restoreCursorBackgroundText();
        mouse.Hidden = 1;

        newVideoMode();
        setMickeyPixelRate((short) 8, (short) 16);

        mouse.MickeyX = 0;
        mouse.MickeyY = 0;

        // Dont set max coordinates here. it is done by SetResolution!
        mouse.X = (float) ((mouse.MaxX + 1) / 2);
        mouse.Y = (float) ((mouse.MaxY + 1) / 2);
        mouse.SubMask = 0;
        mouse.InUIR = false;
    }

    // Does way to much. Many things should be moved to mouse reset one day

    public void init(Section sec) {
        // Callback for mouse interrupt 0x33
        callINT33 = Callback.allocate();
        // RealPt i33loc=RealMake(CB_SEG+1,(call_int33*CB_SIZE)-0x10);
        int i33Loc = Memory.realMake(DOSMain.getMemory(0x1) - 1, 0x10);
        Callback.setup(callINT33, this::int33Handler, Callback.Symbol.MOUSE,
                Memory.real2Phys(i33Loc), "Mouse");
        // Wasteland needs low(seg(int33))!=0 and low(ofs(int33))!=0
        Memory.realWriteD(0, 0x33 << 2, i33Loc);

        callMouseBD = Callback.allocate();
        Callback.setup(callMouseBD, this::bdHandler, Callback.Symbol.RETF8, Memory.physMake(
                0xffff & Memory.realSeg(i33Loc), 0xffff & (Memory.realOff(i33Loc) + 2)), "MouseBD");
        // pseudocode for CB_MOUSE (including the special backdoor entry point):
        // jump near i33hd
        // callback MOUSE_BD_Handler
        // retf 8
        // label i33hd:
        // callback INT33_Handler
        // iret


        // Callback for ps2 irq
        callINT74 = Callback.allocate();
        Callback.setup(callINT74, this::int74Handler, Callback.Symbol.IRQ12, "int 74");
        // pseudocode for CB_IRQ12:
        // push ds
        // push es
        // pushad
        // sti
        // callback INT74_Handler
        // doesn't return here, but rather to CB_IRQ12_RET
        // (ps2 callback/user callback inbetween if requested)

        int74RetCallback = Callback.allocate();
        Callback.setup(int74RetCallback, this::UserIntCBHandler, Callback.Symbol.IRQ12_RET,
                "int 74 ret");
        // pseudocode for CB_IRQ12_RET:
        // callback MOUSE_UserInt_CB_Handler
        // cli
        // mov al, 0x20
        // out 0xa0, al
        // out 0x20, al
        // popad
        // pop es
        // pop ds
        // iret

        int hwVec = 0xff & ((MOUSE_IRQ > 7) ? (0x70 + MOUSE_IRQ - 8) : (0x8 + MOUSE_IRQ));
        Memory.realSetVec(hwVec, Callback.realPointer(callINT74));

        // Callback for ps2 user callback handling
        usePS2Callback = false;
        ps2CallbackInit = false;
        callPS2 = Callback.allocate();
        Callback.setup(callPS2, this::ps2Handler, Callback.Symbol.RETF, "ps2 bios callback");
        ps2Callback = Callback.realPointer(callPS2);

        // memset(&mouse,0,sizeof(mouse));
        mouse = new MouseStruct();

        mouse.Hidden = 1; // Hide mouse on startup
        mouse.TimerInProgress = false;
        mouse.Mode = 0xFF; // Non existing mode

        mouse.SubMask = 0;
        mouse.SubSeg = 0x6362; // magic value
        mouse.SubOfs = 0;

        resetHardware();
        reset();
        setSensitivity(50, 50, 50);
    }

}
