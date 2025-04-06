package igoat.client;

import static java.lang.Math.pow;
import static java.lang.Math.sqrt;

import igoat.Role;
import igoat.client.GUI.LobbyGUI;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.UnaryOperator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import animatefx.animation.FadeInDown;
import animatefx.animation.FadeOutDown;
import animatefx.animation.FadeOutUp;
import animatefx.animation.Shake;
import animatefx.animation.Tada;
import javafx.animation.AnimationTimer;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextFormatter;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * Main game class that handles the core game logic, rendering, and networking.
 * This class manages:
 * - Player movement and collision detection
 * - Camera and viewport management
 * - Network communication (TCP for game state, UDP for position updates)
 * - Remote player synchronization
 * - Game state and visual updates
 *
 * The game uses a client-server architecture where each client sends position updates
 * via UDP and receives game state updates via TCP.
 */
public class Game extends Application {
    private static final Logger logger = LoggerFactory.getLogger(Game.class);

    private static final double PLAYER_WIDTH = 32;
    private static final double PLAYER_HEIGHT = 32;
    private static final double MOVEMENT_SPEED = 300;
    private static final double CAMERA_ZOOM = 3; // Default is 3
    
    private Pane gamePane;
    private Pane uiOverlay;
    private Stage stage;
    private LobbyGUI lobby;
    private double windowWidth;
    private double windowHeight;
    private Set<KeyCode> activeKeys;
    private long lastUpdate;
    private long lastPositionUpdate = 0;
    private static final long POSITION_UPDATE_INTERVAL = 16;
    
    private Player player;
    private igoat.client.Map gameMap;
    private DoorHandler doorHandler;
    private Camera activeCamera;
    private ServerHandler serverHandler;
    private String playerName;
    private String lobbyCode;
    private String username;
    
    private boolean gameStarted = false;
    
    private Map<String, Player> otherPlayers = new HashMap<>();
    private boolean pressedE = false;


    /**
     * Enum for defining the chat modes available in the game.
     * 
     * This enum provides a way to manage different chat modes within the game. It includes three modes:
     * GLOBAL, LOBBY, and TEAM. Each mode has a display name associated with it, which is used to identify
     * the mode in the game's user interface.
     */
    private enum ChatMode {
        GLOBAL("Global"),
        LOBBY("Lobby"),
        TEAM("Team");

        private final String displayName;

        /**
         * Constructor for ChatMode.
         * 
         * @param displayName The display name of the chat mode.
         */
        ChatMode(String displayName) {
            this.displayName = displayName;
        }

        /**
         * Returns the display name of the chat mode.
         * 
         * @return The display name of the chat mode.
         */
        public String getDisplayName() {
            return displayName;
        }
    }


    private VBox chatBox;
    private TextArea chatInput;
    private TextFlow chatFlow;
    private Text chatModeIndicator;
    private ScrollPane chatScrollPane;
    private Timeline chatHideTimer;
    private FadeTransition chatFadeOutTransition;
    private ChatMode currentChatMode = ChatMode.LOBBY;

    private Label terminalActivationBanner;
    private Label allTerminalsBanner;
    private Label alreadyActiveBanner;

    /**
     * Constructor for Game
     * @param lobby The Lobby GUI. This is used to call the lobby exit method after the game window closes.
     */
    public Game(LobbyGUI lobby) {
        this.lobby = lobby;
    }

    /**
     * Initializes the game with necessary data from the lobby.
     *
     * @param handler The active ServerHandler.
     * @param name The player's confirmed name.
     * @param code The lobby code.
     * @param initialPlayerNames List of players already in the lobby.
     */
    public void initialize(ServerHandler handler, String name, String code, List<String> initialPlayerNames) {
        this.serverHandler = handler;
        this.playerName = name;
        this.lobbyCode = code;
        this.gameStarted = true;
        this.username = name;

        startMessageProcessor();
        startUdpUpdateProcessor();

        // Request role information for all players
        if (this.serverHandler != null && this.serverHandler.isConnected()) {
            this.serverHandler.sendMessage("getroles:");
        }

        Platform.runLater(() -> {
            String confirmedNickname = serverHandler.getConfirmedNickname();
            if (confirmedNickname == null) {
                logger.error("Cannot initialize - confirmed nickname is null");
                return;
            }
            
            for (String pName : initialPlayerNames) {
                if (!pName.equals(confirmedNickname)) {
                    createVisualForRemotePlayer(pName, 100, 100);
                }
            }
        });
    }

