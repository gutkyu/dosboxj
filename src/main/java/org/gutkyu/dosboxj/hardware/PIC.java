package org.gutkyu.dosboxj.hardware;

import org.gutkyu.dosboxj.misc.*;
import org.gutkyu.dosboxj.cpu.*;
import org.gutkyu.dosboxj.misc.setup.*;
import org.gutkyu.dosboxj.util.*;
import org.gutkyu.dosboxj.*;
import org.gutkyu.dosboxj.hardware.io.IO;
import org.gutkyu.dosboxj.hardware.io.iohandler.*;

public final class PIC {

    private static final int PIC_MAXIRQ = 15;
    private static final int PIC_NOIRQ = 0xFF;

    private PIC() {
        for (int i = 0; i < irqs.length; i++) {
            irqs[i] = new IRQBlock();
        }
    }

    public static float getTickIndex() {
        return (CPU.CycleMax - CPU.CycleLeft - CPU.Cycles) / (float) CPU.CycleMax;
    }

    private static int getTickIndexND() {
        return CPU.CycleMax - CPU.CycleLeft - CPU.Cycles;
    }

    private static int makeCycles(double amount) {
        return (int) (CPU.CycleMax * amount);
    }

    public static double getFullIndex() {
        return Ticks + (double) getTickIndex();
    }

    private static final int PIC_QUEUESIZE = 512;

    private class IRQBlock {
        public boolean masked;
        public boolean active;
        public boolean inservice;
        public int vector;
    }

    class PICController {
        public int IcwWords;
        public int IcwIndex;
        public int Masked;

        public boolean Special;
        public boolean AutoEOI;
        public boolean RotateOnAutoEOI;
        public boolean Single;
        public boolean RequestISSR;
        public byte vectorBase;
    }

    private static int Ticks = 0;
    public static long IRQCheck;
    private static long IRQOnSecondPicActive;
    private static int IRQActive;


    private static IRQBlock[] irqs = new IRQBlock[16];
    private static PICController[] pics = new PICController[2];
    private static boolean PICSpecialMode = false; // Saves one compare in the pic_run_irqloop

    public final class PICEntry {
        public float Index;
        public int Value;
        public EventHandler PICEvent;
        public PICEntry Next;
    }

    static class picQueue {
        public static PICEntry[] Entries = new PICEntry[PIC_QUEUESIZE];
        public static PICEntry freeEntry;
        public static PICEntry NextEntry;
    }

    private static short[] IRQPriorityTable =
            new short[] {0, 1, 2, 8, 9, 10, 11, 12, 13, 14, 15, 3, 4, 5, 6, 7};

