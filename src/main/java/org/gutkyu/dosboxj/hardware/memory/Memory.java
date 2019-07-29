package org.gutkyu.dosboxj.hardware.memory;

import org.gutkyu.dosboxj.hardware.io.IO;
import org.gutkyu.dosboxj.hardware.io.iohandler.*;
import org.gutkyu.dosboxj.util.*;
import org.gutkyu.dosboxj.misc.setup.*;
import org.gutkyu.dosboxj.misc.setup.Value.WrongType;
import org.gutkyu.dosboxj.misc.*;
import org.gutkyu.dosboxj.interrupt.*;
import org.gutkyu.dosboxj.interrupt.EMS.*;
import java.util.Arrays;
import org.gutkyu.dosboxj.*;
import org.gutkyu.dosboxj.hardware.memory.paging.*;

public final class Memory {

    protected Memory() {

    }

    public static int memStrLen(int pt) {
        int x = 0;
        while (x < 1024) {
            if (Paging.memReadBInline(pt + x) == 0)
                return x;
            x++;
        }
        return 0; // Hope this doesn't happen
    }

    public static void strCpy(int dest, int src) {
        int r;
        while ((r = readB(src++)) > 0)
            Paging.memWriteBInlineB(dest++, r);
        Paging.memWriteBInlineB(dest, 0);
    }

    public static void memCpy(int dest, int src, int size) {
        while (size-- != 0)
            Paging.memWriteBInlineB(dest++, Paging.memReadBInline(src++));
    }

    // byte(byte[], int)
    public static int hostReadB(byte[] mem, int off) {
        return 0xff & mem[off];
    }

    public static int hostReadW(byte[] mem, int off) {
        return ByteConv.getShort(mem, off);
    }

    public static int hostReadD(byte[] mem, int off) {
        return ByteConv.getInt(mem, off);
    }

    public static void hostWriteB(byte[] mem, int off, int val) {
        mem[off] = (byte) val;
    }

    public static void hostWriteW(byte[] mem, int off, int val) {
        mem[off++] = (byte) val;
        mem[off] = (byte) (val >>> 8);
    }

    public static void hostWriteD(byte[] mem, int off, int val) {
        mem[off++] = (byte) val;
        mem[off++] = (byte) (val >>> 8);
        mem[off++] = (byte) (val >>> 16);
        mem[off] = (byte) (val >>> 24);
    }

    // Integer.MaxValue - 1 Bytes 까지의 메인 메모리 접근 보장
    // offset은 int타입
    // byte(int)
    public static int hostReadB(int off) {
        return 0xff & allocMemory[off];
    }

    public static int hostReadW(int off) {
        return ByteConv.getShort(allocMemory, off);
    }

    public static int hostReadD(int off) {
        return ByteConv.getInt(allocMemory, off);
    }

    public static void hostWriteB(int off, int val) {
        allocMemory[off] = (byte) val;
    }

    public static void hostWriteW(int off, int val) {
        allocMemory[off++] = (byte) val;
        allocMemory[off] = (byte) (val >>> 8);
    }

    public static void hostWriteD(int off, int val) {
        allocMemory[off++] = (byte) val;
        allocMemory[off++] = (byte) (val >>> 8);
        allocMemory[off++] = (byte) (val >>> 16);
        allocMemory[off] = (byte) (val >>> 24);
    }

    /*
     * The Folowing six functions are slower but they recognize the paged memory system
     */

    public static void physWriteB(int addr, int val) {
        hostWriteB(MemBase + addr, val);
    }

    public static void physWriteW(int addr, int val) {
        hostWriteW(MemBase + addr, val);
    }

    public static void physWriteD(int addr, int val) {
        hostWriteD(MemBase + addr, val);
    }

    // byte(int)
    public static int physReadB(int addr) {
        return hostReadB(MemBase + addr);
    }

    // uint16
    public static int physReadW(int addr) {
        return hostReadW(MemBase + addr);
    }

    public static int physReadD(int addr) {
        return hostReadD(MemBase + addr);
    }

    // byte(int)
    public static int readB(int address) {
        return Paging.memReadBInline(address);
    }

