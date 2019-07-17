package org.gutkyu.dosboxj.dos.software;

import org.gutkyu.dosboxj.dos.*;
import org.gutkyu.dosboxj.*;
import org.gutkyu.dosboxj.misc.*;
import org.gutkyu.dosboxj.util.*;
import org.gutkyu.dosboxj.hardware.memory.*;
import org.gutkyu.dosboxj.cpu.*;

public final class MEM extends Program {
    public static Program makeProgram() {
        return new MEM();
    }

    @Override
    public void run() {
        /* Show conventional Memory */
        writeOut("\n");

        int umbStart = DOSMain.DOSInfoBlock.getStartOfUMBChain();
        byte umbFlag = DOSMain.DOSInfoBlock.getUMBChainState();
        byte oldMemstrat = (byte) (DOSMain.getMemAllocStrategy() & 0xff);
        if (umbStart != 0xffff) {
            if ((umbFlag & 1) == 1)
                DOSMain.linkUMBsToMemChain( 0);
            DOSMain.setMemAllocStrategy( 0);
        }

        int seg = 0, blocks;
        blocks = 0xffff;
        RefU32Ret refSize = new RefU32Ret(blocks);
        RefU32Ret refSeg = new RefU32Ret(seg);
        DOSMain.allocateMemory(refSeg, refSize);
        seg = refSeg.U32;
        blocks = refSize.U32;

        if ((DOSBox.Machine == DOSBox.MachineType.PCJR) && (Memory.realReadB(0x2000, 0) == 0x5a)
                && (Memory.realReadW(0x2000, 1) == 0) && (Memory.realReadW(0x2000, 3) == 0x7ffe)) {
            writeOut(Message.get("PROGRAM_MEM_CONVEN"), 0x7ffe * 16 / 1024);
        } else
            writeOut(Message.get("PROGRAM_MEM_CONVEN"), blocks * 16 / 1024);

        if (umbStart != 0xffff) {
            DOSMain.linkUMBsToMemChain((short) 1);
            DOSMain.setMemAllocStrategy(0x40); // search in UMBs only

            int largestBlock = 0, total_blocks = 0, block_count = 0;
            for (;; block_count++) {
                blocks = 0xffff;
                refSize.set(blocks);
                refSeg.set(seg);
                DOSMain.allocateMemory(refSeg, refSize);
                seg = refSeg.U32;
                blocks = refSize.U32;

                if (blocks == 0)
                    break;
                total_blocks += blocks;
                if (blocks > largestBlock)
                    largestBlock = blocks;
                refSize.U32 = blocks;
                refSeg.U32 = seg;
                DOSMain.allocateMemory(refSeg, refSize);
                seg = refSeg.U32;
                blocks = refSize.U32;

            }

            byte current_umb_flag = DOSMain.DOSInfoBlock.getUMBChainState();
            if ((current_umb_flag & 1) != (umbFlag & 1))
                DOSMain.linkUMBsToMemChain(umbFlag);
            DOSMain.setMemAllocStrategy(oldMemstrat); // restore strategy

            if (block_count > 0)
                writeOut(Message.get("PROGRAM_MEM_UPPER"), total_blocks * 16 / 1024, block_count,
                        largestBlock * 16 / 1024);
        }

        /* Test for and show free XMS */
        Register.setRegAX(0x4300);
        Callback.runRealInt(0x2f);
        if (Register.getRegAL() == 0x80) {
            Register.setRegAX(0x4310);
            Callback.runRealInt(0x2f);
            int xmsSeg = Register.segValue(Register.SEG_NAME_ES);
            int xms_off = Register.getRegBX();
            Register.setRegAH((byte) 8);
            Callback.runRealFar(xmsSeg, xms_off);
            if (Register.getRegBL() == 0) {
                writeOut(Message.get("PROGRAM_MEM_EXTEND"), Register.getRegDX());
            }
        }
        /* Test for and show free EMS */
        short handle = 0;
        String emm = "EMMXXXX0";
        RefU16Ret refEntry = new RefU16Ret(handle);
        if (DOSMain.openFile(emm, (byte) 0, refEntry)) {
            handle = refEntry.U16;
            DOSMain.closeFile(handle);
            Register.setRegAH(0x42);
            Callback.runRealInt(0x67);
            writeOut(Message.get("PROGRAM_MEM_EXPAND"), Register.getRegBX() * 16);
        }
    }
}
