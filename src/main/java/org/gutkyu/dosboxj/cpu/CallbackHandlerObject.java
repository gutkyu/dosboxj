package org.gutkyu.dosboxj.cpu;

import org.gutkyu.dosboxj.hardware.memory.*;
import org.gutkyu.dosboxj.misc.*;
import org.gutkyu.dosboxj.util.*;

public final class CallbackHandlerObject implements Disposable {
    private boolean _installed;
    private int _callback;

    private enum AnonyEnum {
        NONE, SETUP, SETUPAT
    }

    private AnonyEnum _type;

    private class VectorHandlerInfo {
        public int oldVector;
        public byte interrupt;
        public boolean installed;
    }

    private VectorHandlerInfo _vectorHandler = new VectorHandlerInfo();

    public CallbackHandlerObject() {
        _installed = false;
        _type = AnonyEnum.NONE;
        _vectorHandler.installed = false;
    }

    // Implement IDisposable.
    public void dispose() {
        dispose(true);
    }

    protected void dispose(boolean disposing) {
        if (disposing) {
            eventOnFinalization();

        }
    }

    // 객체 소멸시 실행
    private void eventOnFinalization() {
        if (!_installed)
            return;
        if (_type == AnonyEnum.SETUP) {
            if (_vectorHandler.installed) {
                // See if we are the current handler. if so restore the old one
                if (Memory.realGetVec(_vectorHandler.interrupt) == getRealPointer()) {
                    Memory.realSetVec(_vectorHandler.interrupt, _vectorHandler.oldVector);
                } else
                    Log.logging(Log.LogTypes.MISC, Log.LogServerities.Warn,
                            "Interrupt vector changed on %X %s", _vectorHandler.interrupt,
                            Callback.getDescription(_callback));
            }
            Callback.removeSetup(_callback);
        } else if (_type == AnonyEnum.SETUPAT) {
            Support.exceptionExit("Callback:SETUP at not handled yet.");
        } else if (_type == AnonyEnum.NONE) {
            // Do nothing. Merely DeAllocate the callback
        } else
            Support.exceptionExit("what kind of callback is this!");

        if (Callback.CallbackDescription[_callback] != null)
            Callback.CallbackDescription[_callback] = null;
        // CALLBACK.CallBack_Description[m_callback] = null;
        Callback.deallocate(_callback);
    }

    // Install and allocate a callback.
    public void install(DOSCallbackHandler handler, Callback.Symbol type, String description) {
        if (!_installed) {
            _installed = true;
            _type = AnonyEnum.SETUP;
            _callback = Callback.allocate();
            Callback.setup(_callback, handler, type, description);
        } else
            Support.exceptionExit("Allready installed");
    }

    public void install(DOSCallbackHandler handler, Callback.Symbol type, int addr,
            String description) {
        if (!_installed) {
            _installed = true;
            _type = AnonyEnum.SETUP;
            _callback = Callback.allocate();
            Callback.setup(_callback, handler, type, addr, description);
        } else
            Support.exceptionExit("Allready installed");
    }

    // Only allocate a callback number
    public void allocate(DOSCallbackHandler handler, String description) {
        if (!_installed) {
            _installed = true;
            _type = AnonyEnum.NONE;
            _callback = Callback.allocate();
            Callback.setDescription(_callback, description);
            Callback.CallbackHandlers.set(_callback, handler);
        } else
            Support.exceptionExit("Allready installed");
    }

    public void allocate(DOSCallbackHandler handler) {
        allocate(handler, null);
    }

    // uint16()
    public int getCallback() {
        return 0xffff & _callback;
    }

    public int getRealPointer() {
        return Callback.realPointer(_callback);
    }

    public void setRealVec(byte vec) {
        if (!_vectorHandler.installed) {
            _vectorHandler.installed = true;
            _vectorHandler.interrupt = vec;
            _vectorHandler.oldVector = Memory.realSetVecAndReturnOld(vec, getRealPointer());
        } else
            Support.exceptionExit("double usage of vector handler");
    }
}
