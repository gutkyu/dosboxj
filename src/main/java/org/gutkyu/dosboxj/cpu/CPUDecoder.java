package org.gutkyu.dosboxj.cpu;

public final class CPUDecoder {
    private CPUDecoderHandler handler;

    public CPUDecoder(CPUDecoderHandler handler) {
        this.handler = handler;
    }

    public int decode() {
        return this.handler.decode();
    }

    public boolean equals(CPUDecoder decoder) {
        return this.handler.equals(decoder.handler);
    }
}
