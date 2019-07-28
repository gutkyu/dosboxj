package org.gutkyu.dosboxj.dos.keyboardlayout;

import org.gutkyu.dosboxj.dos.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import org.gutkyu.dosboxj.*;
import org.gutkyu.dosboxj.cpu.*;
import org.gutkyu.dosboxj.interrupt.bios.*;
import org.gutkyu.dosboxj.interrupt.int10.*;
import org.gutkyu.dosboxj.misc.*;
import org.gutkyu.dosboxj.util.*;
import org.gutkyu.dosboxj.hardware.memory.*;
import org.gutkyu.dosboxj.hardware.video.*;
import org.gutkyu.dosboxj.dos.system.drive.*;


public final class KeyboardLayout implements Disposable {

    public KeyboardLayout() {
        for (int i = 0; i < currentLayoutPlanes.length; i++) {
            currentLayoutPlanes[i] = new LayoutPlane();
        }
        this.reset();
        languageCodes = null;
        useForeignLayout = false;
        currentKeyboardFileName = "none";
    }

    private void dispose(boolean disposing) {
        if (disposing) {
        }

        if (languageCodes != null) {
            for (int i = 0; i < languageCodeCount; i++)
                languageCodes[i] = null;
            languageCodes = null;
        }
    }

    public void dispose() {
        if (languageCodes != null) {
            for (int i = 0; i < languageCodeCount; i++)
                languageCodes[i] = null;
            languageCodes = null;
        }

    }

    private SeekableByteChannel openDosboxFile(String name) {

        LocalDrive ldp = null;
        // try to build dos name
        if (DOSMain.makeFullName(name, DOSSystem.DOS_PATHLENGTH)) {
            String fullName = DOSMain.returnedFullName;
            int drive = DOSMain.returnedFullNameDrive;
            try {
                DOSDrive drv = DOSMain.Drives[drive];
                // try to open file on mounted drive first
                if (drv instanceof LocalDrive) {
                    ldp = (LocalDrive) drv;
                    SeekableByteChannel tmpfile =
                            ldp.getSystemFileChannel(fullName, StandardOpenOption.READ);
                    if (tmpfile != null)
                        return tmpfile;
                }
            } catch (Exception e) {
                // todo 오류 발생 처리 추가
                Log.logging(Log.LogTypes.BIOS, Log.LogServerities.Error,
                        "openDosboxFile( %s ) error", name);
                return null;
            }
        }
        try {
            return Files.newByteChannel(Paths.get(name), StandardOpenOption.READ);
        } catch (Exception e) {
            // todo 오류 발생 처리 추가
            Log.logging(Log.LogTypes.BIOS, Log.LogServerities.Error, "openDosboxFile( %s ) error",
                    name);
        }
        return null;
    }

    // read in a codepage from a .cpi-file
    private static byte[] cpiBuf = new byte[65536];

