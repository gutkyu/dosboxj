package org.gutkyu.dosboxj.interrupt.int10;

import org.gutkyu.dosboxj.hardware.io.*;
import org.gutkyu.dosboxj.hardware.memory.*;
import org.gutkyu.dosboxj.*;

/*--------------------------- begin INT10VideoState -----------------------------*/
final class VideoState {
    public static int getSize(int state) {
        // state: bit0=hardware, bit1=bios data, bit2=color regs/dac state
        if ((state & 7) == 0)
            return 0;

        int size = 0x20;
        if ((state & 1) != 0)
            size += 0x46;
        if ((state & 2) != 0)
            size += 0x3a;
        if ((state & 4) != 0)
            size += 0x303;
        if ((DOSBox.SVGACard == DOSBox.SVGACards.S3Trio) && (state & 8) != 0)
            size += 0x43;
        if (size != 0)
            size = (size - 1) / 64 + 1;
        return size;
    }

    public static boolean save(int state, int buffer) {
        int ct;
        if ((state & 7) == 0)
            return false;

        int base_seg = Memory.realSeg(buffer);
        int base_dest = Memory.realOff(buffer) + 0x20;

        if ((state & 1) != 0) {
            Memory.realWriteW(base_seg, Memory.realOff(buffer), base_dest);

            int crt_reg = Memory.realReadW(INT10.BIOSMEM_SEG, INT10.BIOSMEM_CRTC_ADDRESS);
            Memory.realWriteW(base_seg, base_dest + 0x40, crt_reg);

            Memory.realWriteB(base_seg, base_dest + 0x00, IO.readB(0x3c4));
            Memory.realWriteB(base_seg, base_dest + 0x01, IO.readB(0x3d4));
            Memory.realWriteB(base_seg, base_dest + 0x02, IO.readB(0x3ce));
            IO.readB(crt_reg + 6);
            Memory.realWriteB(base_seg, base_dest + 0x03, IO.readB(0x3c0));
            Memory.realWriteB(base_seg, base_dest + 0x04, IO.readB(0x3ca));

            // sequencer
            for (ct = 1; ct < 5; ct++) {
                IO.writeB(0x3c4, ct);
                Memory.realWriteB(base_seg, base_dest + 0x04 + ct, IO.readB(0x3c5));
            }

            Memory.realWriteB(base_seg, base_dest + 0x09, IO.readB(0x3cc));

            // crt controller
            for (ct = 0; ct < 0x19; ct++) {
                IO.writeB(crt_reg, ct);
                Memory.realWriteB(base_seg, base_dest + 0x0a + ct, IO.readB(crt_reg + 1));
            }

            // attr registers
            for (ct = 0; ct < 4; ct++) {
                IO.readB(crt_reg + 6);
                IO.writeB(0x3c0, 0x10 + ct);
                Memory.realWriteB(base_seg, base_dest + 0x33 + ct, IO.readB(0x3c1));
            }

            // graphics registers
            for (ct = 0; ct < 9; ct++) {
                IO.writeB(0x3ce, ct);
                Memory.realWriteB(base_seg, base_dest + 0x37 + ct, IO.readB(0x3cf));
            }

            // save some registers
            IO.writeB(0x3c4, 2);
            int crtc_2 = IO.readB(0x3c5);
            IO.writeB(0x3c4, 4);
            int crtc_4 = IO.readB(0x3c5);
            IO.writeB(0x3ce, 6);
            int gfx_6 = IO.readB(0x3cf);
            IO.writeB(0x3ce, 5);
            int gfx_5 = IO.readB(0x3cf);
            IO.writeB(0x3ce, 4);
            int gfx_4 = IO.readB(0x3cf);

            // reprogram for full access to plane latches
            IO.writeW(0x3c4, 0x0f02);
            IO.writeW(0x3c4, 0x0704);
            IO.writeW(0x3ce, 0x0406);
            IO.writeW(0x3ce, 0x0105);
            Memory.writeB(0xaffff, 0);

            for (ct = 0; ct < 4; ct++) {
                IO.writeW(0x3ce, 0x0004 + ct * 0x100);
                Memory.realWriteB(base_seg, base_dest + 0x42 + ct, Memory.readB(0xaffff));
            }

            // restore registers
            IO.writeW(0x3ce, 0x0004 | (gfx_4 << 8));
            IO.writeW(0x3ce, 0x0005 | (gfx_5 << 8));
            IO.writeW(0x3ce, 0x0006 | (gfx_6 << 8));
            IO.writeW(0x3c4, 0x0004 | (crtc_4 << 8));
            IO.writeW(0x3c4, 0x0002 | (crtc_2 << 8));

            for (ct = 0; ct < 0x10; ct++) {
                IO.readB(crt_reg + 6);
                IO.writeB(0x3c0, ct);
                Memory.realWriteB(base_seg, base_dest + 0x23 + ct, IO.readB(0x3c1));
            }
            IO.writeB(0x3c0, 0x20);

            base_dest += 0x46;
        }

        if ((state & 2) != 0) {
            Memory.realWriteW(base_seg, Memory.realOff(buffer) + 2, base_dest);

            Memory.realWriteB(base_seg, base_dest + 0x00, Memory.readB(0x410) & 0x30);
            for (ct = 0; ct < 0x1e; ct++) {
                Memory.realWriteB(base_seg, base_dest + 0x01 + ct, Memory.readB(0x449 + ct));
            }
            for (ct = 0; ct < 0x07; ct++) {
                Memory.realWriteB(base_seg, base_dest + 0x1f + ct, Memory.readB(0x484 + ct));
            }
            Memory.realWriteD(base_seg, base_dest + 0x26, Memory.readD(0x48a));
            Memory.realWriteD(base_seg, base_dest + 0x2a, Memory.readD(0x14)); // int 5
            Memory.realWriteD(base_seg, base_dest + 0x2e, Memory.readD(0x74)); // int 1d
            Memory.realWriteD(base_seg, base_dest + 0x32, Memory.readD(0x7c)); // int 1f
            Memory.realWriteD(base_seg, base_dest + 0x36, Memory.readD(0x10c)); // int 43

            base_dest += 0x3a;
        }

        if ((state & 4) != 0) {
            Memory.realWriteW(base_seg, Memory.realOff(buffer) + 4, base_dest);

            int crt_reg = Memory.realReadW(INT10.BIOSMEM_SEG, INT10.BIOSMEM_CRTC_ADDRESS);

            IO.readB(crt_reg + 6);
            IO.writeB(0x3c0, 0x14);
            Memory.realWriteB(base_seg, base_dest + 0x303, IO.readB(0x3c1));

            int dac_state = IO.readB(0x3c7) & 1;
            int dac_windex = IO.readB(0x3c8);
            if (dac_state != 0)
                dac_windex--;
            Memory.realWriteB(base_seg, (base_dest + 0x000), dac_state);
            Memory.realWriteB(base_seg, (base_dest + 0x001), dac_windex);
            Memory.realWriteB(base_seg, (base_dest + 0x002), IO.readB(0x3c6));

            for (ct = 0; ct < 0x100; ct++) {
                IO.writeB(0x3c7, ct);
                Memory.realWriteB(base_seg, (base_dest + 0x003 + ct * 3 + 0), IO.readB(0x3c9));
                Memory.realWriteB(base_seg, (base_dest + 0x003 + ct * 3 + 1), IO.readB(0x3c9));
                Memory.realWriteB(base_seg, (base_dest + 0x003 + ct * 3 + 2), IO.readB(0x3c9));
            }

            IO.readB(crt_reg + 6);
            IO.writeB(0x3c0, 0x20);

            base_dest += 0x303;
        }

        if ((DOSBox.SVGACard == DOSBox.SVGACards.S3Trio) && (state & 8) != 0) {
            Memory.realWriteW(base_seg, (Memory.realOff(buffer) + 6), base_dest);

            int crt_reg = Memory.realReadW(INT10.BIOSMEM_SEG, INT10.BIOSMEM_CRTC_ADDRESS);

            IO.writeB(0x3c4, 0x08);
            // int seq_8=iohandler.IO_ReadB(0x3c5);
            IO.readB(0x3c5);
            // MemModule.real_writeb(base_seg,base_dest+0x00,iohandler.IO_ReadB(0x3c5));
            IO.writeB(0x3c5, 0x06); // unlock s3-specific registers

            // sequencer
            for (ct = 0; ct < 0x13; ct++) {
                IO.writeB(0x3c4, 0x09 + ct);
                Memory.realWriteB(base_seg, (base_dest + 0x00 + ct), IO.readB(0x3c5));
            }

            // unlock s3-specific registers
            IO.writeW(crt_reg, 0x4838);
            IO.writeW(crt_reg, 0xa539);

            // crt controller
            int ct_dest = 0x13;
            for (ct = 0; ct < 0x40; ct++) {
                if ((ct == 0x4a - 0x30) || (ct == 0x4b - 0x30)) {
                    IO.writeB(crt_reg, 0x45);
                    IO.readB(crt_reg + 1);
                    IO.writeB(crt_reg, 0x30 + ct);
                    Memory.realWriteB(base_seg, (base_dest + (ct_dest++)), IO.readB(crt_reg + 1));
                    Memory.realWriteB(base_seg, (base_dest + (ct_dest++)), IO.readB(crt_reg + 1));
                    Memory.realWriteB(base_seg, (base_dest + (ct_dest++)), IO.readB(crt_reg + 1));
                } else {
                    IO.writeB(crt_reg, 0x30 + ct);
                    Memory.realWriteB(base_seg, (base_dest + (ct_dest++)), IO.readB(crt_reg + 1));
                }
            }
        }
        return true;
    }

