package org.gutkyu.dosboxj.util;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Resources {

    public static byte[] get(String resourceName) {
        if (resourceName == null)
            return null;
        ClassLoader classLoader = Resources.class.getClassLoader();
        URL url = classLoader.getResource(resourceName);
        if (url == null)
            return null;
        try {
            return Files.readAllBytes(Paths.get(url.toURI()));
        } catch (Exception e) {
            Log.logging(Log.LogTypes.KEYBOARD, Log.LogServerities.Warn, "resource (%s) load fail",
                    resourceName);
            return null;
        }
    }

}

