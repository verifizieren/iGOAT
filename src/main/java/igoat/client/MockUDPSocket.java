package igoat.client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.util.ArrayList;
import java.util.List;

/**
 * Mock implementation of UDPSocket for testing.
 */
public class MockUDPSocket implements UDPSocket {
    public final List<DatagramPacket> sentPackets = new ArrayList<>();
    @Override
    public void send(DatagramPacket packet) throws IOException {
        sentPackets.add(packet);
    }
    @Override
    public void close() {}
}
