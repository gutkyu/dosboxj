package org.gutkyu.dosboxj.dos.system.drive;

import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.gutkyu.dosboxj.dos.DOSSystem;
import org.gutkyu.dosboxj.misc.*;
import org.gutkyu.dosboxj.util.*;

public class DOSDriveCache implements Disposable {
    public DOSDriveCache() {
        dirBase = new CFileInfo();
        save_dir = null;
        srchNr = 0;
        label.set(0, (char) 0);
        nextFreeFindFirst = 0;
        for (int i = 0; i < DOSSystem.MAX_OPENDIRS; i++) {
            dirSearch[i] = null;
            free[i] = true;
            dirFindFirst[i] = null;
        }
        setDirSort(TDirSort.DIRALPHABETICAL);
        updatelabel = true;
    }

    public DOSDriveCache(CStringPt path) {
        dirBase = new CFileInfo();
        save_dir = null;
        srchNr = 0;
        label.set(0, (char) 0);
        nextFreeFindFirst = 0;
        for (int i = 0; i < DOSSystem.MAX_OPENDIRS; i++) {
            dirSearch[i] = null;
            free[i] = true;
            dirFindFirst[i] = null;
        }
        setDirSort(TDirSort.DIRALPHABETICAL);
        setBaseDir(path);
        updatelabel = true;
    }

    private boolean _isDisposed = false;

    public void dispose() {
        dispose(true);
    }

    protected void dispose(boolean disposing) {
        if (_isDisposed) {
            if (disposing) {
            }
            clear();
            for (int i = 0; i < DOSSystem.MAX_OPENDIRS; i++) {
                dirFindFirst[i].dispose();
                dirFindFirst[i] = null;
            }
            _isDisposed = true;
        }
    }

    public enum TDirSort {
        NOSORT, ALPHABETICAL, DIRALPHABETICAL, ALPHABETICALREV, DIRALPHABETICALREV
    }

    public void setBaseDir(CStringPt baseDir) {
        int id = 0;
        CStringPt.copy(baseDir, basePath);
        if (tryOpenDir(baseDir)) {
            id = returnedOpenDirId;
            // char* result = 0;
            CStringPt result = CStringPt.create();
            readDir(id, result);
        }
        // Get Volume Label

        CStringPt labellocal = CStringPt.create(256);
        labellocal.set(0, (char) 0);
        CStringPt drive = CStringPt.create("C:\\");
        drive.set(0, basePath.get(0));
        Path path = Paths.get(drive.toString());
        boolean isCdrom = false;
        try {
            FileStore fsto = Files.getFileStore(path);
            isCdrom = (boolean) fsto.getAttribute("volume:isCdrom");
        } catch (Exception e) {
            Log.logging(Log.LogTypes.MISC, Log.LogServerities.Error,
                    String.format(
                            "DIRCACHE: SetBaseDir(): drive '%1$s', cdrom type check error - %2$s",
                            path.toString(), e.getMessage()));
        }
        setLabel(labellocal.toString(), isCdrom, true);

    }

    public void setDirSort(TDirSort sort) {
        sortDirType = sort;
    }

    public boolean tryOpenDir(CStringPt path) {
        CStringPt expand = CStringPt.create(Cross.LEN);
        expand.set(0, (char) 0);
        CFileInfo dir = findDirInfo(path, expand);
        if (openDir(dir, expand)) {
            dirSearch[returnedOpenDirId].nextEntry = 0;
            return true;
        }
        return false;
    }

