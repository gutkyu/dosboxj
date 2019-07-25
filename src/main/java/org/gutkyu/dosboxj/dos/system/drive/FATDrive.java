package org.gutkyu.dosboxj.dos.system.drive;

import java.nio.channels.*;
import java.nio.file.*;
import java.util.Arrays;
import org.gutkyu.dosboxj.dos.*;
import org.gutkyu.dosboxj.dos.mem_block.*;
import org.gutkyu.dosboxj.hardware.memory.*;
import org.gutkyu.dosboxj.misc.*;
import org.gutkyu.dosboxj.util.*;
import org.gutkyu.dosboxj.interrupt.*;
import org.gutkyu.dosboxj.interrupt.bios.*;
import org.gutkyu.dosboxj.dos.system.file.*;


public final class FATDrive extends DOSDrive implements Disposable {

    private static final int FAT12 = 0;
    private static final int FAT16 = 1;
    private static final int FAT32 = 2;

    private static byte[] _fatSectBuffer = new byte[1024];
    private static int _curFatSect;

    // ------------------------------------ struct sPSP start ----------------------------------//

    private static final int SIZE_UINT = 4;
    private static final int SIZE_USHORT = 2;
    private static final int SIZE_UBYTE = 1;

    private static final int OFF_direntry_entryname = 0;
    private static final int OFF_direntry_attrib = 11;
    private static final int OFF_direntry_NTRes = 12;
    private static final int OFF_direntry_milliSecondStamp = 13;
    private static final int OFF_direntry_crtTime = 14;
    private static final int OFF_direntry_crtDate = 16;
    private static final int OFF_direntry_accessDate = 18;
    private static final int OFF_direntry_hiFirstClust = 20;
    private static final int OFF_direntry_modTime = 22;
    private static final int OFF_direntry_modDate = 24;
    public static final byte OFF_direntry_loFirstClust = 26;
    public static final byte OFF_direntry_entrysize = 28;

    private static final int SIZE_direntry_entryname = 11;
    private static final int SIZE_direntry_attrib = 1;
    private static final int SIZE_direntry_NTRes = 1;
    private static final int SIZE_direntry_milliSecondStamp = 1;
    private static final int SIZE_direntry_crtTime = 2;
    private static final int SIZE_direntry_crtDate = 2;
    private static final int SIZE_direntry_accessDate = 2;
    private static final int SIZE_direntry_hiFirstClust = 2;
    private static final int SIZE_direntry_modTime = 2;
    private static final int SIZE_direntry_modDate = 2;
    public static final byte SIZE_direntry_loFirstClust = 2;
    public static final byte SIZE_direntry_entrysize = 4;

    public static final byte SIZE_direntry_Total = 32;

    // ------------------------------------ struct sPSP end ----------------------------------//


    // ---------------------------------- struct partTable start --------------------------------//

    private static final int OFF_partTable_booter = 0;
    private static final int OFF_partTable_bootflag = 0;
    private static final int OFF_partTable_beginchs = 1;
    private static final int OFF_partTable_parttype = 4;
    private static final int OFF_partTable_endchs = 5;
    private static final int OFF_partTable_absSectStart = 8;
    private static final int OFF_partTable_partSize = 12;
    private static final int OFF_partTable_pentry = 446;
    private static final int OFF_partTable_magic1 = 510; /* 0x55 */
    private static final int OFF_partTable_magic2 = 511; /* 0xaa */
    private static final int OFF_partTable_Total = 512;

    private static final int SIZE_partTable_booter = 446;
    private static final int SIZE_partTable_bootflag = 1;
    private static final int SIZE_partTable_beginchs = 3;
    private static final int SIZE_partTable_parttype = 1;
    private static final int SIZE_partTable_endchs = 3;
    private static final int SIZE_partTable_absSectStart = 4;
    private static final int SIZE_partTable_partSize = 4;
    private static final int SIZE_partTable_pentry = 4;
    private static final int SIZE_partTable_magic1 = 1; /* 0x55 */
    private static final int SIZE_partTable_magic2 = 1; /* 0xaa */

    private static final int SIZE_partTable_Total = 512;

    // ---------------------------------- struct partTable end --------------------------------//


    // ---------------------------------- struct bootstrap start --------------------------------//

    private static final int OFF_bootstrap_nearjmp = 0;
    private static final int OFF_bootstrap_oemname = 3;
    private static final int OFF_bootstrap_bytespersector = 11;
    private static final int OFF_bootstrap_sectorspercluster = 13;
    private static final int OFF_bootstrap_reservedsectors = 14;
    private static final int OFF_bootstrap_fatcopies = 16;
    private static final int OFF_bootstrap_rootdirentries = 17;
    private static final int OFF_bootstrap_totalsectorcount = 19;
    private static final int OFF_bootstrap_mediadescriptor = 21;
    private static final int OFF_bootstrap_sectorsperfat = 22;
    private static final int OFF_bootstrap_sectorspertrack = 24;
    private static final int OFF_bootstrap_headcount = 26;
    // /* 32-bit FAT extensions */
    private static final int OFF_bootstrap_hiddensectorcount = 28;
    private static final int OFF_bootstrap_totalsecdword = 32;
    private static final int OFF_bootstrap_bootcode = 36;
    private static final int OFF_bootstrap_magic1 = 510; /* 0x55 */
    private static final int OFF_bootstrap_magic2 = 511; /* 0xaa */

    private static final int SIZE_bootstrap_nearjmp = 3;
    private static final int SIZE_bootstrap_oemname = 8;
    private static final int SIZE_bootstrap_bytespersector = 2;
    private static final int SIZE_bootstrap_sectorspercluster = 1;
    private static final int SIZE_bootstrap_reservedsectors = 2;
    private static final int SIZE_bootstrap_fatcopies = 1;
    private static final int SIZE_bootstrap_rootdirentries = 2;
    private static final int SIZE_bootstrap_totalsectorcount = 2;
    private static final int SIZE_bootstrap_mediadescriptor = 1;
    private static final int SIZE_bootstrap_sectorsperfat = 2;
    private static final int SIZE_bootstrap_sectorspertrack = 2;
    private static final int SIZE_bootstrap_headcount = 2;
    // /* 32-bit FAT extensions */
    private static final int SIZE_bootstrap_hiddensectorcount = 4;
    private static final int SIZE_bootstrap_totalsecdword = 4;
    private static final int SIZE_bootstrap_bootcode = 474;
    private static final int SIZE_bootstrap_magic1 = 1; /* 0x55 */
    private static final int SIZE_bootstrap_magic2 = 1; /* 0xaa */

    private static final int SIZE_bootstrap_Total = 512;

    // ---------------------------------- struct bootstrap end --------------------------------//