    private static void writeCommand(int port, int val, int iolen) {
        PICController pic = pics[port == 0x20 ? 0 : 1];
        int irq_base = port == 0x20 ? 0 : 8;
        int i;

        if ((val & 0x10) != 0) { // ICW1 issued
            if ((val & 0x04) != 0)
                Support.exceptionExit("PIC: 4 byte interval not handled");
            if ((val & 0x08) != 0)
                Support.exceptionExit("PIC: level triggered mode not handled");
            if ((val & 0xe0) != 0)
                Support.exceptionExit("PIC: 8080/8085 mode not handled");
            pic.Single = (val & 0x02) == 0x02;
            pic.IcwIndex = 1; // next is ICW2
            pic.IcwWords = 2 + (val & 0x01); // =3 if ICW4 needed
        } else if ((val & 0x08) != 0) { // OCW3 issued
            if ((val & 0x04) != 0)
                Support.exceptionExit("PIC: poll command not handled");
            if ((val & 0x02) != 0) { // function select
                if ((val & 0x01) != 0)
                    pic.RequestISSR = true; /* select read interrupt in-service register */
                else
                    pic.RequestISSR = false; /* select read interrupt request register */
            }
            if ((val & 0x40) != 0) { // special mask select
                if ((val & 0x20) != 0)
                    pic.Special = true;
                else
                    pic.Special = false;
                if (pic.Special || pics[1].Special)
                    PICSpecialMode = true;
                else
                    PICSpecialMode = false;
                if (IRQCheck != 0) { // Recheck irqs
                    CPU.CycleLeft += CPU.Cycles;
                    CPU.Cycles = 0;
                }
                Log.logging(Log.LogTypes.PIC, Log.LogServerities.Normal,
                        "port %X : special mask %s", port, (pic.Special) ? "ON" : "OFF");
            }
        } else { // OCW2 issued
            if ((val & 0x20) != 0) { // EOI commands
                if ((val & 0x80) != 0)
                    Support.exceptionExit("rotate mode not supported");
                if ((val & 0x40) != 0) { // specific EOI
                    if (IRQActive == (irq_base + val - 0x60)) {
                        irqs[IRQActive].inservice = false;
                        IRQActive = PIC_NOIRQ;
                        for (i = 0; i <= 15; i++) {
                            if (irqs[IRQPriorityTable[i]].inservice) {
                                IRQActive = IRQPriorityTable[i];
                                break;
                            }
                        }
                    }
                    // if (val&0x80); // perform rotation
                } else { // nonspecific EOI
                    if (IRQActive < (irq_base + 8)) {
                        irqs[IRQActive].inservice = false;
                        IRQActive = PIC_NOIRQ;
                        for (i = 0; i <= 15; i++) {
                            if (irqs[IRQPriorityTable[i]].inservice) {
                                IRQActive = IRQPriorityTable[i];
                                break;
                            }
                        }
                    }
                    // if (val&0x80); // perform rotation
                }
            } else {
                if ((val & 0x40) == 0) { // rotate in auto EOI mode
                    if ((val & 0x80) != 0)
                        pic.RotateOnAutoEOI = true;
                    else
                        pic.RotateOnAutoEOI = false;
                } else if ((val & 0x80) != 0) {
                    Log.logging(Log.LogTypes.PIC, Log.LogServerities.Normal,
                            "set priority command not handled");
                } // else NOP command
            }
        } // end OCW2
    }

    private static void writeData(int port, int val, int iolen) {
        // PIC_Controller pic = pics[port == 0x21 ? 0 : 1];
        PICController pic = pics[port == 0x21 ? 0 : 1];
        int irq_base = (port == 0x21) ? 0 : 8;
        int i;
        boolean old_irq2_mask = irqs[2].masked;
        switch (pic.IcwIndex) {
            case 0: /* mask register */
                Log.logging(Log.LogTypes.PIC, Log.LogServerities.Normal, "%d mask %X",
                        port == 0x21 ? 0 : 1, val);
                for (i = 0; i <= 7; i++) {
                    irqs[i + irq_base].masked = (val & (1 << i)) > 0;
                    if (port == 0x21) {
                        if (irqs[i + irq_base].active && !irqs[i + irq_base].masked)
                            IRQCheck |= 0xffffffff & (1 << (i + irq_base));
                        else
                            IRQCheck &= 0xffffffff & ~(1 << (i + irq_base));
                    } else {
                        if (irqs[i + irq_base].active && !irqs[i + irq_base].masked
                                && !irqs[2].masked)
                            IRQCheck |= 0xffffffff & (1 << (i + irq_base));
                        else
                            IRQCheck &= 0xffffffff & ~(1 << (i + irq_base));
                    }
                }
                if (DOSBox.Machine == DOSBox.MachineType.PCJR) {
                    /* irq6 cannot be disabled as it serves as pseudo-NMI */
                    irqs[6].masked = false;
                }
                if (irqs[2].masked != old_irq2_mask) {
                    /* Irq 2 mask has changed recheck second pic */
                    for (i = 8; i <= 15; i++) {
                        if (irqs[i].active && !irqs[i].masked && !irqs[2].masked)
                            IRQCheck |= 0xffffffff & (1 << i);
                        else
                            IRQCheck &= 0xffffffff & ~(1 << i);
                    }
                }
                if (IRQCheck != 0) {
                    CPU.CycleLeft += CPU.Cycles;
                    CPU.Cycles = 0;
                }
                break;
            case 1: /* icw2 */
                Log.logging(Log.LogTypes.PIC, Log.LogServerities.Normal, "%d:Base vector %X",
                        port == 0x21 ? 0 : 1, val);
                for (i = 0; i <= 7; i++) {
                    irqs[i + irq_base].vector = (val & 0xf8) + i;
                }
                if (pic.IcwIndex++ >= pic.IcwWords)
                    pic.IcwIndex = 0;
                else if (pic.Single)
                    pic.IcwIndex = 3; /* skip ICW3 in single mode */
                break;
            case 2: /* icw 3 */
                Log.logging(Log.LogTypes.PIC, Log.LogServerities.Normal, "%d:ICW 3 %X",
                        port == 0x21 ? 0 : 1, val);
                if (pic.IcwIndex++ >= pic.IcwWords)
                    pic.IcwIndex = 0;
                break;
            case 3: /* icw 4 */
                /*
                 * 0 1 8086/8080 0 mcs-8085 mode 1 1 Auto EOI 0 Normal EOI 2-3 0x Non buffer Mode 10
                 * Buffer Mode Slave 11 Buffer mode Master 4 Special/Not Special nested mode
                 */
                pic.AutoEOI = (val & 0x2) > 0;

                Log.logging(Log.LogTypes.PIC, Log.LogServerities.Normal, "%d:ICW 4 %X",
                        port == 0x21 ? 0 : 1, val);

                if ((val & 0x01) == 0)
                    Support.exceptionExit("PIC:ICW4: %x, 8085 mode not handled", val);
                if ((val & 0x10) != 0)
                    Log.logMsg("PIC:ICW4: %x, special fully-nested mode not handled", val);

                if (pic.IcwIndex++ >= pic.IcwWords)
                    pic.IcwIndex = 0;
                break;
            default:
                Log.logging(Log.LogTypes.PIC, Log.LogServerities.Normal, "ICW HUH? %X", val);
                break;
        }
    }


