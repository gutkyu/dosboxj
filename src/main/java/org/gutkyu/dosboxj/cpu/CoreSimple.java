package org.gutkyu.dosboxj.cpu;

import org.gutkyu.dosboxj.hardware.memory.*;

public final class CoreSimple extends CPUCore {
    private CoreSimple() {
        super();
    }


    @Override
    public String getDecorderName() {
        return "CoreSimple";
    }

    // core_normal의 CPU_Core_Trap_Run가 CPU_Core_Run 안에서 사용되므로 동일할 코드로 정의

    // unsafe 코드를 허용해서 host에서 할당된 실제 메모리의 시작 번지를 가리키는 MemBase를 구현하지 않는한
    // 현재 host할당 dos메모리는 byte[]이고 이 배열의 시작은 항상 0이므로 cpu_normal과 동일한 GetTIP, SAVEIP, LOADIP를 사용.


    @Override
    protected int fetchB() {
        int temp = Memory.hostReadB(Core.CSEIP);
        Core.CSEIP += 1;
        return temp;
    }

    // uint16
    @Override
    protected int fetchW() {
        int temp = Memory.hostReadW(Core.CSEIP);
        Core.CSEIP += 2;
        return temp;
    }

    @Override
    protected int fetchD() {
        int temp = Memory.hostReadD(Core.CSEIP);
        Core.CSEIP += 4;
        return temp;
    }

    @Override
    public void initCPUCore() {

    }

    private static CPUCore _obj = new CoreSimple();

    public static CPUCore instance() {
        return _obj;
    }
}
