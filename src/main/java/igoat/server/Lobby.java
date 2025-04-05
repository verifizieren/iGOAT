package igoat.server;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a game lobby that manages players and game state.
 * A lobby can contain up to MAX_PLAYERS players and transitions through different states
 * (OPEN, FULL, READY, IN_GAME) based on player count and game progression.
 */
public class Lobby {
    private static final Logger logger = LoggerFactory.getLogger(Lobby.class);

    private final int code;
    private final List<ClientHandler> members;
    /** Maximum number of players allowed in a lobby */
    public static int MAX_PLAYERS = 4;

    /**
     * Represents the different states a lobby can be in:
     * - OPEN: Lobby is accepting new players
     * - FULL: Lobby has reached maximum player capacity
     * - READY: All players are ready to start
     * - IN_GAME: Game is currently in progress
     */
    public enum GameState {
        /** Lobby is accepting new players */
        OPEN,
        /** Lobby has reached maximum player capacity */
        FULL,
        /** All players in the lobby are ready to start */
        READY,
        /** Game is currently in progress */
        IN_GAME
    }

    private GameState state = GameState.OPEN;

    /**
     * Gets the current state of the lobby.
     * 
     * @return The current GameState of the lobby
     */
    public GameState getState() {
        return state;
    }

    /**
     * Sets the state of the lobby.
     * 
     * @param state The new GameState to set
     */
    public void setState(GameState state) {
        this.state = state;
    }


    /**
     * Creates a new lobby with the specified code.
     * The lobby starts in OPEN state and uses a thread-safe list for members.
     * 
     * @param code The unique identifier for this lobby
     */
    public Lobby(int code) {
        this.code = code;
        this.members = new CopyOnWriteArrayList<>();
    }

    /**
     * Gets the lobby's unique code.
     * 
     * @return The lobby's code
     */
    public int getCode() {
        return code;
    }

    /**
     * Gets the list of players currently in the lobby.
     * The list is thread-safe and can be modified while being iterated.
     * 
     * @return List of ClientHandler objects representing connected players
     */
    public List<ClientHandler> getMembers() {
        return members;
    }

    /**
     * Checks if the lobby has reached its maximum player capacity.
     * 
     * @return true if the number of players equals or exceeds MAX_PLAYERS
     */
    public boolean isFull() {
        return members.size() >= MAX_PLAYERS;
    }

    /**
     * Updates the lobby's state based on the current number of players.
     * Sets the state to FULL if maximum capacity is reached, otherwise OPEN.
     */
    private void updateState() {
        if (members.size() >= MAX_PLAYERS) {
            state = GameState.FULL;
        } else {
            state = GameState.OPEN;
        }
    }

    /**
     * Adds a new player to the lobby and updates the lobby state.
     * 
     * @param client The ClientHandler representing the player to add
     */
    public void addMember(ClientHandler client) {
        members.add(client);
        updateState();
    }

    /**
     * Removes a player from the lobby and updates the lobby state.
     * 
     * @param client The ClientHandler representing the player to remove
     */
    public void removeMember(ClientHandler client) {
        members.remove(client);
        updateState();
    }

    /**
     * Broadcasts a TCP message to all players in the lobby.
     * Used for reliable communication like chat messages and game state updates.
     * 
     * @param message The message to broadcast to all lobby members
     */
    public void broadcastToLobby(String message) {
        for (ClientHandler member : members) {
            member.sendMessage(message);
        }
    }

    /**
     * Broadcasts a UDP update message to all clients in the lobby.
     * 
     * @param message The message to broadcast.
     * @param excludeMember A member to exclude from the broadcast (usually the sender), can be null.
     */
    public void broadcastUpdateToLobby(String message, ClientHandler excludeMember) {
       logger.info("Broadcasting to lobby {}: {}", code, message);
        
        for (ClientHandler member : members) {
            if (excludeMember != null && member == excludeMember) {
                continue;
            }
            
            member.sendUpdate(message);
        }
    }
}
