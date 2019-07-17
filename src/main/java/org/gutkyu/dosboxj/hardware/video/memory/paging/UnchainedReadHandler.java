package org.gutkyu.dosboxj.hardware.video.memory.paging;


import org.gutkyu.dosboxj.hardware.memory.paging.*;
import org.gutkyu.dosboxj.hardware.video.*;

class UnchainedReadHandler extends PageHandler {
    private VGA _vga;

    public UnchainedReadHandler(VGA vga) {
        _vga = vga;
    }

    public int readHandler(int start) {
        // vga.latch.d = ((Bit32u*)vga.mem.linear)[start];
        int idx4 = _vga.Mem.LinearBase + start * 4;// 1 uint(4 byte)단위
        byte[] linearAlloc = _vga.Mem.LinearAlloc;
        _vga.Latch.b0 = linearAlloc[idx4++];
        _vga.Latch.b1 = linearAlloc[idx4++];
        _vga.Latch.b2 = linearAlloc[idx4++];
        _vga.Latch.b3 = linearAlloc[idx4++];

        switch (_vga.Config.ReadMode) {
            case 0:
                // return (vga.latch.b[vga.config.read_map_select]);
                switch (_vga.Config.ReadMapSelect) {
                    case 0:
                        return _vga.Latch.b0;
                    case 1:
                        return _vga.Latch.b1;
                    case 2:
                        return _vga.Latch.b2;
                    case 3:
                        return _vga.Latch.b3;
                }
                break;// 여기까지 도달 불가능
            case 1:
                VGALatch templatch = new VGALatch();
                templatch.d = (_vga.Latch.d & _vga.FillTable[_vga.Config.ColorDontCare])
                        ^ _vga.FillTable[_vga.Config.ColorCompare & _vga.Config.ColorDontCare];
                return (byte) ~(templatch.b0 | templatch.b1 | templatch.b2 | templatch.b3);
        }
        return 0;
    }

    @Override
    public int readB(int addr) {
        addr = Paging.getPhysicalAddress(addr) & _vga.PageMask;
        addr += _vga.SVGA.BankFeadFull;
        addr = _vga.checked2(addr);
        return readHandler(addr);
    }

    @Override
    public int readW(int addr) {
        addr = Paging.getPhysicalAddress(addr) & _vga.PageMask;
        addr += _vga.SVGA.BankFeadFull;
        addr = _vga.checked2(addr);
        return (readHandler(addr + 0) << 0) | (readHandler(addr + 1) << 8);
    }

    @Override
    public long readD(int addr) {
        addr = Paging.getPhysicalAddress(addr) & _vga.PageMask;
        addr += _vga.SVGA.BankFeadFull;
        addr = _vga.checked2(addr);
        return (readHandler(addr + 0) << 0) | (readHandler(addr + 1) << 8)
                | (readHandler(addr + 2) << 16) | (readHandler(addr + 3) << 24);
    }
}
