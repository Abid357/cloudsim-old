package org.cloudsimfe;

import org.apache.commons.lang3.StringUtils;
import org.cloudbus.cloudsim.core.CloudSimEntity;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.Simulation;
import org.cloudbus.cloudsim.core.events.SimEvent;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class VFpgaManager extends CloudSimEntity implements Addressable {
    private Fpga fpga;
    private List<VFpga> createdVFpgas;
    private List<VFpga> destroyedVFpgas;
    private long[][] scheduledTiles; // row count indicates number of tiles, column count indicates block number (b)
    // by scheduling policy (j), vFpga ID (j+1) and execution time unit (j+2)
    private List<Mapper> mappers;
    private List<Boolean> availableBlocks;
    private UnifiedManager manager;

    public VFpgaManager(Simulation simulation) {
        super(simulation);
        createdVFpgas = new ArrayList<>();
        destroyedVFpgas = new ArrayList<>();
        mappers = new ArrayList<>();
    }

    public void setUnifiedManager(UnifiedManager manager) {
        this.manager = manager;

        // update available block list
        for (int i = 0; i < getTotalRegionCount(); i++)
            availableBlocks.set(i, fpga.getConfigurationManager().getRegions().get(i).isAvailable());
    }

    public Fpga getFpga() {
        return fpga;
    }

    public void setFpga(Fpga fpga) {
        this.fpga = fpga;
        availableBlocks = new ArrayList<>();
        while (availableBlocks.size() < getTotalRegionCount()) {
            availableBlocks.add(true);
        }
    }

    public void notify(Payload payload) {
        VFpga vFpga = (VFpga) payload.getData().get(0);
        send(this, vFpga.getConfigurationTime(), CloudSimTags.VFPGA_RECONFIGURATION_FINISH, payload);
    }

    public void registerVFpga(VFpga vFpga, List<Mapper> vFpgaMappers) {
        for (Mapper mapper : vFpgaMappers)
            availableBlocks.set(mapper.getBlockInHypervisor(), false);
        mappers.addAll(vFpgaMappers);
        createdVFpgas.add(vFpga);
        vFpga.setCreatedAt(getSimulation().clock());
    }

    public List<Boolean> getAvailableBlockList() {
        return availableBlocks;
    }

    public List<VFpga> getAllVirtualFPGAs() {
        List<VFpga> combinedList = new ArrayList<>();
        combinedList.addAll(createdVFpgas);
        combinedList.addAll(destroyedVFpgas);
        return combinedList;
    }

    public long[][] getTiles() {
        return scheduledTiles;
    }

    public void createTiles() {
        scheduledTiles = new long[getReconfigurableRegionCount()][];
    }

    public int getTotalRegionCount() {
        return fpga.getConfigurationManager().getRegions().size();
    }

    public int getReconfigurableRegionCount() {
        return fpga.getConfigurationManager().getDynamicRegions().size();
    }

    public void printPerspective(PrintStream writer) {
        int COLUMN_WIDTH = 25;
        int COLUMN_HEIGHT = 5;
        int START_PRINT_IN_ROW = 2;

        long[][] map = fpga.getConfigurationManager().getMap();

        int rows = map.length;
        int cols = map[0].length;

        // top border of asterisks
        writer.print("*");
        for (int c = 0; c < cols; ++c) {
            for (int w = 0; w < COLUMN_WIDTH / 5; ++w)
                writer.print(StringUtils.center("*", COLUMN_WIDTH / 5));
            if (c < cols - 1)
                writer.print('|');
            else
                writer.println('*');
        }

        for (int r = 0; r < rows; ++r) {
            // determine number of rows or column height
            for (int h = 0; h < COLUMN_HEIGHT; ++h) {
                writer.print("*");

                // a single row inside matrix
                for (int c = 0; c < cols; ++c) {
                    if (h == START_PRINT_IN_ROW)
                        if (map[r][c] > 0) {
                            String ip = "";
                            for (VFpga vFpga : createdVFpgas)
                                if (vFpga.getId() == map[r][c])
                                    ip = vFpga.getIpAddress();
                            if (ip.isEmpty())
                                writer.print(StringUtils.center("[empty]", COLUMN_WIDTH));
                            else
                                writer.print(StringUtils.center("IP: " + ip, COLUMN_WIDTH));
                        } else if (map[r][c] == -1)
                            writer.print(StringUtils.center("Static", COLUMN_WIDTH));
                        else
                            writer.print(StringUtils.center("[empty]", COLUMN_WIDTH));
                    else if (h == START_PRINT_IN_ROW + 1) {
                        if (map[r][c] > 0) {
                            boolean vFpgaExists = false;
                            for (VFpga vFpga : createdVFpgas)
                                if (vFpga.getId() == map[r][c]) {
                                    writer.print(StringUtils.center("VFPGA ID: " + map[r][c], COLUMN_WIDTH));
                                    vFpgaExists = true;
                                    break;
                                }
                            if (!vFpgaExists)
                                writer.print(StringUtils.center("", COLUMN_WIDTH));
                        } else
                            writer.print(StringUtils.center("", COLUMN_WIDTH));

                    } else
                        writer.print(StringUtils.center("", COLUMN_WIDTH));
                    if (c < cols - 1)
                        writer.print('|');
                }
                writer.println("*");
            }

            // horizontal separator
            writer.print("*");
            for (int c = 0; c < cols; ++c) {
                writer.print(StringUtils.center("", COLUMN_WIDTH).replaceAll(" ", "_"));

                if (c < cols - 1)
                    writer.print("|");
            }

            // if last row then return cursor to replace underlines with asterisks
            if (r == rows - 1)
                writer.print('\r');
            else
                writer.println("*");
        }

        // bottom border of asterisks
        writer.print("*");
        for (int c = 0; c < cols; ++c) {
            for (int w = 0; w < COLUMN_WIDTH / 5; ++w)
                writer.print(StringUtils.center("*", COLUMN_WIDTH / 5));
            if (c < cols - 1)
                writer.print('|');
            else
                writer.println('*');
        }

    }

    private void deallocateBlocks(int id) {
        for (Mapper mapper : mappers) {
            if (mapper.getvFpgaId() == id) {
                int indexToDeallocateInFabric = mapper.getBlockInFabric() - 1;
                fpga.getConfigurationManager().deallocateRegionByIndex(indexToDeallocateInFabric);
                int indexToDeallocateInHypervisor = mapper.getBlockInHypervisor() - 1;
                availableBlocks.set(indexToDeallocateInHypervisor, true);
            }
        }
    }

    public void printScheduledTiles() {
        for (int i = 0; i < scheduledTiles.length; i++) {
            for (int j = 0; j < scheduledTiles[0].length; j++)
                System.out.print(scheduledTiles[i][j] + " ");
            System.out.println();
        }
    }

    public double updateProcessing(double currentTime) {
        double nextSimulationTime = Double.MAX_VALUE;
        List<VFpga> listToRemove = new ArrayList<>();
        for (Iterator<VFpga> iterator = createdVFpgas.iterator(); iterator.hasNext(); ) {
            VFpga vFpga = iterator.next();
            double time = vFpga.getAccelerator().updateProcessing(currentTime);

            nextSimulationTime = Math.min(time, nextSimulationTime);
            if (time == Double.MAX_VALUE && vFpga.getAccelerator().getSegmentExecution() != null) {
                vFpga.setDestroyedAt(currentTime);
                listToRemove.add(vFpga);
                destroyedVFpgas.add(vFpga);
                deallocateBlocks(vFpga.getId());
                fpga.getNetworkManager().unregisterRoute(vFpga.getIpAddress());

                sendNow(this, CloudSimTags.VFPGA_SEGMENT_PROCESSING_FINISH, vFpga);
            }
        }
        createdVFpgas.removeAll(listToRemove);

        List<VFpga> combinedList = new ArrayList<>();
        combinedList.addAll(createdVFpgas);
        combinedList.addAll(destroyedVFpgas);
        boolean configureVFpga = false;
        long nextVFpgaId = -1;
        int row = 0;
        int listOffset = 0;

        // offset is to ignore the blocks corresponding to static regions
        while (fpga.getConfigurationManager().getRegions().get(listOffset).isStatic())
            listOffset++;
        // search for vFPGA ID based on scheduled tiles values
        for (; row < scheduledTiles.length; row++) {
            if (availableBlocks.get(row + listOffset).booleanValue()) {
                for (int col = 1; col < scheduledTiles[row].length && !configureVFpga; col++) {
                    configureVFpga = true;
                    if (scheduledTiles[row][col] == 0) {
                        configureVFpga = false;
                        continue;
                    }
                    else if (scheduledTiles[row][col] != nextVFpgaId) {
                        nextVFpgaId = scheduledTiles[row][col];
                        for (VFpga vFpga : combinedList) {
                            if (vFpga.getId() == nextVFpgaId) {
                                configureVFpga = false;
                                break;
                            }
                        }
                    }
                    else
                        configureVFpga = false;
                }

                if (configureVFpga) {
                    List<Object> data = new ArrayList<>();
                    data.add((int) nextVFpgaId);
                    data.add(row);
                    data.add(this);

                    sendNow(manager, CloudSimTags.VFPGA_DYNAMIC_PARTIAL_RECONFIGURATION, new Payload(data));
                    configureVFpga = false;
                }
            }
        }

        return nextSimulationTime;
    }

    @Override
    protected void startEntity() {

    }

    public void processReconfigurationFinish(SimEvent evt){
        Payload payload = (Payload) evt.getData();
        VFpga vFpga = (VFpga) payload.getData().get(0);
        List<Mapper> vFpgaMappers = (List<Mapper>) payload.getData().get(1);
        registerVFpga(vFpga, vFpgaMappers);

        payload.removeData(1);
        fpga.getNetworkManager().sendDataToComponent(payload);
    }

    public void processSegmentFinish(SimEvent evt){
        VFpga vFpga = (VFpga) evt.getData();
        Payload payload = vFpga.getWrapper().readFromBuffer(Wrapper.WRITE_BUFFER);
        if (payload == null)
            payload = new Payload(new SegmentExecution(null));
        payload.addData(vFpga);
        payload.addData(vFpga.getIpAddress());

        fpga.getNetworkManager().sendDataToComponent(payload);
    }

    @Override
    public void processEvent(SimEvent evt) {
        if (evt.getTag() == CloudSimTags.VFPGA_RECONFIGURATION_FINISH) {
            processReconfigurationFinish(evt);
        } else if (evt.getTag() == CloudSimTags.VFPGA_SEGMENT_PROCESSING_FINISH){
            processSegmentFinish(evt);
        }
    }

    @Override
    public boolean schedule(int tag) {
        return false;
    }

    @Override
    public void assignIpAddress(String ipAddress) {

    }

    @Override
    public String withdrawIpAddress() {
        return null;
    }

    @Override
    public String getIpAddress() {
        return getClass().getSimpleName() + fpga.getId();
    }

    @Override
    public void sendDataToComponent(Payload payload) {
        String ipAddress = (String) payload.getData().get(1);
        payload.removeData(1);

        for (VFpga vFpga : createdVFpgas) {
            if (vFpga.getIpAddress().equals(ipAddress)) {
                vFpga.getWrapper().writeToBuffer(payload, Wrapper.READ_BUFFER);
                payload.addData(vFpga);
                sendNow(manager, CloudSimTags.VFPGA_SEGMENT_ACK, payload);
                break;
            }
        }
    }
}
