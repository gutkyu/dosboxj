package org.gutkyu.dosboxj.dos.system.drive;

import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.gutkyu.dosboxj.dos.*;
import org.gutkyu.dosboxj.dos.mem_block.*;
import org.gutkyu.dosboxj.dos.system.file.*;
import org.gutkyu.dosboxj.misc.*;
import org.gutkyu.dosboxj.util.*;

public final class LocalDrive extends DOSDrive implements Disposable {
    public LocalDrive(String startDir, short bytesSector, byte sectorsCluster, short totalClusters,
            short freeClusters, byte mediaId) {
        for (int i = 0; i < _srchInfo.length; i++) {
            _srchInfo[i] = new SrchInfo();
        }

        CStringPt.copy(startDir, basedir);
        info = String.format("local directory %s", startDir);
        _allocation.bytesSector = bytesSector;
        _allocation.sectorsCluster = sectorsCluster;
        _allocation.totalClusters = totalClusters;
        _allocation.freeClusters = freeClusters;
        _allocation.mediaId = mediaId;

        dirCache.setBaseDir(basedir);
    }

    public void dispose() {
        dispose(true);
    }

    private void dispose(boolean disposing) {

    }

    @Override
    public DOSFile fileOpen(String name, int flags) {
        DOSFile file = null;
        // FileMode mode; FileAccess access;

        OpenOption[] openOptions;
        switch (flags & 0xf) {
            case DOSSystem.OPEN_READ:
                openOptions = new OpenOption[] {StandardOpenOption.READ};
                break;
            case DOSSystem.OPEN_WRITE:
                openOptions = new OpenOption[] {StandardOpenOption.WRITE};
                break;
            case DOSSystem.OPEN_READWRITE:
                openOptions = new OpenOption[] {StandardOpenOption.READ, StandardOpenOption.WRITE};

                break;
            default:
                DOSMain.setError(DOSMain.DOSERR_ACCESS_CODE_INVALID);
                return null;
        }
        CStringPt newname = CStringPt.create((int) Cross.LEN);
        CStringPt.copy(basedir, newname);
        newname.concat(name);

        dirCache.expandName(newname);
        SeekableByteChannel chann = null;
        try {
            chann = Files.newByteChannel(Paths.get(newname.toString()), openOptions);
        } catch (Exception ex) {
            Log.logMsg("file :{0}\n{1}", newname, ex.toString());
            return null;
        }
        // int err=errno;

        file = new LocalFile(name, chann);

        file.Flags = flags; // for the inheritance flag and maybe check for others.
        // (*file).SetFileName(newname);
        return file;
    }

    public SeekableByteChannel getSystemFileChannel(String name, OpenOption... options) {
        CStringPt newname = CStringPt.create((int) Cross.LEN);
        CStringPt.copy(basedir, newname);
        newname.concat(name);
        dirCache.expandName(newname);
        Path path = Paths.get(newname.toString());
        SeekableByteChannel chann = null;
        try {
            chann = Files.newByteChannel(path, options);
        } catch (Exception ex) {
            return chann;
        }
        return chann;
    }

    public boolean getSystemFilename(CStringPt sysName, CStringPt dosName) {
        CStringPt.copy(basedir, sysName);
        sysName.concat(dosName);
        dirCache.expandName(sysName);
        return true;
    }

    @Override
    public DOSFile fileCreate(String name, short attributes) {
        DOSFile file = null;
        // TODO Maybe care for attributes but not likely
        CStringPt newName = CStringPt.create((int) Cross.LEN);
        CStringPt.copy(basedir, newName);
        newName.concat(name);
        // Can only be used in till a new drive_cache action is preformed */
        CStringPt tempName = dirCache.getExpandName(newName);
        /* Test if file exists (so we need to truncate it). don't add to dirCache then */
        boolean existingFile = false;

        // FILE * test=fopen(temp_name,"rb+");
        Path path = Paths.get(tempName.toString());

        existingFile = Files.exists(path);

        // FILE * hand=fopen(temp_name,"wb+");
        SeekableByteChannel chann = null;
        try {
            chann = Files.newByteChannel(path, StandardOpenOption.READ, StandardOpenOption.WRITE);
        } catch (Exception e) {
            Log.logMsg("Warning: file creation failed: %s", newName.toString());
            return null;
        }

        if (!existingFile)
            dirCache.addEntry(newName, true);
        /* Make the 16 bit device information */
        file = new LocalFile(name, chann);
        file.Flags = DOSSystem.OPEN_READWRITE;

        return file;
    }

