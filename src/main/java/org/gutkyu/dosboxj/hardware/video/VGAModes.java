package org.gutkyu.dosboxj.hardware.video;



public enum VGAModes {
    CGA2(0), CGA4(1), EGA(2), VGA(3), LIN4(4), LIN8(5), LIN15(6), LIN16(7), LIN32(8), TEXT(
            9), HERC_GFX(10), HERC_TEXT(
                    11), CGA16(12), TANDY2(13), TANDY4(14), TANDY16(15), TANDY_TEXT(16), ERROR(17);

    private int value = 0;

    private VGAModes(int value) {
        this.value = value;
    }

    public int toValue() {
        return value;
    }
}
