package org.gutkyu.dosboxj.interrupt;

import org.gutkyu.dosboxj.cpu.*;
import org.gutkyu.dosboxj.hardware.io.IO;
import org.gutkyu.dosboxj.hardware.memory.*;
import org.gutkyu.dosboxj.hardware.memory.paging.*;
import org.gutkyu.dosboxj.interrupt.bios.BIOS;
import org.gutkyu.dosboxj.misc.*;
import org.gutkyu.dosboxj.util.*;
import org.gutkyu.dosboxj.misc.setup.*;
import org.gutkyu.dosboxj.misc.setup.Value.WrongType;
import org.gutkyu.dosboxj.DOSBox;
import org.gutkyu.dosboxj.dos.*;
import org.gutkyu.dosboxj.dos.system.device.*;

public final class EMS {

    private static final int EMM_PAGEFRAME = 0xE000;
    public static final int EMM_PAGEFRAME4K = ((EMM_PAGEFRAME * 16) / 4096);
    public static final short EMM_MAX_HANDLES = 200; /* 255 Max */
    private static final int EMM_PAGE_SIZE = (16 * 1024);
    private static final int EMM_MAX_PAGES = (32 * 1024 / 16);
    public static final byte EMM_MAX_PHYS = 4; /* 4 16kb pages in pageframe */

    public static final byte EMM_VERSION = 0x40;
    public static final byte EMM_MINOR_VERSION = 0x00;
    // private final byte EMM_MINOR_VERSION =0x30 // emm386 4.48
    public static final int GEMMIS_VERSION = 0x0001; // Version 1.0

    public static final int EMM_SYSTEM_HANDLE = 0x0000;
    public static final int NULL_HANDLE = 0xffff;
    public static final int NULL_PAGE = 0xffff;

    public static final byte ENABLE_VCPI = 1;
    public static final byte ENABLE_V86_STARTUP = 0;

    /* EMM errors */
    private static final int EMM_NO_ERROR = 0x00;
    private static final int EMM_SOFT_MAL = 0x80;
    private static final int EMM_HARD_MAL = 0x81;
    private static final int EMM_INVALID_HANDLE = 0x83;
    private static final int EMM_FUNC_NOSUP = 0x84;
    private static final int EMM_OUT_OF_HANDLES = 0x85;
    private static final int EMM_SAVEMAP_ERROR = 0x86;
    private static final int EMM_OUT_OF_PHYS = 0x87;
    private static final int EMM_OUT_OF_LOG = 0x88;
    private static final int EMM_ZERO_PAGES = 0x89;
    private static final int EMM_LOG_OUT_RANGE = 0x8a;
    private static final int EMM_ILL_PHYS = 0x8b;
    private static final int EMM_PAGE_MAP_SAVED = 0x8d;
    private static final int EMM_NO_SAVED_PAGE_MAP = 0x8e;
    private static final int EMM_INVALID_SUB = 0x8f;
    private static final int EMM_FEAT_NOSUP = 0x91;
    private static final int EMM_MOVE_OVLAP = 0x92;
    private static final int EMM_MOVE_OVLAPI = 0x97;
    private static final int EMM_NOT_FOUND = 0xa0;

    public static EMMHandle[] EMMHandles = new EMMHandle[EMM_MAX_HANDLES];
    public static EMMMapping[] EMMmappings = new EMMMapping[EMM_MAX_PHYS];
    public static EMMMapping[] EMMSegmentMappings = new EMMMapping[0x40];

    static {
        for (int i = 0; i < EMMHandles.length; i++)
            EMMHandles[i] = new EMMHandle();
        for (int i = 0; i < EMMmappings.length; i++)
            EMMmappings[i] = new EMMMapping();
        for (int i = 0; i < EMMSegmentMappings.length; i++)
            EMMSegmentMappings[i] = new EMMMapping();

    }

    protected EMS() {

    }

    public static class EMMMapping {
        // Offset(0)
        public int handle;// uint16
        // Offset(2)
        public int page;// uint16

        // (uint8, uint8)
        public void setHandle(int b0, int b1) {
            this.handle = b0 | b1 << 8;
        }

        // (uint8, uint8)
        public void setPage(int b2, int b3) {
            this.page = b2 | b3 << 8;
        }

        public static final byte Size = 4;
    }

    // 원래는 struct
    public static class EMMHandle {
        public int Pages;// uint16
        public int Mem;
        public CStringPt Name = CStringPt.create();
        public boolean SavedPageMap;
        public EMMMapping[] PageMap = new EMMMapping[EMM_MAX_PHYS];

    }

    private static final int ByteSizeOfEMMMappings = 0xff & (EMM_MAX_PHYS * EMMMapping.Size);

    public static EMMHandle getSystemHandle() {
        return EMMHandles[EMM_SYSTEM_HANDLE];
    }

    public static class VCPI {
        public boolean Enabled;
        public short EMSHandle;
        public int PMInterface;
        public int PrivateAREA;
        public int pic1Remapping, pic2Remapping;// uint8
    }

    public static VCPI vcpi = new VCPI();

    private static class MoveRegion {
        public int bytes;
        public int SrcType;// int8
        public int SrcHandle;// uint16
        public int SrcOffset;// uint16
        public int SrcPageSeg;// uint16
        public int DestType;// int8
        public int DestHandle;// uint16
        public int DestOffset;// uint16
        public int DestPageSeg;// uint16
    }

    // uint16
    private static int EMMGetFreePages() {
        int count = Memory.freeTotal() / 4;
        if (count > 0x7fff)
            count = 0x7fff;
        return count;
    }

    // short
    private static boolean validHandle(int handle) {
        if (handle >= EMM_MAX_HANDLES)
            return false;
        if (EMMHandles[handle].Pages == NULL_HANDLE)
            return false;
        return true;
    }

    private static int EMMAllocatedMemoryHandle = 0;

    // private static byte EMMAllocateMemory(short pages, RefU16Ret refDhandle,boolean
    // canAllocateZPages)
    private static int EMMAllocateMemory(int pages, boolean canAllocateZPages) {
        /* Check for 0 page allocation */
        if (pages == 0) {
            if (!canAllocateZPages)
                return EMM_ZERO_PAGES;
        }
        /* Check for enough free pages */
        if ((Memory.freeTotal() / 4) < pages) {
            return EMM_OUT_OF_LOG;
        }
        short handle = 1;
        /* Check for a free handle */
        while (EMMHandles[handle].Pages != NULL_HANDLE) {
            if (++handle >= EMM_MAX_HANDLES) {
                return EMM_OUT_OF_HANDLES;
            }
        }
        int mem = 0;
        if (pages != 0) {
            mem = Memory.allocatePages(pages * 4, false);
            if (mem == 0)
                Support.exceptionExit("EMS:Memory allocation failure");
        }
        EMMHandles[handle].Pages = pages;
        EMMHandles[handle].Mem = mem;
        /* Change handle only if there is no error. */
        EMMAllocatedMemoryHandle = handle;
        return EMM_NO_ERROR;
    }

    // byte(uint16)
    public static int EMMAllocateSystemHandle(int pages) {
        /* Check for enough free pages */
        if ((Memory.freeTotal() / 4) < pages) {
            return EMM_OUT_OF_LOG;
        }
        short handle = EMM_SYSTEM_HANDLE; // emm system handle (reserved for OS usage)
        /* Release memory if already allocated */
        if (EMMHandles[handle].Pages != NULL_HANDLE) {
            Memory.releasePages(EMMHandles[handle].Mem);
        }
        int mem = Memory.allocatePages(pages * 4, false);
        if (mem == 0)
            Support.exceptionExit("EMS:System handle memory allocation failure");
        EMMHandles[handle].Pages = pages;
        EMMHandles[handle].Mem = mem;
        return EMM_NO_ERROR;
    }

    // private static byte EMMReallocatePages(short handle, short pages) {
    private static int EMMReallocatePages(int handle, int pages) {
        /* Check for valid handle */
        if (!validHandle(handle))
            return EMM_INVALID_HANDLE;
        if (EMMHandles[handle].Pages != 0) {
            /* Check for enough pages */
            if (!Memory.tryReallocatePages(EMMHandles[handle].Mem, pages * 4, false))
                return EMM_OUT_OF_LOG;
            EMMHandles[handle].Mem = Memory.returnedReallocatePagesHandle;
        } else {
            int mem = Memory.allocatePages(pages * 4, false);
            if (mem == 0)
                Support.exceptionExit("EMS:Memory allocation failure during reallocation");
            EMMHandles[handle].Mem = mem;
        }
        /* Update size */
        EMMHandles[handle].Pages = pages;
        return EMM_NO_ERROR;
    }

