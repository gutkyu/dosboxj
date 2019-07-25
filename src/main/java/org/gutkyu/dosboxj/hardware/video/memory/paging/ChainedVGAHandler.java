package org.gutkyu.dosboxj.hardware.video.memory.paging;


import org.gutkyu.dosboxj.hardware.memory.*;
import org.gutkyu.dosboxj.hardware.memory.paging.*;
import org.gutkyu.dosboxj.hardware.video.*;


public class ChainedVGAHandler extends PageHandler {
    private VGA vga;

    public ChainedVGAHandler(VGA vga) {
        Flags = Paging.PFLAG_NOCODE;
        this.vga = vga;
    }

    private byte readBHandler(int addr) {
        return vga.Mem.LinearAlloc[((addr & ~3) << 2) + (addr & 3)];
    }

    private int readWHandler(int addr) {
        return Memory.hostReadW(vga.Mem.LinearAlloc, (addr & ~3) << 2) + (addr & 3);
    }

    private int readDHandler(int addr) {
        return Memory.hostReadD(vga.Mem.LinearAlloc, (addr & ~3) << 2) + (addr & 3);
    }

    private void writeBCache(int addr, int val) {
        vga.FastMemAlloc[addr] = (byte) val;
        if (addr < 320) {
            // And replicate the first line
            vga.FastMemAlloc[addr + 64 * 1024] = (byte) val;
        }
    }

    private void writeWCache(int addr, int val) {
        Memory.hostWriteW(vga.FastMemAlloc, addr, val);
        if (addr < 320) {
            // And replicate the first line
            Memory.hostWriteW(vga.FastMemAlloc, addr + 64 * 1024, val);
        }
    }

    private void writeDCache(int addr, int val) {
        Memory.hostWriteD(vga.FastMemAlloc, addr, val);
        if (addr < 320) {
            // And replicate the first line
            Memory.hostWriteD(vga.FastMemAlloc, addr + 64 * 1024, val);
        }
    }

    private void writeBHandler(int addr, int val) {
        // No need to check for compatible chains here, this one is only enabled if that bit is set
        vga.Mem.LinearAlloc[((addr & ~3) << 2) + (addr & 3)] = (byte) val;
    }

    private void writeDHandler(int addr, int val) {
        // No need to check for compatible chains here, this one is only enabled if that bit is set
        Memory.hostWriteD(vga.Mem.LinearAlloc, ((addr & ~3) << 2) + (addr & 3), val);
    }

    private void writeWHandler(int addr, int val) {
        // No need to check for compatible chains here, this one is only enabled if that bit is set
        Memory.hostWriteW(vga.Mem.LinearAlloc, ((addr & ~3) << 2) + (addr & 3), val);
    }

    @Override
    public int readB(int addr) {
        addr = Paging.getPhysicalAddress(addr) & vga.PageMask;
        addr += vga.SVGA.BankFeadFull;
        addr = vga.checked(addr);
        return readBHandler(addr) & 0xff;
    }

    @Override
    public int readW(int addr) {
        addr = Paging.getPhysicalAddress(addr) & vga.PageMask;
        addr += vga.SVGA.BankFeadFull;
        addr = vga.checked(addr);
        if ((addr & 1) != 0)
            return ((readBHandler(addr + 0) & 0xff) << 0) | ((readBHandler(addr + 1) & 0xff) << 8);
        else
            return readWHandler(addr);
    }

    @Override
    public long readD(int addr) {
        addr = Paging.getPhysicalAddress(addr) & vga.PageMask;
        addr += vga.SVGA.BankFeadFull;
        addr = vga.checked(addr);
        if ((addr & 3) != 0)
            return ((readBHandler(addr + 0) & 0xff) << 0) | ((readBHandler(addr + 1) & 0xff) << 8)
                    | ((readBHandler(addr + 2) & 0xff) << 16)
                    | ((readBHandler(addr + 3) & 0xff) << 24);
        else
            return readDHandler(addr);
    }

    @Override
    public void writeB(int addr, int val) {
        addr = Paging.getPhysicalAddress(addr) & vga.PageMask;
        addr += vga.SVGA.BankWriteFull;
        addr = vga.checked(addr);
        /*
         * #if VGA_KEEP_CHANGES MEM_CHANGED(addr); #endif
         */
        writeBHandler(addr, val);
        writeBCache(addr, val);
    }

    @Override
    public void writeW(int addr, int val) {
        addr = Paging.getPhysicalAddress(addr) & vga.PageMask;
        addr += vga.SVGA.BankWriteFull;
        addr = vga.checked(addr);
        /*
         * #if VGA_KEEP_CHANGES MEM_CHANGED(addr); #endif
         */
        // MEM_CHANGED( addr + 1);
        if ((addr & 1) != 0) {
            writeBHandler(addr + 0, val >>> 0);
            writeBHandler(addr + 1, val >>> 8);
        } else {
            writeWHandler(addr, val);
        }
        writeWCache(addr, val);
    }

    @Override
    public void writeD(int addr, int val) {
        addr = Paging.getPhysicalAddress(addr) & vga.PageMask;
        addr += vga.SVGA.BankWriteFull;
        addr = vga.checked(addr);
        /*
         * #if VGA_KEEP_CHANGES MEM_CHANGED(addr); #endif
         */
        // MEM_CHANGED( addr + 3);
        if ((addr & 3) != 0) {
            writeBHandler(addr + 0, val >>> 0);
            writeBHandler(addr + 1, val >>> 8);
            writeBHandler(addr + 2, val >>> 16);
            writeBHandler(addr + 3, val >>> 24);
        } else {
            writeDHandler(addr, val);
        }
        writeDCache(addr, val);
    }
}
