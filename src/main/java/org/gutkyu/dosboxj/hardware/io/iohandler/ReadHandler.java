package org.gutkyu.dosboxj.hardware.io.iohandler;

@FunctionalInterface
public interface ReadHandler {
    public int run(int port, int iolen);
}
