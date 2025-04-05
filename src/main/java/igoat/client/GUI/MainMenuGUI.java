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
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainMenuGUI extends Application {
    private static final Logger logger = LoggerFactory.getLogger(MainMenuGUI.class);

    private ServerHandler handler;
    private String username = System.getProperty("user.name");
    private Thread serverThread;
    private Stage stage;

    @Override
    public void start(Stage primaryStage) {
        stage = primaryStage;
        primaryStage.setTitle("iGOAT");
        primaryStage.setOnCloseRequest(event -> exit());

        VBox root = new VBox(15);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(20));

        Label titleLabel = new Label("iGOAT");
        titleLabel.setFont(new Font("Arial", 24));

        Button createServerButton = new Button("Create Server");
        createServerButton.setOnAction(e -> {
            logger.info("Create Server button clicked. Starting server on port 61000...");
            serverThread = new Thread(() -> Server.startServer(61000));
            serverThread.setDaemon(true);
            serverThread.start();
        });

        Button joinServerButton = new Button("Join Server");
        joinServerButton.setOnAction(e -> {
            logger.info("Join Server button clicked.");
            if (username.isEmpty()) {
                username = getSystemName();
            }

            TextInputDialog ipDialog = new TextInputDialog("localhost");
            ipDialog.setTitle("Join Server");
            ipDialog.setHeaderText(null);
            ipDialog.setContentText("Enter server IP:");

            ipDialog.showAndWait().ifPresent(serverIP -> {
                join(serverIP);
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
        logger.info("Main menu displayed.");
    }

    /**
     * Establishes a connection to the server and launches the lobby GUI
     * @param serverIP The server IP address
     */
    private void join(String serverIP) {
        if (serverIP.isEmpty()) {
            logger.error("Invalid server IP entered.");
            showAlert(Alert.AlertType.ERROR, "Invalid server IP.");
            return;
        }

        logger.info("Attempting to connect to server: {}:61000", serverIP.trim());
        handler = new ServerHandler(serverIP.trim(), 61000);

        if (!handler.isConnected()) {
            logger.error("Failed to connect to server at: {}", serverIP);
            Platform.runLater(() ->
                showAlert(Alert.AlertType.ERROR, "Failed to connect to server at: " + serverIP)
            );
            return;
        }

        logger.info("Successfully connected to server.");
        Platform.runLater(() -> {
            try {
                logger.info("Setting ServerHandler and launching LobbyGUI...");
                LobbyGUI.setServerHandler(handler);
                LobbyGUI lobby = new LobbyGUI(stage);
                lobby.show(new Stage());
                logger.info("LobbyGUI shown. Closing MainMenuGUI.");
                stage.hide();
            } catch (Exception ex) {
                logger.error("Couldn't launch LobbyGUI", ex);
                ex.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Error launching lobby: " + ex.getMessage());
            }
        });
    }

    private void exit() {
        logger.info("Exit button clicked.");
        if (handler != null) {
            handler.close();
        }
        Platform.exit();
    }

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

    public static String getSystemName() {
        return System.getProperty("user.name");
    }

    public static void main(String[] args) {
        logger.info("Launching application...");
        launch(args);
    }
}
