package org.gutkyu.dosboxj.hardware.video.memory.paging;


import org.gutkyu.dosboxj.hardware.memory.*;
import org.gutkyu.dosboxj.hardware.memory.paging.*;
import org.gutkyu.dosboxj.hardware.video.*;


public final class ChangesHandler extends PageHandler {
    private VGA vga;

    public ChangesHandler(VGA vga) {
        this.vga = vga;
        Flags = Paging.PFLAG_NOCODE;
    }

    @Override
    public int readB(int addr) {
        addr = Paging.getPhysicalAddress(addr) & vga.PageMask;
        addr += vga.SVGA.BankFeadFull;
        addr = vga.checked(addr);
        return 0xff & vga.Mem.LinearAlloc[vga.Mem.LinearBase + addr];
    }

    @Override
    public int readW(int addr) {
        addr = Paging.getPhysicalAddress(addr) & vga.PageMask;
        addr += vga.SVGA.BankFeadFull;
        addr = vga.checked(addr);
        return Memory.hostReadW(vga.Mem.LinearAlloc, vga.Mem.LinearBase + addr);
    }

    @Override
    public long readD(int addr) {
        addr = Paging.getPhysicalAddress(addr) & vga.PageMask;
        addr += vga.SVGA.BankFeadFull;
        addr = vga.checked(addr);
        return 0xffffffffL & Memory.hostReadD(vga.Mem.LinearAlloc, vga.Mem.LinearBase + addr);
    }

    @Override
    public void writeB(int addr, int val) {
        addr = Paging.getPhysicalAddress(addr) & vga.PageMask;
        addr += vga.SVGA.BankWriteFull;
        addr = vga.checked(addr);
        /*
         * #if VGA_KEEP_CHANGES MEM_CHANGED(addr); #endif
         */
        vga.Mem.LinearAlloc[vga.Mem.LinearBase + addr] = (byte) val;
    }

    @Override
    public void writeW(int addr, int val) {
        addr = Paging.getPhysicalAddress(addr) & vga.PageMask;
        addr += vga.SVGA.BankWriteFull;
        addr = vga.checked(addr);
        /*
         * #if VGA_KEEP_CHANGES MEM_CHANGED(addr); #endif
         */
        Memory.hostWriteW(vga.Mem.LinearAlloc, vga.Mem.LinearBase + addr, val);
    }

    @Override
    public void writeD(int addr, int val) {
        addr = Paging.getPhysicalAddress(addr) & vga.PageMask;
        addr += vga.SVGA.BankWriteFull;
        addr = vga.checked(addr);
        /*
         * #if VGA_KEEP_CHANGES MEM_CHANGED(addr); #endif
         */
        Memory.hostWriteD(vga.Mem.LinearAlloc, vga.Mem.LinearBase + addr, val);
    }
}
