package org.gutkyu.dosboxj.misc.setup;

import java.util.List;
import org.gutkyu.dosboxj.misc.setup.Value.WrongType;

public class PropertyMultival extends Property {
    protected SectionProperty section;
    protected char seperator;

    protected void makeDefaultValue() throws WrongType {
        int i = 1;
        Property p = section.getProp(0);
        if (p == null)
            return;

        String result = p.getDefaultValue().toString();
        while (((p = section.getProp(i++)) != null)) {
            String props = p.getDefaultValue().toString();
            if (props == "")
                continue;
            result += seperator;
            result += props;
        }
        Value val = new Value(result, Value.Etype.STRING);
        setVal(val, false, true);
    }

    public PropertyMultival(String propName, Changeable when, char sep) {
        super(propName, when);
        section = new SectionProperty("");
        seperator = sep;
        defaultValue = value = new Value("");
    }

    public SectionProperty getSection() {
        return section;
    }

    @Override
    public void setValue(String input) throws WrongType {
        Value val = new Value(input, Value.Etype.STRING);
        setVal(val, false, true);

        String local = input;
        int i = 0;
        Property p = section.getProp(0);
        // No properties in this section. do nothing
        if (p == null)
            return;
        int loc = -1;
        while ((p = section.getProp(i++)) != null) {
            // trim leading seperators
            // local = local.TrimStart(seperator);
            local = local.replaceFirst("^" + seperator, "");
            loc = local.indexOf(seperator);
            String in1 = "";// default value
            if (loc > -1) { // seperator found
                in1 = local.substring(0, loc);
                local = local.substring(loc);
            } else if (local.length() > 0) { // last argument
                in1 = local;
                local = "";
            }
            // Test Value. If it fails set default
            Value valtest = new Value(input, p.getType());
            if (!p.checkValue(valtest, true)) {
                makeDefaultValue();
                return;
            }
            p.setValue(in1);

        }
    }

    // TODO checkvalue stuff
    @Override
    public List<Value> getValues() {
        Property p = section.getProp(0);
        // No properties in this section. do nothing
        if (p == null)
            return suggestedValues;
        int i = 0;
        while ((p = section.getProp(i++)) != null) {
            List<Value> v = p.getValues();
            if (v.size() > 0)
                return p.getValues();
        }
        return suggestedValues;
    }

    @Override
    protected void dispose(boolean disposing) {
        if (disposing) {
            // Free other state (managed objects).
        }

        // Free your own state (unmanaged objects).
        // Set large fields to null.
        if (section != null)
            section.dispose();
        section = null;
    }
}