    public int readCodePageFile(String codepageFileName, int codepageId) {
        String CdPgFilename = null;
        CdPgFilename = codepageFileName.toString();
        if (CdPgFilename == "none")
            return DOSMain.KEYB_NOERROR;

        if (codepageId == DOSMain.DOS.LoadedCodepage)
            return DOSMain.KEYB_NOERROR;

        if (CdPgFilename == "auto") {
            // select matching .cpi-file for specified codepage
            switch (codepageId) {
                case 437:
                case 850:
                case 852:
                case 853:
                case 857:
                case 858:
                    CdPgFilename = "EGA.CPI";
                    break;
                case 775:
                case 859:
                case 1116:
                case 1117:
                    CdPgFilename = "EGA2.CPI";
                    break;
                case 771:
                case 772:
                case 808:
                case 855:
                case 866:
                case 872:
                    CdPgFilename = "EGA3.CPI";
                    break;
                case 848:
                case 849:
                case 1125:
                case 1131:
                case 61282:
                    CdPgFilename = "EGA4.CPI";
                    break;
                case 737:
                case 851:
                case 869:
                    CdPgFilename = "EGA5.CPI";
                    break;
                case 113:
                case 899:
                case 59829:
                case 60853:
                    CdPgFilename = "EGA6.CPI";
                    break;
                case 58152:
                case 58210:
                case 59234:
                case 60258:
                case 62306:
                    CdPgFilename = "EGA7.CPI";
                    break;
                case 770:
                case 773:
                case 774:
                case 777:
                case 778:
                    CdPgFilename = "EGA8.CPI";
                    break;
                case 860:
                case 861:
                case 863:
                case 865:
                    CdPgFilename = "EGA9.CPI";
                    break;
                case 667:
                case 668:
                case 790:
                case 867:
                case 991:
                case 57781:
                    CdPgFilename = "EGA10.CPI";
                    break;
                default:
                    Log.logMsg("No matching cpi file for codepage %i", codepageId);
                    return DOSMain.KEYB_INVALIDCPFILE;
            }
        }

        int startPos;
        int numberOfCodePages;

        String nbuf = CdPgFilename;
        SeekableByteChannel tempFile = openDosboxFile(nbuf);
        if (tempFile == null) {
            int strsz = nbuf.length();
            if (strsz > 0) {
                char plc = Character.toUpperCase(nbuf.charAt(strsz - 1));
                if (plc == 'I') {
                    // try CPX-extension as well
                    nbuf = nbuf.substring(0, strsz - 1) + 'X';
                    tempFile = openDosboxFile(nbuf);
                } else if (plc == 'X') {
                    // try CPI-extension as well
                    nbuf = nbuf.substring(0, strsz - 1) + 'I';
                    tempFile = openDosboxFile(nbuf);
                }
            }
        }

        // static byte cpi_buf[65536];
        int cpiBufSize = 0, cpxDataSize = 0;;
        boolean upxFound = false;
        short foundAtPos = 5;
        if (tempFile == null) {
            // check if build-in codepage is available
            switch (codepageId) {
                case 437:
                case 850:
                case 852:
                case 853:
                case 857:
                case 858: {
                    byte[] fontCpx = Resources.get("DOSCodePageFontEGA.cpx");
                    for (int bct = 0; bct < 6322; bct++)
                        cpiBuf[bct] = fontCpx[bct];
                    cpiBufSize = 6322;
                    break;
                }
                case 771:
                case 772:
                case 808:
                case 855:
                case 866:
                case 872: {
                    byte[] fontCpx = Resources.get("DOSCodePageFontEGA3.cpx");
                    for (int bct = 0; bct < 5455; bct++)
                        cpiBuf[bct] = fontCpx[bct];
                    cpiBufSize = 5455;
                    break;
                }
                case 737:
                case 851:
                case 869: {
                    byte[] fontCpx = Resources.get("DOSCodePageFontEGA5.cpx");
                    for (int bct = 0; bct < 5720; bct++)
                        cpiBuf[bct] = fontCpx[bct];
                    cpiBufSize = 5720;
                    break;
                }
                default:
                    return DOSMain.KEYB_INVALIDCPFILE;
                // break;
            }
            upxFound = true;
            foundAtPos = 0x29;
            cpxDataSize = cpiBufSize;
        } else {
            ByteBuffer rb = ByteBuffer.wrap(cpiBuf, 0, 5);
            try {
                int dr = tempFile.read(rb);
                // check if file is valid
                if (dr < 5) {
                    Log.logging(Log.LogTypes.BIOS, Log.LogServerities.Error,
                            "Codepage file %s invalid", CdPgFilename);
                    return DOSMain.KEYB_INVALIDCPFILE;
                }
                // check if non-compressed cpi file
                if ((cpiBuf[0] != (byte) 0xff) || (cpiBuf[1] != 0x46) || (cpiBuf[2] != 0x4f)
                        || (cpiBuf[3] != 0x4e) || (cpiBuf[4] != 0x54)) {
                    // check if dr-dos custom cpi file
                    if ((cpiBuf[0] == 0x7f) && (cpiBuf[1] != 0x44) && (cpiBuf[2] != 0x52)
                            && (cpiBuf[3] != 0x46) && (cpiBuf[4] != 0x5f)) {
                        Log.logging(Log.LogTypes.BIOS, Log.LogServerities.Error,
                                "Codepage file %s has unsupported DR-DOS format", CdPgFilename);
                        return DOSMain.KEYB_INVALIDCPFILE;
                    }
                    // check if compressed cpi file
                    byte nextByte = 0;
                    ByteBuffer rb1 = ByteBuffer.allocate(1);
                    for (int i = 0; i < 100; i++) {
                        tempFile.read(rb1);
                        nextByte = rb1.get();
                        foundAtPos++;
                        while (nextByte == 0x55) {
                            tempFile.read(rb1);
                            nextByte = rb1.get();
                            foundAtPos++;
                            if (nextByte == 0x50) {
                                tempFile.read(rb1);
                                nextByte = rb1.get();
                                foundAtPos++;
                                if (nextByte == 0x58) {
                                    tempFile.read(rb1);
                                    nextByte = rb1.get();
                                    foundAtPos++;
                                    if (nextByte == 0x21) {
                                        // read version ID
                                        tempFile.read(rb1);
                                        nextByte = rb1.get();
                                        foundAtPos++;
                                        foundAtPos++;
                                        upxFound = true;
                                        break;
                                    }
                                }
                            }
                        }
                        if (upxFound)
                            break;
                    }
                    if (!upxFound) {
                        Log.logging(Log.LogTypes.BIOS, Log.LogServerities.Error,
                                "Codepage file %s invalid: %x", CdPgFilename, cpiBuf[0]);
                        return DOSMain.KEYB_INVALIDCPFILE;
                    } else {
                        if (nextByte < 10)
                            Support.exceptionExit(
                                    "UPX-compressed cpi file, but upx-version too old");

                        // read in compressed CPX-file
                        tempFile.position(0);
                        rb = ByteBuffer.wrap(cpiBuf, 0, 65536);
                        cpxDataSize = tempFile.read(rb);
                    }
                } else {
                    // standard uncompressed cpi-file
                    tempFile.position(0);
                    rb = ByteBuffer.wrap(cpiBuf, 0, 65536);
                    cpiBufSize = tempFile.read(rb);
                }
            } catch (Exception e) {
                Log.logging(Log.LogTypes.BIOS, Log.LogServerities.Error,
                        "Codepage file %s invalid: file read error, %s", CdPgFilename,
                        e.getMessage());
                return DOSMain.KEYB_INVALIDCPFILE;
            }
        }

        if (upxFound) {
            if (cpxDataSize > 0xfe00)
                Support.exceptionExit("Size of cpx-compressed data too big");

            foundAtPos += 19;
            // prepare for direct decompression
            cpiBuf[foundAtPos] = (byte) 0xcb;

            int seg = 0;
            int size = 0x1500;
            if (!DOSMain.tryAllocateMemory(size))
                Support.exceptionExit("Not enough free low memory to unpack data");
            seg = DOSMain.returnedAllocateMemorySeg;
            size = DOSMain.returnedAllocateMemoryBlock;

            Memory.blockWrite((seg << 4) + 0x100, cpiBuf, 0, cpxDataSize);

            // setup segments
            int saveDS = Register.segValue(Register.SEG_NAME_DS);
            int saveES = Register.segValue(Register.SEG_NAME_ES);
            int saveSS = Register.segValue(Register.SEG_NAME_SS);
            int saveESP = Register.getRegESP();
            Register.segSet16(Register.SEG_NAME_DS, seg);
            Register.segSet16(Register.SEG_NAME_ES, seg);
            Register.segSet16(Register.SEG_NAME_SS, 0xffff & (seg + 0x1000));
            Register.setRegESP(0xfffe);

            // let UPX unpack the file
            Callback.runRealFar(seg, 0x100);

            Register.segSet16(Register.SEG_NAME_DS, saveDS);
            Register.segSet16(Register.SEG_NAME_ES, saveES);
            Register.segSet16(Register.SEG_NAME_SS, saveSS);
            Register.setRegESP(saveESP);

            // get unpacked content
            Memory.blockRead((seg << 4) + 0x100, cpiBuf, 0, 65536);
            cpiBufSize = 65536;

            DOSMain.freeMemory(seg);
        }

        // ByteConvert bc = new ByteConvert();
        long idx = 0;

        // start_pos = MEMORY.host_readd(ref cpi_buf, 0x13);
        idx = 0x13;
        startPos = ByteConv.getInt(cpiBuf, (int) idx);

        // number_of_codepages = MEMORY.host_readw(ref cpi_buf, start_pos);
        idx = startPos;
        numberOfCodePages = ByteConv.getShort(cpiBuf, (int) idx);
        startPos += 4;

        // search if codepage is provided by file
        for (int testCodePage = 0; testCodePage < numberOfCodePages; testCodePage++) {
            int deviceType, fontCodePage, fontType;

            // device type can be display/printer (only the first is supported)
            // device_type = MEMORY.host_readw(ref cpi_buf, start_pos + 0x04);
            idx = startPos + 0x04;
            deviceType = ByteConv.getShort(cpiBuf, (int) idx);
            // font_codepage = MEMORY.host_readw(ref cpi_buf, start_pos + 0x0e);
            idx = startPos + 0x0e;
            fontCodePage = ByteConv.getShort(cpiBuf, (int) idx);

            int fontDataHeaderPt;
            // font_data_header_pt = MEMORY.host_readd(ref cpi_buf, start_pos + 0x16);
            idx = startPos + 0x16;
            fontDataHeaderPt = ByteConv.getInt(cpiBuf, (int) idx);

            // font_type = MEMORY.host_readw(ref cpi_buf, font_data_header_pt);
            idx = fontDataHeaderPt;
            fontType = ByteConv.getShort(cpiBuf, (int) idx);

            if ((deviceType == 0x0001) && (fontType == 0x0001) && (fontCodePage == codepageId)) {
                // valid/matching codepage found

                int numberOfFonts, fontDataLength;
                // number_of_fonts = MEMORY.host_readw(ref cpi_buf, font_data_header_pt + 0x02);
                // font_data_length = MEMORY.host_readw(ref cpi_buf, font_data_header_pt + 0x04);
                idx = fontDataHeaderPt + 0x02;
                numberOfFonts = ByteConv.getShort(cpiBuf, (int) idx);
                idx = fontDataHeaderPt + 0x04;
                fontDataLength = ByteConv.getShort(cpiBuf, (int) idx);

                boolean fontChanged = false;
                int fontDataStart = fontDataHeaderPt + 0x06;

                // load all fonts if possible
                for (int currentFont = 0; currentFont < numberOfFonts; currentFont++) {
                    int fontHeight = 0xff & cpiBuf[fontDataStart];
                    fontDataStart += 6;
                    if (fontHeight == 0x10) {
                        // 16x8 font
                        int font16pt = Memory.real2Phys(INT10.int10.RomFont16);
                        for (int i = 0; i < 256 * 16; i++) {
                            Memory.physWriteB(font16pt + i, cpiBuf[fontDataStart + i]);
                        }
                        fontChanged = true;
                    } else if (fontHeight == 0x0e) {
                        // 14x8 font
                        int font14pt = Memory.real2Phys(INT10.int10.RomFont14);
                        for (int i = 0; i < 256 * 14; i++) {
                            Memory.physWriteB(font14pt + i, cpiBuf[fontDataStart + i]);
                        }
                        fontChanged = true;
                    } else if (fontHeight == 0x08) {
                        // 8x8 fonts
                        int font8pt = Memory.real2Phys(INT10.int10.RomFont8First);
                        for (int i = 0; i < 128 * 8; i++) {
                            Memory.physWriteB(font8pt + i, cpiBuf[fontDataStart + i]);
                        }
                        font8pt = Memory.real2Phys(INT10.int10.RomFont8Second);
                        for (int i = 0; i < 128 * 8; i++) {
                            Memory.physWriteB(font8pt + i, cpiBuf[fontDataStart + i + 128 * 8]);
                        }
                        fontChanged = true;
                    }
                    fontDataStart += fontHeight * 256;
                }

                Log.logging(Log.LogTypes.BIOS, Log.LogServerities.Normal,
                        "Codepage %i successfully loaded", codepageId);

                // set codepage entries
                DOSMain.DOS.LoadedCodepage = codepageId & 0xffff;

                // update font if necessary
                if (fontChanged && (INT10Mode.CurMode.Type == VGAModes.TEXT)
                        && (DOSBox.isEGAVGAArch())) {
                    INT10.reloadFont();
                }
                INT10.setupRomMemoryChecksum();

                return DOSMain.KEYB_NOERROR;
            }
            // start_pos = MEMORY.host_readd(ref cpi_buf, start_pos);
            idx = startPos;
            startPos = ByteConv.getInt(cpiBuf, (int) idx);
            startPos += 2;
        }

        Log.logging(Log.LogTypes.BIOS, Log.LogServerities.Error, "Codepage %i not found",
                codepageId);

        return DOSMain.KEYB_INVALIDCPFILE;
    }

