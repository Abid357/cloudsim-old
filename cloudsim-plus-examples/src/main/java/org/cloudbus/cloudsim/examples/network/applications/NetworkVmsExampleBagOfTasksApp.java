package org.cloudbus.cloudsim.examples.network.applications;

import java.util.ArrayList;
import java.util.List;

import org.cloudbus.cloudsim.brokers.DatacenterBroker;
import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.cloudlets.network.CloudletSendTask;
import org.cloudbus.cloudsim.cloudlets.network.CloudletExecutionTask;
import org.cloudbus.cloudsim.cloudlets.network.CloudletReceiveTask;
import org.cloudbus.cloudsim.cloudlets.network.CloudletTask;
import org.cloudbus.cloudsim.cloudlets.network.NetworkCloudlet;
import org.cloudbus.cloudsim.vms.Vm;
import org.cloudbus.cloudsim.vms.network.NetworkVm;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModel;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModelFull;

/**
 * An example of "Bag of Tasks" application that is compounded by
 * 3 {@link NetworkCloudlet}, where 2 of them send data to the first created one,
 * that waits the data to be received.
 *
 * @author Saurabh Kumar Garg
 * @author Rajkumar Buyya
 * @author Manoel Campos da Silva Filho
 */
public class NetworkVmsExampleBagOfTasksApp extends NetworkVmExampleAbstract {
    private static final long CLOUDLET_TASK_MEMORY = 1000;
    private static final long NETWORK_CLOUDLET_LENGTH = 1;

    /**
     * Starts the execution of the example.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        new NetworkVmsExampleBagOfTasksApp();
    }

    public NetworkVmsExampleBagOfTasksApp(){
        super();
    }

    /**
     * @param broker the broker that the cloudlets will belong to
     * @return
     */
    @Override
    public List<NetworkCloudlet> createNetworkCloudlets(DatacenterBroker broker){
        final int NETCLOUDLETS_FOR_EACH_APP = 2;
        List<NetworkCloudlet> networkCloudletList = new ArrayList<>(NETCLOUDLETS_FOR_EACH_APP+1);
        //basically, each task runs the simulation and then data is consolidated in one task

        for(int i = 0; i < NETCLOUDLETS_FOR_EACH_APP; i++){
            UtilizationModel utilizationModel = new UtilizationModelFull();
            NetworkCloudlet cloudlet = new NetworkCloudlet(i, NETWORK_CLOUDLET_LENGTH, NETCLOUDLET_PES_NUMBER);
            cloudlet
                    .setMemory(CLOUDLET_TASK_MEMORY)
                    .setFileSize(NETCLOUDLET_FILE_SIZE)
                    .setOutputSize(NETCLOUDLET_OUTPUT_SIZE)
                    .setUtilizationModel(utilizationModel)
                    .setVm(getVmList().get(i));
            networkCloudletList.add(cloudlet);
        }

        createTasksForNetworkCloudlets(networkCloudletList);

        return networkCloudletList;
    }

    private void createTasksForNetworkCloudlets(final List<NetworkCloudlet> networkCloudletList) {
        int taskStageId=0;
        for (NetworkCloudlet cloudlet : networkCloudletList) {
            cloudlet.addTask(createExecutionTask(taskStageId++));

            //NetworkCloudlet 0 waits data from other Cloudlets
            if (cloudlet.getId()==0){
                /*
                If there are a total of N Cloudlets, since the first one receives packets
                from all the other ones, this for creates the tasks for the first Cloudlet
                to wait packets from N-1 other Cloudlets.
                 */
                for(int j=1; j < networkCloudletList.size(); j++) {
                    CloudletReceiveTask task = createReceiveTask(taskStageId++, networkCloudletList.get(j).getVm());
                    cloudlet.addTask(task);
                }
            }
            //The other NetworkCloudlets send data to the first one
            else {
                CloudletSendTask task = createSendTask(taskStageId++);
                cloudlet.addTask(task);
                task.addPacket(networkCloudletList.get(0), 1000);
                task.addPacket(networkCloudletList.get(0), 2000);
            }
        }
    }

    private CloudletSendTask createSendTask(final int taskId) {
        CloudletSendTask task = new CloudletSendTask(taskId);
        task.setMemory(CLOUDLET_TASK_MEMORY);
        return task;
    }

    /**
     * Creates a {@link CloudletReceiveTask} to be added for a {@link NetworkCloudlet}.
     * @param taskId the id of the task
     * @param sourceVm the VM where packets are expected to be received from
     * @return the created task
     */
    private CloudletReceiveTask createReceiveTask(final int taskId, final Vm sourceVm) {
        CloudletReceiveTask task = new CloudletReceiveTask(taskId, sourceVm);
        task.setMemory(CLOUDLET_TASK_MEMORY);
        return task;
    }

    private CloudletTask createExecutionTask(final int taskId) {
        final CloudletTask task = new CloudletExecutionTask(taskId, NETWORK_CLOUDLET_LENGTH);
        task.setMemory(CLOUDLET_TASK_MEMORY);
        return task;
    }

}