    // (uint16, string)
    public boolean readDir(int id, CStringPt result) {
        // shouldnt happen...
        if (id > DOSSystem.MAX_OPENDIRS)
            return false;

        if (!isCachedIn(dirSearch[id])) {
            // 디렉토리 확인 구문
            // Try to open directory
            // ...

            Path path = Paths.get(dirPath.toString());
            try {
                if (!Files.exists(path)) {
                    return false;
                }


                // Read complete directory
                // ...

                if (path.getParent() != null) {
                    createEntry(dirSearch[id], ".", true);
                    createEntry(dirSearch[id], "..", true);
                }
                Files.walk(path).forEach(p -> {
                    createEntry(dirSearch[id], p.getFileName().toString(), Files.isDirectory(p));
                });
            } catch (Exception e) {
                free[id] = true;

                return false;
            }

            // Info
            // ...
        }
        if (setResult(dirSearch[id], result, dirSearch[id].nextEntry))
            return true;
        free[id] = true;
        return false;
    }

    public void expandName(CStringPt path) {
        CStringPt.copy(getExpandName(path), path);
    }

    private CStringPt work = CStringPt.create(Cross.LEN);

    public CStringPt getExpandName(CStringPt path) {
        work.set(0, (char) 0);

        CStringPt dir = CStringPt.create(Cross.LEN);

        work.set(0, (char) 0);
        CStringPt.copy(path, dir);

        CStringPt pos = path.lastPositionOf(Cross.FILESPLIT);

        if (!pos.isEmpty())
            dir.set(CStringPt.diff(pos, path) + 1, (char) 0);
        CFileInfo dirInfo = findDirInfo(dir, work);

        if (!pos.isEmpty()) {
            // Last Entry = File
            CStringPt.copy(CStringPt.clone(pos, 1), dir);
            getLongName(dirInfo, dir);
            work.concat(dir);
        }

        if (work.get() != 0) {
            int len = work.length();

            if ((work.get(len - 1) == Cross.FILESPLIT) && (len >= 2)
                    && (work.get(len - 2) != ':')) {

                work.set(len - 1, (char) 0); // Remove trailing slashes except when in root
            }
        }
        return work;
    }

    public boolean getShortName(CStringPt fullName, CStringPt shortName) {
        // Get Dir Info
        CStringPt expand = CStringPt.create(Cross.LEN);
        expand.set(0, (char) 0);
        CFileInfo curDir = findDirInfo(fullName, expand);

        int fileListSize = curDir.longNameList.size();
        if (fileListSize <= 0)
            return false;

        int low = 0;
        int high = (fileListSize - 1);
        int mid, res = 0;

        while (low <= high) {
            mid = (low + high) / 2;
            res = fullName.toString().compareTo(curDir.longNameList.get(mid).orgname.toString());
            if (res > 0)
                low = mid + 1;
            else if (res < 0)
                high = mid - 1;
            else {
                CStringPt.copy(curDir.longNameList.get(mid).shortname, shortName);
                return true;
            }
        }
        return false;
    }