    // uint16
    public static int readW(int address) {
        return Paging.memReadWInline(address);
    }

    public static int readD(int address) {
        return Paging.memReadDInline(address);
    }

    // (int, byte)
    public static void writeB(int address, int val) {
        Paging.memWriteBInlineB(address, val);
    }

    // (int, uint16)
    public static void writeW(int address, int val) {
        Paging.memWriteWInlineW(address, val);
    }

    public static void writeD(int address, int val) {
        Paging.memWriteDInlineD(address, val);
    }

    public static void writeD(int address, long val) {
        writeD(address, (int) val);
    }

    /* These don't check for alignment, better be sure it's correct */
    /*
     * The folowing functions are all shortcuts to the above functions using physical addressing
     */

    // uint16(int, int)
    public static int realReadB(int seg, int off) {

        return readB((seg << 4) + off);
    }

    public static int realReadW(int seg, int off) {
        return readW((seg << 4) + off);
    }

    public static long realReadD(int seg, int off) {
        return readD((seg << 4) + off);
    }

    // public static void RealWriteB(int seg, int off, byte val) {
    public static void realWriteB(int seg, int off, int val) {
        writeB(((seg << 4) + off), val);
    }

    // public static void RealWriteW(int seg, int off, uint16 val) {
    public static void realWriteW(int seg, int off, int val) {
        writeW((seg << 4) + off, val);
    }

    public static void realWriteD(int seg, int off, int val) {
        writeD((seg << 4) + off, val);
    }

    public static void realWriteD(int seg, int off, long val) {
        realWriteD(seg, off, (int) val);
    }

    // uint16(int)
    public static int realSeg(int pt) {
        return pt >>> 16;
    }

    // uint16(int)
    public static int realOff(int pt) {
        return pt & 0xffff;
    }

    public static int real2Phys(int pt) {
        return (realSeg(pt) << 4) + realOff(pt);
    }

    // uint16, uint16
    public static int physMake(int seg, int off) {
        return ((0xffff & seg) << 4) + (0xffff & off);
    }

    // uint16, uint16
    public static int realMake(int seg, int off) {
        return ((0xffff & seg) << 16) + (0xffff & off);
    }

    // (byte, int)
    public static void realSetVec(int vec, int pt) {
        writeD((0xff & vec) << 2, pt);
    }

    // 주의
    // 이 함수를 호출하는 코드에서는 반환된 old값을 저장하는 과정을 추가해야한다.
    // int(byte,int)
    public static int realSetVecAndReturnOld(int vec, int pt) {
        int old = readD((0xff & vec) << 2);
        writeD((0xff & vec) << 2, pt);
        return old;
    }

    // (byte)
    public static int realGetVec(int vec) {
        return readD((0xff & vec) << 2);
    }

    /*--------------------------- begin MEMORYPartial -----------------------------*/
    public static final int MEM_PAGESIZE = 4096;

    private static final int PAGES_IN_BLOCK = (1024 * 1024) / Paging.MEM_PAGE_SIZE;
    private static final int SAFE_MEMORY = 32;
    private static final int MAX_MEMORY = 64;
    private static final int MAX_PAGE_ENTRIES = MAX_MEMORY * 1024 * 1024 / 4096;
    private static final int LFB_PAGES = 512;
    private static final int MAX_LINKS = (MAX_MEMORY * 1024 / 4) + 4096; // Hopefully enough
    public static final int MemBase = 1;

    private static class LinkBlock {
        public int used;
        public int[] pages = new int[MAX_LINKS];
    }

    private static class MemoryBlock {
        public int pages;
        public PageHandler[] pHandlers;
        public int[] mHandles;
        public LinkBlock links = new LinkBlock();
        public int lfbStartPage;
        public int lfbEndPage;
        public int lfbPages;
        public PageHandler lfbHandler;
        public PageHandler lfbMMIOHandler;
        public boolean a20Enabled;
        public byte a20ControlPort;
    }

    private static MemoryBlock memory = new MemoryBlock();

    // HostPt MemBase;
    private static byte[] allocMemory;// 실제 할당된 메모리를 할당하고 가리키는 MemBase 대체