    private static byte[] readBuf = new byte[65535];

    public int extractCodePage(String keyboardFileName) {
        if (keyboardFileName == "none")
            return 437;

        int readBufSize = 0;
        int startPos = 5;
        ByteBuffer klData = null;

        SeekableByteChannel tempfile = openDosboxFile(keyboardFileName + ".kl");
        if (tempfile == null) {
            // try keyboard layout libraries next
            if ((startPos = readKCLFile("keyboard.sys", keyboardFileName, true)) != 0) {
                tempfile = openDosboxFile("keyboard.sys");
            } else if ((startPos = readKCLFile("keybrd2.sys", keyboardFileName, true)) != 0) {
                tempfile = openDosboxFile("keybrd2.sys");
            } else if ((startPos = readKCLFile("keybrd3.sys", keyboardFileName, true)) != 0) {
                tempfile = openDosboxFile("keybrd3.sys");
            } else if ((startPos = readKCLFile("keyboard.sys", keyboardFileName, false)) != 0) {
                tempfile = openDosboxFile("keyboard.sys");
            } else if ((startPos = readKCLFile("keybrd2.sys", keyboardFileName, false)) != 0) {
                tempfile = openDosboxFile("keybrd2.sys");
            } else if ((startPos = readKCLFile("keybrd3.sys", keyboardFileName, false)) != 0) {
                tempfile = openDosboxFile("keybrd3.sys");
            } else if ((klData =
                    readKCLData("DOSKeyboardLayout1", keyboardFileName, true)) != null) {
                startPos = klData.position();
                klData.get(readBuf, 2, klData.limit());
            } else if ((klData =
                    readKCLData("DOSKeyboardLayout2", keyboardFileName, true)) != null) {
                startPos = klData.position();
                klData.get(readBuf, 2, klData.limit());
            } else if ((klData =
                    readKCLData("DOSKeyboardLayout3", keyboardFileName, true)) != null) {
                startPos = klData.position();
                klData.get(readBuf, 2, klData.limit());
            } else if ((klData =
                    readKCLData("DOSKeyboardLayout1", keyboardFileName, false)) != null) {
                startPos = klData.position();
                klData.get(readBuf, 2, klData.limit());
            } else if ((klData =
                    readKCLData("DOSKeyboardLayout2", keyboardFileName, false)) != null) {
                startPos = klData.position();
                klData.get(readBuf, 2, klData.limit());
            } else if ((klData =
                    readKCLData("DOSKeyboardLayout3", keyboardFileName, false)) != null) {
                startPos = klData.position();
                klData.get(readBuf, 2, klData.limit());
            } else {
                startPos = 0;
                Log.logging(Log.LogTypes.BIOS, Log.LogServerities.Error,
                        "Keyboard layout file %s not found", keyboardFileName);
                return 437;
            }
            if (tempfile != null) {
                ByteBuffer rb = ByteBuffer.wrap(readBuf, 0, 65535);
                try {
                    tempfile.position(startPos + 2);
                    readBufSize = tempfile.read(rb);
                    tempfile.close();
                } catch (Exception e) {
                    // todo 오류 발생 처리 추가
                    Log.logging(Log.LogTypes.BIOS, Log.LogServerities.Error,
                            "Keyboard layout file %s access error", keyboardFileName);
                    return 437;
                }
            }
            startPos = 0;
        } else {
            // check ID-bytes of file
            ByteBuffer rb = ByteBuffer.wrap(readBuf, 0, 4);

            try {
                int dr = tempfile.read(rb);
                if ((dr < 4) || (readBuf[0] != 0x4b) || (readBuf[1] != 0x4c)
                        || (readBuf[2] != 0x46)) {
                    Log.logging(Log.LogTypes.BIOS, Log.LogServerities.Error,
                            "Invalid keyboard layout file %s", keyboardFileName);
                    return 437;
                }

                tempfile.position(0);
                rb = ByteBuffer.wrap(readBuf, 0, 65535);
                readBufSize = tempfile.read(rb);
                tempfile.close();
            } catch (Exception e) {
                // todo 오류 발생 처리 추가
                Log.logging(Log.LogTypes.BIOS, Log.LogServerities.Error,
                        "Keyboard layout file %s access error", keyboardFileName);
                return 437;
            }
        }

        int dataLen, subMappings;
        dataLen = 0xff & readBuf[startPos++];

        startPos += dataLen; // start_pos==absolute position of KeybCB block

        subMappings = 0xff & readBuf[startPos];

        // ByteBuffer bc = ByteBuffer.wrap(read_buf);
        int idx = 0;
        // check all submappings and use them if general submapping or same codepage submapping
        for (int subMap = 0; (subMap < subMappings); subMap++) {
            int subMapCP;

            // read codepage of submapping
            // submap_cp = MEMORY.host_readw(ref read_buf, (int)(start_pos + 0x14 + sub_map * 8));
            idx = startPos + 0x14 + subMap * 8;
            subMapCP = ByteConv.getShort(readBuf, (int) idx);
            if (subMapCP != 0)
                return subMapCP;
        }
        return 437;
    }

