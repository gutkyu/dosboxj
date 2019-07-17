package org.gutkyu.dosboxj.shell;

import org.gutkyu.dosboxj.dos.*;
import org.gutkyu.dosboxj.dos.DOSMain.FCBParseNameParamRef;
import java.util.*;
import org.gutkyu.dosboxj.*;
import org.gutkyu.dosboxj.misc.*;
import org.gutkyu.dosboxj.util.*;
import org.gutkyu.dosboxj.hardware.memory.*;
import org.gutkyu.dosboxj.dos.system.drive.*;
import org.gutkyu.dosboxj.dos.mem_block.*;
import org.gutkyu.dosboxj.misc.setup.*;
import org.gutkyu.dosboxj.cpu.*;


public final class DOSShell extends DOSShellBase {

    public DOSShell() {
        super();
        InputHandle = DOSMain.STDIN;
        Echo = true;
        Exit = false;
        BatFile = null;
        Call = false;
        if (cmdList == null)
            initCMDList();
    }
    /*--------------------------- begin DOSShellCmds -----------------------------*/

    private static final class ShellCmd {
        public final String Name; /* Command name */
        public final int Flags; /* Flags about the command */
        public final DOSAction1<CStringPt> Handler; /* Handler for this command */
        public final String Help; /* String with command help */

        public ShellCmd(String name, int flags, DOSAction1<CStringPt> handler, String help) {
            this.Name = name;
            this.Flags = flags;
            this.Handler = handler;
            this.Help = help;
        }
    }

    protected static TreeMap<String, ShellCmd> cmdList = null;

    private void initCMDList() {
        cmdList = new TreeMap<String, ShellCmd>(String.CASE_INSENSITIVE_ORDER);
        cmdList.put("DIR", new ShellCmd("DIR", 0, this::cmdDIR, "SHELL_CMD_DIR_HELP"));
        cmdList.put("CHDIR", new ShellCmd("CHDIR", 1, this::cmdCHDIR, "SHELL_CMD_CHDIR_HELP"));
        cmdList.put("ATTRIB", new ShellCmd("ATTRIB", 1, this::cmdATTRIB, "SHELL_CMD_ATTRIB_HELP"));
        cmdList.put("CALL", new ShellCmd("CALL", 1, this::cmdCALL, "SHELL_CMD_CALL_HELP"));
        cmdList.put("CD", new ShellCmd("CD", 0, this::cmdCHDIR, "SHELL_CMD_CHDIR_HELP"));
        cmdList.put("CHOICE", new ShellCmd("CHOICE", 1, this::cmdCHOICE, "SHELL_CMD_CHOICE_HELP"));
        cmdList.put("CLS", new ShellCmd("CLS", 0, this::cmdCLS, "SHELL_CMD_CLS_HELP"));
        cmdList.put("COPY", new ShellCmd("COPY", 0, this::cmdCOPY, "SHELL_CMD_COPY_HELP"));
        cmdList.put("DEL", new ShellCmd("DEL", 0, this::cmdDELETE, "SHELL_CMD_DELETE_HELP"));
        cmdList.put("DELETE", new ShellCmd("DELETE", 1, this::cmdDELETE, "SHELL_CMD_DELETE_HELP"));
        cmdList.put("ERASE", new ShellCmd("ERASE", 1, this::cmdDELETE, "SHELL_CMD_DELETE_HELP"));
        cmdList.put("ECHO", new ShellCmd("ECHO", 1, this::cmdECHO, "SHELL_CMD_ECHO_HELP"));
        cmdList.put("EXIT", new ShellCmd("EXIT", 0, this::cmdEXIT, "SHELL_CMD_EXIT_HELP"));
        cmdList.put("GOTO", new ShellCmd("GOTO", 1, this::cmdGOTO, "SHELL_CMD_GOTO_HELP"));
        cmdList.put("HELP", new ShellCmd("HELP", 1, this::cmdHELP, "SHELL_CMD_HELP_HELP"));
        cmdList.put("IF", new ShellCmd("IF", 1, this::cmdIF, "SHELL_CMD_IF_HELP"));
        cmdList.put("LOADHIGH",
                new ShellCmd("LOADHIGH", 1, this::cmdLOADHIGH, "SHELL_CMD_LOADHIGH_HELP"));
        cmdList.put("LH", new ShellCmd("LH", 1, this::cmdLOADHIGH, "SHELL_CMD_LOADHIGH_HELP"));
        cmdList.put("MKDIR", new ShellCmd("MKDIR", 1, this::cmdMKDIR, "SHELL_CMD_MKDIR_HELP"));
        cmdList.put("MD", new ShellCmd("MD", 0, this::cmdMKDIR, "SHELL_CMD_MKDIR_HELP"));
        cmdList.put("PATH", new ShellCmd("PATH", 1, this::cmdPATH, "SHELL_CMD_PATH_HELP"));
        cmdList.put("PAUSE", new ShellCmd("PAUSE", 1, this::cmdPAUSE, "SHELL_CMD_PAUSE_HELP"));
        cmdList.put("RMDIR", new ShellCmd("RMDIR", 1, this::cmdRMDIR, "SHELL_CMD_RMDIR_HELP"));
        cmdList.put("RD", new ShellCmd("RD", 0, this::cmdRMDIR, "SHELL_CMD_RMDIR_HELP"));
        cmdList.put("REM", new ShellCmd("REM", 1, this::cmdREM, "SHELL_CMD_REM_HELP"));
        cmdList.put("RENAME", new ShellCmd("RENAME", 1, this::cmdRENAME, "SHELL_CMD_RENAME_HELP"));
        cmdList.put("REN", new ShellCmd("REN", 0, this::cmdRENAME, "SHELL_CMD_RENAME_HELP"));
        cmdList.put("SET", new ShellCmd("SET", 1, this::cmdSET, "SHELL_CMD_SET_HELP"));
        cmdList.put("SHIFT", new ShellCmd("SHIFT", 1, this::cmdSHIFT, "SHELL_CMD_SHIFT_HELP"));
        cmdList.put("SUBST", new ShellCmd("SUBST", 1, this::cmdSUBST, "SHELL_CMD_SUBST_HELP"));
        cmdList.put("TYPE", new ShellCmd("TYPE", 0, this::cmdTYPE, "SHELL_CMD_TYPE_HELP"));
        cmdList.put("VER", new ShellCmd("VER", 0, this::cmdVER, "SHELL_CMD_VER_HELP"));
    }

    @Override
    protected void doCommand(CStringPt line) {
        /* First split the line into command and arguments */
        line.trim();
        CStringPt cmdBuffer = CStringPt.create((int) ShellInner.CMD_MAXLINE);
        CStringPt cmdWrite = CStringPt.clone(cmdBuffer);
        while (line.get() != 0) {
            if (line.get() == 32)
                break;
            if (line.get() == '/')
                break;
            if (line.get() == '\t')
                break;
            if (line.get() == '=')
                break;
            // allow stuff like cd.. and dir.exe cd\kees
            if ((line.get() == '.') || (line.get() == '\\')) {
                cmdWrite.set((char) 0);
                if (!cmdBuffer.isEmpty() && cmdBuffer.length() > 0
                        && cmdList.containsKey(cmdBuffer.toString())) {
                    cmdList.get(cmdBuffer.toString()).Handler.run(line);
                    return;
                }
            }
            cmdWrite.set(line.get());
            cmdWrite.movePtToR1();
            line.movePtToR1();
        }
        cmdWrite.set((char) 0);
        if (cmdBuffer.length() == 0)
            return;
        /* Check the internal list */
        if (!cmdBuffer.isEmpty() && cmdBuffer.length() > 0
                && cmdList.containsKey(cmdBuffer.toString())) {
            cmdList.get(cmdBuffer.toString()).Handler.run(line);
            return;
        }
        /* This isn't an internal command execute it */
        if (execute(cmdBuffer, line))
            return;
        if (checkConfig(cmdBuffer.toString(), line.toString()))
            return;
        writeOut(Message.get("SHELL_EXECUTE_ILLEGAL_COMMAND"), cmdBuffer);
    }


    /* Checks if it matches a hardware-property */
    private boolean checkConfig(String cmdIn, String line) {
        Section test = DOSBox.Control.getSectionFromProperty(cmdIn);
        if (test == null)
            return false;
        if (line != null && line.charAt(0) == 0x00) {
            String val = test.getPropValue(cmdIn);
            if (val != SetupModule.NO_SUCH_PROPERTY)
                writeOut("%s\n", val);
            return true;
        }
        String newcom = "z:\\config " + test.getName() + " " + cmdIn + line;
        doCommand(CStringPt.create(newcom));
        return true;
    }

    // return값은 리턴 유무를 가리킴
    private boolean help(CStringPt args, String command) {
        if (Support.scanCmdBool(args, "?")) {
            writeOut(Message.get("SHELL_CMD_" + command + "_HELP"));
            String long_m = Message.get("SHELL_CMD_" + command + "_HELP_LONG");
            writeOut("\n");
            if ("Message not Found!\n" != long_m)
                writeOut(long_m);
            else
                writeOut(command + "\n");
            return true;
        }
        return false;
    }

    /* Some supported commands */
    // static char empty_char = 0;
    // static char* empty_string = &empty_char;
    private static char _emptyChar = (char) 0;
    private static CStringPt _emptyString = CStringPt.create(new char[] {_emptyChar});
    private static int _emptyStringIdx = 0;

