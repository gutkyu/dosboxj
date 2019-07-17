package org.gutkyu.dosboxj.hardware.io.iohandler;

@FunctionalInterface
public interface WriteHandler {
    public void run(int port, int val, int iolen);
}
