package igoat.client;

import javafx.scene.image.Image;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;

/**
 * A wrapper class for javafx Image with some additional functionality for easier use
 */
public class Sprite extends Image {
    public Sprite(String url, double width, double height) {
        super(url, width, height, true, false);
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