    public FATDrive(String sysFilename, int byteSector, int cylSector, int headsCyl, int cylinders,
            int startSector) {
        for (int i = 0; i < _srchInfo.length; i++) {
            _srchInfo[i] = new SrchInfo();
        }

        createdSuccessfully = true;
        SeekableByteChannel diskfile = null;
        int filesize;
        byte[] mbrData = new byte[SIZE_partTable_Total];// partTable

        if (BIOSDisk.ImgDTASeg == 0) {
            BIOSDisk.ImgDTASeg = DOSMain.getMemory(2);
            BIOSDisk.ImgDTAPtr = Memory.realMake(BIOSDisk.ImgDTASeg, 0);
            BIOSDisk.ImgDTA = new DOSDTA(BIOSDisk.ImgDTAPtr);
        }
        try {
            // diskfile = fopen(sysFilename, "rb+");
            diskfile = Files.newByteChannel(Paths.get(sysFilename), StandardOpenOption.READ,
                    StandardOpenOption.WRITE);
            filesize = (int) (diskfile.size() / 1024);
        } catch (Exception e) {
            createdSuccessfully = false;
            return;
        }
        // fseek(diskfile, 0L, SEEK_END);
        // filesize = (int)ftell(diskfile) / 1024L;

        /* Load disk image */
        LoadedDisk = new ImageDisk(diskfile, sysFilename, filesize, (filesize > 2880));
        if (LoadedDisk == null) {
            createdSuccessfully = false;
            return;
        }

        if (filesize > 2880) {
            /* Set user specified harddrive parameters */
            LoadedDisk.setGeometry(headsCyl, cylinders, cylSector, byteSector);

            LoadedDisk.readSector(0, 0, 1, mbrData);

            if (mbrData[OFF_partTable_magic1] != 0x55 || mbrData[OFF_partTable_magic2] != 0xaa)
                Log.logMsg("Possibly invalid partition table in disk image.");

            startSector = 63;
            int m;
            for (m = 0; m < 4; m++) {
                /* Pick the first available partition */
                int partSize = ByteConv.getInt(mbrData,
                        OFF_partTable_pentry + m * SIZE_partTable_pentry + OFF_partTable_partSize);
                if (partSize != 0x00) {
                    startSector = ByteConv.getInt(mbrData, OFF_partTable_pentry
                            + m * SIZE_partTable_pentry + OFF_partTable_absSectStart);
                    Log.logMsg("Using partition %d on drive; skipping %d sectors", m, startSector);
                    break;
                }
            }

            if (m == 4)
                Log.logMsg("No good partiton found in image.");

            _partSectOff = startSector;
        } else {
            /* Floppy disks don't have partitions */
            _partSectOff = 0;
        }

        LoadedDisk.readAbsoluteSector(0 + _partSectOff, _bootbuffer, 0);
        if ((_bootbuffer[OFF_bootstrap_magic1] != 0x55)
                || (_bootbuffer[OFF_bootstrap_magic2] != 0xaa)) {
            /* Not a FAT filesystem */
            Log.logMsg("Loaded image has no valid magicnumbers at the end!");
        }
        int sectorsperfat = ByteConv.getShort(_bootbuffer, OFF_bootstrap_sectorsperfat);
        if (sectorsperfat == 0) {
            /* FAT32 not implemented yet */
            createdSuccessfully = false;
            return;
        }


        /* Determine FAT format, 12, 16 or 32 */

        /* Get size of root dir in sectors */
        /* TODO: Get 32-bit total sector count if needed */
        int rootdirentries = ByteConv.getShort(_bootbuffer, OFF_bootstrap_rootdirentries);
        int bytespersector = ByteConv.getShort(_bootbuffer, OFF_bootstrap_bytespersector);

        int RootDirSectors = ((rootdirentries * 32) + (bytespersector - 1)) / bytespersector;
        int DataSectors;

        int totalsectorcount = ByteConv.getShort(_bootbuffer, OFF_bootstrap_totalsectorcount);
        int reservedsectors = ByteConv.getShort(_bootbuffer, OFF_bootstrap_reservedsectors);
        int fatcopies = 0xff & _bootbuffer[OFF_bootstrap_fatcopies];
        int totalsecdword = ByteConv.getShort(_bootbuffer, OFF_bootstrap_totalsecdword);

        if (totalsectorcount != 0) {
            DataSectors = totalsectorcount
                    - (reservedsectors + (fatcopies * sectorsperfat) + RootDirSectors);
        } else {
            DataSectors = totalsecdword
                    - (reservedsectors + (fatcopies * sectorsperfat) + RootDirSectors);

        }

        int sectorspercluster = 0xff & _bootbuffer[OFF_bootstrap_sectorspercluster];

        _countOfClusters = DataSectors / sectorspercluster;

        _firstDataSector =
                (reservedsectors + (fatcopies * sectorsperfat) + RootDirSectors) + _partSectOff;
        _firstRootDirSect = (reservedsectors + (fatcopies * sectorsperfat) + _partSectOff);

        if (_countOfClusters < 4085) {
            /* Volume is FAT12 */
            Log.logMsg("Mounted FAT volume is FAT12 with %d clusters", _countOfClusters);
            _fatType = FAT12;
        } else if (_countOfClusters < 65525) {
            Log.logMsg("Mounted FAT volume is FAT16 with %d clusters", _countOfClusters);
            _fatType = FAT16;
        } else {
            Log.logMsg("Mounted FAT volume is FAT32 with %d clusters", _countOfClusters);
            _fatType = FAT32;
        }

        /* There is no cluster 0, this means we are in the root directory */
        _cwdDirCluster = 0;

        Arrays.fill(_fatSectBuffer, 0, 1024, (byte) 0);
        _curFatSect = 0xffffffff;
    }

    public void dispose() {
        dispose(true);
    }

    private void dispose(boolean disposing) {

    }

    @Override
    public DOSFile fileOpen(String name, int flags) {
        DOSFile file = null;
        byte[] fileEntry = new byte[SIZE_direntry_Total];
        int dirClust = 0, subEntry = 0;
        RefU32Ret refDirClust = new RefU32Ret(dirClust);
        RefU32Ret refSubEntry = new RefU32Ret(subEntry);
        if (!getFileDirEntry(name, fileEntry, refDirClust, refSubEntry))
            return null;
        dirClust = refDirClust.U32;
        subEntry = refSubEntry.U32;
        /* TODO: check for read-only flag and requested write access */
        file = new FATFile(name, ByteConv.getInt(fileEntry, OFF_direntry_loFirstClust),
                ByteConv.getInt(fileEntry, OFF_direntry_entrysize), this);
        file.Flags = flags;
        ((FATFile) file).DirCluster = dirClust;
        ((FATFile) file).DirIndex = subEntry;
        /* Maybe modTime and date should be used ? (crt matches findnext) */
        ((FATFile) file).Time = ByteConv.getShort(fileEntry, OFF_direntry_crtTime);
        ((FATFile) file).Date = ByteConv.getShort(fileEntry, OFF_direntry_crtDate);
        return file;
    }

