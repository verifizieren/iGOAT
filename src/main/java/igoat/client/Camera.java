package igoat.client;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.BlendMode;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Scale;

/**
 * Camera with fog of war effect in JavaFX.
 */
public class Camera {
    private final Pane gamePane;
    private final Canvas fogCanvas;
    private final GraphicsContext fogGC;

    private double viewportWidth;
    private double viewportHeight;
    private final double zoom;
    private final Scale scaleTransform;
    private final Rectangle clip;

    private static final double FOG_OPACITY = 0.7;
    private static final double LIGHT_RADIUS_RATIO = 0.08; // About 120px on a 1500px width
    private double lightRadius; // Actual light radius based on viewport size

    /**
     * Creates a new Camera with the specified viewport size and zoom level.
     */
    public Camera(Pane gamePane, double viewportWidth, double viewportHeight, double zoom) {
        this.gamePane = gamePane;
        this.viewportWidth = viewportWidth;
        this.viewportHeight = viewportHeight;
        this.zoom = zoom;

        this.lightRadius = viewportWidth * LIGHT_RADIUS_RATIO;

        this.scaleTransform = new Scale(zoom, zoom);
        gamePane.getTransforms().add(scaleTransform);

        this.clip = new Rectangle();
        clip.setWidth(viewportWidth / zoom);
        clip.setHeight(viewportHeight / zoom);
        gamePane.setClip(clip);

        // Create the fog overlay
        this.fogCanvas = new Canvas(viewportWidth, viewportHeight);
        this.fogGC = fogCanvas.getGraphicsContext2D();
        drawFog(viewportWidth / 2, viewportHeight / 2); // Start with the fog centered

        // Add fog overlay to game pane
        gamePane.getChildren().add(fogCanvas);
    }

    /**
     * Updates the viewport size.
     */
    public void updateViewport(double newWidth, double newHeight) {
        this.viewportWidth = newWidth;
        this.viewportHeight = newHeight;

        clip.setWidth(newWidth / zoom);
        clip.setHeight(newHeight / zoom);

        fogCanvas.setWidth(newWidth);
        fogCanvas.setHeight(newHeight);
    }

    /**
     * Updates the camera to center on the player's position.
     * Also updates the fog effect.
     */
    public void update(double playerX, double playerY) {
        double centerOffsetX = viewportWidth / (2 * zoom);
        double centerOffsetY = viewportHeight / (2 * zoom);

        double targetX = playerX - centerOffsetX;
        double targetY = playerY - centerOffsetY;

        gamePane.setTranslateX(-targetX * zoom);
        gamePane.setTranslateY(-targetY * zoom);

        clip.setX(targetX);
        clip.setY(targetY);

        // Update fog of war effect
        drawFog(playerX, playerY);
    }

    /**
     * Draws the fog overlay with a transparent visibility circle.
     */
    private void drawFog(double playerScreenX, double playerScreenY) {
        fogGC.clearRect(0, 0, fogCanvas.getWidth(), fogCanvas.getHeight());

        // Draw semi-transparent gray fog over the entire screen
        fogGC.setFill(Color.rgb(0, 0, 0, 0));
        fogGC.fillRect(0, 0, fogCanvas.getWidth(), fogCanvas.getHeight());

        // Create a sharper radial gradient
        RadialGradient gradient = new RadialGradient(
                0, 0, playerScreenX, playerScreenY, lightRadius,
                false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.TRANSPARENT),   // Fully transparent at the center
                new Stop(0.8, Color.TRANSPARENT), // Stays transparent up to 80% of the radius
                new Stop(1, Color.rgb(50, 50, 50, FOG_OPACITY)) // Fog appears at the edge
        );

        // Apply the gradient to erase part of the fog
        fogGC.setGlobalBlendMode(BlendMode.SRC_OVER);
        fogGC.setFill(gradient);
        fogGC.fillRect(0, 0, fogCanvas.getWidth(), fogCanvas.getHeight());
    }

    /**
     * Adds a node to the game world.
     */
    public void addToWorld(javafx.scene.Node node) {
        gamePane.getChildren().add(node);
    }

    /**
     * Centers the camera on the specified world position.
     */
    public void centerOn(double x, double y) {
        update(x, y);
    }
}