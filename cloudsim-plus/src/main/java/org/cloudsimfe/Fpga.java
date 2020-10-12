package org.cloudsimfe;

import org.cloudbus.cloudsim.core.Simulation;

import java.awt.*;
import java.io.PrintStream;
import java.util.List;

/**
 * The purpose of the class is to resemble and mimic a physical FPGA in terms of its characteristics. The class
 * characterizes an FPGA using the most essential and common attributes. Just like the way a cloud datacenter has
 * physical severs, it also has instances of FPGAs that are all connected to the main network for communication. Each
 * FPGA can receive and send data from and to the network.
 * <p>
 * There are a number of ways as to how FPGAs can be physically connected within the datacenter. One way is to have a
 * rack of FPGA chips interconnected through switches. Only few of the switches are connected to a router for
 * external communication, so that data can be exchanged between the datacenter's main network and this network of
 * FPGAs. To handle all communications, each FPGA requires a {@link org.cloudsimfe.NetworkManager}.
 * <p>
 * There are only two kinds of data that an FPGA receives. The first is called bitstreams. A bitstream or a bit file
 * contains essential information for the FPGA to configure one or more of its regions. With a bitstream, the
 * configuration manager can allocate appropriate type and number of resources from the FPGA fabric in
 * order to configure an {@link org.cloudsimfe.Accelerator}. This means each FPGA requires a
 * {@link org.cloudsimfe.ConfigurationManager}.
 * <p>
 * The second kind of data an FPGA receives is user data or application data. This data is fed into an existing
 * accelerator that is configured within the FPGA. The accelerator then processes the input data, generates output and
 * sends the output data back to the original sender.
 * <p>
 * For example, a user wishes to accelerate image processing in a cloud datacenter using FPGA. First, such an
 * accelerator has to be designed. The design may be done by the user, or a user may be given the option to select a
 * pre-defined image processing accelerator. Next, the design is converted into a bitstream and sent to the network
 * of the datacenter. This bitstream is transmitted to one of the available FPGAs in the rack. The configuration
 * manager of that FPGA reads the bitstream and, based on a policy, configures the image processing accelerator on
 * the fabric. Once the accelerator is ready, the user can send an image to it for processing. Note that both
 * bitstream data and image (application) data are received by the network manager of the FPGA. As it receives, it
 * checks the kind of received data. If the data is a bitstream, the network manager forwards it to the configuration
 * manager. If the data are application data, the network manager forwards to the intended accelerator.
 * <p>
 * This class uses the builder pattern. It allows users to set the class attributes optionally, so that in the case
 * they do not set, a default value will be used instead.
 *
 * @author Abid Farhan
 * @version CloudSim Plus 1.0
 * @see org.cloudsimfe.ConfigurationManager
 * @see org.cloudsimfe.NetworkManager
 * @see org.cloudsimfe.Accelerator
 * @see org.cloudsimfe.Packet
 */
public class Fpga {

    /**
     * A static counter to automate ID generation of the FPGAs
     */
    public static int CURRENT_FPGA_ID = 1;

    /**
     * A unique identification number for the FPGA instance.
     */
    private int id;

    /**
     * Brand of the FPGA. Typical examples include the two vendors Altera and Xilinx.
     */
    private String brand;

    /**
     * Family of the FPGA. Typical examples include Stratix V of Altera and Virtex-7 of Xilinx.
     */
    private String family;

    /**
     * Model of the FPGA. The model is an element of the FPGA family.
     */
    private String model;

    /**
     * Density of logic elements in the FPGA.
     */
    private long le;

    /**
     * Amount of memory elements in the FPGA.
     */
    private long memory;

    /**
     * Number of blocks of RAM in the FPGA.
     */
    private int bram;

    /**
     * Number of digital signal processing (DSP) slices in the FPGA.
     */
    private int dsp;

    /**
     * Number of input/out (I/O) pins in the FPGA.
     */
    private int io;

    /**
     * Number of transceivers in the FPGA.
     */
    private int transceiver;

    /**
     * Number of phased locked loop (PLL) in the FPGA.
     */
    private int pll;

    /**
     * Frequency of the main global clock in the FPGA.
     */
    private long clock;

    /**
     * Length of the FPGA chip. It is measured in millimeters (mm).
     */
    private float length;

    /**
     * Width of the FPGA chip. It is measured in millimeters (mm).
     */
    private float width;

    /**
     * Configuration manager of the FPGA.
     *
     * @see org.cloudsimfe.ConfigurationManager
     */
    private ConfigurationManager configurationManager;

    /**
     * Network manager of the FPGA.
     *
     * @see org.cloudsimfe.NetworkManager
     */
    private NetworkManager networkManager;

    /**
     * vFPGA manager of the FPGA.
     *
     * @see VFpgaManager
     */
    private VFpgaManager vFpgaManager;

