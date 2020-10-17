package org.cloudsimfe;

import org.apache.commons.lang3.StringUtils;
import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.cloudlets.CloudletSimple;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModel;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public class AccelerableCloudlet extends CloudletSimple {

    public static int CURRENT_ACCELERABLE_CLOUDLET_ID = 1;

    private long length;
    private List<AccelerableSegment> segments;
    private Iterator<AccelerableSegment> iterator;
    private CloudletSimple nonAccelerableSegments;
    private double finishTime;

    public AccelerableCloudlet(long length, int pesNumber, UtilizationModel utilizationModel) {
        super(length, pesNumber, utilizationModel);
        segments = new ArrayList<>();
        this.length = length;
        setId(CURRENT_ACCELERABLE_CLOUDLET_ID++);
    }

    public AccelerableCloudlet(long length, int pesNumber) {
        super(length, pesNumber);
        segments = new ArrayList<>();
        this.length = length;
        setId(CURRENT_ACCELERABLE_CLOUDLET_ID++);
    }

    public AccelerableCloudlet(long length, long pesNumber) {
        super(length, pesNumber);
        segments = new ArrayList<>();
        this.length = length;
        setId(CURRENT_ACCELERABLE_CLOUDLET_ID++);
    }

    public AccelerableCloudlet(long id, long length, long pesNumber) {
        super(id, length, pesNumber);
        segments = new ArrayList<>();
        this.length = length;
        setId(CURRENT_ACCELERABLE_CLOUDLET_ID++);
    }

    public void resetIterator() {
        iterator = segments.iterator();
    }

    public AccelerableSegment getNextSegment() {
        if (iterator == null)
            resetIterator();
        if (iterator.hasNext())
            return iterator.next();
        else
            return null;
    }

    public CloudletSimple getCloudletWithoutAccelerableSegments() {
        CloudletSimple cloudlet = new CloudletSimple(getId(), getTotalLength() - getAccelerableLength(),
                getNumberOfPes());
        cloudlet.setBroker(getBroker());
        cloudlet.setJobId(getJobId());
        cloudlet.setFileSize(getFileSize());
        cloudlet.setExecStartTime(getExecStartTime());
        cloudlet.setSubmissionDelay(getSubmissionDelay());
        cloudlet.setOutputSize(getOutputSize());
        cloudlet.setStatus(getStatus());
        cloudlet.setNetServiceLevel(getNetServiceLevel());
        cloudlet.setPriority(getPriority());
        cloudlet.setLastTriedDatacenter(getLastTriedDatacenter());
        cloudlet.setVm(getVm());
        nonAccelerableSegments = cloudlet;
        return cloudlet;
    }

    public AccelerableSegment getSegmentById(int id) {
        for (AccelerableSegment segment : segments)
            if (segment.getId() == id)
                return segment;
        return null;
    }

    public boolean addSegment(long index, long length, int type) {
        return addSegment(new AccelerableSegment(segments.size() + 1, index, length, this, type));
    }

    public boolean addSegment(AccelerableSegment newSegment) {
        for (AccelerableSegment segment : segments)
            if (segment.getId() == newSegment.getId())
                return false;

        long newStartIndex = newSegment.getIndex();
        long newEndIndex = newStartIndex + newSegment.getLength() - 1;
        for (AccelerableSegment segment : segments) {
            long startIndex = segment.getIndex();
            long endIndex = startIndex + segment.getLength() - 1;
            if ((startIndex >= newStartIndex && startIndex <= newEndIndex) || (endIndex >= newStartIndex && endIndex <= newEndIndex))
                return false;
        }

        return segments.add(newSegment);
    }

    public void setOverallFinishTime(double finishTime) {
        this.finishTime = finishTime;
    }

    @Override
    public double getFinishTime() {
        return finishTime;
    }

    public int getAccelerableSegmentCount() {
        return segments.size();
    }

    public long getAccelerableLength() {
        return segments.stream().mapToLong(AccelerableSegment::getLength).sum();
    }

    public CloudletSimple getNonAccelerableSegments() {
        return nonAccelerableSegments;
    }

    public boolean hasAccelerableLength() {
        return !segments.isEmpty();
    }

    public AccelerableSegment getSegmentAt(int index) {
        return segments.get(index);
    }

    public List<AccelerableSegment> getSegments() {
        return segments;
    }

    public void visualizeCloudlet() {
        resetIterator();
        char symbolAcc = '=';
        char symbolNonAcc = '-';

        // determine major divisor
        int major = 1;
        if (length > 10000000) {
            System.out.println("Cloudlet length is too long to visualize!");
            return;
        } else if (length > 2000000)
            major = 1000000;
        else if (length > 200000)
            major = 100000;
        else if (length > 20000)
            major = 10000;
        else if (length > 2000)
            major = 1000;
        else if (length > 200)
            major = 100;
        else if (length > 20)
            major = 10;

        // determine number of digits
        int digits = 0;
        long temp = length;
        while (temp != 0) {
            temp /= 10;
            ++digits;
        }

        // determine minor divisor
        int minor = 10;
        if (major == 1)
            minor = 1;

        long leftIndex = 0;
        long currentIndex = -1;
        long rightIndex = major - 1;
        long segmentStart = -1;
        long segmentEnd = -1;
        char symbol = symbolNonAcc;
        int totalLines = (int) (length / major);
        // check if length is not a value of the form m*10^n
        double decimal = (length / (double) major) - totalLines;
        if (decimal > 0.0)
            ++totalLines;

        AccelerableSegment currentSegment = null;

        for (int currentLine = 0; currentLine < totalLines; ++currentLine) {
            String line = StringUtils.leftPad(Long.toString(leftIndex), digits, " ");
            if (iterator.hasNext() && currentSegment == null) {
                currentSegment = getNextSegment();
                segmentStart = currentSegment.getIndex();
                segmentEnd = segmentStart + currentSegment.getLength() - 1;
            }

            int symbolCount = 0;
            line += " ";
            // symbol change expected at start or in between for segment start
            if (segmentStart != -1 && segmentStart >= currentIndex && segmentStart <= rightIndex)
                while (symbolCount < minor) {
                    if (segmentStart >= currentIndex && segmentStart <= (currentIndex + major / minor))
                        symbol = symbolAcc;
                        // check if segment starts and ends within current line
                    else if (currentIndex >= segmentEnd) {
                        symbol = symbolNonAcc;
                        currentSegment = null;
                        if (iterator.hasNext()) {
                            currentSegment = getNextSegment();
                            segmentStart = currentSegment.getIndex();
                            segmentEnd = segmentStart + currentSegment.getLength() - 1;
                            continue;
                        }
                    }
                    line += symbol;
                    ++symbolCount;
                    currentIndex += (major / minor);
                }
                // symbol change expected at start or in between for segment end
            else if (segmentStart != -1 && segmentEnd >= currentIndex && segmentEnd <= rightIndex)
                while (symbolCount < minor) {
                    if (segmentStart >= currentIndex && segmentStart <= (currentIndex + major / minor))
                        symbol = symbolAcc;
                    else if (currentIndex >= segmentEnd) {
                        symbol = symbolNonAcc;
                        currentSegment = null;
                        if (iterator.hasNext()) {
                            currentSegment = getNextSegment();
                            segmentStart = currentSegment.getIndex();
                            segmentEnd = segmentStart + currentSegment.getLength() - 1;
                            continue;
                        }
                    }
                    line += symbol;
                    ++symbolCount;
                    currentIndex += (major / minor);
                }
                // check if last line
            else if (rightIndex > length - 1) {
                rightIndex = length - 1;
                while (symbolCount < minor && currentIndex <= rightIndex) {
                    if (segmentStart >= currentIndex && segmentStart <= (currentIndex + major / minor))
                        symbol = symbolAcc;
                        // check if segment starts and ends within current line
                    else if (currentIndex >= segmentEnd)
                        symbol = symbolNonAcc;
                    line += symbol;
                    ++symbolCount;
                    currentIndex += (major / minor);
                }
                while (symbolCount < minor) {
                    line += " ";
                    ++symbolCount;
                }
            } else
                while (symbolCount < minor) {
                    line += symbol;
                    ++symbolCount;
                    currentIndex += (major / minor);
                }
            if (currentIndex >= segmentEnd) {
                symbol = symbolNonAcc;
                currentSegment = null;
            }
            line += " ";

            System.out.println(line + rightIndex);
            leftIndex = rightIndex + 1;
            rightIndex = leftIndex + major - 1;
        }
        resetIterator();
    }

    @Override
    public String toString() {
        return String.format("Cloudlet %d", getId());
    }

    @Override
    public int compareTo(final Cloudlet o) {
        return Long.compare(getLength(), o.getLength());
    }
}
