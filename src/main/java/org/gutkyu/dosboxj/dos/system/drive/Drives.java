package org.gutkyu.dosboxj.dos.system.drive;



import org.gutkyu.dosboxj.misc.setup.*;
import org.gutkyu.dosboxj.util.*;

public final class Drives {
    public static boolean compareWildFile(String file, String wild) {
        String fileName = "", fileExt = "", wildName = "", wildExt = "";
        int extIdx = -1, r;

        extIdx = file.lastIndexOf('.');

        if (extIdx >= 0) {
            int size = extIdx;
            if (size > 8)
                size = 8;
            fileName = file.substring(0, size);
            extIdx++;
            int len = file.length() - extIdx;
            fileExt = file.substring(extIdx, extIdx + ((len > 3) ? 3 : len));
        } else {
            int len = file.length();
            fileName = file.substring(0, (len > 8) ? 8 : len);
        }

        fileName = fileName.toUpperCase();
        fileExt = fileExt.toUpperCase();

        extIdx = wild.lastIndexOf('.');
        if (extIdx >= 0) {
            int size = extIdx;
            if (size > 8)
                size = 8;
            wildName = wild.substring(0, size);
            extIdx++;
            int len = wild.length() - extIdx;
            wildExt = wild.substring(extIdx, extIdx + ((len > 3) ? 3 : len));
        } else {
            int len = wild.length();
            wildName = wild.substring(0, (len > 8) ? 8 : len);
        }

        wildName = wildName.toUpperCase();
        wildExt = wildExt.toUpperCase();

        fileName = StringHelper.padRight(fileName, 8);
        fileExt = StringHelper.padRight(fileExt, 3);
        wildName = StringHelper.padRight(wildName, 8);
        wildExt = StringHelper.padRight(wildExt, 3);

        /* Names are right do some checking */
        r = 0;
        while (r < 8) {
            if (wildName.charAt(r) == '*') {
                break;// goto checkext;
            }
            if (wildName.charAt(r) != '?' && wildName.charAt(r) != fileName.charAt(r))
                return false;
            r++;
        }
        // checkext:
        r = 0;
        while (r < 3) {
            if (wildExt.charAt(r) == '*')
                return true;
            if (wildExt.charAt(r) != '?' && wildExt.charAt(r) != fileExt.charAt(r))
                return false;
            r++;
        }
        return true;
    }

    public static void setLabel(String input, CStringPt output, boolean cdrom) {
        int togo = 8;
        int vnamePos = 0;
        int labelPos = 0;
        boolean point = false;

        // spacepadding the filenamepart to include spaces after the terminating zero is more
        // closely to the specs. (not doing this now)
        // HELLO\0' '' '

        while (togo > 0) {
            if (vnamePos == input.length())
                break;
            if (!point && (input.charAt(vnamePos) == '.')) {
                togo = 4;
                point = true;
            }

            // another mscdex quirk. Label is not always uppercase. (Daggerfall)
            output.set(labelPos, (cdrom ? input.charAt(vnamePos)
                    : Character.toUpperCase(input.charAt(vnamePos))));

            labelPos++;
            vnamePos++;
            togo--;
            if ((togo == 0) && !point) {
                if (input.charAt(vnamePos) == '.')
                    vnamePos++;
                output.set(labelPos, '.');
                labelPos++;
                point = true;
                togo = 3;
            }
        }
        output.set(labelPos, (char) 0);

        // Remove trailing dot. except when on cdrom and filename is exactly 8 (9 including the dot)
        // letters. MSCDEX feature/bug (fifa96 cdrom detection)
        if ((labelPos > 0) && (output.get(labelPos - 1) == (byte) '.') && !(cdrom && labelPos == 9))
            output.set(labelPos - 1, (char) 0);
    }

    public static void initDrives(Section sec) {
        DriveManager.init(sec);
    }
}
