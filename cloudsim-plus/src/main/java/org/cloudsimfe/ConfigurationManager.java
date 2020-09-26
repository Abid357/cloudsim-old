package org.cloudsimfe;

import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * @author Abid Farhan
 * @version CloudSim Plus 1.0
 */
public class ConfigurationManager extends AddressableComponent {

    private Fpga fpga;
    private List<Region> regions;
    private List<Boolean> availabilityList;
    private Rectangle fabric;
    private PartitionPolicy policy;
    private long[][] map;
    private Queue<Bitstream> volatileMemory;
    private Bitstream nonVolatileMemory;

    public ConfigurationManager(Rectangle fabric) {
        this.fabric = fabric;
        regions = new ArrayList<>();
        volatileMemory = new LinkedList<>();
        availabilityList = new ArrayList<>();

//        int height = fabric.height;
//        int width = fabric.width;
//
//        List<Integer> divisorsForRow = new ArrayList<>();
//        for (int i = 1; i <= height / 2; i++)
//            if (height % i == 0)
//                divisorsForRow.add(i);
//        int rows = divisorsForRow.get(divisorsForRow.size() / 2); // get the middle divisor
//
//        List<Integer> divisorsForCol = new ArrayList<>();
//        for (int i = 1; i <= width / 2; i++)
//            if (width % i == 0)
//                divisorsForCol.add(i);
//        int cols = divisorsForCol.get(divisorsForCol.size() / 2); // get the middle divisor
//
//        map = new long[rows][cols];
    }

    public void setMap(long[][] map) {
        this.map = map;
    }

    public void setPartitionOptions(Object... options){
        policy.setOption(options);
    }

    public void doPartition() {
        policy.partition();
    }

    @Override
    public void sendDataToComponent(Payload payload) {
        Bitstream bitstream = (Bitstream) payload.getData().get(0);
        List<Mapper> mappers = (List<Mapper>) payload.getData().get(1);
        int vFpgaId = ((Integer) payload.getData().get(2)).intValue();
        String srcAddress = (String) payload.getData().get(3);

        volatileMemory.add(bitstream);
        List<Region> configuredRegions = doPartialReconfiguration(bitstream, false, mappers);
        VFpga vFpga = new VFpga(vFpgaId, bitstream.getWrapper(), configuredRegions);
        vFpga.setManager(fpga.getVFpgaManager());
        vFpga.setAccelerator(bitstream.getAccelerator());
        fpga.getNetworkManager().sendDataToComponent(new Payload(vFpga));

        //TODO: find a more stable formula for calculating configuration time from literature
        double configurationTime = configuredRegions.size() * 0.5;
        vFpga.setConfigurationTime(configurationTime);

        Payload internalPayload = new Payload();
        internalPayload.addData(vFpga);
        internalPayload.addData(mappers);
        internalPayload.addData(srcAddress);

        fpga.getVFpgaManager().notify(internalPayload);
    }

    public void initialize() {
        if (nonVolatileMemory != null) {
            int staticRegionCount = nonVolatileMemory.getRequiredBlockCount();
            for (int i = 0; i < staticRegionCount; i++) {
                int index = getNextAvailableRegionIndex(false);
                if (index != -1) {
                    regions.get(index).setStatic(true);
                    regions.get(index).setAvailable(false);
                    int row = index / map.length;
                    int col = index % map[0].length;
                    map[row][col] = -1;
                }
            }
        }
    }

    public void printMap() {
        for (int i = 0; i < map.length; i++) {
            for (int j = 0; j < map[0].length; j++)
                System.out.print(map[i][j] + " ");
            System.out.println();
        }
    }

