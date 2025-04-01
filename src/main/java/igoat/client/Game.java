package igoat.client;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

/**
 * A simple 2D game implementation using JavaFX.
 * The game features a player (represented by a blue circle) that can move around
 * using WASD or arrow keys, with collision detection against walls.
 */
public class Game extends Application {
    /** The radius of the player circle in pixels */
    private static final double PLAYER_RADIUS = 10;
    /** The movement speed of the player in pixels per second */
    private static final double MOVEMENT_SPEED = 300;
    /** The thickness of all walls in pixels */
    private static final double WALL_THICKNESS = 20;
    
    /** The player character represented as a circle */
    private Circle player;
    /** List of all walls in the game */
    private List<Rectangle> walls;
    /** The main game pane containing all game elements */
    private Pane gamePane;
    /** Set of currently pressed keys */
    private Set<KeyCode> activeKeys;
    /** Timestamp of the last update for delta time calculation */
    private long lastUpdate;
    
    /**
     * Initializes and starts the game.
     * Sets up the game window, player, walls, and input handling.
     * Also initializes the game loop using AnimationTimer.
     *
     * @param primaryStage The primary stage of the JavaFX application
     */
    @Override
    public void start(Stage primaryStage) {
        gamePane = new Pane();
        gamePane.setStyle("-fx-background-color: #f0f0f0;");
        
        player = new Circle(PLAYER_RADIUS, Color.BLUE);
        player.setCenterX(50);  
        player.setCenterY(50);  
        gamePane.getChildren().add(player);
        
        walls = new ArrayList<>();
        createWalls();
        
        Scene scene = new Scene(gamePane, 800, 600);
        primaryStage.setTitle("iGoat Game");
        primaryStage.setScene(scene);
        primaryStage.show();
        
        activeKeys = new HashSet<>();
        
        scene.setOnKeyPressed(event -> {
            activeKeys.add(event.getCode());
        });
        
        scene.setOnKeyReleased(event -> {
            activeKeys.remove(event.getCode());
        });
        
        gamePane.setFocusTraversable(true);
        gamePane.requestFocus();
        
        lastUpdate = System.nanoTime();
        new AnimationTimer() {
            @Override
            public void handle(long now) {
                double deltaTime = (now - lastUpdate) / 1_000_000_000.0; 
                lastUpdate = now;
                
                update(deltaTime);
            }
        }.start();
    }
    
    /**
     * Updates the game state based on the elapsed time since the last update.
     * Handles player movement based on active keys and checks for collisions.
     *
     * @param deltaTime The time elapsed since the last update in seconds
     */
    private void update(double deltaTime) {
        double newX = player.getCenterX();
        double newY = player.getCenterY();
        
        double dx = 0;
        double dy = 0;
        
        if (activeKeys.contains(KeyCode.W) || activeKeys.contains(KeyCode.UP)) {
            dy -= MOVEMENT_SPEED * deltaTime;
        }
        if (activeKeys.contains(KeyCode.S) || activeKeys.contains(KeyCode.DOWN)) {
            dy += MOVEMENT_SPEED * deltaTime;
        }
        if (activeKeys.contains(KeyCode.A) || activeKeys.contains(KeyCode.LEFT)) {
            dx -= MOVEMENT_SPEED * deltaTime;
        }
        if (activeKeys.contains(KeyCode.D) || activeKeys.contains(KeyCode.RIGHT)) {
            dx += MOVEMENT_SPEED * deltaTime;
        }

        newX += dx;
        newY += dy;
        
        if (!checkCollision(newX, newY)) {
            player.setCenterX(newX);
            player.setCenterY(newY);
        }
    }
    
    /**
     * Creates and initializes all walls in the game.
     * Sets up the boundary walls and some obstacle walls in the middle of the game area.
     */
    private void createWalls() {
        Rectangle topWall = new Rectangle(0, 0, 800, WALL_THICKNESS);
        Rectangle bottomWall = new Rectangle(0, 580, 800, WALL_THICKNESS);
        Rectangle leftWall = new Rectangle(0, 0, WALL_THICKNESS, 600);
        Rectangle rightWall = new Rectangle(780, 0, WALL_THICKNESS, 600);
        
        Rectangle wall1 = new Rectangle(200, 200, WALL_THICKNESS, 200);
        Rectangle wall2 = new Rectangle(400, 300, 200, WALL_THICKNESS);
        
        Color wallColor = Color.GRAY;
        topWall.setFill(wallColor);
        bottomWall.setFill(wallColor);
        leftWall.setFill(wallColor);
        rightWall.setFill(wallColor);
        wall1.setFill(wallColor);
        wall2.setFill(wallColor);
        
        walls.add(topWall);
        walls.add(bottomWall);
        walls.add(leftWall);
        walls.add(rightWall);
        walls.add(wall1);
        walls.add(wall2);
        
        gamePane.getChildren().addAll(walls);
    }
    
    /**
     * Checks if the player would collide with any wall at the given position.
     *
     * @param newX The x-coordinate to check for collision
     * @param newY The y-coordinate to check for collision
     * @return true if there would be a collision, false otherwise
     */
    private boolean checkCollision(double newX, double newY) {
        Circle tempPlayer = new Circle(newX, newY, PLAYER_RADIUS);
        
        for (Rectangle wall : walls) {
            if (tempPlayer.getBoundsInParent().intersects(wall.getBoundsInParent())) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * The main entry point of the game application.
     *
     * @param args Command line arguments (not used)
     */
    public static void main(String[] args) {
        launch(args);
    }
} 