package org.gutkyu.dosboxj.misc.setup;

import java.util.ArrayList;
import java.util.List;
import org.gutkyu.dosboxj.misc.*;
import org.gutkyu.dosboxj.misc.setup.Value.WrongType;
import org.gutkyu.dosboxj.util.*;

public abstract class Property {

    public enum Changeable {
        Always, WhenIdle, OnlyAtStart
    }

    public final String PropName;
    protected final Changeable change;
    protected Value value;
    protected List<Value> suggestedValues = new ArrayList<Value>();
    protected Value defaultValue;

    public Property(String propName, Changeable when) {
        PropName = propName;
        change = when;
    }



    public void setValues(String[] input) throws WrongType {
        Value.Etype type = defaultValue.type;
        int i = 0;
        while (i < input.length) {
            Value val = new Value(input[i], type);
            suggestedValues.add(val);
            i++;
        }
    }

    public void setHelp(String input) {
        String result = "CONFIG_" + PropName;
        result = result.toUpperCase();
        Message.addMsg(result, input);
    }

    public String getHelp() {
        String result = "CONFIG_" + PropName;
        result = result.toUpperCase();
        return Message.get(result);
    }

    public abstract void setValue(String str) throws WrongType;

    public Value getValue() {
        return value;
    }

    public Value getDefaultValue() {
        return defaultValue;
    }

    // CheckValue returns true if value is in suggested_values;
    // Type specific properties are encouraged to override this and check for type
    // specific features.
    public boolean checkValue(Value input, boolean warn) throws WrongType {
        if (suggestedValues.size() == 0)
            return true;
        for (Value val : suggestedValues) {
            if (val == input) { // Match!
                return true;
            }
        }
        if (warn)
            Log.logMsg(
                    "\"%s\" is not a valid value for variable: %s.\nIt might now be reset to the default value: %s",
                    input, PropName, defaultValue);
        return false;
    }

    // Set interval value to in or default if in is invalid. force always sets the value.
    public void setVal(Value input, boolean forced, boolean warn) throws WrongType {
        if (forced || checkValue(input, warn))
            value = input;
        else
            value = defaultValue;
    }

    public void setVal(Value input, boolean forced) throws WrongType {
        setVal(input, forced, true);
    }

    public List<Value> getValues() {
        return suggestedValues;
    }

    public Value.Etype getType() {
        return defaultValue.type;
    }


    // Implement IDisposable.
    public void dispose() {
        dispose(true);
    }

    protected void dispose(boolean disposing) {
        if (disposing) {
            // Free other state (managed objects).
        }

        // Free your own state (unmanaged objects).
        // Set large fields to null.

    }
}
