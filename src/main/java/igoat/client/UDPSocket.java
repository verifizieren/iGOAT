/**
 * Interface for UDP sockets. This interface is used to send UDP packets.
 */
package igoat.client;

import java.io.IOException;
import java.net.DatagramPacket;

public interface UDPSocket {
    void send(DatagramPacket packet) throws IOException;

    void close();
}
