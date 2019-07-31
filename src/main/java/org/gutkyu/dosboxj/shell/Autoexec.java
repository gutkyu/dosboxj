package org.gutkyu.dosboxj.shell;

import org.gutkyu.dosboxj.misc.setup.*;
import org.gutkyu.dosboxj.misc.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import org.gutkyu.dosboxj.*;
import org.gutkyu.dosboxj.dos.system.drive.*;

public final class Autoexec extends ModuleBase {
    private AutoexecObject[] _autoexec = new AutoexecObject[17];
    private AutoexecObject _autoexecEcho;

    public Autoexec(Section configuration) {
        super(configuration);
        for (int a = 0; a < 17; a++)
            _autoexec[a] = new AutoexecObject();
        /* Register a virtual AUOEXEC.BAT file */
        String line = "";
        SectionLine section = (SectionLine) configuration;

        /* Check -securemode switch to disable mount/imgmount/boot after running autoexec.bat */
        boolean secure = DOSBox.Control.CmdLine.findExist("-securemode", true);

        /* add stuff from the configfile unless -noautexec or -securemode is specified. */
        String extra = section.data;
        if (extra != null && !secure && !DOSBox.Control.CmdLine.findExist("-noautoexec", true)) {
            /* detect if "echo off" is the first line */
            boolean echoOff = extra.equalsIgnoreCase("echo off");
            if (!echoOff)
                echoOff = extra.equalsIgnoreCase("@echo off");

            /* if "echo off" add it to the front of autoexec.bat */
            if (echoOff)
                _autoexecEcho.installBefore("@echo off");

            /* Install the stuff from the configfile */
            _autoexec[0].install(section.data);
        }

        /*
         * Check to see for extra command line options to be added (before the command specified on
         * commandline)
         */
        /* Maximum of extra commands: 10 */
        int i = 1;
        while ((line = DOSBox.Control.CmdLine.findString("-c", true)) != null && i <= 11) {
            // replace single with double quotes so that mount commands can contain spaces
            line = line.replace('\'', '"');
            _autoexec[i++].install(line);
        }


        /*
         * Check for the -exit switch which causes dosbox to when the command on the commandline has
         * finished
         */
        boolean addexit = DOSBox.Control.CmdLine.findExist("-exit", true);

        while (true) {
            /* Check for first command being a directory or file */
            String buffer = "";
            String orig = "";
            char cross_filesplit = Cross.FILESPLIT;
            BasicFileAttributes attrs = null;
            if (!DOSBox.Control.CmdLine.findCommand(1)) {
                if (secure)
                    _autoexec[12].install("z:\\config.com -securemode");
            } else {
                line = DOSBox.Control.CmdLine.returnedCmd;
                StringBuilder sb = new StringBuilder();
                sb.append(line);
                buffer = sb.toString();
                Path path = Paths.get(buffer);
                if (Files.notExists(path)) {

                    // if(System.Environment.CurrentDirectory.Length > Cross.CROSS_LEN)
                    // sb.Append(System.Environment.CurrentDirectory,0, Cross.CROSS_LEN);
                    // else
                    // sb.Append(System.Environment.CurrentDirectory);
                    String currDir =
                            Paths.get(System.getProperty("user.dir")).toAbsolutePath().toString();
                    if (currDir.length() > Cross.LEN)
                        sb.append(currDir, 0, Cross.LEN);
                    else
                        sb.append(currDir);

                    sb.append(Cross.FILESPLIT);
                    sb.append(line);
                    buffer = sb.toString();
                    path = Paths.get(buffer);
                    if (Files.exists(path))
                        break;// goto nomount;

                }

                if (Files.isDirectory(path)) {
                    _autoexec[12].install("MOUNT C \"" + buffer + "\"");
                    _autoexec[13].install("C:");
                    if (secure)
                        _autoexec[14].install("z:\\config.com -securemode");
                } else {
                    int idx = buffer.lastIndexOf(Cross.FILESPLIT);
                    String name = "";
                    if (idx < 0) { // Only a filename
                        line = buffer;

                        sb = new StringBuilder();
                        String currDir = Paths.get(System.getProperty("user.dir")).toAbsolutePath()
                                .toString();
                        if (currDir.length() > Cross.LEN)
                            sb.append(currDir, 0, Cross.LEN);
                        else
                            sb.append(currDir);

                        sb.append(Cross.FILESPLIT);
                        sb.append(line);
                        buffer = sb.toString();
                        path = Paths.get(buffer);

                        if (Files.exists(path))
                            break;// goto nomount;

                        idx = buffer.lastIndexOf(Cross.FILESPLIT);
                        if (idx < 0)
                            break;// goto nomount;
                    }
                    name = buffer.substring(0, idx);

                    if (Files.exists(Paths.get(buffer)))
                        break;// goto nomount;
                    _autoexec[12].install("MOUNT C \"" + buffer + "\"");
                    _autoexec[13].install("C:");
                    /*
                     * Save the non modified filename (so boot and imgmount can use it (long
                     * filenames, case sensivitive)
                     */
                    orig = name;
                    name = name.toUpperCase();
                    if (name.lastIndexOf(".BAT") >= 0) {
                        if (secure)
                            _autoexec[14].install("z:\\config.com -securemode");
                        /* BATch files are called else exit will not work */
                        _autoexec[15].install("CALL " + name);
                        if (addexit)
                            _autoexec[16].install("exit");
                    } else if ((name.lastIndexOf(".IMG") >= 0) || (name.lastIndexOf(".IMA") >= 0)) {
                        // No secure mode here as boot is destructive and enabling securemode
                        // disables boot
                        /* Boot image files */
                        _autoexec[15].install("BOOT " + orig);
                    } else if ((name.lastIndexOf(".ISO") >= 0) || (name.lastIndexOf(".CUE") >= 0)) {
                        if (secure)
                            _autoexec[14].install("z:\\config.com -securemode");
                        /* imgmount CD image files */
                        _autoexec[15].install("IMGMOUNT D \"" + orig + "\" -t iso");
                        // autoexec[16].Install("D:");
                        /* Makes no sense to exit here */
                    } else {
                        if (secure)
                            _autoexec[14].install("z:\\config.com -securemode");
                        _autoexec[15].install(name);
                        if (addexit)
                            _autoexec[16].install("exit");
                    }
                }
            }
            break;
        }
        // nomount:
        VFile.register("AUTOEXEC.BAT", ShellInner.autoexecData, ShellInner.autoexecData.length);
    }

    @Override
    protected void dispose(boolean disposing) {
        if (disposing) {
            for (AutoexecObject ae : _autoexec) {
                ae.dispose();
            }
            _autoexec = null;
            _autoexecEcho.dispose();
            _autoexecEcho = null;
        }

        super.dispose(disposing);
    }

    private static Autoexec _autoExec = null;

    public static void init(Section section) {
        // 새로운 AUTOEXEC 객체를 생성하기전에 모든 리소스를 해제한다.
        // C++의 암묵적인 소멸자대신 명시적으로 dispose()를 호출해서 해제한다.
        if (_autoExec != null)
            _autoExec.dispose();
        _autoExec = new Autoexec(section);
    }
}
