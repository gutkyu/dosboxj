package org.gutkyu.dosboxj.hardware.video;

import org.gutkyu.dosboxj.util.*;
import org.gutkyu.dosboxj.DOSBox;
import org.gutkyu.dosboxj.hardware.io.IO;
import org.gutkyu.dosboxj.hardware.io.iohandler.ReadHandler;
import org.gutkyu.dosboxj.hardware.io.iohandler.WriteHandler;
import org.gutkyu.dosboxj.cpu.*;
import org.gutkyu.dosboxj.misc.*;

public final class VGAXGA {

    // private ContextBoundObject XGA_SHOW_COMMAND_TRACE = null;

    public final class ScissorReg {
        public int x1, y1, x2, y2;// uint16
    }
    public final class XGAWaitCmd {
        public boolean NewLine;
        public boolean Wait;
        public short Cmd;
        public int CurX, CurY;// uint16
        public int X1, Y1, X2, Y2, SizeX, SizeY;// uint16
        public int Data; /* transient data passed by multiple calls */
        public int DataSize;
        public int BusWidth;
    }

    // -- #region XGAStatus
    public ScissorReg Scissors = null;

    public int ReadMask;
    public int WriteMask;

    public int Forecolor;
    public int Backcolor;

    public int CurCommand;

    public int Foremix;// uint16
    public int Backmix;// uint16

    public int CurX, CurY;// uint16
    public int DestX, DestY;// uint16

    public int ErrTerm;// uint16
    public int MIPcount;// uint16
    public int MAPcount;// uint16

    public int PixCntl;// uint16
    public int Control1;// uint16
    public int Control2;// uint16
    public int ReadSel;// uint16

    public XGAWaitCmd WaitCmd = null;
    // -- #endregion

    private VGA _vga;

    public VGAXGA(VGA vga) {
        _vga = vga;

        Scissors = new ScissorReg();
        Scissors.x1 = 0;
        Scissors.x2 = 0;
        Scissors.y1 = 0;
        Scissors.y2 = 0;
        ReadMask = 0;
        WriteMask = 0;

        Forecolor = 0;
        Backcolor = 0;

        CurCommand = 0;

        Foremix = 0;
        Backmix = 0;

        CurX = 0;
        CurY = 0;
        DestX = 0;
        DestY = 0;

        ErrTerm = 0;
        MIPcount = 0;
        MAPcount = 0;

        PixCntl = 0;
        Control1 = 0;
        Control2 = 0;
        ReadSel = 0;
        WaitCmd = new XGAWaitCmd();
        WaitCmd.BusWidth = 0;
        WaitCmd.Cmd = 0;
        WaitCmd.CurX = 0;
        WaitCmd.CurY = 0;
        WaitCmd.Data = 0;
        WaitCmd.DataSize = 0;
        WaitCmd.NewLine = false;
        WaitCmd.SizeX = 0;
        WaitCmd.SizeY = 0;
        WaitCmd.Wait = false;
        WaitCmd.X1 = 0;
        WaitCmd.X2 = 0;
        WaitCmd.Y1 = 0;
        WaitCmd.Y2 = 0;
    }

    private void writeMultiFunc(int val, int len) {
        int regselect = val >>> 12;
        int dataval = val & 0xfff;
        switch (regselect) {
            case 0: // minor axis pixel count
                MIPcount = dataval;
                break;
            case 1: // top scissors
                Scissors.y1 = dataval;
                break;
            case 2: // left
                Scissors.x1 = dataval;
                break;
            case 3: // bottom
                Scissors.y2 = dataval;
                break;
            case 4: // right
                Scissors.x2 = dataval;
                break;
            case 0xa: // data manip control
                PixCntl = dataval;
                break;
            case 0xd: // misc 2
                Control2 = dataval;
                break;
            case 0xe:
                Control1 = dataval;
                break;
            case 0xf:
                ReadSel = dataval;
                break;
            default:
                Log.logMsg("XGA: Unhandled multifunction command %x", regselect);
                break;
        }
    }

    private int readMultifunc() {
        switch (ReadSel++) {
            case 0:
                return MIPcount;
            case 1:
                return Scissors.y1;
            case 2:
                return Scissors.x1;
            case 3:
                return Scissors.y2;
            case 4:
                return Scissors.x2;
            case 5:
                return PixCntl;
            case 6:
                return Control1;
            case 7:
                return 0; // TODO
            case 8:
                return 0; // TODO
            case 9:
                return 0; // TODO
            case 10:
                return Control2;
            default:
                return 0;
        }
    }


    private void drawPoint(int x, int y, int c) {
        if ((CurCommand & 0x1) == 0)
            return;
        if ((CurCommand & 0x10) == 0)
            return;

        if (x < Scissors.x1)
            return;
        if (x > Scissors.x2)
            return;
        if (y < Scissors.y1)
            return;
        if (y > Scissors.y2)
            return;

        byte[] linearAlloc = _vga.Mem.LinearAlloc;
        int memaddr = (y * _vga.S3.XGAScreenWidth) + x;
        /*
         * Need to zero out all unused bits in modes that have any (15-bit or "32"-bit -- the last
         * one is actually 24-bit. Without this step there may be some graphics corruption (mainly,
         * during windows dragging.
         */
        switch (_vga.S3.XGAColorMode) {
            case LIN8:
                if (memaddr >= _vga.VMemSize)
                    break;
                linearAlloc[memaddr] = (byte) c;
                break;
            case LIN15:
                if (memaddr * 2 >= _vga.VMemSize)
                    break;
                // ((Bit16u*)(vga.mem.linear))[memaddr] = (Bit16u)(c&0x7fff);
                int idx15 = _vga.Mem.LinearBase + memaddr * 2;
                ByteConv.setShort(linearAlloc, idx15, c & 0x7fff);
                break;
            case LIN16:
                if (memaddr * 2 >= _vga.VMemSize)
                    break;
                // ((Bit16u*)(vga.mem.linear))[memaddr] = (Bit16u)(c&0xffff);
                int idx16 = _vga.Mem.LinearBase + memaddr * 2;
                ByteConv.setShort(linearAlloc, idx16, c & 0xffff);
                break;
            case LIN32:
                if (memaddr * 4 >= _vga.VMemSize)
                    break;
                // ((Bit32u*)(vga.mem.linear))[memaddr] = c;
                int idx32 = _vga.Mem.LinearBase + memaddr * 4;
                ByteConv.setInt(linearAlloc, idx32, c);
                break;
            default:
                break;
        }

    }

