package igoat.client;

import java.util.ArrayList;
import java.util.List;

public class SceneDecoration {
    public static List<Decoration> getDecorList() {
        List<Decoration> decorList = new ArrayList<>();

        // Outer Top wall
        decorList.add(new Decoration(
            Sprite.class.getResource("/sprites/wall.png").toExternalForm(),
            20, -44, 1460, 64,
            true,
            0, 0, 64, 64
        ));

        // Room 1
        // bottom Wall
        decorList.add(new Decoration(
            Sprite.class.getResource("/sprites/wall.png").toExternalForm(),
            1188, 300, 292, 64,
            true,
            0, 0, 292, 64
        ));

        // Decoration

        decorList.add(new Decoration(
            Sprite.class.getResource("/sprites/closet.png").toExternalForm(),
            1300, -10, 64, 64,
            true,
            0, 0, 64, 64
        ));

        decorList.add(new Decoration(
            Sprite.class.getResource("/sprites/shelf_with_objects.png").toExternalForm(),
            1237, -10, 64, 64,
            true,
            0, 0, 64, 64
        ));

        decorList.add(new Decoration(
            Sprite.class.getResource("/sprites/table_dirty.png").toExternalForm(),
            1300, 100, 64, 64,
            true,
            0, 0, 64, 64
        ));

        decorList.add(new Decoration(
            Sprite.class.getResource("/sprites/jar.png").toExternalForm(),
            1300, 70, 64, 64,
            false,
            0, 0, 64, 64
        ));

        // Room 2
        // top Wall
        decorList.add(new Decoration(
            Sprite.class.getResource("/sprites/wall.png").toExternalForm(),
            1030, 550, 158, 64,
            true,
            0, 0, 158, 64
        ));

        // bottom Wall
        decorList.add(new Decoration(
            Sprite.class.getResource("/sprites/wall.png").toExternalForm(),
            1030, 700, 138, 64,
            true,
            0, 0, 138, 64
        ));

        // Room 3
        // top left Wall
        decorList.add(new Decoration(
            Sprite.class.getResource("/sprites/wall.png").toExternalForm(),
            800, 550, 50, 64,
            true,
            0, 0, 50, 64
        ));

        // top right Wall
        decorList.add(new Decoration(
            Sprite.class.getResource("/sprites/wall.png").toExternalForm(),
            900, 550, 110, 64,
            true,
            0, 0, 110, 64
        ));

        // middle Wall
        decorList.add(new Decoration(
            Sprite.class.getResource("/sprites/wall.png").toExternalForm(),
            1010, 850, 178, 64,
            true,
            0, 0, 178, 64
        ));

        // bottom Wall
        decorList.add(new Decoration(
            Sprite.class.getResource("/sprites/wall.png").toExternalForm(),
            750, 1050, 438, 64,
            true,
            0, 0, 438, 64
        ));

        // Room 4
        // Wall 1
        decorList.add(new Decoration(
            Sprite.class.getResource("/sprites/wall.png").toExternalForm(),
            820, 1300, 130, 64,
            true,
            0, 0, 130, 64
        ));

        // Wall 2
        decorList.add(new Decoration(
            Sprite.class.getResource("/sprites/wall.png").toExternalForm(),
            1000, 1300, 100, 64,
            true,
            0, 0, 100, 64
        ));

        // Wall 3
        decorList.add(new Decoration(
            Sprite.class.getResource("/sprites/wall.png").toExternalForm(),
            1120, 1300, 360, 64,
            true,
            0, 0, 360, 64
        ));

        // Room 5
        // top Wall
        decorList.add(new Decoration(
            Sprite.class.getResource("/sprites/wall.png").toExternalForm(),
            20, 1000, 180, 64,
            true,
            0, 0, 180, 64
        ));

        // bottom Wall 1
        decorList.add(new Decoration(
            Sprite.class.getResource("/sprites/wall.png").toExternalForm(),
            200, 1300, 50, 64,
            true,
            0, 0, 50, 64
        ));
        // bottom Wall 2
        decorList.add(new Decoration(
            Sprite.class.getResource("/sprites/wall.png").toExternalForm(),
            300, 1300, 200, 64,
            true,
            0, 0, 200, 64
        ));
        // bottom Wall 3
        decorList.add(new Decoration(
            Sprite.class.getResource("/sprites/wall.png").toExternalForm(),
            520, 1300, 130, 64,
            true,
            0, 0, 130, 64
        ));

        // bottom Wall 4
        decorList.add(new Decoration(
            Sprite.class.getResource("/sprites/wall.png").toExternalForm(),
            700, 1300, 130, 64,
            true,
            0, 0, 130, 64
        ));

        // Room 6
        // Wall
        decorList.add(new Decoration(
            Sprite.class.getResource("/sprites/wall.png").toExternalForm(),
            20, 200, 130, 64,
            true,
            0, 0, 130, 64
        ));

        // Room 7
        // left top Wall
        decorList.add(new Decoration(
            Sprite.class.getResource("/sprites/wall.png").toExternalForm(),
            320, 150, 180, 64,
            true,
            0, 0, 180, 64
        ));

        // middle top Wall
        decorList.add(new Decoration(
            Sprite.class.getResource("/sprites/wall.png").toExternalForm(),
            550, 150, 200, 64,
            true,
            0, 0, 200, 64
        ));

        // right top Wall
        decorList.add(new Decoration(
            Sprite.class.getResource("/sprites/wall.png").toExternalForm(),
            800, 150, 100, 64,
            true,
            0, 0, 100, 64
        ));

        // left middle Wall
        decorList.add(new Decoration(
            Sprite.class.getResource("/sprites/wall.png").toExternalForm(),
            220, 300, 280, 64,
            true,
            0, 0, 280, 64
        ));
        // right middle Wall
        decorList.add(new Decoration(
            Sprite.class.getResource("/sprites/wall.png").toExternalForm(),
            550, 300, 350, 64,
            true,
            0, 0, 350, 64
        ));

        // left from door bottom Wall 1
        decorList.add(new Decoration(
            Sprite.class.getResource("/sprites/wall.png").toExternalForm(),
            20, 600, 380, 64,
            true,
            0, 0, 380, 64
        ));

        // bottom Wall 2
        decorList.add(new Decoration(
            Sprite.class.getResource("/sprites/wall.png").toExternalForm(),
            420, 550, 180, 64,
            true,
            0, 0, 180, 64
        ));

        // bottom Wall 3
        decorList.add(new Decoration(
            Sprite.class.getResource("/sprites/wall.png").toExternalForm(),
            620, 550, 80, 64,
            true,
            0, 0, 80, 64
        ));

        // bottom Wall 4
        decorList.add(new Decoration(
            Sprite.class.getResource("/sprites/wall.png").toExternalForm(),
            750, 550, 30, 64,
            true,
            0, 0, 30, 64
        ));

        // Room 8
        // bottom Wall
        decorList.add(new Decoration(
            Sprite.class.getResource("/sprites/wall.png").toExternalForm(),
            400, 1050, 300, 64,
            true,
            0, 0, 300, 64
        ));

        // top Wall
        decorList.add(new Decoration(
            Sprite.class.getResource("/sprites/wall.png").toExternalForm(),
            620, 700, 160, 64,
            true,
            0, 0, 160, 64
        ));

        return decorList;
    }
}