    @Override
    public DOSFile fileCreate(String name, int attributes) {
        DOSFile file = null;
        byte[] fileEntry = new byte[SIZE_direntry_Total];
        int dirClust = 0, subEntry = 0;
        CStringPt dirName = CStringPt.create(DOSSystem.DOS_NAMELENGTH_ASCII);
        CStringPt pathName = CStringPt.create(11);

        int save_errorcode = DOSMain.DOS.ErrorCode;

        /* Check if file already exists */
        RefU32Ret refDirClust = new RefU32Ret(dirClust);
        RefU32Ret refSubEntry = new RefU32Ret(subEntry);
        if (getFileDirEntry(name, fileEntry, refDirClust, refSubEntry)) {
            dirClust = refDirClust.U32;
            subEntry = refSubEntry.U32;
            /* Truncate file */
            // fileEntry.entrysize=0;
            fileEntry[OFF_direntry_entrysize] = 0;
            fileEntry[OFF_direntry_entrysize + 1] = 0;
            fileEntry[OFF_direntry_entrysize + 2] = 0;
            fileEntry[OFF_direntry_entrysize + 3] = 0;

            directoryChange(dirClust, fileEntry, subEntry);
        } else {
            /* Can we even get the name of the file itself? */
            if (!getEntryName(name, dirName))
                return file;
            convToDirFile(dirName, pathName);

            /* Can we find the base directory? */
            refDirClust.set(dirClust);
            if (!getDirClustNum(name, refDirClust, true))
                return file;
            dirClust = refDirClust.U32;
            Arrays.fill(fileEntry, 0, fileEntry.length, (byte) 0);
            for (int i = 0; i < 11; i++) {
                fileEntry[OFF_direntry_entryname + i] = (byte) pathName.get(i);
            }

            // fileEntry.attrib = (byte)(attributes & 0xff);
            fileEntry[OFF_direntry_attrib] = (byte) (attributes & 0xff);
            addDirectoryEntry(dirClust, fileEntry);

            /* Check if file exists now */
            refDirClust.set(dirClust);
            refSubEntry.set(subEntry);
            if (!getFileDirEntry(name, fileEntry, refDirClust, refSubEntry))
                return file;
        }

        /* Empty file created, now lets open it */
        /* TODO: check for read-only flag and requested write access */
        file = new FATFile(name, ByteConv.getShort(fileEntry, OFF_direntry_loFirstClust),
                ByteConv.getInt(fileEntry, OFF_direntry_entrysize), this);
        file.Flags = DOSSystem.OPEN_READWRITE;
        ((FATFile) file).DirCluster = dirClust;
        ((FATFile) file).DirIndex = subEntry;
        /* Maybe modTime and date should be used ? (crt matches findnext) */
        ((FATFile) file).Time = ByteConv.getShort(fileEntry, OFF_direntry_crtTime);
        ((FATFile) file).Date = ByteConv.getShort(fileEntry, OFF_direntry_crtDate);

        DOSMain.DOS.ErrorCode = save_errorcode;
        return file;
    }

    @Override
    public boolean fileUnlink(String name) {
        byte[] fileEntry = new byte[SIZE_direntry_Total];
        int dirClust = 0, subEntry = 0;
        RefU32Ret refDirClust = new RefU32Ret(dirClust);
        RefU32Ret refSubEntry = new RefU32Ret(subEntry);
        if (!getFileDirEntry(name.toString(), fileEntry, refDirClust, refSubEntry))
            return false;
        dirClust = refDirClust.U32;
        subEntry = refSubEntry.U32;
        // fileEntry.entryname[0] = 0xe5;
        fileEntry[OFF_direntry_entryname] = (byte) 0xe5;

        directoryChange(dirClust, fileEntry, subEntry);
        int loFirstClust = ByteConv.getShort(fileEntry, OFF_direntry_loFirstClust);
        if (loFirstClust != 0)
            deleteClustChain(loFirstClust);

        return true;
    }

    @Override
    public boolean removeDir(String dir) {
        int dummyClust = 0, dirClust = 0;
        byte[] tmpentry = new byte[SIZE_direntry_Total];
        CStringPt dirName = CStringPt.create(DOSSystem.DOS_NAMELENGTH_ASCII);
        CStringPt pathName = CStringPt.create(11);

        /* Can we even get the name of the directory itself? */
        if (!getEntryName(dir.toString(), dirName))
            return false;
        convToDirFile(dirName, pathName);

        /* Get directory starting cluster */
        RefU32Ret refDummyClust = new RefU32Ret(dummyClust);
        if (!getDirClustNum(dir.toString(), refDummyClust, false))
            return false;
        dummyClust = refDummyClust.U32;
        /* Can't remove root directory */
        if (dummyClust == 0)
            return false;

        /* Get parent directory starting cluster */
        RefU32Ret refDirClust = new RefU32Ret(dirClust);
        if (!getDirClustNum(dir.toString(), refDirClust, true))
            return false;
        dirClust = refDirClust.U32;

        /* Check to make sure directory is empty */
        int filecount = 0;
        /* Set to 2 to skip first 2 entries, [.] and [..] */
        int fileidx = 2;
        while (directoryBrowse(dummyClust, tmpentry, fileidx)) {
            /* Check for non-deleted files */
            if (tmpentry[OFF_direntry_entryname] != 0xe5)
                filecount++;
            fileidx++;
        }

        /* Return if directory is not empty */
        if (filecount > 0)
            return false;

        /* Find directory entry in parent directory */
        if (dirClust == 0)
            fileidx = 0; // root directory
        else
            fileidx = 2;
        boolean found = false;
        while (directoryBrowse(dirClust, tmpentry, fileidx)) {
            boolean cmp = true;
            for (int i = 0; i < 11; i++) {
                cmp = tmpentry[OFF_direntry_entryname + i] == (byte) pathName.get(i);
                if (!cmp)
                    break;
            }
            if (cmp) {
                found = true;
                tmpentry[OFF_direntry_entryname] = (byte) 0xe5;
                directoryChange(dirClust, tmpentry, fileidx);
                deleteClustChain(dummyClust);

                break;
            }
            fileidx++;
        }

        if (!found)
            return false;

        return true;
    }

