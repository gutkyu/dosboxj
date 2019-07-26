package org.gutkyu.dosboxj.cpu;

import org.gutkyu.dosboxj.hardware.memory.*;
import org.gutkyu.dosboxj.misc.*;
import org.gutkyu.dosboxj.util.*;

public final class CallbackHandlerObject implements Disposable {
    private boolean installed;
    private int callback;

    private enum AnonyEnum {
        NONE, SETUP, SETUPAT
    }

    private AnonyEnum anonyType;

    private class VectorHandlerInfo {
        public int oldVector;
        public int interrupt;
        public boolean installed;
    }

    private VectorHandlerInfo vectorHandler = new VectorHandlerInfo();

    public CallbackHandlerObject() {
        installed = false;
        anonyType = AnonyEnum.NONE;
        vectorHandler.installed = false;
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
        if (!installed)
            return;
        if (anonyType == AnonyEnum.SETUP) {
            if (vectorHandler.installed) {
                // See if we are the current handler. if so restore the old one
                if (Memory.realGetVec(vectorHandler.interrupt) == getRealPointer()) {
                    Memory.realSetVec(vectorHandler.interrupt, vectorHandler.oldVector);
                } else
                    Log.logging(Log.LogTypes.MISC, Log.LogServerities.Warn,
                            "Interrupt vector changed on %X %s", vectorHandler.interrupt,
                            Callback.getDescription(callback));
            }
            Callback.removeSetup(callback);
        } else if (anonyType == AnonyEnum.SETUPAT) {
            Support.exceptionExit("Callback:SETUP at not handled yet.");
        } else if (anonyType == AnonyEnum.NONE) {
            // Do nothing. Merely DeAllocate the callback
        } else
            Support.exceptionExit("what kind of callback is this!");

        if (Callback.CallbackDescription[callback] != null)
            Callback.CallbackDescription[callback] = null;
        // CALLBACK.CallBack_Description[m_callback] = null;
        Callback.deallocate(callback);
    }

    // Install and allocate a callback.
    public void install(DOSCallbackHandler handler, Callback.Symbol type, String description) {
        if (!installed) {
            installed = true;
            anonyType = AnonyEnum.SETUP;
            callback = Callback.allocate();
            Callback.setup(callback, handler, type, description);
        } else
            Support.exceptionExit("Allready installed");
    }

    public void install(DOSCallbackHandler handler, Callback.Symbol type, int addr,
            String description) {
        if (!installed) {
            installed = true;
            anonyType = AnonyEnum.SETUP;
            callback = Callback.allocate();
            Callback.setup(callback, handler, type, addr, description);
        } else
            Support.exceptionExit("Allready installed");
    }

    // Only allocate a callback number
    public void allocate(DOSCallbackHandler handler, String description) {
        if (!installed) {
            installed = true;
            anonyType = AnonyEnum.NONE;
            callback = Callback.allocate();
            Callback.setDescription(callback, description);
            Callback.CallbackHandlers.set(callback, handler);
        } else
            Support.exceptionExit("Allready installed");
    }

    public void allocate(DOSCallbackHandler handler) {
        allocate(handler, null);
    }

    // uint16()
    public int getCallback() {
        return 0xffff & callback;
    }

    public int getRealPointer() {
        return Callback.realPointer(callback);
    }

    public void setRealVec(int vec) {
        if (!vectorHandler.installed) {
            vectorHandler.installed = true;
            vectorHandler.interrupt = vec;
            vectorHandler.oldVector = Memory.realSetVecAndReturnOld(vec, getRealPointer());
        } else
            Support.exceptionExit("double usage of vector handler");
    }
}