    private void cmdHELP(CStringPt args) {
        if (help(args, "HELP"))
            return;
        boolean optall = Support.scanCmdBool(args, "ALL");
        /* Print the help */
        if (!optall)
            writeOut(Message.get("SHELL_CMD_HELP"));
        int cmd_index = 0, write_count = 0;
        for (ShellCmd cmd : cmdList.values()) {
            if (optall || cmd.Flags == 0) {
                writeOut("<\u001B[34;1m{0,-8}\u001B[0m> {1}", cmd.Name, Message.get(cmd.Help));
                if ((++write_count % 22) == 0)
                    cmdPAUSE(_emptyString);
            }
            cmd_index++;
        }
    }

    private void cmdCLS(CStringPt args) {
        help(args, "CLS");
        Register.setRegAX(0x0003);
        Callback.runRealInt(0x10);
    }

    static CStringPt defaulttarget = CStringPt.create(".");
    static byte[] buffer = new byte[0x8000]; // static, otherwise stack overflow possible.

    private void cmdCOPY(CStringPt args) {
        if (help(args, "COPY"))
            return;
        args.lTrim();
        /* Command uses dta so set it to our internal dta */
        int save_dta = DOSMain.DOS.getDTA();
        DOSMain.DOS.setDTA(DOSMain.DOS.tables.TempDTA);
        DOSDTA dta = new DOSDTA(DOSMain.DOS.getDTA());
        int size = 0;
        short date = 0;
        short time = 0;
        byte attr = 0;
        CStringPt name = CStringPt.create((int) DOSSystem.DOS_NAMELENGTH_ASCII);
        List<CopySource> sources = new ArrayList<CopySource>();
        // ignore /b and /t switches: always copy binary
        while (Support.scanCmdBool(args, "B"));
        while (Support.scanCmdBool(args, "T")); // Shouldn't this be A ?
        while (Support.scanCmdBool(args, "A"));
        Support.scanCmdBool(args, "Y");
        Support.scanCmdBool(args, "-Y");

        CStringPt rem = Support.scanCmdRemain(args);
        if (!rem.isEmpty()) {
            writeOut(Message.get("SHELL_ILLEGAL_SWITCH"), rem);
            DOSMain.DOS.setDTA(save_dta);
            return;
        }
        // Gather all sources (extension to copy more then 1 file specified at commandline)
        // Concatating files go as follows: All parts except for the last bear the concat flag.
        // This construction allows them to be counted (only the non concat set)
        CStringPt sourceP = CStringPt.getZero();
        CStringPt sourceX = CStringPt.create((int) (DOSSystem.DOS_PATHLENGTH + Cross.LEN));
        RefU32Ret refSize = new RefU32Ret(0);
        RefU16Ret refDate = new RefU16Ret(0);
        RefU16Ret refTime = new RefU16Ret(0);
        RefU8Ret refAttr = new RefU8Ret(0);

        while (!(sourceP = Support.stripWord(args)).isEmpty() && sourceP.get() != 0) {
            do {
                CStringPt plus = sourceP.positionOf('+');
                if (!plus.isEmpty())
                    plus.set((char) 0);
                plus.movePtToR1();
                CStringPt.safeCopy(sourceP, sourceX, (int) Cross.LEN);
                boolean hasDriveSpec = false;
                int sourceXLen = sourceX.length();
                if (sourceXLen > 0) {
                    if (sourceX.get(sourceXLen - 1) == ':')
                        hasDriveSpec = true;
                }
                if (!hasDriveSpec) {
                    if (DOSMain.findFirst(sourceP.toString(),
                            0xffff & ~DOSSystem.DOS_ATTR_VOLUME)) {
                        dta.getResult(name, refSize, refDate, refTime, refAttr);
                        attr = refAttr.U8;
                        if ((attr & DOSSystem.DOS_ATTR_DIRECTORY) != 0
                                && sourceP.positionOf("*.*").isEmpty())
                            sourceX.concat("\\*.*");
                    }
                }
                sources.add(new CopySource(sourceX.toString(), !plus.isEmpty()));
                sourceP = plus;
            } while (!sourceP.isEmpty() && sourceP.get() != 0);
        }
        // At least one source has to be there
        if (sources.size() == 0 || sources.get(0).filename.length() == 0) {
            writeOut(Message.get("SHELL_MISSING_PARAMETER"));
            DOSMain.DOS.setDTA(save_dta);
            return;
        }

        CopySource target = new CopySource();
        // If more then one object exists and last target is not part of a
        // concat sequence then make it the target.
        if (sources.size() > 1 && !sources.get(sources.size() - 2).concat) {
            target = sources.get(sources.size() - 1);// sources.Last();
            sources.remove(target);
        }
        // If no target => default target with concat flag true to detect a+b+c
        if (target.filename.length() == 0)
            target = new CopySource(defaulttarget.toString(), true);

        CopySource oldSource = new CopySource();
        CopySource source = new CopySource();
        int count = 0;
        while (sources.size() != 0) {
            /* Get next source item and keep track of old source for concat start end */
            oldSource = source;
            source = sources.get(0);
            sources.remove(sources.get(0));

            // Skip first file if doing a+b+c. Set target to first file
            if (!oldSource.concat && source.concat && target.concat) {
                target = source;
                continue;
            }

            /* Make a full path in the args */
            CStringPt pathSource = CStringPt.create((int) DOSSystem.DOS_PATHLENGTH);
            CStringPt pathTarget = CStringPt.create((int) DOSSystem.DOS_PATHLENGTH);

            if (!DOSMain.canonicalize(source.filename, pathSource)) {
                writeOut(Message.get("SHELL_ILLEGAL_PATH"));
                DOSMain.DOS.setDTA(save_dta);
                return;
            }
            // cut search pattern
            CStringPt pos = pathSource.lastPositionOf('\\');
            if (!pos.isEmpty())
                pos.set(1, (char) 0);

            if (!DOSMain.canonicalize(target.filename, pathTarget)) {
                writeOut(Message.get("SHELL_ILLEGAL_PATH"));
                DOSMain.DOS.setDTA(save_dta);
                return;
            }
            CStringPt temp = pathTarget.positionOf("*.*");
            if (!temp.isEmpty())
                temp.set((char) 0);// strip off *.* from target

            // add '\\' if target is a directoy
            if (pathTarget.get(pathTarget.length() - 1) != '\\') {
                if (DOSMain.findFirst(pathTarget.toString(), 0xffff & ~DOSSystem.DOS_ATTR_VOLUME)) {
                    dta.getResult(name, refSize, refDate, refTime, refAttr);
                    attr = refAttr.U8;
                    if ((attr & DOSSystem.DOS_ATTR_DIRECTORY) != 0)
                        pathTarget.concat("\\");
                }
            }

            // Find first sourcefile
            boolean ret = DOSMain.findFirst(source.filename, 0xffff & ~DOSSystem.DOS_ATTR_VOLUME);
            if (!ret) {
                writeOut(Message.get("SHELL_CMD_FILE_NOT_FOUND"), source.filename);
                DOSMain.DOS.setDTA(save_dta);
                return;
            }

            short sourceHandle = 0, targetHandle = 0;
            RefU16Ret RefSourceHandle = new RefU16Ret(sourceHandle);
            RefU16Ret RefTargetHandle = new RefU16Ret(targetHandle);
            CStringPt nameTarget = CStringPt.create((int) DOSSystem.DOS_PATHLENGTH);
            CStringPt nameSource = CStringPt.create((int) DOSSystem.DOS_PATHLENGTH);

            while (ret) {
                dta.getResult(name, refSize, refDate, refTime, refAttr);
                attr = refAttr.U8;
                if ((attr & DOSSystem.DOS_ATTR_DIRECTORY) == 0) {
                    CStringPt.copy(pathSource, nameSource);
                    nameSource.concat(name);
                    // Open Source
                    if (DOSMain.openFile(nameSource.toString(), 0, RefSourceHandle)) {
                        sourceHandle = RefSourceHandle.U16;
                        // Create Target or open it if in concat mode
                        CStringPt.copy(pathTarget, nameTarget);
                        if (nameTarget.get(nameTarget.length() - 1) == '\\')
                            nameTarget.concat(name);

                        // Don't create a newfile when in concat mode
                        if (oldSource.concat
                                || DOSMain.createFile(nameTarget.toString(), 0, RefTargetHandle)) {
                            targetHandle = RefTargetHandle.U16;
                            int dummy = 0;
                            RefU32Ret refPos = new RefU32Ret(dummy);
                            // In concat mode. Open the target and seek to the eof
                            if (!oldSource.concat || (DOSMain.openFile(nameTarget.toString(),
                                    (byte) DOSSystem.OPEN_READWRITE, RefTargetHandle)
                                    && (dummy = (int) DOSMain.seekFile(targetHandle, dummy,
                                            (byte) DOSSystem.DOS_SEEK_END)) >= 0)) {
                                targetHandle = RefTargetHandle.U16;

                                // Copy
                                // buffer = new byte[0x8000];// static, otherwise stack overflow
                                // possible.
                                boolean failed = false;
                                int toread = 0x8000;
                                BufRef refBuf = new BufRef(buffer, 0, toread);
                                do {
                                    failed |= DOSMain.readFile(sourceHandle, refBuf);
                                    failed |= DOSMain.writeFile(targetHandle, refBuf);
                                } while (refBuf.Len == 0x8000);
                                failed |= DOSMain.closeFile(sourceHandle);
                                failed |= DOSMain.closeFile(targetHandle);
                                writeOut(" %s\n", name);
                                if (!source.concat)
                                    count++; // Only count concat files once
                            } else {
                                DOSMain.closeFile(sourceHandle);
                                writeOut(Message.get("SHELL_CMD_COPY_FAILURE"), target.filename);
                            }
                        } else {
                            DOSMain.closeFile(sourceHandle);
                            writeOut(Message.get("SHELL_CMD_COPY_FAILURE"), target.filename);
                        }
                    } else
                        writeOut(Message.get("SHELL_CMD_COPY_FAILURE"), source.filename);
                }
                // On the next file
                ret = DOSMain.findNext();
            }
        }

        writeOut(Message.get("SHELL_CMD_COPY_SUCCESS"), count);
        DOSMain.DOS.setDTA(save_dta);
    }