    // int, short, short
    private static int EMMMapPage(int phys_page, int handle, int log_page) {
        // Log.LOG_MSG("EMS MapPage handle %d phys %d log
        // %d",handle,phys_page,log_page);
        /* Check for too high physical page */
        if (phys_page >= EMM_MAX_PHYS)
            return EMM_ILL_PHYS;

        /* unmapping doesn't need valid handle (as handle isn't used) */
        if (log_page == NULL_PAGE) {
            /* Unmapping */
            EMMmappings[phys_page].handle = NULL_HANDLE;
            EMMmappings[phys_page].page = NULL_PAGE;
            for (int i = 0; i < 4; i++)
                Paging.mapPage(EMM_PAGEFRAME4K + phys_page * 4 + i,
                        EMM_PAGEFRAME4K + phys_page * 4 + i);
            Paging.clearTLB();
            return EMM_NO_ERROR;
        }
        /* Check for valid handle */
        if (!validHandle(handle))
            return EMM_INVALID_HANDLE;

        if (log_page < EMMHandles[handle].Pages) {
            /* Mapping it is */
            EMMmappings[phys_page].handle = handle;
            EMMmappings[phys_page].page = log_page;

            int memh = Memory.nextHandleAt(EMMHandles[handle].Mem, log_page * 4);;
            for (int i = 0; i < 4; i++) {
                Paging.mapPage(EMM_PAGEFRAME4K + phys_page * 4 + i, memh);
                memh = Memory.nextHandle(memh);
            }
            Paging.clearTLB();
            return EMM_NO_ERROR;
        } else {
            /* Illegal logical page it is */
            return EMM_LOG_OUT_RANGE;
        }
    }

    // int, short, short
    private static int EMMMapSegment(int segment, int handle, int logPage) {
        // Log.LOG_MSG("EMS MapSegment handle %d segment %d log
        // %d",handle,segment,log_page);

        if (((segment >= 0xa000) && (segment < 0xb000))
                || ((segment >= EMM_PAGEFRAME - 0x1000) && (segment < EMM_PAGEFRAME + 0x1000))) {
            int tphysPage = (segment - EMM_PAGEFRAME) / (0x1000 / EMM_MAX_PHYS);

            /* unmapping doesn't need valid handle (as handle isn't used) */
            if (logPage == NULL_PAGE) {
                /* Unmapping */
                if ((tphysPage >= 0) && (tphysPage < EMM_MAX_PHYS)) {
                    EMMmappings[tphysPage].handle = NULL_HANDLE;
                    EMMmappings[tphysPage].page = NULL_PAGE;
                } else {
                    EMMSegmentMappings[segment >>> 10].handle = NULL_HANDLE;
                    EMMSegmentMappings[segment >>> 10].page = NULL_PAGE;
                }
                for (int i = 0; i < 4; i++)
                    Paging.mapPage(segment * 16 / 4096 + i, segment * 16 / 4096 + i);
                Paging.clearTLB();
                return EMM_NO_ERROR;
            }
            /* Check for valid handle */
            if (!validHandle(handle))
                return EMM_INVALID_HANDLE;

            if (logPage < EMMHandles[handle].Pages) {
                /* Mapping it is */
                if ((tphysPage >= 0) && (tphysPage < EMM_MAX_PHYS)) {
                    EMMmappings[tphysPage].handle = handle;
                    EMMmappings[tphysPage].page = logPage;
                } else {
                    EMMSegmentMappings[segment >>> 10].handle = handle;
                    EMMSegmentMappings[segment >>> 10].page = logPage;
                }

                int memh = Memory.nextHandleAt(EMMHandles[handle].Mem, logPage * 4);;
                for (int i = 0; i < 4; i++) {
                    Paging.mapPage(segment * 16 / 4096 + i, memh);
                    memh = Memory.nextHandle(memh);
                }
                Paging.clearTLB();
                return EMM_NO_ERROR;
            } else {
                /* Illegal logical page it is */
                return EMM_LOG_OUT_RANGE;
            }
        }

        return EMM_ILL_PHYS;
    }

    // private static byte EMMReleaseMemory(short handle) {
    private static int EMMReleaseMemory(int handle) {
        /* Check for valid handle */
        if (!validHandle(handle))
            return EMM_INVALID_HANDLE;

        // should check for saved_page_map flag here, returning an error if it's true
        // as apps are required to restore the pagemap beforehand; to be checked
        // if (emm_handles[handle].saved_page_map) return EMM_SAVEMAP_ERROR;

        if (EMMHandles[handle].Pages != 0) {
            Memory.releasePages(EMMHandles[handle].Mem);
        }
        /* Reset handle */
        EMMHandles[handle].Mem = 0;
        if (handle == 0) {
            EMMHandles[handle].Pages = 0; // OS handle is NEVER deallocated
        } else {
            EMMHandles[handle].Pages = NULL_HANDLE;
        }
        EMMHandles[handle].SavedPageMap = false;
        CStringPt.clear(EMMHandles[handle].Name, 0, 8);
        return EMM_NO_ERROR;
    }

    // private static byte EMMSavePageMap(short handle) {
    private static int EMMSavePageMap(int handle) {
        /* Check for valid handle */
        if (handle >= EMM_MAX_HANDLES || EMMHandles[handle].Pages == NULL_HANDLE) {
            if (handle != 0)
                return EMM_INVALID_HANDLE;
        }
        /* Check for previous save */
        if (EMMHandles[handle].SavedPageMap)
            return EMM_PAGE_MAP_SAVED;
        /* Copy the mappings over */
        for (int i = 0; i < EMM_MAX_PHYS; i++) {
            EMMHandles[handle].PageMap[i].page = EMMmappings[i].page;
            EMMHandles[handle].PageMap[i].handle = EMMmappings[i].handle;
        }
        EMMHandles[handle].SavedPageMap = true;
        return EMM_NO_ERROR;
    }

    private static byte EMMRestoreMappingTable() {
        int result = 0;
        /* Move through the mappings table and setup mapping accordingly */
        for (int i = 0; i < 0x40; i++) {
            /* Skip the pageframe */
            if ((i >= EMM_PAGEFRAME / 0x400) && (i < (EMM_PAGEFRAME / 0x400) + EMM_MAX_PHYS))
                continue;
            result = EMMMapSegment(i << 10, EMMSegmentMappings[i].handle,
                    EMMSegmentMappings[i].page);
        }
        for (int i = 0; i < EMM_MAX_PHYS; i++) {
            result = EMMMapPage(i, EMMmappings[i].handle, EMMmappings[i].page);
        }
        return EMM_NO_ERROR;
    }

    // private static byte EMMRestorePageMap(short handle) {
    private static int EMMRestorePageMap(int handle) {
        /* Check for valid handle */
        if (handle >= EMM_MAX_HANDLES || EMMHandles[handle].Pages == NULL_HANDLE) {
            if (handle != 0)
                return EMM_INVALID_HANDLE;
        }
        /* Check for previous save */
        if (!EMMHandles[handle].SavedPageMap)
            return EMM_NO_SAVED_PAGE_MAP;
        /* Restore the mappings */
        EMMHandles[handle].SavedPageMap = false;
        for (int i = 0; i < EMM_MAX_PHYS; i++) {
            EMMmappings[i].page = EMMHandles[handle].PageMap[i].page;
            EMMmappings[i].handle = EMMHandles[handle].PageMap[i].handle;
        }
        return EMMRestoreMappingTable();
    }

    private static int returnedPagesForAllHandles = 0;

    private static byte tryPagesForAllHandles(int table) {
        int handles = 0;
        for (int i = 0; i < EMM_MAX_HANDLES; i++) {
            if (EMMHandles[i].Pages != NULL_HANDLE) {
                handles++;
                Memory.writeW(table, i);
                Memory.writeW(table + 2, EMMHandles[i].Pages);
                table += 4;
            }
        }
        returnedPagesForAllHandles = handles;
        return EMM_NO_ERROR;
    }

    private static int EMMPartialPageMapping() {
        int list, data;
        int count;
        switch (Register.getRegAL()) {
            case 0x00: /* Save Partial Page Map */
                list = Register.segPhys(Register.SEG_NAME_DS) + Register.getRegSI();
                data = Register.segPhys(Register.SEG_NAME_ES) + Register.getRegDI();
                count = Memory.readW(list);
                list += 2;
                Memory.writeW(data, count);
                data += 2;
                for (; count > 0; count--) {
                    int segment = Memory.readW(list);
                    list += 2;
                    if ((segment >= EMM_PAGEFRAME) && (segment < EMM_PAGEFRAME + 0x1000)) {
                        int page = 0xffff & ((segment - EMM_PAGEFRAME) / (EMM_PAGE_SIZE >>> 4));
                        Memory.writeW(data, segment);
                        data += 2;
                        Memory.blockWrite(data, EMMmappings[page]);
                        data += EMMMapping.Size;
                    } else if (((segment >= EMM_PAGEFRAME - 0x1000) && (segment < EMM_PAGEFRAME))
                            || ((segment >= 0xa000) && (segment < 0xb000))) {
                        Memory.writeW(data, segment);
                        data += 2;
                        Memory.blockWrite(data, EMMSegmentMappings[segment >>> 10]);
                        data += EMMMapping.Size;
                    } else {
                        return EMM_ILL_PHYS;
                    }
                }
                break;
            case 0x01: /* Restore Partial Page Map */
                data = Register.segPhys(Register.SEG_NAME_DS) + Register.getRegSI();
                count = Memory.readW(data);
                data += 2;
                for (; count > 0; count--) {
                    int segment = Memory.readW(data);
                    data += 2;
                    if ((segment >= EMM_PAGEFRAME) && (segment < EMM_PAGEFRAME + 0x1000)) {
                        int page = 0xffff & ((segment - EMM_PAGEFRAME) / (EMM_PAGE_SIZE >>> 4));
                        Memory.blockRead(data, EMMmappings, page, 1);
                    } else if (((segment >= EMM_PAGEFRAME - 0x1000) && (segment < EMM_PAGEFRAME))
                            || ((segment >= 0xa000) && (segment < 0xb000))) {
                        Memory.blockRead(data, EMMSegmentMappings, segment >>> 10, 1);
                    } else {
                        return EMM_ILL_PHYS;
                    }
                    data += EMMMapping.Size;
                }
                return EMMRestoreMappingTable();
            // break;
            case 0x02: /* Get Partial Page Map Array Size */
                Register.setRegAL(0xff & (2 + Register.getRegBX() * (2 + EMMMapping.Size)));
                break;
            default:
                Log.logging(Log.LogTypes.MISC, Log.LogServerities.Error,
                        "EMS:Call %2X Subfunction %2X not supported", Register.getRegAH(),
                        Register.getRegAL());
                return EMM_FUNC_NOSUP;
        }
        return EMM_NO_ERROR;
    }

