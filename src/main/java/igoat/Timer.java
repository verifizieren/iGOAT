package igoat;

/**
 * A class for tracking time
 */
public class Timer {

    private long sysTime;
    private long elapsedTime;
    private long time;

    /**
     * Starts/reset the timer
     */
    public void reset() {
        sysTime = getSystemTime();
        elapsedTime = 0;
        time = 0;
    }

    /**
     * Starts/resets the timer with a certain starting time
     *
     * @param time The starting time in milliseconds
     */
    public void reset(long time) {
        sysTime = getSystemTime();
        elapsedTime = 0;
        this.time = time;
    }

    /**
     * Updates the timer to the current system time. This should be called before using
     * getTimeElapsed or getTime
     */
    public void update() {
        long now = getSystemTime();
        elapsedTime = now - sysTime;
        sysTime = now;
        time += elapsedTime;
    }

    /**
     * Gets the time interval since the last time this function was called
     *
     * @return The elapsed time in ms
     */
    public long getTimeElapsed() {
        return elapsedTime;
    }

    public long getTime() {
        return time;
    }

    /**
     * Gets the current system time
     *
     * @return Current time in ms
     */
    public static long getSystemTime() {
        return System.currentTimeMillis();
    }

    /**
     * Converts the provided time into min:sec format. (Example: 90000 -> 1:30)
     *
     * @param time to be converted in milliseconds
     * @return index [0] contains minutes, [1] contains seconds.
     */
    public static double[] convertToMinSec(long time) {
        double[] minSec = new double[2];
        // round
        double totalSeconds = time / 1000.0;
        minSec[0] = Math.floor(totalSeconds / 60.0);
        minSec[1] = totalSeconds % 60;

        return minSec;
    }

    /**
     * Gets the time as a string
     *
     * @return current time (not system time) as a string
     */
    @Override
    public String toString() {
        double[] minSec = convertToMinSec(time);
        return String.format("%d:%02d", Math.round(minSec[0]), Math.round(minSec[1]));
    }
}
