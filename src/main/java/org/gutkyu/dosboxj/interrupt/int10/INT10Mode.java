package org.gutkyu.dosboxj.interrupt.int10;

import org.gutkyu.dosboxj.hardware.io.IO;
import org.gutkyu.dosboxj.hardware.memory.Memory;
import org.gutkyu.dosboxj.hardware.video.*;
import org.gutkyu.dosboxj.hardware.video.svga.ModeExtraData;
import org.gutkyu.dosboxj.interrupt.Mouse;
import org.gutkyu.dosboxj.util.*;
import java.util.Arrays;
import org.gutkyu.dosboxj.*;

/*--------------------------- begin INT10Modes -----------------------------*/
public final class INT10Mode {

    private static final int EGA_HALF_CLOCK = 0x0001;
    private static final int EGA_LINE_DOUBLE = 0x0002;
    private static final int VGA_PIXEL_DOUBLE = 0x0004;

    private static final int SEQ_REGS = 0x05;
    private static final int GFX_REGS = 0x09;
    private static final int ATT_REGS = 0x15;


    public static class VideoModeBlock {
        public int Mode;
        public VGAModes Type;
        public int SWidth, SHeight;
        public int TWidth, THeight;
        public int CWidth, CHeight;
        public int PTotal, PStart, Plength;

        public int HTotal, VTotal;
        public int HDispend, VDispend;
        public int Special;

        protected VideoModeBlock(int Mode, VGAModes Type, int SWidth, int SHeight, int TWidth,
                int THeight, int CWidth, int CHeight, int PTotal, int PStart, int Plength,

                int HTotal, int VTotal, int HDispend, int VDispend, int Special) {
            this.Mode = Mode;
            this.Type = Type;
            this.SWidth = SWidth;
            this.SHeight = SHeight;
            this.TWidth = TWidth;
            this.THeight = THeight;
            this.CWidth = CWidth;
            this.CHeight = CHeight;
            this.PTotal = PTotal;
            this.PStart = PStart;
            this.Plength = Plength;

            this.HTotal = HTotal;
            this.VTotal = VTotal;
            this.HDispend = HDispend;
            this.VDispend = VDispend;
            this.Special = Special;
        }
    }

    public static VideoModeBlock[] ModeList_VGA = {
            /*
             * mode ,type ,sw ,sh ,tw ,th ,cw,ch ,pt,pstart ,plength,htot,vtot,hde,vde special flags
             */
            new VideoModeBlock(0x000, VGAModes.TEXT, 360, 400, 40, 25, 9, 16, 8, 0xB8000, 0x0800,
                    50, 449, 40, 400, EGA_HALF_CLOCK),
            new VideoModeBlock(0x001, VGAModes.TEXT, 360, 400, 40, 25, 9, 16, 8, 0xB8000, 0x0800,
                    50, 449, 40, 400, EGA_HALF_CLOCK),
            new VideoModeBlock(0x002, VGAModes.TEXT, 720, 400, 80, 25, 9, 16, 8, 0xB8000, 0x1000,
                    100, 449, 80, 400, 0),
            new VideoModeBlock(0x003, VGAModes.TEXT, 720, 400, 80, 25, 9, 16, 8, 0xB8000, 0x1000,
                    100, 449, 80, 400, 0),
            new VideoModeBlock(0x004, VGAModes.CGA4, 320, 200, 40, 25, 8, 8, 1, 0xB8000, 0x4000, 50,
                    449, 40, 400, EGA_HALF_CLOCK | EGA_LINE_DOUBLE),
            new VideoModeBlock(0x005, VGAModes.CGA4, 320, 200, 40, 25, 8, 8, 1, 0xB8000, 0x4000, 50,
                    449, 40, 400, EGA_HALF_CLOCK | EGA_LINE_DOUBLE),
            new VideoModeBlock(0x006, VGAModes.CGA2, 640, 200, 80, 25, 8, 8, 1, 0xB8000, 0x4000,
                    100, 449, 80, 400, EGA_HALF_CLOCK | EGA_LINE_DOUBLE),
            new VideoModeBlock(0x007, VGAModes.TEXT, 720, 400, 80, 25, 9, 16, 8, 0xB0000, 0x1000,
                    100, 449, 80, 400, 0),
            new VideoModeBlock(0x00D, VGAModes.EGA, 320, 200, 40, 25, 8, 8, 8, 0xA0000, 0x2000, 50,
                    449, 40, 400, EGA_HALF_CLOCK | EGA_LINE_DOUBLE),
            new VideoModeBlock(0x00E, VGAModes.EGA, 640, 200, 80, 25, 8, 8, 4, 0xA0000, 0x4000, 100,
                    449, 80, 400, EGA_LINE_DOUBLE),
            new VideoModeBlock(0x00F, VGAModes.EGA, 640, 350, 80, 25, 8, 14, 2, 0xA0000, 0x8000,
                    100, 449, 80, 350, 0), /* was EGA_2 */
            new VideoModeBlock(0x010, VGAModes.EGA, 640, 350, 80, 25, 8, 14, 2, 0xA0000, 0x8000,
                    100, 449, 80, 350, 0),
            new VideoModeBlock(0x011, VGAModes.EGA, 640, 480, 80, 30, 8, 16, 1, 0xA0000, 0xA000,
                    100, 525, 80, 480, 0), /* was EGA_2 */
            new VideoModeBlock(0x012, VGAModes.EGA, 640, 480, 80, 30, 8, 16, 1, 0xA0000, 0xA000,
                    100, 525, 80, 480, 0),
            new VideoModeBlock(0x013, VGAModes.VGA, 320, 200, 40, 25, 8, 8, 1, 0xA0000, 0x2000, 100,
                    449, 80, 400, 0),
            new VideoModeBlock(0x054, VGAModes.TEXT, 1056, 688, 132, 43, 8, 16, 1, 0xB8000, 0x4000,
                    192, 800, 132, 688, 0),
            new VideoModeBlock(0x055, VGAModes.TEXT, 1056, 400, 132, 25, 8, 16, 1, 0xB8000, 0x2000,
                    192, 449, 132, 400, 0),
            /* Alias of mode 101 */
            new VideoModeBlock(0x069, VGAModes.LIN8, 640, 480, 80, 30, 8, 16, 1, 0xA0000, 0x10000,
                    100, 525, 80, 480, 0),
            /* Alias of mode 102 */
            new VideoModeBlock(0x06A, VGAModes.LIN4, 800, 600, 100, 37, 8, 16, 1, 0xA0000, 0x10000,
                    128, 663, 100, 600, 0),
            /* Follow vesa 1.2 for first 0x20 */
            new VideoModeBlock(0x100, VGAModes.LIN8, 640, 400, 80, 25, 8, 16, 1, 0xA0000, 0x10000,
                    100, 449, 80, 400, 0),
            new VideoModeBlock(0x101, VGAModes.LIN8, 640, 480, 80, 30, 8, 16, 1, 0xA0000, 0x10000,
                    100, 525, 80, 480, 0),
            new VideoModeBlock(0x102, VGAModes.LIN4, 800, 600, 100, 37, 8, 16, 1, 0xA0000, 0x10000,
                    132, 628, 100, 600, 0),
            new VideoModeBlock(0x103, VGAModes.LIN8, 800, 600, 100, 37, 8, 16, 1, 0xA0000, 0x10000,
                    132, 628, 100, 600, 0),
            new VideoModeBlock(0x104, VGAModes.LIN4, 1024, 768, 128, 48, 8, 16, 1, 0xA0000, 0x10000,
                    168, 806, 128, 768, 0),
            new VideoModeBlock(0x105, VGAModes.LIN8, 1024, 768, 128, 48, 8, 16, 1, 0xA0000, 0x10000,
                    168, 806, 128, 768, 0),
            new VideoModeBlock(0x106, VGAModes.LIN4, 1280, 1024, 160, 64, 8, 16, 1, 0xA0000,
                    0x10000, 212, 1066, 160, 1024, 0),
            new VideoModeBlock(0x107, VGAModes.LIN8, 1280, 1024, 160, 64, 8, 16, 1, 0xA0000,
                    0x10000, 212, 1066, 160, 1024, 0),
            new VideoModeBlock(0x10D, VGAModes.LIN15, 320, 200, 40, 25, 8, 8, 1, 0xA0000, 0x10000,
                    100, 449, 80, 400, VGA_PIXEL_DOUBLE | EGA_LINE_DOUBLE),
            new VideoModeBlock(0x10E, VGAModes.LIN16, 320, 200, 40, 25, 8, 8, 1, 0xA0000, 0x10000,
                    100, 449, 80, 400, VGA_PIXEL_DOUBLE | EGA_LINE_DOUBLE),
            new VideoModeBlock(0x10F, VGAModes.LIN32, 320, 200, 40, 25, 8, 8, 1, 0xA0000, 0x10000,
                    50, 449, 40, 400, VGA_PIXEL_DOUBLE | EGA_LINE_DOUBLE),
            new VideoModeBlock(0x110, VGAModes.LIN15, 640, 480, 80, 30, 8, 16, 1, 0xA0000, 0x10000,
                    200, 525, 160, 480, 0),
            new VideoModeBlock(0x111, VGAModes.LIN16, 640, 480, 80, 30, 8, 16, 1, 0xA0000, 0x10000,
                    200, 525, 160, 480, 0),
            new VideoModeBlock(0x112, VGAModes.LIN32, 640, 480, 80, 30, 8, 16, 1, 0xA0000, 0x10000,
                    100, 525, 80, 480, 0),
            new VideoModeBlock(0x113, VGAModes.LIN15, 800, 600, 100, 37, 8, 16, 1, 0xA0000, 0x10000,
                    264, 628, 200, 600, 0),
            new VideoModeBlock(0x114, VGAModes.LIN16, 800, 600, 100, 37, 8, 16, 1, 0xA0000, 0x10000,
                    264, 628, 200, 600, 0),
            new VideoModeBlock(0x115, VGAModes.LIN32, 800, 600, 100, 37, 8, 16, 1, 0xA0000, 0x10000,
                    132, 628, 100, 600, 0),
            new VideoModeBlock(0x116, VGAModes.LIN15, 1024, 768, 128, 48, 8, 16, 1, 0xA0000,
                    0x10000, 336, 806, 256, 768, 0),
            new VideoModeBlock(0x117, VGAModes.LIN16, 1024, 768, 128, 48, 8, 16, 1, 0xA0000,
                    0x10000, 336, 806, 256, 768, 0),
            new VideoModeBlock(0x118, VGAModes.LIN32, 1024, 768, 128, 48, 8, 16, 1, 0xA0000,
                    0x10000, 168, 806, 128, 768, 0),
            /* those should be interlaced but ok */
            // { 0x119 ,VGAModes.M_LIN15 ,1280,1024,160,64 ,8 ,16 ,1 ,0xA0000 ,0x10000,424
            // ,1066,320,1024,0 },
            // { 0x11A ,VGAModes.M_LIN16 ,1280,1024,160,64 ,8 ,16 ,1 ,0xA0000 ,0x10000,424
            // ,1066,320,1024,0 },
            new VideoModeBlock(0x150, VGAModes.LIN8, 320, 200, 40, 25, 8, 8, 1, 0xA0000, 0x10000,
                    100, 449, 80, 400, VGA_PIXEL_DOUBLE | EGA_LINE_DOUBLE),
            new VideoModeBlock(0x151, VGAModes.LIN8, 320, 240, 40, 30, 8, 8, 1, 0xA0000, 0x10000,
                    100, 525, 80, 480, VGA_PIXEL_DOUBLE | EGA_LINE_DOUBLE),
            new VideoModeBlock(0x152, VGAModes.LIN8, 320, 400, 40, 50, 8, 8, 1, 0xA0000, 0x10000,
                    100, 449, 80, 400, VGA_PIXEL_DOUBLE),
            new VideoModeBlock(0x153, VGAModes.LIN8, 320, 480, 40, 60, 8, 8, 1, 0xA0000, 0x10000,
                    100, 525, 80, 480, VGA_PIXEL_DOUBLE),
            new VideoModeBlock(0x160, VGAModes.LIN15, 320, 240, 40, 30, 8, 8, 1, 0xA0000, 0x10000,
                    100, 525, 80, 480, VGA_PIXEL_DOUBLE | EGA_LINE_DOUBLE),
            new VideoModeBlock(0x161, VGAModes.LIN15, 320, 400, 40, 50, 8, 8, 1, 0xA0000, 0x10000,
                    100, 449, 80, 400, VGA_PIXEL_DOUBLE),
            new VideoModeBlock(0x162, VGAModes.LIN15, 320, 480, 40, 60, 8, 8, 1, 0xA0000, 0x10000,
                    100, 525, 80, 480, VGA_PIXEL_DOUBLE),
            new VideoModeBlock(0x165, VGAModes.LIN15, 640, 400, 80, 25, 8, 16, 1, 0xA0000, 0x10000,
                    200, 449, 160, 400, 0),
            new VideoModeBlock(0x170, VGAModes.LIN16, 320, 240, 40, 30, 8, 8, 1, 0xA0000, 0x10000,
                    100, 525, 80, 480, VGA_PIXEL_DOUBLE | EGA_LINE_DOUBLE),
            new VideoModeBlock(0x171, VGAModes.LIN16, 320, 400, 40, 50, 8, 8, 1, 0xA0000, 0x10000,
                    100, 449, 80, 400, VGA_PIXEL_DOUBLE),
            new VideoModeBlock(0x172, VGAModes.LIN16, 320, 480, 40, 60, 8, 8, 1, 0xA0000, 0x10000,
                    100, 525, 80, 480, VGA_PIXEL_DOUBLE),
            new VideoModeBlock(0x175, VGAModes.LIN16, 640, 400, 80, 25, 8, 16, 1, 0xA0000, 0x10000,
                    200, 449, 160, 400, 0),
            new VideoModeBlock(0x190, VGAModes.LIN32, 320, 240, 40, 30, 8, 8, 1, 0xA0000, 0x10000,
                    50, 525, 40, 480, VGA_PIXEL_DOUBLE | EGA_LINE_DOUBLE),
            new VideoModeBlock(0x191, VGAModes.LIN32, 320, 400, 40, 50, 8, 8, 1, 0xA0000, 0x10000,
                    50, 449, 40, 400, VGA_PIXEL_DOUBLE),
            new VideoModeBlock(0x192, VGAModes.LIN32, 320, 480, 40, 60, 8, 8, 1, 0xA0000, 0x10000,
                    50, 525, 40, 480, VGA_PIXEL_DOUBLE),
            /* S3 specific modesmode= */
            new VideoModeBlock(0x207, VGAModes.LIN8, 1152, 864, 160, 64, 8, 16, 1, 0xA0000, 0x10000,
                    182, 948, 144, 864, 0),
            new VideoModeBlock(0x209, VGAModes.LIN15, 1152, 864, 160, 64, 8, 16, 1, 0xA0000,
                    0x10000, 364, 948, 288, 864, 0),
            new VideoModeBlock(0x20A, VGAModes.LIN16, 1152, 864, 160, 64, 8, 16, 1, 0xA0000,
                    0x10000, 364, 948, 288, 864, 0),
            // { 0x20B ,VGAModes.M_LIN32 mode= ,1152,864,160,64 ,8 ,16 ,1 ,0xA0000 ,0x10000,182 ,948
            // ,144,864 ,0 },
            new VideoModeBlock(0x213, VGAModes.LIN32, 640, 400, 80, 25, 8, 16, 1, 0xA0000, 0x10000,
                    100, 449, 80, 400, 0),
            /* Some custom modesmode= */
            // { 0x220 ,VGAModes.M_LIN32 ,1280,1024,160,64,8 ,16 ,1,0xA0000 ,0x10000,212
            // ,1066,160,1024,0},
            // A nice 16:9 mode
            new VideoModeBlock(0x222, VGAModes.LIN8, 848, 480, 80, 30, 8, 16, 1, 0xA0000, 0x10000,
                    132, 525, 106, 480, 0),
            new VideoModeBlock(0x223, VGAModes.LIN15, 848, 480, 80, 30, 8, 16, 1, 0xA0000, 0x10000,
                    264, 525, 212, 480, 0),
            new VideoModeBlock(0x224, VGAModes.LIN16, 848, 480, 80, 30, 8, 16, 1, 0xA0000, 0x10000,
                    264, 525, 212, 480, 0),
            new VideoModeBlock(0x225, VGAModes.LIN32, 848, 480, 80, 30, 8, 16, 1, 0xA0000, 0x10000,
                    132, 525, 106, 480, 0),

            new VideoModeBlock(0xFFFF, VGAModes.ERROR, 0, 0, 0, 0, 0, 0, 0, 0x00000, 0x0000, 0, 0,
                    0, 0, 0),};

