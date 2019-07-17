package org.gutkyu.dosboxj.util;

@FunctionalInterface
public interface DOSAction2<T, S> {
    public void run(T param1, S param2);
}