    // return dirId
    // if not find, return -1
    public int findFirst(CStringPt path) {
        int dirID = 0;
        // Cache directory in
        if (!tryOpenDir(path))
            return -1;
        dirID = returnedOpenDirId;
        // Find a free slot.
        // If the next one isn't free, move on to the next, if none is free => reset and assume the
        // worst
        int localFindCounter = 0;
        while (localFindCounter < DOSSystem.MAX_OPENDIRS) {
            if (dirFindFirst[this.nextFreeFindFirst] == null)
                break;
            if (++this.nextFreeFindFirst >= DOSSystem.MAX_OPENDIRS)
                this.nextFreeFindFirst = 0; // Wrap around
            localFindCounter++;
        }

        int dirFindFirstID = this.nextFreeFindFirst++;
        if (this.nextFreeFindFirst >= DOSSystem.MAX_OPENDIRS)
            this.nextFreeFindFirst = 0; // Increase and wrap around for the next search.

        // Here is the reset from above. no free slot found...
        if (localFindCounter == DOSSystem.MAX_OPENDIRS) {
            Log.logging(Log.LogTypes.MISC, Log.LogServerities.Error,
                    "DIRCACHE: FindFirst/Next: All slots full. Resetting");
            // Clear the internal list then.
            dirFindFirstID = 0;
            this.nextFreeFindFirst = 1; // the next free one after this search
            for (int n = 0; n < DOSSystem.MAX_OPENDIRS; n++) {
                // Clear and reuse slot
                dirFindFirst[n].dispose();
                dirFindFirst[n] = null;
            }

        }
        dirFindFirst[dirFindFirstID] = new CFileInfo();
        dirFindFirst[dirFindFirstID].nextEntry = 0;

        // Copy entries to use with FindNext
        List<CFileInfo> cfLst = dirSearch[dirID].fileList;
        for (CFileInfo cf : cfLst) {
            copyEntry(dirFindFirst[dirFindFirstID], cf);
        }
        // Now re-sort the fileList accordingly to output
        switch (sortDirType) {
            case ALPHABETICAL:
                break;
            // case ALPHABETICAL : std::sort(dirFindFirst[dirFindFirstID].fileList.begin(),
            // dirFindFirst[dirFindFirstID].fileList.end(), SortByName); break;
            case DIRALPHABETICAL:
                dirFindFirst[dirFindFirstID].fileList.sort(this::sortByDirName);
                break;
            case ALPHABETICALREV:
                dirFindFirst[dirFindFirstID].fileList.sort(this::sortByNameRev);
                break;
            case DIRALPHABETICALREV:
                dirFindFirst[dirFindFirstID].fileList.sort(this::sortByDirNameRev);
                break;
            case NOSORT:
                break;
        }

        // LOG(LOG_MISC,LOG_ERROR)("DIRCACHE: FindFirst : %s (ID:%02X)",path,dirFindFirstID);
        return dirFindFirstID;
    }

    public boolean findNext(int id, CStringPt result) {
        // out of range ?
        if ((id >= DOSSystem.MAX_OPENDIRS) || dirFindFirst[id] == null) {
            Log.logging(Log.LogTypes.MISC, Log.LogServerities.Error,
                    "DIRCACHE: FindFirst/Next failure : ID out of range: %04X", id);
            return false;
        }
        if (!setResult(dirFindFirst[id], result, dirFindFirst[id].nextEntry)) {
            // free slot
            dirFindFirst[id].dispose();
            dirFindFirst[id] = null;
            return false;
        }
        return true;
    }


    public void cacheOut(CStringPt path, boolean ignoreLastDir) {
        CStringPt expand = CStringPt.create(Cross.LEN);
        expand.set(0, (char) 0);
        CFileInfo dir;

        if (ignoreLastDir) {
            CStringPt tmp = CStringPt.create(Cross.LEN);
            tmp.set(0, (char) 0);
            int len = 0;
            CStringPt pos = path.lastPositionOf(Cross.FILESPLIT);
            if (!pos.isEmpty())
                len = CStringPt.diff(pos, path);
            if (len > 0) {
                CStringPt.safeCopy(path, tmp, len + 1);
            } else {
                CStringPt.copy(path, tmp);
            }
            dir = findDirInfo(tmp, expand);
        } else {
            dir = findDirInfo(path, expand);
        }

        // LOG_DEBUG("DIR: Caching out %s : dir %s",expand,dir.orgname);
        // delete file objects...
        for (int i = 0; i < dir.fileList.size(); i++) {
            if (dirSearch[srchNr] == dir.fileList.get(i))
                dirSearch[srchNr] = null;
            dir.fileList.get(i).dispose();
            dir.fileList.set(i, null);
        }
        // clear lists
        dir.fileList.clear();
        dir.longNameList.clear();
        save_dir = null;
    }

    public void cacheOut(CStringPt path) {
        cacheOut(path, false);
    }

