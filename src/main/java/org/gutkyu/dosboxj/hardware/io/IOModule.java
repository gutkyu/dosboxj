package org.gutkyu.dosboxj.hardware.io;


import org.gutkyu.dosboxj.misc.setup.*;

public final class IOModule extends ModuleBase {
    public IOModule(Section configuration) {
        super(configuration);
        IO.iofQueue.used = 0;
        IO.freeReadHandler(0, IO.IO_MA, IO.IO_MAX);
        IO.freeWriteHandler(0, IO.IO_MA, IO.IO_MAX);
    }


    private static IOModule _io = null;

    private static void destroy(Section sect) {
        _io.dispose();
        _io = null;
    }

    public static void init(Section sect) {
        _io = new IOModule(sect);
        sect.addDestroyFunction(IOModule::destroy);
    }

}
