package org.gutkyu.dosboxj.dos.system.file;



import java.util.Arrays;
import org.gutkyu.dosboxj.dos.*;
import org.gutkyu.dosboxj.util.*;
import org.gutkyu.dosboxj.dos.system.drive.*;

public final class FATFile extends DOSFile {

    public FATFile(String name, int startCluster, int fileLen, FATDrive useDrive) {
        long seekTo = 0;
        FirstCluster = startCluster;
        myDrive = useDrive;
        fileLength = fileLen;
        Open = true;
        LoadedSector = false;
        CurSectOff = 0;
        SeekPos = 0;
        Arrays.fill(SectorBuffer, 0, SectorBuffer.length, (byte) 0);

        if (fileLength > 0) {
            seekTo = seek(seekTo, DOSSystem.DOS_SEEK_SET);
            myDrive.LoadedDisk.readAbsoluteSector(CurrentSector, SectorBuffer, 0);
            LoadedSector = true;
        }
    }

    private final byte[] tmpRd = new byte[1];

    @Override
    public boolean read() {
        return this.read(tmpRd, 0, 1);
    }

    @Override
    public boolean read(byte[] buf, int offset, int size) {
        if ((this.Flags & 0xf) == DOSSystem.OPEN_WRITE) { // check if file opened in write-only mode
            DOSMain.setError(DOSMain.DOSERR_ACCESS_DENIED);
            return false;
        }
        int sizeDec, sizeCount;
        if (SeekPos >= fileLength) {
            rdSz = 0;
            return true;
        }

        if (!LoadedSector) {
            CurrentSector = myDrive.getAbsoluteSectFromBytePos(FirstCluster, SeekPos);
            if (CurrentSector == 0) {
                /* EOC reached before EOF */
                rdSz = 0;
                LoadedSector = false;
                return true;
            }
            CurSectOff = 0;
            myDrive.LoadedDisk.readAbsoluteSector(CurrentSector, SectorBuffer, 0);
            LoadedSector = true;
        }

        sizeDec = size;
        sizeCount = 0;
        while (sizeDec != 0) {
            if (SeekPos >= fileLength) {
                rdSz = sizeCount;
                return true;
            }
            buf[offset + sizeCount++] = SectorBuffer[CurSectOff++];
            SeekPos++;
            if (CurSectOff >= myDrive.getSectorSize()) {
                CurrentSector = myDrive.getAbsoluteSectFromBytePos(FirstCluster, SeekPos);
                if (CurrentSector == 0) {
                    /* EOC reached before EOF */
                    // Log.LOG_MSG("EOC reached before EOF, seekpos %d, filelen %d", seekpos,
                    // filelength);
                    rdSz = sizeCount;
                    LoadedSector = false;
                    return true;
                }
                CurSectOff = 0;
                myDrive.LoadedDisk.readAbsoluteSector(CurrentSector, SectorBuffer, 0);
                LoadedSector = true;
                // Log.LOG_MSG("Reading absolute sector at %d for seekpos %d", currentSector,
                // seekpos);
            }
            --sizeDec;
        }
        rdSz = sizeCount;
        return true;
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
        /* TODO: Check for read-only bit */

        if ((this.Flags & 0xf) == DOSSystem.OPEN_READ) { // check if file opened in read-only mode
            DOSMain.setError(DOSMain.DOSERR_ACCESS_DENIED);
            return false;
        }

        byte[] tmpEntry = new byte[FATDrive.SIZE_direntry_Total];
        int sizeCount = 0;
        int sizeDec = size;
        int startIndex = offset;

        while (sizeDec != 0) {
            /* Increase filesize if necessary */
            if (SeekPos >= fileLength) {
                if (fileLength == 0) {
                    FirstCluster = myDrive.getFirstFreeClust();
                    myDrive.allocateCluster(FirstCluster, 0);
                    CurrentSector = myDrive.getAbsoluteSectFromBytePos(FirstCluster, SeekPos);
                    myDrive.LoadedDisk.readAbsoluteSector(CurrentSector, SectorBuffer, 0);
                    LoadedSector = true;
                }
                fileLength = (int) SeekPos + 1;
                if (!LoadedSector) {
                    CurrentSector = myDrive.getAbsoluteSectFromBytePos(FirstCluster, SeekPos);
                    if (CurrentSector == 0) {
                        /* EOC reached before EOF - try to increase file allocation */
                        myDrive.appendCluster(FirstCluster);
                        /* Try getting sector again */
                        CurrentSector = myDrive.getAbsoluteSectFromBytePos(FirstCluster, SeekPos);
                        if (CurrentSector == 0) {
                            /* No can do. lets give up and go home. We must be out of room */
                            // goto finalizeWrite;
                            finalizeWrite(tmpEntry);
                            wrtSz = sizeCount;
                            return true;
                        }
                    }
                    CurSectOff = 0;
                    myDrive.LoadedDisk.readAbsoluteSector(CurrentSector, SectorBuffer, 0);

                    LoadedSector = true;
                }
            }
            SectorBuffer[CurSectOff++] = buf[startIndex + sizeCount++];
            SeekPos++;
            if (CurSectOff >= myDrive.getSectorSize()) {
                if (LoadedSector)
                    myDrive.LoadedDisk.writeAbsoluteSector(CurrentSector, SectorBuffer, 0);

                CurrentSector = myDrive.getAbsoluteSectFromBytePos(FirstCluster, SeekPos);
                if (CurrentSector == 0) {
                    /* EOC reached before EOF - try to increase file allocation */
                    myDrive.appendCluster(FirstCluster);
                    /* Try getting sector again */
                    CurrentSector = myDrive.getAbsoluteSectFromBytePos(FirstCluster, SeekPos);
                    if (CurrentSector == 0) {
                        /* No can do. lets give up and go home. We must be out of room */
                        LoadedSector = false;
                        // goto finalizeWrite;
                        finalizeWrite(tmpEntry);
                        wrtSz = sizeCount;
                        return true;
                    }
                }
                CurSectOff = 0;
                myDrive.LoadedDisk.readAbsoluteSector(CurrentSector, SectorBuffer, 0);

                LoadedSector = true;
            }
            --sizeDec;
        }
        if (CurSectOff > 0 && LoadedSector)
            myDrive.LoadedDisk.writeAbsoluteSector(CurrentSector, SectorBuffer, 0);

        // finalizeWrite:

        finalizeWrite(tmpEntry);
        wrtSz = sizeCount;
        return true;
    }

