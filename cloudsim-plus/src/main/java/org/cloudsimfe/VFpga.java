package org.cloudsimfe;

import java.util.ArrayList;
import java.util.List;

public class VFpga extends AddressableComponent {

    public static int CURRENT_VFPGA_ID = 1;

    private int id;
    private Adapter adapter;
    private Accelerator accelerator;
    private List<Region> regions;
    private List<SegmentExecution> segmentExecutionList;
    private double estimatedFinishTime;
    private double createdAt;
    private double destroyedAt;
    private double configurationTime;
    private VFpgaManager manager;

    public VFpga(int id, Adapter adapter, Region region) {
        this.id = id;
        this.adapter = adapter;
        segmentExecutionList = new ArrayList<>();
        regions = new ArrayList<>();
        regions.add(region);
    }

    public VFpga(int id, Adapter adapter, List<Region> regions) {
        this.id = id;
        this.adapter = adapter;
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
        accelerator.setAdapter(adapter);
        accelerator.setVFpga(this);
        this.accelerator = accelerator;
    }

    public Accelerator getAccelerator(){
        return accelerator;
    }

    public Adapter getAdapter() {
        return adapter;
    }

    public List<Region> getRegions() {
        return regions;
    }

    @Override
    public void sendDataToComponent(Payload payload) {
    }

    @Override
    public String toString() {
        return "Accelerator{" +
                "id=" + id +
                ", adapter=" + adapter +
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