    private static VideoModeBlock[] ModeList_VGA_Text_200lines = {
            /*
             * mode ,type ,sw ,sh ,tw ,th ,cw,ch ,pt,pstart ,plength,htot,vtot,hde,vde special flags
             */
            new VideoModeBlock(0x000, VGAModes.TEXT, 320, 200, 40, 25, 8, 8, 8, 0xB8000, 0x0800, 50,
                    449, 40, 400, EGA_HALF_CLOCK | EGA_LINE_DOUBLE),
            new VideoModeBlock(0x001, VGAModes.TEXT, 320, 200, 40, 25, 8, 8, 8, 0xB8000, 0x0800, 50,
                    449, 40, 400, EGA_HALF_CLOCK | EGA_LINE_DOUBLE),
            new VideoModeBlock(0x002, VGAModes.TEXT, 640, 200, 80, 25, 8, 8, 8, 0xB8000, 0x1000,
                    100, 449, 80, 400, EGA_LINE_DOUBLE),
            new VideoModeBlock(0x003, VGAModes.TEXT, 640, 200, 80, 25, 8, 8, 8, 0xB8000, 0x1000,
                    100, 449, 80, 400, EGA_LINE_DOUBLE)};

    private static VideoModeBlock[] ModeList_VGA_Text_350lines = {
            /*
             * mode ,type ,sw ,sh ,tw ,th ,cw,ch ,pt,pstart ,plength,htot,vtot,hde,vde special flags
             */
            new VideoModeBlock(0x000, VGAModes.TEXT, 320, 350, 40, 25, 8, 14, 8, 0xB8000, 0x0800,
                    50, 449, 40, 350, EGA_HALF_CLOCK),
            new VideoModeBlock(0x001, VGAModes.TEXT, 320, 350, 40, 25, 8, 14, 8, 0xB8000, 0x0800,
                    50, 449, 40, 350, EGA_HALF_CLOCK),
            new VideoModeBlock(0x002, VGAModes.TEXT, 640, 350, 80, 25, 8, 14, 8, 0xB8000, 0x1000,
                    100, 449, 80, 350, 0),
            new VideoModeBlock(0x003, VGAModes.TEXT, 640, 350, 80, 25, 8, 14, 8, 0xB8000, 0x1000,
                    100, 449, 80, 350, 0)};
    private static VideoModeBlock[] ModeList_VGA_Tseng = {
            /*
             * mode ,type ,sw ,sh ,tw ,th ,cw,ch ,pt,pstart ,plength,htot,vtot,hde,vde special flags
             */
            new VideoModeBlock(0x000, VGAModes.TEXT, 360, 400, 40, 25, 9, 16, 8, 0xB8000, 0x0800,
                    50, 449, 40, 400, EGA_HALF_CLOCK),
            new VideoModeBlock(0x001, VGAModes.TEXT, 360, 400, 40, 25, 9, 16, 8, 0xB8000, 0x0800,
                    50, 449, 40, 400, EGA_HALF_CLOCK),
            new VideoModeBlock(0x002, VGAModes.TEXT, 720, 400, 80, 25, 9, 16, 8, 0xB8000, 0x1000,
                    100, 449, 80, 400, 0),
            new VideoModeBlock(0x003, VGAModes.TEXT, 720, 400, 80, 25, 9, 16, 8, 0xB8000, 0x1000,
                    100, 449, 80, 400, 0),
            new VideoModeBlock(0x004, VGAModes.CGA4, 320, 200, 40, 25, 8, 8, 1, 0xB8000, 0x4000, 50,
                    449, 40, 400, EGA_HALF_CLOCK | EGA_LINE_DOUBLE),
            new VideoModeBlock(0x005, VGAModes.CGA4, 320, 200, 40, 25, 8, 8, 1, 0xB8000, 0x4000, 50,
                    449, 40, 400, EGA_HALF_CLOCK | EGA_LINE_DOUBLE),
            new VideoModeBlock(0x006, VGAModes.CGA2, 640, 200, 80, 25, 8, 8, 1, 0xB8000, 0x4000,
                    100, 449, 80, 400, EGA_HALF_CLOCK | EGA_LINE_DOUBLE),
            new VideoModeBlock(0x007, VGAModes.TEXT, 720, 400, 80, 25, 9, 16, 8, 0xB0000, 0x1000,
                    100, 449, 80, 400, 0),

            new VideoModeBlock(0x00D, VGAModes.EGA, 320, 200, 40, 25, 8, 8, 8, 0xA0000, 0x2000, 50,
                    449, 40, 400, EGA_HALF_CLOCK | EGA_LINE_DOUBLE),
            new VideoModeBlock(0x00E, VGAModes.EGA, 640, 200, 80, 25, 8, 8, 4, 0xA0000, 0x4000, 100,
                    449, 80, 400, EGA_LINE_DOUBLE),
            new VideoModeBlock(0x00F, VGAModes.EGA, 640, 350, 80, 25, 8, 14, 2, 0xA0000, 0x8000,
                    100, 449, 80, 350, 0), /* was EGA_2 */
            new VideoModeBlock(0x010, VGAModes.EGA, 640, 350, 80, 25, 8, 14, 2, 0xA0000, 0x8000,
                    100, 449, 80, 350, 0),
            new VideoModeBlock(0x011, VGAModes.EGA, 640, 480, 80, 30, 8, 16, 1, 0xA0000, 0xA000,
                    100, 525, 80, 480, 0), /* was EGA_2 */
            new VideoModeBlock(0x012, VGAModes.EGA, 640, 480, 80, 30, 8, 16, 1, 0xA0000, 0xA000,
                    100, 525, 80, 480, 0),
            new VideoModeBlock(0x013, VGAModes.VGA, 320, 200, 40, 25, 8, 8, 1, 0xA0000, 0x2000, 100,
                    449, 80, 400, 0),

            new VideoModeBlock(0x018, VGAModes.TEXT, 1056, 688, 132, 44, 8, 8, 1, 0xB0000, 0x4000,
                    192, 800, 132, 704, 0),
            new VideoModeBlock(0x019, VGAModes.TEXT, 1056, 400, 132, 25, 8, 16, 1, 0xB0000, 0x2000,
                    192, 449, 132, 400, 0),
            new VideoModeBlock(0x01A, VGAModes.TEXT, 1056, 400, 132, 28, 8, 16, 1, 0xB0000, 0x2000,
                    192, 449, 132, 448, 0),
            new VideoModeBlock(0x022, VGAModes.TEXT, 1056, 688, 132, 44, 8, 8, 1, 0xB8000, 0x4000,
                    192, 800, 132, 704, 0),
            new VideoModeBlock(0x023, VGAModes.TEXT, 1056, 400, 132, 25, 8, 16, 1, 0xB8000, 0x2000,
                    192, 449, 132, 400, 0),
            new VideoModeBlock(0x024, VGAModes.TEXT, 1056, 400, 132, 28, 8, 16, 1, 0xB8000, 0x2000,
                    192, 449, 132, 448, 0),
            new VideoModeBlock(0x025, VGAModes.LIN4, 640, 480, 80, 30, 8, 16, 1, 0xA0000, 0xA000,
                    100, 525, 80, 480, 0),
            new VideoModeBlock(0x029, VGAModes.LIN4, 800, 600, 100, 37, 8, 16, 1, 0xA0000, 0xA000,
                    128, 663, 100, 600, 0),
            new VideoModeBlock(0x02D, VGAModes.LIN8, 640, 350, 80, 21, 8, 16, 1, 0xA0000, 0x10000,
                    100, 449, 80, 350, 0),
            new VideoModeBlock(0x02E, VGAModes.LIN8, 640, 480, 80, 30, 8, 16, 1, 0xA0000, 0x10000,
                    100, 525, 80, 480, 0),
            new VideoModeBlock(0x02F, VGAModes.LIN8, 640, 400, 80, 25, 8, 16, 1, 0xA0000, 0x10000,
                    100, 449, 80, 400, 0), /* ET4000 only */
            new VideoModeBlock(0x030, VGAModes.LIN8, 800, 600, 100, 37, 8, 16, 1, 0xA0000, 0x10000,
                    128, 663, 100, 600, 0),
            new VideoModeBlock(0x036, VGAModes.LIN4, 960, 720, 120, 45, 8, 16, 1, 0xA0000, 0xA000,
                    120, 800, 120, 720, 0), /* STB only */
            new VideoModeBlock(0x037, VGAModes.LIN4, 1024, 768, 128, 48, 8, 16, 1, 0xA0000, 0xA000,
                    128, 800, 128, 768, 0),
            new VideoModeBlock(0x038, VGAModes.LIN8, 1024, 768, 128, 48, 8, 16, 1, 0xA0000, 0x10000,
                    128, 800, 128, 768, 0), /* ET4000 only */
            new VideoModeBlock(0x03D, VGAModes.LIN4, 1280, 1024, 160, 64, 8, 16, 1, 0xA0000, 0xA000,
                    160, 1152, 160, 1024, 0), /* newer ET4000 */
            new VideoModeBlock(0x03E, VGAModes.LIN4, 1280, 960, 160, 60, 8, 16, 1, 0xA0000, 0xA000,
                    160, 1024, 160, 960, 0), /* Definicon only */
            new VideoModeBlock(0x06A, VGAModes.LIN4, 800, 600, 100, 37, 8, 16, 1, 0xA0000, 0xA000,
                    128, 663, 100, 600, 0), /* newer ET4000 */

            new VideoModeBlock(0xFFFF, VGAModes.ERROR, 0, 0, 0, 0, 0, 0, 0, 0x00000, 0x0000, 0, 0,
                    0, 0, 0),};
    private static VideoModeBlock[] ModeList_VGA_Paradise = {
            /*
             * mode ,type ,sw ,sh ,tw ,th ,cw,ch ,pt,pstart ,plength,htot,vtot,hde,vde special flags
             */
            new VideoModeBlock(0x000, VGAModes.TEXT, 360, 400, 40, 25, 9, 16, 8, 0xB8000, 0x0800,
                    50, 449, 40, 400, EGA_HALF_CLOCK),
            new VideoModeBlock(0x001, VGAModes.TEXT, 360, 400, 40, 25, 9, 16, 8, 0xB8000, 0x0800,
                    50, 449, 40, 400, EGA_HALF_CLOCK),
            new VideoModeBlock(0x002, VGAModes.TEXT, 720, 400, 80, 25, 9, 16, 8, 0xB8000, 0x1000,
                    100, 449, 80, 400, 0),
            new VideoModeBlock(0x003, VGAModes.TEXT, 720, 400, 80, 25, 9, 16, 8, 0xB8000, 0x1000,
                    100, 449, 80, 400, 0),
            new VideoModeBlock(0x004, VGAModes.CGA4, 320, 200, 40, 25, 8, 8, 1, 0xB8000, 0x4000, 50,
                    449, 40, 400, EGA_HALF_CLOCK | EGA_LINE_DOUBLE),
            new VideoModeBlock(0x005, VGAModes.CGA4, 320, 200, 40, 25, 8, 8, 1, 0xB8000, 0x4000, 50,
                    449, 40, 400, EGA_HALF_CLOCK | EGA_LINE_DOUBLE),
            new VideoModeBlock(0x006, VGAModes.CGA2, 640, 200, 80, 25, 8, 8, 1, 0xB8000, 0x4000,
                    100, 449, 80, 400, EGA_HALF_CLOCK | EGA_LINE_DOUBLE),
            new VideoModeBlock(0x007, VGAModes.TEXT, 720, 400, 80, 25, 9, 16, 8, 0xB0000, 0x1000,
                    100, 449, 80, 400, 0),

            new VideoModeBlock(0x00D, VGAModes.EGA, 320, 200, 40, 25, 8, 8, 8, 0xA0000, 0x2000, 50,
                    449, 40, 400, EGA_HALF_CLOCK | EGA_LINE_DOUBLE),
            new VideoModeBlock(0x00E, VGAModes.EGA, 640, 200, 80, 25, 8, 8, 4, 0xA0000, 0x4000, 100,
                    449, 80, 400, EGA_LINE_DOUBLE),
            new VideoModeBlock(0x00F, VGAModes.EGA, 640, 350, 80, 25, 8, 14, 2, 0xA0000, 0x8000,
                    100, 449, 80, 350, 0), /* was EGA_2 */
            new VideoModeBlock(0x010, VGAModes.EGA, 640, 350, 80, 25, 8, 14, 2, 0xA0000, 0x8000,
                    100, 449, 80, 350, 0),
            new VideoModeBlock(0x011, VGAModes.EGA, 640, 480, 80, 30, 8, 16, 1, 0xA0000, 0xA000,
                    100, 525, 80, 480, 0), /* was EGA_2 */
            new VideoModeBlock(0x012, VGAModes.EGA, 640, 480, 80, 30, 8, 16, 1, 0xA0000, 0xA000,
                    100, 525, 80, 480, 0),
            new VideoModeBlock(0x013, VGAModes.VGA, 320, 200, 40, 25, 8, 8, 1, 0xA0000, 0x2000, 100,
                    449, 80, 400, 0),

            new VideoModeBlock(0x054, VGAModes.TEXT, 1056, 688, 132, 43, 8, 9, 1, 0xB0000, 0x4000,
                    192, 720, 132, 688, 0),
            new VideoModeBlock(0x055, VGAModes.TEXT, 1056, 400, 132, 25, 8, 16, 1, 0xB0000, 0x2000,
                    192, 449, 132, 400, 0),
            new VideoModeBlock(0x056, VGAModes.TEXT, 1056, 688, 132, 43, 8, 9, 1, 0xB0000, 0x4000,
                    192, 720, 132, 688, 0),
            new VideoModeBlock(0x057, VGAModes.TEXT, 1056, 400, 132, 25, 8, 16, 1, 0xB0000, 0x2000,
                    192, 449, 132, 400, 0),
            new VideoModeBlock(0x058, VGAModes.LIN4, 800, 600, 100, 37, 8, 16, 1, 0xA0000, 0xA000,
                    128, 663, 100, 600, 0),
            new VideoModeBlock(0x05D, VGAModes.LIN4, 1024, 768, 128, 48, 8, 16, 1, 0xA0000, 0x10000,
                    128, 800, 128, 768, 0), // documented only on C00 upwards
            new VideoModeBlock(0x05E, VGAModes.LIN8, 640, 400, 80, 25, 8, 16, 1, 0xA0000, 0x10000,
                    100, 449, 80, 400, 0),
            new VideoModeBlock(0x05F, VGAModes.LIN8, 640, 480, 80, 30, 8, 16, 1, 0xA0000, 0x10000,
                    100, 525, 80, 480, 0),

            new VideoModeBlock(0xFFFF, VGAModes.ERROR, 0, 0, 0, 0, 0, 0, 0, 0x00000, 0x0000, 0, 0,
                    0, 0, 0),};