    private static int searchHandleName() {
        CStringPt name = CStringPt.create(9);
        short handle = 0;
        int data;
        switch (Register.getRegAL()) {
            case 0x00: /* Get all handle names */
                Register.setRegAL(0);
                data = Register.segPhys(Register.SEG_NAME_ES) + Register.getRegDI();
                for (handle = 0; handle < EMM_MAX_HANDLES; handle++) {
                    if (EMMHandles[handle].Pages != NULL_HANDLE) {
                        Register.setRegAL(Register.getRegAL() + 1);
                        Memory.writeW(data, handle);
                        Memory.blockWrite(data + 2, EMMHandles[handle].Name.getAsciiBytes(), 0, 8);
                        data += 10;
                    }
                }
                break;
            case 0x01: /* Search for a handle name */
                Memory.strCopy(Register.segPhys(Register.SEG_NAME_DS) + Register.getRegSI(), name,
                        8);
                name.set(8, (char) 0);
                for (handle = 0; handle < EMM_MAX_HANDLES; handle++) {
                    if (EMMHandles[handle].Pages != NULL_HANDLE) {
                        if (name.equals(EMMHandles[handle].Name)) {
                            Register.setRegDX(handle);
                            return EMM_NO_ERROR;
                        }
                    }
                }
                return EMM_NOT_FOUND;
            // break;
            case 0x02: /* Get Total number of handles */
                Register.setRegBX(EMM_MAX_HANDLES);
                break;
            default:
                Log.logging(Log.LogTypes.MISC, Log.LogServerities.Error,
                        "EMS:Call %2X Subfunction %2X not supported", Register.getRegAH(),
                        Register.getRegAL());
                return EMM_INVALID_SUB;
        }
        return EMM_NO_ERROR;
    }

    private static int getSetHandleName() {
        int handle = Register.getRegDX();
        switch (Register.getRegAL()) {
            case 0x00: /* Get Handle Name */
                if (handle >= EMM_MAX_HANDLES || EMMHandles[handle].Pages == NULL_HANDLE)
                    return EMM_INVALID_HANDLE;
                Memory.blockWrite(Register.segPhys(Register.SEG_NAME_ES) + Register.getRegDI(),
                        EMMHandles[handle].Name.getAsciiBytes(), 0, 8);
                break;
            case 0x01: /* Set Handle Name */
                if (handle >= EMM_MAX_HANDLES || EMMHandles[handle].Pages == NULL_HANDLE)
                    return EMM_INVALID_HANDLE;
                Memory.blockRead(Register.segPhys(Register.SEG_NAME_ES) + Register.getRegDI(),
                        EMMHandles[handle].Name, 8);
                break;
            default:
                Log.logging(Log.LogTypes.MISC, Log.LogServerities.Error,
                        "EMS:Call %2X Subfunction %2X not supported", Register.getRegAH(),
                        Register.getRegAL());
                return EMM_INVALID_SUB;
        }
        return EMM_NO_ERROR;

    }

    private static void loadMoveRegion(int data, MoveRegion region) {
        region.bytes = Memory.readD(data + 0x0);

        region.SrcType = Memory.readB(data + 0x4);
        region.SrcHandle = Memory.readW(data + 0x5);
        region.SrcOffset = Memory.readW(data + 0x7);
        region.SrcPageSeg = Memory.readW(data + 0x9);

        region.DestType = Memory.readB(data + 0xb);
        region.DestHandle = Memory.readW(data + 0xc);
        region.DestOffset = Memory.readW(data + 0xe);
        region.DestPageSeg = Memory.readW(data + 0x10);
    }

    private static int memoryRegion() {
        MoveRegion region = new MoveRegion();
        byte[] bufSrc = new byte[Paging.MEM_PAGE_SIZE];
        byte[] BufDest = new byte[Paging.MEM_PAGE_SIZE];
        if (Register.getRegAL() > 1) {
            Log.logging(Log.LogTypes.MISC, Log.LogServerities.Error,
                    "EMS:Call %2X Subfunction %2X not supported", Register.getRegAH(),
                    Register.getRegAL());
            return EMM_FUNC_NOSUP;
        }
        loadMoveRegion(Register.segPhys(Register.SEG_NAME_DS) + Register.getRegSI(), region);
        /* Parse the region for information */
        int srcMem = 0, destMem = 0;
        int srcHandle = 0, destHandle = 0;
        int srcOff = 0, destOff = 0;
        int srcRemain = 0, destRemain = 0;
        if (region.SrcType == 0) {
            srcMem = region.SrcPageSeg * 16 + region.SrcOffset;
        } else {
            if (!validHandle(region.SrcHandle))
                return EMM_INVALID_HANDLE;
            if ((EMMHandles[region.SrcHandle].Pages
                    * EMM_PAGE_SIZE) < ((region.SrcPageSeg * EMM_PAGE_SIZE) + region.SrcOffset
                            + region.bytes))
                return EMM_LOG_OUT_RANGE;
            srcHandle = EMMHandles[region.SrcHandle].Mem;
            int pages = region.SrcPageSeg * 4 + (region.SrcOffset / Paging.MEM_PAGE_SIZE);
            for (; pages > 0; pages--)
                srcHandle = Memory.nextHandle(srcHandle);
            srcOff = region.SrcOffset & (Paging.MEM_PAGE_SIZE - 1);
            srcRemain = Paging.MEM_PAGE_SIZE - srcOff;
        }
        if (region.DestType == 0) {
            destMem = region.DestPageSeg * 16 + region.DestOffset;
        } else {
            if (!validHandle(region.DestHandle))
                return EMM_INVALID_HANDLE;
            if (EMMHandles[region.DestHandle].Pages
                    * EMM_PAGE_SIZE < (region.DestPageSeg * EMM_PAGE_SIZE) + region.DestOffset
                            + region.bytes)
                return EMM_LOG_OUT_RANGE;
            destHandle = EMMHandles[region.DestHandle].Mem;
            int pages = region.DestPageSeg * 4 + (region.DestOffset / Paging.MEM_PAGE_SIZE);
            for (; pages > 0; pages--)
                destHandle = Memory.nextHandle(destHandle);
            destOff = region.DestOffset & (Paging.MEM_PAGE_SIZE - 1);
            destRemain = Paging.MEM_PAGE_SIZE - destOff;
        }
        int toRead;
        while (region.bytes > 0) {
            if (region.bytes > Paging.MEM_PAGE_SIZE)
                toRead = Paging.MEM_PAGE_SIZE;
            else
                toRead = region.bytes;
            /* Read from the source */
            if (region.SrcType == 0) {
                Memory.blockRead(srcMem, bufSrc, 0, toRead);
            } else {
                if (toRead < srcRemain) {
                    Memory.blockRead((srcHandle * Paging.MEM_PAGE_SIZE) + srcOff, bufSrc, 0,
                            toRead);
                } else {
                    Memory.blockRead((srcHandle * Paging.MEM_PAGE_SIZE) + srcOff, bufSrc, 0,
                            srcRemain);
                    Memory.blockRead((Memory.nextHandle(srcHandle) * Paging.MEM_PAGE_SIZE), bufSrc,
                            srcRemain, toRead - srcRemain);
                }
            }
            /* Check for a move */
            if (Register.getRegAL() == 1) {
                /* Read from the destination */
                if (region.DestType == 0) {
                    Memory.blockRead(destMem, BufDest, 0, toRead);
                } else {
                    if (toRead < destRemain) {
                        Memory.blockRead((destHandle * Paging.MEM_PAGE_SIZE) + destOff, BufDest, 0,
                                toRead);
                    } else {
                        Memory.blockRead((destHandle * Paging.MEM_PAGE_SIZE) + destOff, BufDest, 0,
                                destRemain);
                        Memory.blockRead((Memory.nextHandle(destHandle) * Paging.MEM_PAGE_SIZE),
                                BufDest, destRemain, toRead - destRemain);
                    }
                }
                /* Write to the source */
                if (region.SrcType == 0) {
                    Memory.blockWrite(srcMem, BufDest, 0, toRead);
                } else {
                    if (toRead < srcRemain) {
                        Memory.blockWrite((srcHandle * Paging.MEM_PAGE_SIZE) + srcOff, BufDest, 0,
                                toRead);
                    } else {
                        Memory.blockWrite((srcHandle * Paging.MEM_PAGE_SIZE) + srcOff, BufDest, 0,
                                srcRemain);
                        Memory.blockWrite((Memory.nextHandle(srcHandle) * Paging.MEM_PAGE_SIZE),
                                BufDest, srcRemain, toRead - srcRemain);
                    }
                }
            }
            /* Write to the destination */
            if (region.DestType == 0) {
                Memory.blockWrite(destMem, bufSrc, 0, toRead);
            } else {
                if (toRead < destRemain) {
                    Memory.blockWrite((destHandle * Paging.MEM_PAGE_SIZE) + destOff, bufSrc, 0,
                            toRead);
                } else {
                    Memory.blockWrite((destHandle * Paging.MEM_PAGE_SIZE) + destOff, bufSrc, 0,
                            destRemain);
                    Memory.blockWrite((Memory.nextHandle(destHandle) * Paging.MEM_PAGE_SIZE),
                            bufSrc, destRemain, toRead - destRemain);
                }
            }
            /* Advance the pointers */
            if (region.SrcType == 0)
                srcMem += toRead;
            else
                srcHandle = Memory.nextHandle(srcHandle);
            if (region.DestType == 0)
                destMem += toRead;
            else
                destHandle = Memory.nextHandle(destHandle);
            region.bytes -= toRead;
        }
        return EMM_NO_ERROR;
    }

