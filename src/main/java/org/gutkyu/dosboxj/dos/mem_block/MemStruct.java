package org.gutkyu.dosboxj.dos.mem_block;


import org.gutkyu.dosboxj.hardware.memory.*;

public class MemStruct {
    public int getIt(int size, int addr) {
        switch (size) {
            case 1:
                return Memory.readB(pt + addr);
            case 2:
                return Memory.readW(pt + addr);
            case 4:
                return Memory.readD(pt + addr);
        }
        return 0;
    }

    public void saveIt(int size, int addr, int val) {
        switch (size) {
            case 1:
                Memory.writeB(pt + addr, val);
                break;
            case 2:
                Memory.writeW(pt + addr, val);
                break;
            case 4:
                Memory.writeD(pt + addr, val);
                break;
        }
    }

    public void setSegPt(int seg) {
        pt = Memory.physMake(seg, 0);
    }

    public void setSegPt(int seg, int off) {
        pt = Memory.physMake(seg, off);
    }

    public void setRealPt(int addr) {
        pt = Memory.real2Phys(addr);
    }

    protected int pt;
}
