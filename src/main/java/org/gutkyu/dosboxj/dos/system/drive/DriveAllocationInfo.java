package org.gutkyu.dosboxj.dos.system.drive;

public final class DriveAllocationInfo {
    public int bytesSector;// uint16
    public byte sectorsCluster;
    public short totalClusters;
    public short freeClusters;

    public DriveAllocationInfo(int bytesSector, byte sectorsCluster, short totalClusters,
            short freeClusters) {
        this.bytesSector = bytesSector;
        this.sectorsCluster = sectorsCluster;
        this.totalClusters = totalClusters;
        this.freeClusters = freeClusters;
    }

    public DriveAllocationInfo(int bytesSector, int sectorsCluster, int totalClusters) {
        this(bytesSector, sectorsCluster, totalClusters, (short) 0);
    }

    public DriveAllocationInfo(int bytesSector, int sectorsCluster, int totalClusters,
            int freeClusters) {
        this(bytesSector, (byte) sectorsCluster, (short) totalClusters, (short) freeClusters);
    }
}
