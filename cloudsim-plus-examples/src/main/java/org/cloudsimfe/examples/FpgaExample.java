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

import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.datacenters.Datacenter;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.hosts.HostSimple;
import org.cloudbus.cloudsim.resources.Pe;
import org.cloudbus.cloudsim.resources.PeSimple;
import org.cloudbus.cloudsim.vms.Vm;
import org.cloudbus.cloudsim.vms.VmSimple;
import org.cloudsimfe.*;

import java.util.ArrayList;
import java.util.List;

/**
 * A minimal but organized, structured and re-usable CloudSim Plus example
 * which shows good coding practices for creating simulation scenarios.
 *
 * <p>It defines a set of constants that enables a developer
 * to change the number of Hosts, VMs and Cloudlets to create
 * and the number of {@link Pe}s for Hosts, VMs and Cloudlets.</p>
 *
 * @author Manoel Campos da Silva Filho
 * @since CloudSim Plus 1.0
 */
public class FpgaExample {
    private static final int HOSTS = 1;
    private static final int HOST_CPU_CORES = 2;
    private static final int CPU_CAPACITY_MIPS = 1000;

    private static final int VMS = 1;
    private static final int VM_CPU_CORES = 1;

    private static final int CLOUDLETS = 1;
    private static final int CLOUDLET_LENGTH = 12000;
    private static final int ACCELERABLE_LENGTH = 10000;

    private static final int FPGAS = 1;
    private static final int FPGA_PARTITIONING_ROWS = 4;
    private static final int FPGA_PARTITIONING_COLS = 3;
    private static final int STATIC_REGIONS = 1;

    private static final int ACCELERATORS = 1;
    private static final int ACCELERATOR_REQUIRED_REGIONS = 3;
    private static final int ACCELERATOR_CONCURRENCY = 100;
    private static final int ACCELERATOR_CAPACITY_MFLOPS = 100;

    private final CloudSim simulation;
    private DatacenterBrokerFE broker0;
    private List<Vm> vmList;
    private List<Cloudlet> cloudletList;
    private DatacenterFE datacenter0;
    private List<Bitstream> imageList;
    private DhcpServer server;

    private FpgaExample() {
        /*Enables just some level of log messages.
          Make sure to import org.cloudsimplus.util.Log;*/
        //Log.setLevel(ch.qos.logback.classic.Level.WARN);

        server = new DhcpServer();

        simulation = new CloudSim();
        datacenter0 = createDatacenter();

        //Creates a broker that is a software acting on behalf a cloud customer to manage his/her VMs and Cloudlets
        broker0 = new DatacenterBrokerFE(simulation, "BrokerFE");

        vmList = createVms();
        imageList = createAcceleratorImages();
        cloudletList = createCloudlets();
        broker0.submitVmList(vmList);
        broker0.submitCloudletList(cloudletList);
        broker0.submitAcceleratorImageList(imageList);

        simulation.start();

        System.out.println();
        System.out.println();
        datacenter0.getFpgaList().get(0).getVFpgaManager().printPerspective(System.out);

//        final List<Cloudlet> finishedCloudlets = broker0.getCloudletFinishedList();
//        finishedCloudlets.removeIf(cloudlet -> cloudlet.getStatus().equals(Cloudlet.Status.INSTANTIATED));
//        final List<AccelerableCloudlet> finishedAccelerableCloudlets = new ArrayList<>();
//        for (Cloudlet cloudlet : finishedCloudlets)
//            finishedAccelerableCloudlets.add((AccelerableCloudlet) cloudlet);
//        new AccelerableCloudletsTableBuilder(finishedAccelerableCloudlets).build();
//        new CloudletsTableBuilder(finishedCloudlets).build();
    }

    public static void main(String[] args) {
        new FpgaExample();
    }

