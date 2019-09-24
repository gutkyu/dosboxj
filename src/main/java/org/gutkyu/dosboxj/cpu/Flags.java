package org.gutkyu.dosboxj.cpu;

import org.gutkyu.dosboxj.util.*;

public final class Flags {
    public static LazyFlags LzFlags = new LazyFlags();

    // public static byte getLzFVar1b()
    public static int getLzFVar1b() {
        return Flags.LzFlags.Var1.getByteL();
    }

    // (byte)
    public static void setLzFVar1b(int value) {
        Flags.LzFlags.Var1.setByteL(value);
    }

    // public static byte getLzFVar2b()
    public static int getLzFVar2b() {
        return Flags.LzFlags.Var2.getByteL();
    }

    // (byte)
    public static void setLzFVar2b(int value) {
        Flags.LzFlags.Var2.setByteL(value);
    }

    // public static byte getLzFResb()
    public static int getLzFResb() {
        return Flags.LzFlags.Res.getByteL();
    }

    // (byte)
    public static void setLzFResb(int value) {
        Flags.LzFlags.Res.setByteL(value);
    }

    // public static short getLzFVar1w()
    public static int getLzFVar1w() {
        return Flags.LzFlags.Var1.getWord();
    }

    // (uint16)
    public static void setLzFVar1w(int value) {
        Flags.LzFlags.Var1.setWord(value);
    }

    // public static short getLzFvar2w()
    public static int getLzFvar2w() {
        return Flags.LzFlags.Var2.getWord();
    }

    // (uint16)
    public static void setLzFvar2w(int value) {
        Flags.LzFlags.Var2.setWord(value);
    }

    // public static short getLzFresw()
    public static int getLzFresw() {
        return Flags.LzFlags.Res.getWord();
    }

    public static void setLzFresw(int value) {
        Flags.LzFlags.Res.setWord(value);
    }

    public static int getLzFVar1d() {
        return Flags.LzFlags.Var1.getDWord();
    }

    public static void setLzFVar1d(int value) {
        Flags.LzFlags.Var1.setDWord(value);
    }

    public static int getLzFVar2d() {
        return Flags.LzFlags.Var2.getDWord();
    }

    public static void setLzFVar2d(int value) {
        Flags.LzFlags.Var2.setDWord(value);
    }

    public static int getLzFResd() {
        return Flags.LzFlags.Res.getDWord();
    }

    public static void setLzFResd(int value) {
        Flags.LzFlags.Res.setDWord(value);
    }

    public static void setFLagSb(int FLAGB) {
        Register.setFlagBit(Register.FlagOF, Flags.getOF());
        Flags.LzFlags.Type = TypeFlag.UNKNOWN;
        CPU.setFlags(FLAGB, Register.FMaskNormal & 0xff);
    }

    public static void setFlagSw(int FLAGW) {
        Flags.LzFlags.Type = TypeFlag.UNKNOWN;
        CPU.setFlagsW(FLAGW);
    }

    public static void setFlagSd(int FLAGD) {
        Flags.LzFlags.Type = TypeFlag.UNKNOWN;
        CPU.setFlagsD(FLAGD);
    }

    // 리턴 값은 uint보다 bool type이 더 적합
    public static boolean getTFlgO() {
        return Flags.getOF();
    }

    public static boolean getTFlgNO() {
        return (!Flags.getOF());
    }

    public static boolean getTFlgB() {
        return (Flags.getCF());
    }

    public static boolean getTFlgNB() {
        return (!Flags.getCF());
    }

    public static boolean getTFlgZ() {
        return (Flags.getZF());
    }

    public static boolean getTFlgNZ() {
        return (!Flags.getZF());
    }

    public static boolean getTFlgBE() {
        return (Flags.getCF() || Flags.getZF());
    }

    public static boolean getTFlgNBE() {
        return (!Flags.getCF() && !Flags.getZF());
    }

    public static boolean getTFlgS() {
        return (Flags.getSF());
    }

    public static boolean getTFlgNS() {
        return (!Flags.getSF());
    }

    public static boolean getTFlgP() {
        return (Flags.getPF());
    }

    public static boolean getTFlgNP() {
        return (!Flags.getPF());
    }

    public static boolean getTFlgL() {
        return (Flags.getSF() != Flags.getOF());
    }

    public static boolean getTFlgNL() {
        return (Flags.getSF() == Flags.getOF());
    }

    public static boolean getTFlgLE() {
        return (Flags.getZF() || (Flags.getSF() != Flags.getOF()));
    }

    public static boolean getTFlgNLE() {
        return (!Flags.getZF() && (Flags.getSF() == Flags.getOF()));
    }

    // Types of Flag changing instructions
    public enum TypeFlag {
        //@formatter:off
        UNKNOWN(0),
        ADDb(1), ADDw(2), ADDd(3),
        ORb(4), ORw(5), ORd(6),
        ADCb(7), ADCw(8), ADCd(9),
        SBBb(10), SBBw(11), SBBd(12),
        ANDb(13), ANDw(14), ANDd(15),
        SUBb(16), SUBw(17), SUBd(18),
        XORb(19), XORw(20), XORd(21),
        CMPb(22), CMPw(23), CMPd(24),
        INCb(25), INCw(26), INCd(27),
        DECb(28), DECw(29), DECd(30),
        TESTb(31), TESTw(32), TESTd(33),
        SHLb(34), SHLw(35), SHLd(36),
        SHRb(37), SHRw(38), SHRd(39),
        SARb(40), SARw(41), SARd(42),
        ROLb(43), ROLw(44), ROLd(45),
        RORb(46), RORw(47), RORd(48),
        RCLb(49), RCLw(50), RCLd(51),
        RCRb(52), RCRw(53), RCRd(54),
        NEGb(55), NEGw(56), NEGd(57),

        DSHLw(58), DSHLd(59),
        DSHRw(60), DSHRd(61),
        MUL(61), DIV(63),
        NOTDONE(64),
        LASTFLAG(63);
        //@formatter:on

        private final int value;

        private TypeFlag(int value) {
            this.value = value;
        }

        public int toValue() {
            return this.value;
        }
    }

