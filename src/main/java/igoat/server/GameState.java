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
    private boolean guardWin = false;
    public boolean gameOver = false;
    private boolean doorOpen = false;

    private double playerX = 0;
    private double playerY = 0;

    public GameState(int maxTerminals) {
        this.maxTerminals = maxTerminals;
    }

    public boolean isGuardWin() {
        return guardWin;
    }

    public boolean isDoorOpen() {
        return doorOpen;
    }

    public void setX(double playerX) {
        this.playerX = playerX;
    }

    public void setY(double playerY) {
        this.playerY = playerY;
    }

    public double getPlayerX() {
        return playerX;
    }

    public double getPlayerY() {
        return playerY;
    }

    /**
     * should be called when a player is caught
     */
    public void caught() {
        caughtPlayers++;
        logger.info("caught player");
        if (caughtPlayers == 3) {
            logger.info("guard wins");
            guardWin = true;
        }
    }

    /**
     * should be called when a player is revived
     */
    public void revive() {
        if (caughtPlayers > 0) {
            caughtPlayers--;
            logger.info("revived player");
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
