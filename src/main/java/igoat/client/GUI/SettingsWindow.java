package igoat.client.GUI;

import igoat.client.ScreenUtil;

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

    private final Stage stage = new Stage();
    private final Slider volumeSlider;
    private ChoiceBox<String> windowModeChoice;
    private GridPane layout;

    private Stage gameStage;
    private double volume = SoundManager.getInstance().getVolume();
    private boolean fullscreen = true;

    private SettingsWindow() {
        stage.initModality(Modality.NONE);
        stage.setAlwaysOnTop(true);
        stage.setTitle("Settings");
        stage.setMinWidth(300);
        stage.setMinHeight(200);

        // Volume Control
        Label volumeLabel = new Label("Volume:");
        volumeSlider = new Slider(0, 100, SoundManager.getInstance().getVolume() * 100.0);
        volumeSlider.getStylesheets().add(sliderStyle);
        volumeSlider.setShowTickLabels(true);
        volumeSlider.setShowTickMarks(true);
        volumeSlider.valueChangingProperty().addListener((obs, wasChanging, isChanging) -> {
            if (!isChanging) {
                SoundManager.getInstance().setVolume(volumeSlider.getValue() / 100.0);
            }
        });

        // Window mode
        Label windowModeLabel = new Label("Window Mode:");
        windowModeChoice = new ChoiceBox<>();
        windowModeChoice.getItems().addAll("Windowed", "Fullscreen");
        windowModeChoice.setValue("Windowed");

        // Apply and Close Buttons
        SoundButton applyButton = new SoundButton("OK");
        SoundButton closeButton = new SoundButton("Close");
        closeButton.setOnAction(e -> {
            close();
        });

        applyButton.setOnAction(e -> {
            volume = volumeSlider.getValue() / 100.0;
            SoundManager.getInstance().setVolume(volume);

            fullscreen = windowModeChoice.getValue().equals("Fullscreen");
            if (gameStage != null) {
                gameStage.setFullScreen(fullscreen);
            }
            stage.close();
        });

        stage.setOnCloseRequest(e -> {
            close();
        });

        // Layout
        layout = new GridPane();
        layout.setPadding(new Insets(10));
        layout.setVgap(10);
        layout.setHgap(10);

        layout.add(volumeLabel, 0, 0);
        layout.add(volumeSlider, 1, 0);
        layout.add(windowModeLabel, 0, 1);
        layout.add(windowModeChoice, 1, 1);
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
        volumeSlider.setValue(volume * 100.0);
        windowModeChoice.setValue(fullscreen ? "Fullscreen" : "Windowed");
        ScreenUtil.moveStageToCursorScreen(stage, layout.getPrefWidth() > 0 ? layout.getPrefWidth() : 300, layout.getPrefHeight() > 0 ? layout.getPrefHeight() : 200);
        stage.show();
    }

    public void close() {
        SoundManager.getInstance().setVolume(volume);
        stage.close();
        stage.hide();
    }

    public void setGameStage(Stage stage) {
        gameStage = stage;
    }

    public boolean getFullscreen() {
        return fullscreen;
    }
}