    public static int INT67Handler() {
        int i;
        switch (Register.getRegAH()) {
            case 0x40: /* Get Status */
                Register.setRegAH(EMM_NO_ERROR);
                break;
            case 0x41: /* Get PageFrame Segment */
                Register.setRegBX(EMM_PAGEFRAME);
                Register.setRegAH(EMM_NO_ERROR);
                break;
            case 0x42: /* Get number of pages */
                // Not entirely correct but okay
                Register.setRegDX(0xffff & (Memory.totalPages() / 4));
                Register.setRegBX(EMMGetFreePages());
                Register.setRegAH(EMM_NO_ERROR);
                break;
            case 0x43: /* Get Handle and Allocate Pages */
            {
                Register.setRegAH(EMMAllocateMemory(Register.getRegBX(), false));
                Register.setRegDX(EMMAllocatedMemoryHandle);
            }
                break;
            case 0x44: /* Map Expanded Memory Page */
                Register.setRegAH(
                        EMMMapPage(Register.getRegAL(), Register.getRegDX(), Register.getRegBX()));
                break;
            case 0x45: /* Release handle and free pages */
                Register.setRegAH(EMMReleaseMemory(Register.getRegDX()));
                break;
            case 0x46: /* Get EMM Version */
                Register.setRegAH(EMM_NO_ERROR);
                Register.setRegAL(EMM_VERSION);
                break;
            case 0x47: /* Save Page Map */
                Register.setRegAH(EMMSavePageMap(Register.getRegDX()));
                break;
            case 0x48: /* Restore Page Map */
                Register.setRegAH(EMMRestorePageMap(Register.getRegDX()));
                break;
            case 0x4b: /* Get Handle Count */
                Register.setRegBX(0);
                for (i = 0; i < EMM_MAX_HANDLES; i++)
                    if (EMMHandles[i].Pages != NULL_HANDLE)
                        Register.setRegBX(Register.getRegBX() + 1);
                Register.setRegAH(EMM_NO_ERROR);
                break;
            case 0x4c: /* Get Pages for one Handle */
                if (!validHandle(Register.getRegDX())) {
                    Register.setRegAH(EMM_INVALID_HANDLE);
                    break;
                }
                Register.setRegBX(EMMHandles[Register.getRegDX()].Pages);
                Register.setRegAH(EMM_NO_ERROR);
                break;
            case 0x4d: /* Get Pages for all Handles */
            {
                Register.setRegAH(tryPagesForAllHandles(
                        Register.segPhys(Register.SEG_NAME_ES) + Register.getRegDI()));
                Register.setRegBX(returnedPagesForAllHandles);
            }
                break;
            case 0x4e: /* Save/Restore Page Map */
                switch (Register.getRegAL()) {
                    case 0x00: /* Save Page Map */
                    {
                        Memory.blockWrite(
                                Register.segPhys(Register.SEG_NAME_ES) + Register.getRegDI(),
                                EMMmappings);
                        Register.setRegAH(EMM_NO_ERROR);
                    }
                        break;
                    case 0x01: /* Restore Page Map */
                        Memory.blockRead(
                                Register.segPhys(Register.SEG_NAME_DS) + Register.getRegSI(),
                                EMMmappings);
                        Register.setRegAH(EMMRestoreMappingTable());
                        break;
                    case 0x02: /* Save and Restore Page Map */
                        Memory.blockWrite(
                                Register.segPhys(Register.SEG_NAME_ES) + Register.getRegDI(),
                                EMMmappings);
                        Memory.blockRead(
                                Register.segPhys(Register.SEG_NAME_DS) + Register.getRegSI(),
                                EMMmappings);
                        Register.setRegAH(EMMRestoreMappingTable());
                        break;
                    case 0x03: /* Get Page Map Array Size */
                        Register.setRegAL(ByteSizeOfEMMMappings);
                        Register.setRegAH(EMM_NO_ERROR);
                        break;
                    default:
                        Log.logging(Log.LogTypes.MISC, Log.LogServerities.Error,
                                "EMS:Call %2X Subfunction %2X not supported", Register.getRegAH(),
                                Register.getRegAL());
                        Register.setRegAH(EMM_INVALID_SUB);
                        break;
                }
                break;
            case 0x4f: /* Save/Restore Partial Page Map */
                Register.setRegAH(EMMPartialPageMapping());
                break;
            case 0x50: /* Map/Unmap multiple handle pages */
                Register.setRegAH(EMM_NO_ERROR);
                switch (Register.getRegAL()) {
                    case 0x00: // use physical page numbers
                    {
                        int data = Register.segPhys(Register.SEG_NAME_DS) + Register.getRegSI();
                        int logPage, physPage;
                        for (int j = 0; j < Register.getRegCX(); j++) {
                            logPage = Memory.readW(data);
                            data += 2;
                            physPage = Memory.readW(data);
                            data += 2;
                            Register.setRegAH(EMMMapPage(physPage, Register.getRegDX(), logPage));
                            if (Register.getRegAH() != EMM_NO_ERROR)
                                break;
                        }
                    }
                        break;
                    case 0x01: // use segment address
                    {
                        int data = Register.segPhys(Register.SEG_NAME_DS) + Register.getRegSI();
                        int logPage;
                        for (int j = 0; j < Register.getRegCX(); j++) {
                            logPage = Memory.readW(data);
                            data += 2;
                            Register.setRegAH(EMMMapSegment(Memory.readW(data), Register.getRegDX(),
                                    logPage));
                            data += 2;
                            if (Register.getRegAH() != EMM_NO_ERROR)
                                break;
                        }
                    }
                        break;
                    default:
                        Log.logging(Log.LogTypes.MISC, Log.LogServerities.Error,
                                "EMS:Call %2X Subfunction %2X not supported", Register.getRegAH(),
                                Register.getRegAL());
                        Register.setRegAH(EMM_INVALID_SUB);
                        break;
                }
                break;
            case 0x51: /* Reallocate Pages */
            {
                int regbx = Register.getRegBX();
                Register.setRegAH(EMMReallocatePages(Register.getRegDX(), regbx));
                Register.setRegBX(regbx);
            }
                break;
            case 0x53: // Set/Get Handlename
                Register.setRegAH(getSetHandleName());
                break;
            case 0x54: /* Handle Functions */
                Register.setRegAH(searchHandleName());
                break;
            case 0x57: /* Memory region */
                Register.setRegAH(memoryRegion());
                if (Register.getRegAH() != 0)
                    Log.logging(Log.LogTypes.MISC, Log.LogServerities.Error,
                            "EMS:Function 57 move failed");
                break;
            case 0x58: // Get mappable physical array address array
                if (Register.getRegAL() == 0x00) {
                    int data = Register.segPhys(Register.SEG_NAME_ES) + Register.getRegDI();
                    short step = 0x1000 / EMM_MAX_PHYS;
                    for (short j = 0; j < EMM_MAX_PHYS; j++) {
                        Memory.writeW(data, EMM_PAGEFRAME + step * j);
                        data += 2;
                        Memory.writeW(data, j);
                        data += 2;
                    }
                }
                // Set number of pages
                Register.setRegCX(EMM_MAX_PHYS);
                Register.setRegAH(EMM_NO_ERROR);
                break;
            case 0x5A: /* Allocate standard/raw Pages */
                if (Register.getRegAL() <= 0x01) {
                    // can allocate 0 pages
                    Register.setRegAH(EMMAllocateMemory(Register.getRegBX(), true));
                    Register.setRegDX(EMMAllocatedMemoryHandle);
                } else {
                    Log.logging(Log.LogTypes.MISC, Log.LogServerities.Error,
                            "EMS:Call 5A subfct %2X not supported", Register.getRegAL());
                    Register.setRegAH(EMM_INVALID_SUB);
                }
                break;
            case 0xDE: /* VCPI Functions */
                if (!vcpi.Enabled) {
                    Log.logging(Log.LogTypes.MISC, Log.LogServerities.Error,
                            "EMS:VCPI Call %2X not supported", Register.getRegAL());
                    Register.setRegAH(EMM_FUNC_NOSUP);
                } else {
                    switch (Register.getRegAL()) {
                        case 0x00: /* VCPI Installation Check */
                            if (((Register.getRegCX() == 0) && (Register.getRegDI() == 0x0012))
                                    || (CPU.Block.PMode
                                            && (Register.Flags & Register.FlagVM) != 0)) {
                                /* JEMM detected or already in v86 mode */
                                Register.setRegAH(EMM_NO_ERROR);
                                Register.setRegBX(0x100);
                            } else {
                                Register.setRegAH(EMM_FUNC_NOSUP);
                            }
                            break;
                        case 0x01: { /* VCPI Get Protected Mode Interface */
                            short ct;
                            /* Set up page table buffer */
                            for (ct = 0; ct < 0xff; ct++) {
                                Memory.realWriteB(Register.segValue(Register.SEG_NAME_ES),
                                        Register.getRegDI() + ct * 4 + 0x00, 0x67); // access bits
                                Memory.realWriteW(Register.segValue(Register.SEG_NAME_ES),
                                        Register.getRegDI() + ct * 4 + 0x01, ct * 0x10); // mapping
                                Memory.realWriteB(Register.segValue(Register.SEG_NAME_ES),
                                        Register.getRegDI() + ct * 4 + 0x03, 0x00);
                            }
                            for (ct = 0xff; ct < 0x100; ct++) {
                                Memory.realWriteB(Register.segValue(Register.SEG_NAME_ES),
                                        Register.getRegDI() + ct * 4 + 0x00, 0x67); // access bits
                                Memory.realWriteW(Register.segValue(Register.SEG_NAME_ES),
                                        Register.getRegDI() + ct * 4 + 0x01,
                                        (ct - 0xff) * 0x10 + 0x1100); // mapping
                                Memory.realWriteB(Register.segValue(Register.SEG_NAME_ES),
                                        Register.getRegDI() + ct * 4 + 0x03, 0x00);
                            }
                            /* adjust paging entries for page frame (if mapped) */
                            for (ct = 0; ct < 4; ct++) {
                                int handle = EMMmappings[ct].handle;
                                if (handle != 0xffff) {
                                    int memh = Memory.nextHandleAt(EMMHandles[handle].Mem,
                                            EMMmappings[ct].page * 4);
                                    int entry_addr = Register.getRegDI() + (EMM_PAGEFRAME >>> 6)
                                            + (ct * 0x10);
                                    // mapping of 1/4 of page
                                    Memory.realWriteW(Register.segValue(Register.SEG_NAME_ES),
                                            entry_addr + 0x00 + 0x01, (memh + 0) * 0x10);
                                    // mapping of 2/4 of page
                                    Memory.realWriteW(Register.segValue(Register.SEG_NAME_ES),
                                            entry_addr + 0x04 + 0x01, (memh + 1) * 0x10);
                                    // mapping of 3/4 of page
                                    Memory.realWriteW(Register.segValue(Register.SEG_NAME_ES),
                                            entry_addr + 0x08 + 0x01, (memh + 2) * 0x10);
                                    // mapping of 4/4 of page
                                    Memory.realWriteW(Register.segValue(Register.SEG_NAME_ES),
                                            0xffff & (entry_addr + 0x0c + 0x01),
                                            0xffff & ((memh + 3) * 0x10));
                                }
                            }
                            // advance pointer by 0x100*4
                            Register.setRegDI(Register.getRegDI() + 0x400);

                            /* Set up three descriptor table entries */
                            int cbseg_low = (Callback.getBase() & 0xffff) << 16;
                            int cbseg_high = (Callback.getBase() & 0x1f0000) >>> 16;
                            /* Descriptor 1 (code segment, callback segment) */
                            Memory.realWriteD(Register.segValue(Register.SEG_NAME_DS),
                                    0xffff & (Register.getRegSI() + 0x00), 0x0000ffff | cbseg_low);
                            Memory.realWriteD(Register.segValue(Register.SEG_NAME_DS),
                                    0xffff & (Register.getRegSI() + 0x04), 0x00009a00 | cbseg_high);
                            /* Descriptor 2 (data segment, full access) */
                            Memory.realWriteD(Register.segValue(Register.SEG_NAME_DS),
                                    0xffff & (Register.getRegSI() + 0x08), 0x0000ffff);
                            Memory.realWriteD(Register.segValue(Register.SEG_NAME_DS),
                                    0xffff & (Register.getRegSI() + 0x0c), 0x00009200);
                            /* Descriptor 3 (full access) */
                            Memory.realWriteD(Register.segValue(Register.SEG_NAME_DS),
                                    0xffff & (Register.getRegSI() + 0x10), 0x0000ffff);
                            Memory.realWriteD(Register.segValue(Register.SEG_NAME_DS),
                                    0xffff & (Register.getRegSI() + 0x14), 0x00009200);

                            Register.setRegEBX((vcpi.PMInterface & 0xffff));
                            Register.setRegAH(EMM_NO_ERROR);
                            break;
                        }
                        case 0x02: /* VCPI Maximum Physical Address */
                            Register.setRegEDX(
                                    ((Memory.totalPages() * Memory.MEM_PAGESIZE) - 1) & 0xfffff000);
                            Register.setRegAH(EMM_NO_ERROR);
                            break;
                        case 0x03: /* VCPI Get Number of Free Pages */
                            Register.setRegEDX(Memory.freeTotal());
                            Register.setRegAH(EMM_NO_ERROR);
                            break;
                        case 0x04: { /* VCPI Allocate one Page */
                            int mem = Memory.allocatePages(1, false);
                            if (mem != 0) {
                                Register.setRegEDX(mem << 12);
                                Register.setRegAH(EMM_NO_ERROR);
                            } else {
                                Register.setRegAH(EMM_OUT_OF_LOG);
                            }
                            break;
                        }
                        case 0x05: /* VCPI Free Page */
                            Memory.releasePages(Register.getRegEDX() >>> 12);
                            Register.setRegAH(EMM_NO_ERROR);
                            break;
                        case 0x06: { /* VCPI Get Physical Address of Page in 1st MB */
                            if (((Register.getRegCX() << 8) >= EMM_PAGEFRAME)
                                    && ((Register.getRegCX() << 8) < EMM_PAGEFRAME + 0x1000)) {
                                /*
                                 * Page is in Pageframe, so check what EMS-page it is and return the
                                 * physical address
                                 */
                                byte physPage;
                                int memSeg = Register.getRegCX() << 8;
                                if (memSeg < EMM_PAGEFRAME + 0x400)
                                    physPage = 0;
                                else if (memSeg < EMM_PAGEFRAME + 0x800)
                                    physPage = 1;
                                else if (memSeg < EMM_PAGEFRAME + 0xc00)
                                    physPage = 2;
                                else
                                    physPage = 3;
                                int handle = EMMmappings[physPage].handle;
                                if (handle == 0xffff) {
                                    Register.setRegAH(EMM_ILL_PHYS);
                                    break;
                                } else {
                                    int memh = Memory.nextHandleAt(EMMHandles[handle].Mem,
                                            EMMmappings[physPage].page * 4);
                                    Register.setRegEDX((memh + (Register.getRegCX() & 3)) << 12);
                                }
                            } else {
                                /* Page not in Pageframe, so just translate into physical address */
                                Register.setRegEDX(Register.getRegCX() << 12);
                            }

                            Register.setRegAH(EMM_NO_ERROR);
                        }
                            break;
                        case 0x0a: /* VCPI Get PIC Vector Mappings */
                            Register.setRegBX(vcpi.pic1Remapping); // master PIC
                            Register.setRegCX(vcpi.pic2Remapping); // slave PIC
                            Register.setRegAH(EMM_NO_ERROR);
                            break;
                        case 0x0b: /* VCPI Set PIC Vector Mappings */
                            Register.Flags &= (~Register.FlagIF);
                            vcpi.pic1Remapping = Register.getRegBX() & 0xff;
                            vcpi.pic2Remapping = Register.getRegCX() & 0xff;
                            Register.setRegAH(EMM_NO_ERROR);
                            break;
                        case 0x0c: { /* VCPI Switch from V86 to Protected Mode */
                            Register.Flags &= (~Register.FlagIF);
                            CPU.Block.CPL = 0;

                            /* Read data from ESI (linear address) */
                            int newCR3 = Memory.readD(Register.getRegESI());
                            int newGDTAddr = Memory.readD(Register.getRegESI() + 4);
                            int newIDTAddr = Memory.readD(Register.getRegESI() + 8);
                            int newIDT = Memory.readW(Register.getRegESI() + 0x0c);
                            int newTr = Memory.readW(Register.getRegESI() + 0x0e);
                            int newEIP = Memory.readD(Register.getRegESI() + 0x10);
                            int newCS = Memory.readW(Register.getRegESI() + 0x14);

                            /* Get GDT and IDT entries */
                            int newGDTLimit = Memory.readW(newGDTAddr);
                            int newGDTBase = Memory.readD(newGDTAddr + 2);
                            int newIDTLimit = Memory.readW(newIDTAddr);
                            int newIDTBase = Memory.readD(newIDTAddr + 2);

                            /* Switch to protected mode, paging enabled if necessary */
                            int newCR0 = CPU.getCRX(0) | 1;
                            if (newCR3 != 0)
                                newCR0 |= 0x80000000;
                            CPU.setCRX(0, newCR0);
                            CPU.setCRX(3, newCR3);

                            int tbaddr = newGDTBase + (newTr & 0xfff8) + 5;
                            int tb = Memory.readB(tbaddr);
                            Memory.writeB(tbaddr, tb & 0xfd);

                            /* Load tables and initialize segment registers */
                            CPU.lgdt(newGDTLimit, newGDTBase);
                            CPU.lidt(newIDTLimit, newIDTBase);
                            if (CPU.lldt(newIDT))
                                Log.logMsg("VCPI:Could not load LDT with %x", newIDT);
                            if (CPU.ltr(newTr))
                                Log.logMsg("VCPI:Could not load TR with %x", newTr);

                            CPU.setSegGeneral(Register.SEG_NAME_DS, 0);
                            CPU.setSegGeneral(Register.SEG_NAME_ES, 0);
                            CPU.setSegGeneral(Register.SEG_NAME_FS, 0);
                            CPU.setSegGeneral(Register.SEG_NAME_GS, 0);

                            // MEMORY.MEM_A20_Enable(true);

                            /* Switch to protected mode */
                            Register.Flags &= (~(Register.FlagVM | Register.FlagNT));
                            Register.Flags |= 0x3000;
                            CPU.jmp(true, newCS, newEIP, 0);
                        }
                            break;
                        default:
                            Log.logging(Log.LogTypes.MISC, Log.LogServerities.Error,
                                    "EMS:VCPI Call %x not supported", Register.getRegAX());
                            Register.setRegAH(EMM_FUNC_NOSUP);
                            break;
                    }
                }
                break;
            default:
                Log.logging(Log.LogTypes.MISC, Log.LogServerities.Error,
                        "EMS:Call %2X not supported", Register.getRegAH());
                Register.setRegAH(EMM_FUNC_NOSUP);
                break;
        }
        return Callback.ReturnTypeNone;
    }

