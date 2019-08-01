package org.gutkyu.dosboxj.dos.software;

import org.gutkyu.dosboxj.misc.setup.*;
import org.gutkyu.dosboxj.misc.setup.Value.WrongType;
import org.gutkyu.dosboxj.shell.*;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.*;
import org.gutkyu.dosboxj.DOSBox;
import org.gutkyu.dosboxj.misc.*;

class Config extends Program {
    public static Program makeProgram() {
        return new Config();
    }

    @Override
    public void run() throws WrongType {
        if (cmd.findString("-writeconf", true) || cmd.findString("-wc", true)) {
            tempLine = cmd.returnedString;
            /* In secure mode don't allow a new configfile to be created */
            if (DOSBox.Control.getSecureMode()) {
                writeOut(Message.get("PROGRAM_CONFIG_SECURE_DISALLOW"));
                return;
            }
            try {
                try (SeekableByteChannel bf =
                        Files.newByteChannel(Paths.get(tempLine), StandardOpenOption.CREATE,
                                StandardOpenOption.READ, StandardOpenOption.WRITE)) {
                    DOSBox.Control.printConfig(tempLine);
                    return;
                }

            } catch (Exception e) {
                writeOut(Message.get("PROGRAM_CONFIG_FILE_ERROR"), tempLine);
                return;
            }

        }
        if (cmd.findString("-writelang", true) || cmd.findString("-wl", true)) {
            tempLine = cmd.returnedString;

            /*
             * In secure mode don't allow a new languagefile to be created Who knows which kind of
             * file we would overwriting.
             */
            if (DOSBox.Control.getSecureMode()) {
                writeOut(Message.get("PROGRAM_CONFIG_SECURE_DISALLOW"));
                return;
            }
            try {
                try (SeekableByteChannel bf =
                        Files.newByteChannel(Paths.get(tempLine), StandardOpenOption.CREATE,
                                StandardOpenOption.READ, StandardOpenOption.WRITE)) {
                    Message.write(tempLine);
                    return;
                }

            } catch (Exception e) {
                writeOut(Message.get("PROGRAM_CONFIG_FILE_ERROR"), tempLine);
                return;
            }
        }

        /* Code for switching to secure mode */
        if (cmd.findExist("-securemode", true)) {
            DOSBox.Control.switchToSecureMode();
            writeOut(Message.get("PROGRAM_CONFIG_SECURE_ON"));
            return;
        }

        /*
         * Code for getting the current configuration. * Official format: config -get
         * "section property" * As a bonus it will set %CONFIG% to this value as well
         */
        if (cmd.findString("-get", true)) {
            tempLine = cmd.returnedCmd;
            String temp2 = "";
            temp2 = cmd.getStringRemain(temp2);// So -get n1 n2= can be used without quotes
            temp2 = temp2 == null ? "" : temp2;
            if (temp2 != "")
                tempLine = tempLine + " " + temp2;

            int space = tempLine.indexOf(" ");
            if (space < 0) {
                writeOut(Message.get("PROGRAM_CONFIG_GET_SYNTAX"));
                return;
            }
            // Copy the found property to a new string and erase from templine (mind the space)
            String prop = tempLine.substring(space + 1);
            tempLine = tempLine.substring(0, space + 1);

            Section sec = DOSBox.Control.getSection(tempLine);
            if (sec == null) {
                writeOut(Message.get("PROGRAM_CONFIG_SECTION_ERROR"), tempLine);
                return;
            }
            String val = sec.getPropValue(prop);
            if (val.equals(SetupModule.NO_SUCH_PROPERTY)) {
                writeOut(Message.get("PROGRAM_CONFIG_NO_PROPERTY"), prop, tempLine);
                return;
            }
            writeOut("%s", val);
            DOSShell.firstShell().setEnv("CONFIG", val);
            return;
        }



        /*
         * Code for the configuration changes * Official format: config -set
         * "section property=value" * Accepted: without quotes and/or without -set and/or without
         * section * and/or the "=" replaced by a " "
         */

        if (cmd.findString("-set", true)) { // get all arguments
            tempLine = cmd.returnedString;
            String temp2 = "";
            temp2 = cmd.getStringRemain(temp2);// So -set n1 n2=n3 can be used without quotes
            temp2 = temp2 == null ? "" : temp2;
            if (temp2 != "")
                tempLine = tempLine + " " + temp2;
        } else if ((tempLine = cmd.getStringRemain(tempLine)) == null) {// no set
            writeOut(Message.get("PROGRAM_CONFIG_USAGE")); // and no arguments specified
            return;
        }
        // Wanted input: n1 n2=n3
        String copy = tempLine;
        // seperate section from property
        int tempIdx = copy.indexOf(' ');
        if ((tempIdx >= 0) || (tempIdx = copy.indexOf('=')) < 0)
            copy = copy.substring(0, tempIdx++ - 1);
        else {
            writeOut(Message.get("PROGRAM_CONFIG_USAGE"));
            return;
        }

        String inputLine = "";

        // if n1 n2 n3 then replace last space with =
        int signIdx = tempLine.indexOf('=', tempIdx);
        if (signIdx < 0) {
            signIdx = tempLine.indexOf(' ', tempIdx);
            if (signIdx >= 0) {
                copy = copy.substring(0, signIdx) + "=" + copy.substring(signIdx + 1);
                inputLine = tempLine.substring(tempIdx);
            } else {
                // 2 items specified (no space nor = between n2 and n3
                // assume that they posted: property value
                // Try to determine the section.
                Section sec = DOSBox.Control.getSectionFromProperty(copy);
                if (sec == null) {
                    if (DOSBox.Control.getSectionFromProperty(tempLine.substring(tempIdx)) != null)
                        return; // Weird situation:ignore
                    writeOut(Message.get("PROGRAM_CONFIG_PROPERTY_ERROR"), copy);
                    return;
                } // Hack to allow config ems true
                String buffer = copy + "=" + tempLine.substring(tempIdx);
                signIdx = buffer.indexOf(' ');
                if (signIdx < 0)
                    buffer = buffer.substring(0, signIdx) + "=" + buffer.substring(signIdx + 1);
                copy = sec.getName();
                inputLine = buffer;
            }
            inputLine = tempLine.substring(tempIdx);
        } else {
            inputLine = tempLine.substring(tempIdx);
        }

        /*
         * Input processed. Now the real job starts copy contains the likely "sectionname" temp
         * contains "property=value" the section is destroyed and a new input line is given to the
         * configuration parser. Then the section is restarted.
         */
        // char* inputline = const_cast<char*>(temp);
        Section sec1 = null;
        sec1 = DOSBox.Control.getSection(copy);
        if (sec1 == null) {
            writeOut(Message.get("PROGRAM_CONFIG_SECTION_ERROR"), copy);
            return;
        }
        sec1.executeDestroy(false);
        sec1.handleInputline(inputLine);
        sec1.executeInit(false);
        return;
    }


}
