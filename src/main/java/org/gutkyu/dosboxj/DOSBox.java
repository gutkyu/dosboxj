package org.gutkyu.dosboxj;

import org.gutkyu.dosboxj.dos.*;
import org.gutkyu.dosboxj.dos.system.drive.*;
import org.gutkyu.dosboxj.misc.*;
import org.gutkyu.dosboxj.misc.setup.*;
import org.gutkyu.dosboxj.misc.setup.Value.WrongType;
import org.gutkyu.dosboxj.cpu.*;
import org.gutkyu.dosboxj.gui.*;
import org.gutkyu.dosboxj.interrupt.*;
import org.gutkyu.dosboxj.interrupt.bios.*;
import org.gutkyu.dosboxj.interrupt.int10.*;
import org.gutkyu.dosboxj.dos.software.*;
import org.gutkyu.dosboxj.hardware.*;
import org.gutkyu.dosboxj.hardware.dma.*;
import org.gutkyu.dosboxj.hardware.io.*;
import org.gutkyu.dosboxj.hardware.memory.*;
import org.gutkyu.dosboxj.hardware.memory.paging.*;
import org.gutkyu.dosboxj.hardware.video.*;
import org.gutkyu.dosboxj.shell.*;
import org.gutkyu.dosboxj.dos.keyboardlayout.*;

public final class DOSBox {
    public static final String VERSION = "0.74";
    public static final String BASE_VERSION = "0.74";

    public enum MachineType {
        HERC, CGA, TANDY, PCJR, EGA, VGA
    }

    public enum SVGACards {
        None, S3Trio, TsengET4K, TsengET3K, ParadisePVGA1A
    }

    public static Config Control;
    public static MachineType Machine;
    public static SVGACards SVGACard;

    private static LoopHandler _loop;

    boolean SDLNetInited;

    private static int _ticksRemain;
    private static int _ticksLast;
    private static int _ticksAdded;
    public static int _ticksDone;
    public static int _ticksScheduled;
    public static boolean _ticksLocked;

    public static boolean isTANDYArch() {
        return ((Machine == MachineType.TANDY) || (Machine == MachineType.PCJR));
    }

    public static boolean isEGAVGAArch() {
        return ((Machine == MachineType.EGA) || (Machine == MachineType.VGA));
    }

    public static boolean isVGAArch() {
        return (Machine == MachineType.VGA);
    }

    // original 방식
    public static void runMachine() {
        int ret;
        do {
            ret = _loop.run();
        } while (ret == 0);
    }

    private static int normalLoop() {
        int ret;
        while (true) {
            if (PIC.runQueue()) {
                ret = CPU.CpuDecoder.decode();
                if (ret < 0)
                    return 1;
                if (ret > 0) {
                    int blah = (Callback.CallbackHandlers.get(ret)).run();
                    if (blah != 0)
                        return blah;
                }

            } else {
                GUIPlatform.gfx.GFXEvents();
                if (_ticksRemain > 0) {
                    Timer.addTick();
                    _ticksRemain--;
                } else {
                    increaseTicks();
                    return 0;
                }
            }
        }
    }

