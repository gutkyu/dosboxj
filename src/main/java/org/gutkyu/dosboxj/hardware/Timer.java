package org.gutkyu.dosboxj.hardware;

import org.gutkyu.dosboxj.misc.*;
import org.gutkyu.dosboxj.cpu.*;
import org.gutkyu.dosboxj.misc.setup.*;
import org.gutkyu.dosboxj.util.*;
import org.gutkyu.dosboxj.hardware.io.IO;
import org.gutkyu.dosboxj.hardware.io.iohandler.*;

public final class Timer {

    private Timer() {

    }

    public static final int PIT_TICK_RATE = 1193182;

    // SDL_GetTicks는 SDL library 초기화했을 때 부터 소요된 밀리초
    // 현재시간을 밀리초로 표시, 과거 특정일 기준으로 소요된 밀리초, SDL_GetTicks과 다른 값이지만 사용상에 문제없음
    public static int getTicks() {
        return (int) System.currentTimeMillis();
    }


    private static short doBIN2BCD(short val) {
        return (short) (val % 10 + (((val / 10) % 10) << 4) + (((val / 100) % 10) << 8)
                + (((val / 1000) % 10) << 12));
    }

    private static short doBCD2BIN(short val) {
        return (short) ((val & 0x0f) + ((val >>> 4) & 0x0f) * 10 + ((val >>> 8) & 0x0f) * 100
                + ((val >>> 12) & 0x0f) * 1000);
    }

    class PIT_Block {
        public int cntr;
        public float delay;
        public double start;

        public short read_latch;
        public short write_latch;

        public byte mode;
        public byte latch_mode;
        public byte read_state;
        public byte write_state;

        public boolean bcd;
        public boolean go_read_latch;
        public boolean new_mode;
        public boolean counterstatus_set;
        public boolean counting;
        public boolean update_count;
    }

    private static PIT_Block[] pit = new PIT_Block[3];
    private static boolean gate2;

    private static byte latched_timerstatus;
    // the timer status can not be overwritten until it is read or the timer was
    // reprogrammed.
    private static boolean latched_timerstatus_locked;

    private static EventHandler pit0EventWrap = Timer::PIT0Event;

    private static void PIT0Event(int val) {
        PIC.activateIRQ(0);
        if (pit[0].mode != 0) {
            pit[0].start += pit[0].delay;

            if (pit[0].update_count) {
                pit[0].delay = (1000.0f / ((float) PIT_TICK_RATE / (float) pit[0].cntr));
                pit[0].update_count = false;
            }
            PIC.addEvent(pit0EventWrap, pit[0].delay);
        }
    }

    private static boolean doCounterOutput(int counter) {
        PIT_Block p = pit[counter];
        double index = PIC.getFullIndex() - p.start;
        switch (p.mode) {
            case 0:
                if (p.new_mode)
                    return false;
                if (index > p.delay)
                    return true;
                else
                    return false;
                // break;
            case 2:
                if (p.new_mode)
                    return true;
                index = index % (double) p.delay;
                return index > 0;
            case 3:
                if (p.new_mode)
                    return true;
                index = index % (double) p.delay;
                return index * 2 < p.delay;
            case 4:
                // Only low on terminal count
                // if(fmod(index,(double)p.delay) == 0) return false; //Maybe take one rate tick in
                // consideration
                // Easiest solution is to report always high (Space marines uses this mode)
                return true;
            default:
                Log.logging(Log.LogTypes.PIT, Log.LogServerities.Error,
                        "Illegal Mode %d for reading output", p.mode);
                return true;
        }
    }

