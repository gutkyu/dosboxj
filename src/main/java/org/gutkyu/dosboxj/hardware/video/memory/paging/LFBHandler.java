package org.gutkyu.dosboxj.hardware.video.memory.paging;


import org.gutkyu.dosboxj.hardware.memory.paging.*;
import org.gutkyu.dosboxj.hardware.video.*;

public final class LFBHandler extends PageHandler {
    private VGA vga;

    public LFBHandler(VGA vga) {
        this.vga = vga;
        Flags = Paging.PFLAG_READABLE | Paging.PFLAG_WRITEABLE | Paging.PFLAG_NOCODE;
    }

    // private static byte[] _alloc = vga.mem.linear;//비디오 메모리로 할당된 배열; 실행도중 변경되지 않음
    @Override
    public byte[] getHostMemory() {
        return vga.Mem.LinearAlloc;
    }

    @Override
    public int getHostReadPt(int physPage) {
        physPage -= vga.Lfb.Page;
        // addresse = vga.mem.linear + CHECKED3(phys_page * 4096);
        return vga.Mem.LinearBase + (physPage * 4096) & ((vga.VMemWrap >>>2) - 1);
    }

    @Override
    public int getHostWritePt(int physPage) {
        return getHostReadPt(physPage);
    }
}