    private static VideoModeBlock[] ModeList_EGA = {
            /*
             * mode ,type ,sw ,sh ,tw ,th ,cw,ch ,pt,pstart ,plength,htot,vtot,hde,vde special flags
             */
            new VideoModeBlock(0x000, VGAModes.TEXT, 320, 350, 40, 25, 8, 14, 8, 0xB8000, 0x0800,
                    50, 366, 40, 350, EGA_HALF_CLOCK),
            new VideoModeBlock(0x001, VGAModes.TEXT, 320, 350, 40, 25, 8, 14, 8, 0xB8000, 0x0800,
                    50, 366, 40, 350, EGA_HALF_CLOCK),
            new VideoModeBlock(0x002, VGAModes.TEXT, 640, 350, 80, 25, 8, 14, 8, 0xB8000, 0x1000,
                    96, 366, 80, 350, 0),
            new VideoModeBlock(0x003, VGAModes.TEXT, 640, 350, 80, 25, 8, 14, 8, 0xB8000, 0x1000,
                    96, 366, 80, 350, 0),
            new VideoModeBlock(0x004, VGAModes.CGA4, 320, 200, 40, 25, 8, 8, 1, 0xB8000, 0x4000, 60,
                    262, 40, 200, EGA_HALF_CLOCK | EGA_LINE_DOUBLE),
            new VideoModeBlock(0x005, VGAModes.CGA4, 320, 200, 40, 25, 8, 8, 1, 0xB8000, 0x4000, 60,
                    262, 40, 200, EGA_HALF_CLOCK | EGA_LINE_DOUBLE),
            new VideoModeBlock(0x006, VGAModes.CGA2, 640, 200, 80, 25, 8, 8, 1, 0xB8000, 0x4000,
                    120, 262, 80, 200, EGA_LINE_DOUBLE),
            new VideoModeBlock(0x007, VGAModes.TEXT, 720, 350, 80, 25, 9, 14, 8, 0xB0000, 0x1000,
                    120, 440, 80, 350, 0),

            new VideoModeBlock(0x00D, VGAModes.EGA, 320, 200, 40, 25, 8, 8, 8, 0xA0000, 0x2000, 60,
                    262, 40, 200, EGA_HALF_CLOCK | EGA_LINE_DOUBLE),
            new VideoModeBlock(0x00E, VGAModes.EGA, 640, 200, 80, 25, 8, 8, 4, 0xA0000, 0x4000, 120,
                    262, 80, 200, EGA_LINE_DOUBLE),
            new VideoModeBlock(0x00F, VGAModes.EGA, 640, 350, 80, 25, 8, 14, 2, 0xA0000, 0x8000, 96,
                    366, 80, 350, 0), /* was EGA_2 */
            new VideoModeBlock(0x010, VGAModes.EGA, 640, 350, 80, 25, 8, 14, 2, 0xA0000, 0x8000, 96,
                    366, 80, 350, 0),

            new VideoModeBlock(0xFFFF, VGAModes.ERROR, 0, 0, 0, 0, 0, 0, 0, 0x00000, 0x0000, 0, 0,
                    0, 0, 0),};
    private static VideoModeBlock[] ModeList_OTHER = {
            /*
             * mode ,type ,sw ,sh ,tw ,th ,cw,ch ,pt,pstart ,plength,htot,vtot,hde,vde ,special
             * flags
             */
            new VideoModeBlock(0x000, VGAModes.TEXT, 320, 400, 40, 25, 8, 8, 8, 0xB8000, 0x0800, 56,
                    31, 40, 25, 0),
            new VideoModeBlock(0x001, VGAModes.TEXT, 320, 400, 40, 25, 8, 8, 8, 0xB8000, 0x0800, 56,
                    31, 40, 25, 0),
            new VideoModeBlock(0x002, VGAModes.TEXT, 640, 400, 80, 25, 8, 8, 4, 0xB8000, 0x1000,
                    113, 31, 80, 25, 0),
            new VideoModeBlock(0x003, VGAModes.TEXT, 640, 400, 80, 25, 8, 8, 4, 0xB8000, 0x1000,
                    113, 31, 80, 25, 0),
            new VideoModeBlock(0x004, VGAModes.CGA4, 320, 200, 40, 25, 8, 8, 4, 0xB8000, 0x0800, 56,
                    127, 40, 100, 0),
            new VideoModeBlock(0x005, VGAModes.CGA4, 320, 200, 40, 25, 8, 8, 4, 0xB8000, 0x0800, 56,
                    127, 40, 100, 0),
            new VideoModeBlock(0x006, VGAModes.CGA2, 640, 200, 80, 25, 8, 8, 4, 0xB8000, 0x0800, 56,
                    127, 40, 100, 0),
            new VideoModeBlock(0x008, VGAModes.TANDY16, 160, 200, 20, 25, 8, 8, 8, 0xB8000, 0x2000,
                    56, 127, 40, 100, 0),
            new VideoModeBlock(0x009, VGAModes.TANDY16, 320, 200, 40, 25, 8, 8, 8, 0xB8000, 0x2000,
                    113, 63, 80, 50, 0),
            new VideoModeBlock(0x00A, VGAModes.CGA4, 640, 200, 80, 25, 8, 8, 8, 0xB8000, 0x2000,
                    113, 63, 80, 50, 0),
            new VideoModeBlock(0xFFFF, VGAModes.ERROR, 0, 0, 0, 0, 0, 0, 0, 0x00000, 0x0000, 0, 0,
                    0, 0, 0),};

    private static VideoModeBlock Hercules_Mode = new VideoModeBlock(0x007, VGAModes.TEXT, 640, 400,
            80, 25, 8, 14, 1, 0xB0000, 0x1000, 97, 25, 80, 25, 0);

    static byte[][] textPalette =
            {{0x00, 0x00, 0x00}, {0x00, 0x00, 0x2a}, {0x00, 0x2a, 0x00}, {0x00, 0x2a, 0x2a},
                    {0x2a, 0x00, 0x00}, {0x2a, 0x00, 0x2a}, {0x2a, 0x2a, 0x00}, {0x2a, 0x2a, 0x2a},
                    {0x00, 0x00, 0x15}, {0x00, 0x00, 0x3f}, {0x00, 0x2a, 0x15}, {0x00, 0x2a, 0x3f},
                    {0x2a, 0x00, 0x15}, {0x2a, 0x00, 0x3f}, {0x2a, 0x2a, 0x15}, {0x2a, 0x2a, 0x3f},
                    {0x00, 0x15, 0x00}, {0x00, 0x15, 0x2a}, {0x00, 0x3f, 0x00}, {0x00, 0x3f, 0x2a},
                    {0x2a, 0x15, 0x00}, {0x2a, 0x15, 0x2a}, {0x2a, 0x3f, 0x00}, {0x2a, 0x3f, 0x2a},
                    {0x00, 0x15, 0x15}, {0x00, 0x15, 0x3f}, {0x00, 0x3f, 0x15}, {0x00, 0x3f, 0x3f},
                    {0x2a, 0x15, 0x15}, {0x2a, 0x15, 0x3f}, {0x2a, 0x3f, 0x15}, {0x2a, 0x3f, 0x3f},
                    {0x15, 0x00, 0x00}, {0x15, 0x00, 0x2a}, {0x15, 0x2a, 0x00}, {0x15, 0x2a, 0x2a},
                    {0x3f, 0x00, 0x00}, {0x3f, 0x00, 0x2a}, {0x3f, 0x2a, 0x00}, {0x3f, 0x2a, 0x2a},
                    {0x15, 0x00, 0x15}, {0x15, 0x00, 0x3f}, {0x15, 0x2a, 0x15}, {0x15, 0x2a, 0x3f},
                    {0x3f, 0x00, 0x15}, {0x3f, 0x00, 0x3f}, {0x3f, 0x2a, 0x15}, {0x3f, 0x2a, 0x3f},
                    {0x15, 0x15, 0x00}, {0x15, 0x15, 0x2a}, {0x15, 0x3f, 0x00}, {0x15, 0x3f, 0x2a},
                    {0x3f, 0x15, 0x00}, {0x3f, 0x15, 0x2a}, {0x3f, 0x3f, 0x00}, {0x3f, 0x3f, 0x2a},
                    {0x15, 0x15, 0x15}, {0x15, 0x15, 0x3f}, {0x15, 0x3f, 0x15}, {0x15, 0x3f, 0x3f},
                    {0x3f, 0x15, 0x15}, {0x3f, 0x15, 0x3f}, {0x3f, 0x3f, 0x15}, {0x3f, 0x3f, 0x3f}};

    static byte[][] mtextPalette =
            {{0x00, 0x00, 0x00}, {0x00, 0x00, 0x00}, {0x00, 0x00, 0x00}, {0x00, 0x00, 0x00},
                    {0x00, 0x00, 0x00}, {0x00, 0x00, 0x00}, {0x00, 0x00, 0x00}, {0x00, 0x00, 0x00},
                    {0x2a, 0x2a, 0x2a}, {0x2a, 0x2a, 0x2a}, {0x2a, 0x2a, 0x2a}, {0x2a, 0x2a, 0x2a},
                    {0x2a, 0x2a, 0x2a}, {0x2a, 0x2a, 0x2a}, {0x2a, 0x2a, 0x2a}, {0x2a, 0x2a, 0x2a},
                    {0x00, 0x00, 0x00}, {0x00, 0x00, 0x00}, {0x00, 0x00, 0x00}, {0x00, 0x00, 0x00},
                    {0x00, 0x00, 0x00}, {0x00, 0x00, 0x00}, {0x00, 0x00, 0x00}, {0x00, 0x00, 0x00},
                    {0x3f, 0x3f, 0x3f}, {0x3f, 0x3f, 0x3f}, {0x3f, 0x3f, 0x3f}, {0x3f, 0x3f, 0x3f},
                    {0x3f, 0x3f, 0x3f}, {0x3f, 0x3f, 0x3f}, {0x3f, 0x3f, 0x3f}, {0x3f, 0x3f, 0x3f},
                    {0x00, 0x00, 0x00}, {0x00, 0x00, 0x00}, {0x00, 0x00, 0x00}, {0x00, 0x00, 0x00},
                    {0x00, 0x00, 0x00}, {0x00, 0x00, 0x00}, {0x00, 0x00, 0x00}, {0x00, 0x00, 0x00},
                    {0x2a, 0x2a, 0x2a}, {0x2a, 0x2a, 0x2a}, {0x2a, 0x2a, 0x2a}, {0x2a, 0x2a, 0x2a},
                    {0x2a, 0x2a, 0x2a}, {0x2a, 0x2a, 0x2a}, {0x2a, 0x2a, 0x2a}, {0x2a, 0x2a, 0x2a},
                    {0x00, 0x00, 0x00}, {0x00, 0x00, 0x00}, {0x00, 0x00, 0x00}, {0x00, 0x00, 0x00},
                    {0x00, 0x00, 0x00}, {0x00, 0x00, 0x00}, {0x00, 0x00, 0x00}, {0x00, 0x00, 0x00},
                    {0x3f, 0x3f, 0x3f}, {0x3f, 0x3f, 0x3f}, {0x3f, 0x3f, 0x3f}, {0x3f, 0x3f, 0x3f},
                    {0x3f, 0x3f, 0x3f}, {0x3f, 0x3f, 0x3f}, {0x3f, 0x3f, 0x3f}, {0x3f, 0x3f, 0x3f}};

