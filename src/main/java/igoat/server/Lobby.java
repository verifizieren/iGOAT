package igoat.server;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Lobby {
    private final int code;
    private final List<ClientHandler> members;
    public static int MAX_PLAYERS = 4;

    public enum GameState {
        OPEN,
        FULL,
        READY,
        IN_GAME
    }

    private GameState state = GameState.OPEN;

    public GameState getState() {
        return state;
    }

    public void setState(GameState state) {
        this.state = state;
    }


    public Lobby(int code) {
        this.code = code;
        this.members = new CopyOnWriteArrayList<>();
    }

    public int getCode() {
        return code;
    }

    public List<ClientHandler> getMembers() {
        return members;
    }

    public boolean isFull(){
        return members.size() >= MAX_PLAYERS;
    }

    private void updateState() {
        if (members.size() >= MAX_PLAYERS) {
            state = GameState.FULL;
        } else {
            state = GameState.OPEN;
        }
    }

    public void addMember(ClientHandler client) {
        members.add(client);
        updateState();
    }

    public void removeMember(ClientHandler client) {
        members.remove(client);
        updateState();
    }

    public void broadcastToLobby(String message) {
        for (ClientHandler member : members) {
            member.sendMessage(message);
        }
    }

    /**
     * Broadcasts a UDP update message to all members of the lobby.
     * It calls the sendUpdate method on each ClientHandler.
     *
     * @param message The UDP message to broadcast.
     */
    public void broadcastUpdateToLobby(String message) {
        System.out.println("Broadcasting UDP update to lobby " + code + ": '" + message + "'");
        for (ClientHandler member : members) {
            member.sendUpdate(message);
        }
    }
}
