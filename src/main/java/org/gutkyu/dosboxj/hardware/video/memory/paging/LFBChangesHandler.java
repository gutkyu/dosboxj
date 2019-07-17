package org.gutkyu.dosboxj.hardware.video.memory.paging;


import org.gutkyu.dosboxj.hardware.memory.*;
import org.gutkyu.dosboxj.hardware.memory.paging.*;
import org.gutkyu.dosboxj.hardware.video.*;


public final class LFBChangesHandler extends PageHandler {
    private VGA _vga;

    public LFBChangesHandler(VGA vga) {
        Flags = Paging.PFLAG_NOCODE;
        _vga = vga;
    }

    @Override
    public int readB(int addr) {
        addr = Paging.getPhysicalAddress(addr) - _vga.Lfb.Addr;
        addr = _vga.checked(addr);
        // return MEMORY.host_readb(ref vga.mem.linear_orgptr, vga.mem.linear + addr);
        return _vga.Mem.LinearAlloc[addr];
    }

    @Override
    public int readW(int addr) {
        addr = Paging.getPhysicalAddress(addr) - _vga.Lfb.Addr;
        addr = _vga.checked(addr);
        return Memory.hostReadW(_vga.Mem.LinearAlloc, addr);
    }

    @Override
    public long readD(int addr) {
        addr = Paging.getPhysicalAddress(addr) - _vga.Lfb.Addr;
        addr = _vga.checked(addr);
        return Memory.hostReadD(_vga.Mem.LinearAlloc, addr);
    }

    @Override
    public void writeB(int addr, int val) {
        addr = Paging.getPhysicalAddress(addr) - _vga.Lfb.Addr;
        addr = _vga.checked(addr);
        Memory.hostWriteB(_vga.Mem.LinearAlloc, addr, val);
        /*
         * #if VGA_KEEP_CHANGES MEM_CHANGED(addr); #endif
         */
    }

    @Override
    public void writeW(int addr, int val) {
        addr = Paging.getPhysicalAddress(addr) - _vga.Lfb.Addr;
        addr = _vga.checked(addr);
        Memory.hostWriteW(_vga.Mem.LinearAlloc, addr, val);
        /*
         * #if VGA_KEEP_CHANGES MEM_CHANGED(addr); #endif
         */
    }

    @Override
    public void writeD(int addr, int val) {
        addr = Paging.getPhysicalAddress(addr) - _vga.Lfb.Addr;
        addr = _vga.checked(addr);
        Memory.hostWriteD(_vga.Mem.LinearAlloc, addr, val);
        /*
         * #if VGA_KEEP_CHANGES MEM_CHANGED(addr); #endif
         */
    }
}