    @Override
    public boolean makeDir(String dir) {
        int dummyClust = 0, dirClust = 0;
        byte[] tmpentry = new byte[SIZE_direntry_Total];
        CStringPt dirName = CStringPt.create(DOSSystem.DOS_NAMELENGTH_ASCII);
        CStringPt pathName = CStringPt.create(11);


        /* Can we even get the name of the directory itself? */
        if (!getEntryName(dir.toString(), dirName))
            return false;
        convToDirFile(dirName, pathName);

        /* Fail to make directory if already exists */
        RefU32Ret refDummyClust = new RefU32Ret(dummyClust);
        if (getDirClustNum(dir.toString(), refDummyClust, false))
            return false;

        dummyClust = getFirstFreeClust();
        /* No more space */
        if (dummyClust == 0)
            return false;

        if (!allocateCluster(dummyClust, 0))
            return false;

        zeroOutCluster(dummyClust);

        /* Can we find the base directory? */
        RefU32Ret refDirClust = new RefU32Ret(dirClust);
        if (!getDirClustNum(dir.toString(), refDirClust, true))
            return false;
        dirClust = refDirClust.U32;

        /* Add the new directory to the base directory */
        Arrays.fill(tmpentry, 0, SIZE_direntry_Total, (byte) 0);
        for (int i = 0; i < 11; i++)
            tmpentry[OFF_direntry_entryname + i] = (byte) pathName.get(i);
        ArrayHelper.copy(dummyClust, tmpentry, OFF_direntry_loFirstClust,
                SIZE_direntry_loFirstClust);
        ArrayHelper.copy(dummyClust >>> 16, tmpentry, OFF_direntry_hiFirstClust,
                SIZE_direntry_hiFirstClust);
        tmpentry[OFF_direntry_attrib] = DOSSystem.DOS_ATTR_DIRECTORY;
        addDirectoryEntry(dirClust, tmpentry);

        /* Add the [.] and [..] entries to our new directory */
        /* [.] entry */
        Arrays.fill(tmpentry, 0, SIZE_direntry_Total, (byte) 0);
        tmpentry[OFF_direntry_entryname] = (byte) '.';
        for (int i = 1; i < 11; i++)
            tmpentry[OFF_direntry_entryname + i] = (byte) ' ';
        ArrayHelper.copy(dummyClust, tmpentry, OFF_direntry_loFirstClust,
                SIZE_direntry_loFirstClust);
        ArrayHelper.copy(dummyClust >>> 16, tmpentry, OFF_direntry_hiFirstClust,
                SIZE_direntry_hiFirstClust);
        tmpentry[OFF_direntry_attrib] = DOSSystem.DOS_ATTR_DIRECTORY;
        addDirectoryEntry(dummyClust, tmpentry);

        /* [..] entry */
        Arrays.fill(tmpentry, 0, SIZE_direntry_Total, (byte) 0);
        tmpentry[OFF_direntry_entryname] = (byte) '.';
        tmpentry[OFF_direntry_entryname + 1] = (byte) '.';
        for (int i = 2; i < 11; i++)
            tmpentry[OFF_direntry_entryname + i] = (byte) ' ';
        ArrayHelper.copy(dummyClust, tmpentry, OFF_direntry_loFirstClust,
                SIZE_direntry_loFirstClust);
        ArrayHelper.copy(dummyClust >>> 16, tmpentry, OFF_direntry_hiFirstClust,
                SIZE_direntry_hiFirstClust);
        tmpentry[OFF_direntry_attrib] = DOSSystem.DOS_ATTR_DIRECTORY;
        addDirectoryEntry(dummyClust, tmpentry);

        return true;
    }

    @Override
    public boolean testDir(CStringPt dir) {
        int dummyClust = 0;
        RefU32Ret refDummyClust = new RefU32Ret(dummyClust);
        return getDirClustNum(dir.toString(), refDummyClust, false);
    }


    @Override
    public boolean testDir(String dir) {
        int dummyClust = 0;
        RefU32Ret refDummyClust = new RefU32Ret(dummyClust);
        return getDirClustNum(dir, refDummyClust, false);
    }

    @Override
    public boolean findFirst(CStringPt dir, DOSDTA dta, boolean fcbFindFirst) {
        byte[] dummyClust = new byte[SIZE_direntry_Total];
        int attr = 0;
        CStringPt pattern = CStringPt.create(DOSSystem.DOS_NAMELENGTH_ASCII);
        attr = dta.getSearchParams(pattern);
        if (attr == DOSSystem.DOS_ATTR_VOLUME) {
            if (getLabel().toString() == "") {
                DOSMain.setError(DOSMain.DOSERR_NO_MORE_FILES);
                return false;
            }
            dta.setResult(getLabel(), 0, 0, 0, DOSSystem.DOS_ATTR_VOLUME);
            return true;
        }
        if ((attr & DOSSystem.DOS_ATTR_VOLUME) != 0) // check for root dir or fcb_findfirst
            Log.logging(Log.LogTypes.DOSMISC, Log.LogServerities.Warn,
                    "findfirst for volumelabel used on fatDrive. Unhandled!!!!!");
        RefU32Ret refDirClust = new RefU32Ret(_cwdDirCluster);
        if (!getDirClustNum(dir.toString(), refDirClust, false)) {
            DOSMain.setError(DOSMain.DOSERR_PATH_NOT_FOUND);
            return false;
        }
        _cwdDirCluster = refDirClust.U32;
        dta.setDirID(0);
        dta.setDirIDCluster(_cwdDirCluster & 0xffff);
        return findNextInternal(_cwdDirCluster, dta, dummyClust);
    }

    @Override
    public boolean findFirst(CStringPt dir, DOSDTA dta) {
        return findFirst(dir, dta, false);
    }

    @Override
    public boolean findNext(DOSDTA dta) {
        byte[] dummyClust = new byte[SIZE_direntry_Total];

        return findNextInternal(dta.getDirIDCluster(), dta, dummyClust);
    }

    public int returnedFileAttr = 0;

    @Override
    public boolean tryFileAttr(String name) {
        byte[] fileEntry = new byte[SIZE_direntry_Total];
        int dirClust = 0, subEntry = 0;
        RefU32Ret refDirClust = new RefU32Ret(dirClust);
        RefU32Ret refSubEntry = new RefU32Ret(subEntry);
        if (!getFileDirEntry(name, fileEntry, refDirClust, refSubEntry)) {
            CStringPt dirName = CStringPt.create(DOSSystem.DOS_NAMELENGTH_ASCII);
            CStringPt pathName = CStringPt.create(11);

            /* Can we even get the name of the directory itself? */
            if (!getEntryName(name, dirName))
                return false;
            convToDirFile(dirName, pathName);

            /* Get parent directory starting cluster */
            if (!getDirClustNum(name, refDirClust, true))
                return false;

            /* Find directory entry in parent directory */
            int fileidx = 2;
            if (dirClust == 0)
                fileidx = 0; // root directory
            while (directoryBrowse(dirClust, fileEntry, fileidx)) {
                boolean cmp = true;
                for (int i = 0; i < 11; i++) {
                    cmp = fileEntry[OFF_direntry_entryname + i] == (byte) pathName.get(i);
                    if (!cmp)
                        break;
                }
                if (cmp) {
                    returnedFileAttr = 0xff & fileEntry[OFF_direntry_attrib];
                    return true;
                }
                fileidx++;
            }
            return false;
        } else
            returnedFileAttr = 0xff & fileEntry[OFF_direntry_attrib];
        return true;
    }

    @Override
    public int returnFileAttr() {
        return returnedFileAttr;
    }

