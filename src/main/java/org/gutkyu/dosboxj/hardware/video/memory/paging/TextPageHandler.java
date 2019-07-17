package org.gutkyu.dosboxj.hardware.video.memory.paging;


import org.gutkyu.dosboxj.hardware.memory.paging.*;
import org.gutkyu.dosboxj.hardware.video.*;

public final class TextPageHandler extends PageHandler {
    private VGA _vga;

    public TextPageHandler(VGA vga) {
        _vga = vga;
        Flags = Paging.PFLAG_NOCODE;
    }

    @Override
    public int readB(int addr) {
        addr = Paging.getPhysicalAddress(addr) & _vga.PageMask;
        return 0xff & _vga.Draw.Font[addr];
    }

    @Override
    public void writeB(int addr, int val) {
        addr = Paging.getPhysicalAddress(addr) & _vga.PageMask;
        if ((_vga.Seq.MapMask & 0x4) != 0) {
            _vga.Draw.Font[addr] = (byte) val;
        }
    }
}
