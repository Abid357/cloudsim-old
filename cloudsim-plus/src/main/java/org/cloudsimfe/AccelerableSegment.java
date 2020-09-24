package org.cloudsimfe;

import org.cloudbus.cloudsim.cloudlets.Cloudlet;

public class AccelerableSegment {

    private int id;
    private String uniqueId;
    private long index;
    private long length;
    private long finishedLengthSoFar;
    private AccelerableCloudlet cloudlet;
    private Accelerator accelerator;
    private int type;
    private boolean backwardDependency;
    private boolean forwardDependency;

    public AccelerableSegment(int id, long index, long length, AccelerableCloudlet cloudlet, int type) {
        this.id = id;
        this.index = index;
        this.length = length;
        this.cloudlet = cloudlet;
        finishedLengthSoFar = 0;
        this.type = type;
        setUniqueId();
        backwardDependency = false;
        forwardDependency = false;
    }

    public void setBackwardDependency(boolean backwardDependency) {
        this.backwardDependency = backwardDependency;
    }

    public void setForwardDependency(boolean forwardDependency) {
        this.forwardDependency = forwardDependency;
    }

    public boolean hasBackwardDependency() {
        return backwardDependency;
    }

    public boolean hasForwardDependency() {
        return forwardDependency;
    }

    public String getUniqueId() {
        return uniqueId;
    }

    private void setUniqueId() {
        this.uniqueId = id + "-" + cloudlet.getId();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public long getIndex() {
        return index;
    }

    public void setIndex(long index) {
        this.index = index;
    }

    public long getLength() {
        return length;
    }

    public void setLength(long length) {
        this.length = length;
    }

    public AccelerableCloudlet getCloudlet() {
        return cloudlet;
    }

    public void setCloudlet(AccelerableCloudlet cloudlet) {
        this.cloudlet = cloudlet;
    }

    public boolean isAssignedToAccelerator() {
        return accelerator != null;
    }

    public double registerArrivalInAccelerator() {
        if (!isAssignedToAccelerator()) {
            return Cloudlet.NOT_ASSIGNED;
        }

        double time = cloudlet.getSimulation().clock();
        accelerator.setArrivalTime(time);

        return time;
    }

    public boolean isFinished() {
        return length > 0 && finishedLengthSoFar >= length;
    }

    public void addFinishedLengthSoFar(final long length) {
        finishedLengthSoFar += length;
    }

    public long getFinishedLengthSoFar() {
        return finishedLengthSoFar;
    }

    @Override
    public String toString() {
        return String.format("Segment %d in Cloudlet %d", id, cloudlet.getId());
    }
}