    class CopySource {
        public String filename;
        public boolean concat;

        public CopySource(String filein, boolean concatin) {
            filename = filein;
            concat = concatin;
        }

        public CopySource() {
            filename = "";
            concat = false;
        }
    }


    private static CStringPt expandDot(CStringPt args, CStringPt buffer) {
        if (args.get() == '.') {
            if (CStringPt.clone(args, 1).get() == 0) {
                CStringPt.copy("*.*", buffer);
                return buffer;
            }
            if ((CStringPt.clone(args, 1).get() != '.')
                    && (CStringPt.clone(args, 1).get() != '\\')) {
                buffer.set(0, '*');
                buffer.set(1, (char) 0);
                buffer.concat(args);
                return buffer;
            } else
                CStringPt.copy(args, buffer);
        } else
            CStringPt.copy(args, buffer);
        return buffer;
    }


    private void formatNumber(int num, CStringPt buf) {
        CStringPt.copy(String.format(Locale.US, "%1$,d", num), buf);
    }


    private void cmdDIR(CStringPt args) {
        if (help(args, "DIR"))
            return;

        CStringPt numformat = CStringPt.create(16);
        CStringPt path = CStringPt.create((int) DOSSystem.DOS_PATHLENGTH);

        String line = null;
        if ((line = getEnvStr("DIRCMD")) != null) {
            int idx = line.indexOf('=');
            String value = line.substring(idx + 1);
            line = args.toString() + " " + value;
            args = CStringPt.create(line);
        }

        boolean optW = Support.scanCmdBool(args, "W");
        Support.scanCmdBool(args, "S");
        boolean optP = Support.scanCmdBool(args, "P");
        if (Support.scanCmdBool(args, "WP") || Support.scanCmdBool(args, "PW")) {
            optW = optP = true;
        }
        boolean optB = Support.scanCmdBool(args, "B");
        boolean optAD = Support.scanCmdBool(args, "AD");
        CStringPt rem = Support.scanCmdRemain(args);
        if (!rem.isEmpty()) {
            writeOut(Message.get("SHELL_ILLEGAL_SWITCH"), rem);
            return;
        }
        int byteCount, fileCount, dirCount;
        int wCount = 0;
        int pCount = 0;
        int wSize = optW ? 5 : 1;
        byteCount = fileCount = dirCount = 0;

        CStringPt buffer = CStringPt.create((int) Cross.LEN);
        args.trim();
        int argLen = args.length();
        if (argLen == 0) {
            CStringPt.copy("*.*", args); // no arguments.
        } else {
            switch (args.get(argLen - 1)) {
                case '\\': // handle \, C:\, etc.
                case ':': // handle C:, etc.
                    args.concat("*.*");
                    break;
                default:
                    break;
            }
        }
        args = expandDot(args, buffer);

        if (args.lastPositionOf('*').isEmpty() && args.lastPositionOf('?').isEmpty()) {
            RefU16Ret refAttr = new RefU16Ret(0);
            if (DOSMain.getFileAttr(args.toString(), refAttr)
                    && (refAttr.U16 & DOSSystem.DOS_ATTR_DIRECTORY) != 0) {
                args.concat("\\*.*"); // if no wildcard and a directory, get its files
            }
        }
        if (args.lastPositionOf('.').isEmpty()) {
            args.concat(".*"); // if no extension, get them all
        }

        /* Make a full path in the args */
        if (!DOSMain.canonicalize(args.toString(), path)) {
            writeOut(Message.get("SHELL_ILLEGAL_PATH"));
            return;
        }
        CStringPt tmp = path.lastPositionOf('\\');
        tmp.movePtToR1();
        tmp.set((char) 0);
        if (!optB)
            writeOut(Message.get("SHELL_CMD_DIR_INTRO"), path);

        /* Command uses dta so set it to our internal dta */
        int saveDTA = DOSMain.DOS.getDTA();
        DOSMain.DOS.setDTA(DOSMain.DOS.tables.TempDTA);
        DOSDTA dta = new DOSDTA(DOSMain.DOS.getDTA());
        boolean ret = DOSMain.findFirst(args.toString(), 0xffff & ~DOSSystem.DOS_ATTR_VOLUME);
        if (!ret) {
            if (!optB)
                writeOut(Message.get("SHELL_CMD_FILE_NOT_FOUND"), args);
            DOSMain.DOS.setDTA(saveDTA);
            return;
        }

        RefU32Ret refSize = new RefU32Ret(0);
        RefU16Ret refDate = new RefU16Ret(0);
        RefU16Ret refTime = new RefU16Ret(0);
        RefU8Ret refAttr = new RefU8Ret(0);

        do { /* File name and extension */
            CStringPt name = CStringPt.create((int) DOSSystem.DOS_NAMELENGTH_ASCII);
            int size = 0;
            short date = 0;
            short time = 0;
            byte attr = 0;
            dta.getResult(name, refSize, refDate, refTime, refAttr);
            size = refSize.U32;
            date = refDate.U16;
            time = refTime.U16;
            attr = refAttr.U8;
            /* Skip non-directories if option AD is present */
            if (optAD && (attr & DOSSystem.DOS_ATTR_DIRECTORY) == 0)
                continue;

            /* output the file */
            if (optB) {
                // this overrides pretty much everything
                if (name.equals(".") && name.equals("..")) {
                    writeOut("%s\n", name);
                }
            } else {
                CStringPt ext = _emptyString;
                if (!optW && (name.get(0) != '.')) {
                    ext = name.lastPositionOf('.');
                    if (ext.isEmpty())
                        ext = _emptyString;
                    else {
                        ext.set((char) 0);
                        ext.movePtToR1();
                    }
                }
                byte day = (byte) (date & 0x001f);
                byte month = (byte) ((date >>> 5) & 0x000f);
                short year = (short) ((date >>> 9) + 1980);
                byte hour = (byte) ((time >>> 5) >>> 6);
                byte minute = (byte) ((time >>> 5) & 0x003f);

                if ((attr & DOSSystem.DOS_ATTR_DIRECTORY) != 0) {
                    if (optW) {
                        writeOut("[%s]", name);
                        int namelen = name.length();
                        if (namelen <= 14) {
                            for (int i = 14 - namelen; i > 0; i--)
                                writeOut(" ");
                        }
                    } else {
                        writeOut("%-8s %-3s   %-16s %02d-%02d-%04d %2d:%02d\n", name.toString(),
                                ext.toString(), "<DIR>".toString(), day, month, year, hour, minute);
                    }
                    dirCount++;
                } else {
                    if (optW) {
                        writeOut("%-16s", name);
                    } else {
                        formatNumber(size, numformat);
                        writeOut("%-8s %-3s   %16s %02d-%02d-%04d %2d:%02d\n", name.toString(),
                                ext.toString(), numformat.toString(), day, month, year, hour,
                                minute);
                    }
                    fileCount++;
                    byteCount += size;
                }
                if (optW) {
                    wCount++;
                }
            }
            if (optP && (++pCount % (22 * wSize)) == 0) {
                cmdPAUSE(_emptyString);
            }
        } while ((ret = DOSMain.findNext()));
        if (optW) {
            if (wCount % 5 != 0)
                writeOut("\n");
        }
        if (!optB) {
            /* Show the summary of results */
            formatNumber(byteCount, numformat);
            writeOut(Message.get("SHELL_CMD_DIR_BYTES_USED"), fileCount,
                    StringHelper.padLeft(numformat.toString(), 17));
            byte drive = dta.getSearchDrive();
            // TODO Free Space
            int freeSpace = 1024 * 1024 * 100;
            if (DOSMain.Drives[drive] != null) {
                DriveAllocationInfo alloc = new DriveAllocationInfo(0, 0, 0, 0);
                DOSMain.Drives[drive].allocationInfo(alloc);
                freeSpace = (int) alloc.bytesSector * alloc.sectorsCluster * alloc.freeClusters;
            }
            formatNumber(freeSpace, numformat);
            writeOut(Message.get("SHELL_CMD_DIR_BYTES_FREE"), dirCount,
                    StringHelper.padLeft(numformat.toString(), 17));
        }
        DOSMain.DOS.setDTA(saveDTA);
    }

    private void cmdDELETE(CStringPt args) {
        if (help(args, "DELETE"))
            return;

        /* Command uses dta so set it to our internal dta */
        int save_dta = DOSMain.DOS.getDTA();
        DOSMain.DOS.setDTA(DOSMain.DOS.tables.TempDTA);

        CStringPt rem = Support.scanCmdRemain(args);
        if (!rem.isEmpty()) {
            writeOut(Message.get("SHELL_ILLEGAL_SWITCH"), rem);
            return;
        }
        /* If delete accept switches mind the space infront of them. See the dir /p code */

        CStringPt full = CStringPt.create((int) DOSSystem.DOS_PATHLENGTH);
        CStringPt buffer = CStringPt.create((int) Cross.LEN);
        args = expandDot(args, buffer);
        args.lTrim();
        if (!DOSMain.canonicalize(args.toString(), full)) {
            writeOut(Message.get("SHELL_ILLEGAL_PATH"));
            return;
        }
        // TODO Maybe support confirmation for *.* like dos does.
        boolean res = DOSMain.findFirst(args.toString(), 0xffff & ~DOSSystem.DOS_ATTR_VOLUME);
        if (!res) {
            writeOut(Message.get("SHELL_CMD_DEL_ERROR"), args);
            DOSMain.DOS.setDTA(save_dta);
            return;
        }
        // end can't be 0, but if it is we'll get a nice crash, who cares :)
        CStringPt end = CStringPt.clone(full.lastPositionOf('\\'), 1);
        end.set((char) 0);
        CStringPt name = CStringPt.create((int) DOSSystem.DOS_NAMELENGTH_ASCII);
        int size = 0;
        short time = 0, date = 0;
        byte attr = 0;
        RefU32Ret refSize = new RefU32Ret(size);
        RefU16Ret refDate = new RefU16Ret(time);
        RefU16Ret refTime = new RefU16Ret(date);
        RefU8Ret refAttr = new RefU8Ret(attr);

        DOSDTA dta = new DOSDTA(DOSMain.DOS.getDTA());
        while (res) {
            dta.getResult(name, refSize, refDate, refTime, refAttr);
            attr = refAttr.U8;
            if ((attr & (DOSSystem.DOS_ATTR_DIRECTORY | DOSSystem.DOS_ATTR_READ_ONLY)) == 0) {
                CStringPt.copy(name, end);
                if (!DOSMain.unlinkFile(full.toString()))
                    writeOut(Message.get("SHELL_CMD_DEL_ERROR"), full);
            }
            res = DOSMain.findNext();
        }
        DOSMain.DOS.setDTA(save_dta);
    }

