package igoat.server;

import igoat.Role;
import igoat.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The serverside Player class stores all information related to the in-game player.
 */
public class Player {
    private static final Logger logger = LoggerFactory.getLogger(Player.class);

    private double x;
    private double y;
    private Role role;
    private final String nickname;

    private boolean isCaught = false;
    private boolean positionWasSet = false;

    private final Timer spawnProtection = new Timer();
    private final Lobby lobby;

    public Player(double x, double y, String nickname, Lobby lobby) {
        this.x = x;
        this.y = y;
        this.nickname = nickname;
        this.lobby = lobby;
    }

    /**
     * Moves the player to the specified location
     * @param x target x-coordinate
     * @param y target y-coordinate
     */
    public void teleport(double x, double y) {
        this.x = x;
        this.y = y;
        positionWasSet = false;
        lobby.broadcastUpdateToLobby("player_position:" + nickname + ":" + (int)x + ":" + (int)y, null);
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public Role getRole() {
        return role;
    }

    public boolean isCaught() {
        return isCaught;
    }

    public boolean getPositionWasSet() {
        return positionWasSet;
    }

    public Timer getSpawnProtection() {
        return spawnProtection;
    }

    public void setX(double x) {
        this.x = x;
    }

    public void setY(double y) {
        this.y = y;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public void catchPlayer() {
        isCaught = true;
    }

    public void revive() {
        isCaught = false;
    }

    public void setPositionWasSet(boolean positionWasSet) {
        this.positionWasSet = positionWasSet;
    }
}
