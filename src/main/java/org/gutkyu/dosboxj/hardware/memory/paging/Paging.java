package org.gutkyu.dosboxj.hardware.memory.paging;

import org.gutkyu.dosboxj.misc.setup.*;
import org.gutkyu.dosboxj.cpu.*;
import org.gutkyu.dosboxj.util.*;
import org.gutkyu.dosboxj.*;
import org.gutkyu.dosboxj.hardware.memory.*;
import org.gutkyu.dosboxj.misc.*;

public final class Paging {
    class PagingModule extends ModuleBase {
        public PagingModule(Section configuration) {
            super(configuration);
            /* Setup default Page Directory, force it to update */
            paging.Enabled = false;
            initTLB();
            int i;
            for (i = 0; i < LINK_START; i++) {
                paging.FirstMb[i] = i;
            }
            PFQueue.Used = 0;
        }
    }

    private static final CPUDecoder PageFaultDecoder = new CPUDecoder(Paging::pageFaultCore);
    /*--------------------------- begin PAGINGPartial -----------------------------*/
    private static final int LINK_TOTAL = (64 * 1024);

    public static final short MEM_PAGE_SIZE = (4096);
    public static final short XMS_START = (0x110);

    public static final int TLB_SIZE = 65536; // This must a power of 2 and greater then LINK_START
    public static final int BANK_SHIFT = 28;
    public static final int BANK_MASK = 0xffff; // always the same as TLB_SIZE-1?
    public static final int TLB_BANKS = ((1024 * 1024 / TLB_SIZE) - 1);

    public static final byte PFLAG_READABLE = 0x1;
    public static final byte PFLAG_WRITEABLE = 0x2;
    public static final byte PFLAG_HASROM = 0x4;
    public static final byte PFLAG_HASCODE = 0x8; // Page contains dynamic code
    public static final byte PFLAG_NOCODE = 0x10; // No dynamic code can be generated here
    public static final byte PFLAG_INIT = 0x20; // No dynamic code can be generated here

    public static final int LINK_START = ((1024 + 64) / 4); // Start right after the HMA

    // Allow 128 mb of memory to be linked
    public static final int PAGING_LINKS = (128 * 1024 / 4);


    // public const UInt32 NullState = UInt32.MinValue;
    public static final int NullState = 0;

    public static final class TLBEntry {
        public byte[] RMemAlloc = null;// 포인터 대체용도; read,write가 가리키는 할당된 메모리(dos시스템, 그래픽카드 등)
        public byte[] WMemAlloc = null;// 포인터 대체용도; read,write가 가리키는 할당된 메모리(dos시스템, 그래픽카드 등)
        public int Read = NullState;// 도스 메모리로 할당된 영역의 주소(배열의 인덱스)
        public int Write = NullState;// 도스 메모리로 할당된 영역의 주소(배열의 인덱스)
        public PageHandler ReadHandler;
        public PageHandler WriteHandler;
        public int PhysPage;
    }

    public static final class BaseInfo {
        public long Page;
        public int Addr;
    }

    public static final class Links {
        public long Used;
        public int[] Entries = new int[PAGING_LINKS];
    }

    public static final class PagingBlock {
        public long CR3;
        public long CR2;
        public BaseInfo Base = new BaseInfo();

        public TLBEntry[] TLBh = new TLBEntry[TLB_SIZE];
        public TLBEntry[][] TLBhBanks = new TLBEntry[TLB_BANKS][];

        public Links Links = new Links();
        public int[] FirstMb = new int[LINK_START];
        public boolean Enabled;
    }

    private class PFEntry {
        public int CS;
        public int EIP;
        public int PageAddr;
        public int Mpl;
    }

    private static final int PF_QUEUESIZE = 16;

    private static class PFQueue {
        public static int Used;
        public static PFEntry[] Entries = new PFEntry[PF_QUEUESIZE];
    }


