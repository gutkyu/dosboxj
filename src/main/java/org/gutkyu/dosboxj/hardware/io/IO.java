package org.gutkyu.dosboxj.hardware.io;

import org.gutkyu.dosboxj.cpu.*;
import org.gutkyu.dosboxj.misc.*;
import org.gutkyu.dosboxj.hardware.io.iohandler.*;
import org.gutkyu.dosboxj.hardware.memory.*;
import org.gutkyu.dosboxj.util.*;
import org.gutkyu.dosboxj.*;

public final class IO {
    public static final int IO_MAX = (64 * 1024 + 3);

    public static final int IO_MB = 0x1;
    public static final int IO_MW = 0x2;
    public static final int IO_MD = 0x4;
    public static final int IO_MA = (IO_MB | IO_MW | IO_MD);

    private static CPUDecoder IOFaultDecoder = new CPUDecoder(IO::IOFaultCore);

    private static WriteHandler[][] ioWritehandlers = new WriteHandler[3][IO.IO_MAX];
    private static ReadHandler[][] ioReadhandlers = new ReadHandler[3][IO.IO_MAX];

    public static class IOFEntry {
        public int cs;
        public int eip;
    }

    private static final int IOF_QUEUESIZE = 16;

    public static class iofQueue {
        public static int used;
        public static IOFEntry[] entries = new IOFEntry[IOF_QUEUESIZE];
    }

    private static int IOFaultCore() {
        CPU.CycleLeft += CPU.Cycles;
        CPU.Cycles = 1;
        int ret = CoreFull.instance().runCPUCore();
        CPU.CycleLeft += CPU.Cycles;
        if (ret < 0)
            Support.exceptionExit("Got a dosbox close machine in IO-fault core?");
        if (ret != 0)
            return ret;
        if (iofQueue.used == 0)
            Support.exceptionExit("IO-faul Core without IO-faul");
        IOFEntry entry = iofQueue.entries[iofQueue.used - 1];
        if (entry.cs == Register.segValue(Register.SEG_NAME_CS)
                && entry.eip == Register.getRegEIP())
            return -1;
        return 0;
    }


    // -- #region IO_prefix
    private static int readBlocked(int port, int iolen) {
        return ~0;
    }

    private static void writeBlocked(int port, int val, int iolen) {
    }

    public static void registerReadHandler(int port, ReadHandler handler, int mask, int range) {
        while (range-- != 0) {
            if ((mask & IO.IO_MB) != 0)
                ioReadhandlers[0][port] = handler;
            if ((mask & IO.IO_MW) != 0)
                ioReadhandlers[1][port] = handler;
            if ((mask & IO.IO_MD) != 0)
                ioReadhandlers[2][port] = handler;
            port++;
        }
    }

    public static void registerReadHandler(int port, ReadHandler handler, int mask) {
        registerReadHandler(port, handler, mask, 1);
    }

    public static void registerWriteHandler(int port, WriteHandler handler, int mask, int range) {
        while (range-- != 0) {
            if ((mask & IO.IO_MB) != 0)
                ioWritehandlers[0][port] = handler;
            if ((mask & IO.IO_MW) != 0)
                ioWritehandlers[1][port] = handler;
            if ((mask & IO.IO_MD) != 0)
                ioWritehandlers[2][port] = handler;
            port++;
        }
    }

    public static void registerWriteHandler(int port, WriteHandler handler, int mask) {
        registerWriteHandler(port, handler, mask, 1);
    }

    private static int readDefault(int port, int iolen) {
        switch (iolen) {
            case 1:
                Log.logging(Log.LogTypes.IO, Log.LogServerities.Warn, "Read from port %04X", port);
                ioReadhandlers[0][port] = IO::readBlocked;
                return 0xff;
            case 2:
                return (ioReadhandlers[0][port + 0].run(port + 0, 1) << 0)
                        | (ioReadhandlers[0][port + 1].run(port + 1, 1) << 8);
            case 4:
                return (ioReadhandlers[1][port + 0].run(port + 0, 2) << 0)
                        | (ioReadhandlers[1][port + 2].run(port + 2, 2) << 16);
        }
        return 0;
    }