    private static void increaseTicks() {
        if (_ticksLocked) {
            _ticksRemain = 5;
            /* Reset any auto cycle guessing for this frame */
            _ticksLast = Timer.getTicks();
            _ticksAdded = 0;
            _ticksDone = 0;
            _ticksScheduled = 0;
        } else {
            int ticksNew;
            ticksNew = Timer.getTicks();
            _ticksScheduled += _ticksAdded;
            if (ticksNew > _ticksLast) {
                _ticksRemain = ticksNew - _ticksLast;
                _ticksLast = ticksNew;
                _ticksDone += _ticksRemain;
                if (_ticksRemain > 20) {
                    _ticksRemain = 20;
                }
                _ticksAdded = _ticksRemain;
                if (CPU.CycleAutoAdjust && !CPU.SkipCycleAutoAdjust) {
                    if (_ticksScheduled >= 250 || _ticksDone >= 250
                            || (_ticksAdded > 15 && _ticksScheduled >= 5)) {
                        if (_ticksDone < 1)
                            _ticksDone = 1; // Protect against div by zero
                        /* ratio we are aiming for is around 90% usage */
                        int ratio = (_ticksScheduled * (CPU.CyclePercUsed * 90 * 1024 / 100 / 100))
                                / _ticksDone;
                        int new_cmax = CPU.CycleMax;
                        long cproc = (long) CPU.CycleMax * (long) _ticksScheduled;
                        if (cproc > 0) {
                            /*
                             * ignore the cycles added due to the io delay code in order to have
                             * smoother auto cycle adjustments
                             */
                            double ratioremoved = (double) CPU.IODelayRemoved / (double) cproc;
                            if (ratioremoved < 1.0) {
                                ratio = (int) ((double) ratio * (1 - ratioremoved));
                                /*
                                 * Don't allow very high ratio which can cause us to lock as we
                                 * don't scale down for very low ratios. High ratio might result
                                 * because of timing resolution
                                 */
                                if (_ticksScheduled >= 250 && _ticksDone < 10 && ratio > 20480)
                                    ratio = 20480;
                                long cmax_scaled = (long) CPU.CycleMax * (long) ratio;
                                if (ratio <= 1024)
                                    new_cmax = (int) (cmax_scaled / 1024);
                                else
                                    new_cmax =
                                            (int) (1 + (CPU.CycleMax >>> 1) + cmax_scaled / 2048);
                            }
                        }

                        if (new_cmax < CPU.CyclesLowerLimit)
                            new_cmax = CPU.CyclesLowerLimit;

                        /*
                         * ratios below 1% are considered to be dropouts due to temporary load
                         * imbalance, the cycles adjusting is skipped
                         */
                        if (ratio > 10) {
                            /*
                             * ratios below 12% along with a large time since the last update has
                             * taken place are most likely caused by heavy load through a different
                             * application, the cycles adjusting is skipped as well
                             */
                            if ((ratio > 120) || (_ticksDone < 700)) {
                                CPU.CycleMax = new_cmax;
                                if (CPU.CycleLimit > 0) {
                                    if (CPU.CycleMax > CPU.CycleLimit)
                                        CPU.CycleMax = CPU.CycleLimit;
                                }
                            }
                        }
                        CPU.IODelayRemoved = 0;
                        _ticksDone = 0;
                        _ticksScheduled = 0;
                    } else if (_ticksAdded > 15) {
                        /*
                         * ticksAdded > 15 but ticksScheduled < 5, lower the cycles but do not reset
                         * the scheduled/done ticks to take them into account during the next auto
                         * cycle adjustment
                         */
                        CPU.CycleMax /= 3;
                        if (CPU.CycleMax < CPU.CyclesLowerLimit)
                            CPU.CycleMax = CPU.CyclesLowerLimit;
                    }
                }
            } else {
                _ticksAdded = 0;
                // SDL_Delay(1);
                _ticksDone -= Timer.getTicks() - ticksNew;
                if (_ticksDone < 0)
                    _ticksDone = 0;
            }
        }
    }

    private static void setLoop(LoopHandler handler) {
        _loop = handler;
    }

    private static boolean autoadjust = false;

    private static void unlockSpeed(boolean pressed) {
        if (pressed) {
            // LOG_MSG("Fast Forward ON");
            _ticksLocked = true;
            if (CPU.CycleAutoAdjust) {
                autoadjust = true;
                CPU.CycleAutoAdjust = false;
                CPU.CycleMax /= 3;
                if (CPU.CycleMax < 1000)
                    CPU.CycleMax = 1000;
            }
        } else {
            // LOG_MSG("Fast Forward OFF");
            _ticksLocked = false;
            if (autoadjust) {
                autoadjust = false;
                CPU.CycleAutoAdjust = true;
            }
        }
    }

