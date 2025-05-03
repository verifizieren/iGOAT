package igoat.client;

import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.geometry.Rectangle2D;
import java.awt.MouseInfo;
import java.awt.Point;

public class ScreenUtil {
    /**
     * Moves the given Stage to the screen where the mouse cursor currently is. Optionally centers it.
     * @param stage The JavaFX Stage to move
     * @param width The width of the window (for centering)
     * @param height The height of the window (for centering)
     */
    public static void moveStageToCursorScreen(Stage stage, double width, double height) {
        try {
            Point mouse = MouseInfo.getPointerInfo().getLocation();
            for (Screen screen : Screen.getScreens()) {
                Rectangle2D bounds = screen.getBounds();
                if (bounds.contains(mouse.getX(), mouse.getY())) {
                    double x = bounds.getMinX() + (bounds.getWidth() - width) / 2;
                    double y = bounds.getMinY() + (bounds.getHeight() - height) / 2;
                    stage.setX(x);
                    stage.setY(y);
                    return;
                }
            }
            Rectangle2D bounds = Screen.getPrimary().getBounds();
            double x = bounds.getMinX() + (bounds.getWidth() - width) / 2;
            double y = bounds.getMinY() + (bounds.getHeight() - height) / 2;
            stage.setX(x);
            stage.setY(y);
        } catch (Exception e) {
        }
    }
}
