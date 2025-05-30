package igoat.client.GUI;

import igoat.client.LanguageManager;
import igoat.client.SoundManager;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.SortedMap;
import java.util.TreeMap;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Singleton class for the settings window
 */
public class SettingsWindow {

    private static final Logger logger = LoggerFactory.getLogger(SettingsWindow.class);
    private final LanguageManager lang = LanguageManager.getInstance();

    private static SettingsWindow instance;
    private static final String CONFIG_FILENAME = "igoat_settings.properties";

    private final String style = getClass().getResource("/CSS/UI.css").toExternalForm();
    private final String sliderStyle = getClass().getResource("/CSS/slider.css").toExternalForm();

    private final Popup popup = new Popup();
    private final Slider volumeSlider;
    private final Slider soundtrackSlider;
    private ChoiceBox<String> windowModeChoice;
    private final ChoiceBox<String> languageChoice = new ChoiceBox<>();
    private final TabPane tabPane;
    private final GridPane generalSettingsPane;
    private ScrollPane keyboardScrollPane;
    private final Tab keyboardTab;

    private Stage gameStage;
    private double volume = SoundManager.getInstance().getVolume();
    private double soundtrackVolume = SoundManager.getInstance().getSoundtrackVolume();
    private boolean fullscreen = false;

    private final SortedMap<String, KeyCode> keyBindings;

    private static final SortedMap<String, KeyCode> DEFAULT_KEY_BINDINGS;

    public static String lastIP;
    public static int lastPort;

    public static final Map<String, Locale> AVAILABLE_LANGUAGES = Map.of(
        "English", Locale.ENGLISH,
        "Deutsch", Locale.GERMAN,
        "Português", new Locale.Builder().setLanguage("pt").setRegion("BR").build(),
        "日本語", Locale.JAPANESE,
        "Español", new Locale.Builder().setLanguage("es").setRegion("ES").build(),
        "Italiano", Locale.ITALIAN,
        "汉语", Locale.CHINESE
    );

    static {
        DEFAULT_KEY_BINDINGS = new TreeMap<>();
        DEFAULT_KEY_BINDINGS.put("moveUp", KeyCode.W);
        DEFAULT_KEY_BINDINGS.put("moveDown", KeyCode.S);
        DEFAULT_KEY_BINDINGS.put("moveLeft", KeyCode.A);
        DEFAULT_KEY_BINDINGS.put("moveRight", KeyCode.D);
        DEFAULT_KEY_BINDINGS.put("interact", KeyCode.E);
        DEFAULT_KEY_BINDINGS.put("chat", KeyCode.ENTER);
        DEFAULT_KEY_BINDINGS.put("settings", KeyCode.ESCAPE);
        DEFAULT_KEY_BINDINGS.put("cycleSpectator", KeyCode.TAB);
        DEFAULT_KEY_BINDINGS.put("exitSpectator", KeyCode.SPACE);
    }

