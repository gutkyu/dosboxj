package org.gutkyu.dosboxj.hardware.io.iohandler;

import org.gutkyu.dosboxj.hardware.io.*;
import org.gutkyu.dosboxj.misc.*;
import org.gutkyu.dosboxj.util.Disposable;


public final class IOReadHandleObject extends IOBase implements Disposable {
    public void install(int port, ReadHandler handler, int mask, int range) {
        if (!installed) {
            installed = true;
            _port = port;
            _mask = mask;
            _range = range;
            IO.registerReadHandler(port, handler, mask, range);
        } else
            Support.exceptionExit("IO_readHandler allready installed port %x", port);
    }

    public void install(int port, ReadHandler handler, int mask) {
        install(port, handler, mask, 1);
    }


    // Implement IDisposable.
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
        IO.freeReadHandler(_port, _mask, _range);
    }
}
