package igoat.client.GUI;

import igoat.client.Game;
import igoat.client.GameSpectator;
import igoat.client.LanguageManager;
import igoat.client.ScreenUtil;
import igoat.client.ServerHandler;
import igoat.client.SoundManager;
import igoat.client.Sprite;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents the GUI for the game lobby where players can chat and join games. This class handles
 * the lobby interface including chat functionality, lobby listings, and player listings.
 */
public class LobbyGUI {

    private static final Logger logger = LoggerFactory.getLogger(LobbyGUI.class);
    private static final LanguageManager lang = LanguageManager.getInstance();

    // Server communication
    private static ServerHandler serverHandler;
    private String username;
    private volatile boolean running = true;

    // Chat UI components
    private Stage stage;
    private final Stage mainMenu;

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
    private final Map<String, Boolean> playerReadyStatus = new HashMap<>();
    private String currentLobbyCode = null;

    // Configuration constants
    private boolean isGlobalChat = true;

    /**
     * Constructor for LobbyGUI
     *
     * @param mainMenu The stage of the main menu. The main menu will be shown again after LobbyGUI
     *                 closes.
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
     *
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
        settingsButton.setOnAction(event -> {
            settings.open(stage);
        });
        settingsButton.setStyle("-fx-padding: 0px 0px;");
        HBox topBar = new HBox(10, settingsButton);
        topBar.setAlignment(Pos.TOP_RIGHT);

        // Manual button
        SoundButton manualButton = new SoundButton("");
        manualButton.setGraphic(new ImageView(new Sprite("/sprites/igoat_idle.png", 32, 32)));
        manualButton.setOnAction(event -> {
            manual.open();
        });
        manualButton.setStyle("-fx-padding: 0px 0px;");
        HBox bottomBar = new HBox(20, manualButton);
        bottomBar.setAlignment(Pos.TOP_RIGHT);

        HBox mainLayout = new HBox(20, leftPanel, rightPanel, topBar, bottomBar);
        mainLayout.setPadding(new Insets(20));
        mainLayout.setAlignment(Pos.CENTER);

        Scene scene = new Scene(mainLayout, 750, 600);
        scene.getStylesheets().add(style);
        scene.getStylesheets()
            .add(getClass().getResource("/CSS/LobbyBackground.css").toExternalForm());

        ScreenUtil.moveStageToCursorScreen(stage,
            mainLayout.getPrefWidth() > 0 ? mainLayout.getPrefWidth() : 750,
            mainLayout.getPrefHeight() > 0 ? mainLayout.getPrefHeight() : 600);
        stage.setTitle(lang.get("lobby.title"));
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
        Label lobbyListLabel = new Label(lang.get("lobby.lobbyList"));
        lobbyListLabel.setStyle("-fx-font-weight: bold;");

        lobbyListView = new ListView<>();
        lobbyListView.setStyle("-fx-font-fill: #000000");
        setupLobbyListViewEvents();

        playerListLabel = new Label(lang.get("lobby.playerLabelLobby"));
        playerListLabel.setStyle("-fx-font-weight: bold;");

        playerListView = new ListView<>();
        playerListView.setPrefHeight(300);

        readyButton = new SoundButton(lang.get("lobby.ready"));
        readyButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
        readyButton.setOnAction(e -> toggleReadyStatus());
        readyButton.setDisable(true);

        VBox leftPanel = new VBox(10, lobbyListLabel, lobbyListView, playerListLabel,
            playerListView, readyButton);
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
        chatModeLabel = new Label(lang.get("lobby.globalChat"));
        chatModeLabel.setStyle("-fx-font-weight: bold;");

        messageArea = new TextArea();
        messageArea.setEditable(false);
        messageArea.setWrapText(true);
        messageArea.setPrefHeight(1000);

        chatInput = new TextField();
        chatInput.setPromptText(lang.get("lobby.chatPrompt"));
        chatInput.setDisable(true);

        sendButton = new SoundButton(lang.get("lobby.send"));
        sendButton.setDisable(true);

        toggleChatButton = new SoundButton(lang.get("lobby.switchLobby"));

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
                appendToMessageArea(lang.get("lobby.readyMSG"));
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
     * Attaches double-click event handling for the lobby list, allowing the user to join a lobby by
     * selecting it.
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
     * Configures chat input, send button, and chat toggle button to handle user chat interactions.
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
            toggleChatButton.setText(
                isGlobalChat ? lang.get("lobby.switchGlobal") : lang.get("lobby.switchLobby"));
            playerListLabel.setText(isGlobalChat ? lang.get("lobby.playerLabelGlobal")
                : lang.get("lobby.playerLabelLobby"));
            if (serverHandler != null && serverHandler.isConnected()) {
                serverHandler.sendMessage(isGlobalChat ? "getplayers:" : "getlobbyplayers:");
            }
            updateChatUIForMode();
        });
    }

    /**
     * Sets up and returns the VBox containing the primary action buttons: Start Game, Create Lobby,
     * Exit Lobby, Change Name, and Exit.
     *
     * @return a VBox with all main action buttons
     */
    private VBox setupButtonActions() {
        SoundButton startButton = new SoundButton(lang.get("lobby.startGame"));
        SoundButton createButton = new SoundButton(lang.get("lobby.createLobby"));
        SoundButton leaveLobbyButton = new SoundButton(lang.get("lobby.exitLobby"));
        SoundButton nameButton = new SoundButton(lang.get("lobby.changeName"));
        SoundButton exitButton = new SoundButton(lang.get("main.exit"));
        SoundButton highscoresButton = new SoundButton(lang.get("lobby.highscores"));

        highscoresButton.setOnAction(event -> {
            if (serverHandler != null && serverHandler.isConnected()) {
                serverHandler.sendMessage("gethighscores:");
            } else {
                appendToMessageArea(lang.get("lobby.highscoreError"));
            }
        });

        startButton.setOnAction(event -> {
            if (serverHandler != null && serverHandler.isConnected() && currentLobbyCode != null) {
                serverHandler.sendMessage("startgame:");
            } else {
                appendToMessageArea(lang.get("lobby.startError"));
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
                appendToMessageArea(lang.get("lobby.leftLobby"));
                isReady = false;
                updateReadyButton();
                readyButton.setDisable(true);
                currentLobbyCode = null;
                playerReadyStatus.clear();
            }
        });

        nameButton.setOnAction(event -> {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle(lang.get("lobby.changeName"));
            dialog.setHeaderText(lang.get("lobby.newNamePrompt"));
            dialog.setContentText(lang.get("lobby.usernameLabel"));
            SoundButton.addDialogSound(dialog);

            DialogPane nameDialogPane = dialog.getDialogPane();
            nameDialogPane.getStylesheets().add(style);

            Stage dialogStage = (Stage) dialog.getDialogPane().getScene().getWindow();
            ScreenUtil.moveStageToCursorScreen(dialogStage,
                dialogStage.getWidth() > 0 ? dialogStage.getWidth() : 350,
                dialogStage.getHeight() > 0 ? dialogStage.getHeight() : 200);
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

        VBox buttons = new VBox(10, startButton, createButton, leaveLobbyButton, nameButton,
            exitButton, highscoresButton);
        buttons.setAlignment(Pos.CENTER);
        return buttons;
    }

    /**
     * Handles the exit process for the lobby GUI. Stops the message receiver thread, sends exit
     * message to server, closes the connection, and returns to the main menu.
     */
    public void exit() {
        running = false;
        if (serverHandler != null) {
            serverHandler.sendMessage("exit");
            serverHandler.close();
        }
        SoundManager.getInstance().stopAll();
        stage.close();
        mainMenu.show();
    }

    /**
     * Initializes communication with the server after the GUI is shown, including sending initial
     * connection messages and launching the background message-receiving thread.
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
            logger.error(
                "Cannot initialize server communication: ServerHandler is null or not connected.");
            // Optionally show an error alert to the user
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR, lang.get("lobby.lostConnection"),
                    ButtonType.OK);
                alert.setHeaderText(null);
                alert.showAndWait();
            });
        }
    }

    /**
     * Sends a chat message to either the global or lobby chat. The message is prefixed with either
     * 'chat:' or 'lobbychat:' depending on the current chat mode. After sending, the input field is
     * cleared. Adds the message locally first for immediate display.
     */
    private void sendChatMessage() {
        if (serverHandler != null && serverHandler.isConnected()) {
            String prefix = isGlobalChat ? "chat:" : "lobbychat:";
            String text = chatInput.getText().trim();
            chatInput.setText(""); // clear chat field

            if (!text.isEmpty()) {
                // Get the confirmed nickname, fallback to username if needed
                String confirmedNickname = serverHandler.getConfirmedNickname();
                String localSender = (confirmedNickname != null) ? confirmedNickname
                    : ((username != null) ? username : "You");

                if (!isGlobalChat && currentLobbyCode == null) {
                    appendToMessageArea(lang.get("lobby.lobbyChatError"));
                    chatInput.setText("");
                    return;
                }

                if (text.toLowerCase().startsWith("/whisper ")) {
                    String[] parts = text.split(" ", 3);
                    if (parts.length == 3) {
                        String targetUsername = parts[1];
                        String whisperMessageContent = parts[2];

                        if (targetUsername.equalsIgnoreCase(localSender)) {
                            appendToMessageArea(lang.get("lobby.whisperError"));
                        } else {
                            String whisperMarker = String.format("[WHISPER->%s] ", targetUsername);
                            String messageWithMarker = whisperMarker + whisperMessageContent;
                            String messageToSend = prefix + messageWithMarker;

                            logger.info("Sending whisper via {}: {}", prefix, messageToSend);
                            serverHandler.sendMessage(messageToSend);

                            appendToMessageArea(String.format("[To %s]: %s", targetUsername,
                                whisperMessageContent));
                        }
                    } else {
                        appendToMessageArea(lang.get("lobby.whisperUsage"));
                    }
                } else {
                    String messageToSend = prefix + text;
                    String displayPrefix =
                        isGlobalChat ? lang.get("lobby.global") : lang.get("lobby.lobby");

                    appendToMessageArea(displayPrefix + localSender + ": " + text);
                    serverHandler.sendMessage(messageToSend);
                    logger.info("Sent {} message: {}", isGlobalChat ? "Global" : "Lobby",
                        messageToSend);
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
                appendToMessageArea(lang.get("lobby.lostConnection"));
                break;
            }

            String message = serverHandler.getMessage();
            if (message == null || message.isEmpty()) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } // Avoid busy-waiting on null
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
                            String targetUser = chatMessage.substring(whisperMarkerStart.length(),
                                markerEnd);
                            String whisperContent = chatMessage.substring(
                                markerEnd + 2); // Skip "] "

                            if (localNickname != null && localNickname.equalsIgnoreCase(
                                targetUser)) {
                                logger.info("Received lobby whisper from {}: {}", sender,
                                    whisperContent);
                                appendToMessageArea(
                                    String.format("[From %s]: %s", sender, whisperContent));
                            } else {
                                logger.debug(
                                    "Ignoring whisper not intended for this client (target: {}, local: {})",
                                    targetUser, localNickname);
                            }
                            continue;
                        }
                    }

                    logger.info("Parsed {} message - Sender: '{}', Message: '{}'",
                        chatPrefix.substring(0, chatPrefix.length() - 1).toUpperCase(), sender,
                        chatMessage);
                    String displayPrefix = chatPrefix.equals("chat:") ? lang.get("lobby.global")
                        : lang.get("lobby.lobby");
                    appendToMessageArea(displayPrefix + sender + ": " + chatMessage);
                } else {
                    //no sender, probably server message
                    appendToMessageArea(lang.get("lobby.system") + chatData);
                }
                continue;
            }

