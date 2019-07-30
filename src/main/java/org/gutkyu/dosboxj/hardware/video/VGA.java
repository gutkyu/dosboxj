package org.gutkyu.dosboxj.hardware.video;

import java.util.Arrays;
import org.gutkyu.dosboxj.*;
import org.gutkyu.dosboxj.gui.*;
import org.gutkyu.dosboxj.gui.Mapper;
import org.gutkyu.dosboxj.gui.Render;
import org.gutkyu.dosboxj.hardware.*;
import org.gutkyu.dosboxj.hardware.io.*;
import org.gutkyu.dosboxj.hardware.io.iohandler.ReadHandler;
import org.gutkyu.dosboxj.hardware.io.iohandler.WriteHandler;
import org.gutkyu.dosboxj.hardware.memory.Memory;
import org.gutkyu.dosboxj.hardware.memory.paging.*;
import org.gutkyu.dosboxj.hardware.video.memory.paging.*;
import org.gutkyu.dosboxj.hardware.video.svga.*;
import org.gutkyu.dosboxj.interrupt.int10.*;
import org.gutkyu.dosboxj.misc.*;
import org.gutkyu.dosboxj.misc.setup.*;
import org.gutkyu.dosboxj.util.*;

public final class VGA {

    private static final int VGA_CHANGE_SHIFT = 9;

    public static final int CLK_25 = 25175;
    public static final int CLK_28 = 28322;

    private static final int MIN_VCO = 180000;
    private static final int MAX_VCO = 360000;

    public static final int S3_CLOCK_REF = 14318; /* KHz */
    // private static final int S3_CLOCK(_M,_N,_R) ((S3_CLOCK_REF * ((_M) + 2)) / (((_N) +
    // 2) * (1 << (_R))))
    private static final int S3_MAX_CLOCK = 150000; /* KHz */

    public static final short S3_XGA_1024 = 0x00;
    public static final short S3_XGA_1152 = 0x01;
    public static final short S3_XGA_640 = 0x40;
    public static final short S3_XGA_800 = 0x80;
    public static final short S3_XGA_1280 = 0xc0;
    public static final short S3_XGA_WMASK =
            (S3_XGA_640 | S3_XGA_800 | S3_XGA_1024 | S3_XGA_1152 | S3_XGA_1280);

    public static final short S3_XGA_8BPP = 0x00;
    public static final short S3_XGA_16BPP = 0x10;
    public static final short S3_XGA_32BPP = 0x30;
    public static final short S3_XGA_CMASK = (S3_XGA_8BPP | S3_XGA_16BPP | S3_XGA_32BPP);

    // -- #region VGA_Type
    public VGAModes Mode; /* The mode the vga system is in */
    public int MiscOutput;// byte
    public VGADraw Draw;
    public VGAConfig Config;
    public VGAInternal Internal;
    /* Internal module groups */
    public VGASeq Seq;
    public VGAAttr Attr;
    public VGACrtc Crtc;
    public VGAGFX GFX;
    public VGADac Dac;
    public VGALatch Latch;
    public VGAS3 S3;
    public VGASVGA SVGA;
    public VGAHerc Herc;
    public VGATandy Tandy;
    public VGAOther Other;
    public VGAMemory Mem;
    public int VMemWrap; /* this is assumed to be power of 2 */
    // byte* fastmem;
    /* memory for fast (usually 16-color) rendering, always twice as big as vmemsize */
    public byte[] FastMemAlloc;
    public long FastMemBase;// uint type
    // 메모리 할당시 15비트 이동한 다음 주소숫자의 뒷자리 15비트부분을 깨끗하게 지우는 작업을 하는 이유는?
    // 그러므로 vga.fastmem_orgptr 없이 바로 할당
    // byte* fastmem_orgptr;
    public int VMemSize;
    public VGALFB Lfb;
    // -- #endregion

    public SVGADriver SVGADrv = new SVGADriver();
    public VGAXGA XGA = null;

    private final int[] CGA2Table = new int[16];
    private final int[] CGA4Table = new int[256];
    private final int[] CGA4HiResTable = new int[256];
    private final int[] CGA16Table = new int[256];
    private final int[] TXTFontTable = new int[16];
    private final int[] TXTFGTable = new int[16];
    private final int[] TXTBGTable = new int[16];
    public final int[] ExpandTable = new int[256];
    public final int[][] Expand16Table = new int[4][16];
    public final int[] FillTable = new int[16];
    private final int[] ColorTable = new int[16];

    private VGA() {
        // VGA_Type init
        Draw = new VGADraw();
        Draw.Font = new byte[64 * 1024];
        Draw.FontTablesIdx = new int[2];
        Config = new VGAConfig();
        Internal = new VGAInternal();
        Seq = new VGASeq(this);
        Attr = new VGAAttr(this);
        Crtc = new VGACrtc(this);
        GFX = new VGAGFX(this);
        Dac = new VGADac(this);
        Latch = new VGALatch();
        S3 = new VGAS3();
        S3.CLK = new CLK[4];
        for (int i = 0; i < S3.CLK.length; i++) {
            S3.CLK[i] = new CLK();
        }
        S3.HGC = new VGAHwCursor();
        S3.HGC.ForeStack = new byte[3];
        S3.HGC.BackStack = new byte[3];
        S3.HGC.MC = new byte[64][];

        for (int i = 0; i < 64; i++) {
            S3.HGC.MC[i] = new byte[64];
        }
        SVGA = new VGASVGA();
        Herc = new VGAHerc();
        Tandy = new VGATandy();
        Other = new VGAOther();
        Mem = new VGAMemory();
        Lfb = new VGALFB();

        vgaPageHandler = new VGAPageHandler();

        vgaPageHandler.Map = new MapHandler(this);
        vgaPageHandler.Changes = new ChangesHandler(this);
        vgaPageHandler.Text = new TextPageHandler(this);
        vgaPageHandler.Tandy = new TandyPageHandler(this);
        vgaPageHandler.ChainedEGA = new ChainedEGAHandler(this);
        vgaPageHandler.ChainedVGA = new ChainedVGAHandler(this);
        vgaPageHandler.UnchainedEGA = new UnchainedEGAHandler(this);
        vgaPageHandler.UnchainedVGA = new UnchainedVGAHandler(this);
        vgaPageHandler.PCjr = new PCJrHandler(this);
        vgaPageHandler.Lin4 = new LIN4Handler(this);
        vgaPageHandler.Lfb = new LFBHandler(this);
        vgaPageHandler.LfbChanges = new LFBChangesHandler(this);
        vgaPageHandler.MMIO = new MMIOHandler(this);
        vgaPageHandler.Empty = new EmptyHandler();

        XGA = new VGAXGA(this);

        for (int i = 0; i < 256; i++) {
            ExpandTable[i] = i | (i << 8) | (i << 16) | (i << 24);
        }
        for (int i = 0; i < 16; i++) {
            TXTFGTable[i] = i | (i << 8) | (i << 16) | (i << 24);
            TXTBGTable[i] = i | (i << 8) | (i << 16) | (i << 24);

            FillTable[i] = ((i & 1) != 0 ? 0x000000ff : 0) | ((i & 2) != 0 ? 0x0000ff00 : 0)
                    | ((i & 4) != 0 ? 0x00ff0000 : 0) | ((i & 8) != 0 ? 0xff000000 : 0);
            TXTFontTable[i] = ((i & 1) != 0 ? 0xff000000 : 0) | ((i & 2) != 0 ? 0x00ff0000 : 0)
                    | ((i & 4) != 0 ? 0x0000ff00 : 0) | ((i & 8) != 0 ? 0x000000ff : 0);

        }
        for (int j = 0; j < 4; j++) {
            for (int i = 0; i < 16; i++) {
                Expand16Table[j][i] =
                        ((i & 1) != 0 ? 1 << (24 + j) : 0) | ((i & 2) != 0 ? 1 << (16 + j) : 0)
                                | ((i & 4) != 0 ? 1 << (8 + j) : 0) | ((i & 8) != 0 ? 1 << j : 0);

            }
        }
    }

    private void setModeNow(VGAModes mode) {
        if (Mode == mode)
            return;
        Mode = mode;
        setupHandlers();
        startResize(0);
    }

    public void setMode(VGAModes mode) {
        if (Mode == mode)
            return;
        Mode = mode;
        setupHandlers();
        startResize();
    }

    public void determineMode() {
        if (SVGADrv.DetermineMode != null) {
            SVGADrv.DetermineMode.exec();
            return;
        }
        /* Test for VGA output active or direct color modes */
        switch (S3.MiscControl2 >>> 4) {
            case 0:
                if ((Attr.ModeControl & 1) != 0) { // graphics mode
                    if (DOSBox.isVGAArch() && (GFX.Mode & 0x40) != 0) {
                        // access above 256k?
                        if ((S3.Reg31 & 0x8) != 0)
                            setMode(VGAModes.LIN8);
                        else
                            setMode(VGAModes.VGA);
                    } else if ((GFX.Mode & 0x20) != 0)
                        setMode(VGAModes.CGA4);
                    else if ((GFX.Miscellaneous & 0x0c) == 0x0c)
                        setMode(VGAModes.CGA2);
                    else {
                        // access above 256k?
                        if ((S3.Reg31 & 0x8) != 0)
                            setMode(VGAModes.LIN4);
                        else
                            setMode(VGAModes.EGA);
                    }
                } else {
                    setMode(VGAModes.TEXT);
                }
                break;
            case 1:
                setMode(VGAModes.LIN8);
                break;
            case 3:
                setMode(VGAModes.LIN15);
                break;
            case 5:
                setMode(VGAModes.LIN16);
                break;
            case 13:
                setMode(VGAModes.LIN32);
                break;
        }
    }

    public void startResize(int delay) {
        if (!Draw.Resizing) {
            Draw.Resizing = true;
            if (Mode == VGAModes.ERROR)
                delay = 5;
            /* Start a resize after delay (default 50 ms) */
            if (delay == 0)
                setupDrawing(0);
            else
                PIC.addEvent(setupDrawingWrap, (float) delay);
        }
    }

    public void startResize() {
        startResize(50);
    }

    public void setClock(int which, int target) {
        if (SVGADrv.SetClock != null) {
            SVGADrv.SetClock.exec(which, target);
            return;
        }
        int bestErr = target;
        int bestM = 1;
        int bestN = 1;
        int n;
        int r;
        int m;

        for (r = 0; r <= 3; r++) {
            int fVco = target * (1 << r);
            if (MIN_VCO <= fVco && fVco < MAX_VCO)
                break;
        }
        for (n = 1; n <= 31; n++) {
            m = (target * (n + 2) * (1 << r) + (S3_CLOCK_REF / 2)) / S3_CLOCK_REF - 2;
            if (0 <= m && m <= 127) {
                // int temp_target = S3_CLOCK(m, n, r);
                int tempTarget = (S3_CLOCK_REF * (m + 2)) / ((n + 2) * (1 << r));
                int err = target - tempTarget;
                if (err < 0)
                    err = -err;
                if (err < bestErr) {
                    bestErr = err;
                    bestM = m;
                    bestN = n;
                }
            }
        }
        /* Program the s3 clock chip */
        S3.CLK[which].m = (byte) bestM;
        S3.CLK[which].r = (byte) r;
        S3.CLK[which].n = (byte) bestN;
        startResize();
    }

    // private void setCGA2Table(byte val0, byte val1) {
    private void setCGA2Table(int val0, int val1) {
        int[] total = {val0, val1};
        for (int i = 0; i < 16; i++) {
            CGA2Table[i] = (total[(i >>> 3) & 1] << 0) | (total[(i >>> 2) & 1] << 8)
                    | (total[(i >>> 1) & 1] << 16) | (total[(i >>> 0) & 1] << 24);

        }
    }

    // private void setCGA4Table(byte val0, byte val1, byte val2, byte val3) {
    private void setCGA4Table(int val0, int val1, int val2, int val3) {
        int[] total = {val0, val1, val2, val3};
        for (int i = 0; i < 256; i++) {
            CGA4Table[i] = (total[(i >>> 6) & 3] << 0) | (total[(i >>> 4) & 3] << 8)
                    | (total[(i >>> 2) & 3] << 16) | (total[(i >>> 0) & 3] << 24);

            CGA4HiResTable[i] = (total[((i >>> 3) & 1) | ((i >>> 6) & 2)] << 0)
                    | (total[((i >>> 2) & 1) | ((i >>> 5) & 2)] << 8)
                    | (total[((i >>> 1) & 1) | ((i >>> 4) & 2)] << 16)
                    | (total[((i >>> 0) & 1) | ((i >>> 3) & 2)] << 24);

        }
    }

    private static VGA vga = null;

    public static VGA instance() {
        return vga;
    }

    public static void init(Section sec) {
        vga = new VGA();
        // Section_prop * section=static_cast<Section_prop *>(sec);
        vga.Draw.Resizing = false;
        vga.Mode = VGAModes.ERROR; // For first init
        vga.SVGADrv.setup(vga);
        vga.setupMemory(sec);
        vga.setupMisc();
        vga.Dac.setup();
        vga.GFX.setup();
        vga.Seq.setup();
        vga.Attr.setup();
        vga.setupOther();
        vga.XGA.setup();
        vga.setClock(0, CLK_25);
        vga.setClock(1, CLK_28);
        /* Generate tables */
        vga.setCGA2Table(0, 1);
        vga.setCGA4Table(0, 1, 2, 3);

    }

    /*--------------------------- begin VGADraw -----------------------------*/

    private static final int VGA_PARTS = 4;

    // private delegate void VGALineHandler(int vidstart, int line, out byte[] ret,
    // out int retStart);

    private FuncDrawLine VGADrawLine;
    // private static int[] TempLineUInt32 = new byte[SCALER_MAXWIDTH];
    // private static short[] TempLineshort = new byte[SCALER_MAXWIDTH * 2];
    private byte[] TempLine = new byte[Render.SCALER_MAXWIDTH * 4];

    private byte[] currentVGALine = null;
    private int currentVGALineOffset = 0;

    private void draw1BPPLine(int vidStart, int line) {
        int baseAddr = Tandy.DrawBase + ((line & Tandy.LineMask) << Tandy.LineShift);
        int drawAddr = 0;
        for (int x = Draw.Blocks; x > 0; x--, vidStart++) {
            int val = 0xff & Tandy.DrawAlloc[baseAddr + (vidStart & (8 * 1024 - 1))];
            ByteConv.setInt(TempLine, drawAddr, CGA2Table[val >>> 4]);
            drawAddr += 4;
            ByteConv.setInt(TempLine, drawAddr, CGA2Table[val & 0xf]);
            drawAddr += 4;
        }
        currentVGALine = TempLine;
        currentVGALineOffset = 0;
    }

    private void draw2BPPLine(int vidStart, int line) {
        int baseAddr = Tandy.DrawBase + ((line & Tandy.LineMask) << Tandy.LineShift);
        int drawAddr = 0;
        for (int x = 0; x < Draw.Blocks; x++) {
            int val = 0xff & Tandy.DrawAlloc[baseAddr + (vidStart & Tandy.AddrMask)];
            vidStart++;
            ByteConv.setInt(TempLine, drawAddr, CGA4Table[val]);
            drawAddr += 4;
        }
        currentVGALine = TempLine;
        currentVGALineOffset = 0;
    }

    private void draw2BPPHiResLine(int vidStart, int line) {
        int baseAddr = Tandy.DrawBase + ((line & Tandy.LineMask) << Tandy.LineShift);
        int drawAddr = 0;
        // ByteConvert drawVal = ¸new ByteConvert();
        for (int x = 0; x < Draw.Blocks; x++) {
            int val1 = 0xff & Tandy.DrawAlloc[baseAddr + (vidStart & Tandy.AddrMask)];
            ++vidStart;
            int val2 = 0xff & Tandy.DrawAlloc[baseAddr + (vidStart & Tandy.AddrMask)];
            ++vidStart;
            ByteConv.setInt(TempLine, drawAddr, CGA4HiResTable[(val1 >>> 4) | (val2 & 0xf0)]);
            drawAddr += 4;
            ByteConv.setInt(TempLine, drawAddr,
                    CGA4HiResTable[(val1 & 0x0f) | ((val2 & 0x0f) << 4)]);
            drawAddr += 4;
        }
        currentVGALine = TempLine;
        currentVGALineOffset = 0;
    }

    private int[] temp = new int[643];

