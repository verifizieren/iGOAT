package igoat.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Singleton class that contains all the sounds for the game
 */
public class SoundManager {
    private static final Logger logger = LoggerFactory.getLogger(SoundManager.class);

    private static final SoundManager instance = new SoundManager();
    private double volume = 0.5;

    private SoundManager() {
        setVolume(volume);
    }

    public static SoundManager getInstance() {
        return instance;
    }

    public final Sound click = new Sound("/sounds/click.wav");
    public final Sound goat = new Sound("/sounds/goat.wav");
    public final Sound terminal = new Sound("/sounds/terminal.wav");
    public final Sound doors = new Sound("/sounds/door.wav");
    public final Sound igoatCatch = new Sound("/sounds/igoat_caught.wav");

    /**
     * Stops all sound playback
     */
    public void stopAll() {
        click.stop();
        goat.stop();
        terminal.stop();
        doors.stop();
        igoatCatch.stop();
    }

    public double getVolume() {
        return volume;
    }

    public void setVolume(double volume) {
        this.volume = volume;
        click.setVolume(volume);
        goat.setVolume(volume);
        terminal.setVolume(volume);
        doors.setVolume(volume);
        igoatCatch.setVolume(volume);
    }
}
