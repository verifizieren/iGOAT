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
import javafx.stage.Stage;
/**
 * Represents the GUI for the game lobby where players can chat and join games.
 * This class handles the lobby interface including chat functionality,
 * lobby listings, and player listings.
 */
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

    /**
     * Sets the server handler for communication with the game server.
     *
     * @param handler the ServerHandler instance to use for server communication
     */
    public static void setServerHandler(ServerHandler handler) {
        serverHandler = handler;
    }

    /**
     * Displays the lobby GUI.
     *
     * @param primaryStage the JavaFX stage to display the lobby on
     */
    public void show(Stage primaryStage) {
        VBox leftPanel = setupLeftPanel();
        VBox rightPanel = setupRightPanel();

        HBox mainLayout = new HBox(20, leftPanel, rightPanel);
        mainLayout.setPadding(new Insets(20));

        Scene scene = new Scene(mainLayout, 750, 500);
        primaryStage.setTitle("Lobby Menu");
        primaryStage.setScene(scene);
        primaryStage.show();

        initializeServerCommunication();
    }

    /**
     * Sets up the left panel of the GUI containing the list of lobbies and players.
     *
     * @return a VBox containing the left panel UI elements
     */
    private VBox setupLeftPanel() {
        Label lobbyListLabel = new Label("Available Lobbies");
        lobbyListLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");

        lobbyListView = new ListView<>();
        setupLobbyListViewEvents();

        playerListLabel = new Label("Players in Lobby");
        playerListLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        playerListView = new ListView<>();
        playerListView.setPrefHeight(150);

        VBox leftPanel = new VBox(10, lobbyListLabel, lobbyListView, playerListLabel, playerListView);
        leftPanel.setPadding(new Insets(10));
        leftPanel.setAlignment(Pos.TOP_LEFT);
        leftPanel.setPrefWidth(200);
        return leftPanel;
    }

    /**
     * Sets up the right panel of the GUI containing the chat interface and main action buttons.
     *
     * @return a VBox containing the right panel UI elements
     */
    private VBox setupRightPanel() {
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

        toggleChatButton = new Button("Switch to Global Chat");

        setupChatEvents();
        VBox buttonBox = setupButtonActions();

        VBox rightPanel = new VBox(10);
        rightPanel.setAlignment(Pos.TOP_CENTER);
        rightPanel.setPadding(new Insets(10));
        rightPanel.getChildren().addAll(
            buttonBox, chatModeLabel, messageArea, chatInput, sendButton, toggleChatButton
        );

        return rightPanel;
    }

    /**
     * Attaches double-click event handling for the lobby list,
     * allowing the user to join a lobby by selecting it.
     */
    private void setupLobbyListViewEvents() {
        lobbyListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                String selected = lobbyListView.getSelectionModel().getSelectedItem();
                if (selected != null && !selected.isEmpty()) {
                    String code = selected.split(" ")[0];
                    serverHandler.sendMessage("lobby:" + code);
                }
            }
        });
    }

    /**
     * Configures chat input, send button, and chat toggle button
     * to handle user chat interactions.
     */
    private void setupChatEvents() {
        sendButton.setOnAction(e -> sendChatMessage());

        chatInput.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                sendChatMessage();
            }
        });

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
    }

    /**
     * Sets up and returns the VBox containing the primary action buttons:
     * Start Game, Create Lobby, Exit Lobby, Change Name, and Exit.
     *
     * @return a VBox with all main action buttons
     */
    private VBox setupButtonActions() {
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

        VBox buttons = new VBox(10, startButton, createButton, leaveLobbyButton, nameButton, exitButton);
        buttons.setAlignment(Pos.CENTER);
        return buttons;
    }

    /**
     * Initializes communication with the server after the GUI is shown,
     * including sending initial connection messages and launching the
     * background message-receiving thread.
     */
    private void initializeServerCommunication() {
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
                        if (!isGlobalChat) {
                            Platform.runLater(() -> playerListView.getItems().clear());
                        } else {
                            serverHandler.sendMessage("getplayers:");
                        }
                    } else {
                        appendToMessageArea("Info: Du bist Lobby " + content + " beigetreten.");
                        serverHandler.sendMessage("getlobbyplayers:");
                    }
                    break;
                case "getlobbies":
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
                case "getplayers":
                    if (isGlobalChat) {
                        Platform.runLater(() -> {
                            String[] players = content.split(",");
                            playerListView.getItems().setAll(players);
                        });
                    }
                    break;
                case "getlobbyplayers":
                    if (!isGlobalChat) {
                        Platform.runLater(() -> {
                            String[] players = content.split(",");
                            playerListView.getItems().setAll(players);
                        });
                    }
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