    private void drawCGA16Line(int vidStart, int line) {
        int baseAddr = Tandy.DrawBase + ((line & Tandy.LineMask) << Tandy.LineShift);
        byte[] reader = Tandy.DrawAlloc;
        int readerAddr = baseAddr + vidStart;

        int drawAddr = 0;
        // Generate a temporary bitline to calculate the avarage
        // over bit-2 bit-1 bit bit+1.
        // Combine this number with the current colour to get
        // an unigue index in the pallete. Or it with bit 7 as they are stored
        // in the upperpart to keep them from interfering the regular cga stuff

        for (int x = 0; x < 640; x++)
            temp[x + 2] = (((0xff & reader[readerAddr + (x >>> 3)]) >>> (7 - (x & 7))) & 1) << 4;
        // shift 4 as that is for the index.
        int i = 0, temp1, temp2, temp3, temp4;
        for (int x = 0; x < Draw.Blocks; x++) {
            int val1 = 0xff & reader[readerAddr++];
            int val2 = val1 & 0xf;
            val1 >>>= 4;

            temp1 = temp[i] + temp[i + 1] + temp[i + 2] + temp[i + 3];
            i++;
            temp2 = temp[i] + temp[i + 1] + temp[i + 2] + temp[i + 3];
            i++;
            temp3 = temp[i] + temp[i + 1] + temp[i + 2] + temp[i + 3];
            i++;
            temp4 = temp[i] + temp[i + 1] + temp[i + 2] + temp[i + 3];
            i++;
            ByteConv.setInt(TempLine, drawAddr, 0x80808080 | (temp1 | val1) | ((temp2 | val1) << 8)
                    | ((temp3 | val1) << 16) | ((temp4 | val1) << 24));
            drawAddr += 4;

            temp1 = temp[i] + temp[i + 1] + temp[i + 2] + temp[i + 3];
            i++;
            temp2 = temp[i] + temp[i + 1] + temp[i + 2] + temp[i + 3];
            i++;
            temp3 = temp[i] + temp[i + 1] + temp[i + 2] + temp[i + 3];
            i++;
            temp4 = temp[i] + temp[i + 1] + temp[i + 2] + temp[i + 3];
            i++;
            ByteConv.setInt(TempLine, drawAddr, 0x80808080 | (temp1 | val2) | ((temp2 | val2) << 8)
                    | ((temp3 | val2) << 16) | ((temp4 | val2) << 24));
            drawAddr += 4;
        }
        currentVGALine = TempLine;
        currentVGALineOffset = 0;
    }

    private void draw4BPPLine(int vidStart, int line) {
        byte[] base = Tandy.DrawAlloc;
        int baseAddr = Tandy.DrawBase + ((line & Tandy.LineMask) << Tandy.LineShift);
        int drawAddr = 0;
        for (int x = 0; x < Draw.Blocks; x++) {
            int val1 = 0xff & base[baseAddr + (vidStart & Tandy.AddrMask)];
            ++vidStart;
            int val2 = 0xff & base[baseAddr + (vidStart & Tandy.AddrMask)];
            ++vidStart;
            ByteConv.setInt(TempLine, drawAddr, (val1 & 0x0f) << 8 | (val1 & 0xf0) >>> 4
                    | (val2 & 0x0f) << 24 | (val2 & 0xf0) << 12);
            drawAddr += 4;
        }
        currentVGALine = TempLine;
        currentVGALineOffset = 0;
    }

    private void draw4BPPLineDouble(int vidStart, int line) {
        byte[] base = Tandy.DrawAlloc;
        int baseAddr = Tandy.DrawBase + ((line & Tandy.LineMask) << Tandy.LineShift);
        int drawAddr = 0;
        for (int x = 0; x < Draw.Blocks; x++) {
            int val = 0xff & base[baseAddr + (vidStart & Tandy.AddrMask)];
            ++vidStart;
            ByteConv.setInt(TempLine, drawAddr, (val & 0xf0) >>> 4 | (val & 0xf0) << 4
                    | (val & 0x0f) << 16 | (val & 0x0f) << 24);
            drawAddr += 4;
        }
        currentVGALine = TempLine;
        currentVGALineOffset = 0;

    }

    private void drawLinearLine(int vidStart, int line) {
        // There is guaranteed extra memory past the wrap boundary. So, instead of using
        // temporary
        // storage just copy appropriate chunk from the beginning to the wrap boundary
        // when needed.
        int offset = vidStart & Draw.LinearMask;
        if (Draw.LinearMask - offset < Draw.LineLength)
            ArrayHelper.copy(Draw.LinearAlloc, Draw.LinearBase, Draw.LinearAlloc,
                    Draw.LinearBase + Draw.LinearMask + 1, Draw.LineLength);
        currentVGALine = Draw.LinearAlloc;
        currentVGALineOffset = Draw.LinearBase + offset;

        // Console.WriteLine("vidstart {2}: offset {0} : value {1}", offset,
        // vga.draw.linearAlloc[retStart], vidstart);
    }

    private void drawXlat16LinearLine(int vidStart, int line) {
        // Bit8u* ret = &vga.draw.linear_base[vidstart & vga.draw.linear_mask];
        byte[] retAlloc = Draw.LinearAlloc;
        int retIdx = Draw.LinearBase + (vidStart & Draw.LinearMask);
        // Bit16u* temps = (Bit16u*)TempLine;
        int tempsIdx = 0;
        for (int i = 0; i < Draw.LineLength; i++) {
            ByteConv.setShort(TempLine, tempsIdx, Dac.Xlat16[retAlloc[retIdx + i]]);
            tempsIdx += 2;
        }
        currentVGALine = TempLine;
        currentVGALineOffset = 0;
        /*
         * #if !defined(C_UNALIGNED_MEMORY) if (GCC_UNLIKELY( ((int)ret) & (sizeof(int)-1)) ) {
         * memcpy( TempLine, ret, vga.draw.line_length ); return TempLine; } #endif return ret;
         */
    }

    // Test version, might as well keep it
    /*
     * static byte * VGA_Draw_Chain_Line(int vidstart, int line) { int i = 0; for ( i = 0; i <
     * vga.draw.width;i++ ) { int addr = vidstart + i; TempLine[i] =
     * vga.mem.linear[((addr&~3)<<2)+(addr&3)]; } return TempLine; }
     */

    private void drawVGALineHWMouse(int vidStart, int line) {
        if (SVGADrv.HardwareCursorActive == null || !SVGADrv.HardwareCursorActive.exec()) {
            // HW Mouse not enabled, use the tried and true call
            currentVGALine = Mem.LinearAlloc;
            currentVGALineOffset = vidStart;
            return;
        }
        int lineat = (vidStart - (Config.RealStart << 2)) / Draw.Width;
        if ((S3.HGC.PosX >= Draw.Width) || (lineat < S3.HGC.OriginY)
                || (lineat > (S3.HGC.OriginY + (63 - S3.HGC.PosY)))) {
            // the mouse cursor *pattern* is not on this line
            currentVGALine = Mem.LinearAlloc;
            currentVGALineOffset = vidStart;
            return;
        } else {
            // Draw mouse cursor: cursor is a 64x64 pattern which is shifted (inside the
            // 64x64 mouse cursor space) to the right by posx pixels and up by posy pixels.
            // This is used when the mouse cursor partially leaves the screen.
            // It is arranged as bitmap of 16bits of bitA followed by 16bits of bitB, each
            // AB bits corresponding to a cursor pixel. The whole map is 8kB in size.
            ArrayHelper.copy(Mem.LinearAlloc, vidStart, TempLine, 0, Draw.Width);
            // the index of the bit inside the cursor bitmap we start at:
            int sourceStartBit = ((lineat - S3.HGC.OriginY) + S3.HGC.PosY) * 64 + S3.HGC.PosX;
            // convert to video memory addr and bit index
            // start adjusted to the pattern structure (thus shift address by 2 instead of
            // 3)
            // Need to get rid of the third bit, so "/8 *2" becomes ">>>2 & ~1"
            int cursorMemStart =
                    ((sourceStartBit >>> 2) & ~1) + ((0xffff & S3.HGC.StartAddr) << 10);
            int cursorStartBit = sourceStartBit & 0x7;
            // stay at the right position in the pattern
            if ((cursorMemStart & 0x2) != 0)
                cursorMemStart--;
            int cursorMemEnd = cursorMemStart + ((64 - S3.HGC.PosX) >>> 2);
            byte[] xat = TempLine;
            int xatIdx = S3.HGC.OriginX; // mouse data start pos. in scanline
            for (int m = cursorMemStart; m < cursorMemEnd; m += ((m & 1) != 0) ? 3 : 1) {
                // for each byte of cursor data
                byte bitsA = Mem.LinearAlloc[m];
                byte bitsB = Mem.LinearAlloc[m + 2];
                for (int bit = 0x80 >>> cursorStartBit; bit != 0; bit >>>= 1) {
                    // for each bit
                    cursorStartBit = 0; // only the first byte has some bits cut off
                    if ((bitsA & bit) != 0) {
                        if ((bitsB & bit) != 0)
                            xat[xatIdx] ^= 0xFF; // Invert screen data
                        // else Transparent
                    } else if ((bitsB & bit) != 0) {
                        xat[xatIdx] = S3.HGC.ForeStack[0]; // foreground color
                    } else {
                        xat[xatIdx] = S3.HGC.BackStack[0];
                    }
                    xatIdx++;
                }
            }

            currentVGALine = TempLine;
            currentVGALineOffset = 0;
            return;
        }
    }

    private void drawLIN16LineHWMouse(int vidStart, int line) {
        if (SVGADrv.HardwareCursorActive == null || !SVGADrv.HardwareCursorActive.exec()) {
            currentVGALine = Mem.LinearAlloc;
            currentVGALineOffset = vidStart;
            return;
        }
        int lineAt = ((vidStart - (Config.RealStart << 2)) >>> 1) / Draw.Width;
        if ((S3.HGC.PosX >= Draw.Width) || (lineAt < S3.HGC.OriginY)
                || (lineAt > (S3.HGC.OriginY + (63 - S3.HGC.PosY)))) {
            currentVGALine = Mem.LinearAlloc;
            currentVGALineOffset = vidStart;
            return;
        } else {
            ArrayHelper.copy(Mem.LinearAlloc, vidStart, TempLine, 0, Draw.Width * 2);
            int sourceStartBit = ((lineAt - S3.HGC.OriginY) + S3.HGC.PosY) * 64 + S3.HGC.PosX;
            int cursorMemStart =
                    ((sourceStartBit >>> 2) & ~1) + ((0xffff & S3.HGC.StartAddr) << 10);
            int cursorStartBit = sourceStartBit & 0x7;
            if ((cursorMemStart & 0x2) != 0)
                cursorMemStart--;
            int cursorMemEnd = cursorMemStart + ((64 - S3.HGC.PosX) >>> 2);

            // Bit16u* xat = &((Bit16u*)TempLine)[vga.s3.hgc.originx];
            int xatIdx = S3.HGC.OriginX * 2;
            for (int m = cursorMemStart; m < cursorMemEnd; m += ((m & 1) != 0) ? 3 : 1) {
                // for each byte of cursor data
                byte bitsA = Mem.LinearAlloc[m];
                byte bitsB = Mem.LinearAlloc[m + 2];
                for (int bit = 0x80 >>> cursorStartBit; bit != 0; bit >>>= 1) {
                    // for each bit
                    cursorStartBit = 0;
                    if ((bitsA & bit) != 0) {
                        // byte order doesn't matter here as all bits get flipped
                        if ((bitsB & bit) != 0) {
                            TempLine[xatIdx] ^= 0xff;
                            TempLine[xatIdx + 1] ^= 0xff;
                        }
                        // else Transparent
                    } else if ((bitsB & bit) != 0) {
                        // Source as well as destination are byte arrays,
                        // so this should work out endian-wise?
                        TempLine[xatIdx] = S3.HGC.ForeStack[0];
                        TempLine[xatIdx + 1] = S3.HGC.ForeStack[1];

                    } else {
                        TempLine[xatIdx] = S3.HGC.BackStack[0];
                        TempLine[xatIdx + 1] = S3.HGC.BackStack[1];
                    }
                    xatIdx += 2;
                }
            }
            currentVGALine = TempLine;
            currentVGALineOffset = 0;
            return;
        }
    }

    private void drawLIN32LineHWMouse(int vidStart, int line) {
        if (SVGADrv.HardwareCursorActive == null || !SVGADrv.HardwareCursorActive.exec()) {
            currentVGALine = Mem.LinearAlloc;
            currentVGALineOffset = vidStart;
            return;
        }
        int lineat = ((vidStart - (Config.RealStart << 2)) >>> 2) / Draw.Width;
        if ((S3.HGC.PosX >= Draw.Width) || (lineat < S3.HGC.OriginY)
                || (lineat > (S3.HGC.OriginY + (63 - S3.HGC.PosY)))) {
            currentVGALine = Mem.LinearAlloc;
            currentVGALineOffset = vidStart;
            return;
        } else {
            ArrayHelper.copy(Mem.LinearAlloc, vidStart, TempLine, 0, Draw.Width * 4);
            int sourceStartBit = ((lineat - S3.HGC.OriginY) + S3.HGC.PosY) * 64 + S3.HGC.PosX;
            int cursorMemStart =
                    ((sourceStartBit >>> 2) & ~1) + ((0xffff & S3.HGC.StartAddr) << 10);
            int cursorStartBit = sourceStartBit & 0x7;
            if ((cursorMemStart & 0x2) != 0)
                cursorMemStart--;
            int cursorMemEnd = cursorMemStart + ((64 - S3.HGC.PosX) >>> 2);
            // Bit32u* xat = &((Bit32u*)TempLine)[vga.s3.hgc.originx];
            int xatIdx = S3.HGC.OriginX * 4;
            for (int m = cursorMemStart; m < cursorMemEnd; m += ((m & 1) != 0) ? 3 : 1) {
                // for each byte of cursor data
                byte bitsA = Mem.LinearAlloc[m];
                byte bitsB = Mem.LinearAlloc[m + 2];

                for (int bit = 0x80 >>> cursorStartBit; bit != 0; bit >>>= 1) { // for each bit
                    cursorStartBit = 0;
                    if ((bitsA & bit) != 0) {
                        if ((bitsB & bit) != 0) {
                            TempLine[xatIdx] ^= 0xff;
                            TempLine[xatIdx + 1] ^= 0xff;
                            TempLine[xatIdx + 2] ^= 0xff;
                            TempLine[xatIdx + 3] ^= 0xff;
                        }
                        // else Transparent
                    } else if ((bitsB & bit) != 0) {
                        TempLine[xatIdx] = S3.HGC.ForeStack[0];
                        TempLine[xatIdx + 1] = S3.HGC.ForeStack[1];
                        TempLine[xatIdx + 2] = S3.HGC.ForeStack[2];
                        TempLine[xatIdx + 3] = 0;
                    } else {
                        TempLine[xatIdx] = S3.HGC.BackStack[0];
                        TempLine[xatIdx + 1] = S3.HGC.BackStack[1];
                        TempLine[xatIdx + 2] = S3.HGC.BackStack[2];
                        TempLine[xatIdx + 3] = 0;
                    }
                    xatIdx += 4;
                }
            }
            currentVGALine = TempLine;
            currentVGALineOffset = 0;
            return;
        }
    }

    private byte[] currentTextMemwrap;
    private int currentTextMemwrapOffset;

    private void wrapTextMem(int vidStart) {
        vidStart &= Draw.LinearMask;
        int line_end = 2 * Draw.Blocks;
        if ((vidStart + line_end) > Draw.LinearMask) {
            // wrapping in this line
            int break_pos = (Draw.LinearMask - vidStart) + 1;
            // need a temporary storage - TempLine/2 is ok for a bit more than 132 columns
            ArrayHelper.copy(Tandy.DrawAlloc, Tandy.DrawBase + vidStart, TempLine,
                    TempLine.length / 2, break_pos);
            ArrayHelper.copy(Tandy.DrawAlloc, Tandy.DrawBase, TempLine,
                    TempLine.length / 2 + break_pos, line_end - break_pos);
            currentTextMemwrap = TempLine;
            currentTextMemwrapOffset = TempLine.length / 2;
        } else {
            currentTextMemwrap = Tandy.DrawAlloc;
            currentTextMemwrapOffset = Tandy.DrawBase + vidStart;
        }
    }

    // TODO 텍스트를 원하는 위치에 놓았는지 확인이 필요
    private int[] FontMask = {0xffffffff, 0x0};

    private void drawLineTEXT(int vidStart, int line) {
        int fontAddr;
        int drawIdx = 0;
        wrapTextMem(vidStart);
        byte[] vidmem = currentTextMemwrap;
        int vidmemIdx = currentTextMemwrapOffset;

        for (int cx = 0; cx < Draw.Blocks; cx++) {
            int chr = 0xff & vidmem[vidmemIdx + cx * 2];
            int col = 0xff & vidmem[vidmemIdx + cx * 2 + 1];
            // int font = vga.draw.font_tables[(col >>>3) & 1][chr * 32 + line];
            int font = 0xff & Draw.Font[Draw.FontTablesIdx[(col >>> 3) & 1] + chr * 32 + line];
            int mask1 = TXTFontTable[font >>> 4] & FontMask[col >>> 7];
            int mask2 = TXTFontTable[font & 0xf] & FontMask[col >>> 7];
            int fg = TXTFGTable[col & 0xf];
            int bg = TXTBGTable[col >>> 4];
            ByteConv.setInt(TempLine, drawIdx, (fg & mask1) | (bg & ~mask1));
            drawIdx += 4;
            ByteConv.setInt(TempLine, drawIdx, (fg & mask2) | (bg & ~mask2));
            drawIdx += 4;
        }
        while (true) {
            if (Draw.Cursor.Enabled == 0 || (Draw.Cursor.Count & 0x8) == 0)
                break;// goto skip_cursor;
            fontAddr = (Draw.Cursor.Address - vidStart) >>> 1;
            if (fontAddr >= 0 && fontAddr < Draw.Blocks) {
                if (line < Draw.Cursor.SLine)
                    break;// goto skip_cursor;
                if (line > Draw.Cursor.ELine)
                    break;// goto skip_cursor;
                drawIdx = fontAddr * 8;
                int att =
                        TXTFGTable[Tandy.DrawAlloc[Tandy.DrawBase + Draw.Cursor.Address + 1] & 0xf];
                ByteConv.setInt(TempLine, drawIdx, att);
                drawIdx += 4;
                ByteConv.setInt(TempLine, drawIdx, att);
                drawIdx += 4;
            }
            break;
        }
        // skip_cursor:
        currentVGALine = TempLine;
        currentVGALineOffset = 0;
    }

