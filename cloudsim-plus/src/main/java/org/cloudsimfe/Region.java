package org.cloudsimfe;

import java.awt.*;
import java.util.List;

/**
 * An interface that allows a region within an FPGA's physical fabric to give valuable information about that region.
 * Irrespective of what shape the region takes up and which FPGA fabric it belongs to, it will always occupy a fixed
 * area of the fabric. Also, this means it will contain a fixed portion of the FPGA fabric resources. An FPGA
 * instance can have one or more regions. At any given point in time, users can inquire how much resources of each
 * type are being utilized or occupied by this region.
 * <p>
 * For implementation details, see {@link org.cloudsimfe.RectangleRegion}.
 *
 * @author Abid Farhan
 * @version CloudSim Plus 1.0
 */
public interface Region {
    /**
     * Gets the FPGA to which this region belongs. Usually, an FPGA instance will hold a list of regions.
     *
     * @return the FPGA
     */
    Fpga getFpga();

    /**
     * A region can take up any shape on the fabric - square, rectangle or any other polygon. This returns all the
     * points of the shape of this region.
     *
     * @return a list of all points of the region
     */
    List<Point> getPoints();

    /**
     * @return the total amount of logic elements utilized
     */
    long getUtilizedLogicElements();

    /**
     * @return the total amount of digital signal processing (DSP) slices utilized
     */
    int getUtilizedDspSlices();

    /**
     * @return the total amount of blocks of RAM utilized
     */
    int getUtilizedBlockedRams();

    /**
     * @return the total amount of embedded memory registers utilized
     */
    long getUtilizedMemoryRegisters();

    /**
     * @return the total amount of phased locked loops (PLL) utilized
     */
    int getUtilizedPhaseLockedLoops();

    /**
     * @return the total amount of input/output (I/O) pins utilized
     */
    int getUtilizedIoPins();

    /**
     * @return the total amount of transceivers utilized
     */
    int getUtilizedTransceivers();

    /**
     * @return whether the region is static or non-static (dynamic)
     */
    boolean isStatic();

    /**
     * set the region as static or dynamic
     */
    void setStatic(boolean isStatic);

    /**
     * @return whether the region is available or occupied
     */
    boolean isAvailable();

    /**
     * set the region as available or occupied
     */
    void setAvailable(boolean isAvailable);

    /**
     * A region by default contains the default clock domain inherited from the FPGA it belongs to. Due to having
     * PLLs, which allow generating different clock frequencies, a region can then have multiple clock domains where
     * each domain has different clock frequency.
     *
     * @return the total amount of clock domains present
     */
    List<Long> getClockDomains();
}