    // read in a keyboard layout from a .kl-file
    public int ReadKeyboardFile(String keyboardFileName, int reqCP) {
        return this.readKeyboardFile(keyboardFileName, -1, reqCP);
    }

    // CTRL-ALT-F2 switches between foreign and US-layout using this function
    /*
     * static void switch_keyboard_layout(boolean pressed) { if (!pressed) return; if
     * (loaded_layout) loaded_layout->switch_foreign_layout(); }
     */

    // call layout_key to apply the current language layout
    // public boolean LayoutKey(int key, byte flags1, byte flags2, byte flags3) {
    public boolean LayoutKey(int key, int flags1, int flags2, int flags3) {
        if (key > BIOS.MAX_SCAN_CODE)
            return false;
        if (!this.useForeignLayout)
            return false;

        boolean isSpecialPair = (currentLayout[key * layoutPages + layoutPages - 1] & 0x80) == 0x80;

        if ((((flags1 & usedLockModifiers) & 0x7c) == 0) && ((flags3 & 2) == 0)) {
            // check if shift/caps is active:
            // (left_shift OR right_shift) XOR (key_affected_by_caps AND caps_locked)
            if (((((flags1 & 2) >>> 1) | (flags1 & 1))
                    ^ (((currentLayout[key * layoutPages + layoutPages - 1] & 0x40)
                            & (flags1 & 0x40)) >>> 6)) != 0) {
                // shift plane
                if (currentLayout[key * layoutPages + 1] != 0) {
                    // check if command-bit is set for shift plane
                    boolean is_command =
                            (currentLayout[key * layoutPages + layoutPages - 2] & 2) != 0;
                    if (this.mapKey(key, currentLayout[key * layoutPages + 1], is_command,
                            isSpecialPair))
                        return true;
                }
            } else {
                // normal plane
                if (currentLayout[key * layoutPages] != 0) {
                    // check if command-bit is set for normal plane
                    boolean isCommand =
                            (currentLayout[key * layoutPages + layoutPages - 2] & 1) != 0;
                    if (this.mapKey(key, currentLayout[key * layoutPages], isCommand,
                            isSpecialPair))
                        return true;
                }
            }
        }

        // calculate current flags
        int currentFlags = 0xffff & ((flags1 & 0x7f) | (((flags2 & 3) | (flags3 & 0xc)) << 8));
        if ((flags1 & 3) != 0)
            currentFlags |= 0x4000; // either shift key active
        if ((flags3 & 2) != 0)
            currentFlags |= 0x1000; // e0 prefixed

        // check all planes if flags fit
        for (int cplane = 0; cplane < additionalPlanes; cplane++) {
            int reqFlags = currentLayoutPlanes[cplane].requiredFlags;
            int reqUserFlags = currentLayoutPlanes[cplane].requiredUserFlags;
            // test flags
            if (((currentFlags & reqFlags) == reqFlags)
                    && ((userKeys & reqUserFlags) == reqUserFlags)
                    && ((currentFlags & currentLayoutPlanes[cplane].forbiddenFlags) == 0)
                    && ((userKeys & currentLayoutPlanes[cplane].forbiddenUserFlags) == 0)) {
                // remap key
                if (currentLayout[key * layoutPages + 2 + cplane] != 0) {
                    // check if command-bit is set for this plane
                    boolean isCommand =
                            ((currentLayout[key * layoutPages + layoutPages - 2] >>> (cplane + 2))
                                    & 1) != 0;
                    if (this.mapKey(key, currentLayout[key * layoutPages + 2 + cplane], isCommand,
                            isSpecialPair))
                        return true;
                } else
                    break; // abort plane checking
            }
        }

        if (diacriticsCharacter > 0) {
            // ignore state-changing keys
            switch (key) {
                case 0x1d: /* Ctrl Pressed */
                case 0x2a: /* Left Shift Pressed */
                case 0x36: /* Right Shift Pressed */
                case 0x38: /* Alt Pressed */
                case 0x3a: /* Caps Lock */
                case 0x45: /* Num Lock */
                case 0x46: /* Scroll Lock */
                    break;
                default:
                    if (diacriticsCharacter - 200 >= diacriticsEntries) {
                        diacriticsCharacter = 0;
                        return true;
                    }
                    int diacriticsStart = 0;
                    // search start of subtable
                    for (int i = 0; i < diacriticsCharacter - 200; i++)
                        diacriticsStart +=
                                0xffff & ((0xff & diacritics[diacriticsStart + 1]) * 2 + 2);

                    BIOSKeyboard.addKeyToBuffer(
                            0xffff & ((key << 8) | (0xff & diacritics[diacriticsStart])));
                    diacriticsCharacter = 0;
                    break;
            }
        }

        return false;
    }

