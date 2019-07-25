package org.gutkyu.dosboxj.hardware.video.memory.paging;


import org.gutkyu.dosboxj.hardware.memory.paging.*;
import org.gutkyu.dosboxj.hardware.video.*;

public final class TextPageHandler extends PageHandler {
    private VGA vga;

    public TextPageHandler(VGA vga) {
        this.vga = vga;
        Flags = Paging.PFLAG_NOCODE;
    }

    @Override
    public int readB(int addr) {
        addr = Paging.getPhysicalAddress(addr) & vga.PageMask;
        return 0xff & vga.Draw.Font[addr];
    }

    @Override
    public void writeB(int addr, int val) {
        addr = Paging.getPhysicalAddress(addr) & vga.PageMask;
        if ((vga.Seq.MapMask & 0x4) != 0) {
            vga.Draw.Font[addr] = (byte) val;
        }
    }
}
