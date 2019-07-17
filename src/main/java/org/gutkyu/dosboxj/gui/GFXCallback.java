package org.gutkyu.dosboxj.gui;


public interface GFXCallback {
    public enum GFXCallbackType {
        Reset, Stop, Redraw
    }

    void call(GFXCallbackType function);
}