    public KeyboardLayout returnedSwitchKBLCreatedLayout;
    public int returnedSwitchKBLTriedCP;

    public int trySwitchKeyboardLayout(String newLayout) {

        if (!newLayout.regionMatches(true, 0, "US", 0, 2)) {
            // switch to a foreign layout
            int newlen = newLayout.length();

            boolean languageCodeFound = false;
            // check if language code is present in loaded foreign layout
            for (int i = 0; i < languageCodeCount; i++) {
                if (newLayout.regionMatches(true, 0, languageCodes[i], 0, newlen)) {
                    languageCodeFound = true;
                    break;
                }
            }

            if (languageCodeFound) {
                if (!this.useForeignLayout) {
                    // switch to foreign layout
                    this.useForeignLayout = true;
                    diacriticsCharacter = 0;
                    Log.logging(Log.LogTypes.BIOS, Log.LogServerities.Normal,
                            "Switched to layout %s", newLayout);
                }
            } else {
                KeyboardLayout tempLayout = new KeyboardLayout();
                int reqCodePage = tempLayout.extractCodePage(newLayout);
                returnedSwitchKBLTriedCP = reqCodePage;
                int kErrCode = tempLayout.ReadKeyboardFile(newLayout, reqCodePage);
                if (kErrCode != 0) {
                    tempLayout.dispose();
                    tempLayout = null;
                    return kErrCode;
                }
                // ...else keyboard layout loaded successfully, change codepage accordingly
                kErrCode = tempLayout.readCodePageFile("auto", reqCodePage);
                if (kErrCode != 0) {
                    tempLayout.dispose();
                    tempLayout = null;
                    return kErrCode;
                }
                // Everything went fine, switch to new layout
                returnedSwitchKBLCreatedLayout = tempLayout;
            }
        } else if (this.useForeignLayout) {
            // switch to the US layout
            this.useForeignLayout = false;
            diacriticsCharacter = 0;
            Log.logging(Log.LogTypes.BIOS, Log.LogServerities.Normal, "Switched to US layout");
        }
        return DOSMain.KEYB_NOERROR;
    }

    public void switchForeignLayout() {
        this.useForeignLayout = !this.useForeignLayout;
        diacriticsCharacter = 0;
        if (this.useForeignLayout)
            Log.logging(Log.LogTypes.BIOS, Log.LogServerities.Normal, "Switched to foreign layout");
        else
            Log.logging(Log.LogTypes.BIOS, Log.LogServerities.Normal, "Switched to US layout");
    }

    public String getLayoutName() {
        // get layout name (language ID or null if default layout)
        if (useForeignLayout) {
            if (currentKeyboardFileName != "none") {
                return currentKeyboardFileName;
            }
        }
        return null;
    }

    public String mainLanguageCode() {
        if (languageCodes != null) {
            return languageCodes[0];
        }
        return null;
    }



    private static final int layoutPages = 12;
    private short[] currentLayout = new short[(BIOS.MAX_SCAN_CODE + 1) * layoutPages];

    private final class LayoutPlane {
        public int requiredFlags;// uint16
        public int forbiddenFlags;// uint16
        public int requiredUserFlags;// uint16
        public int forbiddenUserFlags;// uint16
    }

    private LayoutPlane[] currentLayoutPlanes = new LayoutPlane[layoutPages - 4];
    private int additionalPlanes, usedLockModifiers;

    // diacritics table
    private byte[] diacritics = new byte[2048];
    private int diacriticsEntries;// uint16
    private int diacriticsCharacter;// uint16
    private int userKeys;// uint16

    private String currentKeyboardFileName;
    private boolean useForeignLayout;

    // language code storage used when switching layouts
    private String[] languageCodes;
    private int languageCodeCount;

    private void reset() {
        for (int i = 0; i < (BIOS.MAX_SCAN_CODE + 1) * layoutPages; i++)
            currentLayout[i] = 0;
        for (int i = 0; i < layoutPages - 4; i++) {
            currentLayoutPlanes[i].requiredFlags = 0;
            currentLayoutPlanes[i].forbiddenFlags = 0xffff;
            currentLayoutPlanes[i].requiredUserFlags = 0;
            currentLayoutPlanes[i].forbiddenUserFlags = 0xffff;
        }
        usedLockModifiers = 0x0f;
        diacriticsEntries = 0; // no diacritics loaded
        diacriticsCharacter = 0;
        userKeys = 0; // all userkeys off
        languageCodeCount = 0;
    }

    private void readKeyboardFile(int specificLayout) {
        if (currentKeyboardFileName != "none")
            this.readKeyboardFile(currentKeyboardFileName, specificLayout,
                    DOSMain.DOS.LoadedCodepage);
    }

