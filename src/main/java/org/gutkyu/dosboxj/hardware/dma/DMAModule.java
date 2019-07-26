package org.gutkyu.dosboxj.hardware.dma;

import org.gutkyu.dosboxj.misc.setup.*;
import org.gutkyu.dosboxj.misc.*;
import org.gutkyu.dosboxj.*;
import org.gutkyu.dosboxj.hardware.io.*;
import org.gutkyu.dosboxj.hardware.memory.*;
import org.gutkyu.dosboxj.hardware.memory.paging.*;



public final class DMAModule extends ModuleBase {

    private DMAModule(Section configuration) {
        super(configuration);
        int i;
        dmaControllers[0] = new DMAController(0);
        if (DOSBox.isEGAVGAArch())
            dmaControllers[1] = new DMAController(1);
        else
            dmaControllers[1] = null;

        for (i = 0; i < 0x10; i++) {
            int mask = IO.IO_MB;
            if (i < 8)
                mask |= IO.IO_MW;
            /* install handler for first DMA controller ports */
            dmaControllers[0].writeHandler[i].install(i, this::writePort, mask);
            dmaControllers[0].readHandler[i].install(i, this::readPort, mask);
            if (DOSBox.isEGAVGAArch()) {
                /* install handler for second DMA controller ports */
                dmaControllers[1].writeHandler[i].install(0xc0 + i * 2, this::writePort, mask);
                dmaControllers[1].readHandler[i].install(0xc0 + i * 2, this::readPort, mask);
            }
        }
        /* install handlers for ports 0x81-0x83 (on the first DMA controller) */
        dmaControllers[0].writeHandler[0x10].install(0x81, this::writePort, IO.IO_MB, 3);
        dmaControllers[0].readHandler[0x10].install(0x81, this::readPort, IO.IO_MB, 3);

        if (DOSBox.isEGAVGAArch()) {
            /* install handlers for ports 0x81-0x83 (on the second DMA controller) */
            dmaControllers[1].writeHandler[0x10].install(0x89, this::writePort, IO.IO_MB, 3);
            dmaControllers[1].readHandler[0x10].install(0x89, this::readPort, IO.IO_MB, 3);
        }
    }


    @Override
    protected void dispose(boolean disposing) {
        if (disposing) {

        }
        if (dmaControllers[0] != null) {
            dmaControllers[0].dispose();
            dmaControllers[0] = null;
        }
        if (dmaControllers[1] != null) {
            dmaControllers[1].dispose();
            dmaControllers[1] = null;
        }
    }


    private static int _dmaWrapping = 0xffff;

    protected static int getDMAWapping() {
        return _dmaWrapping;
    }

    private DMAController[] dmaControllers = new DMAController[2];

    private static final int EMM_PAGEFRAME4K = ((0xE000 * 16) / 4096);
    private int[] emsBoardMapping = new int[Paging.LINK_START];

    protected void updateEMSMapping() {
        /* if EMS is not present, this will result in a 1:1 mapping */
        int i;
        for (i = 0; i < 0x10; i++) {
            emsBoardMapping[EMM_PAGEFRAME4K + i] = Paging.paging.FirstMb[EMM_PAGEFRAME4K + i];
        }
    }

    public DMAChannel getDMAChannel(int chan) {
        if (chan < 4) {
            /* channel on first DMA controller */
            if (dmaControllers[0] != null)
                return dmaControllers[0].getChannel(chan);
        } else if (chan < 8) {
            /* channel on second DMA controller */
            if (dmaControllers[1] != null)
                return dmaControllers[1].getChannel(chan - 4);
        }
        return null;
    }

    /* remove the second DMA controller (ports are removed automatically) */
    public void closeSecondDMAController() {
        if (dmaControllers[1] != null) {
            dmaControllers[1].dispose();
            dmaControllers[1] = null;
        }
    }

    /* check availability of second DMA controller, needed for SB16 */
    public boolean availableSecondDMAController() {
        if (dmaControllers[1] != null)
            return true;
        else
            return false;
    }