    private static void doStatusLatch(int counter) {
        // the timer status can not be overwritten until it is read or the timer was
        // reprogrammed.
        if (!latched_timerstatus_locked) {
            PIT_Block p = pit[counter];
            latched_timerstatus = 0;
            // Timer Status Word
            // 0: BCD
            // 1-3: Timer mode
            // 4-5: read/load mode
            // 6: "NULL" - this is 0 if "the counter value is in the counter" ;)
            // should rarely be 1 (i.e. on exotic modes)
            // 7: OUT - the logic level on the Timer output pin
            if (p.bcd)
                latched_timerstatus |= 0x1;
            latched_timerstatus |= (byte) ((p.mode & 7) << 1);
            if ((p.read_state == 0) || (p.read_state == 3))
                latched_timerstatus |= 0x30;
            else if (p.read_state == 1)
                latched_timerstatus |= 0x10;
            else if (p.read_state == 2)
                latched_timerstatus |= 0x20;
            if (doCounterOutput(counter))
                latched_timerstatus |= 0x80;
            if (p.new_mode)
                latched_timerstatus |= 0x40;
            // The first thing that is being read from this counter now is the
            // counter status.
            p.counterstatus_set = true;
            latched_timerstatus_locked = true;
        }
    }

    private static void doCounterLatch(int counter) {
        /* Fill the read_latch of the selected counter with current count */
        PIT_Block p = pit[counter];
        p.go_read_latch = false;

        // If gate2 is disabled don't update the read_latch
        if (counter == 2 && !gate2 && p.mode != 1)
            return;

        double index = PIC.getFullIndex() - p.start;
        switch (p.mode) {
            case 4: /* Software Triggered Strobe */
            case 0: /* Interrupt on Terminal Count */
                /* Counter keeps on counting after passing terminal count */
                if (index > p.delay) {
                    index -= p.delay;
                    if (p.bcd) {
                        index = index % (1000.0 / PIT_TICK_RATE) * 10000.0;
                        p.read_latch = (short) (9999 - index * (PIT_TICK_RATE / 1000.0));
                    } else {
                        index = index % (1000.0 / PIT_TICK_RATE) * (double) 0x10000;
                        p.read_latch = (short) (0xffff - index * (PIT_TICK_RATE / 1000.0));
                    }
                } else {
                    p.read_latch = (short) (p.cntr - index * (PIT_TICK_RATE / 1000.0));
                }
                break;
            case 1: // countdown
                if (p.counting) {
                    if (index > p.delay) { // has timed out
                        p.read_latch = (short) 0xffff; // unconfirmed
                    } else {
                        p.read_latch = (short) (p.cntr - index * (PIT_TICK_RATE / 1000.0));
                    }
                }
                break;
            case 2: /* Rate Generator */
                index = index % (double) p.delay;
                p.read_latch = (short) (p.cntr - (index / p.delay) * p.cntr);
                break;
            case 3: /* Square Wave Rate Generator */
                index = index % (double) p.delay;
                index *= 2;
                if (index > p.delay)
                    index -= p.delay;
                p.read_latch = (short) (p.cntr - (index / p.delay) * p.cntr);
                // In mode 3 it never returns odd numbers LSB (if odd number is written 1 will be
                // subtracted on first clock and then always 2)
                // fixes "Corncob 3D"
                p.read_latch &= 0xfffe;
                break;
            default:
                Log.logging(Log.LogTypes.PIT, Log.LogServerities.Error,
                        "Illegal Mode %d for reading counter %d", p.mode, counter);
                p.read_latch = (short) 0xffff;
                break;
        }
    }