    public void addEntry(CStringPt path, boolean checkExists) {
        // Get Last part...
        CStringPt file = CStringPt.create(Cross.LEN);
        CStringPt expand = CStringPt.create(Cross.LEN);

        CFileInfo dir = findDirInfo(path, expand);
        CStringPt pos = path.lastPositionOf(Cross.FILESPLIT);

        if (!pos.isEmpty()) {
            CStringPt.copy(pos, file);
            // Check if file already exists, then don't add new entry...
            if (checkExists) {
                if (getLongName(dir, file) >= 0)
                    return;
            }

            createEntry(dir, file.toString(), false);

            int index = getLongName(dir, file);
            if (index >= 0) {
                int i;
                // Check if there are any open search dir that are affected by this...
                if (dir != null)
                    for (i = 0; i < DOSSystem.MAX_OPENDIRS; i++) {
                        if ((dirSearch[i] == dir) && (index <= dirSearch[i].nextEntry))
                            dirSearch[i].nextEntry++;
                    }
            }
            // LOG_DEBUG("DIR: Added Entry %s",path);
        } else {
            // LOG_DEBUG("DIR: Error: Failed to add %s",path);
        }
    }

    public void addEntry(CStringPt path) {
        addEntry(path, false);
    }

    public void deleteEntry(CStringPt path, boolean ignoreLastDir) {
        cacheOut(path, ignoreLastDir);
        if (dirSearch[srchNr] != null && (dirSearch[srchNr].nextEntry > 0))
            dirSearch[srchNr].nextEntry--;

        if (!ignoreLastDir) {
            // Check if there are any open search dir that are affected by this...
            int i;
            CStringPt expand = CStringPt.create(Cross.LEN);
            CFileInfo dir = findDirInfo(path, expand);
            if (dir != null)
                for (i = 0; i < DOSSystem.MAX_OPENDIRS; i++) {
                    if ((dirSearch[i] == dir) && (dirSearch[i].nextEntry > 0))
                        dirSearch[i].nextEntry--;
                }
        }
    }

    public void deleteEntry(CStringPt path) {
        deleteEntry(path, false);
    }

    public void emptyCache() {
        // Empty Cache and reinit
        clear();
        dirBase = new CFileInfo();
        save_dir = null;
        srchNr = 0;
        for (int i = 0; i < DOSSystem.MAX_OPENDIRS; i++)
            free[i] = true;
        setBaseDir(basePath);
    }

    public void setLabel(String vname, boolean cdrom, boolean allowupdate) {
        /*
         * allowupdate defaults to true. if mount sets a label then allowupdate is false and will
         * this function return at once after the first call. The label will be set at the first
         * call.
         */

        if (!this.updatelabel)
            return;
        this.updatelabel = allowupdate;
        Drives.setLabel(vname, label, cdrom);
        Log.logging(Log.LogTypes.DOSMISC, Log.LogServerities.Normal,
                "DIRCACHE: Set volume label to %s", label.toString());
    }

    public CStringPt getLabel() {
        return label;
    }

    class CFileInfo implements Disposable {
        public CFileInfo() {
            orgname.set(0, (char) 0);
            shortname.set(0, (char) 0);
            nextEntry = shortNr = 0;
            isDir = false;
        }

        public void dispose() {
            dispose(true);
        }

        protected void dispose(boolean disposing) {
            if (disposing) {
            }
            int fsize = fileList.size();
            for (int i = 0; i < fsize; i++)
                fileList.get(i).dispose();
            fileList.clear();
            longNameList.clear();
        }

        public CStringPt orgname = CStringPt.create(Cross.LEN);
        public CStringPt shortname = CStringPt.create(DOSSystem.DOS_NAMELENGTH_ASCII);
        public boolean isDir;
        public int nextEntry;
        public int shortNr;
        // contents
        public List<CFileInfo> fileList = new ArrayList<CFileInfo>();
        public List<CFileInfo> longNameList = new ArrayList<CFileInfo>();
    }

    private boolean removeTrailingDot(CStringPt shortName) {
        // remove trailing '.' if no extension is available (Linux compatibility)
        int len = shortName.length();
        if (len > 0 && (shortName.get(len - 1) == '.')) {
            if (len == 1)
                return false;
            if ((len == 2) && (shortName.get(0) == '.'))
                return false;
            shortName.set(len - 1, (char) 0);
            return true;
        }
        return false;
    }

