package org.cloudsimfe;

public class PartitionPolicyGrid implements PartitionPolicy {

    public static final int OPTION_GRID_ROWS = 0;
    public static final int OPTION_GRID_COLS = 1;

    private int rows = 1;
    private int cols = 1;
    private Fpga fpga;

    public void partition() {
        int width = (int) (fpga.getWidth() / cols);
        int length = (int) (fpga.getLength() / rows);
        int partitions = rows * cols;
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++) {
                RectangleRegion region = new RectangleRegion.Builder(width * c, length * r, width - 1, length - 1, fpga)
                        .setLogicElements(fpga.getLogicElements() / partitions)
                        .setMemoryRegisters(fpga.getMemory() / partitions)
                        .setDspSlices(fpga.getDspSlices() / partitions)
                        .setIoPins(fpga.getIo() / partitions)
                        .build();
                fpga.getConfigurationManager().getRegions().add(region);
            }
        long[][] map = new long[rows][cols];
        fpga.getConfigurationManager().setMap(map);
    }

    @Override
    public void setOption(Object... options) {
        if (options.length != 2)
            return;

        int value = (int) options[0];
        int option = (int) options[1];

        if (value < 0)
            return;

        if (option == OPTION_GRID_ROWS)
            rows = value;
        else if (option == OPTION_GRID_COLS)
            cols = value;
    }

    @Override
    public void setFpga(Fpga fpga) {
        this.fpga = fpga;
    }
}
