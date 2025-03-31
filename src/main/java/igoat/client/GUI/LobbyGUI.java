package igoat.client.GUI;

import igoat.client.ServerHandler;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;

public class LobbyGUI {

    private static ServerHandler serverHandler;
    private String username = System.getProperty("user.name");
    private volatile boolean running = true;

    private TextArea messageArea;
    private TextField lobbyCodeField;
    private Button startButton;
    private Button createButton;
    private TextField chatInput;
    private Button sendButton;
    private ListView<String> lobbyListView;

    private final int MAX_PLAYERS = 4;
    private final int lobbyRefreshTime = 3000;

    public static void setServerHandler(ServerHandler handler) {
        serverHandler = handler;
    }

    public void show(Stage primaryStage) {
        Label label = new Label("Lobby");
        label.setFont(new Font("Arial", 30));

        messageArea = new TextArea();
        messageArea.setEditable(false);
        messageArea.setWrapText(true);
        messageArea.setPrefHeight(200);

        lobbyCodeField = new TextField();
        lobbyCodeField.setVisible(false);
        lobbyCodeField.setManaged(false);

        startButton = new Button("Start Game");
        createButton = new Button("Create Lobby");
        Button exitButton = new Button("Exit");
        Button nameButton = new Button("Change Name");

        startButton.setDisable(true);
        createButton.setDisable(true);

        chatInput = new TextField();
        chatInput.setPromptText("Type a message...");
        chatInput.setDisable(true);

        sendButton = new Button("Send");
        sendButton.setDisable(true);

        sendButton.setOnAction(e -> sendChatMessage());
        chatInput.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                sendChatMessage();
            }
        });

        startButton.setOnAction(event -> {
            String selected = lobbyListView.getSelectionModel().getSelectedItem();
            if (selected != null && !selected.isEmpty() && serverHandler != null && serverHandler.isConnected()) {
                String code = selected.split(" ")[0];
                serverHandler.sendMessage("lobby:" + code);
            }
        });

        createButton.setOnAction(event -> {
            if (serverHandler != null && serverHandler.isConnected()) {
                serverHandler.sendMessage("newlobby:");
                System.out.println("Sent: newlobby:");
            }
        });

        nameButton.setOnAction(event -> {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Change Username");
            dialog.setHeaderText("Enter a new username:");
            dialog.setContentText("Username:");

            dialog.showAndWait().ifPresent(name -> {
                if (!name.isBlank()) {
                    username = name;
                    serverHandler.sendMessage("connect:" + username);
                    appendToMessageArea("Username changed to: " + username);
                }
            });
        });

        exitButton.setOnAction(event -> {
            running = false;
            if (serverHandler != null) {
                serverHandler.sendMessage("exit");
                serverHandler.close();
            }
            Platform.exit();
            System.exit(0);
        });

        lobbyListView = new ListView<>();
        lobbyListView.setPrefWidth(150);

        lobbyListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                String selectedLobby = lobbyListView.getSelectionModel().getSelectedItem();
                if (selectedLobby != null && !selectedLobby.isEmpty() && serverHandler != null && serverHandler.isConnected()) {
                    String code = selectedLobby.split(" ")[0];
                    serverHandler.sendMessage("lobby:" + code);
                    appendToMessageArea("Joining lobby " + code + "...");
                }
            }
        });

        VBox leftPanel = new VBox(10, new Label("Available Lobbies:"), lobbyListView);
        leftPanel.setPadding(new Insets(10));
        leftPanel.setAlignment(Pos.TOP_LEFT);

        VBox rightPanel = new VBox(10);
        rightPanel.setAlignment(Pos.CENTER);
        rightPanel.setPadding(new Insets(10));
        rightPanel.getChildren().addAll(
            label,
            lobbyCodeField,
            startButton,
            createButton,
            nameButton,
            exitButton,
            messageArea,
            chatInput,
            sendButton
        );

        HBox mainLayout = new HBox(10, leftPanel, rightPanel);
        mainLayout.setPadding(new Insets(10));

        Scene scene = new Scene(mainLayout, 650, 500);
        primaryStage.setTitle("Lobby Menu");
        primaryStage.setScene(scene);
        primaryStage.show();

        if (serverHandler != null && serverHandler.isConnected()) {
            serverHandler.sendMessage("connect:" + username);
            serverHandler.sendMessage("getlobbies:");

            startButton.setDisable(false);
            createButton.setDisable(false);
            chatInput.setDisable(false);
            sendButton.setDisable(false);

            Thread messageThread = new Thread(this::startMessageReceiver);
            messageThread.setDaemon(true);
            messageThread.start();

            Thread refreshThread = new Thread(() -> {
                while (running && serverHandler != null && serverHandler.isConnected()) {
                    serverHandler.sendMessage("getlobbies:");
                    try {
                        Thread.sleep(lobbyRefreshTime);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            });
            refreshThread.setDaemon(true);
            refreshThread.start();
        }
    }

    private void sendChatMessage() {
        String text = chatInput.getText().trim();
        if (!text.isEmpty()) {
            if (serverHandler != null && serverHandler.isConnected()) {
                if (text.startsWith("/whisper ")) {
                    String[] parts = text.substring(9).split(" ", 2);
                    if (parts.length == 2) {
                        serverHandler.sendMessage("whisper:" + parts[0] + "," + parts[1]);
                    } else {
                        appendToMessageArea("Usage: /whisper <user> <message>");
                    }
                } else {
                    serverHandler.sendMessage("chat:" + text);
                }
            }
            chatInput.setText("");
        }
    }

    private void startMessageReceiver() {
        while (running && serverHandler != null && serverHandler.isConnected()) {
            String message = serverHandler.getMessage();
            if (message == null || message.isEmpty()) continue;

            int colonIndex = message.indexOf(':');
            if (colonIndex == -1) continue;

            String type = message.substring(0, colonIndex);
            String content = message.substring(colonIndex + 1);

            switch (type) {
                case "chat":
                    int commaIndex = content.indexOf(',');
                    if (commaIndex != -1) {
                        String sender = content.substring(0, commaIndex);
                        String chatMessage = content.substring(commaIndex + 1);
                        appendToMessageArea(sender + ": " + chatMessage);
                    } else {
                        appendToMessageArea(content);
                    }
                    break;
                case "error":
                    appendToMessageArea("Error: " + content);
                    break;
                case "confirm":
                    appendToMessageArea("Info: " + content);
                    break;
                case "lobby":
                    if (content.equals("0")) {
                        appendToMessageArea("Info: Du hast die Lobby verlassen.");
                    } else {
                        appendToMessageArea("Info: Du bist Lobby " + content + " beigetreten.");
                    }
                    break;
                case "role":
                    appendToMessageArea("Info: Du hast Rolle " + roleName(content) + " erhalten.");
                    serverHandler.sendMessage("role:" + content);
                    break;
                case "catch":
                    appendToMessageArea(content + " wurde gefangen.");
                    break;
                case "revive":
                    appendToMessageArea(content + " wurde wiederbelebt.");
                    break;
                case "lobbies":
                    Platform.runLater(() -> {
                        if (content.isBlank()) {
                            lobbyListView.getItems().clear();
                            return;
                        }

                        String[] lobbies = content.split(",");
                        lobbyListView.getItems().clear();

                        for (String entry : lobbies) {
                            String[] parts = entry.split("=");
                            if (parts.length == 2) {
                                String code = parts[0];
                                String playerCount = parts[1];
                                lobbyListView.getItems().add(code + " (" + playerCount + "/" + MAX_PLAYERS + ")");
                            } else {
                                lobbyListView.getItems().add(entry);
                            }
                        }
                    });
                    break;
                default:
                    appendToMessageArea("[Server] " + message);
                    break;
            }

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void appendToMessageArea(String message) {
        Platform.runLater(() -> messageArea.appendText(message + "\n"));
    }

    private String roleName(String code) {
        return switch (code) {
            case "0" -> "Ziege";
            case "1" -> "iGOAT";
            case "2" -> "Wächter";
            default -> "Unbekannt";
        };
    }
}
