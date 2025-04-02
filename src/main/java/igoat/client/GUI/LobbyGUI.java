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

/**
 * Represents the GUI for the game lobby where players can chat and join games.
 * This class handles the lobby interface including chat functionality,
 * lobby listings, and player listing for global and lobby.
 */
public class LobbyGUI {

    // Server communication
    private static ServerHandler serverHandler;
    private String username = System.getProperty("user.name");
    private volatile boolean running = true;

    // Chat UI components
    private TextArea messageArea;
    private TextField chatInput;
    private Button sendButton;
    private Button toggleChatButton;
    private Label chatModeLabel;

    // Lobby and player list components
    private ListView<String> lobbyListView;
    private ListView<String> playerListView;
    private Label playerListLabel;

    // Configuration constants
    private boolean isGlobalChat = false;
    private final int MAX_PLAYERS = 4;

    /**
     * Sets the server handler for communication with the game server.
     *
     * @param handler the ServerHandler instance to use for server communication
     */
    public static void setServerHandler(ServerHandler handler) {
        serverHandler = handler;
    }

    /**
     * Displays the lobby GUI
     *
     * @param primaryStage the JavaFX stage to display the lobby on
     */
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

        playerListLabel = new Label("Players in Lobby");
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
            playerListLabel.setText(isGlobalChat ? "All Connected Players" : "Players in Lobby");
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

        }
    }

    /**
     * Sends a chat message to either the global or lobby chat based on current mode.
     */
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

    /**
     * Starts a background thread to receive and display messages from the server.
     */
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
                        Platform.runLater(() -> playerListView.getItems().clear());
                    } else {
                        appendToMessageArea("Info: Du bist Lobby " + content + " beigetreten.");
                    }
                    break;
                case "getlobbies": // updates the lobby list
                    Platform.runLater(() -> {
                        lobbyListView.getItems().clear();
                        String[] lobbies = content.split(",");
                        for (String entry : lobbies) {
                            String[] parts = entry.split("=");
                            if (parts.length == 2) {
                                String code = parts[0];
                                String[] infoParts = parts[1].split(" ", 2);
                                if (infoParts.length == 2) {
                                    String playerCount = infoParts[0];
                                    String state = infoParts[1];
                                    lobbyListView.getItems().add(code + " (" + playerCount + ") " + state);
                                }
                            }
                        }
                    });
                    break;
                case "getplayers": // all connected Users
                case "getlobbyplayers": // Only Users in the current lobby
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

    /**
     * Appends a message to the chat area.
     *
     * @param message the message to append to the chat area
     */
    private void appendToMessageArea(String message) {
        Platform.runLater(() -> messageArea.appendText(message + "\n"));
    }
}