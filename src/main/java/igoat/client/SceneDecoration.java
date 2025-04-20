package igoat.client;

import java.util.ArrayList;
import java.util.List;

public class SceneDecoration {
    public static List<Decoration> getDecorList() {
        List<Decoration> decorList = new ArrayList<>();

        // Top wall
        decorList.add(new Decoration(
            Sprite.class.getResource("/sprites/Wall.png").toExternalForm(),
            20, -44, 1460, 64,
            true,
            0, 0, 64, 64
        ));

        // Room 1
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

        return decorList;
    }
}
