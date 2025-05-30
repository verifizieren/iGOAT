package igoat.client;

import static java.lang.Math.pow;
import static java.lang.Math.sqrt;

import igoat.Role;
import igoat.Timer;
import igoat.client.GUI.Banner;
import igoat.client.GUI.LobbyGUI;
import igoat.client.GUI.MainMenuGUI;
import igoat.client.GUI.SettingsWindow;
import igoat.client.GUI.SoundButton;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.UnaryOperator;
import javafx.animation.AnimationTimer;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
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
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main game class that handles the core game logic, rendering, and networking. This class manages:
 * - Player movement and collision detection - Camera and viewport management - Network
 * communication (TCP for game state, UDP for position updates) - Remote player synchronization -
 * Game state and visual updates The game uses a client-server architecture where each client sends
 * position updates via UDP and receives game state updates via TCP.
 */
public class Game extends Application {

    private static final Logger logger = LoggerFactory.getLogger(Game.class);
    private static final LanguageManager lang = LanguageManager.getInstance();

    private static final double MOVEMENT_SPEED = 200;
    private static final double CAMERA_ZOOM = 3; // Default is 3
    private final String style = getClass().getResource("/CSS/UI.css").toExternalForm();

    private Pane gamePane;
    private Pane uiOverlay;
    private Stage stage;
    private final LobbyGUI lobby;
    private double windowWidth;
    private double windowHeight;
    private Set<KeyCode> activeKeys;
    private long lastUpdate;
    private long lastPositionUpdate = 0;
    private static final long POSITION_UPDATE_INTERVAL = 100;
    private final SoundManager sound = SoundManager.getInstance();

    private Player player;
    private igoat.client.Map gameMap;
    private Camera camera;
    private ServerHandler serverHandler;
    private String playerName;
    private String lobbyCode;
    private String username;
    private final Timer timer = new Timer();
    private String time = "";

    private boolean gameStarted = false;

    private final LinkedHashMap<String, Player> otherPlayers = new LinkedHashMap<>();
    private final HashMapCycler<String, Player> spectatingPlayer = new HashMapCycler<>(
        otherPlayers);
    private final Map<String, Role> pendingRoles = new ConcurrentHashMap<>();
    private boolean pressedE = false;
    private double mouseX;
    private double mouseY;
    private double initialX = 80;
    private double initialY = 80;
    private boolean doorsOpen = false;
    private boolean spectating = false;
    private boolean initializedViewport = false;

    private static class InterpolatedPosition {

        double lastX, lastY, targetX, targetY;
        long lastUpdateTime, targetTime;

        InterpolatedPosition(double x, double y) {
            lastX = targetX = x;
            lastY = targetY = y;
            lastUpdateTime = targetTime = System.currentTimeMillis();
        }
    }

    private final Map<String, InterpolatedPosition> playerPositions = new ConcurrentHashMap<>();

    /**
     * Enum for defining the chat modes available in the game. This enum provides a way to manage
     * different chat modes within the game. It includes three modes: GLOBAL and LOBBY. Each mode
     * has a display name associated with it, which is used to identify the mode in the game's user
     * interface.
     */
    private enum ChatMode {
        GLOBAL(lang.get("lobby.globalChat")),
        LOBBY(lang.get("lobby.lobbyChat"));

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
    private Text timeText;

    private Banner terminalActivationBanner;
    private Banner allTerminalsBanner;
    private Banner noActivationBanner;
    private Banner reviveBanner;
    private Banner caughtBanner;

    /**
     * Constructor for Game
     *
     * @param lobby The Lobby GUI. This is used to call the lobby exit method after the game window
     *              closes.
     */
    public Game(LobbyGUI lobby) {
        this.lobby = lobby;
    }

    /**
     * Initializes the game with necessary data from the lobby.
     *
     * @param handler The active ServerHandler.
     * @param name    The player's confirmed name.
     * @param code    The lobby code.
     */
    public void initialize(ServerHandler handler, String name, String code) {
        this.serverHandler = handler;
        this.playerName = name;
        this.lobbyCode = code;
        this.gameStarted = true;
        this.username = name;

        startMessageProcessor();
        startUdpUpdateProcessor();

        if (this.serverHandler != null && this.serverHandler.isConnected()) {
            this.serverHandler.sendMessage("getroles:");
        }

        Platform.runLater(() -> {
            String confirmedNickname = serverHandler.getConfirmedNickname();
            if (confirmedNickname == null) {
                logger.error("Cannot initialize - confirmed nickname is null");
            }
        });

        sound.playSoundtrack();
    }

