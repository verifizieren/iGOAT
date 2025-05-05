package igoat.client;

import java.util.ArrayList;
import java.util.List;

import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javafx.scene.image.ImageView;
import javafx.scene.image.Image;
import javafx.scene.paint.ImagePattern;
import java.util.Objects;

/**
 * Represents a game map containing walls and their layout.
 * This class handles the creation and management of walls in the game.
 */
public class Map {
    private static final Logger logger = LoggerFactory.getLogger(Map.class);

    private final boolean noVisuals;

    private static final int WALL_THICKNESS = 20;
    private static final int SPECIAL_ELEMENT_WIDTH = 40;
    private static final int DOOR_WIDTH = 60;
    
    private static final int MAP_WIDTH = 1500;
    private static final int MAP_HEIGHT = 1600;

    private List<Wall> collisionWalls;
    private List<Terminal> terminalList;
    private List<Wall> doorCollisions;
    private List<Wall> windowCollisions;
    private List<ImageView> decorItems;

    private List<Rectangle> visualWalls;
    private List<Rectangle> doorVisuals;
    private List<Rectangle> windowVisuals;
    private List<IgoatStation> IgoatStationList;

    /**
     * Creates a new Map with the layout from the design.
     * @param noVisuals This determines whether the visual elements of the map are created, which
     * are not necessary for the server side implementation
     */
    public Map(boolean noVisuals) {
        this.noVisuals = noVisuals;

        collisionWalls = new ArrayList<>();
        terminalList = new ArrayList<>();
        doorCollisions = new ArrayList<>();
        windowCollisions = new ArrayList<>();
        decorItems = new ArrayList<>();
        IgoatStationList = new ArrayList<>();

        if (!noVisuals) {
            visualWalls = new ArrayList<>();
            doorVisuals = new ArrayList<>();
            windowVisuals = new ArrayList<>();
        }
        createMapLayout();
        createTerminals();
        createDoor();
        createWindow();
        createDecor();
        createIgoatStation();


    }
    
    /**
     * Creates the map layout based on the design image.
     */
    private void createMapLayout() {
        // Outer Walls
        createWall(0, 0, MAP_WIDTH, WALL_THICKNESS); // WALL TOP
        createWall(0, MAP_HEIGHT - WALL_THICKNESS, MAP_WIDTH, WALL_THICKNESS); // WALL BOTTOM
        createWall(0, -42, WALL_THICKNESS, 542); // WALL LEFT
        createWall(0, 560, WALL_THICKNESS, MAP_HEIGHT - 560);
        createWall(MAP_WIDTH - WALL_THICKNESS, -42, WALL_THICKNESS, 542); // WALL RIGHT
        createWall(MAP_WIDTH - WALL_THICKNESS, 560, WALL_THICKNESS, MAP_HEIGHT - 560);

        // Room Top right
        createWall(1170, 300, 330, WALL_THICKNESS);
        createWall(1168, 200, WALL_THICKNESS, 200);
        createWall(1168, -42, WALL_THICKNESS, 142);

        //2
        createWall(1168, 500, WALL_THICKNESS, 100);
        createWall(900, 550, 280, WALL_THICKNESS);
        createWall(1010, 550, WALL_THICKNESS, 300);
        createWall(1020, 700, 168, WALL_THICKNESS);
        createWall(1010, 850, 178, WALL_THICKNESS);
        createWall(1168, 670, WALL_THICKNESS, 240);

        //3
        createWall(750, 550, 100, WALL_THICKNESS);
        createWall(780, 550, WALL_THICKNESS, 500);
        createWall(750, 1050, 438, WALL_THICKNESS);
        createWall(1168, 1000, WALL_THICKNESS, 52);

        //4
        createWall(1000, 1300, 500, WALL_THICKNESS);
        createWall(1100, 1300, WALL_THICKNESS, 100);
        createWall(1100, 1450, WALL_THICKNESS, 150);
        createWall(700,1300, 250, WALL_THICKNESS);
        createWall(800, 1300, WALL_THICKNESS, 300);

        //5
        createWall(300, 1300, 350, WALL_THICKNESS);
        createWall(400, 1250, WALL_THICKNESS, 50);
        createWall(500, 1300, WALL_THICKNESS, 300);
        createWall(200, 1300, 50, WALL_THICKNESS);
        createWall(200, 1000, WALL_THICKNESS, 150);
        createWall(200, 1190, WALL_THICKNESS, 110);
        createWall(0, 1000, 300, WALL_THICKNESS);

        //6
        createWall(0, 600, 280, WALL_THICKNESS);
        createWall(330, 600, 90, WALL_THICKNESS);
        createWall(200, 300, WALL_THICKNESS, 300);
        createWall(200, 300, 300, WALL_THICKNESS);
        createWall(300, 150, WALL_THICKNESS, 150);
        createWall(300, 150, 200, WALL_THICKNESS);
        createWall(0, 200, 150, WALL_THICKNESS);
        createWall(130, 130,WALL_THICKNESS, 20);
        createWall(130, 190, WALL_THICKNESS, 10);
        createWall(130, -42, WALL_THICKNESS, 120);

        //7
        createWall(550, 150, 200, WALL_THICKNESS);
        createWall(650, -42, WALL_THICKNESS, 192);
        createWall(800, 150, 100, WALL_THICKNESS);
        createWall(900, -42, WALL_THICKNESS, 262);
        createWall(900, 260, WALL_THICKNESS, 140);
        createWall(550, 300, 200, WALL_THICKNESS);
        createWall(800, 300, 100, WALL_THICKNESS);
        createWall(900, 500, WALL_THICKNESS, 50);

        //8
        createWall(600, 550, 100, WALL_THICKNESS);
        createWall(600, 550, WALL_THICKNESS, 150);
        createWall(600, 700, 200, WALL_THICKNESS);
        createWall(600, 700, WALL_THICKNESS, 80);
        createWall(600, 820, WALL_THICKNESS, 230);
        createWall(600, 1050, 100, WALL_THICKNESS);
        createWall(400, 550, 300, WALL_THICKNESS);
        createWall(400, 550, WALL_THICKNESS, 350);
        createWall(400,950, WALL_THICKNESS, 150);
        createWall(400, 1050, 200, WALL_THICKNESS);

    }

