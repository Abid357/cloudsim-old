package org.cloudsimfe;

/**
 * @author Abid Farhan
 * @version CloudSim Plus 1.0
 */
public class Bitstream {
    private Adapter adapter;
    private Accelerator accelerator;
    private int requiredRegionCount;
    private int requiredExecutionTime;
    private int deadline;

    public Bitstream(Adapter adapter, Accelerator accelerator, int requiredRegionCount, int requiredExecutionTime) {
        this(adapter, accelerator, requiredRegionCount, requiredExecutionTime, 0);
    }

    private Bitstream(Adapter adapter, Accelerator accelerator, int requiredRegionCount, int requiredExecutionTime,
                      int deadline) {
        this.adapter = adapter;
        this.accelerator = accelerator;
        this.requiredRegionCount = requiredRegionCount;
        this.requiredExecutionTime = requiredExecutionTime;
        this.deadline = deadline;
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

    public int getRequiredExecutionTime() {
        return requiredExecutionTime;
    }

    public void setRequiredExecutionTime(int requiredExecutionTime) {
        this.requiredExecutionTime = requiredExecutionTime;
    }

    public int getDeadline() {
        return deadline;
    }

    public void setDeadline(int deadline) {
        this.deadline = deadline;
    }
}