    private static int pageFaultCore() {
        CPU.CycleLeft += CPU.Cycles;
        CPU.Cycles = 1;
        int ret = CoreFull.instance().runCPUCore();
        CPU.CycleLeft += CPU.Cycles;
        if (ret < 0)
            Support.exceptionExit("Got a dosbox close machine in pagefault core?");
        if (ret != 0)
            return ret;
        if (PFQueue.Used == 0)
            Support.exceptionExit("PF Core without PF");
        PFEntry entry = PFQueue.Entries[PFQueue.Used - 1];
        X86PageEntry pentry = new X86PageEntry();
        pentry.setLoad(Memory.physReadD(entry.PageAddr));
        if (pentry.p != 0 && entry.CS == Register.segValue(Register.SEG_NAME_CS)
                && entry.EIP == Register.getRegEIP()) {
            CPU.Block.MPL = entry.Mpl;
            return -1;
        }
        return 0;
    }

    public static void pageFault(int lin_addr, int page_addr, int faultcode) {
        /* Save the state of the cpu cores */
        LazyFlags old_lflags;
        // memcpy(&old_lflags,&lflags,sizeof(LazyFlags));
        old_lflags = Flags.LzFlags;

        CPUDecoder old_cpudecoder;
        old_cpudecoder = CPU.CpuDecoder;
        CPU.CpuDecoder = Paging.PageFaultDecoder;

        paging.CR2 = lin_addr;
        PFEntry entry = PFQueue.Entries[PFQueue.Used++];
        Log.logging(Log.LogTypes.PAGING, Log.LogServerities.Normal,
                "PageFault at %X type [%x] queue %d", lin_addr, faultcode, PFQueue.Used);
        // Log.LOG_MSG("EAX:%04X ECX:%04X EDX:%04X EBX:%04X",reg_eax,reg_ecx,reg_edx,reg_ebx);
        // Log.LOG_MSG("CS:%04X EIP:%08X SS:%04x
        // SP:%08X",SegValue(cs),reg_eip,SegValue(ss),reg_esp);
        entry.CS = Register.segValue(Register.SEG_NAME_CS);
        entry.EIP = Register.getRegEIP();
        entry.PageAddr = page_addr;
        entry.Mpl = CPU.Block.MPL;
        CPU.Block.MPL = 3;

        CPU.exception(CPU.ExceptionPF, faultcode);

        DOSBox.runMachine();
        PFQueue.Used--;
        Log.logging(Log.LogTypes.PAGING, Log.LogServerities.Normal,
                "Left PageFault for %x queue %d", lin_addr, PFQueue.Used);
        // memcpy(&lflags,&old_lflags,sizeof(LazyFlags));
        // struct는 = 연산자를 사용하면 값 복사를 수행하므로 굳이 memcpy를 사용할 필요없는데...
        Flags.LzFlags = old_lflags;
        CPU.CpuDecoder = old_cpudecoder;
        // Log.LOG_MSG("SS:%04x SP:%08X",SegValue(ss),reg_esp);
    }

    public static void initPageUpdateLink(int relink, int addr) {
        if (relink == 0)
            return;
        if (paging.Links.Used != 0) {
            if (paging.Links.Entries[(int) (paging.Links.Used - 1)] == (addr >>> 12)) {
                paging.Links.Used--;
                unlinkPages(addr >>> 12, 1);
            }
        }
        if (relink > 1)
            linkPageReadOnly(addr >>> 12, relink);
    }

