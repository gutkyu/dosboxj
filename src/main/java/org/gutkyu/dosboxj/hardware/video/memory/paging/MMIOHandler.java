package org.gutkyu.dosboxj.hardware.video.memory.paging;


import org.gutkyu.dosboxj.hardware.memory.paging.*;
import org.gutkyu.dosboxj.hardware.video.*;


public final class MMIOHandler extends PageHandler {
    private VGA vga;

    public MMIOHandler(VGA vga) {
        this.vga = vga;
        Flags = Paging.PFLAG_NOCODE;
    }

    @Override
    public void writeB(int addr, int val) {
        int port = Paging.getPhysicalAddress(addr) & 0xffff;
        vga.XGA.write(port, val, 1);
    }

    @Override
    public void writeW(int addr, int val) {
        int port = Paging.getPhysicalAddress(addr) & 0xffff;
        vga.XGA.write(port, val, 2);
    }

    @Override
    public void writeD(int addr, int val) {
        int port = Paging.getPhysicalAddress(addr) & 0xffff;
        vga.XGA.write(port, val, 4);
    }

    @Override
    public int readB(int addr) {
        int port = Paging.getPhysicalAddress(addr) & 0xffff;
        return vga.XGA.read(port, 1);
    }

    @Override
    public int readW(int addr) {
        int port = Paging.getPhysicalAddress(addr) & 0xffff;
        return vga.XGA.read(port, 2);
    }

    @Override
    public long readD(int addr) {
        int port = Paging.getPhysicalAddress(addr) & 0xffff;
        return vga.XGA.read(port, 4);
    }
}