    /*
     * CF Carry Flag -- Set on high-order bit carry or borrow; cleared otherwise.
     */
    public static boolean getCF() {

        switch (LzFlags.Type) {
            case UNKNOWN:
            case INCb:
            case INCw:
            case INCd:
            case DECb:
            case DECw:
            case DECd:
            case MUL:
                return Register.getFlag(Register.FlagCF) != 0;
            case ADDb:
                return (Flags.getLzFResb() < Flags.getLzFVar1b());
            case ADDw:
                return (Flags.getLzFresw() < Flags.getLzFVar1w());
            case ADDd:
                return ((0xffffffffL & Flags.getLzFResd()) < (0xffffffffL & Flags.getLzFVar1d()));
            case ADCb:
                return (Flags.getLzFResb() < Flags.getLzFVar1b())
                        || (LzFlags.oldCF != 0 && (Flags.getLzFResb() == Flags.getLzFVar1b()));
            case ADCw:
                return (Flags.getLzFresw() < Flags.getLzFVar1w())
                        || (LzFlags.oldCF != 0 && (Flags.getLzFresw() == Flags.getLzFVar1w()));
            case ADCd:
                return ((0xffffffffL & Flags.getLzFResd()) < (0xffffffffL & Flags.getLzFVar1d()))
                        || (LzFlags.oldCF != 0 && (Flags.getLzFResd() == Flags.getLzFVar1d()));
            case SBBb:
                return (Flags.getLzFVar1b() < Flags.getLzFResb())
                        || (LzFlags.oldCF != 0 && (Flags.getLzFVar2b() == 0xff));
            case SBBw:
                return (Flags.getLzFVar1w() < Flags.getLzFresw())
                        || (LzFlags.oldCF != 0 && (Flags.getLzFvar2w() == 0xffff));
            case SBBd:
                return ((0xffffffffL & Flags.getLzFVar1d()) < (0xffffffffL & Flags.getLzFResd()))
                        || (LzFlags.oldCF != 0 && (Flags.getLzFVar2d() == 0xffffffff));
            case SUBb:
            case CMPb:
                return (Flags.getLzFVar1b() < Flags.getLzFVar2b());
            case SUBw:
            case CMPw:
                return (Flags.getLzFVar1w() < Flags.getLzFvar2w());
            case SUBd:
            case CMPd:
                return ((0xffffffffL & Flags.getLzFVar1d()) < (0xffffffffL & Flags.getLzFVar2d()));
            case SHLb:
                if (Flags.getLzFVar2b() > 8)
                    return false;
                else
                    return ((Flags.getLzFVar1b() >>> (8 - Flags.getLzFVar2b())) & 1) != 0;
            case SHLw:
                if (Flags.getLzFVar2b() > 16)
                    return false;
                else
                    return ((Flags.getLzFVar1w() >>> (16 - Flags.getLzFVar2b())) & 1) != 0;
            case SHLd:
            case DSHLw: /* Hmm this is not correct for shift higher than 16 */
            case DSHLd:
                return ((Flags.getLzFVar1d() >>> (32 - Flags.getLzFVar2b())) & 1) != 0;
            case RCRb:
            case SHRb:
                return ((Flags.getLzFVar1b() >>> (Flags.getLzFVar2b() - 1)) & 1) != 0;
            case RCRw:
            case SHRw:
                return ((Flags.getLzFVar1w() >>> (Flags.getLzFVar2b() - 1)) & 1) != 0;
            case RCRd:
            case SHRd:
            case DSHRw: /* Hmm this is not correct for shift higher than 16 */
            case DSHRd:
                return ((Flags.getLzFVar1d() >>> (Flags.getLzFVar2b() - 1)) & 1) != 0;
            case SARb:
                return ((Flags.getLzFVar1b() >>> (Flags.getLzFVar2b() - 1)) & 1) != 0;
            case SARw:
                return ((Flags.getLzFVar1w() >>> (Flags.getLzFVar2b() - 1)) & 1) != 0;
            case SARd:
                return ((Flags.getLzFVar1d() >>> (Flags.getLzFVar2b() - 1)) & 1) != 0;
            case NEGb:
                return Flags.getLzFVar1b() != 0;
            case NEGw:
                return Flags.getLzFVar1w() != 0;
            case NEGd:
                return Flags.getLzFVar1d() != 0;
            case ORb:
            case ORw:
            case ORd:
            case ANDb:
            case ANDw:
            case ANDd:
            case XORb:
            case XORw:
            case XORd:
            case TESTb:
            case TESTw:
            case TESTd:
                return false; /* Set to false */
            case DIV:
                return false; /* Unkown */
            default:
                Log.logging(Log.LogTypes.CPU, Log.LogServerities.Error, "geLazyt_CF Unknown %d",
                        LzFlags.Type.toValue());
                break;
        }
        return false;
    }

    /*
     * AF Adjust flag -- Set on carry from or borrow to the low order four bits of AL; cleared
     * otherwise. Used for decimal arithmetic.
     */
    public static boolean getAF() {
        Flags.TypeFlag type = LzFlags.Type;
        switch (type) {
            case UNKNOWN:
                return Register.getFlag(Register.FlagAF) != 0;
            case ADDb:
            case ADCb:
            case SBBb:
            case SUBb:
            case CMPb:
                return (((Flags.getLzFVar1b() ^ Flags.getLzFVar2b()) ^ Flags.getLzFResb())
                        & 0x10) != 0;
            case ADDw:
            case ADCw:
            case SBBw:
            case SUBw:
            case CMPw:
                return (((Flags.getLzFVar1w() ^ Flags.getLzFvar2w()) ^ Flags.getLzFresw())
                        & 0x10) != 0;
            case ADCd:
            case ADDd:
            case SBBd:
            case SUBd:
            case CMPd:
                return (((Flags.getLzFVar1d() ^ Flags.getLzFVar2d()) ^ Flags.getLzFResd())
                        & 0x10) != 0;
            case INCb:
                return (Flags.getLzFResb() & 0x0f) == 0;
            case INCw:
                return (Flags.getLzFresw() & 0x0f) == 0;
            case INCd:
                return (Flags.getLzFResd() & 0x0f) == 0;
            case DECb:
                return (Flags.getLzFResb() & 0x0f) == 0x0f;
            case DECw:
                return (Flags.getLzFresw() & 0x0f) == 0x0f;
            case DECd:
                return (Flags.getLzFResd() & 0x0f) == 0x0f;
            case NEGb:
                return (Flags.getLzFVar1b() & 0x0f) != 0;
            case NEGw:
                return (Flags.getLzFVar1w() & 0x0f) != 0;
            case NEGd:
                return (Flags.getLzFVar1d() & 0x0f) != 0;
            case SHLb:
            case SHRb:
            case SARb:
                return (Flags.getLzFVar2b() & 0x1f) != 0;
            case SHLw:
            case SHRw:
            case SARw:
                return (Flags.getLzFvar2w() & 0x1f) != 0;
            case SHLd:
            case SHRd:
            case SARd:
                return (Flags.getLzFVar2d() & 0x1f) != 0;
            case ORb:
            case ORw:
            case ORd:
            case ANDb:
            case ANDw:
            case ANDd:
            case XORb:
            case XORw:
            case XORd:
            case TESTb:
            case TESTw:
            case TESTd:
            case DSHLw:
            case DSHLd:
            case DSHRw:
            case DSHRd:
            case DIV:
            case MUL:
                return false; /* Unkown */
            default:
                Log.logging(Log.LogTypes.CPU, Log.LogServerities.Error, "geLazyt_AF Unknown %d",
                        LzFlags.Type.toValue());
                break;
        }
        return false;
    }