    private void cmdECHO(CStringPt args) {
        if (args.get() == 0) {
            if (Echo) {
                writeOut(Message.get("SHELL_CMD_ECHO_ON"));
            } else {
                writeOut(Message.get("SHELL_CMD_ECHO_OFF"));
            }
            return;
        }
        CStringPt buffer = CStringPt.create(512);
        CStringPt pbuffer = buffer;
        CStringPt.safeCopy(args, buffer, 512);
        pbuffer.lTrim();
        if (pbuffer.equalsIgnoreCase("OFF")) {
            Echo = false;
            return;
        }
        if (pbuffer.equalsIgnoreCase("ON")) {
            Echo = true;
            return;
        }
        if (pbuffer.equalsIgnoreCase("/?")) {
            if (help(args, "ECHO"))
                return;
        }

        args.movePtToR1();// skip first character. either a slash or dot or space
        int len = args.length(); // TODO check input of else ook nodig is.
        if (len > 0 && args.get(len - 1) == '\r') {
            Log.logging(Log.LogTypes.MISC, Log.LogServerities.Warn,
                    "Hu ? carriage return allready present. Is this possible?");
            writeOut("{0}\n", args);
        } else
            writeOut("{0}\r\n", args);
    }

    private void cmdEXIT(CStringPt args) {
        if (help(args, "EXIT"))
            return;
        Exit = true;
    }

    private void cmdMKDIR(CStringPt args) {
        if (help(args, "MKDIR"))
            return;
        args.lTrim();
        CStringPt rem = Support.scanCmdRemain(args);
        if (!rem.isEmpty()) {
            writeOut(Message.get("SHELL_ILLEGAL_SWITCH"), rem);
            return;
        }
        if (!DOSMain.makeDir(args.toString())) {
            writeOut(Message.get("SHELL_CMD_MKDIR_ERROR"), args);
        }
    }

    private void cmdCHDIR(CStringPt args) {
        if (help(args, "CHDIR"))
            return;
        args.lTrim();
        if (args.get() == 0) {
            byte drive = (byte) (DOSMain.getDefaultDrive() + 'A');
            CStringPt dir = CStringPt.create((int) DOSSystem.DOS_PATHLENGTH);
            DOSMain.getCurrentDir(0, dir);
            writeOut("{0}:\\{1}\n", (char) drive, dir);
        } else if (args.length() == 2 && args.get(1) == ':') {
            writeOut(Message.get("SHELL_CMD_CHDIR_HINT"), Character.toUpperCase(args.get(0)));
        } else if (!DOSMain.changeDir(args.toString())) {
            /* Changedir failed. Check if the filename is longer then 8 and/or contains spaces */

            String temps = args.toString(), slashpart = "";
            if (temps.charAt(0) == '\\' || temps.charAt(0) == '/') {
                slashpart = temps.substring(0, 1);
                temps = temps.substring(1);
            }
            int separator = -1;
            if ((separator = temps.indexOf('\\')) >= 0 || (separator = temps.indexOf('/')) >= 0)
                temps = temps.substring(0, separator);
            separator = temps.lastIndexOf('.');
            if (separator >= 0)
                temps = temps.substring(0, separator);
            separator = temps.indexOf(' ');
            if (separator >= 0) {/* Contains spaces */
                temps = temps.substring(0, separator);
                if (temps.length() > 6)
                    temps = temps.substring(0, 6);
                temps += "~1";
                writeOut(Message.get("SHELL_CMD_CHDIR_HINT_2"), slashpart + temps);
            } else if (temps.length() > 8) {
                temps = temps.substring(0, 6);
                temps += "~1";
                writeOut(Message.get("SHELL_CMD_CHDIR_HINT_2"), slashpart + temps);
            } else {
                byte drive = (byte) (DOSMain.getDefaultDrive() + 'A');
                if (drive == 'Z') {
                    writeOut(Message.get("SHELL_CMD_CHDIR_HINT_3"));
                } else {
                    writeOut(Message.get("SHELL_CMD_CHDIR_ERROR"), args);
                }
            }
        }
    }

    private void cmdRMDIR(CStringPt args) {
        if (help(args, "RMDIR"))
            return;
        args.lTrim();
        CStringPt rem = Support.scanCmdRemain(args);
        if (!rem.isEmpty()) {
            writeOut(Message.get("SHELL_ILLEGAL_SWITCH"), rem);
            return;
        }
        if (!DOSMain.removeDir(args.toString())) {
            writeOut(Message.get("SHELL_CMD_RMDIR_ERROR"), args);
        }
    }

    private void cmdSET(CStringPt args) {
        if (help(args, "SET"))
            return;
        args.lTrim();
        String line = "";
        if (args.get() == 0) {
            /* No command line show all environment lines */
            int count = getEnvCount();
            for (int a = 0; a < count; a++) {
                if ((line = getEnvNum(a)) != null)
                    writeOut(line + "\n");
            }
            return;
        }
        CStringPt p = args.positionOf("=");
        if (p.isEmpty()) {
            if ((line = getEnvStr(args.toString())) == null)
                writeOut(Message.get("SHELL_CMD_SET_NOT_SET"), args);
            writeOut(line + "\n");
        } else {
            p.set((char) 0);
            p.movePtToR1();
            /* parse p for envirionment variables */
            CStringPt parsed = CStringPt.create((int) ShellInner.CMD_MAXLINE);
            CStringPt p_parsed = parsed;
            while (p.get() != 0) {
                if (p.get() != '%') {
                    p_parsed.set(p.get());
                    p_parsed.movePtToR1();
                    p.movePtToR1();
                } // Just add it (most likely path)
                else if (CStringPt.clone(p, 1).get() == '%') {
                    p_parsed.set('%');
                    p.moveR(2); // %% => %
                    p_parsed.movePtToR1();
                } else {
                    CStringPt second = (p.movePtToR1()).positionOf('%');
                    if (second.isEmpty())
                        continue;
                    second.set((char) 0);
                    second.movePtToR1();
                    String temp = null;
                    if ((temp = getEnvStr(p.toString())) != null) {
                        int equals = temp.indexOf('=');
                        if (equals < 0)
                            continue;
                        CStringPt.copy(temp.substring(equals + 1), p_parsed);
                        p_parsed.moveR(p_parsed.length());
                    }
                    p = second;
                }
            }
            p_parsed.set((char) 0);
            /* Try setting the variable */
            if (!setEnv(args.toString(), parsed.toString())) {
                writeOut(Message.get("SHELL_CMD_SET_OUT_OF_SPACE"));
            }
        }
    }

    private void cmdIF(CStringPt args) {
        if (help(args, "IF"))
            return;
        Support.stripSpaces(args, '=');
        boolean hasNot = false;

        while (args.startWithIgnoreCase("NOT")) {
            if (!Character.isWhitespace(args.get(3)) && (args.get(3) != '='))
                break;
            args.moveR(3); // skip text
            // skip more spaces
            Support.stripSpaces(args, '=');
            hasNot = !hasNot;
        }

        if (args.startWithIgnoreCase("ERRORLEVEL")) {
            args.moveR(10); // skip text
            // Strip spaces and ==
            Support.stripSpaces(args, '=');
            CStringPt word = Support.stripWord(args);
            if (!Character.isDigit(word.get())) {
                writeOut(Message.get("SHELL_CMD_IF_ERRORLEVEL_MISSING_NUMBER"));
                return;
            }

            byte n = 0;
            do
                n = (byte) (n * 10 + (word.get() - '0'));
            while (Character.isDigit((word.movePtToR1()).get()));
            if (word.get() != 0 && !Character.isWhitespace(word.get())) {
                writeOut(Message.get("SHELL_CMD_IF_ERRORLEVEL_INVALID_NUMBER"));
                return;
            }
            /* Read the error code from DOS */
            if ((DOSMain.DOS.ReturnCode >= n) == (!hasNot))
                doCommand(args);
            return;
        }

        if (args.startWithIgnoreCase("EXIST ")) {
            args.moveR(6); // Skip text
            args.lTrim();
            CStringPt word = Support.stripWord(args);
            if (word.get() == 0) {
                writeOut(Message.get("SHELL_CMD_IF_EXIST_MISSING_FILENAME"));
                return;
            }

            { /* DOS_FindFirst uses dta so set it to our internal dta */
                int saveDTA = DOSMain.DOS.getDTA();
                DOSMain.DOS.setDTA(DOSMain.DOS.tables.TempDTA);
                boolean ret =
                        DOSMain.findFirst(word.toString(), 0xffff & ~DOSSystem.DOS_ATTR_VOLUME);
                DOSMain.DOS.setDTA(saveDTA);
                if (ret == (!hasNot))
                    doCommand(args);
            }
            return;
        }

        /* Normal if string compare */

        CStringPt word1 = args;
        // first word is until space or =
        while (args.get() != 0 && !Character.isWhitespace(args.get()) && (args.get() != '='))
            args.movePtToR1();
        CStringPt endWord1 = args;

        // scan for =
        while (args.get() != 0 && (args.get() != '='))
            args.movePtToR1();
        // check for ==
        if ((args.get() == 0) || (args.get(1) != '=')) {
            syntaxError();
            return;
        }
        args.moveR(2);
        Support.stripSpaces(args, '=');

        CStringPt word2 = args;
        // second word is until space or =
        while (args.get() != 0 && !Character.isWhitespace(args.get()) && (args.get() != '='))
            args.movePtToR1();

        if (args.get() != 0) {
            endWord1.set((char) 0); // mark end of first word
            args.set((char) 0);
            args.movePtToR1(); // mark end of second word
            Support.stripSpaces(args, '=');

            if (word1.equals(word2) == (!hasNot))
                doCommand(args);
        }
    }

