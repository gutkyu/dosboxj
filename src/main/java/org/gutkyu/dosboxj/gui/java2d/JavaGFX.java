package org.gutkyu.dosboxj.gui.java2d;

import org.gutkyu.dosboxj.hardware.*;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Frame;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import javax.swing.JOptionPane;
import org.gutkyu.dosboxj.gui.*;
import org.gutkyu.dosboxj.gui.GFXCallback.GFXCallbackType;
import org.gutkyu.dosboxj.cpu.*;
import org.gutkyu.dosboxj.util.DOSException;
import org.gutkyu.dosboxj.misc.setup.*;
import org.gutkyu.dosboxj.misc.setup.Value.WrongType;
import org.gutkyu.dosboxj.interrupt.*;

public final class JavaGFX extends Mapper implements IGFX, MouseAutoLockable {

    enum PriorityLevels {
        Pause, Lowest, Lower, Normal, Higher, Highest
    }

    // --------------------------------- region AWT_Block start ----------------------------------//

    // boolean inited; //여기에서는 무조건 true
    boolean active; // If this isn't set don't draw

    boolean updating;

    int _drawWidth;
    int _drawHeight;
    int _drawBpp;
    int _drawFlags;
    double _drawScaleX, _drawScaleY;
    GFXCallback _drawCallback;
    boolean _waitOnError;

    int _devFullWidth, _devFullHeight;
    boolean _devFullFixed;
    short _devWindowWidth, _devWindowHeight;

    // 32bpp로 고정, 구현필요없음
    int _devBpp;
    boolean _devFullScreen;
    boolean _devDoubleBuf;
    // screen은 사용할 라이브러리가 고정, 구현필요없음
    // SCREEN_TYPES type;
    // SCREEN_TYPES want_type;


    PriorityLevels _priorityFocus;
    PriorityLevels _priorityNoFocus;

    Rectangle _clip = new Rectangle();
    int[] _pixels = null;
    int _pitch;
    int _stride = 0;

    // ??
    // SDL_cond *cond;

    boolean _mouseAutoLock;
    boolean _mouseAutoEnable;
    boolean _mouseRequestLock;
    boolean _mouseLocked;
    int _mouseSensitivity;

    // Int32Rect[] updateRects = new Int32Rect[1024];
    private static Rectangle _upRect = new Rectangle();
    int _numJoysticks;

    // state of alt-keys for certain special handlings
    byte LAltState;
    byte RAltState;
    // ---------------------------------- region AWT_Block end ----------------------------------//

    private static final int DEFAULT_HEIGHT_PIXEL = 480;
    private static final int DEFAULT_WIDTH_PIXEL = 640;

    Frame _main = null;
    JavaSurface surface = null;
    JavaUIEventsBinder eventHandlers = null;

    public JavaGFX() {
        _main = new Frame();
        _main.setBackground(Color.BLACK);
        surface = new JavaSurface();
        _main.add(surface, BorderLayout.CENTER);
        // GFX
        surface.newImage(DEFAULT_WIDTH_PIXEL, DEFAULT_HEIGHT_PIXEL, BufferedImage.TYPE_INT_ARGB);
        _pixels = surface.getIntPixels();
        _pitch = getPitch(_devFullWidth); // uint 기준 pitch
        // _stride = GetStride(imageBuffer, _devFullWidth);

        // Mapper
        eventHandlers = new JavaUIEventsBinder(this);
        new Mouse.AutoLockBinder(this);

        _main.pack();
        _main.setVisible(true);

    }

    private int[] newPixelBuffer(BufferedImage image) {
        return ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
    }

    private int getPixelSize(BufferedImage image) {
        return image.getHeight() * image.getWidth();
    }

    private int getPitch(int width) {
        return width;
    }

    private int getStride(BufferedImage image, int width) {
        return (width * image.getColorModel().getPixelSize() + 7) / 8;
    }

    // TODO have to implement
    public void setTitle(int cycles, int frameskip, boolean paused) {

    }

    private void pauseDOSBox(boolean pressed) {
        if (!pressed)
            return;
        setTitle(-1, -1, true);

        Keyboard.instance().clrBuffer();

        JOptionPane.showMessageDialog(_main, "일시 정지!");
        setTitle(-1, -1, false);

    }

    @Override
    public void GFXEvents() {
        eventHandlers.processEvents();
    }

