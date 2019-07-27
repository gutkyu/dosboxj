package org.gutkyu.dosboxj.hardware.video.memory.paging;

import org.gutkyu.dosboxj.hardware.memory.paging.*;
import org.gutkyu.dosboxj.hardware.video.*;
import org.gutkyu.dosboxj.util.ByteConv;



public class UnchainedEGAHandler extends UnchainedReadHandler {
    private VGA vga;

    public UnchainedEGAHandler(VGA vga) {
        super(vga);
        this.vga = vga;
        Flags = Paging.PFLAG_NOCODE;
    }

    // (int, byte)
    public void writeHandler(int start, int val) {
        int data = vga.modeOperation(val);
        /* Update video memory and the pixel buffer */
        VGALatch pixels = new VGALatch();

        // pixels.d = ((Bit32u*)vga.mem.linear)[start];
        int idx4 = vga.Mem.LinearBase + start * 4;// 1 uint(4 byte)단위
        byte[] linearAlloc = vga.Mem.LinearAlloc;
        pixels.b0 = linearAlloc[idx4];
        pixels.b1 = linearAlloc[idx4 + 1];
        pixels.b2 = linearAlloc[idx4 + 2];
        pixels.b3 = linearAlloc[idx4 + 3];

        pixels.d(pixels.d() & vga.Config.FullNotMapMask);
        pixels.d(pixels.d() | (data & vga.Config.FullMapMask));
        linearAlloc[idx4] = pixels.b0;
        linearAlloc[idx4 + 1] = pixels.b1;
        linearAlloc[idx4 + 2] = pixels.b2;
        linearAlloc[idx4 + 3] = pixels.b3;

        int write_pixels_idx = start << 3;

        // ByteConvert colors0_3, colors4_7;
        VGALatch temp = new VGALatch();
        temp.d((pixels.d() >>> 4) & 0x0f0f0f0f);
        int[][] expand16Tbl = vga.Expand16Table;
        int colors0_3 = expand16Tbl[0][temp.b0] | expand16Tbl[1][temp.b1] | expand16Tbl[2][temp.b2]
                | expand16Tbl[3][temp.b3];

        int fmidx = (int) (vga.FastMemBase + write_pixels_idx);
        byte[] fastmemAlloc = vga.FastMemAlloc;
        ByteConv.setInt(fastmemAlloc, fmidx, colors0_3);
        fmidx += 4;
        temp.d(pixels.d() & 0x0f0f0f0f);
        int colors4_7 = expand16Tbl[0][temp.b0] | expand16Tbl[1][temp.b1] | expand16Tbl[2][temp.b2]
                | expand16Tbl[3][temp.b3];
        ByteConv.setInt(fastmemAlloc, fmidx, colors4_7);
    }

    @Override
    public void writeB(int addr, int val) {
        addr = Paging.getPhysicalAddress(addr) & vga.PageMask;
        addr += vga.SVGA.BankWriteFull;
        addr = vga.checked2(addr);
        /*
         * #if VGA_KEEP_CHANGES MEM_CHANGED(addr << 3); #endif
         */
        writeHandler(addr, 0xff & val);
    }

    @Override
    public void writeW(int addr, int val) {
        addr = Paging.getPhysicalAddress(addr) & vga.PageMask;
        addr += vga.SVGA.BankWriteFull;
        addr = vga.checked2(addr);
        /*
         * #if VGA_KEEP_CHANGES MEM_CHANGED(addr << 3); #endif
         */
        writeHandler(addr++, 0xff & val);
        writeHandler(addr, 0xff & (val >>> 8));
    }

    @Override
    public void writeD(int addr, int val) {
        addr = Paging.getPhysicalAddress(addr) & vga.PageMask;
        addr += vga.SVGA.BankWriteFull;
        addr = vga.checked2(addr);
        /*
         * #if VGA_KEEP_CHANGES MEM_CHANGED(addr << 3); #endif
         */
        writeHandler(addr++, 0xff & val);
        writeHandler(addr++, 0xff & (val >>> 8));
        writeHandler(addr++, 0xff & (val >>> 16));
        writeHandler(addr, 0xff & (val >>> 24));
    }
}
