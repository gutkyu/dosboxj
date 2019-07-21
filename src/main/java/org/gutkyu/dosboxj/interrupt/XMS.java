package org.gutkyu.dosboxj.interrupt;

import org.gutkyu.dosboxj.dos.*;
import org.gutkyu.dosboxj.misc.setup.*;
import org.gutkyu.dosboxj.misc.setup.Value.WrongType;
import org.gutkyu.dosboxj.hardware.io.*;
import org.gutkyu.dosboxj.hardware.memory.*;
import org.gutkyu.dosboxj.interrupt.bios.*;
import org.gutkyu.dosboxj.cpu.*;
import org.gutkyu.dosboxj.util.*;
// using System.Runtime.InteropServices;

public final class XMS {
    private static final int XMS_HANDLES = 50; /* 50 XMS Memory Blocks */
    private static final int XMS_VERSION = 0x0300; /* version 3.00 */
    private static final int XMS_DRIVER_VERSION = 0x0301; /* my driver version 3.01 */

    private static final int XMS_GET_VERSION = 0x00;
    private static final int XMS_ALLOCATE_HIGH_MEMORY = 0x01;
    private static final int XMS_FREE_HIGH_MEMORY = 0x02;
    private static final int XMS_GLOBAL_ENABLE_A20 = 0x03;
    private static final int XMS_GLOBAL_DISABLE_A20 = 0x04;
    private static final int XMS_LOCAL_ENABLE_A20 = 0x05;
    private static final int XMS_LOCAL_DISABLE_A20 = 0x06;
    private static final int XMS_QUERY_A20 = 0x07;
    private static final int XMS_QUERY_FREE_EXTENDED_MEMORY = 0x08;
    private static final int XMS_ALLOCATE_EXTENDED_MEMORY = 0x09;
    private static final int XMS_FREE_EXTENDED_MEMORY = 0x0a;
    private static final int XMS_MOVE_EXTENDED_MEMORY_BLOCK = 0x0b;
    private static final int XMS_LOCK_EXTENDED_MEMORY_BLOCK = 0x0c;
    private static final int XMS_UNLOCK_EXTENDED_MEMORY_BLOCK = 0x0d;
    private static final int XMS_GET_EMB_HANDLE_INFORMATION = 0x0e;
    private static final int XMS_RESIZE_EXTENDED_MEMORY_BLOCK = 0x0f;
    private static final int XMS_ALLOCATE_UMB = 0x10;
    private static final int XMS_DEALLOCATE_UMB = 0x11;
    private static final int XMS_QUERY_ANY_FREE_MEMORY = 0x88;
    private static final int XMS_ALLOCATE_ANY_MEMORY = 0x89;
    private static final int XMS_GET_EMB_HANDLE_INFORMATION_EXT = 0x8e;
    private static final int XMS_RESIZE_ANY_EXTENDED_MEMORY_BLOCK = 0x8f;

    private static final int XMS_FUNCTION_NOT_IMPLEMENTED = 0x80;
    private static final int HIGH_MEMORY_NOT_EXIST = 0x90;
    private static final int HIGH_MEMORY_IN_USE = 0x91;
    private static final int HIGH_MEMORY_NOT_ALLOCATED = 0x93;
    private static final int XMS_OUT_OF_SPACE = 0xa0;
    private static final int XMS_OUT_OF_HANDLES = 0xa1;
    private static final int XMS_INVALID_HANDLE = 0xa2;
    private static final int XMS_INVALID_SOURCE_HANDLE = 0xa3;
    private static final int XMS_INVALID_SOURCE_OFFSET = 0xa4;
    private static final int XMS_INVALID_DEST_HANDLE = 0xa5;
    private static final int XMS_INVALID_DEST_OFFSET = 0xa6;
    private static final int XMS_INVALID_LENGTH = 0xa7;
    private static final int XMS_BLOCK_NOT_LOCKED = 0xaa;
    private static final int XMS_BLOCK_LOCKED = 0xab;
    private static final int UMB_ONLY_SMALLER_BLOCK = 0xb0;
    private static final int UMB_NO_BLOCKS_AVAILABLE = 0xb1;