    private int getPoint(int x, int y) {
        int memAddr = (y * _vga.S3.XGAScreenWidth) + x;


        byte[] linearAlloc = _vga.Mem.LinearAlloc;
        switch (_vga.S3.XGAColorMode) {
            case LIN8:
                if (memAddr >= _vga.VMemSize)
                    break;
                return linearAlloc[memAddr];
            case LIN15:
            case LIN16:
                if (memAddr * 2 >= _vga.VMemSize)
                    break;
                // return ((Bit16u*)(vga.mem.linear))[memaddr];
                int idx2 = _vga.Mem.LinearBase + memAddr * 2;
                return ByteConv.getShort(linearAlloc, idx2);
            case LIN32:
                if (memAddr * 4 >= _vga.VMemSize)
                    break;
                // return ((Bit32u*)(vga.mem.linear))[memaddr];
                int idx4 = _vga.Mem.LinearBase + memAddr * 4;
                return ByteConv.getInt(linearAlloc, idx4);
            default:
                break;
        }
        return 0;
    }


    private int getMixResult(int mixMode, int srcVal, int dstData) {
        int destVal = 0;
        switch (mixMode & 0xf) {
            case 0x00: /* not DST */
                destVal = ~dstData;
                break;
            case 0x01: /* 0 (false) */
                destVal = 0;
                break;
            case 0x02: /* 1 (true) */
                destVal = 0xffffffff;
                break;
            case 0x03: /* 2 DST */
                destVal = dstData;
                break;
            case 0x04: /* not SRC */
                destVal = ~srcVal;
                break;
            case 0x05: /* SRC xor DST */
                destVal = srcVal ^ dstData;
                break;
            case 0x06: /* not (SRC xor DST) */
                destVal = ~(srcVal ^ dstData);
                break;
            case 0x07: /* SRC */
                destVal = srcVal;
                break;
            case 0x08: /* not (SRC and DST) */
                destVal = ~(srcVal & dstData);
                break;
            case 0x09: /* (not SRC) or DST */
                destVal = (~srcVal) | dstData;
                break;
            case 0x0a: /* SRC or (not DST) */
                destVal = srcVal | (~dstData);
                break;
            case 0x0b: /* SRC or DST */
                destVal = srcVal | dstData;
                break;
            case 0x0c: /* SRC and DST */
                destVal = srcVal & dstData;
                break;
            case 0x0d: /* SRC and (not DST) */
                destVal = srcVal & (~dstData);
                break;
            case 0x0e: /* (not SRC) and DST */
                destVal = (~srcVal) & dstData;
                break;
            case 0x0f: /* not (SRC or DST) */
                destVal = ~(srcVal | dstData);
                break;
            default:
                Log.logMsg("XGA: GetMixResult: Unknown mix.  Shouldn't be able to get here!");
                break;
        }
        return destVal;
    }

    private void drawLineVector(int val) {
        int xat, yat;
        int srcVal = 0;
        int destVal;
        int dstData;
        int i;

        int dx, sx, sy;

        dx = MAPcount;
        xat = CurX;
        yat = CurY;

        switch ((val >>> 5) & 0x7) {
            case 0x00: /* 0 degrees */
                sx = 1;
                sy = 0;
                break;
            case 0x01: /* 45 degrees */
                sx = 1;
                sy = -1;
                break;
            case 0x02: /* 90 degrees */
                sx = 0;
                sy = -1;
                break;
            case 0x03: /* 135 degrees */
                sx = -1;
                sy = -1;
                break;
            case 0x04: /* 180 degrees */
                sx = -1;
                sy = 0;
                break;
            case 0x05: /* 225 degrees */
                sx = -1;
                sy = 1;
                break;
            case 0x06: /* 270 degrees */
                sx = 0;
                sy = 1;
                break;
            case 0x07: /* 315 degrees */
                sx = 1;
                sy = 1;
                break;
            default: // Should never get here
                sx = 0;
                sy = 0;
                break;
        }

        for (i = 0; i <= dx; i++) {
            int mixMode = (PixCntl >>> 6) & 0x3;
            switch (mixMode) {
                case 0x00: /* FOREMIX always used */
                    mixMode = Foremix;
                    switch ((mixMode >>> 5) & 0x03) {
                        case 0x00: /* Src is background color */
                            srcVal = Backcolor;
                            break;
                        case 0x01: /* Src is foreground color */
                            srcVal = Forecolor;
                            break;
                        case 0x02: /* Src is pixel data from PIX_TRANS register */
                            // srcval = tmpval;
                            // Log.LOG_MSG("XGA: DrawRect: Wants data from PIX_TRANS register");
                            break;
                        case 0x03: /* Src is bitmap data */
                            Log.logMsg("XGA: DrawRect: Wants data from srcdata");
                            // srcval = srcdata;
                            break;
                        default:
                            Log.logMsg("XGA: DrawRect: Shouldn't be able to get here!");
                            break;
                    }
                    dstData = getPoint(xat, yat);

                    destVal = getMixResult(mixMode, srcVal, dstData);

                    drawPoint(xat, yat, destVal);
                    break;
                default:
                    Log.logMsg("XGA: DrawLine: Needs mixmode %x", mixMode);
                    break;
            }
            xat += sx;
            yat += sy;
        }

        CurX = 0xffff & (xat - 1);
        CurY = 0xffff & yat;
    }

    private void drawLineBresenham(int val) {
        int xat = 0, yat = 0;
        int srcVal = 0;
        int destVal = 0;
        int dstData = 0;
        int i = 0;

        boolean steep = false;


        int dx = 0, sx = 0, dy = 0, sy = 0, e = 0, dmajor = 0, dminor = 0, destxtmp = 0;

        // Probably a lot easier way to do this, but this works.

        dminor = DestY;
        if ((DestY & 0x2000) != 0)
            dminor |= 0xffffe000;
        dminor >>>= 1;

        destxtmp = DestX;
        if ((DestX & 0x2000) != 0)
            destxtmp |= 0xffffe000;


        dmajor = -(destxtmp - (dminor << 1)) >>> 1;

        dx = dmajor;
        if (((val >>> 5) & 0x1) != 0) {
            sx = 1;
        } else {
            sx = -1;
        }
        dy = dminor;
        if (((val >>> 7) & 0x1) != 0) {
            sy = 1;
        } else {
            sy = -1;
        }
        e = 0xffff & ErrTerm;
        if ((ErrTerm & 0x2000) != 0)
            e |= 0xffffe000;
        xat = CurX;
        yat = CurY;

        if (((val >>> 6) & 0x1) != 0) {
            steep = false;
            // swap
            int tmpswap;
            tmpswap = xat;
            xat = yat;
            yat = tmpswap;
            tmpswap = sx;
            sx = sy;
            sy = tmpswap;
        } else {
            steep = true;
        }

        // Log.LOG_MSG("XGA: Bresenham: ASC %d, LPDSC %d, sx %d, sy %d, err %d, steep %d, length %d,
        // dmajor %d, dminor %d, xstart %d, ystart %d", dx, dy, sx, sy, e, steep, xga.MAPcount,
        // dmajor, dminor,xat,yat);

        for (i = 0; i <= MAPcount; i++) {
            int mixMode = (PixCntl >>> 6) & 0x3;
            switch (mixMode) {
                case 0x00: /* FOREMIX always used */
                    mixMode = Foremix;
                    switch ((mixMode >>> 5) & 0x03) {
                        case 0x00: /* Src is background color */
                            srcVal = Backcolor;
                            break;
                        case 0x01: /* Src is foreground color */
                            srcVal = Forecolor;
                            break;
                        case 0x02: /* Src is pixel data from PIX_TRANS register */
                            // srcval = tmpval;
                            Log.logMsg("XGA: DrawRect: Wants data from PIX_TRANS register");
                            break;
                        case 0x03: /* Src is bitmap data */
                            Log.logMsg("XGA: DrawRect: Wants data from srcdata");
                            // srcval = srcdata;
                            break;
                        default:
                            Log.logMsg("XGA: DrawRect: Shouldn't be able to get here!");
                            break;
                    }

                    if (steep) {
                        dstData = getPoint(xat, yat);
                    } else {
                        dstData = getPoint(yat, xat);
                    }

                    destVal = getMixResult(mixMode, srcVal, dstData);

                    if (steep) {
                        drawPoint(xat, yat, destVal);
                    } else {
                        drawPoint(yat, xat, destVal);
                    }

                    break;
                default:
                    Log.logMsg("XGA: DrawLine: Needs mixmode %x", mixMode);
                    break;
            }
            while (e > 0) {
                yat += sy;
                e -= (dx << 1);
            }
            xat += sx;
            e += (dy << 1);
        }

        if (steep) {
            CurX = 0xffff & xat;
            CurY = 0xffff & yat;
        } else {
            CurX = 0xffff & yat;
            CurY = 0xffff & xat;
        }
        // }
        // }

    }