    public Fpga createAndPartitionFpga(int id) {
        PartitionPolicy partitionPolicy = new PartitionPolicyGrid();
        partitionPolicy.setOption(FPGA_PARTITIONING_ROWS, PartitionPolicyGrid.OPTION_GRID_ROWS);
        partitionPolicy.setOption(FPGA_PARTITIONING_COLS, PartitionPolicyGrid.OPTION_GRID_COLS);

        Fpga fpga = new Fpga.Builder(simulation, id, server)
                .setBrand("Altera")
                .setModel("Stratix V 5SGSD3")
                .setLogicElements(325000)
                .setMemoryRegisters(356000)
                .setBlockedRams(600)
                .setDspSlices(1800)
                .setIoPins(450)
                .setTransceivers(12)
                .setPhaseLockedLoops(16)
                .setLength(44)
                .setWidth(33)
                .setClock(150)
                .setPartitionPolicy(partitionPolicy)
                .setStaticRegionCount(STATIC_REGIONS)
                .build();
        return fpga;
    }

    /**
     * Creates a Datacenter and its Hosts.
     */
    private DatacenterFE createDatacenter() {
        final List<Host> hostList = new ArrayList<>(HOSTS);
        for (int i = 0; i < HOSTS; i++) {
            Host host = createHost();
            hostList.add(host);
        }

        final List<Fpga> fpgaList = new ArrayList<>();
        for (int i = 0; i < FPGAS; i++)
        fpgaList.add(createAndPartitionFpga(i + 1));

        //Uses a VmAllocationPolicySimple by default to allocate VMs
        return new DatacenterFE(simulation, hostList, fpgaList, server);
    }

    public List<Bitstream> createAcceleratorImages() {
        List<Bitstream> imageList = new ArrayList<>();

        for (int i = 0; i < ACCELERATORS; i++) {
            Accelerator accelerator0 = new Accelerator(Accelerator.CURRENT_ACCELERATOR_ID++, ACCELERATOR_CAPACITY_MFLOPS,
                    ACCELERATOR_CONCURRENCY,
                    Accelerator.TYPE_IMAGE_PROCESSING);
            accelerator0.setBroker(broker0);
            Bitstream bitstream0 = new Bitstream(new Wrapper(), accelerator0, ACCELERATOR_REQUIRED_REGIONS, 2);
            imageList.add(bitstream0);
        }

//        Accelerator accelerator1 = new Accelerator(Accelerator.CURRENT_ACCELERATOR_ID++, mflops, concurrency,
//                Accelerator.TYPE_ENCRYPTION);
//        accelerator1.setBroker(broker0);
//        Bitstream bitstream1 = new Bitstream(new Wrapper(), accelerator1, 2, 3);
//        imageList.add(bitstream1);

        return imageList;
    }

    private Host createHost() {
        final List<Pe> peList = new ArrayList<>(HOST_CPU_CORES);
        //List of Host's CPUs (Processing Elements, PEs)
        for (int i = 0; i < HOST_CPU_CORES; i++) {
            //Uses a PeProvisionerSimple by default to provision PEs for VMs
            peList.add(new PeSimple(CPU_CAPACITY_MIPS));
        }

        final long ram = 2048; //in Megabytes
        final long bw = 10000; //in Megabits/s
        final long storage = 1000000; //in Megabytes

        /*
        Uses ResourceProvisionerSimple by default for RAM and BW provisioning
        and VmSchedulerSpaceShared for VM scheduling.
        */
        return new HostSimple(ram, bw, storage, peList);
    }

    /**
     * Creates a list of VMs.
     */
    private List<Vm> createVms() {
        final List<Vm> list = new ArrayList<>(VMS);
        for (int i = 0; i < VMS; i++) {
            //Uses a CloudletSchedulerTimeShared by default to schedule Cloudlets
            final Vm vm = new VmSimple(CPU_CAPACITY_MIPS, VM_CPU_CORES);
            vm.setRam(512).setBw(1000).setSize(10000);
            list.add(vm);
        }

        return list;
    }

    /**
     * Creates a list of Cloudlets.
     */
    private List<Cloudlet> createCloudlets() {
        final List<Cloudlet> list = new ArrayList<>(CLOUDLETS);

        for (int i = 0; i < CLOUDLETS; i++) {
            AccelerableCloudlet accelerableCloudlet = new AccelerableCloudlet(CLOUDLET_LENGTH,
                    VM_CPU_CORES);

            long indexPosition = 2000; // meaning cloudlet can be accelerated only
            // from 2000th instruction onward

            accelerableCloudlet.addSegment(indexPosition, ACCELERABLE_LENGTH, Accelerator.TYPE_IMAGE_PROCESSING);
            list.add(accelerableCloudlet);
        }

        return list;
    }
}
