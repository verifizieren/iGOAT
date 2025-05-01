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
    private FloatControl volumeControl;

    /**
     * Loads a sound from a file (ideally .wav format) which can then be played
     * @param path path to audio file
     */
    public Sound(String path) {
        try {
            URL soundURL = getClass().getResource(path);
            if (soundURL == null) {
                throw new IllegalArgumentException("Sound resource not found: " + path);
            }
            AudioInputStream audioIn = AudioSystem.getAudioInputStream(soundURL);
            clip = AudioSystem.getClip();
            clip.open(audioIn);

            if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                volumeControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            }
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

    /**
     * Sets the volume for the sound
     * @param volume The volume between 0.0 and 1.0
     */
    public void setVolume(double volume) {
        if (volumeControl != null) {
            float min = volumeControl.getMinimum();
            float max = volumeControl.getMaximum();
            float dB = (float)(Math.log10(Math.max(volume, 0.0001)) * 20.0);
            dB = Math.max(min, Math.min(dB, max));
            volumeControl.setValue(dB);
        }
    }
}