    private static void writeLatch(int port, int val, int iolen) {
        // LOG(LOG_PIT,LOG_ERROR)("port %X write:%X state:%X",port,val,pit[port-0x40].write_state);
        int counter = port - 0x40;
        PIT_Block p = pit[counter];
        if (p.bcd == true)
            p.write_latch = doBIN2BCD(p.write_latch);

        switch (p.write_state) {
            case 0:
                p.write_latch = (short) (p.write_latch | ((val & 0xff) << 8));
                p.write_state = 3;
                break;
            case 3:
                p.write_latch = (short) (val & 0xff);
                p.write_state = 0;
                break;
            case 1:
                p.write_latch = (short) (val & 0xff);
                break;
            case 2:
                p.write_latch = (short) ((val & 0xff) << 8);
                break;
        }
        if (p.bcd == true)
            p.write_latch = doBCD2BIN(p.write_latch);
        if (p.write_state != 0) {
            if (p.write_latch == 0) {
                if (p.bcd == false)
                    p.cntr = 0x10000;
                else
                    p.cntr = 9999;
            } else
                p.cntr = p.write_latch;

            if ((!p.new_mode) && (p.mode == 2) && (counter == 0)) {
                // In mode 2 writing another value has no direct effect on the count
                // until the old one has run out. This might apply to other modes too.
                // This is not fixed for PIT2 yet!!
                p.update_count = true;
                return;
            }
            p.start = PIC.getFullIndex();
            p.delay = (1000.0f / ((float) PIT_TICK_RATE / (float) p.cntr));

            switch (counter) {
                case 0x00: /* Timer hooked to IRQ 0 */
                    if (p.new_mode || p.mode == 0) {
                        if (p.mode == 0)
                            PIC.removeEvents(pit0EventWrap); // DoWhackaDo demo
                        PIC.addEvent(pit0EventWrap, p.delay);
                    } else
                        Log.logging(Log.LogTypes.PIT, Log.LogServerities.Normal,
                                "PIT 0 Timer set without new control word");
                    Log.logging(Log.LogTypes.PIT, Log.LogServerities.Normal,
                            "PIT 0 Timer at %.4f Hz mode %d", 1000.0 / p.delay, p.mode);
                    break;
                case 0x02: /* Timer hooked to PC-Speaker */
                    // LOG(LOG_PIT,"PIT 2 Timer at %.3g Hz mode
                    // %d",PIT_TICK_RATE/(double)p.cntr,p.mode);

                    // 일단 사운드 부분은 생략
                    // TODO
                    // PCSPEAKER_SetCounter(p.cntr, p.mode);
                    break;
                default:
                    Log.logging(Log.LogTypes.PIT, Log.LogServerities.Error,
                            "PIT:Illegal timer selected for writing");
                    break;
            }
            p.new_mode = false;
        }
    }

    private static int readLatch(int port, int iolen) {
        // LOG(LOG_PIT,LOG_ERROR)("port read %X",port);
        int counter = port - 0x40;
        byte ret = 0;
        if (pit[counter].counterstatus_set) {
            pit[counter].counterstatus_set = false;
            latched_timerstatus_locked = false;
            ret = latched_timerstatus;
        } else {
            if (pit[counter].go_read_latch == true)
                doCounterLatch(counter);

            if (pit[counter].bcd == true)
                pit[counter].read_latch = doBIN2BCD(pit[counter].read_latch);

            switch (pit[counter].read_state) {
                case 0: /* read MSB & return to state 3 */
                    ret = (byte) ((pit[counter].read_latch >>> 8) & 0xff);
                    pit[counter].read_state = 3;
                    pit[counter].go_read_latch = true;
                    break;
                case 3: /* read LSB followed by MSB */
                    ret = (byte) (pit[counter].read_latch & 0xff);
                    pit[counter].read_state = 0;
                    break;
                case 1: /* read LSB */
                    ret = (byte) (pit[counter].read_latch & 0xff);
                    pit[counter].go_read_latch = true;
                    break;
                case 2: /* read MSB */
                    ret = (byte) ((pit[counter].read_latch >>> 8) & 0xff);
                    pit[counter].go_read_latch = true;
                    break;
                default:
                    Support.exceptionExit("Timer.cpp: error in readlatch");
                    break;
            }
            if (pit[counter].bcd == true)
                pit[counter].read_latch = doBCD2BIN(pit[counter].read_latch);
        }
        return ret;
    }

