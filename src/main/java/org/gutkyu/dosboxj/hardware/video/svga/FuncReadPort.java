package org.gutkyu.dosboxj.hardware.video.svga;

@FunctionalInterface
public interface FuncReadPort {
    int exec(int reg, int iolen);
}