    /*
     * ZF Zero Flag -- Set if result is zero; cleared otherwise.
     */

    public static boolean getZF() {
        Flags.TypeFlag type = LzFlags.Type;
        switch (type) {
            case UNKNOWN:
                return Register.getFlag(Register.FlagZF) != 0;
            case ADDb:
            case ORb:
            case ADCb:
            case SBBb:
            case ANDb:
            case XORb:
            case SUBb:
            case CMPb:
            case INCb:
            case DECb:
            case TESTb:
            case SHLb:
            case SHRb:
            case SARb:
            case NEGb:
                return (Flags.getLzFResb() == 0);
            case ADDw:
            case ORw:
            case ADCw:
            case SBBw:
            case ANDw:
            case XORw:
            case SUBw:
            case CMPw:
            case INCw:
            case DECw:
            case TESTw:
            case SHLw:
            case SHRw:
            case SARw:
            case DSHLw:
            case DSHRw:
            case NEGw:
                return (Flags.getLzFresw() == 0);
            case ADDd:
            case ORd:
            case ADCd:
            case SBBd:
            case ANDd:
            case XORd:
            case SUBd:
            case CMPd:
            case INCd:
            case DECd:
            case TESTd:
            case SHLd:
            case SHRd:
            case SARd:
            case DSHLd:
            case DSHRd:
            case NEGd:
                return (Flags.getLzFResd() == 0);
            case DIV:
            case MUL:
                return false; /* Unkown */
            default:
                Log.logging(Log.LogTypes.CPU, Log.LogServerities.Error, "geLazyt_ZF Unknown %d",
                        LzFlags.Type.toValue());
                break;
        }
        return false;
    }

    /*
     * SF Sign Flag -- Set equal to high-order bit of result (0 is positive, 1 if negative).
     */
    public static boolean getSF() {
        Flags.TypeFlag type = LzFlags.Type;
        switch (type) {
            case UNKNOWN:
                return Register.getFlag(Register.FlagSF) != 0;
            case ADDb:
            case ORb:
            case ADCb:
            case SBBb:
            case ANDb:
            case XORb:
            case SUBb:
            case CMPb:
            case INCb:
            case DECb:
            case TESTb:
            case SHLb:
            case SHRb:
            case SARb:
            case NEGb:
                return (Flags.getLzFResb() & 0x80) != 0;
            case ADDw:
            case ORw:
            case ADCw:
            case SBBw:
            case ANDw:
            case XORw:
            case SUBw:
            case CMPw:
            case INCw:
            case DECw:
            case TESTw:
            case SHLw:
            case SHRw:
            case SARw:
            case DSHLw:
            case DSHRw:
            case NEGw:
                return (Flags.getLzFresw() & 0x8000) != 0;
            case ADDd:
            case ORd:
            case ADCd:
            case SBBd:
            case ANDd:
            case XORd:
            case SUBd:
            case CMPd:
            case INCd:
            case DECd:
            case TESTd:
            case SHLd:
            case SHRd:
            case SARd:
            case DSHLd:
            case DSHRd:
            case NEGd:
                return (Flags.getLzFResd() & 0x80000000) != 0;
            case DIV:
            case MUL:
                return false; /* Unkown */
            default:
                Log.logging(Log.LogTypes.CPU, Log.LogServerities.Error, "geLazyt_SF Unkown %d",
                        LzFlags.Type.toValue());
                break;
        }
        return false;

    }

    public static boolean getOF() {
        Flags.TypeFlag type = LzFlags.Type;
        switch (type) {
            case UNKNOWN:
            case MUL:
                return Register.getFlag(Register.FlagOF) != 0;
            case ADDb:
            case ADCb:
                return (((Flags.getLzFVar1b() ^ Flags.getLzFVar2b() ^ 0x80)
                        & (Flags.getLzFResb() ^ Flags.getLzFVar2b())) & 0x80) != 0;
            case ADDw:
            case ADCw:
                return (((Flags.getLzFVar1w() ^ Flags.getLzFvar2w() ^ 0x8000)
                        & (Flags.getLzFresw() ^ Flags.getLzFvar2w())) & 0x8000) != 0;
            case ADDd:
            case ADCd:
                return (((Flags.getLzFVar1d() ^ Flags.getLzFVar2d() ^ 0x80000000)
                        & (Flags.getLzFResd() ^ Flags.getLzFVar2d())) & 0x80000000) != 0;
            case SBBb:
            case SUBb:
            case CMPb:
                return (((Flags.getLzFVar1b() ^ Flags.getLzFVar2b())
                        & (Flags.getLzFVar1b() ^ Flags.getLzFResb())) & 0x80) != 0;
            case SBBw:
            case SUBw:
            case CMPw:
                return (((Flags.getLzFVar1w() ^ Flags.getLzFvar2w())
                        & (Flags.getLzFVar1w() ^ Flags.getLzFresw())) & 0x8000) != 0;
            case SBBd:
            case SUBd:
            case CMPd:
                return (((Flags.getLzFVar1d() ^ Flags.getLzFVar2d())
                        & (Flags.getLzFVar1d() ^ Flags.getLzFResd())) & 0x80000000) != 0;
            case INCb:
                return (Flags.getLzFResb() == 0x80);
            case INCw:
                return (Flags.getLzFresw() == 0x8000);
            case INCd:
                return (Flags.getLzFResd() == 0x80000000);
            case DECb:
                return (Flags.getLzFResb() == 0x7f);
            case DECw:
                return (Flags.getLzFresw() == 0x7fff);
            case DECd:
                return (Flags.getLzFResd() == 0x7fffffff);
            case NEGb:
                return (Flags.getLzFVar1b() == 0x80);
            case NEGw:
                return (Flags.getLzFVar1w() == 0x8000);
            case NEGd:
                return (Flags.getLzFVar1d() == 0x80000000);
            case SHLb:
                return ((Flags.getLzFResb() ^ Flags.getLzFVar1b()) & 0x80) != 0;
            case SHLw:
            case DSHRw:
            case DSHLw:
                return ((Flags.getLzFresw() ^ Flags.getLzFVar1w()) & 0x8000) != 0;
            case SHLd:
            case DSHRd:
            case DSHLd:
                return ((Flags.getLzFResd() ^ Flags.getLzFVar1d()) & 0x80000000) != 0;
            case SHRb:
                if ((Flags.getLzFVar2b() & 0x1f) == 1)
                    return (Flags.getLzFVar1b() > 0x80);
                else
                    return false;
            case SHRw:
                if ((Flags.getLzFVar2b() & 0x1f) == 1)
                    return (Flags.getLzFVar1w() > 0x8000);
                else
                    return false;
            case SHRd:
                if ((Flags.getLzFVar2b() & 0x1f) == 1)
                    return ((0xffffffffL & Flags.getLzFVar1d()) > 0x80000000L);
                else
                    return false;
            case ORb:
            case ORw:
            case ORd:
            case ANDb:
            case ANDw:
            case ANDd:
            case XORb:
            case XORw:
            case XORd:
            case TESTb:
            case TESTw:
            case TESTd:
            case SARb:
            case SARw:
            case SARd:
                return false; /* Return false */
            case DIV:
                return false; /* Unkown */
            default:
                Log.logging(Log.LogTypes.CPU, Log.LogServerities.Error, "get_OF Unkown %d",
                        LzFlags.Type.toValue());
                break;
        }
        return false;
    }