    /**
     * Initializes and starts the game VIEW.
     * Assumes initialize() has been called previously.
     * Sets up the game window, player visuals, map, and input handling.
     * Also initializes the game loop using AnimationTimer.
     *
     * @param primaryStage The primary stage of the JavaFX application
     */
    @Override
    public void start(Stage primaryStage) {
        stage = primaryStage;

        stage.setOnCloseRequest(event -> exit());

        if (serverHandler == null || playerName == null || lobbyCode == null) {
            showError("Initialization Error", "Game cannot start without server connection details.");
            exit();
            return;
        }

        String confirmedNickname = serverHandler.getConfirmedNickname();
        if (confirmedNickname == null) {
            showError("Initialization Error", "Cannot start game without confirmed nickname from server.");
            exit();
            return;
        }

        // Request role assignment from server
        serverHandler.sendMessage("ready:");
        
        gameMap = new igoat.client.Map();
        doorHandler = new DoorHandler(gameMap);
        
        gamePane = new Pane();
        gamePane.setMinSize(gameMap.getWidth(), gameMap.getHeight());
        gamePane.setMaxSize(gameMap.getWidth(), gameMap.getHeight());
        gamePane.setPrefSize(gameMap.getWidth(), gameMap.getHeight());
        gamePane.setStyle("-fx-background-color: white;");
        gamePane.setClip(new Rectangle(0, 0, gameMap.getWidth(), gameMap.getHeight()));

        if (serverHandler != null && serverHandler.isConnected()) {
            int terminalCount = gameMap.getTerminalList().size();
            String mapInfoMessage = "mapinfo:" + terminalCount;
            serverHandler.sendMessage(mapInfoMessage);
            logger.info("Sent map info to server: {}", mapInfoMessage);
        }

        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        double screenWidth = screenBounds.getWidth() * 0.8;
        double screenHeight = screenBounds.getHeight() * 0.8;
        
        double mapAspectRatio = (double)gameMap.getWidth() / gameMap.getHeight();
        
        if (screenWidth / screenHeight > mapAspectRatio) {
            windowHeight = screenHeight;
            windowWidth = screenHeight * mapAspectRatio;
        } else {
            windowWidth = screenWidth;
            windowHeight = screenWidth / mapAspectRatio;
        }
        
        primaryStage.setWidth(windowWidth);
        primaryStage.setHeight(windowHeight);
        primaryStage.setFullScreenExitHint("");



    Pane container = new Pane();
    container.getChildren().add(gamePane);
    
    uiOverlay = new Pane();
    uiOverlay.setMouseTransparent(true); 
    uiOverlay.prefWidthProperty().bind(stage.widthProperty());
    uiOverlay.prefHeightProperty().bind(stage.heightProperty());
    container.getChildren().add(uiOverlay);
    
    Scene scene = new Scene(container);
        scene.setFill(Color.WHITE);
        
        String windowTitle = "iGoat Game - Lobby " + lobbyCode + " - Player: " + confirmedNickname;
        primaryStage.setTitle(windowTitle);
        
        primaryStage.setScene(scene);

        primaryStage.show();

        terminalActivationBanner = new Label("Terminal Activated!");
        terminalActivationBanner.setStyle("-fx-background-color: rgba(0, 200, 0, 0.7); -fx-text-fill: white; -fx-font-size: 24px; -fx-padding: 10px; -fx-background-radius: 5px;");
        terminalActivationBanner.setVisible(false);
        terminalActivationBanner.layoutXProperty().bind(uiOverlay.widthProperty().subtract(terminalActivationBanner.widthProperty()).divide(2));
        terminalActivationBanner.setLayoutY(20);
        uiOverlay.getChildren().add(terminalActivationBanner);
    
        allTerminalsBanner = new Label("All Terminals Activated! Exits Open!");
        allTerminalsBanner.setStyle("-fx-background-color: rgba(0, 100, 255, 0.8); -fx-text-fill: white; -fx-font-size: 28px; -fx-padding: 15px; -fx-background-radius: 8px; -fx-border-color: white; -fx-border-width: 2; -fx-border-radius: 8;");
        allTerminalsBanner.setVisible(false);
        allTerminalsBanner.layoutXProperty().bind(uiOverlay.widthProperty().subtract(allTerminalsBanner.widthProperty()).divide(2));
        allTerminalsBanner.setLayoutY(60); 
        uiOverlay.getChildren().add(allTerminalsBanner);

        alreadyActiveBanner = new Label("Terminal X is already active!");
        alreadyActiveBanner.setStyle("-fx-background-color: rgba(255, 100, 0, 0.8); -fx-text-fill: white; -fx-font-size: 18px; -fx-padding: 8px; -fx-background-radius: 5px;");
        alreadyActiveBanner.setVisible(false);
        alreadyActiveBanner.layoutXProperty().bind(uiOverlay.widthProperty().subtract(alreadyActiveBanner.widthProperty()).divide(2));
        alreadyActiveBanner.setLayoutY(100);
        uiOverlay.getChildren().add(alreadyActiveBanner);

        for (Node wall : gameMap.getVisualWalls()) {
            gamePane.getChildren().add(wall);
        }
        
        player = new Player(gamePane, primaryStage.getWidth(), primaryStage.getHeight(), CAMERA_ZOOM,
                100, 100, (int)PLAYER_WIDTH, (int)PLAYER_HEIGHT, Color.GRAY, confirmedNickname, true);
        
        player.setSpectated(false);
        activeCamera = player.getCamera();
        
        for (Player other : otherPlayers.values()) {
            if (!gamePane.getChildren().contains(other.getVisualRepresentation())) {
                Rectangle otherVisual = other.getVisualRepresentation();
                // add clipping for fog effect

                gamePane.getChildren().add(otherVisual);
            }
        }
        
        scene.widthProperty().addListener((obs, oldVal, newVal) -> {
            activeCamera.updateViewport(newVal.doubleValue(), scene.getHeight());
            windowWidth = newVal.doubleValue();
            updateVisuals();
        });
        
        scene.heightProperty().addListener((obs, oldVal, newVal) -> {
            activeCamera.updateViewport(scene.getWidth(), newVal.doubleValue());
            windowHeight = newVal.doubleValue();
            updateVisuals();
        });
        
        primaryStage.fullScreenProperty().addListener((obs, oldVal, newVal) -> {
            Platform.runLater(() -> {
                activeCamera.updateViewport(scene.getWidth(), scene.getHeight());
            });
        });
        
        activeKeys = new HashSet<>();
        scene.setOnKeyPressed(event -> {
            activeKeys.add(event.getCode());
            if (event.getCode() == KeyCode.ESCAPE) {
                primaryStage.setFullScreen(false);
            }
        });
        
        scene.setOnKeyReleased(event -> {
            activeKeys.remove(event.getCode());
        });
        
        gamePane.setFocusTraversable(true);
        gamePane.requestFocus();
        
        lastUpdate = System.nanoTime();
        new AnimationTimer() {
            @Override
            public void handle(long now) {
                double deltaTime = (now - lastUpdate) / 1_000_000_000.0;
                lastUpdate = now;
                update(deltaTime);
            }
        }.start();
        
        initializeChatUI();
    }