    /**
     * Clock manager of the FPGA.
     *
     * @see ClockManager
     */
    private ClockManager clockManager;

    /**
     * The only constructor available. It is a private constructor because only the Builder inner class can call it.
     *
     * @param id                   {@link org.cloudsimfe.Fpga#id}
     * @param brand                {@link org.cloudsimfe.Fpga#brand}
     * @param family               {@link org.cloudsimfe.Fpga#family}
     * @param model                {@link org.cloudsimfe.Fpga#model}
     * @param le                   {@link org.cloudsimfe.Fpga#le}
     * @param memory               {@link org.cloudsimfe.Fpga#memory}
     * @param bram                 {@link org.cloudsimfe.Fpga#bram}
     * @param dsp                  {@link org.cloudsimfe.Fpga#dsp}
     * @param io                   {@link org.cloudsimfe.Fpga#io}
     * @param transceiver          {@link org.cloudsimfe.Fpga#transceiver}
     * @param clock                {@link org.cloudsimfe.Fpga#clock}
     * @param pll                  {@link org.cloudsimfe.Fpga#pll}
     * @param length               {@link org.cloudsimfe.Fpga#length}
     * @param width                {@link org.cloudsimfe.Fpga#width}
     * @param configurationManager {@link org.cloudsimfe.Fpga#configurationManager}
     * @param networkManager       {@link org.cloudsimfe.Fpga#networkManager}
     * @param vFpgaManager         {@link VFpgaManager}
     * @param clockManager         {@link ClockManager}
     */
    private Fpga(int id, String brand, String family, String model, long le, long memory, int bram, int dsp, int io,
                 int transceiver, int clock, int pll,
                 float length, float width, ConfigurationManager configurationManager, NetworkManager networkManager,
                 VFpgaManager vFpgaManager, ClockManager clockManager) {
        this.id = id;
        this.brand = brand;
        this.family = family;
        this.model = model;
        this.le = le;
        this.memory = memory;
        this.bram = bram;
        this.dsp = dsp;
        this.io = io;
        this.transceiver = transceiver;
        this.pll = pll;
        this.clock = clock;
        this.length = length;
        this.width = width;
        this.configurationManager = configurationManager;
        this.networkManager = networkManager;
        this.vFpgaManager = vFpgaManager;
        this.clockManager = clockManager;
    }

    /**
     * Gets the available amount of resources of a specified type. Available resources are resources that are not
     * allocated to any of the partitioned regions in the FPGA fabric.
     *
     * @param type the requested resource type
     * @return the amount of requested resource type that is available currently
     * @see org.cloudsimfe.Region
     * @see org.cloudsimfe.ConfigurationManager
     */
    public long getAvailableResource(Resources type) {
        List<Region> regions = configurationManager.getRegions();
        switch (type) {
            case LOGIC_ELEMENT:
                long totalLe = 0;
                for (Region region : regions)
                    totalLe += region.getUtilizedLogicElements();
                return le - totalLe;
            case MEMORY_REGISTER:
                long totalMemory = 0;
                for (Region region : regions)
                    totalMemory += region.getUtilizedMemoryRegisters();
                return memory - totalMemory;
            case BLOCKED_RAM:
                long totalBram = 0;
                for (Region region : regions)
                    totalBram += region.getUtilizedBlockedRams();
                return bram - totalBram;
            case DSP_SLICE:
                long totalDsp = 0;
                for (Region region : regions)
                    totalDsp += region.getUtilizedDspSlices();
                return dsp - totalDsp;
            case IO_PIN:
                long totalIo = 0;
                for (Region region : regions)
                    totalIo += region.getUtilizedIoPins();
                return io - totalIo;
            case TRANSCEIVER:
                long totalTransceiver = 0;
                for (Region region : regions)
                    totalTransceiver += region.getUtilizedTransceivers();
                return transceiver - totalTransceiver;
            case DEFAULT_CLOCK:
                return clock;
            case PHASE_LOCKED_LOOP:
                long max = 0;
                for (Region region : regions)
                    max = Math.max(max, region.getUtilizedPhaseLockedLoops());
                return pll - max;
            default:
                return -1;
        }
    }

