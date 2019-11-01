package org.gutkyu.dosboxj.misc.setup;

import org.gutkyu.dosboxj.misc.setup.Value.WrongType;
import org.gutkyu.dosboxj.util.Log;

public final class PropertyInt extends Property {
    private Value min, max;

    public PropertyInt(String propName, Property.Changeable when, int val) {
        super(propName, when);
        defaultValue = value = new Value(val);
        min = max = new Value(-1);
    }

    public PropertyInt(String propName, Property.Changeable when, int min, int max, int val) {
        super(propName, when);
        defaultValue = value = new Value(val);
        this.min = new Value(min);
        this.max = new Value(max);
    }

    public void setMinMax(Value min, Value max) {
        this.min = min;
        this.max = max;
    }

    public void setMinMax(int min, int max) {
        this.min = new Value(min);
        this.max = new Value(max);
    }

    @Override
    public void setValue(String input) throws WrongType {
        Value val = new Value(input, Value.Etype.INT);
        setVal(val, false, true);

    }

    @Override
    public boolean checkValue(Value input, boolean warn) throws WrongType {
        if (suggestedValues.size() == 0 && super.checkValue(input, warn))
            return true;
        // No >= and <= in Value type and == is ambigious
        int mi = min.getInt();
        int ma = max.getInt();
        int va = input.getInt();
        if (mi == -1 && ma == -1)
            return true;
        if (va >= mi && va <= ma)
            return true;
        if (warn)
            Log.logMsg(
                    "%s lies outside the range %s-%s for variable: %s.\nIt might now be reset to the default value: %s",
                    input, min, max, PropName, defaultValue);
        return false;
    }

}
