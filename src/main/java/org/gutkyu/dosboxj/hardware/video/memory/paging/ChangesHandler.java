package org.gutkyu.dosboxj.hardware.video.memory.paging;


import org.gutkyu.dosboxj.hardware.memory.*;
import org.gutkyu.dosboxj.hardware.memory.paging.*;
import org.gutkyu.dosboxj.hardware.video.*;


public final class ChangesHandler extends PageHandler {
    private VGA _vga;

    public ChangesHandler(VGA vga) {
        _vga = vga;
        Flags = Paging.PFLAG_NOCODE;
    }

    @Override
    public int readB(int addr) {
        addr = Paging.getPhysicalAddress(addr) & _vga.PageMask;
        addr += _vga.SVGA.BankFeadFull;
        addr = _vga.checked(addr);
        return _vga.Mem.LinearAlloc[addr];
    }

    @Override
    public int readW(int addr) {
        addr = Paging.getPhysicalAddress(addr) & _vga.PageMask;
        addr += _vga.SVGA.BankFeadFull;
        addr = _vga.checked(addr);
        return Memory.hostReadW(_vga.Mem.LinearAlloc, addr);
    }

    @Override
    public long readD(int addr) {
        addr = Paging.getPhysicalAddress(addr) & _vga.PageMask;
        addr += _vga.SVGA.BankFeadFull;
        addr = _vga.checked(addr);
        return Memory.hostReadD(_vga.Mem.LinearAlloc, addr);
    }

    @Override
    public void writeB(int addr, int val) {
        addr = Paging.getPhysicalAddress(addr) & _vga.PageMask;
        addr += _vga.SVGA.BankWriteFull;
        addr = _vga.checked(addr);
        /*
         * #if VGA_KEEP_CHANGES MEM_CHANGED(addr); #endif
         */
        _vga.Mem.LinearAlloc[addr] = (byte) val;
    }

    @Override
    public void writeW(int addr, int val) {
        addr = Paging.getPhysicalAddress(addr) & _vga.PageMask;
        addr += _vga.SVGA.BankWriteFull;
        addr = _vga.checked(addr);
        /*
         * #if VGA_KEEP_CHANGES MEM_CHANGED(addr); #endif
         */
        Memory.hostWriteW(_vga.Mem.LinearAlloc, addr, val);
    }

    @Override
    public void writeD(int addr, int val) {
        addr = Paging.getPhysicalAddress(addr) & _vga.PageMask;
        addr += _vga.SVGA.BankWriteFull;
        addr = _vga.checked(addr);
        /*
         * #if VGA_KEEP_CHANGES MEM_CHANGED(addr); #endif
         */
        Memory.hostWriteD(_vga.Mem.LinearAlloc, addr, val);
    }
}