            // Handle non-chat messages
            int colonIndex = message.indexOf(':');
            if (colonIndex == -1) {
                logger.error("Invalid message format (no colon and not a known chat prefix): {}",
                    message);
                appendToMessageArea(
                    lang.get("lobby.system") + message); // Display unknown format messages
                continue;
            }

            String type = message.substring(0, colonIndex).toLowerCase();
            String content = message.substring(colonIndex + 1);
            //logger.info("Parsed type: {}, content: {}", type, content);

            switch (type) {
                case "error":
                    appendToMessageArea("Error: " + content);
                    final String codeToSpectate;
                    if (currentLobbyCode != null) {
                        codeToSpectate = currentLobbyCode;
                    } else {
                        String selected = lobbyListView.getSelectionModel().getSelectedItem();
                        if (selected != null && !selected.isEmpty()) {
                            codeToSpectate = selected.split(" ")[0];
                            logger.info("Extracted code from selected lobby: {}", codeToSpectate);
                        } else {
                            codeToSpectate = null;
                        }
                    }

                    if (codeToSpectate != null &&
                        (content.contains("bereits") || content.contains("progress") ||
                            content.contains("corso") || content.contains("curso") ||
                            content.contains("ゲームはすでに進行中です") || content.contains(
                            "andamento") ||
                            content.contains("游戏已在进行中"))) {

                        String spectateMsg = "spectate:" + codeToSpectate;
                        logger.info("Sending spectate message: {}", spectateMsg);
                        serverHandler.sendMessage(spectateMsg);

                        Platform.runLater(() -> {
                            try {
                                GameSpectator spectator = new GameSpectator(this);
                                spectator.initialize(serverHandler, codeToSpectate);
                                Stage spectatorStage = new Stage();
                                spectator.start(spectatorStage);
                                stage.hide();
                                settings.close();
                                manual.close();
                                running = false;
                            } catch (Exception ex) {
                                logger.error("Error starting spectator mode", ex);
                                appendToMessageArea(lang.get("lobby.spectatorError"));
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
                        appendToMessageArea(lang.get("lobby.leftLobby"));
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
                                    lobbyListView.getItems()
                                        .add(code + " (" + playerCount + ") " + state);
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
                            String[] players =
                                content.isEmpty() ? new String[0] : content.split(",");
                            playerListView.getItems().clear();

                            Set<String> namesInUpdate = new HashSet<>();
                            for (String player : players) {
                                namesInUpdate.add(player.trim());
                                boolean isPlayerReady = playerReadyStatus.getOrDefault(
                                    player.trim(), false);
                                String playerDisplay = player.trim() + (isPlayerReady ? " ✓" : "");
                                playerListView.getItems().add(playerDisplay);
                            }

                            Set<String> playersToRemoveFromStatus = new HashSet<>(
                                playerReadyStatus.keySet());
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
                    appendToMessageArea(lang.get("lobby.gameStarted"));
                    Platform.runLater(() -> {
                        if (currentLobbyCode == null) {
                            appendToMessageArea(lang.get("lobby.startError"));
                            return;
                        }
                        readyButton.setDisable(true);

                        try {
                            SoundManager.getInstance().stopAll();
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
                            appendToMessageArea(lang.get("lobby.gameError"));
                            stage.show();
                        }
                    });
                    break;
                case "results":
                    processGameResults(content);
                    break;
                case "highscores":
                    Platform.runLater(() -> {
                        Stage highscoreStage = new Stage();
                        highscoreStage.setTitle(lang.get("hs.leaderboard"));
                        highscoreStage.initStyle(StageStyle.UNDECORATED);

                        String processedContent = content.replace("<br>", "\n");

                        StackPane root = new StackPane();
                        root.setStyle(
                            "-fx-background-color: linear-gradient(to bottom, #1a2a3a, #0d1520);");

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

                        Label title = new Label(lang.get("hs.halloffame"));
                        title.setStyle(
                            "-fx-font-size: 36px; -fx-font-weight: bold; -fx-text-fill: white; " +
                                "-fx-effect: dropshadow(gaussian, gold, 10, 0.6, 0, 0);");

                        SoundButton closeButton = new SoundButton("×");
                        closeButton.setStyle(
                            "-fx-font-size: 20px; -fx-background-color: transparent; -fx-text-fill: white; -fx-cursor: hand;");
                        closeButton.setOnAction(e -> highscoreStage.close());

                        Region spacer = new Region();
                        HBox.setHgrow(spacer, Priority.ALWAYS);

                        header.getChildren().addAll(trophyIcon, title, spacer, closeButton);

                        Label subtitle = new Label(lang.get("hs.fastest"));
                        subtitle.setStyle(
                            "-fx-font-size: 16px; -fx-font-style: italic; -fx-text-fill: #aaccff;");
                        subtitle.setOpacity(0.8);

                        FadeTransition pulse = new FadeTransition(Duration.seconds(2), subtitle);
                        pulse.setFromValue(0.7);
                        pulse.setToValue(1.0);
                        pulse.setCycleCount(Animation.INDEFINITE);
                        pulse.setAutoReverse(true);
                        pulse.play();

                        TabPane tabPane = new TabPane();
                        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
                        tabPane.setStyle(
                            "-fx-background-color: transparent; -fx-tab-min-width: 120;");

                        String[] sections = processedContent.split("\n\n");

                        Tab guardTab = createHighscoreTab(lang.get("hs.guard"), "#4a90e2",
                            sections[0], "👮");

                        Tab goatTab = createHighscoreTab(lang.get("hs.goat"), "#50c878",
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

                        scene.getStylesheets()
                            .add(getClass().getResource("/CSS/highscores.css").toExternalForm());

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
     * Updates the player list view to reflect the current ready status of all players. Each ready
     * player's name is suffixed with a checkmark (✓). This method is only applicable in lobby chat
     * mode.
     */
    private void updatePlayerListWithReadyStatus() {
        Platform.runLater(() -> {
            List<String> currentItems = new ArrayList<>(playerListView.getItems());
            playerListView.getItems().clear();

            for (String player : currentItems) {
                String playerName = player.replace(" ✓", "");
                String playerDisplay = playerName;
                if (playerReadyStatus.getOrDefault(playerName, false)) {
                    playerDisplay += " ✓";
                }
                playerListView.getItems().add(playerDisplay);
            }
        });
    }

    /**
     * Checks if all players in the lobby are ready to start the game. If all players are ready,
     * sends the 'startgame:' command to the server. The game will only start if: - There is at
     * least one player in the lobby - All players have marked themselves as ready - The server
     * connection is active
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
            appendToMessageArea(lang.get("lobby.allReady"));
        }
    }

    /**
     * Appends a message to the chat message area. This method is thread-safe as it uses
     * Platform.runLater().
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
            String modeName =
                isGlobalChat ? lang.get("lobby.globalChat") : lang.get("lobby.lobbyChat");
            String toggleButtonText =
                isGlobalChat ? lang.get("lobby.switchLobby") : lang.get("lobby.switchGlobal");
            String playerListText = isGlobalChat ? lang.get("lobby.playerLabelGlobal")
                : lang.get("lobby.playerLabelLobby");

            chatModeLabel.setText(modeName);
            toggleChatButton.setText(toggleButtonText);
            playerListLabel.setText(playerListText);
            boolean inLobby = currentLobbyCode != null;
            readyButton.setDisable(!inLobby);
        });
    }

    private void displayEnhancedHighscores(String content) {
        Platform.runLater(() -> {
            Stage highscoreStage = new Stage();
            highscoreStage.setTitle(lang.get("hs.leaderboard"));

            String processedContent = content.replace("<br>", "\n");

            StackPane root = new StackPane();
            root.setStyle(
                "-fx-background-color: #ffffff; -fx-border-color: #000000; -fx-border-width: 2px;");

            VBox mainContent = new VBox(15);
            mainContent.setPadding(new Insets(20, 30, 30, 30));
            mainContent.setMaxWidth(720);
            mainContent.setMaxHeight(540);

            HBox header = new HBox(10);
            header.setAlignment(Pos.CENTER);

            Label trophyIcon = new Label("#");
            trophyIcon.setStyle(
                "-fx-font-size: 24px; -fx-text-fill: #000000; -fx-font-family: 'Jersey 10', 'Courier New', monospace;");

            Label title = new Label("HIGHSCORES");
            title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #000000; " +
                "-fx-font-family: 'Jersey 10', 'Courier New', monospace;");

            SoundButton closeButton = new SoundButton("X");
            closeButton.getStyleClass().add("close-button");
            closeButton.setOnAction(e -> highscoreStage.close());

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            header.getChildren().addAll(trophyIcon, title, spacer, closeButton);

            Label subtitle = new Label(lang.get("hs.fastest"));
            subtitle.setStyle(
                "-fx-font-size: 16px; -fx-font-style: italic; -fx-text-fill: #000000; -fx-font-family: 'Jersey 10', 'Courier New', monospace;");

            TabPane tabPane = new TabPane();
            tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
            tabPane.getStyleClass().add("tab-pane");

            String[] sections = processedContent.split("\n\n");

            Tab guardTab = createHighscoreTab(lang.get("hs.guard"), "#b8b6b6", sections[0], "G");

            Tab goatTab = createHighscoreTab(lang.get("hs.goat"), "#b8b6b6",
                sections.length > 1 ? sections[1] : lang.get("hs.noHS"), "G");

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
            scene.getStylesheets()
                .add(getClass().getResource("/CSS/highscores.css").toExternalForm());
            ScreenUtil.moveStageToCursorScreen(highscoreStage, 700, 550);
            highscoreStage.show();
            logger.info("Displayed enhanced highscores window");
        });
    }

    /**
     * Creates a tab for displaying highscores.
     *
     * @param title   The title of the tab
     * @param content The highscore content to display
     * @param icon    The icon to display in the tab (emoji)
     * @return A Tab containing the highscore data
     */
    private Tab createHighscoreTab(String title, String color, String content, String icon) {
        Tab tab = new Tab(icon + " " + title);
        tab.setStyle("-fx-background-color: " + color + "; -fx-text-base-color: white;");

        if (content == null || content.trim().isEmpty() || content.contains(lang.get("hs.noHS"))) {
            Label noDataLabel = new Label(lang.get("hs.noHS"));
            noDataLabel.setStyle(
                "-fx-font-size: 16px; -fx-font-style: italic; -fx-text-fill: #aaccff; -fx-padding: 30px;");
            VBox container = new VBox(noDataLabel);
            container.setAlignment(Pos.CENTER);
            tab.setContent(container);
            return tab;
        }

        List<HighscoreEntry> entries = parseHighscoreContent(content);

        TableView<HighscoreEntry> table = new TableView<>();
        table.setItems(FXCollections.observableArrayList(entries));

        TableColumn<HighscoreEntry, String> rankCol = new TableColumn<>(lang.get("hs.rank"));
        rankCol.setCellValueFactory(
            cellData -> new SimpleStringProperty(cellData.getValue().getRank()));
        rankCol.setPrefWidth(60);

        TableColumn<HighscoreEntry, String> nameCol = new TableColumn<>(lang.get("hs.player"));
        nameCol.setCellValueFactory(
            cellData -> new SimpleStringProperty(cellData.getValue().getName()));
        nameCol.setPrefWidth(200);

        TableColumn<HighscoreEntry, String> timeCol = new TableColumn<>(lang.get("hs.time"));
        timeCol.setCellValueFactory(
            cellData -> new SimpleStringProperty(cellData.getValue().getTime()));
        timeCol.setPrefWidth(100);

        TableColumn<HighscoreEntry, String> dateCol = new TableColumn<>(lang.get("hs.date"));
        dateCol.setCellValueFactory(
            cellData -> new SimpleStringProperty(cellData.getValue().getDate()));
        dateCol.setPrefWidth(160);

        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);

        table.getColumns().addAll(List.of(rankCol, nameCol, timeCol, dateCol));

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

        public String getRank() {
            return rank;
        }

        public String getName() {
            return name;
        }

        public String getTime() {
            return time;
        }

        public String getDate() {
            return date;
        }
    }

    public static class PlayerRow {

        private final String name;
        private final String role;
        private final String outcome;

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

    private void processGameResults(String content) {
        String[] lines = content.split("\n");
        List<PlayerRow> playerRows = new ArrayList<>();

        for (String line : lines) {
            if (line.trim().isEmpty()) {
                continue;
            }
            String[] parts = line.split("\\|");
            if (parts.length >= 3) {
                playerRows.add(new PlayerRow(parts[0].trim(), parts[1].trim(), parts[2].trim()));
            }
        }

        Platform.runLater(() -> {
            TableView<PlayerRow> table = new TableView<>();

            TableColumn<PlayerRow, String> nameColumn = new TableColumn<>(lang.get("results.name"));
            nameColumn.setCellValueFactory(
                cellData -> new SimpleStringProperty(cellData.getValue().getName()));

            TableColumn<PlayerRow, String> roleColumn = new TableColumn<>(lang.get("results.role"));
            roleColumn.setCellValueFactory(
                cellData -> new SimpleStringProperty(cellData.getValue().getRole()));

            TableColumn<PlayerRow, String> outcomeColumn = new TableColumn<>(
                lang.get("results.outcome"));
            outcomeColumn.setCellValueFactory(
                cellData -> new SimpleStringProperty(cellData.getValue().getOutcome()));

            table.getColumns().addAll(List.of(nameColumn, roleColumn, outcomeColumn));
            table.setItems(FXCollections.observableArrayList(playerRows));

            // ... rest of UI setup
        });
    }
}
