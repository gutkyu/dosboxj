package org.gutkyu.dosboxj.dos.system.drive;



import org.gutkyu.dosboxj.dos.mem_block.*;
import org.gutkyu.dosboxj.dos.system.file.*;
import org.gutkyu.dosboxj.util.*;

public abstract class DOSDrive {
    public DOSDrive() {
        curdir = "";
        info = "";
    }

    // file이 없으면 null 반환
    public abstract DOSFile fileOpen(String name, int flags);

    // 실패하면 null 반환
    // object ( string, uint16 )
    public abstract DOSFile fileCreate(String name, int attributes);

    public abstract boolean fileUnlink(String name);

    public abstract boolean removeDir(String dir);

    public abstract boolean makeDir(String dir);

    public abstract boolean testDir(CStringPt dir);

    public abstract boolean testDir(String dir);

    public abstract boolean findFirst(CStringPt dir, DOSDTA dta, boolean fcbFindFirst);

    // fcbFindFirst = false
    public abstract boolean findFirst(CStringPt dir, DOSDTA dta);

    public abstract boolean findNext(DOSDTA dta);

    public abstract boolean tryFileAttr(String name);

    public abstract int returnFileAttr();

    public abstract boolean rename(String oldName, String newName);

    public abstract boolean allocationInfo(DriveAllocationInfo alloc);

    public abstract boolean fileExists(String name);

    public abstract boolean fileStat(String name, FileStatBlock statBlock);

    // uint8
    public abstract int getMediaByte();

    public void setDir(String path) {
        curdir = path;
    }

    public void emptyCache() {
        dirCache.emptyCache();
    }

    public abstract boolean isRemote();

    public abstract boolean isRemovable();

    public abstract int unMount();

    // 주로 로그 출력에 사용하므로 아스키 값의 배열을 반환할 필요 없음.
    // 유니코드 String 타입으로 반환
    public String getInfo() {
        return info;
    }

    public String curdir = "";
    // 소스에서는 public이나 protected로 변경, GetInfo()로 외부 접근 인터페이스 구현
    protected String info = "";

    /* Can be overridden for example in iso images */
    public CStringPt getLabel() {
        return dirCache.getLabel();
    }

    public DOSDriveCache dirCache = new DOSDriveCache();

    // disk cycling functionality (request resources)
    public void activate() {
    }
}