    private void drawRectangle(int val) {
        int xat = 0, yat = 0;
        int srcVal = 0;
        int destVal = 0;
        int dstData = 0;

        int srcX = 0, srcY = 0, dx = 0, dy = 0;

        dx = -1;
        dy = -1;

        if (((val >>> 5) & 0x01) != 0)
            dx = 1;
        if (((val >>> 7) & 0x01) != 0)
            dy = 1;

        srcY = CurY;

        for (yat = 0; yat <= MIPcount; yat++) {
            srcX = CurX;
            for (xat = 0; xat <= MAPcount; xat++) {
                int mixmode = (PixCntl >>> 6) & 0x3;
                switch (mixmode) {
                    case 0x00: /* FOREMIX always used */
                        mixmode = Foremix;
                        switch ((mixmode >>> 5) & 0x03) {
                            case 0x00: /* Src is background color */
                                srcVal = Backcolor;
                                break;
                            case 0x01: /* Src is foreground color */
                                srcVal = Forecolor;
                                break;
                            case 0x02: /* Src is pixel data from PIX_TRANS register */
                                // srcval = tmpval;
                                Log.logMsg("XGA: DrawRect: Wants data from PIX_TRANS register");
                                break;
                            case 0x03: /* Src is bitmap data */
                                Log.logMsg("XGA: DrawRect: Wants data from srcdata");
                                // srcval = srcdata;
                                break;
                            default:
                                Log.logMsg("XGA: DrawRect: Shouldn't be able to get here!");
                                break;
                        }
                        dstData = getPoint(srcX, srcY);

                        destVal = getMixResult(mixmode, srcVal, dstData);

                        drawPoint(srcX, srcY, destVal);
                        break;
                    default:
                        Log.logMsg("XGA: DrawRect: Needs mixmode %x", mixmode);
                        break;
                }
                srcX += dx;
            }
            srcY += dy;
        }
        CurX = 0xffff & srcX;
        CurY = 0xffff & srcY;

        // Log.LOG_MSG("XGA: Draw rect (%d, %d)-(%d, %d), %d", x1, y1, x2, y2, xga.forecolor);
    }

    private boolean checkX() {
        boolean newline = false;
        XGAWaitCmd xgaWaitCmd = WaitCmd;
        if (!xgaWaitCmd.NewLine) {

            if ((xgaWaitCmd.CurX < 2048) && xgaWaitCmd.CurX > (xgaWaitCmd.X2)) {
                xgaWaitCmd.CurX = xgaWaitCmd.X1;
                xgaWaitCmd.CurY++;
                xgaWaitCmd.CurY &= 0x0fff;
                newline = true;
                xgaWaitCmd.NewLine = true;
                if ((xgaWaitCmd.CurY < 2048) && (xgaWaitCmd.CurY > xgaWaitCmd.Y2))
                    xgaWaitCmd.Wait = false;
            } else if (xgaWaitCmd.CurX >= 2048) {
                int realX = 4096 - xgaWaitCmd.CurX;
                if (xgaWaitCmd.X2 > 2047) { // x end is negative too
                    int realxend = 4096 - xgaWaitCmd.X2;
                    if (realX == realxend) {
                        xgaWaitCmd.CurX = xgaWaitCmd.X1;
                        xgaWaitCmd.CurY++;
                        xgaWaitCmd.CurY &= 0x0fff;
                        newline = true;
                        xgaWaitCmd.NewLine = true;
                        if ((xgaWaitCmd.CurY < 2048) && (xgaWaitCmd.CurY > xgaWaitCmd.Y2))
                            xgaWaitCmd.Wait = false;
                    }
                } else { // else overlapping
                    if (realX == xgaWaitCmd.X2) {
                        xgaWaitCmd.CurX = xgaWaitCmd.X1;
                        xgaWaitCmd.CurY++;
                        xgaWaitCmd.CurY &= 0x0fff;
                        newline = true;
                        xgaWaitCmd.NewLine = true;
                        if ((xgaWaitCmd.CurY < 2048) && (xgaWaitCmd.CurY > xgaWaitCmd.Y2))
                            xgaWaitCmd.Wait = false;
                    }
                }
            }
        } else {
            xgaWaitCmd.NewLine = false;
        }
        return newline;
    }

    private void drawWaitSub(int mixMode, int srcVal) {
        int destVal;
        int dstData;
        dstData = getPoint(WaitCmd.CurX, WaitCmd.CurY);
        destVal = getMixResult(mixMode, srcVal, dstData);
        // Log.LOG_MSG("XGA: DrawPattern: Mixmode: %x srcval: %x", mixmode, srcval);

        drawPoint(WaitCmd.CurX, WaitCmd.CurY, destVal);
        WaitCmd.CurX++;
        WaitCmd.CurX &= 0x0fff;
        checkX();
    }

