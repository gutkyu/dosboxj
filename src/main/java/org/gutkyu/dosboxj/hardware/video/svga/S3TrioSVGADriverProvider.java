package org.gutkyu.dosboxj.hardware.video.svga;

import org.gutkyu.dosboxj.util.*;
import org.gutkyu.dosboxj.hardware.memory.*;
import org.gutkyu.dosboxj.hardware.video.*;
import org.gutkyu.dosboxj.interrupt.int10.INT10Mode;

public final class S3TrioSVGADriverProvider {
    VGA vga = null;

    public S3TrioSVGADriverProvider(VGA vga) {
        this.vga = vga;
        vga.SVGADrv.WriteP3D5 = this::writeCrtc;
        vga.SVGADrv.ReadP3D5 = this::readCrtc;
        vga.SVGADrv.WriteP3C5 = this::writeSeq;
        vga.SVGADrv.ReadP3C5 = this::readSEQ;
        vga.SVGADrv.WriteP3C0 = null; /* no S3-specific functionality */
        vga.SVGADrv.ReadP3C1 = null; /* no S3-specific functionality */

        vga.SVGADrv.SetVideoMode = null; /* implemented in core */
        vga.SVGADrv.DetermineMode = null; /* implemented in core */
        vga.SVGADrv.SetClock = null; /* implemented in core */
        vga.SVGADrv.GetClock = this::getClock;
        vga.SVGADrv.HardwareCursorActive = this::activeHWCursor;
        vga.SVGADrv.AcceptsMode = this::acceptsMode;

        if (vga.VMemSize == 0)
            vga.VMemSize = 2 * 1024 * 1024; // the most common S3 configuration

        // Set CRTC 36 to specify amount of VRAM and PCI
        if (vga.VMemSize < 1024 * 1024) {
            vga.VMemSize = 512 * 1024;
            vga.S3.Reg36 = (byte) 0xfa; // less than 1mb fast page mode
        } else if (vga.VMemSize < 2048 * 1024) {
            vga.VMemSize = 1024 * 1024;
            vga.S3.Reg36 = (byte) 0xda; // 1mb fast page mode
        } else if (vga.VMemSize < 3072 * 1024) {
            vga.VMemSize = 2048 * 1024;
            vga.S3.Reg36 = (byte) 0x9a; // 2mb fast page mode
        } else if (vga.VMemSize < 4096 * 1024) {
            vga.VMemSize = 3072 * 1024;
            vga.S3.Reg36 = 0x5a; // 3mb fast page mode
        } else { // Trio64 supported only up to 4M
            vga.VMemSize = 4096 * 1024;
            vga.S3.Reg36 = 0x1a; // 4mb fast page mode
        }

        // S3 ROM signature
        int romBase = Memory.physMake(0xc000, 0);
        Memory.physWriteB(romBase + 0x003f, 'S');
        Memory.physWriteB(romBase + 0x0040, '3');
        Memory.physWriteB(romBase + 0x0041, ' ');
        Memory.physWriteB(romBase + 0x0042, '8');
        Memory.physWriteB(romBase + 0x0043, '6');
        Memory.physWriteB(romBase + 0x0044, 'C');
        Memory.physWriteB(romBase + 0x0045, '7');
        Memory.physWriteB(romBase + 0x0046, '6');
        Memory.physWriteB(romBase + 0x0047, '4');
    }

