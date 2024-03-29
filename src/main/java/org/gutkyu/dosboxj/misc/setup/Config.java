package org.gutkyu.dosboxj.misc.setup;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.LinkedList;
import java.util.List;
import org.gutkyu.dosboxj.*;
import org.gutkyu.dosboxj.misc.*;
import org.gutkyu.dosboxj.util.*;

public final class Config {
    private String currentConfigDir = null;

    public CommandLine CmdLine;
    private LinkedList<Section> SectionList = new LinkedList<Section>();

    private DOSAction startFunction;
    private boolean secureMode; // Sandbox mode

    private DOSAction1<Section> initFunction;

    public Config(CommandLine cmd) {
        CmdLine = cmd;
        secureMode = false;
    }

    public SectionLine addSectionLine(String name, DOSAction1<Section> initFunction) {
        this.initFunction = initFunction;
        SectionLine blah = new SectionLine(name);
        blah.addInitFunction(initFunction);
        SectionList.addLast(blah);
        return blah;
    }

    public SectionProperty addSectionProp(String name, DOSAction1<Section> initFunction,
            boolean canChange) {
        SectionProperty blah = new SectionProperty(name);
        blah.addInitFunction(initFunction, canChange);
        SectionList.addLast(blah);
        return blah;
    }

    public SectionProperty addSectionProp(String name, DOSAction1<Section> initFunction) {
        return addSectionProp(name, initFunction, false);
    }

    public Section getSection(int index) {
        for (Section sec : SectionList) {
            if (index-- == 0)
                return sec;
        }
        return null;
    }

    public Section getSection(String sectionName) {
        for (Section sec : SectionList) {
            if (sec.getName().equalsIgnoreCase(sectionName))
                return sec;
        }
        return null;
    }

    public Section getSectionFromProperty(String prop) {
        for (Section sec : SectionList) {
            if (sec.getPropValue(prop) != SetupModule.NO_SUCH_PROPERTY)
                return sec;
        }
        return null;
    }

    public void setStartUp(DOSAction fn) {
        startFunction = fn;
    }

    public void init() {
        for (Section sec : SectionList) {
            sec.executeInit();
        }
    }

    public void shutdown() {
        for (Section sec : SectionList) {
            sec.executeDestroy();
        }
    }// 원 소스에는 구현이 없다

    public void startup() {
        startFunction.run();
    }

    public boolean printConfig(String configFileName) {
        String temp = null;
        try {
            try (BufferedWriter fileWriter = Files.newBufferedWriter(Paths.get(configFileName),
                    StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE)) {
                /* Print start of configfile and add an return to improve readibility. */

                fileWriter.write(String.format(Message.get("CONFIGFILE_INTRO"), DOSBox.VERSION));
                fileWriter.write("\n");
                for (Section sec : SectionList) {
                    /* Print out the Section header */
                    temp = sec.getName().toLowerCase();

                    fileWriter.write(String.format("[%1$s]\n", temp));

                    if (sec instanceof SectionProperty) {
                        SectionProperty secProp = (SectionProperty) sec;
                        Property p;
                        int i = 0, maxwidth = 0;
                        while ((p = secProp.getProp(i++)) != null) {
                            int w = p.PropName.length();
                            if (w > maxwidth)
                                maxwidth = w;
                        }
                        i = 0;
                        String prefix = "";
                        // prefix = string.Format("\n# %*s ", maxwidth, "");
                        prefix = String.format("\n#   ");
                        while ((p = secProp.getProp(i++)) != null) {
                            String help = p.getHelp();
                            int pos = -1;
                            while ((pos = help.indexOf("\n", pos + 1)) >= 0) {
                                help = help.substring(0, pos) + prefix + help.substring(pos + 1);
                            }

                            // fileWriter.Write(string.Format("# %*s: %s", maxwidth, p.propname,
                            // help));
                            fileWriter.write(String.format("# %1$s: %2$s",
                                    maxwidth < p.PropName.length()
                                            ? p.PropName.substring(0, maxwidth)
                                            : p.PropName,
                                    help));

                            List<Value> values = p.getValues();
                            if (values.size() > 0) {
                                fileWriter.write(String.format("%1$s%2$s:", prefix,
                                        Message.get("CONFIG_SUGGESTED_VALUES")));
                                for (Value it : values) {
                                    // Hack hack hack. else we need to modify GetValues, but that
                                    // one is const...
                                    if (!it.toString().equals("%u")) {
                                        if (it.notEquals(values.get(0)))// if (it != values.First())
                                            fileWriter.write(",");
                                        fileWriter.write(String.format(" %1$s", it.toString()));
                                    }
                                }
                                fileWriter.write(".");
                            }
                            fileWriter.write("\n");
                        }
                    } else {
                        temp = temp.toUpperCase() + "_CONFIGFILE_HELP";
                        String helpstr = Message.get(temp);
                        int helpstrIdx = 0;
                        // StringBuilder helpwrite = new StringBuilder();
                        StringBuffer helpwrite = new StringBuffer();
                        while (helpstrIdx < helpstr.length()) {
                            // helpwrite.Append(helpstr[helpstrIdx]);
                            helpwrite.append(helpstr.charAt(helpstrIdx));
                            if (helpstr.charAt(helpstrIdx) == '\n') {
                                // fileWriter.Write(string.Format("# {0}", helpwrite.toString()));
                                fileWriter.write(String.format("# %1$s", helpwrite.toString()));
                                helpwrite.setLength(0);// clear
                            }
                            helpstrIdx++;
                        }
                    }

                    fileWriter.write("\n");
                    sec.printData(fileWriter);
                    fileWriter.write("\n"); /* Always an empty line between sections */
                }
                fileWriter.flush();
            }
            return true;

        } catch (Exception e) {
            return false;
        }

    }

