package org.cloudsimfe;

public abstract class AddressableComponent implements Addressable {

    private String ipAddress;

    @Override
    public void assignIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    @Override
    public String withdrawIpAddress() {
        String temp = ipAddress;
        ipAddress = null;
        return temp;
    }

    @Override
    public String getIpAddress() {
        return ipAddress;
    }

    @Override
    public abstract void sendDataToComponent(Payload payload);
}