    private void drawLineTEXTHerc(int vidstart, int line) {
        int fontAddr;
        int drawIdx = 0;
        wrapTextMem(vidstart);
        byte[] vidmem = currentTextMemwrap;
        int vidmemIdx = currentTextMemwrapOffset;

        for (int cx = 0; cx < Draw.Blocks; cx++) {
            int chr = 0xff & vidmem[cx * 2];
            int attrib = 0xff & vidmem[cx * 2 + 1];
            if ((attrib & 0x77) == 0) {
                // 00h, 80h, 08h, 88h produce black space
                TempLine[drawIdx++] = 0;
                TempLine[drawIdx++] = 0;
                TempLine[drawIdx++] = 0;
                TempLine[drawIdx++] = 0;
                TempLine[drawIdx++] = 0;
                TempLine[drawIdx++] = 0;
                TempLine[drawIdx++] = 0;
                TempLine[drawIdx++] = 0;

            } else {
                int bg, fg;
                boolean underline = false;
                if ((attrib & 0x77) == 0x70) {
                    bg = TXTBGTable[0x7];
                    if ((attrib & 0x8) != 0)
                        fg = TXTFGTable[0xf];
                    else
                        fg = TXTFGTable[0x0];
                } else {
                    if (((Crtc.UnderlineLocation & 0x1f) == line) && ((attrib & 0x77) == 0x1))
                        underline = true;
                    bg = TXTBGTable[0x0];
                    if ((attrib & 0x8) != 0)
                        fg = TXTFGTable[0xf];
                    else
                        fg = TXTFGTable[0x7];
                }
                int mask1, mask2;
                if (underline)
                    mask1 = mask2 = FontMask[attrib >>> 7];
                else {
                    // int font = vga.draw.font_tables[0][chr * 32 + line];
                    int font = 0xff & Draw.Font[Draw.FontTablesIdx[0] + chr * 32 + line];
                    mask1 = TXTFontTable[font >>> 4] & FontMask[attrib >>> 7]; // blinking
                    mask2 = TXTFontTable[font & 0xf] & FontMask[attrib >>> 7];
                }
                ByteConv.setInt(TempLine, drawIdx, (fg & mask1) | (bg & ~mask1));
                drawIdx += 4;
                ByteConv.setInt(TempLine, drawIdx, (fg & mask2) | (bg & ~mask2));
                drawIdx += 4;
            }
        }
        while (true) {
            if (Draw.Cursor.Enabled == 0 || (Draw.Cursor.Count & 0x8) == 0)
                break;// goto skip_cursor;
            fontAddr = (Draw.Cursor.Address - vidstart) >>> 1;
            if (fontAddr >= 0 && fontAddr < Draw.Blocks) {
                if (line < Draw.Cursor.SLine)
                    break;// goto skip_cursor;
                if (line > Draw.Cursor.ELine)
                    break;// goto skip_cursor;
                drawIdx = fontAddr * 8;
                byte attr = Tandy.DrawAlloc[Tandy.DrawBase + Draw.Cursor.Address + 1];
                int cg;
                if ((attr & 0x8) != 0) {
                    cg = TXTFGTable[0xf];
                } else if ((attr & 0x77) == 0x70) {
                    cg = TXTFGTable[0x0];
                } else {
                    cg = TXTFGTable[0x7];
                }
                ByteConv.setInt(TempLine, drawIdx, cg);
                drawIdx += 4;
                ByteConv.setInt(TempLine, drawIdx, cg);
                drawIdx += 4;
            }
            break;
        }
        // skip_cursor:
        currentVGALine = TempLine;
        currentVGALineOffset = 0;

    }

    private void drawLineTEXTXlat16(int vidStart, int line) {
        int fontAddr;
        int drawIdx = 0;
        wrapTextMem(vidStart);
        byte[] vidmem = currentTextMemwrap;
        int vidmemIdx = currentTextMemwrapOffset;

        for (int cx = 0; cx < Draw.Blocks; cx++) {
            int chr = 0xff & vidmem[cx * 2];
            int col = 0xff & vidmem[cx * 2 + 1];
            // int font = vga.draw.font_tables[(col >>>3) & 1][chr * 32 + line];
            int font = 0xff & Draw.Font[Draw.FontTablesIdx[(col >>> 3) & 1] + chr * 32 + line];
            int mask1 = TXTFontTable[font >>> 4] & FontMask[col >>> 7];
            int mask2 = TXTFontTable[font & 0xf] & FontMask[col >>> 7];
            int fg = TXTFGTable[col & 0xf];
            int bg = TXTBGTable[col >>> 4];

            mask1 = (fg & mask1) | (bg & ~mask1);
            mask2 = (fg & mask2) | (bg & ~mask2);

            for (int i = 0; i < 4; i++) {
                ByteConv.setShort(TempLine, drawIdx, Dac.Xlat16[(mask1 >>> 8 * i) & 0xff]);
                drawIdx += 2;
            }
            for (int i = 0; i < 4; i++) {
                ByteConv.setShort(TempLine, drawIdx, Dac.Xlat16[(mask2 >>> 8 * i) & 0xff]);
                drawIdx += 2;
            }
        }
        while (true) {
            if (Draw.Cursor.Enabled == 0 || (Draw.Cursor.Count & 0x8) == 0)
                break;// goto skip_cursor;
            fontAddr = (Draw.Cursor.Address - vidStart) >>> 1;
            if (fontAddr >= 0 && fontAddr < Draw.Blocks) {
                if (line < Draw.Cursor.SLine)
                    break;// goto skip_cursor;
                if (line > Draw.Cursor.ELine)
                    break;// goto skip_cursor;
                drawIdx = fontAddr * 16;
                int att =
                        TXTFGTable[Tandy.DrawAlloc[Tandy.DrawBase + Draw.Cursor.Address + 1] & 0xf]
                                & 0xff;
                for (int i = 0; i < 8; i++) {
                    ByteConv.setShort(TempLine, drawIdx, Dac.Xlat16[att]);
                    drawIdx += 2;
                }
            }
            break;
        }
        // skip_cursor:
        currentVGALine = TempLine;
        currentVGALineOffset = 0;
    }

    /*
     * static byte * VGA_TEXT_Draw_Line_9(int vidstart, int line) { .... }
     */

    private void drawLine9TEXTXlat16(int vidStart, int line) {
        int fontAddr;
        int drawIdx = 0;
        boolean underline = (Crtc.UnderlineLocation & 0x1f) == line;
        int pelPan = Draw.Panning;
        if ((Attr.ModeControl & 0x20) == 0 && (Draw.LinesDone >= Draw.SplitLine))
            pelPan = 0;
        wrapTextMem(vidStart);
        byte[] vidmem = currentTextMemwrap;
        int vidmemIdx = currentTextMemwrapOffset;

        int chr = 0xff & vidmem[0];
        int col = 0xff & vidmem[1];
        // byte font = (byte)((vga.draw.font_tables[(col >>>3) & 1][chr * 32 + line]) <<
        // pel_pan);
        int font = 0xff & ((0xff
                & Draw.Font[Draw.FontTablesIdx[(col >>> 3) & 1] + chr * 32 + line]) << pelPan);
        if (underline && ((col & 0x07) == 0x01))
            font = 0xff;
        int fg = col & 0xf;
        int bg = TXTBGTable[col >>> 4] & 0xff;
        int drawBlocks = Draw.Blocks;
        drawBlocks++;

        for (int cx = 1; cx < drawBlocks; cx++) {
            if (pelPan != 0) {
                chr = 0xff & vidmem[cx * 2];
                col = 0xff & vidmem[cx * 2 + 1];
                if (underline && ((col & 0x07) == 0x01))
                    font |= 0xff & (0xff >>> (8 - pelPan));
                else
                    font |= 0xff & ((0xff & Draw.Font[Draw.FontTablesIdx[(col >>> 3) & 1] + chr * 32
                            + line]) >>> (8 - pelPan));
                fg = col & 0xf;
                bg = TXTBGTable[col >>> 4] & 0xff;
            } else {
                chr = 0xff & vidmem[(cx - 1) * 2];
                col = 0xff & vidmem[(cx - 1) * 2 + 1];
                if (underline && ((col & 0x07) == 0x01))
                    font = 0xff;
                else
                    font = 0xff & Draw.Font[Draw.FontTablesIdx[(col >>> 3) & 1] + chr * 32 + line];
                fg = col & 0xf;
                bg = TXTBGTable[col >>> 4] & 0xff;
            }
            if (FontMask[col >>> 7] == 0)
                font = 0;
            int mask = 0x80;
            for (int i = 0; i < 7; i++) {
                ByteConv.setShort(TempLine, drawIdx, Dac.Xlat16[(font & mask) != 0 ? fg : bg]);
                drawIdx += 2;
                mask >>>= 1;
            }
            short lastval = Dac.Xlat16[(font & mask) != 0 ? fg : bg];
            ByteConv.setShort(TempLine, drawIdx, lastval);
            drawIdx += 2;
            ByteConv.setShort(TempLine, drawIdx,
                    (((Attr.ModeControl & 0x04) != 0 && ((chr < 0xc0) || (chr > 0xdf)))
                            && !(underline && ((col & 0x07) == 0x01))) ? (Dac.Xlat16[bg])
                                    : lastval);
            drawIdx += 2;
            if (pelPan != 0) {
                if (underline && ((col & 0x07) == 0x01))
                    font = 0xff;
                else
                    font = 0xff & ((0xff & Draw.Font[Draw.FontTablesIdx[(col >>> 3) & 1] + chr * 32
                            + line]) << pelPan);
            }
        }
        while (true) {
            if (Draw.Cursor.Enabled == 0 || (Draw.Cursor.Count & 0x8) == 0)
                break;// goto skip_cursor;
            fontAddr = (Draw.Cursor.Address - vidStart) >>> 1;
            if (fontAddr >= 0 && fontAddr < Draw.Blocks) {
                if (line < Draw.Cursor.SLine)
                    break;// goto skip_cursor;
                if (line > Draw.Cursor.ELine)
                    break;// goto skip_cursor;
                // draw = (Bit16u*)&TempLine[font_addr * 18];
                drawIdx = (fontAddr * 18) * 2;
                fg = Tandy.DrawAlloc[Tandy.DrawBase + Draw.Cursor.Address + 1] & 0xf;
                for (int i = 0; i < 8; i++) {
                    ByteConv.setShort(TempLine, drawIdx, Dac.Xlat16[fg]);
                    drawIdx += 2;
                }
                // if(underline && ((col&0x07) == 0x01))
                // *draw = vga.dac.xlat16[fg];
            }
            break;
        }
        // skip_cursor:
        currentVGALine = TempLine;
        currentVGALineOffset = 0;
    }

    private void processSplit() {
        // On the EGA the address is always reset to 0.
        if ((Attr.ModeControl & 0x20) != 0 || (DOSBox.Machine == DOSBox.MachineType.EGA)) {
            Draw.Address = 0;
        } else {
            // In text mode only the characters are shifted by panning, not the address;
            // this is done in the text line draw function.
            Draw.Address = Draw.BytePanningShift * Draw.BytesSkip;
            if (!(Mode == VGAModes.TEXT))
                Draw.Address += Draw.Panning;
        }
        Draw.AddressLine = 0;
    }

    private EventHandler drawSingleLineWrap = this::drawSingleLine;

    private void drawSingleLine(int blah) {
        if (Attr.Disabled != 0) {
            // draw blanked line (DoWhackaDo, Alien Carnage, TV sports Football)
            Arrays.fill(TempLine, 0, TempLine.length, (byte) 0);
            Render.instance().DrawLine.draw(TempLine, 0);
        } else {
            VGADrawLine.draw(Draw.Address, Draw.AddressLine);
            Render.instance().DrawLine.draw(currentVGALine, currentVGALineOffset);
        }

        Draw.AddressLine++;
        if (Draw.AddressLine >= Draw.AddressLineTotal) {
            Draw.AddressLine = 0;
            Draw.Address += Draw.AddressAdd;
        }
        Draw.LinesDone++;
        if (Draw.SplitLine == Draw.LinesDone)
            processSplit();
        if (Draw.LinesDone < Draw.LinesTotal) {
            PIC.addEvent(drawSingleLineWrap, (float) Draw.Delay.HTotal);
        } else
            Render.instance().endUpdate(false);
    }

    private EventHandler drawPartWrap = this::drawPart;

    private void drawPart(int lines) {
        while (lines-- > 0) {

            VGADrawLine.draw(Draw.Address, Draw.AddressLine);
            Render.instance().DrawLine.draw(currentVGALine, currentVGALineOffset);
            Draw.AddressLine++;
            if (Draw.AddressLine >= Draw.AddressLineTotal) {
                Draw.AddressLine = 0;
                Draw.Address += Draw.AddressAdd;
            }
            Draw.LinesDone++;
            if (Draw.SplitLine == Draw.LinesDone) {

                processSplit();

            }
        }
        if (--Draw.PartsLeft > 0) {
            PIC.addEvent(drawPartWrap, (float) Draw.Delay.Parts,
                    (Draw.PartsLeft != 1) ? Draw.PartsLines : (Draw.LinesTotal - Draw.LinesDone));
        } else {

            Render.instance().endUpdate(false);
        }
    }

    public void setBlinking(int enabled) {
        int b;
        Log.logging(Log.LogTypes.VGA, Log.LogServerities.Normal, "Blinking %d", enabled);
        if (enabled != 0) {
            b = 0;
            Draw.Blinking = 1; // used to -1 but blinking is unsigned
            Attr.ModeControl |= 0x08;
            Tandy.ModeControl |= 0x20;
        } else {
            b = 8;
            Draw.Blinking = 0;
            Attr.ModeControl &= 0xff & (~0x08);
            Tandy.ModeControl &= 0xff & (~0x20);
        }
        for (int i = 0; i < 8; i++)
            TXTBGTable[i + 8] = (b + i) | ((b + i) << 8) | ((b + i) << 16) | ((b + i) << 24);
    }

    private EventHandler vertInterruptWrap = this::vertInterrupt;

    private void vertInterrupt(int val) {
        if ((!Draw.VRetTriggered) && ((Crtc.VerticalRetraceEnd & 0x30) == 0x10)) {
            Draw.VRetTriggered = true;
            if (DOSBox.Machine == DOSBox.MachineType.EGA)
                PIC.activateIRQ(9);
        }
    }

    private EventHandler otherVertInterruptWrap = this::otherVertInterrupt;

    private void otherVertInterrupt(int val) {
        if (val != 0)
            PIC.activateIRQ(5);
        else
            PIC.deactivateIRQ(5);
    }

    private EventHandler vgaDisplayStartLatchWrap = this::displayStartLatch;

    // private void VGA_DisplayStartLatch(int val) {
    private void displayStartLatch(int val) {
        Config.RealStart = Config.DisplayStart & (VMemWrap - 1);
        Draw.BytesSkip = Config.BytesSkip;
    }

    private EventHandler panningLatchWrap = this::panningLatch;

    private void panningLatch(int val) {
        Draw.Panning = Config.PelPanning;
    }

    private EventHandler verticalTimerWrap = this::verticalTimer;

