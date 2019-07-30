package org.gutkyu.dosboxj.hardware.memory.paging;

import org.gutkyu.dosboxj.cpu.*;
import org.gutkyu.dosboxj.hardware.memory.*;
import org.gutkyu.dosboxj.util.*;

class InitPageUserROHandler extends PageHandler {
    public InitPageUserROHandler() {
        Flags = Paging.PFLAG_INIT | Paging.PFLAG_NOCODE;
    }

    @Override
    public void writeB(int addr, int val) {
        initPage(addr, val & 0xff);
        Memory.hostWriteB(Paging.getTLBEntry(addr).Read + addr, val & 0xff);
    }

    @Override
    public void writeW(int addr, int val) {
        initPage(addr, val & 0xffff);
        Memory.hostWriteW(Paging.getTLBEntry(addr).Read + addr, (val & 0xffff));
    }

    @Override
    public void writeD(int addr, int val) {
        initPage(addr, val);
        Memory.hostWriteD(Paging.getTLBEntry(addr).Read + addr, val);
    }

    @Override
    public boolean writeBChecked(int addr, int val) {
        int writecode = initPageCheckOnly(addr, (val & 0xff));
        if (writecode != 0) {
            int tlb_addr;
            if (writecode > 1)
                tlb_addr = Paging.getTLBEntry(addr).Read;
            else
                tlb_addr = Paging.getTLBEntry(addr).Write;
            Memory.hostWriteB(tlb_addr + addr, (val & 0xff));
            return false;
        }
        return true;
    }

    @Override
    public boolean writeWChecked(int addr, int val) {
        int writecode = initPageCheckOnly(addr, val & 0xffff);
        if (writecode != 0) {
            int tlb_addr;
            if (writecode > 1)
                tlb_addr = Paging.getTLBEntry(addr).Read;
            else
                tlb_addr = Paging.getTLBEntry(addr).Write;
            Memory.hostWriteW(tlb_addr + addr, (val & 0xffff));
            return false;
        }
        return true;
    }

    @Override
    public boolean writeDChecked(int addr, int val) {
        int writecode = initPageCheckOnly(addr, val);
        if (writecode != 0) {
            int tlb_addr;
            if (writecode > 1)
                tlb_addr = Paging.getTLBEntry(addr).Read;
            else
                tlb_addr = Paging.getTLBEntry(addr).Write;
            Memory.hostWriteD(tlb_addr + addr, val);
            return false;
        }
        return true;
    }

    public void initPage(int linAddr, int val) {
        int linPage = linAddr >>> 12;
        int physPage;
        if (Paging.paging.Enabled) {
            if ((CPU.Block.CPL & CPU.Block.MPL) != 3)
                return;

            X86PageEntry table = new X86PageEntry();
            X86PageEntry entry = new X86PageEntry();
            Paging.initPageCheckPresence(linAddr, true, table, entry);

            Log.logging(Log.LogTypes.PAGING, Log.LogServerities.Normal,
                    "Page access denied: cpl=%i, %x:%x:%x:%x", CPU.Block.CPL, entry.us, table.us,
                    entry.wr, table.wr);
            Paging.pageFault(linAddr, (table.Base << 12) + (linPage & 0x3ff) * 4, 0x07);

            if (table.a == 0) {
                table.a = 1; // Set access
                Memory.physWriteD((int) ((Paging.paging.Base.Page << 12) + (linPage >>> 10) * 4),
                        table.getLoad());
            }
            if ((entry.a == 0) || (entry.d == 0)) {
                entry.a = 1; // Set access
                entry.d = 1; // Set dirty
                Memory.physWriteD((table.Base << 12) + (linPage & 0x3ff) * 4, entry.getLoad());
            }
            physPage = entry.Base;
            Paging.linkPage(linPage, physPage);
        } else {
            if (linPage < Paging.LINK_START)
                physPage = Paging.paging.FirstMb[linPage];
            else
                physPage = linPage;
            Paging.linkPage(linPage, physPage);
        }
    }

    public int initPageCheckOnly(int linAddr, int val) {
        int linPage = linAddr >>> 12;
        if (Paging.paging.Enabled) {
            if ((CPU.Block.CPL & CPU.Block.MPL) != 3)
                return 2;

            X86PageEntry table = new X86PageEntry();
            X86PageEntry entry = new X86PageEntry();
            if (!Paging.initPageCheckPresenceCheckOnly(linAddr, true, table, entry))
                return 0;

            if (Paging.initPageCheckUseraccess(entry.us, table.us)
                    || (((entry.wr == 0) || (table.wr == 0)))) {
                Log.logging(Log.LogTypes.PAGING, Log.LogServerities.Normal,
                        "Page access denied: cpl=%i, %x:%x:%x:%x", CPU.Block.CPL, entry.us,
                        table.us, entry.wr, table.wr);
                Paging.paging.CR2 = linAddr;
                CPU.Block.Exception.Which = CPU.ExceptionPF;
                CPU.Block.Exception.Error = 0x07;
                return 0;
            }
            Paging.linkPage(linPage, entry.Base);
        } else {
            int phys_page;
            if (linPage < Paging.LINK_START)
                phys_page = Paging.paging.FirstMb[linPage];
            else
                phys_page = linPage;
            Paging.linkPage(linPage, phys_page);
        }
        return 1;
    }

    public void initPageForced(int linAddr) {
        int linPage = linAddr >>> 12;
        int physPage;
        if (Paging.paging.Enabled) {
            X86PageEntry table = new X86PageEntry();
            X86PageEntry entry = new X86PageEntry();
            Paging.initPageCheckPresence(linAddr, true, table, entry);

            if (table.a == 0) {
                table.a = 1; // Set access
                Memory.physWriteD((int) ((Paging.paging.Base.Page << 12) + (linPage >>> 10) * 4),
                        table.getLoad());
            }
            if (entry.a == 0) {
                entry.a = 1; // Set access
                Memory.physWriteD((table.Base << 12) + (linPage & 0x3ff) * 4, entry.getLoad());
            }
            physPage = entry.Base;
        } else {
            if (linPage < Paging.LINK_START)
                physPage = Paging.paging.FirstMb[linPage];
            else
                physPage = linPage;
        }
        Paging.linkPage(linPage, physPage);
    }
}