    public static int VCPIPageModeHandler() {
        // Log.LOG_MSG("VCPI PMODE handler, function %x",regsModule.reg_ax);
        switch (Register.getRegAX()) {
            case 0xDE03: /* VCPI Get Number of Free Pages */
                Register.setRegEDX(Memory.freeTotal());
                Register.setRegAH(EMM_NO_ERROR);
                break;
            case 0xDE04: { /* VCPI Allocate one Page */
                int mem = Memory.allocatePages(1, false);
                if (mem != 0) {
                    Register.setRegEDX(mem << 12);
                    Register.setRegAH(EMM_NO_ERROR);
                } else {
                    Register.setRegAH(EMM_OUT_OF_LOG);
                }
                break;
            }
            case 0xDE05: /* VCPI Free Page */
                Memory.releasePages(Register.getRegEDX() >>> 12);
                Register.setRegAH(EMM_NO_ERROR);
                break;
            case 0xDE0C: { /* VCPI Switch from Protected Mode to V86 */
                Register.Flags &= (~Register.FlagIF);

                /* Flags need to be filled in, VM=true, IOPL=3 */
                Memory.writeD(Register.segPhys(Register.SEG_NAME_SS)
                        + (Register.getRegESP() & CPU.Block.Stack.Mask) + 0x10, 0x23002);

                /* Disable Paging */
                CPU.setCRX(0, CPU.getCRX(0) & 0x7ffffff7);
                CPU.setCRX(3, 0);

                int tbaddr = vcpi.PrivateAREA + 0x0000 + (0x10 & 0xfff8) + 5;
                int tb = Memory.readB(tbaddr);
                Memory.writeB(tbaddr, tb & 0xfd);

                /* Load descriptor table registers */
                CPU.lgdt(0xff, vcpi.PrivateAREA + 0x0000);
                CPU.lidt(0x7ff, vcpi.PrivateAREA + 0x2000);
                if (CPU.lldt(0x08))
                    Log.logMsg("VCPI:Could not load LDT");
                if (CPU.ltr(0x10))
                    Log.logMsg("VCPI:Could not load TR");

                Register.Flags &= (~Register.FlagNT);
                Register.setRegESP(Register.getRegESP() + 8); // skip interrupt return information
                // MEMORY.MEM_A20_Enable(false);

                /* Switch to v86-task */
                CPU.iret(true, 0);
            }
                break;
            default:
                Log.logging(Log.LogTypes.MISC, Log.LogServerities.Warn,
                        "Unhandled VCPI-function %x in protected mode", Register.getRegAL());
                break;
        }
        return Callback.ReturnTypeNone;
    }

