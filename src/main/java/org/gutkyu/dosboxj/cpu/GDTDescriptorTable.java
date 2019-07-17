package org.gutkyu.dosboxj.cpu;



public class GDTDescriptorTable extends DescriptorTable {
    private int ldtBase;
    private int ldtLimit;
    private int ldtValue;

    // TODO : getDescriptor를 override 하지 않은 이유?
    public boolean getDescriptor(int selector, Descriptor desc) {
        int address = selector & ~7;
        if ((selector & 4) != 0) {
            if (address >= ldtLimit)
                return false;
            desc.load(ldtBase + address);
            return true;
        } else {
            if (address >= tableLimit)
                return false;
            desc.load(tableBase + address);
            return true;
        }
    }

    public boolean setDescriptor(int selector, Descriptor desc) {
        int address = selector & ~7;
        if ((selector & 4) != 0) {
            if (address >= ldtLimit)
                return false;
            desc.save(ldtBase + address);
            return true;
        } else {
            if (address >= tableLimit)
                return false;
            desc.save(tableBase + address);
            return true;
        }
    }

    public int sldt() {
        return ldtValue;
    }

    public boolean lldt(int value) {
        if ((value & 0xfffc) == 0) {
            ldtValue = 0;
            ldtBase = 0;
            ldtLimit = 0;
            return true;
        }
        Descriptor desc = new Descriptor();
        if (!getDescriptor(value, desc))
            return !CPU.prepareException(CPU.ExceptionGP, value);
        if (desc.type() != Descriptor.DESC_LDT)
            return !CPU.prepareException(CPU.ExceptionGP, value);
        if (desc.Saved.Seg.P == 0)
            return !CPU.prepareException(CPU.ExceptionNP, value);
        ldtBase = desc.getBase();
        ldtLimit = desc.getLimit();
        ldtValue = value;
        return true;
    }

}