    private static IllegalPageHandler illegalPageHandler = new IllegalPageHandler();
    private static RAMPageHandler ramPageHandler = new RAMPageHandler();
    private static ROMPageHandler romPageHandler = new ROMPageHandler();

    public static void setLFB(int page, int pages, PageHandler handler, PageHandler mmiohandler) {
        memory.lfbHandler = handler;
        memory.lfbMMIOHandler = mmiohandler;
        memory.lfbStartPage = page;
        memory.lfbEndPage = page + pages;
        memory.lfbPages = pages;
        Paging.clearTLB();
    }

    public static PageHandler getPageHandler(int phys_page) {
        if (phys_page < memory.pages) {
            return memory.pHandlers[phys_page];
        } else if ((phys_page >= memory.lfbStartPage) && (phys_page < memory.lfbEndPage)) {
            return memory.lfbHandler;
        } else if ((phys_page >= memory.lfbStartPage + 0x01000000 / 4096)
                && (phys_page < memory.lfbStartPage + 0x01000000 / 4096 + 16)) {
            return memory.lfbMMIOHandler;
        }
        return illegalPageHandler;
    }

    public static void setPageHandler(int phys_page, int pages, PageHandler handler) {
        for (; pages > 0; pages--) {
            memory.pHandlers[phys_page] = handler;
            phys_page++;
        }
    }

    public static void resetPageHandler(int phys_page, int pages) {
        for (; pages > 0; pages--) {
            memory.pHandlers[phys_page] = ramPageHandler;
            phys_page++;
        }
    }

    public static void blockRead(int pt, byte[] read, int readIdx, int size) {
        while (size-- > 0) {
            read[readIdx++] = (byte) Paging.memReadBInline(pt++);
        }
    }

    public static void blockRead(int pt, CStringPt read) {
        int i = 0, size = read.lengthWithNull();
        while (size-- > 0) {
            read.set(i++, (char) Paging.memReadBInline(pt++));
        }
    }

    public static void blockRead(int pt, CStringPt read, int size) {
        int i = 0;
        while (size-- > 0) {
            read.set(i++, (char) Paging.memReadBInline(pt++));
        }
    }

    public static void blockRead(int pt, EMS.EMMMapping[] read, int start, int size) {
        if (size < 0)
            size = read.length;
        for (int i = start; i < size; i++) {
            read[i].setHandle(Paging.memReadBInline(pt++), Paging.memReadBInline(pt++));
            read[i].setPage(Paging.memReadBInline(pt++), Paging.memReadBInline(pt++));
        }
    }

    public static void blockRead(int pt, EMS.EMMMapping[] read) {
        blockRead(pt, read, 0, -1);
    }

    public static void blockWrite(int pt, byte[] write, int writeIdx, int size) {
        while (size-- > 0) {
            Paging.memWriteBInlineB(pt++, write[writeIdx++]);
        }
    }

    public static void blockWrite(int pt, byte[] write) {
        blockWrite(pt, write, 0, write.length);
    }

    // cspt : ASCII, terminated with null
    public static void blockWrite(int pt, CStringPt cspt) {
        blockWrite(pt, cspt, cspt.lengthWithNull());
    }

    public static void blockWrite(int pt, CStringPt cspt, int size) {
        int i = 0;
        while (size-- > 0) {
            Paging.memWriteBInlineB(pt++, cspt.get(i++));
        }
    }

    public static void blockWrite(int pt, ByteSequence bytes, int size) {
        bytes.goFirst();
        while (size-- > 0) {
            if (!bytes.hasNext())
                throw new DOSException("MEM_BlockWrite 오류!");
            Paging.memWriteBInlineB(pt++, bytes.next());
        }
    }

    public static void blockWrite(int pt, EMMMapping write) {
        Paging.memWriteBInlineB(pt++, write.handle);
        Paging.memWriteBInlineB(pt++, write.handle >>> 8);
        Paging.memWriteBInlineB(pt++, write.page);
        Paging.memWriteBInlineB(pt++, write.page >>> 8);
    }