    private static final int BUS_WITDH_LIN8_8 = VGAModes.LIN8.toValue(); // 8 bit
    private static final int BUS_WITDH_LIN8_16 = 0x20 | VGAModes.LIN8.toValue(); // 16 bit
    private static final int BUS_WITDH_LIN8_32 = 0x40 | (0xffff & VGAModes.LIN8.toValue()); // 32
                                                                                            // bit
    private static final int BUS_WITDH_LIN32_16 = 0x20 | (0xffff & VGAModes.LIN32.toValue());
    private static final int BUS_WITDH_LIN32_32 = 0x40 | (0xffff & VGAModes.LIN32.toValue()); // 32
                                                                                              // bit
    private static final int BUS_WITDH_LIN15_16 = 0x20 | (0xffff & VGAModes.LIN15.toValue()); // 16
                                                                                              // bit
    private static final int BUS_WITDH_LIN16_16 = 0x20 | (0xffff & VGAModes.LIN16.toValue()); // 16
                                                                                              // bit
    private static final int BUS_WITDH_LIN15_32 = 0x40 | (0xffff & VGAModes.LIN15.toValue()); // 32
                                                                                              // bit
    private static final int BUS_WITDH_LIN16_32 = 0x40 | (0xffff & VGAModes.LIN16.toValue()); // 32
                                                                                              // bit

    private void drawWait(int val, int len) {
        XGAWaitCmd xgaWaitCmd = WaitCmd;
        if (!xgaWaitCmd.Wait)
            return;
        int mixmode = (PixCntl >>> 6) & 0x3;
        int srcval;
        switch (xgaWaitCmd.Cmd) {
            case 2: /* Rectangle */
                switch (mixmode) {
                    case 0x00: /* FOREMIX always used */
                        mixmode = Foremix;

                        /*
                         * switch((mixmode >>>5) & 0x03) { case 0x00: // Src is background color
                         * srcval = xga.backcolor; break; case 0x01: // Src is foreground color
                         * srcval = xga.forecolor; break; case 0x02: // Src is pixel data from
                         * PIX_TRANS register
                         */
                        if (((mixmode >>> 5) & 0x03) != 0x2) {
                            // those cases don't seem to occur
                            Log.logMsg("XGA: unsupported drawwait operation");
                            break;
                        }
                        switch (xgaWaitCmd.BusWidth) {
                            // case VGAModes.LIN8: // 8 bit
                            case 5: // 8 bit
                                drawWaitSub(mixmode, val);
                                break;
                            // case 0x20 | (short)VGAModes.LIN8: // 16 bit
                            case 0x20 | 5: // 16 bit
                                for (int i = 0; i < len; i++) {
                                    drawWaitSub(mixmode, (val >>> (8 * i)) & 0xff);
                                    if (xgaWaitCmd.NewLine)
                                        break;
                                }
                                break;
                            // case 0x40 | (short)VGAModes.LIN8: // 32 bit
                            case 0x40 | 5: // 32 bit
                                for (int i = 0; i < 4; i++)
                                    drawWaitSub(mixmode, (val >>> (8 * i)) & 0xff);
                                break;
                            // case 0x20 | (short)VGAModes.LIN32:
                            case 0x20 | 8:
                                if (len != 4) { // Win 3.11 864 'hack?'
                                    if (xgaWaitCmd.DataSize == 0) {
                                        // set it up to wait for the next word
                                        xgaWaitCmd.Data = val;
                                        xgaWaitCmd.DataSize = 2;
                                        return;
                                    } else {
                                        srcval = (val << 16) | xgaWaitCmd.Data;
                                        xgaWaitCmd.Data = 0;
                                        xgaWaitCmd.DataSize = 0;
                                        drawWaitSub(mixmode, srcval);
                                    }
                                    break;
                                } // fall-through
                                  // goto Goto0x40;
                                  // case 0x40 | (short)VGAModes.LIN32.toValue(): // 32 bit
                            case 0x40 | 8: // 32 bit
                                // Goto0x40:
                                drawWaitSub(mixmode, val);
                                break;
                            // case 0x20 | (short)VGAModes.LIN15: // 16 bit
                            case 0x20 | 6: // 16 bit
                                // case 0x20 | (short)VGAModes.LIN16: // 16 bit
                            case 0x20 | 7: // 16 bit
                                drawWaitSub(mixmode, val);
                                break;
                            // case 0x40 | (short)VGAModes.LIN15: // 32 bit
                            case 0x40 | 6: // 32 bit
                                // case 0x40 | (short)VGAModes.LIN16: // 32 bit
                            case 0x40 | 7: // 32 bit
                                drawWaitSub(mixmode, val & 0xffff);
                                if (!xgaWaitCmd.NewLine)
                                    drawWaitSub(mixmode, val >>> 16);
                                break;
                            default:
                                // Let's hope they never show up ;)
                                Log.logMsg("XGA: unsupported bpp / datawidth combination %x",
                                        xgaWaitCmd.BusWidth);
                                break;
                        }
                        break;

                    case 0x02: // Data from PIX_TRANS selects the mix
                        int chunkSize = 0;
                        int chunks = 0;
                        switch (xgaWaitCmd.BusWidth & 0x60) {
                            case 0x0:
                                chunkSize = 8;
                                chunks = 1;
                                break;
                            case 0x20: // 16 bit
                                chunkSize = 16;
                                if (len == 4)
                                    chunks = 2;
                                else
                                    chunks = 1;
                                break;
                            case 0x40: // 32 bit
                                chunkSize = 16;
                                if (len == 4)
                                    chunks = 2;
                                else
                                    chunks = 1;
                                break;
                            case 0x60: // undocumented guess (but works)
                                chunkSize = 8;
                                chunks = 4;
                                break;
                        }

                        for (int k = 0; k < chunks; k++) { // chunks counter
                            xgaWaitCmd.NewLine = false;
                            for (int n = 0; n < chunkSize; n++) { // pixels
                                int mixmode1 = 0;

                                // This formula can rule the world ;)
                                int mask =
                                        1 << ((((n & 0xF8) + (8 - (n & 0x7))) - 1) + chunkSize * k);
                                if ((val & mask) != 0)
                                    mixmode1 = Foremix;
                                else
                                    mixmode1 = Backmix;

                                switch ((mixmode1 >>> 5) & 0x03) {
                                    case 0x00: // Src is background color
                                        srcval = Backcolor;
                                        break;
                                    case 0x01: // Src is foreground color
                                        srcval = Forecolor;
                                        break;
                                    default:
                                        Log.logMsg("XGA: DrawBlitWait: Unsupported src %x",
                                                (mixmode1 >>> 5) & 0x03);
                                        srcval = 0;
                                        break;
                                }
                                drawWaitSub(mixmode1, srcval);

                                if ((xgaWaitCmd.CurY < 2048)
                                        && (xgaWaitCmd.CurY >= xgaWaitCmd.Y2)) {
                                    xgaWaitCmd.Wait = false;
                                    k = 1000; // no more chunks
                                    break;
                                }
                                // next chunk goes to next line
                                if (xgaWaitCmd.NewLine)
                                    break;
                            } // pixels loop
                        } // chunks loop
                        break;

                    default:
                        Log.logMsg("XGA: DrawBlitWait: Unhandled mixmode: %d", mixmode);
                        break;
                } // switch mixmode
                break;
            default:
                Log.logMsg("XGA: Unhandled draw command %x", xgaWaitCmd.Cmd);
                break;
        }
    }

