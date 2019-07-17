package org.gutkyu.dosboxj.dos.system.device;

import org.gutkyu.dosboxj.util.*;

public final class DeviceNUL extends DOSDevice {
    public DeviceNUL() {
        setName(CStringPt.create("NUL"));
    }

    private final byte[] tmpRd = new byte[1];

    @Override
    public boolean read() {
        return this.read(tmpRd, 0, 1);
    }

    @Override
    public boolean read(byte[] buf, int offset, int size) {
        for (int i = 0; i < size; i++) {
            buf[i + offset] = 0;
        }
        rdSz = size;
        Log.logging(Log.LogTypes.IOCTL, Log.LogServerities.Normal, "%s:READ", getName().toString());
        return true;
    }

    @Override
    public byte getReadByte(){
        return tmpRd[0];
    }

    private int rdSz = 0;

    @Override
    public int readSize() {
        Log.logging(Log.LogTypes.IOCTL, Log.LogServerities.Normal, "%s:readSize",
                getName().toString());
        return rdSz;
    }

    @Override
    public boolean write(byte buf) {
        wrtSz = 1;
        Log.logging(Log.LogTypes.IOCTL, Log.LogServerities.Normal, "%s:WRITE",
                getName().toString());
        return true;
    }

    @Override
    public boolean write(byte value, int size) {
        wrtSz = size;
        Log.logging(Log.LogTypes.IOCTL, Log.LogServerities.Normal, "%s:WRITE",
                getName().toString());
        return true;
    }

    @Override
    public boolean write(byte[] buf, int offset, int size) {
        wrtSz = size;
        Log.logging(Log.LogTypes.IOCTL, Log.LogServerities.Normal, "%s:WRITE",
                getName().toString());
        return true;
    }

    private int wrtSz = 0;

    @Override
    public int writtenSize() {
        Log.logging(Log.LogTypes.IOCTL, Log.LogServerities.Normal, "%s:writtenSize",
                getName().toString());
        return wrtSz;
    }

    @Override
    public long seek(long pos, int type) {
        Log.logging(Log.LogTypes.IOCTL, Log.LogServerities.Normal, "%s:SEEK", getName().toString());
        return pos;
    }

    @Override
    public boolean close() {
        return true;
    }

    @Override
    public int getInformation() {
        return 0x8084;
    }

    // 실패하면 code -1 리턴
    @Override
    public int readFromControlChannel(int bufPtr, int size) {
        return -1;
    }

    // 실패하면 code -1 리턴
    @Override
    public int writeToControlChannel(int bufPtr, int size) {
        return -1;
    }
}