    public static void blockWrite(int pt, EMS.EMMMapping[] write, int start, int size) {
        if (size < 0)
            size = write.length;
        for (int i = 0; i < size; i++) {
            Paging.memWriteBInlineB(pt++, write[i].handle);
            Paging.memWriteBInlineB(pt++, write[i].handle >>> 8);
            Paging.memWriteBInlineB(pt++, write[i].page);
            Paging.memWriteBInlineB(pt++, write[i].page >>> 8);
        }
    }

    public static void blockWrite(int pt, EMS.EMMMapping[] write) {
        blockWrite(pt, write, 0, -1);
    }

    public static void blockCopy(int dest, int src, int size) {
        Memory.memCpy(dest, src, size);
    }

    public static void strCopy(int pt, byte[] data, int size) {
        int data_idx = 0;
        while (size-- > 0) {
            byte r = (byte) Paging.memReadBInline(pt++);
            if (r == 0)
                break;
            data[data_idx++] = r;
        }
        data[data_idx] = 0;
    }

    // 종료문자열 0x00을 붙인다.
    public static void strCopy(int pt, char[] data, int size) {
        int data_idx = 0;
        while (size-- > 0) {
            byte r = (byte) Paging.memReadBInline(pt++);
            if (r == 0)
                break;
            data[data_idx++] = (char) r;
        }
        if (data_idx < size)
            data[data_idx] = (char) 0;
    }

    public static String strCopy(int pt, int size) {
        char[] str = new char[size];
        int data_idx = 0;
        while (size-- > 0) {
            byte r = (byte) Paging.memReadBInline(pt++);
            if (r == 0)
                break;
            str[data_idx++] = (char) r;
        }
        return new String(str, 0, data_idx);
    }

    public static void strCopy(int pt, CStringPt data, int size) {
        while (size-- > 0) {
            byte r = (byte) Paging.memReadBInline(pt++);
            if (r == 0)
                break;
            data.set((char) r);
            data.movePtToR1();
        }
        data.set('\0');
    }

    public static int totalPages() {
        return memory.pages;
    }

    public static int freeLargest() {
        int size = 0;
        int largest = 0;
        int index = Paging.XMS_START;
        while (index < memory.pages) {
            if (memory.mHandles[index] == 0) {
                size++;
            } else {
                if (size > largest)
                    largest = size;
                size = 0;
            }
            index++;
        }
        if (size > largest)
            largest = size;
        return largest;
    }

    public static int freeTotal() {
        int free = 0;
        int index = Paging.XMS_START;
        while (index < memory.pages) {
            if (memory.mHandles[index] == 0)
                free++;
            index++;
        }
        return free;
    }

    public static int allocatedPages(int handle) {
        int pages = 0;
        while (handle > 0) {
            pages++;
            handle = memory.mHandles[handle];
        }
        return pages;
    }

    // TODO Maybe some protection for this whole allocation scheme

    private static int bestMatch(int size) {
        int index = Paging.XMS_START;
        int first = 0;
        int best = 0xfffffff;
        int best_first = 0;
        while (index < memory.pages) {
            /* Check if we are searching for first free page */
            if (first == 0) {
                /* Check if this is a free page */
                if (memory.mHandles[index] == 0) {
                    first = index;
                }
            } else {
                /* Check if this still is used page */
                if (memory.mHandles[index] != 0) {
                    int pages = index - first;
                    if (pages == size) {
                        return first;
                    } else if (pages > size) {
                        if (pages < best) {
                            best = pages;
                            best_first = first;
                        }
                    }
                    first = 0; // Always reset for new search
                }
            }
            index++;
        }
        /* Check for the final block if we can */
        if (first != 0 && (index - first >= size) && (index - first < best)) {
            return first;
        }
        return best_first;
    }