    private void verticalTimer(int val) {
        Draw.Delay.FrameStart = PIC.getFullIndex();
        PIC.addEvent(verticalTimerWrap, (float) Draw.Delay.VTotal);

        switch (DOSBox.Machine) {
            case PCJR:
            case TANDY:
                // PCJr: Vsync is directly connected to the IRQ controller
                // Some earlier Tandy models are said to have a vsync interrupt too
                PIC.addEvent(otherVertInterruptWrap, (float) Draw.Delay.VRStart, 1);
                PIC.addEvent(otherVertInterruptWrap, (float) Draw.Delay.VREnd, 0);
                // fall-through
                // goto GotoMCH_HERC;
            case CGA:
            case HERC:
                // GotoMCH_HERC:
                // MC6845-powered graphics: Loading the display start latch happens somewhere
                // after vsync off and before first visible scanline, so probably here
                displayStartLatch(0);
                break;
            case VGA:
            case EGA:
                PIC.addEvent(vgaDisplayStartLatchWrap, (float) Draw.Delay.VRStart);
                PIC.addEvent(panningLatchWrap, (float) Draw.Delay.VREnd);
                // EGA: 82c435 datasheet: interrupt happens at display end
                // VGA: checked with scope; however disabled by default by jumper on VGA boards
                // add a little amount of time to make sure the last drawpart has already fired
                PIC.addEvent(vertInterruptWrap, (float) (Draw.Delay.VDEnd + 0.005));
                break;
            default:
                Support.exceptionExit(
                        "This new dosbox.machine needs implementation in VGA_VerticalTimer too.");
                break;
        }
        // Check if we can actually render, else skip the rest (frameskip)
        if (!Render.instance().startUpdate())
            return;

        Draw.AddressLine = Config.HLinesSkip;
        if (DOSBox.isEGAVGAArch()) {
            Draw.SplitLine = (Config.LineCompare + 1) / Draw.LinesScaled;
            if ((DOSBox.SVGACard == DOSBox.SVGACards.S3Trio) && (Config.LineCompare == 0))
                Draw.SplitLine = 0;
            Draw.SplitLine -= Draw.VBlankSkip;
        } else {
            Draw.SplitLine = 0x10000; // don't care
        }
        Draw.Address = Config.RealStart;
        Draw.BytePanningShift = 0;
        // go figure...
        if (DOSBox.Machine == DOSBox.MachineType.EGA)
            Draw.SplitLine *= 2;
        // if (dosbox.machine==dosbox.MachineType.MCH_EGA) vga.draw.split_line =
        // ((((vga.config.line_compare&0x5ff)+1)*2-1)/vga.draw.lines_scaled);
        switch (Mode) {
            case EGA:
                if ((Crtc.ModeControl & 0x1) == 0)
                    Draw.LinearMask &= ~0x10000;
                else
                    Draw.LinearMask |= 0x10000;
                // goto GotoM_LIN4;
            case LIN4:
                // GotoM_LIN4:
                Draw.BytePanningShift = 8;
                Draw.Address += Draw.BytesSkip;
                Draw.Address *= Draw.BytePanningShift;
                Draw.Address += Draw.Panning;

                break;
            case VGA:
                if (Config.CompatibleChain4 && (Crtc.UnderlineLocation & 0x40) != 0) {
                    Draw.LinearAlloc = FastMemAlloc;
                    Draw.LinearBase = (int) FastMemBase;
                    Draw.LinearMask = 0xffff;
                } else {
                    Draw.LinearAlloc = Mem.LinearAlloc;
                    Draw.LinearBase = Mem.LinearBase;
                    Draw.LinearMask = VMemWrap - 1;
                }
                // goto GotoM_LIN32;
            case LIN8:
            case LIN15:
            case LIN16:
            case LIN32:
                // GotoM_LIN32:
                Draw.BytePanningShift = 4;
                Draw.Address += Draw.BytesSkip;
                Draw.Address *= Draw.BytePanningShift;
                Draw.Address += Draw.Panning;

                break;
            case TEXT:
                Draw.BytePanningShift = 2;
                Draw.Address += Draw.BytesSkip;
                // fall-through
                // goto GotoM_HERC_TEXT;
            case TANDY_TEXT:
            case HERC_TEXT:
                // GotoM_HERC_TEXT:
                if (DOSBox.Machine == DOSBox.MachineType.HERC)
                    Draw.LinearMask = 0xfff; // 1 page
                else if (DOSBox.isEGAVGAArch())
                    Draw.LinearMask = 0x7fff; // 8 pages
                else
                    Draw.LinearMask = 0x3fff; // CGA, Tandy 4 pages
                Draw.Cursor.Address = Config.CursorStart * 2;
                Draw.Address *= 2;
                Draw.Cursor.Count++;
                /* check for blinking and blinking change delay */
                FontMask[1] = (Draw.Blinking & (Draw.Cursor.Count >>> 4)) != 0 ? 0 : 0xffffffff;
                break;
            case HERC_GFX:
                break;
            case CGA4:
            case CGA2:
                Draw.Address = (Draw.Address * 2) & 0x1fff;
                break;
            case CGA16:
            case TANDY2:
            case TANDY4:
            case TANDY16:
                Draw.Address *= 2;
                break;
            default:
                break;
        }
        if (Draw.SplitLine == 0)
            processSplit();

        // check if some lines at the top off the screen are blanked
        float drawSkip = 0.0F;
        if (Draw.VBlankSkip != 0) {
            drawSkip = (float) (Draw.Delay.HTotal * Draw.VBlankSkip);
            Draw.Address += Draw.AddressAdd * (Draw.VBlankSkip / (Draw.AddressLineTotal));
        }

        // add the draw event
        switch (Draw.Mode) {
            case PART:
                if (Draw.PartsLeft != 0) {
                    Log.logging(Log.LogTypes.VGAMISC, Log.LogServerities.Normal, "Parts left: %d",
                            Draw.PartsLeft);
                    PIC.removeEvents(drawPartWrap);
                    Render.instance().endUpdate(true);
                }
                Draw.LinesDone = 0;
                Draw.PartsLeft = Draw.PartsTotal;
                PIC.addEvent(drawPartWrap, (float) Draw.Delay.Parts + drawSkip, Draw.PartsLines);
                break;
            case LINE:
                if (Draw.LinesDone < Draw.LinesTotal) {
                    Log.logging(Log.LogTypes.VGAMISC, Log.LogServerities.Normal, "Lines left: %d",
                            Draw.LinesTotal - Draw.LinesDone);
                    PIC.removeEvents(drawSingleLineWrap);
                    Render.instance().endUpdate(true);
                }
                Draw.LinesDone = 0;
                PIC.addEvent(drawSingleLineWrap, (float) (Draw.Delay.HTotal / 4.0 + drawSkip));
                break;
            // case EGALINE:
        }
    }

    public void checkScanLength() {
        switch (Mode) {
            case EGA:
            case LIN4:
                Draw.AddressAdd = Config.ScanLen * 16;
                break;
            case VGA:
            case LIN8:
            case LIN15:
            case LIN16:
            case LIN32:
                Draw.AddressAdd = Config.ScanLen * 8;
                break;
            case TEXT:
                Draw.AddressAdd = Config.ScanLen * 4;
                break;
            case CGA2:
            case CGA4:
            case CGA16:
                Draw.AddressAdd = 80;
                return;
            case TANDY2:
                Draw.AddressAdd = Draw.Blocks / 4;
                break;
            case TANDY4:
                Draw.AddressAdd = Draw.Blocks;
                break;
            case TANDY16:
                Draw.AddressAdd = Draw.Blocks;
                break;
            case TANDY_TEXT:
                Draw.AddressAdd = Draw.Blocks * 2;
                break;
            case HERC_TEXT:
                Draw.AddressAdd = Draw.Blocks * 2;
                break;
            case HERC_GFX:
                Draw.AddressAdd = Draw.Blocks;
                break;
            default:
                Draw.AddressAdd = Draw.Blocks * 8;
                break;
        }
    }

    public void activateHardwareCursor() {
        boolean hwCursorActive = false;
        if (SVGADrv.HardwareCursorActive != null) {
            if (SVGADrv.HardwareCursorActive.exec())
                hwCursorActive = true;
        }
        if (hwCursorActive) {
            switch (Mode) {
                case LIN32:
                    VGADrawLine = this::drawLIN32LineHWMouse;
                    break;
                case LIN15:
                case LIN16:
                    VGADrawLine = this::drawLIN16LineHWMouse;
                    break;
                default:
                    VGADrawLine = this::drawVGALineHWMouse;
                    break;
            }
        } else {
            VGADrawLine = this::drawLinearLine;
        }
    }

    EventHandler setupDrawingWrap = this::setupDrawing;