    public static int V86Monitor() {
        /* Calculate which interrupt did occur */
        int intNum = Memory.readW(Register.segPhys(Register.SEG_NAME_SS)
                + (Register.getRegESP() & CPU.Block.Stack.Mask)) - 0x2803;

        /* See if Exception 0x0d and not Interrupt 0x0d */
        if ((intNum == (0x0d * 4)) && ((Register.getRegSP() & 0xffff) != 0x1fda)) {
            /*
             * Protection violation during V86-execution, needs intervention by monitor (depends on
             * faulting opcode)
             */

            Register.setRegESP(Register.getRegESP() + 6); // skip ip of CALL and error code of
                                                          // EXCEPTION 0x0d

            /* Get adress of faulting instruction */
            int v86CS = Memory.readW(Register.segPhys(Register.SEG_NAME_SS)
                    + ((Register.getRegESP() + 4) & CPU.Block.Stack.Mask));
            int v86IP = Memory.readW(Register.segPhys(Register.SEG_NAME_SS)
                    + ((Register.getRegESP() + 0) & CPU.Block.Stack.Mask));
            int v86Opcode = Memory.readB((v86CS << 4) + v86IP);
            // Log.LOG_MSG("v86 monitor caught protection violation at %x:%x,
            // opcode=%x",v86_cs,v86_ip,v86_opcode);
            switch (v86Opcode) {
                case 0x0f: // double byte opcode
                    v86Opcode = Memory.readB((v86CS << 4) + v86IP + 1);
                    switch (v86Opcode) {
                        case 0x20: { // mov reg,CRx
                            int rmVal = Memory.readB((v86CS << 4) + v86IP + 2);
                            int which = (rmVal >>> 3) & 7;
                            if ((rmVal < 0xc0) || (rmVal >= 0xe8))
                                Support.exceptionExit(
                                        "Invalid opcode 0x0f 0x20 %x caused a protection fault!",
                                        rmVal);
                            int crx = CPU.getCRX(which);
                            switch (rmVal & 7) {
                                case 0:
                                    Register.setRegEAX(crx);
                                    break;
                                case 1:
                                    Register.setRegECX(crx);
                                    break;
                                case 2:
                                    Register.setRegEDX(crx);
                                    break;
                                case 3:
                                    Register.setRegEBX(crx);
                                    break;
                                case 4:
                                    Register.setRegESP(crx);
                                    break;
                                case 5:
                                    Register.setRegEBP(crx);
                                    break;
                                case 6:
                                    Register.setRegESI(crx);
                                    break;
                                case 7:
                                    Register.setRegEDI(crx);
                                    break;
                            }
                            Memory.writeW(
                                    Register.segPhys(Register.SEG_NAME_SS)
                                            + ((Register.getRegESP() + 0) & CPU.Block.Stack.Mask),
                                    v86IP + 3);
                        }
                            break;
                        case 0x22: { // mov CRx,reg
                            int rmVal = Memory.readB((v86CS << 4) + v86IP + 2);
                            int which = (rmVal >>> 3) & 7;
                            if ((rmVal < 0xc0) || (rmVal >= 0xe8))
                                Support.exceptionExit(
                                        "Invalid opcode 0x0f 0x22 %x caused a protection fault!",
                                        rmVal);
                            int crx = 0;
                            switch (rmVal & 7) {
                                case 0:
                                    crx = Register.getRegEAX();
                                    break;
                                case 1:
                                    crx = Register.getRegECX();
                                    break;
                                case 2:
                                    crx = Register.getRegEDX();
                                    break;
                                case 3:
                                    crx = Register.getRegEBX();
                                    break;
                                case 4:
                                    crx = Register.getRegESP();
                                    break;
                                case 5:
                                    crx = Register.getRegEBP();
                                    break;
                                case 6:
                                    crx = Register.getRegESI();
                                    break;
                                case 7:
                                    crx = Register.getRegEDI();
                                    break;
                            }
                            if (which == 0)
                                crx |= 1; // protection bit always on
                            CPU.setCRX(which, crx);
                            Memory.writeW(
                                    Register.segPhys(Register.SEG_NAME_SS)
                                            + ((Register.getRegESP() + 0) & CPU.Block.Stack.Mask),
                                    v86IP + 3);
                        }
                            break;
                        default:
                            Support.exceptionExit(
                                    "Unhandled opcode 0x0f %x caused a protection fault!",
                                    v86Opcode);
                            break;
                    }
                    break;
                case 0xe4: // IN AL,Ib
                    Register.setRegAL(IO.readB(Memory.readB((v86CS << 4) + v86IP + 1)) & 0xff);
                    Memory.writeW(
                            Register.segPhys(Register.SEG_NAME_SS)
                                    + ((Register.getRegESP() + 0) & CPU.Block.Stack.Mask),
                            v86IP + 2);
                    break;
                case 0xe5: // IN AX,Ib
                    Register.setRegAX(IO.readW(Memory.readB((v86CS << 4) + v86IP + 1)) & 0xffff);
                    Memory.writeW(
                            Register.segPhys(Register.SEG_NAME_SS)
                                    + ((Register.getRegESP() + 0) & CPU.Block.Stack.Mask),
                            v86IP + 2);
                    break;
                case 0xe6: // OUT Ib,AL
                    IO.writeB(Memory.readB((v86CS << 4) + v86IP + 1), Register.getRegAL());
                    Memory.writeW(
                            Register.segPhys(Register.SEG_NAME_SS)
                                    + ((Register.getRegESP() + 0) & CPU.Block.Stack.Mask),
                            v86IP + 2);
                    break;
                case 0xe7: // OUT Ib,AX
                    IO.writeW(Memory.readB((v86CS << 4) + v86IP + 1), Register.getRegAX());
                    Memory.writeW(
                            Register.segPhys(Register.SEG_NAME_SS)
                                    + ((Register.getRegESP() + 0) & CPU.Block.Stack.Mask),
                            v86IP + 2);
                    break;
                case 0xec: // IN AL,DX
                    Register.setRegAL(IO.readB(Register.getRegDX() & 0xff));
                    Memory.writeW(
                            Register.segPhys(Register.SEG_NAME_SS)
                                    + ((Register.getRegESP() + 0) & CPU.Block.Stack.Mask),
                            v86IP + 1);
                    break;
                case 0xed: // IN AX,DX
                    Register.setRegAX(IO.readW(Register.getRegDX() & 0xffff));
                    Memory.writeW(
                            Register.segPhys(Register.SEG_NAME_SS)
                                    + ((Register.getRegESP() + 0) & CPU.Block.Stack.Mask),
                            v86IP + 1);
                    break;
                case 0xee: // OUT DX,AL
                    IO.writeB(Register.getRegDX(), Register.getRegAL());
                    Memory.writeW(
                            Register.segPhys(Register.SEG_NAME_SS)
                                    + ((Register.getRegESP() + 0) & CPU.Block.Stack.Mask),
                            v86IP + 1);
                    break;
                case 0xef: // OUT DX,AX
                    IO.writeW(Register.getRegDX(), Register.getRegAX());
                    Memory.writeW(
                            Register.segPhys(Register.SEG_NAME_SS)
                                    + ((Register.getRegESP() + 0) & CPU.Block.Stack.Mask),
                            v86IP + 1);
                    break;
                case 0xf0: // LOCK prefix
                    Memory.writeW(
                            Register.segPhys(Register.SEG_NAME_SS)
                                    + ((Register.getRegESP() + 0) & CPU.Block.Stack.Mask),
                            v86IP + 1);
                    break;
                case 0xf4: // HLT
                    Register.Flags |= Register.FlagIF;
                    CPU.hlt(Register.getRegEIP());
                    Memory.writeW(
                            Register.segPhys(Register.SEG_NAME_SS)
                                    + ((Register.getRegESP() + 0) & CPU.Block.Stack.Mask),
                            v86IP + 1);
                    break;
                default:
                    Support.exceptionExit("Unhandled opcode %x caused a protection fault!",
                            v86Opcode);
                    break;
            }
            return Callback.ReturnTypeNone;
        }

        /* Get address to interrupt handler */
        int vintVectorSeg = Memory.readW(Register.segValue(Register.SEG_NAME_DS) + intNum + 2);
        int vintVectorOfs = Memory.readW(intNum);
        if (Register.getRegSP() != 0x1fda)
            Register.setRegESP(Register.getRegESP() + (2 + 3 * 4)); // Interrupt from within
                                                                    // protected mode
        else
            Register.setRegESP(Register.getRegESP() + 2);

        /* Read entries that were pushed onto the stack by the interrupt */
        int returnIP = Memory.readW(Register.segPhys(Register.SEG_NAME_SS)
                + (Register.getRegESP() & CPU.Block.Stack.Mask));
        int returnCS = Memory.readW(Register.segPhys(Register.SEG_NAME_SS)
                + ((Register.getRegESP() + 4) & CPU.Block.Stack.Mask));
        int returnEFlags = Memory.readD(Register.segPhys(Register.SEG_NAME_SS)
                + ((Register.getRegESP() + 8) & CPU.Block.Stack.Mask));

        /* Modify stack to call v86-interrupt handler */
        Memory.writeD(Register.segPhys(Register.SEG_NAME_SS)
                + (Register.getRegESP() & CPU.Block.Stack.Mask), vintVectorOfs);
        Memory.writeD(Register.segPhys(Register.SEG_NAME_SS)
                + ((Register.getRegESP() + 4) & CPU.Block.Stack.Mask), vintVectorSeg);
        Memory.writeD(
                Register.segPhys(Register.SEG_NAME_SS)
                        + ((Register.getRegESP() + 8) & CPU.Block.Stack.Mask),
                returnEFlags & (~(Register.FlagIF | Register.FlagTF)));

        /* Adjust SP of v86-stack */
        int v86SS = Memory.readW(Register.segPhys(Register.SEG_NAME_SS)
                + ((Register.getRegESP() + 0x10) & CPU.Block.Stack.Mask));
        int v86SP = Memory.readW(Register.segPhys(Register.SEG_NAME_SS)
                + ((Register.getRegESP() + 0x0c) & CPU.Block.Stack.Mask)) - 6;
        Memory.writeW(Register.segPhys(Register.SEG_NAME_SS)
                + ((Register.getRegESP() + 0x0c) & CPU.Block.Stack.Mask), v86SP);

        /* Return to original code after v86-interrupt handler */
        Memory.writeW((v86SS << 4) + v86SP + 0, returnIP);
        Memory.writeW((v86SS << 4) + v86SP + 2, returnCS);
        Memory.writeW((v86SS << 4) + v86SP + 4, returnEFlags & 0xffff);
        return Callback.ReturnTypeNone;
    }

