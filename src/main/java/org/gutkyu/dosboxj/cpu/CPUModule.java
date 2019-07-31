package org.gutkyu.dosboxj.cpu;

import org.gutkyu.dosboxj.misc.setup.*;
import org.gutkyu.dosboxj.misc.setup.Value.WrongType;
import org.gutkyu.dosboxj.gui.*;

public final class CPUModule extends ModuleBase {
    private static boolean inited = false;

    public CPUModule(Section configuration) throws WrongType {
        super(configuration);
        if (inited) {
            changeConfig(configuration);
            return;
        }
        // Section_prop * section=static_cast<Section_prop *>(configuration);
        inited = true;
        Register.init();


        CPU.setFlags(Register.FlagIF, Register.FMaskAll); // Enable interrupts
        CPU.Block.CR0 = 0xffffffff;
        CPU.setCRX(0, 0); // Initialize
        CPU.Block.Code.Big = false;
        CPU.Block.Stack.Mask = 0xffff;
        CPU.Block.Stack.NotMask = 0xffff0000;
        CPU.Block.Stack.Big = false;
        CPU.Block.TrapSkip = false;
        CPU.Block.IDT.setBase(0);
        CPU.Block.IDT.setLimit(1023);

        for (int i = 0; i < 7; i++) {
            CPU.Block.DRX[i] = 0;
            CPU.Block.TRX[i] = 0;
        }
        if (CPU.ArchitectureType == CPU.ArchTypePentiumSlow) {
            CPU.Block.DRX[6] = 0xffff0ff0;
        } else {
            CPU.Block.DRX[6] = 0xffff1ff0;
        }
        CPU.Block.DRX[7] = 0x00000400;

        /* Init the cpu cores */
        CoreNormal.instance().initCPUCore();
        CoreSimple.instance().initCPUCore();
        CoreFull.instance().initCPUCore();

        GUIPlatform.mapper.addKeyHandler(CPU::cycleDecrease, MapKeys.F11, Mapper.MMOD1, "cycledown",
                "Dec Cycles");
        GUIPlatform.mapper.addKeyHandler(CPU::cycleIncrease, MapKeys.F12, Mapper.MMOD1, "cycleup",
                "Inc Cycles");
        changeConfig(configuration);
        CPU.jmp(false, 0, 0, 0); // Setup the first cpu core
    }

