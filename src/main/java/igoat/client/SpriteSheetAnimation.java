package igoat.client;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Duration;

public class SpriteSheetAnimation {
    private final ImageView imageView;
    private final Timeline timeline;
    private final int frameCount;
    private final int frameWidth;
    private final int frameHeight;
    private final int columns;
    private int currentFrame = 0;

    public SpriteSheetAnimation(String spriteSheetPath, int frameWidth, int frameHeight, int frameCount, int columns, double frameDurationMs) {
        Image image = new Image(getClass().getResource(spriteSheetPath).toExternalForm());
        this.imageView = new ImageView(image);
        this.imageView.setSmooth(false);
        this.imageView.setFitWidth(frameWidth);
        this.imageView.setFitHeight(frameHeight);

        this.frameWidth = frameWidth;
        this.frameHeight = frameHeight;
        this.frameCount = frameCount;
        this.columns = columns;

        // Set initial viewport
        imageView.setViewport(new Rectangle2D(0, 0, frameWidth * 10, frameHeight * 10));

        this.timeline = new Timeline(new KeyFrame(Duration.millis(frameDurationMs), e -> updateFrame()));
        timeline.setCycleCount(Timeline.INDEFINITE);
    }

    private void updateFrame() {
        int x = (currentFrame % columns) * frameWidth * 10;
        int y = (currentFrame / columns) * frameHeight * 10;
        imageView.setViewport(new Rectangle2D(x, y, frameWidth * 10, frameHeight * 10));
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