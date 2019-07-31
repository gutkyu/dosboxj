package org.gutkyu.dosboxj.shell;

import org.gutkyu.dosboxj.dos.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.gutkyu.dosboxj.misc.*;
import org.gutkyu.dosboxj.dos.software.*;
import org.gutkyu.dosboxj.util.*;
import org.gutkyu.dosboxj.hardware.memory.*;
import org.gutkyu.dosboxj.dos.mem_block.*;
import org.gutkyu.dosboxj.misc.setup.Value.WrongType;
import org.gutkyu.dosboxj.cpu.*;

public abstract class DOSShellBase extends Program {
    protected LinkedList<String> history = new LinkedList<String>(),
            completion = new LinkedList<String>();

    protected String _completionStart;
    protected int completionIndex;

    /* The shell's variables */
    public int InputHandle;// uint16
    public BatchFile BatFile = null;
    public boolean Echo;
    public boolean Exit;
    public boolean Call;

    protected DOSShellBase() {
        super();
        _completionStart = null;
    }

    abstract protected void doCommand(String line);

    abstract protected void inputCommand(CStringPt line);

    @Override
    public void run() {

        CStringPt inputLine = CStringPt.create(ShellInner.CMD_MAXLINE);
        inputLine.set(0, (char) 0);
        String line = null;
        if ((line = Cmd.findStringRemainS("/C")) != null) {
            CStringPt.copy(line, inputLine);
            int sep_idx = line.indexOf("\r\n");
            if (sep_idx > 0)
                inputLine.set(sep_idx, (char) 0);
            DOSShell temp = new DOSShell();
            temp.Echo = Echo;
            temp.parseLine(inputLine); // for *.exe *.com |*.bat creates the bf needed by
                                       // runinternal;
            temp.runInternal(); // exits when no bf is found.
            return;
        }

        /* Start a normal shell and check for a first command init */
        /*
         * writeOut(Message.get("SHELL_STARTUP_BEGIN"), StringHelper.padRight(DOSBox.VERSION, 8));
         * 
         * if (DOSBox.Machine == DOSBox.MachineType.CGA) writeOut(Message.get("SHELL_STARTUP_CGA"));
         * if (DOSBox.Machine == DOSBox.MachineType.HERC)
         * writeOut(Message.get("SHELL_STARTUP_HERC")); writeOut(Message.get("SHELL_STARTUP_END"));
         */
        if ((line = Cmd.findString("/INIT", true)) != null) {
            CStringPt.copy(line, inputLine);
            line = "";
            parseLine(inputLine);
        }
        do {
            if (BatFile != null) {
                if (BatFile.readLine(inputLine)) {
                    if (Echo) {
                        if (inputLine.get(0) != '@') {
                            showPrompt();
                            writeOutNoParsing(inputLine);
                            writeOutNoParsing("\n");
                        }
                    }
                    parseLine(inputLine);
                    if (Echo)
                        writeOut("\n");
                }
            } else {
                if (Echo)
                    showPrompt();
                inputCommand(inputLine);
                parseLine(inputLine);
                if (Echo && BatFile == null)
                    writeOutNoParsing("\n");
            }
        } while (!Exit);
    }

    protected void runInternal() // for command /C
    {
        CStringPt inputLine = CStringPt.create(ShellInner.CMD_MAXLINE);
        inputLine.set(0, (char) 0);
        while (BatFile != null && BatFile.readLine(inputLine)) {
            if (Echo) {
                if (inputLine.get(0) != '@') {
                    showPrompt();
                    writeOutNoParsing(inputLine);
                    writeOutNoParsing("\n");
                }
            }
            parseLine(inputLine);
        }
        return;
    }

    private void showPrompt() {
        char drive = (char) ('A' + DOSMain.getDefaultDrive());
        String dir = null;
        // DOS_GetCurrentDir doesn't always return something. (if drive is messed up)
        dir = DOSMain.getCurrentDir(0);
        writeOut(drive + ":\\" + dir + ">");
    }

