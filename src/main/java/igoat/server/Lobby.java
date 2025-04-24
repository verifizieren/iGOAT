package igoat.server;

import igoat.Role;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
    // roles
    private static final List<Role> availableRoles = new CopyOnWriteArrayList<>();
    private static final Role[] INITIAL_ROLES = {Role.GUARD, Role.IGOAT, Role.IGOAT,
        Role.GOAT}; // 1 goat, 2 igoat, 1 guard
    public static final java.util.Map<String, Role> roleMap = new ConcurrentHashMap<>();

    private final int code;
    private final List<ClientHandler> members = new ArrayList<>();
    /**
     * Maximum number of players allowed in a lobby
     */
    public static int MAX_PLAYERS = 4;
    private final CollisionMap map = new CollisionMap();
    private long gameTime = 0;
    private Timer timer;

    /**
     * Represents the different states a lobby can be in: - OPEN: Lobby is accepting new players -
     * FULL: Lobby has reached maximum player capacity - READY: All players are ready to start -
     * IN_GAME: Game is currently in progress
     */
    public enum LobbyState {
        /**
         * Lobby is accepting new players
         */
        OPEN,
        /**
         * Lobby has reached maximum player capacity
         */
        FULL,
        /**
         * All players in the lobby are ready to start
         */
        READY,
        /**
         * Game is currently in progress
         */
        IN_GAME,
        /**
         * Game finished
         */
        FINISHED
    }

    private LobbyState state = LobbyState.OPEN;
    private GameState gameState = new GameState(map.getTerminalList().size(), new int[]{1, 2, 3},
        members);

    /**
     * Assigns roles to all players in the lobby
     */
    public void setRoles() {
        for (ClientHandler player : members) {
            Role assignedRole = assignRole();
            player.setRole(assignedRole);
            roleMap.put(player.getNickname(), assignedRole);
            setSpawnPoints(player);
        }

        synchronized (availableRoles) {
            availableRoles.clear();
            availableRoles.addAll(
                Arrays.asList(INITIAL_ROLES[0], INITIAL_ROLES[1], INITIAL_ROLES[2],
                    INITIAL_ROLES[3]));
        }
    }

    /**
     * This method ensures that roles are correctly distributed
     * @return Role for a player
     */
    private Role assignRole() {
        synchronized (availableRoles) {
            if (availableRoles.isEmpty()) {
                // Reset the roles list if empty
                availableRoles.addAll(
                    Arrays.asList(INITIAL_ROLES[0], INITIAL_ROLES[1], INITIAL_ROLES[2],
                        INITIAL_ROLES[3]));
            }

            int randomIndex = (int) (Math.random() * availableRoles.size());
            return availableRoles.remove(randomIndex);
        }
    }

    /**
     * Sets the serverside positions of the players to the correct spawn locations
     */
    private void setSpawnPoints(ClientHandler player) {
        if (player.isPositionWasSet()) {
            logger.info("Spawnpoint für {} wurde bereits gesetzt, wird nicht erneut überschrieben.", player.getNickname());
            return;
        }

        int x;
        int y;

        switch (player.getRole()) {
            case GOAT, IGOAT:
                x = 700;
                y = 1450;
                break;
            case GUARD:
                x = 800;
                y = 50;
                break;
            default:
                logger.warn("Unknown role for player: {}", player.getNickname());
                return;
        }

        player.setPlayerX(x);
        player.setPlayerY(y);
    }

    /**
     * Gets the current state of the lobby.
     *
     * @return The current GameState of the lobby
     */
    public LobbyState getState() {
        return state;
    }

    /**
     * Sets the lobby status to finished
     */
    public void setGameFinished() {
        state = LobbyState.FINISHED;
    }

    public CollisionMap getMap() {
        return map;
    }

    /**
     * Creates a new lobby with the specified code. The lobby starts in OPEN state and uses a
     * thread-safe list for members.
     *
     * @param code The unique identifier for this lobby
     */
    public Lobby(int code) {
        this.code = code;
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
     * Gets the list of players currently in the lobby. The list is thread-safe and can be modified
     * while being iterated.
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
     * Updates the lobby's state based on the current number of players. Sets the state to FULL if
     * maximum capacity is reached, otherwise OPEN.
     */
    private void updateState() {
        if (state == LobbyState.IN_GAME || state == LobbyState.FINISHED) {
            return;
        }

        if (members.size() >= MAX_PLAYERS && state != LobbyState.IN_GAME) {
            state = LobbyState.FULL;
        } else {
            state = LobbyState.OPEN;
        }
    }

    public void startGame() {
        state = LobbyState.IN_GAME;
        timer = new Timer();
    }

    /**
     * updates the game timer
     */
    public void updateTimer() {
        if (state == LobbyState.IN_GAME && timer != null) {
            gameTime += timer.getTimeElapsed();
        }
    }

    /**
     * updates the game time and returns it
     * @return game time
     */
    public long getGameTime() {
        updateTimer();
        return gameTime;
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
     * Broadcasts a TCP message to all players in the lobby. Used for reliable communication like
     * chat messages and game state updates.
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
     * @param message       The message to broadcast.
     * @param excludeMember A member to exclude from the broadcast (usually the sender), can be
     *                      null.
     */
    public void broadcastUpdateToLobby(String message, ClientHandler excludeMember) {
        for (ClientHandler member : members) {
            if (excludeMember != null && member == excludeMember) {
                continue;
            }

            member.sendUpdate(message);
        }
    }
}