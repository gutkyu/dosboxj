package org.gutkyu.dosboxj.misc.setup;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import org.gutkyu.dosboxj.misc.setup.Value.WrongType;
import org.gutkyu.dosboxj.util.Disposable;
import org.gutkyu.dosboxj.util.DOSAction1;

public abstract class Section implements Disposable {
    protected final String NO_SUCH_PROPERTY = "PROP_NOT_EXIST";

    /*
     * Wrapper class around startup and shutdown functions. the variable canchange indicates it can
     * be called on configuration changes
     */
    private class FunctionWrapper {
        public DOSAction1<Section> Func;
        public boolean CanChange;

        public FunctionWrapper(DOSAction1<Section> func, boolean ch) {
            Func = func;
            CanChange = ch;
        }
    }

    private LinkedList<FunctionWrapper> _initFunctions = new LinkedList<FunctionWrapper>();
    private LinkedList<FunctionWrapper> _destroyfunctions = new LinkedList<FunctionWrapper>();
    private String _sectionName;

    public Section(String sectionName) {
        _sectionName = sectionName;
    }

    public void addInitFunction(DOSAction1<Section> func, boolean canChange) {
        _initFunctions.addLast(new FunctionWrapper(func, canChange));
    }

    public void addInitFunction(DOSAction1<Section> func) {
        addInitFunction(func, false);
    }

    public void addDestroyFunction(DOSAction1<Section> func, boolean canChange) {
        _destroyfunctions.addFirst(new FunctionWrapper(func, canChange));
    }

    public void addDestroyFunction(DOSAction1<Section> func) {
        addDestroyFunction(func, false);
    }

    public void executeInit(boolean initall) {
        for (FunctionWrapper fWrap : _initFunctions) {
            if (initall || fWrap.CanChange)
                fWrap.Func.run(this);
        }

    }

    public void executeInit() {
        executeInit(true);
    }

    public void executeDestroy(boolean destroyAll) {
        Iterator it = _destroyfunctions.iterator();

        for (FunctionWrapper fWrap = (FunctionWrapper) it.next(); fWrap != null;)
            if (destroyAll || fWrap.CanChange) {
                fWrap.Func.run(this);
                _destroyfunctions.remove(fWrap); // Remove destroyfunction once used
            } else
                fWrap = (FunctionWrapper) it.next();


    }

    public void executeDestroy() {
        executeDestroy(true);
    }

    public String getName() {
        return _sectionName;
    }

    public abstract String getPropValue(String property);

    public abstract void handleInputline(String line) throws WrongType;

    public abstract void printData(BufferedWriter fileWriter) throws IOException;

    // Implement IDisposable.
    public void dispose() {
        dispose(true);
    }

    protected void dispose(boolean disposing) {
        if (disposing) {
            // Free other state (managed objects).
        }

        // Free your own state (unmanaged objects).
        // Set large fields to null.
        _destroyfunctions = null;
        _initFunctions = null;
        _sectionName = null;
    }
}
