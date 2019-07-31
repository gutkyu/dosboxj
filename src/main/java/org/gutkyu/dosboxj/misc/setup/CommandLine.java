package org.gutkyu.dosboxj.misc.setup;

import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.LinkedList;

public final class CommandLine {
    // argv[0]는 null 문자가 있든 없든 상관없으나 나머지 배열값들은 null 문자(0x00)으로 종료되지 않아야함
    public CommandLine(int argc, String... argv) {
        if (argc > 0) {
            fileName = argv[0];
        }
        int i = 1;
        while (i < argc) {
            cmds.addLast(argv[i]);
            i++;
        }
    }

    // name은 null 문자가 마지막에 있든 없든 상관없음, cmdline은 null 문자(0x00)이 마지막에 위치해야함
    public CommandLine(String name, String cmdline) {
        if (name != null)
            fileName = name;
        /* Parse the cmds and put them in the list */
        boolean inWord, inQuote;
        char c;
        inWord = false;
        inQuote = false;
        StringBuilder str = new StringBuilder();
        // List<char> str = new ArrayList<char>();
        int cCmdlineIdx = 0;
        while (!(cmdline == null || cmdline.isEmpty()) && cCmdlineIdx < cmdline.length()
                && (c = cmdline.charAt(cCmdlineIdx)) != 0) {
            if (inQuote) {
                if (c != '"')
                    str.append(c);
                // str.Add(c);
                else {
                    inQuote = false;
                    cmds.addLast(str.toString());
                    // cmds.AddLast(new String(str.ToArray()));
                    // str.Clear();
                    str.setLength(0);
                }
            } else if (inWord) {
                if (c != ' ')
                    str.append(c);
                // str.Add(c);
                else {
                    inWord = false;
                    cmds.addLast(str.toString());
                    // cmds.AddLast(new String(str.ToArray()));
                    // str.Clear();
                    str.setLength(0);

                }
            } else if (c == '"') {
                inQuote = true;
            } else if (c != ' ') {
                str.append(c);
                inWord = true;
            }
            // else if (c != ' ') { str.Add(c); inWord = true; }
            cCmdlineIdx++;
        }
        if (inWord || inQuote)
            cmds.addLast(str.toString());
        // if (inWord || inQuote) cmds.addLast(new String(str.ToArray()));
    }

    public CommandLine(byte[] name, byte[] cmdline, int cmdlineStart) {
        if (name != null)
            fileName = new String(name, StandardCharsets.UTF_8);
        /* Parse the cmds and put them in the list */
        boolean inWord, inQuote;
        char c;
        inWord = false;
        inQuote = false;
        // StringBuilder str = new StringBuilder();
        StringBuffer str = new StringBuffer();
        // List<char> str = new ArrayList<char>();
        int cCmdlineIdx = cmdlineStart;
        while ((c = (char) cmdline[cCmdlineIdx]) != 0) {
            if (inQuote) {
                if (c != '"')
                    str.append(c);
                // str.Add(c);
                else {
                    inQuote = false;
                    cmds.addLast(str.toString());
                    // cmds.AddLast(new String(str.ToArray()));
                    str.setLength(0);
                    // str.Clear();
                }
            } else if (inWord) {
                if (c != ' ')
                    str.append(c);
                // str.Add(c);
                else {
                    inWord = false;
                    // cmds.AddLast(new String(str.ToArray()));
                    cmds.addLast(str.toString());
                    str.setLength(0);
                    // str.Clear();
                }
            } else if (c == '"') {
                inQuote = true;
            } else if (c != ' ') {
                str.append(c);
                inWord = true;
            }
            // else if (c != ' ') { str.Add(c); inWord = true; }
            cCmdlineIdx++;
        }
        if (inWord || inQuote)
            cmds.addLast(str.toString());
        // if (inWord || inQuote) cmds.AddLast(new String(str.ToArray()));
    }

    public String getFileName() {
        return fileName != null && fileName.length() > 0
                && fileName.charAt(fileName.length() - 1) == '\0' ? fileName : fileName + "\0";
    }

    public boolean findExist(String name, boolean remove) {
        int i = 0;
        if ((i = findEntry(name, false)) < 0)
            return false;
        if (remove)
            cmds.remove(i);
        return true;
    }

    public boolean findExist(String name) {
        return findExist(name, false);
    }

    public int returnedHex;

