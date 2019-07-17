package org.gutkyu.dosboxj.dos.system.file;


import org.gutkyu.dosboxj.dos.*;

public final class VirtualFile extends DOSFile {
    public VirtualFile(byte[] inData, int inSize) {
        _fileSize = inSize;
        _fileData = inData;
        _filePos = 0;
        Date = DOSMain.packDate((short) 2002, (short) 10, (short) 1);
        Time = DOSMain.packTime((short) 12, (short) 34, (short) 56);
        Open = true;
    }

    @Override
    public boolean read(byte[] buf, int offset, int size) {
        int left = _fileSize - _filePos;
        if (left <= size) {
            for (int i = 0; i < left; i++) {
                buf[i + offset] = _fileData[i + _filePos];
            }
            rdSz = left;
        } else {
            for (int i = 0; i < size; i++) {
                buf[i + offset] = _fileData[i + _filePos];
            }
            rdSz = size;
        }
        _filePos += rdSz;
        return true;
    }

    private final byte[] tmpRd = new byte[1];

    @Override
    public boolean read() {
        return this.read(tmpRd, 0, 1);
    }

    @Override
    public byte getReadByte(){
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
                if (pos <= _fileSize)
                    _filePos = (int) pos;
                else
                    return -1;
                break;
            case DOSSystem.DOS_SEEK_CUR:
                if ((pos + _filePos) <= _fileSize)
                    _filePos = (int) (pos + _filePos);
                else
                    return -1;
                break;
            case DOSSystem.DOS_SEEK_END:
                if (pos <= _fileSize)
                    _filePos = (int) (_fileSize - pos);
                else
                    return -1;
                break;
        }
        pos = _filePos & 0xFFFFFFFF;
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

    private int _fileSize;
    private int _filePos;
    private byte[] _fileData;
}