    private XMS() {
        for (int i = 0; i < _xmsHandles.length; i++)
            _xmsHandles[i] = new XMSBlock();
    }

    private class XMSBlock {
        public int Size;
        public int Mem;
        public byte Locked;
        public boolean Free;
    }

    // --------------------------------- struct XMS_MemMove begin --------------------------------//
    private static final int Off_XMS_MemMove_length = 0;
    private static final int Off_XMS_MemMove_src_handle = 4;
    private static final int Off_XMS_MemMove_src_realpt = 6;
    private static final int Off_XMS_MemMove_src_offset = 6;
    private static final int Off_XMS_MemMove_dest_handle = 10;
    private static final int Off_XMS_MemMove_dest_realpt = 12;
    private static final int Off_XMS_MemMove_dest_offset = 12;
    // ---------------------------------- struct XMS_MemMove end ---------------------------------//

    private static int enableA20(boolean enable) {
        int val = IO.read(0x92);
        if (enable)
            IO.write(0x92, 0xff & (val | 2));
        else
            IO.write(0x92, 0xff & (val & ~2));
        return 0;
    }

    private static int getEnabledA20() {
        return (IO.read(0x92) & 2) > 0 ? 1 : 0;
    }

    private static int xmsCallback;
    private static boolean umbAvailable;

    private static XMSBlock[] _xmsHandles = new XMSBlock[XMS_HANDLES];

    private static boolean invalidHandle(int handle) {
        return (handle == 0 || (handle >= XMS_HANDLES) || _xmsHandles[handle].Free);
    }

    private static int queryFreeMemory(RefU16Ret refLargestFree, RefU16Ret refTotalFree) {
        /* Scan the tree for free memory and find largest free block */
        refTotalFree.U16 = (short) (Memory.freeTotal() * 4);
        refLargestFree.U16 = (short) (Memory.freeLargest() * 4);
        if (refTotalFree.U16 == 0)
            return XMS_OUT_OF_SPACE;
        return 0;
    }

    private static int allocateMemory(int size, RefU16Ret refHandle) { // size = kb
        /* Find free handle */
        short index = 1;
        while (!_xmsHandles[index].Free) {
            if (++index >= XMS_HANDLES)
                return XMS_OUT_OF_HANDLES;
        }
        int mem;
        if (size != 0) {
            int pages = (size / 4) + ((size & 3) != 0 ? 1 : 0);
            mem = Memory.allocatePages(pages, true);
            if (mem == 0)
                return XMS_OUT_OF_SPACE;
        } else {
            mem = Memory.getNextFreePage();
            if (mem == 0)
                Log.logging(Log.LogTypes.MISC, Log.LogServerities.Error,
                        "XMS:Allocate zero pages with no memory left");
        }
        _xmsHandles[index].Free = false;
        _xmsHandles[index].Mem = mem;
        _xmsHandles[index].Locked = 0;
        _xmsHandles[index].Size = size;
        refHandle.U16 = index;
        return 0;
    }

    private static int freeMemory(int handle) {
        if (invalidHandle(handle))
            return XMS_INVALID_HANDLE;
        Memory.releasePages(_xmsHandles[handle].Mem);
        _xmsHandles[handle].Mem = -1;
        _xmsHandles[handle].Size = 0;
        _xmsHandles[handle].Free = true;
        return 0;
    }

    private static class MemPos {
        public int RealPt;
        public int Offset;
    }