    private void blitRect(int val) {
        int xat = 0, yat = 0;
        int srcData = 0;
        int dstData = 0;

        int srcVal = 0;
        int destVal = 0;

        int srcX = 0, srcY = 0, tarX = 0, tarY = 0, dx = 0, dy = 0;

        dx = -1;
        dy = -1;

        if (((val >>> 5) & 0x01) != 0)
            dx = 1;
        if (((val >>> 7) & 0x01) != 0)
            dy = 1;

        srcX = CurX;
        srcY = CurY;
        tarX = DestX;
        tarY = DestY;

        int mixselect = (PixCntl >>> 6) & 0x3;
        int mixmode = 0x67; /* Source is bitmap data, mix mode is src */
        switch (mixselect) {
            case 0x00: /* Foreground mix is always used */
                mixmode = Foremix;
                break;
            case 0x02: /* CPU Data determines mix used */
                Log.logMsg("XGA: DrawPattern: Mixselect data from PIX_TRANS register");
                break;
            case 0x03: /* Video memory determines mix */
                // Log.LOG_MSG("XGA: Srcdata: %x, Forecolor %x, Backcolor %x, Foremix: %x Backmix:
                // %x", srcdata, xga.forecolor, xga.backcolor, xga.foremix, xga.backmix);
                break;
            default:
                Log.logMsg("XGA: BlitRect: Unknown mix select register");
                break;
        }


        /* Copy source to video ram */
        for (yat = 0; yat <= MIPcount; yat++) {
            srcX = CurX;
            tarX = DestX;

            for (xat = 0; xat <= MAPcount; xat++) {
                srcData = getPoint(srcX, srcY);
                dstData = getPoint(tarX, tarY);

                if (mixselect == 0x3) {
                    if (srcData == Forecolor) {
                        mixmode = Foremix;
                    } else {
                        if (srcData == Backcolor) {
                            mixmode = Backmix;
                        } else {
                            /* Best guess otherwise */
                            mixmode = 0x67; /* Source is bitmap data, mix mode is src */
                        }
                    }
                }

                switch ((mixmode >>> 5) & 0x03) {
                    case 0x00: /* Src is background color */
                        srcVal = Backcolor;
                        break;
                    case 0x01: /* Src is foreground color */
                        srcVal = Forecolor;
                        break;
                    case 0x02: /* Src is pixel data from PIX_TRANS register */
                        Log.logMsg("XGA: DrawPattern: Wants data from PIX_TRANS register");
                        break;
                    case 0x03: /* Src is bitmap data */
                        srcVal = srcData;
                        break;
                    default:
                        Log.logMsg("XGA: DrawPattern: Shouldn't be able to get here!");
                        srcVal = 0;
                        break;
                }

                destVal = getMixResult(mixmode, srcVal, dstData);
                // Log.LOG_MSG("XGA: DrawPattern: Mixmode: %x Mixselect: %x", mixmode, mixselect);

                drawPoint(tarX, tarY, destVal);

                srcX += dx;
                tarX += dx;
            }
            srcY += dy;
            tarY += dy;
        }
    }

    private void drawPattern(int val) {
        int srcData = 0;
        int dstData = 0;

        int srcVal = 0;
        int destVal = 0;

        int xat = 0, yat = 0, srcx = 0, srcy = 0, tarx = 0, tary = 0, dx = 0, dy = 0;

        dx = -1;
        dy = -1;

        if (((val >>> 5) & 0x01) != 0)
            dx = 1;
        if (((val >>> 7) & 0x01) != 0)
            dy = 1;

        srcx = CurX;
        srcy = CurY;

        tary = DestY;

        int mixselect = (PixCntl >>> 6) & 0x3;
        int mixmode = 0x67; /* Source is bitmap data, mix mode is src */
        switch (mixselect) {
            case 0x00: /* Foreground mix is always used */
                mixmode = Foremix;
                break;
            case 0x02: /* CPU Data determines mix used */
                Log.logMsg("XGA: DrawPattern: Mixselect data from PIX_TRANS register");
                break;
            case 0x03: /* Video memory determines mix */
                // Log.LOG_MSG("XGA: Pixctl: %x, Srcdata: %x, Forecolor %x, Backcolor %x, Foremix:
                // %x Backmix: %x",xga.pix_cntl, srcdata, xga.forecolor, xga.backcolor, xga.foremix,
                // xga.backmix);
                break;
            default:
                Log.logMsg("XGA: DrawPattern: Unknown mix select register");
                break;
        }

        for (yat = 0; yat <= MIPcount; yat++) {
            tarx = DestX;
            for (xat = 0; xat <= MAPcount; xat++) {

                srcData = getPoint(srcx + (tarx & 0x7), srcy + (tary & 0x7));
                // Log.LOG_MSG("patternpoint (%3d/%3d)v%x",srcx + (tarx & 0x7), srcy + (tary &
                // 0x7),srcdata);
                dstData = getPoint(tarx, tary);


                if (mixselect == 0x3) {
                    // TODO lots of guessing here but best results this way
                    /* if(srcdata == xga.forecolor) */
                    mixmode = Foremix;
                    // else
                    if (srcData == Backcolor || srcData == 0)
                        mixmode = Backmix;
                }

                switch ((mixmode >>> 5) & 0x03) {
                    case 0x00: /* Src is background color */
                        srcVal = Backcolor;
                        break;
                    case 0x01: /* Src is foreground color */
                        srcVal = Forecolor;
                        break;
                    case 0x02: /* Src is pixel data from PIX_TRANS register */
                        Log.logMsg("XGA: DrawPattern: Wants data from PIX_TRANS register");
                        break;
                    case 0x03: /* Src is bitmap data */
                        srcVal = srcData;
                        break;
                    default:
                        Log.logMsg("XGA: DrawPattern: Shouldn't be able to get here!");
                        srcVal = 0;
                        break;
                }

                destVal = getMixResult(mixmode, srcVal, dstData);

                drawPoint(tarx, tary, destVal);

                tarx += dx;
            }
            tary += dy;
        }
    }