    private void setupDrawing(int val) {
        if (Mode == VGAModes.ERROR) {
            PIC.removeEvents(verticalTimerWrap);
            PIC.removeEvents(panningLatchWrap);
            PIC.removeEvents(vgaDisplayStartLatchWrap);
            return;
        }
        // set the drawing mode
        switch (DOSBox.Machine) {
            case CGA:
            case PCJR:
                Draw.Mode = DrawMode.LINE;
                break;
            case VGA:
                if (DOSBox.SVGACard == DOSBox.SVGACards.None) {
                    Draw.Mode = DrawMode.LINE;
                    break;
                }
                // fall-through
                // goto GotoDefault;
            default:
                // GotoDefault:
                Draw.Mode = DrawMode.PART;
                break;
        }

        /* Calculate the FPS for this screen */
        double fps;
        int clock;
        int htotal, hdend, hbstart, hbend, hrstart, hrend;
        int vtotal, vdend, vbstart, vbend, vrstart, vrend;
        int vblankSkip;
        if (DOSBox.isEGAVGAArch()) {
            htotal = Crtc.HorizontalTotal;
            hdend = Crtc.HorizontalDisplayEnd;
            hbend = Crtc.EndHorizontalBlanking & 0x1F;
            hbstart = Crtc.StartHorizontalBlanking;
            hrstart = Crtc.StartHorizontalRetrace;

            vtotal = Crtc.VerticalTotal | ((Crtc.Overflow & 1) << 8);
            vdend = Crtc.VerticalDisplayEnd | ((Crtc.Overflow & 2) << 7);
            vbstart = Crtc.StartVerticalBlanking | ((Crtc.Overflow & 0x08) << 5);
            vrstart = Crtc.VerticalRetraceStart + ((Crtc.Overflow & 0x04) << 6);

            if (DOSBox.isVGAArch()) {
                // additional bits only present on vga cards
                htotal |= (S3.ExHorOverflow & 0x1) << 8;
                htotal += 3;
                hdend |= (S3.ExHorOverflow & 0x2) << 7;
                hbend |= (Crtc.EndHorizontalRetrace & 0x80) >>> 2;
                hbstart |= (S3.ExHorOverflow & 0x4) << 6;
                hrstart |= (S3.ExHorOverflow & 0x10) << 4;

                vtotal |= (Crtc.Overflow & 0x20) << 4;
                vtotal |= (S3.ExVerOverflow & 0x1) << 10;
                vdend |= (Crtc.Overflow & 0x40) << 3;
                vdend |= (S3.ExVerOverflow & 0x2) << 9;
                vbstart |= (Crtc.MaximumScanLine & 0x20) << 4;
                vbstart |= (S3.ExVerOverflow & 0x4) << 8;
                vrstart |= ((Crtc.Overflow & 0x80) << 2);
                vrstart |= (S3.ExVerOverflow & 0x10) << 6;
                vbend = Crtc.EndVerticalBlanking & 0x7f;
            } else { // EGA
                vbend = Crtc.EndVerticalBlanking & 0x1f;
            }
            htotal += 2;
            vtotal += 2;
            hdend += 1;
            vdend += 1;
            vbstart += 1;

            hbend = hbstart + ((hbend - hbstart) & 0x3F);
            hrend = Crtc.EndHorizontalRetrace & 0x1f;
            hrend = (hrend - hrstart) & 0x1f;

            if (hrend == 0)
                hrend = hrstart + 0x1f + 1;
            else
                hrend = hrstart + hrend;

            vrend = Crtc.VerticalRetraceEnd & 0xF;
            vrend = (vrend - vrstart) & 0xF;

            if (vrend == 0)
                vrend = vrstart + 0xf + 1;
            else
                vrend = vrstart + vrend;

            vbend = (vbend - vbstart) & 0x7f;
            if (vbend == 0)
                vbend = vbstart + 0x7f + 1;
            else
                vbend = vbstart + vbend;

            vbend++;

            if (SVGADrv.GetClock != null) {
                clock = SVGADrv.GetClock.exec();
            } else {
                switch ((MiscOutput >>> 2) & 3) {
                    case 0:
                        clock = (DOSBox.Machine == DOSBox.MachineType.EGA) ? 14318180 : 25175000;
                        break;
                    case 1:
                    default:
                        clock = (DOSBox.Machine == DOSBox.MachineType.EGA) ? 16257000 : 28322000;
                        break;
                }
            }

            /* Check for 8 for 9 character clock mode */
            if ((Seq.ClockingMode & 1) != 0)
                clock /= 8;
            else
                clock /= 9;
            /* Check for pixel doubling, master clock/2 */
            if ((Seq.ClockingMode & 0x8) != 0) {
                htotal *= 2;
            }
            Draw.AddressLineTotal = (Crtc.MaximumScanLine & 0x1f) + 1;
            if (DOSBox.isVGAArch() && (DOSBox.SVGACard == DOSBox.SVGACards.None)
                    && (Mode == VGAModes.EGA || Mode == VGAModes.VGA)) {
                // vgaonly; can't use with CGA because these use address_line for their
                // own purposes.
                // Set the low resolution modes to have as many lines as are scanned -
                // Quite a few demos change the max_scanline register at display time
                // to get SFX: Majic12 show, Magic circle, Copper, GBU, Party91
                if ((Crtc.MaximumScanLine & 0x80) != 0)
                    Draw.AddressLineTotal *= 2;
                Draw.DoubleScan = false;
            } else if (DOSBox.isVGAArch())
                Draw.DoubleScan = (Crtc.MaximumScanLine & 0x80) > 0;
            else
                Draw.DoubleScan = (vtotal == 262);
        } else {
            htotal = Other.HTotal + 1;
            hdend = Other.HdEnd;
            hbstart = hdend;
            hbend = htotal;
            hrstart = Other.HSyncP;
            hrend = hrstart + Other.HSyncW;

            Draw.AddressLineTotal = Other.MaxScanLine + 1;
            vtotal = Draw.AddressLineTotal * (Other.VTotal + 1) + Other.VAdjust;
            vdend = Draw.AddressLineTotal * Other.VdEnd;
            vrstart = Draw.AddressLineTotal * Other.VSyncP;
            vrend = vrstart + 16; // vsync width is fixed to 16 lines on the MC6845 TODO Tandy
            vbstart = vdend;
            vbend = vtotal;
            Draw.DoubleScan = false;
            switch (DOSBox.Machine) {
                case CGA:
                case TANDY:
                case PCJR:
                    clock = ((Tandy.ModeControl & 1) != 0 ? 14318180 : (14318180 / 2)) / 8;
                    break;
                case HERC:
                    if ((Herc.ModeControl & 0x2) != 0)
                        clock = 16000000 / 16;
                    else
                        clock = 16000000 / 8;
                    break;
                default:
                    clock = 14318180;
                    break;
            }
            Draw.Delay.HDEnd = hdend * 1000.0 / clock; // in milliseconds
        }
        /*
         * #if C_DEBUG LOG(LOG_VGA,LOG_NORMAL)("h total %d end %d blank (%d/%d) retrace (%d/%d)",
         * htotal, hdend, hbstart, hbend, hrstart, hrend );
         * LOG(LOG_VGA,LOG_NORMAL)("v total %d end %d blank (%d/%d) retrace (%d/%d)", vtotal, vdend,
         * vbstart, vbend, vrstart, vrend ); #endif
         */
        if (htotal == 0)
            return;
        if (vtotal == 0)
            return;

        // The screen refresh frequency
        fps = (double) clock / (vtotal * htotal);
        // Horizontal total (that's how long a line takes with whistles and bells)
        Draw.Delay.HTotal = htotal * 1000.0 / clock; // in milliseconds
        // Start and End of horizontal blanking
        Draw.Delay.HBlkStart = hbstart * 1000.0 / clock; // in milliseconds
        Draw.Delay.HBlkEnd = hbend * 1000.0 / clock;
        // Start and End of horizontal retrace
        Draw.Delay.HRStart = hrstart * 1000.0 / clock;
        Draw.Delay.HREnd = hrend * 1000.0 / clock;
        // Start and End of vertical blanking
        Draw.Delay.VBlkStart = vbstart * Draw.Delay.HTotal;
        Draw.Delay.VBlkEnd = vbend * Draw.Delay.HTotal;
        // Start and End of vertical retrace pulse
        Draw.Delay.VRStart = vrstart * Draw.Delay.HTotal;
        Draw.Delay.VREnd = vrend * Draw.Delay.HTotal;

        // Vertical blanking tricks
        vblankSkip = 0;
        if (DOSBox.isVGAArch()) { // others need more investigation
            if (vbend > vtotal) {
                // blanking wraps to the start of the screen
                vblankSkip = vbend & 0x7f;

                // on blanking wrap to 0, the first line is not blanked
                // this is used by the S3 BIOS and other S3 drivers in some SVGA modes
                if ((vbend & 0x7f) == 1)
                    vblankSkip = 0;

                // it might also cut some lines off the bottom
                if (vbstart < vdend) {
                    vdend = vbstart;
                }
                Log.logging(Log.LogTypes.VGA, Log.LogServerities.Warn, "Blanking wrap to line %d",
                        vblankSkip);
            } else if (vbstart == 1) {
                // blanking is used to cut lines at the start of the screen
                vblankSkip = vbend;
                Log.logging(Log.LogTypes.VGA, Log.LogServerities.Warn,
                        "Upper %d lines of the screen blanked", vblankSkip);
            } else if (vbstart < vdend) {
                if (vbend < vdend) {
                    // the game wants a black bar somewhere on the screen
                    Log.logging(Log.LogTypes.VGA, Log.LogServerities.Warn,
                            "Unsupported blanking: line %d-%d", vbstart, vbend);
                } else {
                    // blanking is used to cut off some lines from the bottom
                    vdend = vbstart;
                }
            }
            vdend -= vblankSkip;
        }
        // Display end
        Draw.Delay.VDEnd = vdend * Draw.Delay.HTotal;

        Draw.PartsTotal = VGA_PARTS;
        /*
         * 6 Horizontal Sync Polarity. Negative if set 7 Vertical Sync Polarity. Negative if set Bit
         * 6-7 indicates the number of lines on the display: 1: 400, 2: 350, 3: 480
         */
        // Try to determine the pixel size, aspect correct is based around square pixels

        // Base pixel width around 100 clocks horizontal
        // For 9 pixel text modes this should be changed, but we don't support that
        // anyway :)
        // Seems regular vga only listens to the 9 char pixel mode with character mode
        // enabled
        double pwidth =
                (DOSBox.Machine == DOSBox.MachineType.EGA) ? (114.0 / htotal) : (100.0 / htotal);
        // Base pixel height around vertical totals of modes that have 100 clocks
        // horizontal
        // Different sync values gives different scaling of the whole vertical range
        // VGA monitor just seems to thighten or widen the whole vertical range
        double pheight;
        double targetTotal = (DOSBox.Machine == DOSBox.MachineType.EGA) ? 262.0 : 449.0;
        int sync = MiscOutput >>> 6;
        switch (sync) {
            case 0: // This is not defined in vga specs,
                // Kiet, seems to be slightly less than 350 on my monitor
                // 340 line mode, filled with 449 total
                pheight = (480.0 / 340.0) * (targetTotal / vtotal);
                break;
            case 1: // 400 line mode, filled with 449 total
                pheight = (480.0 / 400.0) * (targetTotal / vtotal);
                break;
            case 2: // 350 line mode, filled with 449 total
                // This mode seems to get regular 640x400 timing and goes for a loong retrace
                // Depends on the monitor to stretch the screen
                pheight = (480.0 / 350.0) * (targetTotal / vtotal);
                break;
            case 3: // 480 line mode, filled with 525 total
            default:
                pheight = (480.0 / 480.0) * (525.0 / vtotal);
                break;
        }

        double aspectRatio = pheight / pwidth;

        Draw.Delay.Parts = Draw.Delay.VDEnd / Draw.PartsTotal;
        Draw.Resizing = false;
        Draw.VRetTriggered = false;

        // Check to prevent useless black areas
        if (hbstart < hdend)
            hdend = hbstart;
        if ((!DOSBox.isVGAArch()) && (vbstart < vdend))
            vdend = vbstart;

        int width = hdend;
        int height = vdend;
        boolean doubleheight = false;
        boolean doublewidth = false;

        // Set the bpp
        int bpp;
        switch (Mode) {
            case LIN15:
                bpp = 15;
                break;
            case LIN16:
                bpp = 16;
                break;
            case LIN32:
                bpp = 32;
                break;
            default:
                bpp = 8;
                break;
        }
        Draw.LinearAlloc = Mem.LinearAlloc;
        Draw.LinearBase = Mem.LinearBase;
        Draw.LinearMask = VMemWrap - 1;
        switch (Mode) {
            case VGA:
                doublewidth = true;
                width <<= 2;
                if ((DOSBox.isVGAArch()) && (DOSBox.SVGACard == DOSBox.SVGACards.None)) {
                    bpp = 16;
                    VGADrawLine = this::drawXlat16LinearLine;
                } else
                    VGADrawLine = this::drawLinearLine;
                break;
            case LIN8:
                if ((Crtc.ModeControl & 0x8) != 0)
                    width >>>= 1;
                else if (DOSBox.SVGACard == DOSBox.SVGACards.S3Trio && (S3.Reg3A & 0x10) == 0) {
                    doublewidth = true;
                    width >>>= 1;
                }
                // fall-through
                // goto GotoM_LIN32;
            case LIN32:
                // GotoM_LIN32:
                width <<= 3;
                if ((Crtc.ModeControl & 0x8) != 0)
                    doublewidth = true;
                /* Use HW mouse cursor drawer if enabled */
                activateHardwareCursor();
                break;
            case LIN15:
            case LIN16:
                // 15/16 bpp modes double the horizontal values
                width <<= 2;
                if ((Crtc.ModeControl & 0x8) != 0
                        || (DOSBox.SVGACard == DOSBox.SVGACards.S3Trio && (S3.PLL.Cmd & 0x10) != 0))
                    doublewidth = true;
                /* Use HW mouse cursor drawer if enabled */
                activateHardwareCursor();
                break;
            case LIN4:
                doublewidth = (Seq.ClockingMode & 0x8) > 0;
                Draw.Blocks = width;
                width <<= 3;
                VGADrawLine = this::drawLinearLine;
                Draw.LinearAlloc = FastMemAlloc;
                Draw.LinearBase = (int) FastMemBase;
                Draw.LinearMask = (VMemWrap << 1) - 1;
                break;
            case EGA:
                doublewidth = (Seq.ClockingMode & 0x8) > 0;
                Draw.Blocks = width;
                width <<= 3;
                if ((DOSBox.isVGAArch()) && (DOSBox.SVGACard == DOSBox.SVGACards.None)) {
                    bpp = 16;
                    VGADrawLine = this::drawXlat16LinearLine;
                } else
                    VGADrawLine = this::drawLinearLine;

                Draw.LinearAlloc = FastMemAlloc;
                Draw.LinearBase = (int) FastMemBase;
                Draw.LinearMask = (VMemWrap << 1) - 1;
                break;
            case CGA16:
                doubleheight = true;
                Draw.Blocks = width * 2;
                width <<= 4;
                VGADrawLine = this::drawCGA16Line;
                break;
            case CGA4:
                doublewidth = true;
                Draw.Blocks = width * 2;
                width <<= 3;
                VGADrawLine = this::draw2BPPLine;
                break;
            case CGA2:
                doubleheight = true;
                Draw.Blocks = 2 * width;
                width <<= 3;
                VGADrawLine = this::draw1BPPLine;
                break;
            case TEXT:
                aspectRatio = 1.0;
                Draw.Blocks = width;
                doublewidth = (Seq.ClockingMode & 0x8) > 0;
                if ((DOSBox.isVGAArch()) && (DOSBox.SVGACard == DOSBox.SVGACards.None)
                        && (Seq.ClockingMode & 0x01) == 0) {
                    width *= 9; /* 9 bit wide text font */
                    VGADrawLine = this::drawLine9TEXTXlat16;
                    bpp = 16;
                    // VGA_DrawLine=VGA_TEXT_Draw_Line_9;
                } else {
                    width <<= 3; /* 8 bit wide text font */
                    if ((DOSBox.isVGAArch()) && (DOSBox.SVGACard == DOSBox.SVGACards.None)) {
                        VGADrawLine = this::drawLineTEXTXlat16;
                        bpp = 16;
                    } else
                        VGADrawLine = this::drawLineTEXT;
                }
                break;
            case HERC_GFX:
                aspectRatio = 1.5;
                Draw.Blocks = width * 2;
                width *= 16;
                VGADrawLine = this::draw1BPPLine;
                break;
            case TANDY2:
                aspectRatio = 1.2;
                doubleheight = true;
                if (DOSBox.Machine == DOSBox.MachineType.PCJR)
                    doublewidth = (Tandy.GFXControl & 0x8) == 0x00;
                else
                    doublewidth = (Tandy.ModeControl & 0x10) == 0;
                Draw.Blocks = width * (doublewidth ? 4 : 8);
                width = Draw.Blocks * 2;
                VGADrawLine = this::draw1BPPLine;
                break;
            case TANDY4:
                aspectRatio = 1.2;
                doubleheight = true;
                if (DOSBox.Machine == DOSBox.MachineType.TANDY)
                    doublewidth = (Tandy.ModeControl & 0x10) == 0;
                else
                    doublewidth = (Tandy.ModeControl & 0x01) == 0x00;
                Draw.Blocks = width * 2;
                width = Draw.Blocks * 4;
                if ((DOSBox.Machine == DOSBox.MachineType.TANDY && (Tandy.GFXControl & 0x8) != 0)
                        || (DOSBox.Machine == DOSBox.MachineType.PCJR
                                && (Tandy.ModeControl == 0x0b)))
                    VGADrawLine = this::draw2BPPHiResLine;
                else
                    VGADrawLine = this::draw2BPPLine;
                break;
            case TANDY16:
                aspectRatio = 1.2;
                doubleheight = true;
                Draw.Blocks = width * 2;
                if ((Tandy.ModeControl & 0x1) != 0) {
                    if ((DOSBox.Machine == DOSBox.MachineType.TANDY)
                            && ((Tandy.ModeControl & 0x10) != 0)) {
                        doublewidth = false;
                        Draw.Blocks *= 2;
                        width = Draw.Blocks * 2;
                    } else {
                        doublewidth = true;
                        width = Draw.Blocks * 2;
                    }
                    VGADrawLine = this::draw4BPPLine;
                } else {
                    doublewidth = true;
                    width = Draw.Blocks * 4;
                    VGADrawLine = this::draw4BPPLineDouble;
                }
                break;
            case TANDY_TEXT:
                doublewidth = (Tandy.ModeControl & 0x1) == 0;
                aspectRatio = 1;
                doubleheight = true;
                Draw.Blocks = width;
                width <<= 3;
                VGADrawLine = this::drawLineTEXT;
                break;
            case HERC_TEXT:
                aspectRatio = 1;
                Draw.Blocks = width;
                width <<= 3;
                VGADrawLine = this::drawLineTEXTHerc;
                break;
            default:
                Log.logging(Log.LogTypes.VGA, Log.LogServerities.Error,
                        "Unhandled VGA mode %d while checking for resolution", Mode.toString());
                break;
        }
        checkScanLength();
        if (Draw.DoubleScan) {
            if (DOSBox.isVGAArch()) {
                Draw.VBlankSkip /= 2;
                height /= 2;
            }
            doubleheight = true;
        }
        Draw.VBlankSkip = vblankSkip;

        if (!(DOSBox.isVGAArch() && (DOSBox.SVGACard == DOSBox.SVGACards.None)
                && (Mode == VGAModes.EGA || Mode == VGAModes.VGA))) {
            // Only check for extra double height in vga modes
            // (line multiplying by address_line_total)
            if (!doubleheight && (Mode.toValue() < VGAModes.TEXT.toValue())
                    && (Draw.AddressLineTotal & 1) == 0) {
                Draw.AddressLineTotal /= 2;
                doubleheight = true;
                height /= 2;
            }
        }
        Draw.LinesTotal = height;
        Draw.PartsLines = Draw.LinesTotal / Draw.PartsTotal;
        Draw.LineLength = width * ((bpp + 1) / 8);
        /*
         * Cheap hack to just make all > 640x480 modes have 4:3 aspect ratio
         */
        if (width >= 640 && height >= 480) {
            aspectRatio = ((float) width / (float) height) * (3.0 / 4.0);
        }
        // LOG_MSG("ht %d vt %d ratio %f", htotal, vtotal, aspect_ratio );

        // need to change the vertical timing?
        if (Math.abs(Draw.Delay.VTotal - 1000.0 / fps) > 0.0001) {
            Draw.Delay.VTotal = 1000.0 / fps;
            killDrawing();
            PIC.removeEvents(otherVertInterruptWrap);
            PIC.removeEvents(verticalTimerWrap);
            PIC.removeEvents(panningLatchWrap);
            PIC.removeEvents(vgaDisplayStartLatchWrap);
            verticalTimer(0);
        }
        /*
         * #if C_DEBUG LOG(LOG_VGA,LOG_NORMAL)
         * ("h total %2.5f (%3.2fkHz) blank(%02.5f/%02.5f) retrace(%02.5f/%02.5f)",
         * vga.draw.delay.htotal,(1.0/vga.draw.delay.htotal),
         * vga.draw.delay.hblkstart,vga.draw.delay.hblkend,
         * vga.draw.delay.hrstart,vga.draw.delay.hrend); LOG(LOG_VGA,LOG_NORMAL)
         * ("v total %2.5f (%3.2fHz) blank(%02.5f/%02.5f) retrace(%02.5f/%02.5f)",
         * vga.draw.delay.vtotal,(1000.0/vga.draw.delay.vtotal),
         * vga.draw.delay.vblkstart,vga.draw.delay.vblkend,
         * vga.draw.delay.vrstart,vga.draw.delay.vrend); #endif
         */
        // need to resize the output window?
        if ((width != Draw.Width) || (height != Draw.Height) || (Draw.DoubleWidth != doublewidth)
                || (Draw.DoubleHeight != doubleheight)
                || (Math.abs(aspectRatio - Draw.AspectRatio) > 0.0001) || (Draw.Bpp != bpp)) {

            killDrawing();

            Draw.Width = width;
            Draw.Height = height;
            Draw.DoubleWidth = doublewidth;
            Draw.DoubleHeight = doubleheight;
            Draw.AspectRatio = aspectRatio;
            Draw.Bpp = bpp;
            if (doubleheight)
                Draw.LinesScaled = 2;
            else
                Draw.LinesScaled = 1;
            /*
             * #if C_DEBUG LOG(LOG_VGA,LOG_NORMAL)("Width %d, Height %d, fps %f",width,height,fps);
             * LOG(LOG_VGA,LOG_NORMAL)("%s width, %s height aspect %f", doublewidth ?
             * "double":"normal",doubleheight ? "double":"normal",aspect_ratio); #endif
             */
            Render.instance().setSize(width, height, bpp, (float) fps, aspectRatio, doublewidth,
                    doubleheight);
            // System.Windows.Application.Current.Dispatcher.Invoke(
            // new Action(()=> Render.Get().RENDER_SetSize(width, height, bpp, (float)fps,
            // aspect_ratio, doublewidth, doubleheight))
            // );
        }
    }

    public void killDrawing() {
        PIC.removeEvents(drawPartWrap);
        PIC.removeEvents(drawSingleLineWrap);
        Draw.PartsLeft = 0;
        Draw.LinesDone = ~0;
        Render.instance().endUpdate(true);
    }

    /*--------------------------- end VGADraw -----------------------------*/

    /*--------------------------- begin VGAMemory -----------------------------*/
    // 일단 VGARAM_CHECKED 옵션을 켜는 걸로 진행
    // #if C_VGARAM_CHECKED
    // Checked linear offset
    public int checked(int v) {
        return ((v) & (VMemWrap - 1));
    }

    // Checked planar offset (latched access)
    public int checked2(int v) {
        return (v & ((VMemWrap >>> 2) - 1));
    }
    // #else
    // #define CHECKED(v) (v)
    // #define CHECKED2(v) (v)
    // #endif

    // Nice one from DosEmu
    private int rasterOp(int input, int mask) {
        switch (Config.RasterOp) {
            case 0x00: /* None */
                return (input & mask) | (Latch.d() & ~mask);
            case 0x01: /* AND */
                return (input | ~mask) & Latch.d();
            case 0x02: /* OR */
                return (input & mask) | Latch.d();
            case 0x03: /* XOR */
                return (input & mask) ^ Latch.d();
        }
        return 0;
    }

    // int(byte)
    public int modeOperation(int val) {
        int full;
        switch (Config.WriteMode) {
            case 0x00:
                // Write Mode 0: In this mode, the host data is first rotated as per the Rotate
                // Count field, then the Enable Set/Reset mechanism selects data from this or
                // the Set/Reset field. Then the selected Logical Operation is performed on the
                // resulting data and the data in the latch register. Then the Bit Mask field is
                // used to select which bits come from the resulting data and which come from
                // the latch register. Finally, only the bit planes enabled by the Memory Plane
                // Write Enable field are written to memory.
                val = 0xff & ((val >>> Config.DataRotate) | (val << (8 - Config.DataRotate)));
                full = ExpandTable[val];
                full = (full & Config.FullNotEnableSetreset) | Config.FullEnableAndSetReset;
                full = rasterOp(full, Config.FullBitMask);
                break;
            case 0x01:
                // Write Mode 1: In this mode, data is transferred directly from the 32 bit
                // latch register to display memory, affected only by the Memory Plane Write
                // Enable field. The host data is not used in this mode.
                full = Latch.d();
                break;
            case 0x02:
                // Write Mode 2: In this mode, the bits 3-0 of the host data are replicated
                // across all 8 bits of their respective planes. Then the selected Logical
                // Operation is performed on the resulting data and the data in the latch
                // register. Then the Bit Mask field is used to select which bits come from the
                // resulting data and which come from the latch register. Finally, only the bit
                // planes enabled by the Memory Plane Write Enable field are written to memory.
                full = rasterOp(FillTable[val & 0xF], Config.FullBitMask);
                break;
            case 0x03:
                // Write Mode 3: In this mode, the data in the Set/Reset field is used as if the
                // Enable Set/Reset field were set to 1111b. Then the host data is first rotated
                // as per the Rotate Count field, then logical ANDed with the value of the Bit
                // Mask field. The resulting value is used on the data obtained from the
                // Set/Reset field in the same way that the Bit Mask field would ordinarily be
                // used. to select which bits come from the expansion of the Set/Reset field and
                // which come from the latch register. Finally, only the bit planes enabled by
                // the Memory Plane Write Enable field are written to memory.
                val = 0xff & ((val >>> Config.DataRotate) | (val << (8 - Config.DataRotate)));
                full = rasterOp(Config.FullSetReset, ExpandTable[val] & Config.FullBitMask);
                break;
            default:
                Log.logging(Log.LogTypes.VGAMISC, Log.LogServerities.Normal,
                        "VGA:Unsupported write mode %d", Config.WriteMode);
                full = 0;
                break;
        }
        return full;
    }

    /* Gonna assume that whoever maps vga memory, maps it on 32/64kb boundary */

    private static final int VGA_PAGES = (128 / 4);
    private static final int VGA_PAGE_A0 = (0xA0000 / 4096);
    private static final int VGA_PAGE_B0 = (0xB0000 / 4096);
    private static final int VGA_PAGE_B8 = (0xB8000 / 4096);

    public int PageBase, PageMask;