    private int getLongName(CFileInfo curDir, CStringPt shortName) {
        int filelist_size = curDir.fileList.size();
        if (filelist_size <= 0)
            return -1;

        // Remove dot, if no extension...
        removeTrailingDot(shortName);
        // Search long name and return array number of element
        int low = 0;
        int high = filelist_size - 1;
        int mid, res;
        while (low <= high) {
            mid = (low + high) / 2;
            res = shortName.toString().compareTo(curDir.fileList.get(mid).shortname.toString());
            if (res > 0)
                low = mid + 1;
            else if (res < 0)
                high = mid - 1;
            else { // Found
                CStringPt.copy(curDir.fileList.get(mid).orgname, shortName);
                return mid;
            }
        }
        // not available
        return -1;
    }

    private void createShortName(CFileInfo curDir, CFileInfo info) {
        int len = 0;
        boolean createShort = false;

        CStringPt tmpNameBuffer = CStringPt.create(Cross.LEN);

        CStringPt tmpName = tmpNameBuffer;

        // Remove Spaces
        CStringPt.copy(info.orgname, tmpName);
        tmpName.upper();
        createShort = removeSpaces(tmpName);

        // Get Length of filename
        CStringPt pos = tmpName.positionOf('.');
        if (!pos.isEmpty()) {
            // ignore preceding '.' if extension is longer than "3"
            if (pos.length() > 4) {
                while (tmpName.get() == '.')
                    tmpName.movePtToR1();
                createShort = true;
            }
            pos = tmpName.positionOf('.');
            if (!pos.isEmpty())
                len = CStringPt.diff(pos, tmpName);
            else
                len = tmpName.length();
        } else {
            len = tmpName.length();
        }

        // Should shortname version be created ?
        createShort = createShort || (len > 8);
        if (!createShort) {
            CStringPt buffer = CStringPt.create(Cross.LEN);
            CStringPt.copy(tmpName, buffer);
            createShort = (getLongName(curDir, buffer) >= 0);
        }

        if (createShort) {
            // Create number
            CStringPt buffer = CStringPt.create(8);
            info.shortNr = createShortNameID(curDir, tmpName);

            // sprintf(buffer,"%d",info.shortNr);
            CStringPt.copy(String.valueOf(info.shortNr), buffer);

            // Copy first letters
            int tocopy = 0;
            int buflen = buffer.length();
            if (len + buflen + 1 > 8)
                tocopy = 8 - buflen - 1;
            else
                tocopy = len;
            CStringPt.safeCopy(tmpName, info.shortname, tocopy + 1);
            // Copy number
            info.shortname.concat("~");
            info.shortname.concat(buffer);
            // Add (and cut) Extension, if available
            if (!pos.isEmpty()) {
                // Step to last extension...
                pos = tmpName.lastPositionOf('.');
                // add extension
                info.shortname.concat(pos.toString().substring(0, 4));
                info.shortname.set(DOSSystem.DOS_NAMELENGTH, (char) 0);
            }

            // keep list sorted for CreateShortNameID to work correctly
            if (curDir.longNameList.size() > 0) {

                if (!(info.shortname.toString()
                        .compareTo(curDir.longNameList.get(curDir.longNameList.size() - 1).shortname
                                .toString()) < 0)) {
                    // append at end of list
                    curDir.longNameList.add(info);
                } else {
                    // look for position where to insert this element
                    boolean found = false;
                    CFileInfo cfItem = null;
                    for (CFileInfo item : curDir.longNameList) {
                        cfItem = item;

                        if (info.shortname.toString().compareTo(item.shortname.toString()) < 0) {
                            found = true;
                            break;
                        }
                    }
                    // Put it in longname list...
                    if (found)
                        curDir.longNameList.add(curDir.longNameList.indexOf(cfItem), info);
                    else
                        curDir.longNameList.add(info);
                }
            } else {
                // empty file list, append
                curDir.longNameList.add(info);
            }
        } else {
            CStringPt.copy(tmpName, info.shortname);
        }
        removeTrailingDot(info.shortname);
    }

