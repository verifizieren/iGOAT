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
        lastTime = getCurrentTime();
    }

    /**
     * Gets the time intervall since the last time this function was called
     * @return The elapsed time in ms
     */
    public long getTimeElapsed() {
        long elapsed = getCurrentTime() - lastTime;
        lastTime = getCurrentTime();
        return elapsed;
    }

    /**
     * Gets the current system time
     * @return Current time in ms
     */
    public static long getCurrentTime() {
        return System.currentTimeMillis();
    }

    public static double[] convertToMinSec(long time) {
        double[] minSec = new double[2];
        // round
        double totalSeconds = time / 1000.0;
        minSec[0] = Math.floor(totalSeconds / 60.0);
        minSec[1] = totalSeconds % 60;

        return minSec;
    }
}
