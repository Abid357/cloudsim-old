package org.cloudsimfe;

import org.cloudbus.cloudsim.cloudlets.CloudletSimple;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.Simulation;
import org.cloudbus.cloudsim.core.events.CloudSimEvent;
import org.cloudbus.cloudsim.core.events.SimEvent;
import org.cloudbus.cloudsim.datacenters.DatacenterSimple;
import org.cloudbus.cloudsim.hosts.Host;

import java.util.ArrayList;
import java.util.List;

public class DatacenterFE extends DatacenterSimple {

    private List<Fpga> fpgaList;
    private UnifiedManager unifiedManager;
    private boolean isRegionSchedulingFinished;
    private DhcpServer server;

    public DatacenterFE(Simulation simulation, List<? extends Host> hostList, List<Fpga> fpgaList, DhcpServer server) {
        super(simulation, hostList);
        this.fpgaList = fpgaList;
        this.server = server;
        setupFpgas();
    }

    @Override
    protected void processCloudletSubmit(SimEvent evt, boolean ack) {
        final AccelerableCloudlet cloudlet = (AccelerableCloudlet) evt.getData();
        SimEvent modifiedEvt = evt;

        // copied from parent class DatacenterSimple since the function was private
        if (cloudlet.isFinished()) {
            LOGGER.warn(
                    "{}: {} owned by {} is already completed/finished. It won't be executed again.",
                    getName(), cloudlet, cloudlet.getBroker());
            if (ack)
                sendNow(cloudlet.getBroker(), CloudSimTags.CLOUDLET_SUBMIT_ACK, cloudlet);
            sendNow(cloudlet.getBroker(), CloudSimTags.CLOUDLET_RETURN, cloudlet);
        }

        if (!fpgaList.isEmpty()) {
            CloudletSimple cloudletWithoutSegments = cloudlet.getCloudletWithoutAccelerableSegments();
            modifiedEvt = new CloudSimEvent(evt.getDestination(), evt.getTag(), cloudletWithoutSegments);
            unifiedManager.submitAccelerableSegments(cloudlet.getSegments());
        }
        super.processCloudletSubmit(modifiedEvt, ack);
    }

    protected double updateCloudletProcessing() {
        // copied from parent class DatacenterSimple since the function was private
        if (!(getSimulation().clock() < 0.111 ||
                getSimulation().clock() >= getLastProcessTime() + getSimulation().getMinTimeBetweenEvents()))
            return Double.MAX_VALUE;

        double nextSimulationTime = unifiedManager.updateSegmentProcessing();

        return Math.min(nextSimulationTime, super.updateCloudletProcessing());
    }

    @Override
    public void processEvent(final SimEvent evt) {
        if (processFpgaEvents(evt)) {
            return;
        } else
            super.processEvent(evt);
    }

    private boolean processFpgaEvents(final SimEvent evt) {
        if (evt.getTag() == CloudSimTags.REGION_SCHEDULING_FINISH) {
            isRegionSchedulingFinished = true;
            double schedulingDuration = (double) evt.getData();
            String unit = "nanoseconds";

            // convert from nanoseconds to seconds
            if (schedulingDuration >= 1000000000) {
                schedulingDuration /= 1000000000;
                unit = "seconds";
            }

            LOGGER.info(
                    "{}: {}: Region scheduling has finished. Scheduling duration: {} {}",
                    getSimulation().clockStr(), getClass().getSimpleName(), schedulingDuration, unit);

//            unifiedManager.printRegionScheduler(RegionSchedulerSA.PRINT_OPTION_SUMMARY);

            sendNow(unifiedManager, CloudSimTags.VFPGA_UPDATE_SEGMENT_PROCESSING);
            return true;
        } else if (evt.getTag() == CloudSimTags.REGION_SCHEDULING_FAIL) {
            LOGGER.warn(
                    "{}: {}: Region scheduling failed. No physical FPGAs found for virtualization. All accelerable " +
                            "segments are routed to VMs.",
                    getSimulation().clockStr(), getClass().getSimpleName());
        }
        return false;
    }

    private void setupFpgas() {
        fpgaList.forEach(fpga -> fpga.getConfigurationManager().initialize());

        List<VFpgaManager> vFpgaManagers = new ArrayList<>();
        for (Fpga fpga : fpgaList)
            vFpgaManagers.add(fpga.getVFpgaManager());

        unifiedManager = new UnifiedManager(getSimulation(), vFpgaManagers);
        unifiedManager.setDatacenter(this);
        unifiedManager.assignIpAddress(server.requestIpAddress());
        server.sendAck(unifiedManager.getIpAddress());

        for (Fpga fpga : fpgaList)
            fpga.getNetworkManager().registerRoute(unifiedManager.getIpAddress(), unifiedManager);
    }

    public List<Fpga> getFpgaList() {
        return fpgaList;
    }

    public UnifiedManager getUnifiedManager() {
        return unifiedManager;
    }

    public boolean isRegionSchedulingFinished() {
        return isRegionSchedulingFinished;
    }

    public void setRegionSchedulingFinished(boolean regionSchedulingFinished) {
        isRegionSchedulingFinished = regionSchedulingFinished;
    }

    public DhcpServer getServer() {
        return server;
    }
}