    /* A load of subfunctions */
    public void parseLine(CStringPt line) {
        Log.logging(Log.LogTypes.EXEC, Log.LogServerities.Error, "Parsing command line: %s",
                line.toString());
        /* Check for a leading @ */
        if (line.get(0) == '@')
            line.set(0, ' ');
        line.trim();

        /* Do redirection and pipe checks */

        CStringPt inPt = CStringPt.create();
        CStringPt outPt = CStringPt.create();

        // short dummy=0, dummy2=0;
        int num = 0; /* Number of commands in this line */
        boolean append = false;
        boolean normalstdin = false; /* wether stdin/out are open on start. */
        boolean normalstdout = false; /* Bug: Assumed is they are "con" */

        num = getRedirection(line, inPt, outPt, append);
        append = returnedGetRedirectionAppend;
        if (num > 1)
            Log.logMsg("SHELL:Multiple command on 1 line not supported");
        if (!inPt.isEmpty() || !outPt.isEmpty()) {
            normalstdin = (PSP.getFileHandle(0) != 0xff);
            normalstdout = (PSP.getFileHandle(1) != 0xff);
        }
        if (!inPt.isEmpty()) {
            // Test if file exists
            if (DOSMain.openFile(inPt.toString(), DOSSystem.OPEN_READ)) {
                DOSMain.closeFile(DOSMain.returnFileHandle);
                Log.logMsg("SHELL:Redirect input from %s", inPt);
                if (normalstdin)
                    DOSMain.closeFile(0); // Close stdin
                DOSMain.openFile(inPt.toString(), DOSSystem.OPEN_READ); // Open new stdin
            }

        }
        if (!outPt.isEmpty()) {
            Log.logMsg("SHELL:Redirect output to %s", outPt);
            if (normalstdout)
                DOSMain.closeFile(1);
            if (!normalstdin && inPt.isEmpty())
                DOSMain.openFile("con", DOSSystem.OPEN_READWRITE);
            boolean status = true;
            /* Create if not exist. Open if exist. Both in read/write mode */
            if (append) {
                if ((status = DOSMain.openFile(outPt.toString(), DOSSystem.OPEN_READWRITE))) {
                    DOSMain.seekFile(1, 0, DOSSystem.DOS_SEEK_END);
                } else {
                    // Create if not exists.
                    status = DOSMain.createFile(outPt.toString(), DOSSystem.DOS_ATTR_ARCHIVE);
                }
            } else {
                status = DOSMain.openFileExtended(outPt.toString(), DOSSystem.OPEN_READWRITE,
                        DOSSystem.DOS_ATTR_ARCHIVE, 0x12);
            }

            if (!status && normalstdout) {
                // Read only file, open con again
                DOSMain.openFile("con", DOSSystem.OPEN_READWRITE);
            }
            if (!normalstdin && inPt.isEmpty())
                DOSMain.closeFile(0);
        }
        /* Run the actual command */
        doCommand(line.toString());
        /* Restore handles */
        if (!inPt.isEmpty()) {
            DOSMain.closeFile(0);
            if (normalstdin)
                DOSMain.openFile("con", DOSSystem.OPEN_READWRITE);
            inPt.empty();
        }
        if (!outPt.isEmpty()) {
            DOSMain.closeFile(1);
            if (!normalstdin)
                DOSMain.openFile("con", DOSSystem.OPEN_READWRITE);
            if (normalstdout)
                DOSMain.openFile("con", DOSSystem.OPEN_READWRITE);
            if (!normalstdin)
                DOSMain.closeFile(0);
            outPt.empty();
        }
    }

    private boolean returnedGetRedirectionAppend;

