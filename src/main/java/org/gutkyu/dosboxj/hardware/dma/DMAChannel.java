package org.gutkyu.dosboxj.hardware.dma;

/*--------------------------- begin DmaChannel -----------------------------*/
final class DMAChannel {

    public int pageBase;// uint32
    public int baseAddr;// uint16
    public int currAddr;// uint32
    public int baseCnt;// uint16
    public int currCnt;// uint16
    public int chanNum;// uint8
    public int pageNum;// uint8
    public byte dma16;
    public boolean increment;
    public boolean autoInit;
    public byte tranType;
    public boolean masked;
    public boolean tCount;
    public boolean request;
    public DMACallBack callback;

    // (uint8, bool)
    protected DMAChannel(int num, boolean dma16) {
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

    public void setMask(boolean val) {
        masked = val;
        doCallBack(masked ? DMAEvent.MASKED : DMAEvent.UNMASKED);
    }

    public void registerCallback(DMACallBack cb) {
        callback = cb;
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

    public void setPage(int val) {
        pageNum = 0xff & val;
        pageBase = (pageNum >>> this.dma16) << (16 + this.dma16);
    }

    public void raiseRequest() {
        request = true;
    }

    public void clearRequest() {
        request = false;
    }

    public int returnedReadIdx;

    // want = size
    // public int Read(int want, byte* buffer)
    // final read idx -> returnedReadIdx
    public int read(int want, byte[] buffer, int offset) {
        int done = 0;
        currAddr &= DMAModule.getDMAWapping();
        // again:
        while (true) {
            int left = currCnt + 1;
            if (want < left) {
                returnedReadIdx = DMAModule.instance().readBlock(pageBase, currAddr, buffer, offset,
                        want, this.dma16);
                done += want;
                currAddr += want;
                currCnt -= want;
            } else {
                returnedReadIdx = DMAModule.instance().readBlock(pageBase, currAddr, buffer, offset,
                        want, this.dma16);
                returnedReadIdx += left << this.dma16;
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

    public int returnedWrittenIdx;

    // want = size
    // public int Write(int want, byte* buffer)
    // final written index -> returnedWrittenIdx
    public int write(int want, byte[] buffer, int offset) {
        int done = 0;
        currAddr &= DMAModule.getDMAWapping();
        // again:
        while (true) {
            int left = currCnt + 1;
            if (want < left) {
                returnedWrittenIdx = DMAModule.instance().writeBlock(pageBase, currAddr, buffer,
                        offset, want, this.dma16);
                done += want;
                currAddr += want;
                currCnt -= want;
            } else {
                returnedWrittenIdx = DMAModule.instance().writeBlock(pageBase, currAddr, buffer,
                        offset, left, this.dma16);
                returnedWrittenIdx += left << this.dma16;
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
