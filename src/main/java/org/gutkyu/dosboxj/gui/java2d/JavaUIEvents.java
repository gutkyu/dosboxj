package org.gutkyu.dosboxj.gui.java2d;

import org.gutkyu.dosboxj.hardware.*;
import org.gutkyu.dosboxj.hardware.Keyboard.KBDKeys;
import org.gutkyu.dosboxj.hardware.video.VGA;
import java.awt.Frame;
import java.awt.Point;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.event.WindowStateListener;
import java.util.HashMap;
import java.util.LinkedList;
import org.gutkyu.dosboxj.gui.*;
import org.gutkyu.dosboxj.gui.java2d.JavaGFX.PriorityLevels;
import org.gutkyu.dosboxj.cpu.*;
import org.gutkyu.dosboxj.interrupt.*;
import org.gutkyu.dosboxj.misc.Debug;


/*--------------------------- begin AWTEvent -----------------------------*/
interface IAWTEvent {
    void handle(JavaGFX gfx);
}


final class JavaUIEventsBinder {
    JavaGFX _gfx;
    private LinkedList<IAWTEvent> _events = new LinkedList<IAWTEvent>();

    protected JavaUIEventsBinder(JavaGFX gfx) {
        initKeyMap();
        this._gfx = gfx;
        // _gfx._main.Activated += ActivatedHandler;
        // _gfx._main.Deactivated += DeactivatedHandler;
        // Application.Current.Activated += ActivatedHandler;
        // Application.Current.Deactivated += DeactivatedHandler;
        // _gfx._main.StateChanged += StateChanged;

        _gfx._main.addWindowStateListener(windowStateListener);
        _gfx._main.addFocusListener(focusListener);

        // TODO dosbox의 autolock을 제공할때 구현, 시스템 커서가 아닌 Dosbox의 고유 커서를 제공
        // _gfx.surface.addMouseListener(mouseListener);
        // _gfx.surface.addMouseMotionListener(mouseMotionListener);
        _gfx.surface.addKeyListener(keyListener);
    }

    // private boolean _enableEvent = false;
    protected void processEvents() {
        IAWTEvent event = null;
        while ((event = _events.poll()) != null) {
            event.handle(this._gfx);
        }
    }

    // void StateChanged(Object sender, EventArgs e)
    // {
    // }
    // void ActivatedHandler(Object sender, EventArgs e)
    // {
    // }
    // void DeactivatedHandler(Object sender, EventArgs e)
    // {
    // }
    WindowStateListener windowStateListener = new WindowStateListener() {

        @Override
        public void windowStateChanged(WindowEvent e) {
            AWTActiveEvent event = new AWTActiveEvent();
            event.State = AWTActiveEvent.APPACTIVE;
            event.Gain = (_gfx._main.getExtendedState() & Frame.ICONIFIED) != Frame.ICONIFIED;
            _events.addLast(event);
        }
    };
    WindowListener windowListener = new WindowListener() {

        @Override
        public void windowOpened(WindowEvent e) {

        }

        @Override
        public void windowIconified(WindowEvent e) {

        }

        @Override
        public void windowDeiconified(WindowEvent e) {

        }

        @Override
        public void windowDeactivated(WindowEvent e) {

        }

        @Override
        public void windowClosing(WindowEvent e) {

        }

        @Override
        public void windowClosed(WindowEvent e) {

        }

        @Override
        public void windowActivated(WindowEvent e) {

        }
    };

    FocusListener focusListener = new FocusListener() {

        @Override
        public void focusLost(FocusEvent e) {
            addEvent(false, e);

        }

        @Override
        public void focusGained(FocusEvent e) {
            addEvent(true, e);
        }

        private void addEvent(boolean gained, FocusEvent e) {
            // 엄밀하게 말하면 app의 포커스가 아니라 main window의 포커스를 처리, sdl과 약간 다름
            AWTActiveEvent event = new AWTActiveEvent();
            event.State = AWTActiveEvent.APPINPUTFOCUS;
            event.Gain = gained;
            _events.addLast(event);
        }
    };


