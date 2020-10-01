package org.cloudsimfe;

/**
 * @author Abid Farhan
 * @version CloudSim Plus 1.0
 */
public class Bitstream {
    private Adapter adapter;
    private Accelerator accelerator;
    private int requiredRegionCount;

    public Bitstream(Adapter adapter, Accelerator accelerator, int requiredRegionCount) {
        this.adapter = adapter;
        this.accelerator = accelerator;
        this.requiredRegionCount = requiredRegionCount;
    }

    public Adapter getAdapter() {
        return adapter;
    }

    public void setAdapter(Adapter adapter) {
        this.adapter = adapter;
    }

    public Accelerator getAccelerator() {
        return accelerator;
    }

    public void setAccelerator(Accelerator accelerator) {
        this.accelerator = accelerator;
    }

    public int getRequiredRegionCount() {
        return requiredRegionCount;
    }

    public void setRequiredRegionCount(int requiredRegionCount) {
        this.requiredRegionCount = requiredRegionCount;
    }

}