    private static void realInit(Section sec) throws WrongType {
        SectionProperty section = (SectionProperty) sec;
        /* Initialize some dosbox internals */

        _ticksRemain = 0;
        _ticksLast = Timer.getTicks();
        _ticksLocked = false;
        setLoop(DOSBox::normalLoop);
        Message.init(section);

        GUIPlatform.mapper.addKeyHandler(DOSBox::unlockSpeed, MapKeys.F12, Mapper.MMOD2,
                "speedlock", "Speedlock");
        String cmd_machine = "";
        if ((cmd_machine = Control.CmdLine.findString("-machine", true)) != null) {
            // update value in config (else no matching against suggested values
            section.handleInputline("machine=" + cmd_machine);
        }

        String mType = section.getString("machine");
        SVGACard = DOSBox.SVGACards.None;
        Machine = DOSBox.MachineType.VGA;
        INT10.int10.VesaNoLFB = false;
        INT10.int10.VesaOldVbe = false;
        if (mType.equals("cga")) {
            Machine = DOSBox.MachineType.CGA;
        } else if (mType.equals("tandy")) {
            Machine = DOSBox.MachineType.TANDY;
        } else if (mType.equals("pcjr")) {
            Machine = DOSBox.MachineType.PCJR;
        } else if (mType.equals("hercules")) {
            Machine = DOSBox.MachineType.HERC;
        } else if (mType.equals("ega")) {
            Machine = DOSBox.MachineType.EGA;
        }
        // else if (mtype == "vga") { svgaCard = SVGA_S3Trio; }
        else if (mType.equals("svga_s3")) {
            SVGACard = DOSBox.SVGACards.S3Trio;
        } else if (mType.equals("vesa_nolfb")) {
            SVGACard = DOSBox.SVGACards.S3Trio;
            INT10.int10.VesaNoLFB = true;
        } else if (mType.equals("vesa_oldvbe")) {
            SVGACard = DOSBox.SVGACards.S3Trio;
            INT10.int10.VesaOldVbe = true;
        } else if (mType.equals("svga_et4000")) {
            SVGACard = DOSBox.SVGACards.TsengET4K;
        } else if (mType.equals("svga_et3000")) {
            SVGACard = DOSBox.SVGACards.TsengET3K;
        }
        // else if (mtype == "vga_pvga1a") { svgaCard = SVGA_ParadisePVGA1A; }
        else if (mType.equals("svga_paradise")) {
            SVGACard = DOSBox.SVGACards.ParadisePVGA1A;
        } else if (mType.equals("vgaonly")) {
            SVGACard = DOSBox.SVGACards.None;
        } else
            Support.exceptionExit("DOSBOX:Unknown machine type %s", mType);
    }

