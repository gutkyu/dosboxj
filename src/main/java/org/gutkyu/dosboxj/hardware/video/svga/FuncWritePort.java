package org.gutkyu.dosboxj.hardware.video.svga;

@FunctionalInterface
public interface FuncWritePort {
    void exec(int reg, int val, int iolen);
}