    KeyListener keyListener = new KeyListener() {

        @Override
        public void keyTyped(KeyEvent e) {

        }

        @Override
        public void keyReleased(KeyEvent e) {
            addEvent(false, e);
        }

        @Override
        public void keyPressed(KeyEvent e) {
            addEvent(true, e);
        }

        private void addEvent(boolean keypPressed, KeyEvent e) {
            // if(_enableEvent) _events.AddLast(e);
            int keyCode = e.getKeyCode();
            if (!keyMap.containsKey(keyCode)) {
                System.out.printf("%d : 처리할 수 없는 키입니다.", keyCode);
                return;
            }

            KBDKeys dosKey = modifyKeyCode(e.getKeyCode(), e.getKeyLocation());
            AWTKeyEvent event = new AWTKeyEvent(dosKey);
            event.IsUp = !(event.IsDown = keypPressed);
            _events.addLast(event);
            System.out.printf("%d, is Up : %s, is Down : %s\n", keyCode, event.IsUp, event.IsDown);
        }
    };

    MouseListener mouseListener = new MouseListener() {

        @Override
        public void mouseReleased(MouseEvent e) {
            addEvent(false, e);
        }

        @Override
        public void mousePressed(MouseEvent e) {
            addEvent(true, e);
        }

        @Override
        public void mouseExited(MouseEvent e) {

        }

        @Override
        public void mouseEntered(MouseEvent e) {

        }

        @Override
        public void mouseClicked(MouseEvent e) {

        }

        private void addEvent(boolean pressed, MouseEvent e) {
            AWTMouseButtonEvent event = new AWTMouseButtonEvent();
            event.ButtonLocation = e.getButton();
            event.ButtonStatus = pressed ? 1 : 2;
            _events.addLast(event);
            System.out.printf("mouse button down : %d", event.ButtonLocation);
        }
    };

    MouseMotionListener mouseMotionListener = new MouseMotionListener() {

        @Override
        public void mouseMoved(MouseEvent e) {
            Point pos = e.getPoint();
            AWTMouseMotionEvent event = new AWTMouseMotionEvent();
            event.Pos = pos;
            _events.addLast(event);
        }

        @Override
        public void mouseDragged(MouseEvent e) {

        }
    };



    static final class AWTActiveEvent implements IAWTEvent {


        public static final int APPMOUSEFOCUS = 0x01;
        public static final int APPINPUTFOCUS = 0x02;
        public static final int APPACTIVE = 0x04;



        public boolean Gain;
        public int State;
        private static boolean paused = false;

        // -- #region WPFEventArgs 멤버
        public void handle(JavaGFX gfx) {
            if (paused) {
                readyActivateEvent(gfx);
                try {
                    Thread.sleep(200);
                } catch (Exception e) {

                }
                return;
            }

            if (State == APPINPUTFOCUS) {
                if (Gain) {
                    if (gfx._devFullScreen && !gfx._mouseLocked)
                        gfx.captureMouse();
                    gfx.setPriority(gfx._priorityNoFocus);
                    CPU.disableSkipAutoAdjust();
                } else {
                    if (gfx.mouselocked) {

                        if (gfx._devFullScreen) {
                            VGA.instance().killDrawing();
                            gfx._devFullScreen = false;
                            gfx.resetScreen();
                        }

                        gfx.captureMouse();
                    }
                    gfx.setPriority(gfx._priorityNoFocus);
                    gfx.losingFocus();
                    CPU.enableSkipAutoAdjust();
                }
            }

            /*
             * Non-focus priority is set to pause; check to see if we've lost window or input focus
             * i.e. has the window been minimised or made inactive?
             */
            if (gfx._priorityNoFocus == PriorityLevels.Pause) {
                if ((State == APPINPUTFOCUS || State == APPACTIVE) && !Gain) {
                    /*
                     * Window has lost focus, pause the emulator. This is similar to what
                     * PauseDOSBox() does, but the exit criteria is different. Instead of waiting
                     * for the user to hit Alt-Break, we wait for the window to regain window or
                     * input focus.
                     */
                    paused = true;

                    GUIPlatform.gfx.setTitle(-1, -1, true);
                    Keyboard.instance().clrBuffer();
                    try {
                        Thread.sleep(500);
                    } catch (Exception e) {

                    }
                    // SDL_Delay(500);
                    // while (SDL_PollEvent(&ev)) {
                    // flush event queue.
                    // }

                }
            }
        }

