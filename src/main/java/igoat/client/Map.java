package igoat.client;

import java.util.ArrayList;
import java.util.List;

import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

/**
 * Represents a game map containing walls and their layout.
 * This class handles the creation and management of walls in the game.
 */
public class Map {
    private static final int WALL_THICKNESS = 20;
    private static final int SPECIAL_ELEMENT_WIDTH = 40;
    
    private static final int MAP_WIDTH = 1200;
    private static final int MAP_HEIGHT = 800;
    
    private List<Rectangle> visualWalls;
    private List<Wall> collisionWalls;
    private List<Rectangle> specialElements;
    
    /**
     * Creates a new Map with the layout from the design.
     */
    public Map() {
        visualWalls = new ArrayList<>();
        collisionWalls = new ArrayList<>();
        specialElements = new ArrayList<>();
        createMapLayout();
    }
    
    /**
     * Creates the map layout based on the design image.
     */
    private void createMapLayout() {
        createWall(0, 0, MAP_WIDTH, WALL_THICKNESS); 
        createWall(0, MAP_HEIGHT - WALL_THICKNESS, MAP_WIDTH, WALL_THICKNESS); 
        createWall(0, 0, WALL_THICKNESS, MAP_HEIGHT); 
        createWall(MAP_WIDTH - WALL_THICKNESS, 0, WALL_THICKNESS, MAP_HEIGHT); 
        
        createWall(400, 0, WALL_THICKNESS, 150);
        createWall(400, 150, 200, WALL_THICKNESS);
        
        createWall(800, 0, WALL_THICKNESS, 400);
        createWall(800, 400, 200, WALL_THICKNESS);
        createWall(1000, 200, WALL_THICKNESS, 400);
        
        createWall(250, 300, 400, WALL_THICKNESS);
        createWall(400, 300, WALL_THICKNESS, 300);
        createWall(600, 400, WALL_THICKNESS, 200);
        
        createWall(0, 500, 200, WALL_THICKNESS);
        createWall(200, 600, 400, WALL_THICKNESS);
        
        createWall(800, 400, WALL_THICKNESS, 200);
        createWall(800, 600, 200, WALL_THICKNESS);
    }
    
    /**
     * Creates a wall with both visual and collision components.
     */
    private void createWall(int x, int y, int width, int height) {
        Rectangle visualWall = new Rectangle(x, y, width, height);
        visualWall.setFill(Color.GRAY);
        visualWalls.add(visualWall);
        
        Wall collisionWall = new Wall(x, y, width, height);
        collisionWalls.add(collisionWall);
    }
    
    /**
     * Adds a special element (terminal, window, or exit) to the map.
     */
    private void addSpecialElement(int x, int y, Color color) {
        Rectangle element = new Rectangle(x, y, SPECIAL_ELEMENT_WIDTH, WALL_THICKNESS);
        element.setFill(color);
        specialElements.add(element);
    }
    
    /**
     * Gets all visual elements including walls and special elements.
     */
    public List<Rectangle> getVisualWalls() {
        List<Rectangle> allElements = new ArrayList<>(visualWalls);
        allElements.addAll(specialElements);
        return allElements;
    }
    
    /**
     * Gets the list of collision wall objects.
     */
    public List<Wall> getCollisionWalls() {
        return collisionWalls;
    }
    
    /**
     * Gets the width of the map.
     */
    public int getWidth() {
        return MAP_WIDTH;
    }
    
    /**
     * Gets the height of the map.
     */
    public int getHeight() {
        return MAP_HEIGHT;
    }
    
    /**
     * Clears all walls from the map.
     */
    public void clearWalls() {
        visualWalls.clear();
        collisionWalls.clear();
        specialElements.clear();
    }
    
    /**
     * Adds a new wall to the map.
     *
     * @param x The x-coordinate of the wall
     * @param y The y-coordinate of the wall
     * @param width The width of the wall
     * @param height The height of the wall
     */
    public void addWall(int x, int y, int width, int height) {
        createWall(x, y, width, height);
    }
} 