    private SettingsWindow() {
        keyBindings = new TreeMap<>();
        keyBindings.putAll(DEFAULT_KEY_BINDINGS);

        volumeSlider = new Slider(0, 100, SoundManager.getInstance().getVolume() * 100.0);
        volumeSlider.getStylesheets().add(sliderStyle);
        volumeSlider.setShowTickLabels(true);
        volumeSlider.setShowTickMarks(true);
        volumeSlider.valueChangingProperty().addListener((obs, wasChanging, isChanging) -> {
            if (!isChanging) {
                volume = volumeSlider.getValue() / 100.0;
                SoundManager.getInstance().setVolume(volume);
                saveSettings();
            }
        });

        soundtrackSlider = new Slider(0, 100, soundtrackVolume * 100.0);
        soundtrackSlider.getStylesheets().add(sliderStyle);
        soundtrackSlider.setShowTickLabels(true);
        soundtrackSlider.setShowTickMarks(true);
        soundtrackSlider.valueChangingProperty().addListener((obs, wasChanging, isChanging) -> {
            if (!isChanging) {
                soundtrackVolume = soundtrackSlider.getValue() / 100.0;
                SoundManager.getInstance().setSoundtrackVolume(soundtrackVolume);
                saveSettings();
            }
        });

        loadSettings();

        tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Tab generalTab = new Tab("General");
        generalSettingsPane = createGeneralSettingsPane();
        generalTab.setContent(generalSettingsPane);

        keyboardTab = new Tab("Keyboard");
        keyboardScrollPane = new ScrollPane(createKeyboardBindingsPane());
        keyboardScrollPane.setFitToWidth(true);
        keyboardScrollPane.setPrefViewportHeight(300);
        keyboardTab.setContent(keyboardScrollPane);

        tabPane.getTabs().addAll(generalTab, keyboardTab);

        VBox mainLayout = new VBox(10);
        mainLayout.setPadding(new Insets(10));
        mainLayout.getStylesheets().add(style);

        // Apply and Close Buttons
        SoundButton applyButton = new SoundButton(lang.get("settings.apply"));
        SoundButton closeButton = new SoundButton(lang.get("settings.close"));

        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        buttonBox.getChildren().addAll(applyButton, closeButton);

        mainLayout.getChildren().addAll(tabPane, buttonBox);
        mainLayout.setPrefWidth(500);
        mainLayout.setPrefHeight(450);

        closeButton.setOnAction(e -> close());

        applyButton.setOnAction(e -> {
            volume = volumeSlider.getValue() / 100.0;
            SoundManager.getInstance().setVolume(volume);

            fullscreen = windowModeChoice.getValue().equals(lang.get("settings.fullscreen"));
            if (gameStage != null) {
                gameStage.setFullScreen(fullscreen);
            }

            saveSettings();

            popup.hide();
        });

        mainLayout.setStyle(
            "-fx-background-color: white; -fx-border-color: black; -fx-border-width: 1; -fx-font-family: \"Jersey 10\", \"Courier New\", monospace;");
        popup.getContent().add(mainLayout);
    }

    /**
     * Creates the general settings pane with volume and window mode controls
     */
    private GridPane createGeneralSettingsPane() {
        GridPane pane = new GridPane();
        pane.setPadding(new Insets(10));
        pane.setVgap(10);
        pane.setHgap(10);

        // Volume Control
        Label volumeLabel = new Label(lang.get("settings.volume") + ":");

        Label soundtrackLabel = new Label(lang.get("settings.soundtrack") + ":");

        // Window mode
        Label windowModeLabel = new Label(lang.get("settings.winMode") + ":");
        windowModeChoice = new ChoiceBox<>();
        windowModeChoice.getItems()
            .addAll(lang.get("settings.windowed"), lang.get("settings.fullscreen"));
        windowModeChoice.setValue(
            fullscreen ? lang.get("settings.fullscreen") : lang.get("settings.windowed"));

        // Language
        Label languageLabel = new Label(lang.get("settings.language") + ":");
        languageChoice.getItems().addAll(AVAILABLE_LANGUAGES.keySet());
        Label languageInfo = new Label(lang.get("settings.langInfo"));
        languageInfo.setWrapText(true);
        languageInfo.setMaxWidth(400);

        pane.add(volumeLabel, 0, 0);
        pane.add(volumeSlider, 1, 0);
        pane.add(soundtrackLabel, 0, 1);
        pane.add(soundtrackSlider, 1, 1);
        pane.add(windowModeLabel, 0, 2);
        pane.add(windowModeChoice, 1, 2);
        pane.add(languageLabel, 0, 3);
        pane.add(languageChoice, 1, 3);
        pane.add(languageInfo, 0, 4, 2, 1);

        return pane;
    }

