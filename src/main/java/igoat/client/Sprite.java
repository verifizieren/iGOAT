package igoat.client;

import javafx.scene.image.Image;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A wrapper class for javafx Image with some additional functionality for easier use
 */
public class Sprite extends Image {
    private static final Logger logger = LoggerFactory.getLogger(Sprite.class);

    public Sprite(String url, double width, double height) {
        super(url);
    }

    /**
     * Creates a repeating background pattern
     * @return Repeating background using the image specified in the constructor
     */
    public Background getBackground() {
        BackgroundSize size = new BackgroundSize(this.getWidth(), this.getHeight(),
            false, false, false, false);

        BackgroundImage backgroundImage = new BackgroundImage(
        this,
        BackgroundRepeat.REPEAT, // repeat horizontally
        BackgroundRepeat.REPEAT, // repeat vertically
        BackgroundPosition.CENTER,
        size
        );

        return new Background(backgroundImage);
    }
}
