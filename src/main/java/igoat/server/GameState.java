package igoat.server;

import igoat.Role;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Contains the game state
 */
public class GameState {

    private static final Logger logger = LoggerFactory.getLogger(GameState.class);

    private final boolean[] terminals;
    private boolean[] stations = new boolean[]{false, false};
    private final int[] ids;
    private final List<ClientHandler> players;
    private final List<String> eventLog;
    private final boolean doorsOpen = false;

    public boolean gameOver = false;

    public GameState(int maxTerminals, int[] ids, List<ClientHandler> players) {
        this.players = players;
        terminals = new boolean[maxTerminals];
        for (int i = 0; i < terminals.length; i++) {
            terminals[i] = false;
        }

        eventLog = new ArrayList<>();

        this.ids = ids;

        if (ids.length > maxTerminals) {
            logger.error("invalid id length");
        }
    }

    /**
     * Checks if all (i)goats are caught
     *
     * @return True if all are caught, false otherwise
     */
    public boolean isGuardWin() {
        // might remove later: solo guard != instawin
        if (players.size() < 2) {
            return false;
        }

        for (ClientHandler client : players) {
            if (client.getPlayer().getRole() == Role.GUARD) {
                continue;
            }

            if (!client.getPlayer().isCaught()) {
                logger.info("found uncaught player {}", client.getNickname());
                return false;
            }
        }

        return true;
    }

    /**
     * Checks if the conditions are met for the doors to open
     *
     * @return True if the correct terminals are active, false otherwise
     */
    public boolean isDoorOpen() {
        if (doorsOpen) {
            return true;
        }

        for (int i : ids) {
            if (!terminals[i]) {
                return false;
            }
        }
        if (!eventLog.contains("door")) {
            eventLog.add("door");
        }

        return true;
    }

    /**
     * Manually activate terminals for doors top open. For debugging / cheat code purposes
     */
    public void openDoors() {
        if (!eventLog.contains("door")) {
            eventLog.add("door");
        }
        for (int id : ids) {
            terminals[id] = true;
        }
    }

    /**
     * Sets an igoat station to active
     *
     * @param id The id of the station
     * @return true if successful, false otherwise
     */
    public boolean activateStation(int id) {
        if (id >= stations.length || id < 0 || stations[id]) {
            return false;
        }

        stations[id] = true;
        return true;
    }

    /**
     * Return the status for the stations
     */
    public boolean[] getStationStatus() {
        return stations;
    }

    /**
     * Should be called when a terminal is activated. Depending on the terminal and player state, it
     * will return the resulting terminal state
     *
     * @return true if terminal was activated, false if not
     */
    public boolean activateTerminal(int id) {
        if (id >= terminals.length || id < 0) {
            logger.error("Invalid id {}", id);
            return false;
        }

        // check if terminal is active
        if (terminals[id]) {
            return false;
        }

        // check if terminal opens doors
        for (int val : ids) {
            if (val == id) {
                terminals[id] = true;
                eventLog.add("terminal:" + id);
                return true;
            }
        }

        // check if GOAT is caught
        for (ClientHandler client : players) {
            if (client.getPlayer().getRole() == Role.GOAT && client.getPlayer().isCaught()) {
                terminals[id] = true;
                eventLog.add("terminal:" + id);
                return true;
            }
        }

        return false;
    }

    /**
     * Gets the list of all events that happened until now
     *
     * @return List containing the protocol commands for each event
     */
    public List<String> getEventLog() {
        return eventLog;
    }

    public boolean[] getStations() {
        return stations;
    }

    public void setStations(boolean[] stations) {
        this.stations = stations;
    }
}
