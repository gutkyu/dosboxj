package org.gutkyu.dosboxj.misc;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.LinkedList;
import org.gutkyu.dosboxj.*;
import org.gutkyu.dosboxj.misc.setup.*;
import org.gutkyu.dosboxj.util.Log;

public final class Message {
    private static final int LINE_IN_MAXLEN = 2048;

    static class MessageBlock {
        public String name;
        public String val;

        public MessageBlock(String name, String val) {
            this.name = name;
            this.val = val;
        }
    }

    private static LinkedList<MessageBlock> Lang = new LinkedList<MessageBlock>();

    public static void addMsg(String name, String val) {
        /* Find the message */
        for (MessageBlock tel : Lang) {
            if (tel.name.equals(name)) {
                // Log.LOG_MSG("double entry for %s",_name); //Message file might be loaded before
                // default text messages
                return;
            }
        }
        /* if the message doesn't exist add it */
        Lang.addLast(new MessageBlock(name, val));
    }

    public static void replace(String name, String val) {
        /* Find the message */
        for (MessageBlock msg : Lang) {
            if (msg.name.equals(name)) {
                Lang.remove(msg);
                break;
            }
        }
        /* Even if the message doesn't exist add it */
        Lang.addLast(new MessageBlock(name, val));
    }


    private static void loadMessageFile(String fname) {
        if (fname == null)
            return;
        // if (String.IsNullOrEmpty(fname) || String.IsNullOrWhiteSpace(fname)) return;//empty
        // string=no languagefile
        if (fname == null || fname.isEmpty() || fname.charAt(0) == '\0')
            return;// empty string=no languagefile

        String name = "";
        // List<char> sbmsg = new ArrayList<char>();
        StringBuffer sbmsg = new StringBuffer();

        try (BufferedReader reader = Files.newBufferedReader(Paths.get(fname))) {
            String linein = null;
            while ((linein = reader.readLine()) != null) {
                linein = linein.replace((char) 10, "".charAt(0)).replace((char) 13, "".charAt(0));
                /* New string name */
                if (linein.charAt(0) == ':') {
                    sbmsg.setLength(0);// clear
                    name = linein.substring(1);
                    /* End of string marker */
                } else if (linein.charAt(0) == '.') {
                    /* Replace/Add the string to the internal langaugefile */
                    /* Remove last newline (marker is \n.\n) */
                    // if (sbmsg[sbmsg.Length - 1] == '\n') sbmsg.Remove(sbmsg.Length - 1,
                    // 1);//Second if should not be needed, but better be safe.
                    int len = sbmsg.length();
                    if (sbmsg.charAt(len) == '\n')
                        sbmsg.delete(len - 1, len);// Second if should not be needed, but better be
                                                   // safe.
                    // Replace(name, sbmsg.toString());
                    replace(name, sbmsg.toString());
                } else {
                    /* Normal string to be added */
                    // sbmsg.AppendLine(linein);
                    sbmsg.append(linein);
                    sbmsg.append('\n');
                }
            }

        } catch (Exception e) {/*
                                * This should never happen and since other modules depend on this
                                * use a normal printf
                                */
            Support.exceptionExit("MSG:Can't load messages: %s", fname);
        }
    }


    public static String get(String msg) {
        for (MessageBlock msgBck : Lang) {
            if (msgBck.name.equals(msg)) {
                return msgBck.val;
            }
        }
        return "Message not Found!\n";
    }

    public static void write(String location) {

        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(location),
                StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE)) {
            for (MessageBlock msgBck : Lang) {
                writer.write(String.format("%s\n%s\n.\n", msgBck.name, msgBck.val));
            }
            writer.flush();
        } catch (Exception e) {
            Log.logging(Log.LogTypes.MISC, Log.LogServerities.Error,
                    "Message.write(string '%1$s') error : %2$s", location, e.getMessage());
        }
    }

    public static void init(SectionProperty section) {
        String fileName = "";
        if ((fileName = DOSBox.Control.CmdLine.findString("-lang", true)) != null) {
            loadMessageFile(fileName);
        } else {
            PropertyPath pathprop = section.getPath("language");
            if (pathprop != null)
                loadMessageFile(pathprop.realpath);
        }
    }
}