    static byte[][] mtextS3Palette =
            {{0x00, 0x00, 0x00}, {0x00, 0x00, 0x00}, {0x00, 0x00, 0x00}, {0x00, 0x00, 0x00},
                    {0x00, 0x00, 0x00}, {0x00, 0x00, 0x00}, {0x00, 0x00, 0x00}, {0x00, 0x00, 0x00},
                    {0x2a, 0x2a, 0x2a}, {0x2a, 0x2a, 0x2a}, {0x2a, 0x2a, 0x2a}, {0x2a, 0x2a, 0x2a},
                    {0x2a, 0x2a, 0x2a}, {0x2a, 0x2a, 0x2a}, {0x2a, 0x2a, 0x2a}, {0x2a, 0x2a, 0x2a},
                    {0x2a, 0x2a, 0x2a}, {0x2a, 0x2a, 0x2a}, {0x2a, 0x2a, 0x2a}, {0x2a, 0x2a, 0x2a},
                    {0x2a, 0x2a, 0x2a}, {0x2a, 0x2a, 0x2a}, {0x2a, 0x2a, 0x2a}, {0x2a, 0x2a, 0x2a},
                    {0x3f, 0x3f, 0x3f}, {0x3f, 0x3f, 0x3f}, {0x3f, 0x3f, 0x3f}, {0x3f, 0x3f, 0x3f},
                    {0x3f, 0x3f, 0x3f}, {0x3f, 0x3f, 0x3f}, {0x3f, 0x3f, 0x3f}, {0x3f, 0x3f, 0x3f},
                    {0x00, 0x00, 0x00}, {0x00, 0x00, 0x00}, {0x00, 0x00, 0x00}, {0x00, 0x00, 0x00},
                    {0x00, 0x00, 0x00}, {0x00, 0x00, 0x00}, {0x00, 0x00, 0x00}, {0x00, 0x00, 0x00},
                    {0x2a, 0x2a, 0x2a}, {0x2a, 0x2a, 0x2a}, {0x2a, 0x2a, 0x2a}, {0x2a, 0x2a, 0x2a},
                    {0x2a, 0x2a, 0x2a}, {0x2a, 0x2a, 0x2a}, {0x2a, 0x2a, 0x2a}, {0x2a, 0x2a, 0x2a},
                    {0x2a, 0x2a, 0x2a}, {0x2a, 0x2a, 0x2a}, {0x2a, 0x2a, 0x2a}, {0x2a, 0x2a, 0x2a},
                    {0x2a, 0x2a, 0x2a}, {0x2a, 0x2a, 0x2a}, {0x2a, 0x2a, 0x2a}, {0x2a, 0x2a, 0x2a},
                    {0x3f, 0x3f, 0x3f}, {0x3f, 0x3f, 0x3f}, {0x3f, 0x3f, 0x3f}, {0x3f, 0x3f, 0x3f},
                    {0x3f, 0x3f, 0x3f}, {0x3f, 0x3f, 0x3f}, {0x3f, 0x3f, 0x3f}, {0x3f, 0x3f, 0x3f}};

    static byte[][] egaPalette =
            {{0x00, 0x00, 0x00}, {0x00, 0x00, 0x2a}, {0x00, 0x2a, 0x00}, {0x00, 0x2a, 0x2a},
                    {0x2a, 0x00, 0x00}, {0x2a, 0x00, 0x2a}, {0x2a, 0x15, 0x00}, {0x2a, 0x2a, 0x2a},
                    {0x00, 0x00, 0x00}, {0x00, 0x00, 0x2a}, {0x00, 0x2a, 0x00}, {0x00, 0x2a, 0x2a},
                    {0x2a, 0x00, 0x00}, {0x2a, 0x00, 0x2a}, {0x2a, 0x15, 0x00}, {0x2a, 0x2a, 0x2a},
                    {0x15, 0x15, 0x15}, {0x15, 0x15, 0x3f}, {0x15, 0x3f, 0x15}, {0x15, 0x3f, 0x3f},
                    {0x3f, 0x15, 0x15}, {0x3f, 0x15, 0x3f}, {0x3f, 0x3f, 0x15}, {0x3f, 0x3f, 0x3f},
                    {0x15, 0x15, 0x15}, {0x15, 0x15, 0x3f}, {0x15, 0x3f, 0x15}, {0x15, 0x3f, 0x3f},
                    {0x3f, 0x15, 0x15}, {0x3f, 0x15, 0x3f}, {0x3f, 0x3f, 0x15}, {0x3f, 0x3f, 0x3f},
                    {0x00, 0x00, 0x00}, {0x00, 0x00, 0x2a}, {0x00, 0x2a, 0x00}, {0x00, 0x2a, 0x2a},
                    {0x2a, 0x00, 0x00}, {0x2a, 0x00, 0x2a}, {0x2a, 0x15, 0x00}, {0x2a, 0x2a, 0x2a},
                    {0x00, 0x00, 0x00}, {0x00, 0x00, 0x2a}, {0x00, 0x2a, 0x00}, {0x00, 0x2a, 0x2a},
                    {0x2a, 0x00, 0x00}, {0x2a, 0x00, 0x2a}, {0x2a, 0x15, 0x00}, {0x2a, 0x2a, 0x2a},
                    {0x15, 0x15, 0x15}, {0x15, 0x15, 0x3f}, {0x15, 0x3f, 0x15}, {0x15, 0x3f, 0x3f},
                    {0x3f, 0x15, 0x15}, {0x3f, 0x15, 0x3f}, {0x3f, 0x3f, 0x15}, {0x3f, 0x3f, 0x3f},
                    {0x15, 0x15, 0x15}, {0x15, 0x15, 0x3f}, {0x15, 0x3f, 0x15}, {0x15, 0x3f, 0x3f},
                    {0x3f, 0x15, 0x15}, {0x3f, 0x15, 0x3f}, {0x3f, 0x3f, 0x15}, {0x3f, 0x3f, 0x3f}};

    static byte[][] cga_palette = {{0x00, 0x00, 0x00}, {0x00, 0x00, 0x2a}, {0x00, 0x2a, 0x00},
            {0x00, 0x2a, 0x2a}, {0x2a, 0x00, 0x00}, {0x2a, 0x00, 0x2a}, {0x2a, 0x15, 0x00},
            {0x2a, 0x2a, 0x2a}, {0x15, 0x15, 0x15}, {0x15, 0x15, 0x3f}, {0x15, 0x3f, 0x15},
            {0x15, 0x3f, 0x3f}, {0x3f, 0x15, 0x15}, {0x3f, 0x15, 0x3f}, {0x3f, 0x3f, 0x15},
            {0x3f, 0x3f, 0x3f},};

    static byte[][] cgaPalette2 = {{0x00, 0x00, 0x00}, {0x00, 0x00, 0x2a}, {0x00, 0x2a, 0x00},
            {0x00, 0x2a, 0x2a}, {0x2a, 0x00, 0x00}, {0x2a, 0x00, 0x2a}, {0x2a, 0x15, 0x00},
            {0x2a, 0x2a, 0x2a}, {0x00, 0x00, 0x00}, {0x00, 0x00, 0x2a}, {0x00, 0x2a, 0x00},
            {0x00, 0x2a, 0x2a}, {0x2a, 0x00, 0x00}, {0x2a, 0x00, 0x2a}, {0x2a, 0x15, 0x00},
            {0x2a, 0x2a, 0x2a}, {0x15, 0x15, 0x15}, {0x15, 0x15, 0x3f}, {0x15, 0x3f, 0x15},
            {0x15, 0x3f, 0x3f}, {0x3f, 0x15, 0x15}, {0x3f, 0x15, 0x3f}, {0x3f, 0x3f, 0x15},
            {0x3f, 0x3f, 0x3f}, {0x15, 0x15, 0x15}, {0x15, 0x15, 0x3f}, {0x15, 0x3f, 0x15},
            {0x15, 0x3f, 0x3f}, {0x3f, 0x15, 0x15}, {0x3f, 0x15, 0x3f}, {0x3f, 0x3f, 0x15},
            {0x3f, 0x3f, 0x3f}, {0x00, 0x00, 0x00}, {0x00, 0x00, 0x2a}, {0x00, 0x2a, 0x00},
            {0x00, 0x2a, 0x2a}, {0x2a, 0x00, 0x00}, {0x2a, 0x00, 0x2a}, {0x2a, 0x15, 0x00},
            {0x2a, 0x2a, 0x2a}, {0x00, 0x00, 0x00}, {0x00, 0x00, 0x2a}, {0x00, 0x2a, 0x00},
            {0x00, 0x2a, 0x2a}, {0x2a, 0x00, 0x00}, {0x2a, 0x00, 0x2a}, {0x2a, 0x15, 0x00},
            {0x2a, 0x2a, 0x2a}, {0x15, 0x15, 0x15}, {0x15, 0x15, 0x3f}, {0x15, 0x3f, 0x15},
            {0x15, 0x3f, 0x3f}, {0x3f, 0x15, 0x15}, {0x3f, 0x15, 0x3f}, {0x3f, 0x3f, 0x15},
            {0x3f, 0x3f, 0x3f}, {0x15, 0x15, 0x15}, {0x15, 0x15, 0x3f}, {0x15, 0x3f, 0x15},
            {0x15, 0x3f, 0x3f}, {0x3f, 0x15, 0x15}, {0x3f, 0x15, 0x3f}, {0x3f, 0x3f, 0x15},
            {0x3f, 0x3f, 0x3f},};