    // 256
    public static int[] ParityLookup = new int[] {Register.FlagPF, 0, 0, Register.FlagPF, 0,
            Register.FlagPF, Register.FlagPF, 0, 0, Register.FlagPF, Register.FlagPF, 0,
            Register.FlagPF, 0, 0, Register.FlagPF, 0, Register.FlagPF, Register.FlagPF, 0,
            Register.FlagPF, 0, 0, Register.FlagPF, Register.FlagPF, 0, 0, Register.FlagPF, 0,
            Register.FlagPF, Register.FlagPF, 0, 0, Register.FlagPF, Register.FlagPF, 0,
            Register.FlagPF, 0, 0, Register.FlagPF, Register.FlagPF, 0, 0, Register.FlagPF, 0,
            Register.FlagPF, Register.FlagPF, 0, Register.FlagPF, 0, 0, Register.FlagPF, 0,
            Register.FlagPF, Register.FlagPF, 0, 0, Register.FlagPF, Register.FlagPF, 0,
            Register.FlagPF, 0, 0, Register.FlagPF, 0, Register.FlagPF, Register.FlagPF, 0,
            Register.FlagPF, 0, 0, Register.FlagPF, Register.FlagPF, 0, 0, Register.FlagPF, 0,
            Register.FlagPF, Register.FlagPF, 0, Register.FlagPF, 0, 0, Register.FlagPF, 0,
            Register.FlagPF, Register.FlagPF, 0, 0, Register.FlagPF, Register.FlagPF, 0,
            Register.FlagPF, 0, 0, Register.FlagPF, Register.FlagPF, 0, 0, Register.FlagPF, 0,
            Register.FlagPF, Register.FlagPF, 0, 0, Register.FlagPF, Register.FlagPF, 0,
            Register.FlagPF, 0, 0, Register.FlagPF, 0, Register.FlagPF, Register.FlagPF, 0,
            Register.FlagPF, 0, 0, Register.FlagPF, Register.FlagPF, 0, 0, Register.FlagPF, 0,
            Register.FlagPF, Register.FlagPF, 0, 0, Register.FlagPF, Register.FlagPF, 0,
            Register.FlagPF, 0, 0, Register.FlagPF, Register.FlagPF, 0, 0, Register.FlagPF, 0,
            Register.FlagPF, Register.FlagPF, 0, Register.FlagPF, 0, 0, Register.FlagPF, 0,
            Register.FlagPF, Register.FlagPF, 0, 0, Register.FlagPF, Register.FlagPF, 0,
            Register.FlagPF, 0, 0, Register.FlagPF, Register.FlagPF, 0, 0, Register.FlagPF, 0,
            Register.FlagPF, Register.FlagPF, 0, 0, Register.FlagPF, Register.FlagPF, 0,
            Register.FlagPF, 0, 0, Register.FlagPF, 0, Register.FlagPF, Register.FlagPF, 0,
            Register.FlagPF, 0, 0, Register.FlagPF, Register.FlagPF, 0, 0, Register.FlagPF, 0,
            Register.FlagPF, Register.FlagPF, 0, Register.FlagPF, 0, 0, Register.FlagPF, 0,
            Register.FlagPF, Register.FlagPF, 0, 0, Register.FlagPF, Register.FlagPF, 0,
            Register.FlagPF, 0, 0, Register.FlagPF, 0, Register.FlagPF, Register.FlagPF, 0,
            Register.FlagPF, 0, 0, Register.FlagPF, Register.FlagPF, 0, 0, Register.FlagPF, 0,
            Register.FlagPF, Register.FlagPF, 0, 0, Register.FlagPF, Register.FlagPF, 0,
            Register.FlagPF, 0, 0, Register.FlagPF, Register.FlagPF, 0, 0, Register.FlagPF, 0,
            Register.FlagPF, Register.FlagPF, 0, Register.FlagPF, 0, 0, Register.FlagPF, 0,
            Register.FlagPF, Register.FlagPF, 0, 0, Register.FlagPF, Register.FlagPF, 0,
            Register.FlagPF, 0, 0, Register.FlagPF};

    public static boolean getPF() {
        switch (LzFlags.Type) {
            case UNKNOWN:
                return Register.getFlag(Register.FlagPF) != 0;
            default:
                return (ParityLookup[Flags.getLzFResb()]) != 0;
        }
        // return false;
    }

    private static void doFlagPF() {
        Register.Flags = (Register.Flags & ~Register.FlagPF) | ParityLookup[Flags.getLzFResb()];
    }

    private static void doFlagAF() {
        Register.Flags = (Register.Flags & ~Register.FlagAF)
                | (((Flags.getLzFVar1b() ^ Flags.getLzFVar2b()) ^ Flags.getLzFResb()) & 0x10);
    }

    private static void doFlagZFb() {
        Register.setFlagBit(Register.FlagZF, Flags.getLzFResb() == 0);
    }

    private static void doFlagZFw() {
        Register.setFlagBit(Register.FlagZF, Flags.getLzFresw() == 0);
    }

    private static void doFlagZFd() {
        Register.setFlagBit(Register.FlagZF, Flags.getLzFResd() == 0);
    }

