package org.cloudsimfe;

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
    private List<VFpgaManager> vFpgaManagers;
    private List<AccelerableSegment> waitingSegmentList;
    private List<AccelerableSegment> submittedSegmentList;
    private List<AccelerableSegment> finishedSegmentList;
    private List<SegmentExecution> segmentExecutionList;
    private List<Bitstream> imageList;
    private String ipAddress;
    private RegionScheduler scheduler;
    private DecimalFormat format;

    public UnifiedManager(Simulation simulation, List<VFpgaManager> vFpgaManagers) {
        super(simulation);
        this.vFpgaManagers = vFpgaManagers;
        waitingSegmentList = new ArrayList<>();
        submittedSegmentList = new ArrayList<>();
        finishedSegmentList = new ArrayList<>();
        segmentExecutionList = new ArrayList<>();
        scheduler = new RegionSchedulerSA(this);
        format = new DecimalFormat("##########.00");
    }

    public Datacenter getDatacenter() {
        return datacenter;
    }

    public void setDatacenter(Datacenter datacenter) {
        this.datacenter = datacenter;
    }

    public Bitstream getImageByJobId(int jobId) {
        return imageList.get(jobId - 1);
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
        if (nextSimulationTime != Double.MAX_VALUE)
            send(this, nextSimulationTime, CloudSimTags.VFPGA_UPDATE_SEGMENT_PROCESSING);
        return nextSimulationTime;
    }

    public Bitstream getImageById(int id) {
        return imageList.get(id - 1);
    }

    public void updateHypervisors(int timeSlots, int[] bestSolution) {
        int b = 0;
        int regionCount = getTotalRegionCount();

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
        this.imageList = (List<Bitstream>) evt.getData();

        if (vFpgaManagers.isEmpty()){
            sendNow(datacenter, CloudSimTags.REGION_SCHEDULING_FAIL);
            return;
        }

        List<Job> jobs = new ArrayList<>();
        for (Bitstream bitstream : imageList) {
            int id = Job.JOB_ID++;
            int tile = bitstream.getRequiredBlockCount();
            int executionTime = bitstream.getRequiredExecutionTime();
            int deadline = bitstream.getDeadline();
            jobs.add(new Job(id, tile, executionTime, deadline));
        }

        int regionCount = getTotalRegionCount();
        scheduler.scheduleRegions(vFpgaManagers.size(), regionCount, jobs);
        send(datacenter, getSchedulingDurationInSeconds(), CloudSimTags.REGION_SCHEDULING_FINISH,
                scheduler.getSchedulingDuration());
    }

    public int getTotalRegionCount() {
        int count = 0;
        for (VFpgaManager vFpgaManager : vFpgaManagers)
            count += vFpgaManager.getReconfigurableRegionCount();
        return count;
    }

    private void processReconfigurationRequest(SimEvent evt) {
        Payload payload = (Payload) evt.getData();
        int vFpgaId = ((Integer) payload.getData().get(0)).intValue();
        int row = ((Integer) payload.getData().get(1)).intValue();
        VFpgaManager vFpgaManager = (VFpgaManager) payload.getData().get(2);

        Bitstream bitstream = getImageById(vFpgaId);
        List<Mapper> mappersForNextVFpga = new ArrayList<>();
        for (int i = 0; i < bitstream.getRequiredBlockCount(); i++) {
            mappersForNextVFpga.add(new Mapper(vFpgaId, row + i + 1));
        }

        List<Object> packetData = new ArrayList<>();
        packetData.add(bitstream);
        packetData.add(mappersForNextVFpga);
        packetData.add(vFpgaId);

        Packet packet = new Packet(getIpAddress(), NetworkManager.CONFIGURATION_MANAGER_ADDRESS,
                Packet.BITSTREAM, new Payload(packetData));
        vFpgaManager.getFpga().getNetworkManager().sendDataToComponent(new Payload(packet));
    }

    public void processSegmentSubmit(SimEvent evt) {
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

    public void processSegmentAck(SimEvent evt) {
        Payload payload = (Payload) evt.getData();
        AccelerableSegment segment = (AccelerableSegment) payload.getData().get(0);
        VFpga vFpga = (VFpga) payload.getData().get(1);

        double transferTime = 0.0;
        vFpga.getAccelerator().segmentSubmit(transferTime);
        LOGGER.info("{}: {}: Accelerable segment {} successfully received by VFPGA {} in FPGA {}.",
                getSimulation().clockStr(),
                getClass().getSimpleName(), segment.getUniqueId(), vFpga.getId(), vFpga.getManager().getFpga().getId());
        sendNow(this, CloudSimTags.VFPGA_UPDATE_SEGMENT_PROCESSING);
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

    public void processVFpgaInfo(Payload payload) {
        VFpga vFpga = (VFpga) payload.getData().get(0);
        LOGGER.info(
                "{}: {}: VFPGA {} successfully created on FPGA {}. Configuration time: {} seconds. Assigned IP" +
                        "Address: {}",
                getSimulation().clockStr(), getClass().getSimpleName(), vFpga.getId(),
                vFpga.getManager().getFpga().getId(),
                format2DecimalPlaces(vFpga.getConfigurationTime()), vFpga.getIpAddress());
        sendNow(this, CloudSimTags.VFPGA_SEGMENT_SUBMIT, vFpga);
    }

    public void processVFpgaOutput(Payload payload) {
        SegmentExecution segmentExecution = (SegmentExecution) payload.getData().get(0);
        VFpga vFpga = (VFpga) payload.getData().get(1);

        if (segmentExecution.getSegment() != null) {
            double executionTime = segmentExecution.getFinishTime() - segmentExecution.getSegmentArrivalTime();

            AccelerableSegment segment = segmentExecution.getSegment();
            submittedSegmentList.remove(segment);
            finishedSegmentList.add(segment);
            segmentExecutionList.add(segmentExecution);

            LOGGER.info("{}: {}: Accelerable segment {} finished. VFPGA {} successfully destroyed on FPGA {}. " +
                            "Execution time: {} seconds",
                    getSimulation().clockStr(),
                    getClass().getSimpleName(), segmentExecution.getSegment().getUniqueId(), vFpga.getId(),
                    vFpga.getManager().getFpga().getId(),
                    format2DecimalPlaces(executionTime));
        } else {
            LOGGER.info("{}: {}: No accelerable segment processed in VFPGA {}. VFPGA successfully destroyed on FPGA " +
                            "{}. Creation time: {}", getSimulation().clockStr(), getClass().getSimpleName(),
                    vFpga.getId(),
                    vFpga.getManager().getFpga().getId(),
                    format2DecimalPlaces(vFpga.getCreatedAt()));
        }
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
