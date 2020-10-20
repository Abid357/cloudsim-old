package org.cloudsimfe;

import org.cloudbus.cloudsim.core.CustomerEntityAbstract;
import org.cloudbus.cloudsim.util.Conversion;

public class Accelerator extends CustomerEntityAbstract implements Clockable {

    public static final int TYPE_IMAGE_PROCESSING = 0;
    public static final int TYPE_ENCRYPTION = 1;
    public static final int TYPE_FAST_FOURIER_TRANSFORM = 2;
    public static final int TYPE_VIDEO_PROCESSING = 3;

    /**
     * A static counter to automate ID generation of the accelerators
     */
    public static int CURRENT_ACCELERATOR_ID = 1;

    /**
     * @see #status
     */
    private static final int IDLE = 0;

    /**
     * @see #status
     */
    private static final int BUSY = 1;

    private int id;

    private long clock;

    private int inputChannels;

    private int outputChannels;

    /**
     * Million instructions per second (MIPS): a rough estimation of MIPS could be clock frequency
     * divided by cpi. For example, if cpi is 1, then in every clock cyse, one instruction is executed.
     * Therefore for a 50 MHz clock, MIPS is 50.
     * private long mips;
     */
    private long mflops;

    /**
     * Concurrency: this is where hardware acceleration gains truly come from. The value defines
     * in discrete integer how many parallel lines of execution can be run simultaneously to process
     * the cloudlet. For a brief idea, if an ordinary cloudlet has 10 MI and a processor core has 10 MIPS
     * it would take 1 second to finish executing the cloudlet. However, if a hardware accelerator is used
     * with a concurrency value of 2, the result should be close to 0.5 second.
     */
    private int concurrency;

    /**
     * Status (of the hardware accelerator): there are only two possible values; BUSY if the
     * accelerator is executing a cloudlet at the current time and hence no other cloudlet can be
     * submitted at this time, IDLE otherwise.
     */
    private int status;

    private double arrivalTime;
    private SegmentExecution se;
    private AccelerableSegment currentSegment;
    private double submissionDelay;
    private VFpga vFpga;
    private int type;
    private Adapter adapter;

    public Accelerator(final int id, final long mflops, final int concurrency, final int type, final long clock,
                       final int inputChannels, final int outputChannels, final double submissionDelay){
        this.id = id;
        this.mflops = mflops;
        this.concurrency = concurrency;
        this.type = type;
        this.clock = clock;
        this.inputChannels = inputChannels;
        this.outputChannels = outputChannels;
        status = IDLE;
        this.submissionDelay = submissionDelay;
    }

    public Accelerator(final int id, final long mflops, final int concurrency, final int type) {
        this(id, mflops, concurrency, type, 50, 3, 3, 0);
    }

    public Accelerator copy(){
        Accelerator accelerator = new Accelerator(id, mflops, concurrency, type, clock, inputChannels, outputChannels,
            submissionDelay);
        accelerator.setBroker(getBroker());
        return accelerator;
    }

    /**
     * Submits the accelerable cloudlet segment that will be processed / accelerated by the accelerator provided the
     * accelerator status was initially IDLE. Then, it uses the SegmentExecution class which has some useful methods
     * for processing.
     *
     * @param fileTransferTime time taken in seconds to transfer cloudlet segment from datacenter to the accelerator.
     * @return total estimated time to finish accelerating cloudlet segment if cloudlet submission was successful
     */
    public double segmentSubmit(final double fileTransferTime) {
        if (status == IDLE) {
            Payload payload = adapter.readFromBuffer(Adapter.READ_BUFFER);
            currentSegment = (AccelerableSegment) payload.getData().get(0);
            currentSegment.setAccelerator(this);
            se = new SegmentExecution(currentSegment);
            vFpga.addSegmentExecution(se);
            se.setLastProcessingTime(getSimulation().clock());
            status = BUSY;
            return fileTransferTime + (currentSegment.getLength() / (double) (mflops * concurrency));
        }
        return -1.0;
    }

    private boolean hasCloudletFileTransferTimePassed(final double currentTime) {
        return se.getFileTransferTime() == 0 ||
                currentTime - se.getSegmentArrivalTime() > se.getFileTransferTime() ||
                se.getSegment().getFinishedLengthSoFar() > 0;
    }