    private int createShortNameID(CFileInfo curDir, CStringPt name) {

        int fileListSize = curDir.longNameList.size();
        if (fileListSize <= 0)
            return 1; // shortener IDs start with 1

        int foundNr = 0;
        int low = 0;
        int high = fileListSize - 1;
        int mid, res;

        while (low <= high) {
            mid = (low + high) / 2;
            res = compareShortname(name, curDir.longNameList.get(mid).shortname);

            if (res > 0)
                low = mid + 1;
            else if (res < 0)
                high = mid - 1;
            else {
                // any more same x chars in next entries ?
                do {
                    foundNr = curDir.longNameList.get(mid).shortNr;
                    mid++;
                } while (mid < curDir.longNameList.size()
                        && (compareShortname(name, curDir.longNameList.get(mid).shortname) == 0));
                break;
            }
        }
        return foundNr + 1;
    }


    private int compareShortname(CStringPt compareName, CStringPt shortName) {
        CStringPt cpos = shortName.positionOf('~');

        if (!cpos.isEmpty()) {
            /* the following code is replaced as it's not safe when char* is 64 bits */
            /*
             * int compareCount1 = (int)cpos - (int)shortName; char* endPos = strchr(cpos,'.'); int
             * numberSize = endPos ? int(endPos)-int(cpos) : strlen(cpos);
             * 
             * char* lpos = strchr(compareName,'.'); int compareCount2 = lpos ?
             * int(lpos)-int(compareName) : strlen(compareName); if (compareCount2>8) compareCount2
             * = 8;
             * 
             * compareCount2 -= numberSize; if (compareCount2>compareCount1) compareCount1 =
             * compareCount2;
             */
            int compareCount1 = shortName.indexOf('~');
            int numberSize = cpos.indexOf('.');
            int compareCount2 = compareName.indexOf('.');
            if (compareCount2 > 8)
                compareCount2 = 8;
            /*
             * We want compareCount2 -= numberSize; if (compareCount2>compareCount1) compareCount1 =
             * compareCount2; but to prevent negative numbers:
             */
            if (compareCount2 > compareCount1 + numberSize)
                compareCount1 = compareCount2 - numberSize;
            return compareName.toString().substring(0, compareCount1)
                    .compareTo(shortName.toString().substring(0, compareCount1));
        }
        return compareName.toString().compareTo(shortName.toString());
    }

    // SetResult(CFileInfo dir, ref CStringPt result, UInt32 entryNr)
    private boolean setResult(CFileInfo dir, CStringPt result, int entryNr) {
        CStringPt res = CStringPt.create(Cross.LEN);
        res.set(0, (char) 0);

        CStringPt.copyPt(res, result);
        if (entryNr >= dir.fileList.size())
            return false;
        CFileInfo info = dir.fileList.get(entryNr);
        // copy filename, short version
        CStringPt.copy(info.shortname, res);
        // Set to next Entry
        dir.nextEntry = entryNr + 1;
        return true;
    }

    private boolean isCachedIn(CFileInfo curDir) {
        return (curDir.fileList.size() > 0);
    }

