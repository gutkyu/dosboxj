package org.gutkyu.dosboxj.shell;

import org.gutkyu.dosboxj.dos.*;
import org.gutkyu.dosboxj.misc.*;
import org.gutkyu.dosboxj.misc.setup.*;
import org.gutkyu.dosboxj.util.*;

class BatchFile implements Disposable {
    public BatchFile(DOSShell host, String resolvedName, String enteredName, String cmdLine) {
        location = 0;
        prev = host.BatFile;
        echo = host.Echo;
        shell = host;
        CStringPt totalname = CStringPt.create(DOSSystem.DOS_PATHLENGTH + 4);
        // Get fullname including drive specificiation
        DOSMain.canonicalize(resolvedName, totalname);
        cmd = new CommandLine(enteredName, cmdLine);
        filename = totalname.toString();

        // Test if file is openable
        if (!DOSMain.openFile(filename, 128)) {
            // TODO Come up with something better
            Support.exceptionExit("SHELL:Can't open BatchFile %s", filename);
        }
        fileHandle = DOSMain.returnFileHandle;
        DOSMain.closeFile(fileHandle);
    }

    public void dispose() {
        dispose(true);
    }

    protected void dispose(boolean disposing) {

        cmd = null;
        shell.BatFile = prev;
        shell.Echo = echo;
    }

    public boolean readLine(CStringPt line) {
        // Open the batchfile and seek to stored postion
        if (!DOSMain.openFile(filename, 128)) {
            Log.logging(Log.LogTypes.MISC, Log.LogServerities.Error,
                    "ReadLine Can't open BatchFile %s", filename);
            dispose();
            return false;
        }
        fileHandle = DOSMain.returnFileHandle;
        long loc = DOSMain.seekFile(fileHandle, this.location, DOSSystem.DOS_SEEK_SET);
        if (loc >= 0)
            this.location = (int) loc;
        int c = 0;// uint8
        int n = 0;
        CStringPt temp = CStringPt.create(ShellInner.CMD_MAXLINE);
        CStringPt cmdWrite = CStringPt.clone(temp);
        // emptyline:
        while (true) {
            do {
                n = 1;
                DOSMain.readFile(fileHandle);
                c = 0xff & DOSMain.ReadByte;
                n = DOSMain.readSize();
                if (n > 0) {
                    /*
                     * Why are we filtering this ? Exclusion list: tab for batch files escape for
                     * ansi backspace for alien odyssey
                     */
                    if (c > 31 || c == 0x1b || c == '\t' || c == 8) {
                        cmdWrite.set((char) c);
                        cmdWrite.movePtToR1();
                    }
                }
            } while (c != '\n' && n != 0);
            cmdWrite.set((char) 0);
            if (n == 0 && cmdWrite.equals(temp)) {
                // Close file and delete bat file
                DOSMain.closeFile(fileHandle);
                dispose();
                return false;
            }
            if (temp.length() == 0)
                continue;// goto emptyline;
            if (temp.get(0) == ':')
                continue;// goto emptyline;
            break;
        }
        /* Now parse the line read from the bat file for % stuff */
        cmdWrite = CStringPt.clone(line);
        CStringPt cmdRead = CStringPt.clone(temp);
        CStringPt envName = CStringPt.create(256);
        CStringPt envWrite;
        while (cmdRead.get() != 0) {
            envWrite = CStringPt.clone(envName);
            if (cmdRead.get() == '%') {
                cmdRead.movePtToR1();
                if (cmdRead.get(0) == '%') {
                    cmdRead.movePtToR1();
                    cmdWrite.set('%');
                    cmdWrite.movePtToR1();
                    continue;
                }
                if (cmdRead.get(0) == '0') { /* Handle %0 */
                    CStringPt fileName = CStringPt.create(cmd.getFileName());
                    cmdRead.movePtToR1();
                    CStringPt.copy(fileName, cmdWrite);
                    cmdWrite = CStringPt.clone(cmdWrite, fileName.length());
                    continue;
                }
                char next = cmdRead.get(0);
                if (next > '0' && next <= '9') {
                    /* Handle %1 %2 .. %9 */
                    cmdRead.movePtToR1(); // Progress reader
                    next -= '0';
                    if (cmd.getCount() < (int) next)
                        continue;
                    String word = null;
                    if (!cmd.findCommand(next))
                        continue;
                    word = cmd.returnedCmd;
                    CStringPt.copy(word, cmdWrite);
                    cmdWrite.moveR(word.length());
                    continue;
                } else {
                    /* Not a command line number has to be an environment */
                    CStringPt first = cmdRead.positionOf('%');
                    /*
                     * No env afterall.Somewhat of a hack though as %% and % aren't handled
                     * consistent in dosbox. Maybe echo needs to parse % and %% as well.
                     */
                    if (!first.isEmpty()) {
                        cmdWrite.set('%');
                        cmdWrite.movePtToR1();
                        continue;
                    }
                    first.set((char) 0);
                    first.movePtToR1();
                    String env = null;
                    if ((env = shell.getEnvStr(cmdRead.toString())) != null) {
                        CStringPt equals = CStringPt.create(env).positionOf('=');
                        if (!equals.isEmpty())
                            continue;
                        equals.movePtToR1();
                        CStringPt.copy(equals, cmdWrite);
                        cmdWrite.moveR(equals.length());
                    }
                    CStringPt.copyPt(first, cmdRead);
                }
            } else {
                cmdWrite.set(cmdRead.get());
                cmdWrite.movePtToR1();
                cmdRead.movePtToR1();
            }
        }
        cmdWrite.set((char) 0);
        // Store current location and close bat file
        this.location = 0;
        loc = DOSMain.seekFile(fileHandle, this.location, DOSSystem.DOS_SEEK_CUR);
        if (loc >= 0)
            this.location = (int) loc;
        DOSMain.closeFile(fileHandle);
        return true;
    }

