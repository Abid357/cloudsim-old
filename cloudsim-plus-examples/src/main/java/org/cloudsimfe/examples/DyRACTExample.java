package org.cloudsimfe.examples;

import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.hosts.HostSimple;
import org.cloudbus.cloudsim.resources.Pe;
import org.cloudbus.cloudsim.resources.PeSimple;
import org.cloudbus.cloudsim.schedulers.cloudlet.CloudletSchedulerSpaceShared;
import org.cloudbus.cloudsim.vms.Vm;
import org.cloudbus.cloudsim.vms.VmSimple;
import org.cloudsimfe.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DyRACTExample {

    private final CloudSim simulation;
    private DatacenterBrokerFE broker0;
    private DatacenterFE datacenter0;
    private DhcpServer server;
    private NetlistStore store;

    private DyRACTExample() {
        server = new DhcpServer();
        store = new NetlistStore();

        simulation = new CloudSim(0.0001);

        // CREATE HOST
        double mipsHost = 3200; // converted from MHz to MIPS
        long ram = 16000; // in MB equivalent of 16GB
        long bandwidth = 1000; // in Mbps equivalent of 1GE Ethernet
        long storage = 100000; // in MB equivalent of 100GB
        int cores = 6;
        List<Pe> peList = new ArrayList<>();

        for (int i = 0; i < cores; i++)
            peList.add(new PeSimple(mipsHost));
        Host host = new HostSimple(ram, bandwidth, storage, peList);

        // CREATE FPGA
        PartitionPolicy partitionPolicy = new PartitionPolicyGrid();
        partitionPolicy.setOption(4, PartitionPolicyGrid.OPTION_GRID_ROWS);
        partitionPolicy.setOption(4, PartitionPolicyGrid.OPTION_GRID_COLS);

        Fpga fpga1 = new Fpga.Builder(simulation, 1, server)
                .setBrand("Xilinx")
                .setFamily("Virtex-6")
                .setModel("XC6VLX240T")
                .setLogicElements(241152)
                .setMemoryRegisters(14976)
                .setBlockedRams(416)
                .setDspSlices(768)
                .setIoPins(720)
                .setTransceivers(24)
                .setPhaseLockedLoops(12)
                .setLength(45)
                .setWidth(45)
                .setClock(600)
                .setConfigurationClock(100)
                .setConfigurationBusWidth(ConfigurationManager.BUS_WIDTH_32_BIT)
                .setPartitionPolicy(partitionPolicy)
                .setStaticRegionCount(2)
                .build();

        Fpga fpga2 = new Fpga.Builder(simulation, 2, server)
                .setBrand("Xilinx")
                .setFamily("Virtex-7")
                .setModel("XC7VX485T")
                .setLogicElements(485760)
                .setMemoryRegisters(37080)
                .setBlockedRams(1030)
                .setDspSlices(2800)
                .setIoPins(700)
                .setTransceivers(56)
                .setPhaseLockedLoops(14)
                .setLength(45)
                .setWidth(45)
                .setClock(600)
                .setConfigurationClock(100)
                .setConfigurationBusWidth(ConfigurationManager.BUS_WIDTH_32_BIT)
                .setPartitionPolicy(partitionPolicy)
                .setStaticRegionCount(2)
                .build();

        // CREATE DATACENTER AND BROKER
        datacenter0 = new DatacenterFE(simulation, Arrays.asList(host), Arrays.asList(fpga1, fpga2), server);
        datacenter0.setName("DatacenterFE");
        broker0 = new DatacenterBrokerFE(simulation, "BrokerFE");

        // CREATE VM
        double mipsVm = 3200;
        Vm vm = new VmSimple(mipsVm, 1);
        vm.setRam(4000).setBw(1000).setSize(10000).setCloudletScheduler(new CloudletSchedulerSpaceShared());

        // CREATE ACCELERATOR
        Accelerator videoProcessor = new Accelerator(2, 500,
                30, Accelerator.TYPE_VIDEO_PROCESSING);
        videoProcessor.setClock(250);
        videoProcessor.setBroker(broker0);

        Netlist netlist = new Netlist(videoProcessor, 20, 1, 2, 1);
        store.addNetlist(netlist);

        // CREATE CLOUDLET
        AccelerableCloudlet frames = new AccelerableCloudlet(6000,
                1);
        frames.addSegment(1261, 4739, Accelerator.TYPE_VIDEO_PROCESSING);

        List<AccelerableCloudlet> videoFrames = Arrays.asList(frames);
        List<Accelerator> requestedAccelerators = createAcceleratorRequests(videoFrames);

        // SETUP
        broker0.submitVmList(Arrays.asList(vm));
        broker0.submitCloudletList(videoFrames);
        broker0.submitAcceleratorRequests(requestedAccelerators);
        datacenter0.getUnifiedManager().setBroker(broker0);
        datacenter0.getUnifiedManager().setNetlistStore(store);
        simulation.start();

        new AccelerableCloudletsTableBuilder(broker0.getCloudletFinishedList()).setTitle("Accelerable Cloudlet " +
                "Execution" +
                " Results").build();
        if (!datacenter0.getFpgaList().isEmpty() && datacenter0.getUnifiedManager().isSchedulingSuccessful()) {
            new VFpgaTableBuilder(broker0.getVirtualFpgas()).setTitle("Virtual FPGAs").build();
            new ResourceUtilizationTableBuilder(broker0.getVirtualFpgas()).setTitle("Resource Utilization").build();
        }
    }

    public static void main(String[] args) {
        new DyRACTExample();
    }

    public List<Accelerator> createAcceleratorRequests(List<AccelerableCloudlet> list) {
        List<Accelerator> requestedAccelerators = new ArrayList<>();
        for (AccelerableCloudlet cloudlet : list) {
            AccelerableSegment segment = cloudlet.getNextSegment();
            while (segment != null) {
                int acceleratorId = store.hasAcceleratorType(segment.getType());
                if (acceleratorId != -1)
                    requestedAccelerators.add(store.getNetlist(acceleratorId).getAccelerator());
                segment = cloudlet.getNextSegment();
            }
        }
        return requestedAccelerators;
    }
}