    /**
     * Initializes and starts the game VIEW. Assumes initialize() has been called previously. Sets
     * up the game window, player visuals, map, and input handling. Also initializes the game loop
     * using AnimationTimer.
     *
     * @param primaryStage The primary stage of the JavaFX application
     */
    @Override
    public void start(Stage primaryStage) {
        try {
            stage = primaryStage;
            stage.getIcons().add(MainMenuGUI.icon);
            stage.setOnCloseRequest(event -> exit());
            stage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);

            SettingsWindow settings = SettingsWindow.getInstance();
            stage.setFullScreen(false);
            settings.setGameStage(stage);

            if (serverHandler == null || playerName == null || lobbyCode == null) {
                showError(lang.get("game.initError"), lang.get("game.noConnectionError"));
                returnToLobby();
                return;
            }

            String confirmedNickname = serverHandler.getConfirmedNickname();
            if (confirmedNickname == null) {
                showError(lang.get("game.initError"), lang.get("game.noNameError"));
                returnToLobby();
                return;
            }

            serverHandler.sendMessage("ready:");

            gameMap = new igoat.client.Map(false);
            gamePane = new Pane();
            gamePane.setMinSize(gameMap.getWidth(), gameMap.getHeight());
            gamePane.setMaxSize(gameMap.getWidth(), gameMap.getHeight());
            gamePane.setPrefSize(gameMap.getWidth(), gameMap.getHeight());
            gamePane.setClip(new Rectangle(0, 0, gameMap.getWidth(), gameMap.getHeight()));

            Sprite floor = new Sprite("sprites/floor_tile01.png");
            gamePane.setBackground(floor.getBackground());

            Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
            double screenWidth = screenBounds.getWidth() * 0.8;
            double screenHeight = screenBounds.getHeight() * 0.8;

            double mapAspectRatio = (double) gameMap.getWidth() / gameMap.getHeight();

            if (screenWidth / screenHeight > mapAspectRatio) {
                windowHeight = screenHeight;
                windowWidth = screenHeight * mapAspectRatio;
            } else {
                windowWidth = screenWidth;
                windowHeight = screenWidth / mapAspectRatio;
            }

            stage.setWidth(windowWidth);
            stage.setHeight(windowHeight);
            stage.setFullScreenExitHint("");

            Pane container = new Pane();
            container.setStyle("-fx-background-color: black;");
            container.getChildren().add(gamePane);

            String style = getClass().getResource("/CSS/UI.css").toExternalForm();

            uiOverlay = new Pane();
            uiOverlay.getStylesheets().add(style);
            uiOverlay.setMouseTransparent(true);
            uiOverlay.prefWidthProperty().bind(stage.widthProperty());
            uiOverlay.prefHeightProperty().bind(stage.heightProperty());
            container.getChildren().add(uiOverlay);

            timeText = new Text();
            timeText.setX(10);
            timeText.setY(20);
            timeText.setFill(Color.WHITE);
            timeText.setFont(new Font("Jersey 10", 25));
            container.getChildren().add(timeText);

            Scene scene = new Scene(container);
            scene.setFill(Color.BLACK);
            scene.setOnMouseMoved(event -> {
                mouseX = event.getSceneX();
                mouseY = event.getSceneY();
                updateVisuals();
            });

            String windowTitle =
                "iGoat - " + lang.get("game.lobby") + " " + lobbyCode + " - " + lang.get(
                    "hs.player") + ": " + confirmedNickname;
            primaryStage.setTitle(windowTitle);

            primaryStage.setScene(scene);

            primaryStage.show();

            // create banners
            terminalActivationBanner = Banner.terminalActivation(uiOverlay);
            allTerminalsBanner = Banner.allTerminals(uiOverlay);
            noActivationBanner = Banner.noActivation(uiOverlay);
            reviveBanner = Banner.revive(uiOverlay);
            caughtBanner = Banner.caught(uiOverlay);

            for (Node wall : gameMap.getVisualWalls()) {
                gamePane.getChildren().add(wall);
            }

            // decoration implementation
            for (ImageView decor : gameMap.getDecorItems()) {
                gamePane.getChildren().add(decor);
            }

            player = new Player(gamePane, initialX, initialY, confirmedNickname);

            camera = new Camera(gamePane, primaryStage.getWidth(), primaryStage.getHeight(),
                CAMERA_ZOOM, true);

            scene.widthProperty().addListener((obs, oldVal, newVal) -> {
                camera.updateViewport(newVal.doubleValue(), scene.getHeight());
                windowWidth = newVal.doubleValue();
                updateVisuals();
            });

            scene.heightProperty().addListener((obs, oldVal, newVal) -> {
                camera.updateViewport(scene.getWidth(), newVal.doubleValue());
                windowHeight = newVal.doubleValue();
                updateVisuals();
            });

            primaryStage.fullScreenProperty().addListener((obs, oldVal, newVal) -> {
                Platform.runLater(() -> {
                    camera.updateViewport(scene.getWidth(), scene.getHeight());
                });
            });

            activeKeys = new HashSet<>();

            gamePane.setFocusTraversable(true);
            gamePane.requestFocus();

            lastUpdate = System.nanoTime();
            AnimationTimer mainLoop = new AnimationTimer() {
                @Override
                public void handle(long now) {
                    double deltaTime = (now - lastUpdate) / 1_000_000_000.0;
                    lastUpdate = now;
                    update(deltaTime);
                    if (serverHandler == null || !serverHandler.isConnected()) {
                        logger.error("Connection was closed");
                        exit();
                        stop();
                    }
                }
            };
            mainLoop.start();
            initializeChatUI();
            timer.reset(0);
            time = "0:0";
        } catch (Exception e) {
            logger.error("Error starting game", e);
        }
    }

    /**
     * Exit routine for the Game instance. This will close the game window and return to the lobby
     * screen.
     */

    private void returnToLobby() {
        gameStarted = false;
        sound.stopAll();
        stage.close();
        lobby.getStage().show();
        lobby.initializeServerCommunication();
    }

    /**
     * Forced exit routine. This will exit the game and disconnect from the server
     */
    private void exit() {
        gameStarted = false;
        sound.stopAll();
        stage.close();
        lobby.exit();
    }

