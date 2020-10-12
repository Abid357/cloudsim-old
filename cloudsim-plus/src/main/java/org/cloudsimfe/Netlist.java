package org.cloudsimfe;

public class Netlist {

    private Accelerator accelerator;
    private int requiredRegionCount;
    private int requiredExecutionTime;
    private int deadline;
    private int fileSize; // in MB

    public Netlist(Accelerator accelerator, int requiredRegionCount, int requiredExecutionTime, int deadline,
                   int fileSize) {
        this.accelerator = accelerator;
        this.requiredRegionCount = requiredRegionCount;
        this.requiredExecutionTime = requiredExecutionTime;
        this.deadline = deadline;
        this.fileSize = fileSize;
    }

    public Netlist copy(){
       return new Netlist(accelerator.copy(), requiredRegionCount, requiredExecutionTime, deadline, fileSize);
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

    public int getFileSize(){
        return fileSize;
    }

    @Override
    public String toString() {
        return accelerator.toString() + "\n" +
                "Required regions: " + requiredRegionCount + "\n" +
                "Estimated execution time: " + requiredExecutionTime + "\n" +
                "Expected deadline: " + deadline + "\n" +
                "Bitstream size: " + fileSize + " MB";
    }
}
