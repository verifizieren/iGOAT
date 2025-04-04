// THIS IS FOR TESTING PURPOSES ONLY
package igoat.client;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

public class MapTest extends Application {
    private static final double WINDOW_WIDTH = 800;
    private static final double WINDOW_HEIGHT = 600;
    private static final double ZOOM = 0.5;
    private static final double MOVE_SPEED = 50.0;
    
    private double cameraX;
    private double cameraY;
    private double dragStartX;
    private double dragStartY;

    @Override
    public void start(Stage primaryStage) {
        Pane gamePane = new Pane();
        Scene scene = new Scene(gamePane, WINDOW_WIDTH, WINDOW_HEIGHT);
        
        Map map = new Map();
        Camera camera = new Camera(gamePane, WINDOW_WIDTH, WINDOW_HEIGHT, ZOOM, false);
        
        map.getVisualWalls().forEach(wall -> camera.addToWorld(wall));
        
        cameraX = map.getWidth() / 2.0;
        cameraY = map.getHeight() / 2.0;
        camera.centerOn(cameraX, cameraY);
        
        scene.setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case W, UP -> cameraY -= MOVE_SPEED;
                case S, DOWN -> cameraY += MOVE_SPEED;
                case A, LEFT -> cameraX -= MOVE_SPEED;
                case D, RIGHT -> cameraX += MOVE_SPEED;
                default -> { /* Do nothing */ }
            }
            camera.centerOn(cameraX, cameraY);
        });
        
        scene.setOnMousePressed(event -> {
            dragStartX = event.getSceneX();
            dragStartY = event.getSceneY();
        });
        
        scene.setOnMouseDragged(event -> {
            double deltaX = (event.getSceneX() - dragStartX) / ZOOM;
            double deltaY = (event.getSceneY() - dragStartY) / ZOOM;
            cameraX -= deltaX;
            cameraY -= deltaY;
            dragStartX = event.getSceneX();
            dragStartY = event.getSceneY();
            camera.centerOn(cameraX, cameraY);
        });

        scene.widthProperty().addListener((obs, oldVal, newVal) -> 
            camera.updateViewport(newVal.doubleValue(), scene.getHeight()));
        scene.heightProperty().addListener((obs, oldVal, newVal) -> 
            camera.updateViewport(scene.getWidth(), newVal.doubleValue()));
        
        primaryStage.setTitle("Map Test - WASD oder Maus zum begewgeng");
        primaryStage.setScene(scene);
        primaryStage.show();
        
        scene.getRoot().requestFocus();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