    private static int moveMemory(int bpt) {
        /* Read the block with mem_read's */
        int length = Memory.readD(bpt + Off_XMS_MemMove_length);
        int srcHandle = Memory.readW(bpt + Off_XMS_MemMove_src_handle);
        MemPos src = new MemPos(), dest = new MemPos();
        src.Offset = Memory.readD(bpt + Off_XMS_MemMove_src_offset);
        int destHandle = Memory.readW(bpt + Off_XMS_MemMove_dest_handle);
        dest.Offset = Memory.readD(bpt + Off_XMS_MemMove_dest_offset);
        int srcPt, destPt;
        if (srcHandle != 0) {
            if (invalidHandle(srcHandle)) {
                return XMS_INVALID_SOURCE_HANDLE;
            }
            if (src.Offset >= (_xmsHandles[srcHandle].Size * 1024)) {
                return XMS_INVALID_SOURCE_OFFSET;
            }
            if (length > _xmsHandles[srcHandle].Size * 1024 - src.Offset) {
                return XMS_INVALID_LENGTH;
            }
            srcPt = (int) (_xmsHandles[srcHandle].Mem * 4096) + src.Offset;
        } else {
            srcPt = Memory.real2Phys(src.RealPt);
        }
        if (destHandle != 0) {
            if (invalidHandle(destHandle)) {
                return XMS_INVALID_DEST_HANDLE;
            }
            if (dest.Offset >= (_xmsHandles[destHandle].Size * 1024)) {
                return XMS_INVALID_DEST_OFFSET;
            }
            if (length > _xmsHandles[destHandle].Size * 1024 - dest.Offset) {
                return XMS_INVALID_LENGTH;
            }
            destPt = (int) (_xmsHandles[destHandle].Mem * 4096) + dest.Offset;
        } else {
            destPt = Memory.real2Phys(dest.RealPt);
        }
        // Log.LOG_MSG("XMS move src %X dest %X length %X",srcpt,destpt,length);
        Memory.memCpy(destPt, srcPt, length);
        return 0;
    }

    private static int lockMemory(int handle, RefU32Ret refAddr) {
        if (invalidHandle(handle))
            return XMS_INVALID_HANDLE;
        if (_xmsHandles[handle].Locked < 255)
            _xmsHandles[handle].Locked++;
        refAddr.U32 = (int) _xmsHandles[handle].Mem * 4096;
        return 0;
    }

    private static int unlockMemory(int handle) {
        if (invalidHandle(handle))
            return XMS_INVALID_HANDLE;
        if (_xmsHandles[handle].Locked != 0) {
            _xmsHandles[handle].Locked--;
            return 0;
        }
        return XMS_BLOCK_NOT_LOCKED;
    }

    // private static int getHandleInformation(int handle, ref byte lockCount, ref
    // byte numFree, ref short size)
    private static int checkHandle(int handle) {
        return invalidHandle(handle) ? XMS_INVALID_HANDLE : 0;
    }

    private static byte getHandleLockCount(int handle) {
        return _xmsHandles[handle].Locked;
    }

    private static byte getHandleAvailBlocks(int handle) {
        /* Find available blocks */
        byte numFree = 0;
        for (int i = 1; i < XMS_HANDLES; i++) {
            if (_xmsHandles[i].Free)
                numFree++;
        }
        return numFree;
    }

    // uint16(int)
    private static int getHandleSize(int handle) {
        return _xmsHandles[handle].Size;
    }

    private static int resizeMemory(int handle, int newSize) {
        if (invalidHandle(handle))
            return XMS_INVALID_HANDLE;
        // Block has to be unlocked
        if (_xmsHandles[handle].Locked > 0)
            return XMS_BLOCK_LOCKED;
        int pages = newSize / 4 + ((newSize & 3) != 0 ? 1 : 0);
        RefU32Ret refHandle = new RefU32Ret(_xmsHandles[handle].Mem);
        if (Memory.reallocatePages(refHandle, pages, true)) {
            _xmsHandles[handle].Mem = refHandle.U32;
            _xmsHandles[handle].Size = newSize;
            return 0;
        } else
            return XMS_OUT_OF_SPACE;
    }

    private static final MultiplexHandler multiplexXMSHandler = XMS::multiplexXMS;

    private static boolean multiplexXMS() {
        switch (Register.getRegAX()) {
            case 0x4300: /* XMS installed check */
                Register.setRegAL(0x80);
                return true;
            case 0x4310: /* XMS handler seg:offset */
                Register.segSet16(Register.SEG_NAME_ES, Memory.realSeg(xmsCallback));
                Register.setRegBX(Memory.realOff(xmsCallback));
                return true;
        }
        return false;

    }

    private static void setResult(int res, boolean touch_bl_on_succes) {
        if (touch_bl_on_succes || res != 0)
            Register.setRegBL(res);
        Register.setRegAX((res == 0) ? 1 : 0);
    }

