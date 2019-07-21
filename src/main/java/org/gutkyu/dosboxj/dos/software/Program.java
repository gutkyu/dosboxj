package org.gutkyu.dosboxj.dos.software;



import org.gutkyu.dosboxj.dos.mem_block.*;
import org.gutkyu.dosboxj.hardware.memory.*;
import org.gutkyu.dosboxj.util.*;
import org.gutkyu.dosboxj.shell.*;
import org.gutkyu.dosboxj.dos.*;
import org.gutkyu.dosboxj.misc.setup.*;
import org.gutkyu.dosboxj.misc.setup.Value.WrongType;

public abstract class Program implements Disposable {
    public Program() {
        /* Find the command line and setup the PSP */
        PSP = new DOSPSP(DOSMain.DOS.getPSP());
        /* Scan environment for filename */
        int envscan = Memory.physMake(PSP.getEnvironment(), 0);
        while (Memory.readB(envscan) != 0)
            envscan += Memory.memStrLen(envscan) + 1;
        envscan += 3;
        byte[] tail = new byte[DOSMain.CommandTailSize];
        Memory.blockRead(Memory.physMake(DOSMain.DOS.getPSP(), 128), tail, 0, 128);
        if (tail[DOSMain.CommandTailOffCount] < 127)
            tail[DOSMain.CommandTailOffBuffer + tail[DOSMain.CommandTailOffCount]] = 0;
        else
            tail[DOSMain.CommandTailOffBuffer + 126] = 0;
        byte[] filename = new byte[256 + 1];
        Memory.strCopy(envscan, filename, 256);
        Cmd = new CommandLine(filename, tail, DOSMain.CommandTailOffBuffer);
    }

    public void dispose() {
        // dispose(true);
    }

    protected void dispose(boolean disposing) {
        /*
         * if (disposing) { internal_progs.Clear(); internal_progs = null; exe_block = null;
         * temp_line = null; cmd = null; psp = null; }
         */

    }

    public String TempLine;
    public CommandLine Cmd;
    public DOSPSP PSP;

    public abstract void run() throws WrongType;

    // 실패하면 null 반환
    public String getEnvStr(String entry) {
        /* Walk through the internal environment and see for a match */
        int env_read = Memory.physMake(PSP.getEnvironment(), 0);

        CStringPt envString = CStringPt.create(1024 + 1);
        // result.erase();
        String result = "";
        if (entry == null || entry == "")
            return null;
        do {
            Memory.strCopy(env_read, envString, 1024);
            if (envString.get(0) == 0)
                return null;
            env_read += (int) (envString.length() + 1);
            CStringPt equal = envString.positionOf('=');
            if (equal.isEmpty())
                continue;
            /* replace the = with \0 to get the length */
            equal.set((char) 0);
            if (envString.length() != entry.length())
                continue;
            if (!envString.equalsIgnoreCase(entry))
                continue;
            /* restore the = to get the original result */
            equal.set('=');
            return result = envString.toString();
        } while (true);
        // return false;
    }

    public String getEnvNum(int num) {
        String result = null;
        CStringPt envString = CStringPt.create(1024 + 1);
        int envRead = Memory.physMake(PSP.getEnvironment(), 0);
        do {
            Memory.strCopy(envRead, envString, 1024);
            if (envString.get(0) == 0)
                break;
            if (num == 0) {
                result = envString.toString();
                return result;
            }
            envRead += (int) (envString.length() + 1);
            num--;
        } while (true);
        return null;
    }

    public int getEnvCount() {
        int envRead = Memory.physMake(PSP.getEnvironment(), 0);
        int num = 0;
        while (Memory.readB(envRead) != 0) {
            for (; Memory.readB(envRead) != 0; envRead++) {
            }
            envRead++;
            num++;
        }
        return num;
    }

    public boolean setEnv(String entry, String newString) {
        int envRead = Memory.physMake(PSP.getEnvironment(), 0);
        int envWrite = envRead;
        CStringPt envString = CStringPt.create(1024 + 1);
        do {
            Memory.strCopy(envRead, envString, 1024);
            if (envString.get(0) == 0)
                break;
            envRead += (int) (envString.length() + 1);
            if (envString.lastIndexOf('=') < 0)
                continue; /* Remove corrupt entry? */
            if ((envString.startWith(entry)) && envString.get(entry.length()) == '=')
                continue;
            Memory.blockWrite(envWrite, envString);
            envWrite += (int) (envString.length() + 1);
        } while (true);
        /* TODO Maybe save the program name sometime. not really needed though */
        /* Save the new entry */
        if (newString.charAt(0) != 0) {
            entry = entry.toUpperCase();
            envString = CStringPt.create(String.format("%1$s=%2$s", entry, newString));
            // sprintf(env_string,"%s=%s",entry,new_string); //oldcode
            Memory.blockWrite(envWrite, envString);
            envWrite += (int) (envString.length() + 1);
        }
        /* Clear out the final piece of the environment */
        Memory.writeD(envWrite, 0);
        return true;
    }

