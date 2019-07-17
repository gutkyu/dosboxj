package org.gutkyu.dosboxj.hardware.video.svga;


@FunctionalInterface
public interface FuncSetClock {
    void exec(int which, int target);
}
