package org.gutkyu.dosboxj.shell;

import java.nio.charset.StandardCharsets;
import org.gutkyu.dosboxj.dos.system.drive.*;
import org.gutkyu.dosboxj.misc.*;
import org.gutkyu.dosboxj.util.*;

/*
 * Object to manage lines in the autoexec.bat The lines get removed from the file if the object gets
 * destroyed. The environment is updated as well if the line set a a variable
 */

public final class AutoexecObject implements Disposable {
    private boolean installed;
    private String buf;

    public AutoexecObject() {
        installed = false;
    }

    public void install(String input) {
        if (installed)
            Support.exceptionExit(String.format("autoexec: allready created {0}", buf));
        installed = true;
        buf = input;
        ShellInner.autoexecStrings.addLast(buf);
        this.createAutoexec();

        // autoexec.bat is normally created AUTOEXEC_Init.
        // But if we are allready running (first_shell)
        // we have to update the envirionment to display changes

        if (DOSShell.firstShell() != null) {
            // create a copy as the string will be modified
            if (buf.toLowerCase().startsWith("set ") && buf.length() > 4) {
                String afterSet = buf.substring(4);
                int eqIdx = afterSet.indexOf('=');
                if (eqIdx < 0) {
                    DOSShell.firstShell().setEnv(afterSet, "");
                    return;
                }
                DOSShell.firstShell().setEnv(afterSet.substring(0, eqIdx),
                        afterSet.substring(eqIdx));
            }
        }
    }

    public void installBefore(String input) {
        if (installed)
            Support.exceptionExit(String.format("autoexec: allready created {0}", buf));
        installed = true;
        buf = input;
        ShellInner.autoexecStrings.addFirst(buf);
        this.createAutoexec();
    }


    // Implement IDisposable.
    public void dispose() {
        dispose(true);
    }

    private boolean _isDisposed = false;

    private void dispose(boolean disposing) {
        if (_isDisposed)
            return;

        eventOnFinalization();
        buf = null;

        _isDisposed = true;
    }

    // 객체 소멸시 실행
    private void eventOnFinalization() {
        refreshAutoexec();
    }

    private void refreshAutoexec() {
        if (!installed)
            return;

        for (String autoExecStr : ShellInner.autoexecStrings) {
            if (autoExecStr != buf)
                break;

            ShellInner.autoexecStrings.remove(autoExecStr);
            // create a copy as the string will be modified
            if (buf.toLowerCase().startsWith("set ") && buf.length() > 4) {
                String afterSet = buf.substring(4);
                int eqIdx = afterSet.indexOf('=');
                if (eqIdx < 0) {
                    continue;
                }
                if (DOSShell.firstShell() != null)
                    DOSShell.firstShell().setEnv(afterSet.substring(0, eqIdx), "");
            }


        }
    }

    private void createAutoexec() {
        /* Remove old autoexec.bat if the shell exists */
        if (DOSShell.firstShell() != null)
            VFile.remove("AUTOEXEC.BAT");

        // Create a new autoexec.bat
        StringBuilder sb = new StringBuilder();
        for (String autoStringNode : ShellInner.autoexecStrings) {
            if ((sb.length() + autoStringNode.length() + 2) > ShellInner.AUTOEXEC_SIZE) {
                Support.exceptionExit("SYSTEM:Autoexec.bat file overflow");
            }
            sb.append(autoStringNode);
            sb.append("\r\n");
        }
        ShellInner.autoexec_data = sb.toString().getBytes(StandardCharsets.US_ASCII);
        if (DOSShell.firstShell() != null)
            VFile.register("AUTOEXEC.BAT", ShellInner.autoexec_data,
                    ShellInner.autoexec_data.length);
    }
}