    // allocation of region in the physical layer, this is decoupled from hypervisor and the virtual layer via the
    // use of the Mapper class
    public List<Region> doPartialReconfiguration(Bitstream bitstream, boolean shouldReplace, List<Mapper> mappers) {
        List<Region> configuredRegions = new ArrayList<>(bitstream.getRequiredBlockCount());
        int occupiedBlocks = (int) getDynamicRegions().stream().filter(region -> !region.isAvailable()).count();
        int availableBlocks = getDynamicRegions().size() - occupiedBlocks;
        if (availableBlocks < bitstream.getRequiredBlockCount() && !shouldReplace)
            return null;

        for (int i = 0; i < bitstream.getRequiredBlockCount(); i++) {
            int index = getNextAvailableRegionIndex(shouldReplace);
            if (index == -1)
                return null;
            else {
                Region region = regions.get(index);
                region.setAvailable(false);
                mappers.get(i).setBlockInFabric(index + 1);
                configuredRegions.add(region);

                int row = index / map.length;
                int col = index % map[0].length;
                map[row][col] = mappers.get(i).getvFpgaId();
            }
        }

        volatileMemory.add(bitstream);
        return configuredRegions;
    }

    public void deallocateRegionByIndex(int index) {
        getDynamicRegions().get(index).setAvailable(true);
        int row = index / map.length;
        int col = index % map[0].length;
        map[row][col] = 0;
    }

    private int getNextAvailableRegionIndex(boolean shouldReplace) {
        for (int i = 0; i < regions.size(); i++) {
            Region region = regions.get(i);
            // region NOT static AND (available OR (NOT available AND replaceable))
            if (!region.isStatic() && (region.isAvailable() || (!region.isAvailable() && shouldReplace)))
                return i;
        }
        return -1;
    }

//    public boolean configure() {
//        int occupiedRegionCount = (int) availabilityList.stream().filter(region -> !region.booleanValue()).count();
//        if (volatileMemory.isEmpty() || occupiedRegionCount == regions.size())
//            return false;
//
//        Iterator<Bitstream> iterator = volatileMemory.iterator();
//        while (iterator.hasNext()) {
//            Bitstream bitstream = iterator.next();
//            List<Region> regions = getRegionsForVFpga(bitstream.getRequiredBlockCount(),
//                    bitstream.getAccelerator().getId());
//            if (regions != null) {
//                Accelerator vFpga = new Accelerator(Accelerator.CURRENT_VFPGA_ID++, bitstream.getWrapper(), regions);
//                vFpga.setAccelerator(bitstream.getAccelerator());
//                fpga.getNetworkManager().sendDataToComponent(vFpga);
//            } else
//                return false;
//        }
//        return true;
//    }

//    private List<Region> getRegionsForVFpga(int requiredRegionCount, long acceleratorId) {
//        List<Region> dynamicRegions = getDynamicRegions();
//        List<Region> regionsForVFpga = new ArrayList<>();
//        for (int i = 0; i < dynamicRegions.size() && requiredRegionCount != 0; ++i) {
//            if (availabilityList.get(i).booleanValue() == true) {
//                regionsForVFpga.add(dynamicRegions.get(i));
//                availabilityList.set(i, false);
//                int row = i / map.length;
//                int col = i % map[0].length;
//                map[row][col] = acceleratorId;
//                --requiredRegionCount;
//            }
//        }
//        if (requiredRegionCount != 0)
//            return null;
//        return regionsForVFpga;
//    }

    public List<Region> getDynamicRegions() {
        List<Region> dynamicRegions = new ArrayList<>();
        if (!regions.isEmpty())
            for (Region region : regions)
                if (!region.isStatic())
                    dynamicRegions.add(region);
        return dynamicRegions;
    }

    public List<Region> getStaticRegions() {
        List<Region> staticRegions = new ArrayList<>();
        for (Region region : regions)
            if (region.isStatic())
                staticRegions.add(region);
        return staticRegions;
    }

    public List<Region> getRegions() {
        return regions;
    }

    public Rectangle getFabric() {
        return fabric;
    }

    public void setPartitionPolicy(PartitionPolicy policy) {
        this.policy = policy;
        policy.setFpga(fpga);
    }

    public Fpga getFpga() {
        return fpga;
    }

    public void setFpga(Fpga fpga) {
        this.fpga = fpga;
    }

    public long[][] getMap() {
        return map;
    }

    public Bitstream getNonVolatileMemory() {
        return nonVolatileMemory;
    }

    public void setNonVolatileMemory(Bitstream nonVolatileMemory) {
        this.nonVolatileMemory = nonVolatileMemory;
    }
}
