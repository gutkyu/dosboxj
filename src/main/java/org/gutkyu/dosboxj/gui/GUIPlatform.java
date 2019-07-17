package org.gutkyu.dosboxj.gui;

import org.gutkyu.dosboxj.gui.java2d.JavaGFX;
import org.gutkyu.dosboxj.misc.setup.*;
import org.gutkyu.dosboxj.util.DOSAction;
import org.gutkyu.dosboxj.util.DOSAction1;

public final class GUIPlatform {

    public static Mapper mapper = null;

    public static IGFX gfx = null;

    private static DOSAction shutdownGUIWrap = null;
    private static DOSAction1<Section> startupGUIWrap = null;

    public static void setup() {
        JavaGFX javaGFX = new JavaGFX();
        mapper = javaGFX;
        gfx = javaGFX;
        // shutdownGUI = javaGFX ::Close;
        startupGUIWrap = javaGFX::startUpGUI;
        // javaGFX.show();
    }

    public static void shutdown() {
        // shutdownGUI.run();
    }

    public static void startupGUI(Section sec) {
        startupGUIWrap.run(sec);
    }

}
