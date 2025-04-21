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
 * A class for animated banner to be shown during the game
 */
public class Banner {
    private Label label;
    /**
     * creates a new banner
     */
    private Banner(Label label) {
        this.label = label;
    }

    public static Banner terminalActivation(Pane pane) {
        Label banner = new Label("Terminal Activated!");
        banner.setStyle("-fx-background-color: rgba(0, 200, 0, 0.7); -fx-text-fill: white; -fx-font-size: 24px; -fx-padding: 10px; -fx-background-radius: 5px;");
        banner.setVisible(false);
        banner.layoutXProperty().bind(pane.widthProperty().subtract(banner.widthProperty()).divide(2));
        banner.setLayoutY(20);
        pane.getChildren().add(banner);

        return new Banner(banner);
    }

    /**
     * creates a new banner
     */
    public static Banner allTerminals(Pane pane) {
        Label banner = new Label("All Terminals Activated! Exits Open!");
        banner.setStyle("-fx-background-color: rgba(0, 100, 255, 0.8); -fx-text-fill: white; -fx-font-size: 28px; -fx-padding: 15px; -fx-background-radius: 8px; -fx-border-color: white; -fx-border-width: 2; -fx-border-radius: 8;");
        banner.setVisible(false);
        banner.layoutXProperty().bind(pane.widthProperty().subtract(banner.widthProperty()).divide(2));
        banner.setLayoutY(60);
        pane.getChildren().add(banner);

        return new Banner(banner);
    }

    /**
     * creates a new banner
     */
    public static Banner noActivation(Pane pane) {
        Label banner = new Label("Terminal X is already active!");
        banner.setStyle("-fx-background-color: rgba(255, 100, 0, 0.8); -fx-text-fill: white; -fx-font-size: 18px; -fx-padding: 8px; -fx-background-radius: 5px;");
        banner.setVisible(false);
        banner.layoutXProperty().bind(pane.widthProperty().subtract(banner.widthProperty()).divide(2));
        banner.setLayoutY(100);
        pane.getChildren().add(banner);

        return new Banner(banner);
    }

    public static Banner revive(Pane pane) {
        Label banner = new Label("Revived Player!");
        banner.setStyle("-fx-background-color: rgba(119,220,23,0.7); -fx-text-fill: white; -fx-font-size: 24px; -fx-padding: 10px; -fx-background-radius: 5px;");
        banner.setVisible(false);
        banner.layoutXProperty().bind(pane.widthProperty().subtract(banner.widthProperty()).divide(2));
        banner.setLayoutY(80);
        pane.getChildren().add(banner);

        return new Banner(banner);
    }

    public static Banner caught(Pane pane) {
        Label banner = new Label("Caught Player!");
        banner.setStyle("-fx-background-color: rgba(255,0,0,0.7); -fx-text-fill: white; -fx-font-size: 24px; -fx-padding: 10px; -fx-background-radius: 5px;");
        banner.setVisible(false);
        banner.layoutXProperty().bind(pane.widthProperty().subtract(banner.widthProperty()).divide(2));
        banner.setLayoutY(20);
        pane.getChildren().add(banner);

        return new Banner(banner);
    }

    /**
     * Plays an animation showing the banner
     * @param message The text to be shown on the banner
     * @param duration The duration in seconds
     */
    public void showAnimation(String message, double duration) {
        Platform.runLater(() -> {
            label.setText(message);

            label.setVisible(true);
            label.setOpacity(1.0);
            new FadeInDown(label).play();

            PauseTransition delay = new PauseTransition(Duration.seconds(duration));
            delay.setOnFinished(event -> {
                new FadeOutUp(label).play();
            });
            delay.play();
        });
    }

    public void shake() {
        Platform.runLater(() -> {
            new Shake(label).play();
        });
    }
}
