package org.gutkyu.dosboxj.gui;



public interface IGFX {


    // struct GFX_PalEntry {
    // byte r;
    // byte g;
    // byte b;
    // byte unused;
    // };

    void GFXEvents();

    // public abstract void GFX_SetPalette(int start,int count,GFX_PalEntry entries);
    int getBestMode(int flags);

    int getRGB(int red, int green, int blue);

    void setTitle(int cycles, int frameskip, boolean paused);

    int setSize(int width, int height, int flags, double scalex, double scaley, GFXCallback cb);

    void resetScreen();

    void start();

    void stop();

    void switchFullScreen();

    // StartUpdate 함수는 단순히 작업 유효성만 체크
    // 성공하면 getPixels, getCurrentPixelIndex, getPitch 함수 순차실행
    boolean startUpdate();

    int[] getPixels();

    long getCurrentPixelIndex();

    int getPitch();

    void endUpdate(short[] changedLines);

    // void GetSize(ref int width, ref int height, ref boolean fullscreen);
    int getWidth();

    int getHeight();

    boolean isFullScreen();

    void losingFocus();

    // #if defined (WIN32)
    // boolean GFX_SDLUsingWinDIB();
    // #endif

    // #if defined (REDUCE_JOYSTICK_POLLING)
    // void MAPPER_UpdateJoysticks();
    // #endif

    /* Mouse related */
    void captureMouse();
}
