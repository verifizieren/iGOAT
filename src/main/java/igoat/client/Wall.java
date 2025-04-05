package igoat.client;

/**
 * Represents a wall in the game with position and dimensions.
 * Used for collision detection with the player.
 */
public class Wall {
    public final int x;
    public final int y;
    public final int width;
    public final int height;

    /**
     * Creates a new Wall with the specified position and dimensions.
     * @param height Wall height
     * @param width Wall width
     * @param x X-position
     * @param y Y-position
     */
    public Wall(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }
} 