package org.gutkyu.dosboxj.interrupt;

import org.gutkyu.dosboxj.misc.setup.*;
import org.gutkyu.dosboxj.cpu.*;
import org.gutkyu.dosboxj.util.*;
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
    private int _callINT33, _callINT74, _int74RetCallback, _callMouseBD;
    private int _ps2CbSeg, _ps2CbOfs;
    private boolean _usePs2Callback, _ps2CallbackInit;
    private int _callPS2;
    private int _ps2Callback;
    private short _oldMouseX, _oldMouseY;

    private static final int QUEUE_SIZE = 32;
    private static final int MOUSE_BUTTONS = 3;
    private static final int MOUSE_IRQ = 12;

    private short posX() {
        return (short) (((short) (mouse.X)) & mouse.GranMask);
    }

    private short posY() {
        return (short) (mouse.Y);
    }

    private static final int CURSORX = 16;
    private static final int CURSORY = 16;
    private static final int HIGHESTBIT = (1 << (CURSORX - 1));

    private Mouse() {
        mouse = new MouseStruct();
    }

    private static Mouse _mouseObj = null;

    public static Mouse instance() {
        return _mouseObj;
    }

    DOSActionBool AutoLock;

    public static class AutoLockBinder {
        public AutoLockBinder(MouseAutoLockable autoLockProvider) {
            if (Mouse._mouseObj == null)
                _mouseObj = new Mouse();
            _mouseObj.AutoLock = autoLockProvider::MouseAutoLock;
        }
    }

    public boolean setPS2State(boolean use) {
        if (use && (!_ps2CallbackInit)) {
            _usePs2Callback = false;
            PIC.setIRQMask(MOUSE_IRQ, true);
            return false;
        }
        _usePs2Callback = use;
        AutoLock.run(_usePs2Callback);
        PIC.setIRQMask(MOUSE_IRQ, !_usePs2Callback);
        return true;
    }

    // public void ChangePS2Callback(short pseg, short pofs) {
    public void changePS2Callback(int pseg, int pofs) {
        if ((pseg == 0) && (pofs == 0)) {
            _ps2CallbackInit = false;
            AutoLock.run(false);
        } else {
            _ps2CallbackInit = true;
            _ps2CbSeg = 0xffff & pseg;
            _ps2CbOfs = 0xffff & pofs;
        }
        AutoLock.run(_ps2CallbackInit);
    }


    private void doPS2Callback(short data, short mouseX, short mouseY) {
        if (_usePs2Callback) {
            short mDat = (short) ((data & 0x03) | 0x08);
            short xDiff = (short) (mouseX - _oldMouseX);
            short yDiff = (short) (_oldMouseY - mouseY);
            _oldMouseX = mouseX;
            _oldMouseY = mouseY;
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
            CPU.push16(Memory.realSeg(_ps2Callback));
            CPU.push16(Memory.realOff(_ps2Callback));
            Register.segSet16(Register.SEG_NAME_CS, _ps2CbSeg);
            Register.setRegIP(_ps2CbOfs);
        }
    }

    private int PS2Handler() {
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
            mouse.EventQueue[0].Buttons = mouse.Buttons;
            mouse.Events++;
        }
        if (!mouse.TimerInProgress) {
            mouse.TimerInProgress = true;
            PIC.addEvent(limitEventsWrap, MOUSE_DELAY);
            PIC.activateIRQ(MOUSE_IRQ);
        }
    }

    private short _defaultTextAndMask = 0x77FF;
    private short _defaultTextXorMask = 0x7700;

    private short[] _defaultScreenMask =
            {0x3FFF, 0x1FFF, 0x0FFF, 0x07FF, 0x03FF, 0x01FF, 0x00FF, 0x007F, 0x003F, 0x001F, 0x01FF,
                    0x00FF, 0x30FF, (short) 0xF87F, (short) 0xF87F, (short) 0xFCFF};

    private short[] _defaultCursorMask = {0x0000, 0x4000, 0x6000, 0x7000, 0x7800, 0x7C00, 0x7E00,
            0x7F00, 0x7F80, 0x7C00, 0x6C00, 0x4600, 0x0600, 0x0300, 0x0300, 0x0000};


    // -- #region mouse
    private static class ButtonEvent {
        public int Type;// uint8
        public byte Buttons;// uint8
    }

    private short[] _userdefScreenMask = new short[CURSORY];
    private short[] _userdefCursorMask = new short[CURSORY];

    private class MouseStruct implements ByteSequence {
        //
        public byte Buttons; // 1
        public short[] TimesPressed; // 6
        public short[] TimesReleased; // 6

        public short[] LastReleasedX; // 6
        public short[] LastReleasedy; // 6
        public short[] LastPressedX; // 6
        public short[] LastPressedY; // 6
        public short Hidden; // 2
        public float addX, addY; // 4,4
        public short MinX, MaxX, MinY, MaxY; // 2,2,2,2
        public float MickeyX, MickeyY; // 4,4
        public float X, Y; // 4,4
        public ButtonEvent[] EventQueue; // 64
        public byte Events;// Increase if QUEUE_SIZE >255 (currently 32) // 1
        public short SubSeg, SubOfs; // 2,2
        public short SubMask; // 2
        //
        public boolean Background; // 1
        public short BackPosX, BackPosY; // 2,2
        public byte[] BackData; // 256
        // screenMask, cursorMask가 가리키는 값은 객체 내부에 이미 저장되어 있으므로 어떤값을 사용할건지만 정의하면됨
        // 예를 들면, blockwrite시 screenMask를 사용할 수 없기 때문에 screenMask 포인터와 같은 크기의 자료형 uint로
        // screenMaskIsDefault를 정의한 다음 default data를 사용할지 user define data를 사용할 지를 나타냄
        // blockread시 이 screenMaskIsDefault값을 읽어 screenMask을 할당할 것
        // uint
        public int ScreenMaskIsDefault; // 자료가 default mask를 사용(값이 0)하는지 user define mask를 사용( 0이 아닌
                                        // 값)하는지 확인하는 값
        public short[] ScreenMask;// 초기화 필요없음 // 소스는 포인터 변수임 4
        // uint
        public int CursorMaskIsDefault; // 자료가 default mask를 사용(값이 0)하는지 user define mask를 사용( 0이 아닌
                                        // 값)하는지 확인하는 값
        public short[] CursorMask; // 소스는 포인터 변수임 4
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
        public byte Mode; // 1
        public short GranMask; // 2

        private static final int MASK_B0 = 0xff;
        private static final int SHIFT_B1 = 8;
        private static final int SHIFT_B2 = 16;
        private static final int SHIFT_B3 = 24;


        private int pos = 0;

        public void goFirst() {
            pos = 0;
        }

        // uint8()
        public int next() throws DOSException {
            return get(pos++);
        }

        public boolean hasNext() {
            return pos < MOUSE_DATA_SIZE;
        }

        private int tmpF2I = 0;

        // uint8(int)
        private int get(int index) throws DOSException {
            switch (index) {
                case 0:
                    return MASK_B0 & Buttons;
                case 1:
                    return MASK_B0 & TimesPressed[0];
                case 2:
                    return MASK_B0 & (TimesPressed[0] >>> SHIFT_B1);
                case 3:
                    return MASK_B0 & TimesPressed[1];
                case 4:
                    return MASK_B0 & (TimesPressed[1] >>> SHIFT_B1);
                case 5:
                    return MASK_B0 & TimesPressed[2];
                case 6:
                    return MASK_B0 & (TimesPressed[2] >>> SHIFT_B1);
                case 7:
                    return MASK_B0 & TimesReleased[0];
                case 8:
                    return MASK_B0 & (TimesReleased[0] >>> SHIFT_B1);
                case 9:
                    return MASK_B0 & TimesReleased[1];
                case 10:
                    return MASK_B0 & (TimesReleased[1] >>> SHIFT_B1);
                case 11:
                    return MASK_B0 & TimesReleased[2];
                case 12:
                    return MASK_B0 & (TimesReleased[2] >>> SHIFT_B1);
                case 13:
                    return MASK_B0 & LastReleasedX[0];
                case 14:
                    return MASK_B0 & (LastReleasedX[0] >>> SHIFT_B1);
                case 15:
                    return MASK_B0 & LastReleasedX[1];
                case 16:
                    return MASK_B0 & (LastReleasedX[1] >>> SHIFT_B1);
                case 17:
                    return MASK_B0 & LastReleasedX[2];
                case 18:
                    return MASK_B0 & (LastReleasedX[2] >>> SHIFT_B1);
                case 19:
                    return MASK_B0 & LastReleasedy[0];
                case 20:
                    return MASK_B0 & (LastReleasedy[0] >>> SHIFT_B1);
                case 21:
                    return MASK_B0 & LastReleasedy[1];
                case 22:
                    return MASK_B0 & (LastReleasedy[1] >>> SHIFT_B1);
                case 23:
                    return MASK_B0 & LastReleasedy[2];
                case 24:
                    return MASK_B0 & (LastReleasedy[2] >>> SHIFT_B1);
                case 25:
                    return MASK_B0 & LastPressedX[0];
                case 26:
                    return MASK_B0 & (LastPressedX[0] >>> SHIFT_B1);
                case 27:
                    return MASK_B0 & LastPressedX[1];
                case 28:
                    return MASK_B0 & (LastPressedX[1] >>> SHIFT_B1);
                case 29:
                    return MASK_B0 & LastPressedX[2];
                case 30:
                    return MASK_B0 & (LastPressedX[2] >>> SHIFT_B1);
                case 31:
                    return MASK_B0 & LastPressedY[0];
                case 32:
                    return MASK_B0 & (LastPressedY[0] >>> SHIFT_B1);
                case 33:
                    return MASK_B0 & LastPressedY[1];
                case 34:
                    return MASK_B0 & (LastPressedY[1] >>> SHIFT_B1);
                case 35:
                    return MASK_B0 & LastPressedY[2];
                case 36:
                    return MASK_B0 & (LastPressedY[2] >>> SHIFT_B1);
                case 37:
                    return MASK_B0 & Hidden;
                case 38:
                    return MASK_B0 & (Hidden >>> SHIFT_B1);
                case 39:
                    tmpF2I = Float.floatToIntBits(addX);
                    return MASK_B0 & tmpF2I;
                case 40:
                    return MASK_B0 & (tmpF2I >>> SHIFT_B1);
                case 41:
                    return MASK_B0 & (tmpF2I >>> SHIFT_B2);
                case 42:
                    return MASK_B0 & (tmpF2I >>> SHIFT_B3);
                case 43:
                    tmpF2I = Float.floatToIntBits(addY);
                    return MASK_B0 & tmpF2I;
                case 44:
                    return MASK_B0 & (tmpF2I >>> SHIFT_B1);
                case 45:
                    return MASK_B0 & (tmpF2I >>> SHIFT_B2);
                case 46:
                    return MASK_B0 & (tmpF2I >>> SHIFT_B3);
                case 47:
                    return MASK_B0 & MinX;
                case 48:
                    return MASK_B0 & (MinX >>> SHIFT_B1);
                case 49:
                    return MASK_B0 & MaxX;
                case 50:
                    return MASK_B0 & (MaxX >>> SHIFT_B1);
                case 51:
                    return MASK_B0 & MinY;
                case 52:
                    return MASK_B0 & (MinY >>> SHIFT_B1);
                case 53:
                    return MASK_B0 & MaxY;
                case 54:
                    return MASK_B0 & (MaxY >>> SHIFT_B1);
                case 55:
                    tmpF2I = Float.floatToIntBits(MickeyX);
                    return MASK_B0 & tmpF2I;
                case 56:
                    return MASK_B0 & (tmpF2I >>> SHIFT_B1);
                case 57:
                    return MASK_B0 & (tmpF2I >>> SHIFT_B2);
                case 58:
                    return MASK_B0 & (tmpF2I >>> SHIFT_B3);
                case 59:
                    tmpF2I = Float.floatToIntBits(MickeyY);
                    return MASK_B0 & tmpF2I;
                case 60:
                    return MASK_B0 & (tmpF2I >>> SHIFT_B1);
                case 61:
                    return MASK_B0 & (tmpF2I >>> SHIFT_B2);
                case 62:
                    return MASK_B0 & (tmpF2I >>> SHIFT_B3);
                case 63:
                    tmpF2I = Float.floatToIntBits(X);
                    return MASK_B0 & tmpF2I;
                case 64:
                    return MASK_B0 & (tmpF2I >>> SHIFT_B1);
                case 65:
                    return MASK_B0 & (tmpF2I >>> SHIFT_B2);
                case 66:
                    return MASK_B0 & (tmpF2I >>> SHIFT_B3);
                case 67:
                    tmpF2I = Float.floatToIntBits(Y);
                    return MASK_B0 & tmpF2I;
                case 68:
                    return MASK_B0 & (tmpF2I >>> SHIFT_B1);
                case 69:
                    return MASK_B0 & (tmpF2I >>> SHIFT_B2);
                case 70:
                    return MASK_B0 & (tmpF2I >>> SHIFT_B3);
                case 135:
                    return Events;
                case 136:
                    return MASK_B0 & SubSeg;
                case 137:
                    return MASK_B0 & (SubSeg >>> SHIFT_B1);
                case 138:
                    return MASK_B0 & SubOfs;
                case 139:
                    return MASK_B0 & (SubOfs >>> SHIFT_B1);
                case 140:
                    return MASK_B0 & SubMask;
                case 141:
                    return MASK_B0 & (SubMask >>> SHIFT_B1);
                case 142:
                    return MASK_B0 & (Background ? 1 : 0);
                case 143:
                    return MASK_B0 & BackPosX;
                case 144:
                    return MASK_B0 & (BackPosX >>> SHIFT_B1);
                case 145:
                    return MASK_B0 & BackPosY;
                case 146:
                    return MASK_B0 & (BackPosY >>> SHIFT_B1);
                // screenMask,cursorMask가 원 소스에서 포인터 변수이므로 길이를 4바이트로 정의하고
                // 가리키는 값은 별도의 메모리에 저장되어 있기 때문에 값을 저장할 필요없음
                // default data 사용 유무만 확인
                //
                // screenMask, cursorMask가 가리키는 값은 객체 내부에 이미 저장되어 있으므로 어떤값을 사용할건지만 정의하면됨
                case 403:
                    return MASK_B0 & ScreenMaskIsDefault;
                case 404:
                    return MASK_B0 & (ScreenMaskIsDefault >>> SHIFT_B1);
                case 405:
                    return MASK_B0 & (ScreenMaskIsDefault >>> SHIFT_B2);
                case 406:
                    return MASK_B0 & (ScreenMaskIsDefault >>> SHIFT_B3);
                case 407:
                    return MASK_B0 & CursorMaskIsDefault;
                case 408:
                    return MASK_B0 & (CursorMaskIsDefault >>> SHIFT_B1);
                case 409:
                    return MASK_B0 & (CursorMaskIsDefault >>> SHIFT_B2);
                case 410:
                    return MASK_B0 & (CursorMaskIsDefault >>> SHIFT_B3);
                case 411:
                    return MASK_B0 & ClipX;
                case 412:
                    return MASK_B0 & (ClipX >>> SHIFT_B1);
                case 413:
                    return MASK_B0 & ClipY;
                case 414:
                    return MASK_B0 & (ClipY >>> SHIFT_B1);
                case 415:
                    return MASK_B0 & HotX;
                case 416:
                    return MASK_B0 & (HotX >>> SHIFT_B1);
                case 417:
                    return MASK_B0 & HotY;
                case 418:
                    return MASK_B0 & (HotY >>> SHIFT_B1);
                case 419:
                    return MASK_B0 & TextAndMask;
                case 420:
                    return MASK_B0 & (TextAndMask >>> SHIFT_B1);
                case 421:
                    return MASK_B0 & TextXorMask;
                case 422:
                    return MASK_B0 & (TextXorMask >>> SHIFT_B1);
                case 423:
                    tmpF2I = Float.floatToIntBits(MickeysPerPixelX);
                    return MASK_B0 & tmpF2I;
                case 424:
                    return MASK_B0 & (tmpF2I >>> SHIFT_B1);
                case 425:
                    return MASK_B0 & (tmpF2I >>> SHIFT_B2);
                case 426:
                    return MASK_B0 & (tmpF2I >>> SHIFT_B3);
                case 427:
                    tmpF2I = Float.floatToIntBits(MickeysPerPixelY);
                    return MASK_B0 & tmpF2I;
                case 428:
                    return MASK_B0 & (tmpF2I >>> SHIFT_B1);
                case 429:
                    return MASK_B0 & (tmpF2I >>> SHIFT_B2);
                case 430:
                    return MASK_B0 & (tmpF2I >>> SHIFT_B3);
                case 431:
                    tmpF2I = Float.floatToIntBits(PixelPerMickeyX);
                    return MASK_B0 & tmpF2I;
                case 432:
                    return MASK_B0 & (tmpF2I >>> SHIFT_B1);
                case 433:
                    return MASK_B0 & (tmpF2I >>> SHIFT_B2);
                case 434:
                    return MASK_B0 & (tmpF2I >>> SHIFT_B3);
                case 435:
                    tmpF2I = Float.floatToIntBits(PixelPerMickeyY);
                    return MASK_B0 & tmpF2I;
                case 436:
                    return MASK_B0 & (tmpF2I >>> SHIFT_B1);
                case 437:
                    return MASK_B0 & (tmpF2I >>> SHIFT_B2);
                case 438:
                    return MASK_B0 & (tmpF2I >>> SHIFT_B3);
                case 439:
                    return MASK_B0 & SenvXVal;
                case 440:
                    return MASK_B0 & (SenvXVal >>> SHIFT_B1);
                case 441:
                    return MASK_B0 & SenvYVal;
                case 442:
                    return MASK_B0 & (SenvYVal >>> SHIFT_B1);
                case 443:
                    return MASK_B0 & DSpeedVal;
                case 444:
                    return MASK_B0 & (DSpeedVal >>> SHIFT_B1);
                case 445:
                    tmpF2I = Float.floatToIntBits(SenvX);
                    return MASK_B0 & tmpF2I;
                case 446:
                    return MASK_B0 & (tmpF2I >>> SHIFT_B1);
                case 447:
                    return MASK_B0 & (tmpF2I >>> SHIFT_B2);
                case 448:
                    return MASK_B0 & (tmpF2I >>> SHIFT_B3);
                case 449:
                    tmpF2I = Float.floatToIntBits(SenvY);
                    return MASK_B0 & tmpF2I;
                case 450:
                    return MASK_B0 & (tmpF2I >>> SHIFT_B1);
                case 451:
                    return MASK_B0 & (tmpF2I >>> SHIFT_B2);
                case 452:
                    return MASK_B0 & (tmpF2I >>> SHIFT_B3);
                case 453:
                    return MASK_B0 & UpdateRegionX[0];
                case 454:
                    return MASK_B0 & (UpdateRegionX[0] >>> SHIFT_B1);
                case 455:
                    return MASK_B0 & UpdateRegionX[1];
                case 456:
                    return MASK_B0 & (UpdateRegionX[1] >>> SHIFT_B1);
                case 457:
                    return MASK_B0 & UpdateRegionY[0];
                case 458:
                    return MASK_B0 & (UpdateRegionY[0] >>> SHIFT_B1);
                case 459:
                    return MASK_B0 & UpdateRegionY[1];
                case 460:
                    return MASK_B0 & (UpdateRegionY[1] >>> SHIFT_B1);
                case 461:
                    return MASK_B0 & DoubleSpeedThreshold;
                case 462:
                    return MASK_B0 & (DoubleSpeedThreshold >>> SHIFT_B1);
                case 463:
                    return MASK_B0 & Language;
                case 464:
                    return MASK_B0 & (Language >>> SHIFT_B1);
                case 465:
                    return MASK_B0 & CursorType;
                case 466:
                    return MASK_B0 & (CursorType >>> SHIFT_B1);
                case 467:
                    return MASK_B0 & OldHidden;
                case 468:
                    return MASK_B0 & (OldHidden >>> SHIFT_B1);
                case 469:
                    return Page;
                case 470:
                    return MASK_B0 & (Enabled ? 1 : 0);
                case 471:
                    return MASK_B0 & (InhibitDraw ? 1 : 0);
                case 472:
                    return MASK_B0 & (TimerInProgress ? 1 : 0);
                case 473:
                    return MASK_B0 & (InUIR ? 1 : 0);
                case 474:
                    return Mode;
                case 475:
                    return MASK_B0 & GranMask;
                case 476:
                    return MASK_B0 & (GranMask >>> SHIFT_B1);
                default: {
                    if (index < 135) {
                        int idx = index - 71;
                        if ((idx & 0b0001) == 0)
                            return EventQueue[idx >>> 1].Type;
                        else
                            return EventQueue[idx >>> 1].Buttons;
                    }
                    if (index < 403) {
                        int idx = index - 147;
                        return BackData[idx - (idx & 0b0001)];
                    }
                    throw new DOSException("");


                }

            }
        }



        // screenMask,cursorMask가 포인터 변수이므로 4바이트로 정의
        private static final int MOUSE_DATA_SIZE = 477;

        public int size() {
            return MOUSE_DATA_SIZE;
        };

        public MouseStruct() {
            TimesPressed = new short[MOUSE_BUTTONS];
            TimesReleased = new short[MOUSE_BUTTONS];
            LastReleasedX = new short[MOUSE_BUTTONS];
            LastReleasedy = new short[MOUSE_BUTTONS];
            LastPressedX = new short[MOUSE_BUTTONS];
            LastPressedY = new short[MOUSE_BUTTONS];
            EventQueue = new ButtonEvent[QUEUE_SIZE];
            for (int i = 0; i < QUEUE_SIZE; i++) {
                EventQueue[i] = new ButtonEvent();
            }
            BackData = new byte[CURSORX * CURSORY];
            UpdateRegionX = new short[2];
            UpdateRegionY = new short[2];

        }

    }
    // -- #endregion

    private MouseStruct mouse;

    // TODO 마우스 인터럽트 Int33 미구현 코드 구현할 것
    private int INT33Handler() {
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
                Register.setRegDX(mouse.LastReleasedy[but]);
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
                // LOG(LOG_MOUSE, LOG_NORMAL)("Define Hortizontal range min:%d max:%d", min, max);
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
                // LOG(LOG_MOUSE, LOG_NORMAL)("Define Vertical range min:%d max:%d", min, max);
            }
                break;
            case 0x09: /* Define GFX Cursor */
            {
                throw new DOSException("INT33_Handler 0x09 미구현");

                // int src = regsModule.SegPhys(regsModule.SEG_NAME_es) + regsModule.reg_dx;
                // MEMORY.MEM_BlockRead(src, userdefScreenMask, CURSORY * 2);
                // MEMORY.MEM_BlockRead(src + CURSORY * 2, userdefCursorMask, CURSORY * 2);
                // mouse.screenMask = userdefScreenMask;
                // mouse.screenMaskIsDefault = 1;
                // mouse.cursorMask = userdefCursorMask;
                // mouse.cursorMaskIsDefault = 1;
                // mouse.hotx = (short)regsModule.reg_bx;
                // mouse.hoty = (short)regsModule.reg_cx;
                // mouse.cursorType = 2;
                // DrawCursor();
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
                //// LOG(LOG_MOUSE, LOG_WARN)("Saving driver state...");
                int dest = Register.segPhys(Register.SEG_NAME_ES) + Register.getRegDX();
                Memory.blockWrite(dest, mouse, mouse.size());
            }
                break;
            case 0x17: /* load driver state */
            {
                throw new DOSException("INT33_Handler 0x17 미구현");

                //// LOG(LOG_MOUSE, LOG_WARN)("Loading driver state...");
                // int src = regsModule. SegPhys(regsModule.SegNames. es) + regsModule.reg_dx;
                // MEMORY.MEM_BlockRead(src, &mouse, mouse.Size);
                // mouse.screenMask = mouse.screenMaskIsDefault == 0 ? defaultScreenMask :
                //// userdefScreenMask;
                // mouse.cursorMask = mouse.cursorMaskIsDefault == 0 ? defaultCursorMask :
                //// userdefCursorMask;
            }
            // break;
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

    private int BD_Handler() {
        throw new DOSException("MOUSE_BD_Handler 미구현");
        // return Callback.ReturnTypeNone;
    }

    private int INT74Handler() {
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
                CPU.push16(Memory.realSeg(Callback.realPointer(_int74RetCallback)));
                CPU.push16(Memory.realOff(Callback.realPointer(_int74RetCallback)));
                Register.segSet16(Register.SEG_NAME_CS, mouse.SubSeg);
                Register.setRegIP(mouse.SubOfs);
                // if (mouse.in_UIR) LOG(LOG_MOUSE, LOG_ERROR)("Already in UIR!");
                mouse.InUIR = true;
            } else if (_usePs2Callback) {
                CPU.push16(Memory.realSeg(Callback.realPointer(_int74RetCallback)));
                CPU.push16(Memory.realOff(Callback.realPointer(_int74RetCallback)));
                doPS2Callback(mouse.EventQueue[mouse.Events].Buttons, posX(), posY());
            } else {
                Register.segSet16(Register.SEG_NAME_CS,
                        Memory.realSeg(Callback.realPointer(_int74RetCallback)));
                Register.setRegIP(Memory.realOff(Callback.realPointer(_int74RetCallback)));
            }
        } else {
            Register.segSet16(Register.SEG_NAME_CS,
                    Memory.realSeg(Callback.realPointer(_int74RetCallback)));
            Register.setRegIP(Memory.realOff(Callback.realPointer(_int74RetCallback)));
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
        byte mode = (byte) Memory.readB(BIOS.BIOS_VIDEO_MODE);
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
        mouse.ScreenMask = _defaultScreenMask;
        mouse.ScreenMaskIsDefault = 0;// screenMask가 Default 정의된 값을 사용한다고 표시, 메모리에
                                      // blockwrite,blockread할때 적용
        mouse.CursorMask = _defaultCursorMask;
        mouse.CursorMaskIsDefault = 0;// cursorMask가 Default 정의된 값을 사용한다고 표시, 메모리에
                                      // blockwrite,blockread할때 적용
        mouse.TextAndMask = _defaultTextAndMask;
        mouse.TextXorMask = _defaultTextXorMask;
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

        _oldMouseX = (short) mouse.X;
        _oldMouseY = (short) mouse.Y;


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
            CHAR.writeChar1(mouse.BackPosX, mouse.BackPosY,
                    Memory.realReadB(INT10.BIOSMEM_SEG, INT10.BIOSMEM_CURRENT_PAGE),
                    mouse.BackData[0], 0xff & mouse.BackData[1], true);
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
        byte page = (byte) Memory.realReadB(INT10.BIOSMEM_SEG, INT10.BIOSMEM_CURRENT_PAGE);
        int result = CHAR.readCharAttr((short) mouse.BackPosX, (short) mouse.BackPosY, page);
        mouse.BackData[0] = (byte) (result & 0xFF);
        mouse.BackData[1] = (byte) (result >>> 8);
        mouse.Background = true;
        // Write Cursor
        result = (short) ((result & mouse.TextAndMask) ^ mouse.TextXorMask);
        CHAR.writeChar1(mouse.BackPosX, mouse.BackPosY, page, (byte) (result & 0xFF),
                0xff & (result >>> 8), true);
    }


    // ***************************************************************************
    // Mouse cursor - graphic mode
    // ***************************************************************************

    private byte[] gfxReg3CE = new byte[9];
    int index3C4, gfxReg3C5;// uint8

    private void aaveVgaRegisters() {
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

    short[] cursorArea2 = new short[7];

    private void restoreCursorBackground() {
        if (mouse.Hidden != 0 || mouse.InhibitDraw)
            return;

        aaveVgaRegisters();
        if (mouse.Background) {
            // Restore background
            short x, y;
            short addx1 = 0, addx2 = 0, addy = 0;
            short dataPos = 0;
            short x1 = mouse.BackPosX;
            short y1 = mouse.BackPosY;
            short x2 = (short) (x1 + CURSORX - 1);
            short y2 = (short) (y1 + CURSORY - 1);

            cursorArea2[0] = x1;
            cursorArea2[1] = x2;
            cursorArea2[2] = y1;
            cursorArea2[3] = y2;
            cursorArea2[4] = addx1;
            cursorArea2[5] = addx2;
            cursorArea2[6] = addy;
            clipCursorArea(cursorArea2);
            x1 = cursorArea2[0];
            x2 = cursorArea2[1];
            y1 = cursorArea2[2];
            y2 = cursorArea2[3];
            addx1 = cursorArea2[4];
            addx2 = cursorArea2[5];
            addy = cursorArea2[6];
            dataPos = (short) (addy * CURSORX);
            for (y = y1; y <= y2; y++) {
                dataPos += addx1;
                for (x = x1; x <= x2; x++) {
                    INT10.putPixel(x, y, mouse.Page, mouse.BackData[dataPos++]);
                }
                dataPos += addx2;
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

    private void clipCursorArea(short[] cursorArea) {
        short x1 = cursorArea[0];
        short x2 = cursorArea[1];
        short y1 = cursorArea[2];
        short y2 = cursorArea[3];
        short addx1 = cursorArea[4];
        short addx2 = cursorArea[5];
        short addy = cursorArea[6];

        addx1 = addx2 = addy = 0;
        // Clip up
        if (y1 < 0) {
            addy += -y1;
            y1 = 0;
        }
        // Clip down
        if (y2 > mouse.ClipY) {
            y2 = mouse.ClipY;
        }
        // Clip left
        if (x1 < 0) {
            addx1 += -x1;
            x1 = 0;
        }
        // Clip right
        if (x2 > mouse.ClipX) {
            addx2 = (short) (x2 - mouse.ClipX);
            x2 = mouse.ClipX;
        }
        cursorArea[0] = x1;
        cursorArea[1] = x2;
        cursorArea[2] = y1;
        cursorArea[3] = y2;
        cursorArea[4] = addx1;
        cursorArea[5] = addx2;
        cursorArea[6] = addy;
    }

    short[] cursorArea1 = new short[7];

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

        aaveVgaRegisters();

        // Save Background
        short x, y;
        short addx1 = 0, addx2 = 0, addy = 0;
        short dataPos = 0;
        short x1 = (short) (posX() / xratio - mouse.HotX);
        short y1 = (short) (posY() - mouse.HotY);
        short x2 = (short) (x1 + CURSORX - 1);
        short y2 = (short) (y1 + CURSORY - 1);

        cursorArea1[0] = x1;
        cursorArea1[1] = x2;
        cursorArea1[2] = y1;
        cursorArea1[3] = y2;
        cursorArea1[4] = addx1;
        cursorArea1[5] = addx2;
        cursorArea1[6] = addy;
        clipCursorArea(cursorArea1);
        x1 = cursorArea1[0];
        x2 = cursorArea1[1];
        y1 = cursorArea1[2];
        y2 = cursorArea1[3];
        addx1 = cursorArea1[4];
        addx2 = cursorArea1[5];
        addy = cursorArea1[6];

        dataPos = (short) (addy * CURSORX);
        byte tmpColor = 0;
        for (y = y1; y <= y2; y++) {
            dataPos += addx1;
            for (x = x1; x <= x2; x++) {
                mouse.BackData[dataPos++] = INT10.getPixel(x, y, mouse.Page);
            }
            dataPos += addx2;
        }
        mouse.Background = true;
        mouse.BackPosX = (short) (posX() / xratio - mouse.HotX);
        mouse.BackPosY = (short) (posY() - mouse.HotY);

        // Draw Mousecursor
        dataPos = (short) (addy * CURSORX);
        for (y = y1; y <= y2; y++) {
            short scMask = mouse.ScreenMask[addy + y - y1];
            short cuMask = mouse.CursorMask[addy + y - y1];
            if (addx1 > 0) {
                scMask <<= addx1;
                cuMask <<= addx1;
                dataPos += addx1;
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
                INT10.putPixel((short) x, (short) y, mouse.Page, pixel);
                dataPos++;
            }
            dataPos += addx2;
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
        if (_usePs2Callback)
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

        if (!_usePs2Callback) {
            if (mouse.X > mouse.MaxX)
                mouse.X = mouse.MaxX;
            if (mouse.X < mouse.MinX)
                mouse.X = mouse.MinX;
            if (mouse.Y > mouse.MaxY)
                mouse.Y = mouse.MaxY;
            if (mouse.Y < mouse.MinY)
                mouse.Y = mouse.MinY;
        }
        addEvent((byte) MOUSE_HAS_MOVED);
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
        mouse.LastReleasedy[button] = (short) posY();
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
        _callINT33 = Callback.allocate();
        // RealPt i33loc=RealMake(CB_SEG+1,(call_int33*CB_SIZE)-0x10);
        int i33Loc = Memory.realMake(DOSMain.getMemory(0x1) - 1, 0x10);
        Callback.setup(_callINT33, this::INT33Handler, Callback.Symbol.MOUSE,
                Memory.real2Phys(i33Loc), "Mouse");
        // Wasteland needs low(seg(int33))!=0 and low(ofs(int33))!=0
        Memory.realWriteD(0, 0x33 << 2, i33Loc);

        _callMouseBD = Callback.allocate();
        Callback.setup(_callMouseBD, this::BD_Handler, Callback.Symbol.RETF8, Memory.physMake(
                0xffff & Memory.realSeg(i33Loc), 0xffff & (Memory.realOff(i33Loc) + 2)), "MouseBD");
        // pseudocode for CB_MOUSE (including the special backdoor entry point):
        // jump near i33hd
        // callback MOUSE_BD_Handler
        // retf 8
        // label i33hd:
        // callback INT33_Handler
        // iret


        // Callback for ps2 irq
        _callINT74 = Callback.allocate();
        Callback.setup(_callINT74, this::INT74Handler, Callback.Symbol.IRQ12, "int 74");
        // pseudocode for CB_IRQ12:
        // push ds
        // push es
        // pushad
        // sti
        // callback INT74_Handler
        // doesn't return here, but rather to CB_IRQ12_RET
        // (ps2 callback/user callback inbetween if requested)

        _int74RetCallback = Callback.allocate();
        Callback.setup(_int74RetCallback, this::UserIntCBHandler, Callback.Symbol.IRQ12_RET,
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

        byte hwvec = (MOUSE_IRQ > 7) ? (0x70 + MOUSE_IRQ - 8) : (0x8 + MOUSE_IRQ);
        Memory.realSetVec(hwvec, Callback.realPointer(_callINT74));

        // Callback for ps2 user callback handling
        _usePs2Callback = false;
        _ps2CallbackInit = false;
        _callPS2 = Callback.allocate();
        Callback.setup(_callPS2, this::PS2Handler, Callback.Symbol.RETF, "ps2 bios callback");
        _ps2Callback = Callback.realPointer(_callPS2);

        // memset(&mouse,0,sizeof(mouse));
        mouse = new MouseStruct();

        mouse.Hidden = 1; // Hide mouse on startup
        mouse.TimerInProgress = false;
        mouse.Mode = (byte) 0xFF; // Non existing mode

        mouse.SubMask = 0;
        mouse.SubSeg = 0x6362; // magic value
        mouse.SubOfs = 0;

        resetHardware();
        reset();
        setSensitivity(50, 50, 50);
    }

}
