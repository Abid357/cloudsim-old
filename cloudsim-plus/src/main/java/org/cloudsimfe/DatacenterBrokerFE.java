package org.cloudsimfe;/*
 * Title:        CloudSim Toolkit
 * Description:  CloudSim (Cloud Simulation) Toolkit for Modeling and Simulation of Clouds
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2009-2012, The University of Melbourne, Australia
 */

import org.cloudbus.cloudsim.brokers.DatacenterBroker;
import org.cloudbus.cloudsim.brokers.DatacenterBrokerAbstract;
import org.cloudbus.cloudsim.brokers.DatacenterBrokerSimple;
import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.core.*;
import org.cloudbus.cloudsim.core.events.CloudSimEvent;
import org.cloudbus.cloudsim.core.events.SimEvent;
import org.cloudbus.cloudsim.datacenters.Datacenter;
import org.cloudbus.cloudsim.datacenters.TimeZoned;
import org.cloudbus.cloudsim.schedulers.cloudlet.CloudletScheduler;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModel;
import org.cloudbus.cloudsim.vms.Vm;
import org.cloudbus.cloudsim.vms.VmGroup;
import org.cloudbus.cloudsim.vms.VmSimple;
import org.cloudsimplus.autoscaling.VerticalVmScaling;
import org.cloudsimplus.listeners.DatacenterBrokerEventInfo;
import org.cloudsimplus.listeners.EventInfo;
import org.cloudsimplus.listeners.EventListener;
import org.cloudsimplus.traces.google.GoogleTaskEventsTraceReader;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

import static java.util.Comparator.comparing;
import static java.util.Objects.requireNonNull;

/**
 * An abstract class to be used as base for implementing a {@link DatacenterBroker}.
 *
 * @author Rodrigo N. Calheiros
 * @author Anton Beloglazov
 * @author Manoel Campos da Silva Filho
 */
public class DatacenterBrokerFE extends DatacenterBrokerSimple {

    private final List<Bitstream> imageList;

    /**
     * Creates a DatacenterBroker giving a specific name.
     * Subclasses usually should provide this constructor and
     * and overloaded version that just requires the {@link CloudSim} parameter.
     *
     * @param simulation the CloudSim instance that represents the simulation the Entity is related to
     * @param name       the DatacenterBroker name
     */
    public DatacenterBrokerFE(CloudSim simulation, String name) {
        super(simulation, name);
        this.imageList = new ArrayList<>();
    }

    @Override
    public void startEntity() {
        super.startEntity();

        if (!imageList.isEmpty()) {
            LOGGER.info(
                    "{}: {}: List of {} accelerators submitted to the broker during simulation execution. " +
                            "Accelerators creation request sent to Datacenter.",
                    getSimulation().clockStr(), getName(), imageList.size());
        }

    }

    public DatacenterBroker submitAcceleratorImageList(final List<Bitstream> bitstreams) {
        imageList.addAll(bitstreams);
        return this;
    }

    @Override
    public void processEvent(SimEvent evt) {
        super.processEvent(evt);
        if (evt.getTag() == CloudSimTags.DATACENTER_LIST_REQUEST) {
            requestAcceleratorCreation();
        }
    }

    private int requestAcceleratorCreation() {
        DatacenterFE datacenter = (DatacenterFE) getDatacenterList().get(0);
        double submissionDelay =
                imageList.stream().max(comparing(bitstream -> bitstream.getAccelerator().getSubmissionDelay())).get().getAccelerator().getSubmissionDelay();

        LOGGER.info(
                "{}: {}: Invoking region scheduler in {}",
                getSimulation().clockStr(), getName(), datacenter.getName());
        send(datacenter.getUnifiedManager(), submissionDelay, CloudSimTags.SCHEDULE_PARTITIONED_REGIONS,
                imageList);

        imageList.forEach(bitstream -> bitstream.getAccelerator().setLastTriedDatacenter(datacenter));
        return imageList.size();
    }
}