    // private void SVGA_S3_WriteCRTC(int reg, int val, int iolen) {
    private void writeCrtc(int reg, int val, int iolen) {
        switch (reg) {
            case 0x31: /* CR31 Memory Configuration */
                // TODO Base address
                vga.S3.Reg31 = (byte) val;
                vga.Config.CompatibleChain4 = (val & 0x08) == 0;
                if (vga.Config.CompatibleChain4)
                    vga.VMemWrap = 256 * 1024;
                else
                    vga.VMemWrap = vga.VMemSize;
                vga.Config.DisplayStart =
                        (vga.Config.DisplayStart & ~0x30000) | ((val & 0x30) << 12);
                vga.determineMode();
                vga.setupHandlers();
                break;
            /*
             * 0 Enable Base Address Offset (CPUA BASE). Enables bank operation if set, disables if
             * clear. 1 Two Page Screen Image. If set enables 2048 pixel wide screen setup 2 VGA
             * 16bit Memory Bus Width. Set for 16bit, clear for 8bit 3 Use Enhanced Mode Memory
             * Mapping (ENH MAP). Set to enable access to video memory above 256k. 4-5 Bit 16-17 of
             * the Display Start Address. For the 801/5,928 see index 51h, for the 864/964 see index
             * 69h. 6 High Speed Text Display Font Fetch Mode. If set enables Page Mode for Alpha
             * Mode Font Access. 7 (not 864/964) Extended BIOS ROM Space Mapped out. If clear the
             * area C6800h-C7FFFh is mapped out, if set it is accessible.
             */
            case 0x35: /* CR35 CRT Register Lock */
                if (vga.S3.RegLock1 != 0x48)
                    return; // Needed for uvconfig detection
                vga.S3.Reg35 = (byte) (val & 0xf0);
                if (((vga.SVGA.BankRead & 0xf) ^ (val & 0xf)) != 0) {
                    vga.SVGA.BankRead &= 0xf0;
                    vga.SVGA.BankRead |= val & 0xf;
                    vga.SVGA.BankWrite = vga.SVGA.BankRead;
                    vga.setupHandlers();
                }
                break;
            /*
             * 0-3 CPU Base Address. 64k bank number. For the 801/5 and 928 see 3d4h index 51h bits
             * 2-3. For the 864/964 see index 6Ah. 4 Lock Vertical Timing Registers (LOCK VTMG).
             * Locks 3d4h index 6, 7 (bits 0,2,3,5,7), 9 bit 5, 10h, 11h bits 0-3, 15h, 16h if set 5
             * Lock Horizontal Timing Registers (LOCK HTMG). Locks 3d4h index 0,1,2,3,4,5,17h bit 2
             * if set 6 (911/924) Lock VSync Polarity. 7 (911/924) Lock HSync Polarity.
             */
            case 0x38: /* CR38 Register Lock 1 */
                vga.S3.RegLock1 = (byte) val;
                break;
            case 0x39: /* CR39 Register Lock 2 */
                vga.S3.RegLock2 = (byte) val;
                break;
            case 0x3a:
                vga.S3.Reg3A = (byte) val;
                break;
            case 0x40: /* CR40 System Config */
                vga.S3.Reg40 = (byte) val;
                break;
            case 0x41: /* CR41 BIOS flags */
                vga.S3.Reg41 = (byte) val;
                break;
            case 0x43: /* CR43 Extended Mode */
                vga.S3.Reg43 = (byte) (val & ~0x4);
                if ((((val & 0x4) ^ (vga.Config.ScanLen >>> 6)) & 0x4) != 0) {
                    vga.Config.ScanLen &= 0x2ff;
                    vga.Config.ScanLen |= (val & 0x4) << 6;
                    vga.checkScanLength();
                }
                break;
            /*
             * 2 Logical Screen Width bit 8. Bit 8 of the Display Offset Register/ (3d4h index 13h).
             * (801/5,928) Only active if 3d4h index 51h bits 4-5 are 0
             */
            case 0x45: /* Hardware cursor mode */
                vga.S3.HGC.CurMode = (byte) val;
                // Activate hardware cursor code if needed
                vga.activateHardwareCursor();
                break;
            case 0x46:
                vga.S3.HGC.OriginX = 0xffff & ((vga.S3.HGC.OriginX & 0x00ff) | (val << 8));
                break;
            case 0x47: /* HGC orgX */
                vga.S3.HGC.OriginX = 0xffff & ((vga.S3.HGC.OriginX & 0xff00) | val);
                break;
            case 0x48:
                vga.S3.HGC.OriginY = 0xffff & ((vga.S3.HGC.OriginY & 0x00ff) | (val << 8));
                break;
            case 0x49: /* HGC orgY */
                vga.S3.HGC.OriginY = 0xffff & ((vga.S3.HGC.OriginY & 0xff00) | val);
                break;
            case 0x4A: /* HGC foreground stack */
                if ((vga.S3.HGC.ForeStackPos & 0xff) > 2)
                    vga.S3.HGC.ForeStackPos = 0;
                vga.S3.HGC.ForeStack[vga.S3.HGC.ForeStackPos] = (byte) val;
                vga.S3.HGC.ForeStackPos++;
                break;
            case 0x4B: /* HGC background stack */
                if ((vga.S3.HGC.BackStackPos & 0xff) > 2)
                    vga.S3.HGC.BackStackPos = 0;
                vga.S3.HGC.BackStack[vga.S3.HGC.BackStackPos] = (byte) val;
                vga.S3.HGC.BackStackPos++;
                break;
            case 0x4c: /* HGC start address high byte */
                vga.S3.HGC.StartAddr &= 0xff;
                vga.S3.HGC.StartAddr |= (val & 0xf) << 8;
                if (((0xffff & vga.S3.HGC.StartAddr) << 10) + ((64 * 64 * 2) / 8) > vga.VMemSize) {
                    vga.S3.HGC.StartAddr &= 0xff; // put it back to some sane area;
                    // if read back of this address is ever implemented this needs to change
                    Log.logging(Log.LogTypes.VGAMISC, Log.LogServerities.Normal,
                            "VGA:S3:CRTC: HGC pattern address beyond video memory");
                }
                break;
            case 0x4d: /* HGC start address low byte */
                vga.S3.HGC.StartAddr &= 0xff00;
                vga.S3.HGC.StartAddr |= val & 0xff;
                break;
            case 0x4e: /* HGC pattern start X */
                vga.S3.HGC.PosX = val & 0x3f; // bits 0-5
                break;
            case 0x4f: /* HGC pattern start Y */
                vga.S3.HGC.PosY = val & 0x3f; // bits 0-5
                break;
            case 0x50: // Extended System Control 1
                vga.S3.Reg50 = (byte) val;
                switch (val & VGA.S3_XGA_CMASK) {
                    case VGA.S3_XGA_32BPP:
                        vga.S3.XGAColorMode = VGAModes.LIN32;
                        break;
                    case VGA.S3_XGA_16BPP:
                        vga.S3.XGAColorMode = VGAModes.LIN16;
                        break;
                    case VGA.S3_XGA_8BPP:
                        vga.S3.XGAColorMode = VGAModes.LIN8;
                        break;
                }
                switch (val & VGA.S3_XGA_WMASK) {
                    case VGA.S3_XGA_1024:
                        vga.S3.XGAScreenWidth = 1024;
                        break;
                    case VGA.S3_XGA_1152:
                        vga.S3.XGAScreenWidth = 1152;
                        break;
                    case VGA.S3_XGA_640:
                        vga.S3.XGAScreenWidth = 640;
                        break;
                    case VGA.S3_XGA_800:
                        vga.S3.XGAScreenWidth = 800;
                        break;
                    case VGA.S3_XGA_1280:
                        vga.S3.XGAScreenWidth = 1280;
                        break;
                    default:
                        vga.S3.XGAScreenWidth = 1024;
                        break;
                }
                break;
            case 0x51: /* Extended System Control 2 */
                vga.S3.Reg51 = (byte) (val & 0xc0); // Only store bits 6,7
                vga.Config.DisplayStart &= 0xF3FFFF;
                vga.Config.DisplayStart |= (val & 3) << 18;
                if (((vga.SVGA.BankRead & 0x30) ^ ((val & 0xc) << 2)) != 0) {
                    vga.SVGA.BankRead &= 0xcf;
                    vga.SVGA.BankRead |= 0xff & ((val & 0xc) << 2);
                    vga.SVGA.BankWrite = vga.SVGA.BankRead;
                    vga.setupHandlers();
                }
                if ((((val & 0x30) ^ (vga.Config.ScanLen >>> 4)) & 0x30) != 0) {
                    vga.Config.ScanLen &= 0xff;
                    vga.Config.ScanLen |= (val & 0x30) << 4;
                    vga.checkScanLength();
                }
                break;
            /*
             * 0 (80x) Display Start Address bit 18 0-1 (928 +) Display Start Address bit 18-19 int
             * 16-17 are in index 31h bits 4-5, int 0-15 are in 3d4h index 0Ch,0Dh. For the 864/964
             * see 3d4h index 69h 2 (80x) CPU BASE. CPU Base Address Bit 18. 2-3 (928 +) Old CPU
             * Base Address int 19-18. 64K Bank register bits 4-5. int 0-3 are in 3d4h index 35h.
             * For the 864/964 see 3d4h index 6Ah 4-5 Logical Screen Width Bit [8-9]. int 8-9 of the
             * CRTC Offset register (3d4h index 13h). If this field is 0, 3d4h index 43h bit 2 is
             * active 6 (928,964) DIS SPXF. Disable Split Transfers if set. Spilt Transfers allows
             * transferring one half of the VRAM shift register data while the other half is being
             * output. For the 964 Split Transfers must be enabled in enhanced modes (4AE8h bit 0
             * set). Guess: They probably can't time the VRAM load cycle closely enough while the
             * graphics engine is running. 7 (not 864/964) Enable EPROM Write. If set enables flash
             * memory write control to the BIOS ROM address
             */
            case 0x52: // Extended System Control 1
                vga.S3.Reg52 = (byte) val;
                break;
            case 0x53:
                // Map or unmap MMIO
                // bit 4 = MMIO at A0000
                // bit 3 = MMIO at LFB + 16M (should be fine if its always enabled for now)
                if ((vga.S3.ExtMemCtrl & 0xff) != val) {
                    vga.S3.ExtMemCtrl = (byte) val;
                    vga.setupHandlers();
                }
                break;
            case 0x55: /* Extended Video DAC Control */
                vga.S3.Reg55 = (byte) val;
                break;
            /*
             * 0-1 DAC Register Select int. Passed to the RS2 and RS3 pins on the RAMDAC, allowing
             * access to all 8 or 16 registers on advanced RAMDACs. If this field is 0, 3d4h index
             * 43h bit 1 is active. 2 Enable General Input Port Read. If set DAC reads are disabled
             * and the STRD strobe for reading the General Input Port is enabled for reading while
             * DACRD is active, if clear DAC reads are enabled. 3 (928) Enable External SID
             * Operation if set. If set video data is passed directly from the VRAMs to the DAC
             * rather than through the VGA chip 4 Hardware Cursor MS/X11 Mode. If set the Hardware
             * Cursor is in X11 mode, if clear in MS-Windows mode 5 (80x,928) Hardware Cursor
             * External Operation Mode. If set the two bits of cursor data ,is output on the HC[0-1]
             * pins for the video DAC The SENS pin becomes HC1 and the MID2 pin becomes HC0. 6 ?? 7
             * (80x,928) Disable PA Output. If set PA[0-7] and VCLK are tristated. (864/964) TOFF
             * VCLK. Tri-State Off VCLK Output. VCLK output tri -stated if set
             */
            case 0x58: /* Linear Address Window Control */
                vga.S3.Reg58 = (byte) val;
                break;
            /*
             * 0-1 Linear Address Window Size. Must be less than or equal to video memory size. 0:
             * 64K, 1: 1MB, 2: 2MB, 3: 4MB (928)/8Mb (864/964) 2 (not 864/964) Enable Read Ahead
             * Cache if set 3 (80x,928) ISA Latch Address. If set latches address during every ISA
             * cycle, unlatches during every ISA cycle if clear. (864/964) LAT DEL. Address Latch
             * Delay Control (VL-Bus only). If set address latching occours in the T1 cycle, if
             * clear in the T2 cycle (I.e. one clock cycle delayed). 4 ENB LA. Enable Linear
             * Addressing if set. 5 (not 864/964) Limit Entry Depth for Write-Post. If set limits
             * Write -Post Entry Depth to avoid ISA bus timeout due to wait cycle limit. 6 (928,964)
             * Serial Access Mode (SAM) 256 Words Control. If set SAM control is 256 words, if clear
             * 512 words. 7 (928) RAS 6-MCLK. If set the random read/write cycle time is 6MCLKs, if
             * clear 7MCLKs
             */
            case 0x59: /* Linear Address Window Position High */
                if (((vga.S3.LaWindow & 0xff00) ^ (val << 8)) != 0) {
                    vga.S3.LaWindow = 0xffff & ((vga.S3.LaWindow & 0x00ff) | (val << 8));
                    vga.startUpdateLFB();
                }
                break;
            case 0x5a: /* Linear Address Window Position Low */
                if (((vga.S3.LaWindow & 0x00ff) ^ val) != 0) {
                    vga.S3.LaWindow = 0xffff & ((vga.S3.LaWindow & 0xff00) | val);
                    vga.startUpdateLFB();
                }
                break;
            case 0x5D: /* Extended Horizontal Overflow */
                if (((val ^ vga.S3.ExHorOverflow) & 3) != 0) {
                    vga.S3.ExHorOverflow = (byte) val;
                    vga.startResize();
                } else
                    vga.S3.ExHorOverflow = (byte) val;
                break;
            /*
             * 0 Horizontal Total bit 8. Bit 8 of the Horizontal Total register (3d4h index 0) 1
             * Horizontal Display End bit 8. Bit 8 of the Horizontal Display End register (3d4h
             * index 1) 2 Start Horizontal Blank bit 8. Bit 8 of the Horizontal Start Blanking
             * register (3d4h index 2). 3 (864,964) EHB+64. End Horizontal Blank +64. If set the
             * /BLANK pulse is extended by 64 DCLKs. Note: Is this bit 6 of 3d4h index 3 or does it
             * really extend by 64 ? 4 Start Horizontal Sync Position bit 8. Bit 8 of the Horizontal
             * Start Retrace register (3d4h index 4). 5 (864,964) EHS+32. End Horizontal Sync +32.
             * If set the HSYNC pulse is extended by 32 DCLKs. Note: Is this bit 5 of 3d4h index 5
             * or does it really extend by 32 ? 6 (928,964) Data Transfer Position bit 8. Bit 8 of
             * the Data Transfer Position register (3d4h index 3Bh) 7 (928,964) Bus-Grant Terminate
             * Position bit 8. Bit 8 of the Bus Grant Termination register (3d4h index 5Fh).
             */
            case 0x5e: /* Extended Vertical Overflow */
                vga.Config.LineCompare = (vga.Config.LineCompare & 0x3ff) | (val & 0x40) << 4;
                if (((val ^ vga.S3.ExVerOverflow) & 0x3) != 0) {
                    vga.S3.ExVerOverflow = (byte) val;
                    vga.startResize();
                } else
                    vga.S3.ExVerOverflow = (byte) val;
                break;
            /*
             * 0 Vertical Total bit 10. Bit 10 of the Vertical Total register (3d4h index 6). int 8
             * and 9 are in 3d4h index 7 bit 0 and 5. 1 Vertical Display End bit 10. Bit 10 of the
             * Vertical Display End register (3d4h index 12h). int 8 and 9 are in 3d4h index 7 bit 1
             * and 6 2 Start Vertical Blank bit 10. Bit 10 of the Vertical Start Blanking register
             * (3d4h index 15h). Bit 8 is in 3d4h index 7 bit 3 and bit 9 in 3d4h index 9 bit 5 4
             * Vertical Retrace Start bit 10. Bit 10 of the Vertical Start Retrace register (3d4h
             * index 10h). int 8 and 9 are in 3d4h index 7 bit 2 and 7. 6 Line Compare Position bit
             * 10. Bit 10 of the Line Compare register (3d4h index 18h). Bit 8 is in 3d4h index 7
             * bit 4 and bit 9 in 3d4h index 9 bit 6.
             */
            case 0x67: /* Extended Miscellaneous Control 2 */
                /*
                 * 0 VCLK PHS. VCLK Phase With Respect to DCLK. If clear VLKC is inverted DCLK, if
                 * set VCLK = DCLK. 2-3 (Trio64V+) streams mode 00 disable Streams Processor 01
                 * overlay secondary stream on VGA-mode background 10 reserved 11 full Streams
                 * Processor operation 4-7 Pixel format. 0 Mode 0: 8bit (1 pixel/VCLK) 1 Mode 8:
                 * 8bit (2 pixels/VCLK) 3 Mode 9: 15bit (1 pixel/VCLK) 5 Mode 10: 16bit (1
                 * pixel/VCLK) 7 Mode 11: 24/32bit (2 VCLKs/pixel) 13 (732/764) 32bit (1 pixel/VCLK)
                 */
                vga.S3.MiscControl2 = 0xff & val;
                vga.determineMode();
                break;
            case 0x69: /* Extended System Control 3 */
                if ((((vga.Config.DisplayStart & 0x1f0000) >>> 16) ^ (val & 0x1f)) != 0) {
                    vga.Config.DisplayStart &= 0xffff;
                    vga.Config.DisplayStart |= (val & 0x1f) << 16;
                }
                break;
            case 0x6a: /* Extended System Control 4 */
                vga.SVGA.BankRead = val & 0x7f;
                vga.SVGA.BankWrite = vga.SVGA.BankRead;
                vga.setupHandlers();
                break;
            case 0x6b: // BIOS scratchpad: LFB adress
                vga.S3.Reg6B = (byte) val;
                break;
            default:
                Log.logging(Log.LogTypes.VGAMISC, Log.LogServerities.Normal,
                        "VGA:S3:CRTC:Write to illegal index %2X", reg);
                break;
        }
    }