    private CFileInfo findDirInfo(CStringPt path, CStringPt expandedPath) {
        // statics
        CStringPt split = CStringPt.create(String.valueOf(Cross.FILESPLIT));

        CStringPt dir = CStringPt.create(Cross.LEN);
        CStringPt work = CStringPt.create(Cross.LEN);
        CStringPt start = CStringPt.clone(path);// path
        CStringPt pos;
        CFileInfo curDir = dirBase;
        int id = 0;

        if (save_dir != null && path.equals(save_path)) {
            CStringPt.copy(save_expanded, expandedPath);
            return save_dir;
        }

        // LOG_DEBUG("DIR: Find %s",path);

        // Remove base dir path
        // start += basePath.length();
        start.moveR(basePath.length());
        CStringPt.copy(basePath, expandedPath);

        // hehe, baseDir should be cached in...
        if (!isCachedIn(curDir)) {
            CStringPt.copy(basePath, work);
            if (openDir(curDir, work)) {
                id = returnedOpenDirId;
                CStringPt buffer = CStringPt.create(Cross.LEN);
                CStringPt result = CStringPt.create();
                CStringPt.copy(dirPath, buffer);
                readDir(id, result);
                CStringPt.copy(buffer, dirPath);
                free[id] = true;
            }
        }

        do {
            // boolean errorcheck = false;
            pos = start.positionOf(Cross.FILESPLIT);
            if (!pos.isEmpty()) {
                CStringPt.safeCopy(start, dir, CStringPt.diff(pos, start) + 1);
                /* errorcheck = true; */ } else {
                CStringPt.copy(start, dir);
            }

            // Path found
            int nextDir = getLongName(curDir, dir);
            expandedPath.concat(dir);

            // Error check
            /*
             * if ((errorcheck) && (nextDir<0)) {
             * LOG_DEBUG("DIR: Error: %s not found.",expandedPath); };
             */
            // Follow Directory
            if ((nextDir >= 0) && curDir.fileList.get(nextDir).isDir) {
                curDir = curDir.fileList.get(nextDir);
                CStringPt.copy(dir, curDir.orgname);
                if (!isCachedIn(curDir)) {

                    if (openDir(curDir, expandedPath)) {
                        id = returnedOpenDirId;
                        CStringPt buffer = CStringPt.create(Cross.LEN);
                        CStringPt result = CStringPt.create();
                        CStringPt.copy(dirPath, buffer);
                        readDir(id, result);
                        CStringPt.copy(buffer, dirPath);
                        free[id] = true;
                    }
                }
            }
            if (!pos.isEmpty()) {
                expandedPath.concat(split);
                start = CStringPt.clone(pos, 1);
            }
        } while (!pos.isEmpty());

        // Save last result for faster access next time
        CStringPt.copy(path, save_path);
        CStringPt.copy(expandedPath, save_expanded);
        save_dir = curDir;

        return curDir;
    }

    private boolean removeSpaces(CStringPt str) {
        // Removes all spaces
        CStringPt curpos = CStringPt.clone(str);
        CStringPt chkpos = CStringPt.clone(str);
        while (chkpos.get() != 0) {
            if (chkpos.get() == ' ')
                chkpos.movePtToR1();
            else
                curpos.set(chkpos.get());
            curpos.movePtToR1();
            chkpos.movePtToR1();
        }
        curpos.set((char) 0);
        return (curpos != chkpos);
    }

    private int returnedOpenDirId = 0;// uint16