    private void cmdGOTO(CStringPt args) {
        if (help(args, "GOTO"))
            return;
        args.lTrim();
        if (BatFile == null)
            return;
        if (args.get() != 0 && (args.get() == ':'))
            args.movePtToR1();
        // label ends at the first space
        CStringPt nonSpace = args;
        while (nonSpace.get() != 0) {
            if ((nonSpace.get() == ' ') || (nonSpace.get() == '\t'))
                nonSpace.set((char) 0);
            else
                nonSpace.movePtToR1();
        }
        if (args.get() == 0) {
            writeOut(Message.get("SHELL_CMD_GOTO_MISSING_LABEL"));
            return;
        }
        if (!BatFile.gotoLabel(args)) {
            writeOut(Message.get("SHELL_CMD_GOTO_LABEL_NOT_FOUND"), args);
            return;
        }
    }

    private void cmdTYPE(CStringPt args) {
        if (help(args, "TYPE"))
            return;
        args.lTrim();
        if (args.get() == 0) {
            writeOut(Message.get("SHELL_SYNTAXERROR"));
            return;
        }
        short handle = 0;
        RefU16Ret refHandle = new RefU16Ret(handle);
        U8Ref refBuf = new U8Ref(0, 0);
        CStringPt word;
        // nextfile:
        while (true) {
            word = Support.stripWord(args);
            if (!DOSMain.openFile(word.toString(), 0, refHandle)) {
                writeOut(Message.get("SHELL_CMD_FILE_NOT_FOUND"), word);
                return;
            }
            handle = refHandle.U16;
            // short n; byte c = 0 ;
            refBuf.set(0, 0);
            do {
                // n = 1;
                refBuf.Len = 1;
                DOSMain.readFile(handle, refBuf);
                DOSMain.writeFile(DOSMain.STDOUT, refBuf);
            } while (refBuf.Len != 0);
            DOSMain.closeFile(handle);
            if (args.get() != 0)
                continue;// goto nextfile;

            break;
        }
    }

    private void cmdREM(CStringPt args) {
        if (help(args, "REM"))
            return;
    }

    private void cmdRENAME(CStringPt args) {
        if (help(args, "RENAME"))
            return;
        args.lTrim();
        if (args.get() == 0) {
            syntaxError();
            return;
        }
        if ((args.indexOf('*') >= 0) || (args.indexOf('?') >= 0)) {
            writeOut(Message.get("SHELL_CMD_NO_WILD"));
            return;
        }
        CStringPt arg1 = Support.stripWord(args);
        CStringPt slash = arg1.lastPositionOf('\\');
        if (!slash.isEmpty()) {
            slash.movePtToR1();
            /*
             * If directory specified (crystal caves installer) rename from c:\X : rename c:\abc.exe
             * abc.shr. File must appear in C:\
             */

            CStringPt dirSource = CStringPt.create((int) DOSSystem.DOS_PATHLENGTH);
            dirSource.set(0, (char) 0);
            // Copy first and then modify, makes GCC happy
            CStringPt.copy(arg1, dirSource);
            CStringPt dummy = dirSource.lastPositionOf('\\');
            dummy.set((char) 0);

            if ((dirSource.length() == 2) && (dirSource.get(1) == ':'))
                dirSource.concat("\\"); // X: add slash

            CStringPt dirCurrent = CStringPt.create((int) DOSSystem.DOS_PATHLENGTH + 1);
            dirCurrent.set(0, '\\'); // Absolute addressing so we can return properly
            CStringPt paramDirCurrent = CStringPt.clone(dirCurrent, 1);
            DOSMain.getCurrentDir(0, paramDirCurrent);
            if (!DOSMain.changeDir(dirSource.toString())) {
                writeOut(Message.get("SHELL_ILLEGAL_PATH"));
                return;
            }
            DOSMain.rename(slash.toString(), args.toString());
            DOSMain.changeDir(dirCurrent.toString());
        } else {
            DOSMain.rename(arg1.toString(), args.toString());
        }
    }

    private void cmdCALL(CStringPt args) {
        if (help(args, "CALL"))
            return;
        this.Call = true; /* else the old batchfile will be closed first */
        this.parseLine(args);
        this.Call = false;
    }


    private void cmdPAUSE(CStringPt args) {
        if (help(args, "PAUSE"))
            return;
        writeOut(Message.get("SHELL_CMD_PAUSE"));
        byte c = 0;
        short n = 1;
        U8Ref refChar = new U8Ref(c, n);
        DOSMain.readFile(DOSMain.STDIN, refChar);
    }

    private void cmdSUBST(CStringPt args) {
        /*
         * If more that one type can be substed think of something else E.g. make basedir member
         * dos_drive instead of localdrive
         */
        if (help(args, "SUBST"))
            return;
        LocalDrive ldp = null;
        CStringPt mountString = CStringPt.create((int) (DOSSystem.DOS_PATHLENGTH + Cross.LEN + 20));
        CStringPt tempStr = CStringPt.create(2);
        tempStr.set(0, (char) 0);
        tempStr.set(1, (char) 0);
        try {
            CStringPt.copy("MOUNT ", mountString);
            args.lTrim();
            String arg = null;
            CommandLine command = new CommandLine(0, args.toString());

            if (command.getCount() != 2)
                throw new DOSException("0");
            arg = command.findCommand(2);
            if ((arg == "/D") || (arg == "/d"))
                throw new DOSException("1"); // No removal (one day)

            arg = command.findCommand(1);
            if ((arg.length() > 1) && arg.charAt(1) != ':')
                throw new DOSException("0");
            tempStr.set(0, Character.toUpperCase(args.get(0)));
            if (DOSMain.Drives[tempStr.get(0) - 'A'] != null)
                throw new DOSException("0"); // targetdrive in use
            mountString.concat(tempStr);
            mountString.concat(" ");

            arg = command.findCommand(2);
            byte drive = 0;
            RefU8Ret refDrive = new RefU8Ret(drive);
            CStringPt fulldir = CStringPt.create((int) DOSSystem.DOS_PATHLENGTH);
            if (!DOSMain.makeName(arg.toUpperCase(), fulldir, refDrive))
                throw new DOSException("0");
            drive = refDrive.U8;
            DOSDrive drv = DOSMain.Drives[drive];
            if (!(DOSMain.Drives[drive] instanceof LocalDrive))
                throw new DOSException("0");
            ldp = (LocalDrive) drv;
            CStringPt newname = CStringPt.create((int) Cross.LEN);
            CStringPt.copy(ldp.basedir, newname);
            newname.concat(fulldir);
            ldp.dirCache.expandName(newname);
            mountString.concat("\"");
            mountString.concat(newname);
            mountString.concat("\"");
            this.parseLine(mountString);
        } catch (DOSException appEx) {
            if (appEx.getMessage() == "0") {
                writeOut(Message.get("SHELL_CMD_SUBST_FAILURE"));
            } else {
                writeOut(Message.get("SHELL_CMD_SUBST_NO_REMOVE"));
            }
            return;
        } catch (Exception e) {// dynamic cast failed =>so no localdrive
            writeOut(Message.get("SHELL_CMD_SUBST_FAILURE"));
            return;
        }

        return;
    }

    private void cmdLOADHIGH(CStringPt args) {
        if (help(args, "LOADHIGH"))
            return;
        int umbStart = DOSMain.DOSInfoBlock.getStartOfUMBChain();
        byte umbFlag = DOSMain.DOSInfoBlock.getUMBChainState();
        byte oldMemstrat = (byte) (DOSMain.getMemAllocStrategy() & 0xff);
        if (umbStart == 0x9fff) {
            if ((umbFlag & 1) == 0)
                DOSMain.linkUMBsToMemChain((short) 1);
            DOSMain.setMemAllocStrategy(0x80); // search in UMBs first
            this.parseLine(args);
            byte currentUmbFlag = DOSMain.DOSInfoBlock.getUMBChainState();
            if ((currentUmbFlag & 1) != (umbFlag & 1))
                DOSMain.linkUMBsToMemChain(umbFlag);
            DOSMain.setMemAllocStrategy(oldMemstrat); // restore strategy
        } else
            this.parseLine(args);
    }

    private static CStringPt _defChoice = CStringPt.create("yn");

