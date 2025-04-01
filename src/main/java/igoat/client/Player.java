package igoat.client;

import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class Player {
    public int x;
    public int y;
    private final int width;
    private final int height;
    private final Rectangle visualRepresentation;
    private final Camera camera;
    private boolean isBeingSpectated;

    /**
     * Creates a new player with the specified position and dimensions.
     */
    public Player(Pane gamePane, double viewportWidth, double viewportHeight, double zoom,
                 int x, int y, int width, int height, Color color) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        
        this.visualRepresentation = new Rectangle(width, height);
        this.visualRepresentation.setFill(color);
        this.visualRepresentation.setX(x);
        this.visualRepresentation.setY(y);
        
        this.camera = new Camera(gamePane, viewportWidth, viewportHeight, zoom);
        this.isBeingSpectated = false;
    }

    /**
     * Updates the player's position and camera.
     */
    public void updatePosition(double newX, double newY) {
        this.x = (int)newX;
        this.y = (int)newY;
        this.visualRepresentation.setX(newX);
        this.visualRepresentation.setY(newY);
        
        if (isBeingSpectated) {
            updateCamera();
        }
    }

    /**
     * Updates the camera to follow this player.
     */
    public void updateCamera() {
        camera.update(visualRepresentation.getX(), visualRepresentation.getY());
    }

    /**
     * Sets whether this player is being spectated.
     */
    public void setSpectated(boolean spectated) {
        this.isBeingSpectated = spectated;
        if (spectated) {
            updateCamera();
        }
    }

    /**
     * Gets the player's camera.
     */
    public Camera getCamera() {
        return camera;
    }

    /**
     * Gets the visual representation of the player.
     */
    public Rectangle getVisualRepresentation() {
        return visualRepresentation;
    }

    /**
     * Checks if this player collides with a wall.
     * Uses exact dimensions for precise collision detection.
     */
    public boolean collidesWithWall(int testX, int testY, Wall wall) {
        return testX < wall.x + wall.width &&
               testX + width > wall.x &&
               testY < wall.y + wall.height &&
               testY + height > wall.y;
    }
} 