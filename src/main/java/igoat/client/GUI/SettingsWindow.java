package igoat.client.GUI;

import igoat.client.SoundManager;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Singleton class for the settings window
 */
public class SettingsWindow {
    private static final Logger logger = LoggerFactory.getLogger(SettingsWindow.class);
    private static final SettingsWindow instance = new SettingsWindow();

    private final String style = getClass().getResource("/CSS/UI.css").toExternalForm();
    private final String sliderStyle = getClass().getResource("/CSS/slider.css").toExternalForm();

    private Stage stage = new Stage();

    private SettingsWindow() {
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Settings");
        stage.setMinWidth(300);
        stage.setMinHeight(200);

        // Volume Control
        Label volumeLabel = new Label("Volume:");
        Slider volumeSlider = new Slider(0, 100, SoundManager.getInstance().getVolume() * 100);
        volumeSlider.getStylesheets().add(sliderStyle);
        volumeSlider.setShowTickLabels(true);
        volumeSlider.setShowTickMarks(true);

        // Window mode
        Label difficultyLabel = new Label("Window Mode:");
        ChoiceBox<String> difficultyChoice = new ChoiceBox<>();
        difficultyChoice.getItems().addAll("Windowed", "Fullscreen");
        difficultyChoice.setValue("Windowed");

        // Apply and Close Buttons
        SoundButton applyButton = new SoundButton("Apply");
        SoundButton closeButton = new SoundButton("Close");
        closeButton.setOnAction(e -> stage.close());

        applyButton.setOnAction(e -> {
            double volume = volumeSlider.getValue() / 100;
            SoundManager.getInstance().setVolume(volume);
            stage.close();
        });

        // Layout
        GridPane layout = new GridPane();
        layout.setPadding(new Insets(10));
        layout.setVgap(10);
        layout.setHgap(10);

        layout.add(volumeLabel, 0, 0);
        layout.add(volumeSlider, 1, 0);
        layout.add(difficultyLabel, 0, 1);
        layout.add(difficultyChoice, 1, 1);
        layout.add(applyButton, 0, 2);
        layout.add(closeButton, 1, 2);

        Scene scene = new Scene(layout);
        scene.getStylesheets().add(style);
        stage.setScene(scene);
    }
    
    public static SettingsWindow getInstance() {
        return instance;
    }

    public void open() {
        stage.show();
    }

    public void close() {
        stage.hide();
    }
}