    public boolean gotoLabel(CStringPt where) {
        // Open bat file and search for the where String
        if (!DOSMain.openFile(filename, 128)) {
            Log.logging(Log.LogTypes.MISC, Log.LogServerities.Error,
                    "SHELL:Goto Can't open BatchFile %s", filename);
            this.dispose();
            return false;
        }
        fileHandle = DOSMain.returnFileHandle;

        CStringPt cmdBuffer = CStringPt.create(ShellInner.CMD_MAXLINE);
        CStringPt cmdWrite;

        /* Scan till we have a match or return false */
        byte c = 0;
        int n = 0;

        // again:
        while (true) {
            cmdWrite = CStringPt.clone(cmdBuffer);
            do {
                n = 1;
                DOSMain.readFile(fileHandle);
                c = DOSMain.ReadByte;
                n = DOSMain.readSize();
                if (n > 0) {
                    if (c > 31)
                        cmdWrite.set((char) c);
                    cmdWrite.movePtToR1();
                }
            } while (c != '\n' && n != 0);

            cmdWrite.set((char) 0);
            cmdWrite.movePtToR1();
            CStringPt nospace = CStringPt.clone(cmdBuffer.trim());
            if (nospace.get(0) == ':') {
                nospace.movePtToR1(); // Skip :
                // Strip spaces and = from it.
                while (nospace.get() != 0
                        && (Character.isWhitespace(nospace.get()) || (nospace.get() == '=')))
                    nospace.movePtToR1();

                // label is until space/=/eol
                CStringPt beginlabel = CStringPt.clone(nospace);
                while (nospace.get() != 0 && !Character.isWhitespace(nospace.get())
                        && (nospace.get() != '='))
                    nospace.movePtToR1();

                nospace.set((char) 0);
                if (beginlabel.equalsIgnoreCase(where)) {
                    // Found it! Store location and continue
                    this.location = 0;
                    long loc = DOSMain.seekFile(fileHandle, this.location, DOSSystem.DOS_SEEK_CUR);
                    if (loc >= 0)
                        this.location = (int) loc;
                    DOSMain.closeFile(fileHandle);
                    return true;
                }

            }
            if (n == 0) {
                DOSMain.closeFile(fileHandle);
                this.dispose();
                return false;
            }
            // goto again;
        }
        // return false;
    }

    public void shift() {
        cmd.shift(1);
    }

    public int fileHandle;// uint16
    public int location;
    public boolean echo;
    public DOSShell shell;
    public BatchFile prev;
    public CommandLine cmd;
    public String filename;
}
