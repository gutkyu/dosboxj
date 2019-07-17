package org.gutkyu.dosboxj.hardware.memory.paging;

import org.gutkyu.dosboxj.cpu.*;
import org.gutkyu.dosboxj.hardware.memory.*;
import org.gutkyu.dosboxj.util.*;

class InitPageHandler extends PageHandler {
    public InitPageHandler() {
        Flags = Paging.PFLAG_INIT | Paging.PFLAG_NOCODE;
    }

    @Override
    public int readB(int addr) {
        int needs_reset = initPage(addr, false);
        int val = Memory.readB(addr);
        Paging.initPageUpdateLink(needs_reset, addr);
        return val;
    }

    @Override
    public int readW(int addr) {
        int needs_reset = initPage(addr, false);
        int val = Memory.readW(addr);
        Paging.initPageUpdateLink(needs_reset, addr);
        return val;
    }

    @Override
    public long readD(int addr) {
        int needs_reset = initPage(addr, false);
        int val = Memory.readD(addr);
        Paging.initPageUpdateLink(needs_reset, addr);
        return val;
    }

    @Override
    public void writeB(int addr, int val) {
        int needs_reset = initPage(addr, true);
        Memory.writeB(addr, val);
        Paging.initPageUpdateLink(needs_reset, addr);
    }

    @Override
    public void writeW(int addr, int val) {
        int needs_reset = initPage(addr, true);
        Memory.writeW(addr, val);
        Paging.initPageUpdateLink(needs_reset, addr);
    }

    @Override
    public void writeD(int addr, int val) {
        int needs_reset = initPage(addr, true);
        Memory.writeD(addr, val);
        Paging.initPageUpdateLink(needs_reset, addr);
    }

    // checked == true 경우 -1 반환
    // checked == false 경우 해당 값 반환
    @Override
    // uint16(int)
    public int readBChecked(int addr) {
        if (initPageCheckOnly(addr, false)) {
            int val = Memory.readB(addr);
            // return false;
            return val;
        } else {
            return -1;
        }
    }

    // checked == true 경우 -1 반환
    // checked == false 경우 해당 값 반환
    @Override
    public int readWChecked(int addr) {
        if (initPageCheckOnly(addr, false)) {
            return Memory.readW(addr);
            // return false;
        } else
            return -1;
        // else return true;
    }

    // checked == true 경우 -1 반환
    // checked == long 경우 해당 값 반환
    @Override
    public long readDChecked(int addr) {
        if (initPageCheckOnly(addr, false)) {
            return Memory.readD(addr);
            // return false;
        } else
            return -1;
        // else return true;
    }

    @Override
    public boolean writeBChecked(int addr, int val) {
        if (initPageCheckOnly(addr, true)) {
            Memory.writeB(addr, val);
            return false;
        } else
            return true;
    }

    @Override
    public boolean writeWChecked(int addr, int val) {
        if (initPageCheckOnly(addr, true)) {
            Memory.writeW(addr, val);
            return false;
        } else
            return true;
    }

    @Override
    public boolean writeDChecked(int addr, int val) {
        if (initPageCheckOnly(addr, true)) {
            Memory.writeD(addr, val);
            return false;
        } else
            return true;
    }

