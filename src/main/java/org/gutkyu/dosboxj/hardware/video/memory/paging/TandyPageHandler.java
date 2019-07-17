package org.gutkyu.dosboxj.hardware.video.memory.paging;

import org.gutkyu.dosboxj.hardware.memory.paging.*;
import org.gutkyu.dosboxj.hardware.video.*;

public final class TandyPageHandler extends PageHandler {
    private VGA _vga;

    public TandyPageHandler(VGA vga) {
        _vga = vga;
        Flags = Paging.PFLAG_READABLE | Paging.PFLAG_WRITEABLE;
        // |PAGING.PFLAG_NOCODE;
    }

    @Override
    public byte[] getHostMemory() {
        return _vga.Tandy.MemAlloc;
    }

    @Override
    public int getHostReadPt(int physPage) {
        if ((_vga.Tandy.MemBank & 1) != 0)
            physPage &= 0x03;
        else
            physPage &= 0x07;
        return _vga.Tandy.MemBase + (physPage * 4096);
    }

    @Override
    public int getHostWritePt(int physPage) {
        return getHostReadPt(physPage);
    }
}