    /**
     * adds terminals to the map
     */
    private void createTerminals(){
        //create Terminals
        addTerminal(1454, 75, 56, 64, 0,
            "/sprites/terminal.png");
        addTerminal(1460, 1450, 52, 64, 1,
            "/sprites/terminal_side.png");
        addTerminal(1040, 1517, 56, 64, 2,
            "/sprites/terminal_behind.png");
        addTerminal(480, 1450, 52, 64, 3,
            "/sprites/terminal_side.png");
        addTerminal(61, 735,56, 64, 4,
            "/sprites/terminal.png");
        addTerminal(874, 50, 56, 64, 5,
            "/sprites/terminal.png");
        addTerminal(760, 620, 52, 64, 6,
            "/sprites/terminal_side.png");
        addTerminal(1070, 660, 56, 64, 7,
            "/sprites/terminal_behind.png");
    }

    /**
     * adds the doors to the map
     */
    private void createDoor() {
       addDoor(0, 500, 20, DOOR_WIDTH, "/sprites/door.png");
       addDoor(1480, 500, 20, DOOR_WIDTH, "/sprites/door.png");
    }

    /**
     * add the windows to the map
     */
    private void createWindow() {
        addWindow(130, 150, 20, 40, "/sprites/window.png");
        addWindow(200, 1150, 20, 40, "/sprites/window.png");
        addWindow(900, 220, 20, 40, "/sprites/window.png");
        addWindow(600, 780,20, 40, "/sprites/window.png");
    }

    private void createIgoatStation() {
        addIgoatStation(20,130,42,64, 1,"/sprites/igoat_station.png");
        addIgoatStation(600,1418,42,64, 2, "/sprites/igoat_station.png");
    }



    /**
     * Creates a wall with both visual and collision components.
     */
    private void createWall(int x, int y, int width, int height) {
        if (!noVisuals) {
            Rectangle visualWall = new Rectangle(x, y, width, height);
            visualWall.setFill(Color.web("#363442"));
            visualWalls.add(visualWall);
        }

        Wall collisionWall = new Wall(x, y, width, height);
        collisionWalls.add(collisionWall);
    }

    private void createDecor() {
        for (Decoration decor : SceneDecoration.getDecorList()) {
            addDecorItem(decor);
        }
    }
    
