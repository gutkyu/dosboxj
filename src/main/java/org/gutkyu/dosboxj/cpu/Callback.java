package org.gutkyu.dosboxj.cpu;

import java.util.*;
import java.util.ArrayList;
import org.gutkyu.dosboxj.DOSBox;
import org.gutkyu.dosboxj.hardware.memory.*;
import org.gutkyu.dosboxj.misc.*;
import org.gutkyu.dosboxj.misc.setup.Section;

public final class Callback {
    public static final int MAX = 128;
    public static final int SIZE = 32;
    public static final int SEG = 0xF000;
    public static final int SOFFSET = 0x1000;

    public static List<DOSCallbackHandler> CallbackHandlers = new ArrayList<>();
    public static String[] CallbackDescription = new String[MAX];
    private static int callStop, callIdle, callDefault, callDefault2;
    public static int callPrivIO;

    public enum Symbol {
        //@formatter:off
        RETN(0), RETF(1), RETF8(2), IRET(3), IRETD(4), IRET_STI(5), IRET_EOI_PIC1(6), IRQ0(7),
        IRQ1(8), IRQ9(9), IRQ12(10), IRQ12_RET(11), IRQ6_PCJR(12), MOUSE(13), INT29(14),
        INT16(15), HOOKABLE(16), TDE_IRET(17), IPXESR(18), IPXESR_RET(19), INT21(20);
        //@formatter:on
        private final byte value;

        private Symbol(int value) {
            this.value = (byte) value;
        }

        public int toValue() {
            return value;
        }
    }


    // enum CBReturnType
    public static final int ReturnTypeNone = 0;
    public static final int ReturnTypeStop = 1;


    public static int realPointer(int callback) {
        return Memory.realMake(SEG, SOFFSET + callback * SIZE);
    }

    public static int physPointer(int callback) {
        return Memory.physMake(SEG, SOFFSET + callback * SIZE);
    }

    public static int getBase() {
        return (SEG << 4) + SOFFSET;
    }

    // UI32
    public static int allocate() {
        int i = 0;
        i = CallbackHandlers.subList(1, CallbackHandlers.size()).indexOf(callbackIllegal) + 1;
        if (i >= 1) {
            CallbackHandlers.set(i, null);
            return i;
        }
        Support.exceptionExit("CALLBACK:Can't allocate handler.");
        return 0;
    }

    public static void deallocate(int input) {
        CallbackHandlers.set(input, callbackIllegal);
    }

    public static void idle() {
        /* this makes the cpu execute instructions to handle irq's and then come back */
        int oldIF = Register.getFlag(Register.FlagIF);
        Register.setFlagBit(Register.FlagIF, true);
        int oldcs = Register.segValue(Register.SEG_NAME_CS);
        int oldeip = Register.getRegEIP();
        Register.segSet16(Register.SEG_NAME_CS, Callback.SEG);
        Register.setRegEIP(callIdle * SIZE);
        DOSBox.runMachine();
        Register.setRegEIP(oldeip);
        Register.segSet16(Register.SEG_NAME_CS, oldcs);
        Register.setFlagBit(Register.FlagIF, oldIF);
        if (!CPU.CycleAutoAdjust && CPU.Cycles > 0)
            CPU.Cycles = 0;
    }

    private static int defaultHandler() {
        // LOG(LOG_CPU,LOG_ERROR)("Illegal Unhandled Interrupt Called %X",lastint);
        return ReturnTypeNone;
    }

    private static int stopHandler() {
        return ReturnTypeStop;
    }

    public static void removeSetup(int callback) {
        for (int i = 0; i < 16; i++) {
            Memory.physWriteB(physPointer(callback) + i, 0x00);
        }
    }

    public static boolean setup(int callback, DOSCallbackHandler handler, Callback.Symbol type,
            String descr) {
        if (callback >= MAX)
            return false;
        setupExtra(callback, type, physPointer(callback) + 0, (handler != null));
        CallbackHandlers.set(callback, handler);
        setDescription(callback, descr);
        return true;
    }

