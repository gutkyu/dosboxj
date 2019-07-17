package org.gutkyu.dosboxj.dos.mem_block;



public final class DOSParamBlock extends MemStruct {
    public DOSParamBlock(int addr) {
        pt = addr;
    }

    public void clear() {
        Exec.CmdTail = 0;
        Exec.FCB1 = 0;
        Exec.FCB2 = 0;
        Exec.InitCSIP = 0;
        Exec.InitSSSP = 0;
        Overlay.LoadSeg = 0;
        Overlay.Relocation = 0;
    }

    public void loadData() {
        Exec.EnvSeg = (short) getIt(SizeSExecEnvSeg, OffSExecEnvSeg);
        Exec.CmdTail = getIt(SizeSExecCmdTail, OffSExecCmdTail);
        Exec.FCB1 = getIt(SizeSExecFCB1, OffSExecFCB1);
        Exec.FCB2 = getIt(SizeSExecFCB2, OffSExecFCB2);
        Exec.InitSSSP = getIt(SizeSExecInitSSSP, OffSExecInitSSSP);
        Exec.InitCSIP = getIt(SizeSExecInitCSIP, OffSExecInitCSIP);
        Overlay.LoadSeg = (short) getIt(Size_SOverlay_LoadSeg, OffSOverlayLoadseg);
        Overlay.Relocation = (short) getIt(SizeSOverlayRelocation, OffSOverlayRelocation);
    }

    public void saveData() /* Save it as an exec block */
    {
        saveIt(SizeSExecEnvSeg, OffSExecEnvSeg, Exec.EnvSeg);
        saveIt(SizeSExecCmdTail, OffSExecCmdTail, Exec.CmdTail);
        saveIt(SizeSExecFCB1, OffSExecFCB1, Exec.FCB1);
        saveIt(SizeSExecFCB2, OffSExecFCB2, Exec.FCB2);
        saveIt(SizeSExecInitSSSP, OffSExecInitSSSP, Exec.InitSSSP);
        saveIt(SizeSExecInitCSIP, OffSExecInitCSIP, Exec.InitCSIP);
    }

    public static class SOverlay {
        public short LoadSeg;
        public short Relocation;
    }

    public static final int Size_SOverlay_LoadSeg = 2;
    public static final int SizeSOverlayRelocation = 2;
    public static final int OffSOverlayLoadseg = 0;
    public static final int OffSOverlayRelocation = 2;

    public static class SExec {
        public short EnvSeg;
        public int CmdTail;
        public int FCB1;
        public int FCB2;
        public int InitSSSP;
        public int InitCSIP;
    }

    private static final int SizeSExecEnvSeg = 2;
    private static final int SizeSExecCmdTail = 4;
    private static final int SizeSExecFCB1 = 4;
    private static final int SizeSExecFCB2 = 4;
    private static final int SizeSExecInitSSSP = 4;
    private static final int SizeSExecInitCSIP = 4;

    private static final int OffSExecEnvSeg = 0;
    private static final int OffSExecCmdTail = 2;
    private static final int OffSExecFCB1 = 6;
    private static final int OffSExecFCB2 = 10;
    private static final int OffSExecInitSSSP = 14;
    private static final int OffSExecInitCSIP = 18;

    public SExec Exec = new SExec();
    public SOverlay Overlay = new SOverlay();
}
