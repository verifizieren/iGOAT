package igoat.client.GUI;

import animatefx.animation.FadeInDown;
import animatefx.animation.FadeOutUp;
import animatefx.animation.Shake;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.util.Duration;

/**
 * this class contains functions to generate different animated notification banners used during game
 */
public class Banner {
    /**
     * creates a new banner
     */
    public static Label terminalActivation(Pane pane) {
        Label banner = new Label("Terminal Activated!");
        banner.setStyle("-fx-background-color: rgba(0, 200, 0, 0.7); -fx-text-fill: white; -fx-font-size: 24px; -fx-padding: 10px; -fx-background-radius: 5px;");
        banner.setVisible(false);
        banner.layoutXProperty().bind(pane.widthProperty().subtract(banner.widthProperty()).divide(2));
        banner.setLayoutY(20);

        return banner;
    }

    /**
     * creates a new banner
     */
    public static Label allTerminals(Pane pane) {
        Label banner = new Label("All Terminals Activated! Exits Open!");
        banner.setStyle("-fx-background-color: rgba(0, 100, 255, 0.8); -fx-text-fill: white; -fx-font-size: 28px; -fx-padding: 15px; -fx-background-radius: 8px; -fx-border-color: white; -fx-border-width: 2; -fx-border-radius: 8;");
        banner.setVisible(false);
        banner.layoutXProperty().bind(pane.widthProperty().subtract(banner.widthProperty()).divide(2));
        banner.setLayoutY(60);

        return banner;
    }

    /**
     * creates a new banner
     */
    public static Label alreadyActive(Pane pane) {
        Label banner = new Label("Terminal X is already active!");
        banner.setStyle("-fx-background-color: rgba(255, 100, 0, 0.8); -fx-text-fill: white; -fx-font-size: 18px; -fx-padding: 8px; -fx-background-radius: 5px;");
        banner.setVisible(false);
        banner.layoutXProperty().bind(pane.widthProperty().subtract(banner.widthProperty()).divide(2));
        banner.setLayoutY(100);

        return banner;
    }

    /**
     * Plays an animation showing the banner
     * @param banner The banner to be shown
     * @param message The text to be shown on the banner
     * @param duration The duration in seconds
     */
    public static void showAnimation(Label banner, String message, double duration) {
        Platform.runLater(() -> {
            banner.setText(message);

            banner.setVisible(true);
            banner.setOpacity(1.0);
            new FadeInDown(banner).play();

            PauseTransition delay = new PauseTransition(Duration.seconds(duration));
            delay.setOnFinished(event -> {
                new FadeOutUp(banner).play();
            });
            delay.play();
        });
    }

    public static void shake(Label banner) {
        Platform.runLater(() -> {
            new Shake(banner).play();
        });
    }
}
