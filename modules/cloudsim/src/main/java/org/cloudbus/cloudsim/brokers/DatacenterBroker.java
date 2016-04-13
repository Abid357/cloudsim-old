package org.cloudbus.cloudsim.brokers;

import java.util.List;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Vm;

/**
 * An interface to be implemented by each class that provides
 * DatacenterBroker features.
 * 
 * @author Manoel Campos da Silva Filho
 */
public interface DatacenterBroker {
    int getId();
    
    String getName();

    /**
     * Specifies that a given cloudlet must run in a specific virtual machine.
     *
     * @param cloudletId ID of the cloudlet being bount to a vm
     * @param vmId the vm id
     * @pre cloudletId > 0
     * @pre id > 0
     * @post $none
     * @todo @author manoelcampos This method would receive a Cloudlet object
     * because it is just setting the vmId cloudlet attribute.
     * When the method is called prior to call {@link DatacenterBroker#submitCloudletList(java.util.List)},
     * it tries to locate the cloudlet in the submitted list
     * and, when it doesn't exist yet, it is thrown a NullPointerException.
     * At leat, an overloaded version of the method would be created
     * and this one would try to find the cloudlet and, when
     * it is not found, thrown an specific exception
     * asking if the cloudlet already was submitted.
     */
    void bindCloudletToVm(int cloudletId, int vmId);

    /**
     * Gets the cloudlet list.
     *
     * @param <T>
     * @return the cloudlet list
     */
    <T extends Cloudlet> List<T> getCloudletList();

    /**
     * Gets the cloudlet received list.
     *
     * @param <T>
     * @return the cloudlet received list
     */
    <T extends Cloudlet> List<T> getCloudletReceivedList();

    Vm getVm(final int index);

    /**
     * Gets the list of VMs submitted to the broker.
     *
     * @param <T>
     * @return the list of submitted VMs
     */
    <T extends Vm> List<T> getVmList();

    /**
     * Gets the vm list.
     *
     * @param <T>
     * @return the vm list
     */
    <T extends Vm> List<T> getVmsCreatedList();

    /**
     * This method is used to send to the broker the list of cloudlets.
     *
     * @param <T>
     * @param list the list
     * @pre list !=null
     * @post $none
     *
     * @todo The name of the method is confused with the {@link #submitCloudlets()},
     * that in fact submit cloudlets to VMs. The term "submit" is being used
     * ambiguously. The method {@link #submitCloudlets()} would be named "sendCloudletsToVMs"
     *
     * The method {@link #submitVmList(java.util.List)} may have
     * be checked too.
     */
    <T extends Cloudlet> void submitCloudletList(List<T> list);

    /**
     * This method is used to send to the broker the list with virtual machines that must be
     * created.
     *
     * @param <T>
     * @param list the list
     * @pre list !=null
     * @post $none
     */
    <T extends Vm> void submitVmList(List<T> list);
    
}
