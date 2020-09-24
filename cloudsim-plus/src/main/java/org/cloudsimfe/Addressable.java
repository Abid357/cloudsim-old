package org.cloudsimfe;

/**
 * An interface for implementing different functions of any class or module that can contain an IP address. In other
 * words, the object of that class is addressable using IPv4/IPv6 addresses. This interface allows to attach and
 * detach an address from the object. It also allows to retrieve the currently assigned address, and furthermore,
 * send data to the object.
 * <p>
 * It is an important feature of the simulator as it enables users to communicate directly to an addressable module
 * simply by knowing its IP address and sending data, all using the provided method(s).
 *
 * @author Abid Farhan
 * @version CloudSim Plus 1.0
 */
public interface Addressable {
    /**
     * Assigns an IP address to the object. This necessitates the object to have a string attribute to hold the address.
     *
     * @param ipAddress the IP address
     */
    void assignIpAddress(String ipAddress);

    /**
     * Withdraws or detaches the IP address that was previously assigned to the object. The string attribute that
     * held the IP address should become null.
     *
     * @return the IP address that has been withdrawn from the object
     */
    String withdrawIpAddress();

    /**
     * Gets the assigned IP address to the object.
     *
     * @return the IP address that is assigned
     */
    String getIpAddress();

    /**
     * Sends payload data to this function. The data can be of any type, which is why the type of data is Object
     * (generic).
     * This will require casting to appropriate data type when implementing the function in a class.
     *
     * @param payload the data
     */
    void sendDataToComponent(Payload payload);
}
