package org.gutkyu.dosboxj.dos.system.file;

import java.nio.*;
import java.nio.channels.*;
import java.nio.file.*;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.gutkyu.dosboxj.dos.*;
import org.gutkyu.dosboxj.hardware.io.*;

public final class LocalFile extends DOSFile {
    public LocalFile(String name, SeekableByteChannel chann) {
        fChann = chann;
        Open = true;
        updateDateTimeFromHost();

        Attr = DOSSystem.DOS_ATTR_ARCHIVE;
        _lastAction = LastActionType.NONE;
        _readOnlyMedium = false;

        Name = "";
        setName(name);
    }

    // TODO Maybe use flush, but that seemed to fuck up in visual c
    @Override
    public boolean read(byte[] buf, int offset, int size) {
        if ((this.Flags & 0xf) == DOSSystem.OPEN_WRITE) { // check if file opened in write-only mode
            DOSMain.setError(DOSMain.DOSERR_ACCESS_DENIED);
            return false;
        }
        // TODO 다음 라인은 별 의미가 없는데 왜 구현했을까?
        // if (_lastAction == LastActionType.WRITE)
        // fChann.Seek(fChann.Position, SeekOrigin.Begin);
        _lastAction = LastActionType.READ;
        ByteBuffer rBuf = ByteBuffer.wrap(buf, offset, size);
        try {
            rdSz = size = fChann.read(rBuf);
        } catch (Exception e) {
            return false;
        }
        /* Fake harddrive motion. Inspector Gadget with soundblaster compatible */
        /* Same for Igor */
        /*
         * hardrive motion => unmask irq 2. Only do it when it's masked as unmasking is realitively
         * heavy to emulate
         */
        int mask = IO.read(0x21);
        if ((mask & 0x4) != 0)
            IO.write(0x21, mask & 0xfb);
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
        if ((this.Flags & 0xf) == DOSSystem.OPEN_READ) { // check if file opened in read-only mode
            DOSMain.setError(DOSMain.DOSERR_ACCESS_DENIED);
            return false;
        }
        // 다음 라인은 별 의미가 없는데 왜 구현했을까?
        // if (_lastAction == LastActionType.READ)
        // fChann.Seek(fChann.Position, SeekOrigin.Begin);
        _lastAction = LastActionType.WRITE;
        if (size == 0) {
            try {
                fChann.truncate(0);
                return true;
            } catch (Exception e) {
                return false;
            }
        }
        // 일부만 파일에 기록할 상황은 없다. 성공하거나 실패.
        try {
            ByteBuffer wrap = ByteBuffer.wrap(buf, offset, size);
            fChann.write(wrap);
        } catch (Exception e) {
            return false;
        }
        wrtSz = size;
        return true;
    }

    private final byte[] tmpWrt = new byte[1];

    @Override
    public boolean write(byte value, int size) {
        tmpWrt[0] = value;
        return write(tmpWrt, 0, size);
    }

    @Override
    public boolean write(byte value) {
        tmpWrt[0] = value;
        return write(tmpWrt, 0, 1);
    }

    private int wrtSz = 0;

    @Override
    public int writtenSize() {
        return wrtSz;
    }

    // 오류 발생하면 -1반환
    // 성공하면 절대 위치
    @Override
    public long seek(long pos, int type) {
        try {
            switch (type) {
                case DOSSystem.DOS_SEEK_SET:
                    fChann.position(pos);

                    break;
                case DOSSystem.DOS_SEEK_CUR:
                    fChann.position(fChann.position() + pos);

                    break;
                case DOSSystem.DOS_SEEK_END:
                    fChann.position(fChann.size() - 1 + fChann.position() + pos);

                    break;
                default:
                    // TODO Give some doserrorcode;
                    return -1;// ERROR
            }
            pos = fChann.position();
        } catch (Exception e) {// Out of file range, pretend everythings ok
            // and move file pointer top end of file... ?! (Black Thorne)
            // fChann.Seek(0, SeekOrigin.End);
            return -1;
        }

        _lastAction = LastActionType.NONE;
        return pos;
    }

    @Override
    public boolean close() {
        // only close if one reference left
        if (RefCtr == 1) {
            try {
                if (fChann != null) {
                    fChann.close();
                }
            } catch (Exception e) {
            }
            fChann = null;
            Open = false;
        }
        return true;
    }

    @Override
    public int getInformation() {
        return _readOnlyMedium ? 0x40 : 0;
    }

    @Override
    public boolean updateDateTimeFromHost() {
        if (!Open)
            return false;


        try {
            FileTime mTime = Files.getLastModifiedTime(Paths.get(getName()));
            LocalDateTime dt = LocalDateTime.ofInstant(mTime.toInstant(), ZoneOffset.UTC);

            Date = DOSMain.packDate(dt.getYear(), dt.getMonthValue(), dt.getDayOfMonth());
            Time = DOSMain.packTime(dt.getHour(), dt.getMinute(), dt.getSecond());

        } catch (Exception e) {
            Time = 1;
            Date = 1;
        }
        return true;


    }

    public void flagReadOnlyMedium() {
        _readOnlyMedium = true;
    }

    private SeekableByteChannel fChann;
    private boolean _readOnlyMedium;

    private enum LastActionType {
        NONE, READ, WRITE
    }

    private LastActionType _lastAction;
}
