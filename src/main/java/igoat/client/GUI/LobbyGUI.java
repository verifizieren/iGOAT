package igoat.client.GUI;

import igoat.client.ScreenUtil;

import igoat.client.Sprite;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javafx.scene.control.DialogPane;
import javafx.scene.control.TextInputDialog;
import javafx.collections.FXCollections;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Rectangle;
import javafx.scene.paint.Color;
import javafx.stage.StageStyle;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.Priority;
import javafx.animation.FadeTransition;
import javafx.animation.Animation;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.util.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import igoat.client.Game;
import igoat.client.ServerHandler;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.beans.property.SimpleStringProperty;
import igoat.client.GameSpectator;

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
    private SoundButton sendButton;
    private SoundButton toggleChatButton;
    private Label chatModeLabel;

    private final SettingsWindow settings = SettingsWindow.getInstance();
    private final ManualWindow manual = ManualWindow.getInstance();
    private final String style = getClass().getResource("/CSS/UI.css").toExternalForm();

    // Lobby and player list components
    private ListView<String> lobbyListView;
    private ListView<String> playerListView;
    private Label playerListLabel;
    private SoundButton readyButton;
    private boolean isReady = false;

    // Player ready status tracking
    private Map<String, Boolean> playerReadyStatus = new HashMap<>();
    private String currentLobbyCode = null;

    // Configuration constants
    private boolean isGlobalChat = true;
    private static final int MAX_PLAYERS = 4;

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
        stage.getIcons().add(MainMenuGUI.icon);
        stage.setOnCloseRequest(event -> exit());

        VBox leftPanel = setupLeftPanel();
        VBox rightPanel = setupRightPanel();

        // Settings button
        SoundButton settingsButton = new SoundButton("");
        settingsButton.setGraphic(new ImageView(new Sprite("/sprites/cog.png", 32, 32)));
        settingsButton.setOnAction(event -> {settings.open(stage);});
        settingsButton.setStyle("-fx-padding: 0px 0px;");
        HBox topBar = new HBox(10, settingsButton);
        topBar.setAlignment(Pos.TOP_RIGHT);

        // Manual button
        SoundButton manualButton = new SoundButton("");
        manualButton.setGraphic(new ImageView(new Sprite("/sprites/igoat_idle.png", 32, 32)));
        manualButton.setOnAction(event -> {manual.open();});
        manualButton.setStyle("-fx-padding: 0px 0px;");
        HBox bottomBar = new HBox(20, manualButton);
        bottomBar.setAlignment(Pos.TOP_RIGHT);

        HBox mainLayout = new HBox(20, leftPanel, rightPanel, topBar, bottomBar);
        mainLayout.setPadding(new Insets(20));
        mainLayout.setAlignment(Pos.CENTER);

        Scene scene = new Scene(mainLayout, 750, 600);
        scene.getStylesheets().add(style);
        scene.getStylesheets().add(getClass().getResource("/CSS/LobbyBackground.css").toExternalForm());

        ScreenUtil.moveStageToCursorScreen(stage, mainLayout.getPrefWidth() > 0 ? mainLayout.getPrefWidth() : 750, mainLayout.getPrefHeight() > 0 ? mainLayout.getPrefHeight() : 600);
        stage.setTitle("Lobby Menu");
        stage.setScene(scene);
        stage.show();

        initializeServerCommunication();
    }

    /**
     * Sets up the left panel of the GUI containing the list of lobbies and players.
     *
     * @return a VBox containing the left panel UI elements
     */
    private VBox setupLeftPanel() {
        Label lobbyListLabel = new Label("Available Lobbies");
        lobbyListLabel.setStyle("-fx-font-weight: bold;");

        lobbyListView = new ListView<>();
        lobbyListView.setStyle("-fx-font-fill: #000000");
        setupLobbyListViewEvents();

        playerListLabel = new Label("Players in Lobby");
        playerListLabel.setStyle("-fx-font-weight: bold;");

        playerListView = new ListView<>();
        playerListView.setPrefHeight(300);
        
        readyButton = new SoundButton("Ready");
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
        chatModeLabel.setStyle("-fx-font-weight: bold;");

        messageArea = new TextArea();
        messageArea.setEditable(false);
        messageArea.setWrapText(true);
        messageArea.setPrefHeight(1000);

        chatInput = new TextField();
        chatInput.setPromptText("Type a message...");
        chatInput.setDisable(true);

        sendButton = new SoundButton("Send");
        sendButton.setDisable(true);

        toggleChatButton = new SoundButton("Switch to Lobby Chat");

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
                serverHandler.sendMessage("unready:");
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
        SoundButton startButton = new SoundButton("Start Game");
        SoundButton createButton = new SoundButton("Create Lobby");
        SoundButton leaveLobbyButton = new SoundButton("Exit Lobby");
        SoundButton nameButton = new SoundButton("Change Name");
        SoundButton exitButton = new SoundButton("Exit");
        SoundButton highscoresButton = new SoundButton("Highscores");

        highscoresButton.setOnAction(event -> {
            if (serverHandler != null && serverHandler.isConnected()) {
                serverHandler.sendMessage("gethighscores:");
            } else {
                appendToMessageArea("Error: Cannot fetch highscores. Not connected.");
            }
        });

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
            SoundButton.addDialogSound(dialog);

            DialogPane nameDialogPane = dialog.getDialogPane();
            nameDialogPane.getStylesheets().add(style);

            Stage dialogStage = (Stage) dialog.getDialogPane().getScene().getWindow();
            ScreenUtil.moveStageToCursorScreen(dialogStage, dialogStage.getWidth() > 0 ? dialogStage.getWidth() : 350, dialogStage.getHeight() > 0 ? dialogStage.getHeight() : 200);
            dialog.showAndWait().ifPresent(name -> {
                // sanitize string
                name = name.replaceAll("[\\s=:,]", "");
                if (!name.isBlank()) {
                    serverHandler.sendMessage("username:" + name);
                    // Username will be updated when server confirms the change
                }
            });
        });

        exitButton.setOnAction(event -> exit());

        VBox buttons = new VBox(10, startButton, createButton, leaveLobbyButton, nameButton, exitButton, highscoresButton);
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
    public void initializeServerCommunication() {
        if (serverHandler != null && serverHandler.isConnected()) {
            serverHandler.sendMessage("getlobbies:");
            serverHandler.sendMessage("getplayers:");
            serverHandler.sendMessage(isGlobalChat ? "getplayers:" : "getlobbyplayers:");

            running = true;
            isReady = false;
            updateReadyButton();
            playerReadyStatus.replaceAll((key, value) -> false);

            isGlobalChat = currentLobbyCode == null;
            updateChatUIForMode();

            chatInput.setDisable(false);
            sendButton.setDisable(false);

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
            chatInput.setText(""); // clear chat field

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
            }
        }
    }
    /**
     * Starts a background thread to receive and display messages from the server.
     */
    private void startMessageReceiver() {
        while (running) {
            if (serverHandler == null || !serverHandler.isConnected()) {
                appendToMessageArea("Connection lost. Please reconnect.");
                break;
            }

            String message = serverHandler.getMessage();
            if (message == null || message.isEmpty()) {
                 try { Thread.sleep(50); } catch (InterruptedException e) { Thread.currentThread().interrupt(); break; } // Avoid busy-waiting on null
                 continue;
            }

            if (message.startsWith("highscores:")) {
                String content = message.substring("highscores:".length());
                String type = "highscores";
                int colonIndex = message.indexOf(':');
                type = message.substring(0, colonIndex).toLowerCase();
                content = message.substring(colonIndex + 1);
                
                switch (type) {
                    case "highscores":
                        displayEnhancedHighscores(content);
                        break;
                    default:
                        break;
                }
                continue;
            }

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
                     //no sender, probably server message
                     appendToMessageArea("[System] " + chatData);
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
            //logger.info("Parsed type: {}, content: {}", type, content);

            switch (type) {
                case "error":
                    appendToMessageArea("Error: " + content);
                    if (content.trim().startsWith("Lobby ") && content.trim().endsWith("is full")) {
                        String[] parts = content.trim().split(" ");
                        if (parts.length >= 2) {
                            String code = parts[1];
                            serverHandler.sendMessage("spectate:" + code);
                            Platform.runLater(() -> {
                                try {
                                    GameSpectator spectator = new GameSpectator(this);
                                    spectator.initialize(serverHandler, code);
                                    Stage spectatorStage = new Stage();
                                    spectator.start(spectatorStage);
                                    stage.hide();
                                    settings.close();
                                    manual.close();
                                    running = false;
                                } catch (Exception ex) {
                                    logger.error("Error starting spectator mode", ex);
                                    appendToMessageArea("Error starting spectator mode: " + ex.getMessage());
                                    stage.show();
                                }
                            });
                        }
                    }
                    if (content.trim().equals("Game is already in progress")) {
                        logger.info("Received 'Game is already in progress'. currentLobbyCode: {}", currentLobbyCode);
                        String codeToSpectate = currentLobbyCode;
                        if (codeToSpectate == null) {
                            String selected = lobbyListView.getSelectionModel().getSelectedItem();
                            if (selected != null && !selected.isEmpty()) {
                                codeToSpectate = selected.split(" ")[0];
                                logger.info("Extracted code from selected lobby: {}", codeToSpectate);
                            }
                        }
                        if (codeToSpectate != null) {
                            String spectateMsg = "spectate:" + codeToSpectate;
                            logger.info("Sending spectate message: {}", spectateMsg);
                            serverHandler.sendMessage(spectateMsg);
                        } else {
                            logger.warn("No lobby code available to spectate.");
                        }
                        final String finalCodeToSpectate = codeToSpectate;
                        Platform.runLater(() -> {
                            try {
                                GameSpectator spectator = new GameSpectator(this);
                                spectator.initialize(serverHandler, finalCodeToSpectate);
                                Stage spectatorStage = new Stage();
                                spectator.start(spectatorStage);
                                stage.hide();
                                settings.close();
                                manual.close();
                                running = false;
                            } catch (Exception ex) {
                                logger.error("Error starting spectator mode", ex);
                                appendToMessageArea("Error starting spectator mode: " + ex.getMessage());
                                stage.show();
                            }
                        });
                    }
                    break;
                case "confirm":
                    appendToMessageArea("Info: " + content);
                    break;
                case "lobby":
                    if (content.equals("0")) {
                        appendToMessageArea("Info: You have left the lobby.");
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
                        logger.info("Joined lobby {}", content);
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
                                for (String nameToRemove : playersToRemoveFromStatus) {
                                    playerReadyStatus.remove(nameToRemove);
                                }
                            }
                            checkAllPlayersReady(players);
                        });
                    }
                    break;
                case "ready_status":
                    String[] readyInfo = content.split(",");
                    if (readyInfo.length == 2) {
                        String playerName = readyInfo[0].trim();
                        boolean ready = Boolean.parseBoolean(readyInfo[1].trim());
                        playerReadyStatus.put(playerName, ready);

                        Platform.runLater(() -> {
                            updatePlayerListWithReadyStatus();
                            String[] currentPlayersArray = playerListView.getItems().stream()
                                   .map(item -> item.replace(" ✓", ""))
                                   .toArray(String[]::new);
                             checkAllPlayersReady(currentPlayersArray);
                        });
                    } else {
                         logger.error("Invalid ready_status content format: {}", content);
                    }
                    break;
                case "game_started":
                    appendToMessageArea("Game started!");
                    Platform.runLater(() -> {
                        if (currentLobbyCode == null) {
                            appendToMessageArea("Error: Cannot start game without being in a lobby.");
                            return;
                        }
                        readyButton.setDisable(true);

                        try {
                            Game game = new Game(this);
                            game.initialize(serverHandler, username, currentLobbyCode);

                            Stage gameStage = new Stage();
                            logger.info("New game started!");

                            game.start(gameStage);

                            stage.hide();
                            settings.close();
                            manual.close();
                            running = false; // Stop the message receiver thread
                        } catch (Exception ex) {
                            logger.error("Error starting game", ex);
                            appendToMessageArea("Error starting game: " + ex.getMessage());
                            stage.show();
                        }
                    });
                    break;
                case "results":
                    Platform.runLater(() -> {
                        Stage stage2 = new Stage();
                        stage2.setTitle("Past Game Results");
                        VBox root = new VBox(10);
                        root.setPadding(new Insets(10));
                        for (String line : content.split(Pattern.quote("\\n"))) {
                            if (line.isBlank()) continue;
                            String ts = extract(line, "timestamp");
                            String lb = extract(line, "lobby");
                            boolean res = Boolean.parseBoolean(extract(line, "result"));
                            int start = line.indexOf("\"players\":");
                            String playersJson = line.substring(start + 11, line.lastIndexOf("]"));
                            List<PlayerRow> playerRows = new ArrayList<>();
                            for (String p : playersJson.split(Pattern.quote("},{"))) {
                                String name = extract(p, "name");
                                String role = extract(p, "role");
                                
                                String actualOutcome;
                                if (res) { 
                                    actualOutcome = role.equals("GUARD") ? "Won" : "Lost";
                                } else {
                                    actualOutcome = role.equals("GUARD") ? "Lost" : "Won";
                                }
                                
                                playerRows.add(new PlayerRow(name, role, actualOutcome));
                            }
                            Label header = new Label(ts + " | Lobby " + lb + " | " + (res ? "Guard Won" : "Goat Won"));
                            TableView<PlayerRow> table = new TableView<>();
                            TableColumn<PlayerRow, String> nameCol = new TableColumn<>("Player");
                            nameCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getName()));
                            TableColumn<PlayerRow, String> roleCol = new TableColumn<>("Role");
                            roleCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getRole()));
                            TableColumn<PlayerRow, String> outCol = new TableColumn<>("Outcome");
                            outCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getOutcome()));
                            table.getColumns().setAll(nameCol, roleCol, outCol);
                            table.setItems(FXCollections.observableArrayList(playerRows));
                            root.getChildren().addAll(header, table);
                        }
                        ScrollPane sp = new ScrollPane(root);
                        Scene sc = new Scene(sp, 600, 400);
                        stage2.setScene(sc);
                        stage2.show();
                    });
                    break;
                case "highscores":
                    Platform.runLater(() -> {
                        Stage highscoreStage = new Stage();
                        highscoreStage.setTitle("iGoat Leaderboard");
                        highscoreStage.initStyle(StageStyle.UNDECORATED);
                        
                        String processedContent = content.replace("<br>", "\n");
                        
                        StackPane root = new StackPane();
                        root.setStyle("-fx-background-color: linear-gradient(to bottom, #1a2a3a, #0d1520);");
                        
                        Rectangle patternOverlay = new Rectangle(800, 600);
                        patternOverlay.setFill(new Color(0, 0, 0, 0.05));
                        patternOverlay.setOpacity(0.3);
                        
                        VBox mainContent = new VBox(25);
                        mainContent.setPadding(new Insets(30, 40, 40, 40));
                        mainContent.setMaxWidth(720);
                        mainContent.setMaxHeight(540);
                        mainContent.setStyle("-fx-background-color: rgba(255, 255, 255, 0.1); " +
                                          "-fx-background-radius: 15; -fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.4), 10, 0, 0, 5);");
                        
                        HBox header = new HBox(15);
                        header.setAlignment(Pos.CENTER);
                        
                        Label trophyIcon = new Label("🏆");
                        trophyIcon.setStyle("-fx-font-size: 40px; -fx-text-fill: gold;");
                        
                        Label title = new Label("HALL OF FAME");
                        title.setStyle("-fx-font-size: 36px; -fx-font-weight: bold; -fx-text-fill: white; " +
                                      "-fx-effect: dropshadow(gaussian, gold, 10, 0.6, 0, 0);");

                        SoundButton closeButton = new SoundButton("×");
                        closeButton.setStyle("-fx-font-size: 20px; -fx-background-color: transparent; -fx-text-fill: white; -fx-cursor: hand;");
                        closeButton.setOnAction(e -> highscoreStage.close());
                        
                        Region spacer = new Region();
                        HBox.setHgrow(spacer, Priority.ALWAYS);
                        
                        header.getChildren().addAll(trophyIcon, title, spacer, closeButton);
                        
                        Label subtitle = new Label("The Fastest Players in the Lab");
                        subtitle.setStyle("-fx-font-size: 16px; -fx-font-style: italic; -fx-text-fill: #aaccff;");
                        subtitle.setOpacity(0.8);
                        
                        FadeTransition pulse = new FadeTransition(Duration.seconds(2), subtitle);
                        pulse.setFromValue(0.7);
                        pulse.setToValue(1.0);
                        pulse.setCycleCount(Animation.INDEFINITE);
                        pulse.setAutoReverse(true);
                        pulse.play();
                        
                        TabPane tabPane = new TabPane();
                        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
                        tabPane.setStyle("-fx-background-color: transparent; -fx-tab-min-width: 120;");
                        
                        String[] sections = processedContent.split("\n\n");
                        
                        Tab guardTab = createHighscoreTab("GUARD", "#4a90e2", sections[0], "👮");
                        
                        Tab goatTab = createHighscoreTab("GOAT", "#50c878", 
                                                      sections.length > 1 ? sections[1] : "No goat highscores yet.", "🐐");
                        
                        tabPane.getTabs().addAll(guardTab, goatTab);
                        
                        mainContent.getChildren().addAll(header, subtitle, tabPane);
                        
                        root.getChildren().addAll(patternOverlay, mainContent);
                        
                        final Delta dragDelta = new Delta();
                        root.setOnMousePressed(mouseEvent -> {
                            dragDelta.x = highscoreStage.getX() - mouseEvent.getScreenX();
                            dragDelta.y = highscoreStage.getY() - mouseEvent.getScreenY();
                        });
                        root.setOnMouseDragged(mouseEvent -> {
                            highscoreStage.setX(mouseEvent.getScreenX() + dragDelta.x);
                            highscoreStage.setY(mouseEvent.getScreenY() + dragDelta.y);
                        });
                        
                        Scene scene = new Scene(root, 750, 600);
                        scene.setFill(Color.TRANSPARENT);
                        highscoreStage.setScene(scene);
                        
                        scene.getStylesheets().add(getClass().getResource("/CSS/highscores.css").toExternalForm());
                        
                        highscoreStage.setOpacity(0);
                        highscoreStage.show();
                        
                        FadeTransition fadeIn = new FadeTransition(Duration.millis(400), root);
                        fadeIn.setFromValue(0);
                        fadeIn.setToValue(1);
                        fadeIn.play();
                        
                        Timeline timeline = new Timeline();
                        KeyFrame key = new KeyFrame(Duration.millis(400),
                                new KeyValue(highscoreStage.opacityProperty(), 1));
                        timeline.getKeyFrames().add(key);
                        timeline.play();
                        
                        logger.info("Displayed premium highscores window");
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
    }
    
    /**
     * Updates the player list view to reflect the current ready status of all players.
     * Each ready player's name is suffixed with a checkmark (✓).
     * This method is only applicable in lobby chat mode.
     */
    private void updatePlayerListWithReadyStatus() {
        if (isGlobalChat) return;
        List<String> currentItems = new ArrayList<>(playerListView.getItems());
        playerListView.getItems().clear();

        for (String item : currentItems) {
            String playerName = item.replace(" ✓", "").trim();
            boolean isPlayerReady = playerReadyStatus.getOrDefault(playerName, false);
            String playerDisplay = playerName + (isPlayerReady ? " ✓" : "");
            playerListView.getItems().add(playerDisplay);
        }
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
              return;
         }

         if (players.length < 1) { 
              logger.info("Not enough players to start (<1).");
              return;
         }

         boolean allReady = true;
         for (String player : players) {
             String trimmedPlayerName = player.trim();
             if (!playerReadyStatus.getOrDefault(trimmedPlayerName, false)) {
                 allReady = false;
                 break;
             }
         }

         if (allReady && players.length == 4) {
             appendToMessageArea("All players are ready! The lobby creator can now start the game.");
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

    private String extract(String line, String key) {
        int startIndex = line.indexOf("\"" + key + "\":");
        if (startIndex == -1) return "";
        startIndex += key.length() + 3;
        int endIndex = line.indexOf(",", startIndex);
        int braceEnd = line.indexOf("}", startIndex);
        if (endIndex == -1 || (braceEnd != -1 && braceEnd < endIndex)) {
            endIndex = braceEnd;
        }
        if (endIndex == -1) {
            endIndex = line.length();
        }
        if (startIndex >= endIndex) return "";
        String value = line.substring(startIndex, endIndex).trim();
        value = value.replaceAll("^[\\\\\"\\{\\[]+|[\\\\\"\\}\\]]+$", "");
        return value;
    }

    /**
     * Displays the enhanced highscores UI with animations and styling.
     * 
     * @param content The highscore content to display
     */
    private void displayEnhancedHighscores(String content) {
        Platform.runLater(() -> {
            Stage highscoreStage = new Stage();
            highscoreStage.setTitle("iGoat Highscores");
            
            String processedContent = content.replace("<br>", "\n");
            
            StackPane root = new StackPane();
            root.setStyle("-fx-background-color: #ffffff; -fx-border-color: #000000; -fx-border-width: 2px;");
            
            VBox mainContent = new VBox(15);
            mainContent.setPadding(new Insets(20, 30, 30, 30));
            mainContent.setMaxWidth(720);
            mainContent.setMaxHeight(540);
            
            HBox header = new HBox(10);
            header.setAlignment(Pos.CENTER);
            
            Label trophyIcon = new Label("#");
            trophyIcon.setStyle("-fx-font-size: 24px; -fx-text-fill: #000000; -fx-font-family: 'Jersey 10', 'Courier New', monospace;");
            
            Label title = new Label("HIGHSCORES");
            title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #000000; " +
                          "-fx-font-family: 'Jersey 10', 'Courier New', monospace;");

            SoundButton closeButton = new SoundButton("X");
            closeButton.getStyleClass().add("close-button");
            closeButton.setOnAction(e -> highscoreStage.close());
            
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            
            header.getChildren().addAll(trophyIcon, title, spacer, closeButton);
            
            Label subtitle = new Label("The Fastest Players");
            subtitle.setStyle("-fx-font-size: 16px; -fx-font-style: italic; -fx-text-fill: #000000; -fx-font-family: 'Jersey 10', 'Courier New', monospace;");
            
            TabPane tabPane = new TabPane();
            tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
            tabPane.getStyleClass().add("tab-pane");
            
            String[] sections = processedContent.split("\n\n");
            
            Tab guardTab = createHighscoreTab("GUARD", "#b8b6b6", sections[0], "G");
            
            Tab goatTab = createHighscoreTab("GOAT", "#b8b6b6", 
                                           sections.length > 1 ? sections[1] : "No goat highscores yet.", "G");
            
            tabPane.getTabs().addAll(guardTab, goatTab);
            
            mainContent.getChildren().addAll(header, subtitle, tabPane);
            
            root.getChildren().add(mainContent);
            
            final Delta dragDelta = new Delta();
            root.setOnMousePressed(mouseEvent -> {
                dragDelta.x = highscoreStage.getX() - mouseEvent.getScreenX();
                dragDelta.y = highscoreStage.getY() - mouseEvent.getScreenY();
            });
            root.setOnMouseDragged(mouseEvent -> {
                highscoreStage.setX(mouseEvent.getScreenX() + dragDelta.x);
                highscoreStage.setY(mouseEvent.getScreenY() + dragDelta.y);
            });

            Scene scene = new Scene(root, 700, 550);
            highscoreStage.setScene(scene);
            scene.getStylesheets().add(getClass().getResource("/CSS/highscores.css").toExternalForm());
            ScreenUtil.moveStageToCursorScreen(highscoreStage, 700, 550);
            highscoreStage.show();
            logger.info("Displayed enhanced highscores window");
        });
    }
    /**
     * Creates a tab for displaying highscores.
     * 
     * @param title The title of the tab
     * @param content The highscore content to display
     * @param icon The icon to display in the tab (emoji)
     * @return A Tab containing the highscore data
     */
    private Tab createHighscoreTab(String title, String color, String content, String icon) {
        Tab tab = new Tab(icon + " " + title);
        tab.setStyle("-fx-background-color: " + color + "; -fx-text-base-color: white;");
        
        if (content == null || content.trim().isEmpty() || content.contains("No highscores yet")) {
            Label noDataLabel = new Label("No " + title + " highscores available yet.");
            noDataLabel.setStyle("-fx-font-size: 16px; -fx-font-style: italic; -fx-text-fill: #aaccff; -fx-padding: 30px;");
            VBox container = new VBox(noDataLabel);
            container.setAlignment(Pos.CENTER);
            tab.setContent(container);
            return tab;
        }
        
        List<HighscoreEntry> entries = parseHighscoreContent(content);
        
        TableView<HighscoreEntry> table = new TableView<>();
        table.setItems(FXCollections.observableArrayList(entries));
        
        TableColumn<HighscoreEntry, String> rankCol = new TableColumn<>("Rank");
        rankCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getRank()));
        rankCol.setPrefWidth(60);
        
        TableColumn<HighscoreEntry, String> nameCol = new TableColumn<>("Player");
        nameCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getName()));
        nameCol.setPrefWidth(200);
        
        TableColumn<HighscoreEntry, String> timeCol = new TableColumn<>("Time");
        timeCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getTime()));
        timeCol.setPrefWidth(100);
        
        TableColumn<HighscoreEntry, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDate()));
        dateCol.setPrefWidth(160);
        
        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        
        table.getColumns().setAll(rankCol, nameCol, timeCol, dateCol);
        
        VBox container = new VBox(10);
        container.setPadding(new Insets(15));
        container.getChildren().add(table);
        
        tab.setContent(container);
        return tab;
    }
    
    /**
     * Parses highscore content into a list of HighscoreEntry objects.
     * 
     * @param content The content to parse
     * @return A list of HighscoreEntry objects
     */
    private List<HighscoreEntry> parseHighscoreContent(String content) {
        List<HighscoreEntry> entries = new ArrayList<>();
        String[] lines = content.split("\n");
        
        for (String line : lines) {
            if (line.contains("===") || line.contains("---") || line.trim().isEmpty()) {
                continue;
            }
            
            if (line.matches("\\d+\\..*")) {
                try {
                    String rank = line.substring(0, line.indexOf(".") + 1);
                    
                    String remaining = line.substring(line.indexOf(".") + 2);
                    String name = remaining.substring(0, remaining.indexOf(" - "));
                    
                    remaining = remaining.substring(remaining.indexOf("Time: ") + 6);
                    String time = remaining.substring(0, remaining.indexOf(" - "));
                    
                    String date = remaining.substring(remaining.indexOf("Date: ") + 6);
                    
                    entries.add(new HighscoreEntry(rank, name, time, date));
                } catch (Exception e) {
                    logger.warn("Could not parse highscore line: {}", line);
                }
            }
        }
        
        return entries;
    }
    
    /**
     * Helper class for tracking mouse drag position.
     */
    private static class Delta {
        double x, y;
    }
    
    /**
     * Creates a visually appealing section for displaying highscores
     * @param title The title of the section (e.g., "Guard Highscores")
     * @param color The color theme for the section
     * @param content The content to parse and display
     * @return A VBox containing the formatted highscore section
     */
    private VBox createHighscoreSection(String title, String color, String content) {
        VBox section = new VBox(10);
        section.setPadding(new Insets(15));
        section.setStyle("-fx-background-color: white; -fx-border-color: " + color + "; -fx-border-width: 2; -fx-border-radius: 5; -fx-background-radius: 5;");
        
        Label sectionTitle = new Label(title);
        sectionTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
        
        if (content.contains("No") && content.contains("highscores yet")) {
            Label noScores = new Label("No highscores recorded yet");
            noScores.setStyle("-fx-font-style: italic; -fx-text-fill: #666666;");
            section.getChildren().addAll(sectionTitle, noScores);
            return section;
        }
        
        TableView<HighscoreEntry> table = new TableView<>();
        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        table.setStyle("-fx-border-color: transparent;");
        
        // Create columns
        TableColumn<HighscoreEntry, String> rankCol = new TableColumn<>("Rank");
        rankCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getRank()));
        rankCol.setMaxWidth(60);
        rankCol.setMinWidth(60);
        
        TableColumn<HighscoreEntry, String> nameCol = new TableColumn<>("Player");
        nameCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getName()));
        
        TableColumn<HighscoreEntry, String> timeCol = new TableColumn<>("Time");
        timeCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getTime()));
        timeCol.setMaxWidth(100);
        timeCol.setMinWidth(100);
        
        TableColumn<HighscoreEntry, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDate()));
        dateCol.setMaxWidth(160);
        dateCol.setMinWidth(160);
        
        table.getColumns().setAll(rankCol, nameCol, timeCol, dateCol);
        
        List<HighscoreEntry> entries = new ArrayList<>();
        String[] lines = content.split("\n");
        
        for (String line : lines) {
            if (line.contains("===") || line.contains("---")) {
                continue;
            }
            
            if (line.matches("\\d+\\..*")) {
                try {
                    String rank = line.substring(0, line.indexOf(".") + 1);
                    
                    String remaining = line.substring(line.indexOf(".") + 2);
                    String name = remaining.substring(0, remaining.indexOf(" - "));
                    
                    remaining = remaining.substring(remaining.indexOf("Time: ") + 6);
                    String time = remaining.substring(0, remaining.indexOf(" - "));
                    
                    String date = remaining.substring(remaining.indexOf("Date: ") + 6);
                    
                    entries.add(new HighscoreEntry(rank, name, time, date));
                } catch (Exception e) {
                    logger.warn("Could not parse highscore line: {}", line);
                }
            }
        }
        
        table.setItems(FXCollections.observableArrayList(entries));
        
        section.getChildren().addAll(sectionTitle, table);
        return section;
    }
    
    /**
     * Helper class for storing highscore entry data
     */
    private static class HighscoreEntry {
        private final String rank;
        private final String name;
        private final String time;
        private final String date;
        
        public HighscoreEntry(String rank, String name, String time, String date) {
            this.rank = rank;
            this.name = name;
            this.time = time;
            this.date = date;
        }
        
        public String getRank() { return rank; }
        public String getName() { return name; }
        public String getTime() { return time; }
        public String getDate() { return date; }
    }

    public static class PlayerRow {
        private String name;
        private String role;
        private String outcome;

        public PlayerRow(String name, String role, String outcome) {
            this.name = name;
            this.role = role;
            this.outcome = outcome;
        }

        public String getName() {
            return name;
        }

        public String getRole() {
            return role;
        }

        public String getOutcome() {
            return outcome;
        }
    }

    public Stage getStage() {
        return stage;
    }
}