    static byte[][] vgaPalette =
            {{0x00, 0x00, 0x00}, {0x00, 0x00, 0x2a}, {0x00, 0x2a, 0x00}, {0x00, 0x2a, 0x2a},
                    {0x2a, 0x00, 0x00}, {0x2a, 0x00, 0x2a}, {0x2a, 0x15, 0x00}, {0x2a, 0x2a, 0x2a},
                    {0x15, 0x15, 0x15}, {0x15, 0x15, 0x3f}, {0x15, 0x3f, 0x15}, {0x15, 0x3f, 0x3f},
                    {0x3f, 0x15, 0x15}, {0x3f, 0x15, 0x3f}, {0x3f, 0x3f, 0x15}, {0x3f, 0x3f, 0x3f},
                    {0x00, 0x00, 0x00}, {0x05, 0x05, 0x05}, {0x08, 0x08, 0x08}, {0x0b, 0x0b, 0x0b},
                    {0x0e, 0x0e, 0x0e}, {0x11, 0x11, 0x11}, {0x14, 0x14, 0x14}, {0x18, 0x18, 0x18},
                    {0x1c, 0x1c, 0x1c}, {0x20, 0x20, 0x20}, {0x24, 0x24, 0x24}, {0x28, 0x28, 0x28},
                    {0x2d, 0x2d, 0x2d}, {0x32, 0x32, 0x32}, {0x38, 0x38, 0x38}, {0x3f, 0x3f, 0x3f},
                    {0x00, 0x00, 0x3f}, {0x10, 0x00, 0x3f}, {0x1f, 0x00, 0x3f}, {0x2f, 0x00, 0x3f},
                    {0x3f, 0x00, 0x3f}, {0x3f, 0x00, 0x2f}, {0x3f, 0x00, 0x1f}, {0x3f, 0x00, 0x10},
                    {0x3f, 0x00, 0x00}, {0x3f, 0x10, 0x00}, {0x3f, 0x1f, 0x00}, {0x3f, 0x2f, 0x00},
                    {0x3f, 0x3f, 0x00}, {0x2f, 0x3f, 0x00}, {0x1f, 0x3f, 0x00}, {0x10, 0x3f, 0x00},
                    {0x00, 0x3f, 0x00}, {0x00, 0x3f, 0x10}, {0x00, 0x3f, 0x1f}, {0x00, 0x3f, 0x2f},
                    {0x00, 0x3f, 0x3f}, {0x00, 0x2f, 0x3f}, {0x00, 0x1f, 0x3f}, {0x00, 0x10, 0x3f},
                    {0x1f, 0x1f, 0x3f}, {0x27, 0x1f, 0x3f}, {0x2f, 0x1f, 0x3f}, {0x37, 0x1f, 0x3f},
                    {0x3f, 0x1f, 0x3f}, {0x3f, 0x1f, 0x37}, {0x3f, 0x1f, 0x2f}, {0x3f, 0x1f, 0x27},

                    {0x3f, 0x1f, 0x1f}, {0x3f, 0x27, 0x1f}, {0x3f, 0x2f, 0x1f}, {0x3f, 0x37, 0x1f},
                    {0x3f, 0x3f, 0x1f}, {0x37, 0x3f, 0x1f}, {0x2f, 0x3f, 0x1f}, {0x27, 0x3f, 0x1f},
                    {0x1f, 0x3f, 0x1f}, {0x1f, 0x3f, 0x27}, {0x1f, 0x3f, 0x2f}, {0x1f, 0x3f, 0x37},
                    {0x1f, 0x3f, 0x3f}, {0x1f, 0x37, 0x3f}, {0x1f, 0x2f, 0x3f}, {0x1f, 0x27, 0x3f},
                    {0x2d, 0x2d, 0x3f}, {0x31, 0x2d, 0x3f}, {0x36, 0x2d, 0x3f}, {0x3a, 0x2d, 0x3f},
                    {0x3f, 0x2d, 0x3f}, {0x3f, 0x2d, 0x3a}, {0x3f, 0x2d, 0x36}, {0x3f, 0x2d, 0x31},
                    {0x3f, 0x2d, 0x2d}, {0x3f, 0x31, 0x2d}, {0x3f, 0x36, 0x2d}, {0x3f, 0x3a, 0x2d},
                    {0x3f, 0x3f, 0x2d}, {0x3a, 0x3f, 0x2d}, {0x36, 0x3f, 0x2d}, {0x31, 0x3f, 0x2d},
                    {0x2d, 0x3f, 0x2d}, {0x2d, 0x3f, 0x31}, {0x2d, 0x3f, 0x36}, {0x2d, 0x3f, 0x3a},
                    {0x2d, 0x3f, 0x3f}, {0x2d, 0x3a, 0x3f}, {0x2d, 0x36, 0x3f}, {0x2d, 0x31, 0x3f},
                    {0x00, 0x00, 0x1c}, {0x07, 0x00, 0x1c}, {0x0e, 0x00, 0x1c}, {0x15, 0x00, 0x1c},
                    {0x1c, 0x00, 0x1c}, {0x1c, 0x00, 0x15}, {0x1c, 0x00, 0x0e}, {0x1c, 0x00, 0x07},
                    {0x1c, 0x00, 0x00}, {0x1c, 0x07, 0x00}, {0x1c, 0x0e, 0x00}, {0x1c, 0x15, 0x00},
                    {0x1c, 0x1c, 0x00}, {0x15, 0x1c, 0x00}, {0x0e, 0x1c, 0x00}, {0x07, 0x1c, 0x00},
                    {0x00, 0x1c, 0x00}, {0x00, 0x1c, 0x07}, {0x00, 0x1c, 0x0e}, {0x00, 0x1c, 0x15},
                    {0x00, 0x1c, 0x1c}, {0x00, 0x15, 0x1c}, {0x00, 0x0e, 0x1c}, {0x00, 0x07, 0x1c},

                    {0x0e, 0x0e, 0x1c}, {0x11, 0x0e, 0x1c}, {0x15, 0x0e, 0x1c}, {0x18, 0x0e, 0x1c},
                    {0x1c, 0x0e, 0x1c}, {0x1c, 0x0e, 0x18}, {0x1c, 0x0e, 0x15}, {0x1c, 0x0e, 0x11},
                    {0x1c, 0x0e, 0x0e}, {0x1c, 0x11, 0x0e}, {0x1c, 0x15, 0x0e}, {0x1c, 0x18, 0x0e},
                    {0x1c, 0x1c, 0x0e}, {0x18, 0x1c, 0x0e}, {0x15, 0x1c, 0x0e}, {0x11, 0x1c, 0x0e},
                    {0x0e, 0x1c, 0x0e}, {0x0e, 0x1c, 0x11}, {0x0e, 0x1c, 0x15}, {0x0e, 0x1c, 0x18},
                    {0x0e, 0x1c, 0x1c}, {0x0e, 0x18, 0x1c}, {0x0e, 0x15, 0x1c}, {0x0e, 0x11, 0x1c},
                    {0x14, 0x14, 0x1c}, {0x16, 0x14, 0x1c}, {0x18, 0x14, 0x1c}, {0x1a, 0x14, 0x1c},
                    {0x1c, 0x14, 0x1c}, {0x1c, 0x14, 0x1a}, {0x1c, 0x14, 0x18}, {0x1c, 0x14, 0x16},
                    {0x1c, 0x14, 0x14}, {0x1c, 0x16, 0x14}, {0x1c, 0x18, 0x14}, {0x1c, 0x1a, 0x14},
                    {0x1c, 0x1c, 0x14}, {0x1a, 0x1c, 0x14}, {0x18, 0x1c, 0x14}, {0x16, 0x1c, 0x14},
                    {0x14, 0x1c, 0x14}, {0x14, 0x1c, 0x16}, {0x14, 0x1c, 0x18}, {0x14, 0x1c, 0x1a},
                    {0x14, 0x1c, 0x1c}, {0x14, 0x1a, 0x1c}, {0x14, 0x18, 0x1c}, {0x14, 0x16, 0x1c},
                    {0x00, 0x00, 0x10}, {0x04, 0x00, 0x10}, {0x08, 0x00, 0x10}, {0x0c, 0x00, 0x10},
                    {0x10, 0x00, 0x10}, {0x10, 0x00, 0x0c}, {0x10, 0x00, 0x08}, {0x10, 0x00, 0x04},
                    {0x10, 0x00, 0x00}, {0x10, 0x04, 0x00}, {0x10, 0x08, 0x00}, {0x10, 0x0c, 0x00},
                    {0x10, 0x10, 0x00}, {0x0c, 0x10, 0x00}, {0x08, 0x10, 0x00}, {0x04, 0x10, 0x00},

                    {0x00, 0x10, 0x00}, {0x00, 0x10, 0x04}, {0x00, 0x10, 0x08}, {0x00, 0x10, 0x0c},
                    {0x00, 0x10, 0x10}, {0x00, 0x0c, 0x10}, {0x00, 0x08, 0x10}, {0x00, 0x04, 0x10},
                    {0x08, 0x08, 0x10}, {0x0a, 0x08, 0x10}, {0x0c, 0x08, 0x10}, {0x0e, 0x08, 0x10},
                    {0x10, 0x08, 0x10}, {0x10, 0x08, 0x0e}, {0x10, 0x08, 0x0c}, {0x10, 0x08, 0x0a},
                    {0x10, 0x08, 0x08}, {0x10, 0x0a, 0x08}, {0x10, 0x0c, 0x08}, {0x10, 0x0e, 0x08},
                    {0x10, 0x10, 0x08}, {0x0e, 0x10, 0x08}, {0x0c, 0x10, 0x08}, {0x0a, 0x10, 0x08},
                    {0x08, 0x10, 0x08}, {0x08, 0x10, 0x0a}, {0x08, 0x10, 0x0c}, {0x08, 0x10, 0x0e},
                    {0x08, 0x10, 0x10}, {0x08, 0x0e, 0x10}, {0x08, 0x0c, 0x10}, {0x08, 0x0a, 0x10},
                    {0x0b, 0x0b, 0x10}, {0x0c, 0x0b, 0x10}, {0x0d, 0x0b, 0x10}, {0x0f, 0x0b, 0x10},
                    {0x10, 0x0b, 0x10}, {0x10, 0x0b, 0x0f}, {0x10, 0x0b, 0x0d}, {0x10, 0x0b, 0x0c},
                    {0x10, 0x0b, 0x0b}, {0x10, 0x0c, 0x0b}, {0x10, 0x0d, 0x0b}, {0x10, 0x0f, 0x0b},
                    {0x10, 0x10, 0x0b}, {0x0f, 0x10, 0x0b}, {0x0d, 0x10, 0x0b}, {0x0c, 0x10, 0x0b},
                    {0x0b, 0x10, 0x0b}, {0x0b, 0x10, 0x0c}, {0x0b, 0x10, 0x0d}, {0x0b, 0x10, 0x0f},
                    {0x0b, 0x10, 0x10}, {0x0b, 0x0f, 0x10}, {0x0b, 0x0d, 0x10}, {0x0b, 0x0c, 0x10},
                    {0x00, 0x00, 0x00}, {0x00, 0x00, 0x00}, {0x00, 0x00, 0x00}, {0x00, 0x00, 0x00},
                    {0x00, 0x00, 0x00}, {0x00, 0x00, 0x00}, {0x00, 0x00, 0x00}, {0x00, 0x00, 0x00}};

    public static VideoModeBlock CurMode;

    private static boolean setCurMode(VideoModeBlock[] modeblock, int mode) {
        int i = 0;
        while (modeblock[i].Mode != 0xffff) {
            if (modeblock[i].Mode != mode)
                i++;
            else {
                if ((!INT10.int10.VesaOldVbe) || (ModeList_VGA[i].Mode < 0x120)) {
                    CurMode = modeblock[i];
                    return true;
                }
                return false;
            }
        }
        return false;
    }


    private static void finishSetMode(boolean clearmem) {
        /* Clear video memory if needs be */
        if (clearmem) {
            switch (CurMode.Type) {
                case CGA4:
                case CGA2:
                case TANDY16:
                    for (int ct = 0; ct < 16 * 1024; ct++) {
                        Memory.realWriteW(0xb800, ct * 2, 0x0000);
                    }
                    break;
                case TEXT: {
                    int seg = (CurMode.Mode == 7 ? 0xb000 : 0xb800);
                    for (int ct = 0; ct < 16 * 1024; ct++)
                        Memory.realWriteW(seg, ct * 2, 0x0720);
                    break;
                }
                case EGA:
                case VGA:
                case LIN8:
                case LIN4:
                case LIN15:
                case LIN16:
                case LIN32:
                    /* Hack we just acess the memory directly */
                    Arrays.fill(VGA.instance().Mem.LinearAlloc, (int) VGA.instance().FastMemBase,
                            VGA.instance().VMemSize, (byte) 0);
                    Arrays.fill(VGA.instance().Mem.LinearAlloc, (int) VGA.instance().FastMemBase,
                            VGA.instance().VMemSize << 1, (byte) 0);
                    break;
            }
        }
        /* Setup the BIOS */
        if (CurMode.Mode < 128)
            Memory.realWriteB(INT10.BIOSMEM_SEG, INT10.BIOSMEM_CURRENT_MODE, CurMode.Mode);
        else
            Memory.realWriteB(INT10.BIOSMEM_SEG, INT10.BIOSMEM_CURRENT_MODE,
                    0xff & (CurMode.Mode - 0x98)); // Looks like the s3 bios
        Memory.realWriteW(INT10.BIOSMEM_SEG, INT10.BIOSMEM_NB_COLS, 0xffff & CurMode.TWidth);
        Memory.realWriteW(INT10.BIOSMEM_SEG, INT10.BIOSMEM_PAGE_SIZE, 0xffff & CurMode.Plength);
        Memory.realWriteW(INT10.BIOSMEM_SEG, INT10.BIOSMEM_CRTC_ADDRESS,
                ((CurMode.Mode == 7) || (CurMode.Mode == 0x0f)) ? 0x3b4 : 0x3d4);
        Memory.realWriteB(INT10.BIOSMEM_SEG, INT10.BIOSMEM_NB_ROWS, 0xff & (CurMode.THeight - 1));
        Memory.realWriteW(INT10.BIOSMEM_SEG, INT10.BIOSMEM_CHAR_HEIGHT, 0xffff & CurMode.CHeight);
        Memory.realWriteB(INT10.BIOSMEM_SEG, INT10.BIOSMEM_VIDEO_CTL, 0x60 | (clearmem ? 0 : 0x80));
        Memory.realWriteB(INT10.BIOSMEM_SEG, INT10.BIOSMEM_SWITCHES, 0x09);

        // this is an index into the dcc table:
        if (DOSBox.isVGAArch())
            Memory.realWriteB(INT10.BIOSMEM_SEG, INT10.BIOSMEM_DCC_INDEX, 0x0b);
        Memory.realWriteD(INT10.BIOSMEM_SEG, INT10.BIOSMEM_VS_POINTER,
                INT10.int10.RomVideoSavePointers);

        // Set cursor shape
        if (CurMode.Type == VGAModes.TEXT) {
            CHAR.setCursorShape(0x06, 07);
        }
        // Set cursor pos for page 0..7
        for (byte ct = 0; ct < 8; ct++)
            CHAR.setCursorPos(0, 0, ct);
        // Set active page 0
        CHAR.setActivePage(0);
        /* Set some interrupt vectors */
        switch (CurMode.CHeight) {
            case 8:
                Memory.realSetVec(0x43, INT10.int10.RomFont8First);
                break;
            case 14:
                Memory.realSetVec(0x43, INT10.int10.RomFont14);
                break;
            case 16:
                Memory.realSetVec(0x43, INT10.int10.RomFont16);
                break;
        }
        /* Tell mouse resolution change */
        Mouse.instance().newVideoMode();
    }