    private static class VGAPageHandler {
        public MapHandler Map;
        public ChangesHandler Changes;
        public TextPageHandler Text;
        public TandyPageHandler Tandy;
        public ChainedEGAHandler ChainedEGA;
        public ChainedVGAHandler ChainedVGA;
        public UnchainedEGAHandler UnchainedEGA;
        public UnchainedVGAHandler UnchainedVGA;
        public PCJrHandler PCjr;
        public LIN4Handler Lin4;
        public LFBHandler Lfb;
        public LFBChangesHandler LfbChanges;
        public MMIOHandler MMIO;
        public EmptyHandler Empty;
    }

    VGAPageHandler vgaPageHandler = null;

    private void changedBank() {

        setupHandlers();
    }

    public void setupHandlers() {
        SVGA.BankFeadFull = SVGA.BankRead * SVGA.BankSize;
        SVGA.BankWriteFull = SVGA.BankWrite * SVGA.BankSize;

        PageHandler newHandler;
        switch (DOSBox.Machine) {
            case CGA:
            case PCJR:
                Memory.setPageHandler(VGA_PAGE_B8, 8, vgaPageHandler.PCjr);
                // goto range_done;
                doneRange();
                return;
            case HERC:
                PageBase = VGA_PAGE_B0;
                if ((Herc.EnableBits & 0x2) != 0) {
                    PageMask = 0xffff;
                    Memory.setPageHandler(VGA_PAGE_B0, 16, vgaPageHandler.Map);
                } else {
                    PageMask = 0x7fff;
                    /* With hercules in 32kb mode it leaves a memory hole on 0xb800 */
                    Memory.setPageHandler(VGA_PAGE_B0, 8, vgaPageHandler.Map);
                    Memory.setPageHandler(VGA_PAGE_B8, 8, vgaPageHandler.Empty);
                }
                // goto range_done;
                doneRange();
                return;
            case TANDY:
                /* Always map 0xa000 - 0xbfff, might overwrite 0xb800 */
                PageBase = VGA_PAGE_A0;
                PageMask = 0x1ffff;
                Memory.setPageHandler(VGA_PAGE_A0, 32, vgaPageHandler.Map);
                if ((Tandy.ExtendedRam & 1) != 0) {
                    // You seem to be able to also map different 64kb banks,
                    // but have to figure that out This seems to work so far though
                    Tandy.DrawBase = Mem.LinearBase;
                    Tandy.DrawAlloc = Mem.LinearAlloc;
                    Tandy.MemBase = Mem.LinearBase;
                    Tandy.MemAlloc = Mem.LinearAlloc;
                } else {
                    // vga.tandy.draw_base = TANDY_VIDBASE( vga.tandy.draw_bank * 16 * 1024);
                    Tandy.DrawBase = Memory.MemBase + 0x80000 + Tandy.DrawBank * 16 * 1024;
                    Tandy.DrawAlloc = Memory.getMemAlloc();
                    // vga.tandy.mem_base = TANDY_VIDBASE( vga.tandy.mem_bank * 16 * 1024);
                    Tandy.MemBase = Memory.MemBase + 0x80000 + Tandy.MemBank * 16 * 1024;
                    Tandy.MemAlloc = Memory.getMemAlloc();
                    Memory.setPageHandler(0xb8, 8, vgaPageHandler.Tandy);
                }
                // goto range_done;
                doneRange();
                return;
            // MEM_SetPageHandler(vga.tandy.mem_bank<<2,vga.tandy.is_32k_mode ? 0x08 :
            // 0x04,range_handler);
            case EGA:
            case VGA:
                break;
            default:
                Log.logMsg("Illegal dosbox.machine type %d", DOSBox.Machine);
                return;
        }

        /* This should be vga only */
        switch (Mode) {
            case ERROR:
            default:
                return;
            case LIN4:
                newHandler = vgaPageHandler.Lin4;
                break;
            case LIN15:
            case LIN16:
            case LIN32:

                newHandler = vgaPageHandler.Map;

                break;
            case LIN8:
            case VGA:
                if (Config.Chained) {
                    if (Config.CompatibleChain4)
                        newHandler = vgaPageHandler.ChainedVGA;
                    else

                        newHandler = vgaPageHandler.Map;

                } else {
                    newHandler = vgaPageHandler.UnchainedVGA;
                }
                break;
            case EGA:
                if (Config.Chained)
                    newHandler = vgaPageHandler.ChainedEGA;
                else
                    newHandler = vgaPageHandler.UnchainedEGA;
                break;
            case TEXT:
                /* Check if we're not in odd/even mode */
                if ((GFX.Miscellaneous & 0x2) != 0)
                    newHandler = vgaPageHandler.Map;
                else
                    newHandler = vgaPageHandler.Text;
                break;
            case CGA4:
            case CGA2:
                newHandler = vgaPageHandler.Map;
                break;
        }
        switch ((GFX.Miscellaneous >>> 2) & 3) {
            case 0:
                PageBase = VGA_PAGE_A0;
                switch (DOSBox.SVGACard) {
                    case TsengET3K:
                    case TsengET4K:
                        PageMask = 0xffff;
                        break;
                    case S3Trio:
                    default:
                        PageMask = 0x1ffff;
                        break;
                }
                Memory.setPageHandler(VGA_PAGE_A0, 32, newHandler);
                break;
            case 1:
                PageBase = VGA_PAGE_A0;
                PageMask = 0xffff;
                Memory.setPageHandler(VGA_PAGE_A0, 16, newHandler);
                Memory.resetPageHandler(VGA_PAGE_B0, 16);
                break;
            case 2:
                PageBase = VGA_PAGE_B0;
                PageMask = 0x7fff;
                Memory.setPageHandler(VGA_PAGE_B0, 8, newHandler);
                Memory.resetPageHandler(VGA_PAGE_A0, 16);
                Memory.resetPageHandler(VGA_PAGE_B8, 8);
                break;
            case 3:
                PageBase = VGA_PAGE_B8;
                PageMask = 0x7fff;
                Memory.setPageHandler(VGA_PAGE_B8, 8, newHandler);
                Memory.resetPageHandler(VGA_PAGE_A0, 16);
                Memory.resetPageHandler(VGA_PAGE_B0, 8);
                break;
        }
        if (DOSBox.SVGACard == DOSBox.SVGACards.S3Trio && (S3.ExtMemCtrl & 0x10) != 0)
            Memory.setPageHandler(VGA_PAGE_A0, 16, vgaPageHandler.MMIO);
        doneRange();
        // range_done:
        // Paging.ClearTLB();
    }

    private void doneRange() {
        Paging.clearTLB();
    }

    public void startUpdateLFB() {
        Lfb.Page = S3.LaWindow << 4;
        Lfb.Addr = S3.LaWindow << 16;

        Lfb.Handler = vgaPageHandler.Lfb;

        Memory.setLFB(S3.LaWindow << 4, VMemSize / 4096, Lfb.Handler, vgaPageHandler.MMIO);
    }

    private void shutdownMemory(Section sec) {
        Mem.LinearAlloc = null;
        FastMemAlloc = null;

    }

    private void setupMemory(Section sec) {
        SVGA.BankRead = SVGA.BankWrite = 0;
        SVGA.BankFeadFull = SVGA.BankWriteFull = 0;

        int vgaAllocsize = VMemSize;
        // Keep lower limit at 512k
        if (vgaAllocsize < 512 * 1024)
            vgaAllocsize = 512 * 1024;
        // We reserve extra 2K for one scan line
        vgaAllocsize += 2048;
        // vga.mem.linear_orgptr = new Bit8u[vga_allocsize + 16];
        // vga.mem.linear = (Bit8u*)(((Bitu)vga.mem.linear_orgptr + 16 - 1) & ~(16 -
        // 1));
        // 15비트 이동한 다음 주소숫자의 뒷자리 15비트부분을 깨끗하게 지우는 작업을 왜 하는지?
        // 일단 ,vga.mem.linear_orgptr없이 바로 할당

        // 첫번째 번지를 가리키는 인덱스 0은 포인터가 0인 것처럼 사용하므로 사용하지 않음
        // 1바이트 더 크게 배열을 할당
        Mem.LinearAlloc = new byte[vgaAllocsize + 1 + 16];
        Mem.LinearBase = 1; // 첫번째 바이트는 빼고 계산
        Arrays.fill(Mem.LinearAlloc, 0, vgaAllocsize + 1, (byte) 0);

        // 15비트 이동한 다음 주소숫자의 뒷자리 15비트부분을 깨끗하게 지우는 작업을 왜 하는지?
        // 일단, vga.fastmem_orgptr 없이 바로 할당
        // vga.fastmem_orgptr = new byte[(vga.vmemsize<<1)+4096+16];
        FastMemAlloc = new byte[(VMemSize << 1) + 4096 + 1 + 16];
        FastMemBase = 1;// 첫번째 바이트는 빼고 계산

        // In most cases these values stay the same. Assumptions: vmemwrap is power of
        // 2,
        // vmemwrap <= vmemsize, fastmem implicitly has mem wrap twice as big
        VMemWrap = VMemSize;

        SVGA.BankRead = SVGA.BankWrite = 0;
        SVGA.BankFeadFull = SVGA.BankWriteFull = 0;
        SVGA.BankSize = 0x10000; /* most common bank size is 64K */

        sec.addDestroyFunction(this::shutdownMemory);

        if (DOSBox.Machine == DOSBox.MachineType.PCJR) {
            /*
             * PCJr does not have dedicated graphics memory but uses conventional memory below 128k
             */
            // TODO map?
        }
    }

    /*--------------------------- end VGAMemory -----------------------------*/
    /*--------------------------- begin VGAMisc -----------------------------*/
    public int readP3DA(int port, int iolen) {
        int retval = 0;
        double timeInFrame = PIC.getFullIndex() - Draw.Delay.FrameStart;

        Internal.AttrIndex = false;
        Tandy.PcjrFlipFlop = 0;

        // 3DAh (R): Status Register
        // bit 0 Horizontal or Vertical blanking
        // 3 Vertical sync

        if (timeInFrame >= Draw.Delay.VRStart && timeInFrame <= Draw.Delay.VREnd)
            retval |= 8;
        if (timeInFrame >= Draw.Delay.VDEnd) {
            retval |= 1;
        } else {
            double timeInLine = timeInFrame % Draw.Delay.HTotal;
            if (timeInLine >= Draw.Delay.HBlkStart && timeInLine <= Draw.Delay.HBlkEnd) {
                retval |= 1;
            }
        }
        return retval;
    }

    private void writeP3C2(int port, int val, int iolen) {
        MiscOutput = 0xff & val;
        if ((val & 0x1) != 0) {
            IO.registerWriteHandler(0x3d4, this.Crtc.writeP3D4Wrap, IO.IO_MB);
            IO.registerReadHandler(0x3d4, this.Crtc.readP3D4Wrap, IO.IO_MB);
            IO.registerReadHandler(0x3da, this::readP3DA, IO.IO_MB);

            IO.registerWriteHandler(0x3d5, this.Crtc.writeP3D5Wrap, IO.IO_MB);
            IO.registerReadHandler(0x3d5, this.Crtc.readP3D5Wrap, IO.IO_MB);

            IO.freeWriteHandler(0x3b4, IO.IO_MB);
            IO.freeReadHandler(0x3b4, IO.IO_MB);
            IO.freeWriteHandler(0x3b5, IO.IO_MB);
            IO.freeReadHandler(0x3b5, IO.IO_MB);
            IO.freeReadHandler(0x3ba, IO.IO_MB);
        } else {
            IO.registerWriteHandler(0x3b4, this.Crtc.writeP3D4Wrap, IO.IO_MB);
            IO.registerReadHandler(0x3b4, this.Crtc.readP3D4Wrap, IO.IO_MB);
            IO.registerReadHandler(0x3ba, this::readP3DA, IO.IO_MB);

            IO.registerWriteHandler(0x3b5, this.Crtc.writeP3D5Wrap, IO.IO_MB);
            IO.registerReadHandler(0x3b5, this.Crtc.readP3D5Wrap, IO.IO_MB);

            IO.freeWriteHandler(0x3d4, IO.IO_MB);
            IO.freeReadHandler(0x3d4, IO.IO_MB);
            IO.freeWriteHandler(0x3d5, IO.IO_MB);
            IO.freeReadHandler(0x3d5, IO.IO_MB);
            IO.freeReadHandler(0x3da, IO.IO_MB);
        }
        /*
         * 0 If set Color Emulation. Base Address=3Dxh else Mono Emulation. Base Address=3Bxh. 2-3
         * Clock Select. 0: 25MHz, 1: 28MHz 5 When in Odd/Even modes Select High 64k bank if set 6
         * Horizontal Sync Polarity. Negative if set 7 Vertical Sync Polarity. Negative if set Bit
         * 6-7 indicates the number of lines on the display: 1: 400, 2: 350, 3: 480 Note: Set to all
         * zero on a hardware reset. Note: This register can be read from port 3CCh.
         */
    }

    private int readP3CC(int port, int iolen) {
        return MiscOutput;
    }

    // VGA feature control register
    private int readP3CA(int port, int iolen) {
        return 0;
    }

    // read_p3c8
    private int readP3C8Misc(int port, int iolen) {
        return 0x10;
    }

    private int readP3C2(int port, int iolen) {
        int retval = 0;

        if (DOSBox.Machine == DOSBox.MachineType.EGA)
            retval = 0x0F;
        else if (DOSBox.isVGAArch())
            retval = 0x60;
        if ((DOSBox.Machine == DOSBox.MachineType.VGA) || (((MiscOutput >>> 2) & 3) == 0)
                || (((MiscOutput >>> 2) & 3) == 3)) {
            retval |= 0x10;
        }

        if (Draw.VRetTriggered)
            retval |= 0x80;
        return retval;
        /*
         * 0-3 0xF on EGA, 0x0 on VGA 4 Status of the switch selected by the Miscellaneous Output
         * Register 3C2h bit 2-3. Switch high if set. (apparently always 1 on VGA) 5 (EGA) Pin 19 of
         * the Feature Connector (FEAT0) is high if set 6 (EGA) Pin 17 of the Feature Connector
         * (FEAT1) is high if set (default differs by card, ET4000 sets them both) 7 If set IRQ 2
         * has happened due to Vertical Retrace. Should be cleared by IRQ 2 interrupt routine by
         * clearing port 3d4h index 11h bit 4.
         */
    }

    private void setupMisc() {
        if (DOSBox.isEGAVGAArch()) {
            Draw.VRetTriggered = false;
            IO.registerReadHandler(0x3c2, this::readP3C2, IO.IO_MB);
            IO.registerWriteHandler(0x3c2, this::writeP3C2, IO.IO_MB);
            if (DOSBox.isVGAArch()) {
                IO.registerReadHandler(0x3ca, this::readP3CA, IO.IO_MB);
                IO.registerReadHandler(0x3cc, this::readP3CC, IO.IO_MB);
            } else {
                IO.registerReadHandler(0x3c8, this::readP3C8Misc, IO.IO_MB);
            }
        } else if (DOSBox.Machine == DOSBox.MachineType.CGA || DOSBox.isTANDYArch()) {
            IO.registerReadHandler(0x3da, this::readP3DA, IO.IO_MB);
        }
    }

    /*--------------------------- end VGAMisc -----------------------------*/
    /*--------------------------- begin VGAOther -----------------------------*/
    private void writeCrtcIndexOther(int port, int val, int iolen) {
        Other.Index = val;
    }

    private int readCrtcIndexOther(int port, int iolen) {
        return Other.Index;
    }

    private void writeCrtcDataOther(int port, int val, int iolen) {
        switch (Other.Index) {
            case 0x00: // Horizontal total
                if ((Other.HTotal ^ val) != 0)
                    startResize();
                Other.HTotal = 0xff & val;
                break;
            case 0x01: // Horizontal displayed chars
                if ((Other.HdEnd ^ val) != 0)
                    startResize();
                Other.HdEnd = 0xff & val;
                break;
            case 0x02: // Horizontal sync position
                Other.HSyncP = 0xff & val;
                break;
            case 0x03: // Horizontal sync width
                if (DOSBox.Machine == DOSBox.MachineType.TANDY)
                    Other.VSyncW = 0xff & (val >>> 4);
                else
                    Other.VSyncW = 16; // The MC6845 has a fixed v-sync width of 16 lines
                Other.HSyncW = val & 0xf;
                break;
            case 0x04: // Vertical total
                if ((Other.VTotal ^ val) != 0)
                    startResize();
                Other.VTotal = 0xff & val;
                break;
            case 0x05: // Vertical display adjust
                if ((Other.VAdjust ^ val) != 0)
                    startResize();
                Other.VAdjust = 0xff & val;
                break;
            case 0x06: // Vertical rows
                if ((Other.VdEnd ^ val) != 0)
                    startResize();
                Other.VdEnd = 0xff & val;
                break;
            case 0x07: // Vertical sync position
                Other.VSyncP = 0xff & val;
                break;
            case 0x09: // Max scanline
                val &= 0x1f; // VGADOC says bit 0-3 but the MC6845 datasheet says bit 0-4
                if ((Other.MaxScanLine ^ val) != 0)
                    startResize();
                Other.MaxScanLine = 0xff & val;
                break;
            case 0x0A: /* Cursor Start Register */
                Other.CursorStart = val & 0x3f;
                Draw.Cursor.SLine = val & 0x1f;
                Draw.Cursor.Enabled = (val & 0x60) != 0x20 ? 1 : 0;
                break;
            case 0x0B: /* Cursor End Register */
                Other.CursorEnd = val & 0x1f;
                Draw.Cursor.ELine = val & 0x1f;
                break;
            case 0x0C: /* Start Address High Register */
                Config.DisplayStart = (Config.DisplayStart & 0x00FF) | (val << 8);
                break;
            case 0x0D: /* Start Address Low Register */
                Config.DisplayStart = (Config.DisplayStart & 0xFF00) | val;
                break;
            case 0x0E: /* Cursor Location High Register */
                Config.CursorStart &= 0x00ff;
                Config.CursorStart |= (0xff & val) << 8;
                break;
            case 0x0F: /* Cursor Location Low Register */
                Config.CursorStart &= 0xff00;
                Config.CursorStart |= 0xff & val;
                break;
            case 0x10: /* Light Pen High */
                Other.LightPen &= 0xff;
                Other.LightPen |= 0xffff & ((val & 0x3f) << 8); // only 6 bits
                break;
            case 0x11: /* Light Pen Low */
                Other.LightPen &= 0xff00;
                Other.LightPen |= 0xff & val;
                break;
            default:
                Log.logging(Log.LogTypes.VGAMISC, Log.LogServerities.Normal,
                        "MC6845:Write %X to illegal index %x", val, Other.Index);
                break;
        }
    }