    /**
     * Prints a high-level abstract view of resources in the FPGA fabric. Below is the list of symbols and what they
     * represent in the visual output of the method:
     * <p>
     * # - any digit
     * <p>
     * . (dot) - available resource
     * <p>
     * S# - resource belonging to a static region
     * <p>
     * D# - resource belonging to a dynamic region
     *
     * @param writer the print (output) stream
     * @see org.cloudsimfe.Region
     */
    public void printFabric(PrintStream writer) {
        int rows = configurationManager.getFabric().height;
        int cols = configurationManager.getFabric().width;
        List<Region> regions = configurationManager.getRegions();

        for (int c = 0; c <= cols; ++c)
            writer.print("*\t");
        writer.println("*");

        for (int r = 0; r < rows; ++r) {
            writer.print("*");
            for (int c = 0; c < cols; ++c) {
                Point point = new Point(c, r);
                writer.print("\t");
                String symbol = ".";
                for (int x = 0; x < regions.size(); ++x)
                    if (((RectangleRegion) regions.get(x)).contains(point)) {
                        if (x == 0 && c < 11)
                            symbol = "S" + 1;
                        else if (x == 0)
                            symbol = "S" + 2;
                        else
                            symbol = "D" + x;
                        break;
                    }
                writer.print(symbol);
            }
            writer.print("\t");
            writer.println("*");
        }

        for (int c = 0; c <= cols; ++c)
            writer.print("*\t");
        writer.println("*");

    }

    @Override
    public String toString() {
        return "Fpga{" +
                "id=" + id +
                ", brand='" + brand + '\'' +
                ", model='" + model + '\'' +
                ", le=" + le +
                ", memory=" + memory +
                ", bram=" + bram +
                ", dsp=" + dsp +
                ", clock=" + clock +
                ", pll=" + pll +
                ", length=" + length +
                ", width=" + width +
                '}';
    }

    public int getId() {
        return id;
    }

    public String getBrand() {
        return brand;
    }

    public String getModel() {
        return model;
    }

    public long getLogicElements() {
        return le;
    }

    public long getMemory() {
        return memory;
    }

    public int getBlockedRams() {
        return bram;
    }

    public int getDspSlices() {
        return dsp;
    }

    public long getClock() {
        return clock;
    }

    public int getPhaseLockedLoops() {
        return pll;
    }

    public float getLength() {
        return length;
    }

    public float getWidth() {
        return width;
    }

    public int getIo() {
        return io;
    }

    public int getTransceivers() {
        return transceiver;
    }

    public ConfigurationManager getConfigurationManager() {
        return configurationManager;
    }

    public NetworkManager getNetworkManager() {
        return networkManager;
    }

    public ClockManager getClockManager() {
        return clockManager;
    }

    public VFpgaManager getVFpgaManager() {
        return vFpgaManager;
    }

    /**
     * List of FPGA resource types.
     */
    public enum Resources {
        LOGIC_ELEMENT, MEMORY_REGISTER, BLOCKED_RAM, DSP_SLICE, IO_PIN, TRANSCEIVER,
        PHASE_LOCKED_LOOP, DEFAULT_CLOCK
    }

    /**
     * Builder design pattern.
     */
    public static class Builder {
        private int id;
        private String brand;
        private String family;
        private String model;
        private long le;
        private long memory;
        private int bram;
        private int dsp;
        private int io;
        private int transceiver;
        private int clock;
        private int pll;
        private float length;
        private float width;
        private ConfigurationManager configurationManager;
        private NetworkManager networkManager;
        private VFpgaManager vFpgaManager;
        private ClockManager clockManager;
        private PartitionPolicy partitionPolicy;
        private int staticRegionCount;

        /**
         * Constructor of the Builder class.
         *
         * @param id     the FPGA ID number
         * @param server the DHCP server which will communicate with the FPGA's network manager
         */
        public Builder(Simulation simulation, int id, DhcpServer server) {
            this.id = id;
            brand = "";
            family = "";
            model = "";
            le = 1000;
            memory = 1000;
            bram = 10;
            dsp = 10;
            io = 500;
            transceiver = 10;
            pll = 1;
            clock = 10;
            length = 10;
            width = 12;
            configurationManager = new ConfigurationManager(new Rectangle(0, 0, 33, 33));
            networkManager = new NetworkManager(server);
            vFpgaManager = new VFpgaManager(simulation);
            clockManager = new ClockManager(clock, pll);
            partitionPolicy = new PartitionPolicyGrid();
            staticRegionCount = 1;
        }

        /**
         * Builds a {@link org.cloudsimfe.Fpga} object.
         */
        public Fpga build() {
            Fpga fpga = new Fpga(id, brand, family, model, le, memory, bram, dsp, io, transceiver, clock, pll, length
                    , width, configurationManager, networkManager, vFpgaManager, clockManager);
            configurationManager.setFpga(fpga);
            configurationManager.setPartitionPolicy(partitionPolicy);
            configurationManager.doPartition();
            configurationManager.setNonVolatileMemory(new Bitstream(null, null, staticRegionCount));
            networkManager.setFpga(fpga);
            networkManager.sendDataToComponent(new Payload(configurationManager));
            vFpgaManager.setFpga(fpga);

            clockManager.setFpga(fpga);
            clockManager.setClock(clock);
            clockManager.setMaxCount(pll);
            clockManager.acquireClockFor(networkManager);
            clockManager.acquireClockFor(configurationManager);
            clockManager.acquireClockFor(vFpgaManager);
            return fpga;
        }

