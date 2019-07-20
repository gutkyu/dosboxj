package org.gutkyu.dosboxj.util;

import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class FileExport {
    public static void write(String fileName, ByteBuffer input) {
        input.position(0);
        try (SeekableByteChannel chan =
                Files.newByteChannel(Paths.get(fileName), StandardOpenOption.CREATE,
                        StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
            chan.write(input);
        }catch(Exception e){
            e.printStackTrace();
        }

    }
}