    public int initPage(int lin_addr, boolean writing) {
        int lin_page = lin_addr >>> 12;
        int phys_page;
        if (Paging.paging.Enabled) {
            X86PageEntry table = new X86PageEntry();
            X86PageEntry entry = new X86PageEntry();
            Paging.initPageCheckPresence(lin_addr, writing, table, entry);

            // 0: no action
            // 1: can (but currently does not) fail a user-level access privilege check
            // 2: can (but currently does not) fail a write privilege check
            // 3: fails a privilege check
            int priv_check = 0;
            if (Paging.initPageCheckUseraccess(entry.us, table.us)) {
                if ((CPU.Block.CPL & CPU.Block.MPL) == 3)
                    priv_check = 3;
                else {
                    switch (CPU.ArchitectureType) {
                        case CPU.ArchTypeMixed:
                        case CPU.ArchType386Fast:
                        default:
                            // priv_check=0; // default
                            break;
                        case CPU.ArchType386Slow:
                        case CPU.ArchType486OldSlow:
                        case CPU.ArchType486NewSlow:
                        case CPU.ArchTypePentiumSlow:
                            priv_check = 1;
                            break;
                    }
                }
            }
            if ((entry.wr == 0) || (table.wr == 0)) {
                // page is write-protected for user mode
                if (priv_check == 0) {
                    switch (CPU.ArchitectureType) {
                        case CPU.ArchTypeMixed:
                        case CPU.ArchType386Fast:
                        default:
                            // priv_check=0; // default
                            break;
                        case CPU.ArchType386Slow:
                        case CPU.ArchType486OldSlow:
                        case CPU.ArchType486NewSlow:
                        case CPU.ArchTypePentiumSlow:
                            priv_check = 2;
                            break;
                    }
                }
                // check if actually failing the write-protected check
                if (writing && (CPU.Block.CPL & CPU.Block.MPL) == 3)
                    priv_check = 3;
            }
            if (priv_check == 3) {
                Log.logging(Log.LogTypes.PAGING, Log.LogServerities.Normal,
                        "Page access denied: cpl=%i, %x:%x:%x:%x", CPU.Block.CPL, entry.us,
                        table.us, entry.wr, table.wr);
                Paging.pageFault(lin_addr, (table.Base << 12) + (lin_page & 0x3ff) * 4,
                        0x05 | (writing ? 0x02 : 0x00));
                priv_check = 0;
            }

            if (table.a == 0) {
                table.a = 1; // set page table accessed
                Memory.physWriteD((int) ((Paging.paging.Base.Page << 12) + (lin_page >>> 10) * 4),
                        table.getLoad());
            }
            if ((entry.a == 0) || (entry.d == 0)) {
                entry.a = 1; // set page accessed

                // page is dirty if we're writing to it, or if we're reading but the
                // page will be fully linked so we can't track later writes
                if (writing || (priv_check == 0))
                    entry.d = 1; // mark page as dirty

                Memory.physWriteD((table.Base << 12) + (lin_page & 0x3ff) * 4, entry.getLoad());
            }

            phys_page = entry.Base;

            // now see how the page should be linked best, if we need to catch privilege
            // checks later on it should be linked as read-only page
            if (priv_check == 0) {
                // if reading we could link the page as read-only to later cacth writes,
                // will slow down pretty much but allows catching all dirty events
                Paging.linkPage(lin_page, phys_page);
            } else {
                if (priv_check == 1) {
                    Paging.linkPage(lin_page, phys_page);
                    return 1;
                } else if (writing) {
                    PageHandler handler = Memory.getPageHandler(phys_page);
                    Paging.linkPage(lin_page, phys_page);
                    if ((handler.Flags & Paging.PFLAG_READABLE) == 0)
                        return 1;
                    if ((handler.Flags & Paging.PFLAG_WRITEABLE) == 0)
                        return 1;
                    if (Paging.getTLBEntry(lin_addr).Read != Paging.getTLBEntry(lin_addr).Write)
                        return 1;
                    if (phys_page > 1)
                        return phys_page;
                    else
                        return 1;
                } else {
                    Paging.linkPageReadOnly(lin_page, phys_page);
                }
            }
        } else {
            if (lin_page < Paging.LINK_START)
                phys_page = Paging.paging.FirstMb[lin_page];
            else
                phys_page = lin_page;
            Paging.linkPage(lin_page, phys_page);
        }
        return 0;
    }

    public boolean initPageCheckOnly(int lin_addr, boolean writing) {
        int lin_page = lin_addr >>> 12;
        if (Paging.paging.Enabled) {
            X86PageEntry table = new X86PageEntry();
            X86PageEntry entry = new X86PageEntry();
            if (!Paging.initPageCheckPresenceCheckOnly(lin_addr, writing, table, entry))
                return false;

            if ((CPU.Block.CPL & CPU.Block.MPL) != 3)
                return true;

            if (Paging.initPageCheckUseraccess(entry.us, table.us)
                    || (((entry.wr == 0) || (table.wr == 0)) && writing)) {
                Log.logging(Log.LogTypes.PAGING, Log.LogServerities.Normal,
                        "Page access denied: cpl=%i, %x:%x:%x:%x", CPU.Block.CPL, entry.us,
                        table.us, entry.wr, table.wr);
                Paging.paging.CR2 = lin_addr;
                CPU.Block.Exception.Which = CPU.ExceptionPF;
                CPU.Block.Exception.Error = 0x05 | (writing ? 0x02 : 0x00);
                return false;
            }
        } else {
            int phys_page;
            if (lin_page < Paging.LINK_START)
                phys_page = Paging.paging.FirstMb[lin_page];
            else
                phys_page = lin_page;
            Paging.linkPage(lin_page, phys_page);
        }
        return true;
    }

    public void initPageForced(int lin_addr) {
        int lin_page = lin_addr >>> 12;
        int phys_page;
        if (Paging.paging.Enabled) {
            X86PageEntry table = new X86PageEntry();
            X86PageEntry entry = new X86PageEntry();
            Paging.initPageCheckPresence(lin_addr, false, table, entry);

            if (table.a == 0) {
                table.a = 1; // Set access
                Memory.physWriteD((int) ((Paging.paging.Base.Page << 12) + (lin_page >>> 10) * 4),
                        table.getLoad());
            }
            if (entry.a == 0) {
                entry.a = 1; // Set access
                Memory.physWriteD((table.Base << 12) + (lin_page & 0x3ff) * 4, entry.getLoad());
            }
            phys_page = entry.Base;
            // maybe use read-only page here if possible
        } else {
            if (lin_page < Paging.LINK_START)
                phys_page = Paging.paging.FirstMb[lin_page];
            else
                phys_page = lin_page;
        }
        Paging.linkPage(lin_page, phys_page);
    }
}