    /**
     * Creates the keyboard bindings pane with key binding controls
     */
    private GridPane createKeyboardBindingsPane() {
        GridPane pane = new GridPane();
        pane.setPadding(new Insets(10));
        pane.setVgap(10);
        pane.setHgap(10);

        Label actionHeader = new Label(lang.get("settings.action"));
        Label keyHeader = new Label(lang.get("settings.key"));
        pane.add(actionHeader, 0, 0);
        pane.add(keyHeader, 1, 0);

        int row = 1;
        for (Map.Entry<String, KeyCode> entry : keyBindings.entrySet()) {
            String action = entry.getKey();
            KeyCode key = entry.getValue();

            String displayAction = formatActionName(action);

            Label actionLabel = new Label(displayAction);
            Button keyButton = new Button(key.getName());

            keyButton.setOnAction(e -> {
                keyButton.setText(lang.get("settings.keyPrompt"));
                keyButton.setOnKeyPressed(keyEvent -> {
                    KeyCode newKey = keyEvent.getCode();
                    keyButton.setText(newKey.getName());
                    keyBindings.put(action, newKey);
                    keyEvent.consume();
                    keyButton.setOnKeyPressed(null);
                });
                keyButton.requestFocus();
            });

            pane.add(actionLabel, 0, row);
            pane.add(keyButton, 1, row);
            row++;
        }

        Button resetButton = new SoundButton(lang.get("settings.reset"));
        resetButton.setOnAction(e -> {
            keyBindings.clear();
            keyBindings.putAll(DEFAULT_KEY_BINDINGS);
            tabPane.getSelectionModel().select(0);
            tabPane.getSelectionModel().select(1);
        });

        pane.add(resetButton, 0, row, 2, 1);
        GridPane.setHalignment(resetButton, javafx.geometry.HPos.CENTER);

        return pane;
    }

    /**
     * Formats an action name for display (e.g., "moveUp" -> "Move Up")
     */
    private String formatActionName(String action) {
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;

        for (char c : action.toCharArray()) {
            if (Character.isUpperCase(c)) {
                result.append(' ').append(c);
                capitalizeNext = false;
            } else if (capitalizeNext) {
                result.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                result.append(c);
            }
        }

        return result.toString();
    }

    /**
     * Loads settings from the properties file
     */
    private void loadSettings() {
        Properties properties = new Properties();

        if (Files.exists(Paths.get(CONFIG_FILENAME))) {
            try {
                properties.load(new FileInputStream(CONFIG_FILENAME));

                String language = properties.getProperty("language",
                    String.valueOf(languageChoice.getValue()));
                languageChoice.setValue(language == null ? "English" : language);

                keyBindings.clear();
                for (String key : DEFAULT_KEY_BINDINGS.keySet()) {
                    String value = properties.getProperty("key." + key);
                    if (value != null) {
                        try {
                            keyBindings.put(key, KeyCode.valueOf(value));
                        } catch (IllegalArgumentException e) {
                            keyBindings.put(key, DEFAULT_KEY_BINDINGS.get(key));
                        }
                    } else {
                        keyBindings.put(key, DEFAULT_KEY_BINDINGS.get(key));
                    }
                }

                volume = Double.parseDouble(
                    properties.getProperty("volume", String.valueOf(volume)));
                soundtrackVolume = Double.parseDouble(
                    properties.getProperty("soundtrackVolume", "0.2"));
                SoundManager.getInstance().setSoundtrackVolume(soundtrackVolume);
                fullscreen = Boolean.parseBoolean(
                    properties.getProperty("fullscreen", String.valueOf(fullscreen)));

                logger.info("Settings loaded from " + CONFIG_FILENAME);
            } catch (IOException e) {
                logger.error("Failed to load settings", e);
                resetToDefaults();
            }
        } else {
            resetToDefaults();
        }
    }