    private static int readCommand(int port, int iolen) {
        PICController pic = pics[port == 0x20 ? 0 : 1];
        int irqBase = (port == 0x20) ? 0 : 8;
        int i;
        int ret = 0;// uint8
        int b = 1;// uint8
        if (pic.RequestISSR) {
            for (i = irqBase; i < irqBase + 8; i++) {
                if (irqs[i].inservice)
                    ret |= b;
                b <<= 1;
            }
        } else {
            for (i = irqBase; i < irqBase + 8; i++) {
                if (irqs[i].active)
                    ret |= b;
                b <<= 1;
            }
            if (irqBase == 0 && (IRQCheck & 0xff00) != 0)
                ret |= 4;
        }
        return 0xff & ret;
    }

    private static int readData(int port, int iolen) {
        int irqBase = (port == 0x21) ? 0 : 8;
        int i;
        int ret = 0;
        int b = 1;
        for (i = irqBase; i <= irqBase + 7; i++) {
            if (irqs[i].masked)
                ret |= b;
            b <<= 1;
        }
        return 0xff & ret;
    }

    public static void increaseTick() {
        Ticks++;
    }

    public static PICEntry getNextPICEntry() {
        return picQueue.NextEntry;
    }

    public static void activateIRQ(int irq) {
        if (irq < 8) {
            irqs[irq].active = true;
            if (!irqs[irq].masked) {
                IRQCheck |= 0xffffffff & (1 << irq);
            }
        } else if (irq < 16) {
            irqs[irq].active = true;
            IRQOnSecondPicActive |= 0xffffffff & (1 << irq);
            if (!irqs[irq].masked && !irqs[2].masked) {
                IRQCheck |= (0xffffffff & (1 << irq));
            }
        }
    }

    public static void deactivateIRQ(int irq) {
        if (irq < 16) {
            irqs[irq].active = false;
            IRQCheck &= (0xffffffff & ~(1 << irq));
            IRQOnSecondPicActive &= 0xffffffff & ~(1 << irq);
        }
    }

