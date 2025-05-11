package igoat.server;

import igoat.Role;
import igoat.Timer;
import igoat.client.Map;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a game lobby that manages players and game state. A lobby can contain up to
 * MAX_PLAYERS players and transitions through different states (OPEN, FULL, READY, IN_GAME) based
 * on player count and game progression.
 */
public class Lobby {

    public static final long GAME_OVER_TIME = 1000 * 300; // 5 minutes
    private static final Logger logger = LoggerFactory.getLogger(Lobby.class);
    // roles
    private static final List<Role> availableRoles = new CopyOnWriteArrayList<>();
    private static final Role[] INITIAL_ROLES = {Role.GUARD, Role.IGOAT, Role.IGOAT,
        Role.GOAT}; // 1 goat, 2 igoat, 1 guard
    public static final java.util.Map<String, Role> roleMap = new ConcurrentHashMap<>();

    private final int code;
    private final List<ClientHandler> members = new ArrayList<>();
    private final List<Player> playerList = new ArrayList<>();
    /**
     * Maximum number of players allowed in a lobby
     */
    public static int MAX_PLAYERS = 4;
    private final Map map = new Map(true);
    private final Timer timer = new Timer();
    private final Cooldown stationCooldown = new Cooldown(10000);
    private final List<ClientHandler> spectators = new ArrayList<>();

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
         * Game is currently in progress
         */
        IN_GAME,
        /**
         * Game finished
         */
        FINISHED
    }

    private LobbyState state = LobbyState.OPEN;
    private boolean cheatLocked = false;

    public boolean isCheatLocked() {
        return cheatLocked;
    }

    public void lockCheats() {
        this.cheatLocked = true;
    }

    private GameState gameState;

    /**
     * Assigns roles to all players in the lobby
     */
    public void setRoles() {
        for (ClientHandler client : members) {
            Role assignedRole = assignRole();
            client.getPlayer().setRole(assignedRole);
            roleMap.put(client.getNickname(), assignedRole);
            setSpawnPoints(client);
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
     *
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
    private void setSpawnPoints(ClientHandler client) {
        int x;
        int y;

        switch (client.getPlayer().getRole()) {
            case GOAT, IGOAT:
                x = 700;
                y = 1450;
                break;
            case GUARD:
                x = 800;
                y = 50;
                break;
            default:
                logger.warn("Unknown role for player: {}", client.getNickname());
                return;
        }

        client.getPlayer().teleport(x, y);
    }

    /**
     * Generates an array of 3 unique random terminal IDs between 0 and 7.
     *
     * @return An array containing 3 different terminal IDs
     */
    private int[] generateRandomTerminalIDs() {
        List<Integer> allTerminalIDs = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            allTerminalIDs.add(i);
        }

        Collections.shuffle(allTerminalIDs, new Random());
        return new int[]{allTerminalIDs.get(0), allTerminalIDs.get(1), allTerminalIDs.get(2)};
    }

    /**
     * Gets the current state of the lobby.
     *
     * @return The current GameState of the lobby
     */
    public LobbyState getState() {
        return state;
    }

    public Map getMap() {
        return map;
    }

    public Timer getTimer() {
        return timer;
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
     * Method for the timer thread. This constantly updates the game timer and ends the game if the
     * maximal game length was reached.
     */
    private void startTimerThread() {
        while (state != LobbyState.FINISHED) {
            timer.update();
            broadcastToAll("timer:" + code + ":" + timer.getTime());
            if (timer.getTime() >= GAME_OVER_TIME) {
                members.getFirst().endGame(true);
                logger.info("Time limit reached - game over");
                break;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
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
     * Gets the list of player names that were in the lobby when the game was started
     *
     * @return List of player names in the game
     */
    public List<Player> getPlayerList() {
        return playerList;
    }

    public Player getPlayer(String name) {
        for (Player player : playerList) {
            if (name.equals(player.getNickname())) {
                return player;
            }
        }
        return null;
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

        if (members.size() >= MAX_PLAYERS) {
            state = LobbyState.FULL;
        } else {
            state = LobbyState.OPEN;
        }
    }

    /**
     * Resets the lobby state. This should only be called after a game has ended.
     */
    public void resetState() {
        state = LobbyState.OPEN;
        updateState();
    }

    public void startGame() {
        // new gamestate
        cheatLocked = false;
        gameState = new GameState(map.getTerminalList().size(), generateRandomTerminalIDs(),
            members);
        for (ClientHandler client : members) {
            Player player = new Player(200, 80, client.getNickname(), this);
            playerList.add(player);
            client.setPlayer(player);
        }

        state = LobbyState.IN_GAME;
        timer.reset();
        Thread timerThread = new Thread(this::startTimerThread);
        timerThread.setDaemon(true);
        timerThread.start();
    }

    public void endGame() {
        playerList.clear();
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
     * Broadcasts a TCP message to all players and spectators in the lobby. Used for reliable
     * communication like chat messages and game state updates.
     *
     * @param message The message to broadcast to all lobby members and spectators
     */
    public void broadcastToAll(String message) {
        for (ClientHandler member : members) {
            member.sendMessage(message);
        }
        for (ClientHandler spectator : spectators) {
            spectator.sendMessage(message);
        }
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
        for (ClientHandler spectator : spectators) {
            spectator.sendMessage(message);
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
        for (ClientHandler spectator : spectators) {
            if (excludeMember != null && spectator == excludeMember) {
                continue;
            }
            spectator.sendUpdate(message);
        }
    }

    public Cooldown getStationCooldown() {
        return stationCooldown;
    }

    public void addSpectator(ClientHandler client) {
        spectators.add(client);
    }

    public void removeSpectator(ClientHandler client) {
        spectators.remove(client);
    }

    public List<ClientHandler> getSpectators() {
        return spectators;
    }
}