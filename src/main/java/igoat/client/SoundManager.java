package igoat.client;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Singleton class that contains all the sounds for the game
 */
public class SoundManager {
    private static final Logger logger = LoggerFactory.getLogger(SoundManager.class);

    private static final SoundManager instance = new SoundManager();
    private double volume = 0.2;
    private double soundtrackVolume = 0.1;
    private MediaPlayer soundtrackPlayer;
    private boolean isSoundtrackPlaying = false;

    private SoundManager() {
        setVolume(volume);
        initializeSoundtrack();
    }

    private void initializeSoundtrack() {
        try {
            String resourcePath = getClass().getResource("/sounds/soundtrack.wav").toString();
            Media media = new Media(resourcePath);
            soundtrackPlayer = new MediaPlayer(media);
            soundtrackPlayer.setCycleCount(MediaPlayer.INDEFINITE);
            soundtrackPlayer.setVolume(soundtrackVolume);
        } catch (Exception e) {
            logger.error("Error initializing soundtrack", e);
        }
    }

    public static SoundManager getInstance() {
        return instance;
    }

    public final Sound click = new Sound("/sounds/click.wav");
    public final Sound goat = new Sound("/sounds/goat.wav");
    public final Sound terminal = new Sound("/sounds/terminal.wav");
    public final Sound doors = new Sound("/sounds/door.wav");
    public final Sound igoatCatch = new Sound("/sounds/igoat_caught.wav");
    public final Sound denied = new Sound("/sounds/denied.wav");

    /**
     * Stops all sound playback including the soundtrack
     */
    public void stopAll() {
        click.stop();
        goat.stop();
        terminal.stop();
        doors.stop();
        igoatCatch.stop();
        denied.stop();
        stopSoundtrack();
    }

    public double getVolume() {
        return volume;
    }

    /**
     * Sets the volume for all sound effects
     */
    public void setVolume(double volume) {
        this.volume = volume;
        click.setVolume(volume);
        goat.setVolume(volume);
        terminal.setVolume(volume);
        doors.setVolume(volume);
        igoatCatch.setVolume(volume);
        denied.setVolume(volume);
    }

    /**
     * Gets the soundtrack volume
     */
    public double getSoundtrackVolume() {
        return soundtrackVolume;
    }

    /**
     * Sets the volume for the background soundtrack
     */
    public void setSoundtrackVolume(double volume) {
        this.soundtrackVolume = volume;
        if (soundtrackPlayer != null) {
            soundtrackPlayer.setVolume(volume);
        }
    }

    /**
     * Starts playing the soundtrack
     */
    public void playSoundtrack() {
        if (soundtrackPlayer != null && !isSoundtrackPlaying) {
            soundtrackPlayer.play();
            isSoundtrackPlaying = true;
        }
    }

    /**
     * Stops the soundtrack
     */
    public void stopSoundtrack() {
        if (soundtrackPlayer != null && isSoundtrackPlaying) {
            soundtrackPlayer.stop();
            isSoundtrackPlaying = false;
        }
    }

    /**
     * Checks if the soundtrack is currently playing
     */
    public boolean isSoundtrackPlaying() {
        return isSoundtrackPlaying;
    }
}
