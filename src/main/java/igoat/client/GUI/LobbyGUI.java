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
    private TextField chatInput;
    private Button sendButton;
    private Button toggleChatButton;
    private Label chatModeLabel;
    private ListView<String> lobbyListView;
    private ListView<String> playerListView;
    private Label playerListLabel;

    private boolean isGlobalChat = false;
    private final int MAX_PLAYERS = 4;
    private final int lobbyRefreshTime = 3000;

    public static void setServerHandler(ServerHandler handler) {
        serverHandler = handler;
    }

    public void show(Stage primaryStage) {
        Label lobbyListLabel = new Label("Available Lobbies");
        lobbyListLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");

        lobbyListView = new ListView<>();
        lobbyListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                String selected = lobbyListView.getSelectionModel().getSelectedItem();
                if (selected != null && !selected.isEmpty()) {
                    String code = selected.split(" ")[0];
                    serverHandler.sendMessage("lobby:" + code);
                }
            }
        });

        playerListLabel = new Label("Players");
        playerListLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        playerListView = new ListView<>();
        playerListView.setPrefHeight(150);

        VBox leftPanel = new VBox(10);
        leftPanel.getChildren().addAll(lobbyListLabel, lobbyListView, playerListLabel, playerListView);
        leftPanel.setPadding(new Insets(10));
        leftPanel.setAlignment(Pos.TOP_LEFT);
        leftPanel.setPrefWidth(200);

        chatModeLabel = new Label("Lobby Chat");
        chatModeLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        messageArea = new TextArea();
        messageArea.setEditable(false);
        messageArea.setWrapText(true);
        messageArea.setPrefHeight(200);

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

        toggleChatButton = new Button("Switch to Global Chat");
        toggleChatButton.setOnAction(e -> {
            isGlobalChat = !isGlobalChat;
            toggleChatButton.setText(isGlobalChat ? "Switch to Lobby Chat" : "Switch to Global Chat");
            chatModeLabel.setText(isGlobalChat ? "Global Chat" : "Lobby Chat");
            appendToMessageArea("Now chatting in " + chatModeLabel.getText());
            if (serverHandler != null && serverHandler.isConnected()) {
                serverHandler.sendMessage(isGlobalChat ? "getplayers:" : "getlobbyplayers:");
            }
        });

        Button startButton = new Button("Start Game");
        Button createButton = new Button("Create Lobby");
        Button leaveLobbyButton = new Button("Exit Lobby");
        Button nameButton = new Button("Change Name");
        Button exitButton = new Button("Exit");

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
            }
        });

        leaveLobbyButton.setOnAction(e -> {
            if (serverHandler != null && serverHandler.isConnected()) {
                serverHandler.sendMessage("lobby:0");
                appendToMessageArea("You have left the lobby.");
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

        VBox rightPanel = new VBox(10);
        rightPanel.setAlignment(Pos.TOP_CENTER);
        rightPanel.setPadding(new Insets(10));
        rightPanel.getChildren().addAll(
            startButton, createButton, leaveLobbyButton, nameButton, exitButton,
            chatModeLabel, messageArea, chatInput, sendButton, toggleChatButton
        );

        HBox mainLayout = new HBox(20, leftPanel, rightPanel);
        mainLayout.setPadding(new Insets(20));

        Scene scene = new Scene(mainLayout, 750, 500);
        primaryStage.setTitle("Lobby Menu");
        primaryStage.setScene(scene);
        primaryStage.show();

        if (serverHandler != null && serverHandler.isConnected()) {
            serverHandler.sendMessage("connect:" + username);
            serverHandler.sendMessage("getlobbies:");
            serverHandler.sendMessage("getlobbyplayers:");

            chatInput.setDisable(false);
            sendButton.setDisable(false);

            Thread messageThread = new Thread(this::startMessageReceiver);
            messageThread.setDaemon(true);
            messageThread.start();

            Thread refreshThread = new Thread(() -> {
                while (running && serverHandler != null && serverHandler.isConnected()) {
                    serverHandler.sendMessage("getlobbies:");
                    serverHandler.sendMessage(isGlobalChat ? "getplayers:" : "getlobbyplayers:");
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
                String prefix = isGlobalChat ? "chat:" : "lobbychat:";
                serverHandler.sendMessage(prefix + text);
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
                case "lobbies":
                    Platform.runLater(() -> {
                        lobbyListView.getItems().clear();
                        String[] lobbies = content.split(",");
                        for (String entry : lobbies) {
                            String[] parts = entry.split("=");
                            if (parts.length == 2) {
                                String code = parts[0];
                                String playerCount = parts[1];
                                lobbyListView.getItems().add(code + " (" + playerCount + "/" + MAX_PLAYERS + ")");
                            }
                        }
                    });
                    break;
                case "players":
                case "lobbyplayers":
                    Platform.runLater(() -> {
                        String[] players = content.split(",");
                        playerListView.getItems().setAll(players);
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
}