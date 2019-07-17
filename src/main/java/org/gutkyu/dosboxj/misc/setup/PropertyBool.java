package org.gutkyu.dosboxj.misc.setup;

import org.gutkyu.dosboxj.misc.setup.Value.WrongType;

public final class PropertyBool extends Property {
    public PropertyBool(String propName, Changeable when, boolean val)

    {
        super(propName, when);
        defaultValue = value = new Value(val);

    }

    @Override
    public void setValue(String input) throws WrongType {
        value.setValue(input, Value.Etype.BOOL);
    }

}
