package org.cloudsimfe;/*
 * Title:        CloudSim Toolkit
 * Description:  CloudSim (Cloud Simulation) Toolkit for Modeling and Simulation of Clouds
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2009-2012, The University of Melbourne, Australia
 */

import org.cloudbus.cloudsim.brokers.DatacenterBroker;
import org.cloudbus.cloudsim.brokers.DatacenterBrokerSimple;
import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.core.*;
import org.cloudbus.cloudsim.core.events.SimEvent;

import java.util.*;

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

    private final List<Accelerator> acceleratorRequests;

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
        this.acceleratorRequests = new ArrayList<>();
    }

    @Override
    public void startEntity() {
        super.startEntity();

        if (!acceleratorRequests.isEmpty()) {
            LOGGER.info(
                    "{}: {}: List of {} accelerators submitted to the broker during simulation execution. " +
                            "Accelerators creation request sent to Datacenter.",
                    getSimulation().clockStr(), getName(), acceleratorRequests.size());
        }

    }

    public DatacenterBroker submitAcceleratorRequests(final List<Accelerator> accelerators) {
        acceleratorRequests.addAll(accelerators);
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
                acceleratorRequests.stream().max(comparing(accelerator -> accelerator.getSubmissionDelay())).get().getSubmissionDelay();

        LOGGER.info(
                "{}: {}: Invoking region scheduler in {}",
                getSimulation().clockStr(), getName(), datacenter.getName());
        send(datacenter.getUnifiedManager(), submissionDelay, CloudSimTags.SCHEDULE_PARTITIONED_REGIONS,
                acceleratorRequests);

        acceleratorRequests.forEach(accelerator -> accelerator.setLastTriedDatacenter(datacenter));
        return acceleratorRequests.size();
    }
}