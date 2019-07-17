package org.gutkyu.dosboxj.misc.setup;



import org.gutkyu.dosboxj.misc.*;
import org.gutkyu.dosboxj.util.DOSException;

public final class Value {
    /*
     * Multitype storage container that is aware of the currently stored type in it. Value st =
     * "hello"; Value in = 1; st = 12 //Exception in = 12 //works
     */
    private Hex _hex;
    private boolean _bool;
    private int _int;
    private String _string;
    private double _double;

    public class WrongType extends DOSException {
    } // Conversion error class
    public enum Etype {
        NONE, HEX, BOOL, INT, STRING, DOUBLE, CURRENT
    }

    public Etype type;

    /* Constructors */
    public Value() {
        _string = null;
        type = Etype.NONE;
    }

    public Value(Hex input) {
        _hex = input;
        type = Etype.HEX;
    }

    public Value(int input) {
        _int = input;
        type = Etype.INT;
    }

    public Value(boolean input) {
        _bool = input;
        type = Etype.BOOL;
    }

    public Value(double input) {
        _double = input;
        type = Etype.DOUBLE;
    }

    public Value(String input) {
        _string = input;
        type = Etype.STRING;
    }

    public Value(Value input) {
        _string = null;
        plaincopy(input);
    }

    // ~Value() { destroy();}
    public Value(String input, Etype _t) throws WrongType {
        _string = null;
        type = Etype.NONE;
        setValue(input, _t);
    }


    private boolean equal(Value object) {
        if (this.equals(object))
            return true;
        if (this.type != object.type)
            return false;
        switch (this.type) {
            case BOOL:
                if (this._bool == object._bool)
                    return true;
                break;
            case INT:
                if (this._int == object._int)
                    return true;
                break;
            case HEX:
                if (this._hex == object._hex)
                    return true;
                break;
            case DOUBLE:
                if (this._double == object._double)
                    return true;
                break;
            case STRING:
                if (this._string == object._string)
                    return true;
                break;
            default:
                Support.exceptionExit("comparing stuff that doesn't make sense");
                break;
        }
        return false;
    }

    public boolean notEquals(Value b) {
        return !this.equal(b);
    }

    public boolean getBoolean() throws WrongType {
        if (this.type != Etype.BOOL)
            throw new WrongType();
        return this._bool;
    }

    public Hex getHex() throws WrongType {
        if (this.type != Etype.HEX)
            throw new WrongType();
        return this._hex;
    }

    public int getInt() throws WrongType {
        if (this.type != Etype.INT)
            throw new WrongType();
        return this._int;
    }

    public double getDouble() throws WrongType {
        if (this.type != Etype.DOUBLE)
            throw new WrongType();
        return this._double;
    }

    public String getString() throws WrongType {
        if (this.type != Etype.STRING)
            throw new WrongType();
        return this._string;
    }

    public void setValue(String input, Etype _type) throws WrongType {
        /*
         * Throw exception if the current type isn't the wanted type Unless the wanted type is
         * current.
         */
        if (_type == Etype.CURRENT && type == Etype.NONE)
            throw new WrongType();
        if (_type != Etype.CURRENT) {
            if (type != Etype.NONE && type != _type)
                throw new WrongType();
            type = _type;
        }
        switch (type) {
            case HEX:
                setHex(input);
                break;
            case INT:
                setInt(input);
                break;
            case BOOL:
                setBool(input);
                break;
            case STRING:
                setString(input);
                break;
            case DOUBLE:
                setDouble(input);
                break;

            case NONE:
            case CURRENT:
            default:
                /* Shouldn't happen!/Unhandled */
                throw new WrongType();
        }
    }

    public void setValue(String input) throws WrongType {
        setValue(input, Etype.CURRENT);
    }

    @Override
    public String toString() {
        String ret = null;
        switch (type) {
            case HEX:
                // ret= Convert.toString(_hex,(int)System.Globalization.NumberStyles.HexNumber) ;
                ret = Integer.toHexString(_hex.getInt());
                break;
            case INT:
                ret = Integer.toString(_int);
                break;
            case BOOL:
                ret = Boolean.toString(_bool);
                break;
            case STRING:
                ret = _string;
                break;
            case DOUBLE:
                ret = String.format("%.2$", _double);
                break;
            case NONE:
            case CURRENT:
            default:
                Support.exceptionExit("toString messed up ?");
                break;
        }
        return ret;
    }

    private void destroy() {
        if (type == Etype.STRING)
            _string = null;
    }

    private Value copy(Value input) throws WrongType {
        if (this != input) { // Selfassigment!
            if (type != Etype.NONE && type != input.type)
                throw new WrongType();
            destroy();
            plaincopy(input);
        }
        return this;
    }

    private void plaincopy(Value input) {
        type = input.type;
        _int = input._int;
        _double = input._double;
        _bool = input._bool;
        _hex = input._hex;
        if (type == Etype.STRING)
            _string = input._string;
    }

    private void setHex(String input) {
        _hex = new Hex(Integer.parseInt(input, 16));
    }

    private void setInt(String input) {
        _int = Integer.parseInt(input);
    }

    private void setBool(String input) {
        _bool = true;
        input = input.toLowerCase();
        _bool = Boolean.parseBoolean(input);
        /* valid false entries: 0 ,d*, f*, off everything else gets true */
        if (input == null || input.isEmpty())
            return;
        char c = input.charAt(0);
        if (c == '0' || c == 'd' || c == 'f' || input == "off")
            _bool = false;
    }

    private void setString(String input) {
        _string = input;
    }

    private void setDouble(String input) {
        _double = Double.parseDouble(input);
    }
}