    public static void initPageCheckPresence(int linAddr, boolean writing, X86PageEntry table,
            X86PageEntry entry) {
        int linPage = linAddr >>> 12;
        int dIndex = linPage >>> 10;
        int tIndex = linPage & 0x3ff;
        int tableAddr = (int) ((paging.Base.Page << 12) + dIndex * 4);
        table.setLoad(Memory.physReadD(tableAddr));
        if (table.p == 0) {
            Log.logging(Log.LogTypes.PAGING, Log.LogServerities.Normal, "NP Table");
            pageFault(linAddr, tableAddr, (writing ? 0x02 : 0x00)
                    | (((CPU.Block.CPL & CPU.Block.MPL) == 0) ? 0x00 : 0x04));
            table.setLoad(Memory.physReadD(tableAddr));
            if (table.p == 0)
                Support.exceptionExit("Pagefault didn't correct table");
        }
        int entry_addr = (table.Base << 12) + tIndex * 4;
        entry.setLoad(Memory.physReadD(entry_addr));
        if (entry.p == 0) {
            // LOG(LOG_PAGING,LOG_NORMAL)("NP Page");
            pageFault(linAddr, entry_addr, (writing ? 0x02 : 0x00)
                    | (((CPU.Block.CPL & CPU.Block.MPL) == 0) ? 0x00 : 0x04));
            entry.setLoad(Memory.physReadD(entry_addr));
            if (entry.p == 0)
                Support.exceptionExit("Pagefault didn't correct page");
        }
    }

    public static boolean initPageCheckPresenceCheckOnly(int linAddr, boolean writing,
            X86PageEntry table, X86PageEntry entry) {
        int linPage = linAddr >>> 12;
        int dIndex = linPage >>> 10;
        int tIndex = linPage & 0x3ff;
        int tableAddr = (int) ((paging.Base.Page << 12) + dIndex * 4);
        table.setLoad(Memory.physReadD(tableAddr));
        if (table.p == 0) {
            paging.CR2 = linAddr;
            CPU.Block.Exception.Which = CPU.ExceptionPF;
            CPU.Block.Exception.Error = (writing ? 0x02 : 0x00)
                    | (((CPU.Block.CPL & CPU.Block.MPL) == 0) ? 0x00 : 0x04);
            return false;
        }
        int entry_addr = (table.Base << 12) + tIndex * 4;
        entry.setLoad(Memory.physReadD(entry_addr));
        if (entry.p == 0) {
            paging.CR2 = linAddr;
            CPU.Block.Exception.Which = CPU.ExceptionPF;
            CPU.Block.Exception.Error = (writing ? 0x02 : 0x00)
                    | (((CPU.Block.CPL & CPU.Block.MPL) == 0) ? 0x00 : 0x04);
            return false;
        }
        return true;
    }

    // check if a user-level memory access would trigger a privilege page fault
    public static boolean initPageCheckUseraccess(int u1, int u2) {
        switch (CPU.ArchitectureType) {
            case CPU.ArchTypeMixed:
            case CPU.ArchType386Slow:
            case CPU.ArchType386Fast:
            default:
                return ((u1) == 0) && ((u2) == 0);
            case CPU.ArchType486OldSlow:
            case CPU.ArchType486NewSlow:
            case CPU.ArchTypePentiumSlow:
                return ((u1) == 0) || ((u2) == 0);
        }
    }


    public static PagingBlock paging = new PagingBlock();

    public static TLBEntry getTLBEntry(int address) {
        int index = address >>> 12;
        if (TLB_BANKS != 0 && (index > TLB_SIZE)) {
            int bank = (address >>> BANK_SHIFT) - 1;
            if (paging.TLBhBanks[bank] == null)
                paging.TLBhBanks[bank] = initTLBBank();
            return paging.TLBhBanks[bank][index & BANK_MASK];
        }
        return paging.TLBh[index];
    }


    public static PageHandler getTLBReadHandler(int address) {
        return getTLBEntry(address).ReadHandler;
    }

    public static PageHandler getTLBWriteHandler(int address) {
        return getTLBEntry(address).WriteHandler;
    }

    /* Use these helper functions to access linear addresses in readX/writeX functions */
    public static int getPhysicalPage(int linePage) {
        TLBEntry entry = getTLBEntry(linePage);
        return entry.PhysPage << 12;
    }

    public static int getPhysicalAddress(int linAddr) {
        TLBEntry entry = getTLBEntry(linAddr);
        return (entry.PhysPage << 12) | (linAddr & 0xfff);
    }
    /* Special inlined memory reading/writing */