    private double cloudletExecutedInstructionsForTimeSpan(final double currentTime) {
        double timeSpan = currentTime - se.getLastProcessingTime();
        final double actualProcessingTime = hasCloudletFileTransferTimePassed(currentTime) ? timeSpan : 0.0;
        return concurrency * mflops * actualProcessingTime * Conversion.MILLION;
    }

    public double updateProcessing(final double currentTime) {
        if (currentSegment == null)
            return Double.MAX_VALUE;

        double partialFinishedInstructions = 0.0;
        partialFinishedInstructions += cloudletExecutedInstructionsForTimeSpan(currentTime);
        se.updateProcessing(partialFinishedInstructions);

        double estimatedFinishTime = se.getRemainingSegmentLength() / (double) (mflops * concurrency);

        if (estimatedFinishTime == 0) {
            estimatedFinishTime = Double.MAX_VALUE;
            se.setFinishTime(currentTime);

            Payload payload = new Payload(se);
            adapter.writeToBuffer(payload, Adapter.WRITE_BUFFER);

            status = IDLE;
            currentSegment = null;
        }

        return Math.max(estimatedFinishTime, getSimulation().getMinTimeBetweenEvents());
    }

    public Adapter getAdapter() {
        return adapter;
    }

    public void setAdapter(Adapter adapter) {
        this.adapter = adapter;
    }

    public long getMflops() {
        return mflops;
    }

    public VFpga getVFpga() {
        return vFpga;
    }

    void setVFpga(VFpga vFpga) {
        this.vFpga = vFpga;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(final int status) {
        this.status = status;
    }

    public long getClock() {
        return clock;
    }

    public void setClock(long clock) {
        this.clock = clock;
    }

    public int getAcceleratorId() {
        return this.id;
    }

    public void setId(final int id) {
        this.id = id;
    }

    public int getConcurrency() {
        return concurrency;
    }

    public void setConcurrency(final int concurrency) {
        this.concurrency = concurrency;
    }

    public void setArrivalTime(double time) {
        arrivalTime = time;
    }

    public int getType() {
        return type;
    }

    public String getTypeInString(){
        switch(type){
            case TYPE_IMAGE_PROCESSING:
                return "Image Processing";
            case TYPE_ENCRYPTION:
                return "Encryption";
            case TYPE_FAST_FOURIER_TRANSFORM:
                return "Fast Fourier Transformation (FFT)";
            case TYPE_VIDEO_PROCESSING:
                return "Video Processing";
        }
        return null;
    }

    public void setType(int type) {
        this.type = type;
    }

    public SegmentExecution getSegmentExecution() {
        return se;
    }

    public void setSegmentExecution(SegmentExecution se) {
        this.se = se;
    }

    @Override
    public double getSubmissionDelay() {
        return submissionDelay;
    }

    @Override
    public void setSubmissionDelay(final double submissionDelay) {
        this.submissionDelay = submissionDelay;
    }

    @Override
    public long getClockValue() {
        return clock;
    }

    @Override
    public String getComponentId() {
        return getClass().getSimpleName() + id;
    }

    public int getInputChannels() {
        return inputChannels;
    }

    public void setInputChannels(int inputChannels) {
        this.inputChannels = inputChannels;
    }

    public int getOutputChannels() {
        return outputChannels;
    }

    public void setOutputChannels(int outputChannels) {
        this.outputChannels = outputChannels;
    }

    public void setMflops(long mflops) {
        this.mflops = mflops;
    }

    public double getArrivalTime() {
        return arrivalTime;
    }

    @Override
    public String toString() {
        return "Accelerator " + id + " specifications:\n" +
                "Type: " + getTypeInString() + "\n" +
                "Capacity: " + mflops + " MFLOPS\n" +
                "Concurrency: " + concurrency + "\n" +
                "Clock: " + clock + " MHz\n" +
                "Input Channels: " + inputChannels + "\n" +
                "Output Channels: " + outputChannels + "\n" +
                "Status: " +  (status == Accelerator.IDLE ? "IDLE" : "BUSY");
    }
}