    private static boolean startIRQ(int i) {
        /* irqs on second pic only if irq 2 isn't masked */
        if (i > 7 && irqs[2].masked)
            return false;
        irqs[i].active = false;
        IRQCheck &= 0xffffffff & ~(1 << i);
        IRQOnSecondPicActive &= 0xffffffff & ~(1 << i);
        // Console.WriteLine(irqs[i].vector);
        CPU.hwHInterrupt(irqs[i].vector);
        int pic = (i & 8) >>> 3;
        if (!pics[pic].AutoEOI) { // irq 0-7 => pic 0 else pic 1
            IRQActive = i;
            irqs[i].inservice = true;
        } else if (pics[pic].RotateOnAutoEOI) {
            Support.exceptionExit("rotate on auto EOI not handled");
        }
        return true;
    }

    private static int[] IRQ_priority_order =
            {0, 1, 2, 8, 9, 10, 11, 12, 13, 14, 15, 3, 4, 5, 6, 7};
    private static short[] IRQ_priority_lookup =
            {0, 1, 2, 11, 12, 13, 14, 15, 3, 4, 5, 6, 7, 8, 9, 10, 16};

    private static void runIRQs() {
        if (Register.getFlag(Register.FlagIF) == 0)
            return;
        if (IRQCheck == 0)
            return;
        if (CPU.CpuDecoder == CoreNormal.instance().CpuTrapDecoder)
            return;

        int activeIRQ = IRQActive;
        if (activeIRQ == PIC_NOIRQ)
            activeIRQ = 16;
        /* Get the priority of the active irq */
        short Priority_Active_IRQ = IRQ_priority_lookup[activeIRQ];

        int i, j;
        /*
         * j is the priority (walker) i is the irq at the current priority
         */

        /* If one of the pics is in special mode use a check that cares for that. */
        if (!PICSpecialMode) {
            for (j = 0; j < Priority_Active_IRQ; j++) {
                i = IRQ_priority_order[j];
                if (!irqs[i].masked && irqs[i].active) {
                    if (startIRQ(i))
                        return;
                }
            }
        } else { /* Special mode variant */
            for (j = 0; j <= 15; j++) {
                i = IRQ_priority_order[j];
                if ((j < Priority_Active_IRQ) || (pics[((i & 8) >>> 3)].Special)) {
                    if (!irqs[i].masked && irqs[i].active) {
                        /*
                         * the irq line is active. it's not masked and the irq is allowed priority
                         * wise. So let's start it
                         */
                        /* If started successfully return, else go for the next */
                        if (startIRQ(i))
                            return;
                    }
                }
            }
        }
    }

    public static void setIRQMask(int irq, boolean masked) {
        if (irqs[irq].masked == masked)
            return; /* Do nothing if mask doesn't change */
        boolean old_irq2_mask = irqs[2].masked;
        irqs[irq].masked = masked;
        if (irq < 8) {
            if (irqs[irq].active && !irqs[irq].masked) {
                IRQCheck |= 0xffffffff & (1 << irq);
            } else {
                IRQCheck &= 0xffffffff & ~(1 << irq);
            }
        } else {
            if (irqs[irq].active && !irqs[irq].masked && !irqs[2].masked) {
                IRQCheck |= 0xffffffff & (1 << irq);
            } else {
                IRQCheck &= 0xffffffff & ~(1 << irq);
            }
        }
        if (irqs[2].masked != old_irq2_mask) {
            /* Irq 2 mask has changed recheck second pic */
            for (int i = 8; i <= 15; i++) {
                if (irqs[i].active && !irqs[i].masked && !irqs[2].masked)
                    IRQCheck |= 0xffffffff & (1 << i);
                else
                    IRQCheck &= 0xffffffff & ~(1 << i);
            }
        }
        if (IRQCheck != 0) {
            CPU.CycleLeft += CPU.Cycles;
            CPU.Cycles = 0;
        }
    }

