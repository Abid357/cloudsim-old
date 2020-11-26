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

public class AlAghbariExample {

    private final CloudSim simulation;
    private DatacenterBrokerFE broker0;
    private DatacenterFE datacenter0;
    private DhcpServer server;
    private NetlistStore store;

    private AlAghbariExample() {
        server = new DhcpServer();
        store = new NetlistStore();

        simulation = new CloudSim(0.0001);

        // CREATE HOST
        double mipsHost = 3000; // converted from MHz to MIPS
        long ram = 16000; // in MB equivalent of 16GB
        long bandwidth = 10000; // in Mbps equivalent of 10GE Ethernet
        long storage = 100000; // in MB equivalent of 100GB
        int cores = 8;
        List<Pe> peList = new ArrayList<>();

        for (int i = 0; i < cores; i++)
            peList.add(new PeSimple(mipsHost));
        Host host = new HostSimple(ram, bandwidth, storage, peList);

        // CREATE FPGA
        PartitionPolicy partitionPolicy = new PartitionPolicyGrid();
        partitionPolicy.setOption(2, PartitionPolicyGrid.OPTION_GRID_ROWS);
        partitionPolicy.setOption(2, PartitionPolicyGrid.OPTION_GRID_COLS);

        Fpga fpga = new Fpga.Builder(simulation, 1, server)
                .setBrand("Xilinx")
                .setFamily("Virtex-6")
                .setModel("XC6VLX550T")
                .setLogicElements(549888)
                .setMemoryRegisters(22752)
                .setBlockedRams(632)
                .setDspSlices(864)
                .setIoPins(1200)
                .setTransceivers(40)
                .setPhaseLockedLoops(18)
                .setLength(43)
                .setWidth(43)
                .setClock(600)
                .setConfigurationClock(100)
                .setConfigurationBusWidth(ConfigurationManager.BUS_WIDTH_16_BIT)
                .setPartitionPolicy(partitionPolicy)
                .setStaticRegionCount(1)
                .build();

        // CREATE DATACENTER AND BROKER
        datacenter0 = new DatacenterFE(simulation, Arrays.asList(host), Arrays.asList(fpga), server);
        broker0 = new DatacenterBrokerFE(simulation, "BrokerFE");

        // CREATE VM
        double mipsVm = 3000;
        Vm vm = new VmSimple(mipsVm, 1);
        vm.setRam(4000).setBw(1000).setSize(10000).setCloudletScheduler(new CloudletSchedulerSpaceShared());

        // CREATE ACCELERATOR
        Accelerator imageProcessor = new Accelerator(1, 400,
                40, Accelerator.TYPE_IMAGE_PROCESSING);
        imageProcessor.setBroker(broker0);

        Netlist netlist = new Netlist(imageProcessor, 2, 1, 3, 5);
        store.addNetlist(netlist);

        // CREATE CLOUDLET
        AccelerableCloudlet image1 = new AccelerableCloudlet(52,
                1);
        image1.addSegment(4, 48, Accelerator.TYPE_IMAGE_PROCESSING);

        AccelerableCloudlet image2 = new AccelerableCloudlet(61, 1);
        image2.addSegment(3, 58, Accelerator.TYPE_IMAGE_PROCESSING);

        AccelerableCloudlet image3 = new AccelerableCloudlet(66, 1);
        image3.addSegment(3, 63, Accelerator.TYPE_IMAGE_PROCESSING);

        AccelerableCloudlet image4 = new AccelerableCloudlet(77, 1);
        image4.addSegment(9, 68, Accelerator.TYPE_IMAGE_PROCESSING);

        AccelerableCloudlet image5 = new AccelerableCloudlet(105, 1);
        image5.addSegment(7, 98, Accelerator.TYPE_IMAGE_PROCESSING);

        AccelerableCloudlet image6 = new AccelerableCloudlet(100, 1);
        image6.addSegment(14, 86, Accelerator.TYPE_IMAGE_PROCESSING);

        AccelerableCloudlet image7 = new AccelerableCloudlet(105, 1);
        image7.addSegment(18, 87, Accelerator.TYPE_IMAGE_PROCESSING);

        AccelerableCloudlet image8 = new AccelerableCloudlet(108, 1);
        image8.addSegment(20, 88, Accelerator.TYPE_IMAGE_PROCESSING);

        AccelerableCloudlet image9 = new AccelerableCloudlet(135, 1);
        image9.addSegment(21, 115, Accelerator.TYPE_IMAGE_PROCESSING);

        AccelerableCloudlet image10 = new AccelerableCloudlet(620, 1);
        image10.addSegment(41, 579, Accelerator.TYPE_IMAGE_PROCESSING);

        List<AccelerableCloudlet> images = Arrays.asList(image1, image2, image3, image4, image5, image6, image7,
                image8, image9, image10);
        List<Accelerator> requestedAccelerators = createAcceleratorRequests(images);

        // SETUP
        broker0.submitVmList(Arrays.asList(vm));
        broker0.submitCloudletList(images);
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
        new AlAghbariExample();
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