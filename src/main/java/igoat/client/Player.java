package igoat.client;

import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

/**
 * Represents a player in the game.
 * Manages the player's visual representation, position, collision detection,
 * and camera view. Can represent either the local player or a remote player.
 */
public class Player {
    private double x;
    private double y;
    private final int width;
    private final int height;
    private final Rectangle visualRepresentation;
    private final Text usernameLabel;
    private final Camera camera;
    private boolean isBeingSpectated;
    private String username;

    /**
     * Creates a new player with the specified position and dimensions.
     */
    /**
     * Creates a new player with the specified properties.
     *
     * @param gamePane The JavaFX pane where the player will be rendered
     * @param viewportWidth The width of the player's viewport
     * @param viewportHeight The height of the player's viewport
     * @param zoom The initial zoom level for the player's camera
     * @param x The initial X coordinate
     * @param y The initial Y coordinate
     * @param width The width of the player's visual representation
     * @param height The height of the player's visual representation
     * @param color The color of the player's visual representation
     * @param username The player's username
     * @param isLocalPlayer Whether this is the local player (true) or a remote player (false)
     */
    public Player(Pane gamePane, double viewportWidth, double viewportHeight, double zoom,
                 double x, double y, int width, int height, Color color, String username, boolean isLocalPlayer) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.username = username;
        
        this.visualRepresentation = new Rectangle(width, height);
        this.visualRepresentation.setFill(color);
        this.visualRepresentation.setX(x);
        this.visualRepresentation.setY(y);
        
        this.usernameLabel = new Text(username);
        this.usernameLabel.setFont(Font.font("Arial", 12));
        this.usernameLabel.setFill(Color.BLACK);
        updateUsernamePosition();
        
        gamePane.getChildren().addAll(visualRepresentation, usernameLabel);
        
        if (isLocalPlayer) {
            this.camera = new Camera(gamePane, viewportWidth, viewportHeight, zoom, true);
        } else {
            this.camera = null;
        }
        this.isBeingSpectated = false;
    }

    /**
     * Updates the player's position and camera.
     */
    /**
     * Updates the player's position and visual representation.
     *
     * @param newX The new X coordinate
     * @param newY The new Y coordinate
     */
    public void updatePosition(double newX, double newY) {
        this.x = newX;
        this.y = newY;
        this.visualRepresentation.setX(newX);
        this.visualRepresentation.setY(newY);
        updateUsernamePosition();
        
        if (isBeingSpectated && camera != null) {
            updateCamera();
        }
    }

    /**
     * Updates the username label position to stay above the player.
     */
    private void updateUsernamePosition() {
        double textWidth = usernameLabel.getLayoutBounds().getWidth();
        usernameLabel.setX(x + (width - textWidth) / 2);
        usernameLabel.setY(y - 5);
    }

    /**
     * Sets the player's username and updates the label.
     */
    /**
     * Sets the player's username and updates the username label.
     *
     * @param username The new username to set
     */
    public void setUsername(String username) {
        this.username = username;
        this.usernameLabel.setText(username);
        updateUsernamePosition();
    }

    /**
     * Gets the player's username.
     */
    /**
     * Gets the player's username.
     *
     * @return The player's current username
     */
    public String getUsername() {
        return username;
    }

    /**
     * Updates the camera to follow this player.
     */
    public void updateCamera() {
        if (camera != null) {
            camera.update(visualRepresentation.getX(), visualRepresentation.getY());
        }
    }

    /**
     * Sets whether this player is being spectated.
     */
    /**
     * Sets whether this player is being spectated.
     * Updates visual effects accordingly.
     *
     * @param spectated true if the player is being spectated, false otherwise
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
    /**
     * Gets the player's camera.
     *
     * @return The Camera object associated with this player
     */
    public Camera getCamera() {
        return camera;
    }

    /**
     * Gets the visual representation of the player.
     */
    /**
     * Gets the player's visual representation.
     *
     * @return The Rectangle object representing the player in the game
     */
    public Rectangle getVisualRepresentation() {
        return visualRepresentation;
    }

    /**
     * Checks if this player collides with a wall.
     * Uses exact dimensions for precise collision detection.
     */
    /**
     * Tests if a potential position would collide with a wall.
     *
     * @param testX The X coordinate to test
     * @param testY The Y coordinate to test
     * @param wall The wall to check collision with
     * @return true if the position would collide with the wall, false otherwise
     */
    public boolean collidesWithWall(double testX, double testY, Wall wall) {
        return testX < wall.x + wall.width &&
               testX + width > wall.x &&
               testY < wall.y + wall.height &&
               testY + height > wall.y;
    }

    /**
     * Gets the player's x-coordinate.
     */
    /**
     * Gets the player's X coordinate.
     *
     * @return The current X coordinate
     */
    public double getX() {
        return x;
    }

    /**
     * Gets the player's y-coordinate.
     */
    /**
     * Gets the player's Y coordinate.
     *
     * @return The current Y coordinate
     */
    public double getY() {
        return y;
    }

    /**
     * Gets the player's width.
     */
    /**
     * Gets the player's width.
     *
     * @return The width of the player's visual representation
     */
    public int getWidth() {
        return width;
    }

    /**
     * Gets the player's height.
     */
    /**
     * Gets the player's height.
     *
     * @return The height of the player's visual representation
     */
    public int getHeight() {
        return height;
    }

    /**
     * Gets the username label for this player.
     */
    /**
     * Gets the player's username label.
     *
     * @return The Text object displaying the player's username
     */
    public Text getUsernameLabel() {
        return usernameLabel;
    }

    public void setColor(Color color) {
        if (visualRepresentation != null) {
            visualRepresentation.setFill(color);
        }
    }

} 