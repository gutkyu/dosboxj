package org.gutkyu.dosboxj.misc.setup;

import org.gutkyu.dosboxj.misc.setup.Value.WrongType;

public final class PropertyHex extends Property {
    public PropertyHex(String propName, Changeable when, Hex val) {
        super(propName, when);
        defaultValue = value = new Value(val);
    }

    @Override
    public void setValue(String input) throws WrongType {
        Value val = new Value(input, Value.Etype.HEX);
        setVal(val, false, true);
    }

}
