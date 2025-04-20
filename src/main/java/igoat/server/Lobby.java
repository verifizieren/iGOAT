package igoat.server;

import igoat.client.Map;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
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
    private Map map = new Map();

    /**
     * Represents the different states a lobby can be in:
     * - OPEN: Lobby is accepting new players
     * - FULL: Lobby has reached maximum player capacity
     * - READY: All players are ready to start
     * - IN_GAME: Game is currently in progress
     */
    public enum LobbyState {
        /** Lobby is accepting new players */
        OPEN,
        /** Lobby has reached maximum player capacity */
        FULL,
        /** All players in the lobby are ready to start */
        READY,
        /** Game is currently in progress */
        IN_GAME,
        /** Game finished */
        FINISHED
    }

    private LobbyState state = LobbyState.OPEN;
    private GameState gameState = new GameState(8);
    private Set<Integer> activatedTerminals = ConcurrentHashMap.newKeySet();
    private int totalTerminalsInMap = 0;

    /**
     * Gets the current state of the lobby.
     * 
     * @return The current GameState of the lobby
     */
    public LobbyState getState() {
        return state;
    }

    /**
     * Sets the state of the lobby.
     * 
     * @param state The new GameState to set
     */
    public void setState(LobbyState state) {
        this.state = state;
    }

    public Map getMap() {
        return map;
    }
    /**
     * Sets the total number of terminals required for the map being played in this lobby.
     * Should be called when the game starts.
     * 
     * @param count The number of terminals on the map.
     */
    public void setTotalTerminalsInMap(int count) {
        this.totalTerminalsInMap = count;
        this.activatedTerminals.clear(); 
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

    public GameState getGameState() {
        return gameState;
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
            state = LobbyState.FULL;
        } else {
            state = LobbyState.OPEN;
        }
    }

    public void startGame() {
        state = LobbyState.IN_GAME;
    }

    public void endGame() {
        state = LobbyState.FINISHED;
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
    public void broadcastChatToLobby(String message) {
        for (ClientHandler member : members) {
            member.sendMessage("lobbychat:" + message);
        }
    }

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
       //logger.info("Broadcasting to lobby {}: {}", code, message);
        
        for (ClientHandler member : members) {
            if (excludeMember != null && member == excludeMember) {
                continue;
            }
            
            member.sendUpdate(message);
        }
    }

    /**
     * Activates a terminal within this lobby.
     * Checks if all terminals are activated after this activation.
     * 
     * @param terminalId The ID of the terminal being activated.
     * @return true if the terminal was newly activated, false if it was already active.
     */
    public boolean activateTerminal(int terminalId) {
        if (activatedTerminals.add(terminalId)) {
            logger.info("Lobby {}: Terminal {} activated.", code, terminalId);
            if (totalTerminalsInMap > 0 && activatedTerminals.size() >= totalTerminalsInMap) {
//                logger.info("Lobby {}: All {}/{} terminals activated! Triggering game event...", code, activatedTerminals.size(), totalTerminalsInMap);

//                broadcastChatToLobby("chat:System:All terminals have been activated!");

                broadcastToLobby("doors");
//                logger.info("Lobby {}: Sent doors_open command.", code);
            }
            return true;
        } else {
            logger.warn("Lobby {}: Terminal {} was already activated.", code, terminalId);
            return false;
        }
    }
}
