package org.gutkyu.dosboxj.misc;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


public final class Cross {
    public static final int NONE = 0;
    public static final int FILE = 1;
    public static final int DIR = 2;
    public static final char FILESPLIT =
            java.nio.file.FileSystems.getDefault().getSeparator().charAt(0);
    public static final int LEN = 512;
    public static final boolean IS_WINDOWS;

    static {
        IS_WINDOWS = System.getProperty("os.name").toLowerCase().startsWith("windows");
    }

    public static String getPlatformConfigDir() {

        String dir = null;
        try {
            dir = new File(Cross.class.getProtectionDomain().getCodeSource().getLocation().toURI())
                    .getParent();
        } catch (Exception e) {
            dir = System.getProperty("user.dir");
        }
        return dir;
    }

    public static String getPlatformConfigName() {
        String fname = null;
        try {
            File runFile = new File(
                    Cross.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            fname = runFile.getName() + ".cfg";
        } catch (Exception e) {
            fname = "dosbox.j";
        }
        return fname;
    }

    public static void createPlatformConfigDir(String dir) {
        Path path = Paths.get(dir);
        if (!Files.exists(path)) {
            try {
                Files.createDirectories(path);
            } catch (Exception e) {

            }
        }
    }

    public static String resolveHomedir(String tempLine) {
        if (tempLine.length() == 0 || tempLine.charAt(0) != '~')
            return tempLine; // No ~

        if (tempLine.length() == 1 || tempLine.charAt(1) == FILESPLIT) { // The ~ and ~/ variant
            String home = System.getProperty("user.home");
            if (home != null)
                return tempLine = home + tempLine.substring(1);

        }
        return tempLine;
    }

    public static void createDir(String temp) {
        try {
            Files.createDirectory(Paths.get(temp));
        } catch (Exception e) {

        }
    }

}
