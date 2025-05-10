package igoat.client.GUI;

import igoat.client.LanguageManager;
import igoat.client.SoundManager;
import java.util.Locale;
import java.util.SortedMap;
import java.util.TreeMap;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Properties;

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
    private Slider volumeSlider;
    private ChoiceBox<String> windowModeChoice;
    private ChoiceBox<String> languageChoice = new ChoiceBox<>();
    private TabPane tabPane;
    private GridPane generalSettingsPane;

    private Stage gameStage;
    private double volume = SoundManager.getInstance().getVolume();
    private boolean fullscreen = false;
    
    private final SortedMap<String, KeyCode> keyBindings;
    private final SortedMap<String, String> controllerBindings;
    
    private static final SortedMap<String, KeyCode> DEFAULT_KEY_BINDINGS;
    private static final SortedMap<String, String> DEFAULT_CONTROLLER_BINDINGS;

    public static String lastIP;
    public static int lastPort;
    
    private boolean useController = true;
    
    private String selectedController = null;
    private ChoiceBox<String> controllerSelector; 

    public static final Map<String, Locale> AVAILABLE_LANGUAGES = Map.of(
        "English", Locale.ENGLISH,
        "Deutsch", Locale.GERMAN
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
        
        DEFAULT_CONTROLLER_BINDINGS = new TreeMap<>();
        DEFAULT_CONTROLLER_BINDINGS.put("moveUp", "DPad Up");
        DEFAULT_CONTROLLER_BINDINGS.put("moveDown", "DPad Down");
        DEFAULT_CONTROLLER_BINDINGS.put("moveLeft", "DPad Left");
        DEFAULT_CONTROLLER_BINDINGS.put("moveRight", "DPad Right");
        DEFAULT_CONTROLLER_BINDINGS.put("interact", "Button A");
        DEFAULT_CONTROLLER_BINDINGS.put("chat", "Button Y");
        DEFAULT_CONTROLLER_BINDINGS.put("settings", "Start");
    }

    private SettingsWindow() {
        keyBindings = new TreeMap<>();
        keyBindings.putAll(DEFAULT_KEY_BINDINGS);
        
        controllerBindings = new TreeMap<>();
        controllerBindings.putAll(DEFAULT_CONTROLLER_BINDINGS);
        
        volumeSlider = new Slider(0, 100, SoundManager.getInstance().getVolume() * 100.0);
        volumeSlider.getStylesheets().add(sliderStyle);
        volumeSlider.setShowTickLabels(true);
        volumeSlider.setShowTickMarks(true);
        volumeSlider.valueChangingProperty().addListener((obs, wasChanging, isChanging) -> {
            if (!isChanging) {
                SoundManager.getInstance().setVolume(volumeSlider.getValue() / 100.0);
            }
        });
        
        loadSettings();
        
        tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        
        Tab generalTab = new Tab("General");
        generalSettingsPane = createGeneralSettingsPane();
        generalTab.setContent(generalSettingsPane);
        
        Tab keyboardTab = new Tab("Keyboard");
        ScrollPane keyboardScrollPane = new ScrollPane(createKeyboardBindingsPane());
        keyboardScrollPane.setFitToWidth(true);
        keyboardScrollPane.setPrefViewportHeight(300);
        keyboardTab.setContent(keyboardScrollPane);
        
        Tab controllerTab = new Tab("Controller");
        ScrollPane controllerScrollPane = new ScrollPane(createControllerBindingsPane());
        controllerScrollPane.setFitToWidth(true);
        controllerScrollPane.setPrefViewportHeight(300);
        controllerTab.setContent(controllerScrollPane);
        
        tabPane.getTabs().addAll(generalTab, keyboardTab, controllerTab);
        
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
        
        mainLayout.setStyle("-fx-background-color: white; -fx-border-color: black; -fx-border-width: 1; -fx-font-family: \"Jersey 10\", \"Courier New\", monospace;");
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
        Label volumeLabel = new Label(lang.get("settings.volume") +":");
        
        // Window mode
        Label windowModeLabel = new Label(lang.get("settings.winMode") + ":");
        windowModeChoice = new ChoiceBox<>();
        windowModeChoice.getItems().addAll(lang.get("settings.windowed"), lang.get("settings.fullscreen"));
        windowModeChoice.setValue(fullscreen ? lang.get("settings.fullscreen") : lang.get("settings.windowed"));

        // Language
        Label languageLabel = new Label(lang.get("settings.language") + ":");
        languageChoice.getItems().addAll(AVAILABLE_LANGUAGES.keySet());

        pane.add(volumeLabel, 0, 0);
        pane.add(volumeSlider, 1, 0);
        pane.add(windowModeLabel, 0, 1);
        pane.add(windowModeChoice, 1, 1);
        pane.add(languageLabel, 0, 2);
        pane.add(languageChoice, 1, 2);
        
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
     * Creates the controller bindings pane with controller binding controls
     */
    private GridPane createControllerBindingsPane() {
        GridPane pane = new GridPane();
        pane.setPadding(new Insets(10));
        pane.setVgap(10);
        pane.setHgap(10);

        CheckBox useControllerCheckbox = new CheckBox(lang.get("settings.useController"));
        useControllerCheckbox.setSelected(useController);
        useControllerCheckbox.setOnAction(e -> {
            useController = useControllerCheckbox.isSelected();
            saveSettings();
        });
        pane.add(useControllerCheckbox, 0, 0, 2, 1);

        Label controllerLabel = new Label(lang.get("settings.selectController"));
        ComboBox<String> controllerSelector = new ComboBox<>();
        controllerSelector.getItems().add("None");
        
        Controller[] controllers = ControllerEnvironment.getDefaultEnvironment().getControllers();
        for (Controller c : controllers) {
            if (c.getType() == Controller.Type.GAMEPAD) {
                controllerSelector.getItems().add(c.getName());
            }
        }
        
        controllerSelector.setValue(selectedController != null ? selectedController : "None");
        
        controllerSelector.setOnAction(e -> {
            String selected = controllerSelector.getValue();
            selectedController = "None".equals(selected) ? null : selected;
            saveSettings();
        });
        
        pane.add(controllerLabel, 0, 1);
        pane.add(controllerSelector, 1, 1);

        Label instructionsLabel = new Label(
            "Controls:\n" +
            "A or X Button - Interact\n" +
            "Left Stick/D-Pad - Movement\n" +
            "Right Stick - Flashlight Control (Guard only)"
        );
        instructionsLabel.setStyle("-fx-font-family: monospace;");
        pane.add(instructionsLabel, 0, 2, 2, 1);

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
                
                useController = Boolean.parseBoolean(properties.getProperty("useController", "true"));
                selectedController = properties.getProperty("selectedController");
                String language = properties.getProperty("language", String.valueOf(languageChoice.getValue()));
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
                
                controllerBindings.clear();
                for (String key : DEFAULT_CONTROLLER_BINDINGS.keySet()) {
                    String value = properties.getProperty("controller." + key);
                    controllerBindings.put(key, value != null ? value : DEFAULT_CONTROLLER_BINDINGS.get(key));
                }
                
                volume = Double.parseDouble(properties.getProperty("volume", String.valueOf(volume)));
                fullscreen = Boolean.parseBoolean(properties.getProperty("fullscreen", String.valueOf(fullscreen)));
                
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
        props.setProperty("fullscreen", String.valueOf(fullscreen));
        props.setProperty("useController", String.valueOf(useController));
        
        if (selectedController != null && !selectedController.equals("None")) {
            props.setProperty("selectedController", selectedController);
        }
        props.setProperty("language", languageChoice.getValue() == null ? "English" : languageChoice.getValue());
        
        for (Map.Entry<String, KeyCode> entry : keyBindings.entrySet()) {
            props.setProperty("key." + entry.getKey(), entry.getValue().name());
        }
        
        Path configPath = getConfigFilePath();
        try {
            if (configPath.getParent() != null) {
                Files.createDirectories(configPath.getParent());
            }
            
            try (OutputStream out = new FileOutputStream(configPath.toFile())) {
                props.store(out, "iGoat Game Settings");
                logger.info("Settings saved to " + configPath);
            }
        } catch (IOException e) {
            logger.error("Failed to save settings", e);
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
     * Gets the controller binding for a specific action
     */
    public String getControllerBinding(String action) {
        return controllerBindings.getOrDefault(action, DEFAULT_CONTROLLER_BINDINGS.get(action));
    }
    
    /**
     * Returns the singleton instance of SettingsWindow
     * Uses lazy initialization to avoid circular dependencies
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
        volumeSlider.setValue(volume * 100.0);
        windowModeChoice.setValue(fullscreen ? lang.get("settings.fullscreen") : lang.get("settings.windowed"));
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

    public boolean getUseController() {
        return useController;
    }

    public void setUseController(boolean useController) {
        this.useController = useController;
    }

    private void updateAvailableControllers() {
        controllerSelector.getItems().clear();
        Controller[] controllers = ControllerEnvironment.getDefaultEnvironment().getControllers();
        for (Controller controller : controllers) {
            if (controller.getType() == Controller.Type.GAMEPAD || 
                controller.getType() == Controller.Type.STICK) {
                controllerSelector.getItems().add(controller.getName());
            }
        }
        controllerSelector.getItems().add("None");
        
        if (selectedController == null || !controllerSelector.getItems().contains(selectedController)) {
            controllerSelector.setValue("None");
        }
    }

    public String getSelectedController() {
        return selectedController;
    }

    public void setSelectedController(String controller) {
        this.selectedController = controller;
        if (controllerSelector != null) {
            controllerSelector.setValue(controller);
        }
        saveSettings();
    }

    private void resetToDefaults() {
        keyBindings.clear();
        keyBindings.putAll(DEFAULT_KEY_BINDINGS);
        controllerBindings.clear();
        controllerBindings.putAll(DEFAULT_CONTROLLER_BINDINGS);
        useController = true;
        selectedController = null;
        volume = SoundManager.getInstance().getVolume();
        fullscreen = false;
        saveSettings();
    }
}
