package org.cloudsimfe;

public class Netlist {

    private Accelerator accelerator;
    private int requiredRegionCount;
    private int requiredExecutionTime;
    private int deadline;

    public Netlist(Accelerator accelerator, int requiredRegionCount, int requiredExecutionTime, int deadline) {
        this.accelerator = accelerator;
        this.requiredRegionCount = requiredRegionCount;
        this.requiredExecutionTime = requiredExecutionTime;
        this.deadline = deadline;
    }

    public Accelerator getAccelerator() {
        return accelerator;
    }

    public int getRequiredRegionCount() {
        return requiredRegionCount;
    }

    public int getRequiredExecutionTime() {
        return requiredExecutionTime;
    }

    public int getDeadline() {
        return deadline;
    }

    @Override
    public String toString() {
        return accelerator.toString() + "\n" +
                "Required regions: " + requiredRegionCount + "\n" +
                "Estimated execution time: " + requiredExecutionTime + "\n" +
                "Expected deadline: " + deadline;
    }
}
