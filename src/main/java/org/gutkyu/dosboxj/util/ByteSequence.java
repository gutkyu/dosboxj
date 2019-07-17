package org.gutkyu.dosboxj.util;

public interface ByteSequence {
    public void goFirst();

    public int next() throws DOSException;

    public boolean hasNext();
}