    private int readCrtcDataOther(int port, int iolen) {
        switch (Other.Index) {
            case 0x00: // Horizontal total
                return Other.HTotal;
            case 0x01: // Horizontal displayed chars
                return Other.HdEnd;
            case 0x02: // Horizontal sync position
                return Other.HSyncP;
            case 0x03: // Horizontal and vertical sync width
                if (DOSBox.Machine == DOSBox.MachineType.TANDY)
                    return Other.HSyncW | (Other.VSyncW << 4);
                else
                    return Other.HSyncW;
            case 0x04: // Vertical total
                return Other.VTotal;
            case 0x05: // Vertical display adjust
                return Other.VAdjust;
            case 0x06: // Vertical rows
                return Other.VdEnd;
            case 0x07: // Vertical sync position
                return Other.VSyncP;
            case 0x09: // Max scanline
                return Other.MaxScanLine;
            case 0x0A: /* Cursor Start Register */
                return Other.CursorStart;
            case 0x0B: /* Cursor End Register */
                return Other.CursorEnd;
            case 0x0C: /* Start Address High Register */
                return 0xff & (Config.DisplayStart >>> 8);
            case 0x0D: /* Start Address Low Register */
                return Config.DisplayStart & 0xff;
            case 0x0E: /* Cursor Location High Register */
                return 0xff & (Config.CursorStart >>> 8);
            case 0x0F: /* Cursor Location Low Register */
                return Config.CursorStart & 0xff;
            case 0x10: /* Light Pen High */
                return 0xff & (Other.LightPen >>> 8);
            case 0x11: /* Light Pen Low */
                return Other.LightPen & 0xff;
            default:
                Log.logging(Log.LogTypes.VGAMISC, Log.LogServerities.Normal,
                        "MC6845:Read from illegal index %x", Other.Index);
                break;
        }
        return ~0;
    }

    private double hueOffset = 0.0;
    private int cga16Val = 0;// uint8
    private int hercPal = 0;// uint8

    private void selectCga16Color(int val) {
        cga16Val = val;
        updateCGA16Color();
    }

    private void updateCGA16Color() {
        // Algorithm provided by NewRisingSun
        // His/Her algorithm is more complex and gives better results than the one below
        // However that algorithm doesn't fit in our vga pallette.
        // Therefore a simple variant is used, but the colours are bit lighter.

        // It uses an avarage over the bits to give smooth transitions from colour to
        // colour
        // This is represented by the j variable. The i variable gives the 16 colours
        // The draw handler calculates the needed avarage and combines this with the
        // colour
        // to match an entry that is generated here.

        int baseR = 0, baseG = 0, baseB = 0;
        double sinhue, coshue, hue, basehue = 50.0;
        double I, Q, Y, pixelI, pixelQ, R, G, B;
        int colorBit1, colorBit2, colorBit3, colorBit4, index;

        if ((cga16Val & 0x01) != 0)
            baseB += 0xa8;
        if ((cga16Val & 0x02) != 0)
            baseG += 0xa8;
        if ((cga16Val & 0x04) != 0)
            baseR += 0xa8;
        if ((cga16Val & 0x08) != 0) {
            baseR += 0x57;
            baseG += 0x57;
            baseB += 0x57;
        }
        if ((cga16Val & 0x20) != 0)
            basehue = 35.0;

        hue = (basehue + hueOffset) * 0.017453239;
        sinhue = Math.sin(hue);
        coshue = Math.cos(hue);

        for (int i = 0; i < 16; i++) {
            for (int j = 0; j < 5; j++) {
                index = 0x80 | (j << 4) | i; // use upperpart of vga pallette
                colorBit4 = (i & 1) >>> 0;
                colorBit3 = (i & 2) >>> 1;
                colorBit2 = (i & 4) >>> 2;
                colorBit1 = (i & 8) >>> 3;

                // calculate lookup table
                I = 0;
                Q = 0;
                I += (double) colorBit1;
                Q += (double) colorBit2;
                I -= (double) colorBit3;
                Q -= (double) colorBit4;
                Y = (double) j / 4.0; // calculated avarage is over 4 bits

                pixelI = I * 1.0 / 3.0; // I* tvSaturnation / 3.0
                pixelQ = Q * 1.0 / 3.0; // Q* tvSaturnation / 3.0
                I = pixelI * coshue + pixelQ * sinhue;
                Q = pixelQ * coshue - pixelI * sinhue;

                R = Y + 0.956 * I + 0.621 * Q;
                if (R < 0.0)
                    R = 0.0;
                if (R > 1.0)
                    R = 1.0;
                G = Y - 0.272 * I - 0.647 * Q;
                if (G < 0.0)
                    G = 0.0;
                if (G > 1.0)
                    G = 1.0;
                B = Y - 1.105 * I + 1.702 * Q;
                if (B < 0.0)
                    B = 0.0;
                if (B > 1.0)
                    B = 1.0;

                // Render.Obj.SetPal((byte)index, (byte)(R * baseR), (byte)(G * baseG), (byte)(B *
                // baseB));
                Render.instance().setPal(index, 0xff & ((int) (R * baseR)),
                        0xff & ((int) (G * baseG)), 0xff & ((int) (B * baseB)));
            }
        }
    }

    private void increaseHue(boolean pressed) {
        if (!pressed)
            return;
        hueOffset += 5.0;
        updateCGA16Color();
        Log.logMsg("Hue at %f", hueOffset);
    }

    private void decreaseHue(boolean pressed) {
        if (!pressed)
            return;
        hueOffset -= 5.0;
        updateCGA16Color();
        Log.logMsg("Hue at %f", hueOffset);
    }

    // (uint8)
    private void writeColorSelect(int val) {
        Tandy.ColorSelect = 0xff & val;
        switch (Mode) {
            case TANDY2:
                setCGA2Table(0, val & 0xf);
                break;
            case TANDY4: {
                if ((DOSBox.Machine == DOSBox.MachineType.TANDY && (Tandy.GFXControl & 0x8) != 0)
                        || (DOSBox.Machine == DOSBox.MachineType.PCJR
                                && (Tandy.ModeControl == 0x0b))) {
                    setCGA4Table(0, 1, 2, 3);
                    return;
                }
                int Base = (val & 0x10) != 0 ? 0x08 : 0;
                /* Check for BW Mode */
                if ((Tandy.ModeControl & 0x4) != 0) {
                    setCGA4Table(val & 0xf, 3 + Base, 4 + Base, 7 + Base);
                } else {
                    if ((val & 0x20) != 0)
                        setCGA4Table(val & 0xf, 3 + Base, 5 + Base, 7 + Base);
                    else
                        setCGA4Table(val & 0xf, 2 + Base, 4 + Base, 6 + Base);
                }
            }
                break;
            case CGA16:
                selectCga16Color(val);
                break;
            case TEXT:
            case TANDY16:
                break;
        }
    }

    private void findModeTANDY() {
        if ((Tandy.ModeControl & 0x2) != 0) {
            if ((Tandy.GFXControl & 0x10) != 0)
                setMode(VGAModes.TANDY16);
            else if ((Tandy.GFXControl & 0x08) != 0)
                setMode(VGAModes.TANDY4);
            else if ((Tandy.ModeControl & 0x10) != 0)
                setMode(VGAModes.TANDY2);
            else
                setMode(VGAModes.TANDY4);
            writeColorSelect(Tandy.ColorSelect);
        } else {
            setMode(VGAModes.TANDY_TEXT);
        }
    }

    private void findModePCJr() {
        if ((Tandy.ModeControl & 0x2) != 0) {
            if ((Tandy.ModeControl & 0x10) != 0) {
                /* bit4 of mode control 1 signals 16 colour graphics mode */
                if (Mode == VGAModes.TANDY4)
                    setModeNow(VGAModes.TANDY16); // TODO lowres mode only
                else
                    setMode(VGAModes.TANDY16);
            } else if ((Tandy.GFXControl & 0x08) != 0) {
                /* bit3 of mode control 2 signals 2 colour graphics mode */
                setMode(VGAModes.TANDY2);
            } else {
                /* otherwise some 4-colour graphics mode */
                if (Mode == VGAModes.TANDY16)
                    setModeNow(VGAModes.TANDY4);
                else
                    setMode(VGAModes.TANDY4);
            }
            writeColorSelect(Tandy.ColorSelect);
        } else {
            setMode(VGAModes.TANDY_TEXT);
        }
    }

    private void TandyCheckLineMask() {
        if ((Tandy.ExtendedRam & 1) != 0) {
            Tandy.LineMask = 0;
        } else if ((Tandy.ModeControl & 0x2) != 0) {
            Tandy.LineMask |= 1;
        }
        if (Tandy.LineMask != 0) {
            Tandy.LineShift = 13;
            Tandy.AddrMask = (1 << 13) - 1;
        } else {
            Tandy.AddrMask = ~0;
            Tandy.LineShift = 0;
        }
    }

    // (uint8)
    private void writeTandyReg(int val) {
        val &= 0xff;
        switch (Tandy.RegIndex) {
            case 0x0:
                if (DOSBox.Machine == DOSBox.MachineType.PCJR) {
                    Tandy.ModeControl = val;
                    setBlinking(val & 0x20);
                    findModePCJr();
                    Attr.Disabled = (val & 0x8) != 0 ? (byte) 0 : (byte) 1;
                } else {
                    Log.logging(Log.LogTypes.VGAMISC, Log.LogServerities.Normal,
                            "Unhandled Write %2X to tandy reg %X", val, Tandy.RegIndex);
                }
                break;
            case 0x2: /* Border color */
                Tandy.BorderColor = (byte) val;
                break;
            case 0x3: /* More control */
                Tandy.GFXControl = (byte) val;
                if (DOSBox.Machine == DOSBox.MachineType.TANDY)
                    findModeTANDY();
                else
                    findModePCJr();
                break;
            case 0x5: /* Extended ram page register */
                // Bit 0 enables extended ram
                // Bit 7 Switches clock, 0 . cga 28.6 , 1 . mono 32.5
                Tandy.ExtendedRam = (byte) val;
                // This is a bit of a hack to enable mapping video memory differently for
                // highres mode
                TandyCheckLineMask();
                setupHandlers();
                break;
            case 0x8: /* Monitor mode seletion */
                // Bit 1 select mode e, for 640x200x16, some double clocking thing?
                // Bit 4 select 350 line mode for hercules emulation
                Log.logging(Log.LogTypes.VGAMISC, Log.LogServerities.Normal,
                        "Write %2X to tandy monitor mode", val);
                break;
            /* palette colors */
            case 0x10:
            case 0x11:
            case 0x12:
            case 0x13:
            case 0x14:
            case 0x15:
            case 0x16:
            case 0x17:
            case 0x18:
            case 0x19:
            case 0x1a:
            case 0x1b:
            case 0x1c:
            case 0x1d:
            case 0x1e:
            case 0x1f:
                Attr.setPalette(Tandy.RegIndex - 0x10, val & 0xf);
                break;
            default:
                Log.logging(Log.LogTypes.VGAMISC, Log.LogServerities.Normal,
                        "Unhandled Write %2X to tandy reg %X", val, Tandy.RegIndex);
                break;
        }
    }

    // writeCGA(UInt32 port, UInt32 val, UInt32 iolen)
    private void writeCGA(int port, int val, int iolen) {
        switch (port) {
            case 0x3d8:
                Tandy.ModeControl = 0xff & val;
                Attr.Disabled = (byte) ((val & 0x8) != 0 ? 0 : 1);
                if ((Tandy.ModeControl & 0x2) != 0) {
                    if ((Tandy.ModeControl & 0x10) != 0) {
                        if ((val & 0x4) == 0 && DOSBox.Machine == DOSBox.MachineType.CGA) {
                            setMode(VGAModes.CGA16); // Video burst 16 160x200 color mode
                        } else {
                            setMode(VGAModes.TANDY2);
                        }
                    } else
                        setMode(VGAModes.TANDY4);
                    writeColorSelect(Tandy.ColorSelect);
                } else {
                    setMode(VGAModes.TANDY_TEXT);
                }
                setBlinking(val & 0x20);
                break;
            case 0x3d9:
                writeColorSelect(val);
                break;
        }
    }

    // (UInt32 port, UInt32 val, UInt32 iolen)
    private void writeTandy(int port, int val, int iolen) {
        switch (port) {
            case 0x3d8:
                Tandy.ModeControl = 0xff & val;
                TandyCheckLineMask();
                setBlinking(val & 0x20);
                findModeTANDY();
                break;
            case 0x3d9:
                writeColorSelect(val);
                break;
            case 0x3da:
                Tandy.RegIndex = 0xff & val;
                break;
            case 0x3db: // Clear lightpen latch
                Other.LightPenTriggered = false;
                break;
            case 0x3dc: // Preset lightpen latch
                if (!Other.LightPenTriggered) {
                    Other.LightPenTriggered = true; // TODO: this shows at port 3ba/3da bit 1

                    double timeInFrame = PIC.getFullIndex() - Draw.Delay.FrameStart;
                    double timeInLine = timeInFrame % Draw.Delay.HTotal;
                    int currentScanline = (int) (timeInFrame / Draw.Delay.HTotal);

                    Other.LightPen = 0xffff & ((Draw.AddressAdd / 2) * (currentScanline / 2));
                    Other.LightPen += 0xffff & (int) ((timeInLine / Draw.Delay.HDEnd)
                            * ((float) (Draw.AddressAdd / 2)));
                }
                break;
            // case 0x3dd: //Extended ram page address register:
            // break;
            case 0x3de:
                writeTandyReg(val);
                break;
            case 0x3df:
                Tandy.LineMask = 0xff & (val >>> 6);
                Tandy.DrawBank = 0xff & (val & ((Tandy.LineMask & 2) != 0 ? 0x6 : 0x7));
                Tandy.MemBank = 0xff & ((val >>> 3) & ((Tandy.LineMask & 2) != 0 ? 0x6 : 0x7));
                TandyCheckLineMask();
                setupHandlers();
                break;
        }
    }

    private void writePCJr(int port, int val, int iolen) {
        switch (port) {
            case 0x3d9:
                writeColorSelect(val);
                break;
            case 0x3da:
                if (Tandy.PcjrFlipFlop != 0)
                    writeTandyReg(val);
                else
                    Tandy.RegIndex = 0xff & val;
                Tandy.PcjrFlipFlop = (byte) (Tandy.PcjrFlipFlop == 0 ? 1 : 0);
                break;
            case 0x3df:
                Tandy.LineMask = 0xff & (val >>> 6);
                Tandy.DrawBank = 0xff & (val & ((Tandy.LineMask & 2) != 0 ? 0x6 : 0x7));
                Tandy.MemBank = 0xff & ((val >>> 3) & ((Tandy.LineMask & 2) != 0 ? 0x6 : 0x7));
                Tandy.DrawBase = Memory.MemBase + Tandy.DrawBank * 16 * 1024;
                Tandy.DrawAlloc = Memory.getMemAlloc();
                Tandy.MemBase = Memory.MemBase + Tandy.MemBank * 16 * 1024;
                Tandy.MemAlloc = Memory.getMemAlloc();
                TandyCheckLineMask();
                setupHandlers();
                break;
        }
    }

    private void cycleHercPal(boolean pressed) {
        if (!pressed)
            return;
        if (++hercPal > 2)
            hercPal = 0;
        HercPalette();
        Dac.combineColor(1, 7);
    }

    public void HercPalette() {
        switch (hercPal) {
            case 0: // White
                Dac.setEntry(0x7, 0x2a, 0x2a, 0x2a);
                Dac.setEntry(0xf, 0x3f, 0x3f, 0x3f);
                break;
            case 1: // Amber
                Dac.setEntry(0x7, 0x34, 0x20, 0x00);
                Dac.setEntry(0xf, 0x3f, 0x34, 0x00);
                break;
            case 2: // Green
                Dac.setEntry(0x7, 0x00, 0x26, 0x00);
                Dac.setEntry(0xf, 0x00, 0x3f, 0x00);
                break;
        }
    }