    @Override
    public boolean rename(String oldName, String newName) {
        byte[] fileEntry1 = new byte[SIZE_direntry_Total];
        int dirClust1 = 0, subEntry1 = 0;
        RefU32Ret refDirClust1 = new RefU32Ret(dirClust1);
        RefU32Ret refSubEntry1 = new RefU32Ret(subEntry1);
        if (!getFileDirEntry(oldName, fileEntry1, refDirClust1, refSubEntry1))
            return false;
        dirClust1 = refDirClust1.U32;
        subEntry1 = refSubEntry1.U32;
        /* File to be renamed really exists */

        byte[] fileEntry2 = new byte[SIZE_direntry_Total];
        int dirClust2 = 0, subEntry2 = 0;

        /* Check if file already exists */
        RefU32Ret refDirClust2 = new RefU32Ret(dirClust2);
        RefU32Ret refSubEntry2 = new RefU32Ret(subEntry2);
        if (!getFileDirEntry(newName, fileEntry2, refDirClust2, refSubEntry2)) {
            /* Target doesn't exist, can rename */

            CStringPt dirName2 = CStringPt.create(DOSSystem.DOS_NAMELENGTH_ASCII);
            CStringPt pathName2 = CStringPt.create(11);

            /* Can we even get the name of the file itself? */
            if (!getEntryName(newName, dirName2))
                return false;
            convToDirFile(dirName2, pathName2);

            /* Can we find the base directory? */
            refDirClust2.set(dirClust2);
            if (!getDirClustNum(newName, refDirClust2, true))
                return false;
            dirClust2 = refDirClust2.U32;
            ArrayHelper.copy(fileEntry1, fileEntry2, SIZE_direntry_Total);
            for (int i = 0; i < 11; i++) {
                fileEntry2[OFF_direntry_entryname + i] = (byte) pathName2.get(i);
            }
            addDirectoryEntry(dirClust2, fileEntry2);

            /* Check if file exists now */
            refDirClust2.set(dirClust2);
            refSubEntry2.set(subEntry2);
            if (!getFileDirEntry(newName, fileEntry2, refDirClust2, refSubEntry2))
                return false;
            dirClust2 = refDirClust2.U32;
            subEntry2 = refSubEntry2.U32;
            /* Remove old entry */
            fileEntry1[OFF_direntry_entryname] = (byte) 0xe5;
            directoryChange(dirClust1, fileEntry1, subEntry1);

            return true;
        }
        /* Target already exists, fail */
        return false;
    }

    @Override
    public boolean allocationInfo(DriveAllocationInfo alloc) {
        int hs = 0, cy = 0, sect = 0, sectsize = 0;
        int countFree = 0;
        int i;

        hs = LoadedDisk.getGeometryHeads();
        cy = LoadedDisk.getGeometryCylinders();
        sect = LoadedDisk.getGeometrySectors();
        sectsize = LoadedDisk.getGeometrySectSize();
        alloc.bytesSector = 0xffff & sectsize;
        alloc.sectorsCluster = 0xff & _bootbuffer[OFF_bootstrap_sectorspercluster];
        if (_countOfClusters < 65536)
            alloc.totalClusters = _countOfClusters;
        else {
            // maybe some special handling needed for fat32
            alloc.totalClusters = 65535;
        }
        for (i = 0; i < _countOfClusters; i++)
            if (getClusterValue(i + 2) == 0)
                countFree++;
        if (countFree < 65536)
            alloc.freeClusters = countFree;
        else {
            // maybe some special handling needed for fat32
            alloc.freeClusters = 65535;
        }

        return true;
    }

    @Override
    public boolean fileExists(String name) {
        byte[] fileEntry = new byte[SIZE_direntry_Total];
        // int dummy1=0,dummy2=0;
        if (!getFileDirEntry(name, fileEntry, new RefU32Ret(0), new RefU32Ret(0)))
            return false;
        return true;
    }

    @Override
    public boolean fileStat(String name, FileStatBlock statBlock) {
        /* TODO: Stub */
        return false;
    }

    @Override
    public int getMediaByte() {
        return LoadedDisk.getBiosType();
    }

    @Override
    public boolean isRemote() {
        return false;
    }

    @Override
    public boolean isRemovable() {
        return false;
    }

    @Override
    public int unMount() {
        this.dispose();
        return 0;
    }

    public int getAbsoluteSectFromBytePos(int startClustNum, long bytePos) {
        return getAbsoluteSectFromChain(startClustNum,
                (int) (bytePos / ByteConv.getShort(_bootbuffer, OFF_bootstrap_bytespersector)));
    }

    public int getSectorSize() {
        return ByteConv.getShort(_bootbuffer, OFF_bootstrap_bytespersector);
    }

    public int getAbsoluteSectFromChain(int startClustNum, int logicalSector) {
        int skipClust = logicalSector / (_bootbuffer[OFF_bootstrap_sectorspercluster] & 0xff);
        int sectClust = logicalSector % (_bootbuffer[OFF_bootstrap_sectorspercluster] & 0xff);

        int currentClust = startClustNum;
        int testvalue;

        while (skipClust != 0) {
            boolean isEOF = false;
            testvalue = getClusterValue(currentClust);
            switch (_fatType) {
                case FAT12:
                    if (testvalue >= 0xff8)
                        isEOF = true;
                    break;
                case FAT16:
                    if (testvalue >= 0xfff8)
                        isEOF = true;
                    break;
                case FAT32:
                    if (testvalue >= 0xfffffff8)
                        isEOF = true;
                    break;
            }
            if ((isEOF) && (skipClust >= 1)) {
                // Log.LOG_MSG("End of cluster chain reached before end of logical sector seek!");
                return 0;
            }
            currentClust = testvalue;
            --skipClust;
        }

        return (getClustFirstSect(currentClust) + sectClust);
    }

    public boolean allocateCluster(int useCluster, int prevCluster) {

        /* Can't allocate cluster #0 */
        if (useCluster == 0)
            return false;

        if (prevCluster != 0) {
            /* Refuse to allocate cluster if previous cluster value is zero (unallocated) */
            if (getClusterValue(prevCluster) == 0)
                return false;

            /* Point cluster to new cluster in chain */
            setClusterValue(prevCluster, useCluster);
            // Log.LOG_MSG("Chaining cluser %d to %d", prevCluster, useCluster);
        }

        switch (_fatType) {
            case FAT12:
                setClusterValue(useCluster, 0xfff);
                break;
            case FAT16:
                setClusterValue(useCluster, 0xffff);
                break;
            case FAT32:
                setClusterValue(useCluster, 0xffffffff);
                break;
        }
        return true;
    }

    public int appendCluster(int startCluster) {
        int testvalue;
        int currentClust = startCluster;
        boolean isEOF = false;

        while (!isEOF) {
            testvalue = getClusterValue(currentClust);
            switch (_fatType) {
                case FAT12:
                    if (testvalue >= 0xff8)
                        isEOF = true;
                    break;
                case FAT16:
                    if (testvalue >= 0xfff8)
                        isEOF = true;
                    break;
                case FAT32:
                    if (testvalue >= 0xfffffff8)
                        isEOF = true;
                    break;
            }
            if (isEOF)
                break;
            currentClust = testvalue;
        }

        int newClust = getFirstFreeClust();
        /* Drive is full */
        if (newClust == 0)
            return 0;

        if (!allocateCluster(newClust, currentClust))
            return 0;

        zeroOutCluster(newClust);

        return newClust;
    }

