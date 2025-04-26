package igoat.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SceneDecoration {
    public static List<Decoration> getDecorList() {
        List<Decoration> decorList = new ArrayList<>();

        // Outer top wall 1
        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/wall.png")).toExternalForm(),
            20, -44, 110, 64,
            true,
            0, 0, 110, 64
        ));

        // Outer top wall 2
        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/wall.png")).toExternalForm(),
            150, -44, 500, 64,
            true,
            0, 0, 500, 64
        ));

        // Outer top wall 3
        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/wall.png")).toExternalForm(),
            670, -44, 230, 64,
            true,
            0, 0, 230, 64
        ));

        // Outer top wall 4
        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/wall.png")).toExternalForm(),
            920, -44, 250, 64,
            true,
            0, 0, 250, 64
        ));

        // Outer top wall 5
        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/wall.png")).toExternalForm(),
            1188, -44, 292, 64,
            true,
            0, 0, 292, 64
        ));

        // Room 1
        // bottom Wall
        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/wall.png")).toExternalForm(),
            1188, 300, 292, 64,
            true,
            0, 0, 292, 64
        ));

        // left Room Decoration 1
        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/table2.png")).toExternalForm(),
            1101, 0, 64, 64,
            true,
            0, 0, 64, 64
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/table2.png")).toExternalForm(),
            1039, 0, 64, 64,
            true,
            0, 0, 64, 64
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/printer.png")).toExternalForm(),
            1043, -20, 64, 64,
            true,
            0, 0, 64, 64
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/printer.png")).toExternalForm(),
            1105, -20, 64, 64,
            true,
            0, 0, 64, 64
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/closet_updown.png")).toExternalForm(),
            1135, 210, 64, 64,
            true,
            0, 0, 32, 64
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/closet_updown.png")).toExternalForm(),
            1135, 247, 64, 64,
            true,
            0, 0, 32, 64
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/closet_updown.png")).toExternalForm(),
            1135, 284, 64, 64,
            true,
            0, 0, 32, 64
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/wall_cable.png")).toExternalForm(),
            970, -30, 64, 64,
            false,
            0, 0, 64, 64
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/glass_cylinder.png")).toExternalForm(),
            930, -10, 96, 96,
            true,
            0, 0, 46, 96
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/glass_cylinder_red.png")).toExternalForm(),
            930, 30, 96, 96,
            true,
            0, 0, 46, 96
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/glass_cylinder_green.png")).toExternalForm(),
            930, 70, 96, 96,
            true,
            0, 0, 46, 96
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/glass_cylinder.png")).toExternalForm(),
            930, 110, 96, 96,
            true,
            0, 0, 46, 96
        ));

        // right Room Decoration 1
        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/wall_cable.png")).toExternalForm(),
            1190, 310, 64, 64,
            false,
            0, 0, 64, 64
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/paper.png")).toExternalForm(),
            1400, 200, 64, 64,
            false,
            0, 0, 64, 64
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/paper.png")).toExternalForm(),
            1215, 100, 64, 64,
            false,
            0, 0, 64, 64
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/paper.png")).toExternalForm(),
            1420, 80, 64, 64,
            false,
            0, 0, 64, 64
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/closet.png")).toExternalForm(),
            1300, -10, 64, 64,
            true,
            0, 0, 64, 64
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/shelf_with_objects.png")).toExternalForm(),
            1237, -10, 64, 64,
            true,
            0, 0, 64, 64
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/potions_in_rack.png")).toExternalForm(),
            1235, -50, 64, 64,
            false,
            0, 0, 64, 64
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/redlight.png")).toExternalForm(),
            1200, -50, 64, 64,
            false,
            0, 0, 64, 64
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/plant.png")).toExternalForm(),
            1420, -20, 64, 64,
            true,
            20, 0, 44, 64
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/circle_chair.png")).toExternalForm(),
            1280, 110, 64, 64,
            true,
            15, 20, 29, 54
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/circle_chair.png")).toExternalForm(),
            1346, 110, 64, 64,
            true,
            15, 20, 29, 54
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/table_dirty.png")).toExternalForm(),
            1280, 150, 64, 64,
            true,
            0, 0, 64, 64
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/table_dirty.png")).toExternalForm(),
            1343, 150, 64, 64,
            true,
            0, 0, 64, 64
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/centrifuge.png")).toExternalForm(),
            1280, 130, 64, 64,
            false,
            0, 0, 32, 32
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/potions.png")).toExternalForm(),
            1340, 130, 64, 64,
            false,
            0, 0, 32, 32
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/circle_chair.png")).toExternalForm(),
            1280, 175, 64, 64,
            true,
            15, 0, 29, 54
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/circle_chair.png")).toExternalForm(),
            1346, 175, 64, 64,
            true,
            15, 0, 29, 54
        ));

        // Room 2
        // top Wall
        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/wall.png")).toExternalForm(),
            1030, 550, 158, 64,
            true,
            0, 0, 158, 64
        ));

        // bottom Wall
        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/wall.png")).toExternalForm(),
            1030, 700, 138, 64,
            true,
            0, 0, 138, 64
        ));

        // left Room Decoration 2
        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/plant.png")).toExternalForm(),
            1020, 580, 64, 64,
            true,
            0, 0, 24, 64
        ));

        // right Room Decoration 2
        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/lab_coat.png")).toExternalForm(),
            1450, 310, 64, 64,
            false,
            0, 0, 292, 64
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/lab_coat.png")).toExternalForm(),
            1430, 310, 64, 64,
            false,
            0, 0, 292, 64
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/lab_coat.png")).toExternalForm(),
            1410, 310, 64, 64,
            false,
            0, 0, 292, 64
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/lab_coat.png")).toExternalForm(),
            1390, 310, 64, 64,
            false,
            0, 0, 292, 64
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/lab_coat.png")).toExternalForm(),
            1370, 310, 64, 64,
            false,
            0, 0, 292, 64
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/locker.png")).toExternalForm(),
            1330, 310, 64, 64,
            true,
            0, 0, 28, 64
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/locker.png")).toExternalForm(),
            1303, 310, 64, 64,
            true,
            0, 0, 28, 64
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/locker.png")).toExternalForm(),
            1276, 310, 64, 64,
            true,
            0, 0, 28, 64
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/locker.png")).toExternalForm(),
            1249, 310, 64, 64,
            true,
            0, 0, 28, 64
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/locker.png")).toExternalForm(),
            1222, 310, 64, 64,
            true,
            0, 0, 28, 64
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/bench_side.png")).toExternalForm(),
            1270, 450, 64, 64,
            true,
            0, 0, 64, 34
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/bench_side.png")).toExternalForm(),
            1333, 450, 64, 64,
            true,
            0, 0, 64, 34
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/bench.png")).toExternalForm(),
            1350, 700, 64, 64,
            true,
            0, 0, 32, 64
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/plant.png")).toExternalForm(),
            1335, 735, 64, 64,
            true,
            20, 0, 22, 64
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/bench.png")).toExternalForm(),
            1350, 800, 64, 64,
            true,
            0, 0, 32, 64
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/plant.png")).toExternalForm(),
            1335, 835, 64, 64,
            true,
            20, 0, 22, 64
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/bench.png")).toExternalForm(),
            1350, 900, 64, 64,
            true,
            0, 0, 32, 60
        ));
        // Table group 1
        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/table3.png")).toExternalForm(),
            1230, 1090, 64, 64,
            true,
            9, 8, 42, 36
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/table3.png")).toExternalForm(),
            1273, 1090, 64, 64,
            true,
            9, 8, 42, 36
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/table3.png")).toExternalForm(),
            1230, 1115, 64, 64,
            true,
            9, 8, 42, 36
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/table3.png")).toExternalForm(),
            1273, 1115, 64, 64,
            true,
            9, 8, 42, 36
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/book2.png")).toExternalForm(),
            1245, 1098, 32, 32,
            false,
            9, 8, 42, 36
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/book.png")).toExternalForm(),
            1280, 1110, 32, 32,
            false,
            9, 8, 42, 36
        ));

        // Table group 2
        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/table3.png")).toExternalForm(),
            1340, 1190, 64, 64,
            true,
            9, 8, 42, 36
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/table3.png")).toExternalForm(),
            1383, 1190, 64, 64,
            true,
            9, 8, 42, 36
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/table3.png")).toExternalForm(),
            1340, 1215, 64, 64,
            true,
            9, 8, 42, 36
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/table3.png")).toExternalForm(),
            1383, 1215, 64, 64,
            true,
            9, 8, 42, 36
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/book_closed.png")).toExternalForm(),
            1398, 1195, 32, 32,
            false,
            9, 8, 42, 36
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/redlight.png")).toExternalForm(),
            1100, 540, 64, 64,
            false,
            0, 0, 44, 64
        ));

        // Room 3
        // top left Wall
        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/wall.png")).toExternalForm(),
            800, 550, 50, 64,
            true,
            0, 0, 50, 64
        ));

        // top right Wall
        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/wall.png")).toExternalForm(),
            900, 550, 110, 64,
            true,
            0, 0, 110, 64
        ));

        // middle Wall
        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/wall.png")).toExternalForm(),
            1010, 850, 178, 64,
            true,
            0, 0, 178, 64
        ));

        // bottom Wall
        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/wall.png")).toExternalForm(),
            750, 1050, 438, 64,
            true,
            0, 0, 438, 64
        ));

        // Decoration 3
        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/paper.png")).toExternalForm(),
            1060, 920, 64, 64,
            false,
            0, 0, 64, 64
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/note.png")).toExternalForm(),
            1000, 940, 64, 64,
            false,
            0, 0, 64, 64
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/wall_cable.png")).toExternalForm(),
            850, 1060, 64, 64,
            false,
            0, 0, 64, 64
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/whiteboard.png")).toExternalForm(),
            1000, 1060, 64, 64,
            false,
            0, 0, 64, 64
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/note.png")).toExternalForm(),
            880, 700, 64, 64,
            false,
            0, 0, 64, 64
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/paper.png")).toExternalForm(),
            850, 600, 64, 64,
            false,
            0, 0, 64, 64
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/paper.png")).toExternalForm(),
            900, 800, 64, 64,
            false,
            0, 0, 64, 64
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/redlight.png")).toExternalForm(),
            810, 540, 64, 64,
            false,
            0, 0, 64, 64
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/display_with_activity.png")).toExternalForm(),
            900, 540, 64, 64,
            false,
            0, 0, 64, 64
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/plant.png")).toExternalForm(),
            960, 585, 64, 64,
            true,
            20, 0, 44, 64
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/closet_updown.png")).toExternalForm(),
            976, 650, 64, 64,
            true,
            0, 0, 64, 64
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/closet_updown.png")).toExternalForm(),
            976, 687, 64, 64,
            true,
            0, 0, 64, 64
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/closet_updown.png")).toExternalForm(),
            976, 724, 64, 64,
            true,
            0, 0, 64, 64
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/closet_updown.png")).toExternalForm(),
            976, 761, 64, 64,
            true,
            0, 0, 64, 64
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/closet_updown.png")).toExternalForm(),
            976, 798, 64, 64,
            true,
            0, 0, 64, 64
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/plant2.png")).toExternalForm(),
            960, 827, 64, 64,
            true,
            20, 0, 44, 64
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/table_dirty_updown.png")).toExternalForm(),
            800, 700, 70, 64,
            true,
            0, 0, 48, 64
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/table_dirty_updown.png")).toExternalForm(),
            800, 748, 70, 64,
            true,
            0, 0, 48, 64
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/table_dirty_updown.png")).toExternalForm(),
            800, 796, 70, 64,
            true,
            0, 0, 48, 64
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/table_dirty_updown.png")).toExternalForm(),
            800, 844, 70, 64,
            true,
            0, 0, 48, 64
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/table_dirty_updown.png")).toExternalForm(),
            800, 892, 70, 64,
            true,
            0, 0, 48, 64
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/trashcan.png")).toExternalForm(),
            790, 930, 64, 64,
            true,
            0, 0, 37, 52
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/microscope.png")).toExternalForm(),
            800, 680, 50, 64,
            false,
            0, 0, 50, 64
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/microscope.png")).toExternalForm(),
            800, 728, 50, 64,
            false,
            0, 0, 50, 64
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/microscope.png")).toExternalForm(),
            800, 776, 50, 64,
            false,
            0, 0, 50, 64
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/potions_in_rack.png")).toExternalForm(),
            800, 824, 50, 64,
            false,
            0, 0, 50, 64
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/circle_chair.png")).toExternalForm(),
            840, 700, 64, 64,
            true,
            0, 20, 46, 34
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/bureau_chair.png")).toExternalForm(),
            835, 748, 64, 64,
            true,
            0, 0, 44, 44
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/circle_chair.png")).toExternalForm(),
            840, 796, 64, 64,
            true,
            0, 20, 46, 34
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/bureau_chair.png")).toExternalForm(),
            835, 844, 64, 64,
            true,
            0, 0, 44, 44
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/time_billboard.png")).toExternalForm(),
            1060, 860, 64, 64,
            false,
            0, 0, 64, 64
        ));

        // Room 4
        // left Wall
        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/wall.png")).toExternalForm(),
            820, 1300, 130, 64,
            true,
            0, 0, 130, 64
        ));

        // middle Wall
        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/wall.png")).toExternalForm(),
            1000, 1300, 100, 64,
            true,
            0, 0, 100, 64
        ));

        // right Wall
        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/wall.png")).toExternalForm(),
            1120, 1300, 360, 64,
            true,
            0, 0, 360, 64
        ));

        // Left Room Decoration 4
        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/display_with_activity.png")).toExternalForm(),
            900, 1290, 64, 64,
            false,
            0, 0, 64, 64
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/redlight.png")).toExternalForm(),
            870, 1290, 64, 64,
            false,
            0, 0, 64, 64
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/closet_updown.png")).toExternalForm(),
            820, 1370, 64, 64,
            true,
            0, 0, 32, 64
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/closet_updown.png")).toExternalForm(),
            820, 1407, 64, 64,
            true,
            0, 0, 32, 64
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/closet_updown.png")).toExternalForm(),
            820, 1444, 64, 64,
            true,
            0, 0, 32, 64
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/bench.png")).toExternalForm(),
            1068, 1470, 64, 64,
            true,
            0, 0, 32, 60
        ));

        // Right Room Decoration 4
        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/paper.png")).toExternalForm(),
            1160, 1400, 64, 64,
            false,
            0, 0, 64, 64
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/paper.png")).toExternalForm(),
            1260, 1370, 64, 64,
            false,
            0, 0, 64, 64
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/paper.png")).toExternalForm(),
            1400, 1480, 64, 64,
            false,
            0, 0, 64, 64
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/plant.png")).toExternalForm(),
            1110, 1330, 64, 64,
            true,
            0, 0, 64, 64
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/drawer.png")).toExternalForm(),
            1165, 1330, 64, 64,
            true,
            0, 0, 64, 62
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/drawer.png")).toExternalForm(),
            1228, 1330, 64, 64,
            true,
            0, 0, 64, 62
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/redlight.png")).toExternalForm(),
            1300, 1290, 64, 64,
            false,
            0, 0, 64, 64
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/shelf_with_objects.png")).toExternalForm(),
            1335, 1330, 64, 64,
            true,
            0, 0, 64, 62
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/shelf.png")).toExternalForm(),
            1398, 1330, 64, 64,
            true,
            0, 0, 64, 62
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/table.png")).toExternalForm(),
            1270, 1450, 64, 64,
            true,
            0, 0, 64, 62
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/bureau_chair.png")).toExternalForm(),
            1270, 1480, 64, 64,
            true,
            20, 0, 24, 44
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/circle_chair.png")).toExternalForm(),
            1350, 1410, 64, 64,
            true,
            16, 22, 25, 34
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/table.png")).toExternalForm(),
            1333, 1450, 64, 64,
            true,
            0, 0, 64, 62
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/jar.png")).toExternalForm(),
            1260, 1420, 64, 64,
            false,
            0, 0, 64, 62
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/jar.png")).toExternalForm(),
            1290, 1425, 64, 64,
            false,
            0, 0, 64, 62
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/potions.png")).toExternalForm(),
            1333, 1430, 64, 64,
            false,
            0, 0, 64, 62
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/trashcan.png")).toExternalForm(),
            1230, 1450, 64, 64,
            true,
            20, 28, 37, 24
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/bench.png")).toExternalForm(),
            1120, 1470, 64, 64,
            true,
            0, 0, 32, 60
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/plant.png")).toExternalForm(),
            1425, 1510, 64, 64,
            true,
            20, 0, 64, 62
        ));

        // Room 5
        // top wall
        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/wall.png")).toExternalForm(),
            20, 600, 260, 64,
            true,
            0, 0, 260, 64
        ));

        // middle left Wall
        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/wall.png")).toExternalForm(),
            20, 1000, 180, 64,
            true,
            0, 0, 180, 64
        ));

        // middle right Wall
        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/wall.png")).toExternalForm(),
            220, 1000, 80, 64,
            true,
            0, 0, 80, 64
        ));

        // bottom Wall 1
        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/wall.png")).toExternalForm(),
            200, 1300, 50, 64,
            true,
            0, 0, 50, 64
        ));
        // bottom Wall 2
        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/wall.png")).toExternalForm(),
            300, 1300, 200, 64,
            true,
            0, 0, 200, 64
        ));
        // bottom Wall 3
        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/wall.png")).toExternalForm(),
            520, 1300, 130, 64,
            true,
            0, 0, 130, 64
        ));

        // bottom Wall 4
        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/wall.png")).toExternalForm(),
            700, 1300, 100, 64,
            true,
            0, 0, 130, 64
        ));

        // top Room Decoration 5
        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/plant2.png")).toExternalForm(),
            15, 620, 64, 64,
            true,
            0, 0, 28, 64
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/locker.png")).toExternalForm(),
            70, 620, 64, 64,
            true,
            0, 0, 28, 64
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/locker.png")).toExternalForm(),
            97, 620, 64, 64,
            true,
            0, 0, 28, 64
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/locker.png")).toExternalForm(),
            124, 620, 64, 64,
            true,
            0, 0, 28, 64
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/locker.png")).toExternalForm(),
            151, 620, 64, 64,
            true,
            0, 0, 28, 64
        ));

        // table group 1
        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/table3.png")).toExternalForm(),
            80, 725, 64, 64,
            true,
            9, 8, 42, 36
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/table3.png")).toExternalForm(),
            123, 725, 64, 64,
            true,
            9, 8, 42, 36
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/table3.png")).toExternalForm(),
            166, 725, 64, 64,
            true,
            9, 8, 42, 36
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/table3.png")).toExternalForm(),
            80, 750, 64, 64,
            true,
            9, 8, 42, 36
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/table3.png")).toExternalForm(),
            123, 750, 64, 64,
            true,
            9, 8, 42, 36
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/table3.png")).toExternalForm(),
            166, 750, 64, 64,
            true,
            9, 8, 42, 36
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/book.png")).toExternalForm(),
            95, 733, 32, 32,
            false,
            9, 8, 42, 36
        ));

        // table group 2
        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/table3.png")).toExternalForm(),
            200, 850, 64, 64,
            true,
            9, 8, 42, 36
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/table3.png")).toExternalForm(),
            243, 850, 64, 64,
            true,
            9, 8, 42, 36
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/table3.png")).toExternalForm(),
            286, 850, 64, 64,
            true,
            9, 8, 42, 36
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/table3.png")).toExternalForm(),
            200, 875, 64, 64,
            true,
            9, 8, 42, 36
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/table3.png")).toExternalForm(),
            243, 875, 64, 64,
            true,
            9, 8, 42, 36
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/table3.png")).toExternalForm(),
            286, 875, 64, 64,
            true,
            9, 8, 42, 36
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/book2.png")).toExternalForm(),
            301, 859, 32, 32,
            false,
            9, 8, 42, 36
        ));

        // Right Room Decoration 5
        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/display_with_activity.png")).toExternalForm(),
            570, 1290, 64, 64,
            false,
            0, 0, 64, 64
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/redlight.png")).toExternalForm(),
            750, 1290, 64, 64,
            false,
            0, 0, 64, 64
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/table_dirty_updown.png")).toExternalForm(),
            520, 1370, 64, 64,
            true,
            10, 10, 32, 44
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/table_dirty_updown.png")).toExternalForm(),
            520, 1418, 64, 64,
            true,
            10, 10, 32, 44
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/table_dirty_updown.png")).toExternalForm(),
            520, 1466, 64, 64,
            true,
            10, 10, 32, 44
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/table_dirty_updown.png")).toExternalForm(),
            520, 1514, 64, 64,
            true,
            10, 10, 32, 44
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/igoat_idle.png")).toExternalForm(),
            510, 1370, 64, 64,
            true,
            10, 10, 47, 44
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/igoat_idle.png")).toExternalForm(),
            510, 1400, 64, 64,
            true,
            10, 10, 47, 44
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/igoat_idle.png")).toExternalForm(),
            510, 1430, 64, 64,
            true,
            10, 10, 47, 44
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/igoat_idle.png")).toExternalForm(),
            510, 1460, 64, 64,
            true,
            10, 10, 47, 44
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/igoat_idle.png")).toExternalForm(),
            510, 1490, 64, 64,
            true,
            10, 10, 47, 44
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/igoat_station.png")).toExternalForm(),
            600, 1418, 64, 64,
            true,
            0, 0, 42, 64
        ));

        // Left Room Decoration 5
        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/whiteboard_with_stuff.png")).toExternalForm(),
            80, 1010, 64, 64,
            false,
            0, 0, 64, 64
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/paper.png")).toExternalForm(),
            90, 1330, 64, 64,
            false,
            0, 0, 64, 64
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/paper.png")).toExternalForm(),
            200, 1410, 64, 64,
            false,
            0, 0, 64, 64
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/display_with_activity.png")).toExternalForm(),
            200, 1285, 50, 64,
            false,
            0, 0, 50, 64
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/glass_cylinder.png")).toExternalForm(),
            25, 1040, 96, 96,
            true,
            0, 0, 46, 96
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/glass_cylinder_red.png")).toExternalForm(),
            25, 1090, 96, 96,
            true,
            0, 0, 46, 96
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/glass_cylinder_green.png")).toExternalForm(),
            25, 1130, 96, 96,
            true,
            0, 0, 46, 96
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/glass_cylinder.png")).toExternalForm(),
            25, 1170, 96, 96,
            true,
            0, 0, 46, 96
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/glass_cylinder_red.png")).toExternalForm(),
            25, 1210, 96, 96,
            true,
            0, 0, 46, 96
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/glass_cylinder_green.png")).toExternalForm(),
            150, 1010, 96, 96,
            true,
            0, 0, 46, 96
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/glass_cylinder.png")).toExternalForm(),
            150, 1060, 96, 96,
            true,
            0, 0, 46, 96
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/circle_chair.png")).toExternalForm(),
            20, 1390, 64, 64,
            true,
            0, 20, 44, 62
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/circle_chair.png")).toExternalForm(),
            80, 1390, 64, 64,
            true,
            0, 20, 44, 62
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/table.png")).toExternalForm(),
            20, 1420, 64, 64,
            true,
            0, 0, 64, 64
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/table.png")).toExternalForm(),
            83, 1420, 64, 64,
            true,
            0, 0, 64, 64
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/table.png")).toExternalForm(),
            20, 1456, 64, 64,
            true,
            0, 0, 64, 64
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/table.png")).toExternalForm(),
            83, 1456, 64, 64,
            true,
            0, 0, 64, 64
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/circle_chair.png")).toExternalForm(),
            20, 1480, 64, 64,
            true,
            0, 0, 44, 60
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/circle_chair.png")).toExternalForm(),
            80, 1480, 64, 64,
            true,
            0, 0, 44, 60
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/laptop.png")).toExternalForm(),
            20, 1400, 64, 64,
            false,
            0, 0, 44, 60
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/laptop.png")).toExternalForm(),
            20, 1430, 64, 64,
            false,
            0, 0, 44, 60
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/laptop.png")).toExternalForm(),
            80, 1400, 64, 64,
            false,
            0, 0, 44, 60
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/laptop.png")).toExternalForm(),
            80, 1430, 64, 64,
            false,
            0, 0, 44, 60
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/trashcan.png")).toExternalForm(),
            130, 1430, 64, 64,
            true,
            20, 26, 17, 26
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/shelf_with_objects2.png")).toExternalForm(),
            330, 1330, 64, 64,
            true,
            0, 0, 64, 62
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/shelf.png")).toExternalForm(),
            393, 1330, 64, 64,
            true,
            0, 0, 64, 62
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/plant.png")).toExternalForm(),
            445, 1330, 64, 64,
            true,
            0, 0, 32, 62
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/table_circle.png")).toExternalForm(),
            300, 1456, 64, 64,
            true,
            10, 10, 45, 45
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/laptop.png")).toExternalForm(),
            300, 1430, 64, 64,
            false,
            10, 20, 54, 44
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/jar.png")).toExternalForm(),
            330, 1460, 32, 32,
            false,
            10, 20, 54, 44
        ));

        // Room 6
        // Wall
        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/wall.png")).toExternalForm(),
            20, 200, 130, 64,
            true,
            0, 0, 130, 64
        ));

        // iGoat station Decoration 6
        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/table_dirty_updown.png")).toExternalForm(),
            20, 10, 64, 64,
            true,
            0, 0, 32, 44
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/table_dirty_updown.png")).toExternalForm(),
            20, 58, 64, 64,
            true,
            0, 0, 32, 44
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/igoat_idle.png")).toExternalForm(),
            10, -30, 64, 64,
            true,
            10, 10, 47, 44
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/igoat_idle.png")).toExternalForm(),
            10, -10, 64, 64,
            true,
            10, 10, 47, 44
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/igoat_idle.png")).toExternalForm(),
            10, 10, 64, 64,
            true,
            10, 10, 47, 44
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/igoat_idle.png")).toExternalForm(),
            10, 30, 64, 64,
            true,
            10, 10, 47, 44
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/igoat_station.png")).toExternalForm(),
            20, 90, 64, 64,
            true,
            0, 0, 42, 64
        ));

        // Decoration 6
        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/plant.png")).toExternalForm(),
            20, 230, 64, 64,
            true,
            20, 0, 20, 62
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/wall_cable.png")).toExternalForm(),
            250, -32, 64, 64,
            false,
            20, 0, 20, 62
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/electricbox.png")).toExternalForm(),
            180, -32, 64, 64,
            false,
            20, 0, 20, 62
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/whiteboard_with_stuff.png")).toExternalForm(),
            320, -32, 64, 64,
            false,
            20, 0, 20, 62
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/wall_cable.png")).toExternalForm(),
            450, -32, 64, 64,
            false,
            20, 0, 20, 62
        ));

        // Room 7
        // left top Wall
        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/wall.png")).toExternalForm(),
            320, 150, 180, 64,
            true,
            0, 0, 180, 64
        ));

        // middle top Wall
        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/wall.png")).toExternalForm(),
            550, 150, 200, 64,
            true,
            0, 0, 200, 64
        ));

        // right top Wall
        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/wall.png")).toExternalForm(),
            800, 150, 100, 64,
            true,
            0, 0, 100, 64
        ));

        // left middle Wall
        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/wall.png")).toExternalForm(),
            220, 300, 280, 64,
            true,
            0, 0, 280, 64
        ));
        // right middle Wall
        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/wall.png")).toExternalForm(),
            550, 300, 350, 64,
            true,
            0, 0, 350, 64
        ));

        // bottom Wall 2
        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/wall.png")).toExternalForm(),
            330, 600, 70, 64,
            true,
            0, 0, 70, 64
        ));

        // bottom Wall 3
        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/wall.png")).toExternalForm(),
            420, 550, 180, 64,
            true,
            0, 0, 180, 64
        ));

        // bottom Wall 4
        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/wall.png")).toExternalForm(),
            620, 550, 80, 64,
            true,
            0, 0, 80, 64
        ));

        // bottom Wall 5
        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/wall.png")).toExternalForm(),
            750, 550, 30, 64,
            true,
            0, 0, 30, 64
        ));

        // Guard Room Decoration 7
        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/plant.png")).toExternalForm(),
            845, -20, 64, 64,
            true,
            20, 0, 44, 63
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/table.png")).toExternalForm(),
            700, 0, 64, 64,
            true,
            0, 0, 64, 64
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/screen.png")).toExternalForm(),
            700, -30, 64, 64,
            false,
            0, 0, 32, 64
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/keyboard_mouse.png")).toExternalForm(),
            720, 10, 32, 32,
            false,
            0, 0, 32, 64
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/bureau_chair.png")).toExternalForm(),
            690, 20, 64, 64,
            true,
            32, 0, 32, 44
        ));

        // top Decoration 7
        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/wall_cable.png")).toExternalForm(),
            620, 160, 64, 64,
            false,
            0, 0, 64, 64
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/bench.png")).toExternalForm(),
            320, 220, 64, 64,
            true,
            0, 0, 32, 64
        ));

        // middle Room Decoration 7
        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/whiteboard.png")).toExternalForm(),
            340, 310, 64, 64,
            false,
            0, 0, 64, 64
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/display_with_activity.png")).toExternalForm(),
            550, 290, 64, 64,
            false,
            0, 0, 64, 64
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/redlight.png")).toExternalForm(),
            850, 290, 64, 64,
            false,
            0, 0, 64, 64
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/plant.png")).toExternalForm(),
            210, 330, 64, 64,
            true,
            0, 0, 44, 63
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/closet_updown.png")).toExternalForm(),
            220, 400, 64, 64,
            true,
            0, 0, 32, 64
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/closet_updown.png")).toExternalForm(),
            220, 437, 64, 64,
            true,
            0, 0, 32, 63
        ));

        // bottom Room Decoration 7
        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/shelf_with_objects2.png")).toExternalForm(),
            620, 580, 64, 64,
            true,
            0, 0, 64, 64
        ));

        // Room 8
        // bottom Wall
        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/wall.png")).toExternalForm(),
            400, 1050, 300, 64,
            true,
            0, 0, 300, 64
        ));

        // top Wall
        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/wall.png")).toExternalForm(),
            620, 700, 160, 64,
            true,
            0, 0, 160, 64
        ));

        // left Room Decoration 8
        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/paper.png")).toExternalForm(),
            480, 620, 64, 64,
            false,
            0, 0, 160, 64
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/paper.png")).toExternalForm(),
            520, 720, 64, 64,
            false,
            0, 0, 160, 64
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/paper.png")).toExternalForm(),
            470, 820, 64, 64,
            false,
            0, 0, 160, 64
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/whiteboard_with_graph.png")).toExternalForm(),
            480, 560, 64, 64,
            false,
            0, 0, 160, 64
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/bench.png")).toExternalForm(),
            420, 600, 64, 64,
            true,
            0, 0, 32, 60
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/bench.png")).toExternalForm(),
            420, 647, 64, 64,
            true,
            0, 0, 32, 60
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/bench.png")).toExternalForm(),
            420, 694, 64, 64,
            true,
            0, 0, 32, 60
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/plant.png")).toExternalForm(),
            550, 580, 64, 64,
            true,
            20, 0, 32, 64
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/closet_updown.png")).toExternalForm(),
            567, 650, 64, 64,
            true,
            0, 0, 32, 64
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/closet_updown.png")).toExternalForm(),
            567, 687, 64, 64,
            true,
            0, 0, 32, 64
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/closet_updown.png")).toExternalForm(),
            567, 870, 64, 64,
            true,
            0, 0, 32, 64
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/closet_updown.png")).toExternalForm(),
            567, 907, 64, 64,
            true,
            0, 0, 32, 64
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/closet_updown.png")).toExternalForm(),
            567, 944, 64, 64,
            true,
            0, 0, 32, 64
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/plant.png")).toExternalForm(),
            550, 980, 64, 64,
            true,
            20, 0, 32, 64
        ));

        // right Room Decoration 8
        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/wall_cable.png")).toExternalForm(),
            440, 1060, 64, 64,
            false,
            0, 0, 64, 64
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/closet.png")).toExternalForm(),
            627, 720, 64, 64,
            true,
            0, 0, 64, 64
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/drawer.png")).toExternalForm(),
            690, 720, 64, 64,
            true,
            0, 0, 64, 64
        ));

        decorList.add(new Decoration(
            Objects.requireNonNull(Sprite.class.getResource("/sprites/redlight.png")).toExternalForm(),
            750, 690, 64, 64,
            false,
            0, 0, 64, 64
        ));

        return decorList;
    }
}
