package org.gutkyu.dosboxj.dos.keyboardlayout;

import org.gutkyu.dosboxj.dos.*;
import org.gutkyu.dosboxj.interrupt.int10.*;
import org.gutkyu.dosboxj.hardware.video.*;
import org.gutkyu.dosboxj.misc.setup.*;
import org.gutkyu.dosboxj.misc.setup.Value.WrongType;
import org.gutkyu.dosboxj.util.*;

public final class DOSKeyboardLayoutModule extends ModuleBase {
    public DOSKeyboardLayoutModule(Section configuration) throws WrongType {
        super(configuration);
        SectionProperty section = (SectionProperty) configuration;
        DOSMain.DOS.LoadedCodepage = 437; // US codepage already initialized
        DOSMain.LoadedLayout = new KeyboardLayout();

        String layoutname = section.getString("keyboardlayout");

        int wants_dos_codepage = -1;
        if (layoutname.startsWith("auto")) {
            // TODO : WIN32 환경에서 작동하는 코드
        }

        boolean extract_codepage = true;
        if (wants_dos_codepage > 0) {
            if ((DOSMain.LoadedLayout.readCodePageFile("auto",
                    wants_dos_codepage)) == DOSMain.KEYB_NOERROR) {
                // preselected codepage was successfully loaded
                extract_codepage = false;
            }
        }
        if (extract_codepage) {
            // try to find a good codepage for the requested layout
            int req_codepage = DOSMain.LoadedLayout.extractCodePage(layoutname);
            DOSMain.LoadedLayout.readCodePageFile("auto", (int) req_codepage);
        }

        /*
         * if (strncmp(layoutname,"auto",4) && strncmp(layoutname,"none",4)) {
         * Log.LOG_MSG("Loading DOS keyboard layout %s ...",layoutname); }
         */
        if (DOSMain.LoadedLayout.ReadKeyboardFile(layoutname,
                (int) DOSMain.DOS.LoadedCodepage) != 0) {
            if (layoutname.startsWith("auto")) {
                Log.logMsg("Error loading keyboard layout %s", layoutname);
            }
        } else {
            String lcode = DOSMain.LoadedLayout.mainLanguageCode();
            if (lcode != null) {
                Log.logMsg("DOS keyboard layout loaded with main language code %s for layout %s",
                        lcode, layoutname);
            }
        }
    }

    @Override
    protected void dispose(boolean disposing) {
        if (disposing) {

        }

        if ((DOSMain.DOS.LoadedCodepage != 437) && (INT10Mode.CurMode.Type == VGAModes.TEXT)) {
            INT10.reloadRomFonts();
            DOSMain.DOS.LoadedCodepage = 437; // US codepage
        }
        if (DOSMain.LoadedLayout != null) {
            DOSMain.LoadedLayout.dispose();
            DOSMain.LoadedLayout = null;
        }

        super.dispose(disposing);
    }

    static DOSKeyboardLayoutModule _doskbd;

    private static void shutdown(Section sec) {
        _doskbd.dispose();
        _doskbd = null;
    }

    public static void init(Section sec) throws WrongType {
        _doskbd = new DOSKeyboardLayoutModule(sec);
        sec.addDestroyFunction(DOSKeyboardLayoutModule::shutdown, true);
        // MAPPER_AddHandler(switch_keyboard_layout,MK_f2,MMOD1|MMOD2,"sw_layout","Switch Layout");
    }
}
