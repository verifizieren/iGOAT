package igoat.client;

/**
 * Represents a Player with a position and size, which can move within a gamemap
 * while avoiding collisions with walls.
 */
class Player{
    int x;
    int y;
    int width;
    int height;

    Player(int x, int y, int width, int height){
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }
    public void move(int x, int y, Wall wall){
        int newX = this.x + x;
        int newY = this.y + y;

        if(!collidesWithWall(newX, newY, wall)){
            x = newX;
            y = newY;
        }else{
            System.out.println("Kollision mit Wand: blockiert");
        }

    }
    private boolean collidesWithWall(int newX, int newY, Wall wall){
        return newX < wall.x + wall.width &&
               newX + width > wall.x &&
               newY < wall.y + wall.height &&
               newY + height > wall.y;
    }
}

/**
 * Represents a rectangular wall within a given coordinate system.
 *
 * This class allows the representation of a wall's position and dimensions
 * through its properties, including x and y coordinates, as well as its width
 * and height.
 */
class Wall{
    int x;
    int y;
    int width;
    int height;

    Wall(int x, int y, int width, int height){
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }



}