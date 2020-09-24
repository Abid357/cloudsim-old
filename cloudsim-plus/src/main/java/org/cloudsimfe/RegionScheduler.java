package org.cloudsimfe;

public interface RegionScheduler {
    boolean scheduleRegions(int fpgaCount, int regionCount, Object tasks);

    void print(Object... options);

    double getSchedulingDuration();
}
