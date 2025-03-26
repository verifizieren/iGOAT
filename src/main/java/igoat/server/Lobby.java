package igoat.server;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Lobby {
    private final int code;
    private final List<ClientHandler> members;

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

    public void addMember(ClientHandler client) {
        members.add(client);
    }

    public void removeMember(ClientHandler client) {
        members.remove(client);
    }

    public void broadcastToLobby(String message) {
        for (ClientHandler member : members) {
            member.sendMessage(message);
        }
    }
}