    private static boolean firstConfigFile = true;

    public boolean parseConfigFile(String configFileName) {
        // static boolean first_configfile = true;
        try {
            try (BufferedReader fileReader = Files.newBufferedReader(Paths.get(configFileName))) {
                String settingsType = firstConfigFile ? "primary" : "additional";
                firstConfigFile = false;
                Log.logMsg("CONFIG:Loading %s settings from config file %s", settingsType,
                        configFileName);

                // Get directory from configfilename, used with relative paths.
                currentConfigDir = configFileName;
                int pos = currentConfigDir.lastIndexOf(Cross.FILESPLIT);
                if (pos < 0)
                    pos = 0; // No directory then erase string
                currentConfigDir = currentConfigDir.substring(0, pos);
                // current_config_dir.erase(pos);

                String gegevens;
                Section currentSection = null;
                Section testSec = null;
                while ((gegevens = fileReader.readLine()) != null) {

                    /* strip leading/trailing whitespace */
                    gegevens = gegevens.trim();
                    if (gegevens.length() == 0)
                        continue;

                    switch (gegevens.charAt(0)) {
                        case '%':
                        case '\0':
                        case '#':
                        case ' ':
                        case '\n':
                            continue;
                        case '[': {
                            int loc = gegevens.indexOf(']');
                            if (loc < 0)
                                continue;
                            gegevens = gegevens.substring(1, loc);
                            testSec = getSection(gegevens);
                            if (testSec != null)
                                currentSection = testSec;
                            testSec = null;
                            break;
                        }
                        default:
                            try {
                                if (currentSection != null)
                                    currentSection.handleInputline(gegevens);
                            } catch (Exception e) {// EXIT with message
                            }
                            break;
                    }
                }
                currentConfigDir = "";// So internal changes don't use the path information
                return true;
            }
        } catch (Exception e) {
            return false;
        }

    }

    void parseEnv(String envp) {
        // for(char** env=envp; *env;env++) {
        // char copy[1024];
        // safe_strncpy(copy,*env,1024);
        // if(strncasecmp(copy,"DOSBOX_",7))
        // continue;
        // char* sec_name = &copy[7];
        // if(!(*sec_name))
        // continue;
        // char* prop_name = strrchr(sec_name,'_');
        // if(!prop_name || !(*prop_name))
        // continue;
        // *prop_name++=0;
        // Section sect = GetSection(sec_name);
        // if(sect == null)
        // continue;
        // sect.HandleInputline(prop_name);
        // }
    }

    public boolean getSecureMode() {
        return secureMode;
    }

    public void switchToSecureMode() {
        secureMode = true;
    }// can't be undone
}