        private void readyActivateEvent(JavaGFX gfx) {
            // switch (State)
            // {
            // case SDL_QUIT: throw (0); break; // a bit redundant at linux at least as the active
            // events gets before the quit event.
            // case States. SDL_ACTIVEEVENT: // wait until we get window focus back
            if (State == APPINPUTFOCUS || State == APPACTIVE) {
                // We've got focus back, so unpause and break out of the loop
                if (Gain) {
                    paused = false;
                    gfx.setTitle(-1, -1, false);
                }

                /*
                 * Now poke a "release ALT" command into the keyboard buffer we have to do this,
                 * otherwise ALT will 'stick' and cause problems with the app running in the DOSBox.
                 */
                Keyboard.instance().addKey(Keyboard.KBDKeys.KBD_leftalt, false);
                Keyboard.instance().addKey(Keyboard.KBDKeys.KBD_rightalt, false);
            }
            // break;
            // }
        }
        // -- #endregion

    }

    final class AWTKeyEvent implements IAWTEvent {
        // public Key Key = Key.None;
        private Keyboard.KBDKeys _key;
        public boolean IsDown;
        public boolean IsUp;

        public AWTKeyEvent(Keyboard.KBDKeys Key) {
            _key = Key;
            IsUp = IsDown = false;
        }

        public void handle(JavaGFX gfx) {
            if (IsDown)
                Keyboard.instance().addKey(_key, true);
            else if (IsUp)
                Keyboard.instance().addKey(_key, false);
        }
    }

    static final class AWTMouseMotionEvent implements IAWTEvent {
        private static Point _prePos = null;
        public Point Pos;


        // HandleMouseMotion
        public void handle(JavaGFX gfx) {
            if (!Debug.mouseEnabled)
                return;
            if (!gfx._mouseLocked && gfx._mouseAutoEnable)
                return;
            double offX = 0.0f, offY = 0.0f;
            if (_prePos != null) {
                offX = Pos.x - _prePos.x;
                offY = Pos.y - _prePos.y;
            }
            // mouse lock 일 경우에만 mouse capture 수행
            // 현재 mouse lock를 배제한 상태
            Mouse.instance().cursorMoved((float) (offX * gfx._mouseSensitivity / 100.0f),
                    (float) (offY * gfx._mouseSensitivity / 100.0f),
                    (float) (Pos.x - gfx._clip.x) / (gfx._clip.width - 1) * gfx._mouseSensitivity
                            / 100.0f,
                    (float) (Pos.y - gfx._clip.y) / (gfx._clip.height - 1) * gfx._mouseSensitivity
                            / 100.0f,
                    (float) (Pos.x * gfx._clip.width / gfx.surface.getWidth()),
                    (float) (Pos.y * gfx._clip.height / gfx.surface.getHeight()), gfx._mouseLocked);
            _prePos = Pos;
        }

    }

    static final class AWTMouseButtonEvent implements IAWTEvent {
        public Point Pos;

        public static final int MOUSE_BUTTON_LEFT = MouseEvent.BUTTON1;
        public static final int MOUSE_BUTTON_MIDDLE = MouseEvent.BUTTON3;
        public static final int MOUSE_BUTTON_RIGHT = MouseEvent.BUTTON2;
        public static final int MOUSE_BUTTON_PRESSED = 1;
        public static final int MOUSE_BUTTON_RELEASED = 2;
        public int ButtonLocation;
        public int ButtonStatus;


        public void handle(JavaGFX gfx) {
            if (!Debug.mouseEnabled)
                return;
            switch (ButtonStatus) {
                case MOUSE_BUTTON_PRESSED:
                    if (gfx._mouseRequestLock && !gfx._mouseLocked) {
                        gfx.captureMouse();
                        // Dont pass klick to mouse handler
                        break;
                    }
                    if (!gfx._mouseAutoEnable && gfx._mouseAutoLock
                            && ButtonLocation == MOUSE_BUTTON_MIDDLE) {
                        gfx.captureMouse();
                        break;
                    }
                    switch (ButtonLocation) {
                        case MOUSE_BUTTON_LEFT:
                            Mouse.instance().buttonPressed(0);
                            break;
                        case MOUSE_BUTTON_RIGHT:
                            Mouse.instance().buttonPressed(1);
                            break;
                        case MOUSE_BUTTON_MIDDLE:
                            Mouse.instance().buttonPressed(2);
                            break;
                    }
                    break;
                case MOUSE_BUTTON_RELEASED:
                    switch (ButtonLocation) {
                        case MOUSE_BUTTON_LEFT:
                            Mouse.instance().buttonReleased(0);
                            break;
                        case MOUSE_BUTTON_RIGHT:
                            Mouse.instance().buttonReleased(1);
                            break;
                        case MOUSE_BUTTON_MIDDLE:
                            Mouse.instance().buttonReleased(2);
                            break;
                    }
                    break;
            }
        }

    }