    private void drawCmd(int val, int len) {
        int cmd;
        cmd = 0xffff & (val >>> 13);
        /*
         * #if XGA_SHOW_COMMAND_TRACE //Log.LOG_MSG("XGA: Draw command %x", cmd); #endif
         */
        CurCommand = val;
        switch (cmd) {
            case 1: /* Draw line */
                if ((val & 0x100) == 0) {
                    if ((val & 0x8) == 0) {
                        /*
                         * #if XGA_SHOW_COMMAND_TRACE Log.LOG_MSG("XGA: Drawing Bresenham line");
                         * #endif
                         */
                        drawLineBresenham(val);
                    } else {
                        /*
                         * #if XGA_SHOW_COMMAND_TRACE Log.LOG_MSG("XGA: Drawing vector line");
                         * #endif
                         */
                        drawLineVector(val);
                    }
                } else {
                    Log.logMsg("XGA: Wants line drawn from PIX_TRANS register!");
                }
                break;
            case 2: /* Rectangle fill */
                XGAWaitCmd xgaWaitCmd = WaitCmd;
                if ((val & 0x100) == 0) {
                    xgaWaitCmd.Wait = false;
                    /*
                     * #if XGA_SHOW_COMMAND_TRACE
                     * Log.LOG_MSG("XGA: Draw immediate rect: xy(%3d/%3d), len(%3d/%3d)",
                     * xga.curx,xga.cury,xga.MAPcount,xga.MIPcount); #endif
                     */
                    drawRectangle(val);

                } else {

                    xgaWaitCmd.NewLine = true;
                    xgaWaitCmd.Wait = true;
                    xgaWaitCmd.CurX = CurX;
                    xgaWaitCmd.CurY = CurY;
                    xgaWaitCmd.X1 = CurX;
                    xgaWaitCmd.Y1 = CurY;
                    xgaWaitCmd.X2 = (CurX + MAPcount) & 0x0fff;
                    xgaWaitCmd.Y2 = (CurY + MIPcount + 1) & 0x0fff;
                    xgaWaitCmd.SizeX = MAPcount;
                    xgaWaitCmd.SizeY = 0xffff & (MIPcount + 1);
                    xgaWaitCmd.Cmd = 2;
                    xgaWaitCmd.BusWidth = _vga.Mode.toValue() | ((val & 0x600) >>> 4);
                    xgaWaitCmd.Data = 0;
                    xgaWaitCmd.DataSize = 0;

                    /*
                     * #if XGA_SHOW_COMMAND_TRACE Log.
                     * LOG_MSG("XGA: Draw wait rect, w/h(%3d/%3d), x/y1(%3d/%3d), x/y2(%3d/%3d), %4x"
                     * , xga.MAPcount+1, xga.MIPcount+1,xga.curx,xga.cury, (xga.curx +
                     * xga.MAPcount)&0x0fff, (xga.cury + xga.MIPcount + 1)&0x0fff,val&0xffff);
                     * #endif
                     */
                }
                break;
            case 6: /* BitBLT */
                /*
                 * #if XGA_SHOW_COMMAND_TRACE Log.LOG_MSG("XGA: Blit Rect"); #endif
                 */
                blitRect(val);
                break;
            case 7: /* Pattern fill */
                /*
                 * #if XGA_SHOW_COMMAND_TRACE
                 * Log.LOG_MSG("XGA: Pattern fill: src(%3d/%3d), dest(%3d/%3d), fill(%3d/%3d)",
                 * xga.curx,xga.cury,xga.destx,xga.desty,xga.MAPcount,xga.MIPcount); #endif
                 */
                drawPattern(val);
                break;
            default:
                Log.logMsg("XGA: Unhandled draw command %x", cmd);
                break;
        }
    }

    private int setDualReg(int reg, int val) {
        switch (_vga.S3.XGAColorMode) {
            case LIN8:
                reg = val & 0xff;
                break;
            case LIN15:
            case LIN16:
                reg = val & 0xffff;
                break;
            case LIN32:
                if ((Control1 & 0x200) != 0)
                    reg = val;
                else if ((Control1 & 0x10) != 0)
                    reg = (reg & 0x0000ffff) | (val << 16);
                else
                    reg = (reg & 0xffff0000) | (val & 0x0000ffff);
                Control1 = 0xffff & (Control1 ^ 0x10);
                break;
        }
        return reg;
    }

    private int getDualReg(int reg) {
        switch (_vga.S3.XGAColorMode) {
            case LIN8:
                return reg & 0xff;
            case LIN15:
            case LIN16:
                return reg & 0xffff;
            case LIN32:
                if ((Control1 & 0x200) != 0)
                    return reg;
                Control1 = 0xffff & (Control1 ^ 0x10);
                if ((Control1 & 0x10) != 0)
                    return reg & 0x0000ffff;
                else
                    return reg >>> 16;
        }
        return 0;
    }