    private static void writeDefault(int port, int val, int iolen) {
        switch (iolen) {
            case 1:
                Log.logging(Log.LogTypes.IO, Log.LogServerities.Warn, "Writing %02X to port %04X",
                        val, port);
                ioWritehandlers[0][port] = IO::writeBlocked;
                break;
            case 2:
                ioWritehandlers[0][port + 0].run(port + 0, (val >>> 0) & 0xff, 1);
                ioWritehandlers[0][port + 1].run(port + 1, (val >>> 8) & 0xff, 1);
                break;
            case 4:
                ioWritehandlers[1][port + 0].run(port + 0, (val >>> 0) & 0xffff, 2);
                ioWritehandlers[1][port + 2].run(port + 2, (val >>> 16) & 0xffff, 2);
                break;
        }
    }

    public static void freeReadHandler(int port, int mask, int range) {
        while (range-- != 0) {
            if ((mask & IO.IO_MB) != 0)
                ioReadhandlers[0][port] = IO::readDefault;
            if ((mask & IO.IO_MW) != 0)
                ioReadhandlers[1][port] = IO::readDefault;
            if ((mask & IO.IO_MD) != 0)
                ioReadhandlers[2][port] = IO::readDefault;
            port++;
        }
    }

    public static void freeReadHandler(int port, int mask) {
        freeReadHandler(port, mask, 1);
    }

    public static void freeWriteHandler(int port, int mask, int range) {
        while (range-- != 0) {
            if ((mask & IO.IO_MB) != 0)
                ioWritehandlers[0][port] = IO::writeDefault;
            if ((mask & IO.IO_MW) != 0)
                ioWritehandlers[1][port] = IO::writeDefault;
            if ((mask & IO.IO_MD) != 0)
                ioWritehandlers[2][port] = IO::writeDefault;
            port++;
        }
    }

    public static void freeWriteHandler(int port, int mask) {
        freeWriteHandler(port, mask, 1);
    }

    private static final int IODELAY_READ_MICROSk = (int) (1024 / 1.0);
    private static final int IODELAY_WRITE_MICROSk = (int) (1024 / 0.75);

    private static void readDelayUSEC() {
        int delaycyc = CPU.CycleMax / IODELAY_READ_MICROSk;
        if ((CPU.Cycles < 3 * delaycyc))
            delaycyc = 0; // Else port acces will set cycles to 0. which might trigger problem with
                          // games which read 16 bit values
        CPU.Cycles -= delaycyc;
        CPU.IODelayRemoved += delaycyc;
    }

    private static void writeDelayUSEC() {
        int delaycyc = CPU.CycleMax / IODELAY_WRITE_MICROSk;
        if ((CPU.Cycles < 3 * delaycyc))
            delaycyc = 0;
        CPU.Cycles -= delaycyc;
        CPU.IODelayRemoved += delaycyc;
    }

    private static void logIO(int width, boolean write, int port, int val) {
    }

    public static void writeB(int port, int val) {
        logIO(0, true, port, val);
        if (Register.getFlag(Register.FlagVM) != 0 && (CPU.ioException(port, 1))) {
            LazyFlags old_lflags;

            old_lflags = Flags.LzFlags.deepCopy();// memcpy(&old_lflags,&lflags,sizeof(LazyFlags));
            CPUDecoder old_cpudecoder;
            old_cpudecoder = CPU.CpuDecoder;
            CPU.CpuDecoder = IO.IOFaultDecoder;
            IOFEntry entry = iofQueue.entries[iofQueue.used++];
            entry.cs = Register.segValue(Register.SEG_NAME_CS);
            entry.eip = Register.getRegEIP();
            CPU.push16(Register.segValue(Register.SEG_NAME_CS));
            CPU.push16(Register.getRegIP());
            int old_al = Register.getRegAL();
            int old_dx = Register.getRegDX();
            Register.setRegAL(val);
            Register.setRegDX(port);
            int icb = Callback.realPointer(Callback.callPrivIO);
            Register.segSet16(Register.SEG_NAME_CS, Memory.realSeg(icb));
            Register.setRegEIP(Memory.realOff(icb) + 0x08);
            CPU.exception(CPU.Block.Exception.Which, CPU.Block.Exception.Error);

            DOSBox.runMachine();
            iofQueue.used--;

            Register.setRegAL(old_al);
            Register.setRegDX(old_dx);
            Flags.LzFlags = old_lflags;// memcpy(&lflags,&old_lflags,sizeof(LazyFlags))
            CPU.CpuDecoder = old_cpudecoder;
        } else {
            writeDelayUSEC();
            ioWritehandlers[0][port].run(port, val, 1);
        }
    }


