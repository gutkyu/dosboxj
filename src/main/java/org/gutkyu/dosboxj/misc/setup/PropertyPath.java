package org.gutkyu.dosboxj.misc.setup;

import org.gutkyu.dosboxj.misc.*;
import org.gutkyu.dosboxj.misc.setup.Value.WrongType;

public final class PropertyPath extends PropertyString {
    public String realpath;

    public PropertyPath(String propName, Changeable when, String val) {
        super(propName, when, val);
        defaultValue = value = new Value(val);
        realpath = val;
    }

    @Override
    public void setValue(String input) throws WrongType {
        // Special version to merge realpath with it

        Value val = new Value(input, Value.Etype.STRING);
        setVal(val, false, true);

        if (input.length() == 0) {
            realpath = "";
            return;
        }
        String workcopy = input;
        workcopy = Cross.resolveHomedir(workcopy); // Parse ~ and friends
        // Prepend config directory in it exists. Check for absolute paths later
        if (SetupModule.CurrentConfigDir == null)
            realpath = workcopy;
        else
            realpath = SetupModule.CurrentConfigDir + Cross.FILESPLIT + workcopy;
        // Absolute paths

        if (workcopy.length() > 2 && workcopy.charAt(1) == ':')
            realpath = workcopy;

    }

}