    private HashMap<Integer, Keyboard.KBDKeys> keyMap = new HashMap<>();

    private void initKeyMap() {

        keyMap.put(KeyEvent.VK_ESCAPE, Keyboard.KBDKeys.KBD_esc);
        keyMap.put(KeyEvent.VK_1, Keyboard.KBDKeys.KBD_1);
        keyMap.put(KeyEvent.VK_2, Keyboard.KBDKeys.KBD_2);
        keyMap.put(KeyEvent.VK_3, Keyboard.KBDKeys.KBD_3);
        keyMap.put(KeyEvent.VK_4, Keyboard.KBDKeys.KBD_4);
        keyMap.put(KeyEvent.VK_5, Keyboard.KBDKeys.KBD_5);
        keyMap.put(KeyEvent.VK_6, Keyboard.KBDKeys.KBD_6);
        keyMap.put(KeyEvent.VK_7, Keyboard.KBDKeys.KBD_7);
        keyMap.put(KeyEvent.VK_8, Keyboard.KBDKeys.KBD_8);
        keyMap.put(KeyEvent.VK_9, Keyboard.KBDKeys.KBD_9);
        keyMap.put(KeyEvent.VK_0, Keyboard.KBDKeys.KBD_0);

        keyMap.put(KeyEvent.VK_MINUS, Keyboard.KBDKeys.KBD_minus);
        keyMap.put(KeyEvent.VK_PLUS, Keyboard.KBDKeys.KBD_equals);
        keyMap.put(KeyEvent.VK_BACK_SPACE, Keyboard.KBDKeys.KBD_backspace);
        keyMap.put(KeyEvent.VK_TAB, Keyboard.KBDKeys.KBD_tab);

        keyMap.put(KeyEvent.VK_Q, Keyboard.KBDKeys.KBD_q);
        keyMap.put(KeyEvent.VK_W, Keyboard.KBDKeys.KBD_w);
        keyMap.put(KeyEvent.VK_E, Keyboard.KBDKeys.KBD_e);
        keyMap.put(KeyEvent.VK_R, Keyboard.KBDKeys.KBD_r);
        keyMap.put(KeyEvent.VK_T, Keyboard.KBDKeys.KBD_t);
        keyMap.put(KeyEvent.VK_Y, Keyboard.KBDKeys.KBD_y);
        keyMap.put(KeyEvent.VK_U, Keyboard.KBDKeys.KBD_u);
        keyMap.put(KeyEvent.VK_I, Keyboard.KBDKeys.KBD_i);
        keyMap.put(KeyEvent.VK_O, Keyboard.KBDKeys.KBD_o);
        keyMap.put(KeyEvent.VK_P, Keyboard.KBDKeys.KBD_p);

        keyMap.put(KeyEvent.VK_OPEN_BRACKET, Keyboard.KBDKeys.KBD_leftbracket);
        keyMap.put(KeyEvent.VK_CLOSE_BRACKET, Keyboard.KBDKeys.KBD_rightbracket);
        keyMap.put(KeyEvent.VK_ENTER, Keyboard.KBDKeys.KBD_enter);// AWT에서 키패드 엔터키 별도 키코드 없음
        keyMap.put(KeyEvent.VK_CONTROL, Keyboard.KBDKeys.KBD_leftctrl);// left control는 이벤트 처리기에서 확인

        keyMap.put(KeyEvent.VK_A, Keyboard.KBDKeys.KBD_a);
        keyMap.put(KeyEvent.VK_S, Keyboard.KBDKeys.KBD_s);
        keyMap.put(KeyEvent.VK_D, Keyboard.KBDKeys.KBD_d);
        keyMap.put(KeyEvent.VK_F, Keyboard.KBDKeys.KBD_f);
        keyMap.put(KeyEvent.VK_G, Keyboard.KBDKeys.KBD_g);
        keyMap.put(KeyEvent.VK_H, Keyboard.KBDKeys.KBD_h);
        keyMap.put(KeyEvent.VK_J, Keyboard.KBDKeys.KBD_j);
        keyMap.put(KeyEvent.VK_K, Keyboard.KBDKeys.KBD_k);
        keyMap.put(KeyEvent.VK_L, Keyboard.KBDKeys.KBD_l);

        keyMap.put(KeyEvent.VK_SEMICOLON, Keyboard.KBDKeys.KBD_semicolon);
        keyMap.put(KeyEvent.VK_QUOTE, Keyboard.KBDKeys.KBD_quote);
        keyMap.put(KeyEvent.VK_BACK_QUOTE, Keyboard.KBDKeys.KBD_grave);
        keyMap.put(KeyEvent.VK_SHIFT, Keyboard.KBDKeys.KBD_leftshift);// left shift는 이벤트처리기에서 확인
        keyMap.put(KeyEvent.VK_BACK_SLASH, Keyboard.KBDKeys.KBD_backslash);
        keyMap.put(KeyEvent.VK_Z, Keyboard.KBDKeys.KBD_z);
        keyMap.put(KeyEvent.VK_X, Keyboard.KBDKeys.KBD_x);
        keyMap.put(KeyEvent.VK_C, Keyboard.KBDKeys.KBD_c);
        keyMap.put(KeyEvent.VK_V, Keyboard.KBDKeys.KBD_v);
        keyMap.put(KeyEvent.VK_B, Keyboard.KBDKeys.KBD_b);
        keyMap.put(KeyEvent.VK_N, Keyboard.KBDKeys.KBD_n);
        keyMap.put(KeyEvent.VK_M, Keyboard.KBDKeys.KBD_m);

        keyMap.put(KeyEvent.VK_COMMA, Keyboard.KBDKeys.KBD_comma);
        keyMap.put(KeyEvent.VK_PERIOD, Keyboard.KBDKeys.KBD_period);
        keyMap.put(KeyEvent.VK_SLASH, Keyboard.KBDKeys.KBD_slash);
        keyMap.put(KeyEvent.VK_SHIFT, Keyboard.KBDKeys.KBD_rightshift);// right shift는 이벤트처리기에서 확인
        keyMap.put(KeyEvent.VK_MULTIPLY, Keyboard.KBDKeys.KBD_kpmultiply);
        keyMap.put(KeyEvent.VK_ALT, Keyboard.KBDKeys.KBD_leftalt);// left alt는 이벤트처리기에서 확인
        keyMap.put(KeyEvent.VK_SPACE, Keyboard.KBDKeys.KBD_space);
        keyMap.put(KeyEvent.VK_CAPS_LOCK, Keyboard.KBDKeys.KBD_capslock);

        keyMap.put(KeyEvent.VK_F1, Keyboard.KBDKeys.KBD_f1);
        keyMap.put(KeyEvent.VK_F2, Keyboard.KBDKeys.KBD_f2);
        keyMap.put(KeyEvent.VK_F3, Keyboard.KBDKeys.KBD_f3);
        keyMap.put(KeyEvent.VK_F4, Keyboard.KBDKeys.KBD_f4);
        keyMap.put(KeyEvent.VK_F5, Keyboard.KBDKeys.KBD_f5);
        keyMap.put(KeyEvent.VK_F6, Keyboard.KBDKeys.KBD_f6);
        keyMap.put(KeyEvent.VK_F7, Keyboard.KBDKeys.KBD_f7);
        keyMap.put(KeyEvent.VK_F8, Keyboard.KBDKeys.KBD_f8);
        keyMap.put(KeyEvent.VK_F9, Keyboard.KBDKeys.KBD_f9);
        keyMap.put(KeyEvent.VK_F10, Keyboard.KBDKeys.KBD_f10);

        keyMap.put(KeyEvent.VK_NUM_LOCK, Keyboard.KBDKeys.KBD_numlock);
        keyMap.put(KeyEvent.VK_SCROLL_LOCK, Keyboard.KBDKeys.KBD_scrolllock);

        keyMap.put(KeyEvent.VK_NUMPAD7, Keyboard.KBDKeys.KBD_kp7);
        keyMap.put(KeyEvent.VK_NUMPAD8, Keyboard.KBDKeys.KBD_kp8);
        keyMap.put(KeyEvent.VK_NUMPAD9, Keyboard.KBDKeys.KBD_kp9);
        keyMap.put(KeyEvent.VK_SUBTRACT, Keyboard.KBDKeys.KBD_kpminus);
        keyMap.put(KeyEvent.VK_NUMPAD4, Keyboard.KBDKeys.KBD_kp4);
        keyMap.put(KeyEvent.VK_NUMPAD5, Keyboard.KBDKeys.KBD_kp5);
        keyMap.put(KeyEvent.VK_NUMPAD6, Keyboard.KBDKeys.KBD_kp6);
        keyMap.put(KeyEvent.VK_ADD, Keyboard.KBDKeys.KBD_kpplus);
        keyMap.put(KeyEvent.VK_NUMPAD1, Keyboard.KBDKeys.KBD_kp1);
        keyMap.put(KeyEvent.VK_NUMPAD2, Keyboard.KBDKeys.KBD_kp2);
        keyMap.put(KeyEvent.VK_NUMPAD3, Keyboard.KBDKeys.KBD_kp3);
        keyMap.put(KeyEvent.VK_NUMPAD0, Keyboard.KBDKeys.KBD_kp0);
        keyMap.put(KeyEvent.VK_DECIMAL, Keyboard.KBDKeys.KBD_kpperiod);
        // _keyMap[Key.]=Keyboard.KBD_KEYS.KBD_extra_lt_gt;//독일 키보드 같이 120key 일때 의미 있을듯, 일단 생략
        keyMap.put(KeyEvent.VK_F11, Keyboard.KBDKeys.KBD_f11);
        keyMap.put(KeyEvent.VK_F12, Keyboard.KBDKeys.KBD_f12);

        // The Extended keys
        keyMap.put(KeyEvent.VK_ENTER, Keyboard.KBDKeys.KBD_kpenter); // AWT에서 키패드 엔터키 별도 구분키코드 없음
        keyMap.put(KeyEvent.VK_CONTROL, Keyboard.KBDKeys.KBD_rightctrl);// right control는 이벤트 처리기에서
                                                                        // 확인
        keyMap.put(KeyEvent.VK_DIVIDE, Keyboard.KBDKeys.KBD_kpdivide);
        keyMap.put(KeyEvent.VK_ALT, Keyboard.KBDKeys.KBD_rightalt);// right alt는 이벤트처리기에서 확인
        keyMap.put(KeyEvent.VK_HOME, Keyboard.KBDKeys.KBD_home);
        keyMap.put(KeyEvent.VK_UP, Keyboard.KBDKeys.KBD_up);
        keyMap.put(KeyEvent.VK_PAGE_UP, Keyboard.KBDKeys.KBD_pageup);
        keyMap.put(KeyEvent.VK_LEFT, Keyboard.KBDKeys.KBD_left);
        keyMap.put(KeyEvent.VK_RIGHT, Keyboard.KBDKeys.KBD_right);
        keyMap.put(KeyEvent.VK_END, Keyboard.KBDKeys.KBD_end);
        keyMap.put(KeyEvent.VK_DOWN, Keyboard.KBDKeys.KBD_down);
        keyMap.put(KeyEvent.VK_PAGE_DOWN, Keyboard.KBDKeys.KBD_pagedown);
        keyMap.put(KeyEvent.VK_INSERT, Keyboard.KBDKeys.KBD_insert);
        keyMap.put(KeyEvent.VK_DELETE, Keyboard.KBDKeys.KBD_delete);
        keyMap.put(KeyEvent.VK_PAUSE, Keyboard.KBDKeys.KBD_pause);
        keyMap.put(KeyEvent.VK_PRINTSCREEN, Keyboard.KBDKeys.KBD_printscreen);

    }