    public static int setup(int callback, DOSCallbackHandler handler, Callback.Symbol type,
            int addr, String descr) {
        if (callback >= MAX)
            return 0;
        int csize = setupExtra(callback, type, addr, (handler != null));
        if (csize > 0) {
            CallbackHandlers.set(callback, handler);
            setDescription(callback, descr);
        }
        return csize;
    }

    public static String getDescription(int nr) {
        if (nr >= MAX)
            return null;
        return CallbackDescription[nr];
    }

    public static int setupExtra(int callback, Callback.Symbol type, int physAddress,
            boolean useCB) {
        if (callback >= MAX)
            return 0;
        switch (type) {
            case RETN:
                if (useCB) {
                    Memory.physWriteB(physAddress + 0x00, 0xFE); // GRP 4
                    Memory.physWriteB(physAddress + 0x01, 0x38); // Extra Callback instruction
                    Memory.physWriteW(physAddress + 0x02, callback); // The immediate word
                    physAddress += 4;
                }
                Memory.physWriteB(physAddress + 0x00, 0xC3); // A RETN Instruction
                return (useCB ? 5 : 1);
            case RETF:
                if (useCB) {
                    Memory.physWriteB(physAddress + 0x00, 0xFE); // GRP 4
                    Memory.physWriteB(physAddress + 0x01, 0x38); // Extra Callback instruction
                    Memory.physWriteW(physAddress + 0x02, callback); // The immediate word
                    physAddress += 4;
                }
                Memory.physWriteB(physAddress + 0x00, 0xCB); // A RETF Instruction
                return (useCB ? 5 : 1);
            case RETF8:
                if (useCB) {
                    Memory.physWriteB(physAddress + 0x00, 0xFE); // GRP 4
                    Memory.physWriteB(physAddress + 0x01, 0x38); // Extra Callback instruction
                    Memory.physWriteW(physAddress + 0x02, callback); // The immediate word
                    physAddress += 4;
                }
                Memory.physWriteB(physAddress + 0x00, 0xCA); // A RETF 8 Instruction
                Memory.physWriteW(physAddress + 0x01, 0x0008);
                return (useCB ? 7 : 3);
            case IRET:
                if (useCB) {
                    Memory.physWriteB(physAddress + 0x00, 0xFE); // GRP 4
                    Memory.physWriteB(physAddress + 0x01, 0x38); // Extra Callback instruction
                    Memory.physWriteW(physAddress + 0x02, callback); // The immediate word
                    physAddress += 4;
                }
                Memory.physWriteB(physAddress + 0x00, 0xCF); // An IRET Instruction
                return (useCB ? 5 : 1);
            case IRETD:
                if (useCB) {
                    Memory.physWriteB(physAddress + 0x00, 0xFE); // GRP 4
                    Memory.physWriteB(physAddress + 0x01, 0x38); // Extra Callback instruction
                    Memory.physWriteW(physAddress + 0x02, callback); // The immediate word
                    physAddress += 4;
                }
                Memory.physWriteB(physAddress + 0x00, 0x66); // An IRETD Instruction
                Memory.physWriteB(physAddress + 0x01, 0xCF);
                return (useCB ? 6 : 2);
            case IRET_STI:
                Memory.physWriteB(physAddress + 0x00, 0xFB); // STI
                if (useCB) {
                    Memory.physWriteB(physAddress + 0x01, 0xFE); // GRP 4
                    Memory.physWriteB(physAddress + 0x02, 0x38); // Extra Callback instruction
                    Memory.physWriteW(physAddress + 0x03, callback); // The immediate word
                    physAddress += 4;
                }
                Memory.physWriteB(physAddress + 0x01, 0xCF); // An IRET Instruction
                return (useCB ? 6 : 2);
            case IRET_EOI_PIC1:
                if (useCB) {
                    Memory.physWriteB(physAddress + 0x00, 0xFE); // GRP 4
                    Memory.physWriteB(physAddress + 0x01, 0x38); // Extra Callback instruction
                    Memory.physWriteW(physAddress + 0x02, callback); // The immediate word
                    physAddress += 4;
                }
                Memory.physWriteB(physAddress + 0x00, 0x50); // push ax
                Memory.physWriteB(physAddress + 0x01, 0xb0); // mov al, 0x20
                Memory.physWriteB(physAddress + 0x02, 0x20);
                Memory.physWriteB(physAddress + 0x03, 0xe6); // out 0x20, al
                Memory.physWriteB(physAddress + 0x04, 0x20);
                Memory.physWriteB(physAddress + 0x05, 0x58); // pop ax
                Memory.physWriteB(physAddress + 0x06, 0xcf); // An IRET Instruction
                return (useCB ? 0x0b : 0x07);
            case IRQ0: // timer int8
                if (useCB) {
                    Memory.physWriteB(physAddress + 0x00, 0xFE); // GRP 4
                    Memory.physWriteB(physAddress + 0x01, 0x38); // Extra Callback instruction
                    Memory.physWriteW(physAddress + 0x02, callback); // The immediate word
                    physAddress += 4;
                }
                Memory.physWriteB(physAddress + 0x00, 0x50); // push ax
                Memory.physWriteB(physAddress + 0x01, 0x52); // push dx
                Memory.physWriteB(physAddress + 0x02, 0x1e); // push ds
                Memory.physWriteW(physAddress + 0x03, 0x1ccd); // int 1c
                Memory.physWriteB(physAddress + 0x05, 0xfa); // cli
                Memory.physWriteB(physAddress + 0x06, 0x1f); // pop ds
                Memory.physWriteB(physAddress + 0x07, 0x5a); // pop dx
                Memory.physWriteW(physAddress + 0x08, 0x20b0); // mov al, 0x20
                Memory.physWriteW(physAddress + 0x0a, 0x20e6); // out 0x20, al
                Memory.physWriteB(physAddress + 0x0c, 0x58); // pop ax
                Memory.physWriteB(physAddress + 0x0d, 0xcf); // An IRET Instruction
                return (useCB ? 0x12 : 0x0e);
            case IRQ1: // keyboard int9
                Memory.physWriteB(physAddress + 0x00, 0x50); // push ax
                Memory.physWriteW(physAddress + 0x01, 0x60e4); // in al, 0x60
                Memory.physWriteW(physAddress + 0x03, 0x4fb4); // mov ah, 0x4f
                Memory.physWriteB(physAddress + 0x05, 0xf9); // stc
                Memory.physWriteW(physAddress + 0x06, 0x15cd); // int 15
                if (useCB) {
                    Memory.physWriteW(physAddress + 0x08, 0x0473); // jc skip
                    Memory.physWriteB(physAddress + 0x0a, 0xFE); // GRP 4
                    Memory.physWriteB(physAddress + 0x0b, 0x38); // Extra Callback instruction
                    Memory.physWriteW(physAddress + 0x0c, callback); // The immediate word
                    // jump here to (skip):
                    physAddress += 6;
                }
                Memory.physWriteB(physAddress + 0x08, 0xfa); // cli
                Memory.physWriteW(physAddress + 0x09, 0x20b0); // mov al, 0x20
                Memory.physWriteW(physAddress + 0x0b, 0x20e6); // out 0x20, al
                Memory.physWriteB(physAddress + 0x0d, 0x58); // pop ax
                Memory.physWriteB(physAddress + 0x0e, 0xcf); // An IRET Instruction
                return (useCB ? 0x15 : 0x0f);
            case IRQ9: // pic cascade interrupt
                if (useCB) {
                    Memory.physWriteB(physAddress + 0x00, 0xFE); // GRP 4
                    Memory.physWriteB(physAddress + 0x01, 0x38); // Extra Callback instruction
                    Memory.physWriteW(physAddress + 0x02, callback); // The immediate word
                    physAddress += 4;
                }
                Memory.physWriteB(physAddress + 0x00, 0x50); // push ax
                Memory.physWriteW(physAddress + 0x01, 0x61b0); // mov al, 0x61
                Memory.physWriteW(physAddress + 0x03, 0xa0e6); // out 0xa0, al
                Memory.physWriteW(physAddress + 0x05, 0x0acd); // int a
                Memory.physWriteB(physAddress + 0x07, 0xfa); // cli
                Memory.physWriteB(physAddress + 0x08, 0x58); // pop ax
                Memory.physWriteB(physAddress + 0x09, 0xcf); // An IRET Instruction
                return (useCB ? 0x0e : 0x0a);
            case IRQ12: // ps2 mouse int74
                if (!useCB)
                    Support.exceptionExit("int74 callback must implement a callback handler!");
                Memory.physWriteB(physAddress + 0x00, 0x1e); // push ds
                Memory.physWriteB(physAddress + 0x01, 0x06); // push es
                Memory.physWriteW(physAddress + 0x02, 0x6066); // pushad
                Memory.physWriteB(physAddress + 0x04, 0xfc); // cld
                Memory.physWriteB(physAddress + 0x05, 0xfb); // sti
                Memory.physWriteB(physAddress + 0x06, 0xFE); // GRP 4
                Memory.physWriteB(physAddress + 0x07, 0x38); // Extra Callback instruction
                Memory.physWriteW(physAddress + 0x08, callback); // The immediate word
                return 0x0a;
            case IRQ12_RET: // ps2 mouse int74 return
                if (useCB) {
                    Memory.physWriteB(physAddress + 0x00, 0xFE); // GRP 4
                    Memory.physWriteB(physAddress + 0x01, 0x38); // Extra Callback instruction
                    Memory.physWriteW(physAddress + 0x02, callback); // The immediate word
                    physAddress += 4;
                }
                Memory.physWriteB(physAddress + 0x00, 0xfa); // cli
                Memory.physWriteW(physAddress + 0x01, 0x20b0); // mov al, 0x20
                Memory.physWriteW(physAddress + 0x03, 0xa0e6); // out 0xa0, al
                Memory.physWriteW(physAddress + 0x05, 0x20e6); // out 0x20, al
                Memory.physWriteW(physAddress + 0x07, 0x6166); // popad
                Memory.physWriteB(physAddress + 0x09, 0x07); // pop es
                Memory.physWriteB(physAddress + 0x0a, 0x1f); // pop ds
                Memory.physWriteB(physAddress + 0x0b, 0xcf); // An IRET Instruction
                return (useCB ? 0x10 : 0x0c);
            case IRQ6_PCJR: // pcjr keyboard interrupt
                Memory.physWriteB(physAddress + 0x00, 0x50); // push ax
                Memory.physWriteW(physAddress + 0x01, 0x60e4); // in al, 0x60
                Memory.physWriteW(physAddress + 0x03, 0xe03c); // cmp al, 0xe0
                if (useCB) {
                    Memory.physWriteW(physAddress + 0x05, 0x0674); // je skip
                    Memory.physWriteB(physAddress + 0x07, 0xFE); // GRP 4
                    Memory.physWriteB(physAddress + 0x08, 0x38); // Extra Callback instruction
                    Memory.physWriteW(physAddress + 0x09, callback); // The immediate word
                    physAddress += 4;
                } else {
                    Memory.physWriteW(physAddress + 0x05, 0x0274); // je skip
                }
                Memory.physWriteW(physAddress + 0x07, 0x09cd); // int 9
                // jump here to (skip):
                Memory.physWriteB(physAddress + 0x09, 0xfa); // cli
                Memory.physWriteW(physAddress + 0x0a, 0x20b0); // mov al, 0x20
                Memory.physWriteW(physAddress + 0x0c, 0x20e6); // out 0x20, al
                Memory.physWriteB(physAddress + 0x0e, 0x58); // pop ax
                Memory.physWriteB(physAddress + 0x0f, 0xcf); // An IRET Instruction
                return (useCB ? 0x14 : 0x10);
            case MOUSE:
                Memory.physWriteW(physAddress + 0x00, 0x07eb); // jmp i33hd
                physAddress += 9;
                // jump here to (i33hd):
                if (useCB) {
                    Memory.physWriteB(physAddress + 0x00, 0xFE); // GRP 4
                    Memory.physWriteB(physAddress + 0x01, 0x38); // Extra Callback instruction
                    Memory.physWriteW(physAddress + 0x02, callback); // The immediate word
                    physAddress += 4;
                }
                Memory.physWriteB(physAddress + 0x00, 0xCF); // An IRET Instruction
                return (useCB ? 0x0e : 0x0a);
            case INT16:
                Memory.physWriteB(physAddress + 0x00, 0xFB); // STI
                if (useCB) {
                    Memory.physWriteB(physAddress + 0x01, 0xFE); // GRP 4
                    Memory.physWriteB(physAddress + 0x02, 0x38); // Extra Callback instruction
                    Memory.physWriteW(physAddress + 0x03, callback); // The immediate word
                    physAddress += 4;
                }
                Memory.physWriteB(physAddress + 0x01, 0xCF); // An IRET Instruction
                for (int i = 0; i <= 0x0b; i++)
                    Memory.physWriteB(physAddress + 0x02 + i, 0x90);
                Memory.physWriteW(physAddress + 0x0e, 0xedeb); // jmp callback
                return (useCB ? 0x10 : 0x0c);
            case INT29: // fast console output
                if (useCB) {
                    Memory.physWriteB(physAddress + 0x00, 0xFE); // GRP 4
                    Memory.physWriteB(physAddress + 0x01, 0x38); // Extra Callback instruction
                    Memory.physWriteW(physAddress + 0x02, callback); // The immediate word
                    physAddress += 4;
                }
                Memory.physWriteB(physAddress + 0x00, 0x50); // push ax
                Memory.physWriteW(physAddress + 0x01, 0x0eb4); // mov ah, 0x0e
                Memory.physWriteW(physAddress + 0x03, 0x10cd); // int 10
                Memory.physWriteB(physAddress + 0x05, 0x58); // pop ax
                Memory.physWriteB(physAddress + 0x06, 0xcf); // An IRET Instruction
                return (useCB ? 0x0b : 0x07);
            case HOOKABLE:
                Memory.physWriteB(physAddress + 0x00, 0xEB); // jump near
                Memory.physWriteB(physAddress + 0x01, 0x03); // offset
                Memory.physWriteB(physAddress + 0x02, 0x90); // NOP
                Memory.physWriteB(physAddress + 0x03, 0x90); // NOP
                Memory.physWriteB(physAddress + 0x04, 0x90); // NOP
                if (useCB) {
                    Memory.physWriteB(physAddress + 0x05, 0xFE); // GRP 4
                    Memory.physWriteB(physAddress + 0x06, 0x38); // Extra Callback instruction
                    Memory.physWriteW(physAddress + 0x07, callback); // The immediate word
                    physAddress += 4;
                }
                Memory.physWriteB(physAddress + 0x05, 0xCB); // A RETF Instruction
                return (useCB ? 0x0a : 0x06);
            case TDE_IRET: // TandyDAC end transfer
                if (useCB) {
                    Memory.physWriteB(physAddress + 0x00, 0xFE); // GRP 4
                    Memory.physWriteB(physAddress + 0x01, 0x38); // Extra Callback instruction
                    Memory.physWriteW(physAddress + 0x02, callback); // The immediate word
                    physAddress += 4;
                }
                Memory.physWriteB(physAddress + 0x00, 0x50); // push ax
                Memory.physWriteB(physAddress + 0x01, 0xb8); // mov ax, 0x91fb
                Memory.physWriteW(physAddress + 0x02, 0x91fb);
                Memory.physWriteW(physAddress + 0x04, 0x15cd); // int 15
                Memory.physWriteB(physAddress + 0x06, 0xfa); // cli
                Memory.physWriteW(physAddress + 0x07, 0x20b0); // mov al, 0x20
                Memory.physWriteW(physAddress + 0x09, 0x20e6); // out 0x20, al
                Memory.physWriteB(physAddress + 0x0b, 0x58); // pop ax
                Memory.physWriteB(physAddress + 0x0c, 0xcf); // An IRET Instruction
                return (useCB ? 0x11 : 0x0d);
            /*
             * case CB_IPXESR: // IPX ESR if (!use_cb)
             * E_Exit("ipx esr must implement a callback handler!");
             * MemModule.phys_writeb(physAddress+0x00,(byte)0x1e); // push ds
             * MemModule.phys_writeb(physAddress+0x01,(byte)0x06); // push es
             * MemModule.phys_writew(physAddress+0x02,(short)0xa00f); // push fs
             * MemModule.phys_writew(physAddress+0x04,(short)0xa80f); // push gs
             * MemModule.phys_writeb(physAddress+0x06,(byte)0x60); // pusha
             * MemModule.phys_writeb(physAddress+0x07,(byte)0xFE); //GRP 4
             * MemModule.phys_writeb(physAddress+0x08,(byte)0x38); //Extra Callback instruction
             * MemModule.phys_writew(physAddress+0x09,(short)callback); //The immediate word
             * MemModule.phys_writeb(physAddress+0x0b,(byte)0xCB); //A RETF Instruction return 0x0c;
             * case CB_IPXESR_RET: // IPX ESR return if (use_cb)
             * E_Exit("ipx esr return must not implement a callback handler!");
             * MemModule.phys_writeb(physAddress+0x00,(byte)0xfa); // cli
             * MemModule.phys_writew(physAddress+0x01,(short)0x20b0); // mov al, 0x20
             * MemModule.phys_writew(physAddress+0x05,(short)0x20e6); // out 0x20, al
             * MemModule.phys_writew(physAddress+0x03,(short)0xa0e6); // out 0xa0, al
             * MemModule.phys_writeb(physAddress+0x07,(byte)0x61); // popa
             * MemModule.phys_writew(physAddress+0x08,(short)0xA90F); // pop gs
             * MemModule.phys_writew(physAddress+0x0a,(short)0xA10F); // pop fs
             * MemModule.phys_writeb(physAddress+0x0c,(byte)0x07); // pop es
             * MemModule.phys_writeb(physAddress+0x0d,(byte)0x1f); // pop ds
             * MemModule.phys_writeb(physAddress+0x0e,(byte)0xcf); //An IRET Instruction return
             * 0x0f;
             */
            case INT21:
                Memory.physWriteB(physAddress + 0x00, 0xFB); // STI
                if (useCB) {
                    Memory.physWriteB(physAddress + 0x01, 0xFE); // GRP 4
                    Memory.physWriteB(physAddress + 0x02, 0x38); // Extra Callback instruction
                    Memory.physWriteW(physAddress + 0x03, callback); // The immediate word
                    physAddress += 4;
                }
                Memory.physWriteB(physAddress + 0x01, 0xCF); // An IRET Instruction
                Memory.physWriteB(physAddress + 0x02, 0xCB); // A RETF Instruction
                Memory.physWriteB(physAddress + 0x03, 0x51); // push cx
                Memory.physWriteB(physAddress + 0x04, 0xB9); // mov cx,
                Memory.physWriteW(physAddress + 0x05, 0x0140); // 0x140
                Memory.physWriteW(physAddress + 0x07, 0xFEE2); // loop $-2
                Memory.physWriteB(physAddress + 0x09, 0x59); // pop cx
                Memory.physWriteB(physAddress + 0x0A, 0xCF); // An IRET Instruction
                return (useCB ? 15 : 11);

            default:
                Support.exceptionExit("CALLBACK:Setup:Illegal type %d", type.toValue());
                break;
        }
        return 0;
    }

