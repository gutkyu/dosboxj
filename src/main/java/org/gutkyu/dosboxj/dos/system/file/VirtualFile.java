package org.gutkyu.dosboxj.dos.system.file;


import org.gutkyu.dosboxj.dos.*;

public final class VirtualFile extends DOSFile {
    public VirtualFile(byte[] inData, int inSize) {
        fileSize = inSize;
        fileData = inData;
        filePos = 0;
        Date = DOSMain.packDate(2002, 10, 1);
        Time = DOSMain.packTime(12, 34, 56);
        Open = true;
    }

    @Override
    public boolean read(byte[] buf, int offset, int size) {
        int left = fileSize - filePos;
        if (left <= size) {
            for (int i = 0; i < left; i++) {
                buf[i + offset] = fileData[i + filePos];
            }
            rdSz = left;
        } else {
            for (int i = 0; i < size; i++) {
                buf[i + offset] = fileData[i + filePos];
            }
            rdSz = size;
        }
        filePos += rdSz;
        return true;
    }

    private final byte[] tmpRd = new byte[1];

    @Override
    public boolean read() {
        return this.read(tmpRd, 0, 1);
    }

    @Override
    public byte getReadByte() {
        return tmpRd[0];
    }

    private int rdSz = 0;

    @Override
    public int readSize() {
        return rdSz;
    }

    @Override
    public boolean write(byte[] buf, int offset, int size) {
        /* Not really writeable */
        return false;
    }

    @Override
    public boolean write(byte value, int size) {
        /* Not really writeable */
        return false;
    }

    @Override
    public boolean write(byte value) {
        /* Not really writeable */
        return false;
    }

    @Override
    public int writtenSize() {
        return 0;
    }

    @Override
    public long seek(long pos, int type) {
        pos &= 0xFFFFFFFF;
        switch (type) {
            case DOSSystem.DOS_SEEK_SET:
                if (pos <= fileSize)
                    filePos = (int) pos;
                else
                    return -1;
                break;
            case DOSSystem.DOS_SEEK_CUR:
                if ((pos + filePos) <= fileSize)
                    filePos = (int) (pos + filePos);
                else
                    return -1;
                break;
            case DOSSystem.DOS_SEEK_END:
                if (pos <= fileSize)
                    filePos = (int) (fileSize - pos);
                else
                    return -1;
                break;
        }
        pos = filePos & 0xFFFFFFFF;
        return pos;
    }

    @Override
    public boolean close() {
        return true;
    }

    @Override
    public int getInformation() {
        return 0x40; // read-only drive
    }

    private int fileSize;
    private int filePos;
    private byte[] fileData;
}
