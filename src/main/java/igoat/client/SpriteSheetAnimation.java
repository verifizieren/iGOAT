package igoat.client;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles sprite sheet animation for an ImageView using a Timeline.
 * Allows playing and stopping frame-based animation from a sprite sheet image.
 */
public class SpriteSheetAnimation {
    private static final Logger logger = LoggerFactory.getLogger(SpriteSheetAnimation.class);

    private final ImageView imageView;
    private final Timeline timeline;
    private final int frameCount;
    private final double actualFrameWidth;
    private final double actualFrameHeight;
    private final int columns;
    private int currentFrame = 0;

    /**
     * Constructs a SpriteSheetAnimation.
     * @param spriteSheetPath Path to the sprite sheet image resource
     * @param frameWidth Width of each frame in pixels
     * @param frameHeight Height of each frame in pixels
     * @param frameCount Total number of frames in the animation
     * @param columns Number of columns in the sprite sheet
     * @param frameDurationMs Duration of each frame in milliseconds
     */
    public SpriteSheetAnimation(String spriteSheetPath, int frameWidth, int frameHeight, int frameCount, int columns, double frameDurationMs) {
        Image image = new Image(getClass().getResource(spriteSheetPath).toExternalForm());
        this.imageView = new ImageView(image);
        this.imageView.setSmooth(false);
        this.imageView.setFitWidth(frameWidth);
        this.imageView.setFitHeight(frameHeight);
        this.actualFrameWidth = image.getWidth() / columns;
        this.actualFrameHeight = image.getHeight();

        this.frameCount = frameCount;
        this.columns = columns;

        // Set initial viewport
        imageView.setViewport(new Rectangle2D(0, 0, actualFrameWidth, actualFrameHeight));

        this.timeline = new Timeline(new KeyFrame(Duration.millis(frameDurationMs), e -> updateFrame()));
        timeline.setCycleCount(Timeline.INDEFINITE);
    }

    private void updateFrame() {
        double x = (currentFrame % columns) * actualFrameWidth + (0.5) * imageView.getScaleX(); // + 0.5 mitigates visual glitch (javafx is shit)
        imageView.setViewport(new Rectangle2D(x, 0, actualFrameWidth, actualFrameHeight));
        currentFrame = (currentFrame + 1) % frameCount;
    }

    /**
     * Starts the animation.
     */
    public void play() {
        timeline.play();
    }

    /**
     * Stops the animation.
     */
    public void stop() {
        timeline.stop();
    }

    /**
     * Returns the ImageView displaying the animation.
     * @return the ImageView
     */
    public ImageView getView() {
        return imageView;
    }
}