    private void writeHercules(int port, int val, int iolen) {
        switch (port) {
            case 0x3b8: {
                // the protected bits can always be cleared but only be set if the
                // protection bits are set
                if ((Herc.ModeControl & 0x2) != 0) {
                    // already set
                    if ((val & 0x2) == 0) {
                        Herc.ModeControl &= 0xff & (~0x2);
                        setMode(VGAModes.HERC_TEXT);
                    }
                } else {
                    // not set, can only set if protection bit is set
                    if ((val & 0x2) != 0 && (Herc.EnableBits & 0x1) != 0) {
                        Herc.ModeControl |= 0x2;
                        setMode(VGAModes.HERC_GFX);
                    }
                }
                if ((Herc.ModeControl & 0x80) != 0) {
                    if ((val & 0x80) == 0) {
                        Herc.ModeControl &= 0xff & (~0x80);
                        Tandy.DrawAlloc = Mem.LinearAlloc;
                        Tandy.DrawBase = Mem.LinearBase;
                    }
                } else {
                    if ((val & 0x80) != 0 && (Herc.EnableBits & 0x2) != 0) {
                        Herc.ModeControl |= 0x80;
                        Tandy.DrawAlloc = Mem.LinearAlloc;
                        Tandy.DrawBase = Mem.LinearBase + 32 * 1024;
                    }
                }
                Draw.Blinking = (val & 0x20) != 0 ? 1 : 0;
                Herc.ModeControl &= 0x82;
                Herc.ModeControl |= 0xff & (val & ~0x82);
                break;
            }
            case 0x3bf:
                Herc.EnableBits = (byte) val;
                break;
        }
    }

    /*
     * static int read_hercules(int port,int iolen) { LOG_MSG("read from Herc port %x",port); return
     * 0; }
     */

    private int readHercStatus(int port, int iolen) {
        // 3BAh (R): Status Register
        // bit 0 Horizontal sync
        // 1 Light pen status (only some cards)
        // 3 Video signal
        // 4-6 000: Hercules
        // 001: Hercules Plus
        // 101: Hercules InColor
        // 111: Unknown clone
        // 7 Vertical sync inverted

        double timeInFrame = PIC.getFullIndex() - Draw.Delay.FrameStart;
        int retval = 0x72; // Hercules ident; from a working card (Winbond W86855AF)
        // Another known working card has 0x76 ("KeysoGood", full-length)
        if (timeInFrame < Draw.Delay.VRStart || timeInFrame > Draw.Delay.VREnd)
            retval |= 0x80;

        double timeInLine = timeInFrame % Draw.Delay.HTotal;
        if (timeInLine >= Draw.Delay.HRStart && timeInLine <= Draw.Delay.HREnd)
            retval |= 0x1;

        // 688 Attack sub checks bit 3 - as a workaround have the bit enabled
        // if no sync active (corresponds to a completely white screen)
        if ((retval & 0x81) == 0x80)
            retval |= 0x8;
        return retval;
    }

    private void setupOther() {
        int i;
        // memset( &vga.tandy, 0, sizeof( vga.tandy ));
        Tandy.Clear();
        Attr.Disabled = 0;
        Config.BytesSkip = 0;

        // Initialize values common for most machines, can be overwritten
        Tandy.DrawBase = Mem.LinearBase;
        Tandy.DrawAlloc = Mem.LinearAlloc;
        Tandy.MemBase = Mem.LinearBase;
        Tandy.MemAlloc = Mem.LinearAlloc;
        Tandy.AddrMask = 8 * 1024 - 1;
        Tandy.LineMask = 3;
        Tandy.LineShift = 13;

        if (DOSBox.Machine == DOSBox.MachineType.CGA || DOSBox.isTANDYArch()) {
            // extern byte int10_font_08[256 * 8];
            for (i = 0; i < 256; i++)
                ArrayHelper.copy(INT10.int10Font08, i * 8, Draw.Font, i * 32, 8);
            // vga.draw.font_tables[0] = vga.draw.font_tables[1] = vga.draw.font;
            Draw.FontTablesIdx[0] = Draw.FontTablesIdx[1] = 0;
        }
        if (DOSBox.Machine == DOSBox.MachineType.HERC) {
            // extern byte int10_font_14[256 * 14];
            for (i = 0; i < 256; i++)
                ArrayHelper.copy(INT10.int10Font14, i * 14, Draw.Font, i * 32, 14);
            // vga.draw.font_tables[0] = vga.draw.font_tables[1] = vga.draw.font;
            Draw.FontTablesIdx[0] = Draw.FontTablesIdx[1] = 0;
            GUIPlatform.mapper.addKeyHandler(this::cycleHercPal, MapKeys.F11, 0, "hercpal",
                    "Herc Pal");
        }
        WriteHandler writeTandyWrap = this::writeTandy;

        if (DOSBox.Machine == DOSBox.MachineType.CGA) {
            WriteHandler writeCGAWrap = this::writeCGA;

            IO.registerWriteHandler(0x3d8, writeCGAWrap, IO.IO_MB);
            IO.registerWriteHandler(0x3d9, writeCGAWrap, IO.IO_MB);
            IO.registerWriteHandler(0x3db, writeTandyWrap, IO.IO_MB);
            IO.registerWriteHandler(0x3dc, writeTandyWrap, IO.IO_MB);
            GUIPlatform.mapper.addKeyHandler(this::increaseHue, MapKeys.F11, Mapper.MMOD2, "inchue",
                    "Inc Hue");
            GUIPlatform.mapper.addKeyHandler(this::decreaseHue, MapKeys.F11, 0, "dechue",
                    "Dec Hue");
        }
        if (DOSBox.Machine == DOSBox.MachineType.TANDY) {

            writeTandy(0x3df, 0x0, 0);
            IO.registerWriteHandler(0x3d8, writeTandyWrap, IO.IO_MB);
            IO.registerWriteHandler(0x3d9, writeTandyWrap, IO.IO_MB);
            IO.registerWriteHandler(0x3de, writeTandyWrap, IO.IO_MB);
            IO.registerWriteHandler(0x3df, writeTandyWrap, IO.IO_MB);
            IO.registerWriteHandler(0x3da, writeTandyWrap, IO.IO_MB);
        }
        if (DOSBox.Machine == DOSBox.MachineType.PCJR) {
            WriteHandler writePCJrWrap = this::writePCJr;

            // write_pcjr will setup base address
            writePCJr(0x3df, 0x7 | (0x7 << 3), 0);
            IO.registerWriteHandler(0x3d9, writePCJrWrap, IO.IO_MB);
            IO.registerWriteHandler(0x3da, writePCJrWrap, IO.IO_MB);
            IO.registerWriteHandler(0x3df, writePCJrWrap, IO.IO_MB);
        }
        ReadHandler readCrtcIndexOtherWrap = this::readCrtcIndexOther;
        ReadHandler readCrtcDataOtherWrap = this::readCrtcDataOther;
        ReadHandler readHercStatusWrap = this::readHercStatus;
        WriteHandler writeCrtcIndexOtherWrap = this::writeCrtcIndexOther;
        WriteHandler writeCrtcDataOtherWrap = this::writeCrtcDataOther;
        if (DOSBox.Machine == DOSBox.MachineType.HERC) {
            int Base = 0x3b0;

            WriteHandler writeHerculesWrap = this::writeHercules;

            for (i = 0; i < 4; i++) {
                // The registers are repeated as the address is not decoded properly;
                // The official ports are 3b4, 3b5
                IO.registerWriteHandler(Base + i * 2, writeCrtcIndexOtherWrap, IO.IO_MB);
                IO.registerWriteHandler(Base + i * 2 + 1, writeCrtcDataOtherWrap, IO.IO_MB);
                IO.registerReadHandler(Base + i * 2, readCrtcIndexOtherWrap, IO.IO_MB);
                IO.registerReadHandler(Base + i * 2 + 1, readCrtcDataOtherWrap, IO.IO_MB);
            }
            Herc.EnableBits = 0;
            Herc.ModeControl = 0xa; // first mode written will be text mode
            Crtc.UnderlineLocation = 13;
            IO.registerWriteHandler(0x3b8, writeHerculesWrap, IO.IO_MB);
            IO.registerWriteHandler(0x3bf, writeHerculesWrap, IO.IO_MB);
            IO.registerReadHandler(0x3ba, readHercStatusWrap, IO.IO_MB);
        }
        if (DOSBox.Machine == DOSBox.MachineType.CGA) {
            int Base = 0x3d0;
            for (int portCrtc = 0; portCrtc < 4; portCrtc++) {
                IO.registerWriteHandler(Base + portCrtc * 2, writeCrtcIndexOtherWrap, IO.IO_MB);
                IO.registerWriteHandler(Base + portCrtc * 2 + 1, writeCrtcDataOtherWrap, IO.IO_MB);
                IO.registerReadHandler(Base + portCrtc * 2, readCrtcIndexOtherWrap, IO.IO_MB);
                IO.registerReadHandler(Base + portCrtc * 2 + 1, readCrtcDataOtherWrap, IO.IO_MB);
            }
        }
        if (DOSBox.isTANDYArch()) {
            int Base = 0x3d4;
            IO.registerWriteHandler(Base, writeCrtcIndexOtherWrap, IO.IO_MB);
            IO.registerWriteHandler(Base + 1, writeCrtcDataOtherWrap, IO.IO_MB);
            IO.registerReadHandler(Base, readCrtcIndexOtherWrap, IO.IO_MB);
            IO.registerReadHandler(Base + 1, readCrtcDataOtherWrap, IO.IO_MB);
        }

    }

    /*--------------------------- end VGAOther -----------------------------*/
    /*--------------------------- bign VGAType -----------------------------*/

    public enum DrawMode {
        PART, LINE,
        // EGALINE
    }

    public static final class VGADraw {

        public boolean Resizing;
        public int Width;
        public int Height;
        public int Blocks;
        public int Address;
        public int Panning;
        public int BytesSkip;
        // byte *linear_base;
        public int LinearBase;
        public byte[] LinearAlloc;// linear에 할당된 배열
        public int LinearMask;
        public int AddressAdd;
        public int LineLength;
        public int AddressLineTotal;
        public int AddressLine;
        public int LinesTotal;
        public int VBlankSkip;
        public int LinesDone;
        public int LinesScaled;
        public int SplitLine;
        public int PartsTotal;
        public int PartsLines;
        public int PartsLeft;
        public int BytePanningShift;
        public Delay Delay = new Delay();
        public int Bpp;
        public double AspectRatio;
        public boolean DoubleScan;
        public boolean DoubleWidth, DoubleHeight;
        // byte font[64*1024];
        public byte[] Font;
        // byte * font_tables[2];
        public int[] FontTablesIdx;// font_tables, 위의 font배열의 포인터 대체
        public int Blinking;
        public Cursor Cursor = new Cursor();
        public DrawMode Mode;
        public boolean VRetTriggered;

    }

    final static class Delay {
        public double FrameStart;
        public double VRStart, VREnd; // V-retrace
        public double HRStart, HREnd; // H-retrace
        public double HBlkStart, HBlkEnd; // H-blanking
        public double VBlkStart, VBlkEnd; // V-Blanking
        public double VDEnd, VTotal;
        public double HDEnd, HTotal;
        public double Parts;
    }

    final static class Cursor {
        public int Address;
        public int SLine, ELine;// byte
        public int Count, Delay;// byte
        public int Enabled;// byte
    }

    public static final class VGAConfig {
        /* Memory handlers */
        public int MemHndMask;

        /* Video drawing */
        public int DisplayStart;
        public int RealStart;
        public boolean Retrace; /* A retrace is active */
        public int ScanLen;
        public int CursorStart;

        /* Some other screen related variables */
        public int LineCompare;
        public boolean Chained; /* Enable or Disabled Chain 4 Mode */
        public boolean CompatibleChain4;

        /* Pixel Scrolling */
        public int PelPanning;// byte /* Amount of pixels to skip when starting horizontal line */
        public int HLinesSkip;// byte
        public int BytesSkip;// byte
        public int AddrShift;// byte

        /* Specific stuff memory write/read handling */

        public int ReadMode;// byte
        public int WriteMode;// byte
        public int ReadMapSelect;// byte
        public int ColorDontCare;// byte
        public int ColorCompare;// byte
        public int DataRotate;// byte
        public int RasterOp;// byte

        public int FullBitMask;
        public int FullMapMask;
        public int FullNotMapMask;
        public int FullSetReset;
        public int FullNotEnableSetreset;
        public int FullEnableSetReset;
        public int FullEnableAndSetReset;
    }

    final static class VGAInternal {
        public boolean AttrIndex;
    }

    final static class RGBEntry {
        public byte Red;
        public byte Green;
        public byte Blue;
    }

    public static final class VGAS3 {
        public byte RegLock1;
        public byte RegLock2;
        public byte Reg31;
        public byte Reg35;
        public byte Reg36; // RAM size
        public byte Reg3A; // 4/8/doublepixel bit in there
        public byte Reg40; // 8415/A functionality register
        public byte Reg41; // BIOS flags
        public byte Reg43;
        public byte Reg45; // Hardware graphics cursor
        public byte Reg50;
        public byte Reg51;
        public byte Reg52;
        public byte Reg55;
        public byte Reg58;
        public byte Reg6B; // LFB BIOS scratchpad
        public byte ExHorOverflow;
        public byte ExVerOverflow;
        public int LaWindow;// uint16
        public int MiscControl2;// uint8
        public byte ExtMemCtrl;
        public int XGAScreenWidth;
        public VGAModes XGAColorMode;
        // public CLK[] clk[4];
        public CLK[] CLK;
        public CLK MCLK;
        public PLL PLL;
        public VGAHwCursor HGC;

        public VGAS3() {
            this.MCLK = new CLK();
            this.PLL = new PLL();
            this.HGC = new VGAHwCursor();
        }
    }

    public static final class CLK {
        public byte r;
        public byte n;
        public byte m;
    }

    public static final class PLL {
        public byte Lock;
        public byte Cmd;
    }

    public static final class VGAHwCursor {
        public byte CurMode;
        public int OriginX, OriginY;// uint16
        public byte ForeStackPos, BackStackPos;
        // public byte forestack[3];
        public byte[] ForeStack;
        // public byte backstack[3];
        public byte[] BackStack;
        public short StartAddr;
        public int PosX, PosY;// byte
        // public byte mc[64][64];
        public byte[][] MC;
    }

    public static final class VGASVGA {
        public int ReadStart, WriteStart;
        public int BankMask;
        public int BankFeadFull;
        public int BankWriteFull;
        public int BankRead;// uint8
        public int BankWrite;// uint8
        public int BankSize;// uint32
    }

    final static class VGAHerc {
        public int ModeControl;// uint8
        public byte EnableBits;
    }

    public static final class VGATandy {
        public byte PcjrFlipFlop;
        public int ModeControl;// uint8
        public int ColorSelect;// uint8
        public byte DispBank;
        public int RegIndex;// byte
        public byte GFXControl;
        public byte PaletteMask;
        public byte ExtendedRam;
        public byte BorderColor;
        public int LineMask, LineShift;// uint8
        public int DrawBank, MemBank;// uint8
        // byte *draw_base, *mem_base;
        public int DrawBase, MemBase;// 할당된 메모리의 지정 인덱스
        public byte[] DrawAlloc, MemAlloc;// draw_base, mem_base가 가리키는 저장공간(할당 공간)
        public int AddrMask;

        public void Clear() {
            PcjrFlipFlop = 0;
            ModeControl = 0;
            ColorSelect = 0;
            DispBank = 0;
            RegIndex = 0;
            GFXControl = 0;
            PaletteMask = 0;
            ExtendedRam = 0;
            BorderColor = 0;
            LineMask = 0;
            LineShift = 0;
            DrawBank = 0;
            MemBank = 0;
            DrawBase = 0;
            MemBase = 0;// 할당된 메모리의 지정 인덱스
            DrawAlloc = null;
            MemAlloc = null;// draw_base, mem_base가 가리키는 저장공간(할당 공간)
            AddrMask = 0;

        }
    }

    final static class VGAOther {
        public int Index;
        public int HTotal;
        public int HdEnd;
        public int HSyncP;
        public int HSyncW;
        public int VTotal;
        public int VdEnd;
        public int VAdjust;
        public int VSyncP;
        public int VSyncW;
        public int MaxScanLine;
        public int LightPen;
        public boolean LightPenTriggered;
        public int CursorStart;
        public int CursorEnd;
    }

    // 15비트 이동한 다음 주소숫자의 뒷자리 15비트부분을 깨끗하게 지우는 작업을 왜 하는지 모르겠음
    // VGA_SetupMemory()에서 vga.mem.linear_orgptr없이 linear에 바로 할당하는 걸로 결정했기때문에
    // linear_orgptr를 삭제
    public static final class VGAMemory {
        // byte* linear;
        public byte[] LinearAlloc;
        public int LinearBase;
        // byte* linear_orgptr;
        // public byte[] linear_orgptr;
    }

    public static final class VGALFB {
        public int Page;
        public int Addr;
        public int Mask;
        public PageHandler Handler;
    }

    /*--------------------------- end VGAType -----------------------------*/

}
