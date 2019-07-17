package org.gutkyu.dosboxj.hardware.video.memory.paging;


import org.gutkyu.dosboxj.hardware.memory.*;
import org.gutkyu.dosboxj.hardware.memory.paging.*;
import org.gutkyu.dosboxj.hardware.video.*;


public class ChainedVGAHandler extends PageHandler {
    private VGA _vga;

    public ChainedVGAHandler(VGA vga) {
        Flags = Paging.PFLAG_NOCODE;
        _vga = vga;
    }

    private byte readBHandler(int addr) {
        return _vga.Mem.LinearAlloc[((addr & ~3) << 2) + (addr & 3)];
    }

    private int readWHandler(int addr) {
        return Memory.hostReadW(_vga.Mem.LinearAlloc, (addr & ~3) << 2) + (addr & 3);
    }

    private int readDHandler(int addr) {
        return Memory.hostReadD(_vga.Mem.LinearAlloc, (addr & ~3) << 2) + (addr & 3);
    }

    private void writeBCache(int addr, int val) {
        _vga.FastMemAlloc[addr] = (byte) val;
        if (addr < 320) {
            // And replicate the first line
            _vga.FastMemAlloc[addr + 64 * 1024] = (byte) val;
        }
    }

    private void writeWCache(int addr, int val) {
        Memory.hostWriteW(_vga.FastMemAlloc, addr, val);
        if (addr < 320) {
            // And replicate the first line
            Memory.hostWriteW(_vga.FastMemAlloc, addr + 64 * 1024, val);
        }
    }

    private void writeDCache(int addr, int val) {
        Memory.hostWriteD(_vga.FastMemAlloc, addr, val);
        if (addr < 320) {
            // And replicate the first line
            Memory.hostWriteD(_vga.FastMemAlloc, addr + 64 * 1024, val);
        }
    }

    private void writeBHandler(int addr, int val) {
        // No need to check for compatible chains here, this one is only enabled if that bit is set
        _vga.Mem.LinearAlloc[(int) ((addr & ~3) << 2) + (addr & 3)] = (byte) val;
    }

    private void writeDHandler(int addr, int val) {
        // No need to check for compatible chains here, this one is only enabled if that bit is set
        Memory.hostWriteD(_vga.Mem.LinearAlloc, (int) ((addr & ~3) << 2) + (addr & 3), val);
    }

    private void writeWHandler(int addr, int val) {
        // No need to check for compatible chains here, this one is only enabled if that bit is set
        Memory.hostWriteW(_vga.Mem.LinearAlloc, (int) ((addr & ~3) << 2) + (addr & 3), val);
    }

    @Override
    public int readB(int addr) {
        addr = Paging.getPhysicalAddress(addr) & _vga.PageMask;
        addr += _vga.SVGA.BankFeadFull;
        addr = _vga.checked(addr);
        return readBHandler(addr);
    }

    @Override
    public int readW(int addr) {
        addr = Paging.getPhysicalAddress(addr) & _vga.PageMask;
        addr += _vga.SVGA.BankFeadFull;
        addr = _vga.checked(addr);
        if ((addr & 1) != 0)
            return (int) ((readBHandler(addr + 0) << 0) | (readBHandler(addr + 1) << 8));
        else
            return readWHandler(addr);
    }

    @Override
    public long readD(int addr) {
        addr = Paging.getPhysicalAddress(addr) & _vga.PageMask;
        addr += _vga.SVGA.BankFeadFull;
        addr = _vga.checked(addr);
        if ((addr & 3) != 0)
            return (int) ((readBHandler(addr + 0) << 0) | (readBHandler(addr + 1) << 8)
                    | (readBHandler(addr + 2) << 16) | (readBHandler(addr + 3) << 24));
        else
            return readDHandler(addr);
    }

    @Override
    public void writeB(int addr, int val) {
        addr = Paging.getPhysicalAddress(addr) & _vga.PageMask;
        addr += _vga.SVGA.BankWriteFull;
        addr = _vga.checked(addr);
        /*
         * #if VGA_KEEP_CHANGES MEM_CHANGED(addr); #endif
         */
        writeBHandler(addr, val);
        writeBCache(addr, val);
    }

    @Override
    public void writeW(int addr, int val) {
        addr = Paging.getPhysicalAddress(addr) & _vga.PageMask;
        addr += _vga.SVGA.BankWriteFull;
        addr = _vga.checked(addr);
        /*
         * #if VGA_KEEP_CHANGES MEM_CHANGED(addr); #endif
         */
        // MEM_CHANGED( addr + 1);
        if ((addr & 1) != 0) {
            writeBHandler(addr + 0, val >>>0);
            writeBHandler(addr + 1, val >>>8);
        } else {
            writeWHandler(addr, val);
        }
        writeWCache(addr, val);
    }

    @Override
    public void writeD(int addr, int val) {
        addr = Paging.getPhysicalAddress(addr) & _vga.PageMask;
        addr += _vga.SVGA.BankWriteFull;
        addr = _vga.checked(addr);
        /*
         * #if VGA_KEEP_CHANGES MEM_CHANGED(addr); #endif
         */
        // MEM_CHANGED( addr + 3);
        if ((addr & 3) != 0) {
            writeBHandler(addr + 0, val >>>0);
            writeBHandler(addr + 1, val >>>8);
            writeBHandler(addr + 2, val >>>16);
            writeBHandler(addr + 3, val >>>24);
        } else {
            writeDHandler(addr, val);
        }
        writeDCache(addr, val);
    }
}