    public static int setupExtra(int callback, Callback.Symbol type, int physAddress) {
        return setupExtra(callback, type, physAddress, true);
    }

    public static void setDescription(int nr, String descr) {
        if (descr != null) {
            CallbackDescription[nr] = descr;
        } else
            CallbackDescription[nr] = null;
    }

    private static final DOSCallbackHandler callbackIllegal = Callback::illegalHandler;

    private static int illegalHandler() {
        Support.exceptionExit("Illegal CallBack Called");
        return 1;
    }

    // uint16, uint16
    public static void runRealFar(int seg, int off) {
        Register.setRegSP(Register.getRegSP() - 4);
        Memory.writeW(Register.segPhys(Register.SEG_NAME_SS) + Register.getRegSP(),
                Memory.realOff(realPointer(callStop)));
        Memory.writeW(Register.segPhys(Register.SEG_NAME_SS) + Register.getRegSP() + 2,
                Memory.realSeg(realPointer(callStop)));
        int oldeip = Register.getRegEIP();
        int oldcs = Register.segValue(Register.SEG_NAME_CS);
        Register.setRegEIP(off);
        Register.segSet16(Register.SEG_NAME_CS, seg);
        DOSBox.runMachine();
        Register.setRegEIP(oldeip);
        Register.segSet16(Register.SEG_NAME_CS, oldcs);
    }

