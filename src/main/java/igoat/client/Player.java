package igoat.client;

import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

public class Player {
    public int x;
    public int y;
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
    public Player(Pane gamePane, double viewportWidth, double viewportHeight, double zoom,
                 int x, int y, int width, int height, Color color, String username) {
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
        updateUsernamePosition();
        
        if (isBeingSpectated) {
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
    public void setUsername(String username) {
        this.username = username;
        this.usernameLabel.setText(username);
        updateUsernamePosition();
    }

    /**
     * Gets the player's username.
     */
    public String getUsername() {
        return username;
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

    /**
     * Gets the player's x-coordinate.
     */
    public int getX() {
        return x;
    }

    /**
     * Gets the player's y-coordinate.
     */
    public int getY() {
        return y;
    }

    /**
     * Gets the player's width.
     */
    public int getWidth() {
        return width;
    }

    /**
     * Gets the player's height.
     */
    public int getHeight() {
        return height;
    }

    /**
     * Gets the username label for this player.
     */
    public Text getUsernameLabel() {
        return usernameLabel;
    }
} 