    // private int SVGA_S3_ReadCRTC(int reg, int iolen) {
    private int readCrtc(int reg, int iolen) {
        switch (reg) {
            case 0x24: /* attribute controller index (read only) */
            case 0x26:
                return ((vga.Attr.Disabled & 1) != 0 ? 0x00 : 0x20) | (vga.Attr.Index & 0x1f);
            case 0x2d: /* Extended Chip ID (high byte of PCI device ID) */
                return 0x88;
            case 0x2e: /* New Chip ID (low byte of PCI device ID) */
                return 0x11; // Trio64
            case 0x2f: /* Revision */
                return 0x00; // Trio64 (exact value?)
            // return 0x44; // Trio64 V+
            case 0x30: /* CR30 Chip ID/REV register */
                return 0xe1; // Trio+ dual byte
            case 0x31: /* CR31 Memory Configuration */
                // TODO mix in bits from baseaddress;
                return vga.S3.Reg31 & 0xff;
            case 0x35: /* CR35 CRT Register Lock */
                return (vga.S3.Reg35 & 0xff) | (vga.SVGA.BankRead & 0xf);
            case 0x36: /* CR36 Reset State Read 1 */
                return vga.S3.Reg36 & 0xff;
            case 0x37: /* Reset state read 2 */
                return 0x2b;
            case 0x38: /* CR38 Register Lock 1 */
                return vga.S3.RegLock1 & 0xff;
            case 0x39: /* CR39 Register Lock 2 */
                return vga.S3.RegLock2 & 0xff;
            case 0x3a:
                return vga.S3.Reg3A & 0xff;
            case 0x40: /* CR40 system config */
                return vga.S3.Reg40 & 0xff;
            case 0x41: /* CR40 system config */
                return vga.S3.Reg41 & 0xff;
            case 0x42: // not interlaced
                return 0x0d;
            case 0x43: /* CR43 Extended Mode */
                return (vga.S3.Reg43 & 0xff) | ((vga.Config.ScanLen >>> 6) & 0x4);
            case 0x45: /* Hardware cursor mode */
                vga.S3.HGC.BackStackPos = 0;
                vga.S3.HGC.ForeStackPos = 0;
                return (vga.S3.HGC.CurMode & 0xff) | 0xa0;
            case 0x46:
                return vga.S3.HGC.OriginX >>> 8;
            case 0x47: /* HGC orgX */
                return vga.S3.HGC.OriginX & 0xff;
            case 0x48:
                return vga.S3.HGC.OriginY >>> 8;
            case 0x49: /* HGC orgY */
                return vga.S3.HGC.OriginY & 0xff;
            case 0x4A: /* HGC foreground stack */
                return vga.S3.HGC.ForeStack[vga.S3.HGC.ForeStackPos & 0xff];
            case 0x4B: /* HGC background stack */
                return vga.S3.HGC.BackStack[vga.S3.HGC.BackStackPos & 0xff];
            case 0x50: // CR50 Extended System Control 1
                return vga.S3.Reg50 & 0xff;
            case 0x51: /* Extended System Control 2 */
                return ((vga.Config.DisplayStart >>> 16) & 3) | ((vga.SVGA.BankRead & 0x30) >>> 2)
                        | ((vga.Config.ScanLen & 0x300) >>> 4) | (vga.S3.Reg51 & 0xff);
            case 0x52: // CR52 Extended BIOS flags 1
                return vga.S3.Reg52 & 0xff;
            case 0x53:
                return vga.S3.ExtMemCtrl & 0xff;
            case 0x55: /* Extended Video DAC Control */
                return vga.S3.Reg55 & 0xff;
            case 0x58: /* Linear Address Window Control */
                return vga.S3.Reg58 & 0xff;
            case 0x59: /* Linear Address Window Position High */
                return vga.S3.LaWindow >>> 8;
            case 0x5a: /* Linear Address Window Position Low */
                return vga.S3.LaWindow & 0xff;
            case 0x5D: /* Extended Horizontal Overflow */
                return vga.S3.ExHorOverflow & 0xff;
            case 0x5e: /* Extended Vertical Overflow */
                return vga.S3.ExVerOverflow & 0xff;
            case 0x67: /* Extended Miscellaneous Control 2 */
                return vga.S3.MiscControl2;
            case 0x69: /* Extended System Control 3 */
                return 0xff & ((vga.Config.DisplayStart & 0x1f0000) >>> 16);
            case 0x6a: /* Extended System Control 4 */
                return vga.SVGA.BankRead & 0x7f;
            case 0x6b: // BIOS scatchpad: LFB address
                return vga.S3.Reg6B & 0xff;
            default:
                return 0x00;
        }
    }