    public static void setupVCPI() {
        vcpi.Enabled = false;

        vcpi.EMSHandle = 0; // use EMM system handle for VCPI data

        vcpi.Enabled = true;

        vcpi.pic1Remapping = 0x08; // master PIC base
        vcpi.pic2Remapping = 0x70; // slave PIC base

        vcpi.PrivateAREA = EMMHandles[vcpi.EMSHandle].Mem << 12;

        /* GDT */
        Memory.writeD(vcpi.PrivateAREA + 0x0000, 0x00000000); // descriptor 0
        Memory.writeD(vcpi.PrivateAREA + 0x0004, 0x00000000); // descriptor 0

        int ldtAddress = (vcpi.PrivateAREA + 0x1000);
        short ldtLimit = 0xff;
        int ldtDescPart = ((ldtAddress & 0xffff) << 16) | ldtLimit;
        Memory.writeD(vcpi.PrivateAREA + 0x0008, ldtDescPart); // descriptor 1 (LDT)
        ldtDescPart = ((ldtAddress & 0xff0000) >>> 16) | (ldtAddress & 0xff000000) | 0x8200;
        Memory.writeD(vcpi.PrivateAREA + 0x000c, ldtDescPart); // descriptor 1

        int tssAddress = (vcpi.PrivateAREA + 0x3000);
        int tssDescPart = ((tssAddress & 0xffff) << 16) | (0x0068 + 0x200);
        Memory.writeD(vcpi.PrivateAREA + 0x0010, tssDescPart); // descriptor 2 (TSS)
        tssDescPart = ((tssAddress & 0xff0000) >>> 16) | (tssAddress & 0xff000000) | 0x8900;
        Memory.writeD(vcpi.PrivateAREA + 0x0014, tssDescPart); // descriptor 2

        /* LDT */
        Memory.writeD(vcpi.PrivateAREA + 0x1000, 0x00000000); // descriptor 0
        Memory.writeD(vcpi.PrivateAREA + 0x1004, 0x00000000); // descriptor 0
        int csDescPart = ((vcpi.PrivateAREA & 0xffff) << 16) | 0xffff;
        Memory.writeD(vcpi.PrivateAREA + 0x1008, csDescPart); // descriptor 1 (code)
        csDescPart =
                ((vcpi.PrivateAREA & 0xff0000) >>> 16) | (vcpi.PrivateAREA & 0xff000000) | 0x9a00;
        Memory.writeD(vcpi.PrivateAREA + 0x100c, csDescPart); // descriptor 1
        int dsDescPart = ((vcpi.PrivateAREA & 0xffff) << 16) | 0xffff;
        Memory.writeD(vcpi.PrivateAREA + 0x1010, dsDescPart); // descriptor 2 (data)
        dsDescPart =
                ((vcpi.PrivateAREA & 0xff0000) >>> 16) | (vcpi.PrivateAREA & 0xff000000) | 0x9200;
        Memory.writeD(vcpi.PrivateAREA + 0x1014, dsDescPart); // descriptor 2

        /* IDT setup */
        for (short intCT = 0; intCT < 0x100; intCT++) {
            /*
             * build a CALL NEAR V86MON, the value of IP pushed by the CALL is used to identify the
             * interrupt number
             */
            Memory.writeB(vcpi.PrivateAREA + 0x2800 + intCT * 4 + 0, 0xe8); // call
            Memory.writeW(vcpi.PrivateAREA + 0x2800 + intCT * 4 + 1, 0x05fd - (intCT * 4));
            Memory.writeB(vcpi.PrivateAREA + 0x2800 + intCT * 4 + 3, 0xcf); // iret(dummy)

            /* put a Gate-Descriptor into the IDT */
            Memory.writeD((vcpi.PrivateAREA + 0x2000 + intCT * 8 + 0),
                    0x000c0000 | (0x2800 + intCT * 4));
            Memory.writeD(vcpi.PrivateAREA + 0x2000 + intCT * 8 + 4, 0x0000ee00);
        }

        /* TSS */
        for (int tseCT = 0; tseCT < 0x68 + 0x200; tseCT++) {
            /* clear the TSS as most entries are not used here */
            Memory.writeB(vcpi.PrivateAREA + 0x3000, 0);
        }
        /* Set up the ring0-stack */
        Memory.writeD(vcpi.PrivateAREA + 0x3004, 0x00002000); // esp
        Memory.writeD(vcpi.PrivateAREA + 0x3008, 0x00000014); // ss
        // io-map base(map follows, all zero)
        Memory.writeD(vcpi.PrivateAREA + 0x3066, 0x0068);

    }

