package org.gutkyu.dosboxj.hardware.memory.paging;



import org.gutkyu.dosboxj.misc.*;

public class PageHandler {
    public int readB(int addr) {
        Support.exceptionExit("No byte handler for read from %d", addr);
        return 0;
    }

    public int readW(int addr) {
        return (readB(addr + 0) << 0) | (readB(addr + 1) << 8);
    }

    public long readD(int addr) {
        return (readB(addr + 0) << 0) | (readB(addr + 1) << 8) | (readB(addr + 2) << 16)
                | (readB(addr + 3) << 24);
    }

    public void writeB(int addr, int val) {
        Support.exceptionExit("No byte handler for write to %d", addr);
    }

    public void writeW(int addr, int val) {
        writeB(addr + 0, val >>> 0);
        writeB(addr + 1, val >>> 8);
    }

    public void writeD(int addr, int val) {
        writeB(addr + 0, val >>> 0);
        writeB(addr + 1, val >>> 8);
        writeB(addr + 2, val >>> 16);
        writeB(addr + 3, val >>> 24);
    }


    // GetHostReadPt,GetHostWritePt가 반환하는 dos 메모리 페이지의 포인터는 각 할당된 메모리의 인덱스로 변환했기 때문에
    // 할당된 메모리 정보도 같이 가지고 있어야 하기에 생성한 메소드
    public byte[] getHostMemory() {
        return null;
    }

    public int getHostReadPt(int physPage) {
        return Paging.NullState;
    }

    public int getHostWritePt(int physPage) {
        return Paging.NullState;
    }

    // checked == true 경우 -1 반환
    // checked == false 경우 해당 값 반환
    // uint16(int)
    public int readBChecked(int addr) {
        // return false;
        return readB(addr);
    }

    // checked == true 경우 -1 반환
    // checked == false 경우 해당 값 반환
    public int readWChecked(int addr) {
        return readW(addr);
        // return false;
    }

    // checked == true 경우 -1 반환
    // checked == long 경우 해당 값 반환
    public long readDChecked(int addr) {
        return readD(addr);
        // return false;
    }

    public boolean writeBChecked(int addr, int val) {
        writeB(addr, val);
        return false;
    }

    public boolean writeWChecked(int addr, int val) {
        writeW(addr, val);
        return false;
    }

    public boolean writeDChecked(int addr, int val) {
        writeD(addr, val);
        return false;
    }

    public int Flags;
}
