package org.gutkyu.dosboxj.hardware.video.memory.paging;

import org.gutkyu.dosboxj.hardware.memory.paging.*;
import org.gutkyu.dosboxj.hardware.video.*;

public final class TandyPageHandler extends PageHandler {
    private VGA vga;

    public TandyPageHandler(VGA vga) {
        this.vga = vga;
        Flags = Paging.PFLAG_READABLE | Paging.PFLAG_WRITEABLE;
        // |PAGING.PFLAG_NOCODE;
    }

    @Override
    public byte[] getHostMemory() {
        return vga.Tandy.MemAlloc;
    }

    @Override
    public int getHostReadPt(int physPage) {
        if ((vga.Tandy.MemBank & 1) != 0)
            physPage &= 0x03;
        else
            physPage &= 0x07;
        return vga.Tandy.MemBase + (physPage * 4096);
    }

    @Override
    public int getHostWritePt(int physPage) {
        return getHostReadPt(physPage);
    }
}