    @Override
    public boolean changeConfig(Section newConfig) throws WrongType {
        SectionProperty section = (SectionProperty) newConfig;
        CPU.AutoDetermineMode = CPU.AutoDetermineNone;
        // CPU_CycleLeft=0;//needed ?
        CPU.Cycles = 0;
        CPU.SkipCycleAutoAdjust = false;

        PropertyMultival p = section.getMultival("cycles");
        String type = p.getSection().getString("type");
        String str = null;
        CommandLine cmd = new CommandLine(null, p.getSection().getString("parameters"));
        if (type.equals("max")) {
            CPU.CycleMax = 0;
            CPU.CyclePercUsed = 100;
            CPU.CycleAutoAdjust = true;
            CPU.CycleLimit = -1;
            for (int cmdnum = 1; cmdnum <= cmd.getCount(); cmdnum++) {
                if (cmd.findCommand(cmdnum)) {
                    str = cmd.returnedCmd;
                    if (str.indexOf('%') == str.length() - 1) {
                        // str = str.Remove(str.indexOf('%'));
                        str = str.substring(0, str.length() - 1);
                        int percval = 0;
                        percval = Integer.parseInt(str);
                        if ((percval > 0) && (percval <= 105))
                            CPU.CyclePercUsed = percval;
                    } else if (str.equals("limit")) {
                        cmdnum++;
                        if (cmd.findCommand(cmdnum)) {
                            str = cmd.returnedCmd;
                            int cyclimit = 0;
                            cyclimit = Integer.parseInt(str);
                            if (cyclimit > 0)
                                CPU.CycleLimit = cyclimit;
                        }
                    }
                }
            }
        } else {
            if (type.equals("auto")) {
                CPU.AutoDetermineMode |= CPU.AutoDetermineCycles;
                CPU.CycleMax = 3000;
                CPU.OldCycleMax = 3000;
                CPU.CyclePercUsed = 100;
                for (int cmdnum = 0; cmdnum <= cmd.getCount(); cmdnum++) {
                    if (cmd.findCommand(cmdnum)) {
                        str = cmd.returnedCmd;
                        if (str.indexOf('%') == str.length() - 1) {
                            // str = str.Remove(str.indexOf('%'));
                            str = str.substring(0, str.length() - 1);
                            int percval = 0;
                            percval = Integer.parseInt(str);
                            if ((percval > 0) && (percval <= 105))
                                CPU.CyclePercUsed = percval;
                        } else if (str.equals("limit")) {
                            cmdnum++;
                            if (cmd.findCommand(cmdnum)) {
                                str = cmd.returnedCmd;
                                int cyclimit = 0;
                                cyclimit = Integer.parseInt(str);
                                if (cyclimit > 0)
                                    CPU.CycleLimit = cyclimit;
                            }
                        } else {
                            int rmdval = 0;
                            rmdval = Integer.parseInt(str);
                            if (rmdval > 0) {
                                CPU.CycleMax = rmdval;
                                CPU.OldCycleMax = rmdval;
                            }
                        }
                    }
                }
            } else if (type.equals("fixed")) {
                str = cmd.findCommand(1) ? cmd.returnedCmd : str;
                int rmdval = 0;
                rmdval = Integer.parseInt(str);
                CPU.CycleMax = rmdval;
            } else {
                int rmdval = 0;
                rmdval = Integer.parseInt(type);
                if (rmdval != 0)
                    CPU.CycleMax = rmdval;
            }
            CPU.CycleAutoAdjust = false;
        }

        CPU.CycleUp = section.getInt("cycleup");
        CPU.CycleDown = section.getInt("cycledown");
        String core = section.getString("core");
        CPU.CpuDecoder = CoreNormal.instance().CpuDecoder;
        if (core.equals("normal")) {
            CPU.CpuDecoder = CoreNormal.instance().CpuDecoder;
        } else if (core.equals("simple")) {
            CPU.CpuDecoder = CoreSimple.instance().CpuDecoder;
        }
        // TODO core full 구현
        // else if (core == "full")
        // {
        // cpuModule.cpudecoder = &CPU_Core_Full_Run;
        // }
        else if (core.equals("auto")) {
            CPU.CpuDecoder = CoreNormal.instance().CpuDecoder;
        }
        CPU.ArchitectureType = CPU.ArchTypeMixed;
        String cpuType = section.getString("cputype");
        if (cpuType.equals("auto")) {
            CPU.ArchitectureType = CPU.ArchTypeMixed;
        } else if (cpuType.equals("386")) {
            CPU.ArchitectureType = CPU.ArchType386Fast;
        }
        // TODO 386_prefetch 구현
        // else if (cputype == "386_prefetch")
        // {
        // cpuModule.CPU_ArchitectureType = cpuModule.CPU_ARCHTYPE_386FAST;
        // if (core == "normal")
        // {
        // cpuModule.cpudecoder = &CPU_Core_Prefetch_Run;
        // cpuModule.CPU_PrefetchQueueSize = 16;
        // }
        // else if (core == "auto")
        // {
        // cpuModule.cpudecoder = &CPU_Core_Prefetch_Run;
        // cpuModule.CPU_PrefetchQueueSize = 16;
        // cpuModule.CPU_AutoDetermineMode &= (~cpuModule.CPU_AUTODETERMINE_CORE);
        // }
        // else
        // {
        // Support.E_Exit("prefetch queue emulation requires the normal core setting.");
        // }
        // }
        else if (cpuType.equals("386_slow")) {
            CPU.ArchitectureType = CPU.ArchType386Slow;
        } else if (cpuType.equals("486_slow")) {
            CPU.ArchitectureType = CPU.ArchType486NewSlow;
        }
        // TODO 486_prefetch 구현
        // else if (cputype == "486_prefetch")
        // {
        // cpuModule.CPU_ArchitectureType = cpuModule.CPU_ARCHTYPE_486NEWSLOW;
        // if (core == "normal")
        // {
        // cpuModule.cpudecoder = &CPU_Core_Prefetch_Run;
        // cpuModule.CPU_PrefetchQueueSize = 32;
        // }
        // else if (core == "auto")
        // {
        // cpuModule.cpudecoder = &CPU_Core_Prefetch_Run;
        // cpuModule.CPU_PrefetchQueueSize = 32;
        // cpuModule.CPU_AutoDetermineMode &= (~cpuModule.CPU_AUTODETERMINE_CORE);
        // }
        // else
        // {
        // Support.E_Exit("prefetch queue emulation requires the normal core setting.");
        // }
        // }
        else if (cpuType.equals("pentium_slow")) {
            CPU.ArchitectureType = CPU.ArchTypePentiumSlow;
        }

        if (CPU.ArchitectureType >= CPU.ArchType486NewSlow)
            CPU.FlagIdToggle = Register.FlagID;
        else
            CPU.FlagIdToggle = 0;


        if (CPU.CycleMax <= 0)
            CPU.CycleMax = 3000;
        if (CPU.CycleUp <= 0)
            CPU.CycleUp = 500;
        if (CPU.CycleDown <= 0)
            CPU.CycleDown = 20;
        if (CPU.CycleAutoAdjust)
            GUIPlatform.gfx.setTitle(CPU.CyclePercUsed, -1, false);
        else
            GUIPlatform.gfx.setTitle(CPU.CycleMax, -1, false);
        return true;

    }
}
