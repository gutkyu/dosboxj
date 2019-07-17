package org.gutkyu.dosboxj.shell;

import java.util.LinkedList;

final class ShellInner {
    protected static final int CMD_MAXLINE = 4096;
    protected static final int CMD_MAXCMDS = 20;
    protected static final int CMD_OLDSIZE = 4096;

    protected static final int AUTOEXEC_SIZE = 4096;
    protected static byte[] autoexec_data = {(byte) '\0'};
    protected static LinkedList<String> autoexecStrings = new LinkedList<String>();
}