    public static void writeW(int port, int val) {
        logIO(1, true, port, val);
        if (Register.getFlag(Register.FlagVM) != 0 && (CPU.ioException(port, 2))) {
            LazyFlags old_lflags;
            old_lflags = Flags.LzFlags.deepCopy();// memcpy(&old_lflags, &lflags,sizeof(LazyFlags));
            CPUDecoder old_cpudecoder;
            old_cpudecoder = CPU.CpuDecoder;
            CPU.CpuDecoder = IO.IOFaultDecoder;
            IOFEntry entry = iofQueue.entries[iofQueue.used++];
            entry.cs = Register.segValue(Register.SEG_NAME_CS);
            entry.eip = Register.getRegEIP();
            CPU.push16(Register.segValue(Register.SEG_NAME_CS));
            CPU.push16(Register.getRegIP());
            int old_ax = Register.getRegAX();
            int old_dx = Register.getRegDX();
            Register.setRegAL(val);
            Register.setRegDX(port);
            int icb = Callback.realPointer(Callback.callPrivIO);
            Register.segSet16(Register.SEG_NAME_CS, Memory.realSeg(icb));
            Register.setRegEIP(Memory.realOff(icb) + 0x0a);
            CPU.exception(CPU.Block.Exception.Which, CPU.Block.Exception.Error);

            DOSBox.runMachine();
            iofQueue.used--;

            Register.setRegAX(old_ax);
            Register.setRegDX(old_dx);
            Flags.LzFlags = old_lflags;// memcpy(&lflags, &old_lflags, sizeof(LazyFlags))
            CPU.CpuDecoder = old_cpudecoder;
        } else {
            writeDelayUSEC();
            ioWritehandlers[1][port].run(port, val, 2);
        }
    }

    public static void writeD(int port, int val) {
        logIO(2, true, port, val);
        if (Register.getFlag(Register.FlagVM) != 0 && (CPU.ioException(port, 4))) {
            LazyFlags old_lflags;
            old_lflags = Flags.LzFlags.deepCopy();// memcpy(&old_lflags, &lflags,sizeof(LazyFlags));
            CPUDecoder old_cpudecoder;
            old_cpudecoder = CPU.CpuDecoder;
            CPU.CpuDecoder = IO.IOFaultDecoder;
            IOFEntry entry = iofQueue.entries[iofQueue.used++];
            entry.cs = Register.segValue(Register.SEG_NAME_CS);
            entry.eip = Register.getRegEIP();
            CPU.push16(Register.segValue(Register.SEG_NAME_CS));
            CPU.push16(Register.getRegIP());
            int old_eax = Register.getRegEAX();
            int old_dx = Register.getRegDX();
            Register.setRegAL(val);
            Register.setRegDX(port);
            int icb = Callback.realPointer(Callback.callPrivIO);
            Register.segSet16(Register.SEG_NAME_CS, Memory.realSeg(icb));
            Register.setRegEIP(Memory.realOff(icb) + 0x0c);
            CPU.exception(CPU.Block.Exception.Which, CPU.Block.Exception.Error);

            DOSBox.runMachine();
            iofQueue.used--;

            Register.setRegEAX(old_eax);
            Register.setRegDX(old_dx);
            Flags.LzFlags = old_lflags; // memcpy(&lflags, &old_lflags, sizeof(LazyFlags))
            CPU.CpuDecoder = old_cpudecoder;
        } else
            ioWritehandlers[2][port].run(port, val, 4);
    }

    public static int readB(int port) {
        int retval;
        if (Register.getFlag(Register.FlagVM) != 0 && (CPU.ioException(port, 1))) {
            LazyFlags old_lflags;
            old_lflags = Flags.LzFlags.deepCopy();// memcpy(&old_lflags, &lflags,sizeof(LazyFlags));
            CPUDecoder old_cpudecoder;
            old_cpudecoder = CPU.CpuDecoder;
            CPU.CpuDecoder = IO.IOFaultDecoder;
            IOFEntry entry = iofQueue.entries[iofQueue.used++];
            entry.cs = Register.segValue(Register.SEG_NAME_CS);
            entry.eip = Register.getRegEIP();
            CPU.push16(Register.segValue(Register.SEG_NAME_CS));
            CPU.push16(Register.getRegIP());
            int old_dx = Register.getRegDX();
            Register.setRegDX(port);
            int icb = Callback.realPointer(Callback.callPrivIO);
            Register.segSet16(Register.SEG_NAME_CS, Memory.realSeg(icb));
            Register.setRegEIP(Memory.realOff(icb) + 0x00);
            CPU.exception(CPU.Block.Exception.Which, CPU.Block.Exception.Error);

            DOSBox.runMachine();
            iofQueue.used--;

            retval = Register.getRegAL();
            Register.setRegDX(old_dx);
            Flags.LzFlags = old_lflags;// memcpy(&lflags, &old_lflags, sizeof(LazyFlags))
            CPU.CpuDecoder = old_cpudecoder;
            return retval;
        } else {
            readDelayUSEC();
            retval = ioReadhandlers[0][port].run(port, 1);
        }
        logIO(0, false, port, retval);
        return retval;
    }