    // private void SVGA_S3_WriteSEQ(int reg, int val, int iolen) {
    private void writeSeq(int reg, int val, int iolen) {
        if (reg > 0x8 && vga.S3.PLL.Lock != 0x6)
            return;
        switch (reg) {
            case 0x08:
                vga.S3.PLL.Lock = (byte) val;
                break;
            case 0x10: /* Memory PLL Data Low */
                vga.S3.MCLK.n = (byte) (val & 0x1f);
                vga.S3.MCLK.r = (byte) (val >>> 5);
                break;
            case 0x11: /* Memory PLL Data High */
                vga.S3.MCLK.m = (byte) (val & 0x7f);
                break;
            case 0x12: /* Video PLL Data Low */
                vga.S3.CLK[3].n = (byte) (val & 0x1f);
                vga.S3.CLK[3].r = (byte) (val >>> 5);
                break;
            case 0x13: /* Video PLL Data High */
                vga.S3.CLK[3].m = (byte) (val & 0x7f);
                break;
            case 0x15:
                vga.S3.PLL.Cmd = (byte) val;
                vga.startResize();
                break;
            default:
                Log.logging(Log.LogTypes.VGAMISC, Log.LogServerities.Normal,
                        "VGA:S3:SEQ:Write to illegal index %2X", reg);
                break;
        }
    }

