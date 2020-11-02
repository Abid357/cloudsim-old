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
public class ConfigurationManager extends AddressableComponent implements Clockable{

    public static final int BUS_WIDTH_4_BIT = 4;
    public static final int BUS_WIDTH_8_BIT = 8;
    public static final int BUS_WIDTH_16_BIT = 16;
    public static final int BUS_WIDTH_32_BIT = 32;
    public static final int BUS_WIDTH_64_BIT = 64;
    public static final int BUS_WIDTH_128_BIT = 128;
    public static final int BUS_WIDTH_256_BIT = 256;
    public static final int BUS_WIDTH_512_BIT = 512;

    private Fpga fpga;
    private List<Region> regions;
    private List<Boolean> availabilityList;
    private Rectangle fabric;
    private PartitionPolicy policy;
    private long[][] map;
    private Queue<Bitstream> volatileMemory;
    private Bitstream nonVolatileMemory;
    private int busWidth;
    private long clock;

    public ConfigurationManager(Rectangle fabric) {
        this.fabric = fabric;
        regions = new ArrayList<>();
        volatileMemory = new LinkedList<>();
        availabilityList = new ArrayList<>();
    }

    public int getBusWidth() {
        return busWidth;
    }

    public void setBusWidth(int busWidth) {
        this.busWidth = busWidth;
    }

    public void setClock(long clock) {
        this.clock = clock;
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

        volatileMemory.add(bitstream);
        List<Region> configuredRegions = doPartialReconfiguration(bitstream, false, mappers);

        fpga.getClockManager().acquireClockFor(bitstream.getAccelerator());

        // calculate configuration time based on bitstream file size, configuration clock and bus width
        long bitstreamLength = bitstream.getFileSize() * 8 * 1000000; // megabytes to bits
        long clockInHertz = clock * 1000000;
        double configurationTime =
                (bitstreamLength / (double) busWidth) * (1.0 / clockInHertz);

        fpga.getVFpgaManager().createVFpga(configuredRegions, configurationTime, payload);
    }

    public void initialize() {
        if (nonVolatileMemory != null) {
            int staticRegionCount = nonVolatileMemory.getRequiredRegionCount();
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
        List<Region> configuredRegions = new ArrayList<>(bitstream.getRequiredRegionCount());
        int occupiedBlocks = (int) getDynamicRegions().stream().filter(region -> !region.isAvailable()).count();
        int availableBlocks = getDynamicRegions().size() - occupiedBlocks;
        if (availableBlocks < bitstream.getRequiredRegionCount() && !shouldReplace)
            return null;
        for (int i = 0; i < bitstream.getRequiredRegionCount(); i++) {
            int index = getNextAvailableRegionIndex(shouldReplace);
            if (index == -1)
                return null;
            else {
                Region region = regions.get(index);
                region.setAvailable(false);
                mappers.get(i).setBlockInFabric(index + 1);
                configuredRegions.add(region);

                int row = index / map[0].length;
                int col = index % map[0].length;
                map[row][col] = mappers.get(i).getvFpgaId();
            }
        }

        volatileMemory.add(bitstream);
        return configuredRegions;
    }

    public void deallocateRegionByIndex(int index) {
        regions.get(index).setAvailable(true);
        int row = index / map[0].length;
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
//                Accelerator vFpga = new Accelerator(Accelerator.CURRENT_VFPGA_ID++, bitstream.getAdapter(), regions);
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

    @Override
    public long getClockValue() {
        return clock;
    }

    @Override
    public String getComponentId() {
        return getClass().getSimpleName() + "-FPGA" + fpga.getId();
    }
}