    private static void addEntry(PICEntry entry) {
        PICEntry find_entry = picQueue.NextEntry;
        if (find_entry == null) {
            entry.Next = null;
            picQueue.NextEntry = entry;
        } else if (find_entry.Index > entry.Index) {
            picQueue.NextEntry = entry;
            entry.Next = find_entry;
        } else
            while (find_entry != null) {
                if (find_entry.Next != null) {
                    /* See if the next index comes later than this one */
                    if (find_entry.Next.Index > entry.Index) {
                        entry.Next = find_entry.Next;
                        find_entry.Next = entry;
                        break;
                    } else {
                        find_entry = find_entry.Next;
                    }
                } else {
                    entry.Next = find_entry.Next;
                    find_entry.Next = entry;
                    break;
                }
            }
        int cycles = makeCycles(picQueue.NextEntry.Index - getTickIndex());
        if (cycles < CPU.Cycles) {
            CPU.CycleLeft += CPU.Cycles;
            CPU.Cycles = 0;
        }
    }

    private static boolean InEventService = false;
    private static float srv_lag = 0;

    public static void addEvent(EventHandler handler, float delay, int val) {
        if (picQueue.freeEntry == null) {
            Log.logging(Log.LogTypes.PIC, Log.LogServerities.Error, "Event queue full");
            return;
        }
        PICEntry entry = picQueue.freeEntry;
        if (InEventService)
            entry.Index = delay + srv_lag;
        else
            entry.Index = delay + getTickIndex();

        entry.PICEvent = handler;
        entry.Value = val;
        picQueue.freeEntry = picQueue.freeEntry.Next;
        addEntry(entry);
    }

    public static void addEvent(EventHandler handler, float delay) {
        addEvent(handler, delay, 0);
    }

    public static void removeSpecificEvents(EventHandler handler, int val) {
        PICEntry entry = picQueue.NextEntry;
        PICEntry prev_entry;
        prev_entry = null;
        while (entry != null) {
            if ((entry.PICEvent == handler) && (entry.Value == val)) {
                if (prev_entry != null) {
                    prev_entry.Next = entry.Next;
                    entry.Next = picQueue.freeEntry;
                    picQueue.freeEntry = entry;
                    entry = prev_entry.Next;
                    continue;
                } else {
                    picQueue.NextEntry = entry.Next;
                    entry.Next = picQueue.freeEntry;
                    picQueue.freeEntry = entry;
                    entry = picQueue.NextEntry;
                    continue;
                }
            }
            prev_entry = entry;
            entry = entry.Next;
        }
    }

    public static void removeEvents(EventHandler handler) {
        PICEntry entry = picQueue.NextEntry;
        PICEntry prev_entry;
        prev_entry = null;
        while (entry != null) {
            if (entry.PICEvent == handler) {
                if (prev_entry != null) {
                    prev_entry.Next = entry.Next;
                    entry.Next = picQueue.freeEntry;
                    picQueue.freeEntry = entry;
                    entry = prev_entry.Next;
                    continue;
                } else {
                    picQueue.NextEntry = entry.Next;
                    entry.Next = picQueue.freeEntry;
                    picQueue.freeEntry = entry;
                    entry = picQueue.NextEntry;
                    continue;
                }
            }
            prev_entry = entry;
            entry = entry.Next;
        }
    }


    public static boolean runQueue() {
        // Console.WriteLine(CPUModule.CPU_Cycles);
        /* Check to see if a new milisecond needs to be started */
        CPU.CycleLeft += CPU.Cycles;
        CPU.Cycles = 0;
        if (CPU.CycleLeft <= 0) {
            return false;
        }
        /* Check the queue for an entry */
        int index_nd = getTickIndexND();
        InEventService = true;
        while (picQueue.NextEntry != null
                && (picQueue.NextEntry.Index * CPU.CycleMax <= index_nd)) {
            PICEntry entry = picQueue.NextEntry;
            picQueue.NextEntry = entry.Next;

            srv_lag = entry.Index;
            entry.PICEvent.raise(entry.Value); // call the event handler

            /* Put the entry in the free list */
            entry.Next = picQueue.freeEntry;
            picQueue.freeEntry = entry;
        }
        InEventService = false;

        /* Check when to set the new cycle end */
        if (picQueue.NextEntry != null) {
            int cycles = (int) (picQueue.NextEntry.Index * CPU.CycleMax - index_nd);
            if (cycles == 0)
                cycles = 1;
            if (cycles < CPU.CycleLeft) {
                CPU.Cycles = cycles;
            } else {
                CPU.Cycles = CPU.CycleLeft;
            }
        } else
            CPU.Cycles = CPU.CycleLeft;
        CPU.CycleLeft -= CPU.Cycles;
        if (IRQCheck != 0)
            runIRQs();
        return true;
    }

