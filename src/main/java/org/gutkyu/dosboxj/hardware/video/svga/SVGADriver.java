package org.gutkyu.dosboxj.hardware.video.svga;

import org.gutkyu.dosboxj.DOSBox;
import org.gutkyu.dosboxj.hardware.video.VGA;

/* Support for modular SVGA implementation */
/*
 * Video mode extra data to be passed to FinishSetMode_SVGA(). This structure will be in flux until
 * all drivers (including S3) are properly separated. Right now it contains only three overflow
 * fields in S3 format and relies on drivers re-interpreting those. For reference:
 * ver_overflow:X|line_comp10|X|vretrace10|X|vbstart10|vdispend10|vtotal10
 * hor_overflow:X|X|X|hretrace8|X|hblank8|hdispend8|htotal8 offset is not currently used by drivers
 * (useful only for S3 itself) It also contains basic int10 mode data - number, vtotal, htotal
 */


// Vector function prototypes


public final class SVGADriver {
    public FuncWritePort WriteP3D5;
    public FuncReadPort ReadP3D5;
    public FuncWritePort WriteP3C5;
    public FuncReadPort ReadP3C5;
    public FuncWritePort WriteP3C0;
    public FuncReadPort ReadP3C1;
    public FuncWritePort WriteP3CF;
    public FuncReadPort ReadP3CF;

    public FuncFinishSetMode SetVideoMode;
    public FuncDetermineMode DetermineMode;
    public FuncSetClock SetClock;
    public FuncGetClock GetClock;
    public FuncHWCursorActive HardwareCursorActive;
    public FuncAcceptsMode AcceptsMode;

    public void clear() {
        WriteP3D5 = null;
        ReadP3D5 = null;
        WriteP3C5 = null;
        ReadP3C5 = null;
        WriteP3C0 = null;
        ReadP3C1 = null;
        WriteP3CF = null;
        ReadP3CF = null;

        SetVideoMode = null;
        DetermineMode = null;
        SetClock = null;
        GetClock = null;
        HardwareCursorActive = null;
        AcceptsMode = null;
    }

    public void setup(VGA vga) {
        switch (DOSBox.SVGACard) {
            case S3Trio:
                new S3TrioSVGADriverProvider(vga);
                break;
            case TsengET4K:
                new TsengET4KSVGADriverProvider(vga);
                break;
            case TsengET3K:
                new TsengET3KSVGADriverProvider(vga);
                break;
            case ParadisePVGA1A:
                new ParadiseSVGADriverProvider(vga);
                break;
            default:
                vga.VMemSize = vga.VMemWrap = 256 * 1024;
                break;
        }
    }

}
