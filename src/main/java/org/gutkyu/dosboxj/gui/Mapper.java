package org.gutkyu.dosboxj.gui;

import org.gutkyu.dosboxj.DOSBox;
import org.gutkyu.dosboxj.misc.setup.*;

public abstract class Mapper {

    public static final byte MMOD1 = 0x1;
    public static final byte MMOD2 = 0x2;
    public static final String MAPPERFILE = "mapper-" + DOSBox.VERSION + ".map"; // 상수처럼 사용

    public abstract void addKeyHandler(GUIKeyHandler handler, MapKeys key, int mods,
            String eventname, String buttonname);

    public abstract void init();

    public abstract void startup(Section sec);

    public abstract void run(boolean pressed);

    public abstract void runInternal();

    public abstract void losingFocusKBD();

    public abstract <T> void checkEvent(T inputEvent);

}
