package org.gutkyu.dosboxj.dos.system.drive;

import org.gutkyu.dosboxj.dos.DOSMain;
import org.gutkyu.dosboxj.dos.DOSSystem;
import org.gutkyu.dosboxj.util.CStringPt;

public final class VFile {
    protected static class VFileBlock {
        public CStringPt Name;
        public byte[] Data;
        public int Size;
        public short Date;
        public short Time;
        public VFileBlock Next;
    }

    protected static VFileBlock firstFile; // only VirtualDrive

    // (string, byte[], uint32)
    public static void register(String name, byte[] data, long size) {
        VFileBlock newFile = new VFileBlock();
        newFile.Name = CStringPt.create((int) DOSSystem.DOS_NAMELENGTH_ASCII);
        CStringPt.copy(name, newFile.Name);
        newFile.Data = data;
        newFile.Size = (int) size;
        newFile.Date = DOSMain.packDate((short) 2002, (short) 10, (short) 1);
        newFile.Time = DOSMain.packTime((short) 12, (short) 34, (short) 56);
        newFile.Next = firstFile;
        firstFile = newFile;
    }

    public static void remove(String name) {
        VFileBlock chan = firstFile;
        VFileBlock preChan = null;
        while (chan != null) {
            if (chan.Name.equals(name)) {
                if (preChan != null)
                    preChan.Next = chan.Next;
                else
                    firstFile = chan.Next;
                chan = null;
                return;
            }
            preChan = chan;
            chan = chan.Next;
        }
    }
}

