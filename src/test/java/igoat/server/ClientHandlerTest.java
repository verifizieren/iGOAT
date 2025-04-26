package igoat.server;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ClientHandlerTest {
    @Test
    public void testPositionWasSetGetterSetter() {
        ClientHandler handler = new ClientHandler(null); // null socket for unit test
        handler.setPositionWasSet(true);
        boolean result = handler.positionWasSet();
        assertTrue(result);
        handler.setPositionWasSet(false);
        assertFalse(handler.positionWasSet());
    }

    static class TestClientHandler extends ClientHandler {
        List<String> errors = new ArrayList<>();
        List<String[]> connectCalls = new ArrayList<>();
        public TestClientHandler(Socket socket) {
            super(socket);
        }
        @Override
        void sendError(String message) {
            errors.add(message);
        }
        @Override
        void handleConnect(String[] params) {
            connectCalls.add(params);
        }
    }

    @Test
    public void testHandleCommandMalformedCommand() {
        TestClientHandler handler = new TestClientHandler(null);
        handler.handleCommand("malformedcommand");
        assertTrue(handler.errors.stream().anyMatch(msg -> msg.contains("fehlender Doppelpunkt")));
    }

    @Test
    public void testHandleCommandUnknownCommand() {
        TestClientHandler handler = new TestClientHandler(null);
        handler.handleCommand("unknown:foo");
        assertTrue(handler.errors.stream().anyMatch(msg -> msg.contains("Unbekannter Befehl")));
    }

    @Test
    public void testHandleCommandConnectCallsHandleConnect() {
        TestClientHandler handler = new TestClientHandler(null);
        handler.handleCommand("connect:Alice");
        assertFalse(handler.connectCalls.isEmpty());
        assertEquals("Alice", handler.connectCalls.get(0)[0]);
    }
}