    private void finalizeWrite(byte[] entry) {
        myDrive.directoryBrowse(DirCluster, entry, DirIndex);

        ArrayHelper.copy(fileLength, entry, FATDrive.OFF_direntry_entrysize,
                FATDrive.SIZE_direntry_entrysize);
        ArrayHelper.copy(FirstCluster, entry, FATDrive.OFF_direntry_loFirstClust,
                FATDrive.SIZE_direntry_loFirstClust);
        myDrive.directoryChange(DirCluster, entry, DirIndex);

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

    @Override
    public long seek(long pos, int type) {
        int seekTo = 0;//Bit32s
        pos &= 0xFFFFFFFF;
        switch (type) {
            case DOSSystem.DOS_SEEK_SET:
                seekTo = (int)pos;//Bit32s
                break;
            case DOSSystem.DOS_SEEK_CUR:
                /* Is this relative seek signed? */
                seekTo = (int)pos + (int)SeekPos;//Bit32s + Bit32s
                break;
            case DOSSystem.DOS_SEEK_END:
                seekTo = (int)fileLength + (int)pos;//Bit32s + Bit32s
                break;
        }
        // Log.LOG_MSG("Seek to %d with type %d (absolute value %d)", *pos, type, seekto);

        if (seekTo > fileLength)
            seekTo = fileLength;
        if (seekTo < 0)
            seekTo = 0;
        SeekPos = 0xffffffffL & seekTo;
        CurrentSector = myDrive.getAbsoluteSectFromBytePos(FirstCluster,  SeekPos);
        if (CurrentSector == 0) {
            /* not within file size, thus no sector is available */
            LoadedSector = false;
        } else {
            CurSectOff = (int) (SeekPos % myDrive.getSectorSize());
            myDrive.LoadedDisk.readAbsoluteSector(CurrentSector, SectorBuffer, 0);
        }
        pos = SeekPos;
        return pos & 0xFFFFFFFFL;
    }

    @Override
    public boolean close() {
        /* Flush buffer */
        if (LoadedSector)
            myDrive.LoadedDisk.writeAbsoluteSector(CurrentSector, SectorBuffer, 0);

        return false;
    }

    @Override
    public int getInformation() {
        return 0;
    }

    @Override
    public boolean updateDateTimeFromHost() {
        return true;
    }


    public int FirstCluster;
    public long SeekPos; // max 4GB까지 가능
    public int fileLength;
    public int CurrentSector;
    public int CurSectOff;
    public byte[] SectorBuffer = new byte[512];
    /* Record of where in the directory structure this file is located */
    public int DirCluster;
    public int DirIndex;

    public boolean LoadedSector;
    public FATDrive myDrive;
    private static final int NONE = 0;
    private static final int READ = 1;
    private static final int WRITE = 2;

    private byte LastAction;
    private short Info;

}
