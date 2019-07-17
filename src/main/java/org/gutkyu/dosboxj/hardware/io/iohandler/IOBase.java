package org.gutkyu.dosboxj.hardware.io.iohandler;



/*
 * Classes to manage the IO objects created by the various devices. The io objects will remove
 * itself on destruction.
 */
public class IOBase {
    protected boolean installed;
    protected int _port, _mask, _range;

    public IOBase() {
        installed = false;
    }
}
