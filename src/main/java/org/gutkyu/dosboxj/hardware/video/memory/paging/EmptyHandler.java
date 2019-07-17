package org.gutkyu.dosboxj.hardware.video.memory.paging;

import org.gutkyu.dosboxj.hardware.memory.paging.*;

public final class EmptyHandler extends PageHandler {
    public EmptyHandler() {
        Flags = Paging.PFLAG_NOCODE;
    }

    @Override
    public int readB(int addr) {
        // LOG(LOG_VGA, LOG_NORMAL ) ( "Read from empty memory space at %x", addr );
        return 0xff;
    }

    @Override
    public void writeB(int addr, int val) {
        // LOG(LOG_VGA, LOG_NORMAL ) ( "Write %x to empty memory space at %x", val, addr );
    }
}