    // Bitu GetRedirection(char *s, char **ifn, char **ofn,bool * append)
    public int getRedirection(CStringPt line, CStringPt ifn, CStringPt ofn, boolean append) {
        CStringPt lr = CStringPt.clone(line);
        CStringPt lw = CStringPt.clone(line);
        char ch;
        int num = 0;
        boolean quote = false;
        CStringPt t;

        while ((ch = lr.get()) > 0) {
            lr.movePtToR1();
            if (quote && ch != '"') { /*
                                       * don't parse redirection within quotes. Not perfect yet.
                                       * Escaped quotes will mess the count up
                                       */
                lw.set(ch);
                lw.movePtToR1();
                continue;
            }

            switch (ch) {
                case '"':
                    quote = !quote;
                    break;
                case '>':
                    returnedGetRedirectionAppend = append = ((lr.get()) == '>');
                    if (append)
                        lr.movePtToR1();
                    lr = lr.lTrim();
                    if (!ofn.isEmpty())
                        ofn.empty();
                    CStringPt.copyPt(lr, ofn);
                    while (lr.get() != 0 && lr.get() != ' ' && lr.get() != '<' && lr.get() != '|')
                        lr.movePtToR1();
                    // if it ends on a : => remove it.
                    if ((ofn.get() != lr.get()) && (lr.get(-1) == ':'))
                        lr.set(-1, (char) 0);
                    // if(*lr && *(lr+1))
                    // *lr++=0;
                    // else
                    // *lr=0;
                    t = CStringPt.create(CStringPt.diff(lr, ofn) + 1);
                    CStringPt.safeCopy(t, ofn, CStringPt.diff(lr, ofn) + 1);
                    CStringPt.copyPt(t, ofn);
                    continue;
                case '<':
                    lr.lTrim();
                    if (!ifn.isEmpty())
                        ifn.empty();
                    CStringPt.copyPt(lr, ifn);
                    while (lr.get() != 0 && lr.get() != ' ' && lr.get() != '>' && lr.get() != '|')
                        lr.movePtToR1();
                    if ((CStringPt.notEqual(ifn, lr)) && (lr.get(-1) == ':'))
                        lr.set(-1, (char) 0);
                    // if(*lr && *(lr+1))
                    // *lr++=0;
                    // else
                    // *lr=0;
                    t = CStringPt.create(CStringPt.diff(lr, ifn) + 1);
                    CStringPt.safeCopy(t, ifn, CStringPt.diff(lr, ifn) + 1);
                    CStringPt.copyPt(t, ifn);
                    continue;
                case '|':
                    ch = (char) 0;
                    num++;
                    break;
            }
            lw.set(ch);
            lw.movePtToR1();
        }
        lw.set((char) 0);
        return num;
    }

    public void syntaxError() {
        writeOut(Message.get("SHELL_SYNTAXERROR"));
    }

    /*--------------------------- begin DOSShellPartial -----------------------------*/
    private static int _callShellStop = 0;

    public static int callShellStop() {
        return _callShellStop;
    }
    /*
     * Larger scope so shell_del autoexec can use it to remove things from the environment
     */

    /*
     * first_shell is used to add and delete stuff from the shell env by "external" programs.
     * (config)
     */
    private static Program _firstShell = null;

    public static Program firstShell() {
        return _firstShell;
    }

    public static int shellStopHandler() {
        return Callback.ReturnTypeStop;
    }

    public static Program makeProgram() {
        return new DOSShell();
    }