    private int readKeyboardFile(String keyboardFileName, int specificLayout,
            int requested_codepage) {
        this.reset();

        if (specificLayout == -1)
            currentKeyboardFileName = keyboardFileName;
        if (keyboardFileName == "none")
            return DOSMain.KEYB_NOERROR;

        // static byte read_buf[65535];
        int readBufSize, readBufIdx, bytesRead;
        int startIdx = 5;

        String nbuf = null;
        readBufSize = 0;
        ByteBuffer klData = null;
        nbuf = keyboardFileName + ".kl";
        SeekableByteChannel tempfile = openDosboxFile(nbuf);
        if (tempfile == null) {
            // try keyboard layout libraries next
            if ((startIdx = readKCLFile("keyboard.sys", keyboardFileName, true)) != 0) {
                tempfile = openDosboxFile("keyboard.sys");
            } else if ((startIdx = readKCLFile("keybrd2.sys", keyboardFileName, true)) != 0) {
                tempfile = openDosboxFile("keybrd2.sys");
            } else if ((startIdx = readKCLFile("keybrd3.sys", keyboardFileName, true)) != 0) {
                tempfile = openDosboxFile("keybrd3.sys");
            } else if ((startIdx = readKCLFile("keyboard.sys", keyboardFileName, false)) != 0) {
                tempfile = openDosboxFile("keyboard.sys");
            } else if ((startIdx = readKCLFile("keybrd2.sys", keyboardFileName, false)) != 0) {
                tempfile = openDosboxFile("keybrd2.sys");
            } else if ((startIdx = readKCLFile("keybrd3.sys", keyboardFileName, false)) != 0) {
                tempfile = openDosboxFile("keybrd3.sys");
            } else if ((klData =
                    readKCLData("DOSKeyboardLayout1", keyboardFileName, true)) != null) {
                startIdx = klData.position();
                klData.get(readBuf, 2, klData.limit());
            } else if ((klData =
                    readKCLData("DOSKeyboardLayout2", keyboardFileName, true)) != null) {
                startIdx = klData.position();
                klData.get(readBuf, 2, klData.limit());
            } else if ((klData =
                    readKCLData("DOSKeyboardLayout3", keyboardFileName, true)) != null) {
                startIdx = klData.position();
                klData.get(readBuf, 2, klData.limit());
            } else if ((klData =
                    readKCLData("DOSKeyboardLayout1", keyboardFileName, false)) != null) {
                startIdx = klData.position();
                klData.get(readBuf, 2, klData.limit());
            } else if ((klData =
                    readKCLData("DOSKeyboardLayout2", keyboardFileName, false)) != null) {
                startIdx = klData.position();
                klData.get(readBuf, 2, klData.limit());
            } else if ((klData =
                    readKCLData("DOSKeyboardLayout3", keyboardFileName, false)) != null) {
                startIdx = klData.position();
                klData.get(readBuf, 2, klData.limit());
            } else {
                Log.logging(Log.LogTypes.BIOS, Log.LogServerities.Error,
                        "Keyboard layout file %s not found", keyboardFileName);
                return DOSMain.KEYB_FILENOTFOUND;
            }
            if (tempfile != null) {
                try {
                    tempfile.position(startIdx + 2);
                    ByteBuffer rb = ByteBuffer.wrap(readBuf, 0, 65535);
                    readBufSize = tempfile.read(rb);
                    tempfile.close();
                } catch (Exception e) {
                    // todo 오류 발생 처리 추가
                    Log.logging(Log.LogTypes.BIOS, Log.LogServerities.Error,
                            "readKeyboardFile( %s ) access error", keyboardFileName);
                    return DOSMain.KEYB_INVALIDFILE;
                }
            }
            startIdx = 0;
        } else {
            // check ID-bytes of file
            ByteBuffer rb = ByteBuffer.wrap(readBuf, 0, 4);
            try {
                int dr = tempfile.read(rb);
                if ((dr < 4) || (readBuf[0] != 0x4b) || (readBuf[1] != 0x4c)
                        || (readBuf[2] != 0x46)) {
                    Log.logging(Log.LogTypes.BIOS, Log.LogServerities.Error,
                            "Invalid keyboard layout file %s", keyboardFileName);
                    return DOSMain.KEYB_INVALIDFILE;
                }

                tempfile.position(0);
                rb = ByteBuffer.wrap(readBuf, 0, 65535);
                readBufSize = tempfile.read(rb);
                tempfile.close();
            }

            catch (Exception e) {
                // todo 오류 발생 처리 추가
                Log.logging(Log.LogTypes.BIOS, Log.LogServerities.Error,
                        "Keyboard layout file %s access error", keyboardFileName);
                return DOSMain.KEYB_INVALIDFILE;
            }
        }

        int dataLen, subMappings;
        dataLen = 0xff & readBuf[startIdx++];

        languageCodes = new String[dataLen];
        languageCodeCount = 0;
        // get all language codes for this layout
        // StringBuilder sb = new StringBuilder();
        // List<char> sb = new ArrayList<char>();
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < dataLen;) {
            sb.setLength(0);
            i += 2;
            for (; i < dataLen;) {
                char lcode = (char) readBuf[startIdx + i];
                i++;
                if (lcode == ',')
                    break;
                // sb.Append(lcode);
                sb.append(lcode);
            }
            // language_codes[language_code_count] = sb.toString();
            languageCodes[languageCodeCount] = sb.toString();
            languageCodeCount++;
        }

        startIdx += dataLen; // start_pos==absolute position of KeybCB block

        subMappings = 0xff & readBuf[startIdx];
        additionalPlanes = 0xff & readBuf[startIdx + 1];

        // four pages always occupied by normal,shift,flags,commandbits
        if (additionalPlanes > (layoutPages - 4))
            additionalPlanes = (layoutPages - 4);

        long idx = 0;
        // ByteBuffer bc = ByteBuffer.wrap(read_buf);
        // seek to plane descriptor
        readBufIdx = startIdx + 0x14 + subMappings * 8;
        for (int cplane = 0; cplane < additionalPlanes; cplane++) {
            int planeFlags;

            // get required-flags (shift/alt/ctrl-states etc.)
            // plane_flags = MEMORY.host_readw(ref read_buf, read_buf_pos);
            idx = readBufIdx;
            planeFlags = ByteConv.getShort(readBuf, (int) idx);
            readBufIdx += 2;
            currentLayoutPlanes[cplane].requiredFlags = planeFlags;
            usedLockModifiers |= planeFlags & 0x70;
            // get forbidden-flags
            // plane_flags = MEMORY.host_readw(ref read_buf, read_buf_pos);
            idx = readBufIdx;
            planeFlags = ByteConv.getShort(readBuf, (int) idx);
            readBufIdx += 2;
            currentLayoutPlanes[cplane].forbiddenFlags = planeFlags;

            // get required-userflags
            // plane_flags = MEMORY.host_readw(ref read_buf, read_buf_pos);
            idx = readBufIdx;
            planeFlags = ByteConv.getShort(readBuf, (int) idx);
            readBufIdx += 2;
            currentLayoutPlanes[cplane].requiredUserFlags = planeFlags;
            // get forbidden-userflags
            // plane_flags = MEMORY.host_readw(ref read_buf, read_buf_pos);
            idx = readBufIdx;
            planeFlags = ByteConv.getShort(readBuf, (int) idx);
            readBufIdx += 2;
            currentLayoutPlanes[cplane].forbiddenUserFlags = planeFlags;
        }

