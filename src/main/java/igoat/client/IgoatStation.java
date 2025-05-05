package igoat.client;

import java.util.Objects;
import javafx.scene.image.Image;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Rectangle;

public class IgoatStation extends Rectangle {
    private final int id;
    private boolean activated = false;

    public IgoatStation(int x, int y, int width, int height, int id) {
        super(x, y, width, height);
        this.id = id;
    }

    public int getStationID() {
        return id;
    }


    public void activate(double x, double y) {
        activated = true;
        String activatedImagePath = getActivatedImagePathById(id);
        Image activatedImage = new Image(Objects.requireNonNull(getClass().getResource(activatedImagePath)).toExternalForm());
        ImagePattern imagePattern = new ImagePattern(activatedImage, x, y , 64,64 , false);
        setFill(imagePattern);

    }

    private String getActivatedImagePathById(int id) {
        switch (id) {
            case 1: return "/sprites/iGOAT_station_activated.png";
            case 2: return "/sprites/iGOAT_station_activated.png";
            default: return "/sprites/terminal_activated.png";
        }
    }

    public boolean isActivated() {
        return activated;
    }
}