    // byte(int)
    public static int memReadBInline(int address) {
        TLBEntry entry = getTLBEntry(address);
        if (entry.Read != NullState)
            return Memory.hostReadB(entry.RMemAlloc, entry.Read + address);
        else
            return getTLBReadHandler(address).readB(address);
    }

    public static int memReadWInline(int address) {
        if ((address & 0xfff) < 0xfff) {
            TLBEntry entry = getTLBEntry(address);
            if (entry.Read != NullState)
                return Memory.hostReadW(entry.RMemAlloc, entry.Read + address);
            else
                return getTLBReadHandler(address).readW(address);
        } else
            return Memory.unalignedReadW(address);
    }

    public static int memReadDInline(int address) {
        if ((address & 0xfff) < 0xffd) {
            TLBEntry entry = getTLBEntry(address);
            if (entry.Read != NullState)
                return Memory.hostReadD(entry.RMemAlloc, entry.Read + address);
            else
                return (int) getTLBReadHandler(address).readD(address);
        } else
            return Memory.unalignedReadD(address);
    }

    public static void memWriteBInlineB(int address, int val) {
        TLBEntry entry = getTLBEntry(address);
        if (entry.Write != NullState)
            Memory.hostWriteB(entry.WMemAlloc, entry.Write + address, val);
        else
            getTLBWriteHandler(address).writeB(address, val);
    }

    public static void memWriteWInlineW(int address, int val) {
        if ((address & 0xfff) < 0xfff) {
            TLBEntry entry = getTLBEntry(address);
            if (entry.Write != NullState)
                Memory.hostWriteW(entry.WMemAlloc, entry.Write + address, val);
            else
                getTLBWriteHandler(address).writeW(address, val);
        } else
            Memory.unalignedWriteW(address, val);
    }

    public static void memWriteDInlineD(int address, int val) {
        if ((address & 0xfff) < 0xffd) {
            TLBEntry entry = getTLBEntry(address);
            if (entry.Write != NullState)
                Memory.hostWriteD(entry.WMemAlloc, entry.Write + address, val);
            else
                getTLBWriteHandler(address).writeD(address, val);
        } else
            Memory.unalignedWriteD(address, val);
    }


    // checked == true 경우 -1 반환
    // checked == false 경우 해당 값 반환
    // byte(int)
    public static int memReadBChecked(int address) {
        TLBEntry entry = getTLBEntry(address);
        if (entry.Read != NullState) {
            // return false;
            return Memory.hostReadB(entry.RMemAlloc, entry.Read + address);
        } else {
            return getTLBReadHandler(address).readBChecked(address);
        }
    }

    // checked == true 경우 -1 반환
    // checked == false 경우 해당 값 반환
    public static int memReadWChecked(int address) {
        if ((address & 0xfff) < 0xfff) {
            TLBEntry entry = getTLBEntry(address);
            if (entry.Read != NullState) {
                return Memory.hostReadW(entry.RMemAlloc, entry.Read + address);
                // return false;
            } else
                return getTLBReadHandler(address).readWChecked(address);
        } else
            return Memory.unalignedReadWChecked(address);
    }

    // checked == true 경우 -1 반환
    // checked == long 경우 해당 값 반환
    public static long memReadDChecked(int address) {
        if ((address & 0xfff) < 0xffd) {
            TLBEntry entry = getTLBEntry(address);
            if (entry.Read != NullState) {
                return Memory.hostReadD(entry.RMemAlloc, entry.Read + address);
                // return false;
            } else
                return getTLBReadHandler(address).readDChecked(address);
        } else
            return Memory.unalignedReadDChecked(address);
    }

    // (int address, byte val)
    public static boolean memWriteBChecked(int address, int val) {
        TLBEntry entry = getTLBEntry(address);
        if (entry.Write != NullState) {
            Memory.hostWriteB(entry.WMemAlloc, entry.Write + address, val);
            return false;
        } else
            return getTLBWriteHandler(address).writeBChecked(address, val);
    }

