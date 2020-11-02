/*
 * CloudSim Plus: A modern, highly-extensible and easier-to-use Framework for
 * Modeling and Simulation of Cloud Computing Infrastructures and Services.
 * http://cloudsimplus.org
 *
 *     Copyright (C) 2015-2018 Universidade da Beira Interior (UBI, Portugal) and
 *     the Instituto Federal de Educação Ciência e Tecnologia do Tocantins (IFTO, Brazil).
 *
 *     This file is part of CloudSim Plus.
 *
 *     CloudSim Plus is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     CloudSim Plus is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with CloudSim Plus. If not, see <http://www.gnu.org/licenses/>.
 */
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

/**
 * A minimal but organized, structured and re-usable CloudSim Plus example
 * which shows good coding practices for creating simulation scenarios.
 *
 * <p>It defines a set of constants that enables a developer
 * to change the number of Hosts, VMs and Cloudlets to create
 * and the number of {@link Pe}s for Hosts, VMs and Cloudlets.</p>
 *
 * @author Abid Farhan
 */
public class MFMAExample {

    private final CloudSim simulation;
    private DatacenterBrokerFE broker0;
    private DatacenterFE datacenter0;
    private DhcpServer server;
    private NetlistStore store;

    private MFMAExample() {
        server = new DhcpServer();
        store = new NetlistStore();

        simulation = new CloudSim(0.0001);

        // CREATE HOST
        double mipsHost = 3000; // converted from MHz to MIPS
        long ram = 16000; // in MB equivalent of 16GB
        long bandwidth = 10000; // in Mbps equivalent of 10GE Ethernet
        long storage = 100000; // in MB equivalent of 100GB
        int cores = 12;
        List<Pe> peList = new ArrayList<>();

        for (int i = 0; i < cores; i++)
            peList.add(new PeSimple(mipsHost));
        Host host = new HostSimple(ram, bandwidth, storage, peList);

        // CREATE FPGA
        PartitionPolicy partitionPolicy = new PartitionPolicyGrid();
        partitionPolicy.setOption(3, PartitionPolicyGrid.OPTION_GRID_ROWS);
        partitionPolicy.setOption(4, PartitionPolicyGrid.OPTION_GRID_COLS);

        Fpga fpga1 = new Fpga.Builder(simulation, 1, server)
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
                .setConfigurationBusWidth(ConfigurationManager.BUS_WIDTH_32_BIT)
                .setPartitionPolicy(partitionPolicy)
                .setStaticRegionCount(2)
                .build();

        Fpga fpga2 = new Fpga.Builder(simulation, 2, server)
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
                .setStaticRegionCount(1)
                .build();

        Fpga fpga3 = new Fpga.Builder(simulation, 3, server)
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
                .setStaticRegionCount(1)
                .build();

        // CREATE DATACENTER AND BROKER
        List<Fpga> fpgaList = new ArrayList<>();
        fpgaList.addAll(Arrays.asList(fpga1, fpga2, fpga3));

        List<Fpga> scalableFpgaList = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            if (i % 5 == 0)
                scalableFpgaList.add(fpgaList.get(2).copy(simulation, i + 1, server, partitionPolicy));
            else
                scalableFpgaList.add(fpgaList.get(i % fpgaList.size()).copy(simulation, i + 1, server, partitionPolicy));
        }

        datacenter0 = new DatacenterFE(simulation, Arrays.asList(host), scalableFpgaList, server);
        datacenter0.setName("DatacenterFE");
        broker0 = new DatacenterBrokerFE(simulation, "BrokerFE");

        // CREATE VM
        double mipsVm = 3000;
        Vm vm = new VmSimple(mipsVm, 1);
        vm.setRam(4000).setBw(1000).setSize(10000).setCloudletScheduler(new CloudletSchedulerSpaceShared());

        // CREATE ACCELERATOR
        Accelerator imageAccelerator = new Accelerator(1, 400,
                40, Accelerator.TYPE_IMAGE_PROCESSING);
        imageAccelerator.setBroker(broker0);

        Accelerator videoAccelerator = new Accelerator(2, 500,
                30, Accelerator.TYPE_VIDEO_PROCESSING);
        videoAccelerator.setClock(250);
        videoAccelerator.setBroker(broker0);

        Accelerator encryptionAccelerator = new Accelerator(3, 100,
                100, Accelerator.TYPE_ENCRYPTION);
        encryptionAccelerator.setClock(100);
        encryptionAccelerator.setBroker(broker0);

        store.addNetlist(new Netlist(imageAccelerator, 4, 1, 2, 1));
        store.addNetlist(new Netlist(videoAccelerator, 10, 1, 3, 3));
        store.addNetlist(new Netlist(encryptionAccelerator, 3, 1, 4, 1));

        // CREATE CLOUDLET
        AccelerableCloudlet videoFrames = new AccelerableCloudlet(6000,
                1);
        videoFrames.addSegment(1000, 4500, Accelerator.TYPE_VIDEO_PROCESSING);

        AccelerableCloudlet image1 = new AccelerableCloudlet(2500,
                1);
        image1.addSegment(500, 2000, Accelerator.TYPE_IMAGE_PROCESSING);

        AccelerableCloudlet image2 = new AccelerableCloudlet(2500,
                1);
        image2.addSegment(500, 2000, Accelerator.TYPE_IMAGE_PROCESSING);

        AccelerableCloudlet encryptionKey = new AccelerableCloudlet(10000,
                1);
        encryptionKey.addSegment(1000, 7000, Accelerator.TYPE_ENCRYPTION);
        encryptionKey.addSegment(9000, 1000, Accelerator.TYPE_ENCRYPTION);

        List<AccelerableCloudlet> cloudlets = Arrays.asList(videoFrames, image1, image2, encryptionKey);
        List<Accelerator> requestedAccelerators = createAcceleratorRequests(cloudlets);

        // SETUP
        broker0.submitVmList(Arrays.asList(vm));
        broker0.submitCloudletList(cloudlets);
        broker0.submitAcceleratorRequests(requestedAccelerators);
        datacenter0.getUnifiedManager().setBroker(broker0);
        datacenter0.getUnifiedManager().setNetlistStore(store);
        simulation.start();

//        fpga.getVFpgaManager().printScheduledTiles();
//        new SegmentsTableBuilder(broker0.getFinishedSegments()).setTitle("Acceleration Results").build();
        new AccelerableCloudletsTableBuilder(broker0.getCloudletFinishedList()).setTitle("Accelerable Cloudlet " +
                "Execution" +
                " Results").build();
        if (!datacenter0.getFpgaList().isEmpty() && datacenter0.getUnifiedManager().isSchedulingSuccessful()) {
            new VFpgaTableBuilder(broker0.getVirtualFpgas()).setTitle("Virtual FPGAs").build();
            new ResourceUtilizationTableBuilder(broker0.getVirtualFpgas()).setTitle("Resource Utilization").build();
        }
    }

    public static void main(String[] args) {
        new MFMAExample();
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