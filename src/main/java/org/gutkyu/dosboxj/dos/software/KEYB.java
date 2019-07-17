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
        if ((TempLine = Cmd.findCommand(1)) != null) {
            if ((TempLine = Cmd.findString("?", false)) != null) {
                writeOut(Message.get("PROGRAM_KEYB_SHOWHELP"));
            } else {
                /* first parameter is layout ID */
                int keybError = 0;
                String cp_string = null;
                int tried_cp = -1;
                if ((cp_string = Cmd.findCommand(2)) != null) {
                    /* second parameter is codepage number */
                    tried_cp = Integer.parseInt(cp_string);
                    CStringPt cp_file_name = CStringPt.create(256);
                    if ((cp_string = Cmd.findCommand(3)) != null) {
                        /* third parameter is codepage file */
                        CStringPt.copy(cp_string, cp_file_name);
                    } else {
                        /* no codepage file specified, use automatic selection */
                        CStringPt.copy("auto", cp_file_name);
                    }

                    keybError =
                            DOSMain.loadKeyboardLayout(TempLine, tried_cp, cp_file_name.toString());
                } else {
                    RefU32Ret refTriedCP = new RefU32Ret(tried_cp);
                    keybError = DOSMain.switchKeyboardLayout(TempLine, refTriedCP);
                    tried_cp = refTriedCP.U32;
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
                        writeOut(Message.get("PROGRAM_KEYB_LAYOUTNOTFOUND"), TempLine, tried_cp);
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
