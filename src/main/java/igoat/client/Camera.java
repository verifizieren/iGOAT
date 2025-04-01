package igoat.client;

import javafx.scene.layout.Pane;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Scale;

/**
 * Represents a camera that keeps the player centered in the viewport.
 * Uses JavaFX bindings and a clip rectangle for smooth camera following.
 */
public class Camera {
    private final Pane gamePane;
    private double viewportWidth;
    private double viewportHeight;
    private final double zoom;
    private final Scale scaleTransform;
    private final Rectangle clip;

    /**
     * Creates a new Camera with the specified viewport size and zoom level.
     */
    public Camera(Pane gamePane, double viewportWidth, double viewportHeight, double zoom) {
        this.gamePane = gamePane;
        this.viewportWidth = viewportWidth;
        this.viewportHeight = viewportHeight;
        this.zoom = zoom;
        
        this.scaleTransform = new Scale(zoom, zoom);
        gamePane.getTransforms().add(scaleTransform);
        
        this.clip = new Rectangle();
        clip.setWidth(viewportWidth);
        clip.setHeight(viewportHeight);
        
        gamePane.setClip(clip);
    }

    /**
     * Updates the viewport dimensions and adjusts the camera accordingly.
     */
    public void updateViewport(double newWidth, double newHeight) {
        this.viewportWidth = newWidth;
        this.viewportHeight = newHeight;
        
        clip.setWidth(newWidth);
        clip.setHeight(newHeight);
    }

    /**
     * Updates the camera to center on the player's position.
     * Uses bindings to automatically update the clip position and pane translation.
     */
    public void update(double playerX, double playerY) {
        double centerOffsetX = viewportWidth / (2 * zoom);
        double centerOffsetY = viewportHeight / (2 * zoom);
        
        double targetX = playerX - centerOffsetX;
        double targetY = playerY - centerOffsetY;
        
        gamePane.setTranslateX(-targetX * zoom);
        gamePane.setTranslateY(-targetY * zoom);
    }
    
    /**
     * Adds a node to the game world.
     */
    public void addToWorld(javafx.scene.Node node) {
        gamePane.getChildren().add(node);
    }
} 