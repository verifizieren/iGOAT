package igoat.client.GUI;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import igoat.client.Game;
import igoat.client.ServerHandler;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
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
    private static final Logger logger = LoggerFactory.getLogger(LobbyGUI.class);
    
    // Server communication
    private static ServerHandler serverHandler;
    private String username;
    private volatile boolean running = true;


    // Chat UI components
    private Stage stage;
    private Stage mainMenu;

    private TextArea messageArea;
    private TextField chatInput;
    private Button sendButton;
    private Button toggleChatButton;
    private Label chatModeLabel;

    // Lobby and player list components
    private ListView<String> lobbyListView;
    private ListView<String> playerListView;
    private Label playerListLabel;
    private Button readyButton;
    private boolean isReady = false;
    
    // Player ready status tracking
    private Map<String, Boolean> playerReadyStatus = new HashMap<>();
    private String currentLobbyCode = null;

    // Configuration constants
    private boolean isGlobalChat = true;
    private final int MAX_PLAYERS = 4;

    /**
     * Constructor for LobbyGUI
     * @param mainMenu The stage of the main menu. The main menu will be shown again after LobbyGUI closes.
     */
    public LobbyGUI(Stage mainMenu) {
        this.mainMenu = mainMenu;
    }

    /**
     * Sets the server handler for communication with the game server.
     *
     * @param handler the ServerHandler instance to use for server communication
     */
    public static void setServerHandler(ServerHandler handler) {
        serverHandler = handler;
    }

    /**
     * Sets the username
     * @param username new username
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Displays the lobby GUI.
     *
     * @param primaryStage the JavaFX stage to display the lobby on
     */
    public void show(Stage primaryStage) {
        stage = primaryStage;
        stage.setOnCloseRequest(event -> exit());

        logger.info("show() called. Setting up UI...");
        VBox leftPanel = setupLeftPanel();
        VBox rightPanel = setupRightPanel();

        HBox mainLayout = new HBox(20, leftPanel, rightPanel);
        mainLayout.setPadding(new Insets(20));

        Scene scene = new Scene(mainLayout, 750, 500);
        stage.setTitle("Lobby Menu");
        stage.setScene(scene);
        stage.show();

        logger.info("LobbyGUI displayed. Initializing server communication...");
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
        
        readyButton = new Button("Ready");
        readyButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
        readyButton.setOnAction(e -> toggleReadyStatus());
        readyButton.setDisable(true); 

        VBox leftPanel = new VBox(10, lobbyListLabel, lobbyListView, playerListLabel, playerListView, readyButton);
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
        chatModeLabel = new Label("Global Chat");
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

        toggleChatButton = new Button("Switch to Lobby Chat");

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
     * Toggles the ready status of the player and sends the ready command to the server.
     */
    private void toggleReadyStatus() {
        if (serverHandler != null && serverHandler.isConnected() && currentLobbyCode != null) {
            isReady = !isReady;
            updateReadyButton();
            
            if (isReady) {
                serverHandler.sendMessage("ready:");
                appendToMessageArea("You are now ready!");
            } else {
                // Hier koennte man unready machen
                appendToMessageArea("You are ready!");
            }
        }
    }
    
    /**
     * Updates the ready button appearance based on the current ready status.
     */
    private void updateReadyButton() {
        Platform.runLater(() -> {
            if (isReady) {
                readyButton.setText("Ready!");
                readyButton.setStyle("-fx-background-color: #FF5722; -fx-text-fill: white;");
            } else {
                readyButton.setText("Ready");
                readyButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
            }
        });
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
            updateChatUIForMode();
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
            if (serverHandler != null && serverHandler.isConnected() && currentLobbyCode != null) {
                serverHandler.sendMessage("startgame:");
            } else {
                appendToMessageArea("Error: Cannot start game. Make sure you are in a lobby and connected to the server.");
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
                isReady = false;
                updateReadyButton();
                readyButton.setDisable(true);
                currentLobbyCode = null;
                playerReadyStatus.clear();
            }
        });

        nameButton.setOnAction(event -> {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Change Username");
            dialog.setHeaderText("Enter a new username:");
            dialog.setContentText("Username:");

            dialog.showAndWait().ifPresent(name -> {
                if (!name.isBlank()) {
                    serverHandler.sendMessage("username:" + name);
                    // Username will be updated when server confirms the change
                }
            });
        });

        exitButton.setOnAction(event -> exit());

        VBox buttons = new VBox(10, startButton, createButton, leaveLobbyButton, nameButton, exitButton);
        buttons.setAlignment(Pos.CENTER);
        return buttons;
    }

    /**
     * Handles the exit process for the lobby GUI.
     * Stops the message receiver thread, sends exit message to server,
     * closes the connection, and returns to the main menu.
     */
    public void exit() {
        running = false;
        if (serverHandler != null) {
            serverHandler.sendMessage("exit");
            serverHandler.close();
        }
        stage.close();
        mainMenu.show();
    }

    /**
     * Initializes communication with the server after the GUI is shown,
     * including sending initial connection messages and launching the
     * background message-receiving thread.
     */
    private void initializeServerCommunication() {
        if (serverHandler != null && serverHandler.isConnected()) {
            logger.info("Initializing Server Communication. Sending getlobbies, getplayers.");
            serverHandler.sendMessage("getlobbies:");
            serverHandler.sendMessage("getplayers:");

            isGlobalChat = true;
            updateChatUIForMode();

            chatInput.setDisable(false);
            sendButton.setDisable(false);

            logger.info("Starting message receiver thread...");
            Thread messageThread = new Thread(this::startMessageReceiver);
            messageThread.setDaemon(true);
            messageThread.start();
        } else {
            logger.error("Cannot initialize server communication: ServerHandler is null or not connected.");
            // Optionally show an error alert to the user
             Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Lost connection to server. Please restart.", ButtonType.OK);
                alert.setHeaderText(null);
                alert.showAndWait();
             });
        }
    }

    /**
     * Sends a chat message to either the global or lobby chat.
     * The message is prefixed with either 'chat:' or 'lobbychat:' depending on the current chat mode.
     * After sending, the input field is cleared. Adds the message locally first for immediate display.
     */
    private void sendChatMessage() {
        if (serverHandler != null && serverHandler.isConnected()) {
            String prefix = isGlobalChat ? "chat:" : "lobbychat:";
            String text = chatInput.getText().trim();

            if (!text.isEmpty()) {
                // Get the confirmed nickname, fallback to username if needed
                String confirmedNickname = serverHandler.getConfirmedNickname();
                String localSender = (confirmedNickname != null) ? confirmedNickname : ((username != null) ? username : "You");

                if (!isGlobalChat && currentLobbyCode == null) {
                    appendToMessageArea("Error: You must be in a lobby to use Lobby Chat.");
                    chatInput.setText("");
                    return;
                }

                if (text.toLowerCase().startsWith("/whisper ")) {
                    String[] parts = text.split(" ", 3);
                    if (parts.length == 3) {
                        String targetUsername = parts[1];
                        String whisperMessageContent = parts[2];

                        if (targetUsername.equalsIgnoreCase(localSender)) {
                            appendToMessageArea("[System] You cannot whisper to yourself.");
                        } else {
                            String whisperMarker = String.format("[WHISPER->%s] ", targetUsername);
                            String messageWithMarker = whisperMarker + whisperMessageContent;
                            String messageToSend = prefix + messageWithMarker;

                            logger.info("Sending whisper via {}: {}", prefix, messageToSend);
                            serverHandler.sendMessage(messageToSend);

                            appendToMessageArea(String.format("[To %s]: %s", targetUsername, whisperMessageContent));
                        }
                    } else {
                        appendToMessageArea("[System] Usage: /whisper <username> <message>");
                    }
                } else {
                    String messageToSend = prefix + text;
                    String displayPrefix = isGlobalChat ? "[GLOBAL] " : "[LOBBY] ";
                    
                    appendToMessageArea(displayPrefix + localSender + ": " + text);
                    serverHandler.sendMessage(messageToSend);
                    logger.info("Sent {} message: {}", isGlobalChat ? "Global" : "Lobby", messageToSend);
                }

                chatInput.setText(""); // Clear input field after processing
            } else {
                 logger.error("Cannot send chat message: ServerHandler not available or connected.");
                 appendToMessageArea("Error: Not connected to server.");
            }
        }
    }
    /**
     * Starts a background thread to receive and display messages from the server.
     */
    private void startMessageReceiver() {
        logger.info("MessageReceiver thread started.");
        while (running && serverHandler != null && serverHandler.isConnected()) {
            String message = serverHandler.getMessage();
            if (message == null || message.isEmpty()) {
                 try { Thread.sleep(50); } catch (InterruptedException e) { Thread.currentThread().interrupt(); break; } // Avoid busy-waiting on null
                 continue;
            }

            logger.info("Raw message received: {}", message);

            String chatPrefix = null;
            if (message.startsWith("chat:")) {
                chatPrefix = "chat:";
            } else if (message.startsWith("lobbychat:")) {
                 chatPrefix = "lobbychat:";
            }

            if (chatPrefix != null) {
                 String chatData = message.substring(chatPrefix.length());
                 int firstColonIndex = chatData.indexOf(':');
                 if (firstColonIndex > 0 && firstColonIndex < chatData.length() - 1) {
                     String sender = chatData.substring(0, firstColonIndex);
                     String chatMessage = chatData.substring(firstColonIndex + 1);

                     String localNickname = serverHandler.getConfirmedNickname();
                     if (localNickname != null && localNickname.equals(sender)) {
                          logger.debug("Ignoring echo of own message from sender: {}", sender);
                          continue; 
                     }

                     final String whisperMarkerStart = "[WHISPER->";
                     if (chatMessage.startsWith(whisperMarkerStart)) {
                         int markerEnd = chatMessage.indexOf("]");
                         if (markerEnd > whisperMarkerStart.length()) {
                             String targetUser = chatMessage.substring(whisperMarkerStart.length(), markerEnd);
                             String whisperContent = chatMessage.substring(markerEnd + 2); // Skip "] "

                             if (localNickname != null && localNickname.equalsIgnoreCase(targetUser)) {
                                 logger.info("Received lobby whisper from {}: {}", sender, whisperContent);
                                 appendToMessageArea(String.format("[From %s]: %s", sender, whisperContent));
                             } else {
                                 logger.debug("Ignoring whisper not intended for this client (target: {}, local: {})", targetUser, localNickname);
                             }
                             continue;
                         }
                     }

                     logger.info("Parsed {} message - Sender: '{}', Message: '{}'", chatPrefix.substring(0, chatPrefix.length() - 1).toUpperCase(), sender, chatMessage);
                     String displayPrefix = chatPrefix.equals("chat:") ? "[GLOBAL] " : "[LOBBY] ";
                     appendToMessageArea(displayPrefix + sender + ": " + chatMessage);
                 } else {
                      logger.warn("Could not parse sender/message from {} data: {}", chatPrefix, chatData);
                      //appendToMessageArea("[System] " + chatData);
                 }
                 continue;
            }

            // Handle non-chat messages
            int colonIndex = message.indexOf(':');
            if (colonIndex == -1) {
                 logger.error("Invalid message format (no colon and not a known chat prefix): {}", message);
                 appendToMessageArea("[System] " + message); // Display unknown format messages
                 continue;
            }

            String type = message.substring(0, colonIndex).toLowerCase();
            String content = message.substring(colonIndex + 1);
            logger.info("Parsed type: {}, content: {}", type, content);

            switch (type) {
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
                        isReady = false;
                        updateReadyButton();
                        readyButton.setDisable(true);
                        currentLobbyCode = null;
                        playerReadyStatus.clear();
                        if (!isGlobalChat) {
                            isGlobalChat = true;
                            updateChatUIForMode();
                            serverHandler.sendMessage("getplayers:");
                        }
                    } else {
                        appendToMessageArea("Info: Du bist Lobby " + content + " beigetreten.");
                        currentLobbyCode = content;
                        readyButton.setDisable(false);
                        if (isGlobalChat) {
                            isGlobalChat = false;
                            updateChatUIForMode();
                        }
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
                            logger.info("Received getlobbyplayers: {}", content);
                            String[] players = content.isEmpty() ? new String[0] : content.split(",");
                            playerListView.getItems().clear();
                            
                            Set<String> namesInUpdate = new HashSet<>();
                            for (String player : players) {
                                namesInUpdate.add(player.trim());
                                boolean isPlayerReady = playerReadyStatus.getOrDefault(player.trim(), false);
                                String playerDisplay = player.trim() + (isPlayerReady ? " ✓" : "");
                                playerListView.getItems().add(playerDisplay);
                            }

                            Set<String> playersToRemoveFromStatus = new HashSet<>(playerReadyStatus.keySet());
                            playersToRemoveFromStatus.removeAll(namesInUpdate);
                            if (!playersToRemoveFromStatus.isEmpty()) {
                                logger.info("Removing players from ready status map who left lobby: {}", playersToRemoveFromStatus);
                                for (String nameToRemove : playersToRemoveFromStatus) {
                                    playerReadyStatus.remove(nameToRemove);
                                }
                                logger.info("playerReadyStatus map after removal: {}", playerReadyStatus);
                            }

                            logger.info("Checking if all players are ready after getlobbyplayers update...");
                            checkAllPlayersReady(players);
                        });
                    }
                    break;
                case "ready_status":
                    logger.info("Entering 'ready_status' case.");
                    String[] readyInfo = content.split(",");
                    if (readyInfo.length == 2) {
                        String playerName = readyInfo[0].trim();
                        boolean ready = Boolean.parseBoolean(readyInfo[1].trim());
                        logger.info("Parsed ready_status for {}: {}", playerName, ready);
                        playerReadyStatus.put(playerName, ready);
                        logger.info("playerReadyStatus map updated: {}", playerReadyStatus);

                        Platform.runLater(() -> {
                            logger.info("Calling updatePlayerListWithReadyStatus for {}", playerName);
                            updatePlayerListWithReadyStatus();
                            String[] currentPlayersArray = playerListView.getItems().stream()
                                   .map(item -> item.replace(" ✓", ""))
                                   .toArray(String[]::new);
                             logger.info("Checking all players ready immediately after ready_status update for {} ...", playerName);
                             checkAllPlayersReady(currentPlayersArray);
                        });
                    } else {
                         logger.error("Invalid ready_status content format: {}", content);
                    }
                    break;
                case "game_started":
                    logger.info("Received game_started message!");
                    appendToMessageArea("Game started!");
                    Platform.runLater(() -> {
                        if (currentLobbyCode == null) {
                            appendToMessageArea("Error: Cannot start game without being in a lobby.");
                            return;
                        }
                        readyButton.setDisable(true);

                        logger.info("Gathering player list for game initialization...");
                        List<String> playerNames = new ArrayList<>();
                        for (String item : playerListView.getItems()) {
                            playerNames.add(item.replace(" ✓", ""));
                        }
                        logger.info("Players in lobby: {}", playerNames);

                        try {
                            logger.info("Creating and initializing Game instance...");
                            Game game = new Game(this);
                            game.initialize(serverHandler, username, currentLobbyCode, playerNames);
                            logger.info("Game instance initialized.");

                            logger.info("Creating new stage for game...");
                            Stage gameStage = new Stage();
                            logger.info("Calling game.start()...");

                            game.start(gameStage);
                            logger.info("game.start() returned. Closing lobby window...");

                            stage.hide();
                            running = false; // Stop the message receiver thread
                            logger.info("Lobby window closed and message receiver stopped.");
                        } catch (Exception ex) {
                            logger.error("Error starting game", ex);
                            appendToMessageArea("Error starting game: " + ex.getMessage());
                            stage.show();
                        }
                    });
                    break;
                default:
                    //appendToMessageArea("[Server] " + message);
                    break;
            }

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                 logger.error("MessageReceiver thread interrupted.");
                break;
            }
        }
         logger.info("MessageReceiver thread stopped.");
    }
    
    /**
     * Updates the player list view to reflect the current ready status of all players.
     * Each ready player's name is suffixed with a checkmark (✓).
     * This method is only applicable in lobby chat mode.
     */
    private void updatePlayerListWithReadyStatus() {
        if (isGlobalChat) return;
        logger.info("Updating player list view based on ready statuses map: {}", playerReadyStatus);

        List<String> currentItems = new ArrayList<>(playerListView.getItems());
        playerListView.getItems().clear();

        for (String item : currentItems) {
            String playerName = item.replace(" ✓", "").trim();
            boolean isPlayerReady = playerReadyStatus.getOrDefault(playerName, false);
            String playerDisplay = playerName + (isPlayerReady ? " ✓" : "");
            playerListView.getItems().add(playerDisplay);
        }
         logger.info("Player list view updated: {}", playerListView.getItems());
    }
    
    /**
     * Checks if all players in the lobby are ready to start the game.
     * If all players are ready, sends the 'startgame:' command to the server.
     * The game will only start if:
     * - There is at least one player in the lobby
     * - All players have marked themselves as ready
     * - The server connection is active
     *
     * @param players Array of player names currently in the lobby (from server message)
     */
    private void checkAllPlayersReady(String[] players) {
         if (currentLobbyCode == null) {
              logger.info("Not in a lobby, skipping check.");
              return;
         }
         logger.info("Checking readiness for players: {}", Arrays.toString(players));
         logger.info("Current ready status map: {}", playerReadyStatus);

         if (players.length < 1) { 
              logger.info("Not enough players to start (<1).");
              return;
         }

         boolean allReady = true;
         for (String player : players) {
             String trimmedPlayerName = player.trim();
             if (!playerReadyStatus.getOrDefault(trimmedPlayerName, false)) {
                 allReady = false;
                 logger.info("Player {} not ready (map status: {})", trimmedPlayerName, playerReadyStatus.getOrDefault(trimmedPlayerName, false));
                 break;
             } else {
                 logger.info("Player {} is ready.", trimmedPlayerName);
             }
         }

         if (allReady && players.length == 4) {
             logger.info("All players are ready! Waiting for game start command...");
             appendToMessageArea("All players are ready! The lobby creator can now start the game.");
         } else {
             logger.info("Not all players are ready yet.");
         }
     }
    
    /**
     * Appends a message to the chat message area.
     * This method is thread-safe as it uses Platform.runLater().
     *
     * @param message the message to append to the chat area
     */
    private void appendToMessageArea(String message) {
        Platform.runLater(() -> messageArea.appendText(message + "\n"));
    }

    /**
     * Updates the chat UI elements (labels, buttons) based on the current chat mode (isGlobalChat).
     * Also displays a notification message about the mode change.
     */
    private void updateChatUIForMode() {
        Platform.runLater(() -> {
            String modeName = isGlobalChat ? "Global" : "Lobby";
            String toggleButtonText = isGlobalChat ? "Switch to Lobby Chat" : "Switch to Global Chat";
            String playerListText = isGlobalChat ? "All Connected Players" : "Players in Lobby";

            chatModeLabel.setText(modeName + " Chat");
            toggleChatButton.setText(toggleButtonText);
            playerListLabel.setText(playerListText);
            //appendToMessageArea("Now chatting in " + modeName + " Chat");

            boolean inLobby = currentLobbyCode != null;
            readyButton.setDisable(!inLobby);
        });
    }
}