    public static int INT4BHandler() {
        switch (Register.getRegAH()) {
            case 0x81:
                Callback.scf(true);
                Register.setRegAX(0x1);
                break;
            default:
                Log.logging(Log.LogTypes.MISC, Log.LogServerities.Warn,
                        "Unhandled interrupt 4B function %x", Register.getRegAH());
                break;
        }
        return Callback.ReturnTypeNone;
    }

    static EMSModule _ems;

    private static void destroy(Section sec) {
        _ems.dispose();
        _ems = null;
    }

    public static void init(Section sec) throws WrongType {
        _ems = (new EMS()).new EMSModule(sec);
        sec.addDestroyFunction(EMS::destroy, true);
    }

    /*--------------------------- begin EMSModule -----------------------------*/
    private static int _emsBaseSeg = 0;

    private class EMSModule extends ModuleBase {

        /*
         * location in protected unfreeable memory where the ems name and callback are stored 32
         * bytes.
         */
        private int old4bPointer, old67Pointer;
        private CallbackHandlerObject callVDMA = new CallbackHandlerObject(),
                callVCPI = new CallbackHandlerObject(), _callV86Mon = new CallbackHandlerObject();
        private int _callINT67;

        public EMSModule(Section configuration) throws WrongType {
            super(configuration);

            /* Virtual DMA interrupt callback */
            callVDMA.install(EMS::INT4BHandler, Callback.Symbol.IRET, "Int 4b vdma");
            callVDMA.setRealVec(0x4b);

            vcpi.Enabled = false;
            DeviceEMM.clearGEMMIS();

            SectionProperty section = (SectionProperty) configuration;
            if (!section.getBool("ems"))
                return;
            if (DOSBox.Machine == DOSBox.MachineType.PCJR) {
                Log.logMsg("EMS disabled for PCJr dosbox.machine");
                return;
            }
            BIOS.ZeroExtendedSize(true);

            if (_emsBaseSeg == 0)
                _emsBaseSeg = DOSMain.getMemory(2); // We have 32 bytes

            /* Add a little hack so it appears that there is an actual ems device installed */
            CStringPt emsname = CStringPt.create("EMMXXXX0");
            Memory.blockWrite(Memory.physMake(_emsBaseSeg, 0xa), emsname);

            _callINT67 = Callback.allocate();
            Callback.setup(_callINT67, EMS::INT67Handler, Callback.Symbol.IRET,
                    Memory.physMake(_emsBaseSeg, 4), "Int 67 ems");
            old67Pointer = Memory.realSetVecAndReturnOld(0x67, Memory.realMake(_emsBaseSeg, 4));

            /* Register the ems device */
            // TODO MAYBE put it in the class.
            DOSDevice newdev = new DeviceEMM();
            DOSMain.addDevice(newdev);

            /* Clear handle and page tables */
            int i;
            for (i = 0; i < EMM_MAX_HANDLES; i++) {
                if (EMMHandles[i] == null)
                    EMMHandles[i] = new EMMHandle();
                EMMHandles[i].Mem = 0;
                EMMHandles[i].Pages = NULL_HANDLE;
                CStringPt.clear(EMMHandles[i].Name, 0, 8);
            }
            for (i = 0; i < EMM_MAX_PHYS; i++) {
                EMMmappings[i].page = NULL_PAGE;
                EMMmappings[i].handle = NULL_HANDLE;
            }
            for (i = 0; i < 0x40; i++) {
                EMMSegmentMappings[i].page = NULL_PAGE;
                EMMSegmentMappings[i].handle = NULL_HANDLE;
            }

            // allocate OS-dedicated handle (ems handle zero, 128kb)
            EMMAllocateSystemHandle(8);


            if (ENABLE_VCPI == 0)
                return;

            /* Install a callback that handles VCPI-requests in protected mode requests */
            callVCPI.install(EMS::VCPIPageModeHandler, Callback.Symbol.IRETD, "VCPI PM");
            vcpi.PMInterface = (callVCPI.getCallback()) * Callback.Symbol.IPXESR.toValue();

            /* Initialize private data area and set up descriptor tables */
            setupVCPI();

            if (!vcpi.Enabled)
                return;

            /*
             * Install v86-callback that handles interrupts occuring in v86 mode, including
             * protection fault exceptions
             */
            _callV86Mon.install(EMS::V86Monitor, Callback.Symbol.IRET, "V86 Monitor");

            Memory.writeB(vcpi.PrivateAREA + 0x2e00, 0xFE); // GRP 4
            Memory.writeB(vcpi.PrivateAREA + 0x2e01, 0x38); // Extra Callback instruction
            // The immediate word
            Memory.writeW(vcpi.PrivateAREA + 0x2e02, _callV86Mon.getCallback());
            Memory.writeB(vcpi.PrivateAREA + 0x2e04, 0x66);
            Memory.writeB(vcpi.PrivateAREA + 0x2e05, 0xCF); // A IRETD Instruction

            /* Testcode only, starts up dosbox in v86-mode */
            if (ENABLE_V86_STARTUP != 0) {
                /* Prepare V86-task */
                CPU.setCRX(0, 1);
                CPU.lgdt(0xff, vcpi.PrivateAREA + 0x0000);
                CPU.lidt(0x7ff, vcpi.PrivateAREA + 0x2000);
                if (CPU.lldt(0x08))
                    Log.logMsg("VCPI:Could not load LDT");
                if (CPU.ltr(0x10))
                    Log.logMsg("VCPI:Could not load TR");

                CPU.push32(Register.segValue(Register.SEG_NAME_GS));
                CPU.push32(Register.segValue(Register.SEG_NAME_FS));
                CPU.push32(Register.segValue(Register.SEG_NAME_DS));
                CPU.push32(Register.segValue(Register.SEG_NAME_ES));
                CPU.push32(Register.segValue(Register.SEG_NAME_SS));
                CPU.push32(0x23002);
                CPU.push32(Register.segValue(Register.SEG_NAME_CS));
                CPU.push32(Register.getRegEIP() & 0xffff);
                /* Switch to V86-mode */
                CPU.Block.CPL = 0;
                CPU.iret(true, 0);
            }
        }

        @Override
        protected void dispose(boolean disposing) {
            if (disposing) {

            }

            SectionProperty section = (SectionProperty) _configuration;
            if (!section.getBool("ems"))
                return;

            /* Undo Biosclearing */
            BIOS.ZeroExtendedSize(false);

            /* Remove ems device */
            DeviceEMM newDev = new DeviceEMM();
            DOSMain.delDevice(newDev);
            DeviceEMM.clearGEMMIS();

            /* Remove the emsname and callback hack */
            byte[] buf = new byte[32];
            buf[0] = 0;
            Memory.blockWrite(Memory.physMake(_emsBaseSeg, 0), buf, 0, 32);
            Memory.realSetVec(0x67, old67Pointer);

            /* Release memory allocated to system handle */
            if (EMMHandles[EMM_SYSTEM_HANDLE].Pages != NULL_HANDLE) {
                Memory.releasePages(EMMHandles[EMM_SYSTEM_HANDLE].Mem);
            }

            /* Clear handle and page tables */
            // TODO

            if ((ENABLE_VCPI == 0) || (!vcpi.Enabled))
                return;

            if (CPU.Block.PMode && Register.getFlag(Register.FlagVM) != 0) {
                /* Switch back to real mode if in v86-mode */
                CPU.setCRX(0, 0);
                CPU.setCRX(3, 0);
                Register.Flags &= (~(Register.FlagIOPL | Register.FlagVM));
                CPU.lidt(0x3ff, 0);
                CPU.Block.CPL = 0;
            }

            super.dispose(disposing);
        }


    }
    /*--------------------------- end EMSModule -----------------------------*/

}
