package org.gutkyu.dosboxj.hardware;

@FunctionalInterface
public interface EventHandler {
    public void raise(int val);
}