    /**
     * Shows an error dialog to the user. This method is thread-safe and will run on the JavaFX
     * Application Thread.
     *
     * @param title   the title of the error dialog
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
     * Starts a background thread that processes TCP messages from the server. Handles game state
     * updates, chat messages, and player events. The thread runs continuously until the game ends
     * or the connection is lost.
     */
    private void startMessageProcessor() {
        if (serverHandler == null) {
            return;
        }

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
        });
        messageProcessor.setDaemon(true);
        messageProcessor.start();
    }

    /**
     * Processes TCP messages received from the server. Handles various message types including: -
     * Lobby player updates - Chat messages - Game state changes - Player events (catch, revive,
     * leave)
     *
     * @param message the message received from the server
     */
    private void processServerMessage(String message) {
        if (message.startsWith("timer:")) {
            String[] parts = message.split(":");
            if (parts.length == 3) {
                String code = parts[1];
                String msStr = parts[2];
                if (lobbyCode != null && lobbyCode.equals(code)) {
                    try {
                        long ms = Long.parseLong(msStr);
                        Platform.runLater(() -> {
                            timer.reset(ms);
                            time = timer.toString();
                            timeText.setText(time);
                        });
                    } catch (NumberFormatException e) {
                        logger.error("Invalid timer value: {}", msStr);
                    }
                }
            }
            return;
        }

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
            if (parts.length == 2) {
                return parts;
            }

            int firstSpaceIndex = data.indexOf(' ');
            if (firstSpaceIndex != -1) {
                String sender = data.substring(0, firstSpaceIndex);
                String content = data.substring(firstSpaceIndex + 1);
                return new String[]{sender, content};
            }

            logger.warn("Could not parse sender from message using known patterns: {}", msg);
            return new String[]{"System", data};
        };

        if (message.startsWith("error:")) {
            logger.warn("Received error: {}", message.substring("error:".length()));
            return;
        }

        ChatMode mode = null;
        String prefixString = null;
        if (message.startsWith("lobbychat:")) {
            mode = ChatMode.LOBBY;
            prefixString = "lobbychat:";
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
                        logger.info("removed player");
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
                String caughtPlayerName = message.substring("catch:".length());
                logger.info("{} was caught!", caughtPlayerName);
                caughtBanner.showAnimation(caughtPlayerName + " was caught!", 2);

                Platform.runLater(() -> {
                    if (player != null && caughtPlayerName.equals(player.getUsername())) {
                        player.setDown(true);
                        if (player.getRole() == Role.IGOAT) {
                            sound.igoatCatch.play();
                        } else if (player.getRole() == Role.GOAT) {
                            sound.goat.play();
                        }

                    } else {
                        Player other = otherPlayers.get(caughtPlayerName);
                        if (other != null) {
                            other.setDown(true);
                            if (other.getRole() == Role.IGOAT) {
                                sound.igoatCatch.play(other.getX(), other.getY(), player.getX(),
                                    player.getY(), 300);
                            } else if (player.getRole() == Role.GOAT) {
                                sound.goat.play(other.getX(), other.getY(), player.getX(),
                                    player.getY(), 300);
                            }
                        }
                    }
                });
            } else if (message.startsWith("revive:")) {
                String revivedPlayerName = message.substring("revive:".length());
                logger.info("{} was revived!", revivedPlayerName);
                reviveBanner.showAnimation(
                    String.format(lang.get("game.revive"), revivedPlayerName), 2);

                Platform.runLater(() -> {
                    if (player != null && revivedPlayerName.equals(player.getUsername())) {
                        player.setDown(false);
                        sound.goat.play();
                        logger.info("exiting spectator mode");
                        spectating = false;
                    } else {
                        Player other = otherPlayers.get(revivedPlayerName);
                        if (other != null) {
                            other.setDown(false);
                            if (other.getRole() == Role.GOAT) {
                                sound.goat.play(other.getX(), other.getY(), player.getX(),
                                    player.getY(), 300);
                            }
                        }
                    }
                });
            } else if (message.startsWith("player_left:")) {
                String leftPlayer = message.substring("player_left:".length());
                if (!leftPlayer.equals(this.playerName)) {
                    removeRemotePlayer(leftPlayer);
                    logger.info("player has left");
                }
            } else if (message.startsWith("terminal:")) {
                String[] parts = message.split(":");
                if (parts.length == 2) {
                    activateTerminal(Integer.parseInt(parts[1]));
                }
            } else if (message.startsWith("activateStation:")) {
                String[] parts = message.split(":");
                if (parts.length == 2) {
                    activateStation(Integer.parseInt(parts[1]));
                }
            } else if (message.startsWith("role:")) {
                String[] parts = message.split(":");
                if (parts.length == 3) {
                    String playerName = parts[1];
                    try {
                        Role role = Role.valueOf(parts[2]);
                        logger.info("Received role {} for player {}", role, playerName);
                        Platform.runLater(() -> {
                            if (player != null && playerName.equals(player.getUsername())) {
                                player.setRole(role);
                            } else {
                                Player other = otherPlayers.get(playerName);
                                if (other != null) {
                                    other.setRole(role);
                                } else {
                                    pendingRoles.put(playerName, role);
                                    logger.warn(
                                        "[role] Received role for unknown player {}. Storing temporarily.",
                                        playerName);
                                }
                            }
                        });
                    } catch (IllegalArgumentException e) {
                        logger.error("Invalid role value in message: {}", message);
                    }
                } else {
                    logger.error("Invalid role message format: {}", message);
                }
            } else if (message.startsWith("roles:")) {
                String rolesData = message.substring("roles:".length());
                if (!rolesData.isEmpty()) {
                    String[] roleEntries = rolesData.split(",");
                    Map<String, Role> rolesToApply = new HashMap<>();
                    Map<String, Role> rolesToPend = new HashMap<>();

                    for (String entry : roleEntries) {
                        String[] parts = entry.split("=");
                        if (parts.length == 2) {
                            String playerName = parts[0];
                            try {
                                Role role = Role.valueOf(parts[1]);
                                //logger.info("[roles] Processing role {} for player {}", role, playerName);
                                Platform.runLater(() -> {
                                    if (player != null && playerName.equals(player.getUsername())) {
                                        player.setRole(role);
                                    } else {
                                        Player other = otherPlayers.get(playerName);
                                        if (other != null) {
                                            other.setRole(role);
                                            rolesToApply.put(playerName, role);
                                        } else {
                                            rolesToPend.put(playerName, role);
                                            logger.warn(
                                                "[roles] Received role for unknown player {}. Storing temporarily.",
                                                playerName);
                                        }
                                    }
                                });
                            } catch (IllegalArgumentException e) {
                                logger.error("Invalid role value in roles message entry: {}",
                                    entry);
                            }
                        }
                    }

                    Platform.runLater(() -> {
                        for (Map.Entry<String, Role> entry : rolesToApply.entrySet()) {
                            String playerName = entry.getKey();
                            Role role = entry.getValue();
                            Player other = otherPlayers.get(playerName);
                            if (other != null) {
                                other.setRole(role);
                                rolesToPend.remove(playerName);
                            } else {
                                pendingRoles.put(playerName, role);
                                logger.warn(
                                    "[roles] Received role for unknown player {}. Storing temporarily.",
                                    playerName);
                            }
                        }
                        pendingRoles.putAll(rolesToPend);
                    });
                }
            } else if (message.equals("door")) {
                Platform.runLater(this::handleDoorsOpen);
                return;
            } else if (message.startsWith("gameover:")) {
                logger.info("received: {}", message);
                String[] parts = message.split(":");
                if (parts.length == 2) {
                    endGame(parts[1].equals("true"));
                }
            } else if (message.startsWith("ready_status:")) {
                return;
            } else {
                logger.warn("Received message with unknown prefix or format: {}", message);
            }
            return;
        }

        String[] parsed = parseSenderAndContent.apply(prefixString, message);
        String sender = parsed[0];
        String content = parsed[1];

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
                    logger.debug(
                        "Ignoring whisper not intended for this client (target: {}, local: {})",
                        targetUser, localNickname);
                }
                return;
            }
        }
        addChatMessage(sender, null, content, mode);

        if (message.startsWith("timer:")) {
            String[] parts = message.split(":");
            if (parts.length == 3) {
                String code = parts[1];
                String msStr = parts[2];
                if (lobbyCode != null && lobbyCode.equals(code)) {
                    try {
                        long ms = Long.parseLong(msStr);
                        Platform.runLater(() -> {
                            timer.reset(ms);
                            time = timer.toString();
                            timeText.setText(time);
                        });
                    } catch (NumberFormatException e) {
                        logger.error("Invalid timer value: {}", msStr);
                    }
                }
            }
        }
    }

    /**
     * shows the victory or loss screen to the player according to their role and the result from
     * the server
     *
     * @param result true if the guard has won, false otherwise
     */
    private void endGame(boolean result) {
        sound.stopAll();
        time = "";
        showWinLossScreen(result);
    }

    private void showWinLossScreen(boolean guardWon) {
        boolean isGuard = player.getRole() == Role.GUARD;
        boolean localWon = (isGuard && guardWon) || (!isGuard && !guardWon);

        String message = localWon ? "🎉 " + lang.get("game.winMSG") + " 🎉"
            : "💀 " + lang.get("game.lostMSG") + " 💀";
        Color color = localWon ? Color.GREEN : Color.RED;

        Platform.runLater(() -> {
            Text title = new Text(message);
            title.setFont(Font.font("Jersey 10", 40));
            title.setFill(color);

            // Scoreboard table
            GridPane grid = new GridPane();
            grid.setAlignment(Pos.CENTER);
            grid.setHgap(20);
            grid.setVgap(10);

            Label headerName = new Label(lang.get("hs.player"));
            headerName.setFont(Font.font("Jersey 10", FontWeight.BOLD, 14));
            headerName.setStyle(
                "-fx-background-color: #343a40; -fx-text-fill: white; -fx-padding: 5;");
            Label headerRole = new Label(lang.get("hs.role"));
            headerRole.setFont(Font.font("Jersey 10", FontWeight.BOLD, 14));
            headerRole.setStyle(
                "-fx-background-color: #343a40; -fx-text-fill: white; -fx-padding: 5;");
            Label headerResult = new Label(lang.get("hs.outcome"));
            headerResult.setFont(Font.font("Jersey 10", FontWeight.BOLD, 14));
            headerResult.setStyle(
                "-fx-background-color: #343a40; -fx-text-fill: white; -fx-padding: 5;");
            grid.add(headerName, 0, 0);
            grid.add(headerRole, 1, 0);
            grid.add(headerResult, 2, 0);

            List<Player> allPlayers = new ArrayList<>(otherPlayers.values());
            allPlayers.add(player);
            allPlayers.sort(Comparator.comparingInt(p ->
                ((guardWon && p.getRole() == Role.GUARD) || (!guardWon
                    && p.getRole() != Role.GUARD)) ? 0 : 1));

            for (int i = 0; i < allPlayers.size(); i++) {
                Player p = allPlayers.get(i);
                if (p.getRole() == null) {
                    continue;
                }

                boolean isWinner = (guardWon && p.getRole() == Role.GUARD) || (!guardWon
                    && p.getRole() != Role.GUARD);
                Label nameLabel = new Label(p.getUsername());

                Label roleLabel = new Label(p.getRole().name());
                Label resultLabel = new Label(isWinner ? lang.get("hs.won") : lang.get("hs.lost"));
                String rowStyle = isWinner
                    ? "-fx-background-color: #d4edda; -fx-text-fill: #155724; -fx-padding: 5;"
                    : "-fx-background-color: #f8d7da; -fx-text-fill: #721c24; -fx-padding: 5;";
                nameLabel.setStyle(rowStyle);
                roleLabel.setStyle(rowStyle);
                resultLabel.setStyle(rowStyle);
                grid.add(nameLabel, 0, i + 1);
                grid.add(roleLabel, 1, i + 1);
                grid.add(resultLabel, 2, i + 1);
            }

            SoundButton exitButton = new SoundButton(lang.get("game.exitGame"));
            exitButton.setOnAction(e -> returnToLobby());

            VBox layout = new VBox(20, title, grid, exitButton);
            layout.setAlignment(Pos.CENTER);
            layout.setStyle("-fx-background-color: #dff0d8; -fx-padding: 20;");
            Scene scene = new Scene(layout);
            scene.getStylesheets().add(style);
            stage.setScene(scene);
            stage.sizeToScene();
            stage.show();
        });
    }

    /**
     * Handles the opening of doors when all terminals are activated.
     */
    private void handleDoorsOpen() {
        if (gameMap != null && !doorsOpen) {
            gameMap.openDoors();
            doorsOpen = true;
            logger.info("Doors have been opened.");
        }

        sound.doors.play();
        allTerminalsBanner.showAnimation(lang.get("game.openDoors"), 4);
    }

    /**
     * Starts a background thread that processes UDP position updates from the server. This thread
     * runs at a higher frequency than the TCP message processor to ensure smooth player movement
     * updates. Also initiates the first position update for the local player.
     */
    private void startUdpUpdateProcessor() {
        if (serverHandler == null) {
            return;
        }

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

            String playerName = parts[1];
            try {
                int x = Integer.parseInt(parts[2]);
                int y = Integer.parseInt(parts[3]);

                String confirmedNickname = serverHandler.getConfirmedNickname();
                if (confirmedNickname == null) {
                    logger.error("Cannot process update - confirmed nickname is null");
                    return;
                }
                if (playerName.equals(confirmedNickname)) {
                    logger.info("Received position correction from server: {}, {}", x, y);
                    if (player == null) {
                        initialX = x;
                        initialY = y;
                    } else {
                        Platform.runLater(() -> player.updatePosition(x, y));
                    }
                    return;
                }
                if (otherPlayers.containsKey(playerName)) {
                    updateRemotePlayerPosition(playerName, x, y);
                } else {
                    createVisualForRemotePlayer(playerName, x, y);
                }
            } catch (NumberFormatException e) {
                logger.error("Invalid coordinates in update: {}", update, e);
            }
        } else if (update.startsWith("udp_ack:")) {
            logger.info("Received UDP acknowledgment from server");
        } else if (update.equals("door")) {
            Platform.runLater(this::handleDoorsOpen);
        } else {
            logger.info("Unrecognized UDP message format: {}", update);
        }
    }

    /**
     * Updates the position of a remote player.
     *
     * @param playerName The name of the remote player
     * @param x          The new x-coordinate
     * @param y          The new y-coordinate
     */
    private void updateRemotePlayerPosition(String playerName, int x, int y) {
        if (otherPlayers.containsKey(playerName)) {
            Player remotePlayer = otherPlayers.get(playerName);
            // Interpolate movement
            InterpolatedPosition interp = playerPositions.computeIfAbsent(playerName,
                k -> new InterpolatedPosition(x, y));
            interp.lastX = remotePlayer.getX();
            interp.lastY = remotePlayer.getY();
            interp.targetX = x;
            interp.targetY = y;
            interp.lastUpdateTime = System.currentTimeMillis();
            interp.targetTime = interp.lastUpdateTime + 100; // 100ms to reach target
        } else {
            logger.info("Player {} not found visually, creating at ({}, {})", playerName, x, y);
            createVisualForRemotePlayer(playerName, x, y);
        }
    }

    /**
     * Creates a visual representation for a remote player. If the player object already exists
     * (placeholder from role message), adds visuals. Otherwise, creates a new player object and
     * adds visuals. Applies any pending role after creation/visual addition.
     *
     * @param playerName The name of the remote player
     * @param x          The x-coordinate
     * @param y          The y-coordinate
     */
    private void createVisualForRemotePlayer(String playerName, int x, int y) {
        logger.info("Creating or updating visual for player: {}", playerName);

        Platform.runLater(() -> {
            Player remotePlayer = otherPlayers.get(playerName);

            if (remotePlayer != null) {
                logger.info(
                    "Player {} already exists in map, updating position and ensuring visuals.",
                    playerName);
                remotePlayer.updatePosition(x, y);

                Role pendingRole = pendingRoles.remove(playerName);
                if (pendingRole != null) {
                    remotePlayer.setRole(pendingRole);
                    logger.info("Applied pending role {} to existing player {}", pendingRole,
                        playerName);
                }

            } else {
                logger.info("Player {} not found, creating new Player object.", playerName);
                remotePlayer = new Player(gamePane, x, y, playerName);

                otherPlayers.put(playerName, remotePlayer);
                logger.info("Added new player {} to otherPlayers map.", playerName);

                Role pendingRole = pendingRoles.remove(playerName);
                if (pendingRole != null) {
                    remotePlayer.setRole(pendingRole);
                    logger.info("Applied pending role {} to newly created player {}", pendingRole,
                        playerName);
                } else {
                    if (serverHandler != null && serverHandler.isConnected()) {
                        logger.info(
                            "Requesting roles again as new player {} was created without a pending role.",
                            playerName);
                        serverHandler.sendMessage("getroles:");
                    }
                }
            }
        });
    }

    /**
     * Calculates the angle of the mouse position relative to the middle of the screen
     *
     * @return angle in radians
     */
    private double getMouseAngle() {
        double x = mouseX - (windowWidth / 2.0);
        double y = mouseY - (windowHeight / 2.0);

        return Math.atan2(y, x);
    }

    /**
     * Updates the visual elements of the game based on the local player's position. This includes:
     * - Updating the fog of war effect around other players - Clipping special game elements based
     * on visibility - Updating visual elements for dynamic game objects
     */
    private void updateVisuals() {
        double centerX;
        double centerY;

        if (spectating && spectatingPlayer.getCurrentValue() != null) {
            centerX = spectatingPlayer.getCurrentValue().getX() + (
                spectatingPlayer.getCurrentValue().getWidth() / 2.0);
            centerY = spectatingPlayer.getCurrentValue().getY() + (
                spectatingPlayer.getCurrentValue().getHeight() / 2.0);
        } else {
            centerX = player.getX() + (player.getWidth() / 2.0);
            centerY = player.getY() + (player.getHeight() / 2.0);
        }

        long now = System.currentTimeMillis();
        for (Map.Entry<String, Player> entry : otherPlayers.entrySet()) {
            String name = entry.getKey();
            Player p = entry.getValue();
            InterpolatedPosition interp = playerPositions.get(name);
            if (interp != null) {
                double t = Math.min(1.0, (now - interp.lastUpdateTime) / (double) (interp.targetTime
                    - interp.lastUpdateTime));
                double ix = interp.lastX + (interp.targetX - interp.lastX) * t;
                double iy = interp.lastY + (interp.targetY - interp.lastY) * t;
                p.updatePosition(ix, iy);
            }
        }
        for (Player otherPlayer : otherPlayers.values()) {
            Shape visualClip;
            Shape labelClip;
            if (player.getRole() == Role.GUARD) {
                visualClip = Camera.getCone(centerX, centerY, 100, getMouseAngle(), false, true);
                labelClip = Camera.getCone(centerX, centerY, 100, getMouseAngle(), false, true);
            } else {
                visualClip = new Circle(centerX, centerY, 100);
                labelClip = new Circle(centerX, centerY, 100);
            }
            otherPlayer.getVisual().setClip(visualClip);
            otherPlayer.getUsernameLabel().setClip(labelClip);
        }

        for (Terminal terminal : gameMap.getTerminalList()) {
            Shape clip = player.getRole() == Role.GUARD ? Camera.getCone(centerX, centerY, 100,
                getMouseAngle(), false, true)
                : new Circle(centerX, centerY, 100);
            terminal.setClip(clip);
        }

        for (IgoatStation station : gameMap.getStationList()) {
            Shape clip = player.getRole() == Role.GUARD ? Camera.getCone(centerX, centerY, 100,
                getMouseAngle(), false, true)
                : new Circle(centerX, centerY, 100);
            station.setClip(clip);
        }

        if (player.getRole() == Role.GUARD) {
            camera.updateCone(getMouseAngle());
        }
    }

    /**
     * Removes the visual representation of a remote player.
     *
     * @param remotePlayerName The name of the player to remove.
     */

    private void removeRemotePlayer(String remotePlayerName) {
        if (remotePlayerName.equals(this.playerName)) {
            logger.info("Ignoring removal of ourselves");
            return;
        }

        otherPlayers.get(remotePlayerName).setIdle();
    }

    /**
     * Updates the game state based on the elapsed time. This is the main game loop that handles: -
     * Player movement based on keyboard input - Collision detection with walls - Camera updates -
     * Position updates to server - Visual updates
     *
     * @param deltaTime time elapsed since last update in seconds
     */
    private void update(double deltaTime) {
        // prevent huge jumps when lagging
        deltaTime = Math.min(deltaTime, 0.4);

        if (player == null || !gameStarted) {
            return;
        }

        if (!initializedViewport) {
            stage.setFullScreen(SettingsWindow.getInstance().getFullscreen());
            camera.updateViewport(stage.getWidth(), stage.getHeight());
            initializedViewport = true;
        }

        double currentX = player.getX();
        double currentY = player.getY();
        double newX = currentX;
        double newY = currentY;

        Point2D direction = new Point2D(0, 0);

        // Only process movement if player is not down
        if (!player.isDown()) {
            SettingsWindow settings = SettingsWindow.getInstance();

            if (activeKeys.contains(settings.getKeyBinding("moveUp")) || activeKeys.contains(
                KeyCode.UP)) {
                direction = direction.add(0, -1);
            }
            if (activeKeys.contains(settings.getKeyBinding("moveDown")) || activeKeys.contains(
                KeyCode.DOWN)) {
                direction = direction.add(0, 1);
            }
            if (activeKeys.contains(settings.getKeyBinding("moveLeft")) || activeKeys.contains(
                KeyCode.LEFT)) {
                direction = direction.add(-1, 0);
            }
            if (activeKeys.contains(settings.getKeyBinding("moveRight")) || activeKeys.contains(
                KeyCode.RIGHT)) {
                direction = direction.add(1, 0);
            }
        }

        if (activeKeys.contains(SettingsWindow.getInstance().getKeyBinding("interact"))) {
            if (!pressedE) {
                pressedE = true;
                switch (player.getRole()) {
                    case Role.GUARD:
                        pressCatch();
                        break;
                    case Role.IGOAT:
                        pressTerminal();
                        break;
                    case Role.GOAT:
                        pressRevive();
                        sound.goat.play();
                }
            }
        } else {
            pressedE = false;
        }

        double slow_factor = player.getRole() == Role.GUARD ? 1 : 0.75;

        if (!direction.equals(Point2D.ZERO)) {
            direction = direction.normalize();
            direction = direction.multiply(MOVEMENT_SPEED * deltaTime * slow_factor);
        }

        double dx = direction.getX();
        double dy = direction.getY();

        double potentialX = currentX + dx;
        double potentialY = currentY + dy;

        boolean canMoveX = true;
        if (dx != 0) {
            for (Wall wall : gameMap.getCollisionWalls()) {
                if (player.collidesWithWall((int) potentialX, (int) currentY, wall)) {
                    canMoveX = false;
                    break;
                }
            }
        }

        if (dx != 0 && player.getRole() != Role.GOAT) {
            for (Wall wall : gameMap.getWindowCollisions()) {
                if (player.collidesWithWall((int) potentialX, (int) currentY, wall)) {
                    canMoveX = false;
                    break;
                }
            }
        }

        boolean canMoveY = true;
        if (dy != 0) {
            for (Wall wall : gameMap.getCollisionWalls()) {
                if (player.collidesWithWall((int) newX, (int) potentialY, wall)) {
                    canMoveY = false;
                    break;
                }
            }
        }
        if (dy != 0 && player.getRole() != Role.GOAT) {
            for (Wall wall : gameMap.getWindowCollisions()) {
                if (player.collidesWithWall((int) newX, (int) potentialY, wall)) {
                    canMoveY = false;
                    break;
                }
            }
        }

        if (canMoveX) {
            newX = potentialX;
        }

        if (canMoveY) {
            newY = potentialY;
        }

        player.updatePosition(newX, newY);

        boolean positionChanged = Math.abs(newX - currentX) > 1 || Math.abs(newY - currentY) > 1;

        long currentTime = System.currentTimeMillis();
        if (positionChanged || (currentTime - lastPositionUpdate > POSITION_UPDATE_INTERVAL)) {
            sendPlayerPositionUpdate();
            lastPositionUpdate = currentTime;
        }

        if (camera != null) {
            if (spectating) {
                Player spectating = spectatingPlayer.getCurrentValue();
                camera.update(spectating.getX() + (spectating.getWidth() / 2.0),
                    spectating.getY() + (spectating.getHeight() / 2.0));
            } else {
                camera.update(player.getX() + (player.getWidth() / 2.0),
                    player.getY() + (player.getHeight() / 2.0));
            }
        }

        updateVisuals();

        // update on screen timer if necessary
        timer.update();
        if (!time.isEmpty() && !time.equals(timer.toString())) {
            time = timer.toString();
            timeText.setText(time);
        }
    }

    private void pressCatch() {
        double x = player.getX() + (player.getWidth() / 2.0);
        double y = player.getY() + (player.getHeight() / 2.0);

        for (Player target : otherPlayers.values()) {
            double tx = target.getX() + (target.getWidth() / 2.0);
            double ty = target.getY() + (target.getHeight() / 2.0);
            if (!target.isDown() && sqrt(pow(tx - x, 2) + pow(ty - y, 2)) < 40.0) {
                logger.info("sent catch msg");
                serverHandler.sendMessage("catch:" + target.getUsername());
                return;
            }
        }
    }

    private void pressRevive() {
        double x = player.getX() + (player.getWidth() / 2.0);
        double y = player.getY() + (player.getHeight() / 2.0);

        // revive at current location
        for (Player target : otherPlayers.values()) {
            double tx = target.getX() + (target.getWidth() / 2.0);
            double ty = target.getY() + (target.getHeight() / 2.0);
            if (target.isDown() && target.getRole() == Role.IGOAT
                && sqrt(pow(tx - x, 2) + pow(ty - y, 2)) < 40.0) {
                serverHandler.sendMessage("revive:" + target.getUsername());
                return;
            }
        }

        // revive at igoat station
        for (IgoatStation station : gameMap.getStationList()) {
            double tx = station.getX() + (station.getWidth() / 2.0);
            double ty = station.getY() + (station.getHeight() / 2.0);
            if ((sqrt(pow(tx - x, 2) + pow(ty - y, 2)) < 60.0) && (!station.isActivated())) {
                serverHandler.sendMessage("station:" + station.getStationID());
                return;
            }
        }
    }

    private void activateStation(int id) {
        for (IgoatStation station : gameMap.getStationList()) {
            if (station.getStationID() == id) {
                station.activate(station.getX(), station.getY());
                return;
            }
        }
    }

    /**
     * Sends checks if the player is in range of a terminal to activate. If so, then a message is
     * sent to the server
     */
    private void pressTerminal() {
        double x = player.getX() + (player.getWidth() / 2.0);
        double y = player.getY() + (player.getHeight() / 2.0);

        for (Terminal terminal : gameMap.getTerminalList()) {
            double tx = terminal.getX() + (terminal.getWidth() / 2.0);
            double ty = terminal.getY() + (terminal.getHeight() / 2.0);
            if (sqrt(pow(tx - x, 2) + pow(ty - y, 2)) < 60.0) {
                serverHandler.sendMessage("terminal:" + terminal.getTerminalID());
                return;
            }
        }
    }

    /**
     * Activates a terminal and displays a banner or shows error banner otherwise
     *
     * @param id The ID of the terminal
     */
    private void activateTerminal(int id) {
        if (id == -1) {
            noActivationBanner.showAnimation(lang.get("game.termError"), 1.5);
            noActivationBanner.shake();
            sound.denied.play();
            return;
        }

        // display terminal activation banner
        terminalActivationBanner.showAnimation(String.format(lang.get("game.terminal"), id), 2.5f);

        for (Terminal terminal : gameMap.getTerminalList()) {
            if (terminal.getTerminalID() == id) {
                terminal.activate();
                sound.terminal.play(terminal.getX(), terminal.getY(), player.getX(), player.getY(),
                    300);
                return;
            }
        }
    }

    /**
     * Sends the local player's current position to the server via UDP. The update includes: -
     * Player's nickname - Current lobby code - X and Y coordinates
     * <p>
     * This is called periodically during the game loop when the player moves or when the position
     * update interval has elapsed.
     */
    private void sendPlayerPositionUpdate() {
        if (serverHandler != null && serverHandler.isConnected() && player != null) {
            int x = (int) player.getX();
            int y = (int) player.getY();

            String confirmedNickname = serverHandler.getConfirmedNickname();
            if (confirmedNickname == null) {
                logger.error("Cannot send position update - confirmed nickname is null");
                return;
            }

            String updateMessage = String.format("position:%s:%s:%d:%d", confirmedNickname,
                lobbyCode, x, y);
            serverHandler.sendUpdate(updateMessage);
        }
    }

    /**
     * Handles the game started event.
     */
    private void handleGameStarted() {
        gameStarted = true;
    }

    /**
     * Initializes the chat UI components with proper styling and positioning
     */
    private void initializeChatUI() {
        if (stage == null) {
            return;
        }
        Scene gameScene = stage.getScene();
        if (gameScene == null) {
            logger.error("Cannot initialize chat UI: Scene is null.");
            return;
        }

        chatBox = new VBox(5);
        chatBox.getStylesheets().add(style);
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
        chatFlow.getStylesheets().add(style);

        chatScrollPane = new ScrollPane(chatFlow);
        chatScrollPane.setStyle(
            "-fx-background: rgba(0, 0, 0, 0); -fx-background-color: rgba(0, 0, 0, 0.3); -fx-padding: 0;");
        chatScrollPane.getStylesheets().add(style);
        chatScrollPane.setFitToWidth(true);
        chatScrollPane.setPrefViewportHeight(250);
        chatScrollPane.setMaxHeight(250);
        chatScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        chatScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        chatScrollPane.setMouseTransparent(false);
        chatScrollPane.setPannable(false);

        chatInput = new TextArea();
        chatInput.setPromptText(lang.get("game.chatPrompt"));
        chatInput.setPrefRowCount(1);
        chatInput.setWrapText(true);
        chatInput.setStyle(
            "-fx-control-inner-background: rgba(0, 0, 0, 0.3); -fx-text-fill: white;");
        chatInput.getStylesheets().add(style);
        chatInput.setFont(new Font("Jersey 10", 16));
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

        chatModeIndicator = new Text(lang.get("game.lobby"));
        chatModeIndicator.setStyle("-fx-fill: white; -fx-font-size: 16px;");
        chatModeIndicator.setFont(new Font("Jersey 10", 16));

        HBox modeBox = new HBox(5);
        modeBox.setAlignment(Pos.CENTER_RIGHT);
        modeBox.getChildren().add(chatModeIndicator);

        chatBox.getChildren().addAll(modeBox, chatScrollPane, chatInput);

        uiOverlay.getChildren().add(chatBox);

        gameScene.setOnKeyPressed(event -> {
            if (event.getCode() == SettingsWindow.getInstance().getKeyBinding("chat")
                && !chatInput.isFocused()) {
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
                if (event.getCode() == SettingsWindow.getInstance().getKeyBinding("settings")) {
                    SettingsWindow.getInstance().open(stage);
                }
            }

            // spectator mode
            if (activeKeys.contains(SettingsWindow.getInstance().getKeyBinding("cycleSpectator"))
                && player.isDown() && player.getRole() != Role.GUARD && !otherPlayers.isEmpty()) {
                if (spectating) {
                    logger.info("switching perspective");
                    do {
                        spectatingPlayer.nextValue();
                    } while (spectatingPlayer.getCurrentValue() == null);
                } else {
                    logger.info("switch to spectator");
                    spectating = true;
                    while (spectatingPlayer.getCurrentValue() == null) {
                        spectatingPlayer.nextValue();
                    }
                }
            } else if (
                activeKeys.contains(SettingsWindow.getInstance().getKeyBinding("exitSpectator"))
                    && spectating) {
                spectating = false;
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
     * Adds a chat message to the chat flow. Makes the chat box appear instantly without starting
     * the hide timer. Handles different message types including regular chat, whispers
     * (sent/received).
     *
     * @param sender    The sender of the message ("You" for sent whispers).
     * @param recipient The recipient of the message ("Target" for sent whispers, null otherwise).
     * @param message   The message content.
     * @param mode      The chat mode (GLOBAL, LOBBY) or null for whispers.
     */
    private void addChatMessage(String sender, String recipient, String message, ChatMode mode) {
        final String timeString = String.format("[%02d:%02d] ",
            LocalTime.now().getHour(),
            LocalTime.now().getMinute());
        final String prefixDisplay;
        final Color prefixColor;
        final String senderDisplay;
        final Color senderColor;
        final Font font = new Font("Jersey 10", 15);

        if (mode != null) {
            prefixColor = switch (mode) {
                case LOBBY -> {
                    prefixDisplay = lang.get("lobby.lobby");
                    yield Color.GREEN;
                }
                default -> {
                    prefixDisplay = lang.get("lobby.global");
                    yield Color.ORANGE;
                }
            };
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
            timeText.setFont(font);

            Text prefixText = new Text(prefixDisplay);
            prefixText.setFill(prefixColor);
            prefixText.setFont(font);

            Text senderText = new Text(senderDisplay);
            senderText.setFill(senderColor);
            senderText.setFont(font);

            Text messageText = new Text(messageContent);
            messageText.setFill(Color.WHITE);
            messageText.setFont(font);

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

        String localSender =
            serverHandler.getConfirmedNickname() != null ? serverHandler.getConfirmedNickname()
                : this.username;

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

                    String prefix = switch (currentChatMode) {
                        case LOBBY -> "lobbychat:";
                        default -> "chat:";
                    };
                    String messageToSend = prefix + messageWithMarker;

                    logger.info("Sending whisper via {}: {}", prefix, messageToSend);
                    serverHandler.sendMessage(messageToSend);

                    addChatMessage(localSender, targetUsername, whisperMessageContent, null);
                }
            } else {
                addChatMessage("System", null, "Usage: /whisper <username> <message>", null);
            }
        } else {
            String prefix = switch (currentChatMode) {
                case LOBBY -> "lobbychat:";
                default -> "chat:";
            };
            String chatMessage = prefix + text;
            logger.info("Sending {} message: {}", currentChatMode, chatMessage);
            serverHandler.sendMessage(chatMessage);
            addChatMessage(localSender, null, text, currentChatMode);
        }
    }

    /**
     * Cycles through the available chat modes (GLOBAL -> LOBBY)
     */
    private void cycleChatMode() {
        currentChatMode = ChatMode.values()[(currentChatMode.ordinal() + 1)
            % ChatMode.values().length];
        String modeName = currentChatMode.getDisplayName();
        chatInput.setPromptText(modeName + lang.get("game.chatPrompt"));
        chatModeIndicator.setText(modeName);
    }

    /**
     * The main entry point for launching the Game application *directly*. This is generally NOT
     * used; Game should be launched via LobbyGUI. Kept for potential testing purposes.
     *
     * @param args Command line arguments (not used)
     */
    public static void main(String[] args) {
        logger.warn(
            "Warning: Launching Game directly via main() requires manual initialization setup.");
        launch(args);
    }

    @Override
    public void stop() {
        if (serverHandler != null) {
            serverHandler.close();
        }
        sound.stopAll();
    }

    public void updateSoundtrackVolume(double volume) {
        sound.setSoundtrackVolume(volume);
    }
} 