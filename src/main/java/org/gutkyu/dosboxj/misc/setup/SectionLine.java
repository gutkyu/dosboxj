package org.gutkyu.dosboxj.misc.setup;

import java.io.BufferedWriter;
import java.io.IOException;

public final class SectionLine extends Section {
    public String data = "";

    public SectionLine(String sectionName) {
        super(sectionName);
    }

    // ~SectionLine() { ExecuteDestroy(true); }
    @Override
    public void handleInputline(String line) {
        data += line;
        data += "\n";
    }

    @Override
    public void printData(BufferedWriter fileWriter) throws IOException {
        fileWriter.write(data);
    }

    @Override
    public String getPropValue(String property) {
        return NO_SUCH_PROPERTY;
    }

}