    private static PICModule _pic;

    private static void destroy(Section sec) {
        _pic.dispose();
        _pic = null;
    }

    public static void init(Section sec) {
        _pic = (new PIC()).new PICModule(sec);
        sec.addDestroyFunction(PIC::destroy);
    }

    /*--------------------------- begin PICModule -----------------------------*/
    protected final class PICModule extends ModuleBase {
        private IOReadHandleObject[] ReadHandler = new IOReadHandleObject[4];
        private IOWriteHandleObject[] WriteHandler = new IOWriteHandleObject[4];

        protected PICModule(Section configuration) {
            super(configuration);
            /* Setup pic0 and pic1 with initial values like DOS has normally */
            IRQCheck = 0;
            IRQActive = PIC_NOIRQ;
            Ticks = 0;
            int i;
            for (i = 0; i < 2; i++) {
                if (pics[i] == null)
                    pics[i] = new PICController();
                pics[i].Masked = 0xff;
                pics[i].AutoEOI = false;
                pics[i].RotateOnAutoEOI = false;
                pics[i].RequestISSR = false;
                pics[i].Special = false;
                pics[i].Single = false;
                pics[i].IcwIndex = 0;
                pics[i].IcwWords = 0;
            }
            for (i = 0; i <= 7; i++) {
                irqs[i].active = false;
                irqs[i].masked = true;
                irqs[i].inservice = false;
                irqs[i + 8].active = false;
                irqs[i + 8].masked = true;
                irqs[i + 8].inservice = false;
                irqs[i].vector = 0x8 + i;
                irqs[i + 8].vector = 0x70 + i;
            }
            irqs[0].masked = false; /* Enable system timer */
            irqs[1].masked = false; /* Enable Keyboard IRQ */
            irqs[2].masked = false; /* Enable second pic */
            irqs[8].masked = false; /* Enable RTC IRQ */
            if (DOSBox.Machine == DOSBox.MachineType.PCJR) {
                /* Enable IRQ6 (replacement for the NMI for PCJr) */
                irqs[6].masked = false;
            }
            for (i = 0; i < ReadHandler.length; i++) {
                ReadHandler[i] = new IOReadHandleObject();
            }
            for (i = 0; i < WriteHandler.length; i++) {
                WriteHandler[i] = new IOWriteHandleObject();
            }

            ReadHandler[0].install(0x20, PIC::readCommand, IO.IO_MB);
            ReadHandler[1].install(0x21, PIC::readData, IO.IO_MB);
            WriteHandler[0].install(0x20, PIC::writeCommand, IO.IO_MB);
            WriteHandler[1].install(0x21, PIC::writeData, IO.IO_MB);
            ReadHandler[2].install(0xa0, PIC::readCommand, IO.IO_MB);
            ReadHandler[3].install(0xa1, PIC::readData, IO.IO_MB);
            WriteHandler[2].install(0xa0, PIC::writeCommand, IO.IO_MB);
            WriteHandler[3].install(0xa1, PIC::writeData, IO.IO_MB);
            /* Initialize the pic queue */
            for (i = 0; i < PIC_QUEUESIZE - 1; i++) {
                if (picQueue.Entries[i] == null)
                    picQueue.Entries[i] = new PICEntry();
                if (picQueue.Entries[i + 1] == null)
                    picQueue.Entries[i + 1] = new PICEntry();
                picQueue.Entries[i].Next = picQueue.Entries[i + 1];
            }
            // if (pic_queue.entries[PIC_QUEUESIZE - 1] == null) pic_queue.entries[PIC_QUEUESIZE -
            // 1] = new PICEntry();
            picQueue.Entries[PIC_QUEUESIZE - 1].Next = null;
            picQueue.freeEntry = picQueue.Entries[0];
            picQueue.NextEntry = null;
        }
    }
    /*--------------------------- end PICModule -----------------------------*/

}