    public static int allocatePages(int pages, boolean sequence) {
        int ret = 0;
        if (pages == 0)
            return 0;
        if (sequence) {
            int index = bestMatch(pages);
            if (index == 0)
                return 0;

            int next_idx = -1;
            while (pages != 0) {
                if (next_idx < 0)
                    ret = index;
                else
                    memory.mHandles[next_idx] = index;
                next_idx = index;
                index++;
                pages--;
            }
            if (next_idx < 0)
                ret = -1;
            else
                memory.mHandles[next_idx] = -1;
        } else {
            if (freeTotal() < pages)
                return 0;
            // int* next = &ret;
            int next_idx = -1;
            while (pages != 0) {
                int index = bestMatch(1);
                if (index == 0)
                    Support.exceptionExit("MEM:corruption during allocate");
                while (pages != 0 && (memory.mHandles[index] == 0)) {
                    // *next = index;
                    // next = &memory.mhandles[index];
                    if (next_idx < 0)
                        ret = index;
                    else
                        memory.mHandles[next_idx] = index;
                    next_idx = index;
                    index++;
                    pages--;
                }
                // *next = -1; //Invalidate it in case we need another match
                // Invalidate it in case we need another match
                if (next_idx < 0)
                    ret = -1;
                else
                    memory.mHandles[next_idx] = -1;
            }
        }
        return ret;
    }

    public static int getNextFreePage() {
        return bestMatch(1);
    }

    public static void releasePages(int handle) {
        while (handle > 0) {
            int next = memory.mHandles[handle];
            memory.mHandles[handle] = 0;
            handle = next;
        }
    }

    public static int returnedReallocatePagesHandle;

    public static boolean tryReallocatePages(int handle, int pages, boolean sequence) {
        if (handle <= 0) {
            if (pages == 0)
                return true;
            returnedReallocatePagesHandle = handle = allocatePages(pages, sequence);
            return (handle > 0);
        }
        if (pages == 0) {
            releasePages(handle);
            returnedReallocatePagesHandle = handle = -1;
            return true;
        }
        int index = handle;
        int last = 0;
        int old_pages = 0;
        while (index > 0) {
            old_pages++;
            last = index;
            index = memory.mHandles[index];
        }
        if (old_pages == pages)
            return true;
        if (old_pages > pages) {
            /* Decrease size */
            pages--;
            index = handle;
            old_pages--;
            while (pages != 0) {
                index = memory.mHandles[index];
                pages--;
                old_pages--;
            }
            int next = memory.mHandles[index];
            memory.mHandles[index] = -1;
            index = next;
            while (old_pages != 0) {
                next = memory.mHandles[index];
                memory.mHandles[index] = 0;
                index = next;
                old_pages--;
            }
            return true;
        } else {
            /* Increase size, check for enough free space */
            int need = pages - old_pages;
            if (sequence) {
                index = last + 1;
                int free = 0;
                while ((index < memory.pages) && memory.mHandles[index] == 0) {
                    index++;
                    free++;
                }
                if (free >= need) {
                    /* Enough space allocate more pages */
                    index = last;
                    while (need != 0) {
                        memory.mHandles[index] = index + 1;
                        need--;
                        index++;
                    }
                    memory.mHandles[index] = -1;
                    return true;
                } else {
                    /* Not Enough space allocate new block and copy */
                    int newhandle = allocatePages(pages, true);
                    if (newhandle == 0)
                        return false;
                    Memory.blockCopy(newhandle * 4096, handle * 4096, old_pages * 4096);
                    releasePages(handle);
                    returnedReallocatePagesHandle = handle = newhandle;
                    return true;
                }
            } else {
                int rem = allocatePages(need, false);
                if (rem == 0)
                    return false;
                memory.mHandles[last] = rem;
                return true;
            }
        }
        // return false;
    }

    public static int nextHandle(int handle) {
        return memory.mHandles[handle];
    }

    public static int nextHandleAt(int handle, int where) {
        while (where != 0) {
            where--;
            handle = memory.mHandles[handle];
        }
        return handle;
    }

    /*
     * A20 line handling, Basically maps the 4 pages at the 1mb to 0mb in the default page directory
     */
    public static boolean A20Enabled() {
        return memory.a20Enabled;
    }

    public static void A20Enable(boolean enabled) {
        int phys_base = enabled ? (1024 / 4) : 0;
        for (int i = 0; i < 16; i++)
            Paging.mapPage((1024 / 4) + i, phys_base + i);
        memory.a20Enabled = enabled;
    }

