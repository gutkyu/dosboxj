package org.gutkyu.dosboxj.misc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class Debug {

    public static boolean mouseEnabled = false;
    public static boolean logOn = false;
    public static long skippedLoggingCount = 0;// 4426544 + 3021597 + 2153103 + 4121869 + 3091062;
    private static long count = 0;

    public static void increaseCount() {
        logOn = count++ > skippedLoggingCount;
    }

    //private static int logClick = 0;

    public static void onKeyboardClicked(int dosboxKeyCode){
        // if (dosboxKeyCode == 60)// f2 click
        //     logClick++;
        // if (logClick > 2)
        //     logOn = false;
    }

    public static void log(String text) {
        if(!logOn)
            return;
        try {
            Files.write(Paths.get("./debug.log"), text.getBytes(), StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND);
        } catch (IOException ex) {
            System.out.printf("log err" + ex.getMessage());
        }
    }

}