    public static void runRealInt(int intnum) {
        int oldeip = Register.getRegEIP();
        int oldcs = Register.segValue(Register.SEG_NAME_CS);
        Register.setRegEIP(Callback.SOFFSET + (Callback.MAX * Callback.SIZE) + (intnum * 6));
        Register.segSet16(Register.SEG_NAME_CS, Callback.SEG);

        DOSBox.runMachine();
        Register.setRegEIP(oldeip);
        Register.segSet16(Register.SEG_NAME_CS, oldcs);
    }

    public static void szf(boolean val) {
        int tempf = Memory.readW(Register.segPhys(Register.SEG_NAME_SS) + Register.getRegSP() + 4);
        if (val)
            tempf |= 0xffff & Register.FlagZF;
        else
            tempf &= 0xffff & ~Register.FlagZF;
        Memory.writeW(Register.segPhys(Register.SEG_NAME_SS) + Register.getRegSP() + 4, tempf);
    }

    public static void scf(boolean val) {
        int tempf = Memory.readW(Register.segPhys(Register.SEG_NAME_SS) + Register.getRegSP() + 4);
        if (val)
            tempf |= 0xffff & Register.FlagCF;
        else
            tempf &= 0xffff & ~Register.FlagCF;
        Memory.writeW(Register.segPhys(Register.SEG_NAME_SS) + Register.getRegSP() + 4, tempf);
    }

