package igoat.client;

import igoat.client.GUI.MainMenuGUI;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpriteSheetAnimation {
    private final ImageView imageView;
    private final Timeline timeline;
    private final int frameCount;
    private final double actualFrameWidth;
    private final double actualFrameHeight;
    private final int columns;
    private int currentFrame = 0;

    public SpriteSheetAnimation(String spriteSheetPath, int frameWidth, int frameHeight, int frameCount, int columns, double frameDurationMs) {
        Logger logger = LoggerFactory.getLogger(SpriteSheetAnimation.class);

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
        int x = (int)((currentFrame % columns) * actualFrameWidth);
        imageView.setViewport(new Rectangle2D(x, 0, actualFrameWidth, actualFrameHeight));
        currentFrame = (currentFrame + 1) % frameCount;
    }

    public void play() {
        timeline.play();
    }

    public void stop() {
        timeline.stop();
    }

    public ImageView getView() {
        return imageView;
    }
}