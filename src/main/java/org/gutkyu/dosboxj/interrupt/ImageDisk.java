package org.gutkyu.dosboxj.interrupt;

import org.gutkyu.dosboxj.hardware.*;
import org.gutkyu.dosboxj.hardware.memory.*;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import org.gutkyu.dosboxj.util.*;
import org.gutkyu.dosboxj.interrupt.bios.*;

public final class ImageDisk implements Disposable {

    public byte readSector(int head, int cylinder, int sector, byte[] data) {
        int sectnum;

        sectnum = ((cylinder * heads + head) * sectors) + sector - 1;

        return readAbsoluteSector(sectnum, data, 0);
    }

    public byte writeSector(int head, int cylinder, int sector, byte[] data) {
        int sectnum;

        sectnum = ((cylinder * heads + head) * sectors) + sector - 1;

        return writeAbsoluteSector(sectnum, data, 0);

    }

    public byte readAbsoluteSector(int sectnum, byte[] data, int data_start) {
        long bytenum;

        bytenum = sectnum * sector_size;
        ByteBuffer buf = ByteBuffer.wrap(data, data_start, 1 * sector_size);
        try {
            diskimg.position(bytenum).read(buf);
        } catch (IOException e) {

        }
        return 0x00;
    }

    public byte writeAbsoluteSector(int sectnum, byte[] data, int data_start) {
        long bytenum;

        bytenum = sectnum * sector_size;

        // Log.LOG_MSG("Writing sectors to %ld at bytenum %d", sectnum, bytenum);
        ByteBuffer buf = ByteBuffer.wrap(data, data_start, 1 * sector_size);

        try {
            diskimg.position(bytenum).write(buf);
        } catch (Exception e) {
            return 0x05;
        }
        return 0x00;

    }


    public void setGeometry(int setHeads, int setCyl, int setSect, int setSectSize) {
        heads = setHeads;
        cylinders = setCyl;
        sectors = setSect;
        sector_size = setSectSize;
        active = true;
    }

    public int getGeometryHeads() {
        return heads;
    }

    public int getGeometryCylinders() {
        return cylinders;
    }

    public int getGeometrySectors() {
        return sectors;
    }

    public int getGeometrySectSize() {
        return sector_size;
    }

    // uint8
    public int getBiosType() {
        if (!hardDrive) {
            return 0xff & BIOSDisk.DiskGeometryList[floppyType].BiosValue;
        } else
            return 0;
    }

    public ImageDisk(SeekableByteChannel imgFile, String imgName, int imgSizeK,
            boolean isHardDisk) {
        heads = 0;
        cylinders = 0;
        sectors = 0;
        sector_size = 512;
        diskimg = imgFile;

        diskname = imgName;

        active = false;
        hardDrive = isHardDisk;
        if (!isHardDisk) {
            int i = 0;
            boolean founddisk = false;
            while (BIOSDisk.DiskGeometryList[i].KSize != 0x0) {
                if ((BIOSDisk.DiskGeometryList[i].KSize == imgSizeK)
                        || (BIOSDisk.DiskGeometryList[i].KSize + 1 == imgSizeK)) {
                    if (BIOSDisk.DiskGeometryList[i].KSize != imgSizeK)
                        Log.logMsg("ImageLoader: image file with additional data, might not load!");
                    founddisk = true;
                    active = true;
                    floppyType = i;
                    heads = BIOSDisk.DiskGeometryList[i].HeadsCylinder;
                    cylinders = BIOSDisk.DiskGeometryList[i].CylinderCount;
                    sectors = BIOSDisk.DiskGeometryList[i].SectorsTrack;
                    break;
                }
                i++;
            }
            if (!founddisk) {
                active = false;
            } else {
                int equipment = Memory.readW(BIOS.BIOS_CONFIGURATION);
                if ((equipment & 1) != 0) {
                    int numofdisks = (equipment >>> 6) & 3;
                    numofdisks++;
                    if (numofdisks > 1)
                        numofdisks = 1;// max 2 floppies at the moment
                    equipment &= 0xffff & ~0x00C0;
                    equipment |= 0xffff & (numofdisks << 6);
                } else
                    equipment |= 1;
                Memory.writeW(BIOS.BIOS_CONFIGURATION, equipment);
                CMOSModule.setRegister(0x14, (byte) equipment);
            }
        }
    }

    public void dispose() {
        dispose(true);
    }

    protected void dispose(boolean disposing) {
        if (disposing) {
            diskname = null;
        }
        if (diskimg != null) {
            try {
                diskimg.close();
            } catch (Exception e) {

            }

        }
    }

    public boolean hardDrive;
    public boolean active;
    public SeekableByteChannel diskimg;
    public String diskname = "";
    public int floppyType;// uint8

    public int sector_size;
    public int heads, cylinders, sectors;

}