    public void deleteClustChain(int startCluster) {
        int testvalue;
        int currentClust = startCluster;
        boolean isEOF = false;
        while (!isEOF) {
            testvalue = getClusterValue(currentClust);
            if (testvalue == 0) {
                /* What the crap? Cluster is already empty - BAIL! */
                break;
            }
            /* Mark cluster as empty */
            setClusterValue(currentClust, 0);
            switch (_fatType) {
                case FAT12:
                    if (testvalue >= 0xff8)
                        isEOF = true;
                    break;
                case FAT16:
                    if (testvalue >= 0xfff8)
                        isEOF = true;
                    break;
                case FAT32:
                    if (testvalue >= 0xfffffff8)
                        isEOF = true;
                    break;
            }
            if (isEOF)
                break;
            currentClust = testvalue;
        }
    }

    public int getFirstFreeClust() {
        int i;
        for (i = 0; i < _countOfClusters; i++) {
            if (getClusterValue(i + 2) == 0)
                return (i + 2);
        }

        /* No free cluster found */
        return 0;
    }

    public boolean directoryBrowse(int dirClustNumber, byte[] useEntry, int entNum) {
        // if (useEntry == null) useEntry= new byte[ SIZE_direntry_Total];

        byte[] sectbuf = new byte[16 * SIZE_direntry_Total]; /* 16 directory entries per sector */
        int logentsector; /* Logical entry sector */
        int entryoffset = 0; /* Index offset within sector */
        int tmpsector;
        int dirPos = 0;

        while (entNum >= 0) {

            logentsector = dirPos / 16;
            entryoffset = dirPos % 16;

            if (dirClustNumber == 0) {

                if (dirPos >= ByteConv.getShort(_bootbuffer, OFF_bootstrap_rootdirentries))
                    return false;
                tmpsector = _firstRootDirSect + logentsector;
                LoadedDisk.readAbsoluteSector(tmpsector, sectbuf, 0);
            } else {
                tmpsector = getAbsoluteSectFromChain(dirClustNumber, logentsector);
                /* A zero sector number can't happen */
                if (tmpsector == 0)
                    return false;
                LoadedDisk.readAbsoluteSector(tmpsector, sectbuf, 0);
            }
            dirPos++;


            /* End of directory list */
            if (sectbuf[entryoffset * SIZE_direntry_Total + OFF_direntry_entryname] == 0x00)
                return false;
            --entNum;
        }
        int offset = entryoffset * SIZE_direntry_Total;
        for (int i = 0; i < SIZE_direntry_Total; i++) {
            useEntry[i] = sectbuf[i + offset];
        }
        return true;
    }

    public boolean directoryChange(int dirClustNumber, byte[] useEntry, int entNum) {
        byte[] sectbuf = new byte[16 * SIZE_direntry_Total]; /* 16 directory entries per sector */
        int logentsector; /* Logical entry sector */
        int entryoffset = 0; /* Index offset within sector */
        int tmpsector = 0;
        int dirPos = 0;

        while (entNum >= 0) {

            logentsector = dirPos / 16;
            entryoffset = dirPos % 16;

            if (dirClustNumber == 0) {
                if (dirPos >= ByteConv.getShort(_bootbuffer, OFF_bootstrap_rootdirentries))
                    return false;
                tmpsector = _firstRootDirSect + logentsector;
                LoadedDisk.readAbsoluteSector(tmpsector, sectbuf, 0);
            } else {
                tmpsector = getAbsoluteSectFromChain(dirClustNumber, logentsector);
                /* A zero sector number can't happen */
                if (tmpsector == 0)
                    return false;
                LoadedDisk.readAbsoluteSector(tmpsector, sectbuf, 0);
            }
            dirPos++;


            /* End of directory list */
            if (sectbuf[entryoffset * SIZE_direntry_Total + OFF_direntry_entryname] == 0x00)
                return false;
            --entNum;
        }
        if (tmpsector != 0) {
            // ArrayHelper.Copy(sectbuf, entryoffset * SIZE_direntry_Total, useEntry, 0,
            // SIZE_direntry_Total);
            int offset = entryoffset * SIZE_direntry_Total;
            for (int i = 0; i < SIZE_direntry_Total; i++) {
                useEntry[i] = sectbuf[i + offset];
            }
            LoadedDisk.writeAbsoluteSector(tmpsector, sectbuf, 0);
            return true;
        } else {
            return false;
        }
    }

    public ImageDisk LoadedDisk = null;
    public boolean createdSuccessfully;

    private int getClusterValue(int clustNum) {
        int fatoffset = 0;
        int fatsectnum = 0;
        int fatentoff = 0;
        int clustValue = 0;

        switch (_fatType) {
            case FAT12:
                fatoffset = clustNum + (clustNum / 2);
                break;
            case FAT16:
                fatoffset = clustNum * 2;
                break;
            case FAT32:
                fatoffset = clustNum * 4;
                break;
        }
        int bytespersector = ByteConv.getShort(_bootbuffer, OFF_bootstrap_bytespersector);
        fatsectnum = ByteConv.getShort(_bootbuffer, OFF_bootstrap_reservedsectors)
                + (fatoffset / bytespersector) + _partSectOff;
        fatentoff = fatoffset % bytespersector;

        if (_curFatSect != fatsectnum) {
            /* Load two sectors at once for FAT12 */
            LoadedDisk.readAbsoluteSector(fatsectnum, _fatSectBuffer, 0);
            if (_fatType == FAT12)
                LoadedDisk.readAbsoluteSector(fatsectnum + 1, _fatSectBuffer, 512);
            _curFatSect = fatsectnum;
        }

        switch (_fatType) {
            case FAT12:
                clustValue = ByteConv.getShort(_fatSectBuffer, fatentoff);
                if ((clustNum & 0x1) != 0) {
                    clustValue >>>= 4;
                } else {
                    clustValue &= 0xfff;
                }
                break;
            case FAT16:
                clustValue = ByteConv.getShort(_fatSectBuffer, fatentoff);
                break;
            case FAT32:
                clustValue = ByteConv.getInt(_fatSectBuffer, fatentoff);
                break;
        }

        return clustValue;
    }