        /**
         * Sets the ID number of this FPGA.
         *
         * @param id {@link org.cloudsimfe.Fpga#id}
         * @return this {@link org.cloudsimfe.Fpga.Builder} object
         */
        public Builder setId(int id) {
            this.id = id;
            return this;
        }

        /**
         * Sets the brand of this FPGA.
         *
         * @param brand {@link org.cloudsimfe.Fpga#brand}
         * @return this {@link org.cloudsimfe.Fpga.Builder} object
         */
        public Builder setBrand(String brand) {
            this.brand = brand;
            return this;
        }

        /**
         * Sets the model of this FPGA.
         *
         * @param model {@link org.cloudsimfe.Fpga#model}
         * @return this {@link org.cloudsimfe.Fpga.Builder} object
         */
        public Builder setModel(String model) {
            this.model = model;
            return this;
        }

        /**
         * Sets density of logic elements of this FPGA.
         *
         * @param le {@link org.cloudsimfe.Fpga#le}
         * @return this {@link org.cloudsimfe.Fpga.Builder} object
         */
        public Builder setLogicElements(long le) {
            this.le = le;
            return this;
        }

        /**
         * Sets amount of memory registers of this FPGA.
         *
         * @param memory {@link org.cloudsimfe.Fpga#memory}
         * @return this {@link org.cloudsimfe.Fpga.Builder} object
         */
        public Builder setMemoryRegisters(long memory) {
            this.memory = memory;
            return this;
        }


        /**
         * Sets number of blocks of RAM of this FPGA.
         *
         * @param bram {@link org.cloudsimfe.Fpga#bram}
         * @return this {@link org.cloudsimfe.Fpga.Builder} object
         */
        public Builder setBlockedRams(int bram) {
            this.bram = bram;
            return this;
        }

        /**
         * Sets number of DSP slices of this FPGA.
         *
         * @param dsp {@link org.cloudsimfe.Fpga#dsp}
         * @return this {@link org.cloudsimfe.Fpga.Builder} object
         */
        public Builder setDspSlices(int dsp) {
            this.dsp = dsp;
            return this;
        }

        /**
         * Sets the clock frequency of this FPGA.
         *
         * @param clock {@link org.cloudsimfe.Fpga#clock}
         * @return this {@link org.cloudsimfe.Fpga.Builder} object
         */
        public Builder setClock(int clock) {
            this.clock = clock;
            return this;
        }

        /**
         * Sets the number of PLLs of this FPGA.
         *
         * @param pll {@link org.cloudsimfe.Fpga#pll}
         * @return this {@link org.cloudsimfe.Fpga.Builder} object
         */
        public Builder setPhaseLockedLoops(int pll) {
            this.pll = pll;
            return this;
        }

        /**
         * Sets the family of this FPGA.
         *
         * @param family {@link org.cloudsimfe.Fpga#family}
         * @return this {@link org.cloudsimfe.Fpga.Builder} object
         */
        public Builder setFamily(String family) {
            this.family = family;
            return this;
        }

        /**
         * Sets the length of this FPGA chip.
         *
         * @param length {@link org.cloudsimfe.Fpga#length}
         * @return this {@link org.cloudsimfe.Fpga.Builder} object
         */
        public Builder setLength(float length) {
            this.length = length;
            return this;
        }

        /**
         * Sets the width of this FPGA chip.
         *
         * @param width {@link org.cloudsimfe.Fpga#width}
         * @return this {@link org.cloudsimfe.Fpga.Builder} object
         */
        public Builder setWidth(float width) {
            this.width = width;
            return this;
        }

        /**
         * Sets number of I/O pins of this FPGA.
         *
         * @param io {@link org.cloudsimfe.Fpga#io}
         * @return this {@link org.cloudsimfe.Fpga.Builder} object
         */
        public Builder setIoPins(int io) {
            this.io = io;
            return this;
        }

        /**
         * Sets anumber of transceivers of this FPGA.
         *
         * @param transceiver {@link org.cloudsimfe.Fpga#transceiver}
         * @return this {@link org.cloudsimfe.Fpga.Builder} object
         */
        public Builder setTransceivers(int transceiver) {
            this.transceiver = transceiver;
            return this;
        }

        public Builder setPartitionPolicy(PartitionPolicy partitionPolicy) {
            this.partitionPolicy = partitionPolicy;
            return this;
        }

        public Builder setStaticRegionCount(int count) {
            this.staticRegionCount = count;
            return this;
        }

        /**
         * Adds a region or a partition within the FPGA fabric.
         *
         * @return this {@link org.cloudsimfe.Fpga.Builder} object
         */
        public Builder addRegion(Region region) {
            configurationManager.getRegions().add(region);
            return this;
        }
    }
}