    /* Reset the screen with current values in the sdl structure */
    // 32bpp만 지원하도록 수정
    // TODO 하드웨어 지원 색상수를 찾는 코드 추가할것
    //
    // 일단 사용하지 않음.
    public int getBestMode(int flags) {
        if ((flags & GFXFlag.CAN32) != 0)
            flags &= ~(GFXFlag.CAN8 | GFXFlag.CAN15 | GFXFlag.CAN16);
        flags |= GFXFlag.CAN_RANDOM;
        return flags;
    }

    public void resetScreen() {
        stop();
        if (_drawCallback != null)
            _drawCallback.call(GFXCallbackType.Reset);
        start();
        CPU.resetAutoAdjust();
    }

    // TODO 런타임에 찾아내거나 시작할때 설정에서 불러오도록 수정 할필요있음
    private static final double _dpi = 300;

    public int setSize(int width, int height, int flags, double scalex, double scaley,
            GFXCallback callback) {
        if (updating)
            endUpdate(null);

        _drawWidth = width;
        _drawHeight = height;
        _drawCallback = callback;
        _drawScaleX = scalex;
        _drawScaleY = scaley;

        // PixelFormat bpp = PixelFormats.Bgra32;
        int bpp = BufferedImage.TYPE_INT_ARGB;
        int retFlags = 0;

        // if ((flags & (uint)GFX_Flag.GFX_CAN_8) != 0) bpp = 8;
        // if ((flags & (uint)GFX_Flag.GFX_CAN_15) != 0) bpp = 15;
        // if ((flags & (uint)GFX_Flag.GFX_CAN_16) != 0) bpp = 16;
        // if ((flags & GFXFlag.CAN32) != 0) bpp = PixelFormats.Bgra32;
        if ((flags & GFXFlag.CAN32) != 0)
            bpp = BufferedImage.TYPE_INT_ARGB;
        _clip.width = width;
        _clip.height = height;

        // _main.Topmost = dev_fullscreen;
        if (_devFullScreen) {
            _main.setResizable(false);

            // setWindowWithNoTitleStyle();//_main.WindowStyle = WindowStyle.None;
            onFullScreen();
            if (_devFullFixed) {
                _clip.x = (_devFullWidth - width) / 2;
                _clip.y = (_devFullHeight - height) / 2;
                surface.newImage(_devFullWidth, _devFullHeight, bpp);
                _pixels = surface.getIntPixels();
                _pitch = getPitch(_devFullWidth);
                _main.pack();
            } else {
                _clip.x = 0;
                _clip.y = 0;
                surface.newImage(_devFullWidth, _devFullHeight, bpp);
                _pixels = surface.getIntPixels();
                _pitch = getPitch(width);
                _main.pack();
            }
        } else {
            offFullScreen();
            _main.setResizable(true);
            _clip.x = 0;
            _clip.y = 0;
            surface.newImage(width, height, bpp);
            _pixels = surface.getIntPixels();
            _pitch = getPitch(width);

            _main.pack();
        }
        if (surface.hasImage()) {
            switch (surface.getPixelSize()) {
                case 8:
                    retFlags = GFXFlag.CAN8;
                    break;
                case 15:
                    retFlags = GFXFlag.CAN15;
                    break;
                case 16:
                    retFlags = GFXFlag.CAN16;
                    break;
                case 32:
                    retFlags = GFXFlag.CAN32;
                    break;
            }

        }

        if (retFlags != 0)
            start();
        if (!_mouseAutoEnable)
            if (_mouseAutoLock)
                showCursor();
            else
                hideCursor();

        return retFlags;
    }

    private void setWindowWithNoTitleStyle() {
        if (_main.isUndecorated())
            return;
        _main.setVisible(false);
        _main.dispose();
        _main.setUndecorated(true);
        _main.setVisible(true);
    }

    private Rectangle windowNormalBounds = null;
    private boolean isFullScreenMode = false;

    private boolean onFullScreen() {
        GraphicsDevice dev = _main.getGraphicsConfiguration().getDevice();
        if (_main.isUndecorated() || !dev.isFullScreenSupported())
            return false;
        windowNormalBounds = _main.getBounds();
        _main.setVisible(false);
        _main.dispose();
        dev.setFullScreenWindow(_main);
        _main.setUndecorated(true);
        _main.setVisible(true);
        return true;
    }

    private boolean offFullScreen() {
        GraphicsDevice dev = _main.getGraphicsConfiguration().getDevice();
        if (!_main.isUndecorated())
            return false;
        if (_main.getGraphicsConfiguration().getDevice().getFullScreenWindow() == _main) {
            _main.setVisible(false);
            _main.dispose();
            if (windowNormalBounds != null)
                _main.setBounds(windowNormalBounds);
            windowNormalBounds = null;
            _main.setUndecorated(false);
            _main.setVisible(true);
        }
        return true;
    }

