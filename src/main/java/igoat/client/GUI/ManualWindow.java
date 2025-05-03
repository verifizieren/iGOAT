package igoat.client.GUI;

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

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;


/**
 * Singleton class for the manual
 */
public class ManualWindow {
    private static final Logger logger = LoggerFactory.getLogger(ManualWindow.class);
    private static final ManualWindow instance = new ManualWindow();

    private final Stage stage = new Stage();

    private final List<CharacterInfo> characters = List.of(
            new CharacterInfo("Goat", "/sprites/goat_idle.png", "/manual/goat.txt"),
            new CharacterInfo("Guard", "/sprites/guard.png", "/manual/guard.txt"),
            new CharacterInfo("iGOAT", "/sprites/igoat_idle.png", "/manual/igoat.txt"),
            new CharacterInfo("Terminal", "/sprites/terminal.png", "/manual/terminal.txt"),
            new CharacterInfo("iGOAT-Station", "/sprites/igoat_station.png", "/manual/igoat_station.txt")
    );

    private int currentIndex = 0;
    private final VBox characterBox = new VBox(10);

    private ManualWindow() {
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Manual");

        BorderPane layout = new BorderPane();
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

        showCharacter(0); // Erste Figur anzeigen
    }

    public static ManualWindow getInstance() {
        return instance;
    }

    public void open() {
        stage.showAndWait();
    }

    public void close() {
        stage.close();
    }

    private void showCharacter(int index) {
        if (index < 0 || index >= characters.size()) return;
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
        name.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Label description = new Label(info.loadDescription());
        description.setWrapText(true);
        description.setStyle("-fx-font-size: 14px;");

        characterBox.getChildren().setAll(name, image, description);
    }


    private static class CharacterInfo {
        String name;
        String imagePath;
        String textFilePath;

        CharacterInfo(String name, String imagePath, String textFilePath) {
            this.name = name;
            this.imagePath = imagePath;
            this.textFilePath = textFilePath;
        }

        String loadDescription() {
            try (InputStream is = getClass().getResourceAsStream(textFilePath);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                return reader.lines().collect(Collectors.joining("\n"));
            } catch (Exception e) {
                return "Beschreibung konnte nicht geladen werden.";
            }
        }
    }
}