    private static boolean setVideoModeOTHER(int mode, boolean clearmem) {
        switch (DOSBox.Machine) {
            case CGA:
                if (mode > 6)
                    return false;
                // goto GotoNext;
            case TANDY:
            case PCJR:
                // GotoNext:
                if (mode > 0xa)
                    return false;
                if (mode == 7)
                    mode = 0; // PCJR defaults to 0 on illegal mode 7
                if (!setCurMode(ModeList_OTHER, mode)) {
                    Log.logging(Log.LogTypes.INT10, Log.LogServerities.Error,
                            "Trying to set illegal mode %X", mode);
                    return false;
                }
                break;
            case HERC:
                // Only init the adapter if the equipment word is set to monochrome (Testdrive)
                if ((Memory.realReadW(INT10.BIOSMEM_SEG, INT10.BIOSMEM_INITIAL_MODE)
                        & 0x30) != 0x30)
                    return false;
                CurMode = Hercules_Mode;
                mode = 7; // in case the video parameter table is modified
                break;
        }
        Log.logging(Log.LogTypes.INT10, Log.LogServerities.Normal, "Set Video Mode %X", mode);

        /* Setup the VGA to the correct mode */
        // VGA_SetMode(CurMode.type);
        /* Setup the CRTC */
        int crtcBase = DOSBox.Machine == DOSBox.MachineType.HERC ? 0x3b4 : 0x3d4;
        // Horizontal total
        IO.writeW(crtcBase, 0x00 | (CurMode.HTotal) << 8);
        // Horizontal displayed
        IO.writeW(crtcBase, 0x01 | (CurMode.HDispend) << 8);
        // Horizontal sync position
        IO.writeW(crtcBase, 0x02 | (CurMode.HDispend + 1) << 8);
        // Horizontal sync width, seems to be fixed to 0xa, for cga at least, hercules has 0xf
        IO.writeW(crtcBase, 0x03 | (0xa) << 8);
        //// Vertical total
        IO.writeW(crtcBase, 0x04 | (CurMode.VTotal) << 8);
        // Vertical total adjust, 6 for cga,hercules,tandy
        IO.writeW(crtcBase, 0x05 | (6) << 8);
        // Vertical displayed
        IO.writeW(crtcBase, 0x06 | (CurMode.VDispend) << 8);
        // Vertical sync position
        IO.writeW(crtcBase,
                0x07 | (CurMode.VDispend + ((CurMode.VTotal - CurMode.VDispend) / 2) - 1) << 8);
        // Maximum scanline
        int scanline, crtpage;// uint16
        scanline = 8;
        switch (CurMode.Type) {
            case TEXT:
                if (DOSBox.Machine == DOSBox.MachineType.HERC)
                    scanline = 14;
                else
                    scanline = 8;
                break;
            case CGA2:
                scanline = 2;
                break;
            case CGA4:
                if (CurMode.Mode != 0xa)
                    scanline = 2;
                else
                    scanline = 4;
                break;
            case TANDY16:
                if (CurMode.Mode != 0x9)
                    scanline = 2;
                else
                    scanline = 4;
                break;
        }
        IO.writeW(crtcBase, 0x09 | (scanline - 1) << 8);
        // Setup the CGA palette using VGA DAC palette
        for (byte ct = 0; ct < 16; ct++)
            VGA.instance().Dac.setEntry(ct, cga_palette[ct][0], cga_palette[ct][1],
                    cga_palette[ct][2]);
        // Setup the tandy palette
        for (byte ct = 0; ct < 16; ct++)
            VGA.instance().Dac.combineColor(ct, ct);
        // Setup the special registers for each dosbox.machine type
        byte[] modeControlList = {0x2c, 0x28, 0x2d, 0x29, // 0-3
                0x2a, 0x2e, 0x1e, 0x29, // 4-7
                0x2a, 0x2b, 0x3b // 8-a
        };
        byte[] modeControlListPCjr = {0x0c, 0x08, 0x0d, 0x09, // 0-3
                0x0a, 0x0e, 0x0e, 0x09, // 4-7
                0x1a, 0x1b, 0x0b // 8-a
        };
        byte modeControl, colorSelect;
        switch (DOSBox.Machine) {
            case HERC:
                IO.writeB(0x3b8, 0x28); // TEXT mode and blinking characters

                VGA.instance().HercPalette();
                VGA.instance().Dac.combineColor(0, 0);
                VGA.instance().Dac.combineColor(1, 7);

                // attribute controls blinking
                Memory.realWriteB(INT10.BIOSMEM_SEG, INT10.BIOSMEM_CURRENT_MSR, 0x29);
                break;
            case CGA:
                modeControl = modeControlList[CurMode.Mode];
                if (CurMode.Mode == 0x6)
                    colorSelect = 0x3f;
                else
                    colorSelect = 0x30;
                IO.writeB(0x3d8, modeControl);
                IO.writeB(0x3d9, colorSelect);
                Memory.realWriteB(INT10.BIOSMEM_SEG, INT10.BIOSMEM_CURRENT_MSR, modeControl);
                Memory.realWriteB(INT10.BIOSMEM_SEG, INT10.BIOSMEM_CURRENT_PAL, colorSelect);
                break;
            case TANDY:
                /* Init some registers */
                IO.writeB(0x3da, 0x1);
                IO.writeB(0x3de, 0xf); // Palette mask always 0xf
                IO.writeB(0x3da, 0x2);
                IO.writeB(0x3de, 0x0); // black border
                IO.writeB(0x3da, 0x3); // Tandy color overrides?
                switch (CurMode.Mode) {
                    case 0x8:
                        IO.writeB(0x3de, 0x14);
                        break;
                    case 0x9:
                        IO.writeB(0x3de, 0x14);
                        break;
                    case 0xa:
                        IO.writeB(0x3de, 0x0c);
                        break;
                    default:
                        IO.writeB(0x3de, 0x0);
                        break;
                }
                // Clear extended mapping
                IO.writeB(0x3da, 0x5);
                IO.writeB(0x3de, 0x0);
                // Clear monitor mode
                IO.writeB(0x3da, 0x8);
                IO.writeB(0x3de, 0x0);
                crtpage = CurMode.Mode >= 0x9 ? 0xf6 : 0x3f;
                IO.writeB(0x3df, crtpage);
                Memory.realWriteB(INT10.BIOSMEM_SEG, INT10.BIOSMEM_CRTCPU_PAGE, crtpage);
                modeControl = modeControlList[CurMode.Mode];
                if (CurMode.Mode == 0x6 || CurMode.Mode == 0xa)
                    colorSelect = 0x3f;
                else
                    colorSelect = 0x30;
                IO.writeB(0x3d8, modeControl);
                IO.writeB(0x3d9, colorSelect);
                Memory.realWriteB(INT10.BIOSMEM_SEG, INT10.BIOSMEM_CURRENT_MSR, modeControl);
                Memory.realWriteB(INT10.BIOSMEM_SEG, INT10.BIOSMEM_CURRENT_PAL, colorSelect);
                break;
            case PCJR:
                /* Init some registers */
                IO.readB(0x3da);
                IO.writeB(0x3da, 0x1);
                IO.writeB(0x3da, 0xf); // Palette mask always 0xf
                IO.writeB(0x3da, 0x2);
                IO.writeB(0x3da, 0x0); // black border
                IO.writeB(0x3da, 0x3);
                if (CurMode.Mode <= 0x04)
                    IO.writeB(0x3da, 0x02);
                else if (CurMode.Mode == 0x06)
                    IO.writeB(0x3da, 0x08);
                else
                    IO.writeB(0x3da, 0x00);

                /* set CRT/Processor page register */
                if (CurMode.Mode < 0x04)
                    crtpage = 0x3f;
                else if (CurMode.Mode >= 0x09)
                    crtpage = 0xf6;
                else
                    crtpage = 0x7f;
                IO.writeB(0x3df, crtpage);
                Memory.realWriteB(INT10.BIOSMEM_SEG, INT10.BIOSMEM_CRTCPU_PAGE, crtpage);

                modeControl = modeControlListPCjr[CurMode.Mode];
                IO.writeB(0x3da, 0x0);
                IO.writeB(0x3da, modeControl);
                Memory.realWriteB(INT10.BIOSMEM_SEG, INT10.BIOSMEM_CURRENT_MSR, modeControl);

                if (CurMode.Mode == 0x6 || CurMode.Mode == 0xa)
                    colorSelect = 0x3f;
                else
                    colorSelect = 0x30;
                IO.writeB(0x3d9, colorSelect);
                Memory.realWriteB(INT10.BIOSMEM_SEG, INT10.BIOSMEM_CURRENT_PAL, colorSelect);
                break;
        }

        int vparams = Memory.realGetVec(0x1d);
        if ((vparams != Memory.realMake(0xf000, 0xf0a4)) && (mode < 8)) {
            // load crtc parameters from video params table
            short crtc_block_index = 0;
            if (mode < 2)
                crtc_block_index = 0;
            else if (mode < 4)
                crtc_block_index = 1;
            else if (mode < 7)
                crtc_block_index = 2;
            else if (mode == 7)
                crtc_block_index = 3; // MDA mono mode; invalid for others
            else if (mode < 9)
                crtc_block_index = 2;
            else
                crtc_block_index = 3; // Tandy/PCjr modes

            // init CRTC registers
            for (short i = 0; i < 16; i++)
                IO.writeW(crtcBase, i | (Memory.realReadB(Memory.realSeg(vparams),
                        (Memory.realOff(vparams) + i + crtc_block_index * 16)) << 8));
        }
        finishSetMode(clearmem);
        return true;
    }


