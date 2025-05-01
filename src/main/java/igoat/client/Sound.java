package igoat.client;

import java.net.URL;
import javax.sound.sampled.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A class for loading and playing sounds
 */
public class Sound {
    private static final Logger logger = LoggerFactory.getLogger(Sound.class);
    private Clip clip;

    /**
     * Loads a sound from a file (ideally .wav format) which can then be played
     * @param path path to audio file
     */
    public Sound(String path) {
        try {
            URL soundURL = getClass().getResource(path); // e.g., "/sounds/effect.wav"
            if (soundURL == null) {
                throw new IllegalArgumentException("Sound resource not found: " + path);
            }
            AudioInputStream audioIn = AudioSystem.getAudioInputStream(soundURL);
            clip = AudioSystem.getClip();
            clip.open(audioIn);
        } catch (Exception e) {
            logger.error("Couldn't load sound", e);
        }
    }

    /**
     * Plays the sound
     */
    public void play() {
        if (clip == null)
            return;
        if (clip.isRunning())
            clip.stop();
        clip.setFramePosition(0);
        clip.start();
    }

    /**
     * Continuously plays the sound until it is stopped
     */
    public void loop() {
        if (clip == null) return;
        clip.loop(Clip.LOOP_CONTINUOUSLY);
    }

    /**
     * Stops the playback
     */
    public void stop() {
        if (clip != null) clip.stop();
    }
}

