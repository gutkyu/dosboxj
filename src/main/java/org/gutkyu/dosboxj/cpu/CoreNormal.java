package org.gutkyu.dosboxj.cpu;

import org.gutkyu.dosboxj.hardware.memory.*;

public final class CoreNormal extends CPUCore {
    private CoreNormal() {
        super();
    }

    @Override
    public String getDecorderName() {
        return "CoreNormal";
    }

    @Override
    protected int getIP() {
        return (Core.CSEIP - Register.segPhys(Register.SEG_NAME_CS));
    }

    @Override
    protected void saveIP() {
        Register.setRegEIP(getIP());
    }

    @Override
    protected void loadIP() {
        Core.CSEIP = (Register.segPhys(Register.SEG_NAME_CS) + Register.getRegEIP());
    }

    @Override
    protected int fetchB() {
        int temp = Memory.readB(Core.CSEIP);
        Core.CSEIP += 1;
        return temp;
    }

    @Override
    protected int fetchW() {
        int temp = Memory.readW(Core.CSEIP);
        Core.CSEIP += 2;
        return temp;
    }

    @Override
    protected int fetchD() {
        int temp = Memory.readD(Core.CSEIP);
        Core.CSEIP += 4;
        return temp;
    }

    @Override
    public void initCPUCore() {

    }

    private static CPUCore cpu = new CoreNormal();

    public static CPUCore instance() {
        return cpu;
    }
}
