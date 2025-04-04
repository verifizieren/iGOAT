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
    
    private static final int MAP_WIDTH = 1500;
    private static final int MAP_HEIGHT = 1600;
    
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
        // Outer Walls
        createWall(0, 0, MAP_WIDTH, WALL_THICKNESS); // WALL TOP
        createWall(0, MAP_HEIGHT - WALL_THICKNESS, MAP_WIDTH + 20, WALL_THICKNESS); // WALL BOTTOM
        createWall(0, 0, WALL_THICKNESS, MAP_HEIGHT); // WALL LEFT
        createWall(MAP_WIDTH - WALL_THICKNESS, 0, WALL_THICKNESS, MAP_HEIGHT); // WALL RIGHT

        // Room Top right
        createWall(1170, 300, 330, WALL_THICKNESS);
        createWall(1168, 200, WALL_THICKNESS, 200);
        createWall(1168, 0, WALL_THICKNESS, 100);

        //2
        createWall(1168, 500, WALL_THICKNESS, 100);
        createWall(900, 550, 280, WALL_THICKNESS);
        createWall(1010, 550, WALL_THICKNESS, 300);
        createWall(1020, 700, 168, WALL_THICKNESS);
        createWall(1010, 850, 178, WALL_THICKNESS);
        createWall(1168, 670, WALL_THICKNESS, 250);

        //3
        createWall(750, 550, 100, WALL_THICKNESS);
        createWall(780, 550, WALL_THICKNESS, 500);
        createWall(750, 1050, 439, WALL_THICKNESS);
        createWall(1168, 1000, WALL_THICKNESS, 52);

        //4
        createWall(1000, 1300, 500, WALL_THICKNESS);
        createWall(1100, 1300, WALL_THICKNESS, 100);
        createWall(1100, 1450, WALL_THICKNESS, 170);
        createWall(700,1300, 250, WALL_THICKNESS);
        createWall(800, 1300, WALL_THICKNESS, 320);

        //5
        createWall(300, 1300, 350, WALL_THICKNESS);
        createWall(400, 1250, WALL_THICKNESS, 50);
        createWall(500, 1300, WALL_THICKNESS, 320);
        createWall(200, 1300, 50, WALL_THICKNESS);
        createWall(200, 1000, WALL_THICKNESS, 300);
        createWall(0, 1000, 200, WALL_THICKNESS);

        //6
        createWall(0, 600, 400, WALL_THICKNESS);
        createWall(200, 300, WALL_THICKNESS, 300);
        createWall(200, 300, 300, WALL_THICKNESS);
        createWall(300, 150, WALL_THICKNESS, 150);
        createWall(300, 150, 200, WALL_THICKNESS);
        createWall(0, 200, 150, WALL_THICKNESS);
        createWall(130, 130,WALL_THICKNESS, 70);
        createWall(130, 0, WALL_THICKNESS, 70);

        //7
        createWall(550, 150, 200, WALL_THICKNESS);
        createWall(650, 0, WALL_THICKNESS, 150);
        createWall(800, 150, 100, WALL_THICKNESS);
        createWall(900, 0, WALL_THICKNESS, 400);
        createWall(550, 300, 200, WALL_THICKNESS);
        createWall(800, 300, 100, WALL_THICKNESS);
        createWall(900, 500, WALL_THICKNESS, 50);

        //8
        createWall(600, 550, 100, WALL_THICKNESS);
        createWall(600, 550, WALL_THICKNESS, 150);
        createWall(600, 700, 200, WALL_THICKNESS);
        createWall(600, 700, WALL_THICKNESS, 350);
        createWall(600, 1050, 100, WALL_THICKNESS);
        createWall(400, 550, 300, WALL_THICKNESS);
        createWall(400, 550, WALL_THICKNESS, 350);
        createWall(400,950, WALL_THICKNESS, 150);
        createWall(400, 1050, 200, WALL_THICKNESS);

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