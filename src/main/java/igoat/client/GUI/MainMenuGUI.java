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

public class MainMenuGUI extends Application {

    private ServerHandler handler;
    private String username = System.getProperty("user.name");

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("iGOAT");

        VBox root = new VBox(15);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(20));

        Label titleLabel = new Label("iGOAT");
        titleLabel.setFont(new Font("Arial", 24));

        Button createServerButton = new Button("Create Server");
        createServerButton.setOnAction(e -> {
            System.out.println("[MainMenuGUI] Create Server button clicked. Starting server on port 61000...");
            new Thread(() -> Server.startServer(61000)).start();
        });

        Button joinServerButton = new Button("Join Server");
        joinServerButton.setOnAction(e -> {
            System.out.println("[MainMenuGUI] Join Server button clicked.");
            if (username.isEmpty()) {
                username = getSystemName();
            }
            TextInputDialog ipDialog = new TextInputDialog("localhost");
            ipDialog.setTitle("Join Server");
            ipDialog.setHeaderText(null);
            ipDialog.setContentText("Enter server IP:");

            ipDialog.showAndWait().ifPresent(serverIP -> {
                if (!serverIP.trim().isEmpty()) {
                    System.out.println("[MainMenuGUI] Attempting to connect to server: " + serverIP.trim() + ":61000");
                    new Thread(() -> {
                        handler = new ServerHandler(serverIP.trim(), 61000);
                        if (handler.isConnected()) {
                            System.out.println("[MainMenuGUI] Successfully connected to server.");
                            Platform.runLater(() -> {
                                try {
                                    System.out.println("[MainMenuGUI] Setting ServerHandler and launching LobbyGUI...");
                                    LobbyGUI.setServerHandler(handler);
                                    LobbyGUI lobby = new LobbyGUI();
                                    lobby.show(new Stage());
                                    System.out.println("[MainMenuGUI] LobbyGUI shown. Closing MainMenuGUI.");
                                    ((Stage) joinServerButton.getScene().getWindow()).close();
                                } catch (Exception ex) {
                                    System.err.println("[MainMenuGUI] Error launching LobbyGUI: " + ex.getMessage());
                                    ex.printStackTrace();
                                    showAlert(Alert.AlertType.ERROR, "Error launching lobby: " + ex.getMessage());
                                }
                            });
                        } else {
                            System.err.println("[MainMenuGUI] Failed to connect to server at: " + serverIP);
                            Platform.runLater(() ->
                                showAlert(Alert.AlertType.ERROR, "Failed to connect to server at: " + serverIP)
                            );
                        }
                    }).start();
                } else {
                    System.err.println("[MainMenuGUI] Invalid server IP entered.");
                    showAlert(Alert.AlertType.ERROR, "Invalid server IP.");
                }
            });
        });

        Button exitButton = new Button("Exit");
        exitButton.setOnAction(e -> {
            System.out.println("[MainMenuGUI] Exit button clicked.");
            if (handler != null) {
                handler.close();
            }
            Platform.exit();
        });

        root.getChildren().addAll(
            titleLabel,
            createServerButton,
            joinServerButton,
            exitButton
        );

        Scene scene = new Scene(root, 400, 300);
        primaryStage.setScene(scene);
        primaryStage.show();
        System.out.println("[MainMenuGUI] Main menu displayed.");
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
        System.out.println("[MainMenuGUI] Launching application...");
        launch(args);
    }
}