    private static void writeP43(int port, int val, int iolen) {
        // LOG(LOG_PIT,LOG_ERROR)("port 43 %X",val);
        int latch = (val >>> 6) & 0x03;
        switch (latch) {
            case 0:
            case 1:
            case 2:
                if ((val & 0x30) == 0) {
                    /* Counter latch command */
                    doCounterLatch(latch);
                } else {
                    pit[latch].bcd = (val & 1) > 0;
                    if ((val & 1) != 0) {
                        if (pit[latch].cntr >= 9999)
                            pit[latch].cntr = 9999;
                    }

                    // Timer is being reprogrammed, unlock the status
                    if (pit[latch].counterstatus_set) {
                        pit[latch].counterstatus_set = false;
                        latched_timerstatus_locked = false;
                    }
                    pit[latch].update_count = false;
                    pit[latch].counting = false;
                    pit[latch].read_state = (byte) ((val >>> 4) & 0x03);
                    pit[latch].write_state = (byte) ((val >>> 4) & 0x03);
                    byte mode = (byte) ((val >>> 1) & 0x07);
                    if (mode > 5)
                        mode -= 4; // 6,7 become 2 and 3

                    /* Don't set it directly so counter_output uses the old mode */
                    /* That's theory. It breaks panic. So set it here again */
                    if (pit[latch].mode == 0)
                        pit[latch].mode = mode;

                    /*
                     * If the line goes from low to up => generate irq. ( BUT needs to stay up until
                     * acknowlegded by the cpuModule.cpu!!! therefore: ) If the line goes to low =>
                     * disable irq. Mode 0 starts with a low line. (so always disable irq) Mode 2,3
                     * start with a high line. counter_output tells if the current counter is high
                     * or low So actually a mode 2 timer enables and disables irq al the time. (not
                     * handled)
                     */

                    if (latch == 0) {
                        PIC.removeEvents(Timer::PIT0Event);
                        if (!doCounterOutput(0) && mode != 0) {
                            PIC.activateIRQ(0);
                            // Don't raise instantaniously. (Origamo)
                            if (CPU.Cycles < 25)
                                CPU.Cycles = 25;
                        }
                        if (mode == 0)
                            PIC.deactivateIRQ(0);
                    }
                    pit[latch].new_mode = true;
                    pit[latch].mode = mode; // Set the correct mode (here)
                }
                break;
            case 3:
                if ((val & 0x20) == 0) { /* Latch multiple pit counters */
                    if ((val & 0x02) != 0)
                        doCounterLatch(0);
                    if ((val & 0x04) != 0)
                        doCounterLatch(1);
                    if ((val & 0x08) != 0)
                        doCounterLatch(2);
                }
                // status and values can be latched simultaneously
                if ((val & 0x10) == 0) { /* Latch status words */
                    // but only 1 status can be latched simultaneously
                    if ((val & 0x02) != 0)
                        doStatusLatch(0);
                    else if ((val & 0x04) != 0)
                        doStatusLatch(1);
                    else if ((val & 0x08) != 0)
                        doStatusLatch(2);
                }
                break;
        }
    }

    public static void setGate2(boolean _in) {
        // No changes if gate doesn't change
        if (gate2 == _in)
            return;
        byte mode = pit[2].mode;
        switch (mode) {
            case 0:
                if (_in)
                    pit[2].start = PIC.getFullIndex();
                else {
                    // Fill readlatch and store it.
                    doCounterLatch(2);
                    pit[2].cntr = pit[2].read_latch;
                }
                break;
            case 1:
                // gate 1 on: reload counter; off: nothing
                if (_in) {
                    pit[2].counting = true;
                    pit[2].start = PIC.getFullIndex();
                }
                break;
            case 2:
            case 3:
                // If gate is enabled restart counting. If disable store the current read_latch
                if (_in)
                    pit[2].start = PIC.getFullIndex();
                else
                    doCounterLatch(2);
                break;
            case 4:
            case 5:
                Log.logging(Log.LogTypes.MISC, Log.LogServerities.Warn,
                        "unsupported gate 2 mode %x", mode);
                break;
        }
        gate2 = _in; // Set it here so the counter_latch above works
    }

    /* The TIMER Part */
    private static class TickerBlock {
        public TickHandler handler;
        public TickerBlock next;
    }

    private static TickerBlock firstTicker = null;


    public static void delTickHandler(TickHandler handler) {
        TickerBlock ticker = firstTicker;
        TickerBlock preTicker = null;
        while (ticker != null) {
            if (ticker.handler == handler) {
                if (preTicker == null)
                    firstTicker = null;
                else
                    preTicker.next = ticker.next;
                ticker = null;
                return;
            }
            preTicker = ticker;
            ticker = ticker.next;
        }
    }

    public static void addTickHandler(TickHandler handler) {
        TickerBlock newTicker = new TickerBlock();
        newTicker.next = firstTicker;
        newTicker.handler = handler;
        firstTicker = newTicker;
    }

