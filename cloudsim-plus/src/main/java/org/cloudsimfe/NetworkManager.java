package org.cloudsimfe;

import java.util.HashMap;
import java.util.Map;

public class NetworkManager extends AddressableComponent implements Clockable {

    public static String NETWORK_MANAGER_ADDRESS;
    public static String CONFIGURATION_MANAGER_ADDRESS;

    private Fpga fpga;
    private DhcpServer server;
    private Router router;
    private Map<String, Addressable> routingTable;
    private Map<String, String> sessionMemory;

    public NetworkManager(DhcpServer server) {
        this.server = server;
        this.router = new Router(this);
        routingTable = new HashMap<>();
        sessionMemory = new HashMap<>();
    }

    public boolean registerRoute(String ipAddress, Addressable component) {
        if (routingTable.containsKey(ipAddress))
            return false;
        else {
            routingTable.put(ipAddress, component);
            return true;
        }
    }

    public boolean unregisterRoute(String ipAddress) {
        if (!routingTable.containsKey(ipAddress))
            return false;
        else {
            routingTable.remove(ipAddress);
            return true;
        }
    }

    public void updateRoute(String oldAddress, String newAddress) {
        if (routingTable.containsKey(oldAddress)) {
            Addressable component = routingTable.remove(oldAddress);
            registerRoute(newAddress, component);
        }
    }

    public Map<String, Addressable> getRoutingTable() {
        return routingTable;
    }

    private boolean openSession(String vFpgaAddress, String userAddress) {
        if (!sessionMemory.containsKey(vFpgaAddress)) {
            sessionMemory.put(vFpgaAddress, userAddress);
            return true;
        } else
            return false;
    }

    private boolean closeSession(String vFpgaAddress) {
        if (!sessionMemory.containsKey(vFpgaAddress))
            return false;
        else{
            sessionMemory.remove(vFpgaAddress);
            return true;
        }
    }

    public String getSession(String vFpgaAddress){
        return sessionMemory.get(vFpgaAddress);
    }

    @Override
    public void sendDataToComponent(Payload payload) {
        switch (payload.getData().get(0).getClass().getSimpleName()) {
            case "ConfigurationManager":
                ConfigurationManager manager = (ConfigurationManager) payload.getData().get(0);
                String ipAddress = server.requestIpAddress();
                manager.assignIpAddress(ipAddress);
                server.sendAck(ipAddress);
                registerRoute(ipAddress, manager);
                CONFIGURATION_MANAGER_ADDRESS = ipAddress;
                break;
            case "Packet": {
                Packet packet = (Packet) payload.getData().get(0);
                if (packet.getType() == Packet.BITSTREAM) {
                    if (packet.getDestinationAddress().equals(CONFIGURATION_MANAGER_ADDRESS))
                        packet.getPayload().addData(packet.getSourceAddress());
                    else
                        return;
                } else if (packet.getType() == Packet.VFPGA_INPUT) {
                    String vFpgaAddress = packet.getDestinationAddress();
                    String userAddress = packet.getSourceAddress();

                    String session = getSession(vFpgaAddress);
                    if (session != null && session.equals(userAddress))
                        packet.getPayload().addData(vFpgaAddress);
                    else
                        packet.getPayload().addData(false); // error - user session does not exist
                }
                router.writeToBuffer(packet.getPayload());
                router.route(packet.getDestinationAddress());
            }
            break;
            case "VFpga": {
                if (payload.getData().size() == 1) {
                    VFpga vFpga = (VFpga) payload.getData().get(0);
                    vFpga.assignIpAddress(server.requestIpAddress());
                    server.sendAck(vFpga.getIpAddress());
                    registerRoute(vFpga.getIpAddress(), vFpga.getManager());
                } else if (payload.getData().size() == 2) {
                    VFpga vFpga = (VFpga) payload.getData().get(0);
                    String dstAddress = (String) payload.getData().get(1);
                    payload.removeData(1);

                    openSession(vFpga.getIpAddress(), dstAddress);

                    Packet packet = new Packet(NETWORK_MANAGER_ADDRESS, dstAddress, Packet.VFPGA_INFO, payload);
                    router.writeToBuffer(new Payload(packet));
                    router.route(dstAddress);
                }
            }
            break;
            case "SegmentExecution": {
                String vFpgaAddress = (String) payload.getData().get(2);
                String session = getSession(vFpgaAddress);
                if (session != null){
                    payload.removeData(2);
                    Packet packet = new Packet(vFpgaAddress, session, Packet.VFPGA_OUTPUT, payload);
                    closeSession(vFpgaAddress);
                    router.writeToBuffer(new Payload(packet));
                    router.route(session);
                } else {
                    payload.addData(false); // error - user session does not exist
                    router.writeToBuffer(payload);
                    router.route(vFpgaAddress);
                }
            }
        }
    }

    public Fpga getFpga() {
        return fpga;
    }

    public void setFpga(Fpga fpga) {
        this.fpga = fpga;
    }

    public Router getRouter() {
        return router;
    }

    public DhcpServer getServer() {
        return server;
    }

    @Override
    public String toString() {
        String entries = "";
        for (Map.Entry<String, Addressable> entry : routingTable.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue().getClass().getSimpleName();
            entries += key + "=" + value + '\n';
        }
        return "Router{" +
                "routingTable=" + "[\n" + entries + ']' +
                '}';
    }

    @Override
    public long getClockValue() {
        return fpga.getClock();
    }

    @Override
    public String getComponentId() {
        return getClass().getSimpleName() + "-FPGA" + fpga.getId();
    }
}
