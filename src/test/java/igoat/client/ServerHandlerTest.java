package igoat.client;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.BufferedReader;
import java.io.StringReader;
import java.net.DatagramPacket;

public class ServerHandlerTest {
    static class TestServerHandler extends ServerHandler {
        public TestServerHandler(String host, int port, String username) {
            super(host, port, username, true); // nonetwork constructor
        }
        public void setMsgWriter(PrintWriter writer) { this.msgWriter = writer; }
        public void setUpdateSocket(UDPSocket socket) { this.updateSocket = socket; }
        public void setMsgReader(BufferedReader reader) { this.msgReader = reader; }
        public void setConnected(boolean c) { this.connected = c; }
        public void setLastUpdate(String update) { this.lastUpdate = update; }
        public void setConfirmedNickname(String n) { this.confirmedNickname = n; }
    }

    @Test
    public void testConfirmedNicknameGetter() {
        TestServerHandler handler = new TestServerHandler("localhost", 12345, "testuser");
        handler.setConfirmedNickname("confirmedUser");
        String nickname = handler.getConfirmedNickname();
        assertEquals("confirmedUser", nickname);
    }

    @Test
    public void testSendMessageSendsCorrectString() {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        TestServerHandler handler = new TestServerHandler("localhost", 12345, "testuser");
        handler.setMsgWriter(pw);
        handler.sendMessage("chat:hello");
        pw.flush();
        assertTrue(sw.toString().contains("chat:hello"));
    }

    @Test
    public void testSendUpdateSendsCorrectUDP() throws Exception {
        MockUDPSocket mockSocket = new MockUDPSocket();
        TestServerHandler handler = new TestServerHandler("127.0.0.1", 12345, "testuser");
        handler.setUpdateSocket(mockSocket);
        handler.setConnected(true);
        handler.sendUpdate("player_position:test:123:1:2");
        assertEquals(1, mockSocket.sentPackets.size());
        DatagramPacket sent = mockSocket.sentPackets.get(0);
        String sentMsg = new String(sent.getData(), 0, sent.getLength());
        assertEquals("player_position:test:123:1:2", sentMsg);
    }

    @Test
    public void testGetMessageReturnsFromBuffer() {
        TestServerHandler handler = new TestServerHandler("localhost", 12345, "testuser");
        handler.messageBuffer.add("testMessage");
        assertEquals("testMessage", handler.getMessage());
    }

    @Test
    public void testGetLastUpdateClearsAndReturns() {
        TestServerHandler handler = new TestServerHandler("localhost", 12345, "testuser");
        handler.setLastUpdate("update1");
        assertEquals("update1", handler.getLastUpdate());
        assertEquals("", handler.getLastUpdate());
    }

    @Test
    public void testReceiveMSGProcessesPingAndConfirm() throws Exception {
        String input = "ping\nconfirm:Username gesetzt zu testuser\nnormalmsg\n";
        BufferedReader reader = new BufferedReader(new StringReader(input));
        TestServerHandler handler = new TestServerHandler("localhost", 12345, "testuser");
        PrintWriter writer = new PrintWriter(new StringWriter());
        handler.setMsgReader(reader);
        handler.setMsgWriter(writer);
        handler.setConnected(true);
        Thread t = new Thread(() -> handler.receiveMSG());
        t.start();
        t.join(200);
        assertTrue(handler.messageBuffer.stream().anyMatch(s -> s.contains("confirm:")));
        assertTrue(handler.messageBuffer.stream().anyMatch(s -> s.contains("normalmsg")));
    }

    @Test
    public void testSendUdpRegistrationPacketSendsCorrectData() throws Exception {
        MockUDPSocket mockSocket = new MockUDPSocket();
        TestServerHandler handler = new TestServerHandler("127.0.0.1", 12345, "testuser");
        handler.setUpdateSocket(mockSocket);
        handler.setConfirmedNickname("testuser");
        java.lang.reflect.Method method = ServerHandler.class.getDeclaredMethod("sendUdpRegistrationPacket");
        method.setAccessible(true);
        method.invoke(handler);
        assertTrue(mockSocket.sentPackets.stream().anyMatch(packet -> {
            String msg = new String(packet.getData(), 0, packet.getLength());
            return msg.startsWith("register_udp:testuser:");
        }));
    }
}
