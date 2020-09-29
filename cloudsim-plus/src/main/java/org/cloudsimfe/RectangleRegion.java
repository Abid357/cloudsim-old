package org.cloudsimfe;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * An FPGA has a variety of resources which, placed and packed together, form the fabric. The fabric can be further
 * divided into separate regions, where each region can be used for various purposes. Dividing the fabric into
 * multiple regions is known as partitioning. As a result, each region has a fixed area that it occupies within the
 * fabric. This also means the region has a fixed portion of the various resources of the FPGA and these resources
 * cannot be shared with another region (i.e. no overlapping of regions).
 * <p>
 * A single region may be made of any polygon. A lot of research works follow a rectangular shape to identify a
 * region. For simplicity and scope of this simulator, the default shape of all FPGA regions is the rectangle. Hence,
 * this class extends from the Rectangle class and implements the Region interface in order to provide useful methods.
 * One may, however, use any other polygon class and would only have to implement the Region interface to be
 * able to place a region in the FPGA fabric.
 * <p>
 * Since a region now is a subclass of java.awt.Rectangle, it will always have four points just like the rectangle
 * shape. The attributes that help to specify these points are {@link java.awt.Rectangle#x},
 * {@link java.awt.Rectangle#y}, {@link java.awt.Rectangle#width} and {@link java.awt.Rectangle#height}.
 * Using these attributes, all four points of the rectangular region can be calculated. Additionally, these points
 * are specified with respect to the FPGA fabric. For example, if a region has the following attribute values: x = 0,
 * y = 0, width = 5, height = 5; it would indicate that the region has equal sides (i.e. square) and is placed at the
 * top-left corner of the fabric after partitioning.
 * <p>
 * This class uses the builder pattern. It allows users to set the class attributes optionally, so that in the case
 * they do not set, a default value will be used instead.
 *
 * @author Abid Farhan
 * @version CloudSim Plus 1.0
 * @see java.awt.Rectangle
 * @see org.cloudsimfe.Region
 * @see org.cloudsimfe.Fpga
 */
public class RectangleRegion extends Rectangle implements Region {

    /**
     * Instance of an FPGA which holds this region within its fabric.
     *
     * @see org.cloudsimfe.Fpga
     */
    private Fpga fpga;

    /**
     * Density of logic elements occupied by this region.
     */
    private long le;

    /**
     * Amount of memory registers occupied by this region.
     */
    private long memory;

    /**
     * Number of RAM blocks occupied by this region.
     */
    private int bram;

    /**
     * Number of digital processing signal (DSP) slices occupied by this region.
     */
    private int dsp;

    /**
     * Number of input/out (I/O) pins occupied by this region.
     */
    private int io;

    /**
     * Number of transceivers occupied by this region.
     */
    private int transceiver;

    /**
     * Default clock frequency of this region.
     */
    private long clock;

    /**
     * Number of phased locked loops (PLL) occupied by this region.
     */
    private int pll;

    /**
     * Number of clock domains present in this region.
     */
    private List<Long> clockDomains;

    /**
     * Indicates whether the region is static (true) or dynamic (false)
     */
    private boolean isStatic;

    /**
     * Indicates whether the region is available (true) or occupied (false)
     */
    private boolean isAvailable;

    /**
     * Copy constructor.
     *
     * @param region the region to make copy of
     */
    public RectangleRegion(RectangleRegion region) {
        this.fpga = region.fpga;
        this.le = region.le;
        this.memory = region.memory;
        this.bram = region.bram;
        this.dsp = region.dsp;
        this.io = region.io;
        this.transceiver = region.transceiver;
        this.clock = region.clock;
        this.pll = region.pll;
        this.clockDomains = region.clockDomains;
        this.isStatic = region.isStatic;
        this.isAvailable = region.isAvailable;
    }

    /**
     * The only constructor available. It is a private constructor because only the Builder inner class can call it.
     *
     * @param x            the x-coordinate of the top-left corner point of this region
     * @param y            the y-coordinate of the top-left corner point of this region
     * @param width        the length of the horizontal sides of the region
     * @param height       the length of vertical sides of the region
     * @param fpga         {@link org.cloudsimfe.RectangleRegion#fpga}
     * @param le           {@link org.cloudsimfe.RectangleRegion#le}
     * @param memory       {@link org.cloudsimfe.RectangleRegion#memory}
     * @param bram         {@link org.cloudsimfe.RectangleRegion#bram}
     * @param dsp          {@link org.cloudsimfe.RectangleRegion#dsp}
     * @param io           {@link org.cloudsimfe.RectangleRegion#io}
     * @param transceiver  {@link org.cloudsimfe.RectangleRegion#transceiver}
     * @param clock        {@link org.cloudsimfe.RectangleRegion#clock}
     * @param pll          {@link org.cloudsimfe.RectangleRegion#pll}
     * @param clockDomains {@link org.cloudsimfe.RectangleRegion#clockDomains}
     * @param isStatic     {@link org.cloudsimfe.RectangleRegion#isStatic}
     * @param isAvailable  {@link org.cloudsimfe.RectangleRegion#isAvailable}
     */
    private RectangleRegion(int x, int y, int width, int height, Fpga fpga, long le, long memory, int bram, int dsp,
                            int io, int transceiver, long clock, int pll, List<Long> clockDomains, boolean isStatic
            , boolean isAvailable) {
        super(x, y, width, height);
        this.fpga = fpga;
        this.le = le;
        this.memory = memory;
        this.bram = bram;
        this.dsp = dsp;
        this.io = io;
        this.transceiver = transceiver;
        this.clock = clock;
        this.pll = pll;
        this.clockDomains = clockDomains;
        this.isStatic = isStatic;
        this.isAvailable = isAvailable;
    }

    @Override
    public Fpga getFpga() {
        return fpga;
    }

    @Override
    public List<Point> getPoints() {
        List<Point> points = new ArrayList<>();
        points.add(new Point(x, y));
        points.add(new Point(x + width, y));
        points.add(new Point(x, y + height));
        points.add(new Point(x + width, y + height));
        return points;
    }

    @Override
    public long getUtilizedLogicElements() {
        return le;
    }

    @Override
    public int getUtilizedDspSlices() {
        return dsp;
    }

    @Override
    public int getUtilizedBlockedRams() {
        return bram;
    }

    @Override
    public long getUtilizedMemoryRegisters() {
        return memory;
    }

    @Override
    public int getUtilizedPhaseLockedLoops() {
        return pll;
    }

    @Override
    public int getUtilizedIoPins() {
        return io;
    }

    @Override
    public int getUtilizedTransceivers() {
        return transceiver;
    }

    @Override
    public boolean isStatic() {
        return isStatic;
    }

    @Override
    public void setStatic(boolean isStatic) {
        this.isStatic = isStatic;
    }

    @Override
    public boolean isAvailable() {
        return isAvailable;
    }

    @Override
    public void setAvailable(boolean isAvailable) {
        this.isAvailable = isAvailable;
    }

    @Override
    public List<Long> getClockDomains() {
        return clockDomains;
    }

    @Override
    public String toString() {
        return "RectangleRegion{" +
                "fpga=" + fpga +
                ", le=" + le +
                ", memory=" + memory +
                ", bram=" + bram +
                ", dsp=" + dsp +
                ", io=" + io +
                ", transceiver=" + transceiver +
                ", clock=" + clock +
                ", pll=" + pll +
                ", clockDomains=" + clockDomains +
                ", static=" + isStatic +
                ", available=" + isAvailable +
                '}';
    }

    /**
     * Builder design pattern.
     */
    public static class Builder {
        private int x;
        private int y;
        private int width;
        private int height;
        private Fpga fpga;
        private long le;
        private long memory;
        private int bram;
        private int dsp;
        private int io;
        private int transceiver;
        private long clock;
        private int pll;
        private boolean isStatic;
        private boolean isAvailable;
        private List<Long> clockDomains;

        /**
         * Constructor of the Builder class.
         *
         * @param x      {@link org.cloudsimfe.RectangleRegion#x}
         * @param y      {@link org.cloudsimfe.RectangleRegion#y}
         * @param width  {@link org.cloudsimfe.RectangleRegion#width}
         * @param height {@link org.cloudsimfe.RectangleRegion#height}
         * @param fpga   {@link org.cloudsimfe.RectangleRegion#fpga}
         */
        public Builder(int x, int y, int width, int height, Fpga fpga) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.fpga = fpga;
            le = 200;
            memory = 80;
            bram = 1;
            dsp = 1;
            io = 20;
            transceiver = 0;
            pll = 0;
            clock = fpga.getClock();
            clockDomains = new ArrayList<>();
            isStatic = false;
            isAvailable = true;
        }

        /**
         * Builds a {@link org.cloudsimfe.RectangleRegion} object.
         */
        public RectangleRegion build() {
            return new RectangleRegion(x, y, width, height, fpga, le, memory, bram, dsp, io, transceiver, clock, pll,
                    clockDomains, isStatic, isAvailable);
        }

        /**
         * Sets density of logic elements for this region.
         *
         * @param le {@link org.cloudsimfe.RectangleRegion#le}
         * @return this {@link org.cloudsimfe.RectangleRegion.Builder} object
         */
        public Builder setLogicElements(long le) {
            this.le = Math.min(le, fpga.getAvailableResource(Fpga.Resources.LOGIC_ELEMENT));
            return this;
        }

        /**
         * Sets amount of memory registers for this region.
         *
         * @param memory {@link org.cloudsimfe.RectangleRegion#memory}
         * @return this {@link org.cloudsimfe.RectangleRegion.Builder} object
         */
        public Builder setMemoryRegisters(long memory) {
            this.memory = Math.min(memory, fpga.getAvailableResource(Fpga.Resources.MEMORY_REGISTER));
            return this;
        }

        /**
         * Sets number of blocks of RAM for this region.
         *
         * @param bram {@link org.cloudsimfe.RectangleRegion#bram}
         * @return this {@link org.cloudsimfe.RectangleRegion.Builder} object
         */
        public Builder setBlockedRams(int bram) {
            this.bram = Math.min(bram, (int) fpga.getAvailableResource(Fpga.Resources.BLOCKED_RAM));
            return this;

        }

        /**
         * Sets number of DSP slices for this region.
         *
         * @param dsp {@link org.cloudsimfe.RectangleRegion#dsp}
         * @return this {@link org.cloudsimfe.RectangleRegion.Builder} object
         */
        public Builder setDspSlices(int dsp) {
            this.dsp = Math.min(dsp, (int) fpga.getAvailableResource(Fpga.Resources.DSP_SLICE));
            return this;
        }

        /**
         * Sets number of I/O pins for this region.
         *
         * @param io {@link org.cloudsimfe.RectangleRegion#io}
         * @return this {@link org.cloudsimfe.RectangleRegion.Builder} object
         */
        public Builder setIoPins(int io) {
            this.io = Math.min(io, (int) fpga.getAvailableResource(Fpga.Resources.IO_PIN));
            return this;
        }

        /**
         * Sets a number of transceivers for this region.
         *
         * @param transceiver {@link org.cloudsimfe.RectangleRegion#transceiver}
         * @return this {@link org.cloudsimfe.RectangleRegion.Builder} object
         */
        public Builder setTransceivers(int transceiver) {
            this.transceiver = Math.min(transceiver, (int) fpga.getAvailableResource(Fpga.Resources.TRANSCEIVER));
            return this;
        }

        /**
         * Sets whether this region is static or dynamic.
         *
         * @param isStatic {@link org.cloudsimfe.RectangleRegion#isStatic}
         * @return this {@link org.cloudsimfe.RectangleRegion.Builder} object
         */
        public Builder setStatic(boolean isStatic){
            this.isStatic = isStatic;
            return this;
        }

        /**
         * Adds a clock domain into this region. If the frequency of the domain appears to be same as the default
         * frequency of the region or if all PLLs of the FPGA have been already used, the new clock domain is not added
         * to the list. However, if it is a different clock frequency, the domain is added to the list of clock
         * domains within the region, and number of used {@link org.cloudsimfe.RectangleRegion#pll} is incremented.
         *
         * @param clockDomain the new clock domain to be added to {@link org.cloudsimfe.RectangleRegion#clockDomains}
         * @return this {@link org.cloudsimfe.RectangleRegion.Builder} object
         */
        public Builder addClockDomain(long clockDomain) {
            if (clockDomain == clock || clockDomains.size() == fpga.getPhaseLockedLoops())
                return this;

            boolean clockExists = false;
            for (Long clock : clockDomains)
                if (clock.longValue() == clockDomain) {
                    clockExists = true;
                    break;
                }

            if (!clockExists) {
                clockDomains.add(clockDomain);
                pll++;
            }
            return this;
        }
    }
}
