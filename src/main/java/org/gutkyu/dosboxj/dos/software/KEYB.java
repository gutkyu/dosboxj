package org.gutkyu.dosboxj.dos.software;



import org.gutkyu.dosboxj.dos.*;
import org.gutkyu.dosboxj.misc.*;
import org.gutkyu.dosboxj.util.*;

public final class KEYB extends Program {
    public static Program makeProgram() {
        return new KEYB();
    }

    @Override
    public void run() {
        if (Cmd.findCommand(1)) {
            TempLine = Cmd.returnedCmd;
            if (Cmd.findString("?", false)) {
                TempLine = Cmd.returnedString;
                writeOut(Message.get("PROGRAM_KEYB_SHOWHELP"));
            } else {
                /* first parameter is layout ID */
                int keybError = 0;
                String cpString = null;
                int triedCP = -1;
                if (Cmd.findCommand(2)) {
                    cpString = Cmd.returnedCmd;
                    /* second parameter is codepage number */
                    triedCP = Integer.parseInt(cpString);
                    CStringPt cpFileName = CStringPt.create(256);
                    if (Cmd.findCommand(3)) {
                        cpString = Cmd.returnedCmd;
                        /* third parameter is codepage file */
                        CStringPt.copy(cpString, cpFileName);
                    } else {
                        /* no codepage file specified, use automatic selection */
                        CStringPt.copy("auto", cpFileName);
                    }

                    keybError =
                            DOSMain.loadKeyboardLayout(TempLine, triedCP, cpFileName.toString());
                } else {
                    keybError = DOSMain.trySwitchKeyboardLayout(TempLine);
                    triedCP = DOSMain.returnedSwitchKBLTryiedCP;
                }
                switch (keybError) {
                    case DOSMain.KEYB_NOERROR:
                        writeOut(Message.get("PROGRAM_KEYB_NOERROR"), TempLine,
                                DOSMain.DOS.LoadedCodepage);
                        break;
                    case DOSMain.KEYB_FILENOTFOUND:
                        writeOut(Message.get("PROGRAM_KEYB_FILENOTFOUND"), TempLine);
                        writeOut(Message.get("PROGRAM_KEYB_SHOWHELP"));
                        break;
                    case DOSMain.KEYB_INVALIDFILE:
                        writeOut(Message.get("PROGRAM_KEYB_INVALIDFILE"), TempLine);
                        break;
                    case DOSMain.KEYB_LAYOUTNOTFOUND:
                        writeOut(Message.get("PROGRAM_KEYB_LAYOUTNOTFOUND"), TempLine, triedCP);
                        break;
                    case DOSMain.KEYB_INVALIDCPFILE:
                        writeOut(Message.get("PROGRAM_KEYB_INVCPFILE"), TempLine);
                        writeOut(Message.get("PROGRAM_KEYB_SHOWHELP"));
                        break;
                    default:
                        Log.logging(Log.LogTypes.DOSMISC, Log.LogServerities.Error,
                                "KEYB:Invalid returncode %x", keybError);
                        break;
                }
            }
        } else {
            /*
             * no parameter in the command line, just output codepage info and possibly loaded
             * layout ID
             */
            String layoutName = DOSMain.getLoadedLayout();
            if (layoutName == null) {
                writeOut(Message.get("PROGRAM_KEYB_INFO"), DOSMain.DOS.LoadedCodepage);
            } else {
                writeOut(Message.get("PROGRAM_KEYB_INFO_LAYOUT"), DOSMain.DOS.LoadedCodepage,
                        layoutName);
            }
        }
    }
}