    @Override
    public boolean fileUnlink(CStringPt name) {
        CStringPt newName = CStringPt.create((int) Cross.LEN);
        CStringPt.copy(basedir, newName);
        newName.concat(name);
        CStringPt fullname = dirCache.getExpandName(newName);
        Path path = Paths.get(fullname.toString());
        boolean isUnlink = false;
        try {
            Files.delete(path);
            isUnlink = true;
        } catch (Exception e) {
        }

        if (isUnlink) {
            dirCache.deleteEntry(newName);
            return true;
        } else {
            // Unlink failed for some reason try finding it.
            if (!Files.exists(path))
                return false;// File not found.

            // FILE* file_writable = fopen(fullname,"rb+");
            SeekableByteChannel chann = null;
            try {
                chann = Files.newByteChannel(path, StandardOpenOption.READ,
                        StandardOpenOption.WRITE);
                chann.close();
            } catch (Exception e) {
                return false; // No acces ? ERROR MESSAGE NOT SET. FIXME ?
            }
            chann = null;

            // File exists and can technically be deleted, nevertheless it failed.
            // This means that the file is probably open by some process.
            // See if We have it open.
            boolean found_file = false;
            for (int i = 0; i < DOSMain.DOS_FILES; i++) {
                if (DOSMain.Files[i] != null && DOSMain.Files[i].isName(name)) {
                    int max = DOSMain.DOS_FILES;
                    while (DOSMain.Files[i].isOpen() && max-- > 0) {
                        DOSMain.Files[i].close();
                        if (DOSMain.Files[i].removeRef() <= 0)
                            break;
                    }
                    found_file = true;
                }
            }
            if (!found_file)
                return false;

            isUnlink = false;
            try {
                Files.delete(path);
                isUnlink = true;
            } catch (Exception e) {
            }

            if (isUnlink) {
                dirCache.deleteEntry(newName);
                return true;
            }
            return false;
        }

    }

    @Override
    public boolean removeDir(CStringPt dir) {
        CStringPt newdir = CStringPt.create((int) Cross.LEN);
        CStringPt.copy(basedir, newdir);
        newdir.concat(dir);

        try {
            Files.delete(Paths.get(dirCache.getExpandName(newdir).toString()));
        } catch (Exception e) {
            return false;
        }

        dirCache.deleteEntry(newdir, true);
        return true;
    }

    @Override
    public boolean makeDir(CStringPt dir) {
        CStringPt newdir = CStringPt.create((int) Cross.LEN);
        CStringPt.copy(basedir, newdir);
        newdir.concat(dir);
        Path dirPath = Paths.get(dirCache.getExpandName(newdir).toString());
        try {
            Files.createDirectory(dirPath);
        } catch (Exception e) {
            return false;
        }
        dirCache.cacheOut(newdir, true);

        return true;// || ((temp!=0) && (errno==EEXIST));
    }

    @Override
    public boolean testDir(CStringPt dir) {
        CStringPt newdir = CStringPt.create((int) Cross.LEN);
        CStringPt.copy(basedir, newdir);
        newdir.concat(dir);
        dirCache.expandName(newdir);
        Path path = Paths.get(newdir.toString());
        // Skip directory test, if "\"
        int len = newdir.length();
        if (len > 0 && (newdir.get(len - 1) != '\\')) {
            // It has to be a directory !
            if (!Files.exists(path) || !Files.isDirectory(path))
                return false;
        }
        return Files.exists(path);
    }


    @Override
    public boolean testDir(String dir) {
        CStringPt newdir = CStringPt.create((int) Cross.LEN);
        CStringPt.copy(basedir, newdir);
        newdir.concat(dir);
        dirCache.expandName(newdir);
        Path path = Paths.get(newdir.toString());
        // Skip directory test, if "\"
        int len = newdir.length();
        if (len > 0 && (newdir.get(len - 1) != '\\')) {
            // It has to be a directory !
            if (!Files.exists(path) || !Files.isDirectory(path))
                return false;
        }
        return Files.exists(path);
    }