        boolean foundMatchingLayout = false;
        // check all submappings and use them if general submapping or same codepage submapping
        for (int subMap = 0; (subMap < subMappings) && (!foundMatchingLayout); subMap++) {
            int subMapCP, tableOffset;

            if ((subMap != 0) && (specificLayout != -1))
                subMap = specificLayout & 0xffff;

            // read codepage of submapping
            // submap_cp = MEMORY.host_readw(ref read_buf, (int)(start_pos + 0x14 + sub_map * 8));
            idx = startIdx + 0x14 + subMap * 8;
            subMapCP = ByteConv.getShort(readBuf, (int) idx);
            if ((subMapCP != 0) && (subMapCP != requested_codepage) && (specificLayout == -1))
                continue; // skip nonfitting submappings

            if (subMapCP == requested_codepage)
                foundMatchingLayout = true;

            // get table offset
            // table_offset = MEMORY.host_readw(ref read_buf, (int)(start_pos + 0x18 + sub_map *
            // 8));
            idx = startIdx + 0x18 + subMap * 8;
            tableOffset = ByteConv.getShort(readBuf, (int) idx);
            diacriticsEntries = 0;
            if (tableOffset != 0) {
                // process table
                int i, j;
                for (i = 0; i < 2048;) {
                    if (readBuf[startIdx + tableOffset + i] == 0)
                        break; // end of table
                    diacriticsEntries++;
                    i += 0xffff & ((readBuf[startIdx + tableOffset + i + 1] & 0xff) * 2 + 2);
                }
                // copy diacritics table
                for (j = 0; j <= i; j++)
                    diacritics[j] = readBuf[startIdx + tableOffset + j];
            }


            // get table offset
            tableOffset = Memory.hostReadW(readBuf, 0xffff & (startIdx + 0x16 + subMap * 8));
            if (tableOffset == 0)
                continue; // non-present table

            readBufIdx = startIdx + tableOffset;

            bytesRead = readBufSize - readBufIdx;

            // process submapping table
            for (int i = 0; i < bytesRead;) {
                int scan = 0xff & readBuf[readBufIdx++];
                if (scan == 0)
                    break;
                int scanLength = 0xff & ((readBuf[readBufIdx] & 7) + 1);// length of data
                                                                        // struct
                readBufIdx += 2;
                i += 3;
                if (((scan & 0x7f) <= BIOS.MAX_SCAN_CODE) && (scanLength > 0)) {
                    // add all available mappings
                    for (int addmap = 0; addmap < scanLength; addmap++) {
                        if (addmap > additionalPlanes + 2)
                            break;
                        int charptr = readBufIdx
                                + addmap * ((readBuf[readBufIdx - 2] & 0x80) != 0 ? 2 : 1);
                        int kchar = 0xff & readBuf[charptr];

                        if (kchar != 0) { // key remapped
                            if ((readBuf[readBufIdx - 2] & 0x80) != 0)
                                kchar |= (0xff & readBuf[charptr + 1]) << 8; // scancode/char pair
                            // overwrite mapping
                            currentLayout[scan * layoutPages + addmap] = (short) kchar;
                            // clear command bit
                            currentLayout[scan * layoutPages + layoutPages - 2] &=
                                    0xffff & (~(1 << addmap));
                            // add command bit
                            currentLayout[scan * layoutPages + layoutPages - 2] |=
                                    0xffff & ((0xff & readBuf[readBufIdx - 1]) & (1 << addmap));
                        }
                    }

                    currentLayout[scan * layoutPages + layoutPages - 1] = readBuf[readBufIdx - 2]; // flags
                    if ((readBuf[readBufIdx - 2] & 0x80) != 0)
                        scanLength *= 2; // granularity flag (S)
                }
                i += scanLength; // advance pointer
                readBufIdx += scanLength;
            }
            if (specificLayout == subMap)
                break;
        }

        if (foundMatchingLayout) {
            if (specificLayout == -1)
                Log.logging(Log.LogTypes.BIOS, Log.LogServerities.Normal,
                        "Keyboard layout %s successfully loaded", keyboardFileName);
            else
                Log.logging(Log.LogTypes.BIOS, Log.LogServerities.Normal,
                        "Keyboard layout %s (%i) successfully loaded", keyboardFileName,
                        specificLayout);
            this.useForeignLayout = true;
            return DOSMain.KEYB_NOERROR;
        }

        Log.logging(Log.LogTypes.BIOS, Log.LogServerities.Error,
                "No matching keyboard layout found in %s", keyboardFileName);

        // reset layout data (might have been changed by general layout)
        this.reset();