    public static boolean restore(int state, int buffer) {
        int ct;
        if ((state & 7) == 0)
            return false;

        int base_seg = Memory.realSeg(buffer);
        int base_dest;

        if ((state & 1) != 0) {
            base_dest = Memory.realReadW(base_seg, Memory.realOff(buffer));
            int crt_reg = Memory.realReadW(base_seg, base_dest + 0x40);

            // reprogram for full access to plane latches
            IO.writeW(0x3c4, 0x0704);
            IO.writeW(0x3ce, 0x0406);
            IO.writeW(0x3ce, 0x0005);

            IO.writeW(0x3c4, 0x0002);
            Memory.writeB(0xaffff, Memory.realReadB(base_seg, (base_dest + 0x42)));
            IO.writeW(0x3c4, 0x0102);
            Memory.writeB(0xaffff, Memory.realReadB(base_seg, (base_dest + 0x43)));
            IO.writeW(0x3c4, 0x0202);
            Memory.writeB(0xaffff, Memory.realReadB(base_seg, (base_dest + 0x44)));
            IO.writeW(0x3c4, 0x0402);
            Memory.writeB(0xaffff, Memory.realReadB(base_seg, (base_dest + 0x45)));
            IO.writeW(0x3c4, 0x0f02);
            Memory.readB(0xaffff);

            IO.writeW(0x3c4, 0x0100);

            // sequencer
            for (ct = 1; ct < 5; ct++) {
                IO.writeW(0x3c4,
                        ct + (int) (Memory.realReadB(base_seg, (base_dest + 0x04 + ct)) << 8));
            }

            IO.writeB(0x3c2, Memory.realReadB(base_seg, (base_dest + 0x09)));
            IO.writeW(0x3c4, 0x0300);
            IO.writeW(crt_reg, 0x0011);

            // crt controller
            for (ct = 0; ct < 0x19; ct++) {
                IO.writeW(crt_reg,
                        ct + (int) (Memory.realReadB(base_seg, (base_dest + 0x0a + ct)) << 8));
            }

            IO.readB(crt_reg + 6);
            // attr registers
            for (ct = 0; ct < 4; ct++) {
                IO.writeB(0x3c0, 0x10 + ct);
                IO.writeB(0x3c0, Memory.realReadB(base_seg, (base_dest + 0x33 + ct)));
            }

            // graphics registers
            for (ct = 0; ct < 9; ct++) {
                IO.writeW(0x3ce,
                        ct + (int) (Memory.realReadB(base_seg, (base_dest + 0x37 + ct)) << 8));
            }

            IO.writeB(crt_reg + 6, Memory.realReadB(base_seg, (base_dest + 0x04)));
            IO.readB(crt_reg + 6);

            // attr registers
            for (ct = 0; ct < 0x10; ct++) {
                IO.writeB(0x3c0, ct);
                IO.writeB(0x3c0, Memory.realReadB(base_seg, (base_dest + 0x23 + ct)));
            }

            IO.writeB(0x3c4, Memory.realReadB(base_seg, (base_dest + 0x00)));
            IO.writeB(0x3d4, Memory.realReadB(base_seg, (base_dest + 0x01)));
            IO.writeB(0x3ce, Memory.realReadB(base_seg, (base_dest + 0x02)));
            IO.readB(crt_reg + 6);
            IO.writeB(0x3c0, Memory.realReadB(base_seg, (base_dest + 0x03)));
        }

        if ((state & 2) != 0) {
            base_dest = Memory.realReadW(base_seg, (Memory.realOff(buffer) + 2));

            Memory.writeB(0x410, ((Memory.readB(0x410) & 0xcf)
                    | Memory.realReadB(base_seg, (base_dest + 0x00))));
            for (ct = 0; ct < 0x1e; ct++) {
                Memory.writeB(0x449 + ct, Memory.realReadB(base_seg, (base_dest + 0x01 + ct)));
            }
            for (ct = 0; ct < 0x07; ct++) {
                Memory.writeB(0x484 + ct, Memory.realReadB(base_seg, (base_dest + 0x1f + ct)));
            }
            Memory.writeD(0x48a, Memory.realReadD(base_seg, (base_dest + 0x26)));
            Memory.writeD(0x14, Memory.realReadD(base_seg, (base_dest + 0x2a))); // int 5
            Memory.writeD(0x74, Memory.realReadD(base_seg, (base_dest + 0x2e))); // int 1d
            Memory.writeD(0x7c, Memory.realReadD(base_seg, (base_dest + 0x32))); // int 1f
            Memory.writeD(0x10c, Memory.realReadD(base_seg, (base_dest + 0x36))); // int 43
        }

        if ((state & 4) != 0) {
            base_dest = Memory.realReadW(base_seg, (Memory.realOff(buffer) + 4));

            int crt_reg = Memory.realReadW(INT10.BIOSMEM_SEG, INT10.BIOSMEM_CRTC_ADDRESS);

            IO.writeB(0x3c6, Memory.realReadB(base_seg, (base_dest + 0x002)));

            for (ct = 0; ct < 0x100; ct++) {
                IO.writeB(0x3c8, ct);
                IO.writeB(0x3c9, Memory.realReadB(base_seg, (base_dest + 0x003 + ct * 3 + 0)));
                IO.writeB(0x3c9, Memory.realReadB(base_seg, (base_dest + 0x003 + ct * 3 + 1)));
                IO.writeB(0x3c9, Memory.realReadB(base_seg, (base_dest + 0x003 + ct * 3 + 2)));
            }

            IO.readB(crt_reg + 6);
            IO.writeB(0x3c0, 0x14);
            IO.writeB(0x3c0, Memory.realReadB(base_seg, (base_dest + 0x303)));

            int dac_state = Memory.realReadB(base_seg, (base_dest + 0x000));
            if (dac_state == 0) {
                IO.writeB(0x3c8, Memory.realReadB(base_seg, (base_dest + 0x001)));
            } else {
                IO.writeB(0x3c7, Memory.realReadB(base_seg, (base_dest + 0x001)));
            }
        }

        if ((DOSBox.SVGACard == DOSBox.SVGACards.S3Trio) && (state & 8) != 0) {
            base_dest = Memory.realReadW(base_seg, (Memory.realOff(buffer) + 6));

            int crt_reg = Memory.realReadW(INT10.BIOSMEM_SEG, INT10.BIOSMEM_CRTC_ADDRESS);

            int seq_idx = IO.readB(0x3c4);
            IO.writeB(0x3c4, 0x08);
            // int seq_8=iohandler.IO_ReadB(0x3c5);
            IO.readB(0x3c5);
            // MemModule.real_writeb(base_seg,base_dest+0x00,iohandler.IO_ReadB(0x3c5));
            IO.writeB(0x3c5, 0x06); // unlock s3-specific registers

            // sequencer
            for (ct = 0; ct < 0x13; ct++) {
                IO.writeW(0x3c4, (0x09 + ct)
                        + (int) (Memory.realReadB(base_seg, (base_dest + 0x00 + ct)) << 8));
            }
            IO.writeB(0x3c4, seq_idx);

            // int crtc_idx=iohandler.IO_ReadB(0x3d4);

            // unlock s3-specific registers
            IO.writeW(crt_reg, 0x4838);
            IO.writeW(crt_reg, 0xa539);

            // crt controller
            int ct_dest = 0x13;
            for (ct = 0; ct < 0x40; ct++) {
                if ((ct == 0x4a - 0x30) || (ct == 0x4b - 0x30)) {
                    IO.writeB(crt_reg, 0x45);
                    IO.readB(crt_reg + 1);
                    IO.writeB(crt_reg, 0x30 + ct);
                    IO.writeB(crt_reg, Memory.realReadB(base_seg, (base_dest + (ct_dest++))));
                } else {
                    IO.writeW(crt_reg, (0x30 + ct)
                            + (int) (Memory.realReadB(base_seg, (base_dest + (ct_dest++))) << 8));
                }
            }

            // mmio
            /*
             * iohandler.IO_WriteB(crt_reg,0x40); int sysval1=iohandler.IO_ReadB(crt_reg+1);
             * iohandler.IO_WriteB(crt_reg+1,sysval|1); iohandler.IO_WriteB(crt_reg,0x53); int
             * sysva2=iohandler.IO_ReadB(crt_reg+1); iohandler.IO_WriteB(crt_reg+1,sysval2|0x10);
             * 
             * MemModule.real_writew(0xa000,0x8128,0xffff);
             * 
             * iohandler.IO_WriteB(crt_reg,0x40); iohandler.IO_WriteB(crt_reg,sysval1);
             * iohandler.IO_WriteB(crt_reg,0x53); iohandler.IO_WriteB(crt_reg,sysval2);
             * iohandler.IO_WriteB(crt_reg,crtc_idx);
             */
        }

        return true;
    }
}
/*--------------------------- end INT10VideoState -----------------------------*/
