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
        for (int i = 0; i < _currentLayoutPlanes.length; i++) {
            _currentLayoutPlanes[i] = new LayoutPlane();
        }
        this.reset();
        _languageCodes = null;
        _useForeignLayout = false;
        currentKeyboardFileName = "none";
    }

    private void dispose(boolean disposing) {
        if (disposing) {
        }

        if (_languageCodes != null) {
            for (int i = 0; i < _languageCodeCount; i++)
                _languageCodes[i] = null;
            _languageCodes = null;
        }
    }

    public void dispose() {
        if (_languageCodes != null) {
            for (int i = 0; i < _languageCodeCount; i++)
                _languageCodes[i] = null;
            _languageCodes = null;
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
    private static byte[] cpi_buf = new byte[65536];

    public int readCodePageFile(String codepageFileName, int codepageId) {
        String cp_filename = null;
        cp_filename = codepageFileName.toString();
        if (cp_filename == "none")
            return DOSMain.KEYB_NOERROR;

        if (codepageId == DOSMain.DOS.LoadedCodepage)
            return DOSMain.KEYB_NOERROR;

        if (cp_filename == "auto") {
            // select matching .cpi-file for specified codepage
            switch (codepageId) {
                case 437:
                case 850:
                case 852:
                case 853:
                case 857:
                case 858:
                    cp_filename = "EGA.CPI";
                    break;
                case 775:
                case 859:
                case 1116:
                case 1117:
                    cp_filename = "EGA2.CPI";
                    break;
                case 771:
                case 772:
                case 808:
                case 855:
                case 866:
                case 872:
                    cp_filename = "EGA3.CPI";
                    break;
                case 848:
                case 849:
                case 1125:
                case 1131:
                case 61282:
                    cp_filename = "EGA4.CPI";
                    break;
                case 737:
                case 851:
                case 869:
                    cp_filename = "EGA5.CPI";
                    break;
                case 113:
                case 899:
                case 59829:
                case 60853:
                    cp_filename = "EGA6.CPI";
                    break;
                case 58152:
                case 58210:
                case 59234:
                case 60258:
                case 62306:
                    cp_filename = "EGA7.CPI";
                    break;
                case 770:
                case 773:
                case 774:
                case 777:
                case 778:
                    cp_filename = "EGA8.CPI";
                    break;
                case 860:
                case 861:
                case 863:
                case 865:
                    cp_filename = "EGA9.CPI";
                    break;
                case 667:
                case 668:
                case 790:
                case 867:
                case 991:
                case 57781:
                    cp_filename = "EGA10.CPI";
                    break;
                default:
                    Log.logMsg("No matching cpi file for codepage %i", codepageId);
                    return DOSMain.KEYB_INVALIDCPFILE;
            }
        }

        int start_pos;
        int number_of_codepages;

        String nbuf = cp_filename;
        SeekableByteChannel tempfile = openDosboxFile(nbuf);
        if (tempfile == null) {
            int strsz = nbuf.length();
            if (strsz > 0) {
                char plc = Character.toUpperCase(nbuf.charAt(strsz - 1));
                if (plc == 'I') {
                    // try CPX-extension as well
                    nbuf = nbuf.substring(0, strsz - 1) + 'X';
                    tempfile = openDosboxFile(nbuf);
                } else if (plc == 'X') {
                    // try CPI-extension as well
                    nbuf = nbuf.substring(0, strsz - 1) + 'I';
                    tempfile = openDosboxFile(nbuf);
                }
            }
        }

        // static byte cpi_buf[65536];
        int cpi_buf_size = 0, size_of_cpxdata = 0;;
        boolean upxfound = false;
        short found_at_pos = 5;
        if (tempfile == null) {
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
                        cpi_buf[bct] = fontCpx[bct];
                    cpi_buf_size = 6322;
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
                        cpi_buf[bct] = fontCpx[bct];
                    cpi_buf_size = 5455;
                    break;
                }
                case 737:
                case 851:
                case 869: {
                    byte[] fontCpx = Resources.get("DOSCodePageFontEGA5.cpx");
                    for (int bct = 0; bct < 5720; bct++)
                        cpi_buf[bct] = fontCpx[bct];
                    cpi_buf_size = 5720;
                    break;
                }
                default:
                    return DOSMain.KEYB_INVALIDCPFILE;
                // break;
            }
            upxfound = true;
            found_at_pos = 0x29;
            size_of_cpxdata = cpi_buf_size;
        } else {
            ByteBuffer rb = ByteBuffer.wrap(cpi_buf, 0, 5);
            try {
                int dr = tempfile.read(rb);
                // check if file is valid
                if (dr < 5) {
                    Log.logging(Log.LogTypes.BIOS, Log.LogServerities.Error,
                            "Codepage file %s invalid", cp_filename);
                    return DOSMain.KEYB_INVALIDCPFILE;
                }
                // check if non-compressed cpi file
                if ((cpi_buf[0] != 0xff) || (cpi_buf[1] != 0x46) || (cpi_buf[2] != 0x4f)
                        || (cpi_buf[3] != 0x4e) || (cpi_buf[4] != 0x54)) {
                    // check if dr-dos custom cpi file
                    if ((cpi_buf[0] == 0x7f) && (cpi_buf[1] != 0x44) && (cpi_buf[2] != 0x52)
                            && (cpi_buf[3] != 0x46) && (cpi_buf[4] != 0x5f)) {
                        Log.logging(Log.LogTypes.BIOS, Log.LogServerities.Error,
                                "Codepage file %s has unsupported DR-DOS format", cp_filename);
                        return DOSMain.KEYB_INVALIDCPFILE;
                    }
                    // check if compressed cpi file
                    byte next_byte = 0;
                    ByteBuffer rb1 = ByteBuffer.allocate(1);
                    for (int i = 0; i < 100; i++) {
                        tempfile.read(rb1);
                        next_byte = rb1.get();
                        found_at_pos++;
                        while (next_byte == 0x55) {
                            tempfile.read(rb1);
                            next_byte = rb1.get();
                            found_at_pos++;
                            if (next_byte == 0x50) {
                                tempfile.read(rb1);
                                next_byte = rb1.get();
                                found_at_pos++;
                                if (next_byte == 0x58) {
                                    tempfile.read(rb1);
                                    next_byte = rb1.get();
                                    found_at_pos++;
                                    if (next_byte == 0x21) {
                                        // read version ID
                                        tempfile.read(rb1);
                                        next_byte = rb1.get();
                                        found_at_pos++;
                                        found_at_pos++;
                                        upxfound = true;
                                        break;
                                    }
                                }
                            }
                        }
                        if (upxfound)
                            break;
                    }
                    if (!upxfound) {
                        Log.logging(Log.LogTypes.BIOS, Log.LogServerities.Error,
                                "Codepage file %s invalid: %x", cp_filename, cpi_buf[0]);
                        return DOSMain.KEYB_INVALIDCPFILE;
                    } else {
                        if (next_byte < 10)
                            Support.exceptionExit(
                                    "UPX-compressed cpi file, but upx-version too old");

                        // read in compressed CPX-file
                        tempfile.position(0);
                        rb = ByteBuffer.wrap(cpi_buf, 0, 65536);
                        size_of_cpxdata = tempfile.read(rb);
                    }
                } else {
                    // standard uncompressed cpi-file
                    tempfile.position(0);
                    rb = ByteBuffer.wrap(cpi_buf, 0, 65536);
                    cpi_buf_size = tempfile.read(rb);
                }
            } catch (Exception e) {
                Log.logging(Log.LogTypes.BIOS, Log.LogServerities.Error,
                        "Codepage file %s invalid: file read error, %s", cp_filename,
                        e.getMessage());
                return DOSMain.KEYB_INVALIDCPFILE;
            }
        }

        if (upxfound) {
            if (size_of_cpxdata > 0xfe00)
                Support.exceptionExit("Size of cpx-compressed data too big");

            found_at_pos += 19;
            // prepare for direct decompression
            cpi_buf[found_at_pos] = (byte) 0xcb;

            int seg = 0;
            int size = 0x1500;
            RefU32Ret refSize = new RefU32Ret(size);
            RefU32Ret refSeg = new RefU32Ret(seg);
            if (!DOSMain.allocateMemory(refSeg, refSize))
                Support.exceptionExit("Not enough free low memory to unpack data");
            seg = refSeg.U32;
            size = refSize.U32;

            Memory.blockWrite((seg << 4) + 0x100, cpi_buf, 0, size_of_cpxdata);

            // setup segments
            int save_ds = Register.segValue(Register.SEG_NAME_DS);
            int save_es = Register.segValue(Register.SEG_NAME_ES);
            int save_ss = Register.segValue(Register.SEG_NAME_SS);
            int save_esp = Register.getRegESP();
            Register.segSet16(Register.SEG_NAME_DS, seg);
            Register.segSet16(Register.SEG_NAME_ES, seg);
            Register.segSet16(Register.SEG_NAME_SS, 0xffff & (seg + 0x1000));
            Register.setRegESP(0xfffe);

            // let UPX unpack the file
            Callback.runRealFar(seg, 0x100);

            Register.segSet16(Register.SEG_NAME_DS, save_ds);
            Register.segSet16(Register.SEG_NAME_ES, save_es);
            Register.segSet16(Register.SEG_NAME_SS, save_ss);
            Register.setRegESP(save_esp);

            // get unpacked content
            Memory.blockRead((seg << 4) + 0x100, cpi_buf, 0, 65536);
            cpi_buf_size = 65536;

            DOSMain.freeMemory(seg);
        }

        // ByteConvert bc = new ByteConvert();
        long idx = 0;

        // start_pos = MEMORY.host_readd(ref cpi_buf, 0x13);
        idx = 0x13;
        start_pos = ByteConv.getInt(cpi_buf, (int) idx);

        // number_of_codepages = MEMORY.host_readw(ref cpi_buf, start_pos);
        idx = start_pos;
        number_of_codepages = ByteConv.getShort(cpi_buf, (int) idx);
        start_pos += 4;

        // search if codepage is provided by file
        for (int test_codepage = 0; test_codepage < number_of_codepages; test_codepage++) {
            int device_type, font_codepage, font_type;

            // device type can be display/printer (only the first is supported)
            // device_type = MEMORY.host_readw(ref cpi_buf, start_pos + 0x04);
            idx = start_pos + 0x04;
            device_type = ByteConv.getShort(cpi_buf, (int) idx);
            // font_codepage = MEMORY.host_readw(ref cpi_buf, start_pos + 0x0e);
            idx = start_pos + 0x0e;
            font_codepage = ByteConv.getShort(cpi_buf, (int) idx);

            int font_data_header_pt;
            // font_data_header_pt = MEMORY.host_readd(ref cpi_buf, start_pos + 0x16);
            idx = start_pos + 0x16;
            font_data_header_pt = ByteConv.getInt(cpi_buf, (int) idx);

            // font_type = MEMORY.host_readw(ref cpi_buf, font_data_header_pt);
            idx = font_data_header_pt;
            font_type = ByteConv.getShort(cpi_buf, (int) idx);

            if ((device_type == 0x0001) && (font_type == 0x0001) && (font_codepage == codepageId)) {
                // valid/matching codepage found

                int number_of_fonts, font_data_length;
                // number_of_fonts = MEMORY.host_readw(ref cpi_buf, font_data_header_pt + 0x02);
                // font_data_length = MEMORY.host_readw(ref cpi_buf, font_data_header_pt + 0x04);
                idx = font_data_header_pt + 0x02;
                number_of_fonts = ByteConv.getShort(cpi_buf, (int) idx);
                idx = font_data_header_pt + 0x04;
                font_data_length = ByteConv.getShort(cpi_buf, (int) idx);

                boolean font_changed = false;
                int font_data_start = font_data_header_pt + 0x06;

                // load all fonts if possible
                for (short current_font = 0; current_font < number_of_fonts; current_font++) {
                    byte font_height = cpi_buf[font_data_start];
                    font_data_start += 6;
                    if (font_height == 0x10) {
                        // 16x8 font
                        int font16pt = Memory.real2Phys(INT10.int10.RomFont16);
                        for (int i = 0; i < 256 * 16; i++) {
                            Memory.physWriteB(font16pt + i, cpi_buf[font_data_start + i]);
                        }
                        font_changed = true;
                    } else if (font_height == 0x0e) {
                        // 14x8 font
                        int font14pt = Memory.real2Phys(INT10.int10.RomFont14);
                        for (int i = 0; i < 256 * 14; i++) {
                            Memory.physWriteB(font14pt + i, cpi_buf[font_data_start + i]);
                        }
                        font_changed = true;
                    } else if (font_height == 0x08) {
                        // 8x8 fonts
                        int font8pt = Memory.real2Phys(INT10.int10.RomFont8First);
                        for (int i = 0; i < 128 * 8; i++) {
                            Memory.physWriteB(font8pt + i, cpi_buf[font_data_start + i]);
                        }
                        font8pt = Memory.real2Phys(INT10.int10.RomFont8Second);
                        for (int i = 0; i < 128 * 8; i++) {
                            Memory.physWriteB(font8pt + i, cpi_buf[font_data_start + i + 128 * 8]);
                        }
                        font_changed = true;
                    }
                    font_data_start += font_height * 256;
                }

                Log.logging(Log.LogTypes.BIOS, Log.LogServerities.Normal,
                        "Codepage %i successfully loaded", codepageId);

                // set codepage entries
                DOSMain.DOS.LoadedCodepage = codepageId & 0xffff;

                // update font if necessary
                if (font_changed && (INT10Mode.CurMode.Type == VGAModes.TEXT)
                        && (DOSBox.isEGAVGAArch())) {
                    INT10.reloadFont();
                }
                INT10.setupRomMemoryChecksum();

                return DOSMain.KEYB_NOERROR;
            }
            // start_pos = MEMORY.host_readd(ref cpi_buf, start_pos);
            idx = start_pos;
            start_pos = ByteConv.getInt(cpi_buf, (int) idx);
            start_pos += 2;
        }

        Log.logging(Log.LogTypes.BIOS, Log.LogServerities.Error, "Codepage %i not found",
                codepageId);

        return DOSMain.KEYB_INVALIDCPFILE;
    }

    private static byte[] read_buf = new byte[65535];

    public int extractCodePage(String keyboardFileName) {
        if (keyboardFileName == "none")
            return 437;

        int read_buf_size = 0;
        int start_pos = 5;
        ByteBuffer klData = null;

        SeekableByteChannel tempfile = openDosboxFile(keyboardFileName + ".kl");
        if (tempfile == null) {
            // try keyboard layout libraries next
            if ((start_pos = readKCLFile("keyboard.sys", keyboardFileName, true)) != 0) {
                tempfile = openDosboxFile("keyboard.sys");
            } else if ((start_pos = readKCLFile("keybrd2.sys", keyboardFileName, true)) != 0) {
                tempfile = openDosboxFile("keybrd2.sys");
            } else if ((start_pos = readKCLFile("keybrd3.sys", keyboardFileName, true)) != 0) {
                tempfile = openDosboxFile("keybrd3.sys");
            } else if ((start_pos = readKCLFile("keyboard.sys", keyboardFileName, false)) != 0) {
                tempfile = openDosboxFile("keyboard.sys");
            } else if ((start_pos = readKCLFile("keybrd2.sys", keyboardFileName, false)) != 0) {
                tempfile = openDosboxFile("keybrd2.sys");
            } else if ((start_pos = readKCLFile("keybrd3.sys", keyboardFileName, false)) != 0) {
                tempfile = openDosboxFile("keybrd3.sys");
            } else if ((klData =
                    readKCLData("DOSKeyboardLayout1", keyboardFileName, true)) != null) {
                start_pos = klData.position();
                klData.get(read_buf, 2, klData.limit());
            } else if ((klData =
                    readKCLData("DOSKeyboardLayout2", keyboardFileName, true)) != null) {
                start_pos = klData.position();
                klData.get(read_buf, 2, klData.limit());
            } else if ((klData =
                    readKCLData("DOSKeyboardLayout3", keyboardFileName, true)) != null) {
                start_pos = klData.position();
                klData.get(read_buf, 2, klData.limit());
            } else if ((klData =
                    readKCLData("DOSKeyboardLayout1", keyboardFileName, false)) != null) {
                start_pos = klData.position();
                klData.get(read_buf, 2, klData.limit());
            } else if ((klData =
                    readKCLData("DOSKeyboardLayout2", keyboardFileName, false)) != null) {
                start_pos = klData.position();
                klData.get(read_buf, 2, klData.limit());
            } else if ((klData =
                    readKCLData("DOSKeyboardLayout3", keyboardFileName, false)) != null) {
                start_pos = klData.position();
                klData.get(read_buf, 2, klData.limit());
            } else {
                start_pos = 0;
                Log.logging(Log.LogTypes.BIOS, Log.LogServerities.Error,
                        "Keyboard layout file %s not found", keyboardFileName);
                return 437;
            }
            if (tempfile != null) {
                ByteBuffer rb = ByteBuffer.wrap(read_buf, 0, 65535);
                try {
                    tempfile.position(start_pos + 2);
                    read_buf_size = tempfile.read(rb);
                    tempfile.close();
                } catch (Exception e) {
                    // todo 오류 발생 처리 추가
                    Log.logging(Log.LogTypes.BIOS, Log.LogServerities.Error,
                            "Keyboard layout file %s access error", keyboardFileName);
                    return 437;
                }
            }
            start_pos = 0;
        } else {
            // check ID-bytes of file
            ByteBuffer rb = ByteBuffer.wrap(read_buf, 0, 4);

            try {
                int dr = tempfile.read(rb);
                if ((dr < 4) || (read_buf[0] != 0x4b) || (read_buf[1] != 0x4c)
                        || (read_buf[2] != 0x46)) {
                    Log.logging(Log.LogTypes.BIOS, Log.LogServerities.Error,
                            "Invalid keyboard layout file %s", keyboardFileName);
                    return 437;
                }

                tempfile.position(0);
                rb = ByteBuffer.wrap(read_buf, 0, 65535);
                read_buf_size = tempfile.read(rb);
                tempfile.close();
            } catch (Exception e) {
                // todo 오류 발생 처리 추가
                Log.logging(Log.LogTypes.BIOS, Log.LogServerities.Error,
                        "Keyboard layout file %s access error", keyboardFileName);
                return 437;
            }
        }

        byte data_len, submappings;
        data_len = read_buf[start_pos++];

        start_pos += data_len; // start_pos==absolute position of KeybCB block

        submappings = read_buf[start_pos];

        // ByteBuffer bc = ByteBuffer.wrap(read_buf);
        int idx = 0;
        // check all submappings and use them if general submapping or same codepage submapping
        for (short sub_map = 0; (sub_map < submappings); sub_map++) {
            int submap_cp;

            // read codepage of submapping
            // submap_cp = MEMORY.host_readw(ref read_buf, (int)(start_pos + 0x14 + sub_map * 8));
            idx = start_pos + 0x14 + sub_map * 8;
            submap_cp = ByteConv.getShort(read_buf, (int) idx);
            if (submap_cp != 0)
                return submap_cp;
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
        if (!this._useForeignLayout)
            return false;

        boolean is_special_pair =
                (_currentLayout[key * _layoutPages + _layoutPages - 1] & 0x80) == 0x80;

        if ((((flags1 & _usedLockModifiers) & 0x7c) == 0) && ((flags3 & 2) == 0)) {
            // check if shift/caps is active:
            // (left_shift OR right_shift) XOR (key_affected_by_caps AND caps_locked)
            if (((((flags1 & 2) >>> 1) | (flags1 & 1))
                    ^ (((_currentLayout[key * _layoutPages + _layoutPages - 1] & 0x40)
                            & (flags1 & 0x40)) >>> 6)) != 0) {
                // shift plane
                if (_currentLayout[key * _layoutPages + 1] != 0) {
                    // check if command-bit is set for shift plane
                    boolean is_command =
                            (_currentLayout[key * _layoutPages + _layoutPages - 2] & 2) != 0;
                    if (this.mapKey(key, _currentLayout[key * _layoutPages + 1], is_command,
                            is_special_pair))
                        return true;
                }
            } else {
                // normal plane
                if (_currentLayout[key * _layoutPages] != 0) {
                    // check if command-bit is set for normal plane
                    boolean is_command =
                            (_currentLayout[key * _layoutPages + _layoutPages - 2] & 1) != 0;
                    if (this.mapKey(key, _currentLayout[key * _layoutPages], is_command,
                            is_special_pair))
                        return true;
                }
            }
        }

        // calculate current flags
        int current_flags = 0xffff & ((flags1 & 0x7f) | (((flags2 & 3) | (flags3 & 0xc)) << 8));
        if ((flags1 & 3) != 0)
            current_flags |= 0x4000; // either shift key active
        if ((flags3 & 2) != 0)
            current_flags |= 0x1000; // e0 prefixed

        // check all planes if flags fit
        for (short cplane = 0; cplane < _additionalPlanes; cplane++) {
            int req_flags = _currentLayoutPlanes[cplane].required_flags;
            int req_userflags = _currentLayoutPlanes[cplane].required_userflags;
            // test flags
            if (((current_flags & req_flags) == req_flags)
                    && ((_userKeys & req_userflags) == req_userflags)
                    && ((current_flags & _currentLayoutPlanes[cplane].forbidden_flags) == 0)
                    && ((_userKeys & _currentLayoutPlanes[cplane].forbidden_userflags) == 0)) {
                // remap key
                if (_currentLayout[key * _layoutPages + 2 + cplane] != 0) {
                    // check if command-bit is set for this plane
                    boolean is_command = ((_currentLayout[key * _layoutPages + _layoutPages
                            - 2] >>> (cplane + 2)) & 1) != 0;
                    if (this.mapKey(key, _currentLayout[key * _layoutPages + 2 + cplane],
                            is_command, is_special_pair))
                        return true;
                } else
                    break; // abort plane checking
            }
        }

        if (_diacriticsCharacter > 0) {
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
                    if (_diacriticsCharacter - 200 >= _diacriticsEntries) {
                        _diacriticsCharacter = 0;
                        return true;
                    }
                    int diacritics_start = 0;
                    // search start of subtable
                    for (short i = 0; i < _diacriticsCharacter - 200; i++)
                        diacritics_start += 0xffff & (diacritics[diacritics_start + 1] * 2 + 2);

                    BIOSKeyboard
                            .addKeyToBuffer(0xffff & ((key << 8) | diacritics[diacritics_start]));
                    _diacriticsCharacter = 0;
                    break;
            }
        }

        return false;
    }

    public static class RefKeyboardLayout {
        public KeyboardLayout KBLayout = null;
    }

    public int switchKeyboardLayout(String newLayout, RefKeyboardLayout refCreatedLayout,
            RefU32Ret refTriedCP) {

        if (!newLayout.regionMatches(true, 0, "US", 0, 2)) {
            // switch to a foreign layout
            int newlen = newLayout.length();

            boolean language_code_found = false;
            // check if language code is present in loaded foreign layout
            for (int i = 0; i < _languageCodeCount; i++) {
                if (newLayout.regionMatches(true, 0, _languageCodes[i], 0, newlen)) {
                    language_code_found = true;
                    break;
                }
            }

            if (language_code_found) {
                if (!this._useForeignLayout) {
                    // switch to foreign layout
                    this._useForeignLayout = true;
                    _diacriticsCharacter = 0;
                    Log.logging(Log.LogTypes.BIOS, Log.LogServerities.Normal,
                            "Switched to layout %s", newLayout);
                }
            } else {
                KeyboardLayout temp_layout = new KeyboardLayout();
                int req_codepage = temp_layout.extractCodePage(newLayout);
                refTriedCP.U32 = req_codepage;
                int kerrcode = temp_layout.ReadKeyboardFile(newLayout, req_codepage);
                if (kerrcode != 0) {
                    temp_layout.dispose();
                    temp_layout = null;
                    return kerrcode;
                }
                // ...else keyboard layout loaded successfully, change codepage accordingly
                kerrcode = temp_layout.readCodePageFile("auto", req_codepage);
                if (kerrcode != 0) {
                    temp_layout.dispose();
                    temp_layout = null;
                    return kerrcode;
                }
                // Everything went fine, switch to new layout
                refCreatedLayout.KBLayout = temp_layout;
            }
        } else if (this._useForeignLayout) {
            // switch to the US layout
            this._useForeignLayout = false;
            _diacriticsCharacter = 0;
            Log.logging(Log.LogTypes.BIOS, Log.LogServerities.Normal, "Switched to US layout");
        }
        return DOSMain.KEYB_NOERROR;
    }

    public void switchForeignLayout() {
        this._useForeignLayout = !this._useForeignLayout;
        _diacriticsCharacter = 0;
        if (this._useForeignLayout)
            Log.logging(Log.LogTypes.BIOS, Log.LogServerities.Normal, "Switched to foreign layout");
        else
            Log.logging(Log.LogTypes.BIOS, Log.LogServerities.Normal, "Switched to US layout");
    }

    public String getLayoutName() {
        // get layout name (language ID or null if default layout)
        if (_useForeignLayout) {
            if (currentKeyboardFileName != "none") {
                return currentKeyboardFileName;
            }
        }
        return null;
    }

    public String mainLanguageCode() {
        if (_languageCodes != null) {
            return _languageCodes[0];
        }
        return null;
    }



    private static final int _layoutPages = 12;
    private short[] _currentLayout = new short[(BIOS.MAX_SCAN_CODE + 1) * _layoutPages];

    private final class LayoutPlane {
        public int required_flags;// uint16
        public int forbidden_flags;// uint16
        public int required_userflags;// uint16
        public int forbidden_userflags;// uint16
    }

    private LayoutPlane[] _currentLayoutPlanes = new LayoutPlane[_layoutPages - 4];
    private byte _additionalPlanes, _usedLockModifiers;

    // diacritics table
    private byte[] diacritics = new byte[2048];
    private int _diacriticsEntries;// uint16
    private int _diacriticsCharacter;// uint16
    private int _userKeys;// uint16

    private String currentKeyboardFileName;
    private boolean _useForeignLayout;

    // language code storage used when switching layouts
    private String[] _languageCodes;
    private int _languageCodeCount;

    private void reset() {
        for (int i = 0; i < (BIOS.MAX_SCAN_CODE + 1) * _layoutPages; i++)
            _currentLayout[i] = 0;
        for (int i = 0; i < _layoutPages - 4; i++) {
            _currentLayoutPlanes[i].required_flags = 0;
            _currentLayoutPlanes[i].forbidden_flags = 0xffff;
            _currentLayoutPlanes[i].required_userflags = 0;
            _currentLayoutPlanes[i].forbidden_userflags = 0xffff;
        }
        _usedLockModifiers = 0x0f;
        _diacriticsEntries = 0; // no diacritics loaded
        _diacriticsCharacter = 0;
        _userKeys = 0; // all userkeys off
        _languageCodeCount = 0;
    }

    private void readKeyboardFile(int specific_layout) {
        if (currentKeyboardFileName != "none")
            this.readKeyboardFile(currentKeyboardFileName, specific_layout,
                    DOSMain.DOS.LoadedCodepage);
    }

    private int readKeyboardFile(String keyboardFileName, int specific_layout,
            int requested_codepage) {
        this.reset();

        if (specific_layout == -1)
            currentKeyboardFileName = keyboardFileName;
        if (keyboardFileName == "none")
            return DOSMain.KEYB_NOERROR;

        // static byte read_buf[65535];
        int read_buf_size, read_buf_pos, bytes_read;
        int start_pos = 5;

        String nbuf = null;
        read_buf_size = 0;
        ByteBuffer klData = null;
        nbuf = keyboardFileName + ".kl";
        SeekableByteChannel tempfile = openDosboxFile(nbuf);
        if (tempfile == null) {
            // try keyboard layout libraries next
            if ((start_pos = readKCLFile("keyboard.sys", keyboardFileName, true)) != 0) {
                tempfile = openDosboxFile("keyboard.sys");
            } else if ((start_pos = readKCLFile("keybrd2.sys", keyboardFileName, true)) != 0) {
                tempfile = openDosboxFile("keybrd2.sys");
            } else if ((start_pos = readKCLFile("keybrd3.sys", keyboardFileName, true)) != 0) {
                tempfile = openDosboxFile("keybrd3.sys");
            } else if ((start_pos = readKCLFile("keyboard.sys", keyboardFileName, false)) != 0) {
                tempfile = openDosboxFile("keyboard.sys");
            } else if ((start_pos = readKCLFile("keybrd2.sys", keyboardFileName, false)) != 0) {
                tempfile = openDosboxFile("keybrd2.sys");
            } else if ((start_pos = readKCLFile("keybrd3.sys", keyboardFileName, false)) != 0) {
                tempfile = openDosboxFile("keybrd3.sys");
            } else if ((klData =
                    readKCLData("DOSKeyboardLayout1", keyboardFileName, true)) != null) {
                start_pos = klData.position();
                klData.get(read_buf, 2, klData.limit());
            } else if ((klData =
                    readKCLData("DOSKeyboardLayout2", keyboardFileName, true)) != null) {
                start_pos = klData.position();
                klData.get(read_buf, 2, klData.limit());
            } else if ((klData =
                    readKCLData("DOSKeyboardLayout3", keyboardFileName, true)) != null) {
                start_pos = klData.position();
                klData.get(read_buf, 2, klData.limit());
            } else if ((klData =
                    readKCLData("DOSKeyboardLayout1", keyboardFileName, false)) != null) {
                start_pos = klData.position();
                klData.get(read_buf, 2, klData.limit());
            } else if ((klData =
                    readKCLData("DOSKeyboardLayout2", keyboardFileName, false)) != null) {
                start_pos = klData.position();
                klData.get(read_buf, 2, klData.limit());
            } else if ((klData =
                    readKCLData("DOSKeyboardLayout3", keyboardFileName, false)) != null) {
                start_pos = klData.position();
                klData.get(read_buf, 2, klData.limit());
            } else {
                Log.logging(Log.LogTypes.BIOS, Log.LogServerities.Error,
                        "Keyboard layout file %s not found", keyboardFileName);
                return DOSMain.KEYB_FILENOTFOUND;
            }
            if (tempfile != null) {
                try {
                    tempfile.position(start_pos + 2);
                    ByteBuffer rb = ByteBuffer.wrap(read_buf, 0, 65535);
                    read_buf_size = tempfile.read(rb);
                    tempfile.close();
                } catch (Exception e) {
                    // todo 오류 발생 처리 추가
                    Log.logging(Log.LogTypes.BIOS, Log.LogServerities.Error,
                            "readKeyboardFile( %s ) access error", keyboardFileName);
                    return DOSMain.KEYB_INVALIDFILE;
                }
            }
            start_pos = 0;
        } else {
            // check ID-bytes of file
            ByteBuffer rb = ByteBuffer.wrap(read_buf, 0, 4);
            try {
                int dr = tempfile.read(rb);
                if ((dr < 4) || (read_buf[0] != 0x4b) || (read_buf[1] != 0x4c)
                        || (read_buf[2] != 0x46)) {
                    Log.logging(Log.LogTypes.BIOS, Log.LogServerities.Error,
                            "Invalid keyboard layout file %s", keyboardFileName);
                    return DOSMain.KEYB_INVALIDFILE;
                }

                tempfile.position(0);
                rb = ByteBuffer.wrap(read_buf, 0, 65535);
                read_buf_size = tempfile.read(rb);
                tempfile.close();
            }

            catch (Exception e) {
                // todo 오류 발생 처리 추가
                Log.logging(Log.LogTypes.BIOS, Log.LogServerities.Error,
                        "Keyboard layout file %s access error", keyboardFileName);
                return DOSMain.KEYB_INVALIDFILE;
            }
        }

        byte data_len, submappings;
        data_len = read_buf[start_pos++];

        _languageCodes = new String[data_len];
        _languageCodeCount = 0;
        // get all language codes for this layout
        // StringBuilder sb = new StringBuilder();
        // List<char> sb = new ArrayList<char>();
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < data_len;) {
            sb.setLength(0);
            i += 2;
            for (; i < data_len;) {
                char lcode = (char) read_buf[start_pos + i];
                i++;
                if (lcode == ',')
                    break;
                // sb.Append(lcode);
                sb.append(lcode);
            }
            // language_codes[language_code_count] = sb.toString();
            _languageCodes[_languageCodeCount] = sb.toString();
            _languageCodeCount++;
        }

        start_pos += data_len; // start_pos==absolute position of KeybCB block

        submappings = read_buf[start_pos];
        _additionalPlanes = read_buf[start_pos + 1];

        // four pages always occupied by normal,shift,flags,commandbits
        if (_additionalPlanes > (_layoutPages - 4))
            _additionalPlanes = (_layoutPages - 4);

        long idx = 0;
        // ByteBuffer bc = ByteBuffer.wrap(read_buf);
        // seek to plane descriptor
        read_buf_pos = start_pos + 0x14 + submappings * 8;
        for (short cplane = 0; cplane < _additionalPlanes; cplane++) {
            int plane_flags;

            // get required-flags (shift/alt/ctrl-states etc.)
            // plane_flags = MEMORY.host_readw(ref read_buf, read_buf_pos);
            idx = read_buf_pos;
            plane_flags = ByteConv.getShort(read_buf, (int) idx);
            read_buf_pos += 2;
            _currentLayoutPlanes[cplane].required_flags = plane_flags;
            _usedLockModifiers |= (byte) (plane_flags & 0x70);
            // get forbidden-flags
            // plane_flags = MEMORY.host_readw(ref read_buf, read_buf_pos);
            idx = read_buf_pos;
            plane_flags = ByteConv.getShort(read_buf, (int) idx);
            read_buf_pos += 2;
            _currentLayoutPlanes[cplane].forbidden_flags = plane_flags;

            // get required-userflags
            // plane_flags = MEMORY.host_readw(ref read_buf, read_buf_pos);
            idx = read_buf_pos;
            plane_flags = ByteConv.getShort(read_buf, (int) idx);
            read_buf_pos += 2;
            _currentLayoutPlanes[cplane].required_userflags = plane_flags;
            // get forbidden-userflags
            // plane_flags = MEMORY.host_readw(ref read_buf, read_buf_pos);
            idx = read_buf_pos;
            plane_flags = ByteConv.getShort(read_buf, (int) idx);
            read_buf_pos += 2;
            _currentLayoutPlanes[cplane].forbidden_userflags = plane_flags;
        }

        boolean found_matching_layout = false;
        // check all submappings and use them if general submapping or same codepage submapping
        for (int sub_map = 0; (sub_map < submappings) && (!found_matching_layout); sub_map++) {
            int submap_cp, table_offset;

            if ((sub_map != 0) && (specific_layout != -1))
                sub_map = specific_layout & 0xffff;

            // read codepage of submapping
            // submap_cp = MEMORY.host_readw(ref read_buf, (int)(start_pos + 0x14 + sub_map * 8));
            idx = start_pos + 0x14 + sub_map * 8;
            submap_cp = ByteConv.getShort(read_buf, (int) idx);
            if ((submap_cp != 0) && (submap_cp != requested_codepage) && (specific_layout == -1))
                continue; // skip nonfitting submappings

            if (submap_cp == requested_codepage)
                found_matching_layout = true;

            // get table offset
            // table_offset = MEMORY.host_readw(ref read_buf, (int)(start_pos + 0x18 + sub_map *
            // 8));
            idx = start_pos + 0x18 + sub_map * 8;
            table_offset = ByteConv.getShort(read_buf, (int) idx);
            _diacriticsEntries = 0;
            if (table_offset != 0) {
                // process table
                int i, j;
                for (i = 0; i < 2048;) {
                    if (read_buf[start_pos + table_offset + i] == 0)
                        break; // end of table
                    _diacriticsEntries++;
                    i += 0xffff & ((read_buf[start_pos + table_offset + i + 1] & 0xff) * 2 + 2);
                }
                // copy diacritics table
                for (j = 0; j <= i; j++)
                    diacritics[j] = read_buf[start_pos + table_offset + j];
            }


            // get table offset
            table_offset = Memory.hostReadW(read_buf, 0xffff & (start_pos + 0x16 + sub_map * 8));
            if (table_offset == 0)
                continue; // non-present table

            read_buf_pos = start_pos + table_offset;

            bytes_read = read_buf_size - read_buf_pos;

            // process submapping table
            for (int i = 0; i < bytes_read;) {
                int scan = 0xff & read_buf[read_buf_pos++];
                if (scan == 0)
                    break;
                int scan_length = 0xff & ((read_buf[read_buf_pos] & 7) + 1);// length of data
                                                                            // struct
                read_buf_pos += 2;
                i += 3;
                if (((scan & 0x7f) <= BIOS.MAX_SCAN_CODE) && (scan_length > 0)) {
                    // add all available mappings
                    for (int addmap = 0; addmap < scan_length; addmap++) {
                        if (addmap > _additionalPlanes + 2)
                            break;
                        int charptr = read_buf_pos
                                + addmap * ((read_buf[read_buf_pos - 2] & 0x80) != 0 ? 2 : 1);
                        int kchar = 0xff & read_buf[charptr];

                        if (kchar != 0) { // key remapped
                            if ((read_buf[read_buf_pos - 2] & 0x80) != 0)
                                kchar |= (0xff & read_buf[charptr + 1]) << 8; // scancode/char pair
                            // overwrite mapping
                            _currentLayout[scan * _layoutPages + addmap] = (short) kchar;
                            // clear command bit
                            _currentLayout[scan * _layoutPages + _layoutPages - 2] &=
                                    (short) (~(1 << addmap));
                            // add command bit
                            _currentLayout[scan * _layoutPages + _layoutPages - 2] |=
                                    (short) (read_buf[read_buf_pos - 1] & (1 << addmap));
                        }
                    }

                    _currentLayout[scan * _layoutPages + _layoutPages - 1] =
                            read_buf[read_buf_pos - 2]; // flags
                    if ((read_buf[read_buf_pos - 2] & 0x80) != 0)
                        scan_length *= 2; // granularity flag (S)
                }
                i += scan_length; // advance pointer
                read_buf_pos += scan_length;
            }
            if (specific_layout == sub_map)
                break;
        }

        if (found_matching_layout) {
            if (specific_layout == -1)
                Log.logging(Log.LogTypes.BIOS, Log.LogServerities.Normal,
                        "Keyboard layout %s successfully loaded", keyboardFileName);
            else
                Log.logging(Log.LogTypes.BIOS, Log.LogServerities.Normal,
                        "Keyboard layout %s (%i) successfully loaded", keyboardFileName,
                        specific_layout);
            this._useForeignLayout = true;
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
            int key_command = layoutedKey & 0xff;
            // check if diacritics-command
            if ((key_command >= 200) && (key_command < 235)) {
                // diacritics command
                _diacriticsCharacter = key_command;
                if (_diacriticsCharacter - 200 >= _diacriticsEntries)
                    _diacriticsCharacter = 0;
                return true;
            } else if ((key_command >= 120) && (key_command < 140)) {
                // switch layout command
                this.readKeyboardFile(key_command - 119);
                return true;
            } else if ((key_command >= 180) && (key_command < 188)) {
                // switch user key off
                _userKeys &= 0xffff & (~(1 << (key_command - 180)));
                return true;
            } else if ((key_command >= 188) && (key_command < 196)) {
                // switch user key on
                _userKeys |= 0xffff & (1 << (key_command - 188));
                return true;
            } else if (key_command == 160)
                return true; // nop command
        } else {
            // non-command
            if (_diacriticsCharacter > 0) {
                if (_diacriticsCharacter - 200 >= _diacriticsEntries)
                    _diacriticsCharacter = 0;
                else {
                    int diacritics_start = 0;
                    // search start of subtable
                    for (short i = 0; i < _diacriticsCharacter - 200; i++)
                        diacritics_start += ((0xff & diacritics[diacritics_start + 1]) * 2 + 2);

                    byte diacritics_length = diacritics[diacritics_start + 1];
                    diacritics_start += 2;
                    _diacriticsCharacter = 0; // reset

                    // search scancode
                    for (short i = 0; i < diacritics_length; i++) {
                        if (diacritics[diacritics_start + i * 2] == (layoutedKey & 0xff)) {
                            // add diacritics to keybuf
                            BIOSKeyboard.addKeyToBuffer(0xffff
                                    & ((key << 8) | diacritics[diacritics_start + i * 2 + 1]));
                            return true;
                        }
                    }
                    // add standard-diacritics to keybuf
                    BIOSKeyboard.addKeyToBuffer(
                            0xffff & ((key << 8) | diacritics[diacritics_start - 2]));
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

            tempfile.position(7 + rbuf[6]);

            for (;;) {
                int cur_pos = (int) tempfile.position();
                rb = ByteBuffer.wrap(rbuf, 0, 5);
                dr = tempfile.read(rb);
                if (dr < 5)
                    break;
                int len = Memory.hostReadW(rbuf, 0);

                byte data_len = rbuf[2];

                CStringPt lng_codes = CStringPt.create(258);
                tempfile.position(tempfile.position() - 2);
                // get all language codes for this layout
                for (int i = 0; i < data_len;) {
                    rb = ByteBuffer.wrap(rbuf, 0, 2);
                    tempfile.read(rb);
                    int lcnum = Memory.hostReadW(rbuf, 0);
                    i += 2;
                    int lcpos = 0;
                    for (; i < data_len;) {
                        rb = ByteBuffer.wrap(rbuf, 0, 1);
                        tempfile.read(rb);
                        i++;
                        if (((char) rbuf[0]) == ',')
                            break;
                        lng_codes.set(lcpos++, (char) rbuf[0]);
                    }
                    lng_codes.set(lcpos, (char) 0);
                    if (lng_codes.equalsIgnoreCase(layoutId)) {
                        // language ID found in file, return file position
                        tempfile.close();
                        return cur_pos;
                    }
                    if (firstIdOnly)
                        break;
                    if (lcnum != 0) {
                        // sprintf(&lng_codes[lcpos],"%d",lcnum);
                        CStringPt pt = CStringPt.clone(lng_codes, lcpos);
                        CStringPt.copy(String.valueOf(lcnum), pt);
                        if (lng_codes.equalsIgnoreCase(layoutId)) {
                            // language ID found in file, return file position
                            return cur_pos;
                        }
                    }
                }
                tempfile.position(cur_pos + 3 + len);
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

        int dpos = 7 + kclData[6];
        long idx;
        for (;;) {
            if (dpos + 5 > kclDataSize)
                break;
            int cur_pos = dpos;
            // short len = MEMORY.host_readw(ref kcl_data, dpos);
            idx = dpos;
            int len = ByteConv.getShort(kclData, (int) idx);
            byte data_len = kclData[dpos + 2];
            dpos += 5;

            CStringPt lng_codes = CStringPt.create(258);
            // get all language codes for this layout
            for (int i = 0; i < data_len;) {
                // short lcnum = MEMORY.host_readw(ref kcl_data, dpos - 2);
                idx = dpos;
                int lcnum = ByteConv.getShort(kclData, (int) idx);
                i += 2;
                int lcpos = 0;
                for (; i < data_len;) {
                    if (dpos + 1 > kclDataSize)
                        break;
                    char lc = (char) kclData[dpos];
                    dpos++;
                    i++;
                    if (lc == ',')
                        break;
                    lng_codes.set(lcpos++, lc);
                }
                lng_codes.set(lcpos, (char) 0);
                if (lng_codes.equalsIgnoreCase(layoutId)) {
                    // language ID found in file, return file position
                    return cur_pos;
                }
                if (firstIdOnly)
                    break;
                if (lcnum != 0) {
                    // sprintf(&lng_codes[lcpos],"%d",lcnum);
                    CStringPt pt = CStringPt.clone(lng_codes, lcpos);
                    CStringPt.copy(String.valueOf(lcnum), pt);
                    if (lng_codes.equalsIgnoreCase(layoutId)) {
                        // language ID found in file, return file position
                        return cur_pos;
                    }
                }
                dpos += 2;
            }
            dpos = cur_pos + 3 + len;
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
            int cur_pos = dpos;
            // short len = MEMORY.host_readw(ref kcl_data, dpos);
            idx = dpos;
            int len = 0xffff & kclData.getShort(idx);
            int data_len = 0xff & kclData.get(dpos + 2);
            dpos += 5;

            CStringPt lng_codes = CStringPt.create(258);
            // get all language codes for this layout
            for (int i = 0; i < data_len;) {
                // short lcnum = MEMORY.host_readw(ref kcl_data, dpos - 2);
                idx = dpos;
                int lcnum = 0xffff & kclData.getShort(idx);
                i += 2;
                int lcpos = 0;
                for (; i < data_len;) {
                    if (dpos + 1 > kclDataSize)
                        break;
                    char lc = (char) kclData.get(dpos);
                    dpos++;
                    i++;
                    if (lc == ',')
                        break;
                    lng_codes.set(lcpos++, lc);
                }
                lng_codes.set(lcpos, (char) 0);
                if (lng_codes.equalsIgnoreCase(layoutId)) {
                    // language ID found in file, return file position
                    kclData.position(cur_pos);
                    return kclData;
                }
                if (firstIdOnly)
                    break;
                if (lcnum != 0) {
                    // sprintf(&lng_codes[lcpos],"%d",lcnum);
                    CStringPt pt = CStringPt.clone(lng_codes, lcpos);
                    CStringPt.copy(String.valueOf(lcnum), pt);
                    if (lng_codes.equalsIgnoreCase(layoutId)) {
                        // language ID found in file, return file position
                        kclData.position(cur_pos);
                        return kclData;
                    }
                }
                dpos += 2;
            }
            dpos = cur_pos + 3 + len;
        }
        return null;
    }

}