    private void setClusterValue(int clustNum, int clustValue) {
        int fatOffset = 0;
        int fatSectNum = 0;
        int fatEntOff = 0;

        switch (_fatType) {
            case FAT12:
                fatOffset = clustNum + (clustNum / 2);
                break;
            case FAT16:
                fatOffset = clustNum * 2;
                break;
            case FAT32:
                fatOffset = clustNum * 4;
                break;
        }
        int bytespersector = ByteConv.getShort(_bootbuffer, OFF_bootstrap_bytespersector);
        fatSectNum = ByteConv.getShort(_bootbuffer, OFF_bootstrap_reservedsectors)
                + (fatOffset / bytespersector) + _partSectOff;
        fatEntOff = fatOffset % bytespersector;

        if (_curFatSect != fatSectNum) {
            /* Load two sectors at once for FAT12 */
            LoadedDisk.readAbsoluteSector(fatSectNum, _fatSectBuffer, 0);
            if (_fatType == FAT12)
                LoadedDisk.readAbsoluteSector(fatSectNum + 1, _fatSectBuffer, 512);
            _curFatSect = fatSectNum;
        }

        switch (_fatType) {
            case FAT12: {
                int tmpValue = ByteConv.getShort(_fatSectBuffer, fatEntOff);
                if ((clustNum & 0x1) != 0) {
                    clustValue &= 0xfff;
                    clustValue <<= 4;
                    tmpValue &= 0xf;
                    tmpValue |= 0xffff & clustValue;

                } else {
                    clustValue &= 0xfff;
                    tmpValue &= 0xf000;
                    tmpValue |= 0xffff & clustValue;
                }
                ArrayHelper.copy(tmpValue, _fatSectBuffer, fatEntOff, SIZE_USHORT);
                break;
            }
            case FAT16:
                ArrayHelper.copy(clustValue, _fatSectBuffer, fatEntOff, SIZE_USHORT);
                break;
            case FAT32:
                ArrayHelper.copy(clustValue, _fatSectBuffer, fatEntOff, SIZE_UINT);
                break;
        }
        byte fatCopies = _bootbuffer[OFF_bootstrap_fatcopies];
        int sectorPerFat = ByteConv.getShort(_bootbuffer, OFF_bootstrap_sectorsperfat);
        for (int fc = 0; fc < fatCopies; fc++) {
            LoadedDisk.writeAbsoluteSector(fatSectNum + (fc * sectorPerFat), _fatSectBuffer, 0);
            if (_fatType == FAT12) {
                if (fatEntOff >= 511)
                    LoadedDisk.writeAbsoluteSector(fatSectNum + 1 + (fc * sectorPerFat),
                            _fatSectBuffer, 512);
            }
        }
    }

    private int getClustFirstSect(int clustNum) {
        return ((clustNum - 2) * _bootbuffer[OFF_bootstrap_sectorspercluster]) + _firstDataSector;
    }

    private boolean findNextInternal(int dirClustNumber, DOSDTA dta, byte[] foundEntry) {
        byte[] sectBuf = new byte[16 * SIZE_direntry_Total]; /* 16 directory entries per sector */
        int logEntSector; /* Logical entry sector */
        int entryOffset; /* Index offset within sector */
        int tmpSector;
        int attrs = 0;
        int dirPos = 0;
        CStringPt srchPattern = CStringPt.create(DOSSystem.DOS_NAMELENGTH_ASCII);
        CStringPt findName = CStringPt.create(DOSSystem.DOS_NAMELENGTH_ASCII);
        CStringPt extension = CStringPt.create(4);

        attrs = dta.getSearchParams(srchPattern);
        dirPos = dta.getDirID();

        nextfile: while (true) {
            logEntSector = dirPos / 16;
            entryOffset = dirPos % 16;

            if (dirClustNumber == 0) {
                LoadedDisk.readAbsoluteSector(_firstRootDirSect + logEntSector, sectBuf, 0);
            } else {
                tmpSector = getAbsoluteSectFromChain(dirClustNumber, logEntSector);
                /* A zero sector number can't happen */
                if (tmpSector == 0) {
                    DOSMain.setError(DOSMain.DOSERR_NO_MORE_FILES);
                    return false;
                }
                LoadedDisk.readAbsoluteSector(tmpSector, sectBuf, 0);
            }
            dirPos++;
            dta.setDirID(dirPos);

            /* Deleted file entry */
            if (sectBuf[entryOffset * SIZE_direntry_Total + OFF_direntry_entryname] == 0xe5)
                continue nextfile;
            // goto nextfile;

            /* End of directory list */
            if (sectBuf[entryOffset * SIZE_direntry_Total + OFF_direntry_entryname] == 0x00) {
                DOSMain.setError(DOSMain.DOSERR_NO_MORE_FILES);
                return false;
            }
            CStringPt.clear(findName, 0, DOSSystem.DOS_NAMELENGTH_ASCII);
            CStringPt.clear(extension, 0, 4);
            long sectbufOff = entryOffset * SIZE_direntry_Total + OFF_direntry_entryname;
            for (int i = 0; i < 8; i++) {
                sectBuf[(int) sectbufOff + i] = (byte) findName.get(i);
            }
            sectbufOff = entryOffset * SIZE_direntry_Total + OFF_direntry_entryname + 8;
            for (int i = 0; i < 3; i++) {
                sectBuf[(int) sectbufOff + i] = (byte) extension.get(i);
            }
            findName.trim();
            extension.trim();
            if ((sectBuf[entryOffset * SIZE_direntry_Total + OFF_direntry_attrib]
                    & DOSSystem.DOS_ATTR_DIRECTORY) == 0 || extension.get(0) != 0) {
                findName.concat(".");
                findName.concat(extension);
            }

            /*
             * Ignore files with volume label. FindFirst should search for those. (return the first
             * one found)
             */
            if ((sectBuf[entryOffset * SIZE_direntry_Total + OFF_direntry_attrib] & 0x8) != 0)
                continue nextfile;
            // goto nextfile;

            /* Always find ARCHIVES even if bit is not set Perhaps test is not the best test */
            if ((~attrs & sectBuf[entryOffset * SIZE_direntry_Total + OFF_direntry_attrib]
                    & (DOSSystem.DOS_ATTR_DIRECTORY | DOSSystem.DOS_ATTR_HIDDEN
                            | DOSSystem.DOS_ATTR_SYSTEM)) != 0)
                continue nextfile;
            // goto nextfile;
            if (!Drives.compareWildFile(findName.toString(), srchPattern.toString()))
                continue nextfile;
            // goto nextfile;

            sectbufOff = entryOffset * SIZE_direntry_Total;
            int entrysize = ByteConv.getInt(sectBuf, (int) sectbufOff + OFF_direntry_entrysize);
            int crtDate = ByteConv.getShort(sectBuf, (int) sectbufOff + OFF_direntry_crtDate);
            int crtTime = ByteConv.getShort(sectBuf, (int) sectbufOff + OFF_direntry_crtTime);
            byte attrib = sectBuf[(int) sectbufOff + OFF_direntry_attrib];
            dta.setResult(findName, entrysize, crtDate, crtTime, attrib);
            for (int i = 0; i < SIZE_direntry_Total; i++) {
                foundEntry[i] = sectBuf[i + (int) sectbufOff];
            }
            return true;
        }
    }

    private boolean getDirClustNum(String dir, RefU32Ret refClustNum, boolean parDir) {
        int len = dir.length();
        int currentClust = 0;
        byte[] foundEntry = null;
        String findDir = null;

        // '\\'로 끝나는 문자열은 root디렉토리
        // 그렇지 않으면 파일경로로 보는 듯
        // 파일이 속한 디렉토리이름(절대 경로가 아님)을 구한다.

        /* Skip if testing for root directory */
        if ((len > 0) && (dir.charAt(len - 1) != '\\')) {
            // Log.LOG_MSG("Testing for dir %s", dir);
            // findDir = strtok(dirtoken, "\\");

            int idx = 0;
            int endIdx = dir.indexOf('\\');
            findDir = dir.substring(idx, endIdx);

            DOSDTA imgDTA = BIOSDisk.ImgDTA;
            while (endIdx >= 0 && endIdx < dir.length()) {

                imgDTA.setupSearch(0, DOSSystem.DOS_ATTR_DIRECTORY, findDir);
                imgDTA.setDirID(0);

                idx = endIdx + 1;
                endIdx = dir.indexOf('\\', idx);

                if (parDir && (endIdx < 0))
                    break;

                findDir = dir.substring(idx, endIdx);

                if (!findNextInternal(currentClust, imgDTA, foundEntry)) {
                    return false;
                } else {
                    if ((imgDTA.getResultAttr() & DOSSystem.DOS_ATTR_DIRECTORY) == 0)
                        return false;
                }
                currentClust = ByteConv.getShort(foundEntry, OFF_direntry_loFirstClust);

            }
            refClustNum.U32 = currentClust;
        } else {
            /* Set to root directory */
            refClustNum.U32 = 0;
        }
        return true;
    }