    /**
     * Adds a terminal to the map.
     */
    private void addTerminal(int x, int y, int width, int height, int id, String imagePath) {
        Terminal terminal = new Terminal(x, y, width, height, id);
        if (!noVisuals) {
            Image image = new Image(Objects.requireNonNull(getClass().getResource(imagePath)).toExternalForm());
            terminal.setFill(new ImagePattern(image));
        }
        terminalList.add(terminal);

        Wall collisionWall = new Wall(x, y, width, height);
        collisionWalls.add(collisionWall);
    }

    private void addIgoatStation(int x, int y, int width, int height, int id, String imagePath) {
        IgoatStation station = new IgoatStation(x, y, width, height, id);
        if (!noVisuals) {
            Image igoatStationImage = new Image(Objects.requireNonNull(getClass().getResource(imagePath)).toExternalForm());
            ImagePattern imagePattern = new ImagePattern(igoatStationImage, x, y, 64, 64,false);
            station.setFill(imagePattern);

            IgoatStationList.add(station);
        }

        Wall collisionWall = new Wall(x, y, width, height);
        collisionWalls.add(collisionWall);
    }

    /**
     * Add two exits to the map
     */
    private void addDoor(int x, int y, int width, int height, String imagePath) {
        if (!noVisuals) {
            Rectangle doorVisual = new Rectangle(x, y, width, height);

            Image doorImage = new Image(imagePath);
            ImagePattern imagePattern = new ImagePattern(doorImage, x, y, 42, 60,false);
            doorVisual.setFill(imagePattern);

            visualWalls.add(doorVisual);
            doorVisuals.add(doorVisual);
        }

        Wall collisionWall = new Wall(x, y, width, height);
        collisionWalls.add(collisionWall);
        doorCollisions.add(collisionWall);
    }

    /**
     * Add windows to the map
     */

    private void addWindow(int x, int y, int width, int height, String imagePath) {
        if (!noVisuals) {
            Rectangle windowVisual = new Rectangle(x, y, width, height);

            Image windowImage = new Image(imagePath);
            ImagePattern imagePattern = new ImagePattern(windowImage, x, y, 42, 60,false);
            windowVisual.setFill(imagePattern);

            visualWalls.add(windowVisual);
            windowVisuals.add(windowVisual);
        }

        Wall collisionWall = new Wall(x, y, width, height);
        windowCollisions.add(collisionWall);
    }

    /**
     * Opens the doors by making them visually slightly transparent and removing their collision.
     */
    public void openDoors() {
        collisionWalls.removeAll(doorCollisions);
        doorCollisions.clear();

        if (!noVisuals) {
            for (Rectangle doorVisual : doorVisuals) {
                doorVisual.setFill(Color.LIMEGREEN.deriveColor(0, 1, 1, 0));
                visualWalls.remove(doorVisual);
            }
            logger.info("Doors opened and collision removed.");
        }
    }

   /* private void windowSetting(Player player) {
        if (player.getRole() == Role.GOAT) {
            collisionWalls.removeAll(windowCollisions);
        }
    }*/


    /**
     * Gets all visual elements including walls and special elements.
     * @return a list containing the elements
     */
    public List<Rectangle> getVisualWalls() {
        List<Rectangle> allElements = new ArrayList<>(visualWalls);
        allElements.addAll(terminalList);
        allElements.addAll(IgoatStationList);
        return allElements;
    }
    
    /**
     * Gets the list of collision wall objects.
     * @return the list of walls
     */
    public List<Wall> getCollisionWalls() {
        return collisionWalls;
    }

    /**
     * Gets the list of window collision objects.
     * @return the list of window collision boxes
     */
    public List<Wall> getWindowCollisions() {
        return windowCollisions;
    }
    
    /**
     * Gets the width of the map.
     * @return map width
     */
    public int getWidth() {
        return MAP_WIDTH;
    }
    
    /**
     * Gets the height of the map.
     * @return map width
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
        terminalList.clear();
        IgoatStationList.clear();
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

    public void addDecorItem(Decoration decor) {
        if (!noVisuals) {
            decorItems.add(decor.createImageView());
        }

        Wall wall = decor.createWallIfNeeded();
        if (wall != null) {
            collisionWalls.add(wall);
        }
    }

    public List<Terminal> getTerminalList(){
        return terminalList;
    }

    public List<ImageView> getDecorItems() {
        return decorItems;
    }

    public List<IgoatStation> getStationList() { return IgoatStationList; }

}