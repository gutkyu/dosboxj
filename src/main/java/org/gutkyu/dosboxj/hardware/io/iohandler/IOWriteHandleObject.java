package org.gutkyu.dosboxj.hardware.io.iohandler;

import org.gutkyu.dosboxj.hardware.io.*;
import org.gutkyu.dosboxj.misc.Support;
import org.gutkyu.dosboxj.util.*;


public final class IOWriteHandleObject extends IOBase implements Disposable {
    public void install(int port, WriteHandler handler, int mask, int range) {
        if (!installed) {
            installed = true;
            _port = port;
            _mask = mask;
            _range = range;
            IO.registerWriteHandler(port, handler, mask, range);
        } else
            Support.exceptionExit("IO_writeHandler allready installed port %x", port);
    }

    public void install(int port, WriteHandler handler, int mask) {
        install(port, handler, mask, 1);
    }

    public void dispose() {
        dispose(true);
    }

    private void dispose(boolean disposing) {

        eventOnFinalization();

    }

    // 객체 소멸시 실행
    private void eventOnFinalization() {
        if (!installed)
            return;
        IO.freeWriteHandler(_port, _mask, _range);
        Log.logMsg("FreeWritehandler called with port %X", _port);
    }
}
