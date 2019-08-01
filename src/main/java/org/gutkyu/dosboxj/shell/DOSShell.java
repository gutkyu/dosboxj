package org.gutkyu.dosboxj.shell;

import org.gutkyu.dosboxj.dos.*;
import java.nio.charset.StandardCharsets;
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
    protected void doCommand(String line) {
        /* First split the line into command and arguments */
        line = line.trim();
        CStringPt cmdBuffer = CStringPt.create(ShellInner.CMD_MAXLINE);
        CStringPt cmdWrite = CStringPt.clone(cmdBuffer);
        int lineIdx = 0;
        char c;
        while (lineIdx < line.length()) {
            c = line.charAt(lineIdx);
            if (c == 32)
                break;
            if (c == '/')
                break;
            if (c == '\t')
                break;
            if (c == '=')
                break;
            // allow stuff like cd.. and dir.exe cd\kees
            if ((c == '.') || (c == '\\')) {
                cmdWrite.set((char) 0);
                if (!cmdBuffer.isEmpty() && cmdBuffer.length() > 0
                        && cmdList.containsKey(cmdBuffer.toString())) {
                    line = line.substring(lineIdx);
                    cmdList.get(cmdBuffer.toString()).Handler.run(CStringPt.create(line));
                    return;
                }
            }
            cmdWrite.set(c);
            cmdWrite.movePtToR1();
            lineIdx++;
        }
        cmdWrite.set((char) 0);
        if (cmdBuffer.length() == 0)
            return;
        line = line.substring(lineIdx);
        /* Check the internal list */
        if (!cmdBuffer.isEmpty() && cmdBuffer.length() > 0
                && cmdList.containsKey(cmdBuffer.toString())) {
            cmdWrite.movePtToR1();
            CStringPt.copy(line, cmdWrite);
            cmdList.get(cmdBuffer.toString()).Handler.run(cmdWrite);
            return;
        }
        /* This isn't an internal command execute it */
        if (execute(cmdBuffer.toString(), line))
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
            if (!val.equals(SetupModule.NO_SUCH_PROPERTY))
                writeOut("%s\n", val);
            return true;
        }
        String newCom = "z:\\config " + test.getName() + " " + cmdIn + line;
        doCommand(newCom);
        return true;
    }

    // return값은 리턴 유무를 가리킴
    private boolean help(CStringPt args, String command) {
        if (Support.scanCmdBool(args, "?")) {
            writeOut(Message.get("SHELL_CMD_" + command + "_HELP"));
            String longM = Message.get("SHELL_CMD_" + command + "_HELP_LONG");
            writeOut("\n");
            if (!longM.equals("Message not Found!\n"))
                writeOut(longM);
            else
                writeOut(command + "\n");
            return true;
        }
        return false;
    }

    /* Some supported commands */
    // static char empty_char = 0;
    // static char* empty_string = &empty_char;
    private static char emptyChar = (char) 0;
    private static CStringPt emptyString = CStringPt.create(new char[] {emptyChar});
    private static int emptyStringIdx = 0;

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
                    cmdPAUSE(emptyString);
            }
            cmd_index++;
        }
    }

    private void cmdCLS(CStringPt args) {
        help(args, "CLS");
        Register.setRegAX(0x0003);
        Callback.runRealInt(0x10);
    }

    static CStringPt defaultTarget = CStringPt.create(".");
    static byte[] buffer = new byte[0x8000]; // static, otherwise stack overflow possible.

    private void cmdCOPY(CStringPt args) {
        if (help(args, "COPY"))
            return;
        args.lTrim();
        /* Command uses dta so set it to our internal dta */
        int save_dta = DOSMain.DOS.getDTA();
        DOSMain.DOS.setDTA(DOSMain.DOS.tables.TempDTA);
        DOSDTA dta = new DOSDTA(DOSMain.DOS.getDTA());
        int attr = 0;// uint8
        CStringPt name = CStringPt.create(DOSSystem.DOS_NAMELENGTH_ASCII);
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
        CStringPt sourceX = CStringPt.create(DOSSystem.DOS_PATHLENGTH + Cross.LEN);

        while (!(sourceP = Support.stripWord(args)).isEmpty() && sourceP.get() != 0) {
            do {
                CStringPt plus = sourceP.positionOf('+');
                if (!plus.isEmpty())
                    plus.set((char) 0);
                plus.movePtToR1();
                CStringPt.safeCopy(sourceP, sourceX, Cross.LEN);
                boolean hasDriveSpec = false;
                int sourceXLen = sourceX.length();
                if (sourceXLen > 0) {
                    if (sourceX.get(sourceXLen - 1) == ':')
                        hasDriveSpec = true;
                }
                if (!hasDriveSpec) {
                    if (DOSMain.findFirst(sourceP.toString(),
                            0xffff & ~DOSSystem.DOS_ATTR_VOLUME)) {
                        dta.getResultName(name);
                        attr = dta.getResultAttr();
                        if ((attr & DOSSystem.DOS_ATTR_DIRECTORY) != 0
                                && sourceP.positionOf("*.*").isEmpty())
                            sourceX.concat("\\*.*");
                    }
                }
                sources.add(new CopySource(sourceX.toString(), !plus.isEmpty()));
                CStringPt.copyPt(plus, sourceP);
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
            target = new CopySource(defaultTarget.toString(), true);

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
            CStringPt pathSource = CStringPt.create(DOSSystem.DOS_PATHLENGTH);
            CStringPt pathTarget = CStringPt.create(DOSSystem.DOS_PATHLENGTH);

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
                    dta.getResultName(name);
                    attr = dta.getResultAttr();
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

            int sourceHandle = 0, targetHandle = 0;
            CStringPt nameTarget = CStringPt.create(DOSSystem.DOS_PATHLENGTH);
            CStringPt nameSource = CStringPt.create(DOSSystem.DOS_PATHLENGTH);

            while (ret) {
                dta.getResultName(name);
                attr = dta.getResultAttr();
                if ((attr & DOSSystem.DOS_ATTR_DIRECTORY) == 0) {
                    CStringPt.copy(pathSource, nameSource);
                    nameSource.concat(name);
                    // Open Source
                    if (DOSMain.openFile(nameSource.toString(), 0)) {
                        sourceHandle = DOSMain.returnFileHandle;
                        // Create Target or open it if in concat mode
                        CStringPt.copy(pathTarget, nameTarget);
                        if (nameTarget.get(nameTarget.length() - 1) == '\\')
                            nameTarget.concat(name);

                        // Don't create a newfile when in concat mode
                        if (oldSource.concat || DOSMain.createFile(nameTarget.toString(), 0)) {
                            targetHandle = DOSMain.returnFileHandle;
                            long dummy = 0;
                            // In concat mode. Open the target and seek to the eof
                            if (!oldSource.concat || (DOSMain.openFile(nameTarget.toString(),
                                    DOSSystem.OPEN_READWRITE)
                                    && (dummy = DOSMain.seekFile(0xffff & targetHandle, dummy,
                                            DOSSystem.DOS_SEEK_END)) >= 0)) {
                                targetHandle = DOSMain.returnFileHandle;

                                // Copy
                                // buffer = new byte[0x8000];// static, otherwise stack overflow
                                // possible.
                                boolean failed = false;
                                int toread = 0x8000;
                                do {
                                    failed |= DOSMain.readFile(sourceHandle, buffer, 0, toread);
                                    toread = DOSMain.readSize();
                                    failed |= DOSMain.writeFile(0xffff & targetHandle, buffer, 0,
                                            toread);
                                    toread = DOSMain.WrittenSize;
                                } while (toread == 0x8000);
                                failed |= DOSMain.closeFile(sourceHandle);
                                failed |= DOSMain.closeFile(0xffff & targetHandle);
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
        CStringPt path = CStringPt.create(DOSSystem.DOS_PATHLENGTH);

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

        CStringPt buffer = CStringPt.create(Cross.LEN);
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
            if (DOSMain.tryFileAttr(args.toString())
                    && (DOSMain.returnFileAttr() & DOSSystem.DOS_ATTR_DIRECTORY) != 0) {
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

        CStringPt name = null;
        int size, date, time, attr;
        do { /* File name and extension */
            name = CStringPt.create(DOSSystem.DOS_NAMELENGTH_ASCII);
            dta.getResultName(name);
            size = dta.getResultSize();
            date = dta.getResultDate();
            time = dta.getResultTime();
            attr = dta.getResultAttr();
            /* Skip non-directories if option AD is present */
            if (optAD && (attr & DOSSystem.DOS_ATTR_DIRECTORY) == 0)
                continue;

            /* output the file */
            if (optB) {
                // this overrides pretty much everything
                if (!name.equals(".") && !name.equals("..")) {
                    writeOut("%s\n", name);
                }
            } else {
                CStringPt ext = CStringPt.clone(emptyString);
                if (!optW && (name.get(0) != '.')) {
                    ext = name.lastPositionOf('.');
                    if (ext.isEmpty())
                        CStringPt.copyPt(emptyString, ext);
                    else {
                        ext.set((char) 0);
                        ext.movePtToR1();
                    }
                }
                int day = date & 0x001f;
                int month = (date >>> 5) & 0x000f;
                int year = (date >>> 9) + 1980;
                int hour = (time >>> 5) >>> 6;
                int minute = (time >>> 5) & 0x003f;

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
                cmdPAUSE(emptyString);
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
            int drive = dta.getSearchDrive();
            // TODO Free Space
            int freeSpace = 1024 * 1024 * 100;
            if (DOSMain.Drives[drive] != null) {
                DriveAllocationInfo alloc = new DriveAllocationInfo(0, 0, 0, 0);
                DOSMain.Drives[drive].allocationInfo(alloc);
                freeSpace = alloc.bytesSector * alloc.sectorsCluster * alloc.freeClusters;
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

        CStringPt full = CStringPt.create(DOSSystem.DOS_PATHLENGTH);
        CStringPt buffer = CStringPt.create(Cross.LEN);
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
        CStringPt name = CStringPt.create(DOSSystem.DOS_NAMELENGTH_ASCII);
        int attr = 0;// uint8

        DOSDTA dta = new DOSDTA(DOSMain.DOS.getDTA());
        while (res) {
            dta.getResultName(name);
            attr = dta.getResultAttr();

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
        CStringPt pbuffer = CStringPt.clone(buffer);
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
            char drive = (char) (DOSMain.getDefaultDrive() + 'A');
            CStringPt dir = CStringPt.create(DOSSystem.DOS_PATHLENGTH);
            DOSMain.getCurrentDir(0, dir);
            writeOut("{0}:\\{1}\n", drive, dir);
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
                char drive = (char) ('A' + DOSMain.getDefaultDrive());
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
            CStringPt parsed = CStringPt.create(ShellInner.CMD_MAXLINE);
            CStringPt p_parsed = CStringPt.clone(parsed);
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
                    CStringPt.copyPt(second, p);
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

            int n = 0;// uint8
            do
                n = 0xff & (n * 10 + (word.get() - '0'));
            while (Character.isDigit((word.movePtToR1()).get()));
            if (word.get() != 0 && !Character.isWhitespace(word.get())) {
                writeOut(Message.get("SHELL_CMD_IF_ERRORLEVEL_INVALID_NUMBER"));
                return;
            }
            /* Read the error code from DOS */
            if ((DOSMain.DOS.ReturnCode >= n) == (!hasNot))
                doCommand(args.toString());
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
                    doCommand(args.toString());
            }
            return;
        }

        /* Normal if string compare */

        CStringPt word1 = CStringPt.clone(args);
        // first word is until space or =
        while (args.get() != 0 && !Character.isWhitespace(args.get()) && (args.get() != '='))
            args.movePtToR1();
        CStringPt endWord1 = CStringPt.clone(args);

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

        CStringPt word2 = CStringPt.clone(args);
        // second word is until space or =
        while (args.get() != 0 && !Character.isWhitespace(args.get()) && (args.get() != '='))
            args.movePtToR1();

        if (args.get() != 0) {
            endWord1.set((char) 0); // mark end of first word
            args.set((char) 0);
            args.movePtToR1(); // mark end of second word
            Support.stripSpaces(args, '=');

            if (word1.equals(word2) == (!hasNot))
                doCommand(args.toString());
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
        CStringPt nonSpace = CStringPt.clone(args);
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
        int handle = 0;
        CStringPt word;
        // nextfile:
        while (true) {
            word = Support.stripWord(args);
            if (!DOSMain.openFile(word.toString(), 0)) {
                writeOut(Message.get("SHELL_CMD_FILE_NOT_FOUND"), word);
                return;
            }
            handle = DOSMain.returnFileHandle;
            int n;
            byte c = 0;
            do {
                // n = 1;
                DOSMain.readFile(handle);
                n = DOSMain.readSize();
                c = DOSMain.ReadByte;
                DOSMain.writeFile(DOSMain.STDOUT, c, n);
                n = DOSMain.WrittenSize;
            } while (n != 0);
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

            CStringPt dirSource = CStringPt.create(DOSSystem.DOS_PATHLENGTH);
            dirSource.set(0, (char) 0);
            // Copy first and then modify, makes GCC happy
            CStringPt.copy(arg1, dirSource);
            CStringPt dummy = dirSource.lastPositionOf('\\');
            dummy.set((char) 0);

            if ((dirSource.length() == 2) && (dirSource.get(1) == ':'))
                dirSource.concat("\\"); // X: add slash

            CStringPt dirCurrent = CStringPt.create(DOSSystem.DOS_PATHLENGTH + 1);
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
        DOSMain.readFile(DOSMain.STDIN);
    }

    private void cmdSUBST(CStringPt args) {
        /*
         * If more that one type can be substed think of something else E.g. make basedir member
         * dos_drive instead of localdrive
         */
        if (help(args, "SUBST"))
            return;
        LocalDrive ldp = null;
        CStringPt mountString = CStringPt.create(DOSSystem.DOS_PATHLENGTH + Cross.LEN + 20);
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
            arg = command.findCommand(2) ? command.returnedCmd : arg;
            if ((arg.equals("/D")) || (arg.equals("/d")))
                throw new DOSException("1"); // No removal (one day)

            arg = command.findCommand(1) ? command.returnedCmd : arg;
            if ((arg.length() > 1) && arg.charAt(1) != ':')
                throw new DOSException("0");
            tempStr.set(0, Character.toUpperCase(args.get(0)));
            if (DOSMain.Drives[tempStr.get(0) - 'A'] != null)
                throw new DOSException("0"); // targetdrive in use
            mountString.concat(tempStr);
            mountString.concat(" ");

            arg = command.findCommand(2) ? command.returnedCmd : arg;
            if (!DOSMain.makeFullName(arg.toUpperCase(), DOSSystem.DOS_PATHLENGTH))
                throw new DOSException("0");
            String fullDir = DOSMain.returnedFullName;
            int drive = DOSMain.returnedFullNameDrive;
            DOSDrive drv = DOSMain.Drives[drive];
            if (!(DOSMain.Drives[drive] instanceof LocalDrive))
                throw new DOSException("0");
            ldp = (LocalDrive) drv;
            CStringPt newname = CStringPt.create(Cross.LEN);
            CStringPt.copy(ldp.basedir, newname);
            newname.concat(fullDir);
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
        int umbFlag = DOSMain.DOSInfoBlock.getUMBChainState();
        byte oldMemstrat = (byte) (DOSMain.getMemAllocStrategy() & 0xff);
        if (umbStart == 0x9fff) {
            if ((umbFlag & 1) == 0)
                DOSMain.linkUMBsToMemChain(1);
            DOSMain.setMemAllocStrategy(0x80); // search in UMBs first
            this.parseLine(args);
            int currentUmbFlag = DOSMain.DOSInfoBlock.getUMBChainState();
            if ((currentUmbFlag & 1) != (umbFlag & 1))
                DOSMain.linkUMBsToMemChain(umbFlag);
            DOSMain.setMemAllocStrategy(oldMemstrat); // restore strategy
        } else
            this.parseLine(args);
    }

    private void cmdCHOICE(CStringPt args) {
        if (help(args, "CHOICE"))
            return;
        CStringPt rem = CStringPt.getZero();
        CStringPt ptr;
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
            rem = CStringPt.create("yn"); /* No choices specified use YN */
        ptr = CStringPt.clone(rem);
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
        do {
            DOSMain.readFile(DOSMain.STDIN);
            c = DOSMain.ReadByte;
            n = DOSMain.readSize();
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
            CStringPt pathstring = CStringPt.create(DOSSystem.DOS_PATHLENGTH + Cross.LEN + 20);
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
        int strLen = 0;
        int strIndex = 0;
        int len = 0;
        boolean currentHist = false; // current command stored in history?

        line.set(0, '\0');
        String currHisStr = null;
        if (!history.isEmpty())
            currHisStr = history.getFirst();
        int currHisIdx = 0;
        String currCmpStr = null;
        if (!completion.isEmpty())
            currCmpStr = completion.getFirst();
        int currCmpIdx = 0;
        while (size > 0) {
            DOSMain.DOS.Echo = false;
            while (!DOSMain.readFile(InputHandle)) {
                int dummy = 0;
                DOSMain.closeFile(InputHandle);
                DOSMain.openFile("con", 2);
                dummy = DOSMain.returnFileHandle;
                Log.logging(Log.LogTypes.MISC, Log.LogServerities.Error,
                        "Reopening the input handle.This is a bug!");
            }
            c = DOSMain.ReadByte;
            n = DOSMain.readSize();
            if (n == 0) {
                size = 0; // Kill the while loop
                continue;
            }
            switch (c) {
                case 0x00: /* Extended Keys */
                {
                    DOSMain.readFile(InputHandle);
                    c = DOSMain.ReadByte;
                    n = DOSMain.readSize();
                    switch (c) {

                        case 0x3d: /* F3 */
                            if (history.size() == 0)
                                break;
                            currHisStr = history.getFirst();
                            currHisIdx = 0;
                            if (currHisStr != null && currHisStr.length() > strLen) {
                                int readerIdx = strLen;
                                while (readerIdx < currHisStr.length()
                                        && (c = (byte) currHisStr.charAt(readerIdx++)) != 0) {
                                    line.set(strIndex++, (char) c);
                                    DOSMain.writeFile(DOSMain.STDOUT, c);
                                }
                                strLen = strIndex = currHisStr.length();
                                size = ShellInner.CMD_MAXLINE - strIndex - 2;
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
                            if (currHisStr == null || history.size() == 0)
                                break;

                            // store current command in history if we are at beginning
                            if (currHisStr.equals(history.getFirst()) && !currentHist) {
                                currentHist = true;
                                history.addFirst(line.toString());
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
                            size = ShellInner.CMD_MAXLINE - strIndex - 2;
                            DOSMain.writeFile(DOSMain.STDOUT, line.getAsciiBytes(), 0, len);
                            len = DOSMain.WrittenSize;
                            currHisStr = history.get(++currHisIdx);
                            break;
                        }
                        case 0x50: /* DOWN */
                        {
                            if (history.size() == 0 || currHisStr.equals(history.getFirst()))
                                break;

                            // not very nice but works ..
                            currHisStr = history.get(--currHisIdx);

                            if (currHisStr.equals(history.getFirst())) {
                                // no previous commands in history
                                currHisStr = history.get(++currHisIdx);

                                // remove current command from history
                                if (currentHist) {
                                    currentHist = false;
                                    history.removeFirst();
                                }
                                break;
                            } else {
                                currHisStr = history.get(--currHisIdx);
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
                            size = ShellInner.CMD_MAXLINE - strIndex - 2;
                            DOSMain.writeFile(DOSMain.STDOUT, line.getAsciiBytes(), 0, len);
                            len = DOSMain.WrittenSize;
                            currHisStr = history.get(++currHisIdx);

                            break;
                        }
                        case 0x53:/* DELETE */
                        {
                            if (strIndex >= strLen)
                                break;
                            short a = (short) (strLen - strIndex - 1);
                            byte[] text = CStringPt.clone(line, strIndex + 1).getAsciiBytes();
                            DOSMain.writeFile(DOSMain.STDOUT, text, 0, text.length);// write buffer
                                                                                    // to screen
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
                            for (int i = strIndex; i < strLen; i++)
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
                    if (completion.size() != 0)
                        completion.clear();
                    break;
                case 0x0a: /* New Line not handled */
                    /* Don't care */
                    break;
                case 0x0d: /* Return */
                    outc((byte) '\n');
                    size = 0; // Kill the while loop
                    break;
                case (byte) '\t': {
                    if (completion.size() > 0) {
                        currCmpStr = completion.get(++currCmpIdx);
                        if (currCmpStr == null) {
                            currCmpIdx = 0;
                            currCmpStr = completion.getFirst();
                        }
                    } else {
                        // build new completion list
                        // Lines starting with CD will only get directories in the list
                        boolean dirOnly =
                                line.toString().substring(0, 3).compareToIgnoreCase("CD") == 0;
                        // get completion mask
                        CStringPt pCompletionStart = line.lastPositionOf(' ');

                        if (!pCompletionStart.isEmpty()) {
                            pCompletionStart.movePtToR1();
                            completionIndex = strLen - pCompletionStart.length();
                        } else {
                            pCompletionStart = line;
                            completionIndex = 0;
                        }

                        CStringPt path;
                        if (!(path = CStringPt.clone(line, completionIndex).lastPositionOf('\\'))
                                .isEmpty())
                            completionIndex = CStringPt.diff(path, line) + 1;
                        if (!(path = CStringPt.clone(line, completionIndex).lastPositionOf('/'))
                                .isEmpty())
                            completionIndex = CStringPt.diff(path, line) + 1;

                        CStringPt mask = CStringPt.create(DOSSystem.DOS_PATHLENGTH);
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
                        int att = 0;// uint8

                        LinkedList<String> executable = new LinkedList<String>();
                        while (res) {
                            dta.getResultName(name);
                            att = dta.getResultAttr();

                            // add result to completion list

                            CStringPt ext; // file extension
                            if (name.equals(".") && name.equals("..")) {
                                if (dirOnly) { // Handle the dir only case different (line starts
                                               // with cd)
                                    if ((att & DOSSystem.DOS_ATTR_DIRECTORY) > 0)
                                        completion.addLast(name.toString());
                                } else {
                                    ext = name.lastPositionOf('.');
                                    if (!ext.isEmpty() && (ext.equals(".BAT") || ext.equals(".COM")
                                            || ext.equals(".EXE")))
                                        // we add executables to the a seperate list and place that
                                        // list infront of the normal files
                                        executable.addFirst(name.toString());
                                    else
                                        completion.addLast(name.toString());
                                }
                            }
                            res = DOSMain.findNext();
                        }

                        /* Add excutable list to front of completion list. */
                        for (String nd : executable) {
                            completion.add(0, nd);
                        }

                        currCmpIdx = 0;
                        currCmpStr = completion.getFirst();
                        DOSMain.DOS.setDTA(saveDTA);
                    }

                    if (completion.size() > 0 && currCmpStr.length() > 0) {
                        for (; strIndex > completionIndex; strIndex--) {
                            // removes all characters
                            outc(8);
                            outc((byte) ' ');
                            outc(8);
                        }

                        CStringPt.copy(currCmpStr, CStringPt.clone(line, completionIndex));
                        len = currCmpStr.length();
                        strLen = strIndex = completionIndex + len;
                        size = ShellInner.CMD_MAXLINE - strIndex - 2;
                        DOSMain.writeFile(DOSMain.STDOUT,
                                CStringPt.create(currCmpStr).getAsciiBytes(), 0, len);
                        len = DOSMain.WrittenSize;
                    }
                }
                    break;
                case 0x1b: /* ESC */
                    // write a backslash and return to the next line
                    outc((byte) '\\');
                    outc((byte) '\n');
                    line.set(0, (char) 0); // reset the line.
                    if (completion.size() > 0)
                        completion.clear(); // reset the completion list.
                    this.inputCommand(line); // Get the NEW line.
                    size = 0; // stop the next loop
                    strLen = 0; // prevent multiple adds of the same line
                    break;
                default:
                    if (completion.size() > 0)
                        completion.clear();
                    // mem_readb(BIOS_KEYBOARD_FLAGS1)&0x80) dev_con.h ?
                    if (strIndex < strLen && true) {
                        outc((byte) ' ');// move cursor one to the right.
                        int a = strLen - strIndex;
                        // write buffer to screen
                        DOSMain.writeFile(DOSMain.STDOUT, line.getAsciiBytes(), 0, a);
                        a = DOSMain.WrittenSize;
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
            history.removeFirst();
        }

        // add command line to history
        history.add(0, line.toString());
        currHisIdx = 0;
        currHisStr = history.getFirst();
        if (completion.size() > 0)
            completion.clear();
    }

    public boolean execute(String name, String args) {
        /*
         * return true => don't check for hardware changes in do_command return false => check for
         * hardware changes in do_command
         */
        // stores results from Which
        String fullname = null;// DOSSystem.DOS_PATHLENGTH + 4
        String line = null;// ShellInner.CMD_MAXLINE

        final int maxLine = ShellInner.CMD_MAXLINE;
        int argsIdx = 0;
        if (args.length() != 0) {
            if (args.charAt(argsIdx) != ' ') { // put a space in front
                line = ' ' + (args.length() < maxLine - 2 ? args : args.substring(0, maxLine - 2));
            } else {
                line = args.length() < maxLine - 1 ? args : args.substring(0, maxLine - 1);
            }
        } else {
            line = "";
        }

        /* check for a drive change */
        String rem = name.substring(1);
        if ((rem.equals(":") || rem.equals(":\\")) && Character.isLetter(name.charAt(0))) {
            char drive = Character.toUpperCase(name.charAt(0));
            if (!DOSMain.setDrive(drive - 'A')) {
                writeOut(Message.get("SHELL_EXECUTE_DRIVE_NOT_FOUND"), drive);
            }
            return true;
        }
        /* Check for a full name */
        fullname = which(name);
        if (fullname == null)
            return false;

        int dotIdx = -1;
        String extension = (dotIdx = fullname.indexOf(".")) < 0 ? "" : fullname.substring(dotIdx);
        /* always disallow files without extension from being executed. */
        /* only internal commands can be run this way and they never get in this handler */
        if (fullname.indexOf(".") < 0) {
            // Check if the result will fit in the parameters. Else abort
            if (fullname.length() > (DOSSystem.DOS_PATHLENGTH - 1))
                return false;
            String tempFullname = null;
            // try to add .com, .exe and .bat extensions to filename

            tempFullname = which(fullname + ".COM");
            if (tempFullname != null) {
                extension = ".com";
                fullname = tempFullname;
            } else {
                tempFullname = which(fullname + ".EXE");
                if (tempFullname != null) {
                    extension = ".exe";
                    fullname = tempFullname;
                } else {
                    tempFullname = which(fullname + ".BAT");
                    if (tempFullname != null) {
                        extension = ".bat";
                        fullname = tempFullname;
                    } else {
                        return false;
                    }
                }
            }
        }

        if (extension.equalsIgnoreCase(".bat")) { /* Run the .bat file */
            /* delete old batch file if call is not active */
            boolean tempEcho =
                    Echo; /* keep the current echostate (as delete bf might change it ) */
            if (BatFile != null && !Call)
                BatFile.dispose();
            BatFile = new BatchFile(this, fullname.toString(), name, line.toString());
            Echo = tempEcho; // restore it.
        } else { /* only .bat .exe .com extensions maybe be executed by the shell */
            if (!extension.equalsIgnoreCase(".com")) {
                if (!extension.equalsIgnoreCase(".exe"))
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
            Memory.blockWrite(Memory.real2Phys(fileName),
                    fullname.getBytes(StandardCharsets.US_ASCII));// 마지막 null포함

            /* HACK: Store full commandline for mount and imgmount */
            FullArguments = line;

            /* Fill the command line */
            byte[] cmdTail = new byte[DOSMain.CommandTailSize];// 첫번째 값은 문자열이 아닌 Count
            // CStringPt csCmdTail = CStringPt.Create(cmdtail);
            cmdTail[DOSMain.CommandTailOffCount] = 0;
            // Else some part of the string is unitialized (valgrind)
            Arrays.fill(cmdTail, DOSMain.CommandTailOffBuffer, 126, (byte) 0);
            if (line.length() > 126)
                line = line.substring(0, 126);
            cmdTail[DOSMain.CommandTailOffCount] = (byte) line.length();
            ArrayHelper.copy(line.getBytes(StandardCharsets.US_ASCII), 0, cmdTail,
                    DOSMain.CommandTailOffBuffer, line.length());// 마지막 null 제외

            cmdTail[DOSMain.CommandTailOffBuffer + line.length()] = 0xd;

            /* Copy command line in stack block too */
            Memory.blockWrite(Register.segPhys(Register.SEG_NAME_SS) + Register.getRegSP() + 0x100,
                    cmdTail, 0, 128);
            /* Parse FCB (first two parameters) and put them into the current DOS_PSP */
            int add = 0;
            CStringPt cmdTailBuffer = CStringPt.create(DOSMain.CommandTail_Size_Buffer);
            for (int i = 0; i < DOSMain.CommandTail_Size_Buffer; i++) {
                cmdTailBuffer.set(i, (char) cmdTail[i + DOSMain.CommandTailOffBuffer]);
            }
            DOSMain.FCBParseName(DOSMain.DOS.getPSP(), 0x5C, 0x00, cmdTailBuffer);
            add = DOSMain.returnedFCBParseNameChange;
            DOSMain.FCBParseName(DOSMain.DOS.getPSP(), 0x6C, 0x00,
                    CStringPt.clone(cmdTailBuffer, add));
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
    static CStringPt whichRet = CStringPt.create(DOSSystem.DOS_PATHLENGTH + 4);

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
        CStringPt path = CStringPt.create(DOSSystem.DOS_PATHLENGTH);
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
                path.set(DOSSystem.DOS_PATHLENGTH - 1, (char) 0);
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

    protected String which(String name) {
        int nameLen = name.length();
        if (nameLen >= DOSSystem.DOS_PATHLENGTH)
            return null;

        /* Parse through the Path to find the correct entry */
        /* Check if name is already ok but just misses an extension */

        if (DOSMain.fileExists(name))
            return name;
        /* try to find .com .exe .bat */
        String findNm = name + comExt;
        if (DOSMain.fileExists(findNm))
            return findNm;
        findNm = name + exeExt;
        if (DOSMain.fileExists(findNm))
            return findNm;
        findNm = name + batExt;
        if (DOSMain.fileExists(findNm))
            return findNm;

        /* No Path in filename look through path environment string */
        // CStringPt path = CStringPt.create(DOSSystem.DOS_PATHLENGTH);
        StringBuffer path = new StringBuffer(DOSSystem.DOS_PATHLENGTH);
        String pathEnv = null;
        if ((pathEnv = getEnvStr("PATH")) == null)
            return null;
        int pathEnvIdx = pathEnv.indexOf("=");
        if (pathEnvIdx < 0)
            return null;
        pathEnvIdx++;
        int pathIdx = 0;

        while (pathEnvIdx < pathEnv.length()) {
            /* remove ; and ;; at the beginning. (and from the second entry etc) */
            while (pathEnvIdx < pathEnv.length() && pathEnv.charAt(pathEnvIdx) == ';')
                pathEnvIdx++;

            /* get next entry */
            pathIdx = 0; /* reset writer */
            while (pathEnvIdx < pathEnv.length() && pathEnv.charAt(pathEnvIdx) != ';'
                    && pathIdx < DOSSystem.DOS_PATHLENGTH)
                path.setCharAt(pathIdx++, pathEnv.charAt(++pathEnvIdx));

            if (pathIdx == DOSSystem.DOS_PATHLENGTH) {
                /* If max size. move till next ; and terminate path */
                while (pathEnv.charAt(pathEnvIdx) != ';')
                    pathEnvIdx++;
                path.setLength(DOSSystem.DOS_PATHLENGTH - 1);// path.set(DOSSystem.DOS_PATHLENGTH -
                                                             // 1, (char) 0);
            } else
                path.setLength(pathIdx); // path.set(pathIdx, (char) 0);

            /* check entry */
            int len = path.length();
            if (len != 0) {
                if (len >= (DOSSystem.DOS_PATHLENGTH - 2))
                    continue;

                if (path.charAt(len - 1) != '\\') {
                    path.append("\\");// concat('\\')
                    len++;
                }

                // If name too long =>next
                if ((nameLen + len + 1) >= DOSSystem.DOS_PATHLENGTH)
                    continue;
                path.append(name);// concat(name)

                name = path.toString();
                findNm = name;
                if (DOSMain.fileExists(findNm))
                    return findNm;
                findNm = name + comExt;
                if (DOSMain.fileExists(findNm))
                    return findNm;
                findNm = name + exeExt;
                if (DOSMain.fileExists(findNm))
                    return findNm;
                findNm = name + batExt;
                if (DOSMain.fileExists(findNm))
                    return findNm;
            }
        }
        return null;
    }

    /*--------------------------- end DOSShellMisc -----------------------------*/

}