    public void write(int port, int val, int len) {
        // Log.LOG_MSG("XGA: Write to port %x, val %8x, len %x", port,val, len);

        switch (port) {
            case 0x8100:// drawing control: row (low word), column (high word)
                // "CUR_X" and "CUR_Y" (see PORT 82E8h,PORT 86E8h)
                CurY = val & 0x0fff;
                if (len == 4)
                    CurX = (val >>> 16) & 0x0fff;
                break;
            case 0x8102:
                CurX = val & 0x0fff;
                break;

            case 0x8108:// DWORD drawing control: destination Y and axial step
                // constant (low word), destination X and axial step
                // constant (high word) (see PORT 8AE8h,PORT 8EE8h)
                DestY = val & 0x3FFF;
                if (len == 4)
                    DestX = (val >>> 16) & 0x3fff;
                break;
            case 0x810a:
                DestX = val & 0x3fff;
                break;
            case 0x8110: // WORD error term (see PORT 92E8h)
                ErrTerm = val & 0x3FFF;
                break;

            case 0x8120: // packed MMIO: DWORD background color (see PORT A2E8h)
                Backcolor = val;
                break;
            case 0x8124: // packed MMIO: DWORD foreground color (see PORT A6E8h)
                Forecolor = val;
                break;
            case 0x8128: // DWORD write mask (see PORT AAE8h)
                WriteMask = val;
                break;
            case 0x812C: // DWORD read mask (see PORT AEE8h)
                ReadMask = val;
                break;
            case 0x8134: // packed MMIO: DWORD background mix (low word) and
                // foreground mix (high word) (see PORT B6E8h,PORT BAE8h)
                Backmix = val & 0xFFFF;
                if (len == 4)
                    Foremix = 0xffff & (val >>> 16);
                break;
            case 0x8136:
                Foremix = 0xffff & val;
                break;
            case 0x8138:// DWORD top scissors (low word) and left scissors (high
                // word) (see PORT BEE8h,#P1047)
                Scissors.y1 = val & 0x0fff;
                if (len == 4)
                    Scissors.x1 = (val >>> 16) & 0x0fff;
                break;
            case 0x813a:
                Scissors.x1 = val & 0x0fff;
                break;
            case 0x813C:// DWORD bottom scissors (low word) and right scissors
                // (high word) (see PORT BEE8h,#P1047)
                Scissors.y2 = val & 0x0fff;
                if (len == 4)
                    Scissors.x2 = (val >>> 16) & 0x0fff;
                break;
            case 0x813e:
                Scissors.x2 = val & 0x0fff;
                break;

            case 0x8140:// DWORD data manipulation control (low word) and
                // miscellaneous 2 (high word) (see PORT BEE8h,#P1047)
                PixCntl = val & 0xFFFF;
                if (len == 4)
                    Control2 = (val >>> 16) & 0x0fff;
                break;
            case 0x8144:// DWORD miscellaneous (low word) and read register select
                // (high word)(see PORT BEE8h,#P1047)
                Control1 = val & 0xffff;
                if (len == 4)
                    ReadSel = (val >>> 16) & 0x7;
                break;
            case 0x8148:// DWORD minor axis pixel count (low word) and major axis
                // pixel count (high word) (see PORT BEE8h,#P1047,PORT 96E8h)
                MIPcount = val & 0x0fff;
                if (len == 4)
                    MAPcount = (val >>> 16) & 0x0fff;
                break;
            case 0x814a:
                MAPcount = val & 0x0fff;
                break;
            case 0x92e8:
                ErrTerm = val & 0x3FFF;
                break;
            case 0x96e8:
                MAPcount = val & 0x0fff;
                break;
            case 0x9ae8:
            case 0x8118: // Trio64V+ packed MMIO
                drawCmd(val, len);
                break;
            case 0xa2e8:
                Backcolor = setDualReg(Backcolor, val);
                break;
            case 0xa6e8:
                Forecolor = setDualReg(Forecolor, val);
                break;
            case 0xaae8:
                WriteMask = setDualReg(WriteMask, val);
                break;
            case 0xaee8:
                ReadMask = setDualReg(ReadMask, val);
                break;
            case 0x82e8:
                CurY = val & 0x0fff;
                break;
            case 0x86e8:
                CurX = val & 0x0fff;
                break;
            case 0x8ae8:
                DestY = val & 0x3fff;
                break;
            case 0x8ee8:
                DestX = val & 0x3fff;
                break;
            case 0xb2e8:
                Log.logMsg("COLOR_CMP not implemented");
                break;
            case 0xb6e8:
                Backmix = 0xffff & val;
                break;
            case 0xbae8:
                Foremix = 0xffff & val;
                break;
            case 0xbee8:
                writeMultiFunc(val, len);
                break;
            case 0xe2e8:
                WaitCmd.NewLine = false;
                drawWait(val, len);
                break;
            case 0x83d4:
                if (len == 1)
                    _vga.Crtc.writeP3D4(0, val, 1);
                else if (len == 2) {
                    _vga.Crtc.writeP3D4(0, val & 0xff, 1);
                    _vga.Crtc.writeP3D5(0, val >>> 8, 1);
                } else
                    Support.exceptionExit("unimplemented XGA MMIO");
                break;
            case 0x83d5:
                if (len == 1)
                    _vga.Crtc.writeP3D5(0, val, 1);
                else
                    Support.exceptionExit("unimplemented XGA MMIO");
                break;
            default:
                if (port <= 0x4000) {
                    // Log.LOG_MSG("XGA: Wrote to port %4x with %08x, len %x", port, val, len);
                    WaitCmd.NewLine = false;
                    drawWait(val, len);

                } else
                    Log.logMsg("XGA: Wrote to port %x with %x, len %x", port, val, len);
                break;
        }
    }

    public int read(int port, int len) {
        switch (port) {
            case 0x8118:
            case 0x9ae8:
                return 0x400; // nothing busy
            case 0x81ec: // S3 video data processor
                return 0x00007000;
            case 0x83da: {
                int delaycyc = CPU.CycleMax / 5000;
                if (CPU.Cycles < 3 * delaycyc)
                    delaycyc = 0;
                CPU.Cycles -= delaycyc;
                CPU.IODelayRemoved += delaycyc;
                return _vga.readP3DA(0, 0);
            }
            case 0x83d4:
                if (len == 1)
                    return _vga.Crtc.readP3D4(0, 0);
                else
                    Support.exceptionExit("unimplemented XGA MMIO");
                break;
            case 0x83d5:
                if (len == 1)
                    return _vga.Crtc.readP3D5(0, 0);
                else
                    Support.exceptionExit("unimplemented XGA MMIO");
                break;
            case 0x9ae9:
                if (WaitCmd.Wait)
                    return 0x4;
                else
                    return 0x0;
            case 0xbee8:
                return readMultifunc();
            case 0xa2e8:
                return getDualReg(Backcolor);
            case 0xa6e8:
                return getDualReg(Forecolor);
            case 0xaae8:
                return getDualReg(WriteMask);
            case 0xaee8:
                return getDualReg(ReadMask);
            default:
                // Log.LOG_MSG("XGA: Read from port %x, len %x", port, len);
                break;
        }
        return 0xffffffff;
    }