    /**
     * Exit routine for the Game instance. This will close the game window and return to the lobby screen.
     */
    private void exit() {
        stage.close();
        lobby.exit();
    }

    /**
     * Shows an error dialog to the user.
     * This method is thread-safe and will run on the JavaFX Application Thread.
     *
     * @param title the title of the error dialog
     * @param content the error message to display
     */
    private void showError(String title, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);
            alert.showAndWait();
        });
    }
    
    /**
     * Starts a background thread that processes TCP messages from the server.
     * Handles game state updates, chat messages, and player events.
     * The thread runs continuously until the game ends or the connection is lost.
     */
    private void startMessageProcessor() {
        if (serverHandler == null) return;

        Thread messageProcessor = new Thread(() -> {
            while (gameStarted && serverHandler.isConnected()) {
                try {
                    String message = serverHandler.getMessage();
                    if (message != null) {
                        processServerMessage(message);
                    }
                    Thread.sleep(16);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.error("Message processor interrupted.");
                    break;
                } catch (Exception e) {
                     logger.error("Error in message processor", e);
                     break;
                 }
            }
             logger.info("Message processor stopped.");
        });
        messageProcessor.setDaemon(true);
        messageProcessor.start();
    }
    
    /**
     * Processes TCP messages received from the server.
     * Handles various message types including:
     * - Lobby player updates
     * - Chat messages
     * - Game state changes
     * - Player events (catch, revive, leave)
     *
     * @param message the message received from the server
     */
    private void processServerMessage(String message) {
        BiFunction<String, String, String[]> parseSenderAndContent = (prefix, msg) -> {
            String data = msg.substring(prefix.length()); 

            if (data.startsWith("[") && data.contains("] ")) {
                int closingBracketIndex = data.indexOf("] ");
                if (closingBracketIndex > 1) { 
                    String sender = data.substring(1, closingBracketIndex);
                    String content = data.substring(closingBracketIndex + 2);
                    return new String[]{sender, content};
                }
            }
            
            String[] parts = data.split(":", 2);
            if (parts.length == 2) return parts;
            
            int firstSpaceIndex = data.indexOf(' ');
            if (firstSpaceIndex != -1) {
                 String sender = data.substring(0, firstSpaceIndex);
                 String content = data.substring(firstSpaceIndex + 1);
                 return new String[]{sender, content};
            }
            
            logger.warn("Could not parse sender from message using known patterns: {}", msg);
            return new String[]{ "System", data }; 
        };

        ChatMode mode = null;
        String prefixString = null;
        if (message.startsWith("lobbychat:")) {
            mode = ChatMode.LOBBY;
            prefixString = "lobbychat:";
        } else if (message.startsWith("teamchat:")) {
            mode = ChatMode.TEAM;
            prefixString = "teamchat:";
        } else if (message.startsWith("chat:")) {
            mode = ChatMode.GLOBAL;
            prefixString = "chat:";
        } else {
            if (message.startsWith("getlobbyplayers:")) {
                String playersData = message.substring("getlobbyplayers:".length());
                if (!playersData.isEmpty()) {
                    String[] playerNames = playersData.split(",");
                    Set<String> currentPlayers = new HashSet<>(List.of(playerNames));
                    currentPlayers.add(this.playerName);

                    Set<String> playersToRemove = new HashSet<>(otherPlayers.keySet());
                    playersToRemove.removeAll(currentPlayers);
                    for (String nameToRemove : playersToRemove) {
                        removeRemotePlayer(nameToRemove);
                    }

                    for (String name : playerNames) {
                        if (!name.equals(this.playerName) && !otherPlayers.containsKey(name)) {
                            createVisualForRemotePlayer(name, 100, 100);
                        }
                    }
                }
            } else if (message.equals("game_started")) {
                handleGameStarted();
            } else if (message.startsWith("catch:")) {
                String caughtPlayer = message.substring("catch:".length());
                logger.info("{} was caught!", caughtPlayer);
            } else if (message.startsWith("revive:")) {
                String revivedPlayer = message.substring("revive:".length());
                logger.info("{} was revived!", revivedPlayer);
            } else if (message.startsWith("player_left:")) {
                String leftPlayer = message.substring("player_left:".length());
                if (!leftPlayer.equals(this.playerName)) {
                    removeRemotePlayer(leftPlayer);
                }
            } else if (message.startsWith("terminal:")) {
                String[] parts = message.split(":");
                if (parts.length == 2) {
                    activateTerminal(Integer.parseInt(parts[1]));
                }
            } else if (message.startsWith("role:")) {
                String[] parts = message.split(":");
                if (parts.length == 3) {
                    String playerName = parts[1];
                    Role role = Role.valueOf(parts[2]);

                    logger.info("Received role {} for player {}", role, playerName);

                    if (player != null && playerName.equals(player.getUsername())) {
                        player.setRole(role);
                    } else if (otherPlayers.containsKey(playerName)) {
                        otherPlayers.get(playerName).setRole(role);
                    }
                    else {
                        logger.warn("Received role for unknown player: {}", playerName);
                    }

                } else {
                    logger.error("Invalid role message format: {}", message);
                }
            } else if (message.startsWith("roles:")) {
                String rolesData = message.substring("roles:".length());
                if (!rolesData.isEmpty()) {
                    String[] roleEntries = rolesData.split(",");
                    for (String entry : roleEntries) {
                        String[] parts = entry.split("=");
                        if (parts.length == 2) {
                            String playerName = parts[0];
                            Role role = Role.valueOf(parts[1]);
                            logger.info("Received role {} for player {}", role, playerName);
                            logger.info("Original message: {}", message);

                            if (player != null && playerName.equals(player.getUsername())) {
                                player.setRole(role);
                            } else if (otherPlayers.containsKey(playerName)) {
                                otherPlayers.get(playerName).setRole(role);
                            }
                            else {
                                logger.warn("Received role for unknown player: {}", playerName);
                            }
                        }
                    }
                }
            } else if (message.equals("doors_open:")) {
                Platform.runLater(this::handleDoorsOpen);
                return;
            } else {
                 logger.warn("Received message with unknown prefix: {}", message);
            }
            return;
        }

        String[] parsed = parseSenderAndContent.apply(prefixString, message);
        String sender = parsed[0];
        String content = parsed[1];

        if ("System".equals(sender) && "All terminals have been activated!".equals(content)) {
            logger.info("Received notification: All terminals activated.");
            Platform.runLater(() -> {
                allTerminalsBanner.setVisible(true);
                allTerminalsBanner.setOpacity(1.0);
                new Tada(allTerminalsBanner).play();

                PauseTransition delay = new PauseTransition(Duration.seconds(4));
                delay.setOnFinished(event -> {
                    new FadeOutDown(allTerminalsBanner).play();
                });
                delay.play();
            });
            return;
        }

        final String alreadyActiveSuffix = " was already activated.";
        if ("System".equals(sender) && content.endsWith(alreadyActiveSuffix) && content.startsWith("Terminal ")) {
            try {
                String terminalIdStr = content.substring("Terminal ".length(), content.length() - alreadyActiveSuffix.length());
                logger.info("Received notification: Terminal {} already activated.", terminalIdStr);

                Platform.runLater(() -> {
                    alreadyActiveBanner.setText("Terminal " + terminalIdStr + " is already active!");
                    alreadyActiveBanner.setVisible(true);
                    alreadyActiveBanner.setOpacity(1.0);
                    new Shake(alreadyActiveBanner).play();

                    PauseTransition delay = new PauseTransition(Duration.seconds(1.5));
                    delay.setOnFinished(event -> {
                        alreadyActiveBanner.setVisible(false); 
                    });
                    delay.play();
                });
            } catch (Exception e) {
                logger.error("Failed to parse already activated message: {}", message, e);
            }
            return;
        }

        String localNickname = serverHandler.getConfirmedNickname();

        if (localNickname != null && localNickname.equals(sender)) {
             logger.debug("Ignoring echo of own message from sender: {}", sender);
            return; 
        }

        final String whisperMarkerStart = "[WHISPER->";
        if (content.startsWith(whisperMarkerStart)) {
            int markerEnd = content.indexOf("]");
            if (markerEnd > whisperMarkerStart.length()) {
                String targetUser = content.substring(whisperMarkerStart.length(), markerEnd);
                String whisperContent = content.substring(markerEnd + 2); // Skip "] "

                if (localNickname != null && localNickname.equalsIgnoreCase(targetUser)) {
                    logger.info("Received whisper from {}: {}", sender, whisperContent);
                    addChatMessage(sender, null, whisperContent, null);
                } else {
                    logger.debug("Ignoring whisper not intended for this client (target: {}, local: {})", targetUser, localNickname);
                }
                return;
            }
        }

        logger.debug("Processing incoming {} chat message. Sender: '{}', Message: '{}'", 
                     mode, sender, content);
        addChatMessage(sender, null, content, mode);
    }
    
    /**
     * Handles the opening of doors when all terminals are activated.
     */
    private void handleDoorsOpen() {
        logger.info("Handling doors open command.");
        if (gameMap != null) {
            gameMap.openDoors();
             logger.info("Doors have been opened.");
        }
    }

    /**
     * Starts a background thread that processes UDP position updates from the server.
     * This thread runs at a higher frequency than the TCP message processor to ensure
     * smooth player movement updates. Also initiates the first position update for
     * the local player.
     */
    private void startUdpUpdateProcessor() {
        if (serverHandler == null) return;

        sendPlayerPositionUpdate();
        
        Thread updateProcessor = new Thread(() -> {
            while (gameStarted && serverHandler.isConnected()) {
                try {
                    String update = serverHandler.getLastUpdate();
                    if (update != null && !update.isEmpty()) {
                        processUdpUpdate(update);
                    }
                    Thread.sleep(2);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    logger.error("Processor error", e);
                    break;
                }
            }
        });
        updateProcessor.setDaemon(true);
        updateProcessor.start();
    }
    
    /**
     * Processes UDP updates received from the server.
     *
     * @param update The UDP update message
     */
    private void processUdpUpdate(String update) {
        if (update.startsWith("player_position:")) {
            String[] parts = update.split(":");
            if (parts.length != 4) {
                logger.error("Invalid position update: {}", update);
                return;
            }

            String remotePlayerName = parts[1];
            try {
                int x = Integer.parseInt(parts[2]);
                int y = Integer.parseInt(parts[3]);

                String confirmedNickname = serverHandler.getConfirmedNickname();
                if (confirmedNickname == null) {
                    logger.error("Cannot process update - confirmed nickname is null");
                    return;
                }
                if (remotePlayerName.equals(confirmedNickname)) {
                    return;
                }
                if (otherPlayers.containsKey(remotePlayerName)) {
                    updateRemotePlayerPosition(remotePlayerName, x, y);
                } else {
                    createVisualForRemotePlayer(remotePlayerName, x, y);
                }
            } catch (NumberFormatException e) {
                logger.error("Invalid coordinates in update: {}", update, e);
            }
        } else if (update.startsWith("udp_ack:")) {
            logger.info("Received UDP acknowledgment from server");
        } else if (update.equals("doors_open:")) {
            Platform.runLater(this::handleDoorsOpen);
        } else {
            logger.info("Unrecognized UDP message format: {}", update);
        }
    }

    /**
     * Updates the position of a remote player.
     *
     * @param playerName The name of the remote player
     * @param x The new x-coordinate
     * @param y The new y-coordinate
     */
    private void updateRemotePlayerPosition(String playerName, int x, int y) {
        //logger.info("Attempting to update player {} to position ({}, {})", playerName, x, y);
        
        if (otherPlayers.containsKey(playerName)) {
            Player remotePlayer = otherPlayers.get(playerName);
            Platform.runLater(() -> {
                remotePlayer.updatePosition(x, y);
                //logger.info("Updated existing player {} to position ({}, {})", playerName, x, y);
            });
        } else {
            logger.info("Player {} not found visually, creating at ({}, {})", playerName, x, y);
            createVisualForRemotePlayer(playerName, x, y);
        }
    }
    
    /**
     * Creates a visual representation for a remote player.
     *
     * @param playerName The name of the remote player
     * @param x The x-coordinate
     * @param y The y-coordinate
     */
    private void createVisualForRemotePlayer(String playerName, int x, int y) {
        logger.info("Creating visual for player: {}", playerName);
        
        if (otherPlayers.containsKey(playerName)) {
            logger.info("Player {} already exists, updating position instead", playerName);
            updateRemotePlayerPosition(playerName, x, y);
            return;
        }

        // Request role information for this player
        if (serverHandler != null && serverHandler.isConnected()) {
            serverHandler.sendMessage("getroles:");
        }
        
        // Start with default color
        Color playerColor = Color.ORANGE;
        
        try {
            final Player[] remotePlayerRef = new Player[1];
            Platform.runLater(() -> {
                if (!otherPlayers.containsKey(playerName)) { 
                    Player remotePlayer = new Player(gamePane, gamePane.getWidth(), gamePane.getHeight(), CAMERA_ZOOM,
                x, y, (int)PLAYER_WIDTH, (int)PLAYER_HEIGHT, playerColor, playerName, false);
                    
                    otherPlayers.put(playerName, remotePlayer);
                    
                    logger.info("Added visual for player {}", playerName);
                }
            });

            Thread.sleep(50);
            
            if (remotePlayerRef[0] != null) {
                Platform.runLater(() -> {
                    if (!gamePane.getChildren().contains(remotePlayerRef[0].getVisualRepresentation())) {
                        gamePane.getChildren().add(remotePlayerRef[0].getVisualRepresentation());
                    }
                    if (!gamePane.getChildren().contains(remotePlayerRef[0].getUsernameLabel())) {
                        gamePane.getChildren().add(remotePlayerRef[0].getUsernameLabel());
                    }
                });
            }
        } catch (Exception e) {
            logger.error("Error creating visual for player", e);
        }
    }

    /**
     * Updates the visual elements of the game based on the local player's position.
     * This includes:
     * - Updating the fog of war effect around other players
     * - Clipping special game elements based on visibility
     * - Updating visual elements for dynamic game objects
     */
    private void updateVisuals() {
        double centerX = player.getX() + (PLAYER_WIDTH/2.0);
        double centerY = player.getY() + (PLAYER_HEIGHT/2.0);

        for (Player otherPlayer : otherPlayers.values()) {
            Circle visualClip = new Circle(centerX, centerY, 100);
            Circle labelClip = new Circle(centerX, centerY, 100);
            
            otherPlayer.getVisualRepresentation().setClip(visualClip);
            otherPlayer.getUsernameLabel().setClip(labelClip);
        }

        for (Node node : gameMap.getVisualWalls()) {
            if (node instanceof Rectangle rectangle && rectangle.getFill() == Color.RED) { // Special elemente die rot sind
                Circle elementClip = new Circle(centerX, centerY, 100);
                node.setClip(elementClip);
            }
        }
    }

    /**
     * Removes the visual representation of a remote player.
     *
     * @param remotePlayerName The name of the player to remove.
     */
    private void removeRemotePlayer(String remotePlayerName) {
        String confirmedNickname = serverHandler.getConfirmedNickname();
        if (confirmedNickname == null) {
            logger.error("Cannot remove player - confirmed nickname is null");
            return;
        }
        
        if (remotePlayerName.equals(confirmedNickname)) {
            logger.info("Ignoring removal of ourselves");
            return;
        }
        
        Player removedPlayer = otherPlayers.remove(remotePlayerName);
        if (removedPlayer != null) {
            Platform.runLater(() -> {
                gamePane.getChildren().remove(removedPlayer.getVisualRepresentation());
                gamePane.getChildren().remove(removedPlayer.getUsernameLabel());
            });
            logger.info("Removed visual for player {}", remotePlayerName);
        }
    }

    /**
     * Updates the game state based on the elapsed time.
     * This is the main game loop that handles:
     * - Player movement based on keyboard input
     * - Collision detection with walls
     * - Camera updates
     * - Position updates to server
     * - Visual updates
     *
     * @param deltaTime time elapsed since last update in seconds
     */
    private void update(double deltaTime) {
        if (player == null || !gameStarted) return;

        double currentX = player.getX();
        double currentY = player.getY();
        double newX = currentX;
        double newY = currentY;

        Point2D direction = new Point2D(0, 0);
        
        if (activeKeys.contains(KeyCode.W) || activeKeys.contains(KeyCode.UP)) {
            direction = direction.add(0, -1);
        }
        if (activeKeys.contains(KeyCode.S) || activeKeys.contains(KeyCode.DOWN)) {
            direction = direction.add(0, 1);
        }
        if (activeKeys.contains(KeyCode.A) || activeKeys.contains(KeyCode.LEFT)) {
            direction = direction.add(-1, 0);
        }
        if (activeKeys.contains(KeyCode.D) || activeKeys.contains(KeyCode.RIGHT)) {
            direction = direction.add(1, 0);
        }
        if (activeKeys.contains(KeyCode.E)) {
            if (!pressedE) {
                pressedE = true;
                switch (player.getRole()){
                    case Role.GUARD:
                        pressCatch();
                        break;
                    case Role.IGOAT:
                        pressTerminal();
                        break;
                    case Role.GOAT:
                        pressRevive();
                }

            }
        }
        else {
            pressedE = false;
        }
        
        if (!direction.equals(Point2D.ZERO)) {
            direction = direction.normalize();
            direction = direction.multiply(MOVEMENT_SPEED * deltaTime);
        }
        
        double dx = direction.getX();
        double dy = direction.getY();

        double potentialX = currentX + dx;
        double potentialY = currentY + dy;

        boolean canMoveX = true;
        if (dx != 0) {
            for (Wall wall : gameMap.getCollisionWalls()) {
                if (player.collidesWithWall((int)potentialX, (int)currentY, wall)) {
                    canMoveX = false;
                    break;
                }
            }
            if (canMoveX) {
                newX = potentialX;
            }
        }

        boolean canMoveY = true;
        if (dy != 0) {
            for (Wall wall : gameMap.getCollisionWalls()) {
                if (player.collidesWithWall((int)newX, (int)potentialY, wall)) {
                    canMoveY = false;
                    break;
                }
            }
            if (canMoveY) {
                newY = potentialY;
            }
        }

        boolean positionChanged = Math.abs(newX - currentX) > 0.01 || Math.abs(newY - currentY) > 0.01;
        if (positionChanged) {
            player.updatePosition(newX, newY);
        }

        long currentTime = System.currentTimeMillis();
        if (positionChanged || (currentTime - lastPositionUpdate > POSITION_UPDATE_INTERVAL)) {
            sendPlayerPositionUpdate();
            lastPositionUpdate = currentTime;
        }

        if (activeCamera != null) {
            activeCamera.centerOn(player.getX() + (player.getWidth() / 2.0), player.getY() + (player.getHeight() / 2.0));
        }

        updateVisuals();
    }

    private void pressCatch() {
        double x = player.getX() + (player.getWidth() / 2.0);
        double y = player.getY() + (player.getHeight() / 2.0);

        for (Player target : otherPlayers.values()) {
            double tx = target.getX() + (target.getWidth() / 2.0);
            double ty = target.getY() + (target.getHeight() / 2.0);
            if (sqrt(pow(tx - x, 2) + pow(ty - y, 2)) < 35.0) {
                serverHandler.sendMessage("catch:" + target.getUsername());
                return;
            }
        }
    }

    private void pressRevive() {
        double x = player.getX() + (player.getWidth() / 2.0);
        double y = player.getY() + (player.getHeight() / 2.0);

        for (Player target : otherPlayers.values()) {
            double tx = target.getX() + (target.getWidth() / 2.0);
            double ty = target.getY() + (target.getHeight() / 2.0);
            if (target.getRole() == Role.IGOAT && sqrt(pow(tx - x, 2) + pow(ty - y, 2)) < 35.0) {
                serverHandler.sendMessage("revive:" + target.getUsername());
                return;
            }
        }
    }

    /**
     * Sends checks if the player is in range of a terminal to activate. If so, then a message is sent to the server
     */
    private void pressTerminal() {
        double x = player.getX() + (player.getWidth() / 2.0);
        double y = player.getY() + (player.getHeight() / 2.0);

        for (Terminal terminal : gameMap.getTerminalList()) {
            double tx = terminal.getX() + (terminal.getWidth() / 2.0);
            double ty = terminal.getY() + (terminal.getHeight() / 2.0);
            if (sqrt(pow(tx - x, 2) + pow(ty - y, 2)) < 35.0) {
                logger.info("Activating terminal");
                serverHandler.sendMessage("terminal:" + terminal.getTerminalID());
                return;
            }
        }
    }

    /**
     * Activates a terminal and displays a banner
     * @param id The ID of the terminal
     */
    private void activateTerminal(int id) {
        // display terminal activation banner
        Platform.runLater(() -> {
            terminalActivationBanner.setText("Terminal " + id + " Activated!");

            terminalActivationBanner.setVisible(true);
            terminalActivationBanner.setOpacity(1.0);
            new FadeInDown(terminalActivationBanner).play();

            PauseTransition delay = new PauseTransition(Duration.seconds(2.5));
            delay.setOnFinished(event -> {
                new FadeOutUp(terminalActivationBanner).play();
                // terminalActivationBanner.setVisible(false);
            });
            delay.play();
        });
        for (Terminal terminal : gameMap.getTerminalList()) {
            if (terminal.getTerminalID() == id) {
                terminal.activate();
                return;
            }
        }
    }

    /**
     * Sends the local player's current position to the server via UDP.
     * The update includes:
     * - Player's nickname
     * - Current lobby code
     * - X and Y coordinates
     * 
     * This is called periodically during the game loop when the player moves
     * or when the position update interval has elapsed.
     */
    private void sendPlayerPositionUpdate() {
        if (serverHandler != null && serverHandler.isConnected() && player != null) {
            int x = (int)player.getX();
            int y = (int)player.getY();
            
            String confirmedNickname = serverHandler.getConfirmedNickname();
            if (confirmedNickname == null) {
                logger.error("Cannot send position update - confirmed nickname is null");
                return;
            }
            
            String updateMessage = String.format("position:%s:%s:%d:%d",
                                               confirmedNickname, lobbyCode, x, y);
            //logger.info("Sending position update: {}", updateMessage);
            serverHandler.sendUpdate(updateMessage);
        }
    }
    
    /**
     * Handles the game started event (defensive).
     * Mainly ensures player is controllable.
     */
    private void handleGameStarted() {
        gameStarted = true;
        Platform.runLater(() -> {
            if(player != null) {
                 player.setSpectated(false);
            }
        });
    }

    /**
     * Initializes the chat UI components with proper styling and positioning
     */
    private void initializeChatUI() {
        if (stage == null) return;
        Scene gameScene = stage.getScene();
        if (gameScene == null) {
            logger.error("Cannot initialize chat UI: Scene is null.");
            return;
        }
        
        chatBox = new VBox(5);
        chatBox.setPadding(new Insets(5));
        chatBox.setStyle("-fx-background-color: rgba(0, 0, 0, 0); -fx-text-fill: white;");
        chatBox.setPrefWidth(400);
        chatBox.setPrefHeight(300);
        chatBox.setMaxWidth(400);
        chatBox.setMaxHeight(300);
        chatBox.setMouseTransparent(false);
        chatBox.setVisible(false);
        chatBox.setOpacity(0.0);

        chatBox.translateXProperty().bind(
            stage.widthProperty().subtract(chatBox.widthProperty()).subtract(10)
        );
        chatBox.setTranslateY(10);

        chatFadeOutTransition = new FadeTransition(Duration.millis(500), chatBox);
        chatFadeOutTransition.setFromValue(1.0);
        chatFadeOutTransition.setToValue(0.0);
        chatFadeOutTransition.setCycleCount(1);
        chatFadeOutTransition.setAutoReverse(false);
        chatFadeOutTransition.setOnFinished(e -> chatBox.setVisible(false)); 

        chatHideTimer = new Timeline(new KeyFrame(
            Duration.seconds(4),
            event -> {
                if (chatBox.isVisible() && chatBox.getOpacity() == 1.0) { 
                    chatFadeOutTransition.playFromStart();
                }
            }
        ));
        chatHideTimer.setCycleCount(1);
        
        chatFlow = new TextFlow();
        chatFlow.setStyle("-fx-background-color: rgba(0, 0, 0, 0);");
        
        chatScrollPane = new ScrollPane(chatFlow);
        chatScrollPane.setStyle("-fx-background: rgba(0, 0, 0, 0); -fx-background-color: rgba(0, 0, 0, 0.3); -fx-padding: 0;");
        chatScrollPane.setFitToWidth(true);
        chatScrollPane.setPrefViewportHeight(250);
        chatScrollPane.setMaxHeight(250);
        chatScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        chatScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        chatScrollPane.setMouseTransparent(false);
        chatScrollPane.setPannable(false);
        
        chatInput = new TextArea();
        chatInput.setPromptText("Press Enter to chat...");
        chatInput.setPrefRowCount(1);
        chatInput.setWrapText(true);
        chatInput.setStyle("-fx-control-inner-background: rgba(0, 0, 0, 0.3); -fx-text-fill: white;");
        chatInput.setMaxHeight(30);
        chatInput.setVisible(false);

        UnaryOperator<TextFormatter.Change> filter = change -> {
            String newText = change.getText();
            if (newText.contains("\t")) {
                String correctedText = newText.replace("\t", ""); 
                change.setText(correctedText);
            }
            return change;
        };
        TextFormatter<String> textFormatter = new TextFormatter<>(filter);
        chatInput.setTextFormatter(textFormatter);
        
        chatModeIndicator = new Text("Lobby");
        chatModeIndicator.setStyle("-fx-fill: white; -fx-font-size: 12px;");
        
        HBox modeBox = new HBox(5);
        modeBox.setAlignment(Pos.CENTER_RIGHT);
        modeBox.getChildren().add(chatModeIndicator);
        
        chatBox.getChildren().addAll(modeBox, chatScrollPane, chatInput);
        
        uiOverlay.getChildren().add(chatBox);
        
        gameScene.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER && !chatInput.isFocused()) { 
                event.consume(); 
                
                chatFadeOutTransition.stop(); 
                chatBox.setOpacity(1.0);      
                chatBox.setVisible(true);     
                
                chatInput.setVisible(true);
                chatInput.requestFocus();
                chatHideTimer.stop(); 
                return; 
            }
            
            if (!chatInput.isFocused()) {
                activeKeys.add(event.getCode());
                if (event.getCode() == KeyCode.ESCAPE) {
                    stage.setFullScreen(false);
                }
            }
        });
        
        gameScene.setOnKeyReleased(event -> {
            if (!chatInput.isFocused()) {
                activeKeys.remove(event.getCode());
            }
        });
        
        chatInput.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.TAB) {
                event.consume(); 
                cycleChatMode();
            } else if (event.getCode() == KeyCode.ENTER && !event.isShiftDown()) {
                event.consume();
                sendChatMessage();
                chatInput.clear();
                chatInput.setVisible(false);
                gameScene.getRoot().requestFocus(); 
                chatHideTimer.playFromStart(); 
            } else if (event.getCode() == KeyCode.ESCAPE) {
                event.consume();
                chatInput.setVisible(false);
                chatInput.clear();
                gameScene.getRoot().requestFocus(); 
                chatHideTimer.playFromStart(); 
            }
        });
    }

    /**
     * Adds a chat message to the chat flow.
     * Makes the chat box appear instantly without starting the hide timer.
     * Handles different message types including regular chat, whispers (sent/received).
     * 
     * @param sender The sender of the message ("You" for sent whispers).
     * @param recipient The recipient of the message ("Target" for sent whispers, null otherwise).
     * @param message The message content.
     * @param mode The chat mode (GLOBAL, LOBBY, TEAM) or null for whispers.
     */
    private void addChatMessage(String sender, String recipient, String message, ChatMode mode) {
        final String timeString = String.format("[%02d:%02d] ", 
                                LocalTime.now().getHour(),
                                LocalTime.now().getMinute());
        final String prefixDisplay;
        final Color prefixColor;
        final String senderDisplay;
        final Color senderColor;

        if (mode != null) { 
             switch (mode) {
                 case LOBBY:
                     prefixDisplay = "[LOBBY] ";
                     prefixColor = Color.GREEN; 
                     break;
                 case TEAM:
                     prefixDisplay = "[TEAM] ";
                     prefixColor = Color.BLUE; 
                     break;
                 case GLOBAL:
                 default:
                     prefixDisplay = "[GLOBAL] ";
                     prefixColor = Color.ORANGE; 
                     break;
             }
             senderDisplay = sender + ":";
             senderColor = Color.LIGHTBLUE;
        } else if (recipient != null) { 
             prefixDisplay = "[To " + recipient + "] ";
             prefixColor = Color.MAGENTA;
             senderDisplay = "You:";
             senderColor = Color.MAGENTA;
        } else { 
             prefixDisplay = "[From " + sender + "] ";
             prefixColor = Color.MAGENTA;
             senderDisplay = "";
             senderColor = Color.MAGENTA;
        }

        final String messageContent = message + "\n";

        Platform.runLater(() -> {
            Text timeText = new Text(timeString);
            timeText.setFill(Color.GRAY);

            Text prefixText = new Text(prefixDisplay);
            prefixText.setFill(prefixColor);

            Text senderText = new Text(senderDisplay);
            senderText.setFill(senderColor);
            
            Text messageText = new Text(messageContent);
            messageText.setFill(Color.WHITE); 
            
            chatFlow.getChildren().add(timeText);
            chatFlow.getChildren().add(prefixText);

            if (!senderDisplay.isEmpty()) {
                chatFlow.getChildren().add(senderText);
                Text spaceBuffer = new Text(" ");
                spaceBuffer.setFill(Color.WHITE);
                chatFlow.getChildren().add(spaceBuffer);
            }

            chatFlow.getChildren().add(messageText);
            
            chatScrollPane.setVvalue(1.0); 
            
            chatFadeOutTransition.stop(); 
            chatBox.setOpacity(1.0);
            chatBox.setVisible(true);
            
            chatHideTimer.playFromStart(); 
        });
    }

    /**
     * Sends the current chat message or processes a command like /whisper.
     */
    private void sendChatMessage() {
        String text = chatInput.getText().trim();
        chatInput.clear();

        if (text.isEmpty()) {
            return;
        }

        if (serverHandler == null || !serverHandler.isConnected()) {
            showError("Chat Error", "Cannot send message: Not connected to server");
            return;
        }

        String localSender = serverHandler.getConfirmedNickname() != null ? serverHandler.getConfirmedNickname() : this.username;

        if (text.toLowerCase().startsWith("/whisper ")) {
            String[] parts = text.split(" ", 3);
            if (parts.length == 3) {
                String targetUsername = parts[1];
                String whisperMessageContent = parts[2];

                if (targetUsername.equalsIgnoreCase(localSender)) {
                    addChatMessage("System", null, "You cannot whisper to yourself.", null);
                } else {
                    String whisperMarker = String.format("[WHISPER->%s] ", targetUsername);
                    String messageWithMarker = whisperMarker + whisperMessageContent;

                    String prefix;
                    switch (currentChatMode) {
                        case LOBBY:
                            prefix = "lobbychat:";
                            break;
                        case TEAM:
                            prefix = "teamchat:";
                            break;
                        case GLOBAL:
                        default:
                            prefix = "chat:";
                            break;
                    }
                    String messageToSend = prefix + messageWithMarker;

                    logger.info("Sending whisper via {}: {}", prefix, messageToSend);
                    serverHandler.sendMessage(messageToSend);

                    addChatMessage(localSender, targetUsername, whisperMessageContent, null);
                }
            } else {
                addChatMessage("System", null, "Usage: /whisper <username> <message>", null);
            }
        } else {
            String prefix;
            switch (currentChatMode) {
                case LOBBY:
                    prefix = "lobbychat:";
                    break;
                case TEAM:
                    prefix = "teamchat:";
                    break;
                case GLOBAL:
                default:
                    prefix = "chat:";
                    break;
            }
            String chatMessage = prefix + text;
            logger.info("Sending {} message: {}", currentChatMode, chatMessage);
            serverHandler.sendMessage(chatMessage);
            addChatMessage(localSender, null, text, currentChatMode);
        }
    }

    /**
     * Cycles through the available chat modes (GLOBAL -> LOBBY -> TEAM)
     */
    private void cycleChatMode() {
        currentChatMode = ChatMode.values()[(currentChatMode.ordinal() + 1) % ChatMode.values().length];
        String modeName = currentChatMode.getDisplayName();
        chatInput.setPromptText(modeName + " Chat (Press Enter)"); 
        chatModeIndicator.setText(modeName);
    }

    /**
     * The main entry point for launching the Game application *directly*.
     * This is generally NOT used; Game should be launched via LobbyGUI.
     * Kept for potential testing purposes.
     *
     * @param args Command line arguments (not used)
     */
    public static void main(String[] args) {
        logger.warn("Warning: Launching Game directly via main() requires manual initialization setup.");
        launch(args);
    }
} 