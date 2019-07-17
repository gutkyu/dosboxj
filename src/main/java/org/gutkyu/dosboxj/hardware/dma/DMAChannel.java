package org.gutkyu.dosboxj.hardware.dma;

import org.gutkyu.dosboxj.util.RefU32Ret;

/*--------------------------- begin DmaChannel -----------------------------*/
final class DMAChannel {

    public int pageBase;// uint32
    public int baseAddr;// uint16
    public int currAddr;// uint32
    public int baseCnt;// uint16
    public int currCnt;// uint16
    public byte chanNum;
    public byte pageNum;
    public byte dma16;
    public boolean increment;
    public boolean autoInit;
    public byte tranType;
    public boolean masked;
    public boolean tCount;
    public boolean request;
    public DMACallBack callback;

    protected DMAChannel(byte num, boolean dma16) {
        masked = true;
        callback = null;
        if (num == 4)
            return;
        chanNum = num;
        this.dma16 = (byte) (dma16 ? 0x1 : 0x0);
        pageNum = 0;
        pageBase = 0;
        baseAddr = 0;
        currAddr = 0;
        baseCnt = 0;
        currCnt = 0;
        increment = true;
        autoInit = false;
        tCount = false;
        request = false;
    }

    public void doCallBack(DMAEvent _event) {
        if (callback != null)
            callback.run(this, _event);
    }

    public void setMask(boolean _mask) {
        masked = _mask;
        doCallBack(masked ? DMAEvent.MASKED : DMAEvent.UNMASKED);
    }

    public void registerCallback(DMACallBack _cb) {
        callback = _cb;
        setMask(masked);
        if (callback != null)
            raiseRequest();
        else
            clearRequest();
    }

    public void reachedTC() {
        tCount = true;
        doCallBack(DMAEvent.REACHED_TC);
    }

    public void setPage(byte val) {
        pageNum = val;
        pageBase = (pageNum >>> this.dma16) << (16 + this.dma16);
    }

    public void raiseRequest() {
        request = true;
    }

    public void clearRequest() {
        request = false;
    }

    // want = size
    // public int Read(int want, byte* buffer)
    public int read(int want, byte[] buffer, RefU32Ret refBufIdx) {
        int bufIdx = refBufIdx.U32;
        int done = 0;
        currAddr &= DMAModule.getDMAWapping();
        // again:
        while (true) {
            int left = currCnt + 1;
            if (want < left) {
                refBufIdx.U32 = bufIdx = DMAModule.instance().readBlock(pageBase, currAddr, buffer,
                        bufIdx, want, this.dma16);
                done += want;
                currAddr += want;
                currCnt -= want;
            } else {
                refBufIdx.U32 = bufIdx = DMAModule.instance().readBlock(pageBase, currAddr, buffer,
                        bufIdx, want, this.dma16);
                refBufIdx.U32 = bufIdx += left << this.dma16;
                want -= left;
                done += left;
                reachedTC();
                if (autoInit) {
                    currCnt = baseCnt;
                    currAddr = baseAddr;
                    if (want != 0)
                        continue;// goto again;

                    DMAModule.instance().updateEMSMapping();
                } else {
                    currAddr += left;
                    currCnt = 0xffff;
                    masked = true;
                    DMAModule.instance().updateEMSMapping();
                    doCallBack(DMAEvent.TRANSFEREND);
                }
            }
            break;
        }
        return done;
    }

    // want = size
    // public int Write(int want, byte* buffer)
    public int write(int want, byte[] buffer, RefU32Ret refBufIdx) {
        int bufIdx = refBufIdx.U32;
        int done = 0;
        currAddr &= DMAModule.getDMAWapping();
        // again:
        while (true) {
            int left = currCnt + 1;
            if (want < left) {
                refBufIdx.U32 = bufIdx = DMAModule.instance().writeBlock(pageBase, currAddr, buffer,
                        bufIdx, want, this.dma16);
                done += want;
                currAddr += want;
                currCnt -= want;
            } else {
                refBufIdx.U32 = bufIdx = DMAModule.instance().writeBlock(pageBase, currAddr, buffer,
                        bufIdx, left, this.dma16);
                refBufIdx.U32 = bufIdx += left << this.dma16;
                want -= left;
                done += left;
                reachedTC();
                if (autoInit) {
                    currCnt = baseCnt;
                    currAddr = baseAddr;
                    if (want != 0)
                        continue; // goto again;

                    DMAModule.instance().updateEMSMapping();
                } else {
                    currAddr += left;
                    currCnt = 0xffff;
                    masked = true;
                    DMAModule.instance().updateEMSMapping();
                    doCallBack(DMAEvent.TRANSFEREND);
                }
            }
            break;
        }
        return done;
    }
}
/*--------------------------- end DmaChannel -----------------------------*/