    private Cursor _currCursor = new Cursor(Cursor.MOVE_CURSOR);
    private Cursor CURSOR_NONE = Toolkit.getDefaultToolkit().createCustomCursor(
            new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB), new Point(0, 0),
            "custom hide cursor");

    private void showCursor() {

        _main.setCursor(_currCursor);
    }

    private void hideCursor() {
        _currCursor = _main.getCursor();
        _main.setCursor(CURSOR_NONE);
    }

    // TODO
    // SDL_WM_GrabInput 미구현
    public void captureMouse() {
        _mouseLocked = !_mouseLocked;
        if (_mouseLocked) {
            // SDL_WM_GrabInput(SDL_GRAB_ON);
            hideCursor();
        } else {
            // SDL_WM_GrabInput(SDL_GRAB_OFF);
            if (_mouseAutoEnable || !_mouseAutoLock)
                showCursor();
        }
        mouselocked = _mouseLocked;
    }

    boolean mouselocked; // Global variable for mapper

    public void switchFullScreen() {
        _devFullScreen = !_devFullScreen;
        if (_devFullScreen) {
            if (!_mouseLocked)
                captureMouse();
        } else {
            if (_mouseLocked)
                captureMouse();
        }
        resetScreen();
    }

    private void switchFullScreen(boolean pressed) {
        if (!pressed)
            return;
        switchFullScreen();
    }

    // lock 구조를 사용하지 않으므로 무조건 false 리턴
    public boolean startUpdate() {
        if (!active || updating)
            return false;

        // return false;//lock 구조를 사용하지 않으므로 무조건 false 리턴

        // if (SDL_MUSTLOCK(sdl.surface) && SDL_LockSurface(sdl.surface))
        // return false;
        updating = true;
        return true;
    }

    public int[] getPixels() {
        return _pixels;
    }

    public long getCurrentPixelIndex() {
        long pxIndex = 0;
        pxIndex += _clip.y * _pitch;
        return pxIndex += _clip.x * surface.getPixelSize();
    }

    public int getPitch() {
        return _pitch;
    }

    // SDL_LOCK없이 업데이트하도록 구현
    // updateRects는 이 클래스에서만 사용
    // updateRects 사용 안함
    public void endUpdate(short[] changedLines) {

        if (!updating)
            return;
        updating = false;

        if (changedLines != null && changedLines.length > 0) {

            int y = 0, index = 0;
            while (y < _drawHeight) {
                if ((index & 1) == 0) {
                    y += changedLines[index];
                } else {
                    _upRect.x = _clip.x;
                    _upRect.y = _clip.y + y;
                    _upRect.width = _drawWidth;
                    _upRect.height = changedLines[index];

                    surface.requestUpdate(_upRect);

                    y += changedLines[index];
                }
                index++;
            }
        }

    }

    // void GFX_SetPalette(Bitu start,Bitu count,GFX_PalEntry * entries)

    // public int getRGB(byte red, byte green, byte blue)
    public int getRGB(int red, int green, int blue) {
        // ARGB
        return 0xff << 24 | (red & 0xff) << 16 | (green & 0xff) << 8 | (blue & 0xff) << 0;
    }

    public void stop() {
        if (updating)
            endUpdate(null);
        active = false;
    }

    public void start() {
        active = true;
    }

    private void shutdownGUI(Section sec) {
        stop();
        if (_drawCallback != null)
            _drawCallback.call(GFXCallbackType.Stop);
        if (_mouseLocked)
            captureMouse();
        if (_devFullScreen)
            switchFullScreen();
    }

    private void killSwitch(boolean pressed) {
        if (!pressed)
            return;
        // throw 1;
        throw new DOSException("Kill Switch");
    }

    // 구현할 필요있을까?
    protected void setPriority(PriorityLevels level) {
        /*
         * #if C_SET_PRIORITY // Do nothing if priorties are not the same and not root, else the
         * highest // priority can not be set as users can only lower priority (not restore it)
         * 
         * if((sdl.priority.focus != sdl.priority.nofocus ) && (getuid()!=0) ) return;
         * 
         * #endif
         */
        // switch (level) {

        // case PRIORITY_LEVEL_PAUSE: // if DOSBox is paused, assume idle priority
        // case PRIORITY_LEVEL_LOWEST:
        // SetPriorityClass(GetCurrentProcess(),IDLE_PRIORITY_CLASS);
        // break;
        // case PRIORITY_LEVEL_LOWER:
        // SetPriorityClass(GetCurrentProcess(),BELOW_NORMAL_PRIORITY_CLASS);
        // break;
        // case PRIORITY_LEVEL_NORMAL:
        // SetPriorityClass(GetCurrentProcess(),NORMAL_PRIORITY_CLASS);
        // break;
        // case PRIORITY_LEVEL_HIGHER:
        // SetPriorityClass(GetCurrentProcess(),ABOVE_NORMAL_PRIORITY_CLASS);
        // break;
        // case PRIORITY_LEVEL_HIGHEST:
        // SetPriorityClass(GetCurrentProcess(),HIGH_PRIORITY_CLASS);
        // break;

        // default:
        // break;
        // }
    }

    // TODO
    // 경고 출력할때만 사용;
    // show_warning
    // 일단 보류
    // static void OutputString(Bitu x,Bitu y,const char * text,Bit32u color,Bit32u
    // color2,SDL_Surface * output_surface) {
    //
    public void startUpGUI(Section sec) throws WrongType {
        sec.addDestroyFunction(this::shutdownGUI);
        SectionProperty section = (SectionProperty) sec;
        active = false;
        updating = false;

        // 아이콘 설정부분 생략

        _devFullScreen = section.getBool("fullscreen");
        _waitOnError = section.getBool("waitonerror");

        PropertyMultival p = section.getMultival("priority");
        String focus = p.getSection().getString("active");
        String notfocus = p.getSection().getString("inactive");

        if (focus == "lowest") {
            _priorityFocus = PriorityLevels.Lowest;
        } else if (focus == "lower") {
            _priorityFocus = PriorityLevels.Lower;
        } else if (focus == "normal") {
            _priorityFocus = PriorityLevels.Normal;
        } else if (focus == "higher") {
            _priorityFocus = PriorityLevels.Higher;
        } else if (focus == "highest") {
            _priorityFocus = PriorityLevels.Highest;
        }

        if (notfocus == "lowest") {
            _priorityNoFocus = PriorityLevels.Lowest;
        } else if (notfocus == "lower") {
            _priorityNoFocus = PriorityLevels.Lower;
        } else if (notfocus == "normal") {
            _priorityNoFocus = PriorityLevels.Normal;
        } else if (notfocus == "higher") {
            _priorityNoFocus = PriorityLevels.Higher;
        } else if (notfocus == "highest") {
            _priorityNoFocus = PriorityLevels.Highest;
        } else if (notfocus == "pause") {
            /*
             * we only check for pause here, because it makes no sense for DOSBox to be paused while
             * it has focus
             */
            _priorityNoFocus = PriorityLevels.Pause;
        }

        setPriority(_priorityFocus); // Assume focus on startup
        _mouseLocked = false;
        mouselocked = false; // Global for mapper
        _mouseRequestLock = false;
        _devFullFixed = false;


        String fullresolution = section.getString("fullresolution");
        _devFullWidth = 0;
        _devFullHeight = 0;
        if (fullresolution != null && !fullresolution.isEmpty()) {
            if (!fullresolution.toLowerCase().startsWith("original")) {
                _devFullFixed = true;
                if (fullresolution.indexOf('x') > 0) {
                    String[] res = fullresolution.split("x");
                    _devFullHeight = Short.parseShort(res[1]);
                    _devFullWidth = Short.parseShort(res[0]);
                }
            }
        }

        _devWindowWidth = 0;
        _devWindowHeight = 0;
        String windowresolution = section.getString("windowresolution");
        if (windowresolution != null && !windowresolution.isEmpty()) {
            if (!windowresolution.toLowerCase().startsWith("original")) {
                if (windowresolution.indexOf('x') > 0) {
                    String[] res = windowresolution.split("x");
                    _devWindowHeight = Short.parseShort(res[1]);
                    _devWindowWidth = Short.parseShort(res[0]);
                }
            }
        }
        _devDoubleBuf = section.getBool("fulldouble");// 일단 더블 버퍼링을 사용하지 않는다.
        if (_devFullWidth == 0 || _devFullWidth == 0) {

            Rectangle rect = GraphicsEnvironment.getLocalGraphicsEnvironment()
                    .getDefaultScreenDevice().getDefaultConfiguration().getBounds();
            _devFullHeight = rect.height;
            _devFullWidth = rect.width;

            // dev_full_width=1024;
            // dev_full_height=768;

        }

        _mouseAutoEnable = section.getBool("autolock");
        // if (!mouse_autoenable) SDL_ShowCursor(SDL_DISABLE);
        _mouseAutoLock = false;
        _mouseSensitivity = section.getInt("sensitivity");
        String output = section.getString("output");

        /* Setup Mouse correctly if fullscreen */
        if (_devFullScreen)
            captureMouse();


        /* Initialize screen for first time */
        // BufferedImage.TYPE_INT_ARGB);
        surface.newImage(DEFAULT_WIDTH_PIXEL, DEFAULT_HEIGHT_PIXEL, BufferedImage.TYPE_INT_ARGB);
        _pixels = surface.getIntPixels();
        _pitch = getPitch(DEFAULT_WIDTH_PIXEL);

        _devBpp = surface.getPixelSize();
        if (_devBpp == 24) {
            // LOG_MSG("SDL:You are running in 24 bpp mode, this will slow down things!");
        }
        stop();
        // SDL_WM_SetCaption("DOSBox",VERSION);

        // ARGB
        int rmask = 0x0000FF00;
        int gmask = 0x00FF0000;
        int bmask = 0xFF000000;


        /* Get some Event handlers */
        GUIPlatform.mapper.addKeyHandler(this::killSwitch, MapKeys.F9, Mapper.MMOD1, "shutdown",
                "Shutdown");
        // GUIInstance.Mapper.MAPPER_AddHandler(CaptureMouse,MapKeys.MK_f10,Mapper.MMOD1,"capmouse","Cap
        // Mouse");
        GUIPlatform.mapper.addKeyHandler(this::switchFullScreen, MapKeys.Return, Mapper.MMOD2,
                "fullscr", "Fullscreen");
        GUIPlatform.mapper.addKeyHandler(this::pauseDOSBox, MapKeys.Pause, Mapper.MMOD2, "pause",
                "Pause");
        /* Get Keyboard state of numlock and capslock */

        // SDLMod keystate = SDL_GetModState();
        // if(keystate&KMOD_NUM) startup_state_numlock = true;
        // if(keystate&KMOD_CAPS) startup_state_capslock = true;
    }

    // void Mouse_AutoLock(boolean enable)

    public void losingFocus() {
        // laltstate = SDL_KEYUP;
        // raltstate = SDL_KEYUP;
        GUIPlatform.mapper.losingFocusKBD();
    }

    /*
     * static variable to show wether there is not a valid stdout. Fixes some bugs when -noconsole
     * is used in a read only directory
     */
    private boolean _noStdOut = false;

    // TODO have to implement
    private void showMsg(String format, Object... args) {
        // char buf[512];
        // va_list msg;
        // va_start(msg,format);
        // vsprintf(buf,format,msg);
        // strcat(buf,"\n");
        // va_end(msg);
        // if(!no_stdout) printf("%s",buf); //Else buf is parsed again.
    }


    // 임시로 팝업창으로 구현
    private void showWarning(String message) {
        JOptionPane.showMessageDialog(_main, message);
    }



    // public void GetSize(ref int width, ref int height, ref boolean fullscreen)
    public int getWidth() {
        return _drawWidth;
    }

    public int getHeight() {
        return _drawHeight;
    }

    public boolean isFullScreen() {
        return _devFullScreen;
    }

    /*--------------------------- begin Mapper -----------------------------*/
    private boolean usescancodes;



    @Override
    public <T> void checkEvent(T inputEvent) {
        // TODO have to implement
    }

    @Override
    public void addKeyHandler(GUIKeyHandler handler, MapKeys key, int mods, String eventname,
            String buttonname) {
    }

    @Override
    public void init() {
    }

    @Override
    public void startup(Section sec) {
    }

    @Override
    public void run(boolean pressed) {
        if (pressed)
            return;
        Keyboard.instance().clrBuffer(); // Clear buffer
        GUIPlatform.gfx.losingFocus(); // Release any keys pressed (buffer gets filled again).
        runInternal();
    }

    @Override
    public void runInternal() {
    }

    @Override
    public void losingFocusKBD() {
    }


    /*--------------------------- end Mapper -----------------------------*/


    public void MouseAutoLock(boolean enable) {
        _mouseAutoLock = enable;
        if (_mouseAutoEnable)
            _mouseRequestLock = enable;
        else {
            if (enable)
                showCursor();
            else
                hideCursor();
            _mouseRequestLock = false;
        }
    }


}
