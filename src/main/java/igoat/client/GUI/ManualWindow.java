package igoat.client.GUI;

import igoat.client.LanguageManager;
import igoat.client.ScreenUtil;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;


/**
 * Singleton window for displaying the game manual, including character info and navigation.
 */
public class ManualWindow {
    private static final Logger logger = LoggerFactory.getLogger(ManualWindow.class);
    private static final LanguageManager lang = LanguageManager.getInstance();

    private BorderPane layout;
    private static final ManualWindow instance = new ManualWindow();

    private final Stage stage = new Stage();

    private final List<CharacterInfo> characters = List.of(
            new CharacterInfo("Goat", "/sprites/goat_idle.png", "tutorial.goat"),
            new CharacterInfo("Guard", "/sprites/guard_idle.png", "tutorial.guard"),
            new CharacterInfo("iGOAT", "/sprites/igoat_idle.png", "tutorial.igoat"),
            new CharacterInfo("Terminal", "/sprites/terminal.png", "tutorial.terminal"),
            new CharacterInfo("iGOAT-Station", "/sprites/igoat_station.png", "tutorial.igoat_station"),
            new CharacterInfo("Exit", "/sprites/door.png", "tutorial.exit"),
            new CharacterInfo("Window", "/sprites/window.png", "tutorial.window")
    );

    private int currentIndex = 0;
    private final VBox characterBox = new VBox(10);

    private ManualWindow() {
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Manual");
        stage.getIcons().add(new Image(getClass().getResourceAsStream("/sprites/igoat_icon.png")));

        layout = new BorderPane();
        layout.setPadding(new Insets(20));

        SoundButton prevButton = new SoundButton("←");
        SoundButton nextButton = new SoundButton("→");

        prevButton.setOnAction(e -> showCharacter(currentIndex - 1));
        nextButton.setOnAction(e -> showCharacter(currentIndex + 1));

        HBox navigation = new HBox(10, prevButton, nextButton);
        navigation.setAlignment(Pos.CENTER);

        characterBox.setAlignment(Pos.CENTER);

        layout.setCenter(characterBox);
        layout.setBottom(navigation);

        Scene scene = new Scene(layout, 600, 400);
        try {
            scene.getStylesheets().add(getClass().getResource("/CSS/UI.css").toExternalForm());
        } catch (Exception e) {
            logger.warn("Stylesheet couldn't load.", e);
        }

        stage.setScene(scene);

        showCharacter(0);
    }

    /**
     * Returns the singleton instance of ManualWindow.
     * @return ManualWindow instance
     */
    public static ManualWindow getInstance() {
        return instance;
    }

    /**
     * Opens the manual window and blocks until it is closed.
     */
    public void open() {
        ScreenUtil.moveStageToCursorScreen(stage, layout.getPrefWidth() > 0 ? layout.getPrefWidth() : 600, layout.getPrefHeight() > 0 ? layout.getPrefHeight() : 400);
stage.showAndWait();
    }

    /**
     * Closes the manual window.
     */
    public void close() {
        stage.close();
    }

    private void showCharacter(int index) {
        if (index < 0) {
            index = characters.size() - 1;
        } else if (index >= characters.size()) {
            index = 0;
        }
        currentIndex = index;

        CharacterInfo info = characters.get(index);

        ImageView image;
        try {
            image = new ImageView(new Image(getClass().getResourceAsStream(info.imagePath)));
        } catch (Exception e) {
            logger.error("Picture couldn't load: " + info.imagePath, e);
            return;
        }

        image.setFitHeight(150);
        image.setPreserveRatio(true);

        Label name = new Label(info.name);
        name.setStyle("-fx-font-size: 25px; -fx-font-weight: bold;");

        Label description = new Label(info.text);
        description.setWrapText(true);
        description.setStyle("-fx-font-size: 20px;");

        characterBox.getChildren().setAll(name, image, description);
    }


    private static class CharacterInfo {
        String name;
        String imagePath;
        String text;

        CharacterInfo(String name, String imagePath, String text) {
            this.name = name;
            this.imagePath = imagePath;
            this.text = lang.get(text);
        }
    }
}
