package igoat.server;

import igoat.Role;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Contains the game state
 */
public class GameState {
    private static final Logger logger = LoggerFactory.getLogger(GameState.class);

    private Lobby lobby;
    private int caughtPlayers = 0;
    private int activatedTerminals = 0;
    private boolean[] terminals;
    private int[] ids;
    private final List<ClientHandler> players;

    public boolean gameOver = false;

    public GameState(int maxTerminals, int[] ids, List<ClientHandler> players) {
        this.players = players;
        terminals = new boolean[maxTerminals];
        for (boolean terminal : terminals) {
            terminal = false;
        }

        this.ids = ids;

        if (ids.length > maxTerminals) {
            logger.error("invalid id length");
        }
    }

    /**
     * Checks if all (i)goats are caught
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
     * @return True if the correct terminals are active, false otherwise
     */
    public boolean isDoorOpen() {
        for (int i : ids) {
            if (!terminals[i]) {
                return false;
            }
        }

        return true;
    }


    /**
     * Should be called when a terminal is activated. Depending on the terminal and player state, it will return the resulting terminal state
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
                return true;
            }
        }

        // check if GOAT is caught
        for (ClientHandler client : players) {
            if (client.getPlayer().getRole() == Role.GOAT && client.getPlayer().isCaught()) {
                terminals[id] = true;
                return true;
            }
        }

        return false;
    }
}