    public static void sif(boolean val) {
        int tempf = Memory.readW(Register.segPhys(Register.SEG_NAME_SS) + Register.getRegSP() + 4);
        if (val)
            tempf |= 0xffff & Register.FlagIF;
        else
            tempf &= 0xffff & ~Register.FlagIF;
        Memory.writeW(Register.segPhys(Register.SEG_NAME_SS) + Register.getRegSP() + 4, tempf);
    }

    public static void init(Section sec) {
        int i;
        for (i = 0; i < MAX; i++) {
            CallbackHandlers.add(i, callbackIllegal);
        }

        /* Setup the Stop Handler */
        callStop = allocate();
        CallbackHandlers.set(callStop, Callback::stopHandler);
        setDescription(callStop, "stop");
        Memory.physWriteB(physPointer(callStop) + 0, 0xFE);
        Memory.physWriteB(physPointer(callStop) + 1, 0x38);
        Memory.physWriteW(physPointer(callStop) + 2, callStop);

        /* Setup the idle handler */
        callIdle = allocate();
        CallbackHandlers.set(callIdle, Callback::stopHandler);
        setDescription(callIdle, "idle");
        for (i = 0; i <= 11; i++)
            Memory.physWriteB(physPointer(callIdle) + i, 0x90);
        Memory.physWriteB(physPointer(callIdle) + 12, 0xFE);
        Memory.physWriteB(physPointer(callIdle) + 13, 0x38);
        Memory.physWriteW(physPointer(callIdle) + 14, callIdle);

        /* Default handlers for unhandled interrupts that have to be non-null */
        callDefault = allocate();
        setup(callDefault, Callback::defaultHandler, Callback.Symbol.IRET, "default");
        callDefault2 = allocate();
        setup(callDefault2, Callback::defaultHandler, Callback.Symbol.IRET, "default");

        /* Only setup default handler for first part of interrupt table */
        for (short ct = 0; ct < 0x60; ct++) {
            Memory.realWriteD(0, (ct * 4), realPointer(callDefault));
        }
        for (short ct = 0x68; ct < 0x70; ct++) {
            Memory.realWriteD(0, (ct * 4), realPointer(callDefault));
        }
        /* Setup block of 0xCD 0xxx instructions */
        int rintBase = getBase() + MAX * SIZE;
        for (i = 0; i <= 0xff; i++) {
            Memory.physWriteB(rintBase, 0xCD);
            Memory.physWriteB(rintBase + 1, i);
            Memory.physWriteB(rintBase + 2, 0xFE);
            Memory.physWriteB(rintBase + 3, 0x38);
            Memory.physWriteW(rintBase + 4, callStop);
            rintBase += 6;

        }
        // setup a few interrupt handlers that point to bios IRETs by default
        Memory.realWriteD(0, 0x0e * 4, realPointer(callDefault2)); // design your own railroad
        Memory.realWriteD(0, 0x66 * 4, realPointer(callDefault)); // war2d
        Memory.realWriteD(0, 0x67 * 4, realPointer(callDefault));
        Memory.realWriteD(0, 0x68 * 4, realPointer(callDefault));
        Memory.realWriteD(0, 0x5c * 4, realPointer(callDefault)); // Network stuff
        // real_writed(0,0xf*4,0); some games don't like it

        callPrivIO = allocate();

        // virtualizable in-out opcodes
        Memory.physWriteB(physPointer(callPrivIO) + 0x00, 0xec); // in al, dx
        Memory.physWriteB(physPointer(callPrivIO) + 0x01, 0xcb); // retf
        Memory.physWriteB(physPointer(callPrivIO) + 0x02, 0xed); // in ax, dx
        Memory.physWriteB(physPointer(callPrivIO) + 0x03, 0xcb); // retf
        Memory.physWriteB(physPointer(callPrivIO) + 0x04, 0x66); // in eax, dx
        Memory.physWriteB(physPointer(callPrivIO) + 0x05, 0xed);
        Memory.physWriteB(physPointer(callPrivIO) + 0x06, 0xcb); // retf

        Memory.physWriteB(physPointer(callPrivIO) + 0x08, 0xee); // out dx, al
        Memory.physWriteB(physPointer(callPrivIO) + 0x09, 0xcb); // retf
        Memory.physWriteB(physPointer(callPrivIO) + 0x0a, 0xef); // out dx, ax
        Memory.physWriteB(physPointer(callPrivIO) + 0x0b, 0xcb); // retf
        Memory.physWriteB(physPointer(callPrivIO) + 0x0c, 0x66); // out dx, eax
        Memory.physWriteB(physPointer(callPrivIO) + 0x0d, 0xef);
        Memory.physWriteB(physPointer(callPrivIO) + 0x0e, 0xcb); // retf
    }
}