    /* Memory access functions */
    // uint16(int)
    public static int unalignedReadW(int address) {
        return Paging.memReadBInline(address) | Paging.memReadBInline(address + 1) << 8;
    }

    public static int unalignedReadD(int address) {
        return Paging.memReadBInline(address) | (Paging.memReadBInline(address + 1) << 8)
                | (Paging.memReadBInline(address + 2) << 16)
                | (Paging.memReadBInline(address + 3) << 24);
    }

    public static void unalignedWriteW(int address, int val) {
        Paging.memWriteBInlineB(address, val);
        val >>>= 8;
        Paging.memWriteBInlineB(address + 1, val);
    }

    public static void unalignedWriteD(int address, int val) {
        Paging.memWriteBInlineB(address, val);
        val >>>= 8;
        Paging.memWriteBInlineB(address + 1, val);
        val >>>= 8;
        Paging.memWriteBInlineB(address + 2, val);
        val >>>= 8;
        Paging.memWriteBInlineB(address + 3, val);
    }

    // checked == true 경우 -1 반환
    // checked == false 경우 해당 값 반환
    // uint16(int)
    public static int unalignedReadWChecked(int address) {
        int rval1 = Paging.memReadBChecked(address + 0);
        if (rval1 < 0)
            return -1;
        int rval2 = Paging.memReadBChecked(address + 1);
        if (rval2 < 0)
            return -1;
        return (0xff & rval1) | ((0xff & rval2) << 8);
    }

    // checked == true 경우 -1 반환
    // checked == false 경우 해당 값 반환
    public static long unalignedReadDChecked(int address) {
        int rval1 = Paging.memReadBChecked(address + 0);
        if (rval1 < 0)
            return -1;
        int rval2 = Paging.memReadBChecked(address + 1);
        if (rval2 < 0)
            return -1;
        int rval3 = Paging.memReadBChecked(address + 2);
        if (rval3 < 0)
            return -1;
        int rval4 = Paging.memReadBChecked(address + 3);
        if (rval4 < 0)
            return -1;
        return (0xff & rval1) | ((0xff & rval2) << 8) | ((0xff & rval3) << 16)
                | ((0xff & rval4) << 24);
        // return false;
    }

    // boolean UnalignedWriteWChecked(int address, short val)
    public static boolean unalignedWriteWChecked(int address, int val) {
        if (Paging.memWriteBChecked(address, val & 0xff))
            return true;
        val >>>= 8;
        if (Paging.memWriteBChecked(address + 1, val & 0xff))
            return true;
        return false;
    }

    public static boolean unalignedWriteDChecked(int address, int val) {
        if (Paging.memWriteBChecked(address, val & 0xff))
            return true;
        val >>>= 8;
        if (Paging.memWriteBChecked(address + 1, val & 0xff))
            return true;
        val >>>= 8;
        if (Paging.memWriteBChecked(address + 2, val & 0xff))
            return true;
        val >>>= 8;
        if (Paging.memWriteBChecked(address + 3, val & 0xff))
            return true;
        return false;
    }

    private static void writeP92(int port, int val, int iolen) {
        // Bit 0 = system reset (switch back to real mode)
        if ((val & 1) != 0)
            Support.exceptionExit("XMS: CPU reset via port 0x92 not supported.");
        memory.a20ControlPort = (byte) (val & ~2);
        A20Enable((val & 2) > 0);
    }

    private static int readP92(int port, int iolen) {
        return (0xff & memory.a20ControlPort) | (memory.a20Enabled ? 0x02 : 0);
    }

    public static void removeEMSPageFrame() {
        /* Setup rom at 0xe0000-0xf0000 */
        for (int ct = 0xe0; ct < 0xf0; ct++) {
            memory.pHandlers[ct] = romPageHandler;
        }
    }

    public static void preparePCJRCartRom() {
        /* Setup rom at 0xd0000-0xe0000 */
        for (int ct = 0xd0; ct < 0xe0; ct++) {
            memory.pHandlers[ct] = romPageHandler;
        }
    }

    public static byte[] getMemAlloc() {
        return allocMemory;
    }

    private static MemoryModule _mem;

    private static void shutdown(Section sec) {
        _mem.dispose();
        _mem = null;
    }

