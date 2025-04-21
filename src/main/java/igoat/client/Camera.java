package igoat.client;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.BlendMode;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.Stop;
import javafx.scene.shape.ArcTo;
import javafx.scene.shape.ClosePath;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.scene.transform.Scale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a camera system for the game world with fog of war effect.
 * The camera follows the player and manages the viewport, including zooming and fog of war effects.
 * The fog of war creates a visibility circle around the player's position, with the rest of the
 * game world obscured by a semi-transparent overlay.
 */
public class Camera {
    private static final Logger logger = LoggerFactory.getLogger(Camera.class);
    private final Pane gamePane;
    private final Canvas fogCanvas;
    private final GraphicsContext fogGC;

    private double viewportWidth;
    private double viewportHeight;
    private final double zoom;
    private final Scale scaleTransform;
    private final Rectangle clip;

    private static final double FOG_OPACITY = 0.8;
    private static final double LIGHT_RADIUS = 100;
    private boolean isLocal = false;
    private Canvas coneCanvas = null;

    /**
     * Creates a new Camera with the specified viewport size and zoom level.
     * Initializes the camera system including the viewport, zoom, and fog of war effect if this
     * is a local player's camera.
     *
     * @param gamePane the main game pane to which all game elements are added
     * @param viewportWidth the initial width of the viewport in pixels
     * @param viewportHeight the initial height of the viewport in pixels
     * @param zoom the zoom level (1.0 = no zoom, &gt; 1.0 = zoom in, &lt; 1.0 = zoom out)
     * @param isLocal true if this is a local player's camera (with fog of war), false otherwise
     */
    public Camera(Pane gamePane, double viewportWidth, double viewportHeight, double zoom, boolean isLocal) {
        this.gamePane = gamePane;
        this.viewportWidth = viewportWidth;
        this.viewportHeight = viewportHeight;
        this.zoom = zoom;
        this.isLocal = isLocal;

        this.scaleTransform = new Scale(zoom, zoom);
        gamePane.getTransforms().add(scaleTransform);

        this.clip = new Rectangle();
        clip.setWidth(viewportWidth / zoom);
        clip.setHeight(viewportHeight / zoom);
        gamePane.setClip(clip);

        this.fogCanvas = new Canvas(viewportWidth, viewportHeight);
        this.fogGC = fogCanvas.getGraphicsContext2D();

        if (isLocal) {
            drawFog(viewportWidth / (2 * zoom), viewportHeight / (2 * zoom));
            gamePane.getChildren().add(fogCanvas);
        }
    }

    /**
     * Updates the viewport size when the window is resized.
     * Adjusts the clip rectangle and fog canvas to match the new dimensions.
     *
     * @param newWidth the new width of the viewport in pixels
     * @param newHeight the new height of the viewport in pixels
     */
    public void updateViewport(double newWidth, double newHeight) {
        this.viewportWidth = newWidth;
        this.viewportHeight = newHeight;

        clip.setWidth(newWidth / zoom);
        clip.setHeight(newHeight / zoom);

        if (isLocal) {
            drawFog(viewportWidth / (2 * zoom), viewportHeight / (2 * zoom));
            fogCanvas.setWidth(newWidth);
            fogCanvas.setHeight(newHeight);

            if (coneCanvas != null) {
                coneCanvas.setWidth(newWidth);
                coneCanvas.setHeight(newHeight);
            }
        }
    }

