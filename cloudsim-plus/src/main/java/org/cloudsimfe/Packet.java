package org.cloudsimfe;

public class Packet {

    public static final int VFPGA_OUTPUT = 0;
    public static final int BITSTREAM = 1;
    public static final int VFPGA_INFO = 2;
    public static final int VFPGA_INPUT = 2;

    private String srcAddress;
    private String dstAddress;
    private int type;
    private Payload payload;

    public Packet(String srcAddress, String dstAddress, int type, Payload payload) {
        this.srcAddress = srcAddress;
        this.dstAddress = dstAddress;
        this.type = type;
        this.payload = payload;
    }

    public String getSourceAddress() {
        return srcAddress;
    }

    public void setSourceAddress(String srcAddress) {
        this.srcAddress = srcAddress;
    }

    public String getDestinationAddress() {
        return dstAddress;
    }

    public void setDestinationAddress(String dstAddress) {
        this.dstAddress = dstAddress;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public Payload getPayload() {
        return payload;
    }

    public void setPayload(Payload payload) {
        this.payload = payload;
    }

    @Override
    public String toString() {
        return "Packet{" +
                "srcAddress='" + srcAddress + '\'' +
                ", dstAddress='" + dstAddress + '\'' +
                ", type=" + type +
                ", data=" + payload +
                '}';
    }
}