    /**
     * Saves settings to the properties file
     */
    private void saveSettings() {
        Properties props = new Properties();

        props.setProperty("volume", String.valueOf(volume));
        props.setProperty("soundtrackVolume", String.valueOf(soundtrackVolume));
        props.setProperty("fullscreen", String.valueOf(fullscreen));
        props.setProperty("language",
            languageChoice.getValue() == null ? "English" : languageChoice.getValue());

        for (Map.Entry<String, KeyCode> entry : keyBindings.entrySet()) {
            props.setProperty("key." + entry.getKey(), entry.getValue().name());
        }

        Path configPath = getConfigFilePath();
        try {
            if (configPath.getParent() != null) {
                Files.createDirectories(configPath.getParent());
            }

            if (Files.exists(configPath)) {
                Path backupPath = Paths.get(configPath + ".backup");
                try {
                    Files.copy(configPath, backupPath, StandardCopyOption.REPLACE_EXISTING);
                    logger.info("Created backup of settings file at " + backupPath);
                } catch (IOException e) {
                    logger.warn("Failed to create backup of settings file", e);
                }
            }

            try (FileOutputStream out = new FileOutputStream(configPath.toFile())) {
                props.store(out, "iGoat Game Settings");
                logger.info("Settings successfully saved to " + configPath);

                Properties verifyProps = new Properties();
                try (FileInputStream in = new FileInputStream(configPath.toFile())) {
                    verifyProps.load(in);
                    if (!verifyProps.getProperty("soundtrackVolume")
                        .equals(String.valueOf(soundtrackVolume))) {
                        logger.error("Settings verification failed - soundtrackVolume mismatch");
                    }
                    if (!verifyProps.getProperty("volume").equals(String.valueOf(volume))) {
                        logger.error("Settings verification failed - volume mismatch");
                    }
                }
            }
        } catch (IOException e) {
            logger.error("Failed to save settings to " + configPath, e);
            try {
                Path tempPath = Paths.get(System.getProperty("java.io.tmpdir"),
                    "igoat_settings.properties");
                try (OutputStream out = new FileOutputStream(tempPath.toFile())) {
                    props.store(out, "iGoat Game Settings (Temporary)");
                    logger.warn("Settings saved to temporary location: " + tempPath);
                }
            } catch (IOException e2) {
                logger.error("Failed to save settings to temporary location", e2);
            }
        }
    }

    /**
     * Gets the path to the config file
     */
    public static Path getConfigFilePath() {
        String userDir = System.getProperty("user.dir");
        return Paths.get(userDir, CONFIG_FILENAME);
    }

    /**
     * Gets the key binding for a specific action
     */
    public KeyCode getKeyBinding(String action) {
        return keyBindings.getOrDefault(action, DEFAULT_KEY_BINDINGS.get(action));
    }

    /**
     * Returns the singleton instance of SettingsWindow Uses lazy initialization to avoid circular
     * dependencies
     */
    public static synchronized SettingsWindow getInstance() {
        if (instance == null) {
            instance = new SettingsWindow();
        }
        return instance;
    }

    /**
     * Opens the settings window
     */
    public void open(Stage parentStage) {
        loadSettings();
        volumeSlider.setValue(volume * 100.0);
        soundtrackSlider.setValue(soundtrackVolume * 100.0);
        windowModeChoice.setValue(
            fullscreen ? lang.get("settings.fullscreen") : lang.get("settings.windowed"));
        keyboardScrollPane = new ScrollPane(createKeyboardBindingsPane());
        keyboardTab.setContent(keyboardScrollPane);
        popup.show(parentStage);
    }

    /**
     * Closes the settings window
     */
    public void close() {
        SoundManager.getInstance().setVolume(volume);
        popup.hide();
    }

    /**
     * Sets the game stage for fullscreen toggling
     */
    public void setGameStage(Stage stage) {
        gameStage = stage;
    }

    /**
     * Gets the fullscreen setting
     */
    public boolean getFullscreen() {
        return fullscreen;
    }

    private void resetToDefaults() {
        keyBindings.clear();
        keyBindings.putAll(DEFAULT_KEY_BINDINGS);
        volume = SoundManager.getInstance().getVolume();
        soundtrackVolume = 0.2;
        SoundManager.getInstance().setSoundtrackVolume(soundtrackVolume);
        fullscreen = false;
        saveSettings();
    }

    public double getSoundtrackVolume() {
        return soundtrackVolume;
    }

    public void setSoundtrackVolume(double volume) {
        this.soundtrackVolume = volume;
        SoundManager.getInstance().setSoundtrackVolume(volume);
        saveSettings();
    }
}
