package igoat.client;

import java.util.Objects;
import javafx.scene.image.Image;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Rectangle;

/**
 * Represents an iGOAT station in the game, which can be activated and displays a specific image
 * when activated.
 */
public class IgoatStation extends Rectangle {

    private final int id;
    private boolean activated = false;

    /**
     * Creates an IgoatStation with the given position, size, and ID.
     *
     * @param x      X position
     * @param y      Y position
     * @param width  Width of the station
     * @param height Height of the station
     * @param id     Station ID
     */
    public IgoatStation(int x, int y, int width, int height, int id) {
        super(x, y, width, height);
        this.id = id;
    }

    /**
     * Returns the station's ID.
     *
     * @return station ID
     */
    public int getStationID() {
        return id;
    }

    /**
     * Activates the station and updates its image.
     *
     * @param x X offset for the image pattern
     * @param y Y offset for the image pattern
     */
    public void activate(double x, double y) {
        activated = true;
        String activatedImagePath = getActivatedImagePathById(id);
        Image activatedImage = new Image(
            Objects.requireNonNull(getClass().getResource(activatedImagePath)).toExternalForm());
        ImagePattern imagePattern = new ImagePattern(activatedImage, x, y, 64, 64, false);
        setFill(imagePattern);
    }

    private String getActivatedImagePathById(int id) {
        switch (id) {
            case 0:
                return "/sprites/iGOAT_station_activated.png";
            case 1:
                return "/sprites/iGOAT_station_activated.png";
            default:
                return "/sprites/iGOAT_station_activated.png";
        }
    }

    /**
     * Returns whether the station is activated.
     *
     * @return true if activated, false otherwise
     */
    public boolean isActivated() {
        return activated;
    }
}