    // (int address, short val)
    public static boolean memWriteWChecked(int address, int val) {
        if ((address & 0xfff) < 0xfff) {
            TLBEntry entry = getTLBEntry(address);
            if (entry.Write != NullState) {
                Memory.hostWriteW(entry.WMemAlloc, entry.Write + address, val);
                return false;
            } else
                return getTLBWriteHandler(address).writeWChecked(address, val);
        } else
            return Memory.unalignedWriteWChecked(address, val);
    }

    public static boolean memWriteDChecked(int address, int val) {
        if ((address & 0xfff) < 0xffd) {
            TLBEntry entry = getTLBEntry(address);
            if (entry.Write != NullState) {
                Memory.hostWriteD(entry.WMemAlloc, entry.Write + address, val);
                return false;
            } else
                return getTLBWriteHandler(address).writeDChecked(address, val);
        } else
            return Memory.unalignedWriteDChecked(address, val);
    }

    private static InitPageHandler _initPageHandler = new InitPageHandler();
    private static InitPageUserROHandler initPageHandlerUserRO = new InitPageUserROHandler();

    public static int getDirBase() {
        return (int) paging.CR3;
    }

    private static void initTLBInt(TLBEntry[] bank) {
        for (int i = 0; i < TLB_SIZE; i++) {
            if (bank[i] == null)
                bank[i] = new TLBEntry();
            bank[i].RMemAlloc = null;
            bank[i].Read = NullState;
            bank[i].WMemAlloc = null;
            bank[i].Write = NullState;
            bank[i].ReadHandler = _initPageHandler;
            bank[i].WriteHandler = _initPageHandler;
        }
    }

    public static TLBEntry[] initTLBBank() {
        TLBEntry[] bank = new TLBEntry[TLB_SIZE];
        // if (bank == null) misc.Support.E_Exit("Out of Memory");
        initTLBInt(bank);
        return bank;
    }

    private static void initTLB() {
        initTLBInt(paging.TLBh);
        paging.Links.Used = 0;
    }

    public static void clearTLB() {
        int entryId = 0;
        for (; paging.Links.Used > 0; paging.Links.Used--) {
            int page = paging.Links.Entries[entryId];
            entryId++;
            TLBEntry entry = getTLBEntry(page << 12);
            entry.RMemAlloc = null;
            entry.Read = NullState;
            entry.WMemAlloc = null;
            entry.Write = NullState;
            entry.ReadHandler = _initPageHandler;
            entry.WriteHandler = _initPageHandler;
        }
        paging.Links.Used = 0;
    }

    private static void unlinkPages(int linPage, int pages) {
        for (; pages > 0; pages--) {
            TLBEntry entry = getTLBEntry(linPage << 12);
            entry.RMemAlloc = null;
            entry.Read = NullState;
            entry.WMemAlloc = null;
            entry.Write = NullState;
            entry.ReadHandler = _initPageHandler;
            entry.WriteHandler = _initPageHandler;
            linPage++;
        }
    }

    public static void mapPage(int linPage, int physPage) {
        if (linPage < LINK_START) {
            paging.FirstMb[linPage] = physPage;
            TLBEntry tlbh = paging.TLBh[linPage];
            tlbh.RMemAlloc = null;
            tlbh.Read = NullState;
            tlbh.WMemAlloc = null;
            tlbh.Write = NullState;
            tlbh.ReadHandler = _initPageHandler;
            tlbh.WriteHandler = _initPageHandler;
        } else {
            linkPage(linPage, physPage);
        }
    }