    @Override
    public boolean findFirst(CStringPt dir, DOSDTA dta, boolean fcbFindfirst) {
        CStringPt tempDir = CStringPt.create((int) Cross.LEN);
        CStringPt.copy(basedir, tempDir);
        tempDir.concat(dir);

        if (_allocation.mediaId == 0xF0) {
            emptyCache(); // rescan floppie-content on each findfirst
        }

        char[] end = {Cross.FILESPLIT, (char) 0};
        if (tempDir.get(tempDir.length() - 1) != Cross.FILESPLIT)
            tempDir.concat(end);

        short id = 0;
        RefU16Ret refId = new RefU16Ret(id);
        if (!dirCache.findFirst(tempDir, refId)) {
            DOSMain.setError(DOSMain.DOSERR_PATH_NOT_FOUND);
            return false;
        }
        id = refId.U16;
        CStringPt.copy(tempDir, _srchInfo[id].srchDir);
        dta.setDirID(id);

        byte sAttr = dta.getSearchParams(tempDir);

        if (this.isRemote() && this.isRemovable()) {
            // cdroms behave a bit different than regular drives
            if (sAttr == DOSSystem.DOS_ATTR_VOLUME) {
                dta.setResult(dirCache.getLabel(), 0, (short) 0, (short) 0,
                        (byte) DOSSystem.DOS_ATTR_VOLUME);
                return true;
            }
        } else {
            if (sAttr == DOSSystem.DOS_ATTR_VOLUME) {
                if (dirCache.getLabel().toString() == "") {
                    // LOG(LOG_DOSMISC,LOG_ERROR)("DRIVELABEL REQUESTED: none present, returned
                    // NOLABEL");
                    // dta.SetResult("NO_LABEL",0,0,0,dos_system.DOS_ATTR_VOLUME);
                    // return true;
                    DOSMain.setError(DOSMain.DOSERR_NO_MORE_FILES);
                    return false;
                }
                dta.setResult(dirCache.getLabel(), 0, (short) 0, (short) 0,
                        (byte) DOSSystem.DOS_ATTR_VOLUME);
                return true;
            } else if ((sAttr & DOSSystem.DOS_ATTR_VOLUME) != 0 && (dir.get() == 0)
                    && !fcbFindfirst) {
                // should check for a valid leading directory instead of 0
                // exists==true if the volume label matches the searchmask and the path is valid
                if (Drives.compareWildFile(dirCache.getLabel().toString(), tempDir.toString())) {
                    dta.setResult(dirCache.getLabel(), 0, (short) 0, (short) 0,
                            (byte) DOSSystem.DOS_ATTR_VOLUME);
                    return true;
                }
            }
        }
        return this.findNext(dta);
    }

    @Override

    public boolean findFirst(CStringPt dir, DOSDTA dta) {
        return findFirst(dir, dta, false);
    }

    @Override
    public boolean findNext(DOSDTA dta) {

        CStringPt dirEnt = CStringPt.create();
        // struct stat stat_block;
        CStringPt fullName = CStringPt.create((int) Cross.LEN);
        CStringPt dirEntCopy = CStringPt.create((int) Cross.LEN);

        byte srchAttr = 0;
        CStringPt srchPattern = CStringPt.create((int) DOSSystem.DOS_NAMELENGTH_ASCII);
        byte findAttr = dta.getSearchParams(srchPattern);
        short id = dta.getDirID();

        BasicFileAttributes attr = null;
        again: while (true) {
            if (!dirCache.findNext(id, dirEnt)) {
                DOSMain.setError(DOSMain.DOSERR_NO_MORE_FILES);
                return false;
            }
            if (!Drives.compareWildFile(dirEnt.toString(), srchPattern.toString()))
                continue again;// goto again;

            CStringPt.copy(_srchInfo[id].srchDir, fullName);
            fullName.concat(dirEnt);

            // GetExpandName might indirectly destroy dir_ent (by caching in a new directory
            // and due to its design dir_ent might be lost.)
            // Copying dir_ent first
            CStringPt.copy(dirEnt, dirEntCopy);
            Path path = Paths.get(fullName.toString());
            try {
                attr = Files.readAttributes(path, BasicFileAttributes.class);
            } catch (Exception e) {
                continue again;// goto again;//No symlinks and such
            }
            if (attr.isDirectory())
                findAttr = DOSSystem.DOS_ATTR_DIRECTORY;
            else
                findAttr = DOSSystem.DOS_ATTR_ARCHIVE;
            if ((~srchAttr & findAttr & (DOSSystem.DOS_ATTR_DIRECTORY | DOSSystem.DOS_ATTR_HIDDEN
                    | DOSSystem.DOS_ATTR_SYSTEM)) != 0)
                continue again;// goto again;

            break;
        }
        /* file is okay, setup everything to be copied in DTA Block */
        CStringPt findName = CStringPt.create((int) DOSSystem.DOS_NAMELENGTH_ASCII);
        short findDate, findTime;
        int findSize = 0;

        if (dirEntCopy.length() < DOSSystem.DOS_NAMELENGTH_ASCII) {
            CStringPt.copy(dirEntCopy, findName);
            findName.upper();
        }
        findSize = (int) attr.size();
        FileTime mTime = attr.lastModifiedTime();
        LocalDateTime dt = LocalDateTime.ofInstant(mTime.toInstant(), ZoneOffset.UTC);
        if (dt != null) {
            findDate = DOSMain.packDate((short) dt.getYear(), (short) dt.getMonthValue(),
                    (short) dt.getDayOfMonth());
            findTime = DOSMain.packTime((short) dt.getHour(), (short) dt.getMinute(),
                    (short) dt.getSecond());
        } else {
            findTime = 6;
            findDate = 4;
        }
        dta.setResult(findName, findSize, findDate, findTime, findAttr);
        return true;
    }

