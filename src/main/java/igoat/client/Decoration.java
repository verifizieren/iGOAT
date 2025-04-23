package igoat.client;

import javafx.scene.image.ImageView;

public class Decoration {
    public String imagePath;
    public double x, y, width, height;
    public boolean isCollidable;
    public double offsetX, offsetY, collisionWidth, collisionHeight;

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

    public ImageView createImageView() {
        Sprite sprite = new Sprite(imagePath);
        ImageView view = new ImageView(sprite);
        view.setX(x);
        view.setY(y);
        view.setFitWidth(width);
        view.setFitHeight(height);
        return view;
    }

    public Wall createWallIfNeeded() {
        if (!isCollidable) return null;
        return new Wall((int)(x + offsetX), (int)(y + offsetY), (int)collisionWidth, (int)collisionHeight);
    }

}
