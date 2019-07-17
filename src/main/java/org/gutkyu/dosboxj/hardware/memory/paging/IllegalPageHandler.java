package org.gutkyu.dosboxj.hardware.memory.paging;



import org.gutkyu.dosboxj.cpu.*;
import org.gutkyu.dosboxj.util.*;

public final class IllegalPageHandler extends PageHandler {
    public IllegalPageHandler() {
        Flags = Paging.PFLAG_INIT | Paging.PFLAG_NOCODE;
    }

    private static int lcountr = 0;

    @Override
    public int readB(int addr) {
        if (lcountr < 1000) {
            lcountr++;
            Log.logMsg("Illegal read from %x, CS:IP %8x:%8x", addr,
                    Register.segValue(Register.SEG_NAME_CS), Register.getRegEIP());
        }
        return 0;
    }

    private static int lcountw = 0;

    @Override
    public void writeB(int addr, int val) {
        if (lcountw < 1000) {
            lcountw++;
            Log.logMsg("Illegal write to %x, CS:IP %8x:%8x", addr,
                    Register.segValue(Register.SEG_NAME_CS), Register.getRegEIP());
        }
    }
}
