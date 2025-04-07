package igoat.server;

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
    private int maxTerminals;

    // if goats lose this is true
    private boolean gameOver = false;
    private boolean doorOpen = false;

    public GameState(int maxTerminals) {
        this.maxTerminals = maxTerminals;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public boolean isDoorOpen() {
        return doorOpen;
    }

    /**
     * should be called when a player is caught
     */
    public void caught() {
        caughtPlayers++;
        if (caughtPlayers == 3) {
            gameOver = true;
        }
    }

    /**
     * should be called when a player is revived
     */
    public void revive() {
        if (caughtPlayers > 0) {
            caughtPlayers--;
        }
        else {
            logger.warn("Revive was triggered but no caught players were found");
        }
    }

    /**
     * should be called when a terminal is activated
     */
    public void activateTerminal() {
        activatedTerminals++;
        if (activatedTerminals == maxTerminals) {
            doorOpen = true;
        }
    }
}