    public void setup() {
        if (!DOSBox.isVGAArch())
            return;

        Scissors.y1 = 0;
        Scissors.x1 = 0;
        Scissors.y2 = 0xFFF;
        Scissors.x2 = 0xFFF;
        WriteHandler writeWrap = this::write;
        ReadHandler readWrap = this::read;
        IO.registerWriteHandler(0x42e8, writeWrap, IO.IO_MB | IO.IO_MW | IO.IO_MD);
        IO.registerReadHandler(0x42e8, readWrap, IO.IO_MB | IO.IO_MW | IO.IO_MD);

        IO.registerWriteHandler(0x46e8, writeWrap, IO.IO_MB | IO.IO_MW | IO.IO_MD);
        IO.registerWriteHandler(0x4ae8, writeWrap, IO.IO_MB | IO.IO_MW | IO.IO_MD);

        IO.registerWriteHandler(0x82e8, writeWrap, IO.IO_MB | IO.IO_MW | IO.IO_MD);
        IO.registerReadHandler(0x82e8, readWrap, IO.IO_MB | IO.IO_MW | IO.IO_MD);
        IO.registerWriteHandler(0x82e9, writeWrap, IO.IO_MB | IO.IO_MW | IO.IO_MD);
        IO.registerReadHandler(0x82e9, readWrap, IO.IO_MB | IO.IO_MW | IO.IO_MD);

        IO.registerWriteHandler(0x86e8, writeWrap, IO.IO_MB | IO.IO_MW | IO.IO_MD);
        IO.registerReadHandler(0x86e8, readWrap, IO.IO_MB | IO.IO_MW | IO.IO_MD);
        IO.registerWriteHandler(0x86e9, writeWrap, IO.IO_MB | IO.IO_MW | IO.IO_MD);
        IO.registerReadHandler(0x86e9, readWrap, IO.IO_MB | IO.IO_MW | IO.IO_MD);

        IO.registerWriteHandler(0x8ae8, writeWrap, IO.IO_MB | IO.IO_MW | IO.IO_MD);
        IO.registerReadHandler(0x8ae8, readWrap, IO.IO_MB | IO.IO_MW | IO.IO_MD);

        IO.registerWriteHandler(0x8ee8, writeWrap, IO.IO_MB | IO.IO_MW | IO.IO_MD);
        IO.registerReadHandler(0x8ee8, readWrap, IO.IO_MB | IO.IO_MW | IO.IO_MD);
        IO.registerWriteHandler(0x8ee9, writeWrap, IO.IO_MB | IO.IO_MW | IO.IO_MD);
        IO.registerReadHandler(0x8ee9, readWrap, IO.IO_MB | IO.IO_MW | IO.IO_MD);

        IO.registerWriteHandler(0x92e8, writeWrap, IO.IO_MB | IO.IO_MW | IO.IO_MD);
        IO.registerReadHandler(0x92e8, readWrap, IO.IO_MB | IO.IO_MW | IO.IO_MD);
        IO.registerWriteHandler(0x92e9, writeWrap, IO.IO_MB | IO.IO_MW | IO.IO_MD);
        IO.registerReadHandler(0x92e9, readWrap, IO.IO_MB | IO.IO_MW | IO.IO_MD);

        IO.registerWriteHandler(0x96e8, writeWrap, IO.IO_MB | IO.IO_MW | IO.IO_MD);
        IO.registerReadHandler(0x96e8, readWrap, IO.IO_MB | IO.IO_MW | IO.IO_MD);
        IO.registerWriteHandler(0x96e9, writeWrap, IO.IO_MB | IO.IO_MW | IO.IO_MD);
        IO.registerReadHandler(0x96e9, readWrap, IO.IO_MB | IO.IO_MW | IO.IO_MD);

        IO.registerWriteHandler(0x9ae8, writeWrap, IO.IO_MB | IO.IO_MW | IO.IO_MD);
        IO.registerReadHandler(0x9ae8, readWrap, IO.IO_MB | IO.IO_MW | IO.IO_MD);
        IO.registerWriteHandler(0x9ae9, writeWrap, IO.IO_MB | IO.IO_MW | IO.IO_MD);
        IO.registerReadHandler(0x9ae9, readWrap, IO.IO_MB | IO.IO_MW | IO.IO_MD);

        IO.registerWriteHandler(0x9ee8, writeWrap, IO.IO_MB | IO.IO_MW | IO.IO_MD);
        IO.registerReadHandler(0x9ee8, readWrap, IO.IO_MB | IO.IO_MW | IO.IO_MD);
        IO.registerWriteHandler(0x9ee9, writeWrap, IO.IO_MB | IO.IO_MW | IO.IO_MD);
        IO.registerReadHandler(0x9ee9, readWrap, IO.IO_MB | IO.IO_MW | IO.IO_MD);

        IO.registerWriteHandler(0xa2e8, writeWrap, IO.IO_MB | IO.IO_MW | IO.IO_MD);
        IO.registerReadHandler(0xa2e8, readWrap, IO.IO_MB | IO.IO_MW | IO.IO_MD);

        IO.registerWriteHandler(0xa6e8, writeWrap, IO.IO_MB | IO.IO_MW | IO.IO_MD);
        IO.registerReadHandler(0xa6e8, readWrap, IO.IO_MB | IO.IO_MW | IO.IO_MD);
        IO.registerWriteHandler(0xa6e9, writeWrap, IO.IO_MB | IO.IO_MW | IO.IO_MD);
        IO.registerReadHandler(0xa6e9, readWrap, IO.IO_MB | IO.IO_MW | IO.IO_MD);

        IO.registerWriteHandler(0xaae8, writeWrap, IO.IO_MB | IO.IO_MW | IO.IO_MD);
        IO.registerReadHandler(0xaae8, readWrap, IO.IO_MB | IO.IO_MW | IO.IO_MD);
        IO.registerWriteHandler(0xaae9, writeWrap, IO.IO_MB | IO.IO_MW | IO.IO_MD);
        IO.registerReadHandler(0xaae9, readWrap, IO.IO_MB | IO.IO_MW | IO.IO_MD);

        IO.registerWriteHandler(0xaee8, writeWrap, IO.IO_MB | IO.IO_MW | IO.IO_MD);
        IO.registerReadHandler(0xaee8, readWrap, IO.IO_MB | IO.IO_MW | IO.IO_MD);
        IO.registerWriteHandler(0xaee9, writeWrap, IO.IO_MB | IO.IO_MW | IO.IO_MD);
        IO.registerReadHandler(0xaee9, readWrap, IO.IO_MB | IO.IO_MW | IO.IO_MD);

        IO.registerWriteHandler(0xb2e8, writeWrap, IO.IO_MB | IO.IO_MW | IO.IO_MD);
        IO.registerReadHandler(0xb2e8, readWrap, IO.IO_MB | IO.IO_MW | IO.IO_MD);
        IO.registerWriteHandler(0xb2e9, writeWrap, IO.IO_MB | IO.IO_MW | IO.IO_MD);
        IO.registerReadHandler(0xb2e9, readWrap, IO.IO_MB | IO.IO_MW | IO.IO_MD);

        IO.registerWriteHandler(0xb6e8, writeWrap, IO.IO_MB | IO.IO_MW | IO.IO_MD);
        IO.registerReadHandler(0xb6e8, readWrap, IO.IO_MB | IO.IO_MW | IO.IO_MD);

        IO.registerWriteHandler(0xbee8, writeWrap, IO.IO_MB | IO.IO_MW | IO.IO_MD);
        IO.registerReadHandler(0xbee8, readWrap, IO.IO_MB | IO.IO_MW | IO.IO_MD);
        IO.registerWriteHandler(0xbee9, writeWrap, IO.IO_MB | IO.IO_MW | IO.IO_MD);
        IO.registerReadHandler(0xbee9, readWrap, IO.IO_MB | IO.IO_MW | IO.IO_MD);

        IO.registerWriteHandler(0xbae8, writeWrap, IO.IO_MB | IO.IO_MW | IO.IO_MD);
        IO.registerReadHandler(0xbae8, readWrap, IO.IO_MB | IO.IO_MW | IO.IO_MD);
        IO.registerWriteHandler(0xbae9, writeWrap, IO.IO_MB | IO.IO_MW | IO.IO_MD);
        IO.registerReadHandler(0xbae9, readWrap, IO.IO_MB | IO.IO_MW | IO.IO_MD);

        IO.registerWriteHandler(0xe2e8, writeWrap, IO.IO_MB | IO.IO_MW | IO.IO_MD);
        IO.registerReadHandler(0xe2e8, readWrap, IO.IO_MB | IO.IO_MW | IO.IO_MD);

        IO.registerWriteHandler(0xe2e0, writeWrap, IO.IO_MB | IO.IO_MW | IO.IO_MD);
        IO.registerReadHandler(0xe2e0, readWrap, IO.IO_MB | IO.IO_MW | IO.IO_MD);

        IO.registerWriteHandler(0xe2ea, writeWrap, IO.IO_MB | IO.IO_MW | IO.IO_MD);
        IO.registerReadHandler(0xe2ea, readWrap, IO.IO_MB | IO.IO_MW | IO.IO_MD);
    }
}
