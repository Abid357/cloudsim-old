/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cloudsimfe;

import java.util.*;

/**
 * Any device in a network requires an IP address if one wishes to access the device through the network. Each device
 * in the network acquires a unique IP address so that the user can specify the target device for communication.
 * Admins may assign different IP addresses to all the devices in the network statically. Static IP addressing,
 * however, is not feasible when it comes to large networks. Therefore, dynamic IP addressing is more suitable and
 * dynamic addressing is carried out by what is called Dynamic Host Configuration Protocol (DHCP) server. In other
 * words, assigning of IP addresses to devices is automated through protocols between host devices and a server.
 * <p>
 * The DHCP server reserves pools of IP addresses, available for being assigned to any device that requests for an IP
 * address. In a table, several network addresses and their subnet masks are stored. Furthermore, this class also
 * maintains separate lists for IP addresses that have been offered, assigned and excluded. As a device requests for an
 * address, the server first offers an address from the pools of available IP addresses. If the device accepts, it
 * notifies the server after which the server sends back an acknowledgement (ACK) signal and adds the offered
 * address into the list of assigned IP addresses. At a later time, if the device no longer has any use of the
 * address, it can release the address so that the server acquires it and adds it back to the pools of available IP
 * addresses, thus recycling addresses in this manner. Lastly, admins may specify certain IP addresses to be excluded
 * or reserved. This is mainly done to keep a handful of addresses for the purpose of static IP addressing.
 *
 * @author Abid Farhan
 * @version CloudSim Plus 1.0
 * @see org.cloudsimfe.Addressable
 */
public class DhcpServer {

    /**
     * List of IP addresses that have been assigned to different devices.
     */
    private Set<String> assignedIps;

    /**
     * List of IP addresses that have been excluded from dynamic addressing and reserved for static addressing.
     */
    private Set<String> excludedIps;

    /**
     * List of IP addresses that have been offered to devices and the server is awaiting their acknowledgement signals.
     */
    private Set<String> offeredIps;

    /**
     * List of network addresses mapped to respective subnet masks to form pools of available IP addresses.
     */
    private Map<String, String> pools;

    /**
     * Default constructor that initializes all lists and the address pools with default private network addresses.
     */
    public DhcpServer() {
        assignedIps = new HashSet<String>();
        excludedIps = new HashSet<String>();
        offeredIps = new HashSet<String>();
        pools = new HashMap<String, String>();

        // default private address pools
        pools.put("192.168.0.0", "255.255.255.0");
        pools.put("10.0.0.0", "255.0.0.0");
        pools.put("172.16.0.0", "255.240.0.0");
    }

    /**
     * Adds a new pool of addresses to the server.
     *
     * @param network the network address
     * @param mask    the subnet mask
     * @return true if the given network address does not already exist and is added newly, false otherwise
     * @see org.cloudsimfe.DhcpServer#pools
     */
    public boolean addPool(String network, String mask) {
        if (pools.containsKey(network)) {
            return false;
        } else {
            pools.put(network, mask);
            return true;
        }
    }

    /**
     * Gets the next available IP address from a specific pool of addresses. The pool is specified by the network
     * address and the subnet mask as parameters.
     * <p>
     * An IPv4 address uses a 32-bit address space and can be written in dot-decimal notation. For example, consider
     * the private network address 192.168.1.0 where 4 decimals are separated by a single dot. Moreover, the subnet
     * mask for this address, which is 255.255.0.0, indicates how many bits are network bits and how many are host
     * bits. For more info on IP addresses, see https://en.wikipedia.org/wiki/IPv4.
     * <p>
     * The function splits all parameters into 4 segments for further operations. This means the network address 192
     * .168.1.0 is broken down to 4 segments where the first segment is the decimal 192 and the second is 168 and so
     * on. For each segment, the max decimal value (upper limit) is calculated using the complement of the subnet mask.
     * Then, the last segment (4th segment) is incremented to fetch the next valid host address. Whenever the current
     * segment exceeds max limit, it is reset to 0 and the preceding segment is incremented by 1. Then it continues
     * to increment the current segment again until all possible addresses are exhausted and the max limit is reached
     * . For example, 192.168.1.1 is a valid host address. If this has been already assigned to a device, the server
     * gets the next available, valid address using this function, which would be 192.168.1.2. Based on the subnet
     * mask, the upper limit for this example would be 255, hence 192.168.1.255 would be the last valid address,
     * after which the next valid address is 192.168.2.0.
     *
     * @param host    the current host address
     * @param mask    the subnet mask
     * @param network the network address
     * @return the next host address that is valid
     */
    private String getNextIpAddress(String host, String mask, String network) {
        // divide each address and the mask into 4 segments or bytes
        String[] tokens = network.split("\\.");
        int nbyte1 = Integer.parseInt(tokens[0]);
        int nbyte2 = Integer.parseInt(tokens[1]);
        int nbyte3 = Integer.parseInt(tokens[2]);
        int nbyte4 = Integer.parseInt(tokens[3]);

        tokens = host.split("\\.");
        int a = Integer.parseInt(tokens[0]);
        int b = Integer.parseInt(tokens[1]);
        int c = Integer.parseInt(tokens[2]);
        int d = Integer.parseInt(tokens[3]);

        tokens = mask.split("\\.");
        int mbyte1 = Integer.parseInt(tokens[0]);
        int mbyte2 = Integer.parseInt(tokens[1]);
        int mbyte3 = Integer.parseInt(tokens[2]);
        int mbyte4 = Integer.parseInt(tokens[3]);

        // flip bits (1's complement) for each mask byte to obtain upper limit
        int limit1 = ~mbyte1 & 255;
        int limit2 = ~mbyte2 & 255;
        int limit3 = ~mbyte3 & 255;
        int limit4 = ~mbyte4 & 255;

        ++d; // increment 4th segment
        if (d > nbyte4 + limit4) {
            ++c; // increment 3rd segment
            d = nbyte4;
            if (c > nbyte3 + limit3) {
                ++b; // increment 2nd segment
                c = nbyte3;
                if (b > nbyte2 + limit2) {
                    ++a; // increment 1st segment
                    b = nbyte2;
                    if (a > nbyte1 + limit1) {
                        return null;
                    }
                }
            }
        }
        return Integer.toString(a) + "." + Integer.toString(b) + "." + Integer.toString(c) + "." + Integer.toString(d);
    }

