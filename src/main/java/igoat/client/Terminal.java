package igoat.client;

import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.image.Image;
import javafx.scene.paint.ImagePattern;
import java.util.Objects;

public class Terminal extends Rectangle {
    private final int id;
    private boolean activated = false;

    public Terminal(int x, int y, int width, int height, int id) {
        super(x, y, width, height);
        this.id = id;
    }

    public int getTerminalID() {
        return id;
    }

    public void activate() {
        activated = true;

        String activatedImagePath = getActivatedImagePathById(id);
        Image activatedImage = new Image(Objects.requireNonNull(getClass().getResource(activatedImagePath)).toExternalForm());
        setFill(new ImagePattern(activatedImage));
    }

    private String getActivatedImagePathById(int id) {
        switch (id) {
            case 0: return "/sprites/terminal_activated.png";
            case 1: return "/sprites/terminal_side_activated.png";
            case 2: return "/sprites/terminal_behind_activated.png";
            case 3: return "/sprites/terminal_side_activated.png";
            case 4: return "/sprites/terminal_activated.png";
            case 5: return "/sprites/terminal_activated.png";
            case 6: return "/sprites/terminal_side_activated.png";
            case 7: return "/sprites/terminal_behind_activated.png";
            default: return "/sprites/terminal_activated.png";
        }
    }


    public boolean isActivated() {
        return activated;
    }
}