    private boolean getFileDirEntry(String fileName, byte[] useEntry, RefU32Ret refDirClust,
            RefU32Ret refSubEntry) {
        int len = fileName.length();

        int currentClust = 0;

        byte[] foundEntry = null;
        String findDir = null;

        // CStringPt.Copy(filename, dirtoken);
        String findFile = null;

        // '\\'로 끝나는 문자열은 root디렉토리
        // 그렇지 않으면 파일경로로 보는 듯
        // 파일이 속한 디렉토리이름(절대 경로가 아님)을 구한다.

        DOSDTA imgDTA = BIOSDisk.ImgDTA;
        /* Skip if testing in root directory */
        if ((len > 0) && (fileName.charAt(len - 1) != '\\')) {
            // Log.LOG_MSG("Testing for filename %s", filename);
            // findDir = strtok(dirtoken, "\\");

            int idx = 0;
            int end_idx = fileName.indexOf('\\');

            while (end_idx >= 0 && end_idx < fileName.length()) {
                findDir = fileName.substring(idx, end_idx);

                imgDTA.setupSearch(0, DOSSystem.DOS_ATTR_DIRECTORY, findDir);
                imgDTA.setDirID(0);

                findFile = findDir;
                if (!findNextInternal(currentClust, imgDTA, foundEntry))
                    break;
                else {
                    // Found something. See if it's a directory (findfirst always finds regular
                    // files)
                    if ((imgDTA.getResultAttr() & DOSSystem.DOS_ATTR_DIRECTORY) == 0)
                        break;
                }
                currentClust = ByteConv.getShort(foundEntry, OFF_direntry_loFirstClust);
                idx = end_idx + 1;
                end_idx = fileName.indexOf('\\', idx);

            }
        } else {
            /* Set to root directory */
        }

        /* Search found directory for our file */
        imgDTA.setupSearch(0, 0x7, findFile);
        imgDTA.setDirID(0);
        if (!findNextInternal(currentClust, imgDTA, foundEntry))
            return false;

        for (int i = 0; i < SIZE_direntry_Total; i++) {
            useEntry[i] = foundEntry[i];
        }
        refDirClust.U32 = currentClust;
        refSubEntry.U32 = imgDTA.getDirID() - 1;
        return true;
    }

    private boolean addDirectoryEntry(int dirClustNumber, byte[] useEntry) {
        byte[] sectBuf = new byte[16 * SIZE_direntry_Total]; /* 16 directory entries per sector */
        int logEntSector; /* Logical entry sector */
        int entryOffset; /* Index offset within sector */
        int tmpSector;
        int dirPos = 0;

        while (true) {

            logEntSector = dirPos / 16;
            entryOffset = dirPos % 16;

            if (dirClustNumber == 0) {
                if (dirPos >= ByteConv.getShort(_bootbuffer, OFF_bootstrap_rootdirentries))
                    return false;
                tmpSector = _firstRootDirSect + logEntSector;
                LoadedDisk.readAbsoluteSector(tmpSector, sectBuf, 0);
            } else {
                tmpSector = getAbsoluteSectFromChain(dirClustNumber, logEntSector);
                /*
                 * A zero sector number can't happen - we need to allocate more room for this
                 * directory
                 */
                if (tmpSector == 0) {
                    int newClust;
                    newClust = appendCluster(dirClustNumber);
                    if (newClust == 0)
                        return false;
                    /* Try again to get tmpsector */
                    tmpSector = getAbsoluteSectFromChain(dirClustNumber, logEntSector);
                    if (tmpSector == 0)
                        return false; /* Give up if still can't get more room for directory */
                }
                LoadedDisk.readAbsoluteSector(tmpSector, sectBuf, 0);
            }
            dirPos++;

            /* Deleted file entry or end of directory list */
            int direntry_entryname =
                    sectBuf[entryOffset * SIZE_direntry_Total + OFF_direntry_entryname];
            if ((direntry_entryname == 0xe5) || (sectBuf[direntry_entryname] == 0x00)) {
                // sectbuf[entryoffset] = useEntry;
                ArrayHelper.copy(useEntry, 0, sectBuf, entryOffset * SIZE_direntry_Total,
                        SIZE_direntry_Total);
                LoadedDisk.writeAbsoluteSector(tmpSector, sectBuf, 0);
                break;
            }
        }

        return true;
    }

    private void zeroOutCluster(int clustNumber) {
        byte[] secBuffer = new byte[512];

        Arrays.fill(secBuffer, 0, 512, (byte) 0);
        int i;
        for (i = 0; i < _bootbuffer[OFF_bootstrap_sectorspercluster]; i++) {
            LoadedDisk.writeAbsoluteSector(getAbsoluteSectFromChain(clustNumber, i), secBuffer, 0);
        }
    }

    private boolean getEntryName(String fullName, CStringPt entName) {
        // Log.LOG_MSG("Testing for filename %s", fullname);

        String fileName = Paths.get(fullName).getFileName().toString();
        CStringPt.copy(fileName, entName);

        return true;
    }


    private static void convToDirFile(CStringPt fileName, CStringPt fileArray) {
        int charIdx = 0;
        int fLen, i;
        fLen = fileName.length();
        for (i = 0; i < 11; i++)
            fileArray.set(i, (char) 32);
        for (i = 0; i < fLen; i++) {
            if (charIdx >= 11)
                break;
            if (fileName.get(i) != '.') {
                fileArray.set(charIdx, fileName.get(i));
                charIdx++;
            } else {
                charIdx = 8;
            }
        }
    }

    private CStringPt basedir = CStringPt.create(Cross.LEN);

    // private friend void DOS_Shell::CMD_SUBST(char* args);
    private class SrchInfo {
        public CStringPt srchDir;

        public SrchInfo() {
            this.srchDir = CStringPt.create(Cross.LEN);
        }
    }

    private SrchInfo[] _srchInfo = new SrchInfo[DOSSystem.MAX_OPENDIRS];

    private class Allocation {
        public short bytesSector;
        public byte sectorsCluster;
        public short totalClusters;
        public short freeClusters;
        public byte mediaId;
    }

    private Allocation _allocation = new Allocation();

    private byte[] _bootbuffer = new byte[SIZE_bootstrap_Total];
    private byte _fatType;
    private int _countOfClusters;
    private int _partSectOff;
    private int _firstDataSector;
    private int _firstRootDirSect;

    private int _cwdDirCluster;
    private int _dirPosition; /* Position in directory search */
}
