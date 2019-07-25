package org.gutkyu.dosboxj.dos.system.drive;

import org.gutkyu.dosboxj.dos.mem_block.*;
import org.gutkyu.dosboxj.util.*;
import org.gutkyu.dosboxj.dos.*;
import org.gutkyu.dosboxj.dos.system.file.*;
import org.gutkyu.dosboxj.dos.system.drive.VFile.VFileBlock;


public final class VirtualDrive extends DOSDrive {

    public VirtualDrive() {
        info = "Internal Virtual Drive";
        searchFile = null;
    }

    @Override
    public DOSFile fileOpen(String name, int flags) {
        DOSFile file = null;
        /* Scan through the internal list of files */
        VFileBlock curFile = VFile.firstFile;
        while (curFile != null) {
            if (curFile.Name.equalsIgnoreCase(name)) {
                /* We have a match */
                file = new VirtualFile(curFile.Data, curFile.Size);
                file.Flags = flags;
                return file;
            }
            curFile = curFile.Next;
        }
        return file;
    }

    @Override
    public DOSFile fileCreate(String name, int attributes) {
        return null;
    }

    @Override
    public boolean fileUnlink(String name) {
        return false;
    }

    @Override
    public boolean removeDir(String dir) {
        return false;
    }

    @Override
    public boolean makeDir(String dir) {
        return false;
    }

    @Override
    public boolean testDir(CStringPt dir) {
        if (dir.get(0) == 0)
            return true; // only valid dir is the empty dir
        return false;
    }


    @Override
    public boolean testDir(String dir) {
        if (dir.length() == 0)
            return true; // only valid dir is the empty dir
        return false;
    }


    private static CStringPt cstrDOSBOX = CStringPt.create("DOSBOX");

    @Override
    public boolean findFirst(CStringPt _dir, DOSDTA dta, boolean fcbFindFirst) {
        searchFile = VFile.firstFile;
        CStringPt pattern = CStringPt.create(DOSSystem.DOS_NAMELENGTH_ASCII);
        int attr = dta.getSearchParams(pattern);
        if (attr == DOSSystem.DOS_ATTR_VOLUME) {
            dta.setResult(cstrDOSBOX, 0, 0, 0, DOSSystem.DOS_ATTR_VOLUME);
            return true;
        } else if ((attr & DOSSystem.DOS_ATTR_VOLUME) != 0 && !fcbFindFirst) {
            if (Drives.compareWildFile("DOSBOX", pattern.toString())) {
                dta.setResult(cstrDOSBOX, 0, 0, 0, DOSSystem.DOS_ATTR_VOLUME);
                return true;
            }
        }
        return findNext(dta);
    }

    @Override
    public boolean findFirst(CStringPt dir, DOSDTA dta) {
        return findFirst(dir, dta, false);
    }

    @Override
    public boolean findNext(DOSDTA dta) {
        CStringPt pattern = CStringPt.create(DOSSystem.DOS_NAMELENGTH_ASCII);
        int attr = dta.getSearchParams(pattern);
        while (searchFile != null) {
            if (Drives.compareWildFile(searchFile.Name.toString(), pattern.toString())) {
                dta.setResult(searchFile.Name, searchFile.Size, searchFile.Date, searchFile.Time,
                        DOSSystem.DOS_ATTR_ARCHIVE);
                searchFile = searchFile.Next;
                return true;
            }
            searchFile = searchFile.Next;
        }
        DOSMain.setError(DOSMain.DOSERR_NO_MORE_FILES);
        return false;
    }

    public int returnedFileAttr = 0;

    @Override
    public boolean tryFileAttr(String name) {
        VFileBlock curFile = VFile.firstFile;
        while (curFile != null) {
            if (curFile.Name.equalsIgnoreCase(name)) {
                returnedFileAttr = 0xff & DOSSystem.DOS_ATTR_ARCHIVE; // Maybe final ?
                return true;
            }
            curFile = curFile.Next;
        }
        return false;
    }

    @Override
    public int returnFileAttr() {
        return returnedFileAttr;
    }

    @Override
    public boolean rename(String oldName, String newName) {
        return false;
    }

    @Override
    public boolean allocationInfo(DriveAllocationInfo alloc) {
        /* Always report 100 mb free should be enough */
        /* Total size is always 1 gb */
        alloc.bytesSector = 512;
        alloc.sectorsCluster = 127;
        alloc.totalClusters = 16513;
        alloc.freeClusters = 00;
        return true;
    }

    @Override
    public boolean fileExists(String name) {
        VFileBlock curFile = VFile.firstFile;
        while (curFile != null) {
            if (curFile.Name.equalsIgnoreCase(name))
                return true;
            curFile = curFile.Next;
        }
        return false;
    }

    @Override
    public boolean fileStat(String name, FileStatBlock statBlock) {
        VFileBlock curFile = VFile.firstFile;
        while (curFile != null) {
            if (curFile.Name.equalsIgnoreCase(name)) {
                statBlock.Attr = DOSSystem.DOS_ATTR_ARCHIVE;
                statBlock.Size = curFile.Size;
                statBlock.Date = DOSMain.packDate(2002, 10, 1);
                statBlock.Time = DOSMain.packTime(12, 34, 56);
                return true;
            }
            curFile = curFile.Next;
        }
        return false;
    }

    @Override
    public int getMediaByte() {
        return 0xF8;
    }

    @Override
    public void emptyCache() {
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
        return 1;
    }

    private VFileBlock searchFile;



}
