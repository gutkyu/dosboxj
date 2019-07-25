package org.gutkyu.dosboxj.hardware.video.memory.paging;


import org.gutkyu.dosboxj.hardware.memory.paging.*;
import org.gutkyu.dosboxj.hardware.video.*;


public final class UnchainedVGAHandler extends ChainedVGAHandler {
    VGA vga = null;

    public UnchainedVGAHandler(VGA vga) {
        super(vga);
        this.vga = vga;
        Flags = Paging.PFLAG_NOCODE;
    }

    // (int,byte)
    public void writeHandler(int addr, int val) {
        int data = vga.modeOperation(val);
        VGALatch pixels = new VGALatch();
        // pixels.d = ((Bit32u*)vga.mem.linear)[addr];
        int idx4 = vga.Mem.LinearBase + addr * 4;// 1 uint(4 byte)단위
        pixels.b0 = vga.Mem.LinearAlloc[idx4];
        pixels.b1 = vga.Mem.LinearAlloc[idx4 + 1];
        pixels.b2 = vga.Mem.LinearAlloc[idx4 + 2];
        pixels.b3 = vga.Mem.LinearAlloc[idx4 + 3];

        pixels.d &= vga.Config.FullNotMapMask;
        pixels.d |= (data & vga.Config.FullMapMask);
        // ((Bit32u*)vga.mem.linear)[addr] = pixels.d;
        vga.Mem.LinearAlloc[idx4] = pixels.b0;
        vga.Mem.LinearAlloc[idx4 + 1] = pixels.b1;
        vga.Mem.LinearAlloc[idx4 + 2] = pixels.b2;
        vga.Mem.LinearAlloc[idx4 + 3] = pixels.b3;
        // if(vga.config.compatible_chain4)
        // ((int*)vga.mem.linear)[CHECKED2(addr+64*1024)]=pixels.d;
    }

    @Override
    public void writeB(int addr, int val) {
        addr = Paging.getPhysicalAddress(addr) & vga.PageMask;
        addr += vga.SVGA.BankWriteFull;
        addr = vga.checked2(addr);
        /*
         * #if VGA_KEEP_CHANGES MEM_CHANGED(addr << 2); #endif
         */
        writeHandler(addr + 0, 0xff & (val >>> 0));
    }

    @Override
    public void writeW(int addr, int val) {
        addr = Paging.getPhysicalAddress(addr) & vga.PageMask;
        addr += vga.SVGA.BankWriteFull;
        addr = vga.checked2(addr);
        /*
         * #if VGA_KEEP_CHANGES MEM_CHANGED(addr << 2); #endif
         */
        writeHandler(addr + 0, 0xff & (val >>> 0));
        writeHandler(addr + 1, 0xff & (val >>> 8));
    }

    @Override
    public void writeD(int addr, int val) {
        addr = Paging.getPhysicalAddress(addr) & vga.PageMask;
        addr += vga.SVGA.BankWriteFull;
        addr = vga.checked2(addr);
        /*
         * #if VGA_KEEP_CHANGES MEM_CHANGED(addr << 2); #endif
         */
        writeHandler(addr + 0, 0xff & (val >>> 0));
        writeHandler(addr + 1, 0xff & (val >>> 8));
        writeHandler(addr + 2, 0xff & (val >>> 16));
        writeHandler(addr + 3, 0xff & (val >>> 24));
    }
}
