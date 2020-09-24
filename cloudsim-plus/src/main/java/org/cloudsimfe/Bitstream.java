package org.cloudsimfe;

/**
 * @author Abid Farhan
 * @version CloudSim Plus 1.0
 */
public class Bitstream {
    private Wrapper wrapper;
    private Accelerator accelerator;
    private int requiredBlockCount;
    private int requiredExecutionTime;
    private int deadline;

    public Bitstream(Wrapper wrapper, Accelerator accelerator, int requiredRegionCount, int requiredExecutionTime) {
        this(wrapper, accelerator, requiredRegionCount, requiredExecutionTime, 0);
    }

    private Bitstream(Wrapper wrapper, Accelerator accelerator, int requiredRegionCount, int requiredExecutionTime,
                     int deadline) {
        this.wrapper = wrapper;
        this.accelerator = accelerator;
        this.requiredBlockCount = requiredRegionCount;
        this.requiredExecutionTime = requiredExecutionTime;
        this.deadline = deadline;
    }

    public Wrapper getWrapper() {
        return wrapper;
    }

    public void setWrapper(Wrapper wrapper) {
        this.wrapper = wrapper;
    }

    public Accelerator getAccelerator() {
        return accelerator;
    }

    public void setAccelerator(Accelerator accelerator) {
        this.accelerator = accelerator;
    }

    public int getRequiredBlockCount() {
        return requiredBlockCount;
    }

    public void setRequiredBlockCount(int requiredBlockCount) {
        this.requiredBlockCount = requiredBlockCount;
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