    private void cmdCHOICE(CStringPt args) {
        if (help(args, "CHOICE"))
            return;
        CStringPt rem = CStringPt.getZero(), ptr;
        boolean optN = Support.scanCmdBool(args, "N");
        boolean optS = Support.scanCmdBool(args, "S"); // Case-sensitive matching
        Support.scanCmdBool(args, "T"); // Default Choice after timeout
        if (!args.isEmpty()) {
            CStringPt last = CStringPt.clone(args, args.length());
            args.lTrim();
            rem = Support.scanCmdRemain(args);
            if (!rem.isEmpty() && rem.get() != 0 && (Character.toLowerCase(rem.get(1)) != 'c')) {
                writeOut(Message.get("SHELL_ILLEGAL_SWITCH"), rem);
                return;
            }
            if (args == rem)
                args = CStringPt.clone(rem, rem.length() + 1);
            if (!rem.isEmpty())
                rem.moveR(2);
            if (!rem.isEmpty() && rem.get(0) == ':')
                rem.movePtToR1(); /* optional : after /c */

            // if (args > last) args.Empty();
            if (CStringPt.great(args, last))
                args.empty();
        }
        if (rem.isEmpty() || rem.get() == 0)
            rem = _defChoice; /* No choices specified use YN */
        ptr = rem;
        byte c = 0;
        if (!optS)
            while ((c = (byte) ptr.get()) != (char) 0) {
                ptr.set(Character.toUpperCase((char) c));
                ptr.movePtToR1();
            } /* When in no case-sensitive mode. make everything upcase */
        if (!args.isEmpty() && args.get() != 0) {
            args.lTrim();
            int argslen = args.length();
            if (argslen > 1 && args.get(0) == '"' && args.get(argslen - 1) == '"') {
                args.set(argslen - 1, (char) 0); // Remove quotes
                args.movePtToR1();
            }
            writeOut(args.toString());
        }
        /* Show question prompt of the form [a,b]? where a b are the choice values */
        if (!optN) {
            if (!args.isEmpty() && args.get() != 0)
                writeOut(" ");
            writeOut("[");
            int len = rem.length();
            for (int t = 1; t < len; t++) {
                writeOut("{0},", (char) rem.get(t - 1));
            }
            writeOut("{0}]?", (char) rem.get(len - 1));
        }

        int n = 1;
        U8Ref refChar = new U8Ref(c, n);
        do {
            DOSMain.readFile(DOSMain.STDIN, refChar);
            c = refChar.U8;
            n = refChar.Len;
        } while (c == 0 || (ptr = rem.positionOf(optS ? (char) c : Character.toUpperCase((char) c)))
                .isEmpty());
        c = optS ? c : (byte) Character.toUpperCase((char) c);
        DOSMain.writeFile(DOSMain.STDOUT, c);
        DOSMain.DOS.ReturnCode = (byte) (CStringPt.diff(ptr, rem) + 1);
    }

    private void cmdATTRIB(CStringPt args) {
        if (help(args, "ATTRIB"))
            return;
        // No-Op for now.
    }

    private void cmdPATH(CStringPt args) {
        if (help(args, "PATH"))
            return;
        if (!args.isEmpty() && args.get() != 0 && args.length() > 0) {
            CStringPt pathstring =
                    CStringPt.create((int) (DOSSystem.DOS_PATHLENGTH + Cross.LEN + 20));
            pathstring.set(0, (char) 0);
            CStringPt.copy("set PATH=", pathstring);
            while (!args.isEmpty() && args.get() != 0 && (args.get() == '=' || args.get() == ' '))
                args.movePtToR1();
            pathstring.concat(args);
            this.parseLine(pathstring);
            return;
        } else {
            String line = "";
            if ((line = getEnvStr("PATH")) != null) {
                writeOut("{0}", line);
            } else {
                writeOut("PATH=(null)");
            }
        }
    }

    private void cmdSHIFT(CStringPt args) {
        if (help(args, "SHIFT"))
            return;
        if (BatFile != null)
            BatFile.shift();
    }

    private void cmdVER(CStringPt args) {
        if (help(args, "VER"))
            return;
        if (!args.isEmpty() && args.get() != 0) {
            CStringPt word = Support.stripWord(args);
            if (!word.equalsIgnoreCase("set"))
                return;
            word = Support.stripWord(args);
            DOSMain.DOS.Version.major = (byte) (Integer.parseInt(word.toString()));
            DOSMain.DOS.Version.minor = (byte) (Integer.parseInt(args.toString()));
        } else
            writeOut(Message.get("SHELL_CMD_VER_VER"), DOSBox.VERSION, DOSMain.DOS.Version.major,
                    DOSMain.DOS.Version.minor);
    }
    /*--------------------------- end DOSShellCmds -----------------------------*/


    /*--------------------------- begin DOSShellMisc -----------------------------*/
    public static String FullArguments = "";

    private static void outc(byte c) {
        DOSMain.writeFile(DOSMain.STDOUT, c);
    }

    private static void outc(int c) {
        outc((byte) c);
    }

