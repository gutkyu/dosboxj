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

    private static final int _EGA_HALF_CLOCK = 0x0001;
    private static final int _EGA_LINE_DOUBLE = 0x0002;
    private static final int _VGA_PIXEL_DOUBLE = 0x0004;

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
                    50, 449, 40, 400, _EGA_HALF_CLOCK),
            new VideoModeBlock(0x001, VGAModes.TEXT, 360, 400, 40, 25, 9, 16, 8, 0xB8000, 0x0800,
                    50, 449, 40, 400, _EGA_HALF_CLOCK),
            new VideoModeBlock(0x002, VGAModes.TEXT, 720, 400, 80, 25, 9, 16, 8, 0xB8000, 0x1000,
                    100, 449, 80, 400, 0),
            new VideoModeBlock(0x003, VGAModes.TEXT, 720, 400, 80, 25, 9, 16, 8, 0xB8000, 0x1000,
                    100, 449, 80, 400, 0),
            new VideoModeBlock(0x004, VGAModes.CGA4, 320, 200, 40, 25, 8, 8, 1, 0xB8000, 0x4000, 50,
                    449, 40, 400, _EGA_HALF_CLOCK | _EGA_LINE_DOUBLE),
            new VideoModeBlock(0x005, VGAModes.CGA4, 320, 200, 40, 25, 8, 8, 1, 0xB8000, 0x4000, 50,
                    449, 40, 400, _EGA_HALF_CLOCK | _EGA_LINE_DOUBLE),
            new VideoModeBlock(0x006, VGAModes.CGA2, 640, 200, 80, 25, 8, 8, 1, 0xB8000, 0x4000,
                    100, 449, 80, 400, _EGA_HALF_CLOCK | _EGA_LINE_DOUBLE),
            new VideoModeBlock(0x007, VGAModes.TEXT, 720, 400, 80, 25, 9, 16, 8, 0xB0000, 0x1000,
                    100, 449, 80, 400, 0),
            new VideoModeBlock(0x00D, VGAModes.EGA, 320, 200, 40, 25, 8, 8, 8, 0xA0000, 0x2000, 50,
                    449, 40, 400, _EGA_HALF_CLOCK | _EGA_LINE_DOUBLE),
            new VideoModeBlock(0x00E, VGAModes.EGA, 640, 200, 80, 25, 8, 8, 4, 0xA0000, 0x4000, 100,
                    449, 80, 400, _EGA_LINE_DOUBLE),
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
                    100, 449, 80, 400, _VGA_PIXEL_DOUBLE | _EGA_LINE_DOUBLE),
            new VideoModeBlock(0x10E, VGAModes.LIN16, 320, 200, 40, 25, 8, 8, 1, 0xA0000, 0x10000,
                    100, 449, 80, 400, _VGA_PIXEL_DOUBLE | _EGA_LINE_DOUBLE),
            new VideoModeBlock(0x10F, VGAModes.LIN32, 320, 200, 40, 25, 8, 8, 1, 0xA0000, 0x10000,
                    50, 449, 40, 400, _VGA_PIXEL_DOUBLE | _EGA_LINE_DOUBLE),
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
                    100, 449, 80, 400, _VGA_PIXEL_DOUBLE | _EGA_LINE_DOUBLE),
            new VideoModeBlock(0x151, VGAModes.LIN8, 320, 240, 40, 30, 8, 8, 1, 0xA0000, 0x10000,
                    100, 525, 80, 480, _VGA_PIXEL_DOUBLE | _EGA_LINE_DOUBLE),
            new VideoModeBlock(0x152, VGAModes.LIN8, 320, 400, 40, 50, 8, 8, 1, 0xA0000, 0x10000,
                    100, 449, 80, 400, _VGA_PIXEL_DOUBLE),
            new VideoModeBlock(0x153, VGAModes.LIN8, 320, 480, 40, 60, 8, 8, 1, 0xA0000, 0x10000,
                    100, 525, 80, 480, _VGA_PIXEL_DOUBLE),
            new VideoModeBlock(0x160, VGAModes.LIN15, 320, 240, 40, 30, 8, 8, 1, 0xA0000, 0x10000,
                    100, 525, 80, 480, _VGA_PIXEL_DOUBLE | _EGA_LINE_DOUBLE),
            new VideoModeBlock(0x161, VGAModes.LIN15, 320, 400, 40, 50, 8, 8, 1, 0xA0000, 0x10000,
                    100, 449, 80, 400, _VGA_PIXEL_DOUBLE),
            new VideoModeBlock(0x162, VGAModes.LIN15, 320, 480, 40, 60, 8, 8, 1, 0xA0000, 0x10000,
                    100, 525, 80, 480, _VGA_PIXEL_DOUBLE),
            new VideoModeBlock(0x165, VGAModes.LIN15, 640, 400, 80, 25, 8, 16, 1, 0xA0000, 0x10000,
                    200, 449, 160, 400, 0),
            new VideoModeBlock(0x170, VGAModes.LIN16, 320, 240, 40, 30, 8, 8, 1, 0xA0000, 0x10000,
                    100, 525, 80, 480, _VGA_PIXEL_DOUBLE | _EGA_LINE_DOUBLE),
            new VideoModeBlock(0x171, VGAModes.LIN16, 320, 400, 40, 50, 8, 8, 1, 0xA0000, 0x10000,
                    100, 449, 80, 400, _VGA_PIXEL_DOUBLE),
            new VideoModeBlock(0x172, VGAModes.LIN16, 320, 480, 40, 60, 8, 8, 1, 0xA0000, 0x10000,
                    100, 525, 80, 480, _VGA_PIXEL_DOUBLE),
            new VideoModeBlock(0x175, VGAModes.LIN16, 640, 400, 80, 25, 8, 16, 1, 0xA0000, 0x10000,
                    200, 449, 160, 400, 0),
            new VideoModeBlock(0x190, VGAModes.LIN32, 320, 240, 40, 30, 8, 8, 1, 0xA0000, 0x10000,
                    50, 525, 40, 480, _VGA_PIXEL_DOUBLE | _EGA_LINE_DOUBLE),
            new VideoModeBlock(0x191, VGAModes.LIN32, 320, 400, 40, 50, 8, 8, 1, 0xA0000, 0x10000,
                    50, 449, 40, 400, _VGA_PIXEL_DOUBLE),
            new VideoModeBlock(0x192, VGAModes.LIN32, 320, 480, 40, 60, 8, 8, 1, 0xA0000, 0x10000,
                    50, 525, 40, 480, _VGA_PIXEL_DOUBLE),
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
                    449, 40, 400, _EGA_HALF_CLOCK | _EGA_LINE_DOUBLE),
            new VideoModeBlock(0x001, VGAModes.TEXT, 320, 200, 40, 25, 8, 8, 8, 0xB8000, 0x0800, 50,
                    449, 40, 400, _EGA_HALF_CLOCK | _EGA_LINE_DOUBLE),
            new VideoModeBlock(0x002, VGAModes.TEXT, 640, 200, 80, 25, 8, 8, 8, 0xB8000, 0x1000,
                    100, 449, 80, 400, _EGA_LINE_DOUBLE),
            new VideoModeBlock(0x003, VGAModes.TEXT, 640, 200, 80, 25, 8, 8, 8, 0xB8000, 0x1000,
                    100, 449, 80, 400, _EGA_LINE_DOUBLE)};

    private static VideoModeBlock[] ModeList_VGA_Text_350lines = {
            /*
             * mode ,type ,sw ,sh ,tw ,th ,cw,ch ,pt,pstart ,plength,htot,vtot,hde,vde special flags
             */
            new VideoModeBlock(0x000, VGAModes.TEXT, 320, 350, 40, 25, 8, 14, 8, 0xB8000, 0x0800,
                    50, 449, 40, 350, _EGA_HALF_CLOCK),
            new VideoModeBlock(0x001, VGAModes.TEXT, 320, 350, 40, 25, 8, 14, 8, 0xB8000, 0x0800,
                    50, 449, 40, 350, _EGA_HALF_CLOCK),
            new VideoModeBlock(0x002, VGAModes.TEXT, 640, 350, 80, 25, 8, 14, 8, 0xB8000, 0x1000,
                    100, 449, 80, 350, 0),
            new VideoModeBlock(0x003, VGAModes.TEXT, 640, 350, 80, 25, 8, 14, 8, 0xB8000, 0x1000,
                    100, 449, 80, 350, 0)};
    private static VideoModeBlock[] ModeList_VGA_Tseng = {
            /*
             * mode ,type ,sw ,sh ,tw ,th ,cw,ch ,pt,pstart ,plength,htot,vtot,hde,vde special flags
             */
            new VideoModeBlock(0x000, VGAModes.TEXT, 360, 400, 40, 25, 9, 16, 8, 0xB8000, 0x0800,
                    50, 449, 40, 400, _EGA_HALF_CLOCK),
            new VideoModeBlock(0x001, VGAModes.TEXT, 360, 400, 40, 25, 9, 16, 8, 0xB8000, 0x0800,
                    50, 449, 40, 400, _EGA_HALF_CLOCK),
            new VideoModeBlock(0x002, VGAModes.TEXT, 720, 400, 80, 25, 9, 16, 8, 0xB8000, 0x1000,
                    100, 449, 80, 400, 0),
            new VideoModeBlock(0x003, VGAModes.TEXT, 720, 400, 80, 25, 9, 16, 8, 0xB8000, 0x1000,
                    100, 449, 80, 400, 0),
            new VideoModeBlock(0x004, VGAModes.CGA4, 320, 200, 40, 25, 8, 8, 1, 0xB8000, 0x4000, 50,
                    449, 40, 400, _EGA_HALF_CLOCK | _EGA_LINE_DOUBLE),
            new VideoModeBlock(0x005, VGAModes.CGA4, 320, 200, 40, 25, 8, 8, 1, 0xB8000, 0x4000, 50,
                    449, 40, 400, _EGA_HALF_CLOCK | _EGA_LINE_DOUBLE),
            new VideoModeBlock(0x006, VGAModes.CGA2, 640, 200, 80, 25, 8, 8, 1, 0xB8000, 0x4000,
                    100, 449, 80, 400, _EGA_HALF_CLOCK | _EGA_LINE_DOUBLE),
            new VideoModeBlock(0x007, VGAModes.TEXT, 720, 400, 80, 25, 9, 16, 8, 0xB0000, 0x1000,
                    100, 449, 80, 400, 0),

            new VideoModeBlock(0x00D, VGAModes.EGA, 320, 200, 40, 25, 8, 8, 8, 0xA0000, 0x2000, 50,
                    449, 40, 400, _EGA_HALF_CLOCK | _EGA_LINE_DOUBLE),
            new VideoModeBlock(0x00E, VGAModes.EGA, 640, 200, 80, 25, 8, 8, 4, 0xA0000, 0x4000, 100,
                    449, 80, 400, _EGA_LINE_DOUBLE),
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
                    50, 449, 40, 400, _EGA_HALF_CLOCK),
            new VideoModeBlock(0x001, VGAModes.TEXT, 360, 400, 40, 25, 9, 16, 8, 0xB8000, 0x0800,
                    50, 449, 40, 400, _EGA_HALF_CLOCK),
            new VideoModeBlock(0x002, VGAModes.TEXT, 720, 400, 80, 25, 9, 16, 8, 0xB8000, 0x1000,
                    100, 449, 80, 400, 0),
            new VideoModeBlock(0x003, VGAModes.TEXT, 720, 400, 80, 25, 9, 16, 8, 0xB8000, 0x1000,
                    100, 449, 80, 400, 0),
            new VideoModeBlock(0x004, VGAModes.CGA4, 320, 200, 40, 25, 8, 8, 1, 0xB8000, 0x4000, 50,
                    449, 40, 400, _EGA_HALF_CLOCK | _EGA_LINE_DOUBLE),
            new VideoModeBlock(0x005, VGAModes.CGA4, 320, 200, 40, 25, 8, 8, 1, 0xB8000, 0x4000, 50,
                    449, 40, 400, _EGA_HALF_CLOCK | _EGA_LINE_DOUBLE),
            new VideoModeBlock(0x006, VGAModes.CGA2, 640, 200, 80, 25, 8, 8, 1, 0xB8000, 0x4000,
                    100, 449, 80, 400, _EGA_HALF_CLOCK | _EGA_LINE_DOUBLE),
            new VideoModeBlock(0x007, VGAModes.TEXT, 720, 400, 80, 25, 9, 16, 8, 0xB0000, 0x1000,
                    100, 449, 80, 400, 0),

            new VideoModeBlock(0x00D, VGAModes.EGA, 320, 200, 40, 25, 8, 8, 8, 0xA0000, 0x2000, 50,
                    449, 40, 400, _EGA_HALF_CLOCK | _EGA_LINE_DOUBLE),
            new VideoModeBlock(0x00E, VGAModes.EGA, 640, 200, 80, 25, 8, 8, 4, 0xA0000, 0x4000, 100,
                    449, 80, 400, _EGA_LINE_DOUBLE),
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
                    50, 366, 40, 350, _EGA_HALF_CLOCK),
            new VideoModeBlock(0x001, VGAModes.TEXT, 320, 350, 40, 25, 8, 14, 8, 0xB8000, 0x0800,
                    50, 366, 40, 350, _EGA_HALF_CLOCK),
            new VideoModeBlock(0x002, VGAModes.TEXT, 640, 350, 80, 25, 8, 14, 8, 0xB8000, 0x1000,
                    96, 366, 80, 350, 0),
            new VideoModeBlock(0x003, VGAModes.TEXT, 640, 350, 80, 25, 8, 14, 8, 0xB8000, 0x1000,
                    96, 366, 80, 350, 0),
            new VideoModeBlock(0x004, VGAModes.CGA4, 320, 200, 40, 25, 8, 8, 1, 0xB8000, 0x4000, 60,
                    262, 40, 200, _EGA_HALF_CLOCK | _EGA_LINE_DOUBLE),
            new VideoModeBlock(0x005, VGAModes.CGA4, 320, 200, 40, 25, 8, 8, 1, 0xB8000, 0x4000, 60,
                    262, 40, 200, _EGA_HALF_CLOCK | _EGA_LINE_DOUBLE),
            new VideoModeBlock(0x006, VGAModes.CGA2, 640, 200, 80, 25, 8, 8, 1, 0xB8000, 0x4000,
                    120, 262, 80, 200, _EGA_LINE_DOUBLE),
            new VideoModeBlock(0x007, VGAModes.TEXT, 720, 350, 80, 25, 9, 14, 8, 0xB0000, 0x1000,
                    120, 440, 80, 350, 0),

            new VideoModeBlock(0x00D, VGAModes.EGA, 320, 200, 40, 25, 8, 8, 8, 0xA0000, 0x2000, 60,
                    262, 40, 200, _EGA_HALF_CLOCK | _EGA_LINE_DOUBLE),
            new VideoModeBlock(0x00E, VGAModes.EGA, 640, 200, 80, 25, 8, 8, 4, 0xA0000, 0x4000, 120,
                    262, 80, 200, _EGA_LINE_DOUBLE),
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

    static byte[][] text_palette =
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

    static byte[][] mtext_palette =
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

    static byte[][] mtext_s3_palette =
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

    static byte[][] ega_palette =
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

    static byte[][] cga_palette_2 = {{0x00, 0x00, 0x00}, {0x00, 0x00, 0x2a}, {0x00, 0x2a, 0x00},
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

    static byte[][] vga_palette =
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
            Memory.realWriteB(INT10.BIOSMEM_SEG, INT10.BIOSMEM_CURRENT_MODE, (byte) CurMode.Mode);
        else
            Memory.realWriteB(INT10.BIOSMEM_SEG, INT10.BIOSMEM_CURRENT_MODE,
                    (byte) (CurMode.Mode - 0x98)); // Looks like the s3 bios
        Memory.realWriteW(INT10.BIOSMEM_SEG, INT10.BIOSMEM_NB_COLS, CurMode.TWidth);
        Memory.realWriteW(INT10.BIOSMEM_SEG, INT10.BIOSMEM_PAGE_SIZE, CurMode.Plength);
        Memory.realWriteW(INT10.BIOSMEM_SEG, INT10.BIOSMEM_CRTC_ADDRESS,
                ((CurMode.Mode == 7) || (CurMode.Mode == 0x0f)) ? 0x3b4 : 0x3d4);
        Memory.realWriteB(INT10.BIOSMEM_SEG, INT10.BIOSMEM_NB_ROWS, (byte) (CurMode.THeight - 1));
        Memory.realWriteW(INT10.BIOSMEM_SEG, INT10.BIOSMEM_CHAR_HEIGHT, CurMode.CHeight);
        Memory.realWriteB(INT10.BIOSMEM_SEG, INT10.BIOSMEM_VIDEO_CTL,
                (byte) (0x60 | (clearmem ? 0 : 0x80)));
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
        CHAR.setActivePage((byte) 0);
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
        int crtc_base = DOSBox.Machine == DOSBox.MachineType.HERC ? (int) 0x3b4 : (int) 0x3d4;
        // Horizontal total
        IO.writeW(crtc_base, 0x00 | (CurMode.HTotal) << 8);
        // Horizontal displayed
        IO.writeW(crtc_base, 0x01 | (CurMode.HDispend) << 8);
        // Horizontal sync position
        IO.writeW(crtc_base, 0x02 | (CurMode.HDispend + 1) << 8);
        // Horizontal sync width, seems to be fixed to 0xa, for cga at least, hercules has 0xf
        IO.writeW(crtc_base, 0x03 | (0xa) << 8);
        //// Vertical total
        IO.writeW(crtc_base, 0x04 | (CurMode.VTotal) << 8);
        // Vertical total adjust, 6 for cga,hercules,tandy
        IO.writeW(crtc_base, 0x05 | (6) << 8);
        // Vertical displayed
        IO.writeW(crtc_base, 0x06 | (CurMode.VDispend) << 8);
        // Vertical sync position
        IO.writeW(crtc_base,
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
        IO.writeW(crtc_base, (int) (0x09 | (scanline - 1) << 8));
        // Setup the CGA palette using VGA DAC palette
        for (byte ct = 0; ct < 16; ct++)
            VGA.instance().Dac.setEntry(ct, cga_palette[ct][0], cga_palette[ct][1],
                    cga_palette[ct][2]);
        // Setup the tandy palette
        for (byte ct = 0; ct < 16; ct++)
            VGA.instance().Dac.combineColor(ct, ct);
        // Setup the special registers for each dosbox.machine type
        byte[] mode_control_list = {0x2c, 0x28, 0x2d, 0x29, // 0-3
                0x2a, 0x2e, 0x1e, 0x29, // 4-7
                0x2a, 0x2b, 0x3b // 8-a
        };
        byte[] mode_control_list_pcjr = {0x0c, 0x08, 0x0d, 0x09, // 0-3
                0x0a, 0x0e, 0x0e, 0x09, // 4-7
                0x1a, 0x1b, 0x0b // 8-a
        };
        byte mode_control, color_select;
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
                mode_control = mode_control_list[CurMode.Mode];
                if (CurMode.Mode == 0x6)
                    color_select = 0x3f;
                else
                    color_select = 0x30;
                IO.writeB(0x3d8, mode_control);
                IO.writeB(0x3d9, color_select);
                Memory.realWriteB(INT10.BIOSMEM_SEG, INT10.BIOSMEM_CURRENT_MSR, mode_control);
                Memory.realWriteB(INT10.BIOSMEM_SEG, INT10.BIOSMEM_CURRENT_PAL, color_select);
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
                mode_control = mode_control_list[CurMode.Mode];
                if (CurMode.Mode == 0x6 || CurMode.Mode == 0xa)
                    color_select = 0x3f;
                else
                    color_select = 0x30;
                IO.writeB(0x3d8, mode_control);
                IO.writeB(0x3d9, color_select);
                Memory.realWriteB(INT10.BIOSMEM_SEG, INT10.BIOSMEM_CURRENT_MSR, mode_control);
                Memory.realWriteB(INT10.BIOSMEM_SEG, INT10.BIOSMEM_CURRENT_PAL, color_select);
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

                mode_control = mode_control_list_pcjr[CurMode.Mode];
                IO.writeB(0x3da, 0x0);
                IO.writeB(0x3da, mode_control);
                Memory.realWriteB(INT10.BIOSMEM_SEG, INT10.BIOSMEM_CURRENT_MSR, mode_control);

                if (CurMode.Mode == 0x6 || CurMode.Mode == 0xa)
                    color_select = 0x3f;
                else
                    color_select = 0x30;
                IO.writeB(0x3d9, color_select);
                Memory.realWriteB(INT10.BIOSMEM_SEG, INT10.BIOSMEM_CURRENT_PAL, color_select);
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
                IO.writeW(crtc_base, i | (Memory.realReadB(Memory.realSeg(vparams),
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
        byte modeset_ctl = (byte) Memory.realReadB(INT10.BIOSMEM_SEG, INT10.BIOSMEM_MODESET_CTL);

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
                if ((modeset_ctl & 0x90) == 0x80) { // 200 lines emulation
                    if (CurMode.Mode <= 3) {
                        CurMode = ModeList_VGA_Text_200lines[CurMode.Mode];
                    }
                } else if ((modeset_ctl & 0x90) == 0x00) { // 350 lines emulation
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
        boolean mono_mode = (mode == 7) || (mode == 0xf);
        if (mono_mode)
            crtcBase = 0x3b4;
        else
            crtcBase = 0x3d4;

        if (DOSBox.isVGAArch() && (DOSBox.SVGACard == DOSBox.SVGACards.S3Trio)) {
            // Disable MMIO here so we can read / write memory
            IO.write(crtcBase, 0x53);
            IO.write((int) (crtcBase + 1), 0x0);
        }

        /* Setup MISC Output Register */
        byte misc_output = (byte) (0x2 | (mono_mode ? 0x0 : 0x1));

        if ((CurMode.Type == VGAModes.TEXT) && (CurMode.CWidth == 9)) {
            // 28MHz (16MHz EGA) clock for 9-pixel wide chars
            misc_output |= 0x4;
        }

        switch (CurMode.VDispend) {
            case 400:
                misc_output |= 0x60;
                break;
            case 480:
                misc_output |= 0xe0;
                break;
            case 350:
                misc_output |= 0xa0;
                break;
            default:
                misc_output |= 0x60;
                break;
        }
        IO.write(0x3c2, misc_output); // Setup for 3b4 or 3d4

        /* Program Sequencer */
        byte[] seq_data = new byte[SEQ_REGS];
        Arrays.fill(seq_data, 0, SEQ_REGS, (byte) 0);
        seq_data[1] |= 0x01; // 8 dot fonts by default
        if ((CurMode.Special & _EGA_HALF_CLOCK) != 0)
            seq_data[1] |= 0x08; // Check for half clock
        if ((DOSBox.Machine == DOSBox.MachineType.EGA) && (CurMode.Special & _EGA_HALF_CLOCK) != 0)
            seq_data[1] |= 0x02;
        seq_data[4] |= 0x02; // More than 64kb
        switch (CurMode.Type) {
            case TEXT:
                if (CurMode.CWidth == 9)
                    seq_data[1] &= (byte) ~1;
                seq_data[2] |= 0x3; // Enable plane 0 and 1
                seq_data[4] |= 0x01; // Alpanumeric
                if (DOSBox.isVGAArch())
                    seq_data[4] |= 0x04; // odd/even enabled
                break;
            case CGA2:
                seq_data[2] |= 0xf; // Enable plane 0
                if (DOSBox.Machine == DOSBox.MachineType.EGA)
                    seq_data[4] |= 0x04; // odd/even enabled
                break;
            case CGA4:
                if (DOSBox.Machine == DOSBox.MachineType.EGA)
                    seq_data[2] |= 0x03; // Enable plane 0 and 1
                break;
            case LIN4:
            case EGA:
                seq_data[2] |= 0xf; // Enable all planes for writing
                if (DOSBox.Machine == DOSBox.MachineType.EGA)
                    seq_data[4] |= 0x04; // odd/even enabled
                break;
            case LIN8: // Seems to have the same reg layout from testing
            case LIN15:
            case LIN16:
            case LIN32:
            case VGA:
                seq_data[2] |= 0xf; // Enable all planes for writing
                seq_data[4] |= 0xc; // Graphics - odd/even - Chained
                break;
        }
        for (byte ct = 0; ct < SEQ_REGS; ct++) {
            IO.write(0x3c4, ct);
            IO.write(0x3c5, seq_data[ct]);
        }
        // this may be changed by SVGA chipset emulation
        VGA.instance().Config.CompatibleChain4 = true;

        /* Program CRTC */
        /* First disable write protection */
        IO.write(crtcBase, 0x11);
        IO.write((int) (crtcBase + 1), IO.read((int) (crtcBase + 1)) & 0x7f);
        /* Clear all the regs */
        for (byte ct = 0x0; ct <= 0x18; ct++) {
            IO.write(crtcBase, ct);
            IO.write((int) (crtcBase + 1), 0);
        }
        byte overflow = 0;
        byte max_scanline = 0;
        byte ver_overflow = 0;
        byte hor_overflow = 0;
        /* Horizontal Total */
        IO.write(crtcBase, 0x00);
        IO.write((int) (crtcBase + 1), 0xff & (CurMode.HTotal - 5));
        hor_overflow |= (byte) (((CurMode.HTotal - 5) & 0x100) >>> 8);
        /* Horizontal Display End */
        IO.write(crtcBase, 0x01);
        IO.write((int) (crtcBase + 1), 0xff & (CurMode.HDispend - 1));
        hor_overflow |= (byte) (((CurMode.HDispend - 1) & 0x100) >>> 7);
        /* Start horizontal Blanking */
        IO.write(crtcBase, 0x02);
        IO.write((int) (crtcBase + 1), 0xff & CurMode.HDispend);
        hor_overflow |= (byte) (((CurMode.HDispend) & 0x100) >>> 6);
        /* End horizontal Blanking */
        int blank_end = (CurMode.HTotal - 2) & 0x7f;
        IO.write(crtcBase, 0x03);
        IO.write((int) (crtcBase + 1), 0xff & (0x80 | (blank_end & 0x1f)));

        /* Start Horizontal Retrace */
        int ret_start;
        if ((CurMode.Special & _EGA_HALF_CLOCK) != 0 && (CurMode.Type != VGAModes.CGA2))
            ret_start = (CurMode.HDispend + 3);
        else if (CurMode.Type == VGAModes.TEXT)
            ret_start = (CurMode.HDispend + 5);
        else
            ret_start = (CurMode.HDispend + 4);
        IO.write(crtcBase, 0x04);
        IO.write((int) (crtcBase + 1), 0xff & ret_start);
        hor_overflow |= (byte) ((ret_start & 0x100) >>> 4);

        /* End Horizontal Retrace */
        int ret_end;
        if ((CurMode.Special & _EGA_HALF_CLOCK) != 0) {
            if (CurMode.Type == VGAModes.CGA2)
                ret_end = 0; // mode 6
            else if ((CurMode.Special & _EGA_LINE_DOUBLE) != 0)
                ret_end = (CurMode.HTotal - 18) & 0x1f;
            else
                ret_end = ((CurMode.HTotal - 18) & 0x1f) | 0x20; // mode 0&1 have 1 char sync delay
        } else if (CurMode.Type == VGAModes.TEXT)
            ret_end = (CurMode.HTotal - 3) & 0x1f;
        else
            ret_end = (CurMode.HTotal - 4) & 0x1f;

        IO.write(crtcBase, 0x05);
        IO.write((int) (crtcBase + 1), 0xff & (ret_end | (blank_end & 0x20) << 2));

        /* Vertical Total */
        IO.write(crtcBase, 0x06);
        IO.write((int) (crtcBase + 1), 0xff & (CurMode.VTotal - 2));
        overflow |= (byte) (((CurMode.VTotal - 2) & 0x100) >>> 8);
        overflow |= (byte) (((CurMode.VTotal - 2) & 0x200) >>> 4);
        ver_overflow |= (byte) (((CurMode.VTotal - 2) & 0x400) >>> 10);

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
        IO.write((int) (crtcBase + 1), 0xff & vretrace);
        overflow |= (byte) ((vretrace & 0x100) >>> 6);
        overflow |= (byte) ((vretrace & 0x200) >>> 2);
        ver_overflow |= (byte) ((vretrace & 0x400) >>> 6);

        /* Vertical Retrace End */
        IO.write(crtcBase, 0x11);
        IO.write((int) (crtcBase + 1), 0xff & ((vretrace + 2) & 0xF));

        /* Vertical Display End */
        IO.write(crtcBase, 0x12);
        IO.write((int) (crtcBase + 1), 0xff & (CurMode.VDispend - 1));
        overflow |= (byte) (((CurMode.VDispend - 1) & 0x100) >>> 7);
        overflow |= (byte) (((CurMode.VDispend - 1) & 0x200) >>> 3);
        ver_overflow |= (byte) (((CurMode.VDispend - 1) & 0x400) >>> 9);

        int vblank_trim;
        if (DOSBox.isVGAArch()) {
            switch (CurMode.VDispend) {
                case 400:
                    vblank_trim = 6;
                    break;
                case 480:
                    vblank_trim = 7;
                    break;
                case 350:
                    vblank_trim = 5;
                    break;
                default:
                    vblank_trim = 8;
                    break;
            }
        } else {
            switch (CurMode.VDispend) {
                case 350:
                    vblank_trim = 0;
                    break;
                default:
                    vblank_trim = 23;
                    break;
            }
        }

        /* Vertical Blank Start */
        IO.write(crtcBase, 0x15);
        IO.write((int) (crtcBase + 1), 0xff & (CurMode.VDispend + vblank_trim));
        overflow |= (byte) (((CurMode.VDispend + vblank_trim) & 0x100) >>> 5);
        max_scanline |= (byte) (((CurMode.VDispend + vblank_trim) & 0x200) >>> 4);
        ver_overflow |= (byte) (((CurMode.VDispend + vblank_trim) & 0x400) >>> 8);

        /* Vertical Blank End */
        IO.write(crtcBase, 0x16);
        IO.write((int) (crtcBase + 1), 0xff & (CurMode.VTotal - vblank_trim - 2));

        /* Line Compare */
        int line_compare = (CurMode.VTotal < 1024) ? 1023 : 2047;
        IO.write(crtcBase, 0x18);
        IO.write((int) (crtcBase + 1), 0xff & (line_compare & 0xff));
        overflow |= (byte) ((line_compare & 0x100) >>> 4);
        max_scanline |= (byte) ((line_compare & 0x200) >>> 3);
        ver_overflow |= (byte) ((line_compare & 0x400) >>> 4);
        byte underline = 0;
        /* Maximum scanline / Underline Location */
        if ((CurMode.Special & _EGA_LINE_DOUBLE) != 0) {
            if (DOSBox.Machine != DOSBox.MachineType.EGA)
                max_scanline |= 0x80;
        }
        switch (CurMode.Type) {
            case TEXT:
                max_scanline |= (byte) (CurMode.CHeight - 1);
                // mode 7 uses a diff underline position
                underline = mono_mode ? (byte) 0x0f : (byte) 0x1f;
                break;
            case VGA:
                underline = 0x40;
                max_scanline |= 1; // Vga doesn't use double line but this
                break;
            case LIN8:
            case LIN15:
            case LIN16:
            case LIN32:
                underline = 0x60; // Seems to enable the every 4th clock on my s3
                break;
            case CGA2:
            case CGA4:
                max_scanline |= 1;
                break;
        }
        if (CurMode.VDispend == 350)
            underline = 0x0f;

        IO.write(crtcBase, 0x09);
        IO.write((int) (crtcBase + 1), max_scanline);
        IO.write(crtcBase, 0x14);
        IO.write((int) (crtcBase + 1), underline);

        /* OverFlow */
        IO.write(crtcBase, 0x07);
        IO.write((int) (crtcBase + 1), overflow);

        if (DOSBox.SVGACard == DOSBox.SVGACards.S3Trio) {
            /* Extended Horizontal Overflow */
            IO.write(crtcBase, 0x5d);
            IO.write((int) (crtcBase + 1), hor_overflow);
            /* Extended Vertical Overflow */
            IO.write(crtcBase, 0x5e);
            IO.write((int) (crtcBase + 1), ver_overflow);
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
        IO.write((int) (crtcBase + 1), offset & 0xff);

        if (DOSBox.SVGACard == DOSBox.SVGACards.S3Trio) {
            /* Extended System Control 2 Register */
            /* This register actually has more bits but only use the extended offset ones */
            IO.write(crtcBase, 0x51);
            IO.write((int) (crtcBase + 1), (offset & 0x300) >>> 4);
            /* Clear remaining bits of the display start */
            IO.write(crtcBase, 0x69);
            IO.write((int) (crtcBase + 1), 0);
            /* Extended Vertical Overflow */
            IO.write(crtcBase, 0x5e);
            IO.write((int) (crtcBase + 1), ver_overflow);
        }

        /* Mode Control */
        short mode_control = 0;

        switch (CurMode.Type) {
            case CGA2:
                mode_control = 0xc2; // 0x06 sets address wrap.
                break;
            case CGA4:
                mode_control = 0xa2;
                break;
            case LIN4:
            case EGA:
                if (CurMode.Mode == 0x11) // 0x11 also sets address wrap. thought maybe all 2 color
                                          // modes did but 0x0f doesn't.
                    mode_control = 0xc3; // so.. 0x11 or 0x0f a one off?
                else {
                    if (DOSBox.Machine == DOSBox.MachineType.EGA) {
                        if ((CurMode.Special & _EGA_LINE_DOUBLE) != 0)
                            mode_control = 0xc3;
                        else
                            mode_control = 0x8b;
                    } else {
                        mode_control = 0xe3;
                    }
                }
                break;
            case TEXT:
            case VGA:
            case LIN8:
            case LIN15:
            case LIN16:
            case LIN32:
                mode_control = 0xa3;
                if ((CurMode.Special & _VGA_PIXEL_DOUBLE) != 0)
                    mode_control |= 0x08;
                break;
        }

        IO.write(crtcBase, 0x17);
        IO.write((int) (crtcBase + 1), mode_control);
        /* Renable write protection */
        IO.write(crtcBase, 0x11);
        IO.write((int) (crtcBase + 1), 0xff & (IO.read((int) (crtcBase + 1)) | 0x80));

        if (DOSBox.SVGACard == DOSBox.SVGACards.S3Trio) {
            /* Setup the correct clock */
            if (CurMode.Mode >= 0x100) {
                misc_output |= 0xef; // Select clock 3
                int clock = CurMode.VTotal * 8 * CurMode.HTotal * 70;
                VGA.instance().setClock(3, clock / 1000);
            }
            short misc_control_2;
            /* Setup Pixel format */
            switch (CurMode.Type) {
                case LIN8:
                    misc_control_2 = 0x00;
                    break;
                case LIN15:
                    misc_control_2 = 0x30;
                    break;
                case LIN16:
                    misc_control_2 = 0x50;
                    break;
                case LIN32:
                    misc_control_2 = 0xd0;
                    break;
                default:
                    misc_control_2 = 0x0;
                    break;
            }
            IO.writeB(crtcBase, 0x67);
            IO.writeB((int) (crtcBase + 1), misc_control_2);
        }

        /* Write Misc Output */
        IO.write(0x3c2, misc_output);
        /* Program Graphics controller */
        byte[] gfx_data = new byte[GFX_REGS];
        Arrays.fill(gfx_data, 0, GFX_REGS, (byte) 0);
        gfx_data[0x7] = 0xf; /* Color don't care */
        gfx_data[0x8] = (byte) 0xff; /* BitMask */
        switch (CurMode.Type) {
            case TEXT:
                gfx_data[0x5] |= 0x10; // Odd-Even Mode
                gfx_data[0x6] |= mono_mode ? (byte) 0x0a : (byte) 0x0e; // Either b800 or b000
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
        byte[] att_data = new byte[ATT_REGS];
        Arrays.fill(att_data, 0, ATT_REGS, (byte) 0);
        att_data[0x12] = 0xf; // Always have all color planes enabled
        /* Program Attribute Controller */
        switch (CurMode.Type) {
            case EGA:
            case LIN4:
                att_data[0x10] = 0x01; // Color Graphics
                switch (CurMode.Mode) {
                    case 0x0f:
                        att_data[0x10] |= 0x0a; // Monochrome
                        att_data[0x01] = 0x08;
                        att_data[0x04] = 0x18;
                        att_data[0x05] = 0x18;
                        att_data[0x09] = 0x08;
                        att_data[0x0d] = 0x18;
                        break;
                    case 0x11:
                        for (i = 1; i < 16; i++)
                            att_data[i] = 0x3f;
                        break;
                    case 0x10:
                    case 0x12:
                        // goto att_text16;
                        attText16(att_data);
                        break;
                    default:
                        if (CurMode.Type == VGAModes.LIN4) {
                            // goto att_text16;
                            attText16(att_data);
                            break;
                        }
                        for (byte ct = 0; ct < 8; ct++) {
                            att_data[ct] = ct;
                            att_data[ct + 8] = (byte) (ct + 0x10);
                        }
                        break;
                }
                break;
            case TANDY16:
                att_data[0x10] = 0x01; // Color Graphics
                for (byte ct = 0; ct < 16; ct++)
                    att_data[ct] = ct;
                break;
            case TEXT:
                if (CurMode.CWidth == 9) {
                    att_data[0x13] = 0x08; // Pel panning on 8, although we don't have 9 dot text
                                           // mode
                    att_data[0x10] = 0x0C; // Color Text with blinking, 9 Bit characters
                } else {
                    att_data[0x13] = 0x00;
                    att_data[0x10] = 0x08; // Color Text with blinking, 8 Bit characters
                }
                Memory.realWriteB(INT10.BIOSMEM_SEG, INT10.BIOSMEM_CURRENT_PAL, 0x30);
                // att_text16:
                attText16(att_data);
                break;
            case CGA2:
                att_data[0x10] = 0x01; // Color Graphics
                att_data[0] = 0x0;
                for (i = 1; i < 0x10; i++)
                    att_data[i] = 0x17;
                att_data[0x12] = 0x1; // Only enable 1 plane
                Memory.realWriteB(INT10.BIOSMEM_SEG, INT10.BIOSMEM_CURRENT_PAL, 0x3f);
                break;
            case CGA4:
                att_data[0x10] = 0x01; // Color Graphics
                att_data[0] = 0x0;
                att_data[1] = 0x13;
                att_data[2] = 0x15;
                att_data[3] = 0x17;
                att_data[4] = 0x02;
                att_data[5] = 0x04;
                att_data[6] = 0x06;
                att_data[7] = 0x07;
                for (byte ct = 0x8; ct < 0x10; ct++)
                    att_data[ct] = (byte) (ct + 0x8);
                Memory.realWriteB(INT10.BIOSMEM_SEG, INT10.BIOSMEM_CURRENT_PAL, 0x30);
                break;
            case VGA:
            case LIN8:
            case LIN15:
            case LIN16:
            case LIN32:
                for (byte ct = 0; ct < 16; ct++)
                    att_data[ct] = ct;
                att_data[0x10] = 0x41; // Color Graphics 8-bit
                break;
        }
        IO.read(mono_mode ? 0x3ba : 0x3da);
        if ((modeset_ctl & 8) == 0) {
            for (byte ct = 0; ct < ATT_REGS; ct++) {
                IO.write(0x3c0, ct);
                IO.write(0x3c0, att_data[ct]);
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
                            IO.write(0x3c9, mtext_s3_palette[i][0]);
                            IO.write(0x3c9, mtext_s3_palette[i][1]);
                            IO.write(0x3c9, mtext_s3_palette[i][2]);
                        }
                    } else {
                        for (i = 0; i < 64; i++) {
                            IO.write(0x3c9, ega_palette[i][0]);
                            IO.write(0x3c9, ega_palette[i][1]);
                            IO.write(0x3c9, ega_palette[i][2]);
                        }
                    }
                    break;
                case CGA2:
                case CGA4:
                case TANDY16:
                    for (i = 0; i < 64; i++) {
                        IO.write(0x3c9, cga_palette_2[i][0]);
                        IO.write(0x3c9, cga_palette_2[i][1]);
                        IO.write(0x3c9, cga_palette_2[i][2]);
                    }
                    break;
                case TEXT:
                    if (CurMode.Mode == 7) {
                        if ((DOSBox.isVGAArch()) && (DOSBox.SVGACard == DOSBox.SVGACards.S3Trio)) {
                            for (i = 0; i < 64; i++) {
                                IO.write(0x3c9, mtext_s3_palette[i][0]);
                                IO.write(0x3c9, mtext_s3_palette[i][1]);
                                IO.write(0x3c9, mtext_s3_palette[i][2]);
                            }
                        } else {
                            for (i = 0; i < 64; i++) {
                                IO.write(0x3c9, mtext_palette[i][0]);
                                IO.write(0x3c9, mtext_palette[i][1]);
                                IO.write(0x3c9, mtext_palette[i][2]);
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
                        IO.write(0x3c9, vga_palette[i][0]);
                        IO.write(0x3c9, vga_palette[i][1]);
                        IO.write(0x3c9, vga_palette[i][2]);
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
                IO.write(0x3c0, att_data[ct]);
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
            IO.write((int) (crtcBase + 1), 0);
            /* Setup the linear frame buffer */
            IO.write(crtcBase, 0x59);
            IO.write((int) (crtcBase + 1), (INT10.S3_LFB_BASE >>> 24) & 0xff);
            IO.write(crtcBase, 0x5a);
            IO.write((int) (crtcBase + 1), (INT10.S3_LFB_BASE >>> 16) & 0xff);
            IO.write(crtcBase, 0x6b); // BIOS scratchpad
            IO.write((int) (crtcBase + 1), (INT10.S3_LFB_BASE >>> 24) & 0xff);

            /* Setup some remaining S3 registers */
            IO.write(crtcBase, 0x41); // BIOS scratchpad
            IO.write((int) (crtcBase + 1), 0x88);
            IO.write(crtcBase, 0x52); // extended BIOS scratchpad
            IO.write((int) (crtcBase + 1), 0x80);

            IO.write(0x3c4, 0x15);
            IO.write(0x3c5, 0x03);

            // Accellerator setup
            int reg_50 = VGA.S3_XGA_8BPP;
            switch (CurMode.Type) {
                case LIN15:
                case LIN16:
                    reg_50 |= VGA.S3_XGA_16BPP;
                    break;
                case LIN32:
                    reg_50 |= VGA.S3_XGA_32BPP;
                    break;
                default:
                    break;
            }
            switch (CurMode.SWidth) {
                case 640:
                    reg_50 |= VGA.S3_XGA_640;
                    break;
                case 800:
                    reg_50 |= VGA.S3_XGA_800;
                    break;
                case 1024:
                    reg_50 |= VGA.S3_XGA_1024;
                    break;
                case 1152:
                    reg_50 |= VGA.S3_XGA_1152;
                    break;
                case 1280:
                    reg_50 |= VGA.S3_XGA_1280;
                    break;
                default:
                    break;
            }
            IO.writeB(crtcBase, 0x50);
            IO.writeB((int) (crtcBase + 1), reg_50);

            byte reg_31, reg_3a;
            switch (CurMode.Type) {
                case LIN15:
                case LIN16:
                case LIN32:
                    reg_3a = 0x15;
                    break;
                case LIN8:
                    // S3VBE20 does it this way. The other double pixel bit does not
                    // seem to have an effect on the Trio64.
                    if ((CurMode.Special & _VGA_PIXEL_DOUBLE) != 0)
                        reg_3a = 0x5;
                    else
                        reg_3a = 0x15;
                    break;
                default:
                    reg_3a = 5;
                    break;
            }

            switch (CurMode.Type) {
                case LIN4: // <- Theres a discrepance with real hardware on this
                case LIN8:
                case LIN15:
                case LIN16:
                case LIN32:
                    reg_31 = 9;
                    break;
                default:
                    reg_31 = 5;
                    break;
            }
            IO.write(crtcBase, 0x3a);
            IO.write((int) (crtcBase + 1), reg_3a);
            IO.write(crtcBase, 0x31);
            IO.write((int) (crtcBase + 1), reg_31); // Enable banked memory and 256k+ access
            IO.write(crtcBase, 0x58);
            IO.write((int) (crtcBase + 1), 0x3); // Enable 8 mb of linear addressing

            IO.write(crtcBase, 0x38);
            IO.write((int) (crtcBase + 1), 0x48); // Register lock 1
            IO.write(crtcBase, 0x39);
            IO.write((int) (crtcBase + 1), 0xa5); // Register lock 2
        } else if (VGA.instance().SVGADrv.SetVideoMode != null) {
            ModeExtraData modeData = new ModeExtraData();
            modeData.VOverflow = ver_overflow;
            modeData.HOverflow = hor_overflow;
            modeData.Offset = offset;
            modeData.ModeNo = CurMode.Mode;
            modeData.HTotal = CurMode.HTotal;
            modeData.VTotal = CurMode.VTotal;
            VGA.instance().SVGADrv.SetVideoMode.exec(crtcBase, modeData);
        }

        finishSetMode(clearmem);

        /* Set vga attrib register into defined state */
        IO.read(mono_mode ? 0x3ba : 0x3da);
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
            IO.write(0x3c9, text_palette[i][0]);
            IO.write(0x3c9, text_palette[i][1]);
            IO.write(0x3c9, text_palette[i][2]);
        }
    }

    // att_text16
    private static void attText16(byte[] att_data) {
        if (CurMode.Mode == 7) {
            att_data[0] = 0x00;
            att_data[8] = 0x10;
            for (int i = 1; i < 8; i++) {
                att_data[i] = 0x08;
                att_data[i + 8] = 0x18;
            }
        } else {
            for (byte ct = 0; ct < 8; ct++) {
                att_data[ct] = ct;
                att_data[ct + 8] = (byte) (ct + 0x38);
            }
            if (DOSBox.isVGAArch())
                att_data[0x06] = 0x14; // Odd Color 6 yellow/brown.
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