    public static void addTick() {
        /* Setup new amount of cycles for PIC */
        CPU.CycleLeft = CPU.CycleMax;
        CPU.Cycles = 0;
        PIC.increaseTick();
        /* Go through the list of scheduled events and lower their index with 1000 */
        PIC.PICEntry entry = PIC.getNextPICEntry();
        while (entry != null) {
            entry.Index -= 1.0F;
            // entry.index = (float)(((int)(entry.index * 10000) - 10000) / 10000);
            entry = entry.Next;
        }
        /* Call our list of ticker handlers */
        TickerBlock ticker = firstTicker;
        while (ticker != null) {
            TickerBlock nextticker = ticker.next;
            ticker.handler.run();
            ticker = nextticker;
        }
    }

    private static TimerModule _timer;

    private static void destroy(Section sec) {
        _timer.dispose();
        _timer = null;
    }

    public static void init(Section sec) {
        _timer = (new Timer()).new TimerModule(sec);
        sec.addDestroyFunction(Timer::destroy);
    }

    /*--------------------------- begin TimerModule -----------------------------*/
    public final class TimerModule extends ModuleBase {
        private IOReadHandleObject[] ReadHandler = new IOReadHandleObject[4];
        private IOWriteHandleObject[] WriteHandler = new IOWriteHandleObject[4];

        public TimerModule(Section configuration) {
            super(configuration);
            for (int i = 0; i < ReadHandler.length; i++) {
                ReadHandler[i] = new IOReadHandleObject();
            }
            for (int i = 0; i < WriteHandler.length; i++) {
                WriteHandler[i] = new IOWriteHandleObject();
            }

            WriteHandler[0].install(0x40, Timer::writeLatch, IO.IO_MB);
            // WriteHandler[1].Install(0x41,write_latch,iohandler.IO_MB);
            WriteHandler[2].install(0x42, Timer::writeLatch, IO.IO_MB);
            WriteHandler[3].install(0x43, Timer::writeP43, IO.IO_MB);
            ReadHandler[0].install(0x40, Timer::readLatch, IO.IO_MB);
            ReadHandler[1].install(0x41, Timer::readLatch, IO.IO_MB);
            ReadHandler[2].install(0x42, Timer::readLatch, IO.IO_MB);
            /* Setup Timer 0 */
            for (int i = 0; i < pit.length; i++) {
                pit[i] = new PIT_Block();
            }
            pit[0].cntr = 0x10000;
            pit[0].write_state = 3;
            pit[0].read_state = 3;
            pit[0].read_latch = 0;
            pit[0].write_latch = 0;
            pit[0].mode = 3;
            pit[0].bcd = false;
            pit[0].go_read_latch = true;
            pit[0].counterstatus_set = false;
            pit[0].update_count = false;

            pit[1].bcd = false;
            pit[1].write_state = 1;
            pit[1].read_state = 1;
            pit[1].go_read_latch = true;
            pit[1].cntr = 18;
            pit[1].mode = 2;
            pit[1].write_state = 3;
            pit[1].counterstatus_set = false;

            pit[2].read_latch = 1320; /* MadTv1 */
            pit[2].write_state = 3; /* Chuck Yeager */
            pit[2].read_state = 3;
            pit[2].mode = 3;
            pit[2].bcd = false;
            pit[2].cntr = 1320;
            pit[2].go_read_latch = true;
            pit[2].counterstatus_set = false;
            pit[2].counting = false;

            pit[0].delay = (1000.0f / ((float) PIT_TICK_RATE / (float) pit[0].cntr));
            pit[1].delay = (1000.0f / ((float) PIT_TICK_RATE / (float) pit[1].cntr));
            pit[2].delay = (1000.0f / ((float) PIT_TICK_RATE / (float) pit[2].cntr));

            latched_timerstatus_locked = false;
            gate2 = false;
            PIC.addEvent(Timer::PIT0Event, pit[0].delay);
        }

        @Override
        protected void dispose(boolean disposing) {
            if (disposing) {

            }
            PIC.removeEvents(Timer::PIT0Event);
            super.dispose(disposing);
        }
    }
    /*--------------------------- end TimerModule -----------------------------*/

}