        return DOSMain.KEYB_LAYOUTNOTFOUND;
    }

    private boolean mapKey(int key, short layoutedKey, boolean isCommand, boolean isKeypair) {
        if (isCommand) {
            int keyCommand = layoutedKey & 0xff;
            // check if diacritics-command
            if ((keyCommand >= 200) && (keyCommand < 235)) {
                // diacritics command
                diacriticsCharacter = keyCommand;
                if (diacriticsCharacter - 200 >= diacriticsEntries)
                    diacriticsCharacter = 0;
                return true;
            } else if ((keyCommand >= 120) && (keyCommand < 140)) {
                // switch layout command
                this.readKeyboardFile(keyCommand - 119);
                return true;
            } else if ((keyCommand >= 180) && (keyCommand < 188)) {
                // switch user key off
                userKeys &= 0xffff & (~(1 << (keyCommand - 180)));
                return true;
            } else if ((keyCommand >= 188) && (keyCommand < 196)) {
                // switch user key on
                userKeys |= 0xffff & (1 << (keyCommand - 188));
                return true;
            } else if (keyCommand == 160)
                return true; // nop command
        } else {
            // non-command
            if (diacriticsCharacter > 0) {
                if (diacriticsCharacter - 200 >= diacriticsEntries)
                    diacriticsCharacter = 0;
                else {
                    int diacriticsStart = 0;
                    // search start of subtable
                    for (short i = 0; i < diacriticsCharacter - 200; i++)
                        diacriticsStart += ((0xff & diacritics[diacriticsStart + 1]) * 2 + 2);

                    int diacriticsLength = 0xff & diacritics[diacriticsStart + 1];
                    diacriticsStart += 2;
                    diacriticsCharacter = 0; // reset

                    // search scancode
                    for (int i = 0; i < diacriticsLength; i++) {
                        if (diacritics[diacriticsStart + i * 2] == (byte) layoutedKey) {
                            // add diacritics to keybuf
                            BIOSKeyboard.addKeyToBuffer(0xffff & ((key << 8)
                                    | (0xff & diacritics[diacriticsStart + i * 2 + 1])));
                            return true;
                        }
                    }
                    // add standard-diacritics to keybuf
                    BIOSKeyboard.addKeyToBuffer(
                            0xffff & ((key << 8) | (0xff & diacritics[diacriticsStart - 2])));
                }
            }

            // add remapped key to keybuf
            if (isKeypair)
                BIOSKeyboard.addKeyToBuffer(layoutedKey);
            else
                BIOSKeyboard.addKeyToBuffer(0xffff & ((key << 8) | (layoutedKey & 0xff)));

            return true;
        }
        return false;
    }

    private static byte[] rbuf = new byte[8192];

    private int readKCLFile(String kclFileName, String layoutId, boolean firstIdOnly) {
        SeekableByteChannel tempfile = openDosboxFile(kclFileName);
        if (tempfile == null)
            return 0;


        try {
            // check ID-bytes of file
            ByteBuffer rb = ByteBuffer.wrap(rbuf, 0, 7);
            int dr = tempfile.read(rb);
            if ((dr < 7) || (rbuf[0] != 0x4b) || (rbuf[1] != 0x43) || (rbuf[2] != 0x46)) {
                tempfile.close();
                return 0;
            }

            tempfile.position(7 + (0xff & rbuf[6]));

            for (;;) {
                int curPos = (int) tempfile.position();
                rb = ByteBuffer.wrap(rbuf, 0, 5);
                dr = tempfile.read(rb);
                if (dr < 5)
                    break;
                int len = Memory.hostReadW(rbuf, 0);

                int dataLen = 0xff & rbuf[2];

                CStringPt lngCodes = CStringPt.create(258);
                tempfile.position(tempfile.position() - 2);
                // get all language codes for this layout
                for (int i = 0; i < dataLen;) {
                    rb = ByteBuffer.wrap(rbuf, 0, 2);
                    tempfile.read(rb);
                    int lcnum = Memory.hostReadW(rbuf, 0);
                    i += 2;
                    int lcpos = 0;
                    for (; i < dataLen;) {
                        rb = ByteBuffer.wrap(rbuf, 0, 1);
                        tempfile.read(rb);
                        i++;
                        if (((char) rbuf[0]) == ',')
                            break;
                        lngCodes.set(lcpos++, (char) rbuf[0]);
                    }
                    lngCodes.set(lcpos, (char) 0);
                    if (lngCodes.equalsIgnoreCase(layoutId)) {
                        // language ID found in file, return file position
                        tempfile.close();
                        return curPos;
                    }
                    if (firstIdOnly)
                        break;
                    if (lcnum != 0) {
                        // sprintf(&lng_codes[lcpos],"%d",lcnum);
                        CStringPt pt = CStringPt.clone(lngCodes, lcpos);
                        CStringPt.copy(String.valueOf(lcnum), pt);
                        if (lngCodes.equalsIgnoreCase(layoutId)) {
                            // language ID found in file, return file position
                            return curPos;
                        }
                    }
                }
                tempfile.position(curPos + 3 + len);
            }

            tempfile.close();
        } catch (Exception e) {
            // todo 오류 발생 처리 추가
            Log.logging(Log.LogTypes.KEYBOARD, Log.LogServerities.Error,
                    "Keyboard layout file %s access error", kclFileName);
        }
        return 0;
    }

    private int readKCLData(byte[] kclData, int kclDataSize, String layoutId, boolean firstIdOnly) {
        // check ID-bytes
        if ((kclData[0] != 0x4b) || (kclData[1] != 0x43) || (kclData[2] != 0x46)) {
            return 0;
        }

        int dpos = 7 + (0xff & kclData[6]);
        long idx;
        for (;;) {
            if (dpos + 5 > kclDataSize)
                break;
            int curPos = dpos;
            // short len = MEMORY.host_readw(ref kcl_data, dpos);
            idx = dpos;
            int len = ByteConv.getShort(kclData, (int) idx);
            int dataLen = 0xff & kclData[dpos + 2];
            dpos += 5;

            CStringPt lngCodes = CStringPt.create(258);
            // get all language codes for this layout
            for (int i = 0; i < dataLen;) {
                // short lcnum = MEMORY.host_readw(ref kcl_data, dpos - 2);
                idx = dpos;
                int lcnum = ByteConv.getShort(kclData, (int) idx);
                i += 2;
                int lcpos = 0;
                for (; i < dataLen;) {
                    if (dpos + 1 > kclDataSize)
                        break;
                    char lc = (char) kclData[dpos];
                    dpos++;
                    i++;
                    if (lc == ',')
                        break;
                    lngCodes.set(lcpos++, lc);
                }
                lngCodes.set(lcpos, (char) 0);
                if (lngCodes.equalsIgnoreCase(layoutId)) {
                    // language ID found in file, return file position
                    return curPos;
                }
                if (firstIdOnly)
                    break;
                if (lcnum != 0) {
                    // sprintf(&lng_codes[lcpos],"%d",lcnum);
                    CStringPt pt = CStringPt.clone(lngCodes, lcpos);
                    CStringPt.copy(String.valueOf(lcnum), pt);
                    if (lngCodes.equalsIgnoreCase(layoutId)) {
                        // language ID found in file, return file position
                        return curPos;
                    }
                }
                dpos += 2;
            }
            dpos = curPos + 3 + len;
        }
        return 0;
    }

    private ByteBuffer readKCLData(String defaultKLName, String layoutId, boolean firstIdOnly) {
        // load default keyboard layout
        ByteBuffer kclData =
                ByteBuffer.wrap(Resources.get(defaultKLName)).order(ByteOrder.LITTLE_ENDIAN);
        if (kclData == null || kclData.limit() == 0)
            return null;
        int kclDataSize = kclData.limit();
        // check ID-bytes
        if ((kclData.get(0) != 0x4b) || (kclData.get(1) != 0x43) || (kclData.get(2) != 0x46)) {
            return null;
        }

        int dpos = 7 + kclData.get(6);
        int idx;
        for (;;) {
            if (dpos + 5 > kclDataSize)
                break;
            int curPos = dpos;
            // short len = MEMORY.host_readw(ref kcl_data, dpos);
            idx = dpos;
            int len = 0xffff & kclData.getShort(idx);
            int dataLen = 0xff & kclData.get(dpos + 2);
            dpos += 5;

            CStringPt lngCodes = CStringPt.create(258);
            // get all language codes for this layout
            for (int i = 0; i < dataLen;) {
                // short lcnum = MEMORY.host_readw(ref kcl_data, dpos - 2);
                idx = dpos;
                int lcnum = 0xffff & kclData.getShort(idx);
                i += 2;
                int lcpos = 0;
                for (; i < dataLen;) {
                    if (dpos + 1 > kclDataSize)
                        break;
                    char lc = (char) kclData.get(dpos);
                    dpos++;
                    i++;
                    if (lc == ',')
                        break;
                    lngCodes.set(lcpos++, lc);
                }
                lngCodes.set(lcpos, (char) 0);
                if (lngCodes.equalsIgnoreCase(layoutId)) {
                    // language ID found in file, return file position
                    kclData.position(curPos);
                    return kclData;
                }
                if (firstIdOnly)
                    break;
                if (lcnum != 0) {
                    // sprintf(&lng_codes[lcpos],"%d",lcnum);
                    CStringPt pt = CStringPt.clone(lngCodes, lcpos);
                    CStringPt.copy(String.valueOf(lcnum), pt);
                    if (lngCodes.equalsIgnoreCase(layoutId)) {
                        // language ID found in file, return file position
                        kclData.position(curPos);
                        return kclData;
                    }
                }
                dpos += 2;
            }
            dpos = curPos + 3 + len;
        }
        return null;
    }

}
