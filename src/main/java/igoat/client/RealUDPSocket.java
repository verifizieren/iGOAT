/**
 * A real UDP socket implementation, which wraps a {@link DatagramSocket}.
 */
package igoat.client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class RealUDPSocket implements UDPSocket {
    private final DatagramSocket socket;

    public RealUDPSocket(DatagramSocket socket) {
        this.socket = socket;
    }

    @Override
    public void send(DatagramPacket packet) throws IOException {
        socket.send(packet);
    }

    @Override
    public void close() {
        socket.close();
    }

    public DatagramSocket getSocket() {
        return socket;
    }
}
