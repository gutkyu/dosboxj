package org.gutkyu.dosboxj.hardware.dma;

import org.gutkyu.dosboxj.hardware.io.iohandler.*;
import org.gutkyu.dosboxj.util.*;

final class DMAController implements Disposable {

    private byte ctrlNum;
    private boolean flipFlop;
    private DMAChannel[] dmaChannels = new DMAChannel[4];

    public IOReadHandleObject[] readHandler = new IOReadHandleObject[0x11];
    public IOWriteHandleObject[] writeHandler = new IOWriteHandleObject[0x11];

    protected DMAController(byte num) {
        flipFlop = false;
        ctrlNum = num; /* first or second DMA controller */
        for (byte i = 0; i < 4; i++) {
            dmaChannels[i] = new DMAChannel((byte) (i + ctrlNum * 4), ctrlNum == 1);
        }
        for (int i = 0; i < readHandler.length; i++) {
            readHandler[i] = new IOReadHandleObject();
        }
        for (int i = 0; i < writeHandler.length; i++) {
            writeHandler[i] = new IOWriteHandleObject();
        }
    }

    public void dispose() {
        dispose(true);
    }

    protected void dispose(boolean disposing) {
        if (disposing) {
        }

        for (byte i = 0; i < 4; i++) {
            dmaChannels[i] = null;
        }
    }

    public DMAChannel getChannel(int chan) {
        if (chan < 4)
            return dmaChannels[chan];
        else
            return null;
    }

    public void writeControllerReg(int reg, int val, int len) {
        DMAChannel chan;
        switch (reg) {
            /* set base address of DMA transfer (1st byte low part, 2nd byte high part) */
            case 0x0:
            case 0x2:
            case 0x4:
            case 0x6:
                DMAModule.instance().updateEMSMapping();
                chan = getChannel(reg >>> 1);
                flipFlop = !flipFlop;
                if (flipFlop) {
                    chan.baseAddr = 0xffff & ((chan.baseAddr & 0xff00) | val);
                    chan.currAddr = 0xffff & ((chan.currAddr & 0xff00) | val);
                } else {
                    chan.baseAddr = 0xffff & ((chan.baseAddr & 0x00ff) | (val << 8));
                    chan.currAddr = (chan.currAddr & 0x00ff) | (val << 8);
                }
                break;
            /* set DMA transfer count (1st byte low part, 2nd byte high part) */
            case 0x1:
            case 0x3:
            case 0x5:
            case 0x7:
                DMAModule.instance().updateEMSMapping();
                chan = getChannel(reg >>> 1);
                flipFlop = !flipFlop;
                if (flipFlop) {
                    chan.baseCnt = 0xffff & ((chan.baseCnt & 0xff00) | val);
                    chan.currCnt = 0xffff & ((chan.currCnt & 0xff00) | val);
                } else {
                    chan.baseCnt = 0xffff & ((chan.baseCnt & 0x00ff) | (val << 8));
                    chan.currCnt = 0xffff & ((chan.currCnt & 0x00ff) | (val << 8));
                }
                break;
            case 0x8: /* Comand reg not used */
                break;
            case 0x9: /* Request registers, memory to memory */
                // TODO Warning?
                break;
            case 0xa: /* Mask Register */
                if ((val & 0x4) == 0)
                    DMAModule.instance().updateEMSMapping();
                chan = getChannel(val & 3);
                chan.setMask((val & 0x4) > 0);
                break;
            case 0xb: /* Mode Register */
                DMAModule.instance().updateEMSMapping();
                chan = getChannel(val & 3);
                chan.autoInit = (val & 0x10) > 0;
                chan.increment = (val & 0x20) > 0;
                // TODO Maybe other bits?
                break;
            case 0xc: /* Clear Flip/Flip */
                flipFlop = false;
                break;
            case 0xd: /* Master Clear/Reset */
                for (byte ct = 0; ct < 4; ct++) {
                    chan = getChannel(ct);
                    chan.setMask(true);
                    chan.tCount = false;
                }
                flipFlop = false;
                break;
            case 0xe: /* Clear Mask register */
                DMAModule.instance().updateEMSMapping();
                for (byte ct = 0; ct < 4; ct++) {
                    chan = getChannel(ct);
                    chan.setMask(false);
                }
                break;
            case 0xf: /* Multiple Mask register */
                DMAModule.instance().updateEMSMapping();
                for (byte ct = 0; ct < 4; ct++) {
                    chan = getChannel(ct);
                    chan.setMask((val & 1) != 0);
                    val >>>= 1;
                }
                break;
        }
    }

    public int readControllerReg(int reg, int len) {
        DMAChannel chan;
        int ret;
        switch (reg) {
            /* read base address of DMA transfer (1st byte low part, 2nd byte high part) */
            case 0x0:
            case 0x2:
            case 0x4:
            case 0x6:
                chan = getChannel(reg >>> 1);
                flipFlop = !flipFlop;
                if (flipFlop) {
                    return chan.currAddr & 0xff;
                } else {
                    return (chan.currAddr >>> 8) & 0xff;
                }
                /* read DMA transfer count (1st byte low part, 2nd byte high part) */
            case 0x1:
            case 0x3:
            case 0x5:
            case 0x7:
                chan = getChannel(reg >>> 1);
                flipFlop = !flipFlop;
                if (flipFlop) {
                    return chan.currCnt & 0xff;
                } else {
                    return (chan.currCnt >>> 8) & 0xff;
                }
            case 0x8: /* Status Register */
                ret = 0;
                for (byte ct = 0; ct < 4; ct++) {
                    chan = getChannel(ct);
                    if (chan.tCount)
                        ret |= 1 << ct;
                    chan.tCount = false;
                    if (chan.request)
                        ret |= 1 << (4 + ct);
                }
                return ret;
            default:
                Log.logging(Log.LogTypes.DMACONTROL, Log.LogServerities.Normal,
                        "Trying to read undefined DMA port %x", reg);
                break;
        }
        return 0xffffffff;
    }
}