    private boolean openDir(CFileInfo dir, CStringPt expand) {
        int id = returnedOpenDirId = getFreeID(dir);
        dirSearch[id] = dir;
        CStringPt expandcopy = CStringPt.create(Cross.LEN);
        CStringPt.copy(expand, expandcopy);
        // Add "/"
        char[] end = {Cross.FILESPLIT, (char) 0};
        if (expandcopy.get(expandcopy.length() - 1) != Cross.FILESPLIT)
            expandcopy.concat(end);
        // open dir
        if (dirSearch[id] != null) {
            // open dir
            Path path = Paths.get(expandcopy.toString());
            try {
                if (Files.exists(path)) {
                    CStringPt.copy(expandcopy, dirPath);
                    free[id] = false;
                    return true;
                }
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    private void createEntry(CFileInfo dir, String name, boolean isDirectory) {
        CFileInfo info = new CFileInfo();
        CStringPt.copy(name, info.orgname);
        info.shortNr = 0;
        info.isDir = isDirectory;

        // Check for long filenames...
        createShortName(dir, info);

        boolean found = false;
        List<CFileInfo> dirFiles = dir.fileList;
        // keep list sorted (so GetLongName works correctly, used by CreateShortName in this
        // routine)
        if (dirFiles.size() > 0) {
            CFileInfo lastFile = dirFiles.get(dirFiles.size() - 1);
            if (!(info.shortname.toString().compareTo(lastFile.shortname.toString()) < 0)) {
                // append at end of list
                dirFiles.add(info);

            } else {
                // look for position where to insert this element
                CFileInfo cf = null;
                for (CFileInfo item : dirFiles) {
                    cf = item;
                    if (info.shortname.toString().compareTo(cf.shortname.toString()) < 0) {
                        found = true;
                        break;
                    }
                }
                // Put file in lists
                if (found)
                    dirFiles.add(dirFiles.indexOf(cf), info);
                else
                    dirFiles.add(info);
            }
        } else {
            // empty file list, append
            dirFiles.add(info);

        }
    }

    private void copyEntry(CFileInfo dir, CFileInfo from) {
        CFileInfo info = new CFileInfo();
        // just copy things into new fileinfo
        CStringPt.copy(from.orgname, info.orgname);
        CStringPt.copy(from.shortname, info.shortname);
        info.shortNr = from.shortNr;
        info.isDir = from.isDir;

        dir.fileList.add(info);
    }

    private short getFreeID(CFileInfo dir) {
        for (short i = 0; i < DOSSystem.MAX_OPENDIRS; i++)
            if (free[i] || (dir == dirSearch[i]))
                return i;
        Log.logging(Log.LogTypes.FILES, Log.LogServerities.Normal,
                "DIRCACHE: Too many open directories!");
        return 0;
    }

    private void clear() {
        dirBase.dispose();
        dirBase = null;
        nextFreeFindFirst = 0;
        for (int i = 0; i < DOSSystem.MAX_OPENDIRS; i++)
            dirSearch[i] = null;
    }

    private CFileInfo dirBase;
    private CStringPt dirPath = CStringPt.create(Cross.LEN);
    private CStringPt basePath = CStringPt.create(Cross.LEN);
    private boolean dirFirstTime;
    private TDirSort sortDirType;
    private CFileInfo save_dir;
    private CStringPt save_path = CStringPt.create(Cross.LEN);
    private CStringPt save_expanded = CStringPt.create(Cross.LEN);

    private short srchNr;
    private CFileInfo[] dirSearch = new CFileInfo[DOSSystem.MAX_OPENDIRS];
    private byte[] dirSearchName = new byte[DOSSystem.MAX_OPENDIRS];
    private boolean[] free = new boolean[DOSSystem.MAX_OPENDIRS];
    private CFileInfo[] dirFindFirst = new CFileInfo[DOSSystem.MAX_OPENDIRS];
    private short nextFreeFindFirst;

    private CStringPt label = CStringPt.create(Cross.LEN);
    private boolean updatelabel;

    // private static boolean SortByName(CFileInfo a, CFileInfo b)
    // {
    // return string.Compare(a.shortname.toString(), b.shortname.toString()) < 0;
    // }


    // private int SortByName(CFileInfo x, CFileInfo y)
    // {
    // return string.Compare(x.shortname.toString(), y.shortname.toString());
    // }

    private int sortByNameRev(CFileInfo x, CFileInfo y) {
        int ret = x.shortname.toString().compareTo(y.shortname.toString());
        return ret * -1;
    }


    private int sortByDirName(CFileInfo x, CFileInfo y) {
        // Directories first...
        if (x.isDir != y.isDir)
            return x.isDir ? -1 : 1;
        return x.shortname.toString().compareTo(y.shortname.toString());
    }

    private int sortByDirNameRev(CFileInfo x, CFileInfo y) {
        // Directories first...
        if (x.isDir != y.isDir)
            return x.isDir ? 1 : -1;
        int ret = x.shortname.toString().compareTo(y.shortname.toString());
        if (ret == 0)
            return ret;
        return ret > 0 ? -1 : 1;

    }
}
