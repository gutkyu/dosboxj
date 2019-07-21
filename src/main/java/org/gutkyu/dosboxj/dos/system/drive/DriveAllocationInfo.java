package org.gutkyu.dosboxj.dos.system.drive;

public final class DriveAllocationInfo {
    public int bytesSector;// uint16
    public int sectorsCluster;// uint8
    public int totalClusters;// uint16
    public int freeClusters;// uint16

    public DriveAllocationInfo(int bytesSector, int sectorsCluster, int totalClusters,
            int freeClusters) {
        this.bytesSector = 0xffff & bytesSector;
        this.sectorsCluster = 0xff & sectorsCluster;
        this.totalClusters = 0xffff & totalClusters;
        this.freeClusters = 0xffff & freeClusters;
    }

    public DriveAllocationInfo(int bytesSector, int sectorsCluster, int totalClusters) {
        this(bytesSector, sectorsCluster, totalClusters, 0);
    }

}