    public static void init(Section sec) throws WrongType {
        /* shutdown function */
        _mem = (new Memory()).new MemoryModule(sec);
        sec.addDestroyFunction(Memory::shutdown);
    }

    /*--------------------------- end MEMORYPartial -----------------------------*/
    /*--------------------------- begin MemoryModule -----------------------------*/
    public final class MemoryModule extends ModuleBase {
        private IOReadHandleObject ReadHandler = new IOReadHandleObject();
        private IOWriteHandleObject WriteHandler = new IOWriteHandleObject();

        public MemoryModule(Section configuration) throws WrongType {
            super(configuration);
            int i;
            SectionProperty section = (SectionProperty) (configuration);

            /* Setup the Physical Page Links */
            int memsize = section.getInt("memsize");

            if (memsize < 1)
                memsize = 1;
            /* max 63 to solve problems with certain xms handlers */
            if (memsize > MAX_MEMORY - 1) {
                Log.logMsg("Maximum memory size is %d MB", MAX_MEMORY - 1);
                memsize = MAX_MEMORY - 1;
            }
            if (memsize > SAFE_MEMORY - 1) {
                Log.logMsg("Memory sizes above %d MB are NOT recommended.", SAFE_MEMORY - 1);
                Log.logMsg("Stick with the default values unless you are absolutely certain.");
            }
            allocMemory = new byte[memsize * 1024 * 1024 + MemBase];
            if (allocMemory == null)
                Support.exceptionExit("Can't allocate main memory of %d MB", memsize);
            /*
             * Clear the memory, as new doesn't always give zeroed memory (Visual C debug mode). We
             * want zeroed memory though.
             */
            long memUse = memsize * 1024 * 1024;
            long memtotal = memUse + MemBase; // memsize * 1024 * 1024 ;
            if (memtotal <= Integer.MAX_VALUE) {
                Arrays.fill(allocMemory, 0, (int) memtotal, (byte) 0);
            } else {
                int cnt = (int) (memtotal / Integer.MAX_VALUE);
                int rem = (int) (memtotal % Integer.MAX_VALUE);
                for (int j = 0; j < cnt; j++)
                    Arrays.fill(allocMemory, 0, Integer.MAX_VALUE, (byte) 0);

                if (rem > 0)
                    Arrays.fill(allocMemory, 0, rem, (byte) 0);
            }
            // memory.pages = (memsize * 1024 * 1024) / 4096;
            memory.pages = (int) memUse / 4096;
            /* Allocate the data for the different page information blocks */
            memory.pHandlers = new PageHandler[memory.pages];
            memory.mHandles = new int[memory.pages];
            for (i = 0; i < memory.pages; i++) {
                memory.pHandlers[i] = ramPageHandler;
                memory.mHandles[i] = 0; // Set to 0 for memory allocation
            }
            /* Setup rom at 0xc0000-0xc8000 */
            for (i = 0xc0; i < 0xc8; i++) {
                memory.pHandlers[i] = romPageHandler;
            }
            /* Setup rom at 0xf0000-0x100000 */
            for (i = 0xf0; i < 0x100; i++) {
                memory.pHandlers[i] = romPageHandler;
            }
            if (DOSBox.Machine == DOSBox.MachineType.PCJR) {
                /* Setup cartridge rom at 0xe0000-0xf0000 */
                for (i = 0xe0; i < 0xf0; i++) {
                    memory.pHandlers[i] = romPageHandler;
                }
            }
            /* Reset some links */
            memory.links.used = 0;
            // A20 Line - PS/2 system control port A
            WriteHandler.install(0x92, Memory::writeP92, IO.IO_MB);
            ReadHandler.install(0x92, Memory::readP92, IO.IO_MB);
            A20Enable(false);
        }

        @Override
        protected void dispose(boolean disposing) {
            if (disposing) {
                // Free other state (managed objects).
            }

            // Free your own state (unmanaged objects).
            // Set large fields to null.
            allocMemory = null;
            memory.pHandlers = null;
            memory.mHandles = null;

            super.dispose(disposing);
        }
    }
    /*--------------------------- end MemoryModule -----------------------------*/

}
