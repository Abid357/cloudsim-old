package org.cloudsimfe;

import org.cloudbus.cloudsim.brokers.DatacenterBroker;
import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.core.CloudSimEntity;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.Simulation;
import org.cloudbus.cloudsim.core.events.SimEvent;
import org.cloudbus.cloudsim.datacenters.Datacenter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class UnifiedManager extends CloudSimEntity implements Addressable {
    private static final Logger LOGGER = LoggerFactory.getLogger(UnifiedManager.class.getSimpleName());

    private Datacenter datacenter;
    private DatacenterBroker broker;
    private List<VFpgaManager> vFpgaManagers;
    private List<AccelerableSegment> waitingSegmentList;
    private List<AccelerableSegment> submittedSegmentList;
    private List<AccelerableSegment> finishedSegmentList;
    private List<SegmentExecution> segmentExecutionList;
    private List<AccelerableCloudlet> trackedCloudletList;
    private List<Accelerator> accelerators;
    private String ipAddress;
    private RegionScheduler scheduler;
    private DecimalFormat format;
    private NetlistStore store;

    public UnifiedManager(Simulation simulation, List<VFpgaManager> vFpgaManagers) {
        super(simulation);
        this.vFpgaManagers = vFpgaManagers;
        waitingSegmentList = new ArrayList<>();
        submittedSegmentList = new ArrayList<>();
        finishedSegmentList = new ArrayList<>();
        segmentExecutionList = new ArrayList<>();
        trackedCloudletList = new ArrayList<>();
        scheduler = new RegionSchedulerMSA(this);
        format = new DecimalFormat("##########.00");
    }

    public void setBroker(DatacenterBroker broker) {
        this.broker = broker;
    }

    public Datacenter getDatacenter() {
        return datacenter;
    }

    public void setDatacenter(Datacenter datacenter) {
        this.datacenter = datacenter;
    }

    public boolean submitAccelerableSegments(List<AccelerableSegment> segments) {
        for (AccelerableSegment segment : segments)
            if (!waitingSegmentList.contains(segment))
                waitingSegmentList.add(segment);
        return true;
    }

    public double updateSegmentProcessing() {
        double currentTime = getSimulation().clock();
        double nextSimulationTime = Double.MAX_VALUE;
        for (VFpgaManager vFpgaManager : vFpgaManagers) {
            double time = vFpgaManager.updateProcessing(currentTime);
            nextSimulationTime = Math.min(time, nextSimulationTime);
        }

        // synchronize any vFPGA that is configured across multiple FPGAs
        for (int i = 0; i < vFpgaManagers.size(); i++) {
            List<VFpga> destroyedVFpgasX = vFpgaManagers.get(i).getDestroyedVFpgas();
            for (int j = 0; j < vFpgaManagers.size(); j++) {
                if (j != i) {
                    List<VFpga> destroyedVFpgasY = vFpgaManagers.get(j).getDestroyedVFpgas();
                    for (VFpga destroyedX : destroyedVFpgasX)
                        for (VFpga destroyedY : destroyedVFpgasY)
                            if (destroyedX.getId() == destroyedY.getId())
                                System.out.println("lel");
                }
            }
        }

        if (nextSimulationTime != Double.MAX_VALUE)
            send(this, nextSimulationTime, CloudSimTags.VFPGA_UPDATE_SEGMENT_PROCESSING);
        return nextSimulationTime;
    }

    public void updateHypervisors(int timeSlots, int[] bestSolution, List<ConfigurationTask> tasks) {
        int b = 0;
        int regionCount = getTotalRegionCount();

        for (ConfigurationTask task : tasks) {
            for (VFpgaManager vFpgaManager : vFpgaManagers) {
                vFpgaManager.addVFpgaToAcceleratorMapping(task.getId(), task.getAcceleratorId());
            }
        }
        for (VFpgaManager vFpgaManager : vFpgaManagers) {
            vFpgaManager.createTiles();
            vFpgaManager.setUnifiedManager(this);

            long tiles[][] = vFpgaManager.getTiles();
            for (int i = 0; i < tiles.length; i++, b++) {
                tiles[i] = new long[timeSlots + 1];
                tiles[i][0] = b + 1;
                for (int j = 0; j < timeSlots; j++) {
                    int index = regionCount * j + b;
                    tiles[i][j + 1] = bestSolution[index];
                }
            }

////             test
//            System.out.println("VFpgaManager of FPGA " + vFpgaManager.getFpga().getId() + ":");
//            for (int i = 0; i < tiles.length; i++) {
//                for (int j = 0; j < tiles[0].length; j++)
//                    System.out.print(tiles[i][j] + " ");
//                System.out.println();
//            }
//            vFpgaManager.printPerspective(System.out);
        }
    }

    private void processRegionScheduling(SimEvent evt) {
        accelerators = (List<Accelerator>) evt.getData();

        if (vFpgaManagers.isEmpty()) {
            sendNow(datacenter, CloudSimTags.REGION_SCHEDULING_FAIL);
            return;
        }

        List<ConfigurationTask> tasks = new ArrayList<>();
        for (Accelerator accelerator : accelerators) {
            Netlist netlist = store.getNetlist(accelerator.getAcceleratorId());
            int id = ConfigurationTask.TASK_ID++;
            int tile = netlist.getRequiredRegionCount();
            int executionTime = netlist.getRequiredExecutionTime();
            int deadline = netlist.getDeadline();
            tasks.add(new ConfigurationTask(id, accelerator.getAcceleratorId(), tile, executionTime, deadline));
        }

        int regionCount = getTotalRegionCount();
        scheduler.schedule(vFpgaManagers.size(), regionCount, tasks);
        send(datacenter, getSchedulingDurationInSeconds(), CloudSimTags.REGION_SCHEDULING_FINISH,
                scheduler.getSchedulingDuration());
    }

    public void setNetlistStore(NetlistStore store) {
        this.store = store;
    }

    public int getTotalRegionCount() {
        int count = 0;
        for (VFpgaManager vFpgaManager : vFpgaManagers)
            count += vFpgaManager.getReconfigurableRegionCount();
        return count;
    }

    private void processReconfigurationRequest(SimEvent evt) {
        Payload payload = (Payload) evt.getData();
        int vFpgaId = (Integer) payload.getData().get(0);
        int row = (Integer) payload.getData().get(1);
        VFpgaManager vFpgaManager = (VFpgaManager) payload.getData().get(2);
        int requiredRegionCount = (Integer) payload.getData().get(3);
        int acceleratorId = vFpgaManager.getVFpgaToAcceleratorMapping(vFpgaId);

        Netlist netlist = store.getNetlist(acceleratorId);
        Adapter adapter = new Adapter(netlist.getAccelerator().getInputChannels(),
                netlist.getAccelerator().getOutputChannels());

        // bitstream generation
        Bitstream bitstream = new Bitstream(adapter, netlist.getAccelerator(), requiredRegionCount,
                netlist.getFileSize());
        List<Mapper> mappersForNextVFpga = new ArrayList<>();
        for (int i = 0; i < requiredRegionCount; i++) {
            mappersForNextVFpga.add(new Mapper(vFpgaId, row + i + 1));
        }

        List<Object> packetData = new ArrayList<>();
        packetData.add(bitstream);
        packetData.add(mappersForNextVFpga);
        packetData.add(vFpgaId);

        Packet packet = new Packet(getIpAddress(), vFpgaManager.getFpga().getConfigurationManager().getIpAddress(),
                Packet.BITSTREAM, new Payload(packetData));
        vFpgaManager.getFpga().getNetworkManager().sendDataToComponent(new Payload(packet));
    }

    private void processSegmentSubmit(SimEvent evt) {
        VFpga vFpga = (VFpga) evt.getData();

        AccelerableSegment segmentToSubmit = null;
        for (AccelerableSegment segment : waitingSegmentList) {
            if (segment.getType() == vFpga.getAccelerator().getType()) {
                segmentToSubmit = segment;
                break;
            }
        }

        if (segmentToSubmit == null) {
            LOGGER.warn("{}: {}: VFPGA {} remains idle because it has no suitable accelerable segment to process.",
                    getSimulation().clockStr(), getClass().getSimpleName(), vFpga.getId());
            return;
        }

        Payload payload = new Payload(segmentToSubmit);
        Packet packet = new Packet(getIpAddress(), vFpga.getIpAddress(), Packet.VFPGA_INPUT, payload);

        for (int i = 0; i < vFpgaManagers.size(); i++) {
            Fpga fpga = vFpgaManagers.get(i).getFpga();
            fpga.getNetworkManager().sendDataToComponent(new Payload(packet));
        }

        waitingSegmentList.remove(segmentToSubmit);
        submittedSegmentList.add(segmentToSubmit);
    }

    private void processSegmentAck(SimEvent evt) {
        Payload payload = (Payload) evt.getData();
        AccelerableSegment segment = (AccelerableSegment) payload.getData().get(0);
        VFpga vFpga = (VFpga) payload.getData().get(1);

        double transferTime = 0.0;
        vFpga.getAccelerator().segmentSubmit(transferTime);
        LOGGER.info("{}: {}: Accelerable segment {} successfully received by vFPGA {}.",
                getSimulation().clockStr(),
                getClass().getSimpleName(), segment.getUniqueId(), vFpga.getId());
        sendNow(this, CloudSimTags.VFPGA_UPDATE_SEGMENT_PROCESSING);
    }

    private void processNonAccelerableSegmentsFinish(SimEvent evt) {
        Cloudlet finishedCloudlet = (Cloudlet) evt.getData();
        AccelerableCloudlet accelerableCloudlet = null;
        for (int i = 0; i < broker.getCloudletSubmittedList().size(); i++) {
            accelerableCloudlet = (AccelerableCloudlet) broker.getCloudletSubmittedList().get(i);
            if (accelerableCloudlet.getNonAccelerableSegments().equals(finishedCloudlet))
                break;
        }
        trackCloudlet(accelerableCloudlet);
    }

    public String format2DecimalPlaces(double value) {
        return format.format(value);
    }

    @Override
    public void shutdownEntity() {
        super.shutdownEntity();
        LOGGER.info("{}: {} is shutting down...", getSimulation().clockStr(), getName());
    }

    @Override
    protected void startEntity() {
        LOGGER.info("{} is starting...", getClass().getSimpleName());
    }

    @Override
    public void processEvent(SimEvent evt) {
        if (evt.getTag() == CloudSimTags.SCHEDULE_PARTITIONED_REGIONS) {
            processRegionScheduling(evt);
        } else if (evt.getTag() == CloudSimTags.VFPGA_UPDATE_SEGMENT_PROCESSING) {
            updateSegmentProcessing();
        } else if (evt.getTag() == CloudSimTags.VFPGA_DYNAMIC_PARTIAL_RECONFIGURATION) {
            processReconfigurationRequest(evt);
        } else if (evt.getTag() == CloudSimTags.VFPGA_SEGMENT_SUBMIT) {
            processSegmentSubmit(evt);
        } else if (evt.getTag() == CloudSimTags.VFPGA_SEGMENT_ACK) {
            processSegmentAck(evt);
        } else if (evt.getTag() == CloudSimTags.CLOUDLET_RETURN) {
            processNonAccelerableSegmentsFinish(evt);
        }
    }

    @Override
    public boolean schedule(int tag) {
        return false;
    }

    double getSchedulingDurationInSeconds() {
        return scheduler.getSchedulingDuration() / 1000000000.0;
    }

    @Override
    public void assignIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    @Override
    public String withdrawIpAddress() {
        String temp = ipAddress;
        ipAddress = null;
        return temp;
    }

    @Override
    public String getIpAddress() {
        return ipAddress;
    }

    public void trackCloudlet(AccelerableCloudlet cloudlet) {
        Cloudlet nonAccelerableSegments = cloudlet.getNonAccelerableSegments();
        List<AccelerableSegment> accelerableSegments = cloudlet.getSegments();

        boolean notifyBroker = true;
        if (nonAccelerableSegments != null) {
            notifyBroker = nonAccelerableSegments.isFinished();
        }

        if (notifyBroker) {
            if (vFpgaManagers.isEmpty()) {
                cloudlet.getSegments().stream().peek(segment -> segment.addFinishedLengthSoFar(segment.getLength()));
            } else
                for (AccelerableSegment segment : accelerableSegments)
                    if (!segment.isFinished()) {
                        notifyBroker = false;
                        break;
                    }
        }
        if (notifyBroker) {
            double minStartTime = Double.MAX_VALUE;
            double maxFinishTime = Double.MIN_VALUE;

            minStartTime = Math.min(minStartTime, nonAccelerableSegments.getExecStartTime());
            maxFinishTime = Math.max(maxFinishTime, nonAccelerableSegments.getFinishTime());

            if (!vFpgaManagers.isEmpty())
                for (AccelerableSegment segment : accelerableSegments) {
                    minStartTime = Math.min(minStartTime, segment.getExecution().getSegmentArrivalTime());
                    maxFinishTime = Math.max(maxFinishTime,
                            segment.getExecution().getFinishTime());
                }

            cloudlet.setExecStartTime(minStartTime);
            cloudlet.setOverallFinishTime(maxFinishTime);
            sendNow(broker, CloudSimTags.ALL_SEGMENTS_MERGED_RETURN, cloudlet);
        }
    }

    public void processVFpgaInfo(Payload payload) {
        VFpga vFpga = (VFpga) payload.getData().get(0);
        LOGGER.info(
                "{}: {}: vFPGA {} successfully created on FPGA {}. Configuration time: {} seconds. Assigned IP" +
                        "Address: {}",
                getSimulation().clockStr(), getClass().getSimpleName(), vFpga.getId(),
                vFpga.getManager().getFpga().getId(),
                format2DecimalPlaces(vFpga.getConfigurationTime()), vFpga.getIpAddress());
        sendNow(this, CloudSimTags.VFPGA_SEGMENT_SUBMIT, vFpga);
        sendNow(broker, CloudSimTags.VFPGA_RECONFIGURATION_FINISH, vFpga);
    }

    public void processVFpgaOutput(Payload payload) {
        SegmentExecution segmentExecution = (SegmentExecution) payload.getData().get(0);
        VFpga vFpga = (VFpga) payload.getData().get(1);

        AccelerableSegment segment = segmentExecution.getSegment();
        if (segment != null) {
            double executionTime = segmentExecution.getFinishTime() - segmentExecution.getSegmentArrivalTime();

            submittedSegmentList.remove(segment);
            finishedSegmentList.add(segment);
            segmentExecutionList.add(segmentExecution);

            LOGGER.info("{}: {}: Accelerable segment {} finished. vFPGA {} successfully destroyed. " +
                            "Execution time: {} seconds",
                    getSimulation().clockStr(),
                    getClass().getSimpleName(), segmentExecution.getSegment().getUniqueId(), vFpga.getId(),
                    executionTime);

            // destroy vFPGA in other FPGAs if it is configured across multiple FPGAs
            for (VFpgaManager manager : vFpgaManagers)
                if (!vFpga.getManager().equals(manager)) {
                    List<VFpga> combinedList = manager.getAllVirtualFPGAs();
                    for (int i = 0; i < combinedList.size(); i++) {
                        VFpga sameVFpga = combinedList.get(i);
                        if (vFpga.getId() == sameVFpga.getId()) {
                            sameVFpga.setCreatedAt(vFpga.getCreatedAt());
                            sameVFpga.setConfigurationTime(vFpga.getConfigurationTime());
                            manager.destroyVFpga(sameVFpga, vFpga.getDestroyedAt());
                            break;
                        }
                    }
                }

            sendNow(broker, CloudSimTags.VFPGA_SEGMENT_FINISH, segment);
        } else {
            LOGGER.info("{}: {}: No accelerable segment processed in vFPGA {}. vFPGA successfully destroyed. Creation" +
                            " time: {}", getSimulation().clockStr(), getClass().getSimpleName(),
                    vFpga.getId(),
                    format2DecimalPlaces(vFpga.getCreatedAt()));
        }
        trackCloudlet(segment.getCloudlet());
    }

    @Override
    public void sendDataToComponent(Payload payload) {
        Packet packet = (Packet) payload.getData().get(0);
        if (packet.getType() == Packet.VFPGA_INFO) {
            processVFpgaInfo(packet.getPayload());
        } else if (packet.getType() == Packet.VFPGA_OUTPUT) {
            processVFpgaOutput(packet.getPayload());
        }
    }

    public void printRegionScheduler(Object... options) {
        scheduler.print(options);
    }
}
