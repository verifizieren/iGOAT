package igoat.client.GUI;

import igoat.client.ServerHandler;
import igoat.server.Server;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The main entry point for the iGoat client application. Displays the main menu that allows users to
 * create a server or join an existing one.
 */
public class MainMenuGUI extends Application {
    /** Logger for this class */
    private static final Logger logger = LoggerFactory.getLogger(MainMenuGUI.class);

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
        primaryStage.setTitle("iGOAT");
        primaryStage.setOnCloseRequest(event -> exit());

        VBox root = new VBox(15);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(20));
        String style = "";

        try {
            Font.loadFont(getClass().getResource("/Jersey10-Regular.ttf").toExternalForm(), 12);
            style = getClass().getResource("/CSS/UI.css").toExternalForm();
            root.getStylesheets().add(style);
        } catch (NullPointerException e) {
            logger.error("Failed to load CSS resources", e);
        }
        String finalStyle = style;

        Label titleLabel = new Label("iGOAT");
        titleLabel.setFont(new Font("Jersey 10", 24));
        titleLabel.setStyle("-fx-font-size: 50px;");

        Button createServerButton = new Button("Create Server");
        createServerButton.setOnAction(e -> {
            TextInputDialog portDialog = new TextInputDialog("61000");
            portDialog.setTitle("Create Server");
            portDialog.setHeaderText(null);
            portDialog.setContentText("Enter server port:");

            DialogPane dialogPane = portDialog.getDialogPane();
            dialogPane.getStylesheets().add(finalStyle);

            portDialog.showAndWait().ifPresent(serverPort -> {
                serverThread = new Thread(() -> Server.startServer(Integer.parseInt(serverPort)));
                serverThread.setDaemon(true);
                serverThread.start();
            });
        });

        Button joinServerButton = new Button("Join Server");
        joinServerButton.setOnAction(e -> {
            TextInputDialog ipDialog = new TextInputDialog("localhost");
            ipDialog.setTitle("Join Server");
            ipDialog.setHeaderText(null);
            ipDialog.setContentText("Enter server IP:");
            DialogPane joinDialogPane = ipDialog.getDialogPane();
            joinDialogPane.getStylesheets().add(finalStyle);

            ipDialog.showAndWait().ifPresent(serverIP -> {
                TextInputDialog portDialog = new TextInputDialog("61000");
                portDialog.setTitle("Join Server");
                portDialog.setHeaderText(null);
                portDialog.setContentText("Enter server port:");
                DialogPane portDialogPane = portDialog.getDialogPane();
                portDialogPane.getStylesheets().add(finalStyle);

                portDialog.showAndWait().ifPresent(port -> {
                    String systemUsername = System.getProperty("user.name");
                    TextInputDialog nameDialog = new TextInputDialog(systemUsername);
                    nameDialog.setTitle("Join Server");
                    nameDialog.setHeaderText(null);
                    nameDialog.setContentText("Enter your username (default: " + systemUsername + "):");
                    DialogPane nameDialogPane = nameDialog.getDialogPane();
                    nameDialogPane.getStylesheets().add(finalStyle);

                    nameDialog.showAndWait().ifPresent(name -> {
                        if (name.isEmpty()) {
                            showAlert(Alert.AlertType.ERROR, "Username cannot be empty");
                            return;
                        }
                        username = name;
                        join(serverIP, Integer.parseInt(port), username);
                    });
                });
            });
        });

        Button exitButton = new Button("Exit");
        exitButton.setOnAction(e -> exit());

        root.getChildren().addAll(
            titleLabel,
            createServerButton,
            joinServerButton,
            exitButton
        );

        Scene scene = new Scene(root, 400, 300);
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
            logger.error("Invalid server IP entered.");
            showAlert(Alert.AlertType.ERROR, "Invalid server IP.");
            return;
        }

        handler = new ServerHandler(serverIP.trim(), port, username);

        if (!handler.isConnected()) {
            logger.error("Failed to connect to server at: {}:{}", serverIP, port);
            Platform.runLater(() ->
                showAlert(Alert.AlertType.ERROR, "Failed to connect to server at: " + serverIP + ":" + port)
            );
            return;
        }

        logger.info("Connected successfully to {}:{}", serverIP, port);
        Platform.runLater(() -> {
            try {
                LobbyGUI.setServerHandler(handler);
                LobbyGUI lobby = new LobbyGUI(stage);
                lobby.setUsername(username);
                lobby.show(new Stage());
                stage.hide();
            } catch (Exception ex) {
                logger.error("Couldn't launch LobbyGUI", ex);
                ex.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Error launching lobby: " + ex.getMessage());
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