    @Override
    public boolean getFileAttr(String name, RefU16Ret refAttr) {
        CStringPt newName = CStringPt.create((int) Cross.LEN);
        CStringPt.copy(basedir, newName);
        newName.concat(name);
        dirCache.expandName(newName);

        Path path = Paths.get(newName.toString());
        if (Files.exists(path)) {
            refAttr.U16 = DOSSystem.DOS_ATTR_ARCHIVE;
            return true;
        } else if (Files.exists(path)) {
            refAttr.U16 = DOSSystem.DOS_ATTR_ARCHIVE | DOSSystem.DOS_ATTR_DIRECTORY;
            return true;
        }
        refAttr.U16 = 0;
        return false;
    }

    @Override
    public boolean rename(String oldName, String newName) {
        CStringPt newOld = CStringPt.create((int) Cross.LEN);
        CStringPt.copy(basedir, newOld);
        newOld.concat(oldName);

        dirCache.expandName(newOld);

        CStringPt newnew = CStringPt.create((int) Cross.LEN);
        CStringPt.copy(basedir, newnew);
        newnew.concat(newName);
        Path src = Paths.get(newOld.toString());
        Path trg = Paths.get(dirCache.getExpandName(newnew).toString());
        try {
            Files.move(src, trg);
        } catch (Exception e) {
            return false;
        }
        dirCache.cacheOut(newnew);
        return true;

    }

    @Override
    public boolean allocationInfo(DriveAllocationInfo alloc) {
        /* Always report 100 mb free should be enough */
        /* Total size is always 1 gb */
        alloc.bytesSector = _allocation.bytesSector;
        alloc.sectorsCluster = _allocation.sectorsCluster;
        alloc.totalClusters = _allocation.totalClusters;
        alloc.freeClusters = _allocation.freeClusters;
        return true;
    }

    @Override
    public boolean fileExists(String name) {
        CStringPt newName = CStringPt.create((int) Cross.LEN);
        CStringPt.copy(basedir, newName);
        newName.concat(name);

        dirCache.expandName(newName);
        return Files.exists(Paths.get(newName.toString()));
    }

    @Override
    public boolean fileStat(String name, FileStatBlock statBlock) {
        CStringPt newName = CStringPt.create((int) Cross.LEN);
        CStringPt.copy(basedir, newName);
        newName.concat(name);

        dirCache.expandName(newName);
        Path path = Paths.get(newName.toString());

        BasicFileAttributes attr = null;
        try {
            attr = Files.readAttributes(path, BasicFileAttributes.class);// st_mtime
        } catch (Exception e) {
            return false;
        }
        /* Convert the stat to a FileStat */
        FileTime mtime = attr.lastModifiedTime();
        LocalDateTime dt = LocalDateTime.ofInstant(mtime.toInstant(), ZoneOffset.UTC);
        if (dt != null) {
            statBlock.Date = DOSMain.packDate((short) dt.getYear(), (short) dt.getMonthValue(),
                    (short) dt.getDayOfMonth());
            statBlock.Time = DOSMain.packTime((short) dt.getHour(), (short) dt.getMinute(),
                    (short) dt.getSecond());
        } else {

        }
        statBlock.Size = (int) attr.size();
        return true;
    }


    @Override
    public byte getMediaByte() {
        return _allocation.mediaId;
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

    public CStringPt basedir = CStringPt.create((int) Cross.LEN);

    // private friend void DOS_Shell::CMD_SUBST(char* args);
    private class SrchInfo {
        public CStringPt srchDir;

        public SrchInfo() {
            this.srchDir = CStringPt.create((int) Cross.LEN);
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
}