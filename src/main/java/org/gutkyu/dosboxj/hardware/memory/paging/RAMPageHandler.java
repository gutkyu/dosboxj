package org.gutkyu.dosboxj.hardware.memory.paging;

import org.gutkyu.dosboxj.hardware.memory.*;


public class RAMPageHandler extends PageHandler {
    public RAMPageHandler() {
        Flags = Paging.PFLAG_READABLE | Paging.PFLAG_WRITEABLE;
    }

    @Override
    public byte[] getHostMemory() {
        return Memory.getMemAlloc();
    }

    @Override
    public int getHostReadPt(int physPage) {
        // return MemBase + phys_page * MEM_PAGESIZE;
        return Memory.MemBase + physPage * Memory.MEM_PAGESIZE;
    }

    @Override
    public int getHostWritePt(int physPage) {
        // return MemBase + phys_page * MEM_PAGESIZE;
        return Memory.MemBase + physPage * Memory.MEM_PAGESIZE;
    }

}