    /**
     * Sends an acknowledgement (ACK) signal to the host device if the IP address specified in the parameter was
     * acquired by the device for use. An ACK would only be sent provided the host device had initially requested for
     * an address and the server had offered a valid host address to the device. Once acknowledged, the host address
     * is added to the list of assigned IP addresses and it is removed from the list of offered IP addresses.
     *
     * @param host the host address acquired by a device
     * @return true if sending a positive ACK signal, false otherwise
     */
    public boolean sendAck(String host) {
        if (offeredIps.contains(host)) {
            assignedIps.add(host);
            offeredIps.remove(host);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Receives a request for IP address from a host device and sends back a valid, available IP addresses from the
     * address pools in the server.
     *
     * @return a valid, available IP address
     * @see org.cloudsimfe.DhcpServer#getNextIpAddress(String, String, String)
     */
    public String requestIpAddress() {
        String host = null;
        Iterator<String> networks = pools.keySet().iterator();
        while (networks.hasNext() && host == null) {
            String network = networks.next();
            String mask = pools.get(network);
            host = getNextIpAddress(network, mask, network);
            while (host != null && (assignedIps.contains(host) || excludedIps.contains(host))) {
                host = getNextIpAddress(host, mask, network);
            }
        }
        if (host != null) {
            offeredIps.add(host);
        }
        return host;
    }

    /**
     * Checks if a given IP address is a valid address based on network addresses in the server. Similar to the
     * function that returns the next valid address, this function divides the address into 4 segments for further
     * processing.
     *
     * @param address the IP address to be checked for validity
     * @return true if address is valid, false otherwise
     * @see org.cloudsimfe.DhcpServer#getNextIpAddress(String, String, String)
     */
    public boolean isValidIpAddress(String address) {
        Iterator<String> networks = pools.keySet().iterator();
        boolean valid = false;
        while (networks.hasNext() && !valid) {
            String network = networks.next();
            String mask = pools.get(network);

            // divide each address and the mask into 4 segments or bytes
            String[] tokens = network.split("\\.");
            int nbyte1 = Integer.parseInt(tokens[0]);
            int nbyte2 = Integer.parseInt(tokens[1]);
            int nbyte3 = Integer.parseInt(tokens[2]);
            int nbyte4 = Integer.parseInt(tokens[3]);

            tokens = address.split("\\.");
            int a = Integer.parseInt(tokens[0]);
            int b = Integer.parseInt(tokens[1]);
            int c = Integer.parseInt(tokens[2]);
            int d = Integer.parseInt(tokens[3]);

            tokens = mask.split("\\.");
            int mbyte1 = Integer.parseInt(tokens[0]);
            int mbyte2 = Integer.parseInt(tokens[1]);
            int mbyte3 = Integer.parseInt(tokens[2]);
            int mbyte4 = Integer.parseInt(tokens[3]);

            // perform bit-wise XOR between network bytes and host bytes, then AND the result with respective mask bytes
            int result = ~(nbyte1 ^ a) & mbyte1;
            if (result == mbyte1) {
                result = ~(nbyte2 ^ b) & mbyte2;
                if (result == mbyte2) {
                    result = ~(nbyte3 ^ c) & mbyte3;
                    if (result == mbyte3) {
                        result = ~(nbyte4 ^ d) & mbyte4;
                        if (result == mbyte4) {
                            valid = true;
                        }
                    }
                }
            }
        }
        return valid;
    }

    /**
     * Gets the requested IP address from the server's address pools. This function is used for static IP addressing.
     *
     * @param address the requested IP address for the host
     * @return true if the address is both valid and available, false otherwise
     */
    public boolean requestIpAddress(String address) {
        if (isValidIpAddress(address)) {
            if (assignedIps.contains(address))
                return false;
            offeredIps.add(address);
            return true;
        }
        return false;
    }

    /**
     * Acquires back an IP address that is released by the host and adds it back to the address pools in the server.
     *
     * @param address the released IP address of the host
     * @return true if the address is successfully removed from the list of assigned addresses and put back into the
     * address pools, false otherwise
     */
    public boolean releaseIpAddress(String address) {
        if (assignedIps.contains(address)) {
            assignedIps.remove(address);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Excludes or reserves an IP address for the purpose of static addressing.
     * @param address the IP address to be excluded
     * @return true if the address is successfully added to the list of excluded addresses, false otherwise
     */
    public boolean excludeIpAddress(String address) {
        if (isValidIpAddress(address)) {
            if (assignedIps.contains(address) || excludedIps.contains(address))
                return false;
            if (offeredIps.contains(address))
                offeredIps.remove(address);

            excludedIps.add(address);
            return true;
        }
        return false;
    }
}