    public static void linkPage(int linPage, int physPage) {
        PageHandler handler = Memory.getPageHandler(physPage);
        int linBase = linPage << 12;
        if (linPage >= (TLB_SIZE * (TLB_BANKS + 1)) || physPage >= (TLB_SIZE * (TLB_BANKS + 1)))
            Support.exceptionExit("Illegal page");

        if (paging.Links.Used >= PAGING_LINKS) {
            Log.logging(Log.LogTypes.PAGING, Log.LogServerities.Normal,
                    "Not enough paging links, resetting cache");
            clearTLB();
        }

        TLBEntry entry = getTLBEntry(linBase);
        entry.PhysPage = physPage;
        if ((handler.Flags & PFLAG_READABLE) != 0) {
            entry.RMemAlloc = handler.getHostMemory();
            entry.Read = handler.getHostReadPt(physPage) - linBase;
        } else {
            entry.RMemAlloc = null;
            entry.Read = NullState;
        }
        if ((handler.Flags & PFLAG_WRITEABLE) != 0) {
            entry.WMemAlloc = handler.getHostMemory();
            entry.Write = handler.getHostWritePt(physPage) - linBase;
        } else {
            entry.WMemAlloc = null;
            entry.Write = NullState;
        }

        paging.Links.Entries[(int) paging.Links.Used++] = linPage;
        entry.ReadHandler = handler;
        entry.WriteHandler = handler;
    }


    public static void linkPageReadOnly(int linPage, int physPage) {
        PageHandler handler = Memory.getPageHandler(physPage);
        int linBase = linPage << 12;
        if (linPage >= (TLB_SIZE * (TLB_BANKS + 1)) || physPage >= (TLB_SIZE * (TLB_BANKS + 1)))
            Support.exceptionExit("Illegal page");

        if (paging.Links.Used >= PAGING_LINKS) {
            Log.logging(Log.LogTypes.PAGING, Log.LogServerities.Normal,
                    "Not enough paging links, resetting cache");
            clearTLB();
        }

        TLBEntry entry = getTLBEntry(linBase);
        entry.PhysPage = physPage;
        if ((handler.Flags & PFLAG_READABLE) != 0) {
            entry.RMemAlloc = handler.getHostMemory();
            entry.Read = handler.getHostReadPt(physPage) - linBase;
        } else {
            entry.RMemAlloc = null;
            entry.Read = NullState;
        }
        entry.WMemAlloc = null;
        entry.Write = NullState;

        paging.Links.Entries[(int) paging.Links.Used++] = linPage;
        entry.ReadHandler = handler;
        entry.WriteHandler = initPageHandlerUserRO;
    }

    public static void setDirBase(int cr3) {
        paging.CR3 = cr3;

        paging.Base.Page = cr3 >>> 12;
        paging.Base.Addr = cr3 & ~4095;
        // Log.Logging(Log.LOG_TYPES.LOG_PAGING,Log.LOG_SEVERITIES.LOG_NORMAL,"CR3:%X Base
        // %X",cr3,paging.super.page);
        if (paging.Enabled) {
            clearTLB();
        }
    }

    public static void enable(boolean enabled) {
        /* If paging is disable we work from a default paging table */
        if (paging.Enabled == enabled)
            return;
        paging.Enabled = enabled;
        if (enabled) {
            // if (CPU.CpuDecoder == CoreSimple.Instance()::CPUCoreRun)
            if (CPU.CpuDecoder == CoreSimple.instance().CpuDecoder) {
                // Log.LOG_MSG("CPU core simple won't run this game,switching to normal");
                CPU.CpuDecoder = CoreNormal.instance().CpuDecoder;
                CPU.CycleLeft += CPU.Cycles;
                CPU.Cycles = 0;
            }
            // Log.Logging(Log.LOG_TYPES.LOG_PAGING,Log.LOG_SEVERITIES.LOG_NORMAL,"Enabled");
            setDirBase((int) paging.CR3);
        }
        clearTLB();
    }

    public static boolean enabled() {
        return paging.Enabled;
    }

    private static PagingModule _paging;

    public static void init(Section sec) {
        _paging = (new Paging()).new PagingModule(sec);
    }
    /*--------------------------- end PAGINGPartial -----------------------------*/

}
