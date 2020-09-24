package org.cloudsimfe;

import java.util.ArrayList;
import java.util.List;

public class VFpga extends AddressableComponent {

    public static int CURRENT_VFPGA_ID = 1;

    private int id;
    private Wrapper wrapper;
    private Accelerator accelerator;
    private List<Region> regions;
    private List<SegmentExecution> segmentExecutionList;
    private double estimatedFinishTime;
    private double createdAt;
    private double destroyedAt;
    private double configurationTime;
    private VFpgaManager manager;

    public VFpga(int id, Wrapper wrapper, Region region) {
        this.id = id;
        this.wrapper = wrapper;
        segmentExecutionList = new ArrayList<>();
        regions = new ArrayList<>();
        regions.add(region);
    }

    public VFpga(int id, Wrapper wrapper, List<Region> regions) {
        this.id = id;
        this.wrapper = wrapper;
        segmentExecutionList = new ArrayList<>();
        this.regions = regions;
    }

    public double updateProcessing(final double currentTime) {
        estimatedFinishTime = accelerator.updateProcessing(currentTime);
        return estimatedFinishTime;
    }

    public void addSegmentExecution(SegmentExecution segmentExecution){
        segmentExecutionList.add(segmentExecution);
    }

    public List<SegmentExecution> getSegmentExecutionList(){
        return segmentExecutionList;
    }

    public VFpgaManager getManager() {
        return manager;
    }

    public void setManager(VFpgaManager manager) {
        this.manager = manager;
    }

    public double getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(double createdAt) {
        this.createdAt = createdAt;
    }

    public double getDestroyedAt() {
        return destroyedAt;
    }

    public double getConfigurationTime() {
        return configurationTime;
    }

    public void setConfigurationTime(double configurationTime) {
        this.configurationTime = configurationTime;
    }

    public void setDestroyedAt(double destroyedAt) {
        this.destroyedAt = destroyedAt;
    }

    public void setAccelerator(Accelerator accelerator) {
        accelerator.setWrapper(wrapper);
        accelerator.setVFpga(this);
        this.accelerator = accelerator;
    }

    public Accelerator getAccelerator(){
        return accelerator;
    }

    public Wrapper getWrapper() {
        return wrapper;
    }

    public List<Region> getRegions() {
        return regions;
    }

    @Override
    public void sendDataToComponent(Payload payload) {
//        AccelerableSegment segment = (AccelerableSegment) payload.getData().get(0);
//        estimatedFinishTime = accelerator.cloudletSubmit(segment, 0.0);
//        wrapper.writeToOutputBuffer(estimatedFinishTime);
    }

    @Override
    public String toString() {
        return "Accelerator{" +
                "id=" + id +
                ", wrapper=" + wrapper +
                ", accelerator=" + accelerator +
                ", regions=" + regions +
                ", estimatedFinishTime=" + estimatedFinishTime +
                '}';
    }

    public int getId() {
        return id;
    }

    public double getEstimatedFinishTime() {
        return estimatedFinishTime;
    }
}
