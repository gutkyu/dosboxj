package org.gutkyu.dosboxj.misc.setup;

import org.gutkyu.dosboxj.misc.setup.Value.WrongType;

class PropertyDouble extends Property {
    public PropertyDouble(String propName, Changeable when, double val) {
        super(propName, when);
        defaultValue = value = new Value(val);
    }

    @Override
    public void setValue(String input) throws WrongType {
        Value val = new Value(input, Value.Etype.DOUBLE);
        setVal(val, false, true);
    }

}