    /**
     * Updates the camera position to center on the player.
     * Calculates the appropriate translation to keep the player centered in the viewport
     * and updates the fog of war effect position if this is a local player's camera.
     *
     * @param playerX the player's x-coordinate in world space
     * @param playerY the player's y-coordinate in world space
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

        if (isLocal) {
            fogCanvas.setTranslateX(targetX);
            fogCanvas.setTranslateY(targetY);

            if (coneCanvas != null) {
                coneCanvas.setTranslateX(targetX);
                coneCanvas.setTranslateY(targetY);
            }
        }
    }

    /**
     * Draws the fog of war overlay with a transparent visibility circle around the player.
     * Creates a radial gradient that transitions from fully transparent near the player
     * to semi-transparent at the edges of the visibility circle.
     *
     * @param playerScreenX the player's x-coordinate in screen space
     * @param playerScreenY the player's y-coordinate in screen space
     */
    private void drawFog(double playerScreenX, double playerScreenY) {
        fogGC.clearRect(0, 0, fogCanvas.getWidth(), fogCanvas.getHeight());
        fogGC.setFill(Color.rgb(0, 0, 0, 0));
        fogGC.fillRect(0, 0, fogCanvas.getWidth(), fogCanvas.getHeight());

        RadialGradient gradient = new RadialGradient(
                0, 0, playerScreenX, playerScreenY, LIGHT_RADIUS,
                false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.TRANSPARENT),   // Fully transparent at the center
                new Stop(0.8, Color.TRANSPARENT), // Stays transparent up to 80% of the radius
                new Stop(1, Color.rgb(0, 0, 0, FOG_OPACITY)) // Fog appears at the edge
        );

        // Apply the gradient to erase part of the fog
        fogGC.setGlobalBlendMode(BlendMode.SRC_OVER);
        fogGC.setFill(gradient);
        fogGC.fillRect(0, 0, fogCanvas.getWidth(), fogCanvas.getHeight());
    }

    public void updateCone(double angle) {
        if (coneCanvas == null) {
            coneCanvas = new Canvas(viewportWidth, viewportHeight);
            gamePane.getChildren().add(coneCanvas);
            logger.info("created cone effect");
        }
        GraphicsContext gc = coneCanvas.getGraphicsContext2D();

        gc.clearRect(0, 0, coneCanvas.getWidth(), coneCanvas.getHeight());
        gc.setGlobalBlendMode(BlendMode.SRC_OVER);
        gc.setFill(Color.rgb(0, 0, 0, FOG_OPACITY));
        gc.fillRect(0, 0, viewportWidth, viewportHeight);

        Path cone = getCone(viewportWidth / (2 * zoom), viewportHeight / (2 * zoom), 100.0, angle, true, false);
        coneCanvas.setClip(cone);

        Rectangle rectangle = new Rectangle(0, 0, fogCanvas.getWidth(), fogCanvas.getHeight());
        Shape spotlight = Shape.subtract(rectangle, cone);
        fogCanvas.setClip(spotlight);
    }

    /**
     * Creates a cone / slice of a circle using JavaFX paths
     * @param x x position
     * @param y y position
     * @param radius radius of the circle
     * @param angle angle where the cone points in radians
     * @param largeArc see largeArcFlag property of JavaFX ArcTo
     * @param sweep  see sweepFlag property of JavaFX ArcTO
     */
    public static Path getCone(double x, double y, double radius, double angle, boolean largeArc, boolean sweep) {
        Path cone = new Path();
        cone.getElements().add(new MoveTo(x, y));
        cone.getElements().add(new LineTo(x + radius * Math.cos(angle - Math.PI / 4.0), y + radius * Math.sin(angle - Math.PI / 4.0)));
        cone.getElements().add(new ArcTo(radius, radius, 0,
            x + radius * Math.cos(angle + Math.PI / 4.0), y + radius * Math.sin(angle + Math.PI / 4.0), largeArc, sweep));
        cone.getElements().add(new ClosePath());
        cone.setFill(Color.BLACK);

        return cone;
    }
    /**
     * Adds a JavaFX node to the game world.
     * The node will be affected by camera movement and zoom.
     *
     * @param node the JavaFX node to add to the game world
     */
    public void addToWorld(javafx.scene.Node node) {
        gamePane.getChildren().add(node);
    }

    /**
     * Centers the camera on a specific position in the game world.
     * This is a convenience method that calls update() internally.
     *
     * @param x the x-coordinate to center on in world space
     * @param y the y-coordinate to center on in world space
     */
    public void centerOn(double x, double y) {
        update(x, y);
    }
}