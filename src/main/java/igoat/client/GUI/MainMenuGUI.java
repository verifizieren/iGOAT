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
import igoat.client.Game;

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

        Button startButton = new Button("Start");
        startButton.setOnAction(e -> {
            try {
                Game game = new Game();
                Stage gameStage = new Stage();
                game.start(gameStage);
                ((Stage) startButton.getScene().getWindow()).close();
            } catch (Exception ex) {
                showAlert(Alert.AlertType.ERROR, "Failed to start game: " + ex.getMessage());
            }
        });


        Button createServerButton = new Button("Create Server");
        createServerButton.setOnAction(e -> new Thread(() -> Server.startServer(5555)).start());

        Button joinServerButton = new Button("Join Server");
        joinServerButton.setOnAction(e -> {
            if (username.isEmpty()) {
                username = getSystemName();
            }
            TextInputDialog ipDialog = new TextInputDialog();
            ipDialog.setTitle("Join Server");
            ipDialog.setHeaderText(null);
            ipDialog.setContentText("Enter server IP:");

            ipDialog.showAndWait().ifPresent(serverIP -> {
                if (!serverIP.trim().isEmpty()) {
                    new Thread(() -> {
                        handler = new ServerHandler(serverIP.trim(), 5555);
                        if (handler.isConnected()) {
                            Platform.runLater(() -> {
                                try {
                                    LobbyGUI.setServerHandler(handler);
                                    LobbyGUI lobby = new LobbyGUI();
                                    lobby.show(new Stage());
                                    ((Stage) joinServerButton.getScene().getWindow()).close();
                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                }
                            });
                        } else {
                            Platform.runLater(() ->
                                showAlert(Alert.AlertType.ERROR, "Failed to connect to server at: " + serverIP)
                            );
                        }
                    }).start();
                } else {
                    showAlert(Alert.AlertType.ERROR, "Invalid server IP.");
                }
            });
        });

        Button exitButton = new Button("Exit");
        exitButton.setOnAction(e -> {
            if (handler != null) {
                handler.close();
            }
            Platform.exit();
        });

        root.getChildren().addAll(
            titleLabel,
            startButton,
            createServerButton,
            joinServerButton,
            exitButton
        );

        Scene scene = new Scene(root, 400, 300);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void showAlert(Alert.AlertType type, String message) {
        Alert alert = new Alert(type, message, ButtonType.OK);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    public static String getSystemName() {
        return System.getProperty("user.name");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
