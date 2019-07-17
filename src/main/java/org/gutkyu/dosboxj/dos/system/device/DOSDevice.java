package org.gutkyu.dosboxj.dos.system.device;

import org.gutkyu.dosboxj.dos.DOSMain;
import org.gutkyu.dosboxj.dos.system.file.*;

public class DOSDevice extends DOSFile {
    public DOSDevice(DOSDevice orig) {
        super(orig);
        devnum = orig.devnum;
        currDev = DOSMain.Devices[devnum];
        Open = true;
    }

    // public DOS_Device & operator= (const DOS_Device & orig) {
    // DOS_File::operator=(orig);
    // devnum=orig.devnum;
    // open=true;
    // return *this;
    // }
    @Override
    protected void dispose(boolean disposing) {
        super.dispose(disposing);
    }

    public DOSDevice() {
        super();
        devnum = 0;
        currDev = DOSMain.Devices[devnum];
    }

    @Override
    public boolean read() {
        return currDev.read();
    }

    @Override
    public boolean read(byte[] buf, int offset, int size) {
        return currDev.read(buf, offset, size);
    }

    @Override
    public byte getReadByte() {
        return currDev.getReadByte();
    }

    @Override
    public int readSize() {
        return currDev.readSize();
    }

    @Override
    public boolean write(byte buf) {
        return currDev.write(buf);
    }

    @Override
    public boolean write(byte[] buf, int offset, int size) {
        return currDev.write(buf, offset, size);
    }

    @Override
    public boolean write(byte value, int size) {
        return currDev.write(value, size);
    }

    @Override
    public int writtenSize() {
        return currDev.writtenSize();
    }

    @Override
    public long seek(long pos, int type) {
        return currDev.seek(pos, type);
    }

    @Override
    public boolean close() {
        return currDev.close();
    }

    @Override
    public int getInformation() {
        return currDev.getInformation();
    }

    // 실패하면 return code -1
    public int readFromControlChannel(int bufPtr, int size) {
        return currDev.readFromControlChannel(bufPtr, size);
    }

    // 실패하면 code -1 리턴
    public int writeToControlChannel(int bufPtr, int size) {
        return currDev.writeToControlChannel(bufPtr, size);
    }

    public void setDeviceNumber(int num) {
        devnum = num;
        currDev = DOSMain.Devices[devnum];
    }

    private int devnum;
    private DOSDevice currDev = null;

}
