package org.gutkyu.dosboxj.misc.setup;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.LinkedList;
import org.gutkyu.dosboxj.misc.setup.Value.WrongType;

public final class SectionProperty extends Section {
    private LinkedList<Property> properties = new LinkedList<Property>();

    public SectionProperty(String sectionName) {
        super(sectionName);
    }

    public PropertyInt addInt(String propName, Property.Changeable when, int val) {
        PropertyInt test = new PropertyInt(propName, when, val);
        properties.addLast(test);
        return test;
    }

    public PropertyInt addInt(String propName, Property.Changeable when) {
        return addInt(propName, when, 0);
    }

    public PropertyString addString(String propName, Property.Changeable when, String val) {
        PropertyString test = new PropertyString(propName, when, val);
        properties.addLast(test);
        return test;
    }

    public PropertyString addString(String propName, Property.Changeable when) {
        return addString(propName, when, null);
    }

    public PropertyPath addPath(String propName, Property.Changeable when, String val) {
        PropertyPath test = new PropertyPath(propName, when, val);
        properties.addLast(test);
        return test;
    }

    public PropertyPath addPath(String propName, Property.Changeable when) {
        return addPath(propName, when, null);
    }

    public PropertyBool addBool(String propName, Property.Changeable when, boolean val) {
        PropertyBool test = new PropertyBool(propName, when, val);
        properties.addLast(test);
        return test;
    }

    public PropertyBool addBool(String propName, Property.Changeable when) {
        return addBool(propName, when, false);
    }

    public PropertyHex addHex(String propName, Property.Changeable when) {
        return addHex(propName, when, Hex.HexZero);
    }

    public PropertyHex addHex(String propName, Property.Changeable when, Hex val) {
        PropertyHex test = new PropertyHex(propName, when, val);
        properties.addLast(test);
        return test;
    }

    // void Add_double(char final * final _propname, double _value=0.0);
    public PropertyMultival addMulti(String propName, Property.Changeable when, char sep) {
        PropertyMultival test = new PropertyMultival(propName, when, sep);
        properties.addLast(test);
        return test;
    }

    public PropertyMultivalRemain addMultiRemain(String propName, Property.Changeable when,
            char sep) {
        PropertyMultivalRemain test = new PropertyMultivalRemain(propName, when, sep);
        properties.addLast(test);
        return test;
    }

    public Property getProp(int index) {

        for (Property prop : properties) {
            if (index-- == 0)
                return (prop);
        }
        return null;
    }

    public int getInt(String propName) throws WrongType {
        for (Property prop : properties) {
            if (!prop.PropName.equals(propName))
                continue;
            return prop.getValue().getInt();
        }
        return 0;
    }

    public String getString(String propName) throws WrongType {
        for (Property prop : properties) {
            if (!prop.PropName.equals(propName))
                continue;
            return prop.getValue().getString();
        }
        return "";
    }

    public boolean getBool(String propName) throws WrongType {
        for (Property prop : properties) {
            if ((!prop.PropName.equals(propName)))
                continue;
            return prop.getValue().getBoolean();
        }
        return false;
    }

    public Hex getHex(String propName) throws WrongType {
        for (Property prop : properties) {
            if (!prop.PropName.equals(propName))
                continue;
            return prop.getValue().getHex();
        }
        return Hex.HexZero;
    }

    public double getDouble(String propName) throws WrongType {
        for (Property prop : properties) {
            if (!prop.PropName.equals(propName))
                continue;
            return prop.getValue().getDouble();
        }
        return 0.0;
    }

    public PropertyPath getPath(String propName) {
        for (Property prop : properties) {
            if (!prop.PropName.equals(propName))
                continue;
            return prop instanceof PropertyPath ? (PropertyPath) prop : null;
        }
        return null;
    }

    public PropertyMultival getMultival(String propName) {
        for (Property prop : properties) {
            if (!prop.PropName.equals(propName))
                continue;
            return prop instanceof PropertyMultival ? (PropertyMultival) prop : null;
        }

        return null;
    }

    public PropertyMultivalRemain getMultivalRemain(String propName) {
        for (Property prop : properties) {
            if (prop.PropName.equals(propName)) {
                if (prop instanceof PropertyMultivalRemain)
                    return null;
                else
                    return (PropertyMultivalRemain) prop;
            }
        }

        return null;
    }

    // TODO double c_str
    @Override
    public void handleInputline(String gegevens) throws WrongType {
        String str1 = gegevens;
        int loc = str1.indexOf('=');
        if (loc < 0)
            return;
        String name = str1.substring(0, loc).trim();
        String val = str1.substring(loc + 1).trim();
        for (Property prop : properties) {
            if (prop.PropName.equalsIgnoreCase(name)) {
                prop.setValue(val);
                return;
            }
        }
    }

    @Override
    public void printData(BufferedWriter fileWriter) throws IOException {
        /* Now print out the individual section entries */
        for (Property prop : properties) {
            fileWriter.write(String.format("%1$s=%2$s\n", prop.PropName, prop.getValue()));
        }
    }

    // TODO geen noodzaak voor 2 keer c_str
    @Override
    public String getPropValue(String property) {
        for (Property prop : properties) {
            if (prop.PropName.equalsIgnoreCase(property)) {
                return property;
            }
        }

        return SetupModule.NO_SUCH_PROPERTY;
    }
    // ExecuteDestroy should be here else the destroy functions use destroyed properties


    //// Implement IDisposable.
    // public void dispose()
    // {
    // dispose(true);
    // }

    @Override
    protected void dispose(boolean disposing) {
        if (disposing) {
            // Free other state (managed objects).

            // ExecuteDestroy should be here else the destroy functions use destroyed properties
            executeDestroy(true);
            /* Delete properties themself (properties stores the pointer of a prop */
            for (Property prop : properties) {
                prop.dispose();
            }
        }

        // Free your own state (unmanaged objects).
        // Set large fields to null.


    }
}
