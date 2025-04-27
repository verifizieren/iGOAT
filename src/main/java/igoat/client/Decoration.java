package igoat.client;

import javafx.scene.image.ImageView;

/**
 * Represents a decorative object in the scene,
 * with optional collision properties.
 */
public class Decoration {
    public String imagePath;
    public double x, y, width, height;
    public boolean isCollidable;
    public double offsetX, offsetY, collisionWidth, collisionHeight;

    /**
     * Constructs a Decoration instance.
     *
     * @param imagePath Path to the decoration's image.
     * @param x X coordinate for the decoration.
     * @param y Y coordinate for the decoration.
     * @param width Width of the decoration.
     * @param height Height of the decoration.
     * @param isCollidable Whether the decoration has collision properties.
     * @param offsetX Horizontal offset for the collision box.
     * @param offsetY Vertical offset for the collision box.
     * @param collisionWidth Width of the collision box.
     * @param collisionHeight Height of the collision box.
     */
    public Decoration(String imagePath, double x, double y, double width, double height,
        boolean isCollidable, double offsetX, double offsetY,
        double collisionWidth, double collisionHeight) {
        this.imagePath = imagePath;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.isCollidable = isCollidable;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.collisionWidth = collisionWidth;
        this.collisionHeight = collisionHeight;
    }

    /**
     * Creates an ImageView representing this decoration.
     *
     * @return An ImageView with the appropriate image, position, and size.
     */
    public ImageView createImageView() {
        Sprite sprite = new Sprite(imagePath);
        ImageView view = new ImageView(sprite);
        view.setX(x);
        view.setY(y);
        view.setFitWidth(width);
        view.setFitHeight(height);
        return view;
    }

    /**
     * Creates a Wall object if this decoration is collidable.
     *
     * @return A Wall representing the collision area, or {@code null} if not collidable.
     */
    public Wall createWallIfNeeded() {
        if (!isCollidable) return null;
        return new Wall((int)(x + offsetX), (int)(y + offsetY), (int)collisionWidth, (int)collisionHeight);
    }

}
