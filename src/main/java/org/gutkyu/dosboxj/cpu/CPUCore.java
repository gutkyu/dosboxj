package org.gutkyu.dosboxj.cpu;

import org.gutkyu.dosboxj.util.*;
import org.gutkyu.dosboxj.hardware.*;
import org.gutkyu.dosboxj.hardware.io.*;
import org.gutkyu.dosboxj.hardware.memory.*;
import org.gutkyu.dosboxj.hardware.memory.paging.*;
import org.gutkyu.dosboxj.misc.*;
import org.gutkyu.dosboxj.cpu.Register.*;


public abstract class CPUCore {
    protected CPUCore() {
        initEATable();
        CpuDecoder = new CPUDecoder(this::runCPUCore);
        CpuTrapDecoder = new CPUDecoder(this::runCPUCoreTrap);
    }

    public CPUDecoder CpuDecoder = null;
    public CPUDecoder CpuTrapDecoder = null;

    public static final int[] AddrMaskTable = {0x0000ffff, 0xffffffff};


    protected static class Core {
        public static int OPCodeIndex;
        public static int CSEIP;
        public static int BaseDS, BaseSS;
        public static byte BaseValDS; // Register.SegNames
        public static boolean RepZero;
        public static int Prefixes;
        public static EAHandler[] EATable;
        public static int EATableBaseIndex; // 0 or 256
    }

    abstract public String getDecorderName();

    protected int getIP() {
        return (Core.CSEIP - Register.segPhys(Register.SEG_NAME_CS) - Memory.MemBase);
    }

    protected void saveIP() {
        Register.setRegEIP(getIP());
    }

    protected void loadIP() {
        Core.CSEIP = Memory.MemBase + Register.segPhys(Register.SEG_NAME_CS) + Register.getRegEIP();
    }


    // byte
    abstract protected int fetchB();

    // uint16
    abstract protected int fetchW();

    // uint32
    abstract protected int fetchD();

    // -- #region support
    // sbyte
    protected byte fetchBS() {
        return (byte) fetchB();
    }

    // int16
    protected short fetchWS() {
        return (short) fetchW();
    }

    // int32
    protected int fetchDS() {
        return (int) fetchD();
    }

    protected void setCC(boolean cc) {
        int rm = fetchB();
        if (rm >= 0xc0) {
            int earbId = lookupRMEAregbl[rm];
            if (earbId >= 0) {
                Register.Regs[earbId].setByteL(cc ? 1 : 0);
            } else {
                Register.Regs[lookupRMEAregbh[rm]].setByteH(cc ? 1 : 0);
            }
        } else {
            Memory.writeB(Core.EATable[rm].get(), cc ? 1 : 0);
        }
    }
    // -- #endregion

    protected final short OpcodeNone = 0x000;
    protected final short Opcode0F = 0x100;
    protected final short OpcodeSize = 0x200;

    protected final static byte PrefixAddr = 0x1;
    protected final static byte PrefixRep = 0x2;

    public abstract void initCPUCore();

    public int runCPUCoreTrap() {
        int oldCycles = CPU.Cycles;
        CPU.Cycles = 1;
        CPU.Block.TrapSkip = false;

        int ret = runCPUCore();
        if (!CPU.Block.TrapSkip)
            CPU.hwHInterrupt(1);
        CPU.Cycles = oldCycles - 1;
        // cpuModule.cpudecoder = &CPU_Core_Normal_Run;
        CPU.CpuDecoder = CpuDecoder;
        return ret;
    }

    /*--------------------------- begin CpuCoreDefine -----------------------------*/

    public final int CASE_W_0x00 = 0;
    public final int CASE_W_0x01 = 1;
    public final int CASE_W_0x02 = 2;
    public final int CASE_W_0x03 = 3;
    public final int CASE_W_0x04 = 4;
    public final int CASE_W_0x05 = 5;
    public final int CASE_W_0x06 = 6;
    public final int CASE_W_0x07 = 7;
    public final int CASE_W_0x08 = 8;
    public final int CASE_W_0x09 = 9;
    public final int CASE_W_0x0a = 10;
    public final int CASE_W_0x0b = 11;
    public final int CASE_W_0x0c = 12;
    public final int CASE_W_0x0d = 13;
    public final int CASE_W_0x0e = 14;
    public final int CASE_W_0x0f = 15;
    public final int CASE_W_0x10 = 16;
    public final int CASE_W_0x11 = 17;
    public final int CASE_W_0x12 = 18;
    public final int CASE_W_0x13 = 19;
    public final int CASE_W_0x14 = 20;
    public final int CASE_W_0x15 = 21;
    public final int CASE_W_0x16 = 22;
    public final int CASE_W_0x17 = 23;
    public final int CASE_W_0x18 = 24;
    public final int CASE_W_0x19 = 25;
    public final int CASE_W_0x1a = 26;
    public final int CASE_W_0x1b = 27;
    public final int CASE_W_0x1c = 28;
    public final int CASE_W_0x1d = 29;
    public final int CASE_W_0x1e = 30;
    public final int CASE_W_0x1f = 31;
    public final int CASE_W_0x20 = 32;
    public final int CASE_W_0x21 = 33;
    public final int CASE_W_0x22 = 34;
    public final int CASE_W_0x23 = 35;
    public final int CASE_W_0x24 = 36;
    public final int CASE_W_0x25 = 37;
    public final int CASE_W_0x26 = 38;
    public final int CASE_W_0x27 = 39;
    public final int CASE_W_0x28 = 40;
    public final int CASE_W_0x29 = 41;
    public final int CASE_W_0x2a = 42;
    public final int CASE_W_0x2b = 43;
    public final int CASE_W_0x2c = 44;
    public final int CASE_W_0x2d = 45;
    public final int CASE_W_0x2e = 46;
    public final int CASE_W_0x2f = 47;
    public final int CASE_W_0x30 = 48;
    public final int CASE_W_0x31 = 49;
    public final int CASE_W_0x32 = 50;
    public final int CASE_W_0x33 = 51;
    public final int CASE_W_0x34 = 52;
    public final int CASE_W_0x35 = 53;
    public final int CASE_W_0x36 = 54;
    public final int CASE_W_0x37 = 55;
    public final int CASE_W_0x38 = 56;
    public final int CASE_W_0x39 = 57;
    public final int CASE_W_0x3a = 58;
    public final int CASE_W_0x3b = 59;
    public final int CASE_W_0x3c = 60;
    public final int CASE_W_0x3d = 61;
    public final int CASE_W_0x3e = 62;
    public final int CASE_W_0x3f = 63;
    public final int CASE_W_0x40 = 64;
    public final int CASE_W_0x41 = 65;
    public final int CASE_W_0x42 = 66;
    public final int CASE_W_0x43 = 67;
    public final int CASE_W_0x44 = 68;
    public final int CASE_W_0x45 = 69;
    public final int CASE_W_0x46 = 70;
    public final int CASE_W_0x47 = 71;
    public final int CASE_W_0x48 = 72;
    public final int CASE_W_0x49 = 73;
    public final int CASE_W_0x4a = 74;
    public final int CASE_W_0x4b = 75;
    public final int CASE_W_0x4c = 76;
    public final int CASE_W_0x4d = 77;
    public final int CASE_W_0x4e = 78;
    public final int CASE_W_0x4f = 79;
    public final int CASE_W_0x50 = 80;
    public final int CASE_W_0x51 = 81;
    public final int CASE_W_0x52 = 82;
    public final int CASE_W_0x53 = 83;
    public final int CASE_W_0x54 = 84;
    public final int CASE_W_0x55 = 85;
    public final int CASE_W_0x56 = 86;
    public final int CASE_W_0x57 = 87;
    public final int CASE_W_0x58 = 88;
    public final int CASE_W_0x59 = 89;
    public final int CASE_W_0x5a = 90;
    public final int CASE_W_0x5b = 91;
    public final int CASE_W_0x5c = 92;
    public final int CASE_W_0x5d = 93;
    public final int CASE_W_0x5e = 94;
    public final int CASE_W_0x5f = 95;
    public final int CASE_W_0x60 = 96;
    public final int CASE_W_0x61 = 97;
    public final int CASE_W_0x62 = 98;
    public final int CASE_W_0x63 = 99;
    public final int CASE_W_0x64 = 100;
    public final int CASE_W_0x65 = 101;
    public final int CASE_W_0x66 = 102;
    public final int CASE_W_0x67 = 103;
    public final int CASE_W_0x68 = 104;
    public final int CASE_W_0x69 = 105;
    public final int CASE_W_0x6a = 106;
    public final int CASE_W_0x6b = 107;
    public final int CASE_W_0x6c = 108;
    public final int CASE_W_0x6d = 109;
    public final int CASE_W_0x6e = 110;
    public final int CASE_W_0x6f = 111;
    public final int CASE_W_0x70 = 112;
    public final int CASE_W_0x71 = 113;
    public final int CASE_W_0x72 = 114;
    public final int CASE_W_0x73 = 115;
    public final int CASE_W_0x74 = 116;
    public final int CASE_W_0x75 = 117;
    public final int CASE_W_0x76 = 118;
    public final int CASE_W_0x77 = 119;
    public final int CASE_W_0x78 = 120;
    public final int CASE_W_0x79 = 121;
    public final int CASE_W_0x7a = 122;
    public final int CASE_W_0x7b = 123;
    public final int CASE_W_0x7c = 124;
    public final int CASE_W_0x7d = 125;
    public final int CASE_W_0x7e = 126;
    public final int CASE_W_0x7f = 127;
    public final int CASE_W_0x80 = 128;
    public final int CASE_W_0x81 = 129;
    public final int CASE_W_0x82 = 130;
    public final int CASE_W_0x83 = 131;
    public final int CASE_W_0x84 = 132;
    public final int CASE_W_0x85 = 133;
    public final int CASE_W_0x86 = 134;
    public final int CASE_W_0x87 = 135;
    public final int CASE_W_0x88 = 136;
    public final int CASE_W_0x89 = 137;
    public final int CASE_W_0x8a = 138;
    public final int CASE_W_0x8b = 139;
    public final int CASE_W_0x8c = 140;
    public final int CASE_W_0x8d = 141;
    public final int CASE_W_0x8e = 142;
    public final int CASE_W_0x8f = 143;
    public final int CASE_W_0x90 = 144;
    public final int CASE_W_0x91 = 145;
    public final int CASE_W_0x92 = 146;
    public final int CASE_W_0x93 = 147;
    public final int CASE_W_0x94 = 148;
    public final int CASE_W_0x95 = 149;
    public final int CASE_W_0x96 = 150;
    public final int CASE_W_0x97 = 151;
    public final int CASE_W_0x98 = 152;
    public final int CASE_W_0x99 = 153;
    public final int CASE_W_0x9a = 154;
    public final int CASE_W_0x9b = 155;
    public final int CASE_W_0x9c = 156;
    public final int CASE_W_0x9d = 157;
    public final int CASE_W_0x9e = 158;
    public final int CASE_W_0x9f = 159;
    public final int CASE_W_0xa0 = 160;
    public final int CASE_W_0xa1 = 161;
    public final int CASE_W_0xa2 = 162;
    public final int CASE_W_0xa3 = 163;
    public final int CASE_W_0xa4 = 164;
    public final int CASE_W_0xa5 = 165;
    public final int CASE_W_0xa6 = 166;
    public final int CASE_W_0xa7 = 167;
    public final int CASE_W_0xa8 = 168;
    public final int CASE_W_0xa9 = 169;
    public final int CASE_W_0xaa = 170;
    public final int CASE_W_0xab = 171;
    public final int CASE_W_0xac = 172;
    public final int CASE_W_0xad = 173;
    public final int CASE_W_0xae = 174;
    public final int CASE_W_0xaf = 175;
    public final int CASE_W_0xb0 = 176;
    public final int CASE_W_0xb1 = 177;
    public final int CASE_W_0xb2 = 178;
    public final int CASE_W_0xb3 = 179;
    public final int CASE_W_0xb4 = 180;
    public final int CASE_W_0xb5 = 181;
    public final int CASE_W_0xb6 = 182;
    public final int CASE_W_0xb7 = 183;
    public final int CASE_W_0xb8 = 184;
    public final int CASE_W_0xb9 = 185;
    public final int CASE_W_0xba = 186;
    public final int CASE_W_0xbb = 187;
    public final int CASE_W_0xbc = 188;
    public final int CASE_W_0xbd = 189;
    public final int CASE_W_0xbe = 190;
    public final int CASE_W_0xbf = 191;
    public final int CASE_W_0xc0 = 192;
    public final int CASE_W_0xc1 = 193;
    public final int CASE_W_0xc2 = 194;
    public final int CASE_W_0xc3 = 195;
    public final int CASE_W_0xc4 = 196;
    public final int CASE_W_0xc5 = 197;
    public final int CASE_W_0xc6 = 198;
    public final int CASE_W_0xc7 = 199;
    public final int CASE_W_0xc8 = 200;
    public final int CASE_W_0xc9 = 201;
    public final int CASE_W_0xca = 202;
    public final int CASE_W_0xcb = 203;
    public final int CASE_W_0xcc = 204;
    public final int CASE_W_0xcd = 205;
    public final int CASE_W_0xce = 206;
    public final int CASE_W_0xcf = 207;
    public final int CASE_W_0xd0 = 208;
    public final int CASE_W_0xd1 = 209;
    public final int CASE_W_0xd2 = 210;
    public final int CASE_W_0xd3 = 211;
    public final int CASE_W_0xd4 = 212;
    public final int CASE_W_0xd5 = 213;
    public final int CASE_W_0xd6 = 214;
    public final int CASE_W_0xd7 = 215;
    public final int CASE_W_0xd8 = 216;
    public final int CASE_W_0xd9 = 217;
    public final int CASE_W_0xda = 218;
    public final int CASE_W_0xdb = 219;
    public final int CASE_W_0xdc = 220;
    public final int CASE_W_0xdd = 221;
    public final int CASE_W_0xde = 222;
    public final int CASE_W_0xdf = 223;
    public final int CASE_W_0xe0 = 224;
    public final int CASE_W_0xe1 = 225;
    public final int CASE_W_0xe2 = 226;
    public final int CASE_W_0xe3 = 227;
    public final int CASE_W_0xe4 = 228;
    public final int CASE_W_0xe5 = 229;
    public final int CASE_W_0xe6 = 230;
    public final int CASE_W_0xe7 = 231;
    public final int CASE_W_0xe8 = 232;
    public final int CASE_W_0xe9 = 233;
    public final int CASE_W_0xea = 234;
    public final int CASE_W_0xeb = 235;
    public final int CASE_W_0xec = 236;
    public final int CASE_W_0xed = 237;
    public final int CASE_W_0xee = 238;
    public final int CASE_W_0xef = 239;
    public final int CASE_W_0xf0 = 240;
    public final int CASE_W_0xf1 = 241;
    public final int CASE_W_0xf2 = 242;
    public final int CASE_W_0xf3 = 243;
    public final int CASE_W_0xf4 = 244;
    public final int CASE_W_0xf5 = 245;
    public final int CASE_W_0xf6 = 246;
    public final int CASE_W_0xf7 = 247;
    public final int CASE_W_0xf8 = 248;
    public final int CASE_W_0xf9 = 249;
    public final int CASE_W_0xfa = 250;
    public final int CASE_W_0xfb = 251;
    public final int CASE_W_0xfc = 252;
    public final int CASE_W_0xfd = 253;
    public final int CASE_W_0xfe = 254;
    public final int CASE_W_0xff = 255;

    public final int CASE_D_0x00 = 512;
    public final int CASE_D_0x01 = 513;
    public final int CASE_D_0x02 = 514;
    public final int CASE_D_0x03 = 515;
    public final int CASE_D_0x04 = 516;
    public final int CASE_D_0x05 = 517;
    public final int CASE_D_0x06 = 518;
    public final int CASE_D_0x07 = 519;
    public final int CASE_D_0x08 = 520;
    public final int CASE_D_0x09 = 521;
    public final int CASE_D_0x0a = 522;
    public final int CASE_D_0x0b = 523;
    public final int CASE_D_0x0c = 524;
    public final int CASE_D_0x0d = 525;
    public final int CASE_D_0x0e = 526;
    public final int CASE_D_0x0f = 527;
    public final int CASE_D_0x10 = 528;
    public final int CASE_D_0x11 = 529;
    public final int CASE_D_0x12 = 530;
    public final int CASE_D_0x13 = 531;
    public final int CASE_D_0x14 = 532;
    public final int CASE_D_0x15 = 533;
    public final int CASE_D_0x16 = 534;
    public final int CASE_D_0x17 = 535;
    public final int CASE_D_0x18 = 536;
    public final int CASE_D_0x19 = 537;
    public final int CASE_D_0x1a = 538;
    public final int CASE_D_0x1b = 539;
    public final int CASE_D_0x1c = 540;
    public final int CASE_D_0x1d = 541;
    public final int CASE_D_0x1e = 542;
    public final int CASE_D_0x1f = 543;
    public final int CASE_D_0x20 = 544;
    public final int CASE_D_0x21 = 545;
    public final int CASE_D_0x22 = 546;
    public final int CASE_D_0x23 = 547;
    public final int CASE_D_0x24 = 548;
    public final int CASE_D_0x25 = 549;
    public final int CASE_D_0x26 = 550;
    public final int CASE_D_0x27 = 551;
    public final int CASE_D_0x28 = 552;
    public final int CASE_D_0x29 = 553;
    public final int CASE_D_0x2a = 554;
    public final int CASE_D_0x2b = 555;
    public final int CASE_D_0x2c = 556;
    public final int CASE_D_0x2d = 557;
    public final int CASE_D_0x2e = 558;
    public final int CASE_D_0x2f = 559;
    public final int CASE_D_0x30 = 560;
    public final int CASE_D_0x31 = 561;
    public final int CASE_D_0x32 = 562;
    public final int CASE_D_0x33 = 563;
    public final int CASE_D_0x34 = 564;
    public final int CASE_D_0x35 = 565;
    public final int CASE_D_0x36 = 566;
    public final int CASE_D_0x37 = 567;
    public final int CASE_D_0x38 = 568;
    public final int CASE_D_0x39 = 569;
    public final int CASE_D_0x3a = 570;
    public final int CASE_D_0x3b = 571;
    public final int CASE_D_0x3c = 572;
    public final int CASE_D_0x3d = 573;
    public final int CASE_D_0x3e = 574;
    public final int CASE_D_0x3f = 575;
    public final int CASE_D_0x40 = 576;
    public final int CASE_D_0x41 = 577;
    public final int CASE_D_0x42 = 578;
    public final int CASE_D_0x43 = 579;
    public final int CASE_D_0x44 = 580;
    public final int CASE_D_0x45 = 581;
    public final int CASE_D_0x46 = 582;
    public final int CASE_D_0x47 = 583;
    public final int CASE_D_0x48 = 584;
    public final int CASE_D_0x49 = 585;
    public final int CASE_D_0x4a = 586;
    public final int CASE_D_0x4b = 587;
    public final int CASE_D_0x4c = 588;
    public final int CASE_D_0x4d = 589;
    public final int CASE_D_0x4e = 590;
    public final int CASE_D_0x4f = 591;
    public final int CASE_D_0x50 = 592;
    public final int CASE_D_0x51 = 593;
    public final int CASE_D_0x52 = 594;
    public final int CASE_D_0x53 = 595;
    public final int CASE_D_0x54 = 596;
    public final int CASE_D_0x55 = 597;
    public final int CASE_D_0x56 = 598;
    public final int CASE_D_0x57 = 599;
    public final int CASE_D_0x58 = 600;
    public final int CASE_D_0x59 = 601;
    public final int CASE_D_0x5a = 602;
    public final int CASE_D_0x5b = 603;
    public final int CASE_D_0x5c = 604;
    public final int CASE_D_0x5d = 605;
    public final int CASE_D_0x5e = 606;
    public final int CASE_D_0x5f = 607;
    public final int CASE_D_0x60 = 608;
    public final int CASE_D_0x61 = 609;
    public final int CASE_D_0x62 = 610;
    public final int CASE_D_0x63 = 611;
    public final int CASE_D_0x64 = 612;
    public final int CASE_D_0x65 = 613;
    public final int CASE_D_0x66 = 614;
    public final int CASE_D_0x67 = 615;
    public final int CASE_D_0x68 = 616;
    public final int CASE_D_0x69 = 617;
    public final int CASE_D_0x6a = 618;
    public final int CASE_D_0x6b = 619;
    public final int CASE_D_0x6c = 620;
    public final int CASE_D_0x6d = 621;
    public final int CASE_D_0x6e = 622;
    public final int CASE_D_0x6f = 623;
    public final int CASE_D_0x70 = 624;
    public final int CASE_D_0x71 = 625;
    public final int CASE_D_0x72 = 626;
    public final int CASE_D_0x73 = 627;
    public final int CASE_D_0x74 = 628;
    public final int CASE_D_0x75 = 629;
    public final int CASE_D_0x76 = 630;
    public final int CASE_D_0x77 = 631;
    public final int CASE_D_0x78 = 632;
    public final int CASE_D_0x79 = 633;
    public final int CASE_D_0x7a = 634;
    public final int CASE_D_0x7b = 635;
    public final int CASE_D_0x7c = 636;
    public final int CASE_D_0x7d = 637;
    public final int CASE_D_0x7e = 638;
    public final int CASE_D_0x7f = 639;
    public final int CASE_D_0x80 = 640;
    public final int CASE_D_0x81 = 641;
    public final int CASE_D_0x82 = 642;
    public final int CASE_D_0x83 = 643;
    public final int CASE_D_0x84 = 644;
    public final int CASE_D_0x85 = 645;
    public final int CASE_D_0x86 = 646;
    public final int CASE_D_0x87 = 647;
    public final int CASE_D_0x88 = 648;
    public final int CASE_D_0x89 = 649;
    public final int CASE_D_0x8a = 650;
    public final int CASE_D_0x8b = 651;
    public final int CASE_D_0x8c = 652;
    public final int CASE_D_0x8d = 653;
    public final int CASE_D_0x8e = 654;
    public final int CASE_D_0x8f = 655;
    public final int CASE_D_0x90 = 656;
    public final int CASE_D_0x91 = 657;
    public final int CASE_D_0x92 = 658;
    public final int CASE_D_0x93 = 659;
    public final int CASE_D_0x94 = 660;
    public final int CASE_D_0x95 = 661;
    public final int CASE_D_0x96 = 662;
    public final int CASE_D_0x97 = 663;
    public final int CASE_D_0x98 = 664;
    public final int CASE_D_0x99 = 665;
    public final int CASE_D_0x9a = 666;
    public final int CASE_D_0x9b = 667;
    public final int CASE_D_0x9c = 668;
    public final int CASE_D_0x9d = 669;
    public final int CASE_D_0x9e = 670;
    public final int CASE_D_0x9f = 671;
    public final int CASE_D_0xa0 = 672;
    public final int CASE_D_0xa1 = 673;
    public final int CASE_D_0xa2 = 674;
    public final int CASE_D_0xa3 = 675;
    public final int CASE_D_0xa4 = 676;
    public final int CASE_D_0xa5 = 677;
    public final int CASE_D_0xa6 = 678;
    public final int CASE_D_0xa7 = 679;
    public final int CASE_D_0xa8 = 680;
    public final int CASE_D_0xa9 = 681;
    public final int CASE_D_0xaa = 682;
    public final int CASE_D_0xab = 683;
    public final int CASE_D_0xac = 684;
    public final int CASE_D_0xad = 685;
    public final int CASE_D_0xae = 686;
    public final int CASE_D_0xaf = 687;
    public final int CASE_D_0xb0 = 688;
    public final int CASE_D_0xb1 = 689;
    public final int CASE_D_0xb2 = 690;
    public final int CASE_D_0xb3 = 691;
    public final int CASE_D_0xb4 = 692;
    public final int CASE_D_0xb5 = 693;
    public final int CASE_D_0xb6 = 694;
    public final int CASE_D_0xb7 = 695;
    public final int CASE_D_0xb8 = 696;
    public final int CASE_D_0xb9 = 697;
    public final int CASE_D_0xba = 698;
    public final int CASE_D_0xbb = 699;
    public final int CASE_D_0xbc = 700;
    public final int CASE_D_0xbd = 701;
    public final int CASE_D_0xbe = 702;
    public final int CASE_D_0xbf = 703;
    public final int CASE_D_0xc0 = 704;
    public final int CASE_D_0xc1 = 705;
    public final int CASE_D_0xc2 = 706;
    public final int CASE_D_0xc3 = 707;
    public final int CASE_D_0xc4 = 708;
    public final int CASE_D_0xc5 = 709;
    public final int CASE_D_0xc6 = 710;
    public final int CASE_D_0xc7 = 711;
    public final int CASE_D_0xc8 = 712;
    public final int CASE_D_0xc9 = 713;
    public final int CASE_D_0xca = 714;
    public final int CASE_D_0xcb = 715;
    public final int CASE_D_0xcc = 716;
    public final int CASE_D_0xcd = 717;
    public final int CASE_D_0xce = 718;
    public final int CASE_D_0xcf = 719;
    public final int CASE_D_0xd0 = 720;
    public final int CASE_D_0xd1 = 721;
    public final int CASE_D_0xd2 = 722;
    public final int CASE_D_0xd3 = 723;
    public final int CASE_D_0xd4 = 724;
    public final int CASE_D_0xd5 = 725;
    public final int CASE_D_0xd6 = 726;
    public final int CASE_D_0xd7 = 727;
    public final int CASE_D_0xd8 = 728;
    public final int CASE_D_0xd9 = 729;
    public final int CASE_D_0xda = 730;
    public final int CASE_D_0xdb = 731;
    public final int CASE_D_0xdc = 732;
    public final int CASE_D_0xdd = 733;
    public final int CASE_D_0xde = 734;
    public final int CASE_D_0xdf = 735;
    public final int CASE_D_0xe0 = 736;
    public final int CASE_D_0xe1 = 737;
    public final int CASE_D_0xe2 = 738;
    public final int CASE_D_0xe3 = 739;
    public final int CASE_D_0xe4 = 740;
    public final int CASE_D_0xe5 = 741;
    public final int CASE_D_0xe6 = 742;
    public final int CASE_D_0xe7 = 743;
    public final int CASE_D_0xe8 = 744;
    public final int CASE_D_0xe9 = 745;
    public final int CASE_D_0xea = 746;
    public final int CASE_D_0xeb = 747;
    public final int CASE_D_0xec = 748;
    public final int CASE_D_0xed = 749;
    public final int CASE_D_0xee = 750;
    public final int CASE_D_0xef = 751;
    public final int CASE_D_0xf0 = 752;
    public final int CASE_D_0xf1 = 753;
    public final int CASE_D_0xf2 = 754;
    public final int CASE_D_0xf3 = 755;
    public final int CASE_D_0xf4 = 756;
    public final int CASE_D_0xf5 = 757;
    public final int CASE_D_0xf6 = 758;
    public final int CASE_D_0xf7 = 759;
    public final int CASE_D_0xf8 = 760;
    public final int CASE_D_0xf9 = 761;
    public final int CASE_D_0xfa = 762;
    public final int CASE_D_0xfb = 763;
    public final int CASE_D_0xfc = 764;
    public final int CASE_D_0xfd = 765;
    public final int CASE_D_0xfe = 766;
    public final int CASE_D_0xff = 767;

    public final int CASE_0F_W_0x00 = 256;
    public final int CASE_0F_W_0x01 = 257;
    public final int CASE_0F_W_0x02 = 258;
    public final int CASE_0F_W_0x03 = 259;
    public final int CASE_0F_W_0x04 = 260;
    public final int CASE_0F_W_0x05 = 261;
    public final int CASE_0F_W_0x06 = 262;
    public final int CASE_0F_W_0x07 = 263;
    public final int CASE_0F_W_0x08 = 264;
    public final int CASE_0F_W_0x09 = 265;
    public final int CASE_0F_W_0x0a = 266;
    public final int CASE_0F_W_0x0b = 267;
    public final int CASE_0F_W_0x0c = 268;
    public final int CASE_0F_W_0x0d = 269;
    public final int CASE_0F_W_0x0e = 270;
    public final int CASE_0F_W_0x0f = 271;
    public final int CASE_0F_W_0x10 = 272;
    public final int CASE_0F_W_0x11 = 273;
    public final int CASE_0F_W_0x12 = 274;
    public final int CASE_0F_W_0x13 = 275;
    public final int CASE_0F_W_0x14 = 276;
    public final int CASE_0F_W_0x15 = 277;
    public final int CASE_0F_W_0x16 = 278;
    public final int CASE_0F_W_0x17 = 279;
    public final int CASE_0F_W_0x18 = 280;
    public final int CASE_0F_W_0x19 = 281;
    public final int CASE_0F_W_0x1a = 282;
    public final int CASE_0F_W_0x1b = 283;
    public final int CASE_0F_W_0x1c = 284;
    public final int CASE_0F_W_0x1d = 285;
    public final int CASE_0F_W_0x1e = 286;
    public final int CASE_0F_W_0x1f = 287;
    public final int CASE_0F_W_0x20 = 288;
    public final int CASE_0F_W_0x21 = 289;
    public final int CASE_0F_W_0x22 = 290;
    public final int CASE_0F_W_0x23 = 291;
    public final int CASE_0F_W_0x24 = 292;
    public final int CASE_0F_W_0x25 = 293;
    public final int CASE_0F_W_0x26 = 294;
    public final int CASE_0F_W_0x27 = 295;
    public final int CASE_0F_W_0x28 = 296;
    public final int CASE_0F_W_0x29 = 297;
    public final int CASE_0F_W_0x2a = 298;
    public final int CASE_0F_W_0x2b = 299;
    public final int CASE_0F_W_0x2c = 300;
    public final int CASE_0F_W_0x2d = 301;
    public final int CASE_0F_W_0x2e = 302;
    public final int CASE_0F_W_0x2f = 303;
    public final int CASE_0F_W_0x30 = 304;
    public final int CASE_0F_W_0x31 = 305;
    public final int CASE_0F_W_0x32 = 306;
    public final int CASE_0F_W_0x33 = 307;
    public final int CASE_0F_W_0x34 = 308;
    public final int CASE_0F_W_0x35 = 309;
    public final int CASE_0F_W_0x36 = 310;
    public final int CASE_0F_W_0x37 = 311;
    public final int CASE_0F_W_0x38 = 312;
    public final int CASE_0F_W_0x39 = 313;
    public final int CASE_0F_W_0x3a = 314;
    public final int CASE_0F_W_0x3b = 315;
    public final int CASE_0F_W_0x3c = 316;
    public final int CASE_0F_W_0x3d = 317;
    public final int CASE_0F_W_0x3e = 318;
    public final int CASE_0F_W_0x3f = 319;
    public final int CASE_0F_W_0x40 = 320;
    public final int CASE_0F_W_0x41 = 321;
    public final int CASE_0F_W_0x42 = 322;
    public final int CASE_0F_W_0x43 = 323;
    public final int CASE_0F_W_0x44 = 324;
    public final int CASE_0F_W_0x45 = 325;
    public final int CASE_0F_W_0x46 = 326;
    public final int CASE_0F_W_0x47 = 327;
    public final int CASE_0F_W_0x48 = 328;
    public final int CASE_0F_W_0x49 = 329;
    public final int CASE_0F_W_0x4a = 330;
    public final int CASE_0F_W_0x4b = 331;
    public final int CASE_0F_W_0x4c = 332;
    public final int CASE_0F_W_0x4d = 333;
    public final int CASE_0F_W_0x4e = 334;
    public final int CASE_0F_W_0x4f = 335;
    public final int CASE_0F_W_0x50 = 336;
    public final int CASE_0F_W_0x51 = 337;
    public final int CASE_0F_W_0x52 = 338;
    public final int CASE_0F_W_0x53 = 339;
    public final int CASE_0F_W_0x54 = 340;
    public final int CASE_0F_W_0x55 = 341;
    public final int CASE_0F_W_0x56 = 342;
    public final int CASE_0F_W_0x57 = 343;
    public final int CASE_0F_W_0x58 = 344;
    public final int CASE_0F_W_0x59 = 345;
    public final int CASE_0F_W_0x5a = 346;
    public final int CASE_0F_W_0x5b = 347;
    public final int CASE_0F_W_0x5c = 348;
    public final int CASE_0F_W_0x5d = 349;
    public final int CASE_0F_W_0x5e = 350;
    public final int CASE_0F_W_0x5f = 351;
    public final int CASE_0F_W_0x60 = 352;
    public final int CASE_0F_W_0x61 = 353;
    public final int CASE_0F_W_0x62 = 354;
    public final int CASE_0F_W_0x63 = 355;
    public final int CASE_0F_W_0x64 = 356;
    public final int CASE_0F_W_0x65 = 357;
    public final int CASE_0F_W_0x66 = 358;
    public final int CASE_0F_W_0x67 = 359;
    public final int CASE_0F_W_0x68 = 360;
    public final int CASE_0F_W_0x69 = 361;
    public final int CASE_0F_W_0x6a = 362;
    public final int CASE_0F_W_0x6b = 363;
    public final int CASE_0F_W_0x6c = 364;
    public final int CASE_0F_W_0x6d = 365;
    public final int CASE_0F_W_0x6e = 366;
    public final int CASE_0F_W_0x6f = 367;
    public final int CASE_0F_W_0x70 = 368;
    public final int CASE_0F_W_0x71 = 369;
    public final int CASE_0F_W_0x72 = 370;
    public final int CASE_0F_W_0x73 = 371;
    public final int CASE_0F_W_0x74 = 372;
    public final int CASE_0F_W_0x75 = 373;
    public final int CASE_0F_W_0x76 = 374;
    public final int CASE_0F_W_0x77 = 375;
    public final int CASE_0F_W_0x78 = 376;
    public final int CASE_0F_W_0x79 = 377;
    public final int CASE_0F_W_0x7a = 378;
    public final int CASE_0F_W_0x7b = 379;
    public final int CASE_0F_W_0x7c = 380;
    public final int CASE_0F_W_0x7d = 381;
    public final int CASE_0F_W_0x7e = 382;
    public final int CASE_0F_W_0x7f = 383;
    public final int CASE_0F_W_0x80 = 384;
    public final int CASE_0F_W_0x81 = 385;
    public final int CASE_0F_W_0x82 = 386;
    public final int CASE_0F_W_0x83 = 387;
    public final int CASE_0F_W_0x84 = 388;
    public final int CASE_0F_W_0x85 = 389;
    public final int CASE_0F_W_0x86 = 390;
    public final int CASE_0F_W_0x87 = 391;
    public final int CASE_0F_W_0x88 = 392;
    public final int CASE_0F_W_0x89 = 393;
    public final int CASE_0F_W_0x8a = 394;
    public final int CASE_0F_W_0x8b = 395;
    public final int CASE_0F_W_0x8c = 396;
    public final int CASE_0F_W_0x8d = 397;
    public final int CASE_0F_W_0x8e = 398;
    public final int CASE_0F_W_0x8f = 399;
    public final int CASE_0F_W_0x90 = 400;
    public final int CASE_0F_W_0x91 = 401;
    public final int CASE_0F_W_0x92 = 402;
    public final int CASE_0F_W_0x93 = 403;
    public final int CASE_0F_W_0x94 = 404;
    public final int CASE_0F_W_0x95 = 405;
    public final int CASE_0F_W_0x96 = 406;
    public final int CASE_0F_W_0x97 = 407;
    public final int CASE_0F_W_0x98 = 408;
    public final int CASE_0F_W_0x99 = 409;
    public final int CASE_0F_W_0x9a = 410;
    public final int CASE_0F_W_0x9b = 411;
    public final int CASE_0F_W_0x9c = 412;
    public final int CASE_0F_W_0x9d = 413;
    public final int CASE_0F_W_0x9e = 414;
    public final int CASE_0F_W_0x9f = 415;
    public final int CASE_0F_W_0xa0 = 416;
    public final int CASE_0F_W_0xa1 = 417;
    public final int CASE_0F_W_0xa2 = 418;
    public final int CASE_0F_W_0xa3 = 419;
    public final int CASE_0F_W_0xa4 = 420;
    public final int CASE_0F_W_0xa5 = 421;
    public final int CASE_0F_W_0xa6 = 422;
    public final int CASE_0F_W_0xa7 = 423;
    public final int CASE_0F_W_0xa8 = 424;
    public final int CASE_0F_W_0xa9 = 425;
    public final int CASE_0F_W_0xaa = 426;
    public final int CASE_0F_W_0xab = 427;
    public final int CASE_0F_W_0xac = 428;
    public final int CASE_0F_W_0xad = 429;
    public final int CASE_0F_W_0xae = 430;
    public final int CASE_0F_W_0xaf = 431;
    public final int CASE_0F_W_0xb0 = 432;
    public final int CASE_0F_W_0xb1 = 433;
    public final int CASE_0F_W_0xb2 = 434;
    public final int CASE_0F_W_0xb3 = 435;
    public final int CASE_0F_W_0xb4 = 436;
    public final int CASE_0F_W_0xb5 = 437;
    public final int CASE_0F_W_0xb6 = 438;
    public final int CASE_0F_W_0xb7 = 439;
    public final int CASE_0F_W_0xb8 = 440;
    public final int CASE_0F_W_0xb9 = 441;
    public final int CASE_0F_W_0xba = 442;
    public final int CASE_0F_W_0xbb = 443;
    public final int CASE_0F_W_0xbc = 444;
    public final int CASE_0F_W_0xbd = 445;
    public final int CASE_0F_W_0xbe = 446;
    public final int CASE_0F_W_0xbf = 447;
    public final int CASE_0F_W_0xc0 = 448;
    public final int CASE_0F_W_0xc1 = 449;
    public final int CASE_0F_W_0xc2 = 450;
    public final int CASE_0F_W_0xc3 = 451;
    public final int CASE_0F_W_0xc4 = 452;
    public final int CASE_0F_W_0xc5 = 453;
    public final int CASE_0F_W_0xc6 = 454;
    public final int CASE_0F_W_0xc7 = 455;
    public final int CASE_0F_W_0xc8 = 456;
    public final int CASE_0F_W_0xc9 = 457;
    public final int CASE_0F_W_0xca = 458;
    public final int CASE_0F_W_0xcb = 459;
    public final int CASE_0F_W_0xcc = 460;
    public final int CASE_0F_W_0xcd = 461;
    public final int CASE_0F_W_0xce = 462;
    public final int CASE_0F_W_0xcf = 463;
    public final int CASE_0F_W_0xd0 = 464;
    public final int CASE_0F_W_0xd1 = 465;
    public final int CASE_0F_W_0xd2 = 466;
    public final int CASE_0F_W_0xd3 = 467;
    public final int CASE_0F_W_0xd4 = 468;
    public final int CASE_0F_W_0xd5 = 469;
    public final int CASE_0F_W_0xd6 = 470;
    public final int CASE_0F_W_0xd7 = 471;
    public final int CASE_0F_W_0xd8 = 472;
    public final int CASE_0F_W_0xd9 = 473;
    public final int CASE_0F_W_0xda = 474;
    public final int CASE_0F_W_0xdb = 475;
    public final int CASE_0F_W_0xdc = 476;
    public final int CASE_0F_W_0xdd = 477;
    public final int CASE_0F_W_0xde = 478;
    public final int CASE_0F_W_0xdf = 479;
    public final int CASE_0F_W_0xe0 = 480;
    public final int CASE_0F_W_0xe1 = 481;
    public final int CASE_0F_W_0xe2 = 482;
    public final int CASE_0F_W_0xe3 = 483;
    public final int CASE_0F_W_0xe4 = 484;
    public final int CASE_0F_W_0xe5 = 485;
    public final int CASE_0F_W_0xe6 = 486;
    public final int CASE_0F_W_0xe7 = 487;
    public final int CASE_0F_W_0xe8 = 488;
    public final int CASE_0F_W_0xe9 = 489;
    public final int CASE_0F_W_0xea = 490;
    public final int CASE_0F_W_0xeb = 491;
    public final int CASE_0F_W_0xec = 492;
    public final int CASE_0F_W_0xed = 493;
    public final int CASE_0F_W_0xee = 494;
    public final int CASE_0F_W_0xef = 495;
    public final int CASE_0F_W_0xf0 = 496;
    public final int CASE_0F_W_0xf1 = 497;
    public final int CASE_0F_W_0xf2 = 498;
    public final int CASE_0F_W_0xf3 = 499;
    public final int CASE_0F_W_0xf4 = 500;
    public final int CASE_0F_W_0xf5 = 501;
    public final int CASE_0F_W_0xf6 = 502;
    public final int CASE_0F_W_0xf7 = 503;
    public final int CASE_0F_W_0xf8 = 504;
    public final int CASE_0F_W_0xf9 = 505;
    public final int CASE_0F_W_0xfa = 506;
    public final int CASE_0F_W_0xfb = 507;
    public final int CASE_0F_W_0xfc = 508;
    public final int CASE_0F_W_0xfd = 509;
    public final int CASE_0F_W_0xfe = 510;
    public final int CASE_0F_W_0xff = 511;

    public final int CASE_0F_D_0x00 = 768;
    public final int CASE_0F_D_0x01 = 769;
    public final int CASE_0F_D_0x02 = 770;
    public final int CASE_0F_D_0x03 = 771;
    public final int CASE_0F_D_0x04 = 772;
    public final int CASE_0F_D_0x05 = 773;
    public final int CASE_0F_D_0x06 = 774;
    public final int CASE_0F_D_0x07 = 775;
    public final int CASE_0F_D_0x08 = 776;
    public final int CASE_0F_D_0x09 = 777;
    public final int CASE_0F_D_0x0a = 778;
    public final int CASE_0F_D_0x0b = 779;
    public final int CASE_0F_D_0x0c = 780;
    public final int CASE_0F_D_0x0d = 781;
    public final int CASE_0F_D_0x0e = 782;
    public final int CASE_0F_D_0x0f = 783;
    public final int CASE_0F_D_0x10 = 784;
    public final int CASE_0F_D_0x11 = 785;
    public final int CASE_0F_D_0x12 = 786;
    public final int CASE_0F_D_0x13 = 787;
    public final int CASE_0F_D_0x14 = 788;
    public final int CASE_0F_D_0x15 = 789;
    public final int CASE_0F_D_0x16 = 790;
    public final int CASE_0F_D_0x17 = 791;
    public final int CASE_0F_D_0x18 = 792;
    public final int CASE_0F_D_0x19 = 793;
    public final int CASE_0F_D_0x1a = 794;
    public final int CASE_0F_D_0x1b = 795;
    public final int CASE_0F_D_0x1c = 796;
    public final int CASE_0F_D_0x1d = 797;
    public final int CASE_0F_D_0x1e = 798;
    public final int CASE_0F_D_0x1f = 799;
    public final int CASE_0F_D_0x20 = 800;
    public final int CASE_0F_D_0x21 = 801;
    public final int CASE_0F_D_0x22 = 802;
    public final int CASE_0F_D_0x23 = 803;
    public final int CASE_0F_D_0x24 = 804;
    public final int CASE_0F_D_0x25 = 805;
    public final int CASE_0F_D_0x26 = 806;
    public final int CASE_0F_D_0x27 = 807;
    public final int CASE_0F_D_0x28 = 808;
    public final int CASE_0F_D_0x29 = 809;
    public final int CASE_0F_D_0x2a = 810;
    public final int CASE_0F_D_0x2b = 811;
    public final int CASE_0F_D_0x2c = 812;
    public final int CASE_0F_D_0x2d = 813;
    public final int CASE_0F_D_0x2e = 814;
    public final int CASE_0F_D_0x2f = 815;
    public final int CASE_0F_D_0x30 = 816;
    public final int CASE_0F_D_0x31 = 817;
    public final int CASE_0F_D_0x32 = 818;
    public final int CASE_0F_D_0x33 = 819;
    public final int CASE_0F_D_0x34 = 820;
    public final int CASE_0F_D_0x35 = 821;
    public final int CASE_0F_D_0x36 = 822;
    public final int CASE_0F_D_0x37 = 823;
    public final int CASE_0F_D_0x38 = 824;
    public final int CASE_0F_D_0x39 = 825;
    public final int CASE_0F_D_0x3a = 826;
    public final int CASE_0F_D_0x3b = 827;
    public final int CASE_0F_D_0x3c = 828;
    public final int CASE_0F_D_0x3d = 829;
    public final int CASE_0F_D_0x3e = 830;
    public final int CASE_0F_D_0x3f = 831;
    public final int CASE_0F_D_0x40 = 832;
    public final int CASE_0F_D_0x41 = 833;
    public final int CASE_0F_D_0x42 = 834;
    public final int CASE_0F_D_0x43 = 835;
    public final int CASE_0F_D_0x44 = 836;
    public final int CASE_0F_D_0x45 = 837;
    public final int CASE_0F_D_0x46 = 838;
    public final int CASE_0F_D_0x47 = 839;
    public final int CASE_0F_D_0x48 = 840;
    public final int CASE_0F_D_0x49 = 841;
    public final int CASE_0F_D_0x4a = 842;
    public final int CASE_0F_D_0x4b = 843;
    public final int CASE_0F_D_0x4c = 844;
    public final int CASE_0F_D_0x4d = 845;
    public final int CASE_0F_D_0x4e = 846;
    public final int CASE_0F_D_0x4f = 847;
    public final int CASE_0F_D_0x50 = 848;
    public final int CASE_0F_D_0x51 = 849;
    public final int CASE_0F_D_0x52 = 850;
    public final int CASE_0F_D_0x53 = 851;
    public final int CASE_0F_D_0x54 = 852;
    public final int CASE_0F_D_0x55 = 853;
    public final int CASE_0F_D_0x56 = 854;
    public final int CASE_0F_D_0x57 = 855;
    public final int CASE_0F_D_0x58 = 856;
    public final int CASE_0F_D_0x59 = 857;
    public final int CASE_0F_D_0x5a = 858;
    public final int CASE_0F_D_0x5b = 859;
    public final int CASE_0F_D_0x5c = 860;
    public final int CASE_0F_D_0x5d = 861;
    public final int CASE_0F_D_0x5e = 862;
    public final int CASE_0F_D_0x5f = 863;
    public final int CASE_0F_D_0x60 = 864;
    public final int CASE_0F_D_0x61 = 865;
    public final int CASE_0F_D_0x62 = 866;
    public final int CASE_0F_D_0x63 = 867;
    public final int CASE_0F_D_0x64 = 868;
    public final int CASE_0F_D_0x65 = 869;
    public final int CASE_0F_D_0x66 = 870;
    public final int CASE_0F_D_0x67 = 871;
    public final int CASE_0F_D_0x68 = 872;
    public final int CASE_0F_D_0x69 = 873;
    public final int CASE_0F_D_0x6a = 874;
    public final int CASE_0F_D_0x6b = 875;
    public final int CASE_0F_D_0x6c = 876;
    public final int CASE_0F_D_0x6d = 877;
    public final int CASE_0F_D_0x6e = 878;
    public final int CASE_0F_D_0x6f = 879;
    public final int CASE_0F_D_0x70 = 880;
    public final int CASE_0F_D_0x71 = 881;
    public final int CASE_0F_D_0x72 = 882;
    public final int CASE_0F_D_0x73 = 883;
    public final int CASE_0F_D_0x74 = 884;
    public final int CASE_0F_D_0x75 = 885;
    public final int CASE_0F_D_0x76 = 886;
    public final int CASE_0F_D_0x77 = 887;
    public final int CASE_0F_D_0x78 = 888;
    public final int CASE_0F_D_0x79 = 889;
    public final int CASE_0F_D_0x7a = 890;
    public final int CASE_0F_D_0x7b = 891;
    public final int CASE_0F_D_0x7c = 892;
    public final int CASE_0F_D_0x7d = 893;
    public final int CASE_0F_D_0x7e = 894;
    public final int CASE_0F_D_0x7f = 895;
    public final int CASE_0F_D_0x80 = 896;
    public final int CASE_0F_D_0x81 = 897;
    public final int CASE_0F_D_0x82 = 898;
    public final int CASE_0F_D_0x83 = 899;
    public final int CASE_0F_D_0x84 = 900;
    public final int CASE_0F_D_0x85 = 901;
    public final int CASE_0F_D_0x86 = 902;
    public final int CASE_0F_D_0x87 = 903;
    public final int CASE_0F_D_0x88 = 904;
    public final int CASE_0F_D_0x89 = 905;
    public final int CASE_0F_D_0x8a = 906;
    public final int CASE_0F_D_0x8b = 907;
    public final int CASE_0F_D_0x8c = 908;
    public final int CASE_0F_D_0x8d = 909;
    public final int CASE_0F_D_0x8e = 910;
    public final int CASE_0F_D_0x8f = 911;
    public final int CASE_0F_D_0x90 = 912;
    public final int CASE_0F_D_0x91 = 913;
    public final int CASE_0F_D_0x92 = 914;
    public final int CASE_0F_D_0x93 = 915;
    public final int CASE_0F_D_0x94 = 916;
    public final int CASE_0F_D_0x95 = 917;
    public final int CASE_0F_D_0x96 = 918;
    public final int CASE_0F_D_0x97 = 919;
    public final int CASE_0F_D_0x98 = 920;
    public final int CASE_0F_D_0x99 = 921;
    public final int CASE_0F_D_0x9a = 922;
    public final int CASE_0F_D_0x9b = 923;
    public final int CASE_0F_D_0x9c = 924;
    public final int CASE_0F_D_0x9d = 925;
    public final int CASE_0F_D_0x9e = 926;
    public final int CASE_0F_D_0x9f = 927;
    public final int CASE_0F_D_0xa0 = 928;
    public final int CASE_0F_D_0xa1 = 929;
    public final int CASE_0F_D_0xa2 = 930;
    public final int CASE_0F_D_0xa3 = 931;
    public final int CASE_0F_D_0xa4 = 932;
    public final int CASE_0F_D_0xa5 = 933;
    public final int CASE_0F_D_0xa6 = 934;
    public final int CASE_0F_D_0xa7 = 935;
    public final int CASE_0F_D_0xa8 = 936;
    public final int CASE_0F_D_0xa9 = 937;
    public final int CASE_0F_D_0xaa = 938;
    public final int CASE_0F_D_0xab = 939;
    public final int CASE_0F_D_0xac = 940;
    public final int CASE_0F_D_0xad = 941;
    public final int CASE_0F_D_0xae = 942;
    public final int CASE_0F_D_0xaf = 943;
    public final int CASE_0F_D_0xb0 = 944;
    public final int CASE_0F_D_0xb1 = 945;
    public final int CASE_0F_D_0xb2 = 946;
    public final int CASE_0F_D_0xb3 = 947;
    public final int CASE_0F_D_0xb4 = 948;
    public final int CASE_0F_D_0xb5 = 949;
    public final int CASE_0F_D_0xb6 = 950;
    public final int CASE_0F_D_0xb7 = 951;
    public final int CASE_0F_D_0xb8 = 952;
    public final int CASE_0F_D_0xb9 = 953;
    public final int CASE_0F_D_0xba = 954;
    public final int CASE_0F_D_0xbb = 955;
    public final int CASE_0F_D_0xbc = 956;
    public final int CASE_0F_D_0xbd = 957;
    public final int CASE_0F_D_0xbe = 958;
    public final int CASE_0F_D_0xbf = 959;
    public final int CASE_0F_D_0xc0 = 960;
    public final int CASE_0F_D_0xc1 = 961;
    public final int CASE_0F_D_0xc2 = 962;
    public final int CASE_0F_D_0xc3 = 963;
    public final int CASE_0F_D_0xc4 = 964;
    public final int CASE_0F_D_0xc5 = 965;
    public final int CASE_0F_D_0xc6 = 966;
    public final int CASE_0F_D_0xc7 = 967;
    public final int CASE_0F_D_0xc8 = 968;
    public final int CASE_0F_D_0xc9 = 969;
    public final int CASE_0F_D_0xca = 970;
    public final int CASE_0F_D_0xcb = 971;
    public final int CASE_0F_D_0xcc = 972;
    public final int CASE_0F_D_0xcd = 973;
    public final int CASE_0F_D_0xce = 974;
    public final int CASE_0F_D_0xcf = 975;
    public final int CASE_0F_D_0xd0 = 976;
    public final int CASE_0F_D_0xd1 = 977;
    public final int CASE_0F_D_0xd2 = 978;
    public final int CASE_0F_D_0xd3 = 979;
    public final int CASE_0F_D_0xd4 = 980;
    public final int CASE_0F_D_0xd5 = 981;
    public final int CASE_0F_D_0xd6 = 982;
    public final int CASE_0F_D_0xd7 = 983;
    public final int CASE_0F_D_0xd8 = 984;
    public final int CASE_0F_D_0xd9 = 985;
    public final int CASE_0F_D_0xda = 986;
    public final int CASE_0F_D_0xdb = 987;
    public final int CASE_0F_D_0xdc = 988;
    public final int CASE_0F_D_0xdd = 989;
    public final int CASE_0F_D_0xde = 990;
    public final int CASE_0F_D_0xdf = 991;
    public final int CASE_0F_D_0xe0 = 992;
    public final int CASE_0F_D_0xe1 = 993;
    public final int CASE_0F_D_0xe2 = 994;
    public final int CASE_0F_D_0xe3 = 995;
    public final int CASE_0F_D_0xe4 = 996;
    public final int CASE_0F_D_0xe5 = 997;
    public final int CASE_0F_D_0xe6 = 998;
    public final int CASE_0F_D_0xe7 = 999;
    public final int CASE_0F_D_0xe8 = 1000;
    public final int CASE_0F_D_0xe9 = 1001;
    public final int CASE_0F_D_0xea = 1002;
    public final int CASE_0F_D_0xeb = 1003;
    public final int CASE_0F_D_0xec = 1004;
    public final int CASE_0F_D_0xed = 1005;
    public final int CASE_0F_D_0xee = 1006;
    public final int CASE_0F_D_0xef = 1007;
    public final int CASE_0F_D_0xf0 = 1008;
    public final int CASE_0F_D_0xf1 = 1009;
    public final int CASE_0F_D_0xf2 = 1010;
    public final int CASE_0F_D_0xf3 = 1011;
    public final int CASE_0F_D_0xf4 = 1012;
    public final int CASE_0F_D_0xf5 = 1013;
    public final int CASE_0F_D_0xf6 = 1014;
    public final int CASE_0F_D_0xf7 = 1015;
    public final int CASE_0F_D_0xf8 = 1016;
    public final int CASE_0F_D_0xf9 = 1017;
    public final int CASE_0F_D_0xfa = 1018;
    public final int CASE_0F_D_0xfb = 1019;
    public final int CASE_0F_D_0xfc = 1020;
    public final int CASE_0F_D_0xfd = 1021;
    public final int CASE_0F_D_0xfe = 1022;
    public final int CASE_0F_D_0xff = 1023;

    /*--------------------------- end CpuCoreDefine -----------------------------*/

    /*--------------------------- begin CpuCoreRunMethod -----------------------------*/

    public int runCPUCore() {
        int ifet;
        main_loop: while (CPU.Cycles-- > 0) {
            loadIP();
            ifet = Core.OPCodeIndex = CPU.Block.Code.Big ? 0x200 : 0;
            Core.Prefixes = CPU.Block.Code.Big ? 1 : 0;
            Core.EATable = EATable[Core.Prefixes];
            Core.BaseDS = Register.segPhys(Register.SEG_NAME_DS);
            Core.BaseSS = Register.segPhys(Register.SEG_NAME_SS);
            Core.BaseValDS = Register.SEG_NAME_DS;

            restart_opcode: while (true) {
                // TODO switch코드 원상복구
                ifet = Core.OPCodeIndex + fetchB();
                /*
                 * if (DOSMain.dbgCurLoadedProgram.equalsIgnoreCase("open.exe")) { if (ifet != 236 &
                 * ifet != 748 && ifet != 424 && ifet != 936 && ifet != 116 && ifet != 628 && ifet
                 * != 0xa8) System.out.printf("ifet 0x%08X\n", ifet); }
                 */
                // Console.WriteLine(core.cseip);
                // Console.WriteLine("{0}\t{1}\t{2}", cpuModule.CPU_Cycles, cpuModule.CPU_CycleLeft
                // , ifet);
                // Console.WriteLine("{0}\t{1}\t{2}\t{3}\t{4}\t{5}\t{6}", ifet, flags.lf_resd,
                // regsModule.regs[0].getDWord(), regsModule.regs[1].getDWord(),
                // regsModule.regs[2].getDWord(), regsModule.regs[3].getDWord(), core.cseip);
                // Console.WriteLine("{0}\t{1}\t{2}\t{3}\t{4}\t{5}", cpuModule.CPU_Cycles, ifet,
                // regsModule.regs[0].getDWord(), regsModule.regs[1].getDWord(),
                // regsModule.regs[2].getDWord(), core.cseip);
                switch (ifet)
                // switch(core.opcode_index + Fetchb())
                {

                    // #include "core_normal/prefix_none.h"
                    // -- #region prefix_none
                    case CASE_W_0x00:
                    case CASE_D_0x00: /* ADD Eb,Gb */
                        RMEbGb(this::ADDBL, this::ADDBH, this::ADDB);
                        break;
                    case CASE_W_0x01: /* ADD Ew,Gw */
                        RMEwGw(this::ADDW, this::ADDW_M);
                        break;
                    case CASE_W_0x02:
                    case CASE_D_0x02: /* ADD Gb,Eb */
                        RMGbEb(this::ADDBL, this::ADDBH);
                        break;
                    case CASE_W_0x03: /* ADD Gw,Ew */
                        RMGwEw(this::ADDW);
                        break;
                    case CASE_W_0x04:
                    case CASE_D_0x04: /* ADD AL,Ib */
                        // ALIb(ADDB);
                        ADDBL(Register.AX, fetchB());
                        break;
                    case CASE_W_0x05: /* ADD AX,Iw */
                        // AXIw(ADDW);
                        ADDW(Register.AX, fetchW());
                        break;
                    case CASE_W_0x06: /* PUSH ES */
                        CPU.push16(Register.segValue(Register.SEG_NAME_ES));
                        break;
                    case CASE_W_0x07: /* POP ES */
                        if (CPU.popSeg(Register.SEG_NAME_ES, false)) {
                            CPU.exception(CPU.Block.Exception.Which, CPU.Block.Exception.Error);
                            continue main_loop;// org_continue;
                        }
                        break;
                    case CASE_W_0x08:
                    case CASE_D_0x08: /* OR Eb,Gb */
                        RMEbGb(this::ORBL, this::ORBH, this::ORB);
                        break;
                    case CASE_W_0x09: /* OR Ew,Gw */
                        RMEwGw(this::ORW, this::ORW_M);
                        break;
                    case CASE_W_0x0a:
                    case CASE_D_0x0a: /* OR Gb,Eb */
                        RMGbEb(this::ORBL, this::ORBH);
                        break;
                    case CASE_W_0x0b: /* OR Gw,Ew */
                        RMGwEw(this::ORW);
                        break;
                    case CASE_W_0x0c:
                    case CASE_D_0x0c: /* OR AL,Ib */
                        // ALIb(ORB);
                        ORBL(Register.AX, fetchB());
                        break;
                    case CASE_W_0x0d: /* OR AX,Iw */
                        // AXIw(ORW);
                        ORW(Register.AX, fetchW());
                        break;
                    case CASE_W_0x0e: /* PUSH CS */
                        // Push_16(SegValue(cs));
                        CPU.push16(Register.segValue(Register.SEG_NAME_CS));
                        break;
                    case CASE_W_0x0f:
                    case CASE_D_0x0f: /* 2 byte opcodes */
                        Core.OPCodeIndex |= Opcode0F;
                        continue restart_opcode; // goto restart_opcode;
                    // break;
                    case CASE_W_0x10:
                    case CASE_D_0x10: /* ADC Eb,Gb */
                        RMEbGb(this::ADCBL, this::ADCBH, this::ADCB);
                        break;
                    case CASE_W_0x11: /* ADC Ew,Gw */
                        RMEwGw(this::ADCW, this::ADCW_M);
                        break;
                    case CASE_W_0x12:
                    case CASE_D_0x12: /* ADC Gb,Eb */
                        RMGbEb(this::ADCBL, this::ADCBH);
                        break;
                    case CASE_W_0x13: /* ADC Gw,Ew */
                        RMGwEw(this::ADCW);
                        break;
                    case CASE_W_0x14:
                    case CASE_D_0x14: /* ADC AL,Ib */
                        // ALIb(ADCB);
                        ADCBL(Register.AX, fetchB());
                        break;
                    case CASE_W_0x15: /* ADC AX,Iw */
                        // AXIw(ADCW);
                        ADCW(Register.AX, fetchW());
                        break;
                    case CASE_W_0x16: /* PUSH SS */
                        // Push_16(SegValue(ss));
                        CPU.push16(Register.segValue(Register.SEG_NAME_SS));
                        break;
                    case CASE_W_0x17: /* POP SS */
                        // if (CPU_PopSeg(ss,false)) {
                        // cpuModule.CPU_Exception(cpuModule.cpu.exception.which,
                        // cpuModule.cpu.exception.error); continue; };
                        if (CPU.popSeg(Register.SEG_NAME_SS, false)) {
                            CPU.exception(CPU.Block.Exception.Which, CPU.Block.Exception.Error);
                            continue main_loop;// org_continue;
                        }
                        CPU.Cycles++; // Always do another instruction
                        break;
                    case CASE_W_0x18:
                    case CASE_D_0x18: /* SBB Eb,Gb */
                        // RMEbGb(SBBB);
                        RMEbGb(this::SBBBL, this::SBBBH, this::SBBB);
                        break;
                    case CASE_W_0x19: /* SBB Ew,Gw */
                        // RMEwGw(SBBW);
                        RMEwGw(this::SBBW, this::SBBW_M);
                        break;
                    case CASE_W_0x1a:
                    case CASE_D_0x1a: /* SBB Gb,Eb */
                        // RMGbEb(SBBB);
                        RMGbEb(this::SBBBL, this::SBBBH);
                        break;
                    case CASE_W_0x1b: /* SBB Gw,Ew */
                        // RMGwEw(SBBW);
                        RMGwEw(this::SBBW);
                        break;
                    case CASE_W_0x1c:
                    case CASE_D_0x1c: /* SBB AL,Ib */
                        // ALIb(SBBB);
                        SBBBL(Register.AX, fetchB());
                        break;
                    case CASE_W_0x1d: /* SBB AX,Iw */
                        // AXIw(SBBW);
                        SBBW(Register.AX, fetchW());
                        break;
                    case CASE_W_0x1e: /* PUSH DS */
                        // Push_16(SegValue(ds));
                        CPU.push16(Register.segValue(Register.SEG_NAME_DS));
                        break;
                    case CASE_W_0x1f: /* POP DS */
                        // if (CPU_PopSeg(ds,false)) {
                        // cpuModule.CPU_Exception(cpuModule.cpu.exception.which,
                        // cpuModule.cpu.exception.error); continue; };
                        if (CPU.popSeg(Register.SEG_NAME_DS, false)) {
                            CPU.exception(CPU.Block.Exception.Which, CPU.Block.Exception.Error);
                            continue main_loop;// org_continue;
                        }
                        break;
                    case CASE_W_0x20:
                    case CASE_D_0x20: /* AND Eb,Gb */
                        // RMEbGb(ANDB);
                        RMEbGb(this::ANDBL, this::ANDBH, this::ANDB);
                        break;
                    case CASE_W_0x21: /* AND Ew,Gw */
                        // RMEwGw(ANDW);
                        RMEwGw(this::ANDW, this::ANDW_M);
                        break;
                    case CASE_W_0x22:
                    case CASE_D_0x22: /* AND Gb,Eb */
                        // RMGbEb(ANDB);
                        RMGbEb(this::ANDBL, this::ANDBH);
                        break;
                    case CASE_W_0x23: /* AND Gw,Ew */
                        // RMGwEw(ANDW);
                        RMGwEw(this::ANDW);
                        break;
                    case CASE_W_0x24:
                    case CASE_D_0x24: /* AND AL,Ib */
                        // ALIb(ANDB);
                        ANDBL(Register.AX, fetchB());
                        break;
                    case CASE_W_0x25: /* AND AX,Iw */
                        // AXIw(ANDW);
                        ANDW(Register.AX, fetchW());
                        break;
                    case CASE_W_0x26:
                    case CASE_D_0x26: /* SEG ES: */
                        // DO_PREFIX_SEG(es);
                        Core.BaseDS = Register.segPhys(Register.SEG_NAME_ES);
                        Core.BaseSS = Register.segPhys(Register.SEG_NAME_ES);
                        Core.BaseValDS = Register.SEG_NAME_ES;
                        continue restart_opcode; // goto restart_opcode;
                    // break;
                    case CASE_W_0x27:
                    case CASE_D_0x27: /* DAA */
                        DAA();
                        break;
                    case CASE_W_0x28:
                    case CASE_D_0x28: /* SUB Eb,Gb */
                        // RMEbGb(SUBB);
                        RMEbGb(this::SUBBL, this::SUBBH, this::SUBB);
                        break;
                    case CASE_W_0x29: /* SUB Ew,Gw */
                        // RMEwGw(SUBW);
                        RMEwGw(this::SUBW, this::SUBW_M);
                        break;
                    case CASE_W_0x2a:
                    case CASE_D_0x2a: /* SUB Gb,Eb */
                        // RMGbEb(SUBB);
                        RMGbEb(this::SUBBL, this::SUBBH);
                        break;
                    case CASE_W_0x2b: /* SUB Gw,Ew */
                        // RMGwEw(SUBW);
                        RMGwEw(this::SUBW);
                        break;
                    case CASE_W_0x2c:
                    case CASE_D_0x2c: /* SUB AL,Ib */
                        // ALIb(SUBB);
                        SUBBL(Register.AX, fetchB());
                        break;
                    case CASE_W_0x2d: /* SUB AX,Iw */
                        // AXIw(SUBW);
                        SUBW(Register.AX, fetchW());
                        break;
                    case CASE_W_0x2e:
                    case CASE_D_0x2e: /* SEG CS: */
                        // DO_PREFIX_SEG(cs);
                        Core.BaseDS = Register.segPhys(Register.SEG_NAME_CS);
                        Core.BaseSS = Register.segPhys(Register.SEG_NAME_CS);
                        Core.BaseValDS = Register.SEG_NAME_CS;
                        continue restart_opcode; // goto restart_opcode;
                    // break;
                    case CASE_W_0x2f:
                    case CASE_D_0x2f: /* DAS */
                        DAS();
                        break;
                    case CASE_W_0x30:
                    case CASE_D_0x30: /* XOR Eb,Gb */
                        // RMEbGb(XORB);
                        RMEbGb(this::XORBL, this::XORBH, this::XORB);
                        break;
                    case CASE_W_0x31: /* XOR Ew,Gw */
                        // RMEwGw(XORW);
                        RMEwGw(this::XORW, this::XORW_M);
                        break;
                    case CASE_W_0x32:
                    case CASE_D_0x32: /* XOR Gb,Eb */
                        // RMGbEb(XORB);
                        RMGbEb(this::XORBL, this::XORBH);
                        break;
                    case CASE_W_0x33: /* XOR Gw,Ew */
                        // RMGwEw(XORW);
                        RMGwEw(this::XORW);
                        break;
                    case CASE_W_0x34:
                    case CASE_D_0x34: /* XOR AL,Ib */
                        // ALIb(XORB);
                        XORBL(Register.AX, fetchB());
                        break;
                    case CASE_W_0x35: /* XOR AX,Iw */
                        // AXIw(XORW);
                        XORW(Register.AX, fetchW());
                        break;
                    case CASE_W_0x36:
                    case CASE_D_0x36: /* SEG SS: */
                        // DO_PREFIX_SEG(ss);
                        Core.BaseDS = Register.segPhys(Register.SEG_NAME_SS);
                        Core.BaseSS = Register.segPhys(Register.SEG_NAME_SS);
                        Core.BaseValDS = Register.SEG_NAME_SS;
                        continue restart_opcode; // goto restart_opcode;
                    // break;
                    case CASE_W_0x37:
                    case CASE_D_0x37: /* AAA */
                        AAA();
                        break;
                    case CASE_W_0x38:
                    case CASE_D_0x38: /* CMP Eb,Gb */
                        // RMEbGb(CMPB);
                        RMEbGb(this::CMPBL, this::CMPBH, this::CMPB);
                        break;
                    case CASE_W_0x39: /* CMP Ew,Gw */
                        // RMEwGw(CMPW);
                        RMEwGw(this::CMPW, this::CMPW_M);
                        break;
                    case CASE_W_0x3a:
                    case CASE_D_0x3a: /* CMP Gb,Eb */
                        // RMGbEb(CMPB);
                        RMGbEb(this::CMPBL, this::CMPBH);
                        break;
                    case CASE_W_0x3b: /* CMP Gw,Ew */
                        // RMGwEw(CMPW);
                        RMGwEw(this::CMPW);
                        break;
                    case CASE_W_0x3c:
                    case CASE_D_0x3c: /* CMP AL,Ib */
                        // ALIb(CMPB);
                        CMPBL(Register.AX, fetchB());
                        break;
                    case CASE_W_0x3d: /* CMP AX,Iw */
                        // AXIw(CMPW);
                        CMPW(Register.AX, fetchW());
                        break;
                    case CASE_W_0x3e:
                    case CASE_D_0x3e: /* SEG DS: */
                        // DO_PREFIX_SEG(ds);
                        Core.BaseDS = Register.segPhys(Register.SEG_NAME_DS);
                        Core.BaseSS = Register.segPhys(Register.SEG_NAME_DS);
                        Core.BaseValDS = Register.SEG_NAME_DS;
                        continue restart_opcode; // goto restart_opcode;
                    // break;
                    case CASE_W_0x3f:
                    case CASE_D_0x3f: /* AAS */
                        AAS();
                        break;
                    case CASE_W_0x40: /* INC AX */
                        INCW(Register.AX);
                        break;
                    case CASE_W_0x41: /* INC CX */
                        INCW(Register.CX);
                        break;
                    case CASE_W_0x42: /* INC DX */
                        INCW(Register.DX);
                        break;
                    case CASE_W_0x43: /* INC BX */
                        INCW(Register.BX);
                        break;
                    case CASE_W_0x44: /* INC SP */
                        INCW(Register.SP);
                        break;
                    case CASE_W_0x45: /* INC BP */
                        INCW(Register.BP);
                        break;
                    case CASE_W_0x46: /* INC SI */
                        INCW(Register.SI);
                        break;
                    case CASE_W_0x47: /* INC DI */
                        INCW(Register.DI);
                        break;
                    case CASE_W_0x48: /* DEC AX */
                        DECW(Register.AX);
                        break;
                    case CASE_W_0x49: /* DEC CX */
                        DECW(Register.CX);
                        break;
                    case CASE_W_0x4a: /* DEC DX */
                        DECW(Register.DX);
                        break;
                    case CASE_W_0x4b: /* DEC BX */
                        DECW(Register.BX);
                        break;
                    case CASE_W_0x4c: /* DEC SP */
                        DECW(Register.SP);
                        break;
                    case CASE_W_0x4d: /* DEC BP */
                        DECW(Register.BP);
                        break;
                    case CASE_W_0x4e: /* DEC SI */
                        DECW(Register.SI);
                        break;
                    case CASE_W_0x4f: /* DEC DI */
                        DECW(Register.DI);
                        break;
                    case CASE_W_0x50: /* PUSH AX */
                        CPU.push16(Register.getRegAX());
                        break;
                    case CASE_W_0x51: /* PUSH CX */
                        CPU.push16(Register.getRegCX());
                        break;
                    case CASE_W_0x52: /* PUSH DX */
                        CPU.push16(Register.getRegDX());
                        break;
                    case CASE_W_0x53: /* PUSH BX */
                        CPU.push16(Register.getRegBX());
                        break;
                    case CASE_W_0x54: /* PUSH SP */
                        CPU.push16(Register.getRegSP());
                        break;
                    case CASE_W_0x55: /* PUSH BP */
                        CPU.push16(Register.getRegBP());
                        break;
                    case CASE_W_0x56: /* PUSH SI */
                        CPU.push16(Register.getRegSI());
                        break;
                    case CASE_W_0x57: /* PUSH DI */
                        CPU.push16(Register.getRegDI());
                        break;
                    case CASE_W_0x58: /* POP AX */
                        Register.setRegAX(CPU.pop16());
                        break;
                    case CASE_W_0x59: /* POP CX */
                        Register.setRegCX(CPU.pop16());
                        break;
                    case CASE_W_0x5a: /* POP DX */
                        Register.setRegDX(CPU.pop16());
                        break;
                    case CASE_W_0x5b: /* POP BX */
                        Register.setRegBX(CPU.pop16());
                        break;
                    case CASE_W_0x5c: /* POP SP */
                        Register.setRegSP(CPU.pop16());
                        break;
                    case CASE_W_0x5d: /* POP BP */
                        Register.setRegBP(CPU.pop16());
                        break;
                    case CASE_W_0x5e: /* POP SI */
                        Register.setRegSI(CPU.pop16());
                        break;
                    case CASE_W_0x5f: /* POP DI */
                        Register.setRegDI(CPU.pop16());
                        break;
                    case CASE_W_0x60: /* PUSHA */
                    {
                        int oldSP = Register.getRegSP();
                        CPU.push16(Register.getRegAX());
                        CPU.push16(Register.getRegCX());
                        CPU.push16(Register.getRegDX());
                        CPU.push16(Register.getRegBX());
                        CPU.push16(oldSP);
                        CPU.push16(Register.getRegBP());
                        CPU.push16(Register.getRegSI());
                        CPU.push16(Register.getRegDI());
                    }
                        break;
                    case CASE_W_0x61: /* POPA */
                        Register.setRegDI(CPU.pop16());
                        Register.setRegSI(CPU.pop16());
                        Register.setRegBP(CPU.pop16());
                        CPU.pop16();// Don't save SP
                        Register.setRegBX(CPU.pop16());
                        Register.setRegDX(CPU.pop16());
                        Register.setRegCX(CPU.pop16());
                        Register.setRegAX(CPU.pop16());
                        break;
                    case CASE_W_0x62: /* BOUND */
                    {
                        short boundMin, boundMax;
                        int rm = fetchB();
                        int rmrw = Register.Regs[lookupRMregw[rm]].getWord();
                        int eaa = Core.EATable[rm].get();
                        boundMin = (short) Memory.readW(eaa);
                        boundMax = (short) Memory.readW(eaa + 2);
                        if ((((short) rmrw) < boundMin) || (((short) rmrw) > boundMax)) {
                            CPU.exception(5);
                            continue main_loop;// org_continue;
                        }
                    }
                        break;
                    case CASE_W_0x63: /* ARPL Ew,Rw */
                    {
                        if ((Register.Flags & Register.FlagVM) != 0 || (!CPU.Block.PMode)) {
                            illegalOpCode(); // goto illegal_opcode
                            continue main_loop;
                        }
                        int rm = fetchB();
                        int rmrw = Register.Regs[lookupRMregw[rm]].getWord();
                        if (rm >= 0xc0) {
                            int earwRegId = lookupRMEAregw[rm];
                            int newSel = Register.Regs[earwRegId].getWord();
                            newSel = CPU.arpl(newSel, rmrw);
                            Register.Regs[earwRegId].setWord(newSel);
                        } else {
                            int eaa = Core.EATable[rm].get();
                            int newSel = Memory.readW(eaa);
                            newSel = CPU.arpl(newSel, rmrw);
                            Memory.writeW(eaa, newSel);
                        }
                    }
                        break;
                    case CASE_W_0x64:
                    case CASE_D_0x64: /* SEG FS: */
                        // DO_PREFIX_SEG(fs);
                        Core.BaseDS = Register.segPhys(Register.SEG_NAME_FS);
                        Core.BaseSS = Register.segPhys(Register.SEG_NAME_FS);
                        Core.BaseValDS = Register.SEG_NAME_FS;
                        continue restart_opcode; // goto restart_opcode;
                    // break;
                    case CASE_W_0x65:
                    case CASE_D_0x65: /* SEG GS: */
                        // DO_PREFIX_SEG(gs);
                        Core.BaseDS = Register.segPhys(Register.SEG_NAME_GS);
                        Core.BaseSS = Register.segPhys(Register.SEG_NAME_GS);
                        Core.BaseValDS = Register.SEG_NAME_GS;
                        continue restart_opcode; // goto restart_opcode;
                    // break;
                    case CASE_W_0x66:
                    case CASE_D_0x66: /* Operand Size Prefix */
                        Core.OPCodeIndex = (Convert.toByte(CPU.Block.Code.Big) ^ 0x1) * 0x200;
                        continue restart_opcode; // goto restart_opcode;
                    case CASE_W_0x67:
                    case CASE_D_0x67: /* Address Size Prefix */
                        // DO_PREFIX_ADDR();
                        Core.Prefixes = (Core.Prefixes & ~PrefixAddr)
                                | (Convert.toByte(CPU.Block.Code.Big) ^ PrefixAddr);
                        Core.EATable = EATable[Core.Prefixes & 1];
                        continue restart_opcode; // goto restart_opcode;
                    case CASE_W_0x68: /* PUSH Iw */
                        CPU.push16(fetchW());
                        break;
                    case CASE_W_0x69: /* IMUL Gw,Ew,Iw */
                    // RMGwEwOp3(DIMULW, Fetchws());
                    {
                        int rm = fetchB();
                        int regId = lookupRMregw[rm];
                        if (rm >= 0xc0) {
                            DIMULW(regId, Register.Regs[lookupRMEAregw[rm]].getWord(), fetchWS());
                        } else {
                            DIMULW(regId, Memory.readW(Core.EATable[rm].get()), fetchWS());
                        }
                    }
                        break;
                    case CASE_W_0x6a: /* PUSH Ib */
                        CPU.push16(fetchBS());
                        break;
                    case CASE_W_0x6b: /* IMUL Gw,Ew,Ib */
                    // RMGwEwOp3(DIMULW, Fetchbs());
                    {
                        int rm = fetchB();
                        int regId = lookupRMregw[rm];
                        if (rm >= 0xc0) {
                            DIMULW(regId, Register.Regs[lookupRMEAregw[rm]].getWord(), fetchBS());
                        } else {
                            DIMULW(regId, Memory.readW(Core.EATable[rm].get()), fetchBS());
                        }

                    }
                        break;
                    case CASE_W_0x6c:
                    case CASE_D_0x6c: /* INSB */
                        if (CPU.ioException(Register.getRegDX(), 1)) {
                            CPU.exception(CPU.Block.Exception.Which, CPU.Block.Exception.Error);
                            continue main_loop;// org_continue;
                        }
                        DoString(STRING_OP_INSB);
                        break;
                    case CASE_W_0x6d: /* INSW */
                        if (CPU.ioException(Register.getRegDX(), 2)) {
                            CPU.exception(CPU.Block.Exception.Which, CPU.Block.Exception.Error);
                            continue main_loop;// org_continue;
                        }
                        DoString(STRING_OP_INSW);
                        break;
                    case CASE_W_0x6e:
                    case CASE_D_0x6e: /* OUTSB */
                        if (CPU.ioException(Register.getRegDX(), 1)) {
                            CPU.exception(CPU.Block.Exception.Which, CPU.Block.Exception.Error);
                            continue main_loop;// org_continue;
                        }
                        DoString(STRING_OP_OUTSB);
                        break;
                    case CASE_W_0x6f: /* OUTSW */
                        if (CPU.ioException(Register.getRegDX(), 2)) {
                            CPU.exception(CPU.Block.Exception.Which, CPU.Block.Exception.Error);
                            continue main_loop;// org_continue;
                        }
                        DoString(STRING_OP_OUTSW);
                        break;
                    case CASE_W_0x70: /* JO */
                    // JumpCond16_b(TFLG_O);
                    {
                        saveIP();
                        if (Flags.getTFlgO())
                            Register.setRegIP(Register.getRegIP() + fetchBS());
                        Register.setRegIP(Register.getRegIP() + 1);
                        continue main_loop;// org_continue;
                    }
                    // break;
                    case CASE_W_0x71: /* JNO */
                    // JumpCond16_b(TFLG_NO);
                    {
                        saveIP();
                        if (Flags.getTFlgNO())
                            Register.setRegIP(Register.getRegIP() + fetchBS());
                        Register.setRegIP(Register.getRegIP() + 1);
                        continue main_loop;// org_continue;
                    }
                    // break;
                    case CASE_W_0x72: /* JB */
                    // JumpCond16_b(TFLG_B);
                    {
                        saveIP();
                        if (Flags.getTFlgB())
                            Register.setRegIP(Register.getRegIP() + fetchBS());
                        Register.setRegIP(Register.getRegIP() + 1);
                        continue main_loop;// org_continue;
                    }
                    // break;
                    case CASE_W_0x73: /* JNB */
                    // JumpCond16_b(TFLG_NB);
                    {
                        saveIP();
                        if (Flags.getTFlgNB())
                            Register.setRegIP(Register.getRegIP() + fetchBS());
                        Register.setRegIP(Register.getRegIP() + 1);
                        continue main_loop;// org_continue;
                    }
                    // break;
                    case CASE_W_0x74: /* JZ */
                    // JumpCond16_b(TFLG_Z);
                    {
                        saveIP();
                        if (Flags.getTFlgZ())
                            Register.setRegIP(Register.getRegIP() + fetchBS());
                        Register.setRegIP(Register.getRegIP() + 1);
                        continue main_loop;// org_continue;
                    }
                    // break;
                    case CASE_W_0x75: /* JNZ */
                    // JumpCond16_b(TFLG_NZ);
                    {
                        saveIP();
                        if (Flags.getTFlgNZ())
                            Register.setRegIP(Register.getRegIP() + fetchBS());
                        Register.setRegIP(Register.getRegIP() + 1);
                        continue main_loop;// org_continue;
                    }
                    // break;
                    case CASE_W_0x76: /* JBE */
                    // JumpCond16_b(TFLG_BE);
                    {
                        saveIP();
                        if (Flags.getTFlgBE())
                            Register.setRegIP(Register.getRegIP() + fetchBS());
                        Register.setRegIP(Register.getRegIP() + 1);
                        continue main_loop;// org_continue;
                    }
                    // break;
                    case CASE_W_0x77: /* JNBE */
                    // JumpCond16_b(TFLG_NBE);
                    {
                        saveIP();
                        if (Flags.getTFlgNBE())
                            Register.setRegIP(Register.getRegIP() + fetchBS());
                        Register.setRegIP(Register.getRegIP() + 1);
                        continue main_loop;// org_continue;
                    }
                    // break;
                    case CASE_W_0x78: /* JS */
                    // JumpCond16_b(TFLG_S);
                    {
                        saveIP();
                        if (Flags.getTFlgS())
                            Register.setRegIP(Register.getRegIP() + fetchBS());
                        Register.setRegIP(Register.getRegIP() + 1);
                        continue main_loop;// org_continue;
                    }
                    // break;
                    case CASE_W_0x79: /* JNS */
                    // JumpCond16_b(TFLG_NS);
                    {
                        saveIP();
                        if (Flags.getTFlgNS())
                            Register.setRegIP(Register.getRegIP() + fetchBS());
                        Register.setRegIP(Register.getRegIP() + 1);
                        continue main_loop;// org_continue;
                    }
                    // break;
                    case CASE_W_0x7a: /* JP */
                    // JumpCond16_b(TFLG_P);
                    {
                        saveIP();
                        if (Flags.getTFlgP())
                            Register.setRegIP(Register.getRegIP() + fetchBS());
                        Register.setRegIP(Register.getRegIP() + 1);
                        continue main_loop;// org_continue;
                    }
                    // break;
                    case CASE_W_0x7b: /* JNP */
                    // JumpCond16_b(TFLG_NP);
                    {
                        saveIP();
                        if (Flags.getTFlgNP())
                            Register.setRegIP(Register.getRegIP() + fetchBS());
                        Register.setRegIP(Register.getRegIP() + 1);
                        continue main_loop;// org_continue;
                    }
                    // break;
                    case CASE_W_0x7c: /* JL */
                    // JumpCond16_b(TFLG_L);
                    {
                        saveIP();
                        if (Flags.getTFlgL())
                            Register.setRegIP(Register.getRegIP() + fetchBS());
                        Register.setRegIP(Register.getRegIP() + 1);
                        continue main_loop;// org_continue;
                    }
                    // break;
                    case CASE_W_0x7d: /* JNL */
                    // JumpCond16_b(TFLG_NL);
                    {
                        saveIP();
                        if (Flags.getTFlgNL())
                            Register.setRegIP(Register.getRegIP() + fetchBS());
                        Register.setRegIP(Register.getRegIP() + 1);
                        continue main_loop;// org_continue;
                    }
                    // break;
                    case CASE_W_0x7e: /* JLE */
                    // JumpCond16_b(TFLG_LE);
                    {
                        saveIP();
                        if (Flags.getTFlgLE())
                            Register.setRegIP(Register.getRegIP() + fetchBS());
                        Register.setRegIP(Register.getRegIP() + 1);
                        continue main_loop;// org_continue;
                    }
                    // break;
                    case CASE_W_0x7f: /* JNLE */
                    // JumpCond16_b(TFLG_NLE);
                    {
                        saveIP();
                        if (Flags.getTFlgNLE())
                            Register.setRegIP(Register.getRegIP() + fetchBS());
                        Register.setRegIP(Register.getRegIP() + 1);
                        continue main_loop;// org_continue;
                    }
                    // break;
                    case CASE_W_0x80:
                    case CASE_D_0x80: /* Grpl Eb,Ib */
                    case CASE_W_0x82:
                    case CASE_D_0x82: /* Grpl Eb,Ib Mirror instruction */
                    {
                        int rm = fetchB();
                        int which = (rm >>> 3) & 7;
                        if (rm >= 0xc0) {
                            int regId = lookupRMEAregbl[rm];
                            int ib = fetchB();
                            if (regId >= 0) {
                                switch (which) {
                                    case 0x00:
                                        ADDBL(regId, ib);
                                        break;
                                    case 0x01:
                                        ORBL(regId, ib);
                                        break;
                                    case 0x02:
                                        ADCBL(regId, ib);
                                        break;
                                    case 0x03:
                                        SBBBL(regId, ib);
                                        break;
                                    case 0x04:
                                        ANDBL(regId, ib);
                                        break;
                                    case 0x05:
                                        SUBBL(regId, ib);
                                        break;
                                    case 0x06:
                                        XORBL(regId, ib);
                                        break;
                                    case 0x07:
                                        CMPBL(regId, ib);
                                        break;
                                }
                            } else {
                                regId = lookupRMEAregbh[rm];
                                switch (which) {
                                    case 0x00:
                                        ADDBH(regId, ib);
                                        break;
                                    case 0x01:
                                        ORBH(regId, ib);
                                        break;
                                    case 0x02:
                                        ADCBH(regId, ib);
                                        break;
                                    case 0x03:
                                        SBBBH(regId, ib);
                                        break;
                                    case 0x04:
                                        ANDBH(regId, ib);
                                        break;
                                    case 0x05:
                                        SUBBH(regId, ib);
                                        break;
                                    case 0x06:
                                        XORBH(regId, ib);
                                        break;
                                    case 0x07:
                                        CMPBH(regId, ib);
                                        break;
                                }
                            }
                        } else {
                            int eaa = Core.EATable[rm].get();
                            int ib = fetchB();
                            switch (which) {
                                case 0x00:
                                    ADDB(eaa, ib);
                                    break;
                                case 0x01:
                                    ORB(eaa, ib);
                                    break;
                                case 0x02:
                                    ADCB(eaa, ib);
                                    break;
                                case 0x03:
                                    SBBB(eaa, ib);
                                    break;
                                case 0x04:
                                    ANDB(eaa, ib);
                                    break;
                                case 0x05:
                                    SUBB(eaa, ib);
                                    break;
                                case 0x06:
                                    XORB(eaa, ib);
                                    break;
                                case 0x07:
                                    CMPB(eaa, ib);
                                    break;
                            }
                        }
                        break;
                    }
                    case CASE_W_0x81: /* Grpl Ew,Iw */
                    {
                        int rm = fetchB();
                        int which = (rm >>> 3) & 7;
                        if (rm >= 0xc0) {
                            int regId = lookupRMEAregw[rm];
                            int iw = fetchW();
                            switch (which) {
                                case 0x00:
                                    ADDW(regId, iw);
                                    break;
                                case 0x01:
                                    ORW(regId, iw);
                                    break;
                                case 0x02:
                                    ADCW(regId, iw);
                                    break;
                                case 0x03:
                                    SBBW(regId, iw);
                                    break;
                                case 0x04:
                                    ANDW(regId, iw);
                                    break;
                                case 0x05:
                                    SUBW(regId, iw);
                                    break;
                                case 0x06:
                                    XORW(regId, iw);
                                    break;
                                case 0x07:
                                    CMPW(regId, iw);
                                    break;
                            }
                        } else {
                            int eaa = Core.EATable[rm].get();
                            int iw = fetchW();
                            switch (which) {
                                case 0x00:
                                    ADDW_M(eaa, iw);
                                    break;
                                case 0x01:
                                    ORW_M(eaa, iw);
                                    break;
                                case 0x02:
                                    ADCW_M(eaa, iw);
                                    break;
                                case 0x03:
                                    SBBW_M(eaa, iw);
                                    break;
                                case 0x04:
                                    ANDW_M(eaa, iw);
                                    break;
                                case 0x05:
                                    SUBW_M(eaa, iw);
                                    break;
                                case 0x06:
                                    XORW_M(eaa, iw);
                                    break;
                                case 0x07:
                                    CMPW_M(eaa, iw);
                                    break;
                            }
                        }
                        break;
                    }
                    case CASE_W_0x83: /* Grpl Ew,Ix */
                    {
                        int rm = fetchB();
                        int which = (rm >>> 3) & 7;
                        if (rm >= 0xc0) {
                            int regId = lookupRMEAregw[rm];
                            int iw = 0xffff & (short) fetchBS();
                            switch (which) {
                                case 0x00:
                                    ADDW(regId, iw);
                                    break;
                                case 0x01:
                                    ORW(regId, iw);
                                    break;
                                case 0x02:
                                    ADCW(regId, iw);
                                    break;
                                case 0x03:
                                    SBBW(regId, iw);
                                    break;
                                case 0x04:
                                    ANDW(regId, iw);
                                    break;
                                case 0x05:
                                    SUBW(regId, iw);
                                    break;
                                case 0x06:
                                    XORW(regId, iw);
                                    break;
                                case 0x07:
                                    CMPW(regId, iw);
                                    break;
                            }
                        } else {
                            int eaa = Core.EATable[rm].get();
                            int iw = 0xffff & (short) fetchBS();
                            switch (which) {
                                case 0x00:
                                    ADDW_M(eaa, iw);
                                    break;
                                case 0x01:
                                    ORW_M(eaa, iw);
                                    break;
                                case 0x02:
                                    ADCW_M(eaa, iw);
                                    break;
                                case 0x03:
                                    SBBW_M(eaa, iw);
                                    break;
                                case 0x04:
                                    ANDW_M(eaa, iw);
                                    break;
                                case 0x05:
                                    SUBW_M(eaa, iw);
                                    break;
                                case 0x06:
                                    XORW_M(eaa, iw);
                                    break;
                                case 0x07:
                                    CMPW_M(eaa, iw);
                                    break;
                            }
                        }
                        break;
                    }
                    case CASE_W_0x84:
                    case CASE_D_0x84: /* TEST Eb,Gb */
                        RMEbGb(this::TESTBL, this::TESTBH, this::TESTB);
                        break;
                    case CASE_W_0x85: /* TEST Ew,Gw */
                        RMEwGw(this::TESTW, this::TESTW_M);
                        break;
                    case CASE_W_0x86:
                    case CASE_D_0x86: /* XCHG Eb,Gb */
                    {
                        int rm = fetchB();
                        int rmrbRegId = lookupRMregbl[rm];
                        int oldrmrb = 0;
                        if (rm >= 0xc0) {
                            int earbRegId = lookupRMEAregbl[rm];
                            if (rmrbRegId >= 0) {
                                oldrmrb = Register.Regs[rmrbRegId].getByteL();
                                if (earbRegId >= 0) {
                                    Register.Regs[rmrbRegId]
                                            .setByteL(Register.Regs[earbRegId].getByteL());
                                    Register.Regs[earbRegId].setByteL(oldrmrb);
                                } else {
                                    earbRegId = lookupRMEAregbh[rm];
                                    Register.Regs[rmrbRegId]
                                            .setByteL(Register.Regs[earbRegId].getByteH());
                                    Register.Regs[earbRegId].setByteH(oldrmrb);
                                }
                            } else {
                                rmrbRegId = lookupRMregbh[rm];
                                oldrmrb = Register.Regs[rmrbRegId].getByteH();
                                if (earbRegId >= 0) {
                                    Register.Regs[rmrbRegId]
                                            .setByteH(Register.Regs[earbRegId].getByteL());
                                    Register.Regs[earbRegId].setByteL(oldrmrb);
                                } else {
                                    earbRegId = lookupRMEAregbh[rm];
                                    Register.Regs[rmrbRegId]
                                            .setByteH(Register.Regs[earbRegId].getByteH());
                                    Register.Regs[earbRegId].setByteH(oldrmrb);
                                }
                            }
                        } else {
                            int eaa = Core.EATable[rm].get();
                            if (rmrbRegId >= 0) {
                                oldrmrb = Register.Regs[rmrbRegId].getByteL();
                                Register.Regs[rmrbRegId].setByteL(Memory.readB(eaa));
                            } else {
                                rmrbRegId = lookupRMregbh[rm];
                                oldrmrb = Register.Regs[rmrbRegId].getByteH();
                                Register.Regs[rmrbRegId].setByteH(Memory.readB(eaa));
                            }
                            Memory.writeB(eaa, oldrmrb);
                        }
                        break;
                    }
                    case CASE_W_0x87: /* XCHG Ew,Gw */
                    {
                        int rm = fetchB();
                        int rmrwRegId = lookupRMregw[rm];
                        int oldrmrw = Register.Regs[rmrwRegId].getWord();
                        if (rm >= 0xc0) {
                            int earwRegId = lookupRMEAregw[rm];
                            Register.Regs[rmrwRegId].setWord(Register.Regs[earwRegId].getWord());
                            Register.Regs[earwRegId].setWord(oldrmrw);
                        } else {
                            int eaa = Core.EATable[rm].get();
                            Register.Regs[rmrwRegId].setWord(Memory.readW(eaa));
                            Memory.writeW(eaa, oldrmrw);
                        }
                        break;
                    }
                    case CASE_W_0x88:
                    case CASE_D_0x88: /* MOV Eb,Gb */
                    {
                        int rm = fetchB();
                        int rmrbRegId = lookupRMregbl[rm];
                        int rmrb = rmrbRegId >= 0 ? Register.Regs[rmrbRegId].getByteL()
                                : Register.Regs[lookupRMregbh[rm]].getByteH();
                        if (rm >= 0xc0) {
                            int earbRegId = lookupRMEAregbl[rm];
                            if (earbRegId >= 0) {
                                Register.Regs[earbRegId].setByteL(rmrb);
                            } else {
                                earbRegId = lookupRMEAregbh[rm];
                                Register.Regs[earbRegId].setByteH(rmrb);
                            }

                        } else {
                            if (CPU.Block.PMode) {
                                if ((rm == 0x05) && (!CPU.Block.Code.Big)) {
                                    Descriptor desc = new Descriptor();
                                    CPU.Block.GDT.getDescriptor(Register.segValue(Core.BaseValDS),
                                            desc);
                                    if ((desc.type() == CPU.DESC_CODE_R_NC_A)
                                            || (desc.type() == CPU.DESC_CODE_R_NC_NA)) {
                                        CPU.exception(CPU.ExceptionGP,
                                                Register.segValue(Core.BaseValDS) & 0xfffc);
                                        continue main_loop;// org_continue;
                                    }
                                }
                            }
                            int eaa = Core.EATable[rm].get();
                            Memory.writeB(eaa, rmrb);
                        }
                        break;
                    }
                    case CASE_W_0x89: /* MOV Ew,Gw */
                    {
                        int rm = fetchB();
                        int rmrw = Register.Regs[lookupRMregw[rm]].getWord();
                        if (rm >= 0xc0) {
                            int earwRegId = lookupRMEAregw[rm];
                            Register.Regs[earwRegId].setWord(rmrw);
                        } else {
                            int eaa = Core.EATable[rm].get();
                            Memory.writeW(eaa, rmrw);
                        }
                        break;
                    }
                    case CASE_W_0x8a:
                    case CASE_D_0x8a: /* MOV Gb,Eb */
                    {
                        int rm = fetchB();
                        int rmrbRegId = lookupRMregbl[rm];
                        if (rm >= 0xc0) {
                            int earbRegId = lookupRMEAregbl[rm];
                            int earb = earbRegId >= 0 ? Register.Regs[earbRegId].getByteL()
                                    : Register.Regs[lookupRMEAregbh[rm]].getByteH();
                            if (rmrbRegId >= 0) {
                                Register.Regs[rmrbRegId].setByteL(earb);
                            } else {
                                rmrbRegId = lookupRMregbh[rm];
                                Register.Regs[rmrbRegId].setByteH(earb);
                            }
                        } else {
                            int eaa = Core.EATable[rm].get();
                            if (rmrbRegId >= 0) {
                                Register.Regs[rmrbRegId].setByteL(Memory.readB(eaa));
                            } else {
                                rmrbRegId = lookupRMregbh[rm];
                                Register.Regs[rmrbRegId].setByteH(Memory.readB(eaa));
                            }
                        }
                        break;
                    }
                    case CASE_W_0x8b: /* MOV Gw,Ew */
                    {
                        int rm = fetchB();
                        int rmrwRegId = lookupRMregw[rm];
                        if (rm >= 0xc0) {
                            Register.Regs[rmrwRegId]
                                    .setWord(Register.Regs[lookupRMEAregw[rm]].getWord());
                        } else {
                            Register.Regs[rmrwRegId].setWord(Memory.readW(Core.EATable[rm].get()));
                        }
                        break;
                    }
                    case CASE_W_0x8c: /* Mov Ew,Sw */
                    {
                        int rm = fetchB();
                        int val;
                        int which = (rm >>> 3) & 7;
                        switch (which) {
                            case 0x00: /* MOV Ew,ES */
                                val = Register.segValue(Register.SEG_NAME_ES);
                                break;
                            case 0x01: /* MOV Ew,CS */
                                val = Register.segValue(Register.SEG_NAME_CS);
                                break;
                            case 0x02: /* MOV Ew,SS */
                                val = Register.segValue(Register.SEG_NAME_SS);
                                break;
                            case 0x03: /* MOV Ew,DS */
                                val = Register.segValue(Register.SEG_NAME_DS);
                                break;
                            case 0x04: /* MOV Ew,FS */
                                val = Register.segValue(Register.SEG_NAME_FS);
                                break;
                            case 0x05: /* MOV Ew,GS */
                                val = Register.segValue(Register.SEG_NAME_GS);
                                break;
                            default:
                                Log.logging(Log.LogTypes.CPU, Log.LogServerities.Error,
                                        "CPU:8c:Illegal RM Byte"); {
                                illegalOpCode(); // goto illegal_opcode
                                continue main_loop;
                            }
                        }
                        if (rm >= 0xc0) {
                            int earwRegId = lookupRMEAregw[rm];
                            Register.Regs[earwRegId].setWord(val);
                        } else {
                            Memory.writeW(Core.EATable[rm].get(), val);
                        }
                        break;
                    }
                    case CASE_W_0x8d: /* LEA Gw */
                    {
                        // Little hack to always use segprefixed version
                        Core.BaseDS = Core.BaseSS = 0;
                        int rm = fetchB();
                        int rmrwRegId = lookupRMregw[rm];
                        if ((Core.Prefixes & PrefixAddr) != 0) {
                            Register.Regs[rmrwRegId].setWord((EATable[1][rm]).get());
                        } else {
                            Register.Regs[rmrwRegId].setWord((EATable[0][rm]).get());
                        }
                        break;
                    }
                    case CASE_W_0x8e:
                    case CASE_D_0x8e: /* MOV Sw,Ew */
                    {
                        int rm = fetchB();
                        int val;
                        int which = (rm >>> 3) & 7;
                        if (rm >= 0xc0) {
                            val = Register.Regs[lookupRMEAregw[rm]].getWord();
                        } else {
                            val = Memory.physReadW(Core.EATable[rm].get());
                        }
                        switch (which) {
                            case 0x02: /* MOV SS,Ew */
                                CPU.Cycles++; // Always do another instruction
                                if (CPU.setSegGeneral(which, val)) {
                                    CPU.exception(CPU.Block.Exception.Which,
                                            CPU.Block.Exception.Error);
                                    continue main_loop;// org_continue;
                                }
                                break;
                            case 0x00: /* MOV ES,Ew */
                            case 0x03: /* MOV DS,Ew */
                            case 0x05: /* MOV GS,Ew */
                            case 0x04: /* MOV FS,Ew */
                                if (CPU.setSegGeneral(which, val)) {
                                    CPU.exception(CPU.Block.Exception.Which,
                                            CPU.Block.Exception.Error);
                                    continue main_loop;// org_continue;
                                }
                                break;
                            default: {
                                illegalOpCode(); // goto illegal_opcode
                                continue main_loop;
                            }
                        }
                        break;
                    }
                    case CASE_W_0x8f: /* POP Ew */
                    {
                        int val = CPU.pop16();
                        int rm = fetchB();
                        if (rm >= 0xc0) {
                            int earwRegId = lookupRMEAregw[rm];
                            Register.Regs[earwRegId].setWord(val);
                        } else {
                            Memory.physWriteW(Core.EATable[rm].get(), val);
                        }
                        break;
                    }
                    case CASE_W_0x90:
                    case CASE_D_0x90: /* NOP */
                        break;
                    case CASE_W_0x91: /* XCHG CX,AX */
                    {
                        int temp = Register.getRegAX();
                        Register.setRegAX(Register.getRegCX());
                        Register.setRegCX(temp);
                    }
                        break;
                    case CASE_W_0x92: /* XCHG DX,AX */
                    {
                        int temp = Register.getRegAX();
                        Register.setRegAX(Register.getRegDX());
                        Register.setRegDX(temp);
                    }
                        break;
                    case CASE_W_0x93: /* XCHG BX,AX */
                    {
                        int temp = Register.getRegAX();
                        Register.setRegAX(Register.getRegBX());
                        Register.setRegBX(temp);
                    }
                        break;
                    case CASE_W_0x94: /* XCHG SP,AX */
                    {
                        int temp = Register.getRegAX();
                        Register.setRegAX(Register.getRegSP());
                        Register.setRegSP(temp);
                    }
                        break;
                    case CASE_W_0x95: /* XCHG BP,AX */
                    {
                        int temp = Register.getRegAX();
                        Register.setRegAX(Register.getRegBP());
                        Register.setRegBP(temp);
                    }
                        break;
                    case CASE_W_0x96: /* XCHG SI,AX */
                    {
                        int temp = Register.getRegAX();
                        Register.setRegAX(Register.getRegSI());
                        Register.setRegSI(temp);
                    }
                        break;
                    case CASE_W_0x97: /* XCHG DI,AX */
                    {
                        int temp = Register.getRegAX();
                        Register.setRegAX(Register.getRegDI());
                        Register.setRegDI(temp);
                    }
                        break;
                    case CASE_W_0x98: /* CBW */
                        // sbyte
                        Register.setRegAX((byte)Register.getRegAL());
                        break;
                    case CASE_W_0x99: /* CWD */
                        if ((Register.getRegAX() & 0x8000) != 0)
                            Register.setRegDX(0xffff);
                        else
                            Register.setRegDX(0);
                        break;
                    case CASE_W_0x9a: /* CALL Ap */
                    {
                        Flags.fillFlags();
                        int newip = fetchW();
                        int newcs = fetchW();
                        CPU.call(false, newcs, newip, getIP());
                        // -- #region CPU_TRAP_CHECK
                        if (Register.getFlag(Register.FlagTF) != 0) {
                            CPU.CpuDecoder = CpuTrapDecoder;
                            return Callback.ReturnTypeNone;
                        }
                        // -- #endregion
                        continue main_loop;// org_continue;
                    }
                    case CASE_W_0x9b:
                    case CASE_D_0x9b: /* WAIT */
                        break; /* No waiting here */
                    case CASE_W_0x9c: /* PUSHF */
                        if (CPU.pushf(false)) {
                            CPU.exception(CPU.Block.Exception.Which, CPU.Block.Exception.Error);
                            continue main_loop;// org_continue;
                        }
                        break;
                    case CASE_W_0x9d: /* POPF */
                        if (CPU.popf(false)) {
                            CPU.exception(CPU.Block.Exception.Which, CPU.Block.Exception.Error);
                            continue main_loop;// org_continue;
                        }
                        // -- #region CPU_TRAP_CHECK
                        if (Register.getFlag(Register.FlagTF) != 0) {
                            CPU.CpuDecoder = CpuTrapDecoder;
                            return decodeEnd();// goto decode_end;
                        }
                        // -- #endregion
                        // -- #region CPU_PIC_CHECK
                        if (Register.getFlag(Register.FlagIF) != 0 && PIC.IRQCheck != 0)
                            return decodeEnd();// goto decode_end;
                        // -- #endregion
                        break;
                    case CASE_W_0x9e:
                    case CASE_D_0x9e: /* SAHF */
                        Flags.setFLagSb(Register.getRegAH());
                        break;
                    case CASE_W_0x9f:
                    case CASE_D_0x9f: /* LAHF */
                        Flags.fillFlags();
                        Register.setRegAH(Register.Flags & 0xff);
                        break;
                    case CASE_W_0xa0:
                    case CASE_D_0xa0: /* MOV AL,Ob */
                    {
                        // GetEADirect;
                        int eaa;
                        if ((Core.Prefixes & PrefixAddr) != 0) {
                            eaa = Core.BaseDS + fetchD();
                        } else {
                            eaa = Core.BaseDS + fetchW();
                        }
                        Register.setRegAL(Memory.readB(eaa));
                    }
                        break;
                    case CASE_W_0xa1: /* MOV AX,Ow */
                    {
                        // GetEADirect;
                        int eaa;
                        if ((Core.Prefixes & PrefixAddr) != 0) {
                            eaa = Core.BaseDS + fetchD();
                        } else {
                            eaa = Core.BaseDS + fetchW();
                        }
                        Register.setRegAX(Memory.readW(eaa));
                    }
                        break;
                    case CASE_W_0xa2:
                    case CASE_D_0xa2: /* MOV Ob,AL */
                    {
                        // GetEADirect;
                        int eaa;
                        if ((Core.Prefixes & PrefixAddr) != 0) {
                            eaa = Core.BaseDS + fetchD();
                        } else {
                            eaa = Core.BaseDS + fetchW();
                        }
                        Memory.writeB(eaa, Register.getRegAL());
                    }
                        break;
                    case CASE_W_0xa3: /* MOV Ow,AX */
                    {
                        // GetEADirect;
                        int eaa;
                        if ((Core.Prefixes & PrefixAddr) != 0) {
                            eaa = Core.BaseDS + fetchD();
                        } else {
                            eaa = Core.BaseDS + fetchW();
                        }
                        Memory.writeW(eaa, Register.getRegAX());
                    }
                        break;
                    case CASE_W_0xa4:
                    case CASE_D_0xa4: /* MOVSB */
                        DoString(STRING_OP_MOVSB);
                        break;
                    case CASE_W_0xa5: /* MOVSW */
                        DoString(STRING_OP_MOVSW);
                        break;
                    case CASE_W_0xa6:
                    case CASE_D_0xa6: /* CMPSB */
                        DoString(STRING_OP_CMPSB);
                        break;
                    case CASE_W_0xa7: /* CMPSW */
                        DoString(STRING_OP_CMPSW);
                        break;
                    case CASE_W_0xa8:
                    case CASE_D_0xa8: /* TEST AL,Ib */
                        // ALIb(TESTB);
                        TESTBL(Register.AX, fetchB());
                        break;
                    case CASE_W_0xa9: /* TEST AX,Iw */
                        // AXIw(TESTW);
                        TESTW(Register.AX, fetchW());
                        break;
                    case CASE_W_0xaa:
                    case CASE_D_0xaa: /* STOSB */
                        DoString(STRING_OP_STOSB);
                        break;
                    case CASE_W_0xab: /* STOSW */
                        DoString(STRING_OP_STOSW);
                        break;
                    case CASE_W_0xac:
                    case CASE_D_0xac: /* LODSB */
                        DoString(STRING_OP_LODSB);
                        break;
                    case CASE_W_0xad: /* LODSW */
                        DoString(STRING_OP_LODSW);
                        break;
                    case CASE_W_0xae:
                    case CASE_D_0xae: /* SCASB */
                        DoString(STRING_OP_SCASB);
                        break;
                    case CASE_W_0xaf: /* SCASW */
                        DoString(STRING_OP_SCASW);
                        break;
                    case CASE_W_0xb0:
                    case CASE_D_0xb0: /* MOV AL,Ib */
                        Register.setRegAL(fetchB());
                        break;
                    case CASE_W_0xb1:
                    case CASE_D_0xb1: /* MOV CL,Ib */
                        Register.setRegCL(fetchB());
                        break;
                    case CASE_W_0xb2:
                    case CASE_D_0xb2: /* MOV DL,Ib */
                        Register.setRegDL(fetchB());
                        break;
                    case CASE_W_0xb3:
                    case CASE_D_0xb3: /* MOV BL,Ib */
                        Register.setRegBL(fetchB());
                        break;
                    case CASE_W_0xb4:
                    case CASE_D_0xb4: /* MOV AH,Ib */
                        Register.setRegAH(fetchB());
                        break;
                    case CASE_W_0xb5:
                    case CASE_D_0xb5: /* MOV CH,Ib */
                        Register.setRegCH(fetchB());
                        break;
                    case CASE_W_0xb6:
                    case CASE_D_0xb6: /* MOV DH,Ib */
                        Register.setRegDH(fetchB());
                        break;
                    case CASE_W_0xb7:
                    case CASE_D_0xb7: /* MOV BH,Ib */
                        Register.setRegBH(fetchB());
                        break;
                    case CASE_W_0xb8: /* MOV AX,Iw */
                        Register.setRegAX(fetchW());
                        break;
                    case CASE_W_0xb9: /* MOV CX,Iw */
                        Register.setRegCX(fetchW());
                        break;
                    case CASE_W_0xba: /* MOV DX,Iw */
                        Register.setRegDX(fetchW());
                        break;
                    case CASE_W_0xbb: /* MOV BX,Iw */
                        Register.setRegBX(fetchW());
                        break;
                    case CASE_W_0xbc: /* MOV SP,Iw */
                        Register.setRegSP(fetchW());
                        break;
                    case CASE_W_0xbd: /* MOV BP.Iw */
                        Register.setRegBP(fetchW());
                        break;
                    case CASE_W_0xbe: /* MOV SI,Iw */
                        Register.setRegSI(fetchW());
                        break;
                    case CASE_W_0xbf: /* MOV DI,Iw */
                        Register.setRegDI(fetchW());
                        break;
                    case CASE_W_0xc0:
                    case CASE_D_0xc0: /* GRP2 Eb,Ib */
                        GRP2B(this::fetchB);
                        break;
                    case CASE_W_0xc1: /* GRP2 Ew,Ib */
                        GRP2W(this::fetchB);
                        break;
                    case CASE_W_0xc2: /* RETN Iw */
                        Register.setRegEIP(CPU.pop16());
                        Register.setRegESP(Register.getRegESP() + fetchW());
                        continue main_loop;// org_continue;
                    case CASE_W_0xc3: /* RETN */
                        Register.setRegEIP(CPU.pop16());
                        continue main_loop;// org_continue;
                    case CASE_W_0xc4: /* LES */
                    {
                        // GetRMrw;
                        int rm = fetchB();
                        int rmrwRegId = lookupRMregw[rm];
                        if (rm >= 0xc0) {
                            illegalOpCode(); // goto illegal_opcode
                            continue main_loop;
                        }
                        // int eaa = core.ea_table[rm]();
                        int eaa = Core.EATable[rm].get();
                        if (CPU.setSegGeneral(Register.SEG_NAME_ES, Memory.readW(eaa + 2))) {
                            CPU.exception(CPU.Block.Exception.Which, CPU.Block.Exception.Error);
                            continue main_loop;// org_continue;
                        }
                        Register.Regs[rmrwRegId].setWord(Memory.readW(eaa));

                        break;
                    }
                    case CASE_W_0xc5: /* LDS */
                    {
                        // GetRMrw;
                        int rm = fetchB();
                        int rmrwRegId = lookupRMregw[rm];
                        if (rm >= 0xc0) {
                            illegalOpCode(); // goto illegal_opcode
                            continue main_loop;
                        }
                        // int eaa = core.ea_table[rm]();
                        int eaa = Core.EATable[rm].get();
                        if (CPU.setSegGeneral(Register.SEG_NAME_DS, Memory.readW(eaa + 2))) {
                            CPU.exception(CPU.Block.Exception.Which, CPU.Block.Exception.Error);
                            continue main_loop;// org_continue;
                        }
                        Register.Regs[rmrwRegId].setWord(Memory.readW(eaa));
                        break;
                    }
                    case CASE_W_0xc6:
                    case CASE_D_0xc6: /* MOV Eb,Ib */
                    {
                        int rm = fetchB();
                        if (rm >= 0xc0) {
                            int earbRegId = lookupRMEAregbl[rm];
                            if (earbRegId >= 0) {
                                Register.Regs[earbRegId].setByteL(fetchB());
                            } else {
                                Register.Regs[lookupRMEAregbh[rm]].setByteH(fetchB());
                            }
                        } else {
                            Memory.writeB(Core.EATable[rm].get(), fetchB());
                        }
                        break;
                    }
                    case CASE_W_0xc7: /* MOV EW,Iw */
                    {
                        int rm = fetchB();
                        if (rm >= 0xc0) {
                            Register.Regs[lookupRMEAregw[rm]].setWord(fetchW());
                        } else {
                            Memory.writeW(Core.EATable[rm].get(), fetchW());
                        }
                        break;
                    }
                    case CASE_W_0xc8: /* ENTER Iw,Ib */
                    {
                        int bytes = fetchW();
                        int level = fetchB();
                        CPU.enter(false, bytes, level);
                    }
                        break;
                    case CASE_W_0xc9: /* LEAVE */
                        Register.setRegESP(Register.getRegESP() & CPU.Block.Stack.NotMask);
                        Register.setRegESP(Register.getRegESP()
                                | (Register.getRegEBP() & CPU.Block.Stack.Mask));
                        Register.setRegBP(CPU.pop16());
                        break;
                    case CASE_W_0xca: /* RETF Iw */
                    {
                        int words = fetchW();
                        Flags.fillFlags();
                        CPU.ret(false, words, getIP());
                        continue main_loop;// org_continue;
                    }
                    case CASE_W_0xcb: /* RETF */
                        Flags.fillFlags();
                        CPU.ret(false, 0, getIP());
                        continue main_loop;// org_continue;
                    case CASE_W_0xcc:
                    case CASE_D_0xcc: /* INT3 */

                        CPU.swInterruptNoIOPLCheck(3, getIP());
                        // -- #region CPU_TRAP_CHECK
                        CPU.Block.TrapSkip = true;
                        // -- #endregion
                        continue main_loop;// org_continue;
                    case CASE_W_0xcd:
                    case CASE_D_0xcd: /* INT Ib */
                    {
                        int num = fetchB();
                        CPU.swInterrupt(num, getIP());
                        // -- #region CPU_TRAP_CHECK
                        CPU.Block.TrapSkip = true;
                        // -- #endregion
                        continue main_loop;// org_continue;
                    }
                    case CASE_W_0xce:
                    case CASE_D_0xce: /* INTO */
                        if (Flags.getOF()) {
                            CPU.swInterrupt(4, getIP());
                            // -- #region CPU_TRAP_CHECK
                            CPU.Block.TrapSkip = true;
                            // -- #endregion
                            continue main_loop;// org_continue;
                        }
                        break;
                    case CASE_W_0xcf: /* IRET */
                    {
                        CPU.iret(false, getIP());
                        // -- #region CPU_TRAP_CHECK
                        if (Register.getFlag(Register.FlagTF) != 0) {
                            CPU.CpuDecoder = CpuTrapDecoder;
                            return Callback.ReturnTypeNone;
                        }
                        // -- #endregion
                        // -- #region CPU_PIC_CHECK
                        if (Register.getFlag(Register.FlagIF) != 0 && PIC.IRQCheck != 0)
                            return Callback.ReturnTypeNone;
                        // -- #endregion
                        continue main_loop;// org_continue;
                    }
                    case CASE_W_0xd0:
                    case CASE_D_0xd0: /* GRP2 Eb,1 */
                        GRP2B(1);
                        break;
                    case CASE_W_0xd1: /* GRP2 Ew,1 */
                        GRP2W(1);
                        break;
                    case CASE_W_0xd2:
                    case CASE_D_0xd2: /* GRP2 Eb,CL */
                        GRP2B(Register.getRegCL());
                        break;
                    case CASE_W_0xd3: /* GRP2 Ew,CL */
                        GRP2W(Register.getRegCL());
                        break;
                    case CASE_W_0xd4:
                    case CASE_D_0xd4: /* AAM Ib */
                        AAM(fetchB());
                        break;
                    case CASE_W_0xd5:
                    case CASE_D_0xd5: /* AAD Ib */
                        AAD(fetchB());
                        break;
                    case CASE_W_0xd6:
                    case CASE_D_0xd6: /* SALC */
                        Register.setRegAL(Flags.getCF() ? 0xFF : 0);
                        break;
                    case CASE_W_0xd7:
                    case CASE_D_0xd7: /* XLAT */
                        if ((Core.Prefixes & PrefixAddr) != 0) {
                            Register.setRegAL(Memory.readB(
                                    Core.BaseDS + (Register.getRegEBX() + Register.getRegAL())));
                        } else {
                            Register.setRegAL(Memory.readB(Core.BaseDS + (0xffff
                                    & (Register.getRegBX() + Register.getRegAL()))));
                        }
                        break;
                    case CASE_W_0xd8:
                    case CASE_D_0xd8: /* FPU ESC 0 */
                    case CASE_W_0xd9:
                    case CASE_D_0xd9: /* FPU ESC 1 */
                    case CASE_W_0xda:
                    case CASE_D_0xda: /* FPU ESC 2 */
                    case CASE_W_0xdb:
                    case CASE_D_0xdb: /* FPU ESC 3 */
                    case CASE_W_0xdc:
                    case CASE_D_0xdc: /* FPU ESC 4 */
                    case CASE_W_0xdd:
                    case CASE_D_0xdd: /* FPU ESC 5 */
                    case CASE_W_0xde:
                    case CASE_D_0xde: /* FPU ESC 6 */
                    case CASE_W_0xdf:
                    case CASE_D_0xdf: /* FPU ESC 7 */
                    {
                        Log.logging(Log.LogTypes.CPU, Log.LogServerities.Normal, "FPU used");
                        int rm = fetchB();
                        if (rm < 0xc0) {
                            int eaa = Core.EATable[rm].get();
                        }
                    }
                        break;
                    case CASE_W_0xe0: /* LOOPNZ */
                        if ((Core.Prefixes & PrefixAddr) != 0) {
                            // JumpCond16_b(--regsModule.reg_ecx && !get_ZF());
                            {
                                saveIP();
                                // --Register.getRegECX()
                                Register.setRegECX(Register.getRegECX() - 1);
                                if (Register.getRegECX() != 0 && !Flags.getZF())
                                    Register.setRegIP(Register.getRegIP() + fetchBS());
                                Register.setRegIP(Register.getRegIP() + 1);
                                continue main_loop;// org_continue;
                            }
                        } else {
                            // JumpCond16_b(--regsModule.reg_cx && !get_ZF());
                            {
                                saveIP();
                                Register.setRegCX(Register.getRegCX() - 1);
                                if (Register.getRegCX() != 0 && !Flags.getZF())
                                    Register.setRegIP(Register.getRegIP() + fetchBS());
                                Register.setRegIP(Register.getRegIP() + 1);
                                continue main_loop;// org_continue;
                            }
                        }
                        // break;
                    case CASE_W_0xe1: /* LOOPZ */
                        if ((Core.Prefixes & PrefixAddr) != 0) {
                            // JumpCond16_b(--regsModule.reg_ecx && get_ZF());
                            {
                                saveIP();
                                // --Register.getRegECX()
                                Register.setRegECX(Register.getRegECX() - 1);
                                if (Register.getRegECX() != 0 && Flags.getZF())
                                    Register.setRegIP(Register.getRegIP() + fetchBS());
                                Register.setRegIP(Register.getRegIP() + 1);
                                continue main_loop;// org_continue;
                            }
                        } else {
                            // JumpCond16_b(--regsModule.reg_cx && get_ZF());
                            {
                                saveIP();
                                Register.setRegCX(Register.getRegCX() - 1);
                                if (Register.getRegCX() != 0 && Flags.getZF())
                                    Register.setRegIP(Register.getRegIP() + fetchBS());
                                Register.setRegIP(Register.getRegIP() + 1);
                                continue main_loop;// org_continue;
                            }
                        }
                        // break;
                    case CASE_W_0xe2: /* LOOP */
                        if ((Core.Prefixes & PrefixAddr) != 0) {
                            // JumpCond16_b(--regsModule.reg_ecx);
                            {
                                saveIP();
                                Register.setRegECX(Register.getRegECX() - 1);
                                if (Register.getRegECX() != 0)
                                    Register.setRegIP(Register.getRegIP() + fetchBS());
                                Register.setRegIP(Register.getRegIP() + 1);
                                continue main_loop;// org_continue;
                            }
                        } else {
                            // JumpCond16_b(--regsModule.reg_cx);
                            {
                                saveIP();
                                Register.setRegCX(Register.getRegCX() - 1);
                                if (Register.getRegCX() != 0)
                                    Register.setRegIP(Register.getRegIP() + fetchBS());
                                Register.setRegIP(Register.getRegIP() + 1);
                                continue main_loop;// org_continue;
                            }
                        }
                        // break;
                    case CASE_W_0xe3: /* JCXZ */
                    // JumpCond16_b(!(regsModule.reg_ecx & AddrMaskTable[core.prefixes&
                    // PREFIX_ADDR]));
                    {
                        saveIP();
                        if ((Register.getRegECX() & AddrMaskTable[Core.Prefixes & PrefixAddr]) == 0)
                            Register.setRegIP(Register.getRegIP() + fetchBS());
                        Register.setRegIP(Register.getRegIP() + 1);
                        continue main_loop;// org_continue;
                    }
                    // break;
                    case CASE_W_0xe4:
                    case CASE_D_0xe4: /* IN AL,Ib */
                    {
                        int port = fetchB();
                        if (CPU.ioException(port, 1)) {
                            CPU.exception(CPU.Block.Exception.Which, CPU.Block.Exception.Error);
                            continue main_loop;// org_continue;
                        }
                        Register.setRegAL(IO.readB(port));
                        break;
                    }
                    case CASE_W_0xe5: /* IN AX,Ib */
                    {
                        int port = fetchB();
                        if (CPU.ioException(port, 2)) {
                            CPU.exception(CPU.Block.Exception.Which, CPU.Block.Exception.Error);
                            continue main_loop;// org_continue;
                        }
                        Register.setRegAL(IO.readW(port));
                        break;
                    }
                    case CASE_W_0xe6:
                    case CASE_D_0xe6: /* OUT Ib,AL */
                    {
                        int port = fetchB();
                        if (CPU.ioException(port, 1)) {
                            CPU.exception(CPU.Block.Exception.Which, CPU.Block.Exception.Error);
                            continue main_loop;// org_continue;
                        }
                        IO.writeB(port, Register.getRegAL());
                        break;
                    }
                    case CASE_W_0xe7: /* OUT Ib,AX */
                    {
                        int port = fetchB();
                        if (CPU.ioException(port, 2)) {
                            CPU.exception(CPU.Block.Exception.Which, CPU.Block.Exception.Error);
                            continue main_loop;// org_continue;
                        }
                        IO.writeW(port, Register.getRegAX());
                        break;
                    }
                    case CASE_W_0xe8: /* CALL Jw */
                    {
                        int addip = 0xffff & fetchWS();// uint16
                        saveIP();
                        CPU.push16(Register.getRegEIP());
                        Register.setRegEIP(0xffff & (Register.getRegEIP() + addip));
                        continue main_loop;// org_continue;
                    }
                    case CASE_W_0xe9: /* JMP Jw */
                    {
                        int addip = 0xffff & fetchWS();// uint16
                        saveIP();
                        Register.setRegEIP(0xffff & (Register.getRegEIP() + addip));
                        continue main_loop;// org_continue;
                    }
                    case CASE_W_0xea: /* JMP Ap */
                    {
                        int newip = fetchW();
                        int newcs = fetchW();
                        Flags.fillFlags();
                        CPU.jmp(false, newcs, newip, getIP());
                        // -- #region CPU_TRAP_CHECK
                        if (Register.getFlag(Register.FlagTF) != 0) {
                            CPU.CpuDecoder = CpuTrapDecoder;
                            return Callback.ReturnTypeNone;
                        }
                        // -- #endregion
                        continue main_loop;// org_continue;
                    }
                    case CASE_W_0xeb: /* JMP Jb */
                    {
                        short addip = fetchBS();
                        saveIP();
                        Register.setRegEIP(0xffff & (Register.getRegEIP() + addip));
                        continue main_loop;// org_continue;
                    }
                    case CASE_W_0xec:
                    case CASE_D_0xec: /* IN AL,DX */
                        if (CPU.ioException(Register.getRegDX(), 1)) {
                            CPU.exception(CPU.Block.Exception.Which, CPU.Block.Exception.Error);
                            continue main_loop;// org_continue;
                        }
                        Register.setRegAL(IO.readB(Register.getRegDX()));
                        break;
                    case CASE_W_0xed: /* IN AX,DX */
                        if (CPU.ioException(Register.getRegDX(), 2)) {
                            CPU.exception(CPU.Block.Exception.Which, CPU.Block.Exception.Error);
                            continue main_loop;// org_continue;
                        }
                        Register.setRegAX(IO.readW(Register.getRegDX()));
                        break;
                    case CASE_W_0xee:
                    case CASE_D_0xee: /* OUT DX,AL */
                        if (CPU.ioException(Register.getRegDX(), 1)) {
                            CPU.exception(CPU.Block.Exception.Which, CPU.Block.Exception.Error);
                            continue main_loop;// org_continue;
                        }
                        IO.writeB(Register.getRegDX(), Register.getRegAL());
                        break;
                    case CASE_W_0xef: /* OUT DX,AX */
                        if (CPU.ioException(Register.getRegDX(), 2)) {
                            CPU.exception(CPU.Block.Exception.Which, CPU.Block.Exception.Error);
                            continue main_loop;// org_continue;
                        }
                        IO.writeW(Register.getRegDX(), Register.getRegAX());
                        break;
                    case CASE_W_0xf0:
                    case CASE_D_0xf0: /* LOCK */
                        Log.logging(Log.LogTypes.CPU, Log.LogServerities.Normal,
                                "CPU:LOCK"); /* FIXME: see case D_LOCK in core_full/load.h */
                        break;
                    case CASE_W_0xf1:
                    case CASE_D_0xf1: /* ICEBP */
                        CPU.swInterruptNoIOPLCheck(1, getIP());
                        // -- #region CPU_TRAP_CHECK
                        CPU.Block.TrapSkip = true;
                        // -- #endregion
                        continue main_loop;// org_continue;
                    case CASE_W_0xf2:
                    case CASE_D_0xf2: /* REPNZ */
                        // DO_PREFIX_REP(false);
                        Core.Prefixes |= PrefixRep;
                        Core.RepZero = false;
                        continue restart_opcode; // goto restart_opcode;
                    // break;
                    case CASE_W_0xf3:
                    case CASE_D_0xf3: /* REPZ */
                        // DO_PREFIX_REP(true);
                        Core.Prefixes |= PrefixRep;
                        Core.RepZero = true;
                        continue restart_opcode; // goto restart_opcode;
                    // break;
                    case CASE_W_0xf4:
                    case CASE_D_0xf4: /* HLT */
                        if (CPU.Block.PMode && CPU.Block.CPL != 0) {
                            CPU.exception(CPU.ExceptionGP);
                            continue main_loop;// org_continue;
                        }
                        Flags.fillFlags();
                        CPU.hlt(getIP());
                        return Callback.ReturnTypeNone; // Needs to return for hlt cpu core
                    case CASE_W_0xf5:
                    case CASE_D_0xf5: /* CMC */
                        Flags.fillFlags();
                        Register.setFlagBit(Register.FlagCF,
                                (Register.Flags & Register.FlagCF) == 0);
                        break;
                    case CASE_W_0xf6:
                    case CASE_D_0xf6: /* GRP3 Eb(,Ib) */
                    {
                        int rm = fetchB();
                        int which = (rm >>> 3) & 7;
                        switch (which) {
                            case 0x00: /* TEST Eb,Ib */
                            case 0x01: /* TEST Eb,Ib Undocumented */
                            {
                                if (rm >= 0xc0) {
                                    int earbRegId = lookupRMEAregbl[rm];
                                    if (earbRegId >= 0)
                                        TESTBL(earbRegId, fetchB());
                                    else
                                        TESTBH(lookupRMEAregbh[rm], fetchB());
                                } else {
                                    TESTB(Core.EATable[rm].get(), fetchB());
                                }
                                break;
                            }
                            case 0x02: /* NOT Eb */
                            {
                                if (rm >= 0xc0) {
                                    int earbRegId = lookupRMEAregbl[rm];
                                    if (earbRegId >= 0)
                                        Register.Regs[earbRegId]
                                                .setByteL(~Register.Regs[earbRegId].getByteL());
                                    else {
                                        earbRegId = lookupRMEAregbh[rm];
                                        Register.Regs[earbRegId]
                                                .setByteH(~Register.Regs[earbRegId].getByteH());
                                    }
                                } else {
                                    int eaa = Core.EATable[rm].get();
                                    Memory.writeB(eaa, 0xff & ~Memory.readB(eaa));
                                }
                                break;
                            }
                            case 0x03: /* NEG Eb */
                            {
                                Flags.LzFlags.Type = Flags.TypeFlag.NEGb;
                                if (rm >= 0xc0) {
                                    int earbRegId = lookupRMEAregbl[rm];
                                    if (earbRegId >= 0) {
                                        Flags.setLzFVar1b(Register.Regs[earbRegId].getByteL());
                                        Flags.setLzFResb(0 - Flags.getLzFVar1b());
                                        Register.Regs[earbRegId].setByteL(Flags.getLzFResb());
                                    } else {
                                        earbRegId = lookupRMEAregbh[rm];
                                        Flags.setLzFVar1b(Register.Regs[earbRegId].getByteH());
                                        Flags.setLzFResb(0 - Flags.getLzFVar1b());
                                        Register.Regs[earbRegId].setByteH(Flags.getLzFResb());
                                    }
                                } else {
                                    int eaa = Core.EATable[rm].get();
                                    Flags.setLzFVar1b(Memory.readB(eaa));
                                    Flags.setLzFResb(0 - Flags.getLzFVar1b());
                                    Memory.writeB(eaa, Flags.getLzFResb());
                                }
                                break;
                            }
                            case 0x04: /* MUL AL,Eb */
                                // RMEb(MULB);
                                if (rm >= 0xc0) {
                                    int earbRegId = lookupRMEAregbl[rm];
                                    if (earbRegId >= 0)
                                        MULBL(earbRegId);
                                    else
                                        MULBH(lookupRMEAregbh[rm]);
                                } else {
                                    MULB(Core.EATable[rm].get());
                                }
                                break;
                            case 0x05: /* IMUL AL,Eb */
                                // RMEb(IMULB);
                                if (rm >= 0xc0) {
                                    int earbRegId = lookupRMEAregbl[rm];
                                    if (earbRegId >= 0)
                                        IMULBL(earbRegId);
                                    else
                                        IMULBH(lookupRMEAregbh[rm]);
                                } else {
                                    IMULB(Core.EATable[rm].get());
                                }
                                break;
                            case 0x06: /* DIV Eb */
                                // RMEb(DIVB);
                                if (rm >= 0xc0) {
                                    int earbRegId = lookupRMEAregbl[rm];
                                    if (earbRegId >= 0) {
                                        if (DIVBL(earbRegId) == SwitchReturn.Continue)
                                            continue main_loop;// org_continue;
                                    } else {
                                        if (DIVBH(lookupRMEAregbh[rm]) == SwitchReturn.Continue)
                                            continue main_loop;// org_continue;
                                    }
                                } else {
                                    if (DIVB(Core.EATable[rm].get()) == SwitchReturn.Continue)
                                        continue main_loop;// org_continue;
                                }
                                break;
                            case 0x07: /* IDIV Eb */
                                // RMEb(IDIVB);
                                if (rm >= 0xc0) {
                                    int earbRegId = lookupRMEAregbl[rm];
                                    if (earbRegId >= 0) {
                                        if (IDIVBL(earbRegId) == SwitchReturn.Continue)
                                            continue main_loop;// org_continue;
                                    } else {
                                        if (IDIVBH(lookupRMEAregbh[rm]) == SwitchReturn.Continue)
                                            continue main_loop;// org_continue;
                                    }
                                } else {
                                    if (IDIVB(Core.EATable[rm].get()) == SwitchReturn.Continue)
                                        continue main_loop;// org_continue;
                                }
                                break;
                        }
                        break;
                    }
                    case CASE_W_0xf7: /* GRP3 Ew(,Iw) */
                    {
                        int rm = fetchB();
                        int which = (rm >>> 3) & 7;
                        switch (which) {
                            case 0x00: /* TEST Ew,Iw */
                            case 0x01: /* TEST Ew,Iw Undocumented */
                            {
                                if (rm >= 0xc0) {
                                    TESTW(lookupRMEAregw[rm], fetchW());
                                } else {
                                    TESTW_M(Core.EATable[rm].get(), fetchW());
                                }
                                break;
                            }
                            case 0x02: /* NOT Ew */
                            {
                                if (rm >= 0xc0) {
                                    int earwRegId = lookupRMEAregw[rm];
                                    Register.Regs[earwRegId]
                                            .setWord(0xffff & ~Register.Regs[earwRegId].getWord());
                                } else {
                                    int eaa = Core.EATable[rm].get();
                                    Memory.writeW(eaa, 0xffff & ~Memory.readW(eaa));
                                }
                                break;
                            }
                            case 0x03: /* NEG Ew */
                            {
                                Flags.LzFlags.Type = Flags.TypeFlag.NEGw;
                                if (rm >= 0xc0) {
                                    int earwRegId = lookupRMEAregw[rm];
                                    Flags.setLzFVar1w(Register.Regs[earwRegId].getWord());
                                    Flags.setLzFresw(0 - Flags.getLzFVar1w());
                                    Register.Regs[earwRegId].setWord(Flags.getLzFresw());
                                } else {
                                    int eaa = Core.EATable[rm].get();
                                    Flags.setLzFVar1w(Memory.readW(eaa));
                                    Flags.setLzFresw(0 - Flags.getLzFVar1w());
                                    Memory.writeW(eaa, Flags.getLzFresw());
                                }
                                break;
                            }
                            case 0x04: /* MUL AX,Ew */
                            // RMEw(MULW);
                            {
                                if (rm >= 0xc0) {
                                    MULW(lookupRMEAregw[rm]);
                                } else {
                                    MULW_M(Core.EATable[rm].get());
                                }
                            }
                                break;
                            case 0x05: /* IMUL AX,Ew */
                            // RMEw(IMULW)
                            {
                                if (rm >= 0xc0) {
                                    IMULW(lookupRMEAregw[rm]);
                                } else {
                                    IMULW_M(Core.EATable[rm].get());
                                }
                            }
                                break;
                            case 0x06: /* DIV Ew */
                            // RMEw(DIVW)
                            {
                                if (rm >= 0xc0) {
                                    DIVW(lookupRMEAregw[rm]);
                                } else {
                                    DIVW_M(Core.EATable[rm].get());
                                }
                            }
                                break;
                            case 0x07: /* IDIV Ew */
                            // RMEw(IDIVW)
                            {
                                if (rm >= 0xc0) {
                                    IDIVW(lookupRMEAregw[rm]);
                                } else {
                                    IDIVW_M(Core.EATable[rm].get());
                                }
                            }
                                break;
                        }
                        break;
                    }
                    case CASE_W_0xf8:
                    case CASE_D_0xf8: /* CLC */
                        Flags.fillFlags();
                        Register.setFlagBit(Register.FlagCF, false);
                        break;
                    case CASE_W_0xf9:
                    case CASE_D_0xf9: /* STC */
                        Flags.fillFlags();
                        Register.setFlagBit(Register.FlagCF, true);
                        break;
                    case CASE_W_0xfa:
                    case CASE_D_0xfa: /* CLI */
                        if (CPU.cli()) {
                            CPU.exception(CPU.Block.Exception.Which, CPU.Block.Exception.Error);
                            continue main_loop;// org_continue;
                        }
                        break;
                    case CASE_W_0xfb:
                    case CASE_D_0xfb: /* STI */
                        if (CPU.sti()) {
                            CPU.exception(CPU.Block.Exception.Which, CPU.Block.Exception.Error);
                            continue main_loop;// org_continue;
                        }
                        // -- #region CPU_PIC_CHECK
                        if (Register.getFlag(Register.FlagIF) != 0 && PIC.IRQCheck != 0)
                            return decodeEnd();// goto decode_end;
                        // -- #endregion
                        break;
                    case CASE_W_0xfc:
                    case CASE_D_0xfc: /* CLD */
                        Register.setFlagBit(Register.FlagDF, false);
                        CPU.Block.Direction = 1;
                        break;
                    case CASE_W_0xfd:
                    case CASE_D_0xfd: /* STD */
                        Register.setFlagBit(Register.FlagDF, true);
                        CPU.Block.Direction = -1;
                        break;
                    case CASE_W_0xfe:
                    case CASE_D_0xfe: /* GRP4 Eb */
                    {
                        int rm = fetchB();
                        int which = (rm >>> 3) & 7;
                        switch (which) {
                            case 0x00: /* INC Eb */
                            // RMEb(INCB);
                            {
                                if (rm >= 0xc0) {
                                    int earbRegId = lookupRMEAregbl[rm];
                                    if (earbRegId >= 0)
                                        INCBL(earbRegId);
                                    else
                                        INCBH(lookupRMEAregbh[rm]);
                                } else {
                                    INCB(Core.EATable[rm].get());
                                }
                            }
                                break;
                            case 0x01: /* DEC Eb */
                            // RMEb(DECB);
                            {
                                if (rm >= 0xc0) {
                                    int earbRegId = lookupRMEAregbl[rm];
                                    if (earbRegId >= 0)
                                        DECBL(earbRegId);
                                    else
                                        DECBH(lookupRMEAregbh[rm]);
                                } else {
                                    DECB(Core.EATable[rm].get());
                                }
                            }
                                break;
                            case 0x07: /* CallBack */
                            {
                                int cb = fetchW();
                                Flags.fillFlags();
                                saveIP();
                                return cb;
                            }
                            default:
                                Support.exceptionExit("Illegal GRP4 Call %d", (rm >>> 3) & 7);
                                break;
                        }
                        break;
                    }
                    case CASE_W_0xff: /* GRP5 Ew */
                    {
                        int rm = fetchB();
                        int which = (rm >>> 3) & 7;
                        switch (which) {
                            case 0x00: /* INC Ew */
                            // RMEw(INCW);
                            {
                                if (rm >= 0xc0) {
                                    INCW(lookupRMEAregw[rm]);
                                } else {
                                    INCW_M(Core.EATable[rm].get());
                                }
                            }
                                break;
                            case 0x01: /* DEC Ew */
                            // RMEw(DECW);
                            {
                                if (rm >= 0xc0) {
                                    DECW(lookupRMEAregw[rm]);
                                } else {
                                    DECW_M(Core.EATable[rm].get());
                                }
                            }
                                break;
                            case 0x02: /* CALL Ev */
                                if (rm >= 0xc0) {
                                    Register.setRegEIP(Register.Regs[lookupRMEAregw[rm]].getWord());
                                } else {
                                    Register.setRegEIP(Memory.readW(Core.EATable[rm].get()));
                                }
                                CPU.push16(getIP());
                                continue main_loop;// org_continue;
                            case 0x03: /* CALL Ep */
                            {
                                if (rm >= 0xc0) {
                                    illegalOpCode(); // goto illegal_opcode
                                    continue main_loop;
                                }
                                int eaa = Core.EATable[rm].get();
                                int newip = Memory.readW(eaa);
                                int newcs = Memory.readW(eaa + 2);
                                Flags.fillFlags();
                                CPU.call(false, newcs, newip, getIP());
                                // -- #region CPU_TRAP_CHECK
                                if (Register.getFlag(Register.FlagTF) != 0) {
                                    CPU.CpuDecoder = CpuTrapDecoder;
                                    return Callback.ReturnTypeNone;
                                }
                                // -- #endregion
                                continue main_loop;// org_continue;
                            }
                            // break;
                            case 0x04: /* JMP Ev */
                                if (rm >= 0xc0) {
                                    Register.setRegEIP(Register.Regs[lookupRMEAregw[rm]].getWord());
                                } else {
                                    Register.setRegEIP(Memory.readW(Core.EATable[rm].get()));
                                }
                                continue main_loop;// org_continue;
                            case 0x05: /* JMP Ep */
                            {
                                if (rm >= 0xc0) {
                                    illegalOpCode(); // goto illegal_opcode
                                    continue main_loop;
                                }
                                int eaa = Core.EATable[rm].get();
                                int newip = Memory.readW(eaa);
                                int newcs = Memory.readW(eaa + 2);
                                Flags.fillFlags();
                                CPU.jmp(false, newcs, newip, getIP());
                                // -- #region CPU_TRAP_CHECK
                                if (Register.getFlag(Register.FlagTF) != 0) {
                                    CPU.CpuDecoder = CpuTrapDecoder;
                                    return Callback.ReturnTypeNone;
                                }
                                // -- #endregion
                                continue main_loop;// org_continue;
                            }
                            // break;
                            case 0x06: /* PUSH Ev */
                                if (rm >= 0xc0) {
                                    CPU.push16(Register.Regs[lookupRMEAregw[rm]].getWord());
                                } else {
                                    CPU.push16(Memory.readW(Core.EATable[rm].get()));
                                }
                                break;
                            default:
                                Log.logging(Log.LogTypes.CPU, Log.LogServerities.Error,
                                        "CPU:GRP5:Illegal Call %2X", which); {
                                illegalOpCode(); // goto illegal_opcode
                                continue main_loop;
                            }
                        }
                        break;
                    }

                    // -- #endregion
                    // #include "core_normal/prefix_0f.h"
                    // -- #region prefix_0f

                    case CASE_0F_W_0x00: /* GRP 6 Exxx */
                    {
                        if ((Register.Flags & Register.FlagVM) != 0 || (!CPU.Block.PMode)) {
                            illegalOpCode(); // goto illegal_opcode
                            continue main_loop;
                        }
                        int rm = fetchB();
                        int which = (rm >>> 3) & 7;
                        switch (which) {
                            case 0x00: /* SLDT */
                            case 0x01: /* STR */
                            {
                                int saveval;
                                if (which == 0)
                                    saveval = CPU.sldt();
                                else
                                    saveval = CPU.str();
                                if (rm >= 0xc0) {
                                    Register.Regs[lookupRMEAregw[rm]].setWord(saveval);
                                } else {
                                    Memory.writeW(Core.EATable[rm].get(), saveval);
                                }
                            }
                                break;
                            case 0x02:
                            case 0x03:
                            case 0x04:
                            case 0x05: {
                                int loadval;
                                if (rm >= 0xc0) {
                                    loadval = Register.Regs[lookupRMEAregw[rm]].getWord();
                                } else {
                                    loadval = Memory.readW(Core.EATable[rm].get());
                                }
                                switch (which) {
                                    case 0x02:
                                        if (CPU.Block.CPL != 0) {
                                            CPU.exception(CPU.ExceptionGP);
                                            continue main_loop;// org_continue;
                                        }
                                        if (CPU.lldt(loadval)) {
                                            CPU.exception(CPU.Block.Exception.Which,
                                                    CPU.Block.Exception.Error);
                                            continue main_loop;// org_continue;
                                        }
                                        break;
                                    case 0x03:
                                        if (CPU.Block.CPL != 0) {
                                            CPU.exception(CPU.ExceptionGP);
                                            continue main_loop;// org_continue;
                                        }
                                        if (CPU.ltr(loadval)) {
                                            CPU.exception(CPU.Block.Exception.Which,
                                                    CPU.Block.Exception.Error);
                                            continue main_loop;// org_continue;
                                        }
                                        break;
                                    case 0x04:
                                        CPU.verr(loadval);
                                        break;
                                    case 0x05:
                                        CPU.verw(loadval);
                                        break;
                                }
                            }
                                break;
                            default: {
                                illegalOpCode(); // goto illegal_opcode
                                continue main_loop;
                            }
                        }
                    }
                        break;
                    case CASE_0F_W_0x01: /* Group 7 Ew */
                    {
                        int rm = fetchB();
                        int which = (rm >>> 3) & 7;
                        if (rm < 0xc0) { // First ones all use EA
                            int eaa = Core.EATable[rm].get();
                            int limit;
                            switch (which) {
                                case 0x00: /* SGDT */
                                    Memory.writeW(eaa, CPU.sgdtLimit());
                                    Memory.writeD(eaa + 2, CPU.sgdtBase());
                                    break;
                                case 0x01: /* SIDT */
                                    Memory.writeW(eaa, CPU.sidtLimit());
                                    Memory.writeD(eaa + 2, CPU.sidtBase());
                                    break;
                                case 0x02: /* LGDT */
                                    if (CPU.Block.PMode && CPU.Block.CPL != 0) {
                                        CPU.exception(CPU.ExceptionGP);
                                        continue main_loop;// org_continue;
                                    }
                                    CPU.lgdt(Memory.readW(eaa), Memory.readD(eaa + 2) & 0xFFFFFF);
                                    break;
                                case 0x03: /* LIDT */
                                    if (CPU.Block.PMode && CPU.Block.CPL != 0) {
                                        CPU.exception(CPU.ExceptionGP);
                                        continue main_loop;// org_continue;
                                    }
                                    CPU.lidt(Memory.readW(eaa), Memory.readD(eaa + 2) & 0xFFFFFF);
                                    break;
                                case 0x04: /* SMSW */
                                    Memory.writeW(eaa, CPU.smsw());
                                    break;
                                case 0x06: /* LMSW */
                                    limit = Memory.readW(eaa);
                                    if (CPU.lmsw(limit)) {
                                        CPU.exception(CPU.Block.Exception.Which,
                                                CPU.Block.Exception.Error);
                                        continue main_loop;// org_continue;
                                    }
                                    break;
                                case 0x07: /* INVLPG */
                                    if (CPU.Block.PMode && CPU.Block.CPL != 0) {
                                        CPU.exception(CPU.ExceptionGP);
                                        continue main_loop;// org_continue;
                                    }
                                    Paging.clearTLB();
                                    break;
                            }
                        } else {
                            int earwId = lookupRMEAregw[rm];
                            switch (which) {
                                case 0x02: /* LGDT */
                                    if (CPU.Block.PMode && CPU.Block.CPL != 0) {
                                        CPU.exception(CPU.ExceptionGP);
                                        continue main_loop;// org_continue;
                                    } {
                                    illegalOpCode(); // goto illegal_opcode
                                    continue main_loop;
                                }
                                case 0x03: /* LIDT */
                                    if (CPU.Block.PMode && CPU.Block.CPL != 0) {
                                        CPU.exception(CPU.ExceptionGP);
                                        continue main_loop;// org_continue;
                                    } {
                                    illegalOpCode(); // goto illegal_opcode
                                    continue main_loop;
                                }
                                case 0x04: /* SMSW */
                                    Register.Regs[earwId].setWord(CPU.smsw());
                                    break;
                                case 0x06: /* LMSW */
                                    if (CPU.lmsw(Register.Regs[earwId].getWord())) {
                                        CPU.exception(CPU.Block.Exception.Which,
                                                CPU.Block.Exception.Error);
                                        continue main_loop;// org_continue;
                                    }
                                    break;
                                default: {
                                    illegalOpCode(); // goto illegal_opcode
                                    continue main_loop;
                                }
                            }
                        }
                    }
                        break;
                    case CASE_0F_W_0x02: /* LAR Gw,Ew */
                    {
                        if ((Register.Flags & Register.FlagVM) != 0 || (!CPU.Block.PMode)) {
                            illegalOpCode(); // goto illegal_opcode
                            continue main_loop;
                        }
                        int rm = fetchB();
                        int rmrwId = lookupRMregw[rm];
                        int ar = Register.Regs[rmrwId].getWord();
                        int ret = ar;
                        if (rm >= 0xc0) {
                            ret = CPU.lar(Register.Regs[lookupRMEAregw[rm]].getWord(), ar);
                        } else {
                            ret = CPU.lar(Memory.readW(Core.EATable[rm].get()), ar);
                        }
                        ar = ret < 0 ? ar : ret;
                        Register.Regs[rmrwId].setWord(ar);
                    }
                        break;
                    case CASE_0F_W_0x03: /* LSL Gw,Ew */
                    {
                        if ((Register.Flags & Register.FlagVM) != 0 || (!CPU.Block.PMode)) {
                            illegalOpCode(); // goto illegal_opcode
                            continue main_loop;
                        }
                        int rm = fetchB();
                        int rmrwId = lookupRMregw[rm];
                        int limit = Register.Regs[rmrwId].getWord();
                        int ret = limit;
                        if (rm >= 0xc0) {
                            ret = CPU.lsl(Register.Regs[lookupRMEAregw[rm]].getWord());
                        } else {
                            ret = CPU.lsl(Memory.readW(Core.EATable[rm].get()));
                        }
                        limit = ret < 0 ? limit : ret;
                        Register.Regs[rmrwId].setWord(limit);
                    }
                        break;
                    case CASE_0F_D_0x06:
                    case CASE_0F_W_0x06: /* CLTS */
                        if (CPU.Block.PMode && CPU.Block.CPL != 0) {
                            CPU.exception(CPU.ExceptionGP);
                            continue main_loop;// org_continue;
                        }
                        CPU.Block.CR0 &= (~CPU.CR0_TASKSWITCH);
                        break;
                    case CASE_0F_D_0x08:
                    case CASE_0F_W_0x08: /* INVD */
                    case CASE_0F_D_0x09:
                    case CASE_0F_W_0x09: /* WBINVD */
                        if (CPU.ArchitectureType < CPU.ArchType486OldSlow) {
                            illegalOpCode(); // goto illegal_opcode
                            continue main_loop;
                        }
                        if (CPU.Block.PMode && CPU.Block.CPL != 0) {
                            CPU.exception(CPU.ExceptionGP);
                            continue main_loop;// org_continue;
                        }
                        break;
                    case CASE_0F_D_0x20:
                    case CASE_0F_W_0x20: /* MOV Rd.CRx */
                    {
                        int rm = fetchB();
                        int which = (rm >>> 3) & 7;
                        if (rm < 0xc0) {
                            rm |= 0xc0;
                            Log.logging(Log.LogTypes.CPU, Log.LogServerities.Error,
                                    "MOV XXX,CR%u with non-register", which);
                        }
                        int eardId = lookupRMEAregd[rm];
                        int crx_value = 0;
                        if ((crx_value = (int) CPU.readCRX(which)) < 0) {
                            CPU.exception(CPU.Block.Exception.Which, CPU.Block.Exception.Error);
                            continue main_loop;// org_continue;
                        }
                        Register.Regs[eardId].setDWord(crx_value);
                    }
                        break;
                    case CASE_0F_D_0x21:
                    case CASE_0F_W_0x21: /* MOV Rd,DRx */
                    {
                        int rm = fetchB();
                        int which = (rm >>> 3) & 7;
                        if (rm < 0xc0) {
                            rm |= 0xc0;
                            Log.logging(Log.LogTypes.CPU, Log.LogServerities.Error,
                                    "MOV XXX,DR%u with non-register", which);
                        }
                        int eardId = lookupRMEAregd[rm];
                        int drx_value = 0;
                        if ((drx_value = (int) CPU.readDRX(which)) < 0) {
                            CPU.exception(CPU.Block.Exception.Which, CPU.Block.Exception.Error);
                            continue main_loop;// org_continue;
                        }
                        Register.Regs[eardId].setDWord(drx_value);
                    }
                        break;
                    case CASE_0F_D_0x22:
                    case CASE_0F_W_0x22: /* MOV CRx,Rd */
                    {
                        int rm = fetchB();
                        int which = (rm >>> 3) & 7;
                        if (rm < 0xc0) {
                            rm |= 0xc0;
                            Log.logging(Log.LogTypes.CPU, Log.LogServerities.Error,
                                    "MOV XXX,CR%u with non-register", which);
                        }
                        int eardId = lookupRMEAregd[rm];
                        if (CPU.writeCRX(which, Register.Regs[eardId].getDWord())) {
                            CPU.exception(CPU.Block.Exception.Which, CPU.Block.Exception.Error);
                            continue main_loop;// org_continue;
                        }
                    }
                        break;
                    case CASE_0F_D_0x23:
                    case CASE_0F_W_0x23: /* MOV DRx,Rd */
                    {
                        int rm = fetchB();
                        int which = (rm >>> 3) & 7;
                        if (rm < 0xc0) {
                            rm |= 0xc0;
                            Log.logging(Log.LogTypes.CPU, Log.LogServerities.Error,
                                    "MOV DR%u,XXX with non-register", which);
                        }
                        int eardId = lookupRMEAregd[rm];
                        if (CPU.writeDRX(which, Register.Regs[eardId].getDWord())) {
                            CPU.exception(CPU.Block.Exception.Which, CPU.Block.Exception.Error);
                            continue main_loop;// org_continue;
                        }
                    }
                        break;
                    case CASE_0F_D_0x24:
                    case CASE_0F_W_0x24: /* MOV Rd,TRx */
                    {
                        int rm = fetchB();
                        int which = (rm >>> 3) & 7;
                        if (rm < 0xc0) {
                            rm |= 0xc0;
                            Log.logging(Log.LogTypes.CPU, Log.LogServerities.Error,
                                    "MOV XXX,TR%u with non-register", which);
                        }
                        int eardId = lookupRMEAregd[rm];
                        int trx_value = 0;
                        if ((trx_value = (int) CPU.readTRX(which)) < 0) {
                            CPU.exception(CPU.Block.Exception.Which, CPU.Block.Exception.Error);
                            continue main_loop;// org_continue;
                        }
                        Register.Regs[eardId].setDWord(trx_value);
                    }
                        break;
                    case CASE_0F_D_0x26:
                    case CASE_0F_W_0x26: /* MOV TRx,Rd */
                    {
                        int rm = fetchB();
                        int which = (rm >>> 3) & 7;
                        if (rm < 0xc0) {
                            rm |= 0xc0;
                            Log.logging(Log.LogTypes.CPU, Log.LogServerities.Error,
                                    "MOV TR%u,XXX with non-register", which);
                        }
                        int eardId = lookupRMEAregd[rm];
                        if (CPU.writeTRX(which, Register.Regs[eardId].getDWord())) {
                            CPU.exception(CPU.Block.Exception.Which, CPU.Block.Exception.Error);
                            continue main_loop;// org_continue;
                        }
                    }
                        break;
                    case CASE_0F_D_0x31:
                    case CASE_0F_W_0x31: /* RDTSC */
                    {
                        if (CPU.ArchitectureType < CPU.ArchTypePentiumSlow) {
                            illegalOpCode(); // goto illegal_opcode
                            continue main_loop;
                        }
                        long tsc = (long) (PIC.getFullIndex() * (double) CPU.CycleMax);
                        Register.setRegEDX((int) (tsc >>> 32));
                        Register.setRegEAX((int) (tsc & 0xffffffff));
                    }
                        break;
                    case CASE_0F_W_0x80: /* JO */
                    // JumpCond16_w(TFLG_O);
                    {
                        saveIP();
                        if (Flags.getTFlgO())
                            Register.setRegIP(Register.getRegIP() + fetchWS());
                        Register.setRegIP(Register.getRegIP() + 2);
                        continue main_loop;// org_continue;
                    }
                    // break;
                    case CASE_0F_W_0x81: /* JNO */
                    // JumpCond16_w(TFLG_NO);
                    {
                        saveIP();
                        if (Flags.getTFlgNO())
                            Register.setRegIP(Register.getRegIP() + fetchWS());
                        Register.setRegIP(Register.getRegIP() + 2);
                        continue main_loop;// org_continue;
                    }
                    // break;
                    case CASE_0F_W_0x82: /* JB */
                    // JumpCond16_w(TFLG_B);
                    {
                        saveIP();
                        if (Flags.getTFlgB())
                            Register.setRegIP(Register.getRegIP() + fetchWS());
                        Register.setRegIP(Register.getRegIP() + 2);
                        continue main_loop;// org_continue;
                    }
                    // break;
                    case CASE_0F_W_0x83: /* JNB */
                    // JumpCond16_w(TFLG_NB);
                    {
                        saveIP();
                        if (Flags.getTFlgNB())
                            Register.setRegIP(Register.getRegIP() + fetchWS());
                        Register.setRegIP(Register.getRegIP() + 2);
                        continue main_loop;// org_continue;
                    }
                    // break;
                    case CASE_0F_W_0x84: /* JZ */
                    // JumpCond16_w(TFLG_Z);
                    {
                        saveIP();
                        if (Flags.getTFlgZ())
                            Register.setRegIP(Register.getRegIP() + fetchWS());
                        Register.setRegIP(Register.getRegIP() + 2);
                        continue main_loop;// org_continue;
                    }
                    // break;
                    case CASE_0F_W_0x85: /* JNZ */
                    // JumpCond16_w(TFLG_NZ);
                    {
                        saveIP();
                        if (Flags.getTFlgNZ())
                            Register.setRegIP(Register.getRegIP() + fetchWS());
                        Register.setRegIP(Register.getRegIP() + 2);
                        continue main_loop;// org_continue;
                    }
                    // break;
                    case CASE_0F_W_0x86: /* JBE */
                    // JumpCond16_w(TFLG_BE);
                    {
                        saveIP();
                        if (Flags.getTFlgBE())
                            Register.setRegIP(Register.getRegIP() + fetchWS());
                        Register.setRegIP(Register.getRegIP() + 2);
                        continue main_loop;// org_continue;
                    }
                    // break;
                    case CASE_0F_W_0x87: /* JNBE */
                    // JumpCond16_w(TFLG_NBE);
                    {
                        saveIP();
                        if (Flags.getTFlgNBE())
                            Register.setRegIP(Register.getRegIP() + fetchWS());
                        Register.setRegIP(Register.getRegIP() + 2);
                        continue main_loop;// org_continue;
                    }
                    // break;
                    case CASE_0F_W_0x88: /* JS */
                    // JumpCond16_w(TFLG_S);
                    {
                        saveIP();
                        if (Flags.getTFlgS())
                            Register.setRegIP(Register.getRegIP() + fetchWS());
                        Register.setRegIP(Register.getRegIP() + 2);
                        continue main_loop;// org_continue;
                    }
                    // break;
                    case CASE_0F_W_0x89: /* JNS */
                    // JumpCond16_w(TFLG_NS);
                    {
                        saveIP();
                        if (Flags.getTFlgNS())
                            Register.setRegIP(Register.getRegIP() + fetchWS());
                        Register.setRegIP(Register.getRegIP() + 2);
                        continue main_loop;// org_continue;
                    }
                    // break;
                    case CASE_0F_W_0x8a: /* JP */
                    // JumpCond16_w(TFLG_P);
                    {
                        saveIP();
                        if (Flags.getTFlgP())
                            Register.setRegIP(Register.getRegIP() + fetchWS());
                        Register.setRegIP(Register.getRegIP() + 2);
                        continue main_loop;// org_continue;
                    }
                    // break;
                    case CASE_0F_W_0x8b: /* JNP */
                    // JumpCond16_w(TFLG_NP);
                    {
                        saveIP();
                        if (Flags.getTFlgNP())
                            Register.setRegIP(Register.getRegIP() + fetchWS());
                        Register.setRegIP(Register.getRegIP() + 2);
                        continue main_loop;// org_continue;
                    }
                    // break;
                    case CASE_0F_W_0x8c: /* JL */
                    // JumpCond16_w(TFLG_L);
                    {
                        saveIP();
                        if (Flags.getTFlgL())
                            Register.setRegIP(Register.getRegIP() + fetchWS());
                        Register.setRegIP(Register.getRegIP() + 2);
                        continue main_loop;// org_continue;
                    }
                    // break;
                    case CASE_0F_W_0x8d: /* JNL */
                    // JumpCond16_w(TFLG_NL);
                    {
                        saveIP();
                        if (Flags.getTFlgNL())
                            Register.setRegIP(Register.getRegIP() + fetchWS());
                        Register.setRegIP(Register.getRegIP() + 2);
                        continue main_loop;// org_continue;
                    }
                    // break;
                    case CASE_0F_W_0x8e: /* JLE */
                    // JumpCond16_w(TFLG_LE);
                    {
                        saveIP();
                        if (Flags.getTFlgLE())
                            Register.setRegIP(Register.getRegIP() + fetchWS());
                        Register.setRegIP(Register.getRegIP() + 2);
                        continue main_loop;// org_continue;
                    }
                    // break;
                    case CASE_0F_W_0x8f: /* JNLE */
                    // JumpCond16_w(TFLG_NLE);
                    {
                        saveIP();
                        if (Flags.getTFlgNLE())
                            Register.setRegIP(Register.getRegIP() + fetchWS());
                        Register.setRegIP(Register.getRegIP() + 2);
                        continue main_loop;// org_continue;
                    }
                    // break;
                    case CASE_0F_D_0x90:
                    case CASE_0F_W_0x90: /* SETO */
                        setCC(Flags.getTFlgO());
                        break;
                    case CASE_0F_D_0x91:
                    case CASE_0F_W_0x91: /* SETNO */
                        setCC(Flags.getTFlgNO());
                        break;
                    case CASE_0F_D_0x92:
                    case CASE_0F_W_0x92: /* SETB */
                        setCC(Flags.getTFlgB());
                        break;
                    case CASE_0F_D_0x93:
                    case CASE_0F_W_0x93: /* SETNB */
                        setCC(Flags.getTFlgNB());
                        break;
                    case CASE_0F_D_0x94:
                    case CASE_0F_W_0x94: /* SETZ */
                        setCC(Flags.getTFlgZ());
                        break;
                    case CASE_0F_D_0x95:
                    case CASE_0F_W_0x95: /* SETNZ */
                        setCC(Flags.getTFlgNZ());
                        break;
                    case CASE_0F_D_0x96:
                    case CASE_0F_W_0x96: /* SETBE */
                        setCC(Flags.getTFlgBE());
                        break;
                    case CASE_0F_D_0x97:
                    case CASE_0F_W_0x97: /* SETNBE */
                        setCC(Flags.getTFlgNBE());
                        break;
                    case CASE_0F_D_0x98:
                    case CASE_0F_W_0x98: /* SETS */
                        setCC(Flags.getTFlgS());
                        break;
                    case CASE_0F_D_0x99:
                    case CASE_0F_W_0x99: /* SETNS */
                        setCC(Flags.getTFlgNS());
                        break;
                    case CASE_0F_D_0x9a:
                    case CASE_0F_W_0x9a: /* SETP */
                        setCC(Flags.getTFlgP());
                        break;
                    case CASE_0F_D_0x9b:
                    case CASE_0F_W_0x9b: /* SETNP */
                        setCC(Flags.getTFlgNP());
                        break;
                    case CASE_0F_D_0x9c:
                    case CASE_0F_W_0x9c: /* SETL */
                        setCC(Flags.getTFlgL());
                        break;
                    case CASE_0F_D_0x9d:
                    case CASE_0F_W_0x9d: /* SETNL */
                        setCC(Flags.getTFlgNL());
                        break;
                    case CASE_0F_D_0x9e:
                    case CASE_0F_W_0x9e: /* SETLE */
                        setCC(Flags.getTFlgLE());
                        break;
                    case CASE_0F_D_0x9f:
                    case CASE_0F_W_0x9f: /* SETNLE */
                        setCC(Flags.getTFlgNLE());
                        break;

                    case CASE_0F_W_0xa0: /* PUSH FS */
                        CPU.push16(Register.segValue(Register.SEG_NAME_FS));
                        break;
                    case CASE_0F_W_0xa1: /* POP FS */
                        if (CPU.popSeg(Register.SEG_NAME_FS, false)) {
                            CPU.exception(CPU.Block.Exception.Which, CPU.Block.Exception.Error);
                            continue main_loop;// org_continue;
                        }
                        break;
                    case CASE_0F_D_0xa2:
                    case CASE_0F_W_0xa2: /* CPUID */
                        if (!CPU.cpuId()) {
                            illegalOpCode(); // goto illegal_opcode
                            continue main_loop;
                        }
                        break;
                    case CASE_0F_W_0xa3: /* BT Ew,Gw */
                    {
                        Flags.fillFlags();
                        int rm = fetchB();
                        int rmrw = 0xffff & Register.Regs[lookupRMregw[rm]].getWord();
                        int mask = 0xffff & (1 << (rmrw & 15));
                        if (rm >= 0xc0) {
                            int earwId = lookupRMEAregw[rm];

                            Register.setFlagBit(Register.FlagCF,
                                    (Register.Regs[earwId].getWord() & mask));
                        } else {
                            int eaa = Core.EATable[rm].get();
                            eaa += (((short) rmrw) >> 4) * 2;
                            int old = Memory.readW(eaa);
                            Register.setFlagBit(Register.FlagCF, old & mask);
                        }
                        break;
                    }
                    case CASE_0F_W_0xa4: /* SHLD Ew,Gw,Ib */
                    // RMEwGwOp3(DSHLW, DSHLW, Fetchb());
                    {
                        int rm = fetchB();
                        int rmrw = Register.Regs[lookupRMregw[rm]].getWord();
                        if (rm >= 0xc0) {
                            DSHLW(lookupRMEAregw[rm], rmrw, fetchB());
                        } else {
                            DSHLW_M(Core.EATable[rm].get(), rmrw, fetchB());
                        }
                    }
                        break;
                    case CASE_0F_W_0xa5: /* SHLD Ew,Gw,CL */
                    // RMEwGwOp3(DSHLW, DSHLW, regsModule.reg_cl);
                    {
                        int rm = fetchB();
                        int rmrw = Register.Regs[lookupRMregw[rm]].getWord();
                        if (rm >= 0xc0) {
                            DSHLW(lookupRMEAregw[rm], rmrw, Register.getRegCL());
                        } else {
                            DSHLW_M(Core.EATable[rm].get(), rmrw, Register.getRegCL());
                        }
                    }
                        break;
                    case CASE_0F_W_0xa8: /* PUSH GS */
                        CPU.push16(Register.segValue(Register.SEG_NAME_GS));
                        break;
                    case CASE_0F_W_0xa9: /* POP GS */
                        if (CPU.popSeg(Register.SEG_NAME_GS, false)) {
                            CPU.exception(CPU.Block.Exception.Which, CPU.Block.Exception.Error);
                            continue main_loop;// org_continue;
                        }
                        break;
                    case CASE_0F_W_0xab: /* BTS Ew,Gw */
                    {
                        Flags.fillFlags();
                        int rm = fetchB();
                        int rmrw = Register.Regs[lookupRMregw[rm]].getWord();
                        int mask = 0xffff & (1 << (rmrw & 15));
                        if (rm >= 0xc0) {
                            int earwId = lookupRMEAregw[rm];
                            Register.setFlagBit(Register.FlagCF,
                                    (Register.Regs[earwId].getWord() & mask));
                            Register.Regs[earwId].setWord(Register.Regs[earwId].getWord() | mask);
                        } else {
                            int eaa = Core.EATable[rm].get();
                            eaa += (((short) rmrw) >> 4) * 2;
                            int old = Memory.readW(eaa);
                            Register.setFlagBit(Register.FlagCF, (old & mask));
                            Memory.writeW(eaa, old | mask);
                        }
                        break;
                    }
                    case CASE_0F_W_0xac: /* SHRD Ew,Gw,Ib */
                    // RMEwGwOp3(DSHRW, DSHRW, Fetchb());
                    {
                        int rm = fetchB();
                        int rmrw = Register.Regs[lookupRMregw[rm]].getWord();
                        if (rm >= 0xc0) {
                            DSHRW(lookupRMEAregw[rm], rmrw, fetchB());
                        } else {
                            DSHRW_M(Core.EATable[rm].get(), rmrw, fetchB());
                        }
                    }
                        break;
                    case CASE_0F_W_0xad: /* SHRD Ew,Gw,CL */
                    // RMEwGwOp3(DSHRW, DSHRW, regsModule.reg_cl);
                    {
                        int rm = fetchB();
                        int rmrw = Register.Regs[lookupRMregw[rm]].getWord();
                        if (rm >= 0xc0) {
                            DSHRW(lookupRMEAregw[rm], rmrw, Register.getRegCL());
                        } else {
                            DSHRW_M(Core.EATable[rm].get(), rmrw, Register.getRegCL());
                        }
                    }
                        break;
                    case CASE_0F_W_0xaf: /* IMUL Gw,Ew */
                        RMGwEwOp3RMrw(this::DIMULW);
                        break;
                    case CASE_0F_D_0xb0:
                    case CASE_0F_W_0xb0: /* cmpxchg Eb,Gb */
                    {
                        if (CPU.ArchitectureType < CPU.ArchType486OldSlow) {
                            illegalOpCode(); // goto illegal_opcode
                            continue main_loop;
                        }
                        Flags.fillFlags();
                        int rm = fetchB();
                        int rmrbId = lookupRMregbl[rm];
                        int rmrb = rmrbId >= 0 ? Register.Regs[rmrbId].getByteL()
                                : Register.Regs[lookupRMregbh[rm]].getByteH();
                        if (rm >= 0xc0) {
                            int earbId = lookupRMEAregbl[rm];
                            int earb = earbId >= 0 ? Register.Regs[earbId].getByteL()
                                    : Register.Regs[lookupRMEAregbh[rm]].getByteH();
                            if (Register.getRegAL() == earb) {
                                if (earbId >= 0) {
                                    Register.Regs[earbId].setByteL(rmrb);
                                } else {
                                    Register.Regs[lookupRMEAregbh[rm]].setByteH(rmrb);
                                }
                                Register.setFlagBit(Register.FlagZF, 1);
                            } else {
                                Register.setRegAL(earb);
                                Register.setFlagBit(Register.FlagZF, 0);
                            }
                        } else {
                            int eaa = Core.EATable[rm].get();
                            int val = Memory.readB(eaa);
                            if (Register.getRegAL() == val) {
                                Memory.writeB(eaa, rmrb);
                                Register.setFlagBit(Register.FlagZF, 1);
                            } else {
                                Memory.writeB(eaa, val); // cmpxchg always issues a write
                                Register.setRegAL(val);
                                Register.setFlagBit(Register.FlagZF, 0);
                            }
                        }
                        break;
                    }
                    case CASE_0F_W_0xb1: /* cmpxchg Ew,Gw */
                    {
                        if (CPU.ArchitectureType < CPU.ArchType486OldSlow) {
                            illegalOpCode(); // goto illegal_opcode
                            continue main_loop;
                        }
                        Flags.fillFlags();
                        int rm = fetchB();
                        int rmrw = Register.Regs[lookupRMregw[rm]].getWord();
                        if (rm >= 0xc0) {
                            int earwId = lookupRMEAregw[rm];
                            if (Register.getRegAX() == Register.Regs[earwId].getWord()) {
                                Register.Regs[earwId].setWord(rmrw);
                                Register.setFlagBit(Register.FlagZF, 1);
                            } else {
                                Register.setRegAX(Register.Regs[earwId].getWord());
                                Register.setFlagBit(Register.FlagZF, 0);
                            }
                        } else {
                            int eaa = Core.EATable[rm].get();
                            int val = Memory.readW(eaa);
                            if (Register.getRegAX() == val) {
                                Memory.writeW(eaa, rmrw);
                                Register.setFlagBit(Register.FlagZF, 1);
                            } else {
                                Memory.writeW(eaa, val); // cmpxchg always issues a write
                                Register.setRegAX(val);
                                Register.setFlagBit(Register.FlagZF, 0);
                            }
                        }
                        break;
                    }

                    case CASE_0F_W_0xb2: /* LSS Ew */
                    {
                        int rm = fetchB();
                        int rmrwId = lookupRMregw[rm];
                        if (rm >= 0xc0) {
                            illegalOpCode(); // goto illegal_opcode
                            continue main_loop;
                        }
                        int eaa = Core.EATable[rm].get();
                        if (CPU.setSegGeneral(Register.SEG_NAME_SS, Memory.readW(eaa + 2))) {
                            CPU.exception(CPU.Block.Exception.Which, CPU.Block.Exception.Error);
                            continue main_loop;// org_continue;
                        }
                        Register.Regs[rmrwId].setWord(Memory.readW(eaa));
                        break;
                    }
                    case CASE_0F_W_0xb3: /* BTR Ew,Gw */
                    {
                        Flags.fillFlags();
                        int rm = fetchB();
                        int rmrw = Register.Regs[lookupRMregw[rm]].getWord();
                        int mask = 1 << (rmrw & 15);
                        if (rm >= 0xc0) {
                            int earwId = lookupRMEAregw[rm];
                            Register.setFlagBit(Register.FlagCF,
                                    (Register.Regs[earwId].getWord() & mask));
                            // Register.Regs[earwId].Word &= (short)~mask;
                            Register.Regs[earwId]
                                    .setWord(Register.Regs[earwId].getWord() & (0xffff & ~mask));
                        } else {
                            int eaa = Core.EATable[rm].get();
                            eaa += (((short) rmrw) >> 4) * 2;
                            int old = Memory.readW(eaa);
                            Register.setFlagBit(Register.FlagCF, (old & mask));
                            Memory.writeW(eaa, 0xffff & (old & ~mask));
                        }
                        break;
                    }
                    case CASE_0F_W_0xb4: /* LFS Ew */
                    {
                        int rm = fetchB();
                        int rmrwId = lookupRMregw[rm];
                        if (rm >= 0xc0) {
                            illegalOpCode(); // goto illegal_opcode
                            continue main_loop;
                        }
                        int eaa = Core.EATable[rm].get();
                        if (CPU.setSegGeneral(Register.SEG_NAME_FS, Memory.readW(eaa + 2))) {
                            CPU.exception(CPU.Block.Exception.Which, CPU.Block.Exception.Error);
                            continue main_loop;// org_continue;
                        }
                        Register.Regs[rmrwId].setWord(Memory.readW(eaa));
                        break;
                    }
                    case CASE_0F_W_0xb5: /* LGS Ew */
                    {
                        int rm = fetchB();
                        int rmrwId = lookupRMregw[rm];
                        if (rm >= 0xc0) {
                            illegalOpCode(); // goto illegal_opcode
                            continue main_loop;
                        }
                        int eaa = Core.EATable[rm].get();
                        if (CPU.setSegGeneral(Register.SEG_NAME_GS, Memory.readW(eaa + 2))) {
                            CPU.exception(CPU.Block.Exception.Which, CPU.Block.Exception.Error);
                            continue main_loop;// org_continue;
                        }
                        Register.Regs[rmrwId].setWord(Memory.readW(eaa));
                        break;
                    }
                    case CASE_0F_W_0xb6: /* MOVZX Gw,Eb */
                    {
                        int rm = fetchB();
                        int rmrwId = lookupRMregw[rm];
                        if (rm >= 0xc0) {
                            int earbId = lookupRMEAregbl[rm];
                            Register.Regs[rmrwId]
                                    .setWord(earbId >= 0 ? Register.Regs[earbId].getByteL()
                                            : Register.Regs[lookupRMEAregbh[rm]].getByteH());
                        } else {
                            int eaa = Core.EATable[rm].get();
                            Register.Regs[rmrwId].setWord(Memory.readB(eaa));
                        }
                        break;
                    }
                    case CASE_0F_W_0xb7: /* MOVZX Gw,Ew */
                    case CASE_0F_W_0xbf: /* MOVSX Gw,Ew */
                    {
                        int rm = fetchB();
                        int rmrwId = lookupRMregw[rm];
                        if (rm >= 0xc0) {
                            int earwId = lookupRMEAregw[rm];
                            Register.Regs[rmrwId].setWord(Register.Regs[earwId].getWord());
                        } else {
                            int eaa = Core.EATable[rm].get();
                            Register.Regs[rmrwId].setWord(Memory.readW(eaa));
                        }
                        break;
                    }
                    case CASE_0F_W_0xba: /* GRP8 Ew,Ib */
                    {
                        Flags.fillFlags();
                        int rm = fetchB();
                        if (rm >= 0xc0) {
                            int earwId = lookupRMEAregw[rm];
                            int mask = 1 << (fetchB() & 15);
                            Register.setFlagBit(Register.FlagCF,
                                    (Register.Regs[earwId].getWord() & mask));
                            switch (rm & 0x38) {
                                case 0x20: /* BT */
                                    break;
                                case 0x28: /* BTS */
                                    // Register.Regs[earwId].Word |= mask;
                                    Register.Regs[earwId]
                                            .setWord(Register.Regs[earwId].getWord() | mask);
                                    break;
                                case 0x30: /* BTR */
                                    // Register.Regs[earwId].Word &= (short)~mask;
                                    Register.Regs[earwId].setWord(
                                            Register.Regs[earwId].getWord() & (0xffff & ~mask));
                                    break;
                                case 0x38: /* BTC */
                                    // Register.Regs[earwId].Word ^= mask;
                                    Register.Regs[earwId].setWord(
                                            0xffff & (Register.Regs[earwId].getWord() ^ mask));
                                    break;
                                default:
                                    Support.exceptionExit("CPU:0F:BA:Illegal subfunction %X",
                                            rm & 0x38);
                                    break;
                            }
                        } else {
                            int eaa = Core.EATable[rm].get();
                            int old = Memory.readW(eaa);
                            int mask = 0xffff & (1 << (fetchB() & 15));
                            Register.setFlagBit(Register.FlagCF, (old & mask));
                            switch (rm & 0x38) {
                                case 0x20: /* BT */
                                    break;
                                case 0x28: /* BTS */
                                    Memory.writeW(eaa, 0xffff & (old | mask));
                                    break;
                                case 0x30: /* BTR */
                                    Memory.writeW(eaa, 0xffff & (old & ~mask));
                                    break;
                                case 0x38: /* BTC */
                                    Memory.writeW(eaa, 0xffff & (old ^ mask));
                                    break;
                                default:
                                    Support.exceptionExit("CPU:0F:BA:Illegal subfunction %X",
                                            rm & 0x38);
                                    break;
                            }
                        }
                        break;
                    }
                    case CASE_0F_W_0xbb: /* BTC Ew,Gw */
                    {
                        Flags.fillFlags();
                        int rm = fetchB();
                        int rmrwId = lookupRMregw[rm];
                        int mask = 0xffff & (1 << (Register.Regs[rmrwId].getWord() & 15));
                        if (rm >= 0xc0) {
                            int earwId = lookupRMEAregw[rm];
                            GeneralReg32 reg = Register.Regs[earwId];
                            Register.setFlagBit(Register.FlagCF, (reg.getWord() & mask));
                            reg.setWord(0xffff & (reg.getWord() ^ mask));
                        } else {
                            int eaa = Core.EATable[rm].get();
                            eaa += (((short) Register.Regs[rmrwId].getWord()) >> 4) * 2;
                            int old = Memory.readW(eaa);
                            Register.setFlagBit(Register.FlagCF, (old & mask));
                            Memory.writeW(eaa, 0xffff & (old ^ mask));
                        }
                        break;
                    }
                    case CASE_0F_W_0xbc: /* BSF Gw,Ew */
                    {
                        int rm = fetchB();
                        int rmrwId = lookupRMregw[rm];
                        int result, value;
                        if (rm >= 0xc0) {
                            int earwId = lookupRMEAregw[rm];
                            value = Register.Regs[earwId].getWord();
                        } else {
                            int eaa = Core.EATable[rm].get();
                            value = Memory.readW(eaa);
                        }
                        if (value == 0) {
                            Register.setFlagBit(Register.FlagZF, true);
                        } else {
                            result = 0;
                            while ((value & 0x01) == 0) {
                                result++;
                                value >>>= 1;
                            }
                            Register.setFlagBit(Register.FlagZF, false);
                            Register.Regs[rmrwId].setWord(result);
                        }
                        Flags.LzFlags.Type = Flags.TypeFlag.UNKNOWN;
                        break;
                    }
                    case CASE_0F_W_0xbd: /* BSR Gw,Ew */
                    {
                        int rm = fetchB();
                        int rmrwId = lookupRMregw[rm];
                        int result, value;
                        if (rm >= 0xc0) {
                            int earwId = lookupRMEAregw[rm];
                            value = Register.Regs[earwId].getWord();
                        } else {
                            int eaa = Core.EATable[rm].get();
                            value = Memory.readW(eaa);
                        }
                        if (value == 0) {
                            Register.setFlagBit(Register.FlagZF, true);
                        } else {
                            result = 15; // Operandsize-1
                            while ((value & 0x8000) == 0) {
                                result--;
                                value <<= 1;
                            }
                            Register.setFlagBit(Register.FlagZF, false);
                            Register.Regs[rmrwId].setWord(result);
                        }
                        Flags.LzFlags.Type = Flags.TypeFlag.UNKNOWN;
                        break;
                    }
                    case CASE_0F_W_0xbe: /* MOVSX Gw,Eb */
                    {
                        int rm = fetchB();
                        int rmrwId = lookupRMregw[rm];
                        if (rm >= 0xc0) {
                            int earbId = lookupRMEAregbl[rm];
                            // sbyte
                            Register.Regs[rmrwId]
                                    .setWord((byte) (earbId >= 0 ? Register.Regs[earbId].getByteL()
                                            : Register.Regs[lookupRMEAregbh[rm]].getByteH()));
                        } else {
                            int eaa = Core.EATable[rm].get();
                            // sbyte
                            Register.Regs[rmrwId].setWord((byte) Memory.readB(eaa));
                        }
                        break;
                    }
                    case CASE_0F_D_0xc0:
                    case CASE_0F_W_0xc0: /* XADD Gb,Eb */
                    {
                        if (CPU.ArchitectureType < CPU.ArchType486OldSlow) {
                            illegalOpCode(); // goto illegal_opcode
                            continue main_loop;
                        }
                        int rm = fetchB();
                        int rmrbId = lookupRMregbl[rm];
                        int oldrmrb = rmrbId >= 0 ? Register.Regs[rmrbId].getByteL()
                                : Register.Regs[lookupRMregbh[rm]].getByteH();
                        if (rm >= 0xc0) {
                            int earbId = lookupRMEAregbl[rm];
                            if (rmrbId >= 0) {
                                if (earbId >= 0) {
                                    Register.Regs[rmrbId]
                                            .setByteL(Register.Regs[earbId].getByteL());
                                    // Register.Regs[earbId].ByteL += oldrmrb;
                                    Register.Regs[earbId]
                                            .setByteL(Register.Regs[earbId].getByteL() + oldrmrb);
                                } else {
                                    earbId = lookupRMEAregbh[rm];
                                    Register.Regs[rmrbId]
                                            .setByteL(Register.Regs[earbId].getByteH());
                                    Register.Regs[earbId]
                                            .setByteH(Register.Regs[earbId].getByteH() + oldrmrb);
                                }

                            } else {
                                rmrbId = lookupRMregbh[rm];
                                if (earbId >= 0) {
                                    Register.Regs[rmrbId]
                                            .setByteH(Register.Regs[earbId].getByteL());
                                    // Register.Regs[earbId].ByteL += oldrmrb;
                                    Register.Regs[earbId]
                                            .setByteL(Register.Regs[earbId].getByteL() + oldrmrb);
                                } else {
                                    earbId = lookupRMEAregbh[rm];
                                    Register.Regs[rmrbId]
                                            .setByteH(Register.Regs[earbId].getByteH());
                                    Register.Regs[earbId]
                                            .setByteH(Register.Regs[earbId].getByteH() + oldrmrb);
                                }
                            }
                        } else {
                            int eaa = Core.EATable[rm].get();
                            if (rmrbId >= 0) {
                                Register.Regs[rmrbId].setByteL(Memory.readB(eaa));
                            } else {
                                Register.Regs[lookupRMregbh[rm]].setByteH(Memory.readB(eaa));
                            }
                            Memory.writeB(eaa, Memory.readB(eaa) + oldrmrb);
                        }
                        break;
                    }
                    case CASE_0F_W_0xc1: /* XADD Gw,Ew */
                    {
                        if (CPU.ArchitectureType < CPU.ArchType486OldSlow) {
                            illegalOpCode(); // goto illegal_opcode
                            continue main_loop;
                        }
                        int rm = fetchB();
                        int rmrwId = lookupRMregw[rm];
                        int oldrmrw = Register.Regs[rmrwId].getWord();
                        if (rm >= 0xc0) {
                            int earwId = lookupRMEAregw[rm];
                            Register.Regs[rmrwId].setWord(Register.Regs[earwId].getWord());
                            Register.Regs[earwId]
                                    .setWord(0xffff & (Register.Regs[earwId].getWord() + oldrmrw));;
                        } else {
                            int eaa = Core.EATable[rm].get();
                            Register.Regs[rmrwId].setWord(Memory.readW(eaa));
                            Memory.writeW(eaa, 0xffff & (Memory.readW(eaa) + oldrmrw));
                        }
                        break;
                    }
                    case CASE_0F_W_0xc8: /* BSWAP AX */
                        if (CPU.ArchitectureType < CPU.ArchType486OldSlow) {
                            illegalOpCode(); // goto illegal_opcode
                            continue main_loop;
                        }
                        BSWAPW(Register.getRegAX());
                        break;
                    case CASE_0F_W_0xc9: /* BSWAP CX */
                        if (CPU.ArchitectureType < CPU.ArchType486OldSlow) {
                            illegalOpCode(); // goto illegal_opcode
                            continue main_loop;
                        }
                        BSWAPW(Register.getRegCX());
                        break;
                    case CASE_0F_W_0xca: /* BSWAP DX */
                        if (CPU.ArchitectureType < CPU.ArchType486OldSlow) {
                            illegalOpCode(); // goto illegal_opcode
                            continue main_loop;
                        }
                        BSWAPW(Register.getRegDX());
                        break;
                    case CASE_0F_W_0xcb: /* BSWAP BX */
                        if (CPU.ArchitectureType < CPU.ArchType486OldSlow) {
                            illegalOpCode(); // goto illegal_opcode
                            continue main_loop;
                        }
                        BSWAPW(Register.getRegBX());
                        break;
                    case CASE_0F_W_0xcc: /* BSWAP SP */
                        if (CPU.ArchitectureType < CPU.ArchType486OldSlow) {
                            illegalOpCode(); // goto illegal_opcode
                            continue main_loop;
                        }
                        BSWAPW(Register.getRegSP());
                        break;
                    case CASE_0F_W_0xcd: /* BSWAP BP */
                        if (CPU.ArchitectureType < CPU.ArchType486OldSlow) {
                            illegalOpCode(); // goto illegal_opcode
                            continue main_loop;
                        }
                        BSWAPW(Register.getRegBP());
                        break;
                    case CASE_0F_W_0xce: /* BSWAP SI */
                        if (CPU.ArchitectureType < CPU.ArchType486OldSlow) {
                            illegalOpCode(); // goto illegal_opcode
                            continue main_loop;
                        }
                        BSWAPW(Register.getRegSI());
                        break;
                    case CASE_0F_W_0xcf: /* BSWAP DI */
                        if (CPU.ArchitectureType < CPU.ArchType486OldSlow) {
                            illegalOpCode(); // goto illegal_opcode
                            continue main_loop;
                        }
                        BSWAPW(Register.getRegDI());
                        break;


                    // -- #endregion
                    // #include "core_normal/prefix_66.h"
                    // -- #region prefix_66


                    case CASE_D_0x01: /* ADD Ed,Gd */
                        RMEdGd(this::ADDD, this::ADDD_M);
                        break;
                    case CASE_D_0x03: /* ADD Gd,Ed */
                        RMGdEd(this::ADDD);
                        break;
                    case CASE_D_0x05: /* ADD EAX,Id */
                    // EAXId(ADDD);
                    {
                        ADDD(Register.AX, fetchD());
                    }
                        break;
                    case CASE_D_0x06: /* PUSH ES */

                        CPU.push32(Register.segValue(Register.SEG_NAME_ES));
                        break;
                    case CASE_D_0x07: /* POP ES */
                        if (CPU.popSeg(Register.SEG_NAME_ES, true)) {
                            CPU.exception(CPU.Block.Exception.Which, CPU.Block.Exception.Error);
                            continue main_loop;// org_continue;
                        }
                        break;
                    case CASE_D_0x09: /* OR Ed,Gd */
                        RMEdGd(this::ORD, this::ORD_M);
                        break;
                    case CASE_D_0x0b: /* OR Gd,Ed */
                        RMGdEd(this::ORD);
                        break;
                    case CASE_D_0x0d: /* OR EAX,Id */
                    // EAXId(ORD);
                    {
                        ORD(Register.AX, fetchD());
                    }
                        break;
                    case CASE_D_0x0e: /* PUSH CS */
                        CPU.push32(Register.segValue(Register.SEG_NAME_CS));
                        break;
                    case CASE_D_0x11: /* ADC Ed,Gd */
                        RMEdGd(this::ADCD, this::ADCD_M);
                        break;
                    case CASE_D_0x13: /* ADC Gd,Ed */
                        RMGdEd(this::ADCD);
                        break;
                    case CASE_D_0x15: /* ADC EAX,Id */
                    // EAXId(ADCD);
                    {
                        ADCD(Register.AX, fetchD());
                    }
                        break;
                    case CASE_D_0x16: /* PUSH SS */
                        CPU.push32(Register.segValue(Register.SEG_NAME_SS));
                        break;
                    case CASE_D_0x17: /* POP SS */
                        if (CPU.popSeg(Register.SEG_NAME_SS, true)) {
                            CPU.exception(CPU.Block.Exception.Which, CPU.Block.Exception.Error);
                            continue main_loop;// org_continue;
                        }
                        CPU.Cycles++;
                        break;
                    case CASE_D_0x19: /* SBB Ed,Gd */
                        RMEdGd(this::SBBD, this::SBBD_M);
                        break;
                    case CASE_D_0x1b: /* SBB Gd,Ed */
                        RMGdEd(this::SBBD);
                        break;
                    case CASE_D_0x1d: /* SBB EAX,Id */
                    // EAXId(SBBD);
                    {
                        SBBD(Register.AX, fetchD());
                    }
                        break;
                    case CASE_D_0x1e: /* PUSH DS */
                        CPU.push32(Register.segValue(Register.SEG_NAME_DS));
                        break;
                    case CASE_D_0x1f: /* POP DS */
                        if (CPU.popSeg(Register.SEG_NAME_DS, true)) {
                            CPU.exception(CPU.Block.Exception.Which, CPU.Block.Exception.Error);
                            continue main_loop;// org_continue;
                        }
                        break;
                    case CASE_D_0x21: /* AND Ed,Gd */
                        RMEdGd(this::ANDD, this::ANDD_M);
                        break;
                    case CASE_D_0x23: /* AND Gd,Ed */
                        RMGdEd(this::ANDD);
                        break;
                    case CASE_D_0x25: /* AND EAX,Id */
                    // EAXId(ANDD);
                    {
                        ANDD(Register.AX, fetchD());
                    }
                        break;
                    case CASE_D_0x29: /* SUB Ed,Gd */
                        RMEdGd(this::SUBD, this::SUBD_M);
                        break;
                    case CASE_D_0x2b: /* SUB Gd,Ed */
                        RMGdEd(this::SUBD);
                        break;
                    case CASE_D_0x2d: /* SUB EAX,Id */
                    // EAXId(SUBD);
                    {
                        SUBD(Register.AX, fetchD());
                    }
                        break;
                    case CASE_D_0x31: /* XOR Ed,Gd */
                        RMEdGd(this::XORD, this::XORD_M);
                        break;
                    case CASE_D_0x33: /* XOR Gd,Ed */
                        RMGdEd(this::XORD);
                        break;
                    case CASE_D_0x35: /* XOR EAX,Id */
                    // EAXId(XORD);
                    {
                        XORD(Register.AX, fetchD());
                    }
                        break;
                    case CASE_D_0x39: /* CMP Ed,Gd */
                        RMEdGd(this::CMPD, this::CMPD_M);
                        break;
                    case CASE_D_0x3b: /* CMP Gd,Ed */
                        RMGdEd(this::CMPD);
                        break;
                    case CASE_D_0x3d: /* CMP EAX,Id */
                    // EAXId(CMPD);
                    {
                        CMPD(Register.AX, fetchD());
                    }
                        break;
                    case CASE_D_0x40: /* INC EAX */
                        // INCD(regsModule.reg_eax); break;
                        INCD(Register.AX);
                        break;
                    case CASE_D_0x41: /* INC ECX */
                        // INCD(regsModule.reg_ecx); break;
                        INCD(Register.CX);
                        break;
                    case CASE_D_0x42: /* INC EDX */
                        // INCD(regsModule.reg_edx); break;
                        INCD(Register.DX);
                        break;
                    case CASE_D_0x43: /* INC EBX */
                        // INCD(regsModule.reg_ebx); break;
                        INCD(Register.BX);
                        break;
                    case CASE_D_0x44: /* INC ESP */
                        // INCD(regsModule.reg_esp); break;
                        INCD(Register.SP);
                        break;
                    case CASE_D_0x45: /* INC EBP */
                        // INCD(regsModule.reg_ebp); break;
                        INCD(Register.BP);
                        break;
                    case CASE_D_0x46: /* INC ESI */
                        // INCD(regsModule.reg_esi); break;
                        INCD(Register.SI);
                        break;
                    case CASE_D_0x47: /* INC EDI */
                        // INCD(regsModule.reg_edi); break;
                        INCD(Register.DI);
                        break;
                    case CASE_D_0x48: /* DEC EAX */
                        // DECD(regsModule.reg_eax); break;
                        DECD(Register.AX);
                        break;
                    case CASE_D_0x49: /* DEC ECX */
                        // DECD(regsModule.reg_ecx); break;
                        DECD(Register.CX);
                        break;
                    case CASE_D_0x4a: /* DEC EDX */
                        // DECD(regsModule.reg_edx); break;
                        DECD(Register.DX);
                        break;
                    case CASE_D_0x4b: /* DEC EBX */
                        // DECD(regsModule.reg_ebx); break;
                        DECD(Register.BX);
                        break;
                    case CASE_D_0x4c: /* DEC ESP */
                        // DECD(regsModule.reg_esp); break;
                        DECD(Register.SP);
                        break;
                    case CASE_D_0x4d: /* DEC EBP */
                        // DECD(regsModule.reg_ebp); break;
                        DECD(Register.BP);
                        break;
                    case CASE_D_0x4e: /* DEC ESI */
                        // DECD(regsModule.reg_esi); break;
                        DECD(Register.SI);
                        break;
                    case CASE_D_0x4f: /* DEC EDI */
                        // DECD(regsModule.reg_edi); break;
                        DECD(Register.DI);
                        break;
                    case CASE_D_0x50: /* PUSH EAX */
                        CPU.push32(Register.getRegEAX());
                        break;
                    case CASE_D_0x51: /* PUSH ECX */
                        CPU.push32(Register.getRegECX());
                        break;
                    case CASE_D_0x52: /* PUSH EDX */
                        CPU.push32(Register.getRegEDX());
                        break;
                    case CASE_D_0x53: /* PUSH EBX */
                        CPU.push32(Register.getRegEBX());
                        break;
                    case CASE_D_0x54: /* PUSH ESP */
                        CPU.push32(Register.getRegESP());
                        break;
                    case CASE_D_0x55: /* PUSH EBP */
                        CPU.push32(Register.getRegEBP());
                        break;
                    case CASE_D_0x56: /* PUSH ESI */
                        CPU.push32(Register.getRegESI());
                        break;
                    case CASE_D_0x57: /* PUSH EDI */
                        CPU.push32(Register.getRegEDI());
                        break;
                    case CASE_D_0x58: /* POP EAX */
                        Register.setRegEAX(CPU.pop32());
                        break;
                    case CASE_D_0x59: /* POP ECX */
                        Register.setRegECX(CPU.pop32());
                        break;
                    case CASE_D_0x5a: /* POP EDX */
                        Register.setRegEDX(CPU.pop32());
                        break;
                    case CASE_D_0x5b: /* POP EBX */
                        Register.setRegEBX(CPU.pop32());
                        break;
                    case CASE_D_0x5c: /* POP ESP */
                        Register.setRegESP(CPU.pop32());
                        break;
                    case CASE_D_0x5d: /* POP EBP */
                        Register.setRegEBP(CPU.pop32());
                        break;
                    case CASE_D_0x5e: /* POP ESI */
                        Register.setRegESI(CPU.pop32());
                        break;
                    case CASE_D_0x5f: /* POP EDI */
                        Register.setRegEDI(CPU.pop32());
                        break;
                    case CASE_D_0x60: /* PUSHAD */
                    {
                        int tmpesp = Register.getRegESP();
                        CPU.push32(Register.getRegEAX());
                        CPU.push32(Register.getRegECX());
                        CPU.push32(Register.getRegEDX());
                        CPU.push32(Register.getRegEBX());
                        CPU.push32(tmpesp);
                        CPU.push32(Register.getRegEBP());
                        CPU.push32(Register.getRegESI());
                        CPU.push32(Register.getRegEDI());
                    }
                        break;
                    case CASE_D_0x61: /* POPAD */
                        Register.setRegEDI(CPU.pop32());
                        Register.setRegESI(CPU.pop32());
                        Register.setRegEBP(CPU.pop32());
                        CPU.pop32();// Don't save ESP
                        Register.setRegEBX(CPU.pop32());
                        Register.setRegEDX(CPU.pop32());
                        Register.setRegECX(CPU.pop32());
                        Register.setRegEAX(CPU.pop32());
                        break;
                    case CASE_D_0x62: /* BOUND Ed */
                    {
                        int boundMin, boundMax;
                        int rm = fetchB();
                        int rmrd = Register.Regs[lookupRMregd[rm]].getDWord();
                        int eaa = Core.EATable[rm].get();
                        boundMin = Memory.readD(eaa);
                        boundMax = Memory.readD(eaa + 4);
                        if ((rmrd < boundMin) || (rmrd > boundMax)) {
                            {
                                CPU.exception(5);
                                continue main_loop;// org_continue;
                            }
                        }
                    }
                        break;
                    case CASE_D_0x63: /* ARPL Ed,Rd */
                    {
                        if (((CPU.Block.PMode) && (Register.Flags & Register.FlagVM) != 0)
                                || (!CPU.Block.PMode)) {
                            illegalOpCode(); // goto illegal_opcode
                            continue main_loop;
                        }
                        int rm = fetchB();
                        int rmrw = Register.Regs[lookupRMregw[rm]].getWord();
                        if (rm >= 0xc0) {
                            int eardId = lookupRMEAregd[rm];
                            int newSel = 0xffff & Register.Regs[eardId].getDWord();
                            newSel = CPU.arpl(newSel, rmrw);
                            Register.Regs[eardId].setDWord(newSel);
                        } else {
                            int eaa = Core.EATable[rm].get();
                            int newSel = Memory.readW(eaa);
                            newSel = CPU.arpl(newSel, rmrw);
                            Memory.writeD(eaa, newSel);
                        }
                    }
                        break;
                    case CASE_D_0x68: /* PUSH Id */
                        CPU.push32(fetchD());
                        break;
                    case CASE_D_0x69: /* IMUL Gd,Ed,Id */
                    // RMGdEdOp3(DIMULD, Fetchds());
                    {
                        int rm = fetchB();
                        int rmrdId = lookupRMregd[rm];
                        if (rm >= 0xc0) {
                            DIMULD(rmrdId, Register.Regs[lookupRMEAregd[rm]].getDWord(), fetchDS());
                        } else {
                            int eaa = Core.EATable[rm].get();
                            DIMULD(rmrdId, Memory.readD(eaa), fetchDS());
                        }
                    }
                        break;
                    case CASE_D_0x6a: /* PUSH Ib */
                        CPU.push32(fetchBS());
                        break;
                    case CASE_D_0x6b: /* IMUL Gd,Ed,Ib */
                    // RMGdEdOp3(DIMULD, Fetchbs());
                    {
                        int rm = fetchB();
                        int rmrdId = lookupRMregd[rm];
                        if (rm >= 0xc0) {
                            DIMULD(rmrdId, Register.Regs[lookupRMEAregd[rm]].getDWord(), fetchBS());
                        } else {
                            int eaa = Core.EATable[rm].get();
                            DIMULD(rmrdId, Memory.readD(eaa), fetchBS());
                        }
                    }
                        break;
                    case CASE_D_0x6d: /* INSD */
                        if (CPU.ioException(Register.getRegDX(), 4)) {
                            CPU.exception(CPU.Block.Exception.Which, CPU.Block.Exception.Error);
                            continue main_loop;// org_continue;
                        }
                        DoString(STRING_OP_INSD);
                        break;
                    case CASE_D_0x6f: /* OUTSD */
                        if (CPU.ioException(Register.getRegDX(), 4)) {
                            CPU.exception(CPU.Block.Exception.Which, CPU.Block.Exception.Error);
                            continue main_loop;// org_continue;
                        }
                        DoString(STRING_OP_OUTSD);
                        break;
                    case CASE_D_0x70: /* JO */
                    // JumpCond32_b(TFLG_O);
                    {
                        saveIP();
                        if (Flags.getTFlgO())
                            Register.setRegEIP(Register.getRegEIP() + fetchBS());
                        Register.setRegEIP(Register.getRegEIP() + 1);
                        continue main_loop;// org_continue;
                    }
                    // break;
                    case CASE_D_0x71: /* JNO */
                    // JumpCond32_b(TFLG_NO);
                    {
                        saveIP();
                        if (Flags.getTFlgNO())
                            Register.setRegEIP(Register.getRegEIP() + fetchBS());
                        Register.setRegEIP(Register.getRegEIP() + 1);
                        continue main_loop;// org_continue;
                    }
                    // break;
                    case CASE_D_0x72: /* JB */
                    // JumpCond32_b(TFLG_B);
                    {
                        saveIP();
                        if (Flags.getTFlgB())
                            Register.setRegEIP(Register.getRegEIP() + fetchBS());
                        Register.setRegEIP(Register.getRegEIP() + 1);
                        continue main_loop;// org_continue;
                    }
                    // break;
                    case CASE_D_0x73: /* JNB */
                    // JumpCond32_b(TFLG_NB);
                    {
                        saveIP();
                        if (Flags.getTFlgNB())
                            Register.setRegEIP(Register.getRegEIP() + fetchBS());
                        Register.setRegEIP(Register.getRegEIP() + 1);
                        continue main_loop;// org_continue;
                    }
                    // break;
                    case CASE_D_0x74: /* JZ */
                    // JumpCond32_b(TFLG_Z);
                    {
                        saveIP();
                        if (Flags.getTFlgZ())
                            Register.setRegEIP(Register.getRegEIP() + fetchBS());
                        Register.setRegEIP(Register.getRegEIP() + 1);
                        continue main_loop;// org_continue;
                    }
                    // break;
                    case CASE_D_0x75: /* JNZ */
                    // JumpCond32_b(TFLG_NZ);
                    {
                        saveIP();
                        if (Flags.getTFlgNZ())
                            Register.setRegEIP(Register.getRegEIP() + fetchBS());
                        Register.setRegEIP(Register.getRegEIP() + 1);
                        continue main_loop;// org_continue;
                    }
                    // break;
                    case CASE_D_0x76: /* JBE */
                    // JumpCond32_b(TFLG_BE);
                    {
                        saveIP();
                        if (Flags.getTFlgBE())
                            Register.setRegEIP(Register.getRegEIP() + fetchBS());
                        Register.setRegEIP(Register.getRegEIP() + 1);
                        continue main_loop;// org_continue;
                    }
                    // break;
                    case CASE_D_0x77: /* JNBE */
                    // JumpCond32_b(TFLG_NBE);
                    {
                        saveIP();
                        if (Flags.getTFlgNBE())
                            Register.setRegEIP(Register.getRegEIP() + fetchBS());
                        Register.setRegEIP(Register.getRegEIP() + 1);
                        continue main_loop;// org_continue;
                    }
                    // break;
                    case CASE_D_0x78: /* JS */
                    // JumpCond32_b(TFLG_S);
                    {
                        saveIP();
                        if (Flags.getTFlgS())
                            Register.setRegEIP(Register.getRegEIP() + fetchBS());
                        Register.setRegEIP(Register.getRegEIP() + 1);
                        continue main_loop;// org_continue;
                    }
                    // break;
                    case CASE_D_0x79: /* JNS */
                    // JumpCond32_b(TFLG_NS);
                    {
                        saveIP();
                        if (Flags.getTFlgNS())
                            Register.setRegEIP(Register.getRegEIP() + fetchBS());
                        Register.setRegEIP(Register.getRegEIP() + 1);
                        continue main_loop;// org_continue;
                    }
                    // break;
                    case CASE_D_0x7a: /* JP */
                    // JumpCond32_b(TFLG_P);
                    {
                        saveIP();
                        if (Flags.getTFlgP())
                            Register.setRegEIP(Register.getRegEIP() + fetchBS());
                        Register.setRegEIP(Register.getRegEIP() + 1);
                        continue main_loop;// org_continue;
                    }
                    // break;
                    case CASE_D_0x7b: /* JNP */
                    // JumpCond32_b(TFLG_NP);
                    {
                        saveIP();
                        if (Flags.getTFlgNP())
                            Register.setRegEIP(Register.getRegEIP() + fetchBS());
                        Register.setRegEIP(Register.getRegEIP() + 1);
                        continue main_loop;// org_continue;
                    }
                    // break;
                    case CASE_D_0x7c: /* JL */
                    // JumpCond32_b(TFLG_L);
                    {
                        saveIP();
                        if (Flags.getTFlgL())
                            Register.setRegEIP(Register.getRegEIP() + fetchBS());
                        Register.setRegEIP(Register.getRegEIP() + 1);
                        continue main_loop;// org_continue;
                    }
                    // break;
                    case CASE_D_0x7d: /* JNL */
                    // JumpCond32_b(TFLG_NL);
                    {
                        saveIP();
                        if (Flags.getTFlgNL())
                            Register.setRegEIP(Register.getRegEIP() + fetchBS());
                        Register.setRegEIP(Register.getRegEIP() + 1);
                        continue main_loop;// org_continue;
                    }
                    // break;
                    case CASE_D_0x7e: /* JLE */
                    // JumpCond32_b(TFLG_LE);
                    {
                        saveIP();
                        if (Flags.getTFlgLE())
                            Register.setRegEIP(Register.getRegEIP() + fetchBS());
                        Register.setRegEIP(Register.getRegEIP() + 1);
                        continue main_loop;// org_continue;
                    }
                    // break;
                    case CASE_D_0x7f: /* JNLE */
                    // JumpCond32_b(TFLG_NLE);
                    {
                        saveIP();
                        if (Flags.getTFlgNLE())
                            Register.setRegEIP(Register.getRegEIP() + fetchBS());
                        Register.setRegEIP(Register.getRegEIP() + 1);
                        continue main_loop;// org_continue;
                    }
                    // break;
                    case CASE_D_0x81: /* Grpl Ed,Id */
                    {
                        int rm = fetchB();
                        int which = (rm >>> 3) & 7;
                        if (rm >= 0xc0) {
                            int eardId = lookupRMEAregd[rm];
                            int id = fetchD();
                            switch (which) {
                                case 0x00:
                                    ADDD(eardId, id);
                                    break;
                                case 0x01:
                                    ORD(eardId, id);
                                    break;
                                case 0x02:
                                    ADCD(eardId, id);
                                    break;
                                case 0x03:
                                    SBBD(eardId, id);
                                    break;
                                case 0x04:
                                    ANDD(eardId, id);
                                    break;
                                case 0x05:
                                    SUBD(eardId, id);
                                    break;
                                case 0x06:
                                    XORD(eardId, id);
                                    break;
                                case 0x07:
                                    CMPD(eardId, id);
                                    break;
                            }
                        } else {
                            int eaa = Core.EATable[rm].get();
                            int id = fetchD();
                            switch (which) {
                                case 0x00:
                                    ADDD_M(eaa, id);
                                    break;
                                case 0x01:
                                    ORD_M(eaa, id);
                                    break;
                                case 0x02:
                                    ADCD_M(eaa, id);
                                    break;
                                case 0x03:
                                    SBBD_M(eaa, id);
                                    break;
                                case 0x04:
                                    ANDD_M(eaa, id);
                                    break;
                                case 0x05:
                                    SUBD_M(eaa, id);
                                    break;
                                case 0x06:
                                    XORD_M(eaa, id);
                                    break;
                                case 0x07:
                                    CMPD_M(eaa, id);
                                    break;
                            }
                        }
                    }
                        break;
                    case CASE_D_0x83: /* Grpl Ed,Ix */
                    {
                        int rm = fetchB();
                        int which = (rm >>> 3) & 7;
                        if (rm >= 0xc0) {
                            int eardId = lookupRMEAregd[rm];
                            int id = fetchBS();
                            switch (which) {
                                case 0x00:
                                    ADDD(eardId, id);
                                    break;
                                case 0x01:
                                    ORD(eardId, id);
                                    break;
                                case 0x02:
                                    ADCD(eardId, id);
                                    break;
                                case 0x03:
                                    SBBD(eardId, id);
                                    break;
                                case 0x04:
                                    ANDD(eardId, id);
                                    break;
                                case 0x05:
                                    SUBD(eardId, id);
                                    break;
                                case 0x06:
                                    XORD(eardId, id);
                                    break;
                                case 0x07:
                                    CMPD(eardId, id);
                                    break;
                            }
                        } else {
                            int eaa = Core.EATable[rm].get();
                            int id = fetchBS();
                            switch (which) {
                                case 0x00:
                                    ADDD_M(eaa, id);
                                    break;
                                case 0x01:
                                    ORD_M(eaa, id);
                                    break;
                                case 0x02:
                                    ADCD_M(eaa, id);
                                    break;
                                case 0x03:
                                    SBBD_M(eaa, id);
                                    break;
                                case 0x04:
                                    ANDD_M(eaa, id);
                                    break;
                                case 0x05:
                                    SUBD_M(eaa, id);
                                    break;
                                case 0x06:
                                    XORD_M(eaa, id);
                                    break;
                                case 0x07:
                                    CMPD_M(eaa, id);
                                    break;
                            }
                        }
                    }
                        break;
                    case CASE_D_0x85: /* TEST Ed,Gd */
                        RMEdGd(this::TESTD, this::TESTD_M);
                        break;
                    case CASE_D_0x87: /* XCHG Ed,Gd */
                    {
                        int rm = fetchB();
                        int rwrdId = lookupRMregd[rm];
                        int oldrmrd = Register.Regs[rwrdId].getDWord();
                        if (rm >= 0xc0) {
                            int eardId = lookupRMEAregd[rm];
                            Register.Regs[rwrdId].setDWord(Register.Regs[eardId].getDWord());
                            Register.Regs[eardId].setDWord(oldrmrd);
                        } else {
                            int eaa = Core.EATable[rm].get();
                            Register.Regs[rwrdId].setDWord(Memory.readD(eaa));
                            Memory.writeD(eaa, oldrmrd);
                        }
                        break;
                    }
                    case CASE_D_0x89: /* MOV Ed,Gd */
                    {
                        int rm = fetchB();
                        int rwrdId = lookupRMregd[rm];
                        if (rm >= 0xc0) {
                            int eardId = lookupRMEAregd[rm];
                            Register.Regs[eardId].setDWord(Register.Regs[rwrdId].getDWord());
                        } else {
                            int eaa = Core.EATable[rm].get();
                            Memory.writeD(eaa, Register.Regs[rwrdId].getDWord());
                        }
                        break;
                    }
                    case CASE_D_0x8b: /* MOV Gd,Ed */
                    {
                        int rm = fetchB();
                        int rwrdId = lookupRMregd[rm];
                        if (rm >= 0xc0) {
                            int eardId = lookupRMEAregd[rm];
                            Register.Regs[rwrdId].setDWord(Register.Regs[eardId].getDWord());
                        } else {
                            int eaa = Core.EATable[rm].get();
                            Register.Regs[rwrdId].setDWord(Memory.readD(eaa));
                        }
                        break;
                    }
                    case CASE_D_0x8c: /* Mov Ew,Sw */
                    {
                        int rm = fetchB();
                        int val;
                        int which = (rm >>> 3) & 7;
                        switch (which) {
                            case 0x00: /* MOV Ew,ES */
                                val = Register.segValue(Register.SEG_NAME_ES);
                                break;
                            case 0x01: /* MOV Ew,CS */
                                val = Register.segValue(Register.SEG_NAME_CS);
                                break;
                            case 0x02: /* MOV Ew,SS */
                                val = Register.segValue(Register.SEG_NAME_SS);
                                break;
                            case 0x03: /* MOV Ew,DS */
                                val = Register.segValue(Register.SEG_NAME_DS);
                                break;
                            case 0x04: /* MOV Ew,FS */
                                val = Register.segValue(Register.SEG_NAME_FS);
                                break;
                            case 0x05: /* MOV Ew,GS */
                                val = Register.segValue(Register.SEG_NAME_GS);
                                break;
                            default:
                                Log.logging(Log.LogTypes.CPU, Log.LogServerities.Error,
                                        "CPU:8c:Illegal RM Byte"); {
                                illegalOpCode(); // goto illegal_opcode
                                continue main_loop;
                            }
                        }
                        if (rm >= 0xc0) {
                            int eardId = lookupRMEAregd[rm];
                            Register.Regs[eardId].setDWord(val);
                        } else {
                            int eaa = Core.EATable[rm].get();
                            Memory.writeW(eaa, val);
                        }
                        break;
                    }
                    case CASE_D_0x8d: /* LEA Gd */
                    {
                        // Little hack to always use segprefixed version
                        int rm = fetchB();
                        int rwrdId = lookupRMregd[rm];
                        Core.BaseDS = Core.BaseSS = 0;
                        if ((Core.Prefixes & PrefixAddr) != 0) {
                            Register.Regs[rwrdId].setDWord((EATable[1][rm]).get());
                        } else {
                            Register.Regs[rwrdId].setDWord((EATable[0][rm]).get());
                        }
                        break;
                    }
                    case CASE_D_0x8f: /* POP Ed */
                    {
                        int val = CPU.pop32();
                        int rm = fetchB();
                        if (rm >= 0xc0) {
                            int eardId = lookupRMEAregd[rm];
                            Register.Regs[eardId].setDWord(val);
                        } else {
                            int eaa = Core.EATable[rm].get();
                            Memory.writeD(eaa, val);
                        }
                        break;
                    }
                    case CASE_D_0x91: /* XCHG ECX,EAX */
                    {
                        int temp = Register.getRegEAX();
                        Register.setRegEAX(Register.getRegECX());
                        Register.setRegECX(temp);
                        break;
                    }
                    case CASE_D_0x92: /* XCHG EDX,EAX */
                    {
                        int temp = Register.getRegEAX();
                        Register.setRegEAX(Register.getRegEDX());
                        Register.setRegEDX(temp);
                        break;
                    }
                    // break;
                    case CASE_D_0x93: /* XCHG EBX,EAX */
                    {
                        int temp = Register.getRegEAX();
                        Register.setRegEAX(Register.getRegEBX());
                        Register.setRegEBX(temp);
                        break;
                    }
                    // break;
                    case CASE_D_0x94: /* XCHG ESP,EAX */
                    {
                        int temp = Register.getRegEAX();
                        Register.setRegEAX(Register.getRegESP());
                        Register.setRegESP(temp);
                        break;
                    }
                    // break;
                    case CASE_D_0x95: /* XCHG EBP,EAX */
                    {
                        int temp = Register.getRegEAX();
                        Register.setRegEAX(Register.getRegEBP());
                        Register.setRegEBP(temp);
                        break;
                    }
                    // break;
                    case CASE_D_0x96: /* XCHG ESI,EAX */
                    {
                        int temp = Register.getRegEAX();
                        Register.setRegEAX(Register.getRegESI());
                        Register.setRegESI(temp);
                        break;
                    }
                    // break;
                    case CASE_D_0x97: /* XCHG EDI,EAX */
                    {
                        int temp = Register.getRegEAX();
                        Register.setRegEAX(Register.getRegEDI());
                        Register.setRegEDI(temp);
                        break;
                    }
                    // break;
                    case CASE_D_0x98: /* CWDE */
                        Register.setRegEAX(Register.getRegAX());
                        break;
                    case CASE_D_0x99: /* CDQ */
                        if ((Register.getRegEAX() & 0x80000000) != 0)
                            Register.setRegEDX(0xffffffff);
                        else
                            Register.setRegEDX(0);
                        break;
                    case CASE_D_0x9a: /* CALL FAR Ad */
                    {
                        int newip = fetchD();
                        int newcs = fetchW();
                        Flags.fillFlags();
                        CPU.call(true, newcs, newip, getIP());
                        // -- #region CPU_TRAP_CHECK
                        if (Register.getFlag(Register.FlagTF) != 0) {
                            CPU.CpuDecoder = CpuTrapDecoder;
                            return Callback.ReturnTypeNone;
                        }
                        // -- #endregion
                        continue main_loop;// org_continue;
                    }
                    case CASE_D_0x9c: /* PUSHFD */
                        if (CPU.pushf(true)) {
                            CPU.exception(CPU.Block.Exception.Which, CPU.Block.Exception.Error);
                            continue main_loop;// org_continue;
                        }
                        break;
                    case CASE_D_0x9d: /* POPFD */
                        if (CPU.popf(true)) {
                            CPU.exception(CPU.Block.Exception.Which, CPU.Block.Exception.Error);
                            continue main_loop;// org_continue;
                        }
                        // -- #region CPU_TRAP_CHECK
                        if (Register.getFlag(Register.FlagTF) != 0) {
                            CPU.CpuDecoder = CpuTrapDecoder;
                            return decodeEnd();// goto decode_end;
                        }
                        // -- #endregion
                        // -- #region CPU_PIC_CHECK
                        if (Register.getFlag(Register.FlagIF) != 0 && PIC.IRQCheck != 0)
                            return decodeEnd();// goto decode_end;
                        // -- #endregion
                        break;
                    case CASE_D_0xa1: /* MOV EAX,Od */
                    {
                        // GetEADirect;
                        int eaa;
                        if ((Core.Prefixes & PrefixAddr) != 0) {
                            eaa = Core.BaseDS + fetchD();
                        } else {
                            eaa = Core.BaseDS + fetchW();
                        }
                        Register.setRegEAX(Memory.readD(eaa));
                    }
                        break;
                    case CASE_D_0xa3: /* MOV Od,EAX */
                    {
                        // GetEADirect;
                        int eaa;
                        if ((Core.Prefixes & PrefixAddr) != 0) {
                            eaa = Core.BaseDS + fetchD();
                        } else {
                            eaa = Core.BaseDS + fetchW();
                        }
                        Memory.writeD(eaa, Register.getRegEAX());
                    }
                        break;
                    case CASE_D_0xa5: /* MOVSD */
                        DoString(STRING_OP_MOVSD);
                        break;
                    case CASE_D_0xa7: /* CMPSD */
                        DoString(STRING_OP_CMPSD);
                        break;
                    case CASE_D_0xa9: /* TEST EAX,Id */
                    // EAXId(TESTD);
                    {
                        TESTD(Register.AX, fetchD());
                    }
                        break;
                    case CASE_D_0xab: /* STOSD */
                        DoString(STRING_OP_STOSD);
                        break;
                    case CASE_D_0xad: /* LODSD */
                        DoString(STRING_OP_LODSD);
                        break;
                    case CASE_D_0xaf: /* SCASD */
                        DoString(STRING_OP_SCASD);
                        break;
                    case CASE_D_0xb8: /* MOV EAX,Id */
                        Register.setRegEAX(fetchD());
                        break;
                    case CASE_D_0xb9: /* MOV ECX,Id */
                        Register.setRegECX(fetchD());
                        break;
                    case CASE_D_0xba: /* MOV EDX,Iw */
                        Register.setRegEDX(fetchD());
                        break;
                    case CASE_D_0xbb: /* MOV EBX,Id */
                        Register.setRegEBX(fetchD());
                        break;
                    case CASE_D_0xbc: /* MOV ESP,Id */
                        Register.setRegESP(fetchD());
                        break;
                    case CASE_D_0xbd: /* MOV EBP.Id */
                        Register.setRegEBP(fetchD());
                        break;
                    case CASE_D_0xbe: /* MOV ESI,Id */
                        Register.setRegESI(fetchD());
                        break;
                    case CASE_D_0xbf: /* MOV EDI,Id */
                        Register.setRegEDI(fetchD());
                        break;
                    case CASE_D_0xc1: /* GRP2 Ed,Ib */
                        GRP2D(this::fetchB);
                        break;
                    case CASE_D_0xc2: /* RETN Iw */
                        Register.setRegEIP(CPU.pop32());
                        Register.setRegESP(Register.getRegESP() + fetchW());
                        continue main_loop;// org_continue;
                    case CASE_D_0xc3: /* RETN */
                        Register.setRegEIP(CPU.pop32());
                        continue main_loop;// org_continue;
                    case CASE_D_0xc4: /* LES */
                    {
                        int rm = fetchB();
                        int rwrdId = lookupRMregd[rm];
                        if (rm >= 0xc0) {
                            illegalOpCode(); // goto illegal_opcode
                            continue main_loop;
                        }
                        int eaa = Core.EATable[rm].get();
                        if (CPU.setSegGeneral(Register.SEG_NAME_ES, Memory.readW(eaa + 4))) {
                            CPU.exception(CPU.Block.Exception.Which, CPU.Block.Exception.Error);
                            continue main_loop;// org_continue;
                        }
                        Register.Regs[rwrdId].setDWord(Memory.readD(eaa));
                        break;
                    }
                    case CASE_D_0xc5: /* LDS */
                    {
                        int rm = fetchB();
                        int rwrdId = lookupRMregd[rm];
                        if (rm >= 0xc0) {
                            illegalOpCode(); // goto illegal_opcode
                            continue main_loop;
                        }
                        int eaa = Core.EATable[rm].get();
                        if (CPU.setSegGeneral(Register.SEG_NAME_DS, Memory.readW(eaa + 4))) {
                            CPU.exception(CPU.Block.Exception.Which, CPU.Block.Exception.Error);
                            continue main_loop;// org_continue;
                        }
                        Register.Regs[rwrdId].setDWord(Memory.readD(eaa));
                        break;
                    }
                    case CASE_D_0xc7: /* MOV Ed,Id */
                    {
                        int rm = fetchB();
                        if (rm >= 0xc0) {
                            int eardId = lookupRMEAregd[rm];
                            Register.Regs[eardId].setDWord(fetchD());
                        } else {
                            int eaa = Core.EATable[rm].get();
                            Memory.writeD(eaa, fetchD());
                        }
                        break;
                    }
                    case CASE_D_0xc8: /* ENTER Iw,Ib */
                    {
                        int bytes = fetchW();
                        int level = fetchB();
                        CPU.enter(true, bytes, level);
                    }
                        break;
                    case CASE_D_0xc9: /* LEAVE */
                        Register.setRegESP(Register.getRegESP() & CPU.Block.Stack.NotMask);
                        Register.setRegESP(Register.getRegESP()
                                | (Register.getRegEBP() & CPU.Block.Stack.Mask));
                        Register.setRegEBP(CPU.pop32());
                        break;
                    case CASE_D_0xca: /* RETF Iw */
                    {
                        int words = fetchW();
                        Flags.fillFlags();
                        CPU.ret(true, words, getIP());
                        continue main_loop;// org_continue;
                    }
                    case CASE_D_0xcb: /* RETF */
                    {
                        Flags.fillFlags();
                        CPU.ret(true, 0, getIP());
                        continue main_loop;// org_continue;
                    }
                    case CASE_D_0xcf: /* IRET */
                    {
                        CPU.iret(true, getIP());
                        // -- #region CPU_TRAP_CHECK
                        if (Register.getFlag(Register.FlagTF) != 0) {
                            CPU.CpuDecoder = CpuTrapDecoder;
                            return Callback.ReturnTypeNone;
                        }
                        // -- #endregion
                        // -- #region CPU_PIC_CHECK
                        if (Register.getFlag(Register.FlagIF) != 0 && PIC.IRQCheck != 0)
                            return Callback.ReturnTypeNone;
                        // -- #endregion
                        continue main_loop;// org_continue;
                    }
                    case CASE_D_0xd1: /* GRP2 Ed,1 */
                        GRP2D(1);
                        break;
                    case CASE_D_0xd3: /* GRP2 Ed,CL */
                        GRP2D(Register.getRegCL());
                        break;
                    case CASE_D_0xe0: /* LOOPNZ */
                        if ((Core.Prefixes & PrefixAddr) != 0) {
                            // JumpCond32_b(--regsModule.reg_ecx && ! get_ZF());
                            {
                                saveIP();
                                Register.setRegECX(Register.getRegECX() - 1);
                                if (Register.getRegECX() != 0 && !Flags.getZF())
                                    Register.setRegEIP(Register.getRegEIP() + fetchBS());
                                Register.setRegEIP(Register.getRegEIP() + 1);
                                continue main_loop;// org_continue;
                            }
                        } else {
                            // JumpCond32_b(--regsModule.reg_cx && !get_ZF());
                            {
                                saveIP();
                                Register.setRegCX(Register.getRegCX() - 1);
                                if (Register.getRegCX() != 0 && !Flags.getZF())
                                    Register.setRegEIP(Register.getRegEIP() + fetchBS());
                                Register.setRegEIP(Register.getRegEIP() + 1);
                                continue main_loop;// org_continue;
                            }
                        }
                        // break;
                    case CASE_D_0xe1: /* LOOPZ */
                        if ((Core.Prefixes & PrefixAddr) != 0) {
                            // JumpCond32_b(--regsModule.reg_ecx && get_ZF());
                            {
                                saveIP();
                                Register.setRegECX(Register.getRegECX() - 1);
                                if (Register.getRegECX() != 0 && Flags.getZF())
                                    Register.setRegEIP(Register.getRegEIP() + fetchBS());
                                Register.setRegEIP(Register.getRegEIP() + 1);
                                continue main_loop;// org_continue;
                            }
                        } else {
                            // JumpCond32_b(--regsModule.reg_cx && get_ZF());
                            {
                                saveIP();
                                Register.setRegCX(Register.getRegCX() - 1);
                                if (Register.getRegCX() != 0 && Flags.getZF())
                                    Register.setRegEIP(Register.getRegEIP() + fetchBS());
                                Register.setRegEIP(Register.getRegEIP() + 1);
                                continue main_loop;// org_continue;
                            }
                        }
                        // break;
                    case CASE_D_0xe2: /* LOOP */
                        if ((Core.Prefixes & PrefixAddr) != 0) {
                            // JumpCond32_b(--regsModule.reg_ecx);
                            {
                                saveIP();
                                Register.setRegECX(Register.getRegECX() - 1);
                                if (Register.getRegECX() != 0)
                                    Register.setRegEIP(Register.getRegEIP() + fetchBS());
                                Register.setRegEIP(Register.getRegEIP() + 1);
                                continue main_loop;// org_continue;
                            }
                        } else {
                            // JumpCond32_b(--regsModule.reg_cx);
                            {
                                saveIP();
                                Register.setRegCX(Register.getRegCX() - 1);
                                if (Register.getRegCX() != 0)
                                    Register.setRegEIP(Register.getRegEIP() + fetchBS());
                                Register.setRegEIP(Register.getRegEIP() + 1);
                                continue main_loop;// org_continue;
                            }
                        }
                        // break;
                    case CASE_D_0xe3: /* JCXZ */
                    // JumpCond32_b(!(regsModule.reg_ecx & AddrMaskTable[core.prefixes &
                    // PREFIX_ADDR]));
                    {
                        saveIP();
                        if ((Register.getRegECX() & AddrMaskTable[Core.Prefixes & PrefixAddr]) == 0)
                            Register.setRegEIP(Register.getRegEIP() + fetchBS());
                        Register.setRegEIP(Register.getRegEIP() + 1);
                        continue main_loop;// org_continue;
                    }
                    // break;
                    case CASE_D_0xe5: /* IN EAX,Ib */
                    {
                        int port = fetchB();
                        if (CPU.ioException(port, 4)) {
                            CPU.exception(CPU.Block.Exception.Which, CPU.Block.Exception.Error);
                            continue main_loop;// org_continue;
                        }
                        Register.setRegEAX(IO.readD(port));
                        break;
                    }
                    case CASE_D_0xe7: /* OUT Ib,EAX */
                    {
                        int port = fetchB();
                        if (CPU.ioException(port, 4)) {
                            CPU.exception(CPU.Block.Exception.Which, CPU.Block.Exception.Error);
                            continue main_loop;// org_continue;
                        }
                        IO.writeD(port, Register.getRegEAX());
                        break;
                    }
                    case CASE_D_0xe8: /* CALL Jd */
                    {
                        int addip = fetchDS();
                        saveIP();
                        CPU.push32(Register.getRegEIP());
                        Register.setRegEIP(Register.getRegEIP() + addip);
                        continue main_loop;// org_continue;
                    }
                    case CASE_D_0xe9: /* JMP Jd */
                    {
                        int addip = fetchDS();
                        saveIP();
                        Register.setRegEIP(Register.getRegEIP() + addip);
                        continue main_loop;// org_continue;
                    }
                    case CASE_D_0xea: /* JMP Ad */
                    {
                        int newip = fetchD();
                        int newcs = fetchW();
                        Flags.fillFlags();
                        CPU.jmp(true, newcs, newip, getIP());
                        // -- #region CPU_TRAP_CHECK
                        if (Register.getFlag(Register.FlagTF) != 0) {
                            CPU.CpuDecoder = CpuTrapDecoder;
                            return Callback.ReturnTypeNone;
                        }
                        // -- #endregion
                        continue main_loop;// org_continue;
                    }
                    case CASE_D_0xeb: /* JMP Jb */
                    {
                        int addip = fetchBS();
                        saveIP();
                        Register.setRegEIP(Register.getRegEIP() + addip);
                        continue main_loop;// org_continue;
                    }
                    case CASE_D_0xed: /* IN EAX,DX */
                        Register.setRegEAX(IO.readD(Register.getRegDX()));
                        break;
                    case CASE_D_0xef: /* OUT DX,EAX */
                        IO.writeD(Register.getRegDX(), Register.getRegEAX());
                        break;
                    case CASE_D_0xf7: /* GRP3 Ed(,Id) */
                    {
                        int rm = fetchB();
                        int which = (rm >>> 3) & 7;
                        switch (which) {
                            case 0x00: /* TEST Ed,Id */
                            case 0x01: /* TEST Ed,Id Undocumented */
                            {
                                if (rm >= 0xc0) {
                                    int eardId = lookupRMEAregd[rm];
                                    TESTD(eardId, fetchD());
                                } else {
                                    int eaa = Core.EATable[rm].get();
                                    TESTD_M(eaa, fetchD());
                                }
                                break;
                            }
                            case 0x02: /* NOT Ed */
                            {
                                if (rm >= 0xc0) {
                                    int eardId = lookupRMEAregd[rm];
                                    Register.Regs[eardId]
                                            .setDWord(~Register.Regs[eardId].getDWord());
                                } else {
                                    int eaa = Core.EATable[rm].get();
                                    Memory.writeD(eaa, ~Memory.readD(eaa));
                                }
                                break;
                            }
                            case 0x03: /* NEG Ed */
                            {
                                Flags.LzFlags.Type = Flags.TypeFlag.NEGd;
                                if (rm >= 0xc0) {
                                    int eardId = lookupRMEAregd[rm];
                                    Flags.setLzFVar1d(Register.Regs[eardId].getDWord());
                                    Flags.setLzFResd(0 - Flags.getLzFVar1d());
                                    Register.Regs[eardId].setDWord(Flags.getLzFResd());
                                } else {
                                    int eaa = Core.EATable[rm].get();
                                    Flags.setLzFVar1d(Memory.readD(eaa));
                                    Flags.setLzFResd(0 - Flags.getLzFVar1d());
                                    Memory.writeD(eaa, Flags.getLzFResd());
                                }
                                break;
                            }
                            case 0x04: /* MUL EAX,Ed */
                                // RMEd(MULD);
                                if (rm >= 0xc0) {
                                    int eardId = lookupRMEAregd[rm];
                                    MULD(eardId);
                                } else {
                                    int eaa = Core.EATable[rm].get();
                                    MULD_M(eaa);
                                }

                                break;
                            case 0x05: /* IMUL EAX,Ed */
                                // RMEd(IMULD);
                                if (rm >= 0xc0) {
                                    int eardId = lookupRMEAregd[rm];
                                    IMULD(eardId);
                                } else {
                                    int eaa = Core.EATable[rm].get();
                                    IMULD_M(eaa);
                                }

                                break;
                            case 0x06: /* DIV Ed */
                                // RMEd(DIVD);
                                if (rm >= 0xc0) {
                                    int eardId = lookupRMEAregd[rm];
                                    DIVD(eardId);
                                } else {
                                    int eaa = Core.EATable[rm].get();
                                    DIVD_M(eaa);
                                }

                                break;
                            case 0x07: /* IDIV Ed */
                                // RMEd(IDIVD);
                                if (rm >= 0xc0) {
                                    int eardId = lookupRMEAregd[rm];
                                    if (IDIVD(eardId) == SwitchReturn.Continue)
                                        continue main_loop;// org_continue;
                                } else {
                                    int eaa = Core.EATable[rm].get();
                                    if (IDIVD_M(eaa) == SwitchReturn.Continue)
                                        continue main_loop;// org_continue;
                                }

                                break;
                        }
                        break;
                    }
                    case CASE_D_0xff: /* GRP 5 Ed */
                    {
                        int rm = fetchB();
                        int which = (rm >>> 3) & 7;
                        switch (which) {
                            case 0x00: /* INC Ed */
                                // RMEd(INCD);
                                if (rm >= 0xc0) {
                                    int eardId = lookupRMEAregd[rm];
                                    INCD(eardId);
                                } else {
                                    int eaa = Core.EATable[rm].get();
                                    INCD_M(eaa);
                                }

                                break;
                            case 0x01: /* DEC Ed */
                                // RMEd(DECD);
                                if (rm >= 0xc0) {
                                    int eardId = lookupRMEAregd[rm];
                                    DECD(eardId);
                                } else {
                                    int eaa = Core.EATable[rm].get();
                                    DECD_M(eaa);
                                }

                                break;
                            case 0x02: /* CALL NEAR Ed */
                                if (rm >= 0xc0) {
                                    int eardId = lookupRMEAregd[rm];
                                    Register.setRegEIP(Register.Regs[eardId].getDWord());
                                } else {
                                    int eaa = Core.EATable[rm].get();
                                    Register.setRegEIP(Memory.readD(eaa));
                                }
                                CPU.push32(getIP());
                                continue main_loop;// org_continue;
                            case 0x03: /* CALL FAR Ed */
                            {
                                if (rm >= 0xc0) {
                                    illegalOpCode(); // goto illegal_opcode
                                    continue main_loop;
                                }
                                int eaa = Core.EATable[rm].get();
                                int newip = Memory.readD(eaa);
                                int newcs = Memory.readW(eaa + 4);
                                Flags.fillFlags();
                                CPU.call(true, newcs, newip, getIP());
                                // -- #region CPU_TRAP_CHECK
                                if (Register.getFlag(Register.FlagTF) != 0) {
                                    CPU.CpuDecoder = CpuTrapDecoder;
                                    return Callback.ReturnTypeNone;
                                }
                                // -- #endregion
                                continue main_loop;// org_continue;
                            }
                            case 0x04: /* JMP NEAR Ed */
                                if (rm >= 0xc0) {
                                    int eardId = lookupRMEAregd[rm];
                                    Register.setRegEIP(Register.Regs[eardId].getDWord());
                                } else {
                                    int eaa = Core.EATable[rm].get();
                                    Register.setRegEIP(Memory.readD(eaa));
                                }
                                continue main_loop;// org_continue;
                            case 0x05: /* JMP FAR Ed */
                            {
                                if (rm >= 0xc0) {
                                    illegalOpCode(); // goto illegal_opcode
                                    continue main_loop;
                                }
                                int eaa = Core.EATable[rm].get();
                                int newip = Memory.readD(eaa);
                                int newcs = Memory.readW(eaa + 4);
                                Flags.fillFlags();
                                CPU.jmp(true, newcs, newip, getIP());
                                // -- #region CPU_TRAP_CHECK
                                if (Register.getFlag(Register.FlagTF) != 0) {
                                    CPU.CpuDecoder = CpuTrapDecoder;
                                    return Callback.ReturnTypeNone;
                                }
                                // -- #endregion
                                continue main_loop;// org_continue;
                            }
                            // break;
                            case 0x06: /* Push Ed */
                                if (rm >= 0xc0) {
                                    int eardId = lookupRMEAregd[rm];
                                    CPU.push32(Register.Regs[eardId].getDWord());
                                } else {
                                    int eaa = Core.EATable[rm].get();
                                    CPU.push32(Memory.readD(eaa));
                                }
                                break;
                            default:
                                Log.logging(Log.LogTypes.CPU, Log.LogServerities.Error,
                                        "CPU:66:GRP5:Illegal call %2X", which); {
                                illegalOpCode(); // goto illegal_opcode
                                continue main_loop;
                            }
                        }
                        break;
                    }


                    // -- #endregion
                    // #include "core_normal/prefix_66_0f.h"
                    // -- #region prefix_66_0f

                    case CASE_0F_D_0x00: /* GRP 6 Exxx */
                    {
                        if ((Register.Flags & Register.FlagVM) != 0 || (!CPU.Block.PMode)) {
                            illegalOpCode(); // goto illegal_opcode
                            continue main_loop;
                        }
                        int rm = fetchB();
                        int which = (rm >>> 3) & 7;
                        switch (which) {
                            case 0x00: /* SLDT */
                            case 0x01: /* STR */
                            {
                                int saveval;
                                if (which == 0)
                                    saveval = CPU.sldt();
                                else
                                    saveval = CPU.str();
                                if (rm >= 0xc0) {
                                    int earwId = lookupRMEAregw[rm];
                                    Register.Regs[earwId].setWord(0xffff & saveval);
                                } else {
                                    int eaa = Core.EATable[rm].get();
                                    Memory.writeW(eaa, 0xffff & saveval);
                                }
                            }
                                break;
                            case 0x02:
                            case 0x03:
                            case 0x04:
                            case 0x05: {
                                /* Just use 16-bit loads since were only using selectors */
                                int loadval;
                                if (rm >= 0xc0) {
                                    int earwId = lookupRMEAregw[rm];
                                    loadval = Register.Regs[earwId].getWord();
                                } else {
                                    int eaa = Core.EATable[rm].get();
                                    loadval = Memory.readW(eaa);
                                }
                                switch (which) {
                                    case 0x02:
                                        if (CPU.Block.CPL != 0) {
                                            CPU.exception(CPU.ExceptionGP);
                                            continue main_loop;// org_continue;
                                        }
                                        if (CPU.lldt(loadval)) {
                                            CPU.exception(CPU.Block.Exception.Which,
                                                    CPU.Block.Exception.Error);
                                            continue main_loop;// org_continue;
                                        }
                                        break;
                                    case 0x03:
                                        if (CPU.Block.CPL != 0) {
                                            CPU.exception(CPU.ExceptionGP);
                                            continue main_loop;// org_continue;
                                        }
                                        if (CPU.ltr(loadval)) {
                                            CPU.exception(CPU.Block.Exception.Which,
                                                    CPU.Block.Exception.Error);
                                            continue main_loop;// org_continue;
                                        }
                                        break;
                                    case 0x04:
                                        CPU.verr(loadval);
                                        break;
                                    case 0x05:
                                        CPU.verw(loadval);
                                        break;
                                }
                            }
                                break;
                            default:
                                Log.logging(Log.LogTypes.CPU, Log.LogServerities.Error,
                                        "GRP6:Illegal call %2X", which); {
                                illegalOpCode(); // goto illegal_opcode
                                continue main_loop;
                            }
                        }
                    }
                        break;
                    case CASE_0F_D_0x01: /* Group 7 Ed */
                    {
                        int rm = fetchB();
                        int which = (rm >>> 3) & 7;
                        if (rm < 0xc0) { // First ones all use EA
                            int eaa = Core.EATable[rm].get();
                            int limit;
                            switch (which) {
                                case 0x00: /* SGDT */
                                    Memory.writeW(eaa, 0xffff & CPU.sgdtLimit());
                                    Memory.writeD(eaa + 2, CPU.sgdtBase());
                                    break;
                                case 0x01: /* SIDT */
                                    Memory.writeW(eaa, 0xffff & CPU.sidtLimit());
                                    Memory.writeD(eaa + 2, CPU.sidtBase());
                                    break;
                                case 0x02: /* LGDT */
                                    if (CPU.Block.PMode && CPU.Block.CPL != 0) {
                                        CPU.exception(CPU.ExceptionGP);
                                        continue main_loop;// org_continue;
                                    }
                                    CPU.lgdt(Memory.readW(eaa), Memory.readD(eaa + 2));
                                    break;
                                case 0x03: /* LIDT */
                                    if (CPU.Block.PMode && CPU.Block.CPL != 0) {
                                        CPU.exception(CPU.ExceptionGP);
                                        continue main_loop;// org_continue;
                                    }
                                    CPU.lidt(Memory.readW(eaa), Memory.readD(eaa + 2));
                                    break;
                                case 0x04: /* SMSW */
                                    Memory.writeW(eaa, 0xffff & CPU.smsw());
                                    break;
                                case 0x06: /* LMSW */
                                    limit = Memory.readW(eaa);
                                    if (CPU.lmsw(limit)) {
                                        CPU.exception(CPU.Block.Exception.Which,
                                                CPU.Block.Exception.Error);
                                        continue main_loop;// org_continue;
                                    }
                                    break;
                                case 0x07: /* INVLPG */
                                    if (CPU.Block.PMode && CPU.Block.CPL != 0) {
                                        CPU.exception(CPU.ExceptionGP);
                                        continue main_loop;// org_continue;
                                    }
                                    Paging.clearTLB();
                                    break;
                            }
                        } else {
                            int eardId = lookupRMEAregd[rm];
                            switch (which) {
                                case 0x02: /* LGDT */
                                    if (CPU.Block.PMode && CPU.Block.CPL != 0) {
                                        CPU.exception(CPU.ExceptionGP);
                                        continue main_loop;// org_continue;
                                    } {
                                    illegalOpCode(); // goto illegal_opcode
                                    continue main_loop;
                                }
                                case 0x03: /* LIDT */
                                    if (CPU.Block.PMode && CPU.Block.CPL != 0) {
                                        CPU.exception(CPU.ExceptionGP);
                                        continue main_loop;// org_continue;
                                    } {
                                    illegalOpCode(); // goto illegal_opcode
                                    continue main_loop;
                                }
                                case 0x04: /* SMSW */
                                    Register.Regs[eardId].setDWord(CPU.smsw());
                                    break;
                                case 0x06: /* LMSW */
                                    if (CPU.lmsw(Register.Regs[eardId].getDWord())) {
                                        CPU.exception(CPU.Block.Exception.Which,
                                                CPU.Block.Exception.Error);
                                        continue main_loop;// org_continue;
                                    }
                                    break;
                                default:
                                    Log.logging(Log.LogTypes.CPU, Log.LogServerities.Error,
                                            "Illegal group 7 RM subfunction %d", which); {
                                    illegalOpCode(); // goto illegal_opcode
                                    continue main_loop;
                                }
                                // break;
                            }

                        }
                    }
                        break;
                    case CASE_0F_D_0x02: /* LAR Gd,Ed */
                    {
                        if ((Register.Flags & Register.FlagVM) != 0 || (!CPU.Block.PMode)) {
                            illegalOpCode(); // goto illegal_opcode
                            continue main_loop;
                        }
                        int rm = fetchB();
                        int rwrdId = lookupRMregd[rm];
                        int ar = Register.Regs[rwrdId].getDWord();
                        int ret = ar;
                        if (rm >= 0xc0) {
                            int earwId = lookupRMEAregw[rm];
                            ret = CPU.lar(Register.Regs[earwId].getWord(), ar);
                        } else {
                            int eaa = Core.EATable[rm].get();
                            ret = CPU.lar(Memory.readW(eaa), ar);
                        }
                        ar = ret < 0 ? ar : ret;
                        Register.Regs[rwrdId].setDWord(ar);
                    }
                        break;
                    case CASE_0F_D_0x03: /* LSL Gd,Ew */
                    {
                        if ((Register.Flags & Register.FlagVM) != 0 || (!CPU.Block.PMode)) {
                            illegalOpCode(); // goto illegal_opcode
                            continue main_loop;
                        }
                        int rm = fetchB();
                        int rwrdId = lookupRMregd[rm];
                        int limit = Register.Regs[rwrdId].getDWord();
                        int ret = limit;
                        /* Just load 16-bit values for selectors */
                        if (rm >= 0xc0) {
                            int earwId = lookupRMEAregw[rm];
                            ret = CPU.lsl(Register.Regs[earwId].getWord());
                        } else {
                            int eaa = Core.EATable[rm].get();
                            ret = CPU.lsl(Memory.readW(eaa));
                        }
                        limit = ret < 0 ? limit : ret;
                        Register.Regs[rwrdId].setDWord(limit);
                    }
                        break;
                    case CASE_0F_D_0x80: /* JO */
                    // JumpCond32_d(TFLG_O);
                    {
                        saveIP();
                        if (Flags.getTFlgO())
                            Register.setRegEIP(Register.getRegEIP() + fetchDS());
                        Register.setRegEIP(Register.getRegEIP() + 4);
                        continue main_loop;// org_continue;
                    }
                    // break;
                    case CASE_0F_D_0x81: /* JNO */
                    // JumpCond32_d(TFLG_NO);
                    {
                        saveIP();
                        if (Flags.getTFlgNO())
                            Register.setRegEIP(Register.getRegEIP() + fetchDS());
                        Register.setRegEIP(Register.getRegEIP() + 4);
                        continue main_loop;// org_continue;
                    }
                    // break;
                    case CASE_0F_D_0x82: /* JB */
                    // JumpCond32_d(TFLG_B);
                    {
                        saveIP();
                        if (Flags.getTFlgB())
                            Register.setRegEIP(Register.getRegEIP() + fetchDS());
                        Register.setRegEIP(Register.getRegEIP() + 4);
                        continue main_loop;// org_continue;
                    }
                    // break;
                    case CASE_0F_D_0x83: /* JNB */
                    // JumpCond32_d(TFLG_NB);
                    {
                        saveIP();
                        if (Flags.getTFlgNB())
                            Register.setRegEIP(Register.getRegEIP() + fetchDS());
                        Register.setRegEIP(Register.getRegEIP() + 4);
                        continue main_loop;// org_continue;
                    }
                    // break;
                    case CASE_0F_D_0x84: /* JZ */
                    // JumpCond32_d(TFLG_Z);
                    {
                        saveIP();
                        if (Flags.getTFlgZ())
                            Register.setRegEIP(Register.getRegEIP() + fetchDS());
                        Register.setRegEIP(Register.getRegEIP() + 4);
                        continue main_loop;// org_continue;
                    }
                    // break;
                    case CASE_0F_D_0x85: /* JNZ */
                    // JumpCond32_d(TFLG_NZ);
                    {
                        saveIP();
                        if (Flags.getTFlgNZ())
                            Register.setRegEIP(Register.getRegEIP() + fetchDS());
                        Register.setRegEIP(Register.getRegEIP() + 4);
                        continue main_loop;// org_continue;
                    }
                    // break;
                    case CASE_0F_D_0x86: /* JBE */
                    // JumpCond32_d(TFLG_BE);
                    {
                        saveIP();
                        if (Flags.getTFlgBE())
                            Register.setRegEIP(Register.getRegEIP() + fetchDS());
                        Register.setRegEIP(Register.getRegEIP() + 4);
                        continue main_loop;// org_continue;
                    }
                    // break;
                    case CASE_0F_D_0x87: /* JNBE */
                    // JumpCond32_d(TFLG_NBE);
                    {
                        saveIP();
                        if (Flags.getTFlgNBE())
                            Register.setRegEIP(Register.getRegEIP() + fetchDS());
                        Register.setRegEIP(Register.getRegEIP() + 4);
                        continue main_loop;// org_continue;
                    }
                    // break;
                    case CASE_0F_D_0x88: /* JS */
                    // JumpCond32_d(TFLG_S);
                    {
                        saveIP();
                        if (Flags.getTFlgS())
                            Register.setRegEIP(Register.getRegEIP() + fetchDS());
                        Register.setRegEIP(Register.getRegEIP() + 4);
                        continue main_loop;// org_continue;
                    }
                    // break;
                    case CASE_0F_D_0x89: /* JNS */
                    // JumpCond32_d(TFLG_NS);
                    {
                        saveIP();
                        if (Flags.getTFlgNS())
                            Register.setRegEIP(Register.getRegEIP() + fetchDS());
                        Register.setRegEIP(Register.getRegEIP() + 4);
                        continue main_loop;// org_continue;
                    }
                    // break;
                    case CASE_0F_D_0x8a: /* JP */
                    // JumpCond32_d(TFLG_P);
                    {
                        saveIP();
                        if (Flags.getTFlgP())
                            Register.setRegEIP(Register.getRegEIP() + fetchDS());
                        Register.setRegEIP(Register.getRegEIP() + 4);
                        continue main_loop;// org_continue;
                    }
                    // break;
                    case CASE_0F_D_0x8b: /* JNP */
                    // JumpCond32_d(TFLG_NP);
                    {
                        saveIP();
                        if (Flags.getTFlgNP())
                            Register.setRegEIP(Register.getRegEIP() + fetchDS());
                        Register.setRegEIP(Register.getRegEIP() + 4);
                        continue main_loop;// org_continue;
                    }
                    // break;
                    case CASE_0F_D_0x8c: /* JL */
                    // JumpCond32_d(TFLG_L);
                    {
                        saveIP();
                        if (Flags.getTFlgL())
                            Register.setRegEIP(Register.getRegEIP() + fetchDS());
                        Register.setRegEIP(Register.getRegEIP() + 4);
                        continue main_loop;// org_continue;
                    }
                    // break;
                    case CASE_0F_D_0x8d: /* JNL */
                    // JumpCond32_d(TFLG_NL);
                    {
                        saveIP();
                        if (Flags.getTFlgNL())
                            Register.setRegEIP(Register.getRegEIP() + fetchDS());
                        Register.setRegEIP(Register.getRegEIP() + 4);
                        continue main_loop;// org_continue;
                    }
                    // break;
                    case CASE_0F_D_0x8e: /* JLE */
                    // JumpCond32_d(TFLG_LE);
                    {
                        saveIP();
                        if (Flags.getTFlgLE())
                            Register.setRegEIP(Register.getRegEIP() + fetchDS());
                        Register.setRegEIP(Register.getRegEIP() + 4);
                        continue main_loop;// org_continue;
                    }
                    // break;
                    case CASE_0F_D_0x8f: /* JNLE */
                    // JumpCond32_d(TFLG_NLE);
                    {
                        saveIP();
                        if (Flags.getTFlgNLE())
                            Register.setRegEIP(Register.getRegEIP() + fetchDS());
                        Register.setRegEIP(Register.getRegEIP() + 4);
                        continue main_loop;// org_continue;
                    }
                    // break;

                    case CASE_0F_D_0xa0: /* PUSH FS */
                        CPU.push32(Register.segValue(Register.SEG_NAME_FS));
                        break;
                    case CASE_0F_D_0xa1: /* POP FS */
                        if (CPU.popSeg(Register.SEG_NAME_FS, true)) {
                            CPU.exception(CPU.Block.Exception.Which, CPU.Block.Exception.Error);
                            continue main_loop;// org_continue;
                        }
                        break;
                    case CASE_0F_D_0xa3: /* BT Ed,Gd */
                    {
                        Flags.fillFlags();
                        int rm = fetchB();
                        int rwrdId = lookupRMregd[rm];
                        int mask = 1 << (Register.Regs[rwrdId].getDWord() & 31);
                        if (rm >= 0xc0) {
                            int eardId = lookupRMEAregd[rm];
                            Register.setFlagBit(Register.FlagCF,
                                    (Register.Regs[eardId].getDWord() & mask));
                        } else {
                            int eaa = Core.EATable[rm].get();
                            eaa += ((Register.Regs[rwrdId].getDWord()) >>> 5) * 4;
                            int old = Memory.readD(eaa);
                            Register.setFlagBit(Register.FlagCF, (old & mask));
                        }
                        break;
                    }
                    case CASE_0F_D_0xa4: /* SHLD Ed,Gd,Ib */
                    // RMEdGdOp3(DSHLD, DSHLD, Fetchb());
                    {
                        int rm = fetchB();
                        int rmrd = Register.Regs[lookupRMregd[rm]].getDWord();
                        if (rm >= 0xc0) {
                            DSHLD(lookupRMEAregd[rm], rmrd, fetchB());
                        } else {
                            DSHLD_M(Core.EATable[rm].get(), rmrd, fetchB());
                        }
                    }
                        break;
                    case CASE_0F_D_0xa5: /* SHLD Ed,Gd,CL */
                    // RMEdGdOp3(DSHLD, DSHLD, regsModule.reg_cl);
                    {
                        int rm = fetchB();
                        int rmrd = Register.Regs[lookupRMregd[rm]].getDWord();
                        if (rm >= 0xc0) {
                            DSHLD(lookupRMEAregd[rm], rmrd, Register.getRegCL());
                        } else {
                            DSHLD_M(Core.EATable[rm].get(), rmrd, Register.getRegCL());
                        }
                    }
                        break;
                    case CASE_0F_D_0xa8: /* PUSH GS */
                        CPU.push32(Register.segValue(Register.SEG_NAME_GS));
                        break;
                    case CASE_0F_D_0xa9: /* POP GS */
                        if (CPU.popSeg(Register.SEG_NAME_GS, true)) {
                            CPU.exception(CPU.Block.Exception.Which, CPU.Block.Exception.Error);
                            continue main_loop;// org_continue;
                        }
                        break;
                    case CASE_0F_D_0xab: /* BTS Ed,Gd */
                    {
                        Flags.fillFlags();
                        int rm = fetchB();
                        int rwrdId = lookupRMregd[rm];
                        int mask = 1 << (Register.Regs[rwrdId].getDWord() & 31);
                        if (rm >= 0xc0) {
                            int eardId = lookupRMEAregd[rm];
                            Register.setFlagBit(Register.FlagCF,
                                    (Register.Regs[eardId].getDWord() & mask));
                            Register.Regs[eardId].setDWord(Register.Regs[eardId].getDWord() | mask);
                        } else {
                            int eaa = Core.EATable[rm].get();
                            eaa += (Register.Regs[rwrdId].getDWord() >>> 5) * 4;
                            int old = Memory.readD(eaa);
                            Register.setFlagBit(Register.FlagCF, (old & mask));
                            Memory.writeD(eaa, old | mask);
                        }
                        break;
                    }

                    case CASE_0F_D_0xac: /* SHRD Ed,Gd,Ib */
                    // RMEdGdOp3(DSHRD, DSHRD, Fetchb());
                    {
                        int rm = fetchB();
                        int rmrd = Register.Regs[lookupRMregd[rm]].getDWord();
                        if (rm >= 0xc0) {
                            DSHRD(lookupRMEAregd[rm], rmrd, fetchB());
                        } else {
                            DSHRD_M(Core.EATable[rm].get(), rmrd, fetchB());
                        }
                    }
                        break;
                    case CASE_0F_D_0xad: /* SHRD Ed,Gd,CL */
                    // RMEdGdOp3(DSHRD, DSHRD, regsModule.reg_cl);
                    {
                        int rm = fetchB();
                        int rmrd = Register.Regs[lookupRMregd[rm]].getDWord();
                        if (rm >= 0xc0) {
                            DSHRD(lookupRMEAregd[rm], rmrd, Register.getRegCL());
                        } else {
                            DSHRD_M(Core.EATable[rm].get(), rmrd, Register.getRegCL());
                        }
                    }
                        break;
                    case CASE_0F_D_0xaf: /* IMUL Gd,Ed */
                    {
                        // RMGdEdOp3(DIMULD, *rmrd);
                        {
                            int rm = fetchB();
                            int rmrdId = lookupRMregd[rm];
                            if (rm >= 0xc0) {
                                DIMULD(rmrdId, Register.Regs[lookupRMEAregd[rm]].getDWord(),
                                        Register.Regs[rmrdId].getDWord());
                            } else {
                                int eaa = Core.EATable[rm].get();
                                DIMULD(rmrdId, Memory.readD(eaa), Register.Regs[rmrdId].getDWord());
                            }
                        }
                        break;
                    }
                    case CASE_0F_D_0xb1: /* CMPXCHG Ed,Gd */
                    {
                        if (CPU.ArchitectureType < CPU.ArchType486NewSlow) {
                            illegalOpCode(); // goto illegal_opcode
                            continue main_loop;
                        }
                        Flags.fillFlags();
                        int rm = fetchB();
                        int rwrdId = lookupRMregd[rm];
                        if (rm >= 0xc0) {
                            int eardId = lookupRMEAregd[rm];
                            if (Register.Regs[eardId].getDWord() == Register.getRegEAX()) {
                                Register.Regs[eardId].setDWord(Register.Regs[rwrdId].getDWord());
                                Register.setFlagBit(Register.FlagZF, 1);
                            } else {
                                Register.setRegEAX(Register.Regs[eardId].getDWord());
                                Register.setFlagBit(Register.FlagZF, 0);
                            }
                        } else {
                            int eaa = Core.EATable[rm].get();
                            int val = Memory.readD(eaa);
                            if (val == Register.getRegEAX()) {
                                Memory.writeD(eaa, Register.Regs[rwrdId].getDWord());
                                Register.setFlagBit(Register.FlagZF, 1);
                            } else {
                                Memory.writeD(eaa, val); // cmpxchg always issues a write
                                Register.setRegEAX(val);
                                Register.setFlagBit(Register.FlagZF, 0);
                            }
                        }
                        break;
                    }
                    case CASE_0F_D_0xb2: /* LSS Ed */
                    {
                        int rm = fetchB();
                        int rwrdId = lookupRMregd[rm];
                        if (rm >= 0xc0) {
                            illegalOpCode(); // goto illegal_opcode
                            continue main_loop;
                        }
                        int eaa = Core.EATable[rm].get();
                        if (CPU.setSegGeneral(Register.SEG_NAME_SS, Memory.readW(eaa + 4))) {
                            CPU.exception(CPU.Block.Exception.Which, CPU.Block.Exception.Error);
                            continue main_loop;// org_continue;
                        }
                        Register.Regs[rwrdId].setDWord(Memory.readD(eaa));
                        break;
                    }
                    case CASE_0F_D_0xb3: /* BTR Ed,Gd */
                    {
                        Flags.fillFlags();
                        int rm = fetchB();
                        int rwrdId = lookupRMregd[rm];
                        int mask = 1 << (Register.Regs[rwrdId].getDWord() & 31);
                        if (rm >= 0xc0) {
                            int eardId = lookupRMEAregd[rm];
                            Register.setFlagBit(Register.FlagCF,
                                    (Register.Regs[eardId].getDWord() & mask));
                            Register.Regs[eardId]
                                    .setDWord(Register.Regs[eardId].getDWord() & ~mask);
                        } else {
                            int eaa = Core.EATable[rm].get();
                            eaa += (Register.Regs[rwrdId].getDWord() >>> 5) * 4;
                            int old = Memory.readD(eaa);
                            Register.setFlagBit(Register.FlagCF, (old & mask));
                            Memory.writeD(eaa, old & ~mask);
                        }
                        break;
                    }
                    case CASE_0F_D_0xb4: /* LFS Ed */
                    {
                        int rm = fetchB();
                        int rwrdId = lookupRMregd[rm];
                        if (rm >= 0xc0) {
                            illegalOpCode(); // goto illegal_opcode
                            continue main_loop;
                        }
                        int eaa = Core.EATable[rm].get();
                        if (CPU.setSegGeneral(Register.SEG_NAME_FS, Memory.readW(eaa + 4))) {
                            CPU.exception(CPU.Block.Exception.Which, CPU.Block.Exception.Error);
                            continue main_loop;// org_continue;
                        }
                        Register.Regs[rwrdId].setDWord(Memory.readD(eaa));
                        break;
                    }
                    case CASE_0F_D_0xb5: /* LGS Ed */
                    {
                        int rm = fetchB();
                        int rwrdId = lookupRMregd[rm];
                        if (rm >= 0xc0) {
                            illegalOpCode(); // goto illegal_opcode
                            continue main_loop;
                        }
                        int eaa = Core.EATable[rm].get();
                        if (CPU.setSegGeneral(Register.SEG_NAME_GS, Memory.readW(eaa + 4))) {
                            CPU.exception(CPU.Block.Exception.Which, CPU.Block.Exception.Error);
                            continue main_loop;// org_continue;
                        }
                        Register.Regs[rwrdId].setDWord(Memory.readD(eaa));
                        break;
                    }
                    case CASE_0F_D_0xb6: /* MOVZX Gd,Eb */
                    {
                        int rm = fetchB();
                        int rwrdId = lookupRMregd[rm];
                        if (rm >= 0xc0) {
                            int earbId = lookupRMEAregbl[rm];
                            Register.Regs[rwrdId]
                                    .setDWord(earbId >= 0 ? Register.Regs[earbId].getByteL()
                                            : Register.Regs[lookupRMEAregbh[rm]].getByteH());
                        } else {
                            int eaa = Core.EATable[rm].get();
                            Register.Regs[rwrdId].setDWord(Memory.readB(eaa));
                        }
                        break;
                    }
                    case CASE_0F_D_0xb7: /* MOVXZ Gd,Ew */
                    {
                        int rm = fetchB();
                        int rwrdId = lookupRMregd[rm];
                        if (rm >= 0xc0) {
                            int earwId = lookupRMEAregw[rm];
                            Register.Regs[rwrdId].setDWord(Register.Regs[earwId].getWord());
                        } else {
                            int eaa = Core.EATable[rm].get();
                            Register.Regs[rwrdId].setDWord(Memory.readW(eaa));
                        }
                        break;
                    }
                    case CASE_0F_D_0xba: /* GRP8 Ed,Ib */
                    {
                        Flags.fillFlags();
                        int rm = fetchB();
                        if (rm >= 0xc0) {
                            int eardId = lookupRMEAregd[rm];
                            int mask = 1 << (fetchB() & 31);
                            Register.setFlagBit(Register.FlagCF,
                                    (Register.Regs[eardId].getDWord() & mask));
                            switch (rm & 0x38) {
                                case 0x20: /* BT */
                                    break;
                                case 0x28: /* BTS */
                                    // Register.Regs[eardId].DWord |= mask;
                                    Register.Regs[eardId]
                                            .setDWord(Register.Regs[eardId].getDWord() | mask);
                                    break;
                                case 0x30: /* BTR */
                                    // Register.Regs[eardId].DWord &= ~mask;
                                    Register.Regs[eardId]
                                            .setDWord(Register.Regs[eardId].getDWord() & ~mask);
                                    break;
                                case 0x38: /* BTC */
                                    if (Register.getFlag(Register.FlagCF) != 0)
                                        // Register.Regs[eardId].DWord &= ~mask;
                                        Register.Regs[eardId]
                                                .setDWord(Register.Regs[eardId].getDWord() & ~mask);
                                    else
                                        // Register.Regs[eardId].DWord |= mask;
                                        Register.Regs[eardId]
                                                .setDWord(Register.Regs[eardId].getDWord() | mask);
                                    break;
                                default:
                                    Support.exceptionExit("CPU:66:0F:BA:Illegal subfunction %X",
                                            rm & 0x38);
                                    break;
                            }
                        } else {
                            int eaa = Core.EATable[rm].get();
                            int old = Memory.readD(eaa);
                            int mask = 1 << (fetchB() & 31);
                            Register.setFlagBit(Register.FlagCF, (old & mask));
                            switch (rm & 0x38) {
                                case 0x20: /* BT */
                                    break;
                                case 0x28: /* BTS */
                                    Memory.writeD(eaa, old | mask);
                                    break;
                                case 0x30: /* BTR */
                                    Memory.writeD(eaa, old & ~mask);
                                    break;
                                case 0x38: /* BTC */
                                    if (Register.getFlag(Register.FlagCF) != 0)
                                        old &= ~mask;
                                    else
                                        old |= mask;
                                    Memory.writeD(eaa, old);
                                    break;
                                default:
                                    Support.exceptionExit("CPU:66:0F:BA:Illegal subfunction %X",
                                            rm & 0x38);
                                    break;
                            }
                        }
                        break;
                    }
                    case CASE_0F_D_0xbb: /* BTC Ed,Gd */
                    {
                        Flags.fillFlags();
                        int rm = fetchB();
                        int rwrdId = lookupRMregd[rm];
                        int mask = 1 << (Register.Regs[rwrdId].getDWord() & 31);
                        if (rm >= 0xc0) {
                            int eardId = lookupRMEAregd[rm];
                            Register.setFlagBit(Register.FlagCF,
                                    (Register.Regs[eardId].getDWord() & mask));
                            // Register.Regs[eardId].DWord ^= mask;
                            Register.Regs[eardId].setDWord(Register.Regs[eardId].getDWord() ^ mask);
                        } else {
                            int eaa = Core.EATable[rm].get();
                            eaa += (Register.Regs[rwrdId].getDWord() >>> 5) * 4;
                            int old = Memory.readD(eaa);
                            Register.setFlagBit(Register.FlagCF, (old & mask));
                            Memory.writeD(eaa, old ^ mask);
                        }
                        break;
                    }
                    case CASE_0F_D_0xbc: /* BSF Gd,Ed */
                    {
                        int rm = fetchB();
                        int rwrdId = lookupRMregd[rm];
                        int result, value;
                        if (rm >= 0xc0) {
                            int eardId = lookupRMEAregd[rm];
                            value = Register.Regs[eardId].getDWord();
                        } else {
                            int eaa = Core.EATable[rm].get();
                            value = Memory.readD(eaa);
                        }
                        if (value == 0) {
                            Register.setFlagBit(Register.FlagZF, true);
                        } else {
                            result = 0;
                            while ((value & 0x01) == 0) {
                                result++;
                                value >>>= 1;
                            }
                            Register.setFlagBit(Register.FlagZF, false);
                            Register.Regs[rwrdId].setDWord(result);
                        }
                        Flags.LzFlags.Type = Flags.TypeFlag.UNKNOWN;
                        break;
                    }
                    case CASE_0F_D_0xbd: /* BSR Gd,Ed */
                    {
                        int rm = fetchB();
                        int rwrdId = lookupRMregd[rm];
                        int result, value;
                        if (rm >= 0xc0) {
                            int eardId = lookupRMEAregd[rm];
                            value = Register.Regs[eardId].getDWord();
                        } else {
                            int eaa = Core.EATable[rm].get();
                            value = Memory.readD(eaa);
                        }
                        if (value == 0) {
                            Register.setFlagBit(Register.FlagZF, true);
                        } else {
                            result = 31; // Operandsize-1
                            while ((value & 0x80000000) == 0) {
                                result--;
                                value <<= 1;
                            }
                            Register.setFlagBit(Register.FlagZF, false);
                            Register.Regs[rwrdId].setDWord(result);
                        }
                        Flags.LzFlags.Type = Flags.TypeFlag.UNKNOWN;
                        break;
                    }
                    case CASE_0F_D_0xbe: /* MOVSX Gd,Eb */
                    {
                        int rm = fetchB();
                        int rwrdId = lookupRMregd[rm];
                        if (rm >= 0xc0) {
                            int earbId = lookupRMEAregbl[rm];
                            // sbyte
                            Register.Regs[rwrdId]
                                    .setDWord((byte) (earbId >= 0 ? Register.Regs[earbId].getByteL()
                                            : Register.Regs[lookupRMEAregbh[rm]].getByteH()));
                        } else {
                            int eaa = Core.EATable[rm].get();
                            // sbyte
                            Register.Regs[rwrdId].setDWord((byte) Memory.readB(eaa));
                        }
                        break;
                    }
                    case CASE_0F_D_0xbf: /* MOVSX Gd,Ew */
                    {
                        int rm = fetchB();
                        int rwrdId = lookupRMregd[rm];
                        if (rm >= 0xc0) {
                            int earwId = lookupRMEAregw[rm];
                            Register.Regs[rwrdId].setDWord(Register.Regs[earwId].getWord());
                        } else {
                            int eaa = Core.EATable[rm].get();
                            Register.Regs[rwrdId].setDWord((short)Memory.readW(eaa));
                        }
                        break;
                    }
                    case CASE_0F_D_0xc1: /* XADD Gd,Ed */
                    {
                        if (CPU.ArchitectureType < CPU.ArchType486OldSlow) {
                            illegalOpCode(); // goto illegal_opcode
                            continue main_loop;
                        }
                        int rm = fetchB();
                        int rwrdId = lookupRMregd[rm];
                        int oldrmrd = Register.Regs[rwrdId].getDWord();
                        if (rm >= 0xc0) {
                            int eardId = lookupRMEAregd[rm];
                            Register.Regs[rwrdId].setDWord(Register.Regs[eardId].getDWord());
                            Register.Regs[eardId]
                                    .setDWord(Register.Regs[eardId].getDWord() + oldrmrd);
                        } else {
                            int eaa = Core.EATable[rm].get();
                            Register.Regs[rwrdId].setDWord(Memory.readD(eaa));
                            Memory.writeD(eaa, Memory.readD(eaa) + oldrmrd);
                        }
                        break;
                    }
                    case CASE_0F_D_0xc8: /* BSWAP EAX */
                        if (CPU.ArchitectureType < CPU.ArchType486OldSlow) {
                            illegalOpCode(); // goto illegal_opcode
                            continue main_loop;
                        }
                        BSWAPD(Register.getRegEAX());
                        break;
                    case CASE_0F_D_0xc9: /* BSWAP ECX */
                        if (CPU.ArchitectureType < CPU.ArchType486OldSlow) {
                            illegalOpCode(); // goto illegal_opcode
                            continue main_loop;
                        }
                        BSWAPD(Register.getRegECX());
                        break;
                    case CASE_0F_D_0xca: /* BSWAP EDX */
                        if (CPU.ArchitectureType < CPU.ArchType486OldSlow) {
                            illegalOpCode(); // goto illegal_opcode
                            continue main_loop;
                        }
                        BSWAPD(Register.getRegEDX());
                        break;
                    case CASE_0F_D_0xcb: /* BSWAP EBX */
                        if (CPU.ArchitectureType < CPU.ArchType486OldSlow) {
                            illegalOpCode(); // goto illegal_opcode
                            continue main_loop;
                        }
                        BSWAPD(Register.getRegEBX());
                        break;
                    case CASE_0F_D_0xcc: /* BSWAP ESP */
                        if (CPU.ArchitectureType < CPU.ArchType486OldSlow) {
                            illegalOpCode(); // goto illegal_opcode
                            continue main_loop;
                        }
                        BSWAPD(Register.getRegESP());
                        break;
                    case CASE_0F_D_0xcd: /* BSWAP EBP */
                        if (CPU.ArchitectureType < CPU.ArchType486OldSlow) {
                            illegalOpCode(); // goto illegal_opcode
                            continue main_loop;
                        }
                        BSWAPD(Register.getRegEBP());
                        break;
                    case CASE_0F_D_0xce: /* BSWAP ESI */
                        if (CPU.ArchitectureType < CPU.ArchType486OldSlow) {
                            illegalOpCode(); // goto illegal_opcode
                            continue main_loop;
                        }
                        BSWAPD(Register.getRegESI());
                        break;
                    case CASE_0F_D_0xcf: /* BSWAP EDI */
                        if (CPU.ArchitectureType < CPU.ArchType486OldSlow) {
                            illegalOpCode(); // goto illegal_opcode
                            continue main_loop;
                        }
                        BSWAPD(Register.getRegEDI());
                        break;

                    // -- #endregion
                    default:
                        illegalOpCode();
                        continue main_loop;
                    // illegal_opcode:
                    // CPU.Exception(6, 0);
                    // continue main_loop;//org_continue;
                }

                break restart_opcode;
            }
            saveIP();
        }
        Flags.fillFlags();
        return Callback.ReturnTypeNone;
        // decode_end:
        // SAVEIP();
        // Flags.FillFlags();
        // return (int)Callback.ReturnTypeNone;
    }

    public void illegalOpCode() {
        CPU.exception(6, 0);
    }

    public int decodeEnd() {
        saveIP();
        Flags.fillFlags();
        return Callback.ReturnTypeNone;
    }

    /*--------------------------- end CpuCoreRunMethod -----------------------------*/
    /*--------------------------- begin CpuCoreTable_EA -----------------------------*/


    /* The MOD/RM Decoder for EA for this decoder's addressing modes */
    private int EA_16_00_n() {
        return Core.BaseDS + (0xffff & (Register.getRegBX() + (short) Register.getRegSI()));
    }

    private int EA_16_01_n() {
        return Core.BaseDS + (0xffff & (Register.getRegBX() + (short) Register.getRegDI()));
    }

    private int EA_16_02_n() {
        return Core.BaseSS + (0xffff & (Register.getRegBP() + (short) Register.getRegSI()));
    }

    private int EA_16_03_n() {
        return Core.BaseSS + (0xffff & (Register.getRegBP() + (short) Register.getRegDI()));
    }

    private int EA_16_04_n() {
        return Core.BaseDS + (0xffff & (Register.getRegSI()));
    }

    private int EA_16_05_n() {
        return Core.BaseDS + (0xffff & (Register.getRegDI()));
    }

    private int EA_16_06_n() {
        return Core.BaseDS + (0xffff & (fetchW()));
    }

    private int EA_16_07_n() {
        return Core.BaseDS + (0xffff & (Register.getRegBX()));
    }

    private int EA_16_40_n() {
        return Core.BaseDS
                + (0xffff & (Register.getRegBX() + (short) Register.getRegSI() + fetchBS()));
    }

    private int EA_16_41_n() {
        return Core.BaseDS
                + (0xffff & (Register.getRegBX() + (short) Register.getRegDI() + fetchBS()));
    }

    private int EA_16_42_n() {
        return Core.BaseSS
                + (0xffff & (Register.getRegBP() + (short) Register.getRegSI() + fetchBS()));
    }

    private int EA_16_43_n() {
        return Core.BaseSS
                + (0xffff & (Register.getRegBP() + (short) Register.getRegDI() + fetchBS()));
    }

    private int EA_16_44_n() {
        return Core.BaseDS + (0xffff & (Register.getRegSI() + fetchBS()));
    }

    private int EA_16_45_n() {
        return Core.BaseDS + (0xffff & (Register.getRegDI() + fetchBS()));
    }

    private int EA_16_46_n() {
        return Core.BaseSS + (0xffff & (Register.getRegBP() + fetchBS()));
    }

    private int EA_16_47_n() {
        return Core.BaseDS + (0xffff & (Register.getRegBX() + fetchBS()));
    }

    private int EA_16_80_n() {
        return Core.BaseDS
                + (0xffff & (Register.getRegBX() + (short) Register.getRegSI() + fetchWS()));
    }

    private int EA_16_81_n() {
        return Core.BaseDS
                + (0xffff & (Register.getRegBX() + (short) Register.getRegDI() + fetchWS()));
    }

    private int EA_16_82_n() {
        return Core.BaseSS
                + (0xffff & (Register.getRegBP() + (short) Register.getRegSI() + fetchWS()));
    }

    private int EA_16_83_n() {
        return Core.BaseSS
                + (0xffff & (Register.getRegBP() + (short) Register.getRegDI() + fetchWS()));
    }

    private int EA_16_84_n() {
        return Core.BaseDS + (0xffff & (Register.getRegSI() + fetchWS()));
    }

    private int EA_16_85_n() {
        return Core.BaseDS + (0xffff & (Register.getRegDI() + fetchWS()));
    }

    private int EA_16_86_n() {
        return Core.BaseSS + (0xffff & (Register.getRegBP() + fetchWS()));
    }

    private int EA_16_87_n() {
        return Core.BaseDS + (0xffff & (Register.getRegBX() + fetchWS()));
    }

    private int Sib(int mode) {
        int sib = fetchB();
        int baseAddr = 0;
        int otherBase = 0;
        switch (sib & 7) {
            case 0: /* EAX Base */
                baseAddr = Core.BaseDS + Register.getRegEAX();
                break;
            case 1: /* ECX Base */
                baseAddr = Core.BaseDS + Register.getRegECX();
                break;
            case 2: /* EDX Base */
                baseAddr = Core.BaseDS + Register.getRegEDX();
                break;
            case 3: /* EBX Base */
                baseAddr = Core.BaseDS + Register.getRegEBX();
                break;
            case 4: /* ESP Base */
                baseAddr = Core.BaseSS + Register.getRegESP();
                break;
            case 5: /* #1 Base */
                if (mode == 0) {
                    baseAddr = Core.BaseDS + fetchD();
                    break;
                } else {
                    baseAddr = Core.BaseSS + Register.getRegEBP();
                    break;
                }
            case 6: /* ESI Base */
                baseAddr = Core.BaseDS + Register.getRegESI();
                break;
            case 7: /* EDI Base */
                baseAddr = Core.BaseDS + Register.getRegEDI();
                break;
        }

        switch ((sib >>> 3) & 7) {
            case 0:
                otherBase = Register.getRegEAX();
                break;
            case 1:
                otherBase = Register.getRegECX();
                break;
            case 2:
                otherBase = Register.getRegEDX();
                break;
            case 3:
                otherBase = Register.getRegEBX();
                break;
            case 4:
                otherBase = 0;
                break;
            case 5:
                otherBase = Register.getRegEBP();
                break;
            case 6:
                otherBase = Register.getRegESI();
                break;
            case 7:
                otherBase = Register.getRegEDI();
                break;
        }
        baseAddr += otherBase << (sib >>> 6);
        return baseAddr;
    }


    private int EA_32_00_n() {
        return Core.BaseDS + Register.getRegEAX();
    }

    private int EA_32_01_n() {
        return Core.BaseDS + Register.getRegECX();
    }

    private int EA_32_02_n() {
        return Core.BaseDS + Register.getRegEDX();
    }

    private int EA_32_03_n() {
        return Core.BaseDS + Register.getRegEBX();
    }

    private int EA_32_04_n() {
        return Sib(0);
    }

    private int EA_32_05_n() {
        return Core.BaseDS + fetchD();
    }

    private int EA_32_06_n() {
        return Core.BaseDS + Register.getRegESI();
    }

    private int EA_32_07_n() {
        return Core.BaseDS + Register.getRegEDI();
    }

    private int EA_32_40_n() {
        return Core.BaseDS + Register.getRegEAX() + fetchBS();
    }

    private int EA_32_41_n() {
        return Core.BaseDS + Register.getRegECX() + fetchBS();
    }

    private int EA_32_42_n() {
        return Core.BaseDS + Register.getRegEDX() + fetchBS();
    }

    private int EA_32_43_n() {
        return Core.BaseDS + Register.getRegEBX() + fetchBS();
    }

    private int EA_32_44_n() {
        int temp = Sib(1);
        return temp + fetchBS();
    }

    // static int EA_32_44_n(void) { return Sib(1)+Fetchbs();}
    private int EA_32_45_n() {
        return Core.BaseSS + Register.getRegEBP() + fetchBS();
    }

    private int EA_32_46_n() {
        return Core.BaseDS + Register.getRegESI() + fetchBS();
    }

    private int EA_32_47_n() {
        return Core.BaseDS + Register.getRegEDI() + fetchBS();
    }

    private int EA_32_80_n() {
        return Core.BaseDS + Register.getRegEAX() + fetchDS();
    }

    private int EA_32_81_n() {
        return Core.BaseDS + Register.getRegECX() + fetchDS();
    }

    private int EA_32_82_n() {
        return Core.BaseDS + Register.getRegEDX() + fetchDS();
    }

    private int EA_32_83_n() {
        return Core.BaseDS + Register.getRegEBX() + fetchDS();
    }

    private int EA_32_84_n() {
        int temp = Sib(2);
        return temp + fetchDS();
    }

    // static int EA_32_84_n(void) { return Sib(2)+Fetchds();}
    private int EA_32_85_n() {
        return Core.BaseSS + Register.getRegEBP() + fetchDS();
    }

    private int EA_32_86_n() {
        return Core.BaseDS + Register.getRegESI() + fetchDS();
    }

    private int EA_32_87_n() {
        return Core.BaseDS + Register.getRegEDI() + fetchDS();
    }

    public EAHandler[][] EATable = new EAHandler[2][];

    protected void initEATable() {
        EATable[0] = new EAHandler[] {// 256
                /* 00 */
                this::EA_16_00_n, this::EA_16_01_n, this::EA_16_02_n, this::EA_16_03_n,
                this::EA_16_04_n, this::EA_16_05_n, this::EA_16_06_n, this::EA_16_07_n,
                this::EA_16_00_n, this::EA_16_01_n, this::EA_16_02_n, this::EA_16_03_n,
                this::EA_16_04_n, this::EA_16_05_n, this::EA_16_06_n, this::EA_16_07_n,
                this::EA_16_00_n, this::EA_16_01_n, this::EA_16_02_n, this::EA_16_03_n,
                this::EA_16_04_n, this::EA_16_05_n, this::EA_16_06_n, this::EA_16_07_n,
                this::EA_16_00_n, this::EA_16_01_n, this::EA_16_02_n, this::EA_16_03_n,
                this::EA_16_04_n, this::EA_16_05_n, this::EA_16_06_n, this::EA_16_07_n,
                this::EA_16_00_n, this::EA_16_01_n, this::EA_16_02_n, this::EA_16_03_n,
                this::EA_16_04_n, this::EA_16_05_n, this::EA_16_06_n, this::EA_16_07_n,
                this::EA_16_00_n, this::EA_16_01_n, this::EA_16_02_n, this::EA_16_03_n,
                this::EA_16_04_n, this::EA_16_05_n, this::EA_16_06_n, this::EA_16_07_n,
                this::EA_16_00_n, this::EA_16_01_n, this::EA_16_02_n, this::EA_16_03_n,
                this::EA_16_04_n, this::EA_16_05_n, this::EA_16_06_n, this::EA_16_07_n,
                this::EA_16_00_n, this::EA_16_01_n, this::EA_16_02_n, this::EA_16_03_n,
                this::EA_16_04_n, this::EA_16_05_n, this::EA_16_06_n, this::EA_16_07_n,
                /* 01 */
                this::EA_16_40_n, this::EA_16_41_n, this::EA_16_42_n, this::EA_16_43_n,
                this::EA_16_44_n, this::EA_16_45_n, this::EA_16_46_n, this::EA_16_47_n,
                this::EA_16_40_n, this::EA_16_41_n, this::EA_16_42_n, this::EA_16_43_n,
                this::EA_16_44_n, this::EA_16_45_n, this::EA_16_46_n, this::EA_16_47_n,
                this::EA_16_40_n, this::EA_16_41_n, this::EA_16_42_n, this::EA_16_43_n,
                this::EA_16_44_n, this::EA_16_45_n, this::EA_16_46_n, this::EA_16_47_n,
                this::EA_16_40_n, this::EA_16_41_n, this::EA_16_42_n, this::EA_16_43_n,
                this::EA_16_44_n, this::EA_16_45_n, this::EA_16_46_n, this::EA_16_47_n,
                this::EA_16_40_n, this::EA_16_41_n, this::EA_16_42_n, this::EA_16_43_n,
                this::EA_16_44_n, this::EA_16_45_n, this::EA_16_46_n, this::EA_16_47_n,
                this::EA_16_40_n, this::EA_16_41_n, this::EA_16_42_n, this::EA_16_43_n,
                this::EA_16_44_n, this::EA_16_45_n, this::EA_16_46_n, this::EA_16_47_n,
                this::EA_16_40_n, this::EA_16_41_n, this::EA_16_42_n, this::EA_16_43_n,
                this::EA_16_44_n, this::EA_16_45_n, this::EA_16_46_n, this::EA_16_47_n,
                this::EA_16_40_n, this::EA_16_41_n, this::EA_16_42_n, this::EA_16_43_n,
                this::EA_16_44_n, this::EA_16_45_n, this::EA_16_46_n, this::EA_16_47_n,
                /* 10 */
                this::EA_16_80_n, this::EA_16_81_n, this::EA_16_82_n, this::EA_16_83_n,
                this::EA_16_84_n, this::EA_16_85_n, this::EA_16_86_n, this::EA_16_87_n,
                this::EA_16_80_n, this::EA_16_81_n, this::EA_16_82_n, this::EA_16_83_n,
                this::EA_16_84_n, this::EA_16_85_n, this::EA_16_86_n, this::EA_16_87_n,
                this::EA_16_80_n, this::EA_16_81_n, this::EA_16_82_n, this::EA_16_83_n,
                this::EA_16_84_n, this::EA_16_85_n, this::EA_16_86_n, this::EA_16_87_n,
                this::EA_16_80_n, this::EA_16_81_n, this::EA_16_82_n, this::EA_16_83_n,
                this::EA_16_84_n, this::EA_16_85_n, this::EA_16_86_n, this::EA_16_87_n,
                this::EA_16_80_n, this::EA_16_81_n, this::EA_16_82_n, this::EA_16_83_n,
                this::EA_16_84_n, this::EA_16_85_n, this::EA_16_86_n, this::EA_16_87_n,
                this::EA_16_80_n, this::EA_16_81_n, this::EA_16_82_n, this::EA_16_83_n,
                this::EA_16_84_n, this::EA_16_85_n, this::EA_16_86_n, this::EA_16_87_n,
                this::EA_16_80_n, this::EA_16_81_n, this::EA_16_82_n, this::EA_16_83_n,
                this::EA_16_84_n, this::EA_16_85_n, this::EA_16_86_n, this::EA_16_87_n,
                this::EA_16_80_n, this::EA_16_81_n, this::EA_16_82_n, this::EA_16_83_n,
                this::EA_16_84_n, this::EA_16_85_n, this::EA_16_86_n, this::EA_16_87_n,
                /* 11 These are illegal so make em 0 */
                null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null,};
        EATable[1] = new EAHandler[] {// 256
                /* 00 */
                this::EA_32_00_n, this::EA_32_01_n, this::EA_32_02_n, this::EA_32_03_n,
                this::EA_32_04_n, this::EA_32_05_n, this::EA_32_06_n, this::EA_32_07_n,
                this::EA_32_00_n, this::EA_32_01_n, this::EA_32_02_n, this::EA_32_03_n,
                this::EA_32_04_n, this::EA_32_05_n, this::EA_32_06_n, this::EA_32_07_n,
                this::EA_32_00_n, this::EA_32_01_n, this::EA_32_02_n, this::EA_32_03_n,
                this::EA_32_04_n, this::EA_32_05_n, this::EA_32_06_n, this::EA_32_07_n,
                this::EA_32_00_n, this::EA_32_01_n, this::EA_32_02_n, this::EA_32_03_n,
                this::EA_32_04_n, this::EA_32_05_n, this::EA_32_06_n, this::EA_32_07_n,
                this::EA_32_00_n, this::EA_32_01_n, this::EA_32_02_n, this::EA_32_03_n,
                this::EA_32_04_n, this::EA_32_05_n, this::EA_32_06_n, this::EA_32_07_n,
                this::EA_32_00_n, this::EA_32_01_n, this::EA_32_02_n, this::EA_32_03_n,
                this::EA_32_04_n, this::EA_32_05_n, this::EA_32_06_n, this::EA_32_07_n,
                this::EA_32_00_n, this::EA_32_01_n, this::EA_32_02_n, this::EA_32_03_n,
                this::EA_32_04_n, this::EA_32_05_n, this::EA_32_06_n, this::EA_32_07_n,
                this::EA_32_00_n, this::EA_32_01_n, this::EA_32_02_n, this::EA_32_03_n,
                this::EA_32_04_n, this::EA_32_05_n, this::EA_32_06_n, this::EA_32_07_n,
                /* 01 */
                this::EA_32_40_n, this::EA_32_41_n, this::EA_32_42_n, this::EA_32_43_n,
                this::EA_32_44_n, this::EA_32_45_n, this::EA_32_46_n, this::EA_32_47_n,
                this::EA_32_40_n, this::EA_32_41_n, this::EA_32_42_n, this::EA_32_43_n,
                this::EA_32_44_n, this::EA_32_45_n, this::EA_32_46_n, this::EA_32_47_n,
                this::EA_32_40_n, this::EA_32_41_n, this::EA_32_42_n, this::EA_32_43_n,
                this::EA_32_44_n, this::EA_32_45_n, this::EA_32_46_n, this::EA_32_47_n,
                this::EA_32_40_n, this::EA_32_41_n, this::EA_32_42_n, this::EA_32_43_n,
                this::EA_32_44_n, this::EA_32_45_n, this::EA_32_46_n, this::EA_32_47_n,
                this::EA_32_40_n, this::EA_32_41_n, this::EA_32_42_n, this::EA_32_43_n,
                this::EA_32_44_n, this::EA_32_45_n, this::EA_32_46_n, this::EA_32_47_n,
                this::EA_32_40_n, this::EA_32_41_n, this::EA_32_42_n, this::EA_32_43_n,
                this::EA_32_44_n, this::EA_32_45_n, this::EA_32_46_n, this::EA_32_47_n,
                this::EA_32_40_n, this::EA_32_41_n, this::EA_32_42_n, this::EA_32_43_n,
                this::EA_32_44_n, this::EA_32_45_n, this::EA_32_46_n, this::EA_32_47_n,
                this::EA_32_40_n, this::EA_32_41_n, this::EA_32_42_n, this::EA_32_43_n,
                this::EA_32_44_n, this::EA_32_45_n, this::EA_32_46_n, this::EA_32_47_n,
                /* 10 */
                this::EA_32_80_n, this::EA_32_81_n, this::EA_32_82_n, this::EA_32_83_n,
                this::EA_32_84_n, this::EA_32_85_n, this::EA_32_86_n, this::EA_32_87_n,
                this::EA_32_80_n, this::EA_32_81_n, this::EA_32_82_n, this::EA_32_83_n,
                this::EA_32_84_n, this::EA_32_85_n, this::EA_32_86_n, this::EA_32_87_n,
                this::EA_32_80_n, this::EA_32_81_n, this::EA_32_82_n, this::EA_32_83_n,
                this::EA_32_84_n, this::EA_32_85_n, this::EA_32_86_n, this::EA_32_87_n,
                this::EA_32_80_n, this::EA_32_81_n, this::EA_32_82_n, this::EA_32_83_n,
                this::EA_32_84_n, this::EA_32_85_n, this::EA_32_86_n, this::EA_32_87_n,
                this::EA_32_80_n, this::EA_32_81_n, this::EA_32_82_n, this::EA_32_83_n,
                this::EA_32_84_n, this::EA_32_85_n, this::EA_32_86_n, this::EA_32_87_n,
                this::EA_32_80_n, this::EA_32_81_n, this::EA_32_82_n, this::EA_32_83_n,
                this::EA_32_84_n, this::EA_32_85_n, this::EA_32_86_n, this::EA_32_87_n,
                this::EA_32_80_n, this::EA_32_81_n, this::EA_32_82_n, this::EA_32_83_n,
                this::EA_32_84_n, this::EA_32_85_n, this::EA_32_86_n, this::EA_32_87_n,
                this::EA_32_80_n, this::EA_32_81_n, this::EA_32_82_n, this::EA_32_83_n,
                this::EA_32_84_n, this::EA_32_85_n, this::EA_32_86_n, this::EA_32_87_n,
                /* 11 These are illegal so make em 0 */
                null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null};
    }

    /*--------------------------- end CpuCoreTable_EA -----------------------------*/
    /*--------------------------- begin Helper -----------------------------*/
    protected void RMEbGb(DOSActionIntInt actRBL, DOSActionIntInt actRBH, DOSActionIntInt actMB) {
        int rm = fetchB();
        int rmrb = lookupRMregbl[rm] >= 0 ? Register.Regs[lookupRMregbl[rm]].getByteL()
                : Register.Regs[lookupRMregbh[rm]].getByteH();
        if (rm >= 0xc0) {
            int regId = lookupRMEAregbl[rm];
            if (regId >= 0)
                actRBL.run(regId, rmrb);
            else {
                regId = lookupRMEAregbh[rm];
                actRBH.run(regId, rmrb);
            }
        } else {
            int eaa = Core.EATable[rm].get();
            actMB.run(eaa, rmrb);
        }
    }

    protected void RMEwGw(DOSActionIntInt actRW, DOSActionIntInt actMW) {
        int rm = fetchB();
        int rmrw = Register.Regs[lookupRMregw[rm]].getWord();
        if (rm >= 0xc0) {
            int regId = lookupRMEAregw[rm];
            actRW.run(regId, rmrw);
        } else {
            int eaa = Core.EATable[rm].get();
            actMW.run(eaa, rmrw);
        }
    }

    // Func(int, byte)
    protected void RMGbEb(DOSActionIntInt actRBL, DOSActionIntInt actRBH) {
        int rm = fetchB();
        int regId = lookupRMregbl[rm];
        int rb;
        if (rm >= 0xc0) {
            int earId = lookupRMEAregbl[rm];
            rb = earId >= 0 ? Register.Regs[earId].getByteL()
                    : Register.Regs[lookupRMEAregbh[rm]].getByteH();
        } else {
            rb = Memory.readB(Core.EATable[rm].get());
        }
        if (regId >= 0)
            actRBL.run(regId, rb);
        else {
            regId = lookupRMregbh[rm];
            actRBH.run(regId, rb);
        }
    }

    // protected void RMEwGwOp3(Func<int, short, byte, boolean> actLW, Func<int, short, byte,
    // boolean> actMW, byte op3)
    // {
    // int rm = Fetchb(); short rmrw = regsModule.regs[lookupRMregw[rm]].getWord();
    // if (rm >= 0xc0) { actLW(lookupRMEAregw[rm], rmrw, op3); }
    // else { actMW(core.ea_table[rm](), rmrw, op3); }
    // }

    protected void RMGwEw(DOSActionIntInt actRW) {// RMGwEw(ADDW)
        int rm = fetchB();
        int regId = lookupRMregw[rm];
        if (rm >= 0xc0) {
            actRW.run(regId, Register.Regs[lookupRMEAregw[rm]].getWord());
        } else {
            actRW.run(regId, Memory.readW(Core.EATable[rm].get()));
        }
    }
    // 함수에 직접 삽입
    // protected void RMGwEwOp3( DosActionIntShortShort actRW, short op3)
    // {
    // byte rm = Fetchb();
    // int regId = lookupRMregw[rm];
    // if (rm >= 0xc0) { actRW(regId, (short)regsModule.regs[lookupRMEAregw[rm]].getWord(), op3); }
    // else { actRW(regId, (Int16)MEMORY.mem_readw(core.ea_table[rm]()), op3); }

    // }
    // prefix_of.h의 RMGwEwOp3(DIMULW,*rmrw)에서만
    protected void RMGwEwOp3RMrw(DOSActionIntIntInt actRW) {
        int rm = fetchB();
        int regId = lookupRMregw[rm];
        if (rm >= 0xc0) {
            actRW.run(regId, Register.Regs[lookupRMEAregw[rm]].getWord(),
                    Register.Regs[regId].getWord());
        } else {
            actRW.run(regId, Memory.readW(Core.EATable[rm].get()), Register.Regs[regId].getWord());
        }

    }

    protected void RMEdGd(DOSActionIntInt actRD, DOSActionIntInt actMD) {
        int rm = fetchB();
        int rmrd = Register.Regs[lookupRMregd[rm]].getDWord();
        if (rm >= 0xc0) {
            actRD.run(lookupRMEAregd[rm], rmrd);
        } else {
            actMD.run(Core.EATable[rm].get(), rmrd);
        }
    }

    // protected void RMEdGdOp3(Func<int, int, byte, boolean> actRD, Func<int, int, byte, boolean>
    // actMD, byte op3)
    // {
    // byte rm = Fetchb(); int rmrd = regsModule.regs[lookupRMregd[rm]].getDWord();
    // if (rm >= 0xc0) { actRD(lookupRMEAregd[rm], rmrd, op3); }
    // else { actMD(core.ea_table[rm](), rmrd, op3); }
    // }

    protected void RMGdEd(DOSActionIntInt actRD) {
        int rm = fetchB();
        int rmrdId = lookupRMregd[rm];
        if (rm >= 0xc0) {
            actRD.run(rmrdId, Register.Regs[lookupRMEAregd[rm]].getDWord());
        } else {
            actRD.run(rmrdId, Memory.readD(Core.EATable[rm].get()));
        }
    }
    // 함수에 직접 삽입
    // protected void RMGdEdOp3(Action<int, int, int> actRD, int op3)
    // {
    // byte rm = Fetchb(); int rmrdId = lookupRMregd[rm];
    // if (rm >= 0xc0) { actRD(rmrdId, regsModule.regs[lookupRMEAregd[rm]].getDWord(), op3); }
    // else { int eaa = core.ea_table[rm](); actRD(rmrdId, MEMORY.mem_readd(eaa), op3); }
    // }

    /*--------------------------- end Helper -----------------------------*/
    /*--------------------------- begin Modrm -----------------------------*/
    protected static boolean[] lookupRMregbSelector = {true, true, true, true, true, true, true,
            true, true, true, true, true, true, true, true, true, true, true, true, true, true,
            true, true, true, true, true, true, true, true, true, true, true, false, false, false,
            false, false, false, false, false, false, false, false, false, false, false, false,
            false, false, false, false, false, false, false, false, false, false, false, false,
            false, false, false, false, false,

            true, true, true, true, true, true, true, true, true, true, true, true, true, true,
            true, true, true, true, true, true, true, true, true, true, true, true, true, true,
            true, true, true, true, false, false, false, false, false, false, false, false, false,
            false, false, false, false, false, false, false, false, false, false, false, false,
            false, false, false, false, false, false, false, false, false, false, false,

            true, true, true, true, true, true, true, true, true, true, true, true, true, true,
            true, true, true, true, true, true, true, true, true, true, true, true, true, true,
            true, true, true, true, false, false, false, false, false, false, false, false, false,
            false, false, false, false, false, false, false, false, false, false, false, false,
            false, false, false, false, false, false, false, false, false, false, false,

            true, true, true, true, true, true, true, true, true, true, true, true, true, true,
            true, true, true, true, true, true, true, true, true, true, true, true, true, true,
            true, true, true, true, false, false, false, false, false, false, false, false, false,
            false, false, false, false, false, false, false, false, false, false, false, false,
            false, false, false, false, false, false, false, false, false, false, false};

    protected static final int[] lookupRMregbl =
            {Register.AX, Register.AX, Register.AX, Register.AX, Register.AX, Register.AX,
                    Register.AX, Register.AX, Register.CX, Register.CX, Register.CX, Register.CX,
                    Register.CX, Register.CX, Register.CX, Register.CX, Register.DX, Register.DX,
                    Register.DX, Register.DX, Register.DX, Register.DX, Register.DX, Register.DX,
                    Register.BX, Register.BX, Register.BX, Register.BX, Register.BX, Register.BX,
                    Register.BX, Register.BX, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
                    -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,

                    Register.AX, Register.AX, Register.AX, Register.AX, Register.AX, Register.AX,
                    Register.AX, Register.AX, Register.CX, Register.CX, Register.CX, Register.CX,
                    Register.CX, Register.CX, Register.CX, Register.CX, Register.DX, Register.DX,
                    Register.DX, Register.DX, Register.DX, Register.DX, Register.DX, Register.DX,
                    Register.BX, Register.BX, Register.BX, Register.BX, Register.BX, Register.BX,
                    Register.BX, Register.BX, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
                    -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,

                    Register.AX, Register.AX, Register.AX, Register.AX, Register.AX, Register.AX,
                    Register.AX, Register.AX, Register.CX, Register.CX, Register.CX, Register.CX,
                    Register.CX, Register.CX, Register.CX, Register.CX, Register.DX, Register.DX,
                    Register.DX, Register.DX, Register.DX, Register.DX, Register.DX, Register.DX,
                    Register.BX, Register.BX, Register.BX, Register.BX, Register.BX, Register.BX,
                    Register.BX, Register.BX, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
                    -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,

                    Register.AX, Register.AX, Register.AX, Register.AX, Register.AX, Register.AX,
                    Register.AX, Register.AX, Register.CX, Register.CX, Register.CX, Register.CX,
                    Register.CX, Register.CX, Register.CX, Register.CX, Register.DX, Register.DX,
                    Register.DX, Register.DX, Register.DX, Register.DX, Register.DX, Register.DX,
                    Register.BX, Register.BX, Register.BX, Register.BX, Register.BX, Register.BX,
                    Register.BX, Register.BX, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
                    -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1};
    protected static final int[] lookupRMregbh =
            {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
                    -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, Register.AX, Register.AX, Register.AX,
                    Register.AX, Register.AX, Register.AX, Register.AX, Register.AX, Register.CX,
                    Register.CX, Register.CX, Register.CX, Register.CX, Register.CX, Register.CX,
                    Register.CX, Register.DX, Register.DX, Register.DX, Register.DX, Register.DX,
                    Register.DX, Register.DX, Register.DX, Register.BX, Register.BX, Register.BX,
                    Register.BX, Register.BX, Register.BX, Register.BX, Register.BX,

                    -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
                    -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, Register.AX, Register.AX,
                    Register.AX, Register.AX, Register.AX, Register.AX, Register.AX, Register.AX,
                    Register.CX, Register.CX, Register.CX, Register.CX, Register.CX, Register.CX,
                    Register.CX, Register.CX, Register.DX, Register.DX, Register.DX, Register.DX,
                    Register.DX, Register.DX, Register.DX, Register.DX, Register.BX, Register.BX,
                    Register.BX, Register.BX, Register.BX, Register.BX, Register.BX, Register.BX,

                    -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
                    -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, Register.AX, Register.AX,
                    Register.AX, Register.AX, Register.AX, Register.AX, Register.AX, Register.AX,
                    Register.CX, Register.CX, Register.CX, Register.CX, Register.CX, Register.CX,
                    Register.CX, Register.CX, Register.DX, Register.DX, Register.DX, Register.DX,
                    Register.DX, Register.DX, Register.DX, Register.DX, Register.BX, Register.BX,
                    Register.BX, Register.BX, Register.BX, Register.BX, Register.BX, Register.BX,

                    -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
                    -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, Register.AX, Register.AX,
                    Register.AX, Register.AX, Register.AX, Register.AX, Register.AX, Register.AX,
                    Register.CX, Register.CX, Register.CX, Register.CX, Register.CX, Register.CX,
                    Register.CX, Register.CX, Register.DX, Register.DX, Register.DX, Register.DX,
                    Register.DX, Register.DX, Register.DX, Register.DX, Register.BX, Register.BX,
                    Register.BX, Register.BX, Register.BX, Register.BX, Register.BX, Register.BX};

    protected static final int[] lookupRMregw = {Register.AX, Register.AX, Register.AX, Register.AX,
            Register.AX, Register.AX, Register.AX, Register.AX, Register.CX, Register.CX,
            Register.CX, Register.CX, Register.CX, Register.CX, Register.CX, Register.CX,
            Register.DX, Register.DX, Register.DX, Register.DX, Register.DX, Register.DX,
            Register.DX, Register.DX, Register.BX, Register.BX, Register.BX, Register.BX,
            Register.BX, Register.BX, Register.BX, Register.BX, Register.SP, Register.SP,
            Register.SP, Register.SP, Register.SP, Register.SP, Register.SP, Register.SP,
            Register.BP, Register.BP, Register.BP, Register.BP, Register.BP, Register.BP,
            Register.BP, Register.BP, Register.SI, Register.SI, Register.SI, Register.SI,
            Register.SI, Register.SI, Register.SI, Register.SI, Register.DI, Register.DI,
            Register.DI, Register.DI, Register.DI, Register.DI, Register.DI, Register.DI,

            Register.AX, Register.AX, Register.AX, Register.AX, Register.AX, Register.AX,
            Register.AX, Register.AX, Register.CX, Register.CX, Register.CX, Register.CX,
            Register.CX, Register.CX, Register.CX, Register.CX, Register.DX, Register.DX,
            Register.DX, Register.DX, Register.DX, Register.DX, Register.DX, Register.DX,
            Register.BX, Register.BX, Register.BX, Register.BX, Register.BX, Register.BX,
            Register.BX, Register.BX, Register.SP, Register.SP, Register.SP, Register.SP,
            Register.SP, Register.SP, Register.SP, Register.SP, Register.BP, Register.BP,
            Register.BP, Register.BP, Register.BP, Register.BP, Register.BP, Register.BP,
            Register.SI, Register.SI, Register.SI, Register.SI, Register.SI, Register.SI,
            Register.SI, Register.SI, Register.DI, Register.DI, Register.DI, Register.DI,
            Register.DI, Register.DI, Register.DI, Register.DI,

            Register.AX, Register.AX, Register.AX, Register.AX, Register.AX, Register.AX,
            Register.AX, Register.AX, Register.CX, Register.CX, Register.CX, Register.CX,
            Register.CX, Register.CX, Register.CX, Register.CX, Register.DX, Register.DX,
            Register.DX, Register.DX, Register.DX, Register.DX, Register.DX, Register.DX,
            Register.BX, Register.BX, Register.BX, Register.BX, Register.BX, Register.BX,
            Register.BX, Register.BX, Register.SP, Register.SP, Register.SP, Register.SP,
            Register.SP, Register.SP, Register.SP, Register.SP, Register.BP, Register.BP,
            Register.BP, Register.BP, Register.BP, Register.BP, Register.BP, Register.BP,
            Register.SI, Register.SI, Register.SI, Register.SI, Register.SI, Register.SI,
            Register.SI, Register.SI, Register.DI, Register.DI, Register.DI, Register.DI,
            Register.DI, Register.DI, Register.DI, Register.DI,

            Register.AX, Register.AX, Register.AX, Register.AX, Register.AX, Register.AX,
            Register.AX, Register.AX, Register.CX, Register.CX, Register.CX, Register.CX,
            Register.CX, Register.CX, Register.CX, Register.CX, Register.DX, Register.DX,
            Register.DX, Register.DX, Register.DX, Register.DX, Register.DX, Register.DX,
            Register.BX, Register.BX, Register.BX, Register.BX, Register.BX, Register.BX,
            Register.BX, Register.BX, Register.SP, Register.SP, Register.SP, Register.SP,
            Register.SP, Register.SP, Register.SP, Register.SP, Register.BP, Register.BP,
            Register.BP, Register.BP, Register.BP, Register.BP, Register.BP, Register.BP,
            Register.SI, Register.SI, Register.SI, Register.SI, Register.SI, Register.SI,
            Register.SI, Register.SI, Register.DI, Register.DI, Register.DI, Register.DI,
            Register.DI, Register.DI, Register.DI, Register.DI};

    protected static final int[] lookupRMregd = {Register.AX, Register.AX, Register.AX, Register.AX,
            Register.AX, Register.AX, Register.AX, Register.AX, Register.CX, Register.CX,
            Register.CX, Register.CX, Register.CX, Register.CX, Register.CX, Register.CX,
            Register.DX, Register.DX, Register.DX, Register.DX, Register.DX, Register.DX,
            Register.DX, Register.DX, Register.BX, Register.BX, Register.BX, Register.BX,
            Register.BX, Register.BX, Register.BX, Register.BX, Register.SP, Register.SP,
            Register.SP, Register.SP, Register.SP, Register.SP, Register.SP, Register.SP,
            Register.BP, Register.BP, Register.BP, Register.BP, Register.BP, Register.BP,
            Register.BP, Register.BP, Register.SI, Register.SI, Register.SI, Register.SI,
            Register.SI, Register.SI, Register.SI, Register.SI, Register.DI, Register.DI,
            Register.DI, Register.DI, Register.DI, Register.DI, Register.DI, Register.DI,

            Register.AX, Register.AX, Register.AX, Register.AX, Register.AX, Register.AX,
            Register.AX, Register.AX, Register.CX, Register.CX, Register.CX, Register.CX,
            Register.CX, Register.CX, Register.CX, Register.CX, Register.DX, Register.DX,
            Register.DX, Register.DX, Register.DX, Register.DX, Register.DX, Register.DX,
            Register.BX, Register.BX, Register.BX, Register.BX, Register.BX, Register.BX,
            Register.BX, Register.BX, Register.SP, Register.SP, Register.SP, Register.SP,
            Register.SP, Register.SP, Register.SP, Register.SP, Register.BP, Register.BP,
            Register.BP, Register.BP, Register.BP, Register.BP, Register.BP, Register.BP,
            Register.SI, Register.SI, Register.SI, Register.SI, Register.SI, Register.SI,
            Register.SI, Register.SI, Register.DI, Register.DI, Register.DI, Register.DI,
            Register.DI, Register.DI, Register.DI, Register.DI,

            Register.AX, Register.AX, Register.AX, Register.AX, Register.AX, Register.AX,
            Register.AX, Register.AX, Register.CX, Register.CX, Register.CX, Register.CX,
            Register.CX, Register.CX, Register.CX, Register.CX, Register.DX, Register.DX,
            Register.DX, Register.DX, Register.DX, Register.DX, Register.DX, Register.DX,
            Register.BX, Register.BX, Register.BX, Register.BX, Register.BX, Register.BX,
            Register.BX, Register.BX, Register.SP, Register.SP, Register.SP, Register.SP,
            Register.SP, Register.SP, Register.SP, Register.SP, Register.BP, Register.BP,
            Register.BP, Register.BP, Register.BP, Register.BP, Register.BP, Register.BP,
            Register.SI, Register.SI, Register.SI, Register.SI, Register.SI, Register.SI,
            Register.SI, Register.SI, Register.DI, Register.DI, Register.DI, Register.DI,
            Register.DI, Register.DI, Register.DI, Register.DI,

            Register.AX, Register.AX, Register.AX, Register.AX, Register.AX, Register.AX,
            Register.AX, Register.AX, Register.CX, Register.CX, Register.CX, Register.CX,
            Register.CX, Register.CX, Register.CX, Register.CX, Register.DX, Register.DX,
            Register.DX, Register.DX, Register.DX, Register.DX, Register.DX, Register.DX,
            Register.BX, Register.BX, Register.BX, Register.BX, Register.BX, Register.BX,
            Register.BX, Register.BX, Register.SP, Register.SP, Register.SP, Register.SP,
            Register.SP, Register.SP, Register.SP, Register.SP, Register.BP, Register.BP,
            Register.BP, Register.BP, Register.BP, Register.BP, Register.BP, Register.BP,
            Register.SI, Register.SI, Register.SI, Register.SI, Register.SI, Register.SI,
            Register.SI, Register.SI, Register.DI, Register.DI, Register.DI, Register.DI,
            Register.DI, Register.DI, Register.DI, Register.DI};

    protected static final int[] lookupRMEAregbl = {
            /* 12 lines of 16*0 should give nice errors when used */
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, Register.AX,
            Register.CX, Register.DX, Register.BX, -1, -1, -1, -1, Register.AX, Register.CX,
            Register.DX, Register.BX, -1, -1, -1, -1, Register.AX, Register.CX, Register.DX,
            Register.BX, -1, -1, -1, -1, Register.AX, Register.CX, Register.DX, Register.BX, -1, -1,
            -1, -1, Register.AX, Register.CX, Register.DX, Register.BX, -1, -1, -1, -1, Register.AX,
            Register.CX, Register.DX, Register.BX, -1, -1, -1, -1, Register.AX, Register.CX,
            Register.DX, Register.BX, -1, -1, -1, -1, Register.AX, Register.CX, Register.DX,
            Register.BX, -1, -1, -1, -1};

    protected static final int[] lookupRMEAregbh = {
            /* 12 lines of 16*0 should give nice errors when used */
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            Register.AX, Register.CX, Register.DX, Register.BX, -1, -1, -1, -1, Register.AX,
            Register.CX, Register.DX, Register.BX, -1, -1, -1, -1, Register.AX, Register.CX,
            Register.DX, Register.BX, -1, -1, -1, -1, Register.AX, Register.CX, Register.DX,
            Register.BX, -1, -1, -1, -1, Register.AX, Register.CX, Register.DX, Register.BX, -1, -1,
            -1, -1, Register.AX, Register.CX, Register.DX, Register.BX, -1, -1, -1, -1, Register.AX,
            Register.CX, Register.DX, Register.BX, -1, -1, -1, -1, Register.AX, Register.CX,
            Register.DX, Register.BX};

    protected static final int[] lookupRMEAregw = {
            /* 12 lines of 16*0 should give nice errors when used */
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, Register.AX,
            Register.CX, Register.DX, Register.BX, Register.SP, Register.BP, Register.SI,
            Register.DI, Register.AX, Register.CX, Register.DX, Register.BX, Register.SP,
            Register.BP, Register.SI, Register.DI, Register.AX, Register.CX, Register.DX,
            Register.BX, Register.SP, Register.BP, Register.SI, Register.DI, Register.AX,
            Register.CX, Register.DX, Register.BX, Register.SP, Register.BP, Register.SI,
            Register.DI, Register.AX, Register.CX, Register.DX, Register.BX, Register.SP,
            Register.BP, Register.SI, Register.DI, Register.AX, Register.CX, Register.DX,
            Register.BX, Register.SP, Register.BP, Register.SI, Register.DI, Register.AX,
            Register.CX, Register.DX, Register.BX, Register.SP, Register.BP, Register.SI,
            Register.DI, Register.AX, Register.CX, Register.DX, Register.BX, Register.SP,
            Register.BP, Register.SI, Register.DI};
    protected static final int[] lookupRMEAregd = {
            /* 12 lines of 16*0 should give nice errors when used */
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, Register.AX,
            Register.CX, Register.DX, Register.BX, Register.SP, Register.BP, Register.SI,
            Register.DI, Register.AX, Register.CX, Register.DX, Register.BX, Register.SP,
            Register.BP, Register.SI, Register.DI, Register.AX, Register.CX, Register.DX,
            Register.BX, Register.SP, Register.BP, Register.SI, Register.DI, Register.AX,
            Register.CX, Register.DX, Register.BX, Register.SP, Register.BP, Register.SI,
            Register.DI, Register.AX, Register.CX, Register.DX, Register.BX, Register.SP,
            Register.BP, Register.SI, Register.DI, Register.AX, Register.CX, Register.DX,
            Register.BX, Register.SP, Register.BP, Register.SI, Register.DI, Register.AX,
            Register.CX, Register.DX, Register.BX, Register.SP, Register.BP, Register.SI,
            Register.DI, Register.AX, Register.CX, Register.DX, Register.BX, Register.SP,
            Register.BP, Register.SI, Register.DI};
    /*--------------------------- end Modrm -----------------------------*/
    /*--------------------------- begin String -----------------------------*/

    private static final int STRING_OP_OUTSB = 0;
    private static final int STRING_OP_OUTSW = 1;
    private static final int STRING_OP_OUTSD = 2;

    private static final int STRING_OP_INSB = 3;
    private static final int STRING_OP_INSW = 4;
    private static final int STRING_OP_INSD = 5;

    private static final int STRING_OP_MOVSB = 6;
    private static final int STRING_OP_MOVSW = 7;
    private static final int STRING_OP_MOVSD = 8;

    private static final int STRING_OP_LODSB = 9;
    private static final int STRING_OP_LODSW = 10;
    private static final int STRING_OP_LODSD = 11;

    private static final int STRING_OP_STOSB = 12;
    private static final int STRING_OP_STOSW = 13;
    private static final int STRING_OP_STOSD = 14;

    private static final int STRING_OP_SCASB = 15;
    private static final int STRING_OP_SCASW = 16;
    private static final int STRING_OP_SCASD = 17;

    private static final int STRING_OP_CMPSB = 18;
    private static final int STRING_OP_CMPSW = 19;
    private static final int STRING_OP_CMPSD = 20;

    protected void DoString(int type) {
        // Console.WriteLine(cpuModule.CPU_Cycles);

        int siBase, diBase;
        int siIndex, diIndex;
        int addMask;
        int count, countLeft = 0;
        int addIndex;

        siBase = Core.BaseDS;
        diBase = Register.segPhys(Register.SEG_NAME_ES);
        addMask = AddrMaskTable[Core.Prefixes & PrefixAddr];
        siIndex = Register.getRegESI() & addMask;
        diIndex = Register.getRegEDI() & addMask;
        count = Register.getRegECX() & addMask;
        if ((Core.Prefixes & PrefixRep) == 0) {
            count = 1;
        } else {
            CPU.Cycles++;
            /* Calculate amount of ops to do before cycles run out */
            if ((count > CPU.Cycles) && (type < STRING_OP_SCASB)) {
                countLeft = count - CPU.Cycles;
                count = CPU.Cycles;
                CPU.Cycles = 0;
                loadIP();// core.cseip = (regsModule.SegPhys(regsModule.SEG_NAME_cs) +
                         // regsModule.reg_eip); //RESET IP to the start
            } else {
                /* Won't interrupt scas and cmps instruction since they can interrupt themselves */
                if ((count <= 1) && (CPU.Cycles <= 1))
                    CPU.Cycles--;
                else if (type < STRING_OP_SCASB)
                    CPU.Cycles -= count;
                countLeft = 0;
            }
        }
        addIndex = CPU.Block.Direction;
        if (count != 0)
            switch (type) {
                case STRING_OP_OUTSB:
                    for (; count > 0; count--) {
                        IO.writeB(Register.getRegDX(), Memory.readB(siBase + siIndex));
                        siIndex = (siIndex + addIndex) & addMask;
                    }
                    break;
                case STRING_OP_OUTSW:
                    addIndex <<= 1;
                    for (; count > 0; count--) {
                        IO.writeW(Register.getRegDX(), Memory.readW(siBase + siIndex));
                        siIndex = (siIndex + addIndex) & addMask;
                    }
                    break;
                case STRING_OP_OUTSD:
                    addIndex <<= 2;
                    for (; count > 0; count--) {
                        IO.writeD(Register.getRegDX(), Memory.readD(siBase + siIndex));
                        siIndex = (siIndex + addIndex) & addMask;
                    }
                    break;
                case STRING_OP_INSB:
                    for (; count > 0; count--) {
                        Memory.writeB(diBase + diIndex, IO.readB(Register.getRegDX()));
                        diIndex = (diIndex + addIndex) & addMask;
                    }
                    break;
                case STRING_OP_INSW:
                    addIndex <<= 1;
                    for (; count > 0; count--) {
                        Memory.writeW(diBase + diIndex, IO.readW(Register.getRegDX()));
                        diIndex = (diIndex + addIndex) & addMask;
                    }
                    break;
                case STRING_OP_STOSB:
                    for (; count > 0; count--) {
                        Memory.writeB(diBase + diIndex, Register.getRegAL());
                        diIndex = (diIndex + addIndex) & addMask;
                    }
                    break;
                case STRING_OP_STOSW:
                    addIndex <<= 1;
                    for (; count > 0; count--) {
                        Memory.writeW(diBase + diIndex, Register.getRegAX());
                        diIndex = (diIndex + addIndex) & addMask;
                    }
                    break;
                case STRING_OP_STOSD:
                    addIndex <<= 2;
                    for (; count > 0; count--) {
                        Memory.writeD(diBase + diIndex, Register.getRegEAX());
                        diIndex = (diIndex + addIndex) & addMask;
                    }
                    break;
                case STRING_OP_MOVSB:
                    for (; count > 0; count--) {
                        Memory.writeB(diBase + diIndex, Memory.readB(siBase + siIndex));
                        diIndex = (diIndex + addIndex) & addMask;
                        siIndex = (siIndex + addIndex) & addMask;
                    }
                    break;
                case STRING_OP_MOVSW:
                    addIndex <<= 1;
                    for (; count > 0; count--) {
                        Memory.writeW(diBase + diIndex, Memory.readW(siBase + siIndex));
                        diIndex = (diIndex + addIndex) & addMask;
                        siIndex = (siIndex + addIndex) & addMask;
                    }
                    break;
                case STRING_OP_MOVSD:
                    addIndex <<= 2;
                    for (; count > 0; count--) {
                        Memory.writeD(diBase + diIndex, Memory.readD(siBase + siIndex));
                        diIndex = (diIndex + addIndex) & addMask;
                        siIndex = (siIndex + addIndex) & addMask;
                    }
                    break;
                case STRING_OP_LODSB:
                    for (; count > 0; count--) {
                        Register.setRegAL(Memory.readB(siBase + siIndex));
                        siIndex = (siIndex + addIndex) & addMask;
                    }
                    break;
                case STRING_OP_LODSW:
                    addIndex <<= 1;
                    for (; count > 0; count--) {
                        Register.setRegAX(Memory.readW(siBase + siIndex));
                        siIndex = (siIndex + addIndex) & addMask;
                    }
                    break;
                case STRING_OP_LODSD:
                    addIndex <<= 2;
                    for (; count > 0; count--) {
                        Register.setRegEAX(Memory.readD(siBase + siIndex));
                        siIndex = (siIndex + addIndex) & addMask;
                    }
                    break;
                case STRING_OP_SCASB: {
                    int val2 = 0;
                    for (; count > 0;) {
                        count--;
                        CPU.Cycles--;
                        val2 = Memory.readB(diBase + diIndex);
                        diIndex = (diIndex + addIndex) & addMask;
                        if ((Register.getRegAL() == val2) != Core.RepZero)
                            break;
                    }
                    CMPBSTR(Register.getRegAL(), val2);
                }
                    break;
                case STRING_OP_SCASW: {
                    addIndex <<= 1;
                    int val2 = 0;
                    for (; count > 0;) {
                        count--;
                        CPU.Cycles--;
                        val2 = Memory.readW(diBase + diIndex);
                        diIndex = (diIndex + addIndex) & addMask;
                        if ((Register.getRegAX() == val2) != Core.RepZero)
                            break;
                    }
                    CMPWSTR(Register.getRegAX(), val2);
                }
                    break;
                case STRING_OP_SCASD: {
                    addIndex <<= 2;
                    int val2 = 0;
                    for (; count > 0;) {
                        count--;
                        CPU.Cycles--;
                        val2 = Memory.readD(diBase + diIndex);
                        diIndex = (diIndex + addIndex) & addMask;
                        if ((Register.getRegEAX() == val2) != Core.RepZero)
                            break;
                    }
                    CMPDSTR(Register.getRegEAX(), val2);
                }
                    break;
                case STRING_OP_CMPSB: {
                    int val1 = 0, val2 = 0;
                    for (; count > 0;) {
                        count--;
                        CPU.Cycles--;
                        val1 = Memory.readB(siBase + siIndex);
                        val2 = Memory.readB(diBase + diIndex);
                        siIndex = (siIndex + addIndex) & addMask;
                        diIndex = (diIndex + addIndex) & addMask;
                        if ((val1 == val2) != Core.RepZero)
                            break;
                    }
                    CMPBSTR(val1, val2);
                }
                    break;
                case STRING_OP_CMPSW: {
                    addIndex <<= 1;
                    int val1 = 0, val2 = 0;
                    for (; count > 0;) {
                        count--;
                        CPU.Cycles--;
                        val1 = Memory.readW(siBase + siIndex);
                        val2 = Memory.readW(diBase + diIndex);
                        siIndex = (siIndex + addIndex) & addMask;
                        diIndex = (diIndex + addIndex) & addMask;
                        if ((val1 == val2) != Core.RepZero)
                            break;
                    }
                    CMPWSTR(val1, val2);
                }
                    break;
                case STRING_OP_CMPSD: {
                    addIndex <<= 2;
                    int val1 = 0, val2 = 0;
                    for (; count > 0;) {
                        count--;
                        CPU.Cycles--;
                        val1 = Memory.readD(siBase + siIndex);
                        val2 = Memory.readD(diBase + diIndex);
                        siIndex = (siIndex + addIndex) & addMask;
                        diIndex = (diIndex + addIndex) & addMask;
                        if ((val1 == val2) != Core.RepZero)
                            break;
                    }
                    CMPDSTR(val1, val2);
                }
                    break;
                default:
                    Log.logging(Log.LogTypes.CPU, Log.LogServerities.Error,
                            "Unhandled string op %d", (double) type);
                    break;
            }
        /* Clean up after certain amount of instructions */
        Register.setRegESI(Register.getRegESI() & (~addMask));
        Register.setRegESI(Register.getRegESI() | (siIndex & addMask));
        Register.setRegEDI(Register.getRegEDI() & (~addMask));
        Register.setRegEDI(Register.getRegEDI() | (diIndex & addMask));

        if ((Core.Prefixes & PrefixRep) != 0) {
            count += countLeft;
            Register.setRegECX(Register.getRegECX() & (~addMask));
            Register.setRegECX(Register.getRegECX() | (count & addMask));
        }
    }

    /*--------------------------- end String -----------------------------*/
    /*--------------------------- begin Instructions -----------------------------*/
    public enum SwitchReturn {
        Continue, Break, None
    }

    /* Jumps */

    /* All Byte general instructions */

    // int op1, byte op2
    public void ADDB(int op1, int op2) {
        Flags.setLzFVar1b(Memory.readB(op1));
        Flags.setLzFVar2b(op2);
        Flags.setLzFResb(Flags.getLzFVar1b() + Flags.getLzFVar2b());
        Memory.writeB(op1, Flags.getLzFResb());
        Flags.LzFlags.Type = Flags.TypeFlag.ADDb;
    }

    // int regId, byte op2
    public void ADDBL(int regId, int op2) {
        Flags.setLzFVar1b(Register.Regs[regId].getByteL());
        Flags.setLzFVar2b(op2);
        Flags.setLzFResb(Flags.getLzFVar1b() + Flags.getLzFVar2b());
        Register.Regs[regId].setByteL(Flags.getLzFResb());

        Flags.LzFlags.Type = Flags.TypeFlag.ADDb;
    }

    // int regId, byte op2
    public void ADDBH(int regId, int op2) {
        Flags.setLzFVar1b(Register.Regs[regId].getByteH());
        Flags.setLzFVar2b(op2);
        Flags.setLzFResb(Flags.getLzFVar1b() + Flags.getLzFVar2b());
        Register.Regs[regId].setByteH(Flags.getLzFResb());
        Flags.LzFlags.Type = Flags.TypeFlag.ADDb;
    }

    // int op1, byte op2
    public void ADCB(int op1, int op2) {
        Flags.LzFlags.oldCF = Flags.getCF() ? 1 : 0;
        Flags.setLzFVar1b(Memory.readB(op1));
        Flags.setLzFVar2b(op2);
        Flags.setLzFResb(Flags.getLzFVar1b() + Flags.getLzFVar2b() + Flags.LzFlags.oldCF);
        Memory.writeB(op1, Flags.getLzFResb());
        Flags.LzFlags.Type = Flags.TypeFlag.ADCb;
    }

    // int regId, byte op2
    public void ADCBL(int regId, int op2) {
        Flags.LzFlags.oldCF = Flags.getCF() ? 1 : 0;
        Flags.setLzFVar1b(Register.Regs[regId].getByteL());
        Flags.setLzFVar2b(op2);
        Flags.setLzFResb(Flags.getLzFVar1b() + Flags.getLzFVar2b() + Flags.LzFlags.oldCF);
        Register.Regs[regId].setByteL(Flags.getLzFResb());
        Flags.LzFlags.Type = Flags.TypeFlag.ADCb;
    }

    // int regId, byte op2
    public void ADCBH(int regId, int op2) {
        Flags.LzFlags.oldCF = Flags.getCF() ? 1 : 0;
        Flags.setLzFVar1b(Register.Regs[regId].getByteH());
        Flags.setLzFVar2b(op2);
        Flags.setLzFResb(Flags.getLzFVar1b() + Flags.getLzFVar2b() + Flags.LzFlags.oldCF);
        Register.Regs[regId].setByteH(Flags.getLzFResb());
        Flags.LzFlags.Type = Flags.TypeFlag.ADCb;
    }

    // int op1, byte op2
    public void SBBB(int op1, int op2) {
        Flags.LzFlags.oldCF = Flags.getCF() ? 1 : 0;
        Flags.setLzFVar1b(Memory.readB(op1));
        Flags.setLzFVar2b(op2);
        Flags.setLzFResb(Flags.getLzFVar1b() - (Flags.getLzFVar2b() + Flags.LzFlags.oldCF));
        Memory.writeB(op1, Flags.getLzFResb());
        Flags.LzFlags.Type = Flags.TypeFlag.SBBb;

    }

    // int regId, byte op2
    public void SBBBL(int regId, int op2) {
        Flags.LzFlags.oldCF = Flags.getCF() ? 1 : 0;
        Flags.setLzFVar1b(Register.Regs[regId].getByteL());
        Flags.setLzFVar2b(op2);
        Flags.setLzFResb(Flags.getLzFVar1b() - (Flags.getLzFVar2b() + Flags.LzFlags.oldCF));
        Register.Regs[regId].setByteL(Flags.getLzFResb());
        Flags.LzFlags.Type = Flags.TypeFlag.SBBb;

    }

    // int regId, byte op2
    public void SBBBH(int regId, int op2) {
        Flags.LzFlags.oldCF = Flags.getCF() ? 1 : 0;
        Flags.setLzFVar1b(Register.Regs[regId].getByteH());
        Flags.setLzFVar2b(op2);
        Flags.setLzFResb(Flags.getLzFVar1b() - (Flags.getLzFVar2b() + Flags.LzFlags.oldCF));
        Register.Regs[regId].setByteH(Flags.getLzFResb());
        Flags.LzFlags.Type = Flags.TypeFlag.SBBb;

    }

    // int op1, byte op2
    public void SUBB(int op1, int op2) {
        Flags.setLzFVar1b(Memory.readB(op1));
        Flags.setLzFVar2b(op2);
        Flags.setLzFResb(Flags.getLzFVar1b() - Flags.getLzFVar2b());
        Memory.writeB(op1, Flags.getLzFResb());
        Flags.LzFlags.Type = Flags.TypeFlag.SUBb;
    }

    // int regId, byte op2
    public void SUBBL(int regId, int op2) {
        Flags.setLzFVar1b(Register.Regs[regId].getByteL());
        Flags.setLzFVar2b(op2);
        Flags.setLzFResb(Flags.getLzFVar1b() - Flags.getLzFVar2b());
        Register.Regs[regId].setByteL(Flags.getLzFResb());
        Flags.LzFlags.Type = Flags.TypeFlag.SUBb;
    }

    // int regId, byte op2
    public void SUBBH(int regId, int op2) {
        Flags.setLzFVar1b(Register.Regs[regId].getByteH());
        Flags.setLzFVar2b(op2);
        Flags.setLzFResb(Flags.getLzFVar1b() - Flags.getLzFVar2b());
        Register.Regs[regId].setByteH(Flags.getLzFResb());
        Flags.LzFlags.Type = Flags.TypeFlag.SUBb;
    }

    // int op1, byte op2
    public void ORB(int op1, int op2) {
        Flags.setLzFVar1b(Memory.readB(op1));
        Flags.setLzFVar2b(op2);
        Flags.setLzFResb(Flags.getLzFVar1b() | Flags.getLzFVar2b());
        Memory.writeB(op1, Flags.getLzFResb());
        Flags.LzFlags.Type = Flags.TypeFlag.ORb;

    }

    // (int, byte)
    public void ORBL(int regId, int op2) {
        Flags.setLzFVar1b(Register.Regs[regId].getByteL());
        Flags.setLzFVar2b(op2);
        Flags.setLzFResb(Flags.getLzFVar1b() | Flags.getLzFVar2b());
        Register.Regs[regId].setByteL(Flags.getLzFResb());

        Flags.LzFlags.Type = Flags.TypeFlag.ORb;

    }

    // int regId, byte op2
    public void ORBH(int regId, int op2) {
        Flags.setLzFVar1b(Register.Regs[regId].getByteH());
        Flags.setLzFVar2b(op2);
        Flags.setLzFResb(Flags.getLzFVar1b() | Flags.getLzFVar2b());
        Register.Regs[regId].setByteH(Flags.getLzFResb());
        Flags.LzFlags.Type = Flags.TypeFlag.ORb;

    }

    // int op1, byte op2
    public void XORB(int op1, int op2) {
        Flags.setLzFVar1b(Memory.readB(op1));
        Flags.setLzFVar2b(op2);
        Flags.setLzFResb(Flags.getLzFVar1b() ^ Flags.getLzFVar2b());
        Memory.writeB(op1, Flags.getLzFResb());
        Flags.LzFlags.Type = Flags.TypeFlag.XORb;

    }

    // int regId, byte op2
    public void XORBL(int regId, int op2) {
        Flags.setLzFVar1b(Register.Regs[regId].getByteL());
        Flags.setLzFVar2b(op2);
        Flags.setLzFResb(Flags.getLzFVar1b() ^ Flags.getLzFVar2b());
        Register.Regs[regId].setByteL(Flags.getLzFResb());
        Flags.LzFlags.Type = Flags.TypeFlag.XORb;
    }

    // int regId, byte op2
    public void XORBH(int regId, int op2) {
        Flags.setLzFVar1b(Register.Regs[regId].getByteH());
        Flags.setLzFVar2b(op2);
        Flags.setLzFResb(Flags.getLzFVar1b() ^ Flags.getLzFVar2b());
        Register.Regs[regId].setByteH(Flags.getLzFResb());
        Flags.LzFlags.Type = Flags.TypeFlag.XORb;
    }

    // int op1, byte op2
    public void ANDB(int op1, int op2) {
        Flags.setLzFVar1b(Memory.readB(op1));
        Flags.setLzFVar2b(op2);
        Flags.setLzFResb(Flags.getLzFVar1b() & Flags.getLzFVar2b());
        Memory.writeB(op1, Flags.getLzFResb());
        Flags.LzFlags.Type = Flags.TypeFlag.ANDb;
    }

    // int regId, byte op2
    public void ANDBL(int regId, int op2) {
        Flags.setLzFVar1b(Register.Regs[regId].getByteL());
        Flags.setLzFVar2b(op2);
        Flags.setLzFResb(Flags.getLzFVar1b() & Flags.getLzFVar2b());
        Register.Regs[regId].setByteL(Flags.getLzFResb());
        Flags.LzFlags.Type = Flags.TypeFlag.ANDb;
    }

    // int regId, byte op2
    public void ANDBH(int regId, int op2) {
        Flags.setLzFVar1b(Register.Regs[regId].getByteH());
        Flags.setLzFVar2b(op2);
        Flags.setLzFResb(Flags.getLzFVar1b() & Flags.getLzFVar2b());
        Register.Regs[regId].setByteH(Flags.getLzFResb());
        Flags.LzFlags.Type = Flags.TypeFlag.ANDb;
    }

    // int op1, byte op2
    public void CMPB(int op1, int op2) {
        Flags.setLzFVar1b(Memory.readB(op1));
        Flags.setLzFVar2b(op2);
        Flags.setLzFResb(Flags.getLzFVar1b() - Flags.getLzFVar2b());
        Flags.LzFlags.Type = Flags.TypeFlag.CMPb;
    }

    // int regId, byte op2
    public void CMPBL(int regId, int op2) {
        Flags.setLzFVar1b(Register.Regs[regId].getByteL());
        Flags.setLzFVar2b(op2);
        Flags.setLzFResb(Flags.getLzFVar1b() - Flags.getLzFVar2b());
        Flags.LzFlags.Type = Flags.TypeFlag.CMPb;
    }

    public void CMPBSTR(int op1, int op2) {
        Flags.setLzFVar1b(op1);
        Flags.setLzFVar2b(op2);
        Flags.setLzFResb(Flags.getLzFVar1b() - Flags.getLzFVar2b());
        Flags.LzFlags.Type = Flags.TypeFlag.CMPb;
    }

    // int regId, byte op2
    public void CMPBH(int regId, int op2) {
        Flags.setLzFVar1b(Register.Regs[regId].getByteH());
        Flags.setLzFVar2b(op2);
        Flags.setLzFResb(Flags.getLzFVar1b() - Flags.getLzFVar2b());
        Flags.LzFlags.Type = Flags.TypeFlag.CMPb;
    }

    // int op1, byte op2
    public void TESTB(int op1, int op2) {
        Flags.setLzFVar1b(Memory.readB(op1));
        Flags.setLzFVar2b(op2);
        Flags.setLzFResb(Flags.getLzFVar1b() & Flags.getLzFVar2b());
        Flags.LzFlags.Type = Flags.TypeFlag.TESTb;
    }

    // int regId, byte op2
    public void TESTBL(int regId, int op2) {
        Flags.setLzFVar1b(Register.Regs[regId].getByteL());
        Flags.setLzFVar2b(op2);
        Flags.setLzFResb(Flags.getLzFVar1b() & Flags.getLzFVar2b());
        Flags.LzFlags.Type = Flags.TypeFlag.TESTb;
    }

    // int regId, byte op2
    public void TESTBH(int regId, int op2) {
        Flags.setLzFVar1b(Register.Regs[regId].getByteH());
        Flags.setLzFVar2b(op2);
        Flags.setLzFResb(Flags.getLzFVar1b() & Flags.getLzFVar2b());
        Flags.LzFlags.Type = Flags.TypeFlag.TESTb;
    }
    /* All Word General instructions */


    // (int, uint16)
    public void ADDW_M(int op1, int op2) {
        op2 &= 0xffff;
        Flags.setLzFVar1w(Memory.readW(op1));
        Flags.setLzFvar2w(op2);
        Flags.setLzFresw(0xffff & (Flags.getLzFVar1w() + Flags.getLzFvar2w()));
        Memory.writeW(op1, Flags.getLzFresw());
        Flags.LzFlags.Type = Flags.TypeFlag.ADDw;
    }

    // (int, uint16)
    public void ADDW(int regId, int op2) {
        op2 &= 0xffff;
        Flags.setLzFVar1w(Register.Regs[regId].getWord());
        Flags.setLzFvar2w(op2);
        Flags.setLzFresw(0xffff & (Flags.getLzFVar1w() + Flags.getLzFvar2w()));
        Register.Regs[regId].setWord(Flags.getLzFresw());
        Flags.LzFlags.Type = Flags.TypeFlag.ADDw;
    }

    public void ADCW_M(int op1, int op2) {
        op2 &= 0xffff;

        Flags.LzFlags.oldCF = Flags.getCF() ? 1 : 0;
        Flags.setLzFVar1w(Memory.readW(op1));
        Flags.setLzFvar2w(op2);
        Flags.setLzFresw(
                0xffff & (Flags.getLzFVar1w() + Flags.getLzFvar2w() + Flags.LzFlags.oldCF));
        Memory.writeW(op1, Flags.getLzFresw());
        Flags.LzFlags.Type = Flags.TypeFlag.ADCw;
    }

    public void ADCW(int regId, int op2) {
        op2 &= 0xffff;

        Flags.LzFlags.oldCF = Flags.getCF() ? 1 : 0;
        Flags.setLzFVar1w(Register.Regs[regId].getWord());
        Flags.setLzFvar2w(op2);
        Flags.setLzFresw(
                0xffff & (Flags.getLzFVar1w() + Flags.getLzFvar2w() + Flags.LzFlags.oldCF));
        Register.Regs[regId].setWord(Flags.getLzFresw());
        Flags.LzFlags.Type = Flags.TypeFlag.ADCw;
    }

    public void SBBW_M(int op1, int op2) {
        op2 &= 0xffff;

        Flags.LzFlags.oldCF = Flags.getCF() ? 1 : 0;
        Flags.setLzFVar1w(Memory.readW(op1));
        Flags.setLzFvar2w(op2);
        Flags.setLzFresw(
                0xffff & (Flags.getLzFVar1w() - (Flags.getLzFvar2w() + Flags.LzFlags.oldCF)));
        Memory.writeW(op1, Flags.getLzFresw());
        Flags.LzFlags.Type = Flags.TypeFlag.SBBw;
    }

    public void SBBW(int regId, int op2) {
        op2 &= 0xffff;

        Flags.LzFlags.oldCF = Flags.getCF() ? 1 : 0;
        Flags.setLzFVar1w(Register.Regs[regId].getWord());
        Flags.setLzFvar2w(op2);
        Flags.setLzFresw(
                0xffff & (Flags.getLzFVar1w() - (Flags.getLzFvar2w() + Flags.LzFlags.oldCF)));
        Register.Regs[regId].setWord(Flags.getLzFresw());
        Flags.LzFlags.Type = Flags.TypeFlag.SBBw;
    }

    public void SUBW_M(int op1, int op2) {
        op2 &= 0xffff;

        Flags.setLzFVar1w(Memory.readW(op1));
        Flags.setLzFvar2w(op2);
        Flags.setLzFresw(0xffff & (Flags.getLzFVar1w() - Flags.getLzFvar2w()));
        Memory.writeW(op1, Flags.getLzFresw());
        Flags.LzFlags.Type = Flags.TypeFlag.SUBw;
    }

    public void SUBW(int regId, int op2) {
        op2 &= 0xffff;

        Flags.setLzFVar1w(Register.Regs[regId].getWord());
        Flags.setLzFvar2w(op2);
        Flags.setLzFresw(0xffff & (Flags.getLzFVar1w() - Flags.getLzFvar2w()));
        Register.Regs[regId].setWord(Flags.getLzFresw());
        Flags.LzFlags.Type = Flags.TypeFlag.SUBw;
    }

    public void ORW_M(int op1, int op2) {
        op2 &= 0xffff;

        Flags.setLzFVar1w(Memory.readW(op1));
        Flags.setLzFvar2w(op2);
        Flags.setLzFresw(0xffff & (Flags.getLzFVar1w() | Flags.getLzFvar2w()));
        Memory.writeW(op1, Flags.getLzFresw());
        Flags.LzFlags.Type = Flags.TypeFlag.ORw;
    }

    public void ORW(int regId, int op2) {
        op2 &= 0xffff;

        Flags.setLzFVar1w(Register.Regs[regId].getWord());
        Flags.setLzFvar2w(op2);
        Flags.setLzFresw(0xffff & (Flags.getLzFVar1w() | Flags.getLzFvar2w()));
        Register.Regs[regId].setWord(Flags.getLzFresw());
        Flags.LzFlags.Type = Flags.TypeFlag.ORw;
    }

    public void XORW_M(int op1, int op2) {
        op2 &= 0xffff;

        Flags.setLzFVar1w(Memory.readW(op1));
        Flags.setLzFvar2w(op2);
        Flags.setLzFresw(0xffff & (Flags.getLzFVar1w() ^ Flags.getLzFvar2w()));
        Memory.writeW(op1, Flags.getLzFresw());
        Flags.LzFlags.Type = Flags.TypeFlag.XORw;
    }

    public void XORW(int regId, int op2) {
        op2 &= 0xffff;

        Flags.setLzFVar1w(Register.Regs[regId].getWord());
        Flags.setLzFvar2w(op2);
        Flags.setLzFresw(0xffff & (Flags.getLzFVar1w() ^ Flags.getLzFvar2w()));
        Register.Regs[regId].setWord(Flags.getLzFresw());
        Flags.LzFlags.Type = Flags.TypeFlag.XORw;
    }

    public void ANDW_M(int op1, int op2) {
        op2 &= 0xffff;

        Flags.setLzFVar1w(Memory.readW(op1));
        Flags.setLzFvar2w(op2);
        Flags.setLzFresw(0xffff & (Flags.getLzFVar1w() & Flags.getLzFvar2w()));
        Memory.writeW(op1, Flags.getLzFresw());
        Flags.LzFlags.Type = Flags.TypeFlag.ANDw;

    }

    public void ANDW(int regId, int op2) {
        op2 &= 0xffff;

        Flags.setLzFVar1w(Register.Regs[regId].getWord());
        Flags.setLzFvar2w(op2);
        Flags.setLzFresw(0xffff & (Flags.getLzFVar1w() & Flags.getLzFvar2w()));
        Register.Regs[regId].setWord(Flags.getLzFresw());
        Flags.LzFlags.Type = Flags.TypeFlag.ANDw;

    }

    public void CMPW_M(int op1, int op2) {
        op2 &= 0xffff;

        Flags.setLzFVar1w(Memory.readW(op1));
        Flags.setLzFvar2w(op2);
        Flags.setLzFresw(0xffff & (Flags.getLzFVar1w() - Flags.getLzFvar2w()));
        Flags.LzFlags.Type = Flags.TypeFlag.CMPw;
    }

    public void CMPW(int regId, int op2) {
        op2 &= 0xffff;

        Flags.setLzFVar1w(Register.Regs[regId].getWord());
        Flags.setLzFvar2w(op2);
        Flags.setLzFresw(0xffff & (Flags.getLzFVar1w() - Flags.getLzFvar2w()));
        Flags.LzFlags.Type = Flags.TypeFlag.CMPw;
    }

    // (uint16, uint16)
    public void CMPWSTR(int op1, int op2) {
        Flags.setLzFVar1w(op1);
        Flags.setLzFvar2w(op2);
        Flags.setLzFresw(Flags.getLzFVar1w() - Flags.getLzFvar2w());
        Flags.LzFlags.Type = Flags.TypeFlag.CMPw;
    }

    public void TESTW_M(int op1, int op2) {
        op2 &= 0xffff;

        Flags.setLzFVar1w(Memory.readW(op1));
        Flags.setLzFvar2w(op2);
        Flags.setLzFresw(0xffff & (Flags.getLzFVar1w() & Flags.getLzFvar2w()));
        Flags.LzFlags.Type = Flags.TypeFlag.TESTw;
    }

    public void TESTW(int regId, int op2) {
        op2 &= 0xffff;

        Flags.setLzFVar1w(Register.Regs[regId].getWord());
        Flags.setLzFvar2w(op2);
        Flags.setLzFresw(0xffff & (Flags.getLzFVar1w() & Flags.getLzFvar2w()));
        Flags.LzFlags.Type = Flags.TypeFlag.TESTw;
    }

    /* All DWORD General Instructions */

    public void ADDD_M(int op1, int op2) {
        Flags.setLzFVar1d(Memory.readD(op1));
        Flags.setLzFVar2d(op2);
        Flags.setLzFResd(Flags.getLzFVar1d() + Flags.getLzFVar2d());
        Memory.writeD(op1, Flags.getLzFResd());
        Flags.LzFlags.Type = Flags.TypeFlag.ADDd;
    }

    public void ADDD(int regId, int op2) {
        Flags.setLzFVar1d(Register.Regs[regId].getDWord());
        Flags.setLzFVar2d(op2);
        Flags.setLzFResd(Flags.getLzFVar1d() + Flags.getLzFVar2d());
        Register.Regs[regId].setDWord(Flags.getLzFResd());
        Flags.LzFlags.Type = Flags.TypeFlag.ADDd;
    }

    public void ADCD_M(int op1, int op2) {
        Flags.LzFlags.oldCF = Flags.getCF() ? 1 : 0;
        Flags.setLzFVar1d(Memory.readD(op1));
        Flags.setLzFVar2d(op2);
        Flags.setLzFResd(Flags.getLzFVar1d() + Flags.getLzFVar2d() + Flags.LzFlags.oldCF);
        Memory.writeD(op1, Flags.getLzFResd());
        Flags.LzFlags.Type = Flags.TypeFlag.ADCd;

    }

    public void ADCD(int regId, int op2) {
        Flags.LzFlags.oldCF = Flags.getCF() ? 1 : 0;
        Flags.setLzFVar1d(Register.Regs[regId].getDWord());
        Flags.setLzFVar2d(op2);
        Flags.setLzFResd(Flags.getLzFVar1d() + Flags.getLzFVar2d() + Flags.LzFlags.oldCF);
        Register.Regs[regId].setDWord(Flags.getLzFResd());
        Flags.LzFlags.Type = Flags.TypeFlag.ADCd;

    }

    public void SBBD_M(int op1, int op2) {
        Flags.LzFlags.oldCF = Flags.getCF() ? 1 : 0;
        Flags.setLzFVar1d(Memory.readD(op1));
        Flags.setLzFVar2d(op2);
        Flags.setLzFResd(Flags.getLzFVar1d() - (Flags.getLzFVar2d() + Flags.LzFlags.oldCF));
        Memory.writeD(op1, Flags.getLzFResd());
        Flags.LzFlags.Type = Flags.TypeFlag.SBBd;
    }

    public void SBBD(int regId, int op2) {
        Flags.LzFlags.oldCF = Flags.getCF() ? 1 : 0;
        Flags.setLzFVar1d(Register.Regs[regId].getDWord());
        Flags.setLzFVar2d(op2);
        Flags.setLzFResd(Flags.getLzFVar1d() - (Flags.getLzFVar2d() + Flags.LzFlags.oldCF));
        Register.Regs[regId].setDWord(Flags.getLzFResd());
        Flags.LzFlags.Type = Flags.TypeFlag.SBBd;
    }

    public void SUBD_M(int op1, int op2) {
        Flags.setLzFVar1d(Memory.readD(op1));
        Flags.setLzFVar2d(op2);
        Flags.setLzFResd(Flags.getLzFVar1d() - Flags.getLzFVar2d());
        Memory.writeD(op1, Flags.getLzFResd());
        Flags.LzFlags.Type = Flags.TypeFlag.SUBd;

    }

    public void SUBD(int regId, int op2) {
        Flags.setLzFVar1d(Register.Regs[regId].getDWord());
        Flags.setLzFVar2d(op2);
        Flags.setLzFResd(Flags.getLzFVar1d() - Flags.getLzFVar2d());
        Register.Regs[regId].setDWord(Flags.getLzFResd());
        Flags.LzFlags.Type = Flags.TypeFlag.SUBd;

    }

    public void ORD_M(int op1, int op2) {
        Flags.setLzFVar1d(Memory.readD(op1));
        Flags.setLzFVar2d(op2);
        Flags.setLzFResd(Flags.getLzFVar1d() | Flags.getLzFVar2d());
        Memory.writeD(op1, Flags.getLzFResd());
        Flags.LzFlags.Type = Flags.TypeFlag.ORd;

    }

    public void ORD(int regId, int op2) {
        Flags.setLzFVar1d(Register.Regs[regId].getDWord());
        Flags.setLzFVar2d(op2);
        Flags.setLzFResd(Flags.getLzFVar1d() | Flags.getLzFVar2d());
        Register.Regs[regId].setDWord(Flags.getLzFResd());
        Flags.LzFlags.Type = Flags.TypeFlag.ORd;

    }

    public void XORD_M(int op1, int op2) {
        Flags.setLzFVar1d(Memory.readD(op1));
        Flags.setLzFVar2d(op2);
        Flags.setLzFResd(Flags.getLzFVar1d() ^ Flags.getLzFVar2d());
        Memory.writeD(op1, Flags.getLzFResd());
        Flags.LzFlags.Type = Flags.TypeFlag.XORd;
    }

    public void XORD(int regId, int op2) {
        Flags.setLzFVar1d(Register.Regs[regId].getDWord());
        Flags.setLzFVar2d(op2);
        Flags.setLzFResd(Flags.getLzFVar1d() ^ Flags.getLzFVar2d());
        Register.Regs[regId].setDWord(Flags.getLzFResd());
        Flags.LzFlags.Type = Flags.TypeFlag.XORd;
    }

    public void ANDD_M(int op1, int op2) {
        Flags.setLzFVar1d(Memory.readD(op1));
        Flags.setLzFVar2d(op2);
        Flags.setLzFResd(Flags.getLzFVar1d() & Flags.getLzFVar2d());
        Memory.writeD(op1, Flags.getLzFResd());
        Flags.LzFlags.Type = Flags.TypeFlag.ANDd;

    }

    public void ANDD(int regId, int op2) {
        Flags.setLzFVar1d(Register.Regs[regId].getDWord());
        Flags.setLzFVar2d(op2);
        Flags.setLzFResd(Flags.getLzFVar1d() & Flags.getLzFVar2d());
        Register.Regs[regId].setDWord(Flags.getLzFResd());
        Flags.LzFlags.Type = Flags.TypeFlag.ANDd;

    }

    public void CMPD_M(int op1, int op2) {
        Flags.setLzFVar1d(Memory.readD(op1));
        Flags.setLzFVar2d(op2);
        Flags.setLzFResd(Flags.getLzFVar1d() - Flags.getLzFVar2d());
        Flags.LzFlags.Type = Flags.TypeFlag.CMPd;
    }

    public void CMPD(int regId, int op2) {
        Flags.setLzFVar1d(Register.Regs[regId].getDWord());

        Flags.setLzFVar2d(op2);
        Flags.setLzFResd(Flags.getLzFVar1d() - Flags.getLzFVar2d());
        Flags.LzFlags.Type = Flags.TypeFlag.CMPd;
    }

    public void CMPDSTR(int op1, int op2) {
        Flags.setLzFVar1d(op1);
        Flags.setLzFVar2d(op2);
        Flags.setLzFResd(Flags.getLzFVar1d() - Flags.getLzFVar2d());
        Flags.LzFlags.Type = Flags.TypeFlag.CMPd;
    }

    public void TESTD_M(int op1, int op2) {
        Flags.setLzFVar1d(Memory.readD(op1));
        Flags.setLzFVar2d(op2);
        Flags.setLzFResd(Flags.getLzFVar1d() & Flags.getLzFVar2d());
        Flags.LzFlags.Type = Flags.TypeFlag.TESTd;
    }

    public void TESTD(int regId, int op2) {
        Flags.setLzFVar1d(Register.Regs[regId].getDWord());
        Flags.setLzFVar2d(op2);
        Flags.setLzFResd(Flags.getLzFVar1d() & Flags.getLzFVar2d());
        Flags.LzFlags.Type = Flags.TypeFlag.TESTd;
    }

    public void INCB(int op1) {
        Register.setFlagBit(Register.FlagCF, Flags.getCF());
        Flags.setLzFVar1b(Memory.readB(op1));
        Flags.setLzFResb(Flags.getLzFVar1b() + 1);
        Memory.writeB(op1, Flags.getLzFResb());
        Flags.LzFlags.Type = Flags.TypeFlag.INCb;

    }

    public void INCBL(int regId) {
        Register.setFlagBit(Register.FlagCF, Flags.getCF());
        Flags.setLzFVar1b(Register.Regs[regId].getByteL());
        Flags.setLzFResb(Flags.getLzFVar1b() + 1);
        Register.Regs[regId].setByteL(Flags.getLzFResb());
        Flags.LzFlags.Type = Flags.TypeFlag.INCb;

    }

    public void INCBH(int regId) {
        Register.setFlagBit(Register.FlagCF, Flags.getCF());
        Flags.setLzFVar1b(Register.Regs[regId].getByteH());
        Flags.setLzFResb(Flags.getLzFVar1b() + 1);
        Register.Regs[regId].setByteH(Flags.getLzFResb());
        Flags.LzFlags.Type = Flags.TypeFlag.INCb;

    }

    public void INCW_M(int op1) {
        Register.setFlagBit(Register.FlagCF, Flags.getCF());
        Flags.setLzFVar1w(Memory.readW(op1));
        Flags.setLzFresw(0xffff & (Flags.getLzFVar1w() + 1));
        Memory.writeW(op1, Flags.getLzFresw());
        Flags.LzFlags.Type = Flags.TypeFlag.INCw;

    }

    public void INCW(int regId) {
        Register.setFlagBit(Register.FlagCF, Flags.getCF());
        Flags.setLzFVar1w(Register.Regs[regId].getWord());
        Flags.setLzFresw(0xffff & (Flags.getLzFVar1w() + 1));
        Register.Regs[regId].setWord(Flags.getLzFresw());
        Flags.LzFlags.Type = Flags.TypeFlag.INCw;
    }

    public void INCD_M(int op1) {
        Register.setFlagBit(Register.FlagCF, Flags.getCF());
        Flags.setLzFVar1d(Memory.readD(op1));
        Flags.setLzFResd(Flags.getLzFVar1d() + 1);
        Memory.writeD(op1, Flags.getLzFResd());
        Flags.LzFlags.Type = Flags.TypeFlag.INCd;

    }

    public void INCD(int regId) {
        Register.setFlagBit(Register.FlagCF, Flags.getCF());
        Flags.setLzFVar1d(Register.Regs[regId].getDWord());
        Flags.setLzFResd(Flags.getLzFVar1d() + 1);
        Register.Regs[regId].setDWord(Flags.getLzFResd());
        Flags.LzFlags.Type = Flags.TypeFlag.INCd;

    }

    public void DECB(int op1) {
        Register.setFlagBit(Register.FlagCF, Flags.getCF());
        Flags.setLzFVar1b(Memory.readB(op1));
        Flags.setLzFResb(Flags.getLzFVar1b() - 1);
        Memory.writeB(op1, Flags.getLzFResb());
        Flags.LzFlags.Type = Flags.TypeFlag.DECb;

    }

    public void DECBL(int regId) {
        Register.setFlagBit(Register.FlagCF, Flags.getCF());
        Flags.setLzFVar1b(Register.Regs[regId].getByteL());
        Flags.setLzFResb(Flags.getLzFVar1b() - 1);
        Register.Regs[regId].setByteL(Flags.getLzFResb());
        Flags.LzFlags.Type = Flags.TypeFlag.DECb;
    }

    public void DECBH(int regId) {
        Register.setFlagBit(Register.FlagCF, Flags.getCF());
        Flags.setLzFVar1b(Register.Regs[regId].getByteH());
        Flags.setLzFResb(Flags.getLzFVar1b() - 1);
        Register.Regs[regId].setByteH(Flags.getLzFResb());
        Flags.LzFlags.Type = Flags.TypeFlag.DECb;

    }

    public void DECW_M(int op1) {
        Register.setFlagBit(Register.FlagCF, Flags.getCF());
        Flags.setLzFVar1w(Memory.readW(op1));
        Flags.setLzFresw(Flags.getLzFVar1w() - 1);
        Memory.writeW(op1, Flags.getLzFresw());
        Flags.LzFlags.Type = Flags.TypeFlag.DECw;

    }

    public void DECW(int regId) {
        Register.setFlagBit(Register.FlagCF, Flags.getCF());
        Flags.setLzFVar1w(Register.Regs[regId].getWord());
        Flags.setLzFresw(Flags.getLzFVar1w() - 1);
        Register.Regs[regId].setWord(Flags.getLzFresw());
        Flags.LzFlags.Type = Flags.TypeFlag.DECw;

    }


    public void DECD_M(int op1) {
        Register.setFlagBit(Register.FlagCF, Flags.getCF());
        Flags.setLzFVar1d(Memory.readD(op1));
        Flags.setLzFResd(Flags.getLzFVar1d() - 1);
        Memory.writeD(op1, Flags.getLzFResd());
        Flags.LzFlags.Type = Flags.TypeFlag.DECd;
    }

    public void DECD(int regId) {
        Register.setFlagBit(Register.FlagCF, Flags.getCF());
        Flags.setLzFVar1d(Register.Regs[regId].getDWord());
        Flags.setLzFResd(Flags.getLzFVar1d() - 1);
        Register.Regs[regId].setDWord(Flags.getLzFResd());
        Flags.LzFlags.Type = Flags.TypeFlag.DECd;
    }

    public void DAA() {
        if (((Register.getRegAL() & 0x0F) > 0x09) || Flags.getAF()) {
            if ((Register.getRegAL() > 0x99) || Flags.getCF()) {
                Register.setRegAL(Register.getRegAL() + 0x60);
                Register.setFlagBit(Register.FlagCF, true);
            } else {
                Register.setFlagBit(Register.FlagCF, false);
            }
            Register.setRegAL(Register.getRegAL() + 0x06);
            Register.setFlagBit(Register.FlagAF, true);
        } else {
            if ((Register.getRegAL() > 0x99) || Flags.getCF()) {
                Register.setRegAL(Register.getRegAL() + 0x60);
                Register.setFlagBit(Register.FlagCF, true);
            } else {
                Register.setFlagBit(Register.FlagCF, false);
            }
            Register.setFlagBit(Register.FlagAF, false);
        }
        Register.setFlagBit(Register.FlagSF, (Register.getRegAL() & 0x80) != 0);
        Register.setFlagBit(Register.FlagZF, (Register.getRegAL() == 0));
        Register.setFlagBit(Register.FlagPF, Flags.ParityLookup[Register.getRegAL()]);
        Flags.LzFlags.Type = Flags.TypeFlag.UNKNOWN;
    }

    public void DAS() {
        boolean osigned = (Register.getRegAL() & 0x80) != 0;
        if (((Register.getRegAL() & 0x0f) > 9) || Flags.getAF()) {
            if ((Register.getRegAL() > 0x99) || Flags.getCF()) {
                Register.setRegAL(Register.getRegAL() - 0x60);
                Register.setFlagBit(Register.FlagCF, true);
            } else {
                Register.setFlagBit(Register.FlagCF, (Register.getRegAL() <= 0x05));
            }
            Register.setRegAL(Register.getRegAL() - 6);
            Register.setFlagBit(Register.FlagAF, true);
        } else {
            if ((Register.getRegAL() > 0x99) || Flags.getCF()) {
                Register.setRegAL(Register.getRegAL() - 0x60);
                Register.setFlagBit(Register.FlagCF, true);
            } else {
                Register.setFlagBit(Register.FlagCF, false);
            }
            Register.setFlagBit(Register.FlagAF, false);
        }
        Register.setFlagBit(Register.FlagOF, osigned && ((Register.getRegAL() & 0x80) == 0));
        Register.setFlagBit(Register.FlagSF, (Register.getRegAL() & 0x80) != 0);
        Register.setFlagBit(Register.FlagZF, (Register.getRegAL() == 0));
        Register.setFlagBit(Register.FlagPF, Flags.ParityLookup[Register.getRegAL()]);
        Flags.LzFlags.Type = Flags.TypeFlag.UNKNOWN;
    }



    public void AAA() {
        Register.setFlagBit(Register.FlagSF,
                ((Register.getRegAL() >= 0x7a) && (Register.getRegAL() <= 0xf9)));
        if ((Register.getRegAL() & 0xf) > 9) {
            Register.setFlagBit(Register.FlagOF, (Register.getRegAL() & 0xf0) == 0x70);
            Register.setRegAX(Register.getRegAX() + 0x106);
            Register.setFlagBit(Register.FlagCF, true);
            Register.setFlagBit(Register.FlagZF, (Register.getRegAL() == 0));
            Register.setFlagBit(Register.FlagAF, true);
        } else if (Flags.getAF()) {
            Register.setRegAX(Register.getRegAX() + 0x106);
            Register.setFlagBit(Register.FlagOF, false);
            Register.setFlagBit(Register.FlagCF, true);
            Register.setFlagBit(Register.FlagZF, false);
            Register.setFlagBit(Register.FlagAF, true);
        } else {
            Register.setFlagBit(Register.FlagOF, false);
            Register.setFlagBit(Register.FlagCF, false);
            Register.setFlagBit(Register.FlagZF, (Register.getRegAL() == 0));
            Register.setFlagBit(Register.FlagAF, false);
        }
        Register.setFlagBit(Register.FlagPF, Flags.ParityLookup[Register.getRegAL()]);
        Register.setRegAL(Register.getRegAL() & 0x0F);
        Flags.LzFlags.Type = Flags.TypeFlag.UNKNOWN;

    }

    public void AAS() {
        if ((Register.getRegAL() & 0x0f) > 9) {
            Register.setFlagBit(Register.FlagSF, (Register.getRegAL() > 0x85));
            Register.setRegAX(Register.getRegAX() - 0x106);
            Register.setFlagBit(Register.FlagOF, false);
            Register.setFlagBit(Register.FlagCF, true);
            Register.setFlagBit(Register.FlagAF, true);
        } else if (Flags.getAF()) {
            Register.setFlagBit(Register.FlagOF,
                    ((Register.getRegAL() >= 0x80) && (Register.getRegAL() <= 0x85)));
            Register.setFlagBit(Register.FlagSF,
                    (Register.getRegAL() < 0x06) || (Register.getRegAL() > 0x85));
            Register.setRegAX(Register.getRegAX() - 0x106);
            Register.setFlagBit(Register.FlagCF, true);
            Register.setFlagBit(Register.FlagAF, true);
        } else {
            Register.setFlagBit(Register.FlagSF, (Register.getRegAL() >= 0x80));
            Register.setFlagBit(Register.FlagOF, false);
            Register.setFlagBit(Register.FlagCF, false);
            Register.setFlagBit(Register.FlagAF, false);
        }
        Register.setFlagBit(Register.FlagZF, (Register.getRegAL() == 0));
        Register.setFlagBit(Register.FlagPF, Flags.ParityLookup[Register.getRegAL()]);
        Register.setRegAL(Register.getRegAL() & 0x0F);
        Flags.LzFlags.Type = Flags.TypeFlag.UNKNOWN;

    }

    // byte op1
    public SwitchReturn AAM(int op1) {
        int dv = op1;
        if (dv != 0) {
            Register.setRegAH(Register.getRegAL() / dv);
            Register.setRegAL(Register.getRegAL() % dv);
            Register.setFlagBit(Register.FlagSF, (Register.getRegAL() & 0x80) != 0);
            Register.setFlagBit(Register.FlagZF, (Register.getRegAL() == 0));
            Register.setFlagBit(Register.FlagPF, Flags.ParityLookup[Register.getRegAL()]);
            Register.setFlagBit(Register.FlagCF, false);
            Register.setFlagBit(Register.FlagOF, false);
            Register.setFlagBit(Register.FlagAF, false);
            Flags.LzFlags.Type = Flags.TypeFlag.UNKNOWN;
        } else {
            byte newNum = 0;
            CPU.exception(newNum, 0);
            return SwitchReturn.Continue;
        }
        return SwitchReturn.None;
    }


    // Took this from bochs, i seriously hate these weird bcd opcodes
    // (short op1)
    public void AAD(int op1) {
        int ax1 = 0xffff & (Register.getRegAH() * op1);
        int ax2 = 0xffff & (ax1 + Register.getRegAL());
        Register.setRegAL(ax2);
        Register.setRegAH(0);
        Register.setFlagBit(Register.FlagCF, false);
        Register.setFlagBit(Register.FlagOF, false);
        Register.setFlagBit(Register.FlagAF, false);
        Register.setFlagBit(Register.FlagSF, Register.getRegAL() >= 0x80);
        Register.setFlagBit(Register.FlagZF, Register.getRegAL() == 0);
        Register.setFlagBit(Register.FlagPF, Flags.ParityLookup[Register.getRegAL()]);
        Flags.LzFlags.Type = Flags.TypeFlag.UNKNOWN;
    }


    public void MULB(int op1) {
        Register.setRegAX(Register.getRegAL() * Memory.readB(op1));
        Flags.fillFlagsNoCFOF();
        Register.setFlagBit(Register.FlagZF, Register.getRegAL() == 0);
        if ((Register.getRegAX() & 0xff00) != 0) {
            Register.setFlagBit(Register.FlagCF, true);
            Register.setFlagBit(Register.FlagOF, true);
        } else {
            Register.setFlagBit(Register.FlagCF, false);
            Register.setFlagBit(Register.FlagOF, false);
        }

    }

    public void MULBL(int regId) {
        Register.setRegAX(Register.getRegAL() * Register.Regs[regId].getByteL());
        Flags.fillFlagsNoCFOF();
        Register.setFlagBit(Register.FlagZF, Register.getRegAL() == 0);
        if ((Register.getRegAX() & 0xff00) != 0) {
            Register.setFlagBit(Register.FlagCF, true);
            Register.setFlagBit(Register.FlagOF, true);
        } else {
            Register.setFlagBit(Register.FlagCF, false);
            Register.setFlagBit(Register.FlagOF, false);
        }

    }

    public void MULBH(int regId) {
        Register.setRegAX(Register.getRegAL() * Register.Regs[regId].getByteH());
        Flags.fillFlagsNoCFOF();
        Register.setFlagBit(Register.FlagZF, Register.getRegAL() == 0);
        if ((Register.getRegAX() & 0xff00) != 0) {
            Register.setFlagBit(Register.FlagCF, true);
            Register.setFlagBit(Register.FlagOF, true);
        } else {
            Register.setFlagBit(Register.FlagCF, false);
            Register.setFlagBit(Register.FlagOF, false);
        }

    }

    public void MULW_M(int op1) {
        int tempu = Register.getRegAX() * Memory.readW(op1);
        Register.setRegAX(0xffff & tempu);
        Register.setRegDX(0xffff & (tempu >>> 16));
        Flags.fillFlagsNoCFOF();
        Register.setFlagBit(Register.FlagZF, Register.getRegAX() == 0);
        if (Register.getRegDX() != 0) {
            Register.setFlagBit(Register.FlagCF, true);
            Register.setFlagBit(Register.FlagOF, true);
        } else {
            Register.setFlagBit(Register.FlagCF, false);
            Register.setFlagBit(Register.FlagOF, false);
        }
    }

    public void MULW(int regId) {
        int tempu = Register.getRegAX() * Register.Regs[regId].getWord();
        Register.setRegAX(0xffff & tempu);
        Register.setRegDX(0xffff & (tempu >>> 16));
        Flags.fillFlagsNoCFOF();
        Register.setFlagBit(Register.FlagZF, Register.getRegAX() == 0);
        if (Register.getRegDX() != 0) {
            Register.setFlagBit(Register.FlagCF, true);
            Register.setFlagBit(Register.FlagOF, true);
        } else {
            Register.setFlagBit(Register.FlagCF, false);
            Register.setFlagBit(Register.FlagOF, false);
        }
    }

    public void MULD_M(int op1) {
        long tempu = (Register.getRegEAX() & 0xffffffffL) * (Memory.readD(op1) & 0xffffffffL);
        Register.setRegEAX((int) tempu);
        Register.setRegEDX((int) (tempu >>> 32));
        Flags.fillFlagsNoCFOF();
        Register.setFlagBit(Register.FlagZF, Register.getRegEAX() == 0);
        if (Register.getRegEDX() != 0) {
            Register.setFlagBit(Register.FlagCF, true);
            Register.setFlagBit(Register.FlagOF, true);
        } else {
            Register.setFlagBit(Register.FlagCF, false);
            Register.setFlagBit(Register.FlagOF, false);
        }
    }

    public void MULD(int regId) {
        long tempu = (Register.getRegEAX() & 0xffffffffL)
                * (Register.Regs[regId].getDWord() & 0xffffffffL);
        Register.setRegEAX((int) tempu);
        Register.setRegEDX((int) (tempu >>> 32));
        Flags.fillFlagsNoCFOF();
        Register.setFlagBit(Register.FlagZF, Register.getRegEAX() == 0);
        if (Register.getRegEDX() != 0) {
            Register.setFlagBit(Register.FlagCF, true);
            Register.setFlagBit(Register.FlagOF, true);
        } else {
            Register.setFlagBit(Register.FlagCF, false);
            Register.setFlagBit(Register.FlagOF, false);
        }
    }


    public SwitchReturn DIVB(int op1) {
        int val = Memory.readB(op1);
        if (val == 0) {
            byte newNum = 0;
            CPU.exception(newNum, 0);
            return SwitchReturn.Continue;
        }

        int quo = Register.getRegAX() / val;
        int rem = 0xff & (Register.getRegAX() % val);
        int quo8 = quo & 0xff;
        if (quo > 0xff) {
            byte newNum = 0;
            CPU.exception(newNum, 0);
            return SwitchReturn.Continue;
        }
        Register.setRegAH(rem);
        Register.setRegAL(quo8);

        return SwitchReturn.None;
    }

    public SwitchReturn DIVBL(int regId) {
        int val = Register.Regs[regId].getByteL();
        if (val == 0) {
            byte newNum = 0;
            CPU.exception(newNum, 0);
            return SwitchReturn.Continue;
        }

        int quo = Register.getRegAX() / val;
        int rem = 0xff & (Register.getRegAX() % val);
        int quo8 = quo & 0xff;
        if (quo > 0xff) {
            byte newNum = 0;
            CPU.exception(newNum, 0);
            return SwitchReturn.Continue;
        }
        Register.setRegAH(rem);
        Register.setRegAL(quo8);

        return SwitchReturn.None;
    }

    public SwitchReturn DIVBH(int regId) {
        int val = Register.Regs[lookupRMregbl[regId]].getByteH();
        if (val == 0) {
            byte newNum = 0;
            CPU.exception(newNum, 0);
            return SwitchReturn.Continue;
        }

        int quo = Register.getRegAX() / val;
        int rem = 0xff & (Register.getRegAX() % val);
        int quo8 = quo & 0xff;
        if (quo > 0xff) {
            byte newNum = 0;
            CPU.exception(newNum, 0);
            return SwitchReturn.Continue;
        }
        Register.setRegAH(rem);
        Register.setRegAL(quo8);

        return SwitchReturn.None;
    }

    public SwitchReturn DIVW_M(int op1) {
        int val = Memory.readW(op1);
        if (val == 0) {
            byte newNum = 0;
            CPU.exception(newNum, 0);
            return SwitchReturn.Continue;
        }
        int num = (Register.getRegDX() << 16) | Register.getRegAX();
        int quo = num / val;
        int rem = 0xffff & (num % val);
        int quo16 = quo & 0xffff;
        if (quo != quo16) {
            byte newNum = 0;
            CPU.exception(newNum, 0);
            return SwitchReturn.Continue;
        }
        Register.setRegDX(rem);
        Register.setRegAX(quo16);
        return SwitchReturn.None;
    }

    public SwitchReturn DIVW(int regId) {
        int val = Register.Regs[regId].getWord();
        if (val == 0) {
            byte newNum = 0;
            CPU.exception(newNum, 0);
            return SwitchReturn.Continue;
        }
        int num = (Register.getRegDX() << 16) | Register.getRegAX();
        int quo = num / val;
        int rem = 0xffff & (num % val);
        int quo16 = quo & 0xffff;
        if (quo != quo16) {
            byte newNum = 0;
            CPU.exception(newNum, 0);
            return SwitchReturn.Continue;
        }
        Register.setRegDX(rem);
        Register.setRegAX(quo16);
        return SwitchReturn.None;
    }

    public SwitchReturn DIVD_M(int op1) {
        long val = Memory.readD(op1) & 0xffffffffL;
        if (val == 0) {
            byte newNum = 0;
            CPU.exception(newNum, 0);
            return SwitchReturn.Continue;
        }
        long edx = Register.getRegEDX() & 0xffffffffL;

        if (edx <= val) {// if (quo!=(Bit64u)quo32)
            byte newNum = 0;
            CPU.exception(newNum, 0);
            return SwitchReturn.Continue;
        }
        long num = (edx << 32) | (Register.getRegEAX() & 0xffffffffL);
        long quo = num / val;
        int rem = (int) (num % val);
        Register.setRegEDX(rem);
        Register.setRegEAX((int) quo);

        return SwitchReturn.None;
    }

    public SwitchReturn DIVD(int regId) {
        long val = Register.Regs[regId].getDWord() & 0xffffffffL;
        if (val == 0) {
            byte newNum = 0;
            CPU.exception(newNum, 0);
            return SwitchReturn.Continue;
        }
        long edx = Register.getRegEDX() & 0xffffffffL;

        if (edx <= val) {// if (quo!=(Bit64u)quo32)
            byte newNum = 0;
            CPU.exception(newNum, 0);
            return SwitchReturn.Continue;
        }
        long num = (edx << 32) | (Register.getRegEAX() & 0xffffffffL);
        long quo = num / val;
        int rem = (int) (num % val);
        Register.setRegEDX(rem);
        Register.setRegEAX((int) quo);

        return SwitchReturn.None;
    }

    public SwitchReturn IDIVB(int op1) { // sbyte
        int val = (byte) Memory.readB(op1);
        if (val == 0) {
            byte newNum = 0;
            CPU.exception(newNum, 0);
            return SwitchReturn.Continue;
        }
        int quo = ((short) Register.getRegAX()) / val; // sbyte
        byte rem = (byte) ((short) Register.getRegAX() % val); // sbyte
        byte quo8s = (byte) (quo & 0xff);
        if (quo != (short) quo8s) {
            byte newNum = 0;
            CPU.exception(newNum, 0);
            return SwitchReturn.Continue;
        }
        Register.setRegAH(rem);
        Register.setRegAL(quo8s);
        return SwitchReturn.None;
    }

    public SwitchReturn IDIVBL(int regId) { // sbyte
        int val = (byte) Register.Regs[regId].getByteL();
        if (val == 0) {
            byte newNum = 0;
            CPU.exception(newNum, 0);
            return SwitchReturn.Continue;
        }
        int quo = ((short) Register.getRegAX()) / val; // sbyte
        byte rem = (byte) ((short) Register.getRegAX() % val); // sbyte
        byte quo8s = (byte) (quo & 0xff);
        if (quo != (short) quo8s) {
            byte newNum = 0;
            CPU.exception(newNum, 0);
            return SwitchReturn.Continue;
        }
        Register.setRegAH(rem);
        Register.setRegAL(quo8s);
        return SwitchReturn.None;
    }

    public SwitchReturn IDIVBH(int regId) {
        // sbyte
        int val = (byte) Register.Regs[regId].getByteH();
        if (val == 0) {
            byte newNum = 0;
            CPU.exception(newNum, 0);
            return SwitchReturn.Continue;
        }
        int quo = ((short) Register.getRegAX()) / val;
        // sbyte
        byte rem = (byte) ((short) Register.getRegAX() % val);
        // sbyte
        byte quo8s = (byte) (quo & 0xff);
        if (quo != (short) quo8s) {
            byte newNum = 0;
            CPU.exception(newNum, 0);
            return SwitchReturn.Continue;
        }
        Register.setRegAH((byte) rem);
        Register.setRegAL((byte) quo8s);
        return SwitchReturn.None;
    }

    public SwitchReturn IDIVW_M(int op1) {
        int val = (short) Memory.readW(op1);// Bit16s
        if (val == 0) {
            byte newNum = 0;
            CPU.exception(newNum, 0);
            return SwitchReturn.Continue;
        }
        int num = ((Register.getRegDX() & 0xffff) << 16) | Register.getRegAX();
        int quo = num / val;
        short rem = (short) (num % val);// Bit16s
        short quo16s = (short) quo;// Bit16s
        if (quo != (int) quo16s) {
            byte newNum = 0;
            CPU.exception(newNum, 0);
            return SwitchReturn.Continue;
        }
        Register.setRegDX(rem);
        Register.setRegAX(quo16s);

        return SwitchReturn.None;
    }

    public SwitchReturn IDIVW(int regId) {
        int val = (short) Register.Regs[regId].getWord();
        if (val == 0) {
            byte newNum = 0;
            CPU.exception(newNum, 0);
            return SwitchReturn.Continue;
        }
        int num = ((Register.getRegDX() & 0xffff) << 16) | Register.getRegAX();
        int quo = num / val;
        short rem = (short) (num % val);
        short quo16s = (short) quo;
        if (quo != (int) quo16s) {
            byte newNum = 0;
            CPU.exception(newNum, 0);
            return SwitchReturn.Continue;
        }
        Register.setRegDX(rem);
        Register.setRegAX(quo16s);

        return SwitchReturn.None;
    }

    public SwitchReturn IDIVD_M(int op1) {
        int val = Memory.readD(op1);// Bit32s
        if (val == 0) {
            byte newNum = 0;
            CPU.exception(newNum, 0);
            return SwitchReturn.Continue;
        }
        long num =
                ((Register.getRegEDX() & 0xffffffffL) << 32) | (Register.getRegEAX() & 0xffffffffL);
        long quo = num / val;
        int rem = (int) (num % val);
        int quo32s = (int) (quo & 0xffffffff);
        if (quo != (long) quo32s) {
            byte newNum = 0;
            CPU.exception(newNum, 0);
            return SwitchReturn.Continue;
        }
        Register.setRegEDX(rem);
        Register.setRegEAX(quo32s);

        return SwitchReturn.None;
    }

    public SwitchReturn IDIVD(int regId) {
        int val = Register.Regs[regId].getDWord();// bit32
        if (val == 0) {
            byte newNum = 0;
            CPU.exception(newNum, 0);
            return SwitchReturn.Continue;
        }
        long num =
                ((Register.getRegEDX() & 0xffffffffL) << 32) | (Register.getRegEAX() & 0xffffffffL);
        long quo = num / val;
        int rem = (int) (num % val);
        int quo32s = (int) (quo & 0xffffffff);
        if (quo != (long) quo32s) {
            byte newNum = 0;
            CPU.exception(newNum, 0);
            return SwitchReturn.Continue;
        }
        Register.setRegEDX(rem);
        Register.setRegEAX(quo32s);

        return SwitchReturn.None;
    }


    public void IMULB(int op1) {
        // sbyte
        Register.setRegAX((byte) Register.getRegAL() * (byte) Memory.readB(op1));
        Flags.fillFlagsNoCFOF();
        if ((Register.getRegAX() & 0xff80) == 0xff80 || (Register.getRegAX() & 0xff80) == 0x0000) {
            Register.setFlagBit(Register.FlagCF, false);
            Register.setFlagBit(Register.FlagOF, false);
        } else {
            Register.setFlagBit(Register.FlagCF, true);
            Register.setFlagBit(Register.FlagOF, true);
        }
    }

    public void IMULBL(int regId) {
        // sbyte
        Register.setRegAX((byte) Register.getRegAL() * (byte) Register.Regs[regId].getByteL());
        Flags.fillFlagsNoCFOF();
        if ((Register.getRegAX() & 0xff80) == 0xff80 || (Register.getRegAX() & 0xff80) == 0x0000) {
            Register.setFlagBit(Register.FlagCF, false);
            Register.setFlagBit(Register.FlagOF, false);
        } else {
            Register.setFlagBit(Register.FlagCF, true);
            Register.setFlagBit(Register.FlagOF, true);
        }
    }

    public void IMULBH(int regId) {
        // sbyte
        Register.setRegAX((byte) Register.getRegAL() * (byte) Register.Regs[regId].getByteH());
        Flags.fillFlagsNoCFOF();
        if ((Register.getRegAX() & 0xff80) == 0xff80 || (Register.getRegAX() & 0xff80) == 0x0000) {
            Register.setFlagBit(Register.FlagCF, false);
            Register.setFlagBit(Register.FlagOF, false);
        } else {
            Register.setFlagBit(Register.FlagCF, true);
            Register.setFlagBit(Register.FlagOF, true);
        }
    }

    public void IMULW_M(int op1) {
        // Bit16s
        int temps = (short) Register.getRegAX() * (short) Memory.readW(op1);
        Register.setRegAX((short) temps);
        Register.setRegDX((short) (temps >> 16));//unsigned shift
        Flags.fillFlagsNoCFOF();
        if (((temps & 0xffff8000) == 0xffff8000 || (temps & 0xffff8000) == 0x0000)) {
            Register.setFlagBit(Register.FlagCF, false);
            Register.setFlagBit(Register.FlagOF, false);
        } else {
            Register.setFlagBit(Register.FlagCF, true);
            Register.setFlagBit(Register.FlagOF, true);
        }
    }

    public void IMULW(int regId) {
        int temps = (short) Register.getRegAX() * (short) Register.Regs[regId].getWord();
        Register.setRegAX((short) temps);
        Register.setRegDX((short) (temps >> 16));//unsigned shift
        Flags.fillFlagsNoCFOF();
        if (((temps & 0xffff8000) == 0xffff8000 || (temps & 0xffff8000) == 0x0000)) {
            Register.setFlagBit(Register.FlagCF, false);
            Register.setFlagBit(Register.FlagOF, false);
        } else {
            Register.setFlagBit(Register.FlagCF, true);
            Register.setFlagBit(Register.FlagOF, true);
        }
    }

    public void IMULD_M(int op1) {
        long temps = ((long) Register.getRegEAX()) * ((long) Memory.readD(op1));
        Register.setRegEAX((int) temps);
        Register.setRegEDX((int) (temps >> 32));//unsigned shift
        Flags.fillFlagsNoCFOF();
        if ((Register.getRegEDX() == 0xffffffff) && (Register.getRegEAX() & 0x80000000) != 0) {
            Register.setFlagBit(Register.FlagCF, false);
            Register.setFlagBit(Register.FlagOF, false);
        } else if ((Register.getRegEDX() == 0x00000000) && (Register.getRegEAX() < 0x80000000)) {
            Register.setFlagBit(Register.FlagCF, false);
            Register.setFlagBit(Register.FlagOF, false);
        } else {
            Register.setFlagBit(Register.FlagCF, true);
            Register.setFlagBit(Register.FlagOF, true);
        }
    }

    public void IMULD(int regId) {
        long temps = ((long) Register.getRegEAX() * (long) Register.Regs[regId].getDWord());
        Register.setRegEAX((int) (temps));
        Register.setRegEDX((int) (temps >> 32));//unsigned shift
        Flags.fillFlagsNoCFOF();
        if ((Register.getRegEDX() == 0xffffffff) && (Register.getRegEAX() & 0x80000000) != 0) {
            Register.setFlagBit(Register.FlagCF, false);
            Register.setFlagBit(Register.FlagOF, false);
        } else if ((Register.getRegEDX() == 0x00000000) && (Register.getRegEAX() < 0x80000000)) {
            Register.setFlagBit(Register.FlagCF, false);
            Register.setFlagBit(Register.FlagOF, false);
        } else {
            Register.setFlagBit(Register.FlagCF, true);
            Register.setFlagBit(Register.FlagOF, true);
        }
    }

    // public void DIMULW(int regId, short op2, short op3)
    public void DIMULW(int regId, int op2, int op3) {
        int res = (short) op2 * (short) op3;
        Register.Regs[regId].setWord((res & 0xffff));
        Flags.fillFlagsNoCFOF();
        if ((res > -32768) && (res < 32767)) {
            Register.setFlagBit(Register.FlagCF, false);
            Register.setFlagBit(Register.FlagOF, false);
        } else {
            Register.setFlagBit(Register.FlagCF, true);
            Register.setFlagBit(Register.FlagOF, true);
        }
    }


    public void DIMULD(int regId, int op2, int op3) {
        long res = ((long) op2) * ((long) op3);
        Register.Regs[regId].setDWord((int) res);
        Flags.fillFlagsNoCFOF();
        if ((res > -((long) 2147483647 + 1)) && (res < (long) 2147483647)) {
            Register.setFlagBit(Register.FlagCF, false);
            Register.setFlagBit(Register.FlagOF, false);
        } else {
            Register.setFlagBit(Register.FlagCF, true);
            Register.setFlagBit(Register.FlagOF, true);
        }
    }

    public void GRP2B(DOSFuncInt blah) {
        int rm = fetchB();
        int which = (rm >>> 3) & 7;

        if (rm >= 0xc0) {
            boolean blhs = lookupRMEAregbl[rm] >= 0;
            int regIdx = blhs ? lookupRMEAregbl[rm] : lookupRMEAregbh[rm];

            int op1 = blhs ? Register.Regs[regIdx].getByteL() : Register.Regs[regIdx].getByteH();

            int val = blah.run() & 0x1f;
            switch (which) {
                case 0x00:
                    // ROLB
                    if ((val & 0x7) == 0) {
                        if ((val & 0x18) != 0) {
                            Flags.fillFlagsNoCFOF();
                            Register.setFlagBit(Register.FlagCF, (op1 & 1) != 0);
                            Register.setFlagBit(Register.FlagOF, ((op1 & 1) ^ (op1 >>> 7)) != 0);
                        }
                        break;
                    }
                    Flags.fillFlagsNoCFOF();
                    Flags.setLzFVar1b(op1);
                    Flags.setLzFVar2b(val & 0x07);
                    Flags.setLzFResb((Flags.getLzFVar1b() << Flags.getLzFVar2b()
                            | (Flags.getLzFVar1b() >>> (8 - Flags.getLzFVar2b()))));
                    if (blhs)
                        Register.Regs[regIdx].setByteL(Flags.getLzFResb());
                    else
                        Register.Regs[regIdx].setByteH(Flags.getLzFResb());
                    Register.setFlagBit(Register.FlagCF, (Flags.getLzFResb() & 1) != 0);
                    Register.setFlagBit(Register.FlagOF,
                            ((Flags.getLzFResb() & 1) ^ (Flags.getLzFResb() >>> 7)) != 0);
                    break;
                case 0x01:
                    // RORB
                    if ((val & 0x7) == 0) {
                        if ((val & 0x18) != 0) {
                            Flags.fillFlagsNoCFOF();
                            Register.setFlagBit(Register.FlagCF, (op1 >>> 7) != 0);
                            Register.setFlagBit(Register.FlagOF,
                                    ((op1 >>> 7) ^ ((op1 >>> 6) & 1)) != 0);
                        }
                        break;
                    }
                    Flags.fillFlagsNoCFOF();
                    Flags.setLzFVar1b(op1);
                    Flags.setLzFVar2b(val & 0x07);
                    Flags.setLzFResb((Flags.getLzFVar1b() >>> Flags.getLzFVar2b()
                            | (Flags.getLzFVar1b() << (8 - Flags.getLzFVar2b()))));
                    if (blhs)
                        Register.Regs[regIdx].setByteL(Flags.getLzFResb());
                    else
                        Register.Regs[regIdx].setByteH(Flags.getLzFResb());
                    Register.setFlagBit(Register.FlagCF, (Flags.getLzFResb() & 0x80) != 0);
                    Register.setFlagBit(Register.FlagOF,
                            ((Flags.getLzFResb() ^ (Flags.getLzFResb() << 1)) & 0x80) != 0);

                    break;
                case 0x02:
                    // RCLB
                    if ((val % 9) == 0)
                        break; {
                    int cf = Flags.fillFlags() & 0x1;
                    Flags.setLzFVar1b(op1);
                    Flags.setLzFVar2b(val % 9);
                    Flags.setLzFResb((Flags.getLzFVar1b() << Flags.getLzFVar2b()
                            | (cf << (Flags.getLzFVar2b() - 1))
                            | (Flags.getLzFVar1b() >>> (9 - Flags.getLzFVar2b()))));
                    if (blhs)
                        Register.Regs[regIdx].setByteL(Flags.getLzFResb());
                    else
                        Register.Regs[regIdx].setByteH(Flags.getLzFResb());
                    Register.setFlagBit(Register.FlagCF,
                            ((Flags.getLzFVar1b() >>> (8 - Flags.getLzFVar2b())) & 1) != 0);
                    Register.setFlagBit(Register.FlagOF,
                            ((Register.Flags & 1) ^ (Flags.getLzFResb() >>> 7)) != 0);
                }
                    break;
                case 0x03:
                    // RCRB
                    if ((val % 9) != 0) {
                        int cf = Flags.fillFlags() & 0x1;
                        Flags.setLzFVar1b(op1);
                        Flags.setLzFVar2b(val % 9);
                        Flags.setLzFResb((Flags.getLzFVar1b() >>> Flags.getLzFVar2b()
                                | (cf << (8 - Flags.getLzFVar2b()))
                                | (Flags.getLzFVar1b() << (9 - Flags.getLzFVar2b()))));
                        if (blhs)
                            Register.Regs[regIdx].setByteL(Flags.getLzFResb());
                        else
                            Register.Regs[regIdx].setByteH(Flags.getLzFResb());
                        Register.setFlagBit(Register.FlagCF,
                                ((Flags.getLzFVar1b() >>> (Flags.getLzFVar2b() - 1)) & 1) != 0);
                        Register.setFlagBit(Register.FlagOF,
                                ((Flags.getLzFResb() ^ (Flags.getLzFResb() << 1)) & 0x80) != 0);
                    }
                    break;
                case 0x04:/* SHL and SAL are the same */
                case 0x06:
                    // SHLB
                    if (val == 0)
                        break;
                    Flags.setLzFVar1b(op1);
                    Flags.setLzFVar2b(val);
                    Flags.setLzFResb(Flags.getLzFVar1b() << Flags.getLzFVar2b());
                    if (blhs)
                        Register.Regs[regIdx].setByteL(Flags.getLzFResb());
                    else
                        Register.Regs[regIdx].setByteH(Flags.getLzFResb());
                    Flags.LzFlags.Type = Flags.TypeFlag.SHLb;
                    break;
                case 0x05:
                    // SHRB
                    if (val == 0)
                        break;
                    Flags.setLzFVar1b(op1);
                    Flags.setLzFVar2b(val);
                    Flags.setLzFResb(Flags.getLzFVar1b() >>> Flags.getLzFVar2b());
                    if (blhs)
                        Register.Regs[regIdx].setByteL(Flags.getLzFResb());
                    else
                        Register.Regs[regIdx].setByteH(Flags.getLzFResb());
                    Flags.LzFlags.Type = Flags.TypeFlag.SHRb;

                    break;
                case 0x07:
                    // SARB
                    if (val == 0)
                        break;
                    Flags.setLzFVar1b(op1);
                    Flags.setLzFVar2b(val);
                    if (Flags.getLzFVar2b() > 8)
                        Flags.setLzFVar2b(8);
                    if ((Flags.getLzFVar1b() & 0x80) != 0) {
                        Flags.setLzFResb((Flags.getLzFVar1b() >>> Flags.getLzFVar2b())
                                | (0xff << (8 - Flags.getLzFVar2b())));
                    } else {
                        Flags.setLzFResb(Flags.getLzFVar1b() >>> Flags.getLzFVar2b());
                    }
                    if (blhs)
                        Register.Regs[regIdx].setByteL(Flags.getLzFResb());
                    else
                        Register.Regs[regIdx].setByteH(Flags.getLzFResb());
                    Flags.LzFlags.Type = Flags.TypeFlag.SARb;

                    break;
            }
        } else {
            int op1 = Core.EATable[rm].get();
            int val = blah.run() & 0x1f;
            switch (which) {
                case 0x00:
                    // ROLB
                    if ((val & 0x7) == 0) {
                        if ((val & 0x18) != 0) {
                            Flags.fillFlagsNoCFOF();
                            Register.setFlagBit(Register.FlagCF, (op1 & 1) != 0);
                            Register.setFlagBit(Register.FlagOF, ((op1 & 1) ^ (op1 >>> 7)) != 0);
                        }
                        break;
                    }
                    Flags.fillFlagsNoCFOF();
                    Flags.setLzFVar1b(Memory.readB(op1));
                    Flags.setLzFVar2b(val & 0x07);
                    Flags.setLzFResb((Flags.getLzFVar1b() << Flags.getLzFVar2b())
                            | (Flags.getLzFVar1b() >>> (8 - Flags.getLzFVar2b())));
                    Memory.writeB(op1, Flags.getLzFResb());
                    Register.setFlagBit(Register.FlagCF, (Flags.getLzFResb() & 1) != 0);
                    Register.setFlagBit(Register.FlagOF,
                            ((Flags.getLzFResb() & 1) ^ (Flags.getLzFResb() >>> 7)) != 0);
                    break;
                case 0x01:
                    // RORB
                    if ((val & 0x7) == 0) {
                        if ((val & 0x18) != 0) {
                            Flags.fillFlagsNoCFOF();
                            Register.setFlagBit(Register.FlagCF, (op1 >>> 7) != 0);
                            Register.setFlagBit(Register.FlagOF,
                                    ((op1 >>> 7) ^ ((op1 >>> 6) & 1)) != 0);
                        }
                        break;
                    }
                    Flags.fillFlagsNoCFOF();
                    Flags.setLzFVar1b(Memory.readB(op1));
                    Flags.setLzFVar2b(val & 0x07);
                    Flags.setLzFResb((Flags.getLzFVar1b() >>> Flags.getLzFVar2b())
                            | (Flags.getLzFVar1b() << (8 - Flags.getLzFVar2b())));
                    Memory.writeB(op1, Flags.getLzFResb());
                    Register.setFlagBit(Register.FlagCF, (Flags.getLzFResb() & 0x80) != 0);
                    Register.setFlagBit(Register.FlagOF,
                            ((Flags.getLzFResb() ^ (Flags.getLzFResb() << 1)) & 0x80) != 0);

                    break;
                case 0x02:
                    // RCLB
                    if ((val % 9) == 0)
                        break; {
                    int cf = Flags.fillFlags() & 0x1;
                    Flags.setLzFVar1b(Memory.readB(op1));
                    Flags.setLzFVar2b(val % 9);
                    Flags.setLzFResb((Flags.getLzFVar1b() << Flags.getLzFVar2b())
                            | (cf << (Flags.getLzFVar2b() - 1))
                            | (Flags.getLzFVar1b() >>> (9 - Flags.getLzFVar2b())));
                    Memory.writeB(op1, Flags.getLzFResb());
                    Register.setFlagBit(Register.FlagCF,
                            ((Flags.getLzFVar1b() >>> (8 - Flags.getLzFVar2b())) & 1) != 0);
                    Register.setFlagBit(Register.FlagOF,
                            ((Register.Flags & 1) ^ (Flags.getLzFResb() >>> 7)) != 0);
                }
                    break;
                case 0x03:
                    // RCRB
                    if ((val % 9) != 0) {
                        int cf = Flags.fillFlags() & 0x1;
                        Flags.setLzFVar1b(Memory.readB(op1));
                        Flags.setLzFVar2b(val % 9);
                        Flags.setLzFResb((Flags.getLzFVar1b() >>> Flags.getLzFVar2b())
                                | (cf << (8 - Flags.getLzFVar2b()))
                                | (Flags.getLzFVar1b() << (9 - Flags.getLzFVar2b())));
                        Memory.writeB(op1, Flags.getLzFResb());
                        Register.setFlagBit(Register.FlagCF,
                                ((Flags.getLzFVar1b() >>> (Flags.getLzFVar2b() - 1)) & 1) != 0);
                        Register.setFlagBit(Register.FlagOF,
                                ((Flags.getLzFResb() ^ (Flags.getLzFResb() << 1)) & 0x80) != 0);
                    }
                    break;
                case 0x04:/* SHL and SAL are the same */
                case 0x06:
                    // SHLB
                    if (val == 0)
                        break;
                    Flags.setLzFVar1b(Memory.readB(op1));
                    Flags.setLzFVar2b(val);
                    Flags.setLzFResb(Flags.getLzFVar1b() << Flags.getLzFVar2b());
                    Memory.writeB(op1, Flags.getLzFResb());
                    Flags.LzFlags.Type = Flags.TypeFlag.SHLb;
                    break;
                case 0x05:
                    // SHRB
                    if (val == 0)
                        break;
                    Flags.setLzFVar1b(Memory.readB(op1));
                    Flags.setLzFVar2b(val);
                    Flags.setLzFResb(Flags.getLzFVar1b() >>> Flags.getLzFVar2b());
                    Memory.writeB(op1, Flags.getLzFResb());
                    Flags.LzFlags.Type = Flags.TypeFlag.SHRb;

                    break;
                case 0x07:
                    // SARB
                    if (val == 0)
                        break;
                    Flags.setLzFVar1b(Memory.readB(op1));
                    Flags.setLzFVar2b(val);
                    if (Flags.getLzFVar2b() > 8)
                        Flags.setLzFVar2b(8);
                    if ((Flags.getLzFVar1b() & 0x80) != 0) {
                        Flags.setLzFResb((Flags.getLzFVar1b() >>> Flags.getLzFVar2b())
                                | (0xff << (8 - Flags.getLzFVar2b())));
                    } else {
                        Flags.setLzFResb(Flags.getLzFVar1b() >>> Flags.getLzFVar2b());
                    }
                    Memory.writeB(op1, Flags.getLzFResb());
                    Flags.LzFlags.Type = Flags.TypeFlag.SARb;

                    break;
            }
        }
    }

    // public void GRP2B(byte blah) {
    public void GRP2B(int blah) {
        int rm = fetchB();
        int which = (rm >>> 3) & 7;

        if (rm >= 0xc0) {
            boolean blhs = lookupRMEAregbl[rm] >= 0;
            int regIdx = blhs ? lookupRMEAregbl[rm] : lookupRMEAregbh[rm];

            int op1 = blhs ? Register.Regs[regIdx].getByteL() : Register.Regs[regIdx].getByteH();

            int val = blah & 0x1f;
            switch (which) {
                case 0x00:
                    // ROLB
                    if ((val & 0x7) == 0) {
                        if ((val & 0x18) != 0) {
                            Flags.fillFlagsNoCFOF();
                            Register.setFlagBit(Register.FlagCF, (op1 & 1) != 0);
                            Register.setFlagBit(Register.FlagOF, ((op1 & 1) ^ (op1 >>> 7)) != 0);
                        }
                        break;
                    }
                    Flags.fillFlagsNoCFOF();
                    Flags.setLzFVar1b(op1);
                    Flags.setLzFVar2b(val & 0x07);
                    Flags.setLzFResb((Flags.getLzFVar1b() << Flags.getLzFVar2b())
                            | (Flags.getLzFVar1b() >>> (8 - Flags.getLzFVar2b())));
                    if (blhs)
                        Register.Regs[regIdx].setByteL(Flags.getLzFResb());
                    else
                        Register.Regs[regIdx].setByteH(Flags.getLzFResb());
                    Register.setFlagBit(Register.FlagCF, (Flags.getLzFResb() & 1) != 0);
                    Register.setFlagBit(Register.FlagOF,
                            ((Flags.getLzFResb() & 1) ^ (Flags.getLzFResb() >>> 7)) != 0);
                    break;
                case 0x01:
                    // RORB
                    if ((val & 0x7) == 0) {
                        if ((val & 0x18) != 0) {
                            Flags.fillFlagsNoCFOF();
                            Register.setFlagBit(Register.FlagCF, (op1 >>> 7) != 0);
                            Register.setFlagBit(Register.FlagOF,
                                    ((op1 >>> 7) ^ ((op1 >>> 6) & 1)) != 0);
                        }
                        break;
                    }
                    Flags.fillFlagsNoCFOF();
                    Flags.setLzFVar1b(op1);
                    Flags.setLzFVar2b(val & 0x07);
                    Flags.setLzFResb((Flags.getLzFVar1b() >>> Flags.getLzFVar2b())
                            | (Flags.getLzFVar1b() << (8 - Flags.getLzFVar2b())));
                    if (blhs)
                        Register.Regs[regIdx].setByteL(Flags.getLzFResb());
                    else
                        Register.Regs[regIdx].setByteH(Flags.getLzFResb());
                    Register.setFlagBit(Register.FlagCF, (Flags.getLzFResb() & 0x80) != 0);
                    Register.setFlagBit(Register.FlagOF,
                            ((Flags.getLzFResb() ^ (Flags.getLzFResb() << 1)) & 0x80) != 0);

                    break;
                case 0x02:
                    // RCLB
                    if ((val % 9) == 0)
                        break; {
                    int cf = Flags.fillFlags() & 0x1;
                    Flags.setLzFVar1b(op1);
                    Flags.setLzFVar2b(val % 9);
                    Flags.setLzFResb((Flags.getLzFVar1b() << Flags.getLzFVar2b())
                            | (cf << (Flags.getLzFVar2b() - 1))
                            | (Flags.getLzFVar1b() >>> (9 - Flags.getLzFVar2b())));
                    if (blhs)
                        Register.Regs[regIdx].setByteL(Flags.getLzFResb());
                    else
                        Register.Regs[regIdx].setByteH(Flags.getLzFResb());
                    Register.setFlagBit(Register.FlagCF,
                            ((Flags.getLzFVar1b() >>> (8 - Flags.getLzFVar2b())) & 1) != 0);
                    Register.setFlagBit(Register.FlagOF,
                            ((Register.Flags & 1) ^ (Flags.getLzFResb() >>> 7)) != 0);
                }
                    break;
                case 0x03:
                    // RCRB
                    if ((val % 9) != 0) {
                        int cf = Flags.fillFlags() & 0x1;
                        Flags.setLzFVar1b(op1);
                        Flags.setLzFVar2b(val % 9);
                        Flags.setLzFResb((Flags.getLzFVar1b() >>> Flags.getLzFVar2b())
                                | (cf << (8 - Flags.getLzFVar2b()))
                                | (Flags.getLzFVar1b() << (9 - Flags.getLzFVar2b())));
                        if (blhs)
                            Register.Regs[regIdx].setByteL(Flags.getLzFResb());
                        else
                            Register.Regs[regIdx].setByteH(Flags.getLzFResb());
                        Register.setFlagBit(Register.FlagCF,
                                ((Flags.getLzFVar1b() >>> (Flags.getLzFVar2b() - 1)) & 1) != 0);
                        Register.setFlagBit(Register.FlagOF,
                                ((Flags.getLzFResb() ^ (Flags.getLzFResb() << 1)) & 0x80) != 0);
                    }
                    break;
                case 0x04:/* SHL and SAL are the same */
                case 0x06:
                    // SHLB
                    if (val == 0)
                        break;
                    Flags.setLzFVar1b(op1);
                    Flags.setLzFVar2b(val);
                    Flags.setLzFResb(Flags.getLzFVar1b() << Flags.getLzFVar2b());
                    if (blhs)
                        Register.Regs[regIdx].setByteL(Flags.getLzFResb());
                    else
                        Register.Regs[regIdx].setByteH(Flags.getLzFResb());
                    Flags.LzFlags.Type = Flags.TypeFlag.SHLb;
                    break;
                case 0x05:
                    // SHRB
                    if (val == 0)
                        break;
                    Flags.setLzFVar1b(op1);
                    Flags.setLzFVar2b(val);
                    Flags.setLzFResb(Flags.getLzFVar1b() >>> Flags.getLzFVar2b());
                    if (blhs)
                        Register.Regs[regIdx].setByteL(Flags.getLzFResb());
                    else
                        Register.Regs[regIdx].setByteH(Flags.getLzFResb());
                    Flags.LzFlags.Type = Flags.TypeFlag.SHRb;

                    break;
                case 0x07:
                    // SARB
                    if (val == 0)
                        break;
                    Flags.setLzFVar1b(op1);
                    Flags.setLzFVar2b(val);
                    if (Flags.getLzFVar2b() > 8)
                        Flags.setLzFVar2b(8);
                    if ((Flags.getLzFVar1b() & 0x80) != 0) {
                        Flags.setLzFResb((Flags.getLzFVar1b() >>> Flags.getLzFVar2b())
                                | (0xff << (8 - Flags.getLzFVar2b())));
                    } else {
                        Flags.setLzFResb(Flags.getLzFVar1b() >>> Flags.getLzFVar2b());
                    }
                    if (blhs)
                        Register.Regs[regIdx].setByteL(Flags.getLzFResb());
                    else
                        Register.Regs[regIdx].setByteH(Flags.getLzFResb());
                    Flags.LzFlags.Type = Flags.TypeFlag.SARb;

                    break;
            }
        } else {
            int op1 = Core.EATable[rm].get();
            int val = blah & 0x1f;
            switch (which) {
                case 0x00:
                    // ROLB
                    if ((val & 0x7) == 0) {
                        if ((val & 0x18) != 0) {
                            Flags.fillFlagsNoCFOF();
                            Register.setFlagBit(Register.FlagCF, (op1 & 1) != 0);
                            Register.setFlagBit(Register.FlagOF, ((op1 & 1) ^ (op1 >>> 7)) != 0);
                        }
                        break;
                    }
                    Flags.fillFlagsNoCFOF();
                    Flags.setLzFVar1b(Memory.readB(op1));
                    Flags.setLzFVar2b(val & 0x07);
                    Flags.setLzFResb((Flags.getLzFVar1b() << Flags.getLzFVar2b())
                            | (Flags.getLzFVar1b() >>> (8 - Flags.getLzFVar2b())));
                    Memory.writeB(op1, Flags.getLzFResb());
                    Register.setFlagBit(Register.FlagCF, (Flags.getLzFResb() & 1) != 0);
                    Register.setFlagBit(Register.FlagOF,
                            ((Flags.getLzFResb() & 1) ^ (Flags.getLzFResb() >>> 7)) != 0);
                    break;
                case 0x01:
                    // RORB
                    if ((val & 0x7) == 0) {
                        if ((val & 0x18) != 0) {
                            Flags.fillFlagsNoCFOF();
                            Register.setFlagBit(Register.FlagCF, (op1 >>> 7) != 0);
                            Register.setFlagBit(Register.FlagOF,
                                    ((op1 >>> 7) ^ ((op1 >>> 6) & 1)) != 0);
                        }
                        break;
                    }
                    Flags.fillFlagsNoCFOF();
                    Flags.setLzFVar1b(Memory.readB(op1));
                    Flags.setLzFVar2b(val & 0x07);
                    Flags.setLzFResb((Flags.getLzFVar1b() >>> Flags.getLzFVar2b())
                            | (Flags.getLzFVar1b() << (8 - Flags.getLzFVar2b())));
                    Memory.writeB(op1, Flags.getLzFResb());
                    Register.setFlagBit(Register.FlagCF, (Flags.getLzFResb() & 0x80) != 0);
                    Register.setFlagBit(Register.FlagOF,
                            ((Flags.getLzFResb() ^ (Flags.getLzFResb() << 1)) & 0x80) != 0);

                    break;
                case 0x02:
                    // RCLB
                    if ((val % 9) == 0)
                        break; {
                    int cf = Flags.fillFlags() & 0x1;
                    Flags.setLzFVar1b(Memory.readB(op1));
                    Flags.setLzFVar2b(val % 9);
                    Flags.setLzFResb((Flags.getLzFVar1b() << Flags.getLzFVar2b())
                            | (cf << (Flags.getLzFVar2b() - 1))
                            | (Flags.getLzFVar1b() >>> (9 - Flags.getLzFVar2b())));
                    Memory.writeB(op1, Flags.getLzFResb());
                    Register.setFlagBit(Register.FlagCF,
                            ((Flags.getLzFVar1b() >>> (8 - Flags.getLzFVar2b())) & 1) != 0);
                    Register.setFlagBit(Register.FlagOF,
                            ((Register.Flags & 1) ^ (Flags.getLzFResb() >>> 7)) != 0);
                }
                    break;
                case 0x03:
                    // RCRB
                    if ((val % 9) != 0) {
                        int cf = Flags.fillFlags() & 0x1;
                        Flags.setLzFVar1b(Memory.readB(op1));
                        Flags.setLzFVar2b(val % 9);
                        Flags.setLzFResb((Flags.getLzFVar1b() >>> Flags.getLzFVar2b())
                                | (cf << (8 - Flags.getLzFVar2b()))
                                | (Flags.getLzFVar1b() << (9 - Flags.getLzFVar2b())));
                        Memory.writeB(op1, Flags.getLzFResb());
                        Register.setFlagBit(Register.FlagCF,
                                ((Flags.getLzFVar1b() >>> (Flags.getLzFVar2b() - 1)) & 1) != 0);
                        Register.setFlagBit(Register.FlagOF,
                                ((Flags.getLzFResb() ^ (Flags.getLzFResb() << 1)) & 0x80) != 0);
                    }
                    break;
                case 0x04:/* SHL and SAL are the same */
                case 0x06:
                    // SHLB
                    if (val == 0)
                        break;
                    Flags.setLzFVar1b(Memory.readB(op1));
                    Flags.setLzFVar2b(val);
                    Flags.setLzFResb(Flags.getLzFVar1b() << Flags.getLzFVar2b());
                    Memory.writeB(op1, Flags.getLzFResb());
                    Flags.LzFlags.Type = Flags.TypeFlag.SHLb;
                    break;
                case 0x05:
                    // SHRB
                    if (val == 0)
                        break;
                    Flags.setLzFVar1b(Memory.readB(op1));
                    Flags.setLzFVar2b(val);
                    Flags.setLzFResb(Flags.getLzFVar1b() >>> Flags.getLzFVar2b());
                    Memory.writeB(op1, Flags.getLzFResb());
                    Flags.LzFlags.Type = Flags.TypeFlag.SHRb;

                    break;
                case 0x07:
                    // SARB
                    if (val == 0)
                        break;
                    Flags.setLzFVar1b(Memory.readB(op1));
                    Flags.setLzFVar2b(val);
                    if (Flags.getLzFVar2b() > 8)
                        Flags.setLzFVar2b(8);
                    if ((Flags.getLzFVar1b() & 0x80) != 0) {
                        Flags.setLzFResb((Flags.getLzFVar1b() >>> Flags.getLzFVar2b())
                                | (0xff << (8 - Flags.getLzFVar2b())));
                    } else {
                        Flags.setLzFResb(Flags.getLzFVar1b() >>> Flags.getLzFVar2b());
                    }
                    Memory.writeB(op1, Flags.getLzFResb());
                    Flags.LzFlags.Type = Flags.TypeFlag.SARb;

                    break;
            }
        }
    }


    public void GRP2W(DOSFuncInt blah) {
        int rm = fetchB();
        int which = (rm >>> 3) & 7;
        if (rm >= 0xc0) {
            int regIdx = lookupRMEAregw[rm];

            int op1 = Register.Regs[regIdx].getWord();

            int val = blah.run() & 0x1f;
            switch (which) {
                case 0x00:
                    // ROLW
                    if ((val & 0xf) == 0) {
                        if ((val & 0x10) != 0) {
                            Flags.fillFlagsNoCFOF();
                            Register.setFlagBit(Register.FlagCF, (op1 & 1) != 0);
                            Register.setFlagBit(Register.FlagOF, ((op1 & 1) ^ (op1 >>> 15)) != 0);
                        }
                        break;
                    }
                    Flags.fillFlagsNoCFOF();
                    Flags.setLzFVar1w(op1);
                    Flags.setLzFVar2b(val & 0xf);
                    Flags.setLzFresw((Flags.getLzFVar1w() << Flags.getLzFVar2b())
                            | (Flags.getLzFVar1w() >>> (16 - Flags.getLzFVar2b())));
                    Register.Regs[regIdx].setWord(Flags.getLzFresw());
                    Register.setFlagBit(Register.FlagCF, (Flags.getLzFresw() & 1) != 0);
                    Register.setFlagBit(Register.FlagOF,
                            ((Flags.getLzFresw() & 1) ^ (Flags.getLzFresw() >>> 15)) != 0);

                    break;
                case 0x01:
                    // RORW
                    if ((val & 0xf) == 0) {
                        if ((val & 0x10) != 0) {
                            Flags.fillFlagsNoCFOF();
                            Register.setFlagBit(Register.FlagCF, (op1 >>> 15) != 0);
                            Register.setFlagBit(Register.FlagOF,
                                    ((op1 >>> 15) ^ ((op1 >>> 14) & 1)) != 0);
                        }
                        break;
                    }
                    Flags.fillFlagsNoCFOF();
                    Flags.setLzFVar1w(op1);
                    Flags.setLzFVar2b(val & 0xf);
                    Flags.setLzFresw((Flags.getLzFVar1w() >>> Flags.getLzFVar2b())
                            | (Flags.getLzFVar1w() << (16 - Flags.getLzFVar2b())));
                    Register.Regs[regIdx].setWord(Flags.getLzFresw());
                    Register.setFlagBit(Register.FlagCF, (Flags.getLzFresw() & 0x8000) != 0);
                    Register.setFlagBit(Register.FlagOF,
                            ((Flags.getLzFresw() ^ (Flags.getLzFresw() << 1)) & 0x8000) != 0);

                    break;
                case 0x02:
                    // RCLW
                    if ((val % 17) == 0)
                        break; {
                    int cf = Flags.fillFlags() & 0x1;
                    Flags.setLzFVar1w(op1);
                    Flags.setLzFVar2b(val % 17);
                    Flags.setLzFresw(0xffff & ((Flags.getLzFVar1w() << Flags.getLzFVar2b())
                            | (cf << (Flags.getLzFVar2b() - 1))
                            | (Flags.getLzFVar1w() >>> (17 - Flags.getLzFVar2b()))));
                    Register.Regs[regIdx].setWord(Flags.getLzFresw());
                    Register.setFlagBit(Register.FlagCF,
                            ((Flags.getLzFVar1w() >>> (16 - Flags.getLzFVar2b())) & 1) != 0);
                    Register.setFlagBit(Register.FlagOF,
                            ((Register.Flags & 1) ^ (Flags.getLzFresw() >>> 15)) != 0);
                }
                    break;
                case 0x03:
                    // RCRW
                    if ((val % 17) != 0) {
                        int cf = Flags.fillFlags() & 0x1;
                        Flags.setLzFVar1w(op1);
                        Flags.setLzFVar2b(val % 17);
                        Flags.setLzFresw(0xffff & ((Flags.getLzFVar1w() >>> Flags.getLzFVar2b())
                                | (cf << (16 - Flags.getLzFVar2b()))
                                | (Flags.getLzFVar1w() << (17 - Flags.getLzFVar2b()))));
                        Register.Regs[regIdx].setWord(Flags.getLzFresw());
                        Register.setFlagBit(Register.FlagCF,
                                ((Flags.getLzFVar1w() >>> (Flags.getLzFVar2b() - 1)) & 1) != 0);
                        Register.setFlagBit(Register.FlagOF,
                                ((Flags.getLzFresw() ^ (Flags.getLzFresw() << 1)) & 0x8000) != 0);
                    }

                    break;
                case 0x04:/* SHL and SAL are the same */
                case 0x06:
                    // SHLW
                    if (val == 0)
                        break;
                    Flags.setLzFVar1w(op1);
                    Flags.setLzFVar2b(val);
                    Flags.setLzFresw(0xffff & (Flags.getLzFVar1w() << Flags.getLzFVar2b()));
                    Register.Regs[regIdx].setWord(Flags.getLzFresw());
                    Flags.LzFlags.Type = Flags.TypeFlag.SHLw;
                    break;
                case 0x05:
                    // SHRW
                    if (val == 0)
                        break;
                    Flags.setLzFVar1w(op1);
                    Flags.setLzFVar2b(val);
                    Flags.setLzFresw(0xffff & (Flags.getLzFVar1w() >>> Flags.getLzFVar2b()));
                    Register.Regs[regIdx].setWord(Flags.getLzFresw());
                    Flags.LzFlags.Type = Flags.TypeFlag.SHRw;

                    break;
                case 0x07:
                    // SARW
                    if (val == 0)
                        break;
                    Flags.setLzFVar1w(op1);
                    Flags.setLzFVar2b(val);
                    if (Flags.getLzFVar2b() > 16)
                        Flags.setLzFVar2b(16);
                    if ((Flags.getLzFVar1w() & 0x8000) != 0) {
                        Flags.setLzFresw(0xffff & ((Flags.getLzFVar1w() >>> Flags.getLzFVar2b())
                                | (0xffff << (16 - Flags.getLzFVar2b()))));
                    } else {
                        Flags.setLzFresw(0xffff & (Flags.getLzFVar1w() >>> Flags.getLzFVar2b()));
                    }
                    Register.Regs[regIdx].setWord(Flags.getLzFresw());
                    Flags.LzFlags.Type = Flags.TypeFlag.SARw;

                    break;
            }
        } else {
            int op1 = Core.EATable[rm].get();
            int val = blah.run() & 0x1f;
            switch (which) {
                case 0x00:
                    // ROLW
                    if ((val & 0xf) == 0) {
                        if ((val & 0x10) != 0) {
                            Flags.fillFlagsNoCFOF();
                            Register.setFlagBit(Register.FlagCF, (op1 & 1) != 0);
                            Register.setFlagBit(Register.FlagOF, ((op1 & 1) ^ (op1 >>> 15)) != 0);
                        }
                        break;
                    }
                    Flags.fillFlagsNoCFOF();
                    Flags.setLzFVar1w(Memory.readW(op1));
                    Flags.setLzFVar2b(val & 0xf);
                    Flags.setLzFresw((Flags.getLzFVar1w() << Flags.getLzFVar2b())
                            | (Flags.getLzFVar1w() >>> (16 - Flags.getLzFVar2b())));
                    Memory.writeW(op1, Flags.getLzFresw());
                    Register.setFlagBit(Register.FlagCF, (Flags.getLzFresw() & 1) != 0);
                    Register.setFlagBit(Register.FlagOF,
                            ((Flags.getLzFresw() & 1) ^ (Flags.getLzFresw() >>> 15)) != 0);

                    break;
                case 0x01:
                    // RORW
                    if ((val & 0xf) == 0) {
                        if ((val & 0x10) != 0) {
                            Flags.fillFlagsNoCFOF();
                            Register.setFlagBit(Register.FlagCF, (op1 >>> 15) != 0);
                            Register.setFlagBit(Register.FlagOF,
                                    ((op1 >>> 15) ^ ((op1 >>> 14) & 1)) != 0);
                        }
                        break;
                    }
                    Flags.fillFlagsNoCFOF();
                    Flags.setLzFVar1w(Memory.readW(op1));
                    Flags.setLzFVar2b(val & 0xf);
                    Flags.setLzFresw(0xffff & ((Flags.getLzFVar1w() >>> Flags.getLzFVar2b())
                            | (Flags.getLzFVar1w() << (16 - Flags.getLzFVar2b()))));
                    Memory.writeW(op1, Flags.getLzFresw());
                    Register.setFlagBit(Register.FlagCF, (Flags.getLzFresw() & 0x8000) != 0);
                    Register.setFlagBit(Register.FlagOF,
                            ((Flags.getLzFresw() ^ (Flags.getLzFresw() << 1)) & 0x8000) != 0);

                    break;
                case 0x02:
                    // RCLW
                    if ((val % 17) == 0)
                        break; {
                    int cf = Flags.fillFlags() & 0x1;
                    Flags.setLzFVar1w(Memory.readW(op1));
                    Flags.setLzFVar2b(val % 17);
                    Flags.setLzFresw(0xffff & ((Flags.getLzFVar1w() << Flags.getLzFVar2b())
                            | (cf << (Flags.getLzFVar2b() - 1))
                            | (Flags.getLzFVar1w() >>> (17 - Flags.getLzFVar2b()))));
                    Memory.writeW(op1, Flags.getLzFresw());
                    Register.setFlagBit(Register.FlagCF,
                            ((Flags.getLzFVar1w() >>> (16 - Flags.getLzFVar2b())) & 1) != 0);
                    Register.setFlagBit(Register.FlagOF,
                            ((Register.Flags & 1) ^ (Flags.getLzFresw() >>> 15)) != 0);
                }
                    break;
                case 0x03:
                    // RCRW
                    if ((val % 17) != 0) {
                        int cf = Flags.fillFlags() & 0x1;
                        Flags.setLzFVar1w(Memory.readW(op1));
                        Flags.setLzFVar2b(val % 17);
                        Flags.setLzFresw(0xffff & ((Flags.getLzFVar1w() >>> Flags.getLzFVar2b())
                                | (cf << (16 - Flags.getLzFVar2b()))
                                | (Flags.getLzFVar1w() << (17 - Flags.getLzFVar2b()))));
                        Memory.writeW(op1, Flags.getLzFresw());
                        Register.setFlagBit(Register.FlagCF,
                                ((Flags.getLzFVar1w() >>> (Flags.getLzFVar2b() - 1)) & 1) != 0);
                        Register.setFlagBit(Register.FlagOF,
                                ((Flags.getLzFresw() ^ (Flags.getLzFresw() << 1)) & 0x8000) != 0);
                    }
                    break;
                case 0x04:/* SHL and SAL are the same */
                case 0x06:
                    // SHLW
                    if (val == 0)
                        break;
                    Flags.setLzFVar1w(Memory.readW(op1));
                    Flags.setLzFVar2b(val);
                    Flags.setLzFresw(0xffff & (Flags.getLzFVar1w() << Flags.getLzFVar2b()));
                    Memory.writeW(op1, Flags.getLzFresw());
                    Flags.LzFlags.Type = Flags.TypeFlag.SHLw;
                    break;
                case 0x05:
                    // SHRW
                    if (val == 0)
                        break;
                    Flags.setLzFVar1w(Memory.readW(op1));
                    Flags.setLzFVar2b(val);
                    Flags.setLzFresw(0xffff & (Flags.getLzFVar1w() >>> Flags.getLzFVar2b()));
                    Memory.writeW(op1, Flags.getLzFresw());
                    Flags.LzFlags.Type = Flags.TypeFlag.SHRw;

                    break;
                case 0x07:
                    // SARW
                    if (val == 0)
                        break;
                    Flags.setLzFVar1w(Memory.readW(op1));
                    Flags.setLzFVar2b(val);
                    if (Flags.getLzFVar2b() > 16)
                        Flags.setLzFVar2b(16);
                    if ((Flags.getLzFVar1w() & 0x8000) != 0) {
                        Flags.setLzFresw(0xffff & ((Flags.getLzFVar1w() >>> Flags.getLzFVar2b())
                                | (0xffff << (16 - Flags.getLzFVar2b()))));
                    } else {
                        Flags.setLzFresw(0xffff & (Flags.getLzFVar1w() >>> Flags.getLzFVar2b()));
                    }
                    Memory.writeW(op1, Flags.getLzFresw());
                    Flags.LzFlags.Type = Flags.TypeFlag.SARw;

                    break;
            }
        }
    }

    // public void GRP2W(byte blah) {
    public void GRP2W(int blah) {
        int rm = fetchB();
        int which = (rm >>> 3) & 7;
        if (rm >= 0xc0) {
            int regIdx = lookupRMEAregw[rm];

            int op1 = Register.Regs[regIdx].getWord();

            int val = blah & 0x1f;
            switch (which) {
                case 0x00:
                    // ROLW
                    if ((val & 0xf) == 0) {
                        if ((val & 0x10) != 0) {
                            Flags.fillFlagsNoCFOF();
                            Register.setFlagBit(Register.FlagCF, (op1 & 1) != 0);
                            Register.setFlagBit(Register.FlagOF, ((op1 & 1) ^ (op1 >>> 15)) != 0);
                        }
                        break;
                    }
                    Flags.fillFlagsNoCFOF();
                    Flags.setLzFVar1w(op1);
                    Flags.setLzFVar2b(val & 0xf);
                    Flags.setLzFresw((Flags.getLzFVar1w() << Flags.getLzFVar2b())
                            | (Flags.getLzFVar1w() >>> (16 - Flags.getLzFVar2b())));
                    Register.Regs[regIdx].setWord(Flags.getLzFresw());
                    Register.setFlagBit(Register.FlagCF, (Flags.getLzFresw() & 1) != 0);
                    Register.setFlagBit(Register.FlagOF,
                            ((Flags.getLzFresw() & 1) ^ (Flags.getLzFresw() >>> 15)) != 0);

                    break;
                case 0x01:
                    // RORW
                    if ((val & 0xf) == 0) {
                        if ((val & 0x10) != 0) {
                            Flags.fillFlagsNoCFOF();
                            Register.setFlagBit(Register.FlagCF, (op1 >>> 15) != 0);
                            Register.setFlagBit(Register.FlagOF,
                                    ((op1 >>> 15) ^ ((op1 >>> 14) & 1)) != 0);
                        }
                        break;
                    }
                    Flags.fillFlagsNoCFOF();
                    Flags.setLzFVar1w(0xffff & op1);
                    Flags.setLzFVar2b(val & 0xf);
                    Flags.setLzFresw(0xffff & ((Flags.getLzFVar1w() >>> Flags.getLzFVar2b())
                            | (Flags.getLzFVar1w() << (16 - Flags.getLzFVar2b()))));
                    Register.Regs[regIdx].setWord(Flags.getLzFresw());
                    Register.setFlagBit(Register.FlagCF, (Flags.getLzFresw() & 0x8000) != 0);
                    Register.setFlagBit(Register.FlagOF,
                            ((Flags.getLzFresw() ^ (Flags.getLzFresw() << 1)) & 0x8000) != 0);

                    break;
                case 0x02:
                    // RCLW
                    if ((val % 17) == 0)
                        break; {
                    int cf = Flags.fillFlags() & 0x1;
                    Flags.setLzFVar1w(op1);
                    Flags.setLzFVar2b(val % 17);
                    Flags.setLzFresw(0xffff & ((Flags.getLzFVar1w() << Flags.getLzFVar2b())
                            | (cf << (Flags.getLzFVar2b() - 1))
                            | (Flags.getLzFVar1w() >>> (17 - Flags.getLzFVar2b()))));
                    Register.Regs[regIdx].setWord(Flags.getLzFresw());
                    Register.setFlagBit(Register.FlagCF,
                            ((Flags.getLzFVar1w() >>> (16 - Flags.getLzFVar2b())) & 1) != 0);
                    Register.setFlagBit(Register.FlagOF,
                            ((Register.Flags & 1) ^ (Flags.getLzFresw() >>> 15)) != 0);
                }
                    break;
                case 0x03:
                    // RCRW
                    if ((val % 17) != 0) {
                        int cf = Flags.fillFlags() & 0x1;
                        Flags.setLzFVar1w(op1);
                        Flags.setLzFVar2b(val % 17);
                        Flags.setLzFresw(0xffff & ((Flags.getLzFVar1w() >>> Flags.getLzFVar2b())
                                | (cf << (16 - Flags.getLzFVar2b()))
                                | (Flags.getLzFVar1w() << (17 - Flags.getLzFVar2b()))));
                        Register.Regs[regIdx].setWord(Flags.getLzFresw());
                        Register.setFlagBit(Register.FlagCF,
                                ((Flags.getLzFVar1w() >>> (Flags.getLzFVar2b() - 1)) & 1) != 0);
                        Register.setFlagBit(Register.FlagOF,
                                ((Flags.getLzFresw() ^ (Flags.getLzFresw() << 1)) & 0x8000) != 0);
                    }

                    break;
                case 0x04:/* SHL and SAL are the same */
                case 0x06:
                    // SHLW
                    if (val == 0)
                        break;
                    Flags.setLzFVar1w(op1);
                    Flags.setLzFVar2b(val);
                    Flags.setLzFresw(0xffff & (Flags.getLzFVar1w() << Flags.getLzFVar2b()));
                    Register.Regs[regIdx].setWord(Flags.getLzFresw());
                    Flags.LzFlags.Type = Flags.TypeFlag.SHLw;
                    break;
                case 0x05:
                    // SHRW
                    if (val == 0)
                        break;
                    Flags.setLzFVar1w(op1);
                    Flags.setLzFVar2b(val);
                    Flags.setLzFresw(0xffff & (Flags.getLzFVar1w() >>> Flags.getLzFVar2b()));
                    Register.Regs[regIdx].setWord(Flags.getLzFresw());
                    Flags.LzFlags.Type = Flags.TypeFlag.SHRw;

                    break;
                case 0x07:
                    // SARW
                    if (val == 0)
                        break;
                    Flags.setLzFVar1w(op1);
                    Flags.setLzFVar2b(val);
                    if (Flags.getLzFVar2b() > 16)
                        Flags.setLzFVar2b(16);
                    if ((Flags.getLzFVar1w() & 0x8000) != 0) {
                        Flags.setLzFresw(0xffff & ((Flags.getLzFVar1w() >>> Flags.getLzFVar2b())
                                | (0xffff << (16 - Flags.getLzFVar2b()))));
                    } else {
                        Flags.setLzFresw(0xffff & (Flags.getLzFVar1w() >>> Flags.getLzFVar2b()));
                    }
                    Register.Regs[regIdx].setWord(Flags.getLzFresw());
                    Flags.LzFlags.Type = Flags.TypeFlag.SARw;

                    break;
            }
        } else {
            int op1 = Core.EATable[rm].get();
            int val = blah & 0x1f;
            switch (which) {
                case 0x00:
                    // ROLW
                    if ((val & 0xf) == 0) {
                        if ((val & 0x10) != 0) {
                            Flags.fillFlagsNoCFOF();
                            Register.setFlagBit(Register.FlagCF, (op1 & 1) != 0);
                            Register.setFlagBit(Register.FlagOF, ((op1 & 1) ^ (op1 >>> 15)) != 0);
                        }
                        break;
                    }
                    Flags.fillFlagsNoCFOF();
                    Flags.setLzFVar1w(Memory.readW(op1));
                    Flags.setLzFVar2b(val & 0xf);
                    Flags.setLzFresw((Flags.getLzFVar1w() << Flags.getLzFVar2b())
                            | (Flags.getLzFVar1w() >>> (16 - Flags.getLzFVar2b())));
                    Memory.writeW(op1, Flags.getLzFresw());
                    Register.setFlagBit(Register.FlagCF, (Flags.getLzFresw() & 1) != 0);
                    Register.setFlagBit(Register.FlagOF,
                            ((Flags.getLzFresw() & 1) ^ (Flags.getLzFresw() >>> 15)) != 0);

                    break;
                case 0x01:
                    // RORW
                    if ((val & 0xf) == 0) {
                        if ((val & 0x10) != 0) {
                            Flags.fillFlagsNoCFOF();
                            Register.setFlagBit(Register.FlagCF, (op1 >>> 15) != 0);
                            Register.setFlagBit(Register.FlagOF,
                                    ((op1 >>> 15) ^ ((op1 >>> 14) & 1)) != 0);
                        }
                        break;
                    }
                    Flags.fillFlagsNoCFOF();
                    Flags.setLzFVar1w(Memory.readW(op1));
                    Flags.setLzFVar2b(val & 0xf);
                    Flags.setLzFresw(0xffff & ((Flags.getLzFVar1w() >>> Flags.getLzFVar2b())
                            | (Flags.getLzFVar1w() << (16 - Flags.getLzFVar2b()))));
                    Memory.writeW(op1, Flags.getLzFresw());
                    Register.setFlagBit(Register.FlagCF, (Flags.getLzFresw() & 0x8000) != 0);
                    Register.setFlagBit(Register.FlagOF,
                            ((Flags.getLzFresw() ^ (Flags.getLzFresw() << 1)) & 0x8000) != 0);

                    break;
                case 0x02:
                    // RCLW
                    if ((val % 17) == 0)
                        break; {
                    int cf = Flags.fillFlags() & 0x1;
                    Flags.setLzFVar1w(Memory.readW(op1));
                    Flags.setLzFVar2b(val % 17);
                    Flags.setLzFresw(0xffff & ((Flags.getLzFVar1w() << Flags.getLzFVar2b())
                            | (cf << (Flags.getLzFVar2b() - 1))
                            | (Flags.getLzFVar1w() >>> (17 - Flags.getLzFVar2b()))));
                    Memory.writeW(op1, Flags.getLzFresw());
                    Register.setFlagBit(Register.FlagCF,
                            ((Flags.getLzFVar1w() >>> (16 - Flags.getLzFVar2b())) & 1) != 0);
                    Register.setFlagBit(Register.FlagOF,
                            ((Register.Flags & 1) ^ (Flags.getLzFresw() >>> 15)) != 0);
                }
                    break;
                case 0x03:
                    // RCRW
                    if ((val % 17) != 0) {
                        int cf = Flags.fillFlags() & 0x1;
                        Flags.setLzFVar1w(Memory.readW(op1));
                        Flags.setLzFVar2b(val % 17);
                        Flags.setLzFresw(0xffff & ((Flags.getLzFVar1w() >>> Flags.getLzFVar2b())
                                | (cf << (16 - Flags.getLzFVar2b()))
                                | (Flags.getLzFVar1w() << (17 - Flags.getLzFVar2b()))));
                        Memory.writeW(op1, Flags.getLzFresw());
                        Register.setFlagBit(Register.FlagCF,
                                ((Flags.getLzFVar1w() >>> (Flags.getLzFVar2b() - 1)) & 1) != 0);
                        Register.setFlagBit(Register.FlagOF,
                                ((Flags.getLzFresw() ^ (Flags.getLzFresw() << 1)) & 0x8000) != 0);
                    }
                    break;
                case 0x04:/* SHL and SAL are the same */
                case 0x06:
                    // SHLW
                    if (val == 0)
                        break;
                    Flags.setLzFVar1w(Memory.readW(op1));
                    Flags.setLzFVar2b(val);
                    Flags.setLzFresw(0xffff & (Flags.getLzFVar1w() << Flags.getLzFVar2b()));
                    Memory.writeW(op1, Flags.getLzFresw());
                    Flags.LzFlags.Type = Flags.TypeFlag.SHLw;
                    break;
                case 0x05:
                    // SHRW
                    if (val == 0)
                        break;
                    Flags.setLzFVar1w(Memory.readW(op1));
                    Flags.setLzFVar2b(val);
                    Flags.setLzFresw(0xffff & (Flags.getLzFVar1w() >>> Flags.getLzFVar2b()));
                    Memory.writeW(op1, Flags.getLzFresw());
                    Flags.LzFlags.Type = Flags.TypeFlag.SHRw;

                    break;
                case 0x07:
                    // SARW
                    if (val == 0)
                        break;
                    Flags.setLzFVar1w(Memory.readW(op1));
                    Flags.setLzFVar2b(val);
                    if (Flags.getLzFVar2b() > 16)
                        Flags.setLzFVar2b(16);
                    if ((Flags.getLzFVar1w() & 0x8000) != 0) {
                        Flags.setLzFresw(0xffff & ((Flags.getLzFVar1w() >>> Flags.getLzFVar2b())
                                | (0xffff << (16 - Flags.getLzFVar2b()))));
                    } else {
                        Flags.setLzFresw(0xffff & (Flags.getLzFVar1w() >>> Flags.getLzFVar2b()));
                    }
                    Memory.writeW(op1, Flags.getLzFresw());
                    Flags.LzFlags.Type = Flags.TypeFlag.SARw;

                    break;
            }
        }
    }

    public void GRP2D(DOSFuncInt blah) {
        int rm = fetchB();
        int which = (rm >>> 3) & 7;
        if (rm >= 0xc0) {
            int regIdx = lookupRMEAregd[rm];
            int op1 = Register.Regs[regIdx].getDWord();


            int val = blah.run() & 0x1f;
            switch (which) {
                case 0x00:
                    // ROLD
                    if (val == 0)
                        break;
                    Flags.fillFlagsNoCFOF();
                    Flags.setLzFVar1d(op1);
                    Flags.setLzFVar2b(val);
                    Flags.setLzFResd((Flags.getLzFVar1d() << Flags.getLzFVar2b())
                            | (Flags.getLzFVar1d() >>> (32 - Flags.getLzFVar2b())));
                    Register.Regs[regIdx].setDWord(Flags.getLzFResd());
                    Register.setFlagBit(Register.FlagCF, Flags.getLzFResd() & 1);
                    Register.setFlagBit(Register.FlagOF,
                            (Flags.getLzFResd() & 1) ^ (Flags.getLzFResd() >>> 31));

                    break;
                case 0x01:
                    // RORD
                    if (val == 0)
                        break;
                    Flags.fillFlagsNoCFOF();
                    Flags.setLzFVar1d(op1);
                    Flags.setLzFVar2b(val);
                    Flags.setLzFResd((Flags.getLzFVar1d() >>> Flags.getLzFVar2b())
                            | (Flags.getLzFVar1d() << (32 - Flags.getLzFVar2b())));
                    Register.Regs[regIdx].setDWord(Flags.getLzFResd());
                    Register.setFlagBit(Register.FlagCF, Flags.getLzFResd() & 0x80000000);
                    Register.setFlagBit(Register.FlagOF,
                            (Flags.getLzFResd() ^ (Flags.getLzFResd() << 1)) & 0x80000000);

                    break;
                case 0x02:
                    // RCLD
                    if (val == 0)
                        break; {
                    int cf = Flags.fillFlags() & 0x1;
                    Flags.setLzFVar1d(op1);
                    Flags.setLzFVar2b(val);
                    if (Flags.getLzFVar2b() == 1) {
                        Flags.setLzFResd((Flags.getLzFVar1d() << 1) | cf);
                    } else {
                        Flags.setLzFResd((Flags.getLzFVar1d() << Flags.getLzFVar2b())
                                | (cf << (Flags.getLzFVar2b() - 1))
                                | (Flags.getLzFVar1d() >>> (33 - Flags.getLzFVar2b())));
                    }
                    Register.Regs[regIdx].setDWord(Flags.getLzFResd());
                    Register.setFlagBit(Register.FlagCF,
                            ((Flags.getLzFVar1d() >>> (32 - Flags.getLzFVar2b())) & 1));
                    Register.setFlagBit(Register.FlagOF,
                            (Register.Flags & 1) ^ (Flags.getLzFResd() >>> 31));
                }
                    break;
                case 0x03:
                    // RCRD
                    if (val != 0) {
                        int cf = Flags.fillFlags() & 0x1;
                        Flags.setLzFVar1d(op1);
                        Flags.setLzFVar2b(val);
                        if (Flags.getLzFVar2b() == 1) {
                            Flags.setLzFResd(Flags.getLzFVar1d() >>> 1 | cf << 31);
                        } else {
                            Flags.setLzFResd((Flags.getLzFVar1d() >>> Flags.getLzFVar2b())
                                    | (cf << (32 - Flags.getLzFVar2b()))
                                    | (Flags.getLzFVar1d() << (33 - Flags.getLzFVar2b())));
                        }
                        Register.Regs[regIdx].setDWord(Flags.getLzFResd());
                        Register.setFlagBit(Register.FlagCF,
                                (Flags.getLzFVar1d() >>> (Flags.getLzFVar2b() - 1)) & 1);
                        Register.setFlagBit(Register.FlagOF,
                                (Flags.getLzFResd() ^ (Flags.getLzFResd() << 1)) & 0x80000000);
                    }
                    break;
                case 0x04:/* SHL and SAL are the same */
                case 0x06:
                    // SHLD
                    if (val == 0)
                        break;
                    Flags.setLzFVar1d(op1);
                    Flags.setLzFVar2b(val);
                    Flags.setLzFResd(Flags.getLzFVar1d() << Flags.getLzFVar2b());
                    Register.Regs[regIdx].setDWord(Flags.getLzFResd());
                    Flags.LzFlags.Type = Flags.TypeFlag.SHLd;

                    break;
                case 0x05:
                    // SHRD
                    if (val == 0)
                        break;
                    Flags.setLzFVar1d(op1);
                    Flags.setLzFVar2b(val);
                    Flags.setLzFResd(Flags.getLzFVar1d() >>> Flags.getLzFVar2b());
                    Register.Regs[regIdx].setDWord(Flags.getLzFResd());
                    Flags.LzFlags.Type = Flags.TypeFlag.SHRd;

                    break;
                case 0x07:
                    // SARD
                    if (val == 0)
                        break;
                    Flags.setLzFVar2b(val);
                    Flags.setLzFVar1d(op1);
                    if ((Flags.getLzFVar1d() & 0x80000000) != 0) {
                        Flags.setLzFResd((Flags.getLzFVar1d() >>> Flags.getLzFVar2b())
                                | (0xffffffff << (32 - Flags.getLzFVar2b())));
                    } else {
                        Flags.setLzFResd(Flags.getLzFVar1d() >>> Flags.getLzFVar2b());
                    }
                    Register.Regs[regIdx].setDWord(Flags.getLzFResd());
                    Flags.LzFlags.Type = Flags.TypeFlag.SARd;

                    break;
            }
        } else {
            int op1 = Core.EATable[rm].get();
            int val = blah.run() & 0x1f;
            switch (which) {
                case 0x00:
                    // ROLD
                    if (val == 0)
                        break;
                    Flags.fillFlagsNoCFOF();
                    Flags.setLzFVar1d(Memory.readD(op1));
                    Flags.setLzFVar2b(val);
                    Flags.setLzFResd((Flags.getLzFVar1d() << Flags.getLzFVar2b())
                            | (Flags.getLzFVar1d() >>> (32 - Flags.getLzFVar2b())));
                    Memory.writeD(op1, Flags.getLzFResd());
                    Register.setFlagBit(Register.FlagCF, Flags.getLzFResd() & 1);
                    Register.setFlagBit(Register.FlagOF,
                            (Flags.getLzFResd() & 1) ^ (Flags.getLzFResd() >>> 31));

                    break;
                case 0x01:
                    // RORD
                    if (val == 0)
                        break;
                    Flags.fillFlagsNoCFOF();
                    Flags.setLzFVar1d(Memory.readD(op1));
                    Flags.setLzFVar2b(val);
                    Flags.setLzFResd((Flags.getLzFVar1d() >>> Flags.getLzFVar2b())
                            | (Flags.getLzFVar1d() << (32 - Flags.getLzFVar2b())));
                    Memory.writeD(op1, Flags.getLzFResd());
                    Register.setFlagBit(Register.FlagCF, Flags.getLzFResd() & 0x80000000);
                    Register.setFlagBit(Register.FlagOF,
                            (Flags.getLzFResd() ^ (Flags.getLzFResd() << 1)) & 0x80000000);

                    break;
                case 0x02:
                    // RCLD
                    if (val == 0)
                        break; {
                    int cf = Flags.fillFlags() & 0x1;
                    Flags.setLzFVar1d(Memory.readD(op1));
                    Flags.setLzFVar2b(val);
                    if (Flags.getLzFVar2b() == 1) {
                        Flags.setLzFResd((Flags.getLzFVar1d() << 1) | cf);
                    } else {
                        Flags.setLzFResd((Flags.getLzFVar1d() << Flags.getLzFVar2b())
                                | (cf << (Flags.getLzFVar2b() - 1))
                                | (Flags.getLzFVar1d() >>> (33 - Flags.getLzFVar2b())));
                    }
                    Memory.writeD(op1, Flags.getLzFResd());
                    Register.setFlagBit(Register.FlagCF,
                            ((Flags.getLzFVar1d() >>> (32 - Flags.getLzFVar2b())) & 1));
                    Register.setFlagBit(Register.FlagOF,
                            (Register.Flags & 1) ^ (Flags.getLzFResd() >>> 31));
                }
                    break;
                case 0x03:
                    // RCRD
                    if (val != 0) {
                        int cf = Flags.fillFlags() & 0x1;
                        Flags.setLzFVar1d(Memory.readD(op1));
                        Flags.setLzFVar2b(val);
                        if (Flags.getLzFVar2b() == 1) {
                            Flags.setLzFResd(Flags.getLzFVar1d() >>> 1 | cf << 31);
                        } else {
                            Flags.setLzFResd((Flags.getLzFVar1d() >>> Flags.getLzFVar2b())
                                    | (cf << (32 - Flags.getLzFVar2b()))
                                    | (Flags.getLzFVar1d() << (33 - Flags.getLzFVar2b())));
                        }
                        Memory.writeD(op1, Flags.getLzFResd());
                        Register.setFlagBit(Register.FlagCF,
                                (Flags.getLzFVar1d() >>> (Flags.getLzFVar2b() - 1)) & 1);
                        Register.setFlagBit(Register.FlagOF,
                                (Flags.getLzFResd() ^ (Flags.getLzFResd() << 1)) & 0x80000000);
                    }
                    break;
                case 0x04:/* SHL and SAL are the same */
                case 0x06:
                    // SHLD
                    if (val == 0)
                        break;
                    Flags.setLzFVar1d(Memory.readD(op1));
                    Flags.setLzFVar2b(val);
                    Flags.setLzFResd(Flags.getLzFVar1d() << Flags.getLzFVar2b());
                    Memory.writeD(op1, Flags.getLzFResd());
                    Flags.LzFlags.Type = Flags.TypeFlag.SHLd;

                    break;
                case 0x05:
                    // SHRD
                    if (val == 0)
                        break;
                    Flags.setLzFVar1d(Memory.readD(op1));
                    Flags.setLzFVar2b(val);
                    Flags.setLzFResd(Flags.getLzFVar1d() >>> Flags.getLzFVar2b());
                    Memory.writeD(op1, Flags.getLzFResd());
                    Flags.LzFlags.Type = Flags.TypeFlag.SHRd;

                    break;
                case 0x07:
                    // SARD
                    if (val == 0)
                        break;
                    Flags.setLzFVar2b(val);
                    Flags.setLzFVar1d(Memory.readD(op1));
                    if ((Flags.getLzFVar1d() & 0x80000000) != 0) {
                        Flags.setLzFResd((Flags.getLzFVar1d() >>> Flags.getLzFVar2b())
                                | (0xffffffff << (32 - Flags.getLzFVar2b())));
                    } else {
                        Flags.setLzFResd(Flags.getLzFVar1d() >>> Flags.getLzFVar2b());
                    }
                    Memory.writeD(op1, Flags.getLzFResd());
                    Flags.LzFlags.Type = Flags.TypeFlag.SARd;

                    break;
            }
        }
    }

    // public void GRP2D(byte blah) {
    public void GRP2D(int blah) {
        int rm = fetchB();
        int which = (rm >>> 3) & 7;
        if (rm >= 0xc0) {
            int regIdx = lookupRMEAregd[rm];
            int op1 = Register.Regs[regIdx].getDWord();


            int val = blah & 0x1f;
            switch (which) {
                case 0x00:
                    // ROLD
                    if (val == 0)
                        break;
                    Flags.fillFlagsNoCFOF();
                    Flags.setLzFVar1d(op1);
                    Flags.setLzFVar2b(val);
                    Flags.setLzFResd((Flags.getLzFVar1d() << Flags.getLzFVar2b())
                            | (Flags.getLzFVar1d() >>> (32 - Flags.getLzFVar2b())));
                    Register.Regs[regIdx].setDWord(Flags.getLzFResd());
                    Register.setFlagBit(Register.FlagCF, Flags.getLzFResd() & 1);
                    Register.setFlagBit(Register.FlagOF,
                            (Flags.getLzFResd() & 1) ^ (Flags.getLzFResd() >>> 31));

                    break;
                case 0x01:
                    // RORD
                    if (val == 0)
                        break;
                    Flags.fillFlagsNoCFOF();
                    Flags.setLzFVar1d(op1);
                    Flags.setLzFVar2b(val);
                    Flags.setLzFResd((Flags.getLzFVar1d() >>> Flags.getLzFVar2b()
                            | (Flags.getLzFVar1d() << (32 - Flags.getLzFVar2b()))));
                    Register.Regs[regIdx].setDWord(Flags.getLzFResd());
                    Register.setFlagBit(Register.FlagCF, Flags.getLzFResd() & 0x80000000);
                    Register.setFlagBit(Register.FlagOF,
                            (Flags.getLzFResd() ^ (Flags.getLzFResd() << 1)) & 0x80000000);

                    break;
                case 0x02:
                    // RCLD
                    if (val == 0)
                        break; {
                    int cf = Flags.fillFlags() & 0x1;
                    Flags.setLzFVar1d(op1);
                    Flags.setLzFVar2b(val);
                    if (Flags.getLzFVar2b() == 1) {
                        Flags.setLzFResd((Flags.getLzFVar1d() << 1) | cf);
                    } else {
                        Flags.setLzFResd((Flags.getLzFVar1d() << Flags.getLzFVar2b())
                                | (cf << (Flags.getLzFVar2b() - 1))
                                | (Flags.getLzFVar1d() >>> (33 - Flags.getLzFVar2b())));
                    }
                    Register.Regs[regIdx].setDWord(Flags.getLzFResd());
                    Register.setFlagBit(Register.FlagCF,
                            ((Flags.getLzFVar1d() >>> (32 - Flags.getLzFVar2b())) & 1));
                    Register.setFlagBit(Register.FlagOF,
                            (Register.Flags & 1) ^ (Flags.getLzFResd() >>> 31));
                }
                    break;
                case 0x03:
                    // RCRD
                    if (val != 0) {
                        int cf = Flags.fillFlags() & 0x1;
                        Flags.setLzFVar1d(op1);
                        Flags.setLzFVar2b(val);
                        if (Flags.getLzFVar2b() == 1) {
                            Flags.setLzFResd(Flags.getLzFVar1d() >>> 1 | cf << 31);
                        } else {
                            Flags.setLzFResd((Flags.getLzFVar1d() >>> Flags.getLzFVar2b())
                                    | (cf << (32 - Flags.getLzFVar2b()))
                                    | (Flags.getLzFVar1d() << (33 - Flags.getLzFVar2b())));
                        }
                        Register.Regs[regIdx].setDWord(Flags.getLzFResd());
                        Register.setFlagBit(Register.FlagCF,
                                (Flags.getLzFVar1d() >>> (Flags.getLzFVar2b() - 1)) & 1);
                        Register.setFlagBit(Register.FlagOF,
                                (Flags.getLzFResd() ^ (Flags.getLzFResd() << 1)) & 0x80000000);
                    }
                    break;
                case 0x04:/* SHL and SAL are the same */
                case 0x06:
                    // SHLD
                    if (val == 0)
                        break;
                    Flags.setLzFVar1d(op1);
                    Flags.setLzFVar2b(val);
                    Flags.setLzFResd(Flags.getLzFVar1d() << Flags.getLzFVar2b());
                    Register.Regs[regIdx].setDWord(Flags.getLzFResd());
                    Flags.LzFlags.Type = Flags.TypeFlag.SHLd;

                    break;
                case 0x05:
                    // SHRD
                    if (val == 0)
                        break;
                    Flags.setLzFVar1d(op1);
                    Flags.setLzFVar2b(val);
                    Flags.setLzFResd(Flags.getLzFVar1d() >>> Flags.getLzFVar2b());
                    Register.Regs[regIdx].setDWord(Flags.getLzFResd());
                    Flags.LzFlags.Type = Flags.TypeFlag.SHRd;

                    break;
                case 0x07:
                    // SARD
                    if (val == 0)
                        break;
                    Flags.setLzFVar2b(val);
                    Flags.setLzFVar1d(op1);
                    if ((Flags.getLzFVar1d() & 0x80000000) != 0) {
                        Flags.setLzFResd((Flags.getLzFVar1d() >>> Flags.getLzFVar2b())
                                | (0xffffffff << (32 - Flags.getLzFVar2b())));
                    } else {
                        Flags.setLzFResd(Flags.getLzFVar1d() >>> Flags.getLzFVar2b());
                    }
                    Register.Regs[regIdx].setDWord(Flags.getLzFResd());
                    Flags.LzFlags.Type = Flags.TypeFlag.SARd;

                    break;
            }
        } else {
            int op1 = Core.EATable[rm].get();
            int val = blah & 0x1f;
            switch (which) {
                case 0x00:
                    // ROLD
                    if (val == 0)
                        break;
                    Flags.fillFlagsNoCFOF();
                    Flags.setLzFVar1d(Memory.readD(op1));
                    Flags.setLzFVar2b(val);
                    Flags.setLzFResd((Flags.getLzFVar1d() << Flags.getLzFVar2b())
                            | (Flags.getLzFVar1d() >>> (32 - Flags.getLzFVar2b())));
                    Memory.writeD(op1, Flags.getLzFResd());
                    Register.setFlagBit(Register.FlagCF, Flags.getLzFResd() & 1);
                    Register.setFlagBit(Register.FlagOF,
                            (Flags.getLzFResd() & 1) ^ (Flags.getLzFResd() >>> 31));

                    break;
                case 0x01:
                    // RORD
                    if (val == 0)
                        break;
                    Flags.fillFlagsNoCFOF();
                    Flags.setLzFVar1d(Memory.readD(op1));
                    Flags.setLzFVar2b(val);
                    Flags.setLzFResd((Flags.getLzFVar1d() >>> Flags.getLzFVar2b())
                            | (Flags.getLzFVar1d() << (32 - Flags.getLzFVar2b())));
                    Memory.writeD(op1, Flags.getLzFResd());
                    Register.setFlagBit(Register.FlagCF, Flags.getLzFResd() & 0x80000000);
                    Register.setFlagBit(Register.FlagOF,
                            (Flags.getLzFResd() ^ (Flags.getLzFResd() << 1)) & 0x80000000);

                    break;
                case 0x02:
                    // RCLD
                    if (val == 0)
                        break; {
                    int cf = Flags.fillFlags() & 0x1;
                    Flags.setLzFVar1d(Memory.readD(op1));
                    Flags.setLzFVar2b(val);
                    if (Flags.getLzFVar2b() == 1) {
                        Flags.setLzFResd((Flags.getLzFVar1d() << 1) | cf);
                    } else {
                        Flags.setLzFResd((Flags.getLzFVar1d() << Flags.getLzFVar2b())
                                | (cf << (Flags.getLzFVar2b() - 1))
                                | (Flags.getLzFVar1d() >>> (33 - Flags.getLzFVar2b())));
                    }
                    Memory.writeD(op1, Flags.getLzFResd());
                    Register.setFlagBit(Register.FlagCF,
                            ((Flags.getLzFVar1d() >>> (32 - Flags.getLzFVar2b())) & 1));
                    Register.setFlagBit(Register.FlagOF,
                            (Register.Flags & 1) ^ (Flags.getLzFResd() >>> 31));
                }
                    break;
                case 0x03:
                    // RCRD
                    if (val != 0) {
                        int cf = Flags.fillFlags() & 0x1;
                        Flags.setLzFVar1d(Memory.readD(op1));
                        Flags.setLzFVar2b(val);
                        if (Flags.getLzFVar2b() == 1) {
                            Flags.setLzFResd(Flags.getLzFVar1d() >>> 1 | cf << 31);
                        } else {
                            Flags.setLzFResd((Flags.getLzFVar1d() >>> Flags.getLzFVar2b())
                                    | (cf << (32 - Flags.getLzFVar2b()))
                                    | (Flags.getLzFVar1d() << (33 - Flags.getLzFVar2b())));
                        }
                        Memory.writeD(op1, Flags.getLzFResd());
                        Register.setFlagBit(Register.FlagCF,
                                (Flags.getLzFVar1d() >>> (Flags.getLzFVar2b() - 1)) & 1);
                        Register.setFlagBit(Register.FlagOF,
                                (Flags.getLzFResd() ^ (Flags.getLzFResd() << 1)) & 0x80000000);
                    }
                    break;
                case 0x04:/* SHL and SAL are the same */
                case 0x06:
                    // SHLD
                    if (val == 0)
                        break;
                    Flags.setLzFVar1d(Memory.readD(op1));
                    Flags.setLzFVar2b(val);
                    Flags.setLzFResd(Flags.getLzFVar1d() << Flags.getLzFVar2b());
                    Memory.writeD(op1, Flags.getLzFResd());
                    Flags.LzFlags.Type = Flags.TypeFlag.SHLd;

                    break;
                case 0x05:
                    // SHRD
                    if (val == 0)
                        break;
                    Flags.setLzFVar1d(Memory.readD(op1));
                    Flags.setLzFVar2b(val);
                    Flags.setLzFResd(Flags.getLzFVar1d() >>> Flags.getLzFVar2b());
                    Memory.writeD(op1, Flags.getLzFResd());
                    Flags.LzFlags.Type = Flags.TypeFlag.SHRd;

                    break;
                case 0x07:
                    // SARD
                    if (val == 0)
                        break;
                    Flags.setLzFVar2b(val);
                    Flags.setLzFVar1d(Memory.readD(op1));
                    if ((Flags.getLzFVar1d() & 0x80000000) != 0) {
                        Flags.setLzFResd((Flags.getLzFVar1d() >>> Flags.getLzFVar2b())
                                | (0xffffffff << (32 - Flags.getLzFVar2b())));
                    } else {
                        Flags.setLzFResd(Flags.getLzFVar1d() >>> Flags.getLzFVar2b());
                    }
                    Memory.writeD(op1, Flags.getLzFResd());
                    Flags.LzFlags.Type = Flags.TypeFlag.SARd;

                    break;
            }
        }
    }

    /* let's hope bochs has it correct with the higher than 16 shifts */
    /* double-precision shift left has low bits in second argument */
    // 코드에 직접 삽입할 것
    // if break, return false
    // (int op1, short op2, byte op3)
    public boolean DSHLW_M(int op1, int op2, int op3) {
        int val = op3 & 0x1F;
        // if (val == 0) break;
        if (val == 0)
            return false;
        Flags.setLzFVar2b(val);
        Flags.setLzFVar1d((Memory.readW(op1) << 16) | op2);
        int tempd = Flags.getLzFVar1d() << Flags.getLzFVar2b();
        if (Flags.getLzFVar2b() > 16)
            tempd |= (op2 << (Flags.getLzFVar2b() - 16));
        Flags.setLzFresw(0xffff & (tempd >>> 16));
        Memory.writeW(op1, Flags.getLzFresw());
        Flags.LzFlags.Type = Flags.TypeFlag.DSHLw;
        return true;
    }

    // if break, false
    // (int regId, short op2, byte op3)
    public boolean DSHLW(int regId, int op2, int op3) {
        int val = op3 & 0x1F;
        if (val == 0)
            return false;
        // if (val == 0) break;
        Flags.setLzFVar2b(val);
        Flags.setLzFVar1d(((Register.Regs[regId].getWord() << 16) | op2));
        int tempd = Flags.getLzFVar1d() << Flags.getLzFVar2b();
        if (Flags.getLzFVar2b() > 16)
            tempd |= (op2 << (Flags.getLzFVar2b() - 16));
        Flags.setLzFresw(0xffff & (tempd >>> 16));
        Register.Regs[regId].setWord(Flags.getLzFresw());
        Flags.LzFlags.Type = Flags.TypeFlag.DSHLw;
        return true;
    }

    // cpu_full의 op.h
    public boolean DSHLWFull(int op1, short op2, byte op3) {
        int val = op3 & 0x1F;
        // if (val == 0) break;
        if (val == 0)
            return false;
        Flags.setLzFVar2b(val);
        Flags.setLzFVar1d((op1 << 16) | op2);
        int tempd = Flags.getLzFVar1d() << Flags.getLzFVar2b();
        if (Flags.getLzFVar2b() > 16)
            tempd |= (op2 << (Flags.getLzFVar2b() - 16));
        Flags.setLzFresw(0xffff & (tempd >>> 16));
        op1 = Flags.getLzFresw();
        Flags.LzFlags.Type = Flags.TypeFlag.DSHLw;
        return true;
    }

    // 코드에 직접 삽입할 것
    // if break, false
    // int op1, int op2, byte op3
    public boolean DSHLD_M(int op1, int op2, int op3) {
        int val = op3 & 0x1F;
        // if (val == 0) break;
        if (val == 0)
            return false;
        Flags.setLzFVar2b(val);
        Flags.setLzFVar1d(Memory.readD(op1));
        Flags.setLzFResd((Flags.getLzFVar1d() << Flags.getLzFVar2b())
                | (op2 >>> (32 - Flags.getLzFVar2b())));
        Memory.writeD(op1, Flags.getLzFResd());
        Flags.LzFlags.Type = Flags.TypeFlag.DSHLd;
        return true;
    }

    // int regId, int op2, byte op3
    public boolean DSHLD(int regId, int op2, int op3) {
        int val = op3 & 0x1F;
        // if (val == 0) break;
        if (val == 0)
            return false;
        Flags.setLzFVar2b(val);
        Flags.setLzFVar1d(Register.Regs[regId].getDWord());
        Flags.setLzFResd((Flags.getLzFVar1d() << Flags.getLzFVar2b())
                | (op2 >>> (32 - Flags.getLzFVar2b())));
        Register.Regs[regId].setDWord(Flags.getLzFResd());
        Flags.LzFlags.Type = Flags.TypeFlag.DSHLd;
        return true;
    }

    // cpu_full의 op.h
    public boolean DSHLDFull(int op1, int op2, byte op3) {
        int val = op3 & 0x1F;
        // if (val == 0) break;
        if (val == 0)
            return false;
        Flags.setLzFVar2b(val);
        Flags.setLzFVar1d(op1);
        Flags.setLzFResd((Flags.getLzFVar1d() << Flags.getLzFVar2b())
                | (op2 >>> (32 - Flags.getLzFVar2b())));
        op1 = Flags.getLzFResd();
        Flags.LzFlags.Type = Flags.TypeFlag.DSHLd;
        return true;
    }

    /* double-precision shift right has high bits in second argument */
    // 코드에 직접 삽입할 것
    // if break, return false
    // (int regId, short op2, byte op3)
    public boolean DSHRW_M(int op1, int op2, int op3) {
        int val = op3 & 0x1F;
        // if (val == 0) break;
        if (val == 0)
            return false;
        Flags.setLzFVar2b(val);
        Flags.setLzFVar1d((op2 << 16) | Memory.readW(op1));
        int tempd = Flags.getLzFVar1d() >>> Flags.getLzFVar2b();
        if (Flags.getLzFVar2b() > 16)
            tempd |= (op2 << (32 - Flags.getLzFVar2b()));
        Flags.setLzFresw(0xffff & (tempd));
        Memory.writeW(op1, Flags.getLzFresw());
        Flags.LzFlags.Type = Flags.TypeFlag.DSHRw;
        return true;
    }

    // break면 false
    // (int regId, short op2, byte op3)
    public boolean DSHRW(int regId, int op2, int op3) {
        int val = op3 & 0x1F;
        // if (val == 0) break;
        if (val == 0)
            return false;
        Flags.setLzFVar2b(val);
        Flags.setLzFVar1d((op2 << 16) | Register.Regs[regId].getWord());
        int tempd = Flags.getLzFVar1d() >>> Flags.getLzFVar2b();
        if (Flags.getLzFVar2b() > 16)
            tempd |= (op2 << (32 - Flags.getLzFVar2b()));
        Flags.setLzFresw(0xffff & (tempd));
        Register.Regs[regId].setWord(Flags.getLzFresw());
        Flags.LzFlags.Type = Flags.TypeFlag.DSHRw;
        return true;
    }

    // cpu_full의 op.h
    // (uint32 op1, uint16 op2, uint8 op3)
    public boolean DSHRWFull(int op1, int op2, int op3) {
        int val = op3 & 0x1F;
        // if (val == 0) break;
        if (val == 0)
            return false;
        Flags.setLzFVar2b(val);
        Flags.setLzFVar1d((op2 << 16) | op1);
        int tempd = Flags.getLzFVar1d() >>> Flags.getLzFVar2b();
        if (Flags.getLzFVar2b() > 16)
            tempd |= (op2 << (32 - Flags.getLzFVar2b()));
        Flags.setLzFresw(0xffff & (tempd));
        op1 = Flags.getLzFresw();
        Flags.LzFlags.Type = Flags.TypeFlag.DSHRw;
        return true;
    }

    // 코드에 직접 삽입할 것
    // break면 false
    // int op1, int op2, byte op3
    public boolean DSHRD_M(int op1, int op2, int op3) {
        int val = op3 & 0x1F;
        // if (val == 0) break;
        if (val == 0)
            return false;
        Flags.setLzFVar2b(val);
        Flags.setLzFVar1d(Memory.readD(op1));
        Flags.setLzFResd((Flags.getLzFVar1d() >>> Flags.getLzFVar2b())
                | (op2 << (32 - Flags.getLzFVar2b())));
        Memory.writeD(op1, Flags.getLzFResd());
        Flags.LzFlags.Type = Flags.TypeFlag.DSHRd;
        return true;
    }

    // break면 false
    // int regId, int op2, byte op3
    public boolean DSHRD(int regId, int op2, int op3) {
        int val = op3 & 0x1F;
        // if (val == 0) break;
        if (val == 0)
            return false;
        Flags.setLzFVar2b(val);
        Flags.setLzFVar1d(Register.Regs[regId].getDWord());
        Flags.setLzFResd((Flags.getLzFVar1d() >>> Flags.getLzFVar2b())
                | (op2 << (32 - Flags.getLzFVar2b())));
        Register.Regs[regId].setDWord(Flags.getLzFResd());
        Flags.LzFlags.Type = Flags.TypeFlag.DSHRd;
        return true;
    }

    // cpu_full의 op.h
    public boolean DSHRDFull(int op1, int op2, byte op3) {
        int val = op3 & 0x1F;
        // if (val == 0) break;
        if (val == 0)
            return false;
        Flags.setLzFVar2b(val);
        Flags.setLzFVar1d(op1);
        Flags.setLzFResd((Flags.getLzFVar1d() >>> Flags.getLzFVar2b())
                | (op2 << (32 - Flags.getLzFVar2b())));
        op1 = Flags.getLzFResd();
        Flags.LzFlags.Type = Flags.TypeFlag.DSHRd;
        return true;
    }

    // 코드에 직접 삽입
    // (short op1)
    public void BSWAPW(int op1) {
        op1 = 0;

    }

    // 코드에 직접삽입
    public void BSWAPD(int op1) {
        op1 = (op1 >>> 24) | ((op1 >>> 8) & 0xFF00) | ((op1 << 8) & 0xFF0000)
                | ((op1 << 24) & 0xFF000000);

    }
    /*--------------------------- end Instructions -----------------------------*/

}


