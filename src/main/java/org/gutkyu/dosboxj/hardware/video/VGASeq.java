package org.gutkyu.dosboxj.hardware.video;

import org.gutkyu.dosboxj.util.*;
import org.gutkyu.dosboxj.DOSBox;
import org.gutkyu.dosboxj.hardware.io.IO;

public final class VGASeq {
    public int Index;// byte
    public int Reset;// byte
    public int ClockingMode;// byte
    public int MapMask;// byte
    public int CharacterMapSelect;// byte
    public int MemoryMode;// byte

    public VGA vga;

    public VGASeq(VGA vga) {
        this.vga = vga;
    }

    private int readP3C4(int port, int iolen) {
        return Index;
    }

    private void writeP3C4(int port, int val, int iolen) {
        Index = 0xff & val;
    }

    private void writeP3C5(int port, int val, int iolen) {
        // Log.LOG_MSG("SEQ WRITE reg %X val %X",seq(index),val);
        switch (Index) {
            case 0: /* Reset */
                Reset = 0xff & val;
                break;
            case 1: /* Clocking Mode */
                if (val != ClockingMode) {
                    // don't resize if only the screen off bit was changed
                    if ((val & (~0x20)) != (ClockingMode & (~0x20))) {
                        ClockingMode = 0xff & val;
                        vga.startResize();
                    } else {
                        ClockingMode = 0xff & val;
                    }
                    if ((val & 0x20) != 0)
                        vga.Attr.Disabled |= 0x2;
                    else
                        vga.Attr.Disabled &= ((byte) ~0x2);
                }
                /*
                 * TODO Figure this out :) 0 If set character clocks are 8 dots wide, else 9. 2 If
                 * set loads video serializers every other character clock cycle, else every one. 3
                 * If set the Dot Clock is Master Clock/2, else same as Master Clock (See 3C2h bit
                 * 2-3). (Doubles pixels). Note: on some SVGA chipsets this bit also affects the
                 * Sequencer mode. 4 If set loads video serializers every fourth character clock
                 * cycle, else every one. 5 if set turns off screen and gives all memory cycles to
                 * the CPU interface.
                 */
                break;
            case 2: /* Map Mask */
                MapMask = 0xff & (val & 15);
                vga.Config.FullMapMask = vga.FillTable[val & 15];
                vga.Config.FullNotMapMask = ~vga.Config.FullMapMask;
                /*
                 * 0 Enable writes to plane 0 if set 1 Enable writes to plane 1 if set 2 Enable
                 * writes to plane 2 if set 3 Enable writes to plane 3 if set
                 */
                break;
            case 3: /* Character Map Select */
            {
                CharacterMapSelect = 0xff & val;
                int font1 = 0xff & ((val & 0x3) << 1);
                if (DOSBox.isVGAArch())
                    font1 |= 0xff & ((val & 0x10) >>> 4);
                vga.Draw.FontTablesIdx[0] = font1 * 8 * 1024;
                int font2 = 0xff & (((val & 0xc) >>> 1));
                if (DOSBox.isVGAArch())
                    font2 |= 0xff & ((val & 0x20) >>> 5);
                vga.Draw.FontTablesIdx[1] = font2 * 8 * 1024;
            }
                /*
                 * 0,1,4 Selects VGA Character Map (0..7) if bit 3 of the character attribute is
                 * clear. 2,3,5 Selects VGA Character Map (0..7) if bit 3 of the character attribute
                 * is set. Note: Character Maps are placed as follows: Map 0 at 0k, 1 at 16k, 2 at
                 * 32k, 3: 48k, 4: 8k, 5: 24k, 6: 40k, 7: 56k
                 */
                break;
            case 4: /* Memory Mode */
                /*
                 * 0 Set if in an alphanumeric mode, clear in graphics modes. 1 Set if more than
                 * 64kbytes on the adapter. 2 Enables Odd/Even addressing mode if set. Odd/Even mode
                 * places all odd bytes in plane 1&3, and all even bytes in plane 0&2. 3 If set
                 * address bit 0-1 selects video memory planes (256 color mode), rather than the Map
                 * Mask and Read Map Select Registers.
                 */
                MemoryMode = 0xff & val;
                if (DOSBox.isVGAArch()) {
                    /* Changing this means changing the VGA Memory Read/Write Handler */
                    if ((val & 0x08) != 0)
                        vga.Config.Chained = true;
                    else
                        vga.Config.Chained = false;
                    vga.setupHandlers();
                }
                break;
            default:
                if (vga.SVGADrv.WriteP3C5 != null) {
                    vga.SVGADrv.WriteP3C5.exec(Index, val, iolen);
                } else {
                    Log.logging(Log.LogTypes.VGAMISC, Log.LogServerities.Normal,
                            "VGA:SEQ:Write to illegal index %2X", Index);
                }
                break;
        }
    }

    private int readP3C5(int port, int iolen) {
        // Log.LOG_MSG("VGA:SEQ:Read from index %2X",seq(index));
        switch (Index) {
            case 0: /* Reset */
                return Reset;
            case 1: /* Clocking Mode */
                return ClockingMode;
            case 2: /* Map Mask */
                return MapMask;
            case 3: /* Character Map Select */
                return CharacterMapSelect;
            case 4: /* Memory Mode */
                return MemoryMode;
            default:
                if (vga.SVGADrv.ReadP3C5 != null)
                    return vga.SVGADrv.ReadP3C5.exec(Index, iolen);
                break;
        }
        return 0;
    }

    public void setup() {
        if (DOSBox.isEGAVGAArch()) {
            IO.registerWriteHandler(0x3c4, this::writeP3C4, IO.IO_MB);
            IO.registerWriteHandler(0x3c5, this::writeP3C5, IO.IO_MB);
            if (DOSBox.isVGAArch()) {
                IO.registerReadHandler(0x3c4, this::readP3C4, IO.IO_MB);
                IO.registerReadHandler(0x3c5, this::readP3C5, IO.IO_MB);
            }
        }
    }

}