    public static int readW(int port) {
        int retval;
        if (Register.getFlag(Register.FlagVM) != 0 && (CPU.ioException(port, 2))) {
            LazyFlags old_lflags;
            old_lflags = Flags.LzFlags.deepCopy();// memcpy(&old_lflags,&lflags,sizeof(LazyFlags));
            CPUDecoder old_cpudecoder;
            old_cpudecoder = CPU.CpuDecoder;
            CPU.CpuDecoder = IO.IOFaultDecoder;
            IOFEntry entry = iofQueue.entries[iofQueue.used++];
            entry.cs = Register.segValue(Register.SEG_NAME_CS);
            entry.eip = Register.getRegEIP();
            CPU.push16(Register.segValue(Register.SEG_NAME_CS));
            CPU.push16(Register.getRegIP());
            int old_dx = Register.getRegDX();
            Register.setRegDX(port);
            int icb = Callback.realPointer(Callback.callPrivIO);
            Register.segSet16(Register.SEG_NAME_CS, Memory.realSeg(icb));
            Register.setRegEIP(Memory.realOff(icb) + 0x02);
            CPU.exception(CPU.Block.Exception.Which, CPU.Block.Exception.Error);

            DOSBox.runMachine();
            iofQueue.used--;

            retval = Register.getRegAX();
            Register.setRegDX(old_dx);
            // memcpy(&lflags, &old_lflags, sizeof(LazyFlags));
            CPU.CpuDecoder = old_cpudecoder;
        } else {
            readDelayUSEC();
            retval = ioReadhandlers[1][port].run(port, 2);
        }
        logIO(1, false, port, retval);
        return retval;
    }

    public static int readD(int port) {
        int retval;
        if (Register.getFlag(Register.FlagVM) != 0 && (CPU.ioException(port, 4))) {
            LazyFlags old_lflags;
            old_lflags = Flags.LzFlags.deepCopy();// memcpy(&old_lflags,&lflags,sizeof(LazyFlags));
            CPUDecoder old_cpudecoder;
            old_cpudecoder = CPU.CpuDecoder;
            CPU.CpuDecoder = IO.IOFaultDecoder;
            IOFEntry entry = iofQueue.entries[iofQueue.used++];
            entry.cs = Register.segValue(Register.SEG_NAME_CS);
            entry.eip = Register.getRegEIP();
            CPU.push16(Register.segValue(Register.SEG_NAME_CS));
            CPU.push16(Register.getRegIP());
            int old_dx = Register.getRegDX();
            Register.setRegDX(port);
            int icb = Callback.realPointer(Callback.callPrivIO);
            Register.segSet16(Register.SEG_NAME_CS, Memory.realSeg(icb));
            Register.setRegEIP(Memory.realOff(icb) + 0x04);
            CPU.exception(CPU.Block.Exception.Which, CPU.Block.Exception.Error);

            DOSBox.runMachine();
            iofQueue.used--;

            retval = Register.getRegEAX();
            Register.setRegDX(old_dx);
            Flags.LzFlags = old_lflags;// memcpy(&lflags, &old_lflags, sizeof(LazyFlags))
            CPU.CpuDecoder = old_cpudecoder;
        } else {
            retval = ioReadhandlers[2][port].run(port, 4);
        }
        logIO(2, false, port, retval);
        return retval;
    }

    // (int, byte)
    public static void write(int port, int val) {
        IO.writeB(port, 0xff & val);
    }

    // uint8 (int)
    public static int read(int port) {
        return IO.readB(port);
    }
    // -- #endregion
}