    private static void doFlagSFb() {
        Register.Flags = (Register.Flags & ~Register.FlagSF) | ((Flags.getLzFResb() & 0x80) >>> 0);
    }

    private static void doFlagSFw() {
        Register.Flags =
                (Register.Flags & ~Register.FlagSF) | ((Flags.getLzFresw() & 0x8000) >>> 8);
    }

    private static void doFlagSFd() {
        Register.Flags =
                (Register.Flags & ~Register.FlagSF) | ((Flags.getLzFResd() & 0x80000000) >>> 24);
    }

    private static void setCF(int NEWBIT) {
        Register.Flags = (Register.Flags & ~Register.FlagCF) | (NEWBIT);
    }

    private static void setFlag(int TYPE, boolean TEST) {
        Register.setFlagBit(TYPE, TEST);
    }

    private static void setFlag(int TYPE, int TEST) {
        Register.setFlagBit(TYPE, TEST);
    }

    private static void setFlag(int TYPE, long TEST) {
        setFlag(TYPE, (int) TEST);
    }

    public static int fillFlags() {
        switch (LzFlags.Type) {
            case UNKNOWN:
                break;
            case ADDb:
                setFlag(Register.FlagCF, (Flags.getLzFResb() < Flags.getLzFVar1b()));
                doFlagAF();
                doFlagZFb();
                doFlagSFb();
                setFlag(Register.FlagOF, ((Flags.getLzFVar1b() ^ Flags.getLzFVar2b() ^ 0x80)
                        & (Flags.getLzFResb() ^ Flags.getLzFVar1b())) & 0x80);
                doFlagPF();
                break;
            case ADDw:
                setFlag(Register.FlagCF, (Flags.getLzFresw() < Flags.getLzFVar1w()));
                doFlagAF();
                doFlagZFw();
                doFlagSFw();
                setFlag(Register.FlagOF, ((Flags.getLzFVar1w() ^ Flags.getLzFvar2w() ^ 0x8000)
                        & (Flags.getLzFresw() ^ Flags.getLzFVar1w())) & 0x8000);
                doFlagPF();
                break;
            case ADDd:
                setFlag(Register.FlagCF,
                        ((0xffffffffL & Flags.getLzFResd()) < (0xffffffffL & Flags.getLzFVar1d())));
                doFlagAF();
                doFlagZFd();
                doFlagSFd();
                setFlag(Register.FlagOF, ((Flags.getLzFVar1d() ^ Flags.getLzFVar2d() ^ 0x80000000)
                        & (Flags.getLzFResd() ^ Flags.getLzFVar1d())) & 0x80000000);
                doFlagPF();
                break;
            case ADCb:
                setFlag(Register.FlagCF, (Flags.getLzFResb() < Flags.getLzFVar1b())
                        || (LzFlags.oldCF != 0 && (Flags.getLzFResb() == Flags.getLzFVar1b())));
                doFlagAF();
                doFlagZFb();
                doFlagSFb();
                setFlag(Register.FlagOF, ((Flags.getLzFVar1b() ^ Flags.getLzFVar2b() ^ 0x80)
                        & (Flags.getLzFResb() ^ Flags.getLzFVar1b())) & 0x80);
                doFlagPF();
                break;
            case ADCw:
                setFlag(Register.FlagCF, (Flags.getLzFresw() < Flags.getLzFVar1w())
                        || (LzFlags.oldCF != 0 && (Flags.getLzFresw() == Flags.getLzFVar1w())));
                doFlagAF();
                doFlagZFw();
                doFlagSFw();
                setFlag(Register.FlagOF, ((Flags.getLzFVar1w() ^ Flags.getLzFvar2w() ^ 0x8000)
                        & (Flags.getLzFresw() ^ Flags.getLzFVar1w())) & 0x8000);
                doFlagPF();
                break;
            case ADCd:
                setFlag(Register.FlagCF,
                        ((0xffffffffL & Flags.getLzFResd()) < (0xffffffffL & Flags.getLzFVar1d()))
                                || (LzFlags.oldCF != 0
                                        && (Flags.getLzFResd() == Flags.getLzFVar1d())));
                doFlagAF();
                doFlagZFd();
                doFlagSFd();
                setFlag(Register.FlagOF, ((Flags.getLzFVar1d() ^ Flags.getLzFVar2d() ^ 0x80000000)
                        & (Flags.getLzFResd() ^ Flags.getLzFVar1d())) & 0x80000000);
                doFlagPF();
                break;

            case SBBb:
                setFlag(Register.FlagCF, (Flags.getLzFVar1b() < Flags.getLzFResb())
                        || (LzFlags.oldCF != 0 && (Flags.getLzFVar2b() == 0xff)));
                doFlagAF();
                doFlagZFb();
                doFlagSFb();
                setFlag(Register.FlagOF, ((Flags.getLzFVar1b() ^ Flags.getLzFVar2b())
                        & (Flags.getLzFVar1b() ^ Flags.getLzFResb()) & 0x80));
                doFlagPF();
                break;
            case SBBw:
                setFlag(Register.FlagCF, (Flags.getLzFVar1w() < Flags.getLzFresw())
                        || (LzFlags.oldCF != 0 && (Flags.getLzFvar2w() == 0xffff)));
                doFlagAF();
                doFlagZFw();
                doFlagSFw();
                setFlag(Register.FlagOF, ((Flags.getLzFVar1w() ^ Flags.getLzFvar2w())
                        & (Flags.getLzFVar1w() ^ Flags.getLzFresw()) & 0x8000));
                doFlagPF();
                break;
            case SBBd:
                setFlag(Register.FlagCF,
                        ((0xffffffffL & Flags.getLzFVar1d()) < (0xffffffffL & Flags.getLzFResd()))
                                || (LzFlags.oldCF != 0 && (Flags.getLzFVar2d() == 0xffffffff)));
                doFlagAF();
                doFlagZFd();
                doFlagSFd();
                setFlag(Register.FlagOF, (Flags.getLzFVar1d() ^ Flags.getLzFVar2d())
                        & (Flags.getLzFVar1d() ^ Flags.getLzFResd()) & 0x80000000);
                doFlagPF();
                break;

            case SUBb:
            case CMPb:
                setFlag(Register.FlagCF, (Flags.getLzFVar1b() < Flags.getLzFVar2b()));
                doFlagAF();
                doFlagZFb();
                doFlagSFb();
                setFlag(Register.FlagOF, (Flags.getLzFVar1b() ^ Flags.getLzFVar2b())
                        & (Flags.getLzFVar1b() ^ Flags.getLzFResb()) & 0x80);
                doFlagPF();
                break;
            case SUBw:
            case CMPw:
                setFlag(Register.FlagCF, (Flags.getLzFVar1w() < Flags.getLzFvar2w()));
                doFlagAF();
                doFlagZFw();
                doFlagSFw();
                setFlag(Register.FlagOF, ((Flags.getLzFVar1w() ^ Flags.getLzFvar2w())
                        & (Flags.getLzFVar1w() ^ Flags.getLzFresw()) & 0x8000));
                doFlagPF();
                break;
            case SUBd:
            case CMPd:
                setFlag(Register.FlagCF, ((0xffffffffL & Flags.getLzFVar1d()) < (0xffffffffL
                        & Flags.getLzFVar2d())));
                doFlagAF();
                doFlagZFd();
                doFlagSFd();
                setFlag(Register.FlagOF, (Flags.getLzFVar1d() ^ Flags.getLzFVar2d())
                        & (Flags.getLzFVar1d() ^ Flags.getLzFResd()) & 0x80000000);
                doFlagPF();
                break;

            case ORb:
                setFlag(Register.FlagCF, false);
                setFlag(Register.FlagAF, false);
                doFlagZFb();
                doFlagSFb();
                setFlag(Register.FlagOF, false);
                doFlagPF();
                break;
            case ORw:
                setFlag(Register.FlagCF, false);
                setFlag(Register.FlagAF, false);
                doFlagZFw();
                doFlagSFw();
                setFlag(Register.FlagOF, false);
                doFlagPF();
                break;
            case ORd:
                setFlag(Register.FlagCF, false);
                setFlag(Register.FlagAF, false);
                doFlagZFd();
                doFlagSFd();
                setFlag(Register.FlagOF, false);
                doFlagPF();
                break;

            case TESTb:
            case ANDb:
                setFlag(Register.FlagCF, false);
                setFlag(Register.FlagAF, false);
                doFlagZFb();
                doFlagSFb();
                setFlag(Register.FlagOF, false);
                doFlagPF();
                break;
            case TESTw:
            case ANDw:
                setFlag(Register.FlagCF, false);
                setFlag(Register.FlagAF, false);
                doFlagZFw();
                doFlagSFw();
                setFlag(Register.FlagOF, false);
                doFlagPF();
                break;
            case TESTd:
            case ANDd:
                setFlag(Register.FlagCF, false);
                setFlag(Register.FlagAF, false);
                doFlagZFd();
                doFlagSFd();
                setFlag(Register.FlagOF, false);
                doFlagPF();
                break;

            case XORb:
                setFlag(Register.FlagCF, false);
                setFlag(Register.FlagAF, false);
                doFlagZFb();
                doFlagSFb();
                setFlag(Register.FlagOF, false);
                doFlagPF();
                break;
            case XORw:
                setFlag(Register.FlagCF, false);
                setFlag(Register.FlagAF, false);
                doFlagZFw();
                doFlagSFw();
                setFlag(Register.FlagOF, false);
                doFlagPF();
                break;
            case XORd:
                setFlag(Register.FlagCF, false);
                setFlag(Register.FlagAF, false);
                doFlagZFd();
                doFlagSFd();
                setFlag(Register.FlagOF, false);
                doFlagPF();
                break;

            case SHLb:
                if (Flags.getLzFVar2b() > 8)
                    setFlag(Register.FlagCF, false);
                else
                    setFlag(Register.FlagCF,
                            (Flags.getLzFVar1b() >>> (8 - Flags.getLzFVar2b())) & 1);
                doFlagZFb();
                doFlagSFb();
                setFlag(Register.FlagOF, (Flags.getLzFResb() ^ Flags.getLzFVar1b()) & 0x80);
                doFlagPF();
                setFlag(Register.FlagAF, (Flags.getLzFVar2b() & 0x1f));
                break;
            case SHLw:
                if (Flags.getLzFVar2b() > 16)
                    setFlag(Register.FlagCF, false);
                else
                    setFlag(Register.FlagCF,
                            (Flags.getLzFVar1w() >>> (16 - Flags.getLzFVar2b())) & 1);
                doFlagZFw();
                doFlagSFw();
                setFlag(Register.FlagOF, (Flags.getLzFresw() ^ Flags.getLzFVar1w()) & 0x8000);
                doFlagPF();
                setFlag(Register.FlagAF, (Flags.getLzFvar2w() & 0x1f));
                break;
            case SHLd:
                setFlag(Register.FlagCF, (Flags.getLzFVar1d() >>> (32 - Flags.getLzFVar2b())) & 1);
                doFlagZFd();
                doFlagSFd();
                setFlag(Register.FlagOF, (Flags.getLzFResd() ^ Flags.getLzFVar1d()) & 0x80000000);
                doFlagPF();
                setFlag(Register.FlagAF, (Flags.getLzFVar2d() & 0x1f));
                break;

            case DSHLw:
                setFlag(Register.FlagCF, (Flags.getLzFVar1d() >>> (32 - Flags.getLzFVar2b())) & 1);
                doFlagZFw();
                doFlagSFw();
                setFlag(Register.FlagOF, (Flags.getLzFresw() ^ Flags.getLzFVar1w()) & 0x8000);
                doFlagPF();
                break;
            case DSHLd:
                setFlag(Register.FlagCF, (Flags.getLzFVar1d() >>> (32 - Flags.getLzFVar2b())) & 1);
                doFlagZFd();
                doFlagSFd();
                setFlag(Register.FlagOF, (Flags.getLzFResd() ^ Flags.getLzFVar1d()) & 0x80000000);
                doFlagPF();
                break;

            case SHRb:
                setFlag(Register.FlagCF, (Flags.getLzFVar1b() >>> (Flags.getLzFVar2b() - 1)) & 1);
                doFlagZFb();
                doFlagSFb();
                if ((Flags.getLzFVar2b() & 0x1f) == 1)
                    setFlag(Register.FlagOF, (Flags.getLzFVar1b() >= 0x80));
                else
                    setFlag(Register.FlagOF, false);
                doFlagPF();
                setFlag(Register.FlagAF, (Flags.getLzFVar2b() & 0x1f));
                break;
            case SHRw:
                setFlag(Register.FlagCF, (Flags.getLzFVar1w() >>> (Flags.getLzFVar2b() - 1)) & 1);
                doFlagZFw();
                doFlagSFw();
                if ((Flags.getLzFvar2w() & 0x1f) == 1)
                    setFlag(Register.FlagOF, (Flags.getLzFVar1w() >= 0x8000));
                else
                    setFlag(Register.FlagOF, false);
                doFlagPF();
                setFlag(Register.FlagAF, Flags.getLzFvar2w() & 0x1f);
                break;
            case SHRd:
                setFlag(Register.FlagCF, (Flags.getLzFVar1d() >>> (Flags.getLzFVar2b() - 1)) & 1);
                doFlagZFd();
                doFlagSFd();
                if ((Flags.getLzFVar2d() & 0x1f) == 1)
                    setFlag(Register.FlagOF, (Flags.getLzFVar1d() >= 0x80000000));
                else
                    setFlag(Register.FlagOF, false);
                doFlagPF();
                setFlag(Register.FlagAF, (Flags.getLzFVar2d() & 0x1f));
                break;

            case DSHRw: /* Hmm this is not correct for shift higher than 16 */
                setFlag(Register.FlagCF, (Flags.getLzFVar1d() >>> (Flags.getLzFVar2b() - 1)) & 1);
                doFlagZFw();
                doFlagSFw();
                setFlag(Register.FlagOF, (Flags.getLzFresw() ^ Flags.getLzFVar1w()) & 0x8000);
                doFlagPF();
                break;
            case DSHRd:
                setFlag(Register.FlagCF, (Flags.getLzFVar1d() >>> (Flags.getLzFVar2b() - 1)) & 1);
                doFlagZFd();
                doFlagSFd();
                setFlag(Register.FlagOF, (Flags.getLzFResd() ^ Flags.getLzFVar1d()) & 0x80000000);
                doFlagPF();
                break;

            case SARb:
                setFlag(Register.FlagCF, (Flags.getLzFVar1b() >>> (Flags.getLzFVar2b() - 1)) & 1);
                doFlagZFb();
                doFlagSFb();
                setFlag(Register.FlagOF, false);
                doFlagPF();
                setFlag(Register.FlagAF, Flags.getLzFVar2b() & 0x1f);
                break;
            case SARw:
                setFlag(Register.FlagCF, (Flags.getLzFVar1w() >>> (Flags.getLzFVar2b() - 1)) & 1);
                doFlagZFw();
                doFlagSFw();
                setFlag(Register.FlagOF, false);
                doFlagPF();
                setFlag(Register.FlagAF, Flags.getLzFvar2w() & 0x1f);
                break;
            case SARd:
                setFlag(Register.FlagCF, (Flags.getLzFVar1d() >>> (Flags.getLzFVar2b() - 1)) & 1);
                doFlagZFd();
                doFlagSFd();
                setFlag(Register.FlagOF, false);
                doFlagPF();
                setFlag(Register.FlagAF, (Flags.getLzFVar2d() & 0x1f));
                break;

            case INCb:
                setFlag(Register.FlagAF, (Flags.getLzFResb() & 0x0f) == 0);
                doFlagZFb();
                doFlagSFb();
                setFlag(Register.FlagOF, (Flags.getLzFResb() == 0x80));
                doFlagPF();
                break;
            case INCw:
                setFlag(Register.FlagAF, (Flags.getLzFresw() & 0x0f) == 0);
                doFlagZFw();
                doFlagSFw();
                setFlag(Register.FlagOF, (Flags.getLzFresw() == 0x8000));
                doFlagPF();
                break;
            case INCd:
                setFlag(Register.FlagAF, (Flags.getLzFResd() & 0x0f) == 0);
                doFlagZFd();
                doFlagSFd();
                setFlag(Register.FlagOF, (Flags.getLzFResd() == 0x80000000));
                doFlagPF();
                break;

            case DECb:
                setFlag(Register.FlagAF, (Flags.getLzFResb() & 0x0f) == 0x0f);
                doFlagZFb();
                doFlagSFb();
                setFlag(Register.FlagOF, (Flags.getLzFResb() == 0x7f));
                doFlagPF();
                break;
            case DECw:
                setFlag(Register.FlagAF, (Flags.getLzFresw() & 0x0f) == 0x0f);
                doFlagZFw();
                doFlagSFw();
                setFlag(Register.FlagOF, (Flags.getLzFresw() == 0x7fff));
                doFlagPF();
                break;
            case DECd:
                setFlag(Register.FlagAF, (Flags.getLzFResd() & 0x0f) == 0x0f);
                doFlagZFd();
                doFlagSFd();
                setFlag(Register.FlagOF, (Flags.getLzFResd() == 0x7fffffff));
                doFlagPF();
                break;

            case NEGb:
                setFlag(Register.FlagCF, (Flags.getLzFVar1b() != 0));
                setFlag(Register.FlagAF, (Flags.getLzFResb() & 0x0f) != 0);
                doFlagZFb();
                doFlagSFb();
                setFlag(Register.FlagOF, (Flags.getLzFVar1b() == 0x80));
                doFlagPF();
                break;
            case NEGw:
                setFlag(Register.FlagCF, (Flags.getLzFVar1w() != 0));
                setFlag(Register.FlagAF, (Flags.getLzFresw() & 0x0f) != 0);
                doFlagZFw();
                doFlagSFw();
                setFlag(Register.FlagOF, (Flags.getLzFVar1w() == 0x8000));
                doFlagPF();
                break;
            case NEGd:
                setFlag(Register.FlagCF, (Flags.getLzFVar1d() != 0));
                setFlag(Register.FlagAF, (Flags.getLzFResd() & 0x0f) != 0);
                doFlagZFd();
                doFlagSFd();
                setFlag(Register.FlagOF, (Flags.getLzFVar1d() == 0x80000000));
                doFlagPF();
                break;

            case DIV:
            case MUL:
                break;

            default:
                Log.logging(Log.LogTypes.CPU, Log.LogServerities.Error, "Unhandled flag type %d",
                        LzFlags.Type.toValue());
                return 0;
        }
        LzFlags.Type = Flags.TypeFlag.UNKNOWN;
        return Register.Flags;
    }

