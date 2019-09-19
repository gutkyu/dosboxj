package org.gutkyu.dosboxj.hardware.video.memory.paging;

import org.gutkyu.dosboxj.hardware.memory.paging.*;
import org.gutkyu.dosboxj.hardware.video.*;


public final class LIN4Handler extends UnchainedEGAHandler {
    VGA vga;

    public LIN4Handler(VGA vga) {
        super(vga);
        this.vga = vga;
        Flags = Paging.PFLAG_NOCODE;
    }

    @Override
    public void writeB(int addr, int val) {
        addr = vga.SVGA.BankWriteFull + (Paging.getPhysicalAddress(addr) & 0xffff);
        // addr = CHECKED4(addr);
        addr = ((addr) & ((vga.VMemWrap >>> 2) - 1));
        // MEM_CHANGED(addr << 3);
        writeHandler(addr + 0, 0xff & (val >>> 0));
    }

    @Override
    public void writeW(int addr, int val) {
        addr = vga.SVGA.BankWriteFull + (Paging.getPhysicalAddress(addr) & 0xffff);
        // addr = CHECKED4(addr);
        addr = ((addr) & ((vga.VMemWrap >>> 2) - 1));
        // MEM_CHANGED(addr << 3);
        writeHandler(addr + 0, 0xff & (val >>> 0));
        writeHandler(addr + 1, 0xff & (val >>> 8));
    }

    @Override
    public void writeD(int addr, int val) {
        addr = vga.SVGA.BankWriteFull + (Paging.getPhysicalAddress(addr) & 0xffff);
        // addr = CHECKED4(addr);
        addr = ((addr) & ((vga.VMemWrap >>> 2) - 1));
        // MEM_CHANGED(addr << 3);
        writeHandler(addr + 0, 0xff & (val >>> 0));
        writeHandler(addr + 1, 0xff & (val >>> 8));
        writeHandler(addr + 2, 0xff & (val >>> 16));
        writeHandler(addr + 3, 0xff & (val >>> 24));
    }

    @Override
    public int readB(int addr) {
        addr = vga.SVGA.BankFeadFull + (Paging.getPhysicalAddress(addr) & 0xffff);
        // addr = CHECKED4(addr);
        addr = ((addr) & ((vga.VMemWrap >>> 2) - 1));
        return readBHandler(addr);
    }

    @Override
    public int readW(int addr) {
        addr = vga.SVGA.BankFeadFull + (Paging.getPhysicalAddress(addr) & 0xffff);
        // addr = CHECKED4(addr);
        addr = ((addr) & ((vga.VMemWrap >>> 2) - 1));
        return (readBHandler(addr + 0) << 0) | (readBHandler(addr + 1) << 8);
    }

    @Override
    public long readD(int addr) {
        addr = vga.SVGA.BankFeadFull + (Paging.getPhysicalAddress(addr) & 0xffff);
        // addr = CHECKED4(addr);
        addr = ((addr) & ((vga.VMemWrap >>> 2) - 1));
        return 0xffffffffL & ((readBHandler(addr + 0) << 0) | (readBHandler(addr + 1) << 8)
                | (readBHandler(addr + 2) << 16) | (readBHandler(addr + 3) << 24));
    }
}
