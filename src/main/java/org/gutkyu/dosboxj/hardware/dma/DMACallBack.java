package org.gutkyu.dosboxj.hardware.dma;

@FunctionalInterface
public interface DMACallBack {
    void run(DMAChannel chan, DMAEvent _event);
}
