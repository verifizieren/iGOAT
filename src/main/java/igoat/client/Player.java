package igoat.client;

import igoat.Role;
import javafx.scene.Group;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a player in the game.
 * Manages the player's visual representation, position, collision detection,
 * and camera view. Can represent either the local player or a remote player.
 */
public class Player {
    private static final Logger logger = LoggerFactory.getLogger(Player.class);

    private double x;
    private double y;
    private final int width;
    private final int height;
    private Group visual;
    private SpriteSheetAnimation animation;
    private ImageView idle;
    private final Text usernameLabel;
    private final Camera camera;
    private boolean isBeingSpectated;
    private String username;
    private Role role = null;
    private boolean isDown;
    private Pane gamePane;

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

        this.gamePane = gamePane;

        this.animation = new SpriteSheetAnimation("/sprites/invisible_placeholder.png", 1, 1, 1, 1, 1);
        this.idle = new ImageView();
        this.visual = new Group();
        
        this.usernameLabel = new Text(username);
        this.usernameLabel.setFont(Font.font("Jersey 10", 12));
        this.usernameLabel.setFill(Color.BLACK);
        updateUsernamePosition();
        
        gamePane.getChildren().add(usernameLabel);
        
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
        // show idle sprite when not moving
        if (x == newX && y == newY) {
            animation.stop();
            animation.getView().setVisible(false);
            idle.setVisible(true);
            return;
        }

        if (x - newX < 0) {
            idle.setScaleX(1);
            animation.getView().setScaleX(1);
        }
        else if (x - newX > 0) {
            idle.setScaleX(-1);
            animation.getView().setScaleX(-1);
        }


        this.x = newX;
        this.y = newY;
        this.animation.getView().setX(x);
        this.animation.getView().setY(y);
        this.idle.setX(x);
        this.idle.setY(y);
        updateUsernamePosition();
        
        if (isBeingSpectated && camera != null) {
            updateCamera();
        }

        // show animation when moving
        idle.setVisible(false);
        animation.getView().setVisible(true);
        animation.play();
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
            camera.update(idle.getX(), idle.getY());
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
    public Group getVisual() {
        return visual;
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
     * @param width player width
     * @param height = player height
     * @param wall The wall to check collision with
     * @return true if the position would collide with the wall, false otherwise
     */
    public static boolean collidesWithWall(double testX, double testY, double width, double height, Wall wall) {
        return testX < wall.x + wall.width &&
               testX + width > wall.x &&
               testY < wall.y + wall.height &&
               testY + height > wall.y;
    }

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

    public void setRole(Role role) {
        this.role = role;

        switch (role) {
            case Role.GOAT -> {
                animation = new SpriteSheetAnimation("/sprites/goat_walking.png",
                    32, 32, 8, 8, 100);
                animation.getView().setX(this.x);
                animation.getView().setY(this.y);
                animation.getView().setVisible(false);

                idle = new ImageView(new Sprite("/sprites/goat_idle.png"));
                idle.setX(this.x);
                idle.setY(this.y);
                idle.setFitWidth(32);
                idle.setFitHeight(32);
                idle.setVisible(true);

                visual = new Group(animation.getView(), idle);
                gamePane.getChildren().add(visual);
            }
            case Role.IGOAT -> {
                animation = new SpriteSheetAnimation("/sprites/igoat_walking.png",
                    32, 32, 8, 8, 100);
                animation.getView().setX(this.x);
                animation.getView().setY(this.y);
                animation.getView().setVisible(false);

                idle = new ImageView(new Sprite("/sprites/igoat_idle.png"));
                idle.setX(this.x);
                idle.setY(this.y);
                idle.setFitWidth(32);
                idle.setFitHeight(32);
                idle.setVisible(true);

                visual = new Group(animation.getView(), idle);
                gamePane.getChildren().add(visual);
            }
            default -> {
                animation = new SpriteSheetAnimation("/sprites/goat_walking.png",
                    32, 32, 8, 8, 100);
                animation.getView().setX(this.x);
                animation.getView().setY(this.y);
                idle.setFitWidth(32);
                idle.setFitHeight(32);
                animation.getView().setVisible(false);

                idle = new ImageView(new Sprite("/sprites/goat_idle.png"));
                idle.setX(this.x);
                idle.setY(this.y);
                idle.setVisible(true);

                visual = new Group(animation.getView(), idle);
                gamePane.getChildren().add(visual);
            }
        }
    }

    public Role getRole() {
        return role;
    }

    /**
     * Checks if the player is down (unable to move).
     * 
     * @return true if the player is down, false otherwise
     */
    public boolean isDown() {
        return isDown;
    }

    /**
     * Sets whether the player is down (unable to move).
     * 
     * @param down true to mark the player as down, false otherwise
     */
    public void setDown(boolean down) {
        this.isDown = down;
    }
} 