    @Override
    protected void inputCommand(CStringPt line) {
        int size = ShellInner.CMD_MAXLINE - 2; // lastcharacter+0
        byte c = 0;
        int n = 1;
        U8Ref refChar = new U8Ref(c, n);
        int strLen = 0;
        int strIndex = 0;
        int len = 0;
        boolean currentHist = false; // current command stored in history?
        RefU16Ret refDummy = new RefU16Ret(0);

        line.set(0, '\0');
        String currHisStr = null;
        if (!_history.isEmpty())
            currHisStr = _history.getFirst();
        int currHisIdx = 0;
        String currCmpStr = null;
        if (!_completion.isEmpty())
            currCmpStr = _completion.getFirst();
        int currCmpIdx = 0;
        while (size > 0) {
            DOSMain.DOS.Echo = false;
            while (!DOSMain.readFile(InputHandle, refChar)) {
                short dummy = 0;
                refDummy.U16 = dummy;
                DOSMain.closeFile(InputHandle);
                DOSMain.openFile("con", 2, refDummy);
                Log.logging(Log.LogTypes.MISC, Log.LogServerities.Error,
                        "Reopening the input handle.This is a bug!");
            }
            c = refChar.U8;
            n = refChar.Len;
            if (n == 0) {
                size = 0; // Kill the while loop
                continue;
            }
            switch (c) {
                case 0x00: /* Extended Keys */
                {
                    DOSMain.readFile(InputHandle, refChar);
                    c = refChar.U8;
                    n = refChar.Len;
                    switch (c) {

                        case 0x3d: /* F3 */
                            if (_history.size() == 0)
                                break;
                            currHisStr = _history.getFirst();
                            currHisIdx = 0;
                            if (currHisStr != null && currHisStr.length() > strLen) {
                                int readerIdx = (int) strLen;
                                while (readerIdx < currHisStr.length()
                                        && (c = (byte) currHisStr.charAt(readerIdx++)) != 0) {
                                    line.set(strIndex++, (char) c);
                                    DOSMain.writeFile(DOSMain.STDOUT, c);
                                }
                                strLen = strIndex = currHisStr.length();
                                size = (int) ShellInner.CMD_MAXLINE - strIndex - 2;
                                line.set(strLen, (char) 0);
                            }
                            break;

                        case 0x4B: /* LEFT */
                            if (strIndex != 0) {
                                outc(8);
                                strIndex--;
                            }
                            break;

                        case 0x4D: /* RIGHT */
                            if (strIndex < strLen) {
                                outc((byte) line.get(strIndex++));
                            }
                            break;

                        case 0x47: /* HOME */
                            while (strIndex != 0) {
                                outc(8);
                                strIndex--;
                            }
                            break;

                        case 0x4F: /* END */
                            while (strIndex < strLen) {
                                outc((byte) line.get(strIndex++));
                            }
                            break;

                        case 0x48: /* UP */
                        {
                            if (currHisStr == null || _history.size() == 0)
                                break;

                            // store current command in history if we are at beginning
                            if (currHisStr == _history.getFirst() && !currentHist) {
                                currentHist = true;
                                _history.addFirst(line.toString());
                            }

                            for (; strIndex > 0; strIndex--) {
                                // removes all characters
                                outc(8);
                                outc((byte) ' ');
                                outc(8);
                            }
                            CStringPt.copy(currHisStr, line);
                            len = currHisStr.length();
                            strLen = strIndex = len;
                            size = (int) ShellInner.CMD_MAXLINE - strIndex - 2;
                            RefU16Ret refLen = new RefU16Ret(len);
                            DOSMain.writeFile(DOSMain.STDOUT, line, refLen);
                            len = refLen.U16;
                            currHisStr = _history.get(++currHisIdx);
                            break;
                        }
                        case 0x50: /* DOWN */
                        {
                            if (_history.size() == 0 || currHisStr == _history.getFirst())
                                break;

                            // not very nice but works ..
                            currHisStr = _history.get(--currHisIdx);

                            if (currHisStr == _history.getFirst()) {
                                // no previous commands in history
                                currHisStr = _history.get(++currHisIdx);

                                // remove current command from history
                                if (currentHist) {
                                    currentHist = false;
                                    _history.removeFirst();
                                }
                                break;
                            } else {
                                currHisStr = _history.get(--currHisIdx);
                            }

                            for (; strIndex > 0; strIndex--) {
                                // removes all characters
                                outc(8);
                                outc((byte) ' ');
                                outc(8);
                            }
                            CStringPt.copy(currHisStr, line);
                            len = currHisStr.length();
                            strLen = strIndex = len;
                            size = (int) ShellInner.CMD_MAXLINE - strIndex - 2;
                            RefU16Ret refLen = new RefU16Ret(len);
                            DOSMain.writeFile(DOSMain.STDOUT, line, refLen);
                            len = refLen.U16;
                            currHisStr = _history.get(++currHisIdx);

                            break;
                        }
                        case 0x53:/* DELETE */
                        {
                            if (strIndex >= strLen)
                                break;
                            short a = (short) (strLen - strIndex - 1);
                            byte[] text = CStringPt.clone(line, strIndex + 1).getAsciiBytes();
                            BufRef buf = new BufRef();
                            buf.Buf = text;
                            buf.StartIndex = 0;
                            buf.Len = text.length;
                            DOSMain.writeFile(DOSMain.STDOUT, buf);// write buffer to screen
                            outc((byte) ' ');
                            outc(8);
                            for (int i = strIndex; i < strLen - 1; i++) {
                                line.set(i, line.get(i + 1));
                                outc(8);
                            }
                            line.set(--strLen, (char) 0);
                            size++;
                        }
                            break;
                        default:
                            break;
                    }
                }
                    break;
                case 0x08: /* BackSpace */
                    if (strIndex != 0) {
                        outc(8);
                        int strRemain = strLen - strIndex;
                        size++;
                        if (strRemain != 0) {
                            // memmove(&line[str_index - 1], &line[str_index], str_remain);
                            CStringPt.rawMove(CStringPt.clone(line, strIndex),
                                    CStringPt.clone(line, strIndex - 1), strRemain);
                            line.set(--strLen, (char) 0);
                            strIndex--;
                            /* Go back to redraw */
                            for (short i = (short) strIndex; i < strLen; i++)
                                outc((byte) line.get(i));
                        } else {
                            line.set(--strIndex, '\0');
                            strLen--;
                        }
                        outc((byte) ' ');
                        outc(8);
                        // moves the cursor left
                        while (strRemain-- != 0)
                            outc(8);
                    }
                    if (_completion.size() != 0)
                        _completion.clear();
                    break;
                case 0x0a: /* New Line not handled */
                    /* Don't care */
                    break;
                case 0x0d: /* Return */
                    outc((byte) '\n');
                    size = 0; // Kill the while loop
                    break;
                case (byte) '\t': {
                    if (_completion.size() > 0) {
                        currCmpStr = _completion.get(++currCmpIdx);
                        if (currCmpStr == null) {
                            currCmpIdx = 0;
                            currCmpStr = _completion.getFirst();
                        }
                    } else {
                        // build new completion list
                        // Lines starting with CD will only get directories in the list
                        boolean dir_only =
                                line.toString().substring(0, 3).compareToIgnoreCase("CD") == 0;
                        // get completion mask
                        CStringPt pCompletionStart = line.lastPositionOf(' ');

                        if (!pCompletionStart.isEmpty()) {
                            pCompletionStart.movePtToR1();
                            _completionIndex = strLen - pCompletionStart.length();
                        } else {
                            pCompletionStart = line;
                            _completionIndex = 0;
                        }

                        CStringPt path;
                        if (!(path = CStringPt.clone(line, _completionIndex).lastPositionOf('\\'))
                                .isEmpty())
                            _completionIndex = CStringPt.diff(path, line) + 1;
                        if (!(path = CStringPt.clone(line, _completionIndex).lastPositionOf('/'))
                                .isEmpty())
                            _completionIndex = CStringPt.diff(path, line) + 1;

                        CStringPt mask = CStringPt.create((int) DOSSystem.DOS_PATHLENGTH);
                        if (!pCompletionStart.isEmpty()) {
                            CStringPt.copy(pCompletionStart, mask);
                            CStringPt dotPos = mask.lastPositionOf('.');
                            CStringPt bsPos = mask.lastPositionOf('\\');
                            CStringPt fsPos = mask.lastPositionOf('/');
                            CStringPt clPos = mask.lastPositionOf(':');
                            // not perfect when line already contains wildcards, but works
                            if ((CStringPt.diff(dotPos, bsPos) > 0)
                                    && (CStringPt.diff(dotPos, fsPos) > 0)
                                    && (CStringPt.diff(dotPos, clPos) > 0))
                                mask.concat("*");
                            else
                                mask.concat("*.*");
                        } else {
                            CStringPt.copy("*.*", mask);
                        }

                        int saveDTA = DOSMain.DOS.getDTA();
                        DOSMain.DOS.setDTA(DOSMain.DOS.tables.TempDTA);

                        boolean res = DOSMain.findFirst(mask.toString(),
                                0xffff & ~DOSSystem.DOS_ATTR_VOLUME);
                        if (!res) {
                            DOSMain.DOS.setDTA(saveDTA);
                            break; // TODO: beep
                        }

                        DOSDTA dta = new DOSDTA(DOSMain.DOS.getDTA());
                        CStringPt name = CStringPt.getZero();
                        int sz = 0;
                        short date = 0;
                        short time = 0;
                        byte att = 0;
                        RefU32Ret refSize = new RefU32Ret(sz);
                        RefU16Ret refDate = new RefU16Ret(date);
                        RefU16Ret refTime = new RefU16Ret(time);
                        RefU8Ret refAttr = new RefU8Ret(att);

                        LinkedList<String> executable = new LinkedList<String>();
                        while (res) {
                            dta.getResult(name, refSize, refDate, refTime, refAttr);
                            att = refAttr.U8;
                            // add result to completion list

                            CStringPt ext; // file extension
                            if (name.equals(".") && name.equals("..")) {
                                if (dir_only) { // Handle the dir only case different (line starts
                                                // with cd)
                                    if ((att & DOSSystem.DOS_ATTR_DIRECTORY) > 0)
                                        _completion.addLast(name.toString());
                                } else {
                                    ext = name.lastPositionOf('.');
                                    if (!ext.isEmpty() && (ext.equals(".BAT") || ext.equals(".COM")
                                            || ext.equals(".EXE")))
                                        // we add executables to the a seperate list and place that
                                        // list infront of the normal files
                                        executable.addFirst(name.toString());
                                    else
                                        _completion.addLast(name.toString());
                                }
                            }
                            res = DOSMain.findNext();
                        }

                        /* Add excutable list to front of completion list. */
                        for (String nd : executable) {
                            _completion.add(0, nd);
                        }

                        currCmpIdx = 0;
                        currCmpStr = _completion.getFirst();
                        DOSMain.DOS.setDTA(saveDTA);
                    }

                    if (_completion.size() > 0 && currCmpStr.length() > 0) {
                        for (; strIndex > _completionIndex; strIndex--) {
                            // removes all characters
                            outc(8);
                            outc((byte) ' ');
                            outc(8);
                        }

                        CStringPt.copy(currCmpStr, CStringPt.clone(line, _completionIndex));
                        len = currCmpStr.length();
                        strLen = strIndex = _completionIndex + len;
                        size = ShellInner.CMD_MAXLINE - strIndex - 2;
                        RefU16Ret refLen = new RefU16Ret(len);
                        DOSMain.writeFile(DOSMain.STDOUT, CStringPt.create(currCmpStr), refLen);
                        len = refLen.U16;
                    }
                }
                    break;
                case 0x1b: /* ESC */
                    // write a backslash and return to the next line
                    outc((byte) '\\');
                    outc((byte) '\n');
                    line.set(0, (char) 0); // reset the line.
                    if (_completion.size() > 0)
                        _completion.clear(); // reset the completion list.
                    this.inputCommand(line); // Get the NEW line.
                    size = 0; // stop the next loop
                    strLen = 0; // prevent multiple adds of the same line
                    break;
                default:
                    if (_completion.size() > 0)
                        _completion.clear();
                    // mem_readb(BIOS_KEYBOARD_FLAGS1)&0x80) dev_con.h ?
                    if (strIndex < strLen && true) {
                        outc((byte) ' ');// move cursor one to the right.
                        short a = (short) (strLen - strIndex);
                        RefU16Ret refLen = new RefU16Ret(a);
                        DOSMain.writeFile(DOSMain.STDOUT, line, refLen);// write buffer to screen
                        a = refLen.U16;
                        outc(8);// undo the cursor the right.
                        for (int i = strLen; i > strIndex; i--) {
                            line.set(i, line.get(i - 1)); // move internal buffer
                            outc(8); // move cursor back (from write buffer to screen)
                        }
                        // new end (as the internal buffer moved one place to the right
                        line.set(++strLen, (char) 0);
                        size--;
                    }

                    line.set(strIndex, (char) c);
                    strIndex++;
                    if (strIndex > strLen) {
                        line.set(strIndex, '\0');
                        strLen++;
                        size--;
                    }
                    DOSMain.writeFile(DOSMain.STDOUT, c);
                    break;
            }
        }

        if (strLen == 0)
            return;
        strLen++;

        // remove current command from history if it's there
        if (currentHist) {
            currentHist = false;
            _history.removeFirst();
        }

        // add command line to history
        _history.add(0, line.toString());
        currHisIdx = 0;
        currHisStr = _history.getFirst();
        if (_completion.size() > 0)
            _completion.clear();
    }


