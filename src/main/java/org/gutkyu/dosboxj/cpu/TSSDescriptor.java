package org.gutkyu.dosboxj.cpu;


public final class TSSDescriptor extends Descriptor {
    public boolean isBusy() {
        return (Saved.Seg.Type & 2) != 0;
    }

    public boolean is386() {
        return (Saved.Seg.Type & 8) != 0;
    }

    public void setBusy(boolean busy) {
        if (busy)
            Saved.Seg.Type |= 2;
        else
            Saved.Seg.Type &= ~(int) 2;
        Saved.updateSDescriptor();// seg의 type을 모두 반영
    }
}
