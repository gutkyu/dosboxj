package org.gutkyu.dosboxj.hardware.video.memory.paging;

import org.gutkyu.dosboxj.hardware.memory.paging.*;
import org.gutkyu.dosboxj.hardware.video.*;
import org.gutkyu.dosboxj.util.ByteConv;

public final class ChainedEGAHandler extends PageHandler {
    private VGA _vga;

    public ChainedEGAHandler(VGA vga) {
        _vga = vga;
        Flags = Paging.PFLAG_NOCODE;
    }

    public int readHandler(int addr) {
        return _vga.Mem.LinearAlloc[addr];
    }

    // (int, byte)
    public void writeHandler(int start, int val) {
        _vga.modeOperation(val);
        /* Update video memory and the pixel buffer */
        VGALatch pixels = new VGALatch();
        byte[] linearAlloc = _vga.Mem.LinearAlloc;
        linearAlloc[start] = (byte) val;
        start >>>= 2;

        // pixels.d = ((Bit32u*)vga.mem.linear)[start];
        int idx4 = _vga.Mem.LinearBase + start * 4;// 1 uint(4 byte)단위 임
        pixels.b0 = linearAlloc[idx4];
        pixels.b1 = linearAlloc[idx4 + 1];
        pixels.b2 = linearAlloc[idx4 + 2];
        pixels.b3 = linearAlloc[idx4 + 3];

        int write_pixels_idx = start << 3;

        // ByteConvert colors0_3 = new ByteConvert();
        //ByteBuffer colors0_3 = ByteBuffer.allocate(4);
        VGALatch temp = new VGALatch();
        temp.d = (pixels.d >>> 4) & 0x0f0f0f0f;
        int[][] expand16Tbl = _vga.Expand16Table;
        int colors0_3 = expand16Tbl[0][temp.b0] | expand16Tbl[1][temp.b1]
                | expand16Tbl[2][temp.b2] | expand16Tbl[3][temp.b3];

        int fmidx = (int) (_vga.FastMemBase + write_pixels_idx);
        byte[] fastmemAlloc = _vga.FastMemAlloc;
        ByteConv.setInt(fastmemAlloc, fmidx, colors0_3);
        fmidx += 4;

        // ByteConvert colors4_7 = new ByteConvert();
        temp.d = pixels.d & 0x0f0f0f0f;
        int colors4_7 = expand16Tbl[0][temp.b0] | expand16Tbl[1][temp.b1]
                | expand16Tbl[2][temp.b2] | expand16Tbl[3][temp.b3];
        ByteConv.setInt(fastmemAlloc, fmidx, colors4_7);
    }

    @Override
    public void writeB(int addr, int val) {
        addr = Paging.getPhysicalAddress(addr) & _vga.PageMask;
        addr += _vga.SVGA.BankWriteFull;
        addr = _vga.checked(addr);
        /*
         * #if VGA_KEEP_CHANGES MEMORY.MEM_CHANGED(addr << 3); #endif
         */
        writeHandler(addr + 0, 0xff & (val >>> 0));
    }

    @Override
    public void writeW(int addr, int val) {
        addr = Paging.getPhysicalAddress(addr) & _vga.PageMask;
        addr += _vga.SVGA.BankWriteFull;
        addr = _vga.checked(addr);
        /*
         * #if VGA_KEEP_CHANGES MEMORY.MEM_CHANGED(addr << 3); #endif
         */
        writeHandler(addr + 0, 0xff & (val >>> 0));
        writeHandler(addr + 1, 0xff & (val >>> 8));
    }

    @Override
    public void writeD(int addr, int val) {
        addr = Paging.getPhysicalAddress(addr) & _vga.PageMask;
        addr += _vga.SVGA.BankWriteFull;
        addr = _vga.checked(addr);
        /*
         * #if VGA_KEEP_CHANGES MEMORY.MEM_CHANGED(addr << 3); #endif
         */
        writeHandler(addr + 0, 0xff & (val >>> 0));
        writeHandler(addr + 1, 0xff & (val >>> 8));
        writeHandler(addr + 2, 0xff & (val >>> 16));
        writeHandler(addr + 3, 0xff & (val >>> 24));
    }

    @Override
    public int readB(int addr) {
        addr = Paging.getPhysicalAddress(addr) & _vga.PageMask;
        addr += _vga.SVGA.BankFeadFull;
        addr = _vga.checked(addr);
        return readHandler(addr);
    }

    @Override
    public int readW(int addr) {
        addr = Paging.getPhysicalAddress(addr) & _vga.PageMask;
        addr += _vga.SVGA.BankFeadFull;
        addr = _vga.checked(addr);
        return (readHandler(addr + 0) << 0) | (readHandler(addr + 1) << 8);
    }

    @Override
    public long readD(int addr) {
        addr = Paging.getPhysicalAddress(addr) & _vga.PageMask;
        addr += _vga.SVGA.BankFeadFull;
        addr = _vga.checked(addr);
        return (readHandler(addr + 0) << 0) | (readHandler(addr + 1) << 8)
                | (readHandler(addr + 2) << 16) | (readHandler(addr + 3) << 24);
    }
}
