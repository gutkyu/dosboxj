package org.gutkyu.dosboxj.misc.setup;



public final class Hex {
    private int _hex;

    public Hex(int input) {
        _hex = input;
    }

    public Hex() {
        _hex = 0;
    }

    public boolean equals(Hex object) {
        return this._hex == object._hex;
    }

    public boolean notEquals(Hex object) {
        return !equals(object);
    }

    public int getInt() {
        return this._hex;
    }

    public static final Hex HexZero = new Hex(0);
}
