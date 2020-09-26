package org.cloudsimfe;

public interface PartitionPolicy {
    void partition();
    void setOption(Object... options);
    void setFpga(Fpga fpga);
}
