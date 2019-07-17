package org.gutkyu.dosboxj.hardware.video.svga;


@FunctionalInterface
public interface FuncFinishSetMode {
    void exec(int crtcBase, ModeExtraData modeData);
}
