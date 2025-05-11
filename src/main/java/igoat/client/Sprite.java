package igoat.client;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;

/**
 * A wrapper class for javafx Image with some additional functionality for easier use
 */
public class Sprite extends Image {

    public Sprite(String url) {
        super(url);
    }

    public Sprite(String url, int width, int height) {
        super(url, width, height, true, false);
    }

    /**
     * Creates a repeating background pattern
     * @return Repeating background using the image specified in the constructor
     */
    public Background getBackground() {
        BackgroundSize size = new BackgroundSize(64, 64,
            false, false, false, false);

        BackgroundImage backgroundImage = new BackgroundImage(
                resample(this, 4),
        BackgroundRepeat.REPEAT, // repeat horizontally
        BackgroundRepeat.REPEAT, // repeat vertically
        BackgroundPosition.CENTER,
        size
        );

        return new Background(backgroundImage);
    }

    public static Image resample(Image input, int scaleFactor) {
        final int W = (int) input.getWidth();
        final int H = (int) input.getHeight();
        final int S = scaleFactor;

        WritableImage output = new WritableImage(
                W * S,
                H * S
        );

        PixelReader reader = input.getPixelReader();
        PixelWriter writer = output.getPixelWriter();

        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                final int argb = reader.getArgb(x, y);
                for (int dy = 0; dy < S; dy++) {
                    for (int dx = 0; dx < S; dx++) {
                        writer.setArgb(x * S + dx, y * S + dy, argb);
                    }
                }
            }
        }

        return output;
    }
}
