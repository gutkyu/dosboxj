package org.gutkyu.dosboxj.misc.setup;

import org.gutkyu.dosboxj.misc.setup.Value.WrongType;
import org.gutkyu.dosboxj.util.Disposable;

public abstract class ModuleBase implements Disposable {
    protected Section _configuration;

    public ModuleBase(Section configuration) {
        _configuration = configuration;
    }

    public boolean changeConfig(Section section) throws WrongType {
        return false;
    }

    // Implement IDisposable.
    public void dispose() {
        dispose(true);
    }

    protected void dispose(boolean disposing) {
        if (disposing) {
            // Free other state (managed objects).
            _configuration.dispose();
            _configuration = null;
        }

        // Free your own state (unmanaged objects).
        // Set large fields to null.
    }

}