    public static boolean setVideoMode(int mode) {
        boolean clearmem = true;
        int i;
        if (mode >= 0x100) {
            if ((mode & 0x4000) != 0 && INT10.int10.VesaNoLFB)
                return false;
            if ((mode & 0x8000) != 0)
                clearmem = false;
            mode &= 0xfff;
        }
        if ((mode < 0x100) && (mode & 0x80) != 0) {
            clearmem = false;
            mode -= 0x80;
        }
        INT10.int10.VesaSetMode = 0xffff;
        Log.logging(Log.LogTypes.INT10, Log.LogServerities.Normal, "Set Video Mode %X", mode);
        if (!DOSBox.isEGAVGAArch())
            return setVideoModeOTHER(mode, clearmem);

        /* First read mode setup settings from bios area */
        // byte video_ctl=MemModule.real_readb(INT10.BIOSMEM_SEG,INT10.BIOSMEM_VIDEO_CTL);
        // byte vga_switches=MemModule.real_readb(INT10.BIOSMEM_SEG,INT10.BIOSMEM_SWITCHES);
        int modesetCTL = Memory.realReadB(INT10.BIOSMEM_SEG, INT10.BIOSMEM_MODESET_CTL);

        if (DOSBox.isVGAArch()) {
            if (VGA.instance().SVGADrv.AcceptsMode != null) {
                if (!VGA.instance().SVGADrv.AcceptsMode.exec(mode))
                    return false;
            }

            switch (DOSBox.SVGACard) {
                case TsengET4K:
                case TsengET3K:
                    if (!setCurMode(ModeList_VGA_Tseng, mode)) {
                        Log.logging(Log.LogTypes.INT10, Log.LogServerities.Error,
                                "VGA:Trying to set illegal mode %X", mode);
                        return false;
                    }
                    break;
                case ParadisePVGA1A:
                    if (!setCurMode(ModeList_VGA_Paradise, mode)) {
                        Log.logging(Log.LogTypes.INT10, Log.LogServerities.Error,
                                "VGA:Trying to set illegal mode %X", mode);
                        return false;
                    }
                    break;
                default:
                    if (!setCurMode(ModeList_VGA, mode)) {
                        Log.logging(Log.LogTypes.INT10, Log.LogServerities.Error,
                                "VGA:Trying to set illegal mode %X", mode);
                        return false;
                    }
                    break;
            }
            // check for scanline backwards compatibility (VESA text modes??)
            if (CurMode.Type == VGAModes.TEXT) {
                if ((modesetCTL & 0x90) == 0x80) { // 200 lines emulation
                    if (CurMode.Mode <= 3) {
                        CurMode = ModeList_VGA_Text_200lines[CurMode.Mode];
                    }
                } else if ((modesetCTL & 0x90) == 0x00) { // 350 lines emulation
                    if (CurMode.Mode <= 3) {
                        CurMode = ModeList_VGA_Text_350lines[CurMode.Mode];
                    }
                }
            }
        } else {
            if (!setCurMode(ModeList_EGA, mode)) {
                Log.logging(Log.LogTypes.INT10, Log.LogServerities.Error,
                        "EGA:Trying to set illegal mode %X", mode);
                return false;
            }
        }

        /* Setup the VGA to the correct mode */

        short crtcBase;
        boolean monoMode = (mode == 7) || (mode == 0xf);
        if (monoMode)
            crtcBase = 0x3b4;
        else
            crtcBase = 0x3d4;

        if (DOSBox.isVGAArch() && (DOSBox.SVGACard == DOSBox.SVGACards.S3Trio)) {
            // Disable MMIO here so we can read / write memory
            IO.write(crtcBase, 0x53);
            IO.write(crtcBase + 1, 0x0);
        }

        /* Setup MISC Output Register */
        int miscOutput = 0x2 | (monoMode ? 0x0 : 0x1);

        if ((CurMode.Type == VGAModes.TEXT) && (CurMode.CWidth == 9)) {
            // 28MHz (16MHz EGA) clock for 9-pixel wide chars
            miscOutput |= 0x4;
        }

        switch (CurMode.VDispend) {
            case 400:
                miscOutput |= 0x60;
                break;
            case 480:
                miscOutput |= 0xe0;
                break;
            case 350:
                miscOutput |= 0xa0;
                break;
            default:
                miscOutput |= 0x60;
                break;
        }
        IO.write(0x3c2, miscOutput); // Setup for 3b4 or 3d4

        /* Program Sequencer */
        byte[] seqData = new byte[SEQ_REGS];
        Arrays.fill(seqData, 0, SEQ_REGS, (byte) 0);
        seqData[1] |= 0x01; // 8 dot fonts by default
        if ((CurMode.Special & EGA_HALF_CLOCK) != 0)
            seqData[1] |= 0x08; // Check for half clock
        if ((DOSBox.Machine == DOSBox.MachineType.EGA) && (CurMode.Special & EGA_HALF_CLOCK) != 0)
            seqData[1] |= 0x02;
        seqData[4] |= 0x02; // More than 64kb
        switch (CurMode.Type) {
            case TEXT:
                if (CurMode.CWidth == 9)
                    seqData[1] &= (byte) ~1;
                seqData[2] |= 0x3; // Enable plane 0 and 1
                seqData[4] |= 0x01; // Alpanumeric
                if (DOSBox.isVGAArch())
                    seqData[4] |= 0x04; // odd/even enabled
                break;
            case CGA2:
                seqData[2] |= 0xf; // Enable plane 0
                if (DOSBox.Machine == DOSBox.MachineType.EGA)
                    seqData[4] |= 0x04; // odd/even enabled
                break;
            case CGA4:
                if (DOSBox.Machine == DOSBox.MachineType.EGA)
                    seqData[2] |= 0x03; // Enable plane 0 and 1
                break;
            case LIN4:
            case EGA:
                seqData[2] |= 0xf; // Enable all planes for writing
                if (DOSBox.Machine == DOSBox.MachineType.EGA)
                    seqData[4] |= 0x04; // odd/even enabled
                break;
            case LIN8: // Seems to have the same reg layout from testing
            case LIN15:
            case LIN16:
            case LIN32:
            case VGA:
                seqData[2] |= 0xf; // Enable all planes for writing
                seqData[4] |= 0xc; // Graphics - odd/even - Chained
                break;
        }
        for (byte ct = 0; ct < SEQ_REGS; ct++) {
            IO.write(0x3c4, ct);
            IO.write(0x3c5, seqData[ct]);
        }
        // this may be changed by SVGA chipset emulation
        VGA.instance().Config.CompatibleChain4 = true;

        /* Program CRTC */
        /* First disable write protection */
        IO.write(crtcBase, 0x11);
        IO.write(crtcBase + 1, IO.read((0xffff & crtcBase) + 1) & 0x7f);
        /* Clear all the regs */
        for (byte ct = 0x0; ct <= 0x18; ct++) {
            IO.write(crtcBase, ct);
            IO.write(crtcBase + 1, 0);
        }
        byte overflow = 0;
        byte maxScanline = 0;
        byte verOverflow = 0;
        int horOverflow = 0;// uint8
        /* Horizontal Total */
        IO.write(crtcBase, 0x00);
        IO.write(crtcBase + 1, 0xff & (CurMode.HTotal - 5));
        horOverflow |= 0xff & (((CurMode.HTotal - 5) & 0x100) >>> 8);
        /* Horizontal Display End */
        IO.write(crtcBase, 0x01);
        IO.write(crtcBase + 1, 0xff & (CurMode.HDispend - 1));
        horOverflow |= 0xff & (((CurMode.HDispend - 1) & 0x100) >>> 7);
        /* Start horizontal Blanking */
        IO.write(crtcBase, 0x02);
        IO.write(crtcBase + 1, 0xff & CurMode.HDispend);
        horOverflow |= 0xff & (((CurMode.HDispend) & 0x100) >>> 6);
        /* End horizontal Blanking */
        int blankEnd = (CurMode.HTotal - 2) & 0x7f;
        IO.write(crtcBase, 0x03);
        IO.write(crtcBase + 1, 0xff & (0x80 | (blankEnd & 0x1f)));

        /* Start Horizontal Retrace */
        int retStart;
        if ((CurMode.Special & EGA_HALF_CLOCK) != 0 && (CurMode.Type != VGAModes.CGA2))
            retStart = (CurMode.HDispend + 3);
        else if (CurMode.Type == VGAModes.TEXT)
            retStart = (CurMode.HDispend + 5);
        else
            retStart = (CurMode.HDispend + 4);
        IO.write(crtcBase, 0x04);
        IO.write(crtcBase + 1, 0xff & retStart);
        horOverflow |= 0xff & ((retStart & 0x100) >>> 4);

        /* End Horizontal Retrace */
        int retEnd;
        if ((CurMode.Special & EGA_HALF_CLOCK) != 0) {
            if (CurMode.Type == VGAModes.CGA2)
                retEnd = 0; // mode 6
            else if ((CurMode.Special & EGA_LINE_DOUBLE) != 0)
                retEnd = (CurMode.HTotal - 18) & 0x1f;
            else
                retEnd = ((CurMode.HTotal - 18) & 0x1f) | 0x20; // mode 0&1 have 1 char sync delay
        } else if (CurMode.Type == VGAModes.TEXT)
            retEnd = (CurMode.HTotal - 3) & 0x1f;
        else
            retEnd = (CurMode.HTotal - 4) & 0x1f;

        IO.write(crtcBase, 0x05);
        IO.write(crtcBase + 1, 0xff & (retEnd | (blankEnd & 0x20) << 2));

        /* Vertical Total */
        IO.write(crtcBase, 0x06);
        IO.write(crtcBase + 1, 0xff & (CurMode.VTotal - 2));
        overflow |= 0xff & (((CurMode.VTotal - 2) & 0x100) >>> 8);
        overflow |= 0xff & (((CurMode.VTotal - 2) & 0x200) >>> 4);
        verOverflow |= 0xff & (((CurMode.VTotal - 2) & 0x400) >>> 10);

        int vretrace;
        if (DOSBox.isVGAArch()) {
            switch (CurMode.VDispend) {
                case 400:
                    vretrace = CurMode.VDispend + 12;
                    break;
                case 480:
                    vretrace = CurMode.VDispend + 10;
                    break;
                case 350:
                    vretrace = CurMode.VDispend + 37;
                    break;
                default:
                    vretrace = CurMode.VDispend + 12;
                    break;
            }
        } else {
            switch (CurMode.VDispend) {
                case 350:
                    vretrace = CurMode.VDispend;
                    break;
                default:
                    vretrace = CurMode.VDispend + 24;
                    break;
            }
        }

        /* Vertical Retrace Start */
        IO.write(crtcBase, 0x10);
        IO.write(crtcBase + 1, 0xff & vretrace);
        overflow |= 0xff & ((vretrace & 0x100) >>> 6);
        overflow |= 0xff & ((vretrace & 0x200) >>> 2);
        verOverflow |= 0xff & ((vretrace & 0x400) >>> 6);

        /* Vertical Retrace End */
        IO.write(crtcBase, 0x11);
        IO.write(crtcBase + 1, (vretrace + 2) & 0xF);

        /* Vertical Display End */
        IO.write(crtcBase, 0x12);
        IO.write(crtcBase + 1, 0xff & (CurMode.VDispend - 1));
        overflow |= 0xff & (((CurMode.VDispend - 1) & 0x100) >>> 7);
        overflow |= 0xff & (((CurMode.VDispend - 1) & 0x200) >>> 3);
        verOverflow |= 0xff & (((CurMode.VDispend - 1) & 0x400) >>> 9);

        int vblankTrim;
        if (DOSBox.isVGAArch()) {
            switch (CurMode.VDispend) {
                case 400:
                    vblankTrim = 6;
                    break;
                case 480:
                    vblankTrim = 7;
                    break;
                case 350:
                    vblankTrim = 5;
                    break;
                default:
                    vblankTrim = 8;
                    break;
            }
        } else {
            switch (CurMode.VDispend) {
                case 350:
                    vblankTrim = 0;
                    break;
                default:
                    vblankTrim = 23;
                    break;
            }
        }

        /* Vertical Blank Start */
        IO.write(crtcBase, 0x15);
        IO.write(crtcBase + 1, 0xff & (CurMode.VDispend + vblankTrim));
        overflow |= 0xff & (((CurMode.VDispend + vblankTrim) & 0x100) >>> 5);
        maxScanline |= 0xff & (((CurMode.VDispend + vblankTrim) & 0x200) >>> 4);
        verOverflow |= 0xff & (((CurMode.VDispend + vblankTrim) & 0x400) >>> 8);

        /* Vertical Blank End */
        IO.write(crtcBase, 0x16);
        IO.write(crtcBase + 1, 0xff & (CurMode.VTotal - vblankTrim - 2));

        /* Line Compare */
        int lineCompare = (CurMode.VTotal < 1024) ? 1023 : 2047;
        IO.write(crtcBase, 0x18);
        IO.write(crtcBase + 1, lineCompare & 0xff);
        overflow |= 0xff & ((lineCompare & 0x100) >>> 4);
        maxScanline |= 0xff & ((lineCompare & 0x200) >>> 3);
        verOverflow |= 0xff & ((lineCompare & 0x400) >>> 4);
        int underline = 0;
        /* Maximum scanline / Underline Location */
        if ((CurMode.Special & EGA_LINE_DOUBLE) != 0) {
            if (DOSBox.Machine != DOSBox.MachineType.EGA)
                maxScanline |= 0x80;
        }
        switch (CurMode.Type) {
            case TEXT:
                maxScanline |= 0xff & (CurMode.CHeight - 1);
                // mode 7 uses a diff underline position
                underline = monoMode ? 0x0f : 0x1f;
                break;
            case VGA:
                underline = 0x40;
                maxScanline |= 1; // Vga doesn't use double line but this
                break;
            case LIN8:
            case LIN15:
            case LIN16:
            case LIN32:
                underline = 0x60; // Seems to enable the every 4th clock on my s3
                break;
            case CGA2:
            case CGA4:
                maxScanline |= 1;
                break;
        }
        if (CurMode.VDispend == 350)
            underline = 0x0f;

        IO.write(crtcBase, 0x09);
        IO.write(crtcBase + 1, maxScanline);
        IO.write(crtcBase, 0x14);
        IO.write(crtcBase + 1, underline);

        /* OverFlow */
        IO.write(crtcBase, 0x07);
        IO.write(crtcBase + 1, overflow);

        if (DOSBox.SVGACard == DOSBox.SVGACards.S3Trio) {
            /* Extended Horizontal Overflow */
            IO.write(crtcBase, 0x5d);
            IO.write(crtcBase + 1, horOverflow);
            /* Extended Vertical Overflow */
            IO.write(crtcBase, 0x5e);
            IO.write(crtcBase + 1, verOverflow);
        }

        /* Offset Register */
        int offset;
        switch (CurMode.Type) {
            case LIN8:
                offset = CurMode.SWidth / 8;
                break;
            case LIN15:
            case LIN16:
                offset = 2 * CurMode.SWidth / 8;
                break;
            case LIN32:
                offset = 4 * CurMode.SWidth / 8;
                break;
            default:
                offset = CurMode.HDispend / 2;
                break;
        }
        IO.write(crtcBase, 0x13);
        IO.write(crtcBase + 1, offset & 0xff);

        if (DOSBox.SVGACard == DOSBox.SVGACards.S3Trio) {
            /* Extended System Control 2 Register */
            /* This register actually has more bits but only use the extended offset ones */
            IO.write(crtcBase, 0x51);
            IO.write(crtcBase + 1, (offset & 0x300) >>> 4);
            /* Clear remaining bits of the display start */
            IO.write(crtcBase, 0x69);
            IO.write(crtcBase + 1, 0);
            /* Extended Vertical Overflow */
            IO.write(crtcBase, 0x5e);
            IO.write(crtcBase + 1, verOverflow);
        }

        /* Mode Control */
        short modeControl = 0;

        switch (CurMode.Type) {
            case CGA2:
                modeControl = 0xc2; // 0x06 sets address wrap.
                break;
            case CGA4:
                modeControl = 0xa2;
                break;
            case LIN4:
            case EGA:
                if (CurMode.Mode == 0x11) // 0x11 also sets address wrap. thought maybe all 2 color
                                          // modes did but 0x0f doesn't.
                    modeControl = 0xc3; // so.. 0x11 or 0x0f a one off?
                else {
                    if (DOSBox.Machine == DOSBox.MachineType.EGA) {
                        if ((CurMode.Special & EGA_LINE_DOUBLE) != 0)
                            modeControl = 0xc3;
                        else
                            modeControl = 0x8b;
                    } else {
                        modeControl = 0xe3;
                    }
                }
                break;
            case TEXT:
            case VGA:
            case LIN8:
            case LIN15:
            case LIN16:
            case LIN32:
                modeControl = 0xa3;
                if ((CurMode.Special & VGA_PIXEL_DOUBLE) != 0)
                    modeControl |= 0x08;
                break;
        }

        IO.write(crtcBase, 0x17);
        IO.write(crtcBase + 1, modeControl);
        /* Renable write protection */
        IO.write(crtcBase, 0x11);
        IO.write(crtcBase + 1, 0xff & (IO.read((0xffff & crtcBase) + 1) | 0x80));

        if (DOSBox.SVGACard == DOSBox.SVGACards.S3Trio) {
            /* Setup the correct clock */
            if (CurMode.Mode >= 0x100) {
                miscOutput |= 0xef; // Select clock 3
                int clock = CurMode.VTotal * 8 * CurMode.HTotal * 70;
                VGA.instance().setClock(3, clock / 1000);
            }
            short miscControl2;
            /* Setup Pixel format */
            switch (CurMode.Type) {
                case LIN8:
                    miscControl2 = 0x00;
                    break;
                case LIN15:
                    miscControl2 = 0x30;
                    break;
                case LIN16:
                    miscControl2 = 0x50;
                    break;
                case LIN32:
                    miscControl2 = 0xd0;
                    break;
                default:
                    miscControl2 = 0x0;
                    break;
            }
            IO.writeB(crtcBase, 0x67);
            IO.writeB(crtcBase + 1, miscControl2);
        }

        /* Write Misc Output */
        IO.write(0x3c2, miscOutput);
        /* Program Graphics controller */
        byte[] gfx_data = new byte[GFX_REGS];
        Arrays.fill(gfx_data, 0, GFX_REGS, (byte) 0);
        gfx_data[0x7] = 0xf; /* Color don't care */
        gfx_data[0x8] = (byte) 0xff; /* BitMask */
        switch (CurMode.Type) {
            case TEXT:
                gfx_data[0x5] |= 0x10; // Odd-Even Mode
                gfx_data[0x6] |= (byte) (monoMode ? 0x0a : 0x0e); // Either b800 or b000
                break;
            case LIN8:
            case LIN15:
            case LIN16:
            case LIN32:
            case VGA:
                gfx_data[0x5] |= 0x40; // 256 color mode
                gfx_data[0x6] |= 0x05; // graphics mode at 0xa000-affff
                break;
            case LIN4:
            case EGA:
                gfx_data[0x6] |= 0x05; // graphics mode at 0xa000-affff
                break;
            case CGA4:
                gfx_data[0x5] |= 0x20; // CGA mode
                gfx_data[0x6] |= 0x0f; // graphics mode at at 0xb800=0xbfff
                if (DOSBox.Machine == DOSBox.MachineType.EGA)
                    gfx_data[0x5] |= 0x10;
                break;
            case CGA2:
                if (DOSBox.Machine == DOSBox.MachineType.EGA) {
                    gfx_data[0x6] |= 0x0d; // graphics mode at at 0xb800=0xbfff
                } else {
                    gfx_data[0x6] |= 0x0f; // graphics mode at at 0xb800=0xbfff
                }
                break;
        }
        for (byte ct = 0; ct < GFX_REGS; ct++) {
            IO.write(0x3ce, ct);
            IO.write(0x3cf, gfx_data[ct]);
        }
        byte[] attData = new byte[ATT_REGS];
        Arrays.fill(attData, 0, ATT_REGS, (byte) 0);
        attData[0x12] = 0xf; // Always have all color planes enabled
        /* Program Attribute Controller */
        switch (CurMode.Type) {
            case EGA:
            case LIN4:
                attData[0x10] = 0x01; // Color Graphics
                switch (CurMode.Mode) {
                    case 0x0f:
                        attData[0x10] |= 0x0a; // Monochrome
                        attData[0x01] = 0x08;
                        attData[0x04] = 0x18;
                        attData[0x05] = 0x18;
                        attData[0x09] = 0x08;
                        attData[0x0d] = 0x18;
                        break;
                    case 0x11:
                        for (i = 1; i < 16; i++)
                            attData[i] = 0x3f;
                        break;
                    case 0x10:
                    case 0x12:
                        // goto att_text16;
                        attText16(attData);
                        break;
                    default:
                        if (CurMode.Type == VGAModes.LIN4) {
                            // goto att_text16;
                            attText16(attData);
                            break;
                        }
                        for (byte ct = 0; ct < 8; ct++) {
                            attData[ct] = ct;
                            attData[ct + 8] = (byte) (ct + 0x10);
                        }
                        break;
                }
                break;
            case TANDY16:
                attData[0x10] = 0x01; // Color Graphics
                for (byte ct = 0; ct < 16; ct++)
                    attData[ct] = ct;
                break;
            case TEXT:
                if (CurMode.CWidth == 9) {
                    attData[0x13] = 0x08; // Pel panning on 8, although we don't have 9 dot text
                                          // mode
                    attData[0x10] = 0x0C; // Color Text with blinking, 9 Bit characters
                } else {
                    attData[0x13] = 0x00;
                    attData[0x10] = 0x08; // Color Text with blinking, 8 Bit characters
                }
                Memory.realWriteB(INT10.BIOSMEM_SEG, INT10.BIOSMEM_CURRENT_PAL, 0x30);
                // att_text16:
                attText16(attData);
                break;
            case CGA2:
                attData[0x10] = 0x01; // Color Graphics
                attData[0] = 0x0;
                for (i = 1; i < 0x10; i++)
                    attData[i] = 0x17;
                attData[0x12] = 0x1; // Only enable 1 plane
                Memory.realWriteB(INT10.BIOSMEM_SEG, INT10.BIOSMEM_CURRENT_PAL, 0x3f);
                break;
            case CGA4:
                attData[0x10] = 0x01; // Color Graphics
                attData[0] = 0x0;
                attData[1] = 0x13;
                attData[2] = 0x15;
                attData[3] = 0x17;
                attData[4] = 0x02;
                attData[5] = 0x04;
                attData[6] = 0x06;
                attData[7] = 0x07;
                for (int ct = 0x8; ct < 0x10; ct++)
                    attData[ct] = (byte) (ct + 0x8);
                Memory.realWriteB(INT10.BIOSMEM_SEG, INT10.BIOSMEM_CURRENT_PAL, 0x30);
                break;
            case VGA:
            case LIN8:
            case LIN15:
            case LIN16:
            case LIN32:
                for (byte ct = 0; ct < 16; ct++)
                    attData[ct] = ct;
                attData[0x10] = 0x41; // Color Graphics 8-bit
                break;
        }
        IO.read(monoMode ? 0x3ba : 0x3da);
        if ((modesetCTL & 8) == 0) {
            for (byte ct = 0; ct < ATT_REGS; ct++) {
                IO.write(0x3c0, ct);
                IO.write(0x3c0, attData[ct]);
            }
            VGA.instance().Config.PelPanning = 0;
            IO.write(0x3c0, 0x20);
            IO.write(0x3c0, 0x00); // Disable palette access
            IO.write(0x3c6, 0xff); // Reset Pelmask
            /* Setup the DAC */
            IO.write(0x3c8, 0);
            switch (CurMode.Type) {
                case EGA:
                    if (CurMode.Mode > 0xf) {
                        // goto dac_text16;
                        dacText16();
                        break;
                    } else if (CurMode.Mode == 0xf) {
                        for (i = 0; i < 64; i++) {
                            IO.write(0x3c9, mtextS3Palette[i][0]);
                            IO.write(0x3c9, mtextS3Palette[i][1]);
                            IO.write(0x3c9, mtextS3Palette[i][2]);
                        }
                    } else {
                        for (i = 0; i < 64; i++) {
                            IO.write(0x3c9, egaPalette[i][0]);
                            IO.write(0x3c9, egaPalette[i][1]);
                            IO.write(0x3c9, egaPalette[i][2]);
                        }
                    }
                    break;
                case CGA2:
                case CGA4:
                case TANDY16:
                    for (i = 0; i < 64; i++) {
                        IO.write(0x3c9, cgaPalette2[i][0]);
                        IO.write(0x3c9, cgaPalette2[i][1]);
                        IO.write(0x3c9, cgaPalette2[i][2]);
                    }
                    break;
                case TEXT:
                    if (CurMode.Mode == 7) {
                        if ((DOSBox.isVGAArch()) && (DOSBox.SVGACard == DOSBox.SVGACards.S3Trio)) {
                            for (i = 0; i < 64; i++) {
                                IO.write(0x3c9, mtextS3Palette[i][0]);
                                IO.write(0x3c9, mtextS3Palette[i][1]);
                                IO.write(0x3c9, mtextS3Palette[i][2]);
                            }
                        } else {
                            for (i = 0; i < 64; i++) {
                                IO.write(0x3c9, mtextPalette[i][0]);
                                IO.write(0x3c9, mtextPalette[i][1]);
                                IO.write(0x3c9, mtextPalette[i][2]);
                            }
                        }
                        break;
                    } // FALLTHROUGH!!!!
                      // goto GotoM_LIN4;
                case LIN4: // Added for CAD Software
                    // GotoM_LIN4:
                    // dac_text16:
                    dacText16();

                    break;
                case VGA:
                case LIN8:
                case LIN15:
                case LIN16:
                case LIN32:
                    for (i = 0; i < 256; i++) {
                        IO.write(0x3c9, vgaPalette[i][0]);
                        IO.write(0x3c9, vgaPalette[i][1]);
                        IO.write(0x3c9, vgaPalette[i][2]);
                    }
                    break;
            }
            if (DOSBox.isVGAArch()) {
                /* check if gray scale summing is enabled */
                if ((Memory.realReadB(INT10.BIOSMEM_SEG, INT10.BIOSMEM_MODESET_CTL) & 2) != 0) {
                    PAL.performGrayScaleSumming(0, 256);
                }
            }
        } else {
            for (byte ct = 0x10; ct < ATT_REGS; ct++) {
                if (ct == 0x11)
                    continue; // skip overscan register
                IO.write(0x3c0, ct);
                IO.write(0x3c0, attData[ct]);
            }
            VGA.instance().Config.PelPanning = 0;
            IO.write(0x3c0, 0x20); // Disable palette access
        }
        /* Setup some special stuff for different modes */
        int feature = Memory.realReadB(INT10.BIOSMEM_SEG, INT10.BIOSMEM_INITIAL_MODE);
        switch (CurMode.Type) {
            case CGA2:
                feature = 0xff & ((feature & ~0x30) | 0x20);
                Memory.realWriteB(INT10.BIOSMEM_SEG, INT10.BIOSMEM_CURRENT_MSR, 0x1e);
                break;
            case CGA4:
                feature = 0xff & ((feature & ~0x30) | 0x20);
                if (CurMode.Mode == 4)
                    Memory.realWriteB(INT10.BIOSMEM_SEG, INT10.BIOSMEM_CURRENT_MSR, 0x2a);
                else if (CurMode.Mode == 5)
                    Memory.realWriteB(INT10.BIOSMEM_SEG, INT10.BIOSMEM_CURRENT_MSR, 0x2e);
                else
                    Memory.realWriteB(INT10.BIOSMEM_SEG, INT10.BIOSMEM_CURRENT_MSR, 0x2);
                break;
            case TANDY16:
                feature = 0xff & ((feature & ~0x30) | 0x20);
                break;
            case TEXT:
                feature = 0xff & ((feature & ~0x30) | 0x20);
                switch (CurMode.Mode) {
                    case 0:
                        Memory.realWriteB(INT10.BIOSMEM_SEG, INT10.BIOSMEM_CURRENT_MSR, 0x2c);
                        break;
                    case 1:
                        Memory.realWriteB(INT10.BIOSMEM_SEG, INT10.BIOSMEM_CURRENT_MSR, 0x28);
                        break;
                    case 2:
                        Memory.realWriteB(INT10.BIOSMEM_SEG, INT10.BIOSMEM_CURRENT_MSR, 0x2d);
                        break;
                    case 3:
                    case 7:
                        Memory.realWriteB(INT10.BIOSMEM_SEG, INT10.BIOSMEM_CURRENT_MSR, 0x29);
                        break;
                }
                break;
            case LIN4:
            case EGA:
            case VGA:
                feature = 0xff & (feature & ~0x30);
                break;
        }
        // disabled, has to be set in bios.cpp exclusively
        // MemModule.real_writeb(INT10.BIOSMEM_SEG,INT10.BIOSMEM_INITIAL_MODE,feature);

        if (DOSBox.SVGACard == DOSBox.SVGACards.S3Trio) {
            /* Setup the CPU Window */
            IO.write(crtcBase, 0x6a);
            IO.write(crtcBase + 1, 0);
            /* Setup the linear frame buffer */
            IO.write(crtcBase, 0x59);
            IO.write(crtcBase + 1, (INT10.S3_LFB_BASE >>> 24) & 0xff);
            IO.write(crtcBase, 0x5a);
            IO.write(crtcBase + 1, (INT10.S3_LFB_BASE >>> 16) & 0xff);
            IO.write(crtcBase, 0x6b); // BIOS scratchpad
            IO.write(crtcBase + 1, (INT10.S3_LFB_BASE >>> 24) & 0xff);

            /* Setup some remaining S3 registers */
            IO.write(crtcBase, 0x41); // BIOS scratchpad
            IO.write(crtcBase + 1, 0x88);
            IO.write(crtcBase, 0x52); // extended BIOS scratchpad
            IO.write(crtcBase + 1, 0x80);

            IO.write(0x3c4, 0x15);
            IO.write(0x3c5, 0x03);

            // Accellerator setup
            int reg50 = VGA.S3_XGA_8BPP;
            switch (CurMode.Type) {
                case LIN15:
                case LIN16:
                    reg50 |= VGA.S3_XGA_16BPP;
                    break;
                case LIN32:
                    reg50 |= VGA.S3_XGA_32BPP;
                    break;
                default:
                    break;
            }
            switch (CurMode.SWidth) {
                case 640:
                    reg50 |= VGA.S3_XGA_640;
                    break;
                case 800:
                    reg50 |= VGA.S3_XGA_800;
                    break;
                case 1024:
                    reg50 |= VGA.S3_XGA_1024;
                    break;
                case 1152:
                    reg50 |= VGA.S3_XGA_1152;
                    break;
                case 1280:
                    reg50 |= VGA.S3_XGA_1280;
                    break;
                default:
                    break;
            }
            IO.writeB(crtcBase, 0x50);
            IO.writeB(crtcBase + 1, reg50);

            int reg31, reg3a;
            switch (CurMode.Type) {
                case LIN15:
                case LIN16:
                case LIN32:
                    reg3a = 0x15;
                    break;
                case LIN8:
                    // S3VBE20 does it this way. The other double pixel bit does not
                    // seem to have an effect on the Trio64.
                    if ((CurMode.Special & VGA_PIXEL_DOUBLE) != 0)
                        reg3a = 0x5;
                    else
                        reg3a = 0x15;
                    break;
                default:
                    reg3a = 5;
                    break;
            }

            switch (CurMode.Type) {
                case LIN4: // <- Theres a discrepance with real hardware on this
                case LIN8:
                case LIN15:
                case LIN16:
                case LIN32:
                    reg31 = 9;
                    break;
                default:
                    reg31 = 5;
                    break;
            }
            IO.write(crtcBase, 0x3a);
            IO.write(crtcBase + 1, reg3a);
            IO.write(crtcBase, 0x31);
            IO.write(crtcBase + 1, reg31); // Enable banked memory and 256k+ access
            IO.write(crtcBase, 0x58);
            IO.write(crtcBase + 1, 0x3); // Enable 8 mb of linear addressing

            IO.write(crtcBase, 0x38);
            IO.write(crtcBase + 1, 0x48); // Register lock 1
            IO.write(crtcBase, 0x39);
            IO.write(crtcBase + 1, 0xa5); // Register lock 2
        } else if (VGA.instance().SVGADrv.SetVideoMode != null) {
            ModeExtraData modeData = new ModeExtraData();
            modeData.VOverflow = verOverflow;
            modeData.HOverflow = horOverflow;
            modeData.Offset = offset;
            modeData.ModeNo = CurMode.Mode;
            modeData.HTotal = CurMode.HTotal;
            modeData.VTotal = CurMode.VTotal;
            VGA.instance().SVGADrv.SetVideoMode.exec(crtcBase, modeData);
        }

        finishSetMode(clearmem);

        /* Set vga attrib register into defined state */
        IO.read(monoMode ? 0x3ba : 0x3da);
        IO.write(0x3c0, 0x20);

        /* Load text mode font */
        if (CurMode.Type == VGAModes.TEXT) {
            INT10.reloadFont();
        }
        return true;
    }

