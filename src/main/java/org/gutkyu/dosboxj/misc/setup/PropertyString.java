package org.gutkyu.dosboxj.misc.setup;

import org.gutkyu.dosboxj.misc.setup.Value.WrongType;
import org.gutkyu.dosboxj.util.*;

public class PropertyString extends Property {
    public PropertyString(String propName, Changeable when, String val) {
        super(propName, when);
        defaultValue = value = new Value(val);
    }

    @Override
    public void setValue(String input) throws WrongType {
        // Special version for lowcase stuff
        String temp = input;
        // suggested values always case insensitive.
        // If there are none then it can be paths and such which are case sensitive
        if (suggestedValues.size() > 0)
            temp = temp.toLowerCase();
        Value val = new Value(temp, Value.Etype.STRING);
        setVal(val, false, true);
    }

    @Override
    public boolean checkValue(Value input, boolean warn) {
        if (suggestedValues.size() == 0)
            return true;
        for (Value val : suggestedValues) {
            if (val == input) { // Match!
                return true;
            }
            if (val.toString() == "%u") {
                String tmpStr = input.toString();
                tmpStr = tmpStr.trim();
                // if(sscanf(in.toString().c_str(),"%u",&value) == 1)
                if (tmpStr.length() > 0 && Character.isDigit(tmpStr.charAt(0))) {
                    return true;
                }
            }
        }
        if (warn)
            Log.logMsg(
                    "\"%s\" is not a valid value for variable: %s.\nIt might now be reset it to default value: %s",
                    input, PropName, defaultValue);
        return false;
    }

}