    // 일부러 문자열을 byte[]로 바꾸지 않고 string으로 처리
    // 미리 파일에 저장된 문자열을 읽어서 (MSG_Get()) 처리하는데 사용하므로
    // 또, 그 파일은 유니코드로 저장할 예정이기 때문에 인자는 string(유니코드 문자열)타입으로 처리한다.
    public void writeOut(String format, Object... args) /* Write to standard output */
    {
        // byte[] buf = new byte[2048];

        String buf = String.format(format, args);
        // short size = (short)CString.strlen(buf);
        int size = buf.length();
        for (short i = 0; i < size; i++) {
            byte outByte = 0;
            short s = 1;
            if (buf.charAt(i) == 0xA && i > 0 && buf.charAt(i - 1) != 0xD) {
                outByte = (byte) 0xD;
                DOSMain.writeFile(DOSMain.STDOUT, outByte);
            }
            outByte = (byte) buf.charAt(i);
            DOSMain.writeFile(DOSMain.STDOUT, outByte);
        }

        // DOS_WriteFile(STDOUT,(byte *)buf,&size);
    }

    public void writeOutNoParsing(byte[] format) {
        int size = CStringHelper.strlen(format);
        int bufIdx = 0;
        byte outByte = 0;
        for (int i = 0; i < size; i++) {
            // short s = 1;
            if (format[bufIdx + i] == 0xA && i > 0 && format[bufIdx + i - 1] != 0xD) {
                outByte = 0xD;
                DOSMain.writeFile(DOSMain.STDOUT, outByte);
            }
            outByte = format[bufIdx + i];
            DOSMain.writeFile(DOSMain.STDOUT, outByte);
        }

        // DOS_WriteFile(STDOUT,(Bit8u *)format,&size);
    }

    public void writeOutNoParsing(CStringPt format) {
        int size = format.length();
        int bufIdx = 0;
        byte outByte = 0;
        for (int i = 0; i < size; i++) {
            // short s = 1;
            if (format.get(bufIdx + i) == 0xA && i > 0 && format.get(bufIdx + i - 1) != 0xD) {
                outByte = 0xD;
                DOSMain.writeFile(DOSMain.STDOUT, outByte);
            }
            outByte = (byte) format.get(bufIdx + i);
            DOSMain.writeFile(DOSMain.STDOUT, outByte);
        }

        // DOS_WriteFile(STDOUT,(Bit8u *)format,&size);
    }

    public void writeOutNoParsing(String format) /* Write to standard output, no parsing */
    {
        // short size = (short)strlen(format);
        int size = format.length();
        // char const* buf = format;
        String buf = format;
        byte outByte = 0;
        for (int i = 0; i < size; i++) {
            // short s = 1;
            if (buf.charAt(i) == 0xA && i > 0 && buf.charAt(i - 1) != 0xD) {
                outByte = 0xD;
                DOSMain.writeFile(DOSMain.STDOUT, outByte);
            }
            outByte = (byte) buf.charAt(i);
            DOSMain.writeFile(DOSMain.STDOUT, outByte);
        }

        // DOS_WriteFile(STDOUT,(byte *)format,&size);
    }

    public void changeToLongCmd() {
        /*
         * Get arguments directly from the shell instead of the psp. this is done in securemode: (as
         * then the arguments to mount and friends can only be given on the shell ( so no int 21 4b)
         * Securemode part is disabled as each of the internal command has already protection for
         * it. (and it breaks games like cdman) it is also done for long arguments to as it is
         * convient (as the total commandline can be longer then 127 characters. imgmount with lot's
         * of parameters Length of arguments can be ~120. but switch when above 100 to be sure
         */

        if (/* control.SecureMode() || */ Cmd.getArgLength() > 100) {
            CommandLine temp = new CommandLine(Cmd.getFileName(), DOSShell.FullArguments + "\0");
            Cmd = null;
            Cmd = temp;
        }
        DOSShell.FullArguments = ""; // Clear so it gets even more save
    }


}
