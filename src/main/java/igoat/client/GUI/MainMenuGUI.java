package igoat.client.GUI;

import igoat.client.LanguageManager;
import igoat.client.ScreenUtil;

import igoat.client.ServerHandler;
import igoat.client.Sprite;
import igoat.server.Server;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import igoat.client.SoundManager;

/**
 * The main entry point for the iGoat client application. Displays the main menu that allows users to
 * create a server or join an existing one.
 */
public class MainMenuGUI extends Application {
    /** Logger for this class */
    private static final Logger logger = LoggerFactory.getLogger(MainMenuGUI.class);
    public static final Sprite icon = new Sprite("/sprites/igoat_icon.png");
    private static final LanguageManager lang = LanguageManager.getInstance();

    private ServerHandler handler;
    private String username;
    private Thread serverThread;
    private Stage stage;

    /**
     * Initializes and displays the main menu of the application.
     * Sets up the GUI components including server creation, joining, and exit options.
     *
     * @param primaryStage the primary stage for this application
     */
    @Override
    public void start(Stage primaryStage) {
        stage = primaryStage;
        stage.getIcons().add(icon);
        primaryStage.setTitle("iGOAT");
        primaryStage.setOnCloseRequest(event -> exit());

        VBox root = new VBox(15);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(20));
        String style = "";
        Scene scene = new Scene(root, 400, 350);

        try {
            Font.loadFont(getClass().getResource("/fonts/Jersey10-Regular.ttf").toExternalForm(), 12);
            style = getClass().getResource("/CSS/UI.css").toExternalForm();
            scene.getStylesheets().add(style);
            scene.getStylesheets().add(getClass().getResource("/CSS/LobbyBackground.css").toExternalForm());
        } catch (NullPointerException e) {
            logger.error("Failed to load CSS resources", e);
        }
        String finalStyle = style;

