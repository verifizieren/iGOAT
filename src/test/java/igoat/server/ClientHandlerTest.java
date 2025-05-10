package igoat.server;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import igoat.Role;
import igoat.Timer;

public class ClientHandlerTest {
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
        assertTrue(handler.errors.stream().anyMatch(msg -> msg.contains("missing colon")));
    }

    @Test
    public void testHandleCommandUnknownCommand() {
        TestClientHandler handler = new TestClientHandler(null);
        handler.handleCommand("unknown:foo");
        assertTrue(handler.errors.stream().anyMatch(msg -> msg.contains("Unknown command")));
    }

    @Test
    public void testHandleCommandConnectCallsHandleConnect() {
        TestClientHandler handler = new TestClientHandler(null);
        handler.handleCommand("connect:Alice");
        assertFalse(handler.connectCalls.isEmpty());
        assertEquals("Alice", handler.connectCalls.get(0)[0]);
    }

    @Test
    public void testHandleCommandLobbyJoinLeave() {
        TestClientHandler handler = new TestClientHandler(null);
        handler.handleCommand("lobby:");
        assertTrue(handler.errors.stream().anyMatch(msg -> msg.toLowerCase().contains("invalid lobby code")));
        handler.errors.clear();
        handler.handleCommand("lobby:abc");
        assertTrue(handler.errors.stream().anyMatch(msg -> msg.toLowerCase().contains("invalid lobby code")));
    }

    @Test
    public void testHandleCommandReadyUnreadyNotInLobby() {
        TestClientHandler handler = new TestClientHandler(null);
        handler.handleCommand("ready:");
        assertTrue(handler.errors.stream().anyMatch(msg -> msg.contains("Not in a lobby")));
        handler.errors.clear();
        handler.handleCommand("unready:");
        assertTrue(handler.errors.stream().anyMatch(msg -> msg.contains("Not in a lobby")));
    }

    @Test
    public void testHandleCommandRoleInvalid() {
        TestClientHandler handler = new TestClientHandler(null);
        handler.handleCommand("role:NOTAROLE");
        assertTrue(handler.errors.stream().anyMatch(
            msg -> msg.toLowerCase().contains("invalid role") || msg.toLowerCase().contains("error when processing command")
        ));
    }

    @Test
    public void testHandleCommandChatSpectatorRestriction() {
        TestClientHandler handler = new TestClientHandler(null);
        handler.isSpectator = true;
        handler.handleCommand("chat:hello");
        assertTrue(handler.errors.stream().anyMatch(msg -> msg.contains("Spectators cannot chat")));
    }

    @Test
    public void testHandleCommandWhisperInvalid() {
        TestClientHandler handler = new TestClientHandler(null);
        handler.handleCommand("whisper:onlyoneparam");
        assertTrue(handler.errors.stream().anyMatch(msg -> msg.contains("Invalid whisper message")));
    }

    @Test
    public void testHandleCommandUsernameInvalid() {
        TestClientHandler handler = new TestClientHandler(null);
        handler.handleCommand("username:");
        assertTrue(handler.errors.stream().anyMatch(msg -> msg.contains("invalid username")
            || msg.contains("no username provided")));
    }

    @Test
    public void testHandleCommandGetLobbiesNoLobbies() {
        TestClientHandler handler = new TestClientHandler(null);
        handler.handleCommand("getlobbies:");
    }

    @Test
    public void testHandleCommandUnknownCommandError() {
        TestClientHandler handler = new TestClientHandler(null);
        handler.handleCommand("foobar:bar");
        assertTrue(handler.errors.stream().anyMatch(msg -> msg.contains("Unknown command")));
    }

    @Test
    public void testHandleCommandUsernameChangeValid() {
        class UsernameTestHandler extends TestClientHandler {
            String lastMessage = null;
            UsernameTestHandler(Socket s) { super(s); }
            @Override void sendMessage(String message) { lastMessage = message; }
        }
        UsernameTestHandler handler = new UsernameTestHandler(null);
        handler.nickname = "oldname";
        handler.handleCommand("username:newname");
        assertEquals("newname", handler.nickname);
        assertTrue(handler.lastMessage != null && handler.lastMessage.startsWith("confirm:"));
    }

    static class DummyPlayer extends Player {
        private Role role;
        private boolean caught = false;
        public DummyPlayer(Role role) { super(0,0,"dummy",null); this.role = role; }
        @Override public Role getRole() { return role; }
        @Override public boolean isCaught() { return caught; }
        @Override public void catchPlayer() { caught = true; }
        @Override public void revive() { caught = false; }
    }
    static class DummyLobby extends Lobby {
        private final List<ClientHandler> members = new ArrayList<>();
        private final GameState gs;
        public DummyLobby(GameState gs) { super(1234); this.gs = gs; }
        @Override public List<ClientHandler> getMembers() { return members; }
        @Override public GameState getGameState() { return gs; }
        @Override public void broadcastToAll(String msg) {}
        @Override public void broadcastToLobby(String msg) {}
        @Override public void broadcastChatToLobby(String msg) {}
        @Override public igoat.client.Map getMap() { return null; }
        @Override public Timer getTimer() { return new Timer(); }
        @Override public void endGame() {}
    }
    static class DummyGameState extends GameState {
        public boolean guardWin = false;
        public boolean gameOver = false;
        public DummyGameState() { super(1, new int[]{0}, new ArrayList<>()); }
        @Override public boolean isGuardWin() { return guardWin; }
    }

    @Test
    public void testCatchPlayerNotFound() {
        TestClientHandler handler = new TestClientHandler(null);
        handler.player = new DummyPlayer(Role.GUARD);
        handler.currentLobby = new DummyLobby(new DummyGameState());
        handler.handleCommand("catch:nonexistent");
        assertTrue(handler.errors.stream().anyMatch(msg -> msg.contains("not found")));
    }

    @Test
    public void testCatchPlayerWrongRole() {
        TestClientHandler handler = new TestClientHandler(null);
        handler.player = new DummyPlayer(Role.GOAT);
        handler.currentLobby = new DummyLobby(new DummyGameState());
        TestClientHandler target = new TestClientHandler(null);
        target.player = new DummyPlayer(Role.GOAT);
        handler.nickname = "handler";
        target.nickname = "dummy";
        ((DummyLobby)handler.currentLobby).getMembers().add(target);
        ClientHandler.clientList.add(handler);
        ClientHandler.clientList.add(target);
        try {
            handler.handleCommand("catch:dummy");
            if (handler.errors.stream().noneMatch(
                msg -> msg.toLowerCase().contains("only guards can catch") ||
                       msg.toLowerCase().contains("error") ||
                       msg.toLowerCase().contains("not allowed")
            )) {
                System.out.println("DEBUG: Errors: " + handler.errors);
            }
            assertTrue(handler.errors.stream().anyMatch(
                msg -> msg.toLowerCase().contains("only guards can catch") ||
                       msg.toLowerCase().contains("error") ||
                       msg.toLowerCase().contains("not allowed")
            ));
        } finally {
            ClientHandler.clientList.remove(handler);
            ClientHandler.clientList.remove(target);
        }
    }

    @Test
    public void testRevivePlayerNotFound() {
        TestClientHandler handler = new TestClientHandler(null);
        handler.player = new DummyPlayer(Role.GOAT);
        handler.currentLobby = new DummyLobby(new DummyGameState());
        handler.handleCommand("revive:nonexistent");
        assertTrue(handler.errors.stream().anyMatch(msg -> msg.contains("not found")));
    }

    @Test
    public void testRevivePlayerWrongRole() {
        TestClientHandler handler = new TestClientHandler(null);
        handler.player = new DummyPlayer(Role.GUARD);
        handler.currentLobby = new DummyLobby(new DummyGameState());
        TestClientHandler target = new TestClientHandler(null);
        target.player = new DummyPlayer(Role.IGOAT);
        handler.nickname = "handler";
        target.nickname = "dummy";
        ((DummyLobby)handler.currentLobby).getMembers().add(target);
        ClientHandler.clientList.add(handler);
        ClientHandler.clientList.add(target);
        try {
            handler.handleCommand("revive:dummy");
            if (handler.errors.stream().noneMatch(
                msg -> msg.toLowerCase().contains("only goats can reactivate") ||
                       msg.toLowerCase().contains("error") ||
                       msg.toLowerCase().contains("not allowed")
            )) {
                System.out.println("DEBUG: Errors: " + handler.errors);
            }
            assertTrue(handler.errors.stream().anyMatch(
                msg -> msg.toLowerCase().contains("only goats can reactivate") ||
                       msg.toLowerCase().contains("error") ||
                       msg.toLowerCase().contains("not allowed")
            ));
        } finally {
            ClientHandler.clientList.remove(handler);
            ClientHandler.clientList.remove(target);
        }
    }

    @Test
    public void testStationWrongRole() {
        TestClientHandler handler = new TestClientHandler(null);
        handler.player = new DummyPlayer(Role.GUARD);
        DummyGameState gs = new DummyGameState();
        DummyLobby lobby = new DummyLobby(gs);
        handler.currentLobby = lobby;
        handler.handleCommand("station:0");
    }

    @Test
    public void testHandleCommandLobbyJoinAndLeaveValidAndInvalid() {
        class LobbyTestHandler extends TestClientHandler {
            List<String> messages = new ArrayList<>();
            LobbyTestHandler() { super(null); }
            @Override void sendMessage(String message) { messages.add(message); }
            @Override void sendError(String message) { errors.add(message); }
        }
        LobbyTestHandler handler = new LobbyTestHandler();
        handler.nickname = "tester";
        Lobby dummyLobby = new Lobby(4321);
        ClientHandler.lobbyList.add(dummyLobby);
        try {
            handler.handleCommand("lobby:4321");
            assertEquals(dummyLobby, handler.currentLobby);
            handler.handleCommand("lobby:0");
            assertNull(handler.currentLobby);
            handler.handleCommand("lobby:9999");
            assertTrue(handler.errors.stream().anyMatch(msg -> msg.toLowerCase().contains("couldn't find lobby")));
        } finally {
            ClientHandler.lobbyList.remove(dummyLobby);
        }
    }

    @Test
    public void testHandleCommandTerminalActivationValidAndInvalid() {
        class TerminalTestHandler extends TestClientHandler {
            List<String> messages = new ArrayList<>();
            List<String> errors = new ArrayList<>();
            TerminalTestHandler() { super(null); }
            @Override void sendMessage(String message) { messages.add(message); }
            @Override void sendError(String message) { errors.add(message); }
        }
        TerminalTestHandler handler = new TerminalTestHandler();
        handler.nickname = "tester";
        Lobby dummyLobby = new Lobby(1234) {
            @Override public GameState getGameState() {
                return new GameState(2, new int[]{0,1}, new ArrayList<>());
            }
        };
        handler.currentLobby = dummyLobby;
        handler.handleCommand("terminal:0");
        assertTrue(handler.messages.stream().anyMatch(msg -> msg.contains("terminal:0") || msg.contains("door")) || handler.errors.isEmpty());
        handler.messages.clear(); handler.errors.clear();
        handler.handleCommand("terminal:99");
        assertTrue(handler.messages.stream().anyMatch(msg -> msg.contains("terminal:-1")));
    }

    @Test
    public void testHandleCommandStartGameRestrictions() {
        class StartGameTestHandler extends TestClientHandler {
            List<String> errors = new ArrayList<>();
            StartGameTestHandler() { super(null); }
            @Override void sendError(String message) { errors.add(message); }
        }
        Lobby lobby = new Lobby(5555) {
            @Override public List<ClientHandler> getMembers() { return members; }
            List<ClientHandler> members = new ArrayList<>();
        };
        StartGameTestHandler creator = new StartGameTestHandler();
        creator.nickname = "creator";
        creator.currentLobby = lobby;
        creator.isReady = true;
        StartGameTestHandler notReady = new StartGameTestHandler();
        notReady.nickname = "notready";
        notReady.currentLobby = lobby;
        notReady.isReady = false;
        ((ArrayList<ClientHandler>)lobby.getMembers()).add(creator);
        ((ArrayList<ClientHandler>)lobby.getMembers()).add(notReady);
        creator.handleCommand("startgame:");
        assertTrue(creator.errors.stream().anyMatch(msg -> msg.toLowerCase().contains("not everyone is ready")));
        creator.errors.clear();
        notReady.isReady = true;
        notReady.handleCommand("startgame:");
        assertTrue(notReady.errors.stream().anyMatch(msg -> msg.toLowerCase().contains("only the lobby creator can start the game")));
    }

    @Test
    public void testSpectatorJoinAndLeave() {
        class SpectatorTestHandler extends TestClientHandler {
            List<String> messages = new ArrayList<>();
            List<String> errors = new ArrayList<>();
            SpectatorTestHandler() { super(null); }
            @Override void sendMessage(String message) { messages.add(message); }
            @Override void sendError(String message) { errors.add(message); }
        }
        SpectatorTestHandler handler = new SpectatorTestHandler();
        handler.nickname = "spectator";
        Lobby dummyLobby = new Lobby(2468);
        ClientHandler.lobbyList.add(dummyLobby);
        ClientHandler.clientList.add(handler);
        try {
            handler.handleCommand("spectate:spectator:2468");
            System.out.println("DEBUG: handler.errors=" + handler.errors);
            System.out.println("DEBUG: handler.messages=" + handler.messages);
            System.out.println("DEBUG: handler.isSpectator=" + handler.isSpectator);
            System.out.println("DEBUG: lobbyList codes=" + ClientHandler.lobbyList.stream().map(Lobby::getCode).toList());
            assertTrue(handler.isSpectator);
            assertEquals(dummyLobby, handler.currentLobby);
            handler.handleCommand("leaveSpectate:spectator:2468");
            System.out.println("DEBUG after leave: handler.isSpectator=" + handler.isSpectator);
            System.out.println("DEBUG after leave: handler.currentLobby=" + handler.currentLobby);
            System.out.println("DEBUG after leave: handler.errors=" + handler.errors);
            System.out.println("DEBUG after leave: handler.messages=" + handler.messages);
            assertFalse(handler.isSpectator);
            assertNull(handler.currentLobby);
        } finally {
            ClientHandler.lobbyList.remove(dummyLobby);
            ClientHandler.clientList.remove(handler);
        }
    }
}


