package org.cloudsimfe;

import java.util.LinkedList;
import java.util.Queue;

public class Router {

    private Queue<Payload> buffer;
    private NetworkManager manager;

    public Router(NetworkManager manager) {
        this.manager = manager;
        buffer = new LinkedList<>();
    }

    public void route(String ipAddress) {
        if (!buffer.isEmpty()) {
            Payload payload = readFromBuffer();

            if (ipAddress == null)
                return;

            Addressable component = manager.getRoutingTable().get(ipAddress);
            if (component != null)
                component.sendDataToComponent(payload);
        }
    }

    public Payload readFromBuffer() {
        return buffer.poll();
    }

    public void writeToBuffer(Payload payload) {
        buffer.add(payload);
    }

    public NetworkManager getManager() {
        return manager;
    }

}
