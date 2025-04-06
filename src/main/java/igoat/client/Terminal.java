package igoat.client;

import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

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
        setFill(Color.BLUE);
    }

    public boolean isActivated() {
        return activated;
    }
}
