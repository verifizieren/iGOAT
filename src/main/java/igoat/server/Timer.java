package igoat.server;

/**
 * A class for tracking time
 */
public class Timer {
    private long lastTime;

    /**
     * Creates a new timer intialized with the current time
     */
    public Timer() {
        lastTime = System.nanoTime();
    }

    /**
     * Gets the time intervall since the last time this function was called
     * @return The elapsed time in ms
     */
    public long getTimeElapsed() {
        long elapsed = System.nanoTime() - lastTime;
        lastTime = System.nanoTime();
        return elapsed;
    }

    /**
     * Gets the current system time
     * @return Current time in ms
     */
    public static long getCurrentTime() {
        return System.nanoTime();
    }

    public static long[] convertToMinSec(long time) {
        long[] minSec = new long[2];
        long totalSeconds = time / 1000;
        minSec[0] = totalSeconds / 60;
        minSec[1] = totalSeconds % 60;

        return minSec;
    }
}
