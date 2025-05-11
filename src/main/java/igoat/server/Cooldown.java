package igoat.server;

import igoat.Timer;

/**
 * A class for tracking cooldowns
 */
public class Cooldown {

    private final Timer timer = new Timer();
    private final long coolDownTime;

    /**
     * Constructor for Cooldown
     *
     * @param cooldownTime Cooldown time
     */
    public Cooldown(long cooldownTime) {
        this.coolDownTime = cooldownTime;
        timer.reset();
    }

    /**
     * Checks whether the cooldown is active.
     *
     * @return true if the cooldown is still active, false otherwise
     */
    public boolean check() {
        timer.update();
        return timer.getTime() < coolDownTime;
    }

    /**
     * Starts the cooldown
     */
    public void start() {
        timer.reset();
    }
}
