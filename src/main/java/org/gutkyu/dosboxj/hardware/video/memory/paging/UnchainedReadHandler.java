package org.gutkyu.dosboxj.hardware.video.memory.paging;


import org.gutkyu.dosboxj.hardware.memory.paging.*;
import org.gutkyu.dosboxj.hardware.video.*;

class UnchainedReadHandler extends PageHandler {
    private VGA vga;

    public UnchainedReadHandler(VGA vga) {
        this.vga = vga;
    }

    public int readBHandler(int start) {
        // vga.latch.d = ((Bit32u*)vga.mem.linear)[start];
        int idx4 = vga.Mem.LinearBase + start * 4;// 1 uint(4 byte)단위
        byte[] linearAlloc = vga.Mem.LinearAlloc;
        vga.Latch.b0 = linearAlloc[idx4++];
        vga.Latch.b1 = linearAlloc[idx4++];
        vga.Latch.b2 = linearAlloc[idx4++];
        vga.Latch.b3 = linearAlloc[idx4++];

        switch (vga.Config.ReadMode) {
            case 0:
                // return (vga.latch.b[vga.config.read_map_select]);
                switch (vga.Config.ReadMapSelect) {
                    case 0:
                        return 0xff & vga.Latch.b0;
                    case 1:
                        return 0xff & vga.Latch.b1;
                    case 2:
                        return 0xff & vga.Latch.b2;
                    case 3:
                        return 0xff & vga.Latch.b3;
                }
                break;// 여기까지 도달 불가능
            case 1:
                VGALatch templatch = new VGALatch();
                templatch.d((vga.Latch.d() & vga.FillTable[vga.Config.ColorDontCare])
                        ^ vga.FillTable[vga.Config.ColorCompare & vga.Config.ColorDontCare]);
                return 0xff & ~(templatch.b0 | templatch.b1 | templatch.b2 | templatch.b3);
        }
        return 0;
    }

    @Override
    public int readB(int addr) {
        addr = Paging.getPhysicalAddress(addr) & vga.PageMask;
        addr += vga.SVGA.BankFeadFull;
        addr = vga.checked2(addr);
        return readBHandler(addr);
    }

    @Override
    public int readW(int addr) {
        addr = Paging.getPhysicalAddress(addr) & vga.PageMask;
        addr += vga.SVGA.BankFeadFull;
        addr = vga.checked2(addr);
        return (readBHandler(addr + 0) << 0) | (readBHandler(addr + 1) << 8);
    }

    @Override
    public long readD(int addr) {
        addr = Paging.getPhysicalAddress(addr) & vga.PageMask;
        addr += vga.SVGA.BankFeadFull;
        addr = vga.checked2(addr);
        return 0xffffffffL & ((readBHandler(addr + 0) << 0) | (readBHandler(addr + 1) << 8)
                | (readBHandler(addr + 2) << 16) | (readBHandler(addr + 3) << 24));
    }
}
