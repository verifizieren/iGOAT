package igoat.server;

/**
 * A class for tracking time
 */
public class Timer {
    private long lastTime;

    // Creates a new timer intialized with the current time
    public Timer() {
        lastTime = System.nanoTime();
    }

    public long getTimeElapsed() {
        long elapsed = System.nanoTime() - lastTime;
        lastTime = System.nanoTime();
        return elapsed;
    }
}
