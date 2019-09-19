package org.gutkyu.dosboxj.hardware.video.memory.paging;


import org.gutkyu.dosboxj.hardware.memory.*;
import org.gutkyu.dosboxj.hardware.memory.paging.*;
import org.gutkyu.dosboxj.hardware.video.*;


public final class LFBChangesHandler extends PageHandler {
    private VGA vga;

    public LFBChangesHandler(VGA vga) {
        Flags = Paging.PFLAG_NOCODE;
        this.vga = vga;
    }

    @Override
    public int readB(int addr) {
        addr = Paging.getPhysicalAddress(addr) - vga.Lfb.Addr;
        addr = vga.checked(addr);
        // return MEMORY.host_readb(ref vga.mem.linear_orgptr, vga.mem.linear + addr);
        return 0xff & vga.Mem.LinearAlloc[vga.Mem.LinearBase + addr];
    }

    @Override
    public int readW(int addr) {
        addr = Paging.getPhysicalAddress(addr) - vga.Lfb.Addr;
        addr = vga.checked(addr);
        return Memory.hostReadW(vga.Mem.LinearAlloc, vga.Mem.LinearBase + addr);
    }

    @Override
    public long readD(int addr) {
        addr = Paging.getPhysicalAddress(addr) - vga.Lfb.Addr;
        addr = vga.checked(addr);
        return 0xffffffffL & Memory.hostReadD(vga.Mem.LinearAlloc, vga.Mem.LinearBase + addr);
    }

    @Override
    public void writeB(int addr, int val) {
        addr = Paging.getPhysicalAddress(addr) - vga.Lfb.Addr;
        addr = vga.checked(addr);
        Memory.hostWriteB(vga.Mem.LinearAlloc, vga.Mem.LinearBase + addr, val);
        /*
         * #if VGA_KEEP_CHANGES MEM_CHANGED(addr); #endif
         */
    }

    @Override
    public void writeW(int addr, int val) {
        addr = Paging.getPhysicalAddress(addr) - vga.Lfb.Addr;
        addr = vga.checked(addr);
        Memory.hostWriteW(vga.Mem.LinearAlloc, vga.Mem.LinearBase + addr, val);
        /*
         * #if VGA_KEEP_CHANGES MEM_CHANGED(addr); #endif
         */
    }

    @Override
    public void writeD(int addr, int val) {
        addr = Paging.getPhysicalAddress(addr) - vga.Lfb.Addr;
        addr = vga.checked(addr);
        Memory.hostWriteD(vga.Mem.LinearAlloc, vga.Mem.LinearBase + addr, val);
        /*
         * #if VGA_KEEP_CHANGES MEM_CHANGED(addr); #endif
         */
    }
}
