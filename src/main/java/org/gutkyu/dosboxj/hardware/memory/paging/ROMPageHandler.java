package org.gutkyu.dosboxj.hardware.memory.paging;


import org.gutkyu.dosboxj.util.*;

public final class ROMPageHandler extends RAMPageHandler {
    public ROMPageHandler() {
        Flags = Paging.PFLAG_READABLE | Paging.PFLAG_HASROM;
    }

    @Override
    public void writeB(int addr, int val) {
        Log.logging(Log.LogTypes.CPU, Log.LogServerities.Error, "Write %x to rom at %x", val, addr);
    }

    @Override
    public void writeW(int addr, int val) {
        Log.logging(Log.LogTypes.CPU, Log.LogServerities.Error, "Write %x to rom at %x", val, addr);
    }

    @Override
    public void writeD(int addr, int val) {
        Log.logging(Log.LogTypes.CPU, Log.LogServerities.Error, "Write %x to rom at %x", val, addr);
    }
}
