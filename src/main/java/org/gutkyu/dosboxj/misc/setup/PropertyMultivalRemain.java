package org.gutkyu.dosboxj.misc.setup;

import org.gutkyu.dosboxj.misc.setup.Value.WrongType;

public final class PropertyMultivalRemain extends PropertyMultival {
    public PropertyMultivalRemain(String propName, Changeable when, char sep) {

        super(propName, when, sep);
    }

    @Override
    public void setValue(String input) throws WrongType {
        Value val = new Value(input, Value.Etype.STRING);
        setVal(val, false, true);

        String local = input;
        int i = 0, numberOfProperties = 0;
        Property p = section.getProp(0);
        // No properties in this section. do nothing
        if (p == null)
            return;

        while ((section.getProp(numberOfProperties)) != null)
            numberOfProperties++;

        int loc = -1;
        while ((p = section.getProp(i++)) != null) {
            // trim leading seperators
            // local = local.TrimStart(seperator);
            local = local.replaceFirst("^" + seperator, "");
            loc = local.indexOf(seperator);
            String in1 = "";// default value
            /*
             * when i == number_of_properties add the total line. (makes more then one string
             * argument possible for parameters of cpu)
             */
            if (loc > -1 && i < numberOfProperties) { // seperator found
                in1 = local.substring(0, loc);
                local = local.substring(loc);
            } else if (local.length() > 0) { // last argument or last property
                in1 = local;
                local = "";
            }
            // Test Value. If it fails set default
            Value valtest = new Value(in1, p.getType());
            if (!p.checkValue(valtest, true)) {
                makeDefaultValue();
                return;
            }
            p.setValue(in1);
        }
    }
}