    private KBDKeys modifyKeyCode(int keyCode, int keyLocation) {
        KBDKeys dosKey = null;

        switch (keyCode) {
            case KeyEvent.VK_ALT:
                dosKey = keyLocation == KeyEvent.KEY_LOCATION_LEFT ? Keyboard.KBDKeys.KBD_leftalt
                        : Keyboard.KBDKeys.KBD_rightalt;
                break;
            case KeyEvent.VK_CONTROL:
                dosKey = keyLocation == KeyEvent.KEY_LOCATION_LEFT ? Keyboard.KBDKeys.KBD_leftctrl
                        : Keyboard.KBDKeys.KBD_rightctrl;
                break;
            case KeyEvent.VK_SHIFT:
                dosKey = keyLocation == KeyEvent.KEY_LOCATION_LEFT ? Keyboard.KBDKeys.KBD_leftshift
                        : Keyboard.KBDKeys.KBD_rightshift;
                break;
            case KeyEvent.VK_ENTER:
                dosKey = keyLocation == KeyEvent.KEY_LOCATION_NUMPAD ? Keyboard.KBDKeys.KBD_enter
                        : Keyboard.KBDKeys.KBD_kpenter;
                break;
            default:
                dosKey = keyMap.get(keyCode);
        }
        return dosKey;

    }

}

/*--------------------------- end AWTEvent -----------------------------*/

