package org.gutkyu.dosboxj.dos.system.file;



import org.gutkyu.dosboxj.util.*;

public abstract class DOSFile implements Disposable {
    public DOSFile() {
        Flags = 0;
        Name = CStringPt.create();
        RefCtr = 0;
        hdrive = (byte) 0xff;
    }

    public DOSFile(DOSFile orig) {
        Flags = orig.Flags;
        Time = orig.Time;
        Date = orig.Date;
        Attr = orig.Attr;
        RefCtr = orig.RefCtr;
        Open = orig.Open;
        Name = CStringPt.create();
        if (!orig.Name.isEmpty()) {
            Name = orig.Name;
        }
    }

    public void dispose() {
        dispose(true);
    }

    protected void dispose(boolean disposing) {
        if (disposing) {
        }
        if (Name != null)
            Name.empty();
    }

    public abstract boolean read();

    public abstract boolean read(byte[] buf, int offset, int size);

    public abstract byte getReadByte();

    public abstract int readSize();

    public abstract boolean write(byte[] buf, int offset, int size);

    public abstract boolean write(byte value, int size);

    public abstract boolean write(byte value);

    public abstract int writtenSize();

    public abstract long seek(long pos, int type);

    public abstract boolean close();

    public abstract int getInformation();

    public void setName(CStringPt name) {
        Name = name;
    }

    public CStringPt getName() {
        return Name;
    }

    public boolean isOpen() {
        return Open;
    }

    public boolean isName(CStringPt name) {
        return Name != null && Name.equalsIgnoreCase(name);
    }

    public void addRef() {
        RefCtr++;
    }

    public int removeRef() {
        return --RefCtr;
    }

    public boolean updateDateTimeFromHost() {
        return true;
    }

    public void setDrive(byte drv) {
        hdrive = drv;
    }

    public byte getDrive() {
        return hdrive;
    }

    public int Flags;
    public int Time;
    public int Date;
    public int Attr;
    public int RefCtr;
    public boolean Open;
    public CStringPt Name;
    /* Some Device Specific Stuff */
    private byte hdrive;
}
