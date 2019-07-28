package org.gutkyu.dosboxj.dos.software;


import java.util.ArrayList;
import java.util.List;
import org.gutkyu.dosboxj.cpu.*;
import org.gutkyu.dosboxj.hardware.memory.*;
import org.gutkyu.dosboxj.misc.*;
import org.gutkyu.dosboxj.dos.*;
import org.gutkyu.dosboxj.misc.setup.*;
import org.gutkyu.dosboxj.misc.setup.Value.WrongType;
import org.gutkyu.dosboxj.dos.system.drive.*;

public class Programs {
    private Programs() {
    }

    private static int callProgram;

    // byte
    private static byte[] exeBlock = {

            (byte) 0xbc, 0x00, 0x04, // MOV SP,0x400 decrease stack size
            (byte) 0xbb, 0x40, 0x00, // MOV BX,0x040 for memory resize
            (byte) 0xb4, 0x4a, // MOV AH,0x4A Resize memory block
            (byte) 0xcd, 0x21, // INT 0x21
            (byte) // pos 12 is callback number
            0xFE, 0x38, 0x00, 0x00, // CALLBack number
            (byte) 0xb8, 0x00, 0x4c, // Mov ax,4c00
            (byte) 0xcd, 0x21, // INT 0x21

    };

    private static final int CB_POS = 12;

    private static List<ProgramMake> internalProgs = new ArrayList<ProgramMake>();

    public static void makeFile(String name, ProgramMake makable) {
        byte[] comdata = new byte[32]; // MEM LEAK
        int exeBlockLen = exeBlock.length;
        for (int i = 0; i < exeBlockLen; i++) {
            comdata[i] = exeBlock[i];
        }
        comdata[CB_POS] = (byte) callProgram;
        comdata[CB_POS + 1] = (byte) (callProgram >>> 8);

        /* Copy save the pointer in the vector and save it's index */
        int progsLen = internalProgs.size();
        if (progsLen > 255)
            Support.exceptionExit("PROGRAMS_MakeFile program size too large (%d)", progsLen);
        byte index = (byte) progsLen;
        internalProgs.add(makable);

        comdata[exeBlockLen] = index;
        VFile.register(name, comdata, exeBlockLen + 1);
    }

    private static int programsHandler() throws WrongType {
        /* This sets up everything for a program start up call */
        int size = 1;// sizeof(byte)
        int index = 0;
        /* Read the index from program code in memory */
        int reader = Memory.physMake(DOSMain.DOS.getPSP(), 256 + exeBlock.length);
        for (int i = 0; i < size; i++)
            index |= Memory.readB(reader++) << i;
        if (index > internalProgs.size())
            Support.exceptionExit("something is messing with the memory");
        Program newProgram = internalProgs.get(index).make();
        newProgram.run();
        return Callback.ReturnTypeNone;
    }



    public static void init(Section sec) {
        /* Setup a special callback to start virtual programs */
        callProgram = Callback.allocate();
        Callback.setup(callProgram, Programs::programsHandler, Callback.Symbol.RETF,
                "internal program");
        makeFile("CONFIG.COM", Config::makeProgram);

        Message.addMsg("PROGRAM_CONFIG_FILE_ERROR", "Can't open file %s\n");
        Message.addMsg("PROGRAM_CONFIG_USAGE",
                "Config tool:\nUse -writeconf filename to write the current config.\nUse -writelang filename to write the current language strings.\n");
        Message.addMsg("PROGRAM_CONFIG_SECURE_ON", "Switched to secure mode.\n");
        Message.addMsg("PROGRAM_CONFIG_SECURE_DISALLOW",
                "This operation is not permitted in secure mode.\n");
        Message.addMsg("PROGRAM_CONFIG_SECTION_ERROR", "Section %s doesn't exist.\n");
        Message.addMsg("PROGRAM_CONFIG_PROPERTY_ERROR", "No such section or property.\n");
        Message.addMsg("PROGRAM_CONFIG_NO_PROPERTY", "There is no property %s in section %s.\n");
        Message.addMsg("PROGRAM_CONFIG_GET_SYNTAX",
                "Correct syntax: config -get \"section property\".\n");
    }
}