    private static void setResult(int res) {
        setResult(res, true);
    }

    private static int XMSHandler() {
        // LOG(LOG_MISC,LOG_ERROR)("XMS: CALL %02X",regsModule.reg_ah);
        switch (0xffffffff & Register.getRegAH()) {
            case XMS_GET_VERSION: /* 00 */
                Register.setRegAX(XMS_VERSION);
                Register.setRegBX(XMS_DRIVER_VERSION);
                Register.setRegDX(0); /* No we don't have HMA */
                break;
            case XMS_ALLOCATE_HIGH_MEMORY: /* 01 */
                Register.setRegAX(0);
                Register.setRegBL(HIGH_MEMORY_NOT_EXIST);
                break;
            case XMS_FREE_HIGH_MEMORY: /* 02 */
                Register.setRegAX(0);
                Register.setRegBL(HIGH_MEMORY_NOT_EXIST);
                break;

            case XMS_GLOBAL_ENABLE_A20: /* 03 */
            case XMS_LOCAL_ENABLE_A20: /* 05 */
                setResult(enableA20(true));
                break;
            case XMS_GLOBAL_DISABLE_A20: /* 04 */
            case XMS_LOCAL_DISABLE_A20: /* 06 */
                setResult(enableA20(false));
                break;
            case XMS_QUERY_A20: /* 07 */
                Register.setRegAX(getEnabledA20());
                Register.setRegBL(0);
                break;
            case XMS_QUERY_FREE_EXTENDED_MEMORY: /* 08 */
            {
                RefU16Ret refReg0 = new RefU16Ret(0);
                RefU16Ret refReg1 = new RefU16Ret(0);
                Register.setRegBL(queryFreeMemory(refReg0, refReg1));
                Register.setRegAX(refReg0.U16);
                Register.setRegDX(refReg1.U16);
            }
                break;
            case XMS_ALLOCATE_ANY_MEMORY: /* 89 */
                Register.setRegEDX(Register.getRegEDX() & 0xffff);
                // goto GotoXMS_ALLOCATE_EXTENDED_MEMORY;
                // fall through
            case XMS_ALLOCATE_EXTENDED_MEMORY: /* 09 */
            // GotoXMS_ALLOCATE_EXTENDED_MEMORY:
            {
                RefU16Ret refHandle = new RefU16Ret(0);
                setResult(allocateMemory(Register.getRegDX(), refHandle));
                Register.setRegDX(refHandle.U16);
            }
                break;
            case XMS_FREE_EXTENDED_MEMORY: /* 0a */
                setResult(freeMemory(Register.getRegDX()));
                break;
            case XMS_MOVE_EXTENDED_MEMORY_BLOCK: /* 0b */
                setResult(moveMemory(Register.segPhys(Register.SEG_NAME_DS) + Register.getRegSI()),
                        false);
                break;
            case XMS_LOCK_EXTENDED_MEMORY_BLOCK: { /* 0c */
                int address = 0;
                RefU32Ret refAddr = new RefU32Ret(address);
                int res = lockMemory(Register.getRegDX(), refAddr);
                address = refAddr.U32;
                if (res != 0)
                    Register.setRegBL(res);
                Register.setRegAX(res == 0 ? 1 : 0);
                if (res == 0) { // success
                    Register.setRegBX(address & 0xFFFF);
                    Register.setRegDX(address >>> 16);
                }
            }
                break;
            case XMS_UNLOCK_EXTENDED_MEMORY_BLOCK: /* 0d */
                setResult(unlockMemory(Register.getRegDX()));
                break;
            case XMS_GET_EMB_HANDLE_INFORMATION: /* 0e */
            {
                int handle = Register.getRegDX();
                int result = checkHandle(handle);
                byte regVal0 = 0, regVal1 = 0;
                int regVal2 = 0;
                setResult(result, false);
                if (result == 0) {
                    regVal0 = getHandleLockCount(handle);
                    regVal1 = getHandleAvailBlocks(handle);
                    regVal2 = getHandleSize(handle);
                }
                Register.setRegBH(regVal0);
                Register.setRegBL(regVal1);
                Register.setRegDX(regVal2);
            }
                break;
            case XMS_RESIZE_ANY_EXTENDED_MEMORY_BLOCK: /* 0x8f */
                if (Register.getRegEBX() > Register.getRegBX())
                    Log.logMsg("64MB memory limit!");
                // goto GotoXMS_RESIZE_EXTENDED_MEMORY_BLOCK;
                // fall through
            case XMS_RESIZE_EXTENDED_MEMORY_BLOCK: /* 0f */
                // GotoXMS_RESIZE_EXTENDED_MEMORY_BLOCK:
                setResult(resizeMemory(Register.getRegDX(), Register.getRegBX()));
                break;
            case XMS_ALLOCATE_UMB: { /* 10 */
                if (!umbAvailable) {
                    Register.setRegAX(0);
                    Register.setRegBL(XMS_FUNCTION_NOT_IMPLEMENTED);
                    break;
                }
                int umb_start = DOSMain.DOSInfoBlock.getStartOfUMBChain();
                if (umb_start == 0xffff) {
                    Register.setRegAX(0);
                    Register.setRegBL(UMB_NO_BLOCKS_AVAILABLE);
                    Register.setRegDX(0); // no upper memory available
                    break;
                }
                /*
                 * Save status and linkage of upper UMB chain and link upper memory to the regular
                 * MCB chain
                 */
                byte umb_flag = DOSMain.DOSInfoBlock.getUMBChainState();
                if ((umb_flag & 1) == 0)
                    DOSMain.linkUMBsToMemChain(1);
                int oldMemstrat = DOSMain.getMemAllocStrategy() & 0xff;
                DOSMain.setMemAllocStrategy(0x40); // search in UMBs only

                int size = Register.getRegDX();
                int seg = 0;
                RefU32Ret refSize = new RefU32Ret(size);
                RefU32Ret refSeg = new RefU32Ret(seg);
                if (DOSMain.allocateMemory(refSeg, refSize)) {
                    seg = refSeg.U32;
                    size = refSize.U32;
                    Register.setRegAX(1);
                    Register.setRegBX(seg);
                } else {
                    Register.setRegAX(0);
                    if (size == 0)
                        Register.setRegBL(UMB_NO_BLOCKS_AVAILABLE);
                    else
                        Register.setRegBL(UMB_ONLY_SMALLER_BLOCK);
                    Register.setRegDX(size); // size of largest available UMB
                }

                /* Restore status and linkage of upper UMB chain */
                byte current_umb_flag = DOSMain.DOSInfoBlock.getUMBChainState();
                if ((current_umb_flag & 1) != (umb_flag & 1))
                    DOSMain.linkUMBsToMemChain(umb_flag);
                DOSMain.setMemAllocStrategy(oldMemstrat);
            }
                break;
            case XMS_DEALLOCATE_UMB: /* 11 */
                if (!umbAvailable) {
                    Register.setRegAX(0);
                    Register.setRegBL(XMS_FUNCTION_NOT_IMPLEMENTED);
                    break;
                }
                if (DOSMain.DOSInfoBlock.getStartOfUMBChain() != 0xffff) {
                    if (DOSMain.freeMemory(Register.getRegDX())) {
                        Register.setRegAX(0x0001);
                        break;
                    }
                }
                Register.setRegAX(0x0000);
                Register.setRegBL(UMB_NO_BLOCKS_AVAILABLE);
                break;
            case XMS_QUERY_ANY_FREE_MEMORY: /* 88 */
            {
                RefU16Ret refReg0 = new RefU16Ret(0);
                RefU16Ret refReg1 = new RefU16Ret(0);
                Register.setRegBL(queryFreeMemory(refReg0, refReg1));
                Register.setRegAX(refReg0.U16);
                Register.setRegDX(refReg1.U16);
                Register.setRegEAX(Register.getRegEAX() & 0xffff);
                Register.setRegEDX(Register.getRegEDX() & 0xffff);
                // highest known physical memory address
                Register.setRegECX((Memory.totalPages() * Memory.MEM_PAGESIZE) - 1);
            }
                break;
            case XMS_GET_EMB_HANDLE_INFORMATION_EXT: { /* 8e */
                byte regVal0 = 0, regVal1 = 0;
                int regVal2 = 0;
                byte free_handles;
                int handle = Register.getRegDX();

                int result = checkHandle(handle);
                if (result == 0) {
                    regVal0 = getHandleLockCount(handle);
                    regVal1 = getHandleAvailBlocks(handle);
                    regVal2 = getHandleSize(handle);
                }
                Register.setRegBH(regVal0);
                free_handles = regVal1;
                Register.setRegDX(regVal2);
                if (result != 0)
                    Register.setRegBL(result);
                else {
                    Register.setRegEDX(Register.getRegEDX() & 0xffff);
                    Register.setRegCX(free_handles);
                }
                Register.setRegAX(result == 0 ? 1 : 0);
            }
                break;
            default:
                Log.logging(Log.LogTypes.MISC, Log.LogServerities.Error,
                        "XMS: unknown function %02X", Register.getRegAH());
                Register.setRegAX(0);
                Register.setRegBL(XMS_FUNCTION_NOT_IMPLEMENTED);
                break;
        }
        // LOG(LOG_MISC,LOG_ERROR)("XMS: CALL Result: %02X",regsModule.reg_bl);
        return Callback.ReturnTypeNone;
    }