    public static void fillFlagsNoCFOF() {
        switch (LzFlags.Type) {
            case UNKNOWN:
                return;
            case ADDb:
                doFlagAF();
                doFlagZFb();
                doFlagSFb();
                doFlagPF();
                break;
            case ADDw:
                doFlagAF();
                doFlagZFw();
                doFlagSFw();
                doFlagPF();
                break;
            case ADDd:
                doFlagAF();
                doFlagZFd();
                doFlagSFd();
                doFlagPF();
                break;
            case ADCb:
                doFlagAF();
                doFlagZFb();
                doFlagSFb();
                doFlagPF();
                break;
            case ADCw:
                doFlagAF();
                doFlagZFw();
                doFlagSFw();
                doFlagPF();
                break;
            case ADCd:
                doFlagAF();
                doFlagZFd();
                doFlagSFd();
                doFlagPF();
                break;

            case SBBb:
                doFlagAF();
                doFlagZFb();
                doFlagSFb();
                doFlagPF();
                break;
            case SBBw:
                doFlagAF();
                doFlagZFw();
                doFlagSFw();
                doFlagPF();
                break;
            case SBBd:
                doFlagAF();
                doFlagZFd();
                doFlagSFd();
                doFlagPF();
                break;

            case SUBb:
            case CMPb:
                doFlagAF();
                doFlagZFb();
                doFlagSFb();
                doFlagPF();
                break;
            case SUBw:
            case CMPw:
                doFlagAF();
                doFlagZFw();
                doFlagSFw();
                doFlagPF();
                break;
            case SUBd:
            case CMPd:
                doFlagAF();
                doFlagZFd();
                doFlagSFd();
                doFlagPF();
                break;

            case ORb:
                setFlag(Register.FlagAF, false);
                doFlagZFb();
                doFlagSFb();
                doFlagPF();
                break;
            case ORw:
                setFlag(Register.FlagAF, false);
                doFlagZFw();
                doFlagSFw();
                doFlagPF();
                break;
            case ORd:
                setFlag(Register.FlagAF, false);
                doFlagZFd();
                doFlagSFd();
                doFlagPF();
                break;

            case TESTb:
            case ANDb:
                setFlag(Register.FlagAF, false);
                doFlagZFb();
                doFlagSFb();
                doFlagPF();
                break;
            case TESTw:
            case ANDw:
                setFlag(Register.FlagAF, false);
                doFlagZFw();
                doFlagSFw();
                doFlagPF();
                break;
            case TESTd:
            case ANDd:
                setFlag(Register.FlagAF, false);
                doFlagZFd();
                doFlagSFd();
                doFlagPF();
                break;

            case XORb:
                setFlag(Register.FlagAF, false);
                doFlagZFb();
                doFlagSFb();
                doFlagPF();
                break;
            case XORw:
                setFlag(Register.FlagAF, false);
                doFlagZFw();
                doFlagSFw();
                doFlagPF();
                break;
            case XORd:
                setFlag(Register.FlagAF, false);
                doFlagZFd();
                doFlagSFd();
                doFlagPF();
                break;

            case SHLb:
                doFlagZFb();
                doFlagSFb();
                doFlagPF();
                setFlag(Register.FlagAF, (Flags.getLzFVar2b() & 0x1f));
                break;
            case SHLw:
                doFlagZFw();
                doFlagSFw();
                doFlagPF();
                setFlag(Register.FlagAF, (Flags.getLzFvar2w() & 0x1f));
                break;
            case SHLd:
                doFlagZFd();
                doFlagSFd();
                doFlagPF();
                setFlag(Register.FlagAF, (Flags.getLzFVar2d() & 0x1f));
                break;

            case DSHLw:
                doFlagZFw();
                doFlagSFw();
                doFlagPF();
                break;
            case DSHLd:
                doFlagZFd();
                doFlagSFd();
                doFlagPF();
                break;

            case SHRb:
                doFlagZFb();
                doFlagSFb();
                doFlagPF();
                setFlag(Register.FlagAF, Flags.getLzFVar2b() & 0x1f);
                break;
            case SHRw:
                doFlagZFw();
                doFlagSFw();
                doFlagPF();
                setFlag(Register.FlagAF, Flags.getLzFvar2w() & 0x1f);
                break;
            case SHRd:
                doFlagZFd();
                doFlagSFd();
                doFlagPF();
                setFlag(Register.FlagAF, (Flags.getLzFVar2d() & 0x1f));
                break;

            case DSHRw: /* Hmm this is not correct for shift higher than 16 */
                doFlagZFw();
                doFlagSFw();
                doFlagPF();
                break;
            case DSHRd:
                doFlagZFd();
                doFlagSFd();
                doFlagPF();
                break;

            case SARb:
                doFlagZFb();
                doFlagSFb();
                doFlagPF();
                setFlag(Register.FlagAF, Flags.getLzFVar2b() & 0x1f);
                break;
            case SARw:
                doFlagZFw();
                doFlagSFw();
                doFlagPF();
                setFlag(Register.FlagAF, Flags.getLzFvar2w() & 0x1f);
                break;
            case SARd:
                doFlagZFd();
                doFlagSFd();
                doFlagPF();
                setFlag(Register.FlagAF, (Flags.getLzFVar2d() & 0x1f));
                break;

            case INCb:
                setFlag(Register.FlagAF, (Flags.getLzFResb() & 0x0f) == 0);
                doFlagZFb();
                doFlagSFb();
                doFlagPF();
                break;
            case INCw:
                setFlag(Register.FlagAF, (Flags.getLzFresw() & 0x0f) == 0);
                doFlagZFw();
                doFlagSFw();
                doFlagPF();
                break;
            case INCd:
                setFlag(Register.FlagAF, (Flags.getLzFResd() & 0x0f) == 0);
                doFlagZFd();
                doFlagSFd();
                doFlagPF();
                break;

            case DECb:
                setFlag(Register.FlagAF, (Flags.getLzFResb() & 0x0f) == 0x0f);
                doFlagZFb();
                doFlagSFb();
                doFlagPF();
                break;
            case DECw:
                setFlag(Register.FlagAF, (Flags.getLzFresw() & 0x0f) == 0x0f);
                doFlagZFw();
                doFlagSFw();
                doFlagPF();
                break;
            case DECd:
                setFlag(Register.FlagAF, (Flags.getLzFResd() & 0x0f) == 0x0f);
                doFlagZFd();
                doFlagSFd();
                doFlagPF();
                break;

            case NEGb:
                setFlag(Register.FlagAF, (Flags.getLzFResb() & 0x0f) != 0);
                doFlagZFb();
                doFlagSFb();
                doFlagPF();
                break;
            case NEGw:
                setFlag(Register.FlagAF, (Flags.getLzFresw() & 0x0f) != 0);
                doFlagZFw();
                doFlagSFw();
                doFlagPF();
                break;
            case NEGd:
                setFlag(Register.FlagAF, (Flags.getLzFResd() & 0x0f) != 0);
                doFlagZFd();
                doFlagSFd();
                doFlagPF();
                break;

            case DIV:
            case MUL:
                break;

            default:
                Log.logging(Log.LogTypes.CPU, Log.LogServerities.Error, "Unhandled flag type %d",
                        LzFlags.Type.toValue());
                break;
        }
        LzFlags.Type = Flags.TypeFlag.UNKNOWN;
    }

    public static void destroyConditionFlags() {
        LzFlags.Type = Flags.TypeFlag.UNKNOWN;
    }

}