    private static final byte[] pathString = "PATH=Z:\\\0".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] comspecString =
            "COMSPEC=Z:\\COMMAND.COM\0".getBytes(StandardCharsets.US_ASCII);;
    private static final byte[] fullName = "Z:\\COMMAND.COM\0".getBytes(StandardCharsets.US_ASCII);;
    private static final byte[] initLine =
            "/INIT AUTOEXEC.BAT\0".getBytes(StandardCharsets.US_ASCII);;

    public static void init() throws WrongType {
        /* Add messages */
        Message.addMsg("SHELL_ILLEGAL_PATH", "Illegal Path.\n");
        Message.addMsg("SHELL_CMD_HELP",
                "If you want a list of all supported commands type \u001B[33;1mhelp /all\u001B[0m .\nA short list of the most often used commands:\n");
        Message.addMsg("SHELL_CMD_ECHO_ON", "ECHO is on.\n");
        Message.addMsg("SHELL_CMD_ECHO_OFF", "ECHO is off.\n");
        Message.addMsg("SHELL_ILLEGAL_SWITCH", "Illegal switch: {0}.\n");
        Message.addMsg("SHELL_MISSING_PARAMETER", "Required parameter missing.\n");
        Message.addMsg("SHELL_CMD_CHDIR_ERROR", "Unable to change to: {0}.\n");
        Message.addMsg("SHELL_CMD_CHDIR_HINT",
                "To change to different drive type \u001B[31m{0}:\u001B[0m\n");
        Message.addMsg("SHELL_CMD_CHDIR_HINT_2",
                "directoryname is longer than 8 characters and/or contains spaces.\nTry \u001B[31mcd {0}\u001B[0m\n");
        Message.addMsg("SHELL_CMD_CHDIR_HINT_3",
                "You are still on drive Z:, change to a mounted drive with \u001B[31mC:\u001B[0m.\n");
        Message.addMsg("SHELL_CMD_MKDIR_ERROR", "Unable to make: {0}.\n");
        Message.addMsg("SHELL_CMD_RMDIR_ERROR", "Unable to remove: {0}.\n");
        Message.addMsg("SHELL_CMD_DEL_ERROR", "Unable to delete: {0}.\n");
        Message.addMsg("SHELL_SYNTAXERROR", "The syntax of the command is incorrect.\n");
        Message.addMsg("SHELL_CMD_SET_NOT_SET", "Environment variable {0} not defined.\n");
        Message.addMsg("SHELL_CMD_SET_OUT_OF_SPACE", "Not enough environment space left.\n");
        Message.addMsg("SHELL_CMD_IF_EXIST_MISSING_FILENAME", "IF EXIST: Missing filename.\n");
        Message.addMsg("SHELL_CMD_IF_ERRORLEVEL_MISSING_NUMBER",
                "IF ERRORLEVEL: Missing number.\n");
        Message.addMsg("SHELL_CMD_IF_ERRORLEVEL_INVALID_NUMBER",
                "IF ERRORLEVEL: Invalid number.\n");
        Message.addMsg("SHELL_CMD_GOTO_MISSING_LABEL", "No label supplied to GOTO command.\n");
        Message.addMsg("SHELL_CMD_GOTO_LABEL_NOT_FOUND", "GOTO: Label {0} not found.\n");
        Message.addMsg("SHELL_CMD_FILE_NOT_FOUND", "File {0} not found.\n");
        Message.addMsg("SHELL_CMD_FILE_EXISTS", "File {0} already exists.\n");
        Message.addMsg("SHELL_CMD_DIR_INTRO", "Directory of %s.\n");
        Message.addMsg("SHELL_CMD_DIR_BYTES_USED", "%5d File(s) %17s Bytes.\n");
        Message.addMsg("SHELL_CMD_DIR_BYTES_FREE", "%5d Dir(s)  %17s Bytes free.\n");
        Message.addMsg("SHELL_EXECUTE_DRIVE_NOT_FOUND",
                "Drive {0} does not exist!\nYou must \u001B[31mmount\u001B[0m it first. Type \u001B[1;33mintro\u001B[0m or \u001B[1;33mintro mount\u001B[0m for more information.\n");
        Message.addMsg("SHELL_EXECUTE_ILLEGAL_COMMAND", "Illegal command: {0}.\n");
        Message.addMsg("SHELL_CMD_PAUSE", "Press any key to continue.\n");
        Message.addMsg("SHELL_CMD_PAUSE_HELP", "Waits for 1 keystroke to continue.\n");
        Message.addMsg("SHELL_CMD_COPY_FAILURE", "Copy failure : {0}.\n");
        Message.addMsg("SHELL_CMD_COPY_SUCCESS", "   {0} File(s) copied.\n");
        Message.addMsg("SHELL_CMD_SUBST_NO_REMOVE",
                "Removing drive not supported. Doing nothing.\n");
        Message.addMsg("SHELL_CMD_SUBST_FAILURE",
                "SUBST failed. You either made an error in your commandline or the target drive is already used.\nIt's only possible to use SUBST on Local drives");

        Message.addMsg("SHELL_STARTUP_BEGIN",
                "\u001B[44;1m\u00C9\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD"
                        + "\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD"
                        + "\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00BB\n"
                        + "\u00BA \u001B[32mWelcome to DOSBox v{0}\u001B[37m                                        \u00BA\n"
                        + "\u00BA                                                                    \u00BA\n"
                        +
                        // "\u00BA DOSBox runs real and protected mode games. \u00BA\n"+
                        "\u00BA For a short introduction for new users type: \u001B[33mINTRO\u001B[37m                 \u00BA\n"
                        + "\u00BA For supported shell commands type: \u001B[33mHELP\u001B[37m                            \u00BA\n"
                        + "\u00BA                                                                    \u00BA\n"
                        + "\u00BA To adjust the emulated CPU speed, use \u001B[31mctrl-F11\u001B[37m and \u001B[31mctrl-F12\u001B[37m.       \u00BA\n"
                        + "\u00BA To activate the keymapper \u001B[31mctrl-F1\u001B[37m.                                 \u00BA\n"
                        + "\u00BA For more information read the \u001B[36mREADME\u001B[37m file in the DOSBox directory. \u00BA\n"
                        + "\u00BA                                                                    \u00BA\n");
        Message.addMsg("SHELL_STARTUP_CGA",
                "\u00BA DOSBox supports Composite CGA mode.                                \u00BA\n"
                        + "\u00BA Use \u001B[31m(alt-)F11\u001B[37m to change the colours when in this mode.             \u00BA\n"
                        + "\u00BA                                                                    \u00BA\n");
        Message.addMsg("SHELL_STARTUP_HERC",
                "\u00BA Use \u001B[31mF11\u001B[37m to cycle through white, amber, and green monochrome color. \u00BA\n"
                        + "\u00BA                                                                    \u00BA\n");
        Message.addMsg("SHELL_STARTUP_DEBUG",
                "\u00BA Press \u001B[31malt-Pause\u001B[37m to enter the debugger or start the exe with \u001B[33mDEBUG\u001B[37m. \u00BA\n"
                        + "\u00BA                                                                    \u00BA\n");
        Message.addMsg("SHELL_STARTUP_END",
                "\u00BA \u001B[32mHAVE FUN!\u001B[37m                                                          \u00BA\n"
                        + "\u00BA \u001B[32mThe DOSBox Team \u001B[33mhttp://www.dosbox.com\u001B[37m                              \u00BA\n"
                        + "\u00C8\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD"
                        + "\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD"
                        + "\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00CD\u00BC\u001B[0m\n"
        // "\n" //Breaks the startup message if you type a mount and a drive change.
        );
        Message.addMsg("SHELL_CMD_CHDIR_HELP", "Displays/changes the current directory.\n");
        Message.addMsg("SHELL_CMD_CHDIR_HELP_LONG", "CHDIR [drive:][path]\n" + "CHDIR [..]\n"
                + "CD [drive:][path]\n" + "CD [..]\n\n"
                + "  ..   Specifies that you want to change to the parent directory.\n\n"
                + "Type CD drive: to display the current directory in the specified drive.\n"
                + "Type CD without parameters to display the current drive and directory.\n");
        Message.addMsg("SHELL_CMD_CLS_HELP", "Clear screen.\n");
        Message.addMsg("SHELL_CMD_DIR_HELP", "Directory View.\n");
        Message.addMsg("SHELL_CMD_ECHO_HELP",
                "Display messages and enable/disable command echoing.\n");
        Message.addMsg("SHELL_CMD_EXIT_HELP", "Exit from the shell.\n");
        Message.addMsg("SHELL_CMD_HELP_HELP", "Show help.\n");
        Message.addMsg("SHELL_CMD_MKDIR_HELP", "Make Directory.\n");
        Message.addMsg("SHELL_CMD_MKDIR_HELP_LONG",
                "MKDIR [drive:][path]\n" + "MD [drive:][path]\n");
        Message.addMsg("SHELL_CMD_RMDIR_HELP", "Remove Directory.\n");
        Message.addMsg("SHELL_CMD_RMDIR_HELP_LONG",
                "RMDIR [drive:][path]\n" + "RD [drive:][path]\n");
        Message.addMsg("SHELL_CMD_SET_HELP", "Change environment variables.\n");
        Message.addMsg("SHELL_CMD_IF_HELP", "Performs conditional processing in batch programs.\n");
        Message.addMsg("SHELL_CMD_GOTO_HELP", "Jump to a labeled line in a batch script.\n");
        Message.addMsg("SHELL_CMD_SHIFT_HELP",
                "Leftshift commandline parameters in a batch script.\n");
        Message.addMsg("SHELL_CMD_TYPE_HELP", "Display the contents of a text-file.\n");
        Message.addMsg("SHELL_CMD_TYPE_HELP_LONG", "TYPE [drive:][path][filename]\n");
        Message.addMsg("SHELL_CMD_REM_HELP", "Add comments in a batch file.\n");
        Message.addMsg("SHELL_CMD_REM_HELP_LONG", "REM [comment]\n");
        Message.addMsg("SHELL_CMD_NO_WILD",
                "This is a simple version of the command, no wildcards allowed!\n");
        Message.addMsg("SHELL_CMD_RENAME_HELP", "Renames one or more files.\n");
        Message.addMsg("SHELL_CMD_RENAME_HELP_LONG", "RENAME [drive:][path]filename1 filename2.\n"
                + "REN [drive:][path]filename1 filename2.\n\n"
                + "Note that you can not specify a new drive or path for your destination file.\n");
        Message.addMsg("SHELL_CMD_DELETE_HELP", "Removes one or more files.\n");
        Message.addMsg("SHELL_CMD_COPY_HELP", "Copy files.\n");
        Message.addMsg("SHELL_CMD_CALL_HELP",
                "Start a batch file from within another batch file.\n");
        Message.addMsg("SHELL_CMD_SUBST_HELP", "Assign an internal directory to a drive.\n");
        Message.addMsg("SHELL_CMD_LOADHIGH_HELP",
                "Loads a program into upper memory (requires xms=true,umb=true).\n");
        Message.addMsg("SHELL_CMD_CHOICE_HELP", "Waits for a keypress and sets ERRORLEVEL.\n");
        Message.addMsg("SHELL_CMD_CHOICE_HELP_LONG",
                "CHOICE [/C:choices] [/N] [/S] text\n"
                        + "  /C[:]choices  -  Specifies allowable keys.  Default is: yn.\n"
                        + "  /N  -  Do not display the choices at end of prompt.\n"
                        + "  /S  -  Enables case-sensitive choices to be selected.\n"
                        + "  text  -  The text to display as a prompt.\n");
        Message.addMsg("SHELL_CMD_ATTRIB_HELP", "Does nothing. Provided for compatibility.\n");
        Message.addMsg("SHELL_CMD_PATH_HELP", "Provided for compatibility.\n");
        Message.addMsg("SHELL_CMD_VER_HELP", "View and set the reported DOS version.\n");
        Message.addMsg("SHELL_CMD_VER_VER",
                "DOSBox version {0}. Reported DOS version {1:D}.{2:D2}.\n");

        /* Regular startup */
        _callShellStop = Callback.allocate();
        /* Setup the startup CS:IP to kill the last running machine when exitted */
        int newcsip = Callback.realPointer(_callShellStop);
        Register.segSet16(Register.SEG_NAME_CS, Memory.realSeg(newcsip));
        Register.setRegIP(Memory.realOff(newcsip));

        Callback.setup(_callShellStop, DOSShell::shellStopHandler, Callback.Symbol.IRET,
                "shell stop");
        Programs.makeFile("COMMAND.COM", DOSShell::makeProgram);

        /* Now call up the shell for the first time */
        int pspSeg = DOSMain.DOS_FIRST_SHELL;
        int env_seg = DOSMain.DOS_FIRST_SHELL + 19; // DOS_GetMemory(1+(4096/16))+1;
        int stack_seg = DOSMain.getMemory(2048 / 16);
        Register.segSet16(Register.SEG_NAME_SS, stack_seg);
        Register.setRegSP(2046);

        /* Set up int 24 and psp (Telarium games) */
        Memory.realWriteB((pspSeg + 16 + 1), 0, 0xea); /* far jmp */
        Memory.realWriteD((pspSeg + 16 + 1), 1, Memory.realReadD(0, 0x24 * 4));
        Memory.realWriteD(0, 0x24 * 4, (pspSeg << 16) | ((16 + 1) << 4));

        /* Set up int 23 to "int 20" in the psp. Fixes what.exe */
        Memory.realWriteD(0, 0x23 * 4, (pspSeg << 16));

        /* Setup MCBs */
        DOSMCB pspmcb = new DOSMCB(pspSeg - 1);
        pspmcb.setPSPSeg(pspSeg); // MCB of the command shell psp
        pspmcb.setSize(0x10 + 2);
        pspmcb.setType(0x4d);
        DOSMCB envmcb = new DOSMCB(env_seg - 1);
        envmcb.setPSPSeg(pspSeg); // MCB of the command shell environment
        envmcb.setSize((DOSMain.DOS_MEM_START - env_seg));
        envmcb.setType(0x4d);

        /* Setup environment */
        int envWrite = Memory.physMake(env_seg, 0);
        Memory.blockWrite(envWrite, pathString, 0, pathString.length);
        envWrite += pathString.length;
        Memory.blockWrite(envWrite, comspecString, 0, comspecString.length);
        envWrite += comspecString.length;
        Memory.writeB(envWrite++, 0);
        Memory.writeW(envWrite, 1);
        envWrite += 2;
        Memory.blockWrite(envWrite, fullName, 0, fullName.length);

        DOSPSP psp = new DOSPSP(pspSeg);
        psp.makeNew(0);
        DOSMain.DOS.setPSP(pspSeg);

        /*
         * The start of the filetable in the psp must look like this: 01 01 01 00 02 In order to
         * achieve this: First open 2 files. Close the first and duplicate the second (so the
         * entries get 01)
         */
        DOSMain.openFile("CON", DOSSystem.OPEN_READWRITE); /* STDIN */
        DOSMain.openFile("CON", DOSSystem.OPEN_READWRITE); /* STDOUT */
        DOSMain.closeFile(0); /* Close STDIN */
        DOSMain.forceDuplicateEntry(1, 0); /* "new" STDIN */
        DOSMain.forceDuplicateEntry(1, 2); /* STDERR */
        DOSMain.openFile("CON", DOSSystem.OPEN_READWRITE); /* STDAUX */
        DOSMain.openFile("CON", DOSSystem.OPEN_READWRITE); /* STDPRN */

        psp.setParent(pspSeg);
        /* Set the environment */
        psp.setEnvironment(env_seg);
        /* Set the command line for the shell start up */
        byte[] tail = new byte[DOSMain.CommandTailSize];
        tail[DOSMain.CommandTailOffCount] = (byte) (initLine.length - 1);// null character제외
        ArrayHelper.copy(initLine, 0, tail, DOSMain.CommandTailOffBuffer, initLine.length);
        Memory.blockWrite(Memory.physMake(pspSeg, 128), tail, 0, 128);

        /* Setup internal DOS Variables */
        DOSMain.DOS.setDTA(Memory.realMake(pspSeg, 0x80));
        DOSMain.DOS.setPSP(pspSeg);

        _firstShell = makeProgram();
        _firstShell.run();
        // first_shell.dispose();
        // first_shell = null;//Make clear that it shouldn't be used anymore
    }

    /*--------------------------- end DOSShellPartial -----------------------------*/

}