    // -- #region DMA_prefix
    /* read a block from physical memory */
    // public static void DMA_BlockRead(PhysPt spage,PhysPt offset,void * data,int size,byte dma16)
    // dataIdx 반환
    protected int readBlock(int spage, int offset, byte[] data, int dataIdx, int size, byte dma16) {

        int highpart_addr_page = spage >>> 12;
        size <<= dma16;
        offset <<= dma16;
        int dmaWrap = ((0xffff << dma16) + dma16) | _dmaWrapping;
        for (; size > 0; size--, offset++) {
            if (offset > (_dmaWrapping << dma16))
                Support.exceptionExit("DMA segbound wrapping (read)");
            offset &= dmaWrap;
            int page = highpart_addr_page + (offset >>> 12);
            /* care for EMS pageframe etc. */
            if (page < EMM_PAGEFRAME4K)
                page = Paging.paging.FirstMb[page];
            else if (page < EMM_PAGEFRAME4K + 0x10)
                page = emsBoardMapping[page];
            else if (page < Paging.LINK_START)
                page = Paging.paging.FirstMb[page];
            data[dataIdx++] = (byte) Memory.physReadB(page * 4096 + (offset & 4095));
        }
        return dataIdx;
    }

    /* write a block into physical memory */
    // public static void DMA_BlockRead(PhysPt spage,PhysPt offset,void * data,int size,byte dma16)
    protected int writeBlock(int spage, int offset, byte[] data, int dataIdx, int size,
            byte dma16) {
        int highpart_addr_page = spage >>> 12;
        size <<= dma16;
        offset <<= dma16;
        int dma_wrap = ((0xffff << dma16) + dma16) | _dmaWrapping;
        for (; size > 0; size--, offset++) {
            if (offset > (_dmaWrapping << dma16))
                Support.exceptionExit("DMA segbound wrapping (write)");
            offset &= dma_wrap;
            int page = highpart_addr_page + (offset >>> 12);
            /* care for EMS pageframe etc. */
            if (page < EMM_PAGEFRAME4K)
                page = Paging.paging.FirstMb[page];
            else if (page < EMM_PAGEFRAME4K + 0x10)
                page = emsBoardMapping[page];
            else if (page < Paging.LINK_START)
                page = Paging.paging.FirstMb[page];
            Memory.physWriteB(page * 4096 + (offset & 4095), data[dataIdx++]);
        }
        return dataIdx;
    }

    private void writePort(int port, int val, int iolen) {
        if (port < 0x10) {
            /* write to the first DMA controller (channels 0-3) */
            dmaControllers[0].writeControllerReg(port, val, 1);
        } else if (port >= 0xc0 && port <= 0xdf) {
            /* write to the second DMA controller (channels 4-7) */
            dmaControllers[1].writeControllerReg((port - 0xc0) >>> 1, val, 1);
        } else {
            updateEMSMapping();
            switch (port) {
                /* write DMA page register */
                case 0x81:
                    getDMAChannel(2).setPage(val);
                    break;
                case 0x82:
                    getDMAChannel(3).setPage(val);
                    break;
                case 0x83:
                    getDMAChannel(1).setPage(val);
                    break;
                case 0x89:
                    getDMAChannel(6).setPage(val);
                    break;
                case 0x8a:
                    getDMAChannel(7).setPage(val);
                    break;
                case 0x8b:
                    getDMAChannel(5).setPage(val);
                    break;
            }
        }
    }

    private int readPort(int port, int iolen) {
        if (port < 0x10) {
            /* read from the first DMA controller (channels 0-3) */
            return dmaControllers[0].readControllerReg(port, iolen);
        } else if (port >= 0xc0 && port <= 0xdf) {
            /* read from the second DMA controller (channels 4-7) */
            return dmaControllers[1].readControllerReg((port - 0xc0) >>> 1, iolen);
        } else
            switch (port) {
                /* read DMA page register */
                case 0x81:
                    return getDMAChannel(2).pageNum;
                case 0x82:
                    return getDMAChannel(3).pageNum;
                case 0x83:
                    return getDMAChannel(1).pageNum;
                case 0x89:
                    return getDMAChannel(6).pageNum;
                case 0x8a:
                    return getDMAChannel(7).pageNum;
                case 0x8b:
                    return getDMAChannel(5).pageNum;
            }
        return 0;
    }


    private void setWrapping(int wrap) {
        _dmaWrapping = wrap;
    }

    private static DMAModule _dma = null;

    private static void destroy(Section sec) {
        _dma.dispose();
        _dma = null;
    }

    public static void init(Section sec) {
        _dma = new DMAModule(sec);
        sec.addDestroyFunction(DMAModule::destroy);
        _dma.setWrapping(0xffff);
        int i;
        for (i = 0; i < Paging.LINK_START; i++) {
            _dma.emsBoardMapping[i] = i;
        }
    }

    protected static DMAModule instance() {
        return _dma;
    }
    // -- #endregion



}
