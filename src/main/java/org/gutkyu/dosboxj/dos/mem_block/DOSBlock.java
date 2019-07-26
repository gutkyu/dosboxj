package org.gutkyu.dosboxj.dos.mem_block;


import org.gutkyu.dosboxj.dos.*;



public final class DOSBlock {
    public static class DOSVersion {
        public byte major, minor, revision;
    }

    public static class DOSDate {
        public int Year;// uint16
        public int Month;// uint8
        public int Day;// uint8
    }

    public DOSBlock() {
    }

    public DOSDate Date = new DOSDate();
    public DOSVersion Version = new DOSVersion();
    // uint16
    public int FirstMCB;
    // uint16
    public int ErrorCode;

    public int getPSP() {
        return (new DOSSDA(DOSMain.DOS_SDA_SEG, DOSMain.DOS_SDA_OFS)).getPSP();
    }

    public void setPSP(int seg) {
        (new DOSSDA(DOSMain.DOS_SDA_SEG, DOSMain.DOS_SDA_OFS)).setPSP(seg);
    }

    public short Env;
    public int cpmentry;

    public int getDTA() {
        return (new DOSSDA(DOSMain.DOS_SDA_SEG, DOSMain.DOS_SDA_OFS)).getDTA();
    }

    public void setDTA(int dta) {
        (new DOSSDA(DOSMain.DOS_SDA_SEG, DOSMain.DOS_SDA_OFS)).setDTA(dta);
    }

    public int ReturnCode, ReturnMode;

    public int CurrentDrive;
    public boolean Verify;
    public boolean BreakCheck;
    public boolean Echo; // if set to true dev_con::read will echo input
    public Tables tables = new Tables();
    public int LoadedCodepage;// uint16

    public static class Tables {
        public int MediaId;
        public int TempDTA;
        public int TempDTA_FCBDelete;
        public int DBCS;
        public int FileNameChar;
        public int CollatingSeq;
        public int Upcase;
        public byte[] Country;// Will be copied to dos memory. resides in real mem
        public int DPB; // Fake Disk parameter system using only the first entry so the drive letter
                        // matches
    }

}