    // dac_text16
    private static void dacText16() {
        for (int i = 0; i < 64; i++) {
            IO.write(0x3c9, textPalette[i][0]);
            IO.write(0x3c9, textPalette[i][1]);
            IO.write(0x3c9, textPalette[i][2]);
        }
    }

    // att_text16
    private static void attText16(byte[] attData) {
        if (CurMode.Mode == 7) {
            attData[0] = 0x00;
            attData[8] = 0x10;
            for (int i = 1; i < 8; i++) {
                attData[i] = 0x08;
                attData[i + 8] = 0x18;
            }
        } else {
            for (byte ct = 0; ct < 8; ct++) {
                attData[ct] = ct;
                attData[ct + 8] = (byte) (ct + 0x38);
            }
            if (DOSBox.isVGAArch())
                attData[0x06] = 0x14; // Odd Color 6 yellow/brown.
        }
    }

    public static int videoModeMemSize(int mode) {
        if (!DOSBox.isVGAArch())
            return 0;

        VideoModeBlock[] modelist = null;

        switch (DOSBox.SVGACard) {
            case TsengET4K:
            case TsengET3K:
                modelist = ModeList_VGA_Tseng;
                break;
            case ParadisePVGA1A:
                modelist = ModeList_VGA_Paradise;
                break;
            default:
                modelist = ModeList_VGA;
                break;
        }

        VideoModeBlock vmodeBlock = null;
        int i = 0;
        while (modelist[i].Mode != 0xffff) {
            if (modelist[i].Mode == mode) {
                vmodeBlock = modelist[i];
                break;
            }
            i++;
        }
        if (vmodeBlock == null)
            return 0;

        switch (vmodeBlock.Type) {
            case LIN4:
                return vmodeBlock.SWidth * vmodeBlock.SHeight / 2;
            case LIN8:
                return vmodeBlock.SWidth * vmodeBlock.SHeight;
            case LIN15:
            case LIN16:
                return vmodeBlock.SWidth * vmodeBlock.SHeight * 2;
            case LIN32:
                return vmodeBlock.SWidth * vmodeBlock.SHeight * 4;
            case TEXT:
                return vmodeBlock.TWidth * vmodeBlock.THeight * 2;
        }
        // Return 0 for all other types, those always fit in memory
        return 0;
    }

}
/*--------------------------- end INT10Modes -----------------------------*/