        Image logo = new Image(getClass().getResource("/Logo/logo.png").toExternalForm());
        ImageView imageView = new ImageView(logo);
        double maxWidth = 200;
        imageView.setFitWidth(maxWidth);
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);
        imageView.setCache(true);
        VBox.setMargin(imageView, new Insets(0, 0, -20, 0));

        Label presentLabel = new Label(lang.get("main.presents"));
        presentLabel.setFont(new Font("Jersey 10", 24));
        presentLabel.setStyle("-fx-font-size: 30px;");
        VBox.setMargin(presentLabel, new Insets(0, 0, -20, 0));

        Label titleLabel = new Label("iGOAT");
        titleLabel.setFont(new Font("Jersey 10", 24));
        titleLabel.setStyle("-fx-font-size: 50px;");

        SoundButton createServerButton = new SoundButton(lang.get("main.createServer"));
        createServerButton.setOnAction(e -> {
            TextInputDialog portDialog = new TextInputDialog("61000");
            portDialog.setTitle(lang.get("main.createServer"));
            portDialog.setHeaderText(null);
            portDialog.setContentText(lang.get("main.enterPort"));
            SoundButton.addDialogSound(portDialog);

            DialogPane dialogPane = portDialog.getDialogPane();
            dialogPane.getStylesheets().add(finalStyle);
            logger.info(finalStyle);

            Stage portDialogStage = (Stage) portDialog.getDialogPane().getScene().getWindow();
            ScreenUtil.moveStageToCursorScreen(portDialogStage, portDialogStage.getWidth() > 0 ? portDialogStage.getWidth() : 350, portDialogStage.getHeight() > 0 ? portDialogStage.getHeight() : 200);
            portDialog.showAndWait().ifPresent(serverPort -> {
                serverThread = new Thread(() -> Server.startServer(Integer.parseInt(serverPort)));
                serverThread.setDaemon(true);
                serverThread.start();
                SettingsWindow.lastPort = Integer.parseInt(serverPort);
            });
        });

        SoundButton joinServerButton = new SoundButton(lang.get("main.joinServer"));
        joinServerButton.setOnAction(e -> {
            TextInputDialog ipDialog = new TextInputDialog(SettingsWindow.lastIP == null? "localhost" : SettingsWindow.lastIP);
            ipDialog.setTitle(lang.get("main.joinServer"));
            ipDialog.setHeaderText(null);
            ipDialog.setContentText(lang.get("main.enterIP"));
            SoundButton.addDialogSound(ipDialog);

            DialogPane joinDialogPane = ipDialog.getDialogPane();
            joinDialogPane.getStylesheets().add(finalStyle);

            Stage ipDialogStage = (Stage) ipDialog.getDialogPane().getScene().getWindow();
            ScreenUtil.moveStageToCursorScreen(ipDialogStage, ipDialogStage.getWidth() > 0 ? ipDialogStage.getWidth() : 350, ipDialogStage.getHeight() > 0 ? ipDialogStage.getHeight() : 200);
            ipDialog.showAndWait().ifPresent(serverIP -> {
                TextInputDialog portDialog = new TextInputDialog(SettingsWindow.lastPort == 0? "61000" : String.valueOf(SettingsWindow.lastPort));
                portDialog.setTitle(lang.get("main.joinServer"));
                portDialog.setHeaderText(null);
                portDialog.setContentText(lang.get("main.enterPort"));
                SoundButton.addDialogSound(portDialog);

                DialogPane portDialogPane = portDialog.getDialogPane();
                portDialogPane.getStylesheets().add(finalStyle);

                Stage joinPortDialogStage = (Stage) portDialog.getDialogPane().getScene().getWindow();
                ScreenUtil.moveStageToCursorScreen(joinPortDialogStage, joinPortDialogStage.getWidth() > 0 ? joinPortDialogStage.getWidth() : 350, joinPortDialogStage.getHeight() > 0 ? joinPortDialogStage.getHeight() : 200);
                portDialog.showAndWait().ifPresent(port -> {
                    String systemUsername = (handler == null || handler.getConfirmedNickname() == null)? System.getProperty("user.name") : handler.getConfirmedNickname();
                    TextInputDialog nameDialog = new TextInputDialog(systemUsername);
                    nameDialog.setTitle(lang.get("main.joinServer"));
                    nameDialog.setHeaderText(null);
                    nameDialog.setContentText(lang.get("main.enterName"));
                    SoundButton.addDialogSound(nameDialog);

                    DialogPane nameDialogPane = nameDialog.getDialogPane();
                    nameDialogPane.getStylesheets().add(finalStyle);

                    Stage dialogStage = (Stage) nameDialog.getDialogPane().getScene().getWindow();
                    ScreenUtil.moveStageToCursorScreen(dialogStage, dialogStage.getWidth() > 0 ? dialogStage.getWidth() : 350, dialogStage.getHeight() > 0 ? dialogStage.getHeight() : 200);
                    nameDialog.showAndWait().ifPresent(name -> {
                        // sanitize string
                        name = name.replaceAll("[\\s=:,]", "");
                        if (name.isEmpty()) {
                            showAlert(Alert.AlertType.ERROR, lang.get("main.nameEmpty"));
                            return;
                        }

                        username = name;
                        join(serverIP, Integer.parseInt(port), username);
                        SettingsWindow.lastPort = Integer.parseInt(port);
                        SettingsWindow.lastIP = serverIP;
                    });
                });
            });
        });

        SoundButton exitButton = new SoundButton(lang.get("main.exit"));
        exitButton.setOnAction(e -> exit());

        root.getChildren().addAll(
                imageView,
                presentLabel,
            titleLabel,
            createServerButton,
            joinServerButton,
            exitButton
        );

        ScreenUtil.moveStageToCursorScreen(primaryStage, root.getPrefWidth() > 0 ? root.getPrefWidth() : 400, root.getPrefHeight() > 0 ? root.getPrefHeight() : 350);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    /**
     * Establishes a connection to the server and launches the lobby GUI.
     * If the connection fails, displays an error message.
     *
     * @param serverIP the IP address of the server to connect to
     * @param port the port number of the server
     * @param username the player's username for the game session
     */
    public void join(String serverIP, int port, String username) {
        if (serverIP.isEmpty()) {
            logger.error("Invalid server IP");
            showAlert(Alert.AlertType.ERROR, lang.get("main.invalidIP"));
            return;
        }

        handler = new ServerHandler(serverIP.trim(), port, username);

        if (!handler.isConnected()) {
            logger.error("Failed to connect to server at: {}:{}", serverIP, port);
            Platform.runLater(() ->
                showAlert(Alert.AlertType.ERROR,  lang.get("main.failedConnect") + ": " + serverIP + ":" + port)
            );
            return;
        }

        logger.info("Connected successfully to {}:{}", serverIP, port);
        Platform.runLater(() -> {
            try {
                LobbyGUI.setServerHandler(handler);
                LobbyGUI lobby = new LobbyGUI(stage);
                lobby.setUsername(username);
                SoundManager.getInstance().stopAll();
                lobby.show(new Stage());
                stage.hide();
            } catch (Exception ex) {
                logger.error("Couldn't launch LobbyGUI", ex);
                showAlert(Alert.AlertType.ERROR, lang.get("main.errorLobby") + ": " + ex.getMessage());
            }
        });
    }

    /**
     * Handles the application exit process.
     * Closes the server connection if it exists and terminates the JavaFX application.
     */
    private void exit() {
        logger.info("Exited application");
        if (handler != null) {
            handler.close();
        }
        SoundManager.getInstance().stopAll();
        Platform.exit();
    }

    /**
     * Displays an alert dialog to the user.
     * This method is thread-safe and can be called from any thread.
     *
     * @param type the type of alert to display (e.g., ERROR, INFORMATION)
     * @param message the message to display in the alert dialog
     */
    private void showAlert(Alert.AlertType type, String message) {
        if (Platform.isFxApplicationThread()) {
            Alert alert = new Alert(type, message, ButtonType.OK);
            alert.setHeaderText(null);
            alert.showAndWait();
        } else {
            Platform.runLater(() -> {
                Alert alert = new Alert(type, message, ButtonType.OK);
                alert.setHeaderText(null);
                alert.showAndWait();
            });
        }
    }
    
    /**
     * Alternative entry point for the application.
     * Launches the JavaFX application thread.
     *
     * @param args command line arguments (not used)
     */
    public static void main(String[] args) {
        logger.info("Launching application...");
        launch(args);
    }
}
