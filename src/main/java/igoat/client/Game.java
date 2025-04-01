package igoat.client;

import java.util.HashSet;
import java.util.Set;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;


public class Game extends Application {

    private static final double PLAYER_WIDTH = 32;
    private static final double PLAYER_HEIGHT = 32;
    private static final double MOVEMENT_SPEED = 300;
    private static final double CAMERA_ZOOM = 4.0;
    
    private Pane gamePane;
    private Set<KeyCode> activeKeys;
    private long lastUpdate;
    
    private Player player;
    private Map gameMap;
    private Camera activeCamera;
    
    /**
     * Initializes and starts the game.
     * Sets up the game window, player, walls, and input handling.
     * Also initializes the game loop using AnimationTimer.
     *
     * @param primaryStage The primary stage of the JavaFX application
     */
    @Override
    public void start(Stage primaryStage) {
        primaryStage.setFullScreen(true);
        primaryStage.setFullScreenExitHint("");
        
        gameMap = new Map();
        
        gamePane = new Pane();
        gamePane.setPrefSize(gameMap.getWidth(), gameMap.getHeight());
        gamePane.setMinSize(gameMap.getWidth(), gameMap.getHeight());
        gamePane.setMaxSize(gameMap.getWidth(), gameMap.getHeight());
        gamePane.setStyle("-fx-background-color: #f0f0f0;");
        
        Scene scene = new Scene(gamePane);
        primaryStage.setTitle("iGoat Game");
        primaryStage.setScene(scene);
        
        primaryStage.show();
        
        for (javafx.scene.Node wall : gameMap.getVisualWalls()) {
            gamePane.getChildren().add(wall);
        }
        
        double startX = 100;
        double startY = 100;
        
        player = new Player(gamePane, scene.getWidth(), scene.getHeight(), CAMERA_ZOOM,
                          (int)startX, (int)startY, (int)PLAYER_WIDTH, (int)PLAYER_HEIGHT, Color.RED);
        
        gamePane.getChildren().add(player.getVisualRepresentation());
        
        player.setSpectated(true);
        activeCamera = player.getCamera();
        
        scene.widthProperty().addListener((obs, oldVal, newVal) -> {
            activeCamera.updateViewport(newVal.doubleValue(), scene.getHeight());
        });
        
        scene.heightProperty().addListener((obs, oldVal, newVal) -> {
            activeCamera.updateViewport(scene.getWidth(), newVal.doubleValue());
        });
        
        primaryStage.fullScreenProperty().addListener((obs, oldVal, newVal) -> {
            javafx.application.Platform.runLater(() -> {
                activeCamera.updateViewport(scene.getWidth(), scene.getHeight());
            });
        });
        
        activeKeys = new HashSet<>();
        scene.setOnKeyPressed(event -> {
            activeKeys.add(event.getCode());
            if (event.getCode() == KeyCode.ESCAPE) {
                primaryStage.setFullScreen(false);
            }
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
        double newX = player.getVisualRepresentation().getX();
        double newY = player.getVisualRepresentation().getY();
        
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

        if (dx != 0) {
            double testX = newX + dx;
            boolean canMove = true;
            for (Wall wall : gameMap.getCollisionWalls()) {
                if (player.collidesWithWall((int)testX, (int)newY, wall)) {
                    canMove = false;
                    break;
                }
            }
            
            if (canMove) {
                newX = testX;
            }
        }
        
        if (dy != 0) {
            double testY = newY + dy;
            boolean canMove = true;
            for (Wall wall : gameMap.getCollisionWalls()) {
                if (player.collidesWithWall((int)newX, (int)testY, wall)) {
                    canMove = false;
                    break;
                }
            }
            
            if (canMove) {
                newY = testY;
            }
        }
        
        player.updatePosition(newX, newY);
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