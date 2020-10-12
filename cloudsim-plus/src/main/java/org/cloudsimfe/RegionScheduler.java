package org.cloudsimfe;

public interface RegionScheduler {
    boolean schedule(int fpgaCount, int regionCount, Object tasks);

    void print(Object... options);

    double getSchedulingDuration();
}
