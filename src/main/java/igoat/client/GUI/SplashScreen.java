package igoat.client.GUI;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

/**
 * Splash screen that shows a GIF animation before transitioning to the main menu.
 */
public class SplashScreen extends Application {

    private static final double SPLASH_DURATION = 3.0;
    private static final String GIF_PATH = "/sprites/splash.gif";

    // parameters for console launch
    private static String host = null;
    private static int port = 0;
    private static String username = null;

    public static void configure(String host, int port, String username) {
        SplashScreen.host = host;
        SplashScreen.port = port;
        SplashScreen.username = username;
    }

    @Override
    public void start(Stage primaryStage) {
        Stage splashStage = new Stage(StageStyle.UNDECORATED);

        try {
            Image splashImage = new Image(getClass().getResourceAsStream(GIF_PATH));
            ImageView imageView = new ImageView(splashImage);

            imageView.setFitWidth(600);
            imageView.setFitHeight(400);
            imageView.setPreserveRatio(true);

            StackPane root = new StackPane(imageView);
            root.setStyle("-fx-background-color: black;");
            root.setAlignment(Pos.CENTER);

            Scene scene = new Scene(root);
            splashStage.setScene(scene);

            splashStage.centerOnScreen();

            splashStage.show();

            FadeTransition fadeOut = new FadeTransition(Duration.seconds(0.5), root);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);

            PauseTransition delay = new PauseTransition(Duration.seconds(SPLASH_DURATION));
            delay.setOnFinished(event -> {
                fadeOut.play();
                fadeOut.setOnFinished(e -> {
                    splashStage.close();

                    Stage mainStage = new Stage();
                    try {
                        MainMenuGUI mainMenu = new MainMenuGUI();
                        mainMenu.start(mainStage);
                        if (SplashScreen.host != null) {
                            mainMenu.join(host, port, username);
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                });
            });
            delay.play();

        } catch (Exception e) {
            e.printStackTrace();
            Stage mainStage = new Stage();
            try {
                MainMenuGUI mainMenu = new MainMenuGUI();
                mainMenu.start(mainStage);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
} 