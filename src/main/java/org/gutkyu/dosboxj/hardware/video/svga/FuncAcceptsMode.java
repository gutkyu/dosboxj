package org.gutkyu.dosboxj.hardware.video.svga;


@FunctionalInterface
public interface FuncAcceptsMode {
    boolean exec(int modeNo);
}