    // public boolean FindHex(String name, ref int value, boolean remove = false)
    // hex value -> returnedHex
    public boolean findHex(String name, boolean remove) {
        int i = 0;
        if ((i = findEntry(name, true)) < 0)
            return false;
        returnedHex = Integer.parseInt(cmds.get(i), 16);
        if (remove) {
            cmds.remove(i + 1);
            cmds.remove(i);
        }
        return true;
    }

    public boolean findHex(String name) {
        return findHex(name, false);
    }

    public int returnedInt;

    // public boolean FindInt(String name, ref int value, boolean remove = false)
    // Int value -> returnedInt
    public boolean findInt(String name, boolean remove) {
        int i = 0;
        if ((i = findEntry(name, true)) < 0)
            return false;
        returnedInt = Integer.parseInt(cmds.get(i));
        if (remove) {
            cmds.remove(i + 1);
            cmds.remove(i);
        }
        return true;
    }

    public boolean findInt(String name) {
        return findInt(name, false);
    }

    public String findString(String name, boolean remove) {
        int i = 0;
        if ((i = findEntry(name, true)) < 0)
            return null;
        String ret = cmds.get(i + 1);
        if (remove) {
            cmds.remove(i + 1);
            cmds.remove(i);
        }
        return ret;
    }

    public String findString(String name) {
        return findString(name, false);
    }

    // 1 <= which <= cmds.size()
    // boolean FindCommand(uint which, ref String value)
    public String findCommand(int which) {
        if (which < 1)
            return null;
        if (which > cmds.size())
            return null;
        return cmds.get(which - 1);// which > 1
    }

    // 인자 begin은 null 문자(0x00)을 제외한 string이 입력되어야한다.
    // public boolean FindStringBegin(String begin, ref String value, boolean remove = false)
    public String findStringBegin(String begin, boolean remove) {
        // size_t len = strlen(begin);
        String ret = null;
        int len = cmds.size();
        for (int i = 0; i < len; i++) {
            String cmd = cmds.get(i);
            if (cmd.toUpperCase().startsWith(begin)) {
                ret = cmd.substring(0, begin.length());
                if (remove)
                    cmds.remove(i);
                return ret;
            }
        }
        return ret;

    }

    public String findStringBegin(String begin) {
        return findStringBegin(begin, false);
    }

    // public boolean FindStringRemain(String name, ref byte[] value)
    public byte[] findStringRemainB(String name) {
        String val = findStringRemainS(name);
        if (val == null)
            return null;
        byte[] ret = new byte[val.length() + 1];
        int i = 0;
        for (byte b : val.getBytes(StandardCharsets.US_ASCII)) {
            ret[i++] = b;
        }
        ret[i] = 0x00;
        return ret;
    }

    // public boolean FindStringRemain(String name, ref String value)
    public String findStringRemainS(String name) {
        StringBuffer sb = new StringBuffer();
        int i = 0;
        if ((i = findEntry(name)) < 0)
            return null;
        i++;
        int len = cmds.size();
        for (; i < len; i++) {
            sb.append(" ");
            sb.append(cmds.get(i));
        }
        return sb.toString();
    }

    public String getStringRemain(String value) {
        if (cmds.size() == 0)
            return null;

        StringBuffer strBuf = new StringBuffer(value);
        for (String cmd : cmds) {
            strBuf.append(" ");
            strBuf.append(cmd);
        }
        return strBuf.toString();
    }

    public int getCount() {
        return cmds.size();
    }

    // public void Shift(uint amount = 1)
    public void shift(int amount) {
        while (amount-- > 0) {
            fileName = cmds.size() > 0 ? cmds.getFirst() : "";
            if (cmds.size() > 0)
                cmds.remove(0);
        }
    }

    public void shift() {
        shift(1);
    }

    // uint16
    public int getArgLength() {
        if (cmds.size() == 0)
            return 0;
        return cmds.stream().mapToInt(s -> s.length() + 1).reduce(0,
                (subtotal, len) -> subtotal + len);
    }


    // 저장된 string은 null 문자(0x00)로 끝나지 않아야함
    private LinkedList<String> cmds = new LinkedList<String>();
    private String fileName;

    private int findEntry(String name, boolean neednext) {
        Iterator<String> it = cmds.iterator();
        String node = null;
        while (it.hasNext()) {
            node = it.next();
            if (!node.equalsIgnoreCase(name))
                continue;

            if (neednext && !it.hasNext())
                return -1;
            return cmds.indexOf(node);

        }
        return -1;
    }

    private int findEntry(String name) {
        return findEntry(name, false);

    }
}