    public static void init() {
        SectionProperty secProp;
        SectionLine secLine;
        PropertyInt pInt;
        PropertyHex pHex;
        PropertyString pString;
        PropertyBool pBool;
        PropertyMultival pMulti;
        PropertyMultivalRemain pMultiRemain;

        // SDLNetInited = false;

        // Some frequently used option sets
        String[] rates = {"44100", "48000", "32000", "22050", "16000", "11025", "8000", "49716"};
        String[] oplrates = {"44100", "49716", "48000", "32000", "22050", "16000", "11025", "8000"};
        String[] ios = {"220", "240", "260", "280", "2a0", "2c0", "2e0", "300"};
        String[] irqssb = {"7", "5", "3", "9", "10", "11", "12"};
        String[] dmassb = {"1", "5", "0", "3", "6", "7"};
        String[] iosgus = {"240", "220", "260", "280", "2a0", "2c0", "2e0", "300"};
        String[] irqsgus = {"5", "3", "7", "9", "10", "11", "12"};
        String[] dmasgus = {"3", "0", "1", "5", "6", "7"};

        /* Setup all the different modules making up DOSBox */
        String[] machines = {"hercules", "cga", "tandy", "pcjr", "ega", "vgaonly", "svga_s3",
                "svga_et3000", "svga_et4000", "svga_paradise", "vesa_nolfb", "vesa_oldvbe"};
        secProp = Control.addSectionProp("dosbox", DOSBox::realInit);
        pString = secProp.addPath("language", Property.Changeable.Always, "");
        pString.setHelp("Select another language file.");

        pString = secProp.addString("machine", Property.Changeable.OnlyAtStart, "svga_s3");
        pString.setValues(machines);
        pString.setHelp("The type of machine tries to emulate.");

        pString = secProp.addPath("captures", Property.Changeable.Always, "capture");
        pString.setHelp("Directory where things like wave, midi, screenshot get captured.");
        /*
         * #if C_DEBUG LOG_StartUp(); #endif
         */
        secProp.addInitFunction(IOModule::init);// done
        secProp.addInitFunction(Paging::init);// done
        secProp.addInitFunction(Memory::init);// done
        // secprop.AddInitFunction(&HARDWARE_Init);//화면, 사운드 관련 캡쳐
        pInt = secProp.addInt("memsize", Property.Changeable.WhenIdle, 16);
        pInt.setMinMax(new Value(1), new Value(63));
        pInt.setHelp("Amount of memory DOSBox has in megabytes.\n"
                + "  This value is best left at its default to avoid problems with some games,\n"
                + "  though few games might require a higher value.\n"
                + "  There is generally no speed advantage when raising this value.");
        secProp.addInitFunction(Callback::init);
        secProp.addInitFunction(PIC::init);// done
        secProp.addInitFunction(Programs::init);
        secProp.addInitFunction(Timer::init);// done
        secProp.addInitFunction(CMOSModule::init);// done

        secProp = Control.addSectionProp("render", Render.instance()::init, true);
        pInt = secProp.addInt("frameskip", Property.Changeable.Always, 0);
        pInt.setMinMax(new Value(0), new Value(10));
        pInt.setHelp("How many frames DOSBox skips before drawing one.");

        pBool = secProp.addBool("aspect", Property.Changeable.Always, false);
        pBool.setHelp(
                "Do aspect correction, if your output method doesn't support scaling this can slow things down!.");

        pMulti = secProp.addMulti("scaler", Property.Changeable.Always, ' ');
        pMulti.setValue("normal2x");
        pMulti.setHelp("Scaler used to enlarge/enhance low resolution modes.\n"
                + "  If 'forced' is appended, then the scaler will be used even if the result might not be desired.");
        pString = pMulti.getSection().addString("type", Property.Changeable.Always, "normal2x");

        String[] scalers = {"none", "normal2x", "normal3x",
                // #if RENDER_USE_ADVANCED_SCALERS>2
                // "advmame2x", "advmame3x", "advinterp2x", "advinterp3x", "hq2x", "hq3x",
                // "2xsai",
                // "super2xsai", "supereagle",
                // #endif
                // #if RENDER_USE_ADVANCED_SCALERS>0
                // "tv2x", "tv3x", "rgb2x", "rgb3x", "scan2x", "scan3x",
                // #endif
        };
        pString.setValues(scalers);

        String[] force = {"", "forced"};
        pString = pMulti.getSection().addString("force", Property.Changeable.Always, "");
        pString.setValues(force);

        secProp = Control.addSectionProp("cpu", CPU::init, true);// done
        String[] cores = {"auto",
                /*
                 * #if (C_DYNAMIC_X86) || (C_DYNREC) "dynamic", #endif
                 */
                "normal", "simple"};
        pString = secProp.addString("core", Property.Changeable.WhenIdle, "auto");
        pString.setValues(cores);
        pString.setHelp(
                "CPU Core used in emulation. auto will switch to dynamic if available and appropriate.");

        String[] cputype_values =
                {"auto", "386", "386_slow", "486_slow", "pentium_slow", "386_prefetch",};
        pString = secProp.addString("cputype", Property.Changeable.Always, "auto");
        pString.setValues(cputype_values);
        pString.setHelp("CPU Type used in emulation. auto is the fastest choice.");

        pMultiRemain = secProp.addMultiRemain("cycles", Property.Changeable.Always, ' ');
        pMultiRemain.setHelp("Amount of instructions DOSBox tries to emulate each millisecond.\n"
                + "Setting this value too high results in sound dropouts and lags.\n"
                + "Cycles can be set in 3 ways:\n"
                + "  'auto'          tries to guess what a game needs.\n"
                + "                  It usually works, but can fail for certain games.\n"
                + "  'fixed #number' will set a fixed amount of cycles. This is what you usually need if 'auto' fails.\n"
                + "                  (Example: fixed 4000).\n"
                + "  'max'           will allocate as much cycles as your computer is able to handle.\n");

        String[] cyclest = {"auto", "fixed", "max", "%u"};
        pString = pMultiRemain.getSection().addString("type", Property.Changeable.Always, "auto");
        pMultiRemain.setValue("auto");
        pString.setValues(cyclest);

        pString = pMultiRemain.getSection().addString("parameters", Property.Changeable.Always, "");

        pInt = secProp.addInt("cycleup", Property.Changeable.Always, 10);
        pInt.setMinMax(new Value(1), new Value(1000000));
        pInt.setHelp("Amount of cycles to decrease/increase with keycombo.(CTRL-F11/CTRL-F12)");

        pInt = secProp.addInt("cycledown", Property.Changeable.Always, 20);
        pInt.setMinMax(new Value(1), new Value(1000000));
        pInt.setHelp("Setting it lower than 100 will be a percentage.");
        /*
         * #if C_FPU secprop.AddInitFunction(&FPU_Init); #endif
         */
        secProp.addInitFunction(DMAModule::init);// done
        secProp.addInitFunction(VGA::init);
        secProp.addInitFunction(Keyboard::init);

        // TODO have to implement "mixer, midi, debug, sblaster, gus, speaker"
        /*
         * secprop=control.AddSection_prop("mixer",&MIXER_Init); ... Pbool.
         * Set_help("Enable Disney Sound Source emulation. (Covox Voice Master and Speech Thing compatible)."
         * );
         */

        secProp = Control.addSectionProp("joystick", BIOS::init, false);// done

        secProp.addInitFunction(INT10::init);

        secProp.addInitFunction(Mouse.instance()::init); // Must be after int10 as it uses CurMode

        // TODO have to implement "JOYSTICK_Init"
        /*
         * secprop.AddInitFunction(&JOYSTICK_Init); ...
         */

        // TODO have to implement "serial"
        /*
         * secprop=control.AddSection_prop("serial",&SERIAL_Init,true); ...
         */

        /* All the DOS Related stuff, which will eventually start up in the shell */
        secProp = Control.addSectionProp("dos", DOSMain::init, false);// done
        secProp.addInitFunction(XMS::init, true);// done
        pBool = secProp.addBool("xms", Property.Changeable.WhenIdle, true);
        pBool.setHelp("Enable XMS support.");

        secProp.addInitFunction(EMS::init, true);// done
        pBool = secProp.addBool("ems", Property.Changeable.WhenIdle, true);
        pBool.setHelp("Enable EMS support.");

        pBool = secProp.addBool("umb", Property.Changeable.WhenIdle, true);
        pBool.setHelp("Enable UMB support.");

        secProp.addInitFunction(DOSKeyboardLayoutModule::init, true);
        pString = secProp.addString("keyboardlayout", Property.Changeable.WhenIdle, "auto");
        pString.setHelp("Language code of the keyboard layout (or none).");

        // Mscdex
        // TODO have to implement "MSCDEX_Init"
        secProp.addInitFunction(Drives::initDrives);
        // TODO have to implement "CDROM_Image_Init"
        // secprop.AddInitFunction(&CDROM_Image_Init);
        // TODO have to implement "ipx"
        /*
         * #if C_IPX secprop=control.AddSection_prop("ipx",&IPX_Init,true); Pbool =
         * secprop.Add_bool("ipx",Property.Changeable.WhenIdle, false);
         * Pbool.Set_help("Enable ipx over UDP/IP emulation."); #endif
         */
        // TODO have to implement "CREDITS_Init"
        // secprop.AddInitFunction(&CREDITS_Init);

        // TODO ?
        secLine = Control.addSectionLine("autoexec", Autoexec::init);
        Message.addMsg("AUTOEXEC_CONFIGFILE_HELP", "Lines in this section will be run at startup.\n"
                + "You can put your MOUNT lines here.\n");
        Message.addMsg("CONFIGFILE_INTRO",
                "# This is the configurationfile for DOSBox %s. (Please use the latest version of DOSBox)\n"
                        + "# Lines starting with a # are commentlines and are ignored by DOSBox.\n"
                        + "# They are used to (briefly) document the effect of each option.\n");
        Message.addMsg("CONFIG_SUGGESTED_VALUES", "Possible values");

        Control.setStartUp(DOSShell::init);

    }

}