    // private int SVGA_S3_ReadSEQ(int reg, int iolen) {
    private int readSEQ(int reg, int iolen) {
        /* S3 specific group */
        if (reg > 0x8 && vga.S3.PLL.Lock != 0x6) {
            if (reg < 0x1b)
                return 0;
            else
                return reg;
        }
        switch (reg) {
            case 0x08: /* PLL Unlock */
                return 0xff & vga.S3.PLL.Lock;
            case 0x10: /* Memory PLL Data Low */
                return vga.S3.MCLK.n != 0 || ((vga.S3.MCLK.r & 0xff) << 5) != 0 ? 1 : 0;
            case 0x11: /* Memory PLL Data High */
                return 0xff & vga.S3.MCLK.m;
            case 0x12: /* Video PLL Data Low */
                return vga.S3.CLK[3].n != 0 || ((vga.S3.CLK[3].r & 0xff) << 5) != 0 ? 1 : 0;
            case 0x13: /* Video Data High */
                return 0xff & vga.S3.CLK[3].m;
            case 0x15:
                return 0xff & vga.S3.PLL.Cmd;
            default:
                Log.logging(Log.LogTypes.VGAMISC, Log.LogServerities.Normal,
                        "VGA:S3:SEQ:Read from illegal index %2X", reg);
                return 0;
        }
    }

    // private int SVGA_S3_GetClock() {
    private int getClock() {
        int clock = (vga.MiscOutput >>> 2) & 3;
        if (clock == 0)
            clock = 25175000;
        else if (clock == 1)
            clock = 28322000;
        else
            // clock=1000*S3_CLOCK(vga.s3.clk[clock].m,vga.s3.clk[clock].n,vga.s3.clk[clock].r);
            clock = 1000 * ((VGA.S3_CLOCK_REF * ((0xff & vga.S3.CLK[clock].m) + 2))
                    / (((0xff & vga.S3.CLK[clock].n) + 2) * (1 << (0xff & vga.S3.CLK[clock].r))));
        /* Check for dual transfer, master clock/2 */
        if ((vga.S3.PLL.Cmd & 0x10) != 0)
            clock /= 2;
        return clock;
    }

    // private boolean SVGA_S3_HWCursorActive() {
    private boolean activeHWCursor() {
        return (vga.S3.HGC.CurMode & 0x1) != 0;
    }

    // private boolean SVGA_S3_AcceptsMode(int mode) {
    private boolean acceptsMode(int mode) {
        return INT10Mode.videoModeMemSize(mode) < vga.VMemSize;
    }

    private void setupS3TrioSVGA() {

    }
}