    public boolean execute(CStringPt name, CStringPt args) {
        /*
         * return true => don't check for hardware changes in do_command return false => check for
         * hardware changes in do_command
         */
        // stores results from Which
        CStringPt fullname = CStringPt.create((int) DOSSystem.DOS_PATHLENGTH + 4);
        CStringPt pFullname;
        CStringPt line = CStringPt.create((int) ShellInner.CMD_MAXLINE);

        if (args.length() != 0) {
            if (args.get() != ' ') { // put a space in front
                line.set(0, ' ');
                line.set(1, (char) 0);
                line.concat(args, (int) ShellInner.CMD_MAXLINE - 2);
                line.set((int) ShellInner.CMD_MAXLINE - 1, (char) 0);
            } else {
                CStringPt.safeCopy(args, line, (int) ShellInner.CMD_MAXLINE);
            }
        } else {
            line.set(0, (char) 0);
        }

        /* check for a drive change */
        if ((CStringPt.clone(name, 1).equals(":") || CStringPt.clone(name, 1).equals(":\\"))
                && Character.isLetter(name.get())) {
            if (!DOSMain.setDrive((byte) (Character.toUpperCase(name.get(0)) - 'A'))) {
                writeOut(Message.get("SHELL_EXECUTE_DRIVE_NOT_FOUND"),
                        Character.toUpperCase(name.get(0)));
            }
            return true;
        }
        /* Check for a full name */
        pFullname = which(name);
        if (pFullname.isEmpty())
            return false;
        CStringPt.copy(pFullname, fullname);
        CStringPt extension = fullname.lastPositionOf('.');

        /* always disallow files without extension from being executed. */
        /* only internal commands can be run this way and they never get in this handler */
        if (extension.isEmpty()) {
            // Check if the result will fit in the parameters. Else abort
            if (fullname.length() > (DOSSystem.DOS_PATHLENGTH - 1))
                return false;
            CStringPt tempName = CStringPt.create((int) DOSSystem.DOS_PATHLENGTH + 4);
            CStringPt tempFullname;
            // try to add .com, .exe and .bat extensions to filename

            CStringPt.copy(fullname, tempName);
            tempName.concat(".COM");
            tempFullname = which(tempName);
            if (!tempFullname.isEmpty()) {
                extension = CStringPt.create(".com");
                CStringPt.copy(tempFullname, fullname);
            }

            else {
                CStringPt.copy(fullname, tempName);
                tempName.concat(".EXE");
                tempFullname = which(tempName);
                if (!tempFullname.isEmpty()) {
                    extension = CStringPt.create(".exe");
                    CStringPt.copy(tempFullname, fullname);
                }

                else {
                    CStringPt.copy(fullname, tempName);
                    tempName.concat(".BAT");
                    tempFullname = which(tempName);
                    if (!tempFullname.isEmpty()) {
                        extension = CStringPt.create(".bat");
                        CStringPt.copy(tempFullname, fullname);
                    }

                    else {
                        return false;
                    }

                }
            }
        }

        if (extension.toString().equalsIgnoreCase(".bat")) { /* Run the .bat file */
            /* delete old batch file if call is not active */
            boolean tempEcho =
                    Echo; /* keep the current echostate (as delete bf might change it ) */
            if (BatFile != null && !Call)
                BatFile.dispose();
            BatFile = new BatchFile(this, fullname, name, line);
            Echo = tempEcho; // restore it.
        } else { /* only .bat .exe .com extensions maybe be executed by the shell */
            if (!extension.toString().equalsIgnoreCase(".com")) {
                if (!extension.toString().equalsIgnoreCase(".exe"))
                    return false;
            }
            /* Run the .exe or .com file from the shell */
            /* Allocate some stack space for tables in physical memory */
            Register.setRegSP(Register.getRegSP() - 0x200);
            // Add Parameter block
            DOSParamBlock block =
                    new DOSParamBlock(Register.segPhys(Register.SEG_NAME_SS) + Register.getRegSP());
            block.clear();
            // Add a filename
            int fileName = Register.realMakeSeg(Register.SEG_NAME_SS, Register.getRegSP() + 0x20);
            Memory.blockWrite(Memory.real2Phys(fileName), fullname);// 마지막 null포함

            /* HACK: Store full commandline for mount and imgmount */
            FullArguments = line.toString();

            /* Fill the command line */
            byte[] cmdtail = new byte[DOSMain.CommandTailSize];// 첫번째 값은 문자열이 아닌 Count
            // CStringPt csCmdTail = CStringPt.Create(cmdtail);
            cmdtail[DOSMain.CommandTailOffCount] = 0;
            // Else some part of the string is unitialized (valgrind)
            Arrays.fill(cmdtail, DOSMain.CommandTailOffBuffer, 126, (byte) 0);
            if (line.length() > 126)
                line.set(126, (char) 0);
            cmdtail[DOSMain.CommandTailOffCount] = (byte) line.length();
            ArrayHelper.copy(line.getAsciiBytes(), 0, cmdtail, DOSMain.CommandTailOffBuffer,
                    line.length());// 마지막 null 제외
            cmdtail[DOSMain.CommandTailOffBuffer + line.length()] = 0xd;

            /* Copy command line in stack block too */
            Memory.blockWrite(Register.segPhys(Register.SEG_NAME_SS) + Register.getRegSP() + 0x100,
                    cmdtail, 0, 128);
            /* Parse FCB (first two parameters) and put them into the current DOS_PSP */
            byte add = 0;
            FCBParseNameParamRef addRef = new FCBParseNameParamRef((byte) 0);
            CStringPt cmdtailBuffer = CStringPt.create(DOSMain.CommandTail_Size_Buffer);
            for (int i = 0; i < DOSMain.CommandTail_Size_Buffer; i++) {
                cmdtailBuffer.set(i, (char) cmdtail[i + DOSMain.CommandTailOffBuffer]);
            }
            DOSMain.FCBParseName(DOSMain.DOS.getPSP(), 0x5C, (byte) 0x00, cmdtailBuffer, addRef);
            DOSMain.FCBParseName(DOSMain.DOS.getPSP(), 0x6C, (byte) 0x00,
                    CStringPt.clone(cmdtailBuffer, addRef.Change), addRef);
            block.Exec.FCB1 = Memory.realMake(DOSMain.DOS.getPSP(), 0x5C);
            block.Exec.FCB2 = Memory.realMake(DOSMain.DOS.getPSP(), 0x6C);
            /* Set the command line in the block and save it */
            block.Exec.CmdTail =
                    Register.realMakeSeg(Register.SEG_NAME_SS, Register.getRegSP() + 0x100);
            block.saveData();

            /* Start up a dos execute interrupt */
            Register.setRegAX(0x4b00);
            // Filename pointer
            Register.segSet16(Register.SEG_NAME_DS, Register.segValue(Register.SEG_NAME_SS));
            Register.setRegDX(Memory.realOff(fileName));
            // Paramblock
            Register.segSet16(Register.SEG_NAME_ES, Register.segValue(Register.SEG_NAME_SS));
            Register.setRegBX(Register.getRegSP());
            Register.setFlagBit(Register.FlagIF, false);
            Callback.runRealInt(0x21);
            /* Restore CS:IP and the stack */
            Register.setRegSP(Register.getRegSP() + 0x200);


        }
        return true; // Executable started
    }

    static String batExt = ".BAT";
    static String comExt = ".COM";
    static String exeExt = ".EXE";
    static CStringPt whichRet = CStringPt.create((int) DOSSystem.DOS_PATHLENGTH + 4);

    protected CStringPt which(CStringPt name) {
        int nameLen = name.length();
        if (nameLen >= DOSSystem.DOS_PATHLENGTH)
            return CStringPt.getZero();

        /* Parse through the Path to find the correct entry */
        /* Check if name is already ok but just misses an extension */

        if (DOSMain.fileExists(name.toString()))
            return name;
        /* try to find .com .exe .bat */
        CStringPt.copy(name, whichRet);
        whichRet.concat(comExt);
        if (DOSMain.fileExists(whichRet.toString()))
            return whichRet;
        CStringPt.copy(name, whichRet);
        whichRet.concat(exeExt);
        if (DOSMain.fileExists(whichRet.toString()))
            return whichRet;
        CStringPt.copy(name, whichRet);
        whichRet.concat(batExt);
        if (DOSMain.fileExists(whichRet.toString()))
            return whichRet;


        /* No Path in filename look through path environment string */
        CStringPt path = CStringPt.create((int) DOSSystem.DOS_PATHLENGTH);
        String temp = null;
        if ((temp = getEnvStr("PATH")) == null)
            return CStringPt.getZero();
        CStringPt pathenv = CStringPt.create(temp);
        if (pathenv.isEmpty())
            return CStringPt.getZero();
        pathenv = pathenv.positionOf('=');
        if (pathenv.isEmpty())
            return CStringPt.getZero();
        pathenv.movePtToR1();
        int i_path = 0;


        while (pathenv.get() != 0) {
            /* remove ; and ;; at the beginning. (and from the second entry etc) */
            while (pathenv.get() != 0 && (pathenv.get() == ';'))
                pathenv.movePtToR1();

            /* get next entry */
            i_path = 0; /* reset writer */
            while (pathenv.get() != 0 && (pathenv.get() != ';')
                    && (i_path < DOSSystem.DOS_PATHLENGTH))
                path.set(i_path++, (pathenv.movePtToR1()).get());

            if (i_path == DOSSystem.DOS_PATHLENGTH) {
                /* If max size. move till next ; and terminate path */
                while (pathenv.get() != ';')
                    pathenv.movePtToR1();
                path.set((int) DOSSystem.DOS_PATHLENGTH - 1, (char) 0);
            } else
                path.set(i_path, (char) 0);


            /* check entry */
            int len = path.length();
            if (len != 00) {
                if (len >= (DOSSystem.DOS_PATHLENGTH - 2))
                    continue;

                if (path.get(len - 1) != '\\') {
                    path.concat("\\");
                    len++;
                }

                // If name too long =>next
                if ((nameLen + len + 1) >= DOSSystem.DOS_PATHLENGTH)
                    continue;
                path.concat(name);

                CStringPt.copy(path, whichRet);
                if (DOSMain.fileExists(whichRet.toString()))
                    return whichRet;
                CStringPt.copy(path, whichRet);
                whichRet.concat(comExt);
                if (DOSMain.fileExists(whichRet.toString()))
                    return whichRet;
                CStringPt.copy(path, whichRet);
                whichRet.concat(exeExt);
                if (DOSMain.fileExists(whichRet.toString()))
                    return whichRet;
                CStringPt.copy(path, whichRet);
                whichRet.concat(batExt);
                if (DOSMain.fileExists(whichRet.toString()))
                    return whichRet;
            }
        }
        return CStringPt.getZero();
    }

    /*--------------------------- end DOSShellMisc -----------------------------*/

}