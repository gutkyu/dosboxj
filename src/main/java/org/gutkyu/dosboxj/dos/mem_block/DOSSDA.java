package org.gutkyu.dosboxj.dos.mem_block;


import org.gutkyu.dosboxj.hardware.memory.*;


public final class DOSSDA extends MemStruct {
    public DOSSDA(int seg, int offs) {
        setSegPt(seg, offs);
    }

    public void init() {
        /* Clear */
        for (int i = 0; i < sSDA_Size; i++)
            Memory.writeB(pt + i, 0x00);
        saveIt(Size_sSDA_drive_crit_error, Off_sSDA_drive_crit_error, 0xff);
    }

    public void setDrive(int drive) {
        saveIt(Size_sSDA_current_drive, Off_sSDA_current_drive, drive);
    }

    public void setDTA(int dta) {
        saveIt(Size_sSDA_current_dta, Off_sSDA_current_dta, dta);
    }

    public void setPSP(int psp) {
        saveIt(Size_sSDA_current_psp, Off_sSDA_current_psp, psp);
    }

    public int getDrive() {
        return 0xff & getIt(Size_sSDA_current_drive, Off_sSDA_current_drive);
    }

    public int getPSP() {
        return getIt(Size_sSDA_current_psp, Off_sSDA_current_psp);
    }

    public int getDTA() {
        return getIt(Size_sSDA_current_dta, Off_sSDA_current_dta);
    }

    // ------------------------------------ struct sSDA start ----------------------------------//

    private static final int Size_sSDA_crit_error_flag = 1;
    private static final int Size_sSDA_inDOS_flag = 1;
    private static final int Size_sSDA_drive_crit_error = 1;
    private static final int Size_sSDA_locus_of_last_error = 1;
    private static final int Size_sSDA_extended_error_code = 2;
    private static final int Size_sSDA_suggested_action = 1;
    private static final int Size_sSDA_error_class = 1;
    private static final int Size_sSDA_last_error_pointer = 4;
    private static final int Size_sSDA_current_dta = 4;
    private static final int Size_sSDA_current_psp = 2;
    private static final int Size_sSDA_sp_int_23 = 2;
    private static final int Size_sSDA_return_code = 2;
    private static final int Size_sSDA_current_drive = 1;
    private static final int Size_sSDA_extended_break_flag = 1;
    private static final int Size_sSDA_fill = 2;

    private static final int Off_sSDA_crit_error_flag = 0; // 0x00 Critical Error Flag
    private static final int Off_sSDA_inDOS_flag = 1; // 0x01 InDOS flag (count of active INT 21
                                                      // calls)
    private static final int Off_sSDA_drive_crit_error = 2; // 0x02 Drive on which current critical
                                                            // error occurred or FFh
    private static final int Off_sSDA_locus_of_last_error = 3; // 0x03 locus of last error
    private static final int Off_sSDA_extended_error_code = 4; // 0x04 extended error code of last
                                                               // error
    private static final int Off_sSDA_suggested_action = 6; // 0x06 suggested action for last error
    private static final int Off_sSDA_error_class = 7;// 0x07 class of last error
    private static final int Off_sSDA_last_error_pointer = 8; // 0x08 ES:DI pointer for last error
    private static final int Off_sSDA_current_dta = 12; // 0x0C current DTA (Disk Transfer Address)
    private static final int Off_sSDA_current_psp = 16; // 0x10 current PSP
    private static final int Off_sSDA_sp_int_23 = 18; // 0x12 stores SP across an INT 23
    // 0x14 return code from last process termination (zerod after reading with AH=4Dh)
    private static final int Off_sSDA_return_code = 20;
    private static final int Off_sSDA_current_drive = 22; // 0x16 current drive
    private static final int Off_sSDA_extended_break_flag = 23;// 0x17 extended break flag
    // 0x18 flag: code page switching || flag: copy of previous byte in case of INT 24 Abort
    private static final int Off_sSDA_fill = 24;

    private static final int sSDA_Size = 26;

    // ------------------------------------ struct sSDA end ----------------------------------//

}
