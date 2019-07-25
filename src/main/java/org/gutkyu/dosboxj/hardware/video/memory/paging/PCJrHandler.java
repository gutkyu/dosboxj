package org.gutkyu.dosboxj.hardware.video.memory.paging;


import org.gutkyu.dosboxj.hardware.memory.paging.*;
import org.gutkyu.dosboxj.hardware.video.*;

public final class PCJrHandler extends PageHandler {
    private VGA vga;

    public PCJrHandler(VGA vga) {
        this.vga = vga;
        Flags = Paging.PFLAG_READABLE | Paging.PFLAG_WRITEABLE;
    }

    @Override
    public byte[] getHostMemory() {
        return vga.Tandy.MemAlloc;
    }

    @Override
    public int getHostReadPt(int physPage) {
        physPage -= 0xb8;
        // test for a unaliged bank, then replicate 2x16kb
        if ((vga.Tandy.MemBank & 1) != 0)
            physPage &= 0x03;
        return vga.Tandy.MemBase + (physPage * 4096);
    }

    @Override
    public int getHostWritePt(int physPage) {
        return getHostReadPt(physPage);
    }
}