    static XMSModule _xms;

    private static void destroy(Section sec) {
        _xms.dispose();
        _xms = null;
    }

    public static void init(Section sec) throws WrongType {
        _xms = (new XMS()).new XMSModule(sec);
        sec.addDestroyFunction(XMS::destroy, true);
    }

    /*--------------------------- begin XMSModule -----------------------------*/
    public final class XMSModule extends ModuleBase {
        private CallbackHandlerObject _callbackHandler = new CallbackHandlerObject();

        public XMSModule(Section configuration) throws WrongType {
            super(configuration);
            SectionProperty section = (SectionProperty) configuration;
            umbAvailable = false;
            if (!section.getBool("xms"))
                return;
            int i;
            BIOS.ZeroExtendedSize(true);
            DOSSystem.addMultiplexHandler(XMS.multiplexXMSHandler);

            /* place hookable callback in writable memory area */
            xmsCallback = Memory.realMake(DOSMain.getMemory(0x1) - 1, 0x10);
            _callbackHandler.install(XMS::XMSHandler, Callback.Symbol.HOOKABLE,
                    Memory.real2Phys(xmsCallback), "XMS Handler");
            // pseudocode for CALLBACK.Symbol.CB_HOOKABLE:
            // jump near skip
            // nop,nop,nop
            // label skip:
            // callback XMS_Handler
            // retf

            for (i = 0; i < XMS_HANDLES; i++) {
                _xmsHandles[i].Free = true;
                _xmsHandles[i].Mem = -1;
                _xmsHandles[i].Size = 0;
                _xmsHandles[i].Locked = 0;
            }
            /* Disable the 0 handle */
            _xmsHandles[0].Free = false;

            /* Set up UMB chain */
            umbAvailable = section.getBool("umb");
            DOSMain.buildUMBChain(section.getBool("umb"), section.getBool("ems"));
        }

        @Override
        protected void dispose(boolean disposing) {
            if (disposing) {

            }

            SectionProperty section = (SectionProperty) _configuration;
            /* Remove upper memory information */
            DOSMain.DOSInfoBlock.setStartOfUMBChain(0xffff);
            if (umbAvailable) {
                DOSMain.DOSInfoBlock.setUMBChainState(0);
                umbAvailable = false;
            }

            if (!section.getBool("xms"))
                return;
            /* Undo biosclearing */
            BIOS.ZeroExtendedSize(false);

            /* Remove Multiplex */
            DOSSystem.delMultiplexHandler(XMS.multiplexXMSHandler);

            /* Free used memory while skipping the 0 handle */
            for (int i = 1; i < XMS_HANDLES; i++)
                if (!_xmsHandles[i].Free)
                    freeMemory(i);

            super.dispose(disposing);
        }
    }

}
