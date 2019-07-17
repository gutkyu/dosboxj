package org.gutkyu.dosboxj.cpu;


public class DescriptorTable {
    protected int tableBase;
    protected int tableLimit;

    public int getBase() {
        return tableBase;
    }

    public int getLimit() {
        return tableLimit;
    }

    public void setBase(int baseAddress) {
        tableBase = baseAddress;
    }

    public void setLimit(int limit) {
        tableLimit = limit;
    }

    public boolean getDescriptor(int selector, Descriptor desc) {
        selector &= ~7;
        if (selector >= tableLimit)
            return false;
        desc.load(tableBase + (selector));
        return true